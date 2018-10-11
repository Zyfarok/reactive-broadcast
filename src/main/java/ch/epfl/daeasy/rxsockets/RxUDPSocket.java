package ch.epfl.daeasy.rxsockets;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class RxUDPSocket extends RxSocket<DatagramPacket> {
    public RxUDPSocket(DatagramSocket udpSocket) {
        super(new RxUDPInputBuilder(udpSocket).build(), new RxUDPOutputBuilder(udpSocket));
    }
}