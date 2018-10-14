package ch.epfl.daeasy.rxlayers;

import ch.epfl.daeasy.rxsockets.RxSocket;
import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.reactivex.observables.GroupedObservable;
import io.reactivex.subjects.PublishSubject;

class RxGroupedLayer<K, A> extends RxLayer<A,A> {
    private final Function<A,K> key;
    private final RxLayer<A,A> innerLayer;

    public RxGroupedLayer(Function<A,K> key, RxLayer<A,A> innerLayer) {
        this.key = key;
        this.innerLayer = innerLayer;
    }

    public RxSocket<A> stackOn(RxSocket<A> subSocket) {
        // #### Zero step : init stuff that will be needed for socket creation
        // Setup incoming input pipe
        Observable<GroupedObservable<K,A>> incomingGroupedInputPipes =
                subSocket.inputPipe.groupBy(key);

        // Setup incoming ouput pipe
        PublishSubject<A> incomingOutputPipe = PublishSubject.create();
        Observable<GroupedObservable<K,A>> incomingGroupedOutputPipes = incomingOutputPipe.groupBy(key);
        //Observable<ConnectableObservable<A>> connectableOutputPipes = incomingGroupedOutputPipes.map(Observable::publish);

        // Create observable of keys to trigger socket creation.
        Observable<K> keyObservable = incomingGroupedInputPipes.map(GroupedObservable::getKey)
                .mergeWith(incomingGroupedOutputPipes.map(GroupedObservable::getKey)).distinct();

        Observable<RxSocket<A>> socketObservable = keyObservable.map(key -> {
            // #### First step : Build groupedSubSocket
            // Setup input subject but we don't feed it yet
            PublishSubject<A> groupedSubSocketSubjectInput = PublishSubject.create();
            // Build the groupedSubSocket
            RxSocket<A> groupedSubSocket = new RxSocket<>(groupedSubSocketSubjectInput);
            // Plug its output
            groupedSubSocket.outputPipe.subscribe(subSocket.outputPipe);

            // #### Second step : Build innerSocket (apply innerLayer to groupedSubSocket)
            RxSocket<A> groupedInnerSocket = groupedSubSocket.stack(innerLayer);

            // #### Third step : Lock the outgoing inputPipe of the innerSocket until subscribed using a last socket
            RxSocket<A> groupedUpperSocket = new RxSocket<>(
                    groupedInnerSocket.inputPipe.publish().refCount(),
                    groupedInnerSocket.outputPipe
            );
            // #### Forth step : Connect the last socket to the incoming outputs
            incomingGroupedOutputPipes.filter(gco -> gco.getKey().equals(key))
                    .forEach(x -> x.subscribe(groupedUpperSocket.outputPipe));
            // #### Fifth step : Plug the subject incoming input :
            incomingGroupedInputPipes.filter(x -> x.getKey().equals(key))
                    .forEach(x -> x.subscribe(groupedSubSocketSubjectInput));

            return groupedUpperSocket;
        });

        // #### Sixth step : Connect the outgoing input pipe to the final socket.
        Observable<A> outgoingInputPipe = socketObservable.flatMap(s -> s.inputPipe);

        return new RxSocket<>(outgoingInputPipe, incomingOutputPipe);
    }
}