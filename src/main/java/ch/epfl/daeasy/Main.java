package ch.epfl.daeasy;

import ch.epfl.daeasy.config.Configuration;
import ch.epfl.daeasy.config.Process;
import ch.epfl.daeasy.logging.Logging;
import ch.epfl.daeasy.protocol.Message;
import ch.epfl.daeasy.rxsockets.RxLoopbackSocket;
import ch.epfl.daeasy.rxsockets.RxUDPSocket;
import ch.epfl.daeasy.signals.StartSignalHandler;
import ch.epfl.daeasy.signals.StopSignalHandler;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
		/*
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
		*/
		// udp.inputPipe.blockingSubscribe(stuff -> System.out.println(stuff));
		RxLoopbackSocket<String> sock = new RxLoopbackSocket<>();

		Disposable d1 = sock.outputPipe.forEach(System.out::println);

		sock.outputPipe.onNext("1");
		sock.outputPipe.onNext("2");
		sock.outputPipe.onNext("3");

		Disposable d2 = sock.inputPipe.forEach(s -> System.out.println(s + "bis"));

		sock.outputPipe.onNext("4");
		sock.outputPipe.onNext("5");
		sock.outputPipe.onNext("6");

		System.out.println("Done pushing.");

		try {Thread.sleep(1000);} catch (InterruptedException ignored) {}
	}

}
