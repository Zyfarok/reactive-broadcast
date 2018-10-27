package ch.epfl.daeasy.layers;

import java.util.Map;

import ch.epfl.daeasy.config.Configuration;
import ch.epfl.daeasy.config.Process;
import ch.epfl.daeasy.protocol.DAPacket;
import ch.epfl.daeasy.rxlayers.RxLayer;
import ch.epfl.daeasy.rxsockets.RxSocket;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public class BestEffortBroadcastLayer extends RxLayer<DAPacket, DAPacket> {

    Map<Integer, Process> processes;

    public BestEffortBroadcastLayer(Configuration cfg) {
        this.processes = cfg.processes;
    }

    /*
     * Assumes subSocket is a PerfectLink GroupedLayer
     */
    public RxSocket<DAPacket> stackOn(RxSocket<DAPacket> subSocket) {

        Subject<DAPacket> subject = PublishSubject.create();

        RxSocket<DAPacket> socket = new RxSocket<>(subject);

        Subject<DAPacket> intOut = subject;
        Observable<DAPacket> intIn = socket.downPipe;
        Observable<DAPacket> extIn = subSocket.upPipe;
        Subject<DAPacket> extOut = subSocket.downPipe;

        // Exterior Messages
        Observable<DAPacket> messagesExt = extIn.filter(DAPacket::isMessage);

        // Interior Messages
        Observable<DAPacket> messagesIn = intIn;

        // upon event <bebBroadcast, m>
        // forall pi in S do
        // trigger < pp2pSend, pi, m>
        messagesIn.subscribe(m -> {
            for (Process p : this.processes.values()) {
                extOut.onNext(new DAPacket(p.address, m.getID()));
            }
        });

        // upon event < pp2pDeliver, pi, m> do
        // trigger < bebDeliver, pi, m>
        messagesExt.subscribe(intOut);

        return socket;
    }
}
