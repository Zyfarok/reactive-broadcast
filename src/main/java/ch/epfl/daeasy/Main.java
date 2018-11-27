package ch.epfl.daeasy;

import java.util.Arrays;

import ch.epfl.daeasy.config.Configuration;
import ch.epfl.daeasy.config.ConfigurationFactory;
import ch.epfl.daeasy.config.FIFOConfiguration;
import ch.epfl.daeasy.config.LCBConfiguration;
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
        if (args.length < 2) {
            throw new IllegalArgumentException("usage: da_proc pid membership [extra params...]");
        }

        int n = Integer.parseInt(args[0]);
        String membershipFilePath = args[1];
        String[] extraArgs = Arrays.copyOfRange(args, 2, args.length);

        // read membership file
        Configuration cfg;
        try {
            cfg = ConfigurationFactory.build(n, membershipFilePath, extraArgs);
        } catch (Exception e) {
            System.out.println("could not read membership file: " + e);
            return;
        }

        // Logging setup
        Logging.configureFileHandler(String.format("da_proc_%d.out", cfg.id));

        Logging.debug(cfg.toString().replaceAll("\n", "\n\t"));

        Process p = cfg.processesByPID.get(n);

        if (p == null) {
            Logging.debug("could not read process " + n + " in configuration file: " + cfg.toString());
            System.exit(1);
        }

        try {
            switch (cfg.getMode()) {
                case FIFO:
                    Logging.debug("da_proc FIFO " + p.toString() + " running");
                    FIFO.run((FIFOConfiguration) cfg, p, activator);
                    break;
                case LCB:
                    Logging.debug("da_proc LCB " + p.toString() + " running");
                    // TODO : Uncomment this
                    //LCB.run((LCBConfiguration) cfg, p, activator);
                    break;
                default:
                    throw new UnsupportedOperationException("unsupported mode");
            }
        } catch (Exception e) {
            Logging.debug("an error occured: " + e.getMessage());
        }

        return;
    }

}
