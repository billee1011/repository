package com.cai.game.mj.handler.hunanhengyang;

import java.util.ArrayList;
import java.util.List;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.GameDescUtil;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJConstants;
import com.cai.game.mj.MJGameLogic.AnalyseItem;
import com.cai.game.mj.MJType;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

/**
 * 湖南衡阳麻将Table
 *
 * @author WalkerGeek 
 * date: 2017年8月7日 下午6:14:35 <br/>
 */
public class MJTable_HY extends AbstractMJTable {
	
	private static final long serialVersionUID = 1L;
	
	protected MJHandlerGang_HY_DispatchCard _handler_gang_hy;
	protected MJHandlerHaiDi_HY _handler_hai_di_hy;
	protected MJHandlerYaoHaiDi_HY _handler_yao_hai_di_hy;
	protected MJHandlerWangBa_HY _handler_wangba_hy;
	
	
	public MJTable_HY() {
		super(MJType.GAME_TYPE_HUNAN_HENGYANG);
	}
	
	
	/**
	 * 海底
	 * @param seat_index
	 * @return
	 */
	public boolean exe_hai_di(int start_index, int seat_index) {
		this.set_handler(this._handler_yao_hai_di_hy);
		this._handler_yao_hai_di_hy.reset_status(start_index);
		this._handler_yao_hai_di_hy.exe(this);
		return true;
	}
	
	/**
	 * 要海底
	 * 
	 * @param seat_index
	 * @return
	 */
	public boolean exe_yao_hai_di(int seat_index) {
		this.set_handler(this._handler_yao_hai_di);
		this._handler_yao_hai_di.reset_status(seat_index);
		this._handler_yao_hai_di.exe(this);
		return true;
	}
	
	
	/**
	 * 杠牌处理
	 * 
	 * @param seat_index
	 * @param d
	 * @return
	 */
	public boolean exe_gang_hy(int seat_index, boolean d) {
		this.set_handler(this._handler_gang_hy);
		this._handler_gang_hy.reset_status(seat_index, d);
		this._handler_gang_hy.exe(this);
		return true;
	}
	/**
	 * 王霸处理
	 * 
	 * @param seat_index
	 * @param d
	 * @return
	 */
	public boolean exe_wangba_hy(int seat_index) {
		this.set_handler(this._handler_wangba_hy);
		this._handler_wangba_hy.reset_status(seat_index);
		this._handler_wangba_hy.exe(this);
		return true;
	}
	
