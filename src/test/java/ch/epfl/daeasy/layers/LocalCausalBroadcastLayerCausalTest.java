package ch.epfl.daeasy.layers;

import static org.junit.Assert.fail;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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


public class LocalCausalBroadcastLayerCausalTest {
    private static List<RxSocket<MessageContent>> sockets;
    private static List<RxSocket<CausalMessageContent>> urbSockets;
    private static List<RxClosableSocket<DatagramPacket>> closables;
    private static List<SocketAddress> addrs;

    private void setup(double dropRate) {
        RxBadRouter router = new RxBadRouter(dropRate);

        List<LCBConfiguration> cfgs = new ArrayList<>();
        List<SocketAddress> addrs = new ArrayList<>();
        List<RxSocket<MessageContent>> sockets = new ArrayList<>();
        List<RxSocket<CausalMessageContent>> urbSockets = new ArrayList<>();
        List<RxClosableSocket<DatagramPacket>> closables = new ArrayList<>();

        // 3 processes
        // dependencies: /

        try {
            for (int i = 0; i < 3; i++) {
                cfgs.add(new LCBConfiguration(i + 1, "test/membership_LCB_3p_causal_test.txt", 1));
                addrs.add(new InetSocketAddress("127.0.0.1", 10001 + i));

                RxLayer<DAPacket<CausalMessageContent>, DAPacket<CausalMessageContent>> perfectLinkLayer = new PerfectLinkLayer<>(
                        CausalMessageContent::toAck);
                if(i == 0) {
                    // p1 does not receive mc from p2
                    perfectLinkLayer = perfectLinkLayer.filterUp(pkt -> pkt.content.pid != 2);
                } else if(i == 1) {
                    // p2 does not receive mc from p3
                    perfectLinkLayer = perfectLinkLayer.filterUp(pkt -> pkt.content.pid != 3);
                }
                final CausalDatagramPacketConverter daConverter = new CausalDatagramPacketConverter();
                final RxLayer<DatagramPacket, DAPacket<CausalMessageContent>> perfectLinks = new RxNil<DatagramPacket>()
                        .convertPipes(daConverter).stack(perfectLinkLayer);
                final RxLayer<DatagramPacket, DAPacket<CausalMessageContent>> beb = perfectLinks
                        .stack(new BestEffortBroadcastLayer<>(cfgs.get(i)));

                final RxLayer<DatagramPacket, CausalMessageContent> urb = beb
                        .stack(new UniformReliableBroadcastLayer<>(cfgs.get(i)));
                final RxLayer<CausalMessageContent, MessageContent> lcb = new LocalizedCausalBroadcastLayer(cfgs.get(i));

                final RxClosableSocket<DatagramPacket> closable = router.buildSocket(addrs.get(i)).toClosable();
                final RxSocket<CausalMessageContent> urbSocket = closable.stack(urb);

                sockets.add(urbSocket.stack(lcb));
                urbSockets.add(urbSocket);
                closables.add(closable);

            }

            LocalCausalBroadcastLayerCausalTest.sockets = sockets;
            LocalCausalBroadcastLayerCausalTest.closables = closables;
            LocalCausalBroadcastLayerCausalTest.addrs = addrs;
            LocalCausalBroadcastLayerCausalTest.urbSockets = urbSockets;

        } catch (Exception e) {
            fail("exception: " + e.toString());
        }
    }

