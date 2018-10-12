/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.c2s;

import java.util.regex.Pattern;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.RMICmd;
import com.cai.common.define.EPropertyType;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.rmi.vo.CertificationRMIVo;
import com.cai.common.util.EmojiFilter;
import com.cai.common.util.SpringService;
import com.cai.util.MessageResponse;
import com.google.common.base.Strings;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.AccountPropertyListResponse;
import protobuf.clazz.Protocol.AccountPropertyResponse;
import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;
import protobuf.clazz.c2s.C2SProto.CertificationProto;

/**
 * 
 * 
 *
 * @author wu_hc date: 2017年12月1日 下午3:02:47 <br/>
 */
@ICmd(code = C2SCmd.CERTIFICATION, desc = "实名认证")
public final class CertificationHandler extends IClientHandler<CertificationProto> {

	/**
	 * 身份证
	 */
	final Pattern p = Pattern.compile("^\\d{15}$|^\\d{17}[0-9Xx]$");

	@Override
	protected void execute(CertificationProto req, Request topRequest, C2SSession session) throws Exception {
		final long accountId = session.getAccountID();
		if (accountId <= 0) {
			return;
		}

		String realName = req.getRealName();
		String identityCard = req.getIdentityCard();

		if (Strings.isNullOrEmpty(realName) || Strings.isNullOrEmpty(identityCard)) {
			return;
		}

		if (EmojiFilter.containsEmoji(realName)) {
			session.send(MessageResponse.getMsgAllResponse("格式不对!").build());
			return;
		}
		if (!p.matcher(identityCard).matches()) {
			session.send(MessageResponse.getMsgAllResponse("身份证格式不对，请检查!").build());
			return;
		}

		final Account account = session.getAccount();
		final AccountModel accountModel = account.getAccountModel();

		if (!Strings.isNullOrEmpty(accountModel.getReal_name()) || !Strings.isNullOrEmpty(accountModel.getIdentity_card())) {
			session.send(MessageResponse.getMsgAllResponse("已经实名认证过!").build());
			return;
		}
		CertificationRMIVo vo = new CertificationRMIVo();
		vo.setAccountId(accountId);
		vo.setRealId(identityCard);
		vo.setRealName(realName);
		SpringService.getBean(ICenterRMIServer.class).rmiInvoke(RMICmd.CERTIFICATION, vo);

		AccountPropertyListResponse.Builder builder = AccountPropertyListResponse.newBuilder();
		AccountPropertyResponse.Builder accountPropertyResponseBuilder = MessageResponse.getAccountPropertyResponse(
				EPropertyType.CERTIFICATION.getId(), null, null, null, null, null, vo.getRealId(), vo.getRealName(), null);
		builder.addAccountProperty(accountPropertyResponseBuilder);

		Response.Builder responseBuilder = Response.newBuilder();
		responseBuilder.setResponseType(ResponseType.PROPERTY);
		responseBuilder.setExtension(Protocol.accountPropertyListResponse, builder.build());
		session.send(responseBuilder.build());

	}
}
