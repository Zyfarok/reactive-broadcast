package ch.epfl.daeasy.layers;

import ch.epfl.daeasy.config.Configuration;
import ch.epfl.daeasy.protocol.MessageContent;
import ch.epfl.daeasy.rxlayers.RxLayer;
import ch.epfl.daeasy.rxsockets.RxSocket;
import com.google.common.collect.ImmutableMap;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;

import java.util.concurrent.atomic.AtomicLong;

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
        ImmutableMap.Builder<Long, AtomicLong> lastDeliveredBuilder = ImmutableMap.builder();
        for(Integer pid : cfg.processesByPID.keySet()) {
            deliveryEventsBuilder.put(pid.longValue(), BehaviorSubject.createDefault(0L));
            lastDeliveredBuilder.put(pid.longValue(), new AtomicLong(0L));
        }
        final ImmutableMap<Long, BehaviorSubject<Long>> deliveryEvents = deliveryEventsBuilder.build();
        final ImmutableMap<Long, AtomicLong> lastDelivered = lastDeliveredBuilder.build();

        // Process incoming messages
        Observable<MC> upPipe = subSocket.upPipe.flatMap(mc ->
                // for each message
                // listen to the delivery events
                deliveryEvents.get(mc.pid)
                        // wait for the previous message to be delivered if not yet delivered
                        .filter(seq -> seq == mc.seq - 1).take(1)
                        // and then deliver the current message
                        .map(seq ->  mc)
        ).doOnNext(mc ->
                // make sure to update the delivered message counter before anything else
                lastDelivered.get(mc.pid).incrementAndGet()
        ).share();

        // And lastly, notify others about the delivery
        upPipe.forEach(mc -> deliveryEvents.get(mc.pid).onNext(mc.seq));

        RxSocket<MC> socket = new RxSocket<>(upPipe);

        // nothing has to be done on down-going messages
        socket.downPipe.subscribe(subSocket.downPipe);

        return socket;
    }
}
