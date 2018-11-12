package tigase.shiku;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;

import tigase.db.NonAuthUserRepository;
import tigase.server.Message;
import tigase.server.Packet;
import tigase.server.monitor.MonitorRuntime;
import tigase.util.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.BareJID;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPException;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.impl.C2SDeliveryErrorProcessor;

/**
 * 视酷信息插件
 * <p>
 * 1、单聊回执
 * </p>
 * <p>
 * 2、群聊回执
 * </p>
 * 
 * @author luorc@www.shiku.co
 *
 */
public class ShikuAutoReplyPlugin extends XMPPProcessor implements XMPPProcessorIfc {
	private static final Logger log = Logger.getLogger(ShikuAutoReplyPlugin.class.getName());
	private static final String ID = "shiku-auto-reply";
	private static final String[] XMLNSS = { Packet.CLIENT_XMLNS };
	private static final Set<StanzaType> TYPES;
	static {
		HashSet<StanzaType> tmpTYPES = new HashSet<StanzaType>();
		tmpTYPES.add(null);
		tmpTYPES.addAll(EnumSet.of(StanzaType.groupchat, StanzaType.chat));
		TYPES = Collections.unmodifiableSet(tmpTYPES);
	}
	private static final String MESSAGE = "message";
	private static final String[][] ELEMENT_PATHS = { { MESSAGE } };

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
		try {
			// 开始聊天啦
			if (Message.ELEM_NAME == packet.getElemName()) {
				// ignoring packets resent from c2s for redelivery as processing
				// them would create unnecessary duplication of messages in
				// archive
				if (C2SDeliveryErrorProcessor.isDeliveryError(packet))
					return;
				StanzaType type = packet.getType();
				// Element message= packet.getElement();
				// System.out.println("ShikuAutoReplyPlugin---message===>
				// "+message);
				String body = packet.getElement().getChildCData(Message.MESSAGE_BODY_PATH);
				if ((packet.getElement().findChildStaticStr(Message.MESSAGE_BODY_PATH) == null)
						|| ((type != null) && (type != StanzaType.chat) && (type != StanzaType.groupchat)
								&& (type != StanzaType.normal))) {

					// 邀请好友协议 服务器发送 回执
					/*
					 * if(message.toString().contains("<invite")){
					 * //System.out.println(message); sendReply(packet,
					 * results); }
					 */
					return;
				}
				if (null == body)
					return;
				JSONObject bodyObj = null;
				try {
					body = body.replaceAll("=&quot;", "\"").replaceAll("&quot;", "\"");
					if (null == body) {
						log.warning("=====警 告 =====:"+"body为空");
						return;
					}
					bodyObj = JSON.parseObject(body);

				} catch (JSONException e) {
					log.severe("=====错 误 日 志=====:json解析失败"+"body内容:"+body);
				}
				if (packet.getElemCDataStaticStr(Message.MESSAGE_BODY_PATH) != null) {
					// 回执接收方是登录用户时
					// 登陆用户为当前session
					BareJID receiver_jid = packet.getStanzaTo().getBareJID();
					if ("" == receiver_jid.toString() || null == receiver_jid) {
						log.severe("=====错 误 日 志=====:ShikuAutoReplyPlugin == Error toJid is null");
					}
					long receiverId = 0;
					try {
						receiverId = getUserId(receiver_jid);
					} catch (NumberFormatException e) {
						log.severe("=====错 误 日 志=====:senderId==parseLong--Fail---" + receiver_jid);
					}
					if (session.isUserId(receiver_jid))
						return;
					boolean isReply = false;
					if (StanzaType.chat == type) {
						// 接收方是否在线
						boolean isOnline = MonitorRuntime.getMonitorRuntime().isJidOnline(packet.getStanzaTo());
						isReply = !isOnline;
					} else if (StanzaType.groupchat == type) {
						isReply = true;
					}
					if (receiverId < 10030)
						isReply = true;
					if (isReply) {
						sendReply(packet, results);
					}
				}
			}
		} catch (Exception e) {
			log.severe("=====错 误 日 志=====:"+e.getMessage());

			e.printStackTrace();
		}
	}

	private Long getUserId(BareJID jid) {
		String strUserId = jid.toString();
		int index = strUserId.indexOf("@");
		strUserId = strUserId.substring(0, index);

		return Long.parseLong(strUserId);
	}

	// 发送消息回执
	private void sendReply(Packet packet, Queue<Packet> results) throws TigaseStringprepException {
		// 获取message ID
		String id = packet.getStanzaId();
		if (null == id || "".equals(id))
			return;
		Element received = new Element("received");
		received.setXMLNS("urn:xmpp:receipts");

		received.addAttribute("id", id);
		// received.addAttribute("messageId", id);
		received.addAttribute("status", "1");

		Packet receipt = Packet.packetInstance("message", packet.getStanzaTo().toString(),
				packet.getStanzaFrom().toString(), StanzaType.normal);
		receipt.getElement().addChild(received);

		// 将回执写入流出队列
		results.offer(receipt);
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

}

