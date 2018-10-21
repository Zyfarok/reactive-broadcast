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

        Observable<GroupedObservable<Boolean, DAPacket>> dapacketsExt = subSocket.upPipe.groupBy(DAPacket::isMessage);

        // Filter Messages
        Observable<DAPacket> messagesExt = dapacketsExt.filter(gpkt -> gpkt.getKey()).flatMap(x -> x);

        // ACK all received messages
        Observable<DAPacket> acksFromMessages = messagesExt.map(msg -> DAPacket.AckFromMessage(msg));

        // Send
        acksFromMessages.subscribe(subSocket.downPipe);

        RxSocket<DAPacket> socket = new RxSocket<>(messagesExt.distinct());

        return socket;
    }

}