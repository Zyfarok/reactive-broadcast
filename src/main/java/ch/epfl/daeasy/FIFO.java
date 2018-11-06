package ch.epfl.daeasy;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

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

    public static void run(FIFOConfiguration cfg, Process p, Object activator, int m) throws SocketException {
        String pid = "p" + cfg.id + " ";
        Logging.debug(p.address.toString());
        // DatagramSocket socket = new DatagramSocket(p.address);
        DatagramSocket socket = new DatagramSocket(p.address);
        // udp socket to rx socket
        RxSocket<DatagramPacket> udpRx = new RxUDPSocket(socket)
        // .stack(new DebugLayer<>(pid + "UDPDeliver : ", pid + "UDPSend : "))
        ;
        // adn the converter layer (DatagramPackets to and from DAPackets)
        RxSocket<DAPacket> converterSocket = udpRx.stack(new RxPipeConverterLayer<>(new DatagramPacketConverter()))
        // .stack(new DebugLayer<>(pid + "PCDeliver : ", pid + "PCSend : "))
        ;
        // inner layer perfect link for each "link"
        RxLayer<DAPacket, DAPacket> perfectLinkLayer = new PerfectLinkLayer()
        // .stack(new DebugLayer<>(pid + "PLDeliver : ", pid + "PLSend : "))
        ;
        // add the perfect link layers
        RxSocket<DAPacket> plSocket = converterSocket// .scheduleOn(Schedulers.trampoline())
                // .stack(RxGroupedLayer.create(x -> x.getPeer().toString(), perfectLinkLayer))
                .stack(perfectLinkLayer);
        // .scheduleOn(Schedulers.trampoline());
        // add the best effort broadcast layer
        RxSocket<DAPacket> bebSocket = plSocket.stack(new BestEffortBroadcastLayer(cfg))
        // .stack(new DebugLayer<>(pid + "BEBDeliver : ", pid + "BEBSend : "))
        ;
        // add the best effort broadcast layer
        RxSocket<DAPacket> urbSocket = bebSocket.stack(new UniformReliableBroadcastLayer(cfg))
        // .stack(new DebugLayer<>(pid + "URBDeliver : ", pid + "URBSend : "))
        ;
        // add the fifo broadcast layer
        RxSocket<DAPacket> fifoSocket = urbSocket.stack(new FirstInFirstOutBroadcastLayer(cfg))
        // .stack(new DebugLayer<>(pid + "FIFODeliver : ", pid + "FIFOSend : "))
        ;

        // logging
        fifoSocket.upPipe.subscribe(
                pkt -> Logging.log("d " + pkt.getContent().getPID() + " " + pkt.getContent().getSeq().get()), error -> {
                    // System.out.println("handled error upPipe");
                    error.printStackTrace();
                });

        fifoSocket.downPipe.subscribe(pkt -> Logging.log("b " + pkt.getContent().getSeq().get()), error -> {
            // System.out.println("handled error downPipe");
            error.printStackTrace();
        });

        List<DAPacket> outMessages = new ArrayList<>();
        for (int i = 0; i < m; i++) {
            outMessages.add(new DAPacket(p.address, MessageContent.Message(i + 1, p.getPID())));
        }

        while (true) {
            try {
                synchronized (activator) {
                    activator.wait();
                }
            } catch (Exception e) {
                Logging.debug("error while waiting for USR2: " + e.toString());
                System.exit(-1);
            }

            Observable.interval(5, TimeUnit.MILLISECONDS).zipWith(outMessages, (i, msg) -> msg)
                    .subscribe(fifoSocket.downPipe);
        }
    }
}
