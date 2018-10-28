package ch.epfl.daeasy.protocol;

import java.net.InetSocketAddress;

/*
 * A DAPacket is a application-level payload with a correspondent.
 * It represents a received or sent message along with additional information on this message.
 */
public class DAPacket {
    // actual message content sent/received on the wire
    protected MessageContent content;

    // correspondant peer
    protected final InetSocketAddress peer;

    public DAPacket(InetSocketAddress peer, MessageContent content) {
        this.content = content;
        this.peer = peer;
    }

    public MessageContent getContent() {
        return this.content;
    }

    public InetSocketAddress getPeer() {
        return this.peer;
    }

    public String toString() {
        return this.content.toString() + " PEER: " + this.peer.toString();
    }
}