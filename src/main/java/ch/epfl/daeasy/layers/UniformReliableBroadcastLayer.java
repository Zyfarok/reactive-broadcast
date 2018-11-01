package ch.epfl.daeasy.layers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ch.epfl.daeasy.config.Configuration;
import ch.epfl.daeasy.config.Process;
import ch.epfl.daeasy.protocol.DAPacket;
import ch.epfl.daeasy.protocol.MessageContent;
import ch.epfl.daeasy.rxlayers.RxLayer;
import ch.epfl.daeasy.rxsockets.RxSocket;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public class UniformReliableBroadcastLayer extends RxLayer<DAPacket, DAPacket> {

    /*
     * A pair <PID, MessageContent> that represents a message sent by process with
     * given PID The sender may not be the origin of the message.
     */
    private class SourceMessagePair {
        protected final long source; // PID of the source
        protected final MessageContent message; // actual message

        private SourceMessagePair(long pid, MessageContent message) {
            this.source = pid;
            this.message = message;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }

            if (!SourceMessagePair.class.isAssignableFrom(obj.getClass())) {
                return false;
            }

            final SourceMessagePair other = (SourceMessagePair) obj;
            if (other.message == null) {
                return false;
            }
            return this.message.equals(other.message) && this.source == other.source;
        }

        @Override
        public int hashCode() {
            return (int) this.source * this.message.hashCode();
        }

    }

    private final Map<String, Process> processesByAddress;
    private final int N;
    private final long pid;

    public UniformReliableBroadcastLayer(Configuration cfg) {
        this.pid = cfg.id;
        this.processesByAddress = cfg.processesByAddress;
        this.N = this.processesByAddress.size();
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
        Set<SourceMessagePair> pending = new HashSet<>();
        Map<MessageContent, Set<Long>> ack = new HashMap<>(); // message -> set of process (ids) that acked the message

        // upon event <urbBroadcast, m> do
        // pending := pending U {[self,m]}
        // trigger < bebBroadcast, [Data,self,m]>
        messagesIn.subscribe(pkt -> {
            pending.add(new SourceMessagePair(this.pid, pkt.getContent()));
            extOut.onNext(pkt);
        }, error -> {
            System.out.println("error while receiving message from interior at URB: ");
            error.printStackTrace();
        });

        // upon event <bebDeliver, pi, [Data,pj,m]> do
        // ack[m] := ack[m] U {pi}
        // if [pj,m] not in pending then
        // pending := pending U {[pj,m]}
        // trigger < bebBroadcast,[Data,pj,m]>
        messagesExt.subscribe(pkt -> {
            ack.putIfAbsent(pkt.getContent(), new HashSet<Long>());
            long s = this.processesByAddress.get(pkt.getPeer()).getPID();
            MessageContent m = pkt.getContent();
            SourceMessagePair sm = new SourceMessagePair(s, m);

            // TODO check if m in delivered

            if (!pending.contains(sm)) {
                pending.add(sm);
                extOut.onNext(pkt);

                // upon exists (s, m) ∈ pending such that candeliver (m) ∧ m ∈ delivered do
                // delivered := delivered ∪ {m} ;
                // trigger <urb, Deliver | s , m> ;

                for (SourceMessagePair smsg : pending) {
                    if (delivered.contains(smsg.message)) {
                        continue;
                    }

                    if (ack.getOrDefault(smsg.message, new HashSet<>()).size() > this.N / 2.) {
                        delivered.add(smsg.message);
                        intOut.onNext(pkt);

                        // TODO remove m of pending
                    }
                }
            }

        }, error -> {
            System.out.println("error while receiving message from exterior at URB: ");
            error.printStackTrace();
        });

        //

        return socket;
    }
}
