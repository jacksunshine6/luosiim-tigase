package tigase.server.xmppsession;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Logger;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.ServerAddress;


import tigase.shiku.ShikuAutoReplyPlugin;
import tigase.xmpp.BareJID;


public class UserDao {

	private static final Logger log = Logger.getLogger(ShikuAutoReplyPlugin.class
			.getName());
	private static UserDao instance = new UserDao();
	public static String dbHost="";
	public static int dbPort=0;
	private static final String USER = "user";
	private static final String NEWFRIENDS = "NewFriends";
	private static final String FRIENDS = "u_friends";
	
	private static final String NOTKEYWORD="notKeyword";
	/** Field description */
	public static final String API_DB_URI_KEY = "--api-db-uri";
	public static String API_DB_URI_VAL="";
	
	//链接 tigase 数据库的 Url
	public static final String USER_DB_URI_KEY = "--user-db-uri";
	public static String USER_DB_URI="";
	/*static{
		apiDbUri=System.getProperty(UserDao.API_DB_URI);
		System.out.println("apiDbUri=====>"+apiDbUri);
	}*/
	public static UserDao getInstance() {
		return instance;
	}

	
	private  MongoClient mongoClient;
	
	
	
	private  MongoClient getMongoClient() {
		try {
			if(null!=mongoClient)
				return mongoClient;
			else{
				System.out.println("mongoClient  is ---null");
				 MongoClientOptions.Builder builder = MongoClientOptions.builder();
				 builder.socketKeepAlive(true);
				 builder.socketTimeout(20000);
				 builder.connectTimeout(20000);
				builder.maxWaitTime(12000000);
				builder.heartbeatFrequency(2000);// 心跳频率
				MongoClientURI uri = new MongoClientURI(API_DB_URI_VAL,builder);
				//uri.get
				mongoClient = new MongoClient(uri);
				return mongoClient;
			}
		} catch ( UnknownHostException e) {
			e.printStackTrace();
			return null;
		}
		
	}
	
	
	private  MongoClient tigaseClient;
	private  MongoClient getTigaseClient() {
		try {
			if(null!=tigaseClient)
				return tigaseClient;
			else{
				System.out.println("tigaseClient  is ---null");
				 MongoClientOptions.Builder builder = MongoClientOptions.builder();
				 builder.socketKeepAlive(true);
				 builder.socketTimeout(20000);
				 builder.connectTimeout(20000);
				builder.maxWaitTime(12000000);
				builder.heartbeatFrequency(2000);// 心跳频率
				MongoClientURI uri = new MongoClientURI(USER_DB_URI,builder);
				//uri.get
				tigaseClient = new MongoClient(uri);
				return tigaseClient;
			}
		} catch ( UnknownHostException e) {
			e.printStackTrace();
			return null;
		}
		
	}
	//private  DBCollection tigUserDB;
	private DBCollection getTigUserDB(){
		DBCollection dbCollection=null;
		if(null!=tigaseClient)
			 dbCollection=tigaseClient.getDB("tigase").getCollection("tig_users");
			else 
				dbCollection=getTigaseClient().getDB("tigase").getCollection("tig_users");
			return dbCollection;
		
	}
	
