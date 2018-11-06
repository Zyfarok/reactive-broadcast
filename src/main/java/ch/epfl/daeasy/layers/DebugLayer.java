package ch.epfl.daeasy.layers;

import ch.epfl.daeasy.rxlayers.RxLayer;
import ch.epfl.daeasy.rxsockets.RxSocket;
import io.reactivex.Observable;

public class DebugLayer<A> extends RxLayer<A,A> {
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

        socket.upPipe.forEach(x -> System.out.println(prefixUp + x));
        downPipe.forEach(x -> System.out.println(prefixDown + x));

        return socket;
    }
}
