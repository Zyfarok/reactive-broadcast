package ch.epfl.daeasy.rxsockets;

import java.util.function.Function;

import io.reactivex.Observable;

public abstract class RxOutputBuilder<A> {
    public abstract void buildOutputPipe(final Observable<A> out);

    public <B> RxOutputBuilder<B> transform(final Function<Observable<B>,Observable<A>> pipeConverter) {
        RxOutputBuilder<A> that = this;
        return new RxOutputBuilder<B>() {
            @Override
            public void buildOutputPipe(final Observable<B> out) {
                that.buildOutputPipe(pipeConverter.apply(out));
            }
        };
    }
}