package com.cai.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.cai.common.constant.S2SCmd;
import com.cai.common.domain.MatchRoundModel;
import com.cai.common.thread.HandleMessageExecutorPool;
import com.cai.common.type.MatchType;
import com.cai.common.util.PBUtil;
import com.cai.dictionary.MatchDict;
import com.cai.game.AbstractRoom;
import com.cai.manager.MatchTableManager;
import com.cai.match.MatchPlayer;
import com.cai.match.MatchSortComparator;
import com.cai.match.MatchStaleInfo;
import com.cai.match.MatchTable;
import com.cai.tasks.MatchEnterTimeOutTask;
import com.cai.tasks.MatchGameObserverTask;
import com.cai.tasks.MatchGameOverTask;
import com.cai.tasks.MatchSendEnterTask;
import com.cai.tasks.MatchUpdateRoomInfoTask;
import com.xianyi.framework.core.transport.netty.session.S2SSession;

import protobuf.clazz.match.MatchRsp.MatchS2SCmd;
import protobuf.clazz.match.MatchRsp.MatchS2SRequest;
import protobuf.clazz.match.MatchRsp.MatchServerStartProto;
import protobuf.clazz.match.MatchRsp.MatchServerTopStatusProto;

public class MatchTableService{
	
	private static Logger logger = LoggerFactory.getLogger(MatchTableService.class);
	private final static int NO_ENTER_TIME = 3 * 60 * 1000; //禁止进入比赛房间时间限制
	private final static int SEND_ENTER_TIME = 30 * 1000; //发送进入房间时间
	private final MatchSortComparator comparator = new MatchSortComparator();
	
	private final Map<Integer, MatchTable> matchTables = new ConcurrentHashMap<>();
	private final Map<Integer, MatchStaleInfo> staleTables = new ConcurrentHashMap<>();
	
	private final HandleMessageExecutorPool matchExcutor = new HandleMessageExecutorPool("match-table-executor");
	private final HandleMessageExecutorPool checkOverPool = new HandleMessageExecutorPool("match-check-executor",1);

	private static final MatchTableService service = new MatchTableService();
	
	private MatchTableService(){
		matchExcutor.scheduleAtFixedSecond(new Runnable() {
			@Override
			public void run() {
				try{
					matchTables.forEach((id, match)->{
						update(match);
					});
				}catch (Exception e) {
					logger.error("match table update error !!",e);
				}
			}
		}, 1, 1);
		
		checkOverPool.scheduleAtFixedSecond(new Runnable() {
			@Override
			public void run() {
				try{
					checkStaleMatch();
				}catch (Exception e) {
					logger.error("checkMatchOver error !!",e);
				}
			}
		}, 5 * 60, 5 * 60);
	}
	
	private void checkStaleMatch(){
		List<MatchStaleInfo> staleList = new ArrayList<>();
		for(MatchStaleInfo info : staleTables.values()){
			if(!info.isSameDay()){
				staleList.add(info);
			}
		}
		if(staleList.size() <= 0){
			return;
		}
		staleList.forEach((info) -> {
			staleTables.remove(info.getId());
		});
	}
	
	private void update(MatchTable match){
		match.addTask(new Runnable() {
			@Override
			public void run() {
				try{
					match.update(System.currentTimeMillis());
				}catch (Exception e) {
					logger.error("match update error matchId:"+match.matchId,e);
				}
			}
		});
	}
	
	public static final MatchTableService getInstance(){
		return service;
	}
	
	public void matchTopStatus(MatchServerTopStatusProto topStatus){
		int id = topStatus.getId();
		MatchTable table = matchTables.get(id);
		MatchTableManager.INSTANCE().checkMatchTop(id, table);
	}

	public void matchStart(MatchServerStartProto matchStart, S2SSession session) {
		int matchId = matchStart.getMatchId();
		int id = matchStart.getId();
		MatchTable matchTable = getTable(id);
		
		if(matchStart.getIsStop()){
			if(matchTable != null && matchTable.getMatchId() == matchId){
				matchOverByException(matchTable, MatchType.FAIL_BG_CLOSE);
			}
			return;
		}
		
		MatchRoundModel matchRound = MatchDict.getInstance().getMatchModel(matchId);
		// 開局失敗
		if (matchRound == null) {
			session.send(PBUtil.toS2SRequet(S2SCmd.MATCH_SERVER,
					MatchS2SRequest.newBuilder().setCmd(MatchS2SCmd.S2S_MATCH_START_FAIL).
					setMatchStart(getMatchServerStartMsg(matchStart, MatchType.FAIL_EXCEPTION))));
			return;
		}
		
		if(matchTable != null){
			logger.error("matchStart->fail exist match game matchId:{} id:{} existMatchId:{}",
					matchId,id,matchTable.getMatchId());
			session.send(PBUtil.toS2SRequet(S2SCmd.MATCH_SERVER,
					MatchS2SRequest.newBuilder().setCmd(MatchS2SCmd.S2S_MATCH_START_FAIL).
					setMatchStart(getMatchServerStartMsg(matchStart, MatchType.FAIL_HAD_START))));
			return;
		}
		
		addTask(id, new Runnable() {
			
			@Override
			public void run() {
				try {
					long now = System.currentTimeMillis();
					List<Long> accountList = matchStart.getAccountIdsList();
					List<Long> adminIds = matchStart.getMatchAdminIdsList();
					MatchTable newTable = new MatchTable(id, matchId, adminIds, matchStart.getIsTop(), comparator);
					newTable.setMatchStartMsg(matchStart);
					String accountListMsg = newTable.gameStart(matchStart);
					MatchEnterTimeOutTask task = new MatchEnterTimeOutTask(newTable);
					addScheduleTask(newTable, task, NO_ENTER_TIME);
					sendEnterMessage(newTable);
					matchTables.put(id, newTable);
					if(matchStart.getIsTop()){
						MatchTableManager.INSTANCE().addMatchTop(id, matchId);
					}
					logger.info("match start ===============>matchId:{},id:{},adminIds:{},playerNum:{} costTime:{}s accountListMsg:{} !!",
							matchId,id,adminIds,accountList.size(),(System.currentTimeMillis()-now)/1000,accountListMsg);
				} catch (Exception e) {
					session.send(PBUtil.toS2SRequet(S2SCmd.MATCH_SERVER,
							MatchS2SRequest.newBuilder().setCmd(MatchS2SCmd.S2S_MATCH_START_FAIL).
							setMatchStart(getMatchServerStartMsg(matchStart, MatchType.FAIL_EXCEPTION))));
					logger.error("比赛开始失败 matchId:"+matchId+" id:"+id,e);
				}
			}
		});
	}
	
