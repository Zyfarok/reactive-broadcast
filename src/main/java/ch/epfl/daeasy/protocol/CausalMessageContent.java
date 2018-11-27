package ch.epfl.daeasy.protocol;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonSyntaxException;

import java.util.Objects;

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

}
