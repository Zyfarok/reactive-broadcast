package ch.epfl.daeasy.protocol;

import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.util.Optional;

import com.google.common.base.Converter;

import io.reactivex.Observable;

public class DatagramPacketConverter extends Converter<Observable<DatagramPacket>, Observable<DAPacket>> {

    public Observable<DAPacket> doForward(Observable<DatagramPacket> pkts) {
        return pkts.map(pkt -> {
            DAPacket packet = null;
            try {
                SocketAddress peer = pkt.getSocketAddress();
                MessageContent content = MessageContent
                        .deserialize(new String(pkt.getData(), pkt.getOffset(), pkt.getLength()));
                packet = new DAPacket(peer, content);
            } catch (Exception ignored) {
            }
            return Optional.ofNullable(packet);
        }).filter(opkt -> opkt.isPresent()).map(opkt -> opkt.get());
    }

    public Observable<DatagramPacket> doBackward(Observable<DAPacket> msgs) {
        return msgs.map(msg -> {
            byte[] buf = msg.content.serialize().getBytes();
            return new DatagramPacket(buf, buf.length, msg.peer);
        });
    }
}