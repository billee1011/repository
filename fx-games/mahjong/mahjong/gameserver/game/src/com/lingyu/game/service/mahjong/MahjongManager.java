package com.lingyu.game.service.mahjong;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.lingyu.common.constant.ModuleConstant;
import com.lingyu.common.core.ErrorCode;
import com.lingyu.common.entity.ChessEvery;
import com.lingyu.common.entity.MahjongResultData;
import com.lingyu.common.entity.MahjongResultDetailsData;
import com.lingyu.common.entity.MahjongResultLog;
import com.lingyu.common.entity.MahjongRoom;
import com.lingyu.common.entity.PlayBackVo;
import com.lingyu.common.entity.Role;
import com.lingyu.common.entity.RoleCache;
import com.lingyu.common.entity.SignVo;
import com.lingyu.common.http.PlatformClient;
import com.lingyu.common.io.MsgType;
import com.lingyu.common.job.Scheduled;
import com.lingyu.common.template.Fan2JifenTemplate;
import com.lingyu.common.template.FanTemplate;
import com.lingyu.common.template.JiFenTemplate;
import com.lingyu.common.util.DatatimeUtil;
import com.lingyu.common.util.FileUtil;
import com.lingyu.common.util.JsonUtil;
import com.lingyu.common.util.Lottery;
import com.lingyu.common.util.ObjectUtil;
import com.lingyu.common.util.TimeUtil;
import com.lingyu.game.GameServerContext;
import com.lingyu.game.RouteManager;
import com.lingyu.game.service.config.ConfigConstant;
import com.lingyu.game.service.config.ConfigDataTemplateManager;
import com.lingyu.game.service.event.ChessJifenEvent;
import com.lingyu.game.service.event.LoginGameEvent;
import com.lingyu.game.service.id.IdManager;
import com.lingyu.game.service.id.TableNameConstant;
import com.lingyu.game.service.job.ScheduleType;
import com.lingyu.game.service.role.RoleManager;
import com.lingyu.game.service.role.RoleRepository;

@Service
public class MahjongManager {
	private static final Logger logger = LogManager.getLogger(MahjongManager.class);

	@Autowired
	private RoleManager roleManager;
	@Autowired
	private RoleRepository roleRepository;
	@Autowired
	private RouteManager routeManager;
	@Autowired
	private MahjongDataTemplateManager mahjongDataTemplateManager;
	@Autowired
	private ConfigDataTemplateManager configDataTemplateManager;
	@Autowired
	private MahjongResultLogRepository mahjongResultLogRepository;
	@Autowired
	private IdManager idManager;

	private PlatformClient platformClient;

	/** 房间列表的缓存 key=房间好，val=room */
	private final Map<Integer, MahjongRoom> rooms = new ConcurrentHashMap<>();

	/** 房间编号缓存 已去重 */
	private final List<Integer> roomIds = new CopyOnWriteArrayList<>();

	public void init() {
		resetMajhongRoomNum();
	}

	/**
	 * 重置麻将房间编号
	 */
	public void resetMajhongRoomNum() {
		long startTime = System.currentTimeMillis();
		roomIds.clear();
		int randomCount = ConfigConstant.ROOM_NUM_COUNT;
		Set<Integer> roomSetIds = new HashSet<>();
		for (int i = 1; i < randomCount; i++) {
			int roomNum = RandomUtils.nextInt(100000, 999999);
			roomSetIds.add(roomNum);
		}
		roomIds.addAll(roomSetIds);
		logger.info("resetRoomIds end, sum={}, interval={}", roomIds.size(), System.currentTimeMillis() - startTime);
	}

	/**
	 * 获取麻将房间号
	 */
	private Integer randomRoomNum() {
		Integer roomNum = Lottery.random(roomIds);
		roomIds.remove(roomNum);
		return roomNum;
	}

	/**
	 * 每局结束 初始化房间信息
	 *
	 * @param roleId
	 * @return
	 */
	public JSONObject startGame(long roleId) {
		JSONObject result = new JSONObject();
		result.put(ErrorCode.RESULT, ErrorCode.OK);
		Role role = roleManager.getRole(roleId);
		if (role == null) {
			result.put(ErrorCode.RESULT, ErrorCode.FAILED);
			result.put(ErrorCode.CODE, ErrorCode.ROLE_NOT_EXIST);
			return result;
		}

		int roomNum = role.getRoomNum();
		MahjongRoom room = rooms.get(roomNum);
		if (room == null) {
			result.put(ErrorCode.RESULT, ErrorCode.FAILED);
			result.put(ErrorCode.CODE, ErrorCode.ROLE_NOT_IN_ROOM);
			return result;
		}
		Map<Long, Boolean> startMap = room.getStartGameMap();
		if (startMap.size() >= ConfigConstant.ROOM_NUM) {
			result.put(ErrorCode.RESULT, ErrorCode.FAILED);
			result.put(ErrorCode.CODE, ErrorCode.OVER_ROOM_NUM);
			return result;
		}

		if (room.getAlreadyJuShu() >= room.getJushu()) {
			result.put(ErrorCode.RESULT, ErrorCode.FAILED);
			result.put(ErrorCode.CODE, ErrorCode.CUR_JUSHU_FINISH);
			return result;
		}

		if (room.getDisMissApplyRoleId() != 0) {
			result.put(ErrorCode.RESULT, ErrorCode.FAILED);
			result.put(ErrorCode.CODE, ErrorCode.START_GAME_ERROR_OPERATE);
			return result;
		}

		RoleCache roleCache = room.getRoleCacheByRoleId(roleId);
		if (roleCache == null) {
			result.put(ErrorCode.RESULT, ErrorCode.FAILED);
			result.put(ErrorCode.CODE, ErrorCode.OPERATE_ERROR);
			return result;
		}
		result.put(MahjongConstant.CLIENT_DATA, new Object[] { roleId, room.getAllRoleJifen() });
		routeManager.broadcast(room.getAllRoleId(), MsgType.MAHJONG_START_GAME_MSG, result);

		startMap.put(roleId, true);
		if (startMap.size() >= ConfigConstant.ROOM_NUM) {
			room.init();
			start(room);
		}

		return null;
	}

	/**
	 * 打牌
	 *
	 * @param roleId
	 * @param paiId
	 * @return
	 */
	public JSONObject play(long roleId, int paiId) {
		JSONObject result = new JSONObject();
		result.put(ErrorCode.RESULT, ErrorCode.OK);

		Role role = roleManager.getRole(roleId);
		if (role == null) {
			result.put(ErrorCode.RESULT, ErrorCode.FAILED);
			result.put(ErrorCode.CODE, ErrorCode.ROLE_NOT_EXIST);
			return result;
		}

		int roomNum = role.getRoomNum();
		MahjongRoom room = rooms.get(roomNum);
		if (room == null) {
			result.put(ErrorCode.RESULT, ErrorCode.FAILED);
			result.put(ErrorCode.CODE, ErrorCode.ROLE_NOT_IN_ROOM);
			return result;
		}

		Map<Integer, RoleCache> map = room.getRoleCacheMap();
		RoleCache roleCache = map.get(room.getOperateIndex());
		if (roleCache == null) {
			result.put(ErrorCode.RESULT, ErrorCode.FAILED);
			result.put(ErrorCode.CODE, ErrorCode.OPERATE_ERROR);
			return result;
		}
		if (roleCache.getRoleId() != roleId) {
			result.put(ErrorCode.RESULT, ErrorCode.FAILED);
			result.put(ErrorCode.CODE, ErrorCode.ROLE_ID_NOT_SAME);
			return result;
		}

		ChessEvery ce = roleCache.getChessEveryById(paiId);
		if (ce == null) {
			result.put(ErrorCode.RESULT, ErrorCode.FAILED);
			result.put(ErrorCode.CODE, ErrorCode.ROLE_NOT_THIS_EVERY);
			return result;
		}

		if (roleCache.isTan()) {
			result.put(ErrorCode.RESULT, ErrorCode.FAILED);
			result.put(ErrorCode.CODE, ErrorCode.ROLECACHE_EXIST_SIGN);
			return result;
		}

		boolean playSpecialChess = false; // 打出的牌是杠牌
		if (isSpecialChess(ce, room)) { // 打出的是癞子、痞子、红中
			ce.setUsed(true);
			playSpecialChess = true;

			addSpecialPlayCount(room, roleCache, ce);
		} else {
			roleCache.addOutChess(ce);
			roleCache.removeRoleChess(ce);
		}
		room.setCurrChessEvery(ce);

		// 只要有人打牌。翻的倍数和胡牌列表和标签缓存都要清空
		if (room.getFan() != 0) {
			room.setFan(0);
		}
		if (MapUtils.isNotEmpty(room.getSignMap())) {
			room.getSignMap().clear();
		}

		// 打出去的牌，推送给房间的每个人
		JSONObject broadPlay = new JSONObject();
		broadPlay.put(ErrorCode.RESULT, ErrorCode.EC_OK);
		Object[] data = new Object[] { roleId, room.getOperateIndex(), chessEveryVo(ce),
		        roleCache.getHeadChess().size() };
		broadPlay.put(MahjongConstant.CLIENT_DATA, data);
		routeManager.broadcast(room.getAllRoleId(), MsgType.MAHJONG_PLAY_MSG, broadPlay);

		// 记录回放打牌操作
		addPlayBack(room, MahjongConstant.PLAY_BACK_TYPE_PLAY, roleId, room.getOperateIndex(), chessEveryVo(ce));

		logger.info("dapai...roleId={}, roleIndex={},name={},paiId={},paiColor={},paiNum={}", roleId,
		        room.getOperateIndex(), roleCache.getName(), ce.getId(), ce.getColor(), ce.getNumber());

		// 打出的是杠牌所以还是自己摸排
		if (playSpecialChess) {
			// 给客户端推送下一个人摸到的牌
			moChess(room.getRoleIdByIndex(room.getOperateIndex()), false);
			// 打出杠牌开花了
			room.setFan(MahjongConstant.MAHJONG_HU_TYPE_QIANGGANG);
			return null;
		}
		// 验证打出去的牌其他3个人是否需要
		boolean flag = pushSign(roleId, room, ce);
		// 如果都不需要，就该下一个人摸牌了
		if (!flag) {
			room.resetOperateIndex(0);

			// 给客户端推送下一个人摸到的牌
			moChess(room.getRoleIdByIndex(room.getOperateIndex()), true);
		}

		return null;
	}

	/**
	 * 摸牌
	 *
	 * @param roleId
	 * @param order
	 *            摸牌顺序 杠了的话。要从最后一张开始摸
	 */
	private void moChess(long roleId, boolean order) {
		Role role = roleManager.getRole(roleId);
		int roomNum = role.getRoomNum();

		MahjongRoom room = rooms.get(roomNum);
		int index = room.getIndexByRoleId(roleId);
		RoleCache roleCache = room.getRoleCacheMap().get(index);

		logger.info("start mopai, roleId={}, roleIndex={},name={}", roleId, index, roleCache.getName());

		// 这里要判断。牌是否都摸完了。
		if (room.allPlayFinish()) {
			logger.info("pai mo finish,sumIndex={}", room.getSumIndex());
			oneEndOutput(room, room.getZhuang(), MahjongConstant.CUR_JU_END_MOPAIFINISH, null);
			return;
		}

		// 摸到的牌
		ChessEvery moChess = null;
		if (order) {
			moChess = room.getMo();
		} else {
			moChess = room.getLastMo();
		}

		room.setCurrMoChessEvery(moChess);

		// 摸到的牌放到玩家身上
		roleCache.addRoleChess(moChess, true);

		// 摸到的牌给摸到的人推送过去
		JSONObject moResult = new JSONObject();
		moResult.put(ErrorCode.RESULT, ErrorCode.EC_OK);
		Object[] moData = new Object[] { roleId, index, chessEveryVo(moChess) };
		moResult.put(MahjongConstant.CLIENT_DATA, moData);
		routeManager.relayMsg(roleCache.getRoleId(), MsgType.MAHJONG_MO_MSG, moResult);

		// 告诉其他人。他摸牌了。
		JSONObject otherResult = new JSONObject();
		otherResult.put(ErrorCode.RESULT, ErrorCode.EC_OK);
		Object[] otherData = new Object[] { roleId, index, null };
		otherResult.put(MahjongConstant.CLIENT_DATA, otherData);
		routeManager.broadcast(room.getOtherRoleId(roleCache.getRoleId()), MsgType.MAHJONG_MO_MSG, otherResult);

		logger.info("mo chess end, roleId={},roleIndex={}, name={},chessId={},chessColor={}, chessNum={}", roleId,
		        index, roleCache.getName(), moChess.getId(), moChess.getColor(), moChess.getNumber());

		if (MapUtils.isNotEmpty(room.getSignMap())) {
			room.getSignMap().clear();
		}
		// 摸完牌。记录摸牌操作
		addPlayBack(room, MahjongConstant.PLAY_BACK_TYPE_MOCHESS, roleId, room.getOperateIndex(),
		        chessEveryVo(moChess));

		// 验证玩家是否有提示标签
		List<Integer> result = check(roleCache, true, moChess, false);
		// 有的话。记录过路杠操作
		if (CollectionUtils.isNotEmpty(result)) {
			int guolugang = MahjongConstant.MAHJONG_GUOLUGANG;
			if (result.contains(guolugang)) {
				room.addSignMap(roleCache.getRoleId(), new SignVo(result, 0));
				roleCache.setGuolugang(true);
			}
		}
	}

	/**
	 * 找出和目标牌相关联的其他牌，例如目标牌是3万，关联牌：1万、2万、4万、5万，风牌不计算
	 *
	 * @param ce
	 * @return
	 */
	private List<ChessEvery> getRelationChess(ChessEvery ce) {
		List<ChessEvery> targetList = new ArrayList<>();
		if (ce.getColor() >= MahjongConstant.MAHJONG_FENG) {
			return targetList;
		}
		for (int i = -2; i <= 2; i++) {
			if (i == 0) {
				continue;
			}
			int chessNumber = ce.getNumber() + i;
			if (chessNumber >= 1 && chessNumber <= 9) {
				ChessEvery target = new ChessEvery();
				target.setNumber(chessNumber);
				target.setColor(ce.getColor());
				targetList.add(target);
			}
		}
		return targetList;
	}

	/**
	 * 牌列表中是否含有ce的牌
	 *
	 * @param list
	 * @param target
	 * @return
	 */
	private boolean chessListContainsChess(List<ChessEvery> list, ChessEvery target) {
		for (ChessEvery ce : list) {
			if (ce.isUsed()) {
				continue;
			}
			if (ce.getNumber() == target.getNumber() && ce.getColor() == target.getColor()) {
				return true;

			}
		}
		return false;
	}

