package com.cai.game.mj.sichuan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.Constants_SiChuan;
import com.cai.common.define.ECardType;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GameRoundRecord;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RandomUtil;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.mj.MJType;
import com.cai.game.mj.NewAbstractMjTable;
import com.cai.game.mj.ThreeDimension;
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
import protobuf.clazz.mj.Basic.HuCard;
import protobuf.clazz.mj.Basic.HuCardList;
import protobuf.clazz.mj.Basic.MJ_Game_End_Basic;
import protobuf.clazz.mj.Basic.RoundHuCards;
import protobuf.clazz.mj.Basic.ScoreRow;

public abstract class AbstractSiChuanMjTable extends NewAbstractMjTable {
	private static final long serialVersionUID = -639658717058181088L;

	public HandlerSwitchCard_SiChuan _handler_switch_card;
	public HandlerDingQue_SiChuan _handler_ding_que;

	/**
	 * 玩家摸牌统计
	 */
	public int[] mo_pai_count = new int[getTablePlayerNumber()];
	/**
	 * 是否已经选择了定缺
	 */
	public boolean[] had_ding_que = new boolean[getTablePlayerNumber()];
	/**
	 * 是否已经选择了换三张
	 */
	public boolean[] had_switch_card = new boolean[getTablePlayerNumber()];
	/**
	 * 玩家定缺时选的颜色
	 */
	public int[] ding_que_pai_se = new int[getTablePlayerNumber()];
	/**
	 * 牌桌上还剩余的玩家数目
	 */
	public int left_player_count = getTablePlayerNumber();
	/**
	 * 玩家是否已经胡牌
	 */
	public boolean[] had_hu_pai = new boolean[getTablePlayerNumber()];;
	/**
	 * 玩家胡牌的顺序
	 */
	public int[] win_order = new int[getTablePlayerNumber()];;
	/**
	 * 玩家胡牌的类型
	 */
	public int[] win_type = new int[getTablePlayerNumber()];;
	/**
	 * 下一局的庄家
	 */
	public int next_banker_player = -1;

	public final int ZI_MO_HU = 1;
	public final int JIE_PAO_HU = 2;

	/**
	 * 换牌数据
	 */
	public final int SWITCH_CARD_COUNT = 3;
	public int[][] switch_card_index = new int[getTablePlayerNumber()][SWITCH_CARD_COUNT];

	/**
	 * 胡牌时，记录一下番数
	 */
	public int[] score_when_win = new int[getTablePlayerNumber()];
	/**
	 * 有胡时，不胡，再次存储一下胡牌时的番数，和score_shu_when_win进行比较
	 */
	public int[] score_when_abandoned_win = new int[getTablePlayerNumber()];

	/**
	 * 每个玩家，接杠能胡时，先碰牌的记录
	 */
	public int[][] passed_gang_cards = new int[getTablePlayerNumber()][GameConstants.MAX_WEAVE];;
	public int[] passed_gang_count = new int[getTablePlayerNumber()];

	/**
	 * 玩家点了哪些炮，比如‘点1胡’‘点2胡’
	 */
	public int[][] dian_pao_order = new int[getTablePlayerNumber()][getTablePlayerNumber()];
	public int[] dian_pao_count = new int[getTablePlayerNumber()];

	/**
	 * 暗杠，直杠，弯杠，点杠，计数
	 */
	public int[] an_gang_count = new int[getTablePlayerNumber()];
	public int[] zhi_gang_count = new int[getTablePlayerNumber()];
	public int[] wan_gang_count = new int[getTablePlayerNumber()];
	public int[] dian_gang_count = new int[getTablePlayerNumber()];

	/**
	 * 统计桌面上杠之后，发的牌的张数
	 */
	public int gang_dispatch_count = 0;

	/**
	 * 小局是否查大叫
	 */
	public boolean[] cha_da_jiao = new boolean[getTablePlayerNumber()];
	/**
	 * 小局是否被查大叫
	 */
	public boolean[] bei_cha_da_jiao = new boolean[getTablePlayerNumber()];

	/**
	 * 牌桌上已经胡了的牌数据
	 */
	public int[] table_hu_cards = new int[getTablePlayerNumber()];
	/**
	 * 牌桌上已经胡的牌的张数
	 */
	public int table_hu_card_count = 0;

	/**
	 * 牌桌上是否是杠上杠状态，用于处理‘花中花炮中炮’
	 */
	public boolean gang_shang_gang = false;

	/**
	 * 玩家是否杠上杠或炮中炮胡的牌
	 */
	public boolean[] player_gsg = new boolean[getTablePlayerNumber()];

	/**
	 * 玩家当前有杠不杠而选择了碰的牌
	 */
	public int cur_round_abandoned_gang = -1;

	/**
	 * 夹心五数量
	 */
	public int[] jia_xin_wu_count = new int[getTablePlayerNumber()];

	/**
	 * 是听牌分析还是正常的胡牌分析
	 */
	public int analyse_state = 0;
	public final int FROM_NORMAL = 1;
	public final int FROM_TING = 2;
	public final int FROM_MAX_COUNT = 3;
	public final int FROM_BAO_JIAO = 4;

	/**
	 * 牌桌上有人开杠时，杠组合的序号，主要用来解决弯杠的时候，杠上炮的问题
	 */
	public int gang_pai_weave_index = 0;

	/**
	 * 牌桌上是否勾选了定缺
	 */
	public boolean hasRuleDingQue = false;
	/**
	 * 牌桌上是否勾选了比牌
	 */
	public boolean hasRuleBiPai = false;
	/**
	 * 牌桌上是否勾选了买马
	 */
	public boolean hasRuleMaiMa = false;
	/**
	 * 牌桌上是否勾选了软碰可杠
	 */
	public boolean hasRuleRuanGang = false;
	/**
	 * 牌桌上是否勾选了无鸡杠翻倍
	 */
	public boolean hasRuleWuJiGangDouble = false;

	/**
	 * 玩家胡牌时的特殊番，可以是正也可以是负的
	 */
	public int[] special_fan_shu = new int[getTablePlayerNumber()];
	/**
	 * 中发白的刻番
	 */
	public int[] zfb_ke_fan = new int[getTablePlayerNumber()];
	/**
	 * 玩家明杠暗杠的番
	 */
	public int[] gang_fan = new int[getTablePlayerNumber()];

	/**
	 * 玩家胡牌时的特殊番，可以是正也可以是负的
	 */
	public int[] ting_special_fan_shu = new int[getTablePlayerNumber()];
	/**
	 * 中发白的刻番
	 */
	public int[] ting_zfb_ke_fan = new int[getTablePlayerNumber()];
	/**
	 * 玩家明杠暗杠的番
	 */
	public int[] ting_gang_fan = new int[getTablePlayerNumber()];

	/**
	 * 游戏结束时，是否是听牌状态，默认为true，最后来查大叫的时候再置为false
	 */
	public boolean[] is_ting_when_finish = new boolean[getTablePlayerNumber()];
	/**
	 * 胡牌的提供者是谁
	 */
	public int[] whoProvided = new int[getTablePlayerNumber()];
	/**
	 * 玩家最后的番数或分数
	 */
	public int[] finallyFanShu = new int[getTablePlayerNumber()];
	/**
	 * 牌桌上的王牌 给个默认值-1
	 */
	public int magicCard = -1;
	/**
	 * 牌桌上的王牌索引 给个默认值一万 防止异常
	 */
	public int magicCardIndex = 0;
	/**
	 * 牌局结束时 是否是四鸡发财
	 */
	public boolean[] is_si_ji_fa_cai = new boolean[getTablePlayerNumber()];
	/**
	 * 每个玩家 打那些牌 听的那些 能胡多少番
	 */
	public int[][][] ting_pai_fan_shu = new int[getTablePlayerNumber()][GameConstants.MAX_INDEX][GameConstants.MAX_INDEX];
	/**
	 * 查大叫时的最大牌型的文字描述
	 */
	public String[] max_pai_xing_desc = new String[getTablePlayerNumber()];
	/**
	 * 得分条目的明显，整型数组的第一个值表示类型，第2-第5个值表示第1-第4个玩家的得分
	 */
	public List<int[]> scoreDetails = new ArrayList<>();

	public boolean hasRuleFourMagic = false;
	public boolean hasRuleEightMagic = false;
	public boolean hasRuleTwelveMagic = false;
	public boolean hasRuleFengDing20 = false;
	public boolean hasRuleFengDing40 = false;
	public boolean hasRuleFengDing80 = false;
	public boolean hasRuleDuanGang = false;

	public boolean[] is_bao_ting = new boolean[getTablePlayerNumber()];

	public boolean[] display_ruan_peng = new boolean[getTablePlayerNumber()];

