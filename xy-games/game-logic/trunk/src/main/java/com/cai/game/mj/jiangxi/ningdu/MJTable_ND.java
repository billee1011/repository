package com.cai.game.mj.jiangxi.ningdu;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.GameConstants_ND;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.GameDescUtil;
import com.cai.common.util.PBUtil;
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
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;
import protobuf.clazz.gzcg.GZCGRsp.EveryJingInfo_GZCG;
import protobuf.clazz.gzcg.GZCGRsp.GameEndResponse_GZCG;

public class MJTable_ND extends AbstractMJTable {

	private int[] dispatchcardNum; // 摸牌次数
	private boolean isCanGenZhuang; // 可以跟庄
	private int genZhuangCount; // 跟庄次数
	private int genZhuangCard; // 跟庄牌
	private int next_seat_index;
	private int shang_ju_zhuang;
	private int maPaiScore[] = new int[getTablePlayerNumber()]; // 马牌分数

	// protected MJHandlerYaoHaiDi_ND _handler_yao_hai_di_nd;

	/**
	 * 跟庄操作
	 * 
	 * @param seat_index
	 * @param card
	 * @param isZhuang
	 */
	public void addGenZhuangCard(int seat_index, int card, boolean isZhuang) {
		if (isZhuang) {
			genZhuangCard = card;
			next_seat_index = (seat_index + getTablePlayerNumber() + 1) % getTablePlayerNumber();
			if (!isDispatchcardNum(seat_index)) {
				setGenZhuangCount();
			}
		} else {
			if (genZhuangCard != card || seat_index != next_seat_index) {
				isCanGenZhuang = false;
			} else {
				next_seat_index = (seat_index + getTablePlayerNumber() + 1) % getTablePlayerNumber();
			}
		}
	}

	/**
	 * 跟庄状态
	 * 
	 * @return the isCanGenZhuang
	 */
	public boolean isCanGenZhuang() {
		return isCanGenZhuang;
	}

	/**
	 * 跟庄次数
	 * 
	 * @return the genZhuangCount
	 */
	public int getGenZhuangCount() {
		return genZhuangCount;
	}

	/**
	 * 跟庄次数累计
	 * 
	 * @param genZhuangCount
	 *            the genZhuangCount to set
	 */
	public void setGenZhuangCount() {
		this.genZhuangCount++;
	}

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

	/**
	* 
	*/
	public MJTable_ND() {
		super(MJType.GAME_TYPE_MJ_NINGDU);
	}

	private static final long serialVersionUID = 1L;

