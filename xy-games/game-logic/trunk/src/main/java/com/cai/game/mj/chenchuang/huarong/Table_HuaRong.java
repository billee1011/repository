package com.cai.game.mj.chenchuang.huarong;

import java.util.Arrays;
import java.util.List;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.Constants_MJ_HUA_RONG;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.RandomUtil;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJType;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.game.util.GameUtilConstants;
import com.cai.service.PlayerServiceImpl;
import com.google.common.collect.Lists;

/**
 * chenchuang
 * 华容逞癞子
 */
public class Table_HuaRong extends AbstractMJTable {

	private static final long serialVersionUID = 1L;
	
	public int[] player_magic_count;//玩家摸牌抓到的癞子数
	
	public int[] player_out_magic_count = new int[getTablePlayerNumber()];//玩家打出的癞子数
	
	public HandlerSelectMagic_HuaRong handler_select_magic;
	
	public Table_HuaRong(MJType mJType) {
		super(mJType);
	}

	@Override
	protected void onInitTable() {
		_handler_chi_peng = new HandlerChiPeng_HuaRong();
		_handler_dispath_card = new HandlerDispatchCard_HuaRong();
		_handler_gang = new HandlerGang_HuaRong();
		_handler_out_card_operate = new HandlerOutCardOperate_HuaRong();
		handler_select_magic = new HandlerSelectMagic_HuaRong();
	}

	public boolean exe_select_magic() {
		this.set_handler(this.handler_select_magic);
		handler_select_magic.reset_status(_cur_banker);
		_handler.exe(this);
		return true;
	}
	
	/**
	 * 开局第1局随机选庄
	 */
	@Override
	protected void initBanker() {
		_cur_banker = RandomUtil.getRandomNumber(getTablePlayerNumber());
	}

	/**
	 * 初始化洗牌数据:默认(如不能满足请重写此方法)
	 * 
	 * @return
	 */
	@Override
	protected void init_shuffle() {
		List<Integer> cards_list = Lists.newArrayList();
		if(has_rule(Constants_MJ_HUA_RONG.GAME_RULE_LIANG_MEN_PAI)) {
			int [] cards = Constants_MJ_HUA_RONG.CARD_DATA72;
			for (int i = 0; i < cards.length; i++) {
				cards_list.add(cards[i]);
			}
			
		}else{
			int [] cards = Constants_MJ_HUA_RONG.CARD_DATA108;
			for (int i = 0; i < cards.length; i++) {
				cards_list.add(cards[i]);
			}
		}

		int[] card = new int[cards_list.size()];
		for (int i = 0; i < cards_list.size(); i++) {
			card[i] = cards_list.get(i);
		}
		_repertory_card = new int[card.length];
		shuffle(_repertory_card, card);
	}
	
	/**
	 * 洗完牌执行开始
	 */
	@Override
	protected boolean on_game_start() {
		
		player_magic_count = new int[getTablePlayerNumber()];
		player_out_magic_count = new int[getTablePlayerNumber()];
		
		_logic.clean_magic_cards();
		//选鬼
		exe_select_magic();
		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);
		_player_result.men_qing[GRR._banker_player]++;

