package com.cai.game.mj.yu.kwx.TwoD;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.GameConstants_KWX;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.PBUtil;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.JianPaoHuRunnable;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJGameLogic.AnalyseItem;
import com.cai.game.mj.MJType;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;
import protobuf.clazz.mj.KwxProto.KWXLiangCard;

public class Table_KWX_2D extends AbstractMJTable {

	private static final long serialVersionUID = -166579336682643575L;

	public HandlerLiangCardOperate_KWX _handler_liang;
	protected MJHandlerPiao_KWX _handler_piao;
	public HandlerXiaoJieSuan_KWX _handler_xiao_jie_suan;

	public RoomResponse.Builder saved_room_response = null;
	public GameEndResponse.Builder saved_game_end_response = null;

	public int[] pass_hu_fan;
	public int[] player_continue_gang;
	public int[] player_liang;

	public int first_liang;

	public int[] pao;
	public int liang_4_type;

	public int[] piao_score;
	public ScheduledFuture future;

	public Table_KWX_2D() {
		super(MJType.GAME_TYPE_MJ_KWX);
	}

	@Override
	protected void onInitTable() {
		_handler_chi_peng = new HandlerChiPeng_KWX();
		_handler_dispath_card = new HandlerDispatchCard_KWX();
		_handler_gang = new HandlerGang_KWX();
		_handler_out_card_operate = new HandlerOutCardOperate_KWX();
		_handler_liang = new HandlerLiangCardOperate_KWX();
		_handler_piao = new MJHandlerPiao_KWX();
		_handler_xiao_jie_suan = new HandlerXiaoJieSuan_KWX();

		pao = new int[getTablePlayerNumber()];
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			pao[i] = -1;
		}
	}

	public int getTablePlayerNumber() {
		if (has_rule(GameConstants_KWX.GAME_RULE_PLAYER_NUM_3)) {
			return 3;
		}
		if (has_rule(GameConstants_KWX.GAME_RULE_PLAYER_NUM_2)) {
			return 2;
		}
		if (playerNumber > 0) {
			return playerNumber;
		}
		return 3;
	}

	public boolean exe_liang(int seat_index, int operate_code, int operate_card, List<Integer> liang_cards, int liang_cards_count) {
		this.set_handler(this._handler_liang);
		_handler_liang.reset_status(seat_index, operate_code, operate_card, liang_cards, liang_cards_count);
		_handler.exe(this);
		return true;
	}

	@Override
	public boolean handler_requst_liang_zhang(int seat_index, int operate_code, int operate_card, List<Integer> linag_cards, int liang_cards_count) {
		return exe_liang(seat_index, operate_code, operate_card, linag_cards, liang_cards_count);
	}

	@Override
	public boolean handler_requst_pao_qiang(Player player, int pao, int qiang) {
		return _handler_piao.handler_pao_qiang(this, player.get_seat_index(), pao, qiang);
	}

	public void clearContinueGang() {
		for (int p = 0; p < getTablePlayerNumber(); p++) {
			player_continue_gang[p] = 0;
		}
	}

	public void progress_banker_select() {
		if (_cur_round == 1) {// 金币场 随机庄家
			Random random = new Random();//
			int rand = random.nextInt(6) + 1 + random.nextInt(6) + 1;
			_cur_banker = rand % getTablePlayerNumber();//

			_shang_zhuang_player = GameConstants.INVALID_SEAT;
			_lian_zhuang_player = GameConstants.INVALID_SEAT;
		}
	}

	@Override
	protected boolean on_game_start() {

		pass_hu_fan = new int[getTablePlayerNumber()];
		player_liang = new int[getTablePlayerNumber()];
		player_continue_gang = new int[getTablePlayerNumber()];
		piao_score = new int[getTablePlayerNumber()];
		first_liang = -1;

		if (has_rule(GameConstants_KWX.GAME_RULE_MEI_JU_PIAO)) {
			this.set_handler(this._handler_piao);
			this._handler_piao.exe(this);
			return true;
		} else if (has_rule(GameConstants_KWX.GAME_RULE_DING_PIAO) && _cur_round == 1) {
			this.set_handler(this._handler_piao);
			this._handler_piao.exe(this);
			return true;
		}
		if (!has_rule(GameConstants_KWX.GAME_RULE_MEI_JU_PIAO) && !has_rule(GameConstants_KWX.GAME_RULE_DING_PIAO)) {
			Arrays.fill(pao, -1);
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_player_result.pao[i] = pao[i]; // 清掉 默认是 -1
			_player_result.ziba[i] = 0;
		}
		operate_player_data();

		return on_game_start_real();
	}

	protected boolean on_game_start_real() {
		_game_status = GameConstants_KWX.GS_MJ_PLAY;

		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants_KWX.MAX_COUNT];

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants_KWX.MAX_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			load_room_info_data(roomResponse);
			load_common_status(roomResponse);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setCurrentPlayer(_current_player == GameConstants_KWX.INVALID_SEAT ? _resume_player : _current_player);
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

			for (int j = 0; j < GameConstants_KWX.MAX_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}
		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(GRR._left_card_count);
		GRR.add_room_response(roomResponse);

		// 检测听牌
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i]._hu_card_count = get_ting_card(_playerStatus[i]._hu_cards, GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i],
					i);
			if (_playerStatus[i]._hu_card_count > 0) {
				operate_chi_hu_cards(i, _playerStatus[i]._hu_card_count, _playerStatus[i]._hu_cards);
			}
		}

		exe_dispatch_card(_current_player, GameConstants_KWX.WIK_NULL, GameConstants_KWX.DELAY_SEND_CARD_DELAY);

		return true;
	}

	@Override
	public boolean operate_player_cards(int seat_index, int card_count, int cards[], int weave_count, WeaveItem weaveitems[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS);// 12更新玩家手牌数据
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
				if (weaveitems[j].weave_kind == GameConstants_KWX.WIK_GANG && weaveitems[j].public_card == 0) {
					weaveItem_item.setCenterCard(weaveitems[j].center_card);

					for (int x = 0; x < 4; x++) {
						weaveItem_item.addWeaveCard(-1);
					}
				} else {
					weaveItem_item.setCenterCard(weaveitems[j].center_card);

					int[] weave_cards = new int[4];
					int count = _logic.get_weave_card_huangshi(weaveitems[j].weave_kind, weaveitems[j].center_card, weave_cards);
					for (int x = 0; x < count; x++) {
						if (_logic.is_magic_card(weave_cards[x]))
							weave_cards[x] += GameConstants_KWX.CARD_ESPECIAL_TYPE_LAI_ZI;

						weaveItem_item.addWeaveCard(weave_cards[x]);
					}
				}

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

				int[] weave_cards = new int[4];
				int count = _logic.get_weave_card_huangshi(weaveitems[j].weave_kind, weaveitems[j].center_card, weave_cards);
				for (int x = 0; x < count; x++) {
					if (_logic.is_magic_card(weave_cards[x]))
						weave_cards[x] += GameConstants_KWX.CARD_ESPECIAL_TYPE_LAI_ZI;

					weaveItem_item.addWeaveCard(weave_cards[x]);
				}

				roomResponse.addWeaveItems(weaveItem_item);
			}
		}
		GRR.add_room_response(roomResponse);
		send_response_to_player(seat_index, roomResponse);

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
				weaveItem_item.setCenterCard(weaveitems[j].center_card);

				int[] weave_cards = new int[4];
				int count = _logic.get_weave_card_huangshi(weaveitems[j].weave_kind, weaveitems[j].center_card, weave_cards);
				for (int x = 0; x < count; x++) {
					if (_logic.is_magic_card(weave_cards[x]))
						weave_cards[x] += GameConstants_KWX.CARD_ESPECIAL_TYPE_LAI_ZI;

					weaveItem_item.addWeaveCard(weave_cards[x]);
				}

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

				int[] weave_cards = new int[4];
				int count = _logic.get_weave_card_huangshi(weaveitems[j].weave_kind, weaveitems[j].center_card, weave_cards);
				for (int x = 0; x < count; x++) {
					if (_logic.is_magic_card(weave_cards[x]))
						weave_cards[x] += GameConstants_KWX.CARD_ESPECIAL_TYPE_LAI_ZI;

					weaveItem_item.addWeaveCard(weave_cards[x]);
				}

				roomResponse.addWeaveItems(weaveItem_item);
			}
		}

		int ting_count = _playerStatus[seat_index]._hu_out_card_count;

		roomResponse.setOutCardCount(ting_count);

		for (int i = 0; i < ting_count; i++) {
			int ting_card_cout = _playerStatus[seat_index]._hu_out_card_ting_count[i];
			roomResponse.addOutCardTingCount(ting_card_cout);

			int out_card = _playerStatus[seat_index]._hu_out_card_ting[i];
			roomResponse.addOutCardTing(out_card + GameConstants_KWX.CARD_ESPECIAL_TYPE_NEW_TING);

			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < ting_card_cout; j++) {
				int card = _playerStatus[seat_index]._hu_out_cards[i][j];
				if (_logic.is_magic_card(card))
					card += GameConstants_KWX.CARD_ESPECIAL_TYPE_LAI_ZI;
				int_array.addItem(card);
			}
			roomResponse.addOutCardTingCards(int_array);
		}

		send_response_to_player(seat_index, roomResponse);

		GRR.add_room_response(roomResponse);

		return true;
	}

	// 显示在玩家前面的牌,小胡牌,摸杠牌
	public boolean operate_show_card(int seat_index, int type, int count, int cards[], int to_player) {
		//// 发送玩家出牌 201 =玩家出牌(CMD_OutCard: operate_player,operate_card)
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_SHOW_CARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(type);// 出牌
		roomResponse.setCardCount(count);

		int weaveCount = 0;
		int weave_count = GRR._weave_count[seat_index];
		WeaveItem[] weaveitems = GRR._weave_items[seat_index];
		// TODO 向其他人发送组合牌数据的时候，暗杠不能发center_card数据
		if (weave_count > 0) {
			for (int j = 0; j < weave_count; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(weaveitems[j].provide_player);
				weaveItem_item.setPublicCard(weaveitems[j].public_card);
				weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
				if (weaveitems[j].weave_kind == GameConstants_KWX.WIK_GANG && weaveitems[j].public_card == 0) {
					weaveItem_item.setCenterCard(weaveitems[j].center_card);

					for (int x = 0; x < 4; x++) {
						weaveItem_item.addWeaveCard(-1);
					}
				} else {
					weaveItem_item.setCenterCard(weaveitems[j].center_card);

					int[] weave_cards = new int[4];
					int c = _logic.get_weave_card_huangshi(weaveitems[j].weave_kind, weaveitems[j].center_card, weave_cards);
					for (int x = 0; x < c; x++) {
						if (_logic.is_magic_card(weave_cards[x]))
							weave_cards[x] += GameConstants_KWX.CARD_ESPECIAL_TYPE_LAI_ZI;

						weaveItem_item.addWeaveCard(weave_cards[x]);
					}
				}

				if (weaveitems[j].weave_kind == GameConstants_KWX.WIK_LIANG && type != GameConstants.Show_Card_LY_HU) {
					roomResponse.addCardData(weaveitems[j].center_card);
					roomResponse.addCardData(weaveitems[j].center_card);
					roomResponse.addCardData(weaveitems[j].center_card);
					roomResponse.setCardCount(roomResponse.getCardCount() + 3);
					continue;
				}
				weaveCount++;
				roomResponse.addWeaveItems(weaveItem_item);
			}
		}
		roomResponse.setWeaveCount(weaveCount);

		for (int i = 0; i < count; i++) {
			roomResponse.addCardData(cards[i]);
		}
		for (int i = 0; i < _playerStatus[seat_index]._hu_card_count; i++) {
			roomResponse.addChiHuCards(_playerStatus[seat_index]._hu_cards[i]);
		}
		if (to_player == GameConstants_KWX.INVALID_SEAT) {
			GRR.add_room_response(roomResponse);
			return this.send_response_to_room(roomResponse);
		} else {
			return this.send_response_to_player(to_player, roomResponse);
		}
	}

	// 七小对牌 七小对：胡牌时，手上任意七对牌。
	public int is_qi_xiao_dui(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int cur_card) {

		// 组合判断
		if (cbWeaveCount != 0)
			return GameConstants_KWX.WIK_NULL;

		// 单牌数目
		int cbReplaceCount = 0;
		int nGenCount = 0;

		// 临时数据
		int cbCardIndexTemp[] = new int[GameConstants_KWX.MAX_INDEX];
		for (int i = 0; i < GameConstants_KWX.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入数据
		int cbCurrentIndex = _logic.switch_to_card_index(cur_card);
		cbCardIndexTemp[cbCurrentIndex]++;

		// 计算单牌
		for (int i = 0; i < GameConstants_KWX.MAX_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];

			// 单牌统计
			if (cbCardCount == 1 || cbCardCount == 3)
				cbReplaceCount++;

			if (cbCardCount == 4) {
				nGenCount++;
			}
		}
		if (cbReplaceCount > 0)
			return GameConstants_KWX.WIK_NULL;

		if (nGenCount > 0) {
			if (nGenCount == 1) {
				return GameConstants_KWX.CHR_LONG_QI_DUI;
			} else if (nGenCount == 2) {
				return GameConstants_KWX.CHR_SHUANG_LONG_QI_DUI;
			} else if (nGenCount == 3) {
				return GameConstants_KWX.CHR_SAN_LONG_QI_DUI;
			}
			return GameConstants_KWX.CHR_LONG_QI_DUI;
		} else {
			return GameConstants_KWX.CHR_XIAO_QI_DUI;
		}

	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {
		if (card_type == GameConstants_KWX.HU_CARD_TYPE_ZIMO) {
			chiHuRight.opr_or(GameConstants_KWX.CHR_ZI_MO);
		} else if (card_type == GameConstants_KWX.HU_CARD_TYPE_QIANG_GANG) {
			chiHuRight.opr_or(GameConstants_KWX.CHR_QING_GANG_HU);// 抢杠胡
		} else if (card_type == GameConstants_KWX.HU_CARD_TYPE_GANG_KAI_HUA) {
			chiHuRight.opr_or(GameConstants_KWX.CHR_GNAG_KAI);
		} else {
			chiHuRight.opr_or(GameConstants_KWX.CHR_SHU_FAN);
		}

		boolean da_hu = checkSanYuan(cards_index, weaveItems, weave_count, cur_card, chiHuRight);

		if (_logic.is_qing_yi_se(cards_index, weaveItems, weave_count, cur_card)) {
			da_hu = true;
			chiHuRight.opr_or(GameConstants_KWX.CHR_QING_YI_SE);
		}

		int xiao_qi_dui = is_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card);
		if (xiao_qi_dui != GameConstants_KWX.WIK_NULL) {
			chiHuRight.opr_or(xiao_qi_dui);

			// // TODO: 暗四归一
			// if (cards_index[_logic.switch_to_card_index(cur_card)] == 3) {
			// chiHuRight.opr_or(GameConstants_KWX.CHR_HU_AN_SI_GUI_YI);
			// }
			//
			// // 明四归一
			// for (int i = 0; i < weave_count; i++) {
			// if (weaveItems[i].weave_kind != GameConstants_KWX.WIK_PENG) {
			// continue;
			// }
			//
			// if (weaveItems[i].center_card == cur_card) {
			// chiHuRight.opr_or(GameConstants_KWX.CHR_HU_MING_SI_GUI_YI);
			// break;
			// }
			//
			// // 只在清一色这种牌型里才算，其他屁胡如果碰到这种牌不算明四归一。
			// if
			// (cards_index[_logic.switch_to_card_index(weaveItems[i].center_card)]
			// == 1
			// &&
			// !chiHuRight.opr_and(GameConstants_KWX.CHR_QING_YI_SE).is_empty())
			// {
			// chiHuRight.opr_or(GameConstants_KWX.CHR_HU_MING_SI_GUI_YI);
			// break;
			// }
			// }
			// // 这副牌算明四归一，不算暗四归一，也是只有在清一色里边才算，屁胡不算
			// int max_ting_count = GameConstants_KWX.MAX_ZI_FENG;
			// for (int i = 0; i < max_ting_count &&
			// _logic.switch_to_card_data(i) != cur_card; i++) {
			// if (cards_index[i] == 4 &&
			// !chiHuRight.opr_and(GameConstants_KWX.CHR_QING_YI_SE).is_empty())
			// {
			// chiHuRight.opr_or(GameConstants_KWX.CHR_HU_MING_SI_GUI_YI);
			// break;
			// }
			// }

			return GameConstants_KWX.WIK_CHI_HU;
		}

		int cbCardIndexTemp[] = new int[GameConstants_KWX.MAX_INDEX];
		for (int i = 0; i < GameConstants_KWX.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}
		cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
		boolean bValue = _logic.analyse_card_(cbCardIndexTemp, weaveItems, weave_count, analyseItemArray, true);
		if (!bValue) {
			chiHuRight.set_empty();
			return GameConstants_KWX.WIK_NULL;
		}

		// 卡五星
		if (_logic.get_card_value(cur_card) == 5) {
			out: for (AnalyseItem analyseItem : analyseItemArray) {
				for (int i = 0; i < analyseItem.cbWeaveKind.length; i++) {
					if (analyseItem.cbWeaveKind[i] == GameConstants_KWX.WIK_LEFT && _logic.get_card_value(analyseItem.cbCenterCard[i]) == 4
							&& analyseItem.cbCardData[i][1] == cur_card) {
						da_hu = true;
						chiHuRight.opr_or(GameConstants_KWX.CHR_KA_WU_XING);
						break out;
					}
					if (analyseItem.cbWeaveKind[i] == GameConstants_KWX.WIK_CENTER && _logic.get_card_value(analyseItem.cbCenterCard[i]) == 5
							&& analyseItem.cbCardData[i][1] == cur_card) {
						da_hu = true;
						chiHuRight.opr_or(GameConstants_KWX.CHR_KA_WU_XING);
						break out;
					}
					if (analyseItem.cbWeaveKind[i] == GameConstants_KWX.WIK_RIGHT && _logic.get_card_value(analyseItem.cbCenterCard[i]) == 6
							&& analyseItem.cbCardData[i][1] == cur_card) {
						da_hu = true;
						chiHuRight.opr_or(GameConstants_KWX.CHR_KA_WU_XING);
						break out;
					}
				}
			}
		}

		if (_logic.get_card_count_by_index(cards_index) == 1) {
			da_hu = true;
			chiHuRight.opr_or(GameConstants_KWX.CHR_SHOU_ZHUA_YI);
		} // 碰碰胡
		else if (AnalyseCardUtil.analyse_peng_hu_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card), _logic.get_all_magic_card_index(),
				_logic.get_magic_card_count())) {
			da_hu = true;
			chiHuRight.opr_or(GameConstants_KWX.CHR_HU_PENG_PENG);
		}

		// 半频道下的清一色明四归一/暗四归一按照全频道的规则来判定 , 李程产品——-2018-7-4 18:01
		if (has_rule(GameConstants_KWX.GAME_RULE_QUAN_PIN_DAO)
				|| (has_rule(GameConstants_KWX.GAME_RULE_BAN_PIN_DAO) && !chiHuRight.opr_and(GameConstants_KWX.CHR_QING_YI_SE).is_empty())) {
			// TODO: 全频道下的暗四归一：手里有四张一样的，没有碰杠，不管胡什么牌，不管有没有跟其他牌组成顺子，都算暗四归一
			// 李程产品——-2018-7-4 18:01
			if (cards_index[_logic.switch_to_card_index(cur_card)] == 3) {
				chiHuRight.opr_or(GameConstants_KWX.CHR_HU_AN_SI_GUI_YI);
			}
			for (int i = 0; i < GameConstants_KWX.MAX_INDEX; i++) {
				if (cards_index[i] == 4) {
					chiHuRight.opr_or(GameConstants_KWX.CHR_HU_AN_SI_GUI_YI);
					break;
				}
			}

			// 明四归一 已经碰了3张，手里有一张，不管胡什么牌都算明四归一 李程产品——-2018-7-4 18:03
			for (int i = 0; i < weave_count; i++) {
				if (weaveItems[i].weave_kind != GameConstants_KWX.WIK_PENG) {
					continue;
				}

				if (weaveItems[i].center_card == cur_card || cards_index[_logic.switch_to_card_index(weaveItems[i].center_card)] == 1) {
					chiHuRight.opr_or(GameConstants_KWX.CHR_HU_MING_SI_GUI_YI);
					break;
				}
			}
		} else {
			// TODO: 半频道下的暗四归一：
			// 手里有3张，胡到第4张就算暗四归一。如果根据胡那张牌，最终胡牌时，手里的3张牌跟任何一组牌组成了顺子，那么就不算暗四归一。
			// 李程产品——-2018-7-4 18:08
			if (cards_index[_logic.switch_to_card_index(cur_card)] == 3) {
				out: for (AnalyseItem analyseItem : analyseItemArray) {
					for (int i = 0; i < analyseItem.cbWeaveKind.length; i++) {
						if (analyseItem.cbWeaveKind[i] == GameConstants_KWX.WIK_PENG && analyseItem.cbCenterCard[i] == cur_card) {
							chiHuRight.opr_or(GameConstants_KWX.CHR_HU_AN_SI_GUI_YI);
							break out;
						}
					}
				}
			}

			// 半频道下的明四归一：已经碰了3张，胡到第4张牌就算明四归一。李程产品——-2018-7-4 18:08
			for (int i = 0; i < weave_count; i++) {
				if (weaveItems[i].weave_kind != GameConstants_KWX.WIK_PENG) {
					continue;
				}

				if (weaveItems[i].center_card == cur_card) {
					chiHuRight.opr_or(GameConstants_KWX.CHR_HU_MING_SI_GUI_YI);
					break;
				}
			}
		}

		// // TODO: 暗四归一
		// if (has_rule(GameConstants_KWX.GAME_RULE_QUAN_PIN_DAO) &&
		// cards_index[_logic.switch_to_card_index(cur_card)] == 3) {
		// chiHuRight.opr_or(GameConstants_KWX.CHR_HU_AN_SI_GUI_YI);
		// }
		// if (has_rule(GameConstants_KWX.GAME_RULE_BAN_PIN_DAO) &&
		// cards_index[_logic.switch_to_card_index(cur_card)] == 3) {
		// out: for (AnalyseItem analyseItem : analyseItemArray) {
		// for (int i = 0; i < analyseItem.cbWeaveKind.length; i++) {
		// if (analyseItem.cbWeaveKind[i] == GameConstants_KWX.WIK_PENG &&
		// analyseItem.cbCenterCard[i] == cur_card) {
		// chiHuRight.opr_or(GameConstants_KWX.CHR_HU_AN_SI_GUI_YI);
		// break out;
		// }
		// }
		// }
		// }
		//
		// // 明四归一
		// for (int i = 0; i < weave_count; i++) {
		// if (weaveItems[i].weave_kind != GameConstants_KWX.WIK_PENG) {
		// continue;
		// }
		//
		// if (weaveItems[i].center_card == cur_card) {
		// chiHuRight.opr_or(GameConstants_KWX.CHR_HU_MING_SI_GUI_YI);
		// break;
		// }
		//
		// // 只在清一色这种牌型里才算，其他屁胡如果碰到这种牌不算明四归一。
		// if
		// (cards_index[_logic.switch_to_card_index(weaveItems[i].center_card)]
		// == 1
		// && !chiHuRight.opr_and(GameConstants_KWX.CHR_QING_YI_SE).is_empty())
		// {
		// chiHuRight.opr_or(GameConstants_KWX.CHR_HU_MING_SI_GUI_YI);
		// break;
		// }
		// }
		// // 这副牌算明四归一，不算暗四归一，也是只有在清一色里边才算，屁胡不算
		// int max_ting_count = GameConstants_KWX.MAX_ZI_FENG;
		// for (int i = 0; i < max_ting_count && _logic.switch_to_card_data(i)
		// != cur_card; i++) {
		// if (cards_index[i] == 4 &&
		// !chiHuRight.opr_and(GameConstants_KWX.CHR_QING_YI_SE).is_empty()) {
		// chiHuRight.opr_or(GameConstants_KWX.CHR_HU_MING_SI_GUI_YI);
		// break;
		// }
		// }

		return GameConstants_KWX.WIK_CHI_HU;
	}

	@Override
	public boolean exe_jian_pao_hu(int seat_index, int action, int card) {

		GameSchedule.put(new JianPaoHuRunnable(this.getRoom_id(), seat_index, action, card), 0, TimeUnit.MILLISECONDS);

		return true;
	}

	/**
	 * 处理吃胡的玩家
	 * 
	 * @param seat_index
	 * @param operate_card
	 * @param rm
	 */
	public void process_chi_hu_player_operate(int seat_index, int operate_card, boolean rm) {
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		// filtrate_right(seat_index, chr);

		if (is_mj_type(GameConstants.GAME_TYPE_ZZ) || is_mj_type(GameConstants.GAME_TYPE_HZ) || is_mj_type(GameConstants.GAME_TYPE_FLS_HZ_LX)
				|| is_mj_type(GameConstants.GAME_TYPE_HENAN_ZHUAN_ZHUAN) || is_mj_type(GameConstants.GAME_TYPE_NEW_SAN_MEN_XIA)) { // liuyan
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
		for (int p = 0; p < getTablePlayerNumber(); p++) {
			int cards[] = new int[GameConstants.MAX_COUNT];
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[p], cards);

			if (p == seat_index) {
				cards[hand_card_count] = operate_card + GameConstants.CARD_ESPECIAL_TYPE_HU;
				hand_card_count++;
			}
			this.operate_show_card(p, GameConstants.Show_Card_HU, hand_card_count, cards, GameConstants.INVALID_SEAT);
		}

		return;
	}

	public boolean checkSanYuan(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight) {
		int[] magic_card = new int[] { GameConstants_KWX.HZ_MAGIC_CARD, GameConstants_KWX.BB_MAGIC_CARD, GameConstants_KWX.FC_MAGIC_CARD };
		boolean[] flag_shuang = new boolean[3];
		int count = 0;
		for (int m = 0; m < magic_card.length; m++) {
			for (int w = 0; w < weave_count; w++) {
				if (weaveItems[w].weave_kind != GameConstants_KWX.WIK_PENG && weaveItems[w].weave_kind != GameConstants_KWX.WIK_GANG
						&& weaveItems[w].weave_kind != GameConstants_KWX.WIK_LIANG) {
					continue;
				}

				if (weaveItems[w].center_card == magic_card[m]) {
					count++;
					break;
				}
			}

			if (cards_index[_logic.switch_to_card_index(magic_card[m])] >= 3
					|| (cards_index[_logic.switch_to_card_index(magic_card[m])] == 2 && cur_card == magic_card[m])) {
				count++;
				continue;
			}
			if (cards_index[_logic.switch_to_card_index(magic_card[m])] == 2
					|| (cards_index[_logic.switch_to_card_index(magic_card[m])] == 1 && cur_card == magic_card[m])) {
				flag_shuang[m] = true;
			}
		}

		boolean da_hu = false;
		boolean flag = flag_shuang[0] | flag_shuang[1] | flag_shuang[2];
		if (count == 2 && flag) {
			da_hu = true;
			chiHuRight.opr_or(GameConstants_KWX.CHR_HU_XIAO_SAN_YUAN);
		}
		if (count == 3) {
			da_hu = true;
			chiHuRight.opr_or(GameConstants_KWX.CHR_HU_DA_SAN_YUAN);
		}
		return da_hu;
	}

	/**
	 * 玩家动作--通知玩家弹出/关闭操作
	 * 
	 * @param seat_index
	 * @param close
	 * @return
	 */
	public boolean operate_player_action(int seat_index, boolean close) {
		PlayerStatus curPlayerStatus = _playerStatus[seat_index];

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PLAYER_ACTION);
		roomResponse.setTarget(seat_index);
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
			for (int n = 0; n < curPlayerStatus._action_weaves[i].weave_card.length; n++) {
				if (curPlayerStatus._action_weaves[i].weave_card[n] == 0) {
					continue;
				}
				weaveItem_item.addWeaveCard(curPlayerStatus._action_weaves[i].weave_card[n]);
			}
			roomResponse.addWeaveItems(weaveItem_item);
		}
		GRR.add_room_response(roomResponse);
		this.send_response_to_player(seat_index, roomResponse);
		return true;
	}

	public int checkLiangAddWeave(int seat_index, int[] com_three_index) {
		int[] three_index = new int[4];
		int three_count = 0;

		for (int i = 0; i < GameConstants_KWX.MAX_ZI_FENG; i++) {
			if (GRR._cards_index[seat_index][i] >= 3) {
				three_index[three_count++] = i;
			}
		}

		int count = 0;
		for (int i = 0; i < three_count; i++) {
			int[] temp_cards_index = Arrays.copyOf(GRR._cards_index[seat_index], GRR._cards_index[seat_index].length);
			temp_cards_index[three_index[i]] -= 3;

			for (int j = 0; j < GameConstants_KWX.MAX_ZI_FENG; j++) {
				if (temp_cards_index[j] > 0) {
					temp_cards_index[j]--;
					int ting_count = get_ting_card(new int[GameConstants_KWX.MAX_ZI_FENG], temp_cards_index, GRR._weave_items[seat_index],
							GRR._weave_count[seat_index], seat_index);
					temp_cards_index[j]++;
					if (ting_count > 0) {
						com_three_index[count++] = three_index[i];
						break;
					}
				}
			}
		}

		return count;
	}

	/**
	 * 
	 * @return
	 */
	public boolean estimate_gang_respond(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		PlayerStatus playerStatus = null;

		int action = GameConstants_KWX.WIK_NULL;

		// 动作判断
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 用户过滤
			if (seat_index == i || player_liang[i] == 1)
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
						GameConstants_KWX.HU_CARD_TYPE_QIANG_GANG, i);

				// 结果判断
				if (action != 0 && get_fan(i, seat_index) > pass_hu_fan[i]) {
					_playerStatus[i].add_action(GameConstants_KWX.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
					chr.opr_or(GameConstants_KWX.CHR_QING_GANG_HU);// 抢杠胡
					bAroseAction = true;
				}
			}
		}

		if (bAroseAction == true) {
			_provide_player = seat_index;// 谁打的牌
			_provide_card = card;// 打的什么牌
			_resume_player = _current_player;// 保存当前轮到的玩家
			_current_player = GameConstants_KWX.INVALID_SEAT;// 有需要操作的玩家。当前玩家为空
		}

		return bAroseAction;
	}

	public float get_di_fen() {
		float lChiHuScore = 1;
		if (has_rule(GameConstants_KWX.GAME_RULE_DI_FEN_1)) {
			lChiHuScore = 1;
		}
		if (has_rule(GameConstants_KWX.GAME_RULE_DI_FEN_2)) {
			lChiHuScore = 2;
		}
		if (has_rule(GameConstants_KWX.GAME_RULE_DI_FEN_3)) {
			lChiHuScore = 3;
		}
		if (has_rule(GameConstants_KWX.GAME_RULE_DI_FEN_5)) {
			lChiHuScore = 5;
		}
		if (has_rule(GameConstants_KWX.GAME_RULE_DI_FEN_10)) {
			lChiHuScore = 10;
		}
		return lChiHuScore;
	}

	/**
	 * @param seat_index
	 * @param room_rq
	 * @param type
	 * @return
	 */
	@Override
	public boolean handler_requst_message_deal(Player player, int seat_index, RoomRequest room_rq, int type) {
		if (type == MsgConstants.REQUST_KWX_KOU_CHECK) {
			KWXLiangCard req = PBUtil.toObject(room_rq, KWXLiangCard.class);
			List<Integer> kou = req.getKouCardsList();
			int[] cards_index = Arrays.copyOf(GRR._cards_index[seat_index], GRR._cards_index[seat_index].length);

			kou.forEach((card) -> {
				int cbRemoveCard[] = new int[] { card, card, card };
				if (!_logic.remove_cards_by_index(cards_index, cbRemoveCard, 3)) {
					log_player_error(seat_index, "扣牌出错");
					return;
				}
			});

			boolean ting_send_card = false;
			int ting_count = 0;
			for (int i = 0; i < GameConstants_KWX.MAX_ZI_FENG; i++) {
				if (cards_index[i] > 0) {
					cards_index[i]--;
					_playerStatus[seat_index]._hu_out_card_ting_count[ting_count] = get_ting_card(_playerStatus[seat_index]._hu_out_cards[ting_count],
							cards_index, GRR._weave_items[seat_index], GRR._weave_count[seat_index], seat_index);

					if (_playerStatus[seat_index]._hu_out_card_ting_count[ting_count] > 0) {
						_playerStatus[seat_index]._hu_out_card_ting[ting_count] = _logic.switch_to_card_data(i);
						ting_count++;
						if (_send_card_data != GameConstants_KWX.INVALID_VALUE && seat_index == _provide_player
								&& _logic.switch_to_card_index(_send_card_data) == i) {
							ting_send_card = true;
						}
					}
					cards_index[i]++;
				}
			}

			if (ting_count > 0) {
				int cards[] = new int[GameConstants_KWX.MAX_COUNT];
				int hand_card_count = _logic.switch_to_cards_data(cards_index, cards);
				for (int i = 0; i < hand_card_count; i++) {
					for (int j = 0; j < ting_count; j++) {
						if (cards[i] == _playerStatus[seat_index]._hu_out_card_ting[j]) {
							cards[i] += GameConstants_KWX.CARD_ESPECIAL_TYPE_TING;
							break;
						}
					}
				}

				filterHandCards(seat_index, cards, hand_card_count);
				for (int c = 0; c < hand_card_count && _send_card_data != GameConstants_KWX.INVALID_VALUE && seat_index == _provide_player; c++) {
					if (cards[c] == _send_card_data || cards[c] == (_send_card_data + GameConstants_KWX.CARD_ESPECIAL_TYPE_NON_OUT)
							|| cards[c] == (_send_card_data + GameConstants_KWX.CARD_ESPECIAL_TYPE_TING)) {
						cards[c] = 0;
						break;
					}
				}
				int show_cards[] = new int[GameConstants_KWX.MAX_COUNT];
				int show_hand_card_count = 0;
				for (int c = 0; c < hand_card_count; c++) {
					if (cards[c] != 0) {
						show_cards[show_hand_card_count++] = cards[c];
					}
				}
				operate_player_cards_with_ting(seat_index, show_hand_card_count, show_cards, 0, null);

				if (_send_card_data != GameConstants_KWX.INVALID_VALUE && seat_index == _provide_player) {
					if (!kou.contains(_send_card_data)
							|| (kou.contains(_send_card_data) && GRR._cards_index[seat_index][_logic.switch_to_card_index(_send_card_data)] == 4)) {
						int show_send_card = _send_card_data;
						if (filterHandCard(seat_index, _send_card_data) && filterHandCards(seat_index, cards, hand_card_count) != hand_card_count) {
							show_send_card += GameConstants_KWX.CARD_ESPECIAL_TYPE_NON_OUT;
						} else if (ting_send_card) {
							show_send_card += GameConstants_KWX.CARD_ESPECIAL_TYPE_TING;
						}
						operate_player_get_card(seat_index, 1, new int[] { show_send_card }, GameConstants_KWX.INVALID_SEAT);
					}
				}
			}
		}
		return true;
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		countCardType(chr, seat_index);
		if (!has_rule(GameConstants_KWX.GAME_RULE_MEI_JU_PIAO) && !has_rule(GameConstants_KWX.GAME_RULE_DING_PIAO)) {
			Arrays.fill(_player_result.pao, -1);
		}
		/////////////////////////////////////////////// 算分//////////////////////////
		// 统计
		// if (zimo) {
		// // 自摸
		// for (int i = 0; i < getTablePlayerNumber(); i++) {
		// if (i == seat_index) {
		// continue;
		// }
		// GRR._lost_fan_shu[i][seat_index] = wFanShu;
		// }
		// } else {// 点炮
		// GRR._lost_fan_shu[provide_index][seat_index] = wFanShu;
		// }

		float lChiHuScore = get_di_fen();

		////////////////////////////////////////////////////// 自摸 算分
		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				int wFanShu = get_fan(seat_index, i);// 番数

				float s = lChiHuScore * (wFanShu + GRR._count_pick_niao); // 自摸加1分

				float piao = 0;
				if (_player_result.pao[i] != -1) {
					piao += _player_result.pao[i] * lChiHuScore;
				}
				if (_player_result.pao[seat_index] != -1) {
					piao += _player_result.pao[seat_index] * lChiHuScore;
				}

				s += piao;

				piao_score[i] -= piao;
				piao_score[seat_index] += piao;
				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;
			}
		}
		////////////////////////////////////////////////////// 点炮 算分
		else {
			GRR._chi_hu_rights[provide_index].opr_or(GameConstants_KWX.CHR_FANG_PAO);
			if (player_liang[seat_index] == 1 && player_liang[provide_index] != 1) {
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (i == seat_index) {
						continue;
					}

					int wFanShu = get_fan(seat_index, provide_index);// 番数
					float s = lChiHuScore * (wFanShu + GRR._count_pick_niao); // 自摸加1分
					float piao = 0;
					if (_player_result.pao[provide_index] != -1) {
						piao += _player_result.pao[i] * lChiHuScore;
					}
					if (_player_result.pao[seat_index] != -1) {
						piao += _player_result.pao[seat_index] * lChiHuScore;
					}

					s += piao;
					piao_score[provide_index] -= piao;
					piao_score[seat_index] += piao;
					GRR._game_score[provide_index] -= s;
					GRR._game_score[seat_index] += s;
				}
			} else {
				int wFanShu = get_fan(seat_index, provide_index);// 番数

				float s = lChiHuScore * (wFanShu + GRR._count_pick_niao);
				float piao = 0;
				if (_player_result.pao[provide_index] != -1) {
					piao += _player_result.pao[provide_index] * lChiHuScore;
				}
				if (_player_result.pao[seat_index] != -1) {
					piao += _player_result.pao[seat_index] * lChiHuScore;
				}

				s += piao;
				piao_score[provide_index] -= piao;
				piao_score[seat_index] += piao;
				// 胡牌分
				GRR._game_score[provide_index] -= s;
				GRR._game_score[seat_index] += s;
			}
		}

		// 设置变量
		GRR._provider[seat_index] = provide_index;
		_status_gang = false;
		_status_gang_hou_pao = false;

		change_player_status(seat_index, GameConstants_KWX.INVALID_VALUE);
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
					if (type == GameConstants_KWX.CHR_QING_GANG_HU) {
						if (has_rule(GameConstants_KWX.GAME_RULE_FENG_DING_16)) {
							result.append(" 抢杠胡X4");
						} else {
							result.append(" 抢杠胡X2");
						}
						qiang_gang_hu = true;
					}
					if (type == GameConstants_KWX.CHR_GNAG_KAI) {
						if (has_rule(GameConstants_KWX.GAME_RULE_GANG_SHANG_HUA)) {
							result.append(" 杠上花X4");
						} else {
							result.append(" 杠上花x2");
						}
					}
					if (type == GameConstants_KWX.CHR_GANG_SHANG_PAO) {
						if (has_rule(GameConstants_KWX.GAME_RULE_GANG_SHANG_HUA)) {
							result.append(" 杠上炮X4");
						} else {
							result.append(" 杠上炮x2");
						}
					}
					if (type == GameConstants_KWX.CHR_QING_YI_SE) {
						result.append(" 清一色X4");
					}
					if (type == GameConstants_KWX.CHR_XIAO_QI_DUI) {
						result.append(" 七小对X4");
					}
					if (type == GameConstants_KWX.CHR_KA_WU_XING) {
						if (has_rule(GameConstants_KWX.GAME_RULE_KA_WU_XING)) {
							result.append(" 卡五星X4");
						} else {
							result.append(" 卡五星X2");
						}
					}
					if (type == GameConstants_KWX.CHR_HU_PENG_PENG) {
						if (has_rule(GameConstants_KWX.GAME_RULE_PENG_PENG_HU)) {
							result.append(" 碰碰胡X4");
						} else {
							result.append(" 碰碰胡X2");
						}
					}
					if (type == GameConstants_KWX.CHR_LIANG_ZHANG) {
						result.append(" 亮张X2");
					}
					if (type == GameConstants_KWX.CHR_HU_MING_SI_GUI_YI) {
						if (has_rule(GameConstants_KWX.GAME_RULE_MING_SI_GUI_YI_4)) {
							result.append(" 明四归一X4");
						} else {
							result.append(" 明四归一X2");
						}
					}
					if (type == GameConstants_KWX.CHR_HU_AN_SI_GUI_YI) {
						result.append(" 暗四归一X4");
					}
					if (has_rule(GameConstants_KWX.GAME_RULE_QUAN_PIN_DAO)) {
						if (type == GameConstants_KWX.CHR_HAI_DI_HU) {
							result.append(" 海底胡X2");
						}
					}
					if (type == GameConstants_KWX.CHR_SHOU_ZHUA_YI) {
						if (has_rule(GameConstants_KWX.GAME_RULE_SHOU_ZHUA_YI)) {
							result.append(" 手抓一X8");
						} else {
							result.append(" 手抓一X4");
						}
					}
					if (type == GameConstants_KWX.CHR_LONG_QI_DUI) {
						result.append(" 龙七对X8");
					}
					if (type == GameConstants_KWX.CHR_SHUANG_LONG_QI_DUI) {
						if (has_rule(GameConstants_KWX.GAME_RULE_FENG_DING_16)) {
							result.append(" 双龙七对X16");
						} else {
							result.append(" 双龙七对X8");
						}
					}
					if (type == GameConstants_KWX.CHR_SAN_LONG_QI_DUI) {
						if (has_rule(GameConstants_KWX.GAME_RULE_FENG_DING_16)) {
							result.append(" 三龙七对X16");
						} else {
							result.append(" 三龙七对X8");
						}
					}
					if (type == GameConstants_KWX.CHR_HU_DA_SAN_YUAN) {
						result.append(" 大三元X8");
					}
					if (type == GameConstants_KWX.CHR_ZI_MO) {
						result.append(" 自摸X1");
					}
					if (type == GameConstants_KWX.CHR_HU_XIAO_SAN_YUAN) {
						result.append(" 小三元X4");
					}
				} else if (type == GameConstants_KWX.CHR_FANG_PAO) {
					if (qiang_gang_hu) {
						result.append(" 被抢杠");
					} else {
						result.append(" 放炮");
					}
				}

				if (has_rule(GameConstants_KWX.GAME_RULE_QUAN_PIN_DAO)) {
					if (type == GameConstants_KWX.CHR_PAO_GANG_GANG) {
						result.append(" 杠上杠X4");
					} else if (type == GameConstants_KWX.CHR_PAO_GANG_GANG_GANG) {
						result.append(" 杠上杠上杠X8");
					} else if (type == GameConstants_KWX.CHR_PAO_GANG_GANG_GANG_GANG) {
						result.append(" 杠上杠上杠上杠X16");
					}
				}
			}

			int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0;

			if (GRR != null) {
				for (int tmpPlayer = 0; tmpPlayer < this.getTablePlayerNumber(); tmpPlayer++) {
					for (int w = 0; w < GRR._weave_count[tmpPlayer]; w++) {
						if (GRR._weave_items[tmpPlayer][w].weave_kind != GameConstants_KWX.WIK_GANG) {
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

			// if (an_gang > 0) {
			// result.append(" 暗杠X" + an_gang);
			// }
			// if (ming_gang > 0) {
			// result.append(" 明杠X" + ming_gang);
			// }
			// if (fang_gang > 0) {
			// result.append(" 放杠X" + fang_gang);
			// }
			// if (jie_gang > 0) {
			// result.append(" 接杠X" + jie_gang);
			// }

			if (player_liang[player] == 1) {
				result.append(" 亮倒 X2");
			} else {
				result.append(" 未亮倒");
			}
			if (piao_score[player] != 0) {
				result.append(" 漂" + piao_score[player]);
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

		for (int i = 0; i < GameConstants_KWX.MAX_NIAO_CARD; i++) {
			GRR._cards_data_niao[i] = GameConstants_KWX.INVALID_VALUE;
		}

		for (int i = 0; i < GameConstants_KWX.GAME_PLAYER; i++) {
			GRR._player_niao_count[i] = 0;
			for (int j = 0; j < GameConstants_KWX.MAX_NIAO_CARD; j++) {
				GRR._player_niao_cards[i][j] = GameConstants_KWX.INVALID_VALUE;
			}
		}

		GRR._show_bird_effect = show;
		GRR._count_niao = getNIaoNum(seat_index);

		if (GRR._count_niao == 0 || GRR._left_card_count == 0) {
			return;
		}

		if (has_rule(GameConstants_KWX.GAME_RULE_MAI_MA_147)) {
			GRR._count_niao = GRR._left_card_count < 6 ? GRR._left_card_count : 6;

			int cbCardIndexTemp[] = new int[GameConstants_KWX.MAX_INDEX];
			_logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count, GRR._count_niao, cbCardIndexTemp);
			GRR._left_card_count -= GRR._count_niao;
			_logic.switch_to_cards_data(cbCardIndexTemp, GRR._cards_data_niao);

			int hu_card_value = _logic.get_card_value(GRR._chi_hu_card[seat_index][0]);
			if (GRR._chi_hu_card[seat_index][0] == GameConstants_KWX.HZ_MAGIC_CARD) {
				hu_card_value = 1;
			}
			if (GRR._chi_hu_card[seat_index][0] == GameConstants_KWX.FC_MAGIC_CARD) {
				hu_card_value = 2;
			}
			if (GRR._chi_hu_card[seat_index][0] == GameConstants_KWX.BB_MAGIC_CARD) {
				hu_card_value = 3;
			}
			int[] cards = new int[3];
			cards[0] = hu_card_value;
			for (int i = 1; i <= 2; i++) {
				int next_value = hu_card_value + 3 * i;
				if (next_value != 9) {
					next_value = next_value % 9;
				}
				cards[i] = next_value;
			}

			int[] niao_data = new int[6];
			int niao_data_count = 0;
			for (int i = 0; i < GRR._count_niao; i++) {

				if (GRR._cards_data_niao[i] >= GameConstants_KWX.HZ_MAGIC_CARD) {
					GRR._count_pick_niao++;
					niao_data[niao_data_count++] = GRR._cards_data_niao[i];
					continue;
				}

				for (int j = 0; j < 3; j++) {
					if (cards[j] == _logic.get_card_value(GRR._cards_data_niao[i])) {
						niao_data[niao_data_count++] = GRR._cards_data_niao[i];
						GRR._count_pick_niao++;
						break;
					}
				}
			}

			GRR._player_niao_cards[seat_index] = Arrays.copyOf(niao_data, niao_data_count);
			GRR._player_niao_count[seat_index] = niao_data_count;
			// GRR._count_niao = niao_data_count;
			// GRR._cards_data_niao = Arrays.copyOf(niao_data, niao_data_count);
			GRR._count_pick_niao = GRR._count_pick_niao == 6 ? 10 : GRR._count_pick_niao;
			return;
		}

		if (GRR._count_niao > GameConstants_KWX.ZHANIAO_0) {
			if (card == GameConstants_KWX.INVALID_VALUE) {
				int cbCardIndexTemp[] = new int[GameConstants_KWX.MAX_INDEX];
				_logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count, GRR._count_niao, cbCardIndexTemp);
				GRR._left_card_count -= GRR._count_niao;
				_logic.switch_to_cards_data(cbCardIndexTemp, GRR._cards_data_niao);
			} else {
				for (int i = 0; i < GRR._count_niao; i++) {
					GRR._cards_data_niao[i] = card;
				}
			}
		}

		if (GRR._cards_data_niao[0] >= GameConstants_KWX.HZ_MAGIC_CARD) {
			GRR._cards_data_niao[0] = _repertory_card[_all_card_len - 1];
		}
		for (int i = 0; i < GRR._count_niao; i++) {
			GRR._player_niao_cards[seat_index][GRR._player_niao_count[seat_index]] = GRR._cards_data_niao[i];
			GRR._player_niao_count[seat_index]++;
		}
		if (GRR._cards_data_niao[0] >= GameConstants_KWX.HZ_MAGIC_CARD) {
			GRR._count_pick_niao = 10;
		} else {
			GRR._count_pick_niao = _logic.get_card_value(GRR._cards_data_niao[0]);
		}
	}

	public boolean filterLiang(int seat_index) {
		boolean showLiang = true;
		int non_out_count = 0;
		for (int t = 0; t < _playerStatus[seat_index]._hu_out_card_count; t++) {
			int hand_card = _playerStatus[seat_index]._hu_out_card_ting[t];
			if (filterHandCard(seat_index, hand_card)) {
				non_out_count++;
			}
		}

		if (non_out_count == _playerStatus[seat_index]._hu_out_card_count) {
			return false;
		}
		return showLiang;
	}

	public int filterHandCards(int seat_index, int[] hand_cards, int hand_count) {
		int[] hand_cards_temp = Arrays.copyOf(hand_cards, hand_count);

		int non_out_card = 0;
		for (int p = 0; p < getTablePlayerNumber(); p++) {
			if (p == seat_index) {
				continue;
			}
			if (player_liang[p] != 1) {
				continue;
			}

			for (int h = 0; h < hand_count; h++) {
				if (hand_cards_temp[h] > GameConstants_KWX.CARD_ESPECIAL_TYPE_NON_OUT) {
					continue;
				}

				int hand_c = hand_cards_temp[h] > GameConstants_KWX.CARD_ESPECIAL_TYPE_TING
						? hand_cards_temp[h] - GameConstants_KWX.CARD_ESPECIAL_TYPE_TING
						: hand_cards_temp[h];
				for (int j = 0; j < _playerStatus[p]._hu_card_count; j++) {
					if (_playerStatus[p]._hu_cards[j] == hand_c) {
						if (hand_cards_temp[h] > GameConstants_KWX.CARD_ESPECIAL_TYPE_TING) {
							hand_cards_temp[h] -= GameConstants_KWX.CARD_ESPECIAL_TYPE_TING;
						}

						hand_cards_temp[h] += GameConstants_KWX.CARD_ESPECIAL_TYPE_NON_OUT;
						non_out_card++;
					}
				}
			}
		}

		if (non_out_card != hand_count) {
			for (int h = 0; h < hand_count; h++) {
				hand_cards[h] = hand_cards_temp[h];
			}
		}
		return non_out_card;
	}

	public boolean filterHandCard(int seat_index, int hand_card) {
		for (int p = 0; p < getTablePlayerNumber(); p++) {
			if (p == seat_index) {
				continue;
			}
			if (player_liang[p] != 1) {
				continue;
			}

			for (int j = 0; j < _playerStatus[p]._hu_card_count; j++) {
				if (_playerStatus[p]._hu_cards[j] == hand_card) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 抓鸟 个数
	 * 
	 * @return
	 */
	public int getNIaoNum(int seat_index) {

		if (has_rule(GameConstants_KWX.GAME_RULE_NON_MA)) {
			return GameConstants_KWX.ZHANIAO_0;
		}

		if (has_rule(GameConstants_KWX.GAME_RULE_ZI_MO_MA)) {
			return GameConstants_KWX.ZHANIAO_1;
		}

		if (has_rule(GameConstants_KWX.GAME_RULE_LIANG_DAO_ZI_MO_MA) && player_liang[seat_index] == 1) {
			return GameConstants_KWX.ZHANIAO_1;
		}

		return GameConstants_KWX.ZHANIAO_0;
	}

	public boolean estimate_player_out_card_respond(int seat_index, int card, int type) {
		boolean bAroseAction = false;

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		PlayerStatus playerStatus = null;

		int action = GameConstants_KWX.WIK_NULL;

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (seat_index == i) {
				continue;
			}
			if (_playerStatus[i].is_bao_ting())
				continue;

			playerStatus = _playerStatus[i];

			if (player_liang[i] != 1) {
				boolean can_peng = true;
				int[] tmp_cards_data = _playerStatus[i].get_cards_abandoned_peng();
				// for (int x = 0; x <
				// GameConstants_KWX.MAX_ABANDONED_CARDS_COUNT; x++) {
				// if (tmp_cards_data[x] == card) {
				// can_peng = false;
				// break;
				// }
				// }
				if (can_peng) {
					action = _logic.check_peng(GRR._cards_index[i], card);
					if (action != 0) {
						playerStatus.add_action(action);
						playerStatus.add_peng(card, seat_index);
						bAroseAction = true;
					}

					if (GRR._left_card_count > 0) {
						action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
						if (action != 0) {
							if (player_liang[i] == 1) {
								int[] temp_cards_index = Arrays.copyOf(GRR._cards_index[i], GRR._cards_index[i].length);
								temp_cards_index[_logic.switch_to_card_index(card)] = 0;
								int[] temp_hu_cards = new int[GameConstants_KWX.MAX_ZI_FENG];
								int hu_count = get_ting_card(temp_hu_cards, temp_cards_index, GRR._weave_items[i], GRR._weave_count[i], i);
								boolean hu = true;
								if (hu_count == _playerStatus[i]._hu_card_count) {
									for (int h = 0; h < hu_count; h++) {
										if (temp_hu_cards[h] != _playerStatus[i]._hu_cards[h]) {
											hu = false;
											break;
										}
									}
								} else {
									hu = false;
								}
								if (hu) {
									playerStatus.add_action(GameConstants_KWX.WIK_GANG);
									playerStatus.add_gang(card, i, 1); // 加上杠
									bAroseAction = true;
								}
							} else {
								playerStatus.add_action(GameConstants_KWX.WIK_GANG);
								playerStatus.add_gang(card, i, 1); // 加上杠
								bAroseAction = true;
							}
						}
					}
				}
			} else {
				for (int w = 0; w < GRR._weave_count[i]; w++) {
					if (GRR._weave_items[i][w].weave_kind == GameConstants_KWX.WIK_LIANG && GRR._weave_items[i][w].center_card == card) {
						playerStatus.add_action(GameConstants_KWX.WIK_GANG);
						playerStatus.add_gang(card, seat_index, 1);
						bAroseAction = true;
						break;
					}
				}
				if (GRR._left_card_count > 0) {
					action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
					if (action != 0) {
						if (player_liang[i] == 1) {
							int[] temp_cards_index = Arrays.copyOf(GRR._cards_index[i], GRR._cards_index[i].length);
							temp_cards_index[_logic.switch_to_card_index(card)] = 0;
							int hu_count = get_ting_card(new int[GameConstants_KWX.MAX_ZI_FENG], temp_cards_index, GRR._weave_items[i],
									GRR._weave_count[i], i);
							if (hu_count == _playerStatus[i]._hu_card_count) {
								playerStatus.add_action(GameConstants_KWX.WIK_GANG);
								playerStatus.add_gang(card, i, 1); // 加上杠
								bAroseAction = true;
							}
						} else {
							playerStatus.add_action(GameConstants_KWX.WIK_GANG);
							playerStatus.add_gang(card, i, 1); // 加上杠
							bAroseAction = true;
						}
					}
				}
			}

			if (_playerStatus[i].is_chi_hu_round()) {
				boolean can_hu_this_card = true;
				int[] tmp_cards_data_hu = _playerStatus[i].get_cards_abandoned_hu();
				for (int x = 0; x < GameConstants_KWX.MAX_ABANDONED_CARDS_COUNT; x++) {
					if (tmp_cards_data_hu[x] == card) {
						can_hu_this_card = false;
						break;
					}
				}
				if (can_hu_this_card) {
					ChiHuRight chr = GRR._chi_hu_rights[i];
					chr.set_empty();
					int cbWeaveCount = GRR._weave_count[i];

					int card_type = GameConstants_KWX.HU_CARD_TYPE_JIE_PAO;
					action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr, card_type, i);
					if ((action != 0) && (type == GameConstants_KWX.GANG_TYPE_AN_GANG || type == GameConstants_KWX.GANG_TYPE_ADD_GANG
							|| type == GameConstants_KWX.GANG_TYPE_JIE_GANG)) {
						chr.opr_or(GameConstants_KWX.CHR_GANG_SHANG_PAO);
					}
					if (action != 0 && get_fan(i, seat_index) > pass_hu_fan[i]) {
						if (player_liang[i] == 1) {
							_playerStatus[i].add_action(GameConstants_KWX.WIK_CHI_HU);
							_playerStatus[i].add_chi_hu(card, seat_index);
							bAroseAction = true;
						} else {
							if (get_fan(i, seat_index) > 1 || (GRR._left_card_count == 0 && has_rule(GameConstants_KWX.GAME_RULE_QUAN_PIN_DAO))) {
								_playerStatus[i].add_action(GameConstants_KWX.WIK_CHI_HU);
								_playerStatus[i].add_chi_hu(card, seat_index);
								bAroseAction = true;
							}
						}
					}
				}
			}
		}

		if (bAroseAction) {
			_resume_player = _current_player;
			_current_player = GameConstants_KWX.INVALID_SEAT;
			_provide_player = seat_index;
		} else {
			return false;
		}

		return true;
	}

	public int get_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		int cbCardIndexTemp[] = new int[GameConstants_KWX.MAX_INDEX];
		for (int i = 0; i < GameConstants_KWX.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int max_ting_count = GameConstants_KWX.MAX_ZI_FENG;
		int real_max_ting_count = GameConstants_KWX.MAX_ZI_FENG;

		for (int i = 0; i < max_ting_count; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			if (GameConstants_KWX.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					GameConstants_KWX.CHR_ZI_MO, seat_index)) {
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

	public int get_fan(int seat_index, int provide_index) {
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		int fan = 1;
		if (!chr.opr_and(GameConstants_KWX.CHR_HU_PENG_PENG).is_empty()) {
			if (has_rule(GameConstants_KWX.GAME_RULE_PENG_PENG_HU)) {
				fan *= 4;
			} else {
				fan *= 2;
			}
		}
		if (!chr.opr_and(GameConstants_KWX.CHR_KA_WU_XING).is_empty()) {
			if (has_rule(GameConstants_KWX.GAME_RULE_KA_WU_XING)) {
				fan *= 4;
			} else {
				fan *= 2;
			}
		}
		if (!chr.opr_and(GameConstants_KWX.CHR_GNAG_KAI).is_empty()) {
			if (has_rule(GameConstants_KWX.GAME_RULE_GANG_SHANG_HUA)) {
				fan *= 4;
			} else {
				fan *= 2;
			}
		}
		if (!chr.opr_and(GameConstants_KWX.CHR_GANG_SHANG_PAO).is_empty()) {
			if (has_rule(GameConstants_KWX.GAME_RULE_GANG_SHANG_HUA)) {
				fan *= 4;
			} else {
				fan *= 2;
			}
		}
		if (!chr.opr_and(GameConstants_KWX.CHR_QING_GANG_HU).is_empty()) {
			fan *= 2;
		}
		if (!chr.opr_and(GameConstants_KWX.CHR_HU_MING_SI_GUI_YI).is_empty()) {
			if (has_rule(GameConstants_KWX.GAME_RULE_MING_SI_GUI_YI_4)) {
				fan *= 4;
			} else {
				fan *= 2;
			}
		}
		if (!chr.opr_and(GameConstants_KWX.CHR_LIANG_ZHANG).is_empty()) {
			fan *= 2;
		}
		if (!chr.opr_and(GameConstants_KWX.CHR_HU_AN_SI_GUI_YI).is_empty()) {
			fan *= 4;
		}
		if (!chr.opr_and(GameConstants_KWX.CHR_XIAO_QI_DUI).is_empty()) {
			fan *= 4;
		}
		if (!chr.opr_and(GameConstants_KWX.CHR_SHOU_ZHUA_YI).is_empty()) {
			if (has_rule(GameConstants_KWX.GAME_RULE_SHOU_ZHUA_YI)) {
				fan *= 8;
			} else {
				fan *= 4;
			}
		}
		if (!chr.opr_and(GameConstants_KWX.CHR_HU_XIAO_SAN_YUAN).is_empty()) {
			fan *= 4;
		}
		if (!chr.opr_and(GameConstants_KWX.CHR_QING_YI_SE).is_empty()) {
			fan *= 4;
		}
		if (!chr.opr_and(GameConstants_KWX.CHR_LONG_QI_DUI).is_empty()) {
			fan *= 8;
		}
		if (!chr.opr_and(GameConstants_KWX.CHR_HU_DA_SAN_YUAN).is_empty()) {
			fan *= 8;
		}
		if (!chr.opr_and(GameConstants_KWX.CHR_SHUANG_LONG_QI_DUI).is_empty() || !chr.opr_and(GameConstants_KWX.CHR_SAN_LONG_QI_DUI).is_empty()) {
			if (has_rule(GameConstants_KWX.GAME_RULE_FENG_DING_16)) {
				fan *= 16;
			} else {
				fan *= 8;
			}
		}
		if (has_rule(GameConstants_KWX.GAME_RULE_QUAN_PIN_DAO) && !chr.opr_and(GameConstants_KWX.CHR_PAO_GANG_GANG).is_empty()) {
			fan *= 4;
		}
		if (has_rule(GameConstants_KWX.GAME_RULE_QUAN_PIN_DAO) && !chr.opr_and(GameConstants_KWX.CHR_PAO_GANG_GANG_GANG).is_empty()) {
			fan *= 8;
		}
		if (has_rule(GameConstants_KWX.GAME_RULE_QUAN_PIN_DAO) && !chr.opr_and(GameConstants_KWX.CHR_PAO_GANG_GANG_GANG_GANG).is_empty()) {
			fan *= 16;
		}

		if (!chr.opr_and(GameConstants_KWX.CHR_HAI_DI_HU).is_empty()) {
			if (fan == 1) {
				if (has_rule(GameConstants_KWX.GAME_RULE_QUAN_PIN_DAO))
					fan *= 2;
			} else {
				fan *= 2;
			}
		}

		if (player_liang[seat_index] == 1) {
			fan *= 2;
		}

		if (has_rule(GameConstants_KWX.GAME_RULE_FENG_DING_16)) {
			if (player_liang[provide_index] == 1) {
				fan *= 2;
			}
		} else {
			if (player_liang[seat_index] == 0 && player_liang[provide_index] == 1) {
				fan *= 2;
			}
		}

		int fan_limit = 8;
		if (has_rule(GameConstants_KWX.GAME_RULE_FENG_DING_8)) {
			fan_limit = 8;
		}
		if (has_rule(GameConstants_KWX.GAME_RULE_FENG_DING_16)) {
			fan_limit = 16;
		}
		return fan > fan_limit ? fan_limit : fan;
	}

	// 杠牌分析 包括补杠
	public int analyse_gang_hong_zhong_all(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount, GangCardResult gangCardResult,
			boolean check_weave, int cards_abandoned_gang[], int seat_index, int send_card_data) {
		// 设置变量
		int cbActionMask = GameConstants.WIK_NULL;

		// 手上杠牌
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (cards_index[i] == 4) {
				if (player_liang[seat_index] == 1) {
					int[] temp_cards_index = Arrays.copyOf(cards_index, cards_index.length);
					temp_cards_index[i] = 0;
					int[] temp_hu_cards = new int[GameConstants_KWX.MAX_ZI_FENG];
					int hu_count = get_ting_card(temp_hu_cards, temp_cards_index, WeaveItem, cbWeaveCount, seat_index);

					boolean hu = true;
					if (hu_count == _playerStatus[seat_index]._hu_card_count) {
						for (int h = 0; h < hu_count; h++) {
							if (temp_hu_cards[h] != _playerStatus[seat_index]._hu_cards[h]) {
								hu = false;
								break;
							}
						}
					} else {
						hu = false;
					}
					if (!hu)
						continue;
				}

				cbActionMask |= GameConstants.WIK_GANG;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = _logic.switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;// 安刚
				gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
			}
		}

		if (check_weave) {
			// 组合杠牌，包括以前能杠，但是不杠，发牌之后而选择补杠的
			for (int i = 0; i < cbWeaveCount; i++) {
				if (WeaveItem[i].weave_kind == GameConstants.WIK_PENG) {
					for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
						if (player_liang[seat_index] == 1 && send_card_data != _logic.switch_to_card_data(j)) {
							continue;
						}
						if (cards_index[j] != 1 || cards_abandoned_gang[j] != 0) { // 红中不能杠，能接杠但是不杠的牌也过滤掉
							continue;
						} else {
							if (WeaveItem[i].center_card == _logic.switch_to_card_data(j)) {
								boolean check_gang = true;
								out: for (int p = 0; p < getTablePlayerNumber(); p++) {
									if (p == seat_index || player_liang[p] != 1) {
										continue;
									}

									for (int h = 0; h < _playerStatus[p]._hu_card_count; h++) {
										if (_playerStatus[p]._hu_cards[h] == _logic.switch_to_card_data(j)) {
											check_gang = false;
											break out;
										}
									}
								}
								if (check_gang) {
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
			}
		}

		return cbActionMask;
	}

	@Override
	public boolean exe_finish(int reason) {
		_end_reason = reason;
		if (_end_reason == GameConstants_KWX.Game_End_NORMAL || _end_reason == GameConstants_KWX.Game_End_DRAW
				|| _end_reason == GameConstants_KWX.Game_End_ROUND_OVER) {
			cost_dou = 0;
		}

		// TODO 操蛋的，在处理小结算数据之前，需要把_game_status设置成GAME_STATUS_WAIT
		_game_status = GameConstants_KWX.GS_MJ_XIAOHU;
		process_xiao_jie_suan(reason);

		set_handler(_handler_xiao_jie_suan);

		_handler_xiao_jie_suan.exe(this);

		return true;
	}

	/**
	 * 将小结算界面的room_response和game_end_responsed处理好之后保存到table的saved_room_response和
	 * saved_game_end_response里面，真正的游戏结束和小结算界面的断线重连的时候，直接取出来用就可以。
	 */
	public void process_xiao_jie_suan(int reason) {
		int real_reason = reason;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_ready[i] = 0;
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

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
					for (int k = 0; k < getTablePlayerNumber(); k++) {
						lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
					}
				}

				for (int j = 0; j < getTablePlayerNumber(); j++) {
					_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
				}
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				GRR._game_score[i] += lGangScore[i];
				GRR._game_score[i] += GRR._start_hu_score[i];

				_player_result.game_score[i] += GRR._game_score[i];
			}

			load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);

			game_end.setBankerPlayer(GRR._banker_player);
			game_end.setLeftCardCount(GRR._left_card_count);
			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

			for (int i = 0; i < GameConstants_KWX.MAX_LAI_ZI_PI_ZI_GANG && i < GRR._count_niao; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao[i]);
			}
			for (int i = 0; i < GameConstants_KWX.MAX_LAI_ZI_PI_ZI_GANG && i < GRR._count_niao_fei; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao_fei[i]);
			}
			game_end.setCountPickNiao(GRR._count_pick_niao + GRR._count_pick_niao_fei);// 中鸟个数

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					pnc.addItem(GRR._player_niao_cards[i][j]);
				}
				for (int j = 0; j < GRR._player_niao_count_fei[i]; j++) {
					pnc.addItem(GRR._player_niao_cards_fei[i][j]);
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
			long rv[] = new long[GameConstants_KWX.MAX_RIGHT_COUNT];

			set_result_describe();

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();

				WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
				for (int j = 0; j < GRR._weave_count[i]; j++) {
					WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
					weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
					weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
					weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
					weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);

					int[] weave_cards = new int[4];
					int count = _logic.get_weave_card_huangshi(GRR._weave_items[i][j].weave_kind, GRR._weave_items[i][j].center_card, weave_cards);
					for (int x = 0; x < count; x++) {
						if (_logic.is_magic_card(weave_cards[x]))
							weave_cards[x] += GameConstants_KWX.CARD_ESPECIAL_TYPE_LAI_ZI;

						weaveItem_item.addWeaveCard(weave_cards[x]);
					}

					if (GRR._weave_items[i][j].weave_kind == GameConstants_KWX.WIK_LIANG) {
						cs.addItem(GRR._weave_items[i][j].center_card);
						cs.addItem(GRR._weave_items[i][j].center_card);
						cs.addItem(GRR._weave_items[i][j].center_card);
						continue;
					}
					weaveItem_array.addWeaveItem(weaveItem_item);
				}
				game_end.addWeaveItemArray(weaveItem_array);

				for (int j = 0; j < GRR._card_count[i]; j++) {
					if (_logic.is_magic_card(GRR._cards_data[i][j])) {
						cs.addItem(GRR._cards_data[i][j] + GameConstants_KWX.CARD_ESPECIAL_TYPE_LAI_ZI);
					} else {
						cs.addItem(GRR._cards_data[i][j]);
					}
				}
				game_end.addCardsData(cs);

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
				for (int j = 0; j < getTablePlayerNumber(); j++) {
					lfs.addItem(GRR._lost_fan_shu[i][j]);
				}

				game_end.addLostFanShu(lfs);

			}
		}

		if (reason == GameConstants_KWX.Game_End_NORMAL || reason == GameConstants_KWX.Game_End_DRAW) {
			if (_cur_round >= _game_round && (!is_sys())) {
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(process_player_result(reason));
			} else {
			}
		} else if ((!is_sys()) && (reason == GameConstants_KWX.Game_End_RELEASE_PLAY || reason == GameConstants_KWX.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants_KWX.Game_End_RELEASE_RESULT || reason == GameConstants_KWX.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants_KWX.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants_KWX.Game_End_RELEASE_SYSTEM)) {
			real_reason = GameConstants_KWX.Game_End_DRAW;
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(process_player_result(reason));
		}
		game_end.setEndType(real_reason);

		roomResponse.setGameEnd(game_end);

		saved_room_response = roomResponse;
		saved_game_end_response = game_end;
	}

	@Override
	protected boolean on_handler_game_finish(int seat_index, int reason) {
		if (future != null) {
			future.cancel(true);
			future = null;
		}
		if (reason != GameConstants_KWX.Game_End_NORMAL && reason != GameConstants_KWX.Game_End_DRAW)
			process_xiao_jie_suan(reason);

		// TODO 操蛋的，在处理小结算数据之前，需要把_game_status设置成GAME_STATUS_WAIT
		_game_status = GameConstants_KWX.GS_MJ_XIAOHU;

		send_response_to_room(saved_room_response);

		record_game_round(saved_game_end_response);

		if (reason == GameConstants_KWX.Game_End_RELEASE_PLAY_TIME_OUT || reason == GameConstants_KWX.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < getTablePlayerNumber(); j++) {
				Player player = get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");
			}
		}

		boolean end = false;
		if (reason == GameConstants_KWX.Game_End_NORMAL || reason == GameConstants_KWX.Game_End_DRAW) {
			if (_cur_round >= _game_round && (!is_sys())) {
				end = true;
			}
		} else if ((!is_sys()) && (reason == GameConstants_KWX.Game_End_RELEASE_PLAY || reason == GameConstants_KWX.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants_KWX.Game_End_RELEASE_RESULT || reason == GameConstants_KWX.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants_KWX.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants_KWX.Game_End_RELEASE_SYSTEM)) {
			end = true;
		}

		if (end && (!is_sys())) {
			PlayerServiceImpl.getInstance().delRoomId(getRoom_id());
		}

		if (is_sys()) {
			clear_score_in_gold_room();
		}
		if (!is_sys()) {
			GRR = null;
		}
		return false;
	}

	@Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x19, 0x11, 0x11, 0x19 };
		int[] cards_of_player1 = new int[] { 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x19, 0x11, 0x11, 0x19 };
		int[] cards_of_player2 = new int[] { 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x19, 0x11, 0x11, 0x19 };
		int[] cards_of_player3 = new int[] { 0x12, 0x12, 0x12, 0x14, 0x14, 0x14, 0x19, 0x19, 0x27, 0x29, 0x29, 0x29, 0x19 };

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants_KWX.MAX_INDEX; j++) {
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
	public boolean trustee_timer(int operate_id, int seat_index) {
		return false;
	}

}
