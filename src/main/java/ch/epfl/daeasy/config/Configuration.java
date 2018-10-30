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
	public final Map<Integer, Process> processesByPID;
	public final Map<String, Process> processesByAddress;
	public final Integer id;
	public final Integer N;
	public final DatagramSocket udpSocket;

	/*
	 * Reads file and builds Configuration.
	 */
	public Configuration(Integer id, String filepath)
			throws FileNotFoundException, IOException, ConfigurationException {
		this.id = id;

		Map<Integer, Process> tempProcessesById = new HashMap<Integer, Process>();
		Map<String, Process> tempProcessesByAddress = new HashMap<String, Process>();
		BufferedReader reader = new BufferedReader(new FileReader(filepath));

		// read first line
		String sn = reader.readLine();
		if (sn == null) {
			reader.close();
			throw new ConfigurationException("first line of configuration should be an integer");
		}
		int n = Integer.parseInt(sn);
		try {
			for (int i = 0; i < n; i++) {
				String sp = reader.readLine();
				String[] sps = sp.split(" ");
				if (sps.length != 3) {
					throw new ConfigurationException("expected line 'n ip port', got: " + sp);
				}
				int num = Integer.parseInt(sps[0]);
				int port = Integer.parseInt(sps[2]);

				InetSocketAddress addr = new InetSocketAddress(sps[1], port);
				tempProcessesById.put(i, new Process(num, addr));
				tempProcessesByAddress.put(addr.toString(), new Process(num, addr));
			}
		} finally {
			reader.close();
		}

		this.processesByPID = Collections.unmodifiableMap(tempProcessesById);
		this.processesByAddress = Collections.unmodifiableMap(tempProcessesByAddress);

		this.N = this.processesByPID.size();

		udpSocket = new DatagramSocket(this.processesByPID.get(id).address);
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Membership ");
		sb.append("#" + this.processesByPID.size());
		sb.append(" with processes: \n");
		for (Process p : this.processesByPID.values()) {
			sb.append(p.toString());
			sb.append("\n");
		}
		return sb.toString();
	}
}
