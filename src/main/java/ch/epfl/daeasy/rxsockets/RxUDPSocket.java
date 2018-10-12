package ch.epfl.daeasy.rxsockets;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class RxUDPSocket extends RxSocket<DatagramPacket> {
    public RxUDPSocket(final DatagramSocket udpSocket) {
        super(RxUDPInputCreator.create(udpSocket), new RxUDPOutputBuilder(udpSocket));
    }
}