	// 是否听牌
	public boolean is_hy_ting_card(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		int handcount = _logic.get_card_count_by_index(cards_index);
		if (handcount == 1) {
			// 全求人
			return true;
		}

		// 复制数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();
		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			chr.set_empty();
			int cbCurrentCard = _logic.switch_to_card_data(i);
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard,
					chr, GameConstants.HU_CARD_TYPE_ZIMO, seat_index,true))
				return true;
		}
		return false;
	}
	
	
	/**
	 * 衡阳
	 * @param seat_index
	 * @param operate_card
	 * @param rm
	 */
	public void process_chi_hu_player_operate_hy(int seat_index, int operate_card[], int card_count, boolean rm) {
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		// 效果
		this.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, chr.type_count, chr.type_list, 1,
				GameConstants.INVALID_SEAT);

		// 手牌删掉
		this.operate_player_cards(seat_index, 0, null, 0, null);

		if (rm) {
			// 把摸的牌从手牌删掉,结算的时候不显示这张牌的
			for (int i = 0; i < card_count; i++) {
				GRR._cards_index[seat_index][_logic.switch_to_card_index(operate_card[i])]--;
			}
		}

		// 显示胡牌
		int cards[] = new int[GameConstants.MAX_INDEX];
		int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], cards);
		for (int i = 0; i < card_count; i++) {
			cards[hand_card_count++] = operate_card[i] + GameConstants.CARD_ESPECIAL_TYPE_HU;
		}
		this.operate_show_card(seat_index, GameConstants.Show_Card_HU, hand_card_count, cards,
				GameConstants.INVALID_SEAT);

		return;
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
	public void set_niao_card_hy(int seat_index, int card, boolean show, int add_niao, boolean isTongPao,int wo_wo_niao) {
		for (int i = 0; i < GameConstants.MAX_NIAO_CARD; i++) {
			GRR._cards_data_niao[i] = GameConstants.INVALID_VALUE;
		}

		for (int i = 0; i <getTablePlayerNumber(); i++) {
			GRR._player_niao_count[i] = 0;
			for (int j = 0; j < GameConstants.MAX_NIAO_CARD; j++) {
				GRR._player_niao_cards[i][j] = GameConstants.INVALID_VALUE;
			}

		}
		GRR._show_bird_effect = show;
		if (card == GameConstants.INVALID_VALUE) {
			GRR._count_niao = get_ding_niao_card_num_hy(true,wo_wo_niao);
		} else {
			//
			GRR._count_niao = get_ding_niao_card_num_hy(false,wo_wo_niao);
		}
		

		if (GRR._count_niao > GameConstants.ZHANIAO_0) {
			if (card == GameConstants.INVALID_VALUE) {
				int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
				_logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count, GRR._count_niao,
						cbCardIndexTemp);
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
			int seat = 0;
			seat = get_zhong_seat_by_value_hy(nValue, seat_index);
			GRR._player_niao_cards[seat][GRR._player_niao_count[seat]] = GRR._cards_data_niao[i];
			GRR._player_niao_count[seat]++;
		}

	/*	if (!isTongPao) {
			set_niao_card_hy_fei(seat_index, card, show, add_niao);
		}*/

	}
	
	/**
	 * 获取窝窝鸟数量
	 * @param card
	 * @return
	 */
	public int getWoWoNiaoNum(int card){
		int num = 0;
		
		return num;
	}
	
	private int get_zhong_seat_by_value_hy(int nValue, int banker_seat) {
		int seat = 0;
		if (getTablePlayerNumber() == 4) {
			seat = (banker_seat + (nValue - 1) % 4) % 4;
		} else {// 3人场//这里这么特殊处理是因为 算鸟是根据玩家逻辑位置,只有庄家上家,庄家下家,不存在对家
			int v = (nValue - 1) % 4;
			switch (v) {
			case 0:// 本家//159
				seat = GRR._banker_player;
				break;
			case 1:// 26//下家
				seat = get_banker_next_seat(GRR._banker_player);
				break;
			case 2:// 37//对家
				seat = get_null_seat();
				break;
			default:// 48//上家
				seat = get_banker_pre_seat(GRR._banker_player);
				break;
			}
		}
		return seat;
	}
	
	
	/**
	 * 获取长沙能抓的定鸟的 数量
	 * 
	 * @return
	 */
	public int get_ding_niao_card_num_hy(boolean check,int wo_niao_card) {
		int nNum = gethyDingNiaoNum(wo_niao_card);
		if (check == false) {
			return nNum;
		}
		if (nNum > GRR._left_card_count) {
			nNum = GRR._left_card_count;
		}
		return nNum;
	}
	
	
	/**
	 *  定鸟 个数
	 * 
	 * @return
	 */
	public int gethyDingNiaoNum(int card) {
		int nNum = GameConstants.ZHANIAO_0;
		
		if(has_rule(GameConstants.GAME_RULE_HUNAN_ZHANIAO2)){
			nNum = GameConstants.ZHANIAO_2;
		}
		if(has_rule(GameConstants.GAME_RULE_HUNAN_ZHANIAO4)){
			nNum = GameConstants.ZHANIAO_4;
		}
		if(has_rule(GameConstants.GAME_RULE_HUNAN_ZHANIAO6)){
			nNum = GameConstants.ZHANIAO_6;
		}
		
		if(card == GameConstants.INVALID_VALUE){
			return nNum;
		}
		//窝窝鸟
		if (has_rule_ex(GameConstants.GAME_RULE_HUNAN_WOWONIAO)) { 
			if (_logic.is_magic_card(card)) {
				nNum = GameConstants.MAX_NIAO_CARD; // 奖10鸟
			} else {
				nNum = _logic.get_card_value(card);
			}
		} else { // 正常的万条筒玩法
			nNum = _logic.get_card_value(card);
		}
		
		return nNum;
	}

	
	
	/**
	 * 杠牌检测
	 * @param seat_index
	 * @param provider
	 * @param card
	 * @param d
	 * @param check_chi
	 * @return
	 */
	public boolean estimate_gang_hy_respond(int seat_index, int provider, int card, boolean d, boolean check_chi ) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		playerStatus = _playerStatus[seat_index];
		// playerStatus.clean_action();

		if (playerStatus.lock_huan_zhang() == false) {
			//// 碰牌判断
			action = _logic.check_peng_hy(GRR._cards_index[seat_index], card);
			if (action != 0) {
				playerStatus.add_action(action);
				playerStatus.add_peng(card, seat_index);

				bAroseAction = true;
			}

		}

		// 杠牌判断 如果剩余牌大于1，是否有杠
		if (GRR._left_card_count > get_liu_niao()) {
			if (GRR._cards_index[seat_index][_logic.switch_to_card_index(card)] == 3) {
				playerStatus.add_action(GameConstants.WIK_GANG);
				playerStatus.add_gang(card, provider, 1);// 加上补涨

				if (GRR._left_card_count > 2) {
					boolean is_ting = false;
					boolean can_gang = false;
					if (is_ting == true) {
						can_gang = true;
					} else {
						// 把可以杠的这张牌去掉。看是不是听牌
						int bu_index = _logic.switch_to_card_index(card);
						int save_count = GRR._cards_index[seat_index][bu_index];
						GRR._cards_index[seat_index][bu_index] = 0;

						int cbWeaveIndex = GRR._weave_count[seat_index];

						GRR._weave_items[seat_index][cbWeaveIndex].public_card = 1;
						GRR._weave_items[seat_index][cbWeaveIndex].center_card = card;
						GRR._weave_items[seat_index][cbWeaveIndex].weave_kind = GameConstants.WIK_GANG;// 接杠
						GRR._weave_items[seat_index][cbWeaveIndex].provide_player = provider;
						GRR._weave_count[seat_index]++;

						can_gang = this.is_hy_ting_card(GRR._cards_index[seat_index], GRR._weave_items[seat_index],
								GRR._weave_count[seat_index], seat_index);

						GRR._weave_count[seat_index] = cbWeaveIndex;
						GRR._cards_index[seat_index][bu_index] = save_count;
					}

					if (can_gang == true) {
						playerStatus.add_action(GameConstants.WIK_YAO_YI_SE);
						playerStatus.add_gang(card, provider, 1);// 加上杠

					}
				}
				bAroseAction = true;
			}

		}

		// 吃胡判断
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];// playerStatus._chiHuRight;
		// chr.set_empty();
		int cbWeaveCount = GRR._weave_count[seat_index];
		action = analyse_chi_hu_card(GRR._cards_index[seat_index], GRR._weave_items[seat_index], cbWeaveCount, card,
				chr, GameConstants.HU_CARD_TYPE_PAOHU, seat_index,true);

		// 结果判断
		if (action != 0) {
			if (d) {
				chr.opr_or(GameConstants.CHR_HUNAN_SHUANG_GANG_SHANG_PAO);
			} else {
				chr.opr_or(GameConstants.CHR_HUNAN_GANG_SHANG_PAO);
			}
			if (_playerStatus[seat_index].has_chi_hu() == false) {
				_playerStatus[seat_index].add_action(GameConstants.WIK_CHI_HU);
				_playerStatus[seat_index].add_chi_hu(card, provider);// 吃胡的组合
			}

			bAroseAction = true;
		}

		if (has_rule_ex(GameConstants.GAME_RULE_HUNAN_EAT)) {
			int chi_seat_index = (provider + 1) %getTablePlayerNumber();
			if (_playerStatus[chi_seat_index].lock_huan_zhang() == false) {
				// 长沙麻将吃操作 转转麻将不能吃
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

		}

		return bAroseAction;
	}
		
	
