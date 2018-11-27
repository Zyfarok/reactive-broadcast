package ch.epfl.daeasy.protocol;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonSyntaxException;

import java.util.Objects;

public class CausalMessageContent extends MessageContent {
    private static final ImmutableSet<Cause> empty = ImmutableSet.<Cause>builder().build();
    public final ImmutableSet<Cause> causes;

    private CausalMessageContent(long pid, long seq) {
        super(pid, seq);
        this.causes = empty;
    }

    private CausalMessageContent(long pid, long seq, String payload, ImmutableSet<Cause> causes) {
        super(pid, seq, payload);
        this.causes = causes;
    }

    public static CausalMessageContent createMessage(long pid, long seq, String payload, ImmutableSet<Cause> causes) {
        return new CausalMessageContent(pid, seq, payload, causes);
    }

    public static CausalMessageContent createMessage(long pid, long seq, ImmutableSet<Cause> causes) {
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

    public MessageContent withoutCauses() {
        return this.payload.map(p -> MessageContent.createMessage(this.pid, this.seq, p))
                .orElse(MessageContent.createAck(this.pid, this.seq));
    }

    public String serialize() {
        return gson.toJson(this);
    }

    public static CausalMessageContent deserialize(String json) throws JsonSyntaxException {
        return gson.fromJson(json, CausalMessageContent.class);
    }

    @Override
    public boolean equals(Object obj) {
        if(super.equals(obj)){
            final CausalMessageContent other = (CausalMessageContent) obj;

            return other.causes.equals(this.causes);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), causes);
    }

    public static class Cause {
        public final long pid;
        public final long seq;
        public Cause(long pid, long seq) {
            this.pid = pid;
            this.seq = seq;
        }

        public boolean equals(Object obj) {
            if(obj != null && obj.getClass() == this.getClass()) {
                Cause other = (Cause) obj;
                return other.pid == pid && other.seq == seq;
            }
            return false;
        }

        public int hashCode() {
            return Objects.hash(pid, seq);
        }
    }
}
