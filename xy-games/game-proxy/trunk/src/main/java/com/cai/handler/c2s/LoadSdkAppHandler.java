/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.c2s;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.util.PBUtil;
import com.cai.dictionary.SdkAppDict;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;

import protobuf.clazz.Protocol.Request;
import protobuf.clazz.c2s.C2SProto.SdkAppRequest;
import protobuf.clazz.c2s.C2SProto.SdkAppResponse;

/**
 * 拉取SDK APP
 * @author chansonyan
 * 2018年9月5日
 */
@ICmd(code = C2SCmd.LOAD_SDK_APP_LIST, desc = "加载SDK第三方APP列表")
public final class LoadSdkAppHandler extends IClientHandler<SdkAppRequest> {


	@Override
	protected void execute(SdkAppRequest req, Request topRequest, C2SSession session) throws Exception {
		SdkAppResponse.Builder sdkBuilder = SdkAppDict.getInstance().getBuilder();
		session.send(PBUtil.toS2CCommonRsp(S2CCmd.LOAD_SDK_APP_RESPONSE, sdkBuilder));
	}

}
