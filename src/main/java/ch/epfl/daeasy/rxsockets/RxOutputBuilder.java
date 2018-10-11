package ch.epfl.daeasy.rxsockets;

import io.reactivex.Observable;

public abstract class RxOutputBuilder<A> {
    public abstract void build(final Observable<A> out);
}