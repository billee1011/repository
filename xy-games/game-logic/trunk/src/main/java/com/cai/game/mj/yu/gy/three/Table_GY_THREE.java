package com.cai.game.mj.yu.gy.three;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.Constants_HuangZhou;
import com.cai.common.constant.game.GameConstants_GY;
import com.cai.common.constant.game.GameConstants_HZLZG;
import com.cai.common.constant.game.GameConstants_HanShouWang;
import com.cai.common.define.ECardType;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.future.GameSchedule;
import com.cai.game.mj.yu.gy.HandlerChiPeng_GY;
import com.cai.game.mj.yu.gy.HandlerDispatchCard_GY;
import com.cai.game.mj.yu.gy.HandlerGang_GY;
import com.cai.game.mj.yu.gy.HandlerOutCardOperate_GY;
import com.cai.game.mj.yu.gy.HandlerSelectMagic_GY;
import com.cai.game.mj.yu.gy.MJHandlerFinish_GY;
import com.cai.game.mj.yu.gy.MJHandlerOutCardBaoTing_GY;
import com.cai.game.mj.yu.gy.MJHandlerYingBao;
import com.cai.game.mj.yu.gy.Table_GY;
import com.cai.service.MongoDBServiceImpl;
import com.cai.util.Tuple;
import com.google.common.collect.Lists;

import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;

/**
 * 贵阳抓鸡
 * 
 * @author yu
 *
 */
public class Table_GY_THREE extends Table_GY {

	private static final long serialVersionUID = -6276068233217765560L;

	// public int chong_feng_ji_seat_yj; // 幺鸡冲锋鸡
	//
	// public int chong_feng_ji_seat_bt; // 八筒冲锋鸡
	//
	// public int[][] out_ji_pai; // 每个人打出的鸡牌数量
	// public int[] out_ji_pai_count; // 每个人打出的鸡牌数量
	//
	// public int[] _ji_card_index; // 指定鸡牌
	//
	// public int _ji_card_count; // 指定 鸡牌数量
	//
	// public Tuple<Integer, Integer>[] responsibility_ji; // 责任鸡
	//
	// public Tuple<Integer, Integer>[] show_responsibility_ji; // 责任鸡
	//
	// public boolean[] jin_ji; // 本局是否有金鸡
	//
	// public boolean[] player_mo_first;
	//
	// public MJHandlerOutCardBaoTing_GY _handler_out_card_bao_ting;
	//
	// public HandlerSelectMagic_GY handler_select_magic;
	//
	// public boolean[] shao; // 烧鸡烧杠
	//
	// public int[] player_all_ji_card;
	//
	// public boolean show_continue_banker;
	//
	// public int[][] player_11_detail;
	//
	// public int[][] player_GangScore_type;
	//
	// public float[] hu_type_socre;
	//
	// public int[] player_ji_score;
	//
	// public int[] player_duan;
	//
	// MJHandlerFinish_GY handler_finish;
	//
	// public GameRoundRecord game_end_GRR;
	//
	// public int[] zi_da;
	//
	// float hu_base_fen;
	//
	// public RoomResponse.Builder roomResponse;
	//
	// public int old_banker;

	public Table_GY_THREE() {
		super();
		_ji_card_count = 0;
		_ji_card_index = new int[GameConstants.MAX_COUNT];
		this.setPlay_card_time(10);
	}

	public void add_ji_card_index(int index) {
		_ji_card_index[_ji_card_count] = index;
		_ji_card_count++;
	}

	public boolean is_ji_card(int card) {
		for (int i = 0; i < _ji_card_count; i++) {
			if (_ji_card_index[i] == _logic.switch_to_card_index(card)) {
				return true;
			}
		}
		return false;
	}

	public boolean is_ji_index(int index) {
		for (int i = 0; i < _ji_card_count; i++) {
			if (_ji_card_index[i] == index) {
				return true;
			}
		}
		return false;
	}

	public void clean_ji_cards() {
		_ji_card_count = 0;
	}

	@Override
	public int get_real_card(int card) {
		if (card > GameConstants_GY.CARD_ESPECIAL_TYPE_TING && card < GameConstants_GY.CARD_ESPECIAL_TYPE_LIANG_ZHANG) {
			card -= GameConstants_GY.CARD_ESPECIAL_TYPE_TING;
			return card;
		}
		if (card > GameConstants_GY.CARD_ESPECIAL_TYPE_LIANG_ZHANG) {
			card -= GameConstants_GY.CARD_ESPECIAL_TYPE_LIANG_ZHANG;
			return card;
		}
		return card;
	}

	@Override
	protected void onInitTable() {
		_handler_chi_peng = new HandlerChiPeng_GY();
		_handler_dispath_card = new HandlerDispatchCard_GY();
		_handler_gang = new HandlerGang_GY();
		_handler_out_card_operate = new HandlerOutCardOperate_GY();
		_handler_out_card_bao_ting = new MJHandlerOutCardBaoTing_GY();
		handler_select_magic = new HandlerSelectMagic_GY();
		handler_finish = new MJHandlerFinish_GY();
		handler_ying_bao = new MJHandlerYingBao();

		end_hu_score = new int[GameConstants.GAME_PLAYER];
		end_ji_score = new int[GameConstants.GAME_PLAYER];
		end_dou_score = new int[GameConstants.GAME_PLAYER];
	}

	@Override
	public boolean exe_out_card_bao_ting(int seat_index, int card, int type) {
		// 出牌
		this.set_handler(this._handler_out_card_bao_ting);
		this._handler_out_card_bao_ting.reset_status(seat_index, card, type);
		this._handler.exe(this);

		return true;
	}

	public boolean exe_select_magic() {
		this.set_handler(this.handler_select_magic);
		handler_select_magic.reset_status(_cur_banker);
		_handler.exe(this);

		return true;
	}

	/**
	 * 
	 * @return
	 */
	@Override
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
			if (player_duan[i] != -1 && _logic.get_card_color(card) == player_duan[i])
				continue;

