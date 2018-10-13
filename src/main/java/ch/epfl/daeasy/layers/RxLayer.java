package ch.epfl.daeasy.layers;

import ch.epfl.daeasy.rxsockets.RxSocket;

/**
 * This is a RxSocket "builder".
 * 
 * @param <A> type of RxSocket that it builds.
 * @param <B> type of RxSocket that it stacks onto.
 */
public abstract class RxLayer<A,B> {
    public abstract RxSocket<A> stackOn(final RxSocket<B> subSocket);

    public <C> RxLayer<A,C> stackOn(final RxLayer<B,C> that) {
        RxLayer<A,B> it = this;
        return new RxLayer<A,C>() {
            @Override
            public RxSocket<A> stackOn(final RxSocket<C> subSocket) {
                return subSocket.stack(that).stack(it);
            }
        };
    }

    public <C> RxLayer<C,B> stack(final RxLayer<C,A> that) {
        return that.stackOn(this);
    }
}