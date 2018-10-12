package com.cai.util;

import java.util.List;
import com.cai.common.base.BaseTask;
import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.Player;
import com.cai.common.util.SpringService;
import com.cai.game.AbstractRoom;
import com.cai.redis.service.RedisService;
import com.cai.service.AiService;
import com.cai.service.PlayerServiceImpl;

public class SystemRoomUtil {
	public static int getRoomId(long accountId){
		String roomId = SpringService.getBean(RedisService.class).hGet(RedisConstant.ROOM_INFO, accountId+"",String.class);
		if(roomId == null){
			return 0;
		}
		return Integer.parseInt(roomId);
	}
	
	/**
	 * 小局结算
	 * @param room
	 */
	public static void onRoomFinish(AbstractRoom room ,List<Player> trutessList) {
		if(trutessList == null || trutessList.size() <= 0){
			return;
		}
		// 最后一局不执行
		if (room._cur_round < room._game_round && PlayerServiceImpl.getInstance().getRoomMap().containsKey(room.getRoom_id())) {
			AiService.getInstance().schedule(room.getRoom_id(), new BaseTask() {
				
				@Override
				public String getTaskName() {
					return "onRoomFinish-"+room.getRoom_id();
				}
				
				@Override
				public void execute() {
					try {
						room.getRoomLock().lock();
						trutessList.forEach((player) ->{
							player.readyGame(player.get_seat_index());
						});
					}catch (Exception e) {
					}finally {
						room.getRoomLock().unlock();
					}
				}
			}, 4000);
		}
	}
	

}
