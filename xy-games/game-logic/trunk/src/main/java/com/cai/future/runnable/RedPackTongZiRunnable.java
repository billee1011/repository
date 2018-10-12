/**
 * 
 */
package com.cai.future.runnable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 定时发送红包雨 
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.activity.ActivityType;
import com.cai.activity.redpackets.RedPackManager;
import com.cai.activity.redpackets.RedPackMoreService;
import com.cai.common.domain.Player;
import com.cai.common.domain.RedPackageModel;
import com.cai.common.domain.Room;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.SpringService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;
import com.cai.service.RedPackageServiceImp;
import com.google.common.collect.Lists;

public class RedPackTongZiRunnable implements Runnable {

	private static Logger logger = LoggerFactory.getLogger(RedPackTongZiRunnable.class);

	private long accountId;
	private int activityId;
	private int money;

	public RedPackTongZiRunnable(long accountId, int activityId,int money) {
		this.accountId = accountId;
		this.activityId = activityId;
		this.money = money;
	}

	@Override
	public void run() {
		try {
			Player player = PlayerServiceImpl.getInstance().getPlayerMap().get(accountId);
			int roomId = player.getRoom_id();
			Room room = PlayerServiceImpl.getInstance().getRoomMap().get(roomId);
			if (room == null) {
				logger.error("红包调度失败,房间[" + roomId + "]不存在");
				return;
			}
			List<Long> list =  new ArrayList<Long>();
			list.add(accountId);
			//客户端发送
			boolean flag =  RedPackManager.getInstance().operate_send_card_room(ActivityType.YUM_CAI_TON_ZI.getType(), money ,roomId, list);
			if(!flag){
				return;
			}
			

			List<RedPackageModel> logList = Lists.newArrayList();
			RedPackageModel redPackageModel = new RedPackageModel();
			redPackageModel.setCreate_time(new Date());
			redPackageModel.setAccount_id(player.getAccount_id());
			redPackageModel.setActive_id(activityId);
			redPackageModel.setActive_type(ActivityType.YUM_CAI_TON_ZI.getType());
			redPackageModel.setMoney(money);
			redPackageModel.setNick_name(player.getNick_name());
			redPackageModel.setRoom_id(roomId);
			logList.add(redPackageModel);
			//存DB
			RedPackageServiceImp.getInstance().addRedPackageRecord(accountId, money,activityId);
			MongoDBServiceImpl.getInstance().all_red_package_active_log(logList);

		} catch (Exception e) {
			logger.error("红包雨--运财童子活动发生错：", e);
			
		}

	}

}
