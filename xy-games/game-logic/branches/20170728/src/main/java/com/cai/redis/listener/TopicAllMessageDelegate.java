package com.cai.redis.listener;

import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.serializer.support.DeserializingConverter;
import org.springframework.core.serializer.support.SerializingConverter;

import com.cai.common.domain.Player;
import com.cai.common.domain.Room;
import com.cai.common.util.PerformanceTimer;
import com.cai.dictionary.GoodsDict;
import com.cai.dictionary.SysParamDict;
import com.cai.service.PlayerServiceImpl;

import protobuf.redis.ProtoRedis;
import protobuf.redis.ProtoRedis.RedisResponse;
import protobuf.redis.ProtoRedis.RedisResponse.RsResponseType;
import protobuf.redis.ProtoRedis.RsCmdResponse;
import protobuf.redis.ProtoRedis.RsDictUpdateResponse;
import protobuf.redis.ProtoRedis.RsDictUpdateResponse.RsDictType;
import protobuf.redis.ProtoRedis.RsRoomResponse;

/**
 * 通用主题监听
 * @author run
 *
 */
public class TopicAllMessageDelegate implements MessageDelegate {

	private static Logger logger = LoggerFactory.getLogger(TopicAllMessageDelegate.class);


	private AtomicLong mesCount = new AtomicLong();
	
	
	private final Converter<Object, byte[]> serializer;
	private final Converter<byte[], Object> deserializer;
	
	
	public TopicAllMessageDelegate(){
		this.serializer = new SerializingConverter();
		this.deserializer = new DeserializingConverter();
	}
	

	@Override
	public void handleMessage(byte[] message) {
		
		mesCount.incrementAndGet();
		//logger.info("接收到redis消息队列==>"+mesCount.get());
		//临时关闭，本地测试
		if(false)
			return;
		try {

			RedisResponse redisResponse = ProtoRedis.RedisResponse.parseFrom(message);
			int type = redisResponse.getRsResponseType().getNumber();
			switch (type) {
			
			//字典更新
			case RsResponseType.DICT_UPDATE_VALUE:
			{
				RsDictUpdateResponse rsDictUpdateResponse = redisResponse.getRsDictUpdateResponse();
				RsDictType rsDictType = rsDictUpdateResponse.getRsDictType();
				switch (rsDictType.getNumber()) {
				//系统参数
				case RsDictType.SYS_PARAM_VALUE:
				{
					logger.info("收到redis消息更新SysParamDict字典");
					SysParamDict.getInstance().load();//系统参数
					break;
				}
				case RsDictType.GOODS_VALUE: {
					logger.info("收到redis消息更新GoodsDict字典");
					GoodsDict.getInstance().load();//道具
					break;
				}
				
				}
			}
			
			//房间
			case RsResponseType.ROOM_VALUE:
			{
				RsRoomResponse rsRoomResponse = redisResponse.getRsRoomResponse();
				
				int type2 = rsRoomResponse.getType();
				//删除房间
				if(type2 == 1){
					Integer room_id = rsRoomResponse.getRoomId();
					//找到所有玩家
					Room room = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);
					if(room!=null){
						room.force_account();
//						for(Player player : room.get_players()){
//							//防止出错，只有房间是一样的才清除玩家缓存
//							if(player!=null && player.getRoom_id()==room_id){
//								PlayerServiceImpl.getInstance().getPlayerMap().remove(player.getAccount_id());
//							}
//						}
						PlayerServiceImpl.getInstance().getRoomMap().remove(room_id);
					}
				}
				
				break;
			}
			
			case RsResponseType.CMD_VALUE: {
				PerformanceTimer timer = new PerformanceTimer();
				RsCmdResponse rsCmdResponse = redisResponse.getRsCmdResponse();
				//强制结算
				if(rsCmdResponse.getType()==2){
					for(Room room : PlayerServiceImpl.getInstance().getRoomMap().values()){
						room .force_account();
						logger.info("强制结算房间:room_id:"+room.getRoom_id());
					}
					
					logger.info("=========强制结算房间完成================"+timer.getStr());
				}
				
				break;
			}
			
			default:
				break;
			}
			
			
		} catch (Exception e) {
			logger.error("error", e);
		}
	}



}
