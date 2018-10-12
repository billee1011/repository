package com.cai.rmi.impl;

import org.apache.commons.lang.StringUtils;

import com.cai.common.domain.LogicStatusModel;
import com.cai.common.rmi.ILogicRMIServer;
import com.cai.core.RequestHandlerThreadPool;
import com.cai.core.SystemConfig;
import com.cai.game.fls.FLSTable;
import com.cai.game.hh.HHTable;
import com.cai.game.mj.MJTable;
import com.cai.game.nn.NNTable;
import com.cai.handler.LogicRoomHandler;
import com.cai.service.PlayerServiceImpl;

public class LogicRMIServerImpl implements ILogicRMIServer{

	@Override
	public String sayHello() {
		System.out.println("logic say hello");
		return "logic say hello";
	}

	@Override
	public Long getCurDate() {
		return System.currentTimeMillis();
	}

	@Override
	public LogicStatusModel getLogicStatus() {
		LogicStatusModel model = new LogicStatusModel();
		model.setLogic_game_id(SystemConfig.logic_index);
		model.setOnline_playe_num(1);
		model.setSocket_connect_num(1);
		
		//消息处理情况
		model.setMsg_receive_count(RequestHandlerThreadPool.getInstance().getTpe().getTaskCount());
		model.setMsg_completed_count(RequestHandlerThreadPool.getInstance().getTpe().getCompletedTaskCount());
		model.setMsg_queue_count(RequestHandlerThreadPool.getInstance().getBlockQueue().size());
		
		return model;
	}
	
	/**
	 * 测试是否通
	 * @return
	 */
	public boolean test(){
		return true;
	}

	@Override
	public String testCard(String cards) {
		try{
			String[] bodys = StringUtils.split(cards, "#");
			if(bodys.length<=0) {
				return "参数非法";
			}
			int roomID = Integer.parseInt(bodys[1]);
			int head = Integer.parseInt(bodys[2]);
			String cardList = bodys[3];
			String[] arrayStr = StringUtils.split(cardList, ",");
			int[] arrays = new int[arrayStr.length];
			for(int i=0;i<arrays.length;i++) {
				arrays[i] = Integer.decode(arrayStr[i]);
			}
			
			if(bodys[0].equalsIgnoreCase("MJ")) {
				MJTable mjTable = (MJTable) PlayerServiceImpl.getInstance().getRoomMap().get(roomID);
				if(mjTable==null) {
					return "房间不存在";
				}
				switch(head) {
				case 1:
					if(arrayStr.length<=40){
						return "牌型数量不对";
					}
					mjTable.BACK_DEBUG_CARDS_MODE=true;
					mjTable.debug_my_cards=arrays;
					break;
				case 2:
					if(arrayStr.length!=13){
						return "牌型数量不对";
					}
					mjTable.BACK_DEBUG_CARDS_MODE=true;
					mjTable.debug_my_cards=arrays;
					break;
				case 3:
					if(arrays.length>(mjTable._all_card_len-mjTable.GRR._left_card_count)) {
						return "牌堆数量不够";
					}
					for(int i=0;i<arrays.length;i++) {
						mjTable._repertory_card[mjTable._all_card_len-mjTable.GRR._left_card_count+i]=arrays[i];
					}
					break;
				case 4:
					mjTable.GRR._left_card_count=1;
					break;
				default:
					return "操作未定义";
				}
				return "操作成功"+cardList;
			}else if(bodys[0].equalsIgnoreCase("FLS")) {
				FLSTable mjTable = (FLSTable) PlayerServiceImpl.getInstance().getRoomMap().get(roomID);
				if(mjTable==null) {
					return "房间不存在";
				}
				switch(head) {
				case 1:
					if(arrayStr.length<=40){
						return "牌型数量不对";
					}
					mjTable.BACK_DEBUG_CARDS_MODE=true;
					mjTable.debug_my_cards=arrays;
					break;
				case 2:
					mjTable.BACK_DEBUG_CARDS_MODE=true;
					mjTable.debug_my_cards=arrays;
					break;
				case 3:
					if(arrays.length>(mjTable._all_card_len-mjTable.GRR._left_card_count)) {
						return "牌堆数量不够";
					}
					for(int i=0;i<arrays.length;i++) {
						mjTable._repertory_card[mjTable._all_card_len-mjTable.GRR._left_card_count+i]=arrays[i];
					}
					break;
				case 4:
					mjTable.GRR._left_card_count=4;
					break;
				default:
					return "操作未定义";
				}
				return "操作成功"+cardList;
			}
			else if(bodys[0].equalsIgnoreCase("NN")) {
				NNTable mjTable = (NNTable) PlayerServiceImpl.getInstance().getRoomMap().get(roomID);
				if(mjTable==null) {
					return "房间不存在";
				}
				switch(head) {
				case 1:
					if(arrayStr.length<=40){
						return "牌型数量不对";
					}
					mjTable.BACK_DEBUG_CARDS_MODE=true;
					mjTable.debug_my_cards=arrays;
					break;
				case 2:
					mjTable.BACK_DEBUG_CARDS_MODE=true;
					mjTable.debug_my_cards=arrays;
					break;
				case 3:
					if(arrays.length>(mjTable._all_card_len-mjTable.GRR._left_card_count)) {
						return "牌堆数量不够";
					}
					for(int i=0;i<arrays.length;i++) {
						mjTable._repertory_card[mjTable._all_card_len-mjTable.GRR._left_card_count+i]=arrays[i];
					}
					break;
				case 4:
					mjTable.GRR._left_card_count=4;
					break;
				default:
					return "操作未定义";
				}
				return "操作成功"+cardList;
			}else if(bodys[0].equalsIgnoreCase("HH")) {
				HHTable mjTable = (HHTable) PlayerServiceImpl.getInstance().getRoomMap().get(roomID);
				if(mjTable==null) {
					return "房间不存在";
				}
				switch(head) {
				case 1:
					if(arrayStr.length<=40){
						return "牌型数量不对";
					}
					mjTable.BACK_DEBUG_CARDS_MODE=true;
					mjTable.debug_my_cards=arrays;
					break;
				case 2:
					mjTable.BACK_DEBUG_CARDS_MODE=true;
					mjTable.debug_my_cards=arrays;
					break;
				case 3:
					if(arrays.length>(mjTable._all_card_len-mjTable.GRR._left_card_count)) {
						return "牌堆数量不够";
					}
					for(int i=0;i<arrays.length;i++) {
						mjTable._repertory_card[mjTable._all_card_len-mjTable.GRR._left_card_count+i]=arrays[i];
					}
					break;
				case 4:
					mjTable.GRR._left_card_count=4;
					break;
				default:
					return "操作未定义";
				}
				return "操作成功"+cardList;
			}
			
		}catch(Exception e) {
			String msg = e.getMessage();
			return msg;
		}
		return "非知指令";
	}

	@Override
	public boolean createRobotRoom(long accountID, int roomID, int game_type_index, int game_rule_index,
			int game_round,String nickName,String groupID,String groupName,int isInner) {
		return LogicRoomHandler.createRoomByBobot(accountID, roomID, game_type_index, game_rule_index, game_round,nickName,groupID,groupName,isInner);
	}
	
	

}
