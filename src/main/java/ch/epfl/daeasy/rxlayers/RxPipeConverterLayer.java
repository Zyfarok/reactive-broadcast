package ch.epfl.daeasy.rxlayers;

import ch.epfl.daeasy.rxsockets.RxSocket;
import com.google.common.base.Converter;
import io.reactivex.Observable;
public class RxPipeConverterLayer<Bottom,Top> extends RxLayer<Bottom,Top> {
    private final Converter<Observable<Bottom>,Observable<Top>> upwardConverter;
    public RxPipeConverterLayer(Converter<Observable<Bottom>, Observable<Top>> upwardConverter) {
        this.upwardConverter = upwardConverter;
    }

    public RxSocket<Top> stackOn(RxSocket<Bottom> subSocket) {
        RxSocket<Top> socket = new RxSocket<>(upwardConverter.convert(subSocket.upPipe));
        upwardConverter.reverse().convert(socket.downPipe).subscribe(subSocket.downPipe);
        return socket;
    }

    public RxPipeConverterLayer<Top,Bottom> reverse() {
        return new RxPipeConverterLayer<>(upwardConverter.reverse());
    }
}


