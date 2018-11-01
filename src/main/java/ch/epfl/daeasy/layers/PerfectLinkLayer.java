package ch.epfl.daeasy.layers;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import ch.epfl.daeasy.protocol.DAPacket;
import ch.epfl.daeasy.protocol.MessageContent;
import ch.epfl.daeasy.rxlayers.RxLayer;
import ch.epfl.daeasy.rxsockets.RxSocket;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public class PerfectLinkLayer extends RxLayer<DAPacket, DAPacket> {

    public RxSocket<DAPacket> stackOn(RxSocket<DAPacket> subSocket) {

        // contains every message ID previously acked
        Set<Long> acked = new HashSet<>();

        Subject<DAPacket> subject = PublishSubject.create();

        RxSocket<DAPacket> socket = new RxSocket<>(subject);

        Subject<DAPacket> intOut = subject;
        Observable<DAPacket> intIn = socket.downPipe;
        Observable<DAPacket> extIn = subSocket.upPipe;
        Subject<DAPacket> extOut = subSocket.downPipe;

        // Exterior Messages
        Observable<DAPacket> messagesExt = extIn.filter(pkt -> pkt.getContent().isMessage());
        // Exterior ACKs
        Observable<DAPacket> acksExt = extIn.filter(pkt -> pkt.getContent().isACK());

        // Interior Messages
        Observable<DAPacket> messagesIn = intIn;
        // No Interior ACKs

        // Transform ack messages to simple long stream
        Observable<Long> acks = acksExt.map(ack -> ack.getContent().getAck().get());

        // add the acks to acked
        acks.subscribe(ack -> acked.add(ack));

        // ACK all received messages
        Observable<DAPacket> acksFromMessages = messagesExt
                .map(msg -> new DAPacket(msg.getPeer(), MessageContent.ackFromMessage(msg.getContent())));

        Observable<DAPacket> messagesOut = messagesIn.map(msg -> {
            return Observable.just(msg).repeatWhen(completed -> completed.delay(50, TimeUnit.MILLISECONDS))
                    .filter(a -> !acked.contains(msg.getContent().getSeq().get()));
        }).flatMap(x -> x);

        // Send to Ext
        acksFromMessages.subscribe(extOut); // acks
        messagesOut.subscribe(extOut); // and messages to retransmit

        // Send to Int
        messagesExt.distinct().subscribe(intOut);

        return socket;
    }

}