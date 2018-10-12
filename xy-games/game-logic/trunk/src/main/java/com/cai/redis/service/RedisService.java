package com.cai.redis.service;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SerializeUtil;
import com.cai.common.util.ThreadUtil;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.service.MongoDBServiceImpl;

@Service
public class RedisService implements IRedisService {

	// @Autowired
	// private StringRedisTemplate stringRedisTemplate;
	@Autowired
	private RedisTemplate redisTemplate;

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
	 * @param key
	 */
	public long del(final String... keys) {

		long b = System.currentTimeMillis();
		long r = (long) redisTemplate.execute(new RedisCallback() {
			public Long doInRedis(RedisConnection connection) throws DataAccessException {

				long result = 0L;
				for (int i = 0; i < keys.length; i++) {
					result = connection.del(keys[i].getBytes());
				}
				return result;
			}
		});
		long e = System.currentTimeMillis();
		long cost = e - b;
		if (cost > WARN_TIME) {
			String keyStr = Arrays.toString(keys); 
			logger.warn("-------- redis [cmd:DEL,keys:{}] 耗时操作[{}]", keyStr, cost);
			
			if(cost>WARN_DB_TIME) {
				MongoDBServiceImpl.getInstance().server_error_log(0, ELogType.redisTimeLong, "逻辑服redis耗时 del"+cost,
						0L, keyStr, 0);
			}
			
		}

		return r;
	}

	/**
	 * @param key
	 * @param value
	 * @param liveTime
	 */
	public void set(final byte[] key, final byte[] value, final long liveTime) {
		long b = System.currentTimeMillis();
		redisTemplate.execute(new RedisCallback() {
			public Long doInRedis(RedisConnection connection) throws DataAccessException {
				connection.set(key, value);
				if (liveTime > 0) {
					connection.expire(key, liveTime);
				}
				return 1L;
			}
		});
		
		long e = System.currentTimeMillis();
		long cost = e - b;
		if (cost > WARN_TIME) {
			String keyStr = new String(key); 
			logger.warn("-------- redis [cmd:SET,key:{},liveTime:{}] 耗时操作[{}]", keyStr, liveTime, e - b);
			
			if(cost>WARN_DB_TIME) {
				MongoDBServiceImpl.getInstance().server_error_log(0, ELogType.redisTimeLong, "逻辑服redis耗时--set"+cost,
						0L, keyStr, 0);
			}
		}
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

		long b = System.currentTimeMillis();
		String r = (String) redisTemplate.execute(new RedisCallback() {
			public String doInRedis(RedisConnection connection) throws DataAccessException {
				try {
					byte[] r = connection.get(key.getBytes());
					return null == r ? null : new String(r, redisCode);
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				return "";
			}
		});
		
		long e = System.currentTimeMillis();
		long cost = e - b;
		if (cost > WARN_TIME) {
			logger.warn("-------- redis [cmd:GET,key:{}] 耗时操作[{}]", key, e - b);
			
			if(cost>WARN_DB_TIME) {
				MongoDBServiceImpl.getInstance().server_error_log(0, ELogType.redisTimeLong, "逻辑服redis耗时--get"+cost,
						0L, key, 0);
			}
		}
		
		
		return r;
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

	@SuppressWarnings("unchecked")
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

	@SuppressWarnings("unchecked")
	public <T> T get(final String key, Class<T> elementType) {

		long b = System.currentTimeMillis();

		T r = (T) redisTemplate.execute(new RedisCallback<T>() {
			@Override
			public T doInRedis(RedisConnection connection) throws DataAccessException {
				byte[] keybytes = redisTemplate.getStringSerializer().serialize(key);
				if (connection.exists(keybytes)) {
					byte[] valuebytes = connection.get(keybytes);
					@SuppressWarnings("unchecked")
					T value = (T) SerializeUtil.unserialize(valuebytes);
					return value;
				}
				return null;
			}
		});
		
		long e = System.currentTimeMillis();
		long cost = e - b;
		if (cost > WARN_TIME) {
			logger.warn("-------- redis [cmd:GET,key:{}] 耗时操作[{}]", key, e - b);
			
			if(cost>WARN_DB_TIME) {
				MongoDBServiceImpl.getInstance().server_error_log(0, ELogType.redisTimeLong, "逻辑服redis耗时--get"+cost,
						0L, key, 0);
			}
		}
		return r;
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
				MongoDBServiceImpl.getInstance().server_error_log(0, ELogType.redisTimeLong, "逻辑服redis耗时--hSet"+cost,
						0L, key, 0);
			}
		}
	}

	@SuppressWarnings("unchecked")
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
				MongoDBServiceImpl.getInstance().server_error_log(0, ELogType.redisTimeLong, "逻辑服hSetNX耗时--hSetNX"+cost,
						0L, key, 0);
			}
		}
		return r;
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

	@SuppressWarnings("unchecked")
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
				@SuppressWarnings("unchecked")
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
						0L, key);
			}
		}
		return r;
	}
	
	public void hDelListByLong(String key,List<Long> fieldList) {
		List<String> stringList = new ArrayList<>();
		fieldList.forEach((field) -> {
			stringList.add(field + "");
		});
		hDelListByString(key, stringList);
	}

	public void hDelListByString(String key,List<String> fieldList) {
		List<byte[]> byteList = new ArrayList<>();
		fieldList.forEach((field) -> {
			byteList.add(field.getBytes());
		});
		byte[][] arr = new byte[byteList.size()][];
		byteList.toArray(arr);
		
		long b = System.currentTimeMillis();
	
		redisTemplate.execute(new RedisCallback<Object>() {
			@Override
			public Object doInRedis(RedisConnection connection) throws DataAccessException {
				connection.hDel(key.getBytes(), arr);
				return null;
			}
		});
		long e = System.currentTimeMillis();
		long cost =e - b;
		if (cost > WARN_TIME) {
			logger.warn("-------- redis 耗时操作[{}] key:{},field:{}", cost, key, fieldList);
			
			if(cost>WARN_DB_TIME) {
				MongoDBServiceImpl.getInstance().server_error_log(0, ELogType.redisTimeLong, "redis耗时--hDelListByString"+cost,
						0L, key);
			}
		}
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
		redisTemplate.execute(new RedisCallback<Object>() {
			@Override
			public Object doInRedis(RedisConnection connection) throws DataAccessException {
				connection.hDel(key.getBytes(), fields.getBytes());
				return null;
			}
		});
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
