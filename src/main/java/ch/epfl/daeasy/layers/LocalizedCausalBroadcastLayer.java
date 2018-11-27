package ch.epfl.daeasy.layers;

import ch.epfl.daeasy.config.Configuration;
import ch.epfl.daeasy.config.LCBConfiguration;
import ch.epfl.daeasy.config.Process;
import ch.epfl.daeasy.protocol.CausalMessageContent;
import ch.epfl.daeasy.protocol.MessageContent;
import ch.epfl.daeasy.rxlayers.RxLayer;
import ch.epfl.daeasy.rxsockets.RxSocket;
import com.google.common.collect.ImmutableSet;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class LocalizedCausalBroadcastLayer extends RxLayer<CausalMessageContent, MessageContent> {
    private final LCBConfiguration cfg;
    private final ImmutableSet<Integer> dependencies;

    public LocalizedCausalBroadcastLayer(LCBConfiguration cfg) {
        this.cfg = cfg;
        dependencies = cfg.dependencies.get(cfg.id);
    }

    /*
     * Assumes subSocket is a UniformReliableBroadcast
     */
    public RxSocket<MessageContent> stackOn(RxSocket<CausalMessageContent> subSocket) {
        Map<Long, BehaviorSubject<Long>> lastDelivered = new HashMap<>();

        // initialize data structures
        for (Integer p : this.cfg.processesByPID.keySet()) {
            lastDelivered.put(p.longValue(), BehaviorSubject.create());
            lastDelivered.get(p.longValue()).onNext(0L);
        }

        Observable<MessageContent> upPipe = subSocket.upPipe.flatMap(cmc -> // For each message
                Observable.fromIterable(cmc.causes).flatMap(c -> // For each clause
                        lastDelivered.get(c.pid) // Check the deliver status
                                .filter(seq -> seq >= c.pid).take(1) // And wait until the status is valid
                ).skip(cmc.causes.size() - 1).take(1) // Wait until all causes are satisfied
                .map(x -> cmc.withoutCauses()) // And then deliver the message
        ).share();

        // When any message is delivered, notify others.
        upPipe.forEach(mc -> lastDelivered.get(mc.pid).onNext(mc.seq));

        RxSocket<MessageContent> socket = new RxSocket<>(upPipe);

        // Add causes to down-going MessageContents
        socket.downPipe.map(mc -> mc.withCauses(
                dependencies.stream()
                        .map(pid -> new CausalMessageContent.Cause(pid, lastDelivered.get(pid.longValue()).getValue()))
                        .collect(Collectors.toList())
        )).subscribe(subSocket.downPipe);

        return socket;
    }

}