/*	*//**
	 * 
	 * @param seat_index
	 * @param card
	 *            固定的鸟
	 * @param show
	 * @param add_niao
	 *            额外奖鸟
	 *//*
	private void set_niao_card_hy_fei(int seat_index, int card, boolean show, int add_niao) {
		for (int i = 0; i < GameConstants.MAX_NIAO_CARD; i++) {
			GRR._cards_data_niao_fei[i] = GameConstants.INVALID_VALUE;
		}

		for (int i = 0; i <getTablePlayerNumber(); i++) {
			GRR._player_niao_count_fei[i] = 0;
			for (int j = 0; j < GameConstants.MAX_NIAO_CARD; j++) {
				GRR._player_niao_cards_fei[i][j] = GameConstants.INVALID_VALUE;
			}

		}
		GRR._count_niao_fei = get_fei_niao_card_num_cs(true);

		if (GRR._count_niao_fei > GameConstants.ZHANIAO_0) {
			if (card == GameConstants.INVALID_VALUE) {
				int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
				_logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count, GRR._count_niao_fei,
						cbCardIndexTemp);
				GRR._left_card_count -= GRR._count_niao_fei;
				_logic.switch_to_cards_data(cbCardIndexTemp, GRR._cards_data_niao_fei);
			}
		}
		// 中鸟个数
		GRR._count_pick_niao_fei = _logic.get_pick_niao_count(GRR._cards_data_niao_fei, GRR._count_niao_fei);

		for (int i = 0; i < GRR._count_niao_fei; i++) {
			int nValue = _logic.get_card_value(GRR._cards_data_niao_fei[i]);
			int seat = 0;
			if ((GameConstants.GAME_TYPE_ZZ == _game_type_index) || is_mj_type(GameConstants.GAME_TYPE_HZ)
					|| is_mj_type(GameConstants.GAME_TYPE_FLS_HZ_LX) || is_mj_type(GameConstants.GAME_TYPE_SHUANGGUI)
					|| is_mj_type(GameConstants.GAME_TYPE_HENAN_HZ)) {
				seat = (seat_index + (nValue - 1) % 4) % 4;
			} else if (GameConstants.GAME_TYPE_CS == _game_type_index
					|| GameConstants.GAME_TYPE_ZHUZHOU == _game_type_index) {
				seat = (GRR._banker_player + (nValue - 1) % 4) % 4;
			}
			GRR._player_niao_cards_fei[seat][GRR._player_niao_count_fei[seat]] = GRR._cards_data_niao_fei[i];
			GRR._player_niao_count_fei[seat]++;
		}
	}
	
	*//**
	 * 获取长沙能抓的飞鸟的 数量
	 * 
	 * @return
	 *//*
	public int get_fei_niao_card_num_cs(boolean check) {
		int fNum = getFeiNiaoNum();
		if (check == false) {
			return fNum;
		}
		if (fNum > GRR._left_card_count) {
			fNum = GRR._left_card_count;
		}
		return fNum;
	}
	
	*//**
	 * 获取飞鸟 数量
	 * 
	 * @return
	 *//*
	public int getFeiNiaoNum() {
		int num = 0;
		if (has_rule(GameConstants.GAME_RULE_HUNAN_CS_FEI_NIAO2)) {
			return GameConstants.FEINIAO_2;
		} else if (has_rule(GameConstants.GAME_RULE_HUNAN_CS_FEI_NIAO4)) {
			return GameConstants.FEINIAO_4;
		}
		return num;
	}*/
	
	
	// 检查杠牌,有没有胡的
	public boolean estimate_gang_respond_hy(int seat_index, int card, int _action) {
		// 变量定义
		boolean isGang = _action == GameConstants.WIK_GANG ? true : false;// 补张
																			// 不算抢杠胡

		boolean bAroseAction = false;// 出现(是否)有

		int action = GameConstants.WIK_NULL;

		// 动作判断
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			//playerStatus = _playerStatus[i];
			// 可以胡的情况 判断
			// if(playerStatus.is_chi_hu_round()){
			// 吃胡判断
			ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
			chr.set_empty();
			int cbWeaveCount = GRR._weave_count[i];
			action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
					GameConstants.HU_CARD_TYPE_QIANGGANG, i,true);

			// 结果判断
			if (action != 0) {
				if (isGang) {
					chr.opr_or(GameConstants.CHR_HUNAN_QIANG_GANG_HU);// 抢杠胡
				}
				_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
				_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
				bAroseAction = true;
			}
			// }
		}

		if (bAroseAction == true) {
			_provide_player = seat_index;// 谁打的牌
			_provide_card = card;// 打的什么牌
			_resume_player = _current_player;// 保存当前轮到的玩家
			_current_player = GameConstants.INVALID_SEAT;// 有需要操作的玩家。当前玩家为空
		}

		return bAroseAction;
	}
	
	
	
	/**
	 * 玩家出牌动作检测
	 * @param seat_index
	 * @param card
	 * @return
	 */
	public boolean estimate_player_out_card_respond_hy(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有
		
		if(_logic.is_magic_card(card)){
			return bAroseAction;
		}
		
		// 用户状态
		for (int i = 0; i <getTablePlayerNumber(); i++) {

			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		// 动作判断
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			if (_playerStatus[i].lock_huan_zhang() == false) {
				//// 碰牌判断
				action = _logic.check_peng_hy(GRR._cards_index[i], card);
				if (action != 0) {
					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);
					bAroseAction = true;
				}
			}
			
			// 杠牌判断 如果剩余牌大于留鸟，是否有杠,
			if (GRR._left_card_count > get_liu_niao()) {
				action = _logic.estimate_gang_card_out_card_hy(GRR._cards_index[i], card);
				if (action != 0) {
					if (_playerStatus[i].lock_huan_zhang() == false) {
						playerStatus.add_action(GameConstants.WIK_GANG);
						playerStatus.add_gang(card, seat_index, 1);// 加上补张
					}

					// 剩一张为海底
					if (GRR._left_card_count > 2) {

						boolean is_ting = false;
						// 把牌加回来
						// GRR._cards_index[i][bu_index]=save_count;
						boolean can_gang = false;
						if (is_ting == true) {
							can_gang = true;
						} else {
							// 把可以杠的这张牌去掉。看是不是听牌
							int bu_index = _logic.switch_to_card_index(card);
							int save_count = GRR._cards_index[i][bu_index];
							GRR._cards_index[i][bu_index] = 0;

							int cbWeaveIndex = GRR._weave_count[i];

							GRR._weave_items[i][cbWeaveIndex].public_card = 0;
							GRR._weave_items[i][cbWeaveIndex].center_card = card;
							GRR._weave_items[i][cbWeaveIndex].weave_kind = GameConstants.WIK_GANG;// 接杠
							GRR._weave_items[i][cbWeaveIndex].provide_player = seat_index;
							GRR._weave_count[i]++;

							can_gang = this.is_hy_ting_card(GRR._cards_index[i], GRR._weave_items[i],
									GRR._weave_count[i], i);

							// 把牌加回来
							GRR._cards_index[i][bu_index] = save_count;
							GRR._weave_count[i] = cbWeaveIndex;

						}

						if (can_gang == true) {
							playerStatus.add_action(GameConstants.WIK_YAO_YI_SE);
							playerStatus.add_gang(card, seat_index, 1);// 加上杠
							bAroseAction = true;
						}
					}
				}
			}
			
			// 如果是自摸胡
			// 可以胡的情况 判断
			if (_playerStatus[i].is_chi_hu_round()) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						GameConstants.HU_CARD_TYPE_PAOHU, i,true);

				// 结果判断
				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
					bAroseAction = true;
				}
			}
		}

		if(has_rule_ex(GameConstants.GAME_RULE_HUNAN_EAT)){
			int chi_seat_index = (seat_index + 1) %getTablePlayerNumber();
			if (_playerStatus[chi_seat_index].lock_huan_zhang() == false) {
				// 这里可能有问题 应该是 |=
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
	protected void onInitTable() {
		_handler_dispath_card = new MJHandlerDispatchCard_HY();
		_handler_out_card_operate = new MJHandlerOutCardOperate_HY();
		_handler_gang = new MJHandlerGang_HY();
		_handler_chi_peng = new MJHandlerChiPeng_HY();

		// 衡阳handler
		_handler_gang_hy = new MJHandlerGang_HY_DispatchCard();
		_handler_hai_di_hy = new MJHandlerHaiDi_HY();
		_handler_yao_hai_di_hy = new MJHandlerYaoHaiDi_HY();
		_handler_wangba_hy = new MJHandlerWangBa_HY();
		
	}
	
	
	@Override
	protected boolean on_game_start() {
		_game_status = GameConstants.GS_MJ_PLAY;// 设置状态
		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		// gameStartResponse.setSiceIndex(rand);
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);
		_logic.clean_magic_cards();
		if (has_rule_ex(GameConstants.GAME_RULE_HUNAN_HONZLAIZI)) {
			_logic.add_magic_card_index(_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD));
		}
		

		int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants.MAX_COUNT];
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}
		// 发送数据
		for (int i = 0; i <getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			// 只发自己的牌
			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);

				cards.addItem(hand_cards[i][j]);
			}
			// 回放数据
			GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			this.load_room_info_data(roomResponse);
			this.load_common_status(roomResponse);

			if (this._cur_round == 1) {
				// shuffle_players();
				this.load_player_info_data(roomResponse);
			}

			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setLeftCardCount(GRR._left_card_count);

			this.send_response_to_player(i, roomResponse);
		}

		
		/*// 检测听牌
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			_playerStatus[i]._hu_card_count = this.get_ting_card(_playerStatus[i]._hu_cards,
					GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i], false, i);
			if (_playerStatus[i]._hu_card_count > 0) {
				this.operate_chi_hu_cards(i, _playerStatus[i]._hu_card_count, _playerStatus[i]._hu_cards);
			}
		}*/
		
		////////////////////////////////////////////////////////////////////////////////////////////////
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		this.load_room_info_data(roomResponse);
		this.load_common_status(roomResponse);

		if (this._cur_round == 1) {
			// shuffle_players();
			this.load_player_info_data(roomResponse);
		}
		for (int i = 0; i <getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}
		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(GRR._left_card_count);
		GRR.add_room_response(roomResponse);
		
		//选择王霸牌
		if(has_rule_ex(GameConstants.GAME_RULE_HUNAN_WANGBAPAI)){
			this.exe_wangba_hy(GRR._banker_player);
		}else{
			this.exe_dispatch_card(_current_player, GameConstants.WIK_NULL, 0);
		}
		return true;
	}

	

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		//int wFanShu = 1;// 番数
		countCardType(chr, seat_index);
		int lChiHuScore =  get_score(chr,zimo,seat_index) * GameConstants.CELL_SCORE;// wFanShu*m_pGameServiceOption->lCellScore;
		
		// 统计
		if (zimo) {
			// 自摸
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				GRR._lost_fan_shu[i][seat_index] = 1;
			}
		} else {// 点炮
			GRR._lost_fan_shu[provide_index][seat_index] = 1;

		}

		/////////////////////////////////////////////// 算分//////////////////////////
		GRR._count_pick_niao = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GRR._player_niao_count[i]; j++) {
				if(has_rule_ex(GameConstants.GAME_RULE_HUNAN_WOWONIAO)){
					//窝窝鸟只算 1 5 9自己的牌
					if (seat_index == i) {// 自己还有放炮的人有效
						GRR._count_pick_niao++;
						GRR._player_niao_cards[i][j] = this.set_ding_niao_valid(GRR._player_niao_cards[i][j], true);// 胡牌的鸟生效
						GRR._player_niao_invalid[i] ++;
					}
				}else{
					if (zimo) {
						GRR._count_pick_niao++;
						GRR._player_niao_cards[i][j] = super.set_ding_niao_valid(GRR._player_niao_cards[i][j], true);// 胡牌的鸟生效
						GRR._player_niao_invalid[i] ++;
					} else {
						if (seat_index == i || provide_index == i) {// 自己还有放炮的人有效
							GRR._count_pick_niao++;
							GRR._player_niao_cards[i][j] = this.set_ding_niao_valid(GRR._player_niao_cards[i][j], true);// 胡牌的鸟生效
							GRR._player_niao_invalid[i] ++;
						} else {
							GRR._player_niao_cards[i][j] = this.set_ding_niao_valid(GRR._player_niao_cards[i][j], false);// 胡牌的鸟生效
							//GRR._player_niao_invalid[i] ++;
						}
					}
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
				if (this.is_zhuang_xian()) {
					if ((GRR._banker_player == i) || (GRR._banker_player == seat_index)) {
						//int zx = lChiHuScore / 6;
						s += 1;
					}
				}
				//GRR._player_niao_invalid[i] = 1;// 庄家鸟生效

				int niao = 0;
				
				if(has_rule_ex(GameConstants.GAME_RULE_HUNAN_WOWONIAO)){
					niao = GRR._player_niao_count[seat_index];
				}else{
					niao = GRR._player_niao_count[seat_index] + GRR._player_niao_count[i];
				}
				if (niao > 0) {
					s += niao;
				}

				// 胡牌分
				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;
			}
		}
		////////////////////////////////////////////////////// 点炮 算分
		else {
			int s = lChiHuScore;
			if (this.is_zhuang_xian()) {
				if ((GRR._banker_player == provide_index) || (GRR._banker_player == seat_index)) {
					int zx = GRR._chi_hu_rights[seat_index].da_hu_count;// lChiHuScore/6;
					s += 1;
				}
			}
		//	int niao = GRR._player_niao_count[seat_index] + GRR._player_niao_count[provide_index];
			
			int niao = 0;
			
			if(has_rule_ex(GameConstants.GAME_RULE_HUNAN_WOWONIAO)){
				niao = GRR._player_niao_count[seat_index];
			}else{
				niao = GRR._player_niao_count[seat_index] + GRR._player_niao_count[provide_index];
			}
			if (niao > 0) {
				s += niao;
			}

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

		_playerStatus[seat_index].clean_status();

		return;
		
	}
	
	/**
	 * 获取分数
	 * @param chr
	 * @param zimo
	 * @return
	 */
	private int get_score( ChiHuRight chr,boolean zimo,int seat_index) {
		int wFanShu = 0;
		if(chr.hengyang_da_hu > 0){
			if(zimo){
				for(int i = 0; i< chr.hengyang_da_hu; i++ ){
					wFanShu +=3;
				}
			}else{
				int[] chi_hu_card=  this.GRR._chi_hu_card[seat_index];
				int count = 0;
				for (int i = 0; i < chi_hu_card.length; i++) {
					if(chi_hu_card[i] != 0){
						count++;
					}
				}
				if(count>1){
					wFanShu = 12;
				}else{
					wFanShu = 6;
				}
			}
		}else {
			if(chr.is_qing_ping_hu){
				wFanShu =2;
			}else {
				wFanShu =1;
			}
		}
		return wFanShu;
	}

	@Override
	protected void set_result_describe() {
		int l;
		long type = 0;
		// 有可能是通炮
		boolean has_da_hu = false;
		// 大胡
		boolean dahu[] = { false, false, false, false };
		for (int i = 0; i <getTablePlayerNumber(); i++) {
			if (GRR._chi_hu_rights[i].is_valid() && GRR._chi_hu_rights[i].hengyang_da_hu > 0) {
				dahu[i] = true;
				has_da_hu = true;
			}
		}
		for (int i = 0; i <getTablePlayerNumber(); i++) {
			ChiHuRight chr = GRR._chi_hu_rights[i];
			String des = "";

			l = chr.type_count;
			for (int j = 0; j < l; j++) {
				type = chr.type_list[j];
				if (chr.is_valid()) {
					if (type == GameConstants.CHR_HUNAN_PENGPENG_HU) {
						des += " 碰碰胡";
					}
					if (type == GameConstants.CHR_HUNAN_JIANGJIANG_HU) {
						des += " 将将胡";
					}
					if (type == GameConstants.CHR_HUNAN_QING_YI_SE) {
						des += " 清一色";
					}
					if (type == GameConstants.CHR_HUNAN_HAI_DI_LAO) {
						des += " 海底捞";
					}
					if (type == GameConstants.CHR_HUNAN_HAI_DI_PAO) {
						des += " 海底炮";
					}
					if (type == GameConstants.CHR_HUNAN_QI_XIAO_DUI) {
						des += " 七小对";
					}
					if (type == GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI) {
						des += " 豪华七小对";
					}
					if (type == GameConstants.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI) {
						des += " 双豪华七小对";
					}
					if (type == GameConstants.CHR_HUNAN_GANG_KAI) {
						des += " 杠上开花";
					}
					if (type == GameConstants.CHR_HUNAN_SHUANG_GANG_KAI) {
						des += " 双杠上开花";
					}
					if (type == GameConstants.CHR_HUNAN_QIANG_GANG_HU) {
						des += " 抢杠胡";
					}
					if (type == GameConstants.CHR_HUNAN_GANG_SHANG_PAO) {
						des += " 杠上炮";
					}
					if (type == GameConstants.CHR_HUNAN_QUAN_QIU_REN) {
						des += " 全求人";
					}
					if (type == GameConstants.CHR_ZI_MO) {
						if (dahu[i] == true) {
							des += " 大胡自摸";
						} else {
							des += " 小胡自摸";
						}
					}
					if (type == GameConstants.CHR_SHU_FAN) {
						if (dahu[i] == true) {
							des += " 大胡接炮";
						} else {
							des += " 小胡接炮";
						}
					}
					if (type == GameConstants.CHR_FANG_PAO) {
						des += " 放炮";
					}
					if (type == GameConstants.CHR_TONG_PAO) {
						des += " 通炮";
					}
				} else {
					if (type == GameConstants.CHR_FANG_PAO) {
						if (has_da_hu == true) {
							des += " 大胡放炮";
						} else {
							des += " 小胡放炮";
						}
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
			
			if (GRR._player_niao_invalid[i] > 0) {
				des += " 中鸟X" + GRR._player_niao_count[i] + "";
			}
			
			if (GRR._player_niao_count_fei[i] > 0) {
				des += " 飞鸟X" + GRR._player_niao_count_fei[i];
			}
			GRR._result_des[i] = des;
		}
	}
	
	@Override
	public int getTablePlayerNumber() {
		if (has_rule(GameConstants.GAME_RULE_HUNAN_THREE)) {
				return GameConstants.GAME_PLAYER - 1;
		}
		return GameConstants.GAME_PLAYER;
	}

	
	protected boolean on_handler_game_finish(int seat_index, int reason) {
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

		RoomInfo.Builder room_info = getRoomInfo();
		game_end.setRoomInfo(room_info);

		game_end.setRoundOverType(0);
		game_end.setRoomOverType(0);
		game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间
		game_end.setGamePlayerNumber(getTablePlayerNumber());
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			game_end.addPao(_player_result.pao[i]);
		}

		if (GRR != null) {
			// reason == MJGameConstants.Game_End_NORMAL || reason
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
				GRR._game_score[i] += lGangScore[i];
				GRR._game_score[i] += GRR._start_hu_score[i];// 起手胡分数

				_player_result.game_score[i] += GRR._game_score[i];

			}

			this.load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);

			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);// 专家
			game_end.setLeftCardCount(GRR._left_card_count);// 剩余牌
			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);
			//game_end.setShowBirdEffect(0);

			// 设置中鸟数据
			for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i < GRR._count_niao; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao[i]);
			}

			for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i < GRR._count_niao_fei; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao_fei[i]);
			}
			game_end.setCountPickNiao(GRR._count_pick_niao + GRR._count_pick_niao_fei);// 中鸟个数

			// 鸟的数据 这里要注意三人场的特殊处理 三人场必须发四个人的鸟 不然显示不全
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				//定鸟
				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					int card = GRR._player_niao_cards[i][j];
					if(card < GameConstants.DING_NIAO_INVALID){
						card += GameConstants.DING_NIAO_INVALID;
					}
					pnc.addItem(card);
				}
				
				game_end.addPlayerNiaoCards(pnc);
			}
			
			for (int i = 0; i < getTablePlayerNumber(); i++) {

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
					//结算牌加入角标
					if (_logic.is_wang_ba_card(GRR._cards_data[i][j]) 
							) {
						GRR._cards_data[i][j] += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
					}
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
					if (j < GRR._weave_count[i]) {
						weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
					} else {
						weaveItem_item.setWeaveKind(GameConstants.WIK_NULL);
					}

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
			if (_cur_round >= _game_round) {// 局数到了
				end = true;
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(this.process_player_result(reason));
			} else {
				// 确定下局庄家
				// 以后谁胡牌，下局谁做庄。
				// 流局,庄家下一个
				// 通炮,放炮的玩家

			}
		} else if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT
				|| reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_SYSTEM) {
			end = true;
			real_reason = GameConstants.Game_End_DRAW;
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(this.process_player_result(reason));
		}
		game_end.setEndType(real_reason);

		////////////////////////////////////////////////////////////////////// 得分总的
		roomResponse.setGameEnd(game_end);

		this.send_response_to_room(roomResponse);

		record_game_round(game_end);

		// 超时解散
		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < getTablePlayerNumber(); j++) {
				Player player = this.get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");

			}
		}

		if (end)// 删除
		{
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
		}
		if (!is_sys()) {
			GRR = null;
		}

		// 错误断言
		return false;
	}
	
	/**
	 * 显示骰子特效
	 * @param num1
	 * @param num2
	 * @param anim_time
	 * @param delay_time
	 * @return
	 */
	public boolean operate_shai_zi_effect(int num1, int num2, int anim_time, int delay_time) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_EFFECT_ACTION);
		roomResponse.setEffectType(GameConstants.Effect_Action_SHAI_ZI);
		roomResponse.setTarget(get_target_shai_zi_player(num1, num2));
		roomResponse.setEffectCount(2);
		roomResponse.addEffectsIndex(num1);
		roomResponse.addEffectsIndex(num2);
		roomResponse.setEffectTime(anim_time);// anim time//摇骰子动画的时间
		roomResponse.setStandTime(delay_time); // delay time//停留时间
		this.send_response_to_room(roomResponse);
		GRR.add_room_response(roomResponse);
		return true;
	}
	
	
	/**
	 * 计算中骰子的玩家
	 * @param num1
	 * @param num2
	 * @return
	 */
	public int get_target_shai_zi_player(int num1, int num2) {
		return get_zhong_seat_by_value_hy(num1 + num2, GRR._banker_player);// GameConstants.INVALID_SEAT;
	}
	
	
	/**
	 * 初始化洗牌
	 */
	@Override
	public void init_shuffle(){
		int[] cards = null;
		if(has_rule_ex(GameConstants.GAME_RULE_HUNAN_HONZLAIZI)){
			cards = MJConstants.CARD_DATA_HNCZ;
		}else if(has_rule_ex(GameConstants.GAME_RULE_HUNAN_WANGBAPAI)){
			cards = MJConstants.CARD_DATA_WAN_TIAO_TONG;
		}else{
			cards = MJConstants.CARD_DATA_WAN_TIAO_TONG;
		}
		_repertory_card = new int[cards.length];
		shuffle(_repertory_card,cards);
	}
	
	
	/**
	 * 显示中间出的牌
	 * @param seat_index
	 */
	public void runnable_remove_middle_cards_wangba(int seat_index) {
		// 牌局未开 或者等待状态 调度不需要执行 add by zain 2017/6/1
		if ((_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) && is_sys())
			return;
		// 去掉
		this.operate_show_card(seat_index, GameConstants.Show_Card_Center, 0, null, GameConstants.INVALID_SEAT);

		// 刷新有王霸的牌
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
					if (_logic.is_wang_ba_card( cards[j]) ) {
						cards[j] += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
					}
				}
				this.operate_player_cards(i, hand_card_count, cards, 0, null);
			}
		}

		this.exe_dispatch_card(seat_index, GameConstants.WIK_NULL, 0);
	}
	
	
	/**
	 * 获取当局留鸟个数
	 * @return
	 */
	public int get_liu_niao(){
		int left_count  = 1;
		if(this.has_rule_ex(GameConstants.GAME_RULE_HUNAN_WOWONIAO)){
			left_count ++;
		}else{
			left_count += this.gethyDingNiaoNum(GameConstants.INVALID_VALUE);
		}
		return left_count;
	}
	
	/**
	 * 庄闲判断
	 */
	@Override
	public boolean is_zhuang_xian(){
		boolean flag =false;
		if(has_rule_ex(GameConstants.GAME_RULE_HUNAN_ZHUANG_XIAN)){
			flag =true;
		}
		return flag;
	}
	
	@Override
	public boolean handler_requst_pao_qiang(Player player, int pao, int qiang) {
		return false;
	}

	@Override
	public boolean handler_requst_nao_zhuang(Player player, int nao) {
		return false;
	}

	@Override
	public  boolean handler_requst_message_deal(Player player,int seat_index, RoomRequest room_rq,int type){
		return true;
	}


	/* (non-Javadoc)
	 * @see com.cai.game.mj.AbstractMJTable#analyse_chi_hu_card(int[], com.cai.common.domain.WeaveItem[], int, int, com.cai.common.domain.ChiHuRight, int, int)
	 */
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weaveCount, int cur_card,
			ChiHuRight chiHuRight, int card_type, int _seat_index,boolean need_clean_chr) {
		if ((has_rule(GameConstants.GAME_RULE_HENAN_ZIMOHU) && (card_type == GameConstants.HU_CARD_TYPE_PAOHU))) {
			return GameConstants.WIK_NULL;
		}

		if (this._playerStatus[_seat_index].isAbandoned()) { // 已经弃胡
			return GameConstants.WIK_NULL;
		}
		//漏胡判断
		int[] luo_hu_card = this._playerStatus[_seat_index].get_cards_abandoned_hu();
		for(int i = 0; i < luo_hu_card.length ; i++){
			if(luo_hu_card[i] == cur_card){
				return GameConstants.WIK_NULL;
			}
		}
		
		
		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		/*// 变量定义
		int cbChiHuKind = GameConstants.WIK_NULL;*/

		// 构造扑克
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入扑克
		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		boolean hu = false;
		boolean qing_yi_se = false;
		boolean peng_peng_hu = false;
		boolean da_hu = false;
		//七小对 将将胡 可能不是能胡牌的牌型 优先判断
		long qxd = _logic.is_qi_xiao_dui_hy(cards_index, weaveItems, weaveCount, cur_card);
		if(card_type == GameConstants.HU_CARD_TYPE_PAOHU){ // 七小对2 5 8 做将可炮
			boolean has_258 = has_258(cbCardIndexTemp,_seat_index);
			if (qxd != GameConstants.WIK_NULL && has_258 ) {
				chiHuRight.opr_or(qxd);
				da_hu = true;
			}
		}else{
			if (qxd != GameConstants.WIK_NULL ) {
				chiHuRight.opr_or(qxd);
				da_hu = true;
			}
		}
		
		// 将将胡
		if (_logic.is_jiangjiang_hu(cards_index, weaveItems, weaveCount, cur_card) 
				&& ( card_type == GameConstants.HU_CARD_TYPE_ZIMO)) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_JIANGJIANG_HU);
			da_hu = true;
		}

		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
		boolean bValue = _logic.analyse_card_henan_hy(cbCardIndexTemp, weaveItems, weaveCount, analyseItemArray, true); // 这里的分析胡牌只能分析一部分的，所以要尽量往后靠
		
		// 胡牌分析 有没有258
		for (int i = 0; i < analyseItemArray.size(); i++) {
			// 变量定义
			AnalyseItem pAnalyseItem = analyseItemArray.get(i);
			if (pAnalyseItem.bMagicEye) { // 如果红中癞子是牌眼
				if (_logic.is_magic_card(pAnalyseItem.cbCardEye)) { // 如果将牌是红中癞子
					hu = true;
					break;
				}
			}
			int color = _logic.get_card_color(pAnalyseItem.cbCardEye);
			if (color > 2)
				continue;
			int cbCardValue = _logic.get_card_value(pAnalyseItem.cbCardEye);
			if (cbCardValue == 2 || cbCardValue == 5 || cbCardValue == 8) {
				hu = true;
				break;
			}
		}
		
		// 清一色牌
		if (_logic.is_qing_yi_se(cards_index, weaveItems, weaveCount, cur_card) && bValue) {
			qing_yi_se = true;
		}
		
		// 牌型分析
		for (int i = 0; i < analyseItemArray.size(); i++) {
			// 变量定义
			AnalyseItem analyseItem = analyseItemArray.get(i);
			// 碰碰和
			if (_logic.is_pengpeng_hu(analyseItem) && bValue) {
				if(card_type == GameConstants.HU_CARD_TYPE_PAOHU){
					if (analyseItem.bMagicEye) { // 如果红中癞子是牌眼
						if (_logic.is_magic_card(analyseItem.cbCardEye)) { // 如果将牌是红中癞子
							peng_peng_hu = true;
							break;
						}
					}
					int color = _logic.get_card_color(analyseItem.cbCardEye);
					if (color > 2)
						continue;
					int cbCardValue = _logic.get_card_value(analyseItem.cbCardEye);
					if (cbCardValue == 2 || cbCardValue == 5 || cbCardValue == 8) {
						peng_peng_hu = true;
						break;
					}
				}else{
					peng_peng_hu = true;
					break;
				}
			}
		}
		
		// 全求人
		if (_logic.is_dan_diao＿hy(cards_index, cur_card) 
				&& ( card_type == GameConstants.HU_CARD_TYPE_ZIMO) ) {// weaveCount == 4&&
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_QUAN_QIU_REN);
			da_hu = true;
		}
		
		//是否可胡清一色 碰碰胡
		boolean da_hu_ke_pao = true;
		if(card_type == GameConstants.HU_CARD_TYPE_PAOHU && !hu ){
			da_hu_ke_pao = false;
		}/*else if( !hu ){
			da_hu_ke_pao = false;
		}*/
		
		if(da_hu_ke_pao){
			if(qing_yi_se){
				chiHuRight.opr_or(GameConstants.CHR_HUNAN_QING_YI_SE);
				da_hu = true;
			}
		}
		
		if(peng_peng_hu){
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_PENGPENG_HU);
			da_hu = true;
		}
		//存在大胡
		if(da_hu){
			if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
				chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
			}  else {
				chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
			}
			return GameConstants.WIK_CHI_HU;
		}
		
		//青平胡判断
		boolean has_wang_ba = false;
		boolean can_pin_hu = false; //可以平胡
		for(int i = 0 ; i< cbCardIndexTemp.length; i++){
			if(cbCardIndexTemp[i]==0)continue;
			if(_logic.is_magic_index(i)){
				has_wang_ba = true;
			}
		}
		
		if( card_type != GameConstants.HU_CARD_TYPE_ZIMO  && !has_wang_ba && hu){
			can_pin_hu = true;
		}else if(card_type == GameConstants.HU_CARD_TYPE_ZIMO && hu){
			can_pin_hu = true;
		}
		
		if(can_pin_hu){
			//标识清平湖
			chiHuRight.setIs_qing_ping_hu(!has_wang_ba);
			if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
				chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
			}  else {
				chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
			}
			return GameConstants.WIK_CHI_HU;
		}
		if(need_clean_chr){
			chiHuRight.set_empty();
		}
		return GameConstants.WIK_NULL;
	}

	
	public boolean has_258(int[] cards_index,int _seat_index){
		int dan[] = new int[14]; 
		int count = 0;
		int magic_count = 0;
		
		for (int i = 0; i < cards_index.length; i++) {
			int cbCardCount = cards_index[i];
			int card  = _logic.switch_to_card_data(i);
			int cbCardValue = _logic.get_card_value(card);
			if ((cbCardValue == 2 || cbCardValue == 5 || cbCardValue == 8) && cbCardCount == 2) {
				return true;
			}
			if(cbCardCount == 0)
				continue;
			
			//剔除对子
			if(!_logic.is_magic_card(card) && cbCardCount == 2)
				continue;
			
			if(_logic.is_magic_card(card)){
				magic_count = cbCardCount;
			}else{
				dan[count]=card;
				count++;
			}
		}
		//分析剩余牌型
		if(magic_count > 0 && magic_count % 2 > 0 ){
			for(int k = 0; k<dan.length; k++){
				int cbCardValue = _logic.get_card_value(dan[k]);
				if (cbCardValue == 2 || cbCardValue == 5 || cbCardValue == 8) {
					return true;
				}
			}
		}else if(magic_count > 0 && magic_count % 2 == 0){
			return true;
		}
		
		return false;
	}

	
	public int get_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount,
			boolean dai_feng, int seat_index) {

		
		// 复制数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int max_ting_count = GameConstants.MAX_INDEX;

		// 如果有红中癞子的玩法，是不需要判断红中的
		for (int i = 0; i < max_ting_count; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard,
					chr, GameConstants.HU_CARD_TYPE_ZIMO, seat_index,true)) {
				if(has_rule_ex(GameConstants.GAME_RULE_HUNAN_WANGBAPAI)){
					if(_logic.is_magic_card(cbCurrentCard)){
						cbCurrentCard += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
					}
				}
				cards[count] = cbCurrentCard;
				count++;
			}
		}

		if (count == 0) {
			// 没听牌
		} else if (count > 0 && count < max_ting_count) {
			if (GameDescUtil.has_rule(_game_rule_index, GameConstants.GAME_RULE_HUNAN_HONGZHONG)) {
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
	


	/* (non-Javadoc)
	 * @see com.cai.common.domain.Room#trustee_timer(int, int)
	 */
	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		return false;
	}
	
	
	
	@Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] {0x01,0x01,0x02,0x02,0x03,0x03,0x04,0x04,0x05,0x05,0x05,0x06,0x06};
		int[] cards_of_player1 = new int[] { 0x01,0x01,0x01,0x01,0x03,0x03,0x03,0x03,0x06,0x06,0x12,0x12,0x13 };
		int[] cards_of_player2 = new int[] { 0x02, 0x02, 0x02, 0x05, 0x05, 0x05, 0x08, 0x08, 0x08, 0x12, 0x12, 0x12, 0x15 };
		int[] cards_of_player3 = new int[] { 0x02, 0x02, 0x02, 0x05, 0x05, 0x05, 0x08, 0x08, 0x08, 0x12, 0x12, 0x12, 0x15 };
		/*int[] cards_of_player1 = new int[] { 0x26, 0x26, 0x27, 0x27, 0x02, 0x02, 0x07, 0x07, 0x08, 0x11, 0x11, 0x13,
				0x13 };
		int[] cards_of_player2 = new int[] { 0x03, 0x04, 0x05, 0x15, 0x16, 0x17, 0x17, 0x18, 0x19, 0x01, 0x01, 0x25,
				0x27 };
		int[] cards_of_player3 = new int[] { 0x01, 0x02, 0x03, 0x05, 0x06, 0x07, 0x09, 0x09, 0x23, 0x24, 0x25, 0x27,
				0x28 };*/

