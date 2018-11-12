package tigase.shiku;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Logger;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import tigase.db.NonAuthUserRepository;
import tigase.db.TigaseDBException;
import tigase.server.Message;
import tigase.server.Packet;
import tigase.server.monitor.MonitorRuntime;
import tigase.shiku.mq.MqMessage;
import tigase.shiku.utils.MqUtil;
import tigase.xmpp.BareJID;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPException;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.impl.C2SDeliveryErrorProcessor;

/**
 * 离线推送通知、离线消息推送插件
 * 
 * @author luorc@www.shiku.co
 *
 */
public class ShikuOfflineMsgPlugin extends XMPPProcessor implements XMPPProcessorIfc {
	private static final Logger log = Logger.getLogger(ShikuOfflineMsgPlugin.class.getName());
	private static final String ID = "shiku-offline-msg";
	private static final String[] XMLNSS = { Packet.CLIENT_XMLNS };
	private static final Set<StanzaType> TYPES;
	//RabbitMqUtil u=new RabbitMqUtil();
	MqUtil u=new MqUtil();
	static {
		HashSet<StanzaType> tmpTYPES = new HashSet<StanzaType>();
		tmpTYPES.add(null);
		tmpTYPES.addAll(EnumSet.of(StanzaType.groupchat, StanzaType.chat));
		TYPES = Collections.unmodifiableSet(tmpTYPES);
	}

	private static final String MESSAGE = "message";
	private static final String[][] ELEMENT_PATHS = { { MESSAGE } };
	private String shiku_PushUrl;
	private String pinba_PushUrl;

	@Override
	public void init(Map<String, Object> settings) throws TigaseDBException {
		// super.init(settings);
		shiku_PushUrl = (String) settings.get("shiku_PushUrl");
		pinba_PushUrl = (String) settings.get("pinba_PushUrl");
		//System.out.println("Shiku Offline Msg shiku_PushUrl = " + shiku_PushUrl);
		//System.out.println("Shiku Offline Msg pinba_PushUrl = " + pinba_PushUrl);
	}

	@Override
	public String id() {
		return ID;
	}

