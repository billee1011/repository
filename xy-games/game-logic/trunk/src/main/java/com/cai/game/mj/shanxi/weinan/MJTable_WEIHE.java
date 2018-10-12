package com.cai.game.mj.shanxi.weinan;

import java.util.Arrays;
import java.util.Random;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.GameConstants_ND;
import com.cai.common.constant.game.mj.GameConstants_WEIHE;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.PerformanceTimer;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJType;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.game.util.GameUtilConstants;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJTable_WEIHE extends AbstractMJTable {

	// 特有处理器
	public MJHandlerDingQue_WEIHE _handler_ding_que;
	public MJHandlerQiShouHu_WEIHE _handler_qi_shou;

	private int[] dispatchcardNum; // 摸牌次数
	/**
	 * 是否已经选择了定缺
	 */
	public boolean[] had_ding_que = new boolean[getTablePlayerNumber()];

	/**
	 * 玩家定缺时选的颜色
	 */
	public int[] ding_que_pai_se = new int[getTablePlayerNumber()];

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
	public MJTable_WEIHE() {
		super(MJType.GAME_TYPE_MJ_WEINAN);
	}

	private static final long serialVersionUID = 1L;

	@Override
	public int getTablePlayerNumber() {
		return GameConstants.GAME_PLAYER;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.game.mj.AbstractMJTable#onInitTable()
	 */
	@Override
	protected void onInitTable() {
		_handler_dispath_card = new MJHandlerDispatchCard_WEIHE();
		_handler_out_card_operate = new MJHandlerOutCardOperate_WEIHE();
		_handler_gang = new MJHandlerGang_WEIHE();
		_handler_chi_peng = new MJHandlerChiPeng_WEIHE();
		_handler_ding_que = new MJHandlerDingQue_WEIHE();
		_handler_qi_shou = new MJHandlerQiShouHu_WEIHE();
	}

	/**
	 * 参数初始化
	 */
	protected void onInitParam() {
		dispatchcardNum = new int[getTablePlayerNumber()];
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			this.ding_que_pai_se[i] = 0;
			this.had_ding_que[i] = false;
		}
	}

	public void progress_banker_select() {
		if (_cur_banker == GameConstants.INVALID_SEAT) {
			_cur_banker = getOpenRoomIndex();// 创建者的玩家为专家
			_shang_zhuang_player = GameConstants.INVALID_SEAT;
			_lian_zhuang_player = GameConstants.INVALID_SEAT;
		}

		if (_cur_round < 2 && !isOpenPlayerInRoom()) {// 金币场 随机庄家
			Random random = new Random();//
			int rand = random.nextInt(6) + 1 + random.nextInt(6) + 1;
			_cur_banker = rand % getTablePlayerNumber();//

			_shang_zhuang_player = GameConstants.INVALID_SEAT;
			_lian_zhuang_player = GameConstants.INVALID_SEAT;
		}
	}

	/**
	 * 游戏开始
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
			int hand_card_count = _logic.switch_to_cards_data_sichuan(GRR._cards_index[i], hand_cards[i],
					ding_que_pai_se[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}
		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			// 自己的牌
			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			// 回放数据
			GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.load_room_info_data(roomResponse);
			this.load_common_status(roomResponse);
			this.load_player_info_data(roomResponse);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);

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

		if (has_rule(GameConstants_WEIHE.GAME_RULE_WEINAN_ZHI_DING_QUE_MEN)) {
			this.exe_ding_que();
		} else {
			this.exe_dispatch_card(_current_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);
		}

		return false;
	}

	/**
	 * 全部玩家进行一次听牌
	 */
	public void firstTing() {
		// 检测听牌
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i]._hu_card_count = this.get_ting_card(_playerStatus[i]._hu_cards, GRR._cards_index[i],
					GRR._weave_items[i], GRR._weave_count[i], false, i);
			if (_playerStatus[i]._hu_card_count > 0) {
				this.operate_chi_hu_cards(i, _playerStatus[i]._hu_card_count, _playerStatus[i]._hu_cards);
			}
		}
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card,
			ChiHuRight chiHuRight, int card_type, int _seat_index) {

		if (cur_card == 0)
			return GameConstants.WIK_NULL;

		// 构造数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}
		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		int cbChiHuKind = GameConstants.WIK_NULL;

		long qxd = _logic.is_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card);
		if (qxd != GameConstants.WIK_NULL) {

			chiHuRight.opr_or(qxd);

			cbChiHuKind = GameConstants.WIK_CHI_HU;
		}

		if (isDispatchcardNum(_cur_banker)) {
			if (_seat_index == _cur_banker && _out_card_count == 0) {
				chiHuRight.opr_or(GameConstants.CHR_HUNAN_TIAN_HU);
			} else {
				boolean can = true;
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (i == _cur_banker) {
						continue;
					}
					if (isDispatchcardNum(i)) {
						can = false;
					}
				}
				if (can) {
					if (_out_card_count == 1) {
						chiHuRight.opr_or(GameConstants.CHR_HUNAN_DI_HU);
					}
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

		// 金钩钓
		boolean dandiao = _logic.is_dan_diao(cards_index, cur_card);
		if (dandiao) {
			chiHuRight.opr_or(GameConstants_ND.CHR_HENAN_DAN_DIAO);
		}

		// 根胡
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (cbCardIndexTemp[i] == 4) {
				chiHuRight.an_ka_count++;
			}
		}

		for (int i = 0; i < weave_count; i++) {
			if (weaveItems[i].weave_kind == GameConstants.WIK_PENG) {
				int index = _logic.switch_to_card_index(weaveItems[i].center_card);
				if (cbCardIndexTemp[index] == 1) {
					chiHuRight.an_ka_count++;
				}
			}
		}

		// 碰碰胡:大对子
		boolean is_peng_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index(cards_index,
				_logic.switch_to_card_index(cur_card), magic_cards_index, magic_card_count);

		if (is_peng_hu && !dandiao) {
			boolean exist_eat = exist_eat(weaveItems, weave_count);
			if (!exist_eat) {
				chiHuRight.opr_or(GameConstants.CHR_HUNAN_PENGPENG_HU);
			}
		}

		// 清一色
		if (_logic.is_qing_yi_se(cards_index, weaveItems, weave_count, cur_card)) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_QING_YI_SE);
		}

		if (GRR._left_card_count == 0) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_HAI_DI_LAO);
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;
		if (card_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_GANG_KAI);
		} else if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else if (card_type == GameConstants.HU_CARD_TYPE_QIANGGANG) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_QIANG_GANG_HU);// 抢杠胡
		} else if (card_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_GANG_KAI);// 抢杠胡
		} else if (card_type == GameConstants.HU_CARD_TYPE_GANG_PAO) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_GANG_SHANG_PAO);// 抢杠胡
		} else {
			chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
		}

		// 炮胡番
		chiHuRight.setPao_fan(get_chi_hu_fen(chiHuRight));
		if (!can_win) {
			if (qxd != GameConstants.WIK_NULL) {
				return cbChiHuKind;
			} else {
				chiHuRight.set_empty();
				return GameConstants.WIK_NULL;
			}
		}

		return cbChiHuKind;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.game.mj.AbstractMJTable#process_chi_hu_player_score(int,
	 * int, int, boolean)
	 */
	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int wFanShu = get_chi_hu_fen(chr);

		countCardType(chr, seat_index);

		int lChiHuScore = wFanShu * getDiScore();// wFanShu*m_pGameServiceOption->lCellScore;

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
				int pao = 0;
				if (has_rule(GameConstants_WEIHE.GAME_RULE_WEINAN_PAO_FEN_SUI_FAN)) {
					pao = wFanShu * getPaoScore();
				} else {
					pao = 2 * (getPaoScore() + getPaoScore());
				}
				pao *= getDiScore();
				s += pao;
				// 胡牌分
				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;
			}
		} else {
			int s = lChiHuScore;
			int pao = 0;
			if (has_rule(GameConstants_WEIHE.GAME_RULE_WEINAN_PAO_FEN_SUI_FAN)) {
				pao = wFanShu * getPaoScore();
			} else {
				pao = getPaoScore() + getPaoScore();
			}
			pao *= getDiScore();
			s += pao;
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

	/**
	 * 获取炮数
	 */
	public int getPaoScore() {
		int pao = 0;
		if (has_rule(GameConstants_WEIHE.GAME_RULE_WEINAN_PAO_1)) {
			pao = 1;
		} else if (has_rule(GameConstants_WEIHE.GAME_RULE_WEINAN_PAO_2)) {
			pao = 2;
		} else if (has_rule(GameConstants_WEIHE.GAME_RULE_WEINAN_PAO_3)) {
			pao = 3;
		} else if (has_rule(GameConstants_WEIHE.GAME_RULE_WEINAN_PAO_4)) {
			pao = 4;
		}
		return pao;
	}

	/**
	 * 获取选择底分
	 * 
	 * @return
	 */
	public int getDiScore() {
		int di = 0;
		if (has_rule(GameConstants_WEIHE.GAME_RULE_WEINAN_DI_FENG_1)) {
			di = 1;
		} else if (has_rule(GameConstants_WEIHE.GAME_RULE_WEINAN_DI_FENG_2)) {
			di = 2;
		} else if (has_rule(GameConstants_WEIHE.GAME_RULE_WEINAN_DI_FENG_5)) {
			di = 5;
		}

		return di;
	}

	// 胡牌底分
	public int get_chi_hu_fen(ChiHuRight chiHuRight) {
		int wFanShu = 0;

		// 七对
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_QI_XIAO_DUI)).is_empty()) {
			wFanShu = 3;
		} else if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI)).is_empty()) {
			wFanShu = 5;
		} else if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI)).is_empty()) {
			wFanShu = 8;
		}

		// 碰碰胡
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_PENGPENG_HU)).is_empty()) {
			wFanShu = 2;
		}

		// 大吊车
		if (!(chiHuRight.opr_and(GameConstants_ND.CHR_HENAN_DAN_DIAO)).is_empty()) {
			wFanShu = 3;
		}

		// 杠开
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_GANG_KAI)).is_empty()) {
			wFanShu += 3;
		}
		// 抢杠胡
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_QIANG_GANG_HU)).is_empty()) {
			wFanShu += 2;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_GANG_SHANG_PAO)).is_empty()) {
			wFanShu += 2;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_HAI_DI_LAO)).is_empty()) {
			wFanShu += 2;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_QING_YI_SE)).is_empty()) {
			wFanShu += 4;
		}

		// 天胡
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_TIAN_HU)).is_empty()) {
			wFanShu += 8;
		}

		// 地胡
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_DI_HU)).is_empty()) {
			wFanShu += 6;
		}

		if (chiHuRight.an_ka_count > 0) {
			wFanShu += 2;
		}
		if (wFanShu == 0) {
			wFanShu = 1;
		}

		return wFanShu;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.game.mj.AbstractMJTable#set_result_describe()
	 */
	@Override
	protected void set_result_describe() {
		int l;
		long type = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			String des = "";
			boolean canGengHu = true;
			l = GRR._chi_hu_rights[i].type_count;

			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					if (type == GameConstants.CHR_ZI_MO) {
						des += " 自摸";
					}
					if (type == GameConstants.CHR_HUNAN_DI_HU) {
						des += " 地胡";
					}
					if (type == GameConstants.CHR_HUNAN_TIAN_HU) {
						des += " 天炸";
					}

					if (type == GameConstants.CHR_SHU_FAN) {
						des += " 接炮";
					}
					if (type == GameConstants.CHR_HUNAN_QIANG_GANG_HU) {
						des += " 抢杠胡";
					}

					if (type == GameConstants_ND.CHR_HENAN_DAN_DIAO) {
						des += " 金勾勾";
					}

					if (type == GameConstants.CHR_HUNAN_GANG_KAI) {
						des += " 杠开";
					}
					if (type == GameConstants.CHR_HUNAN_GANG_SHANG_PAO) {
						des += " 杠上炮";
					}

					if (type == GameConstants.CHR_HUNAN_PENGPENG_HU) {
						des += " 大对胡";
					}
					if (type == GameConstants.CHR_HUNAN_QING_YI_SE) {
						des += " 清一色";
					}

					if (type == GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI) {
						des += " 豪华七小对";
					}
					if (type == GameConstants.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI) {
						des += " 	超级豪华七小对";
					}

					if (type == GameConstants.CHR_HUNAN_QI_XIAO_DUI) {
						des += " 七小对";
					}

					if (type == GameConstants.CHR_HUNAN_HAI_DI_LAO) {
						des += " 海底胡";
					}
					if (canGengHu && GRR._chi_hu_rights[i].an_ka_count > 0) {
						des += " 根胡";
						canGengHu = false;
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
	public boolean estimate_player_out_card_respond(int seat_index, int card, int type) {
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
			int cards[] = new int[GameConstants.MAX_COUNT];
			_logic.switch_to_cards_data_sichuan(GRR._cards_index[i], cards, ding_que_pai_se[i]);
			// 还有缺牌不进行碰杠胡操作
			if (isQueCard(card, i)) {
				continue;
			}

			playerStatus = _playerStatus[i];

			//// 碰牌判断
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
				playerStatus.add_gang(card, seat_index, 1);// 加上杠
				bAroseAction = true;
			}

			if (type == GameConstants.INVALID_SEAT) {
				type = GameConstants.HU_CARD_TYPE_PAOHU;
			}
			// 可以胡的情况
			boolean canHu = true;
			if (has_rule(GameConstants_WEIHE.GAME_RULE_WEINAN_LOU_HU_ONE)) {
				if (!_playerStatus[i].is_chi_hu_round() && _playerStatus[i].guo_hu_card == card) {
					canHu = false;
				}
			} else {
				if (!_playerStatus[i].is_chi_hu_round()) {
					canHu = false;
				}
			}
			if (canHu) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];
				// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr, type,
						i);

				// 结果判断
				if (action != 0) {
					if (chr.getPao_fan() < 2 && has_rule(GameConstants_WEIHE.GAME_RULE_WEINAN_ZHI_DING_QUE_MEN)) {
						this.operate_effect_action(i, GameConstants_WEIHE.EFFECT_ACTION_TYPE, 1,
								new long[] { GameConstants_WEIHE.Effect_Action_Pao_Hu_Fan }, 2000, i);
					} else {
						_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
						_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
						bAroseAction = true;
					}
				}
			}
		}

		if (bAroseAction)

		{
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
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						GameConstants.HU_CARD_TYPE_QIANGGANG, i);

				// 结果判断
				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
					chr.opr_or(GameConstants.CHR_HUNAN_QIANG_GANG_HU);// 抢杠胡
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

			for (int i = 0; i < GRR._especial_card_count; i++) {
				game_end.addEspecialShowCards(GRR._especial_show_cards[i]);
			}

			GRR._end_type = reason;

			// 杠牌，每个人的分数
			float lGangScore[] = new float[getTablePlayerNumber()];

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

			for (int i = 0; i < getTablePlayerNumber(); i++) {

				GRR._game_score[i] += lGangScore[i];

				GRR._game_score[i] += GRR._start_hu_score[i]; // 起手胡分数

				// 记录
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

	public boolean operate_player_info(int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);
		roomResponse.setGameStatus(_game_status);

		load_player_info_data(roomResponse, seat_index);

		if (GRR != null)
			GRR.add_room_response(roomResponse);

		if (seat_index == GameConstants.INVALID_SEAT) {

			send_response_to_room(roomResponse);
		} else {
			send_response_to_player(seat_index, roomResponse);
		}

		return true;
	}

	public boolean exe_ding_que() {
		set_handler(_handler_ding_que);
		_handler.exe(this);
		return true;
	}

	/**
	 * 起手，进行Handler处理器的切换
	 * 
	 * @param seat_index
	 * @param type
	 * @return
	 */
	public boolean exe_qi_shou(int seat_index, int type) {
		set_handler(_handler_qi_shou);

		// _handler_qi_shou.reset_status(seat_index, type);
		_handler_qi_shou.exe(this);

		return true;
	}

	@Override
	public void load_player_info_data(RoomResponse.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			rplayer = get_players()[i];
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

			// 玩家定缺的牌色，1，万，2，条，3，筒，0，还没选定缺
			room_player.setPao(null != ding_que_pai_se ? ding_que_pai_se[i] : 0);

			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			room_player.setHasPiao(_player_result.haspiao[i]);
			room_player.setZiBa(_player_result.ziba[i]);
			room_player.setDuanMen(_player_result.duanmen[i]);
			room_player.setOpenThree(_player_open_less[i] == 0 ? false : true);

			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}

			roomResponse.addPlayers(room_player);
		}
	}

	public void load_player_info_data(RoomResponse.Builder roomResponse, int seat_index) {
		Player rplayer;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			rplayer = get_players()[i];
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

			// 玩家定缺的牌色，1，万，2，条，3，筒，0，还没选定缺
			if (seat_index == i || seat_index == GameConstants.INVALID_SEAT) {
				room_player.setPao(null != ding_que_pai_se ? ding_que_pai_se[i] : 0);
			} else {
				room_player.setPao(0);
			}

			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			room_player.setHasPiao(_player_result.haspiao[i]);
			room_player.setZiBa(_player_result.ziba[i]);
			room_player.setDuanMen(_player_result.duanmen[i]);
			room_player.setOpenThree(_player_open_less[i] == 0 ? false : true);

			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}

			roomResponse.addPlayers(room_player);
		}
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
			roomResponse.addCardData(cards[j]);
		}

		int ting_count = _playerStatus[seat_index]._hu_out_card_count;

		// 打出去可以听牌的个数
		roomResponse.setOutCardCount(ting_count);

		for (int i = 0; i < ting_count; i++) {
			int ting_card_cout = _playerStatus[seat_index]._hu_out_card_ting_count[i];
			roomResponse.addOutCardTingCount(ting_card_cout);
			roomResponse.addOutCardTing(
					_playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants.CARD_ESPECIAL_TYPE_TING);

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

	/**
	 * 使用定飘呛进行定缺
	 */
	@Override
	public boolean handler_requst_pao_qiang(Player player, int pai_se, int other) {
		return _handler_ding_que.handler_ding_que(this, player.get_seat_index(), pai_se);
	}

	/**
	 * 当是硬缺模式的时候转换可出牌值
	 * 
	 * @param card_index
	 * @param seat_index
	 * @return true:有转换的牌
	 */
	public boolean changCards(int card_datas[], int seat_index) {
		boolean flag = false;
		if (has_rule(GameConstants_WEIHE.GAME_RULE_WEINAN_ZHI_DING_QUE_MEN)) {

			boolean needChang = isNeedChang(card_datas, seat_index);
			if (needChang) {
				for (int i = 0; i < card_datas.length; i++) {
					int card = card_datas[i];
					if (card == 0) {
						continue;
					}
					int color = _logic.get_card_color(card);
					if (color + 1 != ding_que_pai_se[seat_index]) {
						card_datas[i] += GameConstants_WEIHE.CARD_ESPECIAL_TYPE_CANT_OUT;
						flag = true;
					}
				}
			}

		}

		return flag;
	}

	public boolean isNeedChang(int card_datas[], int seat_index) {
		boolean needChang = false;
		if (has_rule(GameConstants_WEIHE.GAME_RULE_WEINAN_ZHI_DING_QUE_MEN)) {
			for (int i = 0; i < card_datas.length; i++) {
				int card = card_datas[i];
				if (card == 0) {
					continue;
				}
				int color = _logic.get_card_color(card);
				if (color + 1 == ding_que_pai_se[seat_index]) {
					needChang = true;
				}
			}
		}
		return needChang;
	}

	public boolean isQueCard(int card_data, int seat_index) {
		boolean needChang = false;
		if (has_rule(GameConstants_WEIHE.GAME_RULE_WEINAN_ZHI_DING_QUE_MEN)) {
			int color = _logic.get_card_color(card_data);
			if (color + 1 == ding_que_pai_se[seat_index]) {
				needChang = true;
			}
		}
		return needChang;
	}

	@Override
	public void test_cards() {

		int[] cards_of_player0 = new int[] { 0x02, 0x02, 0x23, 0x23, 0x14, 0x14, 0x01, 0x01, 0x15, 0x15, 0x29, 0x29,
				0x26 };
		int[] cards_of_player1 = new int[] { 0x02, 0x02, 0x23, 0x23, 0x14, 0x14, 0x01, 0x01, 0x15, 0x15, 0x29, 0x29,
				0x26 };
		int[] cards_of_player3 = new int[] { 0x02, 0x02, 0x23, 0x23, 0x14, 0x14, 0x01, 0x01, 0x15, 0x15, 0x29, 0x29,
				0x26 };
		int[] cards_of_player2 = new int[] { 0x02, 0x02, 0x23, 0x23, 0x14, 0x14, 0x01, 0x01, 0x15, 0x15, 0x29, 0x29,
				0x26 };

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

}
