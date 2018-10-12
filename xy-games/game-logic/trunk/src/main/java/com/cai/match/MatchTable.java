package com.cai.match;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.cai.common.constant.ActivityMissionTypeEnum;
import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MatchCmd;
import com.cai.common.constant.RMICmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.constant.S2SCmd;
import com.cai.common.core.ResultCode;
import com.cai.common.define.EMoneyOperateType;
import com.cai.common.domain.AccountMatchInfoRedis;
import com.cai.common.domain.MatchPlayerLogModel;
import com.cai.common.domain.MatchRoundModel;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerViewVO;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.json.AreaLimitJsonModel;
import com.cai.common.domain.json.MatchBaseScoreJsonModel;
import com.cai.common.domain.json.MatchPrizeDetailModel.MatchPrizeRankModel;
import com.cai.common.domain.json.RsShiftDetailJsonModel;
import com.cai.common.domain.json.UpgradeDetailJsonModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.type.MatchType;
import com.cai.common.type.SystemType;
import com.cai.common.util.DescParams;
import com.cai.common.util.MyDateUtil;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.RoomComonUtil;
import com.cai.common.util.SpringService;
import com.cai.common.util.TimeUtil;
import com.cai.core.SystemConfig;
import com.cai.dictionary.MatchDict;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.dictionary.SysParamServerDict;
import com.cai.domain.Session;
import com.cai.game.AbstractRoom;
import com.cai.handler.LogicRoomHandler;
import com.cai.manager.MatchManager;
import com.cai.manager.MatchTableManager;
import com.cai.service.FoundationService;
import com.cai.service.MatchTableService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;
import com.cai.service.RobotService;
import com.cai.service.RobotService.RobotRandom;
import com.cai.service.SessionServiceImpl;
import com.cai.util.MatchPBButils;
import com.cai.util.MatchRoomUtils;
import com.cai.util.RedisRoomUtil;
import com.cai.util.SystemRoomUtil;
import protobuf.clazz.BaseS2S.SendToClientsProto2;
import protobuf.clazz.match.MatchClientRsp.MatchBaseStartResponse;
import protobuf.clazz.match.MatchClientRsp.MatchConnectResponse;
import protobuf.clazz.match.MatchClientRsp.MatchEmpty;
import protobuf.clazz.match.MatchClientRsp.MatchEnterResponse;
import protobuf.clazz.match.MatchClientRsp.MatchGameStartResponse;
import protobuf.clazz.match.MatchClientRsp.MatchInfoResponse;
import protobuf.clazz.match.MatchClientRsp.MatchLeaveResponse;
import protobuf.clazz.match.MatchClientRsp.MatchOverResponse;
import protobuf.clazz.match.MatchClientRsp.MatchProgressInfoProto;
import protobuf.clazz.match.MatchClientRsp.MatchRankInfoProto;
import protobuf.clazz.match.MatchClientRsp.MatchRankMsg;
import protobuf.clazz.match.MatchClientRsp.MatchRankResponse;
import protobuf.clazz.match.MatchClientRsp.MatchRoundOverResponse;
import protobuf.clazz.match.MatchClientRsp.MatchScoreInfoResponse;
import protobuf.clazz.match.MatchClientRsp.MatchTopOverRankMsg;
import protobuf.clazz.match.MatchClientRsp.MatchWinnerInfoMsg;
import protobuf.clazz.match.MatchClientRsp.MatchWinnerTableResponse;
import protobuf.clazz.match.MatchClientRsp.ResponseTopRoundRecord;
import protobuf.clazz.match.MatchClientRsp.TopRoundMsg;
import protobuf.clazz.match.MatchRsp.MatchClientRequestWrap;
import protobuf.clazz.match.MatchRsp.MatchS2SCmd;
import protobuf.clazz.match.MatchRsp.MatchS2SRequest;
import protobuf.clazz.match.MatchRsp.MatchServerOutProto;
import protobuf.clazz.match.MatchRsp.MatchServerOverProto;
import protobuf.clazz.match.MatchRsp.MatchServerStartProto;
import protobuf.clazz.match.MatchRsp.MatchServerTopOverProto;

public class MatchTable {

	private static final Logger logger = LoggerFactory.getLogger(MatchTable.class);
	public final static int START_DELAY = 5000;
	private final static int DEFAULT_ROUND_TIME = 15;
	private final static int MAX_RATE = 100;
	private MatchSortComparator comparator; //排行比较器

	public final int id;
	public final int matchId;
	private boolean isTop; // 是否冲榜赛
	private List<MatchPlayerAdmin> administrators;

	private boolean isTimeout = false;

	private final Map<Long, MatchPlayer> matchPlayerMap;
	private List<MatchPlayer> allocationPlayerList; //配桌列表信息
	private Map<Integer, List<MatchPlayer>> topOverMap = new HashMap<>();

	protected int playerCount;
	private int robotNum;

	private RobotRandom random;

	private final Map<Integer, AbstractRoom> rooms;
	private final Map<Integer, AbstractRoom> roundRooms;
	private final Map<Integer, AbstractRoom> roundStartRooms;

	private IMatchProgress matchProgress = null;

	final Date startTime;

	int curProgress = -1;

	// 比赛进度
	final List<MatchProgressInfo> progresses;

	// 当前打立出局的配置
	private MatchBaseScoreJsonModel outBase;

	// 玩家的初始分数
	private int initScore; 
	private boolean isInitScore = false; //是否初始化玩家分数

	// 下一次检测时间
	private long nextCheckRoomTime;
	
	private int addtionCount; // 附加赛次数
	private int sendEnterNum; //通知进入次数
	private volatile int status; //比赛桌子状态
	private volatile int matchOverStatus; //匹配时桌子解散状态
	private Future<?> overFuture;
	private Map<Integer,Future<?>> roomFutureMap;
	
	private MatchServerStartProto matchStartMsg;
	private String cheatNickname = "";
	private String cheatHeadIcon = "";

	public MatchTable(int id, int matchId, List<Long> adminIds, boolean isTop, MatchSortComparator comparator) {
		this.id = id;
		this.matchId = matchId;
		this.isTop = isTop;
		if(adminIds != null && adminIds.size() > 0){
			this.administrators = new ArrayList<>();
			adminIds.forEach((adminId) -> {
				administrators.add(new MatchPlayerAdmin(adminId));
			});
		}
		this.matchPlayerMap = new HashMap<>();
		this.rooms = new HashMap<>();
		this.roundStartRooms = new HashMap<>();
		this.roundRooms = new HashMap<>();
		this.roomFutureMap = new HashMap<>();
		this.startTime = new Date(System.currentTimeMillis() + START_DELAY);
		this.progresses = new ArrayList<>();
		this.initScore = getMatchModel().getMatchChoiceModel().getBase_score();
		this.comparator = comparator;
		this.status = MatchType.TABLE_NO_START;
		if(isHaveAdmin()){
			this.status = MatchType.TABLE_NO_START_ADMIN;
		}
		if(isCheat()){
			SysParamModel sModel = SysParamServerDict.getInstance().getSysParam(
					SystemType.MATCH_CHEAT, SystemType.MATCH_CHEAT);
			if(sModel != null){
				if(sModel.getStr1() != null){
					this.cheatNickname = sModel.getStr1();
				}
				if(sModel.getStr2() != null){
					this.cheatHeadIcon = sModel.getStr2();
				}
			}
		}
	}
	
	public List<MatchPlayer> getAllocationPlayerList() {
		return allocationPlayerList;
	}

