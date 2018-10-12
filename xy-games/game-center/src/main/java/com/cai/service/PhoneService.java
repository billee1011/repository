/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
/**
 * 
 * 
 * @author wu_hc date: 2017年11月30日 下午5:02:34 <br/>
 */
import java.util.SortedMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.cai.common.define.XYCode;
import com.cai.common.domain.AccountMobileModel;
import com.cai.common.domain.Event;
import com.cai.common.util.MobileUtil;
import com.cai.common.util.SpringService;
import com.cai.core.MonitorEvent;
import com.cai.domain.Session;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * 
 * 帐号与手机绑定服务相关
 *
 * @author wu_hc date: 2017年12月4日 上午11:05:26 <br/>
 */
public class PhoneService extends AbstractService {

	private static PhoneService instance = new PhoneService();

	private final ConcurrentMap<Long, AccountMobileModel> accountMap = Maps.newConcurrentMap();
	private final ConcurrentMap<String, AccountMobileModel> mobileMap = Maps.newConcurrentMap();

	private final List<Integer> identifyCodes = Lists.newArrayListWithExpectedSize(6500);

	private final AtomicInteger idx = new AtomicInteger(0);

	private PhoneService() {
	}

	public static PhoneService getInstance() {
		return instance;
	}

	@Override
	protected void startService() {

		PublicService publicService = SpringService.getBean(PublicService.class);
		List<AccountMobileModel> accountPhones = publicService.getPublicDAO().getAccountMobileList();
		accountPhones.forEach((mobileModel) -> {
			// 兼容旧的
			String o = mobileModel.getMobile_phone();
			if (MobileUtil.isMobileNull(o)) {
				return;
			}
			accountMap.put(mobileModel.getAccount_id(), mobileModel);
			mobileMap.put(mobileModel.getMobile_phone(), mobileModel);
		});

		for (int i = 1500; i < 8000; i++) {
			identifyCodes.add(i);
		}
		Collections.shuffle(identifyCodes);
	}

	/**
	 * 随机验证码
	 * 
	 * @return
	 */
	public synchronized int randomIdentifyCode() {
		return identifyCodes.get(Math.abs(idx.getAndIncrement() % identifyCodes.size()));
	}

	/**
	 * 
	 * @param accountId
	 * @return
	 */
	public Optional<AccountMobileModel> getPhoneModel(long accountId) {
		return Optional.ofNullable(accountMap.get(accountId));
	}

	/**
	 * 
	 * @param accountId
	 * @return
	 */
	public Optional<AccountMobileModel> getPhoneModelByMobileNum(String mobile) {
		return Optional.ofNullable(mobileMap.get(mobile));
	}

	public void bind(AccountMobileModel model) {
		accountMap.put(model.getAccount_id(), model);
		mobileMap.put(model.getMobile_phone(), model);
	}

	public void unBind(long acountId) {
		AccountMobileModel model = accountMap.remove(acountId);
		if (null != model) {
			mobileMap.remove(model.getMobile_phone());
		}
	}

	/**
	 * 
	 * @param model
	 * @return
	 */
	public synchronized int certification(long accountId, String realName, String realId) {
		Optional<AccountMobileModel> opt = getPhoneModel(accountId);
		if (opt.isPresent()) {
		} else {
		}
		return XYCode.SUCCESS;
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
