/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.activity.redpackets;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.S2CCmd;
import com.cai.common.domain.Player;
import com.cai.common.domain.Room;
import com.cai.common.util.RandomUtil;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.activity.ActivityRedPackProto.RedPackSendResp;

/**
 * 
 * 红包活动管理类
 * 
 * @author WalkerGeek date: 2017年9月7日 上午10:10:34 <br/>
 */
public class RedPackManager {
	
	private static final Logger logger  = LoggerFactory.getLogger(RedPackManager.class);

	private ExecutorService executor = Executors.newSingleThreadExecutor();
	
	private static RedPackManager instance = null;

	public static RedPackManager getInstance() {
		if (null == instance) {
			instance = new RedPackManager();
		}
		return instance;
	}

	public  void checkReadPackReward(Room room) {
		executor.submit(new Runnable() {

			@Override
			public void run() {
				//两个类型的红包同一个房间只能获得一个
				boolean has_send = false;
				//随机红包活动的抽取顺序
				if(RandomUtil.generateRandomNumber(0, 10000)<=5000){
					if (RedPackTongZiService.getInstance().isStart() ) {
						has_send =RedPackTongZiService.getInstance().checkReadPackReward(room);
					}
					if (RedPackMoreService.getInstance().isStart() && !has_send) {
						RedPackMoreService.getInstance().checkReadPackReward(room);
					}
				}else{
					if (RedPackMoreService.getInstance().isStart() ) {
						RedPackMoreService.getInstance().checkReadPackReward(room);
					}
					if (RedPackTongZiService.getInstance().isStart() && !has_send) {
						has_send =RedPackTongZiService.getInstance().checkReadPackReward(room);
					}
				}
			}
		});
	}
	
	
	/**
	 * 判断配置信息是否包含本子游戏ID
	 * @param param
	 * @param gameId
	 * @return
	 */
	public boolean checkGameIsInParam(String params,int gameId){
		String[] paramArr =  params.split(",");
		for (String string : paramArr) {
			int tempGameId = Integer.parseInt(string);
			if(tempGameId == 0){
				return true;
			}else if(tempGameId == gameId){
				return true;
			}
		}
		
		return false;
	}
	
	
	/**
	 * 发送红包雨
	 * @param money
	 * @param accountArr
	 * @return
	 */
	public boolean operate_send_card(int activityType,int money,long accountId,List<Long> accountArr) {
		if (accountArr.size() == GameConstants.INVALID_VALUE  || accountId == 0L) {
			return false;
		}
		Player player = PlayerServiceImpl.getInstance().getPlayerMap().get(accountId);
		RedPackSendResp.Builder sendRedPack  = RedPackSendResp.newBuilder();
		sendRedPack.setActivityType(activityType);
		for(int j = 0; j<accountArr.size() ;j++ ){
			Long accountId_other = accountArr.get(j);
			if(accountId_other == null || accountId_other == 0L){
				continue;
			}
			sendRedPack.addSeatIndex(PlayerServiceImpl.getInstance().getPlayerMap().get(accountId_other).get_seat_index());
		}
		//logger.error("红包调度成功,房间["+sendRedPack.getSeatIndexList()+"]"+accountId);
		PlayerServiceImpl.getInstance().sendExMsg(player, S2CCmd.RESPONSE_SEND_RED_PACK, sendRedPack);
		return true;
	}
	
	
	
	/**
	 * 发送红包雨全房间
	 * @param money
	 * @param accountArr
	 * @return
	 */
	public boolean operate_send_card_room(int activityType,int money,int roomId,List<Long> accountArr) {
		if (accountArr.size() == GameConstants.INVALID_VALUE  || roomId == 0) {
			return false;
		}
		//Build协议
		RedPackSendResp.Builder sendRedPack  = RedPackSendResp.newBuilder();
		sendRedPack.setActivityType(activityType);
		for(int j = 0; j<accountArr.size() ;j++ ){
			Long accountId_other = accountArr.get(j);
			if(accountId_other == null || accountId_other == 0L){
				continue;
			}
			sendRedPack.addSeatIndex(PlayerServiceImpl.getInstance().getPlayerMap().get(accountId_other).get_seat_index());
		}
		
		Room room = PlayerServiceImpl.getInstance().getRoomMap().get(roomId);
		if(room == null){
			logger.error("发送红包雨出错 房间不存在");
			return false;
		}
		Player[] players = room.get_players();
		for(Player player:players){
			if(player == null){
				continue;
			}
			PlayerServiceImpl.getInstance().sendExMsg(player, S2CCmd.RESPONSE_SEND_RED_PACK, sendRedPack);
		}
		return true;
	}
	
    
   
}
