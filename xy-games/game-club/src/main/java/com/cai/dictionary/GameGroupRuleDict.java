/**
 * 
 */
package com.cai.dictionary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.ClubRuleModel;
import com.cai.common.domain.GameGroupModel;
import com.cai.common.domain.GameGroupRuleModel;
import com.cai.common.domain.GameGroups;
import com.cai.common.util.SpringService;
import com.cai.redis.service.RedisService;

import protobuf.clazz.Common.CommonGameRuleProto;
import protobuf.clazz.Common.CommonGameRulesProto;

/**
 * 
 */
public class GameGroupRuleDict {

	private Logger logger = LoggerFactory.getLogger(GameGroupRuleDict.class);

	// 每款子游戏的数据
	private Map<Integer, GameGroups> groups;

	/**
	 * 单例
	 */
	private static GameGroupRuleDict instance;

	/**
	 * 私有构造
	 */
	private GameGroupRuleDict() {
		groups = new ConcurrentHashMap<Integer, GameGroups>();
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
		try {
			RedisService redisService = SpringService.getBean(RedisService.class);
			groups = redisService.hGet(RedisConstant.DICT, RedisConstant.DIR_GAME_GROUP_RULE, HashMap.class);
		} catch (Exception e) {
			logger.error("error", e);
		}

	}

	public void checkClubRule(ClubRuleModel model) {
		GameGroups gameGroups = groups.get(model.getGame_type_index());
		if (gameGroups == null) {
			return;
		}

		try {
			CommonGameRulesProto.Builder b = model.getRules().toBuilder();
			boolean isUpdate = false;
			List<CommonGameRuleProto> temp1 = new ArrayList<>(model.getRules().getRulesList());
			// 过期的Id， 玩法规则里已经没有了。
			for (int i = 0; i < model.getRules().getRulesCount(); i++) {
				if(isExpired(gameGroups, model.getRules().getRules(i).getRuleId())){
					temp1.remove(model.getRules().getRules(i));
					isUpdate = true;
				}
			}
			if(isUpdate){
				b.clearRules().addAllRules(temp1);
			}
			
			// 如果这个group是必选的，但是俱乐部玩法里又没有，默认加上
			List<CommonGameRuleProto> temp = new ArrayList<>(4);
//			gameGroups.getCommonGroups().forEach((gameGroupModel) -> {
//				if (gameGroupModel.getType() == 0) {
//					if (!model.getRules().getRulesList().stream().anyMatch(rule -> testRule(rule.getRuleId(), gameGroupModel.getRuleModels()))) {
//						addNewRequireRule(gameGroupModel, model.getRules(), temp);
//					}
//				}
//			});

			if (temp.size() > 0 || isUpdate) {
				
				b.addAllRules(temp);
				model.setRules(b.build());
				model.encodeRule();
				model.init();
			}

		} catch (Exception e) {
			logger.error("检查俱乐部玩法报错" + model.getClub_id() + "," + model.getGame_type_index(), e);
		}

	}
	
	private boolean isExpired(GameGroups gameGroups, int id){
		for (GameGroupModel groupConfig : gameGroups.getGroupModels()) {
			for (GameGroupRuleModel ruleConfig : groupConfig.getRuleModels()) {
				if(ruleConfig.getGame_rule() == id){
					return false;
				}
			}
		}
		
		for (GameGroupModel groupConfig : gameGroups.getCommonGroups()) {
			for (GameGroupRuleModel ruleConfig : groupConfig.getRuleModels()) {
				if(ruleConfig.getGame_rule() == id){
					return false;
				}
			}
		}
		
		return true;
	}

	private void addNewRequireRule(GameGroupModel config, CommonGameRulesProto rules, List<CommonGameRuleProto> temp) {
		if (config.getRuleModels().isEmpty()) {
			return;
		}

		GameGroupRuleModel defaultConfig = null;
		if (StringUtils.isEmpty(config.getSelectedIds())) {
			defaultConfig = config.getRuleModels().get(0);

		} else {
			int selectId = Integer.valueOf(config.getSelectedIds());
			for (GameGroupRuleModel ruleConfig : config.getRuleModels()) {
				if (ruleConfig.getGame_rule() == selectId) {
					defaultConfig = ruleConfig;
					break;
				}
			}

			if (defaultConfig == null) {
				defaultConfig = config.getRuleModels().get(0);
			}

		}

		CommonGameRuleProto.Builder ruleB = CommonGameRuleProto.newBuilder();
		ruleB.setRuleId(defaultConfig.getGame_rule());
		ruleB.setValue(Integer.valueOf(defaultConfig.getValue()));
		temp.add(ruleB.build());
	}

	private boolean testRule(int markId, List<GameGroupRuleModel> ruleConfigs) {
		for (GameGroupRuleModel gameGroupRuleModel : ruleConfigs) {
			if (gameGroupRuleModel.getGame_rule() == markId) {
				return true;
			}
		}
		return false;
	}
	
	
	public GameGroups get(int game_type_index){
		return groups.get(game_type_index);
	}


}
