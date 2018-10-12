package com.cai.game.mj.handler.jszz;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.define.ECardType;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RandomUtil;
import com.cai.dictionary.SysParamDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.DispatchLastCardRunnable;
import com.cai.future.runnable.LengTuoZiRunnable;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJGameLogic.AnalyseItem;
import com.cai.game.mj.MJType;
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
import protobuf.clazz.mj.MjRsp.JS_Mj_Game_End_Detail;
import protobuf.clazz.mj.MjRsp.Mj_Game_End;
import protobuf.clazz.mj.MjRsp.Request_message_chat;
import protobuf.clazz.mj.MjRsp.Respone_Message_Chat;
import protobuf.clazz.mj.MjRsp.USER_MJ_Game_end;

public class MJTable_JangSu_ZZ extends AbstractMJTable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2698317717887322025L;

	public MJHandlerHun_JangSu_ZZ _handler_hun;// 癞子牌
	public int _user_times[];
	public boolean _b_double;
	public int _touzi_count;
	public int _di_fen;
	public int _yuanzi_fen;
	public boolean _user_pao_da[];
	public int _out_card_index[][];
	public int _gen_out_card;
	public int _gen_player;
	public int _xian_chu_count[];
	public boolean _change_banker;
	public int _current_banker = GameConstants.INVALID_SEAT; // 当前庄

	public int _da_tou_da[];
	public int _da_er_da[];
	public int _da_peng_da[];
	public int _gen_zhang[];
	public int _zi_da_an_gang[];
	public int _end_score[];

	public boolean _is_da_shouda[];
	public int _can_not_open_index[][];
	public int _peng_palyer_count[][];
	public int _gang_palyer_count[][];
	public boolean _is_bao_pei;
	public boolean _is_gangkai;
	public int bao_pei_palyer[];

	public int SCORE_TYPE_HU_TYPE = 0x01;
	public int SCORE_TYPE_GEN_ZHANG = 0x02;
	public int SCORE_TYPE_ZI_DA_ANGANG = 0x04;
	public int SCORE_TYPE_BU_GANG = 0x08;
	public int SCORE_TYPE_PENG_GANG = 0x10;
	public int SCORE_TYPE_AN_GANG = 0x20;

	public MJTable_JangSu_ZZ() {
		super(MJType.GAME_TYPE_JIANGSU_ZZ);
	}

	@Override
	protected void onInitTable() {
		_handler_dispath_card = new MJHandlerDispatchCard_JangSu_ZZ();
		_handler_out_card_operate = new MJHandlerOutCardOperate_JangSu_ZZ();
		_handler_gang = new MJHandlerGang_JangSu_ZZ();
		_handler_chi_peng = new MJHandlerChiPeng_JangSu_ZZ();
		_handler_hun = new MJHandlerHun_JangSu_ZZ();
		_user_times = new int[this.getTablePlayerNumber()];
		_user_pao_da = new boolean[this.getTablePlayerNumber()];
		_da_tou_da = new int[this.getTablePlayerNumber()];
		_da_er_da = new int[this.getTablePlayerNumber()];
		_da_peng_da = new int[this.getTablePlayerNumber()];
		_xian_chu_count = new int[this.getTablePlayerNumber()];
		_gen_zhang = new int[getTablePlayerNumber()];
		_zi_da_an_gang = new int[getTablePlayerNumber()];
		_end_score = new int[getTablePlayerNumber()];
		_is_da_shouda = new boolean[getTablePlayerNumber()];
		_out_card_index = new int[this.getTablePlayerNumber()][GameConstants.MAX_INDEX];
		_can_not_open_index = new int[this.getTablePlayerNumber()][GameConstants.MAX_INDEX];
		_peng_palyer_count = new int[this.getTablePlayerNumber()][getTablePlayerNumber()];
		_gang_palyer_count = new int[this.getTablePlayerNumber()][getTablePlayerNumber()];
		bao_pei_palyer = new int[getTablePlayerNumber()];
		Arrays.fill(_user_times, 1);
		Arrays.fill(_da_tou_da, 0);
		Arrays.fill(_da_er_da, 0);
		Arrays.fill(_da_peng_da, 0);
		Arrays.fill(_gen_zhang, 0);
		Arrays.fill(_zi_da_an_gang, 0);
		Arrays.fill(_end_score, 0);
		Arrays.fill(_user_pao_da, false);
		Arrays.fill(_is_da_shouda, false);
		Arrays.fill(bao_pei_palyer, GameConstants.INVALID_CARD);
		_b_double = false;
		_is_bao_pei = false;
		_is_gangkai = false;

		_di_fen = 1;
		_yuanzi_fen = 50;
		_touzi_count = 0;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Arrays.fill(_out_card_index[i], GameConstants.INVALID_CARD);
			Arrays.fill(_can_not_open_index[i], GameConstants.INVALID_CARD);
			Arrays.fill(_peng_palyer_count[i], 0);
			Arrays.fill(_gang_palyer_count[i], 0);
		}
	}

	@Override
	public boolean handler_player_ready(int seat_index, boolean is_cancel) {

		if (this.get_players()[seat_index] == null) {
			return false;
		}
		if (GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) {
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

		// 跑分
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_result.pao[i] = 0;// 清掉 默认是-1
		}
		// 闹庄
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_result.nao[i] = 0;
		}

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

	@Override
	public void progress_banker_select() {
		_b_double = false;
		if (_current_banker == GameConstants.INVALID_SEAT) {
			_current_banker = 0;// 创建者的玩家为专家

			_current_banker = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % this.getTablePlayerNumber());

			_shang_zhuang_player = GameConstants.INVALID_SEAT;
			_lian_zhuang_player = GameConstants.INVALID_SEAT;
			_b_double = false;
		}
		if (_lian_zhuang_player != GameConstants.INVALID_SEAT) {
			_b_double = true;
		}

		if (is_sys()) {// 金币场 随机庄家
			Random random = new Random();//
			int rand = random.nextInt(6) + 1 + random.nextInt(6) + 1;
			_current_banker = rand % GameConstants.GAME_PLAYER;//

			_shang_zhuang_player = GameConstants.INVALID_SEAT;
			_lian_zhuang_player = GameConstants.INVALID_SEAT;
		}
	}

	// 游戏开始
	@Override
	protected boolean on_handler_game_start() {
		if (_cur_round == 2) {
			// real_kou_dou();// 记录真实扣豆
		}

		reset_init_data();
		Arrays.fill(_is_da_shouda, false);
		Arrays.fill(_user_pao_da, false);
		Arrays.fill(_user_times, 1);
		Arrays.fill(_da_tou_da, 0);
		Arrays.fill(_da_er_da, 0);
		Arrays.fill(_da_peng_da, 0);
		Arrays.fill(_gen_zhang, 0);
		Arrays.fill(_zi_da_an_gang, 0);
		Arrays.fill(_xian_chu_count, 0);
		Arrays.fill(_end_score, 0);
		Arrays.fill(_user_times, 1);
		Arrays.fill(bao_pei_palyer, GameConstants.INVALID_CARD);

		_is_bao_pei = false;
		_is_gangkai = false;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Arrays.fill(_out_card_index[i], 0);
			Arrays.fill(_can_not_open_index[i], 0);
			Arrays.fill(_peng_palyer_count[i], 0);
			Arrays.fill(_gang_palyer_count[i], 0);
		}
		_gen_player = GameConstants.INVALID_SEAT;
		_gen_out_card = GameConstants.INVALID_CARD;
		_touzi_count = 0;
		// 庄家选择
		this.progress_banker_select();

		_game_status = GameConstants.GS_MJ_PLAY;
		// 信阳麻将
		GRR._banker_player = _current_banker;
		_current_player = GRR._banker_player;
		_change_banker = false;

		_repertory_card = new int[GameConstants.CARD_COUNT_JIANGSU_ZZ];
		shuffle(_repertory_card, GameConstants.CARD_DATA_JIANGSU_ZZ);

		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();

		getLocationTip();

		try {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				for (int j = 0; j < GRR._cards_index[i].length; j++) {
					if (GRR._cards_index[i][j] == 4) {
						MongoDBServiceImpl.getInstance().card_log(this.get_players()[i], ECardType.anLong, "",
								GRR._cards_index[i][j], 0l, this.getRoom_id());
					}
				}
			}
		} catch (Exception e) {

		}
		// 游戏开始时 初始化 未托管
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			istrustee[i] = false;
		}

		return on_game_start();
	}

	protected void test_cards() {

		int cards[] = new int[] { 0x05, 0x05, 0x05, 0x02, 0x03, 0x07, 0x08, 0x09, 0x14, 0x14, 0x19, 0x01, 0x01 };

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			for (int j = 0; j < 13; j++) {
				GRR._cards_index[i][_logic.switch_to_card_index(cards[j])] += 1;
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
	protected boolean on_game_start() {
		// 游戏开始
		this._game_status = GameConstants.GS_MJ_PLAY;// 设置状态
		_logic.clean_magic_cards();

		this.GRR._banker_player = this._current_player = this._current_banker;
		// 游戏开始
		this._game_status = GameConstants.GS_MJ_PLAY;// 设置状态
		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		// gameStartResponse.setSiceIndex(rand);
		gameStartResponse.setBankerPlayer(this.GRR._banker_player);
		gameStartResponse.setCurrentPlayer(this._current_player);
		gameStartResponse.setLeftCardCount(this.GRR._left_card_count);

		int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants.MAX_COUNT];
		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int hand_card_count = this._logic.switch_to_cards_data(this.GRR._cards_index[i], hand_cards[i]);
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
			roomResponse.setCurrentPlayer(
					this._current_player == GameConstants.INVALID_SEAT ? this._resume_player : this._current_player);
			roomResponse.setLeftCardCount(this.GRR._left_card_count);
			roomResponse.setGameStatus(this._game_status);
			roomResponse.setLeftCardCount(this.GRR._left_card_count);
			this.send_response_to_player(i, roomResponse);
		}
		////////////////////////////////////////////////// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		this.load_room_info_data(roomResponse);
		this.load_common_status(roomResponse);
		this.load_player_info_data(roomResponse);
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}
		for (int i = 0; i < 2; i++) {
			gameStartResponse.addOtherCards(_repertory_card[_gang_mo_posion]);
			_gang_mo_cards[i] = _repertory_card[_gang_mo_posion++];
		}

		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(this.GRR._left_card_count);
		this.GRR.add_room_response(roomResponse);

		if (this.has_rule(GameConstants.GAME_RULE_JIANGSU_DOUBLE)) {

			if (this._lian_zhuang_player == GameConstants.INVALID_SEAT) {
				GameSchedule.put(new LengTuoZiRunnable(getRoom_id(), GRR._banker_player, this),
						GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);
			} else {
				_lian_zhuang_player = GameConstants.INVALID_SEAT;
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					this.send_error_notify(i, 2, "连庄翻倍");
				}
				exe_hun(this.GRR._banker_player);
			}
		} else {
			exe_hun(this.GRR._banker_player);
		}

		return true;

	}

	public void set_handler_out_card_operate() {
		this.set_handler(_handler_dispath_card);
		_handler_dispath_card.other_deal(this);
	}

	public void exe_hun(int seat_index) {
		// 出牌
		this.set_handler(this._handler_hun);
		this._handler_hun.reset_status(seat_index);
		this._handler_hun.exe(this);
	}

	/***
	 * 麻将胡牌解析
	 * 
	 * @param cards_index
	 * @param weaveItems
	 * @param weaveCount
	 * @param cur_card
	 * @param chiHuRight
	 * @param card_type
	 * @return
	 */
	public int analyse_chi_hu_card(int cards_index[], WeaveItem weaveItems[], int weaveCount, int cur_card,
			ChiHuRight chiHuRight, int card_type) {

		// cbCurrentCard一定不为0 !!!!!!!!!
		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		// 变量定义
		int cbChiHuKind = GameConstants.WIK_NULL;
		// 构造扑克
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入扑克
		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		int igc_count = _logic.magic_count(cbCardIndexTemp);

		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
		// 分析扑克
		boolean bValue = _logic.analyse_card_js_zz(cbCardIndexTemp, weaveItems, weaveCount, analyseItemArray, true);
		if (!bValue) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		if (card_type == GameConstants.HU_CARD_TYPE_PAOHU
				&& _logic.is_laizi_hu(analyseItemArray, cbCardIndexTemp, weaveItems, weaveCount)) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}
		if (_logic.is_da_gen_card(cur_card)) {
			if (_logic.is_hu_lai_gen_an_gang(analyseItemArray, cbCardIndexTemp, weaveItems, weaveCount)) {
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO || card_type == GameConstants.HU_CARD_TYPE_TIAN_HU) {
					chiHuRight.opr_or(GameConstants.CHR_JIANGSU_ZZ_ZI_MO_ZHENGDA);
				} else {
					chiHuRight.opr_or(GameConstants.CHR_JIANGSU_ZZ_FANG_PAO_ZHENGDA);
				}

			}
		}

		if (!_logic.is_laizi_hu(analyseItemArray, cbCardIndexTemp, weaveItems, weaveCount)) {
			chiHuRight.opr_or(GameConstants.CHR_JIANGSU_ZZ_TUO_DA);
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;
		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO || card_type == GameConstants.HU_CARD_TYPE_TIAN_HU) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else if (card_type == GameConstants.HU_CARD_TYPE_PAOHU) {
			chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
		} else if (card_type == GameConstants.HU_CARD_TYPE_QIANGGANG) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_QIANG_GANG_HU);
		} else if (card_type == GameConstants.HU_CARD_TYPE_TIAN_HU) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_TIAN_HU);
		} else if (card_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
			chiHuRight.opr_or(GameConstants.CHR_JIANGSU_ZZ_MING_GANG_KAI);
		} else if (card_type == GameConstants.HU_CARD_TYPE_AN_GANG_KAI) {
			chiHuRight.opr_or(GameConstants.CHR_JIANGSU_ZZ_AN_GANG_KAI);
		} else if (card_type == GameConstants.HU_CARD_TYPE_BU_GANG_KAI) {
			chiHuRight.opr_or(GameConstants.CHR_JIANGSU_ZZ_BU_GANG_KAI);
		}

		return cbChiHuKind;
	}

	/**
	 * 三门峡麻将获取听牌
	 * 
	 * @param cards
	 * @param cards_index
	 * @param weaveItem
	 * @param cbWeaveCount
	 * @return
	 */
	public int get_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount) {

		// 复制数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int del = 0;

		boolean isDaiFeng = true;
		int mj_count = GameConstants.MAX_ZI;
		if (isDaiFeng) {
			mj_count = GameConstants.MAX_ZI_FENG;
		} else {
			mj_count = GameConstants.MAX_ZI;
		}

		for (int i = 0; i < mj_count; i++) {
			// if (this._logic.is_magic_index(i))
			// continue;
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard,
					chr, GameConstants.HU_CARD_TYPE_ZIMO)) {

				cards[count] = cbCurrentCard;
				count++;
			}
		}

		// 有胡的牌。癞子肯定能胡
		if (count > 0) {
			// if (has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN)) {
			// cards[count] =
			// _logic.switch_to_card_data(this._logic.get_magic_card_index(0))
			// + GameConstants.CARD_ESPECIAL_TYPE_HUN;
			// count++;
			// }
		} else {
			// if (has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN)) {
			// // 看看鬼牌能不能胡
			// cbCurrentCard =
			// _logic.switch_to_card_data(this._logic.get_magic_card_index(0));
			// chr.set_empty();
			// if (GameConstants.WIK_CHI_HU ==
			// analyse_chi_hu_card_henan(cbCardIndexTemp, weaveItem,
			// cbWeaveCount,
			// cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO)) {
			// cards[count] = cbCurrentCard +
			// GameConstants.CARD_ESPECIAL_TYPE_HUN;
			// count++;
			// }
			// }
		}

		int number = isDaiFeng ? 34 : 27;
		if (count >= number) {
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	/***
	 * 检查杠牌,有没有胡的 检查所有人
	 * 
	 * @param seat_index
	 * @param card
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
			// 可以胡的情况 判断
			if (playerStatus.is_chi_hu_round()) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						GameConstants.HU_CARD_TYPE_PAOHU);

				// 结果判断
				if (action != 0) {
					chr.opr_or(GameConstants.CHR_JIANGSU_ZZ_QIANG_GANG_HU);// 抢杠胡
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

	// 麻将
	public boolean estimate_player_out_card_respond_jszz(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		// 用户状态
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {

			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		// int llcard = get_niao_card_num(true, 0);
		int llcard = 0;

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;
		// 动作判断
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			//// 碰牌判断
			action = _logic.check_peng(GRR._cards_index[i], card);
			if (action != 0 && _can_not_open_index[i][_logic.switch_to_card_index(card)] == 0) {
				playerStatus.add_action(action);
				playerStatus.add_peng(card, seat_index);
				bAroseAction = true;
			}
			if (GRR._left_card_count > llcard) {
				// 杠牌判断
				action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
				if (action != 0) {
					playerStatus.add_action(GameConstants.WIK_GANG);
					playerStatus.add_gang(card, seat_index, 1);// 加上杠
					bAroseAction = true;
				}
			}

			// 可以胡的情况 判断
			if (_playerStatus[i].is_chi_hu_round()) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						GameConstants.HU_CARD_TYPE_PAOHU);

				// 结果判断
				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
					bAroseAction = true;
				}
			}

			// }
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

	protected boolean on_handler_game_finish(int seat_index, int reason) {
		int real_reason = reason;

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
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
		game_end.setRunPlayerId(_run_player_id);
		game_end.setRoundOverType(0);
		game_end.setRoomOverType(0);
		game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间
		game_end.setGamePlayerNumber(getTablePlayerNumber());
		if (GRR != null) {// reason == MJGameConstants.Game_End_NORMAL || reason
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

			if (this._b_double) {
				if (_touzi_count == 0) {
					game_end.setCellScore(7);
				} else {
					game_end.setCellScore(_touzi_count);
				}
			} else {
				game_end.setCellScore(0);
			}

			game_end.setBankerPlayer(GRR._banker_player);// 专家
			game_end.setLeftCardCount(GRR._left_card_count);// 剩余牌
			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

			// 设置中鸟数据
			for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i < GRR._count_niao; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao[i]);
			}
			game_end.setCountPickNiao(GRR._count_pick_niao);// 中鸟个数

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					pnc.addItem(GRR._player_niao_cards[i][j]);
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
			this.load_game_end_detail(game_end);
			this.set_result_describe();

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {

					cs.addItem(GRR._cards_data[i][j]);
				}
				for (int j = 0; j < _da_tou_da[i] / 6; j++) {
					cs.addItem(_logic.switch_to_card_data(_logic.get_magic_card_index(0))
							+ GameConstants.CARD_ESPECIAL_TYPE_TOU_DA);
				}
				if (this._da_tou_da[i] > 0) {
					GRR._card_count[i] += _da_tou_da[i] / 6;
				}
				for (int j = 0; j < _da_er_da[i] / 4; j++) {
					cs.addItem(_logic.switch_to_card_data(_logic.get_magic_card_index(0))
							+ GameConstants.CARD_ESPECIAL_TYPE_ER_DA);
				}
				if (this._da_er_da[i] > 0) {
					GRR._card_count[i] += _da_er_da[i] / 4;
				}
				for (int j = 0; j < _da_peng_da[i] / 2; j++) {
					cs.addItem(_logic.switch_to_card_data(_logic.get_magic_card_index(0))
							+ GameConstants.CARD_ESPECIAL_TYPE_BEIDONG_DA);
				}
				if (this._da_peng_da[i] > 0) {
					GRR._card_count[i] += _da_peng_da[i] / 2;
				}

				game_end.addCardsData(cs);// 牌

				// 组合
				WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
				int index = 0;
				for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
					if (!_logic.is_magic_card(GRR._weave_items[i][j].center_card)) {
						WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
						weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
						weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
						weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
						weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
						weaveItem_array.addWeaveItem(weaveItem_item);
						index++;
					}
				}
				for (int j = index; j < GameConstants.MAX_WEAVE; j++) {
					WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
					weaveItem_item.setCenterCard(0);
					weaveItem_item.setProvidePlayer(0);
					weaveItem_item.setPublicCard(0);
					weaveItem_item.setWeaveKind(0);
					weaveItem_array.addWeaveItem(weaveItem_item);
				}
				game_end.addWeaveItemArray(weaveItem_array);

				GRR._chi_hu_rights[i].get_right_data(rv);// 获取权位数值
				game_end.addChiHuRight(rv[0]);

				GRR._start_hu_right[i].get_right_data(rv);// 获取权位数值
				game_end.addStartHuRight(rv[0]);

				game_end.addProvidePlayer(GRR._provider[i]);
				game_end.addGameScore(_end_score[i]);// 放炮的人？
				game_end.addGangScore(0);// 杠牌得分
				game_end.addStartHuScore(GRR._start_hu_score[i]);
				game_end.addResultDes(GRR._result_des[i]);

				// 胡牌
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
			if (_cur_round >= _game_round && (!is_sys())) {// 局数到了
				end = true;
				real_reason = GameConstants.Game_End_ROUND_OVER;
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
		this.logger.error(game_end.build());
		////////////////////////////////////////////////////////////////////// 得分总的
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

	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {

	}

	/**
	 * 在玩家的前面显示出的牌 --- 发送玩家出牌
	 * 
	 * @param seat_index
	 * @param count
	 * @param cards
	 * @param type
	 * @param to_player
	 * @return
	 */
	//
	public boolean operate_out_card(int seat_index, int count, int cards[], int type, int to_player,
			int discard_count) {
		if (seat_index == GameConstants.INVALID_SEAT) {
			return false;
		}

		//// 发送玩家出牌 201 =玩家出牌(CMD_OutCard: operate_player,operate_card)
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_OUT_CARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(type);// 出牌
		roomResponse.setCardCount(count);
		int flashTime = 150;
		int standTime = 130;
		int gameId = this.getGame_id() == 0 ? 1 : this.getGame_id();
		SysParamModel sysParamModel105 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId)
				.get(1105);
		if (sysParamModel105 != null && sysParamModel105.getVal3() > 0 && sysParamModel105.getVal3() < 1000) {
			flashTime = sysParamModel105.getVal3();
			standTime = sysParamModel105.getVal4();
		}
		roomResponse.setFlashTime(flashTime);
		roomResponse.setStandTime(standTime);
		roomResponse.setTotalSize(discard_count);

		for (int i = 0; i < count; i++) {

			roomResponse.addCardData(cards[i]);
		}

		if (to_player == GameConstants.INVALID_SEAT) {
			GRR.add_room_response(roomResponse);
			return this.send_response_to_room(roomResponse);
		} else {
			return this.send_response_to_player(to_player, roomResponse);
		}

	}

	/**
	 * 麻将算分
	 * 
	 * @param seat_index
	 * @param provide_index
	 * @param operate_card
	 * @param zimo
	 */
	public void process_chi_hu_player_score_jszz(int seat_index, int provide_index, int operate_card, boolean zimo,
			boolean yipaosanxiang, boolean is_beopei, boolean gang_fangpao) {
		if (this.has_rule(GameConstants.GAME_RULE_JIANGSU_DOUBLE)) {
			if (this._current_banker == seat_index) {
				this._lian_zhuang_player = seat_index;
			} else if (!_change_banker) {
				_change_banker = true;
				this._current_banker = (_current_banker + 1) % getTablePlayerNumber();
			}

		} else if (!_change_banker) {
			_change_banker = true;
			this._current_banker = (_current_banker + 1) % getTablePlayerNumber();
		}

		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		if (zimo) {
			ChiHuRight chr = GRR._chi_hu_rights[seat_index];
			if (!(chr.opr_and(GameConstants.CHR_JIANGSU_ZZ_TUO_DA)).is_empty()) {
				if (!(chr.opr_and(GameConstants.CHR_JIANGSU_ZZ_PAO_DA)).is_empty()) {
					_user_times[seat_index] *= 10;
				} else {
					_user_times[seat_index] *= 6;
				}
				if (!(chr.opr_and(GameConstants.CHR_JIANGSU_ZZ_AN_GANG_KAI)).is_empty()) {
					_user_times[seat_index]++;
				}
				if (!(chr.opr_and(GameConstants.CHR_JIANGSU_ZZ_MING_GANG_KAI)).is_empty()) {
					_user_times[seat_index]++;
				}
				if (!(chr.opr_and(GameConstants.CHR_JIANGSU_ZZ_BU_GANG_KAI)).is_empty()) {
					_user_times[seat_index]++;
				}
				if (this._logic.is_magic_card(operate_card)) {
					_da_er_da[seat_index] += 4;
				}
			} else {
				if (!(chr.opr_and(GameConstants.CHR_JIANGSU_ZZ_PAO_DA)).is_empty()) {
					_user_times[seat_index] *= 5;
				} else {
					_user_times[seat_index] *= 3;
				}

				if (!(chr.opr_and(GameConstants.CHR_JIANGSU_ZZ_AN_GANG_KAI)).is_empty()) {
					_user_times[seat_index]++;
				}
				if (!(chr.opr_and(GameConstants.CHR_JIANGSU_ZZ_MING_GANG_KAI)).is_empty()) {
					_user_times[seat_index]++;
				}
				if (!(chr.opr_and(GameConstants.CHR_JIANGSU_ZZ_BU_GANG_KAI)).is_empty()) {
					_user_times[seat_index]++;
				}
			}

			if (!(chr.opr_and(GameConstants.CHR_JIANGSU_ZZ_ZI_MO_ZHENGDA)).is_empty()) {
				_player_result.an_gang_count[seat_index]++;
				if (has_rule(GameConstants.GAME_RULE_JIANGSU_YUAN_ZI)) {
					for (int i = 0; i < getTablePlayerNumber(); i++) {
						if (i == seat_index) {
							continue;
						}
						int score = (int) (_player_result.game_score[i] - 2 * _di_fen);
						if (score < -_yuanzi_fen) {
							_player_result.game_score[i] -= _yuanzi_fen + _player_result.game_score[i];
							_player_result.game_score[seat_index] += _yuanzi_fen + _player_result.game_score[i];
							_end_score[i] -= _yuanzi_fen + _player_result.game_score[i];
							_end_score[seat_index] += _yuanzi_fen + _player_result.game_score[i];
						} else {
							_player_result.game_score[i] -= 2 * _di_fen;
							_player_result.game_score[seat_index] += 2 * _di_fen;
							_end_score[i] -= 2 * _di_fen;
							_end_score[seat_index] += 2 * _di_fen;
						}
					}
				} else {
					int gang_cell = _di_fen;
					if (_b_double && has_rule(GameConstants.GAME_RULE_JIANGSU_DOUBLE)) {
						gang_cell *= 2;
					}
					for (int i = 0; i < getTablePlayerNumber(); i++) {
						if (i == seat_index) {
							continue;
						}
						_player_result.game_score[i] -= 2 * gang_cell;
						_player_result.game_score[seat_index] += 2 * gang_cell;
						_end_score[i] -= 2 * gang_cell;
						_end_score[seat_index] += 2 * gang_cell;
					}
				}
			}
		} else {
			ChiHuRight chr = GRR._chi_hu_rights[seat_index];
			if (!(chr.opr_and(GameConstants.CHR_JIANGSU_ZZ_TUO_DA)).is_empty()) {
				if (this._logic.is_magic_card(operate_card)) {
					_da_er_da[seat_index] += 4;
				}
			}

		}

		// 引用权位
		// 统计
		if (zimo) {
			// 自摸
			int score_time = (_user_times[seat_index] + _da_tou_da[seat_index] + _da_er_da[seat_index]
					+ _da_peng_da[seat_index]);
			if (_b_double && has_rule(GameConstants.GAME_RULE_JIANGSU_DOUBLE)) {
				score_time *= 2;
			}
			if (is_beopei || gang_fangpao) {
				GRR._game_score[provide_index] -= (score_time) * _di_fen * 3;
				this._end_score[provide_index] -= (score_time) * _di_fen * 3;
				GRR._game_score[seat_index] += Math.abs(GRR._game_score[provide_index]);
				this._end_score[seat_index] += Math.abs(GRR._game_score[provide_index]);
			} else {
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (i == seat_index) {
						continue;
					}
					if (has_rule(GameConstants.GAME_RULE_JIANGSU_YUAN_ZI)) {
						if (_player_result.game_score[i] - score_time * _di_fen >= -_yuanzi_fen) {
							GRR._game_score[i] -= (score_time) * _di_fen;
							this._end_score[i] -= (score_time) * _di_fen;
							_user_times[i] = -_user_times[seat_index];
						} else {
							GRR._game_score[i] -= _yuanzi_fen + _player_result.game_score[i];
							this._end_score[i] -= _yuanzi_fen + _player_result.game_score[i];
						}
					} else {
						GRR._game_score[i] -= (score_time) * _di_fen;
						this._end_score[i] -= (score_time) * _di_fen;
					}
					GRR._game_score[seat_index] += Math.abs(GRR._game_score[i]);
					this._end_score[seat_index] += Math.abs(GRR._game_score[i]);
				}
			}

			if (is_beopei) {
				GRR._chi_hu_rights[provide_index].opr_or(GameConstants.CHR_TONG_PAO);
			}
		} else {// 点炮
			int score_time = (_da_tou_da[seat_index] + _da_er_da[seat_index] + _da_peng_da[seat_index]);

			if (has_rule(GameConstants.GAME_RULE_JIANGSU_PEI_CHONG)) {
				if (has_rule(GameConstants.GAME_RULE_JIANGSU_YUAN_ZI)) {
					for (int i = 0; i < getTablePlayerNumber(); i++) {
						if (i == seat_index) {
							continue;
						}
						if (i == provide_index) {
							if (_player_result.game_score[i]
									- (score_time + _user_times[seat_index] * 4) * _di_fen >= -_yuanzi_fen) {
								GRR._game_score[i] -= (score_time + _user_times[seat_index] * 4) * _di_fen;//
								this._end_score[i] -= (score_time + _user_times[seat_index] * 4) * _di_fen;
							} else {
								GRR._game_score[i] -= _yuanzi_fen + _player_result.game_score[_provide_player];
								this._end_score[i] -= _yuanzi_fen + _player_result.game_score[_provide_player];
							}

						} else {
							if (_player_result.game_score[i]
									- (score_time + _user_times[seat_index] * 2) * _di_fen >= -_yuanzi_fen) {
								GRR._game_score[i] -= (score_time + _user_times[seat_index] * 2) * _di_fen;//
								this._end_score[i] -= (score_time + _user_times[seat_index] * 2) * _di_fen;
							} else {
								GRR._game_score[i] -= _yuanzi_fen + _player_result.game_score[i];
								this._end_score[i] -= _yuanzi_fen + _player_result.game_score[i];
							}
							ChiHuRight chr = GRR._chi_hu_rights[i];
							chr.opr_or(GameConstants.CHR_JIANGSU_ZZ_CHU_CHONG);
						}

						GRR._game_score[seat_index] += Math.abs(GRR._game_score[i]);//
						this._end_score[seat_index] += Math.abs(GRR._game_score[i]);
					}
				} else {
					for (int i = 0; i < getTablePlayerNumber(); i++) {
						if (i == seat_index) {
							continue;
						}
						if (i == provide_index) {
							GRR._game_score[i] -= (score_time + _user_times[seat_index] * 4) * _di_fen;//
							this._end_score[i] -= (score_time + _user_times[seat_index] * 4) * _di_fen;
						} else {
							GRR._game_score[i] -= (score_time + _user_times[seat_index] * 2) * _di_fen;//
							this._end_score[i] -= (score_time + _user_times[seat_index] * 2) * _di_fen;
							ChiHuRight chr = GRR._chi_hu_rights[i];
							chr.opr_or(GameConstants.CHR_JIANGSU_ZZ_CHU_CHONG);
						}
						GRR._game_score[seat_index] += Math.abs(GRR._game_score[i]);//
						this._end_score[seat_index] += Math.abs(GRR._game_score[i]);
					}
				}
			} else {
				score_time = (score_time * 3 + _user_times[seat_index] * 8);
				if (_b_double && has_rule(GameConstants.GAME_RULE_JIANGSU_DOUBLE)) {
					score_time *= 2;
				}
				if (yipaosanxiang) {
					GRR._game_score[seat_index] -= score_time * _di_fen;//
					this._end_score[seat_index] -= score_time * _di_fen;//
					GRR._game_score[provide_index] += score_time * _di_fen;
					;//
					this._end_score[provide_index] += score_time * _di_fen;//
					GRR._chi_hu_rights[seat_index].set_valid(false);
					GRR._chi_hu_rights[provide_index].opr_or(GameConstants.CHR_JIANGSU_ZZ_YI_PAO_SAN_XIANG);
					GRR._chi_hu_rights[provide_index].opr_or(GameConstants.CHR_FANG_PAO);
				} else {
					GRR._game_score[provide_index] -= score_time * _di_fen;//
					this._end_score[provide_index] -= score_time * _di_fen;//
					GRR._game_score[seat_index] += score_time * _di_fen;
					;//
					this._end_score[seat_index] += score_time * _di_fen;//
					for (int i = 0; i < getTablePlayerNumber(); i++) {
						if (i == seat_index) {
							continue;
						}
						if (i != provide_index) {
							ChiHuRight chr = GRR._chi_hu_rights[i];
							chr.opr_or(GameConstants.CHR_JIANGSU_ZZ_CHU_CHONG);
						}

					}
				}

			}
			if (is_beopei) {
				GRR._chi_hu_rights[provide_index].opr_or(GameConstants.CHR_TONG_PAO);
			} else {
				GRR._chi_hu_rights[provide_index].opr_or(GameConstants.CHR_FANG_PAO);
			}
			if (!(GRR._chi_hu_rights[seat_index].opr_and(GameConstants.CHR_JIANGSU_ZZ_FANG_PAO_ZHENGDA)).is_empty()) {
				_player_result.ming_gang_count[seat_index]++;
				if (has_rule(GameConstants.GAME_RULE_JIANGSU_PEI_CHONG)) {
					if (has_rule(GameConstants.GAME_RULE_JIANGSU_YUAN_ZI)) {
						for (int i = 0; i < getTablePlayerNumber(); i++) {
							if (i == seat_index) {
								continue;
							}
							int score = (int) (_player_result.game_score[i] - _di_fen);
							if (score < -_yuanzi_fen) {
								_player_result.game_score[i] -= _yuanzi_fen + _player_result.game_score[i];
								_player_result.game_score[seat_index] += _yuanzi_fen + _player_result.game_score[i];
								_end_score[i] -= _yuanzi_fen + _player_result.game_score[i];
								_end_score[seat_index] += _yuanzi_fen + _player_result.game_score[i];
							} else {
								_player_result.game_score[i] -= _di_fen;
								_player_result.game_score[seat_index] += _di_fen;
								_end_score[i] -= _di_fen;
								_end_score[seat_index] += _di_fen;
							}
						}

					} else {
						for (int i = 0; i < getTablePlayerNumber(); i++) {
							if (i == seat_index) {
								continue;
							}
							_player_result.game_score[i] -= _di_fen;
							_player_result.game_score[seat_index] += _di_fen;
							_end_score[i] -= _di_fen;
							_end_score[seat_index] += _di_fen;
						}
					}
				} else {
					if (has_rule(GameConstants.GAME_RULE_JIANGSU_YUAN_ZI)) {
						int score = (int) (_player_result.game_score[provide_index] - _di_fen * 3);
						if (score < -_yuanzi_fen) {
							_player_result.game_score[provide_index] -= _yuanzi_fen
									+ _player_result.game_score[provide_index];
							_player_result.game_score[seat_index] += _yuanzi_fen
									+ _player_result.game_score[provide_index];
							_end_score[provide_index] -= _yuanzi_fen + _player_result.game_score[provide_index];
							_end_score[seat_index] += _yuanzi_fen + _player_result.game_score[provide_index];
						} else {
							_player_result.game_score[provide_index] -= _di_fen * 3;
							_player_result.game_score[seat_index] += _di_fen * 3;
							_end_score[provide_index] -= _di_fen * 3;
							_end_score[seat_index] += _di_fen * 3;
						}
					} else {
						int gang_cell = _di_fen;
						if (_b_double && has_rule(GameConstants.GAME_RULE_JIANGSU_DOUBLE)) {
							gang_cell *= 2;
						}
						// if(table._peng_palyer_count[_seat_index][_provide_player]
						// >= 2){
						// gang_cell*=2;
						// }
						_player_result.game_score[provide_index] -= gang_cell * 3;
						_player_result.game_score[seat_index] += gang_cell * 3;
						_end_score[provide_index] -= gang_cell * 3;
						_end_score[seat_index] += gang_cell * 3;
					}
				}
			}

		}
		//
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			_player_result.game_score[i] += GRR._game_score[i];
		}
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			GRR._game_score[i] = 0;
		}
	}

	public void load_game_end_detail(GameEndResponse.Builder gameendResponse) {

		int hu_count = 0;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			ChiHuRight chr = GRR._chi_hu_rights[i];
			if (chr.is_valid()) {
				hu_count++;
			}
		}

		Mj_Game_End.Builder mj_game_end = Mj_Game_End.newBuilder();
		for (int player = 0; player < GameConstants.GAME_PLAYER; player++) {
			USER_MJ_Game_end.Builder user_game_end = USER_MJ_Game_end.newBuilder();
			JS_Mj_Game_End_Detail.Builder detail = JS_Mj_Game_End_Detail.newBuilder();
			ChiHuRight chr = GRR._chi_hu_rights[player];

			if (chr.is_valid()) {
				String hu_detail = "";
				if (!(chr.opr_and(GameConstants.CHR_ZI_MO)).is_empty()
						|| !(chr.opr_and(GameConstants.CHR_JIANGSU_ZZ_AN_GANG_KAI)).is_empty()
						|| !(chr.opr_and(GameConstants.CHR_JIANGSU_ZZ_MING_GANG_KAI)).is_empty()
						|| !(chr.opr_and(GameConstants.CHR_JIANGSU_ZZ_BU_GANG_KAI)).is_empty()) {
					if (!(chr.opr_and(GameConstants.CHR_JIANGSU_ZZ_AN_GANG_KAI)).is_empty()
							|| !(chr.opr_and(GameConstants.CHR_JIANGSU_ZZ_MING_GANG_KAI)).is_empty()
							|| !(chr.opr_and(GameConstants.CHR_JIANGSU_ZZ_BU_GANG_KAI)).is_empty()) {
						hu_detail += "杠上开花 +1 ";// 杠上开花
					}
					if (!(chr.opr_and(GameConstants.CHR_JIANGSU_ZZ_TUO_DA)).is_empty()) {
						if (!(chr.opr_and(GameConstants.CHR_JIANGSU_ZZ_PAO_DA)).is_empty()) {
							hu_detail += "跑搭脱搭 +10 ";// 跑搭
						} else {
							hu_detail += "脱搭自摸 +6 ";// 脱搭
						}
					} else {
						if (!(chr.opr_and(GameConstants.CHR_JIANGSU_ZZ_PAO_DA)).is_empty()) {
							hu_detail += "跑搭 +5 ";// 跑搭
						} else {
							hu_detail += "自摸 +3 ";// 自摸
						}
					}
					if (_da_tou_da[player] > 0) {
						hu_detail += "头搭 +" + _da_tou_da[player];
					}
					if (_da_er_da[player] > 0) {
						hu_detail += "二搭 +" + _da_er_da[player];
					}
					if (_da_peng_da[player] > 0) {
						hu_detail += "碰搭 +" + _da_peng_da[player];
					}

					for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
						if (i == player) {
							continue;
						}
						detail.setScoreType(SCORE_TYPE_HU_TYPE);// 自摸
						if (this._is_bao_pei) {
							detail.setSeatIndex(this.bao_pei_palyer[player]);
						} else if (this._is_gangkai) {
							detail.setSeatIndex(this._bu_gang_provide_player);

						} else {
							detail.setSeatIndex(i);
						}

						if (has_rule(GameConstants.GAME_RULE_JIANGSU_DOUBLE) && this._b_double) {
							detail.setScore(
									(_user_times[player] + _da_tou_da[player] + _da_er_da[player] + _da_peng_da[player])
											* 2);
						} else {
							detail.setScore(
									_user_times[player] + _da_tou_da[player] + _da_er_da[player] + _da_peng_da[player]);
						}

						detail.setDetail(hu_detail);
						user_game_end.addGameEndDetail(detail);
					}
					if (!(chr.opr_and(GameConstants.CHR_JIANGSU_ZZ_ZI_MO_ZHENGDA)).is_empty()) {
						for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
							if (i == player) {
								continue;
							}
							if (has_rule(GameConstants.GAME_RULE_JIANGSU_DOUBLE) && this._b_double) {
								detail.setScore(4);
							} else {
								detail.setScore(2);
							}
							detail.setScoreType(SCORE_TYPE_AN_GANG);// 暗杠
							detail.setSeatIndex(i);
							detail.setScore(2);
							detail.setDetail("暗杠");
							user_game_end.addGameEndDetail(detail);
						}
					}
				} else {

					int fangpao_player = GameConstants.INVALID_SEAT;
					for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
						if (i == player) {
							continue;
						}
						ChiHuRight otherchr = GRR._chi_hu_rights[i];
						if (!(otherchr.opr_and(GameConstants.CHR_FANG_PAO)).is_empty()) {
							fangpao_player = i;
						}
					}
					if (!(chr.opr_and(GameConstants.CHR_JIANGSU_ZZ_FANG_PAO_ZHENGDA)).is_empty()) {
						if (this.has_rule(GameConstants.GAME_RULE_JIANGSU_BAO_CHONG)
								|| this.has_rule(GameConstants.GAME_RULE_JIANGSU_DOUBLE)) {
							if (has_rule(GameConstants.GAME_RULE_JIANGSU_DOUBLE) && this._b_double) {
								detail.setScore(6);
							} else {
								detail.setScore(3);
							}
							detail.setScoreType(SCORE_TYPE_PENG_GANG);// 暗杠
							detail.setSeatIndex(fangpao_player);

							detail.setDetail("明杠");
							user_game_end.addGameEndDetail(detail);
						} else {
							for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
								if (i == player) {
									continue;
								}
								if (has_rule(GameConstants.GAME_RULE_JIANGSU_DOUBLE) && this._b_double) {
									detail.setScore(2);
								} else {
									detail.setScore(1);
								}
								detail.setScoreType(SCORE_TYPE_PENG_GANG);// 暗杠
								detail.setSeatIndex(i);

								detail.setDetail("明杠");
								user_game_end.addGameEndDetail(detail);
							}
						}

					}

					if (this._is_bao_pei || (GRR._chi_hu_rights[fangpao_player]
							.opr_and(GameConstants.CHR_JIANGSU_ZZ_YI_PAO_SAN_XIANG)).is_empty()) {
						for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
							if (i == player) {
								continue;
							}
							hu_detail = "";
							ChiHuRight otherchr = GRR._chi_hu_rights[i];
							if (!this._is_bao_pei || hu_count == 1) {
								if (!(otherchr.opr_and(GameConstants.CHR_FANG_PAO)).is_empty()
										|| !(otherchr.opr_and(GameConstants.CHR_TONG_PAO)).is_empty()) {
									hu_detail += "放冲 +4 ";

									if (_da_tou_da[player] > 0) {
										hu_detail += "头搭 +" + _da_tou_da[player];
									}
									if (_da_er_da[player] > 0) {
										hu_detail += "二搭 +" + _da_er_da[player];
									}
									if (_da_peng_da[player] > 0) {
										hu_detail += "碰搭 +" + _da_peng_da[player];
									}

									detail.setScoreType(SCORE_TYPE_HU_TYPE);// 类型
									detail.setSeatIndex(i);
									if (has_rule(GameConstants.GAME_RULE_JIANGSU_DOUBLE) && this._b_double) {
										detail.setScore((_user_times[player] * 4 + _da_tou_da[player]
												+ _da_er_da[player] + _da_peng_da[player]) * 2);
									} else {
										detail.setScore(_user_times[player] * 4 + _da_tou_da[player] + _da_er_da[player]
												+ _da_peng_da[player]);
									}

									detail.setDetail(hu_detail);
									user_game_end.addGameEndDetail(detail);
								} else if (!(otherchr.opr_and(GameConstants.CHR_JIANGSU_ZZ_CHU_CHONG)).is_empty()) {
									hu_detail += "出冲 +2 ";
									if (_da_tou_da[player] > 0) {
										hu_detail += "头搭 +" + _da_tou_da[player];
									}
									if (_da_er_da[player] > 0) {
										hu_detail += "二搭 +" + _da_er_da[player];
									}
									if (_da_peng_da[player] > 0) {
										hu_detail += "碰搭 +" + _da_peng_da[player];
									}

									detail.setScoreType(SCORE_TYPE_HU_TYPE);// 类型
									if (this.has_rule(GameConstants.GAME_RULE_JIANGSU_BAO_CHONG)
											|| this.has_rule(GameConstants.GAME_RULE_JIANGSU_DOUBLE)) {
										if (this.bao_pei_palyer[player] != GameConstants.INVALID_SEAT) {
											detail.setSeatIndex(this.bao_pei_palyer[player]);
										} else {
											detail.setSeatIndex(fangpao_player);
										}
									} else {
										detail.setSeatIndex(i);
									}
									if (has_rule(GameConstants.GAME_RULE_JIANGSU_DOUBLE) && this._b_double) {
										detail.setScore((_user_times[player] * 2 + _da_tou_da[player]
												+ _da_er_da[player] + _da_peng_da[player]) * 2);
									} else {
										detail.setScore(_user_times[player] * 2 + _da_tou_da[player] + _da_er_da[player]
												+ _da_peng_da[player]);
									}

									detail.setDetail(hu_detail);
									user_game_end.addGameEndDetail(detail);
								}
							} else {
								if (!(otherchr.opr_and(GameConstants.CHR_TONG_PAO)).is_empty()) {
									hu_detail += "出冲 +2 ";

									if (_da_tou_da[player] > 0) {
										hu_detail += "头搭 +" + _da_tou_da[player];
									}
									if (_da_er_da[player] > 0) {
										hu_detail += "二搭 +" + _da_er_da[player];
									}
									if (_da_peng_da[player] > 0) {
										hu_detail += "碰搭 +" + _da_peng_da[player];
									}

									detail.setScoreType(SCORE_TYPE_HU_TYPE);// 类型
									if (this.bao_pei_palyer[player] == i) {
										detail.setSeatIndex(this.bao_pei_palyer[player]);
									} else {
										detail.setSeatIndex(fangpao_player);
									}
									if (has_rule(GameConstants.GAME_RULE_JIANGSU_DOUBLE) && this._b_double) {
										detail.setScore((_user_times[player] * 2 + _da_tou_da[player]
												+ _da_er_da[player] + _da_peng_da[player]) * 2);
									} else {
										detail.setScore(_user_times[player] * 2 + _da_tou_da[player] + _da_er_da[player]
												+ _da_peng_da[player]);
									}

									detail.setDetail(hu_detail);
									user_game_end.addGameEndDetail(detail);
								} else if (!(otherchr.opr_and(GameConstants.CHR_FANG_PAO)).is_empty()) {
									hu_detail += "放冲 +4 ";

									if (_da_tou_da[player] > 0) {
										hu_detail += "头搭 +" + _da_tou_da[player];
									}
									if (_da_er_da[player] > 0) {
										hu_detail += "二搭 +" + _da_er_da[player];
									}
									if (_da_peng_da[player] > 0) {
										hu_detail += "碰搭 +" + _da_peng_da[player];
									}

									detail.setScoreType(SCORE_TYPE_HU_TYPE);// 类型
									if (this.bao_pei_palyer[player] != GameConstants.INVALID_SEAT) {
										detail.setSeatIndex(this.bao_pei_palyer[player]);
									} else {
										detail.setSeatIndex(i);
									}

									if (has_rule(GameConstants.GAME_RULE_JIANGSU_DOUBLE) && this._b_double) {
										detail.setScore((_user_times[player] * 4 + _da_tou_da[player]
												+ _da_er_da[player] + _da_peng_da[player]) * 2);
									} else {
										detail.setScore(_user_times[player] * 4 + _da_tou_da[player] + _da_er_da[player]
												+ _da_peng_da[player]);
									}

									detail.setDetail(hu_detail);
									user_game_end.addGameEndDetail(detail);
								} else if (!(otherchr.opr_and(GameConstants.CHR_JIANGSU_ZZ_CHU_CHONG)).is_empty()) {
									hu_detail += "出冲 +2 ";
									if (_da_tou_da[player] > 0) {
										hu_detail += "头搭 +" + _da_tou_da[player];
									}
									if (_da_er_da[player] > 0) {
										hu_detail += "二搭 +" + _da_er_da[player];
									}
									if (_da_peng_da[player] > 0) {
										hu_detail += "碰搭 +" + _da_peng_da[player];
									}

									detail.setScoreType(SCORE_TYPE_HU_TYPE);// 类型
									if (this.has_rule(GameConstants.GAME_RULE_JIANGSU_BAO_CHONG)
											|| this.has_rule(GameConstants.GAME_RULE_JIANGSU_DOUBLE)) {
										if (this.bao_pei_palyer[player] != GameConstants.INVALID_SEAT) {
											detail.setSeatIndex(this.bao_pei_palyer[player]);
										} else {
											detail.setSeatIndex(fangpao_player);
										}

									} else {
										detail.setSeatIndex(i);
									}
									if (has_rule(GameConstants.GAME_RULE_JIANGSU_DOUBLE) && this._b_double) {
										detail.setScore((_user_times[player] * 2 + _da_tou_da[player]
												+ _da_er_da[player] + _da_peng_da[player]) * 2);
									} else {
										detail.setScore(_user_times[player] * 2 + _da_tou_da[player] + _da_er_da[player]
												+ _da_peng_da[player]);
									}

									detail.setDetail(hu_detail);
									user_game_end.addGameEndDetail(detail);
								}
							}

						}
					}
				}
			}
			// 一炮三响
			int fangpao_player = GameConstants.INVALID_SEAT;
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				ChiHuRight otherchr = GRR._chi_hu_rights[i];
				if (!(otherchr.opr_and(GameConstants.CHR_FANG_PAO)).is_empty()) {
					fangpao_player = i;
				}
			}
			if (fangpao_player != GameConstants.INVALID_SEAT
					&& !(GRR._chi_hu_rights[fangpao_player].opr_and(GameConstants.CHR_JIANGSU_ZZ_YI_PAO_SAN_XIANG))
							.is_empty()
					&& fangpao_player == player) {

				for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
					String hu_detail = "";
					if (i == fangpao_player) {
						continue;
					}
					ChiHuRight otherchr = GRR._chi_hu_rights[fangpao_player];
					if (!(otherchr.opr_and(GameConstants.CHR_JIANGSU_ZZ_YI_PAO_SAN_XIANG)).is_empty()) {
						hu_detail += "一炮三响 +8 ";
						if (_da_tou_da[i] > 0) {
							hu_detail += "头搭 +" + _da_tou_da[i];
						}
						if (_da_er_da[i] > 0) {
							hu_detail += "二搭 +" + _da_er_da[i];
						}
						if (_da_peng_da[i] > 0) {
							hu_detail += "碰搭 +" + _da_peng_da[i];
						}

						detail.setScoreType(SCORE_TYPE_HU_TYPE);// 类型
						detail.setSeatIndex(i);
						if (has_rule(GameConstants.GAME_RULE_JIANGSU_DOUBLE) && this._b_double) {
							detail.setScore((_user_times[i] * 8 + _da_tou_da[i] + _da_er_da[i] + _da_peng_da[i]) * 2);
						} else {
							detail.setScore(_user_times[i] * 8 + _da_tou_da[i] + _da_er_da[i] + _da_peng_da[i]);
						}

						detail.setDetail(hu_detail);
						user_game_end.addGameEndDetail(detail);
					}
				}
			}

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				if (i == player) {
					continue;
				}
				if (_gen_zhang[i] > 0) {
					detail.setScoreType(SCORE_TYPE_GEN_ZHANG);// 跟张
					detail.setSeatIndex(i);
					detail.setScore(_gen_zhang[i]);
					detail.setDetail("跟张");
					user_game_end.addGameEndDetail(detail);
				}
			}
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				if (i == player) {
					continue;
				}
				if (_zi_da_an_gang[i] > 0) {
					detail.setScoreType(SCORE_TYPE_ZI_DA_ANGANG);// 自大暗杠
					detail.setSeatIndex(i);
					detail.setScore(_zi_da_an_gang[i]);
					detail.setDetail("自打暗杠");
					user_game_end.addGameEndDetail(detail);
				}
			}

			int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0;

			if (GRR != null) {
				for (int tmpPlayer = 0; tmpPlayer < GameConstants.GAME_PLAYER; tmpPlayer++) {
					for (int w = 0; w < GRR._weave_count[tmpPlayer]; w++) {
						if (GRR._weave_items[tmpPlayer][w].weave_kind != GameConstants.WIK_GANG
								&& GRR._weave_items[tmpPlayer][w].weave_kind != GameConstants.WIK_AN_GANG) {
							continue;
						}
						if (tmpPlayer == player) {
							if (GRR._weave_items[tmpPlayer][w].provide_player != tmpPlayer) {
								jie_gang++;
								if (this.has_rule(GameConstants.GAME_RULE_JIANGSU_BAO_CHONG)
										|| this.has_rule(GameConstants.GAME_RULE_JIANGSU_DOUBLE)) {
									if (has_rule(GameConstants.GAME_RULE_JIANGSU_DOUBLE) && this._b_double) {
										detail.setScore(6);
									} else {
										detail.setScore(3);
									}
									detail.setScoreType(SCORE_TYPE_PENG_GANG);// 碰杠
									detail.setSeatIndex(GRR._weave_items[tmpPlayer][w].provide_player);
									detail.setDetail("明杠");
									user_game_end.addGameEndDetail(detail);
								} else {
									for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
										if (i == tmpPlayer) {
											continue;
										}
										if (has_rule(GameConstants.GAME_RULE_JIANGSU_DOUBLE) && this._b_double) {
											detail.setScore(2);
										} else {
											detail.setScore(1);
										}
										detail.setScoreType(SCORE_TYPE_PENG_GANG);// 碰杠
										detail.setSeatIndex(i);

										detail.setDetail("明杠");
										user_game_end.addGameEndDetail(detail);
									}
								}

							} else {
								if (GRR._weave_items[tmpPlayer][w].public_card == 1) {
									ming_gang++;
									if (this.has_rule(GameConstants.GAME_RULE_JIANGSU_BAO_CHONG)
											|| this.has_rule(GameConstants.GAME_RULE_JIANGSU_DOUBLE)) {
										if (has_rule(GameConstants.GAME_RULE_JIANGSU_DOUBLE) && this._b_double) {
											detail.setScore(6);
										} else {
											detail.setScore(3);
										}
										detail.setScoreType(SCORE_TYPE_BU_GANG);// 补杠
										detail.setSeatIndex(GRR._weave_items[tmpPlayer][w].provide_player);

										detail.setDetail("补杠");
										user_game_end.addGameEndDetail(detail);
									} else {
										for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
											if (i == tmpPlayer) {
												continue;
											}
											if (has_rule(GameConstants.GAME_RULE_JIANGSU_DOUBLE) && this._b_double) {
												detail.setScore(2);
											} else {
												detail.setScore(1);
											}
											detail.setScoreType(SCORE_TYPE_BU_GANG);// 补杠
											detail.setSeatIndex(i);

											detail.setDetail("补杠");
											user_game_end.addGameEndDetail(detail);
										}
									}

								} else {
									an_gang++;
									for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
										if (i == tmpPlayer) {
											continue;
										}
										if (has_rule(GameConstants.GAME_RULE_JIANGSU_DOUBLE) && this._b_double) {
											detail.setScore(4);
										} else {
											detail.setScore(2);
										}
										detail.setScoreType(SCORE_TYPE_AN_GANG);// 暗杠
										detail.setSeatIndex(i);

										detail.setDetail("暗杠");
										user_game_end.addGameEndDetail(detail);
									}
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
			mj_game_end.addUserGameEnd(user_game_end);
			logger.error(user_game_end.build());
		}
		gameendResponse.setCommResponse(PBUtil.toByteString(mj_game_end));
	}

	@Override
	protected void set_result_describe() {
		int chrTypes;
		long type = 0;
		int win_player = GameConstants.INVALID_SEAT;
		for (int player = 0; player < GameConstants.GAME_PLAYER; player++) {
			StringBuilder gameDesc = new StringBuilder("");
			ChiHuRight chr = GRR._chi_hu_rights[player];
			if (chr.is_valid()) {
				if (!(chr.opr_and(GameConstants.CHR_ZI_MO)).is_empty()
						|| !(chr.opr_and(GameConstants.CHR_JIANGSU_ZZ_AN_GANG_KAI)).is_empty()) {
					gameDesc.append(" 自摸");
					if (type == GameConstants.CHR_JIANGSU_ZZ_PAO_DA) {
						gameDesc.append(" 跑搭");
					}
					if (type == GameConstants.CHR_JIANGSU_ZZ_TUO_DA) {
						gameDesc.append(" 脱搭");
					}
					if (type == GameConstants.CHR_JIANGSU_ZZ_AN_GANG_KAI
							|| type == GameConstants.CHR_JIANGSU_ZZ_MING_GANG_KAI) {
						gameDesc.append(" 杠上开花");
					}
				} else if (!(chr.opr_and(GameConstants.CHR_JIANGSU_ZZ_MING_GANG_KAI)).is_empty()) {
					gameDesc.append(" 杠上开花");
				} else if (!(chr.opr_and(GameConstants.CHR_JIANGSU_ZZ_BU_GANG_KAI)).is_empty()) {
					gameDesc.append(" 杠上开花");
				} else {
					gameDesc.append(" 放冲");
				}
			} else if (!(chr.opr_and(GameConstants.CHR_FANG_PAO)).is_empty()) {
				gameDesc.append(" 放冲");
			} else if (!(chr.opr_and(GameConstants.CHR_JIANGSU_ZZ_CHU_CHONG)).is_empty()
					&& has_rule(GameConstants.GAME_RULE_JIANGSU_PEI_CHONG)) {
				gameDesc.append(" 出冲");
			} else if (!(chr.opr_and(GameConstants.CHR_TONG_PAO)).is_empty()) {
				gameDesc.append(" 三碰包胡");
			}

			if (win_player != GameConstants.INVALID_SEAT) {
				if (_da_tou_da[player] > 0) {
					gameDesc.append(" 头搭x" + _da_tou_da[player]);
				}
				if (_da_er_da[player] > 0) {
					gameDesc.append(" 二搭x" + _da_er_da[player]);
				}
				if (_da_peng_da[player] > 0) {
					gameDesc.append(" 碰搭x" + _da_peng_da[player]);
				}
				win_player = GameConstants.INVALID_SEAT;
			}

			if (_gen_zhang[player] > 0) {
				gameDesc.append(" 跟张x" + _gen_zhang[player]);
			}
			if (_zi_da_an_gang[player] > 0) {
				gameDesc.append(" 自打暗杠x" + _zi_da_an_gang[player]);
			}

			int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0;

			if (GRR != null) {
				for (int tmpPlayer = 0; tmpPlayer < GameConstants.GAME_PLAYER; tmpPlayer++) {
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

			GRR._result_des[player] = gameDesc.toString();
		}
	}

	@Override
	public boolean handler_requst_pao_qiang(Player player, int pao, int qiang) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean handler_requst_nao_zhuang(Player player, int nao) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean handler_requst_message_deal(Player player, int seat_index, RoomRequest room_rq, int type) {
		if (type == MsgConstants.REQUST_MESSAGE_CHAT) {
			Request_message_chat req = PBUtil.toObject(room_rq, Request_message_chat.class);
			handle_requst_message_chat(seat_index, req.getCharMessage());
		}
		return true;
	}

	public boolean handle_requst_message_chat(int seat_index, String message) {

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_ZZMJ_MESSAGE);

		// 发送数据
		Respone_Message_Chat.Builder message_chat = Respone_Message_Chat.newBuilder();
		message_chat.setSeatIndex(seat_index);
		message_chat.setCharMessage(message);

		roomResponse.setCommResponse(PBUtil.toByteString(message_chat));

		this.send_response_to_room(roomResponse);
		return true;
	}

	public boolean exe_dispatch_last_card(int seat_index, int type, int delay_time) {

		if (delay_time > 0) {
			GameSchedule.put(new DispatchLastCardRunnable(this.getRoom_id(), seat_index, type, false), delay_time,
					TimeUnit.MILLISECONDS);// MJGameConstants.GANG_LAST_CARD_DELAY
		} else {
			// 发牌
			if (_handler_dispath_last_card != null) {
				this.set_handler(this._handler_dispath_last_card);
				this._handler_dispath_last_card.reset_status(seat_index, type);
				this._handler.exe(this);
			}
		}

		return true;
	}

	/**
	 * 显示中间出的牌
	 * 
	 * @param seat_index
	 */
	public void runnable_remove_hun_middle_cards(int seat_index) {
		// 牌局未开 或者等待状态 调度不需要执行 add by zain 2017/6/1
		if ((_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) && is_sys())
			return;
		// add end

		// 去掉
		this.operate_show_card(seat_index, GameConstants.Show_Card_Center, 0, null, GameConstants.INVALID_SEAT);

		// 刷新有癞子的牌
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			boolean has_lai_zi = false;
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				if (GRR._cards_index[i][j] > 0 && _logic.is_magic_index(j)) {
					has_lai_zi = true;
					break;
				}
			}
			// 刷新自己手牌
			int cards[] = new int[GameConstants.MAX_COUNT];
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], cards);
			for (int j = 0; j < hand_card_count; j++) {
				if (_logic.is_magic_card(cards[j])) {
					cards[j] += GameConstants.CARD_ESPECIAL_TYPE_HUN;
					_xian_chu_count[i]++;
				}
			}
			this.operate_player_cards(i, hand_card_count, cards, 0, null);

		}
		if (this._b_double && this._lian_zhuang_player != GameConstants.INVALID_SEAT) {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				this.send_error_notify(i, 2, "骰子翻倍");
			}
		}

		// 检测听牌
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			this._playerStatus[i]._hu_card_count = this.get_ting_card(this._playerStatus[i]._hu_cards,
					this.GRR._cards_index[i], this.GRR._weave_items[i], this.GRR._weave_count[i]);
			if (this._playerStatus[i]._hu_card_count > 0) {
				this.operate_chi_hu_cards(i, this._playerStatus[i]._hu_card_count, this._playerStatus[i]._hu_cards);
			}
		}
		this.exe_dispatch_card(this._current_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);

		//
		// this.exe_dispatch_card(seat_index,MJGameConstants.WIK_NULL, 0);
	}

	/**
	 * 取出实际牌数据
	 * 
	 * @param card
	 * @return
	 */
	public int get_real_card(int card) {
		// 错误断言
		if (card > GameConstants.CARD_ESPECIAL_TYPE_HUN && card < GameConstants.CARD_ESPECIAL_TYPE_DA) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_HUN;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_DA && card < GameConstants.CARD_ESPECIAL_TYPE_CI) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_DA;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_TOU_DA && card < GameConstants.CARD_ESPECIAL_TYPE_ER_DA) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_TOU_DA;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_ER_DA
				&& card < GameConstants.CARD_ESPECIAL_TYPE_BEIDONG_DA) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_ER_DA;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_BEIDONG_DA
				&& card < GameConstants.CARD_ESPECIAL_TYPE_PEN_CHANGER) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_BEIDONG_DA;
		}

		return card;
	}

	/**
	 * 调度 发最后4张牌
	 * 
	 * @param cur_player
	 * @param type
	 * @param tail
	 * @return
	 */
	@Override
	public boolean runnable_dispatch_last_card_data(int cur_player, int type, boolean tail) {
		// 牌局未开 或者等待状态 调度不需要执行 add by zain 2017/6/1
		if ((_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) && is_sys())
			return false;

		if (_handler_dispath_last_card != null) {
			// 发牌
			this.set_handler(this._handler_dispath_last_card);
			this._handler_dispath_last_card.reset_status(cur_player, type);
			this._handler.exe(this);
		}

		return true;
	}

	public void rand_tuozi(int seat_index) {
		int num1 = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % 6) + 1;
		int num2 = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % 6) + 1;

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_EFFECT_ACTION);
		roomResponse.setEffectType(GameConstants.Effect_Action_SHAI_ZI);
		roomResponse.setEffectCount(2);
		roomResponse.addEffectsIndex(num1);
		roomResponse.addEffectsIndex(num2);
		roomResponse.setEffectTime(1500);// anim time//摇骰子动画的时间
		roomResponse.setStandTime(500); // delay time//停留时间
		this.send_response_to_room(roomResponse);
		GRR.add_room_response(roomResponse);
		if (num1 == num2) {
			_b_double = true;
			_touzi_count = num1;
		}
		exe_hun(this.GRR._banker_player);
	}

	@Override
	public boolean exe_out_card_bao_ting(int seat_index, int card, int type) {
		// 出牌
		this.set_handler(this._handler_out_card_bao_ting);
		this._handler_out_card_bao_ting.reset_status(seat_index, card, type);
		this._handler.exe(this);

		return true;
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card,
			ChiHuRight chiHuRight, int card_type, int _seat_index) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		// TODO Auto-generated method stub
		return false;
	}
}
