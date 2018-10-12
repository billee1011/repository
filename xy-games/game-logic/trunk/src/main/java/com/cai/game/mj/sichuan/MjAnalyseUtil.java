package com.cai.game.mj.sichuan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.Pair;

public class MjAnalyseUtil {
	static final int MAGIC = 0xFF;
	private static final Map<Long, Pair<Integer, List<Integer>>> keyToPairMap = new HashMap<>();
	private final WeaveItem[] weaveItems;
	private final int weaveCount;
	private int addedValue;
	private boolean hasDuiDuiHu = false;
	private boolean hasQingYiSe = false;
	private boolean hasZhongZhang = false;
	private boolean hasYaoJiu = false;
	private boolean hasJiangDui = false;
	private List<Integer> magicToRealCard = new ArrayList<>();
	private boolean hasFoundBestSolution = false;
	private Solution bestSolution = new Solution();
	public static final int maxIndex = GameConstants.MAX_ZI_FENG + 1;
	private static boolean initialized = false;

	public MjAnalyseUtil(int weaveCount, WeaveItem[] weaveItems) {
		this.weaveCount = weaveCount;
		this.weaveItems = weaveItems;

		if (initialized == false) {
			initialized = true;

			addKanKeyPair();
			addDuiZiKeyPair();
			addShunZiKeyPair();
		}
	}

	private long getWeaveKey(int card1, int card2, int card3) {
		long key = (long) card1 + (long) (card2 << 8) + (card3 << 16);
		if (addedValue > 0)
			key += addedValue;
		return key;
	}

	private void addWeaveKeyPair(long key, Pair<Integer, List<Integer>> pair) {
		if (!keyToPairMap.containsKey(key)) {
			keyToPairMap.put(key, pair);
		} else {
			System.err.printf("Duplicated key: %d \n", key);
		}
	}

	private void addKanKeyPair() {
		for (int card = 1; card <= 9; card++) {
			addKanKeyPair(card);
		}

		for (int card = 1 + 0x10; card <= 9 + 0x10; card++) {
			addKanKeyPair(card);
		}

		for (int card = 1 + 0x20; card <= 9 + 0x20; card++) {
			addKanKeyPair(card);
		}

		for (int card = 1 + 0x30; card <= 7 + 0x30; card++) {
			addKanKeyPair(card);
		}

		addWeaveKeyPair(getWeaveKey(MAGIC, MAGIC, MAGIC), Pair.of(GameConstants.WIK_PENG, Arrays.asList(MAGIC, MAGIC, MAGIC)));
	}

	private void addKanKeyPair(int card) {
		addWeaveKeyPair(getWeaveKey(card, card, card), Pair.of(GameConstants.WIK_PENG, Arrays.asList()));

		addWeaveKeyPair(getWeaveKey(MAGIC, card, card), Pair.of(GameConstants.WIK_PENG, Arrays.asList(card)));
		addWeaveKeyPair(getWeaveKey(card, MAGIC, card), Pair.of(GameConstants.WIK_PENG, Arrays.asList(card)));
		addWeaveKeyPair(getWeaveKey(card, card, MAGIC), Pair.of(GameConstants.WIK_PENG, Arrays.asList(card)));

		addWeaveKeyPair(getWeaveKey(MAGIC, MAGIC, card), Pair.of(GameConstants.WIK_PENG, Arrays.asList(card, card)));
		addWeaveKeyPair(getWeaveKey(card, MAGIC, MAGIC), Pair.of(GameConstants.WIK_PENG, Arrays.asList(card, card)));
		addWeaveKeyPair(getWeaveKey(MAGIC, card, MAGIC), Pair.of(GameConstants.WIK_PENG, Arrays.asList(card, card)));
	}

	private void addDuiZiKeyPair() {
		for (int card = 1; card <= 9; card++) {
			addDuiZiKeyPair(card);
		}

		for (int card = 1 + 0x10; card <= 9 + 0x10; card++) {
			addDuiZiKeyPair(card);
		}

		for (int card = 1 + 0x20; card <= 9 + 0x20; card++) {
			addDuiZiKeyPair(card);
		}

		for (int card = 1 + 0x30; card <= 9 + 0x30; card++) {
			addDuiZiKeyPair(card);
		}

		addWeaveKeyPair(getWeaveKey(MAGIC, MAGIC, 0), Pair.of(GameConstants.WIK_DUI_ZI, Arrays.asList(MAGIC, MAGIC)));
	}

