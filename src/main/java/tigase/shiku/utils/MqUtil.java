package tigase.shiku.utils;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

import tigase.shiku.mq.MqMessage;
import tigase.shiku.mq.RabbitConnectionPool;

public class MqUtil {
	private static final Logger log = Logger.getLogger(MqUtil.class.getName());

	private static RabbitConnectionPool connPool =null;
	private static String QUEUE_NAME=null;
	
	static {
		connPool =new RabbitConnectionPool();
		QUEUE_NAME=connPool.QUEUE_NAME;
	}


	public void send(MqMessage msg) {
		Channel channel = null;
		try {
			Connection conn = connPool.getConnection();

			 try {
	                channel = conn.createChannel();
	                if (channel == null) {
	                    throw new Exception("connection is null");
	                }
	            } finally {
	            	System.out.println("归还连接");
	                connPool.returnConnection(conn);
	            }
			channel.queueDeclare(QUEUE_NAME, true, false, false, null);
			ObjectMapper mapper = new ObjectMapper();
			String message = mapper.writeValueAsString(msg);
			channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
			 
		} catch (Exception e) {
			log.severe("======错误日志===="+"send message fail. message:" +e.getMessage());
		} finally {
			if (channel != null && channel.isOpen()) {
				try {
					System.out.println("关闭channel");
					channel.close();
				} catch (Exception e) {
					log.severe("======错误日志===="+e.getMessage());
				}
			}
		}
	}

	
	public void send(String message, String routingKey) throws Exception {
		Channel channel = null;
		try {
			Connection conn = connPool.getConnection();

			try {
				channel = conn.createChannel();
				if (channel == null) {
					log.severe("======错误日志===="+"connection is null");
				}
			} finally {
				connPool.returnConnection(conn);
			}

		} catch (Exception e) {
			// logger.error("send message fail. message:" + message);
			throw new Exception(e);
		} finally {
			if (channel != null && channel.isOpen()) {
				try {
					channel.close();
				} catch (IOException e) {
					throw new Exception(e);
				}
			}
		}
	}
	
	
	private ExecutorService executorService = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(),
			Runtime.getRuntime().availableProcessors(), 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(10_000));

	public void asyncSend(String message, String routingKey) {
		executorService.execute(() -> {
			try {
				send(message, routingKey);
			} catch (Exception e) {
				log.severe("======错误日志===="+message + "send failed, exception: " + e);
			}
		});
	}
	public void asyncSend(MqMessage msg) {
		executorService.execute(() -> {
			try {
				send(msg);
			} catch (Exception e) {
				log.severe("======错误日志===="+msg + "send failed, exception: " + e);
			}
		});
	}

}
