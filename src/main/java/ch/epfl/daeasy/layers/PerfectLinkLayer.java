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
import io.reactivex.functions.Predicate;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public class PerfectLinkLayer extends RxLayer<DAPacket, DAPacket> {

    public RxSocket<DAPacket> stackOn(RxSocket<DAPacket> subSocket) {
        Observable<DAPacket> bottomUp = subSocket.upPipe.share();
        // Incoming Messages
        Observable<DAPacket> messagesUp = bottomUp.filter(pkt -> pkt.getContent().isMessage()).share();
        // Incoming ACK
        Observable<DAPacket> acksUp = bottomUp.filter(pkt -> pkt.getContent().isACK());

        RxSocket<DAPacket> socket = new RxSocket<>(messagesUp.distinct());

        // Transform ack messages to simple long stream
        Observable<Long> acks = acksUp.map(ack -> ack.getContent().getAck().get());

        // Send ACK for all received messages
        messagesUp.map(msg -> new DAPacket(msg.getPeer(), MessageContent.ackFromMessage(msg.getContent())))
                .subscribe(subSocket.downPipe);

        // Replay down-going messages until acked
        socket.downPipe.map(p -> {
            Long id = p.getContent().getSeq().get();
            return Observable.just(p)
                            .repeatWhen(completed -> completed.delay(50, TimeUnit.MILLISECONDS))
                            .takeUntil(acks.filter(a -> a.equals(id)));
        }).flatMap(o -> o).subscribe(subSocket.downPipe);

        return socket;
    }

}