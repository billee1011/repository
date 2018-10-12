/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.client;

import com.cai.common.constant.C2SCmd;
import com.cai.common.define.SceneId;
import com.cai.service.PlayerService;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientExHandler;

import protobuf.clazz.c2s.C2SProto.SceneReq;
import protobuf.clazz.s2s.S2SProto.TransmitProto;

/**
 * 
 *
 * @author wu_hc date: 2017年8月02日 上午16:11:00 <br/>
 */
@ICmd(code = C2SCmd.SECNE_REQ, desc = "场景相关")
public final class SceneReqHandler extends IClientExHandler<SceneReq> {

	@Override
	protected void execute(SceneReq req, TransmitProto topReq, C2SSession session) throws Exception {
		if (req.getSceneId() == SceneId.CLUB_SCENE) {
			if (req.getCategory() == 1) {
				PlayerService.getInstance().enter(topReq.getAccountId());
			} else {
				PlayerService.getInstance().exit(topReq.getAccountId());
			}
		}
	}
}
