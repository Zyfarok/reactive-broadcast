package ch.epfl.daeasy.protocol;

import java.nio.ByteBuffer;
import java.util.Arrays;

import com.google.gson.Gson;

import java.net.InetSocketAddress;
import java.io.ByteArrayInputStream;
import java.net.DatagramPacket;

/*
 * A DAPacket is a application-level payload with a correspondent.
 */
public class DAPacket {
    //protected final long payload;
	
	protected MessageContent msg_cont;
	
    protected final InetSocketAddress peer;

    protected DAPacket(InetSocketAddress peer, long payload) {
        //this.payload = payload;
    	msg_cont = new MessageContent(payload);
        this.peer = peer;
    }

    protected DAPacket(InetSocketAddress peer, byte[] payload) {
        this.peer = peer;
        msg_cont = new MessageContent(payload);
        //ByteBuffer buffer = ByteBuffer.wrap(payload);
        //this.payload = buffer.getLong();
    }

    public boolean isACK() {
        //return this.payload < 0;
    	return msg_cont.payload<0;
    }

    public boolean isMessage() {
        //return this.payload > 0;
    	return msg_cont.payload>0;
    }
    
    public String jsonMsgContent(){
    	Gson gson = new Gson();
    	String json = gson.toJson(msg_cont, MessageContent.class);
    	return json;
    }
    
    private MessageContent getMsgContent(){
    	return msg_cont;
    }
    
}