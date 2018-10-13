package ch.epfl.daeasy.rxsockets;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

import ch.epfl.daeasy.rxlayers.RxLayer;

public class RxSocket<A> {
    public final Observable<A> inputPipe;
    public final PublishSubject<A> outputPipe;

    public RxSocket(final Observable<A> in, final PublishSubject<A> out) {
        this.inputPipe = in;
        this.outputPipe = out;
    }

    public <B> RxSocket<B> stack(RxLayer<B,A> layer) {
        return layer.stackOn(this);
    }
}