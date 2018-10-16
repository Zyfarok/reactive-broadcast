package ch.epfl.daeasy.rxlayers;

import com.google.common.base.Converter;
import com.google.common.base.Function;
import io.reactivex.Observable;
import io.reactivex.functions.Predicate;

import javax.annotation.Nullable;

public class RxFilterLayer<A> extends RxPipeConverterLayer<A, A> {

    private static <A> Function<Observable<A>, Observable<A>> observableFilter(@Nullable Predicate<A> f) {
        if (f != null)
            return o -> o.filter(f);
        else
            return o -> o;
    }

    public RxFilterLayer(@Nullable Predicate<A> upFilter, @Nullable Predicate<A> downFilter) {
        super(Converter.from(observableFilter(upFilter), observableFilter(downFilter)));
    }

    public RxFilterLayer(Predicate<A> filter) {
        this(filter, filter);
    }

    public static <A> RxFilterLayer<A> upFilter(Predicate<A> filter) {
        return new RxFilterLayer<A>(filter, x -> true);
    }

    public static <A> RxFilterLayer<A> downFilter(Predicate<A> filter) {
        return new RxFilterLayer<A>(x -> true, filter);
    }

    public static <A> RxFilterLayer<A> bidirectionalFilter(Predicate<A> filter) {
        return new RxFilterLayer<>(filter, filter);
    }

    public static <A> RxFilterLayer<A> bidirectionalFilter(Predicate<A> upFilter, Predicate<A> downFilter) {
        return new RxFilterLayer<>(upFilter,downFilter);
    }
}