package ch.epfl.daeasy.rxlayers;

import com.google.common.base.Converter;
import io.reactivex.Observable;

import javax.annotation.Nonnull;

public class RxConverterLayer<A,B> extends RxPipeConverterLayer<A,B> {
    public RxConverterLayer(Converter<B,A> bottomUpConverter) {
        super(new Converter<Observable<B>,Observable<A>>() {
            final Converter<A,B> topDownConverter = bottomUpConverter.reverse();

            @Override
            protected Observable<A> doForward(@Nonnull Observable<B> o) {
                return o.map(bottomUpConverter::convert);
            }

            @Override
            protected Observable<B> doBackward(@Nonnull Observable<A> o) {
                return o.map(topDownConverter::convert);
            }
        });
    }
}