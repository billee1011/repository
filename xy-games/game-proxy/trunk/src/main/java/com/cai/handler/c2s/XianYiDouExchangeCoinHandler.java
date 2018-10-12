/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.c2s;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.define.EGameType;
import com.cai.common.define.EGoldOperateType;
import com.cai.common.define.EMoneyOperateType;
import com.cai.common.domain.Account;
import com.cai.common.domain.AddGoldResultModel;
import com.cai.common.domain.SysParamModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.type.UIType;
import com.cai.common.util.PBUtil;
import com.cai.common.util.SpringService;
import com.cai.core.SystemConfig;
import com.cai.dictionary.SysParamServerDict;
import com.cai.service.ClientServiceImpl;
import com.cai.service.PlayerServiceImpl;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;

import protobuf.clazz.Protocol.Request;
import protobuf.clazz.c2s.C2SProto.XianYiDouExchangeCoinReq;
import protobuf.clazz.c2s.C2SProto.XianYiDouExchangeCoinResp;
import protobuf.clazz.coin.CoinServerProtocol.S2SCoinStoreStat;

/**
 * 
 *
 * @author zhanglong date: 2018年4月4日 下午6:20:03 <br/>
 */
@ICmd(code = C2SCmd.XIAN_YI_DOU_EXCHANGE_COIN, desc = "闲逸豆兑换金币")
public class XianYiDouExchangeCoinHandler extends IClientHandler<XianYiDouExchangeCoinReq> {

	@Override
	protected void execute(XianYiDouExchangeCoinReq req, Request topRequest, C2SSession session) throws Exception {
		Account account = session.getAccount();
		if (account == null) {
			sendBackMsgToClient(session, false);
			return;
		}

		int costNum = req.getNum();
		if (costNum <= 0) {
			sendBackMsgToClient(session, false);
			return;
		}

		SysParamModel sysParamModel = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(EGameType.DT.getId()).get(2301);
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		AddGoldResultModel result = centerRMIServer.addAccountGold(account.getAccount_id(), -costNum, false, "闲逸豆兑换金币",
				EGoldOperateType.XIAN_YI_DOU_EXCHANGE_COIN);
		if (result.isSuccess()) {
			long tempNum = costNum * sysParamModel.getVal1();
			int addNum = tempNum > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) tempNum;
			centerRMIServer.addAccountMoney(account.getAccount_id(), addNum, false, "闲逸豆兑换金币", EMoneyOperateType.XIAN_YI_DOU_EXCHANGE_COIN);

			sendBackMsgToClient(session, result.isSuccess());
			// 如果在房间里面兑换金币 需要通知 逻辑服更新个人信息
			PlayerServiceImpl.getInstance().notifyLogicToUpdateAccountInfo(session, 5, -costNum, addNum);
			sendStoreStat(account.getAccount_id(), costNum, req);
		} else {
			sendBackMsgToClient(session, result.isSuccess());
		}
	}

	private void sendBackMsgToClient(C2SSession session, boolean result) {
		XianYiDouExchangeCoinResp.Builder builder = XianYiDouExchangeCoinResp.newBuilder();
		builder.setIsSuccess(result);
		session.send(PBUtil.toS2CCommonRsp(S2CCmd.XIAN_YI_DOU_EXCHANGE_COIN, builder));
	}

	private void sendStoreStat(long accountId, int costGold, XianYiDouExchangeCoinReq req) {
		S2SCoinStoreStat.Builder request = S2SCoinStoreStat.newBuilder();

		request.setAccountId(accountId);
		request.setOpType(UIType.OP_EXCHANGE);
		request.setGold(costGold);
		request.setUiType(req.getUiType());
		request.setSubUiType(req.getSubUiType());
		request.setOpId(req.getOpId());

		ClientServiceImpl.getInstance().sendToCoin(SystemConfig.connectCoin, PBUtil.toS2SRequet(C2SCmd.COIN_PAY_MSG_STAT, request).build());
	}
}
