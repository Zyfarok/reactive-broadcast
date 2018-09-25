package signal;

import sun.misc.Signal;
import sun.misc.SignalHandler;

import logging.Logging;

/*
 * Signal Handler for SIGINT and SIGTERM.
 */
public class StopSignalHandler implements SignalHandler {
	
	private String signalName;
    private SignalHandler oldHandler;
    
    private StopSignalHandler(String signalName) {
    	super();
    	this.signalName = signalName;
    }

	public static StopSignalHandler install(String signalName) {
		Signal s = new Signal(signalName);
		StopSignalHandler handler = new StopSignalHandler(signalName);
		handler.oldHandler = Signal.handle(s, handler);
		return handler;
	}

	public void handle(Signal signal) {
		Logging.log("stopsignalhandler: received " + signal.getName());
		Logging.flush();
		
		// TODO kill
		// TODO flush logging
		// TODO
		
		System.exit(0);
	}
}
