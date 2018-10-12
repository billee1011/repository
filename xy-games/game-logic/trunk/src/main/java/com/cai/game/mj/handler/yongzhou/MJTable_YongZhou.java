package com.cai.game.mj.handler.yongzhou;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.RedisConstant;
import com.cai.common.define.ECardType;
import com.cai.common.domain.BrandLogModel;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.GameDescUtil;
import com.cai.common.util.SpringService;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJType;
import com.cai.redis.service.RedisService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;
import com.google.common.collect.Maps;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJTable_YongZhou extends AbstractMJTable {

	BrandLogModel _recordRoomRecord;
	public MJHandlerDaDian_YongZhou _handler_da_dian;
	public MJHandlerQiShouHu _handler_qishou_hun;

	public MJTable_YongZhou() {
		super(MJType.GAME_TYPE_HUNAN_YONGZHOU);
	}

	@Override
	protected void onInitTable() {
		_handler_dispath_card = new MJHandlerDispatchCard_YongZhou();
		_handler_out_card_operate = new MJHandlerOutCardOperate_YongZhou();
		_handler_gang = new MJHandlerGang_YongZhou();
		_handler_chi_peng = new MJHandlerChiPeng_YongZhou();
		_handler_da_dian = new MJHandlerDaDian_YongZhou();
		_handler_qishou_hun = new MJHandlerQiShouHu();
	}

	// 游戏开始
	@Override
	protected boolean on_handler_game_start() {
		if (_cur_round == 2) {
			// real_kou_dou();// 记录真实扣豆
		}

		reset_init_data();

		// 庄家选择
		progress_banker_select();

		_game_status = GameConstants.GS_MJ_PLAY;

		// 信阳麻将
		GRR._banker_player = _cur_banker;
		_current_player = GRR._banker_player;
		banker_count[_current_player]++;
		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HUNAN_SCORE_ADD)) { // 112张
			_repertory_card = new int[GameConstants.CARD_DATA_DAI_FENG.length];
			shuffle(_repertory_card, GameConstants.CARD_DATA_DAI_FENG);
		} else { // 108张
			_repertory_card = new int[GameConstants.CARD_DATA_WAN_TIAO_TONG.length];
			shuffle(_repertory_card, GameConstants.CARD_DATA_WAN_TIAO_TONG);
		}

		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();

		getLocationTip();

		try {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				for (int j = 0; j < GRR._cards_index[i].length; j++) {
					if (GRR._cards_index[i][j] == 4) {
						MongoDBServiceImpl.getInstance().card_log(this.get_players()[i], ECardType.anLong, "", GRR._cards_index[i][j], 0l,
								this.getRoom_id());
					}
				}
			}
		} catch (Exception e) {
			logger.error("card_log", e);
		}
		// 游戏开始时 初始化 未托管
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			istrustee[i] = false;
		}

		// 再次同步一下托管状态
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			be_in_room_trustee_match(i);
		}
		return on_game_start();
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

		// this.exe_dispatch_card(_current_player,
		// GameConstants.DispatchCard_Type_Tian_Hu, 0);
		runnable_ding_gui(GRR._banker_player);
		return true;
	}

	public void runnable_ding_gui(int seat_index) {
		this.set_handler(_handler_da_dian);
		_handler_da_dian.reset_status(seat_index);
		_handler_da_dian.exe(this);
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weaveCount, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {

		if ((has_rule(GameConstants.GAME_RULE_HUNAN_ZIMOHU) && (card_type == GameConstants.HU_CARD_TYPE_PAOHU))) {
			return GameConstants.WIK_NULL;
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

		// 清一色
		if (_logic.is_qing_yi_se(cbCardIndexTemp, weaveItems, weaveCount, cur_card)) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_QING_YI_SE);
		}

		// 十三幺
		if (_logic.isShiSanYao(cbCardIndexTemp, weaveItems, weaveCount)) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_SHI_SAN_YAO);

			// 王牌数量
			if (cbCardIndexTemp[_logic.get_magic_card_index(0)] == 4) {
				chiHuRight.opr_or(GameConstants.CHR_HUNAN_PRIVATE_SI_TIAN_WANG);
			}
			return GameConstants.WIK_CHI_HU;
		}

		// 十三烂
		if (_logic.isShiSanLan(cbCardIndexTemp, weaveItems, weaveCount)) {
			if (qiFengDaoWei(cbCardIndexTemp)) {
				chiHuRight.opr_or(GameConstants.CHR_HUNAN_QI_FENG_DAO_WEI);
			} else {
				chiHuRight.opr_or(GameConstants.CHR_HUNAN_SHI_SAN_LAN);
			}

			// 王牌数量
			if (cbCardIndexTemp[_logic.get_magic_card_index(0)] == 4) {
				chiHuRight.opr_or(GameConstants.CHR_HUNAN_PRIVATE_SI_TIAN_WANG);
			}
			return GameConstants.WIK_CHI_HU;
		}

		// 七对
		int qiXiaoDui = is_qi_xiao_dui(cards_index, weaveItems, weaveCount, cur_card, chiHuRight);
		if (qiXiaoDui != GameConstants.WIK_NULL) {
			// 王牌数量
			if (cbCardIndexTemp[_logic.get_magic_card_index(0)] == 4) {
				chiHuRight.opr_or(GameConstants.CHR_HUNAN_PRIVATE_SI_TIAN_WANG);
			}
			return GameConstants.WIK_CHI_HU;
		}

		boolean bValue = false;
		int magicCount = cbCardIndexTemp[_logic.get_magic_card_index(0)];
		if (card_type == GameConstants.HU_CARD_TYPE_PAOHU && cur_card == GameConstants.HZ_MAGIC_CARD) {
			int count = cbCardIndexTemp[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD)];
			for (int i = 0; i < count; i++) {
				cbCardIndexTemp[_logic.get_magic_card_index(0)] = i;
				cbCardIndexTemp[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD)] = count - i;
				bValue = checkWangGuiWei(cbCardIndexTemp, chiHuRight, cur_card, magicCount);
				if (bValue)
					break;
			}
		} else {
			cbCardIndexTemp[_logic.get_magic_card_index(0)] = cbCardIndexTemp[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD)];
			cbCardIndexTemp[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD)] = 0;
			bValue = checkWangGuiWei(cbCardIndexTemp, chiHuRight, cur_card, magicCount);
		}

		if (!bValue) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		// 王牌数量
		if (magicCount == 4) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_PRIVATE_SI_TIAN_WANG);
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

		int l = GameConstants.MAX_INDEX - GameConstants.CARD_HUA_COUNT;
		int ql = l - 1;
		for (int i = 0; i < l; i++) {
			if (this._logic.is_magic_index(i))
				continue;
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItems, cbWeaveCount, cbCurrentCard, chr,
					GameConstants.HU_CARD_TYPE_ZIMO, seatIndex)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}

		l -= 1;
		if (count == 0) {
		} else if (count > 0 && count < ql) {
			// 有胡的牌。红中肯定能胡
			cards[count] = _logic.switch_to_card_data(this._logic.get_magic_card_index(0)) + GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
			count++;
		} else {
			// 全听
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int wFanShu = 1;// 番数
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

		float lChiHuScore = wFanShu;

		////////////////////////////////////////////////////// 自摸 算分
		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}

				int niao = GRR._count_pick_niao;
				float s = getScore(chr, lChiHuScore);
				s += niao;
				// 胡牌分
				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;
			}
		}
		////////////////////////////////////////////////////// 点炮 算分
		else {
			float s = getScore(chr, lChiHuScore);

			int niao = GRR._count_pick_niao;
			s += niao;

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
		if (!(chr.opr_and(GameConstants.CHR_HUNAN_SHI_SAN_LAN)).is_empty()) {
			s = 2;
		}
		if (!(chr.opr_and(GameConstants.CHR_HUNAN_QI_XIAO_DUI)).is_empty()) {
			s = 2;
		}
		if (!(chr.opr_and(GameConstants.CHR_HUNAN_QI_FENG_DAO_WEI)).is_empty()) {
			s = 4;
		}
		if (!(chr.opr_and(GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI)).is_empty()) {
			s = 4;
		}
		if (!(chr.opr_and(GameConstants.CHR_HUNAN_PRIVATE_WANG_BAO)).is_empty()) {
			s = 4;
		}
		if (!(chr.opr_and(GameConstants.CHR_HUNAN_PRIVATE_WANG_CHUANG)).is_empty()) {
			s = 4;
		}
		if (!(chr.opr_and(GameConstants.CHR_HUNAN_QING_YI_SE)).is_empty()) {
			s = 4;
		}
		if (!(chr.opr_and(GameConstants.CHR_HUNAN_TIAN_HU)).is_empty()) {
			s = 4;
		}
		if (!(chr.opr_and(GameConstants.CHR_HUNAN_PRIVATE_WANG_GUI_WEI)).is_empty()) {
			s = 4;
		}
		if (!(chr.opr_and(GameConstants.CHR_HUNAN_SHI_SAN_YAO)).is_empty()) {
			s = 8;
		}
		if (!(chr.opr_and(GameConstants.CHR_HUNAN_PRIVATE_WANG_YING_QI_DUI)).is_empty()) {
			s = 8;
		}
		if (!(chr.opr_and(GameConstants.CHR_HUNAN_PRIVATE_WANG_BAO_WANG)).is_empty()) {
			s = 8;
		}
		if (!(chr.opr_and(GameConstants.CHR_HUNAN_PRIVATE_WANG_CHUANG_WANG)).is_empty()) {
			s = 8;
		}
		if (!(chr.opr_and(GameConstants.CHR_HUNAN_PRIVATE_WANG_GUI_WEI_SHAUNG)).is_empty()) {
			s = 8;
		}
		if (!(chr.opr_and(GameConstants.CHR_HUNAN_PRIVATE_SI_TIAN_WANG)).is_empty()) {
			s = 8;
		}
		if (!(chr.opr_and(GameConstants.CHR_HUNAN_PRIVATE_QI_SHOU_HU)).is_empty()) {
			s = 8;
		}
		return s;
	}

	@Override
	protected void set_result_describe() {
		int chrTypes;
		long type = 0;
		for (int player = 0; player < GameConstants.GAME_PLAYER; player++) {
			StringBuilder gameDesc = new StringBuilder("");

			GRR._chi_hu_rights[player].opr_or(GRR._chi_hu_rights[player].qi_shou_bao_ting);

			chrTypes = GRR._chi_hu_rights[player].type_count;

			boolean showNiao = true;
			for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
				type = GRR._chi_hu_rights[player].type_list[typeIndex];

				if (GRR._chi_hu_rights[player].is_valid()) {

					if (GRR._count_pick_niao > 0 && showNiao) {
						gameDesc.append(" 中" + GRR._count_pick_niao + "鸟");
						showNiao = false;
					}
					if (type == GameConstants.CHR_ZI_MO) {
						gameDesc.append(" 自摸");
					}
					if (type == GameConstants.CHR_SHU_FAN) {
						gameDesc.append(" 接炮");
					}
					if (type == GameConstants.CHR_HUNAN_TIAN_HU) {
						gameDesc.append(" 天胡");
					}
					if (type == GameConstants.CHR_HUNAN_SHI_SAN_LAN) {
						gameDesc.append(" 十三烂");
					}
					if (type == GameConstants.CHR_HUNAN_QI_XIAO_DUI) {
						gameDesc.append(" 七小对");
					}
					if (type == GameConstants.CHR_HUNAN_QI_FENG_DAO_WEI) {
						gameDesc.append(" 七风到位");
					}
					if (type == GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI) {
						gameDesc.append(" 豪华七小对");
					}
					if (type == GameConstants.CHR_HUNAN_PRIVATE_WANG_BAO) {
						gameDesc.append(" 王钓");
					}
					if (type == GameConstants.CHR_HUNAN_PRIVATE_WANG_CHUANG) {
						gameDesc.append(" 王闯");
					}
					if (type == GameConstants.CHR_HUNAN_PRIVATE_WANG_GUI_WEI) {
						gameDesc.append(" 王归位");
					}
					if (type == GameConstants.CHR_HUNAN_QING_YI_SE) {
						gameDesc.append(" 清一色");
					}
					if (type == GameConstants.CHR_HUNAN_SHI_SAN_YAO) {
						gameDesc.append(" 十三幺");
					}
					if (type == GameConstants.CHR_HUNAN_PRIVATE_WANG_YING_QI_DUI) {
						gameDesc.append(" 硬巧对");
					}
					if (type == GameConstants.CHR_HUNAN_PRIVATE_WANG_BAO_WANG) {
						gameDesc.append(" 王钓王");
					}
					if (type == GameConstants.CHR_HUNAN_PRIVATE_WANG_CHUANG_WANG) {
						gameDesc.append(" 王闯王");
					}
					if (type == GameConstants.CHR_HUNAN_PRIVATE_WANG_GUI_WEI_SHAUNG) {
						gameDesc.append(" 双王归位");
					}
					if (type == GameConstants.CHR_HUNAN_PRIVATE_SI_TIAN_WANG) {
						gameDesc.append(" 四大天王");
					}
					if (type == GameConstants.CHR_HUNAN_PRIVATE_QI_SHOU_HU) {
						gameDesc.append(" 起手胡");
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
	public boolean handler_requst_message_deal(Player player, int seat_index, RoomRequest room_rq, int type) {
		return true;
	}

	public boolean estimate_player_out_card_respond(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		if (_logic.is_magic_card(card)) {
			return false;
		}
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
			if (action != 0 && !_playerStatus[i].is_bao_ting()) {
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

		// 吃
		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HUNAN_EAT)) {
			int chi_seat_index = (seat_index + 1) % GameConstants.GAME_PLAYER;
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
	 * 抓鸟 个数
	 * 
	 * @return
	 */
	public int getCsDingNiaoNum() {
		int nNum = GameConstants.ZHANIAO_0;
		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HUNAN_ZHANIAO1)) {
			return GameConstants.ZHANIAO_1;
		} else if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HUNAN_ZHANIAO2)) {
			return GameConstants.ZHANIAO_2;
		} else if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HUNAN_ZHANIAO3)) {
			return GameConstants.ZHANIAO_3;
		} else if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HUNAN_ZHANIAO4)) {
			return GameConstants.ZHANIAO_4;
		} else if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HUNAN_ZHANIAO6)) {
			return GameConstants.ZHANIAO_6;
		}
		return nNum;
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
				_logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count, GRR._count_niao, cbCardIndexTemp);
				GRR._left_card_count -= GRR._count_niao;
				_logic.switch_to_cards_data(cbCardIndexTemp, GRR._cards_data_niao);
			} else {
				for (int i = 0; i < GRR._count_niao; i++) {
					GRR._cards_data_niao[i] = card;
				}
			}
		}
		// 中鸟个数
		GRR._count_pick_niao = _logic.get_pick_niao_count(GRR._cards_data_niao, GRR._count_niao);

		for (int i = 0; i < GRR._count_niao; i++) {
			int nValue = _logic.get_card_value(GRR._cards_data_niao[i]);
			int color = _logic.get_card_color(GRR._cards_data_niao[i]);
			int seat = 0;
			seat = (seat_index + (nValue - 1) % 4) % 4;
			if (color == 3) {
				seat = seat_index;
			}
			GRR._player_niao_cards[seat][GRR._player_niao_count[seat]] = GRR._cards_data_niao[i];
			GRR._player_niao_count[seat]++;
		}

		GRR._count_pick_niao = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GRR._player_niao_count[i]; j++) {
				if (seat_index == i) {
					GRR._count_pick_niao++;
					GRR._player_niao_cards[i][j] = this.set_ding_niao_valid(GRR._player_niao_cards[i][j], true);// 胡牌的鸟生效
				} else {
					GRR._player_niao_cards[i][j] = this.set_ding_niao_valid(GRR._player_niao_cards[i][j], false);// 胡牌的鸟生效
				}
			}
		}
	}

	/**
	 * 花色分类
	 * 
	 * @param cardDatas
	 * @return
	 */
	private Map<Integer, List<Integer>> getSameColorMap(Integer[] cardDatas) {

		Map<Integer, List<Integer>> colorCardIndex = Maps.newHashMap();

		List<Integer> singleColor = null;
		for (int i = 0; i < cardDatas.length; i++) {
			if (cardDatas[i] == null)
				continue;
			int card = cardDatas[i];
			Integer color = _logic.get_card_color(card);
			singleColor = colorCardIndex.get(color);
			if (singleColor == null) {
				singleColor = new ArrayList<>();
				colorCardIndex.put(color, singleColor);
			}
			singleColor.add(_logic.get_card_value(card));
		}

		return colorCardIndex;
	}

	/**
	 * 分析牌
	 * 
	 * @param cardDatas
	 * @param color
	 * @return
	 */
	public static Tuple<Integer, Integer> ruleByFuzi(Integer[] cardDatas, int color) {
		int length = cardDatas.length;
		int need = 0;
		int used[] = new int[length];

		Tuple<Integer, Integer> tuple = new Tuple<Integer, Integer>(0, 0);

		for (int i = 0; i < length; i++) {

			int card = cardDatas[i];
			if (used[i] == 1) {
				continue;
			} else {
				int needTemp = 2;
				boolean straight = true;

				int m = -1;
				int n = -1;
				for (int j = i + 1; j < length; j++) {
					int nextCard = cardDatas[j];
					if (used[j] == 1) {
						continue;
					}

					if (j + 1 < length && used[j + 1] == 0) {
						if (nextCard == cardDatas[j + 1]) {
							continue;
						}
					}

					if (((card + 1 == nextCard || card + 2 == nextCard) && color != 3)) {
						needTemp = 1;
						m = i;
						n = j;
						break;
					}
					if (card == nextCard) {
						straight = false;
						needTemp = 1;
						m = i;
						n = j;
						break;
					}
				}

				if (i + 2 < length && card == cardDatas[i + 1] && card == cardDatas[i + 2] && used[i + 1] == 0 && used[i + 2] == 0) {
					needTemp = 0;
					used[i] = used[i + 1] = used[i + 2] = 1;
					m = -1;
					n = -1;
				} else if (color != 3) {
					int a = -1;
					for (int j = i + 1; j < length; j++) {
						int nextcard = cardDatas[j];
						if (card + 1 == nextcard && used[j] == 0) {
							a = j;
						}
						if (card + 2 == nextcard && used[j] == 0 && a != -1) {
							needTemp = 0;
							used[i] = used[j] = used[a] = 1;
							m = -1;
							n = -1;
							break;
						}
					}
				}
				if (m != -1 && n != -1) {
					used[m] = used[n] = 1;
				}
				if (straight) {
					tuple.setLeft(tuple.getLeft() + needTemp);
				}
				need = need + needTemp;
			}
		}

		tuple.setRight(need);
		return tuple;
	}

	/**
	 * 将牌分析
	 * 
	 * @param cardDatas
	 * @param magicCount
	 * @param color
	 * @param chiHuRight
	 * @param curCard
	 * @param t
	 * @return
	 */
	private boolean eyeRuleByFuzi(Integer[] cardDatas, int magicCount, int color, ChiHuRight chiHuRight, int curCard, Tuple<Integer, Integer> t,
			int handMagicCount) {
		int length = cardDatas.length;
		// 赖子为0，并且当前花色的个数小于2.肯定不能为将的
		if (magicCount <= 0 && length < 2) {
			return false;
		}

		for (int i = 0; i < length; i++) {
			int card = cardDatas[i];
			if (i + 1 >= length) {
				continue;
			}
			int nextCard = cardDatas[i + 1];
			if (card != nextCard) {
				continue;
			}

			Integer[] newCardDatas = getCardArrExceptArgs(cardDatas, card, nextCard);
			newCardDatas = getCardArrExceptArgs(cardDatas, card, nextCard);
			Tuple<Integer, Integer> tuple = ruleByFuzi(newCardDatas, color);

			if (magicCount < tuple.getRight()) {
				continue;
			}

			if (handMagicCount >= 2) {
				if (_logic.is_magic_card(curCard)) {
					chiHuRight.opr_or(GameConstants.CHR_HUNAN_PRIVATE_WANG_CHUANG_WANG);
				} else {
					chiHuRight.opr_or(GameConstants.CHR_HUNAN_PRIVATE_WANG_CHUANG);
				}
			}
			t.setRight(t.getRight() + tuple.getRight());
			t.setLeft(t.getLeft() + tuple.getLeft());
			return true;
		}

		for (int i = 0; i < length; i++) {
			int card = cardDatas[i];
			if (magicCount > 0) {
				Integer[] newCardDatas = getCardArrExceptArgs(cardDatas, card);
				Tuple<Integer, Integer> tuple = ruleByFuzi(newCardDatas, color);
				tuple.setRight(tuple.getRight() + 1);

				if (magicCount < tuple.getRight()) {
					continue;
				}

				t.setRight(t.getRight() + tuple.getRight());
				t.setLeft(t.getLeft() + tuple.getLeft());
				return true;
			}
		}
		return false;
	}

	/**
	 * 从牌组中移除相应的牌
	 * 
	 * @param cardDatas
	 * @param args
	 * @return
	 */
	public Integer[] getCardArrExceptArgs(Integer[] cardDatas, Integer... args) {

		List<Integer> list = new ArrayList<>();

		Integer[] temp = new Integer[cardDatas.length];
		for (int i = 0; i < cardDatas.length; i++) {
			temp[i] = cardDatas[i];
		}

		for (int except : args) {
			for (int i = 0; i < temp.length; i++) {
				if (temp[i] == except) {
					temp[i] = 0;
					break;
				}
			}
		}

		for (int card : temp) {
			if (card != 0) {
				list.add(card);
			}
		}
		return list.toArray(new Integer[list.size()]);
	}

	public int is_qi_xiao_dui(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int cur_card, ChiHuRight chiHuRight) {
		// 组合判断
		if (cbWeaveCount != 0)
			return GameConstants.WIK_NULL;

		// 单牌数目
		int cbReplaceCount = 0;
		int nGenCount = 0;

		// 临时数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入数据
		int cbCurrentIndex = _logic.switch_to_card_index(cur_card);
		cbCardIndexTemp[cbCurrentIndex]++;

		// 计算单牌
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];

			if (_logic.get_magic_card_count() > 0) {
				for (int m = 0; m < _logic.get_magic_card_count(); m++) {
					// 王牌过滤
					if (i == _logic.get_magic_card_index(m))
						continue;

					// 单牌统计
					if (cbCardCount == 1 || cbCardCount == 3)
						cbReplaceCount++;

					if (cbCardCount == 4) {
						nGenCount++;
					}
				}
			} else {
				// 单牌统计
				if (cbCardCount == 1 || cbCardCount == 3)
					cbReplaceCount++;

				if (cbCardCount == 4) {
					nGenCount++;
				}
			}
		}

		// 王牌不够
		if (_logic.get_magic_card_count() > 0) {
			int count = 0;
			for (int m = 0; m < _logic.get_magic_card_count(); m++) {
				count += cbCardIndexTemp[_logic.get_magic_card_index(m)];
			}

			if (cbReplaceCount > count) {
				return GameConstants.WIK_NULL;
			}
		} else {
			if (cbReplaceCount > 0)
				return GameConstants.WIK_NULL;
		}

		int res = 0;
		if (cbReplaceCount == 0) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_PRIVATE_WANG_YING_QI_DUI);
		}
		if (nGenCount > 0) {
			if (nGenCount == 2) {
				// 双豪华七小对
				chiHuRight.opr_or(GameConstants.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI);
			}
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI);
		} else {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_QI_XIAO_DUI);
		}

		return GameConstants.WIK_CHI_HU;
	}

	private boolean qiFengDaoWei(int cbCardIndexTemp[]) {
		for (int i = GameConstants.MAX_ZI; i < GameConstants.MAX_ZI_FENG; i++) {
			if (cbCardIndexTemp[i] != 1) {
				return false;
			}
		}
		return true;
	}

	private boolean checkWangGuiWei(int[] cbCardIndexTemp, ChiHuRight chiHuRight, int curCard, int magicCount) {

		// 除去王牌的手牌
		Integer[] cardDatas = new Integer[GameConstants.MAX_COUNT];
		int cbPosition = 0;
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (cbCardIndexTemp[i] > 0) {
				for (int j = 0; j < cbCardIndexTemp[i]; j++) {
					cardDatas[cbPosition++] = _logic.switch_to_card_data(i);
				}
			}
		}

		Map<Integer, List<Integer>> sameColorMap = getSameColorMap(cardDatas);
		if (sameColorMap.isEmpty() && magicCount == 2) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_PRIVATE_WANG_CHUANG_WANG);
			return true;
		}
		for (Integer color : sameColorMap.keySet()) {
			int haveMagicCount = magicCount;
			Tuple<Integer, Integer> tuple = getRuleAllHzCount(sameColorMap, color);
			if (haveMagicCount >= tuple.getRight()) {
				haveMagicCount -= tuple.getRight();

				boolean flag = eyeRuleByFuzi(sameColorMap.get(color).toArray(new Integer[sameColorMap.get(color).size()]), haveMagicCount, color,
						chiHuRight, curCard, tuple, magicCount);
				if (flag) {
					if (tuple.getLeft() > 1) {
						chiHuRight.opr_or(GameConstants.CHR_HUNAN_PRIVATE_WANG_GUI_WEI_SHAUNG);
					} else if (tuple.getLeft() == 1) {
						chiHuRight.opr_or(GameConstants.CHR_HUNAN_PRIVATE_WANG_GUI_WEI);
					}
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 除了jiang的花色，其他花色的牌成为副子需要红中赖子的总数
	 *
	 * @param map
	 * @param jiang
	 * @return
	 */
	private Tuple<Integer, Integer> getRuleAllHzCount(Map<Integer, List<Integer>> map, int jiang) {
		Tuple<Integer, Integer> tuple = new Tuple<>(0, 0);
		for (Integer color : map.keySet()) {
			if (color == jiang) {
				continue;
			}
			List<Integer> list = map.get(color);
			Integer[] ceArr = list.toArray(new Integer[list.size()]);

			Tuple<Integer, Integer> temp = ruleByFuzi(ceArr, color);
			tuple.setLeft(tuple.getLeft() + temp.getLeft());
			tuple.setRight(tuple.getRight() + temp.getRight());
		}

		return tuple;
	}

	public Tuple<Integer, List<Integer>> switch_to_cards_data(int seatIndex) {
		// 刷新手牌
		int cards[] = new int[GameConstants.MAX_COUNT];

		int hongZhongCount = GRR._cards_index[seatIndex][_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD)];
		if (hongZhongCount > 0) {
			GRR._cards_index[seatIndex][_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD)] = 0;
		}
		// 刷新自己手牌
		int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seatIndex], cards);

		// 癞子
		for (int j = 0; j < hand_card_count; j++) {
			if (_logic.is_magic_card(cards[j])) {
				cards[j] += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
			}
		}
		GRR._cards_index[seatIndex][_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD)] = hongZhongCount;

		int dataLenght = 0;
		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			if (cards[i] != 0) {
				dataLenght++;
			}
		}
		int newCards[] = new int[dataLenght];
		System.arraycopy(cards, 0, newCards, 0, dataLenght);

		for (int i = 0; i < hongZhongCount; i++) {
			int magicCard = _logic.switch_to_card_data(_logic.get_magic_card_index(0));
			int index = -1;
			if (newCards.length == 0) {
				index = 0;
			} else {
				index = _logic.findInsertIndex(newCards, magicCard, _logic.queryMinIndex(newCards), newCards.length); // 找到插入位置
			}

			int[] temp = new int[dataLenght + i + 1];
			temp[index] = GameConstants.HZ_MAGIC_CARD;
			System.arraycopy(newCards, 0, temp, 0, index);
			System.arraycopy(newCards, index, temp, index + 1, temp.length - index - 1);

			newCards = new int[dataLenght + i + 1];
			System.arraycopy(temp, 0, newCards, 0, dataLenght + i + 1);
		}
		if (hongZhongCount > 0) {
			for (int i = 0; i < newCards.length; i++) {
				cards[i] = newCards[i];
			}
			hand_card_count += hongZhongCount;
		}

		List<Integer> list = new ArrayList<>();
		for (int card : cards) {
			list.add(card);
		}
		return new Tuple<Integer, List<Integer>>(hand_card_count, list);
	}

	/**
	 * 处理牌桌结束逻辑
	 * 
	 * @param seat_index
	 * @param reason
	 */
	@Override
	protected boolean on_handler_game_finish(int seat_index, int reason) {
		int real_reason = reason;

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			_player_ready[i] = 0;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();

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
		if (GRR != null) {// reason == MJGameConstants.Game_End_NORMAL || reason
							// == MJGameConstants.Game_End_DRAW ||
			// (reason ==MJGameConstants.Game_End_RELEASE_PLAY && GRR!=null)
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

			if (this.is_mj_type(GameConstants.GAME_TYPE_HU_NAN_CHANG_DE)) {
				if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HUNAN_HUANG_ZHUANG_HUANG_GANG)
						&& (reason == GameConstants.Game_End_DRAW || reason == GameConstants.Game_End_RELEASE_PLAY)) { // 流局并且荒庄荒杠
					for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
						for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
							_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
						}
					}
				} else {
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
				}
			} else {
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
			long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

			// 设置胡牌描述
			this.set_result_describe();

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				Tuple<Integer, List<Integer>> t = this.switch_to_cards_data(i);
				int hand_card_count = t.getLeft();
				Integer[] temp = t.getRight().toArray(new Integer[t.getRight().size()]);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < hand_card_count; j++) {
					cs.addItem(temp[j]);
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
			real_reason = GameConstants.Game_End_DRAW;// 刘局
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(this.process_player_result(reason));
		}
		game_end.setEndType(real_reason);

		////////////////////////////////////////////////////////////////////// 得分总的
		roomResponse.setGameEnd(game_end);

		this.send_response_to_room(roomResponse);

		record_game_round(game_end);

		// 结束后刷新玩家
		// RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
		// roomResponse2.setGameStatus(_game_status);
		// roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		// this.load_player_info_data(roomResponse2);
		// this.send_response_to_room(roomResponse2);

		// 超时解散
		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT || reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
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

	/**
	 * 取出实际牌数据
	 * 
	 * @param card
	 * @return
	 */
	@Override
	public int get_real_card(int card) {
		// 错误断言
		if (card > GameConstants.CARD_ESPECIAL_TYPE_WANG_BA) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
		}
		return card;
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean exe_qishou_hun(int _banker) {
		this.set_handler(this._handler_qishou_hun);
		this._handler_qishou_hun.reset_status(_banker);
		this._handler_qishou_hun.exe(this);
		return true;
	}

	/**
	 * 杠牌分析 (分析手中的牌是否有杆(暗杆 加杆))
	 * 
	 * @param cards_index
	 * @param WeaveItem
	 * @param cbWeaveCount
	 * @param gangCardResult
	 * @param check_weave
	 *            --是否需要检查碰的牌（加杆）
	 * @return
	 */
	public int analyse_gang_card_all(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount, GangCardResult gangCardResult, boolean check_weave) {
		// 设置变量
		int cbActionMask = GameConstants.WIK_NULL;
		int markColor = -1;
		// 手上杠牌
		for (int i = 0; i < GameConstants.MAX_INDEX - GameConstants.CARD_HUA_COUNT; i++) {
			if (_logic.is_magic_index(i))
				continue;
			if (cards_index[i] == 4 && (markColor == -1 || _logic.get_card_color(_logic.switch_to_card_data(i)) != markColor)) {
				cbActionMask |= GameConstants.WIK_GANG;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = _logic.switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;// 安刚
				gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
			}
		}

		if (check_weave == true) {
			// 组合杠牌
			for (int i = 0; i < cbWeaveCount; i++) {
				if (WeaveItem[i].weave_kind == GameConstants.WIK_PENG) {
					if (cards_index[_logic.switch_to_card_index(WeaveItem[i].center_card)] == 1) {
						cbActionMask |= GameConstants.WIK_GANG;

						int index = gangCardResult.cbCardCount++;
						gangCardResult.cbCardData[index] = WeaveItem[i].center_card;
						gangCardResult.isPublic[index] = 1;// 明刚
						gangCardResult.type[index] = GameConstants.GANG_TYPE_ADD_GANG;
					}
				}
			}

		}

		return cbActionMask;
	}

	@Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x01, 0x02, 0x03, 0x11, 0x11, 0x11, 0x19, 0x19, 0x19, 0x23, 0x23, 0x23, 0x24 };
		int[] cards_of_player1 = new int[] { 0x01, 0x02, 0x03, 0x11, 0x11, 0x11, 0x19, 0x19, 0x19, 0x23, 0x23, 0x23, 0x24 };
		int[] cards_of_player3 = new int[] { 0x01, 0x02, 0x03, 0x11, 0x11, 0x11, 0x19, 0x19, 0x19, 0x23, 0x23, 0x23, 0x24 };
		int[] cards_of_player2 = new int[] { 0x01, 0x02, 0x03, 0x11, 0x11, 0x11, 0x19, 0x19, 0x19, 0x23, 0x23, 0x23, 0x24 };

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
}
