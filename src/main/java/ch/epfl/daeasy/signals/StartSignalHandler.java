package ch.epfl.daeasy.signals;

import sun.misc.Signal;
import sun.misc.SignalHandler;

import ch.epfl.daeasy.logging.Logging;

/*
 * Signal Handler for SIGINT and SIGTERM.
 */
public class StartSignalHandler implements SignalHandler {

    private String signalName;
    private SignalHandler oldHandler;

    private StartSignalHandler(String signalName) {
        super();
        this.signalName = signalName;
    }

    public static StartSignalHandler install(String signalName) {
        Signal s = new Signal(signalName);
        StartSignalHandler handler = new StartSignalHandler(signalName);
        handler.oldHandler = Signal.handle(s, handler);
        return handler;
    }

    public void handle(Signal signal) {
        Logging.debug("startsignalhandler: received " + signal.getName());
    }
}
