package com.cai.game.mj;

import java.util.Arrays;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.RedisConstant;
import com.cai.common.define.ECardType;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.SpringService;
import com.cai.game.mj.handler.NewHandlerQiShou;
import com.cai.redis.service.RedisService;
import com.cai.service.MongoDBServiceImpl;

public abstract class NewAbstractMjTable extends AbstractMJTable {

	private static final long serialVersionUID = 1L;

	@SuppressWarnings("rawtypes")
	public NewHandlerQiShou _handler_qi_shou;

	public NewAbstractMjTable() {
		super(MJType.DEFAULT);
	}

	public NewAbstractMjTable(MJType mjType) {
		super(mjType);
	}

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
	 * 起手，进行Handler处理器的切换
	 * 
	 * @param seat_index
	 * @param type
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public boolean exe_qi_shou(int seat_index, int type) {
		set_handler(_handler_qi_shou);

		_handler_qi_shou.reset_status(seat_index, type);
		_handler_qi_shou.exe(this);

		return true;
	}

	@Override
	protected boolean on_handler_game_start() {
		reset_init_data();

		progress_banker_select();

		_game_status = GameConstants.GS_MJ_PLAY;

		GRR._banker_player = _current_player = _cur_banker;

		init_shuffle();

		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();

		getLocationTip();

		try {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				for (int j = 0; j < GRR._cards_index[i].length; j++) {
					if (GRR._cards_index[i][j] == 4) {
						MongoDBServiceImpl.getInstance().card_log(get_players()[i], ECardType.anLong, "", GRR._cards_index[i][j], 0l, getRoom_id());
					}
				}
			}
		} catch (Exception e) {
			logger.error("card_log", e);
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			istrustee[i] = false;
		}


		// 走的是新的游戏开始方法
		return on_game_start_new();
	}

	public abstract boolean on_game_start_new();

	@Override
	protected void init_shuffle() {
		_repertory_card = new int[mjType.getCardLength()];
		shuffle(_repertory_card, mjType.getCards());
	};

	@Override
	public void shuffle(int[] repertory_card, int[] mj_cards) {
		_all_card_len = repertory_card.length;

		GRR._left_card_count = _all_card_len;

		int xi_pai_count = 0;
		int rand = RandomUtil.generateRandomNumber(3, 6);

		while (xi_pai_count < 6 && xi_pai_count < rand) {
			if (xi_pai_count == 0)
				_logic.random_card_data(repertory_card, mj_cards);
			else
				_logic.random_card_data(repertory_card, repertory_card);

			xi_pai_count++;
		}

		if (this.getClass().getAnnotation(ThreeDimension.class) != null) {
			show_tou_zi(GRR._banker_player);
		}

		int send_count;
		int have_send_count = 0;

		int count = getTablePlayerNumber();

		for (int i = 0; i < count; i++) {
			// 庄家起手14张牌，闲家起手13张牌。和之前的有点区别。GameStart之后，不会再发牌。直接走新的Handler。
			if (i == GRR._banker_player) {
				send_count = GameConstants.MAX_COUNT;
			} else {
				send_count = (GameConstants.MAX_COUNT - 1);
			}

			GRR._left_card_count -= send_count;

			_logic.switch_to_cards_index(repertory_card, have_send_count, send_count, GRR._cards_index[i]);

			have_send_count += send_count;
		}

		if (_recordRoomRecord != null) {
			_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));
		}
	}

	/**
	 * 获取听牌数据
	 * 
	 * @param cards
	 * @param cards_index
	 * @param weaveItem
	 * @param cbWeaveCount
	 * @param seat_index
	 * @return
	 */
	public abstract int get_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index);

	/**
	 * 庄家起手14张的时候，胡牌分析。和之前的是有区别的， 没有当前牌数据。
	 * 
	 * @param cards_index
	 * @param weaveItems
	 * @param weave_count
	 * @param chiHuRight
	 * @param card_type
	 * @param seat_index
	 * @return
	 */
	public abstract int analyse_qi_shou_hu_pai(int cards_index[], WeaveItem weaveItems[], int weave_count, ChiHuRight chiHuRight, int card_type,
			int seat_index);

	@Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x13, 0x19, 0x23, 0x32, 0x26, 0x01, 0x02, 0x04, 0x17, 0x09, 0x26, 0x07, 0x24, 0x24 };
		int[] cards_of_player1 = new int[] { 0x01, 0x02, 0x03, 0x11, 0x11, 0x11, 0x19, 0x19, 0x19, 0x23, 0x23, 0x23, 0x24, 0x24 };
		int[] cards_of_player2 = new int[] { 0x01, 0x01, 0x03, 0x03, 0x05, 0x05, 0x07, 0x07, 0x09, 0x09, 0x11, 0x11, 0x12, 0x24 };
		int[] cards_of_player3 = new int[] { 0x01, 0x02, 0x02, 0x05, 0x06, 0x07, 0x09, 0x09, 0x23, 0x24, 0x25, 0x27, 0x28, 0x24 };

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

		if (cards_of_player0.length > 13) {
			// 庄家起手14张
			GRR._cards_index[GRR._banker_player][_logic.switch_to_card_index(cards_of_player0[13])]++;
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
	public void testRealyCard(int[] realyCards) {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}

		_repertory_card = realyCards;
		_all_card_len = _repertory_card.length;
		GRR._left_card_count = _all_card_len;

		int send_count;
		int have_send_count = 0;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 庄家起手14张牌，闲家起手13张牌。和之前的有点区别。GameStart之后，不会再发牌。直接走新的Handler。
			if (i == GRR._banker_player) {
				send_count = GameConstants.MAX_COUNT;
			} else {
				send_count = (GameConstants.MAX_COUNT - 1);
			}

			GRR._left_card_count -= send_count;

			_logic.switch_to_cards_index(_repertory_card, have_send_count, send_count, GRR._cards_index[i]);

			have_send_count += send_count;
		}

		DEBUG_CARDS_MODE = false;
		BACK_DEBUG_CARDS_MODE = false;
	}

	@Override
	public void testSameCard(int[] cards) {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < 13; j++) {
				GRR._cards_index[i][_logic.switch_to_card_index(cards[j])] += 1;
			}
		}

		if (cards.length > 13) {
			// 庄家起手14张
			GRR._cards_index[GRR._banker_player][_logic.switch_to_card_index(cards[13])]++;
		}

		DEBUG_CARDS_MODE = false;
		BACK_DEBUG_CARDS_MODE = false;
	}
}
