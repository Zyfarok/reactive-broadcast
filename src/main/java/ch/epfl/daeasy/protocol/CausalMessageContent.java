package ch.epfl.daeasy.protocol;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.google.gson.JsonSyntaxException;
import com.google.common.collect.Sets;

public class CausalMessageContent extends MessageContent {
    private static final Set<Cause> empty = new HashSet<>();
    public final Set<Cause> causes;

    private CausalMessageContent(long pid, long seq) {
        super(pid, seq);
        this.causes = empty;
    }

    private CausalMessageContent(long pid, long seq, String payload, Iterable<Cause> causes) {
        super(pid, seq, payload);
        this.causes = Collections.unmodifiableSet(Sets.newHashSet(causes));
    }

    public static CausalMessageContent createMessage(long pid, long seq, String payload, Iterable<Cause> causes) {
        return new CausalMessageContent(pid, seq, payload, Collections.unmodifiableSet(Sets.newHashSet(causes)));
    }

    public static CausalMessageContent createMessage(long pid, long seq, Iterable<Cause> causes) {
        return createMessage(pid, seq, Long.toString(pid) + "->" + Long.toString(seq),
                Collections.unmodifiableSet(Sets.newHashSet(causes)));
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
        if (super.equals(obj)) {
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
            if (obj != null && obj.getClass() == this.getClass()) {
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
