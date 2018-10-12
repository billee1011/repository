/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.future.runnable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.Account;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;
import com.cai.service.PublicServiceImpl;

/**
 *
 * date: 2017年11月11日 下午2:30:17 <br/>
 */
public class ClearAccountCacheRunnable implements Runnable {

	
	private static Logger logger = LoggerFactory.getLogger(ClearAccountCacheRunnable.class);

	@Override
	public void run() {
		
		int count = 0;
		long now = System.currentTimeMillis();
		
		for(Account account:PublicServiceImpl.getInstance().getAccountIdMap().values()) {
			
			if(account.getAccountModel().isNeedDB()) continue;
			
			if(account.getAccountModel().getLast_login_time()!=null) {
				long loginTime = account.getAccountModel().getLast_login_time().getTime();
				long passHour = (now-loginTime)/3600000;
				if(passHour>30) {//超过30个小时的移除掉
					try {
						PublicServiceImpl.getInstance().clearAccount(account);
//						// redis
//						RedisService redisService = SpringService.getBean(RedisService.class);
//						redisService.hDel(RedisConstant.ACCOUNT, (account.getAccount_id() + "").getBytes());
					} catch (Exception e) {
						logger.warn("清理玩家缓存出错",e);
					}
					count++;
				}
			}
		}
		
		long cost=System.currentTimeMillis()-now;
		
		logger.warn("清理玩家缓存花费的毫秒数 "+cost+"ms 清理的数量为:"+count);
		
	}

}
