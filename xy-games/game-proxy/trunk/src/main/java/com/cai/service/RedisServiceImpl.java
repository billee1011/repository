package com.cai.service;

import java.util.SortedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

import com.cai.common.define.ERedisTopicType;
import com.cai.common.domain.Event;
import com.cai.common.util.SpringService;
import com.cai.core.MonitorEvent;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.server.AbstractService;

import protobuf.redis.ProtoRedis.RedisResponse;

public class RedisServiceImpl extends AbstractService{
	
	private static Logger logger = LoggerFactory.getLogger(RedisServiceImpl.class);
	
	private static RedisServiceImpl instance = null;
	
	

	private RedisServiceImpl() {
	}

	public static RedisServiceImpl getInstance() {
		if (null == instance) {
			instance = new RedisServiceImpl();
		}
		return instance;
	}
	
	
//	/**
//	 * 消息队列，账号信息
//	 * @param account
//	 */
//	public void convertAndSendAccount(Account account){
//		RedisTemplate redisTemplate = SpringService.getBean("redisTemplate",RedisTemplate.class);
//		JdkSerializationRedisSerializer ser = new JdkSerializationRedisSerializer(); 
//		System.out.println("对象大小："+ser.serialize(account).length);
//		redisTemplate.convertAndSend("java2", ser.serialize(account));
//	}
//	
//	
//	/**
//	 * 消息队列
//	 * @param account
//	 */
//	public void convertAndSendObject(Object object){
//		RedisTemplate redisTemplate = SpringService.getBean("redisTemplate",RedisTemplate.class);
//		JdkSerializationRedisSerializer ser = new JdkSerializationRedisSerializer(); 
//		redisTemplate.convertAndSend("java2", ser.serialize(object));
//	}
//	
	/**
	 * 加入消息队列
	 * @param redisResponse
	 */
	public void convertAndSendRsResponse(RedisResponse redisResponse,ERedisTopicType eRedisTopicType){
		RedisTemplate redisTemplate = SpringService.getBean("redisTemplate",RedisTemplate.class);
		redisTemplate.convertAndSend(eRedisTopicType.getId(), redisResponse.toByteArray());
	}
	
	
	

	@Override
	protected void startService() {
	}

	@Override
	public MonitorEvent montior() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onEvent(Event<SortedMap<String, String>> event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sessionCreate(C2SSession session) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sessionFree(C2SSession session) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void dbUpdate(int _userID) {
		// TODO Auto-generated method stub
		
	}
	
}
