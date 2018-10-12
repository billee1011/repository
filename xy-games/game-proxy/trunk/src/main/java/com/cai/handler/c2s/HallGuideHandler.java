/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.c2s;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.domain.HallGuideModel;
import com.cai.common.util.PBUtil;
import com.cai.dictionary.HallGuideDict;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;

import protobuf.clazz.Protocol.Request;
import protobuf.clazz.basic.HallGuideProto.HallGuideRequest;
import protobuf.clazz.basic.HallGuideProto.HallGuideResponse;
import protobuf.clazz.basic.HallGuideProto.SingleGuideData;


@ICmd(code = C2SCmd.GET_HALL_GUIDE, desc = "大厅指引")
public final class HallGuideHandler extends IClientHandler<HallGuideRequest> {

	private static final Logger logger = LoggerFactory.getLogger(HallGuideHandler.class);
	
	@Override
	protected void execute(HallGuideRequest req, Request topRequest, C2SSession session) throws Exception {
		session.send(PBUtil.toS2CCommonRsp(S2CCmd.HALL_GUIDE_RSP, getHallGuideData()));
		
	}
	
	/**
	 * 获取后台配置的大厅指引
	 */
	public HallGuideResponse.Builder getHallGuideData() {
		HallGuideResponse.Builder hallGuide = HallGuideResponse.newBuilder();
		List<HallGuideModel> hallGuideList = HallGuideDict.getInstance().getHallGuideDictionary();
		if (hallGuideList == null) {
			logger.info("未查询到大厅指引的配置");
			return hallGuide;
		}
		for (HallGuideModel model : hallGuideList) {
			if (model.getEnd_time().getTime() < new Date().getTime()) {
				continue;
			}
			SingleGuideData.Builder single = SingleGuideData.newBuilder();
			single.setGuideType(model.getGuide_type());
			single.setGuideText(StringUtils.isBlank(model.getGuide_text()) ? "" : model.getGuide_text());
			single.setPropagateIcon(StringUtils.isBlank(model.getPropagate_icon()) ? "" : model.getPropagate_icon());
			single.setStartTime(model.getStart_time().getTime());
			single.setEndTime(model.getEnd_time().getTime());
			hallGuide.addSingleGuide(single);
		}
		return hallGuide;
	}
	
}
