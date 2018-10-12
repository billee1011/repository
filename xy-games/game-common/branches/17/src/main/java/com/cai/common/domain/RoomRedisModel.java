package com.cai.common.domain;

import java.io.Serializable;
import java.util.Set;

import com.google.common.collect.Sets;

/**
 * 存放在redis上的房间
 * @author run
 *
 */
public class RoomRedisModel implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 230582301289925473L;

	/**
	 * 房间号
	 */
	private int room_id;
	
	private String passwd = "123456";
	
	/**
	 * 逻辑服
	 */
	private int logic_index;
	
	/**
	 * 所有玩家
	 */
	private Set<Long> playersIdSet = Sets.newConcurrentHashSet();
	
	/**
	 * 创建时间
	 */
	private long create_time;
	
	/**
	 * 局数
	 */
	private int game_round;
	
	/**
	 * 玩法
	 */
	private int game_rule_index;
	
	/**
	 * 麻将类型
	 */
	private int game_type_index;
	
	
	private int pass;
	

	public int getRoom_id() {
		return room_id;
	}

	public void setRoom_id(int room_id) {
		this.room_id = room_id;
	}

	public int getLogic_index() {
		return logic_index;
	}

	public void setLogic_index(int logic_index) {
		this.logic_index = logic_index;
	}

	public String getPasswd() {
		return passwd;
	}

	public void setPasswd(String passwd) {
		this.passwd = passwd;
	}



	public Set<Long> getPlayersIdSet() {
		return playersIdSet;
	}

	public void setPlayersIdSet(Set<Long> playersIdSet) {
		this.playersIdSet = playersIdSet;
	}

	public long getCreate_time() {
		return create_time;
	}

	public void setCreate_time(long create_time) {
		this.create_time = create_time;
	}

	public int getGame_round() {
		return game_round;
	}

	public void setGame_round(int game_round) {
		this.game_round = game_round;
	}

	public int getGame_rule_index() {
		return game_rule_index;
	}

	public void setGame_rule_index(int game_rule_index) {
		this.game_rule_index = game_rule_index;
	}

	public int getGame_type_index() {
		return game_type_index;
	}

	public void setGame_type_index(int game_type_index) {
		this.game_type_index = game_type_index;
	}

	public int getPass() {
		pass = (int) (System.currentTimeMillis()-create_time);
		return pass;
	}

	public void setPass(int pass) {
		this.pass = pass;
	}

	
	
	
	

}
