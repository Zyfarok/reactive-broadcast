package ch.epfl.daeasy;

import ch.epfl.daeasy.config.Configuration;
import ch.epfl.daeasy.config.ConfigurationFactory;
import ch.epfl.daeasy.config.FIFOConfiguration;
import ch.epfl.daeasy.config.Process;
import ch.epfl.daeasy.logging.Logging;
import ch.epfl.daeasy.signals.StartSignalHandler;
import ch.epfl.daeasy.signals.StopSignalHandler;

public class Main {

	public static void main(String[] args) {

		Object activator = new Object();

		Thread[] threads = {};
		StopSignalHandler.install("INT", threads);
		StopSignalHandler.install("TERM", threads);
		StartSignalHandler.install("USR2", activator);

		// da_proc n membership [extra params...]

		// arguments validation
		if (args.length < 3) {
			throw new IllegalArgumentException("usage: da_proc n membership [extra params...]");
		}

		int n = Integer.parseInt(args[0]);
		String membershipFilePath = args[1];
		int m = Integer.parseInt(args[2]);

		// read membership file
		Configuration cfg;
		try {
			cfg = ConfigurationFactory.build(n, membershipFilePath);
		} catch (Exception e) {
			System.out.println("could not read membership file: " + e);
			return;
		}

		// Logging setup
		Logging.configureFileHandler(String.format("da_proc_%d.out", cfg.id));

		Logging.debug(cfg.toString());

		Process p = cfg.processesByPID.get(n);

		if (p == null) {
			System.out.println("could not read process " + n + " in configuration file: " + cfg.toString());
			return;
		}

		Logging.debug("da_proc " + p.toString() + " running");

		try {
			switch (cfg.getMode()) {
			case FIFO:
				FIFO.run((FIFOConfiguration) cfg, p, activator, m);
				break;
			case LCB:
				throw new UnsupportedOperationException("not yet implemented");
			default:
				throw new UnsupportedOperationException("unsupported mode");
			}
		} catch (Exception e) {
			System.out.println("an error occured: " + e.getMessage());
		}

		return;
	}

}
