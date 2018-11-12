package tigase.shiku.db;

import java.net.UnknownHostException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;

import tigase.db.DBInitException;
import tigase.shiku.model.MessageModel;
import tigase.shiku.model.MucMessageModel;

public class MongoShikuMessageArchiveRepository implements
		ShikuMessageArchiveRepository {

	// private static final String HASH_ALG = "SHA-256";
	private static final Logger log = Logger.getLogger(MongoShikuMessageArchiveRepository.class
			.getName());
	
	//mucMsg_
	private static final String MUC_MSGS_COLLECTION = "shiku_muc_msgs";
	private static final String MSGS_COLLECTION = "shiku_msgs";
	private static final String PUB_MSGS_COLLECTION = "pub_msgs";
	private static final String MUCMsg_="mucmsg_";
//	private static final String ROOM_MEMBER="shiku_room_member";
//	private static final String SHIKU_ROOM="shiku_room";
	private MongoClient mongo;
	private DB db;
	private DB mucdb;
	private DB kfdb;
	// private byte[] generateId(BareJID user) throws TigaseDBException {
	// try {
	// MessageDigest md = MessageDigest.getInstance(HASH_ALG);
	// return md.digest(user.toString().getBytes());
	// } catch (NoSuchAlgorithmException ex) {
	// throw new TigaseDBException("Should not happen!!", ex);
	// }
	// }

	private static MongoShikuMessageArchiveRepository instance = new MongoShikuMessageArchiveRepository();

	public static MongoShikuMessageArchiveRepository getInstance() {
		return instance;
	}
	@Override
	public void initRepository(String resource_uri, Map<String, String> params)
			throws DBInitException {
		try {
			MongoClientURI uri = new MongoClientURI(resource_uri);
			mongo = new MongoClient(uri);
			db = mongo.getDB(uri.getDatabase());
			mucdb=mongo.getDB("imRoom");
			//kfdb=mongo.getDB("imapi");

			// 初始化群组聊天记录集合
			DBCollection dbCollection = null;
			
			/*dbCollection=!db.collectionExists(MUC_MSGS_COLLECTION) ? db
					.createCollection(MUC_MSGS_COLLECTION, new BasicDBObject())
					: db.getCollection(MUC_MSGS_COLLECTION);
			dbCollection.createIndex(new BasicDBObject("room_jid_id", 1));
			dbCollection.createIndex(new BasicDBObject("room_jid_id", 1)
					.append("ts", 1));*/

			// 初始化聊天记录集合
			dbCollection = !db.collectionExists(MSGS_COLLECTION) ? db
					.createCollection(MSGS_COLLECTION, new BasicDBObject())
					: db.getCollection(MSGS_COLLECTION);
			dbCollection.createIndex(new BasicDBObject("sender", 1));
			dbCollection.createIndex(new BasicDBObject("receiver", 1));
			dbCollection.createIndex(new BasicDBObject("sender", 1).append(
					"receiver", 1));
			dbCollection.createIndex(new BasicDBObject("sender", 1).append(
					"receiver", 1).append("ts", 1));

		} catch (UnknownHostException ex) {
			throw new DBInitException(
					"Could not connect to MongoDB server using URI = "
							+ resource_uri, ex);
		}
	}

	@Override
	public void archiveMessage(MessageModel model) {
		
		log.log(Level.INFO, "木瓜网络[MongoShikuMessageArchiveRepository.archiveMessage]");
		
		BasicDBObject dbObj = new BasicDBObject(9);
		dbObj.put("body", model.getBody());
		dbObj.put("direction", model.getDirection());
		dbObj.put("message", model.getMessage());
		dbObj.put("receiver", model.getReceiver());
		dbObj.put("receiver_jid", model.getReceiver_jid());
		dbObj.put("sender", model.getSender());
		dbObj.put("sender_jid", model.getSender_jid());
		dbObj.put("ts", model.getTs());
		dbObj.put("type", model.getType());
		dbObj.put("contentType", model.getContentType());
		dbObj.put("messageId", model.getMessageId());
		dbObj.put("timeSend", model.getTimeSend());
		if(null != model.getContent()){
			dbObj.put("context", model.getContent());
			dbObj.put("pubNumMsgType", model.getPubNumMsgType());
			dbObj.put("pubNumCSId", model.getPubNumCSId());
			dbObj.put("publicId", model.getPublicId());
			
		}
		
		db.getCollection(MSGS_COLLECTION).insert(dbObj);
		
		/*BasicDBObject dbroomObj = new BasicDBObject();
		dbroomObj.put("_id", new ObjectId("5743c86f573edf10203301e3"));
		DBObject o=new BasicDBObject();
		dbroomObj.put("userId", 223556);
		dbroomObj.put("status", 1);
		dbroomObj.put("company_id", 1);  
		o.put("lastmsg", model.getTs());
		kfdb.getCollection("customer").update(dbroomObj, o);*/
//		DBObject q = new BasicDBObject("_id", model.getReceiver());
//		DBObject o = new BasicDBObject("$set", new BasicDBObject("loginLog.offlineTime", System.currentTimeMillis() / 1000));
//		db.getCollection(ROOM_MEMBER).update(q, o);
	}

	@Override
	public void archiveMessage(MucMessageModel model) {
		
		log.log(Level.INFO, "木瓜网络[MongoShikuMessageArchiveRepository.archiveMessage]");
		try {
			//imroom
			//mucMsg_adfsdfdsfds
			BasicDBObject dbObj = new BasicDBObject();
			dbObj.put("body", model.getBody());
			dbObj.put("event_type", model.getEvent_type());
			dbObj.put("message", model.getMessage());
			dbObj.put("nickname", "");
			dbObj.put("public_event", 0);
			dbObj.put("room_jid_id", model.getRoom_id());
			dbObj.put("room_jid", model.getRoom_jid());
			dbObj.put("sender_jid", model.getSender_jid());
			dbObj.put("sender", model.getSender());
			dbObj.put("ts", model.getTs());
			dbObj.put("contentType",model.getContentType());
			dbObj.put("messageId", model.getMessageId());
			dbObj.put("timeSend", model.getTimeSend());
			if(null != model.getContent()){
				dbObj.put("context", model.getContent());
			}
			
			
			if(!mucdb.collectionExists(MUCMsg_+model.getRoom_id()))
				mucdb.createCollection(MUCMsg_+model.getRoom_id(), new BasicDBObject());
			
			mucdb.getCollection(MUCMsg_+model.getRoom_id()).insert(dbObj);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	@Override
	public void archivePubMessage(MessageModel model) {
		BasicDBObject dbObj = new BasicDBObject(9);
		dbObj.put("body", model.getBody());
		dbObj.put("direction", model.getDirection());
		dbObj.put("message", model.getMessage());
		dbObj.put("receiver", model.getReceiver());
		dbObj.put("receiver_jid", model.getReceiver_jid());
		dbObj.put("sender", model.getSender());
		dbObj.put("sender_jid", model.getSender_jid());
		dbObj.put("ts", model.getTs());
		dbObj.put("type", model.getType());
		dbObj.put("contentType", model.getContentType());
		dbObj.put("messageId", model.getMessageId());
		dbObj.put("timeSend", model.getTimeSend());
		if(null != model.getContent()){
			dbObj.put("context", model.getContent());
			dbObj.put("pubNumMsgType", model.getPubNumMsgType());
			dbObj.put("pubNumCSId", model.getPubNumCSId());
			dbObj.put("publicId", model.getPublicId());
			
		}
		
		db.getCollection(PUB_MSGS_COLLECTION).insert(dbObj);
		
		
	}
	
//	@Override
//	public List<Integer> userIdList(String roomId) {	
//		MongoClient mongoClient;
//		try {
//			mongoClient = new MongoClient("192.168.0.25", 27017);
//			db = mongoClient.getDB("tigase");
//		} catch (UnknownHostException e) {
//			e.printStackTrace();
//		}
//		
//		BasicDBObject dbroomObj = new BasicDBObject();
//		dbroomObj.put("jid",roomId);
//		Cursor romcs=db.getCollection(SHIKU_ROOM).find(dbroomObj);	
//		BasicDBObject dbmemObj = new BasicDBObject();
//		dbmemObj.put("roomId", romcs.next().get("_id"));
//		Cursor memcs=db.getCollection(ROOM_MEMBER).find(dbmemObj);
//		List<Integer> list=new ArrayList<Integer>();
//		while(memcs.hasNext()){
//			list.add(Integer.parseInt(memcs.next().get("userId").toString()));
//		}
//		
//		return list;
//	}

}
