package ch.epfl.daeasy.layers;

import static org.junit.Assert.fail;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import ch.epfl.daeasy.rxsockets.RxClosableSocket;
import com.google.common.collect.Sets;

import org.junit.Before;
import org.junit.Test;

import ch.epfl.daeasy.config.Configuration;
import ch.epfl.daeasy.config.FIFOConfiguration;
import ch.epfl.daeasy.protocol.DAPacket;
import ch.epfl.daeasy.protocol.DatagramPacketConverter;
import ch.epfl.daeasy.protocol.MessageContent;
import ch.epfl.daeasy.rxlayers.RxGroupedLayer;
import ch.epfl.daeasy.rxlayers.RxLayer;
import ch.epfl.daeasy.rxlayers.RxNil;
import ch.epfl.daeasy.rxsockets.RxBadRouter;
import ch.epfl.daeasy.rxsockets.RxSocket;
import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;

public class BestEffortBroadcastLayerTest {

        private static List<SocketAddress> addrs;
        private static List<RxClosableSocket<DatagramPacket>> closables;
        private static List<RxSocket<DAPacket<MessageContent>>> sockets;

        private void setup(double dropRate) {
                RxBadRouter router = new RxBadRouter(dropRate);

                List<Configuration> cfgs = new ArrayList<>();
                List<SocketAddress> addrs = new ArrayList<>();
                List<RxClosableSocket<DatagramPacket>> closables = new ArrayList<>();
                List<RxSocket<DAPacket<MessageContent>>> sockets = new ArrayList<>();

                try {
                        for (int i = 0; i < 5; i++) {

                                cfgs.add(new FIFOConfiguration(i + 1, "test/membership_FIFO_template.txt", 1));
                                addrs.add(new InetSocketAddress("127.0.0.1", 10001 + i));

                                RxLayer<DAPacket<MessageContent>, DAPacket<MessageContent>> perfectLinkLayer =
                                        new PerfectLinkLayer<>(MessageContent::toAck);

                                final DatagramPacketConverter daConverter = new DatagramPacketConverter();
                                final RxLayer<DatagramPacket, DAPacket<MessageContent>> perfectLinks = new RxNil<DatagramPacket>()
                                                .convertPipes(daConverter).stack(perfectLinkLayer);
                                // .stack(RxGroupedLayer.create(x -> x.getPeer().toString(), perfectLinkLayer));

                                final RxLayer<DatagramPacket, DAPacket<MessageContent>> beb = perfectLinks
                                                .stack(new BestEffortBroadcastLayer<>(cfgs.get(i)));

                                closables.add(router.buildSocket(addrs.get(i)).toClosable());
                                sockets.add(closables.get(i).stack(beb));
                        }
                        BestEffortBroadcastLayerTest.addrs = addrs;
                        BestEffortBroadcastLayerTest.closables = closables;
                        BestEffortBroadcastLayerTest.sockets = sockets;

                } catch (Exception e) {
                        fail("exception: " + e.toString());
                }
        }

        @Test
        public void broadcastOneProducer() {
                setup(0.8);
                try {

                        List<MessageContent> contents = IntStream.range(0, 100)
                                        .mapToObj(x -> MessageContent.createMessage(1, x)).collect(Collectors.toList());
                        Set<String> msgSet = contents.stream().map(MessageContent::toString)
                                        .collect(Collectors.toSet());

                        // Create TestObservers
                        TestObserver<String> test2 = sockets.get(1).upPipe.map(x -> x.content.toString())
                                        .take(msgSet.size()).test();
                        TestObserver<String> test3 = sockets.get(2).upPipe.map(x -> x.content.toString())
                                        .take(msgSet.size()).test();

                        Observable.interval(10, TimeUnit.MILLISECONDS).zipWith(contents, (a, b) -> b)
                                        .map(c -> new DAPacket<>(addrs.get(0), c))
                                        .forEach(sockets.get(0).downPipe::onNext);

                        test2.awaitDone(10, TimeUnit.SECONDS).assertValueCount(msgSet.size()).assertValueSet(msgSet);
                        test3.awaitDone(10, TimeUnit.SECONDS).assertValueCount(msgSet.size()).assertValueSet(msgSet);

                } catch (Exception e) {
                        fail("exception: " + e.toString());
                }
        }

