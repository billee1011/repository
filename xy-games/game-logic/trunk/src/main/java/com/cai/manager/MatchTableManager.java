package com.cai.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.base.BaseTask;
import com.cai.common.constant.MatchCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.constant.S2SCmd;
import com.cai.common.core.ResultCode;
import com.cai.common.domain.AccountMatchTopRedis;
import com.cai.common.domain.MatchRoundModel;
import com.cai.common.domain.json.MatchFloatPrizeRankModel;
import com.cai.common.domain.json.MatchPrizeDetailModel.MatchPrizeRankModel;
import com.cai.common.thread.HandleMessageExecutorPool;
import com.cai.common.type.MatchType;
import com.cai.common.util.DescParams;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RoomComonUtil;
import com.cai.domain.Session;
import com.cai.game.AbstractRoom;
import com.cai.match.MatchPlayer;
import com.cai.match.MatchPlayerAdmin;
import com.cai.match.MatchTable;
import com.cai.service.SessionServiceImpl;
import com.cai.util.MatchPBButils;
import com.google.protobuf.GeneratedMessage;
import protobuf.clazz.BaseS2S.SendToClientsProto2;
import protobuf.clazz.match.MatchClientHeaderRsp.MatchClientRequest;
import protobuf.clazz.match.MatchClientRsp.MatchRankAttachMsg;
import protobuf.clazz.match.MatchClientRsp.MatchRankItemProto;
import protobuf.clazz.match.MatchClientRsp.MatchRankPrizeConfigProto;
import protobuf.clazz.match.MatchClientRsp.MatchWinnerAllocationRequest;
import protobuf.clazz.match.MatchClientRsp.MatchWinnerAllocationResponse;
import protobuf.clazz.match.MatchClientRsp.MatchWinnerListResponse;
import protobuf.clazz.match.MatchClientRsp.MatchWinnerMsg;
import protobuf.clazz.match.MatchClientRsp.MatchWinnerNoReadyListResponse;
import protobuf.clazz.match.MatchClientRsp.MatchWinnerOperationRequest;
import protobuf.clazz.match.MatchClientRsp.MatchWinnerOperationResponse;
import protobuf.clazz.match.MatchClientRsp.MatchWinnerRankResponse;
import protobuf.clazz.match.MatchClientRsp.MatchWinnerRefreshResponse;
import protobuf.clazz.match.MatchClientRsp.MatchWinnerTableResponse;
import protobuf.clazz.match.MatchClientRsp.TipMsg;
import protobuf.clazz.match.MatchClientRsp.WinnerAllocationMsg;

public class MatchTableManager {
	private static MatchTableManager manager = new MatchTableManager();
	private MatchTableManager(){}
	
	public static MatchTableManager INSTANCE(){
		return manager;
	}
	
	private final static HandleMessageExecutorPool executor = new HandleMessageExecutorPool("logic-table-executor",1);
	private static final Logger logger = LoggerFactory.getLogger(MatchTableManager.class);
	private static final AllocationComparator comparator = new AllocationComparator();
	
	public void sendWinnerRankToAll(MatchTable table,List<MatchPlayer> playerList){
		if(!table.isHaveAdmin()){
			return;
		}
		
		List<Long> accountIdList = new ArrayList<>();
		playerList.forEach(player -> {
			if(player.isRobot()){
				return;
			}
			accountIdList.add(player.getAccount_id());
		});
		
		SendToClientsProto2.Builder accountIdBroadcast = SendToClientsProto2.newBuilder();
		accountIdBroadcast.addAllAccountId(accountIdList);
		
		accountIdBroadcast.setRsp(PBUtil.toS2CCommonRsp(S2CCmd.MATCH,
				MatchPBButils.getMatchResponse(MatchCmd.MATCH_WINNER_RANK_LIST, getWinnerRankResp(false, table, playerList))).build());
		SessionServiceImpl.getInstance().sendMsgToProxy(PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLENT_BATCH_SAME_PKG, accountIdBroadcast));
		
		//管理员
		SendToClientsProto2.Builder adminBroadcast = SendToClientsProto2.newBuilder();
		adminBroadcast.addAllAccountId(table.getAdminIds());
		
