package ch.epfl.daeasy;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import ch.epfl.daeasy.config.FIFOConfiguration;
import ch.epfl.daeasy.layers.*;
import ch.epfl.daeasy.logging.Logging;
import ch.epfl.daeasy.config.Process;
import ch.epfl.daeasy.logging.Logging;
import ch.epfl.daeasy.protocol.DAPacket;
import ch.epfl.daeasy.protocol.DatagramPacketConverter;
import ch.epfl.daeasy.protocol.MessageContent;
import ch.epfl.daeasy.rxlayers.RxGroupedLayer;
import ch.epfl.daeasy.rxlayers.RxLayer;
import ch.epfl.daeasy.rxlayers.RxPipeConverterLayer;
import ch.epfl.daeasy.rxsockets.RxSocket;
import ch.epfl.daeasy.rxsockets.RxUDPSocket;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

import javax.security.auth.Subject;

public class FIFO {

    public static void run(FIFOConfiguration cfg, Process p, Object activator) throws SocketException {
        System.setProperty("rx2.buffer-size", "1024");
        //String pid = "p" + cfg.id + " ";
        // DatagramSocket socket = new DatagramSocket(p.address);
        DatagramSocket socket = new DatagramSocket(p.address);
        // udp socket to rx socket
        RxSocket<DatagramPacket> udpRx = new RxUDPSocket(socket)
        // .stack(new DebugLayer<>(pid + "UDPDeliver : ", pid + "UDPSend : "))
        ;
        // adn the converter layer (DatagramPackets to and from DAPackets)
        RxSocket<DAPacket<MessageContent>> converterSocket = udpRx.stack(new RxPipeConverterLayer<>(new DatagramPacketConverter()))
        // .stack(new DebugLayer<>(pid + "PCDeliver : ", pid + "PCSend : "))
        ;
        // inner layer perfect link for each "link"
        RxLayer<DAPacket<MessageContent>, DAPacket<MessageContent>> perfectLinkLayer =
                new PerfectLinkLayer<>(MessageContent::toAck)
        // .stack(new DebugLayer<>(pid + "PLDeliver : ", pid + "PLSend : "))
        ;
        // add the perfect link layers
        RxSocket<DAPacket<MessageContent>> plSocket = converterSocket// .scheduleOn(Schedulers.trampoline())
                // .stack(RxGroupedLayer.create(x -> x.getPeer().toString(), perfectLinkLayer))
                .stack(perfectLinkLayer);
        // .scheduleOn(Schedulers.trampoline());
        // add the best effort broadcast layer
        RxSocket<DAPacket<MessageContent>> bebSocket = plSocket.stack(new BestEffortBroadcastLayer<>(cfg))
        // .stack(new DebugLayer<>(pid + "BEBDeliver : ", pid + "BEBSend : "))
        ;
        // add the best effort broadcast layer
        RxSocket<MessageContent> urbSocket =
                bebSocket.stack(new UniformReliableBroadcastLayer<>(cfg))
        // .stack(new DebugLayer<>(pid + "URBDeliver : ", pid + "URBSend : "))
        ;
        // add the fifo broadcast layer
        RxSocket<MessageContent> fifoSocket = urbSocket.stack(new FirstInFirstOutBroadcastLayer<>(cfg))
        // .stack(new DebugLayer<>(pid + "FIFODeliver : ", pid + "FIFOSend : "))
        ;

        // logging
        fifoSocket.upPipe.map(mc -> "d " + mc.pid + " " + mc.seq)
                .mergeWith(fifoSocket.downPipe.map(pkt -> "b " + pkt.seq))
                .subscribe(Logging::log, Throwable::printStackTrace);

        AtomicInteger seq = new AtomicInteger(0);
//        for (; i < cfg.m; i++) {
//            outMessages.add(MessageContent.createMessage(p.getPID(), i + 1)));
//        }

        while (true) {
            try {
                synchronized (activator) {
                    activator.wait();
                }
            } catch (Exception e) {
                Logging.debug("error while waiting for USR2: " + e.toString());
                System.exit(-1);
            }

            ;
            Observable.interval(1, TimeUnit.MILLISECONDS).zipWith(
                    Observable.range(0, cfg.m)
                            .map(j -> MessageContent.createMessage(p.getPID(), seq.incrementAndGet())),
                    (i, msg) -> msg
            ).subscribe(fifoSocket.downPipe);
        }
    }
}
