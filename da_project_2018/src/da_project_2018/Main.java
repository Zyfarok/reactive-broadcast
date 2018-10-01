package da_project_2018;

import sun.misc.Signal;
import sun.misc.SignalHandler;
import logging.Logging;
import java.util.logging.Logger;
import java.lang.*;
import java.nio.channels.ClosedByInterruptException;

import da_project_2018.*;

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
					System.out.println("thread " + this.name + " running");				
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
		
		Logging.log("da_proc: running");
		
		Thread[] threads = {
			new Main.MyThread("1"),
			new Main.MyThread("2")
		};
		
		 Runtime.getRuntime().addShutdownHook(new ShutdownThread(threads));
				
		
		for (int i = 0; i < threads.length; i++) {
			Thread t = threads[i];
			t.start();
		}
		
		return;
	}
	

}
