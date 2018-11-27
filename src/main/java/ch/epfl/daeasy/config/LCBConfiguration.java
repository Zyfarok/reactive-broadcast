package ch.epfl.daeasy.config;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.naming.ConfigurationException;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.apache.commons.lang3.StringUtils;

public class LCBConfiguration extends Configuration {

    final public int m;
    final public ImmutableMap<Integer, ImmutableSet<Integer>> dependencies;

    public Mode getMode() {
        return Mode.LCB;
    }

    public LCBConfiguration(int pid, String filepath, int m)
            throws FileNotFoundException, IOException, ConfigurationException, IllegalArgumentException {
        super(pid, filepath);
        this.m = m;

        // reading file for dependencies

        long lineCount = 0;
        BufferedReader reader = new BufferedReader(new FileReader(filepath));
        try {
            String sn = reader.readLine();
            if (sn == null) {
                reader.close();
                throw new ConfigurationException("first line of configuration should be an integer");
            }

            // read through N+1 lines
            String l;
            while ((l = reader.readLine()) != null && lineCount < this.N - 1) {
                if (l.trim().length() > 0) {
                    lineCount += 1;
                }
            }

            Map<Integer, ImmutableSet<Integer>> dependencies = new HashMap<>();

            // begin parsing each line
            while ((l = reader.readLine()) != null) {
                if (l.trim().length() > 0) {
                    lineCount += 1;

                    Stream<Integer> intstream = Pattern.compile(" ").splitAsStream(l)
                            .filter(s -> StringUtils.isNumeric(s)).map(s -> Integer.parseInt(s))
                            .filter(id -> this.processesByPID.keySet().contains(id));

                    Optional<Integer> cid = Pattern.compile(" ").splitAsStream(l).limit(1)
                            .filter(s -> StringUtils.isNumeric(s)).map(s -> Integer.parseInt(s))
                            .filter(id -> this.processesByPID.keySet().contains(id)).findFirst();

                    if (cid.isPresent()) {
                        dependencies.put(cid.get(), ImmutableSet.copyOf(intstream.collect(Collectors.toSet())));
                    }
                }
            }

            for (Integer cid : this.processesByPID.keySet()) {
                if (dependencies.get(cid) == null) {
                    dependencies.put(cid, ImmutableSet.copyOf(new HashSet<Integer>(Arrays.asList(cid))));
                }
            }

            this.dependencies = ImmutableMap.copyOf(dependencies);

        } finally {
            reader.close();
        }

    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("LCB Configuration ");
        sb.append("(pid: " + this.id + " n: " + this.processesByAddress.size() + " m: " + this.m + ")");
        sb.append(" with processes: \n" + this.processesToString());
        sb.append("Dependencies:\n");
        for (Map.Entry<Integer, ImmutableSet<Integer>> e : this.dependencies.entrySet()) {
            sb.append("\t" + e.getKey() + ":");
            for (Integer o : e.getValue()) {
                sb.append(" " + o);
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
