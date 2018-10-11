package ch.epfl.daeasy.config;

import java.net.InetSocketAddress;

public class Process {
    // id
    public final int i;
    // network address
    public final InetSocketAddress address; 
    
    
    public Process(int i, InetSocketAddress address) {
        this.i = i;
        this.address = address;
    }
    
    private String identifier() {
        return String.format("%s:%d", this.address.toString(), this.address.getPort());
    }

    @Override
    public String toString() {
        return new String("Process " + this.i + " @" + this.identifier());
    }
}