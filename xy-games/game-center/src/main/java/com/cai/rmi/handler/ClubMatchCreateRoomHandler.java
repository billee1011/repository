package com.cai.rmi.handler;

import java.util.ArrayList;
import java.util.List;

import com.cai.common.constant.RMICmd;
import com.cai.common.domain.ClubRoomModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.common.rmi.vo.ClubMatchCreateRoomVo;
import com.cai.common.util.SpringService;

/**
 * 
 *
 * @author zhanglong date: 2018年7月3日 下午2:31:10
 */
@IRmi(cmd = RMICmd.CLUB_MATCH_CREATE_ROOM, desc = "亲友圈自建赛创建房间 ")
public class ClubMatchCreateRoomHandler extends IRMIHandler<ClubMatchCreateRoomVo, List<ClubRoomModel>> {

	@Override
	protected List<ClubRoomModel> execute(ClubMatchCreateRoomVo req) {
		ICenterRMIServer centerRmiServer = SpringService.getBean(ICenterRMIServer.class);
		List<ClubRoomModel> list = new ArrayList<>();
		for (int i = 0; i < req.getRoomNum(); i++) {
			ClubRoomModel clubRoomModel = centerRmiServer.createClubRoom(req.getClubOwnerId(), req.getClubId(), req.getClubRuleModel(),
					req.getClubOwnerId(), req.getClubName(), req.isRepair(), req.getTableIndex(), req.getClubMemSize(), req.getLogicId());
			list.add(clubRoomModel);
		}

		return list;
	}

}
