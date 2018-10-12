package com.cai.game.mj.handler.yyzxz;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.CardsData;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.mj.handler.MJHandlerDispatchCard;

/**
 * 摸牌
 * 
 * @author Administrator
 *
 */
public class MJHandlerDispatchCard_YYZXZ extends MJHandlerDispatchCard<MJTable_YYZXZ> {

	private final XiaZiCardResult xiaZiResult;
	
	private int cardCount = 1;
	
	private CardsData card_data;
	
	public MJHandlerDispatchCard_YYZXZ() {
		super();
		xiaZiResult = new XiaZiCardResult();
	}

	@Override
	public void exe(MJTable_YYZXZ table) {
		// 用户状态
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			table._playerStatus[i].clean_action();
			// table._playerStatus[i].clean_status();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
		}

		table._playerStatus[_seat_index].chi_hu_round_valid();// 可以胡了

		// 荒庄结束
		if (table.GRR._left_card_count == 0) {
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				table.GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
			}

			// 流局
			table.handler_game_finish(table._banker_select, GameConstants.Game_End_DRAW);

			return;
		}

		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		table._current_player = _seat_index;// 轮到操作的人是自己

		int[] cards = new int[cardCount];
		xiaZiResult.cbCardCount = 0;
		// 从牌堆拿出指定牌数
		for(int i=0; i < cardCount; i++){
			table._send_card_count++;
			int card = table._repertory_card[table._all_card_len-table.GRR._left_card_count];
			cards[i] = card;
			// 最后一张牌 
			if(i == cardCount -1){
				_send_card_data = card;
			}else {
				// 加到手牌
				table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(card)]++;
			}
		
		}
		int xiaziAction = GameConstants.WIK_NULL;
		//摸到的牌有没虾牌
		for (int i = 0; i < this.card_data.get_card_count(); i++) {
			int color = table._logic.get_card_color(this.card_data.get_cards()[i]);
			if(color == 0){
				xiaziAction = GameConstants.WIK_XIA_ZI_BU;
				int index = xiaZiResult.cbCardCount++;
				xiaZiResult.cbCardData[index] = this.card_data.get_cards()[i];
				xiaZiResult.isPublic[index] = 0;//
			}
		}
		
		table._provide_player = _seat_index;

		
		// //检查是否有虾子
		xiaziAction = table._logic.analyse_xia_card_all(table.GRR._cards_index[_seat_index], xiaZiResult,xiaziAction);
		
		// 虾牌的数比 牌堆得牌还要多 不能补牌了
		if(xiaZiResult.cbCardCount > table.GRR._left_card_count){
			xiaziAction= GameConstants.WIK_NULL;
		}
		if (xiaziAction != GameConstants.WIK_NULL) {

			for (int i = 0; i < xiaZiResult.cbCardCount; i++) {
				curPlayerStatus.add_action(GameConstants.WIK_XIA_ZI_BU);
				// 加上虾子
				curPlayerStatus.add_XiaZi(xiaZiResult.cbCardData[i], _seat_index, xiaZiResult.isPublic[i]);
			}
		} else {
			// 有虾牌就不能胡
			// 发牌处理,判断发给的这个人有没有胡牌或杠牌
			// 胡牌判断
			ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
			chr.set_empty();

			int action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
					table.GRR._weave_count[_seat_index], _send_card_data, chr, GameConstants.HU_CARD_TYPE_ZIMO, _seat_index);// 自摸

			if (action != GameConstants.WIK_NULL) {
				// 添加动作
				curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
				curPlayerStatus.add_zi_mo(_send_card_data, _seat_index);

			} else {
				chr.set_empty();
			}
		}

		// 加到手牌
		table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;

		// 发送数据
		// 只有自己才有数值
		table.operate_player_get_card(_seat_index, cards.length, cards, GameConstants.INVALID_SEAT);

		// 设置变量
		table._provide_card = _send_card_data;// 提供的牌

		m_gangCardResult.cbCardCount = 0;

		if (table.GRR._left_card_count >= 1) {
			// 看手上所有的牌,有没有杠
			int cbActionMask = table._logic.analyse_gang_card_all(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
					table.GRR._weave_count[_seat_index], m_gangCardResult, true, 0);

			if (cbActionMask != GameConstants.WIK_NULL) {// 有杠
				curPlayerStatus.add_action(GameConstants.WIK_GANG);
				for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
					// 加上刚
					curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
				}
			}
		}

		if (curPlayerStatus.has_action()) {// 有动作
			// 操作状态
			table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
			table.operate_player_action(_seat_index, false);
		} else {
			
			// 出牌状态
			table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
			// 不能换章,自动出牌
			if (table._playerStatus[_seat_index].lock_huan_zhang()) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data), GameConstants.DELAY_AUTO_OUT_CARD,
						TimeUnit.MILLISECONDS);
			} else {
				table.operate_player_status();
			}
		}
		
		return;
	}

	/***
	 * //用户操作
	 * 
	 * @param seat_index
	 * @param operate_code
	 * @param operate_card
	 * @return
	 */
	@Override
	public boolean handler_operate_card(MJTable_YYZXZ table, int seat_index, int operate_code, int operate_card) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		// 效验操作
		if ((operate_code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
			table.log_error("没有这个操作");
			return false;
		}

		if (seat_index != _seat_index) {
			table.log_error("不是当前玩家操作");
			return false;
		}
		// 是否已经响应
		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "出牌,玩家已操作");
			return true;
		}
		// 记录玩家的操作
		playerStatus.operate(operate_code, operate_card);
		playerStatus.clean_status();

		// 放弃操作
		if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_NULL }, 1);
			// 用户状态
			table._playerStatus[_seat_index].clean_action();
			table._playerStatus[_seat_index].clean_status();

			if (table._playerStatus[_seat_index].lock_huan_zhang()) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data), GameConstants.DELAY_AUTO_OUT_CARD,
						TimeUnit.MILLISECONDS);
			} else {
				// table._playerStatus[_seat_index].set_status(GameConstants.Player_Status_OUT_CARD);
				table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
				table.operate_player_status();
			}

			return true;
		}

		// 执行动作
		switch (operate_code) {
		case GameConstants.WIK_GANG: // 杠牌操作
		{
			for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
				if (operate_card == m_gangCardResult.cbCardData[i]) {
					// 是否有抢杠胡
					table.exe_gang(_seat_index, _seat_index, operate_card, operate_code, m_gangCardResult.type[i], true, false);
					return true;
				}
			}
			break;
		}
		case GameConstants.WIK_XIA_ZI_BU: // 虾子补牌
		{
			int[] xiaZiCards  = new int[xiaZiResult.cbCardCount];
			for (int i = 0; i < xiaZiResult.cbCardCount; i++) {
				xiaZiCards[i] =xiaZiResult.cbCardData[i];
				table._logic.remove_card_by_index(table.GRR._cards_index[_seat_index], xiaZiCards[i]);
				table.xia_zi_fen[_seat_index]++;
			}
			//刷新手牌
			int cards[] = new int[GameConstants.MAX_COUNT];

			//刷新自己手牌
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
			table.operate_player_cards(_seat_index, hand_card_count, xiaZiCards, 0, null);

			//显示出牌
			table.operate_out_card(_seat_index,xiaZiCards.length,xiaZiCards,GameConstants.OUT_CARD_TYPE_XIA_ZI,GameConstants.INVALID_SEAT);
			
			// 添加到自己的已出牌列表
			table.exe_add_discard(this.xiaZiResult.cbCardCount, xiaZiCards.length, xiaZiCards, false,0);
			
			//  虾子补张
			table.exe_dispatch_card(seat_index, GameConstants.WIK_XIA_ZI_BU, 0, xiaZiResult.cbCardCount,false);
			break;
		}

		case GameConstants.WIK_ZI_MO: // 自摸
		{
			table.GRR._chi_hu_rights[_seat_index].set_valid(true);

			table.GRR._chi_hu_card[_seat_index][0] = operate_card;

			table._banker_select = _seat_index;
			
			if(isGang){
				table.GRR._chi_hu_rights[seat_index].opr_or(GameConstants.CHR_HUNAN_GANG_KAI);
			}
			
			table.process_chi_hu_player_operate(_seat_index, new int[] { operate_card }, 1, true);
			table.process_chi_hu_player_score(_seat_index, _seat_index, operate_card, true);

//			// 记录
//			if (table.GRR._chi_hu_rights[_seat_index].da_hu_count > 0) {
//				table._player_result.da_hu_zi_mo[_seat_index]++;
//			} else {
//				table._player_result.xiao_hu_zi_mo[_seat_index]++;
//			}

			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_NORMAL), GameConstants.GAME_FINISH_DELAY,
					TimeUnit.SECONDS);

			return true;
		}
		}

		return true;
	}

	public class XiaZiCardResult {
		public int cbCardCount;// 扑克数目
		public int cbCardData[];// 扑克数据
		public int isPublic[];//
		public int type[];// ;类型

		public XiaZiCardResult() {
			cbCardData = new int[8];
			isPublic = new int[8];
			type = new int[8];
		}
	}
}
