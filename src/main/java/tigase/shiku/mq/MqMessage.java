package tigase.shiku.mq;

import java.io.Serializable;

public class MqMessage implements Serializable{

	/** serialVersionUID*/ 
	private static final long serialVersionUID = 1L;
	
	private int from;
	private int to;
	private int type;
	private String body;
	private String roomJid;
	private long ts;
	public long getTs() {
		return ts;
	}
	public void setTs(long ts) {
		this.ts = ts;
	}
	public MqMessage() {
		super();
	}
	public int getFrom() {
		return from;
	}
	public void setFrom(int from) {
		this.from = from;
	}
	public int getTo() {
		return to;
	}
	public void setTo(int to) {
		this.to = to;
	}
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	public String getBody() {
		return body;
	}
	public void setBody(String body) {
		this.body = body;
	}
	public String getRoomJid() {
		return roomJid;
	}
	public void setRoomJid(String roomJid) {
		this.roomJid = roomJid;
	}
	@Override
	public String toString() {
		return "MqMessage [from=" + from + ", to=" + to + ", type=" + type + ", body=" + body + ", roomJid=" + roomJid
				+ ", ts=" + ts + "]";
	}
	
	

}
