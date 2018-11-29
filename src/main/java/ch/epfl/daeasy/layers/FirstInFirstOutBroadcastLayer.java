package ch.epfl.daeasy.layers;

import ch.epfl.daeasy.config.Configuration;
import ch.epfl.daeasy.protocol.MessageContent;
import ch.epfl.daeasy.rxlayers.RxLayer;
import ch.epfl.daeasy.rxsockets.RxSocket;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class FirstInFirstOutBroadcastLayer<MC extends MessageContent> extends RxLayer<MC, MC> {
    private final Configuration cfg;

    public FirstInFirstOutBroadcastLayer(Configuration cfg) {
        this.cfg = cfg;
    }

    /*
     * Assumes subSocket is a UniformReliableBroadcast
     */
    public RxSocket<MC> stackOn(RxSocket<MC> subSocket) {
        // initialize data structures
        ImmutableMap.Builder<Long, BehaviorSubject<Long>> deliveryEventsBuilder = ImmutableMap.builder();
        ImmutableMap.Builder<Long, AtomicInteger> lastDeliveredBuilder = ImmutableMap.builder();
        for(Integer pid : cfg.processesByPID.keySet()) {
            deliveryEventsBuilder.put(pid.longValue(), BehaviorSubject.createDefault(0L));
            lastDeliveredBuilder.put(pid.longValue(), new AtomicInteger(0));
        }
        final ImmutableMap<Long, BehaviorSubject<Long>> deliveryEvents = deliveryEventsBuilder.build();
        final ImmutableMap<Long, AtomicInteger> lastDelivered = lastDeliveredBuilder.build();

        // Process incoming messages
        Observable<MC> upPipe = subSocket.upPipe.flatMap(mc -> {
            return deliveryEvents.get(mc.pid) // Take the sequence of last delivered ids
                    .filter(seq -> seq == mc.seq - 1).take(1) // wait for the previous message to be delivered
                    .map(seq ->  mc); // and then send the current message
        }).doOnNext(mc -> lastDelivered.get(mc.pid).incrementAndGet()).share();

        upPipe.forEach(mc -> deliveryEvents.get(mc.pid).onNext(mc.seq));

        RxSocket<MC> socket = new RxSocket<>(upPipe);

        socket.downPipe.subscribe(subSocket.downPipe);

        return socket;
    }
}
