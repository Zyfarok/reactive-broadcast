package ch.epfl.daeasy.rxsockets;

import ch.epfl.daeasy.rxlayers.*;
import com.google.common.base.Converter;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.subjects.PublishSubject;

public class RxSocket<Top> implements RxStack<Top> {
    public final Observable<Top> upPipe;
    public final PublishSubject<Top> downPipe;

    public RxSocket(Observable<Top> up) {
        this(up, PublishSubject.create());
    }

    public RxSocket(Observable<Top> up, PublishSubject<Top> down) {
        this.upPipe = up.share();
        this.downPipe = down;
    }

    @Override
    public <Higher> RxSocket<Higher> stack(RxLayer<Top,Higher> layer) {
        return layer.stackOn(this);
    }

    @Override
    public RxSocket<Top> filter(Predicate<Top> upFilter, Predicate<Top> downFilter) {
        return this.stack(RxFilterLayer.bidirectionalFilter(upFilter,downFilter));
    }

    @Override
    public RxSocket<Top> filter(Predicate<Top> filter) {
        return this.stack(RxFilterLayer.bidirectionalFilter(filter));
    }

    @Override
    public RxSocket<Top> filterUp(Predicate<Top> filter) {
        return this.stack(RxFilterLayer.upFilter(filter));
    }

    @Override
    public RxSocket<Top> filterDown(Predicate<Top> filter) {
        return this.stack(RxFilterLayer.downFilter(filter));
    }

    @Override
    public RxSocket<Top> scheduleOn(Scheduler upScheduler, Scheduler downScheduler) {
        return this.stack(RxSchedulerLayer.bidirectionalScheduler(upScheduler, downScheduler));
    }

    @Override
    public RxSocket<Top> scheduleOn(Scheduler s) {
        return this.stack(RxSchedulerLayer.bidirectionalScheduler(s));
    }

    @Override
    public RxSocket<Top> scheduleUpOn(Scheduler s) {
        return this.stack(RxSchedulerLayer.upScheduler(s));
    }

    @Override
    public RxSocket<Top> scheduleDownOn(Scheduler s) {
        return this.stack(RxSchedulerLayer.downScheduler(s));
    }

    @Override
    public  <Higher> RxSocket<Higher> convertPipes(Converter<Observable<Top>,Observable<Higher>> converter) {
        return this.stack(new RxPipeConverterLayer<>(converter));
    }

    @Override
    public <Higher> RxSocket<Higher> convertValues(Converter<Top,Higher> converter) {
        return this.stack(new RxConverterLayer<>(converter));
    }

    @Deprecated
    @Override
    public <Key,Higher> RxSocket<Higher> stackGroupedBy(Function<Top,Key> bottomKey,
                                                        Function<Higher, Key> topKey,
                                                        RxLayer<Top,Higher> innerLayer) {
        return this.stack(RxGroupedLayer.create(bottomKey, topKey, innerLayer));
    }

    @Deprecated
    @Override
    public <Key> RxSocket<Top> stackGroupedBy(Function<Top, Key> key,
                                              RxLayer<Top, Top> innerLayer) {
        return this.stack(RxGroupedLayer.create(key, innerLayer));
    }

    public RxClosableSocket<Top> toClosable() {
        return RxClosableSocket.from(this);
    }
}