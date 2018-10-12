package com.cai.rmi.handler;

import java.util.HashMap;

import com.cai.common.constant.RMICmd;
import com.cai.common.domain.zhuzhou.AccountZZPromoterModel;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.service.ZZPromoterService;

/**
 * 
 *
 * @author tang date: 2018年09月26日 <br/>
 */
@IRmi(cmd = RMICmd.OPERATE_PROMOTER, desc = "株洲麻将协会推荐人修改删除添加操作")
public final class ZZPromoterHandler extends IRMIHandler<HashMap<String, String>, Integer> {

	@Override
	protected Integer execute(HashMap<String, String> map) {
		String accountId = map.get("accountId");
		String targetId = map.get("targetId");
		String type = map.get("type");
		boolean result = false;
		if (type.equals("1")) {// 添加
			result = ZZPromoterService.getInstance().addPromoterObject(Long.parseLong(accountId), Long.parseLong(targetId));
		} else if (type.equals("2")) {// 删除
			ZZPromoterService.getInstance().removePromoterObject(Long.parseLong(accountId), Long.parseLong(targetId));
			result = true;
		} else if (type.equals("3")) {// 更改
			ZZPromoterService.getInstance().removePromoterObject(Long.parseLong(accountId), Long.parseLong(targetId));
			result = ZZPromoterService.getInstance().addPromoterObject(Long.parseLong(accountId), Long.parseLong(targetId));
		} else if (type.equals("4")) {
			String money = map.get("money");
			int flag = ZZPromoterService.getInstance().drawCash(Long.parseLong(accountId), Integer.parseInt(money), "后台操作");
			result = flag == 1 ? true : false;
		} else if (type.equals("5")) {// 是否协会推广的人员
			AccountZZPromoterModel model = ZZPromoterService.getInstance().getAccountZZPromoterModel(Long.parseLong(targetId));
			result = model != null ? true : false;
		} else if (type.equals("6")) {// 查询协会推广员余额
			long money = ZZPromoterService.getInstance().getRemainMoney(Long.parseLong(accountId));
			return (int) money;
		}
		return result ? 1 : -1;
	}

}