	private void addDuiZiKeyPair(int card) {
		addWeaveKeyPair(getWeaveKey(card, card, 0), Pair.of(GameConstants.WIK_DUI_ZI, Arrays.asList()));

		addWeaveKeyPair(getWeaveKey(MAGIC, card, 0), Pair.of(GameConstants.WIK_DUI_ZI, Arrays.asList(card)));
		addWeaveKeyPair(getWeaveKey(card, MAGIC, 0), Pair.of(GameConstants.WIK_DUI_ZI, Arrays.asList(card)));
	}

	private void addShunZiKeyPair() {
		for (int card = 1; card <= 7; card++) {
			addShunZiKeyPair(card);
		}

		for (int card = 1 + 0x10; card <= 7 + 0x10; card++) {
			addShunZiKeyPair(card);
		}

		for (int card = 1 + 0x20; card <= 7 + 0x20; card++) {
			addShunZiKeyPair(card);
		}

		for (int card = 1; card <= 9; card++) {
			addShunZiKeyPair_(card);
		}

		for (int card = 1 + 0x10; card <= 9 + 0x10; card++) {
			addShunZiKeyPair_(card);
		}

		for (int card = 1 + 0x20; card <= 9 + 0x20; card++) {
			addShunZiKeyPair_(card);
		}
	}

	private void addShunZiKeyPair(int card) {
		addWeaveKeyPair(getWeaveKey(card, card + 1, card + 2), Pair.of(GameConstants.WIK_LEFT, Arrays.asList()));
		addWeaveKeyPair(getWeaveKey(card, card + 2, card + 1), Pair.of(GameConstants.WIK_LEFT, Arrays.asList()));
		addWeaveKeyPair(getWeaveKey(card + 1, card, card + 2), Pair.of(GameConstants.WIK_LEFT, Arrays.asList()));
		addWeaveKeyPair(getWeaveKey(card + 1, card + 2, card), Pair.of(GameConstants.WIK_LEFT, Arrays.asList()));
		addWeaveKeyPair(getWeaveKey(card + 2, card, card + 1), Pair.of(GameConstants.WIK_LEFT, Arrays.asList()));
		addWeaveKeyPair(getWeaveKey(card + 2, card + 1, card), Pair.of(GameConstants.WIK_LEFT, Arrays.asList()));

		addWeaveKeyPair(getWeaveKey(MAGIC, card + 1, card + 2), Pair.of(GameConstants.WIK_LEFT, Arrays.asList(card)));
		addWeaveKeyPair(getWeaveKey(MAGIC, card + 2, card + 1), Pair.of(GameConstants.WIK_LEFT, Arrays.asList(card)));
		addWeaveKeyPair(getWeaveKey(card + 1, card + 2, MAGIC), Pair.of(GameConstants.WIK_LEFT, Arrays.asList(card)));
		addWeaveKeyPair(getWeaveKey(card + 1, MAGIC, card + 2), Pair.of(GameConstants.WIK_LEFT, Arrays.asList(card)));
		addWeaveKeyPair(getWeaveKey(card + 2, card + 1, MAGIC), Pair.of(GameConstants.WIK_LEFT, Arrays.asList(card)));
		addWeaveKeyPair(getWeaveKey(card + 2, MAGIC, card + 1), Pair.of(GameConstants.WIK_LEFT, Arrays.asList(card)));

		addWeaveKeyPair(getWeaveKey(card, MAGIC, card + 2), Pair.of(GameConstants.WIK_LEFT, Arrays.asList(card + 1)));
		addWeaveKeyPair(getWeaveKey(card, card + 2, MAGIC), Pair.of(GameConstants.WIK_LEFT, Arrays.asList(card + 1)));
		addWeaveKeyPair(getWeaveKey(card + 2, card, MAGIC), Pair.of(GameConstants.WIK_LEFT, Arrays.asList(card + 1)));
		addWeaveKeyPair(getWeaveKey(card + 2, MAGIC, card), Pair.of(GameConstants.WIK_LEFT, Arrays.asList(card + 1)));
		addWeaveKeyPair(getWeaveKey(MAGIC, card, card + 2), Pair.of(GameConstants.WIK_LEFT, Arrays.asList(card + 1)));
		addWeaveKeyPair(getWeaveKey(MAGIC, card + 2, card), Pair.of(GameConstants.WIK_LEFT, Arrays.asList(card + 1)));

		addedValue = 101;
		addWeaveKeyPair(getWeaveKey(card, card + 1, MAGIC), Pair.of(GameConstants.WIK_LEFT, Arrays.asList(card + 2)));
		addWeaveKeyPair(getWeaveKey(card, MAGIC, card + 1), Pair.of(GameConstants.WIK_LEFT, Arrays.asList(card + 2)));
		addWeaveKeyPair(getWeaveKey(card + 1, MAGIC, card), Pair.of(GameConstants.WIK_LEFT, Arrays.asList(card + 2)));
		addWeaveKeyPair(getWeaveKey(card + 1, card, MAGIC), Pair.of(GameConstants.WIK_LEFT, Arrays.asList(card + 2)));
		addWeaveKeyPair(getWeaveKey(MAGIC, card + 1, card), Pair.of(GameConstants.WIK_LEFT, Arrays.asList(card + 2)));
		addWeaveKeyPair(getWeaveKey(MAGIC, card, card + 1), Pair.of(GameConstants.WIK_LEFT, Arrays.asList(card + 2)));
		addedValue = 0;
	}

