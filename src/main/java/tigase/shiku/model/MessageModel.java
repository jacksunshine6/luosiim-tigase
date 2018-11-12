package tigase.shiku.model;

public class MessageModel {
	private String body;
	private Integer direction;// 0=发出的；1=收到的
	private String message;
	private Long receiver;
	private String receiver_jid;
	private Long sender;
	private String sender_jid;
	private Long ts;
	private Integer type;
	private String messageId;  //消息id
	//body  消息体里面的 type
	private Integer contentType;
	private String content; //消息内容
	
	private double timeSend;//消息发送时间
	
	
	private Integer pubNumMsgType;//0代表非公众号聊天,1代表用户发给客服,2代表客服发给用户
	private Integer pubNumCSId;//公众号客服id
	private Integer publicId;//公众号id
	/*//消息发送者
	private Long from;
	//消息接受者
	private Long to;*/

	public MessageModel() {
		
		super();
	}

	
	public MessageModel(Long sender, String sender_jid, Long receiver,
			String receiver_jid, Long ts, Integer direction, Integer type,
			String body, String message,String content) {
		super();
		this.sender = sender;
		this.sender_jid = sender_jid;
		this.receiver = receiver;
		this.receiver_jid = receiver_jid;
		this.ts = ts;
		this.direction = direction;
		this.type = type;
		this.body = body;
		this.message = message;
		this.content = content;
	}
	

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}


	public Integer getContentType() {
		return contentType;
	}


	public void setContentType(Integer contentType) {
		this.contentType = contentType;
	}


	public String getContent() {
		return content;
	}


	public void setContent(String content) {
		this.content = content;
	}


	public String getBody() {
		return body;
	}

	public Integer getDirection() {
		return direction;
	}

	public String getMessage() {
		return message;
	}

	public Long getReceiver() {
		return receiver;
	}

	public String getReceiver_jid() {
		return receiver_jid;
	}

	public Long getSender() {
		return sender;
	}

	public String getSender_jid() {
		return sender_jid;
	}

	public Long getTs() {
		return ts;
	}

	public Integer getType() {
		return type;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public void setDirection(Integer direction) {
		this.direction = direction;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public void setReceiver(Long receiver) {
		this.receiver = receiver;
	}

	public void setReceiver_jid(String receiver_jid) {
		this.receiver_jid = receiver_jid;
	}

	public void setSender(Long sender) {
		this.sender = sender;
	}

	public void setSender_jid(String sender_jid) {
		this.sender_jid = sender_jid;
	}

	public void setTs(Long ts) {
		this.ts = ts;
	}

	public void setType(Integer type) {
		this.type = type;
	}


	public double getTimeSend() {
		return timeSend;
	}


	public void setTimeSend(double timeSend) {
		this.timeSend = timeSend;
	}




	public Integer getPubNumMsgType() {
		return pubNumMsgType;
	}


	public void setPubNumMsgType(Integer pubNumMsgType) {
		this.pubNumMsgType = pubNumMsgType;
	}


	public Integer getPubNumCSId() {
		return pubNumCSId;
	}


	public void setPubNumCSId(Integer pubNumCSId) {
		this.pubNumCSId = pubNumCSId;
	}


	public Integer getPublicId() {
		return publicId;
	}


	public void setPublicId(Integer publicId) {
		this.publicId = publicId;
	}

}
