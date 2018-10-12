package ch.epfl.daeasy.layers;

import java.util.function.Function;

import io.reactivex.Observable;

import ch.epfl.daeasy.rxsockets.RxSocket;
/**
 * This is a RxSocket "builder".
 * 
 * @param <A> type of RxSocket that it builds.
 * @param <B> type of RxSocket that it stacks onto.
 */
public class RxLayer<A,B> {
    private final Function<Observable<B>,Observable<A>> inConverter;
    private final Function<Observable<A>,Observable<B>> outConverter;

    public RxLayer(final Function<Observable<B>,Observable<A>> inConverter,
            final Function<Observable<A>,Observable<B>> outConverter) {
        this.inConverter = inConverter;
        this.outConverter = outConverter;
    }

    public RxSocket<A> stackOn(final RxSocket<B> subSocket) {
        return new RxSocket(subSocket, inConverter, outConverter);
    }

    public <C> RxLayer<A,C> stackOn(final RxLayer<B,C> that) {
        return new RxLayer<A,C>(that.inConverter.andThen(this.inConverter),
            this.outConverter.andThen(that.outConverter));
    }
}