	private void addShunZiKeyPair_(int card) {
		if (card == 1 || card == 1 + 0x10 || card == 1 + 0x20) {
			addedValue = 202;
			addWeaveKeyPair(getWeaveKey(card, MAGIC, MAGIC), Pair.of(GameConstants.WIK_LEFT, Arrays.asList(card + 1, card + 2)));
			addWeaveKeyPair(getWeaveKey(MAGIC, card, MAGIC), Pair.of(GameConstants.WIK_LEFT, Arrays.asList(card + 1, card + 2)));
			addWeaveKeyPair(getWeaveKey(MAGIC, MAGIC, card), Pair.of(GameConstants.WIK_LEFT, Arrays.asList(card + 1, card + 2)));
			addedValue = 0;
		} else if (card == 2 || card == 2 + 0x10 || card == 2 + 0x20) {
			addedValue = 202;
			addWeaveKeyPair(getWeaveKey(card, MAGIC, MAGIC), Pair.of(GameConstants.WIK_LEFT, Arrays.asList(card - 1, card + 1)));
			addWeaveKeyPair(getWeaveKey(MAGIC, card, MAGIC), Pair.of(GameConstants.WIK_LEFT, Arrays.asList(card - 1, card + 1)));
			addWeaveKeyPair(getWeaveKey(MAGIC, MAGIC, card), Pair.of(GameConstants.WIK_LEFT, Arrays.asList(card - 1, card + 1)));
			addedValue = 0;

			addedValue = 303;
			addWeaveKeyPair(getWeaveKey(card, MAGIC, MAGIC), Pair.of(GameConstants.WIK_LEFT, Arrays.asList(card + 1, card + 2)));
			addWeaveKeyPair(getWeaveKey(MAGIC, card, MAGIC), Pair.of(GameConstants.WIK_LEFT, Arrays.asList(card + 1, card + 2)));
			addWeaveKeyPair(getWeaveKey(MAGIC, MAGIC, card), Pair.of(GameConstants.WIK_LEFT, Arrays.asList(card + 1, card + 2)));
			addedValue = 0;
		} else if (card == 8 || card == 8 + 0x10 || card == 8 + 0x20) {
			addedValue = 202;
			addWeaveKeyPair(getWeaveKey(card, MAGIC, MAGIC), Pair.of(GameConstants.WIK_LEFT, Arrays.asList(card - 1, card + 1)));
			addWeaveKeyPair(getWeaveKey(MAGIC, card, MAGIC), Pair.of(GameConstants.WIK_LEFT, Arrays.asList(card - 1, card + 1)));
			addWeaveKeyPair(getWeaveKey(MAGIC, MAGIC, card), Pair.of(GameConstants.WIK_LEFT, Arrays.asList(card - 1, card + 1)));
			addedValue = 0;

			addedValue = 303;
			addWeaveKeyPair(getWeaveKey(card, MAGIC, MAGIC), Pair.of(GameConstants.WIK_LEFT, Arrays.asList(card - 1, card - 2)));
			addWeaveKeyPair(getWeaveKey(MAGIC, card, MAGIC), Pair.of(GameConstants.WIK_LEFT, Arrays.asList(card - 1, card - 2)));
			addWeaveKeyPair(getWeaveKey(MAGIC, MAGIC, card), Pair.of(GameConstants.WIK_LEFT, Arrays.asList(card - 1, card - 2)));
			addedValue = 0;
		} else if (card == 9 || card == 9 + 0x10 || card == 9 + 0x20) {
			addedValue = 202;
			addWeaveKeyPair(getWeaveKey(card, MAGIC, MAGIC), Pair.of(GameConstants.WIK_LEFT, Arrays.asList(card - 1, card - 2)));
			addWeaveKeyPair(getWeaveKey(MAGIC, card, MAGIC), Pair.of(GameConstants.WIK_LEFT, Arrays.asList(card - 1, card - 2)));
			addWeaveKeyPair(getWeaveKey(MAGIC, MAGIC, card), Pair.of(GameConstants.WIK_LEFT, Arrays.asList(card - 1, card - 2)));
			addedValue = 0;
		} else {
			addedValue = 202;
			addWeaveKeyPair(getWeaveKey(card, MAGIC, MAGIC), Pair.of(GameConstants.WIK_LEFT, Arrays.asList(card - 1, card - 2)));
			addWeaveKeyPair(getWeaveKey(MAGIC, card, MAGIC), Pair.of(GameConstants.WIK_LEFT, Arrays.asList(card - 1, card - 2)));
			addWeaveKeyPair(getWeaveKey(MAGIC, MAGIC, card), Pair.of(GameConstants.WIK_LEFT, Arrays.asList(card - 1, card - 2)));
			addedValue = 0;

			addedValue = 303;
			addWeaveKeyPair(getWeaveKey(card, MAGIC, MAGIC), Pair.of(GameConstants.WIK_LEFT, Arrays.asList(card - 1, card + 1)));
			addWeaveKeyPair(getWeaveKey(MAGIC, card, MAGIC), Pair.of(GameConstants.WIK_LEFT, Arrays.asList(card - 1, card + 1)));
			addWeaveKeyPair(getWeaveKey(MAGIC, MAGIC, card), Pair.of(GameConstants.WIK_LEFT, Arrays.asList(card - 1, card + 1)));
			addedValue = 0;

			addedValue = 404;
			addWeaveKeyPair(getWeaveKey(card, MAGIC, MAGIC), Pair.of(GameConstants.WIK_LEFT, Arrays.asList(card + 1, card + 2)));
			addWeaveKeyPair(getWeaveKey(MAGIC, card, MAGIC), Pair.of(GameConstants.WIK_LEFT, Arrays.asList(card + 1, card + 2)));
			addWeaveKeyPair(getWeaveKey(MAGIC, MAGIC, card), Pair.of(GameConstants.WIK_LEFT, Arrays.asList(card + 1, card + 2)));
			addedValue = 0;
		}
	}

