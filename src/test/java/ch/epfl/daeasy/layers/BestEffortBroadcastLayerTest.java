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

public class BestEffortBroadcastLayerTest {

    static List<Configuration> cfgs;
    static List<SocketAddress> addrs;
    static List<RxSocket<DAPacket>> sockets;
    static RxBadRouter router;

    @Before
    public void setup() {
        RxBadRouter router = new RxBadRouter(0.9, 0.9, 50, TimeUnit.MILLISECONDS);

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

                final RxLayer<DatagramPacket, DAPacket> beb = perfectLinks
                        .stack(new BestEffortBroadcastLayer(cfgs.get(i)));

                sockets.add(router.buildSocket(addrs.get(i)).stack(beb));
            }
            BestEffortBroadcastLayerTest.cfgs = cfgs;
            BestEffortBroadcastLayerTest.addrs = addrs;
            BestEffortBroadcastLayerTest.sockets = sockets;
            BestEffortBroadcastLayerTest.router = router;

        } catch (Exception e) {
            fail("exception: " + e.toString());
        }
    }

    @Test
    public void broadcastOneProducer() {
        try {

            List<MessageContent> contents = IntStream.range(0, 100).mapToObj(x -> MessageContent.Message(x, 1))
                    .collect(Collectors.toList());
            Set<String> msgSet = contents.stream().map(MessageContent::toString).collect(Collectors.toSet());

            // Create TestObservers
            TestObserver<String> test2 = sockets.get(1).upPipe.map(x -> x.getContent().toString()).take(msgSet.size())
                    .test();
            TestObserver<String> test3 = sockets.get(2).upPipe.map(x -> x.getContent().toString()).take(msgSet.size())
                    .test();

            Observable.interval(10, TimeUnit.MILLISECONDS).zipWith(contents, (a, b) -> b)
                    .map(c -> new DAPacket(addrs.get(0), c)).forEach(sockets.get(0).downPipe::onNext);

            test2.awaitDone(10, TimeUnit.SECONDS).assertValueCount(msgSet.size()).assertValueSet(msgSet);
            test3.awaitDone(10, TimeUnit.SECONDS).assertValueCount(msgSet.size()).assertValueSet(msgSet);

        } catch (Exception e) {
            fail("exception: " + e.toString());
        }
    }

    @Test
    public void broadcastTwoProducer() {
        try {

            List<MessageContent> contents1 = IntStream.range(0, 100).mapToObj(x -> MessageContent.Message(x, 1))
                    .collect(Collectors.toList());
            Set<String> msgSet1 = contents1.stream().map(MessageContent::toString).collect(Collectors.toSet());

            List<MessageContent> contents2 = IntStream.range(0, 50).mapToObj(x -> MessageContent.Message(x, 2))
                    .collect(Collectors.toList());
            Set<String> msgSet2 = contents2.stream().map(MessageContent::toString).collect(Collectors.toSet());

            Set<String> msgAll = Sets.union(msgSet1, msgSet2);

            // Create TestObservers
            TestObserver<String> test1 = sockets.get(0).upPipe.map(x -> x.getContent().toString()).take(msgSet2.size())
                    .test();
            TestObserver<String> test5 = sockets.get(4).upPipe.map(x -> x.getContent().toString())
                    .take(msgSet1.size() + msgSet2.size()).test();

            Observable.interval(10, TimeUnit.MILLISECONDS).zipWith(contents1, (a, b) -> b)
                    .map(c -> new DAPacket(addrs.get(0), c)).forEach(sockets.get(0).downPipe::onNext);
            Observable.interval(10, TimeUnit.MILLISECONDS).zipWith(contents2, (a, b) -> b)
                    .map(c -> new DAPacket(addrs.get(1), c)).forEach(sockets.get(1).downPipe::onNext);

            test1.awaitDone(10, TimeUnit.SECONDS).assertValueCount(msgSet2.size()).assertValueSet(msgSet2);
            test5.awaitDone(10, TimeUnit.SECONDS).assertValueCount(msgAll.size()).assertValueSet(msgAll);

        } catch (Exception e) {
            fail("exception: " + e.toString());
        }

    }
}