package ch.epfl.daeasy.rxsockets;

import io.reactivex.Observable;
import io.reactivex.observables.GroupedObservable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class RxBadRouter {
    private final Random random = new Random();
    private final Subject<PacketAndPeers> downPipe = PublishSubject.create();
    private final Observable<GroupedObservable<SocketAddress, PacketAndPeers>> upPipe;

    public RxBadRouter(double dropRate) {
        upPipe = downPipe.filter(x ->  random.nextDouble() > dropRate).groupBy(x -> x.destination).share();
    }

    public RxSocket<DatagramPacket> buildSocket(SocketAddress address) {
        // Create socket that receives messages sent to him.
        RxSocket<DatagramPacket> socket = new RxSocket<DatagramPacket>(
                upPipe.filter(o -> o.getKey().equals(address))
                        .flatMap(o -> o)
                        .map(PacketAndPeers::getUpPacket));
        // Send the packets coming from the socket to other sockets.
        socket.downPipe.map(x -> new PacketAndPeers(x, address)).subscribe(downPipe);
        return socket;
    }

    private class PacketAndPeers {
        final SocketAddress sender;
        final SocketAddress destination;
        final DatagramPacket packet;

        PacketAndPeers(DatagramPacket packet, SocketAddress sender) {
            this.packet = packet;
            this.sender = sender;
            this.destination = packet.getSocketAddress();
        }

        DatagramPacket getUpPacket() {
            return new DatagramPacket(packet.getData(), packet.getOffset(), packet.getLength(), sender);
        }
    }

}
