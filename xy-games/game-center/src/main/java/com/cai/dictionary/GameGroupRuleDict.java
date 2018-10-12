package com.cai.dictionary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.GameGroupModel;
import com.cai.common.domain.GameGroupSetModel;
import com.cai.common.domain.GameGroups;
import com.cai.common.domain.xml.GameGroupList;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.common.util.XmlUtil;
import com.cai.redis.service.RedisService;
import com.cai.service.PublicService;

/**
 * 游戏类型对应收费索引 游戏类型 描述字典
 */
public class GameGroupRuleDict {

	private Logger logger = LoggerFactory.getLogger(GameGroupRuleDict.class);
	
	private static final int COMMON_ID_MAX = 100;

	private Map<Integer, GameGroups> gameGroups;

	/**
	 * 单例
	 */
	private static GameGroupRuleDict instance;

	/**
	 * 私有构造
	 */
	private GameGroupRuleDict() {
		gameGroups = new ConcurrentHashMap<>();
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

	public void load() {
		PerformanceTimer timer = new PerformanceTimer();
		PublicService publicService = SpringService.getBean(PublicService.class);

		List<GameGroupSetModel> groupSets = publicService.getPublicDAO().getGameGroupSetList();
		Map<Integer, GameGroups> temps = new HashMap<>();
		// ID为0的代表所有游戏都要有
		List<GameGroupModel> defaultGroups = new ArrayList<>();

		// 公共玩法其他游戏可选
		Map<Integer, GameGroups> commonGroups = new HashMap<>();
		for (GameGroupSetModel set : groupSets) {
			try {
				if (set.getGame_type_index() == 0) {
					// ID为0的代表所有游戏都要有
					GameGroupList commonXml = XmlUtil.toObject(set.getGroup_rule(), GameGroupList.class);
					defaultGroups.addAll(commonXml.getGroups());
				} else if (set.getGame_type_index() <= COMMON_ID_MAX) {
					// 100以下的为公用
					GameGroupList commonXml = XmlUtil.toObject(set.getGroup_rule(), GameGroupList.class);
					GameGroups group = new GameGroups(set.getGame_type_index());
					group.getGroupModels().addAll(commonXml.getGroups());
					commonGroups.put(set.getGame_type_index(), group);

				}
			} catch (JAXBException e) {
				logger.error("gameGroupLoad 加载玩法规则xml失败 " + set.getGame_type_index(), e);
			}
		}

		groupSets.forEach((set) -> {
			logger.info("加载游戏玩法配置 id:" + set.getGame_type_index());
			
			if (set.getGame_type_index() <= COMMON_ID_MAX) {
				return;
			}

			GameGroups group = temps.get(set.getGame_type_index());
			
			if (group == null) {
				group = new GameGroups(set.getGame_type_index());
				temps.put(set.getGame_type_index(), group);
			}
		
			try {
				GameGroupList xmlList = XmlUtil.toObject(set.getGroup_rule(), GameGroupList.class);
				group.getGroupModels().addAll(xmlList.getGroups());
				if(xmlList.getSet() != null){
					for (Integer id : xmlList.getSet().getIds()) {
						GameGroups commonGroup = commonGroups.get(id);
						if (commonGroup == null) {
							return;
						}
						group.getCommonGroups().addAll(commonGroup.getGroupModels());
					}
				}
				group.getCommonGroups().addAll(defaultGroups);
				
				group.setFun(xmlList.getFun());
			} catch (Exception e) {
				logger.error("gameGroupLoad 加载玩法规则xml失败 " + set.getGame_type_index(), e);
			}
		});
		Map<Integer, GameGroups> gameGroups = new ConcurrentHashMap<>();

		temps.forEach((index, groups) -> {
			gameGroups.put(index, groups);
		});
		this.gameGroups = gameGroups;

		// 放入redis缓存
		RedisService redisService = SpringService.getBean(RedisService.class);
		redisService.hSet(RedisConstant.DICT, RedisConstant.DIR_GAME_GROUP_RULE, gameGroups);
		logger.info("加载字典GameGroupRuleDict,count=" + gameGroups.size() + timer.getStr());
	}

	public GameGroups get(int game_type_index) {
		return gameGroups.get(game_type_index);
	}

}
