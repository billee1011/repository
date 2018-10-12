package com.cai.rmi.handler;

import java.util.List;
import java.util.Optional;

import com.cai.common.config.ExclusiveGoldCfg;
import com.cai.common.constant.RMICmd;
import com.cai.common.define.EGoldOperateType;
import com.cai.common.define.XYCode;
import com.cai.common.domain.AccountSimple;
import com.cai.common.domain.ClubExclusiveGoldModel;
import com.cai.common.domain.ClubExclusiveResultModel;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.common.rmi.vo.ClubExclusiveRMIVo;
import com.cai.common.rmi.vo.ClubExlusiveTransferVo;
import com.cai.service.ClubExclusiveService;
import com.cai.service.PublicServiceImpl;
import com.cai.util.ClubExclusiveLogUtil;
import com.cai.util.RMIMsgSender;
import com.google.common.collect.Lists;

/**
 * @author wu_hc date: 2018年2月5日 下午5:17:59 <br/>
 */
@IRmi(cmd = RMICmd.CLUB_EXCLUSIVE_TRANSFER, desc = "俱乐部专属豆转让")
public final class ClubExclusiveTransferRMIHandler extends IRMIHandler<ClubExlusiveTransferVo, ClubExlusiveTransferVo> {

	@Override
	public ClubExlusiveTransferVo execute(ClubExlusiveTransferVo vo) {

		logger.warn("接收到来自RMI的专属豆转让请求:{}", vo);

		if (!ExclusiveGoldCfg.get().isTransferActive()) {
			return vo.setStatus(XYCode.FAIL).setDesc("专属豆转让功能暂不开放!");
		}

		final long value = vo.getValue();
		AccountSimple targetLock = PublicServiceImpl.getInstance().getAccountSimpe(vo.getSelfAccountId());
		if (null == targetLock) {
			return vo.setStatus(XYCode.FAIL).setDesc("目标玩家不存在，请确认玩家ID是否正确!");
		}

		// self
		Optional<ClubExclusiveGoldModel> selfOpt = ClubExclusiveService.getInstance().get(vo.getSelfAccountId(), vo.getGameId());
		AccountSimple selfLock = PublicServiceImpl.getInstance().getAccountSimpe(vo.getSelfAccountId());
		if (null == selfLock) {
			return null;
		}

		final List<ClubExclusiveResultModel> R = Lists.newArrayList();
		synchronized (selfLock) {
			if (!selfOpt.isPresent()) {
				return vo.setStatus(XYCode.FAIL).setDesc("你没有该游戏的个人专属豆，不能进行转豆操作！");
			}
			if (vo.getValue() + selfOpt.get().getUsedCount() > selfOpt.get().getExclusiveGold()) {
				return vo.setStatus(XYCode.FAIL).setDesc("个人专属豆不足！");
			}
			ClubExclusiveRMIVo selfVo = ClubExclusiveRMIVo.newVo(vo.getSelfAccountId(), vo.getGameId(), value, EGoldOperateType.EXCLUSIVE_TRANSFER);

			ClubExclusiveResultModel selfR = ClubExclusiveService.getInstance().cost(selfVo);
			if (XYCode.FAIL == selfR.getStatus()) {
				return vo.setStatus(XYCode.FAIL).setDesc("专属豆转让失败，请求联系客服!");
			}
			// 反馈给调用者
			vo.setValue(selfR.getNewValue());

			// log
			ClubExclusiveLogUtil.exclusiveLog(selfVo, selfR, false, vo.getTagetAccountId());

			R.add(selfR);
		}

		// other
		synchronized (targetLock) {
			Optional<ClubExclusiveGoldModel> targetOpt = ClubExclusiveService.getInstance().get(vo.getTagetAccountId(), vo.getGameId());
			ClubExclusiveRMIVo operatorVo = ClubExclusiveRMIVo.newVo(vo.getTagetAccountId(), vo.getGameId(), value);

			//1不存在 2过期了  3使用完了
			if (!targetOpt.isPresent() || targetOpt.get().getExclusiveEndDate().getTime() < System.currentTimeMillis() || (
					targetOpt.get().getExclusiveGold() <= targetOpt.get().getUsedCount())) {
				operatorVo.setExclusiveBeginDate(selfOpt.get().getExclusiveBeginDate());
				operatorVo.setExclusiveEndDate(selfOpt.get().getExclusiveEndDate());
			} else {
				operatorVo.setExclusiveBeginDate(targetOpt.get().getExclusiveBeginDate());
				operatorVo.setExclusiveEndDate(targetOpt.get().getExclusiveEndDate());
			}
			ClubExclusiveResultModel targetR = ClubExclusiveService.getInstance().update(operatorVo);
			if (XYCode.FAIL == targetR.getStatus()) {
				logger.error("专属豆转让失败:{}", targetR);
				return vo.setStatus(XYCode.FAIL).setDesc("专属豆转让失败，请求联系客服!!");
			}
			//记录专属豆赠送记录
			ClubExclusiveRMIVo beSendVo = ClubExclusiveRMIVo.newVo(vo.getTagetAccountId(), vo.getGameId(), value, EGoldOperateType.EXCLUSIVE_TRANSFER);
			ClubExclusiveLogUtil.exclusiveLog(beSendVo, targetR, true, 0);
			R.add(targetR);
		}

		RMIMsgSender.callClub(RMICmd.CLUB_EXCLUSIVE_GOLD_SSHE_BATCH, R);

		return vo.setStatus(XYCode.SUCCESS).setDesc("转让成功!!");
	}
}
