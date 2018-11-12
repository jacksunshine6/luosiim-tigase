package tigase.shiku;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;

import tigase.conf.ConfigurationException;
import tigase.osgi.ModulesManagerImpl;
import tigase.server.AbstractMessageReceiver;
import tigase.server.Packet;
import tigase.server.xmppsession.UserDao;
import tigase.shiku.db.ShikuMessageArchiveRepository;
import tigase.shiku.model.MessageModel;
import tigase.shiku.model.MucMessageModel;
import tigase.xmpp.BareJID;
import tigase.xmpp.StanzaType;

/**
 * 视酷消息记录归档组件
 * <p>
 * <strong>功能：</strong>单聊和群聊消息归档
 * </p>
 * 
 * @author luorc@www.shiku.co
 *
 */
public class ShikuMessageArchiveComponent extends AbstractMessageReceiver {

	private static final Logger log = Logger.getLogger(ShikuMessageArchiveComponent.class.getCanonicalName());
	private static final String MSG_ARCHIVE_REPO_CLASS_PROP_KEY = "archive-repo-class";
	private static final String MSG_ARCHIVE_REPO_URI_PROP_KEY = "archive-repo-uri";
	private static final String[] MSG_BODY_PATH = { "message", "body" };
	public static final String GET_SECRET_KEY = "secret:%s";
	private ShikuMessageArchiveRepository repo = null;

	public ShikuMessageArchiveComponent() {
		super();
		setName("shiku-message-archive");
	}

	@Override
	public void processPacket(Packet packet) {
		if ((packet.getStanzaTo() != null) && !getComponentId().equals(packet.getStanzaTo())) {
			// 保存消息
			storeMessage(packet);
			return;
		}
	}

	@Override
	public Map<String, Object> getDefaults(Map<String, Object> params) {
		return super.getDefaults(params);
	}

	@Override
	public void setProperties(Map<String, Object> props) throws ConfigurationException {
		try {
			super.setProperties(props);

			if (props.size() == 1) {
				return;
			}

			Map<String, String> repoProps = new HashMap<String, String>(4);
			for (Entry<String, Object> entry : props.entrySet()) {
				if ((entry.getKey() == null) || (entry.getValue() == null))
					continue;
				repoProps.put(entry.getKey(), entry.getValue().toString());
			}

			String repoClsName = (String) props.get(MSG_ARCHIVE_REPO_CLASS_PROP_KEY);
			String uri = (String) props.get(MSG_ARCHIVE_REPO_URI_PROP_KEY);

			if (null != uri) {
				if (null != repoClsName) {
					try {
						@SuppressWarnings("unchecked")
						Class<? extends ShikuMessageArchiveRepository> repoCls = (Class<? extends ShikuMessageArchiveRepository>) ModulesManagerImpl
								.getInstance().forName(repoClsName);
						repo = repoCls.newInstance();
						repo.initRepository(uri, repoProps);
					} catch (ClassNotFoundException e) {
						log.log(Level.SEVERE, "Could not find class " + repoClsName
								+ " an implementation of ShikuMessageArchive repository", e);
						throw new ConfigurationException("Could not find class " + repoClsName
								+ " an implementation of ShikuMessageArchive repository", e);
					}
				}
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "视酷消息归档组件初始化失败", e);
			throw new ConfigurationException("视酷消息归档组件初始化失败", e);
		}
	}

	@Override
	public void release() {
		super.release();
	}

	@Override
	public String getDiscoDescription() {
		return "ShiKu Message Archiving Support";
	}

	private Long getUserId(BareJID jid) {
		// 得到账号ID
		String strUserId = jid.toString();
		int index = strUserId.indexOf("@");
		strUserId = strUserId.substring(0, index);

		return Long.parseLong(strUserId);
	}

	private String getRoomId(BareJID jid) {
		// 得到房间ID
		String strUserId = jid.toString();
		int index = strUserId.indexOf("@");
		strUserId = strUserId.substring(0, index);

		return strUserId;
	}

	private void storeMessage(Packet packet) {
		String ownerStr = packet.getAttributeStaticStr(ShikuMessageArchivePlugin.OWNNER_JID);

		if (ownerStr != null) {
			packet.getElement().removeAttribute(ShikuMessageArchivePlugin.OWNNER_JID);

			StanzaType type = packet.getType();
			// 单聊
			if (StanzaType.chat == type) {
				storeMessageChat(packet, ownerStr);
			}
			// 群聊
			else if (StanzaType.groupchat == type) {
				storeMessageGrouChat(packet, ownerStr);
			} else {

			}
		} else {
			log.log(Level.INFO, "Owner attribute missing from packet: {0}", packet);
		}
	}

