package ch.epfl.daeasy.rxsockets;

import io.reactivex.Observable;

public abstract class RxSocket<A> {
    public abstract Observable<A> get();
    public abstract void send(Observable<A> dpOut);
}