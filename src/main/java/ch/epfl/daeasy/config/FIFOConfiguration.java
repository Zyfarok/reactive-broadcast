package ch.epfl.daeasy.config;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import javax.naming.ConfigurationException;

public class FIFOConfiguration extends Configuration {

    public Mode getMode() {
        return Mode.FIFO;
    }

    public FIFOConfiguration(Integer id, String filepath)
            throws FileNotFoundException, IOException, ConfigurationException, IllegalArgumentException {
        super(id, filepath);
    }

    public String toString() {
        return "FIFO " + super.toString();
    }
}
