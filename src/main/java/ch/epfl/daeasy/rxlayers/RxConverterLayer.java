package ch.epfl.daeasy.rxlayers;

import com.google.common.base.Converter;
import io.reactivex.Observable;

import javax.annotation.Nonnull;

public class RxConverterLayer<Bottom, Top> extends RxPipeConverterLayer<Bottom, Top> {
    public RxConverterLayer(Converter<Bottom, Top> upwardConverter) {
        super(new Converter<Observable<Bottom>,Observable<Top>>() {
            final Converter<Top, Bottom> downwardConverter = upwardConverter.reverse();

            @Override
            protected Observable<Top> doForward(@Nonnull Observable<Bottom> o) {
                return o.map(upwardConverter::convert);
            }

            @Override
            protected Observable<Bottom> doBackward(@Nonnull Observable<Top> o) {
                return o.map(downwardConverter::convert);
            }
        });
    }
}