/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.cai.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import com.cai.common.define.ERankType;
import com.cai.common.domain.AccountSimple;
import com.cai.common.domain.Event;
import com.cai.common.domain.RankModel;
import com.cai.common.domain.RedPackageRankModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.SpringService;
import com.cai.core.MonitorEvent;
import com.cai.domain.Session;
import com.cai.timer.RankAndNoticeTimer;
import com.google.common.collect.Maps;

/**
 *
 * 排行榜服务
 * 
 * @author wu_hc
 */
public class RankServiceImp extends AbstractService {

	/**
	 * 排行榜缓存
	 */
	private Map<ERankType, List<RankModel>> rankMap = Maps.newConcurrentMap();
	
	
	private long rankTime = 0;
	/**
	 * 
	 */
	private List<RedPackageRankModel> redPackageRankList = new ArrayList<RedPackageRankModel>();
	/**
	 * 排行榜调度器
	 */
	private final RankAndNoticeTimer rankTimer = new RankAndNoticeTimer();

	private static final class LazzyHolder {
		private static final RankServiceImp INSTANCE = new RankServiceImp();
	}

	public static RankServiceImp getInstance() {
		return LazzyHolder.INSTANCE;
	}

	@Override
	protected void startService() {
		this.doTriggerRank();
	}

	@Override
	public MonitorEvent montior() {
		return null;
	}

	@Override
	public void onEvent(Event<SortedMap<String, String>> event) {

	}

	@Override
	public void sessionCreate(Session session) {

	}

	@Override
	public void sessionFree(Session session) {

	}

	@Override
	public void dbUpdate(int _userID) {

	}

	/**
	 * 排行榜缓存
	 * 
	 * @param type
	 * @param builder
	 */
	public void addRank(ERankType type, List<RankModel> models) {
		rankMap.put(type, models);
	}

	/**
	 * 
	 * @param type
	 * @return
	 */
	public List<RankModel> getRank(ERankType type) {
		List<RankModel> models = rankMap.get(type);
		return null != models ? Collections.unmodifiableList(models) : Collections.emptyList();
	}

	/**
	 * 出发排行榜
	 */
	public void doTriggerRank() {
		rankTimer.run();
	}

	// 获取排行榜数据 key为计算排行榜的时间戳,只需取values就
	public List<RedPackageRankModel> getRedPackageRankList() {
		if(rankTime==0){
			rankTime = System.currentTimeMillis();
			initRedPackageRankList();
		}else{
			long nowTime = System.currentTimeMillis();
			if(nowTime>rankTime+5*60*1000){
				initRedPackageRankList();
			}
		}
		return redPackageRankList;
	}

	private void initRedPackageRankList() {
		List<Entry<Long, Long>> top10 = MongoDBServiceImpl.getInstance().getRedPackageRankByActiveId();
		rankTime = System.currentTimeMillis();
		// 排行是空的
		if (top10 == null || top10.size() == 0) {
			redPackageRankList.addAll(new ArrayList<RedPackageRankModel>());
		} else {
			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
			List<RedPackageRankModel> rankList = new ArrayList<RedPackageRankModel>();
			int i = 1;
			 for (Entry<Long, Long> e: top10) { 
				 AccountSimple targetAccount = centerRMIServer.getSimpleAccount(e.getKey());
					RedPackageRankModel model = new RedPackageRankModel();
					model.setAccountId(e.getKey());
					if(targetAccount!=null){
						model.setHead(targetAccount.getIcon());
						model.setNickName(targetAccount.getNick_name());
					}else{
						model.setHead("");
						model.setNickName("-");
					}
					model.setRank(i);
					model.setValue(e.getValue());
					rankList.add(model);
					i++;
			 }
			redPackageRankList = rankList;
		}
	}
}
