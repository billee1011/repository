package com.cai.game.mj.yu.lu_he;

import java.util.Arrays;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.GameConstants_GY;
import com.cai.common.constant.game.GameConstants_HanShouWang;
import com.cai.common.constant.game.mj.GameConstants_LUHE;
import com.cai.common.constant.game.mj.GameConstants_LUHE.LuHeHuTypeEnum;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.PBUtil;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.LuHeReadyRunnable;
import com.cai.future.runnable.TuoGuanRunnable;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJType;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;
import protobuf.clazz.shangQiu.ShangQiuRsp.OtherResponse;

public class Table_LUHE extends AbstractMJTable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public int bu_hua_count;

	public int bao_si_peng;
	public int bao_si_gang;

	private boolean[] can_bao_si; // 能包四碰
	private int[] can_bao_si_count; // 能包四碰

	public void canBaoSiVaild(int seat_index) {
		can_bao_si[seat_index] = true;
	}

	public void canBaoSiInVaild(int seat_index) {
		can_bao_si[seat_index] = false;
	}

	public boolean isCanBaoSi(int seat_index) {
		return can_bao_si[seat_index];
	}

	public void changeBaoSi(int seat_index) {
		if (can_bao_si[seat_index]) {
			can_bao_si_count[seat_index]++;
		}
		if (can_bao_si_count[seat_index] > 1) {
			canBaoSiInVaild(seat_index);
		}

	}

	public Table_LUHE() {
		super(MJType.GAME_TYPE_LU_HE);
	}

	@Override
	protected void onInitTable() {
		_handler_chi_peng = new HandlerChiPeng_LUHE();
		_handler_dispath_card = new MJHandlerDispatchCard_LUHE();
		_handler_gang = new HandlerGang_LUHE();
		_handler_out_card_operate = new HandlerOutCardOperate_LUHE();
	}

	@Override
	public int getTablePlayerNumber() {
		if (has_rule(GameConstants_LUHE.GAME_RULE_PLAYER_2))
			return 2;
		if (has_rule(GameConstants_LUHE.GAME_RULE_PLAYER_3))
			return 3;
		return 4;
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
			Table_LUHE mjTable_LUHE = this;
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

	@Override
	protected boolean on_game_start() {
		GRR.init_param_sq(getTablePlayerNumber());
		bu_hua_count = 0;

		can_bao_si = new boolean[] { false, false, false, false };

		can_bao_si_count = new int[] { 0, 0, 0, 0 };
		bao_si_peng = GameConstants.INVALID_SEAT;
		bao_si_gang = GameConstants.INVALID_SEAT;

		_game_status = GameConstants.GS_MJ_PLAY;// 设置状态
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

			if (_cur_round == 1) {
				// shuffle_players();
				load_player_info_data(roomResponse);
			}

			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse
					.setCurrentPlayer(_current_player == GameConstants.INVALID_SEAT ? _resume_player : _current_player);
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

		// 检测听牌
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_playerStatus[i]._hu_card_count = this.get_ting_card(_playerStatus[i]._hu_cards, GRR._cards_index[i],
					GRR._weave_items[i], GRR._weave_count[i], i);
			if (_playerStatus[i]._hu_card_count > 0) {
				this.operate_chi_hu_cards(i, _playerStatus[i]._hu_card_count, _playerStatus[i]._hu_cards);
			}
		}

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

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard,
					chr, GameConstants.HU_CARD_TYPE_ZIMO, seat_index)) {
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

	@Override
	protected void init_shuffle() {
		_logic.clean_hua_index();
		int[] card = GameConstants_LUHE.CARD_DATA_HUA;
		for (int i : card) {
			_logic.add_hua_card_index(_logic.switch_to_card_index(i));
		}
		super.init_shuffle();
	}

	/**
	 * 刷新花牌和亮牌
	 */
	public boolean operate_show_card_other(int seat_index, int type) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);

		roomResponse.setType(MsgConstants.RESPONSE_OTHER_CARD);
		roomResponse.setTarget(seat_index);

		OtherResponse.Builder otherBuilder = OtherResponse.newBuilder();
		// 亮牌
		if (type == 1 || type == 3) {
			Int32ArrayResponse.Builder liang_card = Int32ArrayResponse.newBuilder();
			for (int i = 0; i < GRR.get_liang_card_count_show(seat_index); i++) {
				liang_card.addItem(GRR.get_player_liang_card_show(seat_index, i));
			}
			otherBuilder.setLiangZhang(liang_card);
		}

		// 花牌
		if (type == 2 || type == 3) {
			Int32ArrayResponse.Builder hua_cards = Int32ArrayResponse.newBuilder();
			for (int i = 0; i < GRR._hua_pai_card[seat_index].length; i++) {
				if (GRR._hua_pai_card[seat_index][i] == 0) {
					continue;
				}
				hua_cards.addItem(GRR._hua_pai_card[seat_index][i]);
			}
			otherBuilder.setHuaCard(hua_cards);
		}

		roomResponse.setCommResponse(PBUtil.toByteString(otherBuilder));

		GRR.add_room_response(roomResponse);
		return this.send_response_to_room(roomResponse);
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
			WeaveItem weaveitems[], int send_card) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS);// 12更新玩家手牌数据
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(1);

		this.load_common_status(roomResponse);

		// 手牌数量
		roomResponse.setCardCount(card_count);
		roomResponse.setWeaveCount(weave_count);

		this.builderWeaveItemResponse(weave_count, weaveitems, roomResponse);
		if (weave_count > 0) {
			this.builderWeaveItemResponse(weave_count, weaveitems, roomResponse);
		}

		this.send_response_to_other(seat_index, roomResponse);

		int index_card[] = new int[] { GameConstants.INVALID_CARD, GameConstants.INVALID_CARD,
				GameConstants.INVALID_CARD, GameConstants.INVALID_CARD };
		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			// 特殊牌值处理
			int real_card = cards[i];
			for (int k = 0; k < GRR.get_liang_card_count(seat_index); k++) {
				if (index_card[k] == GameConstants.INVALID_CARD
						&& real_card == GRR.get_player_liang_card(seat_index, k)) {
					real_card += GameConstants.CARD_ESPECIAL_TYPE_LIANG_ZHANG;
					index_card[k] = GRR.get_player_liang_card(seat_index, k);
				}
			}
			roomResponse.addCardData(real_card);
		}

		GRR.add_room_response(roomResponse);
		// 自己才有牌数据
		this.send_response_to_player(seat_index, roomResponse);

		return true;
	}

	public boolean estimate_player_out_card_respond(int seat_index, int card, int type) {
		boolean bAroseAction = false;

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (seat_index == i) {
				continue;
			}
			if (_playerStatus[i].is_bao_ting())
				continue;

			playerStatus = _playerStatus[i];

			boolean can_peng = true;
			int[] tmp_cards_data = _playerStatus[i].get_cards_abandoned_peng();
			for (int x = 0; x < GameConstants_LUHE.MAX_ABANDONED_CARDS_COUNT; x++) {
				if (tmp_cards_data[x] == card) {
					can_peng = false;
					break;
				}
			}
			if (can_peng && GRR._left_card_count > 0) {
				action = _logic.check_peng(GRR._cards_index[i], card);
				int cards[] = new int[GameConstants.MAX_COUNT];
				int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], cards);
				if (action != 0) {
					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);
					bAroseAction = true;
				}

				if (GRR._left_card_count > 0) {
					action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
					if (action != 0) {
						playerStatus.add_action(GameConstants_LUHE.WIK_GANG);
						playerStatus.add_gang(card, seat_index, 1); // 加上杠
						bAroseAction = true;
					}
				}
			}

			if (_playerStatus[i].is_chi_hu_round()) {
				boolean can_hu_this_card = true;
				int[] tmp_cards_data_hu = _playerStatus[i].get_cards_abandoned_hu();
				for (int x = 0; x < GameConstants_LUHE.MAX_ABANDONED_CARDS_COUNT; x++) {
					if (tmp_cards_data_hu[x] == card) {
						can_hu_this_card = false;
						break;
					}
				}
				if (can_hu_this_card) {
					ChiHuRight chr = GRR._chi_hu_rights[i];
					chr.set_empty();
					int cbWeaveCount = GRR._weave_count[i];

					int card_type = GameConstants_LUHE.HU_CARD_TYPE_JIE_PAO;
					action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
							card_type, i);

					if (action != 0) { // 5倍起可吃胡
						_playerStatus[i].add_action(GameConstants_LUHE.WIK_CHI_HU);
						_playerStatus[i].add_chi_hu(card, seat_index);
						bAroseAction = true;
					} else {
						chr.set_empty();
					}
				}
			}
		}

		int chi_seat_index = (seat_index + 1 + getTablePlayerNumber()) % getTablePlayerNumber();
		if (has_rule(GameConstants_LUHE.GAME_RULE_PENG_CHI)) {
			// 这里可能有问题 应该是 |=
			action = _logic.check_chi(GRR._cards_index[chi_seat_index], card);
			if ((action & GameConstants.WIK_LEFT) != 0) {
				_playerStatus[chi_seat_index].add_action(GameConstants.WIK_LEFT);
				_playerStatus[chi_seat_index].add_chi(card, GameConstants.WIK_LEFT, seat_index);
			}
			if ((action & GameConstants.WIK_CENTER) != 0) {
				_playerStatus[chi_seat_index].add_action(GameConstants.WIK_CENTER);
				_playerStatus[chi_seat_index].add_chi(card, GameConstants.WIK_CENTER, seat_index);
			}
			if ((action & GameConstants.WIK_RIGHT) != 0) {
				_playerStatus[chi_seat_index].add_action(GameConstants.WIK_RIGHT);
				_playerStatus[chi_seat_index].add_chi(card, GameConstants.WIK_RIGHT, seat_index);
			}

			// 结果判断
			if (_playerStatus[chi_seat_index].has_action()) {
				bAroseAction = true;
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

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card,
			ChiHuRight chiHuRight, int card_type, int _seat_index) {
		if (card_type == GameConstants_LUHE.HU_CARD_TYPE_ZI_MO) {
			chiHuRight.opr_or(GameConstants_LUHE.CHR_ZI_MO);
		} else if (card_type == GameConstants_LUHE.HU_CARD_TYPE_JIE_PAO) {
			chiHuRight.opr_or(GameConstants_LUHE.CHR_SHU_FAN);
		} else if (card_type == GameConstants_LUHE.HU_CARD_TYPE_GANG_KAI_HUA) {
			chiHuRight.opr_or(GameConstants_LUHE.CHR_ZI_MO);
		}

		//仅自摸
		if ((has_rule(GameConstants_LUHE.GAME_RULE_OLNY_ZI_MO)
				|| has_rule(GameConstants_LUHE.GAME_RULE_ER_BEI_QI_ZI_MO))
				&& card_type == GameConstants_LUHE.HU_CARD_TYPE_JIE_PAO) {
			chiHuRight.set_empty();
			return GameConstants_HanShouWang.WIK_NULL;
		}

		int[] temp_cards_index = Arrays.copyOf(cards_index, cards_index.length);
		temp_cards_index[_logic.switch_to_card_index(cur_card)]++;

		boolean has_bei = false;
		// 十三倍: 1、十三幺
		if (_logic.isShiSanYao(temp_cards_index, weaveItems, weave_count)) {
			has_bei = true;
			chiHuRight.beiMap.put(LuHeHuTypeEnum.SHI_SAN_YAO.getMultiple(), LuHeHuTypeEnum.SHI_SAN_YAO.getHuDesc());
			return GameConstants.WIK_CHI_HU;
		}

		// 七小对
		int qxd = _logic.is_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card);
		// 清一色
		boolean qys = _logic.is_qing_yi_se(cards_index, weaveItems, weave_count, cur_card);
		// 混一色
		boolean is_hun_yi_se = is_hun_yi_se(temp_cards_index, weaveItems, weave_count);
		// 幺九
		boolean is_xuan_jiu = is_xuan_jiu(temp_cards_index, weaveItems, weave_count, true);
		// 门清
		int meng_qing = _logic.is_men_qing(weaveItems, weave_count);
		if (qxd == GameConstants.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI && meng_qing != GameConstants.WIK_NULL) { // 十三倍：双豪华七小对
			has_bei = true;
			chiHuRight.beiMap.put(LuHeHuTypeEnum.SHUANG_HAO_HUA_QIXIAODUI_MENQING.getMultiple(),
					LuHeHuTypeEnum.SHUANG_HAO_HUA_QIXIAODUI_MENQING.getHuDesc());
		} else if (qxd == GameConstants.CHR_HUNAN_QI_XIAO_DUI && is_xuan_jiu) {
			has_bei = true;
			chiHuRight.beiMap.put(LuHeHuTypeEnum.YAO_JIU_QIXIAODUI.getMultiple(),
					LuHeHuTypeEnum.YAO_JIU_QIXIAODUI.getHuDesc());
		} else if (qxd == GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI && qys) { // 十二倍：豪华清一色七小对
			has_bei = true;
			chiHuRight.beiMap.put(LuHeHuTypeEnum.HAO_HUA_QING_YI_SE_QI_XIAO_DUI.getMultiple(),
					LuHeHuTypeEnum.HAO_HUA_QING_YI_SE_QI_XIAO_DUI.getHuDesc());
		} else if (qxd == GameConstants.CHR_HUNAN_QI_XIAO_DUI && qys) { // 十倍：清一色七小对
			has_bei = true;
			chiHuRight.beiMap.put(LuHeHuTypeEnum.QING_YI_SE_QI_XIAO_DUI.getMultiple(),
					LuHeHuTypeEnum.QING_YI_SE_QI_XIAO_DUI.getHuDesc());
		} else if (qxd == GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI && meng_qing != GameConstants.WIK_NULL) { // 八倍：豪华七小对
																												// 门清
			has_bei = true;
			chiHuRight.beiMap.put(LuHeHuTypeEnum.HAO_HUA_QIXIAODUI_MENQING.getMultiple(),
					LuHeHuTypeEnum.HAO_HUA_QIXIAODUI_MENQING.getHuDesc());
		} else if (qxd == GameConstants.CHR_HUNAN_QI_XIAO_DUI && is_hun_yi_se) { // 六倍：混色七小对
			has_bei = true;
			chiHuRight.beiMap.put(LuHeHuTypeEnum.HUN_SE_QI_XIAO_DUI.getMultiple(),
					LuHeHuTypeEnum.HUN_SE_QI_XIAO_DUI.getHuDesc());
		} else if (qxd == GameConstants.CHR_HUNAN_QI_XIAO_DUI && card_type != GameConstants_LUHE.HU_CARD_TYPE_JIE_PAO) { // 四倍：七小对
			has_bei = true;
			chiHuRight.beiMap.put(LuHeHuTypeEnum.QIXIAODUI.getMultiple(), LuHeHuTypeEnum.QIXIAODUI.getHuDesc());
		}

		boolean bValue = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
				_logic.get_all_magic_card_index(), _logic.get_magic_card_count());

		if (!bValue) {
			if (has_bei) {
				return GameConstants.WIK_CHI_HU;
			}
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		// 碰碰胡
		boolean is_peng_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index(cards_index,
				_logic.switch_to_card_index(cur_card), _logic.get_all_magic_card_index(),
				_logic.get_magic_card_count());

		if (is_kuan_kuan_hu(temp_cards_index) && weave_count == 0) {// 十三倍：款款胡
			has_bei = true;
			if (card_type == GameConstants_LUHE.HU_CARD_TYPE_JIE_PAO) {
				chiHuRight.beiMap.put(LuHeHuTypeEnum.DUI_DUI_HU.getMultiple(), LuHeHuTypeEnum.DUI_DUI_HU.getHuDesc());
			} else {
				chiHuRight.beiMap.put(LuHeHuTypeEnum.KUANG_KUANG_HU.getMultiple(),
						LuHeHuTypeEnum.KUANG_KUANG_HU.getHuDesc());
			}

		}

		if (is_yi_zi_se(temp_cards_index, weaveItems, weave_count)) {// 十三倍：一色字
			has_bei = true;
			chiHuRight.beiMap.put(LuHeHuTypeEnum.YI_SE_ZI.getMultiple(), LuHeHuTypeEnum.YI_SE_ZI.getHuDesc());
			chiHuRight.can_bao_si_peng = true;
		}

		if (is_chun_xuan_jiu(temp_cards_index, weaveItems, weave_count)) {// 十三倍：纯幺九
			has_bei = true;
			chiHuRight.beiMap.put(LuHeHuTypeEnum.CHUN_YAO_JIU.getMultiple(), LuHeHuTypeEnum.CHUN_YAO_JIU.getHuDesc());
			chiHuRight.can_bao_si_peng = true;
		}
		if (is_shi_ba_luo_han(weaveItems, weave_count)) { // 十三倍：十八罗汉
			has_bei = true;
			chiHuRight.beiMap.put(LuHeHuTypeEnum.SHI_BA_LUO_HAN.getMultiple(),
					LuHeHuTypeEnum.SHI_BA_LUO_HAN.getHuDesc());
			chiHuRight.can_bao_si_gang = true;
		}

		if (is_xuan_jiu && is_hun_yi_se) {// 十一倍：混色幺九
			has_bei = true;
			chiHuRight.beiMap.put(LuHeHuTypeEnum.HUN_SE_YAO_JIU.getMultiple(),
					LuHeHuTypeEnum.HUN_SE_YAO_JIU.getHuDesc());
			chiHuRight.can_bao_si_peng = true;
		}

		if (is_xuan_jiu) {// 九倍：幺九
			has_bei = true;
			chiHuRight.beiMap.put(LuHeHuTypeEnum.YAO_JIU.getMultiple(), LuHeHuTypeEnum.YAO_JIU.getHuDesc());
			chiHuRight.can_bao_si_peng = true;
		}

		if (is_peng_hu) {
			boolean exist_eat = exist_eat(weaveItems, weave_count);
			if (!exist_eat && qys) { // 九倍:清一色对对胡
				has_bei = true;
				chiHuRight.beiMap.put(LuHeHuTypeEnum.QING_YI_SE_DUI_DUI_HU.getMultiple(),
						LuHeHuTypeEnum.QING_YI_SE_DUI_DUI_HU.getHuDesc());
				chiHuRight.can_bao_si_peng = true;
			} else if (!exist_eat && is_hun_yi_se) { // 五倍：混色对对胡
				has_bei = true;
				chiHuRight.beiMap.put(LuHeHuTypeEnum.HUN_SE_DUI_DUI_HU.getMultiple(),
						LuHeHuTypeEnum.HUN_SE_DUI_DUI_HU.getHuDesc());
			} else if (!exist_eat && card_type != GameConstants_LUHE.HU_CARD_TYPE_JIE_PAO) { // 三倍：对对胡
				has_bei = true;
				chiHuRight.beiMap.put(LuHeHuTypeEnum.DUI_DUI_HU.getMultiple(), LuHeHuTypeEnum.DUI_DUI_HU.getHuDesc());
			}
		}

		if (qys) { // 七倍：清一色 纯万、或索、或筒牌组成，可碰
			chiHuRight.beiMap.put(LuHeHuTypeEnum.QING_YI_SE.getMultiple(), LuHeHuTypeEnum.QING_YI_SE.getHuDesc());
			has_bei = true;
			chiHuRight.can_bao_si_peng = true;
		}

		if (is_hun_yi_se && card_type != GameConstants_LUHE.HU_CARD_TYPE_JIE_PAO) { // 二倍：混一色
			has_bei = true;
			chiHuRight.beiMap.put(LuHeHuTypeEnum.HUN_YI_SE.getMultiple(), LuHeHuTypeEnum.HUN_YI_SE.getHuDesc());
		}

		if (!has_bei && card_type != GameConstants_LUHE.HU_CARD_TYPE_JIE_PAO) { // 一倍  ： 鸡胡
			chiHuRight.beiMap.put(LuHeHuTypeEnum.JI_HU.getMultiple(), LuHeHuTypeEnum.JI_HU.getHuDesc());
		}
		
		//二倍起
		if(has_rule(GameConstants_LUHE.GAME_RULE_ER_BEI_QI_ZI_MO) && !has_bei){
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}
		if (!has_bei && has_rule(GameConstants_LUHE.GAME_RULE_ER_BEI_JIE_PAO)) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		return GameConstants_GY.WIK_CHI_HU;
	}

	/**
	 * 
	 * @return
	 */
	public boolean estimate_gang_respond_luhe(int seat_index, int card) {
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
				if (action != 0 && chr.beiMap.get(GameConstants_LUHE.SHI_SAN_YAO_NUMBER) != null && chr.beiMap
						.get(GameConstants_LUHE.SHI_SAN_YAO_NUMBER).equals(LuHeHuTypeEnum.SHI_SAN_YAO.getHuDesc())) {
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

	public int get_max_bei(ChiHuRight chr) {
		for (int i = 13; i > 0; i--) {
			if (chr.beiMap.get(i) != null) {
				return i;
			}
		}
		return 1;
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {

	}

	public void process_chi_hu_player_score_luhe(int seat_index, int provide_index, int operate_card, boolean zimo,
			boolean is_qiang_gang_hu) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int wFanShu = get_max_bei(chr);// 番数
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
		} else {// 点炮
			GRR._lost_fan_shu[provide_index][seat_index] = wFanShu;
		}

		float lChiHuScore = 2 * wFanShu;
		////////////////////////////////////////////////////// 自摸 算分
		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				int real_player = i;
				// 抢杆胡一家包全部
				if (is_qiang_gang_hu) {
					GRR._chi_hu_rights[real_player].opr_or(GameConstants.CHR_FANG_PAO);
					real_player = provide_index;
				}

				real_player = getBaoPeiPlayer(seat_index, chr) == GameConstants.INVALID_SEAT ? real_player
						: getBaoPeiPlayer(seat_index, chr);

				float s = lChiHuScore * (1 + GRR._count_pick_niao);

				GRR._game_score[real_player] -= s;
				GRR._game_score[seat_index] += s;
			}
		}
		////////////////////////////////////////////////////// 点炮 算分
		else {
			int real_player = provide_index;
			if (isCanBaoSi(seat_index)) {
				real_player = getBaoPeiPlayer(seat_index, chr) == GameConstants.INVALID_SEAT ? provide_index
						: getBaoPeiPlayer(seat_index, chr);
			}

			GRR._chi_hu_rights[real_player].opr_or(GameConstants.CHR_FANG_PAO);
			float s = lChiHuScore * (1 + GRR._count_pick_niao);

			// 胡牌分
			GRR._game_score[real_player] -= s;
			GRR._game_score[seat_index] += s;

		}

		// 设置变量
		GRR._provider[seat_index] = provide_index;
		_status_gang = false;
		_status_gang_hou_pao = false;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);
	}

	/**
	 * 判断包赔玩家
	 * 
	 * @param seat_index
	 * @param chiHuRight
	 * @return
	 */
	public int getBaoPeiPlayer(int seat_index, ChiHuRight chr) {

		int real_player = GameConstants.INVALID_SEAT;
		// 包四碰 包四杠
		if (GRR._weave_count[seat_index] == 4) {
			int gang_count = 0;
			for (int j = 0; j < GRR._weave_count[seat_index]; j++) {
				int WIk = GRR._weave_items[seat_index][j].weave_kind;
				if (WIk == GameConstants.WIK_GANG) {
					gang_count++;
				}
			}

			if (gang_count == 4 && chr.can_bao_si_gang) {
				int set_index = GRR._weave_items[seat_index][3].provide_player;
				if (set_index != seat_index) {
					bao_si_gang = set_index;
					real_player = set_index;
				}
			} else if (GRR._weave_items[seat_index][3].weave_kind == GameConstants.WIK_PENG && chr.can_bao_si_peng) {
				int set_index = GRR._weave_items[seat_index][3].provide_player;
				if (set_index != seat_index) {
					bao_si_peng = set_index;
					real_player = set_index;
				}
			}
		}

		return real_player;
	}

	/**
	 * 混一色判断
	 * 
	 * @return
	 */
	public boolean is_hun_yi_se(int cards_index[], WeaveItem weaveItem[], int weaveCount) {
		if (_logic.get_se_count(cards_index, weaveItem, weaveCount) > 1) {
			return false;
		}

		if (_logic.has_feng_pai(cards_index, weaveItem, weaveCount) == false) {
			return false;
		}
		return true;
	}

	/**
	 * 玄九判断
	 * 
	 * @return
	 */
	public boolean is_xuan_jiu(int cards_index[], WeaveItem weaveItem[], int weaveCount, boolean hasFeng) {

		// 组合中心牌(因为没有吃)
		for (int i = 0; i < weaveCount; i++) {
			if ((weaveItem[i].weave_kind == GameConstants.WIK_PENG || weaveItem[i].weave_kind == GameConstants.WIK_GANG)
					&& !is_shi_san_yao_index(_logic.switch_to_card_index(weaveItem[i].center_card), hasFeng)) {
				return false;
			}
		}

		// 一九判断
		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			// 是13幺的牌继续
			if (is_shi_san_yao_index(i, hasFeng)) {
				continue;
			}
			// 无效判断
			if (cards_index[i] > 0) {
				return false;
			}
		}
		return true;
	}

	public boolean is_shi_san_yao_index(int index, boolean hasFeng) {
		// 一九判断
		for (int i = 0; i < 27; i += 9) {
			if (index == i || index == (i + 8)) {
				return true;
			}
		}
		if (hasFeng && index >= GameConstants.MAX_ZI && index < GameConstants.MAX_ZI_FENG) {
			return true;
		}
		return false;
	}

	/**
	 * 纯玄九判断
	 * 
	 * @return
	 */
	public boolean is_chun_xuan_jiu(int cards_index[], WeaveItem weaveItem[], int weaveCount) {
		boolean is_xuan_jiu = is_xuan_jiu(cards_index, weaveItem, weaveCount, false);
		if (!is_xuan_jiu) {
			return false;
		}

		for (int i = 27; i < GameConstants.MAX_INDEX - GameConstants.CARD_HUA_COUNT; i++) {
			if (cards_index[i] > 0) {
				return false;
			}
		}

		return true;
	}

	/**
	 * 款款胡判断没门清判断
	 * 
	 * @return
	 */
	public boolean is_kuan_kuan_hu(int cards_index[]) {
		boolean flag = false;

		for (int i = 0; i < GameConstants.MAX_INDEX - GameConstants.CARD_HUA_COUNT; i++) {
			if (cards_index[i] == 0) {
				continue;
			}

			if (cards_index[i] != 3) {
				if (cards_index[i] == 2 && !flag) {
					flag = true;
				} else {
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * 一字色判断
	 * 
	 * @return
	 */
	public boolean is_yi_zi_se(int cards_index[], WeaveItem weaveItem[], int weaveCount) {
		for (int i = 0; i < weaveCount; i++) {
			// int index =
			// _logic.switch_to_card_index(weaveItem[i].center_card);
			if (weaveItem[i].center_card > 0x37 || weaveItem[i].center_card < 0x31) {
				return false;
			}
		}

		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			if (cards_index[i] > 0) {
				return false;
			}
		}

		return true;
	}

	/**
	 * 十八罗汉判断
	 * 
	 * @return
	 */
	public boolean is_shi_ba_luo_han(WeaveItem weaveItem[], int weaveCount) {
		if (weaveCount < 4) {
			return false;
		}

		for (int i = 0; i < weaveCount; i++) {
			if (weaveItem[i].weave_kind != GameConstants.WIK_GANG) {
				return false;
			}
		}

		return true;
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

			float lGangScore[] = new float[this.getTablePlayerNumber()];

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {

				if (GRR._end_type != GameConstants.Game_End_NORMAL) { // 荒庄荒杠
					continue;
				}

				for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
					for (int k = 0; k < this.getTablePlayerNumber(); k++) {
						lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
					}
				}

				for (int j = 0; j < GRR._weave_count[i]; j++) {
					if (GRR._weave_items[i][j].weave_kind == GameConstants.WIK_GANG) {
						if (GRR._weave_items[i][j].type == GameConstants.GANG_TYPE_JIE_GANG) {
							int score = GRR._weave_items[i][j].weave_score;

							// 取消马牌包杠分
							/*
							 * if(seat_index == i){ score = score* (1 +
							 * GRR._count_pick_niao); }
							 */
							GRR._game_score[GRR._weave_items[i][j].provide_player] -= score;
							GRR._game_score[i] += score;
						} else {
							for (int k = 0; k < getTablePlayerNumber(); k++) {
								int score = GRR._weave_items[i][j].weave_score;
								if (k == i) {
									continue;
								}
								// 取消马牌包杠分
								/*
								 * if(seat_index == i){ score = score * (1 +
								 * GRR._count_pick_niao); }
								 */

								GRR._game_score[k] -= score;
								GRR._game_score[i] += score;
							}
						}
					}
				}

				for (int j = 0; j < this.getTablePlayerNumber(); j++) {
					_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
				}

			}

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				// GRR._game_score[i] += lGangScore[i];
				GRR._game_score[i] += GRR._start_hu_score[i];

				_player_result.game_score[i] += GRR._game_score[i];
			}

			this.load_player_info_data(roomResponse);

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

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
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
					// 客户端特殊处理的牌值
					/*
					 * for(int k = 0; k <
					 * GRR._weave_items[i][j].client_special_count; k++){
					 * weaveItem_item.addClientSpecialCard(GRR._weave_items[i][j
					 * ].client_special_card[k]); }
					 */
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
		} else if ((!is_sys()) && (reason == GameConstants.Game_End_RELEASE_PLAY
				|| reason == GameConstants.Game_End_RELEASE_NO_BEGIN || reason == GameConstants.Game_End_RELEASE_RESULT
				|| reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_SYSTEM)) {
			end = true;
			real_reason = GameConstants.Game_End_DRAW;
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(this.process_player_result(reason));
		}
		game_end.setEndType(real_reason);

		roomResponse.setGameEnd(game_end);

		this.send_response_to_room(roomResponse);

		record_game_round(game_end);

		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
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
		if (!end) {
			GameSchedule.put(new LuHeReadyRunnable(this.getRoom_id()), 10, TimeUnit.SECONDS);
		}

		return false;
	}

	@Override
	protected void set_result_describe() {
		int chrTypes;
		long type = 0;
		boolean qiang_gang_hu = false;
		for (int player = 0; player < this.getTablePlayerNumber(); player++) {
			StringBuilder result = new StringBuilder("");

			chrTypes = GRR._chi_hu_rights[player].type_count;

			for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
				type = GRR._chi_hu_rights[player].type_list[typeIndex];

				if (GRR._chi_hu_rights[player].is_valid()) {
					if (type == GameConstants_LUHE.CHR_ZI_MO) {
						result.append(" 自摸");
					}
					if (type == GameConstants.CHR_HUNAN_QIANG_GANG_HU) {
						qiang_gang_hu = true;
						result.append(" 抢杠胡");
					}
					if (type == GameConstants.CHR_SHU_FAN) {
						result.append(" 接炮");
					}
					// if (type == GameConstants_LUHE.CHR_GNAG_KAI) {
					// result.append(" 杠开");
					// }

				} else if (type == GameConstants_LUHE.CHR_FANG_PAO) {
					if (qiang_gang_hu) {
						result.append("被抢杠");
					} else {
						result.append(" 放炮");
					}
				}
			}

			if (GRR._chi_hu_rights[player].beiMap.get(get_max_bei(GRR._chi_hu_rights[player])) != null) {
				result.append(" " + GRR._chi_hu_rights[player].beiMap.get(get_max_bei(GRR._chi_hu_rights[player])));
				;
			}

			if (bao_si_gang != GameConstants.INVALID_SEAT && player == bao_si_gang) {
				result.append(" 包四杠");
			}
			if (bao_si_peng != GameConstants.INVALID_SEAT && player == bao_si_peng) {
				result.append(" 包四碰");
			}
			int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0;

			if (GRR != null) {
				for (int tmpPlayer = 0; tmpPlayer < this.getTablePlayerNumber(); tmpPlayer++) {
					for (int w = 0; w < GRR._weave_count[tmpPlayer]; w++) {
						if (GRR._weave_items[tmpPlayer][w].weave_kind != GameConstants.WIK_GANG) {
							continue;
						}
						if (tmpPlayer == player) {
							if (GRR._weave_items[tmpPlayer][w].provide_player != tmpPlayer
									&& !GRR._weave_items[tmpPlayer][w].is_add_gang) {
								jie_gang++;
							} else {
								if (GRR._weave_items[tmpPlayer][w].public_card == 1) {
									ming_gang++;
								} else {
									an_gang++;
								}
							}
						} else {
							if (GRR._weave_items[tmpPlayer][w].provide_player == player
									&& !GRR._weave_items[tmpPlayer][w].is_add_gang) {
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
	 * @param isTongPao
	 *            --通炮不抓飞鸟
	 */
	public void set_niao_card(int seat_index, int card, boolean show, int add_niao) {

		for (int i = 0; i < GameConstants.MAX_NIAO_CARD; i++) {
			GRR._cards_data_niao[i] = GameConstants.INVALID_VALUE;
		}

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			GRR._player_niao_count[i] = 0;
			for (int j = 0; j < GameConstants.MAX_NIAO_CARD; j++) {
				GRR._player_niao_cards[i][j] = GameConstants.INVALID_VALUE;
			}
		}

		GRR._show_bird_effect = show;
		GRR._count_niao = getCsDingNiaoNum();

		if (GRR._count_niao > GameConstants.ZHANIAO_0) {
			if (card == GameConstants.INVALID_VALUE) {
				int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];

				/*
				 * _logic.switch_to_cards_index(_repertory_card, _all_card_len -
				 * GRR._left_card_count, GRR._count_niao, cbCardIndexTemp);
				 */
				// 鸟牌不够取最后几张（留马牌能不能做成把最后的马牌照让给人摸牌，假如定四个马，最后一个牌被抓走而且自摸了，还是算最后的四个马牌）
				int stat_index = GRR._left_card_count;
				if (GRR._count_niao > GRR._left_card_count) {
					stat_index = GRR._count_niao;
				}
				_logic.switch_to_cards_index(_repertory_card, _all_card_len - stat_index, GRR._count_niao,
						cbCardIndexTemp);
				GRR._left_card_count -= GRR._count_niao;
				_logic.switch_to_cards_data(cbCardIndexTemp, GRR._cards_data_niao);
			} else {
				for (int i = 0; i < GRR._count_niao; i++) {
					GRR._cards_data_niao[i] = card;
				}
			}
		}
		// 中鸟个数z
		GRR._count_pick_niao = get_pick_niao_count(GRR._cards_data_niao, GRR._count_niao);

		for (int i = 0; i < GRR._count_niao; i++) {
			int nValue = _logic.get_card_value(GRR._cards_data_niao[i]);
			if (GRR._cards_data_niao[i] == 0x32) {
				nValue = 3;
			} else if (GRR._cards_data_niao[i] == 0x33) {
				nValue = 2;
			}
			int seat = (GRR._banker_player + (nValue - 1) + 4) % 4;
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

			if (cards_data[i] == 0x35)
				continue;

			int nValue = _logic.get_card_value(cards_data[i]);
			if (nValue == 1 || nValue == 5 || nValue == 9) {
				cbPickNum++;
			}

		}
		return cbPickNum;
	}

	public int set_ding_niao_valid(int card_data, boolean val) {
		// 先把值还原
		if (val) {
			if (card_data > GameConstants.DING_NIAO_INVALID && card_data < GameConstants.DING_NIAO_VALID) {
				card_data -= GameConstants.DING_NIAO_INVALID;
			} else if (card_data > GameConstants.DING_NIAO_VALID) {
				card_data -= GameConstants.DING_NIAO_VALID;
			}
		} else {
			if (card_data > GameConstants.DING_NIAO_INVALID) {
				return card_data;
			}
		}

		/*
		 * if (card_data == 0x32) { card_data = 0x33; } else if (card_data ==
		 * 0x33) { card_data = 0x32; }
		 */

		if (val == true) {
			// 生效
			return (card_data < GameConstants.DING_NIAO_VALID ? card_data + GameConstants.DING_NIAO_VALID : card_data);
		} else {
			return (card_data < GameConstants.DING_NIAO_INVALID ? card_data + GameConstants.DING_NIAO_INVALID
					: card_data);
		}
	}

	/**
	 * 抓鸟 个数
	 * 
	 * @return
	 */
	public int getCsDingNiaoNum() {
		int nNum = GameConstants.ZHANIAO_0;
		if (has_rule(GameConstants_LUHE.GAME_RULE_MA_2)) {
			return GameConstants.ZHANIAO_2;
		}
		if (has_rule(GameConstants_LUHE.GAME_RULE_MA_4)) {
			return GameConstants.ZHANIAO_4;
		}
		if (has_rule(GameConstants_LUHE.GAME_RULE_MA_6)) {
			return GameConstants.ZHANIAO_6;
		}
		if (has_rule(GameConstants_LUHE.GAME_RULE_MA_8)) {
			return GameConstants.ZHANIAO_8;
		}

		return nNum;
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x01, 0x01, 0x01, 0x22, 0x22, 0x22, 0x14, 0x14, 0x14, 0x06, 0x06, 0x07,
				0x07 };
		int[] cards_of_player1 = new int[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x22, 0x35, 0x35,
				0x35 };
		int[] cards_of_player2 = new int[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x22, 0x35, 0x35,
				0x35 };
		int[] cards_of_player3 = new int[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x22, 0x35, 0x35,
				0x35 };

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}

		for (int j = 0; j < 13; j++) {
			if (this.getTablePlayerNumber() == 4) {
				GRR._cards_index[0][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[1][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[2][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[3][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
			} else if (this.getTablePlayerNumber() == 3) {
				GRR._cards_index[0][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[1][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[2][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
			} else if (this.getTablePlayerNumber() == 2) {
				GRR._cards_index[0][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[1][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
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
	public void sendIsTruetee(int get_seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_IS_TRUSTEE);
		roomResponse.setOperatePlayer(get_seat_index);
		roomResponse.setIstrustee(istrustee[get_seat_index]);
		this.send_response_to_room(roomResponse);

		if (GRR != null) {
			GRR.add_room_response(roomResponse);
		}
	}

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

		if ((st == GameConstants.Player_Status_OPR_CARD || st == GameConstants.Player_Status_OUT_CARD)) {
			_trustee_schedule[seat_index] = GameSchedule.put(new TuoGuanRunnable(getRoom_id(), seat_index),
					GameConstants.TRUSTEE_TIME_OUT_SECONDS_30, TimeUnit.SECONDS);
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
}
