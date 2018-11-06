package ch.epfl.daeasy.layers;

import ch.epfl.daeasy.config.Configuration;
import ch.epfl.daeasy.config.Process;
import ch.epfl.daeasy.protocol.DAPacket;
import ch.epfl.daeasy.rxlayers.RxLayer;
import ch.epfl.daeasy.rxsockets.RxSocket;
import io.reactivex.Observable;

import java.util.stream.Collectors;

public class BestEffortBroadcastLayer extends RxLayer<DAPacket, DAPacket> {

    private final Observable<Process> processes;

    public BestEffortBroadcastLayer(Configuration cfg) {
        this.processes = Observable.fromIterable(
                cfg.processesByPID.values().stream().filter(p -> p.getPID() != cfg.id).collect(Collectors.toList()));
    }

    /*
     * Assumes subSocket is a PerfectLink GroupedLayer
     */
    public RxSocket<DAPacket> stackOn(RxSocket<DAPacket> subSocket) {
        RxSocket<DAPacket> socket = new RxSocket<>(subSocket.upPipe);

        socket.downPipe.flatMap(m -> processes.map(p -> new DAPacket(p.address, m.getContent())))
                .subscribe(subSocket.downPipe);

        return socket;
    }
}
