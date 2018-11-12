package tigase.shiku;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Logger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;

import tigase.conf.Configurable;
import tigase.conf.ConfigurationException;
import tigase.server.ComponentInfo;
import tigase.server.Message;
import tigase.server.Packet;
import tigase.server.PacketFilterIfc;
import tigase.server.QueueType;
import tigase.shiku.utils.AESUtil;
import tigase.shiku.utils.RedisUtil;
import tigase.stats.StatisticsList;
import tigase.xml.Element;
import tigase.xml.XMLNodeIfc;
import tigase.xmpp.BareJID;
import tigase.xmpp.JID;

public class ShikuKeywordFilter implements PacketFilterIfc, Configurable {
	private static final Logger log = Logger.getLogger(ShikuKeywordFilter.class.getName());
	public static final String CONFIRM_OPEN_KEYWORD = "--confirm-open-keyword";
	public static int confirmopenkeyword = 0;// 1为打开 0为关闭
	public static int encryptEnabled = 0;// 1为加密

	public static List<String> keyWords = null;
	public static final String GET_SECRET_KEY = "secret:%s";
	// 是否开启加密
	public static final String GET_ENCRYPT_ENABLED = "app:encrypt";
	// Map keyWords=new HashMap<>();

	@Override
	public void init(String name, QueueType qType) {
		// TODO Auto-generated method stub

	}

	@Override
	public Packet filter(Packet packet) {

		
//		if("message".equals(packet.getElemName())){
//			System.out.println("packet:"+packet.toString());
//		}
		long startTime = System.currentTimeMillis();
		String body = packet.getElement().getChildCData(Message.MESSAGE_BODY_PATH);
		// 如果body内容为空则返回
		if (body == null)
			return packet;
		// 客户端消息是否加密
		body = body.replaceAll("=&quot;", "\"").replaceAll("&quot;", "\"");
		JSONObject bodyObj = null;
		try {
			bodyObj = JSON.parseObject(body);
		} catch (Exception e) {
			log.severe("=====错 误 日 志=====:json解析失败"+"body内容:"+body);
			log.severe("=====错 误 日 志=====packet:"+packet.toString());
			return packet;
		}
		if (1 == bodyObj.getIntValue("filter"))
			return packet;
		// 更换时间为系统时间
		setNewTimeSend(packet, body, bodyObj);
		if (confirmopenkeyword == 0)
			return packet;
		String content = null;

		int type = 0;
		type = bodyObj.getIntValue("type");
		if (type != 1)
			return packet;
		content = bodyObj.getString("content");
		if (content == null)
			return packet;
		if (filterKeyWords(content)) {
			System.out.println("ShikuKeywordFilter  输入字符不规范    " + content);
			long endTime = System.currentTimeMillis();
			System.out.println("程序运行时间：" + (endTime - startTime) + "ms");
			return null;
		} else {
			long endTime = System.currentTimeMillis();
			System.out.println("程序运行时间：" + (endTime - startTime) + "ms");
			return packet;
		}
	}

	@Override
	public void processPacket(Packet packet, Queue<Packet> results) {
		// TODO Auto-generated method stub
		/*
		 * String
		 * body=packet.getElement().getChildCData(Message.MESSAGE_BODY_PATH);
		 * if(body==null)
		 * 
		 * body = body.replaceAll("=&quot;", "\"").replaceAll("&quot;", "\"");
		 * JSONObject bodyObj=JSON.parseObject(body);
		 * 
		 * System.out.println("filter  "
		 * +packet.getElement().getChildCData(Message.MESSAGE_BODY_PATH));
		 * Packet packet2=setNewTimeSend(packet.copyElementOnly(), body,
		 * bodyObj); System.out.println("filter 2 "
		 * +packet2.getElement().getChildCData(Message.MESSAGE_BODY_PATH));
		 * 
		 * results.offer(packet2);
		 */
	}

	private boolean filterKeyWords(String keyword) {

		for (String key : ShikuKeywordFilter.keyWords) {
			if (-1 != keyword.indexOf(key))
				return true;
		}

		return false;
	}

