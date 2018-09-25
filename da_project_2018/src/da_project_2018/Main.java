package da_project_2018;

import signal.*;
import logging.Logging;
import java.util.logging.Logger;
import java.lang.*;

public class Main {

	public static void main(String[] args) {		
		
		Logging.log("da_proc: running");
		
		signal.StopSignalHandler.install("TERM");
		signal.StopSignalHandler.install("INT");
		
		while (true) {
			
			try {
				System.out.println("running...");
				Thread.sleep(1000);
			} catch (Exception e) {
				System.out.println("main: exception: " + e);
				
			}
		}
	}

}
