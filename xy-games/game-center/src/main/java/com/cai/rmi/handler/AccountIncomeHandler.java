package com.cai.rmi.handler;

import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang.StringUtils;

import com.cai.common.constant.RMICmd;
import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountModel;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;
import com.cai.service.PublicService;
import com.cai.service.PublicServiceImpl;

/**
 * 
 * 
 *
 * @author tang date: 2018年01月08日 下午2:11:24 <br/>
 */
@IRmi(cmd = RMICmd.UPDATE_ACCOUNT_INCOME, desc = "用户收益一次提现操作")
public final class AccountIncomeHandler extends IRMIHandler<Map<String,String> , Integer> {
	
	private  int ERROR =-1;
	private  int SUCCESS =1;
	@Override
	public Integer execute(Map<String,String> map) {
		String accountId = map.get("account_id");
		String drawCash = map.get("drawCash");
		if(StringUtils.isBlank(accountId)||StringUtils.isBlank(drawCash)){
			return ERROR;
		}
		Long account_id = Long.parseLong(accountId);
		Account account = PublicServiceImpl.getInstance().getAccount(account_id);
		if(account==null||account.getHallRecommendModel().getRecommend_level()==0){
			return ERROR;
		}
		AccountModel accountModel = account.getAccountModel();
		double cash = Double.parseDouble(drawCash);
		if(cash<=0){
			return ERROR;
		}
		if(cash>accountModel.getRecommend_remain_income()){
			return ERROR;
		}
		ReentrantLock lock = account.getRedisLock();
		lock.lock();
		try{
			accountModel.setRecommend_remain_income(accountModel.getRecommend_remain_income()-cash);
			accountModel.setRecommend_receive_income(accountModel.getRecommend_receive_income()+cash);
//			SpringService.getBean(RedisService.class).hSet(RedisConstant.ACCOUNT, account_id + "", account);
			PublicService publicService = SpringService.getBean(PublicService.class);
			publicService.getPublicDAO().updateAccountIncome(accountModel);
		}catch(Exception e) {
			logger.error("error", e);
		}finally {
			lock.unlock();
		}
		return SUCCESS;
	}

}
