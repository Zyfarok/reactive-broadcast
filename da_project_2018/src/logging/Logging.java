package logging;

import java.io.IOException;
import java.util.logging.*;

/*
 * Thread-safe logging on STDOUT.
 */
public final class Logging {
	private final static String name = "da_proc";
	private final static Logger logger = Logger.getLogger(name);
    private final static SimpleFormatter formatter = new SimpleFormatter();
    private static ConsoleHandler ch;
	private static FileHandler fh;
	
	// try to set the FileHandler and ConsoleHandler
	static {
		try {
			fh = new FileHandler(name + ".log");
			fh.setFormatter(formatter);
			fh.setLevel(Level.INFO);
			logger.addHandler(fh);
			
			ch = new ConsoleHandler();
			ch.setFormatter(formatter);
			ch.setLevel(Level.ALL);
			
		} catch (SecurityException | IOException e) {
			System.out.println(e);
			System.exit(1);
		}
	}
	
	public static void debug(String s) {
		synchronized (logger) {
			logger.log(Level.FINE, s);
		}
	}
	
	public static void log(String s) {
		synchronized (logger) {
			logger.log(Level.INFO, s);
		}
	}
	
	public static void flush() {
		synchronized (logger) {
			ch.flush();
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