	private MongoClient getMongoClientByHostAndPort(){
		 MongoClientOptions.Builder builder = MongoClientOptions.builder();
		 builder.socketKeepAlive(true);
		 builder.socketTimeout(20000);
		 builder.connectTimeout(20000);
		builder.maxWaitTime(12000000);
		builder.heartbeatFrequency(2000);// 心跳频率
		 MongoClientOptions options= builder.build();
		 ServerAddress address=null;
		 try {
			 address= new ServerAddress(dbHost,dbPort);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		  mongoClient = new MongoClient(address,options);
		  return mongoClient;
	}
	
	private DBCollection getCollection(String dbName){
		DBCollection dbCollection=null;
		if(null!=mongoClient)
			 dbCollection=mongoClient.getDB("imapi").getCollection(dbName);
			else 
				dbCollection=getMongoClient().getDB("imapi").getCollection(dbName);
			return dbCollection;
		
	}
	public int getUserXmppVersion(String jid){
		int xmppVersion=0;
		try {
			jid=jid.split("/")[0];
			BasicDBObject query=new BasicDBObject("user_id",jid);
			
			BasicDBObject result =(BasicDBObject) getTigUserDB().findOne(query);
			if(null==result)
				return xmppVersion;
			else {
				xmppVersion=null==result.get("xmppVersion")?0:result.getInt("xmppVersion");
			}
			//System.out.println("getUserXmppVersion =xmmppVersion== "+result);
			return xmppVersion;
		} catch (Exception e) {
			return xmppVersion;
		}
		
	}
	public void saveOfflineTime(final String domain,final Long userId,final String connectionId) {
		System.out.println("domain===> "+domain+"  id=="+userId);
		if(null==userId&&null==connectionId||null==domain)
			return;
		new Thread(new Runnable() {
			@Override
			public void run() {
				DBObject q=null;
				
				/*List<BasicDBObject> orList=new ArrayList<BasicDBObject>(){};
				orList.add(new BasicDBObject("_id", userId));
				orList.add(new BasicDBObject("connectioId",""));*/
				if(null!=userId){
					q = new BasicDBObject("_id", userId);
					List<Object> result=getCollection(USER).distinct("connectionId", q);
					System.out.println("getConnectioId result====>"+result);
					
				}
				else {
					q = new BasicDBObject("connectionId",connectionId);
					List<Object> result=getCollection(USER).distinct("_id", q);
					//System.out.println("Query result====>"+getCollection(domain).findOne(q));
					System.out.println("getUserId result====>"+result);
				}
				DBObject o = new BasicDBObject("$set", new BasicDBObject("loginLog.offlineTime", System.currentTimeMillis() / 1000)
							.append("onlinestate", 0));
					getCollection(USER).update(q, o);
			}
		}).start();
	}
	
	public void saveOnlineState(final String domain,final Long userId,final int onlineState,final String connectionId) throws Exception{
		System.out.println("UserDao--saveOnlineState--domain====>"+domain+"---Id====>"+userId);
		if(null==userId)
			return;
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					DBObject q = new BasicDBObject("_id", userId);
					DBObject o = new BasicDBObject("$set", new BasicDBObject("onlinestate", onlineState).append("connectionId", connectionId));
					getCollection(USER).update(q, o); 

					//暂时不发离线推送
					/*HttpUtil.Request req = new HttpUtil.Request();	
					req.getData().put("userId", userId);
					req.getData().put("OnlineState", OnlineState);
					req.setSpec("http://"+domain+":8092/tigase/OnlineState");
					String result = HttpUtil.asString(req);
					System.out.println(result);*/
				} catch (Exception e) {
					e.printStackTrace();
				}
				
			}
		}).start();
		
	}
	
	public void saveNewFriendsInThread(final long userId,final long toUserId,final int direction,final int type,final String content){
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				saveNewFriends(userId, toUserId, direction,type, content);
			}
		}).start();
		
	}
	public void saveNewFriends(long userId, long toUserId,int direction, int type, String content){
		DBObject query = new BasicDBObject("userId", userId);
		query.put("toUserId", toUserId);
		DBCollection collection=getCollection(NEWFRIENDS);
		DBObject obj = collection.findOne(query);
		DBObject dbObj=new BasicDBObject();
		
		long modifyTime=System.currentTimeMillis()/1000;
		if(null==content)
			content="";
		if(null==obj){
			dbObj.put("userId", userId);
			dbObj.put("toUserId", toUserId);
			dbObj.put("direction", direction);
			dbObj.put("createTime", modifyTime);
			dbObj.put("modifyTime", modifyTime);
			dbObj.put("type", type);
			dbObj.put("content", content);
			collection.insert(dbObj);
		}else{
			dbObj.put("content", content);
			dbObj.put("direction", direction);
			dbObj.put("type", type);
			dbObj.put("content", content);
			dbObj.put("modifyTime", modifyTime);
			collection.update(query, new BasicDBObject("$set", dbObj));
		}
	}
	
	public boolean getKeyWord(String keyword) {
		DBCollection dbCollection = getCollection(NOTKEYWORD);
			BasicDBObject query = new BasicDBObject();
			query.put("word",new BasicDBObject("$regex", keyword)); //妯＄硦鏌ヨ
			long count = dbCollection.count(query);
		return count>0;
	}
	public List<String> getAllKeyWord() {
		DBCollection dbCollection = getCollection(NOTKEYWORD);
			DBCursor cursor = dbCollection.find();
			DBObject dbObj=null;
			List<String> keyWords=new ArrayList<String>();
			while(cursor.hasNext()){
				dbObj=cursor.next();
				if(null==dbObj)
					continue;
				keyWords.add(dbObj.get("word").toString());
			}
		return keyWords;
	}
	//保存最后沟通时间
	public void saveLastTalk(long sender, long receiver){
		DBObject query = new BasicDBObject("userId", receiver);
		query.put("toUserId", sender);
		DBCollection collection=getCollection(FRIENDS);
		DBObject values=new BasicDBObject();
		
		long modifyTime=System.currentTimeMillis()/1000;
		
		values.put("$set", new BasicDBObject("lastTalkTime", modifyTime));
		
		//values.put("$inc", new BasicDBObject("msgNum", 1));
		
		collection.update(query,values);
		
	}
	
	private Long getUserId(BareJID jid) {
		//得到账号ID
		String strUserId = jid.toString();
		int index = strUserId.indexOf("@");
		strUserId = strUserId.substring(0, index);

		return Long.parseLong(strUserId);
	}

}
