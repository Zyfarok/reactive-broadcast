package ch.epfl.daeasy.layers;

import java.util.Set;
import java.util.concurrent.TimeUnit;
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

        RxSocket<DAPacket> socket = new RxSocket<>(subject);

        Subject<DAPacket> intUp = subject;
        Observable<DAPacket> intDown = socket.downPipe;
        Observable<DAPacket> extIn = subSocket.upPipe;
        Subject<DAPacket> extOut = subSocket.downPipe;

        intDown.subscribe(p -> System.out.println("Layer IntDown: " + p.toString()));
        extIn.subscribe(p -> System.out.println("Layer ExtIn: " + p.toString()));

        // Observable<GroupedObservable<Boolean, DAPacket>> dapacketsExt = ;

        // Exterior Messages
        Observable<DAPacket> messagesExt = extIn.filter(DAPacket::isMessage);
        // Exterior ACKs
        Observable<DAPacket> acksExt = extIn.filter(DAPacket::isACK);

        // Interior Messages
        Observable<DAPacket> messagesIn = intDown;
        // No Interior ACKs

        // Transform ack messages to simple long stream
        Observable<Long> acks = acksExt.distinct().map(ack -> -ack.getID());

        // ACK all received messages
        Observable<DAPacket> acksFromMessages = messagesExt.map(msg -> DAPacket.AckFromMessage(msg));

        // Make ACKs stop the repeat sending of Messages
        Observable<GroupedObservable<Long, DAPacket>> groupedMessagesIn = messagesIn.distinct()
                .groupBy(DAPacket::getID);

        // Observable<DAPacket> truc = groupedMessagesIn.map(
        // gMsgs -> (Observable<DAPacket>) gMsgs.repeat(100).takeUntil(acks.filter(ack
        // -> ack == gMsgs.getKey())))
        // .flatMap(x -> x);

        Observable<DAPacket> truc = groupedMessagesIn.map(
                gMsgs -> gMsgs.publish(obs -> obs.repeat(100).takeUntil(acks.filter(ack -> ack == gMsgs.getKey()))))
                .flatMap(x -> x);

        // Observable<Object> truc = groupedMessagesIn.map(grp -> grp.subscribe()).map(p
        // -> p.repeat(100));

        // .publish((Observable<DAPacket>) grp.repeat(100).takeUntil(acks.filter(ack ->
        // ack == grp.getKey()))));

        truc.subscribe(p -> System.out.println("Layer truc: " + p.toString()));

        // machin.subscribe(System.out::println);

        // Send to Ext
        acksFromMessages.subscribe(extOut); // acks
        // machin.subscribe(extOut); // and messages to retransmit

        // Send to Int
        messagesExt.distinct().subscribe(intUp);

        return socket;
    }

}