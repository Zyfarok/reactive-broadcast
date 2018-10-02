package ch.epfl.daeasy.config;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;

public class Configuration {
	
	public class Process {
		// id
		private int i;
		// network address
		private InetSocketAddress address; 
		
		
		private Process(int i, InetSocketAddress address) {
			this.i = i;
			this.address = address;
		}
		
		@Override
		public String toString() {
			return new String("Process " + this.i + " @" + this.address.toString());
		}
	}
	
	// number of processes
	private int n;
	private Map<Integer, Process> processes;
	// TODO to be expanded
	
	/*
	 * Reads file and builds Configuration.
	 */
	public Configuration(String filepath) throws FileNotFoundException, IOException, ConfigurationException {
        BufferedReader reader = 
            new BufferedReader(new FileReader(filepath));
        
        // read first line
        String sn = reader.readLine();
        if (sn == null) {
        	throw new ConfigurationException("first line of configuration should be an integer");
        }
        this.n = Integer.parseInt(sn);
        
        this.processes = new HashMap<Integer, Process>();
        for (int i = 0; i < n; i++) {
        	String sp = reader.readLine();
        	String[] sps = sp.split(" ");
        	if (sps.length < 3) {
            	throw new ConfigurationException("expected line 'i ip port', got: " + sp);
        	}
        	int ni = Integer.parseInt(sps[0]);
        	int port = Integer.parseInt(sps[2]);
        	
        	this.processes.put(ni, 
        			new Process(i, new InetSocketAddress(sps[1], port)));
        }
        
        // rest of file
        // TODO to be expanded
        
        reader.close();
	}
	
	public Map<Integer, Process> getProcesses() {
		return new HashMap<Integer, Process>(this.processes);
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Membership ");
		sb.append("#" + this.n);
		sb.append(" with processes: \n");
		for (Process p : this.processes.values()) {
			sb.append(p.toString());
			sb.append("\n");
		}
		return sb.toString();
	}
}

