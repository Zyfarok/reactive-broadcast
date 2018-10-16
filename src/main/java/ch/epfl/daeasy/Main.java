package ch.epfl.daeasy;

import ch.epfl.daeasy.config.Configuration;
import ch.epfl.daeasy.config.Process;
import ch.epfl.daeasy.logging.Logging;
//import ch.epfl.daeasy.protocol.Message;
import ch.epfl.daeasy.rxlayers.RxConverterLayer;
import ch.epfl.daeasy.rxlayers.RxFilterLayer;
import ch.epfl.daeasy.rxlayers.RxGroupedLayer;
import ch.epfl.daeasy.rxsockets.RxLoopbackSocket;
import ch.epfl.daeasy.rxsockets.RxSocket;
import ch.epfl.daeasy.rxsockets.RxUDPSocket;
import ch.epfl.daeasy.signals.StartSignalHandler;
import ch.epfl.daeasy.signals.StopSignalHandler;
import com.google.common.base.Converter;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

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
        RxSocket<Integer> subSocket = new RxLoopbackSocket<>();
		RxSocket<Integer> sock = subSocket.stack(
		        RxGroupedLayer.create(x -> (x % 2),
                        new RxConverterLayer<>(Converter.from(x -> (x+100), x -> (x-100))))
        );

		Observable<Integer> o = sock.inputPipe.publish().autoConnect();

        subSocket.outputPipe.onNext(0);
        subSocket.outputPipe.onNext(1);
        subSocket.outputPipe.onNext(2);
        subSocket.outputPipe.onNext(3);

        Disposable d1 = o.forEach(System.out::println);

        subSocket.outputPipe.onNext(4);
        subSocket.outputPipe.onNext(5);
        subSocket.outputPipe.onNext(6);
        subSocket.outputPipe.onNext(7);

        Disposable d2 = o.forEach(s -> System.out.println(s + "bis"));

        subSocket.outputPipe.onNext(8);
        subSocket.outputPipe.onNext(9);
        subSocket.outputPipe.onNext(10);
        subSocket.outputPipe.onNext(11);

        d1.dispose();
        d2.dispose();

        subSocket.outputPipe.onNext(12);
        subSocket.outputPipe.onNext(13);
        subSocket.outputPipe.onNext(14);
        subSocket.outputPipe.onNext(15);

        Disposable d3 = o.forEach(s -> System.out.println(s + "ter"));

		System.out.println("Done pushing.");


        //PublishSubject<Integer> incomingOutputPipe = PublishSubject.create();
        //Observable<GroupedObservable<K,A>> incomingGroupedOutputPipes = incomingOutputPipe.groupBy(key);

		try {Thread.sleep(1000);} catch (InterruptedException ignored) {}

        d3.dispose();
	}

}
