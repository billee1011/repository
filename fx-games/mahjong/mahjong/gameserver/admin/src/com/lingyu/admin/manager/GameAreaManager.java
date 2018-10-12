package com.lingyu.admin.manager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.lingyu.admin.core.ErrorCode;
import com.lingyu.admin.dao.GameAreaDao;
import com.lingyu.admin.util.SessionUtil;
import com.lingyu.admin.vo.GameAreaVo;
import com.lingyu.common.entity.GameArea;

@Service
@Transactional(propagation = Propagation.REQUIRED)
public class GameAreaManager {
	private static final Logger logger = LogManager.getLogger(GameAreaManager.class);
	@Autowired
	private GameAreaDao gameAreaDao;
	/**
	 * pid -> GameAreaMap
	 */
	private Map<String, GameAreaMap> gameAreaMap = new HashMap<>();
	
	private Map<Integer, GameArea> gameAreaWorldMap = new HashMap<>();

	// 内存中的缓存
	// private Map<Integer, GameArea> gameAreaMap = new HashMap<Integer,
	// GameArea>();

	public void init() {
		logger.info("游戏区缓存化开始");
		List<GameArea> list = gameAreaDao.queryAll();
		for (GameArea area : list) {
			addGameArea(area);
			logger.info("area={}", area.toString());
		}
		logger.info("游戏区缓存化完毕");
	}
	
	/**
	 * 添加游戏区
	 * @param area
	 */
	public void addGameArea(GameArea area){
		gameAreaDao.add(area);
		GameAreaMap gam = gameAreaMap.get(area.getPid());
		if (gam == null) {
			gam = new GameAreaMap();
			gameAreaMap.put(area.getPid(), gam);
		}
		gam.addGameArea(area);
		
		gameAreaWorldMap.put(area.getWorldId(), area);
	}
	
	/**
	 * 根据worldId找到gameArea
	 * @param worldId
	 * @return
	 */
	public GameArea getGameArea(int worldId) {
		return gameAreaWorldMap.get(worldId);
	}

	/**
	 * 方法描述： 获取所有游戏服务器
	 * 
	 * @return
	 */
	public List<GameArea> getGameAreaList(String pid) {
		GameAreaMap map = gameAreaMap.get(pid);
		if (map == null) {
			return Collections.emptyList();
		}
		List<GameArea> gaList = new ArrayList<GameArea>(map.values());
		Collections.sort(gaList);
		return gaList;
	}
	
	/**
	 * 方法描述： 获取全平台所有游戏服务器
	 * 
	 * @return
	 */
	public List<GameArea> getAllGameAreaList() {
		List<GameArea> allList = new ArrayList<>();
		Collection<GameAreaMap> allGameAreaMap = gameAreaMap.values();
		for (GameAreaMap gameAreaMap : allGameAreaMap) {
			List<GameArea> gaList = new ArrayList<GameArea>(gameAreaMap.values());
			allList.addAll(gaList);
		}
		Collections.sort(allList);
		return allList;
	}
	
	/**
	 * 过滤选择的区服 把主区及其从区加进去 选择的从区不考虑
	 * @param areaIds
	 * @return
	 */
	public List<Integer> filterGameAreaIds(String pid, List<Integer> areaIds) {
		List<Integer> ret = new ArrayList<Integer>();
		Set<Integer> addedMainAreas = new HashSet<>();
		for (Integer areaId : areaIds) {
			GameAreaMap map = gameAreaMap.get(pid);
			if (map != null) {
				GameArea gameArea = map.getGameAreaByAreaId(areaId);
				if (gameArea == null) {
					continue;
				}

				if (gameArea.getFollowerId() == 0 && !addedMainAreas.contains(gameArea.getAreaId())) { // 只添加主区
					// 从区的从服务器数据拿
					ret.add(gameArea.getAreaId());
					addedMainAreas.add(gameArea.getAreaId());
					for (GameArea child : gameArea.getChildAreas()) {
						ret.add(child.getAreaId());
					}
				}
			}
		}

		return ret;
	}
	
	/** 获取将要被处理的游戏区 */
	public List<GameArea> getHandleGameAreaList(String pid, boolean all, List<Integer> list) {
		List<GameArea> ret = new ArrayList<>();
		if (all) {
			// 全服操作
			ret = this.getValidGameAreaList(pid);
		} else {
			// 部分区
			ret = this.getGameAreaListByIds(pid, list);
		}
		return ret;
	}
	
	/**
	 * @param idList
	 * @return
	 */
	public List<GameArea> getGameAreaListByIds(String pid, Collection<Integer> idList) {
		HashSet<Integer> set = new HashSet<>();
		List<GameArea> ret = new ArrayList<GameArea>();
		for (Integer id : idList) {
			GameArea area = this.getGameAreaByAreaId(pid, id);
			if (area.isValid()) {
				if (area.getFollowerId() == 0) {
					if (!set.contains(area.getWorldId())) {
						ret.add(area);
						set.add(area.getWorldId());
					}
				} 
			}
		}
		return ret;
	}
	
