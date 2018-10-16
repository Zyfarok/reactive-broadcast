package ch.epfl.daeasy;

import ch.epfl.daeasy.config.Configuration;
import ch.epfl.daeasy.config.Process;
import ch.epfl.daeasy.logging.Logging;
//import ch.epfl.daeasy.protocol.Message;
import ch.epfl.daeasy.rxlayers.RxGroupedLayer;
import ch.epfl.daeasy.rxlayers.RxLayer;
import ch.epfl.daeasy.rxlayers.RxNil;
import ch.epfl.daeasy.rxsockets.RxLoopbackSocket;
import ch.epfl.daeasy.rxsockets.RxSocket;
import ch.epfl.daeasy.rxsockets.RxUDPSocket;
import ch.epfl.daeasy.signals.StartSignalHandler;
import ch.epfl.daeasy.signals.StopSignalHandler;
import com.google.common.base.Converter;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

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
        RxSocket<Integer> subSocket = new RxLoopbackSocket<>();

        RxLayer<Integer,Integer> innerLayer = new RxNil<Integer>()
                .scheduleOn(Schedulers.trampoline())
                .convertValues(Converter.from(x -> (x+100), x -> (x-100)))
                /*.scheduleOn(Schedulers.trampoline())*/; // For some reason, this fucks-up the RxLayer type.

        RxSocket<Integer> topSocket = subSocket
                .scheduleOn(Schedulers.trampoline())
                .stack(RxGroupedLayer.create(x -> x % 2, innerLayer))
                .scheduleOn(Schedulers.trampoline());

        Observable<Integer> observableOut = topSocket.upPipe;
        Disposable d1 = observableOut.forEach(System.out::println);

        PublishSubject<Integer> subjectIn = topSocket.downPipe;

        subjectIn.onNext(0);
        subjectIn.onNext(1);
        subjectIn.onNext(2);
        subjectIn.onNext(3);

        Disposable d2 = observableOut.forEach(s -> System.out.println(s + "bis"));

        subjectIn.onNext(4);
        subjectIn.onNext(5);
        subjectIn.onNext(6);
        subjectIn.onNext(7);

        d1.dispose();

        subjectIn.onNext(8);
        subjectIn.onNext(9);
        subjectIn.onNext(10);
        subjectIn.onNext(11);

        d2.dispose();

        System.out.println("Done pushing.");

        try {Thread.sleep(1000);} catch (InterruptedException ignored) {}
    }

}
