package ch.epfl.daeasy.layers;

import ch.epfl.daeasy.config.Configuration;
import ch.epfl.daeasy.config.Process;
import ch.epfl.daeasy.protocol.DAPacket;
import ch.epfl.daeasy.protocol.MessageContent;
import ch.epfl.daeasy.rxlayers.RxLayer;
import ch.epfl.daeasy.rxsockets.RxSocket;
import io.reactivex.Observable;
import io.reactivex.observables.GroupedObservable;
import io.reactivex.subjects.PublishSubject;

import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class UniformReliableBroadcastLayer extends RxLayer<DAPacket, MessageContent> {

    private final Map<SocketAddress, Process> processesByAddress;
    private final int N;
    private final int amountOfRequiredAcksFromOthersToDeliver;
    private final long pid;
    private final Process process;

    public UniformReliableBroadcastLayer(Configuration cfg) {
        this.pid = cfg.id;
        this.processesByAddress = cfg.processesByAddress;
        this.N = this.processesByAddress.size();
        this.amountOfRequiredAcksFromOthersToDeliver = this.processesByAddress.size() / 2;
        this.process = cfg.processesByPID.get(cfg.id);
    }

    /*
     * Assumes subSocket is a BestEffortBroadcast
     */
    public RxSocket<MessageContent> stackOn(RxSocket<DAPacket> subSocket) {
        PublishSubject<MessageContent> delivered = PublishSubject.create();

        RxSocket<MessageContent> socket = new RxSocket<>(delivered);

        Observable<GroupedObservable<MessageContent, DAPacket>> groupedAcks = subSocket.upPipe.groupBy(DAPacket::getContent);

        groupedAcks.forEach(o -> {
            AtomicInteger ackCount = new AtomicInteger(amountOfRequiredAcksFromOthersToDeliver - 1);
            o.forEach(x -> {
                if (ackCount.getAndDecrement() == 0)
                    delivered.onNext(x.getContent());
            });
        });

        socket.downPipe.map(mc -> new DAPacket(null, mc)).subscribe(subSocket.downPipe);

        return socket;
    }
}
