package com.cai.rmi.handler;

import java.util.HashMap;

import com.cai.common.constant.RMICmd;
import com.cai.common.constant.RedisConstant;
import com.cai.common.define.EWxHeadimgurlType;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountModel;
import com.cai.common.domain.AccountSimple;
import com.cai.common.domain.AccountWeixinModel;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.common.util.SpringService;
import com.cai.common.util.WxUtil;
import com.cai.dao.PublicDAO;
import com.cai.redis.service.RedisService;
import com.cai.service.PublicService;
import com.cai.service.PublicServiceImpl;

/**
 * 
 * 
 *
 * @author tang date: 2017年12月08日 09:48:24 <br/>
 */
@IRmi(cmd = RMICmd.ACCOUNT_UNION_SWITCH, desc = "用户unionId交换")
public final class AccountSwitchRMIHandler extends IRMIHandler<HashMap<String,Long>, Integer> {

	@Override
	public Integer execute(HashMap<String,Long> map ) {
		long accountId = map.get("newId");//要替换的账号
		long oldAccountId = map.get("oldId");//被替换的账号
		if(accountId == oldAccountId||accountId==0||oldAccountId==0){
			return 0;
		}
		PublicServiceImpl publicServiceImpl = PublicServiceImpl.getInstance();
		Account account = publicServiceImpl.getAccount(accountId);
		if (account == null||account.getAccountWeixinModel()==null) {
			return -1;
		}
		Account oldAccount = publicServiceImpl.getAccount(oldAccountId);
		if (oldAccount == null||oldAccount.getAccountWeixinModel()==null) {
			return -2;
		}
		String accountName = oldAccount.getAccount_name();
		String oldAccountName = account.getAccount_name();
		oldAccount.getAccountModel().setAccount_name(oldAccountName);
		account.getAccountModel().setAccount_name(accountName);
		AccountWeixinModel acountWx = account.getAccountWeixinModel();
		AccountWeixinModel oldAcountWx = oldAccount.getAccountWeixinModel();
		//替换id，oldAccountWx为最新用户的微信信息了
		switchAccountWx(acountWx, oldAcountWx);
		account.setAccountWeixinModel(oldAcountWx);
		oldAccount.setAccountWeixinModel(acountWx);
		publicServiceImpl.switchUnionId(acountWx, oldAcountWx);
		logger.info(oldAccount.getAccountModel().getAccount_name()+"名称替换为"+account.getAccountModel().getAccount_name());
		updateWxAccount(account, oldAccount);
//		try {
//			// 更新redis缓存
//			SpringService.getBean(RedisService.class).hSet(RedisConstant.ACCOUNT, Long.toString(account.getAccount_id()), account);
//			SpringService.getBean(RedisService.class).hSet(RedisConstant.ACCOUNT, Long.toString(oldAccount.getAccount_id()), oldAccount);
//		} catch (Exception e) {
//			e.printStackTrace();
//			logger.error("error", e);
//		}
		AccountSimple accountSimple = publicServiceImpl.getAccountSimpe(account.getAccount_id());
		if (null != accountSimple) {
			accountSimple.setNick_name(account.getAccountWeixinModel().getNickname());
			accountSimple.setIcon(WxUtil.changHeadimgurl(account.getAccountWeixinModel().getHeadimgurl(), EWxHeadimgurlType.S132));
		}		
		AccountSimple oldAccountSimple = publicServiceImpl.getAccountSimpe(oldAccount.getAccount_id());
		if (null != oldAccountSimple) {
			accountSimple.setNick_name(oldAccount.getAccountWeixinModel().getNickname());
			accountSimple.setIcon(WxUtil.changHeadimgurl(oldAccount.getAccountWeixinModel().getHeadimgurl(), EWxHeadimgurlType.S132));
		}
		account.getAccountModel().setNeedDB(true);
		oldAccount.getAccountModel().setNeedDB(true);
		return 1;
	}
	public void switchAccountWx(AccountWeixinModel acountWx,AccountWeixinModel oldAcountWx){
		long accountId = acountWx.getAccount_id();
		long oldAccountId = oldAcountWx.getAccount_id();
		acountWx.setAccount_id(oldAccountId);
		oldAcountWx.setAccount_id(accountId);
	}
	public void updateWxAccount(Account account,Account oldAccount){
		PublicDAO publicDAO = SpringService.getBean(PublicService.class).getPublicDAO();
		publicDAO.updateAccountWeixinModel(account.getAccountWeixinModel());
		publicDAO.updateAccountWeixinModel(oldAccount.getAccountWeixinModel());
		logger.info(oldAccount.getAccountWeixinModel().getUnionid()+"微信唯一码替换为"+account.getAccountWeixinModel().getUnionid());
		account.getAccountModel().setAccount_name(account.getAccountModel().getAccount_name()+"#");
		oldAccount.getAccountModel().setAccount_name(oldAccount.getAccountModel().getAccount_name()+"#");
		publicDAO.updateAccountModel(account.getAccountModel());
		publicDAO.updateAccountModel(oldAccount.getAccountModel());
		account.getAccountModel().setAccount_name(account.getAccountModel().getAccount_name().substring(0,account.getAccountModel().getAccount_name().length()-1));
		oldAccount.getAccountModel().setAccount_name(oldAccount.getAccountModel().getAccount_name().substring(0,oldAccount.getAccountModel().getAccount_name().length()-1));
		publicDAO.updateAccountModel(account.getAccountModel());
		publicDAO.updateAccountModel(oldAccount.getAccountModel());
//		account.setNeedDB(true);
//		oldAccount.setNeedDB(true);
	}
}
