package ch.epfl.daeasy;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import ch.epfl.daeasy.config.FIFOConfiguration;
import ch.epfl.daeasy.logging.Logging;
import ch.epfl.daeasy.config.Process;
import ch.epfl.daeasy.layers.BestEffortBroadcastLayer;
import ch.epfl.daeasy.layers.FirstInFirstOutBroadcastLayer;
import ch.epfl.daeasy.layers.PerfectLinkLayer;
import ch.epfl.daeasy.layers.UniformReliableBroadcastLayer;
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

public class FIFO {

	public static void run(FIFOConfiguration cfg, Process p) throws SocketException {
		Logging.debug(p.address.toString());
		// DatagramSocket socket = new DatagramSocket(p.address);
		DatagramSocket socket = new DatagramSocket(p.address);
		// udp socket to rx socket
		RxUDPSocket udpRx = new RxUDPSocket(socket);
		// adn the converter layer (DatagramPackets to and from DAPackets)
		RxSocket<DAPacket> converterSocket = udpRx.stack(new RxPipeConverterLayer<>(new DatagramPacketConverter()));
		// inner layer perfect link for each "link"
		RxLayer<DAPacket, DAPacket> perfectLinkLayer = new PerfectLinkLayer();
		// add the perfect link layers
		RxSocket<DAPacket> plSocket = converterSocket.scheduleOn(Schedulers.trampoline())
				.stack(RxGroupedLayer.create(x -> x.getPeer().toString(), perfectLinkLayer))
				.scheduleOn(Schedulers.trampoline());
		// add the best effort broadcast layer
		RxSocket<DAPacket> bebSocket = plSocket.stack(new BestEffortBroadcastLayer(cfg));
		// add the best effort broadcast layer
		RxSocket<DAPacket> urbSocket = bebSocket.stack(new UniformReliableBroadcastLayer(cfg));
		// add the fifo broadcast layer
		RxSocket<DAPacket> fifoSocket = urbSocket.stack(new FirstInFirstOutBroadcastLayer(cfg));

		List<DAPacket> outMessages = new ArrayList<>();
		for (int i = 0; i < cfg.getNumberOfMessages(); i++) {
			outMessages.add(new DAPacket(p.address, MessageContent.Message(i + 1, p.getPID())));
		}

		Observable<DAPacket> messages = Observable.fromIterable(outMessages).delay(1, TimeUnit.SECONDS);

		messages.subscribe(fifoSocket.downPipe);

		// logging
		fifoSocket.upPipe.subscribe(pkt -> Logging.log(
				"d " + cfg.processesByAddress.get(pkt.getPeer().toString()) + " " + pkt.getContent().getSeq().get()),
				error -> {
					// System.out.println("handled error upPipe");
					error.printStackTrace();
				});

		fifoSocket.downPipe.blockingSubscribe(pkt -> Logging.log("b " + pkt.getContent().getSeq().get()), error -> {
			// System.out.println("handled error downPipe");
			error.printStackTrace();
		});

	}

}
