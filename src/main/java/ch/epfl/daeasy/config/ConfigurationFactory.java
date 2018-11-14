package ch.epfl.daeasy.config;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.naming.ConfigurationException;

public class ConfigurationFactory {

    // builds a Configuration from a file and the pid of the process
    public static Configuration build(int pid, String filepath, String[] extra_args)
            throws FileNotFoundException, IOException, ConfigurationException, IllegalArgumentException {
        Configuration.Mode mode = Configuration.getMode(filepath);

        switch (mode) {
        case LCB: {
            if (extra_args.length < 1) {
                throw new IllegalArgumentException("LCB mode requires m as extra argument");
            }
            int m = Integer.parseInt(extra_args[0]);
            return new LCBConfiguration(pid, filepath, m);
        }
        case FIFO: {
            if (extra_args.length < 1) {
                throw new IllegalArgumentException("FIFO mode requires m as extra argument");
            }
            int m = Integer.parseInt(extra_args[0]);
            return new FIFOConfiguration(pid, filepath, m);
        }
        default:
            throw new IllegalArgumentException("configuration mode not handled");
        }

    }
}