/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.rmi.handler;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Set;
import java.util.function.Consumer;

import com.cai.common.constant.RMICmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.define.EWealthCategory;
import com.cai.common.rmi.IRMIHandler;
import com.cai.common.rmi.IRmi;
import com.cai.common.rmi.vo.AccountWealthVo;
import com.cai.common.util.Pair;
import com.cai.config.ClubCfg;
import com.cai.constant.Club;
import com.cai.service.ClubService;
import com.cai.utils.Utils;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import protobuf.clazz.ClubMsgProto.ClubCreatorGoldUpdateProto;

/**
 * 
 * 
 *
 * @author wu_hc date: 2017年12月25日 上午11:58:53 <br/>
 */
@IRmi(cmd = RMICmd.ACCOUNT_WEALTH_UPDATE, desc = "个人财富刷新")
public final class ClubCreatorGoldUpdateRMIHandler extends IRMIHandler<AccountWealthVo, Void> {

	static final EnumMap<EWealthCategory, Consumer<AccountWealthVo>> func = Maps.newEnumMap(EWealthCategory.class);

	static {
		func.put(EWealthCategory.GOLD, ClubCreatorGoldUpdateRMIHandler::goldUpdate);
		func.put(EWealthCategory.EXCLUSIVE_GOLD, ClubCreatorGoldUpdateRMIHandler::exclusiveGoldUpdate);
		func.put(EWealthCategory.MONEY, ClubCreatorGoldUpdateRMIHandler::moneyUpdate);
	}

	@Override
	public Void execute(AccountWealthVo vo) {

		Consumer<AccountWealthVo> consumer = func.get(vo.getCategory());
		if (null != consumer) {
			consumer.accept(vo);
		} else {
			logger.error("财富类型[{}]未设置监听并处理!!!", vo.getCategory());
		}
		return null;
	}

	/**
	 * 房卡刷新
	 * 
	 * @param vo
	 */
	private static void goldUpdate(AccountWealthVo vo) {

		Club.wealth.put(Pair.of(vo.getAccountId(), vo.getCategory()), vo.getNewValue());

		// 实时同步专属豆
		if (ClubCfg.get().isSyncGoldUpdateImmediate()) {

			final Set<Long> ids = Sets.newHashSet();
			Collection<Club> clubs = ClubService.getInstance().getMyCreateClub(vo.getAccountId());
			if (!clubs.isEmpty()) {
				clubs.forEach(club -> {
					if (vo.getCategory() == EWealthCategory.GOLD) {
						ids.addAll(club.allMemberIds());
					}
				});
				ClubCreatorGoldUpdateProto.Builder b = ClubCreatorGoldUpdateProto.newBuilder();
				b.setCategory(vo.getCategory().category());
				b.setValue(vo.getNewValue());
				b.setAccountId(vo.getAccountId());
				Utils.sendClient(ids, S2CCmd.CLUB_OWENER_GOLD_UPDATE, b);
			}
		}

	}

	/**
	 * 俱乐部专属豆刷新
	 * 
	 * @param vo
	 */
	private static void exclusiveGoldUpdate(AccountWealthVo vo) {

	}

	/**
	 * 金币刷新
	 * 
	 * @param vo
	 */
	private static void moneyUpdate(AccountWealthVo vo) {

	}
}
