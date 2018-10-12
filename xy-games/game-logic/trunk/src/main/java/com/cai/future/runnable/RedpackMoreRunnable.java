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
import com.cai.future.BaseFuture;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;
import com.cai.service.RedPackageServiceImp;

public class RedpackMoreRunnable extends BaseFuture {

	private static Logger logger = LoggerFactory.getLogger(RedpackMoreRunnable.class);

	private int _room_id;
	private int _activity_id;

	public RedpackMoreRunnable(int room_id, int activity_id) {
		super(room_id);
		_room_id = room_id;
		_activity_id = activity_id;
	}

	@Override
	public void execute() {
		try {
			Room room = PlayerServiceImpl.getInstance().getRoomMap().get(_room_id);
			if (room == null) {
				logger.error("红包调度失败,房间[" + _room_id + "]不存在");
				return;
			}

			List<RedPackageModel> logList = new ArrayList<RedPackageModel>();
			List<Long> accountIdList = RedPackMoreService.getRED_ROOM_MAP().get(_room_id);

			for (Long accountId : accountIdList) {
				Player player = PlayerServiceImpl.getInstance().getPlayerMap().get(accountId);
				// 客户端发送
				boolean flag = RedPackManager.getInstance().operate_send_card(ActivityType.RED_PACK_MORE.getType(),
						RedPackMoreService.getRED_ROOM_MONEY().get(_room_id), accountId, RedPackMoreService.getRED_ROOM_MAP().get(_room_id));
				if (!flag) {
					continue;
				}
				RedPackageModel redPackageModel = new RedPackageModel();
				redPackageModel.setCreate_time(new Date());
				redPackageModel.setAccount_id(player.getAccount_id());
				redPackageModel.setActive_id(_activity_id);
				redPackageModel.setActive_type(ActivityType.RED_PACK_MORE.getType());
				redPackageModel.setMoney(RedPackMoreService.getRED_ROOM_MONEY().get(_room_id));
				redPackageModel.setNick_name(player.getNick_name());
				redPackageModel.setRoom_id(_room_id);
				logList.add(redPackageModel);
				// 存DB
				RedPackageServiceImp.getInstance().addRedPackageRecord(accountId, RedPackMoreService.getRED_ROOM_MONEY().get(_room_id), _activity_id);
			}
			// 入DB
			MongoDBServiceImpl.getInstance().all_red_package_active_log(logList);
		} catch (Exception e) {
			logger.error("红包雨--发生错：", e);
		}

	}

}