	private Pair<Integer, List<Integer>> getWeaveKeyPair(long key) {
		if (keyToPairMap.containsKey(key))
			return keyToPairMap.get(key);

		return Pair.of(-1, Arrays.asList());
	}

	private Pair<Integer, List<Integer>> getWeaveKeyPair(int card1, int card2, int card3) {
		long key = getWeaveKey(card1, card2, card3);
		return getWeaveKeyPair(key);
	}

	private void dealLuoDiPai(List<Integer> magicToRealCard, int[] handCardCounter) {
		for (int i = 0; i < weaveCount; i++) {
			int card = weaveItems[i].center_card;
			int index = HandCardInfo.getCardIndex(card);
			int kind = weaveItems[i].weave_kind;
			if (kind == GameConstants.WIK_PENG) {
				handCardCounter[index] += 3;
			}
			if (kind == GameConstants.WIK_GANG) {
				handCardCounter[index] += 4;
			}
			if (kind == GameConstants.WIK_SUO_PENG_1) {
				handCardCounter[index] += 3;
				magicToRealCard.add(card);
			}
			if (kind == GameConstants.WIK_SUO_PENG_2) {
				handCardCounter[index] += 3;
				magicToRealCard.add(card);
				magicToRealCard.add(card);
			}
			if (kind == GameConstants.WIK_SUO_GANG_1) {
				handCardCounter[index] += 4;
				magicToRealCard.add(card);
			}
			if (kind == GameConstants.WIK_SUO_GANG_2) {
				handCardCounter[index] += 4;
				magicToRealCard.add(card);
				magicToRealCard.add(card);
			}
			if (kind == GameConstants.WIK_SUO_GANG_3) {
				handCardCounter[index] += 4;
				magicToRealCard.add(card);
				magicToRealCard.add(card);
				magicToRealCard.add(card);
			}
		}
	}