// public class ShikuAutoReplyPlugin extends XMPPProcessor implements
// XMPPPreprocessorIfc {
// private static final Logger log = Logger
// .getLogger(ShikuAutoReplyPlugin.class.getName());
//
// private static final String ID = "shiku-auto-reply";
// private static final String[] XMLNSS = { Packet.CLIENT_XMLNS };
// private static final Set<StanzaType> TYPES;
// static {
// HashSet<StanzaType> tmpTYPES = new HashSet<StanzaType>();
// tmpTYPES.add(null);
// tmpTYPES.addAll(EnumSet.of(StanzaType.groupchat, StanzaType.chat));
// TYPES = Collections.unmodifiableSet(tmpTYPES);
// }
// private static final String MESSAGE = "message";
// private static final String[][] ELEMENT_PATHS = { { MESSAGE } };
//
// @Override
// public String id() {
// return ID;
// }
//
// @Override
// public boolean preProcess(Packet packet, XMPPResourceConnection session,
// NonAuthUserRepository repo, Queue<Packet> results,
// Map<String, Object> settings) {
// if (log.isLoggable(Level.FINEST)) {
// log.log(Level.FINEST,
// "SKYJPlugin Processing packet: {0}, for session: {1}",
// new Object[] { packet, session });
// }
// if (null != session) {
// StanzaType type = packet.getType();
// if (type == null)
// type = StanzaType.normal;
// // 单聊消息
// if (type == StanzaType.chat) {
// // 发送方
// BareJID from = (packet.getStanzaFrom() != null) ? packet
// .getStanzaFrom().getBareJID() : null;
// // 接收方
// BareJID to = (packet.getStanzaTo() != null) ? packet
// .getStanzaTo().getBareJID() : null;
// try {
// // 发送方是当前用户、接收方不是当前用户
// if (session.isUserId(from) && !session.isUserId(to)) {
// // 接收方是否在线
// boolean isOnline = MonitorRuntime.getMonitorRuntime()
// .isJidOnline(packet.getStanzaTo());
// // 接收方离线，服务端以接收方身份反馈回执给发送方
// if (!isOnline) {
// Element received = new Element("received");
// received.setXMLNS("urn:xmpp:receipts");
// received.addAttribute("id", packet.getStanzaId());
// received.addAttribute("status", "1");
//
// Packet receipt = Packet.packetInstance("message",
// packet.getStanzaTo().toString(), packet
// .getStanzaFrom().toString(),
// StanzaType.normal);
// receipt.getElement().addChild(received);
//
// // 将回执写入流出队列
// results.offer(receipt);
// }
// }
// } catch (Exception e) {
// e.printStackTrace();
// }
// } else if (type == StanzaType.groupchat) {
// // 发送方
// BareJID from = (packet.getStanzaFrom() != null) ? packet
// .getStanzaFrom().getBareJID() : null;
// // 接收方
// BareJID to = (packet.getStanzaTo() != null) ? packet
// .getStanzaTo().getBareJID() : null;
// try {
// // 发送方是当前用户、接收方不是当前用户
// if (session.isUserId(from) && !session.isUserId(to)) {
// Element received = new Element("received");
// received.setXMLNS("urn:xmpp:receipts");
// received.addAttribute("id", packet.getStanzaId());
// received.addAttribute("status", "1");
//
// Packet receipt = Packet.packetInstance("message",
// packet.getStanzaTo().toString(), packet
// .getStanzaFrom().toString(),
// StanzaType.normal);
// receipt.getElement().addChild(received);
//
// // 将回执写入流出队列
// results.offer(receipt);
// }
// } catch (Exception e) {
// e.printStackTrace();
// }
// }
// }
//
// return false;
// }
//
// @Override
// public String[][] supElementNamePaths() {
// return ELEMENT_PATHS;
//
// }
//
// @Override
// public String[] supNamespaces() {
// return XMLNSS;
// }
//
// @Override
// public Set<StanzaType> supTypes() {
// return TYPES;
// }
//
// }
