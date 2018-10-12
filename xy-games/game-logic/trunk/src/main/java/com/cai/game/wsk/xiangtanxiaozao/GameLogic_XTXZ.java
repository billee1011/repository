package com.cai.game.wsk.xiangtanxiaozao;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.cai.common.constant.GameConstants;
import com.cai.common.util.RandomUtil;
import com.cai.game.wsk.data.tagAnalyseIndexResult_WSK;


class AnalyseIndexResult {
	public int card_index[] = new int[GameConstants.WSK_TC_MAX_INDEX];
	public int card_data[][] = new int[GameConstants.WSK_TC_MAX_INDEX][GameConstants.WSK_MAX_COUNT];

	public AnalyseIndexResult() {
		for (int i = 0; i < GameConstants.WSK_TC_MAX_INDEX; i++) {
			card_index[i] = 0;
			Arrays.fill(card_data[i], 0);
		}
	}

	public void reset() {
		for (int i = 0; i < GameConstants.WSK_TC_MAX_INDEX; i++) {
			card_index[i] = 0;
			Arrays.fill(card_data[i], 0);
		}
	}
};

public class GameLogic_XTXZ {
	public final int TRANSFER_TYPE_MIX_WSK = 101; // 转换后的杂五十K的牌型
	public final int TRANSFER_TYPE_PURE_WSK = 102; // 转换后的纯五十K的牌型
	public final int TRANSFER_TYPE_BOMB_4 = 103; // 转换后的炸弹的牌型(4炸)
	public final int TRANSFER_TYPE_KING_3 = 104; // 转换后的炸弹的牌型(3王炸)
	public final int TRANSFER_TYPE_BOMB_5 = 104; // 转换后的炸弹的牌型(4张以上的王炸)
	public final int TRANSFER_TYPE_KING_4 = 106; // 转换后的炸弹的牌型(4王炸或者2王炸)

	public Map<Integer, Integer> ruleMap = new HashMap<>();

	public boolean has_san_dai_er;
	public boolean has_shuang_wang;
	public GameLogic_XTXZ() {
	}

	public int[] get_boom_type_count(int[] cbCardData, int cbCardCount) {
		int[] count = new int[3];
		AnalyseIndexResult card_index = new AnalyseIndexResult();
		store_card_data(cbCardData, cbCardCount, card_index);
		for (int i = 0; i < GameConstants.WSK_TC_MAX_INDEX; i++) {
			if (card_index.card_index[i] == 8)
				count[0]++;
			if (card_index.card_index[i] == 7)
				count[1]++;
			if (card_index.card_index[i] == 6)
				count[2]++;
		}
		return count;
	}

	/**
	 * 获取出的牌是什么类型的，单张，对子，顺子，连对，飞机，炸弹等。 注意了，出牌的时候，有一个牌转换的过程。
	 * 
	 * @param cbCardData
	 * @param cbCardCount
	 * @return
	 */
	public int get_card_type_after_card_change(int cbCardData[], int cbCardCount) {
		if(cbCardCount <= 0){
			return GameConstants.TCDG_XTXZ_ERROR;
		}
		
		AnalyseIndexResult card_index = new AnalyseIndexResult();

		store_card_data(cbCardData, cbCardCount, card_index);

		int wang_count = get_wang_count_after_card_change(cbCardData, cbCardCount, card_index);
		int er_count = card_index.card_index[Constants_XTXZ.INDEX_CARD_TWO];

		if (wang_count == cbCardCount && cbCardCount > 1) {
			// 王牌只能单张出
			//return GameConstants.TCDG_CT_ERROR;
		}

		//王炸
		if (wang_count == cbCardCount) {
			if(cbCardCount >= 3){
				return GameConstants.TCDG_XTXZ_KING;
			}
		}

		if (card_index.card_index[switch_card_to_index(cbCardData[cbCardCount - 1])] == cbCardCount) {
			// 出的牌由正常牌组成（王牌已经经过转换）
			if (cbCardCount == 1) {
				return GameConstants.TCDG_XTXZ_SINGLE;
			}
			if (cbCardCount == 2) {
				if(get_card_color(cbCardData[0]) == 0x40 && has_shuang_wang){
					return GameConstants.TCDG_XTXZ_KING;
				}
				return GameConstants.TCDG_XTXZ_DOUBLE;
			}
			
			if (cbCardCount == 3) {
				if(has_san_dai_er){
					return GameConstants.TCDG_XTXZ_3_DAI_LOSS;
				}else{
					return GameConstants.TCDG_XTXZ_THREE;
				}
			}
			if (cbCardCount >= 4) {
				return GameConstants.TCDG_XTXZ_BOMB;
			}
			return GameConstants.TCDG_XTXZ_ERROR;
		}

		if (cbCardCount == 3) {
			// 510K
			int one_card_value = get_card_logic_value(cbCardData[0]);
			int two_card_value = get_card_logic_value(cbCardData[1]);
			int three_card_value = get_card_logic_value(cbCardData[2]);

			if (one_card_value == Constants_XTXZ.CARD_THIRTEEN && two_card_value == Constants_XTXZ.CARD_TEN
					&& three_card_value == Constants_XTXZ.CARD_FIVE) {
				int color = get_card_color(cbCardData[cbCardCount - 1]);

				for (int i = 0; i < cbCardCount - 1; i++) {
					if (get_card_color(cbCardData[i]) != color) {
						return GameConstants.TCDG_XTXZ_510K_DC;
					}
				}

				return GameConstants.TCDG_XTXZ_510K_SC;
			}
			return GameConstants.TCDG_XTXZ_ERROR;
		}
		
		//判断三带一
		if(cbCardCount == 4 && has_san_dai_er){
			for (int i = 0; i < cbCardCount; i++) {
				if (card_index.card_index[switch_card_to_index(cbCardData[i])] >= 3) {
					return GameConstants.TCDG_XTXZ_3_DAI_LOSS;
				}
			}
		}
		
		//判断三带二
		if(cbCardCount == 5 && has_san_dai_er){
			for (int i = 0; i < cbCardCount; i++) {
				if (card_index.card_index[switch_card_to_index(cbCardData[i])] >= 3) {
					return GameConstants.TCDG_XTXZ_3_DAI_2;
				}
			}
		}

		// 连对
		if (is_link(card_index, 2, cbCardCount / 2, cbCardData, cbCardCount) && er_count == 0) {
			return GameConstants.TCDG_XTXZ_DOUBLE_LINK;
		}
		
		if(has_san_dai_er){
			int plane = is_plane(card_index, cbCardData, cbCardCount);
			if(plane == 0){
				return GameConstants.TCDG_XTXZ_PLANE_LOSS;
			}else if(plane == 1){
				return GameConstants.TCDG_XTXZ_PLANE;
			}
		}else{
			// 连对
			if (is_link(card_index, 3, cbCardCount / 3, cbCardData, cbCardCount) && er_count == 0) {
				return GameConstants.TCDG_XTXZ_PLANE;
			}
		}

		return GameConstants.TCDG_XTXZ_ERROR;
	}
	
