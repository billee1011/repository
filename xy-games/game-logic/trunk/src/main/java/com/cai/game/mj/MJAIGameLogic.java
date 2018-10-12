/**
 * 
 */
package com.cai.game.mj;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.util.RandomUtil;
import com.cai.game.mj.handler.MJHandlerChiPeng;

//类型子项
class KindItem {
	public int cbWeaveKind;// 组合类型
	public int cbCenterCard;// 中心扑克
	public int cbCardIndex[] = new int[3];// 扑克索引
	public int cbValidIndex[] = new int[3];// 实际扑克索引
	public boolean has_macgic = false;

	public KindItem() {

	}
}

public class MJAIGameLogic {

	private static Logger logger = LoggerFactory.getLogger(MJAIGameLogic.class);

	private static int SHUN_ZI = 1; // 顺子

	private static int DUI_ZI = 2; // 对子

	private static int KAN_ZI = 3; // 坎子

	public static void AI_Out_Card(AbstractMJTable table, int seat_index) {

		if (table._playerStatus[seat_index].get_status() != GameConstants.Player_Status_OUT_CARD)
			return;

		int card = get_card(table, seat_index);
		// logger.debug("出牌玩家Index：{}出的牌是：{}" ,seat_index, card);

		table.handler_player_out_card(seat_index, card);
	}

	public static void AI_Out_Card_ALL(AbstractMJTable table, int seat_index) {

		if (table._playerStatus[seat_index].get_status() != GameConstants.Player_Status_OUT_CARD)
			return;
		// long time = System.currentTimeMillis();
		int card = getOutCardTwo(table, seat_index);
		// logger.debug("出牌玩家Index：{}出的牌是：{}" ,seat_index, card);
		// System.err.println( System.currentTimeMillis()- time);
		table.handler_player_out_card(seat_index, card);
	}

