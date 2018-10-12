package com.cai.dictionary;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.cai.common.constant.RedisConstant;
import com.cai.common.constant.Symbol;
import com.cai.common.define.EGameType;
import com.cai.common.domain.SysParamModel;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.common.util.StringUtil;
import com.cai.common.util.XYRange;
import com.cai.config.ClubCfg;
import com.cai.redis.service.RedisService;
import com.cai.service.ClubService;
import com.cai.utils.ClubRecordRepairUtil;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.google.common.primitives.Longs;

import javolution.util.FastMap;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 系统参数字典
 *
 * @author run
 */
public class SysParamServerDict {

	private Logger logger = LoggerFactory.getLogger(SysParamServerDict.class);

	/**
	 * 系统参数缓存 game_id(id,model)
	 */
	private FastMap<Integer, FastMap<Integer, SysParamModel>> sysParamModelDictionary;

	/**
	 * 单例
	 */
	private static SysParamServerDict instance;

	private volatile boolean isOpenLog = true;

	private volatile boolean isSendGroupRoom = false;

	/**
	 * 私有构造
	 */
	private SysParamServerDict() {
		sysParamModelDictionary = new FastMap<Integer, FastMap<Integer, SysParamModel>>();
	}

	/**
	 * 单例模式
	 *
	 * @return 字典单例
	 */
	public static SysParamServerDict getInstance() {
		if (null == instance) {
			instance = new SysParamServerDict();
		}

		return instance;
	}

	public boolean getIsOpenLog() {
		return isOpenLog;
	}

	public boolean getSendGroupRoom() {
		return isSendGroupRoom;
	}

