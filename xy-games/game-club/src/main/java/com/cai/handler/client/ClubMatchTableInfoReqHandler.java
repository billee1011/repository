package com.cai.handler.client;

import java.util.List;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.constant.Symbol;
import com.cai.common.domain.ClubRuleModel;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RoomComonUtil;
import com.cai.common.util.StringUtil;
import com.cai.constant.Club;
import com.cai.constant.ClubMatchWrap;
import com.cai.service.ClubService;
import com.cai.utils.ClubMatchCostUtil;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import protobuf.clazz.ClubMsgProto.ClubMatchInfoProto;
import protobuf.clazz.ClubMsgProto.ClubMatchTableInfoResponse;
import protobuf.clazz.ClubMsgProto.ClubRuleProto;
import protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * 
 *
 * @author zhanglong date: 2018年6月26日 下午7:47:34
 */
@ICmd(code = C2SCmd.CLUB_MATCH_TABLE_INFO_REQ, desc = "亲友圈自建赛获取牌桌人数")
public class ClubMatchTableInfoReqHandler extends IClientExHandler<ClubMatchInfoProto> {

	@Override
	protected void execute(ClubMatchInfoProto req, TransmitProto topReq, C2SSession session) throws Exception {

		Club club = ClubService.getInstance().getClub(req.getClubId());
		if (null == club) {
			return;
		}
		club.runInReqLoop(() -> {
			ClubRuleModel clubRuleModel = new ClubRuleModel();
			ClubRuleProto ruleProto = req.getRule();
			clubRuleModel.setGame_type_index(ruleProto.getGameTypeIndex());
			clubRuleModel.setRules(ruleProto.getRules());
			clubRuleModel.setGame_round(ruleProto.getGameRound());
			clubRuleModel.init();
			int tablePlayerNum = RoomComonUtil.getMaxNumber(clubRuleModel.getRuleParams());
			ClubMatchTableInfoResponse.Builder builder = ClubMatchTableInfoResponse.newBuilder();
			builder.setTablePlayerNum(tablePlayerNum);
			int cost = ClubMatchCostUtil.finalCost(ruleProto.getGameTypeIndex(), ruleProto.getGameRound(), club.getMemberCount(),
					clubRuleModel.getRuleParams().getMap());
			builder.setTableCost(cost);

			if(req.hasId()) {
				ClubMatchWrap wrap = club.matchs.get(req.getId());
				if (wrap != null) {
					String costGold = wrap.getModel().getCostGold();
					List<Integer> list = StringUtil.toIntList(costGold, Symbol.COMMA);
					if (list != null && list.size() > 2) {
						builder.setOrignalTalbleCost(list.get(2));
					}
				}
			}

			session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_MATCH_TABLE_INFO_RSP, builder));
		});
	}

}
