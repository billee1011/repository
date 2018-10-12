/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.client;

import com.cai.common.constant.C2SCmd;
import com.cai.constant.Club;
import com.cai.constant.ClubRuleTable;
import com.cai.service.ClubCacheService;
import com.cai.service.ClubService;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import protobuf.clazz.ClubMsgProto.ClubCommon;
import protobuf.clazz.ClubMsgProto.ClubSceneTagProto;
import protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * 
 * 
 *
 * @author wu_hc date: 2018年4月16日 下午5:33:48 <br/>
 */
@ICmd(code = C2SCmd.CLUB_SCENE_TAG, desc = "俱乐部场景相关")
public final class ClubSceneTagHandler extends IClientExHandler<ClubSceneTagProto> {

	@Override
	protected void execute(ClubSceneTagProto req, TransmitProto topReq, C2SSession session) throws Exception {

		if (req.getCategory() == 1) { // 俱乐部列表
			ClubCacheService.getInstance().sit(topReq.getAccountId(), ClubService.currentSeat);

		} else if (req.getCategory() == 2) { // 俱乐部&包间
			if (req.hasScene()) {
				ClubCommon scene = req.getScene();
				final int clubId = scene.getClubId();
				final int ruleId = scene.getRuleId();

				if (clubId <= 0) {
					return;
				}

				if (ruleId > 0) { // 包间
					Club club = ClubService.getInstance().getClub(clubId);
					if (null != club) {
						ClubRuleTable ruleTable = club.ruleTables.get(ruleId);
						if (null != ruleTable) {
							ClubCacheService.getInstance().sit(topReq.getAccountId(), ruleTable.currentSeat());
						}
					}
				} else {
					Club club = ClubService.getInstance().getClub(clubId);
					if (null != club) {
						ClubCacheService.getInstance().sit(topReq.getAccountId(), club.currentSeat());
					}
				}
			}

		} else { // 离开俱乐部界面

//			ClubCacheService.getInstance().out(topReq.getAccountId());
		}
	}
}