	/**
	 * 将手牌进行排序
	 * 
	 * @param cbCardData
	 * @param cbCardCount
	 * @param cbSortType
	 */
	public void sort_card_list_before_card_change_xtxz(int cbCardData[], int cbCardCount, int cbSortType) {
		if (cbCardCount == 0)
			return;


		int zheng_510K[] = new int[cbCardCount];
		int zheng_card_num = 0;

		int fu_510K[] = new int[cbCardCount];
		int fu_card_num = 0;

		if (cbSortType == GameConstants.WSK_ST_COUNT) {
			AnalyseIndexResult card_index = new AnalyseIndexResult();
			store_card_data(cbCardData, cbCardCount, card_index);

			int card_five_count = card_index.card_index[2];
			int card_ten_count = card_index.card_index[7];
			int card_thirteen_count = card_index.card_index[10];

			int[] cards_five = card_index.card_data[2];
			int[] cards_ten = card_index.card_data[7];
			int[] cards_thirteen = card_index.card_data[10];

			if (card_five_count > 0 && card_ten_count > 0 && card_thirteen_count > 0 ) {
				// 提取出所有的 正-五十K
				for (int i = 0; i < card_five_count; i++) {
					for (int j = 0; j < card_ten_count; j++) {
						for (int x = 0; x < card_thirteen_count; x++) {
							if (cards_five[i] == 0 || cards_ten[j] == 0 || cards_thirteen[x] == 0) {
								continue;
							}

							if (get_card_color(cards_five[i]) == get_card_color(cards_ten[j])
									&& get_card_color(cards_five[i]) == get_card_color(cards_thirteen[x])) {
								zheng_510K[zheng_card_num++] = cards_five[i];
								zheng_510K[zheng_card_num++] = cards_ten[j];
								zheng_510K[zheng_card_num++] = cards_thirteen[x];

								for (int y = 0; y < cbCardCount; y++) {
									if (cbCardData[y] == cards_five[i]) {
										cbCardData[y] = 0;
										break;
									}
								}

								for (int y = 0; y < cbCardCount; y++) {
									if (cbCardData[y] == cards_ten[j]) {
										cbCardData[y] = 0;
										break;
									}
								}

								for (int y = 0; y < cbCardCount; y++) {
									if (cbCardData[y] == cards_thirteen[x]) {
										cbCardData[y] = 0;
										break;
									}
								}

								cards_five[i] = 0;
								cards_ten[j] = 0;
								cards_thirteen[x] = 0;
							}
						}
					}
				}

				// 提取出所有的 副-五十K
				for (int i = 0; i < card_five_count; i++) {
					for (int j = 0; j < card_ten_count; j++) {
						for (int x = 0; x < card_thirteen_count; x++) {
							if (cards_five[i] == 0 || cards_ten[j] == 0 || cards_thirteen[x] == 0) {
								continue;
							}

							fu_510K[fu_card_num++] = cards_five[i];
							fu_510K[fu_card_num++] = cards_ten[j];
							fu_510K[fu_card_num++] = cards_thirteen[x];

							for (int y = 0; y < cbCardCount; y++) {
								if (cbCardData[y] == cards_five[i]) {
									cbCardData[y] = 0;
									break;
								}
							}

							for (int y = 0; y < cbCardCount; y++) {
								if (cbCardData[y] == cards_ten[j]) {
									cbCardData[y] = 0;
									break;
								}
							}

							for (int y = 0; y < cbCardCount; y++) {
								if (cbCardData[y] == cards_thirteen[x]) {
									cbCardData[y] = 0;
									break;
								}
							}

							cards_five[i] = 0;
							cards_ten[j] = 0;
							cards_thirteen[x] = 0;
						}
					}
				}
			}
			
			int temp[] = new int[cbCardCount];
			int temp_index = 0;
			for(int y = 0;y < cbCardCount;y++ ){
				if(cbCardData[y] == 0){
					continue;
				}
				temp[temp_index++] = cbCardData[y];
			}

			sort_card_list_before_card_change(temp, temp_index, GameConstants.WSK_ST_ORDER);


			int sort_num = 0;
			for (int i = 0; i < temp_index; i++) {
				cbCardData[sort_num++] = temp[i];
			}
			
			for (int i = 0; i < fu_card_num; i++) {
				cbCardData[sort_num++] = fu_510K[i];
			}
			
			for (int i = 0; i < zheng_card_num; i++) {
				cbCardData[sort_num++] = zheng_510K[i];
				
			}

			return;
		} else if (cbSortType == GameConstants.WSK_ST_ORDER) {
			int cbSortValue[] = new int[GameConstants.WSK_MAX_COUNT];
			for (int i = 0; i < cbCardCount; i++) {
				cbSortValue[i] = get_card_logic_value(cbCardData[i]);
			}

			boolean bSorted = true;
			int cbSwitchData = 0, cbLast = cbCardCount - 1;

			do {
				bSorted = true;
				for (int i = 0; i < cbLast; i++) {
					if ((cbSortValue[i] > cbSortValue[i + 1]) || ((cbSortValue[i] == cbSortValue[i + 1]) && (cbCardData[i] > cbCardData[i + 1]))) {
						bSorted = false;

						cbSwitchData = cbCardData[i];
						cbCardData[i] = cbCardData[i + 1];
						cbCardData[i + 1] = cbSwitchData;

						cbSwitchData = cbSortValue[i];
						cbSortValue[i] = cbSortValue[i + 1];
						cbSortValue[i + 1] = cbSwitchData;
					}
				}
				cbLast--;
			} while (bSorted == false);

			return;
		}
	}
	

	/**
	 * 将手牌进行排序
	 * 
	 * @param cbCardData
	 * @param cbCardCount
	 * @param cbSortType
	 */
	public void sort_card_list_before_card_change(int cbCardData[], int cbCardCount, int cbSortType) {
		if (cbCardCount == 0)
			return;

		int removeCount = 0;
		int[] removeCard = new int[cbCardCount];

		for (int i = 0; i < cbCardCount; i++) {
			if (cbCardData[i] > Constants_XTXZ.SPECIAL_CARD_TYPE) {
				removeCard[removeCount++] = cbCardData[i];
			}
		}

		if (removeCount > 0) {
			remove_card(removeCard, removeCount, cbCardData, cbCardCount);
			cbCardCount -= removeCount;
		}

		int zheng_510K[] = new int[cbCardCount];
		int zheng_card_num = 0;

		int fu_510K[] = new int[cbCardCount];
		int fu_card_num = 0;

		if (cbSortType == GameConstants.WSK_ST_COUNT) {
			AnalyseIndexResult card_index = new AnalyseIndexResult();
			store_card_data(cbCardData, cbCardCount, card_index);

			int card_five_count = card_index.card_index[2];
			int card_ten_count = card_index.card_index[7];
			int card_thirteen_count = card_index.card_index[10];

			int[] cards_five = card_index.card_data[2];
			int[] cards_ten = card_index.card_data[7];
			int[] cards_thirteen = card_index.card_data[10];

			if (card_five_count > 0 && card_ten_count > 0 && card_thirteen_count > 0 && card_five_count < 4 && card_ten_count < 4
					&& card_thirteen_count < 4) {
				// 提取出所有的 正-五十K
				for (int i = 0; i < card_five_count; i++) {
					for (int j = 0; j < card_ten_count; j++) {
						for (int x = 0; x < card_thirteen_count; x++) {
							if (cards_five[i] == 0 || cards_ten[j] == 0 || cards_thirteen[x] == 0) {
								continue;
							}

							if (get_card_color(cards_five[i]) == get_card_color(cards_ten[j])
									&& get_card_color(cards_five[i]) == get_card_color(cards_thirteen[x])) {
								zheng_510K[zheng_card_num++] = cards_five[i];
								zheng_510K[zheng_card_num++] = cards_ten[j];
								zheng_510K[zheng_card_num++] = cards_thirteen[x];

								for (int y = 0; y < cbCardCount; y++) {
									if (cbCardData[y] == cards_five[i]) {
										cbCardData[y] = 0;
										break;
									}
								}

								for (int y = 0; y < cbCardCount; y++) {
									if (cbCardData[y] == cards_ten[j]) {
										cbCardData[y] = 0;
										break;
									}
								}

								for (int y = 0; y < cbCardCount; y++) {
									if (cbCardData[y] == cards_thirteen[x]) {
										cbCardData[y] = 0;
										break;
									}
								}

								cards_five[i] = 0;
								cards_ten[j] = 0;
								cards_thirteen[x] = 0;
							}
						}
					}
				}

				// 提取出所有的 副-五十K
				for (int i = 0; i < card_five_count; i++) {
					for (int j = 0; j < card_ten_count; j++) {
						for (int x = 0; x < card_thirteen_count; x++) {
							if (cards_five[i] == 0 || cards_ten[j] == 0 || cards_thirteen[x] == 0) {
								continue;
							}

							fu_510K[fu_card_num++] = cards_five[i];
							fu_510K[fu_card_num++] = cards_ten[j];
							fu_510K[fu_card_num++] = cards_thirteen[x];

							for (int y = 0; y < cbCardCount; y++) {
								if (cbCardData[y] == cards_five[i]) {
									cbCardData[y] = 0;
									break;
								}
							}

							for (int y = 0; y < cbCardCount; y++) {
								if (cbCardData[y] == cards_ten[j]) {
									cbCardData[y] = 0;
									break;
								}
							}

							for (int y = 0; y < cbCardCount; y++) {
								if (cbCardData[y] == cards_thirteen[x]) {
									cbCardData[y] = 0;
									break;
								}
							}

							cards_five[i] = 0;
							cards_ten[j] = 0;
							cards_thirteen[x] = 0;
						}
					}
				}
			}

			sort_card_list_before_card_change(cbCardData, cbCardCount, GameConstants.WSK_ST_ORDER);

			card_index.reset();

			store_card_data(cbCardData, cbCardCount, card_index);

			int index[] = new int[GameConstants.WSK_TC_MAX_INDEX];
			for (int i = GameConstants.WSK_TC_MAX_INDEX - 1; i >= 0; i--) {
				index[i] = i;
			}

			for (int i = GameConstants.WSK_TC_MAX_INDEX - 1; i >= 0; i--) {
				for (int j = i - 1; j >= 0; j--) {
					int count_i = card_index.card_index[index[i]];
					int count_j = card_index.card_index[index[j]];

					if (count_i > count_j) {
						int temp = index[j];
						index[j] = index[i];
						index[i] = temp;
					} else if (count_i == count_j) {
						if (index[i] > index[j]) {
							int temp = index[j];
							index[j] = index[i];
							index[i] = temp;
						}
					}
				}
			}

			int sort_num = 0;

			for (int i = GameConstants.WSK_TC_MAX_INDEX - 1; i >= 0; i--) {
				int count = card_index.card_index[index[i]];
				if (count < 4) {
					for (int j = 0; j < count; j++) {
						cbCardData[sort_num++] = card_index.card_data[index[i]][j];
					}
				}
			}

			sort_card_list_before_card_change(cbCardData, sort_num, GameConstants.WSK_ST_ORDER);

			for (int i = 0; i < fu_card_num; i++) {
				cbCardData[sort_num++] = fu_510K[i];
			}

			for (int i = 0; i < zheng_card_num; i++) {
				cbCardData[sort_num++] = zheng_510K[i];
			}

			for (int i = GameConstants.WSK_TC_MAX_INDEX - 1; i >= 0; i--) {
				int count = card_index.card_index[index[i]];
				if (count >= 4) {
					for (int j = 0; j < count; j++) {
						cbCardData[sort_num++] = card_index.card_data[index[i]][j];
					}
				}
			}

			for (int i = 0; i < removeCount; i++) {
				cbCardData[sort_num++] = removeCard[i];
			}

			return;
		} else if (cbSortType == GameConstants.WSK_ST_ORDER) {
			int cbSortValue[] = new int[GameConstants.WSK_MAX_COUNT];
			for (int i = 0; i < cbCardCount; i++) {
				cbSortValue[i] = get_card_logic_value(cbCardData[i]);
			}

			boolean bSorted = true;
			int cbSwitchData = 0, cbLast = cbCardCount - 1;

			do {
				bSorted = true;
				for (int i = 0; i < cbLast; i++) {
					if ((cbSortValue[i] > cbSortValue[i + 1]) || ((cbSortValue[i] == cbSortValue[i + 1]) && (cbCardData[i] > cbCardData[i + 1]))) {
						bSorted = false;

						cbSwitchData = cbCardData[i];
						cbCardData[i] = cbCardData[i + 1];
						cbCardData[i + 1] = cbSwitchData;

						cbSwitchData = cbSortValue[i];
						cbSortValue[i] = cbSortValue[i + 1];
						cbSortValue[i + 1] = cbSwitchData;
					}
				}
				cbLast--;
			} while (bSorted == false);

			for (int i = 0; i < removeCount; i++) {
				cbCardData[cbCardCount++] = removeCard[i];
			}

			return;
		}
	}

