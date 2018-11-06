package ch.epfl.daeasy.layers;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import ch.epfl.daeasy.config.Configuration;
import ch.epfl.daeasy.config.Process;
import ch.epfl.daeasy.protocol.DAPacket;
import ch.epfl.daeasy.rxlayers.RxLayer;
import ch.epfl.daeasy.rxsockets.RxSocket;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public class FirstInFirstOutBroadcastLayer extends RxLayer<DAPacket, DAPacket> {
    private final Configuration cfg;

    public FirstInFirstOutBroadcastLayer(Configuration cfg) {
        this.cfg = cfg;
    }

    /*
     * Assumes subSocket is a UniformReliableBroadcast
     */
    public RxSocket<DAPacket> stackOn(RxSocket<DAPacket> subSocket) {
        Map<Long, Map<Long, DAPacket>> messages = new HashMap<>(); // pid -> message id -> message
        Map<Long, AtomicLong> nextIDs = new HashMap<>(); // pid -> ID of message to be delivered next

        // initialize data structres
        for (Process p : this.cfg.processesByAddress.values()) {
            messages.put(p.getPID(), new HashMap<>());
            nextIDs.put(p.getPID(), new AtomicLong(1)); // by default, first message is '1'
        }

        Subject<DAPacket> subject = PublishSubject.create();

        RxSocket<DAPacket> socket = new RxSocket<>(subject);

        Subject<DAPacket> intOut = subject;
        Observable<DAPacket> intIn = socket.downPipe;
        Observable<DAPacket> extIn = subSocket.upPipe;
        Subject<DAPacket> extOut = subSocket.downPipe;

        intIn.subscribe(pkt -> {
            extOut.onNext(pkt);
        }, error -> {
            System.out.println("error while receiving message from interior at FIFOB: ");
            error.printStackTrace();
        });

        extIn.subscribe(pkt -> {
            Long seq = pkt.getContent().getSeq().get();
            Long remotePID = pkt.getContent().getPID();
            AtomicLong nextId = nextIDs.get(remotePID);
            Map<Long, DAPacket> pendingMessages = messages.get(remotePID);

            if (nextId.get() == seq) { // In this case, we can directly deliver
                intOut.onNext(pkt);
                nextId.set(seq + 1);

                // If this "unlocks" previously pending messages, we can deliver them
                synchronized (pendingMessages) {
                    while (pendingMessages.containsKey(nextId.get())) {
                        // deliver (and remove from pending)
                        intOut.onNext(pendingMessages.get(nextId.get()));
                        pendingMessages.remove(nextId.get());
                        // update nextId
                        nextId.set(nextId.get() + 1);
                    }
                }
            } else if (nextId.get() > seq) {
                // already received this sequence number
                throw new RuntimeException("received duplicate sequence number in FirstInFirstOutBroadcastLayer :" + nextId.get() + " > " + pkt.getContent().toString());
            } else {
                // The message can't be delivered yet, so we'll add it to pending
                synchronized (pendingMessages) {
                    pendingMessages.put(seq, pkt);
                }
            }
        }, error -> {
            System.out.println("error while receiving message from exteriror at FIFOB: ");
            error.printStackTrace();
        });

        return socket;
    }
}