	/**
	 * 方法描述： 获取有效的游戏服务器 TODO 以后不需要用到的时候再去过滤，和redis的服务器状态结合
	 * 
	 * @return
	 */
	public List<GameArea> getValidGameAreaList(String pid) {
		List<GameArea> ret = new ArrayList<GameArea>();
		Collection<GameArea> list = this.getGameAreaList(pid);
		for (GameArea e : list) {
			if (e.isValid() && e.getFollowerId() == 0) {
				ret.add(e);
			}
		}
		return ret;

	}
	
	public GameArea getGameAreaByAreaId(String pid, int areaId) {
		GameArea ret = null;
		GameAreaMap map = gameAreaMap.get(pid);
		if (map != null) {
			ret = map.getGameAreaByAreaId(areaId);
		}
		return ret;
	}
	
	/**
	 * 修改游戏区
	 * 
	 * @param msg
	 * @return
	 */
	public String updateGameArea(GameArea gameArea) {
		// Date now = new Date();
		// gameArea.setModifyTime(now);
		String retCode = ErrorCode.EC_OK; // gameAreaDao.update(gameArea);
		GameAreaMap map = gameAreaMap.get(gameArea.getPid());
		if (map != null) {
			GameArea oldArea = map.getGameAreaByAreaId(gameArea.getAreaId());
			map.addGameArea(gameArea);

//			if (!oldArea.isValid() && gameArea.isValid()) { // 有关闭状态进入开启或维护状态
//				gameArea.setRestartTime(new Date());
//			} else {
//				gameArea.setRestartTime(oldArea.getRestartTime());
//			}
			gameArea.setRestartTime(new Date());

			gameArea.setCombineTime(oldArea.getCombineTime());

			gameAreaDao.update(gameArea);
		} else {
			retCode = ErrorCode.EC_FAILED;
			}
		logger.info("更新游戏区成功 {}", gameArea.toString());
		return retCode;
	}
	
	public GameArea getFirstGameArea(String pid) {
		Collection<GameArea> list = this.getGameAreaList(pid);
		for (GameArea e : list) {
			if (StringUtils.equals(e.getPid(), pid)) {
				return e;
			}
		}
		return null;
	}
	
	/**
	 * GameArea -> GameAreaVo 多对多
	 * 
	 * @param areas
	 * @return
	 */
	public List<GameAreaVo> transferToGameAreaVo(Collection<GameArea> areas) {
		if (CollectionUtils.isNotEmpty(areas)) {
			List<GameAreaVo> ret = new ArrayList<>(areas.size());
			for (GameArea gameArea : areas) {
				GameAreaVo gameAreaVo = gameArea.getGameAreaVo();
				if (gameAreaVo == null) {
					// Platform platform =
					// platformManager.getPlatform(gameArea.getPid());
					gameAreaVo = gameArea.toGameAreaVo();
					if (gameArea.getFollowerId() != 0) {
						GameArea mainArea = getGameAreaByAreaId(gameArea.getPid(), gameArea.getAreaId());
//						GameArea mainArea = getGameArea(gameArea.getFollowerId());
						gameAreaVo.setFollowerAreaId(mainArea.getAreaId());
					}
					gameArea.setGameAreaVo(gameAreaVo);
				}
				ret.add(gameAreaVo);
			}
			return ret;
		} else {
			return Collections.emptyList();
		}
	}
	
	/**
	 * 设定游戏区
	 * 
	 * @param areaId
	 */
	public String selectGameArea(String pid, int areaId) {
		logger.info("选择游戏区 areaId={}", areaId);
		GameAreaMap map = gameAreaMap.get(pid);
		GameArea gameArea = null;
		if (map != null) {
			gameArea = map.getGameAreaByAreaId(areaId);
		}
		if (gameArea == null) {
			return ErrorCode.GAME_AREA_DATA_WRONG;
		} else {
			SessionUtil.setSessionValue(SessionUtil.AREA_ID_KEY, areaId);
			return ErrorCode.EC_OK;
		}
	}
	
	public class GameAreaMap {
		private Map<Integer, GameArea> areaId2area = new HashMap<>();

		public void addGameArea(GameArea gameArea) {
			areaId2area.put(gameArea.getAreaId(), gameArea);
		}

		public GameArea getGameAreaByAreaId(Integer areaId) {
			return areaId2area.get(areaId);
		}

		public Collection<GameArea> values() {
			return areaId2area.values();
		}

		public Set<Integer> keys() {
			return areaId2area.keySet();
		}
	}
}
