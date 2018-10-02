package ch.epfl.daeasy.logging;

import java.io.IOException;
import java.util.logging.*;

/*
 * Thread-safe logging on STDOUT.
 */
public final class Logging {
	private final static String name = "da_proc";
	private static Logger logger;
    private static SimpleFormatter formatter = new SimpleFormatter();
    private static StreamHandler sh;
	private static FileHandler fh;
	
	// try to set the FileHandler and ConsoleHandler
	static {
		try {
			Logger l0 = Logger.getLogger("");
			l0.removeHandler(l0.getHandlers()[0]);			
			
			logger = Logger.getLogger(name);
			logger.setLevel(Level.ALL);
			
			sh = new StreamHandler(System.out, new SimpleFormatter());
			sh.setLevel(Level.ALL);
			logger.addHandler(sh);

			fh = new FileHandler(name + ".log");
			fh.setFormatter(formatter);
			fh.setLevel(Level.INFO);
			logger.addHandler(fh);			
		} catch (SecurityException | IOException e) {
			System.out.println(e);
			System.exit(1);
		}
	}
	
	public static void debug(String s) {
		synchronized (logger) {
			logger.fine(s);
			sh.flush();
		}
	}
	
	public static void log(String s) {
		synchronized (logger) {
			logger.info(s);
			sh.flush();
		}
	}
	
	public static void flush() {
		synchronized (logger) {
			sh.flush();
			fh.flush();
		}
	}
}


class SimpleFormatter extends Formatter {

    public String format(LogRecord record) {
        StringBuilder builder = new StringBuilder(1000);
        builder.append(formatMessage(record));
        builder.append("\n");
        return builder.toString();
    }
}