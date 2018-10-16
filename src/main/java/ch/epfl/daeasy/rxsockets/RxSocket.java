package ch.epfl.daeasy.rxsockets;

import ch.epfl.daeasy.rxlayers.RxLayer;
import ch.epfl.daeasy.rxlayers.RxStack;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

public class RxSocket<A> implements RxStack<A> {
    public final Observable<A> inputPipe;
    public final PublishSubject<A> outputPipe;

    public RxSocket(Observable<A> in) {
        this(in, PublishSubject.create());
    }

    public RxSocket(Observable<A> in, PublishSubject<A> out) {
        this.inputPipe = in;
        this.outputPipe = out;
    }

    public <B> RxSocket<B> stack(RxLayer<B,A> layer) {
        return layer.stackOn(this);
    }
}