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
        //System.out.println("Stacking " + this + " with " + innerLayer + " on " + subSocket);
        // #### Zero step : init stuff that will be needed for socket creation
        // Setup incoming input pipe
        Observable<GroupedObservable<K,B>> incomingGroupedInputPipes =
                subSocket.inputPipe.groupBy(keyB)
                        .replay(10).autoConnect();

        // Setup incoming ouput pipe
        PublishSubject<A> incomingOutputPipe = PublishSubject.create();
        Observable<GroupedObservable<K,A>> incomingGroupedOutputPipes = incomingOutputPipe.groupBy(keyA)
                .replay(10).autoConnect();
        //Observable<ConnectableObservable<A>> connectableOutputPipes = incomingGroupedOutputPipes.map(Observable::publish);

        // Create observable of keys to trigger socket creation.
        Observable<K> keyObservable = incomingGroupedInputPipes.map(GroupedObservable::getKey)
                .mergeWith(incomingGroupedOutputPipes.map(GroupedObservable::getKey)).distinct();

        Observable<RxSocket<A>> socketObservable = keyObservable.map(key -> {
            //System.out.println("key: " + key + " called ! Creating inner socket...");

            // #### First step : Build groupedSubSocket
            // Setup input subject but we don't feed it yet
            PublishSubject<B> groupedSubSocketSubjectInput = PublishSubject.create();
            // Build the groupedSubSocket
            RxSocket<B> groupedSubSocket = new RxSocket<>(groupedSubSocketSubjectInput);
            // Plug its output
            groupedSubSocket.outputPipe.subscribe(subSocket.outputPipe);

            // #### Second step : Build groupedInnerSocket (apply innerLayer to groupedSubSocket)
            RxSocket<A> groupedInnerSocket = groupedSubSocket.stack(innerLayer);

            /*
            // #### Third step : Lock the outgoing inputPipe of the innerSocket until subscribed using a last socket
            RxSocket<A> groupedUpperSocket = new RxSocket<>(
                    groupedInnerSocket.inputPipe.???,
                    groupedInnerSocket.outputPipe
            );
            */
            RxSocket<A> groupedUpperSocket = groupedInnerSocket;

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