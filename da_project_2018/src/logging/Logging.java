package logging;

/*
 * Thread-safe logging on STDOUT.
 */
public final class Logging {
	
	public static void log(String s) {
		synchronized (System.out) {
			System.out.println(s);
		}
	}
	
	public static void flush() {
		synchronized (System.out) {
			System.out.flush();
		}
	}
}
