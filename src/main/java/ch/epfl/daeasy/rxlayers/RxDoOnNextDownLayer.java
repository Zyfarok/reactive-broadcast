package ch.epfl.daeasy.rxlayers;

import ch.epfl.daeasy.rxsockets.RxSocket;

import java.util.function.Consumer;

public class RxDoOnNextDownLayer<T> extends RxLayer<T,T> {
    private final Consumer<T> consumer;

    public RxDoOnNextDownLayer(Consumer<T> consumer) {
        this.consumer = consumer;
    }

    @Override
    public RxSocket<T> stackOn(RxSocket<T> subSocket) {
        RxSocket<T> socket = new RxSocket<>(subSocket.upPipe);
        socket.downPipe.doOnNext(consumer::accept).subscribe(subSocket.downPipe);
        return socket;
    }
}
