package com.cai.redis.service;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.stereotype.Service;

import com.cai.common.define.ELogType;
import com.cai.common.define.ERedisTopicType;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SerializeUtil;
import com.cai.service.MongoDBServiceImpl;

import protobuf.redis.ProtoRedis.RedisResponse;

@SuppressWarnings({ "unchecked", "rawtypes" })
@Service
public class RedisService implements IRedisService {

	// @Autowired
	// private StringRedisTemplate stringRedisTemplate;
	@Autowired
	private RedisTemplate<String, ?> redisTemplate;

	private static String redisCode = "utf-8";
	
	/**
	 * 
	 */
	private static final Logger logger = LoggerFactory.getLogger(RedisService.class);

	/**
	 */
	private static final long WARN_TIME = 100L;
	
	
	private static final long WARN_DB_TIME=1000L;


	/**
	 * 加入消息队列
	 * 
	 * @param redisResponse
	 */
	public void convertAndSendRsResponse(RedisResponse redisResponse, ERedisTopicType eRedisTopicType) {
		redisTemplate.convertAndSend(eRedisTopicType.getId(), redisResponse.toByteArray());
	}

	/**
	 * @param key
	 */
	public long del(final String... keys) {
		return (long) redisTemplate.execute(new RedisCallback() {
			public Long doInRedis(RedisConnection connection) throws DataAccessException {
				long result = 0L;
				for (int i = 0; i < keys.length; i++) {
					result = connection.del(keys[i].getBytes());
				}
				return result;
			}
		});
	}

	/**
	 * @param key
	 * @param value
	 * @param liveTime
	 */
	public void set(final byte[] key, final byte[] value, final long liveTime) {
		redisTemplate.execute(new RedisCallback() {
			public Long doInRedis(RedisConnection connection) throws DataAccessException {
				connection.set(key, value);
				if (liveTime > 0) {
					connection.expire(key, liveTime);
				}
				return 1L;
			}
		});
	}

	/**
	 * @param key
	 * @param value
	 * @param liveTime
	 */
	public void set(String key, String value, long liveTime) {
		this.set(key.getBytes(), value.getBytes(), liveTime);
	}

	/**
	 * @param key
	 * @param value
	 */
	public void set(String key, String value) {
		this.set(key, value, 0L);
	}

	/**
	 * @param key
	 * @param value
	 */
	public void set(byte[] key, byte[] value) {
		this.set(key, value, 0L);
	}