	/**
	 * 获取出牌数据,不拆对子.顺子.坎子
	 * 
	 * @param table
	 * @param seat_index
	 * @return
	 */
	public static int get_card(AbstractMJTable table, int seat_index) {
		int rust_card = table._send_card_data;
		if(table._player_result.dingQueInfo.isDingQue()){
			boolean flag  =false;
			for(int i = 0; i <table.GRR._cards_index[seat_index].length; i++){
				if(table.GRR._cards_index[seat_index][i] == 0){
					continue;
				}
				int card = table._logic.switch_to_card_data(i);
				int color = table._logic.get_card_color(card);
				if(color == table._player_result.dingQueInfo.getQueColors(seat_index)){
					rust_card = card;
					flag = true;
					break;
				}
			}
			if(!flag){
				if (table._logic.is_magic_card(table._send_card_data) || table._handler instanceof MJHandlerChiPeng) {
					int cards[] = new int[GameConstants.MAX_COUNT];
					int handCount = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);
					rust_card = cards[handCount - 1];
				}
			}
			
		}else{
			if (table._logic.is_magic_card(table._send_card_data) || table._handler instanceof MJHandlerChiPeng) {
				int cards[] = new int[GameConstants.MAX_COUNT];
				int handCount = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);
				rust_card = cards[handCount - 1];
			}
		}

		return rust_card;
	}

	public static int getCard(AbstractMJTable table, int seat_index) {
		int rust_card = 0;
		boolean has_shun = false;
		boolean has_dui = false;
		boolean has_kan = false;

		// 构造数据
		int[] card_index = table.GRR._cards_index[seat_index];
		int[] cbMagicCardIndex = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < card_index.length; i++) {
			cbMagicCardIndex[i] = card_index[i];
		}
		int cbMagicCardCount = table._logic.magic_count(cbMagicCardIndex);
		for (int i = 0; i < table._logic.get_magic_card_count(); i++) {
			if (cbMagicCardIndex[table._logic.get_magic_card_index(i)] > 0) {
				cbMagicCardIndex[table._logic.get_magic_card_index(i)] = 0; // 减小多余组合
			}
		}

		KindItem kindItem[] = new KindItem[27 * 9 + 7 + 14];
		for (int i = 0; i < kindItem.length; i++) {
			kindItem[i] = new KindItem();
		}
		int cbKindItemCount = 0;

		int[] dan_card_index = new int[GameConstants.MAX_COUNT];
		int dan_pai_count = 0;
		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			if (cbMagicCardIndex[i] == 0) {
				continue;
			}

			boolean falg = false;
			// 同牌判断
			if (cbMagicCardIndex[i] + cbMagicCardCount >= 3) { // 顺子
				if (cbKindItemCount >= kindItem.length) {
					break;
				}
				kindItem[cbKindItemCount].cbCardIndex[0] = i;
				kindItem[cbKindItemCount].cbCardIndex[1] = i;
				kindItem[cbKindItemCount].cbCardIndex[2] = i;
				kindItem[cbKindItemCount].cbWeaveKind = KAN_ZI;
				kindItem[cbKindItemCount].cbValidIndex[0] = cbMagicCardIndex[i] > 0 ? i
						: table._logic.get_magic_card_index(0);
				kindItem[cbKindItemCount].cbValidIndex[1] = cbMagicCardIndex[i] > 1 ? i
						: table._logic.get_magic_card_index(0);
				kindItem[cbKindItemCount].cbValidIndex[2] = cbMagicCardIndex[i] > 2 ? i
						: table._logic.get_magic_card_index(0);
				cbKindItemCount++;
				if (cbMagicCardIndex[i] + cbMagicCardCount >= 6)// 再加一个
				{
					if (cbKindItemCount >= kindItem.length) {
						break;
					}
					kindItem[cbKindItemCount].cbCardIndex[0] = i;
					kindItem[cbKindItemCount].cbCardIndex[1] = i;
					kindItem[cbKindItemCount].cbCardIndex[2] = i;
					kindItem[cbKindItemCount].cbWeaveKind = DUI_ZI;
					kindItem[cbKindItemCount].cbCenterCard = table._logic.switch_to_card_data(i);
					kindItem[cbKindItemCount].cbValidIndex[0] = cbMagicCardIndex[i] > 3 ? i
							: table._logic.get_magic_card_index(0);
					kindItem[cbKindItemCount].cbValidIndex[1] = table._logic.get_magic_card_index(0);
					kindItem[cbKindItemCount].cbValidIndex[2] = table._logic.get_magic_card_index(0);
					cbKindItemCount++;
				}
				has_kan = true;
				falg = true;
			} else if (cbMagicCardIndex[i] + cbMagicCardCount == 2) { // 对子
				if (cbKindItemCount >= kindItem.length) {
					break;
				}
				kindItem[cbKindItemCount].cbCardIndex[0] = i;
				kindItem[cbKindItemCount].cbCardIndex[1] = i;
				kindItem[cbKindItemCount].cbWeaveKind = KAN_ZI;
				kindItem[cbKindItemCount].cbValidIndex[0] = cbMagicCardIndex[i] > 0 ? i
						: table._logic.get_magic_card_index(0);
				kindItem[cbKindItemCount].cbValidIndex[1] = cbMagicCardIndex[i] > 1 ? i
						: table._logic.get_magic_card_index(0);
				cbKindItemCount++;
				if (cbMagicCardIndex[i] + cbMagicCardCount >= 6)// 再加一个
				{
					if (cbKindItemCount >= kindItem.length) {
						break;
					}
					kindItem[cbKindItemCount].cbCardIndex[0] = i;
					kindItem[cbKindItemCount].cbCardIndex[1] = i;
					kindItem[cbKindItemCount].cbWeaveKind = KAN_ZI;
					kindItem[cbKindItemCount].cbCenterCard = table._logic.switch_to_card_data(i);
					kindItem[cbKindItemCount].cbValidIndex[0] = cbMagicCardIndex[i] > 3 ? i
							: table._logic.get_magic_card_index(0);
					kindItem[cbKindItemCount].cbValidIndex[1] = table._logic.get_magic_card_index(0);
					cbKindItemCount++;
				}
				has_dui = true;
				falg = true;
			} else if ((i < (GameConstants.MAX_ZI - 2)) && ((i % 9) < 7)) {// 连牌判断
																			// end
				// 只要癞子牌数加上3个顺序索引的牌数大于等于3,则进行组合
				int chi_count = cbMagicCardIndex[i] + cbMagicCardIndex[i + 1] + cbMagicCardIndex[i + 2];
				chi_count += cbMagicCardCount;
				if (chi_count >= 3) {
					int cbIndex[] = { table._logic.is_magic_index(i) ? 0 : cbMagicCardIndex[i],
							table._logic.is_magic_index(i + 1) ? 0 : cbMagicCardIndex[i + 1],
							table._logic.is_magic_index(i + 2) ? 0
									: cbMagicCardIndex[i + 2] };

					int nMagicCountTemp = cbMagicCardCount;

					int cbValidIndex[] = new int[3];// 实际的牌
					while (nMagicCountTemp + cbIndex[0] + cbIndex[1] + cbIndex[2] >= 3) {
						for (int j = 0; j < cbIndex.length; j++) {
							if (cbIndex[j] > 0) {
								cbIndex[j]--;
								cbValidIndex[j] = i + j;
							} else {
								nMagicCountTemp--;
								cbValidIndex[j] = table._logic.get_magic_card_index(0);
							}
						}
						if (nMagicCountTemp >= 0) {
							if (cbKindItemCount >= kindItem.length) {
								break;
							}
							kindItem[cbKindItemCount].cbCardIndex[0] = i;
							kindItem[cbKindItemCount].cbCardIndex[1] = i + 1;
							kindItem[cbKindItemCount].cbCardIndex[2] = i + 2;
							kindItem[cbKindItemCount].cbWeaveKind = SHUN_ZI;

							for (int cbValidIndex_index = 0; cbValidIndex_index < 3; cbValidIndex_index++) {
								kindItem[cbKindItemCount].cbValidIndex[cbValidIndex_index] = cbValidIndex[cbValidIndex_index];
							}
							cbKindItemCount++;
							has_shun = true;
							falg = true;
						} else {
							break;
						}
					}
				}
			}
			if (!falg) {
				if (!table._logic.is_magic_index(i)) {
					dan_card_index[dan_pai_count] = i;
					dan_pai_count++;
				}
			}
		}

		// 挑选出牌值 (癞子牌不打出)
		if (dan_pai_count > 0) {
			int rust_index = dan_card_index[RandomUtil.getRandomNumber(dan_pai_count)];
			rust_card = table._logic.switch_to_card_data(rust_index);
		} else {
			int[] cards = new int[GameConstants.MAX_COUNT];
			int hand_count = table._logic.switch_to_cards_data(card_index, cards);
			int card = -1;
			do {
				card = cards[RandomUtil.getRandomNumber(hand_count)];
			} while (!table._logic.is_magic_card(card));
			rust_card = card;
		}

		return rust_card;
	}

	/**
	 * 麻将AI规则2.0版
	 * 
	 * @param table
	 * @param seat_index
	 * @return
	 */
	public static int getOutCardTwo(AbstractMJTable table, int seat_index) {
		int rust_card = GameConstants.INVALID_CARD;
		// 检测是否听牌
		if (table._playerStatus[seat_index]._hu_out_card_count > 0) {
			int[] cards = table._playerStatus[seat_index]._hu_out_card_ting;
			int ting_count = 0;
			int ret_index = GameConstants.INVALID_SEAT;
			for (int i = 0; i < cards.length; i++) {
				if (cards[i] == 0) {
					continue;
				}
				if (table._logic.is_magic_card(cards[i])) {
					continue;
				}
				if (table.GRR._cards_index[seat_index][table._logic.switch_to_card_index(cards[i])] == 0) {
					continue;
				}
				int ting_count_temp = table._playerStatus[seat_index]._hu_out_card_ting_count[i];
				if (ting_count_temp > ting_count) {
					ting_count = ting_count_temp;
					ret_index = i;
				}
			}
			return cards[ret_index];
		} else {

			int[][] array_ = new int[5][14]; // 牌型花色分类
			int[] array_count = new int[5]; // 花色牌型数量

			// 构造数据
			int[] card_index = table.GRR._cards_index[seat_index];
			int[] cbCardIndex = new int[GameConstants.MAX_INDEX];

			for (int i = 0; i < card_index.length; i++) {
				int index_temp = card_index[i];
				cbCardIndex[i] = index_temp;
				if (index_temp == 0) {
					continue;
				}
				int card = table._logic.switch_to_card_data(i);
				int color = table._logic.get_card_color(card);

				for (int c = 0; c < index_temp; c++) {
					array_[color][array_count[color]++] = card;
				}
			}

			List<Integer> outCards = new ArrayList<Integer>();
			// 风单优先
			for (int i = 0; i < 7; i++) {
				int index = table._logic.switch_to_card_index(0x31) + i;
				if (cbCardIndex[index] == 1 && !table._logic.is_magic_index(index)) {
					outCards.add(table._logic.switch_to_card_data(index));
				}
			}
			if (outCards.size() > 0) {
				return getRandomCard(table,outCards);
			}
			getOutCardListDan2(table, outCards, cbCardIndex, array_count, 1, new ArrayList<Integer>());
			if (outCards.size() > 0) {
				return getRandomCard(table,outCards);
			}
			// 先遍历全部单牌
			getOutCardListDan1(table, outCards, cbCardIndex, array_count, 1,new ArrayList<Integer>());
			if (outCards.size() > 0) {
				return getRandomCard(table,outCards);
			}
			// 顺单牌
			getOutCardListDan(table, outCards, cbCardIndex, array_count, 1,new ArrayList<Integer>());
			if (outCards.size() > 0) {
				return getRandomCard(table,outCards);
			}

			// 对子
			getOutCardListDuiZhi(table, outCards, cbCardIndex, array_count, 1);
			if (outCards.size() > 0) {
				return getRandomCard(table,outCards);
			}

			// getOutCardListShun(outCards, cbCardIndex, array_count, 0);
			// if (outCards.size() > 0) {
			// return getRandomCard(outCards);
			// }

			// 以上情况都没有找到：确保不会卡死随机出最后一张手牌
			int[] cards = new int[GameConstants.MAX_COUNT];
			int handCount = table._logic.switch_to_cards_data(cbCardIndex, cards);
			rust_card = cards[handCount - 1];
		}

		return rust_card;
	}

	/**
	 * 递归查找能单牌出牌牌的集合
	 * 
	 * @param outCards
	 *            出牌集合
	 * @param cardArray
	 *            牌值数组
	 * @param cardCount
	 *            牌值数量
	 * @param num
	 *            递归次数
	 * @return
	 */
	public static List<Integer> getOutCardListDan(AbstractMJTable table, List<Integer> outCards, int[] cbCardIndex,
			int[] cardCount, int num,List<Integer> failColors) {
		if (outCards.size() > 0 || num == 5) {
			return outCards;
		} else {
			int color = getMinCardColor(table, cardCount, num, cbCardIndex,failColors);
			if (color == GameConstants.INVALID_SEAT) {
				return outCards;
			}
			failColors.add(color);
			int start_index = getStartIndex(table, color);
			for (int i = 0; i < 9;) {
				int index = start_index + i;
				if (cbCardIndex[index] != 1 || table._logic.is_magic_index(index)) {
					i++;
					continue;
				}
				boolean flag = false;
				if (i == 8) {
					i++;
					continue;
				}
				for (int k = i + 1; k < 9;) {
					if (flag) {
						break;
					}
					int index2 = start_index + k;
					if (k == 8) {
						if (cbCardIndex[index2] == 1) {
							outCards.add(table._logic.switch_to_card_data(index2));
						}
						i++;
						break;
					} else if (cbCardIndex[index2] == 0 || cbCardIndex[index2] == 3) {
						i += 2;
						outCards.add(table._logic.switch_to_card_data(index));
						break;
					} else if (cbCardIndex[index2] == 2) {
						if (cbCardIndex[index2 + 1] == 1) {
							i += 3;
							outCards.add(table._logic.switch_to_card_data(index2));
							break;
						} else if (cbCardIndex[index2 + 1] == 2) {
							i += 3;
							break;
						}
					}
					for (int u = k + 1; u < 9;) {
						int index3 = start_index + u;
						if (cbCardIndex[index3] == 0) {
							i += 3;
							flag = true;
							outCards.add(table._logic.switch_to_card_data(index));
							outCards.add(table._logic.switch_to_card_data(index2));
							break;
						} else {
							i += 3;
							flag = true;
							break;
						}

					}
				}
			}

			num++;
			return getOutCardListDan(table, outCards, cbCardIndex, cardCount, num, failColors);
		}
	}

	public static List<Integer> getOutCardListDan1(AbstractMJTable table, List<Integer> outCards, int[] cbCardIndex,
			int[] cardCount, int num,List<Integer> failColors) {
		
		if (outCards.size() > 0 || num == 5) {
			return outCards;
		} else {
			int color = getMinCardColor(table, cardCount, num, cbCardIndex,failColors);
			if (color == GameConstants.INVALID_SEAT) {
				return outCards;
			}
			failColors.add(color);
			int start_index = getStartIndex(table, color);
			for (int i = 0; i < 9; i++) {
				int index = start_index + i;
				if (cbCardIndex[index] == 1 && !table._logic.is_magic_index(index)) {
					if (i == 0) {
						if (cbCardIndex[index + 1] == 0) {
							outCards.add(table._logic.switch_to_card_data(index));
						}
					} else if (i == 8) {
						if (cbCardIndex[index - 1] == 0) {
							outCards.add(table._logic.switch_to_card_data(index));
						}
					} else {
						if (cbCardIndex[index + 1] == 0 && cbCardIndex[index - 1] == 0) {
							outCards.add(table._logic.switch_to_card_data(index));
						}
					}
				}
			}
			num++;
			return getOutCardListDan(table, outCards, cbCardIndex, cardCount, num, failColors);
		}
	}
	
	
	public static List<Integer> getOutCardListDan2(AbstractMJTable table, List<Integer> outCards, int[] cbCardIndex,
			int[] cardCount, int num,List<Integer> failColors) {
		
		if (outCards.size() > 0 || num == 5) {
			return outCards;
		} else {
			int color = getMinCardColor(table, cardCount, num, cbCardIndex,failColors);
			if (color == GameConstants.INVALID_SEAT) {
				return outCards;
			}
			failColors.add(color);
			int start_index = getStartIndex(table, color);
			for (int i = 0; i < 9; i++) {
				int index = start_index + i;
				if (cbCardIndex[index] == 1 && !table._logic.is_magic_index(index)) {
					if (i == 0) {
						if (cbCardIndex[index + 1] == 0&& cbCardIndex[index + 2] == 0) {
							outCards.add(table._logic.switch_to_card_data(index));
						}
					} else if (i == 8) {
						if (cbCardIndex[index - 1] == 0&& cbCardIndex[index - 2] == 0) {
							outCards.add(table._logic.switch_to_card_data(index));
						}
					} else {
						if(i==1){
							if (cbCardIndex[index + 1] == 0 && cbCardIndex[index - 1] == 0 && cbCardIndex[index + 2] == 0 ) {
								outCards.add(table._logic.switch_to_card_data(index));
							}
						}else if(i == 7){
							if (cbCardIndex[index + 1] == 0 && cbCardIndex[index - 1] == 0  && cbCardIndex[index - 2] == 0) {
								outCards.add(table._logic.switch_to_card_data(index));
							}
						}else{
							if (cbCardIndex[index + 1] == 0 && cbCardIndex[index - 1] == 0 && cbCardIndex[index + 2] == 0 && cbCardIndex[index - 2] == 0) {
								outCards.add(table._logic.switch_to_card_data(index));
							}
						}
					}
				}
			}
			num++;
			return getOutCardListDan(table, outCards, cbCardIndex, cardCount, num, failColors);
		}
	}

	/**
	 * 递归查找能对子出牌牌的集合
	 * 
	 * @param outCards
	 *            出牌集合
	 * @param cardArray
	 *            牌值数组
	 * @param cardCount
	 *            牌值数量
	 * @param num
	 *            递归次数
	 * @return
	 */
	public static List<Integer> getOutCardListDuiZhi(AbstractMJTable table, List<Integer> outCards, int[] cbCardIndex,
			int[] cardCount, int num) {
		int cards[] = new int[7];
		int count = 0;
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if(table._logic.is_magic_index(i)){
				continue;
			}
			if (cbCardIndex[i] == 2) {
				cards[count] = table._logic.switch_to_card_data(i);
				count++;
			}
		}
		if (count > 1) {
			for (int i = 0; i < count; i++) {
				int color = table._logic.get_card_color(cards[i]);
				if (color > 2) {
					outCards.add(cards[i]);
				}
			}

			for (int i = 0; i < count; i++) {
				outCards.add(cards[i]);
			}
		}
		return outCards;
	}

	/**
	 * 递归查找能对子出牌牌的集合
	 * 
	 * @param outCards
	 *            出牌集合
	 * @param cardArray
	 *            牌值数组
	 * @param cardCount
	 *            牌值数量
	 * @param num
	 *            递归次数
	 * @return
	 */
	public static List<Integer> getOutCardListShun(AbstractMJTable table, List<Integer> outCards, int[] cbCardIndex,
			int[] cardCount, int num,List<Integer> failColors) {
		if (outCards.size() > 0) {
			return outCards;
		} else {
			int color = getMinCardColor(table, cardCount, num, cbCardIndex,failColors);
			if (color == GameConstants.INVALID_SEAT) {
				return outCards;
			}

			int start_index = getStartIndex(table, color);
			for (int i = 0; i < 9;) {
				int index = start_index + i;
				if (cbCardIndex[index] != 1) {
					i++;
					continue;
				}
				boolean flag = false;
				for (int k = i + 1; k < 9;) {
					if (flag) {
						break;
					}
					int index2 = start_index + k;
					if (cbCardIndex[index2] == 1) {
						i += 2;
						outCards.add(table._logic.switch_to_card_data(index));
						break;
					}
					for (int u = k + 1; u < 9;) {
						int index3 = start_index + u;
						if (cbCardIndex[index3] == 0) {
							i += 3;
							flag = true;
							outCards.add(table._logic.switch_to_card_data(index));
							outCards.add(table._logic.switch_to_card_data(index2));
							break;
						}

					}
				}
				// if(cbCardIndex[index] == 1){
				// if(i == 0){
				// if(cbCardIndex[index+1] == 0 ){
				// outCards.add(table._logic.switch_to_card_data(index));
				// }
				// }else if(i == 8){
				// if(cbCardIndex[index-1] == 0 ){
				// outCards.add(table._logic.switch_to_card_data(index));
				// }
				// }else{
				// if(cbCardIndex[index+1] == 0 && cbCardIndex[index-1] == 0 ){
				// outCards.add(table._logic.switch_to_card_data(index));
				// }
				// }
				// }
			}

			num++;
			return getOutCardListDan(table, outCards, cbCardIndex, cardCount, num, failColors);
		}
	}

	/**
	 * 获取开始位置
	 * 
	 * @param table
	 * @param color
	 * @return
	 */
	private static int getStartIndex(AbstractMJTable table, int color) {
		int card = 0x01;
		switch (color) {
		case 0:
			card = 0x01;
			break;
		case 1:
			card = 0x11;
			break;
		case 2:
			card = 0x21;
			break;
		case 3:
			card = 0x31;
			break;
		case 4:
			card = 0x41;
			break;

		default:
			break;
		}
		return table._logic.switch_to_card_index(card);

	}

	/**
	 * 获取现有数组中的最小非0牌值
	 * 
	 * @param cardCount
	 * @param num
	 * @return
	 */
	private static int getMinCardColor(AbstractMJTable table, int[] cardCount, int num, int[] cbCardIndex,List<Integer> failColors) {

		int rt = GameConstants.INVALID_SEAT;
		int[] tempSort = new int[cardCount.length];
		for (int i = 0; i < cardCount.length; i++) {
			tempSort[i] = cardCount[i];
		}
		for (int i = 0; i < tempSort.length - 1; i++) {
			for (int j = 0; j < tempSort.length - i - 1; j++) {
				if (tempSort[j] > tempSort[j + 1]) {
					int temp = tempSort[j];
					tempSort[j] = tempSort[j + 1];
					tempSort[j + 1] = temp;
				}
			}
		}

		int num_temp = 0;
		int count = 0;
		for (int i = 0; i < cardCount.length; i++) {
			if (tempSort[i] == 0) {
				continue;
			}
			count++;
			if (count == num) {
				num_temp = tempSort[i];
			}
		}
		if (num_temp == 0) {
			return rt;
		}

		for (int i = cardCount.length - 1; i >= 0; i--) {
			if(failColors.contains(i)){
				continue;
			}
			if (num_temp == cardCount[i]) {
				rt = i;
			}
		}

		return rt;
	}

	/**
	 * 处理出牌后续操作
	 * 
	 * @param danList
	 * @return
	 */
	private static int getRandomCard(AbstractMJTable table,List<Integer> outCards) {
		if (outCards.size() == 1) {
			return outCards.get(0);
		} else {
			for (Integer integer : outCards) {
				if(integer == null ){
					continue;
				}
				if(table._logic.get_card_value(integer)==9 || table._logic.get_card_value(integer)==1){
					return integer;
				}
			}
			return outCards.get(RandomUtil.getRandomNumber(outCards.size()));
		}
	}

	public static void AI_Operate_Card(AbstractMJTable table, int seat_index) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];
		// 最高有优先级操作
		int operate_code = table._logic.get_max_rank_action(playerStatus._action_count, playerStatus._action,
				GameConstants.WIK_NULL);
		// 操作牌
		int operate_card = playerStatus.get_weave_card(operate_code);
		// 执行操作
		if(operate_code == GameConstants.WIK_NULL){
			logger.error("获取不到操作优先级，默认最高优先级,执行第一个操作:对应子游戏请尽快解决优先级问题", playerStatus._action[0]);
			table.handler_operate_card(seat_index, GameConstants.WIK_NULL, 0, -1);
			
		}else if (playerStatus.has_action_by_code(operate_code) != false) {
			if(table._player_result.dingQueInfo.getNeedDingQue(seat_index)){
				table.handler_operate_card(seat_index, operate_code, operate_card, -1);
				table._player_result.dingQueInfo.needDingQueInVaild(seat_index);
			}else if (playerStatus.has_chi_hu() || playerStatus.has_zi_mo() || playerStatus.has_action_by_code(GameConstants.WIK_XIAO_HU)) {
				table.handler_operate_card(seat_index, operate_code, operate_card, -1);
			} else {
				table.handler_operate_card(seat_index, GameConstants.WIK_NULL, operate_card, -1);
			}
		}
	}

	/**
	 * 执行最高优先级判断
	 * 
	 * @param table
	 * @param seat_index
	 */
	public static void AI_Operate_Card_ALL(AbstractMJTable table, int seat_index) {
		logger.debug("有动作玩家Index：{}", seat_index);
		PlayerStatus playerStatus = table._playerStatus[seat_index];
		// 最高有优先级操作
		int operate_code = table._logic.get_max_rank_action(playerStatus._action_count, playerStatus._action,
				GameConstants.WIK_NULL);
		// 操作牌
		int operate_card = playerStatus.get_weave_card(operate_code);
		int cards_index[] = table.GRR._cards_index[seat_index];
		// 癞子牌不进行碰杠
		if (operate_code == GameConstants.WIK_PENG || operate_code == GameConstants.WIK_GANG) {
			if (table._logic.is_magic_card(operate_card)) {
				operate_code = GameConstants.WIK_NULL;
			}
		}
		if (operate_code == GameConstants.WIK_PENG) {
			int color = table._logic.get_card_color(operate_card);
			if (color <= 2) {
				int value = table._logic.get_card_value(operate_card);
				boolean flag = false;
				if (value == 1 || value == 9) {
					flag = true;
					// if
					// (cards_index[table._logic.switch_to_card_index((operate_card
					// + 1))] == 0) {
					// flag = true;
					// }
					// if
					// (cards_index[table._logic.switch_to_card_index((operate_card
					// - 1))] == 0) {
					// flag = true;
					// }
				} else {
					if (cards_index[table._logic.switch_to_card_index((operate_card - 1))] == 0
							|| cards_index[table._logic.switch_to_card_index((operate_card + 1))] == 0) {
						flag = true;
					}
				}
				if (!flag) {
					operate_code = table._logic.get_max_rank_action(playerStatus._action_count, playerStatus._action,
							GameConstants.WIK_PENG);
				}
			}
		}
		boolean need_check = false;
		int card1 = GameConstants.INVALID_CARD;
		int card2 = GameConstants.INVALID_CARD;
		if (operate_code == GameConstants.WIK_LEFT) {
			need_check = true;
			card1 = operate_card + 1;
			card2 = operate_card + 2;
		} else if (operate_code == GameConstants.WIK_RIGHT) {
			need_check = true;
			card1 = operate_card - 1;
			card2 = operate_card - 2;
		} else if (operate_code == GameConstants.WIK_CENTER) {
			need_check = true;
			card1 = operate_card + 1;
			card2 = operate_card - 2;
		}
		if (need_check) {
			if (cards_index[table._logic.switch_to_card_index((operate_card))] == 0
					|| cards_index[table._logic.switch_to_card_index((card1))] == 0
					|| cards_index[table._logic.switch_to_card_index((card2))] == 0) {
				if (cards_index[table._logic.switch_to_card_index((operate_card))] > 1
						|| cards_index[table._logic.switch_to_card_index((card1))] > 1
						|| cards_index[table._logic.switch_to_card_index((card2))] > 1) {
					operate_code = GameConstants.WIK_NULL;
				}
			} else {
				operate_code = GameConstants.WIK_NULL;
			}
		}
		// 已经听牌不进行吃碰操作
		if ((table._playerStatus[seat_index]._hu_out_card_count > 0
				|| table._playerStatus[seat_index]._hu_card_count > 0) && operate_code != GameConstants.WIK_GANG) {
			operate_code = GameConstants.WIK_NULL;
		}

		logger.debug("最高优先级动作：{}", operate_code);
		// 执行操作
		table.handler_operate_card(seat_index, operate_code, operate_card, -1);
	}

}
