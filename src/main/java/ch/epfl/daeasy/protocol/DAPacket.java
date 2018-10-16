package ch.epfl.daeasy.protocol;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.net.InetSocketAddress;
import java.io.ByteArrayInputStream;
import java.net.DatagramPacket;

/*
 * A DAPacket is a application-level payload with a correspondent.
 */
public class DAPacket {
    protected final long payload;
    protected final InetSocketAddress peer;

    protected DAPacket(InetSocketAddress peer, long payload) {
        this.payload = payload;
        this.peer = peer;
    }

    protected DAPacket(InetSocketAddress peer, byte[] payload) {
        this.peer = peer;
        ByteBuffer buffer = ByteBuffer.wrap(payload);
        this.payload = buffer.getLong();
    }

    public boolean isACK() {
        return this.payload < 0;
    }

    public boolean isMessage() {
        return this.payload > 0;
    }
}