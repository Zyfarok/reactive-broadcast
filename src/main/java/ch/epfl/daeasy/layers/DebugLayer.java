package ch.epfl.daeasy.layers;

import ch.epfl.daeasy.rxlayers.RxLayer;
import ch.epfl.daeasy.rxsockets.RxSocket;
import ch.epfl.daeasy.logging.Logging;
import io.reactivex.Observable;

public class DebugLayer<A> extends RxLayer<A, A> {
    public final String prefixUp;
    public final String prefixDown;

    public DebugLayer(String prefixUp, String prefixDown) {
        this.prefixUp = prefixUp;
        this.prefixDown = prefixDown;
    }

    @Override
    public RxSocket<A> stackOn(RxSocket<A> subSocket) {
        RxSocket<A> socket = new RxSocket<>(subSocket.upPipe.share());
        Observable<A> downPipe = socket.downPipe.share();
        downPipe.subscribe(subSocket.downPipe);

        socket.upPipe.forEach(x -> Logging.debug(prefixUp + x));
        downPipe.forEach(x -> Logging.debug(prefixDown + x));

        return socket;
    }
}
