package ch.epfl.daeasy.rxlayers;

import com.google.common.base.Converter;

import io.reactivex.Observable;

public class RxConverterLayer<A,B> extends RxPipeConverterLayer<A,B> {
    public RxConverterLayer(final Converter<B,A> bottomUpConverter) {
        super(new Converter<Observable<B>,Observable<A>>() {
            final Converter<A,B> topDownConverter = bottomUpConverter.reverse();

            @Override
            protected Observable<A> doForward(Observable<B> ob) {
                return ob.map(bottomUpConverter::convert);
            }

            @Override
            protected Observable<B> doBackward(Observable<A> oa) {
                return oa.map(topDownConverter::convert);
            }
        });
    }
}