package ch.epfl.daeasy.rxsockets;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Cancellable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

import javax.security.auth.Subject;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class RxUDPSocket extends RxSocket<DatagramPacket> {
    public RxUDPSocket(DatagramSocket udpSocket) {
        this(udpSocket, createUDPOuputSubject(udpSocket));
    }

    private RxUDPSocket(DatagramSocket udpSocket, SubjectAndItsDisposable said) {
        super(createUDPInputObserver(udpSocket, said.disposable), said.subject);
    }

    private static Cancellable createCancellable(DatagramSocket udpSocket, Disposable disposable) {
        return new Cancellable() {
            @Override
            public void cancel() throws Exception {
                disposable.dispose();
                if (!udpSocket.isClosed()) {
                    udpSocket.close();
                }
            }
        };
    }

    private static Observable<DatagramPacket> createUDPInputObserver(DatagramSocket udpSocket, Disposable disposable) {
        return Observable.create(
                new ObservableOnSubscribe<DatagramPacket>() {
                    @Override
                    public void subscribe(ObservableEmitter<DatagramPacket> emitter) throws Exception {
                        emitter.setCancellable(createCancellable(udpSocket, disposable));
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

    private static SubjectAndItsDisposable createUDPOuputSubject(final DatagramSocket udpSocket) {
        PublishSubject<DatagramPacket> subject = PublishSubject.create();
        return new SubjectAndItsDisposable(subject, subject.forEach(udpSocket::send));
    }

    private static class SubjectAndItsDisposable {
        final PublishSubject<DatagramPacket> subject;
        final Disposable disposable;

        private SubjectAndItsDisposable(PublishSubject<DatagramPacket> subject, Disposable disposable) {
            this.subject = subject;
            this.disposable = disposable;
        }
    }
}