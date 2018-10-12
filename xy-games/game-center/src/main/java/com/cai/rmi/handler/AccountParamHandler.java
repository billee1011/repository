package com.cai.rmi.handler;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.cai.common.constant.RMICmd;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountParamModel;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.service.PublicServiceImpl;

/**
 * 
 * 
 *
 * @author tang date: 2018年01月24日 上午10:11:20 <br/>
 */
@IRmi(cmd = RMICmd.ACCOUNT_PARAM_HANDLER, desc = "获取用户参数")
public final class AccountParamHandler extends IRMIHandler<Map<String,String> , Map<Integer,AccountParamModel>> {
	
	@Override
	public Map<Integer,AccountParamModel> execute(Map<String,String> map) {
		String accountId = map.get("account_id");
		String paramId = map.get("paramId");
		Map<Integer,AccountParamModel> modelMap = new HashMap<>();
		if(StringUtils.isBlank(accountId)||StringUtils.isBlank(paramId)){
			return modelMap;
		}
		Long account_id = Long.parseLong(accountId);
		Account account = PublicServiceImpl.getInstance().getAccount(account_id);
		if(account==null){
			return modelMap;
		}
		if(paramId.equals("0")){
			return account.getAccountParamModelMap();
		}else{
			String[] params = paramId.split(",");
			for(String str:params){
				int pid = Integer.parseInt(str);
				AccountParamModel model = account.getAccountParamModelMap().get(pid);
				modelMap.put(pid, model);
			}
			return modelMap;
		}
	}

}
