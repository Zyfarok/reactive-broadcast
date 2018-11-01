package ch.epfl.daeasy.rxsockets;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Cancellable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class RxUDPSocket extends RxSocket<DatagramPacket> {
    public RxUDPSocket(DatagramSocket udpSocket) {
        this(udpSocket, PublishSubject.create());
    }

    private RxUDPSocket(DatagramSocket udpSocket, PublishSubject<DatagramPacket> udpSender) {
        super(createUDPReceiver(udpSocket, udpSender.observeOn(Schedulers.trampoline()).forEach(udpSocket::send)),
                udpSender);
    }

    private static Cancellable createCancellable(DatagramSocket udpSocket, Disposable sendersDisposable) {
        return () -> {
            // sendersDisposable.dispose();
            if (!udpSocket.isClosed()) {
                // udpSocket.close();
            }
        };
    }

    private static Observable<DatagramPacket> createUDPReceiver(DatagramSocket udpSocket,
            Disposable sendersDisposable) {
        return Observable.create((ObservableOnSubscribe<DatagramPacket>) emitter -> {
            emitter.setCancellable(createCancellable(udpSocket, sendersDisposable));
            while (true) {
                try {
                    byte[] rcvBuffer = new byte[65536];
                    DatagramPacket datagramPacket = new DatagramPacket(rcvBuffer, rcvBuffer.length);
                    udpSocket.receive(datagramPacket);
                    emitter.onNext(datagramPacket);
                } catch (Exception e) {
                    emitter.onError(e);
                }
            }
        }).subscribeOn(Schedulers.io());
    }
}