	/**
	 * 将打出的牌进行排序
	 * 
	 * @param cbCardData
	 * @param cbCardCount
	 * @param cbSortType
	 */
	public void sort_out_card_list(int cbCardData[], int cbCardCount) {
		if (cbCardCount == 0)
			return;

		int cbSortValue[] = new int[GameConstants.WSK_MAX_COUNT];
		for (int i = 0; i < cbCardCount; i++) {
			if ((cbCardData[i] & 0xFF) == Constants_XTXZ.CARD_SMALL_MAGIC)
				cbSortValue[i] = -1;
			else if ((cbCardData[i] & 0xFF) == Constants_XTXZ.CARD_BIG_MAGIC)
				cbSortValue[i] = -2;
			else if ((cbCardData[i] & 0xFF) == Constants_XTXZ.FLOWER_CARD)
				cbSortValue[i] = -3;
			else
				cbSortValue[i] = get_card_logic_value(cbCardData[i]);
		}

		boolean bSorted = true;
		int cbSwitchData = 0, cbLast = cbCardCount - 1;

		do {
			bSorted = true;
			for (int i = 0; i < cbLast; i++) {
				if ((cbSortValue[i] < cbSortValue[i + 1])
						|| ((cbSortValue[i] == cbSortValue[i + 1]) && ((cbCardData[i] & 0xFF) < (cbCardData[i + 1] & 0xFF)))) {
					bSorted = false;

					cbSwitchData = cbCardData[i];
					cbCardData[i] = cbCardData[i + 1];
					cbCardData[i + 1] = cbSwitchData;

					cbSwitchData = cbSortValue[i];
					cbSortValue[i] = cbSortValue[i + 1];
					cbSortValue[i + 1] = cbSwitchData;
				}
			}
			cbLast--;
		} while (bSorted == false);

		return;
	}
	
	//三代二和飞机排序
	public void sort_card_date_list_by_type(int card_date[], int card_count, int type) {
		switch (type) {
			case GameConstants.TCDG_XTXZ_3_DAI_2:
			case GameConstants.TCDG_XTXZ_PLANE: {
				AnalyseIndexResult card_index = new AnalyseIndexResult();
				store_card_data(card_date, card_count, card_index);
				int count = 0;
				for (int i = 0; i < card_count; i++) {
					int index = switch_card_to_index(card_date[i]);
					if (card_index.card_index[index] >= 3) {
						for (int j = 0; j < 3; j++) {
							card_date[count++] = card_index.card_data[index][j];
							//card_data_index.card_data[index][j] = 0;
						}
						if(card_index.card_index[index] == 4){
							card_index.card_data[index][0] = card_index.card_data[index][3];
						}
						card_index.card_index[index] -= 3;
						
					}
				}
				for (int i = 0; i < GameConstants.WSK_MAX_INDEX; i++) {
					if (card_index.card_index[i] > 0) {
						for (int j = 0; j < card_index.card_index[i]; j++) {
							card_date[count++] = card_index.card_data[i][j];
						}
					}
				}
				return ;
			}
		}
	}
	
	//获取飞机的连对数量
	public int get_three_link_count(int cbCardData[], int cbCardCount) {
		if(!this.has_san_dai_er){
			return cbCardCount / 3;
		}
		int type = get_card_type_after_card_change(cbCardData, cbCardCount);
		AnalyseIndexResult card_data_index = new AnalyseIndexResult();
		store_card_data(cbCardData, cbCardCount, card_data_index);
		if (type == GameConstants.TCDG_XTXZ_PLANE_LOSS) {
			for (int i = GameConstants.WSK_MAX_INDEX - 1; i >= 0; i--) {
				// 三个2不能当做飞机
				if (card_data_index.card_index[i] >= 3) {
					int link_num = 1;
					int prv_index = i;
					for (int j = i - 1; j >= 0; j--) {
						if (card_data_index.card_index[j] >= 3 && prv_index == j + 1) {
							prv_index = j;
							link_num++;
						} else {
							if (link_num > 1 && link_num * 5 > cbCardCount) {
								return link_num;
							}
						}
						if (j == 0) {
							if (link_num > 1 && link_num * 5 > cbCardCount) {
								return link_num;
							}
						}
					}
				}
			}
		} else {
			for (int i = GameConstants.WSK_MAX_INDEX - 1; i >= 0; i--) {
				// 三个2不能当做飞机
				if (card_data_index.card_index[i] >= 3) {
					int link_num = 1;
					int prv_index = i;
					for (int j = i - 1; j >= 0; j--) {
						if (card_data_index.card_index[j] >= 3 && prv_index == j + 1) {
							prv_index = j;
							link_num++;
							if (link_num * 5 == cbCardCount) {
								return link_num;
							}
						}
						if (j == 0) {
							if (link_num > 1 && link_num * 5 == cbCardCount) {
								return link_num;
							}
						}
					}
				}
			}
		}
		return 0;
	}
	
	

