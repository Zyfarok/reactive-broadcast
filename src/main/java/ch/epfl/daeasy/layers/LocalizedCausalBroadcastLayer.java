package ch.epfl.daeasy.layers;

import ch.epfl.daeasy.config.Configuration;
import ch.epfl.daeasy.config.Process;
import ch.epfl.daeasy.protocol.CausalMessageContent;
import ch.epfl.daeasy.rxlayers.RxLayer;
import ch.epfl.daeasy.rxsockets.RxSocket;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

import java.util.HashMap;
import java.util.Map;

public class LocalizedCausalBroadcastLayer<CMC extends CausalMessageContent> extends RxLayer<CMC, CMC> {
    private final Configuration cfg;

    public LocalizedCausalBroadcastLayer(Configuration cfg) {
        this.cfg = cfg;
    }

    /*
     * Assumes subSocket is a UniformReliableBroadcast
     */
    public RxSocket<CMC> stackOn(RxSocket<CMC> subSocket) {

        Map<Long, BehaviorSubject<Long>> lastDelivered = new HashMap<>();

        // initialize data structures
        for (Process p : this.cfg.processesByAddress.values()) {
            lastDelivered.put(p.getPID(), BehaviorSubject.create());
            lastDelivered.get(p.getPID()).onNext(0L);
        }

        Subject<CMC> subject = PublishSubject.create();

        RxSocket<CMC> socket = new RxSocket<>(subject);

        Subject<CMC> intOut = subject;
        Observable<CMC> intIn = socket.downPipe;
        Observable<CMC> extIn = subSocket.upPipe;
        Subject<CMC> extOut = subSocket.downPipe;

        intIn.subscribe(extOut::onNext, error -> {
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
