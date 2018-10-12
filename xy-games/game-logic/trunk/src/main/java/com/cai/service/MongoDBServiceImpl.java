package com.cai.service;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.SortedMap;
import java.util.Timer;
import java.util.concurrent.LinkedBlockingQueue;
import com.cai.common.define.ECardType;
import com.cai.common.define.ELogType;
import com.cai.common.define.ESysLogLevelType;
import com.cai.common.domain.BrandChildLogModel;
import com.cai.common.domain.BrandLogModel;
import com.cai.common.domain.BrandResultModel;
import com.cai.common.domain.CardLogModel;
import com.cai.common.domain.ClubLogModel;
import com.cai.common.domain.Event;
import com.cai.common.domain.GameLogModel;
import com.cai.common.domain.Player;
import com.cai.common.domain.RedPackageModel;
import com.cai.common.domain.RobotModel;
import com.cai.common.domain.Room;
import com.cai.common.domain.RoomClubInfo;
import com.cai.common.domain.ServerErrorLogModel;
import com.cai.common.domain.SystemLogModel;
import com.cai.common.domain.SystemLogQueueModel;
import com.cai.core.MongoDBUpdateThreadPool;
import com.cai.core.MonitorEvent;
import com.cai.core.SystemConfig;
import com.cai.domain.Session;
import com.cai.timer.MogoDBTimer;

/**
 * mogodb服务类
 * 
 * @author run
 *
 */
public class MongoDBServiceImpl extends AbstractService {

	/**
	 * 日志队列,仅用于批量插入
	 */
	private LinkedBlockingQueue<Object> logQueue = new LinkedBlockingQueue<>();

	private Timer timer;

	private static MongoDBServiceImpl instance = null;

	// 引用
	private MogoDBTimer mogoDBTimer;

	private MongoDBServiceImpl() {
		timer = new Timer("Timer-MongoDBServiceImpl Timer");
	}

	public static MongoDBServiceImpl getInstance() {
		if (null == instance) {
			instance = new MongoDBServiceImpl();
		}
		return instance;
	}


