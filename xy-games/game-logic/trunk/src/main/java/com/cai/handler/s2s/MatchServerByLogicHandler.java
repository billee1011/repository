/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.s2s;

import com.cai.common.constant.S2SCmd;
import com.cai.common.handler.IServerHandler;
import com.cai.service.MatchTableService;
import com.xianyi.framework.core.transport.IServerCmd;
import com.xianyi.framework.core.transport.netty.session.S2SSession;

import protobuf.clazz.match.MatchRsp.MatchS2SRequest;

@IServerCmd(code = S2SCmd.MATCH_SERVER, desc = "服务器")
public class MatchServerByLogicHandler extends IServerHandler<MatchS2SRequest> {

	@Override
	public void execute(MatchS2SRequest resp, S2SSession session) throws Exception {
		switch (resp.getCmd()) {
		case S2S_MATCH_START:
			MatchTableService.getInstance().matchStart(resp.getMatchStart(), session);
			break;
		case S2S_MATCH_CLIENT:
			MatchTableService.getInstance().matchStart(resp.getMatchStart(), session);
			break;
		case S2S_MATCH_TOP_STATUS:
			MatchTableService.getInstance().matchTopStatus(resp.getMatchTopStatus());
			break;
		default:
			break;
		}
	}





}
