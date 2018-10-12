/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.client;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.domain.ClubMemberModel;
import com.cai.common.util.PBUtil;
import com.cai.constant.Club;
import com.cai.constant.ClubPlayer;
import com.cai.constant.ClubRuleTable;
import com.cai.constant.ClubSeat;
import com.cai.service.ClubCacheService;
import com.cai.service.ClubService;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import protobuf.clazz.ClubMsgProto.ClubAccountProto;
import protobuf.clazz.ClubMsgProto.ClubRuleOLMemberListProto;
import protobuf.clazz.ClubMsgProto.ClubRuleOLMemberProto;
import protobuf.clazz.ClubMsgProto.ClubRuleOLMemberReqProto;
import protobuf.clazz.ClubMsgProto.ClubRuleTableGroupProto;
import protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * 
 * 
 *
 * @author wu_hc date: 2018年4月16日 下午5:33:48 <br/>
 */
@ICmd(code = C2SCmd.CLUB_RULE_OL_MEMBERS, desc = "玩法对应的在线人数")
public final class ClubRuleOLMemberHandler extends IClientExHandler<ClubRuleOLMemberReqProto> {

	@Override
	protected void execute(ClubRuleOLMemberReqProto req, TransmitProto topReq, C2SSession session) throws Exception {
		if (!req.hasClubId()) {
			return;
		}

		Club club = ClubService.getInstance().getClub(req.getClubId());
		if (null == club) {
			return;
		}

		club.runInReqLoop(() -> {

			ClubRuleOLMemberListProto.Builder builder = ClubRuleOLMemberListProto.newBuilder();
			builder.setClubId(club.getClubId());
			Collection<Integer> reqRuleIds = req.getRuleIdCount() == 0 ? club.ruleTables.keySet() : req.getRuleIdList();

			// 1桌子数据
			Map<Integer, Set<Long>> ontablesMaps = Maps.newHashMap();
			List<ClubRuleTableGroupProto> ruleTablePBs = Lists.newArrayList();
			reqRuleIds.forEach(ruleId -> {
				ClubRuleTable ruleTable = club.ruleTables.get(ruleId);
				if (null != ruleTable) {
					ruleTablePBs.add(ruleTable.toTablesBuilder(req.getClubId(), ClubPlayer.OP_USERNAME, req.getHideStartTable()).build());

					ontablesMaps.put(ruleId, ruleTable.allInTablePlayerIds());
				}
			});
			builder.addAllRuleTables(ruleTablePBs);

			// 2不在桌子内的在线玩家
			// ruleId,players
			Map<Integer, Set<Long>> ruleOLMembers = Maps.newHashMap();

			club.members.forEach((id, model) -> {
				Optional<ClubSeat> opt = ClubCacheService.getInstance().seat(id);
				if (opt.isPresent() && opt.get().getClubId() == req.getClubId() && opt.get().getRuleId() != ClubSeat.INVAL_ID) {
					Set<Long> players = ruleOLMembers.get(opt.get().getRuleId());
					if (null == players) {
						players = Sets.newHashSet();
						ruleOLMembers.put(opt.get().getRuleId(), players);

					}
					players.add(id);
				}
			});

			ruleOLMembers.forEach((ruleId, sets) -> {
				Set<Long> ruleOnTablePlayerIds = ontablesMaps.get(ruleId);
				if (null != ruleOnTablePlayerIds && ruleOnTablePlayerIds.size() > 0) {
					for (Long accountId : ruleOnTablePlayerIds) {
						sets.remove(accountId);
					}
				}
				sets.remove(topReq.getAccountId());
				if (sets.size() > 0) {
					ClubRuleOLMemberProto.Builder mb = ClubRuleOLMemberProto.newBuilder().setRuleId(ruleId);
					sets.forEach(account_id -> {
						ClubMemberModel model = club.members.get(account_id);
						if (null == model) {
							return;
						}
						mb.addMembers(ClubAccountProto.newBuilder().setAccountId(model.getAccount_id()).setNickname(model.getNickname()));
					});
					builder.addRuleOLMembers(mb);
				}
			});

			session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_RULE_OL_MEMBERS, builder));
		});
	}
}