	/**
	 * 是否开口说话了,痞子杠、癞子杠、红中杠不算开口
	 *
	 * @return
	 */
	private boolean isPick(RoleCache cache) {
		List<ChessEvery> list = cache.getRoleChessList();
		Role role = roleManager.getRole(cache.getRoleId());
		MahjongRoom room = rooms.get(role.getRoomNum());

		for (ChessEvery chessEvery : list) {
			if (isSpecialChess(chessEvery, room)) {
				continue;
			}
			if (chessEvery.isUsed()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 是否是特殊牌----痞子牌，癞子牌，红中
	 *
	 * @param chessEvery
	 * @param room
	 * @return
	 */
	public boolean isSpecialChess(ChessEvery chessEvery, MahjongRoom room) {
		if (chessEvery.getNumber() == room.getRuffian().getNumber()
		        && chessEvery.getColor() == room.getRuffian().getColor()) {
			return true;
		}
		if (chessEvery.getNumber() == room.getRandomLaizi().getNumber()
		        && chessEvery.getColor() == room.getRandomLaizi().getColor()) {
			return true;
		}
		if (chessEvery.getNumber() == MahjongConstant.FENG_TYPE_ZHONG
		        && chessEvery.getColor() == MahjongConstant.MAHJONG_FENG) {
			return true;
		}
		return false;
	}

	/**
	 * 移除特殊牌
	 * 
	 * @param roleChessList
	 * @param room
	 * @return
	 */
	private List<ChessEvery> removeSpecialChess(List<ChessEvery> roleChessList, MahjongRoom room) {
		List<ChessEvery> temp = new ArrayList<>();
		for (ChessEvery ce : roleChessList) {
			if (!isSpecialChess(ce, room)) {
				temp.add(ce);
			}
		}
		return temp;
	}

	private void addSpecialPlayCount(MahjongRoom room, RoleCache roleCache, ChessEvery ce) {
		if (ce.getNumber() == room.getRuffian().getNumber() && ce.getColor() == room.getRuffian().getColor()) {
			roleCache.setOneRuffianGangCount(roleCache.getOneRuffianGangCount() + 1);
		}
		if (ce.getNumber() == room.getRandomLaizi().getNumber() && ce.getColor() == room.getRandomLaizi().getColor()) {
			roleCache.setOneLaiZiGangCount(roleCache.getOneLaiZiGangCount() + 1);
		}
		if (ce.getNumber() == MahjongConstant.FENG_TYPE_ZHONG && ce.getColor() == MahjongConstant.MAHJONG_FENG) {
			roleCache.setOneHzGangCount(roleCache.getOneLaiZiGangCount() + 1);
		}
	}

	/**
	 * 打出去牌。验证是否需要推送标签
	 *
	 * @param roleId
	 * @param map
	 * @param vo
	 */
	private boolean pushSign(long roleId, MahjongRoom room, ChessEvery vo) {
		boolean flag = false;
		boolean haveHu = false;
		for (RoleCache cache : room.getRoleCacheMap().values()) {
			// 自己打出去的。就不要推送标签验证了。
			if (cache.getRoleId() == roleId) {
				continue;
			}
			List<Integer> result = check(cache, false, vo, false);
			// 有的话。记录操作
			if (CollectionUtils.isNotEmpty(result)) {
				flag = true;
				int eat = MahjongConstant.MAHJONG_EAT;
				int hu = MahjongConstant.MAHJONG_HU;
				int peng = MahjongConstant.MAHJONG_PENG;
				int minggang = MahjongConstant.MAHJONG_MINGGANG;
				if (result.contains(eat) || result.contains(hu) || result.contains(peng) || result.contains(minggang)) {
					room.addSignMap(cache.getRoleId(), new SignVo(result, 0));
				}

				if (result.contains(hu)) {
					haveHu = true;
				}
			}
		}

		// 推送碰，吃， 杠
		long firstRoleIdSign = getFirstRoleIdSign(room.getSignMap());
		if (!haveHu && firstRoleIdSign != -1) {
			JSONObject resultData = new JSONObject();
			resultData.put(ErrorCode.RESULT, ErrorCode.OK);
			resultData.put(MahjongConstant.CLIENT_DATA, room.getSignMap().get(firstRoleIdSign).getSignList());
			RoleCache roleCache = room.getRoleCacheByRoleId(firstRoleIdSign);
			roleCache.setTan(true);
			routeManager.relayMsg(roleCache.getRoleId(), MsgType.MAHJONG_SHOW_MSG, resultData);
		}
		return flag;
	}

	private long getFirstRoleIdSign(Map<Long, SignVo> map) {
		long oneRoleId = -1;
		long twoRoleId = -2;
		int signRoleCount = 0;

		// 思路：打出一张牌，一家胡，一家碰（杠），一家吃，排除胡标签，只获取不是吃的标签优先推送
		for (long roleId : map.keySet()) {
			SignVo vo = map.get(roleId);
			if (vo.getSignList().contains(MahjongConstant.MAHJONG_HU)) {
				continue;
			}
			signRoleCount++;
			if (signRoleCount == 1) {
				oneRoleId = roleId;
			} else if (signRoleCount == 2) {
				twoRoleId = roleId;
			}
		}

		if (signRoleCount == 1) {
			return oneRoleId;
		} else if (signRoleCount == 2) {
			if (map.get(oneRoleId).getSignList().contains(MahjongConstant.MAHJONG_EAT)) {
				return twoRoleId;
			} else {
				return oneRoleId;
			}
		}
		return oneRoleId;
	}

	/**
	 * 验证除了roleId，其他3家是否有人胡牌
	 *
	 * @param roleId
	 * @param map
	 * @param vo
	 * @return
	 *//*
	   * private List<RoleCache> checkHu(long roleId, Map<Integer, RoleCache>
	   * map, ChessEvery vo) { List<RoleCache> list = new ArrayList<>(); for
	   * (RoleCache cache : map.values()) { if (cache.getRoleId() == roleId) {
	   * continue; } // 添加一个临时变量验证是否胡牌 RoleCache temp = getRoleCacheTemp(cache,
	   * vo);
	   *
	   * Integer hu = isWin(temp); if (hu != null) { list.add(cache); } } return
	   * list; }
	   */

	/**
	 * 点击标签
	 *
	 * @param roleId
	 * @param index
	 *            当前玩家的索引位
	 * @param signType
	 *            标签类型
	 * @return
	 */
	public JSONObject sign(long roleId, int signType) {
		JSONObject result = new JSONObject();
		result.put(ErrorCode.RESULT, ErrorCode.OK);

		Role role = roleManager.getRole(roleId);
		if (role == null) {
			result.put(ErrorCode.RESULT, ErrorCode.FAILED);
			result.put(ErrorCode.CODE, ErrorCode.ROLE_NOT_EXIST);
			return result;
		}

		logger.info("sign operate start...roleId={}, name={}, signType={},", roleId, role.getName(), signType);

		int roomNum = role.getRoomNum();
		MahjongRoom room = rooms.get(roomNum);
		if (room == null) {
			result.put(ErrorCode.RESULT, ErrorCode.FAILED);
			result.put(ErrorCode.CODE, ErrorCode.ROLE_NOT_IN_ROOM);
			return result;
		}

		int index = room.getIndexByRoleId(roleId);

		Map<Integer, RoleCache> map = room.getRoleCacheMap();
		RoleCache cache = map.get(index);
		if (cache == null) {
			result.put(ErrorCode.RESULT, ErrorCode.FAILED);
			result.put(ErrorCode.CODE, ErrorCode.OPERATE_ERROR);
			return result;
		}

		Map<Long, SignVo> signMap = room.getSignMap();
		int peng = MahjongConstant.MAHJONG_PENG;
		int hu = MahjongConstant.MAHJONG_HU;
		int minggang = MahjongConstant.MAHJONG_MINGGANG;
		int guolugang = MahjongConstant.MAHJONG_GUOLUGANG;
		if (signType == peng || signType == hu || signType == minggang || signType == guolugang) {
			if (MapUtils.isEmpty(signMap)) {
				logger.error("peng or gang or hu or guolugang is null,roleId={},signType={}", roleId, signType);
				result.put(ErrorCode.RESULT, ErrorCode.FAILED);
				result.put(ErrorCode.CODE, ErrorCode.DATA_ERROR);
				return result;
			}
			if (signMap.get(roleId) == null) {
				StringBuffer sb = new StringBuffer();
				for (Entry<Long, SignVo> e : signMap.entrySet()) {
					SignVo v = e.getValue();
					sb.append("roleId=").append(e.getKey()).append(",already operate signType=")
					        .append(v.getOperateSign()).append("signList=").append(v.getSignList().toString())
					        .append(",");

					logger.error("sign operate error, roleId={}, alreadySignType={},signMap={}", e.getKey(),
					        v.getOperateSign(), sb.toString());
				}
				// 把错误暴露出来把
				result.put(ErrorCode.RESULT, ErrorCode.FAILED);
				result.put(ErrorCode.CODE, ErrorCode.SIGN_OPERATE_ERROR);
				return result;
			}
		}

		if (MapUtils.isNotEmpty(signMap)) {
			SignVo vo = signMap.get(roleId);
			vo.setOperateSign(signType);

			if (signType == hu && vo.getSignList().contains(hu)) {
				common3(roleId, signType);
			}

			// 胡牌的玩家是否都操作了标签
			boolean huOperate = isSignOperateHu(signMap);
			if (huOperate == false) {
				logger.info("sign operate not finish,curRoleId={},signType={}", roleId, signType);
				return null;
			} else {
				// 都操作完以后。查看是否有人点了胡
				List<Long> huList = isSignHu(signMap);
				if (CollectionUtils.isNotEmpty(huList)) {
					// 这里要记录下一把开始的时候。是谁先抓牌，一个人胡的时候。下把就是胡的人抓牌，一炮多响。就是放炮的人抓牌
					int operateIndex = room.getOperateIndex();
					int huSize = huList.size();
					if (huSize == 1) {
						operateIndex = room.getIndexByRoleId(huList.get(0));
					}
					// 弹出结算面板
					oneEndOutput(room, operateIndex, MahjongConstant.CUR_JU_END_HU, huList.toArray());
					return null;
				} else {
					Object pg[] = null;
					if (signType == MahjongConstant.MAHJONG_GUOLUGANG) {
						// 这里处理过路杠业务
						pg = getSignOperateGLG(signMap);
					} else {
						// 没有人操作胡，肯定是碰或者明杠。而且只有一个玩家，如果为null。说明碰的那个玩家点了过
						pg = getSignOperatePG(signMap);
					}
					if (pg != null) {
						roleId = Long.valueOf(pg[0].toString());
						signType = Integer.valueOf(pg[1].toString());
					}
				}
			}
		}

		if (signType == peng || signType == minggang) {
			common1(roleId, signType);
		} else if (signType == MahjongConstant.MAHJONG_ANGANG || signType == guolugang) {
			common2(roleId, signType);
		} else if (signType == MahjongConstant.MAHJONG_ZIMO) {
			common3(roleId, signType);
		} else if (signType == MahjongConstant.MAHJONG_GUO) {
			guo(roleId, signType);
		}

		cache.setTan(false);
		logger.info("sign operate end...roleId={}, name={}, signType={}", roleId, role.getName(), signType);

		return null;
	}

	/**
	 * 胡牌的人是否都点击过标签
	 *
	 * @param signMap
	 * @return 只要有一个没点击。返回false。没有胡牌的玩家。也会返回true
	 */
	private boolean isSignOperateHu(Map<Long, SignVo> signMap) {
		boolean flag = true;
		for (Entry<Long, SignVo> e : signMap.entrySet()) {
			SignVo vo = e.getValue();
			if (vo.getSignList().contains(MahjongConstant.MAHJONG_HU) && vo.getOperateSign() == 0) {
				flag = false;
				break;
			}
		}
		return flag;
	}

	/**
	 * 有标签操作的人是否都点击过标签
	 *
	 * @param signMap
	 * @return 只要有一个没点击。返回false。没有胡牌的玩家。也会返回true
	 */
	private boolean isSignOperateGuo(Map<Long, SignVo> signMap) {
		boolean flag = true;
		for (Entry<Long, SignVo> e : signMap.entrySet()) {
			SignVo vo = e.getValue();
			if (vo.getOperateSign() != 7) {
				flag = false;
				break;
			}
		}
		return flag;
	}

	/**
	 * 当前操作种是否有人操作了胡
	 *
	 * @param signMap
	 * @return
	 */
	private List<Long> isSignHu(Map<Long, SignVo> signMap) {
		List<Long> huList = new ArrayList<>();
		int hu = MahjongConstant.MAHJONG_HU;
		for (Entry<Long, SignVo> e : signMap.entrySet()) {
			SignVo vo = e.getValue();
			if (vo.getSignList().contains(hu) && vo.getOperateSign() == hu) {
				huList.add(e.getKey());
			}
		}
		return huList;
	}

	/**
	 * 获得碰或杠的人(当前操作只有一个人)
	 *
	 * @param signMap
	 * @return
	 */
	private Object[] getSignOperatePG(Map<Long, SignVo> signMap) {
		int peng = MahjongConstant.MAHJONG_PENG;
		int minggang = MahjongConstant.MAHJONG_MINGGANG;
		for (Entry<Long, SignVo> e : signMap.entrySet()) {
			SignVo vo = e.getValue();
			if ((vo.getSignList().contains(peng) || vo.getSignList().contains(minggang))
			        && (vo.getOperateSign() == peng || vo.getOperateSign() == minggang)) {
				return new Object[] { e.getKey(), vo.getOperateSign() };
			}
		}
		return null;
	}

	/**
	 * 获得过路杠的人。
	 *
	 * @param signMap
	 * @return
	 */
	private Object[] getSignOperateGLG(Map<Long, SignVo> signMap) {
		int guolugang = MahjongConstant.MAHJONG_GUOLUGANG;
		for (Entry<Long, SignVo> e : signMap.entrySet()) {
			SignVo vo = e.getValue();
			if (vo.getSignList().contains(guolugang) && vo.getOperateSign() == guolugang) {
				return new Object[] { e.getKey(), vo.getOperateSign() };
			}
		}
		return null;
	}

	/**
	 * 过
	 *
	 * @param roleId
	 */
	public void guo(long roleId, int signType) {
		Role role = roleManager.getRole(roleId);
		int roomNum = role.getRoomNum();
		MahjongRoom room = rooms.get(roomNum);
		int index = room.getIndexByRoleId(roleId);
		RoleCache roleCache = room.getRoleCacheByRoleId(roleId);

		JSONObject result = new JSONObject();
		result.put(ErrorCode.RESULT, ErrorCode.EC_OK);
		result.put(MahjongConstant.CLIENT_DATA,
		        new Object[] { roleId, index, signType, null, roleCache.getHeadChess().size() });
		routeManager.relayMsg(roleId, MsgType.MAHJONG_CHESS_SIGN_MSG, result);
		// 记录回放标签操作
		addPlayBack(room, MahjongConstant.PLAY_BACK_TYPE_SIGN_OPERATE, roleId, index, new Object[] { signType, null });

		Map<Long, SignVo> signMap = room.getSignMap();
		if (isSignOperateGuo(signMap)) {
			boolean isMo = room.getOperateIndex() == index;
			// 如果是自己摸的牌。点了过。就自己打牌。不是自己摸的，就该打牌的下一个玩家摸牌
			if (!isMo) {
				room.resetOperateIndex(0);
				// 给客户端推送下一个人摸到的牌
				moChess(room.getRoleIdByIndex(room.getOperateIndex()), true);
			}
		} else {
			for (long id : signMap.keySet()) {
				if (signMap.get(id).getOperateSign() != 0) {
					continue;
				}
				JSONObject resultData = new JSONObject();
				resultData.put(ErrorCode.RESULT, ErrorCode.OK);
				resultData.put(MahjongConstant.CLIENT_DATA, signMap.get(id).getSignList());
				roleCache.setTan(true);
				routeManager.relayMsg(id, MsgType.MAHJONG_SHOW_MSG, resultData);
			}
		}
	}

	/**
	 * 胡和自摸的通用方法
	 *
	 * @param roleId
	 * @param signType
	 */
	private void common3(long roleId, int signType) {
		Role role = roleManager.getRole(roleId);
		int roomNum = role.getRoomNum();
		MahjongRoom room = rooms.get(roomNum);
		int index = room.getIndexByRoleId(roleId);
		boolean isMo = room.getOperateIndex() == index;

		RoleCache roleCache = room.getRoleCacheByRoleId(roleId);
		RoleCache temp = null;
		Integer hu = null;
		int operateIndex = room.getOperateIndex();
		ChessEvery curChess = room.getCurrChessEvery();
		// 有过路杠的人当前牌要用过路杠的那人的牌
		boolean checkGuolu = checkGuoLuGang(room.getSignMap());
		if (checkGuolu) {
			curChess = room.getCurrMoChessEvery();
		}
		if (signType == MahjongConstant.MAHJONG_HU && !isMo) {
			temp = getRoleCacheTemp(roleCache, curChess);
			hu = isWin(temp);
		} else if (signType == MahjongConstant.MAHJONG_ZIMO && isMo) {
			temp = getRoleCacheTemp(roleCache, null);
			hu = isWin(temp);
		}

		if (hu != null) {
			if (room.getFan() > 0) {
				FanTemplate fanTempGang = mahjongDataTemplateManager.getFanTemplate(room.getFan());
				room.setFan(fanTempGang.getFan());
			}
			FanTemplate fanTempHu = mahjongDataTemplateManager.getFanTemplate(hu);
			room.setFan(room.getFan() + fanTempHu.getFan()); // 此处才是最终的番薯

			ChessJifenEvent.publish(roleId, roomNum, signType, operateIndex);

			JSONObject result = new JSONObject();
			result.put(ErrorCode.RESULT, ErrorCode.EC_OK);
			result.put(MahjongConstant.CLIENT_DATA,
			        new Object[] { roleId, index, signType, null, roleCache.getHeadChess().size() });
			routeManager.broadcast(room.getAllRoleId(), MsgType.MAHJONG_CHESS_SIGN_MSG, result);

			// 记录回放标签操作
			addPlayBack(room, MahjongConstant.PLAY_BACK_TYPE_SIGN_OPERATE, roleId, index,
			        new Object[] { signType, null });

			if (signType == MahjongConstant.MAHJONG_HU) {
				// 胡的这张牌放到身上
				roleCache.addRoleChess(curChess, false);
				// 打牌的这个人移掉这张牌
				RoleCache outRoleCache = room.getRoleCacheMap().get(operateIndex);
				outRoleCache.removeOutChess(curChess);

				// 计算接炮和点炮次数
				roleCache.setJiePaoCount(roleCache.getJiePaoCount() + 1);
				outRoleCache.setDianPaoCount(outRoleCache.getDianPaoCount() + 1);
			} else if (signType == MahjongConstant.MAHJONG_ZIMO) {
				// 计算自摸次数
				roleCache.setZiMoCount(roleCache.getZiMoCount() + 1);

				oneEndOutput(room, operateIndex, MahjongConstant.CUR_JU_END_ZIMO,
				        new Object[] { roleCache.getRoleId() });
			}
		}
	}

	/**
	 * 查看当前有没有人过路杠
	 *
	 * @param signMap
	 * @return
	 */
	private boolean checkGuoLuGang(Map<Long, SignVo> signMap) {
		for (Entry<Long, SignVo> e : signMap.entrySet()) {
			if (e.getValue().getSignList().contains(MahjongConstant.MAHJONG_GUOLUGANG)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 暗杠和过路杠的通用方法
	 *
	 * @param roleId
	 * @param signType
	 */
	private void common2(long roleId, int signType) {
		Role role = roleManager.getRole(roleId);
		int roomNum = role.getRoomNum();
		MahjongRoom room = rooms.get(roomNum);
		int index = room.getIndexByRoleId(roleId);
		boolean isMo = room.getOperateIndex() == index;

		if (isMo) {
			RoleCache roleCache = room.getRoleCacheByRoleId(roleId);
			List<ChessEvery> chessList = roleCache.getRoleChessList();
			List<ChessEvery> list = null;
			ChessEvery moChess = null;
			int guolugang = MahjongConstant.MAHJONG_GUOLUGANG;
			int angang = MahjongConstant.MAHJONG_ANGANG;
			if (signType == angang) {
				list = angang(chessList);
			} else if (signType == guolugang) {
				moChess = room.getCurrMoChessEvery();
				list = guolugang(chessList, moChess);
			}

			if (list != null) {
				// 给客户端推送过路杠或暗杠的牌
				JSONObject result = new JSONObject();
				result.put(ErrorCode.RESULT, ErrorCode.EC_OK);
				result.put(MahjongConstant.CLIENT_DATA,
				        new Object[] { roleId, index, signType, allChessVo(list), roleCache.getHeadChess().size() });
				routeManager.broadcast(room.getAllRoleId(), MsgType.MAHJONG_CHESS_SIGN_MSG, result);

				// 记录回放标签操作
				addPlayBack(room, MahjongConstant.PLAY_BACK_TYPE_SIGN_OPERATE, roleId, index,
				        new Object[] { signType, allChessVo(list) });

				// 过路杠。要看有没有人胡这张。会抢杠的
				/*
				 * if (signType == guolugang && roleCache.isGuolugang()) { //
				 * 验证其他三个玩家是否会胡这张牌 long qianggangRoleId = 0; for (Entry<Integer,
				 * RoleCache> e : room.getRoleCacheMap().entrySet()) { RoleCache
				 * cache = e.getValue(); if (cache.getRoleId() == roleId) {
				 * continue; } List<Integer> signList = check(cache, false,
				 * moChess, false); if
				 * (signList.contains(MahjongConstant.MAHJONG_HU)) {
				 * room.setFan(MahjongConstant.MAHJONG_HU_TYPE_QIANGGANG); //
				 * 记录胡牌的人 qianggangRoleId = cache.getRoleId();
				 * room.addSignMap(qianggangRoleId, new SignVo(signList, 0)); }
				 * } roleCache.setGuolugang(false);
				 * 
				 * if (room.getFan() != 0 && qianggangRoleId != 0) { //
				 * 弹了胡的提示。当前过路杠的人就不能自己摸牌了,过路杠的这张牌也不是自己的。并且也没有积分
				 * logger.info("过路杠被人抢杠了。抢杠的人roleid={},过路杠的人roleid={}",
				 * qianggangRoleId, roleId); return; } }
				 */
				if (signType == angang) {
					roleCache.setAnGangCount(roleCache.getAnGangCount() + 1);
					roleCache.setOneAnGangCount(roleCache.getOneAnGangCount() + 1);
				} else if (signType == guolugang) { // 过路杠和明杠统一进明杠
					roleCache.setMingGangCount(roleCache.getMingGangCount() + 1);
					roleCache.setOneMingGangCount(roleCache.getOneMingGangCount() + 1);
				}

				// JiFenTemplate template =
				// mahjongDataTemplateManager.getJiFenTemplate(signType);
				// roleCache.setFan(template.getFan() + roleCache.getFan());

				changeChess(roleCache, list, null);
				// ChessJifenEvent.publish(roleId, roomNum, signType, 0);

				// 从后面开始摸牌
				moChess(roleId, false);
				// 摸完牌以后。记录他从后面摸牌了,这里如果摸到牌胡了。就是杠开
				room.setFan(MahjongConstant.MAHJONG_HU_TYPE_GANGKAI);
			}
		}
	}

	/**
	 * 吃
	 *
	 * @param roleId
	 * @param firstPaiId
	 * @param nextPaiId
	 * @return
	 */
	public JSONObject eat(long roleId, int firstPaiId, int nextPaiId) {
		JSONObject result = new JSONObject();
		result.put(ErrorCode.RESULT, ErrorCode.OK);

		Role role = roleManager.getRole(roleId);
		if (role == null) {
			result.put(ErrorCode.RESULT, ErrorCode.FAILED);
			result.put(ErrorCode.CODE, ErrorCode.ROLE_NOT_EXIST);
			return result;
		}

		int roomNum = role.getRoomNum();
		MahjongRoom room = rooms.get(roomNum);
		if (room == null) {
			result.put(ErrorCode.RESULT, ErrorCode.FAILED);
			result.put(ErrorCode.CODE, ErrorCode.ROLE_NOT_IN_ROOM);
			return result;
		}

		Map<Integer, RoleCache> map = room.getRoleCacheMap();
		int index = room.getIndexByRoleId(roleId);
		RoleCache roleCache = map.get(index);

		ChessEvery firstPai = roleCache.getChessEveryById(firstPaiId);
		ChessEvery nextPai = roleCache.getChessEveryById(nextPaiId);
		if (firstPai == null || nextPai == null) {
			result.put(ErrorCode.RESULT, ErrorCode.FAILED);
			result.put(ErrorCode.CODE, ErrorCode.ROLE_NOT_THIS_EVERY);
			return result;
		}
		if (firstPai.getNumber() == nextPai.getNumber() && firstPai.getColor() == nextPai.getColor()) {
			// 吃不存在有两张相同的牌来操作啊，亲
			result.put(ErrorCode.RESULT, ErrorCode.FAILED);
			result.put(ErrorCode.CODE, ErrorCode.EAT_USE_IDENTICAL);
			return result;
		}

		ChessEvery currChessEvery = room.getCurrChessEvery();
		List<ChessEvery> currChessRelation = getRelationChess(currChessEvery);
		if (!chessListContainsChess(currChessRelation, firstPai)) {
			result.put(ErrorCode.RESULT, ErrorCode.FAILED);
			result.put(ErrorCode.CODE, ErrorCode.OPERATE_ERROR);
			return result;
		}
		if (!chessListContainsChess(currChessRelation, nextPai)) {
			result.put(ErrorCode.RESULT, ErrorCode.FAILED);
			result.put(ErrorCode.CODE, ErrorCode.OPERATE_ERROR);
			return result;
		}

		List<ChessEvery> list = new ArrayList<>();
		list.add(firstPai);
		list.add(nextPai);
		// 把这张牌的id也给客户端返回过去
		list.add(currChessEvery);
		// 有碰 把这张牌放到自己身上，改变牌的属性
		changeChess(roleCache, list, currChessEvery);
		// 把打出来这张牌的人。从他的打出牌列表里删掉
		RoleCache outRoleCache = map.get(room.getOperateIndex());
		outRoleCache.removeOutChess(currChessEvery);

		roleCache.setOneEatCount(roleCache.getOneEatCount() + 1); // 吃只加一次
		// 吃加一番
		JiFenTemplate template = mahjongDataTemplateManager.getJiFenTemplate(MahjongConstant.MAHJONG_EAT);
		roleCache.setFan(template.getFan() + roleCache.getFan());

		room.resetOperateIndex(index);

		Object[] data = new Object[] { roleId, index, MahjongConstant.MAHJONG_EAT, allChessVo(sortChess(list)),
		        roleCache.getHeadChess().size() };
		result.put(MahjongConstant.CLIENT_DATA, data);
		routeManager.broadcast(room.getAllRoleId(), MsgType.MAHJONG_CHESS_SIGN_MSG, result);

		roleCache.setTan(false);
		return null;
	}

	/**
	 * 返回大厅
	 * 
	 * @param roleId
	 * @return
	 */
	public JSONObject returnHall(long roleId) {
		JSONObject res = new JSONObject();
		res.put(ErrorCode.RESULT, ErrorCode.OK);
		Role role = roleManager.getRole(roleId);
		if (role == null) {
			res.put(ErrorCode.RESULT, ErrorCode.FAILED);
			res.put(ErrorCode.CODE, ErrorCode.ROLE_NOT_EXIST);
			return res;
		}

		int roomNum = role.getRoomNum();
		MahjongRoom room = rooms.get(roomNum);
		if (room == null) {
			res.put(ErrorCode.RESULT, ErrorCode.FAILED);
			res.put(ErrorCode.CODE, ErrorCode.ROLE_NOT_IN_ROOM);
			return res;
		}
		RoleCache roleCache = room.getRoleCacheByRoleId(roleId);
		roleCache.setInRoom(false);
		res.put(MahjongConstant.CLIENT_DATA, new Object[] { roleId, room.getIndexByRoleId(roleId) });
		routeManager.broadcast(room.getAllRoleId(), MsgType.MAHJONG_RETURN_HALL, res);
		return null;
	}

	/**
	 * 返回房间
	 * 
	 * @param roleId
	 * @return
	 */
	public JSONObject returnRoom(long roleId) {
		JSONObject res = new JSONObject();
		res.put(ErrorCode.RESULT, ErrorCode.OK);
		Role role = roleManager.getRole(roleId);
		if (role == null) {
			res.put(ErrorCode.RESULT, ErrorCode.FAILED);
			res.put(ErrorCode.CODE, ErrorCode.ROLE_NOT_EXIST);
			return res;
		}

		int roomNum = role.getRoomNum();
		MahjongRoom room = rooms.get(roomNum);
		if (room == null) {
			res.put(ErrorCode.RESULT, ErrorCode.FAILED);
			res.put(ErrorCode.CODE, ErrorCode.ROLE_NOT_IN_ROOM);
			return res;
		}
		RoleCache roleCache = room.getRoleCacheByRoleId(roleId);
		roleCache.setInRoom(true);
		res.put(MahjongConstant.CLIENT_DATA, new Object[] { roleId, room.getIndexByRoleId(roleId) });
		routeManager.broadcast(room.getAllRoleId(), MsgType.MAHJONG_RETURN_ROOM, res);

		LoginGameEvent.publish(role.getId(), role.getIp());
		return null;
	}

	/**
	 * 碰和明杠的通用方法
	 *
	 * @param roleId
	 * @param signType
	 */
	private void common1(long roleId, int signType) {
		Role role = roleManager.getRole(roleId);
		int roomNum = role.getRoomNum();

		MahjongRoom room = rooms.get(roomNum);

		// 开始碰或杠操作
		int index = room.getIndexByRoleId(roleId);
		int oldOperateIndex = room.getOperateIndex();
		boolean isMo = oldOperateIndex == index;

		// 不是自己摸牌
		if (!isMo) {
			Map<Integer, RoleCache> map = room.getRoleCacheMap();
			RoleCache roleCache = room.getRoleCacheByRoleId(roleId);
			List<ChessEvery> chessList = roleCache.getRoleChessList();
			ChessEvery currChessEvery = room.getCurrChessEvery();
			List<ChessEvery> list = null;

			if (signType == MahjongConstant.MAHJONG_PENG) {
				list = peng(chessList, currChessEvery);
				roleCache.setOnePengCount(roleCache.getOnePengCount() + 1);
			} else if (signType == MahjongConstant.MAHJONG_MINGGANG) {
				list = minggang(chessList, currChessEvery);
			}

			if (list != null) {
				// 把这张牌的id也给客户端返回过去
				list.add(currChessEvery);
				// 有碰 把这张牌放到自己身上，改变牌的属性
				changeChess(roleCache, list, currChessEvery);
				// 把打出来这张牌的人。从他的打出牌列表里删掉
				RoleCache outRoleCache = map.get(room.getOperateIndex());
				outRoleCache.removeOutChess(currChessEvery);

				room.resetOperateIndex(index);

				// 给客户端推送碰或杠的牌
				JSONObject result = new JSONObject();
				result.put(ErrorCode.RESULT, ErrorCode.EC_OK);
				result.put(MahjongConstant.CLIENT_DATA,
				        new Object[] { roleId, index, signType, allChessVo(list), roleCache.getHeadChess().size() });
				routeManager.broadcast(room.getAllRoleId(), MsgType.MAHJONG_CHESS_SIGN_MSG, result);

				// 记录回放标签操作
				addPlayBack(room, MahjongConstant.PLAY_BACK_TYPE_SIGN_OPERATE, roleId, room.getOperateIndex(),
				        new Object[] { signType, allChessVo(list) });

				if (signType == MahjongConstant.MAHJONG_MINGGANG) {
					// 计算明杠次数
					roleCache.setMingGangCount(roleCache.getMingGangCount() + 1);
					roleCache.setOneMingGangCount(roleCache.getOneMingGangCount() + 1);
					// ChessJifenEvent.publish(roleId, roomNum, signType,
					// oldOperateIndex);

					// 明杠从后面开始摸牌
					moChess(roleId, false);
					// 摸完牌以后。记录他从后面摸牌了 这里如果摸到牌胡了。就是杠开
					room.setFan(MahjongConstant.MAHJONG_HU_TYPE_GANGKAI);
				}

				// JiFenTemplate template =
				// mahjongDataTemplateManager.getJiFenTemplate(signType);
				// roleCache.setFan(template.getFan() + roleCache.getFan());
			}
		}
	}

	private List<ChessEvery> sortChess(List<ChessEvery> list) {
		Collections.sort(list, new Comparator<ChessEvery>() {
			@Override
			public int compare(ChessEvery c1, ChessEvery c2) {
				if (c1.getColor() < c2.getColor()) {
					return -1;
				}
				if (c1.getColor() == c2.getColor() && c1.getNumber() < c2.getNumber()) {
					return -1;
				}
				return 0;
			}
		});
		return list;
	}

	/**
	 * 每一句的结算数据
	 * 
	 * @param room
	 * @return
	 */
	private JSONObject oneEnd(MahjongRoom room, Object[] winRoleIds) {
		JSONObject result = new JSONObject();
		result.put(ErrorCode.RESULT, ErrorCode.EC_OK);
		List<Object[]> list = new ArrayList<>();
		for (Entry<Integer, RoleCache> e : room.getRoleCacheMap().entrySet()) {
			RoleCache cache = e.getValue();
			list.add(new Object[] { cache.getRoleId(), cache.getChangeJifen(),
			        allChessVo(excludeOutSpecialChess(cache.getRoleChessList(), room)) });
		}
		result.put(MahjongConstant.CLIENT_DATA, new Object[] { winRoleIds, list.toArray() });
		return result;
	}

	/**
	 * 每一局的结束面板
	 *
	 * @param room
	 * @param operateIndex
	 *            下把谁第一个摸牌的人
	 * @param endType
	 *            结束的类型
	 */
	private void oneEndOutput(MahjongRoom room, int operateIndex, int endType, Object[] winRoleIds) {

		room.resetOperateIndex(operateIndex);

		// 8局打完。推送总战绩面板
		if (room.getAlreadyJuShu() >= room.getJushu()) {
			room.setSumScorePanel(true);
		}
		// 记录当前局结束
		room.setCurEnd(true);

		// 记录战绩
		RoleCache cache = room.getRoleCacheByIndex(operateIndex);
		createZhanji(room, cache.getCurJuLogId());

		logger.info("cur ju end,roomNum={},di{}ju,endType={}, winRoleId={},name={},loseList={}", room.getRoomNum(),
		        room.getAlreadyJuShu(), endType, cache.getRoleId(), cache.getName(),
		        loseListStr(cache.getRoleId(), room.getRoleCacheMap()));
		if (room.isSumScorePanel()) {
			pushAllSumScore(room);
			return;
		}

		if (endType == MahjongConstant.CUR_JU_END_HU || endType == MahjongConstant.CUR_JU_END_ZIMO) {
			routeManager.broadcast(room.getAllRoleId(), MsgType.MAHJONG_HU_MSG, oneEnd(room, winRoleIds));
		} else {
			routeManager.broadcast(room.getAllRoleId(), MsgType.MAHJONG_HU_MSG, oneEnd(room, null)); //
		}
	}

	private String loseListStr(long winRoleId, Map<Integer, RoleCache> map) {
		String strs = "";
		for (Entry<Integer, RoleCache> e : map.entrySet()) {
			RoleCache cache = e.getValue();
			if (cache.getRoleId() == winRoleId) {
				continue;
			}
			strs += cache.getRoleId() + "," + cache.getName() + "; ";
		}
		return strs;
	}

	/**
	 * 创建战绩
	 *
	 * @param room
	 * @param curJuId
	 */
	private void createZhanji(MahjongRoom room, long curJuId) {
		boolean logId = false;
		MahjongResultLog log = mahjongResultLogRepository.cacheLoad(curJuId);
		if (log == null) {
			log = createMahjongResultLog(room.getRoomNum(), room.getCreateDate());
			logId = true;
		}
		MahjongResultData data = log.getAllInfo().get(room.getAlreadyJuShu());
		if (data == null) {
			data = new MahjongResultData();
			data.setStartTime(room.getCurJuStartDate());
			log.getAllInfo().put(room.getAlreadyJuShu(), data);
		}
		// 创建回放记录
		createPlayBackFile(log.getId(), log.getRoomNum(), room.getAlreadyJuShu(), log.getAddTime(),
		        room.getPlayBackMap());

		List<MahjongResultDetailsData> detailsDatas = data.getInfos();
		for (Entry<Integer, RoleCache> e : room.getRoleCacheMap().entrySet()) {
			RoleCache cache = e.getValue();
			if (logId) {
				cache.setCurJuLogId(log.getId());
			}
			MahjongResultDetailsData detailsData = new MahjongResultDetailsData();
			detailsData.setName(cache.getName());
			detailsData.setRoleId(cache.getRoleId());
			detailsData.setJifen(cache.getChangeJifen());
			detailsDatas.add(detailsData);
		}
		mahjongResultLogRepository.update(log);
	}

	/**
	 * 创建回放操作记录
	 *
	 * @param id
	 * @param roomNum
	 * @param jushu
	 * @param addTime
	 * @param playBackMap
	 */
	private void createPlayBackFile(long id, int roomNum, int jushu, Date addTime,
	        Map<Integer, PlayBackVo> playBackMap) {
		String json = JSON.toJSONString(playBackMap);
		String fileName = getPlayBackFileName(id, roomNum, jushu, addTime);
		String filePath = getFilePath(fileName);
		FileUtil.write(filePath, json.getBytes());
	}

	/**
	 * 获取当前文件。不存在。不创建
	 *
	 * @param fileName
	 * @return
	 */
	private String getFilePath(String fileName) {
		String path = GameServerContext.getAppConfig().getPlaybacklocal();
		return new StringBuffer().append(path).append(File.separator).append(fileName).append(".log").toString();
	}

	/**
	 * 获取文件名
	 *
	 * @param id
	 * @param roomNum
	 * @param addTime
	 * @return
	 */
	private String getPlayBackFileName(long id, int roomNum, int jushu, Date addTime) {
		String timeStr = TimeUtil.format(addTime, TimeUtil.PATTERN_yyyyMMdd);
		return new StringBuffer().append(timeStr).append("-").append(id).append("-").append(roomNum).append("-")
		        .append(jushu).toString();
	}

	/**
	 * 创建麻将战绩日志
	 *
	 * @param roomNum
	 * @return
	 */
	private MahjongResultLog createMahjongResultLog(int roomNum, Date roomCreateDate) {
		MahjongResultLog resultLog = new MahjongResultLog();
		resultLog.setId(idManager.newId(TableNameConstant.MAHJONG_RESULT_LOG));
		resultLog.setRoomNum(roomNum);
		resultLog.setAddTime(roomCreateDate);
		resultLog.setModifyTime(new Date());
		mahjongResultLogRepository.insert(resultLog);
		return resultLog;
	}

	/**
	 * 生成一个临时RoleCache
	 *
	 * @param cache
	 * @param currChessEvery
	 *            当前牌为null，不加入到缓存里
	 * @return
	 */
	private RoleCache getRoleCacheTemp(RoleCache cache, ChessEvery currChessEvery) {
		RoleCache temp = new RoleCache();
		List<ChessEvery> list = new ArrayList<>();
		list.addAll(cache.getRoleChessList());
		// 不能这样写temp = cache;。堆栈信息指向的还是同一个地址
		temp.setRoleId(cache.getRoleId());
		temp.setName(cache.getName());
		temp.setJifen(0);
		temp.copyRoleChess(list);
		temp.setOneEatCount(cache.getOneEatCount());
		if (currChessEvery != null) {
			temp.addRoleChess(currChessEvery, true);
		}
		return temp;
	}

	/**
	 * 改变牌的属性，把used变成true
	 *
	 * @param cache
	 * @param ids
	 * @param currChessEvery
	 */
	private void changeChess(RoleCache cache, List<ChessEvery> ids, ChessEvery currChessEvery) {
		if (currChessEvery != null) {
			cache.addRoleChess(currChessEvery, true);
		}

		List<ChessEvery> list = cache.getRoleChessList();
		for (ChessEvery ce : list) {
			if (ids.contains(ce)) {
				ce.setUsed(true);
			}
		}
	}

	/**
	 * 创建房间
	 *
	 * @param roleId
	 * @param jushu
	 * @param zimohu
	 * @param feng
	 * @param hz
	 * @param yu
	 * @param roomId
	 * @return
	 */
	public JSONObject createRoom(long roleId, int jushu, int playType, int roomId) {

		JSONObject result = new JSONObject();
		result.put(ErrorCode.RESULT, ErrorCode.OK);

		// 检查roleId是否有数据
		Role role = roleManager.getRole(roleId);
		if (role == null) {
			result.put(ErrorCode.RESULT, ErrorCode.FAILED);
			result.put(ErrorCode.CODE, ErrorCode.ROLE_NOT_EXIST);
			return result;
		}
		Integer costCar = configDataTemplateManager.getCostCarTemp(jushu);
		if (costCar == null) {
			result.put(ErrorCode.RESULT, ErrorCode.FAILED);
			result.put(ErrorCode.CODE, ErrorCode.COST_CAR_ERROR);
			return result;
		}
		// 验证是否已创建房间
		if (role.getRoomNum() != 0) {
			result.put(ErrorCode.RESULT, ErrorCode.FAILED);
			result.put(ErrorCode.CODE, ErrorCode.ROOM_EXIST_NOT_CREATE);
			return result;
		}

		// 房间号
		int roomNum = 0;
		if (roomId == 0) {
			roomNum = randomRoomNum();
		} else {
			roomNum = roomId;
		}

		if (rooms.containsKey(roomNum)) {
			result.put(ErrorCode.RESULT, ErrorCode.FAILED);
			result.put(ErrorCode.CODE, ErrorCode.ROOM_EXIST);
			return result;
		}

		MahjongRoom room = rooms.get(roomNum);
		if (room == null) {
			room = new MahjongRoom();
			room.setRoomNum(roomNum);
			room.setJushu(jushu);
			room.setHz(true); // 默认红中癞子
			room.setPlayType(playType);
			rooms.put(roomNum, room);
		}

		room.setLeaderId(roleId);
		// 添加房间成员
		room.addMember(createRoleCache(roleId, role.getName(), role.getIp(), role.getHeadimgurl()));
		// 操作索引
		room.setOperateIndex(room.getMaxIndex());
		room.setCreateDate(new Date());

		RoleCache roleCache = room.getRoleCacheByRoleId(roleId);
		roleCache.setInRoom(true);

		role.setRoomNum(roomNum);
		role.setState(StateType.WAIT.getVal());
		roleRepository.cacheUpdate(role);

		logger.info("create room success,leaderId={},roomNum={}", roleId, roomNum);

		result.put(MahjongConstant.CLIENT_DATA, new Object[] { roomNum, jushu });
		return result;
	}

	/**
	 * 检测玩法参数的合法性
	 *
	 * @param paras
	 * @return
	 */
	private JSONObject checkWanFaPara(int... paras) {
		JSONObject result = new JSONObject();
		for (int para : paras) {
			if (para != 1 && para != 2) {
				result.put(ErrorCode.RESULT, ErrorCode.FAILED);
				result.put(ErrorCode.CODE, ErrorCode.OPERATE_ERROR);
				return result;
			}
		}
		return null;
	}

	/**
	 * 解散房间
	 *
	 * @param roleId
	 * @return
	 */
	public JSONObject dissolvedRoom(long roleId) {
		JSONObject result = new JSONObject();
		result.put(ErrorCode.RESULT, ErrorCode.OK);

		logger.info("dissolveRoom start, roleId={}", roleId);
		Role role = roleManager.getRole(roleId);
		if (role == null) {
			result.put(ErrorCode.RESULT, ErrorCode.FAILED);
			result.put(ErrorCode.CODE, ErrorCode.ROLE_NOT_EXIST);
			return result;
		}
		int roomNum = role.getRoomNum();
		if (roomNum == 0) {
			result.put(ErrorCode.RESULT, ErrorCode.FAILED);
			result.put(ErrorCode.CODE, ErrorCode.ROOM_NOT_EXIST_NOT_DIS);
			return result;
		}
		MahjongRoom room = rooms.get(roomNum);
		if (room == null) {
			result.put(ErrorCode.RESULT, ErrorCode.FAILED);
			result.put(ErrorCode.CODE, ErrorCode.ROOM_NOT_EXIST);
			return result;
		}

		if (room.getLeaderId() != roleId) {
			result.put(ErrorCode.RESULT, ErrorCode.FAILED);
			result.put(ErrorCode.CODE, ErrorCode.NOT_CREATE_LEADER);
			return result;
		}

		if (room.isStart()) {
			result.put(ErrorCode.RESULT, ErrorCode.FAILED);
			result.put(ErrorCode.CODE, ErrorCode.GAME_AREADY_START);
			return result;
		}

		long allRoleIds[] = removeRoom(roomNum);

		// 告诉所有玩家 房间已经解散了。
		Object[] data = new Object[] { role.getName(), roleId };
		result.put(MahjongConstant.CLIENT_DATA, data);
		routeManager.broadcast(allRoleIds, MsgType.DISSOLVED_ROOM_MSG, result);

		logger.info("dissolveRoom end, roleId={}, name={}, roomNum={}", roleId, role.getName(), roomNum);
		return null;
	}

	/**
	 * 移除房间
	 *
	 * @param roomNum
	 * @return
	 */
	public long[] removeRoom(int roomNum) {
		MahjongRoom roomRemove = rooms.remove(roomNum);
		if (roomRemove == null) {
			logger.info("GM命令，此房间已经解散了。不用继续解散,roomNum={}", roomNum);
			return null;
		}
		long allRoleIds[] = roomRemove.getAllRoleId();
		for (long id : allRoleIds) {
			Role role = roleManager.getRole(id);
			role.setRoomNum(0);
			role.setState(0);
			roleRepository.cacheUpdate(role);
		}
		this.roomIds.add(roomNum);
		return allRoleIds;
	}

	/**
	 * 加入房间
	 *
	 * @param roleId
	 * @param roomNum
	 * @return
	 */
	public JSONObject joinRoom(long roleId, int roomNum) {
		JSONObject result = new JSONObject();
		// 检查roleId是否有数据
		Role role = roleManager.getRole(roleId);
		if (role == null) {
			result.put(ErrorCode.RESULT, ErrorCode.FAILED);
			result.put(ErrorCode.CODE, ErrorCode.ROLE_NOT_EXIST);
			return result;
		}

		// 检查是否有房间
		MahjongRoom room = rooms.get(roomNum);
		if (room == null) {
			result.put(ErrorCode.RESULT, ErrorCode.FAILED);
			result.put(ErrorCode.CODE, ErrorCode.ROOM_NOT_EXIST);
			return result;
		}

		if (role.getRoomNum() != 0) {
			result.put(ErrorCode.RESULT, ErrorCode.FAILED);
			result.put(ErrorCode.CODE, ErrorCode.ROLE_IN_ROOM_NOT_JOIN);
			return result;
		}

		// 检查房间是否已经开始
		if (room.isStart()) {
			result.put(ErrorCode.RESULT, ErrorCode.FAILED);
			result.put(ErrorCode.CODE, ErrorCode.GAME_AREADY_START);
			return result;
		}

		if (room.getRoleCacheMap().size() >= ConfigConstant.ROOM_NUM) {
			result.put(ErrorCode.RESULT, ErrorCode.FAILED);
			result.put(ErrorCode.CODE, ErrorCode.OVER_ROOM_NUM);
			return result;
		}

		// 添加角色缓存数据
		room.addMember(createRoleCache(roleId, role.getName(), role.getIp(), role.getHeadimgurl()));
		// 把房间号缓存到role身上
		role.setRoomNum(roomNum);

		RoleCache roleCache = room.getRoleCacheByRoleId(roleId);
		roleCache.setInRoom(true);

		// 推送给房间的所有人。包含自己
		refreshJoinRoom(roleId, roomNum);

		role.setState(StateType.WAIT.getVal());
		roleRepository.cacheUpdate(role);

		logger.info("join room success,roleId={},roomNum={},curRoomSize={}", roleId, roomNum,
		        room.getRoleCacheMap().size());

		// 验证是否可以开始游戏
		checkStart(roleId, roomNum);

		return null;
	}

	/**
	 * 刷新房间
	 *
	 * @param roleId
	 */
	public void refreshJoinRoom(long roleId, int roomNum) {
		JSONObject result = new JSONObject();
		result.put(ErrorCode.RESULT, ErrorCode.EC_OK);

		MahjongRoom room = rooms.get(roomNum);
		Object[] data = new Object[] { roomNum, room.getJushu(), room.getLeaderId(),
		        allRoleCacheOutput(room.getRoleCacheMap()) };

		result.put(MahjongConstant.CLIENT_DATA, data);

		long[] allRoleId = room.getAllRoleId();
		long[] bradcastList = new long[allRoleId.length];
		for (int i = 0; i < allRoleId.length; i++) {
			RoleCache roleCache = room.getRoleCacheByRoleId(allRoleId[i]);
			if (roleCache.isInRoom()) {
				bradcastList[i] = roleCache.getRoleId();
			}
		}
		routeManager.broadcast(bradcastList, MsgType.JOIN_MAHJONG_MSG, result);
	}

	/**
	 * 退出房间
	 *
	 * @param roleId
	 * @return
	 */
	public JSONObject quitRoom(long roleId) {
		JSONObject result = new JSONObject();
		result.put(ErrorCode.RESULT, ErrorCode.OK);
		Role role = roleManager.getRole(roleId);
		if (role == null) {
			result.put(ErrorCode.RESULT, ErrorCode.FAILED);
			result.put(ErrorCode.CODE, ErrorCode.ROLE_NOT_EXIST);
			return result;
		}

		// 检查是否有房间
		int roomNum = role.getRoomNum();
		if (roomNum == 0) {
			result.put(ErrorCode.RESULT, ErrorCode.FAILED);
			result.put(ErrorCode.CODE, ErrorCode.ROOM_NOT_EXIST);
			return result;
		}

		MahjongRoom room = rooms.get(roomNum);
		if (room == null) {
			result.put(ErrorCode.RESULT, ErrorCode.FAILED);
			result.put(ErrorCode.CODE, ErrorCode.ROOM_NOT_EXIST);
			return result;
		}

		if (room.isStart()) {
			result.put(ErrorCode.RESULT, ErrorCode.FAILED);
			result.put(ErrorCode.CODE, ErrorCode.GAME_AREADY_START);
			return result;
		}

		if (room.getLeaderId() == roleId) {
			result.put(ErrorCode.RESULT, ErrorCode.FAILED);
			result.put(ErrorCode.CODE, ErrorCode.ERROR_MSGTYPE);
			return result;
		}

		int index = room.getIndexByRoleId(roleId);
		if (index == 0) {
			result.put(ErrorCode.RESULT, ErrorCode.FAILED);
			result.put(ErrorCode.CODE, ErrorCode.OPERATE_ERROR);
			return result;
		}

		room.removeMember(index);
		role.setRoomNum(0);
		role.setState(0);
		roleRepository.cacheUpdate(role);

		// 刷新房间信息
		refreshJoinRoom(roleId, roomNum);

		result.put(MahjongConstant.CLIENT_DATA, new Object[] {});
		return result;
	}

	/**
	 * 所有角色缓存信息
	 *
	 * @param map
	 * @return
	 */
	private JSONArray allRoleCacheOutput2json(Map<Integer, RoleCache> map) {
		JSONArray result = new JSONArray();
		for (Entry<Integer, RoleCache> e : map.entrySet()) {
			result.add(RoleCacheOutput(e.getKey(), e.getValue()));
		}
		return result;
	}

	/**
	 * 所有角色缓存信息
	 *
	 * @param map
	 * @return
	 */
	private Object[] allRoleCacheOutput(Map<Integer, RoleCache> map) {
		List<Object[]> result = new ArrayList<>(map.size());
		for (Entry<Integer, RoleCache> e : map.entrySet()) {
			result.add(RoleCacheOutput(e.getKey(), e.getValue()));
		}
		return result.toArray();
	}

	/**
	 * 角色缓存信息
	 *
	 * @param cache
	 * @return
	 */
	private Object[] RoleCacheOutput(int index, RoleCache cache) {
		return new Object[] { index, cache.getRoleId(), cache.getName(), cache.getIp(), cache.getHeadimgurl() };
	}

	private RoleCache createRoleCache(long roleId, String name, String ip, String headImg) {
		RoleCache cache = new RoleCache();
		cache.setRoleId(roleId);
		cache.setName(name);
		cache.setIp(ip);
		cache.setHeadimgurl(headImg);
		return cache;
	}

	/**
	 * 验证是否可以开始游戏了。够4个人就行了
	 *
	 * @param roomNum
	 */
	private void checkStart(long roleId, int roomNum) {
		// 检查是否有房间
		MahjongRoom room = rooms.get(roomNum);

		Map<Integer, RoleCache> roleMap = room.getRoleCacheMap();
		if (roleMap.keySet().size() >= ConfigConstant.ROOM_NUM) {
			// 人数够了。那就开始游戏把
			room.setStart(true);
			logger.info("room people enough, start game,roomNum={}", roomNum);

			start(room);

		}
	}

	/**
	 * 初始化房间信息
	 *
	 * @param room
	 */
	private void start(MahjongRoom room) {
		// 当前局的开始时间
		room.setCurJuStartDate(new Date());

		Map<Integer, RoleCache> roleMap = room.getRoleCacheMap();
		// 初始化房间里的牌
		room.startInitRoomChess();

		// 1.洗牌
		room.xipai();

		// 生成痞子和癞子牌
		room.initRuffian();

		// 2.给每个人初始化13张牌
		initThirteenth(room);

		// 3. 排序
		room.sort();

		// 将每个人的牌推送给客户端。
		pushInitChessEveryVo(roleMap.values());

		// 4.初始化完了以后，找到第一个进房间的人(也就是创建房间的人)，他摸牌，摸完以后验证是否有杠或者天胡，依次打牌
		int roleIndex = room.getOperateIndex();
		if (roleIndex == 0) {
			logger.info("玩家摸牌索引位为0,roomNum={}", room.getRoomNum());
		} else {
			RoleCache roleCache = room.getRoleCacheMap().get(roleIndex);
			if (roleCache != null) {
				moChess(roleCache.getRoleId(), true);
				room.setZhuang(roleIndex);
			}
		}

		// 记录玩家状态
		for (Entry<Integer, RoleCache> e : roleMap.entrySet()) {
			RoleCache cache = e.getValue();
			Role role = roleManager.getRole(cache.getRoleId());
			role.setState(StateType.STARTGAME.getVal());
			roleRepository.cacheUpdate(role);
		}
	}

	/**
	 * 初始化牌。并且记录当前的操作
	 *
	 * @param room
	 */
	private void initThirteenth(MahjongRoom room) {
		// 给每个人初始化13张牌
		room.initThirteenth();

		JSONArray list = new JSONArray();
		for (Entry<Integer, RoleCache> e : room.getRoleCacheMap().entrySet()) {
			RoleCache cache = e.getValue();
			list.add(new Object[] { cache.getRoleId(), e.getKey(), cache.getName(), cache.getJifen(),
			        allChessVo(cache.getRoleChessList()) });
		}
		Object result[] = new Object[] { room.getRoomNum(), room.getCurJuStartDate(), room.getJushu(),
		        room.getAlreadyJuShu(), room.getLeftChess(), list.toArray() };

		addPlayBack(room, MahjongConstant.PLAY_BACK_TYPE_INIT, 0, room.getOperateIndex(), result);
	}

	private Object[] pushLaiZiVo(MahjongRoom room) {
		ChessEvery ruffian = room.getRuffian();
		ChessEvery laizi = room.getRandomLaizi();
		List<Object[]> result = new ArrayList<>();
		result.add(chessEveryVo(ruffian)); // 先放痞子
		result.add(chessEveryVo(laizi)); // 再放癞子

		return result.toArray();
	}

	/**
	 * 给玩家推送提示 碰，杠，胡
	 *
	 * @param roleCache
	 * @param mo
	 *            是否是自己摸的牌
	 * @param vo
	 *            摸到的牌
	 * @param zimohu
	 *            是否是只能自摸胡
	 * @return
	 */
	private List<Integer> check(RoleCache roleCache, boolean mo, ChessEvery vo, boolean zimohu) {
		List<Integer> result = new ArrayList<>();

		List<ChessEvery> list = roleCache.getRoleChessList();

		MahjongRoom room = getMahjongRoom(roleCache.getRoleId());

		if (mo) {
			// 自己摸牌 验证是否有暗杠 或者自摸
			// 是否有暗杠
			List<ChessEvery> angangList = angang(removeSpecialChess(list, room));
			if (angangList != null) {
				result.add(MahjongConstant.MAHJONG_ANGANG);
			}

			// 验证是否有过路杠
			List<ChessEvery> guolugangList = guolugang(list, vo);
			if (guolugangList != null) {
				result.add(MahjongConstant.MAHJONG_GUOLUGANG);
			}

			// 验证是否胡牌
			Integer hu = isWin(roleCache);
			if (hu != null && checkHuMinimum(room, roleCache.getRoleId(), MahjongConstant.MAHJONG_ZIMO, hu)) {
				result.add(MahjongConstant.MAHJONG_ZIMO);
			}
		} else {
			// 不是自己摸牌，验证是否有明杠或碰或胡
			int next4Operate = room.getOperateIndex() + 1;
			if (next4Operate > ConfigConstant.ROOM_NUM) {
				next4Operate = 1;
			}
			if (eatChess(list, vo, room) && next4Operate == room.getIndexByRoleId(roleCache.getRoleId())) {
				result.add(MahjongConstant.MAHJONG_EAT);
			}
			List<ChessEvery> pengList = peng(list, vo);
			if (pengList != null) {
				result.add(MahjongConstant.MAHJONG_PENG);
			}

			List<ChessEvery> minggangList = minggang(list, vo);
			if (minggangList != null) {
				result.add(MahjongConstant.MAHJONG_MINGGANG);
			}

			// 未开口，且手中含有两个以上的癞子，不让吃胡
			if (!isPick(roleCache) && getHaveLzCount(roleCache.getRoleChessList(), room.getRandomLaizi()) >= 2) {
				logger.info("head have mony laizi and no pick");
			} else {
				// 添加一个临时变量验证是否胡牌
				RoleCache temp = getRoleCacheTemp(roleCache, vo);

				// 验证是否胡牌
				Integer hu = isWin(temp);
				if (hu != null && checkHuMinimum(room, roleCache.getRoleId(), MahjongConstant.MAHJONG_HU, hu)) {
					result.add(MahjongConstant.MAHJONG_HU);
				}
			}
		}

		// 有的话。就推送吧
		if (CollectionUtils.isNotEmpty(result)) {

			JSONObject resultData = new JSONObject();
			resultData.put(ErrorCode.RESULT, ErrorCode.OK);
			resultData.put(MahjongConstant.CLIENT_DATA, result);

			if (mo) {
				roleCache.setTan(true);
				routeManager.relayMsg(roleCache.getRoleId(), MsgType.MAHJONG_SHOW_MSG, resultData);
			} else if (!mo && result.contains(MahjongConstant.MAHJONG_HU)) {
				roleCache.setTan(true);
				routeManager.relayMsg(roleCache.getRoleId(), MsgType.MAHJONG_SHOW_MSG, resultData);
			}

			logger.info("show sign,roleId={},name={},sign={}, isMo={}", roleCache.getRoleId(), roleCache.getName(),
			        loggerSignType(result), mo);
			// 记录回放标签提示
			addPlayBack(room, MahjongConstant.PLAY_BACK_TYPE_SHOW_SIGN, roleCache.getRoleId(), room.getOperateIndex(),
			        result.toArray());
		}

		return result;
	}

	/**
	 * 验证起胡是否达到番薯
	 * 
	 * @param room
	 * @param roleId
	 * @param huType
	 * @param hu
	 * @return
	 */
	private boolean checkHuMinimum(MahjongRoom room, long roleId, int huType, int hu) {
		JiFenTemplate template = mahjongDataTemplateManager.getJiFenTemplate(huType);
		RoleCache winRole = room.getRoleCacheByRoleId(roleId);
		FanTemplate fanTempHu = mahjongDataTemplateManager.getFanTemplate(hu);

		if (!template.isFlag()) {
			// 只扣一个玩家
			RoleCache loseRole = room.getRoleCacheByIndex(room.getOperateIndex());
			int allfan = calculationFan(loseRole) * calculationFan(winRole) << fanTempHu.getFan();
			if (room.getFan() != 0) {
				allfan = allfan << room.getFan();
			}
			if (allfan >= room.getMinimumHu()) {
				return true;
			}
		} else {
			// 除了roleId，其他3玩家的积分都要扣
			for (Entry<Integer, RoleCache> e : room.getRoleCacheMap().entrySet()) {
				RoleCache cache = e.getValue();
				if (roleId == cache.getRoleId()) {
					continue;
				}

				int allfan = calculationFan(cache) * calculationFan(winRole);
				allfan = allfan << fanTempHu.getFan();
				if (room.getFan() != 0) {
					allfan = allfan << room.getFan();
				}
				if (allfan >= room.getMinimumHu()) {
					return true;
				}
			}
		}

		return false;
	}

	private String loggerSignType(List<Integer> result) {
		String strs = "";
		for (Integer i : result) {
			strs += i + " ";
		}
		return strs;
	}

	/**
	 * 添加回放记录
	 *
	 * @param room
	 *            房间好
	 * @param type
	 *            回放类型
	 * @param roleId
	 *            玩家id
	 * @param obj
	 *            回放数据
	 */
	private void addPlayBack(MahjongRoom room, int type, long roleId, int operateIndex, Object obj) {
		PlayBackVo vo = new PlayBackVo();
		vo.setType(type);
		vo.setRoleId(roleId);
		vo.setIndex(operateIndex);
		vo.setObj(obj);
		room.addPlayBackMap(vo);
	}

	/**
	 * 获取房间
	 *
	 * @param roleId
	 * @return
	 */
	private MahjongRoom getMahjongRoom(long roleId) {
		Role role = roleManager.getRole(roleId);
		return rooms.get(role.getRoomNum());
	}

	/**
	 * 推送初始化13张牌给客户端
	 *
	 * @param roles
	 */
	private void pushInitChessEveryVo(Collection<RoleCache> roles) {
		for (RoleCache roleCache : roles) {
			pushOneRoleVo(roleCache);
		}
	}

	private void pushOneRoleVo(RoleCache roleCache) {
		JSONObject result = new JSONObject();
		result.put(ErrorCode.RESULT, ErrorCode.OK);

		// 痞牌和癞子牌
		Role role = roleManager.getRole(roleCache.getRoleId());
		int roomNum = role.getRoomNum();
		MahjongRoom room = rooms.get(roomNum);
		Object[] data = allChessVo(roleCache.getRoleChessList());
		result.put(MahjongConstant.CLIENT_DATA, new Object[] { pushLaiZiVo(room), data, room.getZhuang() });
		routeManager.relayMsg(roleCache.getRoleId(), MsgType.MAHJONG_PAI_INIT_MSG, result);
	}

	private Object[] allChessVo(List<ChessEvery> all) {
		List<Object[]> result = new ArrayList<>();
		List<ChessEvery> useChessEvery = new ArrayList<>();
		List<ChessEvery> notUseChessEvery = new ArrayList<>();
		for (ChessEvery ce : all) {
			if (ce.isUsed()) {
				useChessEvery.add(ce);
			} else {
				notUseChessEvery.add(ce);
			}
		}
		for (ChessEvery ce : useChessEvery) {
			result.add(chessEveryVo(ce));
		}
		for (ChessEvery ce : notUseChessEvery) {
			result.add(chessEveryVo(ce));
		}
		return result.toArray();
	}

	private JSONArray allChessVo2JsonArray(List<ChessEvery> all) {
		JSONArray allChess = new JSONArray();
		for (ChessEvery ce : all) {
			allChess.add(chessEveryVo2Json(ce));
		}
		return allChess;
	}

	private Object[] chessEveryVo(ChessEvery ce) {
		return new Object[] { ce.getId(), ce.getColor(), ce.getNumber(), ce.isUsed() };
	}

	private JSONObject chessEveryVo2Json(ChessEvery ce) {
		return (JSONObject) JSONObject.toJSON(ce);
	}

	/**
	 * 获取手牌中的癞子数
	 *
	 * @param roleChessList
	 * @param laizi
	 * @return
	 */
	private int getHaveLzCount(List<ChessEvery> roleChessList, ChessEvery laizi) {
		// 拿到已经被用过的赖子数量
		int usedCount = getLzUsedCount(roleChessList, laizi);
		int oldSize = roleChessList.size();
		// 是赖子玩法的话，移除我的牌中的赖子
		int haveLzCount = 0;
		roleChessList = removeHzLz(roleChessList, laizi);
		// 我有的赖子数量
		haveLzCount = oldSize - roleChessList.size();
		// 看下我的赖子有没有被使用过，比如碰了或杠了。
		haveLzCount = haveLzCount - usedCount; // 我真正能用的赖子数量

		return haveLzCount;
	}

	/**
	 * 玩家是否胡了 胡牌算法，对于有没有红中赖子的玩法都无所谓，不是赖子玩法的话，我拥有的赖子数为0，主要是想维护一套代码
	 *
	 * @param roleCache
	 * @return
	 */
	private Integer isWin(RoleCache roleCache) {
		Role role = roleManager.getRole(roleCache.getRoleId());
		MahjongRoom room = rooms.get(role.getRoomNum());

		List<ChessEvery> roleChessList = roleCache.getRoleChessList();
		// 手中有杠牌，不让胡牌
		for (ChessEvery chessEvery : roleChessList) {
			if (chessEvery.isUsed()) {
				continue;
			}
			if (chessEvery.getNumber() == room.getRuffian().getNumber()
			        && chessEvery.getColor() == room.getRuffian().getColor()) {
				return null;
			}
			if (chessEvery.getNumber() == MahjongConstant.FENG_TYPE_ZHONG
			        && chessEvery.getColor() == MahjongConstant.MAHJONG_FENG) {
				return null;
			}
		}
		// 拿到已经被用过的红中赖子数量
		int haveLzCount = getHaveLzCount(roleChessList, room.getRandomLaizi());
		roleChessList = getChessArrExceptLaiZi(roleChessList, room.getRandomLaizi());

		Integer duidui = duiDuiHu(roleChessList, haveLzCount, true);

		if (duidui != null) {
			// room.setFan(duidui);
			return duidui;
		}

		// 假设将在饼种，判断条 万 风成为副子的所需要的红中赖子数，假设将在条中，以此类推
		Map<Integer, List<ChessEvery>> sameColorMap = getSameColorMap(roleChessList);
		for (Integer color : sameColorMap.keySet()) {
			int haveHzCount = haveLzCount; // 有红中赖子的数量
			int need = getRuleAllHzCount(sameColorMap, color);
			if (need <= haveHzCount) {
				// 需要的红中赖子数小于等于我拥有的。我就拿出来将所在的花色，判断是否是有将并且其他牌都能成为副子
				haveHzCount = haveHzCount - need; // 我现在拥有的赖子数

				Integer huType = null;
				boolean flag = false;
				// 是否需要258做将
				boolean requireSpecialJiang = false;
				if (isPingHu(roleCache, haveLzCount, room)) {
					if (haveLzCount > 1) {
						return null;
					}
					requireSpecialJiang = true;
					huType = MahjongConstant.MAHJONG_HU_TYPE_PING;
				} else if (isAllClaimant(roleCache)) {
					requireSpecialJiang = true;
					huType = MahjongConstant.MAHJONG_HE_TYPE_ALL_CLAIMANT;
				} else if (isNotSpeak(roleChessList, room)) {
					requireSpecialJiang = true;
					huType = MahjongConstant.MAHJONG_HU_TYPE_NOT_SPEAK;
				}
				flag = jiang(sameColorMap.get(color), haveHzCount, requireSpecialJiang);

				if (flag) {
					roleCache.setOneHaveLzCount(haveHzCount); // 胡牌含有的癞子数
					if (isUniformly(roleChessList, room)) { // 请一色
						huType = MahjongConstant.MAHJONG_HE_TYPE_PUR_COLOR;
					}
					if (isPengPengHu(roleCache, haveLzCount)) {
						huType = MahjongConstant.MAHJONG_HU_TYPE_PENGPENG;
					}
					return huType;
				}

			}
		}
		return null;
	}

	/**
	 * 屁胡-258做将
	 * 
	 * @param roleChessList
	 * @param room
	 * @return
	 */
	private boolean isPingHu(RoleCache roleCache, int haveLzCount, MahjongRoom room) {
		List<ChessEvery> roleChessList = roleCache.getRoleChessList();

		if (isAllClaimant(roleCache)) {
			return false;
		}
		if (isUniformly(roleChessList, room)) { // 请一色
			return false;
		}
		if (isNotSpeak(roleChessList, room)) {
			return false;
		}
		if (isPengPengHu(roleCache, haveLzCount)) {
			return false;
		}
		return true;
	}

	/**
	 * 是否全求人
	 *
	 * @param roleChessList
	 * @return
	 */
	public boolean isAllClaimant(RoleCache roleCache) {
		List<ChessEvery> roleChessList = roleCache.getRoleChessList();
		List<ChessEvery> headChessList = new ArrayList<>();
		for (ChessEvery chessEvery : roleChessList) {
			if (!chessEvery.isUsed()) {
				headChessList.add(chessEvery);
			}
		}
		if (headChessList.size() <= 2 && roleCache.getOneEatCount() != 0) {
			return true;
		}
		return false;
	}

	public boolean isPengPengHu(RoleCache roleCache, int haveLzCount) {
		if (roleCache.getOneEatCount() != 0) {
			return false;
		}
		List<ChessEvery> roleChessList = roleCache.getRoleChessList();

		Map<Integer, List<ChessEvery>> sameColorMap = getSameColorMap(roleChessList);
		for (Integer jiang : sameColorMap.keySet()) {
			int haveHzCount = haveLzCount; // 有红中赖子的数量
			int need = getRuleAllPengPengHzCount(sameColorMap, jiang);
			if (need > haveHzCount) {
				// return false;
				continue;
			}

			haveHzCount = haveHzCount - need;
			ChessEvery ceArr[] = sameColorMap.get(jiang).toArray(new ChessEvery[sameColorMap.get(jiang).size()]);
			int length = ceArr.length;
			for (int i = 0; i < length; i++) {
				int needCount = 0;
				ChessEvery cur = ceArr[i];
				if (cur.isUsed()) {
					continue;
				}
				if (i < length - 1) {
					ChessEvery next = ceArr[i + 1];
					if (cur.getColor() == next.getColor() && cur.getNumber() == next.getNumber()) {
						ChessEvery newChesses[] = getChessArrExceptArgs(ceArr, cur.getId(), next.getId());
						needCount = rulePengPeng(newChesses);
						if (haveHzCount >= needCount) {
							return true;
						}
						continue;
					}
				}

				if (haveHzCount > 0) {

					needCount++;
					ChessEvery newChesses[] = getChessArrExceptArgs(ceArr, cur.getId());
					needCount = rulePengPeng(newChesses) + needCount;
					if (haveHzCount >= needCount) {
						return true;
					}
					continue;
				}

			}
		}
		return false;
	}

	/**
	 * 除了jiang的花色，其他花色的牌成为碰碰胡刻字需要红中赖子的总数
	 *
	 * @param map
	 * @param jiang
	 * @return
	 */
	private int getRuleAllPengPengHzCount(Map<Integer, List<ChessEvery>> map, int jiang) {
		int need = 0;
		for (Integer color : map.keySet()) {
			if (color == jiang) {
				continue;
			}

			List<ChessEvery> list = map.get(color);
			ChessEvery ceArr[] = list.toArray(new ChessEvery[list.size()]);
			need += rulePengPeng(ceArr);
		}
		return need;
	}

	public int rulePengPeng(ChessEvery[] roleChess) {
		int needCount = 0;
		int length = roleChess.length;
		int used[] = new int[length];

		for (int i = 0; i < length; i++) {
			ChessEvery ce = roleChess[i];
			if (ce.isUsed()) {
				used[i] = 1;
			}
			if (used[i] == 1) {
				continue;
			}
			// 一张牌要想成为副子。有3种可能
			// 1.当前牌 +2个赖子
			int needTemp = 2;
			int m = -1;
			int n = -1;
			if (i + 3 <= length && ce.getNumber() == roleChess[i + 1].getNumber() && used[i] == 0 && used[i + 1] == 0) {
				needTemp = 1;
				m = i;
				n = i + 1;
			}
			if (i + 3 <= length && ce.getNumber() == roleChess[i + 1].getNumber()
			        && ce.getNumber() == roleChess[i + 2].getNumber() && used[i] == 0 && used[i + 1] == 0
			        && used[i + 2] == 0) {
				needTemp = 0;
				used[i] = used[i + 1] = used[i + 2] = 1;
			}
			if (m != -1 && n != -1) {
				used[m] = used[n] = 1;
			}
			needCount += needTemp;
		}
		return needCount;
	}

	/**
	 * 清一色
	 * 
	 * @param roleChessList
	 * @param room
	 * @return
	 */
	public boolean isUniformly(List<ChessEvery> roleChessList, MahjongRoom room) {
		int color = 0;
		int count = 0;
		for (ChessEvery chessEvery : roleChessList) {
			if (isSpecialChess(chessEvery, room)) {
				continue;
			}
			if (color != chessEvery.getColor()) {
				color = chessEvery.getColor();
				count++;
			}
			if (count > 1) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 门前清
	 * 
	 * @param roleCHessList
	 * @return
	 */
	private boolean isNotSpeak(List<ChessEvery> roleCHessList, MahjongRoom room) {
		for (ChessEvery ce : roleCHessList) {
			if (isSpecialChess(ce, room)) {
				continue;
			}
			if (ce.isUsed()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 移除红中赖子
	 *
	 * @param roleChessList
	 * @return
	 */
	private List<ChessEvery> removeHzLz(List<ChessEvery> roleChessList, ChessEvery laizi) {
		List<ChessEvery> hzChessList = new ArrayList<>();
		// 是红中赖子玩法的话。把红中赖子移除掉。
		for (ChessEvery ce : roleChessList) {
			if (ce.getColor() == laizi.getColor() && ce.getNumber() == laizi.getNumber()) {
				continue;
			}
			hzChessList.add(ce);
		}
		return hzChessList;
	}

	/**
	 * 获取赖子被使用过的数量
	 *
	 * @param roleChessList
	 * @return
	 */
	private int getLzUsedCount(List<ChessEvery> roleChessList, ChessEvery laiziChess) {
		int count = 0;
		for (ChessEvery ce : roleChessList) {
			if (ce.getColor() == laiziChess.getColor() && ce.getNumber() == laiziChess.getNumber() && ce.isUsed()) {
				count++;
			}
		}
		return count;
	}

	/**
	 * 把相同花色的牌放到一起
	 *
	 * @param roleChessList
	 * @return
	 */
	private Map<Integer, List<ChessEvery>> getSameColorMap(List<ChessEvery> roleChessList) {
		Map<Integer, List<ChessEvery>> map = new HashMap<>();
		// 相同花色的在一起
		for (ChessEvery c : roleChessList) {
			List<ChessEvery> list = map.get(c.getColor());
			if (list == null) {
				list = new ArrayList<>();
				map.put(c.getColor(), list);
			}
			list.add(c);
		}
		return map;
	}

	/**
	 * 除了jiang的花色，其他花色的牌成为副子需要红中赖子的总数
	 *
	 * @param map
	 * @param jiang
	 * @return
	 */
	private int getRuleAllHzCount(Map<Integer, List<ChessEvery>> map, int jiang) {
		int need = 0;
		for (Integer color : map.keySet()) {
			if (color == jiang) {
				continue;
			}
			List<ChessEvery> list = map.get(color);
			ChessEvery ceArr[] = list.toArray(new ChessEvery[list.size()]);
			int needCount = rule(ceArr);
			need += needCount;
		}
		return need;
	}

	/**
	 * 判断是否是对对胡
	 *
	 * @param roleCache
	 * @return
	 */
	private Integer duiDuiHu(List<ChessEvery> roleChessList, int haveLz, boolean hz) {
		ChessEvery roleChess[] = roleChessList.toArray(new ChessEvery[roleChessList.size()]);
		// 不符合对对胡的列表
		List<ChessEvery> notDuilist = notDuiDuiHu(roleChess);
		if (notDuilist == null) {
			return null;
		}
		int notSize = notDuilist.size();
		// 拥有的大于等于所需要的。肯定就是对对胡
		if (haveLz >= notSize) {
			if (hz) {
				// 红中赖子算豪华对对话 1.红中多，余=0就是豪华 2.除了赖子。已经有4个一样的了
				int left = haveLz - notSize;
				if (left > 0 && left % 2 == 0) {
					return MahjongConstant.MAHJONG_HU_TYPE_HAOHUADUIDUI;
				} else if (CollectionUtils.isNotEmpty(angang(roleChessList))) {
					return MahjongConstant.MAHJONG_HU_TYPE_HAOHUADUIDUI;
				}
			} else {
				// 不是红中赖子的话，就看玩家手里是否有四个一样的
				List<ChessEvery> haohua = angang(roleChessList);
				if (CollectionUtils.isNotEmpty(haohua)) {
					return MahjongConstant.MAHJONG_HU_TYPE_HAOHUADUIDUI;
				}
			}

			return MahjongConstant.MAHJONG_HU_TYPE_DUIDUI;
		}
		return null;
	}

	/**
	 * 不能组成对对胡的牌列表
	 *
	 * @param roleChess
	 * @return
	 */
	private List<ChessEvery> notDuiDuiHu(ChessEvery roleChess[]) {
		// 不符合对对胡的列表
		List<ChessEvery> notDuilist = new ArrayList<>();
		int length = roleChess.length;
		int used[] = new int[length];
		for (int i = 0; i < length; i++) {
			ChessEvery ce = roleChess[i];
			// 碰了或者杠了，不能成为对对胡
			if (ce.isUsed()) {
				return null;
			}
			if (used[i] == 1) {
				continue;
			}

			if (i < roleChess.length - 1) {
				ChessEvery nextCe = roleChess[i + 1];
				// 第一张和第二张的花色或者数值相同就过
				if (ce.getColor() == nextCe.getColor() && ce.getNumber() == nextCe.getNumber()) {
					used[i] = used[i + 1] = 1;
				}
			}

			if (used[i] != 1) {
				notDuilist.add(ce);
			}
		}
		return notDuilist;
	}

	public static void main(String[] args) {
		// String url =
		// "https://api.weixin.qq.com/sns/oauth2/access_token?appid=wxed8911da419d6637&secret=6091500bd1ab216b2ba07552654901bd&code=CODE&grant_type=authorization_code";
		// HttpManager httpManager = new HttpManager();
		// JSONObject js = httpManager.get4https(url);
		//
		// System.out.println(js);

		/*
		 * MahjongManager m = new MahjongManager(); Server s = new Server();
		 * s.setWorldId(1); s.setName("1111");
		 *
		 * Map<String, String> map = new HashMap<>(); map.put("worldId",
		 * s.getWorldId()+""); map.put("name", s.getName()); String a1 =
		 * m.getParamString(map); String url
		 * ="http://192.168.1.30:8089/admin/addAreaSys.do"+a1; long a2 =
		 * System.currentTimeMillis(); PlatformClient c = new PlatformClient();
		 * JSONObject j = c.get(url, false);
		 * System.out.println(System.currentTimeMillis() - a2);
		 */

		MahjongManager m = new MahjongManager();
		//
		int roomNum = 9999;
		MahjongRoom room = m.rooms.get(roomNum);
		if (room == null) {
			room = new MahjongRoom();
			m.rooms.put(roomNum, room);
		}

		// room.setZimohu(false);
		// room.setFeng(false);
		room.startInitRoomChess();
		room.setHz(false);
		room.initRuffian();
		// room.setYu(8);

		int index = room.getMaxIndex();
		Map<Integer, RoleCache> map = room.getRoleCacheMap();
		RoleCache roleCache = map.get(index);
		if (roleCache == null) {
			roleCache = m.createRoleCache(11, "wn", "127.0.0.1", "");
			map.put(index, roleCache);
		}

		// roleCache.addRoleChess(new ChessEvery(1, 2, 2, false), false);
		// roleCache.addRoleChess(new ChessEvery(2, 2, 2, false), false);
		// roleCache.addRoleChess(new ChessEvery(21, 2, 2, false), false);
		// roleCache.addRoleChess(new ChessEvery(3, 2, 2, false), false);
		// roleCache.addRoleChess(new ChessEvery(4, 3, 2, false), false);
		// roleCache.addRoleChess(new ChessEvery(5, 4, 2, false), false);
		// roleCache.addRoleChess(new ChessEvery(6, 6, 2, false), false);
		// roleCache.addRoleChess(new ChessEvery(7, 6, 2, false), false);

		roleCache.addRoleChess(new ChessEvery(8, 4, 2, false), false);
		roleCache.addRoleChess(new ChessEvery(7, 4, 2, false), false);
		roleCache.addRoleChess(new ChessEvery(1, 2, 2, false), false);
		roleCache.addRoleChess(new ChessEvery(2, 2, 2, false), false);
		roleCache.addRoleChess(new ChessEvery(3, 2, 2, false), false);
		roleCache.addRoleChess(new ChessEvery(4, 3, 2, false), false);
		roleCache.addRoleChess(new ChessEvery(5, 3, 2, false), false);
		roleCache.addRoleChess(new ChessEvery(6, 3, 2, false), false);
		roleCache.addRoleChess(new ChessEvery(9, 5, 2, false), false);
		roleCache.addRoleChess(new ChessEvery(10, 5, 2, false), false);
		roleCache.addRoleChess(new ChessEvery(11, 5, 2, false), false);
		System.out.println(m.isPengPengHu(roleCache, 0));
		// roleCache.addRoleChess(new ChessEvery(8, 7, 2, true), false);
		// roleCache.addRoleChess(new ChessEvery(9, 7, 2, true), false);
		// roleCache.addRoleChess(new ChessEvery(11, 7, 2, true), false);
		// roleCache.addRoleChess(new ChessEvery(12, 7, 2, true), false);
		// roleCache.addRoleChess(new ChessEvery(13, 2, 3, false), false);
		// roleCache.addRoleChess(new ChessEvery(14, 3, 3, false), false);
		// roleCache.addRoleChess(new ChessEvery(15, 4, 3, false), false);
		ChessEvery ceArr[] = roleCache.getRoleChessList().toArray(new ChessEvery[roleCache.getRoleChessList().size()]);
		System.out.println(m.rule(ceArr));
		// System.out.println(m.eatChess(roleCache.getRoleChessList(), new
		// ChessEvery(15, 6, 3, false)));
		// roleCache.addRoleChess(new ChessEvery(13,17, 45, false), false);
		// roleCache.addRoleChess(new ChessEvery(14,17, 45, false), false);

		// roleCache.addRoleChess(new ChessEvery(3, 3, false), false);
		// roleCache.addRoleChess(new ChessEvery(3, 3, false), false);
		// roleCache.addRoleChess(new ChessEvery(3, 3, false), false);
		// roleCache.addRoleChess(new ChessEvery(4, 3, false), false);
		// roleCache.addRoleChess(new ChessEvery(5, 3, false), false);
		// roleCache.addRoleChess(new ChessEvery(5, 3, false), false);

		// ChessEvery c = new ChessEvery(6, 1, false);
		// roleCache.addRoleChess(c, false);

		List<ChessEvery> oldList = roleCache.getRoleChessList();

		List<ChessEvery> newList = m.removeHzLz(oldList, room.getRandomLaizi());

		Map<Integer, List<ChessEvery>> sameColorMap = m
		        .getSameColorMap(m.removeHzLz(roleCache.getRoleChessList(), room.getRandomLaizi()));
		for (Integer color : sameColorMap.keySet()) {
			int haveHzCount = oldList.size() - newList.size(); // 有红中赖子的数量
			int need = m.getRuleAllHzCount(sameColorMap, color);
			if (need <= haveHzCount) {
				// 需要的红中赖子数小于等于我拥有的。我就拿出来将所在的花色，判断是否是有将并且其他牌都能成为副子
				haveHzCount = haveHzCount - need; // 我现在拥有的赖子数
				boolean flag = m.jiang(sameColorMap.get(color), haveHzCount, false);
				if (flag) {
					System.out.println(flag);
				}
			}
		}

		// Map<Integer, List<ChessEvery>> map2 =
		// m.getSameColorMap(m.removeHzLz(roleCache.getRoleChessList()));
		// for(Integer color : map2.keySet()){
		// ChessEvery roleChess[] = map2.get(color).toArray(new
		// ChessEvery[map2.get(color).size()]);
		// m.ruleByFuzi(roleChess);
		// }
		//
		// room.startInitRoomChess();
		/*
		 * ChessEvery ces[] = room.getRoomChess(); String s = ""; String s1="";
		 * Map<String, List<ChessEvery>> countMap = new HashMap<>();
		 * for(ChessEvery c : ces){ String key = c.getColor()+"-"+c.getNumber();
		 * List<ChessEvery> list = countMap.get(key); if(list == null){ list =
		 * new ArrayList<>(); countMap.put(key, list); } list.add(c); } int sum
		 * = 0; for(String k : countMap.keySet()){ String strs[] = k.split("-");
		 * int c = Integer.valueOf(strs[0]); int n = Integer.valueOf(strs[1]);
		 * if(c == 1){ s = "筒"; s1=n+""; }else if(c == 2){ s = "条"; s1=n+"";
		 * }else if(c == 3){ s = "万"; s1=n+""; }else if(c == 4){ s = "风"; if(n
		 * == 10){ s1="东"; }else if(n == 11){ s1="西"; }else if(n == 12){ s1="南";
		 * }else if(n == 13){ s1="北"; }else if(n == 14){ s1="中"; }else if(n ==
		 * 15){ s1="发"; }else if(n == 16){ s1="白"; } } sum +=
		 * countMap.get(k).size(); System.out.println(s1 + s + "有"+
		 * countMap.get(k).size()+"个"); } System.out.println("一共有"+sum+"张牌");
		 */

		// StringBuffer sb = new StringBuffer();
		// sb.append("123");
		// String a = sb.substring(0, 1);
		// sb.append(a);
		// sb.append("ddd");
		// System.out.println(a);
		/*
		 * int all[] = new int[]{1,2,3,4,5,6,7}; int index = 2; int left =
		 * all.length - index; int newAll[] = new int[left];
		 *
		 * System.arraycopy(all, index, newAll, 0, left);
		 * System.err.println(newAll);
		 */
		/*
		 * room.xipai();
		 *
		 * room.addMember(m.createRoleCache(11, "wn"));
		 * room.addMember(m.createRoleCache(22, "chy"));
		 *
		 * int index = room.getMaxIndex(); Map<Integer, RoleCache> map =
		 * room.getRoleCacheMap(); RoleCache roleCache = map.get(index); if
		 * (roleCache == null) { roleCache = m.createRoleCache(11, "wn");
		 * map.put(index, roleCache); }
		 *
		 * // 2.给每个人初始化13张牌 room.initThirteenth();
		 *
		 * // 3. 排序 room.sort();
		 *
		 * ChessEvery moChess= room.getMo(); RoleCache roleWn =
		 * room.getRoleCacheByRoleId(11); roleWn.addRoleChess(moChess, true);
		 * String a = "1|1,2,3,5,5;2|2,2,2;3|1,1,1,2,2"; Map<Integer,
		 * List<Integer>> map = m.GMTest(a); m.GMChangeChess(11, map);
		 *
		 * System.out.println("aa");
		 */
		/*
		 * roleCache.addRoleChess(new ChessEvery(6, 1, false), false);
		 * roleCache.addRoleChess(new ChessEvery(7, 1, false), false);
		 * roleCache.addRoleChess(new ChessEvery(8, 1, false), false);
		 * roleCache.addRoleChess(new ChessEvery(1, 2, false), false);
		 * roleCache.addRoleChess(new ChessEvery(2, 2, false), false);
		 * roleCache.addRoleChess(new ChessEvery(3, 2, false), false);
		 * roleCache.addRoleChess(new ChessEvery(5, 2, true), false);
		 * roleCache.addRoleChess(new ChessEvery(5, 2, true), false);
		 * roleCache.addRoleChess(new ChessEvery(5, 2, true), false);
		 * roleCache.addRoleChess(new ChessEvery(4, 3, false), false);
		 * roleCache.addRoleChess(new ChessEvery(4, 3, false), false);
		 * roleCache.addRoleChess(new ChessEvery(6, 3, false), false);
		 * roleCache.addRoleChess(new ChessEvery(7, 3, false), false);
		 *
		 * ChessEvery da = new ChessEvery(5, 3, false);
		 *
		 * Object s[] = roleCache.getRoleChessList().toArray();
		 *
		 * RoleCache temp = m.getRoleCacheTemp(roleCache, da);
		 *
		 * Integer a = m.isWin(temp); m.check(roleCache, false, da);
		 */

		/*
		 * List<ChessEvery> list = new ArrayList<>(); list.add(new ChessEvery(1,
		 * 1, false)); list.add(new ChessEvery(2, 1, false)); list.add(new
		 * ChessEvery(7, 1, false)); list.add(new ChessEvery(7, 1, false));
		 * list.add(new ChessEvery(7, 1, false));
		 *
		 * list.add(new ChessEvery(1, 2, false)); list.add(new ChessEvery(4, 2,
		 * false)); list.add(new ChessEvery(5, 2, false));
		 *
		 * list.add(new ChessEvery(4, 3, false)); list.add(new ChessEvery(5, 3,
		 * false)); list.add(new ChessEvery(6, 3, false)); list.add(new
		 * ChessEvery(7, 3, false)); list.add(new ChessEvery(8, 3, false));
		 * list.add(new ChessEvery(9, 3, false)); String s = ""; String str =
		 * ""; for(ChessEvery ee : list){ if(ee.getColor() == 1){ s = "筒"; }else
		 * if(ee.getColor() == 2){ s = "条"; }else if(ee.getColor() == 3){ s =
		 * "万"; } str += ee.getNumber() + s + " "; }
		 * System.err.println("牌列表："+str); // m.getOutChess(list); List<Integer>
		 * l = new ArrayList<>(); l.add(1); l.add(2); l.add(3); l.add(4);
		 * System.err.println(l.toArray());
		 */

		/*
		 * for(ChessEvery c : list){ list2.add(c); break; } for(ChessEvery c :
		 * list){ if(list2.contains(c)){ System.out.println("aaa"); } }
		 */
		// for(int i = 0; i <=5 ; i++){
		// System.out.println(i%4);
		// }

		// String s = "2016-12-30 01:00:00";
		// String e = "2017-12-31 21:00:00";
		// Date start = TimeUtil.parse(e, TimeUtil.PATTERN_yyyy_MM_dd_HH_mm_ss);
		// Date end = TimeUtil.parse(s, TimeUtil.PATTERN_yyyy_MM_dd_HH_mm_ss);
		// System.err.println(TimeUtil.subDateToDay(start, end));

		// ChessEvery ceArr[] = list.toArray(new ChessEvery[list.size()]);
		// roleCache.setChessEvery(ceArr);

		// m.jiang2(ceArr);
		// boolean f = m.isWin(roleCache);
		// m.sortRoleChess(roleCache);
		// System.out.println(f);

		// System.out.println(RandomUtils.nextInt(1, 999999));
	}

	/**
	 * 验证牌成为副子需要多少个赖子
	 *
	 * @param roleChess
	 *            牌的数组
	 * @return 需要赖子数
	 */
	public int ruleByFuzi(ChessEvery[] roleChess) {
		// 重置标记位
		int length = roleChess.length;
		int used[] = new int[length];

		int need = 0;
		for (int i = 0; i < length; i++) {
			ChessEvery ce = roleChess[i];
			// 当前牌碰了或者杠了，标记当前牌(有用的牌)
			if (ce.isUsed()) {
				used[i] = 1;
			}

			if (used[i] == 1) {
				continue;
			} else {
				// 一张牌要想成为副子。有3种可能
				// 1.当前牌 +2个赖子
				int needTemp = 2;

				// 2.当前牌+1等于下一张牌 或当前牌+2等于下一张牌 或当前牌等于下一张牌
				// +1个赖子(注意：首先要排除第二张牌和第三张是否相同，过滤掉相同的牌，保留一个， 比如 12223，需要赖子数为1)
				int m = -1;
				int n = -1;
				for (int j = i + 1; j < length; j++) {
					ChessEvery next = roleChess[j];
					if (next.isUsed()) {
						continue;
					}
					if (used[j] == 1) {
						continue;
					}
					if (j + 1 <= length - 1 && used[j + 1] == 0) {
						if (next.getNumber() == roleChess[j + 1].getNumber()) {
							continue;
						}
					}

					if (((ce.getNumber() + 1 == next.getNumber() || ce.getNumber() + 2 == next.getNumber())
					        && ce.getColor() < 4) || ce.getNumber() == next.getNumber()) {
						needTemp = 1;
						m = i;
						n = j;
						break;
					}
				}

				// 3. 当前牌、第二张、第三张牌 就是副子。比如 123 或者 111，这就不需要赖子了
				if (i + 3 <= length && ce.getNumber() == roleChess[i + 1].getNumber()
				        && ce.getNumber() == roleChess[i + 2].getNumber() && used[i] == 0 && used[i + 1] == 0
				        && used[i + 2] == 0) {
					needTemp = 0;
					used[i] = used[i + 1] = used[i + 2] = 1;
					m = -1;
					n = -1;
				} else { // 是否为顺子,
					// 风牌不可能为顺子
					if (ce.getColor() < 4) {
						int a = -1;
						for (int j = i + 1; j < length; j++) {
							ChessEvery next = roleChess[j];
							if (next.isUsed()) {
								continue;
							}
							if (ce.getNumber() + 1 == next.getNumber() && used[j] == 0) {
								a = j;
							}
							if (ce.getNumber() + 2 == next.getNumber() && used[j] == 0 && a != -1) {
								needTemp = 0;
								used[i] = used[j] = used[a] = 1;
								m = -1;
								n = -1;
								break;
							}
						}
					}
				}
				// 不符合情况3，但是符合情况2.
				if (m != -1 && n != -1) {
					used[m] = used[n] = 1;
				}
				need = need + needTemp;
			}
		}
		return need;
	}

	public int ruleTingChess(ChessEvery[] roleChess) {
		// 重置标记位
		int length = roleChess.length;
		int used[] = new int[length];

		int need = 0;
		for (int i = 0; i < length; i++) {
			ChessEvery ce = roleChess[i];
			// 当前牌碰了或者杠了，标记当前牌(有用的牌)
			if (ce.isUsed()) {
				used[i] = 1;
			}

			if (used[i] == 1) {
				continue;
			} else {
				// 一张牌要想成为副子。有3种可能
				// 1.当前牌 +2个赖子
				int needTemp = 2;

				// 2.当前牌+1等于下一张牌 或当前牌+2等于下一张牌 或当前牌等于下一张牌
				// +1个赖子(注意：首先要排除第二张牌和第三张是否相同，过滤掉相同的牌，保留一个， 比如 12223，需要赖子数为1)
				int m = -1;
				int n = -1;
				for (int j = i + 1; j < length; j++) {
					ChessEvery next = roleChess[j];
					if (next.isUsed()) {
						continue;
					}
					if (used[j] == 1) {
						continue;
					}
					if (j + 1 <= length - 1 && used[j + 1] == 0) {
						if (next.getNumber() == roleChess[j + 1].getNumber()) {
							continue;
						}
					}

					if (((ce.getNumber() + 1 == next.getNumber() || ce.getNumber() + 2 == next.getNumber())
					        && ce.getColor() < 4) || ce.getNumber() == next.getNumber()) {
						needTemp = 1;
						m = i;
						n = j;
						break;
					}
				}

				// 3. 当前牌、第二张、第三张牌 就是副子。比如 123 或者 111，这就不需要赖子了
				if (i + 3 <= length && ce.getNumber() == roleChess[i + 1].getNumber()
				        && ce.getNumber() == roleChess[i + 2].getNumber() && used[i] == 0 && used[i + 1] == 0
				        && used[i + 2] == 0) {
					needTemp = 0;
					used[i] = used[i + 1] = used[i + 2] = 1;
					m = -1;
					n = -1;
				} else { // 是否为顺子,
					// 风牌不可能为顺子
					if (ce.getColor() < 4) {
						int a = -1;
						for (int j = i + 1; j < length; j++) {
							ChessEvery next = roleChess[j];
							if (next.isUsed()) {
								continue;
							}
							if (ce.getNumber() + 1 == next.getNumber() && used[j] == 0) {
								a = j;
							}
							if (ce.getNumber() + 2 == next.getNumber() && used[j] == 0 && a != -1) {
								needTemp = 0;
								used[i] = used[j] = used[a] = 1;
								m = -1;
								n = -1;
								break;
							}
						}
					}
				}
				// 不符合情况3，但是符合情况2.
				if (m != -1 && n != -1) {
					used[m] = used[n] = 1;
				}
				need = need + needTemp;
			}
		}
		return need;
	}

	/**
	 * 验证牌是顺子或者刻子需要的红中赖子数
	 *
	 * @param roleChess
	 *            牌的数组
	 * @return
	 */
	public int rule(ChessEvery[] roleChess) {
		return ruleByFuzi(roleChess);
	}

	/**
	 * 不符合副子的牌，需要多少个赖子才能符合副子的规则
	 *
	 * @param notRules
	 * @return
	 *//*
	   * private int getNotRuleNeedLzCount(List<ChessEvery> notRules){ int
	   * needHzCount = 0; // 需要赖子的个数 if(CollectionUtils.isNotEmpty(notRules)){
	   * // notRules存放的都是不能成为副子的牌，从这些牌中筛选出需要赖子的个数 int notSize = notRules.size();
	   * int u[] = new int[notSize]; for(int m = 0; m < notSize; m++){
	   * ChessEvery cur = notRules.get(m); if(u[m] == 1){ continue; } for (int n
	   * = m + 1; n < notSize; n++) { ChessEvery next = notRules.get(n); if
	   * (cur.getNumber() + 1 == next.getNumber() || cur.getNumber() + 2 ==
	   * next.getNumber() || cur.getNumber() == next.getNumber()) { // 比如1万 2万
	   * 或者 1万 3万或1万 1万，组成副子，就需要一个赖子 u[m] = 1; u[n] = 1; needHzCount++; break; }
	   * } if(u[m] != 1){ // 说明当前牌是一张单牌，组成副子就需要2个 u[m] = 1; needHzCount++;
	   * needHzCount++; } } } return needHzCount; }
	   */

	/**
	 * 拿到roleChess区间的数组
	 *
	 * @param roleChess
	 * @param startIndex
	 * @param endIndex
	 * @return
	 */
	/*
	 * private ChessEvery[] getChessArrByIndex(ChessEvery[] roleChess, int
	 * startIndex, int endIndex) { List<ChessEvery> list = new ArrayList<>();
	 * for (int i = startIndex; i < endIndex; i++) { list.add(roleChess[i]); }
	 * return list.toArray(new ChessEvery[list.size()]); }
	 */

	/**
	 * 除了args里面的id，获取其他的牌
	 *
	 * @param roleChess
	 * @param args
	 * @return
	 */
	private ChessEvery[] getChessArrExceptArgs(ChessEvery[] roleChess, int... args) {
		List<ChessEvery> list = new ArrayList<>();
		for (ChessEvery c : roleChess) {
			boolean flag = false;
			for (int index : args) {
				if (c.getId() == index) {
					flag = true;
					break;
				}
			}
			if (!flag) {
				list.add(c);
			}
		}
		return list.toArray(new ChessEvery[list.size()]);
	}

	private List<ChessEvery> getChessArrExceptLaiZi(List<ChessEvery> roleChess, ChessEvery laizi) {
		List<ChessEvery> list = new ArrayList<>();
		for (ChessEvery c : roleChess) {
			if (c.getNumber() == laizi.getNumber() && c.getColor() == laizi.getColor()) {
				continue;
			}
			list.add(c);
		}
		return list;
	}

	private boolean jiang(List<ChessEvery> roleChess, int haveLz, boolean specialJiang) {
		ChessEvery ceArr[] = roleChess.toArray(new ChessEvery[roleChess.size()]);
		int length = ceArr.length;
		List<Integer> special = new ArrayList<>();
		special.add(2);
		special.add(5);
		special.add(8);
		// 赖子为0，并且当前花色的个数小于2.肯定不能为将的
		if (haveLz <= 0 && length < 2) {
			return false;
		}

		// 思路：当前牌和下一张牌相同。说明有将，查看其他牌需要的赖子数，如果当前牌和其他牌不一样，那当前牌就需要一个赖子作为将，再算其他牌需要的赖子
		for (int i = 0; i < length; i++) {
			int needCount = 0;
			ChessEvery cur = ceArr[i];
			if (specialJiang && !special.contains(cur.getNumber())) {
				continue;
			}
			if (i < length - 1) {
				ChessEvery next = ceArr[i + 1];
				if (cur.getColor() == next.getColor() && cur.getNumber() == next.getNumber()) {
					ChessEvery newChesses[] = getChessArrExceptArgs(ceArr, cur.getId(), next.getId());
					needCount = rule(newChesses);
					if (haveLz >= needCount) {
						return true;
					}
					continue;
				}
			}
			if (haveLz > 0) {
				needCount++;
				ChessEvery newChesses[] = getChessArrExceptArgs(ceArr, cur.getId());
				needCount = rule(newChesses) + needCount;
				if (haveLz >= needCount) {
					return true;
				}
				continue;
			}
		}
		return false;
	}

	/**
	 * 获取暗杠的索引位
	 *
	 * @param roleChess
	 * @return 为null。没有暗杠,有暗杠的话。返回暗杠的牌id
	 */
	private List<ChessEvery> angang(List<ChessEvery> list) {
		ChessEvery ces[] = list.toArray(new ChessEvery[list.size()]);
		List<ChessEvery> rList = new ArrayList<>();
		for (int i = 0; i < ces.length; i++) {
			if (i + 4 <= ces.length) {
				ChessEvery c1 = ces[i];
				ChessEvery c2 = ces[i + 1];
				ChessEvery c3 = ces[i + 2];
				ChessEvery c4 = ces[i + 3];
				if (!c1.isUsed() && !c2.isUsed() && !c3.isUsed() && !c4.isUsed()) {
					int c1Color = c1.getColor();
					int c1Num = c1.getNumber();
					if (c1Color == c2.getColor() && c1Color == c3.getColor() && c1Color == c4.getColor()) {
						if (c1Num == c2.getNumber() && c1Num == c3.getNumber() && c1Num == c4.getNumber()) {
							rList.add(c1);
							rList.add(c2);
							rList.add(c3);
							rList.add(c4);
							break;
						}
					}
				}
			}
		}
		return CollectionUtils.isEmpty(rList) ? null : rList;
	}

	/**
	 * 获取过路杠的索引位
	 *
	 * @param roleChess
	 * @return 为null。没有过路杠,有过路杠的话。返回过路杠的牌id
	 */
	private List<ChessEvery> guolugang(List<ChessEvery> list, ChessEvery vo) {
		ChessEvery ces[] = list.toArray(new ChessEvery[list.size()]);
		List<ChessEvery> rList = new ArrayList<>();
		for (int i = 0; i < ces.length; i++) {
			if (i + 3 <= ces.length) {
				ChessEvery c1 = ces[i];
				ChessEvery c2 = ces[i + 1];
				ChessEvery c3 = ces[i + 2];
				if (c1.isUsed() && c2.isUsed() && c3.isUsed()) {
					int v1C = vo.getColor();
					int v1N = vo.getNumber();
					if (v1C == c1.getColor() && v1C == c2.getColor() && v1C == c3.getColor()) {
						if (v1N == c1.getNumber() && v1N == c2.getNumber() && v1N == c3.getNumber()) {
							rList.add(vo);
							break;
						}
					}
				}
			}
		}
		return CollectionUtils.isEmpty(rList) ? null : rList;
	}

	/**
	 * 是否有碰
	 *
	 * @param list
	 * @return 这里返回的是自己身上牌的id
	 */
	private List<ChessEvery> peng(List<ChessEvery> list, ChessEvery vo) {
		ChessEvery ces[] = list.toArray(new ChessEvery[list.size()]);
		List<ChessEvery> rList = new ArrayList<>();
		for (int i = 0; i < ces.length; i++) {
			if (i + 2 <= ces.length) {
				ChessEvery c1 = ces[i];
				ChessEvery c2 = ces[i + 1];
				if (!c1.isUsed() && !c2.isUsed() && !vo.isUsed()) {
					int v1C = vo.getColor();
					int v1N = vo.getNumber();
					if (v1C == c1.getColor() && v1C == c2.getColor() && v1N == c1.getNumber()
					        && v1N == c2.getNumber()) {
						rList.add(c1);
						rList.add(c2);
						break;
					}
				}
			}
		}
		return CollectionUtils.isEmpty(rList) ? null : rList;
	}

	private boolean eatChess(List<ChessEvery> list, ChessEvery ce, MahjongRoom room) {
		List<ChessEvery> relationChess = getRelationChess(ce);
		relationChess = excludeSpecialChess(relationChess, room);
		// 目标牌相关联的牌小于两张怎么吃
		if (relationChess.size() < 2) {
			return false;
		}
		int[] flag = new int[relationChess.size()];
		for (int i = 0; i < relationChess.size(); i++) {
			ChessEvery chessEvery = relationChess.get(i);
			if (chessListContainsChess(list, chessEvery)) {
				flag[i] = 1;
			}
			if (i > 0 && flag[i - 1] == 1 && flag[i] == 1) {
				return true;
			}
		}
		return false;
	}

	private List<ChessEvery> excludeSpecialChess(List<ChessEvery> list, MahjongRoom room) {
		List<ChessEvery> temp = new ArrayList<>();
		for (ChessEvery ce : list) {
			if (isSpecialChess(ce, room)) {
				temp.add(new ChessEvery());
			} else {
				temp.add(ce);
			}
		}
		return temp;
	}

	private List<ChessEvery> excludeOutSpecialChess(List<ChessEvery> list, MahjongRoom room) {
		List<ChessEvery> temp = new ArrayList<>();
		for (ChessEvery ce : list) {
			if (ce.isUsed() && isSpecialChess(ce, room)) {
				continue;
			}
			temp.add(ce);
		}
		return temp;
	}

	/**
	 * 是否有明杠
	 *
	 * @param list
	 * @return 这里返回的是自己身上牌的id
	 */
	private List<ChessEvery> minggang(List<ChessEvery> list, ChessEvery vo) {
		ChessEvery ces[] = list.toArray(new ChessEvery[list.size()]);
		List<ChessEvery> rList = new ArrayList<>();
		for (int i = 0; i < ces.length; i++) {
			if (i + 3 <= ces.length) {
				ChessEvery c1 = ces[i];
				ChessEvery c2 = ces[i + 1];
				ChessEvery c3 = ces[i + 2];
				if (!c1.isUsed() && !c2.isUsed() && !c3.isUsed() && !vo.isUsed()) {
					int v1C = vo.getColor();
					int v1N = vo.getNumber();
					if (v1C == c1.getColor() && v1C == c2.getColor() && v1C == c3.getColor()) {
						if (v1N == c1.getNumber() && v1N == c2.getNumber() && v1N == c3.getNumber()) {
							rList.add(c1);
							rList.add(c2);
							rList.add(c3);
							break;
						}
					}
				}
			}
		}
		return CollectionUtils.isEmpty(rList) ? null : rList;
	}

	public int calculationFan(RoleCache roleCache) {
		int fan = 1;
		if (roleCache.getOneEatCount() != 0 || roleCache.getOnePengCount() != 0) {
			fan = fan << 1;
		}
		if (roleCache.getOneLaiZiGangCount() != 0) {
			fan = fan << 1;
			fan = fan << roleCache.getOneLaiZiGangCount();
		}
		if (roleCache.getOneAnGangCount() != 0) {
			fan = fan << 1;
			fan = fan << roleCache.getOneAnGangCount();
		}
		fan = fan << roleCache.getOneHzGangCount();
		fan = fan << roleCache.getOneRuffianGangCount();
		fan = fan << roleCache.getOneMingGangCount();

		return fan;
	}

	private int exceedLimitFan(long winRoleId, MahjongRoom room) {
		RoleCache winRole = room.getRoleCacheByRoleId(winRoleId);
		int exceedLimitFan = 0;
		for (Entry<Integer, RoleCache> e : room.getRoleCacheMap().entrySet()) {
			RoleCache cache = e.getValue();
			if (winRoleId == cache.getRoleId()) {
				continue;
			}

			int allfan = calculationFan(cache) * calculationFan(winRole) * room.getFan();
			if (winRole.getOneHaveLzCount() == 0) {
				allfan = allfan << 1;
			}
			if (allfan > room.getFanLimit()) {
				allfan = room.getFanLimit();
				exceedLimitFan++;
			}
		}
		return exceedLimitFan;
	}

	/**
	 * 积分的加减
	 *
	 * @param roleId
	 *            加分玩家的id
	 * @param roomNum
	 *            房间号
	 * @param signType
	 *            标签类型
	 * @param loseIndex
	 *            减分玩家的索引位
	 */
	public void jifen(long roleId, int roomNum, int signType, int loseIndex) {
		// 杠的积分，一次操作，加分的只可能是一个人，减分的有可能是多个
		MahjongRoom room = rooms.get(roomNum);
		RoleCache winRole = room.getRoleCacheByRoleId(roleId);

		JiFenTemplate template = mahjongDataTemplateManager.getJiFenTemplate(signType);

		int doubleBao = 0; // 位运算
		if ((room.getRuffian().getNumber() % 2) == room.getPlayType()) {
			doubleBao = 1;
		}
		int addJifen = 0;

		if (!template.isFlag()) {
			// 只扣一个玩家积分
			RoleCache loseCache = room.getRoleCacheMap().get(loseIndex);
			int allfan = calculationFan(loseCache) * calculationFan(winRole) << room.getFan();
			if (winRole.getOneHaveLzCount() == 0) {
				allfan = allfan << 1;
			}
			if (allfan > room.getFanLimit()) {
				allfan = room.getFanLimit();
			}
			Fan2JifenTemplate temp = mahjongDataTemplateManager.getFan2JifenTemplate(allfan);
			int jifen = temp.getJifen();
			jifen = jifen << doubleBao;

			loseJifen(loseCache, jifen, 0);
			addJifen = jifen;
		} else {
			int exceedLimitFan = exceedLimitFan(roleId, room);
			// 除了roleId，其他3玩家的积分都要扣
			for (Entry<Integer, RoleCache> e : room.getRoleCacheMap().entrySet()) {
				RoleCache cache = e.getValue();
				if (roleId == cache.getRoleId()) {
					continue;
				}

				int allfan = calculationFan(cache) * calculationFan(winRole) << room.getFan();
				if (winRole.getOneHaveLzCount() == 0) {
					allfan = allfan << 1;
				}
				if (allfan > room.getFanLimit()) {
					if (exceedLimitFan == 2) {
						allfan = 100; // 银顶
					}
					if (exceedLimitFan == 3) {
						allfan = 200; // 银顶
					}
				}
				Fan2JifenTemplate temp = mahjongDataTemplateManager.getFan2JifenTemplate(allfan);
				int jifen = temp.getJifen();
				jifen = jifen << doubleBao;

				loseJifen(cache, jifen, 0);
				addJifen = addJifen + jifen;
			}
		}

		RoleCache winCache = room.getRoleCacheByRoleId(roleId);
		winJifen(winCache, addJifen);
	}

	private void loseJifen(RoleCache loseCache, int jifen, int yu) {
		loseCache.setJifen(loseCache.getJifen() - jifen);
		loseCache.setOneSumfen(loseCache.getOneSumfen() - jifen);
		loseCache.setChangeJifen(loseCache.getChangeJifen() - jifen);
	}

	private void winJifen(RoleCache winCache, int jifen) {
		winCache.setJifen(winCache.getJifen() + jifen);
		winCache.setOneSumfen(winCache.getOneSumfen() + jifen);
		winCache.setChangeJifen(winCache.getChangeJifen() + jifen);
	}

	/**
	 * 积分的加减
	 *
	 * @param roleId
	 *            加分玩家的id
	 * @param roomNum
	 *            房间号
	 * @param signType
	 *            标签类型
	 * @param loseIndex
	 *            减分玩家的索引位
	 */
	// public void jifen(long roleId, int roomNum, int signType, int loseIndex)
	// {
	// // 杠的积分，一次操作，加分的只可能是一个人，减分的有可能是多个
	// MahjongRoom room = rooms.get(roomNum);
	// int fan = 1;
	// if (signType == MahjongConstant.MAHJONG_HU || signType ==
	// MahjongConstant.MAHJONG_ZIMO) {
	// // 胡和自摸才会翻番的。
	// FanTemplate temp =
	// mahjongDataTemplateManager.getFanTemplate(room.getFan());
	// fan = temp == null ? 1 : temp.getFan();
	// }
	//
	// JiFenTemplate template =
	// mahjongDataTemplateManager.getJiFenTemplate(signType);
	//
	// int jifen = template.getJifen();
	// int addJifen = 0;
	// if (!template.isFlag()) {
	// // 只扣一个玩家积分
	// RoleCache loseCache = room.getRoleCacheMap().get(loseIndex);
	// loseJifen(loseCache, template, fan, 0);
	// addJifen = jifen * fan;
	// // if (template.isYu()) {
	// // addJifen = addJifen + room.getYu();
	// // }
	// } else {
	// // 除了roleId，其他3玩家的积分都要扣
	// for (Entry<Integer, RoleCache> e : room.getRoleCacheMap().entrySet()) {
	// RoleCache cache = e.getValue();
	// if (roleId == cache.getRoleId()) {
	// continue;
	// }
	// loseJifen(cache, template, fan, 0);
	// addJifen = addJifen + jifen * fan;
	// // if (template.isYu()) {
	// // addJifen = addJifen + room.getYu();
	// // }
	// }
	// }
	//
	// RoleCache winCache = room.getRoleCacheByRoleId(roleId);
	// winJifen(winCache, template, addJifen);
	// }
	//
	// private void loseJifen(RoleCache loseCache, JiFenTemplate template, int
	// fan, int yu) {
	// int jifen = template.getJifen() * fan;
	// if (template.isYu()) {
	// jifen = jifen + yu;
	// }
	// loseCache.setJifen(loseCache.getJifen() - jifen);
	// if (template.isGang()) {
	// loseCache.setOneGangfen(loseCache.getOneGangfen() - jifen);
	// }
	// loseCache.setOneSumfen(loseCache.getOneSumfen() - jifen);
	//
	// loseCache.setChangeJifen(loseCache.getChangeJifen() - jifen);
	// }
	//
	// private void winJifen(RoleCache winCache, JiFenTemplate template, int
	// jifen) {
	// winCache.setJifen(winCache.getJifen() + jifen);
	// if (template.isGang()) {
	// winCache.setOneGangfen(winCache.getOneGangfen() + jifen);
	// }
	// winCache.setOneSumfen(winCache.getOneSumfen() + jifen);
	//
	// winCache.setChangeJifen(winCache.getChangeJifen() + jifen);
	// }

	/**
	 * 断线重连-->在打牌界面
	 *
	 * @param roleId
	 */
	public void reLogin(long roleId) {
		Role role = roleManager.getRole(roleId);
		int roomNum = role.getRoomNum();
		MahjongRoom room = rooms.get(roomNum);

		// 正常情况下不会走这一步的。8局结束以后。role的state也会清0
		if (roomNum == 0 || room == null) {
			logger.info("reload roomNum is 0, or room is null, roleId={}", roleId);
			return;
		}

		int curIndex = room.getOperateIndex();
		JSONObject res = new JSONObject();
		res.put(ErrorCode.RESULT, ErrorCode.OK);
		Object[] data = new Object[] { roomNum, room.getAlreadyJuShu(), room.getJushu(), room.getSumIndex(), curIndex,
		        pushLaiZiVo(room), ouput(roleId, room.getRoleCacheMap()), room.getZhuang() };
		res.put(MahjongConstant.CLIENT_DATA, data);

		routeManager.relayMsg(roleId, MsgType.MAHJONG_STATE_RELOGIN_MSG, res);

		// 检查当前局是否到每局结算面板，如果没有。1.检查是否有弹标签操作，2.是否有申请解散房间的操作
		if (room.isCurEnd()) {
			Map<Long, Boolean> startGameMap = room.getStartGameMap();
			if (startGameMap.containsKey(roleId)) {
				// 已经点了开始游戏
				JSONObject start_res = new JSONObject();
				start_res.put(ErrorCode.RESULT, ErrorCode.OK);
				start_res.put(MahjongConstant.CLIENT_DATA, new Object[] { roleId, room.getAllRoleJifen() });
				routeManager.relayMsg(roleId, MsgType.MAHJONG_START_GAME_MSG, start_res);
			} else {
				// 没点的话。就弹每局结算面板
				long winRoleId = room.getRoleIdByIndex(room.getOperateIndex());
				routeManager.relayMsg(roleId, MsgType.MAHJONG_HU_MSG, oneEnd(room, new Object[] { winRoleId }));
			}
		} else {
			// 查看玩家是否有弹标签操作
			RoleCache roleCache = room.getRoleCacheByRoleId(roleId);
			int index = room.getIndexByRoleId(roleId);
			boolean mo = index == curIndex;
			ChessEvery che = mo ? room.getCurrMoChessEvery() : room.getCurrChessEvery();
			check(roleCache, mo, che, false);

			// 检查当前局是否有人解散房间
			long disMissRoleId = room.getDisMissApplyRoleId();
			if (disMissRoleId != 0) {
				int errorCode = ErrorCode.Error_1021;
				if (disMissRoleId != roleId) {
					errorCode = ErrorCode.Error_1022;
				}
				int ms = ConfigConstant.DISMISS_MINUTES * 60 * 1000;

				RoleCache disRoleCache = room.getRoleCacheByRoleId(disMissRoleId);
				JSONObject disResult = new JSONObject();
				disResult.put(ErrorCode.RESULT, ErrorCode.EC_OK);
				Object[] disResultData = new Object[] {
				        new Object[] { errorCode, disRoleCache.getName(), ms, disRoleCache.getRoleId() },
				        roleCacheNotIncludeMyself(disMissRoleId, room.getRoleCacheMap(), room.getDisMissMap()) };
				disResult.put(MahjongConstant.CLIENT_DATA, disResultData);
				routeManager.relayMsg(roleId, MsgType.MAHJONG_DISMISS_MSG, disResult);
			}
		}
	}

	private Object[] ouput(long roleId, Map<Integer, RoleCache> map) {

		List<Object[]> result = new ArrayList<>();
		for (Entry<Integer, RoleCache> e : map.entrySet()) {
			RoleCache cache = e.getValue();
			List<ChessEvery> list = cache.getRoleChessList();
			List<ChessEvery> outChess = cache.getOutChessList();
			if (cache.getRoleId() != roleId) {
				List<ChessEvery> otherList = new ArrayList<>();
				for (ChessEvery c : list) {
					if (c.isUsed()) {
						otherList.add(c);
					}
				}
				list = otherList;
			}

			MahjongRoom room = rooms.get(roleManager.getRole(roleId).getRoomNum());
			List<ChessEvery> outSpecial = new ArrayList<>();
			for (ChessEvery ce : list) {
				if (ce.isUsed() && isSpecialChess(ce, room)) {
					outSpecial.add(ce);
				}
			}
			list.removeAll(outSpecial);

			result.add(new Object[] { e.getKey(), cache.getRoleId(), cache.getName(), cache.getIp(), cache.getJifen(),
			        cache.getHeadChess().size(), allChessVo(list), allChessVo(outChess), allChessVo(outSpecial) });

			list.addAll(outSpecial); // 防止影响其他模块
		}
		return result.toArray();
	}

	/**
	 * 房间聊天
	 *
	 * @param roleId
	 * @param roomNum
	 * @param data
	 */
	public void chat(long roleId, int roomNum, JSONObject data) {
		JSONArray jsonArray = (JSONArray) data.get(MahjongConstant.CLIENT_DATA);
		Object[] objects = jsonArray.toArray();
		int type = ObjectUtil.obj2int(objects[0]);

		MahjongRoom room = rooms.get(roomNum);
		JSONObject res = new JSONObject();
		res.put(ErrorCode.RESULT, ErrorCode.EC_OK);
		res.put(MahjongConstant.CLIENT_DATA, new Object[] { room.getIndexByRoleId(roleId), type });

		routeManager.broadcast(room.getAllRoleId(), MsgType.MAHJONG_CHAT_MSG, res);
	}

	/**
	 * GM解散房间
	 *
	 * @param roleId
	 */
	public void GMDisMissRoom(long roleId) {
		Role role = roleManager.getRole(roleId);
		int roomNum = role.getRoomNum();
		MahjongRoom room = rooms.get(roomNum);
		if (room == null) {
			logger.info("GM命令，此房间已经解散了。不用继续解散,roomNum={}", roomNum);
			return;
		}
		Role leaderRole = roleManager.getRole(room.getLeaderId());

		long allRoleIds[] = removeRoom(roomNum);

		// 告诉所有玩家 房间已经解散了。
		JSONObject res = new JSONObject();
		res.put(ErrorCode.RESULT, ErrorCode.EC_OK);
		res.put(MahjongConstant.CLIENT_DATA, new Object[] { leaderRole.getName(), leaderRole.getId() });
		routeManager.broadcast(allRoleIds, MsgType.DISSOLVED_ROOM_MSG, res);

		logger.info("dissolveRoom end, roleId={}, name={}, roomNum={}", leaderRole, leaderRole.getName(), roomNum);
	}

	/**
	 * 改变摸牌的第一张牌属性
	 *
	 * @param roleId
	 * @param c
	 * @param n
	 * @return
	 */
	public String GMChangeNextChess(long roleId, int c, int n) {
		Role role = roleManager.getRole(roleId);
		int roomNum = role.getRoomNum();
		MahjongRoom room = rooms.get(roomNum);
		ChessEvery[] all = room.getRoomChess();
		int index = room.getPaindex();
		int left = room.getLastIndex() + 1 - index;
		ChessEvery[] newAll = new ChessEvery[left];

		System.arraycopy(all, index, newAll, 0, left);

		Integer newIndex = getChessEveryByCN(c, n, newAll);
		if (newIndex == null) {
			String s = "找不到这张牌,可能在其他玩家手里。或已全部打出去了，roleId=" + roleId + ",c=" + c + ",n=" + n + "";
			return s;
		} else {
			ChessEvery next = all[index];
			ChessEvery newNext = new ChessEvery(next.getId(), next.getNumber(), next.getColor(), next.isUsed());
			ChessEvery find = all[newIndex + index];
			next.setColor(find.getColor());
			next.setNumber(find.getNumber());

			find.setColor(newNext.getColor());
			find.setNumber(newNext.getNumber());
		}
		return null;
	}

	/**
	 * 改变最后一张牌的属性
	 *
	 * @param roleId
	 * @param c
	 * @param n
	 * @return
	 */
	public String GMChangeLastChess(long roleId, int c, int n) {
		Role role = roleManager.getRole(roleId);
		int roomNum = role.getRoomNum();
		MahjongRoom room = rooms.get(roomNum);
		ChessEvery[] all = room.getRoomChess();
		int index = room.getPaindex();
		int lastIndex = room.getLastIndex();
		int left = lastIndex - index + 1;
		ChessEvery[] newAll = new ChessEvery[left];

		System.arraycopy(all, index, newAll, 0, left);

		Integer newIndex = getChessEveryByCN(c, n, newAll);
		if (newIndex == null) {
			String s = "找不到这张牌,可能在其他玩家手里。或已全部打出去了，roleId=" + roleId + ",c=" + c + ",n=" + n + "";
			return s;
		} else {
			ChessEvery last = all[lastIndex];
			ChessEvery newLast = new ChessEvery(last.getId(), last.getNumber(), last.getColor(), last.isUsed());
			ChessEvery find = all[newIndex + index];
			last.setColor(find.getColor());
			last.setNumber(find.getNumber());

			find.setColor(newLast.getColor());
			find.setNumber(newLast.getNumber());
		}
		return null;
	}

	/**
	 * gm命令。改变当前手里的牌
	 *
	 * @param roleId
	 * @param map
	 */
	public String GMChangeChess(long roleId, Map<Integer, List<Integer>> map) {
		Role role = roleManager.getRole(roleId);
		int roomNum = role.getRoomNum();
		MahjongRoom room = rooms.get(roomNum);
		RoleCache cache = room.getRoleCacheByRoleId(roleId);
		List<ChessEvery> list = cache.getRoleChessList();
		ChessEvery[] all = room.getRoomChess();
		int index = room.getPaindex();
		int left = all.length - index;
		ChessEvery[] newAll = new ChessEvery[left];

		System.arraycopy(all, index, newAll, 0, left);

		int i = 0;
		for (Integer key : map.keySet()) {
			List<Integer> chesses = map.get(key);
			for (Integer chess : chesses) {
				ChessEvery roleChess = list.get(i);
				ChessEvery temp = new ChessEvery(roleChess.getId(), roleChess.getNumber(), roleChess.getColor(),
				        roleChess.isUsed());
				// 先去摸牌列表里找这张牌
				Integer newIndex = getChessEveryByCN(key, chess, newAll);
				ChessEvery findChess = null;
				if (newIndex == null) {
					// 在摸牌列表里没有找到。在自己身上找一下
					Integer roleIndex = getChessEveryByCN(key, chess, list.toArray(new ChessEvery[list.size()]));
					if (roleIndex == null) {
						String s = "找不到这张牌,可能在其他玩家手里。或已全部打出去了，roleId=" + roleId + ",c=" + key + ",n=" + chess + "";
						return s;
					} else {
						findChess = list.get(roleIndex);
					}
				} else {
					findChess = all[newIndex + index];
				}

				roleChess.setColor(findChess.getColor());
				roleChess.setNumber(findChess.getNumber());

				findChess.setColor(temp.getColor());
				findChess.setNumber(temp.getNumber());
				i++;
			}
		}
		cache.sortRoleChess(false);
		pushOneRoleVo(cache);
		return null;
	}

	/**
	 * 这张牌在摸牌列表里是否还有
	 *
	 * @param c
	 * @param n
	 * @param list
	 * @return
	 */
	private Integer getChessEveryByCN(int c, int n, ChessEvery[] newAll) {
		int i = 0;
		for (ChessEvery ch : newAll) {
			if (ch.getColor() == c && ch.getNumber() == n) {
				return i;
			}
			i++;
		}
		return null;
	}

	/**
	 * 推送给房间里的所有人 总战绩
	 * 
	 * @param roleId
	 * @return
	 */
	public void pushAllSumScore(MahjongRoom room) {
		List<Object[]> list = new ArrayList<>();
		JSONObject res = new JSONObject();
		res.put(ErrorCode.RESULT, ErrorCode.OK);
		Map<Integer, RoleCache> map = room.getRoleCacheMap();
		for (Entry<Integer, RoleCache> e : map.entrySet()) {
			RoleCache cache = e.getValue();
			list.add(new Object[] { e.getKey(), cache.getRoleId(), cache.getName(), cache.getJifen(),
			        cache.getZiMoCount(), cache.getJiePaoCount(), cache.getDianPaoCount(), cache.getAnGangCount(),
			        cache.getMingGangCount() });
		}

		long bestPaoId = -1;
		long bestWinerId = -1;
		int tempPaoCount = 0;
		int tempMaxWin = 0;
		// 把战绩的id放到role身上
		for (Entry<Integer, RoleCache> e : map.entrySet()) {
			RoleCache cache = e.getValue();
			Role role = roleManager.getRole(cache.getRoleId());
			role.getLogIds().add(cache.getCurJuLogId());
			roleRepository.cacheUpdate(role);
			if (cache.getDianPaoCount() >= tempPaoCount) {
				tempPaoCount = cache.getDianPaoCount();
				bestPaoId = cache.getRoleId();
			}
			if (cache.getJifen() > tempMaxWin) {
				tempMaxWin = cache.getJifen();
				bestWinerId = cache.getRoleId();
			}
		}

		long allRoleIds[] = removeRoom(room.getRoomNum());

		res.put(MahjongConstant.CLIENT_DATA, new Object[] { bestPaoId, bestWinerId,
		        TimeUtil.format(new Date(), TimeUtil.PATTERN_yyyy_MM_dd_HH_mm_ss), list.toArray() });
		routeManager.broadcast(allRoleIds, MsgType.MAHJONG_SUM_SCORE_MSG, res);
	}

	/**
	 * 申请解散房间
	 *
	 * @param roleId
	 * @return
	 */
	public JSONObject disMiss(long roleId) {
		JSONObject res = new JSONObject();
		res.put(ErrorCode.RESULT, ErrorCode.EC_OK);
		Role role = roleManager.getRole(roleId);
		if (role == null) {
			res.put(ErrorCode.RESULT, ErrorCode.FAILED);
			res.put(ErrorCode.CODE, ErrorCode.ROLE_NOT_EXIST);
			return res;
		}

		int roomNum = role.getRoomNum();
		MahjongRoom room = rooms.get(roomNum);
		if (room == null) {
			res.put(ErrorCode.RESULT, ErrorCode.FAILED);
			res.put(ErrorCode.CODE, ErrorCode.ROLE_NOT_IN_ROOM);
			return res;
		}

		logger.info("apply dismiss room,roleId={}, name={}, roomNum={}", roleId, role.getName(), roomNum);

		int errorCode = ErrorCode.Error_1021;
		int ms = ConfigConstant.DISMISS_MINUTES * 60 * 1000;

		// 定时操作
		// 先取消定时器
		GameServerContext.getAsyncManager().removeSchedule(ScheduleType.DISMISS_MIN, roomNum);

		Object args[] = new Object[] { roleId, roomNum };
		GameServerContext.getAsyncManager().scheduleOnce(ScheduleType.DISMISS_MIN, ModuleConstant.MODULE_PLAY_MAHJONG,
		        roomNum, ms, args);

		// 添加申请人缓存
		room.setDisMissApplyRoleId(roleId);
		room.addDisMissMap(roleId, true);

		Map<Integer, RoleCache> map = room.getRoleCacheMap();
		for (Entry<Integer, RoleCache> e : map.entrySet()) {
			RoleCache cache = e.getValue();
			if (cache.getRoleId() == roleId) {
				errorCode = ErrorCode.Error_1022;
				continue;
			}

			// res.put(MahjongConstant.CLIENT_DATA, new Object[] { new Object[]
			// { role.getName(), ms, roleId },
			// roleCacheNotIncludeMyself(roleId, map, room.getDisMissMap()) });

			res.put(MahjongConstant.CLIENT_DATA, new Object[] { role.getName(), ms, roleId });
			routeManager.relayMsg(cache.getRoleId(), MsgType.MAHJONG_DISMISS_MSG, res);
		}
		return null;

	}

	/**
	 * 解散房间时间到
	 *
	 * @param roleId
	 * @param roomNum
	 */
	@Scheduled(type = ScheduleType.DISMISS_MIN)
	public void disMissTimeEnd(long roleId, int roomNum) {
		logger.info("dismiss room time end,roleId={},  roomNum={}", roleId, roomNum);
		Role role = roleManager.getRole(roleId);
		if (role == null) {
			logger.warn("解散房间定时时间已到，找不到角色,roleId={}", roleId);
			return;
		}
		if (role.getRoomNum() != roomNum) {
			logger.warn("解散房间定时时间已到，房间号和角色身上的不一致,roleId={}, roomNum={}", roleId, roomNum);
			return;
		}
		MahjongRoom room = rooms.get(roomNum);
		if (room == null) {
			logger.warn("解散房间定时时间已到，找不到房间, roomNum={}", roomNum);
			return;
		}

		JSONObject res = new JSONObject();
		res.put(ErrorCode.RESULT, ErrorCode.OK);
		res.put(MahjongConstant.CLIENT_DATA,
		        new Object[] { MahjongConstant.DISMISS_TIME_END, roomNum, ConfigConstant.DISMISS_MINUTES });
		routeManager.broadcast(room.getAllRoleId(), MsgType.MAHJONG_DISMISS_OPERATE_MSG, res);

		// 可以点击查看总战绩
		room.setSumScorePanel(true);

		// 推送每局结算面板
		oneEndOutput(room, room.getOperateIndex(), MahjongConstant.CUR_JU_END_DISSROOM_TIME_END, null);
	}

	/**
	 * 申请解散房间后续操作
	 *
	 * @param roleId
	 * @param type
	 *            1=同意 2=拒绝
	 * @return
	 */
	public JSONObject disMissOperate(long roleId, int type) {
		JSONObject res = new JSONObject();
		res.put(ErrorCode.RESULT, ErrorCode.EC_OK);
		Role role = roleManager.getRole(roleId);
		if (role == null) {
			res.put(ErrorCode.RESULT, ErrorCode.FAILED);
			res.put(ErrorCode.CODE, ErrorCode.ROLE_NOT_EXIST);
			return res;
		}

		int roomNum = role.getRoomNum();
		MahjongRoom room = rooms.get(roomNum);
		if (room == null) {
			res.put(ErrorCode.RESULT, ErrorCode.FAILED);
			res.put(ErrorCode.CODE, ErrorCode.ROLE_NOT_IN_ROOM);
			return res;
		}

		logger.info("dismiss room operate,roleId={}, name={}, roomNum={},type={}", roleId, role.getName(), roomNum,
		        type);

		RoleCache cache = room.getRoleCacheByRoleId(roleId);
		int agreeSize = room.getRoleCacheMap().size() - 1;
		Map<Long, Boolean> disMissMap = room.getDisMissMap();
		long allRoleId[] = room.getAllRoleId();
		if (type == MahjongConstant.DISMISS_AGREE) {
			room.addDisMissMap(roleId, true);
			res.put(MahjongConstant.CLIENT_DATA, new Object[] { type, new Object[] { roleId, role.getName() } });
			if (disMissMap.size() >= agreeSize) {
				Object[] obj = disMissOutPut(disMissMap, roomNum);
				type = MahjongConstant.DISMISS_ALL_AGREE;
				// 是否要推送总战绩的标示
				room.setSumScorePanel(true);
				// 推送每局结算面板
				oneEndOutput(room, room.getOperateIndex(), MahjongConstant.CUR_JU_END_DISSROOM_THREE, null);
				res.put(MahjongConstant.CLIENT_DATA, new Object[] { type, obj });

				// 取消定时
				GameServerContext.getAsyncManager().removeSchedule(ScheduleType.DISMISS_MIN, roomNum);
			}
		} else {
			res.put(MahjongConstant.CLIENT_DATA, new Object[] { type, new Object[] { roleId, cache.getName() } });

			room.setDisMissApplyRoleId(0);
			disMissMap.clear();

			// 取消定时
			GameServerContext.getAsyncManager().removeSchedule(ScheduleType.DISMISS_MIN, roomNum);
		}

		routeManager.broadcast(allRoleId, MsgType.MAHJONG_DISMISS_OPERATE_MSG, res);

		return null;
	}

	private Object[] disMissOutPut(Map<Long, Boolean> disMissMap, int roomNum) {
		List<Object[]> list = new ArrayList<>();
		MahjongRoom room = rooms.get(roomNum);
		for (Long roleId : disMissMap.keySet()) {
			RoleCache cache = room.getRoleCacheByRoleId(roleId);
			list.add(new Object[] { roleId, cache.getName() });
		}
		return list.toArray();
	}

	/**
	 * 获取缓存中的roleid和name。不包含自己
	 *
	 * @param roleId
	 * @param map
	 * @return
	 */
	// private JSONArray roleCacheNotIncludeMyself2(long roleId, Map<Integer,
	// RoleCache> map,
	// Map<Long, Boolean> disMissMap) {
	// JSONArray res = new JSONArray();
	// for (Entry<Integer, RoleCache> e : map.entrySet()) {
	// RoleCache cache = e.getValue();
	// if (cache.getRoleId() == roleId) {
	// continue;
	// }
	// Boolean f = disMissMap.get(cache.getRoleId());
	// boolean flag = f == null ? false : f;
	// JSONObject object = new JSONObject();
	// object.put("roleId", cache.getRoleId());
	// object.put("name", cache.getName());
	// object.put("flag", flag);
	// res.add(object);
	// }
	// return res;
	// }

	/**
	 * 获取缓存中的roleid和name。不包含自己
	 * 
	 * @param roleId
	 * @param map
	 * @return
	 */
	private Object[] roleCacheNotIncludeMyself(long roleId, Map<Integer, RoleCache> map,
	        Map<Long, Boolean> disMissMap) {
		List<Object[]> list = new ArrayList<>();
		for (Entry<Integer, RoleCache> e : map.entrySet()) {
			RoleCache cache = e.getValue();
			if (cache.getRoleId() == roleId) {
				continue;
			}
			Boolean f = disMissMap.get(cache.getRoleId());
			boolean flag = f == null ? false : f;
			list.add(new Object[] { cache.getRoleId(), cache.getName(), flag });
		}
		return list.toArray();
	}

	/**
	 * 获取缓存中的roleid和name。包含自己
	 *
	 * @param roleId
	 * @param map
	 * @return
	 */
	private Object[] roleCacheIncludeMyself(long roleId, Map<Integer, RoleCache> map) {
		List<Object[]> list = new ArrayList<>();
		for (Entry<Integer, RoleCache> e : map.entrySet()) {
			RoleCache cache = e.getValue();
			list.add(new Object[] { cache.getRoleId(), cache.getName() });
		}
		return list.toArray();
	}

	/**
	 * 得到一张要打出去的牌
	 */
	public ChessEvery getOutChess(List<ChessEvery> list) {
		int used[] = new int[list.size()];
		List<ChessEvery> cs = new ArrayList<>();
		ChessEvery first = null;
		for (int i = 0; i < list.size(); i++) {
			boolean flag = false;
			if (i <= list.size() - 1 && used[i] == 0) {
				ChessEvery curCe = list.get(i);
				if (first == null) {
					first = curCe;
				}
				if (curCe.isUsed()) {
					continue;
				}
				ChessEvery nextCe = list.get(i + 1);
				if (curCe.getNumber() == nextCe.getNumber() && curCe.getColor() == nextCe.getColor()) {
					used[i] = 1;
					used[i + 1] = 1;
					flag = true;
					if (i <= list.size() - 3) {
						nextCe = list.get(i + 2);
						if (curCe.getNumber() == nextCe.getNumber() && curCe.getColor() == nextCe.getColor()) {
							used[i + 2] = 1;
						}
					}
				}

				boolean f = false;
				if (curCe.getNumber() + 1 == nextCe.getNumber() && curCe.getColor() == nextCe.getColor()) {
					used[i] = 1;
					used[i + 1] = 1;
					flag = true;
					f = true;
				}
				if (f && i <= list.size() - 3) {
					nextCe = list.get(i + 2);
					if (curCe.getNumber() + 2 == nextCe.getNumber() && curCe.getColor() == nextCe.getColor()) {
						used[i] = 1;
						used[i + 2] = 1;
						flag = true;
					}
				}

				if (!flag) {
					cs.add(curCe);
				}
			}
		}
		if (CollectionUtils.isEmpty(cs)) {
			cs.add(first);
		}
		return Lottery.random(cs);
	}

	/**
	 * 拉去战绩面板
	 *
	 * @param roleId
	 * @param pageNum
	 * @return
	 */
	public JSONObject getZhanJiInfo(long roleId, int pageNum) {
		Role role = roleManager.getRole(roleId);
		JSONObject res = new JSONObject();
		res.put(ErrorCode.RESULT, ErrorCode.EC_OK);
		if (role == null) {
			res.put(ErrorCode.RESULT, ErrorCode.EC_FAILED);
			res.put(ErrorCode.CODE, ErrorCode.ROLE_NOT_EXIST);
			return res;
		}
		Date now = new Date();
		int beforeDay = -(ConfigConstant.ZHANJI_BEFORE_DAYS - 1);
		long start = TimeUtil.getTimeStart(now, beforeDay);
		long end = TimeUtil.getTimeStart(now, 1);
		int startNum = (pageNum - 1) * MahjongConstant.ZHANJI_RESULT_PAGE_SIZE;
		int endNum = MahjongConstant.ZHANJI_RESULT_PAGE_SIZE;

		List<MahjongResultLog> list = mahjongResultLogRepository.getAllResultLog(role.getLogIds(), new Date(start),
		        new Date(end), startNum, endNum);
		List<Object[]> resData = new ArrayList<>();
		if (CollectionUtils.isNotEmpty(list)) {
			for (MahjongResultLog log : list) {
				Map<Long, Integer> jifenMap = new HashMap<>();
				Map<Long, String> nameMap = new HashMap<>();
				for (Integer jushu : log.getAllInfo().keySet()) {
					MahjongResultData data = log.getAllInfo().get(jushu);
					for (MahjongResultDetailsData detailsData : data.getInfos()) {
						Integer jifen = jifenMap.get(detailsData.getRoleId());
						if (jifen == null) {
							jifen = 0;
							jifenMap.put(detailsData.getRoleId(), jifen);
							nameMap.put(detailsData.getRoleId(), detailsData.getName());
						}
						jifenMap.put(detailsData.getRoleId(), detailsData.getJifen() + jifen);
					}
					/*
					 * for(Entry<Integer, PlayBackVo> e :
					 * data.getPlayBackMap().entrySet()){ PlayBackVo vo =
					 * e.getValue(); logger.info(
					 * "第{}局的回放记录，第{}步，回放类型={}，roleId={},当前操作索引={}，干了={}",jushu,
					 * e.getKey(), vo.getType(), vo.getRoleId(), vo.getIndex(),
					 * vo.getObj()); }
					 */
				}

				List<Object[]> roles = new ArrayList<>();
				for (Long rId : jifenMap.keySet()) {
					int jifen = jifenMap.get(rId);
					roles.add(new Object[] { rId, nameMap.get(rId), jifen });
				}
				resData.add(new Object[] { log.getId(), log.getRoomNum(), roles.toArray(),
				        DatatimeUtil.parseDateToString(log.getAddTime()) });

			}
		}
		res.put(MahjongConstant.CLIENT_DATA, resData.toArray());
		return res;
	}

	/**
	 * 战绩详情
	 *
	 * @param roleId
	 * @param id
	 * @return
	 */
	public JSONObject getZhanJiDetailsInfo(long roleId, long id) {
		JSONObject res = new JSONObject();
		res.put(ErrorCode.RESULT, ErrorCode.EC_OK);
		Role role = roleManager.getRole(roleId);
		if (role == null) {
			res.put(ErrorCode.RESULT, ErrorCode.EC_FAILED);
			res.put(ErrorCode.CODE, ErrorCode.ROLE_NOT_EXIST);
			return res;
		}
		MahjongResultLog log = mahjongResultLogRepository.cacheLoad(id);
		if (log == null) {
			res.put(ErrorCode.RESULT, ErrorCode.EC_FAILED);
			res.put(ErrorCode.CODE, ErrorCode.LOOK_ZHANJI_NOT_DATA);
			return res;
		}

		JSONArray array = new JSONArray();
		for (Integer jushu : log.getAllInfo().keySet()) {
			MahjongResultData data = log.getAllInfo().get(jushu);
			JSONArray roles = new JSONArray();
			for (MahjongResultDetailsData detailsData : data.getInfos()) {
				JSONObject roleJson = new JSONObject();
				roleJson.put("roleId", detailsData.getRoleId());
				roleJson.put("name", detailsData.getName());
				roleJson.put("jifen", detailsData.getJifen());
				roles.add(roleJson);
			}
			JSONObject object = new JSONObject();
			object.put("jushu", jushu);
			object.put("startTime", data.getStartTime());
			object.put("roles", roles);
		}
		res.put("data", array);
		return res;
	}

	/**
	 * 战绩回放
	 *
	 * @param roleId
	 * @param id
	 *            战绩的id
	 * @param xuhao
	 *            序号，就是局数
	 * @return
	 */
	public JSONObject playBack(long roleId, long id, int xuhao) {
		JSONObject res = new JSONObject();
		res.put(ErrorCode.RESULT, ErrorCode.EC_OK);
		Role role = roleManager.getRole(roleId);
		if (role == null) {
			res.put(ErrorCode.RESULT, ErrorCode.EC_FAILED);
			res.put(ErrorCode.CODE, ErrorCode.ROLE_NOT_EXIST);
			return res;
		}
		MahjongResultLog log = mahjongResultLogRepository.cacheLoad(id);
		if (log == null) {
			res.put(ErrorCode.RESULT, ErrorCode.EC_FAILED);
			res.put(ErrorCode.CODE, ErrorCode.LOOK_ZHANJI_NOT_DATA);
			return res;
		}
		MahjongResultData data = log.getAllInfo().get(xuhao);
		if (data == null) {
			res.put(ErrorCode.RESULT, ErrorCode.EC_FAILED);
			res.put(ErrorCode.CODE, ErrorCode.PLAY_BACK_NOT_FIND);
			return res;
		}
		String fileName = getPlayBackFileName(log.getId(), log.getRoomNum(), xuhao, log.getAddTime());
		String filePath = getFilePath(fileName);
		String json = FileUtil.read(filePath);
		Map<Integer, JSONObject> playBackMap = JSON.parseObject(json, Map.class);

		List<Object[]> result = new ArrayList<>();
		JSONArray array = new JSONArray();
		for (Entry<Integer, JSONObject> e : playBackMap.entrySet()) {
			PlayBackVo vo = JSONObject.toJavaObject(e.getValue(), PlayBackVo.class);
			JSONObject object = new JSONObject();
			object.put("key", e.getKey());
			object.put("type", vo.getType());
			object.put("roleId", vo.getRoleId());
			object.put("seatId", vo.getIndex());
			object.put("obj", JsonUtil.toJavaObject(vo.getObj()));
			array.add(object);
		}
		res.put("data", array);
		return res;
	}
}