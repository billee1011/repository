/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.rmi.handler;

import com.cai.common.constant.RMICmd;
import com.cai.common.domain.Room;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.service.PlayerServiceImpl;

import protobuf.redis.ProtoRedis.RsAccountResponse;

/**
 * 
 *
 * @author wu_hc date: 2017年10月17日 下午4:06:12 <br/>
 */
@IRmi(cmd = RMICmd.DEL_ROOM, desc = "删除房间")
public final class DelRoomRMIHandler extends IRMIHandler<RsAccountResponse, Void> {

	@Override
	public Void execute(RsAccountResponse message) {
		int room_id = message.getRoomId();
		// 找到所有玩家
		Room room = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);
		if (room != null) {
			room.force_account("-");
			// huandou(room);
			PlayerServiceImpl.getInstance().getRoomMap().remove(room_id);
		}

		return null;
	}

}
