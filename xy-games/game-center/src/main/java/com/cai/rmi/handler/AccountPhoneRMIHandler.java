package com.cai.rmi.handler;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import com.cai.common.constant.RMICmd;
import com.cai.common.define.EGameType;
import com.cai.common.define.EGoldOperateType;
import com.cai.common.define.IPhoneOperateType;
import com.cai.common.define.XYCode;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountMobileModel;
import com.cai.common.domain.AccountModel;
import com.cai.common.domain.SysParamModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.common.rmi.vo.UserPhoneRMIVo;
import com.cai.common.util.MobileUtil;
import com.cai.common.util.Pair;
import com.cai.common.util.SpringService;
import com.cai.common.util.TimeUtil;
import com.cai.dictionary.SysParamDict;
import com.cai.dictionary.SysParamServerDict;
import com.cai.service.PhoneService;
import com.cai.service.PublicServiceImpl;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;

/**
 * 
 *
 * @author wu_hc date: 2017年11月20日 下午5:48:24 <br/>
 */
@IRmi(cmd = RMICmd.ACCOUNT_PHONE, desc = "手机绑定相关")
public final class AccountPhoneRMIHandler extends IRMIHandler<UserPhoneRMIVo, Pair<Integer, String>> {

	/**
	 * 函数引用
	 */
	static Map<Integer, BiFunction<UserPhoneRMIVo, AccountModel, Pair<Integer, String>>> func = Maps.newHashMap();
	static {
		func.put(IPhoneOperateType.BIND, AccountPhoneRMIHandler::bind);
		func.put(IPhoneOperateType.UN_BIND, AccountPhoneRMIHandler::unBind);
		func.put(IPhoneOperateType.BIND_INFO, AccountPhoneRMIHandler::bindStatus);
	}

	@Override
	protected Pair<Integer, String> execute(UserPhoneRMIVo vo) {
		if (!func.containsKey(vo.getType())) {
			return Pair.of(XYCode.FAIL, "不存在操作类型[" + vo.getType() + "]!");
		}
		if (vo.getAccountId() == 0L) {
			return func.get(vo.getType()).apply(vo, null);
		} else {
			Account account = PublicServiceImpl.getInstance().getAccount(vo.getAccountId());
			AccountModel accountModel = account.getAccountModel();
			return func.get(vo.getType()).apply(vo, accountModel);
		}

	}

	/**
	 * 
	 * @param vo
	 * @param opt
	 * @return
	 */
	private static Pair<Integer, String> bind(final UserPhoneRMIVo vo, AccountModel model) {

		if (!MobileUtil.isMobileNull(model.getMobile_phone())) {
			return Pair.of(XYCode.FAIL, " 帐号已经绑定过！");
		}
		Optional<AccountMobileModel> phoneOpt = PhoneService.getInstance().getPhoneModelByMobileNum(vo.getPhone());
		if (phoneOpt.isPresent()) {
			return Pair.of(XYCode.FAIL, " 手机号已经绑定过！");
		}
		model.setMobile_phone(vo.getPhone());
		if (model.getBinded_mobile() == 0) {
			model.setBinded_mobile(1);
			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
			SysParamModel sysParamModel8019 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(6).get(8019);
			int addGold = 10;
			if (sysParamModel8019 != null) {
				addGold = sysParamModel8019.getVal1();
			}
			centerRMIServer.addAccountGold(model.getAccount_id(), addGold, true, EGoldOperateType.FIRST_BIND_MOBILE.getTransDesc(),
					EGoldOperateType.FIRST_BIND_MOBILE);
		}
		model.setNeedDB(true);
		AccountMobileModel mobileModel = new AccountMobileModel();
		mobileModel.setAccount_id(model.getAccount_id());
		mobileModel.setMobile_phone(vo.getPhone());
		mobileModel.setLastBindTime(System.currentTimeMillis());
		PhoneService.getInstance().bind(mobileModel);

		return Pair.of(XYCode.SUCCESS, " 绑定成功");
	}

	/*
	 * 
	 * @param vo
	 * 
	 * @param opt
	 * 
	 * @return
	 */
	private static Pair<Integer, String> bindStatus(final UserPhoneRMIVo vo, AccountModel model) {

		Optional<AccountMobileModel> phoneOpt = PhoneService.getInstance().getPhoneModelByMobileNum(vo.getPhone());
		if (phoneOpt.isPresent()) {
			return Pair.of(XYCode.SUCCESS, Long.toString(phoneOpt.get().getAccount_id()));
		}
		return Pair.of(XYCode.FAIL, "该手机号没有邦定！");
	}

	/**
	 * 
	 * @param vo
	 * @param opt
	 * @return
	 */
	private static Pair<Integer, String> unBind(final UserPhoneRMIVo vo, AccountModel model) {

		if (Strings.isNullOrEmpty(model.getMobile_phone())) {
			return Pair.of(XYCode.FAIL, " 没有邦定记录，不需要解绑！");
		}
		Optional<AccountMobileModel> opt = PhoneService.getInstance().getPhoneModel(model.getAccount_id());
		if (!opt.isPresent()) {
			return Pair.of(XYCode.FAIL, " 没有邦定记录，不需要解绑!！");
		}

		SysParamModel phoneSvrModels = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(EGameType.DT.getId()).get(2500);

		int time = 24;
		String tip = null;
		if (null != phoneSvrModels) {
			time = phoneSvrModels.getVal1();
			tip = phoneSvrModels.getStr1();
		} else {
			tip = "手机绑定未满24小时，不能进行解绑操作!";
		}
		if (System.currentTimeMillis() - opt.get().getLastBindTime() <= TimeUtil.HOUR * time) {
			return Pair.of(XYCode.FAIL, tip);
		}
		model.setMobile_phone(null);
		model.setNeedDB(true);
		PhoneService.getInstance().unBind(vo.getAccountId());
		return Pair.of(XYCode.SUCCESS, " 解绑定成功！");
	}

}
