/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.game.mj.handler.hunanliling;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.define.ECardType;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.PerformanceTimer;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJConstants;
import com.cai.game.mj.MJGameLogic.AnalyseItem;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.game.util.GameUtilConstants;
import com.cai.game.mj.MJType;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;
import com.cai.util.SysParamServerUtil;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

/**
 * 湖南醴陵麻将Table
 * @author WalkerGeek 
 * date: 2017年9月19日 下午2:58:26 <br/>
 */
public class MJTable_LL extends AbstractMJTable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public MJTable_LL(){
		super(MJType.GAME_TYPE_HUNAN_LILING_TYPE);
	}
	
	
	public MJHandlerQiShouHongZhong_LL _handler_qishou_hongzhong;
	
	public MJHandlerPiao_LL handlerPiao_LL;
	
	/**
	 *  玩家出牌的动作检测
	 * @param seat_index
	 * @param card
	 * @return
	 */
	public boolean estimate_player_out_card_respond(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		// 用户状态
		for (int i = 0; i <getTablePlayerNumber(); i++) {

			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		int llcard = get_niao_card_num(true, 0);
		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		// 动作判断
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			//// 碰牌判断
			action = _logic.check_peng(GRR._cards_index[i], card);
			if (action != 0) {
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

			// 红中不能胡
			// 可以胡的情况 判断
			if (_playerStatus[i].is_chi_hu_round()) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr, GameConstants.HU_CARD_TYPE_PAOHU);
				
				// 结果判断
				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
					bAroseAction = true;
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
	
	
	@Override
	public int getTablePlayerNumber() {
		
		if(playerNumber > 0){
			return playerNumber;
		}
		if (has_rule(GameConstants.GAME_RULE_HUNAN_THREE)) {
				return GameConstants.GAME_PLAYER-1;
		}else{
			return GameConstants.GAME_PLAYER;
		}
	}
	
	
	
	public int analyse_chi_hu_card(int cards_index[], WeaveItem weaveItems[], int weaveCount, int cur_card, ChiHuRight chiHuRight, int card_type) {
		if (SysParamServerUtil.is_new_algorithm(3000, 3000, 1)) {
            return analyse_chi_hu_card_new(cards_index, weaveItems, weaveCount, cur_card, chiHuRight, card_type);
        } else {
            return analyse_chi_hu_card_old(cards_index, weaveItems, weaveCount, cur_card, chiHuRight, card_type);
        }
	}
	
	/***
	 * 麻将胡牌解析
	 * @param cards_index
	 * @param weaveItems
	 * @param weaveCount
	 * @param cur_card
	 * @param chiHuRight
	 * @param card_type
	 * @return
	 */
	public int analyse_chi_hu_card_old(int cards_index[], WeaveItem weaveItems[], int weaveCount, int cur_card, ChiHuRight chiHuRight, int card_type) {
		if ((has_rule(GameConstants.GAME_RULE_HUNAN_ZIMOHU) && (card_type == GameConstants.HU_CARD_TYPE_PAOHU)))
		{
			return GameConstants.WIK_NULL;
		}

		// cbCurrentCard一定不为0 !!!!!!!!!
		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		// 变量定义
		int cbChiHuKind = GameConstants.WIK_NULL;

		if (has_rule(GameConstants.GAME_RULE_HUNAN_QIDUI) || is_mj_type(GameConstants.GAME_TYPE_HENAN_HZ)) { // 7.8

			// 七小对牌 豪华七小对
			long qxd = _logic.is_qi_xiao_dui(cards_index, weaveItems, weaveCount, cur_card);
			if (qxd != GameConstants.WIK_NULL) {
				cbChiHuKind = GameConstants.WIK_CHI_HU;
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
					// cbChiHuKind = MJGameConstants.WIK_CHI_HU;
					chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
				} else {
					chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);

				}
				// 红中七小不算大胡
				// chiHuRight.opr_or(qxd);
			}

		}

		if ((cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD)] == 4)
				|| ((cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD)] == 3) && (cur_card == GameConstants.HZ_MAGIC_CARD))) {

			cbChiHuKind = GameConstants.WIK_CHI_HU;
			if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
				chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
			} else {
				// 这个没必要。一定是自摸
				chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
			}
		}

		if (chiHuRight.is_empty() == false) {

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

		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
		// 分析扑克
		boolean bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weaveCount, analyseItemArray, true);
		if (!bValue) {

			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;

		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
			// cbChiHuKind = MJGameConstants.WIK_CHI_HU;

			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else {
			chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);

		}

		return cbChiHuKind;
	}
	
	
	 public int analyse_chi_hu_card_new(int cards_index[], WeaveItem weaveItems[], int weaveCount, int cur_card,
	            ChiHuRight chiHuRight, int card_type) {
	        if ((has_rule(GameConstants.GAME_RULE_HUNAN_ZIMOHU) && (card_type == GameConstants.HU_CARD_TYPE_PAOHU)))
	            return GameConstants.WIK_NULL;

	        if (cur_card == 0) {
	            return GameConstants.WIK_NULL;
	        }

	        int cbChiHuKind = GameConstants.WIK_NULL;
	        
	        if (has_rule(GameConstants.GAME_RULE_HUNAN_QIDUI) || is_mj_type(GameConstants.GAME_TYPE_HENAN_HZ)) { // 7.8
		        long qxd = _logic.is_qi_xiao_dui(cards_index, weaveItems, weaveCount, cur_card);
		        if (qxd != GameConstants.WIK_NULL) {
		            cbChiHuKind = GameConstants.WIK_CHI_HU;
		            if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
		                chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		            } else {
		                chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
	
		            }
		        }
	        }

	        if ((cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD)] == 4)
	                || ((cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD)] == 3)
	                        && (cur_card == GameConstants.HZ_MAGIC_CARD))) {

	            cbChiHuKind = GameConstants.WIK_CHI_HU;
	            if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
	                chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
	            } else {
	                chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
	            }
	        }

	        if (chiHuRight.is_empty() == false) {
	            return cbChiHuKind;
	        }

	        int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
	        int magic_card_count = _logic.get_magic_card_count();

	        if (magic_card_count > 2) { // 一般只有两种癞子牌存在
	            magic_card_count = 2;
	        }

	        for (int i = 0; i < magic_card_count; i++) {
	            magic_cards_index[i] = _logic.get_magic_card_index(i);
	        }

	        boolean can_win = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
	                magic_cards_index, magic_card_count);

	        if (!can_win) {
	            chiHuRight.set_empty();
	            return GameConstants.WIK_NULL;
	        }

	        cbChiHuKind = GameConstants.WIK_CHI_HU;

	        if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
	            chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
	        } else {
	            chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
	        }

	        return cbChiHuKind;
	    }

	
	 
	public int get_hz_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount,
	            boolean dai_feng) {
        if (SysParamServerUtil.is_new_algorithm(3000, 3000, 1)) {
            return get_hz_ting_card_new(cards, cards_index, weaveItem, cbWeaveCount, dai_feng);
        } else {
            return 0;
        }
	}
	
	public int get_hz_ting_card_new(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount,
            boolean dai_feng) {
        PerformanceTimer timer = new PerformanceTimer();

        int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
        for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
            cbCardIndexTemp[i] = cards_index[i];
        }

        ChiHuRight chr = new ChiHuRight();

        int count = 0;
        int cbCurrentCard;

        int l = GameConstants.MAX_INDEX - GameConstants.CARD_HUA_COUNT - GameConstants.CARD_FENG_COUNT;
        int ql = l;
        if (dai_feng) {
            l += GameConstants.CARD_FENG_COUNT;
            ql += (GameConstants.CARD_FENG_COUNT - 1);
        }
        for (int i = 0; i < l; i++) {
            if (this._logic.is_magic_index(i))
                continue;
            cbCurrentCard = _logic.switch_to_card_data(i);
            chr.set_empty();
            if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount,
                    cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO)) {
                cards[count] = cbCurrentCard;
                count++;
            }
        }

        l -= 1;
        
        if (count == 0) {
			
		} else if (count > 0 && count < ql) {
			// 有胡的牌。红中肯定能胡
			if(this._logic.get_magic_card_count() != 0){
				cards[count] = _logic.switch_to_card_data(this._logic.get_magic_card_index(0));
				count++;
			}
		} else {
			// 全听
			count = 1;
			cards[0] = -1;
		}

        if (timer.get() > 50) {
            logger.warn("cost time too long " + Arrays.toString(cards_index) + ", cost time = " + timer.duration());
        }

        return count;
    }
	
	/*public int get_hz_ting_card_old(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, boolean dai_feng) {

		// 复制数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int l = GameConstants.MAX_INDEX - GameConstants.CARD_HUA_COUNT - GameConstants.CARD_FENG_COUNT;
		int ql = l;
		if (dai_feng) {
			l += GameConstants.CARD_FENG_COUNT;
			ql += (GameConstants.CARD_FENG_COUNT - 1);
		}
		for (int i = 0; i < l; i++) {
			if (this._logic.is_magic_index(i))
				continue;
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					GameConstants.HU_CARD_TYPE_ZIMO)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}

		l -= 1;
		if (count == 0) {
			// 红中能不能胡
			// cbCurrentCard =
			// _logic.switch_to_card_data(this._logic.get_magic_card_index(0));
			// if( MJGameConstants.WIK_CHI_HU == analyse_chi_hu_card_hz(
			// cbCardIndexTemp,weaveItem,cbWeaveCount,cbCurrentCard,chr,MJGameConstants.HU_CARD_TYPE_ZIMO
			// ) ){
			// cards[count] = cbCurrentCard;
			// count++;
			// }
		} else if (count > 0 && count < ql) {
			// 有胡的牌。红中肯定能胡
			if(this._logic.get_magic_card_count() != 0){
				cards[count] = _logic.switch_to_card_data(this._logic.get_magic_card_index(0));
				count++;
			}
		} else {
			// 全听
			count = 1;
			cards[0] = -1;
		}

		return count;
	}*/
	
	
	
	/**
	 * 获取鸟的 数量
	 * @param check
	 * @param add_niao
	 * @return
	 */
	public int get_niao_card_num(boolean check, int add_niao) {
		int nNum = GameConstants.ZHANIAO_0;
		if (has_rule(GameConstants.GAME_RULE_HUNAN_ZHANIAO1)) {
			nNum = GameConstants.ZHANIAO_1;
		} else if (has_rule(GameConstants.GAME_RULE_HUNAN_ZHANIAO2)) {
			nNum = GameConstants.ZHANIAO_2;
		} else if (has_rule(GameConstants.GAME_RULE_HUNAN_ZHANIAO4)) {
			nNum = GameConstants.ZHANIAO_4;
		} else if (has_rule(GameConstants.GAME_RULE_HUNAN_ZHANIAO6)) {
			nNum = GameConstants.ZHANIAO_6;
		}
		nNum += add_niao;

		if (check == false) {
			return nNum;
		}
		if (nNum > GRR._left_card_count) {
			nNum = GRR._left_card_count;
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
	 */
	public void set_niao_card(int seat_index, int card, boolean show, int add_niao) {
		for (int i = 0; i < GameConstants.MAX_NIAO_CARD; i++) {
			GRR._cards_data_niao[i] = GameConstants.INVALID_VALUE;
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			GRR._player_niao_count[i] = 0;
			for (int j = 0; j < GameConstants.MAX_NIAO_CARD; j++) {
				GRR._player_niao_cards[i][j] = GameConstants.INVALID_VALUE;
			}

		}
		GRR._show_bird_effect = show;
		if (card == GameConstants.INVALID_VALUE) {
			GRR._count_niao = get_niao_card_num(true, add_niao);
		} else {
			GRR._count_niao = get_niao_card_num(false, add_niao);
		}

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
			int seat = get_seat_by_value(nValue, seat_index);
			GRR._player_niao_cards[seat][GRR._player_niao_count[seat]] = GRR._cards_data_niao[i];
			GRR._player_niao_count[seat]++;
		}
	}
	
	public boolean exe_qishou_hongzhong(int seat_index) {
		this.set_handler(this._handler_qishou_hongzhong);
		this._handler_qishou_hongzhong.reset_status(seat_index);
		this._handler_qishou_hongzhong.exe(this);

		return true;
	}
	
	
	@Override
	protected void onInitTable() {
		_handler_dispath_card = new MJHandlerDispatchCard_LL();
		_handler_out_card_operate = new MJHandlerOutCardOperate_LL();
		_handler_gang = new MJHandlerGang_LL();
		_handler_chi_peng = new MJHandlerChiPeng_LL();

		_handler_qishou_hongzhong = new MJHandlerQiShouHongZhong_LL();
		handlerPiao_LL = new MJHandlerPiao_LL();
	}

	
	/**
	 * 初始化洗牌
	 */
	@Override
	public void init_shuffle(){
		int[] cards = null;
		if(has_rule(GameConstants.GAME_RULE_HUNAN_HONGZHONG)){
			cards = MJConstants.CARD_DATA_HNCZ;
			// 设置红中为癞子
			_logic.clean_magic_cards();
			_logic.add_magic_card_index(_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD));
		}else{
			cards = MJConstants.CARD_DATA_WAN_TIAO_TONG;
		}
		
		_repertory_card = new int[cards.length];
		shuffle(_repertory_card,cards);
	}
	
	/* (non-Javadoc)
	 * @see com.cai.game.mj.AbstractMJTable#on_game_start()
	 */
	@Override
	protected boolean on_game_start() {
		//飘分规则
		if (has_rule(GameConstants.GAME_RULE_HUNAN_CS_PIAO)) {
			this.set_handler(this.handlerPiao_LL);
			this.handlerPiao_LL.exe(this);
			return true;
		} else {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				_player_result.pao[i] = 0;// 清掉 默认是-1
			}
		}
		return game_start_real();
	}
	
	public boolean game_start_real(){
		_game_status = GameConstants.GS_MJ_PLAY;// 设置状态
		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		// gameStartResponse.setSiceIndex(rand);
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants.MAX_COUNT];
		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards[i]);
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
			GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.load_room_info_data(roomResponse);
			this.load_common_status(roomResponse);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setCurrentPlayer(_current_player == GameConstants.INVALID_SEAT ? _resume_player : _current_player);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			roomResponse.setGameStatus(_game_status);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			this.send_response_to_player(i, roomResponse);
		}
		////////////////////////////////////////////////// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		this.load_room_info_data(roomResponse);
		this.load_common_status(roomResponse);
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
		///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		// 检测听牌
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i]._hu_card_count = this.get_hz_ting_card(_playerStatus[i]._hu_cards, GRR._cards_index[i], GRR._weave_items[i],
					GRR._weave_count[i], false);
			if (_playerStatus[i]._hu_card_count > 0) {
				this.operate_chi_hu_cards(i, _playerStatus[i]._hu_card_count, _playerStatus[i]._hu_cards);
			}
		}
		boolean is_qishou_hu = false;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 起手4个红中
			
			if (this._logic.get_magic_card_count() != 0 && GRR._cards_index[i][_logic.get_magic_card_index(0)] == 4) {

				_playerStatus[i].add_action(GameConstants.WIK_ZI_MO);
				_playerStatus[i].add_zi_mo(_logic.switch_to_card_data(_logic.get_magic_card_index(0)), i);
				GRR._chi_hu_rights[i].opr_or(GameConstants.CHR_ZI_MO);
				GRR._chi_hu_rights[i].opr_or(GameConstants.CHR_HUNAN_HZ_QISHOU_HU);
				this.exe_qishou_hongzhong(i);

				is_qishou_hu = true;
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[i], ECardType.hongZhong4, "", 0, 0l, this.getRoom_id());
				break;
			}
		}
		if (is_qishou_hu == false) {
			this.exe_dispatch_card(_current_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);
		}
		// this.exe_dispatch_card(_current_player,true);

		return false;
	}

	
	@Override
	public boolean handler_requst_pao_qiang(Player player, int pao, int qiang) {

		if (handlerPiao_LL != null) {
			return handlerPiao_LL.handler_pao_qiang(this, player.get_seat_index(), pao, qiang);
		} 

		return false;

	}
	
	/* (non-Javadoc)
	 * @see com.cai.game.mj.AbstractMJTable#process_chi_hu_player_score(int, int, int, boolean)
	 */
	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {

		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int wFanShu = 0;// 番数
			wFanShu = _logic.get_chi_hu_action_rank_ll(chr, getTablePlayerNumber()); // walkergeek

		countCardType(chr, seat_index);

		int lChiHuScore = wFanShu * GameConstants.CELL_SCORE;

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
			GRR._lost_fan_shu[provide_index][seat_index] = wFanShu;//
		}
		//每个鸟分数
		int niaoScore = 1;
		if(has_rule_ex(GameConstants.GAME_RULE_HUNAN_NIAO_FEN_2)){
			niaoScore = 2;
		}
		
		/////////////////////////////////////////////// 算分//////////////////////////
		GRR._count_pick_niao = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GRR._player_niao_count[i]; j++) {
				//////////////////////////////////////////////// 转转麻将抓鸟//////////////////只要159就算
				int nValue = GRR._player_niao_cards[i][j];
				nValue = nValue > 1000 ? (nValue - 1000) : nValue;
				int v = _logic.get_card_value(nValue);
				if (v == 1 || v == 5 || v == 9) {
					GRR._count_pick_niao++;
					GRR._player_niao_cards[i][j] = this.set_ding_niao_valid(GRR._player_niao_cards[i][j], true);// 胡牌的鸟生效
				} else {
					GRR._player_niao_cards[i][j] = this.set_ding_niao_valid(GRR._player_niao_cards[i][j], false);// 胡牌的鸟失效
				}
			}
		}

		////////////////////////////////////////////////////// 自摸 算分
		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}

				int s = lChiHuScore;
				
				//鸟分
				s += GRR._count_pick_niao * niaoScore;
				// 飘分
				s += ((_player_result.pao[i] < 0 ? 0 : _player_result.pao[i])
						+ (_player_result.pao[seat_index] < 0 ? 0 : _player_result.pao[seat_index]));
				
				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;
			}
		}
		////////////////////////////////////////////////////// 点炮 算分
		else {
			int s = lChiHuScore;
			//鸟分
			s += GRR._count_pick_niao * niaoScore;
			// 飘分
			s += ((_player_result.pao[provide_index] < 0 ? 0 : _player_result.pao[provide_index])
					+ (_player_result.pao[seat_index] < 0 ? 0 : _player_result.pao[seat_index]));
			
			GRR._game_score[provide_index] -= s;
			GRR._game_score[seat_index] += s;

			// 点炮的时候，删掉这张牌显示
			// GRR._cards_index[seat_index][_logic.switch_to_card_index(_provide_card)]--;

			GRR._chi_hu_rights[provide_index].opr_or(GameConstants.CHR_FANG_PAO);

		}

		GRR._provider[seat_index] = provide_index;
		// 设置变量

		_status_gang = false;
		_status_gang_hou_pao = false;

		// _playerStatus[seat_index].clean_status();
		change_player_status(seat_index, GameConstants.INVALID_VALUE);
		return;
		
	}

	/**
	 * 
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
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr, GameConstants.HU_CARD_TYPE_QIANGGANG);

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
	
	
	/* (non-Javadoc)
	 * @see com.cai.game.mj.AbstractMJTable#set_result_describe()
	 */
	@Override
	protected void set_result_describe() {
		int l;
		long type = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			String des = "";

			l = GRR._chi_hu_rights[i].type_count;
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					if (type == GameConstants.CHR_TONG_PAO) {
						des += " 通炮";
					}
					if (type == GameConstants.CHR_ZI_MO) {
						if (!(GRR._chi_hu_rights[i].opr_and(GameConstants.CHR_HUNAN_HZ_QISHOU_HU).is_empty())) {
							des += " 起手自摸";
						} else {
							des += " 自摸";
						}

						if (GRR._count_pick_niao > 0) {
							des += " 中鸟X" + GRR._count_pick_niao;
						}
					}
					if (type == GameConstants.CHR_SHU_FAN) {
						des += " 接炮";
						if (GRR._count_pick_niao > 0) {
							des += " 中鸟X" + GRR._count_pick_niao;
						}
					}
					if (type == GameConstants.CHR_HUNAN_QIANG_GANG_HU) {
						des += " 抢杠胡";
					}
				} else {
					if (type == GameConstants.CHR_FANG_PAO) {
						des += " 放炮";
					}
				} 
			}
			int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0;
			if (GRR != null) {
				for (int p = 0; p < getTablePlayerNumber(); p++) {
					for (int w = 0; w < GRR._weave_count[p]; w++) {
						if (GRR._weave_items[p][w].weave_kind != GameConstants.WIK_GANG) {
							continue;
						}
						if (p == i) {// 自己
							// 接杠
							if (GRR._weave_items[p][w].provide_player != p) {
								jie_gang++;
							} else {
								if (GRR._weave_items[p][w].public_card == 1) {// 明杠
									ming_gang++;
								} else {
									an_gang++;
								}
							}
						} else {
							// 放杠
							if (GRR._weave_items[p][w].provide_player == i) {
								fang_gang++;
							}
						}
					}
				}
			}

			if (an_gang > 0) {
				des += " 暗杠X" + an_gang;
			}
			if (ming_gang > 0) {
				des += " 明杠X" + ming_gang;
			}
			if (fang_gang > 0) {
				des += " 放杠X" + fang_gang;
			}
			if (jie_gang > 0) {
				des += " 接杠X" + jie_gang;
			}

			GRR._result_des[i] = des;
		}
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

		roomResponse.setLeftCardCount(0);

		this.load_common_status(roomResponse);
		this.load_room_info_data(roomResponse);

		// 这里记录了两次，先这样
		RoomInfo.Builder room_info = getRoomInfo();
		game_end.setRoomInfo(room_info);
		game_end.setRunPlayerId(_run_player_id);
		game_end.setRoundOverType(0);
		game_end.setGamePlayerNumber(getTablePlayerNumber());
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			game_end.addPao(_player_result.pao[i]);
		}
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

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				//GRR._game_score[i] += lGangScore[i];
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

			// 鸟的数据 这里要注意三人场的特殊处理 三人场必须发四个人的鸟 不然显示不全
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				// 定鸟
				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					int card = GRR._player_niao_cards[i][j];
					if (card < GameConstants.DING_NIAO_INVALID) {
						card += GameConstants.DING_NIAO_INVALID;
					}
					pnc.addItem(card);
				}

				// 飞鸟
				for (int j = 0; j < GRR._player_niao_count_fei[i]; j++) {
					int card = GRR._player_niao_cards_fei[i][j];
					if (card < GameConstants.DING_NIAO_INVALID) {
						card += GameConstants.FEI_NIAO_INVALID;
					}
					pnc.addItem(card);
				}
				game_end.addPlayerNiaoCards(pnc);
			}
			
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

	
	
	/* (non-Javadoc)
	 * @see com.cai.common.domain.Room#trustee_timer(int, int)
	 */
	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		// WalkerGeek Auto-generated method stub
		return false;
	}
	
	/* (non-Javadoc)
	 * @see com.cai.game.mj.AbstractMJTable#analyse_chi_hu_card(int[], com.cai.common.domain.WeaveItem[], int, int, com.cai.common.domain.ChiHuRight, int, int)
	 */
	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card,
			ChiHuRight chiHuRight, int card_type, int _seat_index) {
		// WalkerGeek Auto-generated method stub
		return 0;
	}


}
