package ch.epfl.daeasy.rxsockets;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

public class RxUDPWriter {
    public static void create(final Observable<DatagramPacket> dpOut, final DatagramSocket udpSocket) {
        dpOut.observeOn(Schedulers.io()).subscribe(dpacket -> udpSocket.send(dpacket));
    }
}