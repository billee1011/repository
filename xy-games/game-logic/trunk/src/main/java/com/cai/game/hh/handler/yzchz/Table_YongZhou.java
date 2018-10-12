package com.cai.game.hh.handler.yzchz;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.phz.Constants_YongZhou;
import com.cai.common.define.ECardType;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RandomUtil;
import com.cai.dictionary.SysParamDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.GangCardRunnable;
import com.cai.game.RoomUtil;
import com.cai.game.hh.HHTable;
import com.cai.game.hh.util.AnalyseUtil;
import com.cai.game.hh.util.Solution;
import com.cai.game.hh.util.WeaveInfo;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;
import protobuf.clazz.phz.ScoreRecord.Score_Record;

public class Table_YongZhou extends HHTable {
	private static final long serialVersionUID = -6411443691444147984L;

	public int time_for_animation = 0; // 发牌动画的时间(ms)
	public int time_for_organize = 100; // 理牌的时间(ms)
	public int time_for_operate_dragon = 800; // 执行提的延时(ms)
	public int time_for_add_discard = 800; // 加废牌堆的延时(ms)
	public int time_for_dispatch_card = 800; // 发牌的延时(ms)

	public int time_for_deal_first_card = 300; // 处理第一张牌的延时(ms)
	public int time_for_display_win_border = 3; // 点了胡之后，到出现小结算的延时(s)

	public int time_for_display_wang_pai = 1000; // 抓了王牌之后的展示时间

	public int delay_when_passed = 200; // 所有人点了过之后的延时

	public int[][] all_game_round_score = new int[getTablePlayerNumber()][32];
	public int[] total_score = new int[getTablePlayerNumber()];

	public int[] total_tun_shu = new int[getTablePlayerNumber()];

	public int[] magicCountWhenWin = new int[getTablePlayerNumber()];
	public int[][] magicToRealCard = new int[getTablePlayerNumber()][12];

	/**
	 * 王炸时的王牌映射关系
	 */
	public int[] wangZhaMagicCountWhenWin = new int[getTablePlayerNumber()];
	public int[][] wangZhaMagicToRealCard = new int[getTablePlayerNumber()][12];

	/**
	 * 王闯时的王牌映射关系
	 */
	public int[] wangChuangMagicCountWhenWin = new int[getTablePlayerNumber()];
	public int[][] wangChuangMagicToRealCard = new int[getTablePlayerNumber()][12];

	/**
	 * 王钓时的王牌映射关系
	 */
	public int[] wangDiaoMagicCountWhenWin = new int[getTablePlayerNumber()];
	public int[][] wangDiaoMagicToRealCard = new int[getTablePlayerNumber()][12];

	public int[] magicCount = new int[getTablePlayerNumber()];
	public int[] magicScore = new int[getTablePlayerNumber()];

	public int cardWhenWin = 0;

	public boolean is_analyse_wang_pai_pai_xing = false;

	public boolean[] is_pao_hu = new boolean[getTablePlayerNumber()];

	public int zuo_xing_seat = -1;

	/**
	 * 王炸时的胡牌信息
	 */
	public int _wang_zha_hu_weave_count[];
	public WeaveItem _wang_zha_hu_weave_items[][];

	/**
	 * 王闯时的胡牌信息
	 */
	public int _wang_chuang_hu_weave_count[];
	public WeaveItem _wang_chuang_hu_weave_items[][];

	/**
	 * 王闯时的胡牌信息
	 */
	public int _wang_diao_hu_weave_count[];
	public WeaveItem _wang_diao_hu_weave_items[][];

	/**
	 * 王炸时的最优解
	 */
	ChiHuRight[] wangZhaChr = new ChiHuRight[getTablePlayerNumber()];
	/**
	 * 王闯时的最优解
	 */
	ChiHuRight[] wangChuangChr = new ChiHuRight[getTablePlayerNumber()];
	/**
	 * 王钓时的最优解
	 */
	ChiHuRight[] wangDiaoChr = new ChiHuRight[getTablePlayerNumber()];

	public boolean is_qi_shou_analyse = false;
	/**
	 * 是否是王钓上家的牌
	 */
	public boolean is_wang_zha_others_card = false;
	/**
	 * 是否是王钓之后再次翻牌胡牌
	 */
	public boolean changed = false;
	/**
	 * 是否有翻醒玩法
	 */
	public boolean has_rule_fan_xing = false;

	public Table_YongZhou() {
		super();

		_wang_zha_hu_weave_count = new int[getTablePlayerNumber()];
		_wang_zha_hu_weave_items = new WeaveItem[getTablePlayerNumber()][7];

		_wang_chuang_hu_weave_count = new int[getTablePlayerNumber()];
		_wang_chuang_hu_weave_items = new WeaveItem[getTablePlayerNumber()][7];

		_wang_diao_hu_weave_count = new int[getTablePlayerNumber()];
		_wang_diao_hu_weave_items = new WeaveItem[getTablePlayerNumber()][7];

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			wangZhaChr[i] = new ChiHuRight();
			wangChuangChr[i] = new ChiHuRight();
			wangDiaoChr[i] = new ChiHuRight();
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < 7; j++) {
				_wang_zha_hu_weave_items[i][j] = new WeaveItem();
				_wang_chuang_hu_weave_items[i][j] = new WeaveItem();
				_wang_diao_hu_weave_items[i][j] = new WeaveItem();
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean exe_gang(int seat_index, int provide_player, int center_card, int action, int type, boolean depatch, boolean self, boolean d,
			int delay) {
		if (delay > 0) {
			GameSchedule.put(new GangCardRunnable(getRoom_id(), seat_index, provide_player, center_card, action, type, depatch, self, d), delay,
					TimeUnit.MILLISECONDS);
		} else {
			_handler = _handler_gang;
			_handler_gang.reset_status(seat_index, provide_player, center_card, action, type, self, d, depatch);
			_handler.exe(this);
		}

		return true;
	}

	public void process_pei_fu_four_player() {
		int pCount = getTablePlayerNumber();
		for (int i = 0; i < pCount; i++) {
			if (pCount == 4 && i == zuo_xing_seat)
				continue;

			magicCount[i] = GRR._cards_index[i][Constants_YongZhou.MAX_CARD_INDEX - 1];
		}

		// 非坐醒的两个非庄玩家和庄家进行比较 如果他们要输分 也要输一份给坐醒的人 如果他们赢分 就只是赢庄家的分 坐醒的人不用给分
		if (GRR != null) {
			if (GRR._banker_player == 0) {
				int compare1 = magicCount[1] - magicCount[0];
				int compare2 = magicCount[3] - magicCount[0];

				magicScore[0] += 5 * compare1;
				magicScore[1] -= 5 * compare1;
				if (compare1 > 0) {
					magicScore[2] += 5 * compare1;
					magicScore[1] -= 5 * compare1;
				}

				magicScore[0] += 5 * compare2;
				magicScore[3] -= 5 * compare2;
				if (compare2 > 0) {
					magicScore[2] += 5 * compare2;
					magicScore[3] -= 5 * compare2;
				}
			}
			if (GRR._banker_player == 1) {
				int compare1 = magicCount[0] - magicCount[1];
				int compare2 = magicCount[2] - magicCount[1];

				magicScore[1] += 5 * compare1;
				magicScore[0] -= 5 * compare1;
				if (compare1 > 0) {
					magicScore[3] += 5 * compare1;
					magicScore[0] -= 5 * compare1;
				}

				magicScore[1] += 5 * compare2;
				magicScore[2] -= 5 * compare2;
				if (compare2 > 0) {
					magicScore[3] += 5 * compare2;
					magicScore[2] -= 5 * compare2;
				}
			}
			if (GRR._banker_player == 2) {
				int compare1 = magicCount[1] - magicCount[2];
				int compare2 = magicCount[3] - magicCount[2];

				magicScore[2] += 5 * compare1;
				magicScore[1] -= 5 * compare1;
				if (compare1 > 0) {
					magicScore[0] += 5 * compare1;
					magicScore[1] -= 5 * compare1;
				}

				magicScore[2] += 5 * compare2;
				magicScore[3] -= 5 * compare2;
				if (compare2 > 0) {
					magicScore[0] += 5 * compare2;
					magicScore[3] -= 5 * compare2;
				}
			}
			if (GRR._banker_player == 3) {
				int compare1 = magicCount[0] - magicCount[3];
				int compare2 = magicCount[2] - magicCount[3];

				magicScore[3] += 5 * compare1;
				magicScore[0] -= 5 * compare1;
				if (compare1 > 0) {
					magicScore[1] += 5 * compare1;
					magicScore[0] -= 5 * compare1;
				}

				magicScore[3] += 5 * compare2;
				magicScore[2] -= 5 * compare2;
				if (compare2 > 0) {
					magicScore[1] += 5 * compare2;
					magicScore[2] -= 5 * compare2;
				}
			}
		}
	}

	public void process_pei_fu() {
		int pCount = getTablePlayerNumber();
		for (int i = 0; i < pCount; i++) {
			if (pCount == 4 && i == zuo_xing_seat)
				continue;

			magicCount[i] = GRR._cards_index[i][Constants_YongZhou.MAX_CARD_INDEX - 1];
		}
		for (int i = 0; i < pCount; i++) {
			if (pCount == 4 && i == zuo_xing_seat)
				continue;

			for (int j = i + 1; j < pCount; j++) {
				if (pCount == 4 && j == zuo_xing_seat)
					continue;

				if (magicCount[i] > magicCount[j]) {
					magicScore[i] -= 5 * (magicCount[i] - magicCount[j]);
					magicScore[j] += 5 * (magicCount[i] - magicCount[j]);
				} else {
					magicScore[i] += 5 * (magicCount[j] - magicCount[i]);
					magicScore[j] -= 5 * (magicCount[j] - magicCount[i]);
				}
			}
		}
	}

	public boolean wang_pai_pai_xing_check(int seat_index, int provide_index, int card_data, int card_type) {
		int[] hu_xi = new int[1];
		boolean hasAction = false;
		PlayerStatus tempPlayerStatus = _playerStatus[seat_index];

		int tmpMagicCount = GRR._cards_index[seat_index][Constants_YongZhou.MAX_CARD_INDEX - 1];

		if (!is_wang_zha_others_card || (is_wang_zha_others_card && (tmpMagicCount == 3 || tmpMagicCount == 4))) {
			wangZhaChr[seat_index].set_empty();
			int action_wang_zha = analyse_wang_zha(GRR._cards_index[seat_index], GRR._weave_items[seat_index], GRR._weave_count[seat_index],
					seat_index, provide_index, card_data, wangZhaChr[seat_index], card_type, hu_xi, true);
			if (action_wang_zha != 0) {
				hasAction = true;
				tempPlayerStatus.add_action(GameConstants.WIK_WANG_ZHA);
				tempPlayerStatus.add_normal_wik(card_data, GameConstants.WIK_WANG_ZHA, seat_index);
			}
		}

		if (!is_wang_zha_others_card || (is_wang_zha_others_card && (tmpMagicCount == 2 || tmpMagicCount == 3))) {
			wangChuangChr[seat_index].set_empty();
			int action_wang_chuang = analyse_wang_chuang(GRR._cards_index[seat_index], GRR._weave_items[seat_index], GRR._weave_count[seat_index],
					seat_index, provide_index, card_data, wangChuangChr[seat_index], card_type, hu_xi, true);
			if (action_wang_chuang != 0) {
				hasAction = true;
				tempPlayerStatus.add_action(GameConstants.WIK_WANG_CHUANG);
				tempPlayerStatus.add_normal_wik(card_data, GameConstants.WIK_WANG_CHUANG, seat_index);
			}
		}

		if (!is_wang_zha_others_card || (is_wang_zha_others_card && (tmpMagicCount == 1 || tmpMagicCount == 2))) {
			wangDiaoChr[seat_index].set_empty();
			int action_wang_diao = analyse_wang_diao(GRR._cards_index[seat_index], GRR._weave_items[seat_index], GRR._weave_count[seat_index],
					seat_index, provide_index, card_data, wangDiaoChr[seat_index], card_type, hu_xi, true);
			if (action_wang_diao != 0) {
				hasAction = true;
				tempPlayerStatus.add_action(GameConstants.WIK_WANG_DIAO);
				tempPlayerStatus.add_normal_wik(card_data, GameConstants.WIK_WANG_DIAO, seat_index);
			}
		}

		return hasAction;
	}

	public boolean normal_pai_xing_check(int seat_index, int provide_index, int card_data, int card_type) {
		int[] hu_xi = new int[1];
		boolean hasAction = false;
		PlayerStatus tempPlayerStatus = _playerStatus[seat_index];

		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		chr.set_empty();
		int action_hu = analyse_normal(GRR._cards_index[seat_index], GRR._weave_items[seat_index], GRR._weave_count[seat_index], seat_index,
				provide_index, card_data, chr, card_type, hu_xi, true);
		if (action_hu != 0) {
			hasAction = true;
			if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
				tempPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
				tempPlayerStatus.add_zi_mo(card_data, seat_index);
			} else {
				tempPlayerStatus.add_action(GameConstants.WIK_CHI_HU);
				tempPlayerStatus.add_chi_hu(card_data, seat_index);
			}
		}

		return hasAction;
	}

	public void liu_ju() {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
		}

		int pCount = getTablePlayerNumber();
		for (int i = 0; i < pCount; i++) {
			if (pCount == 4 && i == zuo_xing_seat)
				continue;

			int cards[] = new int[GameConstants.MAX_YONG_ZHOU_HH_COUNT];
			int hand_card_count = _logic.switch_to_cards_data_yongzhou(GRR._cards_index[i], cards);

			operate_show_card(i, GameConstants.Show_Card_HU, hand_card_count, cards, GRR._weave_items[i], GRR._weave_count[i],
					GameConstants.INVALID_SEAT);
		}

		// 统计庄家黄庄次数
		_player_result.liu_zi_fen[GRR._banker_player]++;

		_cur_banker = (GRR._banker_player + getTablePlayerNumber() + 1) % getTablePlayerNumber();
		_shang_zhuang_player = GameConstants.INVALID_SEAT;

		if (pCount == 4 && !has_rule(Constants_YongZhou.GAME_RULE_NO_WANG_PAI)) {
			process_pei_fu_four_player();
		} else if (!has_rule(Constants_YongZhou.GAME_RULE_NO_WANG_PAI)) {
			process_pei_fu();
		}

		// 流局
		operate_effect_action(_cur_banker, GameConstants.EFFECT_ACTION_DRAW, 1, new long[] { GameConstants.WIK_LIU_JU }, 1,
				GameConstants.INVALID_SEAT);

		int delay = time_for_display_win_border;

