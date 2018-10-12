package com.cai.match;

import java.util.List;
import com.cai.common.domain.json.MatchBaseScoreJsonModel;
import com.cai.game.AbstractRoom;
import com.cai.util.MatchRoomUtils;

/**
 * 
 * 瑞士移位
 *
 * @author demon 
 * date: 2018年1月10日 上午11:08:07 <br/>
 */
public class MatchSwissProgress implements IMatchProgress {
	
	private final static MatchSwissProgress instance = new MatchSwissProgress();
	
	private MatchSwissProgress(){
		
	}

	static MatchSwissProgress getInstance(){
		return instance;
	}

	@Override
	public boolean onGameOver(AbstractRoom room, MatchTable table, MatchPlayer player, long ctime) {
		return true;
	}

	@Override
	public void onUpdate(long ctime, MatchTable table) {
		
	}

	@Override
	public void overProgress(MatchTable table) {
		List<MatchPlayer> playerList = table.sort();
		int limitSize = table.getProgressInfo().getRiseCount();

		if (limitSize < playerList.size()) {
			for (int i = limitSize; i < playerList.size(); i++) {
				table.onPlayerOut(playerList.get(i));
			}
		}
	}

	@Override
	public void onInitProgress(MatchTable table, MatchProgressInfo curProgress) {
		MatchBaseScoreJsonModel outBase = curProgress.getMatchBase();
		table.onChangeMatchBase(outBase);
		
		table.getMatchPlayerMap().forEach((accountId,player) -> {
			int oldScore = (int) player.getCurScore();
			int nowScore = table.calNextScore(oldScore);
			
			if(nowScore < 0){
				nowScore = 0;
			}
			
			player.setCurScore(nowScore);
			
			if(table.isSend(player)){
				MatchRoomUtils.sendScoreChange(player, oldScore, table.getCurProgress(),table.getMaxProgress());
			}
		});
	}

	@Override
	public int addMatchTypeRound(MatchPlayer player, MatchTable table, MatchProgressInfo curProgress) {
		player.setMatchTypeRound(player.getMatchTypeRound() + 1);
		return getMatchTypeRound(player, table, curProgress);
	}

	@Override
	public int getMatchTypeRound(MatchPlayer player, MatchTable table, MatchProgressInfo curProgress) {
		return player.getMatchTypeRound();
	}

	@Override
	public int getType() {
		return SWISS_SHIFT;
	}

	@Override
	public boolean isNeedChangeProgress(MatchTable table) {
//		if( table.curPlayers.size() > table.rankSize){
//			return false;
//		}
		return table.getRoomSize() <= 0;
	}

	@Override
	public boolean isWaitRank(MatchTable table, MatchPlayer player) {
		return table.getMatchPlayerSize() <= table.getProgressInfo().getRiseCount() && player.getMyRoom() == null;
	}

}
