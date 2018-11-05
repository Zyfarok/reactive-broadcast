package ch.epfl.daeasy.config;

import javax.naming.ConfigurationException;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class Configuration {
	public final Map<Integer, Process> processesByPID;
	public final Map<SocketAddress, Process> processesByAddress;
	public final Integer id;
	public final Integer N;

	public static enum Mode {
		FIFO, LCB;
	}

	public abstract Mode getMode();

	public static Mode getMode(String filepath) throws FileNotFoundException, IOException, ConfigurationException {
		// distinguish mode by number of non empty lines
		long lineCount = 0;
		BufferedReader reader = new BufferedReader(new FileReader(filepath));
		int n = -1;
		try {
			String sn = reader.readLine();
			if (sn == null) {
				reader.close();
				throw new ConfigurationException("first line of configuration should be an integer");
			}
			n = Integer.parseInt(sn);
			lineCount += 1;

			String l;
			while ((l = reader.readLine()) != null) {
				if (l.trim().length() > 0) {
					lineCount += 1;
				}
			}
		} finally {
			reader.close();
		}

		if (n == -1) {
			throw new ConfigurationException("incorrect configuration");
		}

		if (lineCount == 2 * n + 1) {
			return Mode.LCB;
		} else if (lineCount == n + 2) {
			return Mode.FIFO;
		} else {
			throw new ConfigurationException("configuration mode not handled");
		}
	}

	/*
	 * Reads file and builds Configuration.
	 */
	protected Configuration(Integer id, String filepath)
			throws FileNotFoundException, IOException, ConfigurationException {
		this.id = id;

		Map<Integer, Process> tempProcessesById = new HashMap<Integer, Process>();
		Map<SocketAddress, Process> tempProcessesByAddress = new HashMap<SocketAddress, Process>();
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
				tempProcessesById.put(num, new Process(num, addr));
				tempProcessesByAddress.put(addr, new Process(num, addr));
			}
		} finally {
			reader.close();
		}

		this.processesByPID = Collections.unmodifiableMap(tempProcessesById);
		this.processesByAddress = Collections.unmodifiableMap(tempProcessesByAddress);

		this.N = this.processesByPID.size();

		if (!this.processesByPID.containsKey(this.id)) {
			throw new ConfigurationException("process with pid: " + this.id + " not present in configuration");
		}
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
