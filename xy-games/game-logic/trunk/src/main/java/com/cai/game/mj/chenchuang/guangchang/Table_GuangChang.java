package com.cai.game.mj.chenchuang.guangchang;

import java.util.Arrays;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.Constants_GuangChang;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJType;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.service.PlayerServiceImpl;

/**
 * chenchuang
 * 广昌麻将
 */
public class Table_GuangChang extends AbstractMJTable {

	private static final long serialVersionUID = 1L;
	
	public int gen_zhuang_card; // 为-1 时本局没有更庄,为0时庄家还未出牌 ,-2表示跟庄了
	public boolean first_round; //是否第一圈
	public int last_out_player_4_banker;
	
	public int[] temp_pai_xing_score;//存储玩家的牌型分
	public int[] cannot_pai_xing_score;//玩家点炮小于该分数不能胡

	public int[] player_out_card_count; //每个玩家的出牌数量
	

	public Table_GuangChang() {
		super(MJType.GAME_TYPE_MJ_GUANG_CHANG);
	}

	@Override
	protected void onInitTable() {
		_handler_chi_peng = new HandlerChiPeng_GuangChang();
		_handler_dispath_card = new HandlerDispatchCard_GuangChang();
		_handler_gang = new HandlerGang_GuangChang();
		_handler_out_card_operate = new HandlerOutCardOperate_GuangChang();
	}



