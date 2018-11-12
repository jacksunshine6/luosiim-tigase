package tigase.shiku;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import tigase.db.NonAuthUserRepository;
import tigase.db.TigaseDBException;
import tigase.server.Message;
import tigase.server.Packet;
import tigase.shiku.utils.RedisUtil;
import tigase.util.DNSResolver;
import tigase.xmpp.JID;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPException;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.impl.C2SDeliveryErrorProcessor;

public class ShikuMessageArchivePlugin extends XMPPProcessor implements
		XMPPProcessorIfc {
	public static final String OWNNER_JID = "ownner";

	private static final Logger log = Logger
			.getLogger(ShikuMessageArchivePlugin.class.getName());

	private static final String ID = "shiku-message-archive-plugin";

	private static final String MESSAGE = "message";
	private static final String[][] ELEMENT_PATHS = { { MESSAGE } };

	private static final String[] XMLNSS = { Packet.CLIENT_XMLNS };
	
	private static final Set<StanzaType> TYPES;
	static {
		HashSet<StanzaType> tmpTYPES = new HashSet<StanzaType>();
		tmpTYPES.add(null);
		tmpTYPES.addAll(EnumSet.of(StanzaType.groupchat, StanzaType.chat));
		TYPES = Collections.unmodifiableSet(tmpTYPES);
	}

	private JID shiku_ma_jid = null;

	@Override
	public void init(Map<String, Object> settings) throws TigaseDBException {
		super.init(settings);

		String componentJidStr = (String) settings.get("component-jid");

		if (componentJidStr != null) {
			shiku_ma_jid = JID.jidInstanceNS(componentJidStr);
		} else {
			String defHost = DNSResolver.getDefaultHostname();
			shiku_ma_jid = JID.jidInstanceNS("message-archive", defHost, null);
		}
		log.log(Level.CONFIG,
				"Loaded shiku message archiving component jid option: {0} = {1}",
				new Object[] { "component-jid", shiku_ma_jid });
		System.out.println("Shiku MA LOADED = " + shiku_ma_jid);
	}

	@Override
	public void process(Packet packet, XMPPResourceConnection session,
			NonAuthUserRepository repo, Queue<Packet> results,
			Map<String, Object> settings) throws XMPPException {
		if (session == null) {
			return;
		}
		try {
			
			if (Message.ELEM_NAME == packet.getElemName()) {
				// ignoring packets resent from c2s for redelivery as processing
				// them would create unnecessary duplication of messages in
				// archive
				if (C2SDeliveryErrorProcessor.isDeliveryError(packet))
					return;

				StanzaType type = packet.getType();

				if ((packet.getElement().findChildStaticStr(
						Message.MESSAGE_BODY_PATH) == null)
						|| ((type != null) && (type != StanzaType.chat)
								&& (type != StanzaType.groupchat) && (type != StanzaType.normal))) {
					return;
				}

				if (packet.getElemCDataStaticStr(Message.MESSAGE_BODY_PATH) != null) {
					Packet result = packet.copyElementOnly();
					result.setPacketTo(shiku_ma_jid);
					result.getElement().addAttribute(OWNNER_JID,
							session.getBareJID().toString());
					String body=packet.getElemCDataStaticStr(Message.MESSAGE_BODY_PATH);
					
					result.getElement().setAttribute("body", body);
					// 删除body以外节点
					// Element message = result.getElement();
					// for (Element elem : message.getChildren()) {
					// switch (elem.getName()) {
					// case "body":
					// break;
					// default:
					// message.removeChild(elem);
					// }
					// }
					results.offer(result);
				}
			}
		} catch (Exception e) {
			log.severe("=====错 误 日 志=====:"+e.getMessage());
		}
	}

	@Override
	public String id() {
		return ID;
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
