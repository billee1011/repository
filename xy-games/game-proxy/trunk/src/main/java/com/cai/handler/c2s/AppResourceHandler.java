/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.c2s;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.domain.GameResourceModel;
import com.cai.common.util.PBUtil;
import com.cai.dictionary.HallGuideDict;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;

import protobuf.clazz.Protocol.Request;
import protobuf.clazz.basic.HallGuideProto.GameResourceRequest;
import protobuf.clazz.basic.HallGuideProto.GameResourceResponse;

@ICmd(code = C2SCmd.GET_APP_RESOURCE, desc = "获取游戏印章、背景、标题资源")
public final class AppResourceHandler extends IClientHandler<GameResourceRequest> {

	private static final Logger logger = LoggerFactory.getLogger(AppResourceHandler.class);

	@Override
	protected void execute(GameResourceRequest req, Request topRequest, C2SSession session) throws Exception {
		int appId = req.getAppId();
		GameResourceResponse.Builder builder = GameResourceResponse.newBuilder();
		builder.setAppId(appId);
		GameResourceModel model = HallGuideDict.getInstance().getResourceByAppId(appId);
		if (model != null) {
			builder.setBgUrl(model.getBackground());
			builder.setSealUrl(model.getSeal());
			builder.setTitleUrl(model.getTitle());
		}
		session.send(PBUtil.toS2CCommonRsp(S2CCmd.GET_APP_RESOURCE, builder));
	}

}
