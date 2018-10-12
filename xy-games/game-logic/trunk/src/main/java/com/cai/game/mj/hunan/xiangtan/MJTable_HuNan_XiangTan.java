package com.cai.game.mj.hunan.xiangtan;

import java.util.ArrayList;
import java.util.List;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.define.ECardType;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJGameLogic.AnalyseItem;
import com.cai.game.mj.MJType;
import com.cai.service.MongoDBServiceImpl;

import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;

public class MJTable_HuNan_XiangTan extends AbstractMJTable {

	private static final long serialVersionUID = 6611671011530711969L;

	protected MJHandlerQiShouHun_HuNan_XiangTan _handler_qishou_hun;
	protected MJHandlerSelectMagicCard_HuNan_XiangTan _handler_select_magic_card;
	protected MJHandlerGangXuanMei_HuNan_XiangTan _handler_gang_xuan_mei;

	public MJTable_HuNan_XiangTan() {
		super(MJType.GAME_TYPE_HUNAN_XIANG_TAN);
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {
		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		// 变量定义
		int cbChiHuKind = GameConstants.WIK_NULL;

		boolean need_to_check_258 = true; // 是否需要258将
		boolean is_xiao_hu = true; // 是否为小胡（平胡）
		boolean can_win = false; // 是否能胡牌

		int check_qi_xiao_dui = _logic.is_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card);

		if (check_qi_xiao_dui != GameConstants.WIK_NULL) { // 七小对，不需要258将，能直接胡牌
			need_to_check_258 = false;
			is_xiao_hu = false;
			can_win = true;

			cbChiHuKind = GameConstants.WIK_CHI_HU;
			chiHuRight.opr_or(MJConstants_HuNan_XiangTan.CHR_QI_XIAO_DUI);
		}

		boolean is_qing_yi_se = _logic.is_qing_yi_se(cards_index, weaveItems, weave_count, cur_card);

		if (is_qing_yi_se) { // 清一色，不需要258将
			need_to_check_258 = false;
			is_xiao_hu = false;

			cbChiHuKind = GameConstants.WIK_CHI_HU;
			chiHuRight.opr_or(MJConstants_HuNan_XiangTan.CHR_QING_YI_SE);
		}

		if (weave_count == 0) { // 门清，需要258将
			is_xiao_hu = false;

			cbChiHuKind = GameConstants.WIK_CHI_HU;
			chiHuRight.opr_or(MJConstants_HuNan_XiangTan.CHR_MEN_QING);
		}

		// 构造扑克
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入扑克
		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		// 分析扑克
		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
		boolean bValue = _logic.analyse_card_henan_zhou_kou(cbCardIndexTemp, weaveItems, weave_count, analyseItemArray, false);

		if (bValue) { // 如果能胡
			can_win = true;

			for (AnalyseItem analyseItem : analyseItemArray) {
				if (_logic.is_pengpeng_hu(analyseItem)) { // 碰碰胡
					need_to_check_258 = false;
					is_xiao_hu = false;
					chiHuRight.opr_or(MJConstants_HuNan_XiangTan.CHR_PENG_PENG_HU);
				}
			}

			if (need_to_check_258) { // 如果需要检查258将，也就是没有七小对、清一色、碰碰胡
				can_win = false;

				// 胡牌分析 有没有258
				for (int i = 0; i < analyseItemArray.size(); i++) {
					// 变量定义
					AnalyseItem pAnalyseItem = analyseItemArray.get(i);
					if (pAnalyseItem.bMagicEye) { // 如果红中癞子是牌眼
						if (_logic.is_magic_card(pAnalyseItem.cbCardEye)) { // 如果将牌是红中癞子
							can_win = true;
							break;
						}
					}
					int color = _logic.get_card_color(pAnalyseItem.cbCardEye);
					if (color > 2)
						continue;
					int cbCardValue = _logic.get_card_value(pAnalyseItem.cbCardEye);
					if (cbCardValue == 2 || cbCardValue == 5 || cbCardValue == 8) {
						can_win = true;
						break;
					}
				}
			}
		}

		if (can_win == false) { // 如果不能胡牌
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;

		if (is_xiao_hu) { // 平胡
			chiHuRight.opr_or(MJConstants_HuNan_XiangTan.CHR_PING_HU);
		}

		if (card_type == MJConstants_HuNan_XiangTan.HU_CARD_TYPE_ZI_MO) {
			chiHuRight.opr_or(MJConstants_HuNan_XiangTan.CHR_ZI_MO); // 自摸
		} else if (card_type == MJConstants_HuNan_XiangTan.HU_CARD_TYPE_JIE_PAO) {
			chiHuRight.opr_or(MJConstants_HuNan_XiangTan.CHR_JIE_PAO); // 点炮
		} else if (card_type == MJConstants_HuNan_XiangTan.HU_CARD_TYPE_GANG_KAI) {
			chiHuRight.opr_or(MJConstants_HuNan_XiangTan.CHR_GANG_KAI); // 杠上开花（选美胡）
		} else if (card_type == MJConstants_HuNan_XiangTan.HU_CARD_TYPE_QIANG_GANG_HU) {
			chiHuRight.opr_or(MJConstants_HuNan_XiangTan.CHR_QIANG_GANG_HU); // 抢杠胡（选美炮胡）
		}

		return cbChiHuKind;
	}

	public boolean check_gang_huan_zhang(int seat_index, int card) {
		// 不能换章，需要检测是否改变了听牌
		int gang_card_index = _logic.switch_to_card_index(card);
		int gang_card_count = GRR._cards_index[seat_index][gang_card_index];
		// 假如杠了
		GRR._cards_index[seat_index][gang_card_index] = 0;

		int hu_cards[] = new int[GameConstants.MAX_INDEX];

		// 检查听牌
		int hu_card_count = get_ting_card(hu_cards, GRR._cards_index[seat_index], GRR._weave_items[seat_index], GRR._weave_count[seat_index],
				seat_index);

		// 还原手牌
		GRR._cards_index[seat_index][gang_card_index] = gang_card_count;

		if (hu_card_count != _playerStatus[seat_index]._hu_card_count) {
			return true;
		} else {
			for (int j = 0; j < hu_card_count; j++) {
				if (_playerStatus[seat_index]._hu_cards[j] != hu_cards[j]) {
					return true;
				}
			}
		}

		return false;
	}

	@Override
	public boolean estimate_gang_respond(int seat_index, int card) {
		return true;
	}

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

		if (_logic.is_magic_card(card)) // 打出来的鬼牌其他玩家不能操作
			return false;

		// 动作判断
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			if (GRR._left_card_count > 0) { // 牌堆还有牌才能碰和杠，不然流局算庄会出错
				if (i == this.get_banker_next_seat(seat_index)) { // 如果是下家，可以吃上家打出来的牌
					action = _logic.check_chi(GRR._cards_index[i], card);
					if ((action & GameConstants.WIK_LEFT) != 0) {
						playerStatus.add_action(GameConstants.WIK_LEFT);
						playerStatus.add_chi(card, GameConstants.WIK_LEFT, seat_index);
					}
					if ((action & GameConstants.WIK_CENTER) != 0) {
						playerStatus.add_action(GameConstants.WIK_CENTER);
						playerStatus.add_chi(card, GameConstants.WIK_CENTER, seat_index);
					}
					if ((action & GameConstants.WIK_RIGHT) != 0) {
						playerStatus.add_action(GameConstants.WIK_RIGHT);
						playerStatus.add_chi(card, GameConstants.WIK_RIGHT, seat_index);
					}
					if (playerStatus.has_action()) {
						bAroseAction = true;
					}
				}

				// 碰牌判断
				action = _logic.check_peng(GRR._cards_index[i], card);
				if (action != 0) {
					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);
					bAroseAction = true;
				}

				// 杠牌判断，需要判断是否已听牌，并且杠了之后没换章
				action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
				if (action != 0) {
					if (this.is_ting_card(GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i], i)) { // 玩家已经听牌
						int tmp_index = _logic.switch_to_card_index(card); // 杠的牌的索引

						int resume_card_count = GRR._cards_index[i][tmp_index]; // 杠的牌的数量

						GRR._cards_index[i][tmp_index] = 0; // 减掉杠的牌

						boolean is_ting_state_after_gang = this.is_ting_card(GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i], i);

						GRR._cards_index[i][tmp_index] = resume_card_count; // 把牌加回来

						if (is_ting_state_after_gang) { // 杠后还能听牌
							// 加上刚
							playerStatus.add_action(GameConstants.WIK_GANG);
							playerStatus.add_gang(card, seat_index, 1); // 加上杠
							bAroseAction = true;
						}
					}
				}
			}

			// 构造扑克
			int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
			for (int x = 0; x < GameConstants.MAX_INDEX; x++) {
				cbCardIndexTemp[x] = GRR._cards_index[i][x];
			}

			// 插入扑克
			if (card != GameConstants.INVALID_VALUE) {
				cbCardIndexTemp[_logic.switch_to_card_index(card)]++;
			}

			// 分析扑克
			List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
			boolean bValue = _logic.analyse_card_henan_zhou_kou(cbCardIndexTemp, GRR._weave_items[i], GRR._weave_count[i], analyseItemArray, false);
			boolean is_peng_peng_hu = false;

			if (bValue) {
				for (AnalyseItem analyseItem : analyseItemArray) {
					if (_logic.is_pengpeng_hu(analyseItem)) { // 碰碰胡
						is_peng_peng_hu = true;
					}
				}
			}

			// 碰牌或者吃牌之后，不能接炮，只能自摸；打王牌之后弃胡；注意碰碰胡是可以接炮的
			if (_playerStatus[i].is_chi_hu_round() && !playerStatus.isAbandoned()) {
				if (GRR._weave_count[i] == 0 || is_peng_peng_hu) {
					// 吃胡判断
					ChiHuRight chr = GRR._chi_hu_rights[i];
					chr.set_empty();
					int cbWeaveCount = GRR._weave_count[i];
					action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr, GameConstants.HU_CARD_TYPE_PAOHU,
							i);

					// 结果判断
					if (action != 0) {
						_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
						_playerStatus[i].add_chi_hu(card, seat_index); // 吃胡的组合
						bAroseAction = true;
					}
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

	public void exe_gang_xuan_mei(int seat_index) {
		this.set_handler(_handler_gang_xuan_mei);
		_handler_gang_xuan_mei.reset_status(seat_index);
		_handler_gang_xuan_mei.exe(this);
	}

	public void exe_qishou_hun(int seat_index) {
		this.set_handler(_handler_qishou_hun);
		_handler_qishou_hun.reset_status(seat_index);
		_handler_qishou_hun.exe(this);
	}

	public void exe_select_magic_card(int seat_index) {
		this.set_handler(_handler_select_magic_card);
		_handler_select_magic_card.reset_status(seat_index);
		_handler_select_magic_card.exe(this);
	}

	public int get_xuan_mei_count() {
		int m_count = 0;

		if (this.has_rule(MJConstants_HuNan_XiangTan.GAME_RULE_HUNAN_XUAN_MEI_2)) {
			m_count = MJConstants_HuNan_XiangTan.COUNT_OF_MEI_2;
		} else if (this.has_rule(MJConstants_HuNan_XiangTan.GAME_RULE_HUNAN_XUAN_MEI_3)) {
			m_count = MJConstants_HuNan_XiangTan.COUNT_OF_MEI_3;
		} else if (this.has_rule(MJConstants_HuNan_XiangTan.GAME_RULE_HUNAN_XUAN_MEI_4)) {
			m_count = MJConstants_HuNan_XiangTan.COUNT_OF_MEI_4;
		}

		if (m_count > GRR._left_card_count) {
			m_count = GRR._left_card_count;
		}

		return m_count;
	}

	public int get_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		// 复制数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int max_ting_count = GameConstants.MAX_ZI;

		for (int i = 0; i < max_ting_count; i++) {
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
			// 有听牌，而且鬼牌不用判别
		} else {
			// 全听
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
		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			chr.set_empty();
			int cbCurrentCard = _logic.switch_to_card_data(i);
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					MJConstants_HuNan_XiangTan.HU_CARD_TYPE_ZI_MO, seat_index))
				return true;
		}
		return false;
	}

	@Override
	protected void onInitTable() {
		_handler_chi_peng = new MJHandlerChiPeng_HuNan_XiangTan();
		_handler_dispath_card = new MJHandlerDispatchCard_HuNan_XiangTan();
		_handler_gang = new MJHandlerGang_HuNan_XiangTan();
		_handler_out_card_operate = new MJHandlerOutCardOperate_HuNan_XiangTan();

		_handler_qishou_hun = new MJHandlerQiShouHun_HuNan_XiangTan();
		_handler_select_magic_card = new MJHandlerSelectMagicCard_HuNan_XiangTan();
		_handler_gang_xuan_mei = new MJHandlerGangXuanMei_HuNan_XiangTan();
	}

	@Override
	protected boolean on_game_start() {
		_game_status = GameConstants.GS_MJ_PLAY; // 设置状态

		_logic.clean_magic_cards();

		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[this.getTablePlayerNumber()][GameConstants.MAX_COUNT];

		// 发送数据
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		// 发送数据
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
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

		// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		this.load_room_info_data(roomResponse);
		this.load_common_status(roomResponse);
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}
		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(GRR._left_card_count);
		GRR.add_room_response(roomResponse);

		this.exe_select_magic_card(GRR._banker_player);

		boolean has_qishou_hu = false;

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < this._logic.get_magic_card_count(); j++) {
				// 起手4个一样的混
				if (GRR._cards_index[i][_logic.get_magic_card_index(j)] == 4) {

					_playerStatus[i].add_action(GameConstants.WIK_ZI_MO);
					_playerStatus[i].add_zi_mo(_logic.switch_to_card_data(_logic.get_magic_card_index(0)), i);
					GRR._chi_hu_rights[i].opr_or(MJConstants_HuNan_XiangTan.CHR_ZI_MO);
					GRR._chi_hu_rights[i].opr_or(MJConstants_HuNan_XiangTan.CHR_QI_SHOU_HU);

					has_qishou_hu = true;
					this.exe_qishou_hun(i);

					MongoDBServiceImpl.getInstance().card_log(this.get_players()[i], ECardType.xt_qishou_sihun, "", 0, 0l, this.getRoom_id());
				}
			}
		}

		if (!has_qishou_hu) {
			this.exe_dispatch_card(_current_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);
		}

		return true;
	}

	@Override
	public void process_chi_hu_player_operate(int seat_index, int operate_card, boolean rm) {
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
			// 把摸的牌从手牌删掉，结算的时候不显示这张牌的
			GRR._cards_index[seat_index][_logic.switch_to_card_index(operate_card)]--;
		}

		// 显示胡牌
		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], cards);

		for (int i = 0; i < hand_card_count; i++) {
			if (_logic.is_magic_card(cards[i])) {
				cards[i] += GameConstants.CARD_ESPECIAL_TYPE_GUI;
			}
		}

		cards[hand_card_count] = operate_card + GameConstants.CARD_ESPECIAL_TYPE_HU;
		hand_card_count++;

		this.operate_show_card(seat_index, GameConstants.Show_Card_HU, hand_card_count, cards, GameConstants.INVALID_SEAT);

		return;
	}

	@Override
	public void runnable_remove_middle_cards(int seat_index) {
		if ((_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) && is_sys())
			return;

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
			if (has_lai_zi) {
				// 刷新自己手牌
				int cards[] = new int[GameConstants.MAX_COUNT];
				int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], cards);
				for (int j = 0; j < hand_card_count; j++) {
					if (_logic.is_magic_card(cards[j])) {
						cards[j] += GameConstants.CARD_ESPECIAL_TYPE_GUI;
					}
				}
				this.operate_player_cards(i, hand_card_count, cards, 0, null);
			}
		}

		// 重载的方法里，不直接发牌，交给起手混去处理
		// this.exe_dispatch_card(seat_index, GameConstants.WIK_NULL, 0);
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {

	}

	/**
	 * 根据胡牌的牌类型和胡牌的牌型种类来进行叠加算分
	 * 
	 * @param seat_index
	 * @param provide_index
	 * @param operate_card
	 * @param hu_card_type
	 */
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, int hu_card_type, boolean is_zi_mo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;

		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int wFanShu = 1;

		countCardType(chr, seat_index);

		wFanShu = this.get_fan_shu(hu_card_type, chr); // 根据牌型和吃胡类型获取番数

		int lChiHuScore = wFanShu * GameConstants.CELL_SCORE;

		// 算基础分
		if (is_zi_mo) { // 自摸
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (i == seat_index)
					continue;

				GRR._lost_fan_shu[i][seat_index] = wFanShu;
			}
		} else { // 点炮
			GRR._lost_fan_shu[provide_index][seat_index] = wFanShu;
		}

		// 算分
		if (is_zi_mo) { // 自摸
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (i == seat_index)
					continue;

				int s = lChiHuScore;

				s += GRR._count_pick_niao;

				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;
			}
		} else { // 点炮
			int s = lChiHuScore;

			s += GRR._count_pick_niao;

			GRR._game_score[provide_index] -= s;
			GRR._game_score[seat_index] += s;

			GRR._chi_hu_rights[provide_index].opr_or(MJConstants_HuNan_XiangTan.CHR_FANG_PAO); // 放炮
		}

		GRR._provider[seat_index] = provide_index;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);

		return;
	}

	@Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x03, 0x04, 0x05, 0x12, 0x13, 0x14, 0x16, 0x17, 0x18, 0x08, 0x08, 0x21, 0x25 };
		int[] cards_of_player1 = new int[] { 0x26, 0x26, 0x27, 0x01, 0x02, 0x03, 0x07, 0x08, 0x09, 0x11, 0x11, 0x13, 0x14 };
		int[] cards_of_player2 = new int[] { 0x03, 0x04, 0x05, 0x15, 0x16, 0x17, 0x17, 0x18, 0x19, 0x01, 0x01, 0x25, 0x27 };
		int[] cards_of_player3 = new int[] { 0x01, 0x02, 0x03, 0x05, 0x06, 0x07, 0x09, 0x09, 0x23, 0x24, 0x25, 0x27, 0x28 };

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
	protected void set_result_describe() {
		int chrTypes;
		long type = 0;
		for (int player = 0; player < this.getTablePlayerNumber(); player++) {
			StringBuilder gameDesc = new StringBuilder("");

			chrTypes = GRR._chi_hu_rights[player].type_count;

			if (GRR._chi_hu_rights[player].is_valid()) {
				for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
					type = GRR._chi_hu_rights[player].type_list[typeIndex];

					if (type == MJConstants_HuNan_XiangTan.CHR_ZI_MO) {
						gameDesc.append(" 自摸");
					}
					if (type == MJConstants_HuNan_XiangTan.CHR_JIE_PAO) {
						gameDesc.append(" 接炮");
					}
					if (type == MJConstants_HuNan_XiangTan.CHR_QI_SHOU_HU) {
						gameDesc.append(" 自摸 起手胡");
					}
					if (type == MJConstants_HuNan_XiangTan.CHR_PING_HU) {
						gameDesc.append(" 平胡");
					}

					if (type == MJConstants_HuNan_XiangTan.CHR_MEN_QING) {
						gameDesc.append(" 门清");
					}
					if (type == MJConstants_HuNan_XiangTan.CHR_PENG_PENG_HU) {
						gameDesc.append(" 碰碰胡");
					}
					if (type == MJConstants_HuNan_XiangTan.CHR_QI_XIAO_DUI) {
						gameDesc.append(" 七小对");
					}
					if (type == MJConstants_HuNan_XiangTan.CHR_QING_YI_SE) {
						gameDesc.append(" 清一色");
					}

					if (type == MJConstants_HuNan_XiangTan.CHR_GANG_KAI) {
						gameDesc.append(" 杠上开花");
					}
					if (type == MJConstants_HuNan_XiangTan.CHR_QIANG_GANG_HU) {
						gameDesc.append(" 抢杠胡");
					}
				}

				if (GRR._count_pick_niao > 0) {
					gameDesc.append(" 中鸟X" + GRR._count_pick_niao);
				}
			} else if (!GRR._chi_hu_rights[player].opr_and(MJConstants_HuNan_XiangTan.CHR_FANG_PAO).is_empty()) {
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

	public void set_niao_card(int seat_index) {
		for (int i = 0; i < GameConstants.MAX_NIAO_CARD; i++) {
			GRR._cards_data_niao[i] = GameConstants.INVALID_VALUE;
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			GRR._player_niao_count[i] = 0;
			for (int j = 0; j < GameConstants.MAX_NIAO_CARD; j++) {
				GRR._player_niao_cards[i][j] = GameConstants.INVALID_VALUE;
			}
		}

		GRR._show_bird_effect = true;

		GRR._count_niao = this.get_niao_card_num();

		if (GRR._count_niao > MJConstants_HuNan_XiangTan.COUNT_OF_NIAO_0) {
			int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
			// 从剩余牌堆里顺序取奖码数目的牌
			_logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count, GRR._count_niao, cbCardIndexTemp);
			_logic.switch_to_cards_data(cbCardIndexTemp, GRR._cards_data_niao);
			GRR._left_card_count -= GRR._count_niao;
		}

		for (int i = 0; i < GRR._count_niao; i++) {
			if (_logic.is_magic_card(GRR._cards_data_niao[i])) { // 如果是鬼牌
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
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (i == seat_index) {
				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					GRR._player_niao_cards[i][j] = this.set_ding_niao_valid(GRR._player_niao_cards[i][j], true);
				}
			} else {
				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					GRR._player_niao_cards[i][j] = this.set_ding_niao_valid(GRR._player_niao_cards[i][j], false);
				}
			}
		}

		// 中鸟个数
		GRR._count_pick_niao = this.get_pick_niao_count(GRR._cards_data_niao, GRR._count_niao);
	}

	private int get_niao_card_num() {
		int nNum = MJConstants_HuNan_XiangTan.COUNT_OF_NIAO_0;

		if (has_rule(MJConstants_HuNan_XiangTan.GAME_RULE_HUNAN_ZHUA_NIAO_2)) { // 奖2码
			nNum = MJConstants_HuNan_XiangTan.COUNT_OF_NIAO_2;
		} else if (has_rule(MJConstants_HuNan_XiangTan.GAME_RULE_HUNAN_ZHUA_NIAO_4)) { // 奖4码
			nNum = MJConstants_HuNan_XiangTan.COUNT_OF_NIAO_4;
		} else if (has_rule(MJConstants_HuNan_XiangTan.GAME_RULE_HUNAN_ZHUA_NIAO_6)) { // 奖6码
			nNum = MJConstants_HuNan_XiangTan.COUNT_OF_NIAO_6;
		}

		if (nNum > GRR._left_card_count) {
			nNum = GRR._left_card_count;
		}

		return nNum;
	}

	private int get_pick_niao_count(int cards_data[], int card_num) {
		int cbPickNum = 0;

		for (int i = 0; i < card_num; i++) {
			if (!_logic.is_valid_card(cards_data[i])) // 如果有非法的牌
				return 0;

			if (_logic.is_magic_card(cards_data[i])) { // 鬼牌算中鸟
				cbPickNum++;
				continue;
			}

			int nValue = _logic.get_card_value(cards_data[i]);

			if (nValue == 1 || nValue == 5 || nValue == 9) {
				cbPickNum++;
			}
		}

		return cbPickNum;
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		return false;
	}

	// 获取庄家上家的座位
	@Override
	protected int get_banker_pre_seat(int banker_seat) {
		int count = 0;
		int seat = banker_seat;
		do {
			count++;
			seat = (4 + seat - 1) % 4;
		} while (get_players()[seat] == null && count <= 5);
		return seat;
	}

	// 获取庄家下家的座位
	@Override
	protected int get_banker_next_seat(int banker_seat) {
		int count = 0;
		int seat = banker_seat;
		do {
			count++;
			seat = (seat + 1) % 4;
		} while (get_players()[seat] == null && count <= 5);
		return seat;
	}

	@Override
	protected void countCardType(ChiHuRight chiHuRight, int seat_index) {
		try {
			if (!(chiHuRight.opr_and(MJConstants_HuNan_XiangTan.CHR_QI_SHOU_HU)).is_empty()) { // 起手胡
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.xt_qishouhu, "", _game_type_index, 0l,
						this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(MJConstants_HuNan_XiangTan.CHR_MEN_QING)).is_empty()) { // 门清
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.xt_menqing, "", _game_type_index, 0l,
						this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(MJConstants_HuNan_XiangTan.CHR_PENG_PENG_HU)).is_empty()) { // 碰碰胡
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.xt_pengpenghu, "", _game_type_index, 0l,
						this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(MJConstants_HuNan_XiangTan.CHR_QING_YI_SE)).is_empty()) { // 清一色
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.xt_qingyise, "", _game_type_index, 0l,
						this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(MJConstants_HuNan_XiangTan.CHR_QI_XIAO_DUI)).is_empty()) { // 七小对
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.xt_qixiaodui, "", _game_type_index, 0l,
						this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(MJConstants_HuNan_XiangTan.CHR_GANG_KAI)).is_empty()) { // 选美胡自摸（杠上开花）
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.xt_gangshanghua, "", _game_type_index, 0l,
						this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(MJConstants_HuNan_XiangTan.CHR_QIANG_GANG_HU)).is_empty()) { // 选美胡接炮（抢杠胡）
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.xt_qiangganghu, "", _game_type_index, 0l,
						this.getRoom_id());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private int get_fan_shu(int hu_card_type, ChiHuRight chr) {
		int wFanShu = 1;

		if (hu_card_type == MJConstants_HuNan_XiangTan.HU_CARD_TYPE_QI_SHOU_HU) { // 起手胡
			wFanShu = 3;
		} else if (hu_card_type == MJConstants_HuNan_XiangTan.HU_CARD_TYPE_ZI_MO || hu_card_type == MJConstants_HuNan_XiangTan.HU_CARD_TYPE_JIE_PAO) { // 自摸胡和点炮胡
			wFanShu = 1;

			if (!(chr.opr_and(MJConstants_HuNan_XiangTan.CHR_MEN_QING)).is_empty()) { // 门清
				wFanShu = 2;
			}

			if (!(chr.opr_and(MJConstants_HuNan_XiangTan.CHR_PENG_PENG_HU)).is_empty()) { // 碰碰胡
				wFanShu = 3;
			}
			if (!(chr.opr_and(MJConstants_HuNan_XiangTan.CHR_QING_YI_SE)).is_empty()) { // 清一色
				wFanShu = 3;
			}
			if (!(chr.opr_and(MJConstants_HuNan_XiangTan.CHR_QI_XIAO_DUI)).is_empty()) { // 七小对
				wFanShu = 3;
			}

			if (!(chr.opr_and(MJConstants_HuNan_XiangTan.CHR_QING_YI_SE)).is_empty()
					&& !(chr.opr_and(MJConstants_HuNan_XiangTan.CHR_PENG_PENG_HU)).is_empty()) { // 清一色碰碰胡
				wFanShu = 6;
			}
			if (!(chr.opr_and(MJConstants_HuNan_XiangTan.CHR_QING_YI_SE)).is_empty()
					&& !(chr.opr_and(MJConstants_HuNan_XiangTan.CHR_QI_XIAO_DUI)).is_empty()) { // 清一色七小对
				wFanShu = 6;
			}
			if (!(chr.opr_and(MJConstants_HuNan_XiangTan.CHR_MEN_QING)).is_empty()
					&& !(chr.opr_and(MJConstants_HuNan_XiangTan.CHR_QING_YI_SE)).is_empty()) { // 门清清一色
				wFanShu = 6;
			}
			if (!(chr.opr_and(MJConstants_HuNan_XiangTan.CHR_MEN_QING)).is_empty()
					&& !(chr.opr_and(MJConstants_HuNan_XiangTan.CHR_PENG_PENG_HU)).is_empty()) { // 门清喷喷胡
				wFanShu = 6;
			}

			if (!(chr.opr_and(MJConstants_HuNan_XiangTan.CHR_MEN_QING)).is_empty()
					&& !(chr.opr_and(MJConstants_HuNan_XiangTan.CHR_QING_YI_SE)).is_empty()
					&& !(chr.opr_and(MJConstants_HuNan_XiangTan.CHR_PENG_PENG_HU)).is_empty()) { // 门清清一色碰碰胡
				wFanShu = 12;
			}
		} else if (hu_card_type == MJConstants_HuNan_XiangTan.HU_CARD_TYPE_GANG_KAI
				|| hu_card_type == MJConstants_HuNan_XiangTan.HU_CARD_TYPE_QIANG_GANG_HU) { // 选美自摸胡（杠上开花）和选美点炮胡（抢杠胡）
			wFanShu = 3;

			if (!(chr.opr_and(MJConstants_HuNan_XiangTan.CHR_MEN_QING)).is_empty()) { // 门清
				wFanShu = 6;
			}

			if (!(chr.opr_and(MJConstants_HuNan_XiangTan.CHR_PENG_PENG_HU)).is_empty()) { // 碰碰胡
				wFanShu = 6;
			}
			if (!(chr.opr_and(MJConstants_HuNan_XiangTan.CHR_QING_YI_SE)).is_empty()) { // 清一色
				wFanShu = 6;
			}
			if (!(chr.opr_and(MJConstants_HuNan_XiangTan.CHR_QI_XIAO_DUI)).is_empty()) { // 七小对
				wFanShu = 6;
			}

			if (!(chr.opr_and(MJConstants_HuNan_XiangTan.CHR_QING_YI_SE)).is_empty()
					&& !(chr.opr_and(MJConstants_HuNan_XiangTan.CHR_PENG_PENG_HU)).is_empty()) { // 清一色碰碰胡
				wFanShu = 12;
			}
			if (!(chr.opr_and(MJConstants_HuNan_XiangTan.CHR_QING_YI_SE)).is_empty()
					&& !(chr.opr_and(MJConstants_HuNan_XiangTan.CHR_QI_XIAO_DUI)).is_empty()) { // 清一色七小对
				wFanShu = 12;
			}
			if (!(chr.opr_and(MJConstants_HuNan_XiangTan.CHR_MEN_QING)).is_empty()
					&& !(chr.opr_and(MJConstants_HuNan_XiangTan.CHR_QING_YI_SE)).is_empty()) { // 门清清一色
				wFanShu = 12;
			}
			if (!(chr.opr_and(MJConstants_HuNan_XiangTan.CHR_MEN_QING)).is_empty()
					&& !(chr.opr_and(MJConstants_HuNan_XiangTan.CHR_PENG_PENG_HU)).is_empty()) { // 门清喷喷胡
				wFanShu = 12;
			}

			if (!(chr.opr_and(MJConstants_HuNan_XiangTan.CHR_MEN_QING)).is_empty()
					&& !(chr.opr_and(MJConstants_HuNan_XiangTan.CHR_QING_YI_SE)).is_empty()
					&& !(chr.opr_and(MJConstants_HuNan_XiangTan.CHR_PENG_PENG_HU)).is_empty()) { // 门清清一色碰碰胡
				wFanShu = 18;
			}
		}

		return wFanShu;
	}
}