	/// 设置 timesend 为服务器的时间
	private Packet setNewTimeSend(Packet copyPacket, String body, JSONObject jsonObj) {
		Long time = System.currentTimeMillis();
		double timeSend = getTimeSend(time);
		// 替换timeSend 成服务器的时间
		body = replaceTimeSend(timeSend, body, jsonObj);

		List<Element> children = copyPacket.getElement().getChildren();
		Element removeEle = null;
		for (Element element : children) {
			if ("body".equals(element.getName())) {
				removeEle = element;
				break;
			}
		}
		children.remove(removeEle);

		Element newBody = new Element("body", body);
		children.add(newBody);

		List<XMLNodeIfc> childNew = new ArrayList<XMLNodeIfc>();
		for (Element element : children) {
			childNew.add(element);
		}
		// System.out.println("setNewTimeSend childNew "+childNew);
		copyPacket.getElement().setChildren(childNew);
		return copyPacket;
	};

	/// 设置 timesend 为服务器的时间并加密
	private Packet setNewTimeSend(Packet copyPacket, String body, JSONObject jsonObj, String key) {
		Long time = System.currentTimeMillis();
		double timeSend = getTimeSend(time);
		// 替换timeSend 成服务器的时间
		body = replaceTimeSend(timeSend, body, jsonObj);

		List<Element> children = copyPacket.getElement().getChildren();
		Element removeEle = null;
		for (Element element : children) {
			if ("body".equals(element.getName())) {
				removeEle = element;
				break;
			}
		}
		children.remove(removeEle);
		//
		body = AESUtil.encrypt(body, key);
		Element newBody = new Element("body", body);
		children.add(newBody);

		List<XMLNodeIfc> childNew = new ArrayList<XMLNodeIfc>();
		for (Element element : children) {
			childNew.add(element);
		}
		// System.out.println("setNewTimeSend childNew "+childNew);
		copyPacket.getElement().setChildren(childNew);
		return copyPacket;
	};

	private JSONObject bodyToJson(String body) {
		JSONObject jsonObj = null;
		try {
			jsonObj = JSON.parseObject(body.replaceAll("=&quot;", "\"").replaceAll("&quot;", "\""));
			return jsonObj;
		} catch (JSONException e) {
			log.severe("=====错 误 日 志=method:bodyToJso===:json解析失败"+"body内容:"+body);
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
		jsonObj.put("timeSend", timeSend);
		jsonObj.put("filter", 1);
		return jsonObj.toString();
	}

	@Override
	public void getStatistics(StatisticsList list) {
		// TODO Auto-generated method stub

	}

	@Override
	public void initializationCompleted() {
		// TODO Auto-generated method stub

	}

	@Override
	public void release() {
		// TODO Auto-generated method stub

	}

	@Override
	public JID getComponentId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ComponentInfo getComponentInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isInitializationComplete() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setName(String name) {
		// TODO Auto-generated method stub

	}

	@Override
	public Map<String, Object> getDefaults(Map<String, Object> params) {
		/* Map<String, Object> props = super.getDefaults(params); */
		if (null != params.get(CONFIRM_OPEN_KEYWORD)) {
			confirmopenkeyword = Integer.valueOf(params.get(CONFIRM_OPEN_KEYWORD).toString());
		}
		return null;
	}

	@Override
	public void setProperties(Map<String, Object> properties) throws ConfigurationException {
		// TODO Auto-generated method stub

	}

	private Integer getUserId(BareJID jid) {
		String strUserId = jid.toString();
		int index = strUserId.indexOf("@");
		strUserId = strUserId.substring(0, index);
		return Integer.parseInt(strUserId);
	}

	private String getSecretKey(Integer userId) {
		String key = String.format(GET_SECRET_KEY, userId);
		String result = RedisUtil.get(key);
		return result;
	}

	/*
	 * private int isEncryption() { String result =
	 * RedisUtil.get(GET_ENCRYPT_ENABLED); return Integer.parseInt(result); }
	 */
}
