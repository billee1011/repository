package com.lingyu.game.service.currency.impl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.lingyu.common.constant.CurrencyConstant;
import com.lingyu.common.constant.CurrencyConstant.CurrencyCostType;
import com.lingyu.common.constant.CurrencyConstant.CurrencyType;
import com.lingyu.common.constant.OperateConstant.OperateType;
import com.lingyu.common.core.ErrorCode;
import com.lingyu.common.core.ServiceException;
import com.lingyu.common.entity.Role;
import com.lingyu.common.io.MsgType;
import com.lingyu.game.RouteManager;
import com.lingyu.game.service.currency.IMoneyOperation;
import com.lingyu.game.service.currency.MoneyResponse;
import com.lingyu.game.service.mahjong.MahjongConstant;
import com.lingyu.game.service.role.RoleManager;
import com.lingyu.game.service.role.RoleRepository;

/**
 * 钻石操作管理
 *
 * @author Wang Shuguang
 *
 */
@Component
public class DiamondOperation implements IMoneyOperation {
	private static final Logger logger = LogManager.getLogger(DiamondOperation.class);

	@Autowired
	private RoleManager roleManager;
	@Autowired
	private RoleRepository roleRepository;
	@Autowired
	private RouteManager routeManager;

	@Override
	public MoneyResponse incr(long roleId, long ammount, boolean notify, boolean needAntiAddiction,
	        OperateType reason) {
		Role role = roleManager.getRole(roleId);
		if (ammount > Integer.MAX_VALUE) {
			throw new ServiceException("diamond: roleId={}, incrValue={}, incrDiamondToMuch, 变化金额超过Int上限", roleId,
			        ammount);
		}

		// 计算钻石总和
		long beforeChangedAmount = role.getDiamond();
		long diamond = ammount + role.getDiamond();
		if (diamond > Integer.MAX_VALUE) {
			throw new ServiceException("diamond: roleId={}, incrValue={}, incrDiamondToMuch, 变化后金额超过Int上限", roleId,
			        ammount);
		}

		// 超出了最大值(这就是人民币, 这个不应该有上限的)
		if (diamond > CurrencyConstant.DIAMOND_MAX) {
			logger.warn("diamond: roleId={}, incrValue={}, withoutMaxDiamond={}, withMaxDiamond={}, 超出了策划规定的最大值",
			        roleId, ammount, diamond, CurrencyConstant.DIAMOND_MAX);
			diamond = CurrencyConstant.DIAMOND_MAX;
		}
		role.setDiamond(diamond);
		roleRepository.cacheUpdate(role);

		// 发送消息
		if (notify) {
			JSONObject result = new JSONObject();
			result.put(ErrorCode.RESULT, ErrorCode.EC_OK);
			result.put(MahjongConstant.CLIENT_DATA,
			        new Object[] { CurrencyType.DIAMOND_NEW.getId(), role.getDiamond() });
			routeManager.relayMsg(roleId, MsgType.DIAMOND_CHANGE, result);
		}

		if (role != null) {
			logger.info("diamond: userId={}, roleId={}, incrValue={}, before={}, after={}, reason={}, incrDiamond",
			        role.getUserId(), roleId, ammount, beforeChangedAmount, diamond, reason);
		}

		return new MoneyResponse(true, CurrencyType.DIAMOND_NEW, ammount, beforeChangedAmount, role.getDiamond());
	}

	@Override
	public MoneyResponse decr(long roleId, long ammount, boolean notify, OperateType reason,
	        CurrencyCostType costType) {
		if (ammount > Integer.MAX_VALUE) {
			throw new ServiceException("diamond: roleId={}, incrValue={}, decrDiamond, 变化金额超过Int上限", roleId, ammount);
		}

		if (costType.getId() == CurrencyCostType.ONLY.getId()) {
			return decrDiamond(roleId, (int) ammount, notify, reason.getId());
		} else if (costType.getId() == CurrencyCostType.CURRENT_FIRST.getId()) {
			return decrDiamondFirst(roleId, (int) ammount, notify, reason.getId());
		} else {
			throw new ServiceException("扣除diamond存在没有处理的CurrencyCostType类型：[{}]", costType.getId());
		}
	}

