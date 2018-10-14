package ch.epfl.daeasy;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;

import ch.epfl.daeasy.config.Configuration;
import ch.epfl.daeasy.config.Process;
import ch.epfl.daeasy.logging.Logging;
import ch.epfl.daeasy.protocol.Message;
import ch.epfl.daeasy.rxsockets.RxSocketLoopback;
import ch.epfl.daeasy.rxsockets.RxUDPSocket;
import ch.epfl.daeasy.rxsockets.RxSocketLoopback;
import ch.epfl.daeasy.signals.StartSignalHandler;
import ch.epfl.daeasy.signals.StopSignalHandler;;

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

		RxUDPSocket udp = new RxUDPSocket(cfg.udpSocket);

		// udp.inputPipe.blockingSubscribe(stuff -> System.out.println(stuff));
		List<DatagramPacket> pktList = new ArrayList<DatagramPacket>();
		pktList.add(new Message(new InetSocketAddress("peer", 10000), 1).toDatagramPacket());
		pktList.add(new Message(new InetSocketAddress("peer", 10000), 2).toDatagramPacket());
		pktList.add(new Message(new InetSocketAddress("peer", 10000), 3).toDatagramPacket());
		RxSocketLoopback<DatagramPacket> sock = new RxSocketLoopback<>(
				Observable.fromIterable(pktList).delay(1, TimeUnit.SECONDS));

		sock.outputPipe.blockingSubscribe(machin -> System.out.println(machin));
		return;
	}

}
