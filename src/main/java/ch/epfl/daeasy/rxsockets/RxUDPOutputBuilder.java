package ch.epfl.daeasy.rxsockets;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

public class RxUDPOutputBuilder extends RxOutputBuilder<DatagramPacket> {
    final DatagramSocket udpSocket;

    public RxUDPOutputBuilder(final DatagramSocket us) {
        this.udpSocket = us;
    }

    public void buildOutputPipe(final Observable<DatagramPacket> dpOut) {
        dpOut.observeOn(Schedulers.io()).subscribe(dpacket -> udpSocket.send(dpacket));
    }
}