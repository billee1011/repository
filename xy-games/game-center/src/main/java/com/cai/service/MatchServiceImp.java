/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.cai.service;

import java.util.HashMap;
import java.util.List;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.cai.common.domain.Event;
import com.cai.common.domain.ItemModel;
import com.cai.common.util.SpringService;
import com.cai.core.MonitorEvent;
import com.cai.domain.Session;

/**
 *
 * 红包
 * 
 * @author tang
 * <a style="color:red;">比赛序号分配 </a>
 */
public class MatchServiceImp extends AbstractService {

	/**
	 * 按参赛券分配最大报名序号
	 */
	private ConcurrentHashMap<Integer, AtomicInteger> maxSeqMap = null;
	
	/**
	 * 排行榜调度器
	 */
	private MatchServiceImp() {
		maxSeqMap = new ConcurrentHashMap<Integer, AtomicInteger>();
	}

	private static MatchServiceImp instance = null;

	public static MatchServiceImp getInstance() {
		if (null == instance) {
			instance = new MatchServiceImp();
		}
		return instance;
	}
	// 系统启服的时候分配序号
	public  void dispatchMaxSeqMap() {
		PublicService publicService = SpringService.getBean(PublicService.class);
		List<HashMap<String,Integer>> maxSeqList = publicService.getPublicDAO().getMatchMaxSeq();
		for(HashMap<String,Integer> map:maxSeqList){
			maxSeqMap.put(map.get("itemId"), new AtomicInteger(map.get("seq")));
		}
		List<ItemModel> itemList = publicService.getPublicDAO().getOfflineItemModelList();
		if(itemList.size()>0){
			for(ItemModel model:itemList){
				if(!maxSeqMap.containsKey(model.getItemId())){
					maxSeqMap.put(model.getItemId(), new AtomicInteger(0));
				}
			}
			
		}
		
	}
	// 取分配的报名序号
	public  int takeSeqByItemId(int itemId) {
		if(!maxSeqMap.containsKey(itemId)){
			dispatchMaxSeqMap();
		}
		int maxSeq = maxSeqMap.get(itemId).incrementAndGet();
		return maxSeq;
	}

	@Override
	protected void startService() {
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
}
