package ch.epfl.daeasy.protocol;

import java.nio.ByteBuffer;

import com.google.gson.Gson;

public class MessageContent {
	protected final long payload;
	protected final String json;
	
	public Gson gson;
	
	public MessageContent(long payload){
		this.payload = payload;
		gson = new Gson();
		json = gson.toJson(payload);
	}
	
	public MessageContent(byte[] payload){
		this.payload = ByteBuffer.wrap(payload).getLong();
		gson = new Gson();
		json = gson.toJson(payload);
	}
	
    public boolean isACK() {
        return this.payload < 0;
    }

    public boolean isMessage() {
        return this.payload > 0;
    }
    
    private String getJson(){
    	return json;
    }

}
