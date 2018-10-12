package com.cai.game.mj.chenchuang.pingxiang258;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.Constants_MJ_PING_XIANG_258;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.RandomUtil;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJConstants;
import com.cai.game.mj.MJType;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.service.PlayerServiceImpl;

/**
 * chenchuang
 * 萍乡麻将
 */
public class Table_PING_XIANG extends AbstractMJTable {

	private static final long serialVersionUID = 1L;
	
	public HandlerPao_PING_XIANG _handler_pao;
	
	public int[] fa_pai_count = new int[getTablePlayerNumber()];
	public boolean[] is_gang_yao = new boolean[getTablePlayerNumber()];
	public int[] chi_color = new int[getTablePlayerNumber()];

	public Table_PING_XIANG() {
		super(MJType.GAME_TYPE_MJ_PING_XIANG_258);
	}

	@Override
	protected void onInitTable() {
		_handler_chi_peng = new HandlerChiPeng_PING_XIANG();
		_handler_dispath_card = new HandlerDispatchCard_PING_XIANG();
		_handler_gang = new HandlerGang_PING_XIANG();
		_handler_out_card_operate = new HandlerOutCardOperate_PING_XIANG();
		_handler_pao = new HandlerPao_PING_XIANG();
	}

	/**第一局建房者为庄家，若为代开房/俱乐部开房（即开房者自己未进入牌局），则随机一位为庄家;*/
	@Override
	protected void initBanker() {
		long id = getCreate_player().getAccount_id();
		if(get_player(id) == null)
			_cur_banker = RandomUtil.getRandomNumber(getTablePlayerNumber());
	}

	
	@Override
	protected void init_shuffle() {
		int[] card = GameConstants.CARD_DATA_WAN_TIAO_TONG;
		if(has_rule(Constants_MJ_PING_XIANG_258.GAME_RULE_DAI_FENG_PAI))
			card = MJConstants.DEFAULT;
		_repertory_card = new int[card.length];
		shuffle(_repertory_card, card);
	}
	
	@Override
	public boolean handler_requst_pao_qiang(Player player, int pao, int qiang) {
		if (has_rule(Constants_MJ_PING_XIANG_258.GAME_RULE_MAI_ZI))
			_handler_pao.handler_pao_qiang(this, player.get_seat_index(), pao, qiang);
		return false;
	}
	
	@Override
	public boolean handler_player_ready(int seat_index, boolean is_cancel) {

		if (this.get_players()[seat_index] == null) {
			return false;
		}
		if (GameConstants.GS_MJ_XIAOHU != _game_status && GameConstants.GS_MJ_FREE != _game_status
				&& GameConstants.GS_MJ_WAIT != _game_status) {
			return false;
		}

		if (is_cancel) {//
			_player_ready[seat_index] = 0;
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_PLAYER_READY);
			roomResponse.setOperatePlayer(seat_index);
			roomResponse.setIsCancelReady(is_cancel);
			send_response_to_room(roomResponse);
			if (this._cur_round > 0) {
				// 结束后刷新玩家
				RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
				roomResponse2.setGameStatus(_game_status);
				roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
				this.load_player_info_data(roomResponse2);
				this.send_response_to_player(seat_index, roomResponse2);
			}
			return false;
		}

		_player_ready[seat_index] = 1;

		boolean nt = true;
		if (this.get_players()[seat_index].getAccount_id() == this.getRoom_owner_account_id()) {
			nt = false;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PLAYER_READY);
		roomResponse.setOperatePlayer(seat_index);
		send_response_to_room(roomResponse);
		if (this._cur_round > 0) {
			// 结束后刷新玩家
			RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
			roomResponse2.setGameStatus(_game_status);
			roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
			this.load_player_info_data(roomResponse2);
			this.send_response_to_player(seat_index, roomResponse2);
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (this.get_players()[i] == null) {
				_player_ready[i] = 0;
			}

			if (_player_ready[i] == 0) {
				this.refresh_room_redis_data(GameConstants.PROXY_ROOM_PLAYER, nt);
				return false;
			}
		}

		handler_game_start();

