/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.game;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import com.game.manager.PlayerMananger;

import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomPlayerResponse;

/**
 * 
 *
 * @author wu_hc date: 2017年10月13日 下午2:38:30 <br/>
 */
public final class Room {

	private int roomId;
	private int gameRuleIndex;
	private int game_type_index;
	private int game_round;
	private int cur_round;
	private int game_status;
	private long create_player_id;
	private int _banker_player;

	private RoomInfo roomIfo;

	private List<RoomPlayerResponse> players;

	public int getRoomId() {
		return roomId;
	}

	public void setRoomId(int roomId) {
		this.roomId = roomId;
	}

	public int getGameRuleIndex() {
		return gameRuleIndex;
	}

	public void setGameRuleIndex(int gameRuleIndex) {
		this.gameRuleIndex = gameRuleIndex;
	}

	public int getGame_type_index() {
		return game_type_index;
	}

	public void setGame_type_index(int game_type_index) {
		this.game_type_index = game_type_index;
	}

	public int getGame_round() {
		return game_round;
	}

	public void setGame_round(int game_round) {
		this.game_round = game_round;
	}

	public int getCur_round() {
		return cur_round;
	}

	public void setCur_round(int cur_round) {
		this.cur_round = cur_round;
	}

	public int getGame_status() {
		return game_status;
	}

	public void setGame_status(int game_status) {
		this.game_status = game_status;
	}

	public long getCreate_player_id() {
		return create_player_id;
	}

	public void setCreate_player_id(long create_player_id) {
		this.create_player_id = create_player_id;
	}

	public int get_banker_player() {
		return _banker_player;
	}

	public void set_banker_player(int _banker_player) {
		this._banker_player = _banker_player;
	}

	public RoomInfo getRoomIfo() {
		return roomIfo;
	}

	public void setRoomIfo(RoomInfo roomIfo) {
		this.roomIfo = roomIfo;
	}

	public void setPlayers(List<RoomPlayerResponse> list) {
		this.players = list;
	}

	public List<RoomPlayerResponse> getPlayers() {
		return players;
	}

	public boolean isRoomFull() {
		return null != players && players.size() == 4;
	}

	public boolean isCanStart() {
		return null != players && players.size() >= 2;
	}

	/**
	 * 随机聊天
	 */
	public void randomChat() {
		if (!isRoomFull()) {
			return;
		}
		int emjId = ThreadLocalRandom.current().nextInt(1, 10);
		int sourceId = ThreadLocalRandom.current().nextInt(0, 4);
		int targetSeat = 0;
		for (;;) {
			targetSeat = ThreadLocalRandom.current().nextInt(0, 4);
			if (targetSeat != sourceId) {
				break;
			}
		}
		RoomPlayerResponse playerRsp = players.get(sourceId);
		RoomPlayerResponse tartgetRsp = players.get(targetSeat);
		Player player = PlayerMananger.getInstance().getPlayer(playerRsp.getAccountId());
		if (null != player) {
			player.sendChat(tartgetRsp.getAccountId(), emjId);
		}
	}

	/**
	 * 
	 * @param seatIndex
	 * @return
	 */
	public RoomPlayerResponse getPlayer(int seatIndex) {
		return (null == players || players.size() < seatIndex) ? null : players.get(seatIndex);
	}
}
