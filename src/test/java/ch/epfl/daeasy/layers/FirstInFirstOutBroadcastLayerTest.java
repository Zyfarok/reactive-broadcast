package ch.epfl.daeasy.layers;

import static org.junit.Assert.fail;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

public class FirstInFirstOutBroadcastLayerTest {

    static List<Configuration> cfgs;
    static List<SocketAddress> addrs;
    static List<RxSocket<DAPacket>> sockets;
    static RxBadRouter router;

    @Before
    public void setup() {
        RxBadRouter router = new RxBadRouter(0.1, 0.99, 1005, TimeUnit.MILLISECONDS);

        List<Configuration> cfgs = new ArrayList<>();
        List<SocketAddress> addrs = new ArrayList<>();
        List<RxSocket<DAPacket>> sockets = new ArrayList<>();

        try {
            for (int i = 0; i < 5; i++) {
                cfgs.add(new FIFOConfiguration(i + 1, "test/membership_fifo_test.txt"));
                addrs.add(new InetSocketAddress("127.0.0.1", 10001 + i));

                RxLayer<DAPacket, DAPacket> perfectLinkLayer = new PerfectLinkLayer();

                final DatagramPacketConverter daConverter = new DatagramPacketConverter();
                final RxLayer<DatagramPacket, DAPacket> perfectLinks = new RxNil<DatagramPacket>()
                        .convertPipes(daConverter)
                        .stack(RxGroupedLayer.create(x -> x.getPeer().toString(), perfectLinkLayer));

                final RxLayer<DatagramPacket, DAPacket> fifo = perfectLinks
                        .stack(new BestEffortBroadcastLayer(cfgs.get(i)))
                        .stack(new UniformReliableBroadcastLayer(cfgs.get(i)))
                        .stack(new FirstInFirstOutBroadcastLayer(cfgs.get(i)));

                sockets.add(router.buildSocket(addrs.get(i)).stack(fifo));
            }
            FirstInFirstOutBroadcastLayerTest.cfgs = cfgs;
            FirstInFirstOutBroadcastLayerTest.addrs = addrs;
            FirstInFirstOutBroadcastLayerTest.sockets = sockets;
            FirstInFirstOutBroadcastLayerTest.router = router;

        } catch (Exception e) {
            fail("exception: " + e.toString());
        }
    }

    @Test
    public void fifoOneProducer() {
        // P1 sends 10 message, with a high probability the packets will be delayed and
        // be received unordered
        // Other processes should have received each packet in order
        try {

            List<MessageContent> contents = IntStream.range(1, 11).mapToObj(x -> MessageContent.Message(x, 1))
                    .collect(Collectors.toList());
            List<String> msgList = contents.stream().map(MessageContent::toString).collect(Collectors.toList());

            // Create TestObservers
            TestObserver<String> test2 = sockets.get(1).upPipe.map(x -> x.getContent().toString()).take(msgList.size())
                    .test();
            TestObserver<String> test3 = sockets.get(2).upPipe.map(x -> x.getContent().toString()).take(msgList.size())
                    .test();

            Observable.interval(1, TimeUnit.MILLISECONDS).zipWith(contents, (a, b) -> b)
                    .map(c -> new DAPacket(addrs.get(0), c)).forEach(sockets.get(0).downPipe::onNext);

            test2.awaitDone(10, TimeUnit.SECONDS).assertValueCount(msgList.size()).assertValueSequence(msgList);
            test3.awaitDone(10, TimeUnit.SECONDS).assertValueCount(msgList.size()).assertValueSequence(msgList);

        } catch (Exception e) {
            fail("exception: " + e.toString());
        }
    }

    @Test
    public void fifoTwoProducers() {
        // P1 sends 10 message, with a high probability the packets will be delayed and
        // be received unordered
        // P2 does the same
        // Other processes should have received each packet in order
        try {

            List<MessageContent> contents1 = IntStream.range(1, 11).mapToObj(x -> MessageContent.Message(x, 1))
                    .collect(Collectors.toList());
            List<String> msgList1 = contents1.stream().map(MessageContent::toString).collect(Collectors.toList());
            List<MessageContent> contents2 = IntStream.range(1, 21).mapToObj(x -> MessageContent.Message(x, 2))
                    .collect(Collectors.toList());
            List<String> msgList2 = contents2.stream().map(MessageContent::toString).collect(Collectors.toList());

            Observable<DAPacket> p5socketTest = sockets.get(4).upPipe.share();

            // Create TestObservers
            TestObserver<String> testfromP1 = p5socketTest.filter(x -> x.getContent().getPID() == 1)
                    .map(x -> x.getContent().toString()).take(msgList1.size()).test();
            TestObserver<String> testfromP2 = p5socketTest.filter(x -> x.getContent().getPID() == 2)
                    .map(x -> x.getContent().toString()).take(msgList2.size()).test();

            Observable.interval(1, TimeUnit.MILLISECONDS).zipWith(contents1, (a, b) -> b)
                    .map(c -> new DAPacket(addrs.get(0), c)).forEach(sockets.get(0).downPipe::onNext);
            Observable.interval(1, TimeUnit.MILLISECONDS).zipWith(contents2, (a, b) -> b)
                    .map(c -> new DAPacket(addrs.get(1), c)).forEach(sockets.get(0).downPipe::onNext);

            // P5 should have received every message from P1 in order
            testfromP1.awaitDone(10, TimeUnit.SECONDS).assertValueCount(msgList1.size()).assertValueSequence(msgList1);
            // P5 should have received every message from P2 in order
            testfromP2.awaitDone(10, TimeUnit.SECONDS).assertValueCount(msgList2.size()).assertValueSequence(msgList2);
        } catch (Exception e) {
            fail("exception: " + e.toString());
        }
    }

}