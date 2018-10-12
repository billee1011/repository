/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler.c2s;

import org.apache.commons.lang.StringUtils;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.RMICmd;
import com.cai.common.domain.AccountPushModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.SpringService;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;

import protobuf.clazz.Protocol.Request;
import protobuf.clazz.c2s.C2SProto.PushReport;

/**
 * 
 *
 * @author tang date: 2017年12月22日 上午11:17:00 <br/>
 */
@ICmd(code = C2SCmd.PUSH_REPROT, desc = "推送上报")
public final class PushReportHandler extends IClientHandler<PushReport> {


	@Override
	protected void execute(PushReport req, Request topRequest, C2SSession session) throws Exception {
		if(StringUtils.isBlank(req.getEquipmentId())||req.getPlat()==0){
			return;
		}
		AccountPushModel model = new AccountPushModel();
		model.setAccount_id(session.getAccountID());
		model.setEquipment_id(req.getEquipmentId());
		model.setPlat(req.getPlat());
		SpringService.getBean(ICenterRMIServer.class).rmiInvoke(RMICmd.REPORT_PUSH_ID, model);
	}
}
