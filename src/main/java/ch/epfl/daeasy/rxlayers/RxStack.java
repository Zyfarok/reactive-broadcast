package ch.epfl.daeasy.rxlayers;

import com.google.common.base.Converter;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.functions.Predicate;

public interface RxStack<A> {
    <B> RxStack<B> stack(RxLayer<B,A> layer);

    default RxStack<A> filter(Predicate<A> filter) {
        return this.stack(RxFilterLayer.filter(filter));
    }

    default RxStack<A> filter(Predicate<A> inFilter, Predicate<A> outFilter) {
        return this.stack(RxFilterLayer.filter(inFilter,outFilter));
    }

    default RxStack<A> filterIn(Predicate<A> filter) {
        return this.stack(RxFilterLayer.inFilter(filter));
    }

    default RxStack<A> filterOut(Predicate<A> filter) {
        return this.stack(RxFilterLayer.outFilter(filter));
    }

    default RxStack<A> scheduleOn(Scheduler inputScheduler, Scheduler outputScheduler) {
        return this.stack(RxSchedulerLayer.scheduleOn(inputScheduler, outputScheduler));
    }

    default RxStack<A> scheduleOn(Scheduler s) {
        return this.stack(RxSchedulerLayer.scheduleOn(s));
    }

    default RxStack<A> scheduleInputOn(Scheduler s) {
        return this.stack(RxSchedulerLayer.scheduleInputOn(s));
    }

    default RxStack<A> scheduleOuputOn(Scheduler s) {
        return this.stack(RxSchedulerLayer.scheduleOuputOn(s));
    }

    default <B> RxStack<B> convertPipe(Converter<Observable<A>,Observable<B>> converter) {
        return this.stack(new RxPipeConverterLayer<>(converter));
    }

    default <B> RxStack<B> map(Converter<A,B> converter) {
        return this.stack(new RxConverterLayer<>(converter));
    }
}
