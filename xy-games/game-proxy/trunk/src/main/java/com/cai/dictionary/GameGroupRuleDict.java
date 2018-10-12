/**
 * 
 */
package com.cai.dictionary;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.druid.util.StringUtils;
import com.cai.common.constant.RedisConstant;
import com.cai.common.constant.S2CCmd;
import com.cai.common.domain.GameGroupRuleModel;
import com.cai.common.domain.GameGroups;
import com.cai.common.domain.SysParamModel;
import com.cai.common.util.PBUtil;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;
import com.cai.service.C2SSessionService;

import protobuf.clazz.Common.CommonAppConfigProto;
import protobuf.clazz.Common.CommonGameConfigProto;
import protobuf.clazz.Common.GameRuleResetResponse;
import protobuf.clazz.Common.RuleConfig;
import protobuf.clazz.Common.RuleGroupConfig;
import protobuf.clazz.Protocol.Response;

/**
 * 
 */
public class GameGroupRuleDict {

	private Logger logger = LoggerFactory.getLogger(GameGroupRuleDict.class);

	// 每款子游戏的数据
	private Map<Integer, GameGroups> groups;

	// 根据appid 存玩法规则
	private Map<Integer, CommonAppConfigProto.Builder> groupProtos;

	// 根据gameid 存玩法规则
	private Map<Integer, CommonGameConfigProto> groupSubGamneProtos;

	/**
	 * 单例
	 */
	private static GameGroupRuleDict instance;

	/**
	 * 私有构造
	 */
	private GameGroupRuleDict() {
		groups = new ConcurrentHashMap<Integer, GameGroups>();
		groupProtos = new ConcurrentHashMap<>();
		groupSubGamneProtos = new ConcurrentHashMap<Integer, CommonGameConfigProto>();
	}

	/**
	 * 单例模式
	 *
	 * @return 字典单例
	 */
	public static GameGroupRuleDict getInstance() {
		if (null == instance) {
			instance = new GameGroupRuleDict();
		}

		return instance;
	}

