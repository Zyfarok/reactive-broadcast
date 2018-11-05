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

import org.junit.Before;
import org.junit.Test;

import ch.epfl.daeasy.config.Configuration;
import ch.epfl.daeasy.config.FIFOConfiguration;
import ch.epfl.daeasy.protocol.DAPacket;
import ch.epfl.daeasy.protocol.DatagramPacketConverter;
import ch.epfl.daeasy.protocol.MessageContent;
import ch.epfl.daeasy.rxlayers.RxClosableLayer;
import ch.epfl.daeasy.rxlayers.RxGroupedLayer;
import ch.epfl.daeasy.rxlayers.RxLayer;
import ch.epfl.daeasy.rxlayers.RxNil;
import ch.epfl.daeasy.rxsockets.RxBadRouter;
import ch.epfl.daeasy.rxsockets.RxClosableSocket;
import ch.epfl.daeasy.rxsockets.RxSocket;
import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;

public class UniformReliableBroadcastLayerTest {

    static List<Configuration> cfgs;
    static List<SocketAddress> addrs;
    static List<RxSocket<MessageContent>> sockets;
    static List<RxClosableSocket<DatagramPacket>> closables;
    static RxBadRouter router;

    public void setup(double dropRate, double loopRate, long delayStepMilliseconds) {
        RxBadRouter router = new RxBadRouter(dropRate, loopRate, delayStepMilliseconds, TimeUnit.MILLISECONDS);

        List<Configuration> cfgs = new ArrayList<>();
        List<SocketAddress> addrs = new ArrayList<>();
        List<RxSocket<MessageContent>> sockets = new ArrayList<>();
        List<RxClosableSocket<DatagramPacket>> closables = new ArrayList<>();

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

                final RxLayer<DatagramPacket, MessageContent> urb = beb.stack(new UniformReliableBroadcastLayer(cfgs.get(i)));

                final RxClosableSocket<DatagramPacket> closable = RxClosableSocket
                        .from(router.buildSocket(addrs.get(i)));
                sockets.add(closable.stack(urb));
                closables.add(closable);
            }
            UniformReliableBroadcastLayerTest.cfgs = cfgs;
            UniformReliableBroadcastLayerTest.addrs = addrs;
            UniformReliableBroadcastLayerTest.sockets = sockets;
            UniformReliableBroadcastLayerTest.router = router;
            UniformReliableBroadcastLayerTest.closables = closables;

        } catch (Exception e) {
            fail("exception: " + e.toString());
        }
    }

    @Test
    public void broadcastOneProducer() {
        setup(0.5, 0.5, 50);
        try {
            List<MessageContent> contents = IntStream.range(0, 200).mapToObj(x -> MessageContent.Message(x, 1))
                    .collect(Collectors.toList());
            Set<String> msgSet = contents.stream().map(MessageContent::toString).collect(Collectors.toSet());

            // Create TestObservers
            TestObserver<String> test2 = sockets.get(1).upPipe.map(x -> x.toString()).take(msgSet.size())
                    .test();
            TestObserver<String> test3 = sockets.get(2).upPipe.map(x -> x.toString()).take(msgSet.size())
                    .test();
            TestObserver<String> test4 = sockets.get(3).upPipe.map(x -> x.toString()).take(msgSet.size())
                    .test();
            TestObserver<String> test5 = sockets.get(4).upPipe.map(x -> x.toString()).take(msgSet.size())
                    .test();

            Observable.interval(10, TimeUnit.MILLISECONDS).zipWith(contents, (a, b) -> b)
                    .forEach(sockets.get(0).downPipe::onNext);

            test2.awaitDone(10, TimeUnit.SECONDS).assertValueCount(msgSet.size()).assertValueSet(msgSet);
            test3.awaitDone(10, TimeUnit.SECONDS).assertValueCount(msgSet.size()).assertValueSet(msgSet);
            test4.awaitDone(10, TimeUnit.SECONDS).assertValueCount(msgSet.size()).assertValueSet(msgSet);
            test5.awaitDone(10, TimeUnit.SECONDS).assertValueCount(msgSet.size()).assertValueSet(msgSet);

        } catch (Exception e) {
            fail("exception: " + e.toString());
        }
    }

    @Test
    public void broadcastMajorityFailed() {

        setup(0, 0, 0);
        try {

            List<MessageContent> contents = IntStream.range(0, 10).mapToObj(x -> MessageContent.Message(x, 1))
                    .collect(Collectors.toList());
            Set<String> msgSet = contents.stream().map(MessageContent::toString).collect(Collectors.toSet());

            // Create TestObservers
            TestObserver<String> test1 = sockets.get(0).upPipe.map(x -> x.toString()).take(msgSet.size())
                    .test();
            TestObserver<String> test2 = sockets.get(1).upPipe.map(x -> x.toString()).take(msgSet.size())
                    .test();
            TestObserver<String> test3 = sockets.get(2).upPipe.map(x -> x.toString()).take(msgSet.size())
                    .test();
            TestObserver<String> test4 = sockets.get(3).upPipe.map(x -> x.toString()).take(msgSet.size())
                    .test();
            TestObserver<String> test5 = sockets.get(4).upPipe.map(x -> x.toString()).take(msgSet.size())
                    .test();

            // close sockets after some time
            Observable.just(1).delay(300, TimeUnit.MILLISECONDS).subscribe(o -> closables.get(2).close());
            Observable.just(1).delay(500, TimeUnit.MILLISECONDS).subscribe(o -> closables.get(3).close());
            Observable.just(1).delay(700, TimeUnit.MILLISECONDS).subscribe(o -> closables.get(4).close());

            Observable.interval(200, TimeUnit.MILLISECONDS).zipWith(contents, (a, b) -> b)
                    .forEach(sockets.get(0).downPipe::onNext);

            test1.awaitDone(2, TimeUnit.SECONDS).assertValueCount(3);
            test2.assertValueCount(3);
            test3.assertValueCount(1);
            test4.assertValueCount(2);
            test5.assertValueCount(3);

        } catch (Exception e) {
            fail("exception: " + e.toString());
        }
    }

}