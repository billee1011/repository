/**
 * 
 */
package com.cai.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * @author xwy
 *
 */
public class RobotRoom {

	/**
	 * 创建时间
	 */
	public long createTime;
	/**
	 * 房间号
	 */
	public int roomID;
	/**
	 * 创建者ID
	 */
	public long createID;
	/**
	 * 游戏类型
	 */
	public int game_type_index;
	/**
	 * 游戏规则
	 */
	public int game_rule_index;
	/**
	 * 游戏回合数
	 */
	public int game_round;
	
	public List<String> nickNames=new ArrayList<String>();
	
	public String content;
	
	public String gameDesc="";
	
	/**
	 * @param createTime
	 * @param roomID
	 * @param createID
	 * @param nickNames
	 */
	public RobotRoom(long createTime, int roomID, long createID,int game_type_index,int game_rule_index,int game_round) {
		this.createTime = createTime;
		this.roomID = roomID;
		this.createID = createID;
		this.game_type_index = game_type_index;
		this.game_rule_index = game_rule_index;
		this.game_round = game_round;
	}
}