	public boolean remove_card(int cbRemoveCard[], int cbRemoveCount, int cbCardData[], int cbCardCount) {
		int cbDeleteCount = 0, cbTempCardData[] = new int[GameConstants.WSK_MAX_COUNT];
		if (cbCardCount > cbTempCardData.length)
			return false;
		for (int i = 0; i < cbCardCount; i++) {
			cbTempCardData[i] = cbCardData[i];
		}

		for (int i = 0; i < cbRemoveCount; i++) {
			for (int j = 0; j < cbCardCount; j++) {
				if (cbRemoveCard[i] == cbTempCardData[j]) {
					cbDeleteCount++;
					cbTempCardData[j] = 0;
					break;
				}
			}
		}
		if (cbDeleteCount != cbRemoveCount)
			return false;

		int cbCardPos = 0;
		for (int i = 0; i < cbCardCount; i++) {
			if (cbTempCardData[i] != 0)
				cbCardData[cbCardPos++] = cbTempCardData[i];
		}

		return true;
	}

	public int transfer_card_type(int sourceCardType,int card_count) {
		if (sourceCardType == GameConstants.TCDG_CT_510K_DC)
			return TRANSFER_TYPE_MIX_WSK;
		if (sourceCardType == GameConstants.TCDG_CT_510K_SC)
			return TRANSFER_TYPE_PURE_WSK;
		if (sourceCardType == GameConstants.TCDG_CT_BOMB && card_count == 4)
			return TRANSFER_TYPE_BOMB_4;
		if (sourceCardType == GameConstants.TCDG_CT_BOMB && card_count > 4)
			return TRANSFER_TYPE_BOMB_5;
		if (sourceCardType == GameConstants.TCDG_XTXZ_KING && card_count == 3)
			return TRANSFER_TYPE_KING_3;
		if(sourceCardType == GameConstants.TCDG_XTXZ_KING && (card_count == 2 || card_count == 4))
			return TRANSFER_TYPE_KING_4;
		return sourceCardType;
	}

	/**
	 * 比较两个玩家出的牌的大小。牌数据已经用make_change_card进行合理的转换。
	 * 
	 * @param cbFirstCard
	 * @param cbNextCard
	 * @param cbFirstCount
	 * @param cbNextCount
	 * @return
	 */
	public boolean compare_card(int cbFirstCard[], int cbNextCard[], int cbFirstCount, int cbNextCount) {
		int cbNextType = get_card_type_after_card_change(cbNextCard, cbNextCount);
		int cbFirstType = get_card_type_after_card_change(cbFirstCard, cbFirstCount);

		cbNextType = transfer_card_type(cbNextType,cbNextCount);
		cbFirstType = transfer_card_type(cbFirstType,cbFirstCount);
		
		if(cbNextType == GameConstants.TCDG_XTXZ_3_DAI_LOSS){
			cbNextType = GameConstants.TCDG_XTXZ_3_DAI_2;
		}
		if(cbNextType == GameConstants.TCDG_XTXZ_PLANE_LOSS){
			cbNextType = GameConstants.TCDG_XTXZ_PLANE;
		}
		
		if(cbFirstType == GameConstants.TCDG_XTXZ_3_DAI_LOSS){
			cbFirstType = GameConstants.TCDG_XTXZ_3_DAI_2;
		}
		if(cbFirstType == GameConstants.TCDG_XTXZ_PLANE_LOSS){
			cbFirstType = GameConstants.TCDG_XTXZ_PLANE;
		}

		if (cbNextType >= TRANSFER_TYPE_MIX_WSK && cbFirstType < TRANSFER_TYPE_MIX_WSK)
			return true;

		if (cbNextType < TRANSFER_TYPE_MIX_WSK && cbFirstType >= TRANSFER_TYPE_MIX_WSK) {
			return false;
		}

		if (cbNextType >= TRANSFER_TYPE_MIX_WSK && cbFirstType >= TRANSFER_TYPE_MIX_WSK) {
			if (cbNextType == cbFirstType) {
				if (cbNextType == TRANSFER_TYPE_PURE_WSK) {
					// 正五十K之间不按花色分大小
					return false;
				} else {
					if (cbFirstCount == cbNextCount) {
						// 同样数量牌的炸弹，看牌值的大小
						return get_card_logic_value(cbNextCard[0]) > get_card_logic_value(cbFirstCard[0]);
					} else {
						// 不同数量牌的炸弹，看牌的数量
						return cbNextCount > cbFirstCount;
					}
				}
			} else {
				// 如果大于杂五十K的牌型，并且牌型不相等，直接比牌型大小
				return cbNextType > cbFirstType;
			}
		}

		// 比较五十K以下的牌型大小。只判断同样的牌型，同样的牌数量。
		if (cbNextType != cbFirstType /*|| cbFirstCount != cbNextCount*/) {
			return false;
		} else {
			sort_card_date_list_by_type(cbFirstCard, cbFirstCount,cbFirstType);
			sort_card_date_list_by_type(cbNextCard, cbNextCount,cbNextType);
			int first_value = get_card_logic_value(cbFirstCard[0]);
			int next_value = get_card_logic_value(cbNextCard[0]);

			if (cbNextType == GameConstants.TCDG_CT_SINGLE || cbNextType == GameConstants.TCDG_CT_DOUBLE 
					|| cbNextType == GameConstants.TCDG_CT_THREE|| cbNextType == GameConstants.TCDG_CT_DOUBLE_LINK 
					|| cbNextType == GameConstants.TCDG_CT_PLANE || cbNextType == GameConstants.TCDG_XTXZ_3_DAI_2) {
				return next_value > first_value;
			}
		}

		return false;
	}

