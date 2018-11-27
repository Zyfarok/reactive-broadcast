package ch.epfl.daeasy.layers;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import ch.epfl.daeasy.config.Configuration;
import ch.epfl.daeasy.config.Process;
import ch.epfl.daeasy.protocol.MessageContent;
import ch.epfl.daeasy.rxlayers.RxLayer;
import ch.epfl.daeasy.rxsockets.RxSocket;
import ch.epfl.daeasy.logging.Logging;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public class FirstInFirstOutBroadcastLayer<MC extends MessageContent> extends RxLayer<MC, MC> {
    private final Configuration cfg;

    public FirstInFirstOutBroadcastLayer(Configuration cfg) {
        this.cfg = cfg;
    }

    /*
     * Assumes subSocket is a UniformReliableBroadcast
     */
    public RxSocket<MC> stackOn(RxSocket<MC> subSocket) {
        Map<Long, Map<Long, MC>> messages = new HashMap<>(); // pid -> message id -> message
        Map<Long, AtomicLong> nextIDs = new HashMap<>(); // pid -> ID of message to be delivered next

        // initialize data structures
        for (Process p : this.cfg.processesByAddress.values()) {
            messages.put(p.getPID(), new HashMap<>());
            nextIDs.put(p.getPID(), new AtomicLong(1)); // by default, first message is '1'
        }

        Subject<MC> subject = PublishSubject.create();

        RxSocket<MC> socket = new RxSocket<>(subject);

        Subject<MC> intOut = subject;
        Observable<MC> intIn = socket.downPipe;
        Observable<MC> extIn = subSocket.upPipe;
        Subject<MC> extOut = subSocket.downPipe;

        intIn.subscribe(mc -> {
            extOut.onNext(mc);
        }, error -> {
            Logging.debug("error while receiving message from interior at FIFOB: ");
            error.printStackTrace();
        });

        extIn.subscribe(mc -> {
            long seq = mc.seq;
            long remotePID = mc.pid;
            AtomicLong nextId = nextIDs.get(remotePID);
            Map<Long, MC> pendingMessages = messages.get(remotePID);

            synchronized (pendingMessages) {
                if (nextId.get() == seq) { // In this case, we can directly deliver
                    intOut.onNext(mc);
                    nextId.set(seq + 1);

                    // If this "unlocks" previously pending messages, we can deliver them
                    while (pendingMessages.containsKey(nextId.get())) {
                        // deliver (and remove from pending)
                        intOut.onNext(pendingMessages.get(nextId.get()));
                        pendingMessages.remove(nextId.get());
                        // update nextId
                        nextId.set(nextId.get() + 1);
                    }
                } else {
                    assert nextId.get() < seq;
                    // The message can't be delivered yet, so we'll add it to pending
                    pendingMessages.put(seq, mc);
                }
            }
        }, error -> {
            Logging.debug("error while receiving message from exterior at FIFOB: ");
            error.printStackTrace();
        });

        return socket;
    }
}
