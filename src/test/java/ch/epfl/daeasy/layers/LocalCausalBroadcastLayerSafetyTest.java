package ch.epfl.daeasy.layers;

import static org.junit.Assert.fail;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import ch.epfl.daeasy.config.LCBConfiguration;
import ch.epfl.daeasy.protocol.CausalDatagramPacketConverter;
import ch.epfl.daeasy.protocol.CausalMessageContent;
import ch.epfl.daeasy.protocol.DAPacket;
import ch.epfl.daeasy.protocol.MessageContent;
import ch.epfl.daeasy.rxlayers.RxFilterLayer;
import ch.epfl.daeasy.rxlayers.RxLayer;
import ch.epfl.daeasy.rxlayers.RxNil;
import ch.epfl.daeasy.rxsockets.RxBadRouter;
import ch.epfl.daeasy.rxsockets.RxClosableSocket;
import ch.epfl.daeasy.rxsockets.RxSocket;
import io.reactivex.observers.TestObserver;
import io.reactivex.Observable;

public class LocalCausalBroadcastLayerSafetyTest {

    private static List<LCBConfiguration> cfgs;
    private static List<SocketAddress> addrs;
    private static List<RxSocket<MessageContent>> sockets;
    private static List<RxClosableSocket<DatagramPacket>> closables;
    private static RxBadRouter router;

    private void setup(double dropRate) {
        RxBadRouter router = new RxBadRouter(dropRate);

        List<LCBConfiguration> cfgs = new ArrayList<>();
        List<SocketAddress> addrs = new ArrayList<>();
        List<RxSocket<MessageContent>> sockets = new ArrayList<>();
        List<RxClosableSocket<DatagramPacket>> closables = new ArrayList<>();

        // 3 processes
        // dependencies: p1 -> p2

        try {
            for (int i = 0; i < 3; i++) {
                cfgs.add(new LCBConfiguration(i + 1, "test/membership_LCB_3p_safety_test.txt", 1));
                addrs.add(new InetSocketAddress("127.0.0.1", 10001 + i));

                RxLayer<DAPacket<CausalMessageContent>, DAPacket<CausalMessageContent>> perfectLinkLayer = new PerfectLinkLayer<>(
                        CausalMessageContent::toAck);
                if (i == 2) {
                    // filter: p3 does not receive messages with pid = 1
                    // filter just above the perfect link : beb will never deliver those messages
                    perfectLinkLayer = perfectLinkLayer.stack(new RxFilterLayer<>(x -> x.content.pid != 1, x -> true));
                }
                final CausalDatagramPacketConverter daConverter = new CausalDatagramPacketConverter();
                final RxLayer<DatagramPacket, DAPacket<CausalMessageContent>> perfectLinks = new RxNil<DatagramPacket>()
                        .convertPipes(daConverter).stack(perfectLinkLayer);

                final RxLayer<DatagramPacket, DAPacket<CausalMessageContent>> beb = perfectLinks
                        .stack(new BestEffortBroadcastLayer<>(cfgs.get(i)));

                final RxLayer<DatagramPacket, CausalMessageContent> urb = beb
                        .stack(new UniformReliableBroadcastLayer<>(cfgs.get(i)));
                final RxLayer<DatagramPacket, MessageContent> lcb = urb
                        .stack(new LocalizedCausalBroadcastLayer(cfgs.get(i)));

                final RxClosableSocket<DatagramPacket> closable = router.buildSocket(addrs.get(i)).toClosable();

                sockets.add(closable.stack(lcb));
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
        setup(0);

        int messageCount = 2;
        try {

            Observable<MessageContent> upPipe1 = sockets.get(0).upPipe.share();
            Observable<MessageContent> upPipe2 = sockets.get(1).upPipe.share();
            Observable<MessageContent> upPipe3 = sockets.get(2).upPipe.share();

            // Create TestObservers
            TestObserver<String> test1 = upPipe1.map(MessageContent::toString).take(messageCount).test();
            TestObserver<String> test2 = upPipe2.map(MessageContent::toString).take(messageCount).test();
            TestObserver<String> test3 = upPipe3.map(MessageContent::toString).take(messageCount).test();

            // stop p3 for now
            closables.get(2).close();

            // p2 sends m2 once it delivered m1
            upPipe2.subscribe(x -> sockets.get(1).downPipe.onNext(MessageContent.createMessage(2, 1)));

            // p1 sends m1
            sockets.get(0).downPipe.onNext(MessageContent.createMessage(1, 1));

            // p2 should receive m1
            Thread.sleep(500);

            // stop p1
            closables.get(0).close();

            Thread.sleep(500);

            // resume p3
            closables.get(2).open();

            // p3 should receive m2
            Thread.sleep(500);

            // resume p1
            closables.get(0).open();

            Thread.sleep(1000);

            // p3 should have received m2 from p1 and p2, but not m1 from any p1 or p2
            // thus since m1 -> m2, p3 should not have delivered anything
            test3.awaitDone(5, TimeUnit.SECONDS).assertValueCount(0);

        } catch (Exception e) {
            e.printStackTrace();
            fail("exception: " + e.toString());
        }
    }
}