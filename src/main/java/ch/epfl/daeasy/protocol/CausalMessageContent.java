package ch.epfl.daeasy.protocol;

import com.google.common.collect.ImmutableMap;

public class CausalMessageContent extends MessageContent {
    private static final ImmutableMap<Long, Long> empty = ImmutableMap.<Long, Long>builder().build();
    private final ImmutableMap<Long, Long> causes;

    private CausalMessageContent(long pid, long seq) {
        super(pid, seq);
        this.causes = empty;
    }

    private CausalMessageContent(long pid, long seq, String payload, ImmutableMap<Long, Long> causes) {
        super(pid, seq, payload);
        this.causes = causes;
    }

    public static CausalMessageContent createMessage(long pid, long seq, String payload, ImmutableMap<Long, Long> causes) {
        return new CausalMessageContent(pid, seq, payload, causes);
    }

    public static CausalMessageContent createMessage(long pid, long seq, ImmutableMap<Long, Long> causes) {
        return createMessage(pid, seq, Long.toString(pid) + "->" + Long.toString(seq), causes);
    }

    public static CausalMessageContent createAck(long pid, long seq) {
        return new CausalMessageContent(pid, seq);
    }

    /*
     * Construct an ACK from a Message
     */
    @Override
    public CausalMessageContent toAck() {
        assert this.isMessage();

        return CausalMessageContent.createAck(this.pid, this.seq);
    }
}