	private int getGengCount(final Solution solution, final HandCardInfo handCardInfo) {
		solution.magicToRealCard.clear();

		int gengCount = 0;

		int[] handCardCounter = new int[maxIndex];

		List<Integer> magicToRealCard = new ArrayList<>();
		for (WeaveInfo weaveInfo : solution.weaveInfoList) {
			if (weaveInfo.pair.getSecond().size() != 0) {
				for (int card : weaveInfo.pair.getSecond()) {
					magicToRealCard.add(card);
					int index = HandCardInfo.getCardIndex(card);
					handCardCounter[index]++;
				}
			}
		}

		for (int card : handCardInfo.handCards) {
			if (card == MAGIC)
				continue;

			int index = HandCardInfo.getCardIndex(card);
			handCardCounter[index]++;
		}

		dealLuoDiPai(magicToRealCard, handCardCounter);

		int magicToMagicCount = 0;
		for (int card : magicToRealCard) {
			if (card == MAGIC) {
				magicToMagicCount++;
			}
		}

		for (int i = 0; i < maxIndex - 1; i++) {
			if (handCardCounter[i] >= 4)
				gengCount++;
		}

		if (magicToMagicCount > 0) {
			if (hasJiangDui || hasDuiDuiHu) {
				for (int i = 0; i < maxIndex - 1; i++) {
					if (handCardCounter[i] < 4 && handCardCounter[i] + magicToMagicCount >= 4) {
						gengCount++;

						int card = HandCardInfo.getCardByIndex(i);
						for (int j = 0; j < magicToMagicCount; j++)
							magicToRealCard.add(card);

						break;
					}
				}
			} else {
				if (hasYaoJiu) {
					for (int i = 0; i < maxIndex - 1; i++) {
						if (handCardCounter[i] < 4 && handCardCounter[i] + magicToMagicCount >= 4) {
							int card = HandCardInfo.getCardByIndex(i);
							int value = getCardValue(card);

							if (value == 1 || value == 9) {
								gengCount++;

								for (int j = 0; j < magicToMagicCount; j++)
									magicToRealCard.add(card);

								break;
							}
						}
					}
				} else if (hasZhongZhang) {
					for (int i = 0; i < maxIndex - 1; i++) {
						if (handCardCounter[i] < 4 && handCardCounter[i] + magicToMagicCount >= 4) {
							int card = HandCardInfo.getCardByIndex(i);
							int value = getCardValue(card);

							if (value != 1 && value != 9) {
								gengCount++;

								for (int j = 0; j < magicToMagicCount; j++)
									magicToRealCard.add(card);

								break;
							}
						}
					}
				} else {
					for (int i = 0; i < maxIndex - 1; i++) {
						if (handCardCounter[i] < 4 && handCardCounter[i] + magicToMagicCount >= 4) {
							gengCount++;

							int card = HandCardInfo.getCardByIndex(i);
							for (int j = 0; j < magicToMagicCount; j++)
								magicToRealCard.add(card);

							break;
						}
					}
				}
			}
		}

		return gengCount;
	}

