package com.cai.match;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.cai.common.domain.json.MatchBaseScoreJsonModel;
import com.cai.game.AbstractRoom;

/**
 * 打立出局配置
 * 
 * @author demon date: 2018年1月9日 下午4:02:33 <br/>
 */
public class MatchScoreOutProgress implements IMatchProgress {

	private static final Logger logger = LoggerFactory.getLogger(MatchScoreOutProgress.class);
	private final static MatchScoreOutProgress instance = new MatchScoreOutProgress();

	private MatchScoreOutProgress() {

	}

	static MatchScoreOutProgress getInstance() {
		return instance;
	}

	@Override
	public boolean onGameOver(AbstractRoom room, MatchTable table, MatchPlayer matchPlayer, long ctime) {
		// 打立出局 给玩家结算动画时间后，才进行匹配
		matchPlayer.setMatchingTime(ctime + 4000);
		// 如果玩家低于淘汰分则淘汰
		if ((matchPlayer.getCurScore() <= room.matchBase.getOutScore() && 
				table.getMatchPlayerSize() > table.getProgressInfo().getStopCount())
				|| matchPlayer.isTemporary()) {
			// 有可能分高排名高的先被淘汰
			table.scoreOutAndSend(matchPlayer);
			logger.info("onGameOver->score out accountId:{} matchId:{} id:{} ranIndex:{} curScore:{} outScore:{} stopCount:{} !!",
					matchPlayer.getAccount_id(),table.matchId,table.id,matchPlayer.getCurRank(),
					matchPlayer.getCurScore(),room.matchBase.getOutScore(),table.getProgressInfo().getStopCount());
			table.onPlayerOut(matchPlayer);
			return true;
		}
		return false;
	}

	@Override
	public void onUpdate(long ctime, MatchTable table) {
		if (table.getMatchPlayerSize() > table.getProgressInfo().getStopCount()) {
			table.onCheckMatching(ctime);
			return;
		}
	}

	@Override
	public void overProgress(MatchTable table) {
		// 当前是打立出局，淘汰玩家
		int limitSize = table.getProgressInfo().getRiseCount();

		List<MatchPlayer> playerList = table.sort();

		if (limitSize < playerList.size()) {
			for (int i = limitSize; i < playerList.size(); i++) {
				table.onPlayerOut(playerList.get(i));
			}
		}
	}

	@Override
	public void onInitProgress(MatchTable table, MatchProgressInfo curProgress) {
		MatchBaseScoreJsonModel outBase = table.getMatchModel().getMatchBaseScoreModel().getConfig(table.startTime.getTime());

		table.onChangeMatchBase(outBase);
	}

	@Override
	public int addMatchTypeRound(MatchPlayer player, MatchTable table, MatchProgressInfo curProgress) {
		player.setMatchTypeRound(player.getMatchTypeRound() + 1);
		return getMatchTypeRound(player, table, curProgress);
	}

	@Override
	public int getMatchTypeRound(MatchPlayer player, MatchTable table, MatchProgressInfo curProgress) {
		// TODO Auto-generated method stub
		return player.getMatchTypeRound();
	}

	@Override
	public int getType() {
		return SCORE_OUT;
	}

	@Override
	public boolean isNeedChangeProgress(MatchTable table) {
		if (table.getMatchPlayerSize() > table.getProgressInfo().getStopCount()) {
			return false;
		}
		return table.getRoomSize() <= 0;
	}

	@Override
	public boolean isWaitRank(MatchTable table, MatchPlayer player) {
		return table.getMatchPlayerSize() <= table.getProgressInfo().getStopCount() && player.getMyRoom() == null;
	}

}
