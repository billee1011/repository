/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.s2s;

import com.cai.common.constant.S2SCmd;
import com.cai.common.define.EPropertyType;
import com.cai.common.define.EWealthCategory;
import com.cai.common.domain.Account;
import com.cai.common.handler.IServerHandler;
import com.cai.service.C2SSessionService;
import com.cai.util.MessageResponse;
import com.xianyi.framework.core.transport.IServerCmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.core.transport.netty.session.S2SSession;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.AccountPropertyListResponse;
import protobuf.clazz.Protocol.AccountPropertyResponse;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;
import protobuf.clazz.s2s.S2SProto.RoomWealthProto;

/**
 * 
 * 
 *
 * @author wu_hc date: 2017年11月10日 上午11:02:06 <br/>
 */
@IServerCmd(code = S2SCmd.WEALTH_UPDATE, desc = "财富更新，只是更新缓存")
public class AccountWealthUpdateHandler extends IServerHandler<RoomWealthProto> {

	@Override
	public void execute(RoomWealthProto resp, S2SSession session) throws Exception {

		EWealthCategory category = EWealthCategory.of(resp.getCategory());
		if (EWealthCategory.NONE == category) {
			logger.error("不存在的财富类型更新! {}", resp);
			return;
		}

		C2SSession accountSession = C2SSessionService.getInstance().getSession(resp.getAccountId());
		if (null == accountSession) {
			return;
		}
		final Account account = accountSession.getAccount();
		if (null != account) {
			Response.Builder builder = updateAccountCategory(account, resp);
			if (null != builder) {
				accountSession.send(builder.build());
			}
		}
	}

	/**
	 * 
	 * @param account
	 * @param category
	 * @param value
	 */
	private static Response.Builder updateAccountCategory(final Account account, RoomWealthProto proto) {

		EWealthCategory category = EWealthCategory.of(proto.getCategory());

		AccountPropertyListResponse.Builder propertyListBuilder = AccountPropertyListResponse.newBuilder();

		if (EWealthCategory.GOLD == category) {
			long newGold = account.getAccountModel().getGold() + proto.getValue();
			account.getAccountModel().setGold(newGold);
			AccountPropertyResponse.Builder b = MessageResponse.getAccountPropertyResponse(EPropertyType.GOLD.getId(), proto.getChangeType(), null,
					null, null, null, null, null, newGold);
			propertyListBuilder.addAccountProperty(b);

		} else if (EWealthCategory.MONEY == category) {
			long newMoney = account.getAccountModel().getMoney() + proto.getValue();
			account.getAccountModel().setMoney(newMoney);
			AccountPropertyResponse.Builder b = MessageResponse.getAccountPropertyResponse(EPropertyType.MONEY.getId(), proto.getChangeType(), null,
					null, null, null, null, null, newMoney);
			propertyListBuilder.addAccountProperty(b);

		}

		Response.Builder responseBuilder = Response.newBuilder();
		responseBuilder.setResponseType(ResponseType.PROPERTY);
		responseBuilder.setExtension(Protocol.accountPropertyListResponse, propertyListBuilder.build());
		return responseBuilder;
	}
}
