package ch.epfl.daeasy.protocol;

import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class DatagramPacketConverterTest {

    private DatagramPacketConverter converter = new DatagramPacketConverter();

    @Test
    public void identityTest() {
        // test that converting a DAPacket to a DatagramPacket and then back to a
        // DAPacket is identity
        SocketAddress peer = new InetSocketAddress("127.0.0.1", 10000);
        MessageContent content = MessageContent.createMessage(1, 42);
        DAPacket<MessageContent> msg = new DAPacket<>(peer, content);
        Observable<DAPacket<MessageContent>> msgs = Observable.fromArray(msg);
        TestObserver<DAPacket<MessageContent>> msgs2 = converter.doForward(converter.doBackward(msgs)).test();
        msgs2.assertResult(msg);
    }
}