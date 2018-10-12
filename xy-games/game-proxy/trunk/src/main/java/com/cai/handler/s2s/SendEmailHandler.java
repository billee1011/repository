/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.s2s;

import java.util.Collection;
import java.util.List;

import com.cai.common.constant.S2CCmd;
import com.cai.common.constant.S2SCmd;
import com.cai.common.define.EHeartType;
import com.cai.common.domain.StatusModule;
import com.cai.common.handler.IServerHandler;
import com.cai.common.util.PBUtil;
import com.cai.service.C2SSessionService;
import com.xianyi.framework.core.transport.IServerCmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.core.transport.netty.session.S2SSession;

import protobuf.clazz.c2s.C2SProto.RedHeartRsp;
import protobuf.clazz.s2s.S2SProto.SendEmailProto;

/**
 * 邮件红点推送
 */
@IServerCmd(code = S2SCmd.EMAILSEND, desc = "发送邮件")
public class SendEmailHandler extends IServerHandler<SendEmailProto> {
	//全部在线玩家
	static final int ALL = 0;
	//全部在线安卓玩家
	static final int ANDROID =1;
	//全部在线IOS玩家
	static final int IOS = 2; 
	//指定玩家
	static final int OTHER =3;
	
	
	@Override
	public void execute(SendEmailProto resp, S2SSession session) throws Exception {
		StatusModule status = new StatusModule();
		status.statusAdd(EHeartType.EMAIL);
		RedHeartRsp.Builder builder = RedHeartRsp.newBuilder();
		builder.setStatus(status.getStatus());
		
		int type = resp.getType();
		int userType = resp.getUserType();
		if(userType != OTHER){
			final Collection<C2SSession> onlineAccounts = C2SSessionService.getInstance().getAllOnlieSession();
			onlineAccounts.forEach((c2ssession) -> {
				//玩家设备
				String clientFlag = c2ssession.getAccount().getAccountModel().getLast_client_flag();
				if(type == ANDROID && !clientFlag.equals("android")){
					return;
				}else if(type == IOS && !clientFlag.equals("ios") ){
					return;
				}
				c2ssession.send(PBUtil.toS2CCommonRsp(S2CCmd.RED_HEART, builder));
			});
		}else{
			List<Long> list =  resp.getAccountsList();
			list.forEach((accountId)->{
				C2SSession  c2ssession = C2SSessionService.getInstance().getSession(accountId);
				if(c2ssession == null){
					return;
				}
				c2ssession.send(PBUtil.toS2CCommonRsp(S2CCmd.RED_HEART, builder));
			});
		}
		
	}
}
	
