package ch.epfl.daeasy.protocol;

import com.google.common.base.Converter;
import io.reactivex.Observable;

import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.util.Optional;

public class CausalDatagramPacketConverter extends Converter<Observable<DatagramPacket>, Observable<DAPacket<CausalMessageContent>>> {

    public Observable<DAPacket<CausalMessageContent>> doForward(Observable<DatagramPacket> pkts) {
        return pkts.map(pkt -> {
            DAPacket<CausalMessageContent> packet = null;
            try {
                SocketAddress peer = pkt.getSocketAddress();
                CausalMessageContent content = CausalMessageContent
                        .deserialize(new String(pkt.getData(), pkt.getOffset(), pkt.getLength()));
                packet = new DAPacket<>(peer, content);
            } catch (Exception ignored) {
            }
            return Optional.ofNullable(packet);
        }).filter(opkt -> opkt.isPresent()).map(opkt -> opkt.get());
    }

    public Observable<DatagramPacket> doBackward(Observable<DAPacket<CausalMessageContent>> msgs) {
        return msgs.map(msg -> {
            byte[] buf = msg.content.serialize().getBytes();
            return new DatagramPacket(buf, buf.length, msg.peer);
        });
    }
}