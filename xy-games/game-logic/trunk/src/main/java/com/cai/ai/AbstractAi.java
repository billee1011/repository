package com.cai.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.cai.game.AbstractRoom;
import com.cai.service.AiService;
import protobuf.clazz.Protocol.RoomResponse;

public abstract class AbstractAi<T extends AbstractRoom> {

	private static final long INVAILD_TIME = -1; //无效时间
	public Logger logger = LoggerFactory.getLogger(getClass());

	public void beforeExe(T t, RobotPlayer player, RoomResponse rsp) {
		if(player == null){
			return;
		}
		player.cancel();
		// 是否需要执行
		if (!isNeedExe(t, player, rsp)) {
			return;
		}
		AiWrap aiWrap = needDelay(t, player, rsp);

		//++t.aiFlag[player.get_seat_index()]
		// 延时
		if (aiWrap.getDelayTime() > 0) {
			aiWrap.setMaxTrusteeTime(getMaxTrusteeTime(t));
			AiService.getInstance().schedule(player.getAccount_id(),
					this, player, rsp, t, ++t.aiFlag[player.get_seat_index()], aiWrap);
			return;
		}
		doExe(t, player, rsp);
	}

	/**
	 * 开始处理
	 * 
	 * @param t
	 * @param player
	 * @param rsp
	 */
	public boolean doExe(T t, RobotPlayer player, RoomResponse rsp) {
		if (!isNeedExe(t, player, rsp)) {
			return false;
		}

		onExe(t, player, rsp);
		return true;
	}

	/**
	 * 是否需要执行该命令
	 * 
	 * @param t
	 * @param player
	 * @param rsp
	 * @return
	 */
	protected abstract boolean isNeedExe(T table, RobotPlayer player, RoomResponse rsp);

	/**
	 * 处理数据包
	 * 
	 * @param t
	 * @param player
	 * @param rsp
	 */
	public abstract void onExe(T table, RobotPlayer player, RoomResponse rsp);

	/**
	 * 延迟时间
	 * 
	 * @param table
	 * @param player
	 * @param rsp
	 * @return
	 */
	protected abstract AiWrap needDelay(T table, RobotPlayer player, RoomResponse rsp);
	
	/**
	 * 最大进入托管时间
	 */
	public long getMaxTrusteeTime(T t){
		return INVAILD_TIME;
	}
	
	/**
	 * 是否有效最大托管时间
	 */
	public boolean isValidMaxTrusteeTime(T t){
		if(getMaxTrusteeTime(t) == INVAILD_TIME){
			return false;
		}
		return true;
	}
}
