package com.cai.game.mj.shanximj;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.RedisConstant;
import com.cai.common.constant.game.Constants_ShanXi;
import com.cai.common.define.ECardType;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.GameDescUtil;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.SpringService;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.redis.service.RedisService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

/**
 * 陕西麻将
 * 
 * @author admin
 *
 */
public class Table_ShanXi extends AbstractMJTable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public MJHandlerQiShou_ShanXi _handler_qishou_hongzhong;

	public MJHandlerPao_ShanXi _handler_pao_henna_hz;// 河南红中跑

	public int di_fen;

	public int pao_zi[];

	@Override
	protected void onInitTable() {
		// 初始化基础牌局handler
		_handler_dispath_card = new MJHandlerDispatchCard_ShanXi();
		_handler_out_card_operate = new MJHandlerOutCardOperate_ShanXi();
		_handler_gang = new MJHandlerGang_ShanXi();
		_handler_chi_peng = new MJHandlerChiPeng_ShanXi();

		_handler_qishou_hongzhong = new MJHandlerQiShou_ShanXi();

		_handler_pao_henna_hz = new MJHandlerPao_ShanXi();

		pao_zi = new int[GameConstants.GAME_PLAYER_FOUR];
		set_pao_zi();
	}

	@Override
	protected boolean on_handler_game_start() {
		if (_cur_round == 2) {
			// real_kou_dou();// 记录真实扣豆
		}

		reset_init_data();

		// 庄家选择
		this.progress_banker_select();

		_game_status = GameConstants.GS_MJ_PLAY;

		// 信阳麻将
		GRR._banker_player = _cur_banker;
		// 发牌
		init_shuffle();

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

		}
		// 游戏开始时 初始化 未托管
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			istrustee[i] = false;
		}

		on_game_start();
		return false;
	}

	@Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x04, 0x04, 0x12, 0x12, 0x15, 0x15, 0x16, 0x16, 0x18, 0x18, 0x22, 0x22, 0x01 };
		int[] cards_of_player1 = new int[] { 0x02, 0x01, 0x01, 0x01, 0x05, 0x09, 0x11, 0x13, 0x15, 0x17, 0x19, 0x22, 0x23 };
		int[] cards_of_player2 = new int[] { 0x02, 0x02, 0x02, 0x01, 0x11, 0x15, 0x15, 0x14, 0x12, 0x12, 0x12, 0x18, 0x15 };
		int[] cards_of_player3 = new int[] { 0x02, 0x02, 0x02, 0x01, 0x11, 0x15, 0x15, 0x14, 0x12, 0x12, 0x12, 0x18, 0x15 };

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}

		for (int j = 0; j < 13; j++) {
			if (getTablePlayerNumber() == 4) {
				GRR._cards_index[0][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[1][_logic.switch_to_card_index(cards_of_player1[j])] += 1;
				GRR._cards_index[2][_logic.switch_to_card_index(cards_of_player2[j])] += 1;
				GRR._cards_index[3][_logic.switch_to_card_index(cards_of_player3[j])] += 1;
			} else if (getTablePlayerNumber() == 3) {

				GRR._cards_index[0][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[1][_logic.switch_to_card_index(cards_of_player1[j])] += 1;
				GRR._cards_index[2][_logic.switch_to_card_index(cards_of_player2[j])] += 1;
			} else if (getTablePlayerNumber() == 2) {
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
	protected boolean on_game_start() {
		di_fen = 1;
		Arrays.fill(pao_zi, 0);
		if (xia_pao_zi()) {
			this.set_handler(this._handler_pao_henna_hz);
			this._handler_pao_henna_hz.exe(this);
			return true;
		}

		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		// gameStartResponse.setSiceIndex(rand);
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[GameConstants.GAME_PLAYER][GameConstants.MAX_COUNT];
		// 发送数据
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards[i]);
			for (int j = 0; j < hand_card_count; j++) {
				if (_logic.is_magic_card(hand_cards[i][j])) {
					hand_cards[i][j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				}
			}
			gameStartResponse.addCardsCount(hand_card_count);
		}

		// 发送数据
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
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
			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setCurrentPlayer(_current_player == GameConstants.INVALID_SEAT ? _resume_player : _current_player);
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
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
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

		// 检测听牌
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			_playerStatus[i]._hu_card_count = this.get_hnhz_ting_card_new(_playerStatus[i]._hu_cards, GRR._cards_index[i], GRR._weave_items[i],
					GRR._weave_count[i], false);
			if (_playerStatus[i]._hu_card_count > 0) {
				this.operate_chi_hu_cards(i, _playerStatus[i]._hu_card_count, _playerStatus[i]._hu_cards);
			}
		}
		boolean is_qishou_hu = false;

		// 处理每个玩家手上的牌，如果有王牌，处理一下
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int[] hand_cards1 = new int[GameConstants.MAX_COUNT];
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards1);
			for (int j = 0; j < hand_card_count; j++) {
				if (_logic.is_magic_card(hand_cards1[j])) {
					hand_cards1[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				}
			}
			// 玩家客户端刷新一下手牌
			operate_player_cards(i, hand_card_count, hand_cards1, 0, null);
		}
		this.exe_dispatch_card(GRR._banker_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);
		// }
		// this.exe_dispatch_card(_current_player,true);

		return false;
	}

	public int get_hnhz_ting_card_new(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, boolean dai_feng) {
		PerformanceTimer timer = new PerformanceTimer();

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int l = GameConstants.MAX_INDEX - GameConstants.CARD_HUA_COUNT - GameConstants.CARD_FENG_COUNT;
		int ql = l;
		dai_feng = false;
		if (has_rule(Constants_ShanXi.GAME_RULE_DAI_FENG_PAI)) {
			dai_feng = true;
		}
		if (dai_feng) {
			l += GameConstants.CARD_FENG_COUNT;
			ql += (GameConstants.CARD_FENG_COUNT - 1);
		}
		for (int i = 0; i < l; i++) {
			if (this._logic.is_magic_index(i))
				continue;
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					Constants_ShanXi.HU_CARD_TYPE_ZI_MO, 0)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}

		l -= 1;
		if (count == 0) {
			// if (has_rule(Constants_ShanXi.GAME_RULE_HONG_ZHONG_LAI_ZI)) {
			// if (cards_index[this._logic.get_magic_card_index(0)] == 3) {
			// cards[count] =
			// _logic.switch_to_card_data(this._logic.get_magic_card_index(0));
			// count++;
			// }
			// }
		} else if (count > 0 && count < ql) {
			if (has_rule(Constants_ShanXi.GAME_RULE_HONG_ZHONG_LAI_ZI)) {
				cards[count] = _logic.switch_to_card_data(this._logic.get_magic_card_index(0));
				count++;
			}
		} else {
			count = 1;
			cards[0] = -1;
		}

		if (timer.get() > 50) {
			logger.warn("cost time too long " + Arrays.toString(cards_index) + ", cost time = " + timer.duration());
		}

		return count;
	}

	public boolean exe_qishou_hongzhong(int seat_index) {
		this.set_handler(this._handler_qishou_hongzhong);
		this._handler_qishou_hongzhong.reset_status(seat_index);
		this._handler_qishou_hongzhong.exe(this);

		return true;
	}

	@Override
	public int analyse_chi_hu_card(int cards_index[], WeaveItem weaveItems[], int weaveCount, int cur_card, ChiHuRight chiHuRight, int card_type,
			int seatIndex) {
		int cbChiHuKind = GameConstants.WIK_NULL;
		if (cur_card == 0) {
			return cbChiHuKind;
		}

		if (has_rule(Constants_ShanXi.GAME_RULE_ZHI_ZHA_BU_HU)) {
			if (card_type == Constants_ShanXi.HU_CARD_TYPE_JIE_PAO || card_type == Constants_ShanXi.HU_CARD_TYPE_QIANG_GANG) {
				return cbChiHuKind;
			}
		}

		int hand_magic_card_count = 0;
		// 转换手牌
		int cbValue = 0;
		if (_logic.switch_to_card_index(cur_card) < GameConstants.MAX_ZI) {
			cbValue = _logic.get_card_value(cur_card);
		}
		int[] tmp_hand_cards_index = new int[GameConstants.MAX_INDEX];

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			tmp_hand_cards_index[i] = cards_index[i];
			if (_logic.is_magic_index(i)) {
				hand_magic_card_count += cards_index[i];
			}
		}

		if (_logic.is_valid_card(cur_card)) {
			tmp_hand_cards_index[_logic.switch_to_card_index(cur_card)]++;
		}

		int[] tmp_hand_cards_index1 = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			tmp_hand_cards_index1[i] = tmp_hand_cards_index[i];
		}
		// 点炮红中
		boolean dian_pao_hong_zhong = false;
		// 两个红中赖子当本身
		if (card_type == Constants_ShanXi.HU_CARD_TYPE_JIE_PAO || card_type == Constants_ShanXi.HU_CARD_TYPE_QIANG_GANG) {
			if (_logic.is_magic_card(cur_card)) {
				if (hand_magic_card_count > 0) {
					tmp_hand_cards_index1[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD)]--;
					tmp_hand_cards_index1[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD)]--;
					dian_pao_hong_zhong = true;
				} else {
					return GameConstants.WIK_NULL;
				}
			}
		}

		int card_color_count = _logic.get_se_count(tmp_hand_cards_index, weaveItems, weaveCount);

		boolean have_258 = check_have_258(tmp_hand_cards_index);
		boolean have_yi_dui_258 = check_have_yi_dui_258(tmp_hand_cards_index);
		boolean can_win = false;
		if (dian_pao_hong_zhong) {
			if (has_rule(Constants_ShanXi.GAME_RULE_258_YING_JIANG)) {
				can_win = have_258 && AnalyseCardUtil.analyse_258_by_cards_index(tmp_hand_cards_index1, _logic.switch_to_card_index(cur_card),
						_logic.get_all_magic_card_index(), _logic.get_magic_card_count());
			} else {
				can_win = AnalyseCardUtil.analyse_win_by_cards_index(tmp_hand_cards_index1, _logic.switch_to_card_index(cur_card),
						_logic.get_all_magic_card_index(), _logic.get_magic_card_count());
			}
		} else {
			if (has_rule(Constants_ShanXi.GAME_RULE_258_YING_JIANG)) {
				can_win = have_258 && AnalyseCardUtil.analyse_258_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
						_logic.get_all_magic_card_index(), _logic.get_magic_card_count());
			} else {
				can_win = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
						_logic.get_all_magic_card_index(), _logic.get_magic_card_count());
			}
		}
		// 是否勾了‘可胡七对’
		boolean is_qi_xiao_dui = false;
		if (has_rule(Constants_ShanXi.GAME_RULE_QI_DUI_KE_HU_JIA_FAN) || has_rule(Constants_ShanXi.GAME_RULE_QI_DUI_KE_HU_BU_JIA_FAN)) {
			if (has_rule(Constants_ShanXi.GAME_RULE_258_YING_JIANG)) {
				is_qi_xiao_dui = have_yi_dui_258 && is_qi_xiao_dui(cards_index, weaveItems, weaveCount, cur_card) != GameConstants.WIK_NULL;
			} else {
				is_qi_xiao_dui = is_qi_xiao_dui(cards_index, weaveItems, weaveCount, cur_card) != GameConstants.WIK_NULL;
			}
		}
		// 牌型计算
		if (is_qi_xiao_dui) {
			cbChiHuKind = GameConstants.WIK_CHI_HU;
			chiHuRight.opr_or(Constants_ShanXi.CHR_QI_DUI);
		} else if (can_win) {
			chiHuRight.opr_or(Constants_ShanXi.CHR_PI_HU);
		} else {
			chiHuRight.set_empty();
			return cbChiHuKind;
		}

		boolean jiang_258 = false;
		boolean hu_258 = false;
		if (has_rule(Constants_ShanXi.GAME_RULE_258_YING_JIANG)) {
			if (is_qi_xiao_dui) {
				jiang_258 = true;
				if (cbValue == 2 || cbValue == 5 || cbValue == 8) {
					hu_258 = true;

				} else if (_logic.is_magic_card(cur_card)) {
					int[] tmp_hand_cards_index2 = Arrays.copyOf(cards_index, cards_index.length);
					for (int i = 0; i < GameConstants.MAX_ZI; i++) {
						int card_value = _logic.get_card_value(_logic.switch_to_card_data(i));
						// 单牌统计
						if ((card_value != 2) && (card_value != 5) && (card_value != 8)) {
							continue;
						}
						tmp_hand_cards_index2[i]++;
						if (check_have_yi_dui_258(tmp_hand_cards_index2)) {
							hu_258 = true;
						}
						tmp_hand_cards_index2[i]--;
					}
				}
			} else {
				// 将258加番
				if (has_rule(Constants_ShanXi.GAME_RULE_JIANG_258_JIA_FAN)) {
					jiang_258 = true;
				}
				// 胡258硬将
				if (has_rule(Constants_ShanXi.GAME_RULE_HU_258_JIA_FAN)) {
					if (cbValue == 2 || cbValue == 5 || cbValue == 8) {
						hu_258 = true;
					} else if (_logic.is_magic_card(cur_card) && card_type == Constants_ShanXi.HU_CARD_TYPE_ZI_MO) {
						for (int i = 0; i < GameConstants.MAX_ZI; i++) {
							int card_value = _logic.get_card_value(_logic.switch_to_card_data(i));
							// 单牌统计
							if ((card_value != 2) && (card_value != 5) && (card_value != 8)) {
								continue;
							}
							boolean can_hu = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, i, _logic.get_all_magic_card_index(),
									_logic.get_magic_card_count());
							boolean jiang_hu = AnalyseCardUtil.analyse_258_by_cards_index(cards_index, i, _logic.get_all_magic_card_index(),
									_logic.get_magic_card_count());
							if (jiang_hu && can_hu) {
								hu_258 = true;
							}
						}
					}
				}
			}
		} else {
			if (is_qi_xiao_dui) {
				if (have_yi_dui_258) {
					jiang_258 = true;
				}
				if (cbValue == 2 || cbValue == 5 || cbValue == 8) {
					hu_258 = true;
				} else if (_logic.is_magic_card(cur_card) && card_type == Constants_ShanXi.HU_CARD_TYPE_ZI_MO) {
					int[] tmp_hand_cards_index2 = Arrays.copyOf(cards_index, cards_index.length);
					for (int i = 0; i < GameConstants.MAX_ZI; i++) {
						int card_value = _logic.get_card_value(_logic.switch_to_card_data(i));
						// 单牌统计
						if ((card_value != 2) && (card_value != 5) && (card_value != 8)) {
							continue;
						}
						tmp_hand_cards_index2[i]++;
						if (check_have_yi_dui_258(tmp_hand_cards_index2)) {
							hu_258 = true;
						}
					}
				}
			} else {
				// 胡258加番
				if (has_rule(Constants_ShanXi.GAME_RULE_HU_258_JIA_FAN)) {
					if (cbValue == 2 || cbValue == 5 || cbValue == 8) {
						hu_258 = true;
						// 如果牌里面自带258将 则就是258将
						for (int i = 0; i < GameConstants.MAX_ZI; i++) {
							int card_value = _logic.get_card_value(_logic.switch_to_card_data(i));
							//
							if ((card_value != 2) && (card_value != 5) && (card_value != 8)) {
								continue;
							}
							boolean can_hu = AnalyseCardUtil.analyse_258_by_cards_index(cards_index, i, _logic.get_all_magic_card_index(),
									_logic.get_magic_card_count());
							if (can_hu && _logic.switch_to_card_data(i) == cur_card) {
								jiang_258 = true;
							}
						}
					} else if (_logic.is_magic_card(cur_card) && card_type == Constants_ShanXi.HU_CARD_TYPE_ZI_MO) {
						if (is_qi_xiao_dui) {
							if (have_258) {
								hu_258 = true;
								jiang_258 = true;
							}
						} else {
							for (int i = 0; i < GameConstants.MAX_ZI; i++) {
								int card_value = _logic.get_card_value(_logic.switch_to_card_data(i));
								// 单牌统计
								if ((card_value != 2) && (card_value != 5) && (card_value != 8)) {
									continue;
								}
								boolean can_hu = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, i, _logic.get_all_magic_card_index(),
										_logic.get_magic_card_count());
								boolean jiang_hu = AnalyseCardUtil.analyse_258_by_cards_index(cards_index, i, _logic.get_all_magic_card_index(),
										_logic.get_magic_card_count());
								if (can_hu) {
									hu_258 = true;
								}
								if (can_hu && jiang_hu) {
									jiang_258 = true;
								}
							}
						}
					} else {
						if (is_qi_xiao_dui) {
							if (have_258) {
								jiang_258 = true;
							}
						} else {
							// 别人打的红中
							if (dian_pao_hong_zhong) {
								if (AnalyseCardUtil.analyse_258_by_cards_index(tmp_hand_cards_index1, _logic.switch_to_card_index(cur_card),
										_logic.get_all_magic_card_index(), _logic.get_magic_card_count())) {
									if (have_258) {
										jiang_258 = true;
									}
								}
							} else {
								if (AnalyseCardUtil.analyse_258_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
										_logic.get_all_magic_card_index(), _logic.get_magic_card_count())) {
									if (have_258) {
										jiang_258 = true;
									}
								}
							}
						}
					}
				}
			}
		}

		if (hu_258 && has_rule(Constants_ShanXi.GAME_RULE_HU_258_JIA_FAN)) {
			chiHuRight.opr_or(Constants_ShanXi.CHR_HU_258_JIA_FAN);
		}
		if (jiang_258 && has_rule(Constants_ShanXi.GAME_RULE_JIANG_258_JIA_FAN)) {
			chiHuRight.opr_or(Constants_ShanXi.CHR_JIANG_258_JIA_FAN);
		}

		// 清一色判断
		if (card_color_count == 1 && has_rule(Constants_ShanXi.GAME_RULE_QI_YI_SE_JIA_FAN)) {
			chiHuRight.opr_or(Constants_ShanXi.CHR_QING_YI_SE);
		}
		cbChiHuKind = GameConstants.WIK_CHI_HU;

		if (card_type == Constants_ShanXi.HU_CARD_TYPE_ZI_MO) {
			chiHuRight.opr_or(Constants_ShanXi.CHR_ZI_MO);
		} else if (card_type == Constants_ShanXi.HU_CARD_TYPE_JIE_PAO) {
			chiHuRight.opr_or(Constants_ShanXi.CHR_JIE_PAO);
		} else if (card_type == Constants_ShanXi.HU_CARD_TYPE_QIANG_GANG) {
			chiHuRight.opr_or(Constants_ShanXi.CHR_QIANG_GANG);
		}

		return cbChiHuKind;
	}

	/**
	 * 是否有258
	 * 
	 * @param cards_index
	 * @return
	 */
	private boolean check_have_258(int[] cards_index) {
		// 计算单牌
		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			if (cards_index[i] == 0) {
				continue;
			}
			int cbValue = _logic.get_card_value(_logic.switch_to_card_data(i));
			// 单牌统计
			if ((cbValue == 2) || (cbValue == 5) || (cbValue == 8)) {
				return true;
			}

		}
		return false;
	}

	/**
	 * 是否有一对258
	 * 
	 * @param cards_index
	 * @return
	 */
	private boolean check_have_yi_dui_258(int[] cards_index) {
		// 计算单牌
		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			if (cards_index[i] == 0) {
				continue;
			}
			int cbValue = _logic.get_card_value(_logic.switch_to_card_data(i));
			// 单牌统计
			if ((cbValue != 2) && (cbValue != 5) && (cbValue == 8)) {
				continue;
			}
			if (cards_index[i] == 1) {
				return true;
			}
			if (cards_index[i] == 2) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 第一轮 初始化庄家 默认第一个。需要的继承
	 */
	@Override
	protected void initBanker() {
		// this.shuffle_players();
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// this.get_players()[i].set_seat_index(i);
			if (this.get_players()[i].getAccount_id() == this.getRoom_owner_account_id()) {
				this._cur_banker = i;
			}
		}
	}

	@Override
	protected boolean on_handler_game_finish(int seat_index, int reason) {
		int real_reason = reason;

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			_player_ready[i] = 0;
			_player_result.pao[i] = -1;//
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
		game_end.setRoomOverType(0);
		game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间
		game_end.setRunPlayerId(_run_player_id);
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			game_end.addPao(_player_result.pao[i]);
		}

		setGameEndBasicPrama(game_end);

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

			// 杠牌，每个人的分数
			float lGangScore[] = new float[GameConstants.GAME_PLAYER];
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {

				for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
					for (int k = 0; k < GameConstants.GAME_PLAYER; k++) {
						lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
						// _allGRR._gang_score[i].scores[j][k]+=
						// GRR._gang_score[i].scores[j][k];//杠牌，每个人的分数

						// allGangScore[k]+=_allGRR._gang_score[i].scores[j][k];
					}
				}

				// 记录
				for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
					_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
				}

			}

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				// 流局不算杠分
				if (reason == GameConstants.Game_End_NORMAL) {
					GRR._game_score[i] += lGangScore[i] * di_fen;
				}
				GRR._game_score[i] += GRR._start_hu_score[i];// 起手胡分数

				// 记录
				// _all_start_hu_score[i]+=_start_hu_score[i];//起手胡分数
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
			this.set_result_describe();

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {
					// for (int j = 0; j < GRR._card_count[i]; j++) {
					if (_logic.is_magic_card(GRR._cards_data[i][j])) {
						GRR._cards_data[i][j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
					}
					// }

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
				for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
					lfs.addItem(GRR._lost_fan_shu[i][j]);
				}

				game_end.addLostFanShu(lfs);

			}

		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if ((!is_sys()) && _cur_round >= _game_round) {// 局数到了
				end = true;
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(this.process_player_result(reason));
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
		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT || reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
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

		if (is_sys()) {
			clear_score_in_gold_room();
		}
		// 错误断言
		return false;
	}

	public boolean operate_player_cards_with_ting(int seat_index, int card_count, int cards[], int weave_count, WeaveItem weaveitems[]) {
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
			roomResponse.addCardData(cards[j]);
		}

		int ting_count = _playerStatus[seat_index]._hu_out_card_count;

		// 打出去可以听牌的个数
		roomResponse.setOutCardCount(ting_count);

		for (int i = 0; i < ting_count; i++) {
			int ting_card_cout = _playerStatus[seat_index]._hu_out_card_ting_count[i];
			roomResponse.addOutCardTingCount(ting_card_cout);
			roomResponse.addOutCardTing(_playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants.CARD_ESPECIAL_TYPE_TING);

			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < ting_card_cout; j++) {
				int tmp_card = _playerStatus[seat_index]._hu_out_cards[i][j];
				if (is_mj_type(GameConstants.GAME_TYPE_HENAN_ZHOU_KOU) || is_mj_type(GameConstants.GAME_TYPE_HENAN)
						|| is_mj_type(GameConstants.GAME_TYPE_NEW_HE_NAN) || is_mj_type(GameConstants.GAME_TYPE_JIAO_ZUO_HUAN_HUAN)
						|| is_mj_type(GameConstants.GAME_TYPE_NEW_ZHOU_KOU)) {
					if (has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN)) {
						if (_logic.is_magic_card(tmp_card)) {
							tmp_card += GameConstants.CARD_ESPECIAL_TYPE_HUN;
						}
					}
				} else if (is_mj_type(GameConstants.GAME_TYPE_HUNAN_SHANG_XIA_GUI)) {
					if (_logic.is_magic_card(tmp_card)) {
						tmp_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
					}
				}
				int_array.addItem(tmp_card);
			}
			roomResponse.addOutCardTingCards(int_array);
		}

		// 自己才有牌数据
		this.send_response_to_player(seat_index, roomResponse);

		GRR.add_room_response(roomResponse);

		return true;
	}

	/**
	 * 河南红中麻将
	 * 
	 * @param seat_index
	 * @param operate_card
	 * @param rm
	 */
	public void process_chi_hu_player_operate_hnhz(int seat_index, int operate_card, boolean rm) {
		// 引用权位
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
		this.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, effect_count, effect_indexs, 1, GameConstants.INVALID_SEAT);
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

	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		set_pao_zi();
		GRR._chi_hu_card[seat_index][0] = operate_card;
		GRR._win_order[seat_index] = 1;

		int pai_xing_fen = 1;
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		pai_xing_fen *= get_pai_xing_fen_and_bei_shu(seat_index);

		countCardType(chr, seat_index);

		if (di_fen <= 0) {
			di_fen = 1;
		}
		if (has_rule(Constants_ShanXi.GAME_RULE_2_DI_FEN)) {
			di_fen *= 2;
		}
		if (has_rule(Constants_ShanXi.GAME_RULE_5_DI_FEN)) {
			di_fen *= 5;
		}

		// 统计
		if (zimo) {
			// 自摸
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				GRR._lost_fan_shu[i][seat_index] = pai_xing_fen;
			}
		} else {
			GRR._lost_fan_shu[provide_index][seat_index] = pai_xing_fen;//
		}

		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				// 输分是庄家翻倍
				int tmp = pai_xing_fen;
				if (i == _shang_zhuang_player) {
					tmp *= 2;
				}
				int score = 0;
				if (_player_result.pao[seat_index] > 0 && _player_result.pao[i] > 0) {
					score = (tmp + _player_result.pao[seat_index] + _player_result.pao[i]) * di_fen * 2;
				} else {
					score = tmp * di_fen * 2;
				}
				GRR._game_score[i] -= score;
				GRR._game_score[seat_index] += score;
			}
		} else {
			// 放炮是庄家
			if (provide_index == _shang_zhuang_player) {
				pai_xing_fen *= 2;
			}
			int score = 0;
			if (_player_result.pao[provide_index] > 0 && _player_result.pao[seat_index] > 0) {
				score = (pai_xing_fen + _player_result.pao[provide_index] + _player_result.pao[seat_index]) * di_fen;
			} else {
				score = pai_xing_fen * di_fen;
			}
			GRR._game_score[provide_index] -= score;

			GRR._game_score[seat_index] += score;
			GRR._chi_hu_rights[provide_index].opr_or(Constants_ShanXi.CHR_FANG_PAO);
		}

		// 设置变量
		GRR._provider[seat_index] = provide_index;
		_status_gang = false;
		_status_gang_hou_pao = false;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);

	}

	/**
	 * 牌型分+翻倍计算(可叠加)
	 * 
	 * @param seaxIndex
	 * @return
	 */
	private int get_pai_xing_fen_and_bei_shu(int seaxIndex) {
		int bei_shu = 1;
		ChiHuRight chr = GRR._chi_hu_rights[seaxIndex];
		if (seaxIndex == _shang_zhuang_player) {
			bei_shu *= 2;
		}
		// if (!chr.opr_and(Constants_ShanXi.CHR_ZI_MO).is_empty()) {
		// bei_shu *= 2;
		// }
		if (has_rule(Constants_ShanXi.GAME_RULE_QI_DUI_KE_HU_JIA_FAN) && !chr.opr_and(Constants_ShanXi.CHR_QI_DUI).is_empty()) {
			bei_shu *= 2;
		}
		if (has_rule(Constants_ShanXi.GAME_RULE_QI_YI_SE_JIA_FAN) && !chr.opr_and(Constants_ShanXi.CHR_QING_YI_SE).is_empty()) {
			bei_shu *= 2;
		}
		if (!chr.opr_and(Constants_ShanXi.CHR_HU_258_JIA_FAN).is_empty()) {
			bei_shu *= 2;
		}
		if (!chr.opr_and(Constants_ShanXi.CHR_JIANG_258_JIA_FAN).is_empty()) {
			bei_shu *= 2;
		}
		return bei_shu;
	}

	@Override
	protected void set_result_describe() {
		int chrTypes;
		long type = 0;
		for (int player = 0; player < getTablePlayerNumber(); player++) {
			StringBuilder result = new StringBuilder("");

			chrTypes = GRR._chi_hu_rights[player].type_count;

			for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
				type = GRR._chi_hu_rights[player].type_list[typeIndex];

				if (GRR._chi_hu_rights[player].is_valid()) {
					if (type == Constants_ShanXi.CHR_ZI_MO)
						result.append(" 自摸");

					if (type == Constants_ShanXi.CHR_JIE_PAO)
						result.append(" 接炮");

					if (type == Constants_ShanXi.CHR_PI_HU)
						result.append(" 平胡");

					if (type == Constants_ShanXi.CHR_QI_DUI)
						result.append(" 七对");

					if (type == Constants_ShanXi.CHR_QING_YI_SE)
						result.append(" 清一色");

					if (type == Constants_ShanXi.CHR_GANG_SHANG_KAI_HUA)
						result.append(" 杠开花");

					if (type == Constants_ShanXi.CHR_QIANG_GANG)
						result.append(" 抢杠胡");

					if (type == Constants_ShanXi.CHR_HU_258_JIA_FAN)
						result.append(" 胡258加番");

					if (type == Constants_ShanXi.CHR_JIANG_258_JIA_FAN)
						result.append(" 将258加番");
				} else if (type == Constants_ShanXi.CHR_FANG_PAO) {
					result.append(" 点炮");
				}
			}

			// if (GRR._end_type != GameConstants.Game_End_DRAW) { // 荒庄荒杠
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
			// }

			GRR._result_des[player] = result.toString();
		}
	}

	/**
	 * 
	 * @param seat_index
	 * @param card
	 *            固定的鸟
	 * @param show
	 * @param add_niao
	 *            额外奖鸟
	 */
	public void set_niao_card(int seat_index, int card, boolean show, int add_niao) {
		for (int i = 0; i < GameConstants.MAX_NIAO_CARD; i++) {
			GRR._cards_data_niao[i] = GameConstants.INVALID_VALUE;
		}

		// for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			GRR._player_niao_count[i] = 0;
			for (int j = 0; j < GameConstants.MAX_NIAO_CARD; j++) {
				GRR._player_niao_cards[i][j] = GameConstants.INVALID_VALUE;
			}

		}
		GRR._show_bird_effect = show;
		if (card == GameConstants.INVALID_VALUE) {
			GRR._count_niao = get_niao_card_num(true, add_niao);
		} else {
			GRR._count_niao = get_niao_card_num(false, add_niao);
		}

		if (GRR._count_niao > GameConstants.ZHANIAO_0) {
			if (card == GameConstants.INVALID_VALUE) {
				int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
				// if (is_mj_type(MJGameConstants.GAME_TYPE_HZ)) {
				// _logic.switch_to_cards_index(_repertory_card_zz,
				// _all_card_len-GRR._left_card_count, GRR._count_niao,
				// cbCardIndexTemp);
				// } else {
				// _logic.switch_to_cards_index(_repertory_card_cs,
				// _all_card_len-GRR._left_card_count, GRR._count_niao,
				// cbCardIndexTemp);
				// }
				_logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count, GRR._count_niao, cbCardIndexTemp);
				// cbCardIndexTemp[0] = 3;
				// cbCardIndexTemp[1] = 0x25;
				GRR._left_card_count -= GRR._count_niao;
				_logic.switch_to_cards_data(cbCardIndexTemp, GRR._cards_data_niao);

				// for (int i = 0; i < GRR._count_niao; i++) {
				// GRR._cards_data_niao[i] = 0x13;
				// }
			} else {
				for (int i = 0; i < GRR._count_niao; i++) {
					GRR._cards_data_niao[i] = card;
				}
			}
		}

		if (is_mj_type(GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ)) {
			if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HUNAN_YI_MA_QUAN_ZHONG)) {
				GRR._count_pick_niao = _logic.get_pick_niao_count_new_hz(GRR._cards_data_niao, GRR._count_niao);
			} else {
				// 中鸟个数
				GRR._count_pick_niao = _logic.get_pick_niao_count(GRR._cards_data_niao, GRR._count_niao);
			}
		} else {
			// 中鸟个数
			GRR._count_pick_niao = _logic.get_pick_niao_count(GRR._cards_data_niao, GRR._count_niao);
		}

		if (is_mj_type(GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ)
				&& GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HUNAN_YI_MA_QUAN_ZHONG)) {
			GRR._player_niao_cards[seat_index][GRR._player_niao_count[seat_index]++] = GRR._cards_data_niao[0];
		} else {
			for (int i = 0; i < GRR._count_niao; i++) {
				int nValue = _logic.get_card_value(GRR._cards_data_niao[i]);
				int seat = 0;
				seat = get_zhong_seat_by_value_three(nValue, seat_index);
				GRR._player_niao_cards[seat][GRR._player_niao_count[seat]] = GRR._cards_data_niao[i];
				GRR._player_niao_count[seat]++;
			}
		}
	}

	public void set_niao_card_hz(int seat_index, int card, boolean show, int add_niao, int hu_card) {
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

		GRR._count_niao = get_niao_card_num(true, add_niao);

		if (has_rule_ex(GameConstants.GAME_RULE_HUNAN_MO_JI_JIANG_JI)) {
			if (hu_card == GameConstants.HZ_MAGIC_CARD) {
				GRR._count_niao = GRR._left_card_count > 10 ? 10 : GRR._left_card_count;
			} else {
				GRR._count_niao = _logic.get_card_value(hu_card);
			}
		}
		if (has_rule_ex(GameConstants.GAME_RULE_HUNAN_YI_MA_QUAN_ZHONG)) {
			GRR._count_niao = 1;
		}
		if (GRR._count_niao > GRR._left_card_count) {
			GRR._count_niao = GRR._left_card_count;
		}
		if (GRR._count_niao == GameConstants.ZHANIAO_0)
			return;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];

		_logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count, GRR._count_niao, cbCardIndexTemp);

		GRR._left_card_count -= GRR._count_niao;

		_logic.switch_to_cards_data(cbCardIndexTemp, GRR._cards_data_niao);

		if (has_rule_ex(GameConstants.GAME_RULE_HUNAN_YI_MA_QUAN_ZHONG)) {
			GRR._count_pick_niao = _logic.get_pick_niao_count_new_hz(GRR._cards_data_niao, GRR._count_niao);
		} else {
			GRR._count_pick_niao = get_pick_niao_count(GRR._cards_data_niao, GRR._count_niao);
		}

		if (has_rule_ex(GameConstants.GAME_RULE_HUNAN_YI_MA_QUAN_ZHONG)) {
			GRR._player_niao_cards[seat_index][GRR._player_niao_count[seat_index]++] = GRR._cards_data_niao[0];
		} else {
			for (int i = 0; i < GRR._count_niao; i++) {
				int nValue = _logic.get_card_value(GRR._cards_data_niao[i]);
				int seat = get_zhong_seat_by_value_three(nValue, seat_index);
				GRR._player_niao_cards[seat][GRR._player_niao_count[seat]] = GRR._cards_data_niao[i];
				GRR._player_niao_count[seat]++;
			}
		}

		int[] player_niao_count = new int[GameConstants.GAME_PLAYER];
		int[][] player_niao_cards = new int[GameConstants.GAME_PLAYER][GameConstants.MAX_NIAO_CARD];
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			player_niao_count[i] = 0;
			for (int j = 0; j < GameConstants.MAX_NIAO_CARD; j++) {
				player_niao_cards[i][j] = GameConstants.INVALID_VALUE;
			}
		}
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < GRR._player_niao_count[i]; j++) {
				if (seat_index == i) {
					player_niao_cards[i][player_niao_count[i]] = this.set_ding_niao_valid(GRR._player_niao_cards[i][j], true);//
					// 胡牌的鸟生效
				} else {
					player_niao_cards[i][player_niao_count[i]] = this.set_ding_niao_valid(GRR._player_niao_cards[i][j], false);//
					// 胡牌的鸟生效
				}
				player_niao_count[i]++;
			}
		}
		GRR._player_niao_cards = player_niao_cards;
		GRR._player_niao_count = player_niao_count;
	}

	/**
	 * 获取鸟的 数量
	 * 
	 * @param check
	 * @param add_niao
	 * @return
	 */
	public int get_niao_card_num(boolean check, int add_niao) {
		return 0;
	}

	/***
	 * 河南红中麻将检查杠牌,有没有胡的 检查所有人
	 * 
	 * @param seat_index
	 * @param card
	 * @return
	 */
	public boolean estimate_gang_respond_hnhz(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		// 动作判断
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			// 用户过滤
			if (seat_index == i) {
				continue;
			}

			// 漏胡检测
			boolean abandoned_hu = false;
			for (int abandoned_card : _playerStatus[i].get_cards_abandoned_hu()) {
				if (abandoned_card == card) {
					abandoned_hu = true;
					break;
				}
			}
			if (abandoned_hu) {
				continue;
			}

			playerStatus = _playerStatus[i];
			// 可以胡的情况 判断
			if (playerStatus.is_chi_hu_round()) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						Constants_ShanXi.HU_CARD_TYPE_QIANG_GANG, 0);

				// 结果判断
				if (action != 0) {
					chr.opr_or(GameConstants.CHR_HENAN_QIANG_GANG_HU);// 抢杠胡
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

	/**
	 * 抓鸟 1 5 9
	 * 
	 * @param cards_data
	 * @param card_num
	 * @return
	 */
	public int get_pick_niao_count(int cards_data[], int card_num) {
		// MAX_NIAO_CARD
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
	 * 河南红中麻将出牌检测
	 * 
	 * @param seat_index
	 * @param card
	 * @return
	 */
	public boolean estimate_player_out_card_respond_hnhz(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		// 用户状态
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {

			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		int llcard = get_niao_card_num(true, 0);
		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		// 动作判断
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			//// 碰牌判断
			action = _logic.check_peng(GRR._cards_index[i], card);
			if (action != 0) {
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
				// 漏胡检测
				boolean abandoned_hu = false;
				for (int abandoned_card : _playerStatus[i].get_cards_abandoned_hu()) {
					if (abandoned_card == card) {
						abandoned_hu = true;
						break;
					}
				}
				if (abandoned_hu) {
					continue;
				}
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = this.analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						Constants_ShanXi.HU_CARD_TYPE_JIE_PAO, 0);

				// 结果判断
				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
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

	@Override
	protected void init_shuffle() {
		_logic.clean_magic_cards();
		List<Integer> cards_list = new ArrayList<>();
		int[] all_cards = GameConstants.CARD_DATA_BU_DAI_FENG_LZ;
		if (has_rule(Constants_ShanXi.GAME_RULE_DAI_FENG_PAI)) {
			all_cards = GameConstants.CARD_DATA_DAI_FENG_LZ;
		}
		if (has_rule(Constants_ShanXi.GAME_RULE_HONG_ZHONG_LAI_ZI)) {
			_logic.add_magic_card_index(_logic.switch_to_card_index(Constants_ShanXi.HONG_ZHONG_CARD));
			if (!has_rule(Constants_ShanXi.GAME_RULE_DAI_FENG_PAI)) {
				cards_list.add(Constants_ShanXi.HONG_ZHONG_CARD);
				cards_list.add(Constants_ShanXi.HONG_ZHONG_CARD);
				cards_list.add(Constants_ShanXi.HONG_ZHONG_CARD);
				cards_list.add(Constants_ShanXi.HONG_ZHONG_CARD);
			}
		}
		for (int i : all_cards) {
			cards_list.add(i);
		}
		_repertory_card = new int[cards_list.size()];
		int[] card = new int[cards_list.size()];
		for (int i = 0; i < cards_list.size(); i++) {
			card[i] = cards_list.get(i);
		}
		shuffle(_repertory_card, card);
	}

	/// 洗牌
	@Override
	public void shuffle(int repertory_card[], int mj_cards[]) {
		_all_card_len = repertory_card.length;
		GRR._left_card_count = _all_card_len;

		int xi_pai_count = 0;
		int rand = (int) RandomUtil.generateRandomNumber(3, 6);

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
		// 分发扑克
		for (int i = 0; i < count; i++) {
			send_count = (GameConstants.MAX_COUNT - 1);
			GRR._left_card_count -= send_count;

			// 一人13张牌,庄家多一张
			_logic.switch_to_cards_index(repertory_card, have_send_count, send_count, GRR._cards_index[i]);

			have_send_count += send_count;

		}
		// 记录初始牌型
		if (_recordRoomRecord != null) {
			_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));
		}
	}

	/**
	 * 是否可以下炮子
	 * 
	 * @return
	 */
	private boolean xia_pao_zi() {
		// if (has_rule(Constants_ShanXi.GAME_RULE_1_PAO) ||
		// has_rule(Constants_ShanXi.GAME_RULE_2_PAO) ||
		// has_rule(Constants_ShanXi.GAME_RULE_3_PAO) ||
		// has_rule(Constants_ShanXi.GAME_RULE_4_PAO) ) {
		// if(xia_pao_zi_count == 0){
		// return true;
		// }
		// }
		if (has_rule(Constants_ShanXi.GAME_RULE_ZIYOU_PAO)) {
			return true;
		}
		return false;
	}

	@Override
	public boolean handler_requst_pao_qiang(Player player, int pao, int qiang) {
		// TODO Auto-generated method stub
		return _handler_pao_henna_hz.handler_pao_qiang(this, player.get_seat_index(), pao, qiang);
	}

	/**
	 * 是不是七小对
	 * 
	 * @param cards_index
	 * @param weaveItem
	 * @param cbWeaveCount
	 * @param cur_card
	 * @return
	 */
	private int is_qi_xiao_dui(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int cur_card) {

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
		int magic_card_count = 0;
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];
			if (cbCardCount <= 0) {
				continue;
			}

			if (_logic.is_magic_index(i)) {
				magic_card_count += cbCardCount;
				continue;
			}

			// 单牌统计
			if (cbCardCount == 1 || cbCardCount == 3)
				cbReplaceCount++;

			if (cbCardCount == 4) {
				nGenCount++;
			}
		}

		// 王牌不够
		if (cbReplaceCount > magic_card_count) {
			return GameConstants.WIK_NULL;
		}

		if (nGenCount > 0) {
			if (nGenCount >= 2) {
				// 双豪华七小对
				return GameConstants.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI;
			}
			return GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI;
		} else {
			return GameConstants.CHR_HUNAN_QI_XIAO_DUI;
		}
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * 炮子数不知道哪里重置 这里给他强制设置
	 */
	public void set_pao_zi() {
		if (has_rule(Constants_ShanXi.GAME_RULE_1_PAO)) {
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				_player_result.pao[i] = 1;//
			}
		} else if (has_rule(Constants_ShanXi.GAME_RULE_2_PAO)) {
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				_player_result.pao[i] = 2;//
			}
		} else if (has_rule(Constants_ShanXi.GAME_RULE_3_PAO)) {
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				_player_result.pao[i] = 3;//
			}
		} else if (has_rule(Constants_ShanXi.GAME_RULE_4_PAO)) {
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				_player_result.pao[i] = 4;//
			}
		} else if (has_rule(Constants_ShanXi.GAME_RULE_ZIYOU_PAO)) {
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				_player_result.pao[i] = pao_zi[i];//
			}
		} else {
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				_player_result.pao[i] = -1;//
			}
		}
	}

	/**
	 * 加载房间里的玩家信息
	 * 
	 * @param roomResponse
	 */
	@Override
	public void load_player_info_data(RoomResponse.Builder roomResponse) {
		set_pao_zi();
		Player rplayer;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
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
			room_player.setQiang(_player_result.qiang[i]);
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

}
