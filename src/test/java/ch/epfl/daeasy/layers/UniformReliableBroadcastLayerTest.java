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
import java.util.stream.Stream;

import ch.epfl.daeasy.rxsockets.RxUDPSocket;
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
    static List<RxSocket<DAPacket>> sockets;
    static List<RxClosableSocket<DatagramPacket>> closables;
    static RxBadRouter router;

    public void setup(double dropRate, double loopRate, long delayStepMilliseconds) {
        RxBadRouter router = new RxBadRouter(dropRate, loopRate, delayStepMilliseconds, TimeUnit.MILLISECONDS);

        List<Configuration> cfgs = new ArrayList<>();
        List<SocketAddress> addrs = new ArrayList<>();
        List<RxSocket<DAPacket>> sockets = new ArrayList<>();
        List<RxClosableSocket<DatagramPacket>> closables = new ArrayList<>();

        try {
            for (int i = 0; i < 5; i++) {

                cfgs.add(new FIFOConfiguration(i + 1, "test/membership_fifo_test.txt"));
                addrs.add(new InetSocketAddress("127.0.0.1", 10001 + i));

                RxLayer<DAPacket, DAPacket> perfectLinkLayer = new PerfectLinkLayer();

                final DatagramPacketConverter daConverter = new DatagramPacketConverter();
                final RxLayer<DatagramPacket, DAPacket> perfectLinks = new RxNil<DatagramPacket>()
                        .convertPipes(daConverter)
                        //.stack(RxGroupedLayer.create(x -> x.getPeer().toString(), perfectLinkLayer));
                        .stack(perfectLinkLayer);


                final RxLayer<DatagramPacket, DAPacket> beb = perfectLinks
                        .stack(new BestEffortBroadcastLayer(cfgs.get(i)));

                final RxLayer<DatagramPacket, DAPacket> urb = beb.stack(new UniformReliableBroadcastLayer(cfgs.get(i)));

                final RxClosableSocket<DatagramPacket> closable = router.buildSocket(addrs.get(i)).toClosable();
                //final RxClosableSocket<DatagramPacket> closable = new RxUDPSocket(addrs.get(i)).toClosable();
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
            TestObserver<String> test1 = sockets.get(0).upPipe.map(x -> x.getContent().toString()).take(msgSet.size())
                    .test();
            TestObserver<String> test2 = sockets.get(1).upPipe.map(x -> x.getContent().toString()).take(msgSet.size())
                    .test();
            TestObserver<String> test3 = sockets.get(2).upPipe.map(x -> x.getContent().toString()).take(msgSet.size())
                    .test();
//            TestObserver<String> test4 = sockets.get(3).upPipe.map(x -> x.getContent().toString()).take(msgSet.size())
//                    .test();
//            TestObserver<String> test5 = sockets.get(4).upPipe.map(x -> x.getContent().toString()).take(msgSet.size())
//                    .test();

            closables.get(3).close();
            closables.get(4).close();

            Observable.interval(20, TimeUnit.MILLISECONDS).zipWith(contents, (a, b) -> b)
            //Observable.fromIterable(contents)
                    .map(c -> new DAPacket(addrs.get(0), c)).forEach(sockets.get(0).downPipe::onNext);

            test1.awaitDone(10, TimeUnit.SECONDS).assertValueCount(msgSet.size()).assertValueSet(msgSet);
            test2.awaitDone(8, TimeUnit.SECONDS).assertValueCount(msgSet.size()).assertValueSet(msgSet);
            test3.awaitDone(6, TimeUnit.SECONDS).assertValueCount(msgSet.size()).assertValueSet(msgSet);
            //test4.awaitDone(4, TimeUnit.SECONDS).assertValueCount(msgSet.size()).assertValueSet(msgSet);
            //test5.awaitDone(4, TimeUnit.SECONDS).assertValueCount(msgSet.size()).assertValueSet(msgSet);

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
            TestObserver<String> test1 = sockets.get(0).upPipe.map(x -> x.getContent().toString()).take(msgSet.size())
                    .test();
            TestObserver<String> test2 = sockets.get(1).upPipe.map(x -> x.getContent().toString()).take(msgSet.size())
                    .test();
            TestObserver<String> test3 = sockets.get(2).upPipe.map(x -> x.getContent().toString()).take(msgSet.size())
                    .test();
            TestObserver<String> test4 = sockets.get(3).upPipe.map(x -> x.getContent().toString()).take(msgSet.size())
                    .test();
            TestObserver<String> test5 = sockets.get(4).upPipe.map(x -> x.getContent().toString()).take(msgSet.size())
                    .test();

            // Close socket 1 at the start (for more challenge)
            closables.get(1).close();
            Observable.just(closables.get(1)).delay(225, TimeUnit.MILLISECONDS).subscribe(RxClosableSocket::open);

            // Close sockets after some time
            Observable.just(closables.get(2)).delay(300, TimeUnit.MILLISECONDS).subscribe(RxClosableSocket::close);
            Observable.just(closables.get(3)).delay(500, TimeUnit.MILLISECONDS).subscribe(RxClosableSocket::close);
            Observable.just(closables.get(4)).delay(700, TimeUnit.MILLISECONDS).subscribe(RxClosableSocket::close);

            Observable.interval(200, TimeUnit.MILLISECONDS).zipWith(contents, (a, b) -> b)
                    .map(c -> new DAPacket(addrs.get(0), c)).forEach(sockets.get(0).downPipe::onNext);

            test1.awaitDone(2, TimeUnit.SECONDS).assertValueCount(3);
            test2.assertValueCount(3);
            test3.assertValueCount(1);
            test4.assertValueCount(2);
            test5.assertValueCount(3);

        } catch (Exception e) {
            fail("exception: " + e.toString());
        }
    }

    @Test
    public void advancedTest() throws InterruptedException {
        setup(0, 0, 0);

        closables.forEach(RxClosableSocket::close);
        closables.get(3).open();
        closables.get(4).open();

        List<MessageContent> contents1 = IntStream.range(0, 77).mapToObj(x -> MessageContent.Message(x, 3))
                .collect(Collectors.toList());
        Set<String> msgSet1 = contents1.stream().map(MessageContent::toString).collect(Collectors.toSet());


        List<MessageContent> contents2 = IntStream.range(0, 113).mapToObj(x -> MessageContent.Message(x, 4))
                .collect(Collectors.toList());
        Set<String> msgSet2 = contents2.stream().map(MessageContent::toString).collect(Collectors.toSet());


        TestObserver<String> test1 = sockets.get(0).upPipe
                .map(x -> x.getContent().toString())
                .take(msgSet1.size() + msgSet2.size())
                .test();
        TestObserver<String> test2 = sockets.get(1).upPipe
                .map(x -> x.getContent().toString())
                .take(msgSet1.size() + msgSet2.size())
                .test();
        TestObserver<String> test3 = sockets.get(2).upPipe
                .map(x -> x.getContent().toString())
                .take(msgSet1.size() + msgSet2.size())
                .test();
        TestObserver<String> test4 = sockets.get(3).upPipe
                .map(x -> x.getContent().toString())
                .take(msgSet1.size() + msgSet2.size())
                .test();
        TestObserver<String> test5 = sockets.get(4).upPipe
                .map(x -> x.getContent().toString())
                .take(msgSet1.size() + msgSet2.size())
                .test();

        Thread.sleep(500);

        Observable.interval(5, TimeUnit.MILLISECONDS).zipWith(contents1, (a, b) -> b)
                .map(c -> new DAPacket(null, c)).forEach(sockets.get(2).downPipe::onNext);


        Observable.interval(5, TimeUnit.MILLISECONDS).zipWith(contents2, (a, b) -> b)
                .map(c -> new DAPacket(null, c)).forEach(sockets.get(3).downPipe::onNext);

        Thread.sleep(3000);

        test1.assertValueCount(0);
        test2.assertValueCount(0);
        test3.assertValueCount(0);
        test4.assertValueCount(0);
        test5.assertValueCount(0);

        closables.get(4).close();
        Thread.sleep(500);
        closables.get(2).open();

        Thread.sleep(3000);

        test1.assertValueCount(0);
        test2.assertValueCount(0);
        test3.assertValueCount(0);
        test4.assertValueCount(msgSet2.size()).assertValueSet(msgSet2);
        test5.assertValueCount(0);

        Thread.sleep(3000);

        closables.get(1).open();

        Thread.sleep(3000);
        Set<String> totalSet = Stream.concat(msgSet1.stream(), msgSet2.stream()).collect(Collectors.toSet());

        test1.assertValueCount(0);
        test2.assertValueCount(totalSet.size()).assertValueSet(totalSet);
        test3.assertValueCount(totalSet.size()).assertValueSet(totalSet);
        test4.assertValueCount(totalSet.size()).assertValueSet(totalSet);
        test5.assertValueCount(0);

    }
}