package ch.epfl.daeasy.layers;

import java.util.Set;
import java.util.function.Predicate;

import com.google.common.base.Functions;

import java.util.HashSet;

import io.reactivex.Observable;
import io.reactivex.disposables.*;
import io.reactivex.observables.GroupedObservable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import ch.epfl.daeasy.protocol.DAPacket;
import ch.epfl.daeasy.rxlayers.RxLayer;
import ch.epfl.daeasy.rxsockets.RxSocket;
import ch.epfl.daeasy.protocol.DAPacket;

public class PerfectLinkLayer extends RxLayer<DAPacket, DAPacket> {

    private Set<DAPacket> delivered; // set of delivered messages
    private Set<DAPacket> pending; // set of message sent but not yet believed delivered

    public PerfectLinkLayer() {
        this.delivered = new HashSet<>();
        this.pending = new HashSet<>();
    }

    public RxSocket<DAPacket> stackOn(RxSocket<DAPacket> subSocket) {

        Subject<DAPacket> subject = PublishSubject.create();
        RxSocket<DAPacket> socket = new RxSocket<>(subSocket.upPipe);

        Observable<GroupedObservable<Boolean, DAPacket>> dapacketsExt = subSocket.upPipe.publish()
                .groupBy(DAPacket::isMessage);

        // split by Message/ACK
        Observable<DAPacket> acksExt = dapacketsExt.filter(gpkt -> !gpkt.getKey()).flatMap(x -> x).publish();
        Observable<DAPacket> messagesExt = dapacketsExt.filter(gpkt -> gpkt.getKey()).flatMap(x -> x).publish();

        // ACK all received messages
        Observable<DAPacket> acksFromMessages = messagesExt.map(msg -> DAPacket.AckFromMessage(msg));
        acksFromMessages.subscribe(subSocket.downPipe);

        // messagesExt.distinct();
        // acksExt.distinct();

        return socket;
    }

}