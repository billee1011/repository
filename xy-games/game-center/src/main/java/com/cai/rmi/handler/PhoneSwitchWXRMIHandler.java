package com.cai.rmi.handler;

import java.util.Objects;

import com.cai.common.constant.RMICmd;
import com.cai.common.define.EPtType;
import com.cai.common.define.XYCode;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountModel;
import com.cai.common.domain.AccountWeixinModel;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.common.util.MobileUtil;
import com.cai.common.util.Pair;
import com.cai.common.util.SpringService;
import com.cai.service.PublicService;
import com.cai.service.PublicServiceImpl;

/**
 * 
 * 
 *
 * @author wu_ch date: 2017年11月20日 下午5:48:24 <br/>
 */
@IRmi(cmd = RMICmd.PHONE_SWITCH_WX, desc = "切换微信息")
public final class PhoneSwitchWXRMIHandler extends IRMIHandler<AccountWeixinModel, Pair<Integer, String>> {

	@Override
	public Pair<Integer, String> execute(AccountWeixinModel vo) {

		Account account = PublicServiceImpl.getInstance().getAccount(vo.getAccount_id());
		AccountModel accountModel = account.getAccountModel();

		if (MobileUtil.isMobileNull(accountModel.getMobile_phone())) {
			return Pair.of(XYCode.FAIL, "你的帐号还没绑定手机，绑定手机后才能进行切换微信操作!");
		}

		final AccountWeixinModel wxModel = account.getAccountWeixinModel();
		if (null == wxModel) {
			return Pair.of(XYCode.FAIL, "没有对应的微信模块!");
		}

		if (Objects.equals(wxModel.getUnionid(), vo.getUnionid())) {
			return Pair.of(XYCode.FAIL, "相同微信，不需要切换!");
		}

		String oldUnid = wxModel.getUnionid();
		String oldAccountName = account.getAccount_name();

		String newAccounName = EPtType.WX.getId() + "_" + vo.getUnionid();

		account.getAccountModel().setAccount_name(newAccounName);
		wxModel.setAccess_token(vo.getAccess_token());
		wxModel.setRefresh_token(vo.getRefresh_token());
		wxModel.setOpenid(vo.getOpenid());
		wxModel.setScope(vo.getScope());
		wxModel.setUnionid(vo.getUnionid());
		wxModel.setNickname(vo.getNickname());
		wxModel.setSex(vo.getSex());
		wxModel.setProvince(vo.getProvince());
		wxModel.setCity(vo.getCity());
		wxModel.setCountry(vo.getCountry());
		wxModel.setHeadimgurl(vo.getHeadimgurl());
		wxModel.setPrivilege(vo.getPrivilege());
		wxModel.setLast_flush_time(vo.getLast_flush_time());
		wxModel.setSelf_token(vo.getSelf_token());
		wxModel.setLast_false_self_token(vo.getLast_false_self_token());
		account.getAccountModel().setNeedDB(true);
		wxModel.setNeedDB(true);

		PublicServiceImpl.getInstance().switchWX(oldUnid, oldAccountName, account);
		int r = SpringService.getBean(PublicService.class).getPublicDAO().updateAccountWeixinModel(wxModel);
		if (r > 0) {
			return Pair.of(XYCode.SUCCESS, "微信切换成功，请用新微信进行登陆!");
		}
		return Pair.of(XYCode.FAIL, "微信切换失败!");
	}
}
