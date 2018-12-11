package ch.epfl.daeasy.layers;

import static org.junit.Assert.fail;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.Test;

import ch.epfl.daeasy.config.LCBConfiguration;
import ch.epfl.daeasy.protocol.CausalDatagramPacketConverter;
import ch.epfl.daeasy.protocol.CausalMessageContent;
import ch.epfl.daeasy.protocol.DAPacket;
import ch.epfl.daeasy.protocol.MessageContent;
import ch.epfl.daeasy.rxlayers.RxLayer;
import ch.epfl.daeasy.rxlayers.RxNil;
import ch.epfl.daeasy.rxsockets.RxBadRouter;
import ch.epfl.daeasy.rxsockets.RxClosableSocket;
import ch.epfl.daeasy.rxsockets.RxSocket;
import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;

public class LocalCausalBroadcastLayerFIFOTest {
    // Tests to verify that a LCB with no dependencies behaves like a FIFOB

    private static List<RxSocket<MessageContent>> sockets;
    private static List<RxClosableSocket<DatagramPacket>> closables;

    private void setup(double dropRate, double loopRate, long delayStepMilliseconds) {
        RxBadRouter router = new RxBadRouter(dropRate, loopRate, delayStepMilliseconds, TimeUnit.MILLISECONDS);

        List<LCBConfiguration> cfgs = new ArrayList<>();
        List<SocketAddress> addrs = new ArrayList<>();
        List<RxSocket<MessageContent>> sockets = new ArrayList<>();
        List<RxClosableSocket<DatagramPacket>> closables = new ArrayList<>();

        try {
            for (int i = 0; i < 5; i++) {
                cfgs.add(new LCBConfiguration(i + 1, "test/membership_LCB_5p_liveness_test.txt", 1));
                addrs.add(new InetSocketAddress("127.0.0.1", 10001 + i));

                RxLayer<DAPacket<CausalMessageContent>, DAPacket<CausalMessageContent>> perfectLinkLayer = new PerfectLinkLayer<>(
                        CausalMessageContent::toAck);

                final CausalDatagramPacketConverter daConverter = new CausalDatagramPacketConverter();
                final RxLayer<DatagramPacket, DAPacket<CausalMessageContent>> perfectLinks = new RxNil<DatagramPacket>()
                        .convertPipes(daConverter)
                        // .stack(RxGroupedLayer.create(x -> x.getPeer().toString(), perfectLinkLayer));
                        .stack(perfectLinkLayer);

                final RxLayer<DatagramPacket, MessageContent> lcb_fifo = perfectLinks
                        .stack(new BestEffortBroadcastLayer<>(cfgs.get(i)))
                        .stack(new UniformReliableBroadcastLayer<>(cfgs.get(i)))
                        .stack(new LocalizedCausalBroadcastLayer(cfgs.get(i)));

                final RxClosableSocket<DatagramPacket> closable = RxClosableSocket
                        .from(router.buildSocket(addrs.get(i)));
                closables.add(closable);
                sockets.add(closable.stack(lcb_fifo));
            }
            LocalCausalBroadcastLayerFIFOTest.sockets = sockets;
            LocalCausalBroadcastLayerFIFOTest.closables = closables;

        } catch (Exception e) {
            fail("exception: " + e.toString());
        }
    }

    @Test
    public void fifoOneProducer() {
        setup(0.1, 0.5, 1005);
        // P1 sends 10 message, with a high probability the packets will be delayed and
        // be received unordered
        // Other processes should have received each packet in order
        try {

            List<MessageContent> contents = IntStream.range(1, 101).mapToObj(x -> MessageContent.createMessage(1, x))
                    .collect(Collectors.toList());
            List<String> msgList = contents.stream().map(MessageContent::toString).collect(Collectors.toList());

            // Create TestObservers
            TestObserver<String> test2 = sockets.get(1).upPipe.map(MessageContent::toString).take(msgList.size())
                    .test();
            TestObserver<String> test3 = sockets.get(2).upPipe.map(MessageContent::toString).take(msgList.size())
                    .test();

            Observable.interval(1, TimeUnit.MILLISECONDS).zipWith(contents, (a, b) -> b)
                    .forEach(sockets.get(0).downPipe::onNext);

            test2.awaitDone(10, TimeUnit.SECONDS).assertValueCount(msgList.size()).assertValueSequence(msgList);
            test3.awaitDone(10, TimeUnit.SECONDS).assertValueCount(msgList.size()).assertValueSequence(msgList);

        } catch (Exception e) {
            fail("exception: " + e.toString());
        }
    }

