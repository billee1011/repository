package com.cai.game.mj.handler.zyzj;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.Constants_HuangZhou;
import com.cai.common.constant.game.GameConstants_HZLZG;
import com.cai.common.constant.game.GameConstants_HanShouWang;
import com.cai.common.constant.game.GameConstants_ZYZJ;
import com.cai.common.define.ECardType;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GameRoundRecord;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.GameDescUtil;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.TimeUtil;
import com.cai.future.GameSchedule;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJType;
import com.cai.game.mj.ThreeDimension;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;
import com.cai.util.Tuple;
import com.google.common.collect.Lists;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

/**
 * 贵阳抓鸡
 * 
 * @author yu
 *
 */
public class Table_ZYZJ extends AbstractMJTable {

	private static final long serialVersionUID = 3829093424779167749L;

	public int chong_feng_ji_seat_yj; // 幺鸡冲锋鸡

	public int chong_feng_ji_seat_bt; // 八筒冲锋鸡

	public int[][] out_ji_pai; // 每个人打出的鸡牌数量
	public int[] out_ji_pai_count; // 每个人打出的鸡牌数量

	public int[] _ji_card_index; // 指定鸡牌

	public int _ji_card_count; // 指定 鸡牌数量

	public Tuple<Integer, Integer>[] responsibility_ji; // 责任鸡

	public Tuple<Integer, Integer>[] show_responsibility_ji; // 责任鸡

	public boolean[] jin_ji; // 本局是否有金鸡[0]金鸡

	public boolean[] player_mo_first;

	public MJHandlerOutCardBaoTing_ZYZJ _handler_out_card_bao_ting;

	public HandlerSelectMagic_ZYZJ handler_select_magic;

	public boolean[] shao; // 烧鸡烧杠

	public boolean[] shao_gang; // 烧鸡烧杠

	public int[] player_all_ji_card;

	public boolean show_continue_banker;

	public int[][] player_11_detail;
	public int[][] player_show_desc_10; // 0 : 冲金鸡，1 ：冲幺鸡，2 ：金幺鸡，3 ：幺鸡，4
										// ：冲锋乌骨鸡，5 ：金乌骨鸡，6 ：乌骨鸡，7 ：本鸡
	// 8 ：翻鸡，9
	// ：摇摆鸡，10：星期鸡，11：闷豆，12：点豆，13：转弯豆，14：连庄，15：自摸，16：估买，17：原缺，18：报听，19：杀报，20：胡牌分，
	// 21：天胡，22：地胡，23:杠上花，24：抢杠胡，25：查缺，26：责任鸡
	public int[][] player_GangScore_type;

	public float[] hu_type_socre;

	public int[] player_ji_score;

	public int[] player_duan;

	public MJHandlerFinish_ZYZJ handler_finish;

	public HandlerSwitchCard_ZYZJ _handler_switch_card;

	public HandlerGuMai_ZYZJ _handler_gumai;

	public GameRoundRecord game_end_GRR;

	public int[] zi_da;

	public float hu_base_fen;

	public RoomResponse.Builder roomResponse;

	public int old_banker;

	public int[] shang_xia_ji;

	public int[] ze_ren_ji;

	public boolean[] had_switch_card;

	public final int SWITCH_CARD_COUNT = 3;

	public int[][] switch_card_index;

	public int[] ding_que_pai_se;

	public int[] yuan_que_player;

	public int ben_ji_card;

	public int fan_ji_card;

	public int[] player_duan_score;

	public int[] player_gumai_score;

	public int continue_banker_count = 0; // 连庄次数

	public int hutype;
	public int fangpaoplayer[];
	public int hupaiplayer[];

	public Table_ZYZJ() {
		super(MJType.GAME_TYPE_GUI_YANG);
		_ji_card_count = 0;
		_ji_card_index = new int[GameConstants.MAX_COUNT];
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
		if (card > GameConstants_ZYZJ.CARD_ESPECIAL_TYPE_LAI_ZI && card < GameConstants_ZYZJ.CARD_ESPECIAL_TYPE_TING) {
			card -= GameConstants_ZYZJ.CARD_ESPECIAL_TYPE_LAI_ZI;
			return card;
		}
		if (card > GameConstants_ZYZJ.CARD_ESPECIAL_TYPE_TING
				&& card < GameConstants_ZYZJ.CARD_ESPECIAL_TYPE_TING + GameConstants_ZYZJ.CARD_ESPECIAL_TYPE_LAI_ZI) {
			card -= GameConstants_ZYZJ.CARD_ESPECIAL_TYPE_TING;
			return card;
		}
		if (card > GameConstants_ZYZJ.CARD_ESPECIAL_TYPE_TING + GameConstants_ZYZJ.CARD_ESPECIAL_TYPE_LAI_ZI
				&& card < GameConstants_ZYZJ.CARD_ESPECIAL_TYPE_LIANG_ZHANG) {
			card -= GameConstants_ZYZJ.CARD_ESPECIAL_TYPE_TING + GameConstants_ZYZJ.CARD_ESPECIAL_TYPE_LAI_ZI;
			return card;
		}
		if (card > GameConstants_ZYZJ.CARD_ESPECIAL_TYPE_LIANG_ZHANG) {
			card -= GameConstants_ZYZJ.CARD_ESPECIAL_TYPE_LIANG_ZHANG;
			return card;
		}
		return card;
	}

