/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.cai.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import com.cai.common.domain.AccountPushModel;
import com.cai.common.domain.Event;
import com.cai.common.util.SpringService;
import com.cai.core.MonitorEvent;
import com.cai.domain.Session;

/**
 *
 * 红包
 * 
 * @author tang <a style="color:red;">推送id缓存 </a>
 */
public class AccountPushServiceImp extends AbstractService {

	private static int JG_TYPE = 1;
	private static int IOS_TYPE = 2;
	private static int XG_TYPE = 3;

	private AccountPushServiceImp() {
	}

	private static AccountPushServiceImp instance = null;

	private static Map<Integer, Map<Long, AccountPushModel>> pushMap = null;

	public static AccountPushServiceImp getInstance() {
		if (null == instance) {
			instance = new AccountPushServiceImp();
			pushMap = new HashMap<>();
		}
		return instance;
	}

	public void loadPushMap() {
		try {
			PublicService publicService = SpringService.getBean(PublicService.class);
			List<AccountPushModel> list = publicService.getPublicDAO().getAccountPushModelList();
			Map<Long, AccountPushModel> type1Map = new HashMap<>();
			Map<Long, AccountPushModel> type2Map = new HashMap<>();
			Map<Long, AccountPushModel> type3Map = new HashMap<>();
			pushMap.put(1, type1Map);
			pushMap.put(2, type2Map);
			pushMap.put(3, type3Map);
			if (list.size() > 0) {
				for (AccountPushModel model : list) {
					if (model.getPlat() == JG_TYPE) {
						type1Map.put(model.getAccount_id(), model);
					} else if (model.getPlat() == IOS_TYPE) {
						type2Map.put(model.getAccount_id(), model);
					} else if (model.getPlat() == XG_TYPE) {
						type3Map.put(model.getAccount_id(), model);
					}
				}
			}
			logger.info("加载推送信息成功  loadPushMap success!");
		} catch (Exception e) {
			logger.error("加载推送信息失败  loadPushMap error!", e);
		}

	}

	public void addAccountPushModel(AccountPushModel model) {
		Map<Long, AccountPushModel> map = pushMap.get(model.getPlat());
		if (map != null) {
			AccountPushModel am = map.get(model.getAccount_id());
			if (am == null) {
				map.put(model.getAccount_id(), model);
				PublicService publicService = SpringService.getBean(PublicService.class);
				// 没有数据则插入
				publicService.getPublicDAO().insertAccountPushModel(model);
			} else {
				// 判断数据有没有变化
				if (!am.getEquipment_id().equals(model.getEquipment_id())) {
					try {
						am.setEquipment_id(model.getEquipment_id());
						PublicService publicService = SpringService.getBean(PublicService.class);
						publicService.getPublicDAO().updateAccountPushModel(am);
					} catch (Exception e) {
						logger.error(model.getAccount_id() + "更新数据库失败" + model.getEquipment_id(), e);
					}

				}
			}
		}
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
