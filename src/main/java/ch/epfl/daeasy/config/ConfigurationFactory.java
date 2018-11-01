package ch.epfl.daeasy.config;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.naming.ConfigurationException;

public class ConfigurationFactory {
    public static Configuration build(Integer id, String filepath)
            throws FileNotFoundException, IOException, ConfigurationException, IllegalArgumentException {
        Configuration.Mode mode = Configuration.getMode(filepath);

        switch (mode) {
        case LCB:
            throw new UnsupportedOperationException("not yet implemented");
        case FIFO:
            return new FIFOConfiguration(id, filepath);
        default:
            throw new IllegalArgumentException("configuration mode not handled");
        }

    }
}