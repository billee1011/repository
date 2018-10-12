/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.c2s;

import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.domain.HallMainViewBackModel;
import com.cai.common.util.PBUtil;
import com.cai.dictionary.HallGuideDict;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;

import protobuf.clazz.Protocol.Request;
import protobuf.clazz.basic.HallGuideProto.HallMainViewBackRequest;
import protobuf.clazz.basic.HallGuideProto.HallMainViewBackResponse;


@ICmd(code = C2SCmd.GET_HALL_MAIN_VIEW_BACK, desc = "大厅主界面背景")
public final class HallMainViewBackHandler extends IClientHandler<HallMainViewBackRequest> {

	private static final Logger logger = LoggerFactory.getLogger(HallMainViewBackHandler.class);
	
	@Override
	protected void execute(HallMainViewBackRequest req, Request topRequest, C2SSession session) throws Exception {
		int cityCode = req.getCityCode();
		session.send(PBUtil.toS2CCommonRsp(S2CCmd.HALL_MAIN_VIEW_BACKRSP, getHallMainViewBackData(cityCode)));
		
	}
	
	/**
	 * 获取后台配置的大厅主界面背景资源
	 */
	public HallMainViewBackResponse.Builder getHallMainViewBackData(int cityCode) {
		HallMainViewBackResponse.Builder mainViewBack = HallMainViewBackResponse.newBuilder();
		HallMainViewBackModel hallMainViewBackModel = HallGuideDict.getInstance().getHallMainViewBackModel(cityCode);
		
		if (hallMainViewBackModel == null || hallMainViewBackModel.getCity() == 0) {	//没有查到省份的数据就降cityCode返回
			logger.info("未查询到大厅主界面背景资源的配置");
			mainViewBack.setCityCode(cityCode);
			return mainViewBack;
		}
		if (hallMainViewBackModel.getEnd_time() != null) {		//过滤掉过期的
			if (hallMainViewBackModel.getEnd_time().getTime() < new Date().getTime()) {
				mainViewBack.setCityCode(cityCode);
				return mainViewBack;
			}
		}
		try {
			mainViewBack.setCityCode(hallMainViewBackModel.getCity());
			mainViewBack.setCityCode(hallMainViewBackModel.getCity());
			mainViewBack.setBackImage(StringUtils.isBlank(hallMainViewBackModel.getBack_image()) ? "" : hallMainViewBackModel.getBack_image());
			mainViewBack.setPersonImage(StringUtils.isBlank(hallMainViewBackModel.getPerson_image()) ? "" : hallMainViewBackModel.getPerson_image());
			if (hallMainViewBackModel.getStart_time() != null && hallMainViewBackModel.getEnd_time() != null) {
				mainViewBack.setStartTime(hallMainViewBackModel.getStart_time().getTime());
				mainViewBack.setEndTime(hallMainViewBackModel.getEnd_time().getTime());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return mainViewBack;
	} 
	
}