	@Override
	public int getTablePlayerNumber() {
		if (has_rule(GameConstants_ND.GAME_RULE_PLAYER_2)) {
			return GameConstants.GAME_PLAYER - 2;
		} else if (has_rule(GameConstants_ND.GAME_RULE_PLAYER_3)) {
			return GameConstants.GAME_PLAYER - 1;
		} else {
			return GameConstants.GAME_PLAYER;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.game.mj.AbstractMJTable#onInitTable()
	 */
	@Override
	protected void onInitTable() {
		_handler_dispath_card = new MJHandlerDispatchCard_ND();
		_handler_out_card_operate = new MJHandlerOutCardOperate_ND();
		_handler_gang = new MJHandlerGang_ND();
		_handler_chi_peng = new MJHandlerChiPeng_ND();
		// _handler_yao_hai_di_nd = new MJHandlerYaoHaiDi_ND();
	}

	/**
	 * 参数初始化
	 */
	protected void onInitParam() {
		dispatchcardNum = new int[GameConstants.GAME_PLAYER];
		isCanGenZhuang = true;
		genZhuangCount = 0;
		maPaiScore = new int[getTablePlayerNumber()];
		if (_cur_round == 1) {
			shang_ju_zhuang = 0;
		}

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
		// gameStartResponse.setSiceIndex(rand);
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

		// 检测听牌
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i]._hu_card_count = this.get_ting_card(_playerStatus[i]._hu_cards, GRR._cards_index[i],
					GRR._weave_items[i], GRR._weave_count[i], false, i);
			if (_playerStatus[i]._hu_card_count > 0) {
				this.operate_chi_hu_cards(i, _playerStatus[i]._hu_card_count, _playerStatus[i]._hu_cards);
			}
		}

		boolean is_qishou_hu = false;
		if (is_qishou_hu == false) {
			this.exe_dispatch_card(_current_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);
		}

		return false;
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card,
			ChiHuRight chiHuRight, int card_type, int _seat_index) {

		if (cur_card == 0)
			return GameConstants.WIK_NULL;

		int cbChiHuKind = GameConstants.WIK_NULL;

		long qxd = _logic.is_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card);
		if (qxd != GameConstants.WIK_NULL) {
			if (qxd == GameConstants.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI) {
				chiHuRight.opr_or(GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI);
			} else {
				chiHuRight.opr_or(qxd);
			}

			cbChiHuKind = GameConstants.WIK_CHI_HU;
			if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
				chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
			} else {
				chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
			}
		}

		if (chiHuRight.is_empty() == false) {
			if (card_type != GameConstants.HU_CARD_TYPE_QIANGGANG) {
				if (isDispatchcardNum(_seat_index)) {
					if (_seat_index == _cur_banker) {
						chiHuRight.opr_or(GameConstants.CHR_HUNAN_TIAN_HU);
					} else {
						chiHuRight.opr_or(GameConstants.CHR_HUNAN_DI_HU);
					}
				}
			}

			return cbChiHuKind;
		}
		if (card_type != GameConstants.HU_CARD_TYPE_QIANGGANG) {
			if (isDispatchcardNum(_seat_index) ) {
				if (_seat_index == _cur_banker) {
					chiHuRight.opr_or(GameConstants.CHR_HUNAN_TIAN_HU);
				} else {
					chiHuRight.opr_or(GameConstants.CHR_HUNAN_DI_HU);
				}
			}
		}

		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_card_count = _logic.get_magic_card_count();

		if (magic_card_count > 2) { // 一般只有两种癞子牌存在
			magic_card_count = 2;
		}

		for (int i = 0; i < magic_card_count; i++) {
			magic_cards_index[i] = _logic.get_magic_card_index(i);
		}

		boolean can_win = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
				magic_cards_index, magic_card_count);

		if (!can_win) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		boolean dandiao = _logic.is_dan_diao(cards_index, cur_card);
		if (dandiao) {
			chiHuRight.opr_or(GameConstants_ND.CHR_HENAN_DAN_DIAO);
		}

		// 碰碰胡
		boolean is_peng_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index(cards_index,
				_logic.switch_to_card_index(cur_card), magic_cards_index, magic_card_count);

		if (is_peng_hu && !dandiao) {
			boolean exist_eat = exist_eat(weaveItems, weave_count);
			if (!exist_eat) {
				chiHuRight.opr_or(GameConstants.CHR_HUNAN_PENGPENG_HU);
			}
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;
		if (card_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_GANG_KAI);
		} else if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else if (card_type == GameConstants.HU_CARD_TYPE_QIANGGANG) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_QIANG_GANG_HU);// 抢杠胡
		} else {
			chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
		}

		return cbChiHuKind;
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int wFanShu = get_chi_hu_fen(chr);

		countCardType(chr, seat_index);

		int lChiHuScore = wFanShu * GameConstants.CELL_SCORE;

		// 统计
		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				GRR._lost_fan_shu[i][seat_index] = wFanShu;
			}
		} else {
			GRR._lost_fan_shu[provide_index][seat_index] = wFanShu;
		}

		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}

				int s = lChiHuScore;
				int s1 = lChiHuScore;
				// 马分
				int niao = GRR._count_pick_niao;
				s *= (1 + niao);

				int ms = s1 *= niao;
				// 记录马牌s
				maPaiScore[i] -= ms;
				maPaiScore[seat_index] += ms;
				// 胡牌分
				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;
			}
		} else {
			int s = lChiHuScore;
			int s1 = lChiHuScore;
			// 马分
			int niao = GRR._count_pick_niao;
			s *= (1 + niao);
			int ms = s1 *= niao;
			// 记录马牌
			maPaiScore[provide_index] -= ms;
			maPaiScore[seat_index] += ms;

			GRR._game_score[provide_index] -= s;

			GRR._game_score[seat_index] += s;

			GRR._chi_hu_rights[provide_index].opr_or(GameConstants.CHR_FANG_PAO);

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
		int wFanShu = 0;

		boolean isTianHu = false;
		boolean isPenHu = true;
		boolean isJiaDa = false;

		if (has_rule(GameConstants_ND.GAME_RULE_JIA_DA)) {
			isJiaDa = true;
		}

		// 天胡
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_TIAN_HU)).is_empty()) {
			wFanShu = 12;
			isTianHu = true;
			isPenHu = false;
		}

		// 地胡
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_DI_HU)).is_empty()) {
			wFanShu = 12;
			isPenHu = false;
		}

		// 七对
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_QI_XIAO_DUI)).is_empty() && isTianHu) {
			wFanShu = 16;
			isPenHu = false;
		} else if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_QI_XIAO_DUI)).is_empty()) {
			if (isJiaDa) {
				wFanShu = 12;
			} else {
				wFanShu = 10;
			}
			isPenHu = false;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI)).is_empty() && isTianHu) {// 豪华七小对
			wFanShu = 20;
			isPenHu = false;
		} else if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI)).is_empty()) {
			if (isJiaDa) {
				wFanShu = 16;
			} else {
				wFanShu = 12;
			}
			isPenHu = false;
		}

		boolean gangKai = false;
		// 杠开
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_GANG_KAI)).is_empty()) {
			gangKai = true;
		}

		// 碰碰胡
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_PENGPENG_HU)).is_empty() && isTianHu) {
			wFanShu = 20;
			isPenHu = false;
		} else if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_PENGPENG_HU)).is_empty() && gangKai) {
			if (isJiaDa) {
				wFanShu = 12;
			} else {
				wFanShu = 10;
			}
			isPenHu = false;
		} else if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_PENGPENG_HU)).is_empty()) {
			if (isJiaDa) {
				wFanShu = 8;
			} else {
				wFanShu = 6;
			}
			isPenHu = false;
		}

		// 大吊车
		if (!(chiHuRight.opr_and(GameConstants_ND.CHR_HENAN_DAN_DIAO)).is_empty() && gangKai) {
			if (isJiaDa) {
				wFanShu = 24;
			} else {
				wFanShu = 20;
			}
			isPenHu = false;
		} else if (!(chiHuRight.opr_and(GameConstants_ND.CHR_HENAN_DAN_DIAO)).is_empty()) {
			if (isJiaDa) {
				wFanShu = 12;
			} else {
				wFanShu = 10;
			}
			isPenHu = false;
		}

		// 抢杠胡
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_QIANG_GANG_HU)).is_empty() && isPenHu) {
			if (isJiaDa) {
				wFanShu = 16;
			} else {
				wFanShu = 15;
			}
		} else if (gangKai && isPenHu) {
			if (isJiaDa) {
				wFanShu = 8;
			} else {
				wFanShu = 6;
			}
		} else if (!(chiHuRight.opr_and(GameConstants.CHR_ZI_MO)).is_empty() && isPenHu) {
			wFanShu = 2;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_HAI_DI_LAO)).is_empty()) {
			if (isJiaDa) {
				wFanShu = wFanShu * 2 + 2;
			} else {
				wFanShu = wFanShu * 2;
			}
		}

		return wFanShu;
	}

	@Override
	protected void set_result_describe() {
		int l;
		long type = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			String des = "";

			l = GRR._chi_hu_rights[i].type_count;

			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					if (type == GameConstants.CHR_TONG_PAO) {
						des += " 通炮";
					}
					if (type == GameConstants.CHR_ZI_MO) {
						des += " 自摸";
					}
					if (type == GameConstants.CHR_HUNAN_DI_HU) {
						des += " 地胡";
					}
					if (type == GameConstants.CHR_HUNAN_TIAN_HU) {
						des += " 天胡";
					}

					if (type == GameConstants.CHR_SHU_FAN) {
						des += " 接炮";
					}
					if (type == GameConstants.CHR_HUNAN_QIANG_GANG_HU) {
						des += " 抢杠胡";
					}

					if (type == GameConstants_ND.CHR_HENAN_DAN_DIAO) {
						des += " 大吊车";
					}

					if (type == GameConstants.CHR_HUNAN_GANG_KAI) {
						des += " 杠开";
					}

					if (type == GameConstants.CHR_HUNAN_PENGPENG_HU) {
						des += " 碰碰胡";
					}

					if (type == GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI) {
						des += " 豪华七小对";
					}

					if (type == GameConstants.CHR_HUNAN_QI_XIAO_DUI) {
						des += " 七小对";
					}

					if (type == GameConstants.CHR_HUNAN_HAI_DI_LAO) {
						des += " 海底胡";
					}
				} else {
					if (type == GameConstants.CHR_FANG_PAO) {
						des += " 放炮";
					}
				}
			}

			if (_cur_banker == i && genZhuangCount > 0) {
				des += " 跟庄X" + genZhuangCount;
			}

			int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0;
			if (GRR != null) {
				for (int p = 0; p < GameConstants.GAME_PLAYER; p++) {
					for (int w = 0; w < GRR._weave_count[p]; w++) {
						if (GRR._weave_items[p][w].weave_kind != GameConstants.WIK_GANG) {
							continue;
						}
						if (p == i) {// 自己
							// 接杠
							if (GRR._weave_items[p][w].provide_player != p) {
								jie_gang++;
							} else {
								if (GRR._weave_items[p][w].public_card == 1) {// 明杠
									ming_gang++;
								} else {
									an_gang++;
								}
							}
						} else {
							// 放杠
							if (GRR._weave_items[p][w].provide_player == i) {
								fang_gang++;
							}
						}
					}
				}
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

		int l = GameConstants.MAX_ZI;

		for (int i = 0; i < l; i++) {
			if (this._logic.is_magic_index(i))
				continue;
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
				_logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count, GRR._count_niao,
						cbCardIndexTemp);
				GRR._left_card_count -= GRR._count_niao;
				_logic.switch_to_cards_data(cbCardIndexTemp, GRR._cards_data_niao);

			} else {
				for (int i = 0; i < GRR._count_niao; i++) {
					GRR._cards_data_niao[i] = card;
				}
			}
		}

		// 中鸟个数
		GRR._count_pick_niao = _logic.get_pick_niao_count(GRR._cards_data_niao, GRR._count_niao);

		for (int i = 0; i < GRR._count_niao; i++) {
			int nValue = _logic.get_card_value(GRR._cards_data_niao[i]);
			int index = seat_index;

			int seat = get_zhong_seat_by_value_three(nValue, index);
			GRR._player_niao_cards[seat][GRR._player_niao_count[seat]] = GRR._cards_data_niao[i];
			GRR._player_niao_count[seat]++;
		}

		int[] player_niao_count = new int[GameConstants.GAME_PLAYER];
		int[][] player_niao_cards = new int[GameConstants.GAME_PLAYER][GameConstants.MAX_NIAO_CARD];
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			player_niao_count[i] = 0;
			for (int j = 0; j < GameConstants.MAX_NIAO_CARD; j++) {
				player_niao_cards[i][j] = GameConstants.INVALID_VALUE;
			}
		}
		GRR._count_pick_niao = 0;
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < GRR._player_niao_count[i]; j++) {
				if (seat_index == i) {
					GRR._count_pick_niao++;
					player_niao_cards[seat_index][player_niao_count[seat_index]] = this
							.set_ding_niao_valid(GRR._player_niao_cards[i][j], true);// 胡牌的鸟生效
				} else {
					player_niao_cards[seat_index][player_niao_count[seat_index]] = this
							.set_ding_niao_valid(GRR._player_niao_cards[i][j], false);// 胡牌的鸟生效
				}
				player_niao_count[seat_index]++;
			}
		}
		GRR._player_niao_cards = player_niao_cards;
		GRR._player_niao_count = player_niao_count;

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
	 * 获取鸟的 数量
	 * 
	 * @param check
	 * @param add_niao
	 * @return
	 */
	public int get_niao_card_num(boolean check, int add_niao) {
		int nNum = GameConstants.ZHANIAO_0;

		if (has_rule(GameConstants_ND.GAME_RULE_MA_2)) {
			nNum = GameConstants.ZHANIAO_2;
		} else if (has_rule(GameConstants_ND.GAME_RULE_MA_4)) {
			nNum = GameConstants.ZHANIAO_4;
		} else if (has_rule(GameConstants_ND.GAME_RULE_MA_6)) {
			nNum = GameConstants.ZHANIAO_6;
		} else if (has_rule(GameConstants_ND.GAME_RULE_MA_8)) {
			nNum = GameConstants.ZHANIAO_8;
		}

		nNum += add_niao;

		if (check == false) {
			return nNum;
		}
		if (nNum > GRR._left_card_count) {
			nNum = GRR._left_card_count;
		}
		return nNum;
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

		// 动作判断
		for (int i = 0; i < getTablePlayerNumber(); i++) {
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

			if (GRR._left_card_count > 0) {
				// 杠牌判断
				action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
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
		roomResponse.setType(1013); // 宁都麻将结算类型
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();
		GameEndResponse_GZCG.Builder gameEndGzcg = GameEndResponse_GZCG.newBuilder();

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

			for (int i = 0; i < GRR._especial_card_count; i++) {
				game_end.addEspecialShowCards(GRR._especial_show_cards[i]);
			}

			GRR._end_type = reason;

			int hupaiscore[] = new int[getTablePlayerNumber()];// 胡牌分
			int totalScore[] = new int[getTablePlayerNumber()];// 总分=胡牌分+杠分+跟庄
			int genZhuangScore[] = new int[getTablePlayerNumber()];// 跟庄分
			int mingGangScore[] = new int[getTablePlayerNumber()]; // 明杆得分
			int zhiGangScore[] = new int[getTablePlayerNumber()]; // 直杆得分
			int anGangScore[] = new int[getTablePlayerNumber()]; // 暗杠得分
			int mingGangCard[][] = new int[getTablePlayerNumber()][4]; // 明杠牌
			int zhiGangCard[][] = new int[getTablePlayerNumber()][4]; // 直杠牌
			int anGangCard[][] = new int[getTablePlayerNumber()][4]; // 暗杆牌

			// 杠牌，每个人的分数
			float lGangScore[] = new float[getTablePlayerNumber()];

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
					for (int k = 0; k < getTablePlayerNumber(); k++) {
						lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
					}
				}

				for (int j = 0; j < GRR._weave_count[i]; j++) {
					if (GRR._weave_items[i][j].weave_kind == GameConstants.WIK_GANG) {
						if (GRR._weave_items[i][j].type == GameConstants.GANG_TYPE_JIE_GANG) {
							int score = GRR._weave_items[i][j].weave_score;

							zhiGangScore[GRR._weave_items[i][j].provide_player] -= score;
							zhiGangScore[i] += score;
							zhiGangCard[i][j] = GRR._weave_items[i][j].center_card;
						} else {
							for (int k = 0; k < getTablePlayerNumber(); k++) {
								int score = GRR._weave_items[i][j].weave_score;
								if (k == i) {
									continue;
								}
								if (GRR._weave_items[i][j].type == GameConstants.GANG_TYPE_AN_GANG) {
									anGangScore[k] -= score;
									anGangScore[i] += score;
									anGangCard[i][j] = GRR._weave_items[i][j].center_card;
								} else {
									mingGangScore[k] -= score;
									mingGangScore[i] += score;
									mingGangCard[i][j] = GRR._weave_items[i][j].center_card;
								}
							}
						}
					}
				}

				// 记录
				for (int j = 0; j < getTablePlayerNumber(); j++) {
					_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
				}

			}
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				hupaiscore[i] = (int) GRR._game_score[i] - maPaiScore[i];
			}
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				GRR._game_score[i] += lGangScore[i];
				GRR._game_score[i] += GRR._start_hu_score[i]; // 起手胡分数

				// 跟庄分数
				if (shang_ju_zhuang != i && has_rule(GameConstants_ND.GAME_RULE_GEN_ZHUANG)) {
					GRR._game_score[i] += genZhuangCount;
					GRR._game_score[shang_ju_zhuang] -= genZhuangCount;
					genZhuangScore[i] += genZhuangCount;
					genZhuangScore[shang_ju_zhuang] -= genZhuangCount;
				}
			}
			
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				totalScore[i] = (int) GRR._game_score[i];
				// 记录
				_player_result.game_score[i] += GRR._game_score[i];

			}

			// 结算统计
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				gameEndGzcg.addHuPaiScore(hupaiscore[i]);
				gameEndGzcg.addMingGang(mingGangScore[i]);
				gameEndGzcg.addAnGang(anGangScore[i]);
				gameEndGzcg.addChaoZhuang(genZhuangScore[i]);
				gameEndGzcg.addTotalScore(totalScore[i]);
				gameEndGzcg.addJingScore(zhiGangScore[i]);
				gameEndGzcg.addGangJing(maPaiScore[i]);

				EveryJingInfo_GZCG.Builder gangPai = EveryJingInfo_GZCG.newBuilder();
				for (int k = 0; k < mingGangCard.length; k++) {
					if (mingGangCard[i][k] == 0) {
						continue;
					}
					gangPai.addFuJingCount(mingGangCard[i][k]);
				}
				for (int k = 0; k < anGangCard.length; k++) {
					if (anGangCard[i][k] == 0) {
						continue;
					}
					gangPai.addZhengJingCount(anGangCard[i][k]);
				}
				for (int k = 0; k < zhiGangCard.length; k++) {
					if (zhiGangCard[i][k] == 0) {
						continue;
					}
					gangPai.addEveryJingScore(zhiGangCard[i][k]);
				}
				gameEndGzcg.addJingInfo(gangPai);
			}
			shang_ju_zhuang = _cur_banker;

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
		gameEndGzcg.setGameEnd(game_end);
		roomResponse.setGameEnd(game_end);
		roomResponse.setCommResponse(PBUtil.toByteString(gameEndGzcg));
		this.send_response_to_room(roomResponse);
		game_end.setCommResponse(PBUtil.toByteString(gameEndGzcg));
		record_game_round(game_end);


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
		int[] cards_of_player0 = new int[] { 0x11,0x11,0x12,0x13,0x14,0x15,0x15,0x15,0x22,0x22,0x27,0x27,0x27 };
		int[] cards_of_player1 = new int[] { 0x11,0x11,0x12,0x13,0x14,0x15,0x15,0x15,0x22,0x22,0x27,0x27,0x27 };
		int[] cards_of_player3 = new int[] { 0x11,0x11,0x12,0x13,0x14,0x15,0x15,0x15,0x22,0x22,0x27,0x27,0x27};
		int[] cards_of_player2 = new int[] { 0x11,0x11,0x12,0x13,0x14,0x15,0x15,0x15,0x22,0x22,0x27,0x27,0x27 };

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

}
