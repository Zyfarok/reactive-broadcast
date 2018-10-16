package ch.epfl.daeasy.rxlayers;

import ch.epfl.daeasy.rxsockets.RxSocket;

public class RxNil<T> extends RxLayer<T,T> {
    @Override
    public RxSocket<T> stackOn(RxSocket<T> subSocket) {
        return subSocket;
    }
}
