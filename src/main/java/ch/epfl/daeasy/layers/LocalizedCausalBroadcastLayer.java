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
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

        Map<Integer, BehaviorSubject<Long>> lastDelivered = new HashMap<>();

        // initialize data structures
        for (Integer p : this.cfg.processesByPID.keySet()) {
            lastDelivered.put(p, BehaviorSubject.create());
            lastDelivered.get(p).onNext(0L);
        }

        Subject<MessageContent> subject = PublishSubject.create();

        RxSocket<MessageContent> socket = new RxSocket<>(subject);

        Subject<MessageContent> intOut = subject;
        Observable<MessageContent> intIn = socket.downPipe;
        Observable<CausalMessageContent> extIn = subSocket.upPipe;
        Subject<CausalMessageContent> extOut = subSocket.downPipe;

        intIn.map(mc -> mc.withCauses(
                this.dependencies.stream()
                        .map(i -> new CausalMessageContent.Cause(i, lastDelivered.get(i).getValue()))
                        .collect(Collectors.toList())
        )).subscribe(extOut::onNext, error -> {
            System.out.println("error while receiving message from interior at LCB: ");
            error.printStackTrace();
        });

        extIn.subscribe(cmc -> {
            long seq = cmc.seq;
            long author = cmc.pid;
            Observable.fromIterable(cmc.causes) // for all causes
                    .flatMap(c ->
                            // we filter to get an output when the cause is met
                            lastDelivered.get(c.pid).filter(x -> x >= c.seq).take(1)
                    ).skip(cmc.causes.size() - 1).take(1) // We check when all causes are met...
                    .forEach(x -> {
                        intOut.onNext(cmc);  // And then deliver.
                        lastDelivered.get(cmc.pid).onNext(cmc.seq); // And inform others about the delivery
                    });
        }, error -> {
            System.out.println("error while receiving message from exterior at LCB: ");
            error.printStackTrace();
        });

        return socket;
    }

}