		GameSchedule.put(new GameFinishRunnable(getRoom_id(), _cur_banker, GameConstants.Game_End_DRAW), delay, TimeUnit.SECONDS);
	}

	public int estimate_player_ti_wei_respond(int seat_index, int card_data, boolean special_check) {
		int bAroseAction = GameConstants.WIK_NULL;
		if (_logic.estimate_pao_card_out_card(GRR._cards_index[seat_index], card_data) != GameConstants.WIK_NULL) {
			exe_gang(seat_index, seat_index, card_data, GameConstants.WIK_TI_LONG, GameConstants.PAO_TYPE_TI_MINE_LONG, true, true, false,
					time_for_operate_dragon);
			bAroseAction = GameConstants.WIK_TI_LONG;
		}

		if (bAroseAction == GameConstants.WIK_NULL) {
			for (int weave_index = 0; weave_index < GRR._weave_count[seat_index]; weave_index++) {
				int weave_kind = GRR._weave_items[seat_index][weave_index].weave_kind;
				int weave_card = GRR._weave_items[seat_index][weave_index].center_card;

				if ((weave_card != card_data) || (weave_kind != GameConstants.WIK_WEI))
					continue;

				bAroseAction = GameConstants.WIK_TI_LONG;

				exe_gang(seat_index, seat_index, card_data, GameConstants.WIK_TI_LONG, GameConstants.PAO_TYPE_MINE_SAO_LONG, true, true, false,
						time_for_operate_dragon);
			}
		}

		if ((bAroseAction == GameConstants.WIK_NULL) && (_logic.check_sao(GRR._cards_index[seat_index], card_data) != GameConstants.WIK_NULL)) {
			int action = GameConstants.WIK_WEI;
			if (_cannot_peng[seat_index][_logic.switch_to_card_index(card_data)] != 0) {
				action = GameConstants.WIK_CHOU_WEI;
			}

			bAroseAction = GameConstants.WIK_WEI;

			exe_gang(seat_index, seat_index, card_data, action, GameConstants.SAO_TYPE_MINE_SAO, true, true, false, time_for_operate_dragon);
		}

		return bAroseAction;
	}

	@Override
	public boolean operate_player_cards(int seat_index, int card_count, int cards[], int weave_count, WeaveItem weaveitems[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS); // 12更新玩家手牌数据
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(1);

		load_common_status(roomResponse);

		roomResponse.setFlashTime(200);

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
				weaveItem_item.setHuXi(weaveitems[j].hu_xi);

				weaveItem_item.setCenterCard(weaveitems[j].center_card);
				roomResponse.addWeaveItems(weaveItem_item);
			}
		}

		int pCount = getTablePlayerNumber();
		if (pCount == 4) {
			for (int i = 0; i < pCount; i++) {
				if (i == seat_index)
					continue;
				if (seat_index == GRR._banker_player && i == zuo_xing_seat)
					continue;

				send_response_to_player(i, roomResponse);
			}
		} else {
			send_response_to_other(seat_index, roomResponse);
		}

		if (weave_count > 0) {
			roomResponse.clearWeaveItems();
			for (int j = 0; j < weave_count; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(weaveitems[j].provide_player);
				weaveItem_item.setPublicCard(weaveitems[j].public_card);
				weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
				weaveItem_item.setCenterCard(weaveitems[j].center_card);
				weaveItem_item.setHuXi(weaveitems[j].hu_xi);
				roomResponse.addWeaveItems(weaveItem_item);
			}
		}

		for (int j = 0; j < card_count; j++) {
			roomResponse.addCardData(cards[j]);
		}

		roomResponse.setHuXiCount(_hu_xi[seat_index]);
		GRR.add_room_response(roomResponse);

		if (pCount == 4) {
			if (seat_index == GRR._banker_player) {
				for (int i = 0; i < pCount; i++) {
					if (i == zuo_xing_seat) {
						send_response_to_player(i, roomResponse);
						break;
					}
				}
			}
		}
		send_response_to_player(seat_index, roomResponse);

		return true;
	}

	@Override
	public void load_player_info_data(RoomResponse.Builder roomResponse) {
		Player rplayer;
		int pCount = getTablePlayerNumber();
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
			room_player.setPao(_player_result.pao[i]);
			room_player.setQiang(_player_result.qiang[i]);
			if (pCount == 4 && i == zuo_xing_seat) {
				room_player.setNao(1);
			} else {
				room_player.setNao(0);
			}
			room_player.setOpenThree(_player_open_less[i] == 0 ? false : true);
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			room_player.setGvoiceStatus(rplayer.getGvoiceStatus());
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			roomResponse.addPlayers(room_player);
		}
	}

	@Override
	public void process_chi_hu_player_operate(int seat_index, int operate_card, boolean rm) {
		if (!changed) {
			ChiHuRight chr = GRR._chi_hu_rights[seat_index];
			operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, chr.type_count, chr.type_list, 1, GameConstants.INVALID_SEAT);
		}

		int pCount = getTablePlayerNumber();
		for (int i = 0; i < pCount; i++) {
			if (pCount == 4 && i == zuo_xing_seat)
				continue;

			int cards[] = new int[GameConstants.MAX_YONG_ZHOU_HH_COUNT];
			int hand_card_count = _logic.switch_to_cards_data_yongzhou(GRR._cards_index[i], cards);

			operate_show_card(i, GameConstants.Show_Card_HU, hand_card_count, cards, GRR._weave_items[i], GRR._weave_count[i],
					GameConstants.INVALID_SEAT);
		}

		return;
	}

	@Override
	public void shuffle(int repertory_card[], int mj_cards[]) {
		_all_card_len = repertory_card.length;
		GRR._left_card_count = _all_card_len;

		// _logic.random_card_data(repertory_card, mj_cards);
		int xi_pai_count = 0;
		int rand = RandomUtil.generateRandomNumber(3, 6);
		while (xi_pai_count < 6 && xi_pai_count < rand) {
			if (xi_pai_count == 0)
				_logic.random_card_data(repertory_card, mj_cards);
			else
				_logic.random_card_data(repertory_card, repertory_card);
			xi_pai_count++;
		}

		int send_count = 20;
		int have_send_count = 0;

		int count = getTablePlayerNumber();
		if (count == 4) {
			for (int i = 0; i < count; i++) {
				if (i == zuo_xing_seat)
					continue;

				send_count = GameConstants.MAX_HH_COUNT - 1;

				GRR._left_card_count -= send_count;
				_logic.switch_to_cards_index_yongzhou(repertory_card, have_send_count, send_count, GRR._cards_index[i]);
				have_send_count += send_count;
			}
		} else {
			for (int i = 0; i < count; i++) {
				send_count = GameConstants.MAX_HH_COUNT - 1;

				GRR._left_card_count -= send_count;
				_logic.switch_to_cards_index_yongzhou(repertory_card, have_send_count, send_count, GRR._cards_index[i]);
				have_send_count += send_count;
			}
		}

		_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));
	}

	@Override
	protected boolean on_handler_game_start() {
		_game_status = GameConstants.GS_MJ_PLAY;

		reset_init_data();

		progress_banker_select();

		if (DEBUG_CARDS_MODE) {
			_cur_banker = 0;
		}
		GRR._banker_player = _cur_banker;
		_current_player = GRR._banker_player;

		if (getTablePlayerNumber() == 4) {
			zuo_xing_seat = (GRR._banker_player + 2) % getTablePlayerNumber();
			operate_player_data();
		}

		if (has_rule(Constants_YongZhou.GAME_RULE_NO_MAGIC)) {
			_repertory_card = new int[Constants_YongZhou.CARD_COUNT_NO_MAGIC];
			shuffle(_repertory_card, Constants_YongZhou.CARD_DATA_NO_MAGIC);
		} else if (has_rule(Constants_YongZhou.GAME_RULE_ONE_MAGIC)) {
			_repertory_card = new int[Constants_YongZhou.CARD_COUNT_ONE_MAGIC];
			shuffle(_repertory_card, Constants_YongZhou.CARD_DATA_ONE_MAGIC);
		} else if (has_rule(Constants_YongZhou.GAME_RULE_TWO_MAGIC)) {
			_repertory_card = new int[Constants_YongZhou.CARD_COUNT_TWO_MAGIC];
			shuffle(_repertory_card, Constants_YongZhou.CARD_DATA_TWO_MAGIC);
		} else if (has_rule(Constants_YongZhou.GAME_RULE_THREE_MAGIC)) {
			_repertory_card = new int[Constants_YongZhou.CARD_COUNT_THREE_MAGIC];
			shuffle(_repertory_card, Constants_YongZhou.CARD_DATA_THREE_MAGIC);
		} else if (has_rule(Constants_YongZhou.GAME_RULE_FOUR_MAGIC)) {
			_repertory_card = new int[Constants_YongZhou.CARD_COUNT_FOUR_MAGIC];
			shuffle(_repertory_card, Constants_YongZhou.CARD_DATA_FOUR_MAGIC);
		}

		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();

		return game_start_HH();
	}

	@Override
	public boolean handler_requst_message_deal(Player player, int seat_index, RoomRequest room_rq, int type) {
		if (type == MsgConstants.REQUEST_SCORE_RECORD && _game_status != GameConstants.GS_MJ_FREE) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_SCORE_RECORD);

			Score_Record.Builder sr = Score_Record.newBuilder();
			if (getTablePlayerNumber() == 2) {
				for (int j = 0; j < _cur_round; j++) {
					sr.addScorePlayer1(all_game_round_score[0][j]);
					sr.addScorePlayer2(all_game_round_score[1][j]);
				}
				sr.setScoreTotal1(total_score[0]);
				sr.setScoreTotal2(total_score[1]);
			} else if (getTablePlayerNumber() == 3) {
				for (int j = 0; j < _cur_round; j++) {
					sr.addScorePlayer1(all_game_round_score[0][j]);
					sr.addScorePlayer2(all_game_round_score[1][j]);
					sr.addScorePlayer3(all_game_round_score[2][j]);
				}
				sr.setScoreTotal1(total_score[0]);
				sr.setScoreTotal2(total_score[1]);
				sr.setScoreTotal3(total_score[2]);
			} else if (getTablePlayerNumber() == 4) {
				for (int j = 0; j < _cur_round; j++) {
					sr.addScorePlayer1(all_game_round_score[0][j]);
					sr.addScorePlayer2(all_game_round_score[1][j]);
					sr.addScorePlayer3(all_game_round_score[2][j]);
					sr.addScorePlayer4(all_game_round_score[3][j]);
				}
				sr.setScoreTotal1(total_score[0]);
				sr.setScoreTotal2(total_score[1]);
				sr.setScoreTotal3(total_score[2]);
				sr.setScoreTotal4(total_score[3]);
			}

			load_player_info_data(roomResponse);

			roomResponse.setCommResponse(PBUtil.toByteString(sr));

			send_response_to_player(seat_index, roomResponse);
		}

		return true;
	}

	private boolean game_start_HH() {
		_handler = _handler_chuli_firstcards;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_result.pao[i] = 0;
		}
		total_tun_shu = new int[getTablePlayerNumber()];

		magicCountWhenWin = new int[getTablePlayerNumber()];
		magicToRealCard = new int[getTablePlayerNumber()][12];

		wangZhaMagicCountWhenWin = new int[getTablePlayerNumber()];
		wangZhaMagicToRealCard = new int[getTablePlayerNumber()][12];

		wangChuangMagicCountWhenWin = new int[getTablePlayerNumber()];
		wangChuangMagicToRealCard = new int[getTablePlayerNumber()][12];

		wangDiaoMagicCountWhenWin = new int[getTablePlayerNumber()];
		wangDiaoMagicToRealCard = new int[getTablePlayerNumber()][12];

		magicCount = new int[getTablePlayerNumber()];
		magicScore = new int[getTablePlayerNumber()];

		cardWhenWin = 0;

		changed = false;

		int gameId = getGame_id() == 0 ? 222 : getGame_id();

		SysParamModel sysParamModel1205 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId).get(1205);

		if (sysParamModel1205 != null && sysParamModel1205.getVal1() > 0 && sysParamModel1205.getVal1() < 10000) {
			time_for_animation = sysParamModel1205.getVal1();
		}
		if (sysParamModel1205 != null && sysParamModel1205.getVal2() > 0 && sysParamModel1205.getVal2() < 10000) {
			time_for_organize = sysParamModel1205.getVal2();
		}
		if (sysParamModel1205 != null && sysParamModel1205.getVal3() > 0 && sysParamModel1205.getVal3() < 10000) {
			time_for_operate_dragon = sysParamModel1205.getVal3();
		}
		if (sysParamModel1205 != null && sysParamModel1205.getVal4() > 0 && sysParamModel1205.getVal4() < 10000) {
			time_for_add_discard = sysParamModel1205.getVal4();
		}
		if (sysParamModel1205 != null && sysParamModel1205.getVal5() > 0 && sysParamModel1205.getVal5() < 10000) {
			time_for_dispatch_card = sysParamModel1205.getVal5();
		}

		_logic.clean_magic_cards();
		int playerCount = getTablePlayerNumber();
		GRR._banker_player = _current_player = _cur_banker;
		// 游戏开始
		_game_status = GameConstants.GS_MJ_PLAY;// 设置状态
		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[playerCount][GameConstants.MAX_HH_COUNT];
		// 发送数据
		for (int i = 0; i < playerCount; i++) {
			if (playerCount == 4 && i == zuo_xing_seat)
				continue;

			int hand_card_count = _logic.switch_to_cards_data_yongzhou(GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		@SuppressWarnings("unused")
		boolean can_ti = false;
		int ti_card_count[] = new int[getTablePlayerNumber()];
		int ti_card_index[][] = new int[getTablePlayerNumber()][5];

		for (int i = 0; i < playerCount; i++) {
			if (playerCount == 4 && i == zuo_xing_seat)
				continue;

			ti_card_count[i] = _logic.get_action_ti_Card(GRR._cards_index[i], ti_card_index[i]);
			if (ti_card_count[i] > 0)
				can_ti = true;
		}
		// 发送数据
		for (int i = 0; i < playerCount; i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			// 只发自己的牌
			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.MAX_HH_COUNT; j++) {
				if (playerCount == 4 && i == zuo_xing_seat) {
					gameStartResponse.addCardData(hand_cards[GRR._banker_player][j]);
				} else {
					gameStartResponse.addCardData(hand_cards[i][j]);
				}
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
			roomResponse.setCurrentPlayer(_current_player == GameConstants.INVALID_SEAT ? _resume_player : _current_player);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			roomResponse.setGameStatus(_game_status);
			roomResponse.setLeftCardCount(GRR._left_card_count);

			// 设置发牌动画时间和停留时间
			roomResponse.setFlashTime(time_for_animation);
			roomResponse.setStandTime(time_for_organize);
			send_response_to_player(i, roomResponse);
		}
		////////////////////////////////////////////////// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		load_room_info_data(roomResponse);
		load_common_status(roomResponse);
		load_player_info_data(roomResponse);
		for (int i = 0; i < playerCount; i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			for (int j = 0; j < GameConstants.MAX_HH_COUNT; j++) {
				if (playerCount == 4 && i == zuo_xing_seat)
					continue;

				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}

		roomResponse.setGameStart(gameStartResponse);

		roomResponse.setLeftCardCount(GRR._left_card_count);
		GRR.add_room_response(roomResponse);

		exe_chuli_first_card(_current_player, GameConstants.WIK_NULL, time_for_animation + time_for_organize);

		return true;
	}

	@Override
	public boolean on_room_game_finish(int seat_index, int reason) {
		_game_status = GameConstants.GS_MJ_WAIT;

		if (_cur_round == 1 && (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW)) {
			RoomUtil.realkou_dou(this);
		}

		if (is_sys()) {
			clear_score_in_gold_room();
		}

		return handler_game_finish_phz(seat_index, reason);
	}

	public boolean handler_game_finish_phz(int seat_index, int reason) {
		int real_reason = reason;

		int count = getTablePlayerNumber();
		if (count == 0) {
			count = getTablePlayerNumber();
		}
		for (int i = 0; i < count; i++) {
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
		game_end.setGamePlayerNumber(count);
		game_end.setRoundOverType(0);
		game_end.setRoomOverType(0);
		game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间

		if (GRR != null) {
			// 胡的那张牌，是在哪一个组合里面
			game_end.setCountPickNiao(GRR._count_pick_niao);

			// 醒
			game_end.setWinLziFen(GRR._count_pick_niao);
			// 醒牌
			game_end.setEspecialTxtType(GRR._cards_data_niao[0]);

			// 牌型
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (GRR._win_order[i] == 1) {
					int typeCount = GRR._chi_hu_rights[i].type_count;
					for (int j = 0; j < typeCount; j++) {
						long type = GRR._chi_hu_rights[i].type_list[j];
						game_end.addChiHuRight(type);
					}
				}
			}

			// 王牌对应的实际牌数据
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (GRR._win_order[i] == 1) {
					for (int j = 0; j < magicCountWhenWin[i]; j++) {
						game_end.addQiang(magicToRealCard[i][j]);
					}
				}
			}

			game_end.setRoundOverType(1);
			game_end.setStartTime(GRR._start_time);

			game_end.setGameTypeIndex(GRR._game_type_index);
			roomResponse.setLeftCardCount(GRR._left_card_count);

			for (int i = 0; i < GRR._especial_card_count; i++) {
				game_end.addEspecialShowCards(GRR._especial_show_cards[i]);
			}

			GRR._end_type = reason;

			load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);
			game_end.setGamePlayerNumber(getTablePlayerNumber());
			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);
			game_end.setLeftCardCount(GRR._left_card_count);
			int cards[] = new int[GRR._left_card_count];
			int k = 0;
			int left_card_count = GRR._left_card_count;
			for (int i = _all_card_len - GRR._left_card_count; i < _all_card_len; i++) {
				cards[k] = _repertory_card[_all_card_len - left_card_count];
				game_end.addCardsList(cards[k]);

				k++;
				left_card_count--;
			}
			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				game_end.addHuResult(GRR._hu_result[i]);

				Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_HH_COUNT; j++) {
					hc.addItem(GRR._chi_hu_card[i][j]);
				}

				game_end.addHuCardData(GRR._chi_hu_card[i][0]);
				game_end.addHuCardArray(hc);

				GRR._game_score[i] += magicScore[i];

				_player_result.game_score[i] += GRR._game_score[i];
			}

			long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

			set_result_describe(seat_index);
			if (has_rule(GameConstants.GAME_RULE_DI_HUANG_FAN)) {
				if (reason == GameConstants.Game_End_DRAW) {
					_huang_zhang_count++;
				} else {
					_huang_zhang_count = 0;
				}
			}

			for (int i = 0; i < count; i++) {
				GRR._card_count[i] = _logic.switch_to_cards_data_yongzhou(GRR._cards_index[i], GRR._cards_data[i]);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {

					cs.addItem(GRR._cards_data[i][j]);
				}
				game_end.addCardsData(cs);// 牌
				WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();

				if (_hu_weave_count[i] > 0 && GRR._win_order[i] == 1) {
					for (int j = 0; j < _hu_weave_count[i]; j++) {
						WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();

						weaveItem_item.setProvidePlayer(_hu_weave_items[i][j].provide_player);
						weaveItem_item.setPublicCard(_hu_weave_items[i][j].public_card);
						weaveItem_item.setWeaveKind(_hu_weave_items[i][j].weave_kind);
						weaveItem_item.setHuXi(_hu_weave_items[i][j].hu_xi);

						int tmpCount = _hu_weave_items[i][j].weave_card_count;
						for (int x = 0; x < tmpCount; x++) {
							weaveItem_item.addWeaveCard(_hu_weave_items[i][j].weave_card[x]);
						}

						weaveItem_array.addWeaveItem(weaveItem_item);
					}
				} else {
					for (int j = 0; j < GRR._weave_count[i]; j++) {
						WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();

						weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
						weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
						weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
						weaveItem_item.setHuXi(GRR._weave_items[i][j].hu_xi);

						int[] tmpCards = new int[4];
						int tmpCount = _logic.get_weave_card(GRR._weave_items[i][j].weave_kind, GRR._weave_items[i][j].center_card, tmpCards);

						for (int x = 0; x < tmpCount; x++) {
							weaveItem_item.addWeaveCard(tmpCards[x]);
						}

						weaveItem_array.addWeaveItem(weaveItem_item);
					}
				}
				game_end.addWeaveItemArray(weaveItem_array);

				GRR._start_hu_right[i].get_right_data(rv);
				game_end.addStartHuRight(rv[0]);
				game_end.addProvidePlayer(GRR._provider[i]);

				// 胡息
				game_end.addJettonScore(_hu_xi[i]);
				// 囤数
				game_end.addCardType(_tun_shu[i]);
				// 番数
				game_end.addGangScore(_fan_shu[i]);
				// 总囤数
				game_end.addStartHuScore(total_tun_shu[i]);
				// 输赢分
				game_end.addGameScore(GRR._game_score[i]);

				game_end.addResultDes(GRR._result_des[i]);
				game_end.addWinOrder(GRR._win_order[i]);

				Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < count; j++) {
					lfs.addItem(GRR._lost_fan_shu[i][j]);
				}

				game_end.addLostFanShu(lfs);
			}
		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round) {// 局数到了
				end = true;
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(process_player_result(reason));

				Score_Record.Builder sr = Score_Record.newBuilder();
				if (getTablePlayerNumber() == 2) {
					for (int j = 0; j < _cur_round; j++) {
						sr.addScorePlayer1(all_game_round_score[0][j]);
						sr.addScorePlayer2(all_game_round_score[1][j]);
					}
					sr.setScoreTotal1(total_score[0]);
					sr.setScoreTotal2(total_score[1]);
				} else if (getTablePlayerNumber() == 3) {
					for (int j = 0; j < _cur_round; j++) {
						sr.addScorePlayer1(all_game_round_score[0][j]);
						sr.addScorePlayer2(all_game_round_score[1][j]);
						sr.addScorePlayer3(all_game_round_score[2][j]);
					}
					sr.setScoreTotal1(total_score[0]);
					sr.setScoreTotal2(total_score[1]);
					sr.setScoreTotal3(total_score[2]);
				} else if (getTablePlayerNumber() == 4) {
					for (int j = 0; j < _cur_round; j++) {
						sr.addScorePlayer1(all_game_round_score[0][j]);
						sr.addScorePlayer2(all_game_round_score[1][j]);
						sr.addScorePlayer3(all_game_round_score[2][j]);
						sr.addScorePlayer4(all_game_round_score[3][j]);
					}
					sr.setScoreTotal1(total_score[0]);
					sr.setScoreTotal2(total_score[1]);
					sr.setScoreTotal3(total_score[2]);
					sr.setScoreTotal4(total_score[3]);
				}

				game_end.setCommResponse(PBUtil.toByteString(sr));
			} else {
				// 确定下局庄家
				// 以后谁胡牌，下局谁做庄。
				// 流局,庄家下一个
				// 通炮,放炮的玩家
			}
		} else if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM) {
			end = true;
			real_reason = GameConstants.Game_End_DRAW;// 刘局
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(process_player_result(reason));

			Score_Record.Builder sr = Score_Record.newBuilder();
			if (getTablePlayerNumber() == 2) {
				for (int j = 0; j < _cur_round; j++) {
					sr.addScorePlayer1(all_game_round_score[0][j]);
					sr.addScorePlayer2(all_game_round_score[1][j]);
				}
				sr.setScoreTotal1(total_score[0]);
				sr.setScoreTotal2(total_score[1]);
			} else if (getTablePlayerNumber() == 3) {
				for (int j = 0; j < _cur_round; j++) {
					sr.addScorePlayer1(all_game_round_score[0][j]);
					sr.addScorePlayer2(all_game_round_score[1][j]);
					sr.addScorePlayer3(all_game_round_score[2][j]);
				}
				sr.setScoreTotal1(total_score[0]);
				sr.setScoreTotal2(total_score[1]);
				sr.setScoreTotal3(total_score[2]);
			} else if (getTablePlayerNumber() == 4) {
				for (int j = 0; j < _cur_round; j++) {
					sr.addScorePlayer1(all_game_round_score[0][j]);
					sr.addScorePlayer2(all_game_round_score[1][j]);
					sr.addScorePlayer3(all_game_round_score[2][j]);
					sr.addScorePlayer4(all_game_round_score[3][j]);
				}
				sr.setScoreTotal1(total_score[0]);
				sr.setScoreTotal2(total_score[1]);
				sr.setScoreTotal3(total_score[2]);
				sr.setScoreTotal4(total_score[3]);
			}

			game_end.setCommResponse(PBUtil.toByteString(sr));
		}
		game_end.setEndType(real_reason);

		roomResponse.setGameEnd(game_end);

		send_response_to_room(roomResponse);

		record_game_round(game_end, real_reason);

		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT || reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < getTablePlayerNumber(); j++) {
				Player player = get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");

			}
		}

		if (end) {
			PlayerServiceImpl.getInstance().delRoomId(getRoom_id());
		}
		if (!is_sys()) {
			GRR = null;
		}

		return false;
	}

	@Override
	public void on_init_table(int game_type_index, int game_rule_index, int game_round) {
		super.on_init_table(game_type_index, game_rule_index, game_round);

		_handler_dispath_card = new HandlerDispatchCard_YongZhou();
		_handler_out_card_operate = new HandlerOutCardOperate_YongZhou();
		_handler_gang = new HandlerGang_YongZhou();
		_handler_chi_peng = new HandlerChiPeng_YongZhou();
		_handler_chuli_firstcards = new HandlerDispatchFirstCard_YongZhou();

		all_game_round_score = new int[getTablePlayerNumber()][32];
		total_score = new int[getTablePlayerNumber()];

		has_rule_fan_xing = has_rule(Constants_YongZhou.GAME_RULE_FAN_XING);
	}

	public int analyse_wang_zha(int cards_index[], WeaveItem weaveItems[], int weaveCount, int seat_index, int provider_index, int cur_card,
			ChiHuRight chiHuRight, int card_type, int[] hu_xi_hh, boolean dispatch) {
		_hu_xi[seat_index] = 0;

		if (_is_xiang_gong[seat_index] == true) {
			return GameConstants.WIK_NULL;
		}

		if (_ti_mul_long[seat_index] > 0) {
			return GameConstants.WIK_NULL;
		}

		int cbCardIndexTemp[] = new int[Constants_YongZhou.MAX_CARD_INDEX];
		for (int i = 0; i < Constants_YongZhou.MAX_CARD_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		int cur_index = -1;
		if (cur_card != GameConstants.INVALID_VALUE) {
			cur_index = _logic.switch_to_card_index_yongzhou(cur_card);
			cbCardIndexTemp[cur_index]++;
		}

		int magicCount = cbCardIndexTemp[Constants_YongZhou.MAX_CARD_INDEX - 1];
		int handMagicCount = cbCardIndexTemp[Constants_YongZhou.MAX_CARD_INDEX - 1];
		if (cur_card == Constants_YongZhou.MAGIC_CARD)
			handMagicCount--;

		if (cur_card != GameConstants.INVALID_VALUE) {
			if (handMagicCount >= 3) {
				int xingPai = get_real_xing_pai(magicCount);

				int qiHu = get_basic_hu_xi();

				boolean hasHongZhuanHei = has_rule(Constants_YongZhou.GAME_RULE_HONG_ZHUAN_ZHU_HEI);

				int qiHuFan = get_qi_hu_fan(magicCount);

				Solution bestSolution = new Solution(0);
				AnalyseUtil analyseUtil = new AnalyseUtil(qiHu, xingPai, qiHuFan, weaveCount, weaveItems, hasHongZhuanHei, has_rule_fan_xing);

				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO)
					analyseUtil.setHasZiMo(true);

				int luoDiPaiHuXi = 0; // 落地牌胡息
				for (int i = 0; i < weaveCount; i++) {
					luoDiPaiHuXi += _logic.get_analyse_hu_xi(GRR._weave_items[seat_index][i].weave_kind, GRR._weave_items[seat_index][i].center_card);
				}

				List<Integer> cardList = new ArrayList<>();

				// 判断能否王炸
				cbCardIndexTemp[cur_index]--;
				cbCardIndexTemp[Constants_YongZhou.MAX_CARD_INDEX - 1] -= 3;

				if (is_wang_zha_others_card) {
					luoDiPaiHuXi += 9;
				} else if (cur_index == Constants_YongZhou.MAX_CARD_INDEX - 1 || cur_index >= 10) {
					luoDiPaiHuXi += 12; // 王牌或大字牌
				} else {
					luoDiPaiHuXi += 9;
				}

				if (cur_index == Constants_YongZhou.MAGIC_CARD_INDEX)
					analyseUtil.setHasWangZhaWang(true);
				else
					analyseUtil.setHasWangZha(true);

				List<Integer> addedCards = new ArrayList<>();
				addedCards.add(cur_card);
				addedCards.add(Constants_YongZhou.MAGIC_CARD);
				addedCards.add(Constants_YongZhou.MAGIC_CARD);
				addedCards.add(Constants_YongZhou.MAGIC_CARD);
				analyseUtil.addedCards = addedCards;

				int[] handCards = new int[GameConstants.MAX_YONG_ZHOU_HH_COUNT];
				int cardCount = _logic.switch_to_cards_data_yongzhou(cbCardIndexTemp, handCards);

				for (int j = 0; j < cardCount; j++) {
					cardList.add(handCards[j]);
				}

				boolean tmpBValue = analyseUtil.getSolution(cardList, -1, luoDiPaiHuXi);
				if (tmpBValue) {
					bestSolution = analyseUtil.getBestSolution();

					if (bestSolution.totalHuXi < qiHu) {
						chiHuRight.set_empty();
						return 0;
					}

					hu_xi_hh[0] = bestSolution.totalHuXi;

					if (cur_card == Constants_YongZhou.MAGIC_CARD) {
						chiHuRight.opr_or(Constants_YongZhou.CHR_WANG_ZHA_WANG);
					} else {
						chiHuRight.opr_or(Constants_YongZhou.CHR_WANG_ZHA);
					}

					chiHuRight.opr_or(Constants_YongZhou.CHR_CHI_HU);

					_wang_zha_hu_weave_count[seat_index] = 0;
					int count = _wang_zha_hu_weave_count[seat_index];

					for (int i = 0; i < weaveCount; i++) {
						count = _wang_zha_hu_weave_count[seat_index];
						_wang_zha_hu_weave_items[seat_index][count].weave_kind = weaveItems[i].weave_kind;
						_wang_zha_hu_weave_items[seat_index][count].center_card = weaveItems[i].center_card;
						_wang_zha_hu_weave_items[seat_index][count].hu_xi = _logic.get_weave_hu_xi(_wang_zha_hu_weave_items[seat_index][count]);
						_wang_zha_hu_weave_items[seat_index][count].weave_card_count = _logic.get_weave_card(weaveItems[i].weave_kind,
								weaveItems[i].center_card, _wang_zha_hu_weave_items[seat_index][count].weave_card);
						_wang_zha_hu_weave_count[seat_index]++;
					}

					count = _wang_zha_hu_weave_count[seat_index];
					_wang_zha_hu_weave_items[seat_index][count].weave_kind = GameConstants.WIK_TI_LONG;
					_wang_zha_hu_weave_items[seat_index][count].center_card = cur_card;
					if (bestSolution.smallCardTypeMoreScore || is_wang_zha_others_card) {
						_wang_zha_hu_weave_items[seat_index][count].hu_xi = 9;
					} else if (cur_index == Constants_YongZhou.MAX_CARD_INDEX - 1 || cur_index >= 10) {
						_wang_zha_hu_weave_items[seat_index][count].hu_xi = 12;
					} else {
						_wang_zha_hu_weave_items[seat_index][count].hu_xi = 9;
					}
					_wang_zha_hu_weave_items[seat_index][count].weave_card_count = 4;
					_wang_zha_hu_weave_items[seat_index][count].weave_card[0] = cur_card;
					_wang_zha_hu_weave_items[seat_index][count].weave_card[1] = Constants_YongZhou.MAGIC_CARD;
					_wang_zha_hu_weave_items[seat_index][count].weave_card[2] = Constants_YongZhou.MAGIC_CARD;
					_wang_zha_hu_weave_items[seat_index][count].weave_card[3] = Constants_YongZhou.MAGIC_CARD;

					_wang_zha_hu_weave_count[seat_index]++;

					for (WeaveInfo wInfo : bestSolution.weaveInfoList) {
						count = _wang_zha_hu_weave_count[seat_index];
						_wang_zha_hu_weave_items[seat_index][count].weave_kind = wInfo.triple.getSecond();
						_wang_zha_hu_weave_items[seat_index][count].hu_xi = wInfo.triple.getFirst();
						int tmpCount = _wang_zha_hu_weave_items[seat_index][count].weave_card_count = wInfo.cardPositions.size();

						for (int x = 0; x < tmpCount; x++) {
							_wang_zha_hu_weave_items[seat_index][count].weave_card[x] = cardList.get(wInfo.cardPositions.get(x));
						}

						_wang_zha_hu_weave_count[seat_index]++;
					}

					int hongPaiCount = getWangZhaHongPaiCount(seat_index);

					wangZhaMagicCountWhenWin[seat_index] = 0;
					for (int card : bestSolution.magicToRealCard) {
						wangZhaMagicToRealCard[seat_index][wangZhaMagicCountWhenWin[seat_index]++] = card;
					}

					if (wangZhaMagicCountWhenWin[seat_index] > 0) {
						for (int i = 0; i < wangZhaMagicCountWhenWin[seat_index]; i++) {
							if (!_logic.color_hei(wangZhaMagicToRealCard[seat_index][i])) {
								hongPaiCount++;
							}
						}
					}

					if (hongPaiCount == 0) {
						chiHuRight.opr_or(Constants_YongZhou.CHR_HEI_HU);
					} else if (hongPaiCount >= 10) {
						if (has_rule(Constants_YongZhou.GAME_RULE_HONG_ZHUAN_ZHU_HEI)) {
							if (hongPaiCount >= 13 && hongPaiCount <= 15) {
								chiHuRight.opr_or(Constants_YongZhou.CHR_HONG_ZHUAN_DIAN);
							} else if (hongPaiCount > 15) {
								chiHuRight.opr_or(Constants_YongZhou.CHR_HONG_ZHUAN_HEI);
							} else {
								chiHuRight.opr_or(Constants_YongZhou.CHR_HONG_HU);
							}
						} else {
							chiHuRight.opr_or(Constants_YongZhou.CHR_HONG_HU);
						}
					} else if (hongPaiCount == 1) {
						chiHuRight.opr_or(Constants_YongZhou.CHR_DIAN_HU);
					}

					if (has_rule(Constants_YongZhou.GAME_RULE_AN_FAN_XIAN_HU) && magicCount > 0) {
						int fanShu = get_fan_shu(chiHuRight);
						if ((magicCount == 1 && fanShu < 2) || (magicCount == 2 && fanShu < 4) || (magicCount == 3 && fanShu < 8)
								|| (magicCount == 4 && fanShu < 16)) {
							chiHuRight.set_empty();
							return 0;
						}
					}

					return GameConstants.WIK_WANG_ZHA;
				} else {
					chiHuRight.set_empty();
					return 0;
				}
			}
		}

		return 0;
	}

	public int analyse_wang_chuang(int cards_index[], WeaveItem weaveItems[], int weaveCount, int seat_index, int provider_index, int cur_card,
			ChiHuRight chiHuRight, int card_type, int[] hu_xi_hh, boolean dispatch) {
		_hu_xi[seat_index] = 0;

		if (_is_xiang_gong[seat_index] == true) {
			return GameConstants.WIK_NULL;
		}

		if (_ti_mul_long[seat_index] > 0) {
			return GameConstants.WIK_NULL;
		}

		int cbCardIndexTemp[] = new int[Constants_YongZhou.MAX_CARD_INDEX];
		for (int i = 0; i < Constants_YongZhou.MAX_CARD_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		int cur_index = -1;
		if (cur_card != GameConstants.INVALID_VALUE) {
			cur_index = _logic.switch_to_card_index_yongzhou(cur_card);
			cbCardIndexTemp[cur_index]++;
		}

		int magicCount = cbCardIndexTemp[Constants_YongZhou.MAX_CARD_INDEX - 1];
		int handMagicCount = cbCardIndexTemp[Constants_YongZhou.MAX_CARD_INDEX - 1];
		if (cur_card == Constants_YongZhou.MAGIC_CARD)
			handMagicCount--;

		int winType = 0;

		List<Integer> cardList = new ArrayList<>();
		List<Integer> cardList_10 = new ArrayList<>();
		List<Integer> finalCardList_10 = new ArrayList<>();

		int xingPai = get_real_xing_pai(magicCount);

		int qiHu = get_basic_hu_xi();

		boolean hasHongZhuanHei = has_rule(Constants_YongZhou.GAME_RULE_HONG_ZHUAN_ZHU_HEI);

		int qiHuFan = get_qi_hu_fan(magicCount);

		Solution bestSolution = new Solution(0);
		AnalyseUtil analyseUtil = new AnalyseUtil(qiHu, xingPai, qiHuFan, weaveCount, weaveItems, hasHongZhuanHei, has_rule_fan_xing);

		if (cur_index == Constants_YongZhou.MAGIC_CARD_INDEX)
			analyseUtil.setHasWangChuangWang(true);
		else
			analyseUtil.setHasWangChuang(true);

		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO)
			analyseUtil.setHasZiMo(true);

		int luoDiPaiHuXi = 0; // 落地牌胡息
		for (int i = 0; i < weaveCount; i++) {
			luoDiPaiHuXi += _logic.get_analyse_hu_xi(GRR._weave_items[seat_index][i].weave_kind, GRR._weave_items[seat_index][i].center_card);
		}

		int specialMaxScoreWeaveIndex = -1;

		if (cur_card != GameConstants.INVALID_VALUE) {
			if (handMagicCount >= 2) {
				// 判断能否王闯
				cbCardIndexTemp[cur_index]--;
				cbCardIndexTemp[Constants_YongZhou.MAX_CARD_INDEX - 1] -= 2;

				if (is_wang_zha_others_card) {
					luoDiPaiHuXi += 3;
				} else if (cur_index == Constants_YongZhou.MAX_CARD_INDEX - 1 || cur_index >= 10) {
					luoDiPaiHuXi += 6; // 王牌或大字牌
				} else {
					luoDiPaiHuXi += 3;
				}

				List<Integer> addedCards = new ArrayList<>();
				addedCards.add(cur_card);
				addedCards.add(Constants_YongZhou.MAGIC_CARD);
				addedCards.add(Constants_YongZhou.MAGIC_CARD);
				analyseUtil.addedCards = addedCards;

				int[] handCards = new int[GameConstants.MAX_YONG_ZHOU_HH_COUNT];
				int cardCount = _logic.switch_to_cards_data_yongzhou(cbCardIndexTemp, handCards);

				for (int j = 0; j < cardCount; j++) {
					cardList.add(handCards[j]);
				}

				boolean tmpBValue = analyseUtil.getSolution(cardList, -1, luoDiPaiHuXi);
				if (tmpBValue) {
					Solution tmpSolution = analyseUtil.getBestSolution();

					if (tmpSolution.totalScore > bestSolution.totalScore) {
						bestSolution = tmpSolution;
						winType = 1;
					}
				}

				cbCardIndexTemp[cur_index]++;
				cbCardIndexTemp[Constants_YongZhou.MAX_CARD_INDEX - 1] += 2;
				if (is_wang_zha_others_card) {
					luoDiPaiHuXi -= 3;
				} else if (cur_index == Constants_YongZhou.MAX_CARD_INDEX - 1 || cur_index >= 10) {
					luoDiPaiHuXi -= 6; // 王牌或大字牌
				} else {
					luoDiPaiHuXi -= 3;
				}
				analyseUtil.addedCards = null;
			}

			if (_long_count[seat_index] == 0 && handMagicCount >= 3 && weaveCount > 0) {
				for (int i = 0; i < weaveCount; i++) {
					if (weaveItems[i].weave_kind == GameConstants.WIK_WEI || weaveItems[i].weave_kind == GameConstants.WIK_CHOU_WEI) {
						cbCardIndexTemp[cur_index]--;
						cbCardIndexTemp[Constants_YongZhou.MAX_CARD_INDEX - 1] -= 3;

						if (is_wang_zha_others_card) {
							luoDiPaiHuXi += 3;
						} else if (cur_index == Constants_YongZhou.MAX_CARD_INDEX - 1 || cur_index >= 10) {
							luoDiPaiHuXi += 6; // 王牌或大字牌
						} else {
							luoDiPaiHuXi += 3;
						}

						List<Integer> addedCards = new ArrayList<>();
						analyseUtil.setWeiAddedCard(weaveItems[i].center_card);
						addedCards.add(cur_card);
						addedCards.add(Constants_YongZhou.MAGIC_CARD);
						addedCards.add(Constants_YongZhou.MAGIC_CARD);
						analyseUtil.addedCards = addedCards;

						luoDiPaiHuXi += 6;

						int[] handCards = new int[GameConstants.MAX_YONG_ZHOU_HH_COUNT];
						int cardCount = _logic.switch_to_cards_data_yongzhou(cbCardIndexTemp, handCards);

						for (int j = 0; j < cardCount; j++) {
							cardList_10.add(handCards[j]);
						}

						boolean tmpBValue = analyseUtil.getSolution(cardList_10, cur_index, luoDiPaiHuXi);
						if (tmpBValue) {
							Solution tmpSolution = analyseUtil.getBestSolution();

							if (tmpSolution.totalScore > bestSolution.totalScore) {
								bestSolution = tmpSolution;

								winType = 10;
								specialMaxScoreWeaveIndex = i;

								finalCardList_10 = new ArrayList<>(cardList_10);
							}
						}

						cbCardIndexTemp[cur_index]++;
						cbCardIndexTemp[Constants_YongZhou.MAX_CARD_INDEX - 1] += 3;
						if (is_wang_zha_others_card) {
							luoDiPaiHuXi -= 3;
						} else if (cur_index == Constants_YongZhou.MAX_CARD_INDEX - 1 || cur_index >= 10) {
							luoDiPaiHuXi -= 6; // 王牌或大字牌
						} else {
							luoDiPaiHuXi -= 3;
						}
						analyseUtil.addedCards = null;
						analyseUtil.setWeiAddedCard(0);
						luoDiPaiHuXi -= 6;
					}
				}
			}
		}

		if (winType == 1 || winType == 10) {
			hu_xi_hh[0] = bestSolution.totalHuXi;

			if (cur_card == Constants_YongZhou.MAGIC_CARD) {
				chiHuRight.opr_or(Constants_YongZhou.CHR_WANG_CHUANG_WANG);
			} else {
				chiHuRight.opr_or(Constants_YongZhou.CHR_WANG_CHUANG);
			}

			chiHuRight.opr_or(Constants_YongZhou.CHR_CHI_HU);

			_wang_chuang_hu_weave_count[seat_index] = 0;
			int count = _wang_chuang_hu_weave_count[seat_index];

			for (int i = 0; i < weaveCount; i++) {
				count = _wang_chuang_hu_weave_count[seat_index];

				if (i == specialMaxScoreWeaveIndex && winType == 10) {
					_wang_chuang_hu_weave_items[seat_index][count].weave_kind = GameConstants.WIK_TI_LONG;
					_wang_chuang_hu_weave_items[seat_index][count].center_card = weaveItems[i].center_card;
					_wang_chuang_hu_weave_items[seat_index][count].hu_xi = _logic.get_weave_hu_xi(_wang_chuang_hu_weave_items[seat_index][count]);
					_wang_chuang_hu_weave_items[seat_index][count].weave_card_count = _logic.get_weave_card(weaveItems[i].weave_kind,
							weaveItems[i].center_card, _wang_chuang_hu_weave_items[seat_index][count].weave_card);
					_wang_chuang_hu_weave_items[seat_index][count].weave_card_count++;
					_wang_chuang_hu_weave_items[seat_index][count].weave_card[3] = Constants_YongZhou.MAGIC_CARD;
					_wang_chuang_hu_weave_count[seat_index]++;
				} else {
					_wang_chuang_hu_weave_items[seat_index][count].weave_kind = weaveItems[i].weave_kind;
					_wang_chuang_hu_weave_items[seat_index][count].center_card = weaveItems[i].center_card;
					_wang_chuang_hu_weave_items[seat_index][count].hu_xi = _logic.get_weave_hu_xi(_wang_chuang_hu_weave_items[seat_index][count]);
					_wang_chuang_hu_weave_items[seat_index][count].weave_card_count = _logic.get_weave_card(weaveItems[i].weave_kind,
							weaveItems[i].center_card, _wang_chuang_hu_weave_items[seat_index][count].weave_card);
					_wang_chuang_hu_weave_count[seat_index]++;
				}
			}

			count = _wang_chuang_hu_weave_count[seat_index];
			_wang_chuang_hu_weave_items[seat_index][count].weave_kind = GameConstants.WIK_KAN;
			_wang_chuang_hu_weave_items[seat_index][count].center_card = cur_card;
			if (bestSolution.smallCardTypeMoreScore || is_wang_zha_others_card) {
				_wang_chuang_hu_weave_items[seat_index][count].hu_xi = 3;
			} else if (cur_index == Constants_YongZhou.MAX_CARD_INDEX - 1 || cur_index >= 10) {
				_wang_chuang_hu_weave_items[seat_index][count].hu_xi = 6;
			} else {
				_wang_chuang_hu_weave_items[seat_index][count].hu_xi = 3;
			}
			_wang_chuang_hu_weave_items[seat_index][count].weave_card_count = 3;
			_wang_chuang_hu_weave_items[seat_index][count].weave_card[0] = cur_card;
			_wang_chuang_hu_weave_items[seat_index][count].weave_card[1] = Constants_YongZhou.MAGIC_CARD;
			_wang_chuang_hu_weave_items[seat_index][count].weave_card[2] = Constants_YongZhou.MAGIC_CARD;

			_wang_chuang_hu_weave_count[seat_index]++;

			for (WeaveInfo wInfo : bestSolution.weaveInfoList) {
				count = _wang_chuang_hu_weave_count[seat_index];
				_wang_chuang_hu_weave_items[seat_index][count].weave_kind = wInfo.triple.getSecond();
				_wang_chuang_hu_weave_items[seat_index][count].hu_xi = wInfo.triple.getFirst();
				int tmpCount = _wang_chuang_hu_weave_items[seat_index][count].weave_card_count = wInfo.cardPositions.size();

				for (int x = 0; x < tmpCount; x++) {
					if (winType == 10) {
						_wang_chuang_hu_weave_items[seat_index][count].weave_card[x] = finalCardList_10.get(wInfo.cardPositions.get(x));
					} else {
						_wang_chuang_hu_weave_items[seat_index][count].weave_card[x] = cardList.get(wInfo.cardPositions.get(x));
					}
				}

				_wang_chuang_hu_weave_count[seat_index]++;
			}

			int hongPaiCount = getWangChuangHongPaiCount(seat_index);

			wangChuangMagicCountWhenWin[seat_index] = 0;
			for (int card : bestSolution.magicToRealCard) {
				wangChuangMagicToRealCard[seat_index][wangChuangMagicCountWhenWin[seat_index]++] = card;
			}

			if (wangChuangMagicCountWhenWin[seat_index] > 0) {
				for (int i = 0; i < wangChuangMagicCountWhenWin[seat_index]; i++) {
					if (!_logic.color_hei(wangChuangMagicToRealCard[seat_index][i])) {
						hongPaiCount++;
					}
				}
			}

			if (hongPaiCount == 0) {
				chiHuRight.opr_or(Constants_YongZhou.CHR_HEI_HU);
			} else if (hongPaiCount >= 10) {
				if (has_rule(Constants_YongZhou.GAME_RULE_HONG_ZHUAN_ZHU_HEI)) {
					if (hongPaiCount >= 13 && hongPaiCount <= 15) {
						chiHuRight.opr_or(Constants_YongZhou.CHR_HONG_ZHUAN_DIAN);
					} else if (hongPaiCount > 15) {
						chiHuRight.opr_or(Constants_YongZhou.CHR_HONG_ZHUAN_HEI);
					} else {
						chiHuRight.opr_or(Constants_YongZhou.CHR_HONG_HU);
					}
				} else {
					chiHuRight.opr_or(Constants_YongZhou.CHR_HONG_HU);
				}
			} else if (hongPaiCount == 1) {
				chiHuRight.opr_or(Constants_YongZhou.CHR_DIAN_HU);
			}

			if (has_rule(Constants_YongZhou.GAME_RULE_AN_FAN_XIAN_HU) && magicCount > 0) {
				int fanShu = get_fan_shu(chiHuRight);
				if ((magicCount == 1 && fanShu < 2) || (magicCount == 2 && fanShu < 4) || (magicCount == 3 && fanShu < 8)
						|| (magicCount == 4 && fanShu < 16)) {
					chiHuRight.set_empty();
					return 0;
				}
			}

			return GameConstants.WIK_WANG_CHUANG;
		}

		chiHuRight.set_empty();
		return 0;
	}

	public int analyse_wang_diao(int cards_index[], WeaveItem weaveItems[], int weaveCount, int seat_index, int provider_index, int cur_card,
			ChiHuRight chiHuRight, int card_type, int[] hu_xi_hh, boolean dispatch) {
		_hu_xi[seat_index] = 0;

		if (_is_xiang_gong[seat_index] == true) {
			return GameConstants.WIK_NULL;
		}

		if (_ti_mul_long[seat_index] > 0) {
			return GameConstants.WIK_NULL;
		}

		int cbCardIndexTemp[] = new int[Constants_YongZhou.MAX_CARD_INDEX];
		for (int i = 0; i < Constants_YongZhou.MAX_CARD_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		int cur_index = -1;
		if (cur_card != GameConstants.INVALID_VALUE) {
			cur_index = _logic.switch_to_card_index_yongzhou(cur_card);
			cbCardIndexTemp[cur_index]++;
		}

		int magicCount = cbCardIndexTemp[Constants_YongZhou.MAX_CARD_INDEX - 1];
		int handMagicCount = cbCardIndexTemp[Constants_YongZhou.MAX_CARD_INDEX - 1];
		if (cur_card == Constants_YongZhou.MAGIC_CARD)
			handMagicCount--;

		int winType = 0;

		List<Integer> cardList = new ArrayList<>();
		List<Integer> cardList_10 = new ArrayList<>();
		List<Integer> finalCardList_10 = new ArrayList<>();

		int xingPai = get_real_xing_pai(magicCount);

		int qiHu = get_basic_hu_xi();

		boolean hasHongZhuanHei = has_rule(Constants_YongZhou.GAME_RULE_HONG_ZHUAN_ZHU_HEI);

		int qiHuFan = get_qi_hu_fan(magicCount);

		Solution bestSolution = new Solution(0);
		AnalyseUtil analyseUtil = new AnalyseUtil(qiHu, xingPai, qiHuFan, weaveCount, weaveItems, hasHongZhuanHei, has_rule_fan_xing);

		analyseUtil.setWangDiao(true);

		if (cur_index == Constants_YongZhou.MAGIC_CARD_INDEX)
			analyseUtil.setHasWangDiaoWang(true);
		else
			analyseUtil.setHasWangDiao(true);

		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO)
			analyseUtil.setHasZiMo(true);

		int luoDiPaiHuXi = 0; // 落地牌胡息
		for (int i = 0; i < weaveCount; i++) {
			luoDiPaiHuXi += _logic.get_analyse_hu_xi(GRR._weave_items[seat_index][i].weave_kind, GRR._weave_items[seat_index][i].center_card);
		}

		int specialMaxScoreWeaveIndex = -1;

		if (cur_card != GameConstants.INVALID_VALUE) {
			if (handMagicCount >= 1) {
				// 判断能否王钓
				cbCardIndexTemp[cur_index]--;
				cbCardIndexTemp[Constants_YongZhou.MAX_CARD_INDEX - 1] -= 1;

				List<Integer> addedCards = new ArrayList<>();
				addedCards.add(cur_card);
				addedCards.add(Constants_YongZhou.MAGIC_CARD);
				analyseUtil.addedCards = addedCards;

				int[] handCards = new int[GameConstants.MAX_YONG_ZHOU_HH_COUNT];
				int cardCount = _logic.switch_to_cards_data_yongzhou(cbCardIndexTemp, handCards);

				for (int j = 0; j < cardCount; j++) {
					cardList.add(handCards[j]);
				}

				boolean tmpBValue = analyseUtil.getSolution(cardList, -1, luoDiPaiHuXi);
				if (tmpBValue) {
					Solution tmpSolution = analyseUtil.getBestSolution();

					if (tmpSolution.totalScore > bestSolution.totalScore) {
						bestSolution = tmpSolution;
						winType = 1;
					}
				}

				cbCardIndexTemp[cur_index]++;
				cbCardIndexTemp[Constants_YongZhou.MAX_CARD_INDEX - 1] += 1;

				analyseUtil.addedCards = null;
			}

			if (_long_count[seat_index] == 0 && handMagicCount > 1 && weaveCount > 0) {
				for (int i = 0; i < weaveCount; i++) {
					if (weaveItems[i].weave_kind == GameConstants.WIK_WEI || weaveItems[i].weave_kind == GameConstants.WIK_CHOU_WEI) {
						// 如果落地牌有偎 让一张王牌变成倾
						cbCardIndexTemp[cur_index]--;
						cbCardIndexTemp[Constants_YongZhou.MAX_CARD_INDEX - 1] -= 2;

						List<Integer> addedCards = new ArrayList<>();
						analyseUtil.setWeiAddedCard(weaveItems[i].center_card);
						addedCards.add(cur_card);
						addedCards.add(Constants_YongZhou.MAGIC_CARD);
						analyseUtil.addedCards = addedCards;

						luoDiPaiHuXi += 6;

						int[] handCards = new int[GameConstants.MAX_YONG_ZHOU_HH_COUNT];
						int cardCount = _logic.switch_to_cards_data_yongzhou(cbCardIndexTemp, handCards);

						for (int j = 0; j < cardCount; j++) {
							cardList_10.add(handCards[j]);
						}

						boolean tmpBValue = analyseUtil.getSolution(cardList_10, cur_index, luoDiPaiHuXi);
						if (tmpBValue) {
							Solution tmpSolution = analyseUtil.getBestSolution();

							if (tmpSolution.totalScore > bestSolution.totalScore) {
								bestSolution = tmpSolution;

								winType = 10;
								specialMaxScoreWeaveIndex = i;

								finalCardList_10 = new ArrayList<>(cardList_10);
							}
						}

						cbCardIndexTemp[cur_index]++;
						cbCardIndexTemp[Constants_YongZhou.MAGIC_CARD_INDEX] += 2;
						analyseUtil.addedCards = null;
						analyseUtil.setWeiAddedCard(0);
						cardList_10.clear();
						luoDiPaiHuXi -= 6;
					}
				}
			}
		}

		if (winType == 1 || winType == 10) {
			hu_xi_hh[0] = bestSolution.totalHuXi;

			if (cur_card == Constants_YongZhou.MAGIC_CARD) {
				chiHuRight.opr_or(Constants_YongZhou.CHR_WANG_DIAO_WANG);
			} else {
				chiHuRight.opr_or(Constants_YongZhou.CHR_WANG_DIAO);
			}

			chiHuRight.opr_or(Constants_YongZhou.CHR_CHI_HU);

			_wang_diao_hu_weave_count[seat_index] = 0;
			int count = _wang_diao_hu_weave_count[seat_index];

			for (int i = 0; i < weaveCount; i++) {
				count = _wang_diao_hu_weave_count[seat_index];

				if (i == specialMaxScoreWeaveIndex && winType == 10) {
					_wang_diao_hu_weave_items[seat_index][count].weave_kind = GameConstants.WIK_TI_LONG;
					_wang_diao_hu_weave_items[seat_index][count].center_card = weaveItems[i].center_card;
					_wang_diao_hu_weave_items[seat_index][count].hu_xi = _logic.get_weave_hu_xi(_wang_diao_hu_weave_items[seat_index][count]);
					_wang_diao_hu_weave_items[seat_index][count].weave_card_count = _logic.get_weave_card(weaveItems[i].weave_kind,
							weaveItems[i].center_card, _wang_diao_hu_weave_items[seat_index][count].weave_card);
					_wang_diao_hu_weave_items[seat_index][count].weave_card_count++;
					_wang_diao_hu_weave_items[seat_index][count].weave_card[3] = Constants_YongZhou.MAGIC_CARD;
					_wang_diao_hu_weave_count[seat_index]++;
				} else {
					_wang_diao_hu_weave_items[seat_index][count].weave_kind = weaveItems[i].weave_kind;
					_wang_diao_hu_weave_items[seat_index][count].center_card = weaveItems[i].center_card;
					_wang_diao_hu_weave_items[seat_index][count].hu_xi = _logic.get_weave_hu_xi(_wang_diao_hu_weave_items[seat_index][count]);
					_wang_diao_hu_weave_items[seat_index][count].weave_card_count = _logic.get_weave_card(weaveItems[i].weave_kind,
							weaveItems[i].center_card, _wang_diao_hu_weave_items[seat_index][count].weave_card);
					_wang_diao_hu_weave_count[seat_index]++;
				}
			}

			count = _wang_diao_hu_weave_count[seat_index];
			_wang_diao_hu_weave_items[seat_index][count].weave_kind = GameConstants.WIK_DUI_ZI;
			_wang_diao_hu_weave_items[seat_index][count].center_card = cur_card;
			_wang_diao_hu_weave_items[seat_index][count].hu_xi = 0;
			_wang_diao_hu_weave_items[seat_index][count].weave_card_count = 2;
			_wang_diao_hu_weave_items[seat_index][count].weave_card[0] = cur_card;
			_wang_diao_hu_weave_items[seat_index][count].weave_card[1] = Constants_YongZhou.MAGIC_CARD;

			_wang_diao_hu_weave_count[seat_index]++;

			for (WeaveInfo wInfo : bestSolution.weaveInfoList) {
				count = _wang_diao_hu_weave_count[seat_index];
				_wang_diao_hu_weave_items[seat_index][count].weave_kind = wInfo.triple.getSecond();
				_wang_diao_hu_weave_items[seat_index][count].hu_xi = wInfo.triple.getFirst();
				int tmpCount = _wang_diao_hu_weave_items[seat_index][count].weave_card_count = wInfo.cardPositions.size();

				for (int x = 0; x < tmpCount; x++) {
					if (winType == 1) {
						_wang_diao_hu_weave_items[seat_index][count].weave_card[x] = cardList.get(wInfo.cardPositions.get(x));
					} else if (winType == 10) {
						_wang_diao_hu_weave_items[seat_index][count].weave_card[x] = finalCardList_10.get(wInfo.cardPositions.get(x));
					}
				}

				_wang_diao_hu_weave_count[seat_index]++;
			}

			int hongPaiCount = getWangDiaoHongPaiCount(seat_index);

			wangDiaoMagicCountWhenWin[seat_index] = 0;
			for (int card : bestSolution.magicToRealCard) {
				wangDiaoMagicToRealCard[seat_index][wangDiaoMagicCountWhenWin[seat_index]++] = card;
			}

			if (wangDiaoMagicCountWhenWin[seat_index] > 0) {
				for (int i = 0; i < wangDiaoMagicCountWhenWin[seat_index]; i++) {
					if (!_logic.color_hei(wangDiaoMagicToRealCard[seat_index][i])) {
						hongPaiCount++;
					}
				}
			}

			if (hongPaiCount == 0) {
				chiHuRight.opr_or(Constants_YongZhou.CHR_HEI_HU);
			} else if (hongPaiCount >= 10) {
				if (has_rule(Constants_YongZhou.GAME_RULE_HONG_ZHUAN_ZHU_HEI)) {
					if (hongPaiCount >= 13 && hongPaiCount <= 15) {
						chiHuRight.opr_or(Constants_YongZhou.CHR_HONG_ZHUAN_DIAN);
					} else if (hongPaiCount > 15) {
						chiHuRight.opr_or(Constants_YongZhou.CHR_HONG_ZHUAN_HEI);
					} else {
						chiHuRight.opr_or(Constants_YongZhou.CHR_HONG_HU);
					}
				} else {
					chiHuRight.opr_or(Constants_YongZhou.CHR_HONG_HU);
				}
			} else if (hongPaiCount == 1) {
				chiHuRight.opr_or(Constants_YongZhou.CHR_DIAN_HU);
			}

			if (has_rule(Constants_YongZhou.GAME_RULE_AN_FAN_XIAN_HU) && magicCount > 0) {
				int fanShu = get_fan_shu(chiHuRight);
				if ((magicCount == 1 && fanShu < 2) || (magicCount == 2 && fanShu < 4) || (magicCount == 3 && fanShu < 8)
						|| (magicCount == 4 && fanShu < 16)) {
					chiHuRight.set_empty();
					return 0;
				}
			}

			return GameConstants.WIK_WANG_DIAO;
		}

		chiHuRight.set_empty();
		return 0;
	}

	public int analyse_normal(int cards_index[], WeaveItem weaveItems[], int weaveCount, int seat_index, int provider_index, int cur_card,
			ChiHuRight chiHuRight, int card_type, int[] hu_xi_hh, boolean dispatch) {
		is_pao_hu[seat_index] = false;

		_hu_xi[seat_index] = 0;

		if (_is_xiang_gong[seat_index] == true) {
			return GameConstants.WIK_NULL;
		}

		if (_ti_mul_long[seat_index] > 0) {
			return GameConstants.WIK_NULL;
		}

		int cbChiHuKind = GameConstants.WIK_NULL;

		int cbCardIndexTemp[] = new int[Constants_YongZhou.MAX_CARD_INDEX];
		for (int i = 0; i < Constants_YongZhou.MAX_CARD_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		int cur_index = -1;
		if (cur_card != GameConstants.INVALID_VALUE && !is_qi_shou_analyse) {
			cur_index = _logic.switch_to_card_index_yongzhou(cur_card);
			cbCardIndexTemp[cur_index]++;
		}

		int magicCount = cbCardIndexTemp[Constants_YongZhou.MAX_CARD_INDEX - 1];

		if (magicCount > 0 && card_type != GameConstants.HU_CARD_TYPE_ZIMO)
			return 0;

		int xingPai = get_real_xing_pai(magicCount);

		int qiHu = get_basic_hu_xi();

		boolean hasHongZhuanHei = has_rule(Constants_YongZhou.GAME_RULE_HONG_ZHUAN_ZHU_HEI);

		int qiHuFan = get_qi_hu_fan(magicCount);

		Solution bestSolution = new Solution(0);
		AnalyseUtil analyseUtil = new AnalyseUtil(qiHu, xingPai, qiHuFan, weaveCount, weaveItems, hasHongZhuanHei, has_rule_fan_xing);

		if (is_qi_shou_analyse && has_rule(Constants_YongZhou.GAME_RULE_GEN_XING))
			analyseUtil.setQiShouGenXing(true);

		// 胡牌类型，1：破跑胡；2：碰胡；3：手牌有三张的破跑胡；4：正常胡；5：跑胡；6：王钓；7：王闯；8：王炸；9：萧牌胡牌并且王牌可以让萧变成倾；10：没跑没倾有碰偎有王牌
		// 判断能否用王牌让碰偎变成跑倾 然后胡牌
		int winType = 0;

		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO)
			analyseUtil.setHasZiMo(true);

		int luoDiPaiHuXi = 0; // 落地牌胡息
		for (int i = 0; i < weaveCount; i++) {
			luoDiPaiHuXi += _logic.get_analyse_hu_xi(GRR._weave_items[seat_index][i].weave_kind, GRR._weave_items[seat_index][i].center_card);
		}

		List<Integer> cardList_3 = new ArrayList<>();
		List<Integer> cardList_4 = new ArrayList<>();
		List<Integer> cardList_5 = new ArrayList<>();
		List<Integer> cardList_9 = new ArrayList<>();
		List<Integer> cardList_10 = new ArrayList<>();
		List<Integer> finalCardList_10 = new ArrayList<>();

		boolean bValue = false;

		int specialMaxScoreWeaveIndex = -1;

		if (cur_card != GameConstants.INVALID_VALUE && !is_qi_shou_analyse) {
			if (_long_count[seat_index] == 0 && magicCount > 0 && weaveCount > 0) {
				// 轮询所有的偎落地牌 得到一个最大牌型分
				for (int i = 0; i < weaveCount; i++) {
					if ((weaveItems[i].weave_kind == GameConstants.WIK_WEI || weaveItems[i].weave_kind == GameConstants.WIK_CHOU_WEI)
							&& dispatch == true) {
						// 1.如果落地牌有偎，让一张王牌变成倾
						analyseUtil.setWeiAddedCard(weaveItems[i].center_card);
						cbCardIndexTemp[Constants_YongZhou.MAGIC_CARD_INDEX]--;

						luoDiPaiHuXi += 6;

						int[] handCards = new int[GameConstants.MAX_YONG_ZHOU_HH_COUNT];
						int cardCount = _logic.switch_to_cards_data_yongzhou(cbCardIndexTemp, handCards);

						for (int j = 0; j < cardCount; j++) {
							cardList_10.add(handCards[j]);
						}

						boolean tmpBValue = analyseUtil.getSolution(cardList_10, cur_index, luoDiPaiHuXi);
						if (tmpBValue) {
							bValue = true;
							Solution tmpSolution = analyseUtil.getBestSolution();

							if (tmpSolution.totalScore > bestSolution.totalScore) {
								bestSolution = tmpSolution;
								// 10：没跑没倾有碰偎有王牌判断能否用王牌让碰偎变成跑倾 然后胡牌
								winType = 10;
								specialMaxScoreWeaveIndex = i;

								finalCardList_10 = new ArrayList<>(cardList_10);
							}
						}

						analyseUtil.setWeiAddedCard(0);
						cbCardIndexTemp[Constants_YongZhou.MAGIC_CARD_INDEX]++;
						cardList_10.clear();

						if (weaveItems[i].weave_kind == GameConstants.WIK_PENG) {
							if (weaveItems[i].center_card < 0x10) {
								luoDiPaiHuXi -= 5; // 1胡变6胡
							} else {
								luoDiPaiHuXi -= 6; // 3胡变9胡
							}
						} else {
							if (weaveItems[i].center_card < 0x10) {
								luoDiPaiHuXi -= 6; // 3胡变9胡
							} else {
								luoDiPaiHuXi -= 6; // 6胡变12胡
							}
						}
					}
				}
			}

			for (int i = 0; i < weaveCount; i++) {
				if ((cur_card == weaveItems[i].center_card)
						&& ((weaveItems[i].weave_kind == GameConstants.WIK_PENG || weaveItems[i].weave_kind == GameConstants.WIK_WEI
								|| weaveItems[i].weave_kind == GameConstants.WIK_CHOU_WEI) && dispatch == true)) {
					// 1.如果落地牌有碰或者偎，并且牌是从牌堆里翻出来的，判断是否‘跑成胡牌’
					cbCardIndexTemp[cur_index] = 0;

					if (weaveItems[i].weave_kind == GameConstants.WIK_PENG) {
						if (cur_index < 10) {
							luoDiPaiHuXi += 5; // 1胡变6胡
						} else {
							luoDiPaiHuXi += 6; // 3胡变9胡
						}
					} else {
						if (cur_index < 10) {
							luoDiPaiHuXi += 3; // 3胡变6胡
						} else {
							luoDiPaiHuXi += 3; // 6胡变9胡
						}
					}
					int tmpWik = weaveItems[i].weave_kind;
					weaveItems[i].weave_kind = GameConstants.WIK_PAO;

					int[] handCards = new int[GameConstants.MAX_YONG_ZHOU_HH_COUNT];
					int cardCount = _logic.switch_to_cards_data_yongzhou(cbCardIndexTemp, handCards);

					for (int j = 0; j < cardCount; j++) {
						cardList_5.add(handCards[j]);
					}

					boolean tmpBValue = analyseUtil.getSolution(cardList_5, cur_index, luoDiPaiHuXi);
					if (tmpBValue) {
						bValue = true;
						Solution tmpSolution = analyseUtil.getBestSolution();

						if (tmpSolution.totalScore > bestSolution.totalScore) {
							bestSolution = tmpSolution;
							winType = 5; // 5：跑胡
						}
					}

					weaveItems[i].weave_kind = tmpWik;
					cbCardIndexTemp[cur_index] = 1;
					if (weaveItems[i].weave_kind == GameConstants.WIK_PENG) {
						if (cur_index < 10) {
							luoDiPaiHuXi -= 5; // 1胡变6胡
						} else {
							luoDiPaiHuXi -= 6; // 3胡变9胡
						}
					} else {
						if (cur_index < 10) {
							luoDiPaiHuXi -= 3; // 3胡变6胡
						} else {
							luoDiPaiHuXi -= 3; // 6胡变9胡
						}
					}
				}
			}

			if (cur_index != Constants_YongZhou.MAX_CARD_INDEX - 1 && cbCardIndexTemp[cur_index] == 4 && dispatch == true) {
				// 2.如果手牌里有三张，然后别人从牌堆里翻出来第四张，判断是否‘破跑胡’
				cbCardIndexTemp[cur_index] = 1;

				if (cur_index < 10) {
					luoDiPaiHuXi += 3; // 小字坎3胡
				} else {
					luoDiPaiHuXi += 6; // 大字坎6胡
				}

				weaveItems[weaveCount].weave_kind = GameConstants.WIK_KAN;
				weaveItems[weaveCount].center_card = cur_card;
				analyseUtil.weaveCount++;

				int[] handCards = new int[GameConstants.MAX_YONG_ZHOU_HH_COUNT];
				int cardCount = _logic.switch_to_cards_data_yongzhou(cbCardIndexTemp, handCards);

				for (int j = 0; j < cardCount; j++) {
					cardList_3.add(handCards[j]);
				}

				boolean tmpBValue = analyseUtil.getSolution(cardList_3, cur_index, luoDiPaiHuXi);
				if (tmpBValue) {
					bValue = true;
					Solution tmpSolution = analyseUtil.getBestSolution();

					if (tmpSolution.totalScore > bestSolution.totalScore) {
						bestSolution = tmpSolution;
						winType = 3; // 3：手牌有三张的破跑胡
					}
				}

				analyseUtil.weaveCount--;
				cbCardIndexTemp[cur_index] = 4;
				if (cur_index < 10) {
					luoDiPaiHuXi -= 3; // 小字坎3胡
				} else {
					luoDiPaiHuXi -= 6; // 大字坎6胡
				}
			}
		}

		int[] handCards = new int[GameConstants.MAX_YONG_ZHOU_HH_COUNT];
		int cardCount = _logic.switch_to_cards_data_yongzhou(cbCardIndexTemp, handCards);

		for (int j = 0; j < cardCount; j++) {
			cardList_4.add(handCards[j]);
		}

		boolean tmpBValue = analyseUtil.getSolution(cardList_4, cur_index, luoDiPaiHuXi);
		if (tmpBValue) {
			bValue = true;
			Solution tmpSolution = analyseUtil.getBestSolution();

			if (tmpSolution.totalScore > bestSolution.totalScore) {
				bestSolution = tmpSolution;
				winType = 4; // 4：正常胡牌
			}
		}

		// 如果手里有王并且没跑 并且是从萧牌走的胡牌分析 需要再分析一次
		if (_long_count[seat_index] == 0 && cur_card == 0 && magicCount > 0 && !is_qi_shou_analyse) {
			analyseUtil.setWeiAddedCard(cardWhenWin);
			cbCardIndexTemp[Constants_YongZhou.MAGIC_CARD_INDEX]--;
			luoDiPaiHuXi += 6;

			handCards = new int[GameConstants.MAX_YONG_ZHOU_HH_COUNT];
			cardCount = _logic.switch_to_cards_data_yongzhou(cbCardIndexTemp, handCards);

			for (int j = 0; j < cardCount; j++) {
				cardList_9.add(handCards[j]);
			}

			tmpBValue = analyseUtil.getSolution(cardList_9, cur_index, luoDiPaiHuXi);
			if (tmpBValue) {
				bValue = true;
				Solution tmpSolution = analyseUtil.getBestSolution();

				if (tmpSolution.totalScore > bestSolution.totalScore) {
					bestSolution = tmpSolution;
					winType = 9; // 9：萧牌胡牌并且王牌可以让萧变成倾
				}
			}

			analyseUtil.setWeiAddedCard(0);
			cbCardIndexTemp[Constants_YongZhou.MAGIC_CARD_INDEX]++;
			luoDiPaiHuXi -= 6;
		}

		if (!bValue || bestSolution.totalHuXi < qiHu) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		hu_xi_hh[0] = bestSolution.totalHuXi;

		cbChiHuKind = GameConstants.WIK_CHI_HU;

		if (winType == 10) {
			// 落地牌没跑 并且手里有王牌
			_hu_weave_count[seat_index] = 0;

			for (int i = 0; i < weaveCount; i++) {
				int count = _hu_weave_count[seat_index];
				if (i == specialMaxScoreWeaveIndex) {
					int tmpWKind = weaveItems[i].weave_kind;

					if (tmpWKind == GameConstants.WIK_PENG)
						tmpWKind = GameConstants.WIK_PAO;
					else if (tmpWKind == GameConstants.WIK_WEI || tmpWKind == GameConstants.WIK_CHOU_WEI)
						tmpWKind = GameConstants.WIK_TI_LONG;

					_hu_weave_items[seat_index][count].weave_kind = tmpWKind;
					_hu_weave_items[seat_index][count].center_card = weaveItems[i].center_card;
					_hu_weave_items[seat_index][count].hu_xi = _logic.get_weave_hu_xi(_hu_weave_items[seat_index][count]);
					_hu_weave_items[seat_index][count].weave_card_count = _logic.get_weave_card(weaveItems[i].weave_kind, weaveItems[i].center_card,
							_hu_weave_items[seat_index][count].weave_card);
					_hu_weave_items[seat_index][count].weave_card_count++;
					_hu_weave_items[seat_index][count].weave_card[3] = Constants_YongZhou.MAGIC_CARD;
					_hu_weave_count[seat_index]++;
				} else {
					_hu_weave_items[seat_index][count].weave_kind = weaveItems[i].weave_kind;
					_hu_weave_items[seat_index][count].center_card = weaveItems[i].center_card;
					_hu_weave_items[seat_index][count].hu_xi = _logic.get_weave_hu_xi(_hu_weave_items[seat_index][count]);
					_hu_weave_items[seat_index][count].weave_card_count = _logic.get_weave_card(weaveItems[i].weave_kind, weaveItems[i].center_card,
							_hu_weave_items[seat_index][count].weave_card);
					_hu_weave_count[seat_index]++;
				}
			}

			for (WeaveInfo wInfo : bestSolution.weaveInfoList) {
				int count = _hu_weave_count[seat_index];
				_hu_weave_items[seat_index][count].weave_kind = wInfo.triple.getSecond();
				_hu_weave_items[seat_index][count].hu_xi = wInfo.triple.getFirst();
				int tmpCount = _hu_weave_items[seat_index][count].weave_card_count = wInfo.cardPositions.size();

				for (int x = 0; x < tmpCount; x++) {
					_hu_weave_items[seat_index][count].weave_card[x] = finalCardList_10.get(wInfo.cardPositions.get(x));
				}

				_hu_weave_count[seat_index]++;
			}
		} else if (winType == 9) {
			// 萧牌胡牌并且王牌可以让萧变成倾
			_hu_weave_count[seat_index] = 0;
			for (int i = 0; i < weaveCount - 1; i++) {
				int count = _hu_weave_count[seat_index];
				_hu_weave_items[seat_index][count].weave_kind = weaveItems[i].weave_kind;
				_hu_weave_items[seat_index][count].center_card = weaveItems[i].center_card;
				_hu_weave_items[seat_index][count].hu_xi = _logic.get_weave_hu_xi(_hu_weave_items[seat_index][count]);
				_hu_weave_items[seat_index][count].weave_card_count = _logic.get_weave_card(weaveItems[i].weave_kind, weaveItems[i].center_card,
						_hu_weave_items[seat_index][count].weave_card);
				_hu_weave_count[seat_index]++;
			}

			int count = _hu_weave_count[seat_index];
			_hu_weave_items[seat_index][count].weave_kind = GameConstants.WIK_TI_LONG;
			_hu_weave_items[seat_index][count].center_card = cardWhenWin;
			_hu_weave_items[seat_index][count].hu_xi = _logic.get_weave_hu_xi(_hu_weave_items[seat_index][count]);
			_hu_weave_items[seat_index][count].weave_card_count = 4;
			_hu_weave_items[seat_index][count].weave_card[0] = cardWhenWin;
			_hu_weave_items[seat_index][count].weave_card[1] = cardWhenWin;
			_hu_weave_items[seat_index][count].weave_card[2] = cardWhenWin;
			_hu_weave_items[seat_index][count].weave_card[3] = Constants_YongZhou.MAGIC_CARD;
			_hu_weave_count[seat_index]++;

			for (WeaveInfo wInfo : bestSolution.weaveInfoList) {
				count = _hu_weave_count[seat_index];
				_hu_weave_items[seat_index][count].weave_kind = wInfo.triple.getSecond();
				_hu_weave_items[seat_index][count].hu_xi = wInfo.triple.getFirst();
				int tmpCount = _hu_weave_items[seat_index][count].weave_card_count = wInfo.cardPositions.size();

				for (int x = 0; x < tmpCount; x++) {
					_hu_weave_items[seat_index][count].weave_card[x] = cardList_9.get(wInfo.cardPositions.get(x));
				}

				_hu_weave_count[seat_index]++;
			}
		} else if (winType == 4) {
			// 正常胡牌
			_hu_weave_count[seat_index] = 0;
			for (int i = 0; i < weaveCount; i++) {
				int count = _hu_weave_count[seat_index];
				_hu_weave_items[seat_index][count].weave_kind = weaveItems[i].weave_kind;
				_hu_weave_items[seat_index][count].center_card = weaveItems[i].center_card;
				_hu_weave_items[seat_index][count].hu_xi = _logic.get_weave_hu_xi(_hu_weave_items[seat_index][count]);
				_hu_weave_items[seat_index][count].weave_card_count = _logic.get_weave_card(weaveItems[i].weave_kind, weaveItems[i].center_card,
						_hu_weave_items[seat_index][count].weave_card);
				_hu_weave_count[seat_index]++;
			}

			for (WeaveInfo wInfo : bestSolution.weaveInfoList) {
				int count = _hu_weave_count[seat_index];
				_hu_weave_items[seat_index][count].weave_kind = wInfo.triple.getSecond();
				_hu_weave_items[seat_index][count].hu_xi = wInfo.triple.getFirst();
				int tmpCount = _hu_weave_items[seat_index][count].weave_card_count = wInfo.cardPositions.size();

				for (int x = 0; x < tmpCount; x++) {
					_hu_weave_items[seat_index][count].weave_card[x] = cardList_4.get(wInfo.cardPositions.get(x));
				}

				if (_hu_weave_items[seat_index][count].weave_kind == GameConstants.WIK_TI_LONG) {
					int tmpCurCardCount = 0;
					for (int x = 0; x < tmpCount; x++) {
						if (_hu_weave_items[seat_index][count].weave_card[x] == cur_card) {
							tmpCurCardCount++;
						}
					}
					if (cur_index != -1 && tmpCurCardCount != 0 && tmpCurCardCount == cbCardIndexTemp[cur_index]
							&& card_type != GameConstants.HU_CARD_TYPE_ZIMO) {
						// 如果是正常胡牌，并且最后翻出来的牌，形成4张
						_hu_weave_items[seat_index][count].weave_kind = GameConstants.WIK_PAO;
					}
				}

				if (_hu_weave_items[seat_index][count].weave_kind == GameConstants.WIK_KAN) {
					int tmpCurCardCount = 0;
					for (int x = 0; x < tmpCount; x++) {
						if (_hu_weave_items[seat_index][count].weave_card[x] == cur_card) {
							tmpCurCardCount++;
						}
					}

					if (cur_index != -1 && tmpCurCardCount != 0 && tmpCurCardCount == cbCardIndexTemp[cur_index]
							&& card_type != GameConstants.HU_CARD_TYPE_ZIMO) {
						// 如果是正常胡牌，并且最后翻出来的牌，形成3张
						_hu_weave_items[seat_index][count].weave_kind = GameConstants.WIK_PENG;
					}
				}

				_hu_weave_count[seat_index]++;
			}
		} else if (winType == 3) {
			// 手牌里有三张，然后别人翻出来一张，破跑胡
			_hu_weave_count[seat_index] = 0;
			int count = _hu_weave_count[seat_index];

			for (int i = 0; i < weaveCount; i++) {
				count = _hu_weave_count[seat_index];
				_hu_weave_items[seat_index][count].weave_kind = weaveItems[i].weave_kind;
				_hu_weave_items[seat_index][count].center_card = weaveItems[i].center_card;
				_hu_weave_items[seat_index][count].hu_xi = _logic.get_weave_hu_xi(_hu_weave_items[seat_index][count]);
				_hu_weave_items[seat_index][count].weave_card_count = _logic.get_weave_card(weaveItems[i].weave_kind, weaveItems[i].center_card,
						_hu_weave_items[seat_index][count].weave_card);
				_hu_weave_count[seat_index]++;
			}

			count = _hu_weave_count[seat_index];
			_hu_weave_items[seat_index][count].weave_kind = GameConstants.WIK_KAN;
			_hu_weave_items[seat_index][count].center_card = cur_card;
			_hu_weave_items[seat_index][count].hu_xi = _logic.get_weave_hu_xi(_hu_weave_items[seat_index][count]);
			_hu_weave_items[seat_index][count].weave_card_count = _logic.get_weave_card(GameConstants.WIK_KAN, cur_card,
					_hu_weave_items[seat_index][count].weave_card);

			_hu_weave_count[seat_index]++;

			for (WeaveInfo wInfo : bestSolution.weaveInfoList) {
				count = _hu_weave_count[seat_index];
				_hu_weave_items[seat_index][count].weave_kind = wInfo.triple.getSecond();
				_hu_weave_items[seat_index][count].hu_xi = wInfo.triple.getFirst();
				int tmpCount = _hu_weave_items[seat_index][count].weave_card_count = wInfo.cardPositions.size();

				for (int x = 0; x < tmpCount; x++) {
					_hu_weave_items[seat_index][count].weave_card[x] = cardList_3.get(wInfo.cardPositions.get(x));
				}

				_hu_weave_count[seat_index]++;
			}
		} else if (winType == 5) {
			is_pao_hu[seat_index] = true;

			// 跑胡
			_hu_weave_count[seat_index] = 0;
			int count = _hu_weave_count[seat_index];

			for (int i = 0; i < weaveCount; i++) {
				count = _hu_weave_count[seat_index];

				if (weaveItems[i].center_card == cur_card && (weaveItems[i].weave_kind == GameConstants.WIK_PENG
						|| weaveItems[i].weave_kind == GameConstants.WIK_WEI || weaveItems[i].weave_kind == GameConstants.WIK_CHOU_WEI)) {
					_hu_weave_items[seat_index][count].weave_kind = GameConstants.WIK_PAO;
				} else {
					_hu_weave_items[seat_index][count].weave_kind = weaveItems[i].weave_kind;
				}

				_hu_weave_items[seat_index][count].center_card = weaveItems[i].center_card;
				_hu_weave_items[seat_index][count].hu_xi = _logic.get_weave_hu_xi(_hu_weave_items[seat_index][count]);
				_hu_weave_items[seat_index][count].weave_card_count = _logic.get_weave_card(_hu_weave_items[seat_index][count].weave_kind,
						_hu_weave_items[seat_index][count].center_card, _hu_weave_items[seat_index][count].weave_card);

				_hu_weave_count[seat_index]++;
			}

			for (WeaveInfo wInfo : bestSolution.weaveInfoList) {
				count = _hu_weave_count[seat_index];
				_hu_weave_items[seat_index][count].weave_kind = wInfo.triple.getSecond();
				_hu_weave_items[seat_index][count].hu_xi = wInfo.triple.getFirst();
				int tmpCount = _hu_weave_items[seat_index][count].weave_card_count = wInfo.cardPositions.size();

				for (int x = 0; x < tmpCount; x++) {
					_hu_weave_items[seat_index][count].weave_card[x] = cardList_5.get(wInfo.cardPositions.get(x));
				}

				_hu_weave_count[seat_index]++;
			}
		}

		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
			chiHuRight.opr_or(Constants_YongZhou.CHR_ZI_MO);
		} else {
			chiHuRight.opr_or(Constants_YongZhou.CHR_CHI_HU);
		}

		int hongPaiCount = getHongPaiCount(seat_index);

		magicCountWhenWin[seat_index] = 0;
		for (int card : bestSolution.magicToRealCard) {
			magicToRealCard[seat_index][magicCountWhenWin[seat_index]++] = card;
		}

		if (magicCountWhenWin[seat_index] > 0) {
			for (int i = 0; i < magicCountWhenWin[seat_index]; i++) {
				if (!_logic.color_hei(magicToRealCard[seat_index][i])) {
					hongPaiCount++;
				}
			}
		}

		if (hongPaiCount == 0) {
			chiHuRight.opr_or(Constants_YongZhou.CHR_HEI_HU);
		} else if (hongPaiCount >= 10) {
			if (has_rule(Constants_YongZhou.GAME_RULE_HONG_ZHUAN_ZHU_HEI)) {
				if (hongPaiCount >= 13 && hongPaiCount <= 15) {
					chiHuRight.opr_or(Constants_YongZhou.CHR_HONG_ZHUAN_DIAN);
				} else if (hongPaiCount > 15) {
					chiHuRight.opr_or(Constants_YongZhou.CHR_HONG_ZHUAN_HEI);
				} else {
					chiHuRight.opr_or(Constants_YongZhou.CHR_HONG_HU);
				}
			} else {
				chiHuRight.opr_or(Constants_YongZhou.CHR_HONG_HU);
			}
		} else if (hongPaiCount == 1) {
			chiHuRight.opr_or(Constants_YongZhou.CHR_DIAN_HU);
		}

		if (has_rule(Constants_YongZhou.GAME_RULE_AN_FAN_XIAN_HU) && magicCount > 0) {
			int fanShu = get_fan_shu(chiHuRight);
			if ((magicCount == 1 && fanShu < 2) || (magicCount == 2 && fanShu < 4) || (magicCount == 3 && fanShu < 8)
					|| (magicCount == 4 && fanShu < 16)) {
				chiHuRight.set_empty();
				return 0;
			}
		}

		return cbChiHuKind;
	}

	public int get_real_xing_pai(int magicCount) {
		int xingPai = -1;
		if (has_rule(Constants_YongZhou.GAME_RULE_GEN_XING)) {
			xingPai = cardWhenWin;
		} else if (has_rule(Constants_YongZhou.GAME_RULE_FAN_XING) && GRR._left_card_count == 0) {
			xingPai = cardWhenWin;
			if (xingPai != Constants_YongZhou.MAGIC_CARD) {
				int cardValue = _logic.get_card_value(xingPai);
				int cardColor = _logic.get_card_color(xingPai);
				if (cardValue == 10) {
					cardValue = 1;
				} else {
					cardValue += 1;
				}
				xingPai = (cardColor << 4) + cardValue;
			}
		} else {
			xingPai = _repertory_card[_all_card_len - GRR._left_card_count];
			if (DEBUG_CARDS_MODE) {
				xingPai = Constants_YongZhou.MAGIC_CARD;
			}

			if (xingPai != Constants_YongZhou.MAGIC_CARD) {
				int cardValue = _logic.get_card_value(xingPai);
				int cardColor = _logic.get_card_color(xingPai);
				if (cardValue == 10) {
					cardValue = 1;
				} else {
					cardValue += 1;
				}
				xingPai = (cardColor << 4) + cardValue;
			}
		}
		return xingPai;
	}

	public int get_qi_hu_fan(int magicCount) {
		int qiHuFan = 0;
		if (has_rule(Constants_YongZhou.GAME_RULE_AN_FAN_XIAN_HU)) {
			if (magicCount == 1)
				qiHuFan = 2;
			if (magicCount == 2)
				qiHuFan = 4;
			if (magicCount == 3)
				qiHuFan = 8;
			if (magicCount == 4)
				qiHuFan = 16;
		}
		return qiHuFan;
	}

	@Override
	public int analyse_chi_hu_card(int cards_index[], WeaveItem weaveItems[], int weaveCount, int seat_index, int provider_index, int cur_card,
			ChiHuRight chiHuRight, int card_type, int[] hu_xi_hh, boolean dispatch) {
		return 0;
	}

	public int getHongPaiCount(int seat_index) {
		int weaveCount = _hu_weave_count[seat_index];
		int hongPaiCount = 0;
		for (int i = 0; i < weaveCount; i++) {
			int cardCount = _hu_weave_items[seat_index][i].weave_card_count;
			for (int j = 0; j < cardCount; j++) {
				int card = _hu_weave_items[seat_index][i].weave_card[j];
				if (!_logic.color_hei(card)) {
					hongPaiCount++;
				}
			}
		}
		return hongPaiCount;
	}

	public int getWangZhaHongPaiCount(int seat_index) {
		int weaveCount = _wang_zha_hu_weave_count[seat_index];
		int hongPaiCount = 0;
		for (int i = 0; i < weaveCount; i++) {
			int cardCount = _wang_zha_hu_weave_items[seat_index][i].weave_card_count;
			for (int j = 0; j < cardCount; j++) {
				int card = _wang_zha_hu_weave_items[seat_index][i].weave_card[j];
				if (!_logic.color_hei(card)) {
					hongPaiCount++;
				}
			}
		}
		return hongPaiCount;
	}

	public int getWangChuangHongPaiCount(int seat_index) {
		int weaveCount = _wang_chuang_hu_weave_count[seat_index];
		int hongPaiCount = 0;
		for (int i = 0; i < weaveCount; i++) {
			int cardCount = _wang_chuang_hu_weave_items[seat_index][i].weave_card_count;
			for (int j = 0; j < cardCount; j++) {
				int card = _wang_chuang_hu_weave_items[seat_index][i].weave_card[j];
				if (!_logic.color_hei(card)) {
					hongPaiCount++;
				}
			}
		}
		return hongPaiCount;
	}

	public int getWangDiaoHongPaiCount(int seat_index) {
		int weaveCount = _wang_diao_hu_weave_count[seat_index];
		int hongPaiCount = 0;
		for (int i = 0; i < weaveCount; i++) {
			int cardCount = _wang_diao_hu_weave_items[seat_index][i].weave_card_count;
			for (int j = 0; j < cardCount; j++) {
				int card = _wang_diao_hu_weave_items[seat_index][i].weave_card[j];
				if (!_logic.color_hei(card)) {
					hongPaiCount++;
				}
			}
		}
		return hongPaiCount;
	}

	@Override
	public void set_niao_card(int seat_index, int card, boolean show) {
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
		GRR._count_niao = 0;

		int realXingPai = 0;

		if (_out_card_count == 0 && !has_rule_fan_xing) {
			int index = get_max_hand_card_index(_hu_weave_items[seat_index], _hu_weave_count[seat_index], seat_index);
			GRR._cards_data_niao[0] = _logic.switch_to_card_data_yongzhou(index);
			realXingPai = GRR._cards_data_niao[0];
			GRR._count_niao++;
		} else if (has_rule(Constants_YongZhou.GAME_RULE_GEN_XING)) {
			GRR._cards_data_niao[0] = GRR._chi_hu_card[seat_index][0];
			realXingPai = GRR._cards_data_niao[0];
			GRR._count_niao++;
		} else if (has_rule(Constants_YongZhou.GAME_RULE_FAN_XING) && GRR._left_card_count == 0) {
			// 翻醒时的海底胡
			GRR._cards_data_niao[0] = GRR._chi_hu_card[seat_index][0];
			realXingPai = GRR._cards_data_niao[0];

			if (realXingPai != Constants_YongZhou.MAGIC_CARD) {
				int cardValue = _logic.get_card_value(realXingPai);
				int cardColor = _logic.get_card_color(realXingPai);
				if (cardValue == 10) {
					cardValue = 1;
				} else {
					cardValue += 1;
				}
				realXingPai = (cardColor << 4) + cardValue;
			}

			GRR._count_niao++;
		} else {
			// 翻醒
			GRR._cards_data_niao[0] = _repertory_card[_all_card_len - GRR._left_card_count];

			if (DEBUG_CARDS_MODE) {
				GRR._cards_data_niao[0] = 0x06;
			}

			GRR._count_niao++;
			GRR._left_card_count--;

			realXingPai = GRR._cards_data_niao[0];

			if (realXingPai != Constants_YongZhou.MAGIC_CARD) {
				int cardValue = _logic.get_card_value(realXingPai);
				int cardColor = _logic.get_card_color(realXingPai);
				if (cardValue == 10) {
					cardValue = 1;
				} else {
					cardValue += 1;
				}
				realXingPai = (cardColor << 4) + cardValue;
			}
		}

		GameSchedule.put(() -> {
			changed = true;
			operate_player_get_card(GameConstants.INVALID_SEAT, 1, new int[] { GRR._cards_data_niao[0] }, GameConstants.INVALID_SEAT, false);
		}, 1500, TimeUnit.MILLISECONDS);

		int handMagicCount = GRR._cards_index[seat_index][Constants_YongZhou.MAGIC_CARD_INDEX];

		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		if (_out_card_count == 0 && !has_rule_fan_xing) {
			GRR._count_pick_niao = get_max_hand_card_count(_hu_weave_items[seat_index], _hu_weave_count[seat_index], seat_index);
		} else if (GRR._cards_data_niao[0] == Constants_YongZhou.MAGIC_CARD) {
			if (!chr.opr_and(Constants_YongZhou.CHR_WANG_ZHA_WANG).is_empty() || !chr.opr_and(Constants_YongZhou.CHR_WANG_CHUANG_WANG).is_empty()
					|| !chr.opr_and(Constants_YongZhou.CHR_WANG_DIAO_WANG).is_empty()) {
				GRR._count_pick_niao = get_max_magic_card_count(_hu_weave_items[seat_index], _hu_weave_count[seat_index], seat_index);
			} else if (handMagicCount == 0 || has_rule_fan_xing) {
				GRR._count_pick_niao = get_max_hand_card_count(_hu_weave_items[seat_index], _hu_weave_count[seat_index], seat_index);
			} else {
				GRR._count_pick_niao = get_max_magic_card_count(_hu_weave_items[seat_index], _hu_weave_count[seat_index], seat_index);
			}
		} else {
			GRR._count_pick_niao = get_xing_pai_count(_hu_weave_items[seat_index], _hu_weave_count[seat_index], seat_index, realXingPai);
		}

		if (has_rule(Constants_YongZhou.GAME_RULE_SHUANG_XING))
			GRR._count_pick_niao *= 2;
	}

	public int get_max_magic_card_count(WeaveItem[] weaveItems, int weaveCount, int seat_index) {
		int max_count = 0;

		int[] tmpCardsIndex = new int[20];

		for (int i = 0; i < weaveCount; i++) {
			int wKind = weaveItems[i].weave_kind;
			int j = 3;
			if (wKind == GameConstants.WIK_TI_LONG || wKind == GameConstants.WIK_PAO)
				j = 4;
			if (wKind == GameConstants.WIK_DUI_ZI)
				j = 2;
			for (int x = 0; x < j; x++) {
				int card = weaveItems[i].weave_card[x];
				if (card == Constants_YongZhou.MAGIC_CARD)
					continue;
				int index = _logic.switch_to_card_index_yongzhou(card);
				tmpCardsIndex[index]++;
			}
		}

		for (int j = 0; j < magicCountWhenWin[seat_index]; j++) {
			int index = _logic.switch_to_card_index_yongzhou(magicToRealCard[seat_index][j]);
			tmpCardsIndex[index]++;
		}

		for (int j = 0; j < magicCountWhenWin[seat_index]; j++) {
			int index = _logic.switch_to_card_index_yongzhou(magicToRealCard[seat_index][j]);
			if (tmpCardsIndex[index] > max_count)
				max_count = tmpCardsIndex[index];
		}

		return max_count;
	}

	public int get_max_hand_card_count(WeaveItem[] weaveItems, int weaveCount, int seat_index) {
		int max_count = 0;

		int[] tmpCardsIndex = new int[20];

		for (int i = 0; i < weaveCount; i++) {
			int wKind = weaveItems[i].weave_kind;
			int j = 3;
			if (wKind == GameConstants.WIK_TI_LONG || wKind == GameConstants.WIK_PAO)
				j = 4;
			if (wKind == GameConstants.WIK_DUI_ZI)
				j = 2;
			for (int x = 0; x < j; x++) {
				int card = weaveItems[i].weave_card[x];
				if (card == Constants_YongZhou.MAGIC_CARD)
					continue;
				int index = _logic.switch_to_card_index_yongzhou(card);
				tmpCardsIndex[index]++;
			}
		}

		for (int j = 0; j < magicCountWhenWin[seat_index]; j++) {
			int index = _logic.switch_to_card_index_yongzhou(magicToRealCard[seat_index][j]);
			tmpCardsIndex[index]++;
		}

		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			if (tmpCardsIndex[i] > max_count)
				max_count = tmpCardsIndex[i];
		}

		return max_count;
	}

	public int get_max_hand_card_index(WeaveItem[] weaveItems, int weaveCount, int seat_index) {
		int max_count = 0;
		int result = 0;

		int[] tmpCardsIndex = new int[20];

		for (int i = 0; i < weaveCount; i++) {
			int wKind = weaveItems[i].weave_kind;
			int j = 3;
			if (wKind == GameConstants.WIK_TI_LONG || wKind == GameConstants.WIK_PAO)
				j = 4;
			if (wKind == GameConstants.WIK_DUI_ZI)
				j = 2;
			for (int x = 0; x < j; x++) {
				int card = weaveItems[i].weave_card[x];
				if (card == Constants_YongZhou.MAGIC_CARD)
					continue;
				int index = _logic.switch_to_card_index_yongzhou(card);
				tmpCardsIndex[index]++;
			}
		}

		for (int j = 0; j < magicCountWhenWin[seat_index]; j++) {
			int index = _logic.switch_to_card_index_yongzhou(magicToRealCard[seat_index][j]);
			tmpCardsIndex[index]++;
		}

		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			if (tmpCardsIndex[i] > max_count) {
				max_count = tmpCardsIndex[i];
				result = i;
			}
		}

		return result;
	}

	@Override
	public void runnable_add_discard(int seat_index, int card_count, int card_data[], boolean send_client) {
		if (seat_index == GameConstants.INVALID_SEAT) {
			return;
		}

		if (GRR == null)
			return;

		for (int i = 0; i < card_count; i++) {
			GRR._discard_count[seat_index]++;
			GRR._discard_cards[seat_index][GRR._discard_count[seat_index] - 1] = card_data[i];
		}

		if (send_client == true) {
			operate_add_discard(seat_index, card_count, card_data);
		}
	}

	private boolean operate_add_discard(int seat_index, int count, int cards[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_ADD_DISCARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(2); // 出牌
		roomResponse.setCardCount(count);

		roomResponse.setFlashTime(200);

		for (int i = 0; i < count; i++) {
			roomResponse.addCardData(cards[i]);
		}

		GRR.add_room_response(roomResponse);

		send_response_to_room(roomResponse);

		return true;
	}

	@Override
	public boolean operate_out_card(int seat_index, int count, int cards[], int type, int to_player) {
		if (seat_index == GameConstants.INVALID_SEAT) {
			return false;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_OUT_CARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(type);
		roomResponse.setCardCount(count);

		roomResponse.setFlashTime(10);
		roomResponse.setStandTime(time_for_add_discard);

		for (int i = 0; i < count; i++) {
			roomResponse.addCardData(cards[i]);
		}

		if (to_player == GameConstants.INVALID_SEAT) {
			GRR.add_room_response(roomResponse);
			return send_response_to_room(roomResponse);
		} else {
			return send_response_to_player(to_player, roomResponse);
		}
	}

	@Override
	public boolean operate_player_get_card(int seat_index, int count, int cards[], int to_player, boolean showBlack) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_SEND_CARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(2); // get牌
		roomResponse.setCardCount(count);

		roomResponse.setFlashTime(200);
		roomResponse.setStandTime(time_for_add_discard);

		if (showBlack || changed)
			roomResponse.setEffectType(100);

		if (to_player == GameConstants.INVALID_SEAT) {
			if (showBlack == true) {
				for (int i = 0; i < count; i++) {
					roomResponse.addCardData(GameConstants.BLACK_CARD); // 给别人
																		// 牌背
				}
			} else {
				for (int i = 0; i < count; i++) {
					roomResponse.addCardData(cards[i]); // 给别人 牌数据
				}
			}

			send_response_to_other(seat_index, roomResponse);

			roomResponse.clearCardData();
			for (int i = 0; i < count; i++) {

				roomResponse.addCardData(cards[i]);
			}

			GRR.add_room_response(roomResponse);

			if (seat_index != -1)
				send_response_to_player(seat_index, roomResponse);

			return true;

		} else {
			if (seat_index == to_player) {
				for (int i = 0; i < count; i++) {
					roomResponse.addCardData(cards[i]);
				}

				return send_response_to_player(seat_index, roomResponse);
			} else {
				if (showBlack == true) {
					for (int i = 0; i < count; i++) {
						roomResponse.addCardData(GameConstants.BLACK_CARD); // 给别人牌背
					}
				} else {
					for (int i = 0; i < count; i++) {
						roomResponse.addCardData(cards[i]); // 给别人 牌数据
					}
				}

				return send_response_to_special(seat_index, to_player, roomResponse);
			}
		}

	}

	public int get_xing_pai_count(WeaveItem[] weaveItems, int weaveCount, int seat_index, int xingPai) {
		int[] tmpCardsIndex = new int[20];

		for (int i = 0; i < weaveCount; i++) {
			int wKind = weaveItems[i].weave_kind;
			int j = 3;
			if (wKind == GameConstants.WIK_TI_LONG || wKind == GameConstants.WIK_PAO)
				j = 4;
			if (wKind == GameConstants.WIK_DUI_ZI)
				j = 2;
			for (int x = 0; x < j; x++) {
				int card = weaveItems[i].weave_card[x];
				if (card == Constants_YongZhou.MAGIC_CARD)
					continue;
				int index = _logic.switch_to_card_index_yongzhou(card);
				tmpCardsIndex[index]++;
			}
		}

		for (int j = 0; j < magicCountWhenWin[seat_index]; j++) {
			int index = _logic.switch_to_card_index_yongzhou(magicToRealCard[seat_index][j]);
			tmpCardsIndex[index]++;
		}

		int index = _logic.switch_to_card_index_yongzhou(xingPai);

		return tmpCardsIndex[index];
	}

	public void change_hu_count_and_weave_items(int operate_code, int seat_index) {
		if (operate_code == GameConstants.WIK_WANG_ZHA) {
			_hu_weave_count[seat_index] = _wang_zha_hu_weave_count[seat_index];
			_hu_weave_items[seat_index] = _wang_zha_hu_weave_items[seat_index];
			GRR._chi_hu_rights[seat_index] = wangZhaChr[seat_index];
			magicCountWhenWin[seat_index] = wangZhaMagicCountWhenWin[seat_index];
			magicToRealCard[seat_index] = wangZhaMagicToRealCard[seat_index];
		}
		if (operate_code == GameConstants.WIK_WANG_CHUANG) {
			_hu_weave_count[seat_index] = _wang_chuang_hu_weave_count[seat_index];
			_hu_weave_items[seat_index] = _wang_chuang_hu_weave_items[seat_index];
			GRR._chi_hu_rights[seat_index] = wangChuangChr[seat_index];
			magicCountWhenWin[seat_index] = wangChuangMagicCountWhenWin[seat_index];
			magicToRealCard[seat_index] = wangChuangMagicToRealCard[seat_index];
		}
		if (operate_code == GameConstants.WIK_WANG_DIAO) {
			_hu_weave_count[seat_index] = _wang_diao_hu_weave_count[seat_index];
			_hu_weave_items[seat_index] = _wang_diao_hu_weave_items[seat_index];
			GRR._chi_hu_rights[seat_index] = wangDiaoChr[seat_index];
			magicCountWhenWin[seat_index] = wangDiaoMagicCountWhenWin[seat_index];
			magicToRealCard[seat_index] = wangDiaoMagicToRealCard[seat_index];
		}
	}

	@Override
	public void countCardType(ChiHuRight chiHuRight, int seat_index) {
		try {
			if (getRuleValue(Constants_YongZhou.GAME_RULE_TWO_MAGIC) != 0) {
				if (chiHuRight.opr_and(Constants_YongZhou.CHR_WANG_DIAO_WANG).is_empty() == false) {
					if (chiHuRight.opr_and(Constants_YongZhou.CHR_HONG_ZHUAN_HEI).is_empty() == false) {
						MongoDBServiceImpl.getInstance().card_log(get_players()[seat_index], ECardType.yz_two_hong_zhuan_hei_wang_diao_wang,
								ECardType.yz_two_hong_zhuan_hei_wang_diao_wang.getDesc(), 0, 0l, getRoom_id());
					} else if (chiHuRight.opr_and(Constants_YongZhou.CHR_HONG_ZHUAN_DIAN).is_empty() == false) {
						MongoDBServiceImpl.getInstance().card_log(get_players()[seat_index], ECardType.yz_two_hong_zhuan_dian_wang_diao_wang,
								ECardType.yz_two_hong_zhuan_dian_wang_diao_wang.getDesc(), 0, 0l, getRoom_id());
					} else if (chiHuRight.opr_and(Constants_YongZhou.CHR_HEI_HU).is_empty() == false) {
						MongoDBServiceImpl.getInstance().card_log(get_players()[seat_index], ECardType.yz_two_hei_hu_wang_diao_wang,
								ECardType.yz_two_hei_hu_wang_diao_wang.getDesc(), 0, 0l, getRoom_id());
					} else if (chiHuRight.opr_and(Constants_YongZhou.CHR_HONG_HU).is_empty() == false) {
						MongoDBServiceImpl.getInstance().card_log(get_players()[seat_index], ECardType.yz_two_hong_hu_wang_diao_wang,
								ECardType.yz_two_hong_hu_wang_diao_wang.getDesc(), 0, 0l, getRoom_id());
					} else if (chiHuRight.opr_and(Constants_YongZhou.CHR_DIAN_HU).is_empty() == false) {
						MongoDBServiceImpl.getInstance().card_log(get_players()[seat_index], ECardType.yz_two_yi_dian_zhu_wang_diao_wang,
								ECardType.yz_two_yi_dian_zhu_wang_diao_wang.getDesc(), 0, 0l, getRoom_id());
					} else {
						MongoDBServiceImpl.getInstance().card_log(get_players()[seat_index], ECardType.yz_two_wang_diao_wang,
								ECardType.yz_two_wang_diao_wang.getDesc(), 0, 0l, getRoom_id());
					}
				}
			} else if (getRuleValue(Constants_YongZhou.GAME_RULE_THREE_MAGIC) != 0) {
				if (chiHuRight.opr_and(Constants_YongZhou.CHR_WANG_CHUANG_WANG).is_empty() == false) {
					if (chiHuRight.opr_and(Constants_YongZhou.CHR_HONG_ZHUAN_HEI).is_empty() == false) {
						MongoDBServiceImpl.getInstance().card_log(get_players()[seat_index], ECardType.yz_three_hong_zhuang_hei_wang_chuang_wang,
								ECardType.yz_three_hong_zhuang_hei_wang_chuang_wang.getDesc(), 0, 0l, getRoom_id());
					} else if (chiHuRight.opr_and(Constants_YongZhou.CHR_HONG_ZHUAN_DIAN).is_empty() == false) {
						MongoDBServiceImpl.getInstance().card_log(get_players()[seat_index], ECardType.yz_three_hong_zhuang_dian_wang_chuang_wang,
								ECardType.yz_three_hong_zhuang_dian_wang_chuang_wang.getDesc(), 0, 0l, getRoom_id());
					} else if (chiHuRight.opr_and(Constants_YongZhou.CHR_HEI_HU).is_empty() == false) {
						MongoDBServiceImpl.getInstance().card_log(get_players()[seat_index], ECardType.yz_three_hei_hu_wang_chuang_wang,
								ECardType.yz_three_hei_hu_wang_chuang_wang.getDesc(), 0, 0l, getRoom_id());
					} else if (chiHuRight.opr_and(Constants_YongZhou.CHR_HONG_HU).is_empty() == false) {
						MongoDBServiceImpl.getInstance().card_log(get_players()[seat_index], ECardType.yz_three_hong_hu_wang_chuang_wang,
								ECardType.yz_three_hong_hu_wang_chuang_wang.getDesc(), 0, 0l, getRoom_id());
					} else if (chiHuRight.opr_and(Constants_YongZhou.CHR_DIAN_HU).is_empty() == false) {
						MongoDBServiceImpl.getInstance().card_log(get_players()[seat_index], ECardType.yz_three_yi_dian_zhu_wang_chuang_wang,
								ECardType.yz_three_yi_dian_zhu_wang_chuang_wang.getDesc(), 0, 0l, getRoom_id());
					} else {
						MongoDBServiceImpl.getInstance().card_log(get_players()[seat_index], ECardType.yz_three_wang_chuang_wang,
								ECardType.yz_three_wang_chuang_wang.getDesc(), 0, 0l, getRoom_id());
					}
				}
			} else if (getRuleValue(Constants_YongZhou.GAME_RULE_FOUR_MAGIC) != 0) {
				if (chiHuRight.opr_and(Constants_YongZhou.CHR_WANG_ZHA_WANG).is_empty() == false) {
					if (chiHuRight.opr_and(Constants_YongZhou.CHR_HONG_ZHUAN_HEI).is_empty() == false) {
						MongoDBServiceImpl.getInstance().card_log(get_players()[seat_index], ECardType.yz_four_hong_zhuan_hei_wang_zha_wang,
								ECardType.yz_four_hong_zhuan_hei_wang_zha_wang.getDesc(), 0, 0l, getRoom_id());
					} else if (chiHuRight.opr_and(Constants_YongZhou.CHR_HONG_ZHUAN_DIAN).is_empty() == false) {
						MongoDBServiceImpl.getInstance().card_log(get_players()[seat_index], ECardType.yz_four_hong_zhuan_dian_wang_zha_wang,
								ECardType.yz_four_hong_zhuan_dian_wang_zha_wang.getDesc(), 0, 0l, getRoom_id());
					} else if (chiHuRight.opr_and(Constants_YongZhou.CHR_HEI_HU).is_empty() == false) {
						MongoDBServiceImpl.getInstance().card_log(get_players()[seat_index], ECardType.yz_four_hei_hu_wang_zha_wang,
								ECardType.yz_four_hei_hu_wang_zha_wang.getDesc(), 0, 0l, getRoom_id());
					} else if (chiHuRight.opr_and(Constants_YongZhou.CHR_HONG_HU).is_empty() == false) {
						MongoDBServiceImpl.getInstance().card_log(get_players()[seat_index], ECardType.yz_four_hong_hu_wang_zha_wang,
								ECardType.yz_four_hong_hu_wang_zha_wang.getDesc(), 0, 0l, getRoom_id());
					} else if (chiHuRight.opr_and(Constants_YongZhou.CHR_DIAN_HU).is_empty() == false) {
						MongoDBServiceImpl.getInstance().card_log(get_players()[seat_index], ECardType.yz_four_yi_dian_zhu_wang_zha_wang,
								ECardType.yz_four_yi_dian_zhu_wang_zha_wang.getDesc(), 0, 0l, getRoom_id());
					} else {
						MongoDBServiceImpl.getInstance().card_log(get_players()[seat_index], ECardType.yz_four_wang_zha_wang,
								ECardType.yz_four_wang_zha_wang.getDesc(), 0, 0l, getRoom_id());
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public int get_max_solo_score() {
		int result = 100000;
		if (has_rule(Constants_YongZhou.GAME_RULE_300_FENG_DING))
			result = 300;
		if (has_rule(Constants_YongZhou.GAME_RULE_600_FENG_DING))
			result = 600;
		return result;
	}

	@Override
	public void process_chi_hu_player_score_phz(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;
		GRR._win_order[seat_index] = 1;
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		countCardType(chr, seat_index);

		int all_hu_xi = 0;
		for (int i = 0; i < _hu_weave_count[seat_index]; i++) {
			all_hu_xi += _hu_weave_items[seat_index][i].hu_xi;
		}

		_hu_xi[seat_index] = all_hu_xi; // 胡息
		_tun_shu[seat_index] = 1; // 囤数
		_fan_shu[seat_index] = 1; // 番数
		total_tun_shu[seat_index] = 1; // 总囤数

		int wTunShu = 1;
		int wFanShu = 1;
		int lChiHuScore = 1;

		int realTunShu = (all_hu_xi - get_basic_hu_xi()) / 3 + 2;

		_tun_shu[seat_index] = wTunShu = (all_hu_xi - get_basic_hu_xi()) / 3 + 2 + GRR._count_pick_niao;
		_fan_shu[seat_index] = wFanShu = get_fan_shu(chr);
		total_tun_shu[seat_index] = lChiHuScore = wTunShu * wFanShu;

		int maxSoloScore = get_max_solo_score();

		int zuo_xing_fen = 0;
		int pCount = getTablePlayerNumber();
		if (pCount == 4) {
			zuo_xing_fen = wFanShu * GRR._count_pick_niao;
		}

		if ((maxSoloScore == 300 || maxSoloScore == 600) && zuo_xing_fen > maxSoloScore) {
			zuo_xing_fen = maxSoloScore;
		}
		if ((maxSoloScore == 300 || maxSoloScore == 600) && lChiHuScore > maxSoloScore) {
			total_tun_shu[seat_index] = lChiHuScore = maxSoloScore;
		}

		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				GRR._lost_fan_shu[i][seat_index] = lChiHuScore;
			}
		} else {
			GRR._lost_fan_shu[provide_index][seat_index] = lChiHuScore * (getTablePlayerNumber() - 1);
		}

		int[] tmpPlayerScore = new int[getTablePlayerNumber()];
		boolean different_path = false;
		if (pCount == 4) {
			for (int i = 0; i < pCount; i++) {
				if (i == seat_index)
					continue;
				if (i == zuo_xing_seat)
					continue;

				tmpPlayerScore[i] -= zuo_xing_fen;
				tmpPlayerScore[zuo_xing_seat] += zuo_xing_fen;
			}

			for (int i = 0; i < pCount; i++) {
				if (i == seat_index) {
					continue;
				}

				if (pCount == 4 && i == zuo_xing_seat)
					continue;

				int s = lChiHuScore;

				tmpPlayerScore[i] -= s;
				tmpPlayerScore[seat_index] += s;
			}

			for (int i = 0; i < pCount; i++) {
				if (tmpPlayerScore[i] + maxSoloScore < 0) {
					// 四人坐醒时 如果闲家给的分 多余封顶分 重新计算
					different_path = true;
					break;
				}
			}
		}

		if (different_path) {
			int tmpScore = realTunShu * wFanShu;

			if (tmpScore > maxSoloScore) {
				for (int i = 0; i < pCount; i++) {
					if (i == seat_index) {
						GRR._game_score[i] += 2 * maxSoloScore;
						all_game_round_score[i][_cur_round - 1] += 2 * maxSoloScore;
					} else if (i == zuo_xing_seat) {
						GRR._game_score[i] += 0;
						all_game_round_score[i][_cur_round - 1] += 0;
					} else {
						GRR._game_score[i] -= maxSoloScore;
						all_game_round_score[i][_cur_round - 1] -= maxSoloScore;
					}
				}
			} else {
				int xingScore = maxSoloScore - tmpScore;
				int zhuangScore = (2 * maxSoloScore) - xingScore;

				for (int i = 0; i < pCount; i++) {
					if (i == seat_index) {
						GRR._game_score[i] += zhuangScore;
						all_game_round_score[i][_cur_round - 1] += zhuangScore;
					} else if (i == zuo_xing_seat) {
						GRR._game_score[i] += xingScore;
						all_game_round_score[i][_cur_round - 1] += xingScore;
					} else {
						GRR._game_score[i] -= maxSoloScore;
						all_game_round_score[i][_cur_round - 1] -= maxSoloScore;
					}
				}
			}
		} else if (zimo) {
			if (pCount == 4) {
				for (int i = 0; i < pCount; i++) {
					if (i == seat_index)
						continue;
					if (i == zuo_xing_seat)
						continue;

					GRR._game_score[i] -= zuo_xing_fen;
					GRR._game_score[zuo_xing_seat] += zuo_xing_fen;

					all_game_round_score[i][_cur_round - 1] -= zuo_xing_fen;
					all_game_round_score[zuo_xing_seat][_cur_round - 1] += zuo_xing_fen;
				}
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}

				if (pCount == 4 && i == zuo_xing_seat)
					continue;

				int s = lChiHuScore;

				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;

				all_game_round_score[i][_cur_round - 1] -= s;
				all_game_round_score[seat_index][_cur_round - 1] += s;
			}

			if (pCount == 4 && zuo_xing_fen == 0 && seat_index == GRR._banker_player) {
				// 庄家胡牌 翻醒是0时 得分平分
				GRR._game_score[seat_index] -= lChiHuScore;
				GRR._game_score[zuo_xing_seat] += lChiHuScore;
			}
		}

		// process_xiang_gong();

		GRR._provider[seat_index] = provide_index;
	}

	protected int get_basic_hu_xi() {
		if (has_rule(Constants_YongZhou.GAME_RULE_21_HU_QI_HU))
			return 21;
		return 15;
	}

	protected int get_fan_shu(ChiHuRight chr) {
		int fan = 1;

		if (!chr.opr_and(Constants_YongZhou.CHR_DIAN_HU).is_empty()) {
			fan *= 3;
		}
		if (!chr.opr_and(Constants_YongZhou.CHR_HONG_HU).is_empty()) {
			fan *= 2;
		}
		if (!chr.opr_and(Constants_YongZhou.CHR_HEI_HU).is_empty()) {
			fan *= 4;
		}
		if (!chr.opr_and(Constants_YongZhou.CHR_HONG_ZHUAN_DIAN).is_empty()) {
			fan *= 3;
		}
		if (!chr.opr_and(Constants_YongZhou.CHR_HONG_ZHUAN_HEI).is_empty()) {
			fan *= 4;
		}
		if (!chr.opr_and(Constants_YongZhou.CHR_ZI_MO).is_empty()) {
			fan *= 2;
		}
		if (!chr.opr_and(Constants_YongZhou.CHR_WANG_DIAO).is_empty()) {
			fan *= 4;
		}
		if (!chr.opr_and(Constants_YongZhou.CHR_WANG_DIAO_WANG).is_empty()) {
			fan *= 8;
		}
		if (!chr.opr_and(Constants_YongZhou.CHR_WANG_CHUANG).is_empty()) {
			fan *= 8;
		}
		if (!chr.opr_and(Constants_YongZhou.CHR_WANG_CHUANG_WANG).is_empty()) {
			fan *= 16;
		}
		if (!chr.opr_and(Constants_YongZhou.CHR_WANG_ZHA).is_empty()) {
			fan *= 16;
		}
		if (!chr.opr_and(Constants_YongZhou.CHR_WANG_ZHA_WANG).is_empty()) {
			fan *= 32;
		}

		return fan;
	}

	@Override
	public void set_result_describe(int seat_index) {
	}

	@Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x01, 0x01, 0x01, 0x02, 0x02, 0x03, 0x03, 0x03, 0x04, 0x04, 0x04, 0x05, 0x05, 0x05, 0x07, 0x07, 0x0a,
				0x0a, 0xff, 0xff };
		int[] cards_of_player1 = new int[] { 0x11, 0x11, 0x01, 0x14, 0x14, 0x04, 0x15, 0x15, 0x05, 0x17, 0x17, 0x07, 0x19, 0x19, 0x09, 0x12, 0x12,
				0x02, 0x08, 0x08 };
		int[] cards_of_player2 = new int[] { 0x11, 0x11, 0x11, 0x02, 0x02, 0x02, 0x03, 0x03, 0x03, 0x04, 0x04, 0x04, 0x05, 0x05, 0x05, 0x14, 0x15,
				0x16, 0x01, 0x12 };
		int[] cards_of_player3 = new int[] { 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x03, 0x03, 0x03, 0x04, 0x04, 0x04, 0x05, 0x05, 0x05, 0x06, 0x06,
				0x06, 0x07, 0x08 };

		int pCount = getTablePlayerNumber();

		for (int i = 0; i < pCount; i++) {
			for (int j = 0; j < Constants_YongZhou.MAX_CARD_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}

		if (pCount == 2) {
			for (int j = 0; j < GameConstants.MAX_HH_COUNT - 1; j++) {
				GRR._cards_index[0][_logic.switch_to_card_index_yongzhou(cards_of_player0[j])] += 1;
				GRR._cards_index[1][_logic.switch_to_card_index_yongzhou(cards_of_player1[j])] += 1;
			}
		} else if (pCount == 3) {
			for (int j = 0; j < GameConstants.MAX_HH_COUNT - 1; j++) {
				GRR._cards_index[0][_logic.switch_to_card_index_yongzhou(cards_of_player0[j])] += 1;
				GRR._cards_index[1][_logic.switch_to_card_index_yongzhou(cards_of_player1[j])] += 1;
				GRR._cards_index[2][_logic.switch_to_card_index_yongzhou(cards_of_player2[j])] += 1;
			}
		} else if (pCount == 4) {
			for (int i = 0; i < pCount; i++) {
				if (i == zuo_xing_seat)
					continue;

				for (int j = 0; j < GameConstants.MAX_HH_COUNT - 1; j++) {
					if (i == 0)
						GRR._cards_index[0][_logic.switch_to_card_index_yongzhou(cards_of_player0[j])] += 1;
					if (i == 1)
						GRR._cards_index[1][_logic.switch_to_card_index_yongzhou(cards_of_player1[j])] += 1;
					if (i == 2)
						GRR._cards_index[2][_logic.switch_to_card_index_yongzhou(cards_of_player2[j])] += 1;
					if (i == 3)
						GRR._cards_index[3][_logic.switch_to_card_index_yongzhou(cards_of_player3[j])] += 1;
				}
			}
		}

		// BACK_DEBUG_CARDS_MODE = true;
		// debug_my_cards = new int[] { 4, 2, 18, 9, 255, 1, 5, 25, 20, 7, 255,
		// 23, 2, 10, 4, 23, 18, 9, 26, 9, 25, 2, 5, 25, 255, 3, 24, 17, 9, 19,
		// 23,
		// 7, 23, 3, 1, 26, 21, 21, 22, 5, 20, 18, 6, 3, 7, 1, 19, 6, 6, 8, 19,
		// 8, 24, 17, 17, 6, 4, 18, 4, 5, 255, 10, 3, 2, 20, 24, 21, 10, 22,
		// 10, 20, 17, 22, 7, 26, 22, 1, 24, 19, 21, 25, 8, 26, 8 };

		if (BACK_DEBUG_CARDS_MODE) {
			if (debug_my_cards != null) {
				if (debug_my_cards.length > 20) {
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
		int pCount = getTablePlayerNumber();

		for (int i = 0; i < pCount; i++) {
			for (int j = 0; j < Constants_YongZhou.MAX_CARD_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}

		_repertory_card = realyCards;
		_all_card_len = _repertory_card.length;
		GRR._left_card_count = _all_card_len;

		int send_count;
		int have_send_count = 0;
		for (int i = 0; i < pCount; i++) {
			if (pCount == 4 && i == zuo_xing_seat)
				continue;

			send_count = (GameConstants.MAX_HH_COUNT - 1);

			GRR._left_card_count -= send_count;

			_logic.switch_to_cards_index_yongzhou(_repertory_card, have_send_count, send_count, GRR._cards_index[i]);

			have_send_count += send_count;
		}

		DEBUG_CARDS_MODE = false;
		BACK_DEBUG_CARDS_MODE = false;
	}

	@Override
	public void testSameCard(int[] cards) {
		int pCount = getTablePlayerNumber();

		for (int i = 0; i < pCount; i++) {
			for (int j = 0; j < Constants_YongZhou.MAX_CARD_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}

		for (int i = 0; i < pCount; i++) {
			if (pCount == 4 && i == zuo_xing_seat)
				continue;

			int send_count = (GameConstants.MAX_HH_COUNT - 1);
			if (send_count > cards.length)
				send_count = cards.length;

			for (int j = 0; j < send_count; j++) {
				GRR._cards_index[i][_logic.switch_to_card_index_yongzhou(cards[j])] += 1;
			}
		}

		DEBUG_CARDS_MODE = false;
		BACK_DEBUG_CARDS_MODE = false;
	}

	protected int get_banker_next_seat(int banker_seat) {
		int seat = banker_seat;
		int count = 0;
		if (getTablePlayerNumber() == 4) {
			do {
				count++;
				seat = (seat + 1) % getTablePlayerNumber();
			} while (get_players()[seat] == null && count <= 5);

			if (seat == zuo_xing_seat) {
				do {
					count++;
					seat = (seat + 1) % getTablePlayerNumber();
				} while (get_players()[seat] == null && count <= 5);
			}
		} else {
			do {
				count++;
				seat = (seat + 1) % getTablePlayerNumber();
			} while (get_players()[seat] == null && count <= 5);
		}
		return seat;
	}

	public void runnable_deal_with_first_card(int seat_index) {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i].clean_action();
			_playerStatus[i].clean_status();
		}

		_playerStatus[seat_index].chi_hu_round_valid();

		_current_player = seat_index;

		int pCount = getTablePlayerNumber();

		for (int p = 0; p < pCount; p++) {
			_long_count[p] = 0;
			_ti_mul_long[p] = 0;
		}

		// 处理起手的召牌，从庄家开始处理
		for (int p = 0; p < pCount; p++) {
			int k = (GRR._banker_player + p) % pCount;

			if (pCount == 4 && k == zuo_xing_seat)
				continue;

			for (int i = 0; i < Constants_YongZhou.MAX_CARD_INDEX - 1; i++) {
				if (GRR._cards_index[k][i] == 4) {
					_long_count[k]++;
				}
			}

			if (_long_count[k] >= 2) {
				_ti_mul_long[k] = _long_count[k] - 1;
			}
		}

		// 分析当前庄家能不能胡
		int card_type = GameConstants.HU_CARD_TYPE_ZIMO;
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		chr.set_empty();

		cardWhenWin = _send_card_data;

		PlayerStatus tempPlayerStatus = _playerStatus[seat_index];
		int[] hu_xi = new int[1];
		is_qi_shou_analyse = true;
		int action_hu = analyse_normal(GRR._cards_index[seat_index], GRR._weave_items[seat_index], GRR._weave_count[seat_index], seat_index,
				seat_index, _send_card_data, chr, card_type, hu_xi, true);
		is_qi_shou_analyse = false;

		if (action_hu != GameConstants.WIK_NULL) {
			tempPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
			tempPlayerStatus.add_zi_mo(_send_card_data, seat_index);

			tempPlayerStatus.add_action(GameConstants.WIK_NULL);
			tempPlayerStatus.add_pass(0, seat_index);

			tempPlayerStatus.set_status(GameConstants.Player_Status_OPR_CARD);
			operate_player_action(seat_index, false);
		} else {
			chr.set_empty();

			_playerStatus[seat_index].set_status(GameConstants.Player_Status_OUT_CARD);
			operate_player_status();
		}

		return;
	}

	public void deal_with_zhao(int seat_index, int[] cards_index) {
		int an_long_index[] = new int[5];
		int an_long_count = 0;

		boolean displayAction = false;

		for (int i = 0; i < Constants_YongZhou.MAX_CARD_INDEX - 1; i++) {
			if (cards_index[i] == 4) {
				an_long_index[an_long_count++] = i;
			}
		}

		if (an_long_count > 1 || (an_long_count > 0 && _long_count[seat_index] > 0)) {
			// 如果起手倾大于1个或者补牌之后还有倾 才刷新手牌
			for (int i = 0; i < an_long_count; i++) {
				int cbWeaveIndex = GRR._weave_count[seat_index];
				GRR._weave_items[seat_index][cbWeaveIndex].public_card = 1;
				GRR._weave_items[seat_index][cbWeaveIndex].center_card = _logic.switch_to_card_data_yongzhou(an_long_index[i]);
				GRR._weave_items[seat_index][cbWeaveIndex].weave_kind = GameConstants.WIK_TI_LONG;
				GRR._weave_items[seat_index][cbWeaveIndex].provide_player = seat_index;
				GRR._weave_items[seat_index][cbWeaveIndex].hu_xi = _logic.get_weave_hu_xi(GRR._weave_items[seat_index][cbWeaveIndex]);
				GRR._weave_count[seat_index]++;
				_long_count[seat_index]++;

				GRR._cards_index[seat_index][an_long_index[i]] = 0;

				GRR._card_count[seat_index] = _logic.get_card_count_by_index(GRR._cards_index[seat_index]);
			}

			displayAction = true;
		} else if (an_long_count > 0) {
			_long_count[seat_index]++;
		}

		if (an_long_count == 0) { // 手牌没召了
			return;
		} else { // 手牌有召
			if (displayAction) {
				operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_TI_LONG }, 1,
						GameConstants.INVALID_SEAT);
			}

			int cards[] = new int[GameConstants.MAX_YONG_ZHOU_HH_COUNT];
			int hand_card_count = _logic.switch_to_cards_data_yongzhou(GRR._cards_index[seat_index], cards);
			operate_player_cards(seat_index, hand_card_count, cards, GRR._weave_count[seat_index], GRR._weave_items[seat_index]);

			if (has_first_qi_shou_ti[seat_index] == false) {
				has_first_qi_shou_ti[seat_index] = true;
				if (an_long_count == 1) {
					return;
				} else {
					dispatch_card(seat_index, an_long_count - 1);
				}
			} else if (has_first_qi_shou_ti[seat_index] == true) {
				dispatch_card(seat_index, an_long_count);
			}

			deal_with_zhao(seat_index, GRR._cards_index[seat_index]);
		}
	}

	public void dispatch_card(int seat_index, int count) {
		for (int i = 0; i < count; i++) {
			_send_card_count++;
			int send_card_data = _repertory_card[_all_card_len - GRR._left_card_count];
			GRR._left_card_count--;

			if (seat_index == GRR._banker_player) {
				_send_card_data = send_card_data;
				_provide_card = _send_card_data;
			}

			GRR._cards_index[seat_index][_logic.switch_to_card_index_yongzhou(send_card_data)]++;

			int cards[] = new int[GameConstants.MAX_YONG_ZHOU_HH_COUNT];
			int hand_card_count = _logic.switch_to_cards_data_yongzhou(GRR._cards_index[seat_index], cards);
			operate_player_cards(seat_index, hand_card_count, cards, GRR._weave_count[seat_index], GRR._weave_items[seat_index]);
		}
	}

	@Override
	public boolean operate_effect_action(int seat_index, int effect_type, int effect_count, long effect_indexs[], int time, int to_player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_EFFECT_ACTION);
		roomResponse.setEffectType(effect_type);
		roomResponse.setTarget(seat_index);
		roomResponse.setEffectCount(effect_count);

		for (int i = 0; i < effect_count; i++) {
			roomResponse.addEffectsIndex(effect_indexs[i]);
		}

		if (effect_type == GameConstants.EFFECT_ACTION_DRAW && !has_rule(Constants_YongZhou.GAME_RULE_NO_WANG_PAI)) {
			// 如果是播放流局动画
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				roomResponse.addScore(magicCount[i]);
				roomResponse.addOpereateScore(magicScore[i]);
			}
		}

		roomResponse.setEffectTime(time);

		if (to_player == GameConstants.INVALID_SEAT) {
			GRR.add_room_response(roomResponse);
			send_response_to_room(roomResponse);
		} else {
			send_response_to_player(to_player, roomResponse);
		}

		return true;
	}
}
