package ch.epfl.daeasy.layers;

import com.google.common.base.Converter;

import io.reactivex.Observable;

import ch.epfl.daeasy.rxsockets.RxSocket;

public class RxConverterLayer<A,B> extends RxLayer<A,B> {
    final private Converter<Observable<B>,Observable<A>> converter;
    public RxConverterLayer(final Converter<Observable<B>,Observable<A>> bottomUpConverter) {
        this.converter = bottomUpConverter;
    }

    public RxSocket<A> stackOn(final RxSocket<B> subSocket) {
        return new RxSocket<A>(
            converter.convert(subSocket.inputPipe),
            subSocket.outputBuilder.transform(converter.reverse())
        );
    }
}