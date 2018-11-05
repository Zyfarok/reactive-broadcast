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
    private final Subject<PacketAndPeers> mergedDownPipe = PublishSubject.create();
    private final Observable<GroupedObservable<SocketAddress, PacketAndPeers>> mergedUpPipe;

    public RxBadRouter(double dropRate, double delayRate, long delay, TimeUnit delayUnit) {
        // Packet pass randomly
        Observable<PacketAndPeers> passPipe = mergedDownPipe.filter(x -> random.nextDouble() > dropRate);

        // Randomly split packet to delay some (but not all)
        Observable<GroupedObservable<Boolean, PacketAndPeers>> delayOrNotDelay =
                passPipe.groupBy(x -> random.nextDouble() < delayRate).share();

        // Delay some message, merge with others and group by destination.
        mergedUpPipe = delayOrNotDelay.filter(GroupedObservable::getKey)
                .delay(delay, delayUnit)
                .mergeWith(delayOrNotDelay.filter(o -> !o.getKey()))
                .flatMap(o -> o)
                .groupBy(x -> x.destination).share();
    }

    public RxSocket<DatagramPacket> buildSocket(SocketAddress address) {
        // Create socket that receives messages sent to him.
        RxSocket<DatagramPacket> socket = new RxSocket<>(
                mergedUpPipe.filter(o -> o.getKey().equals(address))
                        .flatMap(o -> o)
                        .map(PacketAndPeers::getUpPacket));
        // Send the packets coming from the socket to other sockets.
        socket.downPipe.map(x -> new PacketAndPeers(x, address)).subscribe(mergedDownPipe);
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