	/**
	 * @param key
	 * @return
	 */
	public String get(final String key) {
		return (String) redisTemplate.execute(new RedisCallback() {
			public String doInRedis(RedisConnection connection) throws DataAccessException {
				try {
					byte[] b = connection.get(key.getBytes());
					if (null != b) {
						return new String(connection.get(key.getBytes()), redisCode);
					} else {
						return null;
					}

				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				return "";
			}
		});
	}

	/**
	 * @param pattern
	 * @return
	 */
	public void Setkeys(String pattern) {
		redisTemplate.keys(pattern);

	}

	/**
	 * @param key
	 * @return
	 */
	public boolean exists(final String key) {
		return (boolean) redisTemplate.execute(new RedisCallback() {
			public Boolean doInRedis(RedisConnection connection) throws DataAccessException {
				return connection.exists(key.getBytes());
			}
		});
	}

	/**
	 * @return
	 */
	public String flushDB() {
		return (String) redisTemplate.execute(new RedisCallback() {
			public String doInRedis(RedisConnection connection) throws DataAccessException {
				connection.flushDb();
				return "ok";
			}
		});
	}

	/**
	 * @return
	 */
	public long dbSize() {
		return (long) redisTemplate.execute(new RedisCallback() {
			public Long doInRedis(RedisConnection connection) throws DataAccessException {
				return connection.dbSize();
			}
		});
	}

	/**
	 * @return
	 */
	public String ping() {
		return (String) redisTemplate.execute(new RedisCallback() {
			public String doInRedis(RedisConnection connection) throws DataAccessException {
				return connection.ping();
			}
		});
	}

	public void save(final String key, Object value) {
		final byte[] vbytes = SerializeUtil.serialize(value);
		redisTemplate.execute(new RedisCallback<Object>() {
			@Override
			public Object doInRedis(RedisConnection connection) throws DataAccessException {
				connection.set(redisTemplate.getStringSerializer().serialize(key), vbytes);
				return null;
			}
		});
	}

	public <T> T get(final String key, Class<T> elementType) {
		return (T) redisTemplate.execute(new RedisCallback<T>() {
			@Override
			public T doInRedis(RedisConnection connection) throws DataAccessException {
				byte[] keybytes = redisTemplate.getStringSerializer().serialize(key);
				if (connection.exists(keybytes)) {
					byte[] valuebytes = connection.get(keybytes);
					T value = (T) SerializeUtil.unserialize(valuebytes);
					return value;
				}
				return null;
			}
		});
	}

	
	public void hSet(String key, String field, Object value) {

		long b = System.currentTimeMillis();
		final byte[] vbytes = SerializeUtil.serialize(value);
		redisTemplate.execute(new RedisCallback<Object>() {
			@Override
			public Object doInRedis(RedisConnection connection) throws DataAccessException {
				connection.hSet(key.getBytes(), field.getBytes(), vbytes);
				return null;
			}
		});

		long e = System.currentTimeMillis();
		long cost = e - b;
		if (cost > WARN_TIME) {
			logger.warn("-------- redis [cmd:HSET,key:{}] 耗时操作[{}]", key, e - b);
			
			if(cost>WARN_DB_TIME) {
				MongoDBServiceImpl.getInstance().server_error_log(0, ELogType.redisTimeLong, "俱乐部redis耗时--hSet"+cost,
						0L, key,0);
			}
		}
	}

	public void hMSet(String key, Map<byte[], byte[]> hashes) {
		redisTemplate.execute(new RedisCallback<Object>() {
			@Override
			public Object doInRedis(RedisConnection connection) throws DataAccessException {
				connection.hMSet(key.getBytes(), hashes);
				return null;
			}
		});
	}

	public Map<byte[], byte[]> hGetAll(String key) {
		return (Map<byte[], byte[]>) redisTemplate.execute(new RedisCallback<Object>() {
			@Override
			public Object doInRedis(RedisConnection connection) throws DataAccessException {
				return connection.hGetAll(key.getBytes());
			}
		});
	}

	public List<byte[]> hMGet(String key, byte[]... fields) {
		return (List<byte[]>) redisTemplate.execute(new RedisCallback<Object>() {
			@Override
			public Object doInRedis(RedisConnection connection) throws DataAccessException {
				return connection.hMGet(key.getBytes(), fields);
			}
		});
	}

	public <T> List<T> getAllVals(String key) {
		return (List<T>) redisTemplate.execute(new RedisCallback<List<T>>() {

			@Override
			public List<T> doInRedis(RedisConnection connection) throws DataAccessException {
				List<byte[]> datas = connection.hVals(key.getBytes());
				List<T> temp = new ArrayList<>();
				for (byte[] ser : datas) {
					temp.add(((T) SerializeUtil.unserialize(ser)));
				}
				return temp;
			}
		});
	}

	
	public <T> T hGet(final String key, final String field, Class<T> elementType) {

		long b = System.currentTimeMillis();

		T r = (T) redisTemplate.execute(new RedisCallback<T>() {
			@Override
			public T doInRedis(RedisConnection connection) throws DataAccessException {
				byte[] valuebytes = connection.hGet(key.getBytes(), field.getBytes());
				if (valuebytes == null)
					return null;
				if (elementType == String.class) {
					return (T) new String(valuebytes);
				}
				T value = (T) SerializeUtil.unserialize(valuebytes);
				return value;
			}
		});

		long e = System.currentTimeMillis();
		long cost =e - b;
		if (cost > WARN_TIME) {
			logger.warn("-------- redis 耗时操作[{}] key:{},field:{}", cost, key, field);
			
			if(cost>WARN_DB_TIME) {
				MongoDBServiceImpl.getInstance().server_error_log(0, ELogType.redisTimeLong, "redis耗时--hGet"+cost,
						0L, key,0);
			}
		}
		return r;
	}

	public void hDel(String key, byte[]... fields) {
		
		redisTemplate.execute(new RedisCallback<Object>() {
			@Override
			public Object doInRedis(RedisConnection connection) throws DataAccessException {
				connection.hDel(key.getBytes(), fields);
				return null;
			}
		});
	}

	public void hDel(String key, String fields) {
		long b = System.currentTimeMillis();
		
		redisTemplate.execute(new RedisCallback<Object>() {
			@Override
			public Object doInRedis(RedisConnection connection) throws DataAccessException {
				connection.hDel(key.getBytes(), fields.getBytes());
				return null;
			}
		});
		
		long e = System.currentTimeMillis();
		long cost =e - b;
		if (cost > WARN_TIME) {
			logger.warn("-------- redis 耗时操作[{}] key:{},field:{}", cost, key, fields);
			
			if(cost>WARN_DB_TIME) {
				MongoDBServiceImpl.getInstance().server_error_log(0, ELogType.redisTimeLong, "redis耗时--hDel",
						0L, key,0);
			}
		}
	}
	
	public Boolean hSetNX(String key, String field, String value) {
		
		long b = System.currentTimeMillis();
		
		Boolean r = (Boolean) redisTemplate.execute(new RedisCallback<Boolean>() {
			@Override
			public Boolean doInRedis(RedisConnection connection) throws DataAccessException {
				return connection.hSetNX(key.getBytes(), field.getBytes(), value.getBytes());
			}
		});
		
		long e = System.currentTimeMillis();
		long cost = e - b;
		if (cost > WARN_TIME) {
			logger.warn("-------- redis [cmd:HSETNX,key:{}] 耗时操作[{}]", key, e - b);
			
			if(cost>WARN_DB_TIME) {
				MongoDBServiceImpl.getInstance().server_error_log(0, ELogType.redisTimeLong, "俱乐部服hSetNX耗时--hSetNX"+cost,
						0L, key, 0);
			}
		}
		return r;
	}
	
	public Boolean hExists(final String key, final String field) {

		long b = System.currentTimeMillis();

		Boolean r = (Boolean) redisTemplate.execute(new RedisCallback<Boolean>() {
			@Override
			public Boolean doInRedis(RedisConnection connection) throws DataAccessException {
				return connection.hExists(key.getBytes(), field.getBytes());
			}
		});

		long e = System.currentTimeMillis();
		long cost =e - b;
		if (cost > WARN_TIME) {
			logger.warn("-------- redis 耗时操作[{}] key:{},field:{}", cost, key, field);
			
			if(cost>WARN_DB_TIME) {
				MongoDBServiceImpl.getInstance().server_error_log(0, ELogType.redisTimeLong, "redis耗时--hExists"+cost,
						0L, key,0);
			}
		}
		return r;
	}

	public void test() {

		// PublicService publicService =
		// SpringService.getBean(PublicService.class);
		// AccountModel accountModel =
		// publicService.getPublicDAO().getAccountById(1);
		// this.save("account-"+accountModel.getAccount_id(), accountModel);
		//
		// AccountModel accountModel2 =
		// this.get("account-"+accountModel.getAccount_id(),AccountModel.class);
		// System.out.println(accountModel2);
		PerformanceTimer timer = new PerformanceTimer();
		// Map<byte[],byte[]> map = Maps.newHashMap();
		// for(int i=0;i<100000;i++){
		//
		// //JdkSerializationRedisSerializer ser = new
		// JdkSerializationRedisSerializer();
		// //redisTemplate.convertAndSend("java", ser.serialize("hello"));
		//
		//// redisTemplate.convertAndSend("java2", "hello");
		//// System.out.println(timer.getStr());
		//
		// //hSet("b1", "f"+i, new Account());
		//
		// final byte[] vbytes = SerializeUtil.serialize(new Account());
		// map.put(("f"+i).getBytes(), vbytes);
		// }
		//
		// hMSet("b3", map);
		//
		// byte[] aaa = new byte[1];
		//
		// hMGet("b3", aaa);
		//
		// System.out.println("!"+timer.getStr());

		///////////////
		for (int i = 0; i < 10; i++) {

			JdkSerializationRedisSerializer ser = new JdkSerializationRedisSerializer();
			redisTemplate.convertAndSend("java2", ser.serialize("hello"));
		}

		System.out.println("!" + timer.getStr());

	}
}
