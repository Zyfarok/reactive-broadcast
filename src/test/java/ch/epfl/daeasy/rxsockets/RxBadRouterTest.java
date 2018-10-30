package ch.epfl.daeasy.rxsockets;

import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subscribers.TestSubscriber;
import org.junit.Test;

import java.net.*;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.*;

public class RxBadRouterTest {
    @Test
    public void testTest() {
        TestObserver<Integer> tester = new TestObserver<>();
        PublishSubject<Integer> subject = PublishSubject.create();
        //TestObserver test = subject.test();
        subject.subscribe(tester);
        subject.onNext(12);
        subject.onComplete();
        tester.assertResult(12);
    }

    @Test
    public void perfectBadRooterTransmitPackets() throws UnknownHostException {
        // Create rooter and 2 sockets
        RxBadRouter rooter = new RxBadRouter(0,0, 0, SECONDS);

        SocketAddress address1 = new InetSocketAddress(InetAddress.getLocalHost(),1000);
        SocketAddress address2 = new InetSocketAddress(InetAddress.getLocalHost(),1001);
        RxSocket<DatagramPacket> socket1 = rooter.buildSocket(address1);
        RxSocket<DatagramPacket> socket2 = rooter.buildSocket(address2);

        // Create TestObservers
        TestObserver<String> test1To2 = socket2.upPipe
                .map(DatagramPacket::getData).map(String::new)
                .firstElement()
                .test();
        TestObserver<String> test2To1 = socket1.upPipe
                .map(DatagramPacket::getData).map(String::new)
                .firstElement()
                .test();

        // None is done yet !
        test1To2.assertNotComplete();
        test2To1.assertNotComplete();

        // Send message from 1 to 2
        String message1To2 = "1To2";
        socket1.downPipe.onNext(new DatagramPacket(message1To2.getBytes(),
                0, message1To2.getBytes().length, address2));

        // This one is still not done yet !
        test2To1.assertNotComplete();

        // But this one is already done now !
        test1To2.assertResult(message1To2);

        // Send message from 2 to 1
        String message2To1 = "2To1";
        socket2.downPipe.onNext(new DatagramPacket(message2To1.getBytes(),
                0, message2To1.getBytes().length, address1));

        // And now this one is done too !
        test2To1.assertResult(message2To1);
    }
}