        @Test
        public void broadcastTwoProducer() {
                setup(0.8);
                try {

                        List<MessageContent> contents1 = IntStream.range(0, 100)
                                        .mapToObj(x -> MessageContent.createMessage(1, x)).collect(Collectors.toList());
                        Set<String> msgSet1 = contents1.stream().map(MessageContent::toString)
                                        .collect(Collectors.toSet());

                        List<MessageContent> contents2 = IntStream.range(0, 50)
                                        .mapToObj(x -> MessageContent.createMessage(2, x)).collect(Collectors.toList());
                        Set<String> msgSet2 = contents2.stream().map(MessageContent::toString)
                                        .collect(Collectors.toSet());

                        Set<String> msgAll = Sets.union(msgSet1, msgSet2);

                        // Create TestObservers
                        TestObserver<String> test1 = sockets.get(0).upPipe.map(x -> x.content.toString())
                                        .take(msgSet2.size()).test();
                        TestObserver<String> test5 = sockets.get(4).upPipe.map(x -> x.content.toString())
                                        .take(msgSet1.size() + msgSet2.size()).test();

                        Observable.interval(10, TimeUnit.MILLISECONDS).zipWith(contents1, (a, b) -> b)
                                        .map(c -> new DAPacket<>(addrs.get(0), c))
                                        .forEach(sockets.get(0).downPipe::onNext);
                        Observable.interval(10, TimeUnit.MILLISECONDS).zipWith(contents2, (a, b) -> b)
                                        .map(c -> new DAPacket<>(addrs.get(1), c))
                                        .forEach(sockets.get(1).downPipe::onNext);

                        test1.awaitDone(10, TimeUnit.SECONDS).assertValueCount(msgSet2.size()).assertValueSet(msgSet2);
                        test5.awaitDone(10, TimeUnit.SECONDS).assertValueCount(msgAll.size()).assertValueSet(msgAll);

                } catch (Exception e) {
                        fail("exception: " + e.toString());
                }

        }

        @Test
        public void advancedTest() throws InterruptedException {
                setup(0);

                closables.forEach(RxClosableSocket::close);
                closables.get(3).open();
                closables.get(4).open();

                List<MessageContent> contents1 = IntStream.range(0, 13).mapToObj(x -> MessageContent.createMessage(3, x))
                                .collect(Collectors.toList());
                Set<String> msgSet1 = contents1.stream().map(MessageContent::toString).collect(Collectors.toSet());

                List<MessageContent> contents2 = IntStream.range(0, 7).mapToObj(x -> MessageContent.createMessage(4, x))
                                .collect(Collectors.toList());
                Set<String> msgSet2 = contents2.stream().map(MessageContent::toString).collect(Collectors.toSet());

                TestObserver<String> test1 = sockets.get(0).upPipe.map(x -> x.content.toString())
                                .take(msgSet1.size() + msgSet2.size()).test();
                TestObserver<String> test2 = sockets.get(1).upPipe.map(x -> x.content.toString())
                                .take(msgSet1.size() + msgSet2.size()).test();
                TestObserver<String> test3 = sockets.get(2).upPipe.map(x -> x.content.toString())
                                .take(msgSet1.size() + msgSet2.size()).test();
                TestObserver<String> test4 = sockets.get(3).upPipe.map(x -> x.content.toString())
                                .take(msgSet1.size() + msgSet2.size()).test();
                TestObserver<String> test5 = sockets.get(4).upPipe.map(x -> x.content.toString())
                                .take(msgSet1.size() + msgSet2.size()).test();

                Thread.sleep(500);

                Observable.interval(50, TimeUnit.MILLISECONDS).zipWith(contents1, (a, b) -> b)
                                .map(c -> new DAPacket<>(null, c)).forEach(sockets.get(2).downPipe::onNext);

                Observable.interval(43, TimeUnit.MILLISECONDS).zipWith(contents2, (a, b) -> b)
                                .map(c -> new DAPacket<>(null, c)).forEach(sockets.get(3).downPipe::onNext);

                Thread.sleep(3000);

                test1.assertValueCount(0);
                test2.assertValueCount(0);
                test3.assertValueCount(0);
                test4.assertValueCount(0);
                test5.assertValueCount(msgSet2.size()).assertValueSet(msgSet2);

                closables.get(4).close();
                Thread.sleep(500);
                closables.get(2).open();

                Thread.sleep(3000);

                test1.assertValueCount(0);
                test2.assertValueCount(0);
                test3.assertValueCount(msgSet2.size()).assertValueSet(msgSet2);
                test4.assertValueCount(msgSet1.size()).assertValueSet(msgSet1);
                test5.assertValueCount(msgSet2.size()).assertValueSet(msgSet2);

                closables.get(1).open();

                Thread.sleep(3000);

                test1.assertValueCount(0);
                test2.assertValueCount(msgSet1.size() + msgSet2.size()).assertValueSet(
                                Stream.concat(msgSet1.stream(), msgSet2.stream()).collect(Collectors.toSet()));
                test3.assertValueCount(msgSet2.size()).assertValueSet(msgSet2);
                test4.assertValueCount(msgSet1.size()).assertValueSet(msgSet1);
                test5.assertValueCount(msgSet2.size()).assertValueSet(msgSet2);
        }
}