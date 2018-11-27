package ch.epfl.daeasy.protocol;

import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.util.Optional;

import com.google.common.base.Converter;

import io.reactivex.Observable;

public class DatagramPacketConverter extends Converter<Observable<DatagramPacket>, Observable<DAPacket<MessageContent>>> {

    public Observable<DAPacket<MessageContent>> doForward(Observable<DatagramPacket> pkts) {
        return pkts.map(pkt -> {
            DAPacket<MessageContent> packet = null;
            try {
                SocketAddress peer = pkt.getSocketAddress();
                MessageContent content = MessageContent
                        .deserialize(new String(pkt.getData(), pkt.getOffset(), pkt.getLength()));
                packet = new DAPacket<>(peer, content);
            } catch (Exception ignored) {
            }
            return Optional.ofNullable(packet);
        }).filter(Optional::isPresent).map(Optional::get);
    }

    public Observable<DatagramPacket> doBackward(Observable<DAPacket<MessageContent>> dpkts) {
        return dpkts.map(msg -> {
            byte[] buf = msg.content.serialize().getBytes();
            return new DatagramPacket(buf, buf.length, msg.peer);
        });
    }
}