package ch.epfl.daeasy.protocol;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.net.InetSocketAddress;
import java.io.ByteArrayInputStream;
import java.net.DatagramPacket;

/*
 * A DAPacket is a application-level payload with a correspondent.
 */
public abstract class DAPacket {
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

    abstract public boolean isACK();

    abstract public boolean isMessage();

    /*
     * Build a DatagramPacket corresponding to the DAPacket.
     */
    public final DatagramPacket toDatagramPacket() {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(this.payload);
        byte[] bs = buffer.array();
        return new DatagramPacket(bs, bs.length);
    }

    /*
     * Build a DAPacket from an inbound packet.
     */
    public static DAPacket buildDAPacket(InetSocketAddress peer, byte[] payload) throws IllegalArgumentException {
        if (payload.length != Long.BYTES) {
            throw new IllegalArgumentException("cannot parse inbound packet: incorrect length");
        }
        ByteBuffer buffer = ByteBuffer.allocate(payload.length);
        buffer.put(payload);
        buffer.flip();
        long id = buffer.getLong();
        System.out.println("ID : " + id);

        if (id > 0) {
            return new Message(peer, payload);
        } else if (id < 0) {
            return new Acknowledgement(peer, payload);
        } else {
            throw new IllegalArgumentException("id of DAPacket cannot be 0");
        }
    }
}