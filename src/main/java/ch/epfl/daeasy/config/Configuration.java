package ch.epfl.daeasy.config;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.naming.ConfigurationException;

public class Configuration {
	public final Map<Integer, Process> processes;
	public final Integer id;
	public final DatagramSocket udpSocket;
	
	/*
	 * Reads file and builds Configuration.
	 */
	public Configuration(Integer id, String filepath) throws FileNotFoundException, IOException, ConfigurationException {
		this.id = id;
		
		Map<Integer, Process> tempProcesses = new HashMap<Integer, Process>();
		BufferedReader reader = new BufferedReader(new FileReader(filepath));
		try {
			String sp = reader.readLine();
			for (int i = 0; sp != null; i++, sp = reader.readLine()) {
				String[] sps = sp.split(" ");
				if (sps.length != 2) {
					throw new ConfigurationException("expected line 'ip port', got: " + sp);
				}
				int port = Integer.parseInt(sps[1]);
				
				tempProcesses.put(i, new Process(i, new InetSocketAddress(sps[0], port)));
			}
		} finally {
			reader.close();
		}

		this.processes = Collections.unmodifiableMap(tempProcesses);

		udpSocket = new DatagramSocket(this.processes.get(id).address);
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Membership ");
		sb.append("#" + this.processes.size());
		sb.append(" with processes: \n");
		for (Process p : this.processes.values()) {
			sb.append(p.toString());
			sb.append("\n");
		}
		return sb.toString();
	}
}

