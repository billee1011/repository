/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.activity.redpackets;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cai.activity.ActivityType;
import com.cai.common.define.EGameType;
import com.cai.common.domain.Player;
import com.cai.common.domain.RedPackageActivityModel;
import com.cai.common.domain.Room;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.SpringService;
import com.cai.dictionary.RedPackageRuleDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.RedpackMoreRunnable;
import com.cai.game.hh.HHManager;
import com.google.common.collect.Multiset;

/**
 * 
 * 红包雨逻辑类
 * 
 * @author WalkerGeek date: 2017年9月7日 上午10:09:09 <br/>
 */
@Service
public class RedPackMoreService extends AbstractRedPackService {
	
	private Logger logger = LoggerFactory.getLogger(RedPackMoreService.class);
	
	private static RedPackMoreService instance = null;

	public static RedPackMoreService getInstance() {
		if (null == instance) {
			instance = new RedPackMoreService();
		}
		return instance;
	}
	
	/**需要发送红包的玩家及房间号ID*/
	private static Map<Integer, List<Long>> RED_ROOM_MAP = new ConcurrentHashMap<Integer, List<Long>>();
	
	/**需要发送红包的房间号ID及对应金额*/
	private static Map<Integer,Integer>  RED_ROOM_MONEY = new ConcurrentHashMap<Integer,Integer>();
	
	
	@Override
	public boolean checkReadPackReward(Room room) {
		
		try {
			RedPackageActivityModel activityModel = RedPackageRuleDict.getInstance().getRedPackageRuleMap().get(getRedPackageActivitType());
			if(activityModel == null){
				return false;
			}
			//参与活动的游戏
			if(!RedPackManager.getInstance().checkGameIsInParam(activityModel.getGame_id(), room.getGameTypeIndex())){
				return false;
			}
			
			//牌桌付豆中奖概率
			if(RandomUtil.generateRandomNumber(0, 10000) >= 
					HHManager.getInstance().getSysParamValue(EGameType.DT.getId(), 0, HHManager.VAL1,HHManager.PARAM_ID1199)  ){
				return false;
			}
			//同一牌桌有能满了领取次数将不再参与抽奖
			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
			Player[] players = room.get_players();
			for (Player player : players) {
				if(player == null){
					continue;
				}
				Multiset<Integer> multiset =centerRMIServer.getRedPackReceiveCount(player.getAccount_id());
				if(multiset.count(activityModel.getId()) >= activityModel.getPlayer_redPackage_num()){
					return false;
				}
			}
			
			//金额判断
			int activity_id = activityModel.getId();
			int money =  centerRMIServer.takeRedPackage(getRedPackageActivitType(), activity_id);
			if(money == 0 ){
				return false;
			}
			
			//保存房间数据及红包发送金额
			List<Long> accountIdArr = new ArrayList<Long>();
			for (Player player : players) {
				if(player == null){
					continue;
				}
				accountIdArr.add(player.getAccount_id());
			}
			RED_ROOM_MAP.put(room.getRoom_id(), accountIdArr);
			RED_ROOM_MONEY.put(room.getRoom_id(), money);
			
			int time = RandomUtil.generateRandomNumber(0, HHManager.getInstance().getSysParamValue(EGameType.DT.getId(), 0, HHManager.VAL2,HHManager.PARAM_ID1199));
			//WalkerGeek 正式版需要删除	
			//logger.error("延迟发红包时间/分钟："+time);
			GameSchedule.put(new RedpackMoreRunnable(room.getRoom_id(),activity_id),time, TimeUnit.MINUTES);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}

	
	@Override
	public int getRedPackageActivitType() {
		return ActivityType.RED_PACK_MORE.getType();
	}


	/**
	 * @return the rED_ROOM_MAP
	 */
	public static Map<Integer, List<Long>> getRED_ROOM_MAP() {
		return RED_ROOM_MAP;
	}


	/**
	 * @param rED_ROOM_MAP the rED_ROOM_MAP to set
	 */
	public static void setRED_ROOM_MAP(Map<Integer, List<Long>> rED_ROOM_MAP) {
		RED_ROOM_MAP = rED_ROOM_MAP;
	}


	/**
	 * @return the rED_ROOM_MONEY
	 */
	public static Map<Integer, Integer> getRED_ROOM_MONEY() {
		return RED_ROOM_MONEY;
	}


	/**
	 * @param rED_ROOM_MONEY the rED_ROOM_MONEY to set
	 */
	public static void setRED_ROOM_MONEY(Map<Integer, Integer> rED_ROOM_MONEY) {
		RED_ROOM_MONEY = rED_ROOM_MONEY;
	}
	
	
}
