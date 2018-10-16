package ch.epfl.daeasy.rxlayers;

import ch.epfl.daeasy.rxsockets.RxSocket;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

public class RxTrampolineLayer<A> extends RxLayer<A,A> {
    @Override
    public RxSocket<A> stackOn(RxSocket<A> subSocket) {
        RxSocket<A> socket = new RxSocket<>(subSocket.inputPipe.observeOn(Schedulers.trampoline()), PublishSubject.create());
        socket.outputPipe.observeOn(Schedulers.trampoline()).subscribe(subSocket.outputPipe);
        return socket;
    }
}
