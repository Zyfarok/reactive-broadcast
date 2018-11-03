package ch.epfl.daeasy.layers;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ch.epfl.daeasy.config.Configuration;
import ch.epfl.daeasy.config.Process;
import ch.epfl.daeasy.logging.Logging;
import ch.epfl.daeasy.protocol.DAPacket;
import ch.epfl.daeasy.protocol.MessageContent;
import ch.epfl.daeasy.rxlayers.RxLayer;
import ch.epfl.daeasy.rxsockets.RxSocket;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public class UniformReliableBroadcastLayer extends RxLayer<DAPacket, DAPacket> {

    private final Map<SocketAddress, Process> processesByAddress;
    private final int N;
    private final long pid;
    private final Process process;

    public UniformReliableBroadcastLayer(Configuration cfg) {
        this.pid = cfg.id;
        this.processesByAddress = cfg.processesByAddress;
        this.N = this.processesByAddress.size();
        this.process = cfg.processesByPID.get(cfg.id);
    }

    /*
     * Assumes subSocket is a BestEffortBroadcast
     */
    public RxSocket<DAPacket> stackOn(RxSocket<DAPacket> subSocket) {

        Subject<DAPacket> subject = PublishSubject.create();

        RxSocket<DAPacket> socket = new RxSocket<>(subject);

        Subject<DAPacket> intOut = subject;
        Observable<DAPacket> intIn = socket.downPipe;
        Observable<DAPacket> extIn = subSocket.upPipe;
        Subject<DAPacket> extOut = subSocket.downPipe;

        // Exterior Messages
        Observable<DAPacket> messagesExt = extIn.filter(pkt -> pkt.getContent().isMessage());

        // Interior Messages
        Observable<DAPacket> messagesIn = intIn;

        // upon event <init> do
        // delivered := ∅
        // pending := ∅
        // forall m do ack [m] := ∅
        Set<MessageContent> delivered = new HashSet<>();
        Set<MessageContent> pending = new HashSet<>();
        Map<MessageContent, Set<Long>> ack = new HashMap<>(); // message -> set of process (ids) that acked the message

        // upon event <urbBroadcast, m> do
        // pending := pending U {[self,m]}
        // trigger <bebBroadcast, [Data,self,m]>
        messagesIn.subscribe(pkt -> {
            MessageContent m = pkt.getContent();
            synchronized (pending) {
                pending.add(m);
            }
            extOut.onNext(pkt);
        }, error -> {
            Logging.debug("error while receiving message from interior at URB: ");
            error.printStackTrace();
        });

        // upon event <bebDeliver, pi, [Data,pj,m]> do
        // ack[m] := ack[m] U {pi}
        // if [pj,m] not in pending then
        // pending := pending U {[pj,m]}
        // trigger <bebBroadcast,[Data,pj,m]>
        messagesExt.subscribe(pkt -> {
            MessageContent m = pkt.getContent();

            // // /!\ check if m in delivered
            // if (delivered.contains(m)) {
            // return;
            // }

            if (!this.processesByAddress.containsKey(pkt.getPeer())) {
                throw new RuntimeException(
                        "unknown process relay of packet: " + pkt.toString() + "\n" + this.processesByAddress.keySet());
            }

            // relay is the process that sent us the message, not the origin of the message
            long relayPID = this.processesByAddress.get(pkt.getPeer()).getPID();

            synchronized (ack) {
                ack.putIfAbsent(m, new HashSet<Long>());
                ack.get(m).add(relayPID);
            }

            synchronized (pending) {
                if (!pending.contains(m)) {
                    pending.add(m);
                    extOut.onNext(new DAPacket(this.process.address, m));
                }
            }
            // upon exists (s, m) ∈ pending such that candeliver (m) ∧ m ∈ delivered do
            // delivered := delivered ∪ {m} ;
            // trigger <urb, Deliver | s , m> ;

            synchronized (pending) {

                for (MessageContent smsg : pending) {

                    synchronized (ack) {
                        if (delivered.contains(smsg)) {
                            continue;
                        }
                        if (ack.getOrDefault(smsg, new HashSet<>()).size() >= (this.N) / 2.) {
                            delivered.add(smsg);
                            intOut.onNext(pkt);

                            // // /!\ remove m of pending
                            // pending.remove(smsg);
                        }
                    }
                }
            }

        }, error -> {
            Logging.debug("error while receiving message from exterior at URB: " + error.toString());
            error.printStackTrace();
        });

        return socket;
    }
}
