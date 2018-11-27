package ch.epfl.daeasy.protocol;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.Objects;
import java.util.Optional;

public class MessageContent {
	// pid = process ID of the origin
	public final long pid;
	// seq = sequence number of this message or ack
    public final long seq;
    // Message content
    public final Optional<String> payload;

	// json serializer/deserializer
	protected static Gson gson = new Gson();

    protected MessageContent(long pid, long seq) {
        this.pid = pid;
        this.seq = seq;
        this.payload = Optional.empty();
    }

    protected MessageContent(long pid, long seq, String payload) {
        this.pid = pid;
        this.seq = seq;
        this.payload = Optional.of(payload);
    }

    public static MessageContent createMessage(long pid, long seq, String payload) {
        return new MessageContent(pid, seq, payload);
    }

    public static MessageContent createMessage(long pid, long seq) {
        return createMessage(pid, seq, Long.toString(pid) + "->" + Long.toString(seq));
    }

	public static MessageContent createAck(long pid, long seq) {
		return new MessageContent(pid, seq);
	}

    public boolean isMessage() {
        return this.payload.isPresent();
    }

    public boolean isAck() {
        return !this.isMessage();
    }

    /*
	 * Construct an ACK from a Message
	 */
	public MessageContent toAck() {
	    assert this.isMessage();

		return createAck(this.pid, this.seq);
	}

	public String serialize() {
        return gson.toJson(this);
	}

	public static MessageContent deserialize(String json) throws JsonSyntaxException {
		return gson.fromJson(json, MessageContent.class);
	}

	public String toString() {
	    return this.payload.map(
	            s -> "MSG: (pid: " + this.pid + " seq: " + this.seq + " payload: " + s + ")"
        ).orElse("ACK: (pid: " + this.pid + " seq: " + this.seq + ")");
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || obj.getClass() != getClass()) {
			return false;
		}

		final MessageContent other = (MessageContent) obj;

		return this.pid == other.pid && this.seq == other.seq && this.payload.map(p1 ->
		        other.payload.map(p1::equals).orElse(false)
        ).orElse(!other.payload.isPresent());
	}

	@Override
	public int hashCode() {
		return Objects.hash(pid, seq, payload);
	}
}