	@Override
	public boolean checkDecr(long roleId, long ammount, CurrencyCostType costType) {
		if (ammount > Integer.MAX_VALUE) {
			throw new ServiceException("diamond:roleId={}, incrValue={}, decrDiamond, 变化金额超过Int上限", roleId, ammount);
		}

		if (costType.getId() == CurrencyCostType.ONLY.getId()) {
			return checkDecrDiamond(roleId, (int) ammount);
		} else if (costType.getId() == CurrencyCostType.CURRENT_FIRST.getId()) {
			return checkDecrDiamondFirst(roleId, (int) ammount);
		} else {
			throw new ServiceException("扣除diamond存在没有处理的CurrencyCostType类型：[{}]", costType.getId());
		}
	}

	@Override
	public CurrencyType getMoneyType() {
		return CurrencyType.DIAMOND_NEW;
	}

	@Override
	public JSONObject getMoneyNotEnoughErrorCode() {
		JSONObject result = new JSONObject();
		result.put(ErrorCode.RESULT, ErrorCode.FAILED);
		result.put(ErrorCode.CODE, ErrorCode.DIAMOND_NOT_ENOUGH);
		return result;
	}

	/** 消耗钻石 */
	public MoneyResponse decrDiamond(long roleId, int decrValue, boolean notify, int reason) {
		Role role = roleManager.getRole(roleId);
		if (role.getDiamond() >= decrValue) {
			long beforeChangedAmount = role.getDiamond();
			long diamond = role.getDiamond() - decrValue;
			role.setDiamond(diamond);
			roleRepository.cacheUpdate(role);

			if (notify) {
				JSONObject result = new JSONObject();
				result.put(ErrorCode.RESULT, ErrorCode.EC_OK);
				result.put(MahjongConstant.CLIENT_DATA,
				        new Object[] { CurrencyType.DIAMOND_NEW.getId(), role.getDiamond() });
				routeManager.relayMsg(roleId, MsgType.DIAMOND_CHANGE, result);
			}

			if (role != null) {
				logger.info("diamond: userId={}, roleId={}, decrValue={}, before={}, after={}, reason={}, decrDiamond",
				        role.getUserId(), roleId, decrValue, beforeChangedAmount, diamond, reason);
			}

			return new MoneyResponse(true, CurrencyType.DIAMOND_NEW, decrValue, beforeChangedAmount, role.getDiamond());
		}
		return new MoneyResponse(false, CurrencyType.DIAMOND_NEW, decrValue, 0, 0);
	}

	/** 优先消耗钻石 */
	MoneyResponse decrDiamondFirst(long roleId, int decrValue, boolean notify, int reason) {
		// 有一些业务需要扣0个钻石
		if (decrValue == 0) {
			return new MoneyResponse(true);
		}

		Role role = roleManager.getRole(roleId);
		synchronized (roleManager.getLock(roleId)) {
			// 验证货币是否足够
			if (!checkDecrDiamondFirst(roleId, decrValue)) {
				return new MoneyResponse(false);
			}

			long roleDiamond = role.getDiamond();

			// 计算需要扣除的钻石和绑定钻石的金额
			int decrDiamondValue = 0; // 扣除钻石金额
			if (roleDiamond >= decrValue) {
				decrDiamondValue = decrValue;
			}

			MoneyResponse moneyResponse = new MoneyResponse(true);
			// 扣除钻石
			if (decrDiamondValue > 0) {
				decrDiamond(roleId, decrDiamondValue, notify, reason);
				moneyResponse.addMoneyChangeInfo(CurrencyType.DIAMOND_NEW, decrDiamondValue, roleDiamond,
				        role.getDiamond());
			}

			roleRepository.cacheUpdate(role);
			return moneyResponse;
		}
	}

	/** 验证钻石是否够 */
	boolean checkDecrDiamond(long roleId, int decrValue) {
		Role role = roleManager.getRole(roleId);
		return role.getDiamond() >= decrValue;
	}

	/**
	 * 验证钻石是否够
	 *
	 * @param roleId
	 * @param decrValue
	 * @return
	 */
	boolean checkDecrDiamondFirst(long roleId, int decrValue) {
		Role role = roleManager.getRole(roleId);
		return role.getDiamond() >= decrValue;
	}
}