		adminBroadcast.setRsp(PBUtil.toS2CCommonRsp(S2CCmd.MATCH,
				MatchPBButils.getMatchResponse(MatchCmd.MATCH_WINNER_RANK_LIST, getWinnerRankResp(true, table, playerList))).build());
		SessionServiceImpl.getInstance().sendMsgToProxy(PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLENT_BATCH_SAME_PKG, adminBroadcast));
		
	}
	
	public MatchWinnerRankResponse.Builder getWinnerRankResp(boolean isAdmin, MatchTable table, List<MatchPlayer> playerList){
		MatchWinnerRankResponse.Builder response = MatchWinnerRankResponse.newBuilder();
		response.setMatchId(table.matchId);
		for(MatchPlayer player : playerList){
			response.addRankMsgs(getWinnerMsg(isAdmin, table, player));
		}
		
		return response;
	}
	
	public MatchRankPrizeConfigProto encodeRankPrize(MatchPrizeRankModel prizeModel){
		
		if(prizeModel != null){
			return prizeModel.encodeClient();
		}
		
		MatchRankPrizeConfigProto.Builder prizeBuilder = MatchRankPrizeConfigProto.newBuilder();
		List<MatchRankItemProto> itemList = new ArrayList<>();
		prizeBuilder.addAllItems(itemList);
		return prizeBuilder.build();
	}
	
	public MatchPrizeRankModel getRankPrize(MatchTable table,int matchNum,int rankIndex){
		if(table == null){
			return null;
		}
		return getRankPrize(table.getMatchModel(), matchNum, rankIndex);
	}
	
	public MatchPrizeRankModel getRankPrize(MatchRoundModel matchModel,int matchNum,int rankIndex){
		MatchPrizeRankModel model = null;
		if(matchModel == null){
			return null;
		}
		boolean isFloat = matchModel.isFloatPrize();
		if(isFloat){
			MatchFloatPrizeRankModel floatModel = matchModel.getPrizeFloatDetailModel().getFloatPrize(matchNum);
			if(floatModel != null){
				model = floatModel.getPrize(rankIndex);
			}
		}else{
			model = matchModel.getPrizeDetailModel().getPrize(rankIndex);
		}
		return model;
	}
	
	public void handleTableManager(long adminId, MatchTable table,MatchClientRequest request,Session session){
		
		int opType = 0;
		try {
			ResultCode code = null;
			MatchWinnerOperationRequest opRequest = MatchWinnerOperationRequest.parseFrom(request.getData());
			if(table.isOver()){
				code = new ResultCode(ResultCode.FAIL, "比赛已结束,不允许操作");
				sendOperationRes(adminId, table, session, code, opRequest);
				return;
			}
			opType = opRequest.getOpType();
			switch (opType) {
			case MatchType.WINNER_OP_START:
				code = handleWinnerStart(table);
				break;
			case MatchType.WINNER_OP_STOP:
				code = handleWinnerStop(table);
				break;
			case MatchType.WINNER_OP_OBSERVE:
				break;
			}
			if(code != null){
				sendOperationRes(adminId, table, session, code, opRequest);
			}
		} catch (Exception e) {
			logger.error("handleTableManager->error type=" + opType,e);
		}
	}

	private void sendOperationRes(long adminId, MatchTable table, Session session, ResultCode code,
			MatchWinnerOperationRequest opRequest) {
		
		MatchWinnerOperationResponse.Builder response = MatchWinnerOperationResponse.newBuilder();
		response.setMatchId(opRequest.getMatchId());
		response.setOpType(opRequest.getOpType());
		TipMsg.Builder tipMsg = TipMsg.newBuilder();
		tipMsg.setStatus(code.getStatus());
		tipMsg.setTip((String) code.getObj());
		response.setMsg(tipMsg);
		
		if(code.isSuc()){
			List<Long> accountList = new ArrayList<>();
			accountList.addAll(table.getAdminIds());
			SendToClientsProto2.Builder broadcast = SendToClientsProto2.newBuilder();
			broadcast.addAllAccountId(accountList);
			broadcast.setRsp(PBUtil.toS2CCommonRsp(S2CCmd.MATCH, MatchPBButils.getMatchResponse(MatchCmd.MATCH_WINNER_OPERATION, response)).build());
			SessionServiceImpl.getInstance().sendMsgToProxy(PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLENT_BATCH_SAME_PKG, broadcast));
		}else{
			session.send(PBUtil.toS_S2CRequet(adminId, S2CCmd.MATCH, MatchPBButils.getMatchResponse(MatchCmd.MATCH_WINNER_OPERATION, response))
					.build());
		}
		
	}
	
	private ResultCode handleWinnerStop(MatchTable table){
		ResultCode code = new ResultCode(ResultCode.SUCCESS, "比赛暂停操作成功");
		if(table.isNoStart()){
			code = new ResultCode(ResultCode.FAIL, "比赛未开始,不允许暂停");
			return code;
		}
		if(table.isGaming()){
			table.onPause();
		}
		return code;
	}
	
	private ResultCode handleWinnerStart(MatchTable table){
		ResultCode code = new ResultCode(ResultCode.SUCCESS, "比赛开始操作成功");
		if(table.isNoStart()){
			if(table.isAdminCanStart()){
				List<MatchPlayer> aList = table.getAllocationPlayerList();
				if(aList != null && aList.size() > 0){
					if(!sendNoReadyListResp(table)){
						table.onStart(System.currentTimeMillis(),aList);
						table.getAllocationPlayerList().clear();
						sendWinnerRefreshResp(table);
					}
				}else{
					code = new ResultCode(ResultCode.FAIL, "请先进行配桌子");
				}
			}else{
				code = new ResultCode(ResultCode.FAIL, "比赛即将开始");
			}
		}else{
			if(table.isPuse()){
				table.onContinue();
			}
		}
		return code;
	}
	
	/** 管理员分配桌子 */
	public void handleTableAllocation(long adminId, MatchTable table,MatchClientRequest request,Session session){
		int status = -1;
		String tip = null;
		try {
			MatchWinnerAllocationRequest alloRequest = MatchWinnerAllocationRequest.parseFrom(request.getData());
			List<WinnerAllocationMsg> alloList = alloRequest.getAllocationMsgList();
			status = -1;
			tip = checkAlloactionMsg(table, alloList);
			if(tip == null){
				List<MatchPlayer> playerList = checkAndUpdateAlloactionMsg(table, alloList);
				table.setAllocationPlayerList(playerList);
				status = 1;
				tip = "分配桌子成功!";
				logger.info("handleTableAllocation->success adminIdL{} matchId:{} id:{} listNum:{} !!",
						adminId,table.getMatchId(),table.id,playerList.size());
			}
		} catch (Exception e) {
			status = -1;
			tip = "分配桌子失败,请联系管理员!";
			logger.error("handleTableAllocation->error !",e);
		}
		MatchWinnerAllocationResponse.Builder response = MatchWinnerAllocationResponse.newBuilder();
		TipMsg.Builder tipMsg = TipMsg.newBuilder();
		tipMsg.setStatus(status);
		tipMsg.setTip(tip);
		response.setMsg(tipMsg);
		
		if(status == 1){
			List<Long> accountList = new ArrayList<>();
			accountList.addAll(table.getAdminIds());
			SendToClientsProto2.Builder broadcast = SendToClientsProto2.newBuilder();
			broadcast.addAllAccountId(accountList);
			broadcast.setRsp(PBUtil.toS2CCommonRsp(S2CCmd.MATCH, MatchPBButils.getMatchResponse(MatchCmd.MATCH_WINNER_ALLOCATION, response)).build());
			SessionServiceImpl.getInstance().sendMsgToProxy(PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLENT_BATCH_SAME_PKG, broadcast));
		}else{
			session.send(PBUtil.toS_S2CRequet(adminId, S2CCmd.MATCH, MatchPBButils.getMatchResponse(MatchCmd.MATCH_WINNER_ALLOCATION, response))
					.build());
		}
	}
	
	public void sendWinnerTableResp(MatchTable table,MatchPlayer player, Session session){
		if(!table.isHaveAdmin()){
			return;
		}
		if(table.getAllocationPlayerList() == null || table.getAllocationPlayerList().size() <= 0){
			return;
		}
		
		if(!table.isNoStart()){
			return;
		}
		MatchWinnerTableResponse.Builder tableResp = MatchWinnerTableResponse.newBuilder();
		tableResp.setMatchId(table.matchId);
		tableResp.setOpType(1);
		tableResp.addInfos(table.getWinnerInfoMsg(player));
		DescParams params = table.getMatchModel().getRuleParam();
		int maxCount = RoomComonUtil.getMaxNumber(params);
		tableResp.setTableNum(maxCount);
		
		SendToClientsProto2.Builder accountIdBroadcast = SendToClientsProto2.newBuilder();
		accountIdBroadcast.addAllAccountId(table.getMatchPlayerIds());
		
		accountIdBroadcast.setRsp(PBUtil.toS2CCommonRsp(S2CCmd.MATCH,
				MatchPBButils.getMatchResponse(MatchCmd.MATCH_WINNER_TABLE_LIST, tableResp)).build());
		
		SessionServiceImpl.getInstance().sendMsgToProxy(PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLENT_BATCH_SAME_PKG, accountIdBroadcast));
		
	}
	
	private boolean sendNoReadyListResp(MatchTable table){
		if(!table.isHaveAdmin()){
			return false;
		}
		
		List<MatchPlayer> noReadyList = new ArrayList<>();
		for(MatchPlayer mPlayer : table.getAllocationPlayerList()){
			if(!mPlayer.isRobot() && !mPlayer.isLeave() && !mPlayer.isEnter()){
				noReadyList.add(mPlayer);
			}
		}
		
		if(noReadyList.size() <= 0){
			return false;
		}
		
		List<Long> accountList = new ArrayList<>();
		accountList.addAll(table.getAdminIds());
		SendToClientsProto2.Builder broadcast = SendToClientsProto2.newBuilder();
		broadcast.addAllAccountId(accountList);
		
		MatchWinnerNoReadyListResponse.Builder noResp = MatchWinnerNoReadyListResponse.newBuilder();
		noResp.setMatchId(table.matchId);
		noReadyList.forEach(mPlayer -> {
			noResp.addInfos(table.getWinnerInfoMsg(mPlayer));
		});
		broadcast.setRsp(PBUtil.toS2CCommonRsp(S2CCmd.MATCH, 
				MatchPBButils.getMatchResponse(MatchCmd.MATCH_WINNER_NO_READY_LIST, noResp)).build());
		SessionServiceImpl.getInstance().sendMsgToProxy(PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLENT_BATCH_SAME_PKG, broadcast));
		return true;
	}
	
	public void sendWinnerRefreshResp(MatchTable table){
		if(!table.isHaveAdmin()){
			return;
		}
		
		List<Long> accountList = new ArrayList<>();
		accountList.addAll(table.getAdminIds());
		SendToClientsProto2.Builder broadcast = SendToClientsProto2.newBuilder();
		broadcast.addAllAccountId(accountList);
		MatchWinnerRefreshResponse.Builder reResp = MatchWinnerRefreshResponse.newBuilder();
		reResp.setId(table.id);
		reResp.setMatchId(table.matchId);
		broadcast.setRsp(PBUtil.toS2CCommonRsp(S2CCmd.MATCH, 
				MatchPBButils.getMatchResponse(MatchCmd.MATCH_WINNER_REFRESH, reResp)).build());
		SessionServiceImpl.getInstance().sendMsgToProxy(PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLENT_BATCH_SAME_PKG, broadcast));
	}
	
	public void sendToAdministrator(MatchTable table,int roomId,int matchCmd,GeneratedMessage.Builder<?> resp){
		List<MatchPlayerAdmin> adminList = table.getAdministrators();
		if(adminList == null || adminList.size() <= 0){
			return;
		}
		
		for(MatchPlayerAdmin admin : adminList){
			int obRoomId = admin.getObRoomId();
			if(roomId == obRoomId){
				sendToAdministrator(admin.getAccount_id(), table, matchCmd, resp);
			}
		}
	}
	
	/** 发送给管理员 */
	public void sendToAdministrator(long adminId, MatchTable table,int matchCmd,GeneratedMessage.Builder<?> resp){
		List<Long> accountList = new ArrayList<>();
		accountList.add(adminId);
		SendToClientsProto2.Builder broadcast = SendToClientsProto2.newBuilder();
		broadcast.addAllAccountId(accountList);
		broadcast.setRsp(PBUtil.toS2CCommonRsp(S2CCmd.MATCH, MatchPBButils.getMatchResponse(matchCmd, resp)).build());
		SessionServiceImpl.getInstance().sendMsgToProxy(PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLENT_BATCH_SAME_PKG, broadcast));
		
	}
	
	public void sendWinnerListResp(long adminId, MatchTable table,Session session){
		List<MatchPlayer> playerList = table.sortNoBroadcast();
		MatchWinnerListResponse.Builder response = MatchWinnerListResponse.newBuilder();
		DescParams params = table.getMatchModel().getRuleParam();
		int maxCount = RoomComonUtil.getMaxNumber(params);
		response.setIsAllocation(table.isCanAllocation());
		response.setTableStatus(table.getStatus());
		response.setTableNum(maxCount);
		for(MatchPlayer player : playerList){
			response.addMsgs(getWinnerMsg(true, table, player));
		}
		
		session.send(PBUtil.toS_S2CRequet(adminId, S2CCmd.MATCH, MatchPBButils.getMatchResponse(MatchCmd.MATCH_WINNER_LIST, response))
				.build());
	}
	
	private List<MatchPlayer> checkAndUpdateAlloactionMsg(MatchTable table,List<WinnerAllocationMsg> alloList){
		List<MatchPlayer> playerList = new ArrayList<>();
		List<Long> idList = null;
		MatchPlayer player = null;
		for(WinnerAllocationMsg msg : alloList){
			idList = msg.getAccountIdsList();
			for(long accountId : idList){
				player = table.getPlayer(accountId);
				player.setAllocationId(msg.getIndex());
				playerList.add(player);
			}
		}
		Collections.sort(playerList, comparator);
		return playerList;
	}
	
	private String checkAlloactionMsg(MatchTable table,List<WinnerAllocationMsg> alloList){
		if(alloList == null){
			return "未分配任何桌子";
		}
		if(!table.isCanAllocation()){
			return "当前不允许配桌";
		}
		DescParams params = table.getMatchModel().getRuleParam();
		int maxCount = RoomComonUtil.getMaxNumber(params);
		List<Long> idList = null;
		MatchPlayer player = null;
		List<Integer> existList = new ArrayList<>();
		int alloNum = 0;
		for(WinnerAllocationMsg msg : alloList){
			if(existList.contains(msg.getIndex())){
				return "分配桌子中存在相同" + msg.getIndex() + "号桌子";
			}
			existList.add(msg.getIndex());
			idList = msg.getAccountIdsList();
			if(idList == null || idList.size() % maxCount != 0){
				return msg.getIndex() + "号牌桌人数不足,提交失败!";
			}
			alloNum += idList.size();
			for(long id : idList){
				player = table.getPlayer(id);
				if(player == null){
					logger.error("checkAlloactionMsg->no find matchPlayer matchId:{} accountId:{} !",table.getMatchId(),id);
					return "分配桌子中有玩家信息有误";
				}
			}
		}
		if(alloNum != table.getMatchPlayerSize()){
			return "还有玩家未进行配桌";
		}
		return null;
	}
	
	private MatchWinnerMsg getWinnerMsg(boolean isAdmin, MatchTable table, MatchPlayer player){
		MatchWinnerMsg.Builder msg = MatchWinnerMsg.newBuilder();
		String nickname = player.getNick_name();
		String icon = player.getAccount_icon();
		if(isAdmin && table.isCheat()){
			nickname = player.getCheatNickname();
			icon = player.getCheatHeadIcon();
		}
		msg.setIndex(player.getCurRank());
		msg.setAccountId(player.getAccount_id());
		msg.setNickname(nickname);
		msg.setHeadUrl(icon);
		msg.setScore((int) player.getCurScore());
		msg.setAlloactionIndex(player.getAllocationId());
		boolean isOb = false;
		int roomId = 0;
		AbstractRoom room = player.getMyRoom();
		if(room != null){
			isOb = true;
			roomId = room.getRoom_id();
		}
		msg.setIsObserve(isOb);
		msg.setRoomId(roomId);
		
		MatchRankAttachMsg.Builder attachMsg = MatchRankAttachMsg.newBuilder();
		attachMsg.setWinNum(player.getWinNum());
		attachMsg.setSingleNum(player.getSingleNum());
		int status = table.getRiseStatus(player.getCurRank(), player.isLeave());
		attachMsg.setStatus(status);
		
		msg.setAttachMsg(attachMsg.build());
		
		return msg.build();
	}
	
	private static class AllocationComparator implements Comparator<MatchPlayer>{

		@Override
		public int compare(MatchPlayer o1, MatchPlayer o2) {
			int result = o1.getAllocationId() - o2.getAllocationId();
			if(result == 0){
				result = (int) (o1.getAccount_id() - o2.getAccount_id());
			}
			if(result > 0){
				return 1;
			}
			if(result < 0){
				return -1;
			}
			return 0;
		}
	}
	
	public void checkMatchTop(int id, MatchTable table){
		executor.execute(new BaseTask() {
			
			@Override
			public void execute() {
				AccountMatchTopRedis redis = MatchManager.INSTANCE().getAccountMatchTop();
				if(table == null){
					redis.removeTopId(id);
				}else{
					redis.addTopId(id, table.getMatchId());
				}
				MatchManager.INSTANCE().saveAccountMatchTop(redis);
			}
		});
	}
	
	public void addMatchTop(int id, int matchId){
		executor.execute(new BaseTask() {
			
			@Override
			public void execute() {
				AccountMatchTopRedis redis = MatchManager.INSTANCE().getAccountMatchTop();
				redis.addTopId(id, matchId);
				MatchManager.INSTANCE().saveAccountMatchTop(redis);
			}
		});
	}
	
	public void deleteMatchTop(int id, boolean isTop){
		if(!isTop){
			return;
		}
		executor.execute(new BaseTask() {
			
			@Override
			public void execute() {
				AccountMatchTopRedis redis = MatchManager.INSTANCE().getAccountMatchTop();
				redis.removeTopId(id);
				MatchManager.INSTANCE().saveAccountMatchTop(redis);
			}
		});
	}

}
