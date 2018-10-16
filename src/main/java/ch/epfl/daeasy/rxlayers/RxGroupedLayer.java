package ch.epfl.daeasy.rxlayers;

import ch.epfl.daeasy.rxsockets.RxSocket;
import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.reactivex.observables.GroupedObservable;
import io.reactivex.subjects.PublishSubject;

public class RxGroupedLayer<K, A, B> extends RxLayer<A,B> {
    private final Function<A,K> keyA;
    private final Function<B,K> keyB;
    private final RxLayer<A,B> innerLayer;

    public RxGroupedLayer(Function<A,K> keyA, Function<B,K> keyB, RxLayer<A,B> innerLayer) {
        this.keyA = keyA;
        this.keyB = keyB;
        this.innerLayer = innerLayer;
    }

    public static <K,A> RxGroupedLayer<K,A,A> create(Function<A,K> key, RxLayer<A,A> innerLayer) {
        return new RxGroupedLayer<>(key,key,innerLayer);
    }

    public RxSocket<A> stackOn(RxSocket<B> subSocket) {
        // #### Zero step : init stuff that will be needed for socket creation
        // Setup the final socket
        PublishSubject<A> outgoingInputPipe = PublishSubject.create();
        RxSocket<A> groupedSocket = new RxSocket<>(outgoingInputPipe);

        // GroupBy incoming input pipe
        Observable<GroupedObservable<K,B>> incomingGroupedInputPipes =
                subSocket.inputPipe.groupBy(keyB)
                        .replay(1).autoConnect();

        // GroupBy incoming output pipe
        Observable<GroupedObservable<K,A>> incomingGroupedOutputPipes =
                groupedSocket.outputPipe.groupBy(keyA)
                        .replay(1).autoConnect();

        // Create observable of keys to trigger socket creation.
        Observable<K> keyObservable = incomingGroupedInputPipes.map(GroupedObservable::getKey)
                .mergeWith(incomingGroupedOutputPipes.map(GroupedObservable::getKey)).distinct();

        // Create an inner socket for each key
        keyObservable.forEach(key -> {
            //System.out.println("key: " + key + " found ! Creating inner socket...");

            // #### First step : Build inner subSocket
            // Setup an input subject but we don't feed it yet
            PublishSubject<B> groupedSubSocketSubjectInput = PublishSubject.create();
            // Build the groupedSubSocket
            RxSocket<B> innerSubSocket = new RxSocket<>(groupedSubSocketSubjectInput);

            // #### Second : Plug the innerSubSocket outgoing output pipe.
            innerSubSocket.outputPipe.subscribe(subSocket.outputPipe);

            // #### Third step : Build innerSocket (apply innerLayer to innerSubSocket)
            RxSocket<A> innerSocket = innerSubSocket.stack(innerLayer);

            // #### Forth step : Plug the innerSocket outgoing input pipe.
            innerSocket.inputPipe.subscribe(outgoingInputPipe);

            // #### Fifth step : Plug the innerSocket incoming output pipe.
            incomingGroupedOutputPipes.filter(gco -> gco.getKey().equals(key)).take(1)
                    .forEach(x -> x.subscribe(innerSocket.outputPipe));

            // #### Sixth step : Plug the innerSubSocket incoming input pipe.
            incomingGroupedInputPipes.filter(x -> x.getKey().equals(key)).take(1)
                    .forEach(x -> x.subscribe(groupedSubSocketSubjectInput));
        });

        return groupedSocket;
    }
}