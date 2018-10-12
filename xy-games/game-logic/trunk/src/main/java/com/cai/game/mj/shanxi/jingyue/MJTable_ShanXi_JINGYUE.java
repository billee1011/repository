package com.cai.game.mj.shanxi.jingyue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.GameConstants_SXJY;
import com.cai.common.define.ECardType;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.TuoGuanRunnable;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJType;
import com.cai.game.mj.handler.yongzhou.Tuple;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.game.util.GameUtilConstants;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;
import com.google.common.collect.Maps;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJTable_ShanXi_JINGYUE extends AbstractMJTable {

	boolean[] feng_ed_gang; // 已经杠了风牌（这里指的是杠了东南西北）

	private int _magic_card_count;// 癞子牌数量

	int all_player_gang_count;// 所有人杠的牌的数量,用来判断老牌规则的

	boolean is_feng_zfb_gang; // 已经杠了中发白风牌（只要勾选了风胡玩法，任何一个玩家杠了中发白，那么所有玩家不能进行“东南西北”杠；）

	boolean is_thirteen_and_only_one_gang;// 老牌规则中比较特殊的 介于13张刚好此时要杠的状态

	/**
	 * 山西静乐麻将
	 */
	private static final long serialVersionUID = 1L;

	protected MJHandlerOutCardBaoTing_ShanXi_JINGYUE _handler_bao_ting;

	public MJTable_ShanXi_JINGYUE() {
		super(MJType.GAME_TYPE_MJ_SXKD);
	}

	// 未报听玩家点炮
	public boolean[] no_bao_ting_dian_pao;

	// 报听玩家点炮
	public boolean[] bao_ting_dian_pao;

	public void NoBaoTingDianPaoVaild(int seat_index) {
		no_bao_ting_dian_pao[seat_index] = true;
	}

	public void BaoTingDianPaoVaild(int seat_index) {
		bao_ting_dian_pao[seat_index] = true;
	}

	@Override
	protected void onInitTable() {
		_handler_chi_peng = new MJHandlerChiPeng_ShanXi_JINGYUE();
		_handler_dispath_card = new MJHandlerDispatchCard_ShanXi_JINGYUE();
		_handler_gang = new MJHandlerGang_ShanXi_JINGYUE();
		_handler_out_card_operate = new MJHandlerOutCardOperate_ShanXi_JINGYUE();
		_handler_bao_ting = new MJHandlerOutCardBaoTing_ShanXi_JINGYUE();
	}

	@Override
	public int getTablePlayerNumber() {
		return 4;
	}

	@Override
	public boolean exe_out_card_bao_ting(int seat_index, int card, int type) {
		// 出牌
		this.set_handler(this._handler_bao_ting);
		this._handler_bao_ting.reset_status(seat_index, card, type);
		this._handler.exe(this);

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

	/**
	 * //执行发牌 是否延迟
	 * 
	 * @param seat_index
	 * @param delay
	 * @return
	 */
	public boolean exe_dispatch_card(final int seat_index, final int type, final int card, int delay) {
		if (delay > 0) {
			MJTable_ShanXi_JINGYUE mjTable_LUHE = this;
			GameSchedule.put(new Runnable() {
				@Override
				public void run() {
					set_handler(_handler_dispath_card);
					_handler_dispath_card.reset_status(seat_index, type, card);
					_handler.exe(mjTable_LUHE);
				}
			}, delay, TimeUnit.MILLISECONDS);
		} else {
			// 发牌
			this.set_handler(this._handler_dispath_card);
			this._handler_dispath_card.reset_status(seat_index, type, card);
			this._handler.exe(this);
		}

		return true;
	}

	/**
	 * 处理吃胡的玩家
	 */
	@Override
	public void process_chi_hu_player_operate(int seat_index, int operate_card, boolean rm) {
		// 将MJConstants_HuNan_XiangTan里的胡牌的CHR常量进行存储
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		int effect_count = chr.type_count;
		long effect_indexs[] = new long[effect_count];
		for (int i = 0; i < effect_count; i++) {
			effect_indexs[i] = chr.type_list[i];
		}

		// 客户端弹出胡牌的效果
		this.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, effect_count, effect_indexs, 1, GameConstants.INVALID_SEAT);

		// 手牌删掉
		this.operate_player_cards(seat_index, 0, null, 0, null);

		// 把摸的牌从手牌删掉，结算的时候不显示这张牌的，自摸胡的时候，需要删除，接炮胡时不用
		if (rm) {
			GRR._cards_index[seat_index][_logic.switch_to_card_index(operate_card)]--;
		}

		// 对手牌进行处理
		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], cards);

		// 将胡的牌加到手牌
		cards[hand_card_count] = operate_card + GameConstants.CARD_ESPECIAL_TYPE_HU;
		hand_card_count++;

		this.operate_show_card(seat_index, GameConstants.Show_Card_HU, hand_card_count, cards, GameConstants.INVALID_SEAT);

		return;
	}

	// 杠牌分析 自己摸起来的牌能不能杠
	public int analyse_gang_exclude_magic_card(int cards_index[], int card, WeaveItem WeaveItem[], int cbWeaveCount, GangCardResult gangCardResult,
			int cards_abandoned_gang[], int feng_gang_count, int seat_index) {
		// 设置变量,
		int cbActionMask = GameConstants.WIK_NULL;

		// 如果玩家进行了“东南西北”杠，那么他之后不能进行“中”、“发”、“白”的杠，其他玩家可以正常杠
		boolean can_gang_zfb = true;// 默认可以杠中发白
		if (this.has_rule(GameConstants_SXJY.GAME_RULE_SHANXI_QYS_YTL_QXD_FH_JF)) {
			if (this.feng_ed_gang[seat_index]) {
				for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
					if (cards_index[i] == 4 && (i >= 31 && i <= 33)) {// 如果手上有暗杠
																		// 并且杠的那张牌的索引是中发白
						can_gang_zfb = false;
					}
				}

				for (int i = 0; i < cbWeaveCount; i++) {
					if (WeaveItem[i].weave_kind == GameConstants.WIK_PENG) {
						for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
							if (WeaveItem[i].center_card == _logic.switch_to_card_data(j) && (j >= 31 && j <= 33)) {
								can_gang_zfb = false;
							}
						}
					}
				}

			}
		}

		// 万条筒的暗杠
		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			if (cards_index[i] == 4) {
				cbActionMask |= GameConstants.WIK_GANG;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = _logic.switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;// 暗刚
				gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
			}
		}

		// 风牌的暗杠
		for (int i = GameConstants.MAX_ZI; i < GameConstants.MAX_INDEX; i++) {
			if (cards_index[i] == 4 && can_gang_zfb) {
				cbActionMask |= GameConstants.WIK_GANG;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = _logic.switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;// 暗刚
				gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
			}
		}

		// 万条筒的碰杠
		for (int i = 0; i < cbWeaveCount; i++) {
			if (WeaveItem[i].weave_kind == GameConstants.WIK_PENG) {
				for (int j = 0; j < GameConstants.MAX_ZI; j++) {
					if (cards_index[j] != 1 || cards_abandoned_gang[j] != 0) { // 少于一张牌也直接过滤,能接杠但是不杠的牌也过滤掉
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

		// 风牌的碰杠
		for (int i = 0; i < cbWeaveCount; i++) {
			if (WeaveItem[i].weave_kind == GameConstants.WIK_PENG && can_gang_zfb) {
				for (int j = GameConstants.MAX_ZI; j < GameConstants.MAX_INDEX; j++) {
					if (cards_index[j] != 1 || cards_abandoned_gang[j] != 0) { // 少于一张牌也直接过滤,能接杠但是不杠的牌也过滤掉
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

		// 勾选风胡玩法的
		if (this.has_rule(GameConstants_SXJY.GAME_RULE_SHANXI_QYS_YTL_QXD_FH_JF) && feng_gang_count < 1 && !is_feng_zfb_gang) {
			boolean flag = true;
			for (int i = 27; i < 31; i++) {
				if (cards_index[i] == 0) {
					flag = false;
					break;
				}
			}
			if (flag) {
				cbActionMask |= GameConstants.WIK_FENG_GANG;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = GameConstants_SXJY.DONG_FENG_CARD + GameConstants_SXJY.CARD_ESPECIAL_TYPE_GANG_NAN_FENG;
				gangCardResult.isPublic[index] = 0;// 暗风刚
				gangCardResult.type[index] = GameConstants_SXJY.GANG_TYPE_AN_FENG_GANG;
			}
		}

		return cbActionMask;
	}

	// 游戏开始
	@Override
	protected boolean on_handler_game_start() {
		if (_cur_round == 2) {
			// real_kou_dou();// 记录真实扣豆
		}

		reset_init_data();

		// 随机庄家
		if (_cur_banker == GameConstants.INVALID_SEAT) {
			Random random = new Random();//
			int rand = random.nextInt(6) + 1 + random.nextInt(6) + 1;
			_cur_banker = rand % GameConstants.GAME_PLAYER;

			Player[] player_temp = new Player[getTablePlayerNumber()];
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				int real_seat_index = (_cur_banker + i) % getTablePlayerNumber();
				player_temp[i] = get_players()[real_seat_index];
			}
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				get_players()[i] = player_temp[i];
				get_players()[i].set_seat_index(i);
			}
			RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
			roomResponse2.setGameStatus(_game_status);
			roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
			this.load_player_info_data(roomResponse2);
			this.send_response_to_room(roomResponse2);
			this.GRR.add_room_response(roomResponse2);// 加到回放

			_cur_banker = 0;
		}

		_game_status = GameConstants.GS_MJ_PLAY;

		GRR._banker_player = _cur_banker;
		_current_player = GRR._banker_player;
		banker_count[_current_player]++;
		this.init_shuffle();
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

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			be_in_room_trustee_match(i);
		}

		return on_game_start();
	}

	@Override
	public void be_in_room_trustee_match(int seat_index) {
		if (!is_match() && !isCoinRoom() && !isClubMatch())
			return;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (!istrustee[i] && isCoinRoom()) {
				continue;
			}
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_IS_TRUSTEE);
			roomResponse.setOperatePlayer(i);
			roomResponse.setIstrustee(istrustee[i]);

			send_response_to_player(seat_index, roomResponse);
		}
	}

	@Override
	protected boolean on_game_start() {

		no_bao_ting_dian_pao = new boolean[] { false, false, false, false };
		bao_ting_dian_pao = new boolean[] { false, false, false, false };
		feng_ed_gang = new boolean[getTablePlayerNumber()];
		is_feng_zfb_gang = false;
		_game_status = GameConstants.GS_MJ_PLAY;// 设置状态
		all_player_gang_count = 0;
		is_thirteen_and_only_one_gang = false;

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
			// int index_card = 0;
			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				int real_card = hand_cards[i][j];
				gameStartResponse.addCardData(real_card);
			}

			// 回放数据
			GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			load_room_info_data(roomResponse);
			load_common_status(roomResponse);
			load_player_info_data(roomResponse);

			// if (_cur_round == 1) {
			// load_player_info_data(roomResponse);
			// }

			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setCurrentPlayer(_current_player == GameConstants.INVALID_SEAT ? _resume_player : _current_player);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			roomResponse.setGameStatus(_game_status);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			send_response_to_player(i, roomResponse);
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

		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(this.GRR._left_card_count);
		this.GRR.add_room_response(roomResponse);

		// 检测听牌（暂时不考虑报听的玩法）
		// for (int i = 0; i < this.getTablePlayerNumber(); i++) {
		// _playerStatus[i]._hu_card_count =
		// this.get_ting_card(_playerStatus[i]._hu_cards, GRR._cards_index[i],
		// GRR._weave_items[i], GRR._weave_count[i], i);
		// if (_playerStatus[i]._hu_card_count > 0) {
		// this.operate_chi_hu_cards(i, _playerStatus[i]._hu_card_count,
		// _playerStatus[i]._hu_cards);
		// }
		// }

		this.exe_dispatch_card(this._current_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);
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

		int max_ting_count = GameConstants.MAX_ZI_FENG;

		for (int i = 0; i < max_ting_count; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					GameConstants.HU_CARD_TYPE_ZIMO, seat_index, false)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}

		if (count == 0) {
		} else if (count == max_ting_count) {
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	// 是否听牌
	public boolean is_ting_card(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		// 复制数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();
		for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
			chr.set_empty();
			int cbCurrentCard = _logic.switch_to_card_data(i);
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					GameConstants_SXJY.HU_CARD_TYPE_ZI_MO, seat_index, false))
				return true;
		}
		return false;
	}

	@Override
	protected void init_shuffle() {
		super.init_shuffle();
	}

	/**
	 * 执行完杠后 进行判断别人能不能抢杠胡
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

			// 小于等于5不能抢杠胡
			int card_value = _logic.get_card_value(card);
			int card_color = _logic.get_card_color(card);
			if (card_value <= 5 && card_color < 3) {
				return false;
			}

			// 可以胡的情况 判断
			if (playerStatus.is_chi_hu_round() && this._playerStatus[i].is_bao_ting()) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						GameConstants_SXJY.HU_CARD_TYPE_QIANGGANG, i, false);

				// 结果判断
				if (action != 0) {

					if (this.has_rule(GameConstants_SXJY.GAME_RULE_SHANXI_NENG_HU_BI_HU)) {
						_playerStatus[i].add_action(GameConstants_SXJY.WIK_HIDE_ACTION);
					}

					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
					// chr.opr_or_xt(GameConstants_SXJY.CHR_TYPE_QIANG_GANG,false);//
					// 抢杠胡
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

	/**
	 * 分析出的牌有没有人进行‘吃’、‘碰’、‘杠’、‘胡’，相应的动作，直接在分析方法里处理好了，山西扣点没有吃
	 * 
	 * @param seat_index
	 * @param card
	 * @return
	 */
	public boolean estimate_player_out_card_respond(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;

		// 用户状态
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		// 动作判断
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			// 用户过滤，不判断出牌的人
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			// 牌堆还有牌才能碰和杠，不然流局算庄会出错
			// if (GRR._left_card_count > 0) {
			// 如果已经报听了，打出来的牌就好根据玩法判断了
			if (this._playerStatus[i].is_bao_ting()) {
				// 如果杠完之后所听的牌没有发生改变，就把杠的动作加上去
				if (GRR._left_card_count > 0) {
					if (this.check_gang_can_hu_out_card(i, card)) {
						action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
						// 如果玩家进行了“东南西北”杠，那么他之后不能进行“中”、“发”、“白”的杠，其他玩家可以正常杠
						boolean can_gang_zfb = true;// 默认可以杠中发白
						if (feng_ed_gang[i]) {
							// int card_value = _logic.get_card_value(card);
							if (card >= 53 && card <= 55) {
								can_gang_zfb = false;
							}
						}
						if (action != 0 && action == GameConstants.WIK_GANG && can_gang_zfb) {
							playerStatus.add_action(GameConstants.WIK_GANG);
							playerStatus.add_gang(card, seat_index, 1);
							bAroseAction = true;
						}
					}
				}
				// }
				// 一定是要报听之后，才会有吃胡的显示
				// 胡要过圈
				int left_card = 0;
				if (this.has_rule(GameConstants_SXJY.GAME_RULE_SHANXI_LAO_PAI)) {
					left_card = 12;
				} else {
					left_card = 0;
				}
				if (GRR._left_card_count >= left_card) {
					if (_playerStatus[i].is_chi_hu_round()) {
						boolean can_hu_this_card = true;
						int[] tmp_cards_data = _playerStatus[i].get_cards_abandoned_hu();
						for (int x = 0; x < GameConstants.MAX_ABANDONED_CARDS_COUNT; x++) {
							if (tmp_cards_data[x] == card) {
								can_hu_this_card = false;
								break;
							}
						}
						if (can_hu_this_card) {
							// if (GRR._weave_count[i] == 0) {
							// 吃胡判断
							ChiHuRight chr = GRR._chi_hu_rights[i];
							chr.set_empty();
							int cbWeaveCount = GRR._weave_count[i];

							int card_value = _logic.get_card_value(card);
							int card_color = _logic.get_card_color(card);
							// 非风牌大于5的点数才能点炮
							if (card_value > 5 && card_color < 3) {
								action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
										GameConstants_SXJY.HU_CARD_TYPE_JIE_PAO, i, false);
							}
							// 风牌不作点数的限制
							if (card_color >= 3) {
								action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
										GameConstants_SXJY.HU_CARD_TYPE_JIE_PAO, i, false);
							}
							// 结果判断
							if (action != 0 && action == GameConstants.WIK_CHI_HU) {
								// 添加能胡必胡的规则
								if (this.has_rule(GameConstants_SXJY.GAME_RULE_SHANXI_NENG_HU_BI_HU)) {
									_playerStatus[i].add_action(GameConstants_SXJY.WIK_HIDE_ACTION);
								}

								_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
								_playerStatus[i].add_chi_hu(card, seat_index); // 吃胡的组合
								bAroseAction = true;
							}
							// }
						}
					}
				}
			} else {// 在没有听牌的情况下，碰和杠牌做简单的处理
					// 碰要过圈
				if (GRR._left_card_count > 0) {
					boolean can_peng = true;
					int[] tmp_cards_data = _playerStatus[i].get_cards_abandoned_peng();
					for (int x = 0; x < GameConstants.MAX_ABANDONED_CARDS_COUNT; x++) {
						if (tmp_cards_data[x] == card) {
							can_peng = false;
							break;
						}
					}

					if (this.has_rule(GameConstants_SXJY.GAME_RULE_SHANXI_LAO_PAI)) {
						if (GRR._left_card_count > (this.all_player_gang_count % 2 + 12)) {
							action = _logic.check_peng(GRR._cards_index[i], card);
							if (action != 0 && can_peng) {
								playerStatus.add_action(action);
								playerStatus.add_peng(card, seat_index);
								bAroseAction = true;
							}
						}
					} else {
						if (GRR._left_card_count > 0) {
							action = _logic.check_peng(GRR._cards_index[i], card);
							if (action != 0 && can_peng) {
								playerStatus.add_action(action);
								playerStatus.add_peng(card, seat_index);
								bAroseAction = true;
							}
						}
					}
					// 碰牌判断

					// 风杠要过圈
					boolean can_feng_gang = true;
					int[] tmp_feng_cards_data = _playerStatus[i].get_cards_abandoned_feng_gang();
					for (int x = 0; x < GameConstants.MAX_ABANDONED_CARDS_COUNT; x++) {
						if (tmp_feng_cards_data[x] == card) {
							can_feng_gang = false;
							break;
						}
					}

					if (can_feng_gang) {
						// 杠牌判断
						action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
						// 如果玩家进行了“东南西北”杠，那么他之后不能进行“中”、“发”、“白”的杠，其他玩家可以正常杠
						boolean can_gang_zfb = true;// 默认可以杠中发白
						if (feng_ed_gang[i]) {
							// int card_value = _logic.get_card_value(card);
							if (card >= 53 && card <= 55) {
								can_gang_zfb = false;
							}
						}
						if (action != 0 && can_gang_zfb) {
							playerStatus.add_action(GameConstants.WIK_GANG);
							playerStatus.add_gang(card, seat_index, 1);
							bAroseAction = true;
						}

						int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
						for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
							cbCardIndexTemp[j] = GRR._cards_index[i][j];
						}
						cbCardIndexTemp[_logic.switch_to_card_index(card)]++;

						// 风杠在后面 一般有真杠先杠真杠
						if (this.has_rule(GameConstants_SXJY.GAME_RULE_SHANXI_QYS_YTL_QXD_FH_JF) && this._playerStatus[i]._cards_feng_gang_count < 1
								&& !is_feng_zfb_gang) {
							if (card >= GameConstants_SXJY.DONG_FENG_CARD && card <= GameConstants_SXJY.BEI_FENG_CARD) {
								boolean flag = true;
								for (int k = 27; k < 31; k++) {
									if (cbCardIndexTemp[k] == 0) {
										flag = false;
										break;
									}
								}
								if (flag) {
									playerStatus.add_action(GameConstants.WIK_FENG_GANG);
									playerStatus.add_feng_gang(card + GameConstants_SXJY.CARD_ESPECIAL_TYPE_GANG_NAN_FENG, seat_index, 1);
									bAroseAction = true;
								}
							}

						}

					}
				}
			}

			// }

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

	/**
	 * 封装落地牌组合
	 * 
	 * @param weave_count
	 * @param weaveitems
	 * @param roomResponse
	 */
	public void builderWeaveItemResponse(int weave_count, WeaveItem weaveitems[], RoomResponse.Builder roomResponse) {
		for (int j = 0; j < weave_count; j++) {
			WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
			weaveItem_item.setProvidePlayer(weaveitems[j].provide_player);
			weaveItem_item.setPublicCard(weaveitems[j].public_card);
			weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
			weaveItem_item.setCenterCard(weaveitems[j].center_card);

			// 客户端特殊处理的牌值
			for (int i = 0; i < weaveitems[j].client_special_count; i++) {
				weaveItem_item.addClientSpecialCard(weaveitems[j].client_special_card[i]);
			}

			roomResponse.addWeaveItems(weaveItem_item);
		}
	}

	/**
	 * 分析能胡哪几种牌型，常用的清一色，碰碰胡，七小对，平胡等等
	 * 
	 * @param cards_index
	 * @param weaveItems
	 * @param weave_count
	 * @param cur_card
	 * @param chiHuRight
	 * @param card_type
	 * @param _seat_index
	 * @param need_to_multiply
	 * @return
	 */
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index, boolean need_to_multiply) {
		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		if (_playerStatus[_seat_index].isAbandoned()) {
			return GameConstants.WIK_NULL;
		}

		int cbChiHuKind = GameConstants.WIK_NULL;

		boolean can_win = false; // 是否能胡牌

		// 这些在能胡牌之后，用来设置CHR，否则选美时CHR会出现不能胡牌时的CHR
		boolean has_qi_xiao_dui = false;// 七小对
		boolean has_qing_yi_se = false;// 清一色
		boolean has_yi_tiao_long = false;// 一条龙
		boolean has_qing_yi_se_yi_tiao_long = false;// 清一色一条龙
		boolean has_qing_yi_se_qi_xiao_dui = false;// 清一色七小对
		boolean has_hao_hua_qi_xiao_dui = false;// 豪华七小对
		boolean has_feng_yi_se = false;// 风一色

		// 判断是不是风一色
		int check_is_feng_yi_se = this.is_feng_is_se(cards_index, weaveItems, weave_count, cur_card);
		if (check_is_feng_yi_se != GameConstants.WIK_NULL && check_is_feng_yi_se == GameConstants_SXJY.CHR_HUNAN_FENG_YI_SE) {
			has_feng_yi_se = true;
		}
		// 判断是不是一条龙
		int[] cbCardIndexTemp_YTL = Arrays.copyOf(cbCardIndexTemp, cbCardIndexTemp.length);
		cbCardIndexTemp_YTL[_logic.switch_to_card_index(cur_card)]++;
		if (_logic.is_yi_tiao_long(cbCardIndexTemp_YTL, weave_count)) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_YI_TIAO_LONG);
			has_yi_tiao_long = true;
		}

		// 判断是不是七小对
		int check_qi_xiao_dui = this.is_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card);

		// 判断是否是七小对
		if (check_qi_xiao_dui != GameConstants.WIK_NULL && check_qi_xiao_dui == GameConstants.CHR_HUNAN_QI_XIAO_DUI) {
			can_win = true;
			has_qi_xiao_dui = true;
		}

		// 判断是否是双豪华七对
		if (check_qi_xiao_dui != GameConstants.WIK_NULL && check_qi_xiao_dui == GameConstants_SXJY.CHR_HHQXD) {
			can_win = true;
			has_hao_hua_qi_xiao_dui = true;
		}
		// 判断是不是清一色
		boolean is_qing_yi_se = _logic.is_qing_yi_se(cards_index, weaveItems, weave_count, cur_card);

		// 清一色
		if (is_qing_yi_se) {
			has_qing_yi_se = true;
		}
		// 清一色，七小对
		if (is_qing_yi_se && has_qi_xiao_dui) {
			has_qing_yi_se_qi_xiao_dui = true;
			can_win = true;
		}
		// 清一色，一条龙
		if (is_qing_yi_se && has_yi_tiao_long) {
			has_qing_yi_se_yi_tiao_long = true;
		}

		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_card_count = _logic.get_magic_card_count();

		if (magic_card_count > 2) { // 一般只有两种癞子牌存在
			magic_card_count = 2;
		}

		for (int i = 0; i < magic_card_count; i++) {
			magic_cards_index[i] = _logic.get_magic_card_index(i);
		}

		// 如果发过来的牌的值为1,2 是不能胡的 3,4,5只能自摸的
		int card_value = _logic.get_card_value(cur_card);
		int card_color = _logic.get_card_color(cur_card);
		if (card_value < 3 && card_color < 3) {
			return GameConstants.WIK_NULL;
		}

		boolean bValue = false;
		if (feng_ed_gang[_seat_index]) {
			int[] cbCardIndexTemp_temp = Arrays.copyOf(cbCardIndexTemp, cbCardIndexTemp.length);
			cbCardIndexTemp_temp[_logic.switch_to_card_index(cur_card)]++;
			bValue = checkWangGuiWei(cbCardIndexTemp_temp, chiHuRight, cur_card, 0);
			for (int i = 31; i < 34; i++) {
				if (cbCardIndexTemp_temp[i] == 0) {
					bValue = false;
					break;
				}
			}
		} else {
			bValue = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card), magic_cards_index,
					magic_card_count);
		}

		if (bValue) { // 如果能胡
			can_win = true;
		}

		if (can_win == false) { // 如果不能胡牌
			return GameConstants.WIK_NULL;
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;

		if (card_value >= 3 && card_value <= 5 && card_color < 3) { // 3,4,5只能自摸的,
			if (card_type == GameConstants_SXJY.HU_CARD_TYPE_ZI_MO) {
				chiHuRight.opr_or_xt(GameConstants_SXJY.CHR_TYPE_ZI_MO, need_to_multiply); // 自摸
			}
		} else {
			if (card_type == GameConstants_SXJY.HU_CARD_TYPE_ZI_MO) {
				chiHuRight.opr_or_xt(GameConstants_SXJY.CHR_TYPE_ZI_MO, need_to_multiply); // 自摸
			} else if (card_type == GameConstants_SXJY.HU_CARD_TYPE_JIE_PAO) {
				chiHuRight.opr_or_xt(GameConstants_SXJY.CHR_TYPE_DIAN_PAO, need_to_multiply); // 点炮
			} else if (card_type == GameConstants_SXJY.HU_CARD_TYPE_QIANG_GANG_HU) {
				chiHuRight.opr_or_xt(GameConstants_SXJY.CHR_TYPE_QIANG_GANG, need_to_multiply); // 抢杠胡
			}
		}
		if (has_qi_xiao_dui) { // 七小对
			chiHuRight.opr_or_xt(GameConstants_SXJY.CHR_QI_XIAO_DUI, need_to_multiply);
		}
		if (has_qing_yi_se) { // 清一色
			chiHuRight.opr_or_xt(GameConstants_SXJY.CHR_QING_YI_SE, need_to_multiply);
		}
		if (has_yi_tiao_long) { // 一条龙
			chiHuRight.opr_or_xt(GameConstants_SXJY.CHR_YI_TIAO_LONG, need_to_multiply);
		}
		if (feng_ed_gang[_seat_index]) { // 风胡
			chiHuRight.opr_or_xt(GameConstants_SXJY.CHR_TYPE_FENG_HU, need_to_multiply);
		}

		if (feng_ed_gang[_seat_index] && has_feng_yi_se) { // 风一色
			chiHuRight.opr_or_xt(GameConstants_SXJY.CHR_HUNAN_FENG_YI_SE, need_to_multiply);
		}
		if (has_hao_hua_qi_xiao_dui) { // 豪华七对
			chiHuRight.opr_or_xt(GameConstants_SXJY.CHR_HHQXD, need_to_multiply);
		}
		if (has_qing_yi_se_qi_xiao_dui) { // 清一色七对
			chiHuRight.opr_or_xt(GameConstants_SXJY.CHR_QYS_QXD, need_to_multiply);
		}
		if (has_qing_yi_se_yi_tiao_long) { // 清一色一条龙
			chiHuRight.opr_or_xt(GameConstants_SXJY.CHR_QYS_YTL, need_to_multiply);
		}
		return cbChiHuKind;
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {

	}

	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo, boolean is_qiang_gang_hu) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int wFanShu = 0;
		if (this._logic.get_card_color(operate_card) > 2) {// 如果胡的那张是风牌 番数就是10
			wFanShu = 10;
		} else {
			wFanShu = this._logic.get_card_value(operate_card);// 番数就是胡的那张牌的点数
		}

		countCardType(chr, seat_index);
		// //勾选 清一色、一条龙、七小对、风胡+10 （10+点数）*2
		float extraScore_one = 0;
		float extraScore_two = 0;
		boolean is_twice_add_score_one = false;
		boolean is_twice_add_score_two = false;
		if (this.has_rule(GameConstants_SXJY.GAME_RULE_SHANXI_QYS_YTL_QXD_FH_JF)) {
			if (!chr.opr_and(GameConstants_SXJY.CHR_QING_YI_SE).is_empty()) {
				is_twice_add_score_one = true;
				extraScore_one = 10;
			}

			if (!chr.opr_and(GameConstants_SXJY.CHR_YI_TIAO_LONG).is_empty()) {
				is_twice_add_score_one = true;
				extraScore_one = 10;
			}

			if (!chr.opr_and(GameConstants_SXJY.CHR_QI_XIAO_DUI).is_empty()) {
				is_twice_add_score_one = true;
				extraScore_one = 10;
			}

			if (!chr.opr_and(GameConstants_SXJY.CHR_TYPE_FENG_HU).is_empty()) {
				is_twice_add_score_one = true;
				extraScore_one = 10;
			}

			extraScore_one = extraScore_one + wFanShu;
		}

		// 清一色一条龙、清一色七小对、豪华七小对 风一色+20 （20+点数）*2
		if (this.has_rule(GameConstants_SXJY.GAME_RULE_SHANXI_QYSYTL_QYSQXD_HHQXD)) {
			if (!chr.opr_and(GameConstants_SXJY.CHR_QYS_YTL).is_empty()) {
				is_twice_add_score_two = true;
				extraScore_two = 20;
			}

			if (!chr.opr_and(GameConstants_SXJY.CHR_QYS_QXD).is_empty()) {
				is_twice_add_score_two = true;
				extraScore_two = 20;
			}

			if (!chr.opr_and(GameConstants_SXJY.CHR_HHQXD).is_empty()) {
				is_twice_add_score_two = true;
				extraScore_two = 20;
			}

			if (!chr.opr_and(GameConstants_SXJY.CHR_HUNAN_FENG_YI_SE).is_empty()) {
				is_twice_add_score_two = true;
				extraScore_two = 20;
			}

			extraScore_two = extraScore_two + wFanShu;
		}

		// // 如果胡风胡的话 番数*2
		// if (!chr.opr_and(GameConstants_SXJY.CHR_TYPE_FENG_HU).is_empty()) {
		// wFanShu = wFanShu * 2;
		// }

		////////////////////////////////////////////////////// 自摸 算分
		if (zimo) {
			if (is_twice_add_score_one && !is_twice_add_score_two) {
				float lChiHuScore = extraScore_one * 2;// 勾选 清一色、一条龙、七小对、风胡+10
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (i == seat_index) {
						continue;
					}
					int real_player = i;
					float s = lChiHuScore;
					GRR._game_score[real_player] -= s;
					GRR._game_score[seat_index] += s;
				}
			} else if (is_twice_add_score_two) {
				float lChiHuScore = extraScore_two * 2;// 清一色一条龙、清一色七小对、豪华七小对+20
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (i == seat_index) {
						continue;
					}
					int real_player = i;
					float s = lChiHuScore;
					GRR._game_score[real_player] -= s;
					GRR._game_score[seat_index] += s;
				}
			} else {
				float lChiHuScore = 2 * wFanShu;// 这个是自摸的番数
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (i == seat_index) {
						continue;
					}
					int real_player = i;
					float s = lChiHuScore;
					GRR._game_score[real_player] -= s;
					GRR._game_score[seat_index] += s;
				}
			}

		}
		////////////////////////////////////////////////////// 点炮 算分
		else {// 山西扣点可以抢杠胡，抢杠胡算点炮
			int real_player = provide_index;

			/// 这个时候就要判断出牌人是否已经报停了 ，如果A报听了点炮给D了，ABC三个人都要给钱，没报听点炮了 A就要替BC两家买单了
			if (this._playerStatus[real_player].is_bao_ting()) {
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (i == seat_index)
						continue;
					GRR._chi_hu_rights[real_player].opr_or(GameConstants.CHR_FANG_PAO);
					float s = 0;
					if (is_twice_add_score_one && !is_twice_add_score_two) {
						s = extraScore_one;
					} else if (is_twice_add_score_two) {
						s = extraScore_two;
					} else {
						s = wFanShu;
					}

					// 胡牌分，
					GRR._game_score[i] -= s;
					GRR._game_score[seat_index] += s;
				}
			} else {
				GRR._chi_hu_rights[real_player].opr_or(GameConstants.CHR_FANG_PAO);
				float s = 0;
				if (is_twice_add_score_one && !is_twice_add_score_two) {
					s = extraScore_one;
				} else if (is_twice_add_score_two) {
					s = extraScore_two;
				} else {
					s = wFanShu;
				}

				// 胡牌分，一个人出三家的钱
				GRR._game_score[real_player] -= 3 * s;
				GRR._game_score[seat_index] += 3 * s;
			}
		}

		// 设置变量
		GRR._provider[seat_index] = provide_index;
		_status_gang = false;
		_status_gang_hou_pao = false;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);
	}

	@Override
	protected void countCardType(ChiHuRight chiHuRight, int seat_index) {
		try {
			if (!(chiHuRight.opr_and(GameConstants_SXJY.CHR_SHI_SAN_YAO)).is_empty()) { // 十三幺
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.sx_shisanyao, "", _game_type_index, 0l,
						this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(GameConstants_SXJY.CHR_QING_YI_SE)).is_empty()) { // 清一色
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.sx_qingyise, "", _game_type_index, 0l,
						this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(GameConstants_SXJY.CHR_QI_XIAO_DUI)).is_empty()) { // 七小对
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.sx_qixiaodui, "", _game_type_index, 0l,
						this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(GameConstants_SXJY.CHR_YI_TIAO_LONG)).is_empty()) {
				// 一条龙
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.sx_yitiaolong, "", _game_type_index, 0l,
						this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(GameConstants_SXJY.CHR_TYPE_FENG_HU)).is_empty()) {
				// 风胡
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.sx_fenghu, "", _game_type_index, 0l,
						this.getRoom_id());
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected boolean on_handler_game_finish(int seat_index, int reason) {
		int real_reason = reason;

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
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

		RoomInfo.Builder room_info = getRoomInfo();
		game_end.setRoomInfo(room_info);
		game_end.setRunPlayerId(_run_player_id);
		game_end.setRoundOverType(0);
		game_end.setGamePlayerNumber(this.getTablePlayerNumber());
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

			// ***********************************将杠分拿到游戏结束的时候来算**************************
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				for (int j = 0; j < GRR._weave_count[i]; j++) {
					if (GRR._weave_items[i][j].weave_kind == GameConstants.WIK_GANG) {
						// 山西扣点算分比较特殊 番数是根据杠的哪张牌的点数算的 算分
						int gang_fen = this._logic.get_card_value(GRR._weave_items[i][j].center_card);// 杠的那张牌的点数
						if (this._logic.get_card_color(GRR._weave_items[i][j].center_card) > 2) {
							gang_fen = 10;
						}
						int cbGangIndex = this.GRR._gang_score[i].gang_count++;

						// 判断是不是风牌
						boolean is_feng_card = false;
						if (this._logic.get_card_color(GRR._weave_items[i][j].center_card) > 2) {
							is_feng_card = true;
						}
						if (no_bao_ting_dian_pao[i]) {// **********如果未报听情况下点炮，点炮者的杠做废*************
							this.GRR._gang_score[i].scores[cbGangIndex][i] = 0;
						}
						// else if (bao_ting_dian_pao[i]) { //
						// 如果报听情况下点炮。点炮者的杠分三家给（直杠、补杠、暗杠）,正常情况补杠和暗杆都是三家给的，这里就只考虑直杠的
						// // 如果点炮者的杠是风牌，则每家给10/20分（明杠10分，暗杠20分）
						// if (GRR._weave_items[i][j].type ==
						// GameConstants.GANG_TYPE_JIE_GANG) {// 直杠
						// // ,这种情况下直杠就是三家给杠分了
						// // 放杠1分
						// // 如果荒张的话 杠分都作废
						// if (GameConstants.Game_End_DRAW != reason) {
						// for (int k = 0; k < this.getTablePlayerNumber(); k++)
						// {
						// if (i == k)
						// continue;
						// if (is_feng_card) {
						// this.GRR._gang_score[i].scores[cbGangIndex][i] += 10;
						// this.GRR._gang_score[i].scores[cbGangIndex][k] -= 10;
						//
						// } else {
						// this.GRR._gang_score[i].scores[cbGangIndex][i] +=
						// gang_fen
						// * GameConstants.CELL_SCORE;
						// this.GRR._gang_score[i].scores[cbGangIndex][k] -=
						// gang_fen
						// * GameConstants.CELL_SCORE;
						// }
						//
						// }
						// }
						// this._player_result.ming_gang_count[i]++;
						//
						// }
						// if (GRR._weave_items[i][j].type ==
						// GameConstants.GANG_TYPE_AN_GANG) {// 暗杠
						// if (GameConstants.Game_End_DRAW != reason) {
						// for (int k = 0; k < this.getTablePlayerNumber(); k++)
						// {
						// if (i == k)
						// continue;
						//
						// // 暗杠每人2分
						// if (is_feng_card) {
						// this.GRR._gang_score[i].scores[cbGangIndex][k] -= 20;
						// this.GRR._gang_score[i].scores[cbGangIndex][i] += 20;
						// } else {
						// this.GRR._gang_score[i].scores[cbGangIndex][k] -=
						// gang_fen * 2
						// * GameConstants.CELL_SCORE;
						// this.GRR._gang_score[i].scores[cbGangIndex][i] +=
						// gang_fen * 2
						// * GameConstants.CELL_SCORE;
						//
						// }
						//
						// }
						// }
						// this._player_result.an_gang_count[i]++;
						// }
						// if (GRR._weave_items[i][j].type ==
						// GameConstants.GANG_TYPE_ADD_GANG) {// 补杠
						// if (GameConstants.Game_End_DRAW != reason) {
						// for (int k = 0; k < this.getTablePlayerNumber(); k++)
						// {
						// if (i == k)
						// continue;
						//
						// // 组合牌杠每人1分
						// if (is_feng_card) {
						// this.GRR._gang_score[i].scores[cbGangIndex][k] -= 10;
						// this.GRR._gang_score[i].scores[cbGangIndex][i] += 10;
						// } else {
						// this.GRR._gang_score[i].scores[cbGangIndex][k] -=
						// gang_fen
						// * GameConstants.CELL_SCORE;
						// this.GRR._gang_score[i].scores[cbGangIndex][i] +=
						// gang_fen
						// * GameConstants.CELL_SCORE;
						// }
						//
						// }
						// }
						// this._player_result.ming_gang_count[i]++;
						// }
						// }
						else {// **********************正常情况下的杠分***********************
							if (GRR._weave_items[i][j].type == GameConstants.GANG_TYPE_JIE_GANG) {
								// 放杠1分
								// 判断当前玩家杠这个牌时，提供牌的玩家是不是报听状态
								if (GameConstants.Game_End_DRAW != reason) {
									// 如果勾选了点杠包杠玩法
									if (this.has_rule(GameConstants_SXJY.GAME_RULE_SHANXI_DIAN_GANG_BAO_GANG)) {
										if (GRR._weave_items[i][j].is_lao_gang) { // 报听点杠三家扣分;
											for (int k = 0; k < this.getTablePlayerNumber(); k++) {
												if (i == k)
													continue;
												this.GRR._gang_score[i].scores[cbGangIndex][k] -= gang_fen * GameConstants.CELL_SCORE;
												this.GRR._gang_score[i].scores[cbGangIndex][i] += gang_fen * GameConstants.CELL_SCORE;
											}
										} else {// 不报听点杠包杠（包三家的杠）
											this.GRR._gang_score[i].scores[cbGangIndex][i] += 3 * gang_fen * GameConstants.CELL_SCORE;
											this.GRR._gang_score[i].scores[cbGangIndex][GRR._weave_items[i][j].provide_player] -= 3 * gang_fen
													* GameConstants.CELL_SCORE;
										}
									} else {// 未勾选点杠包杠，谁点杠谁给(修改的规则为：点杠时无论点杠玩家有没有报听，点的杠均是三家付)
										// if
										// (GRR._weave_items[i][j].is_lao_gang)
										// { // 报听点杠还是三家扣分;
										for (int k = 0; k < this.getTablePlayerNumber(); k++) {
											if (i == k)
												continue;
											this.GRR._gang_score[i].scores[cbGangIndex][k] -= gang_fen * GameConstants.CELL_SCORE;
											this.GRR._gang_score[i].scores[cbGangIndex][i] += gang_fen * GameConstants.CELL_SCORE;
										}
										// } else {// 不报听点杠谁点炮谁给
										//
										// this.GRR._gang_score[i].scores[cbGangIndex][i]
										// += gang_fen
										// * GameConstants.CELL_SCORE;
										// this.GRR._gang_score[i].scores[cbGangIndex][GRR._weave_items[i][j].provide_player]
										// -= gang_fen
										// * GameConstants.CELL_SCORE;
										//
										// }

									}

								}
								this._player_result.ming_gang_count[i]++;

							}
							if (GRR._weave_items[i][j].type == GameConstants.GANG_TYPE_AN_GANG) {
								if (GameConstants.Game_End_DRAW != reason) {
									for (int k = 0; k < this.getTablePlayerNumber(); k++) {
										if (i == k)
											continue;

										// 暗杠每人2分
										this.GRR._gang_score[i].scores[cbGangIndex][k] -= gang_fen * 2 * GameConstants.CELL_SCORE;
										this.GRR._gang_score[i].scores[cbGangIndex][i] += gang_fen * 2 * GameConstants.CELL_SCORE;
									}
								}
								this._player_result.an_gang_count[i]++;
							}
							if (GRR._weave_items[i][j].type == GameConstants.GANG_TYPE_ADD_GANG) {
								if (GameConstants.Game_End_DRAW != reason) {
									for (int k = 0; k < this.getTablePlayerNumber(); k++) {
										if (i == k)
											continue;

										// 组合牌杠每人1分
										this.GRR._gang_score[i].scores[cbGangIndex][k] -= gang_fen * GameConstants.CELL_SCORE;
										this.GRR._gang_score[i].scores[cbGangIndex][i] += gang_fen * GameConstants.CELL_SCORE;
									}
								}

								this._player_result.ming_gang_count[i]++;
							}
						}

					}
				}
			}

			// ***********************************将杠分拿到游戏结束的时候来算**************************

			// // 杠牌，每个人的分数
			float lGangScore[] = new float[this.getTablePlayerNumber()];

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {

				for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
					for (int k = 0; k < this.getTablePlayerNumber(); k++) {
						lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人杠的分数
					}
				}

				// 记录
				for (int j = 0; j < this.getTablePlayerNumber(); j++) {
					_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
				}

			}

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				// 每个小局的分数=胡分+杠分
				GRR._game_score[i] += lGangScore[i];
				// 记录每个小局数分数的累加
				_player_result.game_score[i] += GRR._game_score[i];

			}

			this.load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);

			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);
			game_end.setLeftCardCount(GRR._left_card_count);
			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
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

			this.set_result_describe();

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
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

				game_end.addProvidePlayer(GRR._provider[i]);
				game_end.addGameScore(GRR._game_score[i]);
				game_end.addGangScore(lGangScore[i]);
				game_end.addStartHuScore(GRR._start_hu_score[i]);
				game_end.addResultDes(GRR._result_des[i]);

				game_end.addWinOrder(GRR._win_order[i]);

				Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < this.getTablePlayerNumber(); j++) {
					lfs.addItem(GRR._lost_fan_shu[i][j]);
				}

				game_end.addLostFanShu(lfs);

			}

		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round && (!is_sys())) {
				end = true;
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(this.process_player_result(reason));
			} else {
			}
		} else if ((!is_sys()) && (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM)) {
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
			for (int j = 0; j < this.getTablePlayerNumber(); j++) {
				Player player = this.get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");
			}
		}

		if (end && (!is_sys())) {
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
		}

		if (!is_sys()) {
			GRR = null;
		}

		if (is_sys()) {
			clear_score_in_gold_room();
		}
		return false;
	}

	@Override
	protected void set_result_describe() {
		int chrTypes;
		long type = 0;
		int tmp_count = 0;
		Map<Long, Integer> map = null;
		for (int player = 0; player < this.getTablePlayerNumber(); player++) {
			StringBuilder gameDesc = new StringBuilder("");

			chrTypes = GRR._chi_hu_rights[player].type_count;
			map = GRR._chi_hu_rights[player].get_map_for_type_and_count();

			if (GRR._chi_hu_rights[player].is_valid()) {
				for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
					type = GRR._chi_hu_rights[player].type_list[typeIndex];

					if (type == GameConstants_SXJY.CHR_TYPE_ZI_MO) {
						gameDesc.append(" 自摸");
					}
					if (type == GameConstants_SXJY.CHR_TYPE_DIAN_PAO) {
						gameDesc.append(" 接炮");
					}
					if (type == GameConstants_SXJY.CHR_QI_XIAO_DUI) {
						if (map.containsKey((long) GameConstants_SXJY.CHR_QI_XIAO_DUI)) {
							tmp_count = map.get((long) GameConstants_SXJY.CHR_QI_XIAO_DUI);
							gameDesc.append(" 七小对x" + tmp_count);
						} else {
							gameDesc.append(" 七小对");
						}
					}
					if (type == GameConstants_SXJY.CHR_QING_YI_SE) {
						if (map.containsKey((long) GameConstants_SXJY.CHR_QING_YI_SE)) {
							tmp_count = map.get((long) GameConstants_SXJY.CHR_QING_YI_SE);
							gameDesc.append(" 清一色x" + tmp_count);
						} else {
							gameDesc.append(" 清一色");
						}
					}
					if (type == GameConstants_SXJY.CHR_YI_TIAO_LONG) {
						if (map.containsKey((long) GameConstants_SXJY.CHR_YI_TIAO_LONG)) {
							tmp_count = map.get((long) GameConstants_SXJY.CHR_YI_TIAO_LONG);
							gameDesc.append(" 一条龙x" + tmp_count);
						} else {
							gameDesc.append(" 一条龙");
						}
					}
					if (type == GameConstants_SXJY.CHR_TYPE_FENG_HU) {
						if (map.containsKey((long) GameConstants_SXJY.CHR_TYPE_FENG_HU)) {
							tmp_count = map.get((long) GameConstants_SXJY.CHR_TYPE_FENG_HU);
							gameDesc.append(" 风胡x" + tmp_count);
						} else {
							gameDesc.append(" 风胡");
						}
					}

					if (type == GameConstants_SXJY.CHR_QYS_YTL) {
						if (map.containsKey((long) GameConstants_SXJY.CHR_QYS_YTL)) {
							tmp_count = map.get((long) GameConstants_SXJY.CHR_QYS_YTL);
							gameDesc.append(" 清一色一条龙x" + tmp_count);
						} else {
							gameDesc.append(" 清一色一条龙");
						}
					}

					if (type == GameConstants_SXJY.CHR_QYS_QXD) {
						if (map.containsKey((long) GameConstants_SXJY.CHR_QYS_QXD)) {
							tmp_count = map.get((long) GameConstants_SXJY.CHR_QYS_QXD);
							gameDesc.append(" 清一色七小对x" + tmp_count);
						} else {
							gameDesc.append(" 清一色七小对");
						}
					}

					if (type == GameConstants_SXJY.CHR_HHQXD) {
						if (map.containsKey((long) GameConstants_SXJY.CHR_HHQXD)) {
							tmp_count = map.get((long) GameConstants_SXJY.CHR_HHQXD);
							gameDesc.append(" 豪华七小对x" + tmp_count);
						} else {
							gameDesc.append(" 豪华七小对");
						}
					}

					if (type == GameConstants_SXJY.CHR_HUNAN_FENG_YI_SE) {
						if (map.containsKey((long) GameConstants_SXJY.CHR_HUNAN_FENG_YI_SE)) {
							tmp_count = map.get((long) GameConstants_SXJY.CHR_HUNAN_FENG_YI_SE);
							gameDesc.append(" 风一色x" + tmp_count);
						} else {
							gameDesc.append(" 风一色");
						}
					}

					if (type == GameConstants_SXJY.CHR_TYPE_QIANG_GANG) {
						gameDesc.append(" 抢杠胡");
					}
				}
			} else if (!GRR._chi_hu_rights[player].opr_and(GameConstants_SXJY.CHR_FANG_PAO).is_empty()) {
				gameDesc.append(" 放炮");
			}

			int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0;

			if (GRR != null) {
				for (int tmpPlayer = 0; tmpPlayer < this.getTablePlayerNumber(); tmpPlayer++) {
					for (int w = 0; w < GRR._weave_count[tmpPlayer]; w++) {
						if (GRR._weave_items[tmpPlayer][w].weave_kind != GameConstants.WIK_GANG) {
							continue;
						}
						if (tmpPlayer == player) {
							if (GRR._weave_items[tmpPlayer][w].provide_player != tmpPlayer && !GRR._weave_items[tmpPlayer][w].is_add_gang) {
								jie_gang++;
							} else {
								if (GRR._weave_items[tmpPlayer][w].public_card == 1) {
									ming_gang++;
								} else {
									an_gang++;
								}
							}
						} else {
							if (GRR._weave_items[tmpPlayer][w].provide_player == player && !GRR._weave_items[tmpPlayer][w].is_add_gang) {
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

	/**
	 * 检查杠牌后是否换章 返回true表示杠完还是听牌的状态
	 * 
	 * @param seat_index
	 * @param card
	 * @return
	 */
	// 可以换张，只要杠完后仍可以听牌
	public boolean check_gang_can_hu_out_card(int seat_index, int card) {
		int gang_card_index = _logic.switch_to_card_index(card);
		int gang_card_count = GRR._cards_index[seat_index][gang_card_index];

		// 假如杠了
		if (gang_card_count < 3) {
			return false;
		}
		GRR._cards_index[seat_index][gang_card_index] = 0;

		int hu_cards[] = new int[GameConstants.MAX_INDEX];

		// 检查听牌
		int hu_card_count = 0;
		hu_card_count = this.get_ting_card(hu_cards, GRR._cards_index[seat_index], GRR._weave_items[seat_index], GRR._weave_count[seat_index],
				seat_index);
		// 还原手牌
		GRR._cards_index[seat_index][gang_card_index] = gang_card_count;

		int ting_cards[] = this._playerStatus[seat_index]._hu_cards;
		int ting_count = hu_card_count;

		ting_cards[0] = 0;
		this.operate_chi_hu_cards(seat_index, 1, ting_cards);

		// 如果杠完有胡的牌
		if (ting_count > 0) {
			return true;
		}

		return false;
	}

	// 判断已经碰了 然后再摸一张牌 能不能杠后仍听牌
	public boolean check_gang_can_hu_hand_card_ming_gang(int seat_index, int card) {

		int card_index_ming = 0;
		int gang_card_index_dis_card = 0;
		int gang_card_index_dis_card_count = 0;
		boolean can_gang_hand_card_ming = false;//
		boolean dis_card_equal_peng_card = false;//
		int total_gang = 0;// 可以明杠的个数
		int[] array = new int[14];
		// 如果手上一张可以明杠的判断
		for (int i = 0; i < this.GRR._weave_count[seat_index]; i++) {
			if (this.GRR._weave_items[seat_index][i].weave_kind == GameConstants.WIK_PENG) {
				for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
					if (this.GRR._weave_items[seat_index][i].center_card == _logic.switch_to_card_data(j)) {
						card_index_ming = j;
						can_gang_hand_card_ming = true;
						total_gang++;
						if (card_index_ming == _logic.switch_to_card_index(card)) {
							dis_card_equal_peng_card = true;
						}
					}
				}
			}
		}
		if (total_gang > 1) {
			card_index_ming = _logic.switch_to_card_index(card);
		}
		int gang_card_count = GRR._cards_index[seat_index][card_index_ming];
		if (can_gang_hand_card_ming && dis_card_equal_peng_card) {
			GRR._cards_index[seat_index][card_index_ming] = 0;
		} else {
			gang_card_index_dis_card = _logic.switch_to_card_index(card);
			gang_card_index_dis_card_count = GRR._cards_index[seat_index][gang_card_index_dis_card];
			if (gang_card_index_dis_card_count == 1) {
				GRR._cards_index[seat_index][gang_card_index_dis_card]--;
			} else {
				GRR._cards_index[seat_index][gang_card_index_dis_card] = 0;
			}

		}

		int hu_cards[] = new int[GameConstants.MAX_INDEX];

		// 检查听牌
		int hu_card_count = 0;
		hu_card_count = this.get_ting_card(hu_cards, GRR._cards_index[seat_index], GRR._weave_items[seat_index], GRR._weave_count[seat_index],
				seat_index);
		// 还原手牌
		if (can_gang_hand_card_ming && dis_card_equal_peng_card) {
			GRR._cards_index[seat_index][card_index_ming] = gang_card_count;
		} else {
			if (gang_card_index_dis_card_count == 1) {
				GRR._cards_index[seat_index][gang_card_index_dis_card]++;
			} else {
				GRR._cards_index[seat_index][gang_card_index_dis_card] = gang_card_index_dis_card_count;
			}

		}
		int ting_cards[] = this._playerStatus[seat_index]._hu_cards;
		int ting_count = hu_card_count;

		ting_cards[0] = 0;
		this.operate_chi_hu_cards(seat_index, 1, ting_cards);

		// 如果杠完有胡的牌
		if (ting_count > 0) {
			return true;
		}

		return false;

	}

	// 判断手上的牌是否能开杠 并且杠完能胡牌
	public boolean check_gang_can_hu_hand_card(int seat_index, int card) {

		int gang_card_index_dis_card = _logic.switch_to_card_index(card);
		GRR._cards_index[seat_index][gang_card_index_dis_card]--;

		boolean can_gang_hand_card = false;// 能杠自己手上本来就有的四张牌
		// //如果手上就有四张牌可以杠
		int card_index = 0;
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (GRR._cards_index[seat_index][i] == 4) {
				card_index = i;
				can_gang_hand_card = true;
				this.GRR._weave_count[seat_index]++;
			}
		}

		int gang_card_count = GRR._cards_index[seat_index][card_index];
		if (can_gang_hand_card) {
			GRR._cards_index[seat_index][card_index] = 0;
		}
		int hu_cards[] = new int[GameConstants.MAX_INDEX];

		// 检查听牌
		int hu_card_count = 0;
		hu_card_count = this.get_ting_card(hu_cards, GRR._cards_index[seat_index], GRR._weave_items[seat_index], GRR._weave_count[seat_index],
				seat_index);
		// 还原手牌
		if (can_gang_hand_card) {
			GRR._cards_index[seat_index][card_index] = gang_card_count;
			this.GRR._weave_count[seat_index]--;
		}
		GRR._cards_index[seat_index][gang_card_index_dis_card]++;
		int ting_cards[] = this._playerStatus[seat_index]._hu_cards;
		int ting_count = hu_card_count;

		ting_cards[0] = 0;
		this.operate_chi_hu_cards(seat_index, 1, ting_cards);

		// 如果杠完有胡的牌
		if (ting_count > 0) {
			return true;
		}

		return false;

	}

	/**
	 * 检查杠牌后是否换章 返回true表示杠完还是听牌的状态
	 * 
	 * @param seat_index
	 * @param card
	 * @return
	 */
	// 发牌的时候 可以换张，只要杠完后仍可以听牌
	public boolean check_gang_can_hu_dispatch_card(int seat_index, int card) {

		// 如果摸过来的那张牌可以杠
		int gang_card_index = _logic.switch_to_card_index(card);
		int gang_card_count = GRR._cards_index[seat_index][gang_card_index];
		GRR._cards_index[seat_index][gang_card_index] = 0;

		int hu_cards[] = new int[GameConstants.MAX_INDEX];

		// 检查听牌
		int hu_card_count = 0;
		hu_card_count = this.get_ting_card(hu_cards, GRR._cards_index[seat_index], GRR._weave_items[seat_index], GRR._weave_count[seat_index],
				seat_index);
		// 还原手牌
		GRR._cards_index[seat_index][gang_card_index] = gang_card_count;

		int ting_cards[] = this._playerStatus[seat_index]._hu_cards;
		int ting_count = hu_card_count;

		ting_cards[0] = 0;
		this.operate_chi_hu_cards(seat_index, 1, ting_cards);

		// 如果杠完有胡的牌
		if (ting_count > 0) {
			return true;
		}

		return false;
	}

	/**
	 * 检查杠牌后是否换章 返回true表示已经换张了
	 * 
	 * @param seat_index
	 * @param card
	 * @return
	 */
	// 不能换章，需要检测是否改变了听牌
	public boolean check_gang_huan_zhang(int seat_index, int card) {
		int gang_card_index = _logic.switch_to_card_index(card);
		int gang_card_count = GRR._cards_index[seat_index][gang_card_index];
		// 假如杠了
		if (gang_card_count != 4) {
			return true;
		}
		GRR._cards_index[seat_index][gang_card_index] = 0;

		int hu_cards[] = new int[GameConstants.MAX_INDEX];

		// 检查听牌
		int hu_card_count = 0;
		hu_card_count = this.get_ting_card(hu_cards, GRR._cards_index[seat_index], GRR._weave_items[seat_index], GRR._weave_count[seat_index],
				seat_index);
		// 还原手牌
		GRR._cards_index[seat_index][gang_card_index] = gang_card_count;
		// table._playerStatus[_seat_index]._hu_out_cards[ting_count]
		if (hu_card_count != _playerStatus[seat_index]._hu_card_count) {
			return true;
		} else {
			for (int j = 0; j < hu_card_count; j++) {
				int real_card = get_real_card(_playerStatus[seat_index]._hu_cards[j]);
				if (real_card != get_real_card(hu_cards[j])) {
					return true;
				}
			}
		}

		return false;
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x31, 0x09, 0x33, 0x34, 0x35, 0x35, 0x36, 0x36, 0x37, 0x14, 0x15, 0x16, 0x33 };
		int[] cards_of_player1 = new int[] { 0x34, 0x32, 0x33, 0x31, 0x31, 0x31, 0x01, 0x02, 0x03, 0x23, 0x23, 0x36, 0x35 };
		int[] cards_of_player2 = new int[] { 0x31, 0x31, 0x18, 0x18, 0x36, 0x36, 0x06, 0x07, 0x08, 0x09, 0x16, 0x16, 0x16 };
		int[] cards_of_player3 = new int[] { 0x31, 0x31, 0x31, 0x09, 0x09, 0x09, 0x15, 0x15, 0x15, 0x07, 0x07, 0x07, 0x14 };

		// int[] cards_of_player0 = new int[] { 0x32, 0x29, 0x24, 0x34, 0x35,
		// 0x35, 0x35, 0x07, 0x05, 0x06, 0x09, 0x09,
		// 0x09 };
		// int[] cards_of_player1 = new int[] { 0x31, 0x32, 0x33, 0x34, 0x32,
		// 0x32, 0x35, 0x36, 0x37, 0x37, 0x37, 0x25,
		// 0x26 };
		// int[] cards_of_player2 = new int[] { 0x11, 0x11, 0x12, 0x13, 0x14,
		// 0x14, 0x24, 0x16, 0x16, 0x17, 0x17, 0x18,
		// 0x28 };
		// int[] cards_of_player3 = new int[] { 0x15, 0x15, 0x15, 0x29, 0x29,
		// 0x19, 0x05, 0x06, 0x07, 0x17, 0x18, 0x19,
		// 0x19 };

		// int[] cards_of_player0 = new int[] { 0x07, 0x07, 0x07, 0x03, 0x03,
		// 0x03, 0x24, 0x24, 0x14, 0x15, 0x16, 0x04,
		// 0x05 };
		// int[] cards_of_player1 = new int[] { 0x08, 0x31, 0x32, 0x33, 0x34,
		// 0x03, 0x21, 0x22, 0x23, 0x14, 0x14, 0x25,
		// 0x26 };
		// int[] cards_of_player2 = new int[] { 0x31, 0x32, 0x33, 0x27, 0x35,
		// 0x36, 0x37, 0x12, 0x12, 0x15, 0x16, 0x17,
		// 0x28 };
		// int[] cards_of_player3 = new int[] { 0x04, 0x04, 0x04, 0x05, 0x05,
		// 0x05, 0x16, 0x17, 0x18, 0x27, 0x27, 0x28,
		// 0x28 };

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
	public boolean handler_request_trustee(int get_seat_index, boolean isTrustee, int trustee_type) {
		if (_playerStatus == null || istrustee == null) {
			send_error_notify(get_seat_index, 2, "游戏未开始,无法托管!");
			return false;
		}

		// 游戏开始前 无法托管
		if (_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) {
			send_error_notify(get_seat_index, 2, "游戏还未开始,无法托管!");
			return false;
		}

		istrustee[get_seat_index] = isTrustee;

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_IS_TRUSTEE);
		roomResponse.setOperatePlayer(get_seat_index);
		roomResponse.setIstrustee(isTrustee);
		this.send_response_to_room(roomResponse);

		if (GRR != null) {
			GRR.add_room_response(roomResponse);
		}

		if (_handler != null && isTrustee) {
			_handler.handler_be_set_trustee_not_gold(this, get_seat_index);
		}
		return true;

	}

	public boolean handler_request_trustee_action(int get_seat_index, boolean isTrustee) {
		if (_playerStatus == null || istrustee == null) {
			send_error_notify(get_seat_index, 2, "游戏未开始,无法托管!");
			return false;
		}

		// 游戏开始前 无法托管
		if (_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) {
			send_error_notify(get_seat_index, 2, "游戏还未开始,无法托管!");
			return false;
		}

		istrustee[get_seat_index] = isTrustee;

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_IS_TRUSTEE);
		roomResponse.setOperatePlayer(get_seat_index);
		roomResponse.setIstrustee(isTrustee);
		this.send_response_to_room(roomResponse);

		if (GRR != null) {
			GRR.add_room_response(roomResponse);
		}

		return true;
	}

	/**
	 * 重连发送托管状态
	 * 
	 * @param get_seat_index
	 */
	// public void sendIsTruetee(int get_seat_index) {
	// RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
	// roomResponse.setType(MsgConstants.RESPONSE_IS_TRUSTEE);
	// roomResponse.setOperatePlayer(get_seat_index);
	// roomResponse.setIstrustee(istrustee[get_seat_index]);
	// this.send_response_to_room(roomResponse);
	//
	// if (GRR != null) {
	// GRR.add_room_response(roomResponse);
	// }
	// }

	@Override
	public void change_player_status(int seat_index, int st) {
		if (_trustee_schedule == null) {
			_trustee_schedule = new ScheduledFuture[GameConstants.GAME_PLAYER];
		}

		if (_trustee_schedule[seat_index] != null) {
			_trustee_schedule[seat_index].cancel(false);
			_trustee_schedule[seat_index] = null;
		}

		PlayerStatus curPlayerStatus = _playerStatus[seat_index];
		curPlayerStatus.set_status(st);// 操作状态

		if (is_sys() && (st == GameConstants.Player_Status_OPR_CARD || st == GameConstants.Player_Status_OUT_CARD)) {
			_trustee_schedule[seat_index] = GameSchedule.put(new TuoGuanRunnable(getRoom_id(), seat_index), GameConstants.TRUSTEE_TIME_OUT_SECONDS,
					TimeUnit.SECONDS);
		}
	}

	@Override
	public void runnable_set_trustee(int _seat_index) {
		if (_trustee_schedule == null)
			return;

		if (_trustee_schedule[_seat_index] != null) {
			_trustee_schedule[_seat_index] = null;
		}

		if (_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT)
			return;

		handler_request_trustee(_seat_index, true, 0);
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {
		return 0;
	}

	/**
	 * 花色分类
	 * 
	 * @param cardDatas
	 * @return
	 */
	private Map<Integer, List<Integer>> getSameColorMap(Integer[] cardDatas) {

		Map<Integer, List<Integer>> colorCardIndex = Maps.newHashMap();

		List<Integer> singleColor = null;
		for (int i = 0; i < cardDatas.length; i++) {
			if (cardDatas[i] == null)
				continue;
			int card = cardDatas[i];
			Integer color = _logic.get_card_color(card);
			singleColor = colorCardIndex.get(color);
			if (singleColor == null) {
				singleColor = new ArrayList<>();
				colorCardIndex.put(color, singleColor);
			}
			singleColor.add(_logic.get_card_value(card));
		}

		return colorCardIndex;
	}

	private boolean checkWangGuiWei(int[] cbCardIndexTemp, ChiHuRight chiHuRight, int curCard, int magicCount) {

		// 除去王牌的手牌
		Integer[] cardDatas = new Integer[GameConstants.MAX_COUNT];
		int cbPosition = 0;
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (cbCardIndexTemp[i] > 0) {
				for (int j = 0; j < cbCardIndexTemp[i]; j++) {
					cardDatas[cbPosition++] = _logic.switch_to_card_data(i);
				}
			}
		}

		Map<Integer, List<Integer>> sameColorMap = getSameColorMap(cardDatas);
		if (sameColorMap.isEmpty() && magicCount == 2) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_PRIVATE_WANG_CHUANG_WANG);
			return true;
		}
		for (Integer color : sameColorMap.keySet()) {
			int haveMagicCount = magicCount;
			Tuple<Integer, Integer> tuple = getRuleAllHzCount(sameColorMap, color);
			if (haveMagicCount >= tuple.getRight()) {
				haveMagicCount -= tuple.getRight();

				boolean flag = eyeRuleByFuzi(sameColorMap.get(color).toArray(new Integer[sameColorMap.get(color).size()]), haveMagicCount, color,
						chiHuRight, curCard, tuple, magicCount);
				if (flag) {
					if (tuple.getLeft() > 1) {
						chiHuRight.opr_or(GameConstants.CHR_HUNAN_PRIVATE_WANG_GUI_WEI_SHAUNG);
					} else if (tuple.getLeft() == 1) {
						chiHuRight.opr_or(GameConstants.CHR_HUNAN_PRIVATE_WANG_GUI_WEI);
					}
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 从牌组中移除相应的牌
	 * 
	 * @param cardDatas
	 * @param args
	 * @return
	 */
	public Integer[] getCardArrExceptArgs(Integer[] cardDatas, Integer... args) {

		List<Integer> list = new ArrayList<>();

		Integer[] temp = new Integer[cardDatas.length];
		for (int i = 0; i < cardDatas.length; i++) {
			temp[i] = cardDatas[i];
		}

		for (int except : args) {
			for (int i = 0; i < temp.length; i++) {
				if (temp[i] == except) {
					temp[i] = 0;
					break;
				}
			}
		}

		for (int card : temp) {
			if (card != 0) {
				list.add(card);
			}
		}
		return list.toArray(new Integer[list.size()]);
	}

	/**
	 * 将牌分析
	 * 
	 * @param cardDatas
	 * @param magicCount
	 * @param color
	 * @param chiHuRight
	 * @param curCard
	 * @param t
	 * @return
	 */
	private boolean eyeRuleByFuzi(Integer[] cardDatas, int magicCount, int color, ChiHuRight chiHuRight, int curCard, Tuple<Integer, Integer> t,
			int handMagicCount) {
		int length = cardDatas.length;
		// 赖子为0，并且当前花色的个数小于2.肯定不能为将的
		if (magicCount <= 0 && length < 2) {
			return false;
		}

		for (int i = 0; i < length; i++) {
			int card = cardDatas[i];
			if (i + 1 >= length) {
				continue;
			}
			int nextCard = cardDatas[i + 1];
			if (card != nextCard) {
				continue;
			}

			Integer[] newCardDatas = getCardArrExceptArgs(cardDatas, card, nextCard);
			newCardDatas = getCardArrExceptArgs(cardDatas, card, nextCard);
			Tuple<Integer, Integer> tuple = ruleByFuzi(newCardDatas, color);

			if (magicCount < tuple.getRight()) {
				continue;
			}

			if (handMagicCount >= 2) {
				if (_logic.is_magic_card(curCard)) {
					chiHuRight.opr_or(GameConstants.CHR_HUNAN_PRIVATE_WANG_CHUANG_WANG);
				}
				// } else {
				// chiHuRight.opr_or(GameConstants.CHR_HUNAN_PRIVATE_WANG_CHUANG);
				// }
			}
			t.setRight(t.getRight() + tuple.getRight());
			t.setLeft(t.getLeft() + tuple.getLeft());
			return true;
		}

		for (int i = 0; i < length; i++) {
			int card = cardDatas[i];
			if (magicCount > 0) {
				Integer[] newCardDatas = getCardArrExceptArgs(cardDatas, card);
				Tuple<Integer, Integer> tuple = ruleByFuzi(newCardDatas, color);
				tuple.setRight(tuple.getRight() + 1);

				if (magicCount < tuple.getRight()) {
					continue;
				}

				t.setRight(t.getRight() + tuple.getRight());
				t.setLeft(t.getLeft() + tuple.getLeft());
				return true;
			}
		}
		return false;
	}

	/**
	 * 除了jiang的花色，其他花色的牌成为副子需要红中赖子的总数
	 *
	 * @param map
	 * @param jiang
	 * @return
	 */
	private Tuple<Integer, Integer> getRuleAllHzCount(Map<Integer, List<Integer>> map, int jiang) {
		Tuple<Integer, Integer> tuple = new Tuple<>(0, 0);
		for (Integer color : map.keySet()) {
			if (color == jiang) {
				continue;
			}
			List<Integer> list = map.get(color);
			Integer[] ceArr = list.toArray(new Integer[list.size()]);

			Tuple<Integer, Integer> temp = ruleByFuzi(ceArr, color);
			tuple.setLeft(tuple.getLeft() + temp.getLeft());
			tuple.setRight(tuple.getRight() + temp.getRight());
		}

		return tuple;
	}

	/**
	 * 分析牌
	 * 
	 * @param cardDatas
	 * @param color
	 * @return
	 */
	public static Tuple<Integer, Integer> ruleByFuzi(Integer[] cardDatas, int color) {
		int length = cardDatas.length;
		int need = 0;
		int used[] = new int[length];

		Tuple<Integer, Integer> tuple = new Tuple<Integer, Integer>(0, 0);

		int[] card_value = new int[] { 5, 6, 7 };
		for (int c = 0; c < 3 && color == 3; c++) {
			for (int i = 0; i < length; i++) {
				if (cardDatas[i] == card_value[c]) {
					used[i] = 2;
					break;
				}
			}
		}

		int flag = 0;
		for (int i = 0; i < length; i++) {

			int card = cardDatas[i];
			if (used[i] == 1) {
				continue;
			} else {
				int needTemp = 2;
				boolean straight = true;

				int m = -1;
				int n = -1;
				for (int j = i + 1; j < length; j++) {
					int nextCard = cardDatas[j];
					if (used[j] == 1) {
						continue;
					}

					if (j + 1 < length && used[j + 1] == 0) {
						if (nextCard == cardDatas[j + 1]) {
							continue;
						}
					}

					if (((card + 1 == nextCard || card + 2 == nextCard) && color != 3)) {
						needTemp = 1;
						m = i;
						n = j;
						break;
					}
					if (card == nextCard) {
						straight = false;
						needTemp = 1;
						m = i;
						n = j;
						break;
					}
				}

				if (i + 2 < length && card == cardDatas[i + 1] && card == cardDatas[i + 2] && used[i] == 0 && used[i + 1] == 0 && used[i + 2] == 0) {
					needTemp = 0;
					used[i] = used[i + 1] = used[i + 2] = 1;
					m = -1;
					n = -1;
				} else if (color != 3) {
					int a = -1;
					for (int j = i + 1; j < length; j++) {
						int nextcard = cardDatas[j];
						if (card + 1 == nextcard && used[j] == 0) {
							a = j;
						}
						if (card + 2 == nextcard && used[j] == 0 && a != -1) {
							needTemp = 0;
							used[i] = used[j] = used[a] = 1;
							m = -1;
							n = -1;
							break;
						}
					}
				} else if (color == 3 && card >= 5) {

					int a = -1;
					for (int j = i + 1; j < length; j++) {
						int nextcard = cardDatas[j];
						if (card + 1 == nextcard && (used[j] == 0 | used[j] == 2) && a == -1) {
							a = j;
						}
						if (card + 2 == nextcard && (used[j] == 0 | used[j] == 2) && a != -1) {
							needTemp = 0;
							used[i] = used[j] = used[a] = 1;
							m = -1;
							n = -1;

							flag++;
							if (flag > 1) {
								needTemp = 100;
							}
							break;
						}
					}
				}
				if (m != -1 && n != -1) {
					used[m] = used[n] = 1;
				}
				if (straight) {
					tuple.setLeft(tuple.getLeft() + needTemp);
				}
				need = need + needTemp;
			}
		}

		tuple.setRight(need);
		return tuple;
	}

	// 获取动作序列最高等级
	public int get_action_list_rank(int action_count, int action[]) {
		int max_index = 0;

		for (int i = 0; i < action_count; i++) {
			int index = get_action_rank(action[i]);
			if (max_index < index) {
				max_index = index;
			}

		}

		return max_index;
	}

	/**
	 * 获取操作的优先等级
	 * 
	 **/
	// 获取动作等级
	public int get_action_rank(int player_action) {
		// 自摸牌等级
		if (player_action == GameConstants.WIK_ZI_MO) {
			return 50;
		}

		// 吃胡牌等级
		if (player_action == GameConstants.WIK_CHI_HU) {
			return 40;
		}

		// 地胡牌等级
		if (player_action == GameConstants.WIK_CHI_HU) {
			return 40;
		}

		// 硬扣等级
		if (player_action == GameConstants.WIK_YING_KUO) {
			return 35;
		}

		// 杠牌等级
		if (player_action == GameConstants.WIK_GANG) {
			return 30;
		}

		// 补张牌等级
		if (player_action == GameConstants.WIK_BU_ZHNAG) {
			return 30;
		}

		// 笑
		// if (player_action == GameConstants.WIK_MENG_XIAO) {
		// return 30;
		// }
		if (player_action == GameConstants.WIK_DIAN_XIAO) {
			return 30;
		}
		if (player_action == GameConstants.WIK_HUI_TOU_XIAO) {
			return 30;
		}
		if (player_action == GameConstants.WIK_XIAO_CHAO_TIAN) {
			return 30;
		}
		if (player_action == GameConstants.WIK_DA_CHAO_TIAN) {
			return 30;
		}
		if (player_action == GameConstants.WIK_YAO_YI_SE) {
			return 30;
		}

		// 风杠牌等级
		if (player_action == GameConstants.WIK_FENG_GANG) {
			return 25;
		}

		// 碰牌等级
		if (player_action == GameConstants.WIK_PENG) {
			return 20;
		}

		if (player_action == GameConstants.WIK_BAO_TING) {
			return 15;
		}

		// 上牌等级
		if (player_action == GameConstants.WIK_RIGHT || player_action == GameConstants.WIK_CENTER || player_action == GameConstants.WIK_LEFT) {
			return 10;
		}

		return 0;
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

			if (this._magic_card_count > 0) {
				for (int m = 0; m < _magic_card_count; m++) {
					// 王牌过滤
					if (i == _logic.get_magic_card_index(m))
						continue;

					// 单牌统计
					if (cbCardCount == 1 || cbCardCount == 3)
						cbReplaceCount++;

					if (cbCardCount == 4) {
						nGenCount++;
					}
				}
			} else {
				// 单牌统计
				if (cbCardCount == 1 || cbCardCount == 3)
					cbReplaceCount++;

				if (cbCardCount == 4) {
					nGenCount++;
				}
			}
		}

		// 王牌不够
		if (this._magic_card_count > 0) {
			int count = 0;
			for (int m = 0; m < _magic_card_count; m++) {
				count += cbCardIndexTemp[_logic.get_magic_card_index(m)];
			}

			if (cbReplaceCount > count) {
				return GameConstants.WIK_NULL;
			}
		} else {
			if (cbReplaceCount > 0)
				return GameConstants.WIK_NULL;
		}

		if (nGenCount > 0) {
			// 当勾选了豪华七小对的玩法厚 才显示
			if (this.has_rule(GameConstants_SXJY.GAME_RULE_SHANXI_QYSYTL_QYSQXD_HHQXD)) {
				if (nGenCount >= 2) {
					// 双豪华七小对
					return GameConstants.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI;// 双豪华七对
				}
				return GameConstants_SXJY.CHR_HHQXD;// 豪华七对
			} else {
				return GameConstants.CHR_HUNAN_QI_XIAO_DUI;// 七对
			}

		} else {
			return GameConstants.CHR_HUNAN_QI_XIAO_DUI;// 七对
		}

	}

	// 判断是不是风一色
	public int is_feng_is_se(int cards_index[], WeaveItem weaveItems[], int cbWeaveCount, int cur_card) {
		for (int i = 0; i < cbWeaveCount; i++) {
			WeaveItem weaveItem = weaveItems[i];
			int index = _logic.switch_to_card_index(weaveItem.center_card);
			if (index < GameConstants.MAX_ZI) {
				return GameConstants.WIK_NULL;
			}
		}

		// 临时数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}
		// 插入数据
		int cbCurrentIndex = _logic.switch_to_card_index(cur_card);
		cbCardIndexTemp[cbCurrentIndex]++;
		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			if (cbCardIndexTemp[i] > 0) {
				return GameConstants.WIK_NULL;
			}
		}

		return GameConstants.CHR_HUNAN_FENG_YI_SE;
	}

}
