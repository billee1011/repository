/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.cai.handler;

import org.apache.commons.lang.StringUtils;

import com.cai.common.domain.Account;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.EmojiFilter;
import com.cai.common.util.SpringService;
import com.cai.core.GbCdCtrl;
import com.cai.core.GbCdCtrl.Opt;
import com.cai.service.PlayerServiceImpl;
import com.cai.util.MessageResponse;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;

import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.Request.RequestType;
import protobuf.clazz.Protocol.SignatureRequest;

/**
 *
 * @author wu_hc
 */
@ICmd(code = RequestType.SIGNATURE_VALUE, exName = "signatureRequest")
public class SignatureHandler extends IClientHandler<SignatureRequest> {

	@Override
	protected void execute(SignatureRequest message, Request topRequest, C2SSession session) throws Exception {

		if (!GbCdCtrl.canHandle(session, Opt.SIGNATURE))
			return;

		String signture = message.getText();

		// 1空判断
		if (StringUtils.isEmpty(signture)) {
			session.send(MessageResponse.getMsgAllResponse("签名为空!").build());
			return;
		}

		// 2emoj表情过滤
		if (EmojiFilter.containsEmoji(signture)) {
			session.send(MessageResponse.getMsgAllResponse("不支持该格式签名!").build());
			return;
		}

		// 3长度判断
		if (signture.length() > 200) {

			PlayerServiceImpl.getInstance().sendAccountMsg(session, MessageResponse.getMsgAllResponse("签名长度有误,请重新输入!").build());

			logger.warn("玩家[{}]设置签名，但签名长度不合法!!", session.getAccount());
			return;
		}

		final Account account = session.getAccount();

		if (null == account || null == account.getAccountModel()) {
			return;
		}
		// >>是否和原来的一样
		if (signture.equals(account.getAccountModel().getSignature())) {
			logger.warn("玩家[{}]设置签名，但签名[{}]和以前一样!!", session.getAccount(), signture);
			return;
		}

		// 1更新当前进程的缓存[不需要，在收到center服务器广播玩家数据时会自动刷新]
		// account.getAccountModel().setSignture(signture);

		// 2通知center server
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		int status = centerRMIServer.modifySigntrue(account.getAccount_id(), signture);

		// 3失败的话通知客户端
		if (0 != status) {
			PlayerServiceImpl.getInstance().sendAccountMsg(session, MessageResponse.getMsgAllResponse("设置失败!").build());
		}
	}

}
