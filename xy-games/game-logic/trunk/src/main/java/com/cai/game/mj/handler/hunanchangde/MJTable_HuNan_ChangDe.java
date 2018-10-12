package com.cai.game.mj.handler.hunanchangde;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.RedisConstant;
import com.cai.common.define.ECardType;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.GameDescUtil;
import com.cai.common.util.SpringService;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJType;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.game.util.GameUtilConstants;
import com.cai.redis.service.RedisService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJTable_HuNan_ChangDe extends AbstractMJTable {

	private static final long serialVersionUID = 3596966322374528600L;

	public int add_niao; // 不显示鸟牌 (胡牌玩家无红中奖码)

	protected MJHandlerQiShouHongZhong_HuNan_ChangDe _handler_qishou_hongzhong;

	public MJTable_HuNan_ChangDe(MJType mjType) {
		super(mjType);
	}

	protected boolean on_handler_game_finish(int seat_index, int reason) {
		int real_reason = reason;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_ready[i] = 0;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();
		// 查牌数据
		setGameEndBasicPrama(game_end);

		roomResponse.setLeftCardCount(0);

		load_common_status(roomResponse);
		load_room_info_data(roomResponse);

		// 这里记录了两次，先这样
		RoomInfo.Builder room_info = getRoomInfo();
		game_end.setRoomInfo(room_info);
		game_end.setRunPlayerId(_run_player_id);
		game_end.setRoundOverType(0);
		game_end.setGamePlayerNumber(getTablePlayerNumber());
		game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间
		if (GRR != null) {
			game_end.setRoundOverType(1);
			game_end.setStartTime(GRR._start_time);

			game_end.setGameTypeIndex(GRR._game_type_index);
			roomResponse.setLeftCardCount(GRR._left_card_count);

			// 特别显示的牌
			if (is_mj_type(GameConstants.GAME_TYPE_HUNAN_XIANG_TAN) || is_mj_type(GameConstants.GAME_TYPE_HUNAN_XIANG_TAN_DT)) {
				for (int i = 0; i < GRR._especial_card_count; i++) {
					game_end.addEspecialShowCards(GRR._especial_show_cards[i] + GameConstants.CARD_ESPECIAL_TYPE_DING_GUI);
				}
			} else {
				for (int i = 0; i < GRR._especial_card_count; i++) {
					game_end.addEspecialShowCards(GRR._especial_show_cards[i]);
				}
			}

			GRR._end_type = reason;

			// 杠牌，每个人的分数
			float lGangScore[] = new float[getTablePlayerNumber()];

			if (is_mj_type(GameConstants.GAME_TYPE_HU_NAN_CHANG_DE) || is_mj_type(GameConstants.GAME_TYPE_HUNAN_MJ_CD_DT)) {
				if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HUNAN_HUANG_ZHUANG_HUANG_GANG)
						&& (reason == GameConstants.Game_End_DRAW || reason == GameConstants.Game_End_RELEASE_PLAY)) { // 流局并且荒庄荒杠
					for (int i = 0; i < getTablePlayerNumber(); i++) {
						for (int j = 0; j < getTablePlayerNumber(); j++) {
							_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
						}
					}
				} else {
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
				}
			} else {
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
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				GRR._game_score[i] += lGangScore[i];
				GRR._game_score[i] += GRR._start_hu_score[i]; // 起手胡分数

				// 记录
				_player_result.game_score[i] += GRR._game_score[i];

			}

			load_player_info_data(roomResponse);

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

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();

				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					pnc.addItem(GRR._player_niao_cards[i][j]);
				}
				for (int j = 0; j < GRR._player_niao_count_fei[i]; j++) {
					pnc.addItem(GRR._player_niao_cards_fei[i][j]);
				}

				game_end.addPlayerNiaoCards(pnc);
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				game_end.addHuResult(GRR._hu_result[i]);
				Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
					hc.addItem(GRR._chi_hu_card[i][j]);
				}

				for (int h = 0; h < GRR._chi_hu_card[i].length; h++) {
					game_end.addHuCardData(GRR._chi_hu_card[i][h]);
				}

				game_end.addHuCardArray(hc);
			}

			// 现在权值只有一位
			long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

			// 设置胡牌描述
			set_result_describe();

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
				game_end.setPlayerResult(process_player_result(reason));
			} else {
				// 确定下局庄家
				// 以后谁胡牌，下局谁做庄。
				// 流局,庄家下一个
				// 通炮,放炮的玩家

			}
		} else if ((!is_sys()) && (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM)) {
			end = true;
			real_reason = GameConstants.Game_End_DRAW;// 刘局
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(process_player_result(reason));
		}
		game_end.setEndType(real_reason);

		////////////////////////////////////////////////////////////////////// 得分总的
		roomResponse.setGameEnd(game_end);

		send_response_to_room(roomResponse);

		record_game_round(game_end);

		// 结束后刷新玩家
		// RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
		// roomResponse2.setGameStatus(_game_status);
		// roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		// load_player_info_data(roomResponse2);
		// send_response_to_room(roomResponse2);

		// 超时解散
		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT || reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < getTablePlayerNumber(); j++) {
				Player player = get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");

			}
		}

		if (end && (!is_sys()))// 删除
		{
			PlayerServiceImpl.getInstance().delRoomId(getRoom_id());
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

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {
		if ((has_rule(GameConstants.GAME_RULE_HUNAN_ZIMOHU) && (card_type == GameConstants.HU_CARD_TYPE_PAOHU))) {
			return GameConstants.WIK_NULL;
		}

		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		if (_playerStatus[_seat_index].isAbandoned()) { // 已经弃胡
			return GameConstants.WIK_NULL;
		}

		// 变量定义
		int cbChiHuKind = GameConstants.WIK_NULL;

		// TODO 代理说，常德麻将不能胡七小对
		// long qxd = _logic.is_qi_xiao_dui(cards_index, weaveItems,
		// weave_count,
		// cur_card);
		//
		// if (qxd != GameConstants.WIK_NULL) {
		// cbChiHuKind = GameConstants.WIK_CHI_HU;
		// if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) { // 自摸
		// chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		// } else if (card_type == GameConstants.HU_CARD_TYPE_QIANGGANG) { // 抢杠
		// chiHuRight.opr_or(GameConstants.CHR_HUNAN_QIANG_GANG_HU);
		// } else {
		// chiHuRight.opr_or(GameConstants.CHR_SHU_FAN); // 点炮
		// }
		// }

		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HUNAN_HONGZHONG)) {
			if ((cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD)] == 4)
					|| ((cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD)] == 3) && (cur_card == GameConstants.HZ_MAGIC_CARD))) {
				cbChiHuKind = GameConstants.WIK_CHI_HU;
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) { // 自摸
					chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
				} else if (card_type == GameConstants.HU_CARD_TYPE_QIANGGANG) { // 抢杠
					chiHuRight.opr_or(GameConstants.CHR_HUNAN_QIANG_GANG_HU);
				} else {
					chiHuRight.opr_or(GameConstants.CHR_SHU_FAN); // 点炮
				}
			}
		}

		if (!chiHuRight.is_empty()) { // 如果是上面的七小对或者红中胡牌型
			return cbChiHuKind;
		}

		// // 构造扑克
		// int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		// for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
		// cbCardIndexTemp[i] = cards_index[i];
		// }
		//
		// // 插入扑克
		// if (cur_card != GameConstants.INVALID_VALUE) {
		// cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		// }
		//
		// // 分析扑克
		// List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
		// boolean bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems,
		// weave_count, analyseItemArray, true);

		// 获取癞子牌数据
		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_card_count = _logic.get_magic_card_count();

		if (magic_card_count > 2) { // 一般只有两种癞子牌存在
			magic_card_count = 2;
		}

		for (int i = 0; i < magic_card_count; i++) {
			magic_cards_index[i] = _logic.get_magic_card_index(i);
		}

		boolean bValue = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card), magic_cards_index,
				magic_card_count);

		if (!bValue) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;

		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) { // 自摸
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else if (card_type == GameConstants.HU_CARD_TYPE_QIANGGANG) { // 抢杠
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_QIANG_GANG_HU);
		} else {
			chiHuRight.opr_or(GameConstants.CHR_SHU_FAN); // 点炮
		}

		return cbChiHuKind;
	}

	public boolean estimate_player_out_card_respond(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;

		// 用户状态
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		// 动作判断
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			if (GRR._left_card_count > 0) { // 牌堆还有牌才能碰和杠，不然流局算庄会出错
				// 碰牌判断
				action = _logic.check_peng(GRR._cards_index[i], card);
				if (action != 0) {
					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);
					bAroseAction = true;
				}

				// 杠牌判断
				action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
				if (action != 0) {
					playerStatus.add_action(GameConstants.WIK_GANG);
					playerStatus.add_gang(card, seat_index, 1); // 加上杠
					bAroseAction = true;
				}
			}

			// 可以胡的情况 判断 并且没有弃胡
			if (_playerStatus[i].is_chi_hu_round() && !playerStatus.isAbandoned()) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr, GameConstants.HU_CARD_TYPE_PAOHU, i);

				// 结果判断
				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index); // 吃胡的组合
					bAroseAction = true;
				}
			}
		}

		if (bAroseAction) {
			_resume_player = _current_player; // 保存当前轮到的玩家
			_current_player = GameConstants.INVALID_SEAT; // 有需要操作的玩家。当前玩家为空
			_provide_player = seat_index;
		} else {
			return false;
		}

		return true;
	}

	public boolean exe_qishou_hongzhong(int seat_index) {
		set_handler(_handler_qishou_hongzhong);
		_handler_qishou_hongzhong.reset_status(seat_index);
		_handler_qishou_hongzhong.exe(this);

		return true;
	}

	public int get_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		if (_playerStatus[seat_index].isAbandoned()) { // 弃胡
			return 0;
		}

		// 复制数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int max_ting_count = GameConstants.MAX_ZI;
		if (is_mj_type(GameConstants.GAME_TYPE_MJ_DT_HONG_ZHONG) && getRuleValue(GameConstants.GAME_RULE_HUNAN_DAIFENG) == 1) {
			max_ting_count = GameConstants.MAX_FENG;
		}

		// 如果有红中癞子的玩法，是不需要判断红中的
		for (int i = 0; i < max_ting_count; i++) {
			if (getRuleValue(GameConstants.GAME_RULE_HUNAN_HONGZHONG) == 1 && _logic.is_magic_index(i))
				continue;

			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					GameConstants.HU_CARD_TYPE_ZIMO, seat_index)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}

		if (count == 0) {
			// 没听牌
		} else if (count > 0 && count < max_ting_count) {
			if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HUNAN_HONGZHONG)) {
				// 有胡的牌，红中肯定能胡
				cards[count] = _logic.switch_to_card_data(_logic.get_magic_card_index(0));
				count++;
			}
		} else {
			// 全听
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	@Override
	public boolean handler_requst_message_deal(Player player, int seat_index, RoomRequest room_rq, int type) {
		return true;
	}

	@Override
	public boolean handler_requst_nao_zhuang(Player player, int nao) {
		return false;
	}

	@Override
	public boolean handler_requst_pao_qiang(Player player, int pao, int qiang) {
		return false;
	}

	@Override
	public void onInitTable() {
		_handler_chi_peng = new MJHandlerChiPeng_HuNan_ChangDe();
		_handler_dispath_card = new MJHandlerDispatchCard_HuNan_ChangDe();
		_handler_gang = new MJHandlerGang_HuNan_ChangDe();
		_handler_out_card_operate = new MJHandlerOutCardOperate_HuNan_ChangDe();
		_handler_qishou_hongzhong = new MJHandlerQiShouHongZhong_HuNan_ChangDe();

		if (has_rule(GameConstants.GAME_RULE_HUNAN_HONGZHONG)) { // 红中癞子
			_logic.add_magic_card_index(_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD));
		}
	}

	@Override
	protected boolean on_handler_game_start() {
		reset_init_data();

		progress_banker_select();

		_game_status = GameConstants.GS_MJ_PLAY;

		GRR._banker_player = _cur_banker;
		_current_player = GRR._banker_player;

		banker_count[_current_player]++;

		if (is_mj_type(GameConstants.GAME_TYPE_MJ_DT_HONG_ZHONG) && getRuleValue(GameConstants.GAME_RULE_HUNAN_DAIFENG) == 1) {
			_repertory_card = new int[GameConstants.CARD_COUNT_DAI_FENG_LZ];
			shuffle(_repertory_card, GameConstants.CARD_DATA_DAI_FENG_LZ);
		} else if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HUNAN_HONGZHONG)) { // 112张
			_repertory_card = new int[GameConstants.CARD_COUNT_HZ];
			shuffle(_repertory_card, GameConstants.CARD_DATA_HONG_ZHONG_LAI_ZI);
		} else { // 108张
			_repertory_card = new int[GameConstants.CARD_COUNT_HU_NAN];
			shuffle(_repertory_card, GameConstants.CARD_DATA_WAN_TIAO_TONG);
		}

		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();

		getLocationTip();

		try {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				for (int j = 0; j < GRR._cards_index[i].length; j++) {
					if (GRR._cards_index[i][j] == 4) {
						MongoDBServiceImpl.getInstance().card_log(this.get_players()[i], ECardType.anLong, "", GRR._cards_index[i][j], 0l,
								this.getRoom_id());
					}
				}
			}
		} catch (Exception e) {
			logger.error("card_log", e);
		}
		// 游戏开始时 初始化 未托管
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			istrustee[i] = false;
		}


		return on_game_start();
	}

	@Override
	public boolean on_game_start() {
		// 初始化奖码数
		add_niao = 0;

		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants.MAX_COUNT];
		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			// 只发自己的牌
			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			// 回放数据
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

		// 回放
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

		boolean is_qishou_hu = false;
		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HUNAN_HONGZHONG)) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				// 起手4个红中
				if (GRR._cards_index[i][_logic.get_magic_card_index(0)] == 4) {

					_playerStatus[i].add_action(GameConstants.WIK_ZI_MO);
					_playerStatus[i].add_zi_mo(_logic.switch_to_card_data(_logic.get_magic_card_index(0)), i);
					GRR._chi_hu_rights[i].opr_or(GameConstants.CHR_ZI_MO);
					GRR._chi_hu_rights[i].opr_or(GameConstants.CHR_HUNAN_HZ_QISHOU_HU);

					is_qishou_hu = true;
					exe_qishou_hongzhong(i);

					MongoDBServiceImpl.getInstance().card_log(get_players()[i], ECardType.hongZhong4, "", 0, 0l, getRoom_id());
					break;
				}
			}
		}

		if (is_qishou_hu == false) {
			exe_dispatch_card(_current_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);
		}

		return true;
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {

	}

	// type: 0自摸胡，1抢杠胡，2点炮胡
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, int type) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;

		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int wFanShu = 1;

		if (type == 0 || type == 1) { // 自摸和抢杠都算2分
			wFanShu = 2;
		}

		countCardType(chr, seat_index);

		int lChiHuScore = wFanShu * GameConstants.CELL_SCORE;

		// 算基础分
		if (type == 0) { // 自摸--每人2分
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index)
					continue;

				GRR._lost_fan_shu[i][seat_index] = wFanShu;
			}
		} else if (type == 1) { // 抢杠胡--被抢杠者输6分，抢杠者赢6分
			GRR._lost_fan_shu[provide_index][seat_index] = wFanShu * 3;
		} else { // 点炮--放炮者输一分，接炮者赢一分
			GRR._lost_fan_shu[provide_index][seat_index] = wFanShu;
		}

		// 算奖码分 -- 杠分在开杠时就算进去了
		if (type == 0) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index)
					continue;

				int s = lChiHuScore;

				s += (GRR._count_pick_niao + add_niao) * 2;

				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;
			}
		} else if (type == 1) {
			int s = lChiHuScore; // 抢杠2分，被抢杠者输6分，抢杠者赢6分，并把分加到游戏总分里

			s = s * 3;

			s += (GRR._count_pick_niao + add_niao) * 2;

			GRR._game_score[provide_index] -= s;
			GRR._game_score[seat_index] += s;

			GRR._chi_hu_rights[seat_index].opr_or(GameConstants.CHR_HUNAN_QIANG_GANG_HU); // 抢杠胡
			GRR._chi_hu_rights[provide_index].opr_or(GameConstants.CHR_FANG_PAO); // 放炮
		} else {
			int s = lChiHuScore; // 点炮1分，并把分加到游戏总分里

			s += (GRR._count_pick_niao + add_niao) * 2;

			GRR._game_score[provide_index] -= s;
			GRR._game_score[seat_index] += s;

			GRR._chi_hu_rights[provide_index].opr_or(GameConstants.CHR_FANG_PAO); // 放炮
		}

		GRR._provider[seat_index] = provide_index;

		_status_gang = false;
		_status_gang_hou_pao = false;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);

		return;
	}

	/**
	 * 直接利用已有的抓鸟数据，动画效果可能需要客户端从新做
	 * 
	 * @param seat_index
	 *            当前奖码的玩家，一局只有一个人进行奖码
	 * @param card
	 *            摸几奖几时摸上来的牌
	 * @param show
	 *            是否显示中码效果
	 * @param add_niao
	 *            额外的奖码值，0或1
	 */
	public void set_niao_card(int seat_index, int card, boolean show, int add_niao) {
		for (int i = 0; i < GameConstants.MAX_NIAO_CARD; i++) {
			GRR._cards_data_niao[i] = GameConstants.INVALID_VALUE;
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			GRR._player_niao_count[i] = 0;
			for (int j = 0; j < GameConstants.MAX_NIAO_CARD; j++) {
				GRR._player_niao_cards[i][j] = GameConstants.INVALID_VALUE;
			}
		}

		GRR._show_bird_effect = show;

		GRR._count_niao = get_niao_card_num(card, add_niao);

		if (GRR._count_niao > GameConstants.ZHANIAO_0) {
			int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
			// 从剩余牌堆里顺序取奖码数目的牌
			_logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count, GRR._count_niao, cbCardIndexTemp);
			_logic.switch_to_cards_data(cbCardIndexTemp, GRR._cards_data_niao);
			GRR._left_card_count -= GRR._count_niao;
		}

		for (int i = 0; i < GRR._count_niao; i++) {
			if (_logic.is_magic_card(GRR._cards_data_niao[i])) { // 如果是红中
				GRR._player_niao_cards[seat_index][GRR._player_niao_count[seat_index]] = GRR._cards_data_niao[i];
				GRR._player_niao_count[seat_index]++;
			} else {
				int nValue = _logic.get_card_value(GRR._cards_data_niao[i]);
				int seat = (seat_index + (nValue - 1) % 4) % 4;
				GRR._player_niao_cards[seat][GRR._player_niao_count[seat]] = GRR._cards_data_niao[i];
				GRR._player_niao_count[seat]++;
			}
		}

		// 设置鸟牌显示
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (i == seat_index) {
				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					GRR._player_niao_cards[i][j] = set_ding_niao_valid(GRR._player_niao_cards[i][j], true);
				}
			} else {
				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					GRR._player_niao_cards[i][j] = set_ding_niao_valid(GRR._player_niao_cards[i][j], false);
				}
			}
		}

		// 中鸟个数
		GRR._count_pick_niao = get_pick_niao_count(GRR._cards_data_niao, GRR._count_niao);
	}

	@Override
	public void set_result_describe() {
		int chrTypes;
		long type = 0;
		for (int player = 0; player < getTablePlayerNumber(); player++) {
			StringBuilder gameDesc = new StringBuilder("");

			chrTypes = GRR._chi_hu_rights[player].type_count;

			for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
				type = GRR._chi_hu_rights[player].type_list[typeIndex];

				if (GRR._chi_hu_rights[player].is_valid()) {

					if (type == GameConstants.CHR_ZI_MO) {
						gameDesc.append(" 自摸");

						if (GRR._count_pick_niao > 0) {
							gameDesc.append(" 中码X" + GRR._count_pick_niao);
						}
					}

					if (type == GameConstants.CHR_SHU_FAN) {
						gameDesc.append(" 接炮");

						if (GRR._count_pick_niao > 0) {
							gameDesc.append(" 中码X" + GRR._count_pick_niao);
						}
					}

					if (type == GameConstants.CHR_HUNAN_QIANG_GANG_HU) {
						gameDesc.append(" 抢杠胡");

						if (GRR._count_pick_niao > 0) {
							gameDesc.append(" 中码X" + GRR._count_pick_niao);
						}
					}
					if (add_niao > 0) {
						gameDesc.append(" 无红中奖码");
					}
				} else if (type == GameConstants.CHR_FANG_PAO) {
					gameDesc.append(" 放炮");
				}
			}

			int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0;

			if (GRR != null) {
				for (int tmpPlayer = 0; tmpPlayer < getTablePlayerNumber(); tmpPlayer++) {
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
				gameDesc.append(" 暗杠X" + an_gang);
			}
			if (ming_gang > 0) {
				gameDesc.append(" 明杠X" + ming_gang);
			}
			if (fang_gang > 0) {
				gameDesc.append(" 放杠X" + fang_gang);
			}
			if (jie_gang > 0) {
				gameDesc.append(" 接杠X" + jie_gang);
			}

			GRR._result_des[player] = gameDesc.toString();
		}
	}

	@Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x11, 0x11, 0x12, 0x13, 0x14, 0x15, 0x15, 0x15, 0x22, 0x22, 0x27, 0x27, 0x27 };
		int[] cards_of_player1 = new int[] { 0x11, 0x11, 0x12, 0x13, 0x14, 0x15, 0x15, 0x15, 0x22, 0x22, 0x27, 0x27, 0x27 };
		int[] cards_of_player2 = new int[] { 0x11, 0x11, 0x12, 0x13, 0x14, 0x15, 0x15, 0x15, 0x22, 0x22, 0x27, 0x27, 0x27 };
		int[] cards_of_player3 = new int[] { 0x11, 0x11, 0x12, 0x13, 0x14, 0x15, 0x15, 0x15, 0x22, 0x22, 0x27, 0x27, 0x27 };

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}

		for (int j = 0; j < 13; j++) {
			GRR._cards_index[0][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
			GRR._cards_index[1][_logic.switch_to_card_index(cards_of_player1[j])] += 1;
			GRR._cards_index[2][_logic.switch_to_card_index(cards_of_player2[j])] += 1;
			GRR._cards_index[3][_logic.switch_to_card_index(cards_of_player3[j])] += 1;
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

	/**
	 * 获取奖码数目
	 * 
	 * @param card
	 *            摸几奖几时，摸上来的牌
	 * @param add_niao
	 *            额外的奖码，0或1
	 * @return
	 */
	private int get_niao_card_num(int card, int add_niao) {
		int nNum = GameConstants.ZHANIAO_0;

		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HUNAN_MO_JI_JIANG_JI)) { // 摸几奖几
			if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HUNAN_HONGZHONG)) { // 红中癞子
				if (_logic.is_magic_card(card)) {
					nNum = GameConstants.MAX_NIAO_CARD; // 奖10码
				} else {
					nNum = _logic.get_card_value(card);
				}
			} else { // 正常的万条筒玩法
				nNum = _logic.get_card_value(card);
			}
		} else if (has_rule(GameConstants.GAME_RULE_HUNAN_ZHANIAO2)) { // 奖2码
			nNum = GameConstants.ZHANIAO_2;
		} else if (has_rule(GameConstants.GAME_RULE_HUNAN_ZHANIAO4)) { // 奖4码
			nNum = GameConstants.ZHANIAO_4;
		} else if (has_rule(GameConstants.GAME_RULE_HUNAN_ZHANIAO6)) { // 奖6码
			nNum = GameConstants.ZHANIAO_6;
		}

		nNum += add_niao;

		if (nNum > GRR._left_card_count) {
			nNum = GRR._left_card_count;
		}

		return nNum;
	}

	/**
	 * 获取中码个数
	 * 
	 * @param cards_data
	 *            奖码的牌
	 * @param card_num
	 *            奖码的个数
	 * @return
	 */
	private int get_pick_niao_count(int cards_data[], int card_num) {
		int cbPickNum = 0;

		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HUNAN_HONGZHONG)) { // 红中癞子玩法
			for (int i = 0; i < card_num; i++) {
				if (!_logic.is_valid_card(cards_data[i])) // 如果有非法的牌
					return 0;

				if (_logic.is_magic_card(cards_data[i])) { // 如果是红中也算中码
					cbPickNum++;
				} else {
					int nValue = _logic.get_card_value(cards_data[i]);

					if (nValue == 1 || nValue == 5 || nValue == 9) {
						cbPickNum++;
					}
				}
			}
		} else { // 普通万条筒玩法
			for (int i = 0; i < card_num; i++) {
				if (!_logic.is_valid_card(cards_data[i])) // 如果有非法的牌
					return 0;

				int nValue = _logic.get_card_value(cards_data[i]);

				if (nValue == 1 || nValue == 5 || nValue == 9) {
					cbPickNum++;
				}
			}
		}

		return cbPickNum;
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		// TODO Auto-generated method stub
		return false;
	}
}