	private static int getCardValue(int card) {
		return card & 0x0F;
	}

	private void checkStatus(final Solution solution, final HandCardInfo handCardInfo) {
		int tmpGengCount = getGengCount(solution, handCardInfo);
		if (solution.weaveInfoList.size() + weaveCount == 5 && tmpGengCount > bestSolution.gengCount) {
			bestSolution = new Solution(solution);
			bestSolution.gengCount = tmpGengCount;
			hasFoundBestSolution = true;
		}
	}

	private void recall(Solution solution, HandCardInfo handCardInfo, WeaveInfo weaveInfo) {
		solution.pushWeaveInfo(weaveInfo, handCardInfo);
		analyse(handCardInfo, solution);
		solution.popWeaveInfo(handCardInfo);
	}

	private void analyse(final HandCardInfo handCardInfo, final Solution solution) {
		List<Integer> remainCardPositions = handCardInfo.getRemainCardPositions();

		int remainCount = remainCardPositions.size();

		if (remainCount == 0) {
			checkStatus(solution, handCardInfo);
		} else if (remainCount == 1 || remainCount == 3) {
		} else if (remainCount == 2) {
			int card_1 = handCardInfo.getCard(remainCardPositions.get(0));
			int card_2 = handCardInfo.getCard(remainCardPositions.get(1));

			Pair<Integer, List<Integer>> pair = getWeaveKeyPair(card_1, card_2, 0);
			if (pair.getFirst() >= 0) {
				Solution tmpSolution = new Solution(solution);

				WeaveInfo weaveInfo = new WeaveInfo(pair, remainCardPositions.get(0), remainCardPositions.get(1), -1);
				tmpSolution.pushWeaveInfo(weaveInfo, null);

				checkStatus(tmpSolution, handCardInfo);
			}
		} else {
			remainCount = remainCardPositions.size();

			Map<Long, WeaveInfo> map = new HashMap<>();

			Set<Integer> set = new HashSet<>();

			for (int i = 0; i < remainCount; i++) {
				for (int j = i + 1; j < remainCount; j++) {
					for (int k = j + 1; k < remainCount; k++) {
						int[] tmpPs = new int[] { remainCardPositions.get(i), remainCardPositions.get(j), remainCardPositions.get(k) };
						int card_1 = handCardInfo.getCard(tmpPs[0]);
						int card_2 = handCardInfo.getCard(tmpPs[1]);
						int card_3 = handCardInfo.getCard(tmpPs[2]);

						long key = getWeaveKey(card_1, card_2, card_3);
						Pair<Integer, List<Integer>> pair = getWeaveKeyPair(key);

						WeaveInfo weaveInfo = null;

						if ((hasDuiDuiHu && pair.getFirst() == GameConstants.WIK_PENG) || (!hasDuiDuiHu && pair.getFirst() > 0)) {
							set.add(tmpPs[0]);
							set.add(tmpPs[1]);
							set.add(tmpPs[2]);
							weaveInfo = new WeaveInfo(pair, tmpPs[0], tmpPs[1], tmpPs[2]);
							map.put(key, weaveInfo);
						}

						addedValue = 101;
						key = getWeaveKey(card_1, card_2, card_3);
						addedValue = 0;
						pair = getWeaveKeyPair(key);

						if ((hasDuiDuiHu && pair.getFirst() == GameConstants.WIK_PENG) || (!hasDuiDuiHu && pair.getFirst() > 0)) {
							set.add(tmpPs[0]);
							set.add(tmpPs[1]);
							set.add(tmpPs[2]);
							weaveInfo = new WeaveInfo(pair, tmpPs[0], tmpPs[1], tmpPs[2]);
							map.put(key, weaveInfo);
						}

						addedValue = 202;
						key = getWeaveKey(card_1, card_2, card_3);
						addedValue = 0;
						pair = getWeaveKeyPair(key);

						if ((hasDuiDuiHu && pair.getFirst() == GameConstants.WIK_PENG) || (!hasDuiDuiHu && pair.getFirst() > 0)) {
							set.add(tmpPs[0]);
							set.add(tmpPs[1]);
							set.add(tmpPs[2]);
							weaveInfo = new WeaveInfo(pair, tmpPs[0], tmpPs[1], tmpPs[2]);
							map.put(key, weaveInfo);
						}

						addedValue = 303;
						key = getWeaveKey(card_1, card_2, card_3);
						addedValue = 0;
						pair = getWeaveKeyPair(key);

						if ((hasDuiDuiHu && pair.getFirst() == GameConstants.WIK_PENG) || (!hasDuiDuiHu && pair.getFirst() > 0)) {
							set.add(tmpPs[0]);
							set.add(tmpPs[1]);
							set.add(tmpPs[2]);
							weaveInfo = new WeaveInfo(pair, tmpPs[0], tmpPs[1], tmpPs[2]);
							map.put(key, weaveInfo);
						}

						addedValue = 404;
						key = getWeaveKey(card_1, card_2, card_3);
						addedValue = 0;
						pair = getWeaveKeyPair(key);

						if ((hasDuiDuiHu && pair.getFirst() == GameConstants.WIK_PENG) || (!hasDuiDuiHu && pair.getFirst() > 0)) {
							set.add(tmpPs[0]);
							set.add(tmpPs[1]);
							set.add(tmpPs[2]);
							weaveInfo = new WeaveInfo(pair, tmpPs[0], tmpPs[1], tmpPs[2]);
							map.put(key, weaveInfo);
						}
					}
				}
			}

			if (set.size() < remainCount) {
				if (remainCount - set.size() == 2) {
					int tmpCount = 0;
					int[] tmpPs = new int[] { -1, -1 };
					for (int x = 0; x < remainCount; x++) {
						if (!set.contains(remainCardPositions.get(x))) {
							tmpPs[tmpCount++] = remainCardPositions.get(x);
						}
					}

					int card_1 = handCardInfo.getCard(tmpPs[0]);
					int card_2 = handCardInfo.getCard(tmpPs[1]);

					if (card_1 != MAGIC && card_2 != MAGIC && card_1 != card_2) {
						return;
					}
				} else {
					int tmpMagicCount = 0;
					for (int i = 0; i < remainCount; i++) {
						int card = handCardInfo.getCard(remainCardPositions.get(i));
						if (card == MAGIC)
							tmpMagicCount++;
					}
					if (remainCount - set.size() != 1 || tmpMagicCount <= 0) {
						return;
					}
				}
			}

			for (Map.Entry<Long, WeaveInfo> entry : map.entrySet()) {
				recall(solution, handCardInfo, entry.getValue());
			}
		}
	}

