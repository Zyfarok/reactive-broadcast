package ch.epfl.daeasy.config;

import javax.naming.ConfigurationException;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class FIFOConfiguration extends Configuration {

    private final int m; // number of messages to send

    public Mode getMode() {
        return Mode.FIFO;
    }

    public FIFOConfiguration(Integer id, String filepath)
            throws FileNotFoundException, IOException, ConfigurationException, IllegalArgumentException {
        super(id, filepath);
        BufferedReader reader = new BufferedReader(new FileReader(filepath));

        try {
            for (int i = 0; i < this.N + 1; i++) {
                reader.readLine();
            }
            String l = reader.readLine().trim();

            this.m = Integer.parseInt(l);

            // this should be the last line
            String line;
            try {
                line = reader.readLine();
                while (line != null && line.trim().isEmpty()) {
                    // fine
                    line = reader.readLine();
                }
                if (line != null && !line.trim().isEmpty()) {
                    // line is not empty
                    throw new IllegalArgumentException();
                }

            } catch (IOException e) {
                // no next lines
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException("configuration is not a FIFO configuration: ");
        } finally {
            reader.close();
        }
    }

    public int getNumberOfMessages() {
        return this.m;
    }

    public String toString() {
        return "FIFO " + super.toString() + "Number of messages: " + this.m;
    }
}
