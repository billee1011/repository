package com.cai.game.mj.hubei.huangshi;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.RedisConstant;
import com.cai.common.constant.game.Constants_HuangShi;
import com.cai.common.define.ECardType;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.SpringService;
import com.cai.future.GameSchedule;
import com.cai.game.mj.MJType;
import com.cai.game.mj.NewAbstractMjTable;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.game.util.GameUtilConstants;
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

public class Table_HuangShi extends NewAbstractMjTable {
	private static final long serialVersionUID = -2323639016002945134L;

	public int da_dian_card;
	public int magic_card;

	public HandlerSelectMagic_HuangShi _handler_select_magic;
	public HandlerShowCard_HuangShi _handler_show_card;
	public HandlerXiaoJieSuan_HuangShi _handler_xiao_jie_suan;

	public int[] player_fan_shu = new int[getTablePlayerNumber()];
	public int[] player_zhong_fa_bai_weave_count = new int[getTablePlayerNumber()];
	public int[] player_hao_hua_count = new int[getTablePlayerNumber()]; // 豪华七对的数目，1个豪华1番

	public RoomResponse.Builder saved_room_response = null;
	public GameEndResponse.Builder saved_game_end_response = null;

	public boolean can_reconnect = false;

	public int[] tou_zi_dian_shu = new int[2]; // 用来储存2个骰子的点数

	public int time_for_animation = 2000; // 摇骰子的动画时间(ms)
	public int time_for_fade = 500; // 摇骰子动画的消散延时(ms)

	public int qi_pai_index = 0; // 起牌的索引位置，一共有4个牌堆

	public boolean can_win_but_without_enough_score = false; // 能胡，但是没达到起胡分

	public int auto_out_card_delay = 500; // 自动出牌的延迟时间
	public int action_wait_time = 3000; // 自动胡牌托管之后，如果有操作，等待3秒，3秒之后自动出牌
	public int auto_deal_win_delay = 500; // 自动胡牌的延迟时间

	// 接炮时，记录一下牌型分
	public float[] score_when_jie_pao_hu = new float[getTablePlayerNumber()];
	// 有接炮时，不胡，再次存储一下，胡牌时的牌型分，和score_when_abandoned_win进行比较
	public float[] score_when_abandoned_jie_pao = new float[getTablePlayerNumber()];

	// 有些子麻将，很重要的一个状态位，因为发牌之后，分析胡牌之后，再次获取听牌数据的时候，有些关键的数据不能重置
	public int analyse_state;
	public static final int FROM_TING = 1;
	public static final int NORMAL_STATE = 2;

	public Table_HuangShi() {
		super(MJType.GAME_TYPE_HU_BEI_HUANG_SHI);
	}

	@Override
	protected boolean on_handler_game_start() {
		tou_zi_dian_shu = new int[2];

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

	/**
	 * 在牌桌上显示摇骰子的效果
	 * 
	 * @param tou_zi_one
	 *            骰子1的点数
	 * @param tou_zi_two
	 *            骰子2的点数
	 * @param time_for_animate
	 *            动画时间
	 * @param time_for_fade
	 *            动画保留时间
	 * @return
	 */
	public boolean operate_tou_zi_effect(int tou_zi_one, int tou_zi_two, int time_for_animate, int time_for_fade) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_EFFECT_ACTION);
		roomResponse.setEffectType(GameConstants.Effect_Action_SHAI_ZI);
		if (GRR != null)
			roomResponse.setTarget(GRR._banker_player);
		else
			roomResponse.setTarget(0);
		roomResponse.setEffectCount(2);
		roomResponse.addEffectsIndex(tou_zi_one);
		roomResponse.addEffectsIndex(tou_zi_two);
		roomResponse.setEffectTime(time_for_animate);
		roomResponse.setStandTime(time_for_fade);

		send_response_to_room(roomResponse);

		if (GRR != null)
			GRR.add_room_response(roomResponse);

