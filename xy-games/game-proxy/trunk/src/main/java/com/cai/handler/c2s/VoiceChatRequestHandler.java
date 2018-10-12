package com.cai.handler.c2s;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.domain.Account;
import com.cai.common.domain.VoiceChatLogModel;
import com.cai.common.util.PBUtil;
import com.cai.service.MongoDBServiceImpl;
import com.google.protobuf.ByteString;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;

import protobuf.clazz.Protocol.Request;
import protobuf.clazz.c2s.C2SProto.VoiceChatRequestProto;
import protobuf.clazz.c2s.C2SProto.VoiceChatResponse;

/**
 * 
 *
 * @author zhanglong date: 2018年5月29日 上午11:51:39
 */
@ICmd(code = C2SCmd.GET_VOICE_CHAT, desc = "获取语音聊天")
public class VoiceChatRequestHandler extends IClientHandler<VoiceChatRequestProto> {

	@Override
	protected void execute(VoiceChatRequestProto req, Request topRequest, C2SSession session) throws Exception {
		Account account = session.getAccount();
		if (account == null)
			return;
		VoiceChatResponse.Builder builder = VoiceChatResponse.newBuilder();
		builder.setType(req.getType());
		builder.setClubId(req.getClubId());
		builder.setUniqueId(req.getUniqueId());
		VoiceChatLogModel model = MongoDBServiceImpl.getInstance().getVoiceChat(req);
		if (model != null) {
			builder.setContent(ByteString.copyFrom(model.getContent()));
		}

		session.send(PBUtil.toS2CCommonRsp(S2CCmd.VOICE_CHAT_RSP, builder));

	}

}
