/**
 *
 */
package com.cai.util;

import java.util.Date;

import com.cai.common.define.EGoldOperateType;
import com.cai.common.domain.ClubExclusiveGoldLogModel;
import com.cai.common.domain.ClubExclusiveResultModel;
import com.cai.common.rmi.vo.ClubExclusiveRMIVo;
import com.cai.service.MongoDBServiceImpl;

/**
 * @author wu_hc date: 2017年12月18日 下午12:20:14 <br/>
 */
public final class ClubExclusiveLogUtil {

	/**
	 * @param vo
	 * @param model
	 * @param increment
	 */
	public static void exclusiveLog(ClubExclusiveRMIVo vo, ClubExclusiveResultModel model, boolean increment) {
		exclusiveLog(vo, model, increment, 0L);
	}

	@SuppressWarnings("unchecked")
	public static void exclusiveLog(ClubExclusiveRMIVo vo, ClubExclusiveResultModel model, boolean increment, long targetAccountId) {
		// 日志
		StringBuilder buf = new StringBuilder();
		buf.append("|");
		if (increment) {
			buf.append("增加[" + vo.getValue() + "]");
		} else {
			buf.append("减少[" + vo.getValue() + "]");
		}
		buf.append(",值变化:[").append(model.getOldValue()).append("]->[").append(model.getNewValue()).append("]");

		ClubExclusiveGoldLogModel logModel = new ClubExclusiveGoldLogModel();
		logModel.setAccount_id(vo.getAccountId());
		logModel.setAppId(vo.getGameId());
		logModel.setCreate_time(new Date());
		logModel.setMsg(vo.getDesc() + buf.toString());
		logModel.setV1(model.getOldValue()); // 旧值
		logModel.setV2(model.getNewValue()); // 新值
		logModel.setV3(vo.getValue()); // 冗余字段，方便查询
		logModel.setGameTypeIndex(vo.getGameTypeIndex());
		logModel.setClubId(vo.getClubId());
		logModel.setTargetAccountId(targetAccountId);

		if (null != vo.getType()) {
			logModel.setOperateType(vo.getType().getId());
		}
		MongoDBServiceImpl.getInstance().getLogQueue().add(logModel);
	}

	/**
	 * 克隆
	 *
	 * @param temp
	 * @return
	 */
	public static ClubExclusiveRMIVo clone(ClubExclusiveRMIVo temp, final EGoldOperateType type) {
		ClubExclusiveRMIVo vo = ClubExclusiveRMIVo
				.newVo(temp.getAccountId(), temp.getGameId(), temp.getValue(), null != type ? type : temp.getType());
		vo.setClubId(temp.getClubId());
		vo.setDesc(temp.getDesc());
		vo.setGameTypeIndex(temp.getGameTypeIndex());
		vo.setExclusiveBeginDate(temp.getExclusiveBeginDate());
		vo.setExclusiveEndDate(temp.getExclusiveEndDate());
		vo.setSettings(temp.getSettings());
		return vo;

	}
}
