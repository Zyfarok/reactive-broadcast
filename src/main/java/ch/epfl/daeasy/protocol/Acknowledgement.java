package ch.epfl.daeasy.protocol;

import java.net.InetSocketAddress;

public class Acknowledgement extends DAPacket {

    /*
     * Build an Acknowledgement from local peer.
     */
    public Acknowledgement(InetSocketAddress peer, long id) {
        super(peer, id);

        if (id >= 0) {
            throw new IllegalArgumentException("ack ID must be < 0");
        }
    }

    /*
     * Build an Acknowledgement from an incoming packet.
     */
    public Acknowledgement(InetSocketAddress peer, byte[] payload) {
        super(peer, payload);

        if (this.payload >= 0) {
            throw new IllegalArgumentException("payload does not correspond to a Acknowledgement");
        }
    }

    public boolean isACK() {
        return true;
    }

    public boolean isMessage() {
        return false;
    }
}