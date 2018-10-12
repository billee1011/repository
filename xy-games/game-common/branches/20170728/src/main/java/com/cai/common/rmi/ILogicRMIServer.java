package com.cai.common.rmi;

import com.cai.common.domain.LogicStatusModel;

public interface ILogicRMIServer {
	
	public String sayHello();
	
	public Long getCurDate();

	/**
	 * 状态
	 * @return
	 */
	public LogicStatusModel getLogicStatus();
	
	/**
	 * 测试是否通
	 * @return
	 */
	public boolean test();
	
	/**
	 * 后台测试牌型
	 * @param cards  
	 */
	public String testCard(String cards);
	
	
	public boolean createRobotRoom(long accountID,int roomID,int game_type_index,int game_rule_index,int game_round,String nickName,String groupID,String groupName,int isInner);


}


