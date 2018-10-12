package com.cai.game.mj.handler.hunanchangde;

import java.util.ArrayList;
import java.util.List;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.define.ECardType;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.GameDescUtil;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJGameLogic.AnalyseItem;
import com.cai.game.mj.MJType;
import com.cai.service.MongoDBServiceImpl;

import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;

public class MJTable_HuNan_ChangDe extends AbstractMJTable {

	private static final long serialVersionUID = 3596966322374528600L;

	protected MJHandlerQiShouHongZhong_HuNan_ChangDe _handler_qishou_hongzhong;

	public MJTable_HuNan_ChangDe() {
		super(MJType.GAME_TYPE_HUNAN_CHANGDE);
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card,
			ChiHuRight chiHuRight, int card_type, int _seat_index) {
		if ((has_rule(GameConstants.GAME_RULE_HUNAN_ZIMOHU) && (card_type == GameConstants.HU_CARD_TYPE_PAOHU))) {
			return GameConstants.WIK_NULL;
		}

		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		// 变量定义
		int cbChiHuKind = GameConstants.WIK_NULL;

		long qxd = _logic.is_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card);

		if (qxd != GameConstants.WIK_NULL) {
			cbChiHuKind = GameConstants.WIK_CHI_HU;
			if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) { // 自摸
				chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
			} else if (card_type == GameConstants.HU_CARD_TYPE_QIANGGANG) { // 抢杠
				chiHuRight.opr_or(GameConstants.CHR_HUNAN_QIANG_GANG_HU);
			} else {
				chiHuRight.opr_or(GameConstants.CHR_SHU_FAN); // 点炮
			}
		}

		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HUNAN_HONGZHONG)) {
			if ((cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD)] == 4)
					|| ((cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD)] == 3)
							&& (cur_card == GameConstants.HZ_MAGIC_CARD))) {
				cbChiHuKind = GameConstants.WIK_CHI_HU;
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) { // 自摸
					chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
				} else if (card_type == GameConstants.HU_CARD_TYPE_QIANGGANG) { // 抢杠
					chiHuRight.opr_or(GameConstants.CHR_HUNAN_QIANG_GANG_HU);
				} else {
					chiHuRight.opr_or(GameConstants.CHR_SHU_FAN); // 点炮
				}
			}
		}

		if (!chiHuRight.is_empty()) { // 如果是上面的七小对或者红中胡牌型
			return cbChiHuKind;
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
		boolean bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weave_count, analyseItemArray, true);
		if (!bValue) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;

		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) { // 自摸
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else if (card_type == GameConstants.HU_CARD_TYPE_QIANGGANG) { // 抢杠
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_QIANG_GANG_HU);
		} else {
			chiHuRight.opr_or(GameConstants.CHR_SHU_FAN); // 点炮
		}

		return cbChiHuKind;
	}

	public boolean estimate_player_out_card_respond(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;

		// 用户状态
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		// 动作判断
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			// 碰牌判断
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
					playerStatus.add_gang(card, seat_index, 1); // 加上杠
					bAroseAction = true;
				}
			}

			// 可以胡的情况 判断
			if (_playerStatus[i].is_chi_hu_round()) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						GameConstants.HU_CARD_TYPE_PAOHU, i);

				// 结果判断
				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index); // 吃胡的组合
					bAroseAction = true;
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

	public boolean exe_qishou_hongzhong(int seat_index) {
		this.set_handler(this._handler_qishou_hongzhong);
		this._handler_qishou_hongzhong.reset_status(seat_index);
		this._handler_qishou_hongzhong.exe(this);

		return true;
	}
	
	@Override
	public String get_game_des() {
		// for (int tmpInt : _game_rule_index) {
		// char[] chs = new char[Integer.SIZE];
		// for (int i = 0; i < Integer.SIZE; i++)
		// {
		// chs[Integer.SIZE - 1 - i] = (char) (((tmpInt >> i) & 1) + '0');
		// System.out.println(i+"："+chs[Integer.SIZE - 1 - i]);
		// }
		// System.out.println(new String(chs));
		// }

		StringBuilder gameDesc = new StringBuilder("");
		boolean hasFirst = false;
		if (GameDescUtil.has_rule(this.getGameRuleIndexEx(), GameConstants.GAME_RULE_HUNAN_ZIMOHU)) {
			if (hasFirst) {
				gameDesc.append("\n自摸胡");
			} else {
				gameDesc.append("自摸胡");
				hasFirst = true;
			}
		}
		if (GameDescUtil.has_rule(this.getGameRuleIndexEx(), GameConstants.GAME_RULE_HUNAN_GANG_HU)) {
			if (hasFirst) {
				gameDesc.append("\n点炮胡");
			} else {
				gameDesc.append("点炮胡");
				hasFirst = true;
			}
		}
		if (GameDescUtil.has_rule(this.getGameRuleIndexEx(), GameConstants.GAME_RULE_HUNAN_HONGZHONG)) {
			if (hasFirst) {
				gameDesc.append("\n红中癞子");
			} else {
				gameDesc.append("红中癞子");
				hasFirst = true;
			}
		}
		if (GameDescUtil.has_rule(this.getGameRuleIndexEx(), GameConstants.GAME_RULE_HUNAN_HUANG_ZHUANG_HUANG_GANG)) {
			if (hasFirst) {
				gameDesc.append("\n荒庄荒杠");
			} else {
				gameDesc.append("荒庄荒杠");
				hasFirst = true;
			}
		}
		if (GameDescUtil.has_rule(this.getGameRuleIndexEx(), GameConstants.GAME_RULE_HUNAN_QIANG_GANG_HU_JIANG_MA)) {
			if (hasFirst) {
				gameDesc.append("\n抢杠胡奖码");
			} else {
				gameDesc.append("抢杠胡奖码");
				hasFirst = true;
			}
		}
		if (GameDescUtil.has_rule(this.getGameRuleIndexEx(), GameConstants.GAME_RULE_HUNAN_ZHANIAO2)) {
			if (hasFirst) {
				gameDesc.append("\n159奖码-奖2码");
			} else {
				gameDesc.append("159奖码-奖2码");
				hasFirst = true;
			}
		}
		if (GameDescUtil.has_rule(this.getGameRuleIndexEx(), GameConstants.GAME_RULE_HUNAN_ZHANIAO4)) {
			if (hasFirst) {
				gameDesc.append("\n159奖码-奖4码");
			} else {
				gameDesc.append("159奖码-奖4码");
				hasFirst = true;
			}
		}
		if (GameDescUtil.has_rule(this.getGameRuleIndexEx(), GameConstants.GAME_RULE_HUNAN_ZHANIAO6)) {
			if (hasFirst) {
				gameDesc.append("\n159奖码-奖6码");
			} else {
				gameDesc.append("159奖码-奖6码");
				hasFirst = true;
			}
		}
		if (GameDescUtil.has_rule(this.getGameRuleIndexEx(), GameConstants.GAME_RULE_HUNAN_MO_JI_JIANG_JI)) {
			if (hasFirst) {
				gameDesc.append("\n摸几奖几");
			} else {
				gameDesc.append("摸几奖几");
				hasFirst = true;
			}
		}
		return gameDesc.toString();
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

		// 如果有红中癞子的玩法，是不需要判断红中的
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
			// 没听牌
		} else if (count > 0 && count < max_ting_count) {
			if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HUNAN_HONGZHONG)) {
				// 有胡的牌，红中肯定能胡
				cards[count] = _logic.switch_to_card_data(this._logic.get_magic_card_index(0));
				count++;
			}
		} else {
			// 全听
			count = 1;
			cards[0] = -1;
		}

		return count;
	}
	
	@Override
	public boolean handler_requst_call_qiang_zhuang(int seat_index, int call_banker, int qiang_banker) {
		return false;
	}

	@Override
	public boolean handler_requst_nao_zhuang(Player player, int nao) {
		return false;
	}

	@Override
	public boolean handler_requst_pao_qiang(Player player, int pao, int qiang) {
		return false;
	}

	@Override
	public void init_other_param(Object... objects) {

	}

	@Override
	public void onInitTable() {
		_handler_chi_peng = new MJHandlerChiPeng_HuNan_ChangDe();
		_handler_dispath_card = new MJHandlerDispatchCard_HuNan_ChangDe();
		_handler_gang = new MJHandlerGang_HuNan_ChangDe();
		_handler_out_card_operate = new MJHandlerOutCardOperate_HuNan_ChangDe();
		_handler_qishou_hongzhong = new MJHandlerQiShouHongZhong_HuNan_ChangDe();

		if (has_rule(GameConstants.GAME_RULE_HUNAN_HONGZHONG)) { // 红中癞子
			_logic.add_magic_card_index(_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD));
		}
	}

	@Override
	public boolean on_game_start() {
		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[GameConstants.GAME_PLAYER][GameConstants.MAX_COUNT];
		// 发送数据
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		// 发送数据
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
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
			roomResponse
					.setCurrentPlayer(_current_player == GameConstants.INVALID_SEAT ? _resume_player : _current_player);
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
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}

		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(GRR._left_card_count);
		GRR.add_room_response(roomResponse);

		// 检测听牌
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			_playerStatus[i]._hu_card_count = this.get_ting_card(_playerStatus[i]._hu_cards, GRR._cards_index[i],
					GRR._weave_items[i], GRR._weave_count[i], i);
			if (_playerStatus[i]._hu_card_count > 0) {
				this.operate_chi_hu_cards(i, _playerStatus[i]._hu_card_count, _playerStatus[i]._hu_cards);
			}
		}

		boolean is_qishou_hu = false;
		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HUNAN_HONGZHONG)) {
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				// 起手4个红中
				if (GRR._cards_index[i][_logic.get_magic_card_index(0)] == 4) {

					_playerStatus[i].add_action(GameConstants.WIK_ZI_MO);
					_playerStatus[i].add_zi_mo(_logic.switch_to_card_data(_logic.get_magic_card_index(0)), i);
					GRR._chi_hu_rights[i].opr_or(GameConstants.CHR_ZI_MO);
					GRR._chi_hu_rights[i].opr_or(GameConstants.CHR_HUNAN_HZ_QISHOU_HU);

					is_qishou_hu = true;
					this.exe_qishou_hongzhong(i);

					MongoDBServiceImpl.getInstance().card_log(this.get_players()[i], ECardType.hongZhong4, "", 0, 0l,
							this.getRoom_id());
					break;
				}
			}
		}

		if (is_qishou_hu == false) {
			this.exe_dispatch_card(_current_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);
		}

		return true;
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {

	}

	// type: 0自摸胡，1抢杠胡，2点炮胡
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, int type) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;

		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int wFanShu = 1;

		if (type == 0 || type == 1) { // 自摸和抢杠都算2分
			wFanShu = 2;
		}

		countCardType(chr, seat_index);

		int lChiHuScore = wFanShu * GameConstants.CELL_SCORE;

		// 算基础分
		if (type == 0) { // 自摸--每人2分
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				if (i == seat_index)
					continue;

				GRR._lost_fan_shu[i][seat_index] = wFanShu;
			}
		} else if (type == 1) { // 抢杠胡--被抢杠者输6分，抢杠者赢6分
			GRR._lost_fan_shu[provide_index][seat_index] = wFanShu * 3;
		} else { // 点炮--放炮者输一分，接炮者赢一分
			GRR._lost_fan_shu[provide_index][seat_index] = wFanShu;
		}

		// 算奖码分 -- 杠分在开杠时就算进去了
		if (type == 0) {
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				if (i == seat_index)
					continue;

				int s = lChiHuScore;
				
				s += GRR._count_pick_niao * 2;

				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;
			}
		} else if (type == 1) {
			int s = lChiHuScore; // 抢杠2分，被抢杠者输6分，抢杠者赢6分，并把分加到游戏总分里

			s = s * 3;
			
			s += GRR._count_pick_niao * 2;
			
			GRR._game_score[provide_index] -= s;
			GRR._game_score[seat_index] += s;

			GRR._chi_hu_rights[seat_index].opr_or(GameConstants.CHR_HUNAN_QIANG_GANG_HU); // 抢杠胡
		} else {
			int s = lChiHuScore; // 点炮1分，并把分加到游戏总分里

			s += GRR._count_pick_niao * 2;
			
			GRR._game_score[provide_index] -= s;
			GRR._game_score[seat_index] += s;

			GRR._chi_hu_rights[provide_index].opr_or(GameConstants.CHR_FANG_PAO); // 放炮
		}

		GRR._provider[seat_index] = provide_index;

		_status_gang = false;
		_status_gang_hou_pao = false;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);

		return;
	}

	/**
	 * 直接利用已有的抓鸟数据，动画效果可能需要客户端从新做
	 * 
	 * @param seat_index
	 *            当前奖码的玩家，一局只有一个人进行奖码
	 * @param card
	 *            摸几奖几时摸上来的牌
	 * @param show
	 *            是否显示中码效果
	 * @param add_niao
	 *            额外的奖码值，0或1
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

		GRR._count_niao = get_niao_card_num(card, add_niao);

		if (GRR._count_niao > GameConstants.ZHANIAO_0) {
			int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
			// 从剩余牌堆里顺序取奖码数目的牌
			_logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count, GRR._count_niao,
					cbCardIndexTemp);
			_logic.switch_to_cards_data(cbCardIndexTemp, GRR._cards_data_niao);
			GRR._left_card_count -= GRR._count_niao;
		}

		for (int i = 0; i < GRR._count_niao; i++) {
			GRR._player_niao_cards[seat_index][GRR._player_niao_count[seat_index]] = GRR._cards_data_niao[i];
			GRR._player_niao_count[seat_index]++;
		}

		// 中鸟个数
		GRR._count_pick_niao = get_pick_niao_count(GRR._cards_data_niao, GRR._count_niao);
	}

	@Override
	public void set_result_describe() {
		int chrTypes;
		long type = 0;
		for (int player = 0; player < GameConstants.GAME_PLAYER; player++) {
			StringBuilder gameDesc = new StringBuilder("");

			chrTypes = GRR._chi_hu_rights[player].type_count;

			for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
				type = GRR._chi_hu_rights[player].type_list[typeIndex];

				if (GRR._chi_hu_rights[player].is_valid()) {

					if (type == GameConstants.CHR_ZI_MO) {
						gameDesc.append(" 自摸");
						
						if (GRR._count_pick_niao > 0) {
							gameDesc.append(" 中码X" + GRR._count_pick_niao);
						}
					}

					if (type == GameConstants.CHR_SHU_FAN) {
						gameDesc.append(" 接炮");
						
						if (GRR._count_pick_niao > 0) {
							gameDesc.append(" 中码X" + GRR._count_pick_niao);
						}
					}

					if (type == GameConstants.CHR_HUNAN_QIANG_GANG_HU) {
						gameDesc.append(" 抢杠胡");
						
						if (GRR._count_pick_niao > 0) {
							gameDesc.append(" 中码X" + GRR._count_pick_niao);
						}
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
	public void test_cards() {
		int cards[] = new int[] { 0x35, 0x35, 0x35, 0x35, 0x19, 0x19, 0x19, 0x26, 0x27, 0x28, 0x29, 0x29, 0x29 };

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			for (int j = 0; j < 13; j++) {
				GRR._cards_index[i][_logic.switch_to_card_index(cards[j])] += 1;
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
	 * 获取奖码数目
	 * 
	 * @param card
	 *            摸几奖几时，摸上来的牌
	 * @param add_niao
	 *            额外的奖码，0或1
	 * @return
	 */
	private int get_niao_card_num(int card, int add_niao) {
		int nNum = GameConstants.ZHANIAO_0;

		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HUNAN_MO_JI_JIANG_JI)) { // 摸几奖几
			if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HUNAN_HONGZHONG)) { // 红中癞子
				if (_logic.is_magic_card(card)) {
					nNum = GameConstants.MAX_NIAO_CARD; // 奖10码
				} else {
					nNum = _logic.get_card_value(card);
				}
			} else { // 正常的万条筒玩法
				nNum = _logic.get_card_value(card);
			}
		} else if (has_rule(GameConstants.GAME_RULE_HUNAN_ZHANIAO2)) { // 奖2码
			nNum = GameConstants.ZHANIAO_2;
		} else if (has_rule(GameConstants.GAME_RULE_HUNAN_ZHANIAO4)) { // 奖4码
			nNum = GameConstants.ZHANIAO_4;
		} else if (has_rule(GameConstants.GAME_RULE_HUNAN_ZHANIAO6)) { // 奖6码
			nNum = GameConstants.ZHANIAO_6;
		}

		nNum += add_niao;

		if (nNum > GRR._left_card_count) {
			nNum = GRR._left_card_count;
		}

		return nNum;
	}
	
	/**
	 * 获取中码个数
	 * 
	 * @param cards_data
	 *            奖码的牌
	 * @param card_num
	 *            奖码的个数
	 * @return
	 */
	private int get_pick_niao_count(int cards_data[], int card_num) {
		int cbPickNum = 0;

		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HUNAN_HONGZHONG)) { // 红中癞子玩法
			for (int i = 0; i < card_num; i++) {
				if (!_logic.is_valid_card(cards_data[i])) // 如果有非法的牌
					return 0;

				if (_logic.is_magic_card(cards_data[i])) { // 如果是红中也算中码
					cbPickNum++;
					continue;
				}

				int nValue = _logic.get_card_value(cards_data[i]);

				if (nValue == 1 || nValue == 5 || nValue == 9) {
					cbPickNum++;
				}
			}
		} else { // 普通万条筒玩法
			for (int i = 0; i < card_num; i++) {
				if (!_logic.is_valid_card(cards_data[i])) // 如果有非法的牌
					return 0;

				int nValue = _logic.get_card_value(cards_data[i]);

				if (nValue == 1 || nValue == 5 || nValue == 9) {
					cbPickNum++;
				}
			}
		}

		return cbPickNum;
	}
}
