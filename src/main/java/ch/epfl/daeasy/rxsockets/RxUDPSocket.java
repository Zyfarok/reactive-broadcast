package ch.epfl.daeasy.rxsockets;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

import io.reactivex.Observable;

import ch.epfl.daeasy.config.Configuration;

public class RxUDPSocket extends RxSocket<DatagramPacket> {
    final DatagramSocket udpSocket;
    final Observable<DatagramPacket> reader;
    public RxUDPSocket(Configuration cfg) {
        udpSocket = cfg.udpSocket;
        reader = RxUDPReader.create(udpSocket);
    }

    @Override
    public void send(Observable<DatagramPacket> dpOut) {
        RxUDPWriter.create(dpOut, udpSocket);
    }

    @Override
    public Observable<DatagramPacket> get() {
        return reader;
    }
}