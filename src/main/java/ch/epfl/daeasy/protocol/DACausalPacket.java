package ch.epfl.daeasy.protocol;

import java.net.SocketAddress;
import java.util.Map;

public class DACausalPacket extends DAPacket {
    public DACausalPacket(SocketAddress peer, CausalMessageContent content) {
        super(peer, content);
    }
}
