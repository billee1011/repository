package com.cai.game.mj.jiangxi.duchang;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.GameConstants_DC;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.GameDescUtil;
import com.cai.common.util.PerformanceTimer;
import com.cai.dictionary.SysParamDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJType;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.game.util.GameUtilConstants;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.PlayerResultResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJTable_DC extends AbstractMJTable {

	protected MJHandlerSelectMagicCard_DC _handler_select_magic_card;
	private static final long serialVersionUID = 1L;

	private int[] dispatchcardNum; // 摸牌次数
	private int[] huaGangCount; // 花杠次数
	private int[] caiBaoCount; // 载宝次数

	// protected MJHandlerYaoHaiDi_ND _handler_yao_hai_di_nd;

	/** 摸牌数累计 */
	public void addDispatchcardNum(int seat_index) {
		dispatchcardNum[seat_index]++;
	}

	/**
	 * 是否第一次摸牌
	 * 
	 * @param seat_index
	 * @return
	 */
	public boolean isDispatchcardNum(int seat_index) {
		return dispatchcardNum[seat_index] == 1;
	}

	/** 花杠数累计 */
	protected void addHuaGangNum(int seat_index) {
		huaGangCount[seat_index]++;
	}

	/** 裁宝数累计 */
	protected void addCaiBaoNum(int seat_index) {
		caiBaoCount[seat_index]++;
	}

	/**
	 * 花杠数
	 * 
	 * @param seat_index
	 * @return
	 */
	public int getHuaGangNum(int seat_index) {
		return huaGangCount[seat_index];
	}

	/**
	 * 裁宝数
	 * 
	 * @param seat_index
	 * @return
	 */
	public int getCaiBaoNum(int seat_index) {
		return caiBaoCount[seat_index];
	}

	/**
	* 
	*/
	public MJTable_DC() {
		super(MJType.GAME_TYPE_MJ_NINGDU);
	}

	@Override
	public int getTablePlayerNumber() {
		if (playerNumber > 0) {
			return playerNumber;
		}
		if (getRuleValue(GameConstants.GAME_RULE_ALL_GAME_FOUR_PLAYER) != 0) {
			return GameConstants.GAME_PLAYER;
		}
		if (getRuleValue(GameConstants.GAME_RULE_ALL_GAME_TWO_PLAYER) != 0) {
			return GameConstants.GAME_PLAYER - 2;
		}
		return GameConstants.GAME_PLAYER;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.game.mj.AbstractMJTable#onInitTable()
	 */
	@Override
	protected void onInitTable() {
		_handler_dispath_card = new MJHandlerDispatchCard_DC();
		_handler_out_card_operate = new MJHandlerOutCardOperate_DC();
		_handler_gang = new MJHandlerGang_DC();
		_handler_chi_peng = new MJHandlerChiPeng_DC();
		_handler_select_magic_card = new MJHandlerSelectMagicCard_DC();
		// _handler_yao_hai_di_nd = new MJHandlerYaoHaiDi_ND();

	}

	/**
	 * 参数初始化
	 */
	protected void onInitParam() {
		dispatchcardNum = new int[getTablePlayerNumber()];
		huaGangCount = new int[getTablePlayerNumber()];
		caiBaoCount = new int[getTablePlayerNumber()];
	}

	protected void exe_select_magic_card(int banker) {
		this.set_handler(_handler_select_magic_card);
		_handler_select_magic_card.reset_status(banker);
		_handler_select_magic_card.exe(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.game.mj.AbstractMJTable#on_game_start()
	 */
	@Override
	protected boolean on_game_start() {
		onInitParam();
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
			this.load_room_info_data(roomResponse);
			this.load_common_status(roomResponse);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);

			if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_ALL_RANDOM_INDEX)) {
				this.load_room_info_data(roomResponse);
				this.load_common_status(roomResponse);

				if (this._cur_round == 1) {
					this.load_player_info_data(roomResponse);
				}
			}

			roomResponse.setGameStart(gameStartResponse);
			roomResponse
					.setCurrentPlayer(_current_player == GameConstants.INVALID_SEAT ? _resume_player : _current_player);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			roomResponse.setGameStatus(_game_status);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			this.send_response_to_player(i, roomResponse);
		}
		////////////////////////////////////////////////// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		this.load_room_info_data(roomResponse);
		this.load_common_status(roomResponse);
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
		///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		// 定花杠牌和宝牌
		this.exe_select_magic_card(this.GRR._banker_player);

		// 检测听牌
		// for (int i = 0; i < getTablePlayerNumber(); i++) {
		// _playerStatus[i]._hu_card_count =
		// this.get_ting_card(_playerStatus[i]._hu_cards, GRR._cards_index[i],
		// GRR._weave_items[i], GRR._weave_count[i], false, i);
		// if (_playerStatus[i]._hu_card_count > 0) {
		// this.operate_chi_hu_cards(i, _playerStatus[i]._hu_card_count,
		// _playerStatus[i]._hu_cards);
		// }
		// }
		//
		// boolean has_qishou_hu = false;
		// if (!has_qishou_hu) {
		// this.exe_dispatch_card(_current_player, GameConstants.WIK_NULL,
		// GameConstants.DELAY_SEND_CARD_DELAY);
		// }
		return false;
	}

	// 分析是否是烂牌
	public boolean analyse_lan_pai(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card,
			int[] magic_cards_index, int total_magic_card_indexs) {
		if (cur_card == 0)
			return false;
		if (weave_count > 0)
			return false;

		int[] tmp_hand_cards_index = new int[GameUtilConstants.MAX_CARD_TYPE];
		for (int i = 0; i < GameUtilConstants.MAX_CARD_TYPE; i++) {
			tmp_hand_cards_index[i] = cards_index[i];
		}

		int cur_card_index = _logic.switch_to_card_index(cur_card);
		if (cur_card_index >= 0 && cur_card_index < GameUtilConstants.MAX_CARD_TYPE)
			tmp_hand_cards_index[cur_card_index]++;

		int total_magic = 0;
		for (int i = 0; i < total_magic_card_indexs; i++) {
			total_magic += tmp_hand_cards_index[magic_cards_index[i]];
			tmp_hand_cards_index[magic_cards_index[i]] = 0;
		}

		int[] color_index_pre = { -10, -10, -10 };
		for (int i = 0; i < GameUtilConstants.MAX_CARD_TYPE; i++) {
			if (tmp_hand_cards_index[i] >= 2)
				return false;
			if (tmp_hand_cards_index[i] <= 0)
				continue;
			int color = _logic.get_card_color(cur_card);
			if (color < 3) {
				if (i - color_index_pre[color] < 3)
					return false;
				else {
					color_index_pre[color] = i;
				}
			}
		}

		return true;
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card,
			ChiHuRight chiHuRight, int card_type, int _seat_index) {

		if (cur_card == 0)
			return GameConstants.WIK_NULL;

		int cbChiHuKind = GameConstants.WIK_NULL;

		// 宝牌数量
		int baoCardCount = 0;
		int cardIndex = _logic.switch_to_card_index(cur_card);
		if (_logic.get_magic_card_count() > 0) {
			for (int m = 0; m < _logic.get_magic_card_count(); m++) {
				baoCardCount += cards_index[_logic.get_magic_card_index(m)];
				if (cardIndex == _logic.get_magic_card_index(m)) {
					baoCardCount++;
				}
			}
		}

		// 有宝必裁
		if (has_rule(GameConstants_DC.GAME_RULE_CAI_BAO)) {
			if (baoCardCount > 0)
				return GameConstants.WIK_NULL;
		}

		// 最多只能一张宝牌胡牌
		if (baoCardCount > 1)
			return GameConstants.WIK_NULL;

		long qxd = _logic.is_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card);
		if (qxd != GameConstants.WIK_NULL) {
			chiHuRight.opr_or(GameConstants_DC.CHR_QI_XIAO_DUI);
			cbChiHuKind = GameConstants.WIK_CHI_HU;
		}

		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_card_count = _logic.get_magic_card_count();

		if (magic_card_count > 2) { // 一般只有两种癞子牌存在
			magic_card_count = 2;
		}

		for (int i = 0; i < magic_card_count; i++) {
			magic_cards_index[i] = _logic.get_magic_card_index(i);
		}

		// boolean can_win = AnalyseCardUtil.analyse_win_by_cards_index(cards_index,
		// _logic.switch_to_card_index(cur_card),
		// magic_cards_index, magic_card_count);
		boolean can_win = AnalyseCardUtil.analyse_feng_chi_by_cards_index(cards_index,
				_logic.switch_to_card_index(cur_card), magic_cards_index, magic_card_count);

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}
		cbCardIndexTemp[cardIndex]++;

		if (cbCardIndexTemp[cardIndex] > 4) {
			return GameConstants.WIK_NULL;
		}

		// 烂牌检测
		boolean is_lan_pai = false;
		if (_logic.isShiSanLanRJ(cbCardIndexTemp, weaveItems, weave_count)) {
			chiHuRight.opr_or(GameConstants_DC.CHR_SHI_SAN_LAN);
			is_lan_pai = true;
		}
		
		//去除宝牌
		for (int i = 0; i < magic_card_count; i++) {
			cbCardIndexTemp[magic_cards_index[i]] = 0;
		}
		// 幺九牌检测
		boolean is_yao_jiu_pai = false;
		if (_logic.is_yao_jiu(cbCardIndexTemp, weaveItems, weave_count)) {
			chiHuRight.opr_or(GameConstants_DC.CHR_DUCHANG_YAO_JIU);
			is_yao_jiu_pai = true;
		}

		// boolean is_lan_pai=false;
		// is_lan_pai=analyse_lan_pai(cards_index, weaveItems, weave_count,
		// cur_card,magic_cards_index, magic_card_count);
		// if(is_lan_pai) {
		// chiHuRight.opr_or(GameConstants.CHR_HUNAN_SHI_SAN_LAN);
		// }
		if (!can_win && !is_lan_pai && !is_yao_jiu_pai && qxd == GameConstants.WIK_NULL) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		// 碰碰胡
		boolean is_peng_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index(cards_index,
				_logic.switch_to_card_index(cur_card), magic_cards_index, magic_card_count);
		if (is_peng_hu) {
			boolean exist_eat = exist_eat(weaveItems, weave_count);
			if (!exist_eat) {
				chiHuRight.opr_or(GameConstants_DC.CHR_PENGPENG_HU);
			}
		}

		// 硬胡
		// 是否硬胡牌牌型
		int cur_card_index = _logic.switch_to_card_index(cur_card);
		boolean yinghu = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, cur_card_index, magic_cards_index, 0);
		if (yinghu)
			chiHuRight.opr_or(GameConstants_DC.CHR_DUCHANG_YING_HU);

		// 平胡
		if ((qxd == GameConstants.WIK_NULL) && (!is_peng_hu)) {
			chiHuRight.opr_or(GameConstants_DC.CHR_PING_HU);
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;

		return cbChiHuKind;
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		if (!zimo)
			return;

		GRR._chi_hu_card[seat_index][0] = operate_card;
		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		countCardType(chr, seat_index);

		// 统计胡牌分
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (i == seat_index) {
				continue;
			}
			if (seat_index == _cur_banker) {
				GRR._game_score[seat_index] += 3;
				GRR._game_score[i] -= 3;
				GRR._lost_fan_shu[i][seat_index] = 3;
			} else {
				if (i == _cur_banker) {
					GRR._game_score[seat_index] += 3;
					GRR._game_score[i] -= 3;
					GRR._lost_fan_shu[i][seat_index] = 3;
				} else {
					GRR._game_score[seat_index] += 2;
					GRR._game_score[i] -= 2;
					GRR._lost_fan_shu[i][seat_index] = 2;
				}
			}
		}

		// 统计花杠分和杠分
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (i == seat_index) {
				continue;
			} else {
				GRR._game_score[seat_index] += getHuaGangNum(seat_index) + GRR._gang_score[seat_index].gang_count;
				GRR._game_score[i] -= getHuaGangNum(seat_index) + GRR._gang_score[seat_index].gang_count;
			}
		}

		// 统计额外分(烂牌和硬胡)
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (i == seat_index) {
				continue;
			} else {
				if (!(chr.opr_and(GameConstants_DC.CHR_SHI_SAN_LAN)).is_empty()) {
					GRR._game_score[seat_index] += 1;
					GRR._game_score[i] -= 1;
				}
				if (!(chr.opr_and(GameConstants_DC.CHR_DUCHANG_YING_HU)).is_empty()) {
					GRR._game_score[seat_index] += 1;
					GRR._game_score[i] -= 1;
				}
				if (!(chr.opr_and(GameConstants_DC.CHR_DUCHANG_YAO_JIU)).is_empty()) {
					GRR._game_score[seat_index] += 1;
					GRR._game_score[i] -= 1;
				}
			}
		}

		// 栽宝分
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (i == seat_index) {
				continue;
			} else {
				GRR._game_score[seat_index] += getZaiBaoScore(seat_index);
				GRR._game_score[i] -= getZaiBaoScore(seat_index);
			}
		}

		if (provide_index == GameConstants.INVALID_SEAT) {
			GRR._provider[seat_index] = provide_index;
		} else {
			GRR._provider[seat_index] = provide_index;
		}
		// 设置变量
		_status_gang = false;
		_status_gang_hou_pao = false;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);
		return;
	}

	// 胡牌底分
	public int get_chi_hu_fen(ChiHuRight chiHuRight) {
		return 3;
	}

	@Override
	protected void set_result_describe() {
		int l;
		long type = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			String des = "";
			int count = 0;
			l = GRR._chi_hu_rights[i].type_count;
			if (l > 0) {
				des += " 胡牌";
				count++;
			}
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {

					if (type == GameConstants_DC.CHR_PING_HU) {
						des += " 平胡";
						count++;
						if (count % 2 == 0) {
							des += "\r\n";
						}
					}
					if (type == GameConstants_DC.CHR_PENGPENG_HU) {
						des += " 碰碰胡";
						count++;
						if (count % 2 == 0) {
							des += "\r\n";
						}
					}
					if (type == GameConstants_DC.CHR_QI_XIAO_DUI) {
						des += " 七对胡";
						count++;
						if (count % 2 == 0) {
							des += "\r\n";
						}
					}
					if (type == GameConstants_DC.CHR_SHI_SAN_LAN) {
						des += " 烂牌胡";
						count++;
						if (count % 2 == 0) {
							des += "\r\n";
						}
					}
					if (type == GameConstants_DC.CHR_DUCHANG_YAO_JIU) {
						des += " 幺牌";
						count++;
						if (count % 2 == 0) {
							des += "\r\n";
						}
					}
					if (type == GameConstants_DC.CHR_DUCHANG_YING_HU) {
						des += " 无宝当";
						count++;
						if (count % 2 == 0) {
							des += "\r\n";
						}
					}
				}
			}

			if (l > 0) {
				if (getHuaGangNum(i) > 0) {
					des += " 花杠X" + getHuaGangNum(i);
					count++;
					if (count % 2 == 0) {
						des += "\r\n";
					}
				}
				if (getCaiBaoNum(i) > 0) {
					des += " 栽宝X" + getCaiBaoNum(i);
					count++;
					if (count % 2 == 0) {
						des += "\r\n";
					}
				}
			}
			GRR._result_des[i] = des;
		}
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		return false;
	}

	public int get_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, boolean dai_feng,
			int seat_index) {
		PerformanceTimer timer = new PerformanceTimer();

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();
		int count = 0;
		int cbCurrentCard;
		int l = GameConstants.MAX_ZI_FENG;

		for (int i = 0; i < l; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard,
					chr, GameConstants.HU_CARD_TYPE_ZIMO, seat_index)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}

		if (count == 0) {
		} else if (count > 0 && count < l) {
		} else if (count == l) {
			count = 1;
			cards[0] = -1;
		}

		if (timer.get() > 50) {
			logger.warn("cost time too long " + Arrays.toString(cards_index) + ", cost time = " + timer.duration());
		}

		return count;
	}

	public int get_zhong_seat_by_value_three(int nValue, int banker_seat) {
		int seat = 0;
		if (getTablePlayerNumber() >= 3) {
			seat = (banker_seat + (nValue - 1) % getTablePlayerNumber()) % getTablePlayerNumber();
		} else if (getTablePlayerNumber() == 2) {
			int v = (nValue - 1) % 4;
			switch (v) {
			case 0:
				seat = banker_seat;
				break;
			case 1:
				seat = get_null_seat_for_two_player(banker_seat);
				break;
			case 2:
				seat = get_banker_next_seat(banker_seat);
				break;
			default:
				seat = get_null_seat_for_two_player((banker_seat + 1) / getTablePlayerNumber());
				break;
			}
		}
		return seat;
	}

	/**
	 * 玩家动作--通知玩家弹出/关闭操作
	 * 
	 * @param seat_index
	 * @param close
	 * @return
	 */
	public boolean operate_player_action(int seat_index, boolean close, boolean isNotWait) {
		PlayerStatus curPlayerStatus = _playerStatus[seat_index];

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PLAYER_ACTION);
		roomResponse.setTarget(seat_index);
		roomResponse.setIsGoldRoom(isNotWait);// 暂时用金币场这个字段
		this.load_common_status(roomResponse);

		if (close == true) {
			GRR.add_room_response(roomResponse);
			// 通知玩家关闭
			this.send_response_to_player(seat_index, roomResponse);
			return true;
		}
		for (int i = 0; i < curPlayerStatus._action_count; i++) {
			roomResponse.addActions(curPlayerStatus._action[i]);
		}
		// 组合数据
		for (int i = 0; i < curPlayerStatus._weave_count; i++) {
			WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
			weaveItem_item.setCenterCard(curPlayerStatus._action_weaves[i].center_card);
			weaveItem_item.setProvidePlayer(curPlayerStatus._action_weaves[i].provide_player);
			weaveItem_item.setPublicCard(curPlayerStatus._action_weaves[i].public_card);
			weaveItem_item.setWeaveKind(curPlayerStatus._action_weaves[i].weave_kind);
			roomResponse.addWeaveItems(weaveItem_item);
		}
		GRR.add_room_response(roomResponse);
		this.send_response_to_player(seat_index, roomResponse);
		return true;
	}

	// 玩家出牌的动作检测
	public boolean estimate_player_out_card_respond(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		// 用户状态
		for (int i = 0; i < getTablePlayerNumber(); i++) {

			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		// 花杠牌和宝牌不能被操作
		if ((card == GRR._especial_show_cards[0]) || (card == GRR._especial_show_cards[1]))
			return false;

		// 动作判断
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];
			boolean can_peng = true;
			int[] tmp_cards_data = _playerStatus[i].get_cards_abandoned_peng();
			for (int x = 0; x < GameConstants.MAX_ABANDONED_CARDS_COUNT; x++) {
				if (tmp_cards_data[x] == card) {
					can_peng = false;
					break;
				}
			}

			// 碰牌判断
			if (can_peng && !_logic.is_magic_card(card) && !_logic.is_lai_gen_card(card)) {
				action = _logic.check_peng(GRR._cards_index[i], card);
			}
			if (action != 0) {
				playerStatus.add_action(action);
				playerStatus.add_peng(card, seat_index);
				bAroseAction = true;
			}

			if (GRR._left_card_count > 0) {
				// 杠牌判断
				if (!_logic.is_magic_card(card) && !_logic.is_lai_gen_card(card)) {
					action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
				}
				if (action != 0) {
					playerStatus.add_action(GameConstants.WIK_GANG);
					playerStatus.add_gang(card, seat_index, 1);// 加上杠
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

	/**
	 * 
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

			// 可以胡的情况 判断
			if (playerStatus.is_chi_hu_round()) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						GameConstants.HU_CARD_TYPE_QIANGGANG, i);

				// 结果判断
				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);
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
	 * 抓鸟 1 5 9
	 * 
	 * @param cards_data
	 * @param card_num
	 * @return
	 */
	public int get_pick_niao_count(int cards_data[], int card_num) {
		int cbPickNum = 0;
		for (int i = 0; i < card_num; i++) {
			if (!_logic.is_valid_card(cards_data[i])) {
				return 0;
			}

			int nValue = _logic.get_card_value(cards_data[i]);
			if (nValue == 1 || nValue == 5 || nValue == 9) {
				cbPickNum++;
			}

		}
		return cbPickNum;
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
		if (GRR != null) {
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

			int nCaiBaoCount = 0;
			int nHuaGangCount = 0;
			// 统计花杠数，栽宝数
			if (real_reason == GameConstants.Game_End_NORMAL) {
				nHuaGangCount = this._player_result.zhi_gang_count[seat_index];
				nCaiBaoCount = this._player_result.piao_lai_count[seat_index];
			}
			game_end.addPao(nHuaGangCount);
			game_end.addQiang(nCaiBaoCount);

			this.load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);

			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);// 专家
			game_end.setLeftCardCount(GRR._left_card_count);// 剩余牌
			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

			game_end.setCountPickNiao(GRR._count_pick_niao + GRR._count_pick_niao_fei);// 中鸟个数

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				game_end.addHuResult(GRR._hu_result[i]);

				// 胡的牌
				Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
					hc.addItem(GRR._chi_hu_card[i][j]);
				}

				game_end.addHuCardData(GRR._chi_hu_card[i][0]);
				game_end.addHuCardArray(hc);

				// 花杠宝牌
				Int32ArrayResponse.Builder sc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < getHuaGangNum(i); j++) {
					sc.addItem(GRR._especial_show_cards[0]);
				}
				for (int j = 0; j < getCaiBaoNum(i); j++) {
					sc.addItem(GRR._especial_show_cards[1]);
				}
				game_end.addPlayerNiaoCards(sc);
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
						cs.addItem(GRR._cards_data[i][j] + GameConstants.CARD_ESPECIAL_TYPE_GUI);
					} else if (_logic.is_lai_gen_card(GRR._cards_data[i][j])) {
						cs.addItem(GRR._cards_data[i][j] + GameConstants.CARD_ESPECIAL_TYPE_LAI_GEN);
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
			// 累积总赢分
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				_player_result.game_score[i] += GRR._game_score[i];
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

	@Override
	public void test_cards() {
		// int[] cards_of_player0 = new int[] { 0x11, 0x12, 0x13, 0x11, 0x14, 0x15,
		// 0x16, 0x22, 0x22, 0x22, 0x27, 0x27,
		// 0x27 };
		//
		// int[] cards_of_player1 = new int[] { 0x11, 0x14, 0x17, 0x05, 0x08, 0x01,
		// 0x23, 0x26, 0x29, 0x31, 0x34, 0x36,
		// 0x37 };
		// int[] cards_of_player3 = new int[] { 0x11, 0x11, 0x12, 0x13, 0x14, 0x15,
		// 0x15, 0x15, 0x22, 0x22, 0x27, 0x27,
		// 0x27 };
		// int[] cards_of_player2 = new int[] { 0x11, 0x11, 0x12, 0x13, 0x14, 0x15,
		// 0x15, 0x15, 0x22, 0x22, 0x27, 0x27,
		// 0x27 };

		int[] cards_of_player0 = new int[] { 0x11, 0x11, 0x17, 0x19, 0x19, 0x19, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36,
				0x37 };
		int[] cards_of_player1 = new int[] { 0x05, 0x06, 0x07, 0x09, 0x09, 0x17, 0x18, 0x05, 0x06, 0x07, 0x35, 0x35,
				0x35 };
		int[] cards_of_player2 = new int[] { 0x11, 0x11, 0x19, 0x19, 0x19, 0x19, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36,
				0x37 };
		int[] cards_of_player3 = new int[] { 0x11, 0x11, 0x19, 0x19, 0x19, 0x19, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36,
				0x37 };

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
				GRR._cards_index[1][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[2][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
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

	/**
	 * 刷新玩家的牌--手牌 和 牌堆上的牌
	 * 
	 * @param seat_index
	 * @param card_count
	 * @param cards
	 *            实际的牌数据(手上的牌)
	 * @param weave_count
	 * @param weaveitems
	 *            牌堆中的组合(落地的牌)
	 * @return
	 */
	public boolean operate_player_cards(int seat_index, int card_count, int cards[], int weave_count,
			WeaveItem weaveitems[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS);// 12更新玩家手牌数据
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(1);

		this.load_common_status(roomResponse);

		// 手牌数量
		roomResponse.setCardCount(card_count);
		roomResponse.setWeaveCount(weave_count);
		// 组合牌
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

		this.send_response_to_other(seat_index, roomResponse);

		// 手牌--将自己的手牌数据发给自己
		for (int j = 0; j < card_count; j++) {
			roomResponse.addCardData(cards[j]);
		}
		GRR.add_room_response(roomResponse);
		// 自己才有牌数据
		this.send_response_to_player(seat_index, roomResponse);

		return true;
	}

	@Override
	public boolean handler_release_room(Player player, int opr_code) {
		if (is_sys()) {
			return handler_release_room_in_gold(player, opr_code);
		}
		int seat_index = 0;
		if (player != null) {
			seat_index = player.get_seat_index();
		}

		SysParamModel sysParamModel3007 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(313).get(3007);
		int delay = 60;
		if (sysParamModel3007 != null) {
			delay = sysParamModel3007.getVal1();
		}

		switch (opr_code) {
		case GameConstants.Release_Room_Type_SEND: {
			// 发起解散
			if (GameConstants.GS_MJ_FREE == _game_status) {
				// 游戏还没开始,不能发起解散
				return false;
			}
			// 有人申请解散了
			if (_gameRoomRecord.request_player_seat != GameConstants.INVALID_SEAT) {
				return false;
			}

			// 取消之前的调度
			if (_release_scheduled != null)
				_release_scheduled.cancel(false);
			_request_release_time = System.currentTimeMillis() + delay * 1000;
			if (GRR == null) {
				_release_scheduled = GameSchedule.put(new GameFinishRunnable(this.getRoom_id(), seat_index,
						GameConstants.Game_End_RELEASE_WAIT_TIME_OUT), delay, TimeUnit.SECONDS);
			} else {
				_release_scheduled = GameSchedule.put(new GameFinishRunnable(this.getRoom_id(), seat_index,
						GameConstants.Game_End_RELEASE_PLAY_TIME_OUT), delay, TimeUnit.SECONDS);
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				_gameRoomRecord.release_players[i] = 0;
			}

			_gameRoomRecord.request_player_seat = seat_index;
			_gameRoomRecord.release_players[seat_index] = 1;// 同意
			int count = 0;
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (_gameRoomRecord.release_players[i] == 1) {// 都同意了

					count++;
				}
			}
			if (count == getTablePlayerNumber()) {
				if (GRR == null) {
					this.handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_RESULT);
				} else {
					this.handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_PLAY);
				}

				for (int j = 0; j < getTablePlayerNumber(); j++) {
					player = this.get_players()[j];
					if (player == null)
						continue;
					send_error_notify(j, 1, "游戏解散成功!");

				}
				return true;
			}

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_PLAYER_RELEASE);
			roomResponse.setReleaseTime(delay);
			roomResponse.setOperateCode(0);
			roomResponse.setRequestPlayerSeat(_gameRoomRecord.request_player_seat);
			roomResponse.setLeftTime(delay);
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				roomResponse.addReleasePlayers(_gameRoomRecord.release_players[i]);
			}
			this.send_response_to_room(roomResponse);

			return false;
		}

		case GameConstants.Release_Room_Type_AGREE: {
			if (GameConstants.GS_MJ_FREE == _game_status) {
				// 游戏还没开始,不能同意解散
				return false;
			}
			// 没人发起解散
			if (_gameRoomRecord.request_player_seat == GameConstants.INVALID_SEAT) {
				return false;
			}
			if (_gameRoomRecord.release_players[seat_index] == 1)
				return false;

			_gameRoomRecord.release_players[seat_index] = 1;

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_PLAYER_RELEASE);
			roomResponse.setReleaseTime(delay);
			roomResponse.setRequestPlayerSeat(_gameRoomRecord.request_player_seat);
			roomResponse.setOperateCode(1);
			roomResponse.setLeftTime((_request_release_time - System.currentTimeMillis()) / 1000);
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				roomResponse.addReleasePlayers(_gameRoomRecord.release_players[i]);
			}

			this.send_response_to_room(roomResponse);

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (get_players()[i] == null)
					continue;
				if (_gameRoomRecord.release_players[i] != 1) {
					return false;// 有一个不同意
				}
			}

			for (int j = 0; j < getTablePlayerNumber(); j++) {
				player = this.get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");

			}

			if (_release_scheduled != null)
				_release_scheduled.cancel(false);
			_release_scheduled = null;
			if (GRR == null) {
				this.handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_RESULT);
			} else {
				this.handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_PLAY);
			}

			return false;
		}

		case GameConstants.Release_Room_Type_DONT_AGREE: {
			if (GameConstants.GS_MJ_FREE == _game_status) {
				// 游戏还没开始,不能同意解散
				return false;
			}
			if (_gameRoomRecord.request_player_seat == GameConstants.INVALID_SEAT) {
				return false;
			}
			_gameRoomRecord.release_players[seat_index] = 2;
			_gameRoomRecord.request_player_seat = GameConstants.INVALID_SEAT;

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setReleaseTime(delay);
			roomResponse.setType(MsgConstants.RESPONSE_PLAYER_RELEASE);
			roomResponse.setRequestPlayerSeat(_gameRoomRecord.request_player_seat);
			roomResponse.setOperateCode(1);
			int l = (int) (_request_release_time - System.currentTimeMillis() / 1000);
			if (l <= 0) {
				l = 1;
			}
			roomResponse.setLeftTime(l);
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				roomResponse.addReleasePlayers(_gameRoomRecord.release_players[i]);
			}
			this.send_response_to_room(roomResponse);

			_request_release_time = 0;
			_gameRoomRecord.request_player_seat = GameConstants.INVALID_SEAT;
			if (_release_scheduled != null)
				_release_scheduled.cancel(false);
			_release_scheduled = null;
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				_gameRoomRecord.release_players[i] = 0;
			}

			for (int j = 0; j < getTablePlayerNumber(); j++) {
				player = this.get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散失败!玩家[" + this.get_players()[seat_index].getNick_name() + "]不同意解散");

			}

			return false;
		}

		case GameConstants.Release_Room_Type_CANCEL: {// 房主未开始游戏 解散
			if (GameConstants.GS_MJ_FREE != _game_status) {
				// 游戏已经来时
				return false;
			}
			if (_cur_round == 0) {

				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);// 直接拉出游戏
				this.send_response_to_room(roomResponse);

				for (int i = 0; i < getTablePlayerNumber(); i++) {
					Player p = this.get_players()[i];
					if (p == null)
						continue;
					if (i == seat_index) {
						send_error_notify(i, 2, "游戏已解散");
					} else {
						send_error_notify(i, 2, "游戏已被创建者解散");
					}

				}
				this.huan_dou(GameConstants.Game_End_RELEASE_NO_BEGIN);
				// 删除房间
				PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
			} else {
				return false;

			}
		}
			break;
		case GameConstants.Release_Room_Type_QUIT: {// 玩家未开始游戏 退出
			if (GameConstants.GS_MJ_FREE != _game_status) {
				// 游戏已经开始
				return false;
			}
			RoomResponse.Builder quit_roomResponse = RoomResponse.newBuilder();
			quit_roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);// 直接拉出游戏
			send_response_to_player(seat_index, quit_roomResponse);

			send_error_notify(seat_index, 2, "您已退出该游戏");
			this.get_players()[seat_index] = null;
			_player_ready[seat_index] = 0;
			// WalkerGeek 允许少人模式
			super.init_less_param();

			if (player != null) {
				PlayerServiceImpl.getInstance().quitRoomId(this.getRoom_id(), player.getAccount_id());
			}

			if (_kick_schedule != null) {
				_kick_schedule.cancel(false);
				_kick_schedule = null;
			}
			// 刷新玩家
			RoomResponse.Builder refreshroomResponse = RoomResponse.newBuilder();
			refreshroomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);
			this.load_player_info_data(refreshroomResponse);
			//
			send_response_to_other(seat_index, refreshroomResponse);

			// 通知代理
			this.refresh_room_redis_data(GameConstants.PROXY_ROOM_PLAYER, false);
		}
			break;
		case GameConstants.Release_Room_Type_PROXY: {
			// 游戏还没开始,不能解散
			if (GameConstants.GS_MJ_FREE != _game_status) {
				return false;
			}

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);// 直接拉出游戏
			this.send_response_to_room(roomResponse);

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				Player p = this.get_players()[i];
				if (p == null)
					continue;
				send_error_notify(i, 2, "游戏已被创建者解散");

			}
			this.huan_dou(GameConstants.Game_End_RELEASE_NO_BEGIN);
			// 删除房间
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());

		}
			break;
		}

		return true;

	}

	// 庄家选择
	public void progress_banker_select() {
		if (_cur_round < 2) {
			Random random = new Random();
			int rand = random.nextInt(6) + 1 + random.nextInt(6) + 1;
			_cur_banker = rand % getTablePlayerNumber();

			_shang_zhuang_player = GameConstants.INVALID_SEAT;
			_lian_zhuang_player = GameConstants.INVALID_SEAT;
		}
	}

	// 局数总结算
	public PlayerResultResponse.Builder process_player_result(int result) {

		this.huan_dou(result);

		// 大赢家
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			_player_result.win_order[i] = -1;
		}
		int win_idx = 0;
		float max_score = 0;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			int winner = -1;
			float s = -999999;
			for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
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

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			player_result.addGameScore(_player_result.game_score[i]);
			player_result.addWinOrder(_player_result.win_order[i]);

			Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
				lfs.addItem(_player_result.lost_fan_shu[i][j]);
			}

			player_result.addLostFanShu(lfs);

			player_result.addZiMoCount(_player_result.zi_mo_count[i]);
			player_result.addAnGangCount(_player_result.an_gang_count[i]);
			player_result.addMingGangCount(_player_result.ming_gang_count[i]);

			// 载宝
			player_result.addPiaoLaiCount(_player_result.piao_lai_count[i]);
			// 花杠
			player_result.addSevenCount(_player_result.zhi_gang_count[i]);
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

		RoomPlayerResponse.Builder room_player = RoomPlayerResponse.newBuilder();
		room_player.setAccountId(this.getCreate_player().getAccount_id());
		room_player.setHeadImgUrl(this.getCreate_player().getAccount_icon());
		room_player.setIp(this.getCreate_player().getAccount_ip());
		room_player.setUserName(this.getCreate_player().getNick_name());
		room_player.setSeatIndex(this.getCreate_player().get_seat_index());
		room_player.setOnline(this.getCreate_player().isOnline() ? 1 : 0);
		room_player.setIpAddr(this.getCreate_player().getAccount_ip_addr());
		room_player.setSex(this.getCreate_player().getSex());
		room_player.setScore(0);
		room_player.setReady(0);
		room_player.setPao(0);
		room_player.setQiang(0);
		if (this.getCreate_player().locationInfor != null) {
			room_player.setLocationInfor(this.getCreate_player().locationInfor);
		}
		player_result.setCreatePlayer(room_player);
		return player_result;
	}

	// 统计宝牌分
	public int getZaiBaoScore(int seat_index) {
		int nCaiBaoScore = 0;
		switch (getCaiBaoNum(seat_index)) {
		case 0:
			nCaiBaoScore = 0;
			break;
		case 1:
			nCaiBaoScore = 10;
			break;
		case 2:
			nCaiBaoScore = 30;
			break;
		case 3:
			nCaiBaoScore = 60;
			break;
		case 4:
			nCaiBaoScore = 100;
			break;
		default:
			nCaiBaoScore = 0;
			break;
		}
		return nCaiBaoScore;
	}

	/**
	 * 允许少人模式扩展
	 */
	@Override
	public boolean handler_requst_open_less(Player player, int playerNum) {
		if (GameConstants.GS_MJ_FREE != _game_status) {
			return false;
		}

		// 不允许一个人开启游戏，兼容客户端bug
		if (playerNum == 1) {
			return false;
		}

		// 判断规则是否存在
		if (getRuleValue(GameConstants.GAME_RULE_ALL_GAME_FOUR_PLAYER) == 0) {
			return false;
		}
		// 已经开局 返回
		if (this._cur_round != 0) {
			return false;
		}

		int readys = 0;
		int openLess = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (_player_ready[i] == 1) {
				readys++;
			}
			if (_player_open_less[i] == 1) {
				openLess++;
			}
		}
		// 变更少人模式数组
		int less = playerNum < getTablePlayerNumber() ? 1 : 0;
		if ((openLess + 1) == playerNum) {

			this.changePlayer();
			for (int j = 0; j < this.get_players().length; j++) {
				if (this.get_players()[j] != null) {
					_player_open_less[j] = less;
				} else {
					_player_open_less[j] = 0;
				}
			}

			playerNumber = playerNum;
			// 最小开局人数
			this.syncMinPlayerCountToClub(getTablePlayerNumber());
			this.refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, true);
		} else if (playerNum == GameConstants.INVALID_SEAT) {// 取消勾选少人模式
			less = 0;
			playerNumber = playerNum;
			_player_open_less[player.get_seat_index()] = less;
			// 最小开局人数
			this.syncMinPlayerCountToClub(getTablePlayerNumber());
			this.refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, true);
		} else {
			_player_open_less[player.get_seat_index()] = less;
		}
		// 通知客户端
		this.refresh_less_player();

		if ((openLess + 1) == readys && readys == playerNum) {
			this.changePlayer();
			playerNumber = playerNum;
			this.refresh_less_player();
			// 最小开局人数
			this.syncMinPlayerCountToClub(getTablePlayerNumber());

			handler_game_start();
			this.refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, true);
		}

		return false;
	}

	public void showSpecialCard(int seat_index) {
		// 获取宝牌
		int index = _logic.get_magic_card_index(0);
		if (index < 0 && index >= GameConstants.MAX_ZI_FENG) {
			return;
		}
		int nBaoCard = _logic.switch_to_card_data(index);

		boolean isValid = false;
		for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
			if (_logic.is_lai_gen_card(_logic.switch_to_card_data(i))) {
				isValid = true;
				index = i;
				break;
			}
		}
		if (!isValid) {
			return;
		}
		int nHuaGangCard = _logic.switch_to_card_data(index);
		operate_show_card(seat_index, GameConstants.Show_Card_Center, 2,
				new int[] { nHuaGangCard + GameConstants.CARD_ESPECIAL_TYPE_LAI_GEN,
						nBaoCard + GameConstants.CARD_ESPECIAL_TYPE_GUI },
				GameConstants.INVALID_SEAT);

		GameSchedule.put(new Runnable() {
			@Override
			public void run() {
				// 将翻出来的牌从牌桌的正中央移除
				operate_show_card(seat_index, GameConstants.Show_Card_Center, 0, null, GameConstants.INVALID_SEAT);
			}
		}, 500, TimeUnit.MILLISECONDS);
	}

	/**
	 * 刷新玩家的牌
	 * 
	 * @param seat_index
	 * @param card_count
	 * @param cards
	 * @param weave_count
	 * @param weaveitems
	 * @return
	 */
	public boolean operate_player_cards_with_ting(int seat_index, int card_count, int cards[], int weave_count,
			WeaveItem weaveitems[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS);// 12更新玩家手牌数据
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(1);

		this.load_common_status(roomResponse);

		// 手牌数量
		roomResponse.setCardCount(card_count);
		roomResponse.setWeaveCount(weave_count);
		// 组合牌
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

		this.send_response_to_other(seat_index, roomResponse);

		// 手牌
		for (int j = 0; j < card_count; j++) {
			if (_logic.is_magic_card(cards[j])) {
				cards[j] += GameConstants.CARD_ESPECIAL_TYPE_GUI;
			} else if (_logic.is_lai_gen_card(cards[j])) {
				cards[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_GEN;
			}
			roomResponse.addCardData(cards[j]);
		}

		int ting_count = _playerStatus[seat_index]._hu_out_card_count;

		// 打出去可以听牌的个数
		roomResponse.setOutCardCount(ting_count);

		for (int i = 0; i < ting_count; i++) {
			int ting_card_cout = _playerStatus[seat_index]._hu_out_card_ting_count[i];
			roomResponse.addOutCardTingCount(ting_card_cout);
			roomResponse.addOutCardTing(_playerStatus[seat_index]._hu_out_card_ting[i]);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < ting_card_cout; j++) {
				int_array.addItem(_playerStatus[seat_index]._hu_out_cards[i][j]);
			}
			roomResponse.addOutCardTingCards(int_array);
		}

		// 自己才有牌数据
		this.send_response_to_player(seat_index, roomResponse);

		GRR.add_room_response(roomResponse);

		return true;
	}
}
