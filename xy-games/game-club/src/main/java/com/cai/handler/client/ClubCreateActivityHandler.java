/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.client;

import java.util.Date;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.define.EClubActivityCategory;
import com.cai.common.define.ESysMsgType;
import com.cai.common.domain.ClubActivityModel;
import com.cai.common.domain.ClubMemberModel;
import com.cai.common.util.Bits;
import com.cai.common.util.EmojiFilter;
import com.cai.common.util.PBUtil;
import com.cai.common.util.SpringService;
import com.cai.common.util.TimeUtil;
import com.cai.config.ClubCfg;
import com.cai.constant.Club;
import com.cai.constant.ClubActivityCode;
import com.cai.constant.ClubActivityWrap;
import com.cai.constant.EClubIdentity;
import com.cai.service.ClubDaoService;
import com.cai.service.ClubService;
import com.cai.utils.Utils;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import protobuf.clazz.ClubMsgProto.ClubActivityProto;
import protobuf.clazz.ClubMsgProto.ClubCreateActProto;
import protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * 
 *
 * @author wu_hc date: 2018年1月23日 上午9:58:19 <br/>
 */
@ICmd(code = C2SCmd.CLUB_CREATE_ACTIVITY, desc = "创建俱乐部活动")
public final class ClubCreateActivityHandler extends IClientExHandler<ClubCreateActProto> {

	@Override
	protected void execute(ClubCreateActProto req, TransmitProto topReq, C2SSession session) throws Exception {

		if (!ClubCfg.get().isActivityOpen()) {
			Utils.sendTip(topReq.getAccountId(), "您好，亲友圈活动临时关闭，请联系客服!", ESysMsgType.NONE, session);
			return;
		}

		ClubActivityProto actProto = req.getAct();

		String activityName = actProto.getActivityName();
		if (StringUtils.isEmpty(activityName) || EmojiFilter.containsEmoji(activityName)) {
			Utils.sendTip(topReq.getAccountId(), "活动名称包含特殊字符!", ESysMsgType.NONE, session);
			return;
		}
		EClubActivityCategory category = EClubActivityCategory.of(actProto.getActivityType());
		if (EClubActivityCategory.NONE == category) {
			return;
		}

		long start = actProto.getStartDate() * 1000L, end = actProto.getEndDate() * 1000L;
		long current = System.currentTimeMillis();

		if (start <= current || end <= current) {
			return;
		}

		// 活动最短最长时间判断
		if (((end - start) > ClubCfg.get().getActivityMaxTime() * TimeUtil.HOUR)
				|| (end - start) < ClubCfg.get().getActivityMinTime() * TimeUtil.HOUR) {
			return;
		}

		Club club = ClubService.getInstance().getClub(actProto.getClubId());
		if (null == club) {
			return;
		}

		club.runInReqLoop(() -> {
			ClubMemberModel member = club.members.get(topReq.getAccountId());
			if (null == member || !EClubIdentity.isManager(member.getIdentity())) {
				return;
			}
			// 有活动
			if (actingCount(club.activitys) >= ClubCfg.get().getActivityLimit()) {
				Utils.sendTip(topReq.getAccountId(), "亲友圈活动数量已达上限!", ESysMsgType.NONE, session);
				return;
			}
			// if (!club.activitys.isEmpty() && intersectionAct(club.activitys,
			// start, end).isPresent()) {
			// Utils.sendTip(topReq.getAccountId(), "活动时间与现有活动冲突，请重新输入!",
			// ESysMsgType.INCLUDE_ERROR, session);
			// return;
			// }

			ClubActivityModel model = new ClubActivityModel();
			model.setActivityEndDate(new Date(end));
			model.setActivityName(activityName);
			model.setActivityStartDate(new Date(start));
			model.setCreatorId(member.getAccount_id());
			model.setClubId(club.getClubId());
			model.setStatus(Bits.byte_1);
			model.setActivityType(actProto.getActivityType());
			SpringService.getBean(ClubDaoService.class).getDao().updateOrInsertClubActivity(model);
			ClubActivityWrap wrap = new ClubActivityWrap(model);
			club.activitys.putIfAbsent(model.getId(), wrap);

			session.send(PBUtil.toS_S2CRequet(topReq.getAccountId(), S2CCmd.CLUB_CREATE_ACTIVITY, wrap.toActivityBuilder()));

			Utils.notifyActivityEvent(topReq.getAccountId(), club, wrap.getId(), ClubActivityCode.CREATE);

		});
	}

	/**
	 * 有时间重叠的活动
	 * 
	 * @param start
	 * @param end
	 * @return
	 */
	@SuppressWarnings("unused")
	private final Optional<ClubActivityWrap> intersectionAct(Map<Long, ClubActivityWrap> activitys, long start, long end) {

		for (Map.Entry<Long, ClubActivityWrap> entry : activitys.entrySet()) {
			final ClubActivityWrap wrap = entry.getValue();
			if ((start >= wrap.startMillis() && start <= wrap.endMillis()) || (end >= wrap.startMillis() && end <= wrap.endMillis())) {
				return Optional.of(wrap);
			}
		}
		return Optional.empty();
	}

	/**
	 * 活动中的活动数量
	 * 
	 * @param activitys
	 * @return
	 */
	private static final int actingCount(Map<Long, ClubActivityWrap> activitys) {

		int counter = 0;
		for (Map.Entry<Long, ClubActivityWrap> entry : activitys.entrySet()) {
			if (entry.getValue().inActing()) {
				counter++;
			}
		}
		return counter;
	}
}
