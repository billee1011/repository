package com.cai.handler.client;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.constant.S2SCmd;
import com.cai.common.domain.ClubMemberModel;
import com.cai.common.domain.ClubMemberRecordModel;
import com.cai.common.type.ClubRecordDayType;
import com.cai.common.util.PBUtil;
import com.cai.config.ClubCfg;
import com.cai.constant.Club;
import com.cai.service.ClubService;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import protobuf.clazz.ClubMsgProto.ClubMemberRecord;
import protobuf.clazz.ClubMsgProto.ClubMemberRecordRequestProto;
import protobuf.clazz.ClubMsgProto.ClubMemberRecordResponse;
import protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * 
 *
 * @author zhanglong date: 2018年4月26日 下午6:33:53
 */
@ICmd(code = C2SCmd.CLUB_MEMBER_RECORD_LIST_REQUEST, desc = "俱乐部玩家记录请求")
public class ClubRequestRecordHandler extends IClientExHandler<ClubMemberRecordRequestProto> {

	@Override
	protected void execute(ClubMemberRecordRequestProto req, TransmitProto topReq, C2SSession session) throws Exception {
		Club club = ClubService.getInstance().getClub(req.getClubId());
		if (null == club) {
			return;
		}
		int requestType = req.getRequestType();
		int day = -1;
		if (requestType == ClubRecordDayType.TODAY) {
			day = 1;
		} else if (requestType == ClubRecordDayType.YESTERDAY) {
			day = 2;
		} else if (requestType == ClubRecordDayType.BEFORE_YESTERDAY) {
			day = 3;
		} else if (requestType == ClubRecordDayType.EIGHT) {
			day = 8;
		} else if (requestType == ClubRecordDayType.ALL) {
			day = 0;
		}
		if (day == -1) {
			return;
		}
		final int recordDay = day;
		club.runInReqLoop(() -> {
			final ClubMemberModel operator = club.members.get(topReq.getAccountId());
			if (null == operator) {
				return;
			}
			ClubMemberRecordResponse.Builder builder = ClubMemberRecordResponse.newBuilder();
			builder.setClubId(club.getClubId());
			if (req.getAccountId() == 0) {
				club.members.forEach((accountId, memberModel) -> {
					encodeMemberRecord(memberModel, builder, recordDay);
				});
			} else {
				ClubMemberModel member = club.members.get(req.getAccountId());
				encodeMemberRecord(member, builder, recordDay);
			}
			if (ClubCfg.get().isUseNewGetClubMemRecordWay()) {
				session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_MEMBER_RECORD_LIST_RSP, builder));
			} else {
				// 先切换至旧的局数和大赢家数统计模式(解决今日、昨天、前日、八日没有数据的问题)
				if (requestType == ClubRecordDayType.TODAY || requestType == ClubRecordDayType.YESTERDAY
						|| requestType == ClubRecordDayType.BEFORE_YESTERDAY || requestType == ClubRecordDayType.EIGHT) {
					builder.setRequestType(requestType);
					builder.setAccountId(topReq.getAccountId());
					builder.setTargetAccountId(req.getAccountId());
					session.send(PBUtil.toS2SResponse(S2SCmd.CLUB_MEMBER_RECORD_INFO, builder));
				} else if (requestType == ClubRecordDayType.ALL) {
					session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_MEMBER_RECORD_LIST_RSP, builder));
				}
			}
		});
	}

	void encodeMemberRecord(ClubMemberModel member, ClubMemberRecordResponse.Builder builder, int recordDay) {
		if (member != null) {
			if (recordDay != 8) {
				ClubMemberRecord.Builder recordBuilder = ClubMemberRecord.newBuilder();
				recordBuilder.setAccountId(member.getAccount_id());
				ClubMemberRecordModel recordModel = member.getMemberRecordMap().get(recordDay);
				if (recordModel != null) {
					recordBuilder.setAccountId(recordModel.getAccountId());
					recordBuilder.setGameTime(recordModel.getGameCount());
					recordBuilder.setWinTime(recordModel.getBigWinCount());
					recordBuilder.setIsLike(recordModel.getIsLike());
					if (recordDay == 0) {
						recordBuilder.setTireValue(recordModel.getTireValue());
					} else {
						int tireValue = recordModel.getAccuTireValue();
						Club club = ClubService.getInstance().getClub(member.getClub_id());
						if (club != null) {
							tireValue = club.getMemberRealUseTire(recordModel);
						}
						recordBuilder.setTireValue(tireValue);
					}
				} else if (recordDay == 0) { // 为了兼容线上旧数据,在新数据未生成前使用旧数据
					recordBuilder.setGameTime(member.getGame_count());
					recordBuilder.setWinTime(member.getWinCount());
				}
				builder.addRecord(recordBuilder);
			} else {
				long temp_accountId = 0;
				int temp_gameTime = 0;
				int temp_winTime = 0;
				int temp_tireValue = 0;
				for (int i = 1; i <= 8; i++) {
					ClubMemberRecordModel recordModel = member.getMemberRecordMap().get(i);
					if (recordModel != null) {
						temp_accountId = recordModel.getAccountId();
						temp_gameTime += recordModel.getGameCount();
						temp_winTime += recordModel.getBigWinCount();
						temp_tireValue += recordModel.getTireValue();
					}
				}
				ClubMemberRecord.Builder recordBuilder = ClubMemberRecord.newBuilder();
				recordBuilder.setAccountId(member.getAccount_id());
				if (temp_accountId != 0) {
					recordBuilder.setAccountId(temp_accountId);
					recordBuilder.setGameTime(temp_gameTime);
					recordBuilder.setWinTime(temp_winTime);
					recordBuilder.setIsLike(0);
					recordBuilder.setTireValue(temp_tireValue);
				}
				builder.addRecord(recordBuilder);
			}
		}
	}

}
