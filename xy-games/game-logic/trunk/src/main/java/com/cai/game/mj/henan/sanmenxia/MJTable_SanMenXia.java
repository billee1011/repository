package com.cai.game.mj.henan.sanmenxia;

import java.util.Arrays;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.RedisConstant;
import com.cai.common.define.ECardType;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.SpringService;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.handler.MJHandlerFinish;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.game.util.GameUtilConstants;
import com.cai.redis.service.RedisService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJTable_SanMenXia extends AbstractMJTable {
	private static final long serialVersionUID = 9171444142458092230L;

	public MJHandlerPao_SanMenXia _handler_pao_henna;
	public MJHandlerQiShouHun_SanMenXia _handler_qishou_hun;
	public MJHandlerHun_SanMenXia _handler_hun;
	public MJHandlerGangSelectCard_SanMenXia _handler_select_card;
	protected int select_gang_card = -1;
	protected static final int LIUPAI = 20;// 流牌
	protected int hunweizi = -1;

	@Override
	protected void onInitTable() {
		_handler_dispath_card = new MJHandlerDispatchCard_SanMenXia();
		_handler_out_card_operate = new MJHandlerOutCardOperate_SanMenXia();
		_handler_gang = new MJHandlerGang_SanMenXia();
		_handler_chi_peng = new MJHandlerChiPeng_SanMenXia();

		_handler_qishou_hun = new MJHandlerQiShouHun_SanMenXia();
		_handler_pao_henna = new MJHandlerPao_SanMenXia();
		_handler_hun = new MJHandlerHun_SanMenXia();
		_handler_select_card = new MJHandlerGangSelectCard_SanMenXia();

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			_player_result.pao[i] = 0;
			_player_result.biaoyan[i] = 0;
		}

		_handler_finish = new MJHandlerFinish();
	}

	@Override
	protected void initBanker() {
		if (has_rule(GameConstants.GAME_RULE_HENAN_THREE)) {
			int banker = RandomUtil.getRandomNumber(GameConstants.GAME_PLAYER - 1);
			_cur_banker = banker;
		} else {
			int banker = RandomUtil.getRandomNumber(GameConstants.GAME_PLAYER);
			_cur_banker = banker;
		}
	}

	@Override
	protected boolean on_handler_game_start() {
		reset_init_data();

		progress_banker_select();

		_game_status = GameConstants.GS_MJ_PLAY;

		GRR._banker_player = _cur_banker;
		_current_player = GRR._banker_player;

		if (has_rule(GameConstants.GAME_RULE_DAI_FENG_SMX)) {
			_repertory_card = new int[GameConstants.CARD_COUNT_DAI_FENG_SMX];
			shuffle(_repertory_card, GameConstants.CARD_DATA_DAI_FENG_SMX);
		} else {
			_repertory_card = new int[GameConstants.CARD_COUNT_BU_DAI_FENG_SMX];
			shuffle(_repertory_card, GameConstants.CARD_DATA_BU_DAI_FENG_SMX);
		}

		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();

		getLocationTip();

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GRR._cards_index[i].length; j++) {
				if (GRR._cards_index[i][j] == 4) {
					MongoDBServiceImpl.getInstance().card_log(get_players()[i], ECardType.anLong, "", GRR._cards_index[i][j], 0l, getRoom_id());
				}
			}
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			istrustee[i] = false;
		}


		on_game_start();

		return true;
	}
	
	/**
	 * 参数初始化
	 */
	protected void onInitParam() {
		hunweizi = -1;
		_logic.clean_magic_cards();
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			GRR._score_of_gangpao[i] = 0;
			_player_result.biaoyan[i] = 0;
		}
	}

	public boolean on_game_start() {
		
		onInitParam();
		
		if (has_rule(GameConstants.GAME_RULE_MAI_PAO_SMX)) {
			set_handler(_handler_pao_henna);
			_handler_pao_henna.exe(this);
			return true;
		} else {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				_player_result.pao[i] = 0;
				_player_result.qiang[i] = 0;
			}
		}

		show_tou_zi(GRR._banker_player);
		
		GRR._banker_player = _current_player = _cur_banker;

		_game_status = GameConstants.GS_MJ_PLAY;

		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants.MAX_COUNT];
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			load_room_info_data(roomResponse);
			load_common_status(roomResponse);

			//if (_cur_round == 1) {
				load_player_info_data(roomResponse);
			//}
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
		load_player_info_data(roomResponse);
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
		
		set_two_especial_card();

		if (has_rule(GameConstants.GAME_RULE_SHANG_HUN_SMX) || has_rule(GameConstants.GAME_RULE_YUAN_HUN_SMX)) {
			exe_hun(GRR._banker_player);
			return true;
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i]._hu_card_count = get_henan_ting_card(_playerStatus[i]._hu_cards, GRR._cards_index[i], GRR._weave_items[i],
					GRR._weave_count[i]);
			if (_playerStatus[i]._hu_card_count > 0) {
				operate_chi_hu_cards(i, _playerStatus[i]._hu_card_count, _playerStatus[i]._hu_cards);
			}
		}

		exe_dispatch_card(_current_player, GameConstants.WIK_NULL, 0);

		return true;
	}
	
	protected void set_two_especial_card(){
		int two_especial_card[] = new int[2];
		two_especial_card[0] = _repertory_card[_all_card_len - 1];
		two_especial_card[1] = _repertory_card[_all_card_len - 2];
		//第一位是混牌，第二、三位是杠后选的两张牌，第四位是上混翻的牌，第五位是混牌的位置，第六位是杠的个数
		GRR._especial_card_count = 6;
		GRR._especial_show_cards[0] = -1;
		GRR._especial_show_cards[1] = two_especial_card[0];
		GRR._especial_show_cards[2] = two_especial_card[1];
		GRR._especial_show_cards[3] = -1;
		GRR._especial_show_cards[4] = -1;
		GRR._especial_show_cards[5] = -1;
		operate_show_card(this._cur_banker, GameConstants.Show_Gang_Card, 2, two_especial_card,GameConstants.INVALID_SEAT);
		
	}

	protected boolean on_handler_game_finish(int seat_index, int reason) {
		int real_reason = reason;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_ready[i] = 0;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();

		roomResponse.setLeftCardCount(0);

		this.load_common_status(roomResponse);
		this.load_room_info_data(roomResponse);

		RoomInfo.Builder room_info = getRoomInfo();
		game_end.setRoomInfo(room_info);
		game_end.setRoundOverType(0);
		game_end.setGamePlayerNumber(getTablePlayerNumber());
		game_end.setRoomOverType(0);

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			game_end.addPao(_player_result.pao[i]);
		}

		setGameEndBasicPrama(game_end);

		if (GRR != null) {
			game_end.setRoundOverType(1);
			game_end.setStartTime(GRR._start_time);
			game_end.setGameTypeIndex(GRR._game_type_index);
			roomResponse.setLeftCardCount(GRR._left_card_count);

			for (int i = 0; i < GRR._especial_card_count; i++) {
				game_end.addEspecialShowCards(GRR._especial_show_cards[i] + GameConstants.CARD_ESPECIAL_TYPE_HUN);
			}

			GRR._end_type = reason;
			float lGangScore[] = new float[getTablePlayerNumber()];

			if (reason == GameConstants.Game_End_NORMAL ) {
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
						for (int k = 0; k < getTablePlayerNumber(); k++) {
							lGangScore[k] += GRR._gang_score[i].scores[j][k];
						}
					}

					for (int j = 0; j < getTablePlayerNumber(); j++) {
						_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
					}
				}

				for (int i = 0; i < getTablePlayerNumber(); i++) {
					GRR._game_score[i] += lGangScore[i];
					GRR._game_score[i] += GRR._start_hu_score[i];
					if(this.has_rule(GameConstants.GAME_RULE_DAI_GANG_PAO_SMX))
						GRR._game_score[i] += GRR._score_of_gangpao[i];
					_player_result.game_score[i] += GRR._game_score[i];
				}
			}
			//不是正常结束去掉杠跑分
			if(reason != GameConstants.Game_End_NORMAL){
				for(int i = 0;i < getTablePlayerNumber();i++){
					GRR._score_of_gangpao[i] = 0;
				}
			}
			

			this.load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);

			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);
			game_end.setLeftCardCount(GRR._left_card_count);
			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					pnc.addItem(GRR._player_niao_cards[i][j]);
				}
				game_end.addPlayerNiaoCards(pnc);
				game_end.addHuResult(GRR._hu_result[i]);

				Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
					hc.addItem(GRR._chi_hu_card[i][j]);
				}

				game_end.addHuCardData(GRR._chi_hu_card[i][0]);
				game_end.addHuCardArray(hc);

				if (GRR._chi_hu_rights[i].bao_ting_card != 0) {
					game_end.addBaoTingCards(GRR._chi_hu_rights[i].bao_ting_card + GameConstants.CARD_ESPECIAL_TYPE_BAO_TING);// 报听的牌
				} else {
					game_end.addBaoTingCards(0);
				}
			}

			long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

			this.set_result_describe();

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {
					if (_logic.is_magic_card(GRR._cards_data[i][j])) {
						cs.addItem(GRR._cards_data[i][j] + GameConstants.CARD_ESPECIAL_TYPE_HUN);
					} else {
						cs.addItem(GRR._cards_data[i][j]);
					}
				}
				game_end.addCardsData(cs);

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

				GRR._chi_hu_rights[i].get_right_data(rv);
				game_end.addChiHuRight(rv[0]);

				GRR._start_hu_right[i].get_right_data(rv);
				game_end.addStartHuRight(rv[0]);

				game_end.addProvidePlayer(GRR._provider[i]);
				game_end.addGameScore(GRR._game_score[i]);
				game_end.addGangScore(lGangScore[i]);
				//game_end.addStartHuScore(GRR._start_hu_score[i]);
				game_end.addStartHuScore(GRR._score_of_gangpao[i]);
				game_end.addResultDes(GRR._result_des[i]);

				game_end.addWinOrder(GRR._win_order[i]);

				Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
					lfs.addItem(GRR._lost_fan_shu[i][j]);
				}

				game_end.addLostFanShu(lfs);
			}
		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round) {
				end = true;
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(this.process_player_result(reason));
			} else {
				// 确定下局庄家
				// 以后谁胡牌，下局谁做庄。
				// 流局,庄家下一个
				// 通炮,放炮的玩家
			}
		} else if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM) {
			end = true;
			real_reason = GameConstants.Game_End_DRAW;
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(this.process_player_result(reason));
		}
		game_end.setEndType(real_reason);

		roomResponse.setGameEnd(game_end);

		this.send_response_to_room(roomResponse);

		record_game_round(game_end);

		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT || reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
				Player player = this.get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");
			}
		}

		if (end) {
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
		}

		if (!is_sys()) {
			GRR = null;
		}

		return true;
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weave_items, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {
		if ((!has_rule(GameConstants.GAME_RULE_DIAN_PAO_HU_SMX) && (card_type == GameConstants.HU_CARD_TYPE_PAOHU)))
			return GameConstants.WIK_NULL;

		if (cur_card == 0)
			return GameConstants.WIK_NULL;

		int[] tmp_cards_index = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			tmp_cards_index[i] = cards_index[i];
		}

		if (cur_card != 0)
			tmp_cards_index[_logic.switch_to_card_index(cur_card)]++;

		int chiHuKind = GameConstants.WIK_NULL;

		int magic_count = _logic.magic_count(tmp_cards_index);
		magic_count %= 2;

		if (magic_count == 0 && this.has_rule(GameConstants.GAME_RULE_HAVE_QI_DUI_SMX)) {
			int qi_xiao_dui = _logic.is_qi_xiao_dui_henan(tmp_cards_index, weave_items, weave_count);
			if(magic_count != 0 && card_type == GameConstants.HU_CARD_TYPE_PAOHU){
				qi_xiao_dui = GameConstants.WIK_NULL;
			}
			if (qi_xiao_dui != GameConstants.WIK_NULL) {
				chiHuKind = GameConstants.WIK_CHI_HU;
				/*if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
					chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
				} else {
					chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
				}*/
				chiHuRight.opr_or(GameConstants.CHR_HENAN_QI_XIAO_DUI);
			}
		}

		if (chiHuRight.is_empty() == false) {
			if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
				chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
			} else if (card_type == GameConstants.HU_CARD_TYPE_PAOHU) {
				chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
			} else if (card_type == GameConstants.HU_CARD_TYPE_QIANGGANG) {
				chiHuRight.opr_or(GameConstants.CHR_HENAN_QIANG_GANG_HU);
			} else if (card_type == GameConstants.HU_CARD_TYPE_TIAN_HU) {
				chiHuRight.opr_or(GameConstants.CHR_HENAN_TIAN_HU);
			} else if (card_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
				chiHuRight.opr_or(GameConstants.CHR_HENAN_GANG_KAI);
			}
			return chiHuKind;
		}

		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_card_count = _logic.get_magic_card_count();

		if (magic_card_count > 2) { // 一般只有两种癞子牌存在
			magic_card_count = 2;
		}

		for (int i = 0; i < magic_card_count; i++) {
			magic_cards_index[i] = _logic.get_magic_card_index(i);
		}

		boolean can_win = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card), magic_cards_index,
				magic_card_count);

		if (!can_win) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		chiHuKind = GameConstants.WIK_CHI_HU;
		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else if (card_type == GameConstants.HU_CARD_TYPE_PAOHU) {
			chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
		} else if (card_type == GameConstants.HU_CARD_TYPE_QIANGGANG) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_QIANG_GANG_HU);
		} else if (card_type == GameConstants.HU_CARD_TYPE_TIAN_HU) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_TIAN_HU);
		} else if (card_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_GANG_KAI);
		}

		return chiHuKind;
	}

	@Override
	protected void set_result_describe() {
		int lGangScore[] = new int[GameConstants.GAME_PLAYER];
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {

			for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
				for (int k = 0; k < GameConstants.GAME_PLAYER; k++) {
					lGangScore[k] += GRR._gang_score[i].scores[j][k];
				}
			}
		}

		int l;
		long type = 0;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			String des = "";

			if (has_rule(GameConstants.GAME_RULE_BANKER_JIA_DI_SMX) && i == GRR._banker_player) {
				des += " 庄家加底";
			}

			l = GRR._chi_hu_rights[i].type_count;
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					if (type == GameConstants.CHR_ZI_MO) {
						des += " 自摸";
					}

					if (type == GameConstants.CHR_SHU_FAN) {
						des += " 接炮";
					}
					if (type == GameConstants.CHR_HENAN_QIANG_GANG_HU) {
						des += " 抢杠胡";
					}
					if (type == GameConstants.CHR_HENAN_TIAN_HU) {
						des += " 天胡";
					}
					if (type == GameConstants.CHR_HENAN_QI_XIAO_DUI || type == GameConstants.CHR_HENAN_HH_QI_XIAO_DUI) {
						if (has_rule(GameConstants.GAME_RULE_DOUBLE_QI_DUI_SMX)) {
							des += " 七小对加倍";
						} else {
							des += " 七小对";
						}
					}
					if (type == GameConstants.CHR_HENAN_GANG_KAI) {
							des += " 杠上花";
					}
				} else {
					if (type == GameConstants.CHR_FANG_PAO) {
						des += " 放炮";
					}
				}
			}
			int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0;
			if (GRR != null) {
				for (int p = 0; p < GameConstants.GAME_PLAYER; p++) {
					for (int w = 0; w < GRR._weave_count[p]; w++) {
						if (GRR._weave_items[p][w].weave_kind != GameConstants.WIK_GANG) {
							continue;
						}
						if (p == i) {
							if (GRR._weave_items[p][w].type == GameConstants.GANG_TYPE_AN_GANG) {
								an_gang++;
							} 
							else if(GRR._weave_items[p][w].type == GameConstants.GANG_TYPE_ADD_GANG){
									ming_gang++;
							}
							else if(GRR._weave_items[p][w].type == GameConstants.GANG_TYPE_JIE_GANG){
								jie_gang++;
							}
						}else{
							if (GRR._weave_items[p][w].provide_player == i && GRR._weave_items[p][w].type == GameConstants.GANG_TYPE_JIE_GANG) {
								fang_gang++;
							}
						}
					}
				}
			}
			
			if(_player_result.biaoyan[i] != 0){
				des += " 表演" + _player_result.biaoyan[i];
			}

			if (an_gang > 0) {
				des += " 暗杠X" + an_gang;
			}
			if (ming_gang > 0) {
				des += " 明杠X" + ming_gang;
			}
			if (fang_gang > 0) {
				des += " 放杠X" + fang_gang;
			}
			if (jie_gang > 0) {
				des += " 接杠X" + jie_gang;
			}
			GRR._result_des[i] = des;
		}
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		return false;
	}

	public int get_henan_ting_card(int[] cards, int[] cards_index, WeaveItem[] weave_items, int weave_count) {
		PerformanceTimer timer = new PerformanceTimer();

		int count = 0;

		int[] tmp_cards_index = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++)
			tmp_cards_index[i] = cards_index[i];

		int card_type_count = GameConstants.MAX_ZI;
		if (has_rule(GameConstants.GAME_RULE_DAI_FENG_SMX))
			card_type_count = GameConstants.MAX_ZI_FENG;

		ChiHuRight chiHuRight = new ChiHuRight();
		int tmp_card = 0;
		for (int i = 0; i < card_type_count; i++) {
			tmp_card = _logic.switch_to_card_data(i);
			chiHuRight.set_empty();

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(tmp_cards_index, weave_items, weave_count, tmp_card, chiHuRight,
					GameConstants.HU_CARD_TYPE_ZIMO, i)) {
				cards[count] = tmp_card;

				if (_logic.is_magic_index(i)) {
					if (chiHuRight.opr_and(GameConstants.CHR_HENAN_QI_XIAO_DUI).is_empty()
							|| chiHuRight.opr_and(GameConstants.CHR_HENAN_HH_QI_XIAO_DUI).is_empty()) {
						cards[count] += GameConstants.CARD_ESPECIAL_TYPE_HUN;
					}
				}

				count++;
			}
		}

		if (count >= card_type_count) {
			count = 1;
			cards[0] = -1;
		}

		if (timer.get() > 50) {
			logger.warn("cost time too long " + Arrays.toString(cards_index) + ", cost time = " + timer.duration());
		}

		return count;
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
	}
	
	public void process_gang_score(int _seat_index){
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if(i == _seat_index) continue;
			for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
				for (int k = 0; k < getTablePlayerNumber(); k++) {
					GRR._gang_score[i].scores[j][k] = 0;
				}
			}
		}
		if(this.has_rule(GameConstants.GAME_RULE_DAI_GANG_PAO_SMX)){
			for(int i = 0;i < getTablePlayerNumber();i++){
				GRR._score_of_gangpao[i] = 0;
				if(i != _seat_index) continue;
				for(int j = 0;j < GRR._gang_score[i].gang_count;j++){
					for (int k = 0; k < getTablePlayerNumber(); k++) {
						if(GRR._gang_score[i].scores[j][k] != 0){
							int gangpaoscore = _player_result.pao[i] + _player_result.pao[k];
							GRR._score_of_gangpao[k] -= gangpaoscore;
							GRR._score_of_gangpao[i] += gangpaoscore;
						}
					}
				}
			}
		}
	}

	public void process_chi_hu_player_score_henan(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int wFanShu = _logic.get_chi_hu_action_rank_henan(chr);

		countCardType(chr, seat_index);

		/*if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				GRR._lost_fan_shu[i][seat_index] = wFanShu;
			}
		} else {
			GRR._lost_fan_shu[provide_index][seat_index] = wFanShu;
		}*/

		float lChiHuScore = wFanShu;

		boolean zhuang_hu = (GRR._banker_player == seat_index ? true : false);
		boolean zhuang_fang_hu = (GRR._banker_player == provide_index ? true : false);
		boolean jia_di = has_rule(GameConstants.GAME_RULE_BANKER_JIA_DI_SMX);

		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}

				float s = lChiHuScore;

				if (jia_di) {
					if (zhuang_hu) {
						s += 1;
					} else if (GRR._banker_player == i) {
						s += 1;
					}
				}

				s += (_player_result.pao[i] + _player_result.pao[seat_index]);

				if(_player_result.biaoyan[seat_index] != 0){
					double a = Math.pow(2,_player_result.biaoyan[seat_index]);
					s = s*(float)a;
				}
				else if (!(chr.opr_and(GameConstants.CHR_HENAN_QI_XIAO_DUI)).is_empty() && has_rule(GameConstants.GAME_RULE_DOUBLE_QI_DUI_SMX)) {
					s *= 2;
				}

				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;
				GRR._lost_fan_shu[i][seat_index] = (int)s;

			}
		} else {
			float s = lChiHuScore;

			if (jia_di) {
				if (zhuang_hu) {
					s += 1;
				} else if (zhuang_fang_hu) {
					s += 1;
				}
			}

			s += (_player_result.pao[provide_index] + _player_result.pao[seat_index]);

			if (!(chr.opr_and(GameConstants.CHR_HENAN_QI_XIAO_DUI)).is_empty() && has_rule(GameConstants.GAME_RULE_DOUBLE_QI_DUI_SMX)) {
				s *= 2;
			}

			GRR._game_score[provide_index] -= s;
			GRR._game_score[seat_index] += s;
			GRR._lost_fan_shu[provide_index][seat_index] = (int)s;

			GRR._chi_hu_rights[provide_index].opr_or(GameConstants.CHR_FANG_PAO);
		}

		GRR._provider[seat_index] = provide_index;

		_status_gang = false;
		_status_gang_hou_pao = false;

		if(this.has_rule(GameConstants.GAME_RULE_GANG_SUI_HU_SMX)){
			process_hu_player_gang_score(seat_index);
		}
		
		change_player_status(seat_index, GameConstants.INVALID_VALUE);
	}
	
	//去掉非胡牌玩家的杠分
	public void process_hu_player_gang_score(int seat_index){
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			GRR._score_of_gangpao[i] = 0;
			if(i == seat_index)
				continue;
			for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
				for (int k = 0; k < getTablePlayerNumber(); k++) {
					GRR._gang_score[i].scores[j][k] = 0;
				}
			}
		}
		
		for (int j = 0; j < GRR._gang_score[seat_index].gang_count; j++) {
			for (int k = 0; k < getTablePlayerNumber(); k++) {
				if(k == seat_index)
					continue;
				if(GRR._gang_score[seat_index].scores[j][k] < 0){
					int gangpaoscore = _player_result.pao[seat_index] + _player_result.pao[k];
					GRR._score_of_gangpao[k] -= gangpaoscore;
					GRR._score_of_gangpao[seat_index] += gangpaoscore;
				}
			}
			
		}
		
	}

	public boolean estimate_gang_respond_henan(int seat_index, int card) {
		boolean bAroseAction = false;

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];
			if (playerStatus.is_chi_hu_round()) {
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();

				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr, GameConstants.HU_CARD_TYPE_QIANGGANG,
						i);

				if (action != 0) {
					chr.opr_or(GameConstants.CHR_HUNAN_QIANG_GANG_HU);
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);
					bAroseAction = true;
				}
			}
		}

		if (bAroseAction == true) {
			_provide_player = seat_index;
			_provide_card = card;
			_resume_player = _current_player;
			_current_player = GameConstants.INVALID_SEAT;
		}

		return bAroseAction;
	}

	public boolean estimate_player_out_card_respond_henan(int seat_index, int card) {
		boolean bAroseAction = false;

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		int llcard = LIUPAI;
		
		
		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;
		
		if(this._logic.is_magic_card(card)){
			return false;
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			action = _logic.check_peng(GRR._cards_index[i], card);
			if (action != 0 && _logic.is_magic_card(card)) {
				action = 0;
			}
			if (action != 0) {
				playerStatus.add_action(action);
				playerStatus.add_peng(card, seat_index);
				bAroseAction = true;
			}

			if (GRR._left_card_count > llcard) {
				action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
				if (action != 0  && _logic.is_magic_card(card)) {// 鬼牌不能杠
					action = 0;
				}
				if (action != 0) {
					playerStatus.add_action(GameConstants.WIK_GANG);
					playerStatus.add_gang(card, seat_index, 1);// 加上杠
					bAroseAction = true;
				}
			}

			if (_playerStatus[i].is_chi_hu_round() && _player_result.biaoyan[i] == 0){
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];

				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr, GameConstants.HU_CARD_TYPE_PAOHU, i);
				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
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

	public boolean exe_hun(int seat_index) {
		set_handler(_handler_hun);
		_handler_hun.reset_status(seat_index);
		_handler_hun.exe(this);

		return true;
	}

	public boolean exe_qishou_hun(int seat_index) {
		set_handler(_handler_qishou_hun);
		_handler_qishou_hun.reset_status(seat_index);
		_handler_qishou_hun.exe(this);
		return true;
	}
	
	public boolean exe_gang_selectcard(int seat_index) {
		set_handler(_handler_select_card);
		_handler_select_card.reset_status(seat_index);
		_handler_select_card.exe(this);
		return true;
	}


	@Override
	public boolean operate_player_cards_with_ting(int seat_index, int card_count, int cards[], int weave_count, WeaveItem weaveitems[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS);
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(1);

		load_common_status(roomResponse);

		roomResponse.setCardCount(card_count);
		roomResponse.setWeaveCount(weave_count);

		if (weave_count > 0) {
			for (int j = 0; j < weave_count; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(weaveitems[j].provide_player);
				weaveItem_item.setPublicCard(weaveitems[j].public_card);
				weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
				weaveItem_item.setCenterCard(weaveitems[j].center_card);
				roomResponse.addWeaveItems(weaveItem_item);
			}
		}

		send_response_to_other(seat_index, roomResponse);

		for (int j = 0; j < card_count; j++) {
			roomResponse.addCardData(cards[j]);
		}

		int ting_count = _playerStatus[seat_index]._hu_out_card_count;

		roomResponse.setOutCardCount(ting_count);

		for (int i = 0; i < ting_count; i++) {
			int ting_card_cout = _playerStatus[seat_index]._hu_out_card_ting_count[i];
			roomResponse.addOutCardTingCount(ting_card_cout);

			roomResponse.addOutCardTing(_playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants.CARD_ESPECIAL_TYPE_TING);

			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < ting_card_cout; j++) {
				int tmp_card = _playerStatus[seat_index]._hu_out_cards[i][j];
				
				if (_logic.is_magic_card(tmp_card)) {
					tmp_card += GameConstants.CARD_ESPECIAL_TYPE_HUN;
				}
				
				int_array.addItem(tmp_card);
			}
			roomResponse.addOutCardTingCards(int_array);
		}

		send_response_to_player(seat_index, roomResponse);

		GRR.add_room_response(roomResponse);

		return true;
	}
	
	public void operate_player_gang_get_card(int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_NAO_ACTION);
		load_room_info_data(roomResponse);
		//选择杠的牌
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_result.nao[i]=0;
		}
		operate_player_data();
		
		roomResponse.setTarget(0);
		roomResponse.setNao(GRR._especial_show_cards[1]);
		roomResponse.setTarget(1);
		roomResponse.setNao(GRR._especial_show_cards[2]);
		roomResponse.setNaodes("当前可以选择的牌");
		send_response_to_player(seat_index, roomResponse);
	}
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * 表演的玩家
	 * 
	 * @param seat_index
	 * @param operate_card
	 * @param rm
	 */
	public void process_player_biaoyan(int seat_index, int effect_type,int to_player) {

		
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int effect_count = chr.type_count;
		long effect_indexs[] = new long[effect_count];
		for (int i = 0; i < effect_count; i++) {
			if (chr.type_list[i] == GameConstants.CHR_SHU_FAN) {
				effect_indexs[i] = GameConstants.CHR_HU;
			} else {
				effect_indexs[i] = chr.type_list[i];
			}

		}
		// 效果
		//this.operate_effect_action(seat_index, effect_type, effect_count, effect_indexs, 1, GameConstants.INVALID_SEAT);
		
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_EFFECT_ACTION);
		roomResponse.setEffectType(effect_type);
		roomResponse.setTarget(seat_index);
		roomResponse.setEffectCount(effect_count);
		for (int i = 0; i < effect_count; i++) {
			roomResponse.addEffectsIndex(effect_indexs[i]);
		}
		roomResponse.setEffectTime(1);
		load_room_info_data(roomResponse);
		operate_player_data();
		if (to_player == GameConstants.INVALID_SEAT) {
			this.send_response_to_room(roomResponse);
		} else {
			this.send_response_to_player(to_player, roomResponse);
		}
		return;
	}
	
	protected void test_cards() {
		int cards[] = new int[] { 0x02, 0x02, 0x02, 0x03, 0x03, 0x11, 0x11, 0x11, 0x15, 0x15, 0x21, 0x21, 0x21,
								  0x11, 0x11, 0x11, 0x11, 0x13, 0x13, 0x13, 0x13, 0x16, 0x19, 0x21, 0x33, 0x33,
								  0x21, 0x21, 0x21, 0x21, 0x23, 0x23, 0x15, 0x15, 0x31, 0x32, 0x33, 0x33, 0x34,
								  0x01, 0x01, 0x01, 0x02, 0x03, 0x13, 0x21, 0x22, 0x23, 0x32, 0x33, 0x33, 0x34};

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		} 
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < 13; j++) {
				GRR._cards_index[i][_logic.switch_to_card_index(cards[13*i+j])] += 1;
			}
		}

		/*************** 如果要测试线上实际牌 把下面代码取消注释 放入实际牌型即可 ***********************/

		/********
		 * 下面这个给测试用的 如果开发用 把上面的注释去掉 realyCards
		 **************************/

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

	@Override
	public boolean handler_requst_pao_qiang(Player player, int pao, int qiang) {
		return _handler_pao_henna.handler_pao_qiang(this, player.get_seat_index(), pao, qiang);
	}
	
	@Override
	public boolean handler_requst_nao_zhuang(Player player, int card) {
		return _handler_select_card.handler_selectcard(this, player.get_seat_index(), card);
	}
	
	@Override
	public int get_real_card(int card) {
		// 错误断言
		if (card > GameConstants.CARD_ESPECIAL_TYPE_GUI && card < GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_GUI;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI && card < GameConstants.CARD_ESPECIAL_TYPE_DING_GUI) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;

		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_DING_GUI && card < GameConstants.CARD_ESPECIAL_TYPE_LAI_GEN) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_DING_GUI;

		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_LAI_GEN && card < GameConstants.CARD_ESPECIAL_TYPE_TING) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_LAI_GEN;

		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_TING && card < GameConstants.CARD_ESPECIAL_TYPE_BAO_TING) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_TING;

		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_BAO_TING && card < GameConstants.CARD_ESPECIAL_TYPE_HUN) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_BAO_TING;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_HUN && card < GameConstants.CARD_ESPECIAL_TYPE_CI) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_HUN;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_CI && card < GameConstants.CARD_ESPECIAL_TYPE_TOU_DA) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_CI;
		}else if(card > GameConstants.CARD_ESPECIAL_TYPE_TOU_DA && card < GameConstants.CARD_ESPECIAL_TYPE_WANG_BA){
			card -= GameConstants.CARD_ESPECIAL_TYPE_TOU_DA;
		}else if (card > GameConstants.CARD_ESPECIAL_TYPE_WANG_BA && card < GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_OUT) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
		}else if(card > GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_OUT && card < GameConstants.CARD_ESPECIAL_TYPE_LIANG_ZHANG){
			card -= GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_OUT;
		}else if (card > GameConstants.CARD_ESPECIAL_TYPE_LIANG_ZHANG && card < GameConstants.CARD_ESPECIAL_TYPE_LIANG_ZHANG_BIAO_TING) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_LIANG_ZHANG;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_LIANG_ZHANG_BIAO_TING && card < GameConstants.CARD_ESPECIAL_TYPE_YAOJIU) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_LIANG_ZHANG_BIAO_TING;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_YAOJIU && card < GameConstants.CARD_ESPECIAL_TYPE_SHENGPAI) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_YAOJIU;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_SHENGPAI && card < GameConstants.CARD_ESPECIAL_TYPE_SHENGPAI_TING) { // 窟窿带神神牌
			card -= GameConstants.CARD_ESPECIAL_TYPE_SHENGPAI;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_SHENGPAI_TING && card < GameConstants.CARD_ESPECIAL_TYPE_BAO) { // 窟窿带神神牌
			card -= GameConstants.CARD_ESPECIAL_TYPE_SHENGPAI_TING;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_BAO) { // 瑞金麻将宝牌
			card -= GameConstants.CARD_ESPECIAL_TYPE_BAO;
		}
		return card;
	}
}
