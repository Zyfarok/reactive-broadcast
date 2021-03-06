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

    public long getPID() {
        return this.i;
    }

    private String identifier() { return this.address.toString();}

    @Override
    public String toString() {
        return "pid: " + this.i + " @:" + this.identifier();
    }
}