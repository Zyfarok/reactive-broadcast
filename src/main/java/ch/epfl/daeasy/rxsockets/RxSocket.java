package ch.epfl.daeasy.rxsockets;

import io.reactivex.Observable;

public class RxSocket<A> {
    private final Observable<A> inputPipe;
    private final RxOutputBuilder<A> outputBuilder;

    public RxSocket(Observable<A> in, RxOutputBuilder<A> ob) {
        this.inputPipe = in;
        this.outputBuilder = ob;
    }

    public Observable<A> getInputPipe() {
        return inputPipe;
    }
    
    public void setOutputPipe(Observable<A> out) {
        outputBuilder.build(out);
    }
}