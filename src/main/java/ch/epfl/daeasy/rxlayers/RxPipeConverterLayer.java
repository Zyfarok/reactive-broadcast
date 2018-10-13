package ch.epfl.daeasy.rxlayers;

import javax.annotation.Nonnull;

import com.google.common.base.Converter;

import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.reactivex.observables.GroupedObservable;
import io.reactivex.subjects.PublishSubject;

import ch.epfl.daeasy.rxsockets.RxSocket;

public class RxPipeConverterLayer<A,B> extends RxLayer<A,B> {
    private final Converter<Observable<B>,Observable<A>> converter;
    public RxPipeConverterLayer(final Converter<Observable<B>,Observable<A>> bottomUpConverter) {
        this.converter = bottomUpConverter;
    }

    public RxSocket<A> stackOn(final RxSocket<B> subSocket) {
        PublishSubject<A> subject = PublishSubject.create();
        converter.reverse().convert(subject).subscribe(subSocket.outputPipe);
        return new RxSocket<A>(converter.convert(subSocket.inputPipe), subject);
    }

    public RxPipeConverterLayer<B,A> reverse() {
        return new RxPipeConverterLayer<B,A>(converter.reverse());
    }

    public static <K,A> RxPipeConverterLayer<GroupedObservable<K,A>,A> mux(Function<A,K> key) {
        return new RxPipeConverterLayer<GroupedObservable<K,A>,A>(new MuxConverter<K,A>(key));
    }

    public static <K,A> RxPipeConverterLayer<A,GroupedObservable<K,A>> demux(Function<A,K> key) {
        return mux(key).reverse();
    }

    private static class MuxConverter<K,A> extends Converter<Observable<A>, Observable<GroupedObservable<K, A>>> {
        private final Function<A,K> key;
        MuxConverter(Function<A,K> key) {
            this.key = key;
        }

        @Override
        protected Observable<GroupedObservable<K, A>> doForward(@Nonnull Observable<A> inputPipe) {
            return inputPipe.groupBy(key);
        }

        @Override
        protected Observable<A> doBackward(@Nonnull Observable<GroupedObservable<K, A>> groupedOuputPipe) {
            return groupedOuputPipe.flatMap(group -> group);
        }
    }
}


