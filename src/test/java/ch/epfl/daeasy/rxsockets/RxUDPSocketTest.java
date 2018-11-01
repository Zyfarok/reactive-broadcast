package ch.epfl.daeasy.rxsockets;

import io.reactivex.observers.TestObserver;
import org.junit.Test;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.concurrent.TimeUnit;

public class RxUDPSocketTest {

    @Test
    public void canSendAndReceiveLocalUDPPackets() throws SocketException {
        SocketAddress address1 = new InetSocketAddress("127.0.0.1", 2001);
        RxUDPSocket socket1 = new RxUDPSocket(address1);
        SocketAddress address2 = new InetSocketAddress("127.0.0.1", 2002);
        RxUDPSocket socket2 = new RxUDPSocket(address2);

        // Create TestObservers
        TestObserver<String> test1To2 = socket2.upPipe
                .map(x -> new String(x.getData(), x.getOffset(), x.getLength()))
                .firstElement().test();
        TestObserver<String> test2To1 = socket1.upPipe
                .map(x -> new String(x.getData(), x.getOffset(), x.getLength()))
                .firstElement().test();

        // None is done yet !
        test1To2.assertNotComplete();
        test2To1.assertNotComplete();

        // Send message from 1 to 2
        String message1To2 = "1To2";
        socket1.downPipe.onNext(new DatagramPacket(message1To2.getBytes(),
                0, message1To2.getBytes().length, address2));

        // This should be done now !
        test1To2.awaitDone(1, TimeUnit.SECONDS).assertResult(message1To2);

        // But this one is still not done yet !
        test2To1.assertNotComplete();

        // Send message from 2 to 1
        String message2To1 = "2To1";
        socket2.downPipe.onNext(new DatagramPacket(message2To1.getBytes(),
                0, message2To1.getBytes().length, address1));

        // And now this one is done too !
        test2To1.awaitDone(1, TimeUnit.SECONDS).assertResult(message2To1);
    }

}