	public int[] finalGengCount = new int[getTablePlayerNumber()];
	/**
	 * 流水数据一共有6列 第一列是类别 2-5列是4个玩家的得分 6列是操作的牌
	 */
	public final int SCORE_DETAIL_COLUMN = 6;
	/**
	 * 默认剩余0张才结束，如果是2人 3人的血流成河，剩余10张结束
	 */
	public int LEFT_CARD = 0;
	/**
	 * 底分
	 */
	public int BASIC_SCORE = 1;
	/**
	 * 玩家的底分，会随着开杠而增加，初始值为BASIC_SCORE
	 */
	public int[] player_basic_score = new int[getTablePlayerNumber()];
	/**
	 * 每大局开始时，所有人的分数固定，如果中间过程有人的分变成0或负数了，当前小局不再参与，然后小局结束之后，大局直接结束
	 */
	public int[] player_left_score = new int[getTablePlayerNumber()];
	/**
	 * 每小局的总输赢，下一小局开始时就会清0
	 */
	public int[] small_round_total_score = new int[getTablePlayerNumber()];
	/**
	 * 玩家已经输完底分
	 */
	public boolean[] no_score_left = new boolean[getTablePlayerNumber()];
	/**
	 * 超时托管
	 */
	public boolean[] over_time_trustee = new boolean[getTablePlayerNumber()];
	/**
	 * 超时托管的读秒时间，默认-1，表示不读秒
	 */
	public int[] over_time_left = new int[getTablePlayerNumber()];
	/**
	 * 超时之后，自动操作的延时时间，单位毫秒
	 */
	public final int DELAY_AUTO_OPERATE = 1000;
	/**
	 * 超时托管定时器
	 */
	public ScheduledFuture<?>[] over_time_trustee_schedule = new ScheduledFuture[getTablePlayerNumber()];
	/**
	 * 血流成河，查叫时的牌型值
	 */
	public int[] cha_jiao_pai_xing_type = new int[getTablePlayerNumber()];

	@SuppressWarnings("unchecked")
	public Map<Integer, Integer>[] player_switched_cards = new HashMap[getTablePlayerNumber()];

	public HuCardInfo[] player_hu_card_info = new HuCardInfo[getTablePlayerNumber()];

	/**
	 * 是否已经输完底分
	 */
	public boolean[] has_ren_shu = new boolean[getTablePlayerNumber()];
	/**
	 * 坐庄次数
	 */
	public int[] player_banker_count = new int[getTablePlayerNumber()];
	/**
	 * 超时次数
	 */
	public int[] player_over_time_count = new int[getTablePlayerNumber()];
	/**
	 * 有人开始托管倒计时的时候，系统当前时间，如果没人开始托管倒计时，默认为-1
	 */
	public long schedule_start_time = -1;
	/**
	 * 初始分
	 */
	private int start_score = 100;
	/**
	 * 牌桌上已经胡过的牌，一炮多响的牌不重复计算
	 */
	public List<Integer> hu_card_list = new ArrayList<>();

	public AbstractSiChuanMjTable(MJType type) {
		super(type);

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			ting_pai_fan_shu[i] = new int[GameConstants.MAX_INDEX][GameConstants.MAX_INDEX];
			for (int j = 0; j < GameConstants.MAX_INDEX; j++)
				ting_pai_fan_shu[i][j] = new int[GameConstants.MAX_INDEX];

			player_switched_cards[i] = new HashMap<Integer, Integer>();

			player_hu_card_info[i] = new HuCardInfo();
		}

