package ch.epfl.daeasy.rxlayers;

import ch.epfl.daeasy.rxsockets.RxSocket;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.observables.GroupedObservable;
import io.reactivex.subjects.PublishSubject;

public class RxGroupedLayer<Key, Bottom, Top> extends RxLayer<Bottom, Top> {
    private final Function<Bottom, Key> keyB;
    private final Function<Top, Key> keyA;
    private final RxLayer<Bottom, Top> innerLayer;

    private RxGroupedLayer(Function<Bottom, Key> keyB, Function<Top, Key> keyA, RxLayer<Bottom, Top> innerLayer) {
        this.keyB = keyB;
        this.keyA = keyA;
        this.innerLayer = innerLayer;
    }

    public static <Key, Bottom, Top> RxGroupedLayer<Key, Bottom, Top> create(Function<Bottom, Key> keyB, Function<Top, Key> keyA, RxLayer<Bottom, Top> innerLayer) {
        return new RxGroupedLayer<>(keyB,keyA,innerLayer);
    }

    public static <Key, Bottom> RxGroupedLayer<Key, Bottom, Bottom> create(Function<Bottom, Key> key, RxLayer<Bottom, Bottom> innerLayer) {
        return create(key,key,innerLayer);
    }

    public RxSocket<Top> stackOn(RxSocket<Bottom> subSocket) {
        // #### Zero step : init stuff that will be needed for socket creation
        // Prepare the final topSocket
        PublishSubject<Top> topUpPipeOut = PublishSubject.create();
        RxSocket<Top> topSocket = new RxSocket<>(topUpPipeOut);

        // GroupBy upPipe input from subSocket
        Observable<GroupedObservable<Key, Bottom>> groupedBottomUpPipesIn =
                subSocket.upPipe.groupBy(keyB)
                        .replay(1).autoConnect();

        // GroupBy downPipe input from topSocket
        Observable<GroupedObservable<Key, Top>> groupedTopDownPipesIn =
                topSocket.downPipe.groupBy(keyA)
                        .replay(1).autoConnect();

        // Create observable of keys to trigger socket creation.
        Observable<Key> keyObservable = groupedBottomUpPipesIn.map(GroupedObservable::getKey)
                .mergeWith(groupedTopDownPipesIn.map(GroupedObservable::getKey)).distinct();

        // TODO : Avoid potential OutOfMemory errors by handling disposables and avoiding "distinct" usage.
        // Create an inner socket for each key
        Observable<Disposable> disposables = keyObservable.map(key -> {
            //System.out.println("key: " + key + " found ! Creating inner socket...");

            // #### First step : Build inner subSocket
            // Setup an up subject but we don't feed it yet
            PublishSubject<Bottom> innerBottomUpPipeIn = PublishSubject.create();
            // Build the groupedSubSocket
            RxSocket<Bottom> innerSubSocket = new RxSocket<>(innerBottomUpPipeIn);

            // #### Second : Plug the innerSubSocket out down pipe.
            innerSubSocket.downPipe.subscribe(subSocket.downPipe);

            // #### Third step : Build innerSocket (apply innerLayer to innerSubSocket)
            RxSocket<Top> innerSocket = innerSubSocket.stack(innerLayer);

            // #### Forth step : Plug the innerSocket out up pipe.
            innerSocket.upPipe.subscribe(topUpPipeOut);

            // #### Fifth step : Plug the innerSocket in down pipe.
            Disposable d1 = groupedTopDownPipesIn.filter(gco -> gco.getKey().equals(key)).take(1)
                    .forEach(x -> x.subscribe(innerSocket.downPipe));

            // #### Sixth step : Plug the innerSubSocket in up pipe.
            Disposable d2 = groupedBottomUpPipesIn.filter(x -> x.getKey().equals(key)).take(1)
                    .forEach(x -> x.subscribe(innerBottomUpPipeIn));

            return new Disposable() {
                @Override
                public void dispose() {
                    d1.dispose();
                    d2.dispose();
                }

                @Override
                public boolean isDisposed() {
                    return d1.isDisposed() && d2.isDisposed();
                }
            };
        });

        return topSocket;
    }
}