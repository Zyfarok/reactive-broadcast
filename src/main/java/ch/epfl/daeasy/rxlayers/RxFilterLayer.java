package ch.epfl.daeasy.rxlayers;

import com.google.common.base.Converter;
import io.reactivex.functions.Predicate;

import javax.annotation.Nonnull;
class RxFilterLayer<A> extends RxPipeConverterLayer<A,A> {
    public RxFilterLayer(@Nonnull Predicate<A> inFilter, @Nonnull Predicate<A> outFilter) {
        super(Converter.from(x -> x.filter(inFilter), x -> x.filter(outFilter)));
    }

    public RxFilterLayer(Predicate<A> filter){
        this(filter, filter);
    }

    public static <A> RxFilterLayer<A> inFilter(Predicate<A> filter) {
        return new RxFilterLayer<A>(filter, x -> true);
    }

    public static <A> RxFilterLayer<A> outFilter(Predicate<A> filter) {
        return new RxFilterLayer<A>(x -> true, filter);
    }
}