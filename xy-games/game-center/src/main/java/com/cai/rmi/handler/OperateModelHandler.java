package com.cai.rmi.handler;

import com.cai.common.constant.RMICmd;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountRecommendModel;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.common.util.SpringService;
import com.cai.service.PublicService;
import com.cai.service.PublicServiceImpl;

/**
 * 
 * 
 *
 * @author tang date: 2017年11月27日 下午3:41:24 <br/>
 */
@IRmi(cmd = RMICmd.UPDATE_MODEL, desc = "更新model")
public final class OperateModelHandler extends IRMIHandler<AccountRecommendModel, Integer> {

	@Override
	public Integer execute(AccountRecommendModel recommendModel) {
		int result = 0;
		try{
			long account_id = recommendModel.getAccount_id();
			Account account =  PublicServiceImpl.getInstance().getAccount(account_id);
			if (account == null){
				return result;
			}
			account.getAccountRecommendModelMap().put(recommendModel.getTarget_account_id(), recommendModel);
			PublicService publicService = SpringService.getBean(PublicService.class);
			publicService.getPublicDAO().updateAccountRecommendModel(recommendModel);
		}catch(Exception e){
			logger.error(recommendModel.getTarget_account_id()+"updateAccountRecommendModel error",e);
		}
		return 1;
		
	}

}
