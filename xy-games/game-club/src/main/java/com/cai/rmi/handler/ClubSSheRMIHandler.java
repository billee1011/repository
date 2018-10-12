package com.cai.rmi.handler;

import java.util.List;

import com.cai.common.constant.RMICmd;
import com.cai.common.define.EClubSettingStatus;
import com.cai.common.define.IClubSsheOperateType;
import com.cai.common.define.ITypeStatus;
import com.cai.common.domain.StatusModule;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.common.rmi.vo.ClubRMIVo;
import com.cai.common.type.ClubExitType;
import com.cai.constant.Club;
import com.cai.service.ClubService;
import com.cai.utils.Utils;
import com.google.common.base.Strings;

import protobuf.clazz.ClubMsgProto.ClubNoticeProto;
import protobuf.clazz.ClubMsgProto.ClubRuleProto;
import protobuf.clazz.ClubMsgProto.ClubUpdateProto;
import protobuf.clazz.Common.CommonII;

/**
 * 
 *
 * @author wu_hc date: 2017年8月02日 上午16:11:00 <br/>
 */
@IRmi(cmd = RMICmd.CLUB_SET, desc = "来自ssh后台的一些俱乐部设置")
public final class ClubSSheRMIHandler extends IRMIHandler<ClubRMIVo, Integer> {
	@Override
	protected Integer execute(ClubRMIVo vo) {

		logger.warn("俱乐部服接受到来自后台操作!{}", vo);

		if (vo.getType() == IClubSsheOperateType.SET_CHAT || vo.getType() == IClubSsheOperateType.SET_BULLETIN
				|| vo.getType() == IClubSsheOperateType.SET_MARQUEE) {
			return Club.SUCCESS;
		} else {
			int clubId = vo.getClubId();
			if (clubId <= 0) {
				logger.error("后台操作，俱乐部id[{}] 不合理！", clubId);
				return Club.FAIL;
			}
			Club club = ClubService.getInstance().getClub(clubId);
			if (null == club) {
				logger.error("后台操作，不存在俱乐部[{}]！", clubId);
				return Club.FAIL;
			}

			long clubOwnerId = club.getOwnerId();
			if (vo.getType() == IClubSsheOperateType.CLUB_SETS) { // 俱乐部设置，冻结俱乐部
				ClubUpdateProto.Builder builder = ClubUpdateProto.newBuilder();
				builder.setClubId(clubId);
				builder.setType(6);
				StatusModule statusModel = StatusModule.newWithStatus(vo.getClubSettings());
				int value = -1;
				ITypeStatus iTypeStatus = EClubSettingStatus.NONE;
				if (vo.getType() == IClubSsheOperateType.CLUB_SETS) {
					iTypeStatus = EClubSettingStatus.CLUB_FREEZE;
				}
				value = statusModel.isStatusTrue(iTypeStatus) ? 1 : 0;
				if (EClubSettingStatus.NONE == iTypeStatus || value == -1) {
					return Club.FAIL;
				}

				builder.addSetStatus(CommonII.newBuilder().setK(iTypeStatus.position()).setV(value));
				return ClubService.getInstance().updateClub(clubOwnerId, builder.build()).getStatus();

			} else if (vo.getType() == IClubSsheOperateType.CLUB_DEL) { // 解散/删除俱乐部##########很严重的，操作一定要特别小心################

				return ClubService.getInstance().deleteClub(clubId, clubOwnerId, null, true).getStatus();

			} else if (vo.getType() == IClubSsheOperateType.UPDATE_CLUB_NAME) { // 修改名称
				final String newClubName = vo.getNewClubName();
				if (Strings.isNullOrEmpty(newClubName)) {
					logger.error("俱乐部名字不合理!");
					return Club.FAIL;
				}
				return ClubService.getInstance().updateClubName(club, newClubName, Boolean.TRUE).getStatus();

			} else if (vo.getType() == IClubSsheOperateType.UPDATE_CLUB_NOTICE) { // 修改公告。
				ClubNoticeProto.Builder builder = ClubNoticeProto.newBuilder().setText(vo.getNewNotice());
				return ClubService.getInstance().clubNoticeSets(clubId, clubOwnerId, builder.build()).getStatus();

			} else if (vo.getType() == IClubSsheOperateType.DISBAND_TABLE) { // 解散桌子，桌子未开始才可以
				int joinId = (vo.getTableIndex() << 16) & 0xffff0000;
				return ClubService.getInstance().disbandTable(clubId, vo.getRuleId(), joinId, clubOwnerId).getStatus();

			} else if (vo.getType() == IClubSsheOperateType.KICK) { // 踢人

				List<Long> accountIds = vo.getMemberId();
				accountIds.forEach((accountId) -> {
					ClubService.getInstance().kickClub(clubId, clubOwnerId, accountId, ClubExitType.KICK);
				});
				return Club.SUCCESS;
			} else if (vo.getType() == IClubSsheOperateType.DEL_RULE) { // 删除包间

				ClubUpdateProto.Builder builder = ClubUpdateProto.newBuilder().setClubId(clubId)
						.addClubRule(ClubRuleProto.newBuilder().setId(vo.getRuleId()));
				ClubService.getInstance().updateClub(clubOwnerId, builder.build());
				return Club.SUCCESS;
			} else if (vo.getType() == IClubSsheOperateType.AGREE_ALL) {

				ClubService.getInstance().agreeJoinClubBatch(clubOwnerId, clubId);
				return Club.SUCCESS;
			} else if (vo.getType() == IClubSsheOperateType.COMMON_CLUB_SET) { // 全部设置，

				StatusModule status = StatusModule.newWithStatus(vo.getClubSettings());
				ClubUpdateProto.Builder builder = ClubUpdateProto.newBuilder();
				builder.setClubId(clubId);
				builder.setType(6);
				builder.addAllSetStatus(Utils.toRuleStatusBuilder(status));
				return ClubService.getInstance().updateClub(clubOwnerId, builder.build()).getStatus();
			}
		}

		return Club.FAIL;
	}
}
