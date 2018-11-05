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
        Observable<GroupedObservable<MessageContent, DAPacket>> groupedAcks = subSocket.upPipe.groupBy(DAPacket::getContent).replay(1).autoConnect();

        PublishSubject<MessageContent> delivered = PublishSubject.create();

        RxSocket<MessageContent> socket = new RxSocket<>(delivered);

        Observable<MessageContent> pending = groupedAcks.map(o -> o.getKey()).mergeWith(socket.downPipe).distinct().share();

        pending.flatMap(mc ->
                groupedAcks.filter(o -> o.getKey().equals(mc)).flatMap(o -> o).skip(amountOfRequiredAcksFromOthersToDeliver - 1).take(1)
        ).map(DAPacket::getContent).subscribe(delivered);

        pending.map(mc -> new DAPacket(null, mc)).subscribe(subSocket.downPipe);

        return socket;
    }
}