	public void sendEnterMessage(MatchTable table){
		MatchSendEnterTask task = new MatchSendEnterTask(table);
		addScheduleTask(table, task, SEND_ENTER_TIME);
	}
	
	//检查突然关闭的比赛
	public void checkCloseMatch(){
		checkOverPool.execute(new Runnable() {
			
			@Override
			public void run() {
				try{
					List<MatchTable> staleList = new ArrayList<>();
					MatchRoundModel matchRound = null;
					for(MatchTable table : matchTables.values()){
						matchRound = MatchDict.getInstance().getMatchModel(table.matchId);
						if(matchRound == null || matchRound.isClose()){
							staleList.add(table);
						}
					}
					
					for(MatchTable table : staleList){
						matchOverByException(table, MatchType.FAIL_BREAK_UP);
					}
					
				}catch (Exception e) {
					logger.error("checkCloseMatch error !!",e);
				}
			}
		});
	}
	
	private void matchOverByException(MatchTable table,int failType){
		if (table != null) {
			table.addTask(new Runnable() {
				@Override
				public void run() {
					table.allOverByException(failType);
				}
			});
		}
	}
	
	/**
	 * 比赛场游戏旁观处理
	 */
	public void matchObserver(int id,AbstractRoom room,long accountId,boolean isEnter){
		MatchTable table = getTable(id);
		if(table == null){
			return;
		}
		MatchGameObserverTask task = new MatchGameObserverTask(table, room, accountId, isEnter);
		addTask(table, task);
	}
	
	/**
	 * 比赛场游戏结束处理
	 */
	public void matchOver(int id,AbstractRoom room){
		MatchTable table = getTable(id);
		if(table == null){
			return;
		}
		MatchGameOverTask task = new MatchGameOverTask(table, room);
		addTask(table, task);
	}
	
	/**
	 * 比赛场更新房间信息
	 */
	public void updateRoomInfo(int id,AbstractRoom room){
		MatchTable table = getTable(id);
		if(table == null){
			return;
		}
		MatchUpdateRoomInfoTask task = new MatchUpdateRoomInfoTask(table, room);
		addTask(table, task);
	}
	
	public void roomFinish(AbstractRoom room){
		if(room == null){
			return;
		}
		MatchTable table = getTable(room.id);
		if (table != null) {
			table.addTask(new Runnable() {
				@Override
				public void run() {
					table.onRoundOver(room);
				}
			});
		}
	}
	
	public MatchTable getTable(int id){
		return matchTables.get(id);
	}
	
	public MatchTable getMatchByAccountId(long accountId){
		for (MatchTable table : matchTables.values()) {
			MatchPlayer player = table.getPlayer(accountId);
			if(player == null || player.isOut() || player.isLeave()){
				continue;
			}
			
			return table;
		}
		return null;
	}

	public void addMatchTable(MatchTable table){
		if(table == null){
			return;
		}
		matchTables.put(table.id, table);
	}
	
	public void removeMatchTable(int id) {
		MatchTable table = matchTables.remove(id);
		if(table != null){
			MatchStaleInfo staleInfo = new MatchStaleInfo(table.matchId, table.id);
			staleTables.put(table.id, staleInfo);
		}
	}

	public void addTask(MatchTable table,Runnable task){
		addTask(table.id, task);
	}
	
	public void addTask(int index,Runnable task){
		matchExcutor.execute(index, task);
	}
	
	public Future<?> addScheduleTask(MatchTable table,Runnable task,int delayTime){
		Future<?> future = matchExcutor.scheduleMSecond(table.id,task, delayTime);
		return future;
	}
	
	public MatchServerStartProto getMatchServerStartMsg(MatchServerStartProto matchStart,int status){
		MatchServerStartProto.Builder msg = MatchServerStartProto.newBuilder();
		msg.setMatchId(matchStart.getMatchId());
		msg.setId(matchStart.getId());
		msg.setStartTime(matchStart.getStartTime());
		msg.setLogicIndex(matchStart.getLogicIndex());
		msg.addAllAccountIds(matchStart.getAccountIdsList());
		msg.setFailType(status);
		
		return msg.build();
	}
	
}
