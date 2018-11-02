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
    private Object activator;

    private StartSignalHandler(String signalName, Object activator) {
        super();
        this.signalName = signalName;
        this.activator = activator;
    }

    public static StartSignalHandler install(String signalName, Object activator) {
        Signal s = new Signal(signalName);
        StartSignalHandler handler = new StartSignalHandler(signalName, activator);
        handler.oldHandler = Signal.handle(s, handler);
        return handler;
    }

    public void handle(Signal signal) {
        Logging.debug("startsignalhandler: received " + signal.getName());
        synchronized (activator) {
            this.activator.notifyAll();
        }
    }
}