	@SuppressWarnings("unchecked")
	public void load() {
		PerformanceTimer timer = new PerformanceTimer();
		try {
			RedisService redisService = SpringService.getBean(RedisService.class);
			groups = redisService.hGet(RedisConstant.DICT, RedisConstant.DIR_GAME_GROUP_RULE, HashMap.class);
		} catch (Exception e) {
			logger.error("error", e);
		}

		HashMap<Integer, CommonAppConfigProto.Builder> temps = new HashMap<>();

		Map<Integer, CommonGameConfigProto> groupSubGamneProtos = new ConcurrentHashMap<>();

		groups.forEach((game_type_index, groups) -> {
			int appId = SysGameTypeDict.getInstance().getGameIDByTypeIndex(game_type_index);
			CommonAppConfigProto.Builder b = temps.get(appId);
			if (b == null) {
				b = CommonAppConfigProto.newBuilder();
				b.setAppId(appId);
				temps.put(appId, b);
			}
			CommonGameConfigProto.Builder itemB = CommonGameConfigProto.newBuilder();
			itemB.setGameTypeIndex(game_type_index);
			itemB.setName(SysGameTypeDict.getInstance().getMJname(game_type_index));

			int gold_index = SysGameTypeDict.getInstance().getGameGoldTypeIndex(game_type_index);

			SysParamModel sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(appId).get(gold_index);
			if (sysParamModel == null) {
				logger.error("GameGroupRuleDict获取系统参数错误:game_type_index-->[{}];appId-->[{}];gold_index-->[{}]", game_type_index, appId, gold_index);
				return;
			}

			// 是否免费
			final boolean isFree = sysParamModel.getVal2() == 0;

			int[] roundGoldArray = SysGameTypeDict.getInstance().getGoldIndexByTypeIndex(game_type_index);

			for (int goldConfig : roundGoldArray) {
				SysParamModel tempParam = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(appId).get(goldConfig);
				if (tempParam == null) {
					continue;
				}
				itemB.addGameRound(tempParam.getVal1());
				itemB.addCostCard(tempParam.getVal2());
				itemB.addNeedClubCost(tempParam.getVal4());
				itemB.addClubCost(tempParam.getVal5());
				itemB.addVoiceCost(tempParam.getVal3());
			}

			itemB.setIsFree(isFree);
			if (!StringUtils.isEmpty(groups.getFun())) {
				itemB.setFun(groups.getFun());
			}

			groups.getGroupModels().forEach((group) -> {
				RuleGroupConfig.Builder groupB = RuleGroupConfig.newBuilder();
				groupB.setDesc(group.getDescription());
				groupB.setSelectedIds(group.getSelectedIds());
				groupB.setType(group.getType());
				for (GameGroupRuleModel rule : group.getRuleModels()) {
					RuleConfig.Builder ruleB = RuleConfig.newBuilder();
					if (rule.getValue() == null) {
						continue;
					}
					ruleB.setDesc(rule.getDescription() == null ? "" : rule.getDescription());
					ruleB.setMode(rule.getMode() == null ? "" : rule.getMode());
					ruleB.setRuleIndex(rule.getGame_rule());
					ruleB.setValue(rule.getValue() == null ? "" : rule.getValue());
					ruleB.setSwitch(rule.getStatus() == null ? "" : rule.getStatus());
					ruleB.setTips(rule.getTips() == null ? "" : rule.getTips());
					ruleB.setBased(rule.getBased() == null ? "" : rule.getBased());
					ruleB.setLine(rule.getLine());
					ruleB.setExcludes(rule.getExcludes() == null ? "" : rule.getExcludes());
					groupB.addRules(ruleB);
				}
				itemB.addGroups(groupB);
			});

			groups.getCommonGroups().forEach((group) -> {
				RuleGroupConfig.Builder groupB = RuleGroupConfig.newBuilder();
				groupB.setDesc(group.getDescription());
				groupB.setSelectedIds(group.getSelectedIds());
				groupB.setType(group.getType());
				for (GameGroupRuleModel rule : group.getRuleModels()) {

					RuleConfig.Builder ruleB = RuleConfig.newBuilder();
					ruleB.setDesc(rule.getDescription());
					ruleB.setMode(rule.getMode());
					ruleB.setRuleIndex(rule.getGame_rule());
					ruleB.setValue(rule.getValue());
					ruleB.setSwitch(rule.getStatus());
					ruleB.setBased(rule.getBased() == null ? "" : rule.getBased());
					ruleB.setLine(rule.getLine());
					ruleB.setExcludes(rule.getExcludes() == null ? "" : rule.getExcludes());
					groupB.addRules(ruleB);
				}
				itemB.addCommonGroups(groupB);
			});

			groupSubGamneProtos.put(game_type_index, itemB.build());
			b.addConfigs(itemB);

		});

		this.groupSubGamneProtos = groupSubGamneProtos;
		Map<Integer, CommonAppConfigProto.Builder> groupProtos = new ConcurrentHashMap<>();

		temps.forEach((appId, b) -> {
			groupProtos.put(appId, b);
		});
		this.groupProtos = groupProtos;

		logger.info("redis缓存加载字典GameGroupRuleDict" + timer.getStr());

		// 告诉玩家玩法有更新
		GameRuleResetResponse.Builder p = GameRuleResetResponse.newBuilder();
		Response.Builder resetRsp = PBUtil.toS2CCommonRsp(S2CCmd.GAME_RULE_RESET, p);
		C2SSessionService.getInstance().getAllOnlieSession().forEach((session) -> {
			session.send(resetRsp);
		});
	}

	public CommonAppConfigProto.Builder get(int appId) {
		return groupProtos.get(appId);
	}

	public CommonGameConfigProto getBySubId(int gameId) {
		return groupSubGamneProtos.get(gameId);
	}

}
