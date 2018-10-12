package com.cai.game.mj.chenchuang.pc;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.Constants_MJ_PC;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.RandomUtil;
import com.cai.future.GameSchedule;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJType;
import com.cai.game.mj.handler.MJHandlerChiPeng;
import com.cai.game.mj.handler.MJHandlerDispatchCard;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.game.util.GameUtilConstants;
import com.cai.service.PlayerServiceImpl;

/**
 * chenchuang
 * 浦城麻将
 */
public class Table_PuCheng extends AbstractMJTable {

	private static final long serialVersionUID = 1L;
	
	public int action_wait_time = -1;//超时等待秒
	public boolean is_chase_vaild;//跟庄是否有效;
	public int banker_out_card;//庄家出的牌
	public int chase_count;//烧庄次数
	public int[][] player_game_score = new int[getTablePlayerNumber()][getTablePlayerNumber()];//每个玩家的分数
	
	public Table_PuCheng() {
		super(MJType.GAME_TYPE_MJ_PU_CHENG);
	}

	@Override
	protected void onInitTable() {
		_handler_chi_peng = new HandlerChiPeng_PuCheng();
		_handler_dispath_card = new HandlerDispatchCard_PuCheng();
		_handler_gang = new HandlerGang_PuCheng();
		_handler_out_card_operate = new HandlerOutCardOperate_PuCheng();
	}

