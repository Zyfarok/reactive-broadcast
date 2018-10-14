package ch.epfl.daeasy.signals;

import sun.misc.Signal;
import sun.misc.SignalHandler;

import ch.epfl.daeasy.logging.Logging;

/*
 * Signal Handler for SIGINT and SIGTERM.
 */
public class StopSignalHandler implements SignalHandler {

	private String signalName;
	private SignalHandler oldHandler;
	private Thread[] threads;

	private StopSignalHandler(String signalName, Thread[] threads) {
		super();
		this.signalName = signalName;
		this.threads = threads;
	}

	public static StopSignalHandler install(String signalName, Thread[] threads) {
		Signal s = new Signal(signalName);
		StopSignalHandler handler = new StopSignalHandler(signalName, threads);
		handler.oldHandler = Signal.handle(s, handler);
		return handler;
	}

	public void handle(Signal signal) {
		Logging.debug("stopsignalhandler: received " + signal.getName());
		for (Thread t : this.threads) {
			t.interrupt();
		}
		Logging.flush();
		System.exit(0);
	}
}