	/**
	 * 服务器报错日志
	 * 
	 * @param account_id
	 * @param eLogType
	 * @param msg
	 * @param v1
	 * @param v2
	 * @param account_ip
	 */
	public void server_error_log(int roomId, ELogType eLogType, String msg, Long accountID, String extractMsg, int appId) {
		try {
			ServerErrorLogModel gameLogModel = new ServerErrorLogModel();
			gameLogModel.setCreate_time(new Date());
			gameLogModel.setLogic_id(SystemConfig.logic_index);
			gameLogModel.setLog_type(eLogType.getId());
			gameLogModel.setMsg(msg);
			gameLogModel.setRoomId(roomId);
			gameLogModel.setExtractMsg(extractMsg);
			gameLogModel.setAppId(appId);
			gameLogModel.setAccountId(accountID);
			logQueue.add(gameLogModel);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void server_error_log(int roomId, ELogType eLogType, String msg, Long accountID, String extractMsg) {
		try {
			ServerErrorLogModel gameLogModel = new ServerErrorLogModel();
			gameLogModel.setCreate_time(new Date());
			gameLogModel.setLogic_id(SystemConfig.logic_index);
			gameLogModel.setLog_type(eLogType.getId());
			gameLogModel.setMsg(msg);
			gameLogModel.setRoomId(roomId);
			gameLogModel.setExtractMsg(extractMsg);
			logQueue.add(gameLogModel);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 玩家重要数值日志
	 * 
	 * @param account_id
	 * @param eLogType
	 * @param msg
	 * @param v1
	 * @param v2
	 * @param account_ip
	 */
	public void log(Room room, ELogType eLogType, String msg, Long v1, Long v2, String account_ip) {
		GameLogModel gameLogModel = new GameLogModel();
		gameLogModel.setCreate_time(new Date());
		gameLogModel.setAccount_id(room.getRoom_owner_account_id());
		gameLogModel.setLogic_id(SystemConfig.logic_index);
		gameLogModel.setLog_type(eLogType.getId());
		gameLogModel.setMsg(msg);
		gameLogModel.setV1(v1);
		gameLogModel.setV2(v2);
		gameLogModel.setLocal_ip(SystemConfig.localip);
		gameLogModel.setAccount_ip(account_ip);
		gameLogModel.setAppId(room.getGame_id());
		gameLogModel.setOpenRoomWay(room.getCreate_type());
		gameLogModel.setGameTypeIndex(room.getGameTypeIndex());
		logQueue.add(gameLogModel);
	}

	public void robot_log(long account_id, ELogType eLogType, String msg, long v1, String groupID, String groupName, int _game_type_index,
			int _game_round, int _game_rule_index, int roomID, String nick_names) {
		RobotModel robotModel = new RobotModel();
		robotModel.setCreate_time(new Date());
		robotModel.setAccount_id(account_id);
		robotModel.setLogic_id(SystemConfig.logic_index);
		robotModel.setMsg(msg);
		robotModel.setV1(v1);
		robotModel.setV2(1l);
		robotModel.setGroupId(groupID);
		robotModel.setGroupName(groupName);
		robotModel.setGame_round(_game_round);
		robotModel.setGame_rule_index(_game_rule_index);
		robotModel.setGame_type_index(_game_type_index);
		robotModel.setRoomID(roomID);
		robotModel.setNick_names(nick_names);
		logQueue.add(robotModel);
	}

	public void club_log(long account_id, ELogType eLogType, String msg, long v1, int clubId, int _game_type_index, int _game_round,
			int _game_rule_index, int roomID, String playerMsg, boolean isExclusive) {
		ClubLogModel clubLogModel = new ClubLogModel();
		clubLogModel.setCreate_time(new Date());
		clubLogModel.setAccount_id(account_id);
		clubLogModel.setLogic_id(SystemConfig.logic_index);
		clubLogModel.setMsg(msg);
		clubLogModel.setV1(v1);
		clubLogModel.setV2(1l);
		clubLogModel.setClubId(clubId);
		clubLogModel.setGame_round(_game_round);
		clubLogModel.setGame_rule_index(_game_rule_index);
		clubLogModel.setPlayerMsg(playerMsg);
		clubLogModel.setGame_type_index(_game_type_index);
		clubLogModel.setRoomID(roomID);
		clubLogModel.setExclusiveGold(isExclusive);
		logQueue.add(clubLogModel);
	}

	public void insert_Model(Serializable card) {
		logQueue.add(card);
	}

	/**
	 * 牌型日志
	 * 
	 * @param account_id
	 * @param eLogType
	 * @param msg
	 * @param v1
	 * @param v2
	 * @param account_ip
	 */
	public void card_log(Player player, ECardType eLogType, String msg, int v1, Long v2, int roomID) {
		try {
			CardLogModel cardLogModel = new CardLogModel();
			cardLogModel.setCreate_time(new Date());
			cardLogModel.setAccount_id(player.getAccount_id());
			cardLogModel.setLog_type(eLogType.getId());
			cardLogModel.setMsg(msg);
			cardLogModel.setV1((int) v1);
			cardLogModel.setV2(v2);
			cardLogModel.setRoom_id(roomID);
			
			Room room = PlayerServiceImpl.getInstance().getRoomMap().get(roomID);
			if(room!=null) {
				cardLogModel.setGame_id(room.getGame_id());
			}
			
			logQueue.add(cardLogModel);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 系统日志
	 * 
	 * @param eLogType
	 * @param msg
	 * @param v1
	 * @param v2
	 */
	public void systemLog(ELogType eLogType, String msg, Long v1, Long v2, ESysLogLevelType eSysLogLevelType) {
		SystemLogModel systemLogModel = new SystemLogModel();
		systemLogModel.setCreate_time(new Date());
		systemLogModel.setLogic_id(SystemConfig.logic_index);
		systemLogModel.setLog_type(eLogType.getId());
		systemLogModel.setMsg(msg);
		systemLogModel.setV1(v1);
		systemLogModel.setV2(v2);
		systemLogModel.setLocal_ip(SystemConfig.localip);
		systemLogModel.setLevel(eSysLogLevelType.getId());
		logQueue.add(systemLogModel);
	}

	/**
	 * 系统日志
	 * 
	 * @param eLogType
	 * @param msg
	 * @param v1
	 * @param v2
	 */
	public void systemLog_queue(ELogType eLogType, String msg, Long v1, Long v2, ESysLogLevelType eSysLogLevelType) {
		SystemLogQueueModel systemLogModel = new SystemLogQueueModel();
		systemLogModel.setCreate_time(new Date());
		systemLogModel.setLogic_id(SystemConfig.logic_index);
		systemLogModel.setLog_type(eLogType.getId());
		systemLogModel.setMsg(msg);
		systemLogModel.setV1(v1);
		systemLogModel.setV2(v2);
		systemLogModel.setLocal_ip(SystemConfig.localip);
		systemLogModel.setLevel(eSysLogLevelType.getId());
		logQueue.add(systemLogModel);
	}

	/**
	 * 牌局记录
	 * 
	 * @param game_id
	 *            游戏id
	 * @param brand_id
	 *            牌局id
	 * @param brand_child_id
	 *            牌局子id列表
	 * @param msg
	 *            详细信息
	 * @param v1
	 *            扩展值
	 * @param v2
	 *            扩展值
	 * @param matchId
	 * @param isSys
	 *            是否金币场
	 */
	public BrandLogModel parentBrand(int game_id, Long brand_id, String brand_child_id, String msg, Long v1, Long v2, String room_id,
			Long create_account_id, RoomClubInfo clubInfo, int cost_dou, String name, int matchId, boolean isSys, Date createDate,int createType) {
		BrandLogModel brandLogModel = new BrandLogModel();
		brandLogModel.setCreate_time(createDate);
		brandLogModel.setGame_id(game_id);
		brandLogModel.setLogic_id(SystemConfig.logic_index);
		brandLogModel.setLog_type(ELogType.parentBrand.getId());
		brandLogModel.setBrand_id(brand_id);
		brandLogModel.setBrand_child_id(brand_child_id);
		brandLogModel.setMsg(msg);
		brandLogModel.setV1(v1);
		brandLogModel.setV2(v2);
		brandLogModel.setClub_id(clubInfo.clubId);
		brandLogModel.setRuleId(clubInfo.ruleId);
		brandLogModel.setV3(room_id);// 房间号
		brandLogModel.setLocal_ip(SystemConfig.localip);
		brandLogModel.setCreate_account_id(create_account_id);
		brandLogModel.setGold_count((long) cost_dou);
		brandLogModel.setName(name);
		brandLogModel.setMatch_id(matchId);
		brandLogModel.setExclusiveGold(clubInfo.exclusive);
		brandLogModel.setCreateType(createType);
		brandLogModel.setClubMatchId(clubInfo.matchId);
		// 金币场不入库
		if (!isSys) {
			logQueue.add(brandLogModel);
		}
		return brandLogModel;
	}

	/**
	 * 更新大牌局
	 * 
	 * @param brandLogModel
	 */
	public void updateParenBrand(BrandLogModel brandLogModel) {
		MongoDBUpdateThreadPool.getInstance().addTask(brandLogModel);
	}

	/**
	 * 牌局记录
	 * 
	 * @param game_id
	 *            游戏id
	 * @param brand_id
	 *            牌局id
	 * @param msg
	 * @param v1
	 * @param v2
	 */
	public void childBrand(int game_id, Long brand_id, Long brand_parent_id, String msg, Long v1, Long v2, byte video_record[], String room_id,
			String beginArray, Long create_account_id) {
		BrandChildLogModel brandLogModel = new BrandChildLogModel();
		brandLogModel.setCreate_time(new Date());
		brandLogModel.setGame_id(game_id);
		brandLogModel.setLogic_id(SystemConfig.logic_index);
		brandLogModel.setLog_type(ELogType.childBrand.getId());
		brandLogModel.setBrand_id(brand_id);
		brandLogModel.setBrand_parent_id(brand_parent_id);
		brandLogModel.setMsg(msg);
		brandLogModel.setV1(v1);
		brandLogModel.setV2(v2);
		brandLogModel.setV3(room_id);
		brandLogModel.setLocal_ip(SystemConfig.localip);
		brandLogModel.setVideo_record(video_record);
		brandLogModel.setBeginArray(beginArray);
		brandLogModel.setCreate_account_id(create_account_id);
		logQueue.add(brandLogModel);
	}

	/**
	 * 玩家牌局索引
	 */
	public BrandLogModel accountBrand(int game_id, long account_id, long brand_id, long create_account_id, int club_id,int createType,RoomClubInfo clubInfo) {
		BrandLogModel brandLogModel = new BrandLogModel();
		brandLogModel.setCreate_time(new Date());
		brandLogModel.setAccount_id(account_id);
		brandLogModel.setGame_id(game_id);
		brandLogModel.setLogic_id(SystemConfig.logic_index);
		brandLogModel.setLog_type(ELogType.accountBrand.getId());
		brandLogModel.setBrand_id(brand_id);
		brandLogModel.setLocal_ip(SystemConfig.localip);
		brandLogModel.setClub_id(club_id);
		brandLogModel.setCreate_account_id(create_account_id);
		brandLogModel.setCreateType(createType);
		brandLogModel.setClubMatchId(clubInfo.matchId);
		logQueue.add(brandLogModel);
		return brandLogModel;
	}

	// 战绩入库
	public BrandResultModel accountBrandResult(int game_id, String gameName, long accountId, int clubId, String groupId, String details, long gold,
			int openType, int roomId, String winner, int gameRound, long winnerId) {
		BrandResultModel brandResultModel = new BrandResultModel();
		brandResultModel.setAccountId(accountId);
		brandResultModel.setClubId(clubId);
		brandResultModel.setCreate_time(new Date());
		brandResultModel.setDetails(details);
		brandResultModel.setGameId(game_id);
		brandResultModel.setGameName(gameName);
		brandResultModel.setGold(gold);
		brandResultModel.setGroupId(groupId);
		brandResultModel.setOpenType(openType);
		brandResultModel.setRoomId(roomId);
		brandResultModel.setWinner(winner);
		brandResultModel.setWinnerId(winnerId);
		brandResultModel.setGameRound(gameRound);
		logQueue.add(brandResultModel);
		return brandResultModel;
	}

	public void red_package_active_log(long account_id, int active_id, int active_type, int money, String nick_name, int room_id) {
		RedPackageModel redPackageModel = new RedPackageModel();
		redPackageModel.setCreate_time(new Date());
		redPackageModel.setAccount_id(account_id);
		redPackageModel.setActive_id(active_id);
		redPackageModel.setActive_type(active_type);
		redPackageModel.setMoney(money);
		redPackageModel.setNick_name(nick_name);
		redPackageModel.setRoom_id(room_id);
		logQueue.add(redPackageModel);
	}

	public void all_red_package_active_log(List<RedPackageModel> list) {
		logQueue.addAll(list);
	}

	@Override
	protected void startService() {
		mogoDBTimer = new MogoDBTimer();
		timer.schedule(mogoDBTimer, 1000, 1000);
	}

	@Override
	public MonitorEvent montior() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onEvent(Event<SortedMap<String, String>> event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void sessionCreate(Session session) {
		// TODO Auto-generated method stub

	}

	@Override
	public void sessionFree(Session session) {

	}

	@Override
	public void dbUpdate(int _userID) {
	}

	public LinkedBlockingQueue<Object> getLogQueue() {
		return logQueue;
	}

	public MogoDBTimer getMogoDBTimer() {
		return mogoDBTimer;
	}

}
