package ch.epfl.daeasy.rxsockets;

import ch.epfl.daeasy.rxlayers.RxLayer;
import com.google.common.base.Converter;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;

public interface RxStack<Top> {
    <Higher> RxStack<Higher> stack(RxLayer<Top,Higher> layer);

    RxStack<Top> filter(Predicate<Top> upFilter, Predicate<Top> downFilter);

    RxStack<Top> filter(Predicate<Top> filter);

    RxStack<Top> filterUp(Predicate<Top> filter);

    RxStack<Top> filterDown(Predicate<Top> filter);

    RxStack<Top> scheduleOn(Scheduler upScheduler, Scheduler downScheduler);

    RxStack<Top> scheduleOn(Scheduler s);

    RxStack<Top> scheduleUpOn(Scheduler s);

    RxStack<Top> scheduleDownOn(Scheduler s);

    <Higher> RxStack<Higher> convertPipes(Converter<Observable<Top>,Observable<Higher>> converter);

    <Higher> RxStack<Higher> convertValues(Converter<Top,Higher> converter);

    <Key,Higher> RxStack<Higher> stackGroupedBy(Function<Top,Key> bottomKey,
                                                Function<Higher, Key> topKey,
                                                RxLayer<Top,Higher> innerLayer);

    <Key> RxStack<Top> stackGroupedBy(Function<Top,Key> key,
                                      RxLayer<Top,Top> innerLayer);
}
