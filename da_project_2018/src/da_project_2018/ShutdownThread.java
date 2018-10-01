package da_project_2018;

import logging.Logging;

public class ShutdownThread extends Thread {
	private final Thread[] threads;
	
	/*
	 * Initializes a ShutdownThread hook.
	 * @param 	threads		array of Thread to be interrupted once stop signal has been received
	 */
	public ShutdownThread(Thread[] threads) {
		int N = threads.length;
		this.threads = new Thread[N];
		for (int i = 0; i < N; i++) {
			this.threads[i] = threads[i];
		}
	}
	
	/*
	 * Logic executed after a stop signal is received, and before the JVM actual stop.
	 * @see java.lang.Thread#run()
	 */
	public void run()  {
		Logging.debug("shutting down...");
		
		// stop threads
		for (Thread t: this.threads) {
			t.interrupt();
		}
		
		// flush logs
		Logging.flush();
		
		return;
	}
}
	