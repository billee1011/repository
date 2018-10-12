package com.cai.game.mj;

import com.cai.common.constant.GameConstants;

public class FengKanUtil {
	private static final int MAX_ITEMS_COUNT = 7 + 4 * (4 + 1);
	private static final int KIND_CARD_COUNT = 3;
	private static final int KAN = 1; // 坎
	private static final int DNX = 2; // 东南西
	private static final int DNB = 3; // 东南北
	private static final int DXB = 4; // 东西北
	private static final int NXB = 5; // 南西北
	private static final int ZFB = 6; // 中发白

	/**
	 * 无王牌时，获取风坎（三张不同的风牌）的数目
	 * 
	 * @param cards_index
	 *            手牌
	 * @param feng_kan
	 *            数组大小为2，第一个值存东南西北的风坎，第二个值存中发白的风坎
	 * @param has_dong_feng_ling
	 *            是否有东风令
	 */
	public static void getFengKanCount(int[] cards_index, int[] feng_kan, boolean has_dong_feng_ling) {
		if (cards_index == null || feng_kan == null || feng_kan.length != 2)
			return;

		int feng_card_count = get_feng_card_count(cards_index);
		if (feng_card_count == 0 || feng_card_count == 2 || feng_card_count % 3 == 1)
			return;

		boolean need_eye = false;
		if (feng_card_count % KIND_CARD_COUNT == 2)
			need_eye = true;

		int less_items_count = (feng_card_count - 2) / KIND_CARD_COUNT;
		if (!need_eye)
			less_items_count = feng_card_count / KIND_CARD_COUNT;

		int dong = GameConstants.MAX_ZI;
		int nan = GameConstants.MAX_ZI + 1;
		int xi = GameConstants.MAX_ZI + 2;
		int bei = GameConstants.MAX_ZI + 3;
		int zhong = GameConstants.MAX_ZI + 4;
		int fa = GameConstants.MAX_ZI + 5;
		int bai = GameConstants.MAX_ZI + 6;

		if (less_items_count == 0) { // 风牌数目为3
			int[] tmp_cards_index = new int[GameConstants.MAX_INDEX];
			for (int i = GameConstants.MAX_ZI; i < GameConstants.MAX_ZI_FENG; i++) {
				tmp_cards_index[i] = cards_index[i];
			}

			if (feng_kan[0] == 0)
				feng_kan[0] = locate_feng_kan(tmp_cards_index, dong, nan, xi, need_eye); // 东南西
			if (feng_kan[0] == 0)
				feng_kan[0] = locate_feng_kan(tmp_cards_index, dong, nan, bei, need_eye); // 东南北
			if (feng_kan[0] == 0)
				feng_kan[0] = locate_feng_kan(tmp_cards_index, dong, xi, bei, need_eye); // 东西北
			if (feng_kan[0] == 0 && !has_dong_feng_ling)
				feng_kan[0] = locate_feng_kan(tmp_cards_index, nan, xi, bei, need_eye); // 南西北
			if (feng_kan[0] == 0)
				feng_kan[1] = locate_feng_kan(tmp_cards_index, zhong, fa, bai, need_eye); // 中发白

			return;
		}

		Item[] items = new Item[MAX_ITEMS_COUNT];
		for (int i = 0; i < MAX_ITEMS_COUNT; i++) {
			items[i] = new Item();
		}
		int items_count = 0;

		if (feng_card_count >= 3) { // 风牌数目为3*n或3*n+2 (其中n>=1 && n<=4)
			for (int i = GameConstants.MAX_ZI; i < GameConstants.MAX_ZI_FENG; i++) {
				if (cards_index[i] >= KIND_CARD_COUNT) {
					items[items_count].kind = KAN;
					for (int x = 0; x < KIND_CARD_COUNT; x++) {
						items[items_count].card_index[x] = i;
					}
					items_count++;
				}
				if (i == GameConstants.MAX_ZI)
					items_count = combine_cards(cards_index, dong, nan, xi, items, items_count, DNX); // 东南西
				if (i == GameConstants.MAX_ZI + 1)
					items_count = combine_cards(cards_index, dong, nan, bei, items, items_count, DNB); // 东南北
				if (i == GameConstants.MAX_ZI + 2)
					items_count = combine_cards(cards_index, dong, xi, bei, items, items_count, DXB); // 东西北
				if (i == GameConstants.MAX_ZI + 3)
					if (!has_dong_feng_ling)
						items_count = combine_cards(cards_index, nan, xi, bei, items, items_count, NXB); // 南西北
				if (i == GameConstants.MAX_ZI + 4)
					items_count = combine_cards(cards_index, zhong, fa, bai, items, items_count, ZFB); // 中发白
			}
		}

		if (items_count >= less_items_count) {
			int[] tmp_cards_index = new int[GameConstants.MAX_INDEX];

			int[] iterator_index = new int[] { 0, 1, 2, 3 };
			Item[] tmp_items = new Item[4];
			for (int i = 0; i < 4; i++)
				tmp_items[i] = new Item();

			int count = 0;

			do {
				count++;

				for (int i = GameConstants.MAX_ZI; i < GameConstants.MAX_ZI_FENG; i++)
					tmp_cards_index[i] = cards_index[i];

				for (int i = 0; i < less_items_count; i++) {
					tmp_items[i].kind = items[iterator_index[i]].kind;
					for (int x = 0; x < KIND_CARD_COUNT; x++) {
						tmp_items[i].card_index[x] = items[iterator_index[i]].card_index[x];
					}
				}

				boolean enough_card = true;

				for (int i = 0; i < less_items_count * KIND_CARD_COUNT; i++) {
					int card_index = tmp_items[i / 3].card_index[i % 3];
					if (tmp_cards_index[card_index] == 0) {
						enough_card = false;
						break;
					} else {
						tmp_cards_index[card_index]--;
					}
				}

				if (enough_card) {
					int card_eye = 0;

					if (need_eye) {
						for (int i = GameConstants.MAX_ZI; i < GameConstants.MAX_ZI_FENG; i++) {
							if (tmp_cards_index[i] == 2) {
								card_eye = 1;
								break;
							}
						}
					} else {
						card_eye = 1;
					}

					if (card_eye != 0) {
						int[] tmp_feng_kan = new int[2];
						for (int i = 0; i < less_items_count; i++) {
							if (tmp_items[i].kind == ZFB) {
								tmp_feng_kan[1]++;
							} else if (tmp_items[i].kind == DNX || tmp_items[i].kind == DNB || tmp_items[i].kind == DXB || tmp_items[i].kind == NXB) {
								tmp_feng_kan[0]++;
							}
						}
						if (tmp_feng_kan[0] + tmp_feng_kan[1] > feng_kan[0] + feng_kan[1]) {
							feng_kan[0] = tmp_feng_kan[0];
							feng_kan[1] = tmp_feng_kan[1];
						}
					}
				}

				if (iterator_index[less_items_count - 1] == (items_count - 1)) {
					int i = less_items_count - 1;

					for (; i > 0; i--) {
						if ((iterator_index[i - 1] + 1) != iterator_index[i]) {
							int iterator_count = iterator_index[i - 1];

							for (int j = i - 1; j < less_items_count; j++) {
								iterator_index[j] = iterator_count + j - i + 2;
							}

							break;
						}
					}

					if (i == 0)
						break;

				} else {
					iterator_index[less_items_count - 1]++;
				}

			} while (true && count <= 1000);
		}

		return;
	}

