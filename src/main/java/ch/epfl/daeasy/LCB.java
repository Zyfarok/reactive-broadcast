package ch.epfl.daeasy;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.Observable;

import ch.epfl.daeasy.config.LCBConfiguration;
import ch.epfl.daeasy.config.Process;
import ch.epfl.daeasy.layers.BestEffortBroadcastLayer;
import ch.epfl.daeasy.layers.LocalizedCausalBroadcastLayer;
import ch.epfl.daeasy.layers.PerfectLinkLayer;
import ch.epfl.daeasy.layers.UniformReliableBroadcastLayer;
import ch.epfl.daeasy.logging.Logging;
import ch.epfl.daeasy.protocol.CausalDatagramPacketConverter;
import ch.epfl.daeasy.protocol.CausalMessageContent;
import ch.epfl.daeasy.protocol.DAPacket;
import ch.epfl.daeasy.protocol.MessageContent;
import ch.epfl.daeasy.rxlayers.RxLayer;
import ch.epfl.daeasy.rxlayers.RxPipeConverterLayer;
import ch.epfl.daeasy.rxsockets.RxSocket;
import ch.epfl.daeasy.rxsockets.RxUDPSocket;

public class LCB {

    public static void run(LCBConfiguration cfg, Process p, Object activator) throws SocketException {
        System.setProperty("rx2.buffer-size", "1024");
        // String pid = "p" + cfg.id + " ";
        DatagramSocket socket = new DatagramSocket(p.address);
        // udp socket to rx socket
        RxSocket<DatagramPacket> udpRx = new RxUDPSocket(socket);
        // adn the converter layer (DatagramPackets to and from DAPackets)
        RxSocket<DAPacket<CausalMessageContent>> converterSocket = udpRx
                .stack(new RxPipeConverterLayer<>(new CausalDatagramPacketConverter()));
        // inner layer perfect link for each "link"
        RxLayer<DAPacket<CausalMessageContent>, DAPacket<CausalMessageContent>> perfectLinkLayer = new PerfectLinkLayer<>(
                CausalMessageContent::toAck);
        // add the perfect link layers
        RxSocket<DAPacket<CausalMessageContent>> plSocket = converterSocket.stack(perfectLinkLayer);
        // add the best effort broadcast layer
        RxSocket<DAPacket<CausalMessageContent>> bebSocket = plSocket.stack(new BestEffortBroadcastLayer<>(cfg));
        // add the best uniform reliable broadcast layer
        RxSocket<CausalMessageContent> urbSocket = bebSocket.stack(new UniformReliableBroadcastLayer<>(cfg));
        // add the lcb layer
        RxSocket<MessageContent> lcbSocket = urbSocket.stack(new LocalizedCausalBroadcastLayer(cfg));

        // logging
        lcbSocket.upPipe.map(mc -> "d " + mc.pid + " " + mc.seq)
                .mergeWith(lcbSocket.downPipe.map(pkt -> "b " + pkt.seq))
                .subscribe(Logging::log, Throwable::printStackTrace);

        AtomicInteger seq = new AtomicInteger(0);

        while (true) {
            try {
                synchronized (activator) {
                    activator.wait();
                }
            } catch (Exception e) {
                Logging.debug("error while waiting for USR2: " + e.toString());
                System.exit(-1);
            }

            Observable.range(0, cfg.m)
                    .map(j -> MessageContent.createMessage(p.getPID(), seq.incrementAndGet()))
                    .subscribe(lcbSocket.downPipe);
        }
    }
}
