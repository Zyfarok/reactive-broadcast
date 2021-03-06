package ch.epfl.daeasy.rxsockets;

import io.reactivex.subjects.PublishSubject;

public class RxLoopbackSocket<A> extends RxSocket<A> {
    private RxLoopbackSocket(PublishSubject<A> loopbackSubject) {
        super(loopbackSubject, loopbackSubject);
    }

    public RxLoopbackSocket() {
        this(PublishSubject.create());
    }
}