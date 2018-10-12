package com.cai.game.mj.handler.henanpy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.RedisConstant;
import com.cai.common.define.ECardType;
import com.cai.common.domain.BrandLogModel;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.GameDescUtil;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.SpringService;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.DispatchLastCardRunnable;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJGameLogic.AnalyseItem;
import com.cai.game.mj.MJType;
import com.cai.redis.service.RedisService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJTable_PY extends AbstractMJTable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2698317717887322025L;

	BrandLogModel _recordRoomRecord;
	public int gang_shang_pao_score[];
	public int _difen;

	public MJTable_PY() {
		super(MJType.GAME_TYPE_HENANPY);
	}

	@Override
	protected void onInitTable() {
		_handler_dispath_card = new MJHandlerDispatchCard_HeNan_py();
		_handler_out_card_operate = new MJHandlerOutCardOperate_HeNan_py();
		_handler_gang = new MJHandlerGang_HeNan_py();
		_handler_chi_peng = new MJHandlerChiPeng_HeNan_py();
		_handler_out_card_bao_ting = new MJHandlerOutCardBaoTing_py(); // 报听
		gang_shang_pao_score = new int[this.getTablePlayerNumber()];
		_difen = 0;
		Arrays.fill(gang_shang_pao_score, 0);
	}

	@Override
	public boolean handler_player_ready(int seat_index, boolean is_cancel) {

		if (this.get_players()[seat_index] == null) {
			return false;
		}
		if (GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) {
			return false;
		}
		if (is_cancel) {
			_player_ready[seat_index] = 0;
		} else {
			_player_ready[seat_index] = 1;
		}

		boolean nt = true;
		if (this.get_players()[seat_index].getAccount_id() == this.getRoom_owner_account_id()) {
			nt = false;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PLAYER_READY);
		roomResponse.setOperatePlayer(seat_index);
		roomResponse.setIsCancelReady(is_cancel);
		send_response_to_room(roomResponse);

		// 跑分
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_result.pao[i] = 0;// 清掉 默认是-1
		}
		// 闹庄
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_result.nao[i] = 0;
		}

		if (this._cur_round > 0) {
			// 结束后刷新玩家
			RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
			roomResponse2.setGameStatus(_game_status);
			roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
			this.load_player_info_data(roomResponse2);
			this.send_response_to_player(seat_index, roomResponse2);
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (this.get_players()[i] == null) {
				_player_ready[i] = 0;
			}

			if (_player_ready[i] == 0) {
				this.refresh_room_redis_data(GameConstants.PROXY_ROOM_PLAYER, nt);
				return false;
			}
		}

		if (this.has_rule(GameConstants.GAME_RULE_PUYANG_DI_FEN_ONE)) {
			this._difen = 1;
		} else if (this.has_rule(GameConstants.GAME_RULE_PUYANG_DI_FEN_TWO)) {
			this._difen = 2;
		} else if (this.has_rule(GameConstants.GAME_RULE_PUYANG_DI_FEN_THREE)) {
			this._difen = 5;
		} else if (this.has_rule(GameConstants.GAME_RULE_PUYANG_DI_FEN_FOUR)) {
			this._difen = 10;
		}

		handler_game_start();

		this.refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, nt);

		return true;
	}

	@Override
	public void progress_banker_select() {
		if (_cur_banker == GameConstants.INVALID_SEAT) {
			_cur_banker = 0;// 创建者的玩家为专家

			// Random random = new Random();//
			// int rand = random.nextInt(6) + 1 + random.nextInt(6) + 1;
			// _banker_select = rand % MJGameConstants.GAME_PLAYER;//
			// ((lSiceCount>>24)+(lSiceCount>>16)-1)%MJGameConstants.GAME_PLAYER;

			_shang_zhuang_player = GameConstants.INVALID_SEAT;
			_lian_zhuang_player = GameConstants.INVALID_SEAT;
		}

		if (is_sys()) {// 金币场 随机庄家
			Random random = new Random();//
			int rand = random.nextInt(6) + 1 + random.nextInt(6) + 1;
			_cur_banker = rand % getTablePlayerNumber();//

			_shang_zhuang_player = GameConstants.INVALID_SEAT;
			_lian_zhuang_player = GameConstants.INVALID_SEAT;
		}
	}

	// 游戏开始
	@Override
	protected boolean on_handler_game_start() {
		if (_cur_round == 2) {
			// real_kou_dou();// 记录真实扣豆
		}

		reset_init_data();

		// 庄家选择
		this.progress_banker_select();

		_game_status = GameConstants.GS_MJ_PLAY;

		// 信阳麻将
		GRR._banker_player = _cur_banker;
		_current_player = GRR._banker_player;

		if (has_rule(GameConstants.GAME_RULE_PUYANG_DAIFENG)) { // 112张
			_repertory_card = new int[GameConstants.CARD_COUNT_DAI_FENG_PY];
			shuffle(_repertory_card, GameConstants.CARD_DATA_DAI_FENG_PY);
		} else { // 108张
			_repertory_card = new int[GameConstants.CARD_COUNT_BU_DAI_FENG_PY];
			shuffle(_repertory_card, GameConstants.CARD_DATA_BU_DAI_FENG_PY);
		}

		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();

		_gang_mo_posion = 0;
		for (int i = 0; i < 2; i++) {
			_gang_mo_cards[i] = GameConstants.INVALID_CARD;
		}

		getLocationTip();

		try {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				for (int j = 0; j < GRR._cards_index[i].length; j++) {
					if (GRR._cards_index[i][j] == 4) {
						MongoDBServiceImpl.getInstance().card_log(this.get_players()[i], ECardType.anLong, "", GRR._cards_index[i][j], 0l,
								this.getRoom_id());
					}
				}
				_biaoyan_count[i] = 0;
			}
		} catch (Exception e) {

		}
		// 游戏开始时 初始化 未托管
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			istrustee[i] = false;
		}

		return on_game_start();
	}

	public int getTablePlayerNumber() {
		if (this.has_rule(GameConstants.GAME_RULE_PUYANG_SANREN)) {
			return GameConstants.GAME_PLAYER - 1;
		}
		return GameConstants.GAME_PLAYER;
	}

	/// 洗牌
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

		int send_count;
		int have_send_count = 0;

		int count = getTablePlayerNumber();
		// 分发扑克
		for (int i = 0; i < count; i++) {
			// if(GRR._banker_player == i){
			// send_count = MJGameConstants.MAX_COUNT;
			// }else{
			//
			// send_count = (MJGameConstants.MAX_COUNT - 1);
			// }
			send_count = (GameConstants.MAX_COUNT - 1);
			GRR._left_card_count -= send_count;

			// 一人13张牌,庄家多一张
			_logic.switch_to_cards_index(repertory_card, have_send_count, send_count, GRR._cards_index[i]);

			have_send_count += send_count;

		}
		// 记录初始牌型
		if (_recordRoomRecord != null) {
			_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));
		}
	}

	public void rand_tuozi(int seat_index) {
		int num1 = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % 6) + 1;

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_EFFECT_ACTION);
		roomResponse.setEffectType(GameConstants.Effect_Action_SHAI_ZI);
		roomResponse.setEffectCount(1);
		roomResponse.addEffectsIndex(num1);
		roomResponse.setEffectTime(1500);// anim time//摇骰子动画的时间
		roomResponse.setStandTime(500); // delay time//停留时间
		this.send_response_to_room(roomResponse);
		GRR.add_room_response(roomResponse);
		if (num1 * 2 > GRR._left_card_count) {
			int cbGangIndex = --GRR._gang_score[seat_index].gang_count;
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				GRR._gang_score[seat_index].scores[cbGangIndex][i] = 0;// 烧杠
			}
			for (int w = GRR._weave_count[_out_card_player] - 1; w >= 0; w--) {
				if (GRR._weave_items[_out_card_player][w].weave_kind != GameConstants.WIK_GANG || !GRR._weave_items[_out_card_player][w].is_vavild) {
					continue;
				}
				GRR._weave_items[_out_card_player][w].is_vavild = false;
			}
			// 从后面发一张牌给玩家
			seat_index = (seat_index + 1) % this.getTablePlayerNumber();
			exe_dispatch_card(seat_index, GameConstants.WIK_NULL, 2000);
		} else {
			exe_dispatch_card(seat_index, GameConstants.HU_CARD_TYPE_GANG_DI, 2000);
		}
	}

	protected void test_cards() {
		int cards[] = new int[] { 0x01, 0x01, 0x01, 0x19, 0x29, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x12 };

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

		/*************** 如果要测试线上实际牌 把下面代码取消注释 放入实际牌型即可 ***********************/

		/********
		 * 下面这个给测试用的 如果开发用 把上面的注释去掉 realyCards
		 **************************/

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
	protected boolean on_game_start() {
		// 游戏开始
		this._game_status = GameConstants.GS_MJ_PLAY;// 设置状态
		_logic.clean_magic_cards();

		this.GRR._banker_player = this._current_player = this._cur_banker;
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
			if (has_rule(GameConstants.GAME_RULE_PUYANG_YAOJIUPU)) {
				for (int j = 0; j < hand_card_count; j++) {
					if (_logic.switch_to_card_index(hand_cards[i][j]) % 9 == 0 && _logic.switch_to_card_index(hand_cards[i][j]) < 27) {
						hand_cards[i][j] += GameConstants.CARD_ESPECIAL_TYPE_YAOJIU;
					} else if (_logic.switch_to_card_index(hand_cards[i][j]) % 9 == 8 && _logic.switch_to_card_index(hand_cards[i][j]) < 27) {
						hand_cards[i][j] += GameConstants.CARD_ESPECIAL_TYPE_YAOJIU;
					}
				}
			}

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
		for (int i = 0; i < 2; i++) {
			gameStartResponse.addOtherCards(_repertory_card[_gang_mo_posion]);
			_gang_mo_cards[i] = _repertory_card[_gang_mo_posion++];
		}

		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(this.GRR._left_card_count);
		this.GRR.add_room_response(roomResponse);

		// 检测听牌
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			this._playerStatus[i]._hu_card_count = this.get_ting_card(this._playerStatus[i]._hu_cards, this.GRR._cards_index[i],
					this.GRR._weave_items[i], this.GRR._weave_count[i]);
			if (this._playerStatus[i]._hu_card_count > 0) {
				if (_playerStatus[i].is_bao_ting() || has_rule(GameConstants.GAME_RULE_PUYANG_BUBAOTING)) {
					{
						this.operate_chi_hu_cards(i, this._playerStatus[i]._hu_card_count, this._playerStatus[i]._hu_cards);
					}
				}
			}
		}

		this.exe_dispatch_card(_current_player, GameConstants.WIK_NULL, 0);

		return true;

	}

	/***
	 * 濮阳麻将胡牌解析
	 * 
	 * @param cards_index
	 * @param weaveItems
	 * @param weaveCount
	 * @param cur_card
	 * @param chiHuRight
	 * @param card_type
	 * @return
	 */
	public int analyse_chi_hu_card_henan_py(int cards_index[], WeaveItem weaveItems[], int weaveCount, int cur_card, ChiHuRight chiHuRight,
			int card_type) {
		if (card_type == GameConstants.HU_CARD_TYPE_PAOHU && this.has_rule(GameConstants.GAME_RULE_PUYANG_ZIMOHU))// 如果胡牌类型是炮胡
		// 但没这个规则
		// return
		{
			return GameConstants.WIK_NULL;

		}

		// cbCurrentCard一定不为0 !!!!!!!!!
		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		// 变量定义
		int cbChiHuKind = GameConstants.WIK_NULL;
		// 构造扑克
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入扑克
		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		int igc_count = _logic.magic_count(cbCardIndexTemp);

		long qxd = _logic.is_qi_xiao_dui_henan(cbCardIndexTemp, weaveItems, weaveCount);
		if (qxd != GameConstants.WIK_NULL && has_rule(GameConstants.GAME_RULE_PUYANG_DAIPU)) {
			cbChiHuKind = GameConstants.WIK_CHI_HU;
			chiHuRight.opr_or(GameConstants.CHR_HENAN_QI_XIAO_DUI);// 都是七小对

			if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
				chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
			} else if (card_type == GameConstants.HU_CARD_TYPE_PAOHU) {
				chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
			} else if (card_type == GameConstants.HU_CARD_TYPE_QIANGGANG) {
				chiHuRight.opr_or(GameConstants.CHR_HENAN_QIANG_GANG_HU);
			} else if (card_type == GameConstants.HU_CARD_TYPE_TIAN_HU) {
				chiHuRight.opr_or(GameConstants.CHR_HENAN_TIAN_HU);
			} else if (card_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
				chiHuRight.opr_or(GameConstants.CHR_HENAN_GANG_KAI);
			}

			// 胡牌算缺门
			if (this.has_rule(GameConstants.GAME_RULE_PUYANG_QUEMENJIAFEN)) {
				int colorCount = this._logic.get_se_count(cbCardIndexTemp, weaveItems, weaveCount);
				chiHuRight.duanmen_count = 3 - colorCount;
			}
			return cbChiHuKind;
		}

		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
		// 分析扑克
		boolean bValue = _logic.analyse_card_henanpy_new(cbCardIndexTemp, weaveItems, weaveCount, analyseItemArray, chiHuRight,
				has_rule(GameConstants.GAME_RULE_PUYANG_YAOJIUPU), has_rule(GameConstants.GAME_RULE_PUYANG_FENGPU),
				has_rule(GameConstants.GAME_RULE_PUYANG_JIANGPU), has_rule(GameConstants.GAME_RULE_PUYANG_DAIFENG));
		if (!bValue) {

			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}
		// 风扑，将扑，幺九扑
		boolean can_paohu = false;
		for (int i = 0; i < analyseItemArray.size(); i++) {
			// 变量定义
			AnalyseItem pAnalyseItem = analyseItemArray.get(i);
			can_paohu = true;
			for (int j = 0; j < 4; j++) {
				if ((pAnalyseItem.cbCardData[j][0] == cur_card || pAnalyseItem.cbCardData[j][1] == cur_card
						|| pAnalyseItem.cbCardData[j][2] == cur_card) && _logic.get_card_color(cur_card) == 3) {
					if (pAnalyseItem.cbWeaveKind[j] == GameConstants.WIK_LEFT) {
						can_paohu = false;
						break;
					}
				}
				if ((pAnalyseItem.cbCardData[j][0] == cur_card) || (pAnalyseItem.cbCardData[j][1] == cur_card)
						|| (pAnalyseItem.cbCardData[j][2] == cur_card)) {
					if (pAnalyseItem.cbWeaveKind[j] == GameConstants.WIK_YAO_JIU) {
						can_paohu = false;
						break;
					}
				}
				if (j == 3) {
					can_paohu = true;
				}
			}
			if (can_paohu) {
				break;
			}
		}
		if ((card_type == GameConstants.HU_CARD_TYPE_PAOHU || card_type == GameConstants.HU_CARD_TYPE_QIANGGANG
				|| card_type == GameConstants.HU_CARD_TYPE_QIANGGANG) && !can_paohu) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		// 胡牌分析 有没有风组
		int maxHeiFeng = 0;
		int maxBaiFeng = 0;
		int maxyaojiu = 0;
		int maxFeng = 0;
		for (int i = 0; i < analyseItemArray.size(); i++) {
			// 变量定义
			AnalyseItem pAnalyseItem = analyseItemArray.get(i);
			int temphei = 0;
			int tempbai = 0;
			int tempyaojiu = 0;
			for (int j = 0; j < 4; j++) {
				if (pAnalyseItem.cbWeaveKind[j] == GameConstants.WIK_LEFT || pAnalyseItem.cbWeaveKind[j] == GameConstants.WIK_CENTER
						|| pAnalyseItem.cbWeaveKind[j] == GameConstants.WIK_RIGHT) {
					// int[] cardDate = pAnalyseItem.cbCardData[j];
					int curCard = pAnalyseItem.cbCardData[j][0];
					if (_logic.get_card_color(pAnalyseItem.cbCardData[j][0]) == 3 || _logic.get_card_color(pAnalyseItem.cbCardData[j][1]) == 3
							|| _logic.get_card_color(pAnalyseItem.cbCardData[j][2]) == 3) {
						if (_logic.switch_to_card_index(curCard) < 31) {
							temphei += 1;
						} else {
							tempbai += 1;
						}
					}

				}
				if (pAnalyseItem.cbWeaveKind[j] == GameConstants.WIK_YAO_JIU) {
					tempyaojiu++;
				}
			}
			if (tempyaojiu > maxyaojiu) {
				maxyaojiu = tempyaojiu;
			}
			if (maxFeng < (temphei + tempbai)) {
				maxFeng = temphei + tempbai;
				maxHeiFeng = temphei;
				maxBaiFeng = tempbai;
			}
		}
		chiHuRight.baifeng_count = maxBaiFeng;
		chiHuRight.heifeng_count = maxHeiFeng;
		chiHuRight.yaojiu_count = maxyaojiu;

		if (is_kazhang_henan_xy(cbCardIndexTemp, cur_card, analyseItemArray) != GameConstants.WIK_NULL
				&& has_rule(GameConstants.GAME_RULE_PUYANG_KAZHANGJIAFEN)) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_KA_ZHANG);
		}

		int card_count = 0;
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			card_count += cbCardIndexTemp[i];
		}
		if (card_count == 2 && has_rule(GameConstants.GAME_RULE_PUYANG_DANDIAOJIAFEN)) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_DAN_DIAO);
		}

		// 胡牌算缺门
		if (this.has_rule(GameConstants.GAME_RULE_PUYANG_QUEMENJIAFEN)) {
			int colorCount = this._logic.get_se_count(cbCardIndexTemp, weaveItems, weaveCount);
			chiHuRight.duanmen_count = 3 - colorCount;
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;
		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else if (card_type == GameConstants.HU_CARD_TYPE_PAOHU) {
			chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
		} else if (card_type == GameConstants.HU_CARD_TYPE_QIANGGANG) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_QIANG_GANG_HU);
		} else if (card_type == GameConstants.HU_CARD_TYPE_TIAN_HU) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_TIAN_HU);
		} else if (card_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_GANG_KAI);
		}

		return cbChiHuKind;
	}

	// 卡张
	public int is_kazhang_henan_xy(int cards_index[], int cur_card, List<AnalyseItem> analyseItemArray) {

		// 胡牌判断
		int cbCardCount = 0;

		// 胡牌分析
		if (analyseItemArray.size() > 0) {

			// 牌型分析
			// 变量定义
			AnalyseItem analyseItem = analyseItemArray.get(0);

			for (int j = 0; j < analyseItem.cbWeaveKind.length; j++) {
				if ((analyseItem.cbWeaveKind[j] & GameConstants.WIK_LEFT) != 0 && analyseItem.cbCardData[j][1] == cur_card)
					return GameConstants.CHR_HENAN_XY_JIAZI;
			}

		}
		return GameConstants.WIK_NULL;
	}

	/**
	 * 三门峡麻将获取听牌
	 * 
	 * @param cards
	 * @param cards_index
	 * @param weaveItem
	 * @param cbWeaveCount
	 * @return
	 */
	public int get_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount) {

		// 复制数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int del = 0;

		boolean isDaiFeng = has_rule(GameConstants.GAME_RULE_PUYANG_DAIFENG);
		int mj_count = GameConstants.MAX_ZI;
		if (isDaiFeng) {
			mj_count = GameConstants.MAX_ZI_FENG;
		} else {
			mj_count = GameConstants.MAX_ZI;
		}

		for (int i = 0; i < mj_count; i++) {
			// if (this._logic.is_magic_index(i))
			// continue;
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (cbCardIndexTemp[_logic.switch_to_card_index(cbCurrentCard)] == 4) {
				continue;
			}
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_henan_py(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					GameConstants.HU_CARD_TYPE_ZIMO)) {

				cards[count] = cbCurrentCard;
				if (this._logic.is_magic_index(i)) {
					if (chr.opr_and(GameConstants.CHR_HENAN_QI_XIAO_DUI).is_empty()
							|| chr.opr_and(GameConstants.CHR_HENAN_HH_QI_XIAO_DUI).is_empty()) {
						cards[count] += GameConstants.CARD_ESPECIAL_TYPE_HUN;
					}
				}
				count++;
			}
		}

		// 有胡的牌。癞子肯定能胡
		if (count > 0) {
			// if (has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN)) {
			// cards[count] =
			// _logic.switch_to_card_data(this._logic.get_magic_card_index(0))
			// + GameConstants.CARD_ESPECIAL_TYPE_HUN;
			// count++;
			// }
		} else {
			// if (has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN)) {
			// // 看看鬼牌能不能胡
			// cbCurrentCard =
			// _logic.switch_to_card_data(this._logic.get_magic_card_index(0));
			// chr.set_empty();
			// if (GameConstants.WIK_CHI_HU ==
			// analyse_chi_hu_card_henan(cbCardIndexTemp, weaveItem,
			// cbWeaveCount,
			// cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO)) {
			// cards[count] = cbCurrentCard +
			// GameConstants.CARD_ESPECIAL_TYPE_HUN;
			// count++;
			// }
			// }
		}

		int number = isDaiFeng ? 34 : 27;
		if (count >= number) {
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	/***
	 * 检查杠牌,有没有胡的 检查所有人
	 * 
	 * @param seat_index
	 * @param card
	 * @return
	 */
	public boolean estimate_gang_respond_henan(int seat_index, int card) {
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
			// 可以胡的情况 判断
			if (playerStatus.is_chi_hu_round()) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card_henan_py(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						GameConstants.HU_CARD_TYPE_QIANGGANG);

				// 结果判断
				if (action != 0) {
					chr.opr_or(GameConstants.CHR_HUNAN_QIANG_GANG_HU);// 抢杠胡
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
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

	// 濮阳麻将
	public boolean estimate_player_out_card_respond_henan_py(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		// 用户状态
		for (int i = 0; i < getTablePlayerNumber(); i++) {

			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		// int llcard = get_niao_card_num(true, 0);
		int llcard = 0;
		if (has_rule(GameConstants.GAME_RULE_PUYANG_DAIPU)) {
			int gang_total_count = 0;
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				gang_total_count += GRR._gang_score[seat_index].gang_count;
			}
			llcard = 12 + gang_total_count * 2;
		}

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;
		if (_logic.is_magic_card(card)) {
			return false;
		}
		// 动作判断
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			//// 碰牌判断
			action = _logic.check_peng(GRR._cards_index[i], card);
			if (action != 0 && has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN) && _logic.is_magic_card(card)) {// 鬼牌不能碰
				action = 0;
			}
			if (action != 0 && !_playerStatus[i].is_bao_ting()) {
				playerStatus.add_action(action);
				playerStatus.add_peng(card, seat_index);
				bAroseAction = true;
			}
			if (GRR._left_card_count > llcard) {
				// 杠牌判断
				action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
				if (action != 0 && has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN) && _logic.is_magic_card(card)) {// 鬼牌不能杠
					action = 0;
				}
				if (action != 0) {
					if (_playerStatus[i].is_bao_ting()) {
						int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
						for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
							cbCardIndexTemp[j] = GRR._cards_index[i][j];
						}
						cbCardIndexTemp[_logic.switch_to_card_index(card)] = 0;
						int ting_cards[] = _playerStatus[i]._hu_cards;
						int ting_count = get_ting_card(ting_cards, cbCardIndexTemp, GRR._weave_items[i], GRR._weave_count[i] + 1);
						if (ting_count > 0) {
							playerStatus.add_action(GameConstants.WIK_GANG);
							playerStatus.add_gang(card, seat_index, 1);// 加上杠
							bAroseAction = true;
						}
					} else {
						playerStatus.add_action(GameConstants.WIK_GANG);
						playerStatus.add_gang(card, seat_index, 1);// 加上杠
						bAroseAction = true;
					}

				}
			}
			if (this.has_rule(GameConstants.GAME_RULE_PUYANG_BAOTING) && this._playerStatus[_out_card_player].is_bao_ting() == false) {
				this._playerStatus[_out_card_player]._hu_card_count = this.get_ting_card(this._playerStatus[_out_card_player]._hu_cards,
						this.GRR._cards_index[_out_card_player], this.GRR._weave_items[_out_card_player], this.GRR._weave_count[_out_card_player]);
				int ting_count = this._playerStatus[_out_card_player]._hu_card_count;
				if (ting_count > 0) {
					this._playerStatus[_out_card_player].add_action(GameConstants.WIK_BAO_TING);
					bAroseAction = true;
				}
			}

			if (has_rule(GameConstants.GAME_RULE_PUYANG_BAOTING)) {
				if (_playerStatus[i]._hu_card_count > 0 && _playerStatus[i].is_bao_ting() == true) {
					// 可以胡的情况 判断
					if (_playerStatus[i].is_chi_hu_round()) {
						// 吃胡判断
						ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
						chr.set_empty();
						int cbWeaveCount = GRR._weave_count[i];
						action = analyse_chi_hu_card_henan_py(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
								GameConstants.HU_CARD_TYPE_PAOHU);

						// 结果判断
						if (action != 0) {
							_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
							_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
							bAroseAction = true;
						}
					}
				}
			} else {
				// 可以胡的情况 判断
				if (_playerStatus[i].is_chi_hu_round()) {
					// 吃胡判断
					ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
					chr.set_empty();
					int cbWeaveCount = GRR._weave_count[i];
					action = analyse_chi_hu_card_henan_py(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
							GameConstants.HU_CARD_TYPE_PAOHU);

					// 结果判断
					if (action != 0) {
						_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
						_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
						bAroseAction = true;
					}
				}
			}

			// }
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

	protected boolean on_handler_game_finish(int seat_index, int reason) {
		// 荒庄不荒杠结束时算杠分
		// 杠牌，每个人的分数
		if (GRR != null) {
			if (!has_rule(GameConstants.GAME_RULE_PUYANG_HUANGZHUANGHUANGGANG)) {
				float lGangScore[] = new float[getTablePlayerNumber()];
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
						for (int k = 0; k < getTablePlayerNumber(); k++) {
							lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
						}
					}
					lGangScore[i] += gang_shang_pao_score[i];
				}

				//
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					GRR._game_score[i] += lGangScore[i];
					_player_result.game_score[i] += GRR._game_score[i];
				}
			} else if (reason == GameConstants.Game_End_NORMAL) {
				float lGangScore[] = new float[getTablePlayerNumber()];
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
						for (int k = 0; k < getTablePlayerNumber(); k++) {
							lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
						}
					}
					lGangScore[i] += gang_shang_pao_score[i];
				}

				//
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					GRR._game_score[i] += lGangScore[i];
					_player_result.game_score[i] += GRR._game_score[i];
				}
			}
		}

		int real_reason = reason;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
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
		game_end.setRoomOverType(0);
		game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间
		game_end.setGamePlayerNumber(getTablePlayerNumber());
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

			this.load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);

			game_end.setCellScore(_difen);

			game_end.setBankerPlayer(GRR._banker_player);// 专家
			game_end.setLeftCardCount(GRR._left_card_count);// 剩余牌
			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

			// 设置中鸟数据
			for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i < GRR._count_niao; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao[i]);
			}
			game_end.setCountPickNiao(GRR._count_pick_niao);// 中鸟个数

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					pnc.addItem(GRR._player_niao_cards[i][j]);
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
				GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {

					cs.addItem(GRR._cards_data[i][j]);
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
				game_end.addGangScore(0);// 杠牌得分
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
			for (int j = 0; j < getTablePlayerNumber(); j++) {
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
	 * 濮阳麻将算分
	 * 
	 * @param seat_index
	 * @param provide_index
	 * @param operate_card
	 * @param zimo
	 */
	public void process_chi_hu_player_score_py(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		// 胡的人是庄家
		boolean zhuang_hu = (GRR._banker_player == seat_index ? true : false);
		// 是否是庄家放胡
		boolean zhuang_fang_hu = (GRR._banker_player == provide_index ? true : false);
		// 庄家不翻倍
		boolean jiabei = GameDescUtil.has_rule(this.getGameRuleIndexEx(), GameConstants.GAME_RULE_HENAN_SMX_ZHUANG_NO_DOUBLE);

		////////////////////////////////////////////////////// 自摸 算分
		if (zimo) {
			float lChiHuScore = 2;
			lChiHuScore += chr.yaojiu_count;
			lChiHuScore += chr.baifeng_count;
			lChiHuScore += chr.heifeng_count;
			lChiHuScore += chr.duanmen_count;
			if (!(chr.opr_and(GameConstants.CHR_HENAN_KA_ZHANG)).is_empty()) {
				lChiHuScore++;
			}
			if (!(chr.opr_and(GameConstants.CHR_HENAN_DAN_DIAO)).is_empty()) {
				lChiHuScore++;
			}

			if (!(chr.opr_and(GameConstants.CHR_HENAN_QI_XIAO_DUI)).is_empty() && has_rule(GameConstants.GAME_RULE_PUYANG_QIDUIDOUBLE)) {
				lChiHuScore *= 2;
			}
			lChiHuScore *= _difen;
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}

				// 胡牌分
				GRR._game_score[i] -= lChiHuScore;
				GRR._game_score[seat_index] += lChiHuScore;

			}
		}
		////////////////////////////////////////////////////// 点炮 算分
		else {
			float lChiHuScore = 1;
			lChiHuScore += chr.yaojiu_count;
			lChiHuScore += chr.baifeng_count;
			lChiHuScore += chr.heifeng_count;
			lChiHuScore += chr.duanmen_count;
			if (!(chr.opr_and(GameConstants.CHR_HENAN_KA_ZHANG)).is_empty()) {
				lChiHuScore++;
			}
			if (!(chr.opr_and(GameConstants.CHR_HENAN_DAN_DIAO)).is_empty()) {
				lChiHuScore++;
			}
			if (!(chr.opr_and(GameConstants.CHR_HENAN_QI_XIAO_DUI)).is_empty() && has_rule(GameConstants.GAME_RULE_PUYANG_QIDUIDOUBLE)) {
				lChiHuScore *= 2;
			}
			lChiHuScore *= _difen;
			// 跑和呛
			GRR._game_score[provide_index] -= lChiHuScore;
			GRR._game_score[seat_index] += lChiHuScore;

			GRR._chi_hu_rights[provide_index].opr_or(GameConstants.CHR_FANG_PAO);

		}

		// 设置变量
		GRR._provider[seat_index] = provide_index;
		_status_gang = false;
		_status_gang_hou_pao = false;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);
		// _playerStatus[seat_index].clean_status();
	}

	private float getScore(ChiHuRight chr, float lChiHuScore) {
		float s = lChiHuScore;

		if (!(chr.opr_and(GameConstants.CHR_HUNAN_MEN_QING)).is_empty()) {
			chr.da_hu_count--;
		}
		if (chr.da_hu_count > 0) {
			int count = 0;
			if (!(chr.opr_and(GameConstants.CHR_HUNAN_TIAN_HU)).is_empty()) {
				count++;
			}
			if (!(chr.opr_and(GameConstants.CHR_HUNAN_DI_HU)).is_empty()) {
				count++;
			}
			if (!(chr.opr_and(GameConstants.CHR_HUNAN_QISHOU_BAO_TING)).is_empty()) {
				count++;
			}
			if (!(chr.opr_and(GameConstants.CHR_HUNAN_PENGPENG_HU)).is_empty()) {
				count++;
			}
			if (!(chr.opr_and(GameConstants.CHR_HUNAN_JIANGJIANG_HU)).is_empty()) {
				count++;
			}
			if (!(chr.opr_and(GameConstants.CHR_HUNAN_QING_YI_SE)).is_empty()) {
				count++;
			}
			if (!(chr.opr_and(GameConstants.CHR_HUNAN_QI_XIAO_DUI)).is_empty()) {
				count++;
			}
			if (!(chr.opr_and(GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI)).is_empty()) {
				count++;
			}
			if (!(chr.opr_and(GameConstants.CHR_HUNAN_HAI_DI_LAO)).is_empty()) {
				count++;
			}
			if (!(chr.opr_and(GameConstants.CHR_HUNAN_QUAN_QIU_REN)).is_empty()) {
				count++;
			}
			if (!(chr.opr_and(GameConstants.CHR_HUNAN_QIANG_GANG_HU)).is_empty()) {
				count++;
			}
			if (!(chr.opr_and(GameConstants.CHR_HUNAN_GANG_KAI)).is_empty()) {
				count++;
			}

			s = 3 * (1 << (count - 1));
		} else {
			if (!(chr.opr_and(GameConstants.CHR_HUNAN_MEN_QING)).is_empty()) {
				s = 2;
			} else {
				s = 1;
			}
		}
		return s;
	}

	@Override
	protected void set_result_describe() {
		int chrTypes;
		long type = 0;
		for (int player = 0; player < getTablePlayerNumber(); player++) {
			StringBuilder gameDesc = new StringBuilder("");

			chrTypes = GRR._chi_hu_rights[player].type_count;
			for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
				type = GRR._chi_hu_rights[player].type_list[typeIndex];

				if (GRR._chi_hu_rights[player].is_valid()) {
					if (type == GameConstants.CHR_HENAN_QIANG_GANG_HU) {
						gameDesc.append(" 抢杠胡");
					}
					if (type == GameConstants.CHR_ZI_MO) {
						gameDesc.append(" 自摸");
					}
					if (type == GameConstants.CHR_SHU_FAN) {
						gameDesc.append(" 接炮");
					}
					if (type == GameConstants.CHR_HUNAN_GANG_SHANG_PAO) {
						gameDesc.append(" 杠上炮");
					}
					if (type == GameConstants.CHR_HENAN_GANG_KAI) {
						gameDesc.append(" 杠上开花");
					}
					if (type == GameConstants.CHR_HENAN_KA_ZHANG) {
						gameDesc.append(" 卡张");
					}
					if (type == GameConstants.CHR_HENAN_DAN_DIAO) {
						gameDesc.append(" 单吊");
					}
					if (type == GameConstants.CHR_HENAN_QI_XIAO_DUI) {
						gameDesc.append(" 七对");
					}
				} else if (type == GameConstants.CHR_FANG_PAO) {
					gameDesc.append(" 放炮");
				}
			}
			if (GRR._chi_hu_rights[player].is_valid()) {
				if (GRR._chi_hu_rights[player].yaojiu_count > 0) {
					gameDesc.append(" 幺九扑X" + GRR._chi_hu_rights[player].yaojiu_count);
				}
				if (GRR._chi_hu_rights[player].heifeng_count > 0) {
					gameDesc.append(" 风扑X" + GRR._chi_hu_rights[player].heifeng_count);
				}
				if (GRR._chi_hu_rights[player].baifeng_count > 0) {
					gameDesc.append(" 将扑X" + GRR._chi_hu_rights[player].baifeng_count);
				}
				if (GRR._chi_hu_rights[player].duanmen_count > 0) {
					gameDesc.append(" 断门X" + GRR._chi_hu_rights[player].duanmen_count);
				}
			}

			int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0, shao_gang = 0;

			if (GRR != null) {
				for (int tmpPlayer = 0; tmpPlayer < getTablePlayerNumber(); tmpPlayer++) {
					for (int w = 0; w < GRR._weave_count[tmpPlayer]; w++) {
						if (GRR._weave_items[tmpPlayer][w].weave_kind != GameConstants.WIK_GANG) {
							continue;
						}
						if (tmpPlayer == player) {
							if (!GRR._weave_items[player][w].is_vavild) {
								shao_gang++;
							} else {
								if (GRR._weave_items[tmpPlayer][w].provide_player != tmpPlayer) {
									jie_gang++;
								} else {
									if (GRR._weave_items[tmpPlayer][w].public_card == 1) {
										ming_gang++;
									} else {
										an_gang++;
									}
								}
							}
						} else {
							if (!GRR._weave_items[player][w].is_vavild) {
								continue;
							}
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
				gameDesc.append(" 绕杠X" + ming_gang);
			}
			if (fang_gang > 0) {
				gameDesc.append(" 放杠X" + fang_gang);
			}
			if (jie_gang > 0) {
				gameDesc.append(" 接杠X" + jie_gang);
			}
			if (shao_gang > 0) {
				gameDesc.append(" 烧杠X" + shao_gang);
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

	/**
	 * 显示中间出的牌
	 * 
	 * @param seat_index
	 */
	public void runnable_remove_hun_middle_cards(int seat_index) {
		// 牌局未开 或者等待状态 调度不需要执行 add by zain 2017/6/1
		if ((_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) && is_sys())
			return;
		// add end

		// 去掉
		this.operate_show_card(seat_index, GameConstants.Show_Card_Center, 0, null, GameConstants.INVALID_SEAT);

		// 刷新有癞子的牌
		for (int i = 0; i < getTablePlayerNumber(); i++) {
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
						cards[j] += GameConstants.CARD_ESPECIAL_TYPE_HUN;
					}
				}
				this.operate_player_cards(i, hand_card_count, cards, 0, null);
			}
		}

		// 检测听牌
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			this._playerStatus[i]._hu_card_count = this.get_ting_card(this._playerStatus[i]._hu_cards, this.GRR._cards_index[i],
					this.GRR._weave_items[i], this.GRR._weave_count[i]);
			if (this._playerStatus[i]._hu_card_count > 0) {
				this.operate_chi_hu_cards(i, this._playerStatus[i]._hu_card_count, this._playerStatus[i]._hu_cards);
			}
		}
		this.exe_dispatch_card(this._current_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);

		//
		// this.exe_dispatch_card(seat_index,MJGameConstants.WIK_NULL, 0);
	}

	/**
	 * 调度 发最后4张牌
	 * 
	 * @param cur_player
	 * @param type
	 * @param tail
	 * @return
	 */
	@Override
	public boolean runnable_dispatch_last_card_data(int cur_player, int type, boolean tail) {
		// 牌局未开 或者等待状态 调度不需要执行 add by zain 2017/6/1
		if ((_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) && is_sys())
			return false;

		if (_handler_dispath_last_card != null) {
			// 发牌
			this.set_handler(this._handler_dispath_last_card);
			this._handler_dispath_last_card.reset_status(cur_player, type);
			this._handler.exe(this);
		}

		return true;
	}

	@Override
	public boolean exe_out_card_bao_ting(int seat_index, int card, int type) {
		// 出牌
		this.set_handler(this._handler_out_card_bao_ting);
		this._handler_out_card_bao_ting.reset_status(seat_index, card, type);
		this._handler.exe(this);

		return true;
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		// TODO Auto-generated method stub
		return false;
	}
}
