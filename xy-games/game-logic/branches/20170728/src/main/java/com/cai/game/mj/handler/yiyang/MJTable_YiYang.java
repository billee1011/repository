package com.cai.game.mj.handler.yiyang;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.BrandLogModel;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.GameDescUtil;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.DispatchLastCardRunnable;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJGameLogic.AnalyseItem;
import com.cai.game.mj.MJType;

import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;

public class MJTable_YiYang extends AbstractMJTable {

	BrandLogModel _recordRoomRecord;

	protected MJHandlerOutCardBaoTing_YiYang _handler_out_card_bao_ting; // 报听
	protected MJHandlerDispatchLastCard_YiYang _handler_dispath_last_card;

	public MJTable_YiYang() {
		super(MJType.GAME_TYPE_HUNAN_YIYANG);
	}

	@Override
	protected void onInitTable() {
		_handler_dispath_card = new MJHandlerDispatchCard_YiYang();
		_handler_out_card_operate = new MJHandlerOutCardOperate_YiYang();
		_handler_gang = new MJHandlerGang_YiYang();
		_handler_chi_peng = new MJHandlerChiPeng_YiYang();
		_handler_out_card_bao_ting = new MJHandlerOutCardBaoTing_YiYang();
		_handler_dispath_last_card = new MJHandlerDispatchLastCard_YiYang();
	}

