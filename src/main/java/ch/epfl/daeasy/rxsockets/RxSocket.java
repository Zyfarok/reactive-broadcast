package ch.epfl.daeasy.rxsockets;

import io.reactivex.Observable;

import ch.epfl.daeasy.layers.RxLayer;

public class RxSocket<A> {
    public final Observable<A> inputPipe;
    public final RxOutputBuilder<A> outputBuilder;

    public RxSocket(final Observable<A> in, final RxOutputBuilder<A> out) {
        this.inputPipe = in;
        this.outputBuilder = out;
    }

    public <B> RxSocket<B> stack(RxLayer<B,A> layer) {
        return layer.stackOn(this);
    }
}