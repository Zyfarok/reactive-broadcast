package ch.epfl.daeasy.layers;

import ch.epfl.daeasy.config.Configuration;
import ch.epfl.daeasy.protocol.MessageContent;
import ch.epfl.daeasy.rxlayers.RxLayer;
import ch.epfl.daeasy.rxsockets.RxSocket;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;

import java.util.HashMap;
import java.util.Map;

public class FirstInFirstOutBroadcastLayer<MC extends MessageContent> extends RxLayer<MC, MC> {
    private final Configuration cfg;

    public FirstInFirstOutBroadcastLayer(Configuration cfg) {
        this.cfg = cfg;
    }

    /*
     * Assumes subSocket is a UniformReliableBroadcast
     */
    public RxSocket<MC> stackOn(RxSocket<MC> subSocket) {
        Map<Long, BehaviorSubject<Long>> lastDelivered = new HashMap<>();

        // initialize data structures
        for(Integer pid : cfg.processesByPID.keySet()) {
            lastDelivered.put(pid.longValue(), BehaviorSubject.createDefault(0L));
        }

        Observable<MC> upPipe = subSocket.upPipe.flatMap(mc ->
                lastDelivered.get(mc.pid) // Take the sequence of last delivered ids
                        .filter(seq -> seq == mc.seq - 1).take(1) // wait for the previous message to be delivered
                        .map(seq -> mc) // and then send the current message
        ).map(mc -> {
            // When any message is delivered, notify others.
            lastDelivered.get(mc.pid).onNext(mc.seq);
            return mc;
        });


        RxSocket<MC> socket = new RxSocket<>(upPipe);

        socket.downPipe.subscribe(subSocket.downPipe);

        return socket;
    }
}