	@Override
	public String get_game_des() {
		String des = "";
		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HUNAN_DAIFENG)) {
			des += "带风";
		} else {
			des += "不带风";
		}
		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HUNAN_EAT)) {
			des += "\n" + "可吃";
		} else {
			des += "\n" + "不可吃";
		}
		return des;
	}

	@Override
	protected boolean on_game_start() {
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
			roomResponse.setCurrentPlayer(this._current_player == GameConstants.INVALID_SEAT ? this._resume_player : this._current_player);
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

		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(this.GRR._left_card_count);
		this.GRR.add_room_response(roomResponse);

		// 检测听牌
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			this._playerStatus[i]._hu_card_count = get_ting_card(this._playerStatus[i]._hu_cards, this.GRR._cards_index[i], this.GRR._weave_items[i],
					this.GRR._weave_count[i], i);
			if (this._playerStatus[i]._hu_card_count > 0) {
				this.operate_chi_hu_cards(i, this._playerStatus[i]._hu_card_count, this._playerStatus[i]._hu_cards);
			}
		}

		this.exe_dispatch_card(_current_player, GameConstants.WIK_NULL, 0);

		return true;
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weaveCount, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {

		// 构造扑克
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入扑克
		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		// 清一色
		if (_logic.is_qing_yi_se(cbCardIndexTemp, weaveItems, weaveCount, cur_card)) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_QING_YI_SE);
		}

		// 七对
		int qiXiaoDui = _logic.is_qi_xiao_dui(cbCardIndexTemp, weaveItems, weaveCount, cur_card);
		if (qiXiaoDui != GameConstants.WIK_NULL) {
			chiHuRight.opr_or(qiXiaoDui);
			return GameConstants.WIK_CHI_HU;
		}

		// 大对胡
		if (_logic.is_da_dui_hu_ShaoYang(cbCardIndexTemp, weaveItems, weaveCount, cur_card) != GameConstants.WIK_NULL) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_DA_DUI_HU);
			return GameConstants.WIK_CHI_HU;
		}

		// 风一色
		if (_logic.is_feng_is_se(cbCardIndexTemp, weaveItems, weaveCount, cur_card) != GameConstants.WIK_NULL) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_FENG_YI_SE);
			return GameConstants.WIK_CHI_HU;
		}

		// 十三幺
		if (_logic.isShiSanYao(cbCardIndexTemp, weaveItems, weaveCount)) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_SHI_SAN_YAO);
			return GameConstants.WIK_CHI_HU;
		}

		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
		boolean bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weaveCount, analyseItemArray, true);
		if (!bValue) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		// 门清
		if (weaveCount == 0) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_MEN_QING);
			return GameConstants.WIK_CHI_HU;
		}

		return GameConstants.WIK_CHI_HU;
	}

	public int get_ting_card(int[] cards, int cards_index[], WeaveItem weaveItems[], int cbWeaveCount, int seatIndex) {
		// 复制数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();
		int cbCurrentCard;
		int count = 0;
		// 遍历所有的牌去判断能不能胡牌
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (this._logic.is_magic_index(i))
				continue;
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cards_index, weaveItems, cbWeaveCount, cbCurrentCard, chr,
					GameConstants.HU_CARD_TYPE_ZIMO, seatIndex)) {
				cards[count++] = cbCurrentCard;
			}
		}
		return count;
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int wFanShu = 2;// 番数
		countCardType(chr, seat_index);

		// 统计
		if (zimo) {
			// 自摸
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				GRR._lost_fan_shu[i][seat_index] = wFanShu;
			}
		} else {// 点炮
			GRR._lost_fan_shu[provide_index][seat_index] = wFanShu;
		}

		float lChiHuScore = wFanShu;

		// 胡的人是庄家
		boolean zhuang_hu = (GRR._banker_player == seat_index ? true : false);
		// 是否是庄家放胡
		boolean zhuang_fang_hu = (GRR._banker_player == provide_index ? true : false);

		////////////////////////////////////////////////////// 自摸 算分
		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}

				float s = getScore(chr, lChiHuScore);
				// 胡牌分
				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;
			}
		}
		////////////////////////////////////////////////////// 点炮 算分
		else {
			float s = getScore(chr, lChiHuScore);

			// 胡牌分
			GRR._game_score[provide_index] -= s;
			GRR._game_score[seat_index] += s;

			GRR._chi_hu_rights[provide_index].opr_or(GameConstants.CHR_FANG_PAO);
		}

		// 设置变量
		GRR._provider[seat_index] = provide_index;
		_status_gang = false;
		_status_gang_hou_pao = false;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);
	}

	private float getScore(ChiHuRight chr, float lChiHuScore) {
		float s = lChiHuScore;

		if (!(chr.opr_and(GameConstants.CHR_HUNAN_QING_YI_SE)).is_empty()) {
			s = 8;
			if (!(chr.opr_and(GameConstants.CHR_HUNAN_MEN_QING)).is_empty()) {
				s = 16;
			}
			if (!(chr.opr_and(GameConstants.CHR_HUNAN_DA_DUI_HU)).is_empty()) {
				s = 16;
			}
			if (!(chr.opr_and(GameConstants.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI)).is_empty()) {
				s = 16;
			}
			if (!(chr.opr_and(GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI)).is_empty()) {
				s = 16;
			}
			if (!(chr.opr_and(GameConstants.CHR_HUNAN_QI_XIAO_DUI)).is_empty()) {
				s = 16;
			}
		} else {
			if (!(chr.opr_and(GameConstants.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI)).is_empty()) {
				s = 16;
			}
			if (!(chr.opr_and(GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI)).is_empty()) {
				s = 16;
			}
			if (!(chr.opr_and(GameConstants.CHR_HUNAN_QI_XIAO_DUI)).is_empty()) {
				s = 8;
			}
			if (!(chr.opr_and(GameConstants.CHR_HUNAN_DA_DUI_HU)).is_empty()) {
				s = 8;
			}
			if (!(chr.opr_and(GameConstants.CHR_HUNAN_MEN_QING)).is_empty()) {
				s = 4;
			}
		}
		if (!(chr.opr_and(GameConstants.CHR_HUNAN_FENG_YI_SE)).is_empty()) {
			s = 24;
		}
		if (!(chr.opr_and(GameConstants.CHR_HUNAN_SHI_SAN_YAO)).is_empty()) {
			s = 24;
		}
		return s;
	}

	@Override
	protected void set_result_describe() {
		int chrTypes;
		long type = 0;
		for (int player = 0; player < GameConstants.GAME_PLAYER; player++) {
			StringBuilder gameDesc = new StringBuilder("");

			chrTypes = GRR._chi_hu_rights[player].type_count;

			for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
				type = GRR._chi_hu_rights[player].type_list[typeIndex];

				if (GRR._chi_hu_rights[player].is_valid()) {

					if (type == GameConstants.WIK_ZI_MO) {
						gameDesc.append(" 自摸");
					}
					if (type == GameConstants.CHR_SHU_FAN) {
						gameDesc.append(" 接炮");
					}
					if (type == GameConstants.CHR_HUNAN_QIANG_GANG_HU) {
						gameDesc.append(" 抢杠胡");
					}
					if (type == GameConstants.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI || type == GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI) {
						gameDesc.append(" 龙七对");
					}
					if (type == GameConstants.CHR_HUNAN_QI_XIAO_DUI) {
						gameDesc.append(" 七巧对");
					}
					if (type == GameConstants.CHR_HUNAN_SHUANG_GANG_KAI) {
						gameDesc.append(" 杠上开花");
					}
					if (type == GameConstants.CHR_HUNAN_QING_YI_SE) {
						gameDesc.append(" 清一色");
					}
					if (type == GameConstants.CHR_HUNAN_DA_DUI_HU) {
						gameDesc.append(" 大对胡");
					}
					if (type == GameConstants.CHR_HUNAN_FENG_YI_SE) {
						gameDesc.append(" 风一色");
					}
					if (type == GameConstants.CHR_HUNAN_SHI_SAN_YAO) {
						gameDesc.append(" 十三幺");
					}
				} else if (type == GameConstants.CHR_FANG_PAO) {
					gameDesc.append(" 放炮");
				}
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
	public boolean handler_requst_call_qiang_zhuang(int seat_index, int call_banker, int qiang_banker) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void init_other_param(Object... objects) {
		// TODO Auto-generated method stub

	}

	public boolean estimate_player_out_card_respond(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		// 用户状态
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {

			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		int llcard = 0;
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
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr, GameConstants.HU_CARD_TYPE_PAOHU,
						seat_index);

				// 结果判断
				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
					chr.opr_or(GameConstants.CHR_SHU_FAN);
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

	public boolean exe_dispatch_last_card(int seat_index, int type, int delay_time) {

		if (delay_time > 0) {
			GameSchedule.put(new DispatchLastCardRunnable(this.getRoom_id(), seat_index, type, false), delay_time, TimeUnit.MILLISECONDS);// MJGameConstants.GANG_LAST_CARD_DELAY
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
}