	public void setAllocationPlayerList(List<MatchPlayer> allocationPlayerList) {
		this.allocationPlayerList = allocationPlayerList;
		
		List<Long> newAccountList = new ArrayList<>();
		newAccountList.addAll(matchStartMsg.getAccountIdsList());
		newAccountList.addAll(getAdminIds());
		SendToClientsProto2.Builder broadcast = SendToClientsProto2.newBuilder();
		broadcast.addAllAccountId(newAccountList);
		broadcast.setRsp(PBUtil.toS2CCommonRsp(S2CCmd.MATCH, 
				MatchPBButils.getMatchResponse(MatchCmd.MATCH_WINNER_TABLE_LIST, getWinnerTableResponse(0))).build());
		SessionServiceImpl.getInstance().sendMsgToProxy(PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLENT_BATCH_SAME_PKG, broadcast));
	}
	
	public void requestTopRoundRecord(MatchPlayer player, Session session){
		ResponseTopRoundRecord.Builder resp = ResponseTopRoundRecord.newBuilder();
		resp.setMatchId(matchId);
		List<MatchRoundRecord> records = player.getRoundRecords();
		List<TopRoundMsg> list = new ArrayList<>();
		records.forEach((record) -> {
			TopRoundMsg.Builder msg = TopRoundMsg.newBuilder();
			msg.setRound(record.getRound());
			msg.setIndex(record.getIndex());
			msg.setScore(record.getScore());
			msg.setTimes(player.getTopTimes());
			
			list.add(msg.build());
		});
		resp.setCurScore((int)player.getCurScore());
		resp.addAllRoundMsgs(list);
		
		session.send(PBUtil.toS_S2CRequet(player.getAccount_id(), S2CCmd.MATCH, 
				MatchPBButils.getMatchResponse(MatchCmd.MATCH_TOP_ROUND_RECORD, resp)).build());
	}
	
	public void requestWinnerTableByAdmin(long adminId, Session session){
		if(!isAdminId(adminId)){
			return;
		}
		session.send(PBUtil.toS_S2CRequet(adminId, S2CCmd.MATCH, 
				MatchPBButils.getMatchResponse(MatchCmd.MATCH_WINNER_TABLE_LIST, getWinnerTableResponse(0))).build());
	}
	
	public void requestWinnerTable(long accountId, Session session) {
		if(!isHaveAdmin()){
			return;
		}
		session.send(PBUtil.toS_S2CRequet(accountId, S2CCmd.MATCH, 
				MatchPBButils.getMatchResponse(MatchCmd.MATCH_WINNER_TABLE_LIST, getWinnerTableResponse(0))).build());
	}
	
	private MatchWinnerTableResponse.Builder getWinnerTableResponse(int opType){
		MatchWinnerTableResponse.Builder tableResp = MatchWinnerTableResponse.newBuilder();
		tableResp.setMatchId(matchId);
		tableResp.setOpType(opType);
		DescParams params = getMatchModel().getRuleParam();
		int maxCount = RoomComonUtil.getMaxNumber(params);
		tableResp.setTableNum(maxCount);
		if(this.allocationPlayerList == null || this.allocationPlayerList.size() <= 0){
			List<MatchWinnerInfoMsg> msgList = new ArrayList<>();
			tableResp.addAllInfos(msgList);
			return tableResp;
		}
		
		for(MatchPlayer mPlayer : allocationPlayerList){
			tableResp.addInfos(getWinnerInfoMsg(mPlayer));
		}
		return tableResp;
	}
	
	public MatchWinnerInfoMsg getWinnerInfoMsg(MatchPlayer mPlayer){
		MatchWinnerInfoMsg.Builder tableMsg = MatchWinnerInfoMsg.newBuilder();
		tableMsg.setIndex(mPlayer.getAllocationId());
		tableMsg.setAccountId(mPlayer.getAccount_id());
		tableMsg.setHeadUrl(mPlayer.getAccount_icon());
		tableMsg.setNickname(mPlayer.getNick_name());
		boolean isReady = mPlayer.isEnter();
		if(mPlayer.isLeave()){
			isReady = true;
		}
		tableMsg.setIsReady(isReady);
		
		return tableMsg.build();
	}

	public Map<Long, MatchPlayer> getMatchPlayerMap() {
		return matchPlayerMap;
	}

	private List<MatchPlayer> getMatchPlayers(){
		List<MatchPlayer> list = new ArrayList<>();
		list.addAll(getMatchPlayerMap().values());
		Collections.sort(list, comparator);
		return list;
	}
	
	public List<Long> getMatchPlayerIds(){
		List<Long> list = new ArrayList<>();
		for(MatchPlayer mPlayer : getMatchPlayerMap().values()){
			if(mPlayer.isRobot()){
				continue;
			}
			list.add(mPlayer.getAccount_id());
		}
		if(isHaveAdmin()){
			list.addAll(getAdminIds());
		}
		return list;
	}
	
	public void updatePlayerIndex(List<MatchPlayer> waitPlayerList){
		Collections.sort(waitPlayerList, comparator);
		MatchPlayer player = null;
		for(int index = 0;index < waitPlayerList.size();index++){
			player = waitPlayerList.get(index);
			player.setCurIndex(index + 1);
		}
	}
	
	public int getMatchId(){
		return matchId;
	}
	
	public Map<Integer, AbstractRoom> getRooms() {
		return rooms;
	}
	
	public int getStatus(){
		return status;
	}

	public MatchRoundModel getMatchModel(){
		MatchRoundModel model = MatchDict.getInstance().getMatchModel(matchId);
		return model;
	}

	public RobotRandom getRandom() {
		if (random == null) {
			random = RobotService.getInstance().getRobotRandom();
		}
		return random;
	}
	
	public boolean isAdminId(long accountId){
		if(administrators == null){
			return false;
		}
		for(MatchPlayerAdmin aPlayer : administrators){
			if(aPlayer.getAccount_id() == accountId){
				return true;
			}
		}
		return false;
	}
	
	public boolean isHaveAdmin(){
		if(administrators != null && administrators.size() > 0){
			return true;
		}
		return false;
	}
	
	public List<Long> getAdminIds(){
		if(administrators == null){
			return null;
		}
		List<Long> list = new ArrayList<>();
		administrators.forEach((administrator) -> {
			list.add(administrator.getAccount_id());
		});
		return list;
	}

	public List<MatchPlayerAdmin> getAdministrators() {
		return administrators;
	}
	
	public MatchPlayerAdmin getAdministratorById(long accountId){
		if(administrators == null){
			return null;
		}
		for(MatchPlayerAdmin ad : administrators){
			if(ad.getAccount_id() == accountId){
				return ad;
			}
		}
		return null;
	}

	public void update(long ctime) {
		if (isNoStart()) {
			if(isHaveAdmin()){
				int pType = getCurProgressType(); 
				if(pType == MatchType.MATCH_FORMAT_FIX){
					return;
				}
				this.status = MatchType.TABLE_NO_START;
			}
			if (ctime > startTime.getTime()) {
				onStart(ctime);
				MatchTableManager.INSTANCE().sendWinnerRefreshResp(this);
			}
			return;
		}

		if (curProgress == -1) {
			return;
		}
		
		if(isPuse()){
			return;
		}

		if (nextCheckRoomTime <= ctime) {
			nextCheckRoomTime += 10000;
			// TODO 需要放到比赛配置
			rooms.forEach((id, room) -> {
				try {
					room.getRoomLock().lock();
					int roundTime = DEFAULT_ROUND_TIME;
					int mRoundTime = getMatchModel().getMatchChoiceModel().getRound_time();
					if(mRoundTime > 0){
						roundTime = mRoundTime;
					}
					long overTime = (long) (room.getCreate_time() * 1000 + getProgressInfo().getRound_num() * roundTime * TimeUtil.MINUTE);
					if (PlayerServiceImpl.getInstance().getRoomMap().containsKey(room.getRoom_id()) && overTime <= ctime) {
						logger.warn("force break room matchId:{} id:{} roomId:{} !!",matchId,id,room.getRoom_id());
						room.force_account();
					}
				} finally {
					room.getRoomLock().unlock();
				}
			});

		}

		if (matchProgress != null) {
			matchProgress.onUpdate(ctime, this);
		}
	}

	public String gameStart(MatchServerStartProto matchStart) {
		StringBuffer sb = new StringBuffer();
		List<Long> accountList = matchStart.getAccountIdsList();
		List<Integer> timesList = matchStart.getAccountTimesList();

		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		List<PlayerViewVO> playerList = centerRMIServer.rmiInvoke(RMICmd.MATCH_PLAYER_INFO, accountList);
		PlayerViewVO pVo = null;
		for(int i=0;i<playerList.size();i++){
			pVo = playerList.get(i);
			MatchPlayer player = createMatchPlayer(pVo, id);
			player.setTopTimes(timesList.get(i));
			getMatchPlayerMap().put(player.getAccount_id(), player);
			sb.append(pVo.getAccountId()).append(",");
		}
		playerList.forEach((account) -> {
		});
		
		int gamerNum = matchStart.getGamerNum();
		// 补齐机器人
		if (getMatchPlayerSize() < gamerNum) {
			List<MatchPlayer> robotList = getRandom().getRandomMatchPlayers(gamerNum - getMatchPlayerSize(), id);
			robotNum += robotList.size();
			robotList.forEach((player) -> {
				if(isCheat()){
					player.setCheatNickname(player.getNick_name());
					player.setCheatHeadIcon(player.getAccount_icon());
					player.setNick_name(cheatNickname);
					player.setAccount_icon(cheatHeadIcon);
				}
				player.setMatchId(matchId);
				getMatchPlayerMap().put(player.getAccount_id(), player);
			});
		}

		DescParams params = getMatchModel().getRuleParam();
		int maxPlayerCount = RoomComonUtil.getMaxNumber(params);
		int mode = getMatchPlayerSize() % maxPlayerCount; // 轮空的

		// 补齐玩家人数
		if (mode > 0) {
			List<MatchPlayer> robotList = getRandom().getRandomMatchPlayers(maxPlayerCount - mode, id);
			robotNum += robotList.size();
			robotList.forEach((player) -> {
				if(isCheat()){
					player.setCheatNickname(player.getNick_name());
					player.setCheatHeadIcon(player.getAccount_icon());
					player.setNick_name(cheatNickname);
					player.setAccount_icon(cheatHeadIcon);
				}
				player.setMatchId(matchId);
				getMatchPlayerMap().put(player.getAccount_id(), player);
			});
		}
		
		playerCount = getMatchPlayerSize();
		MatchGameStartResponse.Builder b = MatchGameStartResponse.newBuilder();
		b.setId(id);
		b.setMatchId(matchId);

		List<Long> newAccountList = new ArrayList<>();
		newAccountList.addAll(accountList);
		//管理员
		if(isHaveAdmin()){
			for(long adminId : getAdminIds()){
				MatchManager.INSTANCE().startAccountMatchInfo(adminId, matchId, id, SystemConfig.logic_index, MatchType.WINNER_ADMIN_MANAGER);
			}
			newAccountList.addAll(getAdminIds());
		}
		
		SendToClientsProto2.Builder broadcast = SendToClientsProto2.newBuilder();
		broadcast.addAllAccountId(accountList);
		broadcast.setRsp(PBUtil.toS2CCommonRsp(S2CCmd.MATCH, MatchPBButils.getMatchResponse(MatchCmd.S2C_MATCH_START, b)).build());

		try {
			int gameID = SysGameTypeDict.getInstance().getGameIDByTypeIndex(params._game_type_index);
			for(MatchPlayer mPlayer : getMatchPlayerMap().values()){
				if(!mPlayer.isRobot()) {
					int applyType = MatchManager.INSTANCE().startAccountMatchInfo(mPlayer.getAccount_id(), matchId, id, SystemConfig.logic_index, MatchType.APPLY);
					mPlayer.setApplyType(applyType);
					//参与比赛的人员都完成一次任务,非机器人
					FoundationService.getInstance().sendActivityMissionProcess(mPlayer.getAccount_id(),
							ActivityMissionTypeEnum.JOIN_MATCH, 1, 1);
					//参加XX游戏比赛XX次
					FoundationService.getInstance().sendActivityMissionProcess(mPlayer.getAccount_id(),
							ActivityMissionTypeEnum.JOIN_TARGET_GAME_FORMATCH, gameID, 1);
					//参加XX比赛XX次
					FoundationService.getInstance().sendActivityMissionProcess(mPlayer.getAccount_id(),
							ActivityMissionTypeEnum.JOIN_TARGET_MATCH, this.matchId, 1);
				}
			}
		} catch (Exception e) {
			logger.error("任务处理器执行出错",e);
		}

		SessionServiceImpl.getInstance().sendMsgToProxy(PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLENT_BATCH_SAME_PKG, broadcast));
		sb.append(" playerNum:").append(getMatchPlayerSize());
		
		initProgress();
		return sb.toString();
	}

	// 初始化比赛进度
	private void initProgress() {

		int riseCount = getMatchPlayerSize(); // 晋级人数
		
		int round = 1;
		int outType = getMatchModel().getMatchFormatModel().getOut_right_type();
		// 打立出局
		if (outType == 1) {
			AreaLimitJsonModel areaLimit = getMatchModel().getMatchFormatModel().getOutDetailJsonModel().getConfig(playerCount);
			riseCount = areaLimit.getGoNextRoundPerson();
			MatchProgressInfo info = new MatchProgressInfo();
			info.setType(1);
			info.setCurRound(0);
			info.setRound_num(1);
			info.setStartCount(playerCount);
			info.setRiseCount(riseCount);
			info.setStopCount(areaLimit.getNum());
			info.setNextBili(areaLimit.getNextBili());
			progresses.add(info);
		} else if (outType == 2) {
			
			// 瑞士移位
			List<RsShiftDetailJsonModel> rsDetail = getMatchModel().getMatchFormatModel().getRsDetailJsonModelList();
			for (RsShiftDetailJsonModel rs : rsDetail) {
				MatchProgressInfo info = new MatchProgressInfo();
				info.setStartCount(riseCount);
				AreaLimitJsonModel areaLimit = rs.getConfig(playerCount);
				if (areaLimit != null) {
					riseCount = areaLimit.getGoNextRoundPerson();
					info.setRiseCount(riseCount);
				}

				info.setType(3);
				info.setCurRound(round++);
				
				info.setRound(rs.getRound());
				info.setRound_num(rs.getRound_num());
				info.setBase_num(rs.getBase_num());
				info.setBase_score(rs.getBase_score());
				info.setBase_times(rs.getBase_times());
				info.setNextBili(rs.getNextBili());
				
				progresses.add(info);
			}
		}

		List<UpgradeDetailJsonModel> upgradeDetail = getMatchModel().getMatchFormatModel().getUpgradeDetailJsonModelList();
		for (UpgradeDetailJsonModel upgrade : upgradeDetail) {
			MatchProgressInfo info = new MatchProgressInfo();
			info.setStartCount(riseCount);
			AreaLimitJsonModel areaLimit = upgrade.getConfig(playerCount);
			if (areaLimit != null) {
				riseCount = areaLimit.getGoNextRoundPerson();
				info.setRiseCount(riseCount);
			}

			info.setType(2);
			info.setCurRound(round++);
			
			info.setRound(upgrade.getRound());
			info.setRound_num(upgrade.getRound_num());
			info.setBase_num(upgrade.getBase_num());
			info.setBase_score(upgrade.getBase_score());
			info.setBase_times(upgrade.getBase_times());
			info.setNextBili(upgrade.getNextBili());
			info.setNextScore(upgrade.getNextScore());
			
			progresses.add(info);
		}

		nextProgress();
	}
	
	public boolean isPuse(){ //是否暂停
		if(getStatus() == MatchType.TABLE_PAUSE){
			return true;
		}
		return false;
	}
	
	public boolean isGaming(){ //是否进行中
		if(getStatus() == MatchType.TABLE_START){
			return true;
		}
		return false;
	}
	
	public boolean isOver(){ //是否结束
		if(getStatus() == MatchType.TABLE_OVER){
			return true;
		}
		return false;
	}
	
	private boolean isStart(){
		if(getStatus() == MatchType.TABLE_START || getStatus() == MatchType.TABLE_PAUSE){
			return true;
		}
		return false;
	}
	
	public boolean isNoStart(){
		if(getStatus() == MatchType.TABLE_NO_START || getStatus() == MatchType.TABLE_NO_START_ADMIN){
			return true;
		}
		return false;
	}
	
	public void onPause(){
		this.status = MatchType.TABLE_PAUSE;
		int roomSize = rooms.size();
		if(roomSize > 0){
			AbstractRoom table = null;
			List<Player> allPlayer = null;
			for(int roomId : rooms.keySet()){
				table = rooms.get(roomId);
				table.isPauseGame = true;
				try{
					allPlayer = table.getAllPlayers();
					for(Player player : allPlayer){
						player.pauseAi();
					}
					table.handler_pause_room();
				}catch (Exception e) {
					logger.error("onPause->rooms handel error roomId="+roomId,e);
				}
			}
		}
		broadcastMatchInfo();
		logger.info("onPause->matchId:{} id:{} roomSize:{} roundRooms:{} roundStartRooms:{} status:{} !!",
				matchId,id,roomSize,roundRooms.size(),roundStartRooms.size(),status);
	}
	
	public void onContinue(){
		int roomSize = rooms.size();
		this.status = MatchType.TABLE_START;
		logger.info("onContinue->matchId:{} id:{} roomSize:{} matchStatus:{} roundRooms:{} roundStartRooms:{} status:{} !!",
				matchId,id,roomSize,matchOverStatus,roundRooms.size(),roundStartRooms.size(),status);
		AbstractRoom table = null;
		if(roomSize <= 0){
			switch (matchOverStatus) {
			case MatchType.TABLE_MATCH_LAST_OVER:
				onChangeProgress();
				break;
			case MatchType.TABLE_MATCH_NEXT_START:
				onToMatching();
				break;
			}
			
		}else{
			List<Player> allPlayer = null;
			for(int roomId : rooms.keySet()){
				table = rooms.get(roomId);
				table.isPauseGame = false;
				try{
					allPlayer = table.getAllPlayers();
					for(Player player : allPlayer){
						table.handler_reconnect_room(player);
						player.continueAi();
					}
					table.handler_continue_room();
				}catch (Exception e) {
					logger.error("onContinue->rooms handel error roomId="+roomId,e);
				}
			}
			
			for(int roomId : roundRooms.keySet()){
				table = roundRooms.remove(roomId);
				onRoundOver(table);
			}
			
			for(AbstractRoom mRoom : getRoundStartRoomList()){
				onRoundOverStart(mRoom);
			}
		}
		broadcastMatchInfo();
	}
	
	private List<AbstractRoom> getRoundStartRoomList(){
		List<AbstractRoom> list = new ArrayList<>();
		list.addAll(roundStartRooms.values());
		
		roundStartRooms.clear();
		return list;
	}
	
	private void initPlayerScore(){
		if(!isInitScore){
			isInitScore = true;
			getMatchPlayerMap().forEach((accountId,player) -> {
				player.setCurScore(initScore);
			});
		}
	}
	
	public void onStart(long ctime){
		List<MatchPlayer> playerList = getMatchPlayers();
		if(isTop){
			Collections.sort(playerList, (player1,player2) -> player2.getTopTimes() - player1.getTopTimes());
		}else{
			Collections.shuffle(playerList);
		}

		onStart(ctime, playerList);
	}

	public void onStart(long ctime, List<MatchPlayer> curPlayers) {
		initPlayerScore();
		this.status = MatchType.TABLE_START;
		DescParams params = getMatchModel().getRuleParam();
		int playerCount = RoomComonUtil.getMaxNumber(params);
		int roomCount = curPlayers.size() / playerCount;
		if (roomCount == 0) {
			allOver();
			return;
		}
		MatchBaseStartResponse.Builder b = MatchBaseStartResponse.newBuilder();
		b.setType(progresses.get(curProgress).getType());
		b.setIsFirstStart(curProgress == 0);

		b.setBase(outBase.getBase());
		b.setBaseScore(outBase.getBaseScore());
		b.setTimes(outBase.getTimes());
		b.setOutScore(outBase.getOutScore());
		b.setMatchId(matchId);

		curPlayers.forEach(player -> {
			if(!player.isRobot() && !player.isLeave() && player.isEnter()){
				b.setMatchRound(matchProgress.addMatchTypeRound(player, this, progresses.get(curProgress)));
				PlayerServiceImpl.getInstance().sendMatchRsp(player, MatchPBButils.getMatchResponse(MatchCmd.S2C_MATCH_BASE_START, b));
			}
		});

		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		int[] roomIds = centerRMIServer.randomRoomIds(getMatchModel().getMatchRuleModel().getGame_id(), roomCount);

		for (int i = 0; i < roomIds.length; i++) {
			// 打立出局是打一局，定局賽是打三局
			int round = getProgressInfo().getRound_num();
			if(addtionCount > 0){
				round = 1;
			}
			handler_player_create_room(roomIds[i], params, curPlayers.subList(i * playerCount, (i + 1) * playerCount), round);
		}
	}
	
	public void gameUpdate(AbstractRoom room){
		if(room == null){
			return;
		}
		
		int playerNum = room.getPlayerCount();
		for (int i = 0; i < playerNum; i++) {
			Player player = room.get_players()[i];
			if (player == null) {
				continue;
			}

			MatchPlayer matchPlayer = (MatchPlayer) player;
			if(isSend(matchPlayer)){
				PlayerServiceImpl.getInstance().sendMatchRsp(matchPlayer, 
						MatchPBButils.getMatchResponse(MatchCmd.MATCH_SCORE_INFOS, getScoreInfoResponse(room,i)));
			}
		}
	}
	
	private MatchScoreInfoResponse.Builder getScoreInfoResponse(AbstractRoom room,int seatIndex){
		MatchScoreInfoResponse.Builder resp = MatchScoreInfoResponse.newBuilder();
		resp.setBase(room.matchBase.getBase());
		resp.setBaseScore(room.matchBase.getBaseScore());
		resp.setOutScore(room.matchBase.getOutScore());
		resp.setTimes(room.getTimes(seatIndex));
		resp.setMatchId(matchId);
		
		return resp;
	}

	public void gameOver(AbstractRoom room) {
		if(getStatus() == MatchType.TABLE_OVER){
			logger.error("gameOver->>> match over matchId:{} id:{} roomId:{} !!",matchId,id,room.getRoom_id());
			return;
		}
		
		if (rooms.remove(room.getRoom_id()) == null) {
			return;
		}
		
		gameOver0(room);
	}

	private void gameOver0(AbstractRoom room) {
		onRoundOverSettlement(room);

		long ctime = System.currentTimeMillis();

		List<MatchPlayer> mPlayerList = new ArrayList<>();
		for (int i = 0; i < room._player_result.game_score.length; i++) {
			Player player = room.get_players()[i];
			if (player == null) {
				continue;
			}
			MatchPlayer matchPlayer = (MatchPlayer) player;
			matchProgress.onGameOver(room, this, matchPlayer, ctime);
			matchPlayer.setAllocationId(0);
			mPlayerList.add(matchPlayer);
		}
		
		sort();
		logger.info("match room over matchId:{} id:{} roomId:{} roomSize:{} accountMsg:{} !!", 
				matchId, id, room.getRoom_id(), rooms.size(), printMatchMsg(mPlayerList));

		if (!matchProgress.isNeedChangeProgress(this)) {
			return;
		}
		
		// 进度发生变化
		if(isTop){
			topOverMap.put(room.getRoom_id(), mPlayerList);
			gameSettleByTop(room.getRoom_id());
			onChangeProgressByTop(room.getRoom_id());
			return;
		}
		
		onChangeProgress();
	}
	
	private void gameSettleByTop(int roomId){
		List<MatchPlayer> topPlayerList = topOverMap.get(roomId);
		if(topPlayerList == null){
			return;
		}
		
		MatchServerTopOverProto.Builder topOver = MatchServerTopOverProto.newBuilder();
		topOver.setMatchId(matchId);
		topOver.setId(id);
		topOver.setStartTime(matchStartMsg.getStartTime());
		for(MatchPlayer mPlayer : topPlayerList){
			if (mPlayer.isRobot()) {
				continue;
			}
			topOver.addAccountIds(mPlayer.getAccount_id());
			topOver.addWinScore((int)mPlayer.getCurScore());
			topOver.addIsNewAccounts(mPlayer.isNewAccount());
			topOver.addIsPayAccounts(mPlayer.isPayAccount());
			topOver.addTimes(mPlayer.getTopTimes());
			topOver.addWinNum(mPlayer.getWinNum());
			topOver.addSingleNum(mPlayer.getSingleNum());
		}
		
		SessionServiceImpl.getInstance().sendMatch(SystemConfig.match_id,
				PBUtil.toS2SRequet(S2SCmd.MATCH_SERVER,
						MatchS2SRequest.newBuilder().setCmd(MatchS2SCmd.S2S_MATCH_TOP_OVER).setMatchTopOver(topOver)).build());
		
	}
	
	private void gameOver0ByTop(List<MatchPlayer> topPlayerList){
		
		Collections.sort(topPlayerList, (player1,player2) -> (int)(player2.getCurScore() - player1.getCurScore()));
		List<Long> topList = new ArrayList<>();
		List<MatchPlayer> sendTopList = new ArrayList<>();
		List<MatchTopOverRankMsg> topRankMsgList = new ArrayList<>();

		MatchPlayer mPlayer = null;
		for(int i=0; i<topPlayerList.size();i++){
			mPlayer = topPlayerList.get(i);
			removeAccountMatchInfo(mPlayer, MatchType.REMOVE_TOP_OVER, false);
			
			MatchTopOverRankMsg.Builder topRankMsg = MatchTopOverRankMsg.newBuilder();
			topRankMsg.setAccountId(mPlayer.getAccount_id());
			topRankMsg.setRankId(i + 1);
			topRankMsg.setCurScore(mPlayer.getCurScore());
			topRankMsg.setTimes(mPlayer.getTopTimes());
			topRankMsg.setHeadUrl(mPlayer.getAccount_icon());
			topRankMsg.setNickname(mPlayer.getNick_name());
			if(isCheat()){
				topRankMsg.setHeadUrl(mPlayer.getCheatHeadIcon());
				topRankMsg.setNickname(mPlayer.getCheatNickname());
			}
			
			topRankMsgList.add(topRankMsg.build());
			
			if (mPlayer.isRobot()) {
				continue;
			}
			createLog(mPlayer, 0, id, null, MatchType.REMOVE_TOP_OVER,"冲榜赛比赛结束");
			
			sendTopList.add(mPlayer);
			if(mPlayer.isEnter()){
				PlayerServiceImpl.getInstance().getPlayerMap().remove(mPlayer.getAccount_id());
			}
		}

		sendTopList.forEach((player) -> {
			// 告诉客户端比赛结束了
			MatchOverResponse.Builder clientOver = MatchOverResponse.newBuilder();
			clientOver.setMatchId(matchId);
			clientOver.setMyRank(player.getCurRank());
			clientOver.setRankPrize(MatchTableManager.INSTANCE().encodeRankPrize(null));
			clientOver.setMatchType(getMatchModel().getMatchRuleModel().getMatch_type());
			clientOver.addAllTopRankMsgs(topRankMsgList);
			PlayerServiceImpl.getInstance().sendMatchRsp(player, MatchPBButils.getMatchResponse(MatchCmd.S2C_MATCH_OVER, clientOver));
		
			topList.add(player.getAccount_id());
			player.setVail(true);
			player.setLeave(true);
			player.setEnter(false);
			player.setChannel(null);
		});
		
		logger.info("match over top ===============>matchId:{},id:{} topList:{} !!",
				matchId, id, topList);
	}
	
	public void gameObserver(AbstractRoom room,long accountId,boolean isEnter){
		if(!isAdminId(accountId)){
			return;
		}
		MatchPlayerAdmin administrator = getAdministratorById(accountId);
		administrator.setObserver(isEnter);
		administrator.setObRoomId(0);
		MatchTableManager.INSTANCE().sendToAdministrator(accountId, this, MatchCmd.S2C_MATCH_INFO, encodeInfo());
		if(isEnter){
			administrator.setObRoomId(room.getRoom_id());
			MatchTableManager.INSTANCE().sendToAdministrator(accountId, this, MatchCmd.MATCH_SCORE_INFOS, getScoreInfoResponse(room,0));
		}
		logger.info("gameObserver->accountId:{} roomId:{} isEnter:{} !!",accountId, room.getRoom_id(), isEnter);
	}
	
	// 进度发生变化
	private void onChangeProgressByTop(int roomId){
		
		MatchTableService.getInstance().addScheduleTask(this, () -> {
			try {
				List<MatchPlayer> topPlayerList = topOverMap.get(roomId);
				if(topPlayerList != null){
					gameOver0ByTop(topPlayerList);
					topOverMap.remove(roomId);
				}
				if(topOverMap.size() <= 0 && getRoomSize() <= 0){
					onChangeProgress();
				}
			} catch (Exception e) {
				logger.error("onChangeProgressByTop->matchId:{} id:{} error:{} change progress error !!",
						matchId,id,e);
			}
		}, 7000);
	}
	
	// 进度发生变化
	private void onChangeProgress(){
		cancelOverFuture();
		matchOverStatus = MatchType.TABLE_MATCH_LAST_OVER;
		int delayTime = curProgress == (progresses.size() - 1) ? 3000 : 7000;
		if(isTop){
			delayTime = 1000;
		}
		
//		gameOver0ByTop(mPlayerList);
		overFuture = MatchTableService.getInstance().addScheduleTask(this, () -> {
			try {
				changeProgress();
			} catch (Exception e) {
				logger.error("gameOver->matchId:{} id:{} error:{} change progress error !!",
						matchId,id,e);
				allOver();
			}
		}, delayTime);
	}
	
	// 检查打立出局匹配
	void onCheckMatching(long ctime) {
		List<MatchPlayer> waitPlayer = new ArrayList<>();
		for(MatchPlayer player : getMatchPlayerMap().values()){
			if(player.getMyRoom() == null && player.getMatchingTime() <= ctime){
				waitPlayer.add(player);
			}
		}
		
		int size = waitPlayer.size();
		if (size > RoomComonUtil.getMaxNumber(getMatchModel().getRuleParam()) * 2) {
			MatchBaseScoreJsonModel newOutBase = getMatchModel().getMatchBaseScoreModel().getConfig(startTime.getTime());
			if(!isHaveAdmin()){
				newOutBase.setCommonBoomBase();
			}
			outBase = newOutBase;
			updatePlayerIndex(waitPlayer);
			String matchMsg = toMatching(waitPlayer, false);
			if(matchMsg.length() > 0){
				MatchTableManager.INSTANCE().sendWinnerRefreshResp(this);
				logger.info("Match score ->matchId:{} id:{} matchNum:{} matchMsg:{} !!",matchId,id,size,matchMsg);
			}
		}
	}
	
	private boolean isAddtionRound(){
		int addRound = getMatchModel().getMatchChoiceModel().getPlay_off_round();
		if(addRound > addtionCount){
			logger.info("Addtion Round1 -> matchId:{} id:{} addRound:{} addtionCount:{}  !!",
					matchId,id,addRound,addtionCount);
			return true;
		}
		if(getMatchPlayerSize() > 2){
			List<MatchPlayer> playerList = getMatchPlayers();
			MatchPlayer one = playerList.get(0);
			MatchPlayer two = playerList.get(1);
			float oneScore = one.getCurScore();
			float twoScore = two.getCurScore();
			if(oneScore == twoScore){
				logger.info("Addtion Round2 -> matchId:{} id:{} oneAccountId:{} oneScore:{} twoAccountId:{} twoScore:{} addtionCount:{} !!",
						matchId,id,one.getAccount_id(),oneScore,two.getAccount_id(),twoScore,addtionCount);
				return true;
			}
		}
		return false;
	}

	private void changeProgress() {
		if(isPuse()){
			return;
		}
		if (curProgress >= progresses.size() - 1) {
			if (!isTop && isAddtionRound()) {
				// 第一名第二名分数相同。打附加赛
				try {
					addtionCount += 1;
					List<MatchPlayer> playerList = sort();
					for (MatchPlayer matchPlayer : playerList) {
						if(!isSend(matchPlayer)){
							continue;
						}
						PlayerServiceImpl.getInstance().sendMatchRsp(matchPlayer,
								MatchPBButils.getMatchResponse(MatchCmd.S2C_MATCH_ADDTION, MatchEmpty.newBuilder().setMatchId(matchId)));
					}
					String matchMsg = toMatching(playerList, true);
					if(matchMsg.length() > 0){
						MatchTableManager.INSTANCE().sendWinnerRefreshResp(this);
						logger.info("Match addtion ->matchId:{} id:{} matchNum:{} matchMsg:{} !!",matchId,id,playerList.size(),matchMsg);
					}
				} catch (Exception e) {
					logger.error("changeProgress->matchId:{} id:{} error:{} matching error !!",
							matchId,id,e);
					allOver();
				}
				return;
			}

			allOver();
			return;
		}

		matchProgress.overProgress(this);
		nextProgress();

		int roomMaxCount = RoomComonUtil.getMaxNumber(getMatchModel().getRuleParam());
		int mode = getMatchPlayers().size() % roomMaxCount; // 轮空的

		// 补齐玩家人数 容错
		if (mode > 0) {
			List<MatchPlayer> robotList = getRandom().getRandomMatchPlayers(roomMaxCount - mode, id);
			robotList.forEach((player) -> {
				if(isCheat()){
					player.setCheatNickname(player.getNick_name());
					player.setCheatHeadIcon(player.getAccount_icon());
					player.setNick_name(cheatNickname);
					player.setAccount_icon(cheatHeadIcon);
				}
				player.setMatchId(matchId);
				getMatchPlayerMap().put(player.getAccount_id(), player);
			});
			logger.info("changeProgress -> matchId:{} id:{} mode:{} addRobotNum:{} !!",matchId,id,mode,robotList.size());
		}

		// 进度发生变化，3秒后匹配牌桌
		onToMatching();
	}
	
	// 进度发生变化，3秒后匹配牌桌
	private void onToMatching(){
		cancelOverFuture();
		matchOverStatus = MatchType.TABLE_MATCH_NEXT_START;
		overFuture = MatchTableService.getInstance().addScheduleTask(this, () -> {
			try {
				List<MatchPlayer> playerList = sort();
				String matchMsg = toMatching(playerList, true);
				if(matchMsg.length() > 0){
					logger.info("Match fix ->matchId:{} id:{} matchNum:{} matchMsg:{} !!",matchId,id,playerList.size(),matchMsg);
					MatchTableManager.INSTANCE().sendWinnerRefreshResp(this);
					matchOverStatus = MatchType.TABLE_MATCH_NEXT_OVER;
				}
			} catch (Exception e) {
				logger.error("changeProgress->matchId:{} id:{} error:{} matching error !!",
						matchId,id,e);
				allOver();
			}
		}, 3000);
	}

	// 按阶段匹配        isRankSort:是否排行分配
	private String toMatching(List<MatchPlayer> players, boolean isRankMatch) {
		if(isPuse()){
			return "";
		}
		StringBuffer matchMsg = new StringBuffer();
		int playerNum = players.size();
		int stage = RoomComonUtil.getMaxNumber(getMatchModel().getRuleParam());
		// 各阶段的玩家
		List<List<MatchPlayer>> stagePlayers = new ArrayList<>(stage);
		int preStageRank = 0;
		for (int i = 0; i < stage; i++) {
			int stageMinRank = preStageRank + 1;
			int stageRank = (playerNum / stage + (playerNum % stage > 0 ? 1 : 0)) * (i + 1);
			// 查找阶段内的玩家
			if(isRankMatch){
				stagePlayers.add(players.stream().filter(p -> p.getCurRank() <= stageRank && p.getCurRank() >= stageMinRank).collect(Collectors.toList()));
			}else{
				stagePlayers.add(players.stream().filter(p -> p.getCurIndex() <= stageRank && p.getCurIndex() >= stageMinRank).collect(Collectors.toList()));
			}
			preStageRank = stageRank;
			// 这个阶段没有玩家没法匹配开局
			if (stagePlayers.get(i).size() <= 0) {
				return "";
			}
		}
		List<MatchPlayer> temp = new ArrayList<>();
		while (checkStageCount(stagePlayers)) {
			temp.clear();
			for (List<MatchPlayer> list : stagePlayers) {
				temp.add(list.remove(RandomUtil.getRandomNumber(list.size())));
			}
			onStart(System.currentTimeMillis(), temp);
			matchMsg.append(printMatchMsg(temp));
		}
		sort();
		int leftNum = calStageCount(stagePlayers);
		matchMsg.append("leftNum:").append(leftNum);
		return matchMsg.toString();
	}
	
	private String printMatchMsg(List<MatchPlayer> temp){
		StringBuffer sb = new StringBuffer();
		temp.forEach((player) -> {
			sb.append(player.getAccount_id()).append(",");
		});
		if(sb.length() > 0){
			sb.deleteCharAt(sb.length() - 1);
			sb.append("#");
		}
		return sb.toString();
	}

	private boolean checkStageCount(List<List<MatchPlayer>> stagePlayers) {
		for (List<MatchPlayer> list : stagePlayers) {
			if (list.size() <= 0) {
				return false;
			}
		}
		return true;
	}
	
	private int calStageCount(List<List<MatchPlayer>> stagePlayers) {
		int num = 0;
		for (List<MatchPlayer> list : stagePlayers) {
			num += list.size();
		}
		return num;
	}

	// 玩家被淘汰
	void onPlayerOut(MatchPlayer player) {
		long accountId = player.getAccount_id();
		player.setOut(true);
		// 检查玩家是否在当前比赛场
		boolean isNeedSend = removeAccountMatchInfo(player, MatchType.REMOVE_OUT);
		// 机器人直接移除
		if (player.isRobot()) {
			return;
		}
		
		sendOutToClient(player);
		MatchPrizeRankModel prize = null;
		// 玩家已经离开比赛场 没有奖励
		if (player.isLeave()) {
			createLog(player, player.getCurRank(), id, prize, MatchType.REMOVE_OUT,"您已被淘汰");
			return;
		}
		
		prize = MatchTableManager.INSTANCE().getRankPrize(this, playerCount, player.getCurRank());
		createLog(player, player.getCurRank(), id, prize, MatchType.REMOVE_OUT,"您已被淘汰");
		
		player.setLeave(true);

		if (player.isEnter()) {
			PlayerServiceImpl.getInstance().getPlayerMap().remove(player.getAccount_id());
			MatchPrizeRankModel rankPrize = MatchTableManager.INSTANCE().getRankPrize(this, playerCount, player.getCurRank());
			MatchOverResponse.Builder b = MatchOverResponse.newBuilder();
			b.setMatchId(matchId);
			b.setMyRank(player.getCurRank());
			b.setRankPrize(MatchTableManager.INSTANCE().encodeRankPrize(rankPrize));

			if (player.getChannel() == null) {
				SessionServiceImpl.getInstance()
						.sendGate(PBUtil.toS_S2CRequet(accountId, S2CCmd.MATCH, MatchPBButils.getMatchResponse(MatchCmd.S2C_MATCH_OUT, b)).build());
			} else {
				PlayerServiceImpl.getInstance().sendMatchRsp(player, MatchPBButils.getMatchResponse(MatchCmd.S2C_MATCH_OUT, b));
			}
			
		} else if (isNeedSend) {
			sendOverToClient(accountId, player.getCurRank(), MatchCmd.S2C_MATCH_OUT);
		}
	}
	
	private void sendOutToClient(MatchPlayer player){
		MatchServerOutProto.Builder outB = MatchServerOutProto.newBuilder();
		outB.setAccountId(player.getAccount_id());
		outB.setRank(player.getCurRank());
		outB.setId(id);
		outB.setMatchId(matchId);
		outB.setIsNewAccount(player.isNewAccount());
		outB.setIsPayAccount(player.isPayAccount());
		outB.setPlayerCount(playerCount);
		SessionServiceImpl.getInstance().sendMatch(SystemConfig.match_id, PBUtil
				.toS2SRequet(S2SCmd.MATCH_SERVER, MatchS2SRequest.newBuilder().setCmd(MatchS2SCmd.S2S_MATCH_PLAYER_OUT).setPlayerOut(outB))
				.build());
	}
	
	private void sendOverToClient(long accountId,int rank,int cmd){
		MatchOverResponse.Builder clientOver = MatchOverResponse.newBuilder();
		clientOver.setMatchId(matchId);
		clientOver.setMyRank(rank);
		clientOver.setMatchType(getMatchModel().getMatchRuleModel().getMatch_type());
		SessionServiceImpl.getInstance().sendMsgToProxy(
				PBUtil.toS_S2CRequet(accountId, S2CCmd.MATCH, 
						MatchPBButils.getMatchResponse(cmd, clientOver)));
	}
	
	/** 比赛结束了 */
	private void allOver() {
		MatchTableService.getInstance().removeMatchTable(this.id);
		status = MatchType.TABLE_OVER;

		int playTime = (int) ((System.currentTimeMillis() - startTime.getTime()) / 1000);
		
		MatchServerOverProto.Builder b = MatchServerOverProto.newBuilder();
		b.setId(id);
		b.setMatchId(matchId);
		b.setPlayTime(playTime);
		b.setRobotNum(robotNum);
		b.setPlayerCount(playerCount);

		List<MatchPlayer> playerList = sort();
		playerList.forEach((player) -> {

			removeAccountMatchInfo(player, MatchType.REMOVE_OVER);
			if (player.isRobot()) {
				return;
			}
			
			MatchPrizeRankModel rankPrize = null;
			if (player.isLeave()) {
				createLog(player, player.getCurRank(), id, rankPrize, MatchType.REMOVE_OVER,"比赛结束");
				return;
			}

			if (!player.isEnter()) {
				createLog(player, player.getCurRank(), id, rankPrize, MatchType.REMOVE_OVER,"比赛结束");
				if(!isHaveAdmin()){
					sendOverToClient(player.getAccount_id(), player.getCurRank(),MatchCmd.S2C_MATCH_OVER);
				}
				return;
			}
			
			PlayerServiceImpl.getInstance().getPlayerMap().remove(player.getAccount_id());
			rankPrize = MatchTableManager.INSTANCE().getRankPrize(this, playerCount, player.getCurRank());
			createLog(player, player.getCurRank(), id, rankPrize, MatchType.REMOVE_OVER,"比赛结束");
			
			b.addAccountIds(player.getAccount_id());
			b.addRanks(player.getCurRank());
			b.addIsNewAccounts(player.isNewAccount());
			b.addIsPayAccounts(player.isPayAccount());

			if(!isHaveAdmin()){ //管理员赛  不发送结束协议
				MatchOverResponse.Builder clientOver = MatchOverResponse.newBuilder();
				clientOver.setMatchId(matchId);
				clientOver.setMyRank(player.getCurRank());
				clientOver.setRankPrize(MatchTableManager.INSTANCE().encodeRankPrize(rankPrize));
				clientOver.setMatchType(getMatchModel().getMatchRuleModel().getMatch_type());
				// 告诉客户端比赛结束了
				PlayerServiceImpl.getInstance().sendMatchRsp(player, MatchPBButils.getMatchResponse(MatchCmd.S2C_MATCH_OVER, clientOver));
			}

			try {
				if (player.getCurRank() == 1) {
					MatchRoomUtils.onMatchChampionBroadcast(player, getMatchModel(),playerCount); //临时
					//比赛获得第一名任务
					FoundationService.getInstance().sendActivityMissionProcess(player.getAccount_id(),
							ActivityMissionTypeEnum.MATCH_RANK_SUMMARY, 1, 1);
				}
			} catch (Exception e) {
				logger.error("任务执行出错",e);
			}
		});
		
		if(isHaveAdmin()){
//			sendOverToClient(getAdminId(), -1,MatchCmd.S2C_MATCH_OVER);
			for(long adminId : getAdminIds()){
				MatchManager.INSTANCE().removeAccountMatchInfo(adminId, matchId, id);
			}
			MatchTableManager.INSTANCE().sendWinnerRankToAll(this, playerList);
		}

		SessionServiceImpl.getInstance().sendMatch(SystemConfig.match_id,
				PBUtil.toS2SRequet(S2SCmd.MATCH_SERVER, MatchS2SRequest.newBuilder().setCmd(MatchS2SCmd.S2S_MATCH_OVER).setMatchOver(b)).build());
		MatchTableManager.INSTANCE().deleteMatchTop(id, isTop);
		
		logger.info("match end ===============>matchId:{},id:{} matchTime:{}s !!",
				matchId,id,(System.currentTimeMillis() - startTime.getTime()) / 1000);
	}
	
	// 比赛异常结束了
	public void allOverByException(int failType){
		status = MatchType.TABLE_OVER;
		MatchTableService.getInstance().removeMatchTable(this.id);
		
		rooms.forEach((id, room) -> {
			try {
				if (PlayerServiceImpl.getInstance().getRoomMap().containsKey(room.getRoom_id())) {
					room.force_account();
//					PlayerServiceImpl.getInstance().getRoomMap().remove(room.getRoom_id());
				}
			}catch (Exception e) {
				logger.error("allOverByException->room info roomId="+id+"|matchId="+matchId+"|id="+id,e);
			}
		});
		rooms.clear();
		
		List<MatchPlayer> playerList = getMatchPlayers();
		playerList.forEach((player) -> {
			try{
				String removeFlag = MatchType.REMOVE_BREAK;
				if(failType == MatchType.FAIL_BG_CLOSE){
					removeFlag = MatchType.REMOVE_BREAK;
				}
				removeAccountMatchInfo(player, removeFlag);
				if(player.isRobot()){
					return;
				}
				if (player.isOut()) {
					return;
				}
				if (player.isLeave()) {
					return;
				}
				if(player.isEnter()){
					PlayerServiceImpl.getInstance().getPlayerMap().remove(player.getAccount_id());
					RedisRoomUtil.clearRoom(player.getAccount_id(), 0);
					AbstractRoom room = player.getMyRoom();
					if(room != null){
					}
				}
			}catch (Exception e) {
				logger.error("allOverByException->match player matchId="+matchId+"|id="+id,e);
			}
		});
		
		if(isHaveAdmin()){
			MatchTableManager.INSTANCE().sendWinnerRefreshResp(this);
			for(long adId : getAdminIds()){
				MatchManager.INSTANCE().removeAccountMatchInfo(adId, matchId, id);
			}
		}
		
		SessionServiceImpl.getInstance().sendMatch(SystemConfig.match_id,PBUtil.toS2SRequet(S2SCmd.MATCH_SERVER, 
				MatchS2SRequest.newBuilder().setCmd(MatchS2SCmd.S2S_MATCH_START_FAIL).
				setMatchStart(MatchTableService.getInstance().getMatchServerStartMsg(matchStartMsg,failType))).build());
		MatchTableManager.INSTANCE().deleteMatchTop(id, isTop);
	}

	public List<MatchPlayer> sort() {
		List<MatchPlayer> playerList = getMatchPlayers();
		MatchPlayer player = null;
		for(int index=0;index<playerList.size();index++){
			player = playerList.get(index);
			if(player == null){
				continue;
			}
			player.setCurRank(index + 1);
		}
		broadcastMatchInfo();
		return playerList;
	}
	
	public List<MatchPlayer> sortNoBroadcast() {
		List<MatchPlayer> playerList = getMatchPlayers();
		MatchPlayer player = null;
		for(int index=0;index<playerList.size();index++){
			player = playerList.get(index);
			if(player == null){
				continue;
			}
			player.setCurRank(index + 1);
		}
		return playerList;
	}

	protected boolean handler_player_create_room(int room_id, DescParams params, List<MatchPlayer> players, int game_round) {
		int game_type_index = params._game_type_index;

		// 测试牌局
		AbstractRoom table = LogicRoomHandler.createRoom(game_type_index, params._game_rule_index);
		table.enableRobot();
		table.setCreate_type(GameConstants.CREATE_ROOM_MATCH);
		table.setRoom_id(room_id);
		table.setCreate_time(System.currentTimeMillis() / 1000L);
		table.setRoom_owner_account_id(players.get(0).getAccount_id());

		table.setRoom_owner_name(players.get(0).getNick_name());

		table.initRoomRule(id, matchId, params);
		table.setPlayOutTime(getMatchModel().getMatchChoiceModel().getPlay_card_time());
		table.matchBase = outBase;
		table.init_table(game_type_index, params._game_rule_index, game_round);
		table.setGame_id(SysGameTypeDict.getInstance().getGameIDByTypeIndex(game_type_index));
		table.handler_create_room(players, GameConstants.CREATE_ROOM_MATCH, RoomComonUtil.getMaxNumber(table, table.getDescParams()), id,
				matchId, outBase);
		PlayerServiceImpl.getInstance().getRoomMap().put(room_id, table);
		rooms.put(room_id, table);
		logger.info("match room start matchId:{} id:{} roomId:{} accountMsg:{} !!", 
				matchId, id, room_id, printMatchMsg(players));
		return true;
	}

	public int getMatchPlayerSize() {
		return getMatchPlayerMap().size();
	}
	
	void onChangeMatchBase(MatchBaseScoreJsonModel base) {
		if(!isHaveAdmin()){
			base.setCommonBoomBase();
		}
		outBase = base;
	}

	private boolean nextProgress() {
		curProgress++;

		if (curProgress >= progresses.size()) {
			return false;
		}

		MatchProgressInfo progress = progresses.get(curProgress);

		matchProgress = IMatchProgress.create(progress.getType());

		matchProgress.onInitProgress(this, progress);
		sort();
		return true;
	}

	/**
	 * 离开比赛场
	 */
	public void leave(MatchPlayer player, Session session) {
		long accountId = player.getAccount_id();
		
		player.setChannel(null);
		if (player.isOut()) {
			return;
		}

		if (player.isLeave()) {
			return;
		}
		
		if(player.isEnter()){
			PlayerServiceImpl.getInstance().getPlayerMap().remove(player.getAccount_id());
		}
		
		boolean isDelete = true;
		player.setLeave(true);
		player.setEnter(false);
		player.setOnline(false);
		player.setId(0);
		if(isHaveAdmin() && isStart() && 
				curProgress >= progresses.size() - 1 && player.getMyRoom() == null){//管理员赛 最后一局退出不算退出
			MatchManager.INSTANCE().addAccountMatchInfo(player.getAccount_id(), matchId, id, MatchType.WAIT_RANK);
			player.setVail(true);
			isDelete = false;
		}
		
		if(isDelete){
			removeAccountMatchInfo(player, MatchType.REMOVE_LEAVE,false);
		}
		
		logger.info("match leave -> accountId:{} matchId:{} id:{} status:{} !!",
				player.getAccount_id(),matchId,id,getStatus());

		AbstractRoom room = player.getMyRoom();
		if (isStart() && room != null) {
			ReentrantLock lock = room.getRoomLock();
			try {
				lock.lock();
				if (PlayerServiceImpl.getInstance().getRoomMap().containsKey(room.getRoom_id())) {
					RedisRoomUtil.clearRoom(player.getAccount_id(), 0);
				}
			} finally {
				lock.unlock();
			}
		}
		
		MatchLeaveResponse.Builder response = MatchLeaveResponse.newBuilder();
		response.setMatchId(matchId);
		session.send(PBUtil.toS_S2CRequet(accountId, S2CCmd.MATCH, MatchPBButils.getMatchResponse(MatchCmd.S2C_MATCH_LEAVE, response))
				.build());
		
		MatchServerOutProto.Builder outB = MatchServerOutProto.newBuilder();
		outB.setAccountId(accountId);
		outB.setId(id);
		outB.setMatchId(matchId);
		SessionServiceImpl.getInstance().sendMatch(SystemConfig.match_id, PBUtil
				.toS2SRequet(S2SCmd.MATCH_SERVER, MatchS2SRequest.newBuilder().setCmd(MatchS2SCmd.S2S_MATCH_PLAYER_LEAVE).setPlayerOut(outB))
				.build());
	}
	
	public void enterByAdmin(long adminId, Session session){
		MatchPlayerAdmin admin = getAdministratorById(adminId);
		if(admin == null){
			return;
		}
		int obRoomId = admin.getObRoomId();
		AbstractRoom sbRoom = rooms.get(obRoomId);
		MatchInfoResponse.Builder nb = encodeInfo();
		nb.setIsWaitRank(isAdminWaitRank(sbRoom));
	
		session.send(PBUtil.toS_S2CRequet(adminId, S2CCmd.MATCH, MatchPBButils.getMatchResponse(MatchCmd.S2C_MATCH_INFO, nb))
				.build());
	
	}
	/**
	 * 进入比赛场
	 * 
	 * @param req
	 * @param player
	 * @param session
	 */
	public void enter(MatchClientRequestWrap req, MatchPlayer player, Session session) {
		if(isHaveAdmin() && isCanAllocation()){
			return;
		}
		
		ResultCode code = isCanEnterMatch(player);
		int status = code.getStatus();
		logger.info("match enter -> accountId:{} matchId:{} id:{} status:{} eStatus:{} !!",
				player.getAccount_id(),matchId,id,getStatus(),status);
		String tip = (String) code.getObj();
		if(status != MatchType.ENTER_SAVE){
			sendEnterRsp(session,player, status, tip);
			return;
		}

		player.setChannel(session.getChannel());
		player.setMatchConnectStatus(MatchType.C_STATUS_COMMON);
		player.setOnline(true);
		player.setProxy_index(req.getProxyIndex());

		MatchInfoResponse.Builder b = encodeInfo();
		b.setIsWaitRank(isStart() && matchProgress.isWaitRank(this, player));
		b.setCurRank(player.getCurRank());
		PlayerServiceImpl.getInstance().sendMatchRsp(player, MatchPBButils.getMatchResponse(MatchCmd.S2C_MATCH_INFO, b));
		MatchManager.INSTANCE().addAccountMatchInfo(player.getAccount_id(), matchId, id, MatchType.START);
		
		sendEnterRsp(session, player, status, "");

		if (!player.isEnter()) {
			AbstractRoom room = player.getMyRoom();
		
			if (isStart() && room != null) {
				ReentrantLock lock = room.getRoomLock();
				try {
					lock.lock();
					if (PlayerServiceImpl.getInstance().getRoomMap().containsKey(room.getRoom_id())) {
						room.onPlayerEnterUpdateRedis(player.getAccount_id());
						room.handler_reconnect_room(player);
					}
				} finally {
					lock.unlock();
				}

			}
			
			player.setEnter(true);
			PlayerServiceImpl.getInstance().getPlayerMap().put(player.getAccount_id(), player);
		}
		MatchTableManager.INSTANCE().sendWinnerTableResp(this, player, session);

		if (isStart()) {
			reconnect(player);
		}
	}
	
	public void rank(MatchPlayer myPlayer, Session session){
		int rankCmd = MatchCmd.MATCH_GMAE_RANK;
		session.send(PBUtil.toS_S2CRequet(myPlayer.getAccount_id(), S2CCmd.MATCH,
				MatchPBButils.getMatchResponse(rankCmd, getRankResp(myPlayer))).build());
	}
	
	private MatchRankResponse.Builder getRankResp(MatchPlayer myPlayer){
		List<MatchPlayer> playerList = sort();
		MatchRankResponse.Builder response = MatchRankResponse.newBuilder();
		response.setMatchId(matchId);
		MatchRankMsg msg = null;
		long accountId = 0;
		int num = 1;
		boolean isMySelf = false;
		for(MatchPlayer player : playerList){
			accountId = player.getAccount_id();
			if(myPlayer != null){
				isMySelf = accountId == myPlayer.getAccount_id();
			}
			if(isMySelf){
				msg = getRankMsg(player, isMySelf);
				response.setMyRankMsg(msg);
			}
			if(num > 50){
				continue;
			}
			msg = getRankMsg(player, isMySelf);
			response.addRankMsgs(msg);
			num ++;
		}
		
		return response;
	}
	
	private MatchRankMsg getRankMsg(MatchPlayer player,boolean isMySelf){
		MatchRankMsg.Builder msg = MatchRankMsg.newBuilder();
		String nickname = player.getNick_name();
		String icon = player.getAccount_icon();
		msg.setRankIndex(player.getCurRank());
		msg.setAccountId(player.getAccount_id());
		msg.setNickname(nickname);
		msg.setHeadUrl(icon);
		msg.setScore((int) player.getCurScore() * player.getTopTimes());
		msg.setIsMySelf(isMySelf);
		msg.setIsLeave(player.isLeave());
		
		return msg.build();
	}

	protected MatchInfoResponse.Builder encodeInfo() {
		MatchInfoResponse.Builder b = MatchInfoResponse.newBuilder();
		b.setIsWaitRank(false);
		b.setCurCount(getCurCount());
		b.setCurProgess(curProgress);
		b.setCurRoom(rooms.size());
		b.setOutRank(getProgressInfo().getRiseCount());
		b.addAllProgresses(getProgressList());
		b.setMatchId(matchId);
		b.setStartTime(startTime.getTime());
		b.setIsAddtionMatch(addtionCount);
		b.setIsHaveAdmin(isHaveAdmin());
		b.setTableStatus(status);
		return b;
	}
	
	private int getCurCount(){
		int num = getMatchPlayerSize();
//		DescParams params = getMatchModel().getRuleParam();
//		int maxPlayerCount = RoomComonUtil.getMaxNumber(params);
//		int mode = num % maxPlayerCount; // 轮空的
//		if(mode == 0){
//			return num;
//		}
//		int leftNum = maxPlayerCount - mode;
//		num = leftNum + num;
		return num;
	}

	// 比赛场用
	public MatchPlayer createMatchPlayer(PlayerViewVO account, int id) {
		MatchPlayer player = new MatchPlayer();

		player.setNick_name(account.getNickName());
		player.setAccount_icon(account.getHead());
		if(isCheat()){
			player.setCheatNickname(account.getNickName());
			player.setCheatHeadIcon(account.getHead());
			player.setNick_name(cheatNickname);
			player.setAccount_icon(cheatHeadIcon);
		}
		player.setAccount_id(account.getAccountId());
		player.setProxy_session_id(account.getAccountId());
		player.setGold(account.getGold());
		player.setAccount_ip("");
		player.setAccount_ip_addr("");
		player.setId(id);
		player.setSex(account.getSex());
		player.set_seat_index(GameConstants.INVALID_SEAT);
		player.setRoom_id(0);
		player.setMoney(account.getMoney());
		player.updateLastOverTime();
		player.setPayAccount(account.isPayAccount());
		player.setNewAccount(MyDateUtil.isSameDay(account.getCreate_time()));
		player.setMatch(true);
		player.setAdminMatch(isHaveAdmin());
		player.setMatchId(matchId);
		return player;
	}

	public MatchPlayer getPlayer(long accountId) {
		MatchPlayer player = getMatchPlayerMap().get(accountId);
		return player;
	}

	// 小局结束
	public void onRoundOver(AbstractRoom room) {
		if(isPuse()){
			roundRooms.put(room.getRoom_id(), room);
			return;
		}
		
		// 定局赛有多局
		if (room._cur_round < room._game_round && PlayerServiceImpl.getInstance().getRoomMap().containsKey(room.getRoom_id())) {
			onRoundOverStart(room);
		}

		onRoundOverSettlement(room);
	}

	/**  小局结算 -> 下一局开始   */
	private void onRoundOverStart(AbstractRoom room) {
		cancelRoomFuture(room.getRoom_id());
		Future<?> rFuture = MatchTableService.getInstance().addScheduleTask(this, new Runnable() {

			@Override
			public void run() {
				if(isPuse()){
					roundStartRooms.put(room.getRoom_id(), room);
					return;
				}
				ReentrantLock lock = room.getRoomLock();
				try {
					lock.lock();
					if (!PlayerServiceImpl.getInstance().getRoomMap().containsKey(room.getRoom_id())) {
						return;
					}
					for (int i = 0; i < room.getTablePlayerNumber(); i++) {
//							room._player_ready[i] = 1;// 默认为true
					}
					room.handler_game_start();

				} catch (Exception e) {
					logger.error("onRoundOver->matchId:{} id:{} roomId:{} curRound:{} error:{} round over error !!",
							matchId,id,room.getRoom_id(),room._cur_round,e);
					room.force_account();
				} finally {
					lock.unlock();
				}
			}
		}, 4000);
		roomFutureMap.put(room.getRoom_id(), rFuture);
	}

	/**  小局结算 -> 结算   */
	private void onRoundOverSettlement(AbstractRoom room) {
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		List<MatchRankInfoProto> list = new ArrayList<>();
		for (int i = 0; i < room._player_result.game_score.length; i++) {
			Player player = room.get_players()[i];
			if (player == null) {
				continue;
			}

			// 玩家小局结算
			MatchPlayer matchPlayer = (MatchPlayer) player;
			if(i == room._cur_banker){
				matchPlayer.setWinNum(matchPlayer.getWinNum() + 1);
			}
			Integer surplusValue = matchPlayer.getConditionGroup().getSurplusCardValue();
			if(surplusValue != null && surplusValue.intValue() == 1){
				matchPlayer.setSingleNum(matchPlayer.getSingleNum() + 1);
			}
			matchPlayer.getConditionGroup().deleteSurplusCardValue();
			matchPlayer.setRoundScore(room._player_result.game_score[i] - matchPlayer.getCurScore());
			matchPlayer.setCurScore(room._player_result.game_score[i]);
			matchPlayer.setWinOrder(room._player_result.win_order[i]);
			matchPlayer.setIsTrusteeOver(room.checkTrutess(i));
			matchPlayer.setCardType(room._player_result.getCardType(i));
			matchPlayer.updateLastOverTime();
			matchPlayer.addRoundRecords(getMatchTypeRound(matchPlayer), room._cur_round, 
					(int)matchPlayer.getRoundScore());
			MatchRankInfoProto.Builder b = MatchRankInfoProto.newBuilder();
			b.setCurScore(matchPlayer.getCurScore());
			b.setNickname(matchPlayer.getNick_name());
			b.setRoundScore(matchPlayer.getRoundScore());
			b.setCardType(matchPlayer.getCardType());
			b.setAccountId(matchPlayer.getAccount_id());
			b.setTimes(matchPlayer.getTopTimes());
			list.add(b.build());

			if (getMatchModel().getMatchChoiceModel().isBalance()) {
				try {
					if (!matchPlayer.isRobot()) {
						centerRMIServer.addAccountMoney(player.getAccount_id(), (int) matchPlayer.getRoundScore(), true, "比赛牌局",
								EMoneyOperateType.ROOM_COST);
					}

					matchPlayer.setMoney(matchPlayer.getMoney() + (int) matchPlayer.getRoundScore());
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		sort();

		// 发送小局结算
		for (int i = 0; i < room._player_result.game_score.length; i++) {
			Player player = room.get_players()[i];
			if (player == null) {
				continue;
			}
			MatchPlayer matchPlayer = (MatchPlayer) player;
			if(!isSend(matchPlayer)){
				continue;
			}

			PlayerServiceImpl.getInstance().sendMatchRsp(player, 
					MatchPBButils.getMatchResponse(MatchCmd.S2C_MATCH_ROUND, getRoundOverResp(room, list, matchPlayer.getCurRank())));
			
		}
		MatchTableManager.INSTANCE().sendToAdministrator(this, room.getRoom_id(), MatchCmd.S2C_MATCH_ROUND, getRoundOverResp(room, list, 0));
	}
	
	private MatchRoundOverResponse.Builder getRoundOverResp(AbstractRoom room,List<MatchRankInfoProto> list,int rankIndex){
		MatchRoundOverResponse.Builder resp = MatchRoundOverResponse.newBuilder();
		resp.setCurRank(rankIndex);
		resp.setCurRoomCount(rooms.size());
		resp.setIsTableOver(room._cur_round == room._game_round);
		resp.addAllRanks(list);
		resp.setMatchId(matchId);
		
		return resp;
	}

	private void sendEnterRsp(Session session,MatchPlayer player, int status, String msg) {
		if(status == MatchType.NO_ENTER_REMOVE){
			removeAccountMatchInfo(player, MatchType.REMOVE_STALE);
		}

		MatchEnterResponse.Builder response = MatchEnterResponse.newBuilder();
		response.setMatchId(matchId);
		response.setStatus(status);
		response.setMsg(msg);
	
		session.send(PBUtil.toS_S2CRequet(player.getAccount_id(), S2CCmd.MATCH, MatchPBButils.getMatchResponse(MatchCmd.S2C_MATCH_ENTER, response))
				.build());
	}

	// 重连
	public void reconnect(MatchPlayer matchPlayer) {
		MatchConnectResponse.Builder b = MatchConnectResponse.newBuilder();
		if (curProgress >= 0) {
			b.setType(progresses.get(curProgress).getType());
		} else {
			b.setType(0);
		}
		AbstractRoom room = matchPlayer.getMyRoom();
		if (room != null) {
			b.setBase(room.matchBase.getBase());
			b.setBaseScore(room.matchBase.getBaseScore());
			b.setTimes(room.matchBase.getTimes());
			b.setOutScore(room.matchBase.getOutScore());
			
		} else if (outBase != null) {
			b.setBase(outBase.getBase());
			b.setBaseScore(outBase.getBaseScore());
			b.setTimes(outBase.getTimes());
			b.setOutScore(outBase.getOutScore());
		}

		b.setMatchRound(getMatchTypeRound(matchPlayer));
		MatchInfoResponse.Builder info = encodeInfo().setCurRank(matchPlayer.getCurRank());
		info.setIsWaitRank(isStart() && matchProgress.isWaitRank(this, matchPlayer));
		b.setMatchInfo(info);

		b.setCurScore((int) matchPlayer.getCurScore() * matchPlayer.getTopTimes());
		b.setMatchId(matchId);
		PlayerServiceImpl.getInstance().sendMatchRsp(matchPlayer, MatchPBButils.getMatchResponse(MatchCmd.S2C_MATCH_RESET_CONNECT, b));
		
		gameUpdate(room);
	}

	private int getMatchTypeRound(MatchPlayer player) {
		if (curProgress < 0) {
			return 1;
		}

		return matchProgress.getMatchTypeRound(player, this, progresses.get(curProgress));
	}

	private void createLog(MatchPlayer player, int rank, int id, MatchPrizeRankModel prize,
			String status,String tip) {
		if(player.isCreateLog()){
			return;
		}
		player.setCreateLog(true);
		MatchPlayerLogModel log = new MatchPlayerLogModel();
		log.setAccountId(player.getAccount_id());
		log.setStartTime(startTime);
		log.setRank(rank);
		log.setGameName(SysGameTypeDict.getInstance().getMJname(getMatchModel().getMatchRuleModel().getGame_id()));
		log.setUnqueueId(id);
		log.setAccountName(player.getNick_name());
		log.setMatchId(matchId);
		log.setGameId(getMatchModel().getMatchRuleModel().getGame_id());
		log.setMatchName(getMatchModel().getMatchRuleModel().getMatch_name());
		log.setMatchType(getMatchModel().getMatchRuleModel().getMatch_type());
		log.setStatus(status);
		log.setTip(tip);
		log.setRiseStatus(getRiseStatus(rank, player.isLeave()));
		log.setMatchGameType(isHaveAdmin() ? 1 : 0);
		log.setApplyType(player.getApplyType());
		log.setTop(isTop);
		if(isTop){
			log.setMatchGameType(2);
			log.setWinScore((int)player.getCurScore());
			log.setTimes(player.getTopTimes());
		}
		
		if (prize != null) {
			log.setHasPrize(true);
			log.setPrizes(prize.encodeLog().toByteArray());
		}
		
		MongoDBServiceImpl.getInstance().getLogQueue().add(log);
	}

	private void broadcastMatchInfo() {
		MatchInfoResponse.Builder b = encodeInfo();
		getMatchPlayerMap().forEach((accountId,player) -> {
			if(isSend(player)){
				b.setCurRank(player.getCurRank());
				b.setIsWaitRank(isStart() && matchProgress.isWaitRank(this, player));
				PlayerServiceImpl.getInstance().sendMatchRsp(player, MatchPBButils.getMatchResponse(MatchCmd.S2C_MATCH_INFO, b));
			}
		});
		
		if(isHaveAdmin()){
			MatchTableManager.INSTANCE().sendWinnerRefreshResp(this);
			for(MatchPlayerAdmin administrator : administrators){
				int obRoomId = administrator.getObRoomId();
				AbstractRoom sbRoom = rooms.get(obRoomId);
				MatchInfoResponse.Builder nb = encodeInfo();
				nb.setIsWaitRank(isAdminWaitRank(sbRoom));
//				MatchTableManager.INSTANCE().sendToAdministrator(administrator.getAccount_id(), this, MatchCmd.S2C_MATCH_INFO, nb);
			
				List<Long> accountList = new ArrayList<>();
				accountList.add(administrator.getAccount_id());
				SendToClientsProto2.Builder broadcast = SendToClientsProto2.newBuilder();
				broadcast.addAllAccountId(accountList);
				broadcast.setRsp(PBUtil.toS2CCommonRsp(S2CCmd.MATCH, MatchPBButils.getMatchResponse(MatchCmd.S2C_MATCH_INFO, nb)).build());
				SessionServiceImpl.getInstance().sendMsgToProxy(PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLENT_BATCH_SAME_PKG, broadcast));
			}
		}
	}
	
	public int getRoomSize() {
		return rooms.size();
	}
	
	private ResultCode isCanEnterMatch(MatchPlayer player){
		ResultCode code = null;

		if (player.isOut()) {
			code = new ResultCode(MatchType.NO_ENTER_REMOVE, "您已退出或被淘汰,进入失败");
			return code;
		}

		if (player.isLeave()) {
			code = new ResultCode(MatchType.NO_ENTER_REMOVE, "您已选择主动退赛,进入失败");
			return code;
		}
		
		if(player.isTimeOut()){
			code = new ResultCode(MatchType.NO_ENTER_TIMEOUT, "超过比赛限制进入时间,进入失败");
			return code;
		}

		if (!isEnterMatch(player.getAccount_id(), matchId, id)) {
			code = new ResultCode(MatchType.NO_ENTER_SAVE, "在其他比赛场中，无法进入");
			return code;
		}
		
		int roomId = SystemRoomUtil.getRoomId(player.getAccount_id());
		if(roomId > 0){
			int nowRoomId = 0;
			boolean isGame = false;
			if(isStart()){
				AbstractRoom room = player.getMyRoom();
				if(room != null && room.getRoom_id() != roomId){
					nowRoomId = room.getRoom_id();
					isGame = true;
				}
			}else{
				isGame = true;
			}
			if(isGame){
				logger.error("match enter error -> accountId:{} matchId:{} id:{} roomId:{} nowRoomId:{} !!",
						player.getAccount_id(),matchId,id,roomId,nowRoomId);
				String mName = getMatchModel().getMatchRuleModel().getMatch_name();
				code = new ResultCode(MatchType.NO_ENTER_SAVE, "由于您已在其他房间开始牌局,当前报名的[" + mName + "],无法进入!");
				return code;
			}
		}
		
		code = new ResultCode(MatchType.ENTER_SAVE,"进入成功");
		return code;
	}
	
	private boolean isEnterMatch(long accountId,int matchId,int id){
		AccountMatchInfoRedis matchInfo = MatchManager.INSTANCE().getAccountMatchByStatus(accountId, MatchType.START);
		if(matchInfo == null){
			return true;
		}
		int mMatchId = matchInfo.getMatchId();
		int mId = matchInfo.getId();
		if(mMatchId == matchId && mId == id){
			return true;
		}
		return false;
	}
	
	public boolean isSend(MatchPlayer player){
		if(player == null){
			return false;
		}
		if (!player.isEnter() || player.isLeave() || player.isRobot()) {
			return false;
		}
		return true;
	}
	
	public void addTask(Runnable task){
		MatchTableService.getInstance().addTask(this, task);
	}
	
	private List<MatchProgressInfoProto> getProgressList(){
		List<MatchProgressInfoProto> list = new ArrayList<>();
		MatchProgressInfoProto.Builder msg = null;
		for(MatchProgressInfo info : progresses){
			msg = MatchProgressInfoProto.newBuilder();
			msg.setCurRound(info.getCurRound());
			msg.setRiseCount(info.getRiseCount());
			msg.setType(info.getType());
			msg.setStartCount(info.getStartCount());
			
			list.add(msg.build());
		}
		return list;
	}
	
	public MatchProgressInfo getProgressInfo(){
		int pSize = progresses.size();
		if(curProgress < 0 || curProgress >= pSize){
			return null;
		}
		MatchProgressInfo info = progresses.get(curProgress);
		return info;
	}
	
	/** 发送进入房间通知 */
	public boolean sendEnterMsg(){
		if(isTimeout || sendEnterNum > 2){
			return false;
		}
		sendEnterNum++;
		List<MatchPlayer> noEnterList = new ArrayList<>();
		List<MatchPlayer> playerList = getMatchPlayers();
		for(MatchPlayer player : playerList){
			if(!player.isRobot() && !player.isEnter() && !player.isLeave()){
				noEnterList.add(player);
			}
		}
		
		if(noEnterList.size() <= 0){
			return false;
		}
		
		List<Long> accountList = new ArrayList<>();
		for(MatchPlayer player : noEnterList){
			if(player.isEnter() || player.isLeave()){
				continue;
			}
			accountList.add(player.getAccount_id());
		}
		
		logger.info("sendEnterMsg->matchId:{} id:{} accountList:{} !!",matchId,id,accountList);
		MatchGameStartResponse.Builder b = MatchGameStartResponse.newBuilder();
		b.setId(id);
		b.setMatchId(matchId);

		SendToClientsProto2.Builder broadcast = SendToClientsProto2.newBuilder();
		broadcast.addAllAccountId(accountList);
		broadcast.setRsp(PBUtil.toS2CCommonRsp(S2CCmd.MATCH, MatchPBButils.getMatchResponse(MatchCmd.S2C_MATCH_START, b)).build());
		SessionServiceImpl.getInstance().sendMsgToProxy(PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLENT_BATCH_SAME_PKG, broadcast));
		
		MatchServerOutProto.Builder outB = MatchServerOutProto.newBuilder();
		outB.addAllAccountIds(accountList);
		outB.setId(id);
		outB.setMatchId(matchId);
		SessionServiceImpl.getInstance().sendMatch(SystemConfig.match_id, PBUtil
				.toS2SRequet(S2SCmd.MATCH_SERVER, MatchS2SRequest.newBuilder().setCmd(MatchS2SCmd.S2S_MATCH_PLAYER_TIMEOUT).setPlayerOut(outB))
				.build());
		return true;
	}
	
	/**  一分钟为进入房间处理  */
	public void checkEnterTimeOut(){
		if(isHaveAdmin()){
			return;
		}
		isTimeout = true;
		List<MatchPlayer> timeOutList = new ArrayList<>();
		List<MatchPlayer> playerList = getMatchPlayers();
		for(MatchPlayer player : playerList){
			if(!player.isRobot() && !player.isEnter() && !player.isLeave()){
				timeOutList.add(player);
			}
		}
		
		if(timeOutList.size() <= 0){
			return;
		}

		List<Long> accountList = new ArrayList<>();
		for(MatchPlayer player : timeOutList){
			if(player.isEnter() || player.isLeave()){
				continue;
			}
			player.setLeave(true);
			player.setTimeOut(true);
			removeAccountMatchInfo(player, MatchType.REMOVE_TIMEOUT, false);
			accountList.add(player.getAccount_id());
		}
		
		logger.info("checkEnterTimeOut->matchId:{} id:{} accountList:{} timeoutNum:{} !!",matchId,id,accountList,accountList.size());
		MatchEnterResponse.Builder response = MatchEnterResponse.newBuilder();
		response.setMatchId(matchId);
		response.setStatus(MatchType.NO_ENTER_TIMEOUT);
		response.setMsg("超过比赛限制进入时间");
		
		SendToClientsProto2.Builder broadcast = SendToClientsProto2.newBuilder();
		broadcast.addAllAccountId(accountList);
		broadcast.setRsp(PBUtil.toS2CCommonRsp(S2CCmd.MATCH, MatchPBButils.getMatchResponse(MatchCmd.S2C_MATCH_ENTER, response)).build());
		SessionServiceImpl.getInstance().sendMsgToProxy(PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLENT_BATCH_SAME_PKG, broadcast));
	}
	
	public void setMatchStartMsg(MatchServerStartProto matchStartMsg) {
		this.matchStartMsg = matchStartMsg;
	}
	
	/*
	 * 移除玩家相关比赛信息
	 */
	private boolean removeAccountMatchInfo(MatchPlayer player,String removeType){
		return removeAccountMatchInfo(player, removeType, true);
	}
	
	private boolean removeAccountMatchInfo(MatchPlayer player,String removeType,boolean isRemove){
		if(player == null){
			return false;
		}
		long accountId = player.getAccount_id();
		boolean result = false;
		if(isRemove){
			MatchPlayer removePlayer = getMatchPlayerMap().remove(accountId);
			if(removePlayer != null){
				MatchTableManager.INSTANCE().sendWinnerRefreshResp(this);
				result = true;
			}
		}
		boolean isSuc = false;
		boolean isRobot = player.isRobot();
		if(!isRobot){
			isSuc = MatchManager.INSTANCE().removeAccountMatchInfo(accountId, matchId, id);
		}
		logger.info("delAccountMatchInfo-> accountId:{} isRobot:{} matchId:{} id:{} playerMsg:{} removeType:{} "
				+ "isOut:{} isSuc:{} leftNum:{} isRemove:{} status:{} !!",
				accountId,isRobot,matchId,id,player.printInfo(),removeType,result,isSuc,getMatchPlayerSize(),isRemove,status);
		return isSuc;
	}
	
	public int getCurProgress(){
		return curProgress;
	}
	
	public int getMaxProgress(){
		if(progresses == null){
			return 0;
		}
		return progresses.size();
	}
	
	public int getCurProgressType(){
		MatchProgressInfo info = getProgressInfo();
		if(info == null){
			return 0;
		}
		int type = info.getType();
		return type;
	}
	
	public int calNextScore(int score){
		int rate = MAX_RATE;
		MatchProgressInfo info = getNextProgressInfo();
		if(info != null){
			rate = info.getNextBili();
			int pType = getCurProgressType(); 
			if(rate == 0 && pType == MatchType.MATCH_FORMAT_FIX){
				return info.getNextScore();
			}
		}
		int resultScore = score * rate / MAX_RATE;
		return resultScore;
	}
	
	private MatchProgressInfo getNextProgressInfo(){
		int lastProgress = curProgress - 1;
		int pSize = progresses.size();
		if(lastProgress < 0 || lastProgress >= pSize){
			return null;
		}
		MatchProgressInfo info = progresses.get(lastProgress);
		return info;
	}
	
	public boolean isCanAllocation(){
		if(isNoStart()){
			int pType = getCurProgressType();
			if(pType == MatchType.MATCH_FORMAT_FIX && allocationPlayerList == null){
				return true;
			}
		}
		return false;
	}
	
	public boolean isAdminCanStart(){
		if(isNoStart()){
			int pType = getCurProgressType();
			if(pType == MatchType.MATCH_FORMAT_FIX && allocationPlayerList != null){
				return true;
			}
		}
		return false;
	}
	
	private void cancelOverFuture(){
		try{
			if(overFuture != null){
				overFuture.cancel(true);
				overFuture = null;
			}
		}catch (Exception e) {
			logger.error("cancelOverFuture-> error !!",e);
		}
	}
	
	private void cancelRoomFuture(int roomId){
		try{
			Future<?> roomFutrue = roomFutureMap.get(roomId);
			if(roomFutrue != null){
				roomFutrue.cancel(true);
				roomFutureMap.remove(roomId);
			}
		}catch (Exception e) {
			logger.error("cancelRoomFuture-> error roomId = " + roomId,e);
		}
	}
	
	private boolean isAdminWaitRank(AbstractRoom room){
		boolean isNextWait = false;
		MatchPlayer mPlayer = null;
		if(room != null && room.get_players() != null){
			for(Player player : room.get_players()){
				mPlayer = getPlayer(player.getAccount_id());
				if(mPlayer != null){
					break;
				}
			}
		}
		if(mPlayer != null){
			isNextWait = isStart() && matchProgress.isWaitRank(this, mPlayer);
		}else{
			isNextWait = isAdminWaitRank0();
		}
		return isNextWait;
	}
	
	private boolean isAdminWaitRank0(){
		boolean isNextWait = false;
		int pType = getCurProgressType();
		switch (pType) {
		case MatchType.MATCH_FORMAT_FIX:
			isNextWait = true;
			break;
		case MatchType.MATCH_FORMAT_SCORE_OUT:
			if(getMatchPlayerSize() <= getProgressInfo().getStopCount()){
				isNextWait = true;
			}
			break;
		case MatchType.MATCH_FORMAT_SWISS_SHIFT:
			if(getMatchPlayerSize() <= getProgressInfo().getRiseCount()){
				isNextWait = true;
			}
			break;
		}
		return isNextWait;
	}
	
	public void scoreOutAndSend(MatchPlayer matchPlayer) {
		matchPlayer.setCurRank(getMatchPlayerSize());
		MatchInfoResponse.Builder b = encodeInfo();
		if(isSend(matchPlayer)){
			b.setCurRank(matchPlayer.getCurRank());
			b.setIsWaitRank(isStart() && matchProgress.isWaitRank(this, matchPlayer));
			PlayerServiceImpl.getInstance().sendMatchRsp(matchPlayer, MatchPBButils.getMatchResponse(MatchCmd.S2C_MATCH_INFO, b));
		}
	}
	
	/** 是否开启作弊 */
	public boolean isCheat(){
		int cheatValue = getMatchModel().getMatchChoiceModel().getCheat();
		if(cheatValue == MatchType.MATCH_OPEN){
			return true;
		}
		return false;
	}
	
	public boolean isTop(){
		return isTop;
	}
	
	/** 获取晋级状态     0淘汰  1替补  2晋级  */
	public int getRiseStatus(int rankIndex, boolean isLeave){
		int status = 0;
		int subNum = 10;
		SysParamModel sModel = SysParamServerDict.getInstance().getSysParam(
				SystemType.MATCH_CHEAT, SystemType.MATCH_CHEAT);
		if(sModel != null){
			subNum = sModel.getVal1();  //替补人数
		}
		
		MatchProgressInfo cPro = getProgressInfo();
		if(cPro != null){
			int riseCount = cPro.getRiseCount();
			if(rankIndex <= riseCount){
				status = 2;
			}else if(rankIndex <= riseCount + subNum){
				status = 1;
			}
		}
		if(isLeave){
			status = 3;
		}
		return status;
	}

}