	private static int get_feng_card_count(int[] cards_index) {
		int card_count = 0;
		for (int i = GameConstants.MAX_ZI; i < GameConstants.MAX_ZI_FENG; i++)
			card_count += cards_index[i];

		return card_count;
	}

	private static int switch_to_feng_cards_data(int[] cards_index, int[] cards_data) {
		int count = 0;
		for (int i = GameConstants.MAX_ZI; i < GameConstants.MAX_ZI_FENG; i++) {
			if (cards_index[i] > 0) {
				for (int j = 0; j < cards_index[i]; j++) {
					cards_data[count++] = switch_to_card_data(i);
				}
			}
		}

		return count;
	}

	private static int switch_to_card_data(int card_index) {
		if (card_index >= GameConstants.MAX_INDEX) {
			return GameConstants.MAX_INDEX;
		}
		return ((card_index / 9) << 4) | (card_index % 9 + 1);
	}

	private static int locate_feng_kan(int[] cards_index, int index_a, int index_b, int index_c, boolean need_eye) {
		int result = 0;
		if (cards_index[index_a] >= 1 && cards_index[index_b] >= 1 && cards_index[index_c] >= 1) {
			cards_index[index_a] -= 1;
			cards_index[index_b] -= 1;
			cards_index[index_c] -= 1;

			if (need_eye) {
				int[] hand_cards = new int[GameConstants.MAX_COUNT];
				switch_to_feng_cards_data(cards_index, hand_cards);
				if (hand_cards[0] == hand_cards[1]) {
					result = 1;
				}
			} else {
				result = 1;
			}

			if (result == 0) {
				cards_index[index_a] += 1;
				cards_index[index_b] += 1;
				cards_index[index_c] += 1;
			}
		}

		return result;
	}

	private static int combine_cards(int[] cards_index, int index_a, int index_b, int index_c, Item[] items, int items_count, int type) {
		int count = items_count;
		int tmp_cards[] = { cards_index[index_a], cards_index[index_b], cards_index[index_c] };

		int itr = 0;

		while (tmp_cards[0] + tmp_cards[1] + tmp_cards[2] >= KIND_CARD_COUNT && itr <= 12) {
			itr++;

			boolean can_combine = true;
			for (int x = 0; x < KIND_CARD_COUNT; x++) {
				if (tmp_cards[x] > 0) {
					tmp_cards[x]--;
				} else {
					can_combine = false;
					break;
				}
			}

			if (can_combine) {
				items[count].kind = type;
				items[count].card_index[0] = index_a;
				items[count].card_index[1] = index_b;
				items[count].card_index[2] = index_c;
				count++;
			} else
				break;
		}
		return count;
	}

	static class Item {
		public int kind;
		public int[] card_index = new int[KIND_CARD_COUNT];

		public Item() {
		}
	}
}
