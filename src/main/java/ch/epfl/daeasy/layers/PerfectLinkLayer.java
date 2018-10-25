package ch.epfl.daeasy.layers;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import ch.epfl.daeasy.protocol.DAPacket;
import ch.epfl.daeasy.rxlayers.RxLayer;
import ch.epfl.daeasy.rxsockets.RxSocket;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public class PerfectLinkLayer extends RxLayer<DAPacket, DAPacket> {


    // contains every message ID previously acked
    private Set<Long> acked = new HashSet<>();

    public RxSocket<DAPacket> stackOn(RxSocket<DAPacket> subSocket) {

        Subject<DAPacket> subject = PublishSubject.create();

        RxSocket<DAPacket> socket = new RxSocket<>(subject);

        Subject<DAPacket> intOut = subject;
        Observable<DAPacket> intIn = socket.downPipe;
        Observable<DAPacket> extIn = subSocket.upPipe;
        Subject<DAPacket> extOut = subSocket.downPipe;

        // Exterior Messages
        Observable<DAPacket> messagesExt = extIn.filter(DAPacket::isMessage);
        // Exterior ACKs
        Observable<DAPacket> acksExt = extIn.filter(DAPacket::isACK);

        // Interior Messages
        Observable<DAPacket> messagesIn = intIn;
        // No Interior ACKs

        // Transform ack messages to simple long stream
        Observable<Long> acks = acksExt.map(ack -> -ack.getID());

        // add the acks to acked
        acks.subscribe(ack -> acked.add(ack));

        // ACK all received messages
        Observable<DAPacket> acksFromMessages = messagesExt.map(msg -> DAPacket.AckFromMessage(msg));

        Observable<DAPacket> messagesOut = 
            messagesIn
                .map(msg -> {
                    return Observable.just(msg)
                        .repeatWhen(completed -> completed.delay(1, TimeUnit.SECONDS))
                        .filter(a -> !acked.contains(msg.getID()));
                        })
                .flatMap(x -> x);

        // Send to Ext
        acksFromMessages.subscribe(extOut); // acks
        messagesOut.subscribe(extOut); // and messages to retransmit

        // Send to Int
        messagesExt.distinct().subscribe(intOut);

        return socket;
    }

}