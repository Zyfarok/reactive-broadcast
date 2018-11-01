package ch.epfl.daeasy.layers;

import ch.epfl.daeasy.protocol.DAPacket;
import ch.epfl.daeasy.protocol.DatagramPacketConverter;
import ch.epfl.daeasy.protocol.MessageContent;
import ch.epfl.daeasy.rxlayers.RxLayer;
import ch.epfl.daeasy.rxlayers.RxNil;
import ch.epfl.daeasy.rxsockets.RxBadRouter;
import ch.epfl.daeasy.rxsockets.RxSocket;
import io.reactivex.observers.TestObserver;
import org.junit.Test;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PerfectLinkLayerTest {
    private final DatagramPacketConverter daConverter = new DatagramPacketConverter();
    private final RxLayer<DatagramPacket,DAPacket> layers = new RxNil<DatagramPacket>()
            .convertPipes(daConverter).stack(new PerfectLinkLayer());
    @Test
    public void perfectLinkEndsUpSendingPackets() {
        RxBadRouter router = new RxBadRouter(0.9,0.9,0, TimeUnit.NANOSECONDS);

        SocketAddress address1 = new InetSocketAddress("127.0.0.1",1000);
        SocketAddress address2 = new InetSocketAddress("127.0.0.1",1001);
        RxSocket<DAPacket> socket1 = router.buildSocket(address1).stack(layers);
        RxSocket<DAPacket> socket2 = router.buildSocket(address2).stack(layers);

        // Create TestObservers
        TestObserver<String> test = socket2.upPipe
                .map(x -> x.getContent().toString()).take(3).test();

        MessageContent msg1 = MessageContent.Message(12,12);
        MessageContent msg2 = MessageContent.Message(13,17);
        MessageContent msg3 = MessageContent.Message(14,128);
        Set<String> messages = Stream.of(msg1, msg2, msg3).map(MessageContent::toString).collect(Collectors.toSet());

        socket1.downPipe.onNext(new DAPacket(address2, msg1));
        socket1.downPipe.onNext(new DAPacket(address2, msg2));
        socket1.downPipe.onNext(new DAPacket(address2, msg3));

        test.awaitDone(60, TimeUnit.SECONDS)
                .assertComplete()
                .assertValueCount(3)
                .assertValueSet(messages);
    }

}