		int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants.MAX_COUNT];

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}
		//发送给玩家手牌
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				if (_logic.is_magic_card(hand_cards[i][j])) {
					hand_cards[i][j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
                }else if(_logic.is_lai_gen_card(hand_cards[i][j])){
                	hand_cards[i][j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_GEN;
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

			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
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
			player_magic_count[i] = GRR._cards_index[i][_logic.get_magic_card_index(0)];
			if(player_magic_count[i] == 4 && has_rule(Constants_MJ_HUA_RONG.GAME_RULE_SI_LAI_YOU_XI)){
				GRR._chi_hu_rights[i].opr_or(Constants_MJ_HUA_RONG.CHR_SI_LAI_YOU_XI);
				for (int p = 0; p < getTablePlayerNumber(); p++) {
					if (i == p)
						continue;
					GRR._game_score[p] -= 10;
					GRR._game_score[i] += 10;
				}
			}
		}

		//发第一张牌
		exe_dispatch_card(_current_player, GameConstants.GANG_TYPE_HONG_ZHONG, GameConstants.DELAY_SEND_CARD_DELAY);

		return true;
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {
		if (card_type == Constants_MJ_HUA_RONG.HU_CARD_TYPE_ZI_MO) {
			chiHuRight.opr_or(Constants_MJ_HUA_RONG.CHR_ZI_MO);//自摸
		} else if (card_type == Constants_MJ_HUA_RONG.HU_CARD_TYPE_QIANG_GANG) {
			chiHuRight.opr_or(Constants_MJ_HUA_RONG.CHR_QIANG_GANG);//抢杠胡
		}
		int cur_card_index = _logic.switch_to_card_index(cur_card);
		int[] cbCardIndexTemp = Arrays.copyOf(cards_index, cards_index.length);
		cbCardIndexTemp[cur_card_index]++;
		int magic = cbCardIndexTemp[_logic.get_magic_card_index(0)];
		if(magic >= 2)
			return GameConstants.WIK_NULL;
		if(has_rule(Constants_MJ_HUA_RONG.GAME_RULE_WU_LAI_DAO_DI)){//无癞到底
			if(magic >= 1)
				return GameConstants.WIK_NULL;
		}
		//得到癞子牌的个数，和索引
		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
        int magic_card_count = _logic.get_magic_card_count();

        if (magic_card_count > 2) { // 一般只有两种癞子牌存在
            magic_card_count = 2;
        }

        for (int i = 0; i < magic_card_count; i++) {
            magic_cards_index[i] = _logic.get_magic_card_index(i);
        }
        
		//是否硬胡牌牌型
        boolean yinghu = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, cur_card_index, magic_cards_index, 0);
        if(has_rule(Constants_MJ_HUA_RONG.GAME_RULE_BAN_LAI)){//半癞
			if(!yinghu)
				return GameConstants.WIK_NULL;
		}
        
		boolean hu = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, cur_card_index, magic_cards_index, magic_card_count);
		if(hu && magic == 1 && !yinghu){//一个癞子单吊不能胡
			int count = 0;
			for (int i = 0; i < GameConstants.MAX_ZI; i++) {
				boolean hu_ = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, i, magic_cards_index, magic_card_count);
				if(hu_)
					count++;
			}
			if(count == GameConstants.MAX_ZI)
				return GameConstants.WIK_NULL;
		}
		
		if(yinghu){//硬胡
			chiHuRight.opr_or(Constants_MJ_HUA_RONG.CHR_YING_HU);
		}
		if (card_type == GameConstants.GANG_TYPE_LAI_ZI) {
			chiHuRight.opr_or(Constants_MJ_HUA_RONG.CHR_LAI_YOU);//癞油
		}
		if(hu)
			return GameConstants.WIK_CHI_HU;
		return GameConstants.WIK_NULL;
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
			if (playerStatus.is_chi_hu_round()) {
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				// 吃胡判断
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						Constants_MJ_HUA_RONG.HU_CARD_TYPE_QIANG_GANG, i);

				// 结果判断
				if (action != 0) {
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

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		
		_player_result.hai_di[seat_index]++;
		
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		countCardType(chr, seat_index);

		int wFanShu = 0;// 番数
		int roomScore = getRuleValue(Constants_MJ_HUA_RONG.GAME_RULE_DI_FEN);
		if(!chr.opr_and(Constants_MJ_HUA_RONG.CHR_LAI_YOU).is_empty())
			wFanShu++;
		if(!chr.opr_and(Constants_MJ_HUA_RONG.CHR_YING_HU).is_empty())
			wFanShu++;
		wFanShu += player_out_magic_count[seat_index];
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
		} else {
			GRR._lost_fan_shu[provide_index][seat_index] = wFanShu;
		}

		////////////////////////////////////////////////////// 自摸 算分
		if (zimo) {//自摸
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index)
					continue;
				int lChiHuScore = (1 << (wFanShu + player_out_magic_count[i])) * roomScore;
				GRR._game_score[i] -= lChiHuScore;
				GRR._game_score[seat_index] += lChiHuScore;
			}
		}else {//抢杠胡
			int lChiHuScore = (1 << (wFanShu + player_out_magic_count[provide_index])) * roomScore;
			if(has_rule(Constants_MJ_HUA_RONG.GAME_RULE_QIANG_GANG_BAO_PEI)){
				GRR._game_score[provide_index] -= lChiHuScore * (getTablePlayerNumber() - 1);
				GRR._game_score[seat_index] += lChiHuScore * (getTablePlayerNumber() - 1);
			}else{
				GRR._game_score[provide_index] -= lChiHuScore;
				GRR._game_score[seat_index] += lChiHuScore;
			}
		}
			
		// 设置变量
		GRR._provider[seat_index] = provide_index;
		_status_gang = false;
		_status_gang_hou_pao = false;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);
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
					if (type == Constants_MJ_HUA_RONG.CHR_ZI_MO) {
						result.append(" 自摸");
					}
					if (type == Constants_MJ_HUA_RONG.CHR_QIANG_GANG) {
						result.append(" 抢杠胡");
					}
					if (type == Constants_MJ_HUA_RONG.CHR_YING_HU) {
						result.append(" 硬胡");
					}
					if (type == Constants_MJ_HUA_RONG.CHR_LAI_YOU) {
						result.append(" 癞油");
					}
					if (type == Constants_MJ_HUA_RONG.CHR_SI_LAI_YOU_XI) {
						result.append(" 四癞有喜");
					}
					if (type == Constants_MJ_HUA_RONG.CHR_LIAN_GUN_DAI_PA) {
						result.append(" 连滚带爬");
					}
				} else{
					if (type == Constants_MJ_HUA_RONG.CHR_BEI_QIANG_GANG) {
						result.append(" 被抢杠");
					}
					if (type == Constants_MJ_HUA_RONG.CHR_SI_LAI_YOU_XI) {
						result.append(" 四癞有喜");
					}
				}
			}

			int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0, xiao_chao_tian = 0, da_chao_tian = 0;

			if (GRR != null) {
				for (int tmpPlayer = 0; tmpPlayer < this.getTablePlayerNumber(); tmpPlayer++) {
					for (int w = 0; w < GRR._weave_count[tmpPlayer]; w++) {
						if (GRR._weave_items[tmpPlayer][w].weave_kind == GameConstants.WIK_DA_CHAO_TIAN){
							if (tmpPlayer == player)
								da_chao_tian = 1;
						}
						if (GRR._weave_items[tmpPlayer][w].weave_kind == GameConstants.WIK_XIAO_CHAO_TIAN){
							if (tmpPlayer == player)
								xiao_chao_tian = 1;
							if (GRR._weave_items[tmpPlayer][w].provide_player == player) {
								fang_gang++;
							}
						}
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
			if (xiao_chao_tian == 1) {
				result.append(" 小朝天");
			}
			if (da_chao_tian == 1) {
				result.append(" 大朝天");
			}
			
			if(player_out_magic_count[player] > 0)
				result.append(" 逞X" + player_out_magic_count[player]);

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

			boolean can_peng = true;
			int[] tmp_cards_data = _playerStatus[i].get_cards_abandoned_peng();
			for (int x = 0; x < GameConstants.MAX_ABANDONED_CARDS_COUNT; x++) {
				if (tmp_cards_data[x] == card) {
					can_peng = false;
					break;
				}
			}
			//can_peng = can_peng && playerStatus.is_chi_peng_round();
			if (can_peng && GRR._left_card_count > 0) {//如果牌堆已经没有牌，出的最后一张牌了不能碰，杠
				action = check_peng(GRR._cards_index[i], card);
				if (action != 0) {
					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);
					bAroseAction = true;
				}
			}
			if (GRR._left_card_count > 0) {
				action = estimate_gang_card_out_card(GRR._cards_index[i], card);
				if (action != 0) {
					playerStatus.add_action(action);
					playerStatus.add_xiao(card, action, seat_index, 1);
					bAroseAction = true;
				}
			}

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
	
	/**
	 * 杠牌判断 别人打的牌自己能不能杠
	 */
	public int estimate_gang_card_out_card(int card_index[], int cur_card) {
		// 参数效验
		if (_logic.is_valid_card(cur_card) == false) {
			return GameConstants.WIK_NULL;
		}
		if(_logic.is_lai_gen_card(cur_card))
			return card_index[_logic.switch_to_card_index(cur_card)] == 2 ? GameConstants.WIK_XIAO_CHAO_TIAN : GameConstants.WIK_NULL;
		return card_index[_logic.switch_to_card_index(cur_card)] == 3 ? GameConstants.WIK_GANG : GameConstants.WIK_NULL;
	}
	
	// 碰牌判断
	public int check_peng(int card_index[], int cur_card) {
		// 参数效验
		if (_logic.is_valid_card(cur_card) == false || _logic.is_lai_gen_card(cur_card)) {
			return GameConstants.WIK_NULL;
		}
		// 碰牌判断
		return (card_index[_logic.switch_to_card_index(cur_card)] >= 2) ? GameConstants.WIK_PENG : GameConstants.WIK_NULL;
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
		if (count == GameConstants.MAX_ZI_FENG) {
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		return false;
	}
	
	
	
    
    // 杠牌分析 包括补杠 check_weave检查补杠
    public int analyse_gang(PlayerStatus curPlayerStatus, int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount,
            GangCardResult gangCardResult, boolean check_weave) {
        // 设置变量
        int cbActionMask = GameConstants.WIK_NULL;
		if (GRR._left_card_count > 0) {
	        // 手上杠牌
	        for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
	            if (cards_index[i] == 4 && !_logic.is_magic_index(i)) {
	                cbActionMask |= GameConstants.WIK_GANG;
	                curPlayerStatus.add_action(GameConstants.WIK_GANG);
	                int index = gangCardResult.cbCardCount++;
	                gangCardResult.cbCardData[index] = _logic.switch_to_card_data(i);
	                gangCardResult.isPublic[index] = 0;// 暗杠
	                gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
	            } else if ((cards_index[i] == 3) && (_logic.is_lai_gen_card(_logic.switch_to_card_data(i)))) {
					cbActionMask |= GameConstants.WIK_DA_CHAO_TIAN;
					curPlayerStatus.add_action(GameConstants.WIK_DA_CHAO_TIAN);
					int index = gangCardResult.cbCardCount++;
					gangCardResult.cbCardData[index] = _logic.switch_to_card_data(i);
					gangCardResult.isPublic[index] = 0;// 暗杠
					gangCardResult.type[index] = GameConstants.GANG_TYPE_PI_ZI;
				}
	        }
	
	        if (check_weave) {
	            // 组合杠牌，包括以前能杠，但是不杠，发牌之后而选择补杠的
	            for (int i = 0; i < cbWeaveCount; i++) {
	                if (WeaveItem[i].weave_kind == GameConstants.WIK_PENG && WeaveItem[i].is_vavild) {
	                    for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
	                        if (cards_index[j] != 1) {
	                            continue;
	                        } else {
	                            if (WeaveItem[i].center_card == _logic.switch_to_card_data(j) && WeaveItem[i].is_vavild) {
	                                cbActionMask |= GameConstants.WIK_GANG;
	                                curPlayerStatus.add_action(GameConstants.WIK_GANG);
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
            for (int i = 0; i < getTablePlayerNumber(); i++) {
                // 记录
                for (int j = 0; j < getTablePlayerNumber(); j++) {
                    _player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
                }
                
                for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
            		for (int k = 0; k < getTablePlayerNumber(); k++) {
            			lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
            		}
            	}

            }

            for (int i = 0; i < getTablePlayerNumber(); i++) {
                GRR._game_score[i] += lGangScore[i];
                GRR._game_score[i] += GRR._start_hu_score[i]; // 起手胡分数

                // 记录
                _player_result.game_score[i] += GRR._game_score[i];
                _player_result.haspiao[i] = 0;
    			_player_result.biaoyan[i] = 0;
    			_player_result.nao[i] = 0;

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
                    pnc.addItem(GRR._lai_zi_pi_zi_gang[i][j]);
                }
                for (int j = 0; j < GRR._player_niao_count_fei[i]; j++) {
                    pnc.addItem(GRR._player_niao_cards_fei[i][j]);
                }
                game_end.addPlayerNiaoCards(pnc);
                game_end.addHuResult(GRR._hu_result[i]);

                // 胡的牌
                Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
                for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
					if (_logic.is_magic_card(GRR._chi_hu_card[i][j])) {
						hc.addItem(GRR._chi_hu_card[i][j]
								+ GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
					}else if(_logic.is_lai_gen_card(GRR._chi_hu_card[i][j])){
						hc.addItem(GRR._chi_hu_card[i][j]
								+ GameConstants.CARD_ESPECIAL_TYPE_LAI_GEN);
					}else {
						hc.addItem(GRR._chi_hu_card[i][j]);
					}
				}

				for (int h = 0; h < GRR._chi_hu_card[i].length; h++) {
					if (_logic.is_magic_card(GRR._chi_hu_card[i][h])) {
						game_end.addHuCardData(GRR._chi_hu_card[i][h]
								+ GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
					}else if(_logic.is_lai_gen_card(GRR._chi_hu_card[i][h])){
						game_end.addHuCardData(GRR._chi_hu_card[i][h]
								+ GameConstants.CARD_ESPECIAL_TYPE_LAI_GEN);
					} else {
						game_end.addHuCardData(GRR._chi_hu_card[i][h]);
					}
				}

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
					if (_logic.is_magic_card(GRR._cards_data[i][j])) {
						cs.addItem(GRR._cards_data[i][j]
								+ GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
					}else if(_logic.is_lai_gen_card(GRR._cards_data[i][j])){
						cs.addItem(GRR._cards_data[i][j]
								+ GameConstants.CARD_ESPECIAL_TYPE_LAI_GEN);
					} else {
						cs.addItem(GRR._cards_data[i][j]);
					}
				}
                game_end.addCardsData(cs);// 牌

                // 组合
                WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
                for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
                    WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
                    weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
                    weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
                    if(_logic.is_lai_gen_card(GRR._weave_items[i][j].center_card))
    					weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card + GameConstants.CARD_ESPECIAL_TYPE_LAI_GEN);
    				else
    					weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
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
                game_end.addPao(_player_result.pao[i]);

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
        // 错误断言
        return false;
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
	public boolean operate_player_cards(int seat_index, int card_count, int cards[], int weave_count, WeaveItem weaveitems[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS);// 12更新玩家手牌数据
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(1);

		this.load_common_status(roomResponse);

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
				if(_logic.is_lai_gen_card(weaveitems[j].center_card))
					weaveItem_item.setCenterCard(weaveitems[j].center_card + GameConstants.CARD_ESPECIAL_TYPE_LAI_GEN);
				else
					weaveItem_item.setCenterCard(weaveitems[j].center_card);
				roomResponse.addWeaveItems(weaveItem_item);
			}
		}

		this.send_response_to_other(seat_index, roomResponse);

		// 手牌--将自己的手牌数据发给自己
		for (int j = 0; j < card_count; j++) {
			roomResponse.addCardData(cards[j]);
		}
		GRR.add_room_response(roomResponse);
		// 自己才有牌数据
		this.send_response_to_player(seat_index, roomResponse);

		return true;
	}
	
	public void showAction(int seat_index){
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		int a = 0;
		boolean YING_HU = !chr.opr_and(Constants_MJ_HUA_RONG.CHR_YING_HU).is_empty();
		boolean LAI_YOU = !chr.opr_and(Constants_MJ_HUA_RONG.CHR_LAI_YOU).is_empty();
		if(YING_HU && LAI_YOU)
			a = Constants_MJ_HUA_RONG.CHR_YING_LAI_YOU;
		else if(!YING_HU && LAI_YOU)
			a = Constants_MJ_HUA_RONG.CHR_RUAN_LAI_YOU;
		else if(YING_HU && !LAI_YOU)
			a = Constants_MJ_HUA_RONG.CHR_YING_ZI_MO;
		else if(!YING_HU && !LAI_YOU)
			a = Constants_MJ_HUA_RONG.CHR_RUAN_ZI_MO;
		operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, 1, new long[]{a}, 1, GameConstants.INVALID_SEAT);
	}
	
	public void changeBiaoYan(){
        for (int i = 0; i < getTablePlayerNumber(); i++) {
        	for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
        		for (int k = 0; k < getTablePlayerNumber(); k++) {
        			this._player_result.biaoyan[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
        		}
        	}
        }
    }
    
    
    @Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x21,0x21,0x21,0x11,0x11,0x11,0x19,0x19,0x17,0x12,0x12,0x12,0x13 };
		int[] cards_of_player1 = new int[] { 0x21,0x21,0x21,0x11,0x11,0x11,0x16,0x15,0x14,0x18,0x18,0x18,0x13 };
		int[] cards_of_player3 = new int[] { 0x21,0x21,0x21,0x11,0x11,0x11,0x19,0x19,0x17,0x29,0x29,0x29,0x13 };
		int[] cards_of_player2 = new int[] { 0x21,0x21,0x21,0x11,0x11,0x11,0x19,0x19,0x17,0x29,0x29,0x37,0x13 };

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}

		for (int j = 0; j < 13; j++) {
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
