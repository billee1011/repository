package com.cai.game.mj.chenchuang.quanzhou;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.Constants_MJ_QUAN_ZHOU;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GameRoundRecord;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.RandomUtil;
import com.cai.future.GameSchedule;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJType;
import com.cai.game.mj.ThreeDimension;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.game.util.GameUtilConstants;
import com.cai.service.PlayerServiceImpl;

/**
 * chenchuang
 * 浦城麻将
 */
public class Table_QuanZhou extends AbstractMJTable {

	private static final long serialVersionUID = 1L;
	
	public int[] an_gang_count = new int[getTablePlayerNumber()];
	public int[] ming_gang_count = new int[getTablePlayerNumber()];
	public int[] hua_count = new int[getTablePlayerNumber()];
	public int[] gold_count = new int[getTablePlayerNumber()];
	public int[] swim_status = new int[getTablePlayerNumber()];
	
	public Table_QuanZhou() {
		super(MJType.GAME_TYPE_MJ_QUAN_ZHOU);
	}

	@Override
	protected void onInitTable() {
		_handler_chi_peng = new HandlerChiPeng_QuanZhou();
		_handler_dispath_card = new HandlerDispatchCard_QuanZhou();
		_handler_gang = new HandlerGang_QuanZhou();
		_handler_out_card_operate = new HandlerOutCardOperate_QuanZhou();
	}

	public void exe_select_magic(){
		_send_card_count++;
		int gold_card = 0;
		for(int i = GRR._left_card_count; i > 0; i--){
			gold_card = _repertory_card[_all_card_len - i];
			if(gold_card < 0x38){
				_repertory_card[_all_card_len - i] = _repertory_card[_all_card_len - GRR._left_card_count];
				break;
			}
		}
        GRR._left_card_count--;

        if (DEBUG_CARDS_MODE)
        	gold_card = 0x07;

        // 将翻出来的牌显示在牌桌的正中央
        operate_show_card(_cur_banker, GameConstants.Show_Card_Center, 1, new int[] { gold_card },
                GameConstants.INVALID_SEAT);
        //operate_effect_action(-1, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { Constants_MJ_QUAN_ZHOU.ACTION_KAI_JIN }, 1, GameConstants.INVALID_SEAT);

        // 添加鬼
        _logic.add_magic_card_index(_logic.switch_to_card_index(gold_card));
        GRR._especial_card_count = 1;
        GRR._especial_show_cards[0] = gold_card + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;

        // 处理每个玩家手上的牌，如果有王牌，处理一下
        for (int i = 0; i < getTablePlayerNumber(); i++) {
            int[] hand_cards = new int[Constants_MJ_QUAN_ZHOU.MAX_HAND_CARD_COUNT];
            if(i == _cur_banker)
            	GRR._cards_index[i][_logic.switch_to_card_index(_send_card_data)]--;
            int hand_card_count = switch_to_cards_data(GRR._cards_index[i], hand_cards);
            for (int j = 0; j < hand_card_count; j++) {
                if (_logic.is_magic_card(hand_cards[j])) {
                    hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
                }
            }
            // 玩家客户端刷新一下手牌
            operate_player_cards(i, hand_card_count, hand_cards, 0, null);
        }

        GameSchedule.put(new Runnable() {
            @Override
            public void run() {
                // 将翻出来的牌从牌桌的正中央移除
                operate_show_card(_cur_banker, GameConstants.Show_Card_Center, 0, null, GameConstants.INVALID_SEAT);
            }
        }, 2000, TimeUnit.MILLISECONDS);
	}
	
	/**第一局建房者为庄家，若为代开房/俱乐部开房（即开房者自己未进入牌局），则随机一位为庄家;*/
	@Override
	protected void initBanker() {
		_cur_banker = RandomUtil.getRandomNumber(getTablePlayerNumber());
	}
	
	@Override
	public void shuffle(int repertory_card[], int mj_cards[]) {
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

		int send_count;
		int have_send_count = 0;

		if (this.getClass().getAnnotation(ThreeDimension.class) != null) {
			show_tou_zi(GRR._banker_player);
		}

		int count = getTablePlayerNumber();

		// 分发扑克
		for (int i = 0; i < count; i++) {
			send_count = (Constants_MJ_QUAN_ZHOU.MAX_HAND_CARD_COUNT - 1);
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
	
	/**
	 * 处理一些值的清理
	 * 
	 * @return
	 */
	@Override
	public boolean reset_init_data() {
		if (_cur_round == 0) {

			this.initBanker();
			record_game_room();
		}

		virtual_online = new boolean[GameConstants.GAME_PLAYER];
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			virtual_online[i] = true;
		}

		gang_dispatch_count = 0;

		_last_dispatch_player = -1;

		_card_can_not_out_after_chi = new int[this.getTablePlayerNumber()];
		_chi_pai_count = new int[this.getTablePlayerNumber()][this.getTablePlayerNumber()];

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

		GRR = new GameRoundRecord(Constants_MJ_QUAN_ZHOU.MAX_WEAVE, Constants_MJ_QUAN_ZHOU.MAX_HAND_CARD_COUNT, GameConstants.MAX_INDEX);
		GRR.setRoom(this);
		GRR._start_time = System.currentTimeMillis() / 1000L;
		GRR._game_type_index = _game_type_index;
		GRR._cur_round = _cur_round;
		_end_reason = GameConstants.INVALID_VALUE;
		istrustee = new boolean[4];
		// 新建
		_playerStatus = new PlayerStatus[4];
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			_playerStatus[i] = new PlayerStatus(Constants_MJ_QUAN_ZHOU.MAX_HAND_CARD_COUNT);
		}

		// _cur_round=8;
		_cur_round++;

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			_playerStatus[i].reset();
		}

