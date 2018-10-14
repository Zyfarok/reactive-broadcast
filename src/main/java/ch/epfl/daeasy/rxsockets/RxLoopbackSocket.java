package ch.epfl.daeasy.rxsockets;

import io.reactivex.subjects.PublishSubject;

public class RxLoopbackSocket<A> extends RxSocket<A> {
    public RxLoopbackSocket(PublishSubject<A> loopbackSubject) {
        super(loopbackSubject);
        loopbackSubject.subscribe(this.outputPipe);
    }

    public RxLoopbackSocket() {
        this(PublishSubject.create());
    }
}