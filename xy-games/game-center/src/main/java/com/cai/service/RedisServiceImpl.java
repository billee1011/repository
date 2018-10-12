package com.cai.service;

import java.util.List;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

import com.cai.common.define.ERedisTopicType;
import com.cai.common.domain.Event;
import com.cai.common.util.SpringService;
import com.cai.core.MonitorEvent;
import com.cai.domain.RobotRoom;
import com.cai.domain.Session;
import com.cai.redis.service.RedisService;

import protobuf.redis.ProtoRedis.RedisResponse;

public class RedisServiceImpl extends AbstractService{
	
	private static Logger logger = LoggerFactory.getLogger(RedisServiceImpl.class);
	
	private static RedisServiceImpl instance = null;
	
	/**
	 *robotMap
	 */
	private final ConcurrentHashMap<String,List<RobotRoom>> groupRobotMap;

	private RedisServiceImpl() {
		groupRobotMap =new ConcurrentHashMap<String,List<RobotRoom>>();
	}

	public static RedisServiceImpl getInstance() {
		if (null == instance) {
			instance = new RedisServiceImpl();
		}
		return instance;
	}
	
	public List<RobotRoom> getListRobotRoom(String groupID) {
		return groupRobotMap.get(groupID);
	}
	
	public void putListRobotRoom(String groupID,List<RobotRoom> list) {
		 groupRobotMap.put(groupID, list);
	}
	
	
//	/**
//	 * 消息队列，账号信息
//	 * @param account
//	 */
//	public void convertAndSendAccount(Account account){
//		RedisTemplate redisTemplate = SpringService.getBean("redisTemplate",RedisTemplate.class);
//		JdkSerializationRedisSerializer ser = new JdkSerializationRedisSerializer(); 
//		redisTemplate.convertAndSend("java2", ser.serialize(account));
//	}
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
	
	
	/**
	 * 加入消息队列
	 * @param redisResponse
	 */
	public void convertAndSendRsResponse(RedisResponse redisResponse,ERedisTopicType eRedisTopicType){
		RedisTemplate redisTemplate = SpringService.getBean("redisTemplate",RedisTemplate.class);
		redisTemplate.convertAndSend(eRedisTopicType.getId(), redisResponse.toByteArray());
		
	}
	
	/**
	 * 加入消息队列
	 * @param redisResponse
	 */
	public void convertAndSendRsResponse(RedisResponse redisResponse,String eRedisTopicType){
		RedisTemplate redisTemplate = SpringService.getBean("redisTemplate",RedisTemplate.class);
		redisTemplate.convertAndSend(eRedisTopicType, redisResponse.toByteArray());
		
	}
	
	/**
	 * 退出程序时自动清除缓存
	 */
	public void clearCache(){
		SpringService.getBean(RedisService.class).flushDB();
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
	public void sessionCreate(Session session) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sessionFree(Session session) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void dbUpdate(int _userID) {
		// TODO Auto-generated method stub
		
	}
	
}