	public boolean getSolution(final List<Integer> handCard) {
		int cardCount = handCard.size();

		hasFoundBestSolution = false;

		bestSolution = new Solution();

		HandCardInfo handCardInfo = new HandCardInfo();
		Solution solution = new Solution();

		Map<Integer, List<Integer>> cardToCardPositions = new HashMap<>();

		for (int i = 0; i < cardCount; i++) {
			handCardInfo.addCard(handCard.get(i));

			if (!cardToCardPositions.containsKey(handCard.get(i))) {
				cardToCardPositions.put(handCard.get(i), new ArrayList<Integer>());
			}

			cardToCardPositions.get(handCard.get(i)).add(i);
		}

		analyse(handCardInfo, solution);

		return hasFoundBestSolution;
	}

	public boolean isHasDuiDuiHu() {
		return hasDuiDuiHu;
	}

	public void setHasDuiDuiHu(boolean hasDuiDuiHu) {
		this.hasDuiDuiHu = hasDuiDuiHu;
	}

	public boolean isHasQingYiSe() {
		return hasQingYiSe;
	}

	public void setHasQingYiSe(boolean hasQingYiSe) {
		this.hasQingYiSe = hasQingYiSe;
	}

	public boolean isHasZhongZhang() {
		return hasZhongZhang;
	}

