package ch.epfl.daeasy.rxlayers;

import ch.epfl.daeasy.rxsockets.RxSocket;

import java.util.function.Consumer;

public class RxDoOnNextUpLayer<T> extends RxLayer<T,T> {
    private final Consumer<T> consumer;

    public RxDoOnNextUpLayer(Consumer<T> consumer) {
        this.consumer = consumer;
    }

    @Override
    public RxSocket<T> stackOn(RxSocket<T> subSocket) {
        return new RxSocket<>(subSocket.upPipe.doOnNext(consumer::accept), subSocket.downPipe);
    }
}