    @Test
    public void fifoTwoProducers() {
        setup(0.1, 0.1, 505);
        // P1 sends 30 messages, with a high probability the packets will be delayed
        // and received unordered
        // P2 does the same with 60 messages
        // Other processes should have received each packet in order
        try {

            List<MessageContent> contents1 = IntStream.range(1, 21).mapToObj(x -> MessageContent.createMessage(1, x))
                    .collect(Collectors.toList());
            List<String> msgList1 = contents1.stream().map(MessageContent::toString).collect(Collectors.toList());
            List<MessageContent> contents2 = IntStream.range(1, 61).mapToObj(x -> MessageContent.createMessage(2, x))
                    .collect(Collectors.toList());
            List<String> msgList2 = contents2.stream().map(MessageContent::toString).collect(Collectors.toList());

            Observable<MessageContent> p5socketTest = sockets.get(4).upPipe.share();

            // Create TestObservers
            TestObserver<String> testfromP1 = p5socketTest.filter(x -> x.pid == 1).map(MessageContent::toString)
                    .take(msgList1.size()).test();
            TestObserver<String> testfromP2 = p5socketTest.filter(x -> x.pid == 2).map(MessageContent::toString)
                    .take(msgList2.size()).test();

            Observable.interval(10, TimeUnit.MILLISECONDS).zipWith(contents1, (a, b) -> b)
                    .subscribe(sockets.get(0).downPipe);
            Observable.interval(10, TimeUnit.MILLISECONDS).zipWith(contents2, (a, b) -> b)
                    .subscribe(sockets.get(1).downPipe);

            // P5 should have received every message from P1 in order
            testfromP1.awaitDone(10, TimeUnit.SECONDS).assertValueCount(msgList1.size()).assertValueSequence(msgList1);
            // P5 should have received every message from P2 in order
            testfromP2.awaitDone(10, TimeUnit.SECONDS).assertValueCount(msgList2.size()).assertValueSequence(msgList2);
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

        List<MessageContent> contents1 = IntStream.range(1, 27).mapToObj(x -> MessageContent.createMessage(3, x))
                .collect(Collectors.toList());
        List<String> msgList1 = contents1.stream().map(MessageContent::toString).collect(Collectors.toList());

        List<MessageContent> contents2 = IntStream.range(1, 53).mapToObj(x -> MessageContent.createMessage(4, x))
                .collect(Collectors.toList());
        List<String> msgList2 = contents2.stream().map(MessageContent::toString).collect(Collectors.toList());

        TestObserver<String> test1 = sockets.get(0).upPipe.map(MessageContent::toString)
                .take(msgList1.size() + msgList2.size()).test();
        TestObserver<String> test2 = sockets.get(1).upPipe.map(MessageContent::toString)
                .take(msgList1.size() + msgList2.size()).test();
        TestObserver<String> test3 = sockets.get(2).upPipe.map(MessageContent::toString)
                .take(msgList1.size() + msgList2.size()).test();
        TestObserver<String> test4 = sockets.get(3).upPipe.map(MessageContent::toString)
                .take(msgList1.size() + msgList2.size()).test();
        TestObserver<String> test5 = sockets.get(4).upPipe.map(MessageContent::toString)
                .take(msgList1.size() + msgList2.size()).test();

        Thread.sleep(500);

        Observable.interval(5, TimeUnit.MILLISECONDS).zipWith(contents1, (a, b) -> b)
                .forEach(sockets.get(2).downPipe::onNext);

        Observable.interval(5, TimeUnit.MILLISECONDS).zipWith(contents2, (a, b) -> b)
                .forEach(sockets.get(3).downPipe::onNext);

        Thread.sleep(3000);

        test1.assertValueCount(0);
        test2.assertValueCount(0);
        test3.assertValueCount(0);
        test4.assertValueCount(0);
        test5.assertValueCount(0);

        closables.get(4).close();
        Thread.sleep(500);
        closables.get(2).open();

        Thread.sleep(6000);

        test1.assertValueCount(0);
        test2.assertValueCount(0);
        test3.assertValueCount(0);
        test4.assertValueCount(msgList2.size()).assertValueSequence(msgList2);
        test5.assertValueCount(0);

        Thread.sleep(3000);

        closables.get(1).open();

        Thread.sleep(20000);
        List<String> totalList = Stream.concat(msgList2.stream(), msgList1.stream()).collect(Collectors.toList());

        test1.assertValueCount(0);
        test2.assertValueCount(totalList.size()).assertValueSet(totalList);
        test3.assertValueCount(totalList.size()).assertValueSet(totalList);
        test4.assertValueCount(totalList.size()).assertValueSequence(totalList);
        test5.assertValueCount(0);

    }
}