package ch.epfl.daeasy.layers;

import static org.junit.Assert.fail;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
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

public class LocalCausalBroadcastLayerSafetyTest {

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
        // dependencies: p1 -> p2

        try {
            for (int i = 0; i < 3; i++) {
                cfgs.add(new LCBConfiguration(i + 1, "test/membership_LCB_3p_safety_test.txt", 1));
                addrs.add(new InetSocketAddress("127.0.0.1", 10001 + i));

                RxLayer<DAPacket, DAPacket> perfectLinkLayer = new PerfectLinkLayer();
                if (i == 2) {
                    // filter: p3 does not receive messages from p1 with pid = 1
                    // filter just above the perfect link : beb will never deliver those messages
                    perfectLinkLayer = perfectLinkLayer
                            .stack(new RxFilterLayer<DAPacket>(x -> x.getContent().getPID() != 1, x -> true));
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

            LocalCausalBroadcastLayerSafetyTest.cfgs = cfgs;
            LocalCausalBroadcastLayerSafetyTest.addrs = addrs;
            LocalCausalBroadcastLayerSafetyTest.sockets = sockets;
            LocalCausalBroadcastLayerSafetyTest.router = router;
            LocalCausalBroadcastLayerSafetyTest.closables = closables;

        } catch (Exception e) {
            fail("exception: " + e.toString());
        }
    }

    @Test
    public void testSafety() {
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

            // stop p3 for now
            closables.get(2).close();

            // p1 sends m1
            sockets.get(0).downPipe.onNext(new DAPacket(addrs.get(0), MessageContent.Message(1, 1)));

            // p2 should receive m1
            Thread.sleep(500);

            // stop p1
            closables.get(0).close();

            // p2 sends m2
            sockets.get(1).downPipe.onNext(new DAPacket(addrs.get(1), MessageContent.Message(1, 2)));

            Thread.sleep(500);

            // resume p3
            closables.get(2).open();

            // p3 should receive m2
            Thread.sleep(500);

            // resume p1
            closables.get(0).open();

            Thread.sleep(1000);

            // p3 should have received m2 from p1 and p2, but m1 only from p2
            // thus since m1 -> m2, p3 should not have delivered anything
            // p2 should have delivered no messages as well (m1 -> m2 and p3 does not ack
            // m1)
            // p1 should not have delivered any messages since m1 -> n2 and p3 does not ack
            // m1
            test1.awaitDone(5, TimeUnit.SECONDS).assertValueCount(0);
            test2.awaitDone(5, TimeUnit.SECONDS).assertValueCount(0);
            test3.awaitDone(5, TimeUnit.SECONDS).assertValueCount(0);

        } catch (Exception e) {
            e.printStackTrace();
            fail("exception: " + e.toString());
        }
    }
}