	public void exe_select_magic(){
		if(!has_rule(Constants_MJ_PC.GAME_RULE_HZJ) && !has_rule(Constants_MJ_PC.GAME_RULE_ONLY_HZJ))
			return;
		_logic.add_magic_card_index(Constants_MJ_PC.HONG_ZHONG_INDEX);
		GRR._especial_card_count = 1;
		GRR._especial_show_cards[0] = Constants_MJ_PC.HONG_ZHONG_DATA + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
		// 处理每个玩家手上的牌，如果有王牌，处理一下
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int[] hand_cards = new int[GameConstants.MAX_COUNT];
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards);
			for (int j = 0; j < hand_card_count; j++) {
				if (_logic.is_magic_card(hand_cards[j])) {
					hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				}
			}
			// 玩家客户端刷新一下手牌
			operate_player_cards(i, hand_card_count, hand_cards, 0, null);
		}
	}
	
	/**第一局建房者为庄家，若为代开房/俱乐部开房（即开房者自己未进入牌局），则随机一位为庄家;*/
	@Override
	protected void initBanker() {
		long id = getCreate_player().getAccount_id();
		if(get_player(id) == null)
			_cur_banker = RandomUtil.getRandomNumber(getTablePlayerNumber());
		else
			_cur_banker = getCreate_player().get_seat_index();
	}


	/**
	 * 洗完牌执行开始
	 */
	@Override
	protected boolean on_game_start() {
		//初始化常量
		is_chase_vaild = has_rule(Constants_MJ_PC.GAME_RULE_SHAO_ZHUANG);
		banker_out_card = 0;
		chase_count = 0;
		action_wait_time = get_action_wait_time();
		player_game_score = new int[getTablePlayerNumber()][getTablePlayerNumber()];
		//选鬼
		_logic.clean_magic_cards();
		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

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
		//选鬼
		exe_select_magic();
		// 检测听牌
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i]._hu_card_count = get_ting_card(_playerStatus[i]._hu_cards, GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i],
					i);
			if (_playerStatus[i]._hu_card_count > 0) {
				operate_chi_hu_cards(i, _playerStatus[i]._hu_card_count, _playerStatus[i]._hu_cards);
			}
		}

		//发第一张牌
		exe_dispatch_card(_current_player, GameConstants.GANG_TYPE_HONG_ZHONG, GameConstants.DELAY_SEND_CARD_DELAY);

		return true;
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {
		if (card_type == Constants_MJ_PC.HU_CARD_TYPE_ZI_MO) {
			chiHuRight.opr_or(Constants_MJ_PC.CHR_ZI_MO);//自摸
		} else if (card_type == Constants_MJ_PC.HU_CARD_TYPE_QIANG_GANG) {
			chiHuRight.opr_or(Constants_MJ_PC.CHR_QING_GANG_HU);//抢杠胡
		}else if (card_type == Constants_MJ_PC.HU_CARD_TYPE_JIE_PAO) {
			chiHuRight.opr_or(Constants_MJ_PC.CHR_DIAN_PAO_HU);//点炮胡
		}
		//拷贝把当前牌加入手牌
		int cur_card_index = _logic.switch_to_card_index(cur_card);
		int[] temp_cards_index = Arrays.copyOf(cards_index, cards_index.length);
		temp_cards_index[cur_card_index]++;
		
		boolean has_da_hu = has_rule(Constants_MJ_PC.GAME_RULE_ONLY_HZJ);
		
		//七对4倍
  		boolean xiao_qi_dui = false;
  		int qi_dui_value = 0;
		int hu = is_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card);
		if (hu != GameConstants.WIK_NULL) {
			xiao_qi_dui =true;
			qi_dui_value = hu;
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
        //是否胡牌牌型
  		boolean analyse_win_by_cards_index = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, cur_card_index, magic_cards_index, magic_card_count);
  		if(!(analyse_win_by_cards_index || xiao_qi_dui)){
  			return GameConstants.WIK_NULL;
  		}
		//清一色
		boolean is_qing_yi_se = _logic.is_qing_yi_se(cards_index, weaveItems, weave_count, cur_card);
		
		//七大对
		boolean analyse_peng_hu_by_cards_index = AnalyseCardUtil.analyse_peng_hu_by_cards_index(cards_index, cur_card_index, magic_cards_index, magic_card_count);
		
		if(is_qing_yi_se && qi_dui_value > 0x20 && (has_rule(Constants_MJ_PC.GAME_RULE_QING_YI_SE_HH_QI_XIAO_DUI) || has_da_hu))
			chiHuRight.opr_or(Constants_MJ_PC.CHR_QING_YI_SE_HH_QI_XIAO_DUI);
		else if(is_qing_yi_se && analyse_peng_hu_by_cards_index && (has_rule(Constants_MJ_PC.GAME_RULE_QING_YI_SE_QI_DA_DUI) || has_da_hu))
			chiHuRight.opr_or(Constants_MJ_PC.CHR_QING_YI_SE_QI_DA_DUI);
		else if(is_qing_yi_se && qi_dui_value > 0 && (has_rule(Constants_MJ_PC.GAME_RULE_QING_YI_SE_QI_XIAO_DUI) || has_da_hu))
			chiHuRight.opr_or(Constants_MJ_PC.CHR_QING_YI_SE_QI_XIAO_DUI);
		else if(qi_dui_value > 0x20 && (has_rule(Constants_MJ_PC.GAME_RULE_HH_QI_XIAO_DUI) || has_da_hu))
			chiHuRight.opr_or(Constants_MJ_PC.CHR_HH_QI_XIAO_DUI);
		else if(is_qing_yi_se && (has_rule(Constants_MJ_PC.GAME_RULE_QING_YI_SE) || has_da_hu))
			chiHuRight.opr_or(Constants_MJ_PC.CHR_QING_YI_SE);
		else if(card_type == Constants_MJ_PC.HU_CARD_TYPE_ZI_MO && ((HandlerDispatchCard_PuCheng)_handler_dispath_card)._type == GameConstants.GANG_TYPE_HONG_ZHONG && (has_rule(Constants_MJ_PC.GAME_RULE_TIANDIHU) || has_da_hu))
			chiHuRight.opr_or(Constants_MJ_PC.CHR_TIAN_HU);
		else if(card_type == Constants_MJ_PC.HU_CARD_TYPE_JIE_PAO && _handler_out_card_operate._type == GameConstants.GANG_TYPE_HONG_ZHONG && (has_rule(Constants_MJ_PC.GAME_RULE_TIANDIHU) || has_da_hu))
			chiHuRight.opr_or(Constants_MJ_PC.CHR_DI_HU);
		else if(qi_dui_value > 0 && (has_rule(Constants_MJ_PC.GAME_RULE_QI_XIAO_DUI) || has_da_hu))
			chiHuRight.opr_or(Constants_MJ_PC.CHR_QI_XIAO_DUI);
		else if(analyse_peng_hu_by_cards_index && (has_rule(Constants_MJ_PC.GAME_RULE_QI_DA_DUI) || has_da_hu))
			chiHuRight.opr_or(Constants_MJ_PC.CHR_QI_DA_DUI);
		else
			chiHuRight.opr_or(Constants_MJ_PC.CHR_PING_HU);
		if(has_rule(Constants_MJ_PC.GAME_RULE_ZI_MO) && card_type == Constants_MJ_PC.HU_CARD_TYPE_JIE_PAO && !chiHuRight.opr_and(Constants_MJ_PC.CHR_PING_HU).is_empty())
			return GameConstants.WIK_NULL;
		return GameConstants.WIK_CHI_HU;
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
			if ( _playerStatus[i].is_chi_hu_round()) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						Constants_MJ_PC.HU_CARD_TYPE_QIANG_GANG, i);
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
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		countCardType(chr, seat_index);
		
		boolean is_zm = zimo || !chr.opr_and(Constants_MJ_PC.CHR_QING_GANG_HU).is_empty();
		int score = getPaiXingScore(chr, is_zm);
		////////////////////////////////////////////////////// 自摸 算分
		
		if (is_zm) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if(i == seat_index)
					continue;
				player_game_score[seat_index][i] += score;
				player_game_score[i][seat_index] -= score;
			}
		}else{
			player_game_score[seat_index][provide_index] += score;
			player_game_score[provide_index][seat_index] -= score;
		}
		// 设置变量
		GRR._provider[seat_index] = provide_index;
		_status_gang = false;
		_status_gang_hou_pao = false;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);
	}


	private int getPaiXingScore(ChiHuRight chr, boolean is_zm) {
		int score = is_zm ? 2 : 1;
		if(getTablePlayerNumber() == 4){
			if(!chr.opr_and(Constants_MJ_PC.CHR_QI_DA_DUI).is_empty())
				score = is_zm ? 4 : 6;
			if(!chr.opr_and(Constants_MJ_PC.CHR_QI_XIAO_DUI).is_empty())
				score = is_zm ? 4 : 6;
			if(!chr.opr_and(Constants_MJ_PC.CHR_QING_YI_SE).is_empty())
				score = is_zm ? 8 : 12;
			if(!chr.opr_and(Constants_MJ_PC.CHR_HH_QI_XIAO_DUI).is_empty())
				score = is_zm ? 8 : 12;
			if(!chr.opr_and(Constants_MJ_PC.CHR_DI_HU).is_empty())
				score = 12;
			if(!chr.opr_and(Constants_MJ_PC.CHR_TIAN_HU).is_empty())
				score = 4;
			if(!chr.opr_and(Constants_MJ_PC.CHR_QING_YI_SE_QI_XIAO_DUI).is_empty())
				score = is_zm ? 16 : 24;
			if(!chr.opr_and(Constants_MJ_PC.CHR_QING_YI_SE_QI_DA_DUI).is_empty())
				score = is_zm ? 16 : 24;
			if(!chr.opr_and(Constants_MJ_PC.CHR_QING_YI_SE_HH_QI_XIAO_DUI).is_empty())
				score = is_zm ? 32 : 48;
		} else if(getTablePlayerNumber() == 3){
			if(!chr.opr_and(Constants_MJ_PC.CHR_QI_DA_DUI).is_empty())
				score = is_zm ? 4 : 4;
			if(!chr.opr_and(Constants_MJ_PC.CHR_QI_XIAO_DUI).is_empty())
				score = is_zm ? 4 : 4;
			if(!chr.opr_and(Constants_MJ_PC.CHR_QING_YI_SE).is_empty())
				score = is_zm ? 8 : 8;
			if(!chr.opr_and(Constants_MJ_PC.CHR_HH_QI_XIAO_DUI).is_empty())
				score = is_zm ? 8 : 8;
			if(!chr.opr_and(Constants_MJ_PC.CHR_DI_HU).is_empty())
				score = 8;
			if(!chr.opr_and(Constants_MJ_PC.CHR_TIAN_HU).is_empty())
				score = 4;
			if(!chr.opr_and(Constants_MJ_PC.CHR_QING_YI_SE_QI_XIAO_DUI).is_empty())
				score = is_zm ? 16 : 16;
			if(!chr.opr_and(Constants_MJ_PC.CHR_QING_YI_SE_QI_DA_DUI).is_empty())
				score = is_zm ? 16 : 16;
			if(!chr.opr_and(Constants_MJ_PC.CHR_QING_YI_SE_HH_QI_XIAO_DUI).is_empty())
				score = is_zm ? 32 : 32;
		} else if(getTablePlayerNumber() == 2){
			if(!chr.opr_and(Constants_MJ_PC.CHR_QI_DA_DUI).is_empty())
				score = is_zm ? 4 : 2;
			if(!chr.opr_and(Constants_MJ_PC.CHR_QI_XIAO_DUI).is_empty())
				score = is_zm ? 4 : 2;
			if(!chr.opr_and(Constants_MJ_PC.CHR_QING_YI_SE).is_empty())
				score = is_zm ? 8 : 4;
			if(!chr.opr_and(Constants_MJ_PC.CHR_HH_QI_XIAO_DUI).is_empty())
				score = is_zm ? 8 : 4;
			if(!chr.opr_and(Constants_MJ_PC.CHR_DI_HU).is_empty())
				score = 4;
			if(!chr.opr_and(Constants_MJ_PC.CHR_TIAN_HU).is_empty())
				score = 4;
			if(!chr.opr_and(Constants_MJ_PC.CHR_QING_YI_SE_QI_XIAO_DUI).is_empty())
				score = is_zm ? 16 : 8;
			if(!chr.opr_and(Constants_MJ_PC.CHR_QING_YI_SE_QI_DA_DUI).is_empty())
				score = is_zm ? 16 : 8;
			if(!chr.opr_and(Constants_MJ_PC.CHR_QING_YI_SE_HH_QI_XIAO_DUI).is_empty())
				score = is_zm ? 32 : 16;
		}
		return score;
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
					if (type == Constants_MJ_PC.CHR_TIAN_HU) {
						result.append(" 天胡");
					}
					if (type == Constants_MJ_PC.CHR_DI_HU) {
						result.append(" 地胡");
					}
					if (type == Constants_MJ_PC.CHR_ZI_MO) {
						result.append(" 自摸");
					}
					if (type == Constants_MJ_PC.CHR_QING_GANG_HU) {
						result.append(" 抢杠胡");
					}
					if (type == Constants_MJ_PC.CHR_DIAN_PAO_HU) {
						result.append(" 接炮");
					}
					if (type == Constants_MJ_PC.CHR_QI_XIAO_DUI) {
						result.append(" 七小对");
					}
					if (type == Constants_MJ_PC.CHR_QI_DA_DUI) {
						result.append(" 七大对");
					}
					if (type == Constants_MJ_PC.CHR_QING_YI_SE) {
						result.append(" 清一色");
					}
					if (type == Constants_MJ_PC.CHR_HH_QI_XIAO_DUI) {
						result.append(" 豪华七小对");
					}
					if (type == Constants_MJ_PC.CHR_QING_YI_SE_QI_XIAO_DUI) {
						result.append(" 清一色七小对");
					}
					if (type == Constants_MJ_PC.CHR_QING_YI_SE_QI_DA_DUI) {
						result.append(" 清一色七大对");
					}
					if (type == Constants_MJ_PC.CHR_QING_YI_SE_HH_QI_XIAO_DUI) {
						result.append(" 清一色豪华七小对");
					}
					if (type == Constants_MJ_PC.CHR_PING_HU) {
						result.append(" 平胡");
					}
					
				} else if (type == Constants_MJ_PC.CHR_FANG_PAO) {
					result.append(" 放炮");
				} else if (type == Constants_MJ_PC.CHR_BEI_QIANG_GANG) {
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
			if (ming_gang > 0) {
				result.append(" 加杠X" + ming_gang);
			}
			if (fang_gang > 0) {
				result.append(" 放杠X" + fang_gang);
			}
			if (jie_gang > 0) {
				result.append(" 接杠X" + jie_gang);
			}
			
			if(chase_count > 0 && player == GRR._banker_player){
				result.append(" 烧庄X" + chase_count);
			}
			
			GRR._result_des[player] = result.toString();
		}
	}
	
	public int switch_to_cards_data_bird(int cards_index[], int cards_data[]) {
		// 转换扑克
		int cbPosition = 0;
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (cards_index[i] > 0) {
				for (int j = 0; j < cards_index[i]; j++) {
					cards_data[cbPosition++] = _logic.switch_to_card_data(i);
				}
			}
		}
		return cbPosition;
	}
	
	public void set_niao_card(){
		//初始化鸟牌数组
		GRR._count_niao = get_ma_num();
		if(GRR._count_niao == 0)
			return;
		GRR._cards_data_niao=new int[get_ma_num()];
		for (int i = 0; i < GRR._count_niao; i++) {
			GRR._cards_data_niao[i] = GameConstants.INVALID_VALUE;
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			GRR._player_niao_count[i] = 0;
			for (int j = 0; j < GameConstants.MAX_NIAO_CARD; j++) {
				GRR._player_niao_cards[i][j] = GameConstants.INVALID_VALUE;
			}
		}
		int player_bird_count = get_player_bird_count();
		boolean zhuang_fei = player_bird_count == GRR._count_niao;
		
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		_logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count, GRR._count_niao, cbCardIndexTemp);
		GRR._left_card_count -= GRR._count_niao;
		switch_to_cards_data_bird(cbCardIndexTemp, GRR._cards_data_niao);
		
		if(zhuang_fei){
			for (int j = 0; j < player_bird_count; j++) {
				GRR._player_niao_cards[GRR._banker_player][j] = GRR._cards_data_niao[j];
				GRR._player_niao_count[GRR._banker_player]++;
			}
		}else{
			for (int j = 0; j < GRR._count_niao; j++) {
				int p = j / player_bird_count;
				int i = j % player_bird_count;
				GRR._player_niao_cards[p][i] = GRR._cards_data_niao[j];
				GRR._player_niao_count[p]++;
			}
		}
	}
	
	


	public boolean estimate_player_out_card_respond(int seat_index, int card, int type) {
		boolean bAroseAction = false;
		
		if(has_rule(Constants_MJ_PC.GAME_RULE_HZJ) || has_rule(Constants_MJ_PC.GAME_RULE_ONLY_HZJ)){
			if(card == Constants_MJ_PC.HONG_ZHONG_DATA)
				return bAroseAction;
		}

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
			if (GRR._left_card_count > get_ma_num()) {//如果牌堆已经没有牌，出的最后一张牌了不能碰，杠
				action = _logic.check_peng(GRR._cards_index[i], card);
				if (can_peng && action != 0) {
					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);
					bAroseAction = true;
				}

				action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
				if (action != 0) {
					playerStatus.add_action(GameConstants.WIK_GANG);
					playerStatus.add_gang(card, i, 1);
					bAroseAction = true;
				}
			}
			
			if (_playerStatus[i].is_chi_hu_round()) {
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				int card_type = Constants_MJ_PC.HU_CARD_TYPE_JIE_PAO;
				action = analyse_chi_hu_card(GRR._cards_index[i],
						GRR._weave_items[i], cbWeaveCount, card, chr,
						card_type, i);
				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);
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

	public int get_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					GameConstants.CHR_ZI_MO, seat_index)) {
				cards[count++] = cbCurrentCard;
			}
		}
		
		cbCurrentCard = _logic.switch_to_card_data(Constants_MJ_PC.HONG_ZHONG_INDEX);
		chr.set_empty();
		
		if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
				GameConstants.CHR_ZI_MO, seat_index)) {
			cards[count++] = cbCurrentCard;
		}
		
		//全听
		if (count == 28) {
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
            set_game_score(reason == GameConstants.Game_End_NORMAL);
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
                for (int j = 0; j < get_player_bird_count(); j++) {
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
                GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

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
    
	
	private void set_game_score(boolean b) {
		if(!b)
			return;
		if(chase_count > 0){
			int chase_score = 1 << (chase_count - 1);
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if(i == GRR._banker_player)
					continue;
				player_game_score[i][GRR._banker_player] += chase_score;
				player_game_score[GRR._banker_player][i] -= chase_score;
			}
		}
		int[][] player_double_count = new int[getTablePlayerNumber()][getTablePlayerNumber()];
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int n = 0; n < GRR._player_niao_count[i]; n++) {
				int value = (_logic.get_card_value(GRR._player_niao_cards[i][n]) - 1) % getTablePlayerNumber();
				int p = (GRR._banker_player+ value) % getTablePlayerNumber();
				player_double_count[i][p]++;	
			}

        }
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < getTablePlayerNumber(); j++) {
				int j_count = player_double_count[i][j];
				if(j_count > 0){
					for (int k = 0; k < getTablePlayerNumber(); k++) {
						if(k != i){
							GRR._game_score[i] += player_game_score[j][k] * j_count; 
							GRR._game_score[k] -= player_game_score[j][k] * j_count; 
						}
					}
				}
			}
		}
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < getTablePlayerNumber(); j++) {
				GRR._game_score[i] += player_game_score[i][j];
			}
		}
		for (int j = 0; j < getTablePlayerNumber(); j++) {
			_player_result.game_score[j] += GRR._game_score[j];
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
	
	public boolean is_yi_tiao_long(int cards_index[], WeaveItem weaveItem[], int weaveCount) {
		Set<Integer> set = new HashSet<Integer>();
		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			if(cards_index[i] == 0){
				set.add(_logic.get_card_color(_logic.switch_to_card_data(i)));
			}
 		}
		if(set.size() != 3){
			int[] copyOf = Arrays.copyOf(cards_index, cards_index.length);
			if(!set.contains(0)){
				for(int i = 0; i < 9; i++)
					copyOf[i]--;
			}else if(!set.contains(1)){
				for(int i = 9; i < 18; i++)
					copyOf[i]--;
			}else if(!set.contains(2)){
				for(int i = 18; i < 27; i++)
					copyOf[i]--;
			}
			boolean hu;
				hu = AnalyseCardUtil.analyse_win_by_cards_index(copyOf, -1, null, 0);
			if(hu)
				return hu;
		}
		
		// 落地一条龙判断
		/*if(weaveCount == 0)
			return false;
		for (int i = 0; i < weaveCount; i++) {
			if(weaveItem[i].center_card < 0x31){
				int value = _logic.get_card_value(weaveItem[i].center_card);
				int color = _logic.get_card_color(weaveItem[i].center_card);
				if((value == 1 && weaveItem[i].weave_kind == GameConstants.WIK_LEFT) ||
						(value == 2 && weaveItem[i].weave_kind == GameConstants.WIK_CENTER) ||
						(value == 3 && weaveItem[i].weave_kind == GameConstants.WIK_RIGHT)){
					int[] copyOf = Arrays.copyOf(cards_index, cards_index.length);
					boolean flag = true;
					for(int c = color * 9 + 3; c < color * 9 + 9; c++){
						if(copyOf[c] == 0){
							flag = false;
							break;
						}
						copyOf[c]--;
					}
					boolean hu = flag && AnalyseCardUtil.analyse_feng_chi_by_cards_index(copyOf, -1, null, 0);
					if(hu)
						return hu;
				}
				if((value == 4 && weaveItem[i].weave_kind == GameConstants.WIK_LEFT) ||
						(value == 5 && weaveItem[i].weave_kind == GameConstants.WIK_CENTER) ||
						(value == 6 && weaveItem[i].weave_kind == GameConstants.WIK_RIGHT)){
					int[] copyOf = Arrays.copyOf(cards_index, cards_index.length);
					boolean flag = true;
					for(int c = color * 9 + 0; c < color * 9 + 3; c++){
						if(copyOf[c] == 0){
							flag = false;
							break;
						}
						copyOf[c]--;
					}
					for(int c = color * 9 + 6; c < color * 9 + 9; c++){
						if(copyOf[c] == 0){
							flag = false;
							break;
						}
						copyOf[c]--;
					}
					boolean hu = flag && AnalyseCardUtil.analyse_feng_chi_by_cards_index(copyOf, -1, null, 0);
					if(hu)
						return hu;
				}
				if((value == 7 && weaveItem[i].weave_kind == GameConstants.WIK_LEFT) ||
						(value == 8 && weaveItem[i].weave_kind == GameConstants.WIK_CENTER) ||
						(value == 9 && weaveItem[i].weave_kind == GameConstants.WIK_RIGHT)){
					int[] copyOf = Arrays.copyOf(cards_index, cards_index.length);
					boolean flag = true;
					for(int c = color * 9 + 0; c < color * 9 + 6; c++){
						if(copyOf[c] == 0){
							flag = false;
							break;
						}
						copyOf[c]--;
					}
					boolean hu = flag && AnalyseCardUtil.analyse_feng_chi_by_cards_index(copyOf, -1, null, 0);
					if(hu)
						return hu;
				}
			}
		}*/
		
		return false;
	}
	
	@Override
	public int getTablePlayerNumber() {
		if (has_rule(Constants_MJ_PC.GAME_RULE_PLAYER_2))
			return 2;
		if (has_rule(Constants_MJ_PC.GAME_RULE_PLAYER_3))
			return 3;
		return 4;
	}
	
	@Override
	public boolean handler_request_trustee(int seat_index, boolean isTrustee, int Trustee_type) {
		// 游戏开始前 无法托管
		if (_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) {
			send_error_notify(seat_index, 2, "游戏还未开始,无法托管!");
			return false;
		}
		operate_trustee(seat_index, isTrustee);
		return true;
	}
	
	public boolean operate_trustee(int seat_index, boolean isTrustee) {
		istrustee[seat_index] = isTrustee;
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_IS_TRUSTEE);
		roomResponse.setOperatePlayer(seat_index);
		roomResponse.setIstrustee(isTrustee);
		send_response_to_room(roomResponse);
		if (GRR != null) {
			GRR.add_room_response(roomResponse);
		}
		return true;
	}
	
	public void operate_action_cancel_trustee(int seat_index) {
		if(!istrustee[seat_index]){
			if(_trustee_schedule[seat_index] != null){
				_trustee_schedule[seat_index].cancel(false);
				_trustee_schedule[seat_index] = null;
			}
		}
	}
	
	@SuppressWarnings("rawtypes")
	public void execute_trustee(int seat_index){
		PlayerStatus playerStatus = _playerStatus[seat_index];
		if(istrustee[seat_index]){
			if (playerStatus.has_action() && playerStatus.is_respone() == false) {
				operate_player_action(seat_index, true);
				_handler.handler_operate_card(this, seat_index, 0, 0);
			} else{
				if(_handler instanceof MJHandlerChiPeng){
					HandlerChiPeng_PuCheng handlerChiPeng = (HandlerChiPeng_PuCheng)((MJHandlerChiPeng)_handler);
					if(handlerChiPeng._seat_index == seat_index){
						int cards[] = new int[GameConstants.MAX_COUNT];
						int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], cards);
						GameSchedule.put(() -> {
							handlerChiPeng.handler_player_out_card(this, seat_index, cards[hand_card_count - 1]);
						}, 500, TimeUnit.MILLISECONDS);
					}
				}else if(_handler instanceof MJHandlerDispatchCard){
					HandlerDispatchCard_PuCheng handlerDispatchCard = (HandlerDispatchCard_PuCheng)((MJHandlerDispatchCard)_handler);
					if(handlerDispatchCard._seat_index == seat_index){
						GameSchedule.put(() -> {
							handlerDispatchCard.handler_player_out_card(this, seat_index, handlerDispatchCard._send_card_data);
						}, 500, TimeUnit.MILLISECONDS);
					}
				}
			}
		}else{
			if(action_wait_time != -1){
				if (playerStatus.has_action() && playerStatus.is_respone() == false){
					change_player_status(seat_index, GameConstants.Player_Status_OPR_CARD);
					operate_player_action(seat_index, false);
					_trustee_schedule[seat_index] = GameSchedule.put(() -> {
						ReentrantLock lock = getRoomLock();
						try {
							lock.lock();
							if(!istrustee[seat_index]){
								// 关闭操作按钮
								operate_player_action(seat_index, true);
								operate_trustee(seat_index, true);
								_handler.handler_operate_card(this, seat_index, 0, 0);
							}
						} finally {
							lock.unlock();
						}
					}, action_wait_time, TimeUnit.SECONDS);
				}else{
					if(_handler instanceof MJHandlerChiPeng){
						HandlerChiPeng_PuCheng handlerChiPeng = (HandlerChiPeng_PuCheng)((MJHandlerChiPeng)_handler);
						if(handlerChiPeng._seat_index == seat_index){
							change_player_status(seat_index, GameConstants.Player_Status_OUT_CARD);
							operate_player_status();
							int cards[] = new int[GameConstants.MAX_COUNT];
							int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], cards);
							_trustee_schedule[seat_index] = GameSchedule.put(() -> {
								ReentrantLock lock = getRoomLock();
								try {
									lock.lock();
									if(!istrustee[seat_index]){
										operate_trustee(seat_index, true);
										handlerChiPeng.handler_player_out_card(this, seat_index, cards[hand_card_count - 1]);
									}
								} finally {
									lock.unlock();
								}
							}, action_wait_time, TimeUnit.SECONDS);
						}
					}else if(_handler instanceof MJHandlerDispatchCard){
						HandlerDispatchCard_PuCheng handlerDispatchCard = (HandlerDispatchCard_PuCheng)((MJHandlerDispatchCard)_handler);
						if(handlerDispatchCard._seat_index == seat_index){
							change_player_status(seat_index, GameConstants.Player_Status_OUT_CARD);
							operate_player_status();
							_trustee_schedule[seat_index] = GameSchedule.put(() -> {
								ReentrantLock lock = getRoomLock();
								try {
									lock.lock();
									if(!istrustee[seat_index]){
										operate_trustee(seat_index, true);
										handlerDispatchCard.handler_player_out_card(this, seat_index, handlerDispatchCard._send_card_data);
									}
								} finally {
									lock.unlock();
								}
							}, action_wait_time, TimeUnit.SECONDS);
						}
					}
				}
			}else {
				if (playerStatus.has_action() && playerStatus.is_respone() == false){
					change_player_status(seat_index, GameConstants.Player_Status_OPR_CARD);
					operate_player_action(seat_index, false);
				}else{
					change_player_status(seat_index, GameConstants.Player_Status_OUT_CARD);
					operate_player_status();
				}
			}
		}
	}
	
	public int get_ma_num() {
		if(has_rule(Constants_MJ_PC.GAME_RULE_FEI_ZHUANG_1)) {
			return 1;
		}
		if(has_rule(Constants_MJ_PC.GAME_RULE_FEI_ZHUANG_2)) {
			return 2;
		}
		if(has_rule(Constants_MJ_PC.GAME_RULE_FEI_ZHUANG_4)) {
			return 4;
		}
		if (has_rule(Constants_MJ_PC.GAME_RULE_FEI_EVERY_1)) {
			return getTablePlayerNumber() * 1;
		}
		if (has_rule(Constants_MJ_PC.GAME_RULE_FEI_EVERY_2)) {
			return getTablePlayerNumber() * 2;
		}
		if (has_rule(Constants_MJ_PC.GAME_RULE_FEI_EVERY_4)) {
			return getTablePlayerNumber() * 4;
		}          
		return 0;
	}
	public int get_player_bird_count() {
		if(has_rule(Constants_MJ_PC.GAME_RULE_FEI_ZHUANG_1)) {
			return 1;
		}
		if(has_rule(Constants_MJ_PC.GAME_RULE_FEI_ZHUANG_2)) {
			return 2;
		}
		if(has_rule(Constants_MJ_PC.GAME_RULE_FEI_ZHUANG_4)) {
			return 4;
		}
		if (has_rule(Constants_MJ_PC.GAME_RULE_FEI_EVERY_1)) {
			return 1;
		}
		if (has_rule(Constants_MJ_PC.GAME_RULE_FEI_EVERY_2)) {
			return 2;
		}
		if (has_rule(Constants_MJ_PC.GAME_RULE_FEI_EVERY_4)) {
			return 4;
		}          
		return 0;
	}
	public int get_action_wait_time() {
		if(has_rule(Constants_MJ_PC.GAME_RULE_OVERTIME_10S)) {
			return 10;
		}
		if(has_rule(Constants_MJ_PC.GAME_RULE_OVERTIME_1M)) {
			return 60;
		}
		if (has_rule(Constants_MJ_PC.GAME_RULE_OVERTIME_5M)) {
			return 300;
		}
		return -1;
	}
	
    @Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x11,0x11,0x11,0x11,0x15,0x15,0x15,0x15,0x22,0x22,0x22,0x03,0x35 };
		int[] cards_of_player1 = new int[] { 0x21,0x21,0x21,0x11,0x11,0x11,0x19,0x19,0x19,0x29,0x29,0x29,0x35 };
		int[] cards_of_player2 = new int[] { 0x21,0x21,0x21,0x11,0x11,0x11,0x19,0x19,0x19,0x29,0x29,0x29,0x35 };
		int[] cards_of_player3 = new int[] { 0x21,0x21,0x21,0x11,0x11,0x11,0x19,0x19,0x19,0x29,0x29,0x29,0x35 };

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
