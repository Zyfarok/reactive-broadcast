package ch.epfl.daeasy.layers;

import ch.epfl.daeasy.protocol.DAPacket;
import ch.epfl.daeasy.protocol.DatagramPacketConverter;
import ch.epfl.daeasy.protocol.MessageContent;
import ch.epfl.daeasy.rxlayers.RxLayer;
import ch.epfl.daeasy.rxlayers.RxNil;
import ch.epfl.daeasy.rxsockets.RxBadRouter;
import ch.epfl.daeasy.rxsockets.RxSocket;
import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;
import org.junit.Test;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PerfectLinkLayerTest {
    private final DatagramPacketConverter daConverter = new DatagramPacketConverter();
    private final RxLayer<DatagramPacket,DAPacket> layers = new RxNil<DatagramPacket>()
            .scheduleOn(Schedulers.trampoline())
            .convertPipes(daConverter)
            .stack(new PerfectLinkLayer());
    @Test
    public void perfectLinkEndsUpSendingPackets() {
        RxBadRouter router = new RxBadRouter(0.2,0.0,50, TimeUnit.MILLISECONDS);

        SocketAddress address1 = new InetSocketAddress("127.0.0.1",1000);
        SocketAddress address2 = new InetSocketAddress("127.0.0.1",1001);
        RxSocket<DAPacket> socket1 = router.buildSocket(address1).stack(layers);

        RxSocket<DAPacket> socket2 = router.buildSocket(address2).stack(layers);


        List<MessageContent> contents = IntStream.range(0,100)
                .mapToObj(x -> MessageContent.Message(x, 100+x)).collect(Collectors.toList());
        Set<String> msgSet = contents.stream().map(MessageContent::toString).collect(Collectors.toSet());

        // Create TestObservers
        TestObserver<String> test = socket2.upPipe
                .map(x -> x.getContent().toString()).take(msgSet.size()).test();

        //socket2.upPipe.forEach(x -> System.out.println(x.getContent().toString()));
        Observable.interval(10, TimeUnit.MILLISECONDS)
                .zipWith(contents, (a, b) -> b)
                .map(c -> new DAPacket(address2,c))
                .forEach(socket1.downPipe::onNext);

        test.awaitDone(10, TimeUnit.SECONDS)
                .assertValueCount(msgSet.size())
                .assertValueSet(msgSet);
    }

}