package com.cai.util;

import java.util.Date;

import org.apache.commons.lang.StringUtils;

import com.cai.common.domain.Account;
import com.cai.common.domain.AccountModel;
import com.cai.common.domain.AccountSimple;
import com.cai.common.domain.PlayerSdkViewVo;
import com.cai.common.domain.PlayerViewVO;
import com.cai.service.PublicServiceImpl;

public class AccountUtil {

	public static PlayerViewVO getVo(Account account) {
		AccountModel accountModel = account.getAccountModel();

		AccountSimple simple = PublicServiceImpl.getInstance().getAccountSimpe(accountModel.getAccount_id());
		PlayerViewVO vo = new PlayerViewVO();
		vo.setAccountId(account.getAccount_id());
		vo.setGold(accountModel.getGold());
		vo.setHead(null != simple ? simple.getIcon() : "");
		vo.setMoney(accountModel.getMoney());
		vo.setNickName(account.getNickName());
		vo.setSignature(StringUtils.isEmpty(accountModel.getSignature()) ? "该用户很懒，什么也没写" : accountModel.getSignature());
		vo.setCreate_time(accountModel.getCreate_time());
		vo.setSex(account.getSex());
		vo.setVipLv(0);
		vo.setRecommendId(accountModel.getRecommend_id());
		vo.setPhoneNum(accountModel.getMobile_phone());
		Date date = accountModel.getCoin_play_time();
		if (date == null) {
			date = new Date();
			accountModel.setCoin_play_time(date);
			accountModel.setNeedDB(true);
		}

		vo.setCoinPlayTime(date);
		vo.setPayAccount(accountModel.getHistory_pay_gold() > 0 ? true : false);
		return vo;
	}
	
	public static PlayerSdkViewVo getSdkVo(Account account) {
		AccountModel accountModel = account.getAccountModel();

		AccountSimple simple = PublicServiceImpl.getInstance().getAccountSimpe(accountModel.getAccount_id());
		PlayerSdkViewVo vo = new PlayerSdkViewVo();
		vo.setAccountId(account.getAccount_id());
		vo.setHead(null != simple ? simple.getIcon() : "");
		vo.setNickName(account.getNickName());
		vo.setSignature(StringUtils.isEmpty(accountModel.getSignature()) ? "该用户很懒，什么也没写" : accountModel.getSignature());
		vo.setCreate_time(accountModel.getCreate_time());
		vo.setSex(account.getSex());
		Date date = accountModel.getCoin_play_time();
		if (date == null) {
			date = new Date();
			accountModel.setCoin_play_time(date);
			accountModel.setNeedDB(true);
		}
		vo.setDiamond(accountModel.getDiamond());
		return vo;
	}

}
