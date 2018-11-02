package ch.epfl.daeasy.rxsockets;

import sun.java2d.xr.MutableInteger;

public class RxClosableSocket<Top> extends RxSocket<Top> {
    private final MutableInteger tapState;
    private RxClosableSocket(RxSocket<Top> subSocket, MutableInteger tapState) {
        super(subSocket.upPipe.filter(x -> tapState.getValue() != 0));
        this.tapState = tapState;
        this.downPipe.filter(x -> tapState.getValue() != 0).subscribe(subSocket.downPipe);
    }

    public static <Top> RxClosableSocket<Top> from(RxSocket<Top> subSocket) {
        MutableInteger tapState = new MutableInteger(1);
        return new RxClosableSocket<>(subSocket, tapState);
    }

    public void close() {
        tapState.setValue(0);
    }

    public void open() {
        tapState.setValue(1);
    }

    public boolean isClosed() {
        return tapState.getValue() == 0;
    }

    public boolean isOpen() {
        return tapState.getValue() != 0;
    }
}