		Arrays.fill(over_time_left, -1);
	}

	public abstract void process_gang_score();

	public abstract void cha_da_jiao();

	public abstract int get_max_pai_xing_fen(int seat_index);

	public int analyse_tiao_pai(int[] cards_index, WeaveItem[] weaveItems, int weaveCount, int[] tiao_cards) {
		int count = 0;
		int[] cards_data = new int[GameConstants.MAX_COUNT];
		int cards_count = _logic.switch_to_cards_data(cards_index, cards_data);

		for (int i = 0; i < weaveCount; i++) {
			int ck = weaveItems[i].weave_kind;
			if (ck == GameConstants.WIK_SUO_GANG_1 || ck == GameConstants.WIK_SUO_GANG_2 || ck == GameConstants.WIK_SUO_GANG_3) {
				int card = weaveItems[i].center_card;

				for (int j = 0; j < cards_count; j++) {
					if (cards_data[j] == card)
						tiao_cards[count++] = card;
				}
			}
		}

		return count;
	}

	@Override
	public void shuffle(int[] repertory_card, int[] mj_cards) {
		_all_card_len = repertory_card.length;

		GRR._left_card_count = _all_card_len;

		int xi_pai_count = 0;
		int rand = RandomUtil.generateRandomNumber(5, 9);

		while (xi_pai_count < 9 && xi_pai_count < rand) {
			if (xi_pai_count == 0)
				_logic.random_card_data(repertory_card, mj_cards);
			else
				_logic.random_card_data(repertory_card, repertory_card);

			xi_pai_count++;
		}

		if (getClass().getAnnotation(ThreeDimension.class) != null) {
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

	public boolean has_win() {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (had_hu_pai[i])
				return true;
		}
		return false;
	}

	/**
	 * 重置每小局的部分数据
	 */
	public void reset_small_round_data() {
		mo_pai_count = new int[getTablePlayerNumber()];
		had_ding_que = new boolean[getTablePlayerNumber()];
		had_switch_card = new boolean[getTablePlayerNumber()];
		ding_que_pai_se = new int[getTablePlayerNumber()];
		left_player_count = getTablePlayerNumber();
		had_hu_pai = new boolean[getTablePlayerNumber()];
		next_banker_player = -1;
		win_order = new int[getTablePlayerNumber()];
		win_type = new int[getTablePlayerNumber()];
		switch_card_index = new int[getTablePlayerNumber()][SWITCH_CARD_COUNT];
		score_when_win = new int[getTablePlayerNumber()];
		score_when_abandoned_win = new int[getTablePlayerNumber()];
		passed_gang_cards = new int[getTablePlayerNumber()][GameConstants.MAX_WEAVE];
		passed_gang_count = new int[getTablePlayerNumber()];
		dian_pao_order = new int[getTablePlayerNumber()][getTablePlayerNumber()];
		dian_pao_count = new int[getTablePlayerNumber()];
		an_gang_count = new int[getTablePlayerNumber()];
		zhi_gang_count = new int[getTablePlayerNumber()];
		wan_gang_count = new int[getTablePlayerNumber()];
		dian_gang_count = new int[getTablePlayerNumber()];
		gang_dispatch_count = 0;
		cha_da_jiao = new boolean[getTablePlayerNumber()];
		bei_cha_da_jiao = new boolean[getTablePlayerNumber()];
		table_hu_cards = new int[getTablePlayerNumber()];
		table_hu_card_count = 0;
		gang_shang_gang = false;
		player_gsg = new boolean[getTablePlayerNumber()];
		cur_round_abandoned_gang = -1;
		jia_xin_wu_count = new int[getTablePlayerNumber()];
		gang_pai_weave_index = 0;
		special_fan_shu = new int[getTablePlayerNumber()];
		zfb_ke_fan = new int[getTablePlayerNumber()];
		gang_fan = new int[getTablePlayerNumber()];

		ting_special_fan_shu = new int[getTablePlayerNumber()];
		ting_zfb_ke_fan = new int[getTablePlayerNumber()];
		ting_gang_fan = new int[getTablePlayerNumber()];

		Arrays.fill(is_ting_when_finish, true);
		Arrays.fill(whoProvided, -1);
		Arrays.fill(finallyFanShu, 0);
		Arrays.fill(is_si_ji_fa_cai, false);
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			max_pai_xing_desc[i] = "";
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			ting_pai_fan_shu[i] = new int[GameConstants.MAX_INDEX][GameConstants.MAX_INDEX];
			for (int j = 0; j < GameConstants.MAX_INDEX; j++)
				ting_pai_fan_shu[i][j] = new int[GameConstants.MAX_INDEX];
		}

		if (is_mj_type(GameConstants.GAME_TYPE_LE_SHAN_YAO_JI)) {
			int magic_card_index = _logic.switch_to_card_index(0x11);
			_logic.add_magic_card_index(magic_card_index);
			GRR._especial_card_count = 1;
			GRR._especial_show_cards[0] = 0x11 + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;

			magicCard = 0x11;
			magicCardIndex = magic_card_index;
		}

		if (is_mj_type(GameConstants.GAME_TYPE_LU_ZHOU_GUI)) {
			int magic_card_index = _logic.switch_to_card_index(0x35);
			_logic.add_magic_card_index(magic_card_index);
			GRR._especial_card_count = 1;
			GRR._especial_show_cards[0] = 0x35 + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;

			magicCard = 0x35;
			magicCardIndex = magic_card_index;
		}

		scoreDetails = new ArrayList<>();

		is_bao_ting = new boolean[getTablePlayerNumber()];
		finalGengCount = new int[getTablePlayerNumber()];

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i].clear_cards_abandoned_gang();

			player_switched_cards[i].clear();

			player_hu_card_info[i] = new HuCardInfo();
		}

		if (is_mj_type(GameConstants.GAME_TYPE_XUE_LIU_CHENG_HE)) {
			if (getTablePlayerNumber() == 2 || getTablePlayerNumber() == 3)
				LEFT_CARD = 10;
		}

		Arrays.fill(player_basic_score, BASIC_SCORE);

		Arrays.fill(small_round_total_score, 0);

		Arrays.fill(over_time_left, -1);
		has_ren_shu = new boolean[getTablePlayerNumber()];

		hu_card_list.clear();
	}

	public int get_over_time_value() {
		if (getRuleValue(Constants_SiChuan.GAME_RULE_CHAO_SHI_3) != 0)
			return 180;
		if (getRuleValue(Constants_SiChuan.GAME_RULE_CHAO_SHI_5) != 0)
			return 300;
		if (getRuleValue(Constants_SiChuan.GAME_RULE_CHAO_SHI_10) != 0)
			return 600;
		return 180;
	}

	public boolean remove_card_by_data(int cards[], int card_data) {
		int card_count = cards.length;

		if (card_count == 0) {
			return false;
		}

		int cbDeleteCount = 0;
		int cbTempCardData[] = new int[GameConstants.MAX_COUNT];

		for (int i = 0; i < card_count; i++) {
			cbTempCardData[i] = cards[i];
		}

		for (int i = 0; i < card_count; i++) {
			if (card_data == get_real_card(cbTempCardData[i])) {
				cbDeleteCount++;
				cbTempCardData[i] = 0;
				break;
			}
		}

		if (cbDeleteCount != 1) {
			return false;
		}

		for (int i = 0; i < card_count; i++) {
			cards[i] = 0;
		}

		int cbCardPos = 0;

		for (int i = 0; i < card_count; i++) {
			if (cbTempCardData[i] != 0)
				cards[cbCardPos++] = cbTempCardData[i];
		}

		return true;

	}

	@Override
	public void process_chi_hu_player_operate(int seat_index, int operate_card, boolean rm) {
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		long effect_indexs[] = new long[1];
		if (!chr.opr_and_long(Constants_SiChuan.CHR_ZI_MO).is_empty() || !chr.opr_and_long(Constants_SiChuan.CHR_GANG_KAI).is_empty()
				|| !chr.opr_and_long(Constants_SiChuan.CHR_DG_GANG_KAI).is_empty()) {
			effect_indexs[0] = Constants_SiChuan.CHR_ZI_MO;
		} else {
			effect_indexs[0] = Constants_SiChuan.CHR_JIE_PAO;
		}

		operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, 1, effect_indexs, 1, GameConstants.INVALID_SEAT);

		if (rm) {
			GRR._cards_index[seat_index][_logic.switch_to_card_index(get_real_card(operate_card))]--;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_SHOW_CARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(GameConstants.Show_Card_Si_Chuan);
		roomResponse.setCardCount(1);
		roomResponse.addCardData(operate_card);
		roomResponse.setOperateLen(_logic.get_card_count_by_index(GRR._cards_index[seat_index]));

		send_response_to_other(seat_index, roomResponse);

		// 回放数据
		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = _logic.switch_to_cards_data_sichuan(GRR._cards_index[seat_index], cards, ding_que_pai_se[seat_index]);
		cards[hand_card_count++] = get_real_card(operate_card) + GameConstants.CARD_ESPECIAL_TYPE_HU;

		RoomResponse.Builder tmp_roomResponse = RoomResponse.newBuilder();
		tmp_roomResponse.setType(MsgConstants.RESPONSE_SHOW_CARD);
		tmp_roomResponse.setTarget(seat_index);
		tmp_roomResponse.setCardType(1);
		tmp_roomResponse.setCardCount(hand_card_count);

		for (int j = 0; j < hand_card_count; j++) {
			tmp_roomResponse.addCardData(cards[j]);
		}

		GRR.add_room_response(tmp_roomResponse);
	}

	public void process_duan_xian_chong_lian(int seat_index) {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (win_order[i] != 0) {
				RoomResponse.Builder tmp_roomResponse = RoomResponse.newBuilder();
				tmp_roomResponse.setType(MsgConstants.RESPONSE_SHOW_CARD);
				tmp_roomResponse.setTarget(i);
				tmp_roomResponse.setCardType(GameConstants.Show_Card_Si_Chuan);
				tmp_roomResponse.setCardCount(1);
				tmp_roomResponse.addCardData(GRR._chi_hu_card[i][0]);
				tmp_roomResponse.setOperateLen(_logic.get_card_count_by_index(GRR._cards_index[i]));

				if (i == seat_index)
					operate_player_get_card(i, 1, new int[] { GRR._chi_hu_card[i][0] }, i);

				send_response_to_other(i, tmp_roomResponse);
			}
		}
	}

	public void process_show_hand_card() {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int cards[] = new int[GameConstants.MAX_COUNT];
			int hand_card_count = _logic.switch_to_cards_data_sichuan(GRR._cards_index[i], cards, ding_que_pai_se[i]);

			if (win_order[i] != 0) {
				if (is_mj_type(GameConstants.GAME_TYPE_LE_SHAN_YAO_JI) || is_mj_type(GameConstants.GAME_TYPE_LU_ZHOU_GUI)) {
					if (_logic.is_magic_card(GRR._chi_hu_card[i][0])) {
						cards[hand_card_count++] = GRR._chi_hu_card[i][0] + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
					} else {
						cards[hand_card_count++] = GRR._chi_hu_card[i][0] + GameConstants.CARD_ESPECIAL_TYPE_HU;
					}
				} else {
					cards[hand_card_count++] = GRR._chi_hu_card[i][0] + GameConstants.CARD_ESPECIAL_TYPE_HU;
				}
			}

			operate_show_card(i, GameConstants.Show_Card_HU, hand_card_count, cards, GameConstants.INVALID_SEAT);
		}
	}

	public void process_ren_shu() {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (player_left_score[i] <= 0)
				no_score_left[i] = true;

			if (player_left_score[i] <= 0 && has_ren_shu[i] == false) {
				has_ren_shu[i] = true;
				operate_effect_action(i, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { Constants_SiChuan.WIK_REN_SHU }, 1,
						GameConstants.INVALID_SEAT);
			}
		}
	}

	public boolean is_ting_card(int card, int seat_index) {
		int count = _playerStatus[seat_index]._hu_card_count;
		for (int i = 0; i < count; i++) {
			int tmp_card = get_real_card(_playerStatus[seat_index]._hu_cards[i]);
			if (card == tmp_card) {
				return true;
			}
		}

		if (count == 1 && _playerStatus[seat_index]._hu_cards[0] == -1) {
			// 全听
			return true;
		}

		return false;
	}

	public boolean is_ting_state(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();
		for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
			chr.set_empty();
			int cbCurrentCard = _logic.switch_to_card_data(i);
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					Constants_SiChuan.HU_CARD_TYPE_ZI_MO, seat_index)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public int get_ting_card(int[] cards, int[] cards_index, WeaveItem[] weaveItem, int cbWeaveCount, int seat_index) {
		return 0;
	}

	public int get_ting_card(int[] cards, int[] cards_index, WeaveItem[] weaveItem, int cbWeaveCount, int seat_index, int ting_count) {
		analyse_state = FROM_TING;

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

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					Constants_SiChuan.HU_CARD_TYPE_ZI_MO, seat_index)) {
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
	public int get_banker_pre_seat(int banker_seat) {
		int count = 0;
		int seat = banker_seat;
		do {
			count++;
			seat = (getTablePlayerNumber() + seat - 1) % getTablePlayerNumber();
		} while (count <= 5 && get_players()[seat] == null);
		return seat;
	}

	@Override
	public int get_banker_next_seat(int banker_seat) {
		int count = 0;
		int seat = banker_seat;
		do {
			count++;
			seat = (seat + 1) % getTablePlayerNumber();
		} while (count <= 5 && get_players()[seat] == null);
		return seat;
	}

	public int get_next_seat(int seat_index) {
		int count = 0;
		int seat = seat_index;
		do {
			count++;
			seat = (seat + 1) % getTablePlayerNumber();
		} while (count <= 5 && (get_players()[seat] == null || had_hu_pai[seat]));
		return seat;
	}

	@Override
	protected boolean on_game_start() {
		return false;
	}

	@Override
	public int analyse_qi_shou_hu_pai(int[] cards_index, WeaveItem[] weaveItems, int weave_count, ChiHuRight chiHuRight, int card_type,
			int seat_index) {
		return 0;
	}

	public boolean is_yao_jiu_weave(WeaveItem[] weave_items, int weave_count) {
		for (int i = 0; i < weave_count; i++) {
			if (_logic.get_card_value(weave_items[i].center_card) != 1 && _logic.get_card_value(weave_items[i].center_card) != 9) {
				return false;
			}
		}
		return true;
	}

	public boolean is_zhong_zhang_weave(WeaveItem[] weave_items, int weave_count) {
		for (int i = 0; i < weave_count; i++) {
			if (_logic.get_card_value(weave_items[i].center_card) == 1 || _logic.get_card_value(weave_items[i].center_card) == 9) {
				return false;
			}
		}
		return true;
	}

	public boolean is_zhong_zhang(int[] cards_index, WeaveItem[] weaveItems, int weave_count) {
		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			if (cards_index[i] > 0) {
				int card_value = _logic.get_card_value(_logic.switch_to_card_data(i));
				if (card_value == 1 || card_value == 9) {
					return false;
				}
			}
		}

		for (int i = 0; i < weave_count; i++) {
			if (weaveItems[i].weave_kind == 1 || weaveItems[i].weave_kind == 9) {
				return false;
			}
		}

		return true;
	}

	@Override
	public boolean reset_init_data() {
		if (_cur_round == 0) {
			initBanker();
			record_game_room();
		}

		virtual_online = new boolean[GameConstants.GAME_PLAYER];
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			virtual_online[i] = true;
		}

		gang_dispatch_count = 0;

		_last_dispatch_player = -1;

		_card_can_not_out_after_chi = new int[getTablePlayerNumber()];
		_chi_pai_count = new int[getTablePlayerNumber()][getTablePlayerNumber()];

		_run_player_id = 0;
		// 设置变量
		_provide_card = GameConstants.INVALID_VALUE;
		_out_card_data = GameConstants.INVALID_VALUE;
		_send_card_data = GameConstants.INVALID_VALUE;

		_provide_player = GameConstants.INVALID_SEAT;
		_out_card_player = GameConstants.INVALID_SEAT;
		_current_player = GameConstants.INVALID_SEAT;

		_send_card_count = 0;
		_out_card_count = 0;

		GRR = new GameRoundRecord();
		GRR.setRoom(this);
		GRR._start_time = System.currentTimeMillis() / 1000L;
		GRR._game_type_index = _game_type_index;
		GRR._cur_round = _cur_round;
		_end_reason = GameConstants.INVALID_VALUE;
		istrustee = new boolean[4];
		// 新建
		_playerStatus = new PlayerStatus[4];
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			_playerStatus[i] = new PlayerStatus();
		}

		// _cur_round=8;
		_cur_round++;

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			_playerStatus[i].reset();
		}

		// 牌局回放
		GRR._room_info.setRoomId(getRoom_id());
		GRR._room_info.setGameRuleIndex(_game_rule_index);
		GRR._room_info.setGameRuleDes(get_game_des());
		GRR._room_info.setGameTypeIndex(_game_type_index);
		GRR._room_info.setGameRound(_game_round);
		GRR._room_info.setCurRound(_cur_round);
		GRR._room_info.setGameStatus(_game_status);
		GRR._room_info.setCreatePlayerId(getRoom_owner_account_id());

		// 设置房间的xml玩法信息
		if (commonGameRuleProtos != null) {
			GRR._room_info.setNewRules(commonGameRuleProtos);
		}

		Player rplayer;

		// WalkerGeek 允许少人模式
		// for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
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

			if (is_mj_type(GameConstants.GAME_TYPE_LU_ZHOU_GUI)) {
				if (display_ruan_peng != null) {
					// 贴鬼碰/杠 0不勾中 1勾中
					room_player.setZiBa(display_ruan_peng[i] ? 1 : 0);
				} else {
					room_player.setZiBa(0);
				}
			}
			if (is_mj_type(GameConstants.GAME_TYPE_XUE_LIU_CHENG_HE)) {
				// 玩家底分
				room_player.setZiBa(player_basic_score[i]);
			}

			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}

			GRR._video_recode.addPlayers(room_player);
		}

		GRR._video_recode.setBankerPlayer(_cur_banker);

		return true;
	}

	public void process_hu_cards(int seat_index, int provider_index, int hu_card) {
		HuCardInfo info = player_hu_card_info[seat_index];
		info.hu_cards[info.count] = hu_card;
		info.provider_index[info.count] = provider_index;
		info.count++;
	}

	public void process_over_time_counter(int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_SC_OVER_TIME_COUNTER);

		// 玩家超时托管的读秒时间，如果是-1表示不读秒，如果是大于0就表示是读秒
		if (over_time_left != null) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				roomResponse.addPlayerStatus(over_time_left[i]);
			}
		} else {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				roomResponse.addPlayerStatus(-1);
			}
		}

		send_response_to_room(roomResponse);
	}

	public long analyse_qi_xiao_dui(int cards_index[], int cbWeaveCount) {
		if (cbWeaveCount != 0)
			return GameConstants.WIK_NULL;

		int cbReplaceCount = 0;
		int nGenCount = 0;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];

			// 单牌统计
			if (cbCardCount == 1 || cbCardCount == 3)
				cbReplaceCount++;

			if (cbCardCount == 4) {
				nGenCount++;
			}
		}

		if (cbReplaceCount > 0)
			return GameConstants.WIK_NULL;

		if (nGenCount > 0) {
			if (is_mj_type(GameConstants.GAME_TYPE_LE_SHAN_YAO_JI)) {
				if (nGenCount == 3)
					return Constants_SiChuan.CHR_SAN_LONG_QI_DUI;
				else if (nGenCount == 2)
					return Constants_SiChuan.CHR_SHUANG_LONG_QI_DUI;
				else
					return Constants_SiChuan.CHR_LONG_QI_DUI;
			} else {
				return Constants_SiChuan.CHR_LONG_QI_DUI;
			}
		} else {
			return Constants_SiChuan.CHR_QI_DUI;
		}
	}

	public long analyse_qi_xiao_dui(int cards_index[], int cbWeaveCount, int cur_card, int seat_index) {
		if (cbWeaveCount != 0)
			return GameConstants.WIK_NULL;

		int cbReplaceCount = 0;
		int nGenCount = 0;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		boolean special = false;
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];

			// 单牌统计
			if (cbCardCount == 1 || cbCardCount == 3)
				cbReplaceCount++;

			if (cbCardCount == 4) {
				nGenCount++;
				int tmp_card = _logic.switch_to_card_data(i);
				if (cur_card == tmp_card) {
					special = true;
				}
				if (_handler == _handler_qi_shou) {
					special = true;
				}
				if (_handler == _handler_qi_shou || (cur_card != tmp_card && cur_card != 0)) {
					if (_handler == _handler_qi_shou && nGenCount == 1)
						continue;

					int color = _logic.get_card_color(tmp_card);
					if (analyse_state == FROM_NORMAL || analyse_state == FROM_MAX_COUNT) {
						if (color == 3) {
							special_fan_shu[seat_index] += 2;
						} else {
							special_fan_shu[seat_index] += 1;
						}
					}
				}
			}
		}

		if (cbReplaceCount > 0)
			return GameConstants.WIK_NULL;

		if (nGenCount > 0) {
			if (special) {
				return Constants_SiChuan.CHR_LONG_QI_DUI;
			} else {
				return Constants_SiChuan.CHR_QI_DUI;
			}
		} else {
			return Constants_SiChuan.CHR_QI_DUI;
		}
	}

	public boolean is_jiang_yi_se(int[] cards_index, WeaveItem[] weave_items, int weave_count) {
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

		if (_logic.exist_eat(weave_items, weave_count))
			return false;

		for (int i = 0; i < weave_count; i++) {
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

	@Override
	protected boolean on_handler_game_start() {
		reset_init_data();

		progress_banker_select();

		_game_status = GameConstants.GS_MJ_PLAY;

		GRR._banker_player = _current_player = _cur_banker;

		player_banker_count[_cur_banker]++;

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

	public void cancel_trustee_schedule(int seat_index) {
		if (over_time_trustee[seat_index] == false && over_time_trustee_schedule[seat_index] != null) {
			over_time_trustee_schedule[seat_index].cancel(false);
			over_time_trustee_schedule[seat_index] = null;
			over_time_left[seat_index] = -1;
			process_over_time_counter(seat_index);
		}

		int count = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (over_time_left[i] == -1)
				count++;
		}
		if (count == getTablePlayerNumber())
			schedule_start_time = -1;
	}

	@Override
	protected boolean on_handler_game_finish(int seat_index, int reason) {
		int real_reason = reason;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_ready[i] = 0;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();

		boolean[] has_si_ji_fa_cai = new boolean[getTablePlayerNumber()];
		if (is_mj_type(GameConstants.GAME_TYPE_LE_SHAN_YAO_JI) && has_rule(Constants_SiChuan.GAME_RULE_SI_JI_FA_CAI) && GRR != null) {
			if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
				// 乐山幺鸡麻将 流局和正常结束的时候 处理四鸡发财的分数和小结算显示
				for (int i = 0; i < getTablePlayerNumber(); i++) {

					int winCard = 0xFF;
					if (GRR._win_order[i] != 0)
						winCard = GRR._chi_hu_card[i][0];

					boolean is_si_ji_fa_cai = is_si_ji_fa_cai(GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i], winCard);
					if (is_si_ji_fa_cai) {
						has_si_ji_fa_cai[i] = true;

						int[] row = new int[SCORE_DETAIL_COLUMN];
						row[0] = ScoreRowType.SI_JI_FA_CAI.getType();

						int maxScore = 1 << get_max_fan_shu();
						for (int j = 0; j < getTablePlayerNumber(); j++) {
							if (j == i)
								continue;

							GRR._game_score[i] += maxScore;
							GRR._game_score[j] -= maxScore;

							row[i + 1] += maxScore;
							row[j + 1] -= maxScore;
						}

						scoreDetails.add(row);

						break;
					}
				}
			}
		}

		roomResponse.setLeftCardCount(0);

		load_common_status(roomResponse);
		load_room_info_data(roomResponse);

		RoomInfo.Builder room_info = getRoomInfo();
		game_end.setRoomInfo(room_info);
		game_end.setRunPlayerId(_run_player_id);
		game_end.setRoundOverType(0);
		game_end.setGamePlayerNumber(getTablePlayerNumber());
		game_end.setEndTime(System.currentTimeMillis() / 1000L);

		// 回放时、查牌时，牌桌上胡牌的顺序
		MJ_Game_End_Basic.Builder basic_game_end = MJ_Game_End_Basic.newBuilder();
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			basic_game_end.addPao(null != ding_que_pai_se ? ding_que_pai_se[i] : 0);
			basic_game_end.addNao(null != win_order ? win_order[i] : 0);
			basic_game_end.addQiang(null != win_type ? win_type[i] : 0);
			if (!has_rule(Constants_SiChuan.GAME_RULE_HUAN_SAN_ZHANG)) {
				basic_game_end.addBianYan(3);
			} else {
				basic_game_end.addBianYan((null != had_switch_card) ? (had_switch_card[i] ? 1 : 2) : 2);
			}

			basic_game_end.addIsTingWhenFinish(is_ting_when_finish[i]);
			basic_game_end.addWhoProvided(whoProvided[i]);
			basic_game_end.addFinallyFanShu(finallyFanShu[i]);
		}

		if (is_mj_type(GameConstants.GAME_TYPE_LE_SHAN_YAO_JI) || is_mj_type(GameConstants.GAME_TYPE_XUE_LIU_CHENG_HE)) {
			// 添加流水明显
			for (int i = 0; i < scoreDetails.size(); i++) {
				ScoreRow.Builder scoreRow = ScoreRow.newBuilder();
				int[] row = scoreDetails.get(i);
				if (row.length == SCORE_DETAIL_COLUMN) {
					scoreRow.setType(row[0]);
					scoreRow.setPScore1(row[1]);
					scoreRow.setPScore2(row[2]);
					scoreRow.setPScore3(row[3]);
					scoreRow.setPScore4(row[4]);
				}
				basic_game_end.addAllScoreDetails(scoreRow);
			}
		}

		game_end.setCommResponse(PBUtil.toByteString(basic_game_end));

		if (GRR != null) {
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
			float lGangScore[] = new float[getTablePlayerNumber()];

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				for (int j = 0; j < getTablePlayerNumber(); j++) {
					_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
				}
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				GRR._game_score[i] += lGangScore[i];
				GRR._game_score[i] += GRR._start_hu_score[i]; // 起手胡分数

				_player_result.game_score[i] += GRR._game_score[i];
			}

			load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);

			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);
			game_end.setLeftCardCount(GRR._left_card_count); // 剩余牌
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

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				game_end.addHuResult(GRR._hu_result[i]);
				// 胡的牌
				Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();

				for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
					int card = get_real_card(GRR._chi_hu_card[i][j]);
					if (card != 0 && _logic.is_magic_card(card)) {
						hc.addItem(card + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
					} else {
						hc.addItem(card);
					}
				}

				for (int h = 0; h < GRR._chi_hu_card[i].length; h++) {
					int card = get_real_card(GRR._chi_hu_card[i][h]);
					if (card != 0 && _logic.is_magic_card(card)) {
						game_end.addHuCardData(card + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
					} else {
						game_end.addHuCardData(card);
					}
				}

				game_end.addHuCardArray(hc);
			}

			// 现在权值只有一位
			long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

			// 设置胡牌描述
			set_result_describe();

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				GRR._result_des[i] = max_pai_xing_desc[i] + GRR._result_des[i];
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (has_si_ji_fa_cai[i])
					GRR._result_des[i] += " 四鸡发财";
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				GRR._card_count[i] = _logic.switch_to_cards_data_sichuan(GRR._cards_index[i], GRR._cards_data[i], ding_que_pai_se[i]);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {
					cs.addItem(GRR._cards_data[i][j]);
				}
				game_end.addCardsData(cs); // 牌

				// 组合
				WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
				for (int j = 0; j < GRR._weave_count[i]; j++) {
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
				if (is_mj_type(GameConstants.GAME_TYPE_XUE_LIU_CHENG_HE)) {
					game_end.addGameScore(small_round_total_score[i]); // 本局输赢

					game_end.addGangCount(player_left_score[i] - start_score); // 小结算时的总输赢分

					game_end.addStartHuScore(start_score); // 游戏的初始分
				} else {
					game_end.addGameScore(GRR._game_score[i]);
					game_end.addStartHuScore(GRR._start_hu_score[i]);
				}
				game_end.addGangScore(lGangScore[i]); // 杠牌得分
				game_end.addResultDes(GRR._result_des[i]);

				if (is_mj_type(GameConstants.GAME_TYPE_XUE_LIU_CHENG_HE)) {
					// 胡牌次数
					game_end.addJettonScore(player_hu_card_info[i].count);
				}

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

		if (is_mj_type(GameConstants.GAME_TYPE_XUE_LIU_CHENG_HE)) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (no_score_left[i])
					_cur_round = _game_round;
			}
		}

		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round && (!is_sys())) { // 局数到了
				end = true;
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(process_player_result(reason));
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
			real_reason = GameConstants.Game_End_DRAW; // 流局
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(process_player_result(reason));
		}
		game_end.setEndType(real_reason);

		// 得分总的
		roomResponse.setGameEnd(game_end);

		send_response_to_room(roomResponse);

		record_game_round(game_end);

		// 超时解散
		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT || reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < getTablePlayerNumber(); j++) {
				Player player = get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");
			}
		}

		if (end && (!is_sys())) {
			PlayerServiceImpl.getInstance().delRoomId(getRoom_id());
		}

		if (!is_sys()) {
			GRR = null;
		}

		if (is_sys()) {
			clear_score_in_gold_room();
		}

		Arrays.fill(over_time_trustee, false);

		return true;
	}

	public boolean exe_switch_card() {
		set_handler(_handler_switch_card);
		_handler.exe(this);
		return true;
	}

	public boolean exe_ding_que() {
		set_handler(_handler_ding_que);
		_handler.exe(this);
		return true;
	}

	@Override
	protected void onInitTable() {
		_handler_chi_peng = new HandlerChiPeng_SiChuan();
		_handler_dispath_card = new HandlerDispatchCard_SiChuan();
		_handler_gang = new HandlerGang_SiChuan();
		_handler_out_card_operate = new HandlerOutCardOperate_SiChuan();

		_handler_switch_card = new HandlerSwitchCard_SiChuan();
		_handler_ding_que = new HandlerDingQue_SiChuan();

		_handler_qi_shou = new HandlerQiShou_SiChuan();

		hasRuleDingQue = has_rule(Constants_SiChuan.GAME_RULE_DING_QUE);
		hasRuleBiPai = has_rule(Constants_SiChuan.GAME_RULE_BI_PAI);
		hasRuleMaiMa = has_rule(Constants_SiChuan.GAME_RULE_ZHUANG_JIA_MAI_MA);
		hasRuleRuanGang = has_rule(Constants_SiChuan.GAME_RULE_RUAN_PENG_KE_GANG);
		hasRuleWuJiGangDouble = getRuleValue(Constants_SiChuan.GAME_RULE_WU_JI_GANG_DOUBLE) != 0;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			max_pai_xing_desc[i] = "";
		}

		hasRuleFourMagic = getRuleValue(Constants_SiChuan.GAME_RULE_FOUR_MAGIC) != 0;
		hasRuleEightMagic = getRuleValue(Constants_SiChuan.GAME_RULE_EIGHT_MAGIC) != 0;
		hasRuleTwelveMagic = getRuleValue(Constants_SiChuan.GAME_RULE_TWELVE_MAGIC) != 0;
		hasRuleFengDing20 = getRuleValue(Constants_SiChuan.GAME_RULE_FENG_DING_20KE) != 0;
		hasRuleFengDing40 = getRuleValue(Constants_SiChuan.GAME_RULE_FENG_DING_40KE) != 0;
		hasRuleFengDing80 = getRuleValue(Constants_SiChuan.GAME_RULE_FENG_DING_80KE) != 0;
		hasRuleDuanGang = getRuleValue(Constants_SiChuan.GAME_RULE_DUAN_GANG) != 0;

		if (getRuleValue(Constants_SiChuan.GAME_RULE_DI_FEN_1) != 0)
			BASIC_SCORE = 1;
		else if (getRuleValue(Constants_SiChuan.GAME_RULE_DI_FEN_3) != 0)
			BASIC_SCORE = 3;
		else if (getRuleValue(Constants_SiChuan.GAME_RULE_DI_FEN_5) != 0)
			BASIC_SCORE = 5;

		Arrays.fill(player_basic_score, BASIC_SCORE);

		if (getRuleValue(Constants_SiChuan.GAME_RULE_QI_SHI_100) != 0)
			start_score = 100;
		else if (getRuleValue(Constants_SiChuan.GAME_RULE_QI_SHI_200) != 0)
			start_score = 200;
		else if (getRuleValue(Constants_SiChuan.GAME_RULE_QI_SHI_50) != 0)
			start_score = 50;

		Arrays.fill(player_left_score, start_score);
	}

	@Override
	public int get_real_card(int card) {
		return card % 100;
	}

	public boolean estimate_player_out_card_respond(int seat_index, int card, int type) {
		boolean bAroseAction = false;

		int pai_se = _logic.get_card_color(card) + 1;

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

			if (win_order[i] != 0) {
				continue;
			}

			if (pai_se == ding_que_pai_se[i]) {
				continue;
			}

			playerStatus = _playerStatus[i];

			boolean can_peng_this_card = true;
			int[] tmp_cards_data = _playerStatus[i].get_cards_abandoned_peng();
			for (int x = 0; x < GameConstants.MAX_ZI_FENG; x++) {
				if (tmp_cards_data[x] == card) {
					can_peng_this_card = false;
					break;
				}
			}

			if (can_peng_this_card && !is_bao_ting[i]) {
				if (is_mj_type(GameConstants.GAME_TYPE_LE_SHAN_YAO_JI)) {
					action = _logic.check_peng_with_suo_pai(GRR._cards_index[i], card);
					if (action != 0) {
						// 如果碰完之后只有两张幺鸡了 不能让玩家碰牌
						int cardCount = _logic.get_card_count_by_index(GRR._cards_index[i]);

						if ((action & GameConstants.WIK_PENG) != 0) {
							boolean can_continue = true;
							if (cardCount == 4) {
								int[] tmp_cards_index = Arrays.copyOf(GRR._cards_index[i], GRR._cards_index[i].length);
								int[] remove_card = new int[] { card, card };
								if (_logic.remove_cards_by_index(tmp_cards_index, remove_card, 2)) {
									if (tmp_cards_index[magicCardIndex] == 2)
										can_continue = false;
								}
							}

							if (can_continue) {
								playerStatus.add_normal_wik(card, GameConstants.WIK_PENG, seat_index);
								playerStatus.add_action(GameConstants.WIK_PENG);
								bAroseAction = true;
							}
						} else if ((action & GameConstants.WIK_SUO_PENG_1) != 0) {
							boolean can_continue = true;
							if (cardCount == 4) {
								int[] tmp_cards_index = Arrays.copyOf(GRR._cards_index[i], GRR._cards_index[i].length);
								int[] remove_card = new int[] { magicCard, card };
								if (_logic.remove_cards_by_index(tmp_cards_index, remove_card, 2)) {
									if (tmp_cards_index[magicCardIndex] == 2)
										can_continue = false;
								}
							}

							if (can_continue) {
								playerStatus.add_normal_wik(card, GameConstants.WIK_SUO_PENG_1, seat_index);
								playerStatus.add_action(GameConstants.WIK_PENG);
								bAroseAction = true;
							}
						} else if ((action & GameConstants.WIK_SUO_PENG_2) != 0) {
							boolean can_continue = true;
							if (cardCount == 4) {
								int[] tmp_cards_index = Arrays.copyOf(GRR._cards_index[i], GRR._cards_index[i].length);
								int[] remove_card = new int[] { magicCard, magicCard };
								if (_logic.remove_cards_by_index(tmp_cards_index, remove_card, 2)) {
									if (tmp_cards_index[magicCardIndex] == 2)
										can_continue = false;
								}
							}

							if (can_continue) {
								playerStatus.add_normal_wik(card, GameConstants.WIK_SUO_PENG_2, seat_index);
								playerStatus.add_action(GameConstants.WIK_PENG);
								bAroseAction = true;
							}
						}
					}
				} else {
					action = _logic.check_peng(GRR._cards_index[i], card);
					if (action != 0) {
						playerStatus.add_action(action);
						playerStatus.add_peng(card, seat_index);
						bAroseAction = true;
					}
				}
			}

			if (GRR._left_card_count > LEFT_CARD) {
				if (is_bao_ting[i]) {
					action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
					if (action != 0) {
						// 删除手牌并放入落地牌之前，保存状态数据信息
						int tmp_card_index = _logic.switch_to_card_index(card);
						int tmp_card_count = GRR._cards_index[i][tmp_card_index];
						int tmp_weave_count = GRR._weave_count[i];

						// 删除手牌并加入一个落地牌组合，别人出牌时，杠都是明杠，因为等下分析听牌时要用
						GRR._cards_index[i][tmp_card_index] = 0;
						GRR._weave_items[i][tmp_weave_count].public_card = 1;
						GRR._weave_items[i][tmp_weave_count].center_card = card;
						GRR._weave_items[i][tmp_weave_count].weave_kind = GameConstants.WIK_GANG;
						GRR._weave_items[i][tmp_weave_count].provide_player = seat_index;
						++GRR._weave_count[i];

						boolean is_ting_state = is_ting_state(GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i], i);

						// 还原手牌数据和落地牌数据
						GRR._cards_index[i][tmp_card_index] = tmp_card_count;
						GRR._weave_count[i] = tmp_weave_count;

						// 杠牌之后还是听牌状态，并不需要在gang handler里更新听牌状态，只要出牌时更新就可以
						if (is_ting_state) {
							playerStatus.add_action(GameConstants.WIK_GANG);
							playerStatus.add_gang(card, seat_index, 1);
							bAroseAction = true;
						}
					}
				} else if (is_mj_type(GameConstants.GAME_TYPE_LE_SHAN_YAO_JI) || is_mj_type(GameConstants.GAME_TYPE_LU_ZHOU_GUI)) {
					action = _logic.estimate_gang_card_with_suo_pai(GRR._cards_index[i], card);
					if (action != 0) {
						playerStatus.add_action(GameConstants.WIK_GANG);
						bAroseAction = true;

						if ((action & GameConstants.WIK_GANG) != 0) {
							playerStatus.add_normal_gang_wik(card, GameConstants.WIK_GANG, seat_index, 1);
						}
						if ((action & GameConstants.WIK_SUO_GANG_1) != 0) {
							playerStatus.add_normal_gang_wik(card, GameConstants.WIK_SUO_GANG_1, seat_index, 1);
						}
						if ((action & GameConstants.WIK_SUO_GANG_2) != 0) {
							playerStatus.add_normal_gang_wik(card, GameConstants.WIK_SUO_GANG_2, seat_index, 1);
						}
						if ((action & GameConstants.WIK_SUO_GANG_3) != 0) {
							playerStatus.add_normal_gang_wik(card, GameConstants.WIK_SUO_GANG_3, seat_index, 1);
						}
					}
				} else {
					action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
					if (action != 0) {
						playerStatus.add_action(GameConstants.WIK_GANG);
						playerStatus.add_gang(card, seat_index, 1);
						bAroseAction = true;
					}
				}
			}

			if (_playerStatus[i].is_chi_hu_round()) {
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();

				int hu_card_type = Constants_SiChuan.HU_CARD_TYPE_JIE_PAO;
				if (type == GameConstants.WIK_GANG)
					hu_card_type = Constants_SiChuan.HU_CARD_TYPE_GANG_PAO;

				analyse_state = FROM_NORMAL;
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i], card, chr, hu_card_type, i);
				if (action != 0) {
					if (is_mj_type(GameConstants.GAME_TYPE_GUANG_AN)) {
						playerStatus.add_action(GameConstants.WIK_CHI_HU);
						playerStatus.add_chi_hu(card, seat_index);
						bAroseAction = true;
					} else {
						// 接炮时，总得分变大了，才能接炮胡
						if (score_when_win[i] > score_when_abandoned_win[i]) {
							playerStatus.add_action(GameConstants.WIK_CHI_HU);
							playerStatus.add_chi_hu(card, seat_index);
							bAroseAction = true;
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

	public boolean check_finished() {
		int count = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (no_score_left[i])
				count++;
		}

		if (count == getTablePlayerNumber() || count == getTablePlayerNumber() - 1)
			return true;

		return false;
	}

	public boolean liu_ju() {
		if (GRR._left_card_count <= LEFT_CARD || check_finished() == true) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (over_time_trustee_schedule != null && over_time_trustee_schedule[i] != null) {
					over_time_trustee_schedule[i].cancel(false);
					over_time_trustee_schedule[i] = null;
				}
			}

			// 处理杠分
			process_gang_score();

			// 查大叫
			cha_da_jiao();

			operate_player_score();

			process_show_hand_card();

			int seat = _cur_banker;
			do {
				seat = (seat + 1) % getTablePlayerNumber();
			} while (get_players()[seat] == null);

			_cur_banker = seat;

			if (has_win()) {
				GameSchedule.put(new GameFinishRunnable(getRoom_id(), _cur_banker, GameConstants.Game_End_NORMAL), GameConstants.GAME_FINISH_DELAY,
						TimeUnit.SECONDS);
			} else {
				GameSchedule.put(new GameFinishRunnable(getRoom_id(), _cur_banker, GameConstants.Game_End_DRAW), GameConstants.GAME_FINISH_DELAY,
						TimeUnit.SECONDS);
			}

			return true;
		}
		return false;
	}

	public boolean operate_player_score() {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_SC_OPERATE_RT_SCORE);
		roomResponse.setGameStatus(_game_status);

		load_player_info_data(roomResponse);

		if (GRR != null)
			GRR.add_room_response(roomResponse);

		send_response_to_room(roomResponse);

		process_ren_shu();

		return true;
	}

	public boolean operate_player_hu_cards() {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_SC_OPERATE_RT_HU_CARDS);
		roomResponse.setGameStatus(_game_status);

		RoundHuCards.Builder rhd = RoundHuCards.newBuilder();
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			HuCardList.Builder hcl = HuCardList.newBuilder();

			HuCardInfo info = player_hu_card_info[i];
			for (int j = 0; j < info.count; j++) {
				HuCard.Builder hc = HuCard.newBuilder();
				hc.setCard(info.hu_cards[j]);
				hc.setProviderIndex(info.provider_index[j]);

				hcl.addHuCards(hc);
			}

			rhd.addAllHuCardsList(hcl);
		}

		for (int card : hu_card_list) {
			roomResponse.addCardData(card);
		}

		roomResponse.setCommResponse(PBUtil.toByteString(rhd));

		if (GRR != null)
			GRR.add_room_response(roomResponse);

		send_response_to_room(roomResponse);

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
			// 玩家胡牌的顺序，1，2，3
			room_player.setNao(null != win_order ? win_order[i] : 0);
			// 玩家胡牌的类型，1，自摸，2，胡
			room_player.setQiang(null != win_type ? win_type[i] : 0);
			// 玩家是否已经换三张，1，是，2，否，3，没换三张玩法
			if (!has_rule(Constants_SiChuan.GAME_RULE_HUAN_SAN_ZHANG)) {
				room_player.setBiaoyan(3);
			} else {
				room_player.setBiaoyan((null != had_switch_card) ? (had_switch_card[i] ? 1 : 2) : 2);
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

	public boolean check_gang_huan_zhang(int seat_index, int card) {
		int gang_card_index = _logic.switch_to_card_index(card);
		int gang_card_count = GRR._cards_index[seat_index][gang_card_index];

		GRR._cards_index[seat_index][gang_card_index] = 0;

		int hu_cards[] = new int[GameConstants.MAX_INDEX];

		int hu_card_count = get_ting_card(hu_cards, GRR._cards_index[seat_index], GRR._weave_items[seat_index], GRR._weave_count[seat_index],
				seat_index, 0);

		GRR._cards_index[seat_index][gang_card_index] = gang_card_count;

		if (hu_card_count != _playerStatus[seat_index]._hu_card_count) {
			return true;
		} else {
			for (int j = 0; j < hu_card_count; j++) {
				int real_card = get_real_card(_playerStatus[seat_index]._hu_cards[j]);
				if (real_card != hu_cards[j]) {
					return true;
				}
			}
		}

		return false;
	}

	@Override
	public boolean estimate_gang_respond(int seat_index, int card) {
		boolean bAroseAction = false;

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			if (playerStatus.is_chi_hu_round() && win_order[i] == 0) {
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();

				analyse_state = FROM_NORMAL;
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i], card, chr,
						Constants_SiChuan.HU_CARD_TYPE_QIANG_GANG, i);

				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);
					bAroseAction = true;
				}
			}
		}

		if (bAroseAction == true) {
			_provide_player = seat_index;
			_provide_card = card;
			_resume_player = _current_player;
			_current_player = GameConstants.INVALID_SEAT;
		}

		return bAroseAction;
	}

	@Override
	public boolean on_game_start_new() {
		_logic.clean_magic_cards();

		reset_small_round_data();

		_game_status = GameConstants.GS_MJ_PLAY;

		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants.MAX_COUNT];
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int hand_card_count = _logic.switch_to_cards_data_sichuan(GRR._cards_index[i], hand_cards[i], ding_que_pai_se[i]);
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

		if (is_mj_type(GameConstants.GAME_TYPE_LU_ZHOU_GUI)) {
			exe_qi_shou(GRR._banker_player, GameConstants.WIK_NULL);
		} else if (is_mj_type(GameConstants.GAME_TYPE_MJ_SRLF)) {
			if (has_rule(Constants_SiChuan.GAME_RULE_HUAN_SAN_ZHANG)) {
				exe_switch_card();
			} else {
				exe_qi_shou(GRR._banker_player, GameConstants.WIK_NULL);
			}
		} else if (is_mj_type(GameConstants.GAME_TYPE_GUANG_AN)) {
			if (has_rule(Constants_SiChuan.GAME_RULE_HUAN_SAN_ZHANG)) {
				exe_switch_card();
			} else if (has_rule(Constants_SiChuan.GAME_RULE_DING_QUE)) {
				if (getTablePlayerNumber() == 4) {
					exe_ding_que();
				} else {
					exe_qi_shou(GRR._banker_player, GameConstants.WIK_NULL);
				}
			} else {
				exe_qi_shou(GRR._banker_player, GameConstants.WIK_NULL);
			}
		} else if (is_mj_type(GameConstants.GAME_TYPE_LE_SHAN_YAO_JI)) {
			if (has_rule(Constants_SiChuan.GAME_RULE_HUAN_SAN_ZHANG)) {
				exe_switch_card();
			} else if (getTablePlayerNumber() == 4) {
				exe_ding_que();
			} else {
				exe_qi_shou(GRR._banker_player, GameConstants.WIK_NULL);
			}
		} else {
			if (has_rule(Constants_SiChuan.GAME_RULE_HUAN_SAN_ZHANG)) {
				exe_switch_card();
			} else {
				exe_ding_que();
			}
		}

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

		if (weave_count > 0) {
			for (int j = 0; j < weave_count; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				if (is_mj_type(GameConstants.GAME_TYPE_XUE_LIU_CHENG_HE)) {
					weaveItem_item.setProvidePlayer(weaveitems[j].provide_player + GameConstants.WEAVE_SHOW_DIRECT);
				} else {
					weaveItem_item.setProvidePlayer(weaveitems[j].provide_player);
				}
				weaveItem_item.setPublicCard(weaveitems[j].public_card);
				weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
				weaveItem_item.setCenterCard(weaveitems[j].center_card);
				roomResponse.addWeaveItems(weaveItem_item);
			}
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (i == seat_index)
				continue;

			roomResponse.clearDouliuzi();

			int count = _playerStatus[i]._hu_card_count;
			for (int j = 0; j < count; j++) {
				int fanShu = ting_pai_fan_shu[i][0][j];
				roomResponse.addDouliuzi(fanShu);
			}

			send_response_to_player(i, roomResponse);
		}

		for (int j = 0; j < card_count; j++) {
			roomResponse.addCardData(cards[j]);
		}

		roomResponse.clearDouliuzi();

		int count = _playerStatus[seat_index]._hu_card_count;
		for (int j = 0; j < count; j++) {
			int fanShu = ting_pai_fan_shu[seat_index][0][j];
			roomResponse.addDouliuzi(fanShu);
		}

		GRR.add_room_response(roomResponse);

		send_response_to_player(seat_index, roomResponse);

		return true;
	}

	@Override
	public boolean handler_requst_pao_qiang(Player player, int pai_se, int other) {
		return _handler_ding_que.handler_ding_que(this, player.get_seat_index(), pai_se);
	}

	@Override
	public boolean handler_requst_message_deal(Player player, int seat_index, RoomRequest room_rq, int type) {
		if (type == MsgConstants.REQUEST_SWITCH_CARDS) {
			int size = room_rq.getOutCardsList().size();
			int[] cards = new int[size];

			for (int i = 0; i < size; i++) {
				cards[i] = room_rq.getOutCardsList().get(i);
			}

			_handler_switch_card.handler_switch_cards(this, seat_index, cards);
		} else if (type == MsgConstants.REQUEST_SC_MJ_LIU_SHUI) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_SC_SHOW_SCORE_DETAIL);

			MJ_Game_End_Basic.Builder basic_game_end = MJ_Game_End_Basic.newBuilder();

			// 添加流水明显
			for (int i = 0; i < scoreDetails.size(); i++) {
				ScoreRow.Builder scoreRow = ScoreRow.newBuilder();
				int[] row = scoreDetails.get(i);
				if (row.length == SCORE_DETAIL_COLUMN) {
					scoreRow.setType(row[0]);
					scoreRow.setPScore1(row[1]);
					scoreRow.setPScore2(row[2]);
					scoreRow.setPScore3(row[3]);
					scoreRow.setPScore4(row[4]);
				}
				basic_game_end.addAllScoreDetails(scoreRow);
			}

			roomResponse.setCommResponse(PBUtil.toByteString(basic_game_end));

			send_response_to_player(seat_index, roomResponse);
		}

		return true;
	}

	public int get_max_fan_shu() {
		if (has_rule(Constants_SiChuan.GAME_RULE_FD_2_FAN)) {
			return 2;
		}
		if (has_rule(Constants_SiChuan.GAME_RULE_FD_3_FAN)) {
			return 3;
		}
		if (has_rule(Constants_SiChuan.GAME_RULE_FD_4_FAN)) {
			return 4;
		}
		if (has_rule(Constants_SiChuan.GAME_RULE_FD_5_FAN)) {
			return 5;
		}
		return 5;
	}

	public int get_geng_count(int[] cards_index, WeaveItem[] weave_items, int weave_count, int hu_card) {
		int geng = 0;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		if (_logic.is_valid_card(hu_card))
			cbCardIndexTemp[_logic.switch_to_card_index(hu_card)]++;

		for (int i = 0; i < weave_count; i++) {
			if (weave_items[i].weave_kind == GameConstants.WIK_GANG) {
				int card_index = _logic.switch_to_card_index(weave_items[i].center_card);
				cbCardIndexTemp[card_index] += 4;
			}
			if (weave_items[i].weave_kind == GameConstants.WIK_SUO_GANG_1) {
				int card_index = _logic.switch_to_card_index(weave_items[i].center_card);
				cbCardIndexTemp[card_index] += 3;
				cbCardIndexTemp[magicCardIndex] += 1;
			}
			if (weave_items[i].weave_kind == GameConstants.WIK_SUO_GANG_2) {
				int card_index = _logic.switch_to_card_index(weave_items[i].center_card);
				cbCardIndexTemp[card_index] += 2;
				cbCardIndexTemp[magicCardIndex] += 2;
			}
			if (weave_items[i].weave_kind == GameConstants.WIK_SUO_GANG_3) {
				int card_index = _logic.switch_to_card_index(weave_items[i].center_card);
				cbCardIndexTemp[card_index] += 1;
				cbCardIndexTemp[magicCardIndex] += 3;
			}
			if (weave_items[i].weave_kind == GameConstants.WIK_PENG) {
				int card_index = _logic.switch_to_card_index(weave_items[i].center_card);
				cbCardIndexTemp[card_index] += 3;
			}
			if (weave_items[i].weave_kind == GameConstants.WIK_SUO_PENG_1) {
				int card_index = _logic.switch_to_card_index(weave_items[i].center_card);
				cbCardIndexTemp[card_index] += 2;
				cbCardIndexTemp[magicCardIndex] += 1;
			}
			if (weave_items[i].weave_kind == GameConstants.WIK_SUO_PENG_2) {
				int card_index = _logic.switch_to_card_index(weave_items[i].center_card);
				cbCardIndexTemp[card_index] += 1;
				cbCardIndexTemp[magicCardIndex] += 2;
			}
		}

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (cbCardIndexTemp[i] >= 4) {
				geng++;
			}
		}

		return geng;
	}

	public boolean is_si_ji_fa_cai(int[] cards_index, WeaveItem[] weave_items, int weave_count, int hu_card) {
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		if (_logic.is_valid_card(hu_card))
			cbCardIndexTemp[_logic.switch_to_card_index(hu_card)]++;

		for (int i = 0; i < weave_count; i++) {
			if (weave_items[i].weave_kind == GameConstants.WIK_GANG) {
				int card_index = _logic.switch_to_card_index(weave_items[i].center_card);
				cbCardIndexTemp[card_index] += 4;
			}
			if (weave_items[i].weave_kind == GameConstants.WIK_SUO_GANG_1) {
				int card_index = _logic.switch_to_card_index(weave_items[i].center_card);
				cbCardIndexTemp[card_index] += 3;
				cbCardIndexTemp[magicCardIndex] += 1;
			}
			if (weave_items[i].weave_kind == GameConstants.WIK_SUO_GANG_2) {
				int card_index = _logic.switch_to_card_index(weave_items[i].center_card);
				cbCardIndexTemp[card_index] += 2;
				cbCardIndexTemp[magicCardIndex] += 2;
			}
			if (weave_items[i].weave_kind == GameConstants.WIK_SUO_GANG_3) {
				int card_index = _logic.switch_to_card_index(weave_items[i].center_card);
				cbCardIndexTemp[card_index] += 1;
				cbCardIndexTemp[magicCardIndex] += 3;
			}
			if (weave_items[i].weave_kind == GameConstants.WIK_PENG) {
				int card_index = _logic.switch_to_card_index(weave_items[i].center_card);
				cbCardIndexTemp[card_index] += 3;
			}
			if (weave_items[i].weave_kind == GameConstants.WIK_SUO_PENG_1) {
				int card_index = _logic.switch_to_card_index(weave_items[i].center_card);
				cbCardIndexTemp[card_index] += 2;
				cbCardIndexTemp[magicCardIndex] += 1;
			}
			if (weave_items[i].weave_kind == GameConstants.WIK_SUO_PENG_2) {
				int card_index = _logic.switch_to_card_index(weave_items[i].center_card);
				cbCardIndexTemp[card_index] += 1;
				cbCardIndexTemp[magicCardIndex] += 2;
			}
		}

		if (cbCardIndexTemp[magicCardIndex] == 4)
			return true;

		return false;
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		return false;
	}

	@Override
	public boolean operate_player_cards_with_ting(int seat_index, int card_count, int cards[], int weave_count, WeaveItem weaveitems[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS); // 12更新玩家手牌数据
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(1);

		load_common_status(roomResponse);

		roomResponse.setCardCount(card_count);
		roomResponse.setWeaveCount(weave_count);

		if (weave_count > 0) {
			for (int j = 0; j < weave_count; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				if (is_mj_type(GameConstants.GAME_TYPE_XUE_LIU_CHENG_HE)) {
					weaveItem_item.setProvidePlayer(weaveitems[j].provide_player + GameConstants.WEAVE_SHOW_DIRECT);
				} else {
					weaveItem_item.setProvidePlayer(weaveitems[j].provide_player);
				}
				weaveItem_item.setPublicCard(weaveitems[j].public_card);
				weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
				weaveItem_item.setCenterCard(weaveitems[j].center_card);
				roomResponse.addWeaveItems(weaveItem_item);
			}
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (i == seat_index)
				continue;

			roomResponse.clearDouliuzi();

			int count = _playerStatus[i]._hu_card_count;
			for (int j = 0; j < count; j++) {
				int fanShu = ting_pai_fan_shu[i][0][j];
				roomResponse.addDouliuzi(fanShu);
			}

			send_response_to_player(i, roomResponse);
		}

		for (int j = 0; j < card_count; j++) {
			roomResponse.addCardData(cards[j]);
		}

		int ting_count = _playerStatus[seat_index]._hu_out_card_count;

		roomResponse.setOutCardCount(ting_count);

		roomResponse.clearDouliuzi();

		for (int i = 0; i < ting_count; i++) {
			int ting_card_cout = _playerStatus[seat_index]._hu_out_card_ting_count[i];

			roomResponse.addOutCardTingCount(ting_card_cout);
			roomResponse.addOutCardTing(_playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants.CARD_ESPECIAL_TYPE_TING);

			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < ting_card_cout; j++) {
				int_array.addItem(_playerStatus[seat_index]._hu_out_cards[i][j]);
			}
			roomResponse.addOutCardTingCards(int_array);

			for (int j = 0; j < ting_card_cout; j++) {
				roomResponse.addDouliuzi(ting_pai_fan_shu[seat_index][i][j]);
			}
		}

		for (int i = 0; i < table_hu_card_count; i++) {
			roomResponse.addCardsList(table_hu_cards[i]);
		}

		send_response_to_player(seat_index, roomResponse);

		GRR.add_room_response(roomResponse);

		return true;
	}

	@Override
	public boolean operate_chi_hu_cards(int seat_index, int count, int cards[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_CHI_HU_CARDS);
		roomResponse.setTarget(seat_index);

		for (int i = 0; i < table_hu_card_count; i++) {
			roomResponse.addCardsList(table_hu_cards[i]);
		}

		for (int i = 0; i < count; i++) {
			roomResponse.addChiHuCards(cards[i]);
		}

		for (int j = 0; j < count; j++) {
			roomResponse.addDouliuzi(ting_pai_fan_shu[seat_index][0][j]);
		}

		send_response_to_player(seat_index, roomResponse);

		return true;
	}

	public int analyse_chi_hu_card(int cards_index[], WeaveItem weaveItems[], int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index, int _provide_index) {
		return 0;
	}

	@Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x07, 0x07, 0x07, 0x15, 0x15, 0x15, 0x18, 0x18, 0x18, 0x01, 0x01, 0x01, 0x03, 0x03 };
		int[] cards_of_player1 = new int[] { 0x07, 0x07, 0x07, 0x15, 0x15, 0x15, 0x18, 0x18, 0x18, 0x01, 0x01, 0x01, 0x03, 0x03 };
		int[] cards_of_player2 = new int[] { 0x01, 0x01, 0x02, 0x05, 0x05, 0x05, 0x08, 0x21, 0x21, 0x23, 0x26, 0x13, 0x14, 0x11 };
		int[] cards_of_player3 = new int[] { 0x11, 0x11, 0x13, 0x13, 0x15, 0x15, 0x17, 0x17, 0x19, 0x19, 0x11, 0x11, 0x15, 0x15 };

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

	public int getStart_score() {
		return start_score;
	}
}
