package ch.epfl.daeasy.layers;

import ch.epfl.daeasy.config.LCBConfiguration;
import ch.epfl.daeasy.protocol.CausalMessageContent;
import ch.epfl.daeasy.protocol.MessageContent;
import ch.epfl.daeasy.rxlayers.RxLayer;
import ch.epfl.daeasy.rxsockets.RxSocket;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;

import java.util.concurrent.atomic.AtomicLong;
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
        // initialize data structures
        ImmutableMap.Builder<Long, BehaviorSubject<Long>> deliveryEventsBuilder = ImmutableMap.builder();
        ImmutableMap.Builder<Long, AtomicLong> lastDeliveredBuilder = ImmutableMap.builder();
        for(Integer pid : cfg.processesByPID.keySet()) {
            deliveryEventsBuilder.put(pid.longValue(), BehaviorSubject.createDefault(0L));
            lastDeliveredBuilder.put(pid.longValue(), new AtomicLong(0L));
        }
        final ImmutableMap<Long, BehaviorSubject<Long>> deliveryEvents = deliveryEventsBuilder.build();
        final ImmutableMap<Long, AtomicLong> lastDelivered = lastDeliveredBuilder.build();

        // Process incoming messages
        Observable<MessageContent> upPipe = subSocket.upPipe.flatMap(cmc ->
                // for each message
                Observable.fromIterable(cmc.causes).flatMap(c ->
                        // for each clause
                        // listen to the delivery events
                        deliveryEvents.get(c.pid)
                                // wait for the previous message to be delivered if not yet delivered
                                .filter(seq -> seq >= c.seq).take(1)
                )
                        // wait until all causes are satisfied
                        .takeLast(1)
                        // and then deliver the current message
                        .map(x -> cmc.withoutCauses())
        ).doOnNext(mc ->
                // make sure to update the delivered message counter before anything else
                lastDelivered.get(mc.pid).incrementAndGet()
        ).share();

        // And lastly, notify others about the delivery
        upPipe.forEach(mc -> deliveryEvents.get(mc.pid).onNext(mc.seq));

        RxSocket<MessageContent> socket = new RxSocket<>(upPipe);

        // add causes to down-going messages
        socket.downPipe.map(mc -> mc.withCauses(
                dependencies.stream()
                        .map(pid ->
                                new CausalMessageContent.Cause(
                                        pid,
                                        lastDelivered.get(pid.longValue()).get()))
                        .collect(Collectors.toList())
        )).subscribe(subSocket.downPipe);

        return socket;
    }

}