	/**
	 * 洗完牌执行开始
	 */
	@Override
	protected boolean on_game_start() {
		//初始化属性
		temp_pai_xing_score = new int[getTablePlayerNumber()];
		cannot_pai_xing_score = new int[getTablePlayerNumber()];
		player_out_card_count = new int[getTablePlayerNumber()];
		gen_zhuang_card = -3;
		
		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants.MAX_COUNT];

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}
		//发送给玩家手牌
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				if (_logic.is_magic_card(hand_cards[i][j])) {
					hand_cards[i][j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
                }
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			load_room_info_data(roomResponse);
			load_common_status(roomResponse);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setCurrentPlayer(_current_player == GameConstants.INVALID_SEAT ? _resume_player : _current_player);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			roomResponse.setGameStatus(_game_status);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			send_response_to_player(i, roomResponse);
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		load_room_info_data(roomResponse);
		load_common_status(roomResponse);
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

		// 检测听牌
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i]._hu_card_count = get_ting_card(_playerStatus[i]._hu_cards, GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i],
					i);
			if (_playerStatus[i]._hu_card_count > 0) {
				operate_chi_hu_cards(i, _playerStatus[i]._hu_card_count, _playerStatus[i]._hu_cards);
			}
		}

		//发第一张牌
		exe_dispatch_card(_current_player, Constants_GuangChang.HU_CARD_TYPE_TIAN_HU, GameConstants.DELAY_SEND_CARD_DELAY);

		return true;
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {
		if (card_type == Constants_GuangChang.HU_CARD_TYPE_ZI_MO) {
			chiHuRight.opr_or(Constants_GuangChang.CHR_ZI_MO);//自摸
		} else if (card_type == Constants_GuangChang.HU_CARD_TYPE_QIANG_GANG) {
			chiHuRight.opr_or(Constants_GuangChang.CHR_QIANG_GANG);//抢杠胡
		} else if (card_type == Constants_GuangChang.HU_CARD_TYPE_DIAN_PAO) {
			chiHuRight.opr_or(Constants_GuangChang.CHR_DIAN_PAO);//点炮胡
		} else if (card_type == Constants_GuangChang.HU_CARD_TYPE_GANG_KAI_HUA) {
			chiHuRight.opr_or(Constants_GuangChang.CHR_GANG_KAI_HUA);//杠开花
		}
		//拷贝把当前牌加入手牌
		int cur_card_index = _logic.switch_to_card_index(cur_card);
		int[] temp_cards_index = Arrays.copyOf(cards_index, cards_index.length);
		temp_cards_index[cur_card_index]++;
		
		//十三烂--
		int shiSanLan = isShiSanLan(temp_cards_index, weave_count);
		
		//小七对--
		int xiao_qi_dui = is_xiao_qi_dui(temp_cards_index, weave_count);
		
		//幺九--
		boolean is_yao_jiu = false;
		if(has_rule(Constants_GuangChang.GAME_RULE_YES_QUAN_YAO_JIU))
			is_yao_jiu = is_yao_jiu(temp_cards_index, weaveItems, weave_count);
		
		//潇洒小七对--
		boolean is_xiao_sha =false;
		if(xiao_qi_dui	!= -1)
			is_xiao_sha = is_xiao_sha(temp_cards_index, weave_count);
		
		//清一色--
		boolean is_qing_yi_se = _logic.is_qing_yi_se(cards_index, weaveItems, weave_count, cur_card);
		
		//是否是风吃类胡牌牌型;
		boolean analyse_win_by_cards_index = AnalyseCardUtil.analyse_feng_chi_by_cards_index(cards_index, cur_card_index, null, 0);
		
		//暗藏大七对(没有碰杠操作形成的大七对不包括暗杠)
		boolean is_ac_da_qi = is_no_pg(weaveItems, weave_count);
		
		if(!(shiSanLan != 0 || xiao_qi_dui != -1 || is_qing_yi_se || analyse_win_by_cards_index))
			return GameConstants.WIK_NULL;
		
		//判断全求人
		boolean is_quan_qiu_ren = false;
		int tmp_card_count = _logic.get_card_count_by_index(temp_cards_index);
	    if (tmp_card_count == 2 && card_type != Constants_GuangChang.HU_CARD_TYPE_ZI_MO && card_type != Constants_GuangChang.HU_CARD_TYPE_GANG_KAI_HUA && analyse_win_by_cards_index) {
	    	is_quan_qiu_ren = true;
	    }
	    
	    //大七对(碰碰胡)
	    boolean peng_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index(cards_index, cur_card_index, null, 0);
		
		//断幺九
		boolean is_duan_yao_jiu = false;
		if(has_rule(Constants_GuangChang.GAME_RULE_YES_DUAN_YAO_JIU))
			is_duan_yao_jiu = is_duan_yao_jiu(temp_cards_index, weaveItems, weave_count);
		
		/*//全求人
		if(is_quan_qiu_ren)
			chiHuRight.opr_or(Constants_GuangChang.CHR_QUAN_QIU_REN);
		//十三烂
		if(shiSanLan == 2)
			chiHuRight.opr_or(Constants_GuangChang.CHR_QX_SHI_SAN_LAN);
		else if(shiSanLan == 1)
			chiHuRight.opr_or(Constants_GuangChang.CHR_SHI_SAN_LAN);
		
		//大七对
		if(is_yao_jiu && peng_hu)
			chiHuRight.opr_or(Constants_GuangChang.CHR_YJ_DA_QI_DUI);
		if(is_ac_da_qi && peng_hu)
			chiHuRight.opr_or(Constants_GuangChang.CHR_AC_DA_QI_DUI);
		if(peng_hu && !(is_yao_jiu || is_ac_da_qi))
			chiHuRight.opr_or(Constants_GuangChang.CHR_DA_QI_DUI);
		
		//幺九
		if(is_yao_jiu && xiao_qi_dui != -1)
			chiHuRight.opr_or(Constants_GuangChang.CHR_YJ_XIAO_QI_DUI);
		if(is_yao_jiu && is_quan_qiu_ren)
			chiHuRight.opr_or(Constants_GuangChang.CHR_YJ_QUAN_QIU_REN);
		if(is_yao_jiu && !(is_quan_qiu_ren || xiao_qi_dui != -1 || peng_hu))
			chiHuRight.opr_or(Constants_GuangChang.CHR_YAO_JIU);
		
		//断幺九
		if(is_duan_yao_jiu)
			chiHuRight.opr_or(Constants_GuangChang.CHR_DUAN_YAO_JIU);
		
		//小七对
		if(xiao_qi_dui == 3)
			chiHuRight.opr_or(Constants_GuangChang.CHR_3HH_XIAO_QI_DUI);
		if(xiao_qi_dui == 2)
			chiHuRight.opr_or(Constants_GuangChang.CHR_2HH_XIAO_QI_DUI);
		if(xiao_qi_dui == 1)
			chiHuRight.opr_or(Constants_GuangChang.CHR_HH_XIAO_QI_DUI);
		if(is_xiao_sha)
			chiHuRight.opr_or(Constants_GuangChang.CHR_XS_XIAO_QI_DUI);
		if(xiao_qi_dui == 0 && !(is_xiao_sha ||is_yao_jiu))
			chiHuRight.opr_or(Constants_GuangChang.CHR_XAIO_QI_DUI);
		
		//清一色
		if(is_qing_yi_se && is_no_p(weaveItems, weave_count) && analyse_win_by_cards_index)
			chiHuRight.opr_or(Constants_GuangChang.CHR_WP_QING_YI_SE);
		else if(is_qing_yi_se)
			chiHuRight.opr_or(Constants_GuangChang.CHR_QING_YI_SE);*/
		boolean is_big = true;
		
		{
			if(xiao_qi_dui == 3)
				chiHuRight.opr_or(Constants_GuangChang.CHR_3HH_XIAO_QI_DUI);
			else if(xiao_qi_dui == 2)
				chiHuRight.opr_or(Constants_GuangChang.CHR_2HH_XIAO_QI_DUI);
			else if(is_yao_jiu && is_quan_qiu_ren)
				chiHuRight.opr_or(Constants_GuangChang.CHR_YJ_QUAN_QIU_REN);
			else if(is_xiao_sha)
				chiHuRight.opr_or(Constants_GuangChang.CHR_XS_XIAO_QI_DUI);
			else if(xiao_qi_dui == 1)
				chiHuRight.opr_or(Constants_GuangChang.CHR_HH_XIAO_QI_DUI);
			else if(is_yao_jiu && xiao_qi_dui != -1)
				chiHuRight.opr_or(Constants_GuangChang.CHR_YJ_XIAO_QI_DUI);
			else if(is_ac_da_qi && peng_hu)
				chiHuRight.opr_or(Constants_GuangChang.CHR_AC_DA_QI_DUI);
			else if(is_yao_jiu && peng_hu)
				chiHuRight.opr_or(Constants_GuangChang.CHR_YJ_DA_QI_DUI);
			else if(is_quan_qiu_ren)
				chiHuRight.opr_or(Constants_GuangChang.CHR_QUAN_QIU_REN);
			else if(xiao_qi_dui == 0 && !(is_xiao_sha ||is_yao_jiu))
				chiHuRight.opr_or(Constants_GuangChang.CHR_XAIO_QI_DUI);
			else if(is_duan_yao_jiu)
				chiHuRight.opr_or(Constants_GuangChang.CHR_DUAN_YAO_JIU);
			else if(is_yao_jiu && !(is_quan_qiu_ren || xiao_qi_dui != -1 || peng_hu))
				chiHuRight.opr_or(Constants_GuangChang.CHR_YAO_JIU);
			else if(peng_hu && !(is_yao_jiu || is_ac_da_qi))
				chiHuRight.opr_or(Constants_GuangChang.CHR_DA_QI_DUI);
			else if(shiSanLan == 2)
				chiHuRight.opr_or(Constants_GuangChang.CHR_QX_SHI_SAN_LAN);
			else if(shiSanLan == 1)
				chiHuRight.opr_or(Constants_GuangChang.CHR_SHI_SAN_LAN);
			else
				is_big = false;
			
			if(is_big){
				if(is_qing_yi_se)
				chiHuRight.opr_or(Constants_GuangChang.CHR_QING_YI_SE);
			}else{
				if(is_qing_yi_se && is_no_p(weaveItems, weave_count) && analyse_win_by_cards_index)
					chiHuRight.opr_or(Constants_GuangChang.CHR_WP_QING_YI_SE);
				else if(is_qing_yi_se && !analyse_win_by_cards_index){
					chiHuRight.opr_or(Constants_GuangChang.CHR_QING_YI_SE_JIA_HU);
				}else if(is_qing_yi_se && analyse_win_by_cards_index)
					chiHuRight.opr_or(Constants_GuangChang.CHR_QING_YI_SE);
			}
			
			if(card_type == Constants_GuangChang.HU_CARD_TYPE_QIANG_GANG || 
					card_type == Constants_GuangChang.HU_CARD_TYPE_GANG_SHANG_PAO ||
					card_type == Constants_GuangChang.HU_CARD_TYPE_DIAN_PAO)
				temp_pai_xing_score[_seat_index] = get_pai_xing_score(chiHuRight, _seat_index);
		}
		
		return GameConstants.WIK_CHI_HU;
	}

	

	/**
	 * 判断抢杠胡
	 * @return
	 */
	public boolean estimate_gang_respond(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		// 动作判断
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			if (playerStatus.isAbandoned()) // 已经弃胡
				continue;

			// 吃胡判断
			ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
			chr.set_empty();
			int cbWeaveCount = GRR._weave_count[i];
			action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
					Constants_GuangChang.HU_CARD_TYPE_QIANG_GANG, i);
			// 可以胡的情况 判断 ,强杠胡不用判断是否过圈
			if (action != GameConstants.WIK_NULL && (playerStatus.is_chi_hu_round() || temp_pai_xing_score[i] > cannot_pai_xing_score[i])) {
				// 结果判断
				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
					bAroseAction = true;
				}
			}
		}

		if (bAroseAction == true) {
			_provide_player = seat_index;// 谁打的牌
			_provide_card = card;// 打的什么牌
			_resume_player = _current_player;// 保存当前轮到的玩家
			_current_player = GameConstants.INVALID_SEAT;// 有需要操作的玩家。当前玩家为空
		}

		return bAroseAction;
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		int wFanShu = 1;// 番数
		if(!chr.opr_and(Constants_GuangChang.CHR_QING_YI_SE).is_empty());
			wFanShu *= 2;
			
		countCardType(chr, seat_index);

		/////////////////////////////////////////////// 算番//////////////////////////
		// 统计
		if (zimo) {
			// 自摸
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				GRR._lost_fan_shu[i][seat_index] = wFanShu;
			}
		} else {
			GRR._lost_fan_shu[provide_index][seat_index] = wFanShu * 2;
		}
		
		////////////////////////////////////////////////////// 自摸 算分
		int di_score = getRoomScore();
		if(!chr.opr_and(Constants_GuangChang.CHR_TIAN_HU).is_empty()){//天胡
			int lChiHuScore = 10 * di_score;
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index)
					continue;
				GRR._game_score[i] -= lChiHuScore;
				GRR._game_score[seat_index] += lChiHuScore;
			}
		}else if(!chr.opr_and(Constants_GuangChang.CHR_DI_HU).is_empty()){//地胡
			int lChiHuScore = 5 * di_score;
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				
				if(i == provide_index){
					GRR._game_score[i] -= lChiHuScore * 2;
					GRR._game_score[seat_index] += lChiHuScore * 2;
				}else{
					GRR._game_score[i] -= lChiHuScore;
					GRR._game_score[seat_index] += lChiHuScore;
				}
			}
		}else{
			int get_pai_xing_score = get_pai_xing_score(chr,seat_index);
			if (zimo) {//自摸
				int lChiHuScore = 2 * get_pai_xing_score;
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (i == seat_index) {
						continue;
					}
					GRR._game_score[i] -= lChiHuScore * di_score;
					GRR._game_score[seat_index] += lChiHuScore * di_score;
				}
			}else {//抢杠胡
				if(!chr.opr_and(Constants_GuangChang.CHR_QIANG_GANG).is_empty()){
					GRR._game_score[provide_index] -= get_pai_xing_score * di_score;
					GRR._game_score[seat_index] += get_pai_xing_score * di_score;
				}else{//点炮胡
					if(get_pai_xing_score >= 4 && !chr.opr_and(Constants_GuangChang.CHR_GANG_SHANG_PAO).is_empty()){
						for (int i = 0; i < getTablePlayerNumber(); i++) {
							if (i == seat_index) {
								continue;
							}
							
							if(i == provide_index){
								GRR._game_score[provide_index] -= 2 * get_pai_xing_score * di_score;
								GRR._game_score[seat_index] += 2 * get_pai_xing_score * di_score;
								continue;
							}
							
							GRR._game_score[i] -= get_pai_xing_score * di_score / 2;
							GRR._game_score[seat_index] += get_pai_xing_score * di_score / 2;
						}
					}else{
						int score = 2 * get_pai_xing_score * di_score;
						if(!chr.opr_and(Constants_GuangChang.CHR_GANG_SHANG_PAO).is_empty())
							score *=2;
						GRR._game_score[provide_index] -= score;
						GRR._game_score[seat_index] += score;
					}
				}
			}
		}
			
		// 设置变量
		GRR._provider[seat_index] = provide_index;
		_status_gang = false;
		_status_gang_hou_pao = false;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);
	}
	
	/** 胡牌的牌型总分
	 * @param _seat_index */
	public int get_pai_xing_score(ChiHuRight chr, int _seat_index) {
		int score = 1;
		boolean is_qys = !chr.opr_and(Constants_GuangChang.CHR_QING_YI_SE).is_empty()||
				!chr.opr_and(Constants_GuangChang.CHR_QING_YI_SE_JIA_HU).is_empty();
		if(!chr.opr_and(Constants_GuangChang.CHR_DI_HU).is_empty()||
				!chr.opr_and(Constants_GuangChang.CHR_TIAN_HU).is_empty()){
			return 10;
		}
			
		if(!chr.opr_and(Constants_GuangChang.CHR_QX_SHI_SAN_LAN).is_empty()||
				!chr.opr_and(Constants_GuangChang.CHR_DA_QI_DUI).is_empty()||
				!chr.opr_and(Constants_GuangChang.CHR_YAO_JIU).is_empty()||
				!chr.opr_and(Constants_GuangChang.CHR_DUAN_YAO_JIU).is_empty())
			score = 2;
		if(!chr.opr_and(Constants_GuangChang.CHR_XAIO_QI_DUI).is_empty())
			score = 3;
		if(!chr.opr_and(Constants_GuangChang.CHR_QUAN_QIU_REN).is_empty()||
				!chr.opr_and(Constants_GuangChang.CHR_YJ_DA_QI_DUI).is_empty()||
				!chr.opr_and(Constants_GuangChang.CHR_AC_DA_QI_DUI).is_empty()||
				!chr.opr_and(Constants_GuangChang.CHR_WP_QING_YI_SE).is_empty())
			score = 4;
		if(!chr.opr_and(Constants_GuangChang.CHR_YJ_XIAO_QI_DUI).is_empty()||
				!chr.opr_and(Constants_GuangChang.CHR_HH_XIAO_QI_DUI).is_empty()||
				!chr.opr_and(Constants_GuangChang.CHR_XS_XIAO_QI_DUI).is_empty())
			score = 6;
		if(!chr.opr_and(Constants_GuangChang.CHR_YJ_QUAN_QIU_REN).is_empty())
			score = 8;
		if(!chr.opr_and(Constants_GuangChang.CHR_2HH_XIAO_QI_DUI).is_empty())
			score = 12;
		if(!chr.opr_and(Constants_GuangChang.CHR_3HH_XIAO_QI_DUI).is_empty())
			score = 24;
		
		return (is_qys ? 2 * score : score) * getGangFan(_seat_index);
	}
	
	public int getGangFan(int _seat_index){
		int weave_count = GRR._weave_count[_seat_index];
		int gang_count = 0;
		for (int i = 0; i < weave_count; i++) {
			if(GRR._weave_items[_seat_index][i].weave_kind == GameConstants.WIK_GANG)
				gang_count++;
		}
		if(gang_count == 2)
			return 2;
		if(gang_count == 3)
			return 4;
		return 1;
	}
	
	public int getBasicScore(ChiHuRight chr){
		int score = 1;
		if(!chr.opr_and(Constants_GuangChang.CHR_DI_HU).is_empty()||
				!chr.opr_and(Constants_GuangChang.CHR_TIAN_HU).is_empty()){
			return 10;
		}
		if(!chr.opr_and(Constants_GuangChang.CHR_QX_SHI_SAN_LAN).is_empty()||
				!chr.opr_and(Constants_GuangChang.CHR_DA_QI_DUI).is_empty()||
				!chr.opr_and(Constants_GuangChang.CHR_YAO_JIU).is_empty()||
				!chr.opr_and(Constants_GuangChang.CHR_DUAN_YAO_JIU).is_empty())
			score = 2;
		if(!chr.opr_and(Constants_GuangChang.CHR_XAIO_QI_DUI).is_empty())
			score = 3;
		if(!chr.opr_and(Constants_GuangChang.CHR_QUAN_QIU_REN).is_empty()||
				!chr.opr_and(Constants_GuangChang.CHR_YJ_DA_QI_DUI).is_empty()||
				!chr.opr_and(Constants_GuangChang.CHR_AC_DA_QI_DUI).is_empty()||
				!chr.opr_and(Constants_GuangChang.CHR_WP_QING_YI_SE).is_empty())
			score = 4;
		if(!chr.opr_and(Constants_GuangChang.CHR_YJ_XIAO_QI_DUI).is_empty()||
				!chr.opr_and(Constants_GuangChang.CHR_HH_XIAO_QI_DUI).is_empty()||
				!chr.opr_and(Constants_GuangChang.CHR_XS_XIAO_QI_DUI).is_empty())
			score = 6;
		if(!chr.opr_and(Constants_GuangChang.CHR_YJ_QUAN_QIU_REN).is_empty())
			score = 8;
		if(!chr.opr_and(Constants_GuangChang.CHR_2HH_XIAO_QI_DUI).is_empty())
			score = 12;
		if(!chr.opr_and(Constants_GuangChang.CHR_3HH_XIAO_QI_DUI).is_empty())
			score = 24;
		
		return score;
	}
	
	public int getOtherAddScore(ChiHuRight chr, int _seat_index){
		int score = 1;
		if(!chr.opr_and(Constants_GuangChang.CHR_ZI_MO).is_empty()||
				!chr.opr_and(Constants_GuangChang.CHR_DIAN_PAO).is_empty()||
				!chr.opr_and(Constants_GuangChang.CHR_GANG_KAI_HUA).is_empty())
			score *= 2;
		if(!chr.opr_and(Constants_GuangChang.CHR_QING_YI_SE).is_empty())
			score *= 2;
		return score * getGangFan(_seat_index);
	}

	@Override
	protected void set_result_describe() {
		int chrTypes;
		long type = 0;
		boolean is_p = false;
		for (int player = 0; player < this.getTablePlayerNumber(); player++) {
			StringBuilder result = new StringBuilder("");

			chrTypes = GRR._chi_hu_rights[player].type_count;

			for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
				type = GRR._chi_hu_rights[player].type_list[typeIndex];

				if (GRR._chi_hu_rights[player].is_valid()) {
					if (get_pai_xing_score(GRR._chi_hu_rights[player], player) / getGangFan(player) == 1 
							&& GRR._chi_hu_rights[player].opr_and(Constants_GuangChang.CHR_SHI_SAN_LAN).is_empty()) {
						if(!is_p){
							result.append(" 平胡");
							is_p = true;
						}
					}
					if (type == Constants_GuangChang.CHR_ZI_MO) {
						result.append(" 自摸");
					}
					if (type == Constants_GuangChang.CHR_QIANG_GANG) {
						result.append(" 抢杠胡");
					}
					if (type == Constants_GuangChang.CHR_GANG_KAI_HUA) {
						result.append(" 杠开花");
					}
					if (type == Constants_GuangChang.CHR_DIAN_PAO) {
						result.append(" 点炮胡");
					}
					if (type == Constants_GuangChang.CHR_QUAN_QIU_REN) {
						result.append(" 全求人");
					}
					if (type == Constants_GuangChang.CHR_SHI_SAN_LAN) {
						result.append(" 十三烂");
					}
					if (type == Constants_GuangChang.CHR_QX_SHI_SAN_LAN) {
						result.append(" 七星十三烂");
					}
					if (type == Constants_GuangChang.CHR_DA_QI_DUI) {
						result.append(" 大七对");
					}
					if (type == Constants_GuangChang.CHR_XAIO_QI_DUI) {
						result.append(" 小七对");
					}
					if (type == Constants_GuangChang.CHR_YAO_JIU) {
						result.append(" 幺九");
					}
					if (type == Constants_GuangChang.CHR_DUAN_YAO_JIU) {
						result.append(" 断幺九");
					}
					if (type == Constants_GuangChang.CHR_YJ_DA_QI_DUI) {
						result.append(" 幺九大七对");
					}
					if (type == Constants_GuangChang.CHR_YJ_XIAO_QI_DUI) {
						result.append(" 幺九小七对");
					}
					if (type == Constants_GuangChang.CHR_YJ_QUAN_QIU_REN) {
						result.append(" 幺九全求人");
					}
					if (type == Constants_GuangChang.CHR_HH_XIAO_QI_DUI) {
						result.append(" 豪华小七对");
					}
					if (type == Constants_GuangChang.CHR_2HH_XIAO_QI_DUI) {
						result.append(" 双豪华小七对");
					}
					if (type == Constants_GuangChang.CHR_3HH_XIAO_QI_DUI) {
						result.append(" 三豪华小七对");
					}
					if (type == Constants_GuangChang.CHR_AC_DA_QI_DUI) {
						result.append(" 暗藏大七对");
					}
					if (type == Constants_GuangChang.CHR_XS_XIAO_QI_DUI) {
						result.append(" 潇洒小七对");
					}
					if (type == Constants_GuangChang.CHR_TIAN_HU) {
						result.append(" 天胡");
					}
					if (type == Constants_GuangChang.CHR_DI_HU) {
						result.append(" 地胡");
					}
					if (type == Constants_GuangChang.CHR_QING_YI_SE) {
						result.append(" 清一色");
					}
					if (type == Constants_GuangChang.CHR_WP_QING_YI_SE) {
						result.append(" 清一色真胡");
					}
					if (type == Constants_GuangChang.CHR_QING_YI_SE_JIA_HU) {
						result.append(" 清一色假胡");
					}
					if (type == Constants_GuangChang.CHR_GANG_SHANG_PAO) {
						result.append(" 杠上炮");
					}
				} else if (type == Constants_GuangChang.CHR_FANG_PAO) {
					result.append(" 放炮");
				} else if (type == Constants_GuangChang.CHR_FANG_GANG) {
					result.append(" 被抢杠");
				}
			}

			int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0;

			if (GRR != null) {
				for (int tmpPlayer = 0; tmpPlayer < this.getTablePlayerNumber(); tmpPlayer++) {
					for (int w = 0; w < GRR._weave_count[tmpPlayer]; w++) {
						if (GRR._weave_items[tmpPlayer][w].weave_kind != GameConstants.WIK_GANG) {
							continue;
						}
						if (tmpPlayer == player) {
							if (GRR._weave_items[tmpPlayer][w].provide_player != tmpPlayer) {
								jie_gang++;
							} else {
								if (GRR._weave_items[tmpPlayer][w].public_card == 1) {
									ming_gang++;
								} else {
									an_gang++;
								}
							}
						} else {
							if (GRR._weave_items[tmpPlayer][w].provide_player == player) {
								fang_gang++;
							}
						}
					}
				}
			}

			if (an_gang > 0) {
				result.append(" 暗杠X" + an_gang);
			}
			if (ming_gang > 0) {
				result.append(" 明杠X" + ming_gang);
			}
			if (fang_gang > 0) {
				result.append(" 放杠X" + fang_gang);
			}
			if (jie_gang > 0) {
				result.append(" 接杠X" + jie_gang);
			}
			
			if(_cur_banker == player && gen_zhuang_card == -2 && getTablePlayerNumber() == 4){
				result.append(" 被烧庄");
			}

			GRR._result_des[player] = result.toString();
		}
	}




	public boolean estimate_player_out_card_respond(int seat_index, int card, int type) {
		boolean bAroseAction = false;

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (seat_index == i) {
				continue;
			}

			playerStatus = _playerStatus[i];

			boolean can_peng = true;
			int[] tmp_cards_data = _playerStatus[i].get_cards_abandoned_peng();
			for (int x = 0; x < GameConstants.MAX_ABANDONED_CARDS_COUNT; x++) {
				if (tmp_cards_data[x] == card) {
					can_peng = false;
					break;
				}
			}
			if (can_peng && GRR._left_card_count > 0) {//如果牌堆已经没有牌，出的最后一张牌了不能碰，杠
				action = _logic.check_peng(GRR._cards_index[i], card);
				if (action != 0) {
					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);
					bAroseAction = true;
				}

				if (GRR._left_card_count > 0) {
					action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
					if (action != 0) {
						playerStatus.add_action(GameConstants.WIK_GANG);
						playerStatus.add_gang(card, seat_index, 1); // 加上杠
						bAroseAction = true;
					}
				}
			}
			
			// 接炮时，牌型分大，才能接炮胡
			ChiHuRight chr = GRR._chi_hu_rights[i];
			chr.set_empty();
			int cbWeaveCount = GRR._weave_count[i];
			action = analyse_chi_hu_card(GRR._cards_index[i],
					GRR._weave_items[i], cbWeaveCount, card, chr,
					Constants_GuangChang.HU_CARD_TYPE_DIAN_PAO, i);

			if (action != GameConstants.WIK_NULL && (_playerStatus[i].is_chi_hu_round() || temp_pai_xing_score[i] > cannot_pai_xing_score[i])) {
				_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
				_playerStatus[i].add_chi_hu(card, seat_index);
				bAroseAction = true;
			}

		}

		if (bAroseAction) {
			_resume_player = _current_player;
			_current_player = GameConstants.INVALID_SEAT;
			_provide_player = seat_index;
		} else {
			return false;
		}

		return true;
	}

	public int get_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					Constants_GuangChang.HU_CARD_TYPE_ZI_MO, seat_index)) {
				cards[count++] = cbCurrentCard;
			}
		}
		
		//全听
		if (count == GameConstants.MAX_ZI_FENG) {
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		return false;
	}
	
	
	
	/**
	 * 混一色判断
	 * 
	 * @return
	 */
	public boolean is_hun_yi_se(int cards_index[], WeaveItem weaveItem[], int weaveCount) {
		if (_logic.get_se_count(cards_index, weaveItem, weaveCount) != 1) {
			return false;
		}

		if (_logic.has_feng_pai(cards_index, weaveItem, weaveCount) == false) {
			return false;
		}
		return true;
	}
	
	
	
	
    public boolean is_yao_jiu(int[] hand_indexs, WeaveItem weaveItem[], int weaveCount) {
    	// 一九判断
		for (int i = 0; i < GameConstants.MAX_ZI_FENG; i ++) {
			if(hand_indexs[i] == 0)
				continue;
			if(_logic.is_magic_index(i))
				continue;
			if(i >= GameConstants.MAX_ZI)
        		return false;
			// 无效判断
			int value = _logic.get_card_value(_logic.switch_to_card_data(i));
			if (value != 1 && value != 9)
				return false;
		}
    	
		// 落地牌都是19
        for (int i = 0; i < weaveCount; i++) {
        	if(_logic.switch_to_card_index(weaveItem[i].center_card) >= GameConstants.MAX_ZI)
        		return false;
            int value = _logic.get_card_value(weaveItem[i].center_card);
            if (value != 1 && value != 9)
				return false;
        }
        return true;
    }
    
    public boolean is_duan_yao_jiu(int[] hand_indexs, WeaveItem weaveItem[], int weaveCount) {
		for (int i = 0; i < GameConstants.MAX_ZI_FENG; i ++) {
			if(hand_indexs[i] == 0)
				continue;
			if(_logic.is_magic_index(i))
				continue;
			// 无效判断
			int value = _logic.get_card_value(_logic.switch_to_card_data(i));
			if (value == 1 || value == 9)
				return false;
		}
    	
		// 落地牌都不是19
        for (int i = 0; i < weaveCount; i++) {
            int value = _logic.get_card_value(weaveItem[i].center_card);
            if (value == 1 || value == 9)
				return false;
        }
        return true;
    }
    
    public boolean is_no_pg(WeaveItem weaveItem[], int weaveCount) {
    	if(weaveCount == 0)
    		return true;
    	
    	// 都是暗杠
        for (int i = 0; i < weaveCount; i++) {
            if(weaveItem[i].weave_kind != GameConstants.WIK_GANG || weaveItem[i].public_card != 0)
            	return false;
        }
        return true;
    }
    
    //没有碰
    public boolean is_no_p(WeaveItem weaveItem[], int weaveCount) {
    	if(weaveCount == 0)
    		return true;
    	
    	// 都是暗杠
    	for (int i = 0; i < weaveCount; i++) {
    		if(weaveItem[i].weave_kind == GameConstants.WIK_PENG)
    			return false;
    	}
    	return true;
    }
    
    // 杠牌分析 包括补杠 check_weave检查补杠
    public int analyse_gang(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount,
            GangCardResult gangCardResult, boolean check_weave, int cards_abandoned_gang[]) {
        // 设置变量
        int cbActionMask = GameConstants.WIK_NULL;

        // 手上杠牌
        for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
            if (cards_index[i] == 4) {
                cbActionMask |= GameConstants.WIK_GANG;
                int index = gangCardResult.cbCardCount++;
                gangCardResult.cbCardData[index] = _logic.switch_to_card_data(i);
                gangCardResult.isPublic[index] = 0;// 暗刚
                gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
            }
        }

        if (check_weave) {
            // 组合杠牌，包括以前能杠，但是不杠，发牌之后不能再杠
            for (int i = 0; i < cbWeaveCount; i++) {
                if (WeaveItem[i].weave_kind == GameConstants.WIK_PENG && WeaveItem[i].is_vavild) {
                    for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
                        if (cards_index[j] != 1) { //  || cards_abandoned_gang[j] != 0
                            continue;
                        } else {
                            if (WeaveItem[i].center_card == _logic.switch_to_card_data(j)) {
                                cbActionMask |= GameConstants.WIK_GANG;
                                int index = gangCardResult.cbCardCount++;
                                gangCardResult.cbCardData[index] = WeaveItem[i].center_card;
                                gangCardResult.isPublic[index] = 1;// 明刚
                                gangCardResult.type[index] = GameConstants.GANG_TYPE_ADD_GANG;
                                break;
                            }
                        }
                    }
                }
            }
        }

        return cbActionMask;
    }
    
    /**
     * 处理牌桌结束逻辑
     * 
     * @param seat_index
     * @param reason
     */
    protected boolean on_handler_game_finish(int seat_index, int reason) {
        int real_reason = reason;

        for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
            _player_ready[i] = 0;
        }

        RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
        roomResponse.setType(MsgConstants.RESPONSE_GAME_END);
        GameEndResponse.Builder game_end = GameEndResponse.newBuilder();
        // 查牌数据
        this.setGameEndBasicPrama(game_end);

        roomResponse.setLeftCardCount(0);

        this.load_common_status(roomResponse);
        this.load_room_info_data(roomResponse);

        // 这里记录了两次，先这样
        RoomInfo.Builder room_info = getRoomInfo();
        game_end.setRoomInfo(room_info);
        game_end.setRunPlayerId(_run_player_id);
        game_end.setRoundOverType(0);
        game_end.setGamePlayerNumber(getTablePlayerNumber());
        game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间
        if (GRR != null) {// reason == MJGameConstants.Game_End_NORMAL || reason
                          // == MJGameConstants.Game_End_DRAW ||
            // (reason ==MJGameConstants.Game_End_RELEASE_PLAY && GRR!=null)
            game_end.setRoundOverType(1);
            game_end.setStartTime(GRR._start_time);

            game_end.setGameTypeIndex(GRR._game_type_index);
            roomResponse.setLeftCardCount(GRR._left_card_count);

            // 特别显示的牌
            if (this.is_mj_type(GameConstants.GAME_TYPE_HUNAN_XIANG_TAN)
                    || this.is_mj_type(GameConstants.GAME_TYPE_HUNAN_XIANG_TAN_DT)) {
                for (int i = 0; i < GRR._especial_card_count; i++) {
                    game_end.addEspecialShowCards(
                            GRR._especial_show_cards[i] + GameConstants.CARD_ESPECIAL_TYPE_DING_GUI);
                }
            } else {
                for (int i = 0; i < GRR._especial_card_count; i++) {
                    game_end.addEspecialShowCards(GRR._especial_show_cards[i]);
                }
            }

            GRR._end_type = reason;

            // 杠牌，每个人的分数
            float lGangScore[] = new float[getTablePlayerNumber()];
            
            for (int i = 0; i < getTablePlayerNumber(); i++) {
            	for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
            		for (int k = 0; k < getTablePlayerNumber(); k++) {
            			lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
            		}
            	}

                // 记录
                for (int j = 0; j < getTablePlayerNumber(); j++) {
                    _player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
                }

            }

            for (int i = 0; i < getTablePlayerNumber(); i++) {
                GRR._game_score[i] += lGangScore[i];
                GRR._game_score[i] += GRR._start_hu_score[i]; // 起手胡分数

                // 记录
                _player_result.game_score[i] += GRR._game_score[i];
                
                _player_result.biaoyan[i] = 0;

            }

            this.load_player_info_data(roomResponse);

            game_end.setGameRound(_game_round);
            game_end.setCurRound(_cur_round);

            game_end.setCellScore(GameConstants.CELL_SCORE);

            game_end.setBankerPlayer(GRR._banker_player);// 专家
            game_end.setLeftCardCount(GRR._left_card_count);// 剩余牌
            game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

            // 设置中鸟数据
            for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i < GRR._count_niao; i++) {
                game_end.addCardsDataNiao(GRR._cards_data_niao[i]);
            }
            // 设置中鸟数据
            for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i < GRR._count_niao_fei; i++) {
                game_end.addCardsDataNiao(GRR._cards_data_niao_fei[i]);
            }
            game_end.setCountPickNiao(GRR._count_pick_niao + GRR._count_pick_niao_fei);// 中鸟个数

            for (int i = 0; i < getTablePlayerNumber(); i++) {
                Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
                for (int j = 0; j < GRR._player_niao_count[i]; j++) {
                    pnc.addItem(GRR._player_niao_cards[i][j]);
                }
                for (int j = 0; j < GRR._player_niao_count_fei[i]; j++) {
                    pnc.addItem(GRR._player_niao_cards_fei[i][j]);
                }
                game_end.addPlayerNiaoCards(pnc);
                game_end.addHuResult(GRR._hu_result[i]);

                // 胡的牌
                Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
                for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
                    hc.addItem(GRR._chi_hu_card[i][j]);
                }

                game_end.addHuCardData(GRR._chi_hu_card[i][0]);
                game_end.addHuCardArray(hc);
            }

            // 现在权值只有一位
            long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

            // 设置胡牌描述
            this.set_result_describe();
            
            boolean liu_ju = reason == GameConstants.Game_End_DRAW || reason == GameConstants.Game_End_RELEASE_PLAY;
            if(liu_ju && gen_zhuang_card == -2 && getTablePlayerNumber() == 4){
            	for (int i = 0; i < getTablePlayerNumber(); i++) {
    				if (i == GRR._banker_player)
    					continue;
    				GRR._game_score[i] -= getRoomScore();
    				GRR._game_score[GRR._banker_player] += getRoomScore();
    			}
            }

            for (int i = 0; i < getTablePlayerNumber(); i++) {
                GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

                Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
                for (int j = 0; j < GRR._card_count[i]; j++) {

                    cs.addItem(GRR._cards_data[i][j]);
                }
                game_end.addCardsData(cs);// 牌

                // 组合
                WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
                for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
                    WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
                    weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
                    weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
                    weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
                    weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
                    weaveItem_array.addWeaveItem(weaveItem_item);
                }
                game_end.addWeaveItemArray(weaveItem_array);

                GRR._chi_hu_rights[i].get_right_data(rv);// 获取权位数值
                game_end.addChiHuRight(rv[0]);

                GRR._start_hu_right[i].get_right_data(rv);// 获取权位数值
                game_end.addStartHuRight(rv[0]);

                game_end.addProvidePlayer(GRR._provider[i]);
                game_end.addGameScore(GRR._game_score[i]);// 胡牌分
                game_end.addGangScore(lGangScore[i]);// 杠牌得分
                game_end.addStartHuScore(GRR._start_hu_score[i]);
                game_end.addResultDes(GRR._result_des[i]);
                
                int a = 0;
                //烧庄分
                if(reason == GameConstants.Game_End_NORMAL && gen_zhuang_card == -2 && getTablePlayerNumber() == 4){
                	if(i == GRR._banker_player)
                		game_end.addPao(a=-getRoomScore() * 3);
                	else
                		game_end.addPao(a=getRoomScore());
                }
                game_end.addQiang((int)(GRR._game_score[i]-lGangScore[i]-a));//胡牌分

                // 胡牌
                game_end.addWinOrder(GRR._win_order[i]);

                Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
                for (int j = 0; j < getTablePlayerNumber(); j++) {
                    lfs.addItem(GRR._lost_fan_shu[i][j]);
                }

                game_end.addLostFanShu(lfs);

            }

        }

        boolean end = false;
        if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
            if (_cur_round >= _game_round && (!is_sys())) {// 局数到了
                end = true;
                game_end.setRoomOverType(1);
                game_end.setPlayerResult(this.process_player_result(reason));
            } else {
                // 确定下局庄家
                // 以后谁胡牌，下局谁做庄。
                // 流局,庄家下一个
                // 通炮,放炮的玩家

            }
        } else if ((!is_sys()) && (reason == GameConstants.Game_End_RELEASE_PLAY
                || reason == GameConstants.Game_End_RELEASE_NO_BEGIN || reason == GameConstants.Game_End_RELEASE_RESULT
                || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
                || reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT
                || reason == GameConstants.Game_End_RELEASE_SYSTEM)) {
            end = true;
            real_reason = GameConstants.Game_End_DRAW;// 刘局
            game_end.setRoomOverType(1);
            game_end.setPlayerResult(this.process_player_result(reason));
        }
        game_end.setEndType(real_reason);

        ////////////////////////////////////////////////////////////////////// 得分总的
        game_end.setPlayerResult(this.process_player_result(reason));
        /*if(seat_index >= 0){
        	ChiHuRight chr = GRR._chi_hu_rights[seat_index];
        	game_end.setFanShu(get_pai_xing_score(chr, seat_index));//基础分
        	game_end.setTunShu(getOtherAddScore(chr, seat_index));//另加番
        }*/
        
        
        roomResponse.setGameEnd(game_end);

        this.send_response_to_room(roomResponse);

        record_game_round(game_end);

        // 结束后刷新玩家
        // RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
        // roomResponse2.setGameStatus(_game_status);
        // roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
        // this.load_player_info_data(roomResponse2);
        // this.send_response_to_room(roomResponse2);

        // 超时解散
        if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
                || reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
            for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
                Player player = this.get_players()[j];
                if (player == null)
                    continue;
                send_error_notify(j, 1, "游戏解散成功!");

            }
        }

        if (end && (!is_sys()))// 删除
        {
            PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
        }

        if (!is_sys()) {
            GRR = null;
        }

        if (is_sys()) {
            clear_score_in_gold_room();
        }
        // 错误断言
        return false;
    }
    
    //判断十三烂 ，0不是， 1是，2七星十三烂
    public int isShiSanLan(int[] cards_index,int weaveCount) {
		if (weaveCount != 0) {
			return 0;
		}

		for (int i = 0; i < 27; i += 9) {
			for (int j = i; j < i + 9; j++) {
				if (cards_index[j] == 0) {
					continue;
				}
				if (cards_index[j] > 1) {
					return 0;
				}

				int limitIndex = i + 9;
				if (j + 1 < limitIndex && cards_index[j + 1] != 0) {
					return 0;
				}
				if (j + 2 < limitIndex && cards_index[j + 2] != 0) {
					return 0;
				}
			}
		}

		int count = 0;
		for (int i = GameConstants.MAX_ZI; i < GameConstants.MAX_ZI_FENG; i++) {
			if (cards_index[i] > 1) {
				return 0;
			}
			if(cards_index[i] == 1)
				count++;
		}
		if(count == 7)
			return 2;
		return 1;
	}
    
    // -1 不是小七对 0是 1豪华 2双豪华 3三豪华
 	public int is_xiao_qi_dui(int cards_index[], int cbWeaveCount) {

 		// 组合判断
 		if (cbWeaveCount != 0)
 			return -1;

 		// 单牌数目
 		int cbReplaceCount = 0;
 		int nGenCount = 0;

 		// 计算单牌
 		for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
 			int cbCardCount = cards_index[i];
 				// 单牌统计
 				if (cbCardCount == 1 || cbCardCount == 3)
 					cbReplaceCount++;

 				if (cbCardCount == 4) {
 					nGenCount++;
 				}
 		}

		if (cbReplaceCount > 0)
			return -1;

 		if (nGenCount == 1)
 			return 1;
 		if (nGenCount == 2)
 			return 2;
 		if (nGenCount == 3)
 			return 3;
 			
 		return 0;
 	}
 	
 	public boolean is_xiao_sha(int cards_index[],int weaveCount){
 		if (weaveCount != 0) {
			return false;
		}
 		int[] copy_cards = Arrays.copyOf(cards_index, cards_index.length);
 		return is_xiao_sha1(copy_cards) && is_xiao_sha1(copy_cards);
 	}
 	
 	
    
    private boolean is_xiao_sha1(int[] cards_index) {
    	
		for (int i = 0; i < 27; i += 9) {
			for (int j = i + 1; j < i + 8; j++) {
				if (cards_index[j] == 0) {
					continue;
				}
				if (cards_index[j + 1] != 0 && cards_index[j - 1] != 0) {
					cards_index[j + 1] = cards_index[j + 1] - 2;
					cards_index[j - 1] = cards_index[j - 1] - 2;
					cards_index[j] = cards_index[j] - 2;
					return true;
				}
			}
		}
		
		int count = 0;
		for (int i = GameConstants.MAX_ZI; i < GameConstants.MAX_FENG; i++) {
			if (cards_index[i] != 0)
				count++;
			if(count == 3){
				int k = 0;
				for (int j = GameConstants.MAX_ZI; j < GameConstants.MAX_FENG; j++) {
					if(k == 3)
						break;
					if (cards_index[i] != 0){
						cards_index[j] = cards_index[j] - 2;
						k++;
					}
				}
				return true;
			}
		}
		for (int i = GameConstants.MAX_FENG; i < GameConstants.MAX_ZI_FENG; i++) {
			if (cards_index[i] != 0)
				count++;
			if(count == 3){
				cards_index[GameConstants.MAX_FENG] = cards_index[GameConstants.MAX_FENG] - 2;
				cards_index[GameConstants.MAX_FENG + 1] = cards_index[GameConstants.MAX_FENG + 1] - 2;
				cards_index[GameConstants.MAX_FENG + 2] = cards_index[GameConstants.MAX_FENG + 2] - 2;
				return true;
			}
		}
		return false;
	}
    
    public int getRoomScore(){
    	int di_score = 1;
		if(has_rule(Constants_GuangChang.GAME_RULE_ROOM_SCORE_2))
			di_score = 2;
		if(has_rule(Constants_GuangChang.GAME_RULE_ROOM_SCORE_5))
			di_score = 5;
		return di_score;
    }
    
    public boolean operate_player_info() {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);
		roomResponse.setGameStatus(_game_status);

		load_player_info_data(roomResponse);

		if (GRR != null)
			GRR.add_room_response(roomResponse);

		send_response_to_room(roomResponse);

		return true;
	}
    
    public void changeBiaoYan(){
        for (int i = 0; i < getTablePlayerNumber(); i++) {
        	for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
        		for (int k = 0; k < getTablePlayerNumber(); k++) {
        			_player_result.biaoyan[i] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
        		}
        	}
        }
    }

	@Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x1,0x5,0x9,0x12,0x15,0x19,0x31,0x32,0x33,0x34,0x35,0x36,0x22 };
		int[] cards_of_player1 = new int[] { 0x11, 0x11, 0x11, 0x12, 0x15,
				0x12, 0x13, 0x13, 0x13, 0x14, 0x14, 0x14, 0x15 };
		int[] cards_of_player3 = new int[] { 0x31, 0x31, 0x31, 0x32, 0x32,
				0x32, 0x33, 0x33, 0x33, 0x34, 0x34, 0x34, 0x11 };
		int[] cards_of_player2 = new int[] { 0x31, 0x31, 0x31, 0x32, 0x32,
				0x32, 0x33, 0x33, 0x33, 0x34, 0x34, 0x34, 0x35 };

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}

		for (int j = 0; j < 13; j++) {
			if (getTablePlayerNumber() == 4) {
				GRR._cards_index[0][_logic
						.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[1][_logic
						.switch_to_card_index(cards_of_player1[j])] += 1;
				GRR._cards_index[2][_logic
						.switch_to_card_index(cards_of_player2[j])] += 1;
				GRR._cards_index[3][_logic
						.switch_to_card_index(cards_of_player3[j])] += 1;
			} else if (getTablePlayerNumber() == 3) {
				GRR._cards_index[0][_logic
						.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[1][_logic
						.switch_to_card_index(cards_of_player1[j])] += 1;
				GRR._cards_index[2][_logic
						.switch_to_card_index(cards_of_player2[j])] += 1;
			} else if (getTablePlayerNumber() == 2) {
				GRR._cards_index[0][_logic
								.switch_to_card_index(cards_of_player0[j])] += 1;
					GRR._cards_index[1][_logic
							.switch_to_card_index(cards_of_player1[j])] += 1;
				}
		}

		if (BACK_DEBUG_CARDS_MODE) {
			if (debug_my_cards != null) {
				if (debug_my_cards.length > 14) {
					int[] temps = new int[debug_my_cards.length];
					System.arraycopy(debug_my_cards, 0, temps, 0, temps.length);
					testRealyCard(temps);
					debug_my_cards = null;
				} else {
					int[] temps = new int[debug_my_cards.length];
					System.arraycopy(debug_my_cards, 0, temps, 0, temps.length);
					testSameCard(temps);
					debug_my_cards = null;
				}
			}
		}
	}
	
	
}
