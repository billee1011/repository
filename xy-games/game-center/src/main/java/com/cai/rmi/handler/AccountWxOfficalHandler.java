package com.cai.rmi.handler;

import java.util.Date;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.cai.common.constant.RMICmd;
import com.cai.common.domain.AccountMobileModel;
import com.cai.common.domain.AccountWxOfficalModel;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.dictionary.AccountWxOfficialDict;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PhoneService;

/**
 * 
 * 
 *
 * @author tang date: 2018年04月19日 下午16:08:20 <br/>
 */
@IRmi(cmd = RMICmd.BIND_WX_OFFICAL, desc = "手机绑定微信公众号")
public final class AccountWxOfficalHandler extends IRMIHandler<Map<String, String>,Integer> {
	private static final int TYPE_EXIST_ACCOUNT = 1;
	private static final int TYPE_EXIST_OPENID = 2;
	private static final int TYPE_UNBIND = 3;
//	private static final int TYPE_BIND = 0;
	
	
	@SuppressWarnings("unchecked")
	@Override
	public Integer execute(Map<String, String> map) {
		String mobile = map.get("mobile");
		String accountId = map.get("accountId");
		String openId = map.get("openId");
		String typeStr = map.get("type");
//		if(StringUtils.isBlank(mobile)||StringUtils.isBlank(accountId)||StringUtils.isBlank(openId)){
//			return -1;
//		}
		int type = 0;
		if(StringUtils.isNotBlank(typeStr)){
			type = Integer.parseInt(typeStr);
		}
		if(TYPE_EXIST_ACCOUNT == type){
			if(StringUtils.isBlank(accountId)){
				return -1;
			}
			long account_id = Long.parseLong(accountId);
			if(AccountWxOfficialDict.getInstance().isExistAccount(account_id)){
				return 1;
			}else{
				return 0;
			}
		}
		if(TYPE_EXIST_OPENID == type){
			if(StringUtils.isBlank(openId)){
				return -1;
			}
			if(AccountWxOfficialDict.getInstance().isExistOpenId(openId)){
				return 1;
			}else{
				return 0;
			}
		}
		if(TYPE_UNBIND == type){
			if(StringUtils.isBlank(openId)||StringUtils.isBlank(mobile)){
				return -1;
			}
			if(!AccountWxOfficialDict.getInstance().isExistOpenId(openId)){
				return 0;
			}else{
				long account_id = AccountWxOfficialDict.getInstance().getAccountIdByOpenId(openId);
				boolean res = AccountWxOfficialDict.getInstance().unBindOpenId(openId, account_id,mobile);
				if(res){
					return 1;
				}
				return 0;
			}
		}
		if(StringUtils.isBlank(mobile)||StringUtils.isBlank(accountId)||StringUtils.isBlank(openId)){
			return -1;
		}
		long account_id = Long.parseLong(accountId);
//		Account account = PublicServiceImpl.getInstance().getAccount(account_id);
//		if(account == null){
//			return -1;
//		}
//		ReentrantLock lock = account.getRedisLock();
//		lock.lock();
//		try{
			if(AccountWxOfficialDict.getInstance().isExistAccount(account_id)){
				return 2;
			}
			AccountMobileModel model = PhoneService.getInstance().getPhoneModel(account_id).get();
			if(model != null&&model.getMobile_phone().equals(mobile)){
				AccountWxOfficalModel am = new AccountWxOfficalModel();
				am.setAccount_id(account_id);
				am.setCreate_time(new Date());
				am.setMobile(mobile);
				am.setOpenId(openId);
				AccountWxOfficalModel om = AccountWxOfficialDict.getInstance().put(am);
				if(om==null){
					MongoDBServiceImpl.getInstance().getLogQueue().add(am);
				}
				return 1;
			}else{
				return -1;
			}
//		}catch(Exception e) {
//			logger.error("error", e);
//		}finally {
//			lock.unlock();
//		}
//		return -1;
	}
}