		// 牌局回放
		GRR._room_info.setRoomId(this.getRoom_id());
		GRR._room_info.setGameRuleIndex(_game_rule_index);
		GRR._room_info.setGameRuleDes(get_game_des());
		GRR._room_info.setGameTypeIndex(_game_type_index);
		GRR._room_info.setGameRound(_game_round);
		GRR._room_info.setCurRound(_cur_round);
		GRR._room_info.setGameStatus(_game_status);
		GRR._room_info.setCreatePlayerId(this.getRoom_owner_account_id());

		// TODO 设置房间的xml玩法信息
		if (commonGameRuleProtos != null) {
			GRR._room_info.setNewRules(commonGameRuleProtos);
		}

		Player rplayer;
		// WalkerGeek 允许少人模式
		// for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
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
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			GRR._video_recode.addPlayers(room_player);
		}

		GRR._video_recode.setBankerPlayer(this._cur_banker);

		return true;
	}


	/**
	 * 洗完牌执行开始
	 */
	@Override
	protected boolean on_game_start() {
		//初始化常量
		an_gang_count = new int[getTablePlayerNumber()];
		ming_gang_count = new int[getTablePlayerNumber()];
		hua_count = new int[getTablePlayerNumber()];
		gold_count = new int[getTablePlayerNumber()];
		swim_status = new int[getTablePlayerNumber()];
		//选鬼
		_logic.clean_magic_cards();
		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[getTablePlayerNumber()][Constants_MJ_QUAN_ZHOU.MAX_HAND_CARD_COUNT];

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int hand_card_count = switch_to_cards_data(GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}
		//发送给玩家手牌
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			gameStartResponse.clearCardData();
			for (int j = 0; j < Constants_MJ_QUAN_ZHOU.MAX_HAND_CARD_COUNT; j++) {
				if (_logic.is_magic_card(hand_cards[i][j])) {
					hand_cards[i][j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
                }
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			load_room_info_data(roomResponse);
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

			for (int j = 0; j < Constants_MJ_QUAN_ZHOU.MAX_HAND_CARD_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}
		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(GRR._left_card_count);
		GRR.add_room_response(roomResponse);

		//发第一张牌
		exe_dispatch_card(_current_player, GameConstants.GANG_TYPE_HONG_ZHONG, GameConstants.DELAY_SEND_CARD_DELAY);

		return true;
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {
		if (card_type == Constants_MJ_QUAN_ZHOU.HU_CARD_TYPE_ZI_MO) {
			chiHuRight.opr_or(Constants_MJ_QUAN_ZHOU.CHR_ZI_MO);//自摸
		} else if (card_type == Constants_MJ_QUAN_ZHOU.HU_CARD_TYPE_QIANG_GANG) {
			chiHuRight.opr_or(Constants_MJ_QUAN_ZHOU.CHR_QIANG_GANG);//抢杠胡
		}else if (card_type == Constants_MJ_QUAN_ZHOU.HU_CARD_TYPE_DIAN_PAO) {
			chiHuRight.opr_or(Constants_MJ_QUAN_ZHOU.CHR_DIAN_PAO_HU);//点炮胡
		}
		//拷贝把当前牌加入手牌
		int cur_card_index = _logic.switch_to_card_index(cur_card);
		int[] temp_cards_index = Arrays.copyOf(cards_index, cards_index.length);
		temp_cards_index[cur_card_index]++;
		
		if(cards_index[_logic.get_magic_card_index(0)] > 0 && (card_type == Constants_MJ_QUAN_ZHOU.HU_CARD_TYPE_DIAN_PAO || card_type == Constants_MJ_QUAN_ZHOU.HU_CARD_TYPE_QIANG_GANG))
				return GameConstants.WIK_NULL;
		
		//得到癞子牌的个数，和索引
		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
        int magic_card_count = _logic.get_magic_card_count();
        if (magic_card_count > 2) { // 一般只有两种癞子牌存在
            magic_card_count = 2;
        }
        for (int i = 0; i < magic_card_count; i++) {
            magic_cards_index[i] = _logic.get_magic_card_index(i);
        }
        //是否胡牌牌型
  		boolean analyse_win_by_cards_index = analyse_can_hu_17(temp_cards_index, magic_cards_index, magic_card_count);
  		if(!analyse_win_by_cards_index){
  			return GameConstants.WIK_NULL;
  		}
		return GameConstants.WIK_CHI_HU;
	}
	

	private boolean analyse_can_hu_17(int[] cards_index,
			int[] magic_cards_index, int magic_card_count) {
		int tmp_cards[] = new int[Constants_MJ_QUAN_ZHOU.MAX_HAND_CARD_COUNT];
		int tmp_hand_card_count = switch_to_cards_data(cards_index, tmp_cards);
		if(tmp_hand_card_count != 17){
			return AnalyseCardUtil.analyse_win_by_cards_index(cards_index, -1, magic_cards_index, magic_card_count);
		}else{
			int[] temp_cards_index = Arrays.copyOf(cards_index, cards_index.length);
			int magic_index = _logic.get_magic_card_index(0);
			temp_cards_index[magic_index] = 0;
			for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
				if(temp_cards_index[i] >= 3){
					cards_index[i] -= 3;
					boolean analyse_win_by_cards_index = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, -1, magic_cards_index, magic_card_count);
					if(analyse_win_by_cards_index)
						return true;
					cards_index[i] += 3;
				}
			}
			for (int i = 0; i < GameConstants.MAX_ZI; i++) {
				int value = _logic.get_card_value(_logic.switch_to_card_data(i));
				if(value == 1){
					if(temp_cards_index[i] > 0 && temp_cards_index[i + 1] > 0 && temp_cards_index[i + 2] > 0){
						cards_index[i]--;
						cards_index[i + 1]--;
						cards_index[i + 2]--;
						boolean analyse_win_by_cards_index = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, -1, magic_cards_index, magic_card_count);
						if(analyse_win_by_cards_index)
							return true;
						cards_index[i]++;
						cards_index[i + 1]++;
						cards_index[i + 2]++;
					}
				}else if(value == 9){
					if(temp_cards_index[i] > 0 && temp_cards_index[i - 1] > 0 && temp_cards_index[i - 2] > 0){
						cards_index[i]--;
						cards_index[i - 1]--;
						cards_index[i - 2]--;
						boolean analyse_win_by_cards_index = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, -1, magic_cards_index, magic_card_count);
						if(analyse_win_by_cards_index)
							return true;
						cards_index[i]++;
						cards_index[i - 1]++;
						cards_index[i - 2]++;
					}
				}else{
					if(temp_cards_index[i] > 0 && temp_cards_index[i - 1] > 0 && temp_cards_index[i + 1] > 0){
						cards_index[i]--;
						cards_index[i - 1]--;
						cards_index[i + 1]--;
						boolean analyse_win_by_cards_index = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, -1, magic_cards_index, magic_card_count);
						if(analyse_win_by_cards_index)
							return true;
						cards_index[i]++;
						cards_index[i - 1]++;
						cards_index[i + 1]++;
					}
				}
			}
		}
		return false;
	}

	/**
	 * 判断抢杠胡
	 * @return
	 */
	public boolean estimate_gang_respond(int seat_index, int card) {
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
			//if ( _playerStatus[i].is_chi_hu_round()) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						Constants_MJ_QUAN_ZHOU.HU_CARD_TYPE_QIANG_GANG, i);
				// 结果判断
				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
					bAroseAction = true;
				}
			//}
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
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		countCardType(chr, seat_index);
		
		int multiple= getPaiXingScore(chr);
		////////////////////////////////////////////////////// 自摸 算分
		int gold_score = GRR._cards_index[seat_index][_logic.get_magic_card_index(0)];
		if(zimo && _logic.is_magic_card(_provide_card))
			gold_score++ ;
		gold_count[seat_index] = gold_score;
		int hua_score = hua_count[seat_index] > 1 ? hua_count[seat_index] - 1 : 0;
		
		int basic_score = ming_gang_count[seat_index] + an_gang_count[seat_index] * 2 + hua_score + gold_score + 1;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if(i == seat_index)
				continue;
			int score = basic_score;
			if(seat_index == GRR._banker_player || i == GRR._banker_player)
				score++;
			GRR._game_score[seat_index] += score * multiple;
			GRR._game_score[i] -= score * multiple;
		}
		// 设置变量
		GRR._provider[seat_index] = provide_index;
		_status_gang = false;
		_status_gang_hou_pao = false;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);
	}


	private int getPaiXingScore(ChiHuRight chr) {
		if(!chr.opr_and(Constants_MJ_QUAN_ZHOU.CHR_THREE_SWIM).is_empty())
			return 16;
		if(!chr.opr_and(Constants_MJ_QUAN_ZHOU.CHR_TWO_SWIM).is_empty())
			return 8;
		if(!chr.opr_and(Constants_MJ_QUAN_ZHOU.CHR_SWIN_GOLD).is_empty())
			return 4;
		if(!chr.opr_and(Constants_MJ_QUAN_ZHOU.CHR_THREE_GOLD_INVERTED).is_empty())
			return 4;
		if(!chr.opr_and(Constants_MJ_QUAN_ZHOU.CHR_ZI_MO).is_empty())
			return 2;
		return 1;
	}

	@Override
	protected void set_result_describe() {
		int chrTypes;
		long type = 0;
		for (int player = 0; player < this.getTablePlayerNumber(); player++) {
			StringBuilder result = new StringBuilder("");

			chrTypes = GRR._chi_hu_rights[player].type_count;

			for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
				type = GRR._chi_hu_rights[player].type_list[typeIndex];

				if (GRR._chi_hu_rights[player].is_valid()) {
					if (type == Constants_MJ_QUAN_ZHOU.CHR_ZI_MO) {
						result.append(" 自摸");
					}
					if (type == Constants_MJ_QUAN_ZHOU.CHR_QIANG_GANG) {
						result.append(" 抢杠胡");
					}
					if (type == Constants_MJ_QUAN_ZHOU.CHR_DIAN_PAO_HU) {
						result.append(" 接炮");
					}
					if (type == Constants_MJ_QUAN_ZHOU.CHR_SWIN_GOLD) {
						result.append(" 游金");
					}
					if (type == Constants_MJ_QUAN_ZHOU.CHR_TWO_SWIM) {
						result.append(" 双游");
					}
					if (type == Constants_MJ_QUAN_ZHOU.CHR_THREE_SWIM) {
						result.append(" 三游");
					}
					if (type == Constants_MJ_QUAN_ZHOU.CHR_THREE_GOLD_INVERTED) {
						result.append(" 三金倒");
					}
				} else if (type == Constants_MJ_QUAN_ZHOU.CHR_FANG_PAO) {
					result.append(" 放炮");
				} else if (type == Constants_MJ_QUAN_ZHOU.CHR_BEI_QIANG_GANG) {
					result.append("被抢杠");
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
			if (fang_gang > 0) {
				result.append(" 放杠X" + fang_gang);
			}
			if (jie_gang + ming_gang > 0) {
				result.append(" 明杠X" + (jie_gang + ming_gang));
			}
			
			if(gold_count[player] > 0)
				result.append(" 金X" + gold_count[player]);
			if(hua_count[player] > 1)
				result.append(" 花X" + hua_count[player]);
			
			GRR._result_des[player] = result.toString();
		}
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

			playerStatus = _playerStatus[i];
			if (GRR._left_card_count > 16) {//如果牌堆已经没有牌，出的最后一张牌了不能碰，杠
				if (i == get_banker_next_seat(seat_index) && !is_swim_ban()) {
					action = _logic.check_chi(GRR._cards_index[i], card);
	
					if ((action & GameConstants.WIK_RIGHT) != 0) {
						_playerStatus[i].add_action(GameConstants.WIK_RIGHT);
						_playerStatus[i].add_chi(card, GameConstants.WIK_RIGHT, seat_index);
					}
					if ((action & GameConstants.WIK_CENTER) != 0) {
						_playerStatus[i].add_action(GameConstants.WIK_CENTER);
						_playerStatus[i].add_chi(card, GameConstants.WIK_CENTER, seat_index);
					}
					if ((action & GameConstants.WIK_LEFT) != 0) {
						_playerStatus[i].add_action(GameConstants.WIK_LEFT);
						_playerStatus[i].add_chi(card, GameConstants.WIK_LEFT, seat_index);
					}
	
					if (_playerStatus[i].has_action()) {
						bAroseAction = true;
					}
				}

				boolean can_peng = true;
				int[] tmp_cards_data = _playerStatus[i].get_cards_abandoned_peng();
				for (int x = 0; x < GameConstants.MAX_ABANDONED_CARDS_COUNT; x++) {
					if (tmp_cards_data[x] == card) {
						can_peng = false;
						break;
					}
				}
			
				action = _logic.check_peng(GRR._cards_index[i], card);
				if (can_peng && action != 0 && !is_swim_ban()) {
					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);
					bAroseAction = true;
				}
			}
			action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
			if (action != 0) {
				playerStatus.add_action(GameConstants.WIK_GANG);
				playerStatus.add_gang(card, i, 1);
				bAroseAction = true;
			}
			
			//if (_playerStatus[i].is_chi_hu_round()) {
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				int card_type = Constants_MJ_QUAN_ZHOU.HU_CARD_TYPE_DIAN_PAO;
				action = analyse_chi_hu_card(GRR._cards_index[i],
						GRR._weave_items[i], cbWeaveCount, card, chr,
						card_type, i);
				if (action != 0 && !is_swim_ban()) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);
					bAroseAction = true;
				}
			//}

		}

		if (bAroseAction) {
			_resume_player = _current_player;
			_current_player = GameConstants.INVALID_SEAT;
			_provide_player = seat_index;
		} else {
			return false;
		}

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

		for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					GameConstants.CHR_ZI_MO, seat_index)) {
				cards[count++] = cbCurrentCard;
			}
		}
		
		//全听
		if (count >= 34) {
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		return false;
	}
	
	
	
	/**
	 * 混一色判断
	 * 
	 * @return
	 */
	public boolean is_hun_yi_se(int cards_index[], WeaveItem weaveItem[], int weaveCount) {
		if (_logic.get_se_count(cards_index, weaveItem, weaveCount) != 1) {
			return false;
		}

		if (_logic.has_feng_pai(cards_index, weaveItem, weaveCount) == false) {
			return false;
		}
		return true;
	}
	
	
	public boolean is_yao_jiu(int[] hand_indexs, WeaveItem weaveItem[], int weaveCount) {
    	// 一九判断
		for (int i = 0; i < GameConstants.MAX_ZI_FENG; i ++) {
			if(hand_indexs[i] == 0)
				continue;
			if(_logic.is_magic_index(i))
				continue;
			if(i >= GameConstants.MAX_ZI)
        		return false;
			// 无效判断
			int value = _logic.get_card_value(_logic.switch_to_card_data(i));
			if (value != 1 && value != 9)
				return false;
		}
    	
		// 落地牌都是19
        for (int i = 0; i < weaveCount; i++) {
        	if(_logic.switch_to_card_index(weaveItem[i].center_card) >= GameConstants.MAX_ZI)
        		return false;
            int value = _logic.get_card_value(weaveItem[i].center_card);
            if (value != 1 && value != 9)
				return false;
        }
        return true;
    }
    
    // 杠牌分析 包括补杠 check_weave检查补杠
    public int analyse_gang(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount,
            GangCardResult gangCardResult, boolean check_weave, int cards_abandoned_gang[]) {
        // 设置变量
        int cbActionMask = GameConstants.WIK_NULL;

        // 手上杠牌
        for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
            if (cards_index[i] == 4 && !_logic.is_magic_index(i)) {
                cbActionMask |= GameConstants.WIK_GANG;
                int index = gangCardResult.cbCardCount++;
                gangCardResult.cbCardData[index] = _logic.switch_to_card_data(i);
                gangCardResult.isPublic[index] = 0;// 暗刚
                gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
            }
        }

        if (check_weave) {
            // 组合杠牌，包括以前能杠，但是不杠，发牌之后而选择补杠的
            for (int i = 0; i < cbWeaveCount; i++) {
                if (WeaveItem[i].weave_kind == GameConstants.WIK_PENG) {
                    for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
                        if (cards_index[j] != 1) {
                            continue;
                        } else if(WeaveItem[i].is_vavild && cards_abandoned_gang[j] != 1){
                            if (WeaveItem[i].center_card == _logic.switch_to_card_data(j)) {
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

        return cbActionMask;
    }
    
    /**
     * 处理牌桌结束逻辑
     * 
     * @param seat_index
     * @param reason
     */
    protected boolean on_handler_game_finish(int seat_index, int reason) {
        int real_reason = reason;

        for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
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
            if (this.is_mj_type(GameConstants.GAME_TYPE_HUNAN_XIANG_TAN)
                    || this.is_mj_type(GameConstants.GAME_TYPE_HUNAN_XIANG_TAN_DT)) {
                for (int i = 0; i < GRR._especial_card_count; i++) {
                    game_end.addEspecialShowCards(
                            GRR._especial_show_cards[i] + GameConstants.CARD_ESPECIAL_TYPE_DING_GUI);
                }
            } else {
                for (int i = 0; i < GRR._especial_card_count; i++) {
                    game_end.addEspecialShowCards(GRR._especial_show_cards[i]);
                }
            }

            GRR._end_type = reason;

            // 杠牌，每个人的分数
            float lGangScore[] = new float[getTablePlayerNumber()];
            //boolean liu_ju = reason == GameConstants.Game_End_DRAW || reason == GameConstants.Game_End_RELEASE_PLAY;
            if(reason == GameConstants.Game_End_NORMAL)
            	set_eat_basic_score();
            
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
                for (int j = 0; j < Constants_MJ_QUAN_ZHOU.MAX_HAND_CARD_COUNT; j++) {
                	if (_logic.is_magic_card(GRR._chi_hu_card[i][j])) {
						hc.addItem(GRR._chi_hu_card[i][j]
								+ GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
					} else {
						hc.addItem(GRR._chi_hu_card[i][j]);
					}
                }
                
				if (_logic.is_magic_card(GRR._chi_hu_card[i][0])) {
					game_end.addHuCardData(GRR._chi_hu_card[i][0]
							+ GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
				} else {
					game_end.addHuCardData(GRR._chi_hu_card[i][0]);
				}

                game_end.addHuCardArray(hc);
            }

            // 现在权值只有一位
            long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

            // 设置胡牌描述
            this.set_result_describe();

            for (int i = 0; i < getTablePlayerNumber(); i++) {
                GRR._card_count[i] = switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

                Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
                for (int j = 0; j < GRR._card_count[i]; j++) {
                	if (_logic.is_magic_card(GRR._cards_data[i][j])) {
						cs.addItem(GRR._cards_data[i][j]
								+ GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
					} else {
						cs.addItem(GRR._cards_data[i][j]);
					}
                }
                game_end.addCardsData(cs);// 牌

                // 组合
                WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
                for (int j = 0; j < Constants_MJ_QUAN_ZHOU.MAX_WEAVE; j++) {
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
        } else if ((!is_sys()) && (reason == GameConstants.Game_End_RELEASE_PLAY
                || reason == GameConstants.Game_End_RELEASE_NO_BEGIN || reason == GameConstants.Game_End_RELEASE_RESULT
                || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
                || reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT
                || reason == GameConstants.Game_End_RELEASE_SYSTEM)) {
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
        if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
                || reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
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
        for (int i = 0; i < getTablePlayerNumber(); i++) {
        	_player_result.biaoyan[i] = 0;
		}
        // 错误断言
        return false;
    }
    
	
	private void set_eat_basic_score() {
		int[] basic_score = new int[getTablePlayerNumber()];
		for(int i = 0; i < getTablePlayerNumber(); i++){
			if(GRR._chi_hu_rights[i].is_valid())
				continue;
			basic_score[i] += ming_gang_count[i];
			basic_score[i] += an_gang_count[i] * 2;
			if(hua_count[i] > 1)
				basic_score[i] += hua_count[i] - 1;
			if(GRR._cards_index[i][_logic.get_magic_card_index(0)] > 0)
				basic_score[i] += GRR._cards_index[i][_logic.get_magic_card_index(0)];
		}
		for(int i = 0; i < getTablePlayerNumber(); i++){
			if(GRR._chi_hu_rights[i].is_valid())
				continue;
			for(int j = 0; j < getTablePlayerNumber(); j++){
				if(!GRR._chi_hu_rights[j].is_valid()){
					GRR._game_score[i] += basic_score[i] - basic_score[j];
				}
			}
		}
		
		for (int i = 0; i < getTablePlayerNumber(); i++) {
            _player_result.game_score[i] += GRR._game_score[i];
        }
		
	}

		// 七小对牌 七小对：胡牌时，手上任意七对牌。
		public int is_qi_xiao_dui(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int cur_card) {

			// 组合判断
			if (cbWeaveCount != 0)
				return GameConstants.WIK_NULL;

			// 单牌数目
			int cbReplaceCount = 0;
			int cbReplaceCount3 = 0;
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
						if (cbCardCount == 1)
							cbReplaceCount++;
						if (cbCardCount == 3)
							cbReplaceCount3++;

						if (cbCardCount == 4) {
							nGenCount++;
						}
					}
				} else {
					// 单牌统计
					if (cbCardCount == 1)
						cbReplaceCount++;
					if (cbCardCount == 3)
						cbReplaceCount3++;

					if (cbCardCount == 4) {
						nGenCount++;
					}
				}
			}

			// 王牌不够
			int count = 0;
			if (_logic.get_magic_card_count() > 0) {
				for (int m = 0; m < _logic.get_magic_card_count(); m++) {
					count += cbCardIndexTemp[_logic.get_magic_card_index(m)];
				}

				if (cbReplaceCount + cbReplaceCount3 > count) {
					return GameConstants.WIK_NULL;
				}
				// //王牌不够
				// if( get_magic_card_index() != MJGameConstants.MAX_INDEX &&
				// cbReplaceCount > cbCardIndexTemp[get_magic_card_index()] ||
				// get_magic_card_index() == MJGameConstants.MAX_INDEX &&
				// cbReplaceCount > 0 )
				// return MJGameConstants.WIK_NULL;
			} else {
				if (cbReplaceCount + cbReplaceCount3 > 0)
					return GameConstants.WIK_NULL;
			}

			if (nGenCount > 0 || (count - cbReplaceCount -cbReplaceCount3) > 1 || (count > 0 && cbReplaceCount3 > 0)) {
				return GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI;
			} else {
				return GameConstants.CHR_HUNAN_QI_XIAO_DUI;
			}

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
	
	public void execute_flower_start(){
		int _seat_index = _cur_banker;
		ChiHuRight chr = GRR._chi_hu_rights[_seat_index];
		chr.set_empty();
		PlayerStatus curPlayerStatus = _playerStatus[_seat_index];
		curPlayerStatus.reset();

		// 判断是否是杠后抓牌,杠开花只算接杠
		int card_type = Constants_MJ_QUAN_ZHOU.HU_CARD_TYPE_ZI_MO;
		// 检查牌型,听牌
		int action = analyse_chi_hu_card(GRR._cards_index[_seat_index], GRR._weave_items[_seat_index],
				GRR._weave_count[_seat_index], _send_card_data, chr, card_type, _seat_index);
		if(GRR._cards_index[_seat_index][_logic.get_magic_card_index(0)] + (_logic.is_magic_card(_send_card_data)?1:0) == 3){
			curPlayerStatus.add_action(Constants_MJ_QUAN_ZHOU.WIK_SAN_JIN_DAO);
			curPlayerStatus.add_action_card(1, _send_card_data, Constants_MJ_QUAN_ZHOU.WIK_SAN_JIN_DAO, _seat_index);
		}else if (action != GameConstants.WIK_NULL) {
			if(swim_status[_seat_index] == 0){
				process_swim_out_card(_seat_index, _send_card_data);
				if(swim_status[_seat_index] > 0)
					swim_status[_seat_index] = 1;
			}
			curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
			curPlayerStatus.add_action_card(swim_status[_seat_index] + 1, _send_card_data, GameConstants.WIK_ZI_MO, _seat_index);
		} else {
			GRR._chi_hu_rights[_seat_index].set_empty();
			chr.set_empty();
		}
		GRR._cards_index[_seat_index][_logic.switch_to_card_index(_send_card_data)]++;
		// 出任意一张牌时，能胡哪些牌 -- Begin
		int count = 0;
		int ting_count = 0;
		int send_card_index = _logic.switch_to_card_index(_send_card_data);
		((HandlerDispatchCard_QuanZhou)_handler_dispath_card).ting_send_card = false;

		int card_type_count = GameConstants.MAX_ZI_FENG;

		for (int i = 0; i < card_type_count; i++) {
			count = GRR._cards_index[_seat_index][i];

			if (count > 0) {
				GRR._cards_index[_seat_index][i]--;
				// 打出哪些牌可以听牌，同时得到听牌的数量，把可以胡牌的数据data型放入table._playerStatus[_seat_index]._hu_out_cards[ting_count]
				_playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] = get_ting_card(
						_playerStatus[_seat_index]._hu_out_cards[ting_count], GRR._cards_index[_seat_index],
						GRR._weave_items[_seat_index], GRR._weave_count[_seat_index], _seat_index);

				if (_playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] > 0) {
					_playerStatus[_seat_index]._hu_out_card_ting[ting_count] = _logic.switch_to_card_data(i);

					ting_count++;

					if (send_card_index == i) {
						((HandlerDispatchCard_QuanZhou)_handler_dispath_card).ting_send_card = true;
					}
				}

				GRR._cards_index[_seat_index][i]++;
			}
		}

		_playerStatus[_seat_index]._hu_out_card_count = ting_count;

		if (ting_count > 0) {
			GRR._cards_index[_seat_index][send_card_index]--;

			int cards[] = new int[Constants_MJ_QUAN_ZHOU.MAX_HAND_CARD_COUNT];
			int hand_card_count = switch_to_cards_data(GRR._cards_index[_seat_index], cards);
			int[] copy_tmp_cards = Arrays.copyOf(cards, Constants_MJ_QUAN_ZHOU.MAX_HAND_CARD_COUNT);
			GRR._cards_index[_seat_index][send_card_index]++;

			for (int i = 0; i < hand_card_count; i++) {
				for (int j = 0; j < ting_count; j++) {
					if (cards[i] == _playerStatus[_seat_index]._hu_out_card_ting[j]) {
						cards[i] += GameConstants.CARD_ESPECIAL_TYPE_TING;
						break;
					}
				}
				if (_logic.is_magic_card(copy_tmp_cards[i])) {
					cards[i] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				}
			}

			operate_player_cards_with_ting(_seat_index, hand_card_count, cards, 0, null);
		}
		// 出任意一张牌时，能胡哪些牌 -- End

		int show_send_card = _send_card_data;
		if(swim_status[_seat_index] > 0){
			show_send_card += 10000 + swim_status[_seat_index] * 1000;
		}else if (((HandlerDispatchCard_QuanZhou)_handler_dispath_card).ting_send_card) {
			show_send_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
		}
		if (_logic.is_magic_card(_send_card_data)) 
			show_send_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
		// 显示牌
		operate_player_get_card(_seat_index, 1, new int[] { show_send_card }, GameConstants.INVALID_SEAT);

		// 检查能不能杠
		if (GRR._left_card_count > 15) {
			GangCardResult m_gangCardResult = ((HandlerDispatchCard_QuanZhou)_handler_dispath_card).m_gangCardResult;
			m_gangCardResult.cbCardCount = 0;

			int cbActionMask = analyse_gang(GRR._cards_index[_seat_index], GRR._weave_items[_seat_index],
					GRR._weave_count[_seat_index], m_gangCardResult, true, GRR._cards_abandoned_gang[_seat_index]);

			if (cbActionMask != GameConstants.WIK_NULL) {
				curPlayerStatus.add_action(GameConstants.WIK_GANG);
				for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
					curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
				}
			}
		}
		_playerStatus[_seat_index]._card_status = 0;
		if (curPlayerStatus.has_action()) {
			change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
			operate_player_action(_seat_index, false);
		} else {
			change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
			operate_player_status();
		}
	}
	
	public void execute_first_flower(){
		if(is_exist_flower()){
			GameSchedule.put(() -> {
				execute_do_flower();
				execute_first_flower();
			}, 1, TimeUnit.SECONDS);
			return;
		}
		GameSchedule.put(() -> {
			try{
				//选鬼
				exe_select_magic();
				// 检测听牌
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if(i == _cur_banker)
						continue;
					_playerStatus[i]._hu_card_count = get_ting_card(_playerStatus[i]._hu_cards, GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i],
							i);
					if (_playerStatus[i]._hu_card_count > 0) {
						operate_chi_hu_cards(i, _playerStatus[i]._hu_card_count, _playerStatus[i]._hu_cards);
					}
				}
				execute_flower_start();
			} catch(Exception e){
				e.printStackTrace();
			}
			
		}, 2, TimeUnit.SECONDS);
	}
	
	public void execute_do_flower() {
		for (int n = 0; n < getTablePlayerNumber(); n++) {
			for (int i = GameConstants.MAX_ZI_FENG; i < GameConstants.MAX_INDEX; i++) {
				int index = i;
				int p = n;
				if (GRR._cards_index[p][i] > 0) {
					_playerStatus[p]._card_status = 2;
					operate_effect_action(p, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { Constants_MJ_QUAN_ZHOU.ACTION_OUT_FLOWER_CARD }, 1, GameConstants.INVALID_SEAT);
					hua_count[p]++;
					int data = _logic.switch_to_card_data(index);
					GRR._player_niao_cards[p][GRR._player_niao_count[p]++] = data;
					execute_out_flower(p, data);
					execute_dispatch_flower(p);
					break;
				}
			}
		}
	}
	
	public boolean is_exist_flower() {
		for (int p = 0; p < getTablePlayerNumber(); p++)
			for (int i = GameConstants.MAX_ZI_FENG; i < GameConstants.MAX_INDEX; i++)
				if (GRR._cards_index[p][i] > 0)
					return true;
		return false;
	}
	
	public void execute_out_flower(int _out_card_player,int _out_card_data){
		_logic.remove_card_by_index(GRR._cards_index[_out_card_player], _out_card_data);
		int cards[] = new int[Constants_MJ_QUAN_ZHOU.MAX_HAND_CARD_COUNT];
		int hand_card_count = switch_to_cards_data(GRR._cards_index[_out_card_player], cards);
		operate_player_cards(_out_card_player, hand_card_count, cards, 0, null);
		int data = _logic.is_magic_card(_out_card_data)?_out_card_data+GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI:_out_card_data;
		operate_out_card(_out_card_player, 1, new int[] { data }, GameConstants.OUT_CARD_TYPE_MID,
				GameConstants.INVALID_SEAT);
		exe_add_discard(_out_card_player, 1, new int[] { _out_card_data }, false, GameConstants.DELAY_SEND_CARD_DELAY);
	}
	
	public void execute_dispatch_flower(int _seat_index){
		_send_card_count++;
		int _send_card_data = _repertory_card[_all_card_len - GRR._left_card_count];
		--GRR._left_card_count;
		/*if (AbstractMJTable.DEBUG_CARDS_MODE) {
			_send_card_data = 0x01;
		}*/
		GRR._cards_index[_seat_index][_logic.switch_to_card_index(_send_card_data)]++;
		// 显示牌
		if(_seat_index == _cur_banker){
			this._send_card_data = _send_card_data;
			operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, GameConstants.INVALID_SEAT);
		}else{
			int cards[] = new int[Constants_MJ_QUAN_ZHOU.MAX_HAND_CARD_COUNT];
			int hand_card_count = switch_to_cards_data(GRR._cards_index[_seat_index], cards);
			operate_player_cards(_seat_index, hand_card_count, cards, 0, null);
		}
	}
	
	public boolean execute_exist_flower(int _seat_index) {
		int[] cards_index = GRR._cards_index[_seat_index];
		_playerStatus[_seat_index]._card_status = 0;
		for (int i = GameConstants.MAX_ZI_FENG; i < GameConstants.MAX_INDEX; i++) {
			int index = i;
			if (cards_index[i] > 0) {
				GameSchedule.put(() -> {
					_playerStatus[_seat_index]._card_status = 2;
					operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { Constants_MJ_QUAN_ZHOU.ACTION_OUT_FLOWER_CARD }, 1, GameConstants.INVALID_SEAT);
					hua_count[_seat_index]++;
					int data = _logic.switch_to_card_data(index);
					GRR._player_niao_cards[_seat_index][GRR._player_niao_count[_seat_index]++] = data;
					_handler.handler_player_out_card(this, _seat_index, data);
					//exe_dispatch_card(_seat_index, 0, 0);
				}, 1, TimeUnit.SECONDS);
				return true;
			}
		}
		return false;
	}
	
	public boolean is_swim_ban(){
		for(int i = 0; i < getTablePlayerNumber(); i++){
			if(swim_status[i] > 1){
				return true;
			}
		}
		return false;
	}
	
	public void process_swim_out_card(int seat_index, int card){
		ChiHuRight chr = new ChiHuRight();
		int count = 0;
		int cbCurrentCard;
		for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(GRR._cards_index[seat_index], GRR._weave_items[seat_index], GRR._weave_count[seat_index], cbCurrentCard, chr,
					GameConstants.CHR_ZI_MO, seat_index)) {
				count++;
			}
		}
		if(count >= 34){
			if(_logic.is_magic_card(card)){
				if(swim_status[seat_index] == 2)
					swim_status[seat_index] = 3;
				else
					swim_status[seat_index] = 2;
			}else
				swim_status[seat_index] = 1;
		}else 
			swim_status[seat_index] = 0;
	}
	
	@Override
	public int get_real_card(int card) {
		if (card > GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI && card < GameConstants.CARD_ESPECIAL_TYPE_HZ) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_TING && card < GameConstants.CARD_ESPECIAL_TYPE_BAO_TING) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_TING;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_TING + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI
				&& card < GameConstants.CARD_ESPECIAL_TYPE_BAO_TING + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_TING + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
		} else if (card > 11000 && card < 11200) {
			card -= 11000;
		} else if (card > 12000 && card < 12200) {
			card -= 12000;
		} else if (card > 13000 && card < 13200) {
			card -= 13000;
		} else if (card > 14200 && card < 14400) {
			card -= 14200;
		} else if (card > 15200 && card < 15400) {
			card -= 15200;
		} else if (card > 16200 && card < 16400) {
			card -= 16200;
		}

		return card;
	}
	
	/**
	 * 扑克转换 将手中牌索引 转换为实际牌数据
	 * 
	 * @param cards_index
	 * @param cards_data
	 * @return
	 */
	public int switch_to_cards_data(int cards_index[], int cards_data[]) {
		// 转换扑克
		int cbPosition = 0;
		for (int m = 0; m < _logic.get_magic_card_count(); m++) {
			for (int i = 0; i < cards_index[_logic.get_magic_card_index(m)]; i++) {
				cards_data[cbPosition++] = _logic.switch_to_card_data(_logic.get_magic_card_index(m));
			}
		}
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (_logic.is_magic_index(i))
				continue;
			if (cards_index[i] > 0) {
				for (int j = 0; j < cards_index[i]; j++) {
					cards_data[cbPosition++] = _logic.switch_to_card_data(i);
				}
			}
		}
		return cbPosition;
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
		int cards[] = new int[Constants_MJ_QUAN_ZHOU.MAX_HAND_CARD_COUNT];
		int hand_card_count = switch_to_cards_data(GRR._cards_index[seat_index], cards);
		cards[hand_card_count] = operate_card + GameConstants.CARD_ESPECIAL_TYPE_HU;
		hand_card_count++;
		this.operate_show_card(seat_index, GameConstants.Show_Card_HU, hand_card_count, cards, GameConstants.INVALID_SEAT);

		return;
	}
	
	// 删除扑克 by data
	public boolean remove_card_by_data(int cards[], int card_data) {
		int card_count = cards.length;

		if (card_count == 0) {
			return false;
		}

		// 定义变量
		int cbDeleteCount = 0;
		int cbTempCardData[] = new int[Constants_MJ_QUAN_ZHOU.MAX_HAND_CARD_COUNT];

		for (int i = 0; i < card_count; i++) {
			cbTempCardData[i] = cards[i];
		}

		// 置零扑克
		for (int i = 0; i < card_count; i++) {
			if (card_data == cbTempCardData[i]) {
				cbDeleteCount++;
				cbTempCardData[i] = 0;
				break;
			}
		}

		// 成功判断
		if (cbDeleteCount != 1) {
			return false;
		}

		// 清理扑克
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
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x01,0x01,0x02,0x02,0x03,0x03,0x04,0x04,0x05,0x05,0x06,0x06,0x07,0x39,0x29,0x29};
		int[] cards_of_player1 = new int[] { 0x12,0x12,0x13,0x14,0x15,0x17,0x18,0x19,0x23,0x24,0x24,0x25,0x25,0x26,0x27,0x28};
		int[] cards_of_player2 = new int[] { 0x12,0x12,0x13,0x14,0x15,0x17,0x18,0x19,0x23,0x24,0x24,0x25,0x25,0x26,0x27,0x28};
		int[] cards_of_player3 = new int[] { 0x12,0x12,0x13,0x14,0x15,0x17,0x18,0x19,0x23,0x24,0x24,0x25,0x25,0x26,0x27,0x28};

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}

		for (int j = 0; j < 16; j++) {
			if (getTablePlayerNumber() == 4) {
				GRR._cards_index[0][_logic
						.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[1][_logic
						.switch_to_card_index(cards_of_player1[j])] += 1;
				GRR._cards_index[2][_logic
						.switch_to_card_index(cards_of_player2[j])] += 1;
				GRR._cards_index[3][_logic
						.switch_to_card_index(cards_of_player3[j])] += 1;
			} else if (getTablePlayerNumber() == 3) {
				GRR._cards_index[0][_logic
						.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[1][_logic
						.switch_to_card_index(cards_of_player1[j])] += 1;
				GRR._cards_index[2][_logic
						.switch_to_card_index(cards_of_player2[j])] += 1;
			} else if (getTablePlayerNumber() == 2) {
				GRR._cards_index[0][_logic
						.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[1][_logic
						.switch_to_card_index(cards_of_player1[j])] += 1;
			}
		}

		if (BACK_DEBUG_CARDS_MODE) {
			if (debug_my_cards != null) {
				if (debug_my_cards.length > 16) {
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
	 * 模拟牌型--相同牌
	 */
	public void testSameCard(int[] cards) {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < 16; j++) {
				GRR._cards_index[i][_logic.switch_to_card_index(cards[j])] += 1;
			}
		}
		DEBUG_CARDS_MODE = false; // 把调试模式关闭
		BACK_DEBUG_CARDS_MODE = false;
	}


}
