package com.cai.game.mj.handler.yyzxz;

import java.util.ArrayList;
import java.util.List;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJGameLogic.AnalyseItem;
import com.cai.game.mj.MJType;

import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;

public class MJTable_YYZXZ extends AbstractMJTable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	final int[] xia_zi_fen = new int[getTablePlayerNumber()];
	
	//胡牌类型。大刀还是小刀
	int hu_type;

	public MJTable_YYZXZ() {
		super(MJType.GAME_TYPE_YYZXZ);
	}

	@Override
	protected void onInitTable() {
		_handler_dispath_card = new MJHandlerDispatchCard_YYZXZ();
		_handler_out_card_operate = new MJHandlerOutCardOperate_YYZXZ();
		_handler_gang = new MJHandlerGang_YYZXZ();
		_handler_chi_peng = new MJHandlerChiPeng_YYZXZ();
	}

	@Override
	protected boolean on_game_start() {
		_game_status = GameConstants.GS_MJ_PLAY;// 设置状态
		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		// gameStartResponse.setSiceIndex(rand);
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants.MAX_COUNT];
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
			xia_zi_fen[i] = 0;
		}
		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			// 只发自己的牌
			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);

				cards.addItem(hand_cards[i][j]);
			}
			// 回放数据
			GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			this.load_room_info_data(roomResponse);
			this.load_common_status(roomResponse);

			if (this._cur_round == 1) {
				// shuffle_players();
				this.load_player_info_data(roomResponse);
			}

			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setLeftCardCount(GRR._left_card_count);

			this.send_response_to_player(i, roomResponse);
		}

		////////////////////////////////////////////////////////////////////////////////////////////////
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		this.load_room_info_data(roomResponse);
		this.load_common_status(roomResponse);

		if (this._cur_round == 1) {
			// shuffle_players();
			this.load_player_info_data(roomResponse);
		}
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}
		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(GRR._left_card_count);
		GRR.add_room_response(roomResponse);

		// GRR._left_card_count=1;
		///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		this.exe_dispatch_card(_current_player, GameConstants.WIK_NULL, 0);

		return true;
	}
	
	/**
	 * //执行发牌 是否延迟
	 * 
	 * @param seat_index
	 * @param delay
	 * @return
	 */
	public boolean exe_dispatch_card(int seat_index, int type, int delay,int cardCount,boolean isGang) {
		this._handler_dispath_card.reset_card_count(cardCount,isGang);

		return super.exe_dispatch_card(seat_index, type, delay);
	}
	
	/**
	 * //执行发牌 是否延迟
	 * 
	 * @param seat_index
	 * @param delay
	 * @return
	 */
	public boolean exe_dispatch_card(int seat_index, int type, int delay) {
		return this.exe_dispatch_card(seat_index, type, delay,1,false);
	}
	
	
    boolean checkXiaPai(int[] cards_index,int cur_card){
    	return _logic.checkWanZi(cur_card) &&_logic.checkWanZiByIndex(cards_index);
    }

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {
		
		// 变量定义
			// cbCurrentCard一定不为0 !!!!!!!!!
			if (cur_card == 0)
				return GameConstants.WIK_NULL;
					
			// 设置变量
			List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
			// chiHuRight.set_empty();可以重复
		
			// 构造扑克
			int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
			for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
				cbCardIndexTemp[i] = cards_index[i];
			}
		
			// 插入扑克
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
			
			boolean hu = false;// 是否胡的标记--//七小对 将将胡 可能不是能胡牌的牌型 优先判断
			int qxd = _logic.is_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card);
			if (qxd != GameConstants.WIK_NULL) {
				chiHuRight.opr_or(qxd);
				hu = true;
			}
		
		
			// 分析扑克--通用的判断胡牌方法
			boolean bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weave_count, analyseItemArray, false);
		
			// 胡牌分析
			if (bValue == false) {
				// 不能胡的情况,有可能是七小对
				// 七小对牌 豪华七小对
				if (hu == false) {
					// chiHuRight.set_empty();
					return GameConstants.WIK_NULL;
				}
			}
		
			// 牌型分析
			for (int i = 0; i < analyseItemArray.size(); i++) {
				// 变量定义
				AnalyseItem analyseItem = analyseItemArray.get(i);
				/*
				 * // 判断番型
				 */
				// 碰碰和
				if (_logic.is_pengpeng_hu(analyseItem)) {
					chiHuRight.opr_or(GameConstants.CHR_HUNAN_PENGPENG_HU);
					hu = true;
					break;
				}
		
			}
		
			if (hu == true) {
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
					chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
				} else {
					chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
				}
				// 有大胡
				return GameConstants.WIK_CHI_HU;
			}
		
			return GameConstants.WIK_NULL;
	}
	
	
	
	/***
	 * 玩家出牌的动作检测--玩家出牌 响应判断,是否有吃碰杠补胡
	 * 
	 * @param seat_index
	 * @param card
	 * @return
	 */
	public boolean estimate_player_out_card_respond(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		// 用户状态
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {

			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		// 动作判断
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			if (_playerStatus[i].lock_huan_zhang() == false) {
				//// 碰牌判断
				action = _logic.check_peng_yyzxz(GRR._cards_index[i], card);
				if (action != 0) {
					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);
					bAroseAction = true;
				}
			}
			// 如果是自摸胡
			// 可以胡的情况 判断
			if (_playerStatus[i].is_chi_hu_round()) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						GameConstants.HU_CARD_TYPE_PAOHU, i);

				// 结果判断
				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
					bAroseAction = true;
				}
			}
		}


		if (bAroseAction) {
			_resume_player = _current_player;// 保存当前轮到的玩家
			_current_player = GameConstants.INVALID_SEAT;// 有需要操作的玩家。当前玩家为空
			_provide_player = seat_index;
		} else {
			return false;
		}
		return true;

	}
	
	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int xiaziFen = xia_zi_fen[seat_index];// 番数
		
		int wFanShu = 1;
		
		// 大刀小刀
		hu_type = GameConstants.CHR_HUNAN_XIADOU;
		countCardType(chr, seat_index);
		
		
		if(!(chr.opr_and(GameConstants.CHR_HUNAN_GANG_KAI)).is_empty()){
			wFanShu = 3;
			hu_type = GameConstants.CHR_HUNAN_DADOU;
		}else if(!(chr.opr_and(GameConstants.CHR_HUNAN_QIANG_GANG_HU)).is_empty()){
			wFanShu = 6;
			hu_type = GameConstants.CHR_HUNAN_DADOU;
		}else if(zimo){
			boolean isPeng = false;
			//TODO
			//碰牌后自摸算小刀
			if(isPeng){
				wFanShu = 2;
			}else{
				
				wFanShu = 3;
				hu_type = GameConstants.CHR_HUNAN_DADOU;
			}
		
		}else{
			//
			wFanShu = 1;
		}
		
		
		int lChiHuScore = (xiaziFen+wFanShu) * GameConstants.CELL_SCORE;// wFanShu*m_pGameServiceOption->lCellScore;

		// 统计
		if (zimo) {
			// 自摸
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				GRR._lost_fan_shu[i][seat_index] = wFanShu;
			}
		} else {// 点炮
			GRR._lost_fan_shu[provide_index][seat_index] = wFanShu;//

		}

		////////////////////////////////////////////////////// 自摸 算分
		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}

				int s = lChiHuScore;
				//////////////////////////////////////////////// 长沙麻将自摸算分//////////////////

				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;
				
				GRR._game_score[i] -= xia_zi_fen[i];
				GRR._game_score[seat_index] += xia_zi_fen[i];
			}
		}
		////////////////////////////////////////////////////// 点炮 算分
		else {
			int s = lChiHuScore;
			GRR._game_score[provide_index] -= s;
			GRR._game_score[seat_index] += s;
			
			GRR._game_score[provide_index] -= xia_zi_fen[provide_index];
			GRR._game_score[seat_index] += xia_zi_fen[provide_index];

			// 点炮的时候，删掉这张牌显示
			// GRR._cards_index[seat_index][_logic.switch_to_card_index(_provide_card)]--;

			GRR._chi_hu_rights[provide_index].opr_or(GameConstants.CHR_FANG_PAO);

		}

		GRR._provider[seat_index] = provide_index;
		// 设置变量

		_status_gang = false;
		_status_gang_hou_pao = false;

		_playerStatus[seat_index].clean_status();

		return;
	}

	@Override
	protected void set_result_describe() {
		int l;
		long type = 0;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			ChiHuRight chr = GRR._chi_hu_rights[i];
			String des = hu_type == GameConstants.CHR_HUNAN_XIADOU ? " 小刀": "大刀";
		

			l = GRR._chi_hu_rights[i].type_count;
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					
					if (type == GameConstants.CHR_HUNAN_GANG_KAI) {
						if (chr.is_mul(GameConstants.CHR_HUNAN_GANG_KAI)) {
							des += " 杠上开花*2";
						} else {
							des += " 杠上开花";
						}
					}
					if (type == GameConstants.CHR_HUNAN_QIANG_GANG_HU) {
						des += " 抢杠胡";
					}
					
					if (type == GameConstants.CHR_ZI_MO) {
						
							des += " 自摸";
					}
			
				} 
			}

			GRR._result_des[i] = des;
		}
	}

	@Override
	public boolean handler_requst_pao_qiang(Player player, int pao, int qiang) {
		return false;
	}

	@Override
	public boolean handler_requst_nao_zhuang(Player player, int nao) {
		return false;
	}

	@Override
	public boolean handler_requst_call_qiang_zhuang(int seat_index, int call_banker, int qiang_banker) {
		return false;
	}

	@Override
	public void init_other_param(Object... objects) {

	}

}
