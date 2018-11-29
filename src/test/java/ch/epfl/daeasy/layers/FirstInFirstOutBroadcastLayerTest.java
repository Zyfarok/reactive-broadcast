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

public class FirstInFirstOutBroadcastLayerTest {

    private static List<RxSocket<MessageContent>> sockets;
    private static List<RxClosableSocket<DatagramPacket>> closables;

    private void setup(double dropRate, double loopRate, long delayStepMilliseconds) {
        RxBadRouter router = new RxBadRouter(dropRate, loopRate, delayStepMilliseconds, TimeUnit.MILLISECONDS);

        List<Configuration> cfgs = new ArrayList<>();
        List<SocketAddress> addrs = new ArrayList<>();
        List<RxSocket<MessageContent>> sockets = new ArrayList<>();
        List<RxClosableSocket<DatagramPacket>> closables = new ArrayList<>();

        try {
            for (int i = 0; i < 5; i++) {
                cfgs.add(new FIFOConfiguration(i + 1, "test/membership_FIFO_template.txt", 1));
                addrs.add(new InetSocketAddress("127.0.0.1", 10001 + i));

                RxLayer<DAPacket<MessageContent>, DAPacket<MessageContent>> perfectLinkLayer =
                        new PerfectLinkLayer<>(MessageContent::toAck);

                final DatagramPacketConverter daConverter = new DatagramPacketConverter();
                final RxLayer<DatagramPacket, DAPacket<MessageContent>> perfectLinks = new RxNil<DatagramPacket>()
                        .convertPipes(daConverter)
                        //.stack(RxGroupedLayer.create(x -> x.getPeer().toString(), perfectLinkLayer));
                        .stack(perfectLinkLayer);

                final RxLayer<DatagramPacket, MessageContent> fifo = perfectLinks
                        .stack(new BestEffortBroadcastLayer<>(cfgs.get(i)))
                        .stack(new UniformReliableBroadcastLayer<>(cfgs.get(i)))
                        .stack(new FirstInFirstOutBroadcastLayer<>(cfgs.get(i)));


                final RxClosableSocket<DatagramPacket> closable = RxClosableSocket
                        .from(router.buildSocket(addrs.get(i)));
                closables.add(closable);
                sockets.add(closable.stack(fifo));
            }
            FirstInFirstOutBroadcastLayerTest.sockets = sockets;
            FirstInFirstOutBroadcastLayerTest.closables = closables;

        } catch (Exception e) {
            fail("exception: " + e.toString());
        }
    }

    @Test
    public void fifoOneProducer() {
        setup(0.1,0.5,1005);
        // P1 sends 10 message, with a high probability the packets will be delayed and
        // be received unordered
        // Other processes should have received each packet in order
        try {

            List<MessageContent> contents = IntStream.range(1, 11).mapToObj(x -> MessageContent.createMessage(1, x))
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
        setup(0.1,0.5,1005);
        // P1 sends 10 message, with a high probability the packets will be delayed and
        // be received unordered
        // P2 does the same
        // Other processes should have received each packet in order
        try {

            List<MessageContent> contents1 = IntStream.range(1, 11).mapToObj(x -> MessageContent.createMessage(1, x))
                    .collect(Collectors.toList());
            List<String> msgList1 = contents1.stream().map(MessageContent::toString).collect(Collectors.toList());
            List<MessageContent> contents2 = IntStream.range(1, 21).mapToObj(x -> MessageContent.createMessage(2, x))
                    .collect(Collectors.toList());
            List<String> msgList2 = contents2.stream().map(MessageContent::toString).collect(Collectors.toList());

            Observable<MessageContent> p5socketTest = sockets.get(4).upPipe.share();

            // Create TestObservers
            TestObserver<String> testfromP1 = p5socketTest.filter(x -> x.pid == 1)
                    .map(MessageContent::toString).take(msgList1.size()).test();
            TestObserver<String> testfromP2 = p5socketTest.filter(x -> x.pid == 2)
                    .map(MessageContent::toString).take(msgList2.size()).test();

            Observable.interval(1, TimeUnit.MILLISECONDS).zipWith(contents1, (a, b) -> b)
                    .forEach(sockets.get(0).downPipe::onNext);
            Observable.interval(1, TimeUnit.MILLISECONDS).zipWith(contents2, (a, b) -> b)
                    .forEach(sockets.get(0).downPipe::onNext);

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

        List<MessageContent> contents1 = IntStream.range(1, 67).mapToObj(x -> MessageContent.createMessage(3, x))
                .collect(Collectors.toList());
        List<String> msgList1 = contents1.stream().map(MessageContent::toString).collect(Collectors.toList());


        List<MessageContent> contents2 = IntStream.range(1, 103).mapToObj(x -> MessageContent.createMessage(4, x))
                .collect(Collectors.toList());
        List<String> msgList2 = contents2.stream().map(MessageContent::toString).collect(Collectors.toList());


        TestObserver<String> test1 = sockets.get(0).upPipe
                .map(MessageContent::toString)
                .take(msgList1.size() + msgList2.size())
                .test();
        TestObserver<String> test2 = sockets.get(1).upPipe
                .map(MessageContent::toString)
                .take(msgList1.size() + msgList2.size())
                .test();
        TestObserver<String> test3 = sockets.get(2).upPipe
                .map(MessageContent::toString)
                .take(msgList1.size() + msgList2.size())
                .test();
        TestObserver<String> test4 = sockets.get(3).upPipe
                .map(MessageContent::toString)
                .take(msgList1.size() + msgList2.size())
                .test();
        TestObserver<String> test5 = sockets.get(4).upPipe
                .map(MessageContent::toString)
                .take(msgList1.size() + msgList2.size())
                .test();

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

        Thread.sleep(3000);
        List<String> totalList = Stream.concat(msgList2.stream(), msgList1.stream()).collect(Collectors.toList());

        test1.assertValueCount(0);
        test2.assertValueCount(totalList.size()).assertValueSet(totalList);
        test3.assertValueCount(totalList.size()).assertValueSet(totalList);
        test4.assertValueCount(totalList.size()).assertValueSequence(totalList);
        test5.assertValueCount(0);

    }
}