package ch.epfl.daeasy.rxlayers;

import ch.epfl.daeasy.rxsockets.RxSocket;
import ch.epfl.daeasy.rxsockets.RxStack;
import com.google.common.base.Converter;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;

/**
 * This is a RxSocket "builder".
 *
 * @param <Bottom> type of RxSocket that it can stacks onto.
 * @param <Top> type of RxSocket that it builds.
 */
public abstract class RxLayer<Bottom,Top> implements RxStack<Top> {

    public abstract RxSocket<Top> stackOn(RxSocket<Bottom> subSocket);

    @Override
    public <Higher> RxLayer<Bottom,Higher> stack(RxLayer<Top,Higher> that) {
        RxLayer<Bottom,Top> it = this;
        return new RxLayer<Bottom, Higher>() {
            @Override
            public RxSocket<Higher> stackOn(RxSocket<Bottom> subSocket) {
                return that.stackOn(it.stackOn(subSocket));
            }
        };
    }

    public <Lower> RxLayer<Lower,Top> stackOn(RxLayer<Lower,Bottom> that) {
        RxLayer<Bottom,Top> it = this;
        return new RxLayer<Lower, Top>() {
            @Override
            public RxSocket<Top> stackOn(RxSocket<Lower> subSocket) {
                return it.stackOn(that.stackOn(subSocket));
            }
        };
    }

    @Override
    public RxLayer<Bottom, Top> filter(Predicate<Top> upFilter, Predicate<Top> downFilter) {
        return this.stack(RxFilterLayer.bidirectionalFilter(upFilter,downFilter));
    }

    @Override
    public RxLayer<Bottom, Top> filter(Predicate<Top> filter) {
        return this.stack(RxFilterLayer.bidirectionalFilter(filter));
    }

    @Override
    public RxLayer<Bottom, Top> filterUp(Predicate<Top> filter) {
        return this.stack(RxFilterLayer.upFilter(filter));
    }

    @Override
    public RxLayer<Bottom, Top> filterDown(Predicate<Top> filter) {
        return this.stack(RxFilterLayer.downFilter(filter));
    }

    @Override
    public RxLayer<Bottom, Top> scheduleOn(Scheduler upScheduler, Scheduler downScheduler) {
        return this.stack(RxSchedulerLayer.bidirectionalScheduler(upScheduler, downScheduler));
    }

    @Override
    public RxLayer<Bottom, Top> scheduleOn(Scheduler s) {
        return this.stack(RxSchedulerLayer.bidirectionalScheduler(s));
    }

    @Override
    public RxLayer<Bottom, Top> scheduleUpOn(Scheduler s) {
        return this.stack(RxSchedulerLayer.upScheduler(s));
    }

    @Override
    public RxLayer<Bottom, Top> scheduleDownOn(Scheduler s) {
        return this.stack(RxSchedulerLayer.downScheduler(s));
    }

    @Override
    public <Higher> RxLayer<Bottom,Higher> convertPipes(Converter<Observable<Top>,Observable<Higher>> converter) {
        return this.stack(new RxPipeConverterLayer<>(converter));
    }

    @Override
    public <Higher> RxLayer<Bottom,Higher> convertValues(Converter<Top, Higher> converter) {
        return this.stack(new RxConverterLayer<>(converter));
    }

    @Override
    public <Key,Higher> RxLayer<Bottom,Higher> stackGroupedBy(Function<Top,Key> bottomKey,
                                                              Function<Higher, Key> topKey,
                                                              RxLayer<Top,Higher> innerLayer) {
        return this.stack(RxGroupedLayer.create(bottomKey, topKey, innerLayer));
    }

    @Override
    public <Key> RxLayer<Bottom,Top> stackGroupedBy(Function<Top,Key> key,
                                                    RxLayer<Top,Top> innerLayer) {
        return this.stack(RxGroupedLayer.create(key, innerLayer));
    }
}