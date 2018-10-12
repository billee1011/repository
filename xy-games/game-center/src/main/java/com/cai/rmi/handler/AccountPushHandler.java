package com.cai.rmi.handler;

import org.apache.commons.lang.StringUtils;

import com.cai.common.constant.RMICmd;
import com.cai.common.domain.AccountPushModel;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.common.util.GlobalExecutor;
import com.cai.service.AccountPushServiceImp;

/**
 * 
 * 
 *
 * @author tang date: 2017年11月27日 下午3:41:24 <br/>
 */
@IRmi(cmd = RMICmd.REPORT_PUSH_ID, desc = "推送上报信息处理")
public final class AccountPushHandler extends IRMIHandler<AccountPushModel , Integer> {

	@Override
	public Integer execute(final AccountPushModel model) {
		int result = -1;
		if(model==null||model.getAccount_id()==0||StringUtils.isBlank(model.getEquipment_id())){
			return result;
		}
		
		
		GlobalExecutor.asyn_execute(new Runnable(){
			@Override
			public void run() {
				AccountPushServiceImp.getInstance().addAccountPushModel(model);
			}
		});
		
		return 1;
	}
}