	@SuppressWarnings("unused")
	private void is_open_log() {
		if (sysParamModelDictionary != null) {
			FastMap<Integer, SysParamModel> paramMap = getSysParamModelDictionaryByGameId(1);
			if (paramMap != null) {
				SysParamModel model = paramMap.get(1000);
				if (model != null) {
					isOpenLog = model.getVal4() == 1 ? true : false;
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void load() {
		PerformanceTimer timer = new PerformanceTimer();
		try {
			RedisService redisService = SpringService.getBean(RedisService.class);
			sysParamModelDictionary = redisService.hGet(RedisConstant.DICT, RedisConstant.DICT_SYSPARAM_SERVER, FastMap.class);

			parseClubCfg();
		} catch (Exception e) {
			logger.error("error", e);
		}
		logger.info("redis缓存加载加载字典sys_param" + timer.getStr());
	}

	public FastMap<Integer, SysParamModel> getSysParamModelDictionaryByGameId(int game_id) {
		FastMap<Integer, SysParamModel> dict = sysParamModelDictionary.get(game_id);
		return null == dict ? new FastMap<>() : dict;
	}

	public FastMap<Integer, FastMap<Integer, SysParamModel>> getSysParamModelDictionary() {
		return sysParamModelDictionary;
	}

	public void setSysParamModelDictionary(FastMap<Integer, FastMap<Integer, SysParamModel>> sysParamModelDictionary) {
		this.sysParamModelDictionary = sysParamModelDictionary;
	}

	/**
	 * 解析俱乐部配置
	 */
	private void parseClubCfg() {
		FastMap<Integer, SysParamModel> params = sysParamModelDictionary.get(EGameType.DT.getId());
		if (null == params) {
			logger.error("找不到gameId[{}]相关配置!", EGameType.DT.getId());
			return;
		}

		SysParamModel paramModel = params.get(2231);
		if (null != paramModel) {
			ClubCfg.get().setOwnerClubMax(paramModel.getVal1()).setClubRuleMax(paramModel.getVal2()).setRuleTableMax(paramModel.getVal3())
					.setClubMemberMax(paramModel.getVal4()).setOpen(paramModel.getVal5().intValue() == 1);
			if (StringUtils.isNotEmpty(paramModel.getStr1())) {
				List<Integer> arr = com.cai.common.util.StringUtil.toIntList(paramModel.getStr1(), Symbol.COLON);
				if (arr.size() > 0) {
					ClubCfg.get().setManagerMax(arr.get(0).intValue());
				}
				if (arr.size() > 1) {
					ClubCfg.get().setSyncGoldUpdateImmediate(arr.get(1).intValue() == 1);
				}
				if (arr.size() > 2) {
					ClubCfg.get().setCheckSeat(arr.get(2).intValue() == 1);
				}
			}

			if (!Strings.isNullOrEmpty(paramModel.getStr2()) && !"0".equals(paramModel.getStr2())) {
				ClubCfg.get().setTip(paramModel.getStr2());
			}
		} else {
			logger.error("############### 找不到id[2231]相关配置! ###################");
		}

		paramModel = params.get(2238);
		if (null != paramModel) {
			ClubCfg.get().setActivityOpen(paramModel.getVal1() == 1);
			ClubCfg.get().setActivityMinTime(paramModel.getVal2());
			ClubCfg.get().setActivityMaxTime(paramModel.getVal3());
			ClubCfg.get().setActivityLimit(paramModel.getVal4());
			ClubCfg.get().setShowHistoryTime(paramModel.getVal5());

			Set<Integer> gameTypeIndexs = Sets.newHashSet();
			if (StringUtils.isNotEmpty(paramModel.getStr1())) {
				List<Integer> arr = com.cai.common.util.StringUtil.toIntList(paramModel.getStr1(), Symbol.COLON);
				gameTypeIndexs.addAll(arr);

			}
			if (StringUtils.isNotEmpty(paramModel.getStr2())) {
				List<Integer> arr = com.cai.common.util.StringUtil.toIntList(paramModel.getStr2(), Symbol.COLON);
				gameTypeIndexs.addAll(arr);
			}
			ClubCfg.get().setRuleUpdateSubGameIds(gameTypeIndexs);
		}

		paramModel = params.get(2239);
		if (null != paramModel) {
			ClubCfg.get().setUseNewGetClubWay(paramModel.getVal1() == 1);
			ClubCfg.get().setSaveClubEventDB(paramModel.getVal2() == 1);
			ClubCfg.get().setUseOwnThreadSyncRoomStatus(paramModel.getVal3() == 1);
			ClubCfg.get().setIgnoreInviteTime(paramModel.getVal4());
			ClubCfg.get().setDelRedisCache(paramModel.getVal5() == 1);
			if (!Strings.isNullOrEmpty(paramModel.getStr1())) {
				ClubCfg.get().setDelRedisCacheTip(paramModel.getStr1());
			}

		}

		paramModel = params.get(2240);
		if (null != paramModel) {
			ClubCfg.get().setCheckRoomId(paramModel.getVal1() == 1);
			ClubCfg.get().setUseNewGetClubMemRecordWay(paramModel.getVal2() == 1);
			ClubCfg.get().setCanDelStartedRoom(paramModel.getVal3() == 1);
			ClubCfg.get().setAccessLimit(paramModel.getVal4() == 1);
		}
		ClubService.getInstance().clubCfgReload();
		logger.info("club cfg:{}", ClubCfg.get());

		paramModel = params.get(2233);
		if (paramModel != null) {
			isSendGroupRoom = paramModel.getVal5() == 1 ? true : false;
		}

		paramModel = params.get(2241);
		if (null != paramModel && paramModel.getVal1().intValue() == 1) {

			int clubId = paramModel.getVal2().intValue();
			if (clubId < 10000) {
				return;
			}

			Long accountId = Longs.tryParse(paramModel.getStr1());

			long account_id = 0L;
			if (null != accountId) {
				account_id = accountId.longValue();
			}
			String dayRange = paramModel.getStr2();

			try {

				if (Strings.isNullOrEmpty(dayRange) || "0".equals(dayRange) || "1".equals(dayRange)) {
					logger.error("修复俱乐部战绩相关，但格式不合适！ranger:{}", dayRange);
					return;
				}

				XYRange range = new XYRange(dayRange);
				if (range.getBegin() < 1 || range.getEnd() > 8) {
					logger.error("修复俱乐部战绩相关，但日期不合适！day:{}", range);
					return;
				}

				boolean fromBinlog = paramModel.getVal3() == 1;
				ClubRecordRepairUtil.repair(clubId, account_id, new XYRange(dayRange), fromBinlog);
			} catch (Exception e) {

				logger.error("range:{}", dayRange, e);
			}
		}

		// 俱乐部任务队列修复
		paramModel = params.get(2242);
		if (null != paramModel) {

			// 删除队列任务
			if (paramModel.getVal1().intValue() == 1) {
				int count = paramModel.getVal2();
				if (count > 20) {
					ClubService.getInstance().clubs.forEach((id, club) -> {
						if (club.taskQueue().size() > count) {
							club.taskQueue().clear();
						}
					});
				}
			}

			ClubCfg.get().setUseReserveWorker(paramModel.getVal3().intValue() == 1);
		}

		// 下线游戏
		paramModel = params.get(2243);
		if (null != paramModel) {

			Set<Integer> offLineGames = Sets.newHashSet();
			if (paramModel.getVal1().intValue() == 1) {
				try {
					offLineGames.addAll(com.cai.common.util.StringUtil.toIntSet(paramModel.getStr1(), Symbol.COMMA));
				} catch (Exception e) {
					e.printStackTrace();
					offLineGames.clear();
				}

				// 魔数，防止误操作
				if (paramModel.getVal2().intValue() == 937590235 && !offLineGames.isEmpty()) {
					ClubService.getInstance().delClubGameTypeIndex(offLineGames);
				}
			}
			if (!"0".equals(paramModel.getStr2())) {
				ClubCfg.get().setOfflineGameTip(paramModel.getStr2());
			}
			ClubCfg.get().setOfflineGames(offLineGames);

		}

		//
		paramModel = params.get(2244);
		if (null != paramModel) {
			ClubCfg.get().setUseOldRecordSaveWay(paramModel.getVal1().intValue() == 1);
			ClubCfg.get().setUseOldTireWay(paramModel.getVal2().intValue() == 1);
			ClubCfg.get().setUseOldRecordInsertWay(paramModel.getVal3().intValue() == 1);
			ClubCfg.get().setUseNewClubRuleRecordGetWay(paramModel.getVal4().intValue() == 1);
		}

		//
		boolean oldBanChatStatus = ClubCfg.get().isBanChat();
		boolean oldBanBulletinStatus = ClubCfg.get().isBanBulletin();
		boolean oldBanMarqueeStatus = ClubCfg.get().isBanMarquee();
		paramModel = params.get(2245);
		if (null != paramModel) {
			ClubCfg.get().setBanChat(paramModel.getVal1().intValue() == 1);
			ClubCfg.get().setBanBulletin(paramModel.getVal2().intValue() == 1);
			ClubCfg.get().setBanMarquee(paramModel.getVal3().intValue() == 1);

			ClubCfg.get().setClubMatchMinStartMinute(paramModel.getVal4().intValue());
			ClubCfg.get().setClubMatchWillStartMinute(paramModel.getVal5().intValue());

			if (!"0".equals(paramModel.getStr1())) {
				ClubCfg.get().setClubMatchLogicIndexs(paramModel.getStr1());
				List<Integer> logicList = new ArrayList<>();
				Set<Integer> tmpList = StringUtil.toIntSet(paramModel.getStr1(), Symbol.COMMA);
				if (tmpList != null) {
					logicList.addAll(tmpList);
				}
				ClubCfg.get().setClubMatchLogicList(logicList);
			}
		}
		if (oldBanChatStatus != ClubCfg.get().isBanChat() || oldBanBulletinStatus != ClubCfg.get().isBanBulletin() || oldBanMarqueeStatus != ClubCfg
				.get().isBanMarquee()) {
			ClubService.getInstance().notifyBanSwitch();
		}

		paramModel = params.get(2246);
		if (null != paramModel) {
			if (paramModel.getVal1() == 1) {
				Set<Long> accountIDs = com.cai.common.util.StringUtil.toLongSet(paramModel.getStr1(), Symbol.COMMA);
				if (null != accountIDs && !accountIDs.isEmpty()) {
					ClubService.getInstance().becomeObserver(paramModel.getVal2(), accountIDs, paramModel.getVal3() == 1);
				}
			}
		}

		paramModel = params.get(2247);
		if (null != paramModel) {
			ClubCfg.get().setDefendCheating(paramModel.getVal1() == 1);
			if (paramModel.getVal2() > 0) {
				ClubCfg.get().setClubMatchEnrollTimeLimit(paramModel.getVal2());
			}
			if (paramModel.getVal3() > 0) {
				ClubCfg.get().setClubMatchSetEnrollTimeLimit(paramModel.getVal3());
			}
			if (paramModel.getVal4() > 0) {
				ClubCfg.get().setAutoKickoutPlayerTime(paramModel.getVal4());
			}
			if (paramModel.getVal5() > 0) {
				ClubCfg.get().setPlayerEnterTableBanTime(paramModel.getVal5());
			}
		}

		paramModel = params.get(2255);
		if (null != paramModel) {
			if (paramModel.getVal1() > 0) {
				ClubCfg.get().setClubWelfareAotoLotteryTime(paramModel.getVal1());
			}
			if (paramModel.getVal2() > 0) {
				ClubCfg.get().setClubWelfareDailyGetCount(paramModel.getVal2());
			}
		}
	}

	/**
	 * 替换提示语中的闲逸豆为豆(玩一局需求)
	 *
	 * @param msg
	 * @return
	 */
	public String replaceGoldTipsWord(String msg) {
		FastMap<Integer, SysParamModel> params = sysParamModelDictionary.get(EGameType.DT.getId());
		if (null == params) {
			return msg;
		}
		SysParamModel paramModel = params.get(2400);
		if (null == paramModel) {
			return msg;
		}
		if (paramModel.getVal1() == 0) {
			return msg;
		}
		if (StringUtils.isEmpty(paramModel.getStr1()) || StringUtils.isEmpty(paramModel.getStr2())) {
			return msg;
		}
		return msg.replace(paramModel.getStr1(), paramModel.getStr2());
	}

}