	// 保存单聊记录
	private void storeMessageChat(Packet packet, String ownerStr) {
		try {
			BareJID sender_jid = BareJID.bareJIDInstanceNS(ownerStr);
			Long sender = getUserId(sender_jid);
			// 单聊
			Integer direction = sender_jid.equals(packet.getStanzaFrom().getBareJID()) ? 0 : 1;// 0=发出去的；1=收到的
			BareJID receiver_jid = direction == 0 ? packet.getStanzaTo().getBareJID()
					: packet.getStanzaFrom().getBareJID();
			Long receiver = getUserId(receiver_jid);

			long ts = System.currentTimeMillis();
			double timeSend = getTimeSend(ts);
			String message = packet.getElement().toString();
			String messageId = packet.getElement().getAttribute("id");

			Integer messageType = 1;// 1=chat
			String body = packet.getElement().getChildCData(MSG_BODY_PATH);
			/*// 解密相关代码,如果自己发送则,利用发送者的秘钥解密,
			//如果自己为接收者说明是服务器发送消息,还是用此时发送者的秘钥解密
			// 0为自己发出时,即客户端发送
			Integer from = getUserID(sender_jid);
			// 获取密钥
			String secretKey = getSecretKey(from);
			secretKey = "1111111111111111";
			// 对消息进行解密
			body = AESUtil.decrypt(body, secretKey);*/

			int contextType = 0;
			boolean isReadDel = false; // 是否为阅后即焚消息 false:不是 true:是
			String content = ""; // 聊天内容
			int pubNumMsgType = 0;
			int pubNumCSId = 0;
			int publicId = 0;
			if (null != body) {
				JSONObject jsonObj = bodyToJson(body);
				contextType = jsonObj.getIntValue("type");
				isReadDel = jsonObj.getBooleanValue("isReadDel");
				content = jsonObj.getString("content");
				pubNumMsgType = jsonObj.getIntValue("pubNumMsgType");
				pubNumCSId = jsonObj.getIntValue("pubNumCSId");
				publicId = jsonObj.getIntValue("publicId");
				// 替换timeSend 成服务器的时间
				// body=replaceTimeSend(timeSend, body, jsonObj);
			}

			// System.out.println("视酷消息归档组件 storeMessage "+jsonObj);
			// System.out.println("视酷消息归档组件 storeMessage
			// contextType----"+contextType);
			// 消息回执存储

			// 加关注 加好友 打招呼 信息
			if (contextType / 100 == 5) {
				UserDao.getInstance().saveNewFriendsInThread(sender, receiver, direction, contextType, content);
				return;
			} else if (contextType / 100 == 9)// 群控制信息
				return;
			else if (26 == contextType || 27 == contextType || isReadDel == true)
				return;
			else if (contextType == 41 || contextType == 42 || contextType == 43 || contextType == 201)
				return;

			MessageModel model = new MessageModel(sender, sender_jid.toString(), receiver, receiver_jid.toString(), ts,
					direction, messageType, body, message, content);
			model.setContentType(contextType);
			model.setMessageId(messageId);
			model.setTimeSend(timeSend);
			model.setPubNumMsgType(pubNumMsgType);
			model.setPubNumCSId(pubNumCSId);
			model.setPublicId(publicId);
			if (0 == pubNumMsgType) {
				repo.archiveMessage(model);
			} else {
				repo.archivePubMessage(model);
			}

		} catch (JSONException e) {
			log.severe("=====错 误 日 志=====:json解析失败"+"原因"+e.getMessage());
			e.printStackTrace();
		}
	}

	// 保存群聊记录
	private void storeMessageGrouChat(Packet packet, String ownerStr) {
		BareJID sender_jid = BareJID.bareJIDInstanceNS(ownerStr);
		Long sender = getUserId(sender_jid);
		if (sender_jid.equals(packet.getStanzaFrom().getBareJID())) {
			BareJID room_jid = packet.getStanzaTo().getBareJID();
			String room_id = getRoomId(room_jid);
			String nickname = "";
			String body = packet.getElement().getChildCData(MSG_BODY_PATH);
			String message = packet.getElement().toString();
			String messageId = packet.getElement().getAttribute("id");
			Integer public_event = 1;
			Long ts = System.currentTimeMillis();
			double timeSend = getTimeSend(ts);
			Integer event_type = 1;

			int contextType = 0;
			String context = ""; // 消息内容
			if (null != body) {
				try {
					JSONObject jsonObj = bodyToJson(body);
					contextType = jsonObj.getIntValue("type");
					context = jsonObj.getString("content");
					// 替换timeSend 成服务器的时间
					// body=replaceTimeSend(timeSend, body, jsonObj);
				} catch (JSONException e) {

				}
			}
			if (contextType / 100 == 9)// 群控制信息
				return;
			else if (26 == contextType || 27 == contextType)
				return;
			else if (contextType == 41 || contextType == 42 || contextType == 43)
				return;

			MucMessageModel model = new MucMessageModel(room_id, room_jid.toString(), sender, sender_jid.toString(),
					nickname, body, message, public_event, ts, event_type, context);
			model.setContentType(contextType);
			model.setMessageId(messageId);
			model.setTimeSend(timeSend);
			repo.archiveMessage(model);
			// System.out.println(room_id);
			// ObjectId id=new ObjectId("pnkkng");
			// repo.userIdList(id);
		}
	}

	private JSONObject bodyToJson(String body) {
		JSONObject jsonObj = null;
		try {
			jsonObj = JSON.parseObject(body.replaceAll("=&quot;", "\"").replaceAll("&quot;", "\""));
			return jsonObj;
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
	}

	private double getTimeSend(long ts) {
		double time = (double) ts;
		DecimalFormat dFormat = new DecimalFormat("#.000");
		return new Double(dFormat.format(time / 1000));
	}

	private String replaceTimeSend(Double timeSend, String body, JSONObject jsonObj) {
		String oldTime = jsonObj.getString("timeSend");
		if (oldTime == null) {
			return body;
		}
		body = body.replace(oldTime, timeSend.toString());
		return body;
	}


}
