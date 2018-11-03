package ch.epfl.daeasy.layers;

import java.util.HashMap;
import java.util.Map;

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
        Map<Long, Long> nextIDs = new HashMap<>(); // pid -> ID of message to be delivered next

        // initialize data structres
        for (Process p : this.cfg.processesByAddress.values()) {
            messages.put(p.getPID(), new HashMap<>());
            nextIDs.put(p.getPID(), (long) 1); // by default, first message is '1'
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

            if (nextIDs.get(remotePID) > seq) {
                // already received this sequence number
                throw new RuntimeException("received duplicate sequence number in FirstInFirstOutBroadcastLayer");
            }

            Map<Long, DAPacket> remoteMessages = messages.get(remotePID);
            remoteMessages.put(seq, pkt);

            while (remoteMessages.containsKey(nextIDs.get(remotePID))) {
                // deliver
                intOut.onNext(remoteMessages.get(nextIDs.get(remotePID)));
                // update nextID
                nextIDs.put(remotePID, nextIDs.get(remotePID) + 1);
            }

        }, error -> {
            System.out.println("error while receiving message from exteriror at FIFOB: ");
            error.printStackTrace();
        });

        return socket;
    }
}