	private static void random_cards(int card_data[], int return_cards[], int card_count) {
		int bRandCount = 0, bPosition = 0;
		do {
			bPosition = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % (card_count - bRandCount));
			return_cards[bRandCount++] = card_data[bPosition];
			card_data[bPosition] = card_data[card_count - bRandCount];
		} while (bRandCount < card_count);
	}

	public void random_card_data(int return_cards[], final int mj_cards[]) {
		int card_count = return_cards.length;
		int card_data[] = new int[card_count];
		for (int i = 0; i < card_count; i++) {
			card_data[i] = mj_cards[i];
		}
		random_cards(card_data, return_cards, card_count);
	}

	/**
	 * 获取牌数据里的分值，5对应5分，10或K对应10分
	 * 
	 * @param cbCardData
	 * @param cbCardCount
	 * @return
	 */
	public int get_card_score(int cbCardData[], int cbCardCount) {
		int score = 0;
		for (int i = 0; i < cbCardCount; i++) {
			if (get_card_logic_value(cbCardData[i]) == Constants_XTXZ.CARD_FIVE) {
				score += 5;
			} else if (get_card_logic_value(cbCardData[i]) == Constants_XTXZ.CARD_TEN
					|| get_card_logic_value(cbCardData[i]) == Constants_XTXZ.CARD_THIRTEEN) {
				score += 10;
			}
		}
		return score;
	}

	public int get_card_value(int cbCardData) {
		return cbCardData & GameConstants.LOGIC_MASK_VALUE;
	}

	public int get_card_color(int cbCardData) {
		return cbCardData & GameConstants.LOGIC_MASK_COLOR;
	}

	public int get_card_color_one_bit(int cbCardData) {
		return (cbCardData & GameConstants.LOGIC_MASK_COLOR) >> 4;
	}

	/**
	 * 分析出的牌是不是连对。
	 * 
	 * @param card_data_index
	 *            中间过程存储的牌数据。
	 * @param link_num
	 *            几张牌的连对，为2张或3张的连对
	 * @param link_count_num
	 *            根据出的牌的数量和几张牌的连对计算出来的值
	 * @return
	 */
	public boolean is_link(AnalyseIndexResult card_data_index, int link_num, int link_count_num, int cbCardData[], int cbCardCount) {
		int pai_count = 0;
		int wang_count = get_wang_count_after_card_change(cbCardData, cbCardCount, card_data_index);
		wang_count = 0;

		for (int i = 0; i < GameConstants.WSK_TC_MAX_INDEX; i++) {
			// 打出的牌，一共有几张
			pai_count += card_data_index.card_index[i];
		}

		int sum = 0;
		for (int i = 0; i < GameConstants.WSK_TC_MAX_INDEX - Constants_XTXZ.INDEX_FENCE; i++) {
			if (card_data_index.card_index[i] == 0) {
				if (sum == 0) {
					continue;
				} else {
					if (sum >= link_count_num && (sum * link_num == pai_count)) {
						// 如果分析出来的连对数目大于或等于实际的，并且牌数据符合
						return true;
					} else {
						return false;
					}
				}
			}

			if (card_data_index.card_index[i] == link_num) {
				sum++;
			} else if (card_data_index.card_index[i] > link_num) {
				// 如果打出牌的真实牌数据的单种类牌，大于2或者大于3了，那肯定不是连对了
				return false;
			} else {
				if (card_data_index.card_index[i] + wang_count >= link_num) {
					// 如果打出的牌里有王牌
					sum++;

					wang_count -= link_num - card_data_index.card_index[i];
				} else {
					return false;
				}
			}
		}

		if (sum >= link_count_num) {
			return true;
		} else {
			return false;
		}
	}
	
	//  0飞机缺翅膀     1飞机
	public int is_plane(AnalyseIndexResult card_data_index, int cbCardData[], int cbCardCount) {
		if (cbCardCount < 6) {
			return -1;
		}
		for (int i = GameConstants.WSK_MAX_INDEX - 1/*4*/; i >= 0; i--) {
			// 三个2不能当做飞机
			if (card_data_index.card_index[i] >= 3) {
				int link_num = 1;
				for (int j = i - 1; j >= 0; j--) {
					if (card_data_index.card_index[j] >= 3) {
						link_num++;
						if (link_num * 5 == cbCardCount) {
							return 1;
						} else if (link_num * 5 > cbCardCount) {
							return 0;
						}
					} else {
						i = j + 1;
						break;
					}
				}
			}
		}

		return -1;
	}

	/**
	 * 获取十六进制牌型的逻辑牌值。 A的十六进制为0x01，逻辑牌值为1+13。 2的十六进制为0x02，逻辑牌值为2+13。
	 * 小王的十六进制为0x4E，逻辑牌值为14+2。大王的十六进制为0x4F，逻辑牌值为15+2。花牌的十六进制为0x5F，逻辑牌值为15+3。
	 * 
	 * @param CardData
	 * @return
	 */
	public int get_card_logic_value(int CardData) {
		int cbCardColor = get_card_color(CardData);
		int cbCardValue = get_card_value(CardData);

		if (cbCardColor == 0x40)
			return cbCardValue + 2;

		if (cbCardColor == 0x50)
			return cbCardValue + 3;

		return (cbCardValue == 2 || cbCardValue == 1) ? (cbCardValue + 13) : cbCardValue;
	}

	/**
	 * 获取大小王和花牌的张数
	 * 
	 * @param cbCardData
	 * @param cbCardCount
	 * @return
	 */
	public int get_wang_count_before_card_change(int cbCardData[], int cbCardCount) {
		int count = 0;
		for (int i = 0; i < cbCardCount; i++) {
			if ((cbCardData[i] & 0xFF) >= Constants_XTXZ.CARD_SMALL_MAGIC)
				count++;
		}

		return count;
	}

	/**
	 * 获取大小王的张数
	 * 
	 * @param cbCardData
	 * @param cbCardCount
	 * @return
	 */
	public int get_magic_count(int cbCardData[], int cbCardCount) {
		AnalyseIndexResult card_index = new AnalyseIndexResult();

		store_card_data(cbCardData, cbCardCount, card_index);

		return card_index.card_index[13] + card_index.card_index[14];
	}

	/**
	 * 获取花牌的张数
	 * 
	 * @param cbCardData
	 * @param cbCardCount
	 * @return
	 */
	public int get_flower_count(int cbCardData[], int cbCardCount) {
		AnalyseIndexResult card_index = new AnalyseIndexResult();

		store_card_data(cbCardData, cbCardCount, card_index);

		return card_index.card_index[15];
	}

	/**
	 * 牌进行转换之后，获取大小王和花牌的张数
	 * 
	 * @param cbCardData
	 * @param cbCardCount
	 * @param AnalyseIndexResult
	 * @return
	 */
	public int get_wang_count_after_card_change(int cbCardData[], int cbCardCount, AnalyseIndexResult AnalyseIndexResult) {
		int count = 0;
		for (int i = 0; i < cbCardCount; i++) {
			if (cbCardData[i] > Constants_XTXZ.SPECIAL_CARD_TYPE)
				count++;
		}
		count += AnalyseIndexResult.card_index[13] + AnalyseIndexResult.card_index[14] + AnalyseIndexResult.card_index[15];
		return count;
	}

	/**
	 * 保存中间分析结果的十六进制牌值数据，和每种牌的张数。
	 * 
	 * @param cbCardData
	 * @param cbCardCount
	 * @param analyseIndexResult
	 */
	public void store_card_data(int cbCardData[], int cbCardCount, AnalyseIndexResult analyseIndexResult) {
		for (int i = 0; i < cbCardCount; i++) {
			int index = switch_card_to_index(cbCardData[i]);

			if (index < 0)
				continue;

			int count = analyseIndexResult.card_index[index];

			analyseIndexResult.card_data[index][count] = cbCardData[i];

			analyseIndexResult.card_index[index]++;
		}
	}

	/**
	 * 根据十六进制牌值，获取到实际的牌值索引。先获取到逻辑牌值，再获取到时间的索引。
	 * 3-K的索引为0-10，A为11，2为12，小王为13，大王为14，花牌为15。
	 * 
	 * @param card
	 * @return
	 */
	public int switch_card_to_index(int card) {
		int index = get_card_logic_value(card) - 3;
		return index;
	}

	/**
	 * 将牌的索引转换成牌值（统一转成方块0x0*）。 索引0-10对应3-K。 索引11对应14（A）。 索引12对应15（2）。不会转大小王和花牌
	 * 
	 * @param index
	 * @return
	 */
	public int switch_index_to_value(int index) {
		return index + 3;
	}

	/**
	 * 将打出的手牌数据，进行合理的转换，然后再进行排序判断
	 * 
	 * @param cbCardData
	 * @param cbCardCount
	 * @param cbRealData
	 * @param type
	 */
	public void make_change_card(int cbCardData[], int cbCardCount, int cbRealData[], int type) {
		AnalyseIndexResult card_index = new AnalyseIndexResult();

		store_card_data(cbRealData, cbCardCount, card_index);

		int wang_count = get_wang_count_before_card_change(cbRealData, cbCardCount);
		for (int i = 0; i < cbCardCount; i++) {
			cbCardData[i] = cbRealData[i];
		}

		if (wang_count == 0) {
			// 如果打出的牌里面没王牌，不需要转换
			return;
		}

		if (wang_count == cbCardCount) {
			// 如果打出的牌全部为王牌
			for (int i = 0; i < cbCardCount; i++) {
				cbCardData[i] = Constants_XTXZ.CARD_THREE + Constants_XTXZ.SPECIAL_CARD_TYPE;
			}
			return;
		}

		int tmp_count = card_index.card_index[switch_card_to_index(cbCardData[0])];
		if (wang_count + tmp_count == cbCardCount) {
			// 如果打出的牌，只有一种正常牌，再加上王牌
			for (int i = 0; i < cbCardCount; i++) {
				if (cbCardData[i] >= Constants_XTXZ.CARD_SMALL_MAGIC) {
					cbCardData[i] = cbCardData[0] + Constants_XTXZ.SPECIAL_CARD_TYPE;
				}
			}
			return;
		}

		if (cbCardCount == 3) {
			// 如果打出的牌为三张，并且有五十K里的两张牌+1张王牌
			int num = 0;

			for (int i = 0; i < cbCardCount; i++) {
				if (get_card_logic_value(cbCardData[i]) == Constants_XTXZ.CARD_FIVE) {
					num++;
				}
				if (get_card_logic_value(cbCardData[i]) == Constants_XTXZ.CARD_TEN) {
					num++;
				}
				if (get_card_logic_value(cbCardData[i]) == Constants_XTXZ.CARD_THIRTEEN) {
					num++;
				}
			}

			if (num == 2 && wang_count == 1) {
				if (get_card_color(cbCardData[0]) == get_card_color(cbCardData[1])) {
					cbCardData[2] = get_card_color_one_bit(cbCardData[0]) * Constants_XTXZ.CARD_MASK + Constants_XTXZ.SUM_WSK
							- get_card_value(cbCardData[0]) - get_card_value(cbCardData[1]) + Constants_XTXZ.SPECIAL_CARD_TYPE;
				} else {
					cbCardData[2] = Constants_XTXZ.COLOR_SPACE * Constants_XTXZ.CARD_MASK + Constants_XTXZ.SUM_WSK
							- get_card_value(cbCardData[0]) - get_card_value(cbCardData[1]) + Constants_XTXZ.SPECIAL_CARD_TYPE;
				}
			}

			return;
		}

		// 打出的牌里面，数量最多的一种牌
		int max_count = 0;
		for (int i = 0; i < cbCardCount - wang_count; i++) {
			int index = switch_card_to_index(cbCardData[i]);

			if (card_index.card_index[index] > max_count) {
				max_count = card_index.card_index[index];
			}
		}

		if (max_count >= 4) {
			// 分析到这里之后 只能是2连或3连的牌型
			return;
		}

		// 如果是2连对，牌数不能过2，并且需要补齐的牌数目等于王牌数目，否则自动认为是3连对
		int link_num = 2;
		if (max_count == 1 || max_count == 2) {
			int magic_needed = 0;
			for (int i = 0; i < cbCardCount - wang_count; i++) {
				int index = switch_card_to_index(cbCardData[i]);

				if (card_index.card_index[index] == 1) {
					magic_needed++;
				}
			}
			if (magic_needed == wang_count) {
				link_num = 2;
			} else {
				link_num = 3;
			}
		} else {
			link_num = 3;
		}

		// 根据是2连还是3连进行相应的转换
		for (int i = 0; i < cbCardCount - wang_count;) {
			int count = card_index.card_index[switch_card_to_index(cbCardData[i])];

			if (count < link_num) {
				int temp = 0;

				for (int j = cbCardCount - wang_count; j < cbCardCount;) {
					cbCardData[j] = cbCardData[i] + Constants_XTXZ.SPECIAL_CARD_TYPE;

					temp++;
					wang_count--;

					if (temp >= link_num - count || wang_count <= 0) {
						break;
					}

					j += count;
				}
			}

			i += count;
		}

		store_card_data(cbCardData, cbCardCount, card_index);

		if (wang_count == 0) {
			return;
		} else {
			int max_index = switch_card_to_index(cbCardData[cbCardCount - wang_count]);
			int min_index = switch_card_to_index(cbCardData[0]);

			for (int i = max_index + 1; i < GameConstants.WSK_TC_MAX_INDEX - Constants_XTXZ.INDEX_FENCE; i++) {
				for (int j = 0; j < link_num; j++) {
					cbCardData[cbCardCount - wang_count] = switch_index_to_value(i) + Constants_XTXZ.SPECIAL_CARD_TYPE;

					wang_count--;

					if (wang_count <= 0) {
						return;
					}
				}

			}

			if (wang_count == 0) {
				return;
			}

			for (int i = min_index - 1; i >= 0; i--) {
				for (int j = 0; j < link_num; j++) {
					cbCardData[cbCardCount - wang_count] = switch_index_to_value(i) + Constants_XTXZ.SPECIAL_CARD_TYPE;

					wang_count--;

					if (wang_count <= 0) {
						return;
					}
				}
			}
		}
	}

	/**
	 * 全局搜索哪些牌能压上家出的牌
	 * 
	 * @param hand_card_data
	 * @param cbHandCardCount
	 * @param cbOutCardData
	 * @param out_card_count
	 * @param can_out_cards
	 * @return
	 */
	public int search_can_out_cards(int hand_card_data[], int cbHandCardCount, int cbOutCardData[], int out_card_count, int can_out_cards[]) {
		int cbHandCardData[] = Arrays.copyOf(hand_card_data, cbHandCardCount);

		int can_out_card_count = 0;

		// 标记能出的牌，不管手牌里是否有两张一样的牌，最后轮询手牌，进行转换
		Set<Integer> set = new HashSet<>();


		
		int card_type = get_card_type_after_card_change(cbOutCardData, out_card_count);
		
		if(card_type == GameConstants.TCDG_XTXZ_3_DAI_LOSS){
			card_type = GameConstants.TCDG_XTXZ_3_DAI_2;
		}
		if(card_type == GameConstants.TCDG_XTXZ_PLANE_LOSS){
			card_type = GameConstants.TCDG_XTXZ_PLANE;
		}
		
		//排一下序
		sort_card_date_list_by_type( cbOutCardData, out_card_count,card_type);
		
		//出了王炸就要不起了
		if(card_type == GameConstants.TCDG_XTXZ_KING && (out_card_count == 2 || out_card_count == 4)){
			return can_out_card_count;
		}

		
		sort_card_list_before_card_change(cbHandCardData, cbHandCardCount, GameConstants.WSK_ST_ORDER);

		int magic_count = get_wang_count_before_card_change(cbHandCardData, cbHandCardCount);
		int king_count = get_wang_count_before_card_change(cbHandCardData, cbHandCardCount);
		magic_count = 0;
		boolean has_wsk = false;

		int[] tmpHandCardData = Arrays.copyOf(cbHandCardData, cbHandCardCount);
		int tmpHandCardCount = cbHandCardCount;

		AnalyseIndexResult analyseIndexResult = new AnalyseIndexResult();
		store_card_data(tmpHandCardData, tmpHandCardCount, analyseIndexResult);

		if (card_type == GameConstants.TCDG_XTXZ_SINGLE || card_type == GameConstants.TCDG_XTXZ_DOUBLE || 
				card_type == GameConstants.TCDG_XTXZ_THREE|| card_type == GameConstants.TCDG_XTXZ_DOUBLE_LINK || 
				card_type == GameConstants.TCDG_XTXZ_PLANE || card_type == GameConstants.TCDG_XTXZ_3_DAI_2) {
			//首先先去掉王炸
			if((king_count == 2 && this.has_shuang_wang) || king_count >= 3){
				for(int i = 0;i < analyseIndexResult.card_index[13];i++){
					set.add(analyseIndexResult.card_data[13][i]);
				}
				for(int i = 0;i < analyseIndexResult.card_index[14];i++){
					set.add(analyseIndexResult.card_data[14][i]);
				}
			}
			
			// 单张、对子、三张
			// 第一轮减掉炸弹
			for (int i = 0; i < GameConstants.WSK_TC_MAX_INDEX - Constants_XTXZ.INDEX_FENCE + 1; i++) {
				int count = analyseIndexResult.card_index[i];
				int[] cards = analyseIndexResult.card_data[i];

				if (count + magic_count >= 4) {
					for (int j = 0; j < count; j++) {
						// 标记牌数据
						set.add(cards[j]);
					}
				}
			}

			// 第二轮减掉五十K
			int card_five_count = analyseIndexResult.card_index[2];
			int card_ten_count = analyseIndexResult.card_index[7];
			int card_thirteen_count = analyseIndexResult.card_index[10];

			int[] cards_five = analyseIndexResult.card_data[2];
			int[] cards_ten = analyseIndexResult.card_data[7];
			int[] cards_thirteen = analyseIndexResult.card_data[10];

			if (card_five_count > 0 && card_ten_count > 0 && card_thirteen_count > 0) {
				has_wsk = true;
			} else if (card_five_count > 0 && card_ten_count > 0 && magic_count > 0) {
				has_wsk = true;
			} else if (card_five_count > 0 && magic_count > 0 && card_thirteen_count > 0) {
				has_wsk = true;
			} else if (magic_count > 0 && card_ten_count > 0 && card_thirteen_count > 0) {
				has_wsk = true;
			}
			if (has_wsk) {
				for (int i = 0; i < card_five_count; i++) {
					// 标记牌数据
					set.add(cards_five[i]);
				}
				for (int i = 0; i < card_ten_count; i++) {
					// 标记牌数据
					set.add(cards_ten[i]);
				}
				for (int i = 0; i < card_thirteen_count; i++) {
					// 标记牌数据
					set.add(cards_thirteen[i]);
				}
			}

			if (card_type == GameConstants.TCDG_XTXZ_SINGLE) {
				// 第四轮减掉单张
				for (int i = 0; i < cbHandCardCount; i++) {
					if (cbHandCardData[i] <= 0) {
						continue;
					}

					int value_1 = get_card_logic_value(cbHandCardData[i]);
					int value_2 = get_card_logic_value(cbOutCardData[0]);
					if (value_1 > value_2) {
						// 标记牌数据
						set.add(cbHandCardData[i]);
					}
				}
			} else if (card_type == GameConstants.TCDG_XTXZ_DOUBLE) {
				// 第四轮减掉对子
				for (int i = 0; i < GameConstants.WSK_TC_MAX_INDEX - 1; i++) {
					int count = analyseIndexResult.card_index[i];
					int[] cards = analyseIndexResult.card_data[i];

					int value_1 = get_card_logic_value(cards[0]);
					int value_2 = get_card_logic_value(cbOutCardData[0]);

					if (count > 0 && count + magic_count > 1 && value_1 > value_2) {
						for (int j = 0; j < count; j++) {
							// 标记牌数据
							set.add(cards[j]);
						}
					}
				}
			} else if (card_type == GameConstants.TCDG_XTXZ_THREE) {
				// 第四轮减掉三张
				for (int i = 0; i < GameConstants.WSK_TC_MAX_INDEX - Constants_XTXZ.INDEX_FENCE + 1; i++) {
					int count = analyseIndexResult.card_index[i];
					int[] cards = analyseIndexResult.card_data[i];

					int value_1 = get_card_logic_value(cards[0]);
					int value_2 = get_card_logic_value(cbOutCardData[0]);

					if (count > 0 && count + magic_count > 2 && value_1 > value_2) {
						for (int j = 0; j < count; j++) {
							// 标记牌数据
							set.add(cards[j]);
						}
					}
				}
			} else if (card_type == GameConstants.TCDG_XTXZ_DOUBLE_LINK) {
				// 第四轮减掉连对
				int dui_zi_count = out_card_count / 2;

				int card_value_1 = get_card_logic_value(cbOutCardData[0]);
				int card_index_1 = switch_card_to_index(cbOutCardData[0]);

				int needed_magic_count = 0;

				for (int i = GameConstants.WSK_TC_MAX_INDEX - Constants_XTXZ.INDEX_FENCE - 1; i > card_index_1; i--) {
					int card_value_2 = switch_index_to_value(i);

					if (card_value_2 <= card_value_1)
						break;

					needed_magic_count = 0;
					boolean has_enough_card = true;
					for (int j = i; j > i - dui_zi_count; j--) {
						int count = analyseIndexResult.card_index[j];
						if (count < 1) {
							has_enough_card = false;
							break;
						}
						if (count == 1) {
							needed_magic_count++;
						}
					}

					if (has_enough_card && needed_magic_count <= magic_count) {
						for (int j = i; j > i - dui_zi_count; j--) {
							int count = analyseIndexResult.card_index[j];
							int[] cards = analyseIndexResult.card_data[j];

							for (int x = 0; x < count; x++) {
								// 标记牌数据
								set.add(cards[x]);
							}
						}
					}
				}
			}else if(card_type == GameConstants.TCDG_XTXZ_3_DAI_2) {
				// 第四轮减掉三张
				boolean have_san = false;
				for (int i = 0; i < GameConstants.WSK_TC_MAX_INDEX - Constants_XTXZ.INDEX_FENCE + 1; i++) {
					int count = analyseIndexResult.card_index[i];
					int[] cards = analyseIndexResult.card_data[i];

					int value_1 = get_card_logic_value(cards[0]);
					int value_2 = get_card_logic_value(cbOutCardData[0]);
					if (count > 0 && count + magic_count > 2 && value_1 > value_2) {
						have_san = true;
					}
				}
				if(have_san){
					for (int i = 0; i < cbHandCardCount; i++) {
						if (cbHandCardData[i] <= 0) {
							continue;
						}
						set.add(cbHandCardData[i]);
					}
				}
			}else if (card_type == GameConstants.TCDG_XTXZ_PLANE) {//飞机
				
				// 第四轮减掉飞机
				//int san_zhang_count = out_card_count / 3;
				int san_zhang_count = get_three_link_count(cbOutCardData,out_card_count);

				int card_value_1 = get_card_logic_value(cbOutCardData[0]);
				int card_index_1 = switch_card_to_index(cbOutCardData[0]);

				int needed_magic_count = 0;

				for (int i = GameConstants.WSK_TC_MAX_INDEX - Constants_XTXZ.INDEX_FENCE - 1; i > card_index_1; i--) {
					int card_value_2 = switch_index_to_value(i);

					if (card_value_2 <= card_value_1)
						break;

					needed_magic_count = 0;
					boolean has_enough_card = true;
					for (int j = i; j > i - san_zhang_count; j--) {
						int count = analyseIndexResult.card_index[j];
						if (count < 1) {
							has_enough_card = false;
							break;
						}
						if (count < 3) {
							needed_magic_count += count;
						}
					}

					if (has_enough_card && needed_magic_count <= magic_count) {
						for (int j = i; j > i - san_zhang_count; j--) {
							int count = analyseIndexResult.card_index[j];
							int[] cards = analyseIndexResult.card_data[j];

							for (int x = 0; x < count; x++) {
								// 标记牌数据
								set.add(cards[x]);
							}
						}
					}
				}
			}
		} else if (card_type == GameConstants.TCDG_XTXZ_510K_DC) {
			//首先先去掉王炸
			if((king_count == 2 && this.has_shuang_wang) || king_count >= 3){
				for(int i = 0;i < analyseIndexResult.card_index[13];i++){
					set.add(analyseIndexResult.card_data[13][i]);
				}
				for(int i = 0;i < analyseIndexResult.card_index[14];i++){
					set.add(analyseIndexResult.card_data[14][i]);
				}
			}
			// 杂五十K
			// 第一轮减掉炸弹
			for (int i = 0; i < GameConstants.WSK_TC_MAX_INDEX - Constants_XTXZ.INDEX_FENCE + 1; i++) {
				int count = analyseIndexResult.card_index[i];
				int[] cards = analyseIndexResult.card_data[i];

				if (count > 0 && count + magic_count >= 4) {
					for (int j = 0; j < count; j++) {
						// 标记牌数据
						set.add(cards[j]);
					}
				}
			}

			// 第二轮减掉正五十K
			int card_five_count = analyseIndexResult.card_index[2];
			int card_ten_count = analyseIndexResult.card_index[7];
			int card_thirteen_count = analyseIndexResult.card_index[10];

			int[] cards_five = analyseIndexResult.card_data[2];
			int[] cards_ten = analyseIndexResult.card_data[7];
			int[] cards_thirteen = analyseIndexResult.card_data[10];

			int space_five_count = 0;
			int space_ten_count = 0;
			int space_thirteen_count = 0;
			int heart_five_count = 0;
			int heart_ten_count = 0;
			int heart_thirteen_count = 0;
			int club_five_count = 0;
			int club_ten_count = 0;
			int club_thirteen_count = 0;
			int diamond_five_count = 0;
			int diamond_ten_count = 0;
			int diamond_thirteen_count = 0;

			int[] space_five_cards = new int[4];
			int[] space_ten_cards = new int[4];
			int[] space_thirteen_cards = new int[4];
			int[] heart_five_cards = new int[4];
			int[] heart_ten_cards = new int[4];
			int[] heart_thirteen_cards = new int[4];
			int[] club_five_cards = new int[4];
			int[] club_ten_cards = new int[4];
			int[] club_thirteen_cards = new int[4];
			int[] diamond_five_cards = new int[4];
			int[] diamond_ten_cards = new int[4];
			int[] diamond_thirteen_cards = new int[4];

			for (int i = 0; i < card_five_count; i++) {
				int color = get_card_color(cards_five[i]);
				if (color == 0x00) {
					space_five_cards[space_five_count++] = cards_five[i];
				}
				if (color == 0x10) {
					heart_five_cards[heart_five_count++] = cards_five[i];
				}
				if (color == 0x20) {
					club_five_cards[club_five_count++] = cards_five[i];
				}
				if (color == 0x30) {
					diamond_five_cards[diamond_five_count++] = cards_five[i];
				}
			}

			for (int i = 0; i < card_ten_count; i++) {
				int color = get_card_color(cards_ten[i]);
				if (color == 0x00) {
					space_ten_cards[space_ten_count++] = cards_ten[i];
				}
				if (color == 0x10) {
					heart_ten_cards[heart_ten_count++] = cards_ten[i];
				}
				if (color == 0x20) {
					club_ten_cards[club_ten_count++] = cards_ten[i];
				}
				if (color == 0x30) {
					diamond_ten_cards[diamond_ten_count++] = cards_ten[i];
				}
			}

			for (int i = 0; i < card_thirteen_count; i++) {
				int color = get_card_color(cards_thirteen[i]);
				if (color == 0x00) {
					space_thirteen_cards[space_thirteen_count++] = cards_thirteen[i];
				}
				if (color == 0x10) {
					heart_thirteen_cards[heart_thirteen_count++] = cards_thirteen[i];
				}
				if (color == 0x20) {
					club_thirteen_cards[club_thirteen_count++] = cards_thirteen[i];
				}
				if (color == 0x30) {
					diamond_thirteen_cards[diamond_thirteen_count++] = cards_thirteen[i];
				}
			}

			if ((space_five_count > 0 && space_ten_count > 0 && space_thirteen_count > 0)
					|| (magic_count > 0 && space_ten_count > 0 && space_thirteen_count > 0)
					|| (space_five_count > 0 && magic_count > 0 && space_thirteen_count > 0)
					|| (space_five_count > 0 && space_ten_count > 0 && magic_count > 0)) {
				for (int i = 0; i < space_five_count; i++) {
					// 标记牌数据
					set.add(space_five_cards[i]);
				}
				for (int i = 0; i < space_ten_count; i++) {
					// 标记牌数据
					set.add(space_ten_cards[i]);
				}
				for (int i = 0; i < space_thirteen_count; i++) {
					// 标记牌数据
					set.add(space_thirteen_cards[i]);
				}
			}

			if ((heart_five_count > 0 && heart_ten_count > 0 && heart_thirteen_count > 0)
					|| (magic_count > 0 && heart_ten_count > 0 && heart_thirteen_count > 0)
					|| (heart_five_count > 0 && magic_count > 0 && heart_thirteen_count > 0)
					|| (heart_five_count > 0 && heart_ten_count > 0 && magic_count > 0)) {
				for (int i = 0; i < heart_five_count; i++) {
					// 标记牌数据
					set.add(heart_five_cards[i]);
				}
				for (int i = 0; i < heart_ten_count; i++) {
					// 标记牌数据
					set.add(heart_ten_cards[i]);
				}
				for (int i = 0; i < heart_thirteen_count; i++) {
					// 标记牌数据
					set.add(heart_thirteen_cards[i]);
				}
			}

			if ((club_five_count > 0 && club_ten_count > 0 && club_thirteen_count > 0)
					|| (magic_count > 0 && club_ten_count > 0 && club_thirteen_count > 0)
					|| (club_five_count > 0 && magic_count > 0 && club_thirteen_count > 0)
					|| (club_five_count > 0 && club_ten_count > 0 && magic_count > 0)) {
				for (int i = 0; i < club_five_count; i++) {
					// 标记牌数据
					set.add(club_five_cards[i]);
				}
				for (int i = 0; i < club_ten_count; i++) {
					// 标记牌数据
					set.add(club_ten_cards[i]);
				}
				for (int i = 0; i < club_thirteen_count; i++) {
					// 标记牌数据
					set.add(club_thirteen_cards[i]);
				}
			}

			if ((diamond_five_count > 0 && diamond_ten_count > 0 && diamond_thirteen_count > 0)
					|| (magic_count > 0 && diamond_ten_count > 0 && diamond_thirteen_count > 0)
					|| (diamond_five_count > 0 && magic_count > 0 && diamond_thirteen_count > 0)
					|| (diamond_five_count > 0 && diamond_ten_count > 0 && magic_count > 0)) {
				for (int i = 0; i < diamond_five_count; i++) {
					// 标记牌数据
					set.add(diamond_five_cards[i]);
				}
				for (int i = 0; i < diamond_ten_count; i++) {
					// 标记牌数据
					set.add(diamond_ten_cards[i]);
				}
				for (int i = 0; i < diamond_thirteen_count; i++) {
					// 标记牌数据
					set.add(diamond_thirteen_cards[i]);
				}
			}
		} else if (card_type == GameConstants.TCDG_XTXZ_510K_SC) {
			//首先先去掉王炸
			if((king_count == 2 && this.has_shuang_wang) || king_count >= 3){
				for(int i = 0;i < analyseIndexResult.card_index[13];i++){
					set.add(analyseIndexResult.card_data[13][i]);
				}
				for(int i = 0;i < analyseIndexResult.card_index[14];i++){
					set.add(analyseIndexResult.card_data[14][i]);
				}
			}
			// 正五十K
			// 第一轮减掉炸弹
			for (int i = 0; i < GameConstants.WSK_TC_MAX_INDEX - Constants_XTXZ.INDEX_FENCE + 1; i++) {
				int count = analyseIndexResult.card_index[i];
				int[] cards = analyseIndexResult.card_data[i];

				if (count > 0 && count + magic_count >= 4) {
					for (int j = 0; j < count; j++) {
						// 标记牌数据
						set.add(cards[j]);
					}
				}
			}
		} else if (card_type == GameConstants.TCDG_XTXZ_BOMB) {
			
			if(out_card_count == 4){
				if(king_count == 3){
					for(int i = 0;i < analyseIndexResult.card_index[13];i++){
						set.add(analyseIndexResult.card_data[13][i]);
					}
					for(int i = 0;i < analyseIndexResult.card_index[14];i++){
						set.add(analyseIndexResult.card_data[14][i]);
					}
				}
			}else{
				//首先先去掉王炸
				if((king_count == 2 && this.has_shuang_wang) || king_count > 3){
					for(int i = 0;i < analyseIndexResult.card_index[13];i++){
						set.add(analyseIndexResult.card_data[13][i]);
					}
					for(int i = 0;i < analyseIndexResult.card_index[14];i++){
						set.add(analyseIndexResult.card_data[14][i]);
					}
				}
			}

			// 炸弹
			// 第一轮减掉大于上家炸弹的炸弹
			for (int i = 0; i < GameConstants.WSK_TC_MAX_INDEX - Constants_XTXZ.INDEX_FENCE + 1; i++) {
				int count = analyseIndexResult.card_index[i];
				int[] cards = analyseIndexResult.card_data[i];

				if (count > 0 && count + magic_count > out_card_count) {
					for (int j = 0; j < count; j++) {
						// 标记牌数据
						set.add(cards[j]);
					}
				} else if (count > 0 && count + magic_count == out_card_count) {
					int value_1 = get_card_logic_value(cards[0]);
					int value_2 = get_card_logic_value(cbOutCardData[0]);

					if (value_1 > value_2) {
						for (int j = 0; j < count; j++) {
							// 标记牌数据
							set.add(cards[j]);
						}
					}
				}
			}
		}else if(out_card_count == 3 && card_type == GameConstants.TCDG_XTXZ_KING){//三王炸
			// 炸弹
			// 第一轮减掉大于上家炸弹的炸弹
			for (int i = 0; i < GameConstants.WSK_TC_MAX_INDEX - Constants_XTXZ.INDEX_FENCE + 1; i++) {
				int count = analyseIndexResult.card_index[i];
				int[] cards = analyseIndexResult.card_data[i];
				if (count > 0 && count + magic_count > 4) {
					for (int j = 0; j < count; j++) {
						// 标记牌数据
						set.add(cards[j]);
					}
				} 
			}
		}

		// 最后一轮将标记了的牌数据，转换成数组
		for (int i = 0; i < cbHandCardCount; i++) {
			if (set.contains(cbHandCardData[i])) {
				// 如果牌已经被标记
				can_out_cards[can_out_card_count++] = cbHandCardData[i];
			}
		}

		return can_out_card_count;
	}

	public boolean is_magic_or_flower(int cardData) {
		return cardData == Constants_XTXZ.CARD_SMALL_MAGIC || cardData == Constants_XTXZ.CARD_BIG_MAGIC
				|| cardData == Constants_XTXZ.FLOWER_CARD;
	}
	
}
