package com.cai.manager;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.C2SCmd;
import com.cai.common.constant.GameConstants;
import com.cai.common.domain.Player;
import com.cai.common.domain.SysGameType;
import com.cai.common.util.PBUtil;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.game.AbstractRoom;
import com.cai.service.PlayerServiceImpl;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.c2s.C2SProto.GameSettlementMsg;
import protobuf.clazz.c2s.C2SProto.SettlementAccountMsg;

public class MessageManager {
	
	private final Logger logger = LoggerFactory.getLogger(MessageManager.class);
	private static MessageManager manager = new MessageManager();
	private MessageManager(){}
	public static MessageManager INSTATNCE(){
		return manager;
	}
	
	public void sendSettlementMsg(AbstractRoom room){
		if(room == null){
			logger.info("sendSettlementMsg->send fail room == null !!");
			return;
		}
		int gameType = room.getCreate_type();
		if(gameType == GameConstants.CREATE_ROOM_MATCH){
			return;
		}
		GameSettlementMsg.Builder response = GameSettlementMsg.newBuilder();
		response.setGameId(room._game_type_index);
		response.setRoomId(room.getRoom_id());
		response.setCurRound(room._cur_round);
		response.setMaxRound(room._game_round);
		response.setOverTime(System.currentTimeMillis()/1000);
		response.setGameType(gameType);
		if(null != room._recordRoomRecord) {
			if(null != room._recordRoomRecord.getBrand_id()) {
				response.setBrandId(room._recordRoomRecord.getBrand_id().toString());
			}
		}
		int typeId = 0;
		int typeSubId = 0;
		String typeName = "";
		switch (gameType) {
		case GameConstants.CREATE_ROOM_MATCH:
			typeId = room.matchId;
			typeSubId = room.id;
			break;
		case GameConstants.CREATE_ROOM_CLUB:
			typeId = room.clubInfo.clubId;
			typeName = room.clubInfo.clubName;
			break;
		}
		response.setTypeName(typeName);
		response.setTypeId(typeId);
		response.setTypeSubId(typeSubId);
		String appName = "";
		String gameName = "";
		SysGameType sysType = SysGameTypeDict.getInstance().getSysGameType(room._game_type_index);
		if(sysType != null){
			appName = sysType.getAppName();
			gameName = sysType.getDesc();
		}
		response.setAppName(appName);
		response.setGameName(gameName);
		Player player = null;
		SettlementAccountMsg.Builder aMsg = null;
		List<Player> sendList = new ArrayList<>();
		for (int index = 0; index < room._player_result.game_score.length; index++) {
			player = room.getPlayerBySeatId(index);
			if (player == null) {
				continue;
			}
			sendList.add(player);
			aMsg = SettlementAccountMsg.newBuilder();
			aMsg.setAccountId(player.getAccount_id());
			aMsg.setNickName(player.getNick_name());
			aMsg.setScore((int) room._player_result.game_score[index]);
			
			response.addAccountMsgs(aMsg);
		}
		
		sendToPlayers(sendList,PBUtil.toS2CCommonRsp(C2SCmd.GAME_SETTLEMENT, response).build());
	}
	
	private void sendToPlayers(List<Player> list,Response response){
		for(Player player : list){
			sendToPlayer(player, response);
		}
	}
	
	private void sendToPlayer(Player player,Response response){
		if(player == null){
			return;
		}
		PlayerServiceImpl.getInstance().send(player, response);
	}

}
