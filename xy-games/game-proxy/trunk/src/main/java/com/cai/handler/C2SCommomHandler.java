/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler;

import com.cai.common.constant.C2SCmd;
import com.cai.common.domain.Account;
import com.cai.intercept.c2s.PhoneReqIntercept;
import com.cai.intercept.c2s.ReqClubIntercept;
import com.cai.intercept.c2s.ReqCoinIntercept;
import com.cai.intercept.c2s.ReqFoundationIntercept;
import com.cai.intercept.c2s.ReqIntercept;
import com.cai.intercept.c2s.ReqLogicIntercept;
import com.cai.intercept.c2s.ReqMatchIntercept;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;
import com.xianyi.framework.handler.ReqExExecutor;

import protobuf.clazz.Protocol.CommonProto;
import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.Request.RequestType;

/**
 * 拆包用,拆包的一个中间层
 *
 * @author wu_hc date: 2017年7月31日 上午10:38:27 <br/>
 */
@ICmd(code = RequestType.C2S_VALUE, exName = "c2sRequest")
public class C2SCommomHandler extends IClientHandler<CommonProto> {

	// 拦截器
	private static final ReqIntercept clubIntercept = new ReqClubIntercept();
	private static final ReqIntercept phoneIntercept = new PhoneReqIntercept();
	private static final ReqIntercept matchIntercept = new ReqMatchIntercept();
	private static final ReqFoundationIntercept foundationIntercept = new ReqFoundationIntercept();
	private static final ReqIntercept logicIntercept = new ReqLogicIntercept();
	private static final ReqIntercept coinIntercept = new ReqCoinIntercept();

	@Override
	protected void execute(CommonProto commProto, Request topRequest, C2SSession session) throws Exception {

		// 很特殊特殊,需要在登陆之前请求
		if (phoneIntercept.intercept(commProto, topRequest, session)) {
			return;
		}

		final Account account = session.getAccount();
		if (null == account) {
			logger.error("玩家不存在，但请求了通用协议!协议号:[{}]", commProto.getCmd());
			return;
		}

		// 容错，防止客户端误传：此类用于处理的拓展协议，如果客户端传值太小
		if (commProto.getCmd() > 0 && commProto.getCmd() <= C2SCmd.C2S) {
			logger.error("玩家:[{}],发起C2S拓展协议请求，但 cmd不合法[{}]", account, commProto.getCmd());
			return;
		}

		// 拦截到则返回
		if (clubIntercept.intercept(commProto, topRequest, session)) {
			return;
		}
		// 拦截到则返回
		if (matchIntercept.intercept(commProto, topRequest, session)) {
			return;
		}

		// 拦截则返回
		if (foundationIntercept.intercept(commProto, topRequest, session)) {
			return;
		}

		// 逻辑服拦截
		if (logicIntercept.intercept(commProto, topRequest, session)) {
			return;
		}

		//金币场
		if (coinIntercept.intercept(commProto, topRequest, session)) {
			return;
		}
		Runnable reqExExecutor = new ReqExExecutor(commProto, topRequest, session);
		if (account.getWorkerLoop().inEventLoop()) {
			reqExExecutor.run();
		} else {
			account.getWorkerLoop().runInLoop(reqExExecutor);
		}
	}
}
