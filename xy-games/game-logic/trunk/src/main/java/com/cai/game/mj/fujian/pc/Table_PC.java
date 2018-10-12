package com.cai.game.mj.fujian.pc;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.RedisConstant;
import com.cai.common.define.ECardType;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.PBUtil;
import com.cai.common.util.SpringService;
import com.cai.future.GameSchedule;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJType;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.game.util.GameUtilConstants;
import com.cai.redis.service.RedisService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;
import protobuf.clazz.shangQiu.ShangQiuRsp.OtherResponse;

public class Table_PC extends AbstractMJTable {

	private static final long serialVersionUID = 2373854457078205932L;

	public MJHandlerPiao_PC _handler_piao;

	public int[] player_mai_ma_count;
	public int[][] player_mai_ma_data;
	public int bu_hua_count;
	int[] dispatchcardNum; // 摸牌次数
	private boolean isCanGenZhuang; // 可以跟庄
	private int genZhuangCount; // 跟庄次数
	private int genZhuangCard; // 跟庄牌
	private int next_seat_index;
	private int shang_ju_zhuang;

	public Table_PC() {
		super(MJType.GAME_TYPE_MJ_PU_CHENG);
	}

	@Override
	protected void onInitTable() {
		_handler_chi_peng = new HandlerChiPeng_PC();
		_handler_dispath_card = new MJHandlerDispatchCard_PC();
		_handler_gang = new HandlerGang_PC();
		_handler_out_card_operate = new HandlerOutCardOperate_PC();
		_handler_piao = new MJHandlerPiao_PC();
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
	public int getTablePlayerNumber() {
		if (has_rule(GameConstants_PC.GAME_RULE_PLAYER_2))
			return 2;
		if (has_rule(GameConstants_PC.GAME_RULE_PLAYER_3))
			return 3;
		return 4;
	}

	/**
	 * 跟庄操作
	 * 
	 * @param seat_index
	 * @param card
	 * @param isZhuang
	 */
	public void addGenZhuangCard(int seat_index, int card, boolean isZhuang) {
		if (isZhuang) {
			genZhuangCard = card;
			next_seat_index = (seat_index + getTablePlayerNumber() + 1) % getTablePlayerNumber();
			if (!isDispatchcardNum(seat_index)) {
				setGenZhuangCount();
			}
		} else {
			if (genZhuangCard != card || seat_index != next_seat_index) {
				isCanGenZhuang = false;
			} else {
				next_seat_index = (seat_index + getTablePlayerNumber() + 1) % getTablePlayerNumber();
			}
		}
	}
	/**
	 * 是否第一次摸牌
	 * 
	 * @param seat_index
	 * @return
	 */
	public boolean isDispatchcardNum(int seat_index) {
		return dispatchcardNum[seat_index] == 1;
	}
	
	/**
	 * 跟庄次数
	 * 
	 * @return the genZhuangCount
	 */
	public int getGenZhuangCount() {
		return genZhuangCount;
	}
	
	/**
	 * 跟庄次数累计
	 * 
	 * @param genZhuangCount
	 *            the genZhuangCount to set
	 */
	public void setGenZhuangCount() {
		this.genZhuangCount++;
	}
	/**
	 * 跟庄状态
	 * 
	 * @return the isCanGenZhuang
	 */
	public boolean isCanGenZhuang() {
		return isCanGenZhuang;
	}
	
	/** 摸牌数累计 */
	public void addDispatchcardNum(int seat_index) {
		dispatchcardNum[seat_index]++;
	}
	
	@Override
	public boolean reset_init_data() {
		super.reset_init_data();
		if (_cur_round == 1) {
			shang_ju_zhuang = 0;
		}
		player_mai_ma_count = new int[getTablePlayerNumber()];
		player_mai_ma_data = new int[getTablePlayerNumber()][get_ma_num()];

		for (int p = 0; p < getTablePlayerNumber(); p++) {
			_playerStatus[p].clear_cards_abandoned_gang();
		}
		dispatchcardNum = new int[getTablePlayerNumber()];
		return true;
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
			Table_PC mjTable_PC = this;
			GameSchedule.put(new Runnable() {
				@Override
				public void run() {
					set_handler(_handler_dispath_card);
					_handler_dispath_card.reset_status(seat_index, type, card);
					_handler.exe(mjTable_PC);
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

		if (has_rule(GameConstants_PC.GAME_RULE_ONLY_HZJ) || has_rule(GameConstants_PC.GAME_RULE_HZJ)) {
			_logic.clean_magic_cards();
			_logic.add_magic_card_index(_logic.switch_to_card_index(GameConstants_PC.HZ_MAGIC_CARD));
		} 
		_repertory_card = new int[GameConstants.CARD_DATA_HONG_ZHONG_LAI_ZI.length];
		shuffle(_repertory_card, GameConstants.CARD_DATA_HONG_ZHONG_LAI_ZI);
		
		if(has_rule(GameConstants_PC.GAME_RULE_ONLY_HZJ)) {
			_logic.clean_magic_cards();
			_logic.add_magic_card_index(_logic.switch_to_card_index(GameConstants_PC.HZ_MAGIC_CARD));
		}else if(has_rule(GameConstants_PC.GAME_RULE_QUAN_KAI_FANG)) {
			_logic.clean_magic_cards();
		}else if(has_rule(GameConstants_PC.GAME_RULE_QUAN_KAI_FANG_HZJ)) {
			_logic.clean_magic_cards();
			if(has_rule(GameConstants_PC.GAME_RULE_HZJ)) {
				_logic.add_magic_card_index(_logic.switch_to_card_index(GameConstants_PC.HZ_MAGIC_CARD));
			}
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


		/*if (get_ma_num() > 0) {
			this.set_handler(this._handler_piao);
			this._handler_piao.exe(this);
			return true;
		} else {
		}*/
		return this.on_game_start();
	}

	public int get_ma_num() {
		if(has_rule(GameConstants_PC.GAME_RULE_FEI_0)) {
			return 0;
		}
		if(has_rule(GameConstants_PC.GAME_RULE_FEI_ZHUANG_1)) {
			return 1;
		}
		if(has_rule(GameConstants_PC.GAME_RULE_FEI_ZHUANG_2)) {
			return 2;
		}
		if (has_rule(GameConstants_PC.GAME_RULE_FEI_EVERY_1)) {
			return getTablePlayerNumber()*1;
		}
		if (has_rule(GameConstants_PC.GAME_RULE_FEI_EVERY_2)) {
			return getTablePlayerNumber()*2;
		}
		if (has_rule(GameConstants_PC.GAME_RULE_FEI_EVERY_4)) {
			return getTablePlayerNumber()*4;
		}          
		return 0;
	}

	@Override
	protected boolean on_game_start() {
		_game_status = GameConstants_PC.GS_MJ_PLAY;

		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants_PC.MAX_COUNT];
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {

			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants_PC.MAX_COUNT; j++) {
				if (hand_cards[i][j] == GameConstants_PC.HZ_MAGIC_CARD) {
					gameStartResponse.addCardData(hand_cards[i][j] + GameConstants_PC.CARD_ESPECIAL_TYPE_LAI_ZI);
				} else {
					gameStartResponse.addCardData(hand_cards[i][j]);
				}
			}

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			load_room_info_data(roomResponse);
			load_common_status(roomResponse);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setCurrentPlayer(_current_player == GameConstants_PC.INVALID_SEAT ? _resume_player : _current_player);
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

			for (int j = 0; j < GameConstants_PC.MAX_COUNT; j++) {
				if (hand_cards[i][j] == GameConstants_PC.HZ_MAGIC_CARD) {
					cards.addItem(hand_cards[i][j] + GameConstants_PC.CARD_ESPECIAL_TYPE_LAI_ZI);
				} else {
					cards.addItem(hand_cards[i][j]);
				}
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

		exe_dispatch_card(_current_player, GameConstants_PC.DispatchCard_Type_Tian_Hu, GameConstants_PC.DELAY_SEND_CARD_DELAY);

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

		int max_ting_count = GameConstants.MAX_ZI;//27

		for (int i = 0; i < max_ting_count; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					GameConstants.HU_CARD_TYPE_ZIMO, seat_index)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}

		if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, GameConstants_PC.HZ_MAGIC_CARD, chr,
				GameConstants.HU_CARD_TYPE_ZIMO, seat_index)) {
			cards[count] = GameConstants_PC.HZ_MAGIC_CARD;
			count++;
		}

		if (count == 0) {
		} else if (count >= max_ting_count) {
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	public boolean operate_mai_ma_card(int to_player, boolean show_cards) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);

		//!!!!!!!!!!!
		roomResponse.setType(MsgConstants.RESPONSE_SX_MAI_MA_CARDS);

		for (int p = 0; p < getTablePlayerNumber(); p++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			roomResponse.addOutCardTingCount(player_mai_ma_count[p]);
			for (int mc = 0; mc <GameConstants.MAX_NIAO_CARD; mc++) {
				if (show_cards) {
					cards.addItem(GRR.get_player_niao_cards()[p][mc]);
				} else {
					cards.addItem(-1);
				}
			}
			roomResponse.addOutCardTingCards(cards);
		}

		if (to_player == GameConstants_PC.INVALID_SEAT) {
			GRR.add_room_response(roomResponse);
			return this.send_response_to_room(roomResponse);
		} else {
			return this.send_response_to_player(to_player, roomResponse);
		}
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
	public boolean operate_player_cards(int seat_index, int card_count, int cards[], int weave_count, WeaveItem weaveitems[], int send_card) {
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

		int index_card[] = new int[] { GameConstants.INVALID_CARD, GameConstants.INVALID_CARD, GameConstants.INVALID_CARD,
				GameConstants.INVALID_CARD };
		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			// 特殊牌值处理
			int real_card = cards[i];
			for (int k = 0; k < GRR.get_liang_card_count(seat_index); k++) {
				if (index_card[k] == GameConstants.INVALID_CARD && real_card == GRR.get_player_liang_card(seat_index, k)) {
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
			for (int x = 0; x < GameConstants_PC.MAX_ABANDONED_CARDS_COUNT; x++) {
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
						playerStatus.add_action(GameConstants_PC.WIK_GANG);
						playerStatus.add_gang(card, seat_index, 1); // 加上杠
						bAroseAction = true;
					}
				}
			}

			if (_playerStatus[i].is_chi_hu_round()) {
				boolean can_hu_this_card = true;
				int[] tmp_cards_data_hu = _playerStatus[i].get_cards_abandoned_hu();
				for (int x = 0; x < GameConstants_PC.MAX_ABANDONED_CARDS_COUNT; x++) {
					if (tmp_cards_data_hu[x] != 0) {
						can_hu_this_card = false;
						break;
					}
				}
				if (can_hu_this_card) {
					ChiHuRight chr = GRR._chi_hu_rights[i];
					chr.set_empty();
					int cbWeaveCount = GRR._weave_count[i];

					int card_type = GameConstants_PC.HU_CARD_TYPE_JIE_PAO;
					action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr, card_type, i);

					if (action != 0) {
						_playerStatus[i].add_action(GameConstants_PC.WIK_CHI_HU);
						_playerStatus[i].add_chi_hu(card, seat_index);
						bAroseAction = true;
					}
				}
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

	// 杠牌分析 包括补杠
	public int analyse_gang_hong_zhong_all(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount, GangCardResult gangCardResult,
			boolean check_weave, int cards_abandoned_gang[], int seat_index, int send_card_data) {
		// 设置变量
		int cbActionMask = GameConstants_PC.WIK_NULL;

		// 手上杠牌
		for (int i = 0; i < GameConstants_PC.MAX_INDEX; i++) {
			if (cards_index[i] == 4) {
				cbActionMask |= GameConstants_PC.WIK_GANG;

				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = _logic.switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;// 安刚
				gangCardResult.type[index] = GameConstants_PC.GANG_TYPE_AN_GANG;
			}
		}

		if (check_weave) {
			// 组合杠牌，包括以前能杠，但是不杠，发牌之后而选择补杠的
			for (int i = 0; i < cbWeaveCount; i++) {
				if (WeaveItem[i].weave_kind != GameConstants_PC.WIK_PENG) {
					continue;
				}
				for (int j = 0; j < GameConstants_PC.MAX_INDEX; j++) {
					if (cards_index[j] != 1 || cards_abandoned_gang[j] != 0) { // 红中不能杠，能接杠但是不杠的牌也过滤掉
						continue;
					}

					if (WeaveItem[i].center_card == _logic.switch_to_card_data(j)) {
						cbActionMask |= GameConstants_PC.WIK_GANG;

						int index = gangCardResult.cbCardCount++;
						gangCardResult.cbCardData[index] = WeaveItem[i].center_card;
						gangCardResult.isPublic[index] = 1;// 明刚
						gangCardResult.type[index] = GameConstants_PC.GANG_TYPE_ADD_GANG;
						break;
					}
				}
			}
		}

		return cbActionMask;
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
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {

	

		if (card_type == GameConstants_PC.HU_CARD_TYPE_ZI_MO) {
			chiHuRight.opr_or(GameConstants_PC.CHR_ZI_MO);
		} else if (card_type == GameConstants_PC.HU_CARD_TYPE_JIE_PAO) {
			chiHuRight.opr_or(GameConstants_PC.CHR_SHU_FAN);
		} else if (card_type == GameConstants_PC.HU_CARD_TYPE_QIANG_GANG) {
			chiHuRight.opr_or(GameConstants_PC.CHR_QING_GANG_HU);
		}
		
		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_card_count = _logic.get_magic_card_count();

		if (magic_card_count > 2) { // 一般只有两种癞子牌存在
			magic_card_count = 2;
		}
		for (int i = 0; i < magic_card_count; i++) {
			magic_cards_index[i] = _logic.get_magic_card_index(i);
		}

		
		int cbChiHuKind = GameConstants_PC.WIK_NULL;

		// 构造扑克
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入扑克
		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}
		boolean pinghu = false;
		boolean is_qing_yi_se = false;
		boolean is_qi_da_dui = false;
		boolean is_qi_xiao_dui = false;
		boolean is_hh_qi_xiao_dui = false;
		boolean is_qys_qxd = false;
		boolean is_qys_qdd = false;
		boolean is_qys_hhqxd = false;
		
		//判断是否清一色
		if(has_rule(GameConstants_PC.GAME_RULE_ONLY_HZJ)||has_rule(GameConstants_PC.GAME_RULE_HZJ)) {
			is_qing_yi_se = _logic.is_qing_yi_se(cards_index, weaveItems, weave_count, cur_card);
		}else {
			is_qing_yi_se = _logic.check_hubei_ying_qing_yi_se(cbCardIndexTemp, weaveItems, weave_count);
		}
		
		//判断是否是七大对 大七对的胡牌规则和碰碰胡一样 
		boolean only_gang = only_gang(weaveItems, weave_count);
		is_qi_da_dui = AnalyseCardUtil.analyse_peng_hu_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card), magic_cards_index, magic_card_count)
				&& only_gang; // 不能吃不能碰
		
		//判断是否是七小对
		int qi_xiao_dui=_logic.is_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card);
		//判断是否是豪华七小对
		if(qi_xiao_dui==GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI||qi_xiao_dui==GameConstants.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI) {
			is_hh_qi_xiao_dui=true;
			is_qi_xiao_dui=true;
		}else if (GameConstants.WIK_NULL!=qi_xiao_dui) {
			is_qi_xiao_dui=true;
		}
		if(is_qing_yi_se&&is_hh_qi_xiao_dui) {
			is_qys_hhqxd=true;
		}
		if(is_qing_yi_se&&is_qi_da_dui) {
			is_qys_qdd=true;
		}
		if(is_qing_yi_se&&is_qi_xiao_dui) {
			is_qys_qxd=true;
		}
		if (has_rule(GameConstants_PC.GAME_RULE_ZI_MO) && card_type == GameConstants_PC.HU_CARD_TYPE_JIE_PAO&&!is_qing_yi_se&&!is_qing_yi_se &&! is_qi_da_dui&&!is_qi_xiao_dui &&!is_hh_qi_xiao_dui&&!is_qys_qxd&&!is_qys_qdd &&!is_qys_hhqxd ) {
			chiHuRight.set_empty();
			return GameConstants_PC.WIK_NULL;
		}
		if(has_rule(GameConstants_PC.GAME_RULE_ONLY_HZJ)) {
		}else if(has_rule(GameConstants_PC.GAME_RULE_QUAN_KAI_FANG)) {
			if(has_rule(GameConstants_PC.GAME_RULE_QI_XIAO_DUI)&&is_qi_xiao_dui) {
				is_qi_xiao_dui=true;
			}else if(is_qi_xiao_dui){
				is_qi_xiao_dui=false;
				pinghu=true;
			}
			if(has_rule(GameConstants_PC.GAME_RULE_QI_DA_DUI)&&is_qi_da_dui) {
				is_qi_da_dui=true;
			}else if(is_qi_da_dui){
				is_qi_da_dui=false;
				pinghu=true;
			}
			if(has_rule(GameConstants_PC.GAME_RULE_QING_YI_SE)&&is_qing_yi_se) {
				is_qing_yi_se=true;
			}else if(is_qing_yi_se){
				is_qing_yi_se=false;
				pinghu=true;
			}
			if(has_rule(GameConstants_PC.GAME_RULE_HH_QI_XIAO_DUI)&&is_hh_qi_xiao_dui) {
				is_hh_qi_xiao_dui=true;
			}else if(is_hh_qi_xiao_dui){
				is_hh_qi_xiao_dui=false;
				pinghu=true;
			}
			if(has_rule(GameConstants_PC.GAME_RULE_QING_YI_SE_QI_XIAO_DUI)&&is_qys_qxd) {
				is_qys_qxd=true;
			}else if(is_qys_qxd){
				is_qys_qxd=false;
				pinghu=true;
			}
			if(has_rule(GameConstants_PC.GAME_RULE_QING_YI_SE_QI_DA_DUI)&&is_qys_qdd) {
				is_qys_qdd=true;
			}else if(is_qys_qdd){
				is_qys_qdd=false;
				pinghu=true;
			}
			if(has_rule(GameConstants_PC.GAME_RULE_QING_YI_SE_HH_QI_XIAO_DUI)&&is_qys_hhqxd) {
				is_qys_hhqxd=true;
			}else if(is_qys_hhqxd){
				is_qys_hhqxd=false;
				pinghu=true;
			}
			
		}else if(has_rule(GameConstants_PC.GAME_RULE_QUAN_KAI_FANG_HZJ)) {
			if(has_rule(GameConstants_PC.GAME_RULE_QI_XIAO_DUI)&&is_qi_xiao_dui) {
				is_qi_xiao_dui=true;
			}else if(is_qi_xiao_dui){
				is_qi_xiao_dui=false;
				pinghu=true;
			}
			if(has_rule(GameConstants_PC.GAME_RULE_QI_DA_DUI)&&is_qi_da_dui) {
				is_qi_da_dui=true;
			}else if(is_qi_da_dui){
				is_qi_da_dui=false;
				pinghu=true;
			}
			if(has_rule(GameConstants_PC.GAME_RULE_QING_YI_SE)&&is_qing_yi_se) {
				is_qing_yi_se=true;
			}else if(is_qing_yi_se){
				is_qing_yi_se=false;
				pinghu=true;
			}
			if(has_rule(GameConstants_PC.GAME_RULE_HH_QI_XIAO_DUI)&&is_hh_qi_xiao_dui) {
				is_hh_qi_xiao_dui=true;
			}else if(is_hh_qi_xiao_dui){
				is_hh_qi_xiao_dui=false;
				pinghu=true;
			}
			if(has_rule(GameConstants_PC.GAME_RULE_QING_YI_SE_QI_XIAO_DUI)&&is_qys_qxd) {
				is_qys_qxd=true;
			}else if(is_qys_qxd){
				is_qys_qxd=false;
				pinghu=true;
			}
			if(has_rule(GameConstants_PC.GAME_RULE_QING_YI_SE_QI_DA_DUI)&&is_qys_qdd) {
				is_qys_qdd=true;
			}else if(is_qys_qdd){
				is_qys_qdd=false;
				pinghu=true;
			}
			if(has_rule(GameConstants_PC.GAME_RULE_QING_YI_SE_HH_QI_XIAO_DUI)&&is_qys_hhqxd) {
				is_qys_hhqxd=true;
			}else if(is_qys_hhqxd){
				is_qys_hhqxd=false;
				pinghu=true;
			}
		}

		if(is_qys_hhqxd) {
			chiHuRight.opr_or(GameConstants_PC.CHR_QING_YI_SE_HH_QI_XIAO_DUI);
			cbChiHuKind = GameConstants_PC.WIK_CHI_HU;
			return cbChiHuKind;
		}else if(is_qys_qdd) {
			chiHuRight.opr_or(GameConstants_PC.CHR_QING_YI_SE_QI_DA_DUI);
			cbChiHuKind = GameConstants_PC.WIK_CHI_HU;
			return cbChiHuKind;
		}else if(is_qys_qxd) {
			chiHuRight.opr_or(GameConstants_PC.CHR_QING_YI_SE_QI_XIAO_DUI);
			cbChiHuKind = GameConstants_PC.WIK_CHI_HU;
			return cbChiHuKind;
		}else if(is_hh_qi_xiao_dui) {
			chiHuRight.opr_or(GameConstants_PC.CHR_HH_QI_XIAO_DUI);
			cbChiHuKind = GameConstants_PC.WIK_CHI_HU;
			return cbChiHuKind;
		}else if(is_qing_yi_se) {
			chiHuRight.opr_or(GameConstants_PC.CHR_QING_YI_SE);
			cbChiHuKind = GameConstants_PC.WIK_CHI_HU;
			return cbChiHuKind;
		}else if(is_qi_da_dui) {
			chiHuRight.opr_or(GameConstants_PC.CHR_QI_DA_DUI);
			cbChiHuKind = GameConstants_PC.WIK_CHI_HU;
			return cbChiHuKind;
		}else if(is_qi_xiao_dui) {
			chiHuRight.opr_or(GameConstants_PC.CHR_QI_XIAO_DUI);
			cbChiHuKind = GameConstants_PC.WIK_CHI_HU;
			return cbChiHuKind;
		}else if(pinghu) {
			chiHuRight.opr_or(GameConstants_PC.CHR_PING_HU);
			cbChiHuKind = GameConstants_PC.WIK_CHI_HU;
			return cbChiHuKind;
		}
		//pinghu=AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card), magic_cards_index, magic_card_count);
		pinghu=AnalyseCardUtil.analyse_win_by_cards_index(cbCardIndexTemp, -1, magic_cards_index, magic_card_count);
		
		if (card_type != GameConstants_PC.HU_CARD_TYPE_QIANG_GANG) {
			if (dispatchcardNum[_seat_index]<= 1) {
				if (_seat_index == _cur_banker){
					if(card_type == GameConstants_PC.HU_CARD_TYPE_ZI_MO&&pinghu){
						chiHuRight.opr_or(GameConstants_PC.CHR_TIAN_HU);
						cbChiHuKind = GameConstants_PC.WIK_CHI_HU;
						return cbChiHuKind;
					}
				} 
			}
			if (weave_count == 0 && _out_card_count == 1 && _out_card_player == GRR._banker_player && _seat_index != GRR._banker_player
					&& card_type != GameConstants_PC.HU_CARD_TYPE_ZI_MO&&pinghu) {
				chiHuRight.opr_or(GameConstants_PC.CHR_DI_HU); 
				cbChiHuKind = GameConstants_PC.WIK_CHI_HU;
				return cbChiHuKind;
			}
		}
		
		if(pinghu) {
			chiHuRight.opr_or(GameConstants_PC.CHR_PING_HU);
			cbChiHuKind = GameConstants_PC.WIK_CHI_HU;
			return cbChiHuKind;
		}else {
			return GameConstants_PC.WIK_NULL;
		}
		
	}


	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, int hu_type) {
		//hu_type 1自摸 2抢杠胡 3点炮
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		int wFanShu = get_hu_type_score(seat_index,hu_type==1||hu_type==2);
		countCardType(chr, seat_index);
		
		/////////////////////////////////////////////// 算分//////////////////////////
		// 统计
		if (hu_type==1) {
			// 自摸
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				GRR._lost_fan_shu[i][seat_index] = wFanShu;
			}
		} else if(hu_type==2){// 抢杠胡算胡牌玩家自摸 杠牌玩家包赔三家
			GRR._lost_fan_shu[provide_index][seat_index] = wFanShu*(getTablePlayerNumber()-1);
		}else {
			GRR._lost_fan_shu[provide_index][seat_index] = wFanShu;
		}
		
		
		float lChiHuScore = get_hu_type_score(seat_index,hu_type==1||hu_type==2);
		////////////////////////////////////////////////////// 自摸 算分
		if (hu_type==1) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				float s = lChiHuScore; // 

				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;
			}
		}else if(hu_type==2) {
			float s = lChiHuScore;

			// 胡牌分
			GRR._game_score[provide_index] -= s*(getTablePlayerNumber()-1);
			GRR._game_score[seat_index] += s*(getTablePlayerNumber()-1);
		}
		////////////////////////////////////////////////////// 点炮 算分
		else {
			GRR._chi_hu_rights[provide_index].opr_or(GameConstants.CHR_FANG_PAO);

			float s = lChiHuScore ;

			// 胡牌分
			GRR._game_score[provide_index] -= s;
			GRR._game_score[seat_index] += s;
		}

		int[] ma_score = get_ma_score(seat_index, provide_index, hu_type);
		for (int p = 0; p < getTablePlayerNumber(); p++) {
			GRR._game_score[p] += ma_score[p];
			if (get_ma_num() > 0) {
				GRR._result_des[p] += "苍蝇分：" + ma_score[p];
			}
		}
		// 设置变量
		GRR._provider[seat_index] = provide_index;
		_status_gang = false;
		_status_gang_hou_pao = false;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);
		
	}

	public int get_hu_type_score(int seat_index,boolean zimo) {
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		int fan = 1;
		int max_chr = 0;
		if(getTablePlayerNumber()==4) {
			if(!zimo) {
				if (!chr.opr_and(GameConstants_PC.CHR_QI_XIAO_DUI).is_empty()) {
					fan = 6;
					max_chr = GameConstants_PC.CHR_QI_XIAO_DUI;
				}
				if (!chr.opr_and(GameConstants_PC.CHR_QI_DA_DUI).is_empty()) {
					fan = 6;
					max_chr = GameConstants_PC.CHR_QI_DA_DUI;
				}
				if (!chr.opr_and(GameConstants_PC.CHR_QING_YI_SE).is_empty()) {
					fan = 12;
					max_chr = GameConstants_PC.CHR_QING_YI_SE;
				}
				if (!chr.opr_and(GameConstants_PC.CHR_HH_QI_XIAO_DUI).is_empty()) {
					fan = 12;
					max_chr = GameConstants_PC.CHR_HH_QI_XIAO_DUI;
				}
				if (!chr.opr_and(GameConstants_PC.CHR_QING_YI_SE_QI_XIAO_DUI).is_empty()) {
					fan = 24;
					max_chr = GameConstants_PC.CHR_QING_YI_SE_QI_XIAO_DUI;
				}
				if (!chr.opr_and(GameConstants_PC.CHR_QING_YI_SE_QI_DA_DUI).is_empty()) {
					fan = 24;
					max_chr = GameConstants_PC.CHR_QING_YI_SE_QI_DA_DUI;
				}
				if (!chr.opr_and(GameConstants_PC.CHR_QING_YI_SE_HH_QI_XIAO_DUI).is_empty()) {
					fan = 48;
					max_chr = GameConstants_PC.CHR_QING_YI_SE_HH_QI_XIAO_DUI;
				}
				if (!chr.opr_and(GameConstants_PC.CHR_DI_HU).is_empty()) {
					fan = 12;
					max_chr = GameConstants_PC.CHR_DI_HU;
				}
				if (!chr.opr_and(GameConstants_PC.CHR_PING_HU).is_empty()) {
					fan = 1;
					max_chr = GameConstants_PC.CHR_PING_HU;
				}
			}else {
				if (!chr.opr_and(GameConstants_PC.CHR_QI_XIAO_DUI).is_empty()) {
					fan = 4;
					max_chr = GameConstants_PC.CHR_QI_XIAO_DUI;
				}
				if (!chr.opr_and(GameConstants_PC.CHR_QI_DA_DUI).is_empty()) {
					fan = 4;
					max_chr = GameConstants_PC.CHR_QI_DA_DUI;
				}
				if (!chr.opr_and(GameConstants_PC.CHR_QING_YI_SE).is_empty()) {
					fan = 8;
					max_chr = GameConstants_PC.CHR_QING_YI_SE;
				}
				if (!chr.opr_and(GameConstants_PC.CHR_HH_QI_XIAO_DUI).is_empty()) {
					fan = 8;
					max_chr = GameConstants_PC.CHR_HH_QI_XIAO_DUI;
				}
				if (!chr.opr_and(GameConstants_PC.CHR_QING_YI_SE_QI_XIAO_DUI).is_empty()) {
					fan = 16;
					max_chr = GameConstants_PC.CHR_QING_YI_SE_QI_XIAO_DUI;
				}
				if (!chr.opr_and(GameConstants_PC.CHR_QING_YI_SE_QI_DA_DUI).is_empty()) {
					fan = 16;
					max_chr = GameConstants_PC.CHR_QING_YI_SE_QI_DA_DUI;
				}
				if (!chr.opr_and(GameConstants_PC.CHR_QING_YI_SE_HH_QI_XIAO_DUI).is_empty()) {
					fan = 32;
					max_chr = GameConstants_PC.CHR_QING_YI_SE_HH_QI_XIAO_DUI;
				}
				if (!chr.opr_and(GameConstants_PC.CHR_TIAN_HU).is_empty()) {
					fan = 4;
					max_chr = GameConstants_PC.CHR_TIAN_HU;
				}
				if (!chr.opr_and(GameConstants_PC.CHR_PING_HU).is_empty()) {
					fan = 2;
					max_chr = GameConstants_PC.CHR_PING_HU;
				}
				/*if (!chr.opr_and(GameConstants_PC.CHR_QING_GANG_HU).is_empty()) {
					fan = 2;
					max_chr = GameConstants_PC.CHR_QING_GANG_HU;
				}*/
			}
		}else if(getTablePlayerNumber()==3) {
			if(!zimo) {
				if (!chr.opr_and(GameConstants_PC.CHR_QI_XIAO_DUI).is_empty()) {
					fan = 4;
					max_chr = GameConstants_PC.CHR_QI_XIAO_DUI;
				}
				if (!chr.opr_and(GameConstants_PC.CHR_QI_DA_DUI).is_empty()) {
					fan = 4;
					max_chr = GameConstants_PC.CHR_QI_DA_DUI;
				}
				if (!chr.opr_and(GameConstants_PC.CHR_QING_YI_SE).is_empty()) {
					fan = 8;
					max_chr = GameConstants_PC.CHR_QING_YI_SE;
				}
				if (!chr.opr_and(GameConstants_PC.CHR_HH_QI_XIAO_DUI).is_empty()) {
					fan = 8;
					max_chr = GameConstants_PC.CHR_HH_QI_XIAO_DUI;
				}
				if (!chr.opr_and(GameConstants_PC.CHR_QING_YI_SE_QI_XIAO_DUI).is_empty()) {
					fan = 16;
					max_chr = GameConstants_PC.CHR_QING_YI_SE_QI_XIAO_DUI;
				}
				if (!chr.opr_and(GameConstants_PC.CHR_QING_YI_SE_QI_DA_DUI).is_empty()) {
					fan = 16;
					max_chr = GameConstants_PC.CHR_QING_YI_SE_QI_DA_DUI;
				}
				if (!chr.opr_and(GameConstants_PC.CHR_QING_YI_SE_HH_QI_XIAO_DUI).is_empty()) {
					fan = 32;
					max_chr = GameConstants_PC.CHR_QING_YI_SE_HH_QI_XIAO_DUI;
				}
				if (!chr.opr_and(GameConstants_PC.CHR_DI_HU).is_empty()) {
					fan = 8;
					max_chr = GameConstants_PC.CHR_DI_HU;
				}
				if (!chr.opr_and(GameConstants_PC.CHR_PING_HU).is_empty()) {
					fan = 1;
					max_chr = GameConstants_PC.CHR_PING_HU;
				}
			}else {
				if (!chr.opr_and(GameConstants_PC.CHR_QI_XIAO_DUI).is_empty()) {
					fan = 4;
					max_chr = GameConstants_PC.CHR_QI_XIAO_DUI;
				}
				if (!chr.opr_and(GameConstants_PC.CHR_QI_DA_DUI).is_empty()) {
					fan = 4;
					max_chr = GameConstants_PC.CHR_QI_DA_DUI;
				}
				if (!chr.opr_and(GameConstants_PC.CHR_QING_YI_SE).is_empty()) {
					fan = 8;
					max_chr = GameConstants_PC.CHR_QING_YI_SE;
				}
				if (!chr.opr_and(GameConstants_PC.CHR_HH_QI_XIAO_DUI).is_empty()) {
					fan = 8;
					max_chr = GameConstants_PC.CHR_HH_QI_XIAO_DUI;
				}
				if (!chr.opr_and(GameConstants_PC.CHR_QING_YI_SE_QI_XIAO_DUI).is_empty()) {
					fan = 16;
					max_chr = GameConstants_PC.CHR_QING_YI_SE_QI_XIAO_DUI;
				}
				if (!chr.opr_and(GameConstants_PC.CHR_QING_YI_SE_QI_DA_DUI).is_empty()) {
					fan = 16;
					max_chr = GameConstants_PC.CHR_QING_YI_SE_QI_DA_DUI;
				}
				if (!chr.opr_and(GameConstants_PC.CHR_QING_YI_SE_HH_QI_XIAO_DUI).is_empty()) {
					fan = 32;
					max_chr = GameConstants_PC.CHR_QING_YI_SE_HH_QI_XIAO_DUI;
				}
				if (!chr.opr_and(GameConstants_PC.CHR_TIAN_HU).is_empty()) {
					fan = 4;
					max_chr = GameConstants_PC.CHR_TIAN_HU;
				}
				if (!chr.opr_and(GameConstants_PC.CHR_PING_HU).is_empty()) {
					fan = 2;
					max_chr = GameConstants_PC.CHR_PING_HU;
				}
				/*if (!chr.opr_and(GameConstants_PC.CHR_QING_GANG_HU).is_empty()) {
					fan = 2;
					max_chr = GameConstants_PC.CHR_QING_GANG_HU;
				}*/
			}
		}else if(getTablePlayerNumber()==2) {
			if(!zimo) {
				if (!chr.opr_and(GameConstants_PC.CHR_QI_XIAO_DUI).is_empty()) {
					fan = 2;
					max_chr = GameConstants_PC.CHR_QI_XIAO_DUI;
				}
				if (!chr.opr_and(GameConstants_PC.CHR_QI_DA_DUI).is_empty()) {
					fan = 2;
					max_chr = GameConstants_PC.CHR_QI_DA_DUI;
				}
				if (!chr.opr_and(GameConstants_PC.CHR_QING_YI_SE).is_empty()) {
					fan = 4;
					max_chr = GameConstants_PC.CHR_QING_YI_SE;
				}
				if (!chr.opr_and(GameConstants_PC.CHR_HH_QI_XIAO_DUI).is_empty()) {
					fan = 4;
					max_chr = GameConstants_PC.CHR_HH_QI_XIAO_DUI;
				}
				if (!chr.opr_and(GameConstants_PC.CHR_QING_YI_SE_QI_XIAO_DUI).is_empty()) {
					fan = 8;
					max_chr = GameConstants_PC.CHR_QING_YI_SE_QI_XIAO_DUI;
				}
				if (!chr.opr_and(GameConstants_PC.CHR_QING_YI_SE_QI_DA_DUI).is_empty()) {
					fan = 8;
					max_chr = GameConstants_PC.CHR_QING_YI_SE_QI_DA_DUI;
				}
				if (!chr.opr_and(GameConstants_PC.CHR_QING_YI_SE_HH_QI_XIAO_DUI).is_empty()) {
					fan = 16;
					max_chr = GameConstants_PC.CHR_QING_YI_SE_HH_QI_XIAO_DUI;
				}
				if (!chr.opr_and(GameConstants_PC.CHR_DI_HU).is_empty()) {
					fan = 4;
					max_chr = GameConstants_PC.CHR_DI_HU;
				}
				if (!chr.opr_and(GameConstants_PC.CHR_PING_HU).is_empty()) {
					fan = 1;
					max_chr = GameConstants_PC.CHR_PING_HU;
				}
			}else {
				if (!chr.opr_and(GameConstants_PC.CHR_QI_XIAO_DUI).is_empty()) {
					fan = 4;
					max_chr = GameConstants_PC.CHR_QI_XIAO_DUI;
				}
				if (!chr.opr_and(GameConstants_PC.CHR_QI_DA_DUI).is_empty()) {
					fan = 4;
					max_chr = GameConstants_PC.CHR_QI_DA_DUI;
				}
				if (!chr.opr_and(GameConstants_PC.CHR_QING_YI_SE).is_empty()) {
					fan = 8;
					max_chr = GameConstants_PC.CHR_QING_YI_SE;
				}
				if (!chr.opr_and(GameConstants_PC.CHR_HH_QI_XIAO_DUI).is_empty()) {
					fan = 8;
					max_chr = GameConstants_PC.CHR_HH_QI_XIAO_DUI;
				}
				if (!chr.opr_and(GameConstants_PC.CHR_QING_YI_SE_QI_XIAO_DUI).is_empty()) {
					fan = 16;
					max_chr = GameConstants_PC.CHR_QING_YI_SE_QI_XIAO_DUI;
				}
				if (!chr.opr_and(GameConstants_PC.CHR_QING_YI_SE_QI_DA_DUI).is_empty()) {
					fan = 16;
					max_chr = GameConstants_PC.CHR_QING_YI_SE_QI_DA_DUI;
				}
				if (!chr.opr_and(GameConstants_PC.CHR_QING_YI_SE_HH_QI_XIAO_DUI).is_empty()) {
					fan = 32;
					max_chr = GameConstants_PC.CHR_QING_YI_SE_HH_QI_XIAO_DUI;
				}
				if (!chr.opr_and(GameConstants_PC.CHR_TIAN_HU).is_empty()) {
					fan = 4;
					max_chr = GameConstants_PC.CHR_TIAN_HU;
				}
				if (!chr.opr_and(GameConstants_PC.CHR_PING_HU).is_empty()) {
					fan = 2;
					max_chr = GameConstants_PC.CHR_PING_HU;
				}
				/*if (!chr.opr_and(GameConstants_PC.CHR_QING_GANG_HU).is_empty()) {
					fan = 2;
					max_chr = GameConstants_PC.CHR_QING_GANG_HU;
				}*/
			}
		}
		


		int hu_type = 0;
		if (!chr.opr_and(GameConstants_PC.CHR_ZI_MO).is_empty()) {
			hu_type = GameConstants_PC.CHR_ZI_MO;
		} else if (!chr.opr_and(GameConstants_PC.CHR_PING_HU).is_empty()) {
			hu_type = GameConstants_PC.CHR_PING_HU;
		} else if (!chr.opr_and(GameConstants_PC.CHR_QING_GANG_HU).is_empty()) {
			hu_type = GameConstants_PC.CHR_QING_GANG_HU;
		}

		chr.opr_or(max_chr);
		chr.opr_or(hu_type);

		return fan;
	}

	private int[] get_ma_score(int seat_index, int provide_index, int hu_type) {

		int[] ma_score = {0,0,0,0};
		if(getTablePlayerNumber()<4) {
			return ma_score;
		}
		// 每个人
		for (int p = 0; p < getTablePlayerNumber(); p++) {
			// 每个人的每个码
			for (int j = 0; j < GameConstants.MAX_NIAO_CARD; j++) {
				if (GRR.get_player_niao_cards()[p][j] == GameConstants_PC.HZ_MAGIC_CARD) {
					continue;
				}
				int seat = (GRR._banker_player + GRR.get_player_niao_cards()[p][j] - 1 + getTablePlayerNumber()) % getTablePlayerNumber();
				// 每个人的每个码对应人的每个杠
				for (int k = 0; k < GRR._gang_score[seat].gang_count; k++) {
					// 每个人的每个码对应人的每个杠的每个人出的分，，，哇！！！！！心好痛
					for (int kk = 0; kk < this.getTablePlayerNumber(); kk++) {
						if (kk == seat) {
							ma_score[p] += GRR._gang_score[seat].scores[k][kk];
						} else {
							ma_score[kk] += GRR._gang_score[seat].scores[k][kk];
						}
					}
				}
			}
		}

		int hu_type_score = get_hu_type_score(seat_index,hu_type==1||hu_type==2);
		if (hu_type==1) {
			// 每个人
			for (int p = 0; p < getTablePlayerNumber(); p++) {
				// 每个人的每个码
				for (int j = 0; j < GameConstants.MAX_NIAO_CARD; j++) {
					if (GRR.get_player_niao_cards()[p][j] == GameConstants_PC.HZ_MAGIC_CARD) {
						continue;
					}
					int seat = (GRR._banker_player + GRR.get_player_niao_cards()[p][j] - 1 + getTablePlayerNumber()) % getTablePlayerNumber();
					if (seat == seat_index) {
						for (int pp = 0; pp < getTablePlayerNumber(); pp++) {
							ma_score[p] += hu_type_score;
							ma_score[pp] -= hu_type_score;
						}
					}
				}
			}
		} else if(hu_type==2){
			// 每个人
			for (int p = 0; p < getTablePlayerNumber(); p++) {
				// 每个人的每个码
				for (int j = 0; j < GameConstants.MAX_NIAO_CARD; j++) {
					if (GRR.get_player_niao_cards()[p][j] == GameConstants_PC.HZ_MAGIC_CARD) {
						continue;
					}
					int seat = (GRR._banker_player + GRR.get_player_niao_cards()[p][j] - 1 + getTablePlayerNumber()) % getTablePlayerNumber();
					if (seat == seat_index) {
						ma_score[p] += hu_type_score*(getTablePlayerNumber()-1);
						ma_score[provide_index] -= hu_type_score*(getTablePlayerNumber()-1);
					} else if (seat_index == provide_index) {
						ma_score[seat_index] += hu_type_score*(getTablePlayerNumber()-1);
						ma_score[p] -= hu_type_score*(getTablePlayerNumber()-1);
					}
				}
			}
		} else{
			// 每个人
			for (int p = 0; p < getTablePlayerNumber(); p++) {
				// 每个人的每个码
				for (int j = 0; j <GameConstants.MAX_NIAO_CARD; j++) {
					if (GRR.get_player_niao_cards()[p][j] == GameConstants_PC.HZ_MAGIC_CARD) {
						continue;
					}
					int seat = (GRR._banker_player + GRR.get_player_niao_cards()[p][j] - 1 + getTablePlayerNumber()) % getTablePlayerNumber();
					if (seat == seat_index) {
						ma_score[p] += hu_type_score;
						ma_score[provide_index] -= hu_type_score;
					} else if (seat_index == provide_index) {
						ma_score[seat_index] += hu_type_score;
						ma_score[p] -= hu_type_score;
					}
				}
			}
		}

		return ma_score;
	}

	protected boolean on_game_finish(int seat_index, int reason) {

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
			// operate_mai_ma_card(GameConstants_PC.INVALID_SEAT, true);

			game_end.setRoundOverType(1);
			game_end.setStartTime(GRR._start_time);

			game_end.setGameTypeIndex(GRR._game_type_index);
			roomResponse.setLeftCardCount(GRR._left_card_count);

			for (int i = 0; i < GRR._especial_card_count; i++) {
				game_end.addEspecialShowCards(GRR._especial_show_cards[i]);
			}

			GRR._end_type = reason;

			// 流局不需要计算杠分。
			float lGangScore[] = new float[this.getTablePlayerNumber()];
			int genZhuangScore[] = new int[getTablePlayerNumber()];// 跟庄分
			for (int i = 0; i < this.getTablePlayerNumber() && reason != GameConstants_PC.Game_End_DRAW; i++) {
				for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
					for (int k = 0; k < this.getTablePlayerNumber(); k++) {
						lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
					}
				}

				for (int j = 0; j < this.getTablePlayerNumber(); j++) {
					_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
				}
			}

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				GRR._game_score[i] += lGangScore[i];
				GRR._game_score[i] += GRR._start_hu_score[i];
				// 跟庄分数
				if (shang_ju_zhuang != i && has_rule(GameConstants_PC.GAME_RULE_SHAO_ZHUANG)) {
					GRR._game_score[i] += genZhuangCount;
					GRR._game_score[shang_ju_zhuang] -= genZhuangCount;
					genZhuangScore[i] += genZhuangCount;
					genZhuangScore[shang_ju_zhuang] -= genZhuangCount;
				}
				_player_result.game_score[i] += GRR._game_score[i];
			}
			shang_ju_zhuang = _cur_banker;
			this.load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);

			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);
			game_end.setLeftCardCount(GRR._left_card_count);
			game_end.setShowBirdEffect(1);

			//for (int i = 0; i < GameConstants.MAX_NIAO_CARD; i++) {
			for (int i = 0; i <get_ma_num(); i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao[i]);
			}
			/*for (int i = 0; i < GameConstants.MAX_NIAO_CARD ; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao_fei[i]);
			}*/
			game_end.setCountPickNiao(GRR._count_pick_niao + GRR._count_pick_niao_fei);// 中鸟个数

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				// 鸟牌，必须按四人计算
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_NIAO_CARD; j++) {
					pnc.addItem(GRR.get_player_niao_cards()[i][j]);
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
		} else if ((!is_sys()) && (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM)) {
			end = true;
			real_reason = GameConstants.Game_End_DRAW;
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(this.process_player_result(reason));
		}
		game_end.setEndType(real_reason);

		roomResponse.setGameEnd(game_end);

		this.send_response_to_room(roomResponse);

		record_game_round(game_end);

		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT || reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
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

		return false;
	
	}
	
	@Override
	protected boolean on_handler_game_finish(int seat_index, int reason) {
		/*this.set_handler(this._handler_piao);
		this._handler_piao.exe(this);*/
		return on_game_finish(seat_index,reason);
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
					if (type == GameConstants_PC.CHR_TIAN_HU) {
						result.append(" 天胡");
					}
					if (type == GameConstants_PC.CHR_DI_HU) {
						result.append(" 地胡");
					}
					if (type == GameConstants_PC.CHR_ZI_MO) {
						result.append(" 自摸");
					}
					if (type == GameConstants_PC.CHR_HUNAN_QIANG_GANG_HU) {
						qiang_gang_hu = true;
						result.append(" 抢杠胡");
					}
					if (type == GameConstants_PC.CHR_SHU_FAN) {
						result.append(" 接炮");
					}
					if (type == GameConstants_PC.CHR_QI_XIAO_DUI) {
						result.append(" 七小对");
					}
					if (type == GameConstants_PC.CHR_QI_DA_DUI) {
						result.append(" 七大对");
					}
					if (type == GameConstants_PC.CHR_QING_YI_SE) {
						result.append(" 清一色");
					}
					if (type == GameConstants_PC.CHR_HH_QI_XIAO_DUI) {
						result.append(" 豪华七小对");
					}
					if (type == GameConstants_PC.CHR_QING_YI_SE_QI_XIAO_DUI) {
						result.append(" 清一色七小对");
					}
					if (type == GameConstants_PC.CHR_QING_YI_SE_QI_DA_DUI) {
						result.append(" 清一色七大对");
					}
					if (type == GameConstants_PC.CHR_QING_YI_SE_HH_QI_XIAO_DUI) {
						result.append(" 清一色豪华七小对");
					}
					if (type == GameConstants_PC.CHR_PING_HU) {
						result.append(" 平胡");
					}
					
				} else if (type == GameConstants_PC.CHR_FANG_PAO) {
					if (qiang_gang_hu) {
						result.append("被抢杠");
					} else {
						result.append(" 放炮");
					}
				}
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

			GRR._result_des[player] += result.toString();
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
		//初始化鸟牌数组
		GRR._cards_data_niao=new int[get_ma_num()];
		for (int i = 0; i <get_ma_num(); i++) {
			GRR._cards_data_niao[i] = GameConstants.INVALID_VALUE;
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			GRR._player_niao_count[i] = 0;
			for (int j = 0; j < GameConstants.MAX_NIAO_CARD; j++) {
				GRR._player_niao_cards[i][j] = GameConstants.INVALID_VALUE;
			}
		}
		//GRR._count_niao 总飞鸟数
		int zhuang_niao=0;
		int every_niao=0;
		if(has_rule(GameConstants_PC.GAME_RULE_FEI_ZHUANG_1)) {
			zhuang_niao=1;
		}
		if(has_rule(GameConstants_PC.GAME_RULE_FEI_ZHUANG_2)) {
			zhuang_niao=2;
		}
		if(has_rule(GameConstants_PC.GAME_RULE_FEI_EVERY_1)) {
			every_niao=1;
		}
		if(has_rule(GameConstants_PC.GAME_RULE_FEI_EVERY_2)) {
			every_niao=2;
		}
		if(has_rule(GameConstants_PC.GAME_RULE_FEI_EVERY_4)) {
			every_niao=4;
		}
		if(zhuang_niao>0) {
			for(int i = 0; i < this.getTablePlayerNumber(); i++) {
				if(i==_cur_banker) {
					GRR._player_niao_count[i] = zhuang_niao;
					this._send_card_count+=zhuang_niao;
					this.GRR._left_card_count-=zhuang_niao;
					int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
					_logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count, GRR._player_niao_count[i], cbCardIndexTemp);
					_logic.switch_to_cards_data(cbCardIndexTemp, GRR._cards_data_niao);
					GRR._player_niao_cards[i]=GRR._cards_data_niao;
				}
			}
		}else if(every_niao>0) {
			this._send_card_count+=every_niao*getTablePlayerNumber();
			this.GRR._left_card_count-=every_niao*getTablePlayerNumber();
			int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
			_logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count, every_niao*getTablePlayerNumber(), cbCardIndexTemp);
			_logic.switch_to_cards_data(cbCardIndexTemp, GRR._cards_data_niao);
			for(int i = 0; i < this.getTablePlayerNumber(); i++) {
				GRR._player_niao_count[i] = every_niao;
				for(int j=0;j<every_niao;j++) {
					GRR._player_niao_cards[i][j]=GRR._cards_data_niao[i*every_niao+j];
				}
			}
		
		}
		
		
		/*if(has_rule(GameConstants_PC.GAME_RULE_FEI_ZHUANG_1)) {
			for(int i = 0; i < this.getTablePlayerNumber(); i++) {
				if(i==_cur_banker) {
					GRR._player_niao_count[i] = 1;
					this._send_card_count++;
					GRR._cards_data_niao[0]=this._repertory_card[this._all_card_len - this.GRR._left_card_count];
					--this.GRR._left_card_count;
					GRR._player_niao_cards[i][0]=GRR._cards_data_niao[0];
				}
			}
		}else if(has_rule(GameConstants_PC.GAME_RULE_FEI_ZHUANG_2)) {
			for(int i = 0; i < this.getTablePlayerNumber(); i++) {
				if(i==_cur_banker) {
					GRR._player_niao_count[i] = 2;
					this._send_card_count+=2;
					this.GRR._left_card_count-=2;
					int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
					_logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count, GRR._player_niao_count[i], cbCardIndexTemp);
					GRR._left_card_count -= GRR._count_niao;
					_logic.switch_to_cards_data(cbCardIndexTemp, GRR._cards_data_niao);
					GRR._player_niao_cards[i]=GRR._cards_data_niao;
				}
			}
		}else if(has_rule(GameConstants_PC.GAME_RULE_FEI_EVERY_1)) {
			this._send_card_count+=1*getTablePlayerNumber();
			this.GRR._left_card_count-=1*getTablePlayerNumber();
			int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
			_logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count, 1*getTablePlayerNumber(), cbCardIndexTemp);
			GRR._left_card_count -= GRR._count_niao;
			_logic.switch_to_cards_data(cbCardIndexTemp, GRR._cards_data_niao);
			for(int i = 0; i < this.getTablePlayerNumber(); i++) {
				GRR._player_niao_count[i] = 1;
				GRR._player_niao_cards[i][0]=GRR._cards_data_niao[i];
			}
		}else if(has_rule(GameConstants_PC.GAME_RULE_FEI_EVERY_2)) {
			this._send_card_count+=1*getTablePlayerNumber();
			this.GRR._left_card_count-=1*getTablePlayerNumber();
			int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
			_logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count, 2*getTablePlayerNumber(), cbCardIndexTemp);
			GRR._left_card_count -= GRR._count_niao;
			_logic.switch_to_cards_data(cbCardIndexTemp, GRR._cards_data_niao);
			for(int i = 0; i < this.getTablePlayerNumber(); i++) {
				GRR._player_niao_count[i] = 2;
				GRR._player_niao_cards[i][0]=GRR._cards_data_niao[i*2];
				GRR._player_niao_cards[i][1]=GRR._cards_data_niao[i*2+1];
			}
		}else if(has_rule(GameConstants_PC.GAME_RULE_FEI_EVERY_4)) {
			this._send_card_count+=1*getTablePlayerNumber();
			this.GRR._left_card_count-=1*getTablePlayerNumber();
			int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
			_logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count, 4*getTablePlayerNumber(), cbCardIndexTemp);
			GRR._left_card_count -= GRR._count_niao;
			_logic.switch_to_cards_data(cbCardIndexTemp, GRR._cards_data_niao);
			for(int i = 0; i < this.getTablePlayerNumber(); i++) {
				GRR._player_niao_count[i] = 4;
				GRR._player_niao_cards[i][0]=GRR._cards_data_niao[i*2];
				GRR._player_niao_cards[i][1]=GRR._cards_data_niao[i*2+1];
				GRR._player_niao_cards[i][2]=GRR._cards_data_niao[i*2+2];
				GRR._player_niao_cards[i][3]=GRR._cards_data_niao[i*2+3];
			}
		}*/

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

		if (val == true) {
			// 生效
			return (card_data < GameConstants.DING_NIAO_VALID ? card_data + GameConstants.DING_NIAO_VALID : card_data);
		} else {
			return (card_data < GameConstants.DING_NIAO_INVALID ? card_data + GameConstants.DING_NIAO_INVALID : card_data);
		}
	}

	@Override
	public boolean handler_requst_pao_qiang(Player player, int pao, int qiang) {
		return _handler_piao.handler_pao_qiang(this, player.get_seat_index(), pao, qiang);
	}


	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * 
	 * @return
	 */
	public boolean estimate_gang_respond(int seat_index, int card) {

		if (has_rule(GameConstants_PC.GAME_RULE_ZI_MO)) {
			return false;
		}

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
				boolean can_hu_this_card = true;
				int[] tmp_cards_data_hu = _playerStatus[i].get_cards_abandoned_hu();
				for (int x = 0; x < GameConstants_PC.MAX_ABANDONED_CARDS_COUNT; x++) {
					if (tmp_cards_data_hu[x] != 0) {
						can_hu_this_card = false;
						break;
					}
				}
				if (!can_hu_this_card) {
					continue;
				}

				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						GameConstants_PC.HU_CARD_TYPE_QIANG_GANG, i);

				// 结果判断
				if (action != 0) {
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

	@Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x01, 0x07, 0x14, 0x17, 0x21, 0x24, 0x35, 0x31, 0x32, 0x33, 0x34, 0x36, 0x37 };
		int[] cards_of_player1 = new int[] { 0x01, 0x04, 0x07, 0x11, 0x14, 0x17, 0x21, 0x24, 0x27, 0x31, 0x32, 0x33, 0x34 };
		int[] cards_of_player2 = new int[] { 0x01, 0x01, 0x02, 0x02, 0x03, 0x03, 0x04, 0x04, 0x09, 0x21, 0x21, 0x23, 0x23 };
		int[] cards_of_player3 = new int[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x11, 0x12, 0x13, 0x11 };

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
			} else if (this.getTablePlayerNumber() == 2) {
				GRR._cards_index[0][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[1][_logic.switch_to_card_index(cards_of_player1[j])] += 1;
			}
		}
		
		//this.BACK_DEBUG_CARDS_MODE=true;
		//debug_my_cards=new int[] {53, 22, 24, 34, 34, 25, 5, 53, 18, 40, 1, 24, 38, 21, 6, 41, 22, 38, 35, 19, 3, 21, 17, 8, 40, 4, 24, 23, 38, 7, 37, 22, 39, 53, 33, 36, 9, 3, 1, 33, 6, 41, 19, 7, 37, 23, 2, 22, 20, 20, 3, 19, 37, 4, 34, 21, 38, 20, 6, 9, 3, 36, 23, 17, 5, 5, 2, 33, 18, 39, 40, 35, 23, 21, 35, 24, 1, 33, 41, 6, 1, 2, 35, 8, 5, 41, 9, 7, 25, 37, 8, 19, 53, 25, 4, 34, 40, 8, 17, 4, 18, 20, 36, 2, 9, 36, 18, 39, 17, 7, 39, 25};

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
	
	public boolean only_gang(WeaveItem weaveItem[], int cbWeaveCount) {
		for (int i = 0; i < cbWeaveCount; i++) {
			if (weaveItem[i].weave_kind != GameConstants.WIK_GANG)
				return false;
		}
		return true;
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		// TODO Auto-generated method stub
		
	}
}
