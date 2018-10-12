/**
 * 湘ICP备15020076 copyright@2015-2016湖南旗胜网络科技有限公司
 */
package com.cai.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.cai.common.define.ERankType;
import com.cai.common.domain.AccountSimple;
import com.cai.common.domain.RedPackageRankModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.SpringService;
import com.google.common.collect.Maps;

import protobuf.clazz.Protocol.RankInfoProto;

/**
 * 排行榜，排行榜数据会从center server当前服务器
 *
 * @author wu_hc
 */
public final class RankService {

	/**
	 * 
	 */
	private static final RankService INSTANCE = new RankService();

	/**
	 * 排行榜缓存
	 */
	private final Map<ERankType, List<RankInfoProto>> rankMap = Maps.newConcurrentMap();
	
	private long rankTime = 0;
	/**
	 * 
	 */
	private List<RedPackageRankModel> redPackageRankList = new ArrayList<RedPackageRankModel>();
	private RankService() {
	}

	/**
	 * 
	 * @return
	 */
	public static RankService getInstance() {
		return RankService.INSTANCE;
	}

	/**
	 * 
	 * @param type
	 * @param ranks
	 */
	public void addOrUpdate(ERankType type, List<RankInfoProto> ranks) {
		rankMap.put(type, ranks);
	}

	/**
	 * 
	 * @param type
	 * @return
	 */
	public List<RankInfoProto> getRankByType(ERankType type) {
		List<RankInfoProto> ranks = rankMap.get(type);
		return null != ranks ? Collections.unmodifiableList(ranks) : Collections.emptyList();
	}

	// 获取排行榜数据 key为计算排行榜的时间戳,只需取values就
	public List<RedPackageRankModel> getRedPackageRankList() {
		if(rankTime==0){
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
			for (Entry<Long, Long> e : top10) {
				AccountSimple targetAccount = centerRMIServer.getSimpleAccount( e.getKey());
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
