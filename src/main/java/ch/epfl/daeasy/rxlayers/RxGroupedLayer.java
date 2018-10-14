package ch.epfl.daeasy.rxlayers;

import ch.epfl.daeasy.rxsockets.RxSocket;
import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.reactivex.observables.ConnectableObservable;
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
        Observable<GroupedConnectableObservable> incomingGroupedInputPipes =
                subSocket.inputPipe.groupBy(key)
                .map(x -> new GroupedConnectableObservable(x.getKey(), x.publish()));

        // Setup incoming ouput pipe
        PublishSubject<A> incomingOutputPipe = PublishSubject.create();
        Observable<GroupedObservable<K,A>> incomingGroupedOutputPipes = incomingOutputPipe.groupBy(key);
        //Observable<ConnectableObservable<A>> connectableOutputPipes = incomingGroupedOutputPipes.map(Observable::publish);

        // Create observable of keys to trigger socket creation.
        Observable<K> keyObservable = incomingGroupedInputPipes.map(x -> x.key)
                .mergeWith(incomingGroupedOutputPipes.map(GroupedObservable::getKey)).distinct();

        Observable<RxSocket<A>> socketObservable = keyObservable.map(key -> {
            // #### First step : Build groupedSubSocket
            // Get it's input observable. (Locked for know)
            ConnectableObservable<A> in = incomingGroupedInputPipes
                    .filter(x -> x.key.equals(key))
                    .map(x -> x.c)
                    .flatMap(x -> x).publish();
            // Build the groupedSubSocket
            RxGroupedConnectableSocket<K,A> groupedSubSocket = new RxGroupedConnectableSocket<>(key, in);
            // Plug its output
            groupedSubSocket.outputPipe.subscribe(subSocket.outputPipe);

            // #### Second step : Build innerSocket (apply innerLayer to groupedSubSocket)
            RxSocket<A> innerSocket = groupedSubSocket.stack(innerLayer);

            // #### Third step : Lock the outgoing inputPipe of the innerSocket until subscribed using a last socket
            ConnectableObservable<A> publishedInnerSocket = innerSocket.inputPipe.publish();
            RxSocket<A> socket = new RxSocket<>(
                    publishedInnerSocket.doOnSubscribe(d -> publishedInnerSocket.connect()),
                    innerSocket.outputPipe
            );
            // #### Forth step : Connect the last socket to the incoming outputs
            incomingGroupedOutputPipes.filter(gco -> gco.getKey().equals(key))
                    .forEach(x -> x.subscribe(socket.outputPipe));
            // #### Fifth step : Unlock the groupedSubSocket input.
            groupedSubSocket.connect();
            return socket;
        });

        // #### Sixth step : Connect the outgoing input pipe to the final socket.
        Observable<A> outgoingInputPipe = socketObservable.flatMap(s -> s.inputPipe);

        return new RxSocket<>(outgoingInputPipe, incomingOutputPipe);
    }

    //public RxSocket<A> stackOn(RxSocket<B> subSocket) {
    //    return new RxSocket<A>(
    //        converter.convert(subSocket.inputPipe),
    //        subSocket.outputBuilder.transform(converter.reverse())
    //    );
    //}

    private class GroupedConnectableObservable {
        final K key;
        final ConnectableObservable<A> c;

        GroupedConnectableObservable(K key, ConnectableObservable<A> c) {
            this.key = key;
            this.c = c;
        }
    }

    private static class RxGroupedConnectableSocket<K,A> extends RxSocket<A> {
        final K key;
        private final ConnectableObservable<A> connectableInput;

        RxGroupedConnectableSocket(K key, ConnectableObservable<A> in, PublishSubject<A> out) {
            super(in, out);
            connectableInput = in;
            this.key = key;
        }

        RxGroupedConnectableSocket(K key, ConnectableObservable<A> in) {
            this(key, in, PublishSubject.create());
        }

        void connect() {
            connectableInput.connect();
        }
    }
}