//		int[] cards_of_player0 = new int[] { 0x01,0x01,0x01,0x02,0x02,0x02,0x03,0x03,0x03,0x05,0x05,0x05,0x07 };
//		int[] cards_of_player1 = new int[] { 0x01,0x01,0x01,0x02,0x02,0x02,0x03,0x03,0x03,0x05,0x05,0x05,0x07 };
//		int[] cards_of_player2 = new int[] { 0x01,0x01,0x01,0x02,0x02,0x02,0x03,0x03,0x03,0x05,0x05,0x05,0x07};
//		int[] cards_of_player3 = new int[] { 0x01,0x01,0x01,0x02,0x02,0x02,0x03,0x03,0x03,0x05,0x05,0x05,0x07 };
//		
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}

		for (int j = 0; j < 13; j++) {
			if (this.getTablePlayerNumber() == 4) {
				GRR._cards_index[0][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[1][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[2][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[3][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
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


	/* (non-Javadoc)
	 * @see com.cai.game.mj.AbstractMJTable#analyse_chi_hu_card(int[], com.cai.common.domain.WeaveItem[], int, int, com.cai.common.domain.ChiHuRight, int, int)
	 */
	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card,
			ChiHuRight chiHuRight, int card_type, int _seat_index) {
		return 0;
	}
	

}
