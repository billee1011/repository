/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.c2s;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.define.EGameType;
import com.cai.common.domain.Account;
import com.cai.common.domain.SysParamModel;
import com.cai.common.util.PBUtil;
import com.cai.dictionary.SysParamServerDict;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;

import protobuf.clazz.Protocol.Request;
import protobuf.clazz.c2s.C2SProto.XianYiDouExchangeCoinRateReq;
import protobuf.clazz.c2s.C2SProto.XianYiDouExchangeCoinRateResp;

/**
 * 
 *
 * @author zhanglong date: 2018年4月4日 下午6:20:03 <br/>
 */
@ICmd(code = C2SCmd.XIAN_YI_DOU_EXCHANGE_COIN_RATE, desc = "闲逸豆兑换金币比例")
public class XianYiDouExchangeCoinRateHandler extends IClientHandler<XianYiDouExchangeCoinRateReq> {

	@Override
	protected void execute(XianYiDouExchangeCoinRateReq req, Request topRequest, C2SSession session) throws Exception {
		Account account = session.getAccount();
		if (account == null)
			return;
		SysParamModel sysParamModel = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(EGameType.DT.getId()).get(2301);
		XianYiDouExchangeCoinRateResp.Builder builder = XianYiDouExchangeCoinRateResp.newBuilder();
		builder.setRate(sysParamModel.getVal1());
		session.send(PBUtil.toS2CCommonRsp(S2CCmd.XIAN_YI_DOU_EXCHANGE_COIN_RATE, builder));
	}
}
