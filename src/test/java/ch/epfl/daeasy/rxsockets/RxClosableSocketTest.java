package ch.epfl.daeasy.rxsockets;

import io.reactivex.observers.TestObserver;
import io.reactivex.subjects.PublishSubject;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class RxClosableSocketTest {

    @Test
    public void openSocketTransmitsPackets() {
        PublishSubject<Integer> bottomUpPipe = PublishSubject.create();
        RxSocket<Integer> subSocket = new RxSocket<>(bottomUpPipe);
        RxClosableSocket<Integer> socket = RxClosableSocket.from(subSocket);

        TestObserver<Integer> downTest  = subSocket.downPipe.firstElement().test();
        TestObserver<Integer> upTest  = socket.upPipe.firstElement().test();

        assert socket.isOpen() && !socket.isClosed();

        int testInt = 0;

        // Test down
        socket.downPipe.onNext(++testInt);

        downTest.awaitDone(50, TimeUnit.MILLISECONDS)
                .assertResult(testInt);

        // Test up
        bottomUpPipe.onNext(++testInt);

        upTest.awaitDone(50, TimeUnit.MILLISECONDS)
                .assertResult(testInt);

        // Open socket should not change anything on an already open socket
        socket.open();
        assert socket.isOpen() && !socket.isClosed();

        // Reopening should work
        socket.close();
        assert socket.isClosed() && !socket.isOpen();
        socket.open();
        assert socket.isOpen() && !socket.isClosed();

        // Reset tests
        downTest  = subSocket.downPipe.firstElement().test();
        upTest  = socket.upPipe.firstElement().test();

        // Test down
        socket.downPipe.onNext(++testInt);

        downTest.awaitDone(50, TimeUnit.MILLISECONDS)
                .assertResult(testInt);

        // Test up
        bottomUpPipe.onNext(++testInt);

        upTest.awaitDone(50, TimeUnit.MILLISECONDS)
                .assertResult(testInt);

    }


    @Test
    public void closedSocketDoesNotTransmitPackets() {
        PublishSubject<Integer> bottomUpPipe = PublishSubject.create();
        RxSocket<Integer> subSocket = new RxSocket<>(bottomUpPipe);
        RxClosableSocket<Integer> socket = RxClosableSocket.from(subSocket);

        TestObserver<Integer> downTest  = subSocket.downPipe.firstElement().test();
        TestObserver<Integer> upTest  = socket.upPipe.firstElement().test();

        assert socket.isOpen() && !socket.isClosed();
        socket.close();
        assert socket.isClosed() && !socket.isOpen();

        socket.downPipe.onNext(0);
        bottomUpPipe.onNext(1);

        downTest.awaitDone(50, TimeUnit.MILLISECONDS)
                .assertNotComplete();
        upTest.assertNotComplete();

        socket.close();
        assert socket.isClosed() && !socket.isOpen();
    }
}