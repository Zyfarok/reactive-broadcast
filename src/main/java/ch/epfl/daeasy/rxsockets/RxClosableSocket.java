package ch.epfl.daeasy.rxsockets;

import java.util.concurrent.atomic.AtomicBoolean;

public class RxClosableSocket<Top> extends RxSocket<Top> {
    private final AtomicBoolean isTapOpen;

    public RxClosableSocket(RxSocket<Top> subSocket, AtomicBoolean isTapOpen) {
        super(subSocket.upPipe.filter(x -> isTapOpen.get()));
        this.isTapOpen = isTapOpen;
        this.downPipe.filter(x -> isTapOpen.get()).subscribe(subSocket.downPipe);
    }

    public static <Top> RxClosableSocket<Top> from(RxSocket<Top> subSocket) {
        AtomicBoolean tapState = new AtomicBoolean(true);
        return new RxClosableSocket<>(subSocket, tapState);
    }

    public void close() {
        isTapOpen.set(false);
    }

    public void open() {
        isTapOpen.set(true);
    }

    public boolean isClosed() {
        return !isTapOpen.get();
    }

    public boolean isOpen() {
        return isTapOpen.get();
    }
}