		this.refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, nt);

		return true;
	}

	/**
	 * 洗完牌执行开始
	 */
	@Override
	protected boolean on_game_start() {
		fa_pai_count = new int[getTablePlayerNumber()];
		is_gang_yao = new boolean[getTablePlayerNumber()];
		chi_color = new int[getTablePlayerNumber()];
		
		//下炮
		if (has_rule(Constants_MJ_PING_XIANG_258.GAME_RULE_MAI_ZI)) {
			Arrays.fill(_player_result.haspiao, (byte)1);
			this.set_handler(this._handler_pao);
			this._handler_pao.exe(this);
			return true;
		} else {
			Arrays.fill(_player_result.pao, 0);
		}
		
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
		exe_dispatch_card(_current_player, GameConstants.GANG_TYPE_HONG_ZHONG, GameConstants.DELAY_SEND_CARD_DELAY);

		return true;
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {
		if (card_type == Constants_MJ_PING_XIANG_258.HU_CARD_TYPE_ZI_MO) {
			chiHuRight.opr_or(Constants_MJ_PING_XIANG_258.CHR_ZI_MO);//自摸
		} else if (card_type == Constants_MJ_PING_XIANG_258.HU_CARD_TYPE_QIANG_GANG) {
			chiHuRight.opr_or(Constants_MJ_PING_XIANG_258.CHR_QIANG_GANG);//抢杠胡
		} else if (card_type == Constants_MJ_PING_XIANG_258.HU_CARD_TYPE_DIAN_PAO) {
			chiHuRight.opr_or(Constants_MJ_PING_XIANG_258.CHR_DIAN_PAO);//点炮胡
		} else if (card_type == Constants_MJ_PING_XIANG_258.HU_CARD_TYPE_GANG_KAI_HUA) {
			chiHuRight.opr_or(Constants_MJ_PING_XIANG_258.CHR_GANG_SHANG_HUA);//杠上开花
		} else if (card_type == Constants_MJ_PING_XIANG_258.HU_CARD_TYPE_GANG_SHANG_PAO) {
			chiHuRight.opr_or(Constants_MJ_PING_XIANG_258.CHR_GANG_SHANG_PAO);//杠上炮
		}

		//拷贝把当前牌加入手牌
		int cur_card_index = _logic.switch_to_card_index(cur_card);
		int[] temp_cards_index = Arrays.copyOf(cards_index, cards_index.length);
		temp_cards_index[cur_card_index]++;
		
		//-1 不是小七对 0是 1豪华 2双豪华 3三豪华
		int xiao_qi_dui = -1;
		if(has_rule(Constants_MJ_PING_XIANG_258.GAME_RULE_KE_HU_QI_DUI)){
			xiao_qi_dui = is_xiao_qi_dui(temp_cards_index, weave_count);
		}
		
		//判断十三烂 
		int shiSanLan = isShiSanLan(temp_cards_index, weave_count);
		
		//0字一色1清一色
		int se_count = get_se_count(temp_cards_index, weaveItems, weave_count);
		
		//大碰胡
		boolean is_peng_hu = !_logic.exist_eat(weaveItems, weave_count) && AnalyseCardUtil.analyse_peng_hu_by_cards_index(cards_index, cur_card_index, null, 0);
		
		//将将胡---  全部由258组成的牌型，不用满足基本胡牌条件(大碰胡存在时没有乱将)
		boolean is_jiang_jiang_hu = !is_peng_hu && is_jiang_jiang_hu(temp_cards_index, weaveItems, weave_count);
		
		//是否胡牌牌型
		boolean analyse_win_by_cards_index = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, cur_card_index, null, 0);
		if(!(analyse_win_by_cards_index || xiao_qi_dui != -1 || shiSanLan != 0 || is_jiang_jiang_hu || se_count == 0)){
			return GameConstants.WIK_NULL;
		}
		
		//258将
		boolean analyse_258_by_cards_index = AnalyseCardUtil.analyse_258_by_cards_index(cards_index, cur_card_index, null, 0);
		
		//一条龙
		boolean is_yi_tiao_long = is_yi_tiao_long(temp_cards_index, weaveItems, weave_count, false);
		
		if(_logic.exist_eat(weaveItems, weave_count) && se_count != 1)//吃牌后只能胡清一色
			return GameConstants.WIK_NULL;
		if(is_peng_hu && se_count == 1 && analyse_258_by_cards_index)
			chiHuRight.opr_or(Constants_MJ_PING_XIANG_258.CHR_QYS_DA_PENG258);
		else if(se_count == 1 && is_yi_tiao_long && analyse_258_by_cards_index)
			chiHuRight.opr_or(Constants_MJ_PING_XIANG_258.CHR_QYS_YI_TIAO_LONG258);
		else{
			if(se_count == 1){
				if(analyse_258_by_cards_index)
					chiHuRight.opr_or(Constants_MJ_PING_XIANG_258.CHR_QYS_ZHEN);
				else
					chiHuRight.opr_or(Constants_MJ_PING_XIANG_258.CHR_QING_YI_SE);
				if(is_peng_hu)
					chiHuRight.opr_or(Constants_MJ_PING_XIANG_258.CHR_DA_PENG_HU);
			}
			if(se_count == 0 && is_peng_hu)
				chiHuRight.opr_or(Constants_MJ_PING_XIANG_258.CHR_DA_PENG_HU);
			if(is_peng_hu && analyse_258_by_cards_index)
				chiHuRight.opr_or(Constants_MJ_PING_XIANG_258.CHR_DA_PENG_HU);
			if(is_yi_tiao_long)
				chiHuRight.opr_or(Constants_MJ_PING_XIANG_258.CHR_YI_TIAO_LONG);
		}
		if(is_jiang_jiang_hu)
			chiHuRight.opr_or(Constants_MJ_PING_XIANG_258.CHR_LUAN_JIANG);
		if(se_count == 0)
			chiHuRight.opr_or(Constants_MJ_PING_XIANG_258.CHR_ZI_YI_SE);
		if(xiao_qi_dui == 0)
			chiHuRight.opr_or(Constants_MJ_PING_XIANG_258.CHR_QI_DUI);
		if(xiao_qi_dui == 1)
			chiHuRight.opr_or(Constants_MJ_PING_XIANG_258.CHR_LONG_QI_DUI);
		if(xiao_qi_dui == 2)
			chiHuRight.opr_or(Constants_MJ_PING_XIANG_258.CHR_2LONG_QI_DUI);
		if(xiao_qi_dui == 3)
			chiHuRight.opr_or(Constants_MJ_PING_XIANG_258.CHR_3LONG_QI_DUI);
		if(shiSanLan != 0)
			chiHuRight.opr_or(Constants_MJ_PING_XIANG_258.CHR_LUAN_HU);
		if(GRR._left_card_count == 0 && (card_type == Constants_MJ_PING_XIANG_258.HU_CARD_TYPE_ZI_MO || card_type == Constants_MJ_PING_XIANG_258.HU_CARD_TYPE_GANG_KAI_HUA))
			chiHuRight.opr_or(Constants_MJ_PING_XIANG_258.CHR_HAI_DI_PAI);
		
		if(chiHuRight.m_dwRight[0] < 0x40){
			if(!analyse_258_by_cards_index)
				return GameConstants.WIK_NULL;
			if(card_type != Constants_MJ_PING_XIANG_258.HU_CARD_TYPE_QIANG_GANG){
				if(card_type != Constants_MJ_PING_XIANG_258.HU_CARD_TYPE_ZI_MO &&
						card_type != Constants_MJ_PING_XIANG_258.HU_CARD_TYPE_GANG_KAI_HUA && card_type != GameConstants.CHR_ZI_MO)
					return GameConstants.WIK_NULL;
			}
			chiHuRight.opr_or(Constants_MJ_PING_XIANG_258.CHR_PING_HU);
		}
  		
  		//碰碰胡
		return GameConstants.WIK_CHI_HU;
	}

	private boolean is_jiang_jiang_hu(int[] temp_cards_index,
			WeaveItem[] weaveItems, int weave_count) {
		for(int i = 0; i < weave_count; i++){
			int value = _logic.get_card_value(weaveItems[i].center_card);
			if((value != 2 && value != 5 && value != 8) || weaveItems[i].center_card > 0x29)
				return false;
		}
		for(int i = 0; i < GameConstants.MAX_ZI_FENG; i++){
			if(temp_cards_index[i] != 0){
				int data = _logic.switch_to_card_data(i);
				int value = _logic.get_card_value(data);
				if((value != 2 && value != 5 && value != 8) || data > 0x29)
					return false;
			}
		}
		return true;
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

			// 可以胡的情况 判断 ,强杠胡不用判断是否过圈
			if (_playerStatus[i].is_chi_hu_round()) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						Constants_MJ_PING_XIANG_258.HU_CARD_TYPE_QIANG_GANG, i);

				// 结果判断
				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					if(is_gang_yao[i] && fa_pai_count[i] == 0)
						_playerStatus[i].add_action_card(-1, card, GameConstants.WIK_CHI_HU, seat_index);
					else
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


		countCardType(chr, seat_index);
		int score = getPaiXingScore(chr, seat_index);
		int bird = GRR._player_niao_count[seat_index] * (has_rule(Constants_MJ_PING_XIANG_258.GAME_RULE_BIRD_SCORE_2)?2:1);
		int pao = _player_result.pao[seat_index];
		int flyBirdScore = getFlyBirdScore();
		bird += flyBirdScore + pao;
		/////////////////////////////////////////////// 算分//////////////////////////
		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				GRR._game_score[i] -= score + bird + _player_result.pao[i];
				GRR._game_score[seat_index] += score + bird + _player_result.pao[i];
			}
		}else {//放炮
			GRR._game_score[provide_index] -= score + bird + _player_result.pao[provide_index];
			GRR._game_score[seat_index] += score + bird + _player_result.pao[provide_index];
		}
		// 设置变量
		GRR._provider[seat_index] = provide_index;
		_status_gang = false;
		_status_gang_hou_pao = false;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);
	}

	private int getFlyBirdScore() {
		int score = 0;
		if(has_rule(Constants_MJ_PING_XIANG_258.GAME_RULE_ZHUA_FEI_BIRD)){
			int data = GRR._cards_data_niao[0];
			if(data > 0){
				score = _logic.get_card_value(data);
				if(data == 0x35)
					score = 10;
				if(data == 0x36)
					score = 2;
				if(data == 0x37)
					score = 3;
			}
		}
		return score;
	}

	private int getPaiXingScore(ChiHuRight chr, int seat_index) {
		int da_hu_count = 0;
		if(!chr.opr_and(Constants_MJ_PING_XIANG_258.CHR_GANG_SHANG_PAO).is_empty())
			da_hu_count++;
		if(!chr.opr_and(Constants_MJ_PING_XIANG_258.CHR_GANG_SHANG_HUA).is_empty())
			da_hu_count++;
		if(!chr.opr_and(Constants_MJ_PING_XIANG_258.CHR_QYS_DA_PENG258).is_empty())
			da_hu_count += 3;
		else if(!chr.opr_and(Constants_MJ_PING_XIANG_258.CHR_QYS_YI_TIAO_LONG258).is_empty())
			da_hu_count += 3;
		else{
			if(!chr.opr_and(Constants_MJ_PING_XIANG_258.CHR_DA_PENG_HU).is_empty())
				da_hu_count++;
			if(!chr.opr_and(Constants_MJ_PING_XIANG_258.CHR_QING_YI_SE).is_empty())
				da_hu_count++;
			
			if(!chr.opr_and(Constants_MJ_PING_XIANG_258.CHR_QYS_ZHEN).is_empty())
				da_hu_count += 2;
				
			if(!chr.opr_and(Constants_MJ_PING_XIANG_258.CHR_YI_TIAO_LONG).is_empty())
				da_hu_count++;
		}
		if(!chr.opr_and(Constants_MJ_PING_XIANG_258.CHR_LUAN_JIANG).is_empty())
			da_hu_count += 1;
		if(!chr.opr_and(Constants_MJ_PING_XIANG_258.CHR_ZI_YI_SE).is_empty())
			da_hu_count += 2;
		if(!chr.opr_and(Constants_MJ_PING_XIANG_258.CHR_QI_DUI).is_empty())
			da_hu_count += 1;
		if(!chr.opr_and(Constants_MJ_PING_XIANG_258.CHR_LONG_QI_DUI).is_empty())
			da_hu_count += 2;
		if(!chr.opr_and(Constants_MJ_PING_XIANG_258.CHR_2LONG_QI_DUI).is_empty())
			da_hu_count += 3;
		if(!chr.opr_and(Constants_MJ_PING_XIANG_258.CHR_3LONG_QI_DUI).is_empty())
			da_hu_count += 4;
		if(!chr.opr_and(Constants_MJ_PING_XIANG_258.CHR_LUAN_HU).is_empty())
			da_hu_count += 2;
		int score = 1;
		if(da_hu_count > 0){
			score = 4 * (1 << (da_hu_count - 1));
		}
			
		if(!chr.opr_and(Constants_MJ_PING_XIANG_258.CHR_HAI_DI_PAI).is_empty())
			score += score / 2;
		return score;
	}
	
	@Override
	protected void set_result_describe() {
		int chrTypes;
		long type = 0;
		for (int player = 0; player < this.getTablePlayerNumber(); player++) {
			StringBuilder result = new StringBuilder("");

			chrTypes = GRR._chi_hu_rights[player].type_count;

			for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
				type = GRR._chi_hu_rights[player].type_list[typeIndex];

				if (GRR._chi_hu_rights[player].is_valid()) {
					if (type == Constants_MJ_PING_XIANG_258.CHR_DIAN_PAO) {
						result.append(" 点炮胡");
					}
					if (type == Constants_MJ_PING_XIANG_258.CHR_ZI_MO) {
						result.append(" 自摸");
					}
					if (type == Constants_MJ_PING_XIANG_258.CHR_PING_HU) {
						result.append(" 平胡");
					}
					if (type == Constants_MJ_PING_XIANG_258.CHR_LUAN_JIANG) {
						result.append(" 乱将");
					}
					if (type == Constants_MJ_PING_XIANG_258.CHR_QIANG_GANG) {
						result.append(" 抢杠胡");
					}
					if (type == Constants_MJ_PING_XIANG_258.CHR_QI_DUI) {
						result.append(" 七对");
					}
					if (type == Constants_MJ_PING_XIANG_258.CHR_LONG_QI_DUI) {
						result.append(" 龙七对");
					}
					if (type == Constants_MJ_PING_XIANG_258.CHR_2LONG_QI_DUI) {
						result.append(" 双龙七对");
					}
					if (type == Constants_MJ_PING_XIANG_258.CHR_3LONG_QI_DUI) {
						result.append(" 三龙七对");
					}
					if (type == Constants_MJ_PING_XIANG_258.CHR_QING_YI_SE) {
						result.append(" 假清一色");
					}
					if (type == Constants_MJ_PING_XIANG_258.CHR_QYS_ZHEN) {
						result.append(" 真清一色");
					}
					if (type == Constants_MJ_PING_XIANG_258.CHR_DA_PENG_HU) {
						result.append(" 大碰胡");
					}
					if (type == Constants_MJ_PING_XIANG_258.CHR_GANG_SHANG_HUA) {
						result.append(" 杠上开花");
					}
					if (type == Constants_MJ_PING_XIANG_258.CHR_ZI_YI_SE) {
						result.append(" 字一色");
					}
					if (type == Constants_MJ_PING_XIANG_258.CHR_LUAN_HU) {
						result.append(" 烂胡");
					}
					if (type == Constants_MJ_PING_XIANG_258.CHR_YI_TIAO_LONG) {
						result.append(" 一条龙");
					}
					if (type == Constants_MJ_PING_XIANG_258.CHR_QYS_DA_PENG258) {
						result.append(" 清一色  大碰胡（258将）");
					}
					if (type == Constants_MJ_PING_XIANG_258.CHR_QYS_YI_TIAO_LONG258) {
						result.append(" 清一色  一条龙（258将）");
					}
					if (type == Constants_MJ_PING_XIANG_258.CHR_HAI_DI_PAI) {
						result.append(" 海底牌");
					}
				} else if (type == Constants_MJ_PING_XIANG_258.CHR_FANG_PAO) {
					result.append(" 放炮");
				} else if (type == Constants_MJ_PING_XIANG_258.CHR_GANG_SHANG_PAO) {
					result.append(" 杠上炮");
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
			
			if(GRR._player_niao_count[player] > 0){
				result.append(" 中鸟X"+GRR._player_niao_count[player]);
			}
			if(_player_result.pao[player] > 0){
				result.append(" 买" + _player_result.pao[player] + "子");
			}

			GRR._result_des[player] = result.toString();
		}
	}
	
	
	private static final int[][] birdVaule = new int[][]{{1,5,9},{2,6},{3,7},{4,8}};
	private static final int[][] birdVaule3 = new int[][]{{1,4,7},{2,5,8},{3,6,9}};
	public void set_pick_direction_niao_cards(int cards_data[], int card_num, int seat_index) {
		int seat = (seat_index - _cur_banker + getTablePlayerNumber()) % getTablePlayerNumber();
		for (int i = 0; i < card_num; i++) {
			int nValue = _logic.get_card_value(cards_data[i]);
			boolean flag = false;
			if(getTablePlayerNumber() == 4){
				for(int v = 0; v < birdVaule[seat].length; v++){
					if(nValue == birdVaule[seat][v]){
						flag = true;
						break;
					}
				}
			}else{
				for(int v = 0; v < birdVaule3[seat].length; v++){
					if((nValue == birdVaule3[seat][v] && cards_data[i] != 0x35) || (seat_index == _cur_banker && cards_data[i] == 0x35)){
						flag = true;
						break;
					}
				}
			}
			if (flag) {
				GRR._player_niao_cards[seat_index][i] = GRR._cards_data_niao[i] + GameConstants.DING_NIAO_VALID;
				GRR._player_niao_count[seat_index]++;
			}else{
				GRR._player_niao_cards[seat_index][i] = GRR._cards_data_niao[i];
			}

		}
	}
	
	/**
	 * 设置鸟
	 */
	public void set_niao_card(int seat_index, boolean is_yi_pao_duo_xiang) {

		for (int i = 0; i < GameConstants.MAX_NIAO_CARD; i++) {
			GRR._cards_data_niao[i] = GameConstants.INVALID_VALUE;
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			GRR._player_niao_count[i] = 0;
			for (int j = 0; j < GameConstants.MAX_NIAO_CARD; j++) {
				GRR._player_niao_cards[i][j] = GameConstants.INVALID_VALUE;
			}
		}

		GRR._show_bird_effect = true;
		int bird_num = get_bird_num();
		GRR._count_niao = bird_num > GRR._left_card_count ? GRR._left_card_count : bird_num;

		if (GRR._count_niao > GameConstants.ZHANIAO_0) {
			int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
			_logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count, GRR._count_niao, cbCardIndexTemp);
			GRR._left_card_count -= GRR._count_niao;
			_logic.switch_to_cards_data(cbCardIndexTemp, GRR._cards_data_niao);
			if(is_yi_pao_duo_xiang){
				if(has_rule(Constants_MJ_PING_XIANG_258.GAME_RULE_ZHUA_FEI_BIRD)){
					GRR._player_niao_cards[_cur_banker][0] = GRR._cards_data_niao[0] + 1000;
					return;
				}
				set_pick_niao_cards_duo_xiang(GRR._cards_data_niao, GRR._count_niao);
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if(GRR._chi_hu_rights[i].is_valid())
						set_pick_niao_cards_duo_xiang(GRR._cards_data_niao, GRR._count_niao, i);
				}
			}else{
				if(has_rule(Constants_MJ_PING_XIANG_258.GAME_RULE_ZHUA_FEI_BIRD)){
					GRR._player_niao_cards[seat_index][0] = GRR._cards_data_niao[0] + 1000;
					return;
				}
				set_pick_niao_cards(GRR._cards_data_niao, GRR._count_niao, seat_index);
			}
		}
	}
	
	public void set_pick_niao_cards_duo_xiang(int cards_data[], int card_num) {
		for (int i = 0; i < card_num; i++) {
			int nValue = _logic.get_card_value(cards_data[i]);
			if ((nValue == 1 || nValue == 5 || nValue == 9) && cards_data[i] != 31) {
				GRR._player_niao_cards[_cur_banker][i] = GRR._cards_data_niao[i] + GameConstants.DING_NIAO_VALID;
			}else{
				GRR._player_niao_cards[_cur_banker][i] = GRR._cards_data_niao[i] + 200;
			}

		}
	}
	
	public void set_pick_niao_cards_duo_xiang(int cards_data[], int card_num, int seat_index) {
		for (int i = 0; i < card_num; i++) {
			int nValue = _logic.get_card_value(cards_data[i]);
			if ((nValue == 1 || nValue == 5 || nValue == 9) && cards_data[i] != 31) {
				GRR._player_niao_count[seat_index]++;
			}
		}
	}
	
	public void set_pick_niao_cards(int cards_data[], int card_num, int seat_index) {
		for (int i = 0; i < card_num; i++) {
			int nValue = _logic.get_card_value(cards_data[i]);
			if ((nValue == 1 || nValue == 5 || nValue == 9) && cards_data[i] != 31) {
				GRR._player_niao_cards[seat_index][i] = GRR._cards_data_niao[i] + GameConstants.DING_NIAO_VALID;
				GRR._player_niao_count[seat_index]++;
			}else{
				GRR._player_niao_cards[seat_index][i] = GRR._cards_data_niao[i] + 200;
			}

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

			if (GRR._left_card_count > 0 && !is_gang_yao[i]) {
				if (i == get_banker_next_seat(seat_index) && (chi_color[i] == 0 || (chi_color[i] - 1) == _logic.get_card_color(card)) && has_rule(Constants_MJ_PING_XIANG_258.GAME_RULE_KE_CHI_PAI)) {
					action = _logic.check_chi(GRR._cards_index[i], card);

					if ((action & GameConstants.WIK_RIGHT) != 0) {
						_playerStatus[i].add_action(GameConstants.WIK_RIGHT);
						_playerStatus[i].add_chi(card, GameConstants.WIK_RIGHT, seat_index);
					}
					if ((action & GameConstants.WIK_CENTER) != 0) {
						_playerStatus[i].add_action(GameConstants.WIK_CENTER);
						_playerStatus[i].add_chi(card, GameConstants.WIK_CENTER, seat_index);
					}
					if ((action & GameConstants.WIK_LEFT) != 0) {
						_playerStatus[i].add_action(GameConstants.WIK_LEFT);
						_playerStatus[i].add_chi(card, GameConstants.WIK_LEFT, seat_index);
					}

					if (_playerStatus[i].has_action()) {
						bAroseAction = true;
					}
				}
				boolean can_peng = true;
				int[] tmp_cards_data = _playerStatus[i].get_cards_abandoned_peng();
				for (int x = 0; x < GameConstants.MAX_ABANDONED_CARDS_COUNT; x++) {
					if (tmp_cards_data[x] == card) {
						can_peng = false;
						break;
					}
				}
				action = _logic.check_peng(GRR._cards_index[i], card);
				if (can_peng && action != 0) {
					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);
					bAroseAction = true;
				}

				action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
				if(!is_chi_same_color(i))
            			action = 0;
				if (action != 0) {
					playerStatus.add_action(GameConstants.WIK_GANG);
					playerStatus.add_gang(card, seat_index, 1); // 加上杠
					if(is_can_gang_yao(i, card)){
						playerStatus.add_action(Constants_MJ_PING_XIANG_258.WIK_GANG_YAO);
						add_gang_yao(playerStatus, card, i, 1);
					}
					bAroseAction = true;
				}
			}
			
			if (_playerStatus[i].is_chi_hu_round() && !has_rule(Constants_MJ_PING_XIANG_258.GAME_RULE_ZI_MO_HU)) {
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				
				int card_type = Constants_MJ_PING_XIANG_258.HU_CARD_TYPE_DIAN_PAO;
				if(type == Constants_MJ_PING_XIANG_258.HU_CARD_TYPE_GANG_KAI_HUA)
					card_type = Constants_MJ_PING_XIANG_258.HU_CARD_TYPE_GANG_SHANG_PAO;
				action = analyse_chi_hu_card(GRR._cards_index[i],
						GRR._weave_items[i], cbWeaveCount, card, chr,
						card_type, i);
				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					if(is_gang_yao[i] && fa_pai_count[i] == 0)
						_playerStatus[i].add_action_card(-1, card, GameConstants.WIK_CHI_HU, seat_index);
					else
						_playerStatus[i].add_chi_hu(card, seat_index);
					bAroseAction = true;
				}
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
					GameConstants.CHR_ZI_MO, seat_index)) {
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
	
	
	
	public int get_se_count(int cards_index[], WeaveItem weaveItem[], int weaveCount) {
		int cbQueYiMenColor[] = new int[] { 1, 1, 1 };// 缺一色
		boolean has_feng = false;
		// 计算单牌
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cards_index[i];// 数量
			if (cbCardCount == 0) {
				continue;
			}
			int card = _logic.switch_to_card_data(i);
			// 缺一色：起完牌后，玩家手上筒、索、万任缺一门，即可胡牌。（等同小胡自摸）
			int cbCardColor = _logic.get_card_color(card);
			if (cbCardColor > 2){
				has_feng = true;
				continue;
			}
			cbQueYiMenColor[cbCardColor] = 0;
		}
		for (int i = 0; i < weaveCount; i++) {
			int cbCardColor = _logic.get_card_color(weaveItem[i].center_card);
			// 字牌过滤
			if (cbCardColor > 2){
				has_feng = true;
				continue;
			}
			cbQueYiMenColor[cbCardColor] = 0;
		}
		int count = 0;
		for (int i = 0; i < 3; i++) {
			if (cbQueYiMenColor[i] == 0) {
				count += 1;
			}
		}
		if(count != 0 && has_feng)
			count++;
		return count;
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
    
    // 杠牌分析 包括补杠 check_weave检查补杠
    public int analyse_gang(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount,
            GangCardResult gangCardResult, boolean check_weave, int cards_abandoned_gang[], int seat_index) {
        // 设置变量
        int cbActionMask = GameConstants.WIK_NULL;
        if(!is_chi_same_color(seat_index))
        	return cbActionMask;

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
            // 组合杠牌，包括以前能杠，但是不杠，发牌之后而选择补杠的
            for (int i = 0; i < cbWeaveCount; i++) {
                if (WeaveItem[i].weave_kind == GameConstants.WIK_PENG) {
                    for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
                        if (cards_index[j] != 1) {
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
            	//荒庄荒杠
                if(reason == GameConstants.Game_End_NORMAL){
                	for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
                		for (int k = 0; k < getTablePlayerNumber(); k++) {
                			lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
                		}
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
                for (int j = 0; j < GRR._count_niao; j++) {
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
                	if (_logic.is_magic_card(GRR._chi_hu_card[i][j])) {
						hc.addItem(GRR._chi_hu_card[i][j]
								+ GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
					} else {
						hc.addItem(GRR._chi_hu_card[i][j]);
					}
                }
                
				if (_logic.is_magic_card(GRR._chi_hu_card[i][0])) {
					game_end.addHuCardData(GRR._chi_hu_card[i][0]
							+ GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
				} else {
					game_end.addHuCardData(GRR._chi_hu_card[i][0]);
				}

                game_end.addHuCardArray(hc);
            }

            // 现在权值只有一位
            long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

            // 设置胡牌描述
            this.set_result_describe();

            for (int i = 0; i < getTablePlayerNumber(); i++) {
                GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

                Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
                for (int j = 0; j < GRR._card_count[i]; j++) {
                	if (_logic.is_magic_card(GRR._cards_data[i][j])) {
						cs.addItem(GRR._cards_data[i][j]
								+ GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
					} else {
						cs.addItem(GRR._cards_data[i][j]);
					}
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
                game_end.addGameScore(GRR._game_score[i]);// 放炮的人？
                game_end.addGangScore(lGangScore[i]);// 杠牌得分
                game_end.addStartHuScore(GRR._start_hu_score[i]);
                game_end.addResultDes(GRR._result_des[i]);

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
        roomResponse.setGameEnd(game_end);

        this.send_response_to_room(roomResponse);

        record_game_round(game_end);
        for (int j = 0; j < getTablePlayerNumber(); j++) {
        	_player_result.biaoyan[j] = 0;
		}

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
    
	public int get_bird_num() {
		int num = 0;
		if(has_rule(Constants_MJ_PING_XIANG_258.GAME_RULE_CATCH_TWO_BIRD))
			num = 2;
		if(has_rule(Constants_MJ_PING_XIANG_258.GAME_RULE_CATCH_FOUR_BIRD))
			num = 4;
		if(has_rule(Constants_MJ_PING_XIANG_258.GAME_RULE_CATCH_SIX_BIRD))
			num = 6;
		if(has_rule(Constants_MJ_PING_XIANG_258.GAME_RULE_CATCH_EIGHT_BIRD))
			num = 8;
		if(has_rule(Constants_MJ_PING_XIANG_258.GAME_RULE_ZHUA_FEI_BIRD))
			num = 1;
		return num;
	}
	
	public void add_gang_yao(PlayerStatus curPlayerStatus, int card, int provider, int p) {
		for (int i = 0; i < curPlayerStatus._weave_count; i++) {
			if ((curPlayerStatus._action_weaves[i].center_card == card) && (curPlayerStatus._action_weaves[i].weave_kind == Constants_MJ_PING_XIANG_258.WIK_GANG_YAO)) {
				return;
			}
		}
		curPlayerStatus._action_weaves[curPlayerStatus._weave_count].public_card = p;
		curPlayerStatus._action_weaves[curPlayerStatus._weave_count].center_card = card;
		curPlayerStatus._action_weaves[curPlayerStatus._weave_count].weave_kind = Constants_MJ_PING_XIANG_258.WIK_GANG_YAO;
		curPlayerStatus._action_weaves[curPlayerStatus._weave_count].provide_player = provider;
		curPlayerStatus._weave_count++;
	}
	
	public boolean is_can_gang_yao(int seat_index, int card) {
		if(!has_rule(Constants_MJ_PING_XIANG_258.GAME_RULE_GANG_PAI_DA_TOU) || is_gang_yao[seat_index])
			return false;
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = GRR._cards_index[seat_index][i];
		}
		cbCardIndexTemp[_logic.switch_to_card_index(card)] = 0;
		
		WeaveItem weaves[] = new WeaveItem[GameConstants.MAX_WEAVE];
		int weave_count = GRR._weave_count[seat_index];
		for (int i = 0; i < weave_count; i++) {
			weaves[i] = new WeaveItem();
			weaves[i].weave_kind = GRR._weave_items[seat_index][i].weave_kind;
			weaves[i].center_card = GRR._weave_items[seat_index][i].center_card;
			weaves[i].public_card = GRR._weave_items[seat_index][i].public_card;
			weaves[i].provide_player = GRR._weave_items[seat_index][i].provide_player;
		}
		if(weave_count < 4){//防止下标越界
			weaves[weave_count] = new WeaveItem();
			weaves[weave_count].weave_kind = GameConstants.WIK_GANG;
			weaves[weave_count].center_card = card;
			weaves[weave_count].public_card = 0;
			weaves[weave_count].provide_player = seat_index;
			weave_count++;
		}
		
		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			int cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaves,
					weave_count, cbCurrentCard, chr,
					GameConstants.CHR_ZI_MO, seat_index)) {
				count++;
			}
		}
		if(count > 0)
			return true;
			
		return false;
	}
	
    public int isShiSanLan1(int[] cards_index,int weaveCount) {
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
    
    public int isShiSanLan(int[] cards_index,int weaveCount) {
    	if (weaveCount != 0) {
    		return 0;
    	}
    	Map<Integer, Integer> map = new HashMap<Integer, Integer>();
    	for (int i = 0; i < GameConstants.MAX_ZI; i++) {
    		if(cards_index[i] > 1)
    			return 0;
    		if(cards_index[i] != 0){
    			int data = _logic.switch_to_card_data(i);
    			int value = _logic.get_card_value(data);
    			int color = _logic.get_card_color(data);
    			if(map.containsKey(value))
    				return 0;
    			map.put(value, color);
    		}
    	}
    	if(map.size() != 9)
    		return 0;
    	if(!(map.get(1) == map.get(4) && map.get(4) == map.get(7)))
    		return 0;
    	if(!(map.get(2) == map.get(5) && map.get(5) == map.get(8)))
    		return 0;
    	if(!(map.get(3) == map.get(6) && map.get(6) == map.get(9)))
    		return 0;
    	if(map.get(1) == map.get(2) || map.get(2) == map.get(3) || map.get(1) == map.get(3))
    		return 0;
    	
    	for (int i = GameConstants.MAX_ZI; i < GameConstants.MAX_ZI_FENG; i++) {
    		if (cards_index[i] > 1) {
    			return 0;
    		}
    	}
    	return 1;
    }
    
    public boolean is_yi_tiao_long(int cards_index[], WeaveItem weaveItem[], int weaveCount, boolean isCheckChi) {
		Set<Integer> set = new HashSet<Integer>();
		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			if(cards_index[i] == 0){
				set.add(_logic.get_card_color(_logic.switch_to_card_data(i)));
			}
 		}
		//手上有同花色1-9时检查能不能胡
		if(set.size() != 3){
			int[] copyOf = Arrays.copyOf(cards_index, cards_index.length);
			if(!set.contains(0)){
				for(int i = 0; i < 9; i++)
					copyOf[i]--;
			}else if(!set.contains(1)){
				for(int i = 9; i < 18; i++)
					copyOf[i]--;
			}else if(!set.contains(2)){
				for(int i = 18; i < 27; i++)
					copyOf[i]--;
			}
			boolean hu;
				hu = AnalyseCardUtil.analyse_feng_chi_by_cards_index(copyOf, -1, null, 0);
			if(hu)
				return hu;
		}
		if(!isCheckChi)
			return false; 
		// 检查有吃情况下，一条龙判断
		if(weaveCount == 0)
			return false;
		int[][] chi3 = new int[3][3]; 
		for (int i = 0; i < weaveCount; i++) {
			if(weaveItem[i].center_card < 0x31){
				int value = _logic.get_card_value(weaveItem[i].center_card);
				int color = _logic.get_card_color(weaveItem[i].center_card);
				boolean is123 = (value == 1 && weaveItem[i].weave_kind == GameConstants.WIK_LEFT) ||
						(value == 2 && weaveItem[i].weave_kind == GameConstants.WIK_CENTER) ||
						(value == 3 && weaveItem[i].weave_kind == GameConstants.WIK_RIGHT);
				boolean is456 = (value == 4 && weaveItem[i].weave_kind == GameConstants.WIK_LEFT) ||
						(value == 5 && weaveItem[i].weave_kind == GameConstants.WIK_CENTER) ||
						(value == 6 && weaveItem[i].weave_kind == GameConstants.WIK_RIGHT);
				boolean is789 = (value == 7 && weaveItem[i].weave_kind == GameConstants.WIK_LEFT) ||
						(value == 8 && weaveItem[i].weave_kind == GameConstants.WIK_CENTER) ||
						(value == 9 && weaveItem[i].weave_kind == GameConstants.WIK_RIGHT);
				//一个吃的情况
				if(is123){
					int[] copyOf = Arrays.copyOf(cards_index, cards_index.length);
					boolean flag = true;
					for(int c = color * 9 + 3; c < color * 9 + 9; c++){
						if(copyOf[c] == 0){
							flag = false;
							break;
						}
						copyOf[c]--;
					}
					boolean hu = flag && AnalyseCardUtil.analyse_feng_chi_by_cards_index(copyOf, -1, null, 0);
					if(hu)
						return hu;
				}
				if(is456){
					int[] copyOf = Arrays.copyOf(cards_index, cards_index.length);
					boolean flag = true;
					for(int c = color * 9 + 0; c < color * 9 + 3; c++){
						if(copyOf[c] == 0){
							flag = false;
							break;
						}
						copyOf[c]--;
					}
					for(int c = color * 9 + 6; c < color * 9 + 9; c++){
						if(copyOf[c] == 0){
							flag = false;
							break;
						}
						copyOf[c]--;
					}
					boolean hu = flag && AnalyseCardUtil.analyse_feng_chi_by_cards_index(copyOf, -1, null, 0);
					if(hu)
						return hu;
				}
				if(is789){
					int[] copyOf = Arrays.copyOf(cards_index, cards_index.length);
					boolean flag = true;
					for(int c = color * 9 + 0; c < color * 9 + 6; c++){
						if(copyOf[c] == 0){
							flag = false;
							break;
						}
						copyOf[c]--;
					}
					boolean hu = flag && AnalyseCardUtil.analyse_feng_chi_by_cards_index(copyOf, -1, null, 0);
					if(hu)
						return hu;
				}
				
				if(is123)
					chi3[color][0] = 1;
				if(is456)
					chi3[color][1] = 1;
				if(is789)
					chi3[color][2] = 1;
			}
		}
			
		for(int c = 0; c < 3; c++){
			//三个吃的情况
			if(chi3[c][0] == 1 && chi3[c][1] == 1 && chi3[c][2] == 1)
				return true;
			
			//两个吃的情况
			if(chi3[c][0] == 1 && chi3[c][1] == 1){
				int[] copyOf = Arrays.copyOf(cards_index, cards_index.length);
				boolean flag = true;
				for(int cd = c * 9 + 6; cd < c * 9 + 9; cd++){
					if(copyOf[cd] == 0){
						flag = false;
						break;
					}
					copyOf[cd]--;
				}
				boolean hu = flag && AnalyseCardUtil.analyse_feng_chi_by_cards_index(copyOf, -1, null, 0);
				if(hu)
					return hu;
			}
			if(chi3[c][0] == 1 && chi3[c][2] == 1){
				int[] copyOf = Arrays.copyOf(cards_index, cards_index.length);
				boolean flag = true;
				for(int cd = c * 9 + 3; cd < c * 9 + 6; cd++){
					if(copyOf[cd] == 0){
						flag = false;
						break;
					}
					copyOf[cd]--;
				}
				boolean hu = flag && AnalyseCardUtil.analyse_feng_chi_by_cards_index(copyOf, -1, null, 0);
				if(hu)
					return hu;
			}
			if(chi3[c][1] == 1 && chi3[c][2] == 1){
				int[] copyOf = Arrays.copyOf(cards_index, cards_index.length);
				boolean flag = true;
				for(int cd = c * 9 + 0; cd < c * 9 + 3; cd++){
					if(copyOf[cd] == 0){
						flag = false;
						break;
					}
					copyOf[cd]--;
				}
				boolean hu = flag && AnalyseCardUtil.analyse_feng_chi_by_cards_index(copyOf, -1, null, 0);
				if(hu)
					return hu;
			}
		}
		
		return false;
	}
    
    public boolean is_chi_same_color(int seat_index){
    	if(chi_color[seat_index] == 0)
    		return true;
    	int[] cards = GRR._cards_index[seat_index];
    	for(int i = 0; i < GameConstants.MAX_INDEX; i++){
    		if(cards[i] > 0){
    			if(chi_color[seat_index] - 1 != _logic.get_card_color(_logic.switch_to_card_data(i)))
    				return false;
    		}
    	}
    	return true;
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
  	
  	/**
	 * 后期需要添加的摇骰子的效果
	 */
	public void show_tou_zi(int seat_index) {
		tou_zi_dian_shu[0] = RandomUtil.getRandomNumber(Integer.MAX_VALUE) % 6 + 1;
		tou_zi_dian_shu[1] = RandomUtil.getRandomNumber(Integer.MAX_VALUE) % 6 + 1;

		operate_tou_zi_effect(tou_zi_dian_shu[0], tou_zi_dian_shu[1], time_for_tou_zi_animation, time_for_tou_zi_fade, seat_index);
	}
	
	/**
	 * 在牌桌上显示摇骰子的效果
	 * 
	 * @param tou_zi_one
	 *            骰子1的点数
	 * @param tou_zi_two
	 *            骰子2的点数
	 * @param time_for_animate
	 *            动画时间
	 * @param time_for_fade
	 *            动画保留时间
	 * @return
	 */
	public boolean operate_tou_zi_effect(int tou_zi_one, int tou_zi_two, int time_for_animate, int time_for_fade, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_EFFECT_ACTION);
		roomResponse.setEffectType(GameConstants.Effect_Action_SHAI_ZI);
		roomResponse.setTarget(seat_index);
		roomResponse.setEffectCount(2);
		roomResponse.addEffectsIndex(tou_zi_one);
		roomResponse.addEffectsIndex(tou_zi_two);
		roomResponse.setEffectTime(time_for_animate);
		roomResponse.setStandTime(time_for_fade);

		send_response_to_room(roomResponse);

		if (GRR != null)
			GRR.add_room_response(roomResponse);

		return true;
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
	
    
    @Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x11,0x11,0x11,0x11,0x15,0x15,0x15,0x15,0x22,0x22,0x22,0x29,0x03 };
		int[] cards_of_player1 = new int[] { 0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,0x09,0x09,0x03,0x03 };
		int[] cards_of_player2 = new int[] { 0x11,0x12,0x13,0x02,0x02,0x02,0x03,0x03,0x03,0x05,0x05,0x09,0x09 };
		int[] cards_of_player3 = new int[] { 0x21,0x21,0x21,0x11,0x11,0x11,0x19,0x19,0x19,0x29,0x29,0x29,0x03 };

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
