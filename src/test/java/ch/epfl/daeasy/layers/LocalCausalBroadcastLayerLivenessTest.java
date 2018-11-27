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

import ch.epfl.daeasy.protocol.*;
import org.junit.Test;

import ch.epfl.daeasy.config.Configuration;
import ch.epfl.daeasy.config.LCBConfiguration;
import ch.epfl.daeasy.rxlayers.RxFilterLayer;
import ch.epfl.daeasy.rxlayers.RxLayer;
import ch.epfl.daeasy.rxlayers.RxNil;
import ch.epfl.daeasy.rxsockets.RxBadRouter;
import ch.epfl.daeasy.rxsockets.RxClosableSocket;
import ch.epfl.daeasy.rxsockets.RxSocket;
import io.reactivex.observers.TestObserver;

public class LocalCausalBroadcastLayerLivenessTest {

    private static List<LCBConfiguration> cfgs;
    private static List<SocketAddress> addrs;
    private static List<RxSocket<MessageContent>> sockets;
    private static List<RxClosableSocket<DatagramPacket>> closables;

    public void setup(double dropRate, double loopRate, long delayStepMilliseconds) {
        RxBadRouter router = new RxBadRouter(dropRate, loopRate, delayStepMilliseconds, TimeUnit.MILLISECONDS);

        List<LCBConfiguration> cfgs = new ArrayList<>();
        List<SocketAddress> addrs = new ArrayList<>();
        List<RxSocket<MessageContent>> sockets = new ArrayList<>();
        List<RxClosableSocket<DatagramPacket>> closables = new ArrayList<>();

        // 3 processes
        // dependencies: /

        try {
            for (int i = 0; i < 3; i++) {
                cfgs.add(new LCBConfiguration(i + 1, "test/membership_LCB_3p_liveness_test.txt", 1));
                addrs.add(new InetSocketAddress("127.0.0.1", 10001 + i));

                RxLayer<DAPacket<CausalMessageContent>, DAPacket<CausalMessageContent>> perfectLinkLayer = new PerfectLinkLayer<>(CausalMessageContent::toAck);
                if (i == 0) {
                    // filter: p1 does not receive messages from p1 with pid = 3
                    // filter just above the perfect link : beb will never deliver those messages
                    perfectLinkLayer = perfectLinkLayer.stack(new RxFilterLayer<>(x -> true, x -> true)); // x.getContent().getPID()
                                                                                                                  // !=
                                                                                                                  // 3
                }
                final CausalDatagramPacketConverter daConverter = new CausalDatagramPacketConverter();
                final RxLayer<DatagramPacket, DAPacket<CausalMessageContent>> perfectLinks = new RxNil<DatagramPacket>()
                        .convertPipes(daConverter).stack(perfectLinkLayer);

                final RxLayer<DatagramPacket, DAPacket<CausalMessageContent>> beb = perfectLinks
                        .stack(new BestEffortBroadcastLayer<>(cfgs.get(i)));

                final RxLayer<DatagramPacket, CausalMessageContent> urb = beb.stack(new UniformReliableBroadcastLayer<>(cfgs.get(i)));
                final RxLayer<DatagramPacket, MessageContent> lcb = urb
                        .stack(new LocalizedCausalBroadcastLayer(cfgs.get(i)));

                final RxClosableSocket<DatagramPacket> closable = router.buildSocket(addrs.get(i)).toClosable();
                sockets.add(closable.stack(lcb));
                closables.add(closable);

            }

            LocalCausalBroadcastLayerLivenessTest.cfgs = cfgs;
            LocalCausalBroadcastLayerLivenessTest.addrs = addrs;
            LocalCausalBroadcastLayerLivenessTest.sockets = sockets;
            LocalCausalBroadcastLayerLivenessTest.closables = closables;

        } catch (Exception e) {
            fail("exception: " + e.toString());
        }
    }

    @Test
    public void testLiveness() {
        setup(0, 0, 0);

        int messageCount = 2;
        try {
            // Create TestObservers
            TestObserver<String> test1 = sockets.get(0).upPipe.map(MessageContent::toString).take(messageCount)
                    .test();
            TestObserver<String> test2 = sockets.get(1).upPipe.map(MessageContent::toString).take(messageCount)
                    .test();
            TestObserver<String> test3 = sockets.get(2).upPipe.map(MessageContent::toString).take(messageCount)
                    .test();

            MessageContent m1 = MessageContent.createMessage(3, 1);
            MessageContent m2 = MessageContent.createMessage(2, 1);

            Set<String> deliveredMessages = new HashSet<>();
            deliveredMessages.add(m2.toString());

            // p3 sends m1
            sockets.get(2).downPipe.onNext(m1);

            // p2 should receive m1
            Thread.sleep(500);

            // p2 sends m2
            sockets.get(1).downPipe.onNext(m2);

            // p3 and p1 should receive m2
            Thread.sleep(500);

            // since p1 does not receive m1, all processes should deliver m2 but not m1
            test1.awaitDone(5, TimeUnit.SECONDS).assertValueCount(1).assertValueSet(deliveredMessages);
            test2.awaitDone(5, TimeUnit.SECONDS).assertValueCount(1).assertValueSet(deliveredMessages);
            test3.awaitDone(5, TimeUnit.SECONDS).assertValueCount(1).assertValueSet(deliveredMessages);

        } catch (Exception e) {
            e.printStackTrace();
            fail("exception: " + e.toString());
        }
    }
}