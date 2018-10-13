package ch.epfl.daeasy.rxlayers.filters;

import com.google.common.base.Converter;

import io.reactivex.Observable;
import io.reactivex.functions.Predicate;

import ch.epfl.daeasy.rxlayers.RxPipeConverterLayer;

public class RxFilterLayer<A> extends RxPipeConverterLayer<A,A> {
    public RxFilterLayer(Predicate<A> inFilter, Predicate<A> outFilter) {
        super(new Converter<Observable<A>,Observable<A>>() {

            @Override
            protected Observable<A> doForward(Observable<A> ob) {
                return ob.filter(inFilter);
            }

            @Override
            protected Observable<A> doBackward(Observable<A> oa) {
                return oa.filter(outFilter);
            }
        });
    }

    public RxFilterLayer(Predicate<A> filter){
        this(filter, filter);
    }
}