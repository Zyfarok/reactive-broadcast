package ch.epfl.daeasy.rxlayers.filters;

import com.google.common.base.Converter;

import io.reactivex.Observable;
import io.reactivex.functions.Predicate;

import ch.epfl.daeasy.rxlayers.RxPipeConverterLayer;

public class RxInputFilterLayer<A> extends RxPipeConverterLayer<A,A> {
    public RxInputFilterLayer(Predicate<A> filter) {
        super(new Converter<Observable<A>,Observable<A>>() {

            @Override
            protected Observable<A> doForward(Observable<A> ob) {
                return ob.filter(filter);
            }

            @Override
            protected Observable<A> doBackward(Observable<A> oa) {
                return oa;
            }
        });
    }
}