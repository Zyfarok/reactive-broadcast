package ch.epfl.daeasy.layers;

import ch.epfl.daeasy.config.Configuration;
import ch.epfl.daeasy.protocol.DAPacket;
import ch.epfl.daeasy.rxlayers.RxLayer;
import ch.epfl.daeasy.rxsockets.RxSocket;
import io.reactivex.Observable;

import java.net.InetSocketAddress;
import java.util.stream.Collectors;

public class BestEffortBroadcastLayer extends RxLayer<DAPacket, DAPacket> {

    private final Observable<InetSocketAddress> otherProcessesAddresses;

    public BestEffortBroadcastLayer(Configuration cfg) {
        this.otherProcessesAddresses = Observable.fromIterable(
                cfg.processesByPID.values().stream()
                        .filter(p -> p.getPID() != cfg.id)
                        .map(p -> p.address)
                        .collect(Collectors.toList())
        );
    }

    /*
     * Assumes subSocket is a PerfectLink GroupedLayer
     */
    public RxSocket<DAPacket> stackOn(RxSocket<DAPacket> subSocket) {
        RxSocket<DAPacket> socket = new RxSocket<>(subSocket.upPipe);

        socket.downPipe.flatMap(m -> otherProcessesAddresses
                //.zipWith(Observable.interval(1, TimeUnit.MILLISECONDS),(address, i) -> address)
                .map(address -> new DAPacket(address, m.getContent()))
        ).subscribe(subSocket.downPipe);

        return socket;
    }
}
