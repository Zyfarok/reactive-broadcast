package ch.epfl.daeasy.config;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.naming.ConfigurationException;

public class LCBConfiguration extends Configuration {

    final public int m;

    public Mode getMode() {
        return Mode.LCB;
    }

    public LCBConfiguration(int pid, String filepath, int m)
            throws FileNotFoundException, IOException, ConfigurationException, IllegalArgumentException {
        super(pid, filepath);
        this.m = m;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("LCB Configuration ");
        sb.append("(pid: " + this.id + " n: " + this.processesByAddress.size() + " m: " + this.m + ")");
        sb.append(" with processes: \n" + this.processesToString());
        return sb.toString();
    }
}
