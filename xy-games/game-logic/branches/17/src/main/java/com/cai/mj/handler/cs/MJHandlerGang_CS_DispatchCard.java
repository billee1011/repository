package com.cai.mj.handler.cs;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.MJGameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.CardsData;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.AddDiscardRunnable;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.RemoveOutCardRunnable;
import com.cai.mj.MJTable;
import com.cai.mj.handler.MJHandler;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerGang_CS_DispatchCard extends MJHandler {
	
	private int _seat_index;
//	private int _provide_player;
//	private int _center_card;
//	private int _action;//
//	private boolean _p;
//	private boolean _self;
	private boolean _double;
	
	private GangCardResult m_gangCardResult;
	private CardsData _gang_card_data;
	
	public MJHandlerGang_CS_DispatchCard(){
		_gang_card_data= new CardsData();
		m_gangCardResult = new GangCardResult();
	}
	
	
	
	public void reset_status(int seat_index,boolean d){
		_seat_index = seat_index;
		_double =d;
	}
	
	public void reset_status(int seat_index,int provide_player, int center_card,int action, boolean p, boolean self){
		_seat_index = seat_index;
//		_provide_player = provide_player;
//		_center_card = center_card;
//		_action = action;
//		_p = p;
//		_self = self;
	}
	
	
	@Override
	public void exe(MJTable table) {
		ChiHuRight chr = null;
		for (int i = 0; i < MJGameConstants.GAME_PLAYER; i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();
			chr = table.GRR._chi_hu_rights[i];
			chr.set_empty();
			table.operate_player_action(i, true);
		}
		table._playerStatus[_seat_index].set_card_status(MJGameConstants.CARD_STATUS_CS_GANG);
		table._playerStatus[_seat_index].chi_hu_round_valid();//可以胡了
		
		
		this._gang_card_data.clean_cards();
		
		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();
		
		// 设置变量
		table._out_card_data = MJGameConstants.INVALID_VALUE;
		table._out_card_player = MJGameConstants.INVALID_SEAT;
		table._current_player = _seat_index;// 轮到操作的人是自己

		table._provide_player = _seat_index;
			
		int bu_card;
		// 出牌响应判断
		
		// 从牌堆拿出2张牌
		for(int i=0; i < MJGameConstants.CS_GANG_DRAW_COUNT; i++){
			table._send_card_count++;
			if (table.is_mj_type(MJGameConstants.GAME_TYPE_HZ)) {
				bu_card = table._repertory_card_zz[table._all_card_len-table.GRR._left_card_count];
			} else {
				bu_card = table._repertory_card_cs[table._all_card_len-table.GRR._left_card_count];
			}
			if(table.DEBUG_CARDS_MODE){
				if(i==0)bu_card=0x21;
				if(i==1)bu_card=0x09;
			}
			--table.GRR._left_card_count;
			this._gang_card_data.add_card(bu_card);
		}
		
		//显示两张牌
		table.operate_out_card(_seat_index, MJGameConstants.CS_GANG_DRAW_COUNT, this._gang_card_data.get_cards(),MJGameConstants.OUT_CARD_TYPE_LEFT,MJGameConstants.INVALID_SEAT);
		
		boolean has_action =false;
		
		//显示玩家对这两张牌的操作
		for(int i=0; i < MJGameConstants.CS_GANG_DRAW_COUNT; i++){
			boolean 	bAroseAction = false;
			bu_card = this._gang_card_data.get_card(i);
			
			for(int k=0; k < MJGameConstants.GAME_PLAYER;k++){
				//自己只有杠和 自摸
				if(k==_seat_index){
					chr = table.GRR._chi_hu_rights[k];
					
					int action = table.analyse_chi_hu_card(table.GRR._cards_index[k],
							table.GRR._weave_items[k], table.GRR._weave_count[k], bu_card, chr,MJGameConstants.HU_CARD_TYPE_ZIMO);//自摸
					if(action != MJGameConstants.WIK_NULL){
						//添加动作
						if(_double){
							chr.opr_or(MJGameConstants.CHR_SHUANG_GANG_KAI);
						}else{
							chr.opr_or(MJGameConstants.CHR_GANG_KAI);
						}
						
						curPlayerStatus.add_action(MJGameConstants.WIK_ZI_MO);
						curPlayerStatus.add_zi_mo(bu_card,k);
						bAroseAction=true;
					}
					
					// 如果牌堆还有牌，判断能不能杠
					if (table.GRR._left_card_count > 2) {
						// 加到手牌
						table.GRR._cards_index[k][table._logic.switch_to_card_index(bu_card)]++;

						int cbActionMask=table._logic.analyse_gang_card_all(table.GRR._cards_index[k],
								table.GRR._weave_items[k], table.GRR._weave_count[k], m_gangCardResult,true);
						
						table.GRR._cards_index[k][table._logic.switch_to_card_index(bu_card)]--;
						
						if(cbActionMask!=MJGameConstants.WIK_NULL){//有杠
							for(int gc= 0; gc < m_gangCardResult.cbCardCount; gc++){
//								if(gangCardResult.cbCardData[gc]==bu_card){
//									//只有杠的这张才算
									//把可以杠的这张牌去掉。看是不是听牌
									int bu_index = table._logic.switch_to_card_index(m_gangCardResult.cbCardData[gc]);
									int save_count = table.GRR._cards_index[k][bu_index];
									table.GRR._cards_index[k][bu_index]=0;
									
									
									boolean is_ting = table.is_cs_ting_card(table.GRR._cards_index[k],
											table.GRR._weave_items[k], table.GRR._weave_count[k]);
									
									//把牌加回来
									table.GRR._cards_index[k][bu_index] = save_count;
									
									if(is_ting){
										curPlayerStatus.add_action(MJGameConstants.WIK_GANG);//听牌的时候可以杠
										curPlayerStatus.add_gang(m_gangCardResult.cbCardData[gc], k, m_gangCardResult.isPublic[gc]);
										bAroseAction=true;
									}
//								}
							}
							
							
						}
					}
					
				}else{
					int chi_seat_index = (_seat_index+1)%MJGameConstants.GAME_PLAYER;
					if(k==chi_seat_index){
						bAroseAction = table.estimate_gang_cs_respond(k, _seat_index,bu_card,_double,true);//, EstimatKind.EstimatKind_OutCard
					}else{
						bAroseAction = table.estimate_gang_cs_respond(k, _seat_index,bu_card,_double,false);//, EstimatKind.EstimatKind_OutCard
					}
					
				}
				// 出牌响应判断
				
				// 如果没有需要操作的玩家，派发扑克
				if (bAroseAction == true) {
					has_action= true;
				
				} 
			}
		}
		
		if(has_action==false){
			//_status_cs_gang = false;
			//添加到牌堆
//			for(int i=0; i < this._gang_card_data.get_card_count(); i++){
//				GRR._discard_count[cur_player]++;
//				GRR._discard_cards[cur_player][GRR._discard_count[cur_player] - 1] = this._gang_card_data.get_card(i);
//			}
//			
//			this.runnable_add_discard(cur_player, this._gang_card_data.get_card_count(), this._gang_card_data.get_card_count());
			
			table._provide_player =  MJGameConstants.INVALID_SEAT;
			table._out_card_player = _seat_index;
			
			GameSchedule.put(new RemoveOutCardRunnable(table.getRoom_id(), _seat_index,MJGameConstants.OUT_CARD_TYPE_LEFT),
					MJGameConstants.GANG_CARD_CS_DELAY, TimeUnit.MILLISECONDS);
			GameSchedule.put(new AddDiscardRunnable(table.getRoom_id(), _seat_index, this._gang_card_data.get_card_count(),this._gang_card_data.get_cards(),true),
					MJGameConstants.GANG_CARD_CS_DELAY, TimeUnit.MILLISECONDS);
			
	
			//继续发牌
			table._current_player = (_seat_index+1)%MJGameConstants.GAME_PLAYER;
			
			table.exe_dispatch_card(table._current_player,MJGameConstants.WIK_NULL, MJGameConstants.GANG_CARD_CS_DELAY);

		}else{
			table._provide_player =  _seat_index;
			//玩家有操作
			for (int i = 0; i < MJGameConstants.GAME_PLAYER; i++) {
				if(table._playerStatus[i].has_action()){
					table.operate_player_action(i, false);
				}
			}
		}
		
	}
	
	/***
	 * //用户操作
	 * 
	 * @param seat_index
	 * @param operate_code
	 * @param operate_card
	 * @return
	 */
	@Override
	public boolean handler_operate_card(MJTable table,int seat_index, int operate_code, int operate_card){
		PlayerStatus playerStatus = table._playerStatus[seat_index];
		
		 // 是否已经响应
		if(playerStatus.has_action()==false){
			table.log_player_error(seat_index, "出牌,玩家操作已失效");
			return true;
		}
		
		 // 是否已经响应
		if(playerStatus.is_respone()){
			table.log_player_error(seat_index, "出牌,玩家已操作");
			return true;
		}
		
		if ((operate_code != MJGameConstants.WIK_NULL) && playerStatus.has_action_by_code(operate_code) == false)// 没有这个操作动作
		{
			table.log_player_error(seat_index, "出牌操作,没有动作");
			return true;
		}

		

		//玩家的操作
		playerStatus.operate(operate_code,operate_card);
		
		if(operate_code == MJGameConstants.WIK_CHI_HU){
			table.GRR._chi_hu_rights[seat_index].set_valid(true);//胡牌生效
			//效果
			if(table._playerStatus[_seat_index].has_zi_mo()==false || table._playerStatus[_seat_index].is_respone()==true){
				//table.process_chi_hu_player_operate(seat_index, operate_card,false);
			}
			
		}else if(operate_code == MJGameConstants.WIK_NULL){
			if(table._playerStatus[seat_index].has_chi_hu()){
				table._playerStatus[seat_index].chi_hu_round_invalid();//这一轮就不能吃胡了没过牌之前都不能胡
			}
		}
		
		// 变量定义 优先级最高操作的玩家和操作
		int target_player = seat_index;
		int target_action = operate_code;
		int target_p = 0;
		for (int p = 0; p < MJGameConstants.GAME_PLAYER; p++) {
			int i = (_seat_index + p) % MJGameConstants.GAME_PLAYER;
			if (i == target_player) {
				target_p = MJGameConstants.GAME_PLAYER - p;
			}
		}
				
		if(operate_code != MJGameConstants.WIK_ZI_MO){
			// 吃胡等待 因为胡牌的等级是一样的，可以一炮多响，看看是不是还有能胡的
			for (int i = 0; i < MJGameConstants.GAME_PLAYER; i++) {
				if ((table._playerStatus[i].is_respone()== false) && (table._playerStatus[i].has_chi_hu()))
					return false;
			}
			
			
			// 执行判断
			
			for (int i = 0; i <MJGameConstants.GAME_PLAYER; i++){
				int p =(_seat_index+i) % MJGameConstants.GAME_PLAYER;
				// 获取动作
				int cbUserActionRank =0;
				// 优先级别
				int cbTargetActionRank =0;
				
				if(table._playerStatus[p].has_action()){
					if(table._playerStatus[p].is_respone()){
						//获取已经执行的动作的优先级
						cbUserActionRank =  table._logic.get_action_rank(table._playerStatus[p].get_perform())+MJGameConstants.GAME_PLAYER-i;
					}else{
						//获取最大的动作的优先级
						cbUserActionRank =  table._logic.get_action_list_rank(table._playerStatus[p]._action_count,table._playerStatus[p]._action)+MJGameConstants.GAME_PLAYER-i;
					}
					
					if(table._playerStatus[target_player].is_respone()){
						// 优先级别
						cbTargetActionRank = table._logic.get_action_rank(table._playerStatus[target_player].get_perform())+target_p;
					}else{
						//获取最大的动作的优先级
						cbTargetActionRank =  table._logic.get_action_list_rank(table._playerStatus[target_player]._action_count,table._playerStatus[target_player]._action)+target_p;
					}
					
	
					// 动作判断 优先级最高的人和动作
					if (cbUserActionRank > cbTargetActionRank) {
						target_player = p;//最高级别人
						target_action = table._playerStatus[i].get_perform();
						target_p = MJGameConstants.GAME_PLAYER-i;
					}
				}
			}
			// 优先级最高的人还没操作
			if (table._playerStatus[target_player].is_respone()== false)
				return true;
		}

		// 变量定义
		int target_card = table._playerStatus[target_player]._operate_card;

		
		
		// 删除扑克
		switch (target_action) {
				case MJGameConstants.WIK_LEFT: // 上牌操作
				{
					// 删除扑克
					int cbRemoveCard[] = new int[] { target_card + 1, target_card + 2 };
					if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
						table.log_player_error(seat_index, "吃牌删除出错");
						return false;
					}
					
					this.exe_chi_peng(table, target_player, target_action, target_card);
				}
				break;
			case MJGameConstants.WIK_RIGHT: // 上牌操作
				{
					// 删除扑克
					int cbRemoveCard[] = new int[] { target_card - 1, target_card - 2 };
		
					if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
						table.log_player_error(seat_index, "吃牌删除出错");
						return false;
					}
					this.exe_chi_peng(table, target_player, target_action, target_card);
				}
				break;
			case MJGameConstants.WIK_CENTER: // 上牌操作
				{
					// 删除扑克
					int cbRemoveCard[] = new int[] { target_card - 1, target_card + 1 };
					if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
						table.log_player_error(seat_index, "吃牌删除出错");
						return false;
					}
					this.exe_chi_peng(table, target_player, target_action, target_card);
				}
				break;
			case MJGameConstants.WIK_PENG: // 碰牌操作
				{
					// 删除扑克
					int cbRemoveCard[] = new int[] { target_card, target_card };
					if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
						table.log_player_error(seat_index, "碰牌删除出错");
						return false;
					}
		
					this.exe_chi_peng(table, target_player, target_action, target_card);
				}
				break;
			case MJGameConstants.WIK_BU_ZHNAG: // 补张牌操作
			case MJGameConstants.WIK_GANG: // 杠牌操作
			{
				
				//删掉出来的那两张张牌
				table.operate_out_card(this._seat_index, 0, null,MJGameConstants.OUT_CARD_TYPE_LEFT,MJGameConstants.INVALID_SEAT);
				
				int add_card=-1;
				boolean card_check=true;
				for(int i=0; i < MJGameConstants.CS_GANG_DRAW_COUNT; i++){
					//是摸出来的牌
					if(card_check && (target_card == this._gang_card_data.get_card(i))){
						card_check=false;
					}else{
						add_card  = this._gang_card_data.get_card(i);
					}
				}
				if(add_card==-1){
					table.runnable_add_discard(this._seat_index, 2, this._gang_card_data.get_cards(),true);
				}else{
					table.runnable_add_discard(this._seat_index, 1, new int[]{add_card},true);
				}
				
				for (int i = 0; i < MJGameConstants.GAME_PLAYER; i++) {
					table._playerStatus[i].clean_action();
					table._playerStatus[i].clean_status();
					
					table.operate_player_action(i, true);
				}
				
				if(_seat_index == target_player){
					for(int i= 0; i < m_gangCardResult.cbCardCount; i++){
						if(target_card == m_gangCardResult.cbCardData[i]){
							//是否有抢杠胡
							table.exe_gang(_seat_index, _seat_index, target_card, operate_code, m_gangCardResult.type[i], true,true);
							return true;
						}
					}
				}else{
					table.exe_gang(target_player, _seat_index, target_card, target_action, MJGameConstants.GANG_TYPE_JIE_GANG, false,false);
				}
				
				return true;
			}
			case  MJGameConstants.WIK_NULL:{

				//删掉出来的那张牌
				table.operate_out_card(this._seat_index, 0, null,MJGameConstants.OUT_CARD_TYPE_LEFT,MJGameConstants.INVALID_SEAT);
				
				//剩下的牌放到牌堆
				table.runnable_add_discard(this._seat_index, 2, this._gang_card_data.get_cards(),true);
				
				// 用户切换
				table._current_player = (_seat_index + MJGameConstants.GAME_PLAYER + 1) % MJGameConstants.GAME_PLAYER;
				//发牌
				table.exe_dispatch_card(table._current_player,MJGameConstants.WIK_NULL,0);
				
				return true;
			}
			case MJGameConstants.WIK_ZI_MO: // 自摸
			{
				
				//删掉出来的那两张张牌
				//table.operate_out_card(this._seat_index, 0, null,MJGameConstants.OUT_CARD_TYPE_LEFT,MJGameConstants.INVALID_SEAT);
				//table.runnable_add_discard(this._seat_index, 2, this._gang_card_data.get_cards());
				
				for(int i=0; i<MJGameConstants.GAME_PLAYER;i++){
					if(i == _seat_index){
						table.GRR._chi_hu_rights[i].set_valid(true);
					}else{
						table.GRR._chi_hu_rights[i].set_valid(false);
					}
				}
				
				// 下局胡牌的是庄家
				table.set_niao_card(table.GRR._banker_player,MJGameConstants.INVALID_VALUE,true,0);// 结束后设置鸟牌MJGameConstants.INVALID_VALUE
				// 吃牌权位
				if (table._out_card_count == 0) {//天胡
					table._provide_player = _seat_index;
					table._provide_card = target_card;
				}
	
				// 检查自摸
				int cbWeaveItemCount = table.GRR._weave_count[_seat_index];
				WeaveItem pWeaveItem[] = table.GRR._weave_items[_seat_index];
				
				//有两张牌,所以要重新算一次
				table.GRR._chi_hu_kind[_seat_index] = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], pWeaveItem,
						cbWeaveItemCount, target_card, table.GRR._chi_hu_rights[_seat_index],MJGameConstants.HU_CARD_TYPE_ZIMO);
	
				if(table.GRR._chi_hu_kind[_seat_index]!=0)
				{
					table.GRR._chi_hu_rights[_seat_index].set_valid(true);
					// 结束信息
					table.GRR._chi_hu_card[_seat_index] = target_card;
					
					if(_double){
						table.GRR._chi_hu_rights[_seat_index].opr_or(MJGameConstants.CHR_SHUANG_GANG_KAI);
					}else{
						table.GRR._chi_hu_rights[_seat_index].opr_or(MJGameConstants.CHR_GANG_KAI);
					}
					
					table.process_chi_hu_player_operate(_seat_index, target_card,false);
					table.process_chi_hu_player_score(_seat_index,_seat_index,target_card, true);
					
					// 记录
					table._player_result.da_hu_zi_mo[_seat_index]++;
		
				}
				for (int i = 0; i < MJGameConstants.GAME_PLAYER; i++) {
					//table._playerStatus[i].clean_action();
					table._playerStatus[i].clean_status();
					
					table.operate_player_action(i, true);
				}
				table._banker_select = _seat_index;
				GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._banker_select, MJGameConstants.Game_End_NORMAL),
						MJGameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);
	
				return true;
			}
			case MJGameConstants.WIK_CHI_HU: //胡
			{
				//删掉出来的那两张张牌
				//table.operate_out_card(this._seat_index, 0, null,MJGameConstants.OUT_CARD_TYPE_LEFT,MJGameConstants.INVALID_SEAT);
				//table.runnable_add_discard(this._seat_index, 2, this._gang_card_data.get_cards());
				//放炮
				table.GRR._chi_hu_rights[_seat_index].opr_or(MJGameConstants.CHR_FANG_PAO);
				
				int jie_pao_count = 0;
				for(int i=0; i<MJGameConstants.GAME_PLAYER;i++){
					if((i == _seat_index) ||(table.GRR._chi_hu_rights[i].is_valid()==false )){
						continue;
					}
					jie_pao_count++;
				}
				if(jie_pao_count>1){
					table._banker_select = _seat_index;
				}else{
					table._banker_select = target_player;
				}
				
				table.set_niao_card(table.GRR._banker_player,MJGameConstants.INVALID_VALUE,true,0);
				for(int i=0; i<MJGameConstants.GAME_PLAYER;i++){
					if((i == _seat_index) ||(table.GRR._chi_hu_rights[i].is_valid()==false )){
						continue;
					}
					
					int cbWeaveItemCount = table.GRR._weave_count[i];
					WeaveItem pWeaveItem[] = table.GRR._weave_items[i];
		
					//有两张牌,所以要重新算一次
					table.GRR._chi_hu_kind[i] = table.analyse_chi_hu_card(table.GRR._cards_index[i], pWeaveItem,
							cbWeaveItemCount, table._playerStatus[i]._operate_card, table.GRR._chi_hu_rights[i],MJGameConstants.HU_CARD_TYPE_PAOHU);
			
					if(table.GRR._chi_hu_kind[i]!=0)
					{
						table.GRR._chi_hu_rights[i].set_valid(true);
						if(_double){
							table.GRR._chi_hu_rights[i].opr_or(MJGameConstants.CHR_SHUANG_GANG_SHANG_PAO);
						}else{
							table.GRR._chi_hu_rights[i].opr_or(MJGameConstants.CHR_GANG_SHANG_PAO);
						}
						table.process_chi_hu_player_operate(i, table._playerStatus[i]._operate_card,false);
						table.process_chi_hu_player_score(i,_seat_index, table._playerStatus[i]._operate_card,false);
						
						// 记录
						table._player_result.da_hu_jie_pao[i]++;
						table._player_result.da_hu_dian_pao[_seat_index]++;
						
					}
				}
				for (int i = 0; i < MJGameConstants.GAME_PLAYER; i++) {
					//table._playerStatus[i].clean_action();
					table._playerStatus[i].clean_status();
					
					table.operate_player_action(i, true);
				}
				GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._banker_select, MJGameConstants.Game_End_NORMAL),
						MJGameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);
				return true;
			}
			default:
				return false;
		}
		return true;		
	}
	
	
	
	@Override
	public boolean handler_player_be_in_room(MJTable table,int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);
		
		
		// 游戏变量
		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(_seat_index);
		tableResponse.setCellScore(0);

		// 状态变量
		tableResponse.setActionCard(0);
		//tableResponse.setActionMask((_response[seat_index] == false) ? _player_action[seat_index] : MJGameConstants.WIK_NULL);

		// 历史记录
		tableResponse.setOutCardData(0);
		tableResponse.setOutCardPlayer(0);

		for (int i = 0; i < MJGameConstants.GAME_PLAYER; i++) {
			tableResponse.addTrustee(false);// 是否托管
			// 剩余牌数
			tableResponse.addDiscardCount(table.GRR._discard_count[i]);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < 55; j++) {
				int_array.addItem(table.GRR._discard_cards[i][j]);
			}
			tableResponse.addDiscardCards(int_array);

			// 组合扑克
			tableResponse.addWeaveCount(table.GRR._weave_count[i]);
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < MJGameConstants.MAX_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			//
			tableResponse.addWinnerOrder(0);

			tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
			
		}

		// 数据
		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[MJGameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);


		for (int i = 0; i < MJGameConstants.MAX_COUNT; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);
		
		//效果
		table.operate_effect_action(_seat_index,MJGameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[]{MJGameConstants.WIK_GANG}, 1,seat_index);
		
		//出牌
		table.operate_out_card(_seat_index, 2, this._gang_card_data.get_cards(),MJGameConstants.OUT_CARD_TYPE_LEFT,seat_index);
		
		
		if(table._playerStatus[seat_index].has_action() && table._playerStatus[seat_index].is_respone()==false){
			table.operate_player_action(seat_index, false);
		}
	
		
		return true;
	}
	
	private void exe_chi_peng(MJTable table,int target_player,int target_action,int target_card){
		
		// 组合扑克
		int wIndex = table.GRR._weave_count[target_player]++;
		table.GRR._weave_items[target_player][wIndex].public_card = 1;
		table.GRR._weave_items[target_player][wIndex].center_card = target_card;
		table.GRR._weave_items[target_player][wIndex].weave_kind = target_action;
		table.GRR._weave_items[target_player][wIndex].provide_player = _seat_index;
		
		//删掉出来的那两张张牌
		table.operate_out_card(this._seat_index, 0, null,MJGameConstants.OUT_CARD_TYPE_LEFT,MJGameConstants.INVALID_SEAT);
		
		int add_card=0;
		for(int i=0; i < MJGameConstants.CS_GANG_DRAW_COUNT; i++){
			add_card = this._gang_card_data.get_card(i);
			if(target_card != add_card){
				break;
			}
		}
		
		table.runnable_add_discard(this._seat_index, 1, new int[]{add_card},true);
		
		
		// 设置用户
		table._current_player = target_player;

		//效果
		table.operate_effect_action(target_player,MJGameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[]{target_action}, 1,MJGameConstants.INVALID_SEAT);
		
		//刷新手牌包括组合
		int cards[]= new int[MJGameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[target_player], cards);
		table.operate_player_cards(target_player, hand_card_count, cards, table.GRR._weave_count[target_player], table.GRR._weave_items[target_player]);
		
		for (int i = 0; i < MJGameConstants.GAME_PLAYER; i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();
			
			table.operate_player_action(i, true);
		}
		
		table.exe_chi_peng(target_player,target_action);
		
	}

}