	public void setHasZhongZhang(boolean hasZhongZhang) {
		this.hasZhongZhang = hasZhongZhang;
	}

	public boolean isHasYaoJiu() {
		return hasYaoJiu;
	}

	public void setHasYaoJiu(boolean hasYaoJiu) {
		this.hasYaoJiu = hasYaoJiu;
	}

	public boolean isHasJiangDui() {
		return hasJiangDui;
	}

	public void setHasJiangDui(boolean hasJiangDui) {
		this.hasJiangDui = hasJiangDui;
	}

	public List<Integer> getMagicToRealCard() {
		return magicToRealCard;
	}

	public Solution getBestSolution() {
		return bestSolution;
	}
}

class HandCardInfo {
	public List<Integer> handCards = new ArrayList<>();
	private int cardsFlag = 0;
	@SuppressWarnings("unused")
	private int remainCardCount = 0;
	@SuppressWarnings("unchecked")
	public List<Integer>[] cardIndexToCardPositions = new ArrayList[MjAnalyseUtil.maxIndex];

	public HandCardInfo() {
		handCards = new ArrayList<>();
		cardsFlag = 0;
		remainCardCount = 0;

		for (int i = 0; i < MjAnalyseUtil.maxIndex; i++) {
			cardIndexToCardPositions[i] = new ArrayList<Integer>();
		}
	}

	public static int getCardIndex(int card) {
		if (card == MjAnalyseUtil.MAGIC)
			return MjAnalyseUtil.maxIndex - 1;
		else if (card < 0x01 || card > 0x37)
			return -1;

		int color = (card & 0xF0) >> 4;
		int value = card & 0x0F;
		int index = color * 9 + value - 1;

		return index;
	}

	public static int getCardByIndex(int index) {
		if (index == MjAnalyseUtil.maxIndex - 1)
			return MjAnalyseUtil.MAGIC;
		else if (index < 0 || index > MjAnalyseUtil.maxIndex - 1)
			return -1;

		return ((index / 9) << 4) | (index % 9 + 1);
	}

	public void addCard(int card) {
		int index = getCardIndex(card);
		if (index >= 0 && index <= MjAnalyseUtil.maxIndex - 1) {
			// 记录某种类型的所有牌 在手牌的什么位置
			cardIndexToCardPositions[index].add(handCards.size());
		}

		handCards.add(card);
		remainCardCount++;
	}

	public int getCard(int position) {
		return handCards.get(position);
	}

	public List<Integer> getRemainCardPositions() {
		List<Integer> remainCardPositions = new ArrayList<>();

		int cardCount = handCards.size();
		for (int i = 0; i < cardCount; i++) {
			if (!cardIsUsed(i)) {
				remainCardPositions.add(i);
			}
		}

		return remainCardPositions;
	}

	private boolean cardIsUsed(int position) {
		int result = cardsFlag & (1 << position);
		return result != 0 ? true : false;
	}

	public void setIsUsed(int position, boolean isUsed) {
		if (position < handCards.size()) {
			if (isUsed) {
				if (!cardIsUsed(position)) {
					int cardIndex = getCardIndex(handCards.get(position));

					if (cardIndex >= 0 && cardIndex <= MjAnalyseUtil.maxIndex - 1) {
						int tmpIndex = cardIndexToCardPositions[cardIndex].indexOf(position);
						if (tmpIndex != -1) {
							cardIndexToCardPositions[cardIndex].remove(tmpIndex);
						}
					}

					remainCardCount--;
					cardsFlag = cardsFlag | (1 << position);
				}
			} else {
				if (cardIsUsed(position)) {
					int cardIndex = getCardIndex(handCards.get(position));
					if (cardIndex >= 0 && cardIndex <= MjAnalyseUtil.maxIndex - 1) {
						cardIndexToCardPositions[cardIndex].add(position);
					}

					remainCardCount++;
					cardsFlag = cardsFlag & (~(1 << position));
				}
			}
		}
	}

	public List<Integer> getCardPositionsByCard(int card) {
		int index = getCardIndex(card);
		return cardIndexToCardPositions[index];
	}
}
