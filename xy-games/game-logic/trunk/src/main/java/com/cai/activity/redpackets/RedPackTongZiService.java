package com.cai.activity.redpackets;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.activity.ActivityType;
import com.cai.coin.CoinTable;
import com.cai.common.define.EGameType;
import com.cai.common.domain.Player;
import com.cai.common.domain.RedPackageActivityModel;
import com.cai.common.domain.Room;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.SpringService;
import com.cai.dictionary.RedPackageRuleDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.RedPackTongZiRunnable;
import com.cai.game.hh.HHManager;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;

/**
 * 运财童子红包逻辑实现
 * 
 * @author yu
 *
 */
public class RedPackTongZiService extends AbstractRedPackService {

	
	private static final Logger logger = LoggerFactory.getLogger(RedPackTongZiService.class);
	
	private static RedPackTongZiService instance = null;
	
	public static RedPackTongZiService getInstance() {
		if (null == instance) {
			instance = new RedPackTongZiService();
		}
		return instance;
	}
	
	@Override
	public int getRedPackageActivitType() {
		return ActivityType.YUM_CAI_TON_ZI.getType();
	}

	@Override
	public boolean checkReadPackReward(Room room) {
		try {
			Player[] players = room.get_players();

			RedPackageActivityModel activityModel = RedPackageRuleDict.getInstance().getRedPackageRuleMap().get(getRedPackageActivitType());
			if(activityModel == null){
				return false;
			}
			//参与活动的游戏
			if(!RedPackManager.getInstance().checkGameIsInParam(activityModel.getGame_id(), room.getGameTypeIndex())){
				return false;
			}
			
			// RMI 中心服取红包数据
			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
			int money = centerRMIServer.takeRedPackage(ActivityType.YUM_CAI_TON_ZI.getType(), activityModel.getId());
			if (money == 0) {
				return false;
			}
			
			List<Player> randomList = new ArrayList<Player>();
			for (Player p : filterPlayer(players,activityModel)) {
				// 牌桌付豆中奖概率
				if (RandomUtil.generateRandomNumber(0, 10000) >= HHManager.getInstance().getSysParamValue(EGameType.DT.getId(), 0, HHManager.VAL3, HHManager.PARAM_ID1199)) {
					return false;
				}
				randomList.add(p);
			}
			
			
			//单个房间只选择一个人
			int index = RandomUtil.generateRandomNumber(0, randomList.size());
			Player p = randomList.get(index);
			if(p == null){
				return false;
			}
			
			int time  = RandomUtil.generateRandomNumber(0, HHManager.getInstance().getSysParamValue(EGameType.DT.getId(), 0, HHManager.VAL4, HHManager.PARAM_ID1199));
			GameSchedule.put(new RedPackTongZiRunnable(p.getAccount_id(), activityModel.getId(),money),time, TimeUnit.MINUTES);
		} catch (Exception e) {
			logger.error("error",e);
			return false;
		}
		return true;
	}

	private Player[] filterPlayer(Player[] players,RedPackageActivityModel activityModel) {
		List<Player> list = Lists.newArrayList();
		for (Player player : players) {
			if(player == null){
				continue;
			}
			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
			if(centerRMIServer.isLessRoundNum(player.getAccount_id(), activityModel.getBig_roundNum())) {
				continue;
			}
			
			//活动期间内抽奖次数校验
			Multiset<Integer> model = centerRMIServer.getRedPackReceiveCount(player.getAccount_id());
			if (model.count(activityModel.getId()) >= activityModel.getPlayer_redPackage_num())
				continue;
			list.add(player);
		}

		return list.toArray(new Player[list.size()]);
	}
	

}
