/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.c2s;

import java.util.List;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.domain.AppItem;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.FilterUtil;
import com.cai.common.util.PBUtil;
import com.cai.common.util.SpringService;
import com.cai.dictionary.AppItemDict;
import com.cai.module.LoginModule;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;

import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.UpdateSubAppItemResponse;
import protobuf.clazz.c2s.C2SProto.AppItemsReq;

/**
 * @author wu_hc date: 2017年7月31日 上午11:11:00 <br/>
 */
@ICmd(code = C2SCmd.APPITEM, desc = "app推送")
public final class AppItemHandler extends IClientHandler<AppItemsReq> {

	@Override
	protected void execute(AppItemsReq req, Request topRequest, C2SSession session) throws Exception {

		session.send(PBUtil.toS2CCommonRsp(S2CCmd.APPITEM, processAppReq(req)));
	}

	/**
	 * @param req
	 */
	private UpdateSubAppItemResponse.Builder processAppReq(final AppItemsReq req) {
		List<AppItem> appItems = AppItemDict.getInstance().getAppItemList();
		if (appItems == null) {
			SpringService.getBean(ICenterRMIServer.class).reLoadAppItemDictionary();
			AppItemDict.getInstance().load();
			appItems = AppItemDict.getInstance().getAppItemList();
		}
		if (null == appItems) {
			logger.error("玩家请求子游戏，但子游戏列表为空!!");
			return UpdateSubAppItemResponse.newBuilder();
		}

		appItems = FilterUtil.filter(appItems, (item) -> item.getT_status() == 1);
		return LoginModule.newAppItemsBuilder(appItems);
	}
}
