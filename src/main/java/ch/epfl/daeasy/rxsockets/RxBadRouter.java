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

    public RxBadRouter(double dropRate, double loopRate, long delayStep, TimeUnit delayUnit) {
        // Packet pass randomly
        Observable<PacketAndPeers> passPipe = downPipe.filter(x -> random.nextDouble() > dropRate);

        passPipe.forEach(x -> System.out.println("passPipe"));
        // Subject that will receive packets to delay
        //Subject<PacketAndPeers> toDelay = PublishSubject.create();



        Subject<PacketAndPeers> couldBeDelayed = PublishSubject.create();
        couldBeDelayed.forEach(x -> System.out.println("couldBeDelayed"));
        passPipe.subscribe(couldBeDelayed);

        // Randomly split packet to delay some
        Observable<GroupedObservable<Boolean, PacketAndPeers>> delayOrNotDelay =
                couldBeDelayed.groupBy(x -> random.nextDouble() < loopRate);
        couldBeDelayed.forEach(x -> System.out.println("delayOrNotDelay"));

        // Delay some messages
        delayOrNotDelay.filter(o -> o.getKey()).flatMap(o -> o).delay(delayStep, delayUnit).subscribe(couldBeDelayed);

        // Send the other messages
        upPipe = delayOrNotDelay.filter(o -> !o.getKey()).flatMap(o -> o).groupBy(x -> x.destination);
        upPipe.forEach(x -> System.out.println("upPipe"));
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
