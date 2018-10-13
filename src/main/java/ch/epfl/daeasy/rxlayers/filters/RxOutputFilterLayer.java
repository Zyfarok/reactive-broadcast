package ch.epfl.daeasy.rxlayers.filters;

import com.google.common.base.Converter;

import io.reactivex.Observable;
import io.reactivex.functions.Predicate;

import ch.epfl.daeasy.rxlayers.RxPipeConverterLayer;;

public class RxOutputFilterLayer<A> extends RxPipeConverterLayer<A,A> {
    public RxOutputFilterLayer(Predicate<A> filter) {
        super(new Converter<Observable<A>,Observable<A>>() {

            @Override
            protected Observable<A> doForward(Observable<A> ob) {
                return ob;
            }

            @Override
            protected Observable<A> doBackward(Observable<A> oa) {
                return oa.filter(filter);
            }
        });
    }
}