	@Override
	protected void onInitTable() {
		_handler_chi_peng = new HandlerChiPeng_ZYZJ();
		_handler_dispath_card = new HandlerDispatchCard_ZYZJ();
		_handler_gang = new HandlerGang_ZYZJ();
		_handler_out_card_operate = new HandlerOutCardOperate_ZYZJ();
		_handler_out_card_bao_ting = new MJHandlerOutCardBaoTing_ZYZJ();
		handler_select_magic = new HandlerSelectMagic_ZYZJ();
		handler_finish = new MJHandlerFinish_ZYZJ();
		_handler_switch_card = new HandlerSwitchCard_ZYZJ();
		_handler_gumai = new HandlerGuMai_ZYZJ();
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
	 * 结束调度
	 * 
	 * @return
	 */
	@Override
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

	public boolean exe_switch_card() {
		set_handler(_handler_switch_card);
		_handler.exe(this);
		return true;
	}

	public boolean exe_gu_mai() {
		set_handler(_handler_gumai);
		_handler.exe(this);
		return true;
	}

	@Override
	public void shuffle(int[] repertory_card, int[] mj_cards) {
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

		if (this.getClass().getAnnotation(ThreeDimension.class) != null) {
			show_tou_zi(GRR._banker_player);
		}

		int send_count;
		int have_send_count = 0;

		int count = getTablePlayerNumber();

		for (int i = 0; i < count; i++) {
			// 庄家起手14张牌，闲家起手13张牌。和之前的有点区别。GameStart之后，不会再发牌。直接走新的Handler。
			// if (i == GRR._banker_player) {
			// send_count = GameConstants.MAX_COUNT;
			// } else {
			send_count = (GameConstants.MAX_COUNT - 1);
			// }

			GRR._left_card_count -= send_count;

			_logic.switch_to_cards_index(repertory_card, have_send_count, send_count, GRR._cards_index[i]);

			have_send_count += send_count;
		}

		if (_recordRoomRecord != null) {
			_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));
		}
	}

	// 游戏开始
	@Override
	protected boolean on_handler_game_start() {
		if (_cur_round == 2) {
			// real_kou_dou();// 记录真实扣豆
		}

		reset_init_data();

		InitPama();

		// 庄家选择
		progress_banker_select();

		_game_status = GameConstants.GS_MJ_PLAY;

		// 信阳麻将
		GRR._banker_player = _cur_banker;
		_current_player = GRR._banker_player;
		banker_count[_current_player]++;
		_repertory_card = new int[GameConstants.CARD_COUNT_HU_NAN];
		shuffle(_repertory_card, GameConstants.CARD_DATA_WAN_TIAO_TONG);

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

		// operate_player_data();

		if (this.has_rule(GameConstants_ZYZJ.GAME_RULE_GUMAI_ZYZJ)) {
			exe_gu_mai();
			return true;
		}

		return on_game_start();
	}

	public void InitPama() {
		_logic.clean_magic_cards();
		this.clean_ji_cards();
		this.add_ji_card_index(_logic.switch_to_card_index(GameConstants_ZYZJ.YJ_CARD));
		if (has_rule(GameConstants_ZYZJ.GAME_RULE_WG_ZYZJ))
			this.add_ji_card_index(_logic.switch_to_card_index(GameConstants_ZYZJ.BA_TONG_CARD));

		if (this.has_rule(GameConstants_ZYZJ.GAME_RULE_LAIZI_ZYZJ)) {
			_logic.clean_magic_cards();
			_logic.add_magic_card_index(_logic.switch_to_card_index(GameConstants_ZYZJ.YI_TONG_CARD));
			GRR._especial_card_count = 2;
			GRR._especial_show_cards[0] = GameConstants_ZYZJ.YI_TONG_CARD + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
		}

		_game_status = GameConstants_ZYZJ.GS_MJ_PLAY;

		chong_feng_ji_seat_bt = -1;
		chong_feng_ji_seat_yj = -1;
		out_ji_pai = new int[getTablePlayerNumber()][];

		out_ji_pai_count = new int[getTablePlayerNumber()];
		jin_ji = new boolean[2]; // 只有两个默认的鸡产生金鸡
		responsibility_ji = new Tuple[2];
		show_responsibility_ji = new Tuple[2];
		player_mo_first = new boolean[getTablePlayerNumber()];
		shao = new boolean[getTablePlayerNumber()];
		shao_gang = new boolean[getTablePlayerNumber()];
		player_all_ji_card = new int[getTablePlayerNumber()];
		player_11_detail = new int[getTablePlayerNumber()][11];
		shang_xia_ji = new int[2];
		player_show_desc_10 = new int[getTablePlayerNumber()][30];
		hu_type_socre = new float[getTablePlayerNumber()];
		player_ji_score = new int[getTablePlayerNumber()];
		player_duan = new int[getTablePlayerNumber()];
		zi_da = new int[getTablePlayerNumber()];
		player_GangScore_type = new int[getTablePlayerNumber()][4];
		had_switch_card = new boolean[getTablePlayerNumber()];
		switch_card_index = new int[getTablePlayerNumber()][SWITCH_CARD_COUNT];
		ding_que_pai_se = new int[getTablePlayerNumber()];
		yuan_que_player = new int[getTablePlayerNumber()];
		player_duan_score = new int[getTablePlayerNumber()];
		player_gumai_score = new int[getTablePlayerNumber()];
		ze_ren_ji = new int[getTablePlayerNumber()];
		hu_base_fen = 0;
		ben_ji_card = GameConstants.INVALID_CARD;
		fan_ji_card = GameConstants.INVALID_CARD;
		hutype = 0;
		fangpaoplayer = new int[getTablePlayerNumber()];
		hupaiplayer = new int[getTablePlayerNumber()];
		for (int i = 0; i < 2; i++) {
			responsibility_ji[i] = new Tuple<Integer, Integer>(-1, -1);
			show_responsibility_ji[i] = new Tuple<Integer, Integer>(-1, -1);
		}
		for (int player = 0; player < getTablePlayerNumber(); player++) {
			shao[player] = false;
			shao_gang[player] = false;
			GRR._result_des[player] = "";
			out_ji_pai[player] = new int[20];
			player_mo_first[player] = true;
			player_duan[player] = -1;
			_player_result.pao[player] = 0;
			_player_result.qiang[player] = 0;
			yuan_que_player[player] = 0;
			hupaiplayer[player] = 0;
			fangpaoplayer[player] = 0;
			_player_result.ziba[player] = 0;
			ze_ren_ji[player] = 0;
			player_duan_score[player] = 0;
			player_gumai_score[player] = 0;
			for (int j = 0; j < 30; j++) {
				player_show_desc_10[player][j] = 0;
			}
		}

	}

	@Override
	protected boolean on_game_start() {

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
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			for (int j = 0; j < GameConstants_HanShouWang.MAX_COUNT; j++) {
				if (_logic.is_magic_card(hand_cards[i][j])) {
					hand_cards[i][j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				}
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}
		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(GRR._left_card_count);
		GRR.add_room_response(roomResponse);

		// 检测听牌
		// for (int i = 0; i < getTablePlayerNumber(); i++) {
		// _playerStatus[i]._hu_card_count =
		// get_ting_card(_playerStatus[i]._hu_cards,
		// _playerStatus[i]._hu_out_cards_fan[0], GRR._cards_index[i],
		// GRR._weave_items[i], GRR._weave_count[i], i);
		// if (_playerStatus[i]._hu_card_count > 0) {
		// operate_chi_hu_cards(i, _playerStatus[i]._hu_card_count,
		// _playerStatus[i]._hu_cards);
		// }
		// }

		old_banker = _current_player;

		if (this.has_rule(GameConstants_ZYZJ.GAME_RULE_SWAP_ZYZJ)) {
			exe_switch_card();
		} else {
			exe_dispatch_card(_current_player, GameConstants_ZYZJ.DispatchCard_Type_Tian_Hu, GameConstants_ZYZJ.DELAY_SEND_CARD_DELAY);
		}

		return true;
	}

	/**
	 * 加载房间里的玩家信息
	 * 
	 * @param roomResponse
	 */
	@Override
	public void load_player_info_data(RoomResponse.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponse.Builder room_player = RoomPlayerResponse.newBuilder();
			room_player.setAccountId(rplayer.getAccount_id());
			room_player.setHeadImgUrl(rplayer.getAccount_icon());
			room_player.setIp(rplayer.getAccount_ip());
			room_player.setUserName(rplayer.getNick_name());
			room_player.setSeatIndex(rplayer.get_seat_index());
			room_player.setOnline(rplayer.isOnline() ? 1 : 0);
			room_player.setIpAddr(rplayer.getAccount_ip_addr());
			room_player.setSex(rplayer.getSex());
			room_player.setScore(_player_result.game_score[i]);
			room_player.setReady(_player_ready[i]);
			room_player.setPao(_player_result.pao[i]);
			room_player.setNao(_player_result.nao[i]);
			if (has_rule(GameConstants_ZYZJ.GAME_RULE_LIANZHUANG_ZYZJ)) {
				room_player.setQiang(_player_result.qiang[i]);
			}
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			room_player.setHasPiao(_player_result.haspiao[i]);
			room_player.setBiaoyan(_player_result.biaoyan[i]);
			room_player.setZiBa(_player_result.ziba[i]);
			room_player.setDuanMen(_player_result.duanmen[i]);
			room_player.setOpenThree(_player_open_less[i] == 0 ? false : true);

			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}

			if (_game_status == GameConstants.GS_MJ_PAO) {
				if (null != _playerStatus[i]) {
					// 叫地主的字段值，用来标示玩家是否点了跑呛
					if (_playerStatus[i]._is_pao_qiang) {
						room_player.setJiaoDiZhu(1);
					} else {
						room_player.setJiaoDiZhu(0);
					}
				}
			}

			roomResponse.addPlayers(room_player);
		}
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
						GameConstants_ZYZJ.HU_CARD_TYPE_QIANG_GANG, i);

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
			if (player_duan[i] != -1 && _logic.get_card_color(card) == player_duan[i])
				continue;

			playerStatus = _playerStatus[i];

			boolean can_peng = true;
			int[] tmp_cards_data = _playerStatus[i].get_cards_abandoned_peng();
			for (int x = 0; x < GameConstants_ZYZJ.MAX_ABANDONED_CARDS_COUNT; x++) {
				if (tmp_cards_data[x] == card) {
					can_peng = false;
					break;
				}
			}
			// 有癞子的碰、杠
			/*
			 * if(this.has_rule(GameConstants_ZYZJ.GAME_RULE_LAIZI_ZYZJ)){ if
			 * (can_peng && GRR._left_card_count > 0 &&
			 * !_playerStatus[i].is_bao_ting()) { action =
			 * _logic.check_peng_with_laizi_zyzj(GRR._cards_index[i], card); if
			 * ((action & GameConstants.WIK_PENG) != 0) {
			 * playerStatus.add_normal_wik(card, GameConstants.WIK_PENG,
			 * seat_index); playerStatus.add_action(GameConstants.WIK_PENG);
			 * bAroseAction = true; } if ((action &
			 * GameConstants.WIK_SUO_PENG_1) != 0) {
			 * if(!this._logic.is_magic_card(card)){
			 * playerStatus.add_normal_wik(card, GameConstants.WIK_SUO_PENG_1,
			 * seat_index); playerStatus.add_action(GameConstants.WIK_PENG);
			 * bAroseAction = true; } } if ((action &
			 * GameConstants.WIK_SUO_PENG_2) != 0) {
			 * if(!this._logic.is_magic_card(card)){
			 * playerStatus.add_normal_wik(card, GameConstants.WIK_SUO_PENG_2,
			 * seat_index); playerStatus.add_action(GameConstants.WIK_PENG);
			 * bAroseAction = true; } }
			 * 
			 * action =
			 * _logic.estimate_gang_card_with_laizi_zyzj(GRR._cards_index[i],
			 * card); if (action != 0) {
			 * playerStatus.add_action(GameConstants.WIK_GANG); bAroseAction =
			 * true;
			 * 
			 * if ((action & GameConstants.WIK_GANG) != 0) {
			 * playerStatus.add_normal_gang_wik(card, GameConstants.WIK_GANG,
			 * seat_index, 1); } if ((action & GameConstants.WIK_SUO_GANG_1) !=
			 * 0 && !this._logic.is_magic_card(card)) {
			 * playerStatus.add_normal_gang_wik(card,
			 * GameConstants.WIK_SUO_GANG_1, seat_index, 1); } if ((action &
			 * GameConstants.WIK_SUO_GANG_2) != 0 &&
			 * !this._logic.is_magic_card(card)) {
			 * playerStatus.add_normal_gang_wik(card,
			 * GameConstants.WIK_SUO_GANG_2, seat_index, 1); } if ((action &
			 * GameConstants.WIK_SUO_GANG_3) != 0 &&
			 * !this._logic.is_magic_card(card)) {
			 * playerStatus.add_normal_gang_wik(card,
			 * GameConstants.WIK_SUO_GANG_3, seat_index, 1); } } } }else{
			 */
			if (can_peng && GRR._left_card_count > 0 && !_playerStatus[i].is_bao_ting()) {
				action = _logic.check_peng(GRR._cards_index[i], card);
				if (action != 0) {
					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);
					bAroseAction = true;
				}

				if (GRR._left_card_count > 0) {
					action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
					if (action != 0) {
						playerStatus.add_action(GameConstants_ZYZJ.WIK_GANG);
						playerStatus.add_gang(card, seat_index, 1); // 加上杠
						bAroseAction = true;
					}
				}
			}
			// }

			if (_playerStatus[i].is_chi_hu_round()) {
				boolean can_hu_this_card = true;
				int[] tmp_cards_data_hu = _playerStatus[i].get_cards_abandoned_hu();
				for (int x = 0; x < GameConstants_ZYZJ.MAX_ABANDONED_CARDS_COUNT; x++) {
					if (tmp_cards_data_hu[x] == card) {
						can_hu_this_card = false;
						break;
					}
				}
				if (can_hu_this_card) {
					ChiHuRight chr = GRR._chi_hu_rights[i];
					chr.set_empty();
					int cbWeaveCount = GRR._weave_count[i];

					int card_type = GameConstants_ZYZJ.HU_CARD_TYPE_JIE_PAO;
					if (type == GameConstants.GANG_TYPE_AN_GANG || type == GameConstants.GANG_TYPE_ADD_GANG
							|| type == GameConstants.GANG_TYPE_JIE_GANG) {
						card_type = GameConstants_ZYZJ.HU_CARD_TYPE_RE_PAO;
					}

					// 此处偷个懒，若出牌得人已报听牌，其他人没有通行证的情况下也是可以胡的
					// if (_playerStatus[seat_index].is_bao_ting())
					// card_type = GameConstants_ZYZJ.HU_CARD_TYPE_ZI_MO;

					action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr, card_type, i);

					if (player_duan[i] != -1) {
						for (int c = 0; c < GameConstants.MAX_INDEX; c++) {
							if (GRR._cards_index[i][c] > 0 && _logic.get_card_color(_logic.switch_to_card_data(c)) == player_duan[i]
									&& !_logic.is_magic_card(_logic.switch_to_card_data(c))) {
								action = GameConstants_HZLZG.WIK_NULL;
								break;
							}
						}
					}
					if (action != 0) {
						_playerStatus[i].add_action(GameConstants_ZYZJ.WIK_CHI_HU);
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

	// @Override
	// public int getTablePlayerNumber() {
	// return 4;
	// }

	@Override
	public int getTablePlayerNumber() {
		if (this.has_rule(GameConstants_ZYZJ.GAME_RULE_FOUR_ZYZJ)) {
			return 4;
		}
		if (this.has_rule(GameConstants_ZYZJ.GAME_RULE_THREE_ZYZJ) || this.has_rule(GameConstants_ZYZJ.GAME_RULE_SDG_ZYZJ)) {
			return 3;
		}
		if (this.has_rule(GameConstants_ZYZJ.GAME_RULE_TWO_ZYZJ) || this.has_rule(GameConstants_ZYZJ.GAME_RULE_EDG_ZYZJ)) {
			return 2;
		}
		return 4;
	}

	@Override
	protected boolean on_handler_game_finish(int seat_index, int reason) {
		int real_reason = reason;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_ready[i] = 0;
			_player_result.ziba[i] = 0;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();

		roomResponse.setLeftCardCount(0);

		load_common_status(roomResponse);
		load_room_info_data(roomResponse);

		RoomInfo.Builder room_info = getRoomInfo();
		game_end.setRoomInfo(room_info);
		game_end.setRunPlayerId(_run_player_id);
		game_end.setRoundOverType(0);
		game_end.setGamePlayerNumber(getTablePlayerNumber());
		game_end.setEndTime(System.currentTimeMillis() / 1000L);
		if (GRR != null) {
			game_end.setRoundOverType(1);
			game_end.setStartTime(GRR._start_time);

			game_end.setGameTypeIndex(GRR._game_type_index);
			roomResponse.setLeftCardCount(GRR._left_card_count);

			for (int i = 0; i < GRR._especial_card_count; i++) {
				game_end.addEspecialShowCards(GRR._especial_show_cards[i]);
			}

			GRR._end_type = reason;

			float lGangScore[] = new float[getTablePlayerNumber()];

			if (reason == GameConstants.Game_End_NORMAL) {
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (shao[i] || shao_gang[i])
						continue;
					for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
						for (int k = 0; k < getTablePlayerNumber(); k++) {
							lGangScore[k] += GRR._gang_score[i].scores[j][k];
						}
					}
					for (int j = 0; j < getTablePlayerNumber(); j++) {
						_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
					}
				}
			}
			
			if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
				cel_yuan_que();
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				GRR._game_score[i] += lGangScore[i];
				GRR._game_score[i] += GRR._start_hu_score[i];

				_player_result.game_score[i] += GRR._game_score[i];

				Int32ArrayResponse.Builder items = Int32ArrayResponse.newBuilder();

				items.addItem(player_show_desc_10[i][0]);

				items.addItem(player_show_desc_10[i][1]);

				items.addItem(player_show_desc_10[i][2]);

				items.addItem(player_show_desc_10[i][3]);

				items.addItem(player_show_desc_10[i][4]);

				items.addItem(player_show_desc_10[i][5]);

				items.addItem(player_show_desc_10[i][6]);

				items.addItem(player_show_desc_10[i][7]);

				items.addItem(player_show_desc_10[i][8]);

				items.addItem(player_show_desc_10[i][9]);

				items.addItem(player_show_desc_10[i][10]);

				items.addItem(player_show_desc_10[i][11]);

				items.addItem(player_show_desc_10[i][12]);

				items.addItem(player_show_desc_10[i][13]);

				items.addItem(player_show_desc_10[i][14]);

				items.addItem(player_show_desc_10[i][15]);

				items.addItem(player_show_desc_10[i][16]);

				items.addItem(player_show_desc_10[i][17]);

				items.addItem(player_show_desc_10[i][18]);

				items.addItem(player_show_desc_10[i][19]);

				items.addItem(player_show_desc_10[i][20]);

				items.addItem(player_show_desc_10[i][21]);

				items.addItem(player_show_desc_10[i][22]);

				items.addItem(player_show_desc_10[i][23]);

				items.addItem(player_show_desc_10[i][24]);

				items.addItem(player_show_desc_10[i][25]);

				items.addItem(player_show_desc_10[i][26]);

				game_end.addLostFanShu(items);
				game_end.addGangScore(lGangScore[i]);
				game_end.addPao((int) hu_type_socre[i]);
				game_end.addJettonScore(player_ji_score[i]);
			}

			load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);

			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);
			game_end.setLeftCardCount(GRR._left_card_count);
			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

			for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i < GRR._count_niao; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao[i]);
			}
			for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i < GRR._count_niao_fei; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao_fei[i]);
			}
			game_end.setCountPickNiao(GRR._count_pick_niao + GRR._count_pick_niao_fei);// 中鸟个数

			game_end.setCountPickNiao(this.hutype);
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				// 鸟牌，必须按四人计算
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

			set_result_describe();

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {
					cs.addItem(GRR._cards_data[i][j]);
				}
				game_end.addCardsData(cs);

				WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
				for (int j = 0; j < GRR._weave_count[i]; j++) {
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

				game_end.addCardsDataNiao(this.hupaiplayer[i]);
				game_end.addProvidePlayer(this.fangpaoplayer[i]);
				// game_end.addProvidePlayer(GRR._provider[i]);
				game_end.addGameScore(GRR._game_score[i]);
				game_end.addGangScore(lGangScore[i]);
				game_end.addStartHuScore(GRR._start_hu_score[i]);
				game_end.addResultDes(GRR._result_des[i]);

				game_end.addWinOrder(GRR._win_order[i]);

				// Int32ArrayResponse.Builder lfs =
				// Int32ArrayResponse.newBuilder();
				// for (int j = 0; j < getTablePlayerNumber(); j++) {
				// lfs.addItem(GRR._lost_fan_shu[i][j]);
				// }
				//
				// game_end.addLostFanShu(lfs);

			}

		}

		for (int player = 0; player < getTablePlayerNumber() && GRR != null; player++) {
			GRR._result_des[player] = "";
		}
		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round && (!is_sys())) {
				end = true;
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(process_player_result(reason));
			} else {
			}
		} else if ((!is_sys()) && (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM)) {
			end = true;
			real_reason = GameConstants.Game_End_DRAW;
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(process_player_result(reason));
		}
		game_end.setEndType(real_reason);

		roomResponse.setGameEnd(game_end);

		send_response_to_room(roomResponse);
		this.roomResponse = roomResponse;

		record_game_round(game_end);

		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT || reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < getTablePlayerNumber(); j++) {
				Player player = get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");

			}
		}

		if (end && (!is_sys())) {
			PlayerServiceImpl.getInstance().delRoomId(getRoom_id());
		}

		if (!is_sys()) {
			game_end_GRR = GRR;
			GRR = null;
		}

		if (is_sys()) {
			clear_score_in_gold_room();
		}

		return false;
	}
	
	//算原缺的分数
	public void cel_yuan_que(){
		int yuan_que[] = new int[getTablePlayerNumber()];
		for(int i = 0;i < this.getTablePlayerNumber();i++)
			yuan_que[i] = 0;
		for(int i = 0;i < this.getTablePlayerNumber();i++){
			if (this.yuan_que_player[i] == 1) {
				for(int k = 0;k < this.getTablePlayerNumber();k++){
					if(k == i)
						continue;
					yuan_que[k] -= 2;
					yuan_que[i] += 2;
				}
			}
		}
		
		for(int i = 0;i < this.getTablePlayerNumber();i++){
			player_show_desc_10[i][17] = yuan_que[i];
			GRR._game_score[i] += yuan_que[i];
			hu_type_socre[i] += yuan_que[i];
		}

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
				// if (weaveitems[j].weave_kind == GameConstants.WIK_GANG &&
				// weaveitems[j].public_card == 0) {
				// weaveItem_item.setCenterCard(0);
				// } else {
				weaveItem_item.setCenterCard(weaveitems[j].center_card);
				// }
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

	/**
	 * 扑克转换 将手中牌索引 转换为实际牌数据
	 * 
	 * @param cards_index
	 * @param cards_data
	 * @return
	 */
	public int switch_to_cards_data(int cards_index[], int cards_data[], int seat_index) {
		boolean add_flag = false;
		if (player_duan[seat_index] != -1) {
			for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
				if (cards_index[i] > 0 && _logic.get_card_color(_logic.switch_to_card_data(i)) == player_duan[seat_index]
						&& !_logic.is_magic_card(_logic.switch_to_card_data(i))) {
					add_flag = true;
					break;
				}
			}
		}
		// 转换扑克
		int cbPosition = 0;
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (cards_index[i] > 0) {
				for (int j = 0; j < cards_index[i]; j++) {
					if (add_flag && _logic.get_card_color(_logic.switch_to_card_data(i)) != player_duan[seat_index]) {
						cards_data[cbPosition++] = _logic.switch_to_card_data(i) + GameConstants_ZYZJ.CARD_ESPECIAL_TYPE_LIANG_ZHANG;
					} else {
						//cards_data[cbPosition++] = _logic.switch_to_card_data(i);
					}
				}
			}
		}
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (cards_index[i] > 0) {
				for (int j = 0; j < cards_index[i]; j++) {
					if (add_flag && _logic.get_card_color(_logic.switch_to_card_data(i)) != player_duan[seat_index]) {
						//cards_data[cbPosition++] = _logic.switch_to_card_data(i) + GameConstants_ZYZJ.CARD_ESPECIAL_TYPE_LIANG_ZHANG;
					} else {
						cards_data[cbPosition++] = _logic.switch_to_card_data(i);
					}
				}
			}
		}
		return cbPosition;
	}

	public int get_ting_card(int[] cards, int[] cards_hu_fan, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		int cbCardIndexTemp[] = new int[GameConstants_ZYZJ.MAX_INDEX];
		for (int i = 0; i < GameConstants_ZYZJ.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		if (player_duan[seat_index] != -1) {
			for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
				if (GRR._cards_index[seat_index][i] > 0 && _logic.get_card_color(_logic.switch_to_card_data(i)) == player_duan[seat_index]
						&& !_logic.is_magic_card(_logic.switch_to_card_data(i))) {
					return count;
				}
			}
		}

		int max_ting_count = GameConstants_ZYZJ.MAX_ZI;
		int real_max_ting_count = GameConstants_ZYZJ.MAX_ZI;

		for (int i = 0; i < max_ting_count; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			if (GameConstants_ZYZJ.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					Constants_HuangZhou.CHR_ZI_MO, seat_index)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}

		if (count == 0) {
		} else if (count == real_max_ting_count) {
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	public int get_pai_xin_fan(ChiHuRight chr) {
		if (!chr.opr_and(GameConstants_ZYZJ.CHR_QING_LONG_BEI).is_empty()) {
			return 30;
		}
		if (!chr.opr_and(GameConstants_ZYZJ.CHR_QING_QI_DUI).is_empty()) {
			return 20;
		}
		if (!chr.opr_and(GameConstants_ZYZJ.CHR_LONG_QI_DUI).is_empty()) {
			return 20;
		}
		if (!chr.opr_and(GameConstants_ZYZJ.CHR_SHUANG_QING).is_empty()) {
			return 20;
		}
		if (!chr.opr_and(GameConstants_ZYZJ.CHR_QING_DA_DUI).is_empty()) {
			return 15;
		}
		if (!chr.opr_and(GameConstants_ZYZJ.CHR_XIAO_QI_DUI).is_empty()) {
			return 10;
		}
		if (!chr.opr_and(GameConstants_ZYZJ.CHR_DAN_DIAO).is_empty()) {
			return 10;
		}
		if (!chr.opr_and(GameConstants_ZYZJ.CHR_QING_YI_SE).is_empty()) {
			return 10;
		}
		if (!chr.opr_and(GameConstants_ZYZJ.CHR_DA_DUI_ZI).is_empty()) {
			return 5;
		}
		return 1;
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

			GRR._chi_hu_rights[i] = chr;
			GRR._chi_hu_rights[i].set_valid(true);
			process_cha_hu_score(i, chr);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public ChiHuRight huan_zhuang_cha_hu(int[] cards_index, WeaveItem[] weaveItems, int cbWeaveCount, int seat_index) {
		int cbCardIndexTemp[] = new int[GameConstants_ZYZJ.MAX_INDEX];
		for (int i = 0; i < GameConstants_ZYZJ.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		int cbCurrentCard;
		int max_ting_count = GameConstants_ZYZJ.MAX_ZI;

		List<com.cai.common.util.Tuple> list = Lists.newArrayList();
		for (int i = 0; i < max_ting_count; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			ChiHuRight chr = new ChiHuRight();
			if (GameConstants_ZYZJ.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItems, cbWeaveCount, cbCurrentCard, chr,
					Constants_HuangZhou.CHR_ZI_MO, seat_index)) {
				list.add(new com.cai.common.util.Tuple<>(cbCurrentCard, chr, 0));
			}
		}

		if (list.isEmpty())
			return null;

		if (!this.has_rule(GameConstants_ZYZJ.GAME_RULE_EDG_ZYZJ) && !this.has_rule(GameConstants_ZYZJ.GAME_RULE_SDG_ZYZJ)) {
			// 设置各胡型权重
			list.forEach((tuple) -> {
				ChiHuRight chr = (ChiHuRight) tuple.getCenter();
				if (!chr.opr_and(GameConstants_ZYZJ.CHR_QING_LONG_BEI).is_empty()) {
					tuple.setRight(GameConstants_ZYZJ.CHR_QING_LONG_BEI);
					return;
				}
				if (!chr.opr_and(GameConstants_ZYZJ.CHR_QING_QI_DUI).is_empty()) {
					tuple.setRight(GameConstants_ZYZJ.CHR_QING_QI_DUI);
					return;
				}
				if (!chr.opr_and(GameConstants_ZYZJ.CHR_LONG_QI_DUI).is_empty()) {
					tuple.setRight(GameConstants_ZYZJ.CHR_LONG_QI_DUI);
					return;
				}
				if (!chr.opr_and(GameConstants_ZYZJ.CHR_QING_YI_SE).is_empty()) {
					tuple.setRight(GameConstants_ZYZJ.CHR_QING_YI_SE);
					return;
				}
				if (!chr.opr_and(GameConstants_ZYZJ.CHR_XIAO_QI_DUI).is_empty()) {
					tuple.setRight(GameConstants_ZYZJ.CHR_XIAO_QI_DUI);
					return;
				}
				if (!chr.opr_and(GameConstants_ZYZJ.CHR_QING_YI_SE).is_empty()) {
					tuple.setRight(GameConstants_ZYZJ.CHR_QING_YI_SE);
					return;
				}
				if (!chr.opr_and(GameConstants_ZYZJ.CHR_DA_DUI_ZI).is_empty()) {
					tuple.setRight(GameConstants_ZYZJ.CHR_DA_DUI_ZI);
					return;
				}
				if (!chr.opr_and(GameConstants_ZYZJ.CHR_DA_KUAN_ZHNAG).is_empty()) {
					tuple.setRight(GameConstants_ZYZJ.CHR_DA_KUAN_ZHNAG);
					return;
				} else if (!chr.opr_and(GameConstants_ZYZJ.CHR_KA_DIAN).is_empty()) {
					tuple.setRight(GameConstants_ZYZJ.CHR_KA_DIAN);
					return;
				}

				tuple.setRight(1); // 权重最低
			});
		} else {
			// 设置各胡型权重
			list.forEach((tuple) -> {
				ChiHuRight chr = (ChiHuRight) tuple.getCenter();
				if (!chr.opr_and(GameConstants_ZYZJ.CHR_QING_LONG_BEI).is_empty()) {
					tuple.setRight(GameConstants_ZYZJ.CHR_QING_LONG_BEI);
					return;
				}
				if (!chr.opr_and(GameConstants_ZYZJ.CHR_QING_QI_DUI).is_empty()) {
					tuple.setRight(GameConstants_ZYZJ.CHR_QING_QI_DUI);
					return;
				}
				if (!chr.opr_and(GameConstants_ZYZJ.CHR_QING_DAN).is_empty()) {
					tuple.setRight(GameConstants_ZYZJ.CHR_QING_DAN);
					return;
				}
				if (!chr.opr_and(GameConstants_ZYZJ.CHR_LONG_QI_DUI).is_empty()) {
					tuple.setRight(GameConstants_ZYZJ.CHR_LONG_QI_DUI);
					return;
				}
				if (!chr.opr_and(GameConstants_ZYZJ.CHR_QING_DA_DUI).is_empty()) {
					tuple.setRight(GameConstants_ZYZJ.CHR_QING_DA_DUI);
					return;
				}
				if (!chr.opr_and(GameConstants_ZYZJ.CHR_DAN_DIAO).is_empty()) {
					tuple.setRight(GameConstants_ZYZJ.CHR_DAN_DIAO);
					return;
				}
				if (!chr.opr_and(GameConstants_ZYZJ.CHR_QING_YI_SE).is_empty()) {
					tuple.setRight(GameConstants_ZYZJ.CHR_QING_YI_SE);
					return;
				}
				if (!chr.opr_and(GameConstants_ZYZJ.CHR_XIAO_QI_DUI).is_empty()) {
					tuple.setRight(GameConstants_ZYZJ.CHR_XIAO_QI_DUI);
					return;
				} else if (!chr.opr_and(GameConstants_ZYZJ.CHR_DA_DUI_ZI).is_empty()) {
					tuple.setRight(GameConstants_ZYZJ.CHR_DA_DUI_ZI);
					return;
				}

				tuple.setRight(1); // 权重最低
			});
		}

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

			float s = lChiHuScore /*
									 * + (continue_banker_count > 0 ?
									 * continue_banker_count + 1 : 0)
									 */;

			GRR._game_score[i] -= s;
			hu_type_socre[i] -= s;
			GRR._game_score[seat_index] += s;
			hu_type_socre[seat_index] += s;
		}
	}

	// 七小对牌 七小对：胡牌时，手上任意七对牌。
	public int is_qi_xiao_dui(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int cur_card) {

		// 组合判断
		if (cbWeaveCount != 0)
			return GameConstants.WIK_NULL;

		// 单牌数目
		int cbReplaceCount = 0;
		int nGenCount = 0;

		// 临时数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入数据
		int cbCurrentIndex = _logic.switch_to_card_index(cur_card);
		cbCardIndexTemp[cbCurrentIndex]++;

		// 计算单牌
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];

			// 单牌统计
			if (cbCardCount == 1 || cbCardCount == 3)
				cbReplaceCount++;

			if (cbCardCount == 4 && _logic.switch_to_card_data(i) == cur_card) {
				nGenCount++;
			}
		}

		if (cbReplaceCount > 0)
			return GameConstants.WIK_NULL;

		if (nGenCount > 0) {
			return GameConstants_ZYZJ.CHR_LONG_QI_DUI;
		} else {
			return GameConstants_ZYZJ.CHR_XIAO_QI_DUI;
		}
	}

	private int get_hu_fen(ChiHuRight chr) {
		int fen = 1;
		if (!this.has_rule(GameConstants_ZYZJ.GAME_RULE_EDG_ZYZJ) && !this.has_rule(GameConstants_ZYZJ.GAME_RULE_SDG_ZYZJ)) {
			fen = 2;
			if (!chr.opr_and(GameConstants_ZYZJ.CHR_QING_LONG_BEI).is_empty()) {
				fen = 30;
				return fen;
			}
			if (!chr.opr_and(GameConstants_ZYZJ.CHR_QING_QI_DUI).is_empty()) {
				fen = 20;
				return fen;
			}
			if (!chr.opr_and(GameConstants_ZYZJ.CHR_LONG_QI_DUI).is_empty()) {
				fen = 10;
				return fen;
			}
			if (!chr.opr_and(GameConstants_ZYZJ.CHR_QING_YI_SE).is_empty() && !chr.opr_and(GameConstants_ZYZJ.CHR_DA_DUI_ZI).is_empty()) {
				fen = 16;
				return fen;
			}
			if (!chr.opr_and(GameConstants_ZYZJ.CHR_QING_YI_SE).is_empty() && !chr.opr_and(GameConstants_ZYZJ.CHR_DA_KUAN_ZHNAG).is_empty()) {
				fen = 15;
				return fen;
			}
			if (!chr.opr_and(GameConstants_ZYZJ.CHR_QING_YI_SE).is_empty() && !chr.opr_and(GameConstants_ZYZJ.CHR_KA_DIAN).is_empty()) {
				fen = 13;
				return fen;
			}
			if (!chr.opr_and(GameConstants_ZYZJ.CHR_QING_YI_SE).is_empty()) {
				fen = 10;
				return fen;
			}
			if (!chr.opr_and(GameConstants_ZYZJ.CHR_XIAO_QI_DUI).is_empty()) {
				fen = 7;
				return fen;
			}
			if (!chr.opr_and(GameConstants_ZYZJ.CHR_DA_DUI_ZI).is_empty()) {
				fen = 6;
				return fen;
			}
			if (!chr.opr_and(GameConstants_ZYZJ.CHR_DA_KUAN_ZHNAG).is_empty()) {
				fen = 5;
				return fen;
			}
			if (!chr.opr_and(GameConstants_ZYZJ.CHR_KA_DIAN).is_empty()) {
				fen = 4;
				return fen;
			}
		} else {
			fen = 1;
			if (!chr.opr_and(GameConstants_ZYZJ.CHR_QING_LONG_BEI).is_empty()) {
				fen = 30;
				return fen;
			}
			if (!chr.opr_and(GameConstants_ZYZJ.CHR_QING_QI_DUI).is_empty()) {
				fen = 20;
				return fen;
			}
			if (!chr.opr_and(GameConstants_ZYZJ.CHR_QING_DAN).is_empty()) {
				fen = 20;
				return fen;
			}
			if (!chr.opr_and(GameConstants_ZYZJ.CHR_LONG_QI_DUI).is_empty()) {
				fen = 20;
				return fen;
			}
			if (!chr.opr_and(GameConstants_ZYZJ.CHR_QING_DA_DUI).is_empty()) {
				fen = 15;
				return fen;
			}
			if (!chr.opr_and(GameConstants_ZYZJ.CHR_DAN_DIAO).is_empty()) {
				fen = 10;
				return fen;
			}
			if (!chr.opr_and(GameConstants_ZYZJ.CHR_QING_YI_SE).is_empty()) {
				fen = 10;
				return fen;
			}
			if (!chr.opr_and(GameConstants_ZYZJ.CHR_XIAO_QI_DUI).is_empty()) {
				fen = 10;
				return fen;
			}
			if (!chr.opr_and(GameConstants_ZYZJ.CHR_DA_DUI_ZI).is_empty()) {
				fen = 5;
				return fen;
			}
		}

		return fen;
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {

		if (!this._logic.is_magic_card(cur_card) && _logic.get_card_color(cur_card) == player_duan[_seat_index]) {
			return GameConstants.WIK_NULL;
		}

		boolean out_card_laizi = false;
		if ((card_type == GameConstants_ZYZJ.HU_CARD_TYPE_JIE_PAO || card_type == GameConstants_ZYZJ.HU_CARD_TYPE_RE_PAO)
				&& this._logic.is_magic_card(cur_card)) {
			out_card_laizi = true;
		}

		boolean have_fan_hu = false;

		// 清一色
		boolean bqing_yi_se = false;
		if (_logic.is_qing_yi_se(cards_index, weaveItems, weave_count, cur_card)) {
			have_fan_hu = true;
			bqing_yi_se = true;

		}

		// 小七对
		// int check_dui_zi_hu = _logic.analyse_qi_xiao_dui(cards_index,
		// weaveItems, weave_count, cur_card);
		long check_dui_zi_hu = GameConstants.WIK_NULL;
		if (out_card_laizi) {
			check_dui_zi_hu = analyse_qi_xiao_dui_out_laizi(cards_index, weave_count, cur_card);
		} else {
			check_dui_zi_hu = analyse_qi_xiao_dui(cards_index, weave_count, cur_card);
		}

		if (GameConstants.WIK_NULL != check_dui_zi_hu) {
			have_fan_hu = true;
			if (bqing_yi_se) {
				if (GameConstants_ZYZJ.CHR_LONG_QI_DUI == check_dui_zi_hu) {
					chiHuRight.opr_or(GameConstants_ZYZJ.CHR_QING_LONG_BEI);
				} else if (GameConstants_ZYZJ.CHR_XIAO_QI_DUI == check_dui_zi_hu) {
					chiHuRight.opr_or(GameConstants_ZYZJ.CHR_QING_QI_DUI);
				}
			} else {
				chiHuRight.opr_or(check_dui_zi_hu);
			}
		}

		boolean bValue = false;
		if (out_card_laizi) {
			bValue = AnalyseCardUtil.analyse_win_by_cards_index_shanxill(cards_index, _logic.switch_to_card_index(cur_card),
					_logic.get_all_magic_card_index(), _logic.get_magic_card_count());
		} else {
			bValue = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card), _logic.get_all_magic_card_index(),
					_logic.get_magic_card_count());
		}

		if (!bValue && check_dui_zi_hu == GameConstants_ZYZJ.WIK_NULL) {
			chiHuRight.set_empty();
			return GameConstants_HanShouWang.WIK_NULL;
		}

		// 三丁拐，二丁拐有单吊
		if (this.has_rule(GameConstants_ZYZJ.GAME_RULE_EDG_ZYZJ) || this.has_rule(GameConstants_ZYZJ.GAME_RULE_SDG_ZYZJ)) {
			if (weave_count >= 4) {
				have_fan_hu = true;
				if (bqing_yi_se) {
					chiHuRight.opr_or(GameConstants_ZYZJ.CHR_QING_DAN);
				} else {
					chiHuRight.opr_or(GameConstants_ZYZJ.CHR_DAN_DIAO);
				}
			}
		}

		// 大七对
		boolean pengpeng_hu = false;
		if (out_card_laizi) {
			pengpeng_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index_taojiang(cards_index, _logic.switch_to_card_index(cur_card),
					_logic.get_all_magic_card_index(), _logic.get_magic_card_count());

		} else {
			pengpeng_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
					_logic.get_all_magic_card_index(), _logic.get_magic_card_count());
		}

		if (pengpeng_hu) {
			have_fan_hu = true;
			// 三丁拐，二丁拐有清大对
			if (this.has_rule(GameConstants_ZYZJ.GAME_RULE_EDG_ZYZJ) || this.has_rule(GameConstants_ZYZJ.GAME_RULE_SDG_ZYZJ)) {
				if (bqing_yi_se) {
					chiHuRight.opr_or(GameConstants_ZYZJ.CHR_QING_DA_DUI);
				} else {
					chiHuRight.opr_or(GameConstants_ZYZJ.CHR_DA_DUI_ZI);
				}
			} else {
				chiHuRight.opr_or(GameConstants_ZYZJ.CHR_DA_DUI_ZI);
			}

		}

		// 清一色
		if (bqing_yi_se) {
			chiHuRight.opr_or(GameConstants_ZYZJ.CHR_QING_YI_SE);
		}

		if (is_ka_bian_diao(_seat_index)) {
			have_fan_hu = true;
			chiHuRight.opr_or(GameConstants_ZYZJ.CHR_KA_DIAN);
		}

		if (is_da_kuan_zhang(_seat_index)) {
			have_fan_hu = true;
			chiHuRight.opr_or(GameConstants_ZYZJ.CHR_DA_KUAN_ZHNAG);
		}

		if (card_type == GameConstants_ZYZJ.HU_CARD_TYPE_ZI_MO) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else if (card_type == GameConstants_ZYZJ.HU_CARD_TYPE_QIANG_GANG) {
			have_fan_hu = true;
			chiHuRight.opr_or(GameConstants_ZYZJ.CHR_QING_GANG_HU);// 抢杠胡
		} else if (card_type == GameConstants_ZYZJ.HU_CARD_TYPE_RE_PAO) {
			chiHuRight.opr_or(GameConstants_ZYZJ.CHR_RE_PAO);// 热炮
		} else if (card_type == GameConstants_ZYZJ.HU_CARD_TYPE_GANG_KAI_HUA) {
			chiHuRight.opr_or(GameConstants_ZYZJ.CHR_GNAG_KAI);
		} else if (card_type == GameConstants_ZYZJ.HU_CARD_TYPE_JIE_PAO) {
			chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
		}

		// 三丁拐，二丁拐，接炮要有杠才能胡
		if (card_type == GameConstants_ZYZJ.HU_CARD_TYPE_JIE_PAO
				&& (this.has_rule(GameConstants_ZYZJ.GAME_RULE_EDG_ZYZJ) || this.has_rule(GameConstants_ZYZJ.GAME_RULE_SDG_ZYZJ))) {
			int gang_count = 0;
			for (int w = 0; w < GRR._weave_count[_seat_index]; w++) {
				if (GRR._weave_items[_seat_index][w].weave_kind != GameConstants.WIK_GANG) {
					continue;
				}
				gang_count++;
			}
			if (have_fan_hu)
				return GameConstants_ZYZJ.WIK_CHI_HU;
			if (gang_count > 0 || _playerStatus[_seat_index].is_bao_ting() || _out_card_count == 1)
				return GameConstants_ZYZJ.WIK_CHI_HU;

			chiHuRight.set_empty();
			return GameConstants_ZYZJ.WIK_NULL;
		}
		return GameConstants_ZYZJ.WIK_CHI_HU;
	}

	/*
	 * public void process_ji_fen_1(int bao_seat) { for (int player = 0; player
	 * < getTablePlayerNumber(); player++) { int s = 0; //int ting_count =
	 * _playerStatus[player]._hu_card_count;
	 * 
	 * // 冲锋鸡 if (chong_feng_ji_seat_yj == player) { if(jin_ji[0]){ s += 4;
	 * player_show_desc_10[player][0] += 4; }else{ s += 2;
	 * player_show_desc_10[player][1] += 2; } } if (chong_feng_ji_seat_bt ==
	 * player) { s += 4; player_show_desc_10[player][4] += 4; }
	 * 
	 * if (out_ji_pai_count[player] > 0) player_all_ji_card[player] =
	 * out_ji_pai_count[player];
	 * 
	 * // 已打出的固定鸡牌 for (int j = 0; j < out_ji_pai_count[player]; j++) { int
	 * out_card = out_ji_pai[player][j];
	 * 
	 * if (!is_ji_card(out_card)) continue;
	 * 
	 * // 幺鸡 if (out_card == GameConstants_ZYZJ.YJ_CARD) { int yao_base = 1;
	 * if(jin_ji[0]){ yao_base = 3; player_show_desc_10[player][2] += 3; }else{
	 * yao_base = 1; player_show_desc_10[player][3] += 1; } s += yao_base;
	 * continue; }
	 * 
	 * // 乌骨鸡 if (out_card == GameConstants_ZYZJ.BA_TONG_CARD) { int wu_base =
	 * 2; if(jin_ji[1]){ wu_base = 4; player_show_desc_10[player][5] += 4;
	 * }else{ wu_base = 2; player_show_desc_10[player][6] += 2; } s += wu_base;
	 * continue; }
	 * 
	 * s += 1; player_11_detail[player][3]++; //本鸡 if(ben_ji_card == out_card){
	 * player_show_desc_10[player][7] += 1; } //翻鸡 if(fan_ji_card == out_card){
	 * player_show_desc_10[player][8] += 1; }
	 * 
	 * //摇摆鸡 if (shang_xia_ji[0] == out_card || shang_xia_ji[1] == out_card) {
	 * player_show_desc_10[player][9] += 1; }
	 * 
	 * 
	 * }
	 * 
	 * //if (ting_count > 0) { // 已被打出翻鸡牌 for (int dis = 0; dis <
	 * GRR._discard_count[player]; dis++) { int discard =
	 * GRR._discard_cards[player][dis]; // 星期鸡 if
	 * (has_rule(GameConstants_ZYZJ.GAME_RULE_XQ_ZYZJ) &&
	 * TimeUtil.isSameWeekDay(_logic.get_card_value(discard))) { s += 1;
	 * player_all_ji_card[player]++; player_show_desc_10[player][10] += 1; } //
	 * 本鸡不算打出去的鸡 if (has_rule(GameConstants_ZYZJ.GAME_RULE_BEN_ZYZJ) && discard
	 * == this.ben_ji_card) { continue; }
	 * 
	 * if (discard == GameConstants_ZYZJ.YJ_CARD) continue; if
	 * (has_rule(GameConstants_ZYZJ.GAME_RULE_WG_ZYZJ) && discard ==
	 * GameConstants_ZYZJ.BA_TONG_CARD) continue;
	 * 
	 * if (is_ji_card(discard)) { s += 1; player_all_ji_card[player]++; //本鸡
	 * if(ben_ji_card == discard){ player_show_desc_10[player][7] += 1; } //翻鸡
	 * if(fan_ji_card == discard){ player_show_desc_10[player][8] += 1; } //摇摆鸡
	 * if (shang_xia_ji[0] == discard || shang_xia_ji[1] == discard) {
	 * player_show_desc_10[player][9] += 1; } } }
	 * 
	 * // 叫牌了，算手上的鸡牌了咯 int hand_cards[] = new int[GameConstants.MAX_COUNT];
	 * _logic.switch_to_cards_data(GRR._cards_index[player], hand_cards); for
	 * (int i = 0; i < GameConstants.MAX_COUNT; i++) { int hand_card =
	 * hand_cards[i];
	 * 
	 * // 星期鸡 if (has_rule(GameConstants_ZYZJ.GAME_RULE_XQ_ZYZJ) &&
	 * TimeUtil.isSameWeekDay(_logic.get_card_value(hand_card))) { s += 1;
	 * player_all_ji_card[player] += 1; player_show_desc_10[player][10] += 1; }
	 * 
	 * if (!is_ji_card(hand_card)) continue;
	 * 
	 * player_all_ji_card[player]++;
	 * 
	 * // 幺鸡 if (hand_card == GameConstants_ZYZJ.YJ_CARD) { int yao_base = 1;
	 * if(jin_ji[0]){ yao_base = 3; player_show_desc_10[player][2] += 3; }else{
	 * yao_base = 1; player_show_desc_10[player][3] += 1; } s += yao_base;
	 * continue; }
	 * 
	 * // 乌骨鸡 if (hand_card == GameConstants_ZYZJ.BA_TONG_CARD &&
	 * has_rule(GameConstants_ZYZJ.GAME_RULE_WG_ZYZJ)) { int wu_base = 2;
	 * if(jin_ji[1]){ wu_base = 4; player_show_desc_10[player][5] += 4; }else{
	 * wu_base = 2; player_show_desc_10[player][6] += 2; } s += wu_base;
	 * continue; }
	 * 
	 * 
	 * 
	 * s += 1;
	 * 
	 * //本鸡 if(ben_ji_card == hand_card){ player_show_desc_10[player][7] += 1; }
	 * //翻鸡 if(fan_ji_card == hand_card){ player_show_desc_10[player][8] += 1; }
	 * //摇摆鸡 if (shang_xia_ji[0] == hand_card || shang_xia_ji[1] == hand_card) {
	 * player_show_desc_10[player][9] += 1; }
	 * 
	 * }
	 * 
	 * for (int j = 0; j < GRR._weave_count[player]; j++) { // 星期鸡 if
	 * (has_rule(GameConstants_ZYZJ.GAME_RULE_XQ_ZYZJ) &&
	 * TimeUtil.isSameWeekDay(_logic.get_card_value(GRR._weave_items[player][j].
	 * center_card))) { if (GameConstants_ZYZJ.WIK_PENG ==
	 * GRR._weave_items[player][j].weave_kind) { player_all_ji_card[player] +=
	 * 3; s += 3; player_show_desc_10[player][10] += 3; } else {
	 * player_all_ji_card[player] += 4; s += 4; player_show_desc_10[player][10]
	 * += 4; } }
	 * 
	 * if (!is_ji_card(GRR._weave_items[player][j].center_card)) continue;
	 * 
	 * 
	 * // 乌骨鸡 if (GRR._weave_items[player][j].center_card ==
	 * GameConstants_ZYZJ.BA_TONG_CARD &&
	 * has_rule(GameConstants_ZYZJ.GAME_RULE_WG_ZYZJ)) { continue; } // 幺鸡 if
	 * (GRR._weave_items[player][j].center_card == GameConstants_ZYZJ.YJ_CARD) {
	 * continue; }
	 * 
	 * if (GameConstants_ZYZJ.WIK_PENG ==
	 * GRR._weave_items[player][j].weave_kind) { s += 3;
	 * player_all_ji_card[player] += 3; //本鸡 if(ben_ji_card ==
	 * GRR._weave_items[player][j].center_card){ player_show_desc_10[player][7]
	 * += 3; } //翻鸡 if(fan_ji_card == GRR._weave_items[player][j].center_card){
	 * player_show_desc_10[player][8] += 3; } //摇摆鸡 if (shang_xia_ji[0] ==
	 * GRR._weave_items[player][j].center_card || shang_xia_ji[1] ==
	 * GRR._weave_items[player][j].center_card) { player_show_desc_10[player][9]
	 * += 3; } } else { player_11_detail[player][3] += 4; s += 4;
	 * player_all_ji_card[player] += 4;
	 * 
	 * //翻鸡 if(fan_ji_card == GRR._weave_items[player][j].center_card){
	 * player_show_desc_10[player][8] += 4; } //摇摆鸡 if (shang_xia_ji[0] ==
	 * GRR._weave_items[player][j].center_card || shang_xia_ji[1] ==
	 * GRR._weave_items[player][j].center_card) { player_show_desc_10[player][9]
	 * += 4; } } } //}
	 * 
	 * //没叫嘴的不管了 if(_playerStatus[player]._hu_card_count <= 0 &&
	 * !GRR._chi_hu_rights[player].is_valid()){ continue; }
	 * 
	 * //被烧了就没了 if ((shao[player] && s > 0) || s == 0) continue;
	 * 
	 * //没叫嘴的给叫嘴的 for (int k = 0; k < getTablePlayerNumber(); k++) { if (player
	 * == k) { continue; } if (_playerStatus[k]._hu_card_count > 0 ||
	 * GRR._chi_hu_rights[k].is_valid()) continue;
	 * 
	 * if(bao_seat != -1){ GRR._game_score[bao_seat] -= s;
	 * GRR._game_score[player] += s; }else{ GRR._game_score[k] -= s;
	 * GRR._game_score[player] += s; }
	 * 
	 * }
	 * 
	 * }
	 * 
	 * for (int p = 0; p < getTablePlayerNumber(); p++){ player_ji_score[p] =
	 * (int) GRR._game_score[p]; player_show_desc_10[p][26] = this.ze_ren_ji[p];
	 * GRR._game_score[p] += this.ze_ren_ji[p]; } }
	 */

	public void process_ji_fen(int bao_seat) {
		for (int player = 0; player < getTablePlayerNumber(); player++) {
			int s = 0;
			int ting_count = _playerStatus[player]._hu_card_count;

			// 冲锋鸡
			if (chong_feng_ji_seat_yj == player) {
				if (jin_ji[0]) {
					s += 4;
					player_show_desc_10[player][0] += 4;
				} else {
					s += 2;
					player_show_desc_10[player][1] += 2;
				}
			}
			if (chong_feng_ji_seat_bt == player) {
				s += 4;
				player_show_desc_10[player][4] += 4;
			}

			if (out_ji_pai_count[player] > 0)
				player_all_ji_card[player] = out_ji_pai_count[player];

			// 已打出的固定鸡牌
			for (int j = 0; j < out_ji_pai_count[player]; j++) {
				int out_card = out_ji_pai[player][j];

				if (!is_ji_card(out_card))
					continue;

				// 幺鸡
				if (out_card == GameConstants_ZYZJ.YJ_CARD) {
					int yao_base = 1;
					if (ting_count < 0 && !GRR._chi_hu_rights[player].is_valid()) {
						yao_base = 1;
						player_show_desc_10[player][3] += 1;
					} else {
						if (jin_ji[0]) {
							yao_base = 3;
							player_show_desc_10[player][2] += 3;
						} else {
							yao_base = 1;
							player_show_desc_10[player][3] += 1;
						}
					}
					s += yao_base;
					continue;
				}

				// 乌骨鸡
				if (out_card == GameConstants_ZYZJ.BA_TONG_CARD) {
					int wu_base = 2;
					if (ting_count < 0 && !GRR._chi_hu_rights[player].is_valid()) {
						wu_base = 2;
						player_show_desc_10[player][6] += 2;
					} else {
						if (jin_ji[1]) {
							wu_base = 4;
							player_show_desc_10[player][5] += 4;
						} else {
							wu_base = 2;
							player_show_desc_10[player][6] += 2;
						}
					}
					s += wu_base;
					continue;
				}

				s += 1;

				// 本鸡
				if (ben_ji_card == out_card) {
					player_show_desc_10[player][7] += 1;
				}
				// 翻鸡
				if (fan_ji_card == out_card) {
					player_show_desc_10[player][8] += 1;
				}
				// 摇摆鸡
				if (shang_xia_ji[0] == out_card || shang_xia_ji[1] == out_card) {
					player_show_desc_10[player][9] += 1;
				}
				// 星期鸡
				if (has_rule(GameConstants_ZYZJ.GAME_RULE_XQ_ZYZJ) && TimeUtil.isSameWeekDay(_logic.get_card_value(out_card))) {
					player_show_desc_10[player][10] += 1;
				}
			}

			if (ting_count > 0 || GRR._chi_hu_rights[player].is_valid()) {
				// 已被打出翻鸡牌,不算咯
				/*
				 * for (int dis = 0; dis < GRR._discard_count[player]; dis++) {
				 * int discard = GRR._discard_cards[player][dis]; // 星期鸡 if
				 * (has_rule(GameConstants_ZYZJ.GAME_RULE_XQ_ZYZJ) &&
				 * TimeUtil.isSameWeekDay(_logic.get_card_value(discard))) { s
				 * += 1; player_all_ji_card[player]++;
				 * player_show_desc_10[player][10] += 1; } // 本鸡不算打出去的鸡 if
				 * (has_rule(GameConstants_ZYZJ.GAME_RULE_BEN_ZYZJ) && discard
				 * == this.ben_ji_card) { continue; }
				 * 
				 * if (discard == GameConstants_ZYZJ.YJ_CARD && (discard !=
				 * this.fan_ji_card && discard != this.shang_xia_ji[0] &&
				 * discard != this.shang_xia_ji[1])) continue; if
				 * (has_rule(GameConstants_ZYZJ.GAME_RULE_WG_ZYZJ) && discard ==
				 * GameConstants_ZYZJ.BA_TONG_CARD && (discard !=
				 * this.fan_ji_card && discard != this.shang_xia_ji[0] &&
				 * discard != this.shang_xia_ji[1])) continue;
				 * 
				 * if (is_ji_card(discard)) { s += 1;
				 * player_all_ji_card[player]++; //本鸡 if(ben_ji_card ==
				 * discard){ player_show_desc_10[player][7] += 1; } //翻鸡
				 * if(fan_ji_card == discard){ player_show_desc_10[player][8] +=
				 * 1; } //摇摆鸡 if (shang_xia_ji[0] == discard || shang_xia_ji[1]
				 * == discard) { player_show_desc_10[player][9] += 1; } } }
				 */

				// 叫牌了，算手上的鸡牌了咯
				int hand_cards[] = new int[GameConstants.MAX_COUNT];
				_logic.switch_to_cards_data(GRR._cards_index[player], hand_cards);
				for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
					int hand_card = hand_cards[i];

					// 星期鸡
					if (has_rule(GameConstants_ZYZJ.GAME_RULE_XQ_ZYZJ) && TimeUtil.isSameWeekDay(_logic.get_card_value(hand_card))) {
						s += 1;
						player_all_ji_card[player] += 1;
						player_show_desc_10[player][10] += 1;
					}

					if (!is_ji_card(hand_card))
						continue;

					player_all_ji_card[player]++;

					// 幺鸡
					if (hand_card == GameConstants_ZYZJ.YJ_CARD) {
						int yao_base = 1;
						if (jin_ji[0]) {
							yao_base = 3;
							player_show_desc_10[player][2] += 3;
						} else {
							yao_base = 1;
							player_show_desc_10[player][3] += 1;
						}
						s += yao_base;
						continue;
					}

					// 乌骨鸡
					if (hand_card == GameConstants_ZYZJ.BA_TONG_CARD && has_rule(GameConstants_ZYZJ.GAME_RULE_WG_ZYZJ)) {
						int wu_base = 2;
						if (jin_ji[1]) {
							wu_base = 4;
							player_show_desc_10[player][5] += 4;
						} else {
							wu_base = 2;
							player_show_desc_10[player][6] += 2;
						}
						s += wu_base;
						continue;
					}

					s += 1;

					// 本鸡
					if (ben_ji_card == hand_card) {
						player_show_desc_10[player][7] += 1;
					}
					// 翻鸡
					if (fan_ji_card == hand_card) {
						player_show_desc_10[player][8] += 1;
					}
					// 摇摆鸡
					if (shang_xia_ji[0] == hand_card || shang_xia_ji[1] == hand_card) {
						player_show_desc_10[player][9] += 1;
					}

				}

				for (int j = 0; j < GRR._weave_count[player]; j++) {
					// 星期鸡
					int center_card = GRR._weave_items[player][j].center_card;
					if (has_rule(GameConstants_ZYZJ.GAME_RULE_XQ_ZYZJ) && TimeUtil.isSameWeekDay(_logic.get_card_value(center_card))) {
						if (GameConstants_ZYZJ.WIK_PENG == GRR._weave_items[player][j].weave_kind) {
							player_all_ji_card[player] += 3;
							s += 3;
							player_show_desc_10[player][10] += 3;
						} else {
							player_all_ji_card[player] += 4;
							s += 4;
							player_show_desc_10[player][10] += 4;
						}
					}

					if (!is_ji_card(GRR._weave_items[player][j].center_card))
						continue;

					// 乌骨鸡
					if (center_card == GameConstants_ZYZJ.BA_TONG_CARD && has_rule(GameConstants_ZYZJ.GAME_RULE_WG_ZYZJ)) {
						int wu_base = 2;
						if (jin_ji[1]) {
							wu_base = 4;
							if (GameConstants_ZYZJ.WIK_PENG == GRR._weave_items[player][j].weave_kind) {
								player_show_desc_10[player][5] += 4 * 3;
							} else {
								player_show_desc_10[player][5] += 4 * 5;
							}

						} else {
							wu_base = 2;
							if (GameConstants_ZYZJ.WIK_PENG == GRR._weave_items[player][j].weave_kind) {
								player_show_desc_10[player][6] += 2 * 3;
							} else {
								player_show_desc_10[player][6] += 2 * 5;
							}

						}
						if (GameConstants_ZYZJ.WIK_PENG == GRR._weave_items[player][j].weave_kind) {
							s += wu_base * 3;
						} else {
							s += wu_base * 5;
						}
						continue;
					}
					// 幺鸡
					if (center_card == GameConstants_ZYZJ.YJ_CARD) {
						int yao_base = 1;
						if (jin_ji[0]) {
							yao_base = 3;
							if (GameConstants_ZYZJ.WIK_PENG == GRR._weave_items[player][j].weave_kind) {
								player_show_desc_10[player][2] += 3 * 3;
							} else {
								player_show_desc_10[player][2] += 3 * 5;
							}

						} else {
							yao_base = 1;
							if (GameConstants_ZYZJ.WIK_PENG == GRR._weave_items[player][j].weave_kind) {
								player_show_desc_10[player][3] += 1 * 3;
							} else {
								player_show_desc_10[player][3] += 1 * 5;
							}

						}
						if (GameConstants_ZYZJ.WIK_PENG == GRR._weave_items[player][j].weave_kind) {
							s += yao_base * 3;
						} else {
							s += yao_base * 5;
						}
						continue;

					}

					// 乌骨鸡
					// if (center_card == GameConstants_ZYZJ.BA_TONG_CARD &&
					// has_rule(GameConstants_ZYZJ.GAME_RULE_WG_ZYZJ) &&
					// (center_card != this.fan_ji_card && center_card !=
					// this.shang_xia_ji[0] && center_card !=
					// this.shang_xia_ji[1])) {
					// continue;
					// }
					// 幺鸡
					// if (center_card == GameConstants_ZYZJ.YJ_CARD &&
					// (center_card != this.fan_ji_card
					// && center_card != this.shang_xia_ji[0] && center_card !=
					// this.shang_xia_ji[1])) {
					// continue;
					// }

					if (GameConstants_ZYZJ.WIK_PENG == GRR._weave_items[player][j].weave_kind) {
						s += 3;
						player_all_ji_card[player] += 3;
						// 本鸡
						if (ben_ji_card == center_card) {
							player_show_desc_10[player][7] += 3;
						}
						// 翻鸡
						if (fan_ji_card == center_card) {
							player_show_desc_10[player][8] += 3;
						}
						// 摇摆鸡
						if (shang_xia_ji[0] == center_card || shang_xia_ji[1] == center_card) {
							player_show_desc_10[player][9] += 3;
						}
					} else {

						// 满鸡加1鸡
						s += 5;
						player_all_ji_card[player] += 5;

						// 翻鸡
						if (fan_ji_card == center_card) {
							player_show_desc_10[player][8] += 5;
						}

						// 摇摆鸡
						if (shang_xia_ji[0] == center_card || shang_xia_ji[1] == center_card) {
							player_show_desc_10[player][9] += 5;
						}
					}
				}
			}

			// 叫嘴了，又被烧了，就不算了
			if ((shao[player] && _playerStatus[player]._hu_card_count > 0)){
				player_show_desc_10[player][0] = 0;
				player_show_desc_10[player][1] = 0;
				player_show_desc_10[player][2] = 0;
				player_show_desc_10[player][3] = 0;
				player_show_desc_10[player][4] = 0;
				player_show_desc_10[player][5] = 0;
				player_show_desc_10[player][6] = 0;
				player_show_desc_10[player][7] = 0;
				player_show_desc_10[player][8] = 0;
				player_show_desc_10[player][9] = 0;
				player_show_desc_10[player][10] = 0;
				continue;
			}

			// 未叫嘴的减去星期鸡、摇摆鸡、本鸡、翻鸡,并且把自己的鸡分给别人
			if (_playerStatus[player]._hu_card_count <= 0 && !GRR._chi_hu_rights[player].is_valid()) {
				s -= player_show_desc_10[player][7];
				s -= player_show_desc_10[player][8];
				s -= player_show_desc_10[player][9];
				s -= player_show_desc_10[player][10];
				player_show_desc_10[player][7] = 0;
				player_show_desc_10[player][8] = 0;
				player_show_desc_10[player][9] = 0;
				player_show_desc_10[player][10] = 0;
				for (int k = 0; k < getTablePlayerNumber(); k++) {
					if (player == k)
						continue;
					// if (_playerStatus[k]._hu_card_count <= 0 &&
					// !GRR._chi_hu_rights[k].is_valid())
					// continue;
					GRR._game_score[k] += s;
					GRR._game_score[player] -= s;
				}
			}
			// 叫了嘴的收未叫嘴的
			else if (_playerStatus[player]._hu_card_count > 0 || GRR._chi_hu_rights[player].is_valid()) {
				for (int k = 0; k < getTablePlayerNumber(); k++) {
					if (player == k)
						continue;
					// if (_playerStatus[k]._hu_card_count > 0 ||
					// GRR._chi_hu_rights[k].is_valid())
					// continue;
					GRR._game_score[k] -= s;
					GRR._game_score[player] += s;
				}
			}
		}

		for (int p = 0; p < getTablePlayerNumber(); p++) {
			player_ji_score[p] = (int) GRR._game_score[p];

			// 添加责任鸡
			player_show_desc_10[p][26] = this.ze_ren_ji[p];
			GRR._game_score[p] += this.ze_ren_ji[p];
		}
	}

	/**
	 * 责任鸡算分
	 */
	public void process_reponsibility_ji_fen() {
		for (int count = 0; count < 2; count++) {
			Tuple<Integer, Integer> responsibility = responsibility_ji[count];

			if (responsibility.getLeft() == -1 || responsibility.getRight() == -1)
				continue;

			int ting_left = _playerStatus[responsibility.getLeft()]._hu_card_count;
			int ting_right = _playerStatus[responsibility.getRight()]._hu_card_count;

			Tuple<Integer, Integer> check_ting = new Tuple<>(0, 0);
			if (ting_left > 0)
				check_ting.setLeft(1);
			if (ting_right > 0)
				check_ting.setRight(1);

			if (check_ting.getLeft() == 0 && check_ting.getRight() == 0)
				continue;

			int base = count == 0 ? 1 : 2;
			if (check_ting.getRight() >= check_ting.getLeft()) {
				player_show_desc_10[responsibility_ji[count].getLeft()][7] -= base;
				player_show_desc_10[responsibility_ji[count].getRight()][7] += base;
				GRR._game_score[responsibility_ji[count].getLeft()] -= base;
				GRR._game_score[responsibility_ji[count].getRight()] += base;
			} else {
				player_show_desc_10[responsibility_ji[count].getLeft()][7] += base;
				player_show_desc_10[responsibility_ji[count].getRight()][7] -= base;
				GRR._game_score[responsibility_ji[count].getLeft()] += base;
				GRR._game_score[responsibility_ji[count].getRight()] -= base;
			}

			show_responsibility_ji[count].setLeft(responsibility.getLeft());
			show_responsibility_ji[count].setRight(responsibility.getRight());
			// 算了一次就请掉哈，不要重复计算撤
			responsibility.setLeft(-1);
			responsibility.setRight(-1);
		}

		for (int p = 0; p < getTablePlayerNumber(); p++)
			player_ji_score[p] = (int) GRR._game_score[p];
	}

	/**
	 * 初始化plyaer_11_detail 的第十一位
	 */
	private void init_11_player_detail() {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			ChiHuRight chr = GRR._chi_hu_rights[i];

			if (GRR._chi_hu_rights[i].is_valid()) {
				if (!chr.opr_and(GameConstants_ZYZJ.CHR_ZI_MO).is_empty())
					player_11_detail[i][10] = 1;
				if (!chr.opr_and(GameConstants_ZYZJ.CHR_SHU_FAN).is_empty())
					player_11_detail[i][10] = 2;
				if (!chr.opr_and(GameConstants_ZYZJ.CHR_QING_GANG_HU).is_empty())
					player_11_detail[i][10] = 3;
				if (!chr.opr_and(GameConstants_ZYZJ.CHR_GNAG_KAI).is_empty())
					player_11_detail[i][10] = 4;
				if (!chr.opr_and(GameConstants_ZYZJ.CHR_RE_PAO).is_empty())
					player_11_detail[i][10] = 6;
			} else {
				if (!chr.opr_and(GameConstants_ZYZJ.CHR_FANG_PAO).is_empty())
					player_11_detail[i][10] = 5;
			}
		}
	}

	/*
	 * @Override public void process_chi_hu_player_score(int seat_index, int
	 * provide_index, int operate_card, boolean zimo) {
	 * GRR._chi_hu_card[seat_index][0] = operate_card;
	 * 
	 * GRR._win_order[seat_index] = 1; // 引用权位 ChiHuRight chr =
	 * GRR._chi_hu_rights[seat_index];
	 * 
	 * int wFanShu = 1;// 番数 countCardType(chr, seat_index);
	 * 
	 * ///////////////////////////////////////////////
	 * 算分////////////////////////// // 统计 if (zimo) { // 自摸 for (int i = 0; i <
	 * getTablePlayerNumber(); i++) { if (i == seat_index) { continue; }
	 * GRR._lost_fan_shu[i][seat_index] = wFanShu;
	 * 
	 * if (this._player_result.pao[i] == 2) { player_show_desc_10[i][16] = -3; }
	 * if (this._player_result.pao[seat_index] == 2) {
	 * player_show_desc_10[seat_index][16] = 3; } } show_continue_banker = true;
	 * } else {// 点炮 GRR._lost_fan_shu[provide_index][seat_index] = wFanShu;
	 * show_continue_banker = false;
	 * 
	 * if (this._player_result.pao[provide_index] == 2) {
	 * player_show_desc_10[provide_index][16] = -3; } if
	 * (this._player_result.pao[seat_index] == 2) {
	 * player_show_desc_10[seat_index][16] = 3; } }
	 * 
	 * float lChiHuScore = get_hu_fen(chr);
	 * ////////////////////////////////////////////////////// 自摸 算分 if (zimo) {
	 * chr.opr_or(GameConstants.CHR_ZI_MO); int add_fen = 0; add_fen =
	 * has_rule(GameConstants_ZYZJ.GAME_RULE_ZIMO1_ZYZJ) ? 1 : 0; add_fen =
	 * has_rule(GameConstants_ZYZJ.GAME_RULE_ZIMO2_ZYZJ) ? 1 : 0; add_fen =
	 * has_rule(GameConstants_ZYZJ.GAME_RULE_ZIMO_FAN_ZYZJ) ? (int) lChiHuScore
	 * : 0; player_show_desc_10[seat_index][15] += add_fen; float s =
	 * lChiHuScore + add_fen; hu_base_fen = s;
	 * 
	 * // 天听软报额外收取10分 if (_playerStatus[seat_index].is_bao_ting()) { s += 10;
	 * player_show_desc_10[seat_index][18] += 10; }
	 * 
	 * // 杠开多加10分 if (!chr.opr_and(GameConstants_ZYZJ.CHR_GNAG_KAI).is_empty())
	 * { s += 10; }
	 * 
	 * // 天胡算硬报，20分 if (!chr.opr_and(GameConstants_ZYZJ.CHR_TIAN_HU).is_empty())
	 * { s += 10; } else if (player_mo_first[seat_index]) {
	 * chr.opr_or(GameConstants_ZYZJ.CHR_TIAN_HU); s += 10; }
	 * 
	 * // 原缺+2 if (this.yuan_que_player[seat_index] == 1) { s += 2;
	 * player_show_desc_10[seat_index][17] += 2; }
	 * 
	 * //买跑 if(_player_result.pao[seat_index] == 2){ s += 3;
	 * player_show_desc_10[seat_index][16] += 3; }
	 * 
	 * // 连庄玩法 加连庄数 if (has_rule(GameConstants_ZYZJ.GAME_RULE_LIANZHUANG_ZYZJ))
	 * { if (old_banker == seat_index ) { if (continue_banker_count > 0) {
	 * player_show_desc_10[seat_index][14] += continue_banker_count; }
	 * continue_banker_count++; }else{ continue_banker_count = 0; } }
	 * 
	 * for (int i = 0; i < getTablePlayerNumber(); i++) { if (i == seat_index) {
	 * continue; } float ss = s;
	 * 
	 * if(_player_result.pao[i] == 2){ ss += 3; player_show_desc_10[i][16] += 3;
	 * }
	 * 
	 * // 添加动作) GRR._game_score[i] -= ss; hu_type_socre[i] -= ss;
	 * GRR._game_score[seat_index] += ss; hu_type_socre[seat_index] += ss;
	 * 
	 * // 连庄玩法 加连庄数 if (has_rule(GameConstants_ZYZJ.GAME_RULE_LIANZHUANG_ZYZJ))
	 * { if (old_banker != _cur_banker && i == old_banker) { GRR._game_score[i]
	 * -= continue_banker_count; hu_type_socre[i] -= continue_banker_count;
	 * GRR._game_score[seat_index] += continue_banker_count;
	 * hu_type_socre[seat_index] += continue_banker_count; if
	 * (continue_banker_count > 0) { player_show_desc_10[i][14] +=
	 * continue_banker_count; } continue_banker_count = 0; } } } }
	 * ////////////////////////////////////////////////////// 点炮 算分 else {
	 * chr.opr_or(GameConstants.CHR_SHU_FAN);
	 * GRR._chi_hu_rights[provide_index].opr_or(GameConstants.CHR_FANG_PAO);
	 * 
	 * int add_fen = 0;
	 * 
	 * float s = lChiHuScore + add_fen; hu_base_fen = s;
	 * 
	 * // 报听额外收取10分 if (_playerStatus[seat_index].is_bao_ting()) { s += 10;
	 * player_show_desc_10[seat_index][18] += 10; }
	 * 
	 * // 杀报 if (_playerStatus[provide_index].is_bao_ting()) { s += 10;
	 * player_show_desc_10[seat_index][19] += 10; } //买跑
	 * if(_player_result.pao[seat_index] == 2){ s += 3;
	 * player_show_desc_10[seat_index][16] += 3; }
	 * if(_player_result.pao[provide_index] == 2){ s += 3;
	 * player_show_desc_10[provide_index][16] += 3; }
	 * 
	 * // 连庄玩法 加连庄数 if (has_rule(GameConstants_ZYZJ.GAME_RULE_LIANZHUANG_ZYZJ))
	 * { if (seat_index == old_banker) { s += continue_banker_count; if
	 * (continue_banker_count > 0){ player_show_desc_10[seat_index][14] +=
	 * continue_banker_count; } continue_banker_count++; } if (provide_index ==
	 * old_banker) { s += continue_banker_count; if (continue_banker_count > 0){
	 * player_show_desc_10[provide_index][14] += continue_banker_count; }
	 * continue_banker_count = 0; } }
	 * 
	 * // 地胡 硬报加10分 if
	 * (!GRR._chi_hu_rights[seat_index].opr_and(GameConstants_ZYZJ.CHR_DI_HU).
	 * is_empty()) s += 10;
	 * 
	 * // 抢杠胡包胡 if (!GRR._chi_hu_rights[seat_index].opr_and(GameConstants_ZYZJ.
	 * CHR_QING_GANG_HU).is_empty()) { s *= 3;
	 * chr.opr_or(GameConstants_ZYZJ.CHR_QING_GANG_HU); }
	 * 
	 * // 原缺+2 if (this.yuan_que_player[seat_index] == 1) { s += 2; }
	 * 
	 * // 胡牌分 GRR._game_score[provide_index] -= s; hu_type_socre[provide_index]
	 * -= s; GRR._game_score[seat_index] += s; hu_type_socre[seat_index] += s; }
	 * 
	 * for (int i = 0; i < getTablePlayerNumber(); i++) { if (i == _cur_banker)
	 * { _player_result.qiang[_cur_banker] = continue_banker_count; } else {
	 * _player_result.qiang[i] = 0; } }
	 * 
	 * // 设置变量 GRR._provider[seat_index] = provide_index; _status_gang = false;
	 * _status_gang_hou_pao = false;
	 * 
	 * change_player_status(seat_index, GameConstants.INVALID_VALUE);
	 * 
	 * init_11_player_detail();
	 * 
	 * search_cha_que(); }
	 */

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int wFanShu = 1;// 番数
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
				int pao_score = 0;
				if (this._player_result.pao[i] == 2) {
					pao_score += 1;
				} else if (this._player_result.pao[i] == 3) {
					pao_score += 3;
				}

				if (this._player_result.pao[seat_index] == 2) {
					pao_score += 1;
				} else if (this._player_result.pao[seat_index] == 3) {
					pao_score += 3;
				}
				player_show_desc_10[i][16] -= pao_score;
				player_show_desc_10[seat_index][16] += pao_score;
			}
			show_continue_banker = true;
		} else {// 点炮
			GRR._lost_fan_shu[provide_index][seat_index] = wFanShu;
			show_continue_banker = false;

			int pao_score = 0;
			if (this._player_result.pao[provide_index] == 2) {
				pao_score += 1;
			} else if (this._player_result.pao[provide_index] == 3) {
				pao_score += 3;
			}
			if (this._player_result.pao[seat_index] == 2) {
				pao_score += 1;
			} else if (this._player_result.pao[seat_index] == 3) {
				pao_score += 3;
			}
			player_show_desc_10[provide_index][16] -= pao_score;
			player_show_desc_10[seat_index][16] += pao_score;
		}

		float lChiHuScore = get_hu_fen(chr);
		////////////////////////////////////////////////////// 自摸 算分
		if (zimo) {
			int mo = 0;
			int yuan = 0;
			int baoting = 0;
			int lianzhuang = 0;
			int tianhu = 0;
			int di_hu = 0;
			int gangshanghua = 0;
			chr.opr_or(GameConstants.CHR_ZI_MO);
			if (has_rule(GameConstants_ZYZJ.GAME_RULE_ZIMO1_ZYZJ)) {
				mo = 1;
			} else if (has_rule(GameConstants_ZYZJ.GAME_RULE_ZIMO2_ZYZJ)) {
				mo = 2;
			} else if (has_rule(GameConstants_ZYZJ.GAME_RULE_ZIMO_FAN_ZYZJ)) {
				mo = (int) lChiHuScore;
			}

			float s = lChiHuScore + mo;
			hu_base_fen = s;

			// 杠开多加10分
			if (!chr.opr_and(GameConstants_ZYZJ.CHR_GNAG_KAI).is_empty()) {
				s += 10;
				gangshanghua = 10;
			}

			// 天胡，10分
			if (!chr.opr_and(GameConstants_ZYZJ.CHR_TIAN_HU).is_empty()) {
				s += 10;
				tianhu = 10;
			}

			// 地胡，10分
			if (!chr.opr_and(GameConstants_ZYZJ.CHR_DI_HU).is_empty()) {
				s += 10;
				di_hu = 10;
			}

			// 报听10分
			if (_playerStatus[seat_index].is_bao_ting()) {
				s += 10;
				baoting = 10;
			}

			// 原缺+2
			//if (this.yuan_que_player[seat_index] == 1) {
			//	s += 2;
			//	yuan = 2;
			//}

			// 买跑
			if (_player_result.pao[seat_index] == 2) {
				s += 1;
			} else if (_player_result.pao[seat_index] == 3) {
				s += 3;
			}

			// 连庄加分，庄家是赢家
			if (has_rule(GameConstants_ZYZJ.GAME_RULE_LIANZHUANG_ZYZJ)) {
				if (old_banker == seat_index) {
					if (continue_banker_count > 0) {
						s += continue_banker_count;
						lianzhuang = continue_banker_count;
					}
					continue_banker_count++;
				}
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				int shabao = 0;
				if (i == seat_index) {
					continue;
				}
				float ss = s;

				if (_player_result.pao[i] == 2) {
					ss += 1;
				} else if (_player_result.pao[i] == 3) {
					ss += 3;
				}

				// 杀报多收10分
				if (_playerStatus[i].is_bao_ting()) {
					ss += 10;
					shabao = 10;
				}

				// 连庄加分，庄家不是赢家
				if (has_rule(GameConstants_ZYZJ.GAME_RULE_LIANZHUANG_ZYZJ)) {
					if (i == old_banker) {
						if (continue_banker_count > 0) {
							GRR._game_score[i] -= continue_banker_count - 1;
							GRR._game_score[seat_index] += continue_banker_count - 1;
							player_show_desc_10[i][14] -= continue_banker_count - 1;
							player_show_desc_10[seat_index][14] += continue_banker_count - 1;
						}
						continue_banker_count = 0;
					}
				}

				// 添加动作
				GRR._game_score[i] -= ss;
				hu_type_socre[i] -= ss;
				GRR._game_score[seat_index] += ss;
				hu_type_socre[seat_index] += ss;

				// 连庄加分
				player_show_desc_10[i][14] -= lianzhuang;
				player_show_desc_10[seat_index][14] += lianzhuang;

				// 自摸加分
				player_show_desc_10[i][15] -= mo;
				player_show_desc_10[seat_index][15] += mo;

				// 原缺加分
				//player_show_desc_10[i][17] -= yuan;
				//player_show_desc_10[seat_index][17] += yuan;

				// 报听加分
				player_show_desc_10[i][18] -= baoting;
				player_show_desc_10[seat_index][18] += baoting;

				// 杀报加分
				player_show_desc_10[i][19] -= shabao;
				player_show_desc_10[seat_index][19] += shabao;

				// 胡牌分
				player_show_desc_10[i][20] -= lChiHuScore;
				player_show_desc_10[seat_index][20] += lChiHuScore;

				// 天胡
				player_show_desc_10[i][21] -= tianhu;
				player_show_desc_10[seat_index][21] += tianhu;

				// 地胡
				player_show_desc_10[i][22] -= di_hu;
				player_show_desc_10[seat_index][22] += di_hu;

				// 杠上花
				player_show_desc_10[i][23] -= gangshanghua;
				player_show_desc_10[seat_index][23] += gangshanghua;

			}
		}
		////////////////////////////////////////////////////// 点炮 算分
		else {
			int yuan = 0;
			int shabao = 0;
			int baoting = 0;
			int lianzhuang = 0;
			int dihu = 0;
			int qiangganghu = 0;

			chr.opr_or(GameConstants.CHR_SHU_FAN);
			GRR._chi_hu_rights[provide_index].opr_or(GameConstants.CHR_FANG_PAO);

			float s = lChiHuScore;
			hu_base_fen = s;

			// 报听
			if (_playerStatus[seat_index].is_bao_ting()) {
				s += 10;
				baoting = 10;
			}
			// 杀报
			if (_playerStatus[provide_index].is_bao_ting()) {
				s += 10;
				shabao = 10;
			}
			// 买跑
			if (_player_result.pao[seat_index] == 2) {
				s += 1;
			} else if (_player_result.pao[seat_index] == 3) {
				s += 3;
			}
			if (_player_result.pao[provide_index] == 2) {
				s += 1;
			} else if (_player_result.pao[provide_index] == 3) {
				s += 3;
			}

			// 连庄玩法 加连庄数
			if (has_rule(GameConstants_ZYZJ.GAME_RULE_LIANZHUANG_ZYZJ)) {
				if (seat_index == old_banker) {
					s += continue_banker_count;
					lianzhuang = continue_banker_count;
					continue_banker_count++;
				}
				if (provide_index == old_banker) {
					if (continue_banker_count > 0) {
						s += continue_banker_count - 1;
						lianzhuang = continue_banker_count - 1;
					}
					// continue_banker_count = 0;
				}
			}

			// 地胡 硬报加10分
			if (!GRR._chi_hu_rights[seat_index].opr_and(GameConstants_ZYZJ.CHR_DI_HU).is_empty()) {
				s += 10;
				dihu = 10;
			}

			// 抢杠胡包胡
			if (!GRR._chi_hu_rights[seat_index].opr_and(GameConstants_ZYZJ.CHR_QING_GANG_HU).is_empty()) {
				qiangganghu = (int) lChiHuScore * (this.getTablePlayerNumber() - 2);
				s += qiangganghu;
				// chr.opr_or(GameConstants_ZYZJ.CHR_QING_GANG_HU);
			}

			// 原缺+2
			//if (this.yuan_que_player[seat_index] == 1) {
			//	s += 2;
			//	yuan = 2;
			//}

			// 胡牌分
			GRR._game_score[provide_index] -= s;
			hu_type_socre[provide_index] -= s;
			GRR._game_score[seat_index] += s;
			hu_type_socre[seat_index] += s;

			// 连庄加分
			player_show_desc_10[provide_index][14] -= lianzhuang;
			player_show_desc_10[seat_index][14] += lianzhuang;

			// 原缺加分
			//player_show_desc_10[provide_index][17] -= yuan;
			//player_show_desc_10[seat_index][17] += yuan;

			// 报听加分
			player_show_desc_10[provide_index][18] -= baoting;
			player_show_desc_10[seat_index][18] += baoting;

			// 杀报加分
			player_show_desc_10[provide_index][19] -= shabao;
			player_show_desc_10[seat_index][19] += shabao;

			// 胡牌分
			player_show_desc_10[provide_index][20] -= lChiHuScore;
			player_show_desc_10[seat_index][20] += lChiHuScore;

			// 地胡
			player_show_desc_10[provide_index][22] -= dihu;
			player_show_desc_10[seat_index][22] += dihu;

			// 抢杠胡
			player_show_desc_10[provide_index][24] -= qiangganghu;
			player_show_desc_10[seat_index][24] += qiangganghu;

			// 其他人报听了也要出杀报分
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (provide_index == i || i == seat_index || GRR._chi_hu_rights[i].is_valid() == true) {
					continue;
				}
				if (_playerStatus[i].is_bao_ting()) {
					player_show_desc_10[i][19] -= 10;
					player_show_desc_10[seat_index][19] += 10;
					GRR._game_score[i] -= 10;
					hu_type_socre[i] -= 10;
					GRR._game_score[seat_index] += 10;
					hu_type_socre[seat_index] += 10;
				}

			}
			
			// 其他人没有原缺的要出分
			/*if (this.yuan_que_player[seat_index] == 1){
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (provide_index == i || i == seat_index || this.yuan_que_player[i] == 1) {
						continue;
					}
					player_show_desc_10[i][17] -= 2;
					player_show_desc_10[seat_index][17] += 2;
					GRR._game_score[i] -= 2;
					hu_type_socre[i] -= 2;
					GRR._game_score[seat_index] += 2;
					hu_type_socre[seat_index] += 2;
				}
			}*/

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

		init_11_player_detail();

		search_cha_que();
	}

	/**
	 * 
	 * @param seat_index
	 * @param card
	 *            固定的鸟
	 * @param show
	 * @param add_niao
	 *            额外奖鸟
	 * @param isTongPao
	 *            --通炮不抓飞鸟
	 */
	public void set_niao_card(int seat_index, int card, boolean show, int add_niao, int hu_card) {

		if (true)
			return;
		// for (int i = 0; i < GameConstants.MAX_NIAO_CARD; i++) {
		// GRR._cards_data_niao[i] = GameConstants.INVALID_VALUE;
		// }
		//
		// for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
		// GRR._player_niao_count[i] = 0;
		// for (int j = 0; j < GameConstants.MAX_NIAO_CARD; j++) {
		// GRR._player_niao_cards[i][j] = GameConstants.INVALID_VALUE;
		// }
		// }
		//
		// GRR._show_bird_effect = show;
		// GRR._count_niao = getCsDingNiaoNum();
		//
		// if (GRR._count_niao > GameConstants.ZHANIAO_0) {
		// if (card == GameConstants.INVALID_VALUE) {
		// int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		// _logic.switch_to_cards_index(_repertory_card, _all_card_len -
		// GRR._left_card_count, GRR._count_niao, cbCardIndexTemp);
		// GRR._left_card_count -= GRR._count_niao;
		// _logic.switch_to_cards_data(cbCardIndexTemp, GRR._cards_data_niao);
		// } else {
		// for (int i = 0; i < GRR._count_niao; i++) {
		// GRR._cards_data_niao[i] = card;
		// }
		// }
		// }
		// // 中鸟个数
		// GRR._count_pick_niao =
		// _logic.get_pick_niao_count(GRR._cards_data_niao, GRR._count_niao);
		//
		// for (int i = 0; i < GRR._count_niao; i++) {
		// int nValue = _logic.get_card_value(GRR._cards_data_niao[i]);
		// int seat = 0;
		// seat = (seat_index + (nValue - 1) % 4) % 4;
		// GRR._player_niao_cards[seat][GRR._player_niao_count[seat]] =
		// GRR._cards_data_niao[i];
		// GRR._player_niao_count[seat]++;
		// }
		//
		// // GRR._count_pick_niao = 0;
		// // for (int i = 0; i < getTablePlayerNumber(); i++) {
		// // for (int j = 0; j < GRR._player_niao_count[i]; j++) {
		// // if (seat_index == i) {
		// // GRR._count_pick_niao++;
		// // GRR._player_niao_cards[i][j] =
		// // this.set_ding_niao_valid(GRR._player_niao_cards[i][j], true);//
		// // 胡牌的鸟生效
		// // } else {
		// // GRR._player_niao_cards[i][j] =
		// // this.set_ding_niao_valid(GRR._player_niao_cards[i][j], false);//
		// // 胡牌的鸟生效
		// // }
		// // }
		// // }
		//
		// int[] player_niao_count = new int[GameConstants.GAME_PLAYER];
		// int[][] player_niao_cards = new
		// int[GameConstants.GAME_PLAYER][GameConstants.MAX_NIAO_CARD];
		// for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
		// player_niao_count[i] = 0;
		// for (int j = 0; j < GameConstants.MAX_NIAO_CARD; j++) {
		// player_niao_cards[i][j] = GameConstants.INVALID_VALUE;
		// }
		// }
		//
		// GRR._count_pick_niao = 0;
		// if (has_rule(GameConstants_HanShouWang.GAME_RULE_HU_JI_JIANG_JI)) {
		// player_niao_cards[seat_index][player_niao_count[seat_index]] =
		// this.set_ding_niao_valid(hu_card, true);
		// player_niao_count[seat_index]++;
		// if (hu_card == GameConstants_HanShouWang.HZ_MAGIC_CARD) {
		// GRR._count_pick_niao = 10;
		// } else {
		// GRR._count_pick_niao = _logic.get_card_value(hu_card);
		// }
		// } else {
		// for (int i = 0; i < getTablePlayerNumber(); i++) {
		// for (int j = 0; j < GRR._player_niao_count[i]; j++) {
		// if (seat_index == i) {
		// GRR._count_pick_niao++;
		// player_niao_cards[seat_index][player_niao_count[seat_index]] =
		// this.set_ding_niao_valid(GRR._player_niao_cards[i][j], true);//
		// 胡牌的鸟生效
		// } else {
		// player_niao_cards[seat_index][player_niao_count[seat_index]] =
		// this.set_ding_niao_valid(GRR._player_niao_cards[i][j], false);//
		// 胡牌的鸟生效
		// }
		// player_niao_count[seat_index]++;
		// }
		// }
		// }
		// GRR._player_niao_cards = player_niao_cards;
		// GRR._player_niao_count = player_niao_count;
	}

	/**
	 * 抓鸟 个数
	 * 
	 * @return
	 */
	public int getCsDingNiaoNum() {
		int nNum = GameConstants.ZHANIAO_0;

		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants_HanShouWang.GAME_RULE_NOT_ZHUA_NIAO))
			return GameConstants.ZHANIAO_0;
		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants_HanShouWang.GAME_RULE_ZHUA_NIAO_2))
			return GameConstants.ZHANIAO_2;
		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants_HanShouWang.GAME_RULE_ZHUA_NIAO_4))
			return GameConstants.ZHANIAO_4;
		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants_HanShouWang.GAME_RULE_ZHUA_NIAO_6))
			return GameConstants.ZHANIAO_6;

		return nNum;
	}

	/*
	 * @Override protected void set_result_describe() { if (GRR == null) GRR =
	 * game_end_GRR;
	 * 
	 * int chrTypes; long type = 0; for (int player = 0; player <
	 * this.getTablePlayerNumber(); player++) { StringBuilder result = new
	 * StringBuilder("");
	 * 
	 * if (_playerStatus[player]._hu_card_count > 0) {
	 * if(!GRR._chi_hu_rights[player].is_valid()){ result.append("已叫牌"); } }
	 * else { result.append("未叫牌"); } chrTypes =
	 * GRR._chi_hu_rights[player].type_count;
	 * 
	 * boolean ping_hu = true; boolean have_qing = false;
	 * 
	 * 
	 * 
	 * for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) { type =
	 * GRR._chi_hu_rights[player].type_list[typeIndex];
	 * 
	 * if (GRR._chi_hu_rights[player].is_valid()) { if (type ==
	 * GameConstants_ZYZJ.CHR_QING_LONG_BEI) { result.append("清龙背 "); have_qing
	 * = true; ping_hu = false; } if (type ==
	 * GameConstants_ZYZJ.CHR_QING_QI_DUI) { result.append("清七对 "); have_qing =
	 * true; ping_hu = false; } if (type == GameConstants_ZYZJ.CHR_LONG_QI_DUI)
	 * { result.append("龙七对 "); ping_hu = false; } if (type ==
	 * GameConstants_ZYZJ.CHR_QING_DAN) { result.append("清单吊 "); ping_hu =
	 * false; } if (type == GameConstants_ZYZJ.CHR_QING_DA_DUI) {
	 * result.append("清大对 "); ping_hu = false; } if (type ==
	 * GameConstants_ZYZJ.CHR_DAN_DIAO) { result.append("单吊 "); ping_hu = false;
	 * } if (type == GameConstants_ZYZJ.CHR_XIAO_QI_DUI) {
	 * result.append("七小对 "); ping_hu = false; } if (type ==
	 * GameConstants_ZYZJ.CHR_QING_YI_SE && !have_qing) { result.append("清一色 ");
	 * ping_hu = false; } if (type == GameConstants_ZYZJ.CHR_DA_DUI_ZI) {
	 * result.append("大对子 "); ping_hu = false; } if (type ==
	 * GameConstants_ZYZJ.CHR_KA_DIAN) { result.append("边卡吊 "); ping_hu = false;
	 * } if (type == GameConstants_ZYZJ.CHR_DA_KUAN_ZHNAG) {
	 * result.append("大宽张 "); ping_hu = false; } if (ping_hu)
	 * result.append("平胡 "); } }
	 * 
	 * // 传给客户端数据格式：平胡13 清一色13@鸡牌 +1_乌骨鸡 +3 GRR._result_des[player] =
	 * result.toString() + " " + GRR._result_des[player]; } }
	 */

	@Override
	protected void set_result_describe() {
		if (GRR == null)
			GRR = game_end_GRR;

		int chrTypes;
		long type = 0;
		for (int player = 0; player < this.getTablePlayerNumber(); player++) {
			StringBuilder result = new StringBuilder("");

			if (_playerStatus[player]._hu_card_count > 0) {
				if (!GRR._chi_hu_rights[player].is_valid()) {
					result.append("已叫牌");
				}
			} else {
				result.append("未叫牌");
			}
			chrTypes = GRR._chi_hu_rights[player].type_count;

			boolean ping_hu = true;

			if (GRR._chi_hu_rights[player].is_valid()) {
				for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
					type = GRR._chi_hu_rights[player].type_list[typeIndex];
					if (type == GameConstants_ZYZJ.CHR_QING_LONG_BEI) {
						ping_hu = false;
						result.append("清龙背 ");
						break;
					}
				}
				for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
					type = GRR._chi_hu_rights[player].type_list[typeIndex];
					if (type == GameConstants_ZYZJ.CHR_QING_QI_DUI) {
						ping_hu = false;
						result.append("清七对 ");
						break;
					}
				}
				for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
					type = GRR._chi_hu_rights[player].type_list[typeIndex];
					if (type == GameConstants_ZYZJ.CHR_LONG_QI_DUI) {
						ping_hu = false;
						result.append("龙七对 ");
						break;
					}
				}
				for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
					type = GRR._chi_hu_rights[player].type_list[typeIndex];
					if (type == GameConstants_ZYZJ.CHR_QING_DAN) {
						ping_hu = false;
						result.append("清单吊 ");
						break;
					}
				}
				for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
					type = GRR._chi_hu_rights[player].type_list[typeIndex];
					if (type == GameConstants_ZYZJ.CHR_QING_DA_DUI) {
						ping_hu = false;
						result.append("清大对 ");
						break;
					}
				}
				for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
					type = GRR._chi_hu_rights[player].type_list[typeIndex];
					if (type == GameConstants_ZYZJ.CHR_DAN_DIAO) {
						ping_hu = false;
						result.append("单吊吊 ");
						break;
					}
				}
				for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
					type = GRR._chi_hu_rights[player].type_list[typeIndex];
					if (type == GameConstants_ZYZJ.CHR_QING_YI_SE) {
						ping_hu = false;
						result.append("清一色 ");
						break;
					}
				}
				for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
					type = GRR._chi_hu_rights[player].type_list[typeIndex];
					if (type == GameConstants_ZYZJ.CHR_XIAO_QI_DUI) {
						ping_hu = false;
						result.append("七小对 ");
						break;
					}
				}
				for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
					type = GRR._chi_hu_rights[player].type_list[typeIndex];
					if (type == GameConstants_ZYZJ.CHR_DA_DUI_ZI) {
						ping_hu = false;
						result.append("大对子 ");
						break;
					}
				}

				for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
					type = GRR._chi_hu_rights[player].type_list[typeIndex];
					if (type == GameConstants_ZYZJ.CHR_KA_DIAN) {
						ping_hu = false;
						result.append("边卡吊 ");
						break;
					}
				}
				for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
					type = GRR._chi_hu_rights[player].type_list[typeIndex];
					if (type == GameConstants_ZYZJ.CHR_DA_KUAN_ZHNAG) {
						ping_hu = false;
						result.append("大宽张 ");
						break;
					}
				}
				if (ping_hu) {
					result.append("平胡 ");
				}
			}

			// 传给客户端数据格式：平胡13 清一色13@鸡牌 +1_乌骨鸡 +3
			GRR._result_des[player] = result.toString() + " " + GRR._result_des[player];
		}
	}

	/**
	 * 处理吃胡的玩家
	 * 
	 * @param seat_index
	 * @param operate_card
	 * @param rm
	 */
	@Override
	public void process_chi_hu_player_operate(int seat_index, int operate_card, boolean rm) {
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		// filtrate_right(seat_index, chr);

		if (is_mj_type(GameConstants.GAME_TYPE_ZZ) || is_mj_type(GameConstants.GAME_TYPE_HZ) || is_mj_type(GameConstants.GAME_TYPE_FLS_HZ_LX)
				|| is_mj_type(GameConstants.GAME_TYPE_HENAN_ZHUAN_ZHUAN)) { // liuyan
																			// 2017/7/10
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
			this.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, effect_count, effect_indexs, 1, GameConstants.INVALID_SEAT);
		} else {
			// 效果
			this.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, chr.type_count, chr.type_list, 1, GameConstants.INVALID_SEAT);
		}

		// 手牌删掉
		this.operate_player_cards(seat_index, 0, null, 0, null);

		if (rm) {
			// 把摸的牌从手牌删掉,结算的时候不显示这张牌的
			GRR._cards_index[seat_index][_logic.switch_to_card_index(operate_card)]--;
		}

		// 显示胡牌
		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], cards);
		cards[hand_card_count] = operate_card + GameConstants.CARD_ESPECIAL_TYPE_HU;
		hand_card_count++;
		this.operate_show_card(seat_index, GameConstants.Show_Card_HU, hand_card_count, cards, GameConstants.INVALID_SEAT);

		return;
	}

	@Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x18, 0x18, 0x19, 0x19, 0x19, 0x11 };
		int[] cards_of_player1 = new int[] { 0x13, 0x13, 0x13, 0x13, 0x15, 0x15, 0x14, 0x14, 0x16, 0x16, 0x22, 0x28, 0x28 };
		int[] cards_of_player2 = new int[] { 0x13, 0x13, 0x13, 0x13, 0x12, 0x15, 0x15, 0x14, 0x14, 0x16, 0x16, 0x22, 0x28 };
		int[] cards_of_player3 = new int[] { 0x13, 0x13, 0x13, 0x13, 0x03, 0x05, 0x05, 0x25, 0x17, 0x08, 0x09, 0x28, 0x28 };

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
	public int get_banker_pre_seat(int banker_seat) {
		int count = 0;
		int seat = banker_seat;
		do {
			count++;
			seat = (getTablePlayerNumber() + seat - 1) % getTablePlayerNumber();
		} while (get_players()[seat] == null && count <= 5);
		return seat;
	}

	@Override
	public int get_banker_next_seat(int banker_seat) {
		int count = 0;
		int seat = banker_seat;
		do {
			count++;
			seat = (seat + 1) % getTablePlayerNumber();
		} while (get_players()[seat] == null && count <= 5);
		return seat;
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
	public boolean handler_requst_pao_qiang(Player player, int pao, int qiang) {
		return _handler_gumai.handler_pao_qiang(this, player.get_seat_index(), pao, qiang);
	}

	@Override
	public boolean handler_requst_message_deal(Player player, int seat_index, RoomRequest room_rq, int type) {
		int size = room_rq.getOutCardsList().size();
		int[] cards = new int[size];

		for (int i = 0; i < size; i++) {
			cards[i] = room_rq.getOutCardsList().get(i);
		}

		return _handler_switch_card.handler_switch_cards(this, seat_index, cards);
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		// TODO Auto-generated method stub
		return false;
	}

	// 查找原缺
	public void search_yuan_que() {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			yuan_que_player[i] = -1;
			int cards[] = new int[GameConstants.MAX_COUNT];
			int hand_card_count = switch_to_cards_data(GRR._cards_index[i], cards, i);
			int frist_color = _logic.get_card_color(cards[0]);
			int color_count = 0;
			for (int j = 0; j < hand_card_count; j++) {
				int color = _logic.get_card_color(cards[j]);
				if (frist_color != color) {
					color_count++;
					frist_color = color;
				}
			}
			if (color_count <= 1) {
				yuan_que_player[i] = 1;
			}
		}
	}

	// 结算查缺
	public void search_cha_que() {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			player_duan_score[i] = 0;
			player_show_desc_10[i][25] = 0;
		}
		for (int player = 0; player < this.getTablePlayerNumber(); player++) {
			// 胡牌的人
			if (GRR._chi_hu_rights[player].is_valid() == true) {
				for (int k = 0; k < this.getTablePlayerNumber(); k++) {
					if (GRR._chi_hu_rights[k].is_valid() == true || player == k) {
						continue;
					}
					// 没胡的人
					if (GRR._chi_hu_rights[k].is_valid() == false) {
						int cards[] = new int[GameConstants.MAX_COUNT];
						int hand_card_count = switch_to_cards_data(GRR._cards_index[k], cards, k);
						int score = 0;
						for (int i = 0; i < hand_card_count; i++) {
							if (this._logic.is_magic_card(cards[i])) {
								continue;
							}
							if (_logic.get_card_color(cards[i]) == this.player_duan[k]) {
								score++;
							}
						}
						player_duan_score[k] -= score;
						player_duan_score[player] += score;
						player_show_desc_10[k][25] -= score;
						player_show_desc_10[player][25] += score;
					}
				}
			}
		}

		// 查缺
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			GRR._game_score[i] += player_duan_score[i];
		}
	}

	public boolean is_ka_bian_diao(int seat_index) {
		// 边卡吊
		if (this.has_rule(GameConstants_ZYZJ.GAME_RULE_LAIZI_ZYZJ)) {
			if (_playerStatus[seat_index]._hu_card_count != 2) {
				return false;
			}
			for (int i = 0; i < _playerStatus[seat_index]._hu_card_count; i++) {
				if (_playerStatus[seat_index]._hu_cards[i] == GameConstants_ZYZJ.YI_TONG_CARD)
					continue;
				if (_logic.get_card_value(_playerStatus[seat_index]._hu_cards[0]) == 3
						|| _logic.get_card_value(_playerStatus[seat_index]._hu_cards[0]) == 7) {
					// 三丁拐，二丁拐没有卡边吊
					if (!this.has_rule(GameConstants_ZYZJ.GAME_RULE_EDG_ZYZJ) && !this.has_rule(GameConstants_ZYZJ.GAME_RULE_SDG_ZYZJ)) {
						return true;
					}
				}
			}
		} else {
			if (_playerStatus[seat_index]._hu_card_count == 1 && (_logic.get_card_value(_playerStatus[seat_index]._hu_cards[0]) == 3
					|| _logic.get_card_value(_playerStatus[seat_index]._hu_cards[0]) == 7)) {
				// 三丁拐，二丁拐没有卡边吊
				if (!this.has_rule(GameConstants_ZYZJ.GAME_RULE_EDG_ZYZJ) && !this.has_rule(GameConstants_ZYZJ.GAME_RULE_SDG_ZYZJ)) {
					return true;
				}

			}
		}

		return false;
	}

	public boolean is_da_kuan_zhang(int seat_index) {
		if (_playerStatus[seat_index]._hu_card_count != 4 && _playerStatus[seat_index]._hu_card_count != 3) {
			return false;
		}
		if (this.has_rule(GameConstants_ZYZJ.GAME_RULE_EDG_ZYZJ) || this.has_rule(GameConstants_ZYZJ.GAME_RULE_SDG_ZYZJ)) {
			return false;
		}

		List<Integer> ting_card = new ArrayList<Integer>();
		if (this.has_rule(GameConstants_ZYZJ.GAME_RULE_LAIZI_ZYZJ)) {
			ting_card.clear();
			for (int i = 0; i < 4; i++) {
				if (_playerStatus[seat_index]._hu_cards[i] == GameConstants_ZYZJ.YI_TONG_CARD) {
					continue;
				}
				ting_card.add(_playerStatus[seat_index]._hu_cards[i]);
			}
		} else {
			ting_card.clear();
			for (int i = 0; i < _playerStatus[seat_index]._hu_card_count; i++) {
				ting_card.add(_playerStatus[seat_index]._hu_cards[i]);
			}
		}

		if (ting_card.size() != 3) {
			return false;
		}

		if (_logic.get_card_value(ting_card.get(0)) + 3 == _logic.get_card_value(ting_card.get(1))
				&& _logic.get_card_value(ting_card.get(1)) + 3 == _logic.get_card_value(ting_card.get(2))) {
			return true;
		}

		return false;
	}

	public long analyse_qi_xiao_dui(int cards_index[], int cbWeaveCount, int cur_card) {
		if (cbWeaveCount != 0)
			return 0;

		int cbReplaceCount = 0;
		int nGenCount = 0;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入数据
		int cbCurrentIndex = _logic.switch_to_card_index(cur_card);
		cbCardIndexTemp[cbCurrentIndex]++;

		int magic_card_count = _logic.get_magic_card_count();

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];

			if (magic_card_count > 0) {
				for (int m = 0; m < magic_card_count; m++) {
					if (i == _logic.get_magic_card_index(m))
						continue;

					if (cbCardCount == 1 || cbCardCount == 3)
						cbReplaceCount++;

					if (cbCardCount == 4) {
						nGenCount++;
					}
				}
			} else {
				if (cbCardCount == 1 || cbCardCount == 3)
					cbReplaceCount++;

				if (cbCardCount == 4) {
					nGenCount++;
				}
			}
		}

		if (magic_card_count > 0) {
			int count = 0;
			for (int m = 0; m < magic_card_count; m++) {
				count += cbCardIndexTemp[_logic.get_magic_card_index(m)];
			}

			if (cbReplaceCount > count) {
				return 0;
			}

			if (count > 0) {
				for (int i = 0; i < GameConstants.MAX_INDEX && count > 0; i++) {
					int cbCardCount = cbCardIndexTemp[i];

					if (cbReplaceCount > 0 && ((cbCardCount == 1 || cbCardCount == 3))) {
						if (cbCardCount + count >= 4) {
							nGenCount++;
							count -= 4 - cbCardCount;
							cbReplaceCount--;
						}
					} else if (cbReplaceCount == 0 && cbCardCount > 0 && cbCardCount < 4) {
						if (cbCardCount + count >= 4) {
							nGenCount++;
							count -= 4 - cbCardCount;
						}
					}
				}
				if (count == 4) {
					nGenCount++;
				}
			}
		} else {
			if (cbReplaceCount > 0)
				return 0;
		}

		if (nGenCount > 0) {
			return GameConstants_ZYZJ.CHR_LONG_QI_DUI;
		} else {
			return GameConstants_ZYZJ.CHR_XIAO_QI_DUI;
		}
	}

	public long analyse_qi_xiao_dui_out_laizi(int cards_index[], int cbWeaveCount, int cur_card) {
		if (cbWeaveCount != 0)
			return 0;

		int cbReplaceCount = 0;
		int nGenCount = 0;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入数据
		int cbCurrentIndex = _logic.switch_to_card_index(cur_card);
		cbCardIndexTemp[cbCurrentIndex]++;

		int magic_card_count = _logic.get_magic_card_count();

		//
		magic_card_count -= 1;

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];

			if (magic_card_count > 0) {
				for (int m = 0; m < magic_card_count; m++) {
					if (i == _logic.get_magic_card_index(m))
						continue;

					if (cbCardCount == 1 || cbCardCount == 3)
						cbReplaceCount++;

					if (cbCardCount == 4) {
						nGenCount++;
					}
				}
			} else {
				if (cbCardCount == 1 || cbCardCount == 3)
					cbReplaceCount++;

				if (cbCardCount == 4) {
					nGenCount++;
				}
			}
		}

		if (magic_card_count > 0) {
			int count = 0;
			for (int m = 0; m < magic_card_count; m++) {
				count += cbCardIndexTemp[_logic.get_magic_card_index(m)];
			}

			if (cbReplaceCount > count) {
				return 0;
			}

			if (count > 0) {
				for (int i = 0; i < GameConstants.MAX_INDEX && count > 0; i++) {
					int cbCardCount = cbCardIndexTemp[i];

					if (cbReplaceCount > 0 && ((cbCardCount == 1 || cbCardCount == 3))) {
						if (cbCardCount + count >= 4) {
							nGenCount++;
							count -= 4 - cbCardCount;
							cbReplaceCount--;
						}
					} else if (cbReplaceCount == 0 && cbCardCount > 0 && cbCardCount < 4) {
						if (cbCardCount + count >= 4) {
							nGenCount++;
							count -= 4 - cbCardCount;
						}
					}
				}
				if (count == 4) {
					nGenCount++;
				}
			}
		} else {
			if (cbReplaceCount > 0)
				return 0;
		}

		if (nGenCount > 0) {
			return GameConstants_ZYZJ.CHR_LONG_QI_DUI;
		} else {
			return GameConstants_ZYZJ.CHR_XIAO_QI_DUI;
		}
	}

	// 重置杠分
	public void process_reset_gang_score(int provide_index) {
		// 先把所有的杠分都清空
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
				for (int k = 0; k < getTablePlayerNumber(); k++) {
					GRR._gang_score[i].scores[j][k] = 0;// 杠牌，每个人的分数
				}
			}
			player_show_desc_10[i][11] = 0;
			player_show_desc_10[i][12] = 0;
			player_show_desc_10[i][13] = 0;

		}

		for (int player = 0; player < getTablePlayerNumber(); player++) {
			if (shao_gang[player] || shao[player])
				continue;
			int iGangcount = 0;
			for (int w = 0; w < GRR._weave_count[player]; w++) {
				if (GRR._weave_items[player][w].weave_kind != GameConstants.WIK_GANG)
					continue;

				if (GRR._weave_items[player][w].type == GameConstants.GANG_TYPE_AN_GANG) {
					int score = GameConstants.CELL_SCORE;
					score = 3;
					if (has_rule(GameConstants_ZYZJ.GAME_RULE_EDG_ZYZJ) || has_rule(GameConstants_ZYZJ.GAME_RULE_SDG_ZYZJ)) {
						if (GRR._weave_items[player][w].center_card == GameConstants_ZYZJ.YJ_CARD) {
							score = 7;
						} else if (GRR._weave_items[player][w].center_card == GameConstants_ZYZJ.BA_TONG_CARD) {
							score = 11;
						}
					}
					for (int m = 0; m < getTablePlayerNumber(); m++) {
						if (m == player)
							continue;
						GRR._gang_score[player].scores[iGangcount][m] -= score;
						GRR._gang_score[player].scores[iGangcount][player] += score;
						player_show_desc_10[m][11] -= score;
						player_show_desc_10[player][11] += score;
					}
					iGangcount++;
				} else if (GRR._weave_items[player][w].type == GameConstants.GANG_TYPE_ADD_GANG) {
					int score = 2;
					for (int m = 0; m < getTablePlayerNumber(); m++) {
						if (m == player)
							continue;
						GRR._gang_score[player].scores[iGangcount][m] -= score;
						GRR._gang_score[player].scores[iGangcount][player] += score;
						player_show_desc_10[m][11] -= score;
						player_show_desc_10[player][11] += score;
					}
					iGangcount++;
				} else if (GRR._weave_items[player][w].type == GameConstants.GANG_TYPE_JIE_GANG) {
					int score = 2;
					GRR._gang_score[player].scores[iGangcount][GRR._weave_items[player][w].provide_player] -= score;
					GRR._gang_score[player].scores[iGangcount][player] += score;
					player_show_desc_10[GRR._weave_items[player][w].provide_player][11] -= score;
					player_show_desc_10[player][11] += score;
					iGangcount++;
				}
			}
		}
	}

}