    @Test
    public void testCausal() {
        setup(0.4);
        //setup(0, 0, 0);

        try {
            Observable<MessageContent> upPipe1 = sockets.get(0).upPipe.share();
            Observable<MessageContent> upPipe2 = sockets.get(1).upPipe.share();
            Observable<MessageContent> upPipe3 = sockets.get(2).upPipe.share();

            // Message counts
            int count1 = 100;
            int count2 = (count1 / 5);
            int count3 = count2;

            // Prepare messages
            List<MessageContent> contents1 = IntStream.range(1, count1 + 1).mapToObj(x -> MessageContent.createMessage(1, x))
                    .collect(Collectors.toList());

            List<MessageContent> contents2 = IntStream.range(1, count2 + 1).mapToObj(x -> MessageContent.createMessage(2, x))
                    .collect(Collectors.toList());

            List<MessageContent> contents3 = IntStream.range(1, count3 + 1).mapToObj(x -> MessageContent.createMessage(3, x))
                    .collect(Collectors.toList());

            // Desired outputs
            List<String> contents1Strings = contents1.stream().map(MessageContent::toString)
                    .collect(Collectors.toList());

            List<String> contents2Strings = contents2.stream().map(MessageContent::toString)
                    .collect(Collectors.toList());

            List<String> contents1And2Strings = Stream.concat(contents1.stream(), contents2.stream())
                    .map(MessageContent::toString).collect(Collectors.toList());

            List<String> contents3Strings = contents3.stream().map(MessageContent::toString)
                    .collect(Collectors.toList());


            // p2 sends one message every 5 message from p1
            upPipe2.filter(mc -> mc.pid == 1).filter(mc -> (mc.seq % 5) == 0).map(mc ->
                    contents2.get((int) (mc.seq / 5) - 1)
            ).subscribe(sockets.get(1).downPipe);

            // p3 sends one message for every message from p2
            upPipe3.filter(mc -> mc.pid == 2).map(mc ->
                    contents3.get((int) mc.seq - 1)
            ).subscribe(sockets.get(2).downPipe);


            // Create first phase TestObservers
            TestObserver<String> test1 = upPipe1.map(MessageContent::toString).take(count1).test();
            TestObserver<String> test2 = upPipe2.map(MessageContent::toString).take(count1).test();
            TestObserver<String> test3 = upPipe3.map(MessageContent::toString).take(1).test();


            // ENTERING FIRST PHASE
            // close p3 for now
            closables.get(2).close();
            Thread.sleep(500);


            // p1 starts sending
            Observable.interval(10L, TimeUnit.MILLISECONDS)
                    .zipWith(contents1, (i, m) -> m)
                    .subscribe(sockets.get(0).downPipe);

            // p1 received it's messages (but not the one from p2 (see setup))
            test1.awaitDone(2000, TimeUnit.MILLISECONDS).assertComplete()
                    .assertValueSequence(contents1Strings);
            // same (could not deliver p2 messages yet because of URB, since p1 does not receive p2 messages)
            test2.awaitDone(500, TimeUnit.MILLISECONDS).assertComplete()
                    .assertValueSequence(contents1Strings);
            // p3 still closed
            test3.awaitDone(100, TimeUnit.MILLISECONDS).assertNotComplete().assertValueCount(0);


            // Prepare second phase TestObservers
            TestObserver<String> test1Bis = upPipe1.map(MessageContent::toString).take(1).test();
            TestObserver<String> test2Bis = upPipe2.map(MessageContent::toString).take(count2).test();
            TestObserver<String> test3Bis = upPipe3.map(MessageContent::toString).take(count1 + count2).test();
            TestObserver<MessageContent> test3BisContent = upPipe3.take(count1 + count2).test();

            // ENTERING SECOND PHASE
            // Close p1, open p3
            closables.get(0).close();
            closables.get(2).open();


            // p2 can deliver it's messages (URB majority reached with p3)
            test2Bis.awaitDone(2000, TimeUnit.MILLISECONDS).assertComplete()
                    .assertValueSequence(contents2Strings);
            // p3 delivers every p1 and p2 messages
            test3Bis.awaitDone(500, TimeUnit.MILLISECONDS).assertComplete()
                    .assertValueSet(contents1And2Strings);

            // p1 cannot receive any more messages (because it did not receive p2 messages)
            test1Bis.awaitDone(100, TimeUnit.MILLISECONDS).assertNotComplete();

            {
                // Verify causality in p3's delivery order
                int m1Count = 0;
                int m2Count = 0;
                for(MessageContent mc : test3BisContent.values()) {
                    if(mc.pid == 1) {
                        assert m1Count == mc.seq - 1;
                        ++m1Count;
                    } else if (mc.pid == 2) {
                        //System.out.println(mc.seq + "->" + m1Count);
                        assert m2Count == mc.seq - 1 && m1Count >= (mc.seq * 5);
                        ++m2Count;
                    }
                }
            }


            // Create third phase TestObservers
            TestObserver<String> test1Ter = test1Bis; // keep the same here (still nothing received)
            TestObserver<String> test2Ter = upPipe2.map(MessageContent::toString).take(1).test();
            TestObserver<String> test3Ter = upPipe3.map(MessageContent::toString).take(count3).test();

            // ENTERING THIRD PHASE
            // Reopen p1
            closables.get(0).open();

            // p3 finally also delivers his messages (URB majority reached with p1)
            test3Ter.awaitDone(2000, TimeUnit.MILLISECONDS).assertComplete()
                    .assertValueSet(contents3Strings);

            // p1 cannot receive any more messages (because it did not receive p2 messages)
            test1Ter.awaitDone(100, TimeUnit.MILLISECONDS).assertNotComplete();
            // p2 cannot receive any mc from p3 (see setup)
            test2Ter.awaitDone(50, TimeUnit.MILLISECONDS).assertNotComplete();


        } catch (Exception e) {
            e.printStackTrace();
            fail(e.toString());
        }

    }
}