package tigase.rest.muc

import tigase.http.rest.Service
import tigase.server.Iq;
import tigase.server.Packet
import tigase.xml.Element
import tigase.xmpp.StanzaType


class PushHandler extends tigase.http.rest.Handler {

	public PushHandler() {
		regex = /\/(?:([^@\/]+)@){0,1}([^@\/]+)/
		isAsync = true
		execGet = { Service service, callback, localPart, domain ->

//			Element body = new Element("body", "我艹你大爷！！！");
//			Packet message = Packet.packetInstance("message",
//					"$localPart@$domain", "88888@$domain",
//					StanzaType.chat);
//			message.getElement().addChild(body);
			
			/*
			Element thread = new Element("thread","6VNiuj");
			Element x = new Element("x");
			Element offline = new Element("offline");
			Element composing = new Element("composing");
			x.addChild(composing);
			x.addChild(offline);
			x.setAttribute("xmlns", "jabber:x:event");
			Element body = new Element("body", "我艹你大爷！！！");
			Packet message = Packet.packetInstance("message",
					"88888@$domain", "10000@$domain",
					StanzaType.chat);
//			message.stanzaId="z45TP-30";
			message.getElement().addChild(body);
			message.getElement().addChild(x);
			message.getElement().addChild(thread);
            */
			Element from = new Element("from", "10000@$domain")
			Element to = new Element("to", "88888@$domain,luorc@$domain")
			Element body = new Element("body", "我艹你大爷的！！！！")
			
			Element query = new Element("query")
			query.addAttribute("xmlns", "jabber:iq:shiku-push")
			query.addChild(from)
			query.addChild(to)
			query.addChild(body)
			
			Element iq = new Element("iq")
			iq.addChild(query)
			iq.setAttribute("to", "luorc@127.0.0.1")
			
			service.sendPacket(new Iq(iq), 30, { result ->
				callback(null);
				return;
			});
		}
	}
}