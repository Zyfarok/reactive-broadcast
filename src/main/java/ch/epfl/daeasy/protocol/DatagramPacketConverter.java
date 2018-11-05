package ch.epfl.daeasy.protocol;

import com.google.common.base.Converter;
import com.google.gson.JsonSyntaxException;
import io.reactivex.Observable;

import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.util.Optional;

public class DatagramPacketConverter extends Converter<Observable<DatagramPacket>, Observable<DAPacket>> {

    @Override
    public Observable<DAPacket> doForward(Observable<DatagramPacket> pkts) {
        return pkts.flatMap(pkt -> {
            SocketAddress peer = pkt.getSocketAddress();
            String packetString = new String(pkt.getData(), pkt.getOffset(), pkt.getLength());
            try {
                MessageContent content = MessageContent
                        .deserialize(packetString);
                return Observable.just(new DAPacket(peer, content));
            } catch (JsonSyntaxException ignored) {
                System.err.println("Error encountered in DatagramPacketConverter while converting DatagramPacket to DAPacket, packet ignored :");
                System.err.println("from : "+ peer + " , content : \"" + packetString + "\"");
                return Observable.empty();
            }
        });
    }

    @Override
    public Observable<DatagramPacket> doBackward(Observable<DAPacket> msgs) {
        return msgs.map(msg -> {
            byte[] buf = msg.content.serialize().getBytes();
            return new DatagramPacket(buf, buf.length, msg.peer);
        });
    }
}