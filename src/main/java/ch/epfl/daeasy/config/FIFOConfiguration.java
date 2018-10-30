package ch.epfl.daeasy.config;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;

import javax.naming.ConfigurationException;

public class FIFOConfiguration extends Configuration {

    public final int m;

    public FIFOConfiguration(Integer id, String filepath)
            throws FileNotFoundException, IOException, ConfigurationException {
        super(id, filepath);
        LineNumberReader reader = new LineNumberReader(new FileReader(filepath));

        try {
            reader.setLineNumber(this.N + 1);
            String l = reader.readLine().trim();

            this.m = Integer.parseInt(l);

        } finally {
            reader.close();
        }
    }

    public String toString() {
        return "FIFO " + super.toString() + "m: " + this.m;
    }
}