	@Override
	public void process(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo,
			Queue<Packet> results, Map<String, Object> settings) throws XMPPException {
		if (session == null) {
			return;
		}
		String body = packet.getElement().getChildCData(new String[] { "message", "body" });
		JSONObject bodyObj = null;
		int contextType = 0;
		String objectId = null;
		if (null != body) {
			try {
				bodyObj = JSON.parseObject(body.replaceAll("=&quot;", "\"").replaceAll("&quot;", "\""));
				contextType = bodyObj.getIntValue("type");
				objectId = bodyObj.getString("objectId");
				// 消息回执不处理
				if (26 == contextType || 27 == contextType)
					return;
			} catch (JSONException e) {
				log.severe("=====错 误 日 志=====:body解析出错" + body);
				log.severe("=====错 误 日 志=====:" + e.getMessage());
			}
		}

		final JSONObject jsonObj = bodyObj;
		String pushUrl = shiku_PushUrl;
		BareJID sender_jid = packet.getStanzaFrom().getBareJID();
		BareJID receiver_jid = packet.getStanzaTo().getBareJID();
		StanzaType type = packet.getType();
		long senderId = 0;
		long receiverId = 0;
		if (type != null && type != StanzaType.groupchat) {
			//// 如果是聘吧用户则不需要离线推送
			try {
				senderId = getUserId(sender_jid);
				receiverId = getUserId(receiver_jid);
			} catch (NumberFormatException e) {
				log.severe("=====错 误 日 志=====:senderId==parseLong--Fail---sender_jid:" + sender_jid);
			}
			// 系统推送 不离线通知
			if (receiverId == 10006 || receiverId == 10005)
				return;
			String domain = sender_jid.getDomain();
			if ("www.pinba.co".equals(domain) || "www.youjob.co".equals(domain))
				pushUrl = pinba_PushUrl;
		}
		try {
			if (Message.ELEM_NAME != packet.getElemName())
				return;
			// ignoring packets resent from c2s for redelivery as processing
			// them would create unnecessary duplication of messages in
			// archive
			if (C2SDeliveryErrorProcessor.isDeliveryError(packet))
				return;

			if ((packet.getElement().findChildStaticStr(Message.MESSAGE_BODY_PATH) == null) || ((type != null)
					&& (type != StanzaType.chat) && (type != StanzaType.groupchat) && (type != StanzaType.normal))) {
				return;
			}

			if (packet.getElemCDataStaticStr(Message.MESSAGE_BODY_PATH) == null)
				return;

			// 消息接收方是登录用户
			if (session.isUserId(receiver_jid)) {
				// System.out.println("ShikuOfflineMsgPlugin发出：消息接收方是登录用户");
				return;
			}

			// 接收方是否在线
			boolean isOnline = MonitorRuntime.getMonitorRuntime().isJidOnline(packet.getStanzaTo());
			// 是否进行离线通知
			System.out.println("ShikuOfflineMsgPlugin发出：接收方是否在线---" + isOnline);
			boolean isNotify = !isOnline;
			// 用户离线、执行离线推送逻辑
			if (isNotify) {
				// System.out.println("ShikuOfflineMsgPlugin发出:"+pushUrl);
				//final HttpUtil.Request req = new HttpUtil.Request();
				//req.setSpec(pushUrl);
				final Integer sender = getUserId(sender_jid);

				final Long ts = System.currentTimeMillis();
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							MqMessage msg=new MqMessage();
							if (StanzaType.chat == type) {
								final Integer receiver = getUserId(receiver_jid);
								msg.setType(1);
								msg.setTo(receiver);
							} else if (StanzaType.groupchat == type) {
								// 群聊消息
								msg.setType(2);
								msg.setRoomJid(getRoomJid(receiver_jid));
							}
							msg.setFrom(sender);
							msg.setBody(jsonObj.toString());
							msg.setTs(ts);
							u.asyncSend(msg);
							//RabbitMqUtil.send(msg);
							log.info("离线推送内容-msg:" + msg);
						} catch (Exception e) {
							log.severe("=====错 误 日 志==ShikuOfflineMsgPlugin==:" + e.getMessage());
							e.printStackTrace();
						}
						
						
						
						
						
//						try {
//							// System.out.println("ShikuOfflineMsgPlugin发出：单聊消息");
//							if (StanzaType.chat == type) {
//								final Long receiver = getUserId(receiver_jid);
//								req.getData().put("type", 1);
//								req.getData().put("to", receiver);
//							} else if (StanzaType.groupchat == type) {
//								// 群聊消息
//								req.getData().put("type", 2);
//								req.getData().put("roomJid", getRoomJid(receiver_jid));
//							}
//							req.getData().put("from", sender);
//							req.getData().put("body", jsonObj);
//							req.getData().put("ts", ts);
//							String result = HttpUtil.asString(req);
//							log.info("离线推送内容-req:" + req.toString());
//							log.info("离线推送结果-result:" + result);
//						} catch (Exception e) {
//							log.severe("=====错 误 日 志==ShikuOfflineMsgPlugin==:" + e.getMessage());
//						}

					}
				}).start();

			} else {/*
				if (contextType == 1 && objectId != null) {
					if (StanzaType.groupchat == type) {
						final Integer sender = getUserId(sender_jid);
						final Long ts = System.currentTimeMillis();
						MqMessage msg=new MqMessage();
						final Integer receiver = getUserId(receiver_jid);
						msg.setType(1);
						msg.setTo(receiver);
						// 群聊消息
						msg.setType(2);
						msg.setRoomJid(getRoomJid(receiver_jid));
						msg.setFrom(sender);
						msg.setBody("jsonObj");
						msg.setTs(ts);
						MqUtil.send(msg);
						log.info("离线推送内容-msg:" + msg);
//						final HttpUtil.Request req = new HttpUtil.Request();
//						req.setSpec(pushUrl);
//						final Long sender = getUserId(sender_jid);
//						// final Long receiver = getUserId(receiver_jid);
//						final Long ts = System.currentTimeMillis();
//						req.getData().put("type", 2);
//						req.getData().put("roomJid", getRoomJid(receiver_jid));
//						req.getData().put("from", sender);
//						req.getData().put("body", jsonObj);
//						req.getData().put("ts", ts);
//						String result = HttpUtil.asString(req);
//						log.info("离线推送内容-req:" + req.toString());
//						log.info("离线推送结果-result:" + result);
					}
				}
			*/
			return;	
			}
		} catch (Exception e) {
			log.severe("=====错 误 日 志==ShikuOfflineMsgPlugin==:" + e.getMessage());
		}
	}

	@Override
	public String[][] supElementNamePaths() {
		return ELEMENT_PATHS;

	}

	@Override
	public String[] supNamespaces() {
		return XMLNSS;
	}

	@Override
	public Set<StanzaType> supTypes() {
		return TYPES;
	}

	private Integer getUserId(BareJID jid) {
		String strUserId = jid.toString();
		int index = strUserId.indexOf("@");
		strUserId = strUserId.substring(0, index);
		//Long.parseLong(strUserId);
		return Integer.parseInt(strUserId);
	}

	private String getRoomJid(BareJID jid) {
		String strUserId = jid.toString();
		int index = strUserId.indexOf("@");
		strUserId = strUserId.substring(0, index);

		return strUserId;
	}

}
