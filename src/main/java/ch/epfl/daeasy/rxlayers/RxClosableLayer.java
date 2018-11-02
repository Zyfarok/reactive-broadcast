package ch.epfl.daeasy.rxlayers;

import ch.epfl.daeasy.rxsockets.RxClosableSocket;
import ch.epfl.daeasy.rxsockets.RxSocket;

public class RxClosableLayer<A> extends RxLayer<A,A> {
    @Override
    public RxClosableSocket<A> stackOn(RxSocket<A> subSocket) {
        return RxClosableSocket.from(subSocket);
    }
}
