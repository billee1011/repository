package com.cai.handler.client;

import java.util.List;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.constant.S2SCmd;
import com.cai.common.domain.ClubRuleRecordModel;
import com.cai.common.type.ClubRecordDayType;
import com.cai.common.util.PBUtil;
import com.cai.config.ClubCfg;
import com.cai.constant.Club;
import com.cai.constant.ClubRuleTable;
import com.cai.service.ClubService;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import protobuf.clazz.ClubMsgProto.ClubRuleRecordProto;
import protobuf.clazz.ClubMsgProto.ClubRuleRecordRequestProto;
import protobuf.clazz.ClubMsgProto.ClubRuleRecordResponse;
import protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * 
 *
 * @author zhanglong date: 2018年7月16日 上午10:09:27
 */
@ICmd(code = C2SCmd.CLUB_RULE_RECORD_REQ, desc = "亲友圈包间记录请求")
public class ClubRuleRecordReqHandler extends IClientExHandler<ClubRuleRecordRequestProto> {

	@Override
	protected void execute(ClubRuleRecordRequestProto req, TransmitProto topReq, C2SSession session) throws Exception {
		Club club = ClubService.getInstance().getClub(req.getClubId());
		if (club == null) {
			return;
		}
		int requestType = req.getRequestType();
		if (requestType <= 0) {
			return;
		}

		club.runInReqLoop(() -> {

			ClubRuleRecordResponse.Builder builder = ClubRuleRecordResponse.newBuilder();
			builder.setClubId(req.getClubId());
			builder.setRequestType(requestType);
			List<Integer> ruleIdList = req.getRuleIdList();
			if (ClubCfg.get().isUseNewClubRuleRecordGetWay()) {
				for (Integer ruleId : ruleIdList) {
					ClubRuleRecordProto.Builder recordBuilder = ClubRuleRecordProto.newBuilder();
					recordBuilder.setRuleId(ruleId);
					builder.addRuleRecord(recordBuilder);
					ClubRuleTable ruleTable = club.ruleTables.get(ruleId);
					if (ruleTable == null) {
						continue;
					}
					if (requestType == ClubRecordDayType.EIGHT) {
						int tmpGameCount = 0;
						int tmpExclusiveGold = 0;
						int tmpGold = 0;
						for (ClubRuleRecordModel model : ruleTable.getRuleRecordMap().values()) {
							if (model.getIsTotal() == 1) {
								continue;
							}
							tmpGameCount += model.getGameCount();
							tmpExclusiveGold += model.getExclusiveGold();
							tmpGold += model.getGold();
						}
						recordBuilder.setGameCount(tmpGameCount);
						recordBuilder.setExclusiveGold(tmpExclusiveGold);
						recordBuilder.setGold(tmpGold);
					} else {
						ClubRuleRecordModel record = ruleTable.getRuleRecordKeyByDay(requestType);
						if (record != null) {
							recordBuilder.setGameCount(record.getGameCount());
							recordBuilder.setExclusiveGold(record.getExclusiveGold());
							recordBuilder.setGold(record.getGold());
						}
					}
				}
				session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_RULE_RECORD_RSP, builder));
			} else { // 为兼容线上数据，先按查战绩方式跑八天，此时选全部查最近八天数据
				for (Integer ruleId : ruleIdList) {
					ClubRuleRecordProto.Builder recordBuilder = ClubRuleRecordProto.newBuilder();
					recordBuilder.setRuleId(ruleId);
					builder.addRuleRecord(recordBuilder);
				}
				builder.setAccountId(topReq.getAccountId());
				session.send(PBUtil.toS2SResponse(S2SCmd.CLUB_RULE_RECORD_INFO, builder));
			}
		});
	}
}
