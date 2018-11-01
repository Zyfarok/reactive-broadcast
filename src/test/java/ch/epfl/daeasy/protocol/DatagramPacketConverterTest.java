package ch.epfl.daeasy.protocol;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.junit.Test;

import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;

public class DatagramPacketConverterTest {

    DatagramPacketConverter converter = new DatagramPacketConverter();

    @Test
    public void identityTest() {
        // test that converting a DAPacket to a DatagramPacket and then back to a
        // DAPacket is identity
        SocketAddress peer = new InetSocketAddress("localhost", 10000);
        MessageContent content = MessageContent.Message(1, 42);
        DAPacket msg = new DAPacket(peer, content);
        Observable<DAPacket> msgs = Observable.fromArray(new DAPacket[] { msg });
        TestObserver<DAPacket> msgs2 = converter.doForward(converter.doBackward(msgs)).test();
        msgs2.assertResult(msg);
    }
}