package com.cai.util;

import com.cai.common.constant.MatchCmd;
import com.cai.common.constant.S2CCmd;
import com.cai.common.constant.S2SCmd;
import com.cai.common.domain.MatchBroadModel;
import com.cai.common.domain.MatchRoundModel;
import com.cai.common.domain.MatchUnionModel;
import com.cai.common.domain.Player;
import com.cai.common.domain.json.MatchPrizeDetailModel.MatchPrizeRankModel;
import com.cai.common.util.PBUtil;
import com.cai.dictionary.MatchDict;
import com.cai.dictionary.SysMatchBroadDict;
import com.cai.dictionary.SysParamServerDict;
import com.cai.game.AbstractRoom;
import com.cai.manager.MatchTableManager;
import com.cai.match.MatchPlayer;
import com.cai.match.MatchTable;
import com.cai.service.MatchTableService;
import com.cai.service.PlayerServiceImpl;
import com.cai.service.SessionServiceImpl;
import protobuf.clazz.BaseS2S.SendToClientsProto2;
import protobuf.clazz.Common.NoticeProto;
import protobuf.clazz.match.MatchClientHeaderRsp.MatchClientResponse;
import protobuf.clazz.match.MatchClientRsp.MatchRoundStartRepsonse;
import protobuf.clazz.match.MatchClientRsp.MatchScoreChangeResponse;

public class MatchRoomUtils {
	public static void onRoomFinish(AbstractRoom room) {
		// 最后一局不执行
		if (room.matchId > 0 && room._cur_round < room._game_round && PlayerServiceImpl.getInstance().getRoomMap().containsKey(room.getRoom_id())) {
			MatchTableService.getInstance().roomFinish(room);
		}
	}

	public static void onRoundStart(AbstractRoom room) {
		if (!room.is_match()) {
			return;
		}
		MatchRoundStartRepsonse.Builder b = MatchRoundStartRepsonse.newBuilder();
		b.setCurRound(room._cur_round);
		b.setGameRound(room._game_round);
		broadcast(room, MatchPBButils.getMatchResponse(MatchCmd.S2C_MATCH_ROUND_STATR, b));
		
		MatchTable table = MatchTableService.getInstance().getTable(room.id);
		if(table != null){
			MatchTableManager.INSTANCE().sendToAdministrator(table, room.getRoom_id(), MatchCmd.S2C_MATCH_ROUND_STATR, b);
		}
	}

	private static void broadcast(AbstractRoom room, MatchClientResponse.Builder response) {

		for (int i = 0; i < room.get_players().length; i++) {
			Player p = room.get_players()[i];
			if(p == null || p.isRobot() || !p.isMatch()){
				continue;
			}
			MatchPlayer mPlayer = (MatchPlayer) p;
			if(!mPlayer.isLeave() && mPlayer.isEnter()){
				PlayerServiceImpl.getInstance().sendMatchRsp(p, response);
			}
		}
	}

	// 玩家分数变化
	public static void sendScoreChange(MatchPlayer player, float oldScore,int curProgress,int maxProgress) {

		if (player.isRobot()) {
			return;
		}
		MatchScoreChangeResponse.Builder b = MatchScoreChangeResponse.newBuilder();
		b.setNewScore((int) player.getCurScore());
		b.setOldScore((int) oldScore);
		b.setCurProgress(curProgress + 1);
		b.setMaxProgress(maxProgress);
		b.setMatchId(player.getMatchId());

		PlayerServiceImpl.getInstance().sendMatchRsp(player, MatchPBButils.getMatchResponse(MatchCmd.S2C_MATCH_SCORE_CHANGE, b));
	}

	public static void onMatchChampionBroadcast(MatchPlayer player, MatchRoundModel matchModel,int playerCount) {
		MatchBroadModel broadModel = SysMatchBroadDict.getInstance().getMatchBroad(matchModel.getMatchId());
		if (broadModel == null) {
			return;
		}
		
		MatchUnionModel union = MatchDict.getInstance().getUnionModel(matchModel.getMatchRuleModel().getMatch_union_id());
		if (union == null) {
			return;
		}

		MatchPrizeRankModel prizes = MatchTableManager.INSTANCE().getRankPrize(matchModel, playerCount, player.getCurRank());
		
		StringBuilder stringB = new StringBuilder();

		if (prizes.getPrizeExattr() != null && prizes.getPrizeExattr().size() > 0) {
			prizes.getPrizeExattr().forEach((prize) -> {
				stringB.append(prize.getPrizeName()).append(" X ").append(prize.getCount()).append(",");
			});
		}

		if (prizes.getGold() > 0) {
			stringB.append(SysParamServerDict.getInstance().replaceGoldTipsWord("闲逸豆 X ")).append(prizes.getGold()).append(",");
		}

		if (prizes.getCoins() > 0) {
			stringB.append("金币 X ").append(prizes.getCoins()).append(",");
		}

		if (stringB.length() > 0) {
			stringB.deleteCharAt(stringB.length() - 1);
		}

		String content = String.format(broadModel.getNotice_content(), player.getNick_name(), union.getMatch_union_name(),
				matchModel.getMatchRuleModel().getMatch_name(), stringB.toString());
		
		SendToClientsProto2.Builder broadcast = SendToClientsProto2.newBuilder();
		broadcast.setSendAll(true);
		NoticeProto.Builder b = NoticeProto.newBuilder();
		b.setContent(content);
		b.setBroadNum(broadModel.getBroad_num());
		b.setBroadType(broadModel.getBroad_type());
		b.setInterval(0);
		broadcast.setRsp(PBUtil.toS2CCommonRsp(S2CCmd.NOTICE, b).build());

		SessionServiceImpl.getInstance().sendMsgToProxy(PBUtil.toS2SResponse(S2SCmd.SEND_TO_CLENT_BATCH_SAME_PKG, broadcast));
	}
}
