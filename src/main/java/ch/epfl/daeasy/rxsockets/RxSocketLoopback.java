package ch.epfl.daeasy.rxsockets;

import io.reactivex.Observable;

public class RxSocketLoopback<A> extends RxSocket<A> {
    public RxSocketLoopback(Observable<A> in) {
        super(in);
        this.inputPipe.subscribe(pkt -> this.outputPipe.onNext(pkt));
    }
}