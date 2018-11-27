package ch.epfl.daeasy.layers;

import ch.epfl.daeasy.protocol.DAPacket;
import ch.epfl.daeasy.protocol.DatagramPacketConverter;
import ch.epfl.daeasy.protocol.MessageContent;
import ch.epfl.daeasy.rxlayers.RxLayer;
import ch.epfl.daeasy.rxlayers.RxNil;
import ch.epfl.daeasy.rxsockets.RxBadRouter;
import ch.epfl.daeasy.rxsockets.RxSocket;
import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;
import org.junit.Test;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class PerfectLinkLayerTest {
        private final DatagramPacketConverter daConverter = new DatagramPacketConverter();
        private final RxLayer<DatagramPacket, DAPacket<MessageContent>> layers = new RxNil<DatagramPacket>()
                        .scheduleOn(Schedulers.trampoline())
                        .convertPipes(daConverter)
                        .stack(new PerfectLinkLayer<>(MessageContent::toAck))
                        .scheduleOn(Schedulers.trampoline());

        @Test
        public void perfectLinkEndsUpSendingPacketsAndRemoveDuplicates() {
                RxBadRouter router = new RxBadRouter(0.8, 0.8, 25, MILLISECONDS);

                SocketAddress address1 = new InetSocketAddress("127.0.0.1", 1000);
                SocketAddress address2 = new InetSocketAddress("127.0.0.1", 1001);

                RxSocket<DAPacket<MessageContent>> socket1 = router.buildSocket(address1).stack(layers);
                RxSocket<DAPacket<MessageContent>> socket2 = router.buildSocket(address2).stack(layers);

                List<MessageContent> contents = IntStream.range(0, 100)
                                .mapToObj(x -> MessageContent.createMessage(x, 100 + x)).collect(Collectors.toList());
                Set<String> msgSet = contents.stream().map(MessageContent::toString).collect(Collectors.toSet());

                // Create TestObservers
                TestObserver<String> test = socket2.upPipe.map(x -> x.content.toString()).take(msgSet.size())
                                .test();
                Observable.interval(1, MILLISECONDS).zipWith(contents, (a, b) -> b).map(c -> new DAPacket<>(address2, c))
                                .forEach(socket1.downPipe::onNext);

                test.awaitDone(10, TimeUnit.SECONDS).assertValueCount(msgSet.size()).assertValueSet(msgSet);
        }

        private final RxLayer<DatagramPacket, DAPacket<MessageContent>> groupedLayers = new RxNil<DatagramPacket>()
                        .scheduleOn(Schedulers.trampoline()).convertPipes(daConverter)
                        .stackGroupedBy(dpkt -> dpkt.peer, new PerfectLinkLayer<>(MessageContent::toAck))
                        .scheduleOn(Schedulers.trampoline());

        @Test
        public void perfectLinkWithGroupedLayers() {
                RxBadRouter router = new RxBadRouter(0.8, 0.8, 25, MILLISECONDS);

                SocketAddress address1 = new InetSocketAddress("127.0.0.1", 1000);
                SocketAddress address2 = new InetSocketAddress("127.0.0.1", 1001);
                SocketAddress address3 = new InetSocketAddress("127.0.0.1", 1002);

                RxSocket<DAPacket<MessageContent>> socket1 = router.buildSocket(address1).stack(groupedLayers);
                RxSocket<DAPacket<MessageContent>> socket2 = router.buildSocket(address2).stack(groupedLayers);
                RxSocket<DAPacket<MessageContent>> socket3 = router.buildSocket(address3).stack(groupedLayers);

                List<Integer> allInts = IntStream.range(0, 200).boxed().collect(Collectors.toList());

                List<MessageContent> contentsTo2 = allInts.stream().filter(x -> x % 2 == 0)
                                .map(x -> MessageContent.createMessage(x, 200 + x)).collect(Collectors.toList());
                List<MessageContent> contentsTo3 = allInts.stream().filter(x -> x % 2 != 0)
                                .map(x -> MessageContent.createMessage(x, 200 + x)).collect(Collectors.toList());

                Set<String> msgSetTo2 = contentsTo2.stream().map(MessageContent::toString).collect(Collectors.toSet());
                Set<String> msgSetTo3 = contentsTo3.stream().map(MessageContent::toString).collect(Collectors.toSet());

                List<DAPacket> packets = Stream
                                .concat(contentsTo2.stream().map(c -> new DAPacket<>(address2, c)),
                                                contentsTo3.stream().map(c -> new DAPacket<>(address3, c)))
                                .collect(Collectors.toList());

                // Create TestObservers
                TestObserver<String> test2 = socket2.upPipe.map(x -> x.content.toString()).take(msgSetTo2.size())
                                .test();
                TestObserver<String> test3 = socket3.upPipe.map(x -> x.content.toString()).take(msgSetTo3.size())
                                .test();

                // Send all packets
                Observable.interval(1, MILLISECONDS).zipWith(packets, (a, b) -> b).forEach(socket1.downPipe::onNext);

                // Check that they all got their packets.
                test2.awaitDone(10, TimeUnit.SECONDS).assertValueCount(msgSetTo2.size()).assertValueSet(msgSetTo2);

                test3.awaitDone(5, TimeUnit.SECONDS).assertValueCount(msgSetTo3.size()).assertValueSet(msgSetTo3);
        }

}