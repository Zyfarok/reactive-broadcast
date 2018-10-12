package ch.epfl.daeasy.rxsockets;

import java.util.function.Function;

import io.reactivex.Observable;

public class RxSocket<A> {
    private final Observable<A> subInputPipe;
    private final RxOutputBuilder<A> subOutputBuilder;

    public RxSocket(final Observable<A> sin, final RxOutputBuilder<A> sob) {
        this.subInputPipe = sin;
        this.subOutputBuilder = sob;
    }

    public <B> RxSocket(final RxSocket<B> subSocket,
            final Function<Observable<B>,Observable<A>> inConverter,
            final Function<Observable<A>,Observable<B>> outConverter) {
        this(inConverter.apply(subSocket.getInputPipe()), subSocket.getOutputBuilder().transform(outConverter));
    }

    public Observable<A> getInputPipe() {
        return subInputPipe;
    }
    
    public RxOutputBuilder<A> getOutputBuilder() {
        return subOutputBuilder;
    }
}