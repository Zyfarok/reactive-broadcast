package ch.epfl.daeasy.rxlayers;

import ch.epfl.daeasy.rxsockets.RxSocket;
import io.reactivex.Scheduler;
import io.reactivex.subjects.PublishSubject;

import javax.annotation.Nullable;
import java.util.Optional;

public class RxSchedulerLayer<A> extends RxLayer<A,A> {
    private final Scheduler inputScheduler;
    private final Scheduler outputScheduler;
    public RxSchedulerLayer(@Nullable Scheduler inputScheduler, @Nullable Scheduler outputScheduler) {
        this.inputScheduler = inputScheduler;
        this.outputScheduler = outputScheduler;
    }

    public static <A> RxSchedulerLayer<A> scheduleInputOn(Scheduler s) {
        return new RxSchedulerLayer<>(s,null);
    }

    public static <A> RxSchedulerLayer<A> scheduleOuputOn(Scheduler s) {
        return new RxSchedulerLayer<>(null,s);
    }

    public static <A> RxSchedulerLayer<A> scheduleOn(Scheduler s) {
        return new RxSchedulerLayer<>(s,s);
    }

    @Override
    public RxSocket<A> stackOn(RxSocket<A> subSocket) {
        PublishSubject<A> newOutputPipe = Optional.ofNullable(outputScheduler).map(scheduler -> {
            PublishSubject<A> subject = PublishSubject.create();
            subject.observeOn(scheduler).subscribe(subSocket.outputPipe);
            return subject;
        }).orElse(
                subSocket.outputPipe
        );
        return Optional.ofNullable(inputScheduler).map(scheduler ->
                new RxSocket<>(subSocket.inputPipe.observeOn(scheduler), newOutputPipe)
        ).orElse(
                new RxSocket<>(subSocket.inputPipe, newOutputPipe)
        );
    }
}
