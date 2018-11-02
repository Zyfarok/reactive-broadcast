package ch.epfl.daeasy.protocol;

import java.net.SocketAddress;

/*
 * A DAPacket is a application-level payload with a correspondent.
 * It represents a received or sent message along with additional information on this message.
 */
public class DAPacket {
    // actual message content sent/received on the wire
    protected final MessageContent content;

    // correspondant peer
    // if this peer is sending the DAPacket, peer is the destination
    // if this peer is receiving the DAPacket, peer is the sender
    protected final SocketAddress peer;

    public DAPacket(SocketAddress peer, MessageContent content) {
        this.content = content;
        this.peer = peer;
    }

    public MessageContent getContent() {
        return this.content;
    }

    public SocketAddress getPeer() {
        return this.peer;
    }

    public String toString() {
        return this.content.toString() + " PEER: " + this.peer.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (!DAPacket.class.isAssignableFrom(obj.getClass())) {
            return false;
        }

        final DAPacket other = (DAPacket) obj;

        return this.content.equals(other.content) && this.peer.equals(other.peer);
    }

    @Override
    public int hashCode() {
        return this.peer.hashCode() * this.content.hashCode();
    }
}