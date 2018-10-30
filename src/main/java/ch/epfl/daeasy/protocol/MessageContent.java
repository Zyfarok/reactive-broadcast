package ch.epfl.daeasy.protocol;

import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class MessageContent {
	// seq = sequence number of this message
	private final Optional<Long> seq;
	// ack = sequence number of the acked message
	private final Optional<Long> ack;
	// pid = process ID of the origin
	private final long pid;

	// json serializer/deserializer
	private static Gson gson = new Gson();

	private MessageContent(Optional<Long> seq, Optional<Long> ack, long pid) {
		this.seq = seq;
		this.ack = ack;
		this.pid = pid;
	}

	public static MessageContent Message(long seq, long pid) {
		return new MessageContent(Optional.of(seq), Optional.empty(), pid);
	}

	public static MessageContent ACK(long ack, long pid) {
		return new MessageContent(Optional.empty(), Optional.of(ack), pid);
	}

	/*
	 * Construct an ACK from a Message
	 */
	public static MessageContent ackFromMessage(MessageContent content) throws IllegalArgumentException {
		if (!content.isMessage()) {
			throw new IllegalArgumentException("cannot create ACK");
		}

		return MessageContent.ACK(content.seq.get(), content.pid);
	}

	public boolean isACK() {
		return this.ack.isPresent();
	}

	public boolean isMessage() {
		return this.seq.isPresent();
	}

	public String serialize() {
		String jsonString = gson.toJson(this);
		return jsonString;
	}

	public static MessageContent deserialize(String json) throws JsonSyntaxException {
		return gson.fromJson(json, MessageContent.class);
	}

	public long getPID() {
		return this.pid;
	}

	public Optional<Long> getSeq() {
		return this.seq;
	}

	public Optional<Long> getAck() {
		return this.ack;
	}

	public String toString() {
		if (this.isACK()) {
			return "ACK: (ack: " + this.ack.get() + " pid: " + this.pid + ")";
		} else if (this.isMessage()) {
			return "MSG: (seq: " + this.seq.get() + " pid: " + this.pid + ")";
		} else {
			return "undefined";
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}

		if (!MessageContent.class.isAssignableFrom(obj.getClass())) {
			return false;
		}

		final MessageContent other = (MessageContent) obj;
		if (other.seq == null || other.ack == null) {
			return false;
		}

		if (!other.seq.isPresent() || !other.ack.isPresent()) {
			return false;
		}

		return this.ack.equals(other.ack) && this.seq.equals(other.seq) && this.pid == other.pid;
	}

	@Override
	public int hashCode() {
		return this.ack.hashCode() * this.seq.hashCode() * (int) this.pid;
	}
}