			// 可以胡的情况 判断
			if (playerStatus.is_chi_hu_round()) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						GameConstants_GY.HU_CARD_TYPE_QIANG_GANG, i);

				if (player_duan[i] != -1) {
					for (int c = 0; c < GameConstants.MAX_INDEX; c++) {
						if (GRR._cards_index[i][c] > 0 && _logic.get_card_color(_logic.switch_to_card_data(c)) == player_duan[i]) {
							action = GameConstants_HZLZG.WIK_NULL;
							break;
						}
					}
				}
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

	public boolean estimate_player_out_card_respond(int seat_index, int card, int type) {
		boolean bAroseAction = false;

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		PlayerStatus playerStatus = null;

		int action = GameConstants_HanShouWang.WIK_NULL;

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (seat_index == i) {
				continue;
			}
			// if (_playerStatus[i].is_bao_ting())
			// continue;
			if (player_duan[i] != -1 && _logic.get_card_color(card) == player_duan[i])
				continue;

			playerStatus = _playerStatus[i];

			boolean can_peng = true;
			int[] tmp_cards_data = _playerStatus[i].get_cards_abandoned_peng();
			for (int x = 0; x < GameConstants_GY.MAX_ABANDONED_CARDS_COUNT; x++) {
				if (tmp_cards_data[x] == card) {
					can_peng = false;
					break;
				}
			}
			if (can_peng && GRR._left_card_count > 0 && !_playerStatus[i].is_bao_ting()) {
				action = _logic.check_peng(GRR._cards_index[i], card);
				// int cards[] = new int[GameConstants.MAX_COUNT];
				// int hand_card_count =
				// _logic.switch_to_cards_data(GRR._cards_index[i], cards);
				if (action != 0) {
					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);
					bAroseAction = true;
				}

				if (GRR._left_card_count > 0) {
					action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
					if (action != 0) {
						playerStatus.add_action(GameConstants_GY.WIK_GANG);
						playerStatus.add_gang(card, seat_index, 1); // 加上杠
						bAroseAction = true;
					}
				}
			}

			if (_playerStatus[i].is_chi_hu_round()) {
				boolean can_hu_this_card = true;
				int[] tmp_cards_data_hu = _playerStatus[i].get_cards_abandoned_hu();
				for (int x = 0; x < GameConstants_GY.MAX_ABANDONED_CARDS_COUNT; x++) {
					if (tmp_cards_data_hu[x] == card) {
						can_hu_this_card = false;
						break;
					}
				}
				if (can_hu_this_card) {
					ChiHuRight chr = GRR._chi_hu_rights[i];
					chr.set_empty();
					int cbWeaveCount = GRR._weave_count[i];

					int card_type = GameConstants_GY.HU_CARD_TYPE_JIE_PAO;
					if (type == GameConstants.GANG_TYPE_AN_GANG || type == GameConstants.GANG_TYPE_ADD_GANG
							|| type == GameConstants.GANG_TYPE_JIE_GANG) {
						card_type = GameConstants_GY.HU_CARD_TYPE_RE_PAO;
					} else
					// 此处偷个懒，若出牌得人已报听牌，其他人没有通行证的情况下也是可以胡的
					if (_playerStatus[seat_index].is_bao_ting() || _playerStatus[i].is_bao_ting())
						card_type = GameConstants_GY.HU_CARD_TYPE_SHA_BAO;

					action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr, card_type, i);

					if (player_duan[i] != -1) {
						for (int c = 0; c < GameConstants.MAX_INDEX; c++) {
							if (GRR._cards_index[i][c] > 0 && _logic.get_card_color(_logic.switch_to_card_data(c)) == player_duan[i]) {
								action = GameConstants_HZLZG.WIK_NULL;
								break;
							}
						}
					}
					if (action != 0) {
						_playerStatus[i].add_action(GameConstants_GY.WIK_CHI_HU);
						_playerStatus[i].add_chi_hu(card, seat_index);
						bAroseAction = true;
					}
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

	@Override
	public int getTablePlayerNumber() {
		return 3;
	}

	/**
	 * 结束调度
	 * 
	 * @return
	 */
	public boolean exe_finish(int reason) {
		this._end_reason = reason;
		if (_end_reason == GameConstants.Game_End_NORMAL || _end_reason == GameConstants.Game_End_DRAW
				|| _end_reason == GameConstants.Game_End_ROUND_OVER) {
			cost_dou = 0;
		}

		this.set_handler(this.handler_finish);
		this.handler_finish.exe(this);

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(int seat_index) {
		// && GameConstants.GS_MJ_WAIT != _game_statu
		if ((GameConstants.GS_MJ_FREE != _game_status) && this.get_players()[seat_index] != null) {
			// this.send_play_data(seat_index);

			if (this._handler != null)
				this._handler.handler_player_be_in_room(this, seat_index);

		}
		GameSchedule.put(new Runnable() {
			@Override
			public void run() {
				fixBugTemp(seat_index);
			}
		}, 1, TimeUnit.SECONDS);

		if (is_sys())
			return true;
		// return handler_player_ready(seat_index, false);
		return true;
	}

	// 游戏开始
	@Override
	protected boolean on_handler_game_start() {
		if (_cur_round == 2) {
			// real_kou_dou();// 记录真实扣豆
		}
		zi_da = new int[getTablePlayerNumber()];
		reset_init_data();
		Arrays.fill(_player_result.ziba, 0);

		// 庄家选择
		progress_banker_select();

		_game_status = GameConstants.GS_MJ_PLAY;

		// 信阳麻将
		GRR._banker_player = _cur_banker;
		_current_player = GRR._banker_player;
		banker_count[_current_player]++;

		if (has_rule(GameConstants_GY.GAME_RULE_ER_FANG_PAI)) {
			_repertory_card = new int[GameConstants_GY.CARD_DATA_TIAO_TONG.length];
			shuffle(_repertory_card, GameConstants_GY.CARD_DATA_TIAO_TONG);
		} else {
			_repertory_card = new int[GameConstants_GY.CARD_COUNT_HU_NAN];
			shuffle(_repertory_card, GameConstants_GY.CARD_DATA_WAN_TIAO_TONG);
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
	protected boolean on_game_start() {
		_logic.clean_magic_cards();
		this.clean_ji_cards();
		this.add_ji_card_index(_logic.switch_to_card_index(GameConstants_GY.YJ_CARD));
		if (has_rule(GameConstants_GY.GAME_RULE_WU_GU_JI))
			this.add_ji_card_index(_logic.switch_to_card_index(GameConstants_GY.BA_TONG_CARD));

		_game_status = GameConstants_GY.GS_MJ_PLAY;

		chong_feng_ji_seat_bt = -1;
		chong_feng_ji_seat_yj = -1;
		out_ji_pai = new int[getTablePlayerNumber()][];

		out_ji_pai_count = new int[getTablePlayerNumber()];
		jin_ji = new boolean[2]; // 只有两个默认的鸡产生金鸡
		responsibility_ji = new Tuple[2];
		show_responsibility_ji = new Tuple[2];
		player_mo_first = new boolean[getTablePlayerNumber()];
		shao = new boolean[getTablePlayerNumber()];
		player_all_ji_card = new int[getTablePlayerNumber()];
		player_11_detail = new int[getTablePlayerNumber()][11];
		hu_type_socre = new float[getTablePlayerNumber()];
		player_ji_score = new int[getTablePlayerNumber()];
		player_duan = new int[getTablePlayerNumber()];
		player_GangScore_type = new int[getTablePlayerNumber()][4];
		shang_xia_ji = new int[3];
		player_show_desc_11 = new int[getTablePlayerNumber()][11];
		hu_base_fen = 0;
		player_ying_bao = new boolean[getTablePlayerNumber()];
		player_ruan_bao = new boolean[getTablePlayerNumber()];
		fan_pao_zhuan_fen = 0;
		for (int i = 0; i < 2; i++) {
			responsibility_ji[i] = new Tuple<Integer, Integer>(-1, -1);
			show_responsibility_ji[i] = new Tuple<Integer, Integer>(-1, -1);
		}
		for (int player = 0; player < getTablePlayerNumber(); player++) {
			GRR._result_des[player] = "";
			out_ji_pai[player] = new int[28];
			player_mo_first[player] = true;
			player_duan[player] = -1;
		}

		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants_HanShouWang.MAX_COUNT];

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants_HanShouWang.MAX_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			load_room_info_data(roomResponse);
			load_common_status(roomResponse);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setCurrentPlayer(_current_player == GameConstants_HanShouWang.INVALID_SEAT ? _resume_player : _current_player);
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

			for (int j = 0; j < GameConstants_HanShouWang.MAX_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}
		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(GRR._left_card_count);
		GRR.add_room_response(roomResponse);

		// 检测听牌
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i]._hu_card_count = get_ting_card(_playerStatus[i]._hu_cards, _playerStatus[i]._hu_out_cards_fan[0], GRR._cards_index[i],
					GRR._weave_items[i], GRR._weave_count[i], i);
			if (_playerStatus[i]._hu_card_count > 0) {
				operate_chi_hu_cards(i, _playerStatus[i]._hu_card_count, _playerStatus[i]._hu_cards);
			}
		}

		old_banker = _current_player;
		exe_dispatch_card(_current_player, GameConstants_GY.DispatchCard_Type_Tian_Hu, GameConstants_GY.DELAY_SEND_CARD_DELAY);

		return true;
	}

	public void huan_zhuan() {
		int[] jiao_pai = new int[getTablePlayerNumber()];
		for (int i = 0; i < getTablePlayerNumber(); i++)
			if (_playerStatus[i]._hu_card_count > 0)
				jiao_pai[i] = 1;

		int flag = jiao_pai[0];
		boolean equally = true;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (flag != jiao_pai[i]) {
				equally = false;
				break;
			}
		}

		if (equally)
			return;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			ChiHuRight chr = huan_zhuang_cha_hu(GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i], i);
			if (chr == null)
				continue;

			process_cha_hu_score(i, chr);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public ChiHuRight huan_zhuang_cha_hu(int[] cards_index, WeaveItem[] weaveItems, int cbWeaveCount, int seat_index) {
		int cbCardIndexTemp[] = new int[GameConstants_GY.MAX_INDEX];
		for (int i = 0; i < GameConstants_GY.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		int cbCurrentCard;
		int max_ting_count = GameConstants_GY.MAX_ZI;

		List<com.cai.common.util.Tuple> list = Lists.newArrayList();
		for (int i = 0; i < max_ting_count; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			ChiHuRight chr = new ChiHuRight();
			if (GameConstants_GY.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItems, cbWeaveCount, cbCurrentCard, chr,
					Constants_HuangZhou.CHR_ZI_MO, seat_index)) {
				list.add(new com.cai.common.util.Tuple<>(cbCurrentCard, chr, 0));
			}
		}

		if (list.isEmpty())
			return null;

		// 设置各胡型权重
		list.forEach((tuple) -> {
			ChiHuRight chr = (ChiHuRight) tuple.getCenter();
			if (!chr.opr_and(GameConstants_GY.CHR_QING_LONG_BEI).is_empty()) {
				tuple.setRight(GameConstants_GY.CHR_QING_LONG_BEI);
				return;
			}
			if (!chr.opr_and(GameConstants_GY.CHR_LONG_QI_DUI).is_empty()) {
				tuple.setRight(GameConstants_GY.CHR_LONG_QI_DUI);
				return;
			}
			if (!chr.opr_and(GameConstants_GY.CHR_QING_QI_DUI).is_empty()) {
				tuple.setRight(GameConstants_GY.CHR_QING_QI_DUI);
				return;
			}
			if (!chr.opr_and(GameConstants_GY.CHR_SHUANG_QING).is_empty()) {
				tuple.setRight(GameConstants_GY.CHR_SHUANG_QING);
				return;
			}
			if (!chr.opr_and(GameConstants_GY.CHR_QING_DA_DUI).is_empty()) {
				tuple.setRight(GameConstants_GY.CHR_QING_DA_DUI);
				return;
			}
			if (!chr.opr_and(GameConstants_GY.CHR_QING_YI_SE).is_empty()) {
				tuple.setRight(GameConstants_GY.CHR_QING_YI_SE);
				return;
			}
			if (!chr.opr_and(GameConstants_GY.CHR_XIAO_QI_DUI).is_empty()) {
				tuple.setRight(GameConstants_GY.CHR_XIAO_QI_DUI);
				return;
			}
			if (!chr.opr_and(GameConstants_GY.CHR_DAN_DIAO).is_empty()) {
				tuple.setRight(GameConstants_GY.CHR_QING_YI_SE);
				return;
			} else if (!chr.opr_and(GameConstants_GY.CHR_DA_DUI_ZI).is_empty()) {
				tuple.setRight(GameConstants_GY.CHR_DA_DUI_ZI);
				return;
			}

			tuple.setRight(1); // 权重最低
		});

		Collections.sort(list, new Comparator<com.cai.common.util.Tuple>() {
			@Override
			public int compare(com.cai.common.util.Tuple o1, com.cai.common.util.Tuple o2) {
				int r1 = (Integer) o1.getRight();
				int r2 = (Integer) o2.getRight();
				if (r1 == r2)
					return 0;

				return r1 > r2 ? -1 : 2;
			}
		});

		return (ChiHuRight) (list.get(0).getCenter());
	}

	public void process_cha_hu_score(int seat_index, ChiHuRight chr) {
		float lChiHuScore = get_hu_fen(chr);

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (i == seat_index) {
				continue;
			}
			if (_playerStatus[i]._hu_card_count > 0)
				continue;

			float s = lChiHuScore + (continue_banker_count > 0 ? continue_banker_count + 1 : 0);

			GRR._game_score[i] -= s;
			hu_type_socre[i] -= s;
			GRR._game_score[seat_index] += s;
			hu_type_socre[seat_index] += s;
		}
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int wFanShu = 2;// 番数
		countCardType(chr, seat_index);

		/////////////////////////////////////////////// 算分//////////////////////////
		// 统计
		if (zimo) {
			// 自摸
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				GRR._lost_fan_shu[i][seat_index] = wFanShu;
			}
			show_continue_banker = true;
		} else {// 点炮
			GRR._lost_fan_shu[provide_index][seat_index] = wFanShu;
			show_continue_banker = false;
		}

		float lChiHuScore = get_hu_fen(chr);
		////////////////////////////////////////////////////// 自摸 算分
		if (zimo) {
			chr.opr_or(GameConstants.CHR_ZI_MO);
			hu_base_fen = lChiHuScore;

			// 天听软报额外收取10分
			if (_playerStatus[seat_index].is_bao_ting()) {
				lChiHuScore += get_hu_fen(chr) == 1 ? 9 : 10;
			}

			// 自摸+1 分,排除天胡
			if (chr.opr_and(GameConstants_GY.CHR_TIAN_HU).is_empty()) {
				lChiHuScore++;
			}
			// 杠开多加一分
			if (!chr.opr_and(GameConstants_GY.CHR_GNAG_KAI).is_empty()) {
				lChiHuScore += 1;
			}

			// 连庄玩法 加连庄数
			if (old_banker == _cur_banker) {
				lChiHuScore += continue_banker_count;
				if (continue_banker_count > 0) {
					GRR._result_des[seat_index] = "连庄 +" + continue_banker_count + GRR._result_des[seat_index];
				}
				continue_banker_count++;
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}

				float s = lChiHuScore;

				// 杀报不和平胡叠加，所以加9
				if (_playerStatus[i].is_bao_ting()) {
					s += get_hu_fen(chr) == 1 ? 9 : 10;
				}
				// 添加动作)
				GRR._game_score[i] -= s;
				hu_type_socre[i] -= s;
				GRR._game_score[seat_index] += s;
				hu_type_socre[seat_index] += s;

				// 连庄玩法 加连庄数
				if (old_banker != _cur_banker && i == old_banker) {
					GRR._game_score[i] -= continue_banker_count;
					hu_type_socre[i] -= continue_banker_count;
					GRR._game_score[seat_index] += continue_banker_count;
					hu_type_socre[seat_index] += continue_banker_count;
					if (continue_banker_count > 0) {
						GRR._result_des[i] = "连庄  -" + continue_banker_count + GRR._result_des[i];
						continue_banker_count = 1;
					}
				}
			}
		}
		////////////////////////////////////////////////////// 点炮 算分
		else {
			chr.opr_or(GameConstants.CHR_SHU_FAN);
			GRR._chi_hu_rights[provide_index].opr_or(GameConstants.CHR_FANG_PAO);

			int add_fen = has_rule(GameConstants_GY.GAME_RULE_TONG_3) ? 3 : 1;
			if (lChiHuScore > 0 && add_fen == 1)
				add_fen = 0;

			float s = lChiHuScore + add_fen;
			hu_base_fen = s;

			// 天听软报额外收取10分
			boolean flag = false;
			if (_playerStatus[seat_index].is_bao_ting()) {
				s += get_hu_fen(chr) == 1 ? 9 : 10;
				flag = true;
			} else
			// 地胡
			if (!GRR._chi_hu_rights[seat_index].opr_and(GameConstants_GY.CHR_DI_HU).is_empty()) {
				s += get_hu_fen(chr) == 1 ? 9 : 10;
				flag = true;
			}

			// 杀报不和平胡叠加，所以加9
			if (_playerStatus[provide_index].is_bao_ting()) {
				s += get_hu_fen(chr) == 1 && !flag ? 9 : 10;
			}

			// 连庄玩法 加连庄数
			if (seat_index == old_banker) {
				s += continue_banker_count;
				if (continue_banker_count > 0)
					GRR._result_des[seat_index] = "连庄  +" + continue_banker_count + GRR._result_des[seat_index];
				continue_banker_count++;
			}

			if (provide_index == old_banker) {
				s += continue_banker_count;
			}

			// 地胡 硬报加20分
			if (!GRR._chi_hu_rights[seat_index].opr_and(GameConstants_GY.CHR_DI_HU).is_empty())
				s += 20;

			// 热炮通三多出3分,其他热炮多加一分
			if (!GRR._chi_hu_rights[seat_index].opr_and(GameConstants_GY.CHR_RE_PAO).is_empty())
				s += 1;

			// 抢杠胡多出9分
			if (!GRR._chi_hu_rights[seat_index].opr_and(GameConstants_GY.CHR_QING_GANG_HU).is_empty()) {
				s += 1;
			}

			// 胡牌分
			GRR._game_score[provide_index] -= s;
			hu_type_socre[provide_index] -= s;
			GRR._game_score[seat_index] += s;
			hu_type_socre[seat_index] += s;
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (i == _cur_banker) {
				_player_result.qiang[_cur_banker] = continue_banker_count;
			} else {
				_player_result.qiang[i] = 0;
			}
		}

		// 设置变量
		GRR._provider[seat_index] = provide_index;
		_status_gang = false;
		_status_gang_hou_pao = false;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);
	}

	private int get_hu_fen(ChiHuRight chr) {
		int fen = 0;

		if (!chr.opr_and(GameConstants_GY.CHR_TIAN_HU).is_empty()) {
			fen += 10;
		}
		if (!chr.opr_and(GameConstants_GY.CHR_QING_LONG_BEI).is_empty()) {
			fen += 30;
			return fen;
		}
		if (!chr.opr_and(GameConstants_GY.CHR_LONG_QI_DUI).is_empty()) {
			fen += 20;
			return fen;
		}
		if (!chr.opr_and(GameConstants_GY.CHR_QING_QI_DUI).is_empty()) {
			fen += 20;
			return fen;
		}
		if (!chr.opr_and(GameConstants_GY.CHR_SHUANG_QING).is_empty()) {
			fen += 20;
			return fen;
		}
		if (!chr.opr_and(GameConstants_GY.CHR_QING_DA_DUI).is_empty()) {
			fen += 15;
			return fen;
		}
		if (!chr.opr_and(GameConstants_GY.CHR_QING_YI_SE).is_empty()) {
			fen += 10;
			return fen;
		}
		if (!chr.opr_and(GameConstants_GY.CHR_XIAO_QI_DUI).is_empty()) {
			fen += 10;
			return fen;
		}
		if (!chr.opr_and(GameConstants_GY.CHR_DAN_DIAO).is_empty()) {
			fen += 10;
			return fen;
		} else if (!chr.opr_and(GameConstants_GY.CHR_DA_DUI_ZI).is_empty()) {
			fen += 5;
			return fen;
		}
		return fen == 0 ? 1 : fen;
	}

	/**
	 * 初始化plyaer_11_detail 的第十一位
	 */
	private void init_11_player_detail() {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			ChiHuRight chr = GRR._chi_hu_rights[i];

			if (GRR._chi_hu_rights[i].is_valid()) {
				if (!chr.opr_and(GameConstants_GY.CHR_ZI_MO).is_empty())
					player_11_detail[i][10] = 1;
				if (!chr.opr_and(GameConstants_GY.CHR_SHU_FAN).is_empty())
					player_11_detail[i][10] = 2;
				if (!chr.opr_and(GameConstants_GY.CHR_QING_GANG_HU).is_empty())
					player_11_detail[i][10] = 3;
				if (!chr.opr_and(GameConstants_GY.CHR_GNAG_KAI).is_empty())
					player_11_detail[i][10] = 4;
				if (!chr.opr_and(GameConstants_GY.CHR_RE_PAO).is_empty())
					player_11_detail[i][10] = 6;
			} else {
				if (!chr.opr_and(GameConstants_GY.CHR_FANG_PAO).is_empty())
					player_11_detail[i][10] = 5;
			}
		}
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

		// TODO 向其他人发送组合牌数据的时候，暗杠不能发center_card数据
		if (weave_count > 0) {
			for (int j = 0; j < weave_count; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(weaveitems[j].provide_player);
				weaveItem_item.setPublicCard(weaveitems[j].public_card);
				weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
				if (weaveitems[j].weave_kind == GameConstants.WIK_GANG && weaveitems[j].public_card == 0) {
					weaveItem_item.setCenterCard(0);
				} else {
					weaveItem_item.setCenterCard(weaveitems[j].center_card);
				}
				roomResponse.addWeaveItems(weaveItem_item);
			}
		}
		send_response_to_other(seat_index, roomResponse);

		// TODO 向自己发送手牌数据，自己的数据里，暗杠是能看到的
		for (int j = 0; j < card_count; j++) {
			roomResponse.addCardData(cards[j]);
		}
		if (weave_count > 0) {
			roomResponse.clearWeaveItems();
			for (int j = 0; j < weave_count; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(weaveitems[j].provide_player);
				weaveItem_item.setPublicCard(weaveitems[j].public_card);
				weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
				weaveItem_item.setCenterCard(weaveitems[j].center_card);
				roomResponse.addWeaveItems(weaveItem_item);
			}
		}

		int ting_count = _playerStatus[seat_index]._hu_out_card_count;

		roomResponse.setOutCardCount(ting_count);

		for (int i = 0; i < ting_count; i++) {
			int ting_card_cout = _playerStatus[seat_index]._hu_out_card_ting_count[i];
			roomResponse.addOutCardTingCount(ting_card_cout);

			int out_card = _playerStatus[seat_index]._hu_out_card_ting[i];

			roomResponse.addOutCardTing(out_card + GameConstants.CARD_ESPECIAL_TYPE_TING);

			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < ting_card_cout; j++) {
				int card = _playerStatus[seat_index]._hu_out_cards[i][j];
				if (_logic.is_magic_card(card))
					card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				int_array.addItem(card);
				roomResponse.addDouliuzi(_playerStatus[seat_index]._hu_out_cards_fan[i][j]);
			}
			roomResponse.addOutCardTingCards(int_array);
		}

		send_response_to_player(seat_index, roomResponse);

		GRR.add_room_response(roomResponse);

		return true;
	}

	@Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x11, 0x11, 0x02, 0x02, 0x03, 0x03, 0x04, 0x04, 0x05, 0x05, 0x06, 0x06, 0x07 };
		int[] cards_of_player1 = new int[] { 0x11, 0x12, 0x13, 0x02, 0x02, 0x02, 0x04, 0x04, 0x04, 0x06, 0x06, 0x07, 0x07 };
		int[] cards_of_player2 = new int[] { 0x11, 0x11, 0x02, 0x02, 0x03, 0x03, 0x04, 0x04, 0x05, 0x05, 0x06, 0x06, 0x07 };
		int[] cards_of_player3 = new int[] { 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x04, 0x04, 0x04, 0x06, 0x06, 0x07, 0x07 };

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}

		for (int j = 0; j < 13; j++) {
			if (this.getTablePlayerNumber() == 4) {
				GRR._cards_index[0][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[1][_logic.switch_to_card_index(cards_of_player1[j])] += 1;
				GRR._cards_index[2][_logic.switch_to_card_index(cards_of_player2[j])] += 1;
				GRR._cards_index[3][_logic.switch_to_card_index(cards_of_player3[j])] += 1;
			} else if (this.getTablePlayerNumber() == 3) {
				GRR._cards_index[0][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[1][_logic.switch_to_card_index(cards_of_player1[j])] += 1;
				GRR._cards_index[2][_logic.switch_to_card_index(cards_of_player2[j])] += 1;
			} else if (this.getTablePlayerNumber() == 2) {
				GRR._cards_index[0][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[1][_logic.switch_to_card_index(cards_of_player1[j])] += 1;
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

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		// TODO Auto-generated method stub
		return false;
	}

}
