package com.cai.game.phz.handler.yjghz;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.GameConstants_YJGHZ;
import com.cai.common.constant.game.mj.GameConstants_XiangXiang;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.Player;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.RandomUtil;
import com.cai.dictionary.SysParamDict;
import com.cai.game.RoomUtil;
import com.cai.game.phz.PHZTable;
import com.cai.game.phz.data.AnalyseItem;
import com.cai.service.PlayerServiceImpl;
import com.google.common.collect.Lists;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.PlayerResultResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class YuanJiangGHZTable extends PHZTable {

	private static final long serialVersionUID = -8297543110475427339L;

	public int[] player_yuan;

	public boolean banker_qi_shou_qing;

	public List<Integer>[] qi_shou_four;

	private int nei_yuan_count;
	private int wai_yuan_count;

	public YuanJiangGHZTable() {
		super();
	}

	@Override
	public void on_init_table(int game_type_index, int game_rule_index, int game_round) {
		onInitTable();
		super.on_init_table(game_type_index, game_rule_index, game_round);
	}

	private void onInitTable() {
		_handler_dispath_firstcards = new WHZHandlerDispatchFirstCard_YuanJiang();
		_handler_chuli_firstcards = new WHZHandlerChuLiFirstCard_YuanJiang();
		_handler_dispath_card = new WHZHandlerDispatchCard_YuanJiang();
		_handler_out_card_operate = new WHZHandlerOutCardOperate_YuanJiang();
		_handler_gang = new WHZHandlerGang_YuanJiang();
		_handler_chi_peng = new WHZHandlerChiPeng_YuanJiang();
		_handler_wai = new WHZHandlerWai_YuanJiang();
		_handler_liu = new WHZHandlerQing_YuanJiang();
		_handler_piao = new WHZHandlerPiao_YuanJiang();
	}

	@Override
	public int getTablePlayerNumber() {
		return 3;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean reset_init_data() {
		super.reset_init_data();
		player_yuan = new int[getTablePlayerNumber()];
		banker_qi_shou_qing = false;
		qi_shou_four = new List[getTablePlayerNumber()];
		for (int p = 0; p < getTablePlayerNumber(); p++) {
			qi_shou_four[p] = new ArrayList<Integer>();
		}
		wai_yuan_count = 0;
		nei_yuan_count = 0;
		return true;
	}

	private void progress_banker_select() {
		if (_cur_banker == GameConstants.INVALID_SEAT) {
			Random random = new Random();//
			int rand = random.nextInt(6) + 1 + random.nextInt(6) + 1;
			_cur_banker = rand % getTablePlayerNumber();

			_shang_zhuang_player = GameConstants.INVALID_SEAT;
			_lian_zhuang_player = GameConstants.INVALID_SEAT;
		}
	}

	// 游戏结束
	@Override
	public boolean on_room_game_finish(int seat_index, int reason) {

		boolean ret = true;

		if (_cur_round == 1 && (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW)) {
			RoomUtil.realkou_dou(this);
		}
		ret = on_game_finish(seat_index, reason);

		if (is_sys()) {
			clear_score_in_gold_room();
		}
		return ret;
	}

	public boolean on_game_finish(int seat_index, int reason) {
		_game_status = GameConstants.GS_MJ_WAIT;
		int real_reason = reason;

		int count = getTablePlayerNumber();
		if (count == 0) {
			count = this.getTablePlayerNumber();
		}
		for (int i = 0; i < count; i++) {
			_player_ready[i] = 0;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();

		roomResponse.setLeftCardCount(0);

		this.load_common_status(roomResponse);
		this.load_room_info_data(roomResponse);

		// 这里记录了两次，先这样
		RoomInfo.Builder room_info = getRoomInfo();
		game_end.setRoomInfo(room_info);
		game_end.setGamePlayerNumber(count);
		game_end.setRoundOverType(0);
		game_end.setRoomOverType(0);
		game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间

		if (GRR != null) {// reason == MJGameConstants.Game_End_NORMAL ||
							// reason
							// == MJGameConstants.Game_End_DRAW ||
			// (reason ==MJGameConstants.Game_End_RELEASE_PLAY && GRR!=null)
			game_end.setRoundOverType(1);
			game_end.setStartTime(GRR._start_time);

			game_end.setGameTypeIndex(GRR._game_type_index);
			roomResponse.setLeftCardCount(GRR._left_card_count);

			// 特别显示的牌
			for (int i = 0; i < GRR._especial_card_count; i++) {
				game_end.addEspecialShowCards(GRR._especial_show_cards[i]);

			}

			GRR._end_type = reason;

			this.load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);
			game_end.setGamePlayerNumber(getTablePlayerNumber());
			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);// 专家
			game_end.setLeftCardCount(GRR._left_card_count);// 剩余牌
			int left_card_count = GRR._left_card_count;
			int cards[] = new int[22];
			int k = 0;
			for (int i = _all_card_len - GRR._left_card_count; i < _all_card_len; i++) {
				cards[k] = _repertory_card[_all_card_len - left_card_count];
				game_end.addCardsList(cards[k]);

				k++;
				left_card_count--;
			}

			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				game_end.addHuResult(GRR._hu_result[i]);

				// 胡的牌
				Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < this.get_hand_card_count_max(); j++) {
					hc.addItem(GRR._chi_hu_card[i][j]);
				}

				game_end.addHuCardData(GRR._chi_hu_card[i][0]);
				game_end.addHuCardArray(hc);
			}

			// 现在权值只有一位
			long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

			// 设置胡牌描述
			if (reason == GameConstants.Game_End_NORMAL) {
				this.set_result_describe();
				game_end.setFanShu(_fan_shu[seat_index]);
				game_end.setCountPickNiao(GRR._count_pick_niao);// 中鸟个数
			}

			for (int i = 0; i < count; i++) {
				GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {

					cs.addItem(GRR._cards_data[i][j]);
				}
				game_end.addCardsData(cs);// 牌
				int all_hu_xi = 0;
				// 组合
				WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();

				if (GRR._win_order[i] > 0) {
					for (int j = 0; j < _hu_weave_count[i]; j++) {
						WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
						weaveItem_item.setProvidePlayer(_hu_weave_items[i][j].provide_player);
						weaveItem_item.setPublicCard(_hu_weave_items[i][j].public_card);
						weaveItem_item.setWeaveKind(_hu_weave_items[i][j].weave_kind);
						weaveItem_item.setHuXi(_hu_weave_items[i][j].hu_xi);
						all_hu_xi += _hu_weave_items[i][j].hu_xi;
						weaveItem_item.setCenterCard(_hu_weave_items[i][j].center_card);
						for (int wd = 0; wd < _hu_weave_items[i][j].weave_card.length; wd++) {
							weaveItem_item.addWeaveCard(_hu_weave_items[i][j].weave_card[wd]);
						}
						weaveItem_array.addWeaveItem(weaveItem_item);
					}
				} else {
					for (int j = 0; j < GRR._weave_count[i]; j++) {
						WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
						weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
						weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
						weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
						weaveItem_item.setHuXi(GRR._weave_items[i][j].hu_xi);
						weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
						weaveItem_array.addWeaveItem(weaveItem_item);
					}
				}
				game_end.addWeaveItemArray(weaveItem_array);

				GRR._chi_hu_rights[i].get_right_data(rv);// 获取权位数值
				game_end.addChiHuRight(rv[0]);

				GRR._start_hu_right[i].get_right_data(rv);// 获取权位数值
				game_end.addStartHuRight(rv[0]);

				game_end.addProvidePlayer(GRR._provider[i]);
				game_end.addGameScore(GRR._game_score[i]);// 放炮的人？
				// game_end.addGangScore(lGangScore[i]);// 杠牌得分
				game_end.addStartHuScore(this._hu_xi[i]);
				game_end.addResultDes(GRR._result_des[i]);
				game_end.addGangCount(player_yuan[i]); // 内元外元
				// 胡牌
				game_end.addWinOrder(GRR._win_order[i]);

				Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < count; j++) {
					lfs.addItem(GRR._lost_fan_shu[i][j]);
				}
				game_end.addLostFanShu(lfs);
			}
			if (reason == GameConstants.Game_End_NORMAL) {
				for (int p = 0; p < getTablePlayerNumber(); p++) {
					Int32ArrayResponse.Builder ming_tang = Int32ArrayResponse.newBuilder();
					// 名堂展示
					int chrTypes = GRR._chi_hu_rights[seat_index].type_count;
					for (int typeIndex = 0; typeIndex < chrTypes && p == seat_index; typeIndex++) {
						ming_tang.addItem((int) GRR._chi_hu_rights[seat_index].type_list[typeIndex]);
					}
					game_end.addPlayerNiaoCards(ming_tang);
				}
			}
		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round) {// 局数到了
				end = true;
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(this.process_player_result(reason));

				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					game_end.addGameScore(this._player_result.game_score[i]);
				}

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
			real_reason = GameConstants.Game_End_DRAW;// 刘局
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(this.process_player_result(reason));

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				game_end.addGameScore(this._player_result.game_score[i]);
			}
		}

		game_end.setEndType(real_reason);

		////////////////////////////////////////////////////////////////////// 得分总的
		roomResponse.setGameEnd(game_end);

		this.send_response_to_room(roomResponse);

		record_game_round(game_end, real_reason);

		// 超时解散
		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT || reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < this.getTablePlayerNumber(); j++) {
				Player player = this.get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");

			}
		}

		if (end)// 删除
		{
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
		}
		if (!is_sys()) {
			GRR = null;
		}

		// 错误断言
		return false;
	}

	private PlayerResultResponse.Builder process_player_result(int reason) {
		this.huan_dou(reason);
		// 大赢家
		int count = getTablePlayerNumber();
		if (count == 0) {
			count = this.getTablePlayerNumber();
		}
		for (int i = 0; i < count; i++) {
			_player_result.win_order[i] = -1;
		}
		int win_idx = 0;
		float max_score = 0;
		for (int i = 0; i < count; i++) {
			int winner = -1;
			float s = -999999;
			for (int j = 0; j < count; j++) {
				if (_player_result.win_order[j] != -1) {
					continue;
				}
				if (_player_result.game_score[j] > s) {
					s = _player_result.game_score[j];
					winner = j;
				}
			}
			if (s >= max_score) {
				max_score = s;
			} else {
				win_idx++;
			}
			if (winner != -1) {
				_player_result.win_order[winner] = win_idx;
				// win_idx++;
			}
		}

		PlayerResultResponse.Builder player_result = PlayerResultResponse.newBuilder();

		for (int i = 0; i < count; i++) {
			player_result.addGameScore(_player_result.game_score[i]);
			player_result.addWinOrder(_player_result.win_order[i]);

			Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < count; j++) {
				lfs.addItem(_player_result.lost_fan_shu[i][j]);
			}

			player_result.addLostFanShu(lfs);

			player_result.addHuPaiCount(_player_result.hu_pai_count[i]);
			player_result.addMingTangCount(_player_result.ming_tang_count[i]);
			player_result.addYingXiCount(_player_result.ying_xi_count[i]);
			player_result.addLiuZiFen(_player_result.liu_zi_fen[i]);

			player_result.addPlayersId(i);
			// }
		}

		player_result.setRoomId(this.getRoom_id());
		player_result.setRoomOwnerAccountId(this.getRoom_owner_account_id());
		player_result.setRoomOwnerName(this.getRoom_owner_name());
		player_result.setCreateTime(this.getCreate_time());
		player_result.setRecordId(this.get_record_id());
		player_result.setGameRound(_game_round);
		player_result.setGameRuleDes(get_game_des());
		player_result.setGameRuleIndex(_game_rule_index);
		player_result.setGameTypeIndex(_game_type_index);
		return player_result;
	}

	protected void set_result_describe() {
		int chrTypes;
		long type = 0;
		for (int player = 0; player < this.getTablePlayerNumber(); player++) {
			StringBuilder result = new StringBuilder("");

			chrTypes = GRR._chi_hu_rights[player].type_count;

			for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
				type = GRR._chi_hu_rights[player].type_list[typeIndex];

				if (GRR._chi_hu_rights[player].is_valid()) {
					// 一点朱
					if (type == GameConstants_YJGHZ.CHR_YI_DIAN_ZHU) {
						result.append(" 一点朱");
					}
					// 十三红
					if (type == GameConstants_YJGHZ.CHR_SHI_SAN_HONG) {
						result.append(" 十三红");
					}
					// 十四红
					if (type == GameConstants_YJGHZ.CHR_SHI_SI_HONG) {
						result.append(" 十四红");
					}
					// 十五红
					if (type == GameConstants_YJGHZ.CHR_SHI_WU_HONG) {
						result.append(" 十五红");
					}
					// 对子胡
					if (type == GameConstants_YJGHZ.CHR_DUI_ZI) {
						result.append(" 对子胡");
					} else // 项
					if (type == GameConstants_YJGHZ.CHR_XIANG_XIANG_XI) {
						result.append(" 项项息");
					}
					// 黑子胡
					if (type == GameConstants_YJGHZ.CHR_HEI_ZI) {
						result.append(" 黑子胡");
					}
					// 黑对子胡
					if (type == GameConstants_YJGHZ.CHR_HEI_DUI_ZI) {
						result.append(" 黑对子胡");
					}
					// 大字牌
					if (type == GameConstants_YJGHZ.CHR_DA_ZI_PAI) {
						result.append(" 大字胡");
					}
					// 小字牌
					if (type == GameConstants_YJGHZ.CHR_XIAO_ZI_PAI) {
						result.append(" 小字胡");
					}
					// 无息平
					if (type == GameConstants_YJGHZ.CHR_WU_XI) {
						result.append(" 无息平");
					}
					// 吊吊手
					if (type == GameConstants_YJGHZ.CHR_DIAO_DIAO) {
						result.append(" 吊吊手");
					}
					// 海底胡
					if (type == GameConstants_YJGHZ.CHR_HAI_DI) {
						result.append(" 海底胡");
					}
					// 九对半
					if (type == GameConstants_YJGHZ.CHR_JIU_DUI_BAN) {
						result.append(" 九对半 ");
					}
				} else if (type == GameConstants_YJGHZ.CHR_FANG_PAO) {
				}
			}

			GRR._result_des[player] = result.toString();
		}
	}

	/// 洗牌
	private void shuffle(int repertory_card[], int mj_cards[]) {
		_all_card_len = repertory_card.length;
		GRR._left_card_count = _all_card_len;

		int xi_pai_count = 0;
		int rand = RandomUtil.generateRandomNumber(3, 6);

		while (xi_pai_count < 6 && xi_pai_count < rand) {
			if (xi_pai_count == 0)
				_logic.random_card_data(repertory_card, mj_cards);
			else
				_logic.random_card_data(repertory_card, repertory_card);

			xi_pai_count++;
		}

		int send_count;
		int have_send_count = 0;

		int count = getTablePlayerNumber();
		for (int i = 0; i < count; i++) {
			send_count = (GameConstants_YJGHZ.MAX_HAND_CARD_MAX_COUNT - 1);
			GRR._left_card_count -= send_count;
			// 一人20张牌,庄家多一张
			_logic.switch_to_cards_index(repertory_card, have_send_count, send_count, GRR._cards_index[i]);
			have_send_count += send_count;
		}
		_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));

	}

	@Override
	protected boolean on_handler_game_start() {
		_game_status = GameConstants.GS_MJ_PLAY;
		this.log_error("gme_status:" + this._game_status);

		reset_init_data();

		// 庄家选择
		this.progress_banker_select();

		GRR._banker_player = _cur_banker;
		_current_player = GRR._banker_player;

		_repertory_card = new int[GameConstants.CARD_COUNT_PHZ_YYWHZ];
		shuffle(_repertory_card, GameConstants.CARD_DATA_PHZ_YYWHZ);

		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();

		return on_game_start();
	}

	private boolean on_game_start() {
		_logic.clean_magic_cards();
		this.GRR._banker_player = this._current_player = this._cur_banker;

		int playerCount = getTablePlayerNumber();
		// 游戏开始
		this._game_status = GameConstants.GS_MJ_PLAY;// 设置状态
		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(this.GRR._banker_player);
		gameStartResponse.setCurrentPlayer(this._current_player);
		gameStartResponse.setLeftCardCount(this.GRR._left_card_count);

		int hand_cards[][] = new int[playerCount][GameConstants.MAX_HH_COUNT];
		// 发送数据
		for (int i = 0; i < playerCount; i++) {
			int hand_card_count = this._logic.switch_to_cards_data(this.GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		int FlashTime = 4000;
		int standTime = 1000;
		// 发送数据
		for (int i = 0; i < playerCount; i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			// 只发自己的牌
			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.MAX_HH_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			// 回放数据
			this.GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.load_room_info_data(roomResponse);
			this.load_common_status(roomResponse);

			if (this._cur_round == 1) {
				// shuffle_players();
				this.load_player_info_data(roomResponse);
			}
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setCurrentPlayer(this._current_player == GameConstants.INVALID_SEAT ? this._resume_player : this._current_player);
			roomResponse.setLeftCardCount(this.GRR._left_card_count);
			roomResponse.setGameStatus(this._game_status);
			roomResponse.setLeftCardCount(this.GRR._left_card_count);
			int gameId = this.getGame_id() == 0 ? 5 : this.getGame_id();
			SysParamModel sysParamModel1104 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId).get(1104);
			if (sysParamModel1104 != null && sysParamModel1104.getVal1() > 0 && sysParamModel1104.getVal1() < 10000) {
				FlashTime = sysParamModel1104.getVal1();
			}
			if (sysParamModel1104 != null && sysParamModel1104.getVal2() > 0 && sysParamModel1104.getVal2() < 10000) {
				standTime = sysParamModel1104.getVal2();
			}
			roomResponse.setFlashTime(FlashTime);
			roomResponse.setStandTime(standTime);
			this.send_response_to_player(i, roomResponse);

		}
		////////////////////////////////////////////////// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		this.load_room_info_data(roomResponse);
		this.load_common_status(roomResponse);
		this.load_player_info_data(roomResponse);
		for (int i = 0; i < playerCount; i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			for (int j = 0; j < GameConstants.MAX_HH_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}

		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(this.GRR._left_card_count);

		// 检测听牌
		for (int i = 0; i < playerCount; i++) {
			this._playerStatus[i]._hu_card_count = get_ting_card(_playerStatus[i]._hu_cards, GRR._cards_index[i], GRR._weave_items[i],
					GRR._weave_count[i], i, i);
			if (this._playerStatus[i]._hu_card_count > 0) {
				this.operate_chi_hu_cards(i, this._playerStatus[i]._hu_card_count, this._playerStatus[i]._hu_cards);
			}
		}

		this.GRR.add_room_response(roomResponse);

		this._handler = this._handler_dispath_firstcards;
		this.exe_dispatch_first_card(_current_player, GameConstants.WIK_NULL, FlashTime + standTime);

		return true;
	}

	public boolean check_jiu_dui_ban(int[] cards_index, int weave_count) {
		if (weave_count != 0) {
			return false;
		}
		int dan = 0;
		for (int i = 0; i < GameConstants_YJGHZ.MAX_HH_INDEX; i++) {
			if (cards_index[i] == 0) {
				continue;
			}
			if ((cards_index[i] % 2) == 1) {
				dan++;
			}
		}

		if (dan == 2 || dan == 0) {
			return true;
		}
		return false;
	}

	/***
	 * 胡牌解析
	 * 
	 * @param cards_index
	 * @param weaveItems
	 * @param weaveCount
	 * @param cur_card
	 * @param chiHuRight
	 * @param card_type
	 * @return
	 */
	public int analyse_chi_hu_card(int cards_index[], WeaveItem weaveItems[], int weaveCount, int seat_index, int provider_index, int cur_card,
			ChiHuRight chiHuRight, int card_type, boolean dispatch) {
		// 构造扑克
		int cbCardIndexTemp[] = new int[GameConstants.MAX_HH_INDEX];
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入扑克
		if (dispatch) {
			if (cur_card != GameConstants.INVALID_VALUE) {
				int index = _logic.switch_to_card_index(cur_card);
				cbCardIndexTemp[index]++;
			}
		}

		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();

		// 分析扑克
		this._hu_xi[seat_index] = 0;
		int hu_xi[] = new int[1];
		hu_xi[0] = 0;
		boolean yws_type = false;
		boolean bValue = _logic.analyse_card_yjghz(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index, cur_card, analyseItemArray,
				false, hu_xi, yws_type);

		if (!bValue) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}
		AnalyseItem analyseItem = new AnalyseItem();
		int temp_hu_xi;
		int temp_total_hu_xi;
		int max_hu_xi = 0;
		int max_hu_index = -1;
		int[] kan_count = new int[analyseItemArray.size()];
		for (int i = 0; i < analyseItemArray.size(); i++) {
			temp_hu_xi = 0;
			temp_total_hu_xi = 0;
			analyseItem = analyseItemArray.get(i);
			for (int j = 0; j < 6; j++) {
				if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL) {
					break;
				}
				if (analyseItem.cbWeaveKind[j] == GameConstants_YJGHZ.WIK_KAN) {
					kan_count[i]++;
				}

				WeaveItem weave_items = new WeaveItem();
				weave_items.center_card = analyseItem.cbCenterCard[j];
				weave_items.weave_kind = analyseItem.cbWeaveKind[j];
				temp_hu_xi += _logic.get_weave_hu_xi_yjghz(weave_items);
				temp_total_hu_xi += _logic.get_weave_hu_xi_yjghz(weave_items);
			}

			if (analyseItem.curCardEye == true) {
				temp_total_hu_xi += 1;
				temp_hu_xi += 1;
			} else if (analyseItem.cbMenEye[0] != 0) {

				// 胡红色门子算两息，红色门子算一息
				if (analyseItem.cbMenEye[0] > GameConstants.CARD_ESPECIAL_TYPE_NIAO
						|| analyseItem.cbMenEye[1] > GameConstants.CARD_ESPECIAL_TYPE_NIAO) {
					this.GRR._count_pick_niao = GameConstants_YJGHZ.WIK_MEN_ZI;
					if (analyseItem.cbMenEye[0] > GameConstants.CARD_ESPECIAL_TYPE_NIAO) {
						analyseItem.cbMenEye[0] -= GameConstants.CARD_ESPECIAL_TYPE_NIAO;
					}
					if (analyseItem.cbMenEye[1] > GameConstants.CARD_ESPECIAL_TYPE_NIAO) {
						analyseItem.cbMenEye[1] -= GameConstants.CARD_ESPECIAL_TYPE_NIAO;
					}
				}

				List<Integer> magicCardLogicValue = Lists.newArrayList();
				magicCardLogicValue.add(2);
				magicCardLogicValue.add(7);
				magicCardLogicValue.add(10);
				if (magicCardLogicValue.contains(_logic.get_card_value(analyseItem.cbMenEye[0]))
						&& magicCardLogicValue.contains(_logic.get_card_value(analyseItem.cbMenEye[1]))) {
					temp_total_hu_xi += 1;
					temp_hu_xi += 1;
				}
			}

			// 一点朱
			// if (_logic.calculate_hong_pai_count_yjghz(analyseItem) == 1) {
			// temp_hu_xi += 50;
			// temp_total_hu_xi += 50;
			// }
			// // 十三红
			// if (_logic.calculate_hong_pai_count_yjghz(analyseItem) == 13) {
			// temp_hu_xi += 50;
			// temp_total_hu_xi += 50;
			// }
			// // 十四红
			// if (_logic.calculate_hong_pai_count_yjghz(analyseItem) == 15) {
			// temp_hu_xi += 100;
			// temp_total_hu_xi += 100;
			// }
			// // 十五红
			// if (_logic.calculate_hong_pai_count_yjghz(analyseItem) == 15) {
			// temp_hu_xi += 200;
			// temp_total_hu_xi += 200;
			// }
			// // 黑
			// if (_logic.calculate_hei_dui_zi_pai_count_yjghz(analyseItem) ==
			// _logic.calculate_all_pai_count_yjghz(analyseItem)) {
			// temp_hu_xi += 200;
			// temp_total_hu_xi += 200;
			// } else if (_logic.calculate_hei_pai_count_yjghz(analyseItem) ==
			// _logic.calculate_all_pai_count_yjghz(analyseItem)) {
			// temp_hu_xi += 100;
			// temp_total_hu_xi += 100;
			// }
			// // 对子胡
			// if (_logic.calculate_dui_zi_pai_count_yjghz(analyseItem) ==
			// _logic.calculate_all_pai_count_yjghz(analyseItem)) {
			// temp_hu_xi += 100;
			// temp_total_hu_xi += 100;
			// }
			// // 大字牌
			// if (_logic.calculate_da_pai_count_yiyang(analyseItem) >= 20) {
			// temp_hu_xi += 100;
			// temp_total_hu_xi += 100;
			// }
			// // 小字牌
			// if (_logic.calculate_xiao_pai_count_yiyang(analyseItem) >= 20) {
			// temp_hu_xi += 100;
			// temp_total_hu_xi += 100;
			// }
			// 无息平
			if (temp_total_hu_xi == 0 && has_rule(GameConstants_YJGHZ.GAME_RULE_WU_XI_PING) && weaveCount == 0) {
				temp_hu_xi += 50;
				temp_total_hu_xi += 50;
			}
			// // 吊吊手
			// if (_logic.get_card_count_by_index(cbCardIndexTemp) == 2 &&
			// has_rule(GameConstants_YJGHZ.GAME_RULE_DIAO_DIAO_SHOU)) {
			// temp_hu_xi += 100;
			// temp_total_hu_xi += 100;
			// }

			int base_xi = 5;

			if (temp_hu_xi >= base_xi) {
				if (temp_total_hu_xi >= max_hu_xi) {
					max_hu_index = i;
					max_hu_xi = temp_total_hu_xi;
				}
			}
		}
		if (max_hu_index == -1) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		int max_kan_count = 0;
		for (int i = 0; i < analyseItemArray.size(); i++) {
			if (max_kan_count < kan_count[i]) {
				max_kan_count = kan_count[i];
				max_hu_index = i;
			}
		}

		analyseItem = analyseItemArray.get(max_hu_index);
		temp_total_hu_xi = 0;
		for (int j = 0; j < 7; j++) {
			if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL)
				break;
			_hu_weave_items[seat_index][j].weave_kind = analyseItem.cbWeaveKind[j];
			_hu_weave_items[seat_index][j].center_card = analyseItem.cbCenterCard[j];
			_hu_weave_items[seat_index][j].weave_card = analyseItem.cbCardData[j];
			_hu_weave_items[seat_index][j].hu_xi = _logic.get_weave_hu_xi_yjghz(_hu_weave_items[seat_index][j]);
			_hu_weave_count[seat_index] = j + 1;

			temp_total_hu_xi += _hu_weave_items[seat_index][j].hu_xi;
		}

		if (analyseItem.curCardEye == true) {
			if (analyseItem.cbCardEye > GameConstants.CARD_ESPECIAL_TYPE_NIAO) {
				this.GRR._count_pick_niao = GameConstants_YJGHZ.WIK_MEN_ZI;
				analyseItem.cbCardEye -= GameConstants.CARD_ESPECIAL_TYPE_NIAO;
			}

			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].hu_xi = 1;
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].weave_card[0] = analyseItem.cbCardEye;
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].weave_card[1] = analyseItem.cbCardEye;
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].center_card = analyseItem.cbCardEye;
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].public_card = analyseItem.cbCardEye;
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].weave_kind = GameConstants_YJGHZ.WIK_MEN_ZI;

			_hu_weave_count[seat_index]++;
			temp_total_hu_xi += 1;

		} else if (analyseItem.cbMenEye[0] != 0) {

			// 胡红色门子算两息，红色门子算一息
			if (analyseItem.cbMenEye[0] > GameConstants.CARD_ESPECIAL_TYPE_NIAO || analyseItem.cbMenEye[1] > GameConstants.CARD_ESPECIAL_TYPE_NIAO) {
				this.GRR._count_pick_niao = GameConstants_YJGHZ.WIK_MEN_ZI;
				if (analyseItem.cbMenEye[0] > GameConstants.CARD_ESPECIAL_TYPE_NIAO) {
					analyseItem.cbMenEye[0] -= GameConstants.CARD_ESPECIAL_TYPE_NIAO;
				}
				if (analyseItem.cbMenEye[1] > GameConstants.CARD_ESPECIAL_TYPE_NIAO) {
					analyseItem.cbMenEye[1] -= GameConstants.CARD_ESPECIAL_TYPE_NIAO;
				}
			}

			List<Integer> magicCardLogicValue = Lists.newArrayList();
			magicCardLogicValue.add(2);
			magicCardLogicValue.add(7);
			magicCardLogicValue.add(10);
			if (magicCardLogicValue.contains(_logic.get_card_value(analyseItem.cbMenEye[0]))
					&& magicCardLogicValue.contains(_logic.get_card_value(analyseItem.cbMenEye[1]))) {
				_hu_weave_items[seat_index][_hu_weave_count[seat_index]].hu_xi = 1;
				temp_total_hu_xi += 1;
			} else {
				_hu_weave_items[seat_index][_hu_weave_count[seat_index]].hu_xi = 0;
			}
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].weave_card[0] = analyseItem.cbMenEye[0];
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].weave_card[1] = analyseItem.cbMenEye[1];
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].center_card = analyseItem.cbMenEye[0];
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].public_card = analyseItem.cbMenEye[1];
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].weave_kind = GameConstants_YJGHZ.WIK_MEN_ZI;

			_hu_weave_count[seat_index]++;
		}

		for (int i = 0; i < analyseItem.cbWeaveKind.length; i++) {
			if (analyseItem.cbCenterCard[i] >= GameConstants.CARD_ESPECIAL_TYPE_NIAO) {
				analyseItem.cbCenterCard[i] -= GameConstants.CARD_ESPECIAL_TYPE_NIAO;
			}
		}

		boolean have_ming_tang = false;
		// 一点朱
		if (_logic.calculate_hong_pai_count_yjghz(analyseItem) == 1) {
			chiHuRight.opr_or(GameConstants_YJGHZ.CHR_YI_DIAN_ZHU);
			have_ming_tang = true;
		}
		// 十三红
		if (_logic.calculate_hong_pai_count_yjghz(analyseItem) == 13) {
			chiHuRight.opr_or(GameConstants_YJGHZ.CHR_SHI_SAN_HONG);
			have_ming_tang = true;
		}
		// 十四红
		if (_logic.calculate_hong_pai_count_yjghz(analyseItem) == 14) {
			chiHuRight.opr_or(GameConstants_YJGHZ.CHR_SHI_SI_HONG);
			have_ming_tang = true;
		}
		// 十五红
		if (_logic.calculate_hong_pai_count_yjghz(analyseItem) >= 15) {
			chiHuRight.opr_or(GameConstants_YJGHZ.CHR_SHI_WU_HONG);
			have_ming_tang = true;
		}
		// 黑
		if (_logic.calculate_hei_dui_zi_pai_count_yjghz(analyseItem) == _logic.calculate_all_pai_count_yjghz(analyseItem)) {
			chiHuRight.opr_or(GameConstants_YJGHZ.CHR_HEI_DUI_ZI);
			have_ming_tang = true;
		} else if (_logic.calculate_hei_pai_count_yjghz(analyseItem) == _logic.calculate_all_pai_count_yjghz(analyseItem)) {
			chiHuRight.opr_or(GameConstants_YJGHZ.CHR_HEI_ZI);
			have_ming_tang = true;
		}
		// 对子胡
		if (_logic.calculate_dui_zi_pai_count_yjghz(analyseItem) == _logic.calculate_all_pai_count_yjghz(analyseItem)
				&& chiHuRight.opr_and(GameConstants_YJGHZ.CHR_HEI_DUI_ZI).is_empty()) {
			chiHuRight.opr_or(GameConstants_YJGHZ.CHR_DUI_ZI);
			have_ming_tang = true;
		}
		// 大字牌
		if (_logic.calculate_da_pai_count_qlhf(analyseItem) >= 20) {
			chiHuRight.opr_or(GameConstants_YJGHZ.CHR_DA_ZI_PAI);
			have_ming_tang = true;
		}
		// 小字牌
		if (_logic.calculate_xiao_pai_count_qlhf(analyseItem) >= 20) {
			chiHuRight.opr_or(GameConstants_YJGHZ.CHR_XIAO_ZI_PAI);
			have_ming_tang = true;
		}
		// 海底胡
		if (this.GRR._left_card_count == 0) {
			chiHuRight.opr_or(GameConstants_YJGHZ.CHR_HAI_DI);
			have_ming_tang = true;
		}
		// 无息平
		if (temp_total_hu_xi == 0 && has_rule(GameConstants_YJGHZ.GAME_RULE_WU_XI_PING) && weaveCount == 0) {
			chiHuRight.opr_or(GameConstants_YJGHZ.CHR_WU_XI);
			have_ming_tang = true;
		}
		// 项项息
		if (temp_total_hu_xi == 7 && chiHuRight.opr_and(GameConstants_YJGHZ.CHR_DUI_ZI).is_empty()
				&& chiHuRight.opr_and(GameConstants_YJGHZ.CHR_HEI_DUI_ZI).is_empty()) {
			chiHuRight.opr_or(GameConstants_YJGHZ.CHR_XIANG_XIANG_XI);
			have_ming_tang = true;
		}
		// 吊吊手
		if (_logic.get_card_count_by_index(cbCardIndexTemp) == 2 && has_rule(GameConstants_YJGHZ.GAME_RULE_DIAO_DIAO_SHOU)) {
			chiHuRight.opr_or(GameConstants_YJGHZ.CHR_DIAO_DIAO);
			have_ming_tang = true;
		}
		if (weaveCount == 0 && _handler != _handler_chuli_firstcards && chiHuRight.opr_and(GameConstants_YJGHZ.CHR_WU_XI).is_empty()) {
			chiHuRight.opr_or(GameConstants_YJGHZ.CHR_QI_SHOU_BAO_TING);
			have_ming_tang = true;
		}
		if (weaveCount == 1 && banker_qi_shou_qing && seat_index == _cur_banker && _handler != _handler_chuli_firstcards
				&& chiHuRight.opr_and(GameConstants_YJGHZ.CHR_WU_XI).is_empty()) {
			chiHuRight.opr_or(GameConstants_YJGHZ.CHR_QI_SHOU_BAO_TING);
			have_ming_tang = true;
		}

		if (!have_ming_tang) {
			cal_yuan(seat_index, provider_index);
			if (player_yuan[seat_index] != 0) {
				return GameConstants_YJGHZ.WIK_CHI_HU;
			}

			for (int j = 0; j < _hu_weave_count[seat_index]; j++) {
				if (_hu_weave_items[seat_index][j].weave_kind == GameConstants_YJGHZ.WIK_KAN) {
					return GameConstants_YJGHZ.WIK_CHI_HU;
				}
				if (_hu_weave_items[seat_index][j].weave_kind == GameConstants_YJGHZ.WIK_WAI) {
					return GameConstants_YJGHZ.WIK_CHI_HU;
				}
			}

			if (analyseItem.curCardEye == true && seat_index == provider_index && analyseItem.cbCardEye == cur_card) {
				return GameConstants_YJGHZ.WIK_CHI_HU;
			}
			if (analyseItem.curCardEye == true && cards_index[_logic.switch_to_card_index(analyseItem.cbCardEye)] == 3) {
				return GameConstants_YJGHZ.WIK_CHI_HU;
			}
			return GameConstants_YJGHZ.WIK_NULL;
		}
		return GameConstants_YJGHZ.WIK_CHI_HU;
	}

	/**
	 * 听牌
	 * 
	 * @param cards
	 * @param cards_index
	 * @param weaveItem
	 * @param cbWeaveCount
	 * @returnt
	 */
	public int get_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index, int provate_index) {

		// 复制数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_HH_INDEX];
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int mj_count = GameConstants.MAX_HH_INDEX;

		for (int i = 0; i < mj_count; i++) {
			if (this._logic.is_magic_index(i))
				continue;
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, seat_index, provate_index, cbCurrentCard,
					chr, GameConstants.WIK_ZI_MO, true)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}

		if (count >= mj_count) {
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	// 获取动作序列最高等级
	public int get_action_list_rank(int action_count, int action[], int seat_index, int provider) {
		int MAX_HH_INDEX = 0;

		for (int i = 0; i < action_count; i++) {
			int index = get_action_rank(action[i], seat_index, provider);
			if (MAX_HH_INDEX < index) {
				MAX_HH_INDEX = index;
			}

		}

		return MAX_HH_INDEX;
	}

	/**
	 * 获取操作的优先等级
	 * 
	 **/
	// 获取动作等级
	public int get_action_rank(int player_action, int seat_index, int provider) {
		// 溜等级
		if (player_action == GameConstants_YJGHZ.WIK_QING_NEI) {
			return 60;
		}
		// 溜等级
		if (player_action == GameConstants_YJGHZ.WIK_QING_WAI) {
			return 60;
		}

		// 歪操作
		if (player_action == GameConstants_YJGHZ.WIK_WAI) {
			return 50;
		}
		// 溜等级
		if (player_action == GameConstants_YJGHZ.WIK_PIAO) {
			return 40;
		}
		// 自摸牌等级
		if (player_action == GameConstants.WIK_ZI_MO) {
			return 30;
		}
		// 碰牌等级
		if (player_action == GameConstants.WIK_PENG) {
			return 20;
		}

		// 上牌等级
		if (player_action == GameConstants.WIK_RIGHT || player_action == GameConstants.WIK_CENTER || player_action == GameConstants.WIK_LEFT
				|| player_action == GameConstants.WIK_XXD || player_action == GameConstants.WIK_DDX || player_action == GameConstants.WIK_EQS
				|| player_action == GameConstants.WIK_YWS) {
			return 10;
		}

		return 0;
	}

	public int check_piao(int card_index[], WeaveItem WeaveItem[], int cbWeaveCount, int cur_card, GangCardResult gangCardResult, int seat_index,
			int provider, boolean is_dispath) {
		int cbActionMask = GameConstants.WIK_NULL;
		if (seat_index == provider) {
			// 手上杠牌
			for (int i = 0; i < cbWeaveCount; i++) {
				if (WeaveItem[i].weave_kind == GameConstants.WIK_PENG) {
					if (WeaveItem[i].center_card == cur_card) {
						cbActionMask |= GameConstants_YJGHZ.WIK_PIAO;

						int index = gangCardResult.cbCardCount++;
						gangCardResult.cbCardData[index] = cur_card;
						gangCardResult.isPublic[index] = 1;// 明刚
						gangCardResult.type[index] = GameConstants_YJGHZ.WIK_PIAO;
					}
					if (!is_dispath) {
						for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
							if (_logic.switch_to_card_data(j) == WeaveItem[i].center_card && card_index[j] > 0) {
								cbActionMask |= GameConstants_YJGHZ.WIK_PIAO;

								int index = gangCardResult.cbCardCount++;
								gangCardResult.cbCardData[index] = WeaveItem[i].center_card;
								gangCardResult.isPublic[index] = 1;// 明刚
								gangCardResult.type[index] = GameConstants_YJGHZ.WIK_PIAO;
							}
						}
					}

				}
			}
		}

		// if (seat_index != provider) {
		// for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
		// if (card_index[j] == 3 && _logic.switch_to_card_data(j) == cur_card)
		// {
		// cbActionMask |= GameConstants_YJGHZ.WIK_PIAO;
		//
		// int index = gangCardResult.cbCardCount++;
		// gangCardResult.cbCardData[index] = cur_card;
		// gangCardResult.isPublic[index] = 1;// 明刚
		// gangCardResult.type[index] = GameConstants_YJGHZ.WIK_PIAO;
		// }
		// }
		// }
		return cbActionMask;
	}

	public boolean estimate_player_dispatch_qing_piao_respond(int seat_index, int card) {
		boolean bAroseAction = false;// 出现(是否)有
		// 内元
		GangCardResult gangCardResult = new GangCardResult();
		int cbActionMask = check_qing(GRR._cards_index[seat_index], GRR._weave_items[seat_index], GRR._weave_count[seat_index], card, gangCardResult);
		if (cbActionMask != GameConstants.WIK_NULL) {//
			for (int i = 0; i < gangCardResult.cbCardCount; i++) {
				_playerStatus[seat_index].add_action_card(0, gangCardResult.cbCardData[i], gangCardResult.type[i], seat_index);
				_playerStatus[seat_index].add_action(GameConstants.WIK_NULL);
				_playerStatus[seat_index].add_action(gangCardResult.type[i]);// 溜牌
				_playerStatus[seat_index].add_pass(card, seat_index);

				_playerStatus[seat_index].set_status(GameConstants.Player_Status_OPR_CARD);// 操作状态
			}
		}

		// (外元)飘
		gangCardResult.cbCardCount = 0;
		cbActionMask = check_piao(GRR._cards_index[seat_index], GRR._weave_items[seat_index], GRR._weave_count[seat_index], card, gangCardResult,
				seat_index, seat_index, true);
		if (cbActionMask != GameConstants.WIK_NULL) {// 有飘
			for (int j = 0; j < gangCardResult.cbCardCount; j++) {
				_playerStatus[seat_index].add_action_card(1, gangCardResult.cbCardData[j], GameConstants_YJGHZ.WIK_PIAO, seat_index);
				_playerStatus[seat_index].add_action(GameConstants_YJGHZ.WIK_PIAO);// 飘牌
				_playerStatus[seat_index].add_action(GameConstants.WIK_NULL);
				_playerStatus[seat_index].add_pass(card, seat_index);

				_playerStatus[seat_index].set_status(GameConstants.Player_Status_OPR_CARD);// 操作状态
			}
			bAroseAction = true;
		}

		return bAroseAction;
	}

	public int check_qing(int card_index[], WeaveItem WeaveItem[], int cbWeaveCount, int cur_card, GangCardResult gangCardResult) {
		int cbActionMask = GameConstants.WIK_NULL;
		// 手上杠牌
		// for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
		// if (card_index[i] == 4) {
		// cbActionMask |= GameConstants_YJGHZ.WIK_QING_NEI;
		// int index = gangCardResult.cbCardCount++;
		// gangCardResult.cbCardData[index] = _logic.switch_to_card_data(i);
		// gangCardResult.isPublic[index] = 0;// 暗杠
		// gangCardResult.type[index] = GameConstants_YJGHZ.WIK_QING_NEI;
		// }
		// }

		if (card_index[_logic.switch_to_card_index(cur_card)] == 3) {
			cbActionMask |= GameConstants_YJGHZ.WIK_QING_NEI;
			int index = gangCardResult.cbCardCount++;
			gangCardResult.cbCardData[index] = cur_card;
			gangCardResult.isPublic[index] = 0;// 暗杠
			gangCardResult.type[index] = GameConstants_YJGHZ.WIK_QING_NEI;
		}

		for (int w = 0; w < cbWeaveCount; w++) {
			if (WeaveItem[w].weave_kind == GameConstants_YJGHZ.WIK_WAI && WeaveItem[w].center_card == cur_card) {
				cbActionMask |= GameConstants_YJGHZ.WIK_QING_WAI;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = cur_card;
				gangCardResult.isPublic[index] = 0;// 暗杠
				gangCardResult.type[index] = GameConstants_YJGHZ.WIK_QING_WAI;
			}
		}
		return cbActionMask;
	}

	public boolean estimate_player_chipeng_qing_piao_respond(int seat_index, int provider, int card) {
		boolean is_liu = false;
		int cbActionMask = GameConstants.WIK_NULL;

		GangCardResult gangCardResult = new GangCardResult();
		// 手上杠牌
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			if (GRR._cards_index[seat_index][i] == 4) {
				cbActionMask |= GameConstants_YJGHZ.WIK_QING_NEI;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = _logic.switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;// 暗杠
				gangCardResult.type[index] = GameConstants_YJGHZ.WIK_QING_NEI;
			}
		}
		if (cbActionMask != GameConstants.WIK_NULL) {//
			for (int i = 0; i < gangCardResult.cbCardCount; i++) {
				_playerStatus[seat_index].add_action_card(0, gangCardResult.cbCardData[i], GameConstants_YJGHZ.WIK_QING_NEI, seat_index);
				_playerStatus[seat_index].add_action(GameConstants_YJGHZ.WIK_QING_NEI);// 溜牌
				_playerStatus[seat_index].add_action(GameConstants.WIK_NULL);
				_playerStatus[seat_index].add_pass(gangCardResult.cbCardData[i], seat_index);

				_playerStatus[seat_index].set_status(GameConstants.Player_Status_OPR_CARD);// 操作状态

			}
			is_liu = true;
		}

		for (int w = 0; w < GRR._weave_count[seat_index]; w++) {
			if (GRR._cards_index[seat_index][_logic.switch_to_card_index(GRR._weave_items[seat_index][w].center_card)] == 1
					&& GRR._weave_items[seat_index][w].weave_kind == GameConstants_YJGHZ.WIK_WAI) {
				_playerStatus[seat_index].add_action_card(0, GRR._weave_items[seat_index][w].center_card, GameConstants_YJGHZ.WIK_QING_WAI,
						seat_index);
				_playerStatus[seat_index].add_action(GameConstants_YJGHZ.WIK_QING_WAI);// 溜牌
				_playerStatus[seat_index].add_action(GameConstants.WIK_NULL);
				_playerStatus[seat_index].add_pass(GRR._weave_items[seat_index][w].center_card, seat_index);

				_playerStatus[seat_index].set_status(GameConstants.Player_Status_OPR_CARD);// 操作状态
				is_liu = true;
				// break;
			}
		}
		// for (int w = 0; w < GRR._weave_count[seat_index] &&
		// GRR._cards_index[seat_index][_logic.switch_to_card_index(card)] == 1;
		// w++) {
		// if (GRR._weave_items[seat_index][w].center_card == card &&
		// GRR._weave_items[seat_index][w].weave_kind ==
		// GameConstants_YJGHZ.WIK_WAI) {
		// _playerStatus[seat_index].add_action_card(0, card,
		// GameConstants_YJGHZ.WIK_QING_WAI, seat_index);
		// _playerStatus[seat_index].add_action(GameConstants_YJGHZ.WIK_QING_WAI);//
		// 溜牌
		// _playerStatus[seat_index].add_action(GameConstants.WIK_NULL);
		// _playerStatus[seat_index].add_pass(card, seat_index);
		//
		// _playerStatus[seat_index].set_status(GameConstants.Player_Status_OPR_CARD);//
		// 操作状态
		// is_liu = true;
		// break;
		// }
		// }

		return is_liu;
	}

	public int estimate_player_wai_respond(int seat_index, int card_data) {

		if (_logic.is_valid_card(card_data) == false) {
			return GameConstants.WIK_NULL;
		}

		int peng_index = 0;
		for (; peng_index < this._cannot_peng_count[seat_index]; peng_index++) {
			if (this._cannot_peng[seat_index][peng_index] == card_data) {
				break;
			}
		}
		if (peng_index != this._cannot_peng_count[seat_index])
			return GameConstants.WIK_NULL;

		if (GRR._cards_index[seat_index][_logic.switch_to_card_index(card_data)] >= 2) {
			return GameConstants_YJGHZ.WIK_WAI;
		}

		return GameConstants.WIK_NULL;
	}

	public boolean exe_piao(int seat_index, int provider, int action, int card, int type, int lou_operate) {
		// 出牌
		_last_player = provider;
		this._handler = this._handler_piao;
		this._handler_piao.reset_status(seat_index, provider, action, card, type, lou_operate);
		this._handler.exe(this);
		this._user_out_card_count[seat_index]++;
		return true;
	}

	public boolean exe_wai(int seat_index, int provider, int action, int card, int type, int lou_operate) {
		// 出牌
		_last_player = provider;
		this._handler = this._handler_wai;
		this._handler_wai.reset_status(seat_index, provider, action, card, type, lou_operate);
		this._handler.exe(this);

		return true;
	}

	public boolean exe_liu(int seat_index, int provider, int action, int card, int type, int lou_operate) {
		// 出牌
		_last_player = provider;
		this._handler = this._handler_liu;
		this._handler_liu.reset_status(seat_index, provider, action, card, type, lou_operate);
		this._handler.exe(this);

		return true;
	}

	public int get_ming_tang_fen(int seat_index) {
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		int fen = 0;

		// 一点朱
		if (!chr.opr_and(GameConstants_YJGHZ.CHR_YI_DIAN_ZHU).is_empty()) {
			fen += 50;
		}
		// 十三红
		if (!chr.opr_and(GameConstants_YJGHZ.CHR_SHI_SAN_HONG).is_empty()) {
			fen += 50;
		}
		// 十四红
		if (!chr.opr_and(GameConstants_YJGHZ.CHR_SHI_SI_HONG).is_empty()) {
			fen += 100;
		}
		// 十五红
		if (!chr.opr_and(GameConstants_YJGHZ.CHR_SHI_WU_HONG).is_empty()) {
			fen += 200;
		}
		// 对子胡
		if (!chr.opr_and(GameConstants_YJGHZ.CHR_DUI_ZI).is_empty()) {
			fen += 100;
		} else // 项项息
		if (!chr.opr_and(GameConstants_YJGHZ.CHR_XIANG_XIANG_XI).is_empty()) {
			fen += 50;
		}
		// 黑子胡
		if (!chr.opr_and(GameConstants_YJGHZ.CHR_HEI_ZI).is_empty()) {
			fen += 100;
		}
		// 黑对子胡
		if (!chr.opr_and(GameConstants_YJGHZ.CHR_HEI_DUI_ZI).is_empty()) {
			fen += 200;
		}
		// 大字牌
		if (!chr.opr_and(GameConstants_YJGHZ.CHR_DA_ZI_PAI).is_empty()) {
			fen += 100;
		}
		// 小字牌
		if (!chr.opr_and(GameConstants_YJGHZ.CHR_XIAO_ZI_PAI).is_empty()) {
			fen += 100;
		}
		// 无息平
		if (!chr.opr_and(GameConstants_YJGHZ.CHR_WU_XI).is_empty()) {
			fen += 50;
		}

		// 吊吊手
		if (!chr.opr_and(GameConstants_YJGHZ.CHR_DIAO_DIAO).is_empty()) {
			fen += 100;
		}
		// 九对半
		if (!chr.opr_and(GameConstants_YJGHZ.CHR_JIU_DUI_BAN).is_empty()) {
			fen += 100;
		}
		// 天胡
		if (!chr.opr_and(GameConstants_YJGHZ.CHR_TIAN_HU).is_empty()) {
			fen += 200;
		}
		return fen;
	}

	public int get_fan(int seat_index) {
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		int fan = 1;

		// 吊吊手
		if (!chr.opr_and(GameConstants_YJGHZ.CHR_HAI_DI).is_empty()) {
			fan *= 4;
		}
		// 吊吊手
		if (nei_yuan_count != 0) {
			for (int i = 0; i < nei_yuan_count; i++) {
				fan *= 4;
			}
		}
		// 吊吊手
		if (wai_yuan_count != 0) {
			fan *= 2 << (wai_yuan_count - 1);
		}

		return fan == 0 ? 1 : fan;
	}

	public int get_limit_fen() {
		if (has_rule(GameConstants_YJGHZ.GAME_RULE_FENG_DING_100)) {
			return 100;
		}
		if (has_rule(GameConstants_YJGHZ.GAME_RULE_FENG_DING_200)) {
			return 200;
		}
		return 200;
	}

	public void cal_yuan(int seat_index, int provide_index) {
		player_yuan[seat_index] = 0;
		nei_yuan_count = 0;
		wai_yuan_count = 0;

		int[] cards_index = new int[GameConstants.MAX_HH_INDEX];
		for (int j = 0; j < _hu_weave_count[seat_index]; j++) {

			// 飘-外元
			if (_hu_weave_items[seat_index][j].weave_kind == GameConstants_YJGHZ.WIK_PIAO) {
				if (wai_yuan_count == 0) {
					GRR._chi_hu_rights[seat_index].opr_or(GameConstants_YJGHZ.CHR_WAI_YUAN);
				}
				wai_yuan_count++;
				continue;
			}

			// 清-内元
			if (_hu_weave_items[seat_index][j].weave_kind == GameConstants_YJGHZ.WIK_QING_WAI
					|| _hu_weave_items[seat_index][j].weave_kind == GameConstants_YJGHZ.WIK_QING_NEI) {
				if (nei_yuan_count == 0) {
					GRR._chi_hu_rights[seat_index].opr_or(GameConstants_YJGHZ.CHR_NEI_YUAN);
				}
				nei_yuan_count++;
				continue;
			}

			// 碰-外元
			if (_hu_weave_items[seat_index][j].weave_kind == GameConstants_YJGHZ.WIK_PENG) {
				int center_card = _hu_weave_items[seat_index][j].center_card;
				if (center_card > GameConstants.CARD_ESPECIAL_TYPE_NIAO) {
					center_card -= GameConstants.CARD_ESPECIAL_TYPE_NIAO;
				}
				if (GRR._cards_index[seat_index][_logic.switch_to_card_index(center_card)] == 1
						|| (GRR._cards_index[seat_index][_logic.switch_to_card_index(center_card)] == 0
								&& GRR._chi_hu_card[seat_index][0] == _hu_weave_items[seat_index][j].center_card)) {
					if (wai_yuan_count == 0) {
						GRR._chi_hu_rights[seat_index].opr_or(GameConstants_YJGHZ.CHR_WAI_YUAN);
					}
					wai_yuan_count++;

					continue;
				}

				boolean flag = false;
				for (int k = 0; k < _hu_weave_count[seat_index]; k++) {
					if (k == j) {
						continue;
					}
					for (int wd = 0; wd < _hu_weave_items[seat_index][j].weave_card.length; wd++) {
						if (_hu_weave_items[seat_index][k].weave_card[wd] == _hu_weave_items[seat_index][j].center_card) {
							if (nei_yuan_count == 0) {
								GRR._chi_hu_rights[seat_index].opr_or(GameConstants_YJGHZ.CHR_WAI_YUAN);
							}
							wai_yuan_count++;
							flag = true;
							break;
						}
					}
					if (flag) {
						break;
					}
				}
				if (flag) {
					continue;
				}
			}

			// 歪-内元
			if (_hu_weave_items[seat_index][j].weave_kind == GameConstants_YJGHZ.WIK_WAI) {
				int center_card = _hu_weave_items[seat_index][j].center_card;
				if (center_card > GameConstants.CARD_ESPECIAL_TYPE_NIAO) {
					center_card -= GameConstants.CARD_ESPECIAL_TYPE_NIAO;
				}
				if (GRR._cards_index[seat_index][_logic.switch_to_card_index(center_card)] == 1) {
					if (nei_yuan_count == 0) {
						GRR._chi_hu_rights[seat_index].opr_or(GameConstants_YJGHZ.CHR_NEI_YUAN);
					}
					nei_yuan_count++;
					continue;
				}

				boolean flag = false;
				for (int k = 0; k < _hu_weave_count[seat_index]; k++) {
					if (_hu_weave_items[seat_index][j].center_card == _hu_weave_items[seat_index][k].center_card || k == j) {
						continue;
					}
					for (int wd = 0; wd < _hu_weave_items[seat_index][j].weave_card.length; wd++) {
						if (_hu_weave_items[seat_index][k].weave_card[wd] == _hu_weave_items[seat_index][j].center_card) {
							if (nei_yuan_count == 0) {
								GRR._chi_hu_rights[seat_index].opr_or(GameConstants_YJGHZ.CHR_NEI_YUAN);
							}
							nei_yuan_count++;
							flag = true;
							break;
						}
					}
					if (flag) {
						break;
					}
				}
				if (flag) {
					continue;
				}
			}

			for (int wd = 0; wd < _hu_weave_items[seat_index][j].weave_card.length; wd++) {
				if (_hu_weave_items[seat_index][j].weave_card[wd] == 0) {
					continue;
				}
				int card = _hu_weave_items[seat_index][j].weave_card[wd];
				if (card > GameConstants.CARD_ESPECIAL_TYPE_NIAO) {
					card -= GameConstants.CARD_ESPECIAL_TYPE_NIAO;
				}
				cards_index[_logic.switch_to_card_index(card)]++;
			}
		}
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			if (cards_index[i] == 4 && GRR._cards_index[seat_index][i] != 4) {
				if (qi_shou_four[seat_index].contains(i)) {
					if (nei_yuan_count == 0) {
						GRR._chi_hu_rights[seat_index].opr_or(GameConstants_YJGHZ.CHR_NEI_YUAN);
					}
					nei_yuan_count++;
				} else {
					if (provide_index == seat_index && GRR._chi_hu_card[seat_index][0] != 0
							&& i == _logic.switch_to_card_index(GRR._chi_hu_card[seat_index][0])) {
						if (nei_yuan_count == 0) {
							GRR._chi_hu_rights[seat_index].opr_or(GameConstants_YJGHZ.CHR_NEI_YUAN);
						}
						nei_yuan_count++;
					} else {
						if (wai_yuan_count == 0) {
							GRR._chi_hu_rights[seat_index].opr_or(GameConstants_YJGHZ.CHR_WAI_YUAN);
						}
						wai_yuan_count++;
					}
				}
			}
		}

		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			if (GRR._cards_index[seat_index][i] == 4) {
				if (nei_yuan_count == 0) {
					GRR._chi_hu_rights[seat_index].opr_or(GameConstants_YJGHZ.CHR_NEI_YUAN);
				}
				nei_yuan_count++;
			}
		}

		if (wai_yuan_count != 0) {
			player_yuan[seat_index] = 2;
		}
		if (nei_yuan_count != 0) {
			player_yuan[seat_index] = 1;
		}
	}

	/**
	 * 算分
	 * 
	 * @param seat_index
	 * @param provide_index
	 * @param operate_card
	 * @param zimo
	 */
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		for (int j = 0; j < _hu_weave_count[seat_index]; j++) {
			if (_hu_weave_items[seat_index][j].center_card > GameConstants.CARD_ESPECIAL_TYPE_NIAO) {
				this.GRR._count_pick_niao = _hu_weave_items[seat_index][j].weave_kind;
				_hu_weave_items[seat_index][j].center_card -= GameConstants.CARD_ESPECIAL_TYPE_NIAO;
			}
		}
		cal_yuan(seat_index, provide_index);

		int kan_count = 0;
		for (int j = 0; j < _hu_weave_count[seat_index]; j++) {
			if (_hu_weave_items[seat_index][j].weave_kind == GameConstants_YJGHZ.WIK_KAN) {
				kan_count++;
			}
			if (_hu_weave_items[seat_index][j].weave_kind == GameConstants_YJGHZ.WIK_WAI) {
				kan_count++;
			}
			if (_hu_weave_items[seat_index][j].weave_kind == GameConstants_YJGHZ.WIK_QING_NEI) {
				kan_count++;
			}
			if (_hu_weave_items[seat_index][j].weave_kind == GameConstants_YJGHZ.WIK_QING_WAI) {
				kan_count++;
			}
		}

		int hu_xi = 0;
		for (int j = 0; j < _hu_weave_count[seat_index]; j++) {
			if (_hu_weave_items[seat_index][j].weave_kind == GameConstants.WIK_NULL)
				break;

			hu_xi += _hu_weave_items[seat_index][j].hu_xi;
		}

		float lChiHuScore = 0;
		if (hu_xi >= 5) {
			lChiHuScore = 5 + get_ming_tang_fen(seat_index);
		} else {
			lChiHuScore = get_ming_tang_fen(seat_index);
		}
		if (!chr.opr_and(GameConstants_YJGHZ.CHR_QI_SHOU_BAO_TING).is_empty() && chr.opr_and(GameConstants_YJGHZ.CHR_WU_XI).is_empty()
				&& chr.opr_and(GameConstants_YJGHZ.CHR_TIAN_HU).is_empty()) {
			lChiHuScore += 50;
		}

		_player_result.ming_tang_count[seat_index] = _player_result.ming_tang_count[seat_index] > (hu_xi + kan_count * 5)
				? _player_result.ming_tang_count[seat_index]
				: (hu_xi + kan_count * 5);
		////////////////////////////////////////////////////// 自摸 算分
		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				int wFanShu = get_fan(seat_index);// 番数

				float s = lChiHuScore + kan_count * 5;
				if (wFanShu > 0) {
					s *= wFanShu;
				}

				if (s > get_limit_fen()) {
					s = get_limit_fen();
				}
				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;
			}
		}
		////////////////////////////////////////////////////// 点炮 算分
		else {
			GRR._chi_hu_rights[provide_index].opr_or(GameConstants_YJGHZ.CHR_FANG_PAO);
			int wFanShu = get_fan(seat_index);// 番数

			float s = lChiHuScore + kan_count * 5;
			if (wFanShu > 0) {
				s *= wFanShu;
			}

			s = s > get_limit_fen() ? s : get_limit_fen();
			// 胡牌分
			GRR._game_score[provide_index] -= s;
			GRR._game_score[seat_index] += s;
		}

		_player_result.ying_xi_count[seat_index] = _player_result.ying_xi_count[seat_index] > (int) GRR._game_score[seat_index]
				? _player_result.ying_xi_count[seat_index]
				: (int) GRR._game_score[seat_index];
		// 设置变量
		GRR._provider[seat_index] = provide_index;
		_status_gang = false;
		_status_gang_hou_pao = false;
	}

	@Override
	public boolean record_effect_action(int seat_index, int effect_type, int effect_count, long effect_indexs[], int time) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_EFFECT_ACTION_RECORD);
		roomResponse.setEffectType(effect_type);
		roomResponse.setTarget(seat_index);
		roomResponse.setEffectCount(effect_count);
		for (int i = 0; i < effect_count; i++) {
			roomResponse.addEffectsIndex(effect_indexs[i]);
		}

		roomResponse.setEffectTime(time);
		if (GRR == null) {
			return true;
		}
		GRR.add_room_response(roomResponse);

		return true;
	}

	/**
	 * 测试线上 实际牌
	 */
	@Override
	public void testRealyCard(int[] realyCards) {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants_XiangXiang.MAX_HH_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}

		this._repertory_card = realyCards;
		_all_card_len = _repertory_card.length;
		GRR._left_card_count = _all_card_len;

		int send_count;
		int have_send_count = 0;
		// 分发扑克
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			send_count = (GameConstants_XiangXiang.MAX_HH_INDEX - 1);
			GRR._left_card_count -= send_count;

			// 一人20张牌,庄家多一张
			_logic.switch_to_cards_index(_repertory_card, have_send_count, send_count, GRR._cards_index[i]);

			have_send_count += send_count;
		}
		DEBUG_CARDS_MODE = false;// 把调试模式关闭
		BACK_DEBUG_CARDS_MODE = false;
		System.err.println("=========开始调试线上牌型 调试模式自动关闭*=========");
	}

	/**
	 * 模拟牌型--相同牌
	 */
	@Override
	public void testSameCard(int[] cards) {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants_XiangXiang.MAX_HH_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			int send_count = GameConstants_XiangXiang.MAX_HH_INDEX - 1;
			if (send_count > cards.length) {
				send_count = cards.length;
			}
			for (int j = 0; j < send_count; j++) {
				GRR._cards_index[i][_logic.switch_to_card_index(cards[j])] += 1;
			}
		}
		DEBUG_CARDS_MODE = false;// 把调试模式关闭
		BACK_DEBUG_CARDS_MODE = false;
	}

	private void test_cards() {

		int cards[] = new int[] { 0x03, 0x03, 0x3, 0x2, 0x7, 0xa, 0x6, 0x7, 0x8, 0x12, 0x12, 0x14, 0x14, 0x15, 0x16, 0x17, 0x17, 0x2, 0x7 };
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants_XiangXiang.MAX_HH_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < cards.length; j++) {
				GRR._cards_index[i][_logic.switch_to_card_index(cards[j])] += 1;
			}
		}

		/********
		 * 下面这个给测试用的 如果开发用 把上面的注释去掉 realyCards
		 **************************/

		if (BACK_DEBUG_CARDS_MODE) {
			if (debug_my_cards != null)
				if (debug_my_cards.length > 56) {
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