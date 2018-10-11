package ch.epfl.daeasy.rxsocket;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

import ch.epfl.daeasy.config.Configuration;
import ch.epfl.daeasy.rxsocket.RxUDPWriter;
import ch.epfl.daeasy.rxsocket.RxUDPReader;
import io.reactivex.Observable;

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