package ch.epfl.daeasy.rxsockets;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

import io.reactivex.functions.Cancellable;
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableEmitter;
import io.reactivex.schedulers.Schedulers;

public class RxUDPInputCreator {
    private static Cancellable createCancellable(final DatagramSocket udpSocket) {
        return new Cancellable() {
            @Override
            public void cancel() throws Exception {
                if (!udpSocket.isClosed()) {
                    udpSocket.close();
                }
            }
        };
    }

    public static Observable<DatagramPacket> create(final DatagramSocket udpSocket) {
        return Observable.create(
                new ObservableOnSubscribe<DatagramPacket>() {
                    @Override
                    public void subscribe(ObservableEmitter<DatagramPacket> emitter) throws Exception {
                        emitter.setCancellable(createCancellable(udpSocket));
                        while (true) {
                            try {
                                byte[] rcvBuffer = new byte[1000000]; // TODO : Define the array size in an a better fashion.
                                DatagramPacket datagramPacket = new DatagramPacket(rcvBuffer, rcvBuffer.length);
                                udpSocket.receive(datagramPacket);
                                emitter.onNext(datagramPacket);
                            } catch (Exception e) {
                                emitter.onError(e);
                            }
                        }
                    }
                }
        ).subscribeOn(Schedulers.io());
    }
}