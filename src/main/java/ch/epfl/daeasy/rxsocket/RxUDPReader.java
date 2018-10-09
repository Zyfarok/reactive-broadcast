package ch.epfl.daeasy.rxsocket;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

import io.reactivex.functions.Cancellable;
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableEmitter;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;

class RxUDPReader {

    // static Observable<DatagramPacket> packets(DatagramSocket socket) {
    //     return Observable.<DatagramPacket>create(subscriber -> {
    //         byte[] buf = new byte[socket.getReceiveBufferSize()];
    //         DatagramPacket pkt = new DatagramPacket(buf, buf.length);

    //         while (!socket.isClosed()) {
    //             socket.receive(pkt);
    //             subscriber.onNext(pkt);
    //             if (subscriber.isDisposed()) {
    //                 break;
    //             }
    //         }
    //         subscriber.onComplete();
    //     }).subscribeOn(Schedulers.io());
    // }

    private static Cancellable getCancellable(final DatagramSocket udpSocket) {
        return new Cancellable() {
            @Override
            public void cancel() throws Exception {
                if (!udpSocket.isClosed()) {
                    udpSocket.close();
                }
            }
        };
    }

    public static Observable<DatagramPacket> create(final int portNo, final int bufferSizeInBytes) {
        return Observable.create(
                new ObservableOnSubscribe<DatagramPacket>() {
                    @Override
                    public void subscribe(ObservableEmitter<DatagramPacket> emitter) throws Exception {
                        final DatagramSocket udpSocket = new DatagramSocket(portNo);
                        emitter.setCancellable(RxUDPReader.getCancellable(udpSocket));
                        while (true) {
                            try {
                                byte[] rcvBuffer = new byte[bufferSizeInBytes];
                                DatagramPacket datagramPacket = new DatagramPacket(rcvBuffer, rcvBuffer.length);
                                udpSocket.receive(datagramPacket);
                                emitter.onNext(datagramPacket);
                            } catch (Exception e) {
                                emitter.onError(e);
                            }
                        }
                    }
                }).subscribeOn(Schedulers.io());
}


    public static void main(String[] args) {
        System.out.println("hey");
        try {
            // DatagramSocket socket = new DatagramSocket(8998);
            Observable<DatagramPacket> source = RxUDPReader.create(8998, 1000000).share();
            // ConnectableObservable<DatagramPacket> csource = source.publish();

            source
                .observeOn(Schedulers.trampoline())
                .subscribe(pkt -> System.out.println(new String(pkt.getData(), pkt.getOffset(), pkt.getLength())));

            source
                .observeOn(Schedulers.trampoline())
                .blockingSubscribe(pkt -> System.out.println("NEW PACKET!"));
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}