package ch.epfl.daeasy.rxsockets;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

import io.reactivex.functions.Cancellable;
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableEmitter;
import io.reactivex.schedulers.Schedulers;

public class RxUDPInputBuilder {
    final DatagramSocket udpSocket;

    public RxUDPInputBuilder(final DatagramSocket udpSocket) {
        this.udpSocket = udpSocket;
    }

    private Cancellable buildCancellable() {
        return new Cancellable() {
            @Override
            public void cancel() throws Exception {
                if (!udpSocket.isClosed()) {
                    udpSocket.close();
                }
            }
        };
    }

    public Observable<DatagramPacket> build() {
        Cancellable cancellable = buildCancellable();
        return Observable.create(
                new ObservableOnSubscribe<DatagramPacket>() {
                    @Override
                    public void subscribe(ObservableEmitter<DatagramPacket> emitter) throws Exception {
                        emitter.setCancellable(cancellable);
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