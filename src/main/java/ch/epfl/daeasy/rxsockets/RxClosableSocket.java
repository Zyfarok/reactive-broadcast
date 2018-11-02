package ch.epfl.daeasy.rxsockets;

import java.util.concurrent.atomic.AtomicBoolean;

public class RxClosableSocket<Top> extends RxSocket<Top> {
    private final AtomicBoolean tapState;

    private RxClosableSocket(RxSocket<Top> subSocket, AtomicBoolean tapState) {
        super(subSocket.upPipe.filter(x -> tapState.get()));
        this.tapState = tapState;
        this.downPipe.filter(x -> tapState.get()).subscribe(subSocket.downPipe);
    }

    public static <Top> RxClosableSocket<Top> from(RxSocket<Top> subSocket) {
        AtomicBoolean tapState = new AtomicBoolean(true);
        return new RxClosableSocket<>(subSocket, tapState);
    }

    public void close() {
        tapState.set(false);
    }

    public void open() {
        tapState.set(true);
    }

    public boolean isClosed() {
        return !tapState.get();
    }

    public boolean isOpen() {
        return tapState.get();
    }
}
