/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.s2s;

import com.cai.common.constant.S2SCmd;
import com.cai.common.handler.IServerHandler;
import com.cai.common.util.RoomIDUtil;
import com.xianyi.framework.core.transport.IServerCmd;
import com.xianyi.framework.core.transport.netty.session.S2SSession;

import protobuf.clazz.s2s.S2SProto.DelRoomNotifyProto;

/**
 * 
 * 
 *
 * @author wu_hc date: 2018年2月26日 下午3:30:27 <br/>
 */
@IServerCmd(code = S2SCmd.RM_ROOM_CACHE, desc = "登陆服务器返回")
public class LogicRmRoomCacheRspHandler extends IServerHandler<DelRoomNotifyProto> {

	@Override
	public void execute(DelRoomNotifyProto resp, S2SSession session) throws Exception {

		int serverIndex = RoomIDUtil.ID_CACHE.get(resp.getRoomId());
		if (serverIndex != -1 && serverIndex == resp.getServerIndex()) {
			RoomIDUtil.ID_CACHE.invalidate(resp.getRoomId());
		}
	}
}
