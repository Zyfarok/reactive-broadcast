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

import org.junit.Test;

import ch.epfl.daeasy.config.Configuration;
import ch.epfl.daeasy.config.LCBConfiguration;
import ch.epfl.daeasy.protocol.DAPacket;
import ch.epfl.daeasy.protocol.DatagramPacketConverter;
import ch.epfl.daeasy.protocol.MessageContent;
import ch.epfl.daeasy.rxlayers.RxFilterLayer;
import ch.epfl.daeasy.rxlayers.RxLayer;
import ch.epfl.daeasy.rxlayers.RxNil;
import ch.epfl.daeasy.rxsockets.RxBadRouter;
import ch.epfl.daeasy.rxsockets.RxClosableSocket;
import ch.epfl.daeasy.rxsockets.RxSocket;
import io.reactivex.observers.TestObserver;

public class LocalCausalBroadcastLayerLivenessTest {

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

        // 3 processes
        // dependencies: /

        try {
            for (int i = 0; i < 3; i++) {
                cfgs.add(new LCBConfiguration(i + 1, "test/membership_LCB_3p_liveness_test.txt", 1));
                addrs.add(new InetSocketAddress("127.0.0.1", 10001 + i));

                RxLayer<DAPacket, DAPacket> perfectLinkLayer = new PerfectLinkLayer();
                if (i == 0) {
                    // filter: p1 does not receive messages from p1 with pid = 3
                    // filter just above the perfect link : beb will never deliver those messages
                    perfectLinkLayer = perfectLinkLayer.stack(new RxFilterLayer<DAPacket>(x -> true, x -> true)); // x.getContent().getPID()
                                                                                                                  // !=
                                                                                                                  // 3
                }
                final DatagramPacketConverter daConverter = new DatagramPacketConverter();
                final RxLayer<DatagramPacket, DAPacket> perfectLinks = new RxNil<DatagramPacket>()
                        .convertPipes(daConverter).stack(perfectLinkLayer);

                final RxLayer<DatagramPacket, DAPacket> beb = perfectLinks
                        .stack(new BestEffortBroadcastLayer(cfgs.get(i)));

                final RxLayer<DatagramPacket, DAPacket> urb = beb.stack(new UniformReliableBroadcastLayer(cfgs.get(i)));
                final RxLayer<DatagramPacket, DAPacket> fifo = urb
                        .stack(new FirstInFirstOutBroadcastLayer(cfgs.get(i)));

                // TODO: replace FIFO with LCB
                final RxClosableSocket<DatagramPacket> closable = router.buildSocket(addrs.get(i)).toClosable();
                sockets.add(closable.stack(fifo));
                closables.add(closable);

            }

            LocalCausalBroadcastLayerLivenessTest.cfgs = cfgs;
            LocalCausalBroadcastLayerLivenessTest.addrs = addrs;
            LocalCausalBroadcastLayerLivenessTest.sockets = sockets;
            LocalCausalBroadcastLayerLivenessTest.router = router;
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
            TestObserver<String> test1 = sockets.get(0).upPipe.map(x -> x.getContent().toString()).take(messageCount)
                    .test();
            TestObserver<String> test2 = sockets.get(1).upPipe.map(x -> x.getContent().toString()).take(messageCount)
                    .test();
            TestObserver<String> test3 = sockets.get(2).upPipe.map(x -> x.getContent().toString()).take(messageCount)
                    .test();

            MessageContent m1 = MessageContent.Message(1, 3);
            MessageContent m2 = MessageContent.Message(1, 2);

            Set<String> deliveredMessages = new HashSet<>();
            deliveredMessages.add(m2.toString());

            // p3 sends m1
            sockets.get(2).downPipe.onNext(new DAPacket(addrs.get(2), m1));

            // p2 should receive m1
            Thread.sleep(500);

            // p2 sends m2
            sockets.get(1).downPipe.onNext(new DAPacket(addrs.get(1), m2));

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