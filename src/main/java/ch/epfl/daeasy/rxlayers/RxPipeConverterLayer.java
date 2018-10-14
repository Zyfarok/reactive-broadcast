package ch.epfl.daeasy.rxlayers;

import ch.epfl.daeasy.rxsockets.RxSocket;
import com.google.common.base.Converter;
import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.reactivex.observables.GroupedObservable;
import io.reactivex.subjects.PublishSubject;

public class RxPipeConverterLayer<A,B> extends RxLayer<A,B> {
    private final Converter<Observable<B>,Observable<A>> converter;
    public RxPipeConverterLayer(Converter<Observable<B>,Observable<A>> bottomUpConverter) {
        this.converter = bottomUpConverter;
    }

    public RxSocket<A> stackOn(RxSocket<B> subSocket) {
        PublishSubject<A> subject = PublishSubject.create();
        converter.reverse().convert(subject).subscribe(subSocket.outputPipe);
        return new RxSocket<>(converter.convert(subSocket.inputPipe), subject);
    }

    public RxPipeConverterLayer<B,A> reverse() {
        return new RxPipeConverterLayer<B,A>(converter.reverse());
    }

    public static <K,A> RxPipeConverterLayer<GroupedObservable<K,A>,A> mux(Function<A,K> key) {
        return new RxPipeConverterLayer<>(muxConverter(key));
    }

    public static <K,A> RxPipeConverterLayer<A,GroupedObservable<K,A>> demux(Function<A,K> key) {
        return mux(key).reverse();
    }

    private static <K,A> Converter<Observable<A>, Observable<GroupedObservable<K, A>>> muxConverter(Function<A,K> key) {
        return Converter.from(inputPipe -> inputPipe.groupBy(key), groupedOutputPipe -> groupedOutputPipe.flatMap(group -> group));
    }
}


