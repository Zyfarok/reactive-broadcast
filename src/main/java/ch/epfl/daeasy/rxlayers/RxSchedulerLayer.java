package ch.epfl.daeasy.rxlayers;

import ch.epfl.daeasy.rxsockets.RxSocket;
import io.reactivex.Scheduler;
import io.reactivex.subjects.PublishSubject;

import javax.annotation.Nullable;
import java.util.Optional;

public class RxSchedulerLayer<A> extends RxLayer<A,A> {
    private final Scheduler upScheduler;
    private final Scheduler downScheduler;

    private RxSchedulerLayer(@Nullable Scheduler upScheduler, @Nullable Scheduler downScheduler) {
        this.upScheduler = upScheduler;
        this.downScheduler = downScheduler;
    }

    public static <A> RxSchedulerLayer<A> bidirectionalScheduler(Scheduler upScheduler, Scheduler downScheduler) {
        return new RxSchedulerLayer<>(upScheduler,downScheduler);
    }

    public static <A> RxSchedulerLayer<A> bidirectionalScheduler(Scheduler s) {
        return new RxSchedulerLayer<>(s,s);
    }

    public static <A> RxSchedulerLayer<A> upScheduler(Scheduler s) {
        return new RxSchedulerLayer<>(s,null);
    }

    public static <A> RxSchedulerLayer<A> downScheduler(Scheduler s) {
        return new RxSchedulerLayer<>(null,s);
    }

    @Override
    public RxSocket<A> stackOn(RxSocket<A> subSocket) {
        PublishSubject<A> downPipe = Optional.ofNullable(downScheduler).map(scheduler -> {
            PublishSubject<A> subject = PublishSubject.create();
            subject.observeOn(scheduler).subscribe(subSocket.downPipe);
            return subject;
        }).orElse(
                subSocket.downPipe
        );
        return Optional.ofNullable(upScheduler).map(scheduler ->
                new RxSocket<>(subSocket.upPipe.observeOn(scheduler), downPipe)
        ).orElse(
                new RxSocket<>(subSocket.upPipe, downPipe)
        );
    }
}