		return true;
	}

	/**
	 * 根据2个骰子的点数和当前赢家的位置，确定起拍的位置
	 * 
	 * @param tou_zi_one
	 * @param tou_zi_two
	 * @return
	 */
	public int get_qi_pai_player(int tou_zi_one, int tou_zi_two) {
		int banker_player = 0;
		if (GRR != null)
			banker_player = GRR._banker_player;

		qi_pai_index = (banker_player + (tou_zi_one + tou_zi_two - 1) % 4) % 4;

		return qi_pai_index;
		// return get_seat(tou_zi_one + tou_zi_two, banker_player);
	}

	/**
	 * 后期需要添加的摇骰子的效果
	 * 
	 * @param table
	 * @param seat_index
	 */
	public void show_tou_zi(int seat_index) {
		tou_zi_dian_shu[0] = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % 6) + 1;
		tou_zi_dian_shu[1] = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % 6) + 1;

		operate_tou_zi_effect(tou_zi_dian_shu[0], tou_zi_dian_shu[1], time_for_animation, time_for_fade);
	}

	@Override
	protected void init_shuffle() {
		_repertory_card = new int[mjType.getCardLength()];
		shuffle(_repertory_card, mjType.getCards());
	};

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

		// TODO 洗完牌之后，发牌之前，需要摇骰子
		qi_pai_index = 0;
		show_tou_zi(GRR._banker_player);

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

	public boolean check_zhong_fa_bai(int[] cards_index, int seat_index) {
		boolean result = false;
		if (cards_index[Constants_HuangShi.HONG_ZHONG_INDEX] > 0 && cards_index[Constants_HuangShi.FA_CAI_INDEX] > 0
				&& cards_index[Constants_HuangShi.BAI_BAN_INDEX] > 0) {
			result = true;

			if (cards_index[Constants_HuangShi.HONG_ZHONG_INDEX] >= 4 && cards_index[Constants_HuangShi.FA_CAI_INDEX] >= 4
					&& cards_index[Constants_HuangShi.BAI_BAN_INDEX] >= 4)
				player_zhong_fa_bai_weave_count[seat_index] = 4;
			else if (cards_index[Constants_HuangShi.HONG_ZHONG_INDEX] >= 3 && cards_index[Constants_HuangShi.FA_CAI_INDEX] >= 3
					&& cards_index[Constants_HuangShi.BAI_BAN_INDEX] >= 3)
				player_zhong_fa_bai_weave_count[seat_index] = 3;
			else if (cards_index[Constants_HuangShi.HONG_ZHONG_INDEX] >= 2 && cards_index[Constants_HuangShi.FA_CAI_INDEX] >= 2
					&& cards_index[Constants_HuangShi.BAI_BAN_INDEX] >= 2)
				player_zhong_fa_bai_weave_count[seat_index] = 2;
			else if (cards_index[Constants_HuangShi.HONG_ZHONG_INDEX] >= 1 && cards_index[Constants_HuangShi.FA_CAI_INDEX] >= 1
					&& cards_index[Constants_HuangShi.BAI_BAN_INDEX] >= 1)
				player_zhong_fa_bai_weave_count[seat_index] = 1;
		}
		return result;
	}

	public int get_player_fan_shu(int seat_index) {
		int fan_shu = 0;

		boolean can_fa_cai_gang = has_rule(Constants_HuangShi.GAME_RULE_HONG_ZHONG_FA_CAI_GANG);

		if (GRR != null) {
			for (int x = 0; x < GRR._player_niao_count[seat_index]; x++) {
				if (_logic.is_magic_card(GRR._lai_zi_pi_zi_gang[seat_index][x]))
					fan_shu += 2;
				else
					fan_shu += 1;
			}

			fan_shu += GRR._gang_score[seat_index].ming_gang_count + GRR._gang_score[seat_index].an_gang_count * 2;

			// TODO 暗杠不算开口
			int weave_count = get_effective_weave_count(seat_index);
			if (weave_count == 3)
				fan_shu += 1;
			else if (weave_count == 4)
				fan_shu += 2;

			for (int counter = 0; counter < GRR._weave_count[seat_index]; counter++) {
				if (GRR._weave_items[seat_index][counter].weave_kind == GameConstants.WIK_SHOW_CARD) {
					if (can_fa_cai_gang) {
						if (magic_card == Constants_HuangShi.BAI_BAN_CARD) {
							fan_shu += 5;
						} else {
							fan_shu += 3;
						}
					} else {
						if (magic_card == Constants_HuangShi.BAI_BAN_CARD || magic_card == Constants_HuangShi.FA_CAI_CARD) {
							fan_shu += 4;
						} else {
							fan_shu += 2;
						}
					}
				}
			}
		}

		return fan_shu;
	}

	public int get_effective_weave_count(int seat_index) {
		int weave_count = 0;

		for (int i = 0; i < GRR._weave_count[seat_index]; i++) {
			// 黄石麻将，亮的中发白，不算开口
			if (GRR._weave_items[seat_index][i].weave_kind == GameConstants.WIK_SHOW_CARD) {
				continue;
			}
			if (!(GRR._weave_items[seat_index][i].weave_kind == GameConstants.WIK_GANG && GRR._weave_items[seat_index][i].public_card == 0)) {
				weave_count++;
			}
		}

		return weave_count;
	}

	@Override
	public void load_player_info_data(RoomResponse.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
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
			room_player.setPao(_player_result.pao[i]);
			room_player.setNao(_player_result.nao[i]);
			room_player.setQiang(_player_result.qiang[i]);
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			room_player.setHasPiao(_player_result.haspiao[i]);
			// room_player.setBiaoyan(_player_result.biaoyan[i]);
			room_player.setBiaoyan(get_player_fan_shu(i));

			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}

			roomResponse.addPlayers(room_player);
		}
	}

	@Override
	public boolean exe_finish(int reason) {
		_end_reason = reason;
		if (_end_reason == GameConstants.Game_End_NORMAL || _end_reason == GameConstants.Game_End_DRAW
				|| _end_reason == GameConstants.Game_End_ROUND_OVER) {
			cost_dou = 0;
		}

		// TODO 操蛋的，在处理小结算数据之前，需要把_game_status设置成GAME_STATUS_WAIT
		_game_status = GameConstants.GAME_STATUS_WAIT;
		process_xiao_jie_suan(reason);

		set_handler(_handler_xiao_jie_suan);

		_handler_xiao_jie_suan.exe(this);

		return true;
	}

	@Override
	public void load_common_status(RoomResponse.Builder roomResponse) {
		roomResponse.setCurrentPlayer(_current_player == GameConstants.INVALID_SEAT ? _resume_player : _current_player);

		if (GRR != null) {
			roomResponse.setLeftCardCount(GRR._left_card_count);

			// TODO 注意这里固定用两个特殊牌值，一个是癞子，一个是翻出来的那张牌
			for (int i = 0; i < 2; i++) {
				roomResponse.addEspecialShowCards(GRR._especial_show_cards[i]);
			}

			if (GRR._especial_txt != "") {
				roomResponse.setEspecialTxt(GRR._especial_txt);
				roomResponse.setEspecialTxtType(GRR._especial_txt_type);
			}
		}

		roomResponse.setGameStatus(_game_status);

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			roomResponse.addCardStatus(_playerStatus[i]._card_status);
			roomResponse.addPlayerStatus(_playerStatus[i].get_status());
		}
	}

	@Override
	public boolean operate_player_cards(int seat_index, int card_count, int cards[], int weave_count, WeaveItem weaveitems[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS);// 12更新玩家手牌数据
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setScoreType(get_player_fan_shu(seat_index));
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
				if (weaveitems[j].weave_kind == GameConstants.WIK_GANG && weaveitems[j].public_card == 0) {
					weaveItem_item.setCenterCard(0);

					for (int x = 0; x < 4; x++) {
						weaveItem_item.addWeaveCard(-1);
					}
				} else {
					weaveItem_item.setCenterCard(weaveitems[j].center_card);

					int[] weave_cards = new int[4];
					int count = _logic.get_weave_card_huangshi(weaveitems[j].weave_kind, weaveitems[j].center_card, weave_cards);
					for (int x = 0; x < count; x++) {
						if (_logic.is_magic_card(weave_cards[x]))
							weave_cards[x] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;

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
						weave_cards[x] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;

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
		roomResponse.setScoreType(get_player_fan_shu(seat_index));
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

				if (weaveitems[j].weave_kind == GameConstants.WIK_GANG && weaveitems[j].public_card == 0) {
					weaveItem_item.setCenterCard(0);

					for (int x = 0; x < 4; x++) {
						weaveItem_item.addWeaveCard(-1);
					}
				} else {
					weaveItem_item.setCenterCard(weaveitems[j].center_card);

					int[] weave_cards = new int[4];
					int count = _logic.get_weave_card_huangshi(weaveitems[j].weave_kind, weaveitems[j].center_card, weave_cards);
					for (int x = 0; x < count; x++) {
						if (_logic.is_magic_card(weave_cards[x]))
							weave_cards[x] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;

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
						weave_cards[x] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;

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

			if (out_card == magic_card) {
				roomResponse.addOutCardTing(out_card + GameConstants.CARD_ESPECIAL_TYPE_NEW_TING + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
			} else if (out_card == Constants_HuangShi.HONG_ZHONG_CARD) {
				roomResponse.addOutCardTing(out_card + GameConstants.CARD_ESPECIAL_TYPE_NEW_TING + GameConstants.CARD_ESPECIAL_TYPE_GANG);
			} else if (out_card == Constants_HuangShi.FA_CAI_CARD && has_rule(Constants_HuangShi.GAME_RULE_HONG_ZHONG_FA_CAI_GANG)) {
				roomResponse.addOutCardTing(out_card + GameConstants.CARD_ESPECIAL_TYPE_NEW_TING + GameConstants.CARD_ESPECIAL_TYPE_GANG);
			} else {
				roomResponse.addOutCardTing(out_card + GameConstants.CARD_ESPECIAL_TYPE_NEW_TING);
			}

			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < ting_card_cout; j++) {
				int card = _playerStatus[seat_index]._hu_out_cards[i][j];
				if (_logic.is_magic_card(card))
					card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				int_array.addItem(card);
			}
			roomResponse.addOutCardTingCards(int_array);
		}

		send_response_to_player(seat_index, roomResponse);

		GRR.add_room_response(roomResponse);

		return true;
	}

	@Override
	public void process_chi_hu_player_operate(int seat_index, int operate_card, boolean rm) {
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		int effect_count = chr.type_count;
		long effect_indexs[] = new long[effect_count];
		for (int i = 0; i < effect_count; i++) {
			effect_indexs[i] = chr.type_list[i];
		}

		operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, effect_count, effect_indexs, 1, GameConstants.INVALID_SEAT);

		operate_player_cards(seat_index, 0, null, 0, null);

		if (rm) {
			GRR._cards_index[seat_index][_logic.switch_to_card_index(operate_card)]--;
		}

		boolean can_fa_cai_gang = has_rule(Constants_HuangShi.GAME_RULE_HONG_ZHONG_FA_CAI_GANG);
		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = _logic.switch_to_cards_data_huangshi(GRR._cards_index[seat_index], cards, can_fa_cai_gang);

		for (int i = 0; i < hand_card_count; i++) {
			if (_logic.is_magic_card(cards[i]))
				cards[i] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
		}

		cards[hand_card_count] = operate_card + GameConstants.CARD_ESPECIAL_TYPE_HU;
		hand_card_count++;

		operate_show_card(seat_index, GameConstants.Show_Card_HU, hand_card_count, cards, GameConstants.INVALID_SEAT);

		return;
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
				// for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
				// for (int k = 0; k < getTablePlayerNumber(); k++) {
				// lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
				// }
				// }

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

			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);
			game_end.setLeftCardCount(GRR._left_card_count);
			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

			for (int i = 0; i < GameConstants.MAX_LAI_ZI_PI_ZI_GANG && i < GRR._count_niao; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao[i]);
			}
			for (int i = 0; i < GameConstants.MAX_LAI_ZI_PI_ZI_GANG && i < GRR._count_niao_fei; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao_fei[i]);
			}
			game_end.setCountPickNiao(GRR._count_pick_niao + GRR._count_pick_niao_fei);// 中鸟个数

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				// 鸟牌，必须按四人计算
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					pnc.addItem(GRR._lai_zi_pi_zi_gang[i][j]);
				}
				for (int j = 0; j < GRR._player_niao_count_fei[i]; j++) {
					pnc.addItem(GRR._player_niao_cards_fei[i][j]);
				}
				game_end.addPlayerNiaoCards(pnc);
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
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

			set_result_describe();

			boolean can_fa_cai_gang = has_rule(Constants_HuangShi.GAME_RULE_HONG_ZHONG_FA_CAI_GANG);
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				GRR._card_count[i] = _logic.switch_to_cards_data_huangshi(GRR._cards_index[i], GRR._cards_data[i], can_fa_cai_gang);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {
					if (_logic.is_magic_card(GRR._cards_data[i][j])) {
						cs.addItem(GRR._cards_data[i][j] + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
					} else {
						cs.addItem(GRR._cards_data[i][j]);
					}
				}
				game_end.addCardsData(cs);

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
							weave_cards[x] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;

						weaveItem_item.addWeaveCard(weave_cards[x]);
					}

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
				for (int j = 0; j < getTablePlayerNumber(); j++) {
					lfs.addItem(GRR._lost_fan_shu[i][j]);
				}

				game_end.addLostFanShu(lfs);

			}

		}

		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round && (!is_sys())) {
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(process_player_result(reason));
			} else {
			}
		} else if ((!is_sys()) && (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM)) {
			real_reason = GameConstants.Game_End_DRAW;
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
		if (reason != GameConstants.Game_End_NORMAL && reason != GameConstants.Game_End_DRAW)
			process_xiao_jie_suan(reason);

		send_response_to_room(saved_room_response);

		record_game_round(saved_game_end_response);

		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT || reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < getTablePlayerNumber(); j++) {
				Player player = get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");
			}
		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round && (!is_sys())) {
				end = true;
			}
		} else if ((!is_sys()) && (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM)) {
			end = true;
		}

		if (end == false)
			can_reconnect = true;

		if (end && (!is_sys())) {
			PlayerServiceImpl.getInstance().delRoomId(getRoom_id());
		}

		if (is_sys()) {
			clear_score_in_gold_room();
		}

		return false;
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {
		return 0;
	}

	public float analyse_da_hu_first_qi_shou(int[] cards_index, WeaveItem[] weaveItems, int weave_count, ChiHuRight chiHuRight, int card_type,
			int seat_index) {
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// TODO: 手里有红中不能胡牌
		if (cbCardIndexTemp[Constants_HuangShi.HONG_ZHONG_INDEX] > 0)
			return 0;

		// TODO 红中发财杠时，发财也是固定杠牌
		if (has_rule(Constants_HuangShi.GAME_RULE_HONG_ZHONG_FA_CAI_GANG)) {
			if (cbCardIndexTemp[Constants_HuangShi.FA_CAI_INDEX] > 0) {
				return 0;
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

		boolean can_win_with_magic = AnalyseCardUtil.analyse_win_by_cards_index(cbCardIndexTemp, -1, magic_cards_index, magic_card_count); // 带癞子时能胡
		boolean can_win_without_magic = AnalyseCardUtil.analyse_win_by_cards_index(cbCardIndexTemp, -1, magic_cards_index, 0); // 癞子牌还原之后能胡或者手牌没癞子牌能胡

		boolean can_win_qi_dui_with_magic = _logic.check_hubei_qi_xiao_dui_qi_shou(cards_index, weaveItems, weave_count); // 带癞子时能胡七对
		boolean can_win_qi_dui_without_magic = _logic.check_hubei_ying_qi_xiao_dui_qi_shou(cards_index, weaveItems, weave_count); // 硬七对
		if (can_win_qi_dui_with_magic)
			chiHuRight.opr_or(Constants_HuangShi.CHR_QI_DUI);

		if (analyse_state == NORMAL_STATE) {
			// TODO 能胡七对时，统计豪华七对的数目
			player_hao_hua_count[seat_index] = 0;
			if (can_win_qi_dui_with_magic)
				player_hao_hua_count[seat_index] = get_hao_hua_count(cbCardIndexTemp);
		}

		boolean can_win_jiang_yi_se_with_magic = _logic.check_hubei_jiang_yi_se(cbCardIndexTemp, weaveItems, weave_count); // 将一色，不用一对牌+3n，也可以胡牌
		boolean can_win_jiang_yi_se_without_magic = _logic.check_hubei_ying_jiang_yi_se(cbCardIndexTemp, weaveItems, weave_count); // 硬将一色
		if (can_win_jiang_yi_se_with_magic)
			chiHuRight.opr_or(Constants_HuangShi.CHR_JIANG_YI_SE);

		boolean can_win = can_win_with_magic || can_win_qi_dui_with_magic || can_win_jiang_yi_se_with_magic; // 能胡牌

		if (can_win == false) {
			chiHuRight.set_empty();
			return 0;
		}

		boolean can_win_qing_yi_se_with_magic = _logic.check_hubei_qing_yi_se(cbCardIndexTemp, weaveItems, weave_count); // 清一色
		boolean can_win_qing_yi_se_without_magic = _logic.check_hubei_ying_qing_yi_se(cbCardIndexTemp, weaveItems, weave_count)
				&& can_win_without_magic; // 硬清一色
		if (can_win_qing_yi_se_with_magic)
			chiHuRight.opr_or(Constants_HuangShi.CHR_QING_YI_SE);

		boolean can_peng_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index(cbCardIndexTemp, -1, magic_cards_index, magic_card_count);
		boolean can_ying_peng_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index(cards_index, -1, magic_cards_index, 0);

		boolean exist_eat = _logic.exist_eat_hubei(weaveItems, weave_count);

		boolean can_win_peng_peng_hu_with_magic = can_peng_hu && !exist_eat; // 碰碰胡
		boolean can_win_peng_peng_hu_without_magic = can_ying_peng_hu && !exist_eat; // 硬碰碰胡
		if (can_win_peng_peng_hu_with_magic)
			chiHuRight.opr_or(Constants_HuangShi.CHR_PENG_PENG_HU);

		boolean has_big_win = false;
		if (can_win_qi_dui_with_magic || can_win_jiang_yi_se_with_magic || can_win_qing_yi_se_with_magic || can_win_peng_peng_hu_with_magic) {
			has_big_win = true;

			boolean can_ying_hu = true;

			if (can_win_qi_dui_with_magic && can_win_qi_dui_without_magic == false) // 能胡七对但是不能胡硬七对
				can_ying_hu = false;
			if (can_win_jiang_yi_se_with_magic && can_win_jiang_yi_se_without_magic == false) // 能胡将一色但是不能胡硬将一色
				can_ying_hu = false;
			if (can_win_qing_yi_se_with_magic && can_win_qing_yi_se_without_magic == false) // 能胡清一色但是不能胡硬清一色
				can_ying_hu = false;
			if (can_win_peng_peng_hu_with_magic && can_win_peng_peng_hu_without_magic == false) // 能胡碰碰胡但是不能胡硬碰碰胡
				can_ying_hu = false;

			if (can_ying_hu)
				chiHuRight.opr_or(Constants_HuangShi.CHR_YING_HU);
			else
				chiHuRight.opr_or(Constants_HuangShi.CHR_RUAN_HU);
		} else {
			if (can_win_without_magic)
				chiHuRight.opr_or(Constants_HuangShi.CHR_YING_HU);
			else
				chiHuRight.opr_or(Constants_HuangShi.CHR_RUAN_HU);
		}

		int tmp_card_count = _logic.get_card_count_by_index(cbCardIndexTemp);
		if (tmp_card_count == 2) {
			chiHuRight.opr_or(Constants_HuangShi.CHR_QUAN_QIU_REN);
			has_big_win = true;
		}

		int magic_count = _logic.magic_count(cbCardIndexTemp);
		if (magic_count >= 2 && has_big_win == false) {
			chiHuRight.set_empty();
			return 0;
		}

		boolean zi_mo = false;
		if (card_type == Constants_HuangShi.HU_CARD_TYPE_ZI_MO) {
			chiHuRight.opr_or(Constants_HuangShi.CHR_ZI_MO);
			zi_mo = true;
		} else {
			chiHuRight.opr_or(Constants_HuangShi.CHR_JIE_PAO);
		}

		int effective_weave_count = get_effective_weave_count(seat_index);

		if (zi_mo && !has_big_win) {
			if (effective_weave_count == 0) {
				chiHuRight.opr_or(Constants_HuangShi.CHR_DA_DAO);
			} else {
				chiHuRight.opr_or(Constants_HuangShi.CHR_XIAO_DAO);
			}
		}

		return get_tmp_score(chiHuRight, seat_index, seat_index, zi_mo);
	}

	public float analyse_ying_hu_first_qi_shou(int[] cards_index, WeaveItem[] weaveItems, int weave_count, ChiHuRight chiHuRight, int card_type,
			int seat_index) {
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// TODO: 手里有红中不能胡牌
		if (cbCardIndexTemp[Constants_HuangShi.HONG_ZHONG_INDEX] > 0)
			return 0;

		// TODO 红中发财杠时，发财也是固定杠牌
		if (has_rule(Constants_HuangShi.GAME_RULE_HONG_ZHONG_FA_CAI_GANG)) {
			if (cbCardIndexTemp[Constants_HuangShi.FA_CAI_INDEX] > 0) {
				return 0;
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

		boolean can_win_with_magic = AnalyseCardUtil.analyse_win_by_cards_index(cbCardIndexTemp, -1, magic_cards_index, magic_card_count); // 带癞子时能胡
		boolean can_win_without_magic = AnalyseCardUtil.analyse_win_by_cards_index(cbCardIndexTemp, -1, magic_cards_index, 0); // 癞子牌还原之后能胡或者手牌没癞子牌能胡

		boolean can_win_qi_dui_with_magic = _logic.check_hubei_qi_xiao_dui_qi_shou(cards_index, weaveItems, weave_count); // 带癞子时能胡七对
		boolean can_win_qi_dui_without_magic = _logic.check_hubei_ying_qi_xiao_dui_qi_shou(cards_index, weaveItems, weave_count); // 硬七对

		if (analyse_state == NORMAL_STATE) {
			// TODO 能胡七对时，统计豪华七对的数目
			player_hao_hua_count[seat_index] = 0;
			if (can_win_qi_dui_with_magic)
				player_hao_hua_count[seat_index] = get_hao_hua_count(cbCardIndexTemp);
		}

		boolean can_win_jiang_yi_se_with_magic = _logic.check_hubei_jiang_yi_se(cbCardIndexTemp, weaveItems, weave_count); // 将一色，不用一对牌+3n，也可以胡牌
		boolean can_win_jiang_yi_se_without_magic = _logic.check_hubei_ying_jiang_yi_se(cbCardIndexTemp, weaveItems, weave_count); // 硬将一色

		boolean can_win = can_win_with_magic || can_win_qi_dui_with_magic || can_win_jiang_yi_se_with_magic; // 能胡牌

		if (can_win == false) {
			chiHuRight.set_empty();
			return 0;
		}

		boolean can_win_qing_yi_se_with_magic = _logic.check_hubei_qing_yi_se(cbCardIndexTemp, weaveItems, weave_count); // 清一色
		boolean can_win_qing_yi_se_without_magic = _logic.check_hubei_ying_qing_yi_se(cbCardIndexTemp, weaveItems, weave_count)
				&& can_win_without_magic; // 硬清一色

		boolean can_peng_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index(cbCardIndexTemp, -1, magic_cards_index, magic_card_count);
		boolean can_ying_peng_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index(cards_index, -1, magic_cards_index, 0);

		boolean exist_eat = _logic.exist_eat_hubei(weaveItems, weave_count);

		boolean can_win_peng_peng_hu_with_magic = can_peng_hu && !exist_eat; // 碰碰胡
		boolean can_win_peng_peng_hu_without_magic = can_ying_peng_hu && !exist_eat; // 硬碰碰胡

		boolean has_big_win = false;

		if (can_win_qi_dui_without_magic || can_win_jiang_yi_se_without_magic || can_win_qing_yi_se_without_magic
				|| can_win_peng_peng_hu_without_magic) {
			chiHuRight.opr_or(Constants_HuangShi.CHR_YING_HU);

			if (can_win_qi_dui_without_magic) {
				chiHuRight.opr_or(Constants_HuangShi.CHR_QI_DUI);
				has_big_win = true;
			}
			if (can_win_jiang_yi_se_without_magic) {
				chiHuRight.opr_or(Constants_HuangShi.CHR_JIANG_YI_SE);
				has_big_win = true;
			}
			if (can_win_qing_yi_se_without_magic) {
				chiHuRight.opr_or(Constants_HuangShi.CHR_QING_YI_SE);
				has_big_win = true;
			}
			if (can_win_peng_peng_hu_without_magic) {
				chiHuRight.opr_or(Constants_HuangShi.CHR_PENG_PENG_HU);
				has_big_win = true;
			}
		} else if (can_win_without_magic) {
			chiHuRight.opr_or(Constants_HuangShi.CHR_YING_HU);
		} else {
			chiHuRight.opr_or(Constants_HuangShi.CHR_RUAN_HU);

			if (can_win_qi_dui_with_magic) {
				chiHuRight.opr_or(Constants_HuangShi.CHR_QI_DUI);
				has_big_win = true;
			}
			if (can_win_jiang_yi_se_with_magic) {
				chiHuRight.opr_or(Constants_HuangShi.CHR_JIANG_YI_SE);
				has_big_win = true;
			}
			if (can_win_qing_yi_se_with_magic) {
				chiHuRight.opr_or(Constants_HuangShi.CHR_QING_YI_SE);
				has_big_win = true;
			}
			if (can_win_peng_peng_hu_with_magic) {
				chiHuRight.opr_or(Constants_HuangShi.CHR_PENG_PENG_HU);
				has_big_win = true;
			}
		}

		int tmp_card_count = _logic.get_card_count_by_index(cbCardIndexTemp);
		if (tmp_card_count == 2) {
			chiHuRight.opr_or(Constants_HuangShi.CHR_QUAN_QIU_REN);
			has_big_win = true;
		}

		int magic_count = _logic.magic_count(cbCardIndexTemp);
		if (magic_count >= 2 && has_big_win == false) {
			chiHuRight.set_empty();
			return 0;
		}

		boolean zi_mo = false;
		if (card_type == Constants_HuangShi.HU_CARD_TYPE_ZI_MO) {
			chiHuRight.opr_or(Constants_HuangShi.CHR_ZI_MO);
			zi_mo = true;
		} else {
			chiHuRight.opr_or(Constants_HuangShi.CHR_JIE_PAO);
		}

		int effective_weave_count = get_effective_weave_count(seat_index);

		if (zi_mo && !has_big_win) {
			if (effective_weave_count == 0) {
				chiHuRight.opr_or(Constants_HuangShi.CHR_DA_DAO);
			} else {
				chiHuRight.opr_or(Constants_HuangShi.CHR_XIAO_DAO);
			}
		}

		return get_tmp_score(chiHuRight, seat_index, seat_index, zi_mo);
	}

	@Override
	public int analyse_qi_shou_hu_pai(int[] cards_index, WeaveItem[] weaveItems, int weave_count, ChiHuRight chiHuRight, int card_type,
			int seat_index) {
		can_win_but_without_enough_score = false;

		ChiHuRight chr = new ChiHuRight();
		chr.set_empty();
		float tmp_score = analyse_da_hu_first_qi_shou(cards_index, weaveItems, weave_count, chr, card_type, seat_index);

		if (analyse_state == NORMAL_STATE) {
			GRR._chi_hu_rights[seat_index] = chr;
		} else {
			chiHuRight = chr;
		}

		chr = new ChiHuRight();
		chr.set_empty();
		float tmp_score_2 = analyse_ying_hu_first_qi_shou(cards_index, weaveItems, weave_count, chr, card_type, seat_index);

		if (tmp_score_2 > tmp_score) {
			tmp_score = tmp_score_2;

			if (analyse_state == NORMAL_STATE) {
				GRR._chi_hu_rights[seat_index] = chr;
			} else {
				chiHuRight = chr;
			}
		}

		if (tmp_score < get_min_score()) {
			if (tmp_score > 0) {
				can_win_but_without_enough_score = true;
			}

			if (analyse_state == NORMAL_STATE) {
				GRR._chi_hu_rights[seat_index].set_empty();
			} else {
				chiHuRight.set_empty();
			}
			return GameConstants.WIK_NULL;
		}

		if (card_type == Constants_HuangShi.HU_CARD_TYPE_JIE_PAO) {
			score_when_jie_pao_hu[seat_index] = tmp_score;
		}

		return GameConstants.WIK_CHI_HU;
	}

	public float analyse_da_hu_first_new(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight,
			int card_type, int seat_index, int provide_index) {
		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		// TODO: 手里有红中不能胡牌
		if (cbCardIndexTemp[Constants_HuangShi.HONG_ZHONG_INDEX] > 0)
			return GameConstants.WIK_NULL;

		// TODO 红中发财杠时，发财也是固定杠牌
		if (has_rule(Constants_HuangShi.GAME_RULE_HONG_ZHONG_FA_CAI_GANG)) {
			if (cbCardIndexTemp[Constants_HuangShi.FA_CAI_INDEX] > 0) {
				return GameConstants.WIK_NULL;
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

		boolean can_win_with_magic = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card), magic_cards_index,
				magic_card_count); // 带癞子时能胡
		boolean can_win_without_magic = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
				magic_cards_index, 0); // 癞子牌还原之后能胡或者手牌没癞子牌能胡

		boolean can_win_qi_dui_with_magic = _logic.check_hubei_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card); // 带癞子时能胡七对
		boolean can_win_qi_dui_without_magic = _logic.check_hubei_ying_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card); // 硬七对
		if (can_win_qi_dui_with_magic)
			chiHuRight.opr_or(Constants_HuangShi.CHR_QI_DUI);

		if (analyse_state == NORMAL_STATE) {
			// TODO 能胡七对时，统计豪华七对的数目
			player_hao_hua_count[seat_index] = 0;
			if (can_win_qi_dui_with_magic)
				player_hao_hua_count[seat_index] = get_hao_hua_count(cbCardIndexTemp);
		}

		boolean can_win_jiang_yi_se_with_magic = _logic.check_hubei_jiang_yi_se(cbCardIndexTemp, weaveItems, weave_count); // 将一色，不用一对牌+3n，也可以胡牌
		boolean can_win_jiang_yi_se_without_magic = _logic.check_hubei_ying_jiang_yi_se(cbCardIndexTemp, weaveItems, weave_count); // 硬将一色
		if (can_win_jiang_yi_se_with_magic)
			chiHuRight.opr_or(Constants_HuangShi.CHR_JIANG_YI_SE);

		boolean can_win = can_win_with_magic || can_win_qi_dui_with_magic || can_win_jiang_yi_se_with_magic; // 能胡牌

		if (can_win == false) {
			chiHuRight.set_empty();
			return 0;
		}

		boolean can_win_qing_yi_se_with_magic = _logic.check_hubei_qing_yi_se(cbCardIndexTemp, weaveItems, weave_count); // 清一色
		boolean can_win_qing_yi_se_without_magic = _logic.check_hubei_ying_qing_yi_se(cbCardIndexTemp, weaveItems, weave_count)
				&& can_win_without_magic; // 硬清一色
		if (can_win_qing_yi_se_with_magic)
			chiHuRight.opr_or(Constants_HuangShi.CHR_QING_YI_SE);

		boolean can_peng_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card), magic_cards_index,
				magic_card_count);
		boolean can_ying_peng_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
				magic_cards_index, 0);

		boolean exist_eat = _logic.exist_eat_hubei(weaveItems, weave_count);

		boolean can_win_peng_peng_hu_with_magic = can_peng_hu && !exist_eat; // 碰碰胡
		boolean can_win_peng_peng_hu_without_magic = can_ying_peng_hu && !exist_eat; // 硬碰碰胡
		if (can_win_peng_peng_hu_with_magic)
			chiHuRight.opr_or(Constants_HuangShi.CHR_PENG_PENG_HU);

		boolean has_big_win = false;
		if (can_win_qi_dui_with_magic || can_win_jiang_yi_se_with_magic || can_win_qing_yi_se_with_magic || can_win_peng_peng_hu_with_magic) {
			has_big_win = true;

			boolean can_ying_hu = true;

			if (can_win_qi_dui_with_magic && can_win_qi_dui_without_magic == false) // 能胡七对但是不能胡硬七对
				can_ying_hu = false;
			if (can_win_jiang_yi_se_with_magic && can_win_jiang_yi_se_without_magic == false) // 能胡将一色但是不能胡硬将一色
				can_ying_hu = false;
			if (can_win_qing_yi_se_with_magic && can_win_qing_yi_se_without_magic == false) // 能胡清一色但是不能胡硬清一色
				can_ying_hu = false;
			if (can_win_peng_peng_hu_with_magic && can_win_peng_peng_hu_without_magic == false) // 能胡碰碰胡但是不能胡硬碰碰胡
				can_ying_hu = false;

			if (can_ying_hu)
				chiHuRight.opr_or(Constants_HuangShi.CHR_YING_HU);
			else
				chiHuRight.opr_or(Constants_HuangShi.CHR_RUAN_HU);
		} else {
			if (can_win_without_magic)
				chiHuRight.opr_or(Constants_HuangShi.CHR_YING_HU);
			else
				chiHuRight.opr_or(Constants_HuangShi.CHR_RUAN_HU);
		}

		int tmp_card_count = _logic.get_card_count_by_index(cbCardIndexTemp);
		if (tmp_card_count == 2) {
			chiHuRight.opr_or(Constants_HuangShi.CHR_QUAN_QIU_REN);
			has_big_win = true;
		}

		int magic_count = _logic.magic_count(cbCardIndexTemp);
		if (magic_count >= 2 && has_big_win == false) {
			chiHuRight.set_empty();
			return 0;
		}

		boolean zi_mo = false;
		if (card_type == Constants_HuangShi.HU_CARD_TYPE_ZI_MO) {
			chiHuRight.opr_or(Constants_HuangShi.CHR_ZI_MO);
			zi_mo = true;
		} else {
			chiHuRight.opr_or(Constants_HuangShi.CHR_JIE_PAO);
		}

		int effective_weave_count = get_effective_weave_count(seat_index);

		if (zi_mo && !has_big_win) {
			if (effective_weave_count == 0) {
				chiHuRight.opr_or(Constants_HuangShi.CHR_DA_DAO);
			} else {
				chiHuRight.opr_or(Constants_HuangShi.CHR_XIAO_DAO);
			}
		}

		return get_tmp_score(chiHuRight, seat_index, provide_index, zi_mo);
	}

	public float analyse_ying_hu_first_new(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight,
			int card_type, int seat_index, int provide_index) {
		if (cur_card == 0) {
			return 0;
		}

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		// TODO: 手里有红中不能胡牌
		if (cbCardIndexTemp[Constants_HuangShi.HONG_ZHONG_INDEX] > 0)
			return 0;

		// TODO 红中发财杠时，发财也是固定杠牌
		if (has_rule(Constants_HuangShi.GAME_RULE_HONG_ZHONG_FA_CAI_GANG)) {
			if (cbCardIndexTemp[Constants_HuangShi.FA_CAI_INDEX] > 0) {
				return 0;
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

		boolean can_win_with_magic = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card), magic_cards_index,
				magic_card_count); // 带癞子时能胡
		boolean can_win_without_magic = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
				magic_cards_index, 0); // 癞子牌还原之后能胡或者手牌没癞子牌能胡

		boolean can_win_qi_dui_with_magic = _logic.check_hubei_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card); // 带癞子时能胡七对
		boolean can_win_qi_dui_without_magic = _logic.check_hubei_ying_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card); // 硬七对

		if (analyse_state == NORMAL_STATE) {
			// TODO 能胡七对时，统计豪华七对的数目
			player_hao_hua_count[seat_index] = 0;
			if (can_win_qi_dui_with_magic)
				player_hao_hua_count[seat_index] = get_hao_hua_count(cbCardIndexTemp);
		}

		boolean can_win_jiang_yi_se_with_magic = _logic.check_hubei_jiang_yi_se(cbCardIndexTemp, weaveItems, weave_count); // 将一色，不用一对牌+3n，也可以胡牌
		boolean can_win_jiang_yi_se_without_magic = _logic.check_hubei_ying_jiang_yi_se(cbCardIndexTemp, weaveItems, weave_count); // 硬将一色

		boolean can_win = can_win_with_magic || can_win_qi_dui_with_magic || can_win_jiang_yi_se_with_magic; // 能胡牌

		if (can_win == false) {
			chiHuRight.set_empty();
			return 0;
		}

		boolean can_win_qing_yi_se_with_magic = _logic.check_hubei_qing_yi_se(cbCardIndexTemp, weaveItems, weave_count); // 清一色
		boolean can_win_qing_yi_se_without_magic = _logic.check_hubei_ying_qing_yi_se(cbCardIndexTemp, weaveItems, weave_count)
				&& can_win_without_magic; // 硬清一色

		boolean can_peng_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card), magic_cards_index,
				magic_card_count);
		boolean can_ying_peng_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
				magic_cards_index, 0);

		boolean exist_eat = _logic.exist_eat_hubei(weaveItems, weave_count);

		boolean can_win_peng_peng_hu_with_magic = can_peng_hu && !exist_eat; // 碰碰胡
		boolean can_win_peng_peng_hu_without_magic = can_ying_peng_hu && !exist_eat; // 硬碰碰胡

		boolean has_big_win = false;

		if (can_win_qi_dui_without_magic || can_win_jiang_yi_se_without_magic || can_win_qing_yi_se_without_magic
				|| can_win_peng_peng_hu_without_magic) {
			chiHuRight.opr_or(Constants_HuangShi.CHR_YING_HU);

			if (can_win_qi_dui_without_magic) {
				chiHuRight.opr_or(Constants_HuangShi.CHR_QI_DUI);
				has_big_win = true;
			}
			if (can_win_jiang_yi_se_without_magic) {
				chiHuRight.opr_or(Constants_HuangShi.CHR_JIANG_YI_SE);
				has_big_win = true;
			}
			if (can_win_qing_yi_se_without_magic) {
				chiHuRight.opr_or(Constants_HuangShi.CHR_QING_YI_SE);
				has_big_win = true;
			}
			if (can_win_peng_peng_hu_without_magic) {
				chiHuRight.opr_or(Constants_HuangShi.CHR_PENG_PENG_HU);
				has_big_win = true;
			}
		} else if (can_win_without_magic) {
			chiHuRight.opr_or(Constants_HuangShi.CHR_YING_HU);
		} else {
			chiHuRight.opr_or(Constants_HuangShi.CHR_RUAN_HU);

			if (can_win_qi_dui_with_magic) {
				chiHuRight.opr_or(Constants_HuangShi.CHR_QI_DUI);
				has_big_win = true;
			}
			if (can_win_jiang_yi_se_with_magic) {
				chiHuRight.opr_or(Constants_HuangShi.CHR_JIANG_YI_SE);
				has_big_win = true;
			}
			if (can_win_qing_yi_se_with_magic) {
				chiHuRight.opr_or(Constants_HuangShi.CHR_QING_YI_SE);
				has_big_win = true;
			}
			if (can_win_peng_peng_hu_with_magic) {
				chiHuRight.opr_or(Constants_HuangShi.CHR_PENG_PENG_HU);
				has_big_win = true;
			}
		}

		int tmp_card_count = _logic.get_card_count_by_index(cbCardIndexTemp);
		if (tmp_card_count == 2) {
			chiHuRight.opr_or(Constants_HuangShi.CHR_QUAN_QIU_REN);
			has_big_win = true;
		}

		int magic_count = _logic.magic_count(cbCardIndexTemp);
		if (magic_count >= 2 && has_big_win == false) {
			chiHuRight.set_empty();
			return 0;
		}

		boolean zi_mo = false;
		if (card_type == Constants_HuangShi.HU_CARD_TYPE_ZI_MO) {
			chiHuRight.opr_or(Constants_HuangShi.CHR_ZI_MO);
			zi_mo = true;
		} else {
			chiHuRight.opr_or(Constants_HuangShi.CHR_JIE_PAO);
		}

		int effective_weave_count = get_effective_weave_count(seat_index);

		if (zi_mo && !has_big_win) {
			if (effective_weave_count == 0) {
				chiHuRight.opr_or(Constants_HuangShi.CHR_DA_DAO);
			} else {
				chiHuRight.opr_or(Constants_HuangShi.CHR_XIAO_DAO);
			}
		}

		return get_tmp_score(chiHuRight, seat_index, provide_index, zi_mo);
	}

	public int analyse_chi_hu_card_new(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int seat_index, int provide_index) {
		can_win_but_without_enough_score = false;

		ChiHuRight chr = new ChiHuRight();
		chr.set_empty();
		float tmp_score = analyse_da_hu_first_new(cards_index, weaveItems, weave_count, cur_card, chr, card_type, seat_index, provide_index);
		if (analyse_state == NORMAL_STATE) {
			GRR._chi_hu_rights[seat_index] = chr;
		} else {
			chiHuRight = chr;
		}

		chr = new ChiHuRight();
		chr.set_empty();
		float tmp_score_2 = analyse_ying_hu_first_new(cards_index, weaveItems, weave_count, cur_card, chr, card_type, seat_index, provide_index);

		if (tmp_score_2 > tmp_score) {
			tmp_score = tmp_score_2;

			if (analyse_state == NORMAL_STATE) {
				GRR._chi_hu_rights[seat_index] = chr;
			} else {
				chiHuRight = chr;
			}
		}

		if (tmp_score < get_min_score()) {
			if (tmp_score > 0) {
				can_win_but_without_enough_score = true;
			}

			if (analyse_state == NORMAL_STATE) {
				GRR._chi_hu_rights[seat_index].set_empty();
			} else {
				chiHuRight.set_empty();
			}
			return GameConstants.WIK_NULL;
		}

		if (card_type == Constants_HuangShi.HU_CARD_TYPE_JIE_PAO) {
			score_when_jie_pao_hu[seat_index] = tmp_score;
		}

		return GameConstants.WIK_CHI_HU;
	}

	public boolean is_ying_jiang_yi_se(int[] cards_index, WeaveItem[] weave_items, int weave_count) {
		for (int i = GameConstants.MAX_ZI; i < GameConstants.MAX_INDEX; i++) {
			if (cards_index[i] > 0)
				return false;
		}

		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			if (cards_index[i] == 0) {
				continue;
			}

			int cbValue = _logic.get_card_value(_logic.switch_to_card_data(i));

			if ((cbValue != 2) && (cbValue != 5) && (cbValue != 8)) {
				return false;
			}
		}

		for (int i = 0; i < weave_count; i++) {
			if (weave_items[i].weave_kind == GameConstants.WIK_LEFT || weave_items[i].weave_kind == GameConstants.WIK_CENTER
					|| weave_items[i].weave_kind == GameConstants.WIK_RIGHT) {

				return false;
			}

			int color = _logic.get_card_color(weave_items[i].center_card);
			if (color > 2)
				return false;

			int cbValue = _logic.get_card_value(weave_items[i].center_card);

			if ((cbValue != 2) && (cbValue != 5) && (cbValue != 8)) {
				return false;
			}

		}

		return true;
	}

	public float get_tmp_score(ChiHuRight chr, int seat_index, int provide_index, boolean zimo) {
		float score = 0;

		int di_fen = get_di_fen();

		int pai_xing_fen = get_pai_xing_fen(chr);

		int fan_shu = 0;

		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index)
					continue;

				int tmp_fan_shu = fan_shu;

				if (!chr.opr_and(Constants_HuangShi.CHR_ZI_MO).is_empty())
					fan_shu += 1;
				if (!chr.opr_and(Constants_HuangShi.CHR_YING_HU).is_empty())
					fan_shu += 1;

				fan_shu += get_player_fan_shu(i) + get_player_fan_shu(seat_index);

				fan_shu += player_hao_hua_count[i] + player_hao_hua_count[seat_index];

				int real_fan_shu = (int) Math.pow(2, fan_shu);

				score += di_fen * pai_xing_fen * real_fan_shu / 10.0F;

				fan_shu = tmp_fan_shu;
			}
		} else {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index)
					continue;

				int tmp_fan_shu = fan_shu;

				if (provide_index == i) {
					// 放炮人多给1番
					fan_shu += 1;
				}

				if (!chr.opr_and(Constants_HuangShi.CHR_YING_HU).is_empty())
					fan_shu += 1;

				fan_shu += get_player_fan_shu(i) + get_player_fan_shu(seat_index);

				fan_shu += player_hao_hua_count[i] + player_hao_hua_count[seat_index];

				int real_fan_shu = (int) Math.pow(2, fan_shu);

				score += di_fen * pai_xing_fen * real_fan_shu / 10.0F;

				fan_shu = tmp_fan_shu;
			}
		}

		return score;
	}

	public boolean is_ying_qi_xiao_dui(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int cur_card) {
		if (cbWeaveCount != 0)
			return false;

		int cbReplaceCount = 0;
		@SuppressWarnings("unused")
		int nGenCount = 0;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		int cbCurrentIndex = _logic.switch_to_card_index(cur_card);
		cbCardIndexTemp[cbCurrentIndex]++;

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];

			if (cbCardCount == 1 || cbCardCount == 3)
				cbReplaceCount++;

			if (cbCardCount == 4) {
				nGenCount++;
			}
		}

		if (cbReplaceCount > 0)
			return false;

		return true;
	}

	public int get_hao_hua_count(int[] cards_index) {
		int count = 0;

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (cards_index[i] == 4)
				count++;
		}

		return count;
	}

	public boolean is_qi_xiao_dui(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int cur_card) {
		if (cbWeaveCount != 0)
			return false;

		int cbReplaceCount = 0;
		@SuppressWarnings("unused")
		int nGenCount = 0;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		int cbCurrentIndex = _logic.switch_to_card_index(cur_card);
		cbCardIndexTemp[cbCurrentIndex]++;

		int magic_card_count = _logic.get_magic_card_count();

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];

			if (magic_card_count > 0) {
				for (int m = 0; m < magic_card_count; m++) {
					if (i == _logic.get_magic_card_index(m))
						continue;

					if (cbCardCount == 1 || cbCardCount == 3)
						cbReplaceCount++;

					if (cbCardCount == 4) {
						nGenCount++;
					}
				}
			} else {
				if (cbCardCount == 1 || cbCardCount == 3)
					cbReplaceCount++;

				if (cbCardCount == 4) {
					nGenCount++;
				}
			}
		}

		if (magic_card_count > 0) {
			int count = 0;
			for (int m = 0; m < magic_card_count; m++) {
				count += cbCardIndexTemp[_logic.get_magic_card_index(m)];
			}

			if (cbReplaceCount > count) {
				return false;
			}
		} else {
			if (cbReplaceCount > 0)
				return false;
		}

		return true;
	}

	public int get_min_score() {
		int min_score = 8;

		if (has_rule(Constants_HuangShi.GAME_RULE_HONG_ZHONG_FA_CAI_GANG)) {
			if (ruleMap.containsKey(Constants_HuangShi.GAME_RULE_QI_HU_XUAN_ZE_2)) {
				min_score = ruleMap.get(Constants_HuangShi.GAME_RULE_QI_HU_XUAN_ZE_2);
			}
		} else if (has_rule(Constants_HuangShi.GAME_RULE_HONG_ZHONG_LAI_ZI_GANG)) {
			if (ruleMap.containsKey(Constants_HuangShi.GAME_RULE_QI_HU_XUAN_ZE_1)) {
				min_score = ruleMap.get(Constants_HuangShi.GAME_RULE_QI_HU_XUAN_ZE_1);
			}
		}

		return min_score;
	}

	public boolean exist_eat(WeaveItem weaveItem[], int cbWeaveCount) {
		for (int i = 0; i < cbWeaveCount; i++) {
			if (weaveItem[i].weave_kind == GameConstants.WIK_LEFT || weaveItem[i].weave_kind == GameConstants.WIK_RIGHT
					|| weaveItem[i].weave_kind == GameConstants.WIK_CENTER)
				return true;
		}

		return false;
	}

	public boolean is_ting_card(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		analyse_state = FROM_TING;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();
		for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
			chr.set_empty();
			int cbCurrentCard = _logic.switch_to_card_data(i);
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_new(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					Constants_HuangShi.HU_CARD_TYPE_ZI_MO, seat_index, seat_index))
				return true;
		}

		return false;
	}

	public boolean exe_select_magic_card(int seat_index) {
		set_handler(_handler_select_magic);
		_handler_select_magic.reset_status(seat_index);
		_handler_select_magic.exe(this);
		return true;
	}

	public boolean exe_show_card(int seat_index) {
		set_handler(_handler_show_card);
		_handler_show_card.reset_status(GRR._banker_player);
		_handler_show_card.exe(this);
		return true;
	}

	@Override
	public int get_real_card(int card) {
		if (card > GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI && card < GameConstants.CARD_ESPECIAL_TYPE_GANG) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_GANG && card < GameConstants.CARD_ESPECIAL_TYPE_PI_ZI) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_GANG;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_NEW_TING
				&& card < GameConstants.CARD_ESPECIAL_TYPE_NEW_TING + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_NEW_TING;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_NEW_TING + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI
				&& card < GameConstants.CARD_ESPECIAL_TYPE_NEW_TING + GameConstants.CARD_ESPECIAL_TYPE_GANG) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_NEW_TING + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_NEW_TING + GameConstants.CARD_ESPECIAL_TYPE_GANG
				&& card < GameConstants.CARD_ESPECIAL_TYPE_NEW_TING + GameConstants.CARD_ESPECIAL_TYPE_WANG_BA) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_NEW_TING + GameConstants.CARD_ESPECIAL_TYPE_GANG;
		}

		return card;
	}

	@Override
	protected void onInitTable() {
		_handler_chi_peng = new HandlerChiPeng_HuangShi();
		_handler_dispath_card = new HandlerDispatchCard_HuangShi();
		_handler_gang = new HandlerGang_HuangShi();
		_handler_out_card_operate = new HandlerOutCardOperate_HuangShi();
		_handler_select_magic = new HandlerSelectMagic_HuangShi();
		_handler_show_card = new HandlerShowCard_HuangShi();
		_handler_xiao_jie_suan = new HandlerXiaoJieSuan_HuangShi();

		_handler_qi_shou = new HandlerQiShou_HuangShi();
	}

	public boolean estimate_player_out_card_respond(int seat_index, int card, int type) {
		boolean bAroseAction = false;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (seat_index == i) {
				continue;
			}

			playerStatus = _playerStatus[i];

			if (GRR._left_card_count > 0) {
				if (i == get_banker_next_seat(seat_index)) {
					// TODO: 癞子参与吃的时候，算打出了一个癞子杠
					action = _logic.check_chi_ezhou(GRR._cards_index[i], card);

					if ((action & GameConstants.WIK_LEFT) != 0) {
						_playerStatus[i].add_action(GameConstants.WIK_LEFT);
						_playerStatus[i].add_chi(card, GameConstants.WIK_LEFT, seat_index);
					}
					if ((action & GameConstants.WIK_CENTER) != 0) {
						_playerStatus[i].add_action(GameConstants.WIK_CENTER);
						_playerStatus[i].add_chi(card, GameConstants.WIK_CENTER, seat_index);
					}
					if ((action & GameConstants.WIK_RIGHT) != 0) {
						_playerStatus[i].add_action(GameConstants.WIK_RIGHT);
						_playerStatus[i].add_chi(card, GameConstants.WIK_RIGHT, seat_index);
					}

					if (_playerStatus[i].has_action()) {
						bAroseAction = true;
					}
				}

				action = _logic.check_peng(GRR._cards_index[i], card);
				if (action != 0) {
					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);
					bAroseAction = true;
				}

				if (GRR._left_card_count > 0) {
					action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
					if (action != 0) {
						playerStatus.add_action(GameConstants.WIK_GANG);
						playerStatus.add_gang(card, seat_index, 1); // 加上杠
						bAroseAction = true;
					}
				}
			}

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
					ChiHuRight chr = GRR._chi_hu_rights[i];
					chr.set_empty();
					int cbWeaveCount = GRR._weave_count[i];

					int card_type = Constants_HuangShi.HU_CARD_TYPE_JIE_PAO;

					analyse_state = NORMAL_STATE;
					action = analyse_chi_hu_card_new(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr, card_type, i, seat_index);

					if (action != 0) {
						// 接炮时，总得分变大了，才能接炮胡
						if (score_when_jie_pao_hu[i] > score_when_abandoned_jie_pao[i]) {
							_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
							_playerStatus[i].add_chi_hu(card, seat_index);
							bAroseAction = true;
						}
					} else {
						// TODO 表示牌型能胡但是没达到起胡分
						if (can_win_but_without_enough_score) {
							operate_cant_win_info(i);
						}
					}
				}
			}
		}

		if (bAroseAction) {
			_resume_player = _current_player;
			_current_player = GameConstants.INVALID_SEAT;
			_provide_player = seat_index;
		}

		return bAroseAction;
	}

	public boolean operate_cant_win_info(int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_CAN_WIN_BUT_WITHOUT_ENOUGH_SCORE);
		roomResponse.setTarget(seat_index);
		roomResponse.setIsXiangGong(true); // TODO 表示牌型能胡但是没达到起胡分

		send_response_to_player(seat_index, roomResponse);

		return true;
	}

	public int get_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		analyse_state = FROM_TING;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int max_ting_count = GameConstants.MAX_ZI_FENG;
		int real_max_ting_count = GameConstants.MAX_ZI + 3;

		for (int i = 0; i < max_ting_count; i++) {
			if (i >= GameConstants.MAX_ZI && i <= GameConstants.MAX_ZI + 3)
				continue;

			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_new(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					Constants_HuangShi.HU_CARD_TYPE_ZI_MO, seat_index, seat_index)) {
				cards[count] = cbCurrentCard;
				if (_logic.is_magic_card(cbCurrentCard))
					cards[count] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				count++;
			}
		}

		if (count == 0) {
		} else if (count == real_max_ting_count - 1) {
			count = 1;
			cards[0] = -1;
		}

		return count;
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

	@Override
	public boolean on_game_start_new() {
		_logic.clean_magic_cards();

		da_dian_card = 0;
		magic_card = 0;

		saved_room_response = null;
		saved_game_end_response = null;

		player_fan_shu = new int[getTablePlayerNumber()];
		player_zhong_fa_bai_weave_count = new int[getTablePlayerNumber()];
		player_hao_hua_count = new int[getTablePlayerNumber()];

		score_when_jie_pao_hu = new float[getTablePlayerNumber()];
		score_when_abandoned_jie_pao = new float[getTablePlayerNumber()];

		can_reconnect = false;

		// TODO: 用之前的鸟牌数据来存储杠番的牌和统计
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			GRR._player_niao_count[i] = 0;
			for (int j = 0; j < GameConstants.MAX_LAI_ZI_PI_ZI_GANG; j++) {
				GRR._lai_zi_pi_zi_gang[i][j] = GameConstants.INVALID_VALUE;
			}
		}

		_game_status = GameConstants.GS_MJ_PLAY;

		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		boolean can_fa_cai_gang = has_rule(Constants_HuangShi.GAME_RULE_HONG_ZHONG_FA_CAI_GANG);
		int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants.MAX_COUNT];
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int hand_card_count = _logic.switch_to_cards_data_huangshi(GRR._cards_index[i], hand_cards[i], can_fa_cai_gang);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			load_room_info_data(roomResponse);
			load_player_info_data(roomResponse);
			load_common_status(roomResponse);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setCurrentPlayer(_current_player == GameConstants.INVALID_SEAT ? _resume_player : _current_player);
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

			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}
		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(GRR._left_card_count);
		GRR.add_room_response(roomResponse);

		exe_select_magic_card(GRR._banker_player);

		// 检测听牌
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i]._hu_card_count = get_ting_card(_playerStatus[i]._hu_cards, GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i],
					i);
			if (_playerStatus[i]._hu_card_count > 0) {
				operate_chi_hu_cards(i, _playerStatus[i]._hu_card_count, _playerStatus[i]._hu_cards);
			}
		}

		// exe_qi_shou(GRR._banker_player, GameConstants.WIK_NULL);

		exe_show_card(GRR._banker_player);

		// exe_dispatch_card(_current_player, GameConstants.WIK_NULL,
		// GameConstants.DELAY_SEND_CARD_DELAY);

		return true;
	}

	public int get_pai_xing_fen(ChiHuRight chr) {
		int fen = 0;

		boolean has_big_win = false;
		if (!chr.opr_and(Constants_HuangShi.CHR_QING_YI_SE).is_empty() || !chr.opr_and(Constants_HuangShi.CHR_PENG_PENG_HU).is_empty()
				|| !chr.opr_and(Constants_HuangShi.CHR_QI_DUI).is_empty() || !chr.opr_and(Constants_HuangShi.CHR_JIANG_YI_SE).is_empty()
				|| !chr.opr_and(Constants_HuangShi.CHR_QUAN_QIU_REN).is_empty()) {
			fen = 5;
			has_big_win = true;
		}

		if (!has_big_win) {
			fen = 1;
			if (!chr.opr_and(Constants_HuangShi.CHR_XIAO_DAO).is_empty()) {
				fen = 3;
			} else if (!chr.opr_and(Constants_HuangShi.CHR_DA_DAO).is_empty()) {
				fen = 5;
			}
		}

		return fen;
	}

	public int get_di_fen() {
		// 使用整型底分，计算时转换一下就可以
		int di_fen = 10;

		if (has_rule(Constants_HuangShi.GAME_RULE_DI_FEN_1))
			di_fen = 10;
		else if (has_rule(Constants_HuangShi.GAME_RULE_DI_FEN_2))
			di_fen = 20;
		else if (has_rule(Constants_HuangShi.GAME_RULE_DI_FEN_5))
			di_fen = 5;

		return di_fen;
	}

	public int get_max_fen() {
		int max_fen = 40;

		if (has_rule(Constants_HuangShi.GAME_RULE_HONG_ZHONG_FA_CAI_GANG)) {
			max_fen = 80;
			if (has_rule(Constants_HuangShi.GAME_RULE_DI_FEN_1))
				max_fen = 80;
			else if (has_rule(Constants_HuangShi.GAME_RULE_DI_FEN_2))
				max_fen = 160;
			else if (has_rule(Constants_HuangShi.GAME_RULE_DI_FEN_5))
				max_fen = 40;
		} else if (has_rule(Constants_HuangShi.GAME_RULE_HONG_ZHONG_LAI_ZI_GANG)) {
			max_fen = 40;
			if (has_rule(Constants_HuangShi.GAME_RULE_DI_FEN_1))
				max_fen = 40;
			else if (has_rule(Constants_HuangShi.GAME_RULE_DI_FEN_2))
				max_fen = 80;
			else if (has_rule(Constants_HuangShi.GAME_RULE_DI_FEN_5))
				max_fen = 20;
		}

		return max_fen;
	}

	public int get_jin_ding() {
		int jin_ding = 50;

		if (has_rule(Constants_HuangShi.GAME_RULE_HONG_ZHONG_FA_CAI_GANG)) { // 红中发财杠
			jin_ding = 100;
			if (has_rule(Constants_HuangShi.GAME_RULE_DI_FEN_1))
				jin_ding = 100;
			else if (has_rule(Constants_HuangShi.GAME_RULE_DI_FEN_2))
				jin_ding = 200;
			else if (has_rule(Constants_HuangShi.GAME_RULE_DI_FEN_5))
				jin_ding = 50;
		} else if (has_rule(Constants_HuangShi.GAME_RULE_HONG_ZHONG_LAI_ZI_GANG)) { // 红中癞子杠
			jin_ding = 50;
			if (has_rule(Constants_HuangShi.GAME_RULE_DI_FEN_1))
				jin_ding = 50;
			else if (has_rule(Constants_HuangShi.GAME_RULE_DI_FEN_2))
				jin_ding = 100;
			else if (has_rule(Constants_HuangShi.GAME_RULE_DI_FEN_5))
				jin_ding = 25;
		}

		return jin_ding;
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;

		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int di_fen = get_di_fen();

		int pai_xing_fen = get_pai_xing_fen(chr);

		int fan_shu = 0;
		int max_fen = get_max_fen();
		int jin_ding = get_jin_ding();
		boolean all_over_max_fen = true;
		float[] player_scores = new float[getTablePlayerNumber()];

		countCardType(chr, seat_index);

		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index)
					continue;

				GRR._lost_fan_shu[i][seat_index] = (int) di_fen * pai_xing_fen;
			}
		} else {
			GRR._lost_fan_shu[provide_index][seat_index] = (int) di_fen * pai_xing_fen;
		}

		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index)
					continue;

				int tmp_fan_shu = fan_shu;

				if (!chr.opr_and(Constants_HuangShi.CHR_ZI_MO).is_empty())
					fan_shu += 1;
				if (!chr.opr_and(Constants_HuangShi.CHR_YING_HU).is_empty())
					fan_shu += 1;

				fan_shu += get_player_fan_shu(i) + get_player_fan_shu(seat_index);

				fan_shu += player_hao_hua_count[seat_index];

				int real_fan_shu = (int) Math.pow(2, fan_shu);

				float s = di_fen * pai_xing_fen * real_fan_shu / 10.0F;

				player_scores[i] = s;

				if (s < max_fen)
					all_over_max_fen = false;

				fan_shu = tmp_fan_shu;
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (all_over_max_fen) {
					player_scores[i] = jin_ding;
				} else {
					if (player_scores[i] > max_fen)
						player_scores[i] = max_fen;
				}

				GRR._game_score[i] -= player_scores[i];
				GRR._game_score[seat_index] += player_scores[i];
			}
		} else {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index)
					continue;

				int tmp_fan_shu = fan_shu;

				// TODO 放炮者多给1番
				if (!GRR._chi_hu_rights[i].opr_and(Constants_HuangShi.CHR_FANG_PAO).is_empty())
					fan_shu += 1;
				if (!chr.opr_and(Constants_HuangShi.CHR_YING_HU).is_empty())
					fan_shu += 1;

				fan_shu += get_player_fan_shu(i) + get_player_fan_shu(seat_index);

				fan_shu += player_hao_hua_count[seat_index];

				int real_fan_shu = (int) Math.pow(2, fan_shu);

				float s = di_fen * pai_xing_fen * real_fan_shu / 10.0F;

				player_scores[i] = s;

				if (s < max_fen)
					all_over_max_fen = false;

				fan_shu = tmp_fan_shu;
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (all_over_max_fen) {
					player_scores[i] = jin_ding;
				} else {
					if (player_scores[i] > max_fen)
						player_scores[i] = max_fen;
				}

				GRR._game_score[i] -= player_scores[i];
				GRR._game_score[seat_index] += player_scores[i];
			}
		}

		GRR._provider[seat_index] = provide_index;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);

		return;
	}

	public int get_gang_fan_shu(int lost_player, int win_player) {
		int gang_fan_shu = 0;
		gang_fan_shu += (GRR._gang_score[lost_player].an_gang_count + GRR._gang_score[win_player].an_gang_count) * 2;
		gang_fan_shu += GRR._gang_score[lost_player].ming_gang_count + GRR._gang_score[win_player].ming_gang_count;

		return gang_fan_shu;
	}

	public boolean check_big_win(ChiHuRight chr) {
		boolean has_big_win = false;

		if (!chr.opr_and(Constants_HuangShi.CHR_QING_YI_SE).is_empty()) {
			has_big_win = true;
		}
		if (!chr.opr_and(Constants_HuangShi.CHR_PENG_PENG_HU).is_empty()) {
			has_big_win = true;
		}
		if (!chr.opr_and(Constants_HuangShi.CHR_QI_DUI).is_empty()) {
			has_big_win = true;
		}
		if (!chr.opr_and(Constants_HuangShi.CHR_JIANG_YI_SE).is_empty()) {
			has_big_win = true;
		}

		return has_big_win;
	}

	@Override
	protected void set_result_describe() {
		int chrTypes;
		long type = 0;
		for (int player = 0; player < getTablePlayerNumber(); player++) {
			StringBuilder result = new StringBuilder("");

			chrTypes = GRR._chi_hu_rights[player].type_count;

			boolean is_ying_hu = false;
			@SuppressWarnings("unused")
			boolean has_big_win = check_big_win(GRR._chi_hu_rights[player]);

			for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
				type = GRR._chi_hu_rights[player].type_list[typeIndex];

				if (GRR._chi_hu_rights[player].is_valid()) {
					if (type == Constants_HuangShi.CHR_XIAO_DAO)
						result.append(" 小刀");
					if (type == Constants_HuangShi.CHR_DA_DAO)
						result.append(" 大刀");
					if (type == Constants_HuangShi.CHR_ZI_MO)
						result.append(" 自摸");
					if (type == Constants_HuangShi.CHR_JIE_PAO)
						result.append(" 接炮");
					if (type == Constants_HuangShi.CHR_YING_HU) {
						result.append(" 硬胡");
						is_ying_hu = true;
					}
					if (type == Constants_HuangShi.CHR_QING_YI_SE)
						result.append(" 清一色");
					if (type == Constants_HuangShi.CHR_PENG_PENG_HU)
						result.append(" 碰碰胡");
					if (type == Constants_HuangShi.CHR_QI_DUI) {
						result.append(" 七对");

						if (player_hao_hua_count[player] > 0)
							result.append(" 豪华+" + player_hao_hua_count[player]);
					}
					if (type == Constants_HuangShi.CHR_JIANG_YI_SE)
						result.append(" 将一色");
					if (type == Constants_HuangShi.CHR_QUAN_QIU_REN)
						result.append(" 全求人");
				} else if (type == Constants_HuangShi.CHR_FANG_PAO) {
					if (type == Constants_HuangShi.CHR_FANG_PAO)
						result.append(" 放炮");
				}
			}

			int effective_weave_count = get_effective_weave_count(player);
			if (effective_weave_count == 3)
				result.append(" 开3口");
			else if (effective_weave_count == 4)
				result.append(" 开4口");

			if (GRR._chi_hu_rights[player].is_valid() && is_ying_hu == false)
				result.append(" 软胡");

			if (GRR._gang_score[player].an_gang_count > 0) {
				result.append(" 暗杠" + GRR._gang_score[player].an_gang_count * 2 + "番");
			}
			if (GRR._gang_score[player].ming_gang_count > 0) {
				result.append(" 明杠" + GRR._gang_score[player].ming_gang_count + "番");
			}

			GRR._result_des[player] = result.toString();
		}
	}

	@Override
	public boolean handler_player_be_in_room(int seat_index) {
		if (can_reconnect && get_players()[seat_index] != null && _release_scheduled == null) {
			if (_handler != null)
				_handler.handler_player_be_in_room(this, seat_index);

			return true;
		} else if ((GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) && get_players()[seat_index] != null) {
			if (_handler != null)
				_handler.handler_player_be_in_room(this, seat_index);
		}

		GameSchedule.put(new Runnable() {
			@Override
			public void run() {
				fixBugTemp(seat_index);
			}
		}, 1, TimeUnit.SECONDS);

		if (is_sys())
			return true;

		return true;
	}

	@Override
	public boolean handler_request_trustee(int seat_index, boolean isTrustee, int trustee_type) {
		if (_playerStatus == null || istrustee == null) {
			send_error_notify(seat_index, 2, "游戏未开始,无法进行自动胡牌托管!");
			return false;
		}

		if (_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) {
			send_error_notify(seat_index, 2, "游戏还未开始,无法进行自动胡牌托管!");
			return false;
		}

		if (istrustee[seat_index] == false) {
			int card_count = _logic.get_card_count_by_index(GRR._cards_index[seat_index]);
			if (card_count % 3 != 1) {
				send_error_notify(seat_index, 2, "您还未听牌,无法进行自动胡牌托管!");
				return false;
			} else if (!is_ting_card(GRR._cards_index[seat_index], GRR._weave_items[seat_index], GRR._weave_count[seat_index], seat_index)) {
				send_error_notify(seat_index, 2, "您还未听牌,无法进行自动胡牌托管!");
				return false;
			}
		}

		istrustee[seat_index] = isTrustee;

		// 这个取消托管的操作，由客户端主动发起，如果玩家是托管状态，并点了操作，取消定时任务
		if (_trustee_schedule[seat_index] != null) {
			_trustee_schedule[seat_index].cancel(false);
			_trustee_schedule[seat_index] = null;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_IS_TRUSTEE);
		roomResponse.setOperatePlayer(seat_index);
		roomResponse.setIstrustee(isTrustee);

		send_response_to_player(seat_index, roomResponse);

		if (GRR != null) {
			GRR.add_room_response(roomResponse);
		}

		if (_handler != null && isTrustee) {
			_handler.handler_be_set_trustee(this, seat_index);
		}

		return true;

	}

	public boolean operate_auto_win_card(int seat_index, boolean isTurnOn) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_SWITCH_AUTO_WIN_CARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setIsXiangGong(isTurnOn); // TODO false 表示隐藏 true 表示显示

		send_response_to_player(seat_index, roomResponse);

		return true;
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		return true;
	}

	@Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x08, 0x08, 0x08, 0x08, 0x12, 0x12, 0x15, 0x15, 0x18, 0x18, 0x28, 0x28, 0x02, 0x02 };
		int[] cards_of_player1 = new int[] { 0x08, 0x08, 0x08, 0x08, 0x12, 0x12, 0x15, 0x15, 0x18, 0x18, 0x28, 0x28, 0x02, 0x02 };
		int[] cards_of_player2 = new int[] { 0x08, 0x08, 0x08, 0x08, 0x12, 0x12, 0x15, 0x15, 0x18, 0x18, 0x28, 0x28, 0x02, 0x02 };
		int[] cards_of_player3 = new int[] { 0x08, 0x08, 0x08, 0x08, 0x12, 0x12, 0x15, 0x15, 0x18, 0x18, 0x28, 0x28, 0x02, 0x02 };

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

	@Override
	protected boolean on_game_start() {
		return false;
	}
}
