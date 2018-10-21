package ch.epfl.daeasy;

import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Converter;

import ch.epfl.daeasy.config.Configuration;
import ch.epfl.daeasy.config.Process;
import ch.epfl.daeasy.layers.PerfectLinkLayer;
import ch.epfl.daeasy.logging.Logging;
import ch.epfl.daeasy.protocol.DAPacket;
//import ch.epfl.daeasy.protocol.Message;
import ch.epfl.daeasy.rxlayers.RxGroupedLayer;
import ch.epfl.daeasy.rxlayers.RxLayer;
import ch.epfl.daeasy.rxlayers.RxNil;
import ch.epfl.daeasy.rxsockets.RxLoopbackSocket;
import ch.epfl.daeasy.rxsockets.RxSocket;
import ch.epfl.daeasy.signals.StartSignalHandler;
import ch.epfl.daeasy.signals.StopSignalHandler;
import ch.epfl.daeasy.protocol.DAPacket;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

public class Main {

	public static class MyThread extends Thread {
		private String name;

		public MyThread(String name) {
			super();
			this.name = name;
		}

		public void run() {
			while (true) {
				try {
					Logging.log("thread " + this.name + " running");
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					return;
				} catch (Exception e) {
					System.out.println(e);
					return;
				}
			}
		}
	}

	public static void main(String[] args) {

		// da_proc n membership [extra params...]

		// arguments validation
		if (args.length < 2) {
			throw new IllegalArgumentException("usage: da_proc n membership [extra params...]");
		}

		int n = Integer.parseInt(args[0]);
		String membershipFilePath = args[1];

		// read membership file
		Configuration cfg;
		try {
			cfg = new Configuration(n, membershipFilePath);
		} catch (Exception e) {
			System.out.println("could not read membership file: " + e);
			return;
		}

		Logging.debug(cfg.toString());

		Process p = cfg.processes.get(n);

		if (p == null) {
			System.out.println("could not read process " + n + " in configuration file: " + cfg.toString());
			return;
		}

		Logging.debug("da_proc " + p.toString() + " running");
		Logging.log("da_proc " + p.toString() + " running");

		Thread[] threads = {};
		StopSignalHandler.install("INT", threads);
		StopSignalHandler.install("TERM", threads);
		StartSignalHandler.install("USR2");

		InetSocketAddress extPeer = new InetSocketAddress("10.0.0.2", 9999);
		InetSocketAddress intPeer = new InetSocketAddress("10.0.0.1", 1000);

		DAPacket[] packetsInt = { new DAPacket(intPeer, 1) };

		DAPacket[] packetsExt = { new DAPacket(extPeer, -1) };

		Observable<DAPacket> packetsFromInt = Observable.fromArray(packetsInt).delay(1, TimeUnit.SECONDS);
		Observable<DAPacket> packetsFromExt = Observable.fromArray(packetsExt).delay(3, TimeUnit.SECONDS);

		// feed the subsocket with my ACK for message 1
		RxSocket<DAPacket> subSocket = new RxSocket<DAPacket>(packetsFromExt);

		RxLayer<DAPacket, DAPacket> innerLayer = new PerfectLinkLayer();

		RxSocket<DAPacket> topSocket = subSocket.scheduleOn(Schedulers.trampoline())
				.stack(RxGroupedLayer.create(x -> x, innerLayer)).scheduleOn(Schedulers.trampoline());

		// feed the topsocket with my Message 1
		packetsFromInt.subscribe(topSocket.downPipe);

		// print packets out of the system
		subSocket.downPipe.blockingSubscribe(truc -> System.out.println("OUT: " + truc.toString()));

		System.out.println("Done pushing.");

		try {
			Thread.sleep(2000);
		} catch (InterruptedException ignored) {
		}
	}

}
