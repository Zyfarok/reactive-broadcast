package ch.epfl.daeasy.layers;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import ch.epfl.daeasy.protocol.DAPacket;
import ch.epfl.daeasy.protocol.MessageContent;
import ch.epfl.daeasy.rxlayers.RxLayer;
import ch.epfl.daeasy.rxsockets.RxSocket;
import io.reactivex.Observable;

public class PerfectLinkLayer<MC extends MessageContent> extends RxLayer<DAPacket<MC>, DAPacket<MC>> {
    private final Function<MC, MC> toAck;

    public PerfectLinkLayer(Function<MC, MC> toAck){
        this.toAck = toAck;
    }

    public RxSocket<DAPacket<MC>> stackOn(RxSocket<DAPacket<MC>> subSocket) {
        Observable<DAPacket<MC>> bottomUp = subSocket.upPipe.share();
        // Incoming Messages
        Observable<DAPacket<MC>> messagesUp = bottomUp.filter(pkt -> pkt.content.isMessage()).share();
        // Incoming ACK
        Observable<DAPacket<MC>> acksUp = bottomUp.filter(pkt -> pkt.content.isAck()).share();

        RxSocket<DAPacket<MC>> socket = new RxSocket<>(messagesUp.distinct());

        // Send ACK for all received messages
        messagesUp.map(msg -> new DAPacket<>(msg.peer, toAck.apply(msg.content)))
                .subscribe(subSocket.downPipe);

        // Replay down-going messages until acked
        socket.downPipe.map(p -> {
            long id = p.content.seq;
            return Observable.just(p)
                    .repeatWhen(completed -> completed.delay(50, TimeUnit.MILLISECONDS))
                    .takeUntil(acksUp
                            .filter(a -> a.content.seq == id)
                            .filter(a -> a.content.pid == p.content.pid)
                            .filter(a -> a.peer.equals(p.peer))
                    );
        }).flatMap(o -> o).subscribe(subSocket.downPipe);

        return socket;
    }

}