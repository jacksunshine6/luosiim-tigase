package tigase.shiku.mq;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class RabbitConnectionPool {

	private static final Logger log = Logger.getLogger(RabbitConnectionPool.class.getName());

	private static final int INIT_SIZE = 20; // 连接池初始化大小

	private static final int MAX_SIZE = 1000; // 连接池的最大值
	private static final int DEFAULT_RETURN_INTERVAL_MILLS = 5000; // 默认返回间隔毫秒
	private static final int DEFAULT_WAIT_TIME = 2000; // 默认等待时间
	public static String QUEUE_NAME = "ole-push-dev";
	private String url;
	private int port;
	private String username;
	private String password;
	/**
	 * 正常的连接的队列
	 */
	private LinkedBlockingDeque<Connection> connList = null;
	/**
	 * 异常连接的队列
	 */
	private LinkedBlockingDeque<Connection> connClosedList = null;
	private Timer timer;
	
	public RabbitConnectionPool() {
		System.out.println("初始化RabbitConnectionPool");
		if(connList==null){
			connList = new LinkedBlockingDeque<Connection>(MAX_SIZE);
		}
		if(connClosedList==null){
			connClosedList = new LinkedBlockingDeque<Connection>(MAX_SIZE);
		}
		if(timer==null){
			timer=new Timer();
		}
		// 初始化连接的配置
		this.initProperties();
		// 初始化数据库的连接池
		this.initPool();
	}

	private void initProperties() {
		log.info("======日志====读取配置文件信息");
		Properties dbPro = new Properties();
		// InputStream input =
		// this.getClass().getResourceAsStream("mq.properties");
		try {
			InputStream input = new FileInputStream("etc/mq.properties");
			dbPro.load(input);
			this.url = dbPro.getProperty("rabbitmq.host");
			this.port = Integer.parseInt(dbPro.getProperty("rabbitmq.port"));
			this.username = dbPro.getProperty("rabbitmq.user");
			this.password = dbPro.getProperty("rabbitmq.password");
			QUEUE_NAME = dbPro.getProperty("rabbitmq.queue.name");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * <p>Title: initPool</p>  
	 * <p>Description: 初始化创建连接</p>
	 */
	private void initPool() {
		try {
            rtnConnTask();
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(url);// MQ的IP  
 	        factory.setPort(port);// MQ端口  
 	        factory.setUsername(username);// MQ用户名  
 	        factory.setPassword(password);// MQ密码  
            factory.setAutomaticRecoveryEnabled(true);
            factory.setRequestedHeartbeat(5);
            factory.setConnectionTimeout(2000);
            for (int i = 0; i < INIT_SIZE; i++) {
            	System.out.println("创建连接"+i);
            	Connection connection = factory.newConnection();
                connList.add(connection);
            }
        } catch (Exception e) {
        	log.severe("======错误日志===="+e.getMessage());
        }
		 
	}
	
	
	 public Connection getConnection() throws Exception {
	        Connection connection = null;
	        try {
	            connection = connList.poll(DEFAULT_WAIT_TIME, TimeUnit.MILLISECONDS);
	            System.out.println("获取连接"+connection);
	        } catch (InterruptedException e) {
	        	log.severe("======错误日志===="+e.getMessage());
	        }
	        if (connection == null && connList.size()<MAX_SIZE) {
	        	System.out.println("创建新连接");
	        	ConnectionFactory factory = new ConnectionFactory();
	            factory.setHost(url);// MQ的IP  
	 	        factory.setPort(port);// MQ端口  
	 	        factory.setUsername(username);// MQ用户名  
	 	        factory.setPassword(password);// MQ密码  
	            factory.setAutomaticRecoveryEnabled(true);
	            factory.setRequestedHeartbeat(5);
	            factory.setConnectionTimeout(2000);
	            connection=factory.newConnection();
	        	connList.add(connection);
	            log.info("======日志===="+"rabbitMQ connections pool full and conn are all in using");
	        }
	        if (!connection.isOpen()) {
	            connClosedList.offer(connection);
	            return getConnection();
	        }
	        return connection;
	    }
	
	
	
	
	/**
	 * 
	 * <p>Title: rtnConnTask</p>  
	 * <p>Description: 将恢复的异常connection放入正常的queue的task</p>
	 */
	 public void rtnConnTask() {
	        timer.schedule(new TimerTask() {
	            @Override
	            public void run() {
	                try {
	                    rtnConnToClosedConnList();
	                } catch (Exception e) {
	                	log.severe("======错误日志===="+"rtnConnToClosedConnList failed");
	                }
	            }
	        }, DEFAULT_RETURN_INTERVAL_MILLS, DEFAULT_RETURN_INTERVAL_MILLS);
	    }
	/**
	 * 
	 * <p>Title: rtnConnToClosedConnList</p>  
	 * <p>Description: </p>  
	 * @throws Exception
	 */
	 private void rtnConnToClosedConnList() throws Exception {
	        Connection connection = connClosedList.poll();
	        if (connection == null) {
	            return;
	        } else {
	            boolean flag = connection.isOpen() ? connList.offer(connection) : connClosedList.offer(connection);
	            if (!flag) {
	                log.severe("======错误日志===="+"RabbitMQConnectionLost : rtnConnToClosedConnList cause ERROR");
	            }
	        }

	    }
	 
	 /**
	  * 
	  * <p>Title: returnConnection</p>  
	  * <p>Description: 归还connection</p>  
	  * @param connection
	  */
	 public void returnConnection(Connection connection) {
	        if (!connList.offer(connection)){
	        	log.severe("======错误日志===="+"RabbitMQConnectionLost : returnConnection cause ERROR");
	        }
	    }
}
