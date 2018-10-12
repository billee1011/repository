package com.cai.match;

import java.util.List;
import com.cai.common.domain.json.MatchBaseScoreJsonModel;
import com.cai.game.AbstractRoom;
import com.cai.util.MatchRoomUtils;

/**
 * 定局赛
 * 
 *
 * @author demon 
 * date: 2018年1月15日 下午6:38:15 <br/>
 */
public class MatchFixProgress implements IMatchProgress {
	
	private final static MatchFixProgress instance = new MatchFixProgress();
	
	private MatchFixProgress(){
		
	}
	
	static MatchFixProgress getInstance(){
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
		List<MatchPlayer> matchPlayers = table.sort();
		int limitSize = table.getProgressInfo().getRiseCount();

		int playerNum = matchPlayers.size();
		if (limitSize < playerNum) {
			for (int i = limitSize; i < playerNum; i++) {
				table.onPlayerOut(matchPlayers.get(i));
			}
		}
		
//		List<MatchPlayer> temp = new ArrayList<>();
//		for (MatchPlayer matchPlayer : matchPlayers) {
//			if (matchPlayer.getWinOrder() == 0 && temp.size() < limitSize) {
//				temp.add(matchPlayer);
//			}
//		}
//
//		// 规则先让小组第一晋级，然后按排名补充到比赛人数
//		if (limitSize > temp.size()) {
//			// 第一名的人数不够，补满晋级人数
//			for (int i = 0; i < matchPlayers.size(); i++) {
//				MatchPlayer matchPlayer = matchPlayers.get(i);
//				if (limitSize <= temp.size()) {
//					break;
//				} else if (matchPlayer.getWinOrder() != 0) {
//					temp.add(matchPlayer);
//				}
//			}
//		}
//		int index = limitSize;
//		for (int i = 0; i < matchPlayers.size(); i++) {
//			MatchPlayer matchPlayer = matchPlayers.get(i);
//			if (temp.contains(matchPlayer)) {
//				continue;
//			}
//			table.onPlayerOut(++index, matchPlayer);
//		}
	}

	@Override
	public void onInitProgress(MatchTable table, MatchProgressInfo curProgress) {
		//初始化分数
		MatchBaseScoreJsonModel outBase = curProgress.getMatchBase();
		
		table.onChangeMatchBase(outBase);
		
		//弱化分数
		table.getMatchPlayerMap().forEach((accountId,player) -> {
			int oldScore = (int) player.getCurScore();
			int nowScore = table.calNextScore(oldScore);
			
			if(nowScore < 0){
				nowScore = 0;
			}
			
			player.setCurScore(nowScore);
			if(table.isSend(player)){
				MatchRoomUtils.sendScoreChange(player, oldScore,table.getCurProgress(),table.getMaxProgress());
			}
		});
	}

	@Override
	public int addMatchTypeRound(MatchPlayer player, MatchTable table, MatchProgressInfo curProgress) {
		return getMatchTypeRound(player, table, curProgress);
	}

	@Override
	public int getMatchTypeRound(MatchPlayer player, MatchTable table, MatchProgressInfo curProgress) {
		return curProgress.getCurRound();
	}

	@Override
	public int getType() {
		return FIXED;
	}

	@Override
	public boolean isNeedChangeProgress(MatchTable table) {
		if(table.isTop()){
			return true;
		}
		return table.getRoomSize() <= 0;
	}

	@Override
	public boolean isWaitRank(MatchTable table, MatchPlayer player) {
		if(table.isTop()){
			return false;
		}
		return player.getMyRoom() == null;
	}

}
