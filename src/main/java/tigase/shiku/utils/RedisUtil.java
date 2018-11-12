package tigase.shiku.utils;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisUtil{

	// 可用连接实例的最大数目，默认值为8；
	// 如果赋值为-1，则表示不限制；如果pool已经分配了maxActive个jedis实例，则此时pool的状态为exhausted(耗尽)。
	private static int MAX_ACTIVE = 1024;

	// 控制一个pool最多有多少个状态为idle(空闲的)的jedis实例，默认值也是8。
	private static int MAX_IDLE = 200;

	// 等待可用连接的最大时间，单位毫秒，默认值为-1，表示永不超时。如果超过等待时间，则直接抛出JedisConnectionException；
	private static int MAX_WAIT = 10000;

	private static int TIMEOUT = 10000;

	// 在borrow一个jedis实例时，是否提前进行validate操作；如果为true，则得到的jedis实例均是可用的；
	private static boolean TEST_ON_BORROW = true;

	private static JedisPool jedisPool = null;

	/**
	 * 初始化Redis连接池
	 */
	static {
		Properties props=new Properties();
		
		try {
			InputStream is=new FileInputStream("etc/redis.properties");
			props.load(is);
			String address = props.getProperty("redis.address");
			Integer port=Integer.parseInt( props.getProperty("redis.port"));
			Integer database=Integer.parseInt( props.getProperty("redis.database"));
			String auth=props.getProperty("redis.auth");
			if("".equals(auth)||null==auth){
				auth=null;
			}
			System.out.println("=====redis address:"+address+"port:"+port+"auth"+auth);
			//加载配置
			// 在高版本的jedis
			// jar包，比如2.8.2，我们在使用中发现使用JedisPoolConfig时，没有setMaxActive和setMaxWait属性了，这是因为高版本中官方废弃了此方法，用以下两个属性替换。
			// maxActive ==> maxTotal
			// maxWait ==> maxWaitMillis
			JedisPoolConfig config = new JedisPoolConfig();
			config.setMaxTotal(MAX_ACTIVE);
			config.setMaxIdle(MAX_IDLE);
			config.setMaxWaitMillis(MAX_WAIT);
			config.setTestOnBorrow(TEST_ON_BORROW);
			// jedisPool = new JedisPool(config, ADDR, PORT, TIMEOUT, AUTH);
			//jedisPool = new JedisPool(config, ADDR, PORT, TIMEOUT, AUTH, DATABASE, null);
			jedisPool = new JedisPool(config, address, port, TIMEOUT, auth, database, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 获取Jedis实例
	 * 
	 * @return
	 */
	public synchronized static Jedis getJedis() {
		try {
			if (jedisPool != null) {
				Jedis resource = jedisPool.getResource();
				return resource;
			} else {
				return null;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 释放jedis资源
	 * 
	 * @param jedis
	 */
	public static void returnResource(final Jedis jedis) {
		if (jedis != null) {
			jedisPool.returnResource(jedis);
		}
	}

	/**
	 * 
	 * @功能：通过Redis的key获取值，并释放连接资源
	 * 
	 * @参数：key，键值
	 * 
	 * 			@返回： 成功返回value，失败返回null
	 * 
	 */

	public static String get(String key) {

		Jedis jedis = null;

		String value = null;

		try {

			jedis = jedisPool.getResource();

			value = jedis.get(key);

		} catch (Exception e) {

			jedisPool.returnBrokenResource(jedis);

			e.printStackTrace();

		} finally {

			if (null != jedisPool) {

				jedisPool.returnResource(jedis);

			}

		}

		return value;

	}

	/**
	 * 
	 * @功能：向redis存入key和value（如果key已经存在 则覆盖），并释放连接资源
	 * 
	 * @参数：key，键
	 * 
	 * @参数：value，与key对应的值
	 * 
	 * @返回：成功返回“OK”，失败返回“0”
	 * 
	 */

	public static String set(String key, String value) {

		Jedis jedis = null;

		try {

			jedis = jedisPool.getResource();

			return jedis.set(key, value);

		} catch (Exception e) {

			jedisPool.returnBrokenResource(jedis);

			e.printStackTrace();

			return "0";

		} finally {

			if (null != jedisPool) {

				jedisPool.returnResource(jedis);

			}

		}

	}

}
