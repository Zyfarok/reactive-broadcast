package ch.epfl.daeasy.layers;

import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import ch.epfl.daeasy.config.Configuration;
import ch.epfl.daeasy.config.Process;
import ch.epfl.daeasy.protocol.DAPacket;
import ch.epfl.daeasy.protocol.MessageContent;
import ch.epfl.daeasy.rxlayers.RxLayer;
import ch.epfl.daeasy.rxsockets.RxSocket;
import io.reactivex.Observable;
import io.reactivex.observables.GroupedObservable;
import io.reactivex.subjects.PublishSubject;

public class UniformReliableBroadcastLayer<MC extends MessageContent> extends RxLayer<DAPacket<MC>, MC> {

    private final Map<SocketAddress, Process> processesByAddress;
    private final int N;
    private final long pid;
    private final Process process;

    public UniformReliableBroadcastLayer(Configuration cfg) {
        this.pid = cfg.id;
        this.processesByAddress = cfg.processesByAddress;
        this.N = this.processesByAddress.size();
        this.process = cfg.processesByPID.get(cfg.id);
    }

    /*
     * Assumes subSocket is a BestEffortBroadcast
     */
    public RxSocket<MC> stackOn(RxSocket<DAPacket<MC>> subSocket) {

        PublishSubject<MC> downPipe = PublishSubject.create();

        // group incoming packets and outgoing packet per MessageContent
        // Each GroupedObservable is equivalent to an element in pending
        // The DAPackets that goes through the GroupedObservable serves as acks.
        Observable<GroupedObservable<MC, DAPacket<MC>>> groupedPendingAndAck =
                subSocket.upPipe.mergeWith(downPipe.map(mc -> new DAPacket<>(null, mc))).groupBy(x -> x.content);

        Observable<MC> delivered = groupedPendingAndAck.flatMap(o -> {
            // Broadcast any message when we see it for the first time
            subSocket.downPipe.onNext(new DAPacket<MC>(null, o.getKey()));

            // Create an ack counter
            AtomicInteger ackCount = new AtomicInteger(N / 2);

            // Count any packet that is not coming from us as an ack
            // let ONE packet pass once we reach the required amount of acks.
            return o.filter(
                    x -> x.peer != null
                            && !x.peer.equals(process.address))
                    .filter(x -> ackCount.decrementAndGet() == 0)
                    .map(x -> x.content);

        });

        return new RxSocket<>(delivered, downPipe);
    }
}
