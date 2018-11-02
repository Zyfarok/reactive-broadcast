package ch.epfl.daeasy.rxsockets;

import io.reactivex.observers.TestObserver;
import org.junit.Assert;
import org.junit.Test;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

public class RxBadRouterTest {
    @Test
    public void perfectBadRouterTransmitPackets() {
        // Create rooter and 2 sockets
        RxBadRouter rooter = new RxBadRouter(0, 0.5, 1, MILLISECONDS);

        SocketAddress address1 = new InetSocketAddress("127.0.0.1",1000);
        SocketAddress address2 = new InetSocketAddress("127.0.0.1",1001);
        RxSocket<DatagramPacket> socket1 = rooter.buildSocket(address1);
        RxSocket<DatagramPacket> socket2 = rooter.buildSocket(address2);

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

        // This one is already done now !
        test1To2.awaitDone(2, SECONDS).assertResult(message1To2);

        // But this one is still not done yet !
        test2To1.assertNotComplete();

        // Send message from 2 to 1
        String message2To1 = "2To1";
        socket2.downPipe.onNext(new DatagramPacket(message2To1.getBytes(),
                0, message2To1.getBytes().length, address1));

        // And now this one is done too !
        test2To1.awaitDone(2, SECONDS).assertResult(message2To1);
    }


    @Test
    public void worstBadRouterNeverTransmitPacket() throws InterruptedException {
        // Create rooter and 2 sockets
        RxBadRouter rooter = new RxBadRouter(1, 0.0, 0, MILLISECONDS);

        SocketAddress address1 = new InetSocketAddress("127.0.0.1",1000);
        SocketAddress address2 = new InetSocketAddress("127.0.0.1",1001);
        RxSocket<DatagramPacket> socket1 = rooter.buildSocket(address1);
        RxSocket<DatagramPacket> socket2 = rooter.buildSocket(address2);

        // Create TestObservers
        TestObserver<String> test = socket2.upPipe.mergeWith(socket1.upPipe)
                .map(x -> new String(x.getData(), x.getOffset(), x.getLength()))
                .firstElement().test();

        // Nothing is done yet !
        test.assertNotComplete();

        // Send message from 1 to 2
        String message1To2 = "1To2";
        socket1.downPipe.onNext(new DatagramPacket(message1To2.getBytes(),
                0, message1To2.getBytes().length, address2));

        // Send message from 2 to 1
        String message2To1 = "2To1";
        socket2.downPipe.onNext(new DatagramPacket(message2To1.getBytes(),
                0, message2To1.getBytes().length, address1));

        // Nothing is received since packets are dropped
        Assert.assertFalse(test.await(50, MILLISECONDS));
        test.assertNever("1To2").assertNever("2To1").assertNotComplete();
    }
}