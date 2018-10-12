/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.cai.service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.cai.common.domain.Event;
import com.cai.common.domain.RedPackageActivityModel;
import com.cai.core.MonitorEvent;
import com.cai.dictionary.RedPackageRuleDict;
import com.cai.domain.Session;

/**
 *
 * 红包
 * 
 * @author tang
 * <a style="color:red;">红包服务 </a>
 */
public class RedPackageServiceImp extends AbstractService {

	private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	/**
	 * 红包分配
	 */
	private ConcurrentHashMap<Integer, RedPackageActivity> redPackageTypeMap = null;
	
	private static Random random = new Random();;

	/**
	 * 排行榜调度器
	 */
	private RedPackageServiceImp() {
		redPackageTypeMap = new ConcurrentHashMap<Integer, RedPackageActivity>();
	}

	private static RedPackageServiceImp instance = null;

	public static RedPackageServiceImp getInstance() {
		if (null == instance) {
			instance = new RedPackageServiceImp();
		}
		return instance;
	}
	// 自动分配红包库存
	public synchronized void dispatchRedPackage() {
		RedPackageActivity type1 = new RedPackageActivity(1);
		type1.dispatchRedPackage();
		RedPackageActivity type2 = new RedPackageActivity(2);
		type2.dispatchRedPackage();
		redPackageTypeMap.put(1, type1);
		redPackageTypeMap.put(2, type2);
	}
	// 取红包
	public int takeRedPackage(int type) {
		return redPackageTypeMap.get(type).takeRedPackage();
	}

	public static class RedPackageActivity {
		public int type;
		public long startTime;
		public long endTime;
		public int total;//红包总数
		public int size;
		public long areaTime;//红包发送时间区间毫秒为单位
		private ConcurrentHashMap<Integer, Integer> redPackageMap = null;

		public ConcurrentHashMap<Integer, Integer> getRedPackageMap() {
			return redPackageMap;
		}

		public RedPackageActivity(int type) {
			super();
			this.type = type;
			redPackageMap = new ConcurrentHashMap<Integer, Integer>();
		}

		public void dispatchRedPackage() {
			ConcurrentHashMap<Integer, RedPackageActivityModel> redPackageRuleMap = RedPackageRuleDict.getInstance().getRedPackageRuleDictionary();
			if (redPackageRuleMap == null || redPackageRuleMap.size() == 0) {
				return;
			}
			for (int type : redPackageRuleMap.keySet()) {
				if (type != this.type) {
					continue;
				}
				RedPackageActivityModel model = redPackageRuleMap.get(type);
				String red_package_content = model.getRed_package_content(); // 红包内容(档次,概率,金额)
				int total = model.getPrize_count();// 红包总数
				addRedPackage(red_package_content, total, type);
			}
			// 初始化活动开始时间
			initActiveTime();
		}

		// 添加红包
		public void addRedPackage(String chanceArray, int total, int type) {
			JSONArray array = JSONArray.parseArray(chanceArray);
			redPackageMap.clear();
			int i = 1;
			for (int size = 0; size < array.size(); size++) {
				JSONObject json = array.getJSONObject(size);
				double chance = json.getDoubleValue("probability");
				int price = json.getIntValue("money");
				int count = ((int) (total * chance)) / 100;
				for (int k = i; k < i + count; k++) {
					redPackageMap.put(k, price);
				}
				i += count;
			}
		}
		public boolean judgeActiveBegin() {
			if (startTime == 0 || startTime == 0) {
				return false;
			}
			long nowTime = System.currentTimeMillis();
			if (startTime <= nowTime && nowTime <= endTime) {
				return true;
			}
			// 已经过了活动时间则重置下次活动开始时间
			if (endTime < nowTime) {
				initActiveTime();
			}
			return false;
		}
		//判断时间段内是否还有领取红包资格
		public boolean judgeAreaTimeIsRemain(){
			int remainCount = redPackageMap.size();
			if(remainCount == 0){
				return false;
			}
			long nowTime = System.currentTimeMillis();
			//计算当前时间到第几个区间段发红包了
			long sendTimes =  (long) Math.ceil((nowTime-startTime)/(areaTime*1.00));
			if((total - remainCount)<sendTimes*size){
				return true;
			}
			return false;
		}
		public int takeRedPackage() {
			if (!judgeActiveBegin()) {
				return 0;
			}
			if(!judgeAreaTimeIsRemain()){
				return 0;
			}
			if (redPackageMap.size() > 0) {
				int size = redPackageMap.size();
				int randNum = random.nextInt(size);
				int i = 0;
				for (Integer key : redPackageMap.keySet()) {
					if (i == randNum) {
						Integer value = redPackageMap.remove(key);
						if (value != null) {
							return value;
						}
					}
					i++;
				}
			}
			return 0;
		}

		public void initActiveTime() {
			ConcurrentHashMap<Integer, RedPackageActivityModel> redPackageRuleMap = RedPackageRuleDict.getInstance().getRedPackageRuleDictionary();
			if (redPackageRuleMap == null || redPackageRuleMap.size() == 0) {
				// 没有活动则活动时间都为0；
				startTime = 0;
				endTime = 0;
				return;
			}
			long nowTime = System.currentTimeMillis();// 当前时间
			try {
				for (int type : redPackageRuleMap.keySet()) {
					RedPackageActivityModel model = redPackageRuleMap.get(type);
					String activeTimeArea = model.getActivity_time(); // 活动时间区间
					if (type == this.type) {
						if (StringUtils.isBlank(activeTimeArea)) {
							startTime = 0;
							endTime = 0;
							return;
						}
						String[] daysTimeArea = activeTimeArea.split("\\|");
						for (String dayArea : daysTimeArea) {
							long startTime = dateFormat.parse(dayArea.split("\\~")[0]).getTime();
							long endTime = dateFormat.parse(dayArea.split("\\~")[1]).getTime();
							if (startTime >= nowTime || endTime >= nowTime) {// 活动的开始时间大于当前时间，则设置开始跟结束时间
								this.startTime = startTime;
								this.endTime = endTime;
								this.total = redPackageMap.size();//model.getPrize_count();//实际奖品数，而不是配置的奖品总数
								this.size = total/model.getLottery_num()+1;//model.getPrize_count()/model.getLottery_num()+1;
								this.areaTime = (endTime - startTime)/model.getLottery_num();
								
								break;
							}
						}
					}
				}
			} catch (Exception e) {
				startTime = 0;
				endTime = 0;
				return;
			}
		}

	}

	public ConcurrentHashMap<Integer, RedPackageActivity> getRedPackageTypeMap() {
		return redPackageTypeMap;
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
