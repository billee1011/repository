package com.cai.manager;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.AccountMatchInfoRedis;
import com.cai.common.domain.AccountMatchRedis;
import com.cai.common.domain.AccountMatchTopRedis;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;

public class MatchManager {
	private static final Logger logger = LoggerFactory.getLogger(MatchManager.class);
	private static MatchManager matchManager = new MatchManager();
	private Map<Integer, Object> lockMap = new HashMap<>();
	private static final long LOCK_NUM = 8;
	private MatchManager(){
		initLock();
	}
	
	public static MatchManager INSTANCE(){
		return matchManager;
	}
	
	private void initLock(){
		Object obj = null;
		for(int index = 0;index < LOCK_NUM; index ++){
			obj = new Object();
			lockMap.put(index, obj);
		}
	}
	
	private Object getLockByAccountId(long accountId){
		int index = 0;
		if(accountId > 0){
			index = (int) (accountId % LOCK_NUM);
			if(index < 0){
				index = 0;
			}
		}
		Object obj = lockMap.get(index);
		return obj;
	}
	
	public AccountMatchInfoRedis getAccountMatchByStatus(long accountId,int status){
		if(accountId <= 0){
			return null;
		}
		AccountMatchRedis accountMatchRedis = getAccountMatch(accountId);
		if(accountMatchRedis == null){
			return null;
		}
		Map<Integer, AccountMatchInfoRedis> map = accountMatchRedis.getMatchInfoMap();
		for(AccountMatchInfoRedis info : map.values()){
			if(info.getStatus() == status){
				return info;
			}
		}
		return null;
	}
	
	public AccountMatchInfoRedis getAccountMatchStatus(long accountId,int matchId,int id){
		if(accountId <= 0){
			return null;
		}
		AccountMatchInfoRedis matchInfo = getAccountMatchInfo(accountId,matchId);
		if(matchInfo == null){
			return null;
		}
		if(matchInfo.getId() == id){
			return matchInfo;
		}
		return null;
	}
	
	public void addAccountMatchInfo(long accountId,int matchId,int id,int status){
		Object lock = getLockByAccountId(accountId);
		synchronized (lock) {
			AccountMatchRedis accountMatchRedis = getAccountMatch(accountId);
			if(accountMatchRedis == null){
				accountMatchRedis = new AccountMatchRedis();
			}
			AccountMatchInfoRedis matchInfo = accountMatchRedis.getMatchInfo(matchId);
			if(matchInfo == null){
				matchInfo = new AccountMatchInfoRedis();
			}
			matchInfo.setMatchId(matchId);
			matchInfo.setId(id);
			matchInfo.setStatus(status);
			accountMatchRedis.addMatchInfo(matchInfo);
			setAccountMatch(accountId, accountMatchRedis);
		}
	}
	
	public int startAccountMatchInfo(long accountId,int matchId,int id,int logicIndex,int status){
		Object lock = getLockByAccountId(accountId);
		synchronized (lock) {
			AccountMatchRedis accountMatchRedis = getAccountMatch(accountId);
			boolean isUpdate = false;
			if(accountMatchRedis == null){
				accountMatchRedis = new AccountMatchRedis();
				isUpdate = true;
			}
			
			AccountMatchInfoRedis matchInfo = accountMatchRedis.getMatchInfo(matchId);
			if(matchInfo == null){
				matchInfo = new AccountMatchInfoRedis();
				matchInfo.setMatchId(matchId);
				matchInfo.setId(id);
				matchInfo.setStatus(status);
				matchInfo.setStart(true);
				matchInfo.setLogicIndex(logicIndex);
				matchInfo.setApplyType(-1);
				accountMatchRedis.addMatchInfo(matchInfo);
				isUpdate = true;
				logger.error("startAccountMatchInfo -> no account match info accountId:{} matchId:{} id:{} !!",
						accountId,matchId,id);
			}else{
				if(matchInfo.getId() == id){
					matchInfo.setStatus(status);
					matchInfo.setStart(true);
					matchInfo.setLogicIndex(logicIndex);
					accountMatchRedis.addMatchInfo(matchInfo);
					isUpdate = true;
				}else{
					logger.error("startAccountMatchInfo -> error account match info accountId:{} matchId:{} id:{} mId:{} !!",
							accountId,matchId,id,matchInfo.getId());
				}
			}
			if(isUpdate){
				setAccountMatch(accountId, accountMatchRedis);
			}
			return matchInfo.getApplyType();
		}
	}
	
	public AccountMatchInfoRedis getAccountMatchInfo(long accountId,int matchId,int id){
		AccountMatchInfoRedis matchInfo = getAccountMatchInfo(accountId, matchId);
		if(matchInfo != null && matchInfo.getId() == id){
			return matchInfo;
		}
		return null;
	}
	
	public AccountMatchInfoRedis getAccountMatchInfo(long accountId,int matchId){
		AccountMatchRedis accountMatchRedis = getAccountMatch(accountId);
		if(accountMatchRedis == null){
			return null;
		}
		AccountMatchInfoRedis matchInfo = accountMatchRedis.getMatchInfo(matchId);
		return matchInfo;
	}
	
	public AccountMatchInfoRedis getAccountMatchInfo(AccountMatchRedis accountMatchRedis,int matchId,int id){
		if(accountMatchRedis == null){
			return null;
		}
		AccountMatchInfoRedis matchInfo = accountMatchRedis.getMatchInfo(matchId);
		if(matchInfo != null && matchInfo.getId() == id){
			return matchInfo;
		}
		return null;
	}
	
	public boolean removeAccountMatchInfo(long accountId,int matchId,int id){
		Object lock = getLockByAccountId(accountId);
		synchronized (lock) {
			AccountMatchRedis accountMatchRedis = getAccountMatch(accountId);
			if(accountMatchRedis == null){
				return false;
			}
			if(accountMatchRedis.removeMatchInfo(matchId,id)){
				setAccountMatch(accountId, accountMatchRedis);
				return true;
			}
		}
		return false;
	}
	
	public AccountMatchRedis getAccountMatch(long accountId){
		AccountMatchRedis accountMatchRedis = SpringService.getBean(RedisService.class).hGet(RedisConstant.MATCH_ROOM_ACCOUNT, accountId + "",
				AccountMatchRedis.class);
		return accountMatchRedis;
	}
	
	private void setAccountMatch(long accountId,AccountMatchRedis accountMatchRedis){
		SpringService.getBean(RedisService.class).hSet(RedisConstant.MATCH_ROOM_ACCOUNT, accountId + "", accountMatchRedis);
	}
	
	public AccountMatchTopRedis getAccountMatchTop(){
		AccountMatchTopRedis redis = SpringService.getBean(RedisService.class).hGet(RedisConstant.DICT_MATCH_TOP_STATUS, RedisConstant.DICT_MATCH_TOP_STATUS,
				AccountMatchTopRedis.class);
		if(redis == null){
			redis = new AccountMatchTopRedis();
		}
		return redis;
	} 
	
	public void saveAccountMatchTop(AccountMatchTopRedis redis){
		SpringService.getBean(RedisService.class).hSet(RedisConstant.DICT_MATCH_TOP_STATUS, RedisConstant.DICT_MATCH_TOP_STATUS, redis);
	} 

}
