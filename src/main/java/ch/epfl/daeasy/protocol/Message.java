package ch.epfl.daeasy.protocol;

import java.net.InetSocketAddress;

public class Message extends DAPacket {

    /*
     * Build a Message from local peer.
     */
    public Message(InetSocketAddress peer, long id) {
        super(peer, id);

        if (id <= 0) {
            throw new IllegalArgumentException("message ID must be > 0");
        }
    }

    /*
     * Build a Message from an incoming packet.
     */
    public Message(InetSocketAddress peer, byte[] payload) {
        super(peer, payload);

        if (this.payload <= 0) {
            throw new IllegalArgumentException("payload does not correspond to a Message");
        }
    }

    public boolean isACK() {
        return false;
    }

    public boolean isMessage() {
        return true;
    }
}