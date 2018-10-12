package com.cai.game.hh.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.WeaveItem;

public class AnalyseUtil {
	private static final int MAGIC = 0xFF;
	private static final Map<Long, Triple<Integer, Integer, List<Integer>>> keyTripleMap = new HashMap<>();
	private Solution bestSolution = new Solution(0);
	private int scoreFence = 0;
	private boolean hasFoundBestSolution = false;
	private boolean wangDiao = false;
	private int specialKeyFence = 0;
	private int xingPai = -1;
	private WeaveItem[] weaveItems;
	public int weaveCount;
	private boolean hasHongZhuanHei = false;
	public List<Integer> addedCards = null;
	private int weiAddedCard = 0;
	private int qiHuFan = 0;
	private boolean hasZiMo = false;
	private boolean hasWangDiao = false;
	private boolean hasWangDiaoWang = false;
	private boolean hasWangChuang = false;
	private boolean hasWangChuangWang = false;
	private boolean hasWangZha = false;
	private boolean hasWangZhaWang = false;
	private boolean qiShouGenXing = false;
	private boolean hasRuleFanXing = false;

	private static boolean initialized = false;

	public AnalyseUtil(int scoreFence, int xingPai, int qiHuFan, int weaveCount, WeaveItem[] weaveItems, boolean hasHongZhuanHei,
			boolean hasRuleFanXing) {
		this.scoreFence = scoreFence;
		this.xingPai = xingPai;
		this.qiHuFan = qiHuFan;
		this.weaveCount = weaveCount;
		this.weaveItems = weaveItems;
		this.hasHongZhuanHei = hasHongZhuanHei;
		this.hasRuleFanXing = hasRuleFanXing;

		if (initialized == false) {
			initial();
			initialized = true;
		}
	}

	public Solution getBestSolution() {
		return bestSolution;
	}

	private void initial() {
		addTiKeyTriple();
		addKanJiaoKeyTriple();
		addTwoSevenTenKeyTriple();
		addShunZiKeyTriple();
		addDuiZiKeyTriple();
	}

	private long getCardsKey(int card_1, int card_2, int card_3, int card_4) {
		List<Integer> cards = new ArrayList<>();
		cards.add(card_1);
		cards.add(card_2);
		cards.add(card_3);
		cards.add(card_4);
		long key = cards.get(0) + (cards.get(1) << 8) + (cards.get(2) << 16) + (long) (cards.get(3) << 24);
		if (specialKeyFence > 0) {
			key += specialKeyFence;
		}
		return key;
	}

	private void addKeyTriple(long key, Triple<Integer, Integer, List<Integer>> triple) {
		if (!keyTripleMap.containsKey(key)) {
			keyTripleMap.put(key, triple);
		}
	}

	private void addTiKeyTriple() {
		for (int card = 1; card <= 10; card++) {
			int bigCard = card + 0x10;
			addKeyTriple(getCardsKey(card, card, card, card), Triple.of(9, GameConstants.WIK_TI_LONG, Arrays.asList()));
			addKeyTriple(getCardsKey(bigCard, bigCard, bigCard, bigCard), Triple.of(12, GameConstants.WIK_TI_LONG, Arrays.asList()));

			addKeyTriple(getCardsKey(MAGIC, card, card, card), Triple.of(9, GameConstants.WIK_TI_LONG, Arrays.asList(card)));
			addKeyTriple(getCardsKey(MAGIC, MAGIC, card, card), Triple.of(9, GameConstants.WIK_TI_LONG, Arrays.asList(card, card)));
			addKeyTriple(getCardsKey(MAGIC, MAGIC, MAGIC, card), Triple.of(9, GameConstants.WIK_TI_LONG, Arrays.asList(card, card, card)));

			addKeyTriple(getCardsKey(MAGIC, bigCard, bigCard, bigCard), Triple.of(12, GameConstants.WIK_TI_LONG, Arrays.asList(bigCard)));
			addKeyTriple(getCardsKey(MAGIC, MAGIC, bigCard, bigCard), Triple.of(12, GameConstants.WIK_TI_LONG, Arrays.asList(bigCard, bigCard)));
			addKeyTriple(getCardsKey(MAGIC, MAGIC, MAGIC, bigCard),
					Triple.of(12, GameConstants.WIK_TI_LONG, Arrays.asList(bigCard, bigCard, bigCard)));
		}

		addKeyTriple(getCardsKey(MAGIC, MAGIC, MAGIC, MAGIC), Triple.of(12, GameConstants.WIK_TI_LONG, Arrays.asList(MAGIC, MAGIC, MAGIC, MAGIC)));
	}

	private void addKanJiaoKeyTriple() {
		for (int card = 1; card <= 10; card++) {
			int bigCard = card + 0x10;
			addKeyTriple(getCardsKey(card, card, card, 0), Triple.of(3, GameConstants.WIK_KAN, Arrays.asList()));
			addKeyTriple(getCardsKey(bigCard, bigCard, bigCard, 0), Triple.of(6, GameConstants.WIK_KAN, Arrays.asList()));

			// 绞牌
			addKeyTriple(getCardsKey(bigCard, card, card, 0), Triple.of(0, GameConstants.WIK_XXD, Arrays.asList()));
			addKeyTriple(getCardsKey(card, bigCard, card, 0), Triple.of(0, GameConstants.WIK_XXD, Arrays.asList()));
			addKeyTriple(getCardsKey(card, card, bigCard, 0), Triple.of(0, GameConstants.WIK_XXD, Arrays.asList()));

			addKeyTriple(getCardsKey(bigCard, bigCard, card, 0), Triple.of(0, GameConstants.WIK_DDX, Arrays.asList()));
			addKeyTriple(getCardsKey(bigCard, card, bigCard, 0), Triple.of(0, GameConstants.WIK_DDX, Arrays.asList()));
			addKeyTriple(getCardsKey(card, bigCard, bigCard, 0), Triple.of(0, GameConstants.WIK_DDX, Arrays.asList()));

			addKeyTriple(getCardsKey(MAGIC, bigCard, card, 0), Triple.of(0, GameConstants.WIK_DDX, Arrays.asList(bigCard)));
			addKeyTriple(getCardsKey(MAGIC, card, bigCard, 0), Triple.of(0, GameConstants.WIK_DDX, Arrays.asList(bigCard)));
			addKeyTriple(getCardsKey(card, MAGIC, bigCard, 0), Triple.of(0, GameConstants.WIK_DDX, Arrays.asList(bigCard)));
			addKeyTriple(getCardsKey(card, bigCard, MAGIC, 0), Triple.of(0, GameConstants.WIK_DDX, Arrays.asList(bigCard)));
			addKeyTriple(getCardsKey(bigCard, card, MAGIC, 0), Triple.of(0, GameConstants.WIK_DDX, Arrays.asList(bigCard)));
			addKeyTriple(getCardsKey(bigCard, MAGIC, card, 0), Triple.of(0, GameConstants.WIK_DDX, Arrays.asList(bigCard)));

			specialKeyFence = 101;
			addKeyTriple(getCardsKey(MAGIC, card, bigCard, 0), Triple.of(0, GameConstants.WIK_XXD, Arrays.asList(card)));
			addKeyTriple(getCardsKey(MAGIC, bigCard, card, 0), Triple.of(0, GameConstants.WIK_XXD, Arrays.asList(card)));
			addKeyTriple(getCardsKey(card, MAGIC, bigCard, 0), Triple.of(0, GameConstants.WIK_XXD, Arrays.asList(card)));
			addKeyTriple(getCardsKey(card, bigCard, MAGIC, 0), Triple.of(0, GameConstants.WIK_XXD, Arrays.asList(card)));
			addKeyTriple(getCardsKey(bigCard, MAGIC, card, 0), Triple.of(0, GameConstants.WIK_XXD, Arrays.asList(card)));
			addKeyTriple(getCardsKey(bigCard, card, MAGIC, 0), Triple.of(0, GameConstants.WIK_XXD, Arrays.asList(card)));
			specialKeyFence = 0;

			addKeyTriple(getCardsKey(MAGIC, card, card, 0), Triple.of(3, GameConstants.WIK_KAN, Arrays.asList(card)));
			addKeyTriple(getCardsKey(card, MAGIC, card, 0), Triple.of(3, GameConstants.WIK_KAN, Arrays.asList(card)));
			addKeyTriple(getCardsKey(card, card, MAGIC, 0), Triple.of(3, GameConstants.WIK_KAN, Arrays.asList(card)));

			addKeyTriple(getCardsKey(MAGIC, MAGIC, card, 0), Triple.of(3, GameConstants.WIK_KAN, Arrays.asList(card, card)));
			addKeyTriple(getCardsKey(MAGIC, card, MAGIC, 0), Triple.of(3, GameConstants.WIK_KAN, Arrays.asList(card, card)));
			addKeyTriple(getCardsKey(card, MAGIC, MAGIC, 0), Triple.of(3, GameConstants.WIK_KAN, Arrays.asList(card, card)));

			addKeyTriple(getCardsKey(MAGIC, bigCard, bigCard, 0), Triple.of(6, GameConstants.WIK_KAN, Arrays.asList(bigCard)));
			addKeyTriple(getCardsKey(bigCard, MAGIC, bigCard, 0), Triple.of(6, GameConstants.WIK_KAN, Arrays.asList(bigCard)));
			addKeyTriple(getCardsKey(bigCard, bigCard, MAGIC, 0), Triple.of(6, GameConstants.WIK_KAN, Arrays.asList(bigCard)));

			addKeyTriple(getCardsKey(MAGIC, MAGIC, bigCard, 0), Triple.of(6, GameConstants.WIK_KAN, Arrays.asList(bigCard, bigCard)));
			addKeyTriple(getCardsKey(MAGIC, bigCard, MAGIC, 0), Triple.of(6, GameConstants.WIK_KAN, Arrays.asList(bigCard, bigCard)));
			addKeyTriple(getCardsKey(bigCard, MAGIC, MAGIC, 0), Triple.of(6, GameConstants.WIK_KAN, Arrays.asList(bigCard, bigCard)));
		}

		addKeyTriple(getCardsKey(MAGIC, MAGIC, MAGIC, 0), Triple.of(6, GameConstants.WIK_KAN, Arrays.asList(MAGIC, MAGIC, MAGIC)));
	}

	private void addTwoSevenTenKeyTriple() {
		int two = 0x02;
		int seven = 0x07;
		int ten = 0x0A;
		int bigTwo = 0x12;
		int bigSeven = 0x17;
		int bigTen = 0x1A;

		addKeyTriple(getCardsKey(two, seven, ten, 0), Triple.of(3, GameConstants.WIK_EQS, Arrays.asList()));
		addKeyTriple(getCardsKey(two, ten, seven, 0), Triple.of(3, GameConstants.WIK_EQS, Arrays.asList()));
		addKeyTriple(getCardsKey(seven, two, ten, 0), Triple.of(3, GameConstants.WIK_EQS, Arrays.asList()));
		addKeyTriple(getCardsKey(seven, ten, two, 0), Triple.of(3, GameConstants.WIK_EQS, Arrays.asList()));
		addKeyTriple(getCardsKey(ten, two, seven, 0), Triple.of(3, GameConstants.WIK_EQS, Arrays.asList()));
		addKeyTriple(getCardsKey(ten, seven, two, 0), Triple.of(3, GameConstants.WIK_EQS, Arrays.asList()));

		addKeyTriple(getCardsKey(bigTwo, bigSeven, bigTen, 0), Triple.of(6, GameConstants.WIK_EQS, Arrays.asList()));
		addKeyTriple(getCardsKey(bigTwo, bigTen, bigSeven, 0), Triple.of(6, GameConstants.WIK_EQS, Arrays.asList()));
		addKeyTriple(getCardsKey(bigSeven, bigTwo, bigTen, 0), Triple.of(6, GameConstants.WIK_EQS, Arrays.asList()));
		addKeyTriple(getCardsKey(bigSeven, bigTen, bigTwo, 0), Triple.of(6, GameConstants.WIK_EQS, Arrays.asList()));
		addKeyTriple(getCardsKey(bigTen, bigTwo, bigSeven, 0), Triple.of(6, GameConstants.WIK_EQS, Arrays.asList()));
		addKeyTriple(getCardsKey(bigTen, bigSeven, bigTwo, 0), Triple.of(6, GameConstants.WIK_EQS, Arrays.asList()));

		addKeyTriple(getCardsKey(two, seven, MAGIC, 0), Triple.of(3, GameConstants.WIK_EQS, Arrays.asList(ten)));
		addKeyTriple(getCardsKey(two, MAGIC, seven, 0), Triple.of(3, GameConstants.WIK_EQS, Arrays.asList(ten)));
		addKeyTriple(getCardsKey(seven, two, MAGIC, 0), Triple.of(3, GameConstants.WIK_EQS, Arrays.asList(ten)));
		addKeyTriple(getCardsKey(seven, MAGIC, two, 0), Triple.of(3, GameConstants.WIK_EQS, Arrays.asList(ten)));
		addKeyTriple(getCardsKey(MAGIC, two, seven, 0), Triple.of(3, GameConstants.WIK_EQS, Arrays.asList(ten)));
		addKeyTriple(getCardsKey(MAGIC, seven, two, 0), Triple.of(3, GameConstants.WIK_EQS, Arrays.asList(ten)));

		addKeyTriple(getCardsKey(two, MAGIC, ten, 0), Triple.of(3, GameConstants.WIK_EQS, Arrays.asList(seven)));
		addKeyTriple(getCardsKey(two, ten, MAGIC, 0), Triple.of(3, GameConstants.WIK_EQS, Arrays.asList(seven)));
		addKeyTriple(getCardsKey(ten, MAGIC, two, 0), Triple.of(3, GameConstants.WIK_EQS, Arrays.asList(seven)));
		addKeyTriple(getCardsKey(ten, two, MAGIC, 0), Triple.of(3, GameConstants.WIK_EQS, Arrays.asList(seven)));
		addKeyTriple(getCardsKey(MAGIC, two, ten, 0), Triple.of(3, GameConstants.WIK_EQS, Arrays.asList(seven)));
		addKeyTriple(getCardsKey(MAGIC, ten, two, 0), Triple.of(3, GameConstants.WIK_EQS, Arrays.asList(seven)));

		addKeyTriple(getCardsKey(MAGIC, seven, ten, 0), Triple.of(3, GameConstants.WIK_EQS, Arrays.asList(two)));
		addKeyTriple(getCardsKey(MAGIC, ten, seven, 0), Triple.of(3, GameConstants.WIK_EQS, Arrays.asList(two)));
		addKeyTriple(getCardsKey(ten, seven, MAGIC, 0), Triple.of(3, GameConstants.WIK_EQS, Arrays.asList(two)));
		addKeyTriple(getCardsKey(ten, MAGIC, seven, 0), Triple.of(3, GameConstants.WIK_EQS, Arrays.asList(two)));
		addKeyTriple(getCardsKey(seven, ten, MAGIC, 0), Triple.of(3, GameConstants.WIK_EQS, Arrays.asList(two)));
		addKeyTriple(getCardsKey(seven, MAGIC, ten, 0), Triple.of(3, GameConstants.WIK_EQS, Arrays.asList(two)));

		addKeyTriple(getCardsKey(bigTwo, bigSeven, MAGIC, 0), Triple.of(6, GameConstants.WIK_EQS, Arrays.asList(bigTen)));
		addKeyTriple(getCardsKey(bigTwo, MAGIC, bigSeven, 0), Triple.of(6, GameConstants.WIK_EQS, Arrays.asList(bigTen)));
		addKeyTriple(getCardsKey(bigSeven, MAGIC, bigTwo, 0), Triple.of(6, GameConstants.WIK_EQS, Arrays.asList(bigTen)));
		addKeyTriple(getCardsKey(bigSeven, bigTwo, MAGIC, 0), Triple.of(6, GameConstants.WIK_EQS, Arrays.asList(bigTen)));
		addKeyTriple(getCardsKey(MAGIC, bigTwo, bigSeven, 0), Triple.of(6, GameConstants.WIK_EQS, Arrays.asList(bigTen)));
		addKeyTriple(getCardsKey(MAGIC, bigSeven, bigTwo, 0), Triple.of(6, GameConstants.WIK_EQS, Arrays.asList(bigTen)));

		addKeyTriple(getCardsKey(bigTwo, MAGIC, bigTen, 0), Triple.of(6, GameConstants.WIK_EQS, Arrays.asList(bigSeven)));
		addKeyTriple(getCardsKey(bigTwo, bigTen, MAGIC, 0), Triple.of(6, GameConstants.WIK_EQS, Arrays.asList(bigSeven)));
		addKeyTriple(getCardsKey(bigTen, MAGIC, bigTwo, 0), Triple.of(6, GameConstants.WIK_EQS, Arrays.asList(bigSeven)));
		addKeyTriple(getCardsKey(bigTen, bigTwo, MAGIC, 0), Triple.of(6, GameConstants.WIK_EQS, Arrays.asList(bigSeven)));
		addKeyTriple(getCardsKey(MAGIC, bigTen, bigTwo, 0), Triple.of(6, GameConstants.WIK_EQS, Arrays.asList(bigSeven)));
		addKeyTriple(getCardsKey(MAGIC, bigTwo, bigTen, 0), Triple.of(6, GameConstants.WIK_EQS, Arrays.asList(bigSeven)));

		addKeyTriple(getCardsKey(MAGIC, bigSeven, bigTen, 0), Triple.of(6, GameConstants.WIK_EQS, Arrays.asList(bigTwo)));
		addKeyTriple(getCardsKey(MAGIC, bigTen, bigSeven, 0), Triple.of(6, GameConstants.WIK_EQS, Arrays.asList(bigTwo)));
		addKeyTriple(getCardsKey(bigTen, bigSeven, MAGIC, 0), Triple.of(6, GameConstants.WIK_EQS, Arrays.asList(bigTwo)));
		addKeyTriple(getCardsKey(bigTen, MAGIC, bigSeven, 0), Triple.of(6, GameConstants.WIK_EQS, Arrays.asList(bigTwo)));
		addKeyTriple(getCardsKey(bigSeven, MAGIC, bigTen, 0), Triple.of(6, GameConstants.WIK_EQS, Arrays.asList(bigTwo)));
		addKeyTriple(getCardsKey(bigSeven, bigTen, MAGIC, 0), Triple.of(6, GameConstants.WIK_EQS, Arrays.asList(bigTwo)));

		specialKeyFence = 202;
		addKeyTriple(getCardsKey(two, MAGIC, MAGIC, 0), Triple.of(3, GameConstants.WIK_EQS, Arrays.asList(seven, ten)));
		addKeyTriple(getCardsKey(MAGIC, two, MAGIC, 0), Triple.of(3, GameConstants.WIK_EQS, Arrays.asList(seven, ten)));
		addKeyTriple(getCardsKey(MAGIC, MAGIC, two, 0), Triple.of(3, GameConstants.WIK_EQS, Arrays.asList(seven, ten)));

		addKeyTriple(getCardsKey(seven, MAGIC, MAGIC, 0), Triple.of(3, GameConstants.WIK_EQS, Arrays.asList(two, ten)));
		addKeyTriple(getCardsKey(MAGIC, seven, MAGIC, 0), Triple.of(3, GameConstants.WIK_EQS, Arrays.asList(two, ten)));
		addKeyTriple(getCardsKey(MAGIC, MAGIC, seven, 0), Triple.of(3, GameConstants.WIK_EQS, Arrays.asList(two, ten)));

		addKeyTriple(getCardsKey(ten, MAGIC, MAGIC, 0), Triple.of(3, GameConstants.WIK_EQS, Arrays.asList(two, seven)));
		addKeyTriple(getCardsKey(MAGIC, ten, MAGIC, 0), Triple.of(3, GameConstants.WIK_EQS, Arrays.asList(two, seven)));
		addKeyTriple(getCardsKey(MAGIC, MAGIC, ten, 0), Triple.of(3, GameConstants.WIK_EQS, Arrays.asList(two, seven)));

		addKeyTriple(getCardsKey(bigTwo, MAGIC, MAGIC, 0), Triple.of(3, GameConstants.WIK_EQS, Arrays.asList(bigSeven, bigTen)));
		addKeyTriple(getCardsKey(MAGIC, bigTwo, MAGIC, 0), Triple.of(3, GameConstants.WIK_EQS, Arrays.asList(bigSeven, bigTen)));
		addKeyTriple(getCardsKey(MAGIC, MAGIC, bigTwo, 0), Triple.of(3, GameConstants.WIK_EQS, Arrays.asList(bigSeven, bigTen)));

		addKeyTriple(getCardsKey(bigSeven, MAGIC, MAGIC, 0), Triple.of(3, GameConstants.WIK_EQS, Arrays.asList(bigTwo, bigTen)));
		addKeyTriple(getCardsKey(MAGIC, bigSeven, MAGIC, 0), Triple.of(3, GameConstants.WIK_EQS, Arrays.asList(bigTwo, bigTen)));
		addKeyTriple(getCardsKey(MAGIC, MAGIC, bigSeven, 0), Triple.of(3, GameConstants.WIK_EQS, Arrays.asList(bigTwo, bigTen)));

		addKeyTriple(getCardsKey(bigTen, MAGIC, MAGIC, 0), Triple.of(3, GameConstants.WIK_EQS, Arrays.asList(bigTwo, bigSeven)));
		addKeyTriple(getCardsKey(MAGIC, bigTen, MAGIC, 0), Triple.of(3, GameConstants.WIK_EQS, Arrays.asList(bigTwo, bigSeven)));
		addKeyTriple(getCardsKey(MAGIC, MAGIC, bigTen, 0), Triple.of(3, GameConstants.WIK_EQS, Arrays.asList(bigTwo, bigSeven)));
		specialKeyFence = 0;

		specialKeyFence = 303;
		addKeyTriple(getCardsKey(MAGIC, MAGIC, MAGIC, 0), Triple.of(6, GameConstants.WIK_EQS, Arrays.asList(bigTwo, bigSeven, bigTen)));
		specialKeyFence = 0;

		specialKeyFence = 404;
		addKeyTriple(getCardsKey(MAGIC, MAGIC, MAGIC, 0), Triple.of(3, GameConstants.WIK_EQS, Arrays.asList(two, seven, ten)));
		specialKeyFence = 0;
	}

	private void addShunZiKeyTriple() {
		for (int card = 1; card <= 8; card++) {
			int bigCard = card + 0x10;
			int huXi = (card == 1) ? 3 : 0;
			addKeyTriple(getCardsKey(card, card + 1, card + 2, 0), Triple.of(huXi, GameConstants.WIK_LEFT, Arrays.asList()));
			addKeyTriple(getCardsKey(card, card + 2, card + 1, 0), Triple.of(huXi, GameConstants.WIK_LEFT, Arrays.asList()));
			addKeyTriple(getCardsKey(card + 1, card, card + 2, 0), Triple.of(huXi, GameConstants.WIK_LEFT, Arrays.asList()));
			addKeyTriple(getCardsKey(card + 1, card + 2, card, 0), Triple.of(huXi, GameConstants.WIK_LEFT, Arrays.asList()));
			addKeyTriple(getCardsKey(card + 2, card, card + 1, 0), Triple.of(huXi, GameConstants.WIK_LEFT, Arrays.asList()));
			addKeyTriple(getCardsKey(card + 2, card + 1, card, 0), Triple.of(huXi, GameConstants.WIK_LEFT, Arrays.asList()));

			addKeyTriple(getCardsKey(bigCard, bigCard + 1, bigCard + 2, 0), Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList()));
			addKeyTriple(getCardsKey(bigCard, bigCard + 2, bigCard + 1, 0), Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList()));
			addKeyTriple(getCardsKey(bigCard + 1, bigCard, bigCard + 2, 0), Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList()));
			addKeyTriple(getCardsKey(bigCard + 1, bigCard + 2, bigCard, 0), Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList()));
			addKeyTriple(getCardsKey(bigCard + 2, bigCard, bigCard + 1, 0), Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList()));
			addKeyTriple(getCardsKey(bigCard + 2, bigCard + 1, bigCard, 0), Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList()));

			addKeyTriple(getCardsKey(MAGIC, card + 1, card + 2, 0), Triple.of(huXi, GameConstants.WIK_LEFT, Arrays.asList(card)));
			addKeyTriple(getCardsKey(MAGIC, card + 2, card + 1, 0), Triple.of(huXi, GameConstants.WIK_LEFT, Arrays.asList(card)));
			addKeyTriple(getCardsKey(card + 1, card + 2, MAGIC, 0), Triple.of(huXi, GameConstants.WIK_LEFT, Arrays.asList(card)));
			addKeyTriple(getCardsKey(card + 1, MAGIC, card + 2, 0), Triple.of(huXi, GameConstants.WIK_LEFT, Arrays.asList(card)));
			addKeyTriple(getCardsKey(card + 2, card + 1, MAGIC, 0), Triple.of(huXi, GameConstants.WIK_LEFT, Arrays.asList(card)));
			addKeyTriple(getCardsKey(card + 2, MAGIC, card + 1, 0), Triple.of(huXi, GameConstants.WIK_LEFT, Arrays.asList(card)));

			addKeyTriple(getCardsKey(card, MAGIC, card + 2, 0), Triple.of(huXi, GameConstants.WIK_LEFT, Arrays.asList(card + 1)));
			addKeyTriple(getCardsKey(card, card + 2, MAGIC, 0), Triple.of(huXi, GameConstants.WIK_LEFT, Arrays.asList(card + 1)));
			addKeyTriple(getCardsKey(card + 2, card, MAGIC, 0), Triple.of(huXi, GameConstants.WIK_LEFT, Arrays.asList(card + 1)));
			addKeyTriple(getCardsKey(card + 2, MAGIC, card, 0), Triple.of(huXi, GameConstants.WIK_LEFT, Arrays.asList(card + 1)));
			addKeyTriple(getCardsKey(MAGIC, card, card + 2, 0), Triple.of(huXi, GameConstants.WIK_LEFT, Arrays.asList(card + 1)));
			addKeyTriple(getCardsKey(MAGIC, card + 2, card, 0), Triple.of(huXi, GameConstants.WIK_LEFT, Arrays.asList(card + 1)));

			specialKeyFence = 101;
			addKeyTriple(getCardsKey(card, card + 1, MAGIC, 0), Triple.of(huXi, GameConstants.WIK_LEFT, Arrays.asList(card + 2)));
			addKeyTriple(getCardsKey(card, MAGIC, card + 1, 0), Triple.of(huXi, GameConstants.WIK_LEFT, Arrays.asList(card + 2)));
			addKeyTriple(getCardsKey(card + 1, MAGIC, card, 0), Triple.of(huXi, GameConstants.WIK_LEFT, Arrays.asList(card + 2)));
			addKeyTriple(getCardsKey(card + 1, card, MAGIC, 0), Triple.of(huXi, GameConstants.WIK_LEFT, Arrays.asList(card + 2)));
			addKeyTriple(getCardsKey(MAGIC, card + 1, card, 0), Triple.of(huXi, GameConstants.WIK_LEFT, Arrays.asList(card + 2)));
			addKeyTriple(getCardsKey(MAGIC, card, card + 1, 0), Triple.of(huXi, GameConstants.WIK_LEFT, Arrays.asList(card + 2)));
			specialKeyFence = 0;

			addKeyTriple(getCardsKey(MAGIC, bigCard + 1, bigCard + 2, 0), Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(bigCard)));
			addKeyTriple(getCardsKey(MAGIC, bigCard + 2, bigCard + 1, 0), Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(bigCard)));
			addKeyTriple(getCardsKey(bigCard + 2, MAGIC, bigCard + 1, 0), Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(bigCard)));
			addKeyTriple(getCardsKey(bigCard + 2, bigCard + 1, MAGIC, 0), Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(bigCard)));
			addKeyTriple(getCardsKey(bigCard + 1, bigCard + 2, MAGIC, 0), Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(bigCard)));
			addKeyTriple(getCardsKey(bigCard + 1, MAGIC, bigCard + 2, 0), Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(bigCard)));

			addKeyTriple(getCardsKey(bigCard, MAGIC, bigCard + 2, 0), Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(bigCard + 1)));
			addKeyTriple(getCardsKey(bigCard, bigCard + 2, MAGIC, 0), Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(bigCard + 1)));
			addKeyTriple(getCardsKey(bigCard + 2, bigCard, MAGIC, 0), Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(bigCard + 1)));
			addKeyTriple(getCardsKey(bigCard + 2, MAGIC, bigCard, 0), Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(bigCard + 1)));
			addKeyTriple(getCardsKey(MAGIC, bigCard + 2, bigCard, 0), Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(bigCard + 1)));
			addKeyTriple(getCardsKey(MAGIC, bigCard, bigCard + 2, 0), Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(bigCard + 1)));

			specialKeyFence = 101;
			addKeyTriple(getCardsKey(bigCard, bigCard + 1, MAGIC, 0), Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(bigCard + 2)));
			addKeyTriple(getCardsKey(bigCard, MAGIC, bigCard + 1, 0), Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(bigCard + 2)));
			addKeyTriple(getCardsKey(MAGIC, bigCard, bigCard + 1, 0), Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(bigCard + 2)));
			addKeyTriple(getCardsKey(MAGIC, bigCard + 1, bigCard, 0), Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(bigCard + 2)));
			addKeyTriple(getCardsKey(bigCard + 1, MAGIC, bigCard, 0), Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(bigCard + 2)));
			addKeyTriple(getCardsKey(bigCard + 1, bigCard, MAGIC, 0), Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(bigCard + 2)));
			specialKeyFence = 0;
		}

		for (int card = 1; card <= 10; card++) {
			int bigCard = card + 0x10;
			int huXi = (card == 1) ? 3 : 0;

			if (card == 0x01) {
				specialKeyFence = 505;
				addKeyTriple(getCardsKey(card, MAGIC, MAGIC, 0), Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(card + 1, card + 2)));
				addKeyTriple(getCardsKey(MAGIC, card, MAGIC, 0), Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(card + 1, card + 2)));
				addKeyTriple(getCardsKey(MAGIC, MAGIC, card, 0), Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(card + 1, card + 2)));

				addKeyTriple(getCardsKey(bigCard, MAGIC, MAGIC, 0),
						Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(bigCard + 1, bigCard + 2)));
				addKeyTriple(getCardsKey(MAGIC, bigCard, MAGIC, 0),
						Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(bigCard + 1, bigCard + 2)));
				addKeyTriple(getCardsKey(MAGIC, MAGIC, bigCard, 0),
						Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(bigCard + 1, bigCard + 2)));
				specialKeyFence = 0;
			} else if (card == 0x02) {
				specialKeyFence = 505;
				addKeyTriple(getCardsKey(card, MAGIC, MAGIC, 0), Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(card - 1, card + 1)));
				addKeyTriple(getCardsKey(MAGIC, card, MAGIC, 0), Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(card - 1, card + 1)));
				addKeyTriple(getCardsKey(MAGIC, MAGIC, card, 0), Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(card - 1, card + 1)));

				addKeyTriple(getCardsKey(bigCard, MAGIC, MAGIC, 0),
						Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(bigCard - 1, bigCard + 1)));
				addKeyTriple(getCardsKey(MAGIC, bigCard, MAGIC, 0),
						Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(bigCard - 1, bigCard + 1)));
				addKeyTriple(getCardsKey(MAGIC, MAGIC, bigCard, 0),
						Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(bigCard - 1, bigCard + 1)));
				specialKeyFence = 0;

				specialKeyFence = 606;
				addKeyTriple(getCardsKey(card, MAGIC, MAGIC, 0), Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(card + 1, card + 2)));
				addKeyTriple(getCardsKey(MAGIC, card, MAGIC, 0), Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(card + 1, card + 2)));
				addKeyTriple(getCardsKey(MAGIC, MAGIC, card, 0), Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(card + 1, card + 2)));

				addKeyTriple(getCardsKey(bigCard, MAGIC, MAGIC, 0),
						Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(bigCard + 1, bigCard + 2)));
				addKeyTriple(getCardsKey(MAGIC, bigCard, MAGIC, 0),
						Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(bigCard + 1, bigCard + 2)));
				addKeyTriple(getCardsKey(MAGIC, MAGIC, bigCard, 0),
						Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(bigCard + 1, bigCard + 2)));
				specialKeyFence = 0;
			} else if (card == 0x09) {
				specialKeyFence = 505;
				addKeyTriple(getCardsKey(card, MAGIC, MAGIC, 0), Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(card - 1, card + 1)));
				addKeyTriple(getCardsKey(MAGIC, card, MAGIC, 0), Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(card - 1, card + 1)));
				addKeyTriple(getCardsKey(MAGIC, MAGIC, card, 0), Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(card - 1, card + 1)));

				addKeyTriple(getCardsKey(bigCard, MAGIC, MAGIC, 0),
						Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(bigCard - 1, bigCard + 1)));
				addKeyTriple(getCardsKey(MAGIC, bigCard, MAGIC, 0),
						Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(bigCard - 1, bigCard + 1)));
				addKeyTriple(getCardsKey(MAGIC, MAGIC, bigCard, 0),
						Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(bigCard - 1, bigCard + 1)));
				specialKeyFence = 0;

				specialKeyFence = 606;
				addKeyTriple(getCardsKey(card, MAGIC, MAGIC, 0), Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(card - 1, card - 2)));
				addKeyTriple(getCardsKey(MAGIC, card, MAGIC, 0), Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(card - 1, card - 2)));
				addKeyTriple(getCardsKey(MAGIC, MAGIC, card, 0), Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(card - 1, card - 2)));

				addKeyTriple(getCardsKey(bigCard, MAGIC, MAGIC, 0),
						Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(bigCard - 1, bigCard - 2)));
				addKeyTriple(getCardsKey(MAGIC, bigCard, MAGIC, 0),
						Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(bigCard - 1, bigCard - 2)));
				addKeyTriple(getCardsKey(MAGIC, MAGIC, bigCard, 0),
						Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(bigCard - 1, bigCard - 2)));
				specialKeyFence = 0;
			} else if (card == 0x0A) {
				specialKeyFence = 505;
				addKeyTriple(getCardsKey(card, MAGIC, MAGIC, 0), Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(card - 1, card - 2)));
				addKeyTriple(getCardsKey(MAGIC, card, MAGIC, 0), Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(card - 1, card - 2)));
				addKeyTriple(getCardsKey(MAGIC, MAGIC, card, 0), Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(card - 1, card - 2)));

				addKeyTriple(getCardsKey(bigCard, MAGIC, MAGIC, 0),
						Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(bigCard - 1, bigCard - 2)));
				addKeyTriple(getCardsKey(MAGIC, bigCard, MAGIC, 0),
						Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(bigCard - 1, bigCard - 2)));
				addKeyTriple(getCardsKey(MAGIC, MAGIC, bigCard, 0),
						Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(bigCard - 1, bigCard - 2)));
				specialKeyFence = 0;
			} else {
				specialKeyFence = 505;
				addKeyTriple(getCardsKey(card, MAGIC, MAGIC, 0), Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(card - 1, card - 2)));
				addKeyTriple(getCardsKey(MAGIC, card, MAGIC, 0), Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(card - 1, card - 2)));
				addKeyTriple(getCardsKey(MAGIC, MAGIC, card, 0), Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(card - 1, card - 2)));

				addKeyTriple(getCardsKey(bigCard, MAGIC, MAGIC, 0),
						Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(bigCard - 1, bigCard - 2)));
				addKeyTriple(getCardsKey(MAGIC, bigCard, MAGIC, 0),
						Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(bigCard - 1, bigCard - 2)));
				addKeyTriple(getCardsKey(MAGIC, MAGIC, bigCard, 0),
						Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(bigCard - 1, bigCard - 2)));
				specialKeyFence = 0;

				specialKeyFence = 606;
				addKeyTriple(getCardsKey(card, MAGIC, MAGIC, 0), Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(card - 1, card + 1)));
				addKeyTriple(getCardsKey(MAGIC, card, MAGIC, 0), Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(card - 1, card + 1)));
				addKeyTriple(getCardsKey(MAGIC, MAGIC, card, 0), Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(card - 1, card + 1)));

				addKeyTriple(getCardsKey(bigCard, MAGIC, MAGIC, 0),
						Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(bigCard - 1, bigCard + 1)));
				addKeyTriple(getCardsKey(MAGIC, bigCard, MAGIC, 0),
						Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(bigCard - 1, bigCard + 1)));
				addKeyTriple(getCardsKey(MAGIC, MAGIC, bigCard, 0),
						Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(bigCard - 1, bigCard + 1)));
				specialKeyFence = 0;

				specialKeyFence = 707;
				addKeyTriple(getCardsKey(card, MAGIC, MAGIC, 0), Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(card + 1, card + 2)));
				addKeyTriple(getCardsKey(MAGIC, card, MAGIC, 0), Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(card + 1, card + 2)));
				addKeyTriple(getCardsKey(MAGIC, MAGIC, card, 0), Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(card + 1, card + 2)));

				addKeyTriple(getCardsKey(bigCard, MAGIC, MAGIC, 0),
						Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(bigCard + 1, bigCard + 2)));
				addKeyTriple(getCardsKey(MAGIC, bigCard, MAGIC, 0),
						Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(bigCard + 1, bigCard + 2)));
				addKeyTriple(getCardsKey(MAGIC, MAGIC, bigCard, 0),
						Triple.of(2 * huXi, GameConstants.WIK_LEFT, Arrays.asList(bigCard + 1, bigCard + 2)));
				specialKeyFence = 0;
			}
		}
	}

	private void addDuiZiKeyTriple() {
		for (int card = 1; card <= 10; card++) {
			int bigCard = card + 0x10;
			addKeyTriple(getCardsKey(card, card, 0, 0), Triple.of(0, GameConstants.WIK_DUI_ZI, Arrays.asList()));
			addKeyTriple(getCardsKey(bigCard, bigCard, 0, 0), Triple.of(0, GameConstants.WIK_DUI_ZI, Arrays.asList()));

			addKeyTriple(getCardsKey(MAGIC, bigCard, 0, 0), Triple.of(0, GameConstants.WIK_DUI_ZI, Arrays.asList(bigCard)));
			addKeyTriple(getCardsKey(bigCard, MAGIC, 0, 0), Triple.of(0, GameConstants.WIK_DUI_ZI, Arrays.asList(bigCard)));

			addKeyTriple(getCardsKey(MAGIC, card, 0, 0), Triple.of(0, GameConstants.WIK_DUI_ZI, Arrays.asList(card)));
			addKeyTriple(getCardsKey(card, MAGIC, 0, 0), Triple.of(0, GameConstants.WIK_DUI_ZI, Arrays.asList(card)));
		}

		addKeyTriple(getCardsKey(MAGIC, MAGIC, 0, 0), Triple.of(0, GameConstants.WIK_DUI_ZI, Arrays.asList(MAGIC, MAGIC)));
	}

	private static Triple<Integer, Integer, List<Integer>> getTriple(long key) {
		if (keyTripleMap.containsKey(key)) {
			return keyTripleMap.get(key);
		}
		return Triple.of(-1, -1, new ArrayList<Integer>());
	}

	private Triple<Integer, Integer, List<Integer>> getTriple(int card_1, int card_2, int card_3, int card_4) {
		long key = getCardsKey(card_1, card_2, card_3, card_4);
		return getTriple(key);
	}

	public int getMingTang(int hongPaiCount) {
		int mingTang = 1;

		if (hasZiMo)
			mingTang *= 2;
		if (hasWangDiao)
			mingTang *= 4;
		if (hasWangDiaoWang)
			mingTang *= 8;
		if (hasWangChuang)
			mingTang *= 8;
		if (hasWangChuangWang)
			mingTang *= 16;
		if (hasWangZha)
			mingTang *= 16;
		if (hasWangZhaWang)
			mingTang *= 32;

		if (hongPaiCount == 0) {
			mingTang *= 4;
		} else if (hongPaiCount == 1) {
			mingTang *= 3;
		} else if (hongPaiCount >= 10) {
			if (hasHongZhuanHei) {
				if (hongPaiCount >= 13 && hongPaiCount <= 15) {
					mingTang *= 3;
				} else if (hongPaiCount > 15) {
					mingTang *= 4;
				} else {
					mingTang *= 2;
				}
			} else {
				mingTang *= 2;
			}
		}

		return mingTang;
	}

	public void change_weave_items_cards_index(WeaveItem[] weaveItems, int weaveCount, int[] handCardCounter) {
		for (int i = 0; i < weaveCount; i++) {
			switch (weaveItems[i].weave_kind) {
			case GameConstants.WIK_TI_LONG:
			case GameConstants.WIK_AN_LONG:
			case GameConstants.WIK_PAO:
				int card = weaveItems[i].center_card;
				int index = HandCardInfo.getCardIndex(card);
				handCardCounter[index] += 4;
				continue;
			case GameConstants.WIK_SAO:
			case GameConstants.WIK_PENG:
			case GameConstants.WIK_KAN:
			case GameConstants.WIK_WEI:
			case GameConstants.WIK_CHOU_WEI:
				card = weaveItems[i].center_card;
				index = HandCardInfo.getCardIndex(card);
				handCardCounter[index] += 3;
				continue;
			case GameConstants.WIK_LEFT:
				for (int j = 0; j < 3; j++) {
					card = weaveItems[i].center_card + j;
					index = HandCardInfo.getCardIndex(card);
					handCardCounter[index] += 1;
				}
				continue;
			case GameConstants.WIK_CENTER:
				for (int j = 0; j < 3; j++) {
					card = weaveItems[i].center_card + j - 1;
					index = HandCardInfo.getCardIndex(card);
					handCardCounter[index] += 1;
				}
				continue;
			case GameConstants.WIK_RIGHT:
				for (int j = 0; j < 3; j++) {
					card = weaveItems[i].center_card - j;
					index = HandCardInfo.getCardIndex(card);
					handCardCounter[index] += 1;
				}
				continue;
			case GameConstants.WIK_XXD:
				card = weaveItems[i].center_card;
				index = HandCardInfo.getCardIndex(card);
				if (index >= 10) {
					handCardCounter[index] += 1;
					handCardCounter[index - 10] += 2;
				} else {
					handCardCounter[index] += 2;
					handCardCounter[index + 10] += 1;
				}
				continue;
			case GameConstants.WIK_DDX:
				card = weaveItems[i].center_card;
				index = HandCardInfo.getCardIndex(card);
				if (index >= 10) {
					handCardCounter[index] += 2;
					handCardCounter[index - 10] += 1;
				} else {
					handCardCounter[index] += 1;
					handCardCounter[index + 10] += 2;
				}
				continue;
			case GameConstants.WIK_EQS:
				card = weaveItems[i].center_card;
				if (card > 0x10) {
					handCardCounter[11] += 1;
					handCardCounter[16] += 1;
					handCardCounter[19] += 1;
				} else {
					handCardCounter[1] += 1;
					handCardCounter[6] += 1;
					handCardCounter[9] += 1;
				}
				continue;
			case GameConstants.WIK_DUI_ZI:
				card = weaveItems[i].center_card;
				index = HandCardInfo.getCardIndex(card);
				handCardCounter[index] += 2;
				continue;
			}
		}
	}

	public int getTotalScore(Solution solution, HandCardInfo handCardInfo) {
		solution.magicToRealCard.clear();

		int totalScore = 0;

		int tunShu = (solution.totalHuXi - scoreFence) / 3 + 2;

		int handMagicCount = 0;
		int[] handCardCounter = new int[21];

		List<Integer> magicToRealCard = new ArrayList<>();
		for (WeaveInfo weaveInfo : solution.weaveInfoList) {
			if (weaveInfo.triple.getThird().size() != 0) {
				for (int card : weaveInfo.triple.getThird()) {
					magicToRealCard.add(card);
					handMagicCount++;
					int index = HandCardInfo.getCardIndex(card);
					handCardCounter[index]++;
				}
			}
		}

		if (addedCards != null) {
			for (int card : addedCards) {
				if (card == MAGIC)
					handMagicCount++;
			}
			if (addedCards.get(0) != MAGIC) {
				if (addedCards.size() == 1) {
					magicToRealCard.add(addedCards.get(0));
					int index = HandCardInfo.getCardIndex(addedCards.get(0));
					handCardCounter[index]++;
				} else {
					for (int i = 1; i < addedCards.size(); i++) {
						magicToRealCard.add(addedCards.get(0));
						int index = HandCardInfo.getCardIndex(addedCards.get(0));
						handCardCounter[index]++;
					}
				}
			} else {
				for (int i = 0; i < addedCards.size(); i++) {
					magicToRealCard.add(MAGIC);
				}
			}
		}

		if (weiAddedCard != 0) {
			magicToRealCard.add(weiAddedCard);
			int index = HandCardInfo.getCardIndex(weiAddedCard);
			handCardCounter[index]++;
		}

		int xingPaiCount = 0;
		int hongPaiCount = 0;
		int magicToMagicCount = 0;

		for (int card : handCardInfo.handCards) {
			if (card == MAGIC)
				continue;
			if (card == xingPai)
				xingPaiCount++;
			if (isHongPai(card))
				hongPaiCount++;
			int index = HandCardInfo.getCardIndex(card);
			handCardCounter[index]++;
		}

		if (addedCards != null) {
			for (int card : addedCards) {
				if (card == MAGIC)
					continue;
				if (card == xingPai)
					xingPaiCount++;
				if (isHongPai(card))
					hongPaiCount++;
			}
		}

		hongPaiCount += getHongPaiCountNew(weaveItems, weaveCount, handCardCounter);

		for (int card : magicToRealCard) {
			if (card == MAGIC) {
				magicToMagicCount++;
			}
		}

		if (xingPai != MAGIC && !qiShouGenXing) {
			if (magicToMagicCount > 0) {
				int tmpType = 0;

				int tmpMaxScore = 0;
				int tmpXingPaiCount = xingPaiCount;
				int tmpHongPaiCount = hongPaiCount;

				for (int card : magicToRealCard) {
					if (card == MAGIC) {
						if (xingPai == 0x1A)
							tmpXingPaiCount++;
						tmpHongPaiCount++;
					}
					if (card == xingPai)
						tmpXingPaiCount++;
					if (isHongPai(card))
						tmpHongPaiCount++;
				}

				int mingTang = getMingTang(tmpHongPaiCount);
				if (mingTang >= qiHuFan)
					tmpMaxScore = (tunShu + tmpXingPaiCount) * mingTang;
				else
					tmpMaxScore = 0;

				if (tmpMaxScore > totalScore) {
					totalScore = tmpMaxScore;
					tmpType = 1;
				}

				tmpMaxScore = 0;
				tmpXingPaiCount = xingPaiCount;
				tmpHongPaiCount = hongPaiCount;

				for (int card : magicToRealCard) {
					if (card == MAGIC) {
						if (xingPai == 0x19)
							tmpXingPaiCount++;
					}
					if (card == xingPai)
						tmpXingPaiCount++;
					if (isHongPai(card))
						tmpHongPaiCount++;
				}

				mingTang = getMingTang(tmpHongPaiCount);
				if (mingTang >= qiHuFan)
					tmpMaxScore = (tunShu + tmpXingPaiCount) * mingTang;
				else
					tmpMaxScore = 0;

				if (tmpMaxScore > totalScore) {
					totalScore = tmpMaxScore;
					tmpType = 2;
				}

				tmpMaxScore = 0;
				tmpXingPaiCount = xingPaiCount;
				tmpHongPaiCount = hongPaiCount;

				for (int card : magicToRealCard) {
					if (card == MAGIC) {
						tmpXingPaiCount++;
						if (isHongPai(xingPai))
							tmpHongPaiCount++;
					}
					if (card == xingPai)
						tmpXingPaiCount++;
					if (isHongPai(card))
						tmpHongPaiCount++;
				}

				mingTang = getMingTang(tmpHongPaiCount);
				if (mingTang >= qiHuFan) {
					if (magicToMagicCount >= 3) {
						if (xingPai < 0x10) {
							if (solution.totalHuXi - 3 >= scoreFence) {
								// 如果是王闯王这些牌型，并且醒牌是小字牌，胡息够的时候，需要先减掉1囤再计算总分
								tmpMaxScore = (tunShu - 1 + tmpXingPaiCount) * mingTang;
							} else {
								tmpMaxScore = 0;
							}
						} else {
							tmpMaxScore = (tunShu + tmpXingPaiCount) * mingTang;
						}
					} else {
						tmpMaxScore = (tunShu + tmpXingPaiCount) * mingTang;
					}
				} else {
					tmpMaxScore = 0;
				}

				if (tmpMaxScore > totalScore) {
					if (magicToMagicCount >= 3) {
						if (xingPai < 0x10 && solution.totalHuXi - 3 >= scoreFence) {
							totalScore = tmpMaxScore;
							tmpType = 3;
							solution.smallCardTypeMoreScore = true;
						} else if (xingPai > 0x10) {
							totalScore = tmpMaxScore;
							tmpType = 3;
						}
					} else {
						totalScore = tmpMaxScore;
						tmpType = 3;
						if (xingPai < 0x10) {
							solution.smallCardTypeMoreScore = true;
						}
					}
				}

				if (tmpType == 1) {
					for (int card : magicToRealCard) {
						if (card == MAGIC) {
							solution.magicToRealCard.add(0x1A);
						} else {
							solution.magicToRealCard.add(card);
						}
					}
				} else if (tmpType == 2) {
					for (int card : magicToRealCard) {
						if (card == MAGIC) {
							solution.magicToRealCard.add(0x19);
						} else {
							solution.magicToRealCard.add(card);
						}
					}
				} else if (tmpType == 3) {
					for (int card : magicToRealCard) {
						if (card == MAGIC) {
							solution.magicToRealCard.add(xingPai);
						} else {
							solution.magicToRealCard.add(card);
						}
					}
				}
			} else {
				for (int card : magicToRealCard) {
					if (card == xingPai)
						xingPaiCount++;
					if (isHongPai(card))
						hongPaiCount++;
					solution.magicToRealCard.add(card);
				}

				int mingTang = getMingTang(hongPaiCount);
				if (mingTang >= qiHuFan)
					totalScore = (tunShu + xingPaiCount) * mingTang;
				else
					totalScore = 0;
			}
		} else {
			if (magicToMagicCount > 0) {
				int finalIndex = 0;
				int finalCard = 0;
				int finalScore = 0;

				for (int i = 0; i < 20; i++) {
					if (magicToMagicCount >= 3 && i < 10 && solution.totalHuXi - 3 < scoreFence)
						continue;

					int realCard = HandCardInfo.getCardByIndex(i);

					int tmpHongPaiCount = hongPaiCount;
					int tmpXingPaiCount = xingPaiCount + handCardCounter[i] + magicToMagicCount;

					for (int card : magicToRealCard) {
						if (card == MAGIC)
							card = realCard;
						if (isHongPai(card))
							tmpHongPaiCount++;
					}

					int mingTang = getMingTang(tmpHongPaiCount);

					if (mingTang >= qiHuFan) {
						int tScore = (tunShu + tmpXingPaiCount) * mingTang;
						if (magicToMagicCount >= 3 && i < 10) {
							// 如果是王闯王这些牌型，并且当前的索引是小字牌的索引，囤数需要减1之后再计算总分
							tScore = (tunShu - 1 + tmpXingPaiCount) * mingTang;
						}

						if (tScore > finalScore) {
							finalIndex = i;
							finalCard = realCard;
							finalScore = tScore;
						}
					}
				}

				if (finalIndex < 10)
					solution.smallCardTypeMoreScore = true;

				xingPaiCount = xingPaiCount + handCardCounter[finalIndex] + magicToMagicCount;

				for (int card : magicToRealCard) {
					if (card == MAGIC)
						card = finalCard;
					if (isHongPai(card))
						hongPaiCount++;
					solution.magicToRealCard.add(card);
				}

				int mingTang = getMingTang(hongPaiCount);
				if (mingTang >= qiHuFan)
					totalScore = finalScore;
				else
					totalScore = 0;
			} else {
				if (handMagicCount == 0 || qiShouGenXing || hasRuleFanXing) {
					xingPaiCount = max_type_counter(handCardCounter);
				} else {
					int tmpResult = 0;
					for (int card : magicToRealCard) {
						int index = HandCardInfo.getCardIndex(card);
						if (handCardCounter[index] > tmpResult)
							tmpResult = handCardCounter[index];
					}
					xingPaiCount = tmpResult;
				}

				for (int card : magicToRealCard) {
					if (isHongPai(card))
						hongPaiCount++;
					solution.magicToRealCard.add(card);
				}

				int mingTang = getMingTang(hongPaiCount);
				if (mingTang >= qiHuFan)
					totalScore = (tunShu + xingPaiCount) * mingTang;
				else
					totalScore = 0;
			}
		}

		return totalScore;
	}

	public int max_type_counter(int[] handCardCounter) {
		int result = 0;
		for (int i = 0; i < 20; i++) {
			if (handCardCounter[i] > result)
				result = handCardCounter[i];
		}
		return result;
	}

	public static int getCardValue(int card) {
		return card & 0x0F;
	}

	public static int getHongPaiCountNew(WeaveItem[] weaveItems, int weaveCount, int[] handCardCounter) {
		int count = 0;
		for (int i = 0; i < weaveCount; i++) {
			switch (weaveItems[i].weave_kind) {
			case GameConstants.WIK_TI_LONG:
			case GameConstants.WIK_AN_LONG:
			case GameConstants.WIK_PAO:
				int card = weaveItems[i].center_card;
				int index = HandCardInfo.getCardIndex(card);
				handCardCounter[index] += 4;
				if (isHongPai(card)) {
					count += 4;
				}
				continue;
			case GameConstants.WIK_SAO:
			case GameConstants.WIK_PENG:
			case GameConstants.WIK_KAN:
			case GameConstants.WIK_WEI:
			case GameConstants.WIK_CHOU_WEI:
				card = weaveItems[i].center_card;
				index = HandCardInfo.getCardIndex(card);
				handCardCounter[index] += 3;
				if (isHongPai(card)) {
					count += 3;
				}
				continue;
			case GameConstants.WIK_LEFT:
				for (int j = 0; j < 3; j++) {
					card = weaveItems[i].center_card + j;
					index = HandCardInfo.getCardIndex(card);
					handCardCounter[index] += 1;
					if (isHongPai(weaveItems[i].center_card + j))
						count += 1;
				}
				continue;
			case GameConstants.WIK_CENTER:
				for (int j = 0; j < 3; j++) {
					card = weaveItems[i].center_card + j - 1;
					index = HandCardInfo.getCardIndex(card);
					handCardCounter[index] += 1;
					if (isHongPai(weaveItems[i].center_card + j - 1))
						count += 1;
				}
				continue;
			case GameConstants.WIK_RIGHT:
				for (int j = 0; j < 3; j++) {
					card = weaveItems[i].center_card - j;
					index = HandCardInfo.getCardIndex(card);
					handCardCounter[index] += 1;
					if (isHongPai(weaveItems[i].center_card - j))
						count += 1;
				}
				continue;
			case GameConstants.WIK_XXD:
				card = weaveItems[i].center_card;
				index = HandCardInfo.getCardIndex(card);
				if (index >= 10) {
					handCardCounter[index] += 1;
					handCardCounter[index - 10] += 2;
				} else {
					handCardCounter[index] += 2;
					handCardCounter[index + 10] += 1;
				}
				if (isHongPai(weaveItems[i].center_card))
					count += 3;
				continue;
			case GameConstants.WIK_DDX:
				card = weaveItems[i].center_card;
				index = HandCardInfo.getCardIndex(card);
				if (index >= 10) {
					handCardCounter[index] += 2;
					handCardCounter[index - 10] += 1;
				} else {
					handCardCounter[index] += 1;
					handCardCounter[index + 10] += 2;
				}
				if (isHongPai(weaveItems[i].center_card))
					count += 3;
				continue;
			case GameConstants.WIK_EQS:
				card = weaveItems[i].center_card;
				if (card > 0x10) {
					handCardCounter[11] += 1;
					handCardCounter[16] += 1;
					handCardCounter[19] += 1;
				} else {
					handCardCounter[1] += 1;
					handCardCounter[6] += 1;
					handCardCounter[9] += 1;
				}
				if (isHongPai(weaveItems[i].center_card))
					count += 3;
				continue;
			case GameConstants.WIK_DUI_ZI:
				card = weaveItems[i].center_card;
				index = HandCardInfo.getCardIndex(card);
				handCardCounter[index] += 2;
				if (isHongPai(weaveItems[i].center_card))
					count += 2;
				continue;
			}
		}

		return count;
	}

	public static int getHongPaiCount(WeaveItem[] weaveItems, int weaveCount) {
		int count = 0;
		for (int i = 0; i < weaveCount; i++) {
			switch (weaveItems[i].weave_kind) {
			case GameConstants.WIK_TI_LONG:
			case GameConstants.WIK_AN_LONG:
			case GameConstants.WIK_PAO:
				if (isHongPai(weaveItems[i].center_card))
					count += 4;
				continue;
			case GameConstants.WIK_SAO:
			case GameConstants.WIK_PENG:
			case GameConstants.WIK_KAN:
			case GameConstants.WIK_WEI:
			case GameConstants.WIK_CHOU_WEI:
				if (isHongPai(weaveItems[i].center_card))
					count += 3;
				continue;
			case GameConstants.WIK_LEFT:
				for (int j = 0; j < 3; j++) {
					if (isHongPai(weaveItems[i].center_card + j))
						count += 1;
				}
				continue;
			case GameConstants.WIK_CENTER:
				for (int j = 0; j < 3; j++) {
					if (isHongPai(weaveItems[i].center_card + j - 1))
						count += 1;
				}
				continue;
			case GameConstants.WIK_RIGHT:
				for (int j = 0; j < 3; j++) {
					if (isHongPai(weaveItems[i].center_card - j))
						count += 1;
				}
				continue;
			case GameConstants.WIK_XXD:
			case GameConstants.WIK_DDX:
			case GameConstants.WIK_EQS:
				if (isHongPai(weaveItems[i].center_card))
					count += 3;
				continue;
			case GameConstants.WIK_DUI_ZI:
				if (isHongPai(weaveItems[i].center_card))
					count += 2;
				continue;
			}
		}

		return count;
	}

	public static boolean isHongPai(int card) {
		if (card == 0x02 || card == 0x07 || card == 0x0A || card == 0x12 || card == 0x17 || card == 0x1A) {
			return true;
		}
		return false;
	}

	private void checkStatus(Solution solution, HandCardInfo handCardInfo) {
		int tmpScore = getTotalScore(solution, handCardInfo);
		int neededWeaveCount = 7;
		if (hasWangDiao || hasWangDiaoWang || hasWangChuang || hasWangChuangWang || hasWangZha || hasWangZhaWang) {
			neededWeaveCount = 6;
		}
		if ((solution.weaveInfoList.size() + weaveCount) == neededWeaveCount && solution.totalHuXi >= scoreFence
				&& tmpScore > bestSolution.totalScore) {
			bestSolution = new Solution(solution);
			bestSolution.totalScore = tmpScore;
			hasFoundBestSolution = true;
		}
	}

	private void analyseWin(final HandCardInfo handCardInfo, final Solution solution, final int lastCardIndex) {
		List<Integer> remainCardPositions = handCardInfo.getRemainCardPositions();

		int remainCount = remainCardPositions.size();

		if (remainCount == 0) {
			checkStatus(solution, handCardInfo);
		} else if (remainCount == 1) {
		} else if (remainCount == 3) {
			int card_1 = handCardInfo.getCard(remainCardPositions.get(0));
			int card_2 = handCardInfo.getCard(remainCardPositions.get(1));
			int card_3 = handCardInfo.getCard(remainCardPositions.get(2));

			Triple<Integer, Integer, List<Integer>> triple = getTriple(card_1, card_2, card_3, 0);

			if (card_1 == card_2 && card_1 == card_3 && card_1 != MAGIC && lastCardIndex == HandCardInfo.getCardIndex(card_1)) {
				int tmpI = triple.getFirst();
				if (tmpI == 6) {
					triple = Triple.of(3, GameConstants.WIK_KAN, Arrays.asList());
				} else {
					triple = Triple.of(1, GameConstants.WIK_KAN, Arrays.asList());
				}
			}
			if (triple.getFirst() >= 0) {
				Solution tmpSolution = new Solution(solution);

				WeaveInfo weaveInfo = new WeaveInfo(triple, remainCardPositions.get(0), remainCardPositions.get(1), remainCardPositions.get(2), -1);
				tmpSolution.pushWeaveInfo(weaveInfo, null);

				checkStatus(tmpSolution, handCardInfo);
			}

			specialKeyFence = 101;
			triple = getTriple(card_1, card_2, card_3, 0);
			specialKeyFence = 0;
			if (triple.getFirst() >= 0) {
				Solution tmpSolution = new Solution(solution);

				WeaveInfo weaveInfo = new WeaveInfo(triple, remainCardPositions.get(0), remainCardPositions.get(1), remainCardPositions.get(2), -1);
				tmpSolution.pushWeaveInfo(weaveInfo, null);

				checkStatus(tmpSolution, handCardInfo);
			}

			if (card_1 == MAGIC && card_2 == MAGIC && card_3 == MAGIC) {
				specialKeyFence = 303;
				triple = getTriple(card_1, card_2, card_3, 0);
				specialKeyFence = 0;
				if (triple.getFirst() >= 0) {
					Solution tmpSolution = new Solution(solution);

					WeaveInfo weaveInfo = new WeaveInfo(triple, remainCardPositions.get(0), remainCardPositions.get(1), remainCardPositions.get(2),
							-1);
					tmpSolution.pushWeaveInfo(weaveInfo, null);

					checkStatus(tmpSolution, handCardInfo);
				}

				specialKeyFence = 404;
				triple = getTriple(card_1, card_2, card_3, 0);
				specialKeyFence = 0;
				if (triple.getFirst() >= 0) {
					Solution tmpSolution = new Solution(solution);

					WeaveInfo weaveInfo = new WeaveInfo(triple, remainCardPositions.get(0), remainCardPositions.get(1), remainCardPositions.get(2),
							-1);
					tmpSolution.pushWeaveInfo(weaveInfo, null);

					checkStatus(tmpSolution, handCardInfo);
				}
			} else if ((card_1 == MAGIC && card_2 == MAGIC) || (card_1 == MAGIC && card_3 == MAGIC) || (card_2 == MAGIC && card_3 == MAGIC)) {
				specialKeyFence = 202;
				triple = getTriple(card_1, card_2, card_3, 0);
				specialKeyFence = 0;
				if (triple.getFirst() >= 0) {
					Solution tmpSolution = new Solution(solution);

					WeaveInfo weaveInfo = new WeaveInfo(triple, remainCardPositions.get(0), remainCardPositions.get(1), remainCardPositions.get(2),
							-1);
					tmpSolution.pushWeaveInfo(weaveInfo, null);

					checkStatus(tmpSolution, handCardInfo);
				}

				specialKeyFence = 505;
				triple = getTriple(card_1, card_2, card_3, 0);
				specialKeyFence = 0;
				if (triple.getFirst() >= 0) {
					Solution tmpSolution = new Solution(solution);

					WeaveInfo weaveInfo = new WeaveInfo(triple, remainCardPositions.get(0), remainCardPositions.get(1), remainCardPositions.get(2),
							-1);
					tmpSolution.pushWeaveInfo(weaveInfo, null);

					checkStatus(tmpSolution, handCardInfo);
				}

				specialKeyFence = 606;
				triple = getTriple(card_1, card_2, card_3, 0);
				specialKeyFence = 0;
				if (triple.getFirst() >= 0) {
					Solution tmpSolution = new Solution(solution);

					WeaveInfo weaveInfo = new WeaveInfo(triple, remainCardPositions.get(0), remainCardPositions.get(1), remainCardPositions.get(2),
							-1);
					tmpSolution.pushWeaveInfo(weaveInfo, null);

					checkStatus(tmpSolution, handCardInfo);
				}

				specialKeyFence = 707;
				triple = getTriple(card_1, card_2, card_3, 0);
				specialKeyFence = 0;
				if (triple.getFirst() >= 0) {
					Solution tmpSolution = new Solution(solution);

					WeaveInfo weaveInfo = new WeaveInfo(triple, remainCardPositions.get(0), remainCardPositions.get(1), remainCardPositions.get(2),
							-1);
					tmpSolution.pushWeaveInfo(weaveInfo, null);

					checkStatus(tmpSolution, handCardInfo);
				}
			}
		} else if (remainCount == 2) {
			if (getWangDiao() == false) {
				int card_1 = handCardInfo.getCard(remainCardPositions.get(0));
				int card_2 = handCardInfo.getCard(remainCardPositions.get(1));

				Triple<Integer, Integer, List<Integer>> triple = getTriple(card_1, card_2, 0, 0);
				if (triple.getFirst() >= 0) {
					Solution tmpSolution = new Solution(solution);

					WeaveInfo weaveInfo = new WeaveInfo(triple, remainCardPositions.get(0), remainCardPositions.get(1), -1, -1);
					tmpSolution.pushWeaveInfo(weaveInfo, null);

					checkStatus(tmpSolution, handCardInfo);
				}
			}
		} else {
			int[] indexToCard = new int[] { //
					0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, //
					0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1A, //
					0xff, //
			};

			List<Integer> magicCardPositions = handCardInfo.getCardPositionsByCard(MAGIC);
			int magicCount = magicCardPositions.size();

			// Step1.尝试12或9胡
			if (magicCount == 4) {
				// 如果有四个王
				WeaveInfo weaveInfo = new WeaveInfo(Triple.of(12, GameConstants.WIK_TI_LONG, Arrays.asList(0xFF, 0xFF, 0xFF, 0xFF)),
						magicCardPositions.get(0), magicCardPositions.get(1), magicCardPositions.get(2), magicCardPositions.get(3));

				// 递归回溯
				solution.pushWeaveInfo(weaveInfo, handCardInfo);
				analyseWin(handCardInfo, solution, lastCardIndex);
				solution.popWeaveInfo(handCardInfo);
			}

			// 注意这里只能用if 而不是 else if
			if (magicCount >= 3) {
				// 三个王带一个
				for (int i = 19; i >= 0; i--) {
					List<Integer> cardPositions = handCardInfo.getCardPositionsByCard(indexToCard[i]);
					int count = cardPositions.size();
					if ((count > 0 && count < 3) || (count == 3 && i == lastCardIndex)) {
						// 牌数必须为1张或2张 不能为3张 或者是最后一张牌是三张
						int huXi = i >= 10 ? 12 : 9;
						int card = handCardInfo.getCard(cardPositions.get(0));
						WeaveInfo weaveInfo = new WeaveInfo(Triple.of(huXi, GameConstants.WIK_TI_LONG, Arrays.asList(card, card, card)),
								magicCardPositions.get(0), magicCardPositions.get(1), magicCardPositions.get(2), cardPositions.get(0));

						// 递归回溯
						solution.pushWeaveInfo(weaveInfo, handCardInfo);
						analyseWin(handCardInfo, solution, lastCardIndex);
						solution.popWeaveInfo(handCardInfo);
					}
				}
			}

			if (magicCount >= 2) {
				// 两个王带两个
				for (int i = 19; i >= 0; i--) {
					List<Integer> cardPositions = handCardInfo.getCardPositionsByCard(indexToCard[i]);
					int count = cardPositions.size();
					if (count == 2 || (count == 3 && i == lastCardIndex)) {
						// 牌数必须为2张 或者是最后一张牌是三张
						int huXi = i >= 10 ? 12 : 9;
						int card = handCardInfo.getCard(cardPositions.get(0));
						WeaveInfo weaveInfo = new WeaveInfo(Triple.of(huXi, GameConstants.WIK_TI_LONG, Arrays.asList(card, card)),
								magicCardPositions.get(0), magicCardPositions.get(1), cardPositions.get(0), cardPositions.get(1));

						// 递归回溯
						solution.pushWeaveInfo(weaveInfo, handCardInfo);
						analyseWin(handCardInfo, solution, lastCardIndex);
						solution.popWeaveInfo(handCardInfo);
					}
				}
			}

			if (magicCount >= 1) {
				// 一个王带三个
				for (int i = 19; i >= 0; i--) {
					List<Integer> cardPositions = handCardInfo.getCardPositionsByCard(indexToCard[i]);
					int count = cardPositions.size();
					if (count == 3) {
						// 牌数必须为3张
						int huXi = i >= 10 ? 12 : 9;
						int card = handCardInfo.getCard(cardPositions.get(0));
						WeaveInfo weaveInfo = new WeaveInfo(Triple.of(huXi, GameConstants.WIK_TI_LONG, Arrays.asList(card)),
								magicCardPositions.get(0), cardPositions.get(0), cardPositions.get(1), cardPositions.get(2));

						// 递归回溯
						solution.pushWeaveInfo(weaveInfo, handCardInfo);
						analyseWin(handCardInfo, solution, lastCardIndex);
						solution.popWeaveInfo(handCardInfo);
					}
				}
			}

			// Step2.尝试6或3胡
			if (magicCount >= 3) {
				// 三个王
				WeaveInfo weaveInfo = new WeaveInfo(Triple.of(6, GameConstants.WIK_KAN, Arrays.asList(0xFF, 0xFF, 0xFF)), magicCardPositions.get(0),
						magicCardPositions.get(1), magicCardPositions.get(2), -1);

				// 递归回溯
				solution.pushWeaveInfo(weaveInfo, handCardInfo);
				analyseWin(handCardInfo, solution, lastCardIndex);
				solution.popWeaveInfo(handCardInfo);
			}

			if (magicCount >= 2) {
				// 两个王带一个
				for (int i = 19; i >= 0; i--) {
					List<Integer> cardPositions = handCardInfo.getCardPositionsByCard(indexToCard[i]);
					int count = cardPositions.size();
					if ((count > 0 && count < 3) || (count == 3 && i == lastCardIndex)) {
						// 牌数必须为1张或2张 不能为3张 或者是最后一张牌是三张
						int huXi = i >= 10 ? 6 : 3;
						int card = handCardInfo.getCard(cardPositions.get(0));
						WeaveInfo weaveInfo = new WeaveInfo(Triple.of(huXi, GameConstants.WIK_KAN, Arrays.asList(card, card)),
								magicCardPositions.get(0), magicCardPositions.get(1), cardPositions.get(0), -1);

						// 递归回溯
						solution.pushWeaveInfo(weaveInfo, handCardInfo);
						analyseWin(handCardInfo, solution, lastCardIndex);
						solution.popWeaveInfo(handCardInfo);
					}
				}
			}

			if (magicCount >= 1) {
				// 一个王带两个
				for (int i = 19; i >= 0; i--) {
					List<Integer> cardPositions = handCardInfo.getCardPositionsByCard(indexToCard[i]);
					int count = cardPositions.size();
					if (count == 2 || (count == 3 && i == lastCardIndex)) {
						// 牌数必须为2张
						int huXi = i >= 10 ? 6 : 3;
						int card = handCardInfo.getCard(cardPositions.get(0));
						WeaveInfo weaveInfo = new WeaveInfo(Triple.of(huXi, GameConstants.WIK_KAN, Arrays.asList(card)), magicCardPositions.get(0),
								cardPositions.get(0), cardPositions.get(1), -1);

						// 递归回溯
						solution.pushWeaveInfo(weaveInfo, handCardInfo);
						analyseWin(handCardInfo, solution, lastCardIndex);
						solution.popWeaveInfo(handCardInfo);
					}
				}
			}

			{
				// 三张
				for (int i = 19; i >= 0; i--) {
					List<Integer> cardPositions = handCardInfo.getCardPositionsByCard(indexToCard[i]);
					int count = cardPositions.size();
					if (count == 3) {
						// 牌数必须为3张
						int huXi = i >= 10 ? 6 : 3;

						if (i == lastCardIndex) {
							if (huXi == 6) {
								huXi = 3;
							} else {
								huXi = 1;
							}
						}

						WeaveInfo weaveInfo = new WeaveInfo(Triple.of(huXi, GameConstants.WIK_KAN, Arrays.asList()), cardPositions.get(0),
								cardPositions.get(1), cardPositions.get(2), -1);

						// 递归回溯
						solution.pushWeaveInfo(weaveInfo, handCardInfo);
						analyseWin(handCardInfo, solution, lastCardIndex);
						solution.popWeaveInfo(handCardInfo);
					}
				}
			}

			{
				// 大一二三
				List<Integer> positions_1 = handCardInfo.getCardPositionsByCard(0x11);
				List<Integer> positions_2 = handCardInfo.getCardPositionsByCard(0x12);
				List<Integer> positions_3 = handCardInfo.getCardPositionsByCard(0x13);
				int count_1 = positions_1.size();
				int count_2 = positions_2.size();
				int count_3 = positions_3.size();

				if (count_1 > 0 && count_2 > 0 && count_3 > 0 && (count_1 < 3 || HandCardInfo.getCardIndex(0x11) == lastCardIndex)
						&& (count_2 < 3 || HandCardInfo.getCardIndex(0x12) == lastCardIndex)
						&& (count_3 < 3 || HandCardInfo.getCardIndex(0x13) == lastCardIndex)) {
					WeaveInfo weaveInfo = new WeaveInfo(Triple.of(6, GameConstants.WIK_LEFT, Arrays.asList()), positions_1.get(0), positions_2.get(0),
							positions_3.get(0), -1);

					// 递归回溯
					solution.pushWeaveInfo(weaveInfo, handCardInfo);
					analyseWin(handCardInfo, solution, lastCardIndex);
					solution.popWeaveInfo(handCardInfo);
				}

				if (magicCount >= 1) {
					if (count_1 > 0 && count_2 > 0 && (count_1 < 3 || HandCardInfo.getCardIndex(0x11) == lastCardIndex)
							&& (count_2 < 3 || HandCardInfo.getCardIndex(0x12) == lastCardIndex)) {
						WeaveInfo weaveInfo = new WeaveInfo(Triple.of(6, GameConstants.WIK_LEFT, Arrays.asList(0x13)), positions_1.get(0),
								positions_2.get(0), magicCardPositions.get(0), -1);

						// 递归回溯
						solution.pushWeaveInfo(weaveInfo, handCardInfo);
						analyseWin(handCardInfo, solution, lastCardIndex);
						solution.popWeaveInfo(handCardInfo);
					}

					if (count_2 > 0 && count_3 > 0 && (count_2 < 3 || HandCardInfo.getCardIndex(0x12) == lastCardIndex)
							&& (count_3 < 3 || HandCardInfo.getCardIndex(0x13) == lastCardIndex)) {
						WeaveInfo weaveInfo = new WeaveInfo(Triple.of(6, GameConstants.WIK_LEFT, Arrays.asList(0x11)), magicCardPositions.get(0),
								positions_2.get(0), positions_3.get(0), -1);

						// 递归回溯
						solution.pushWeaveInfo(weaveInfo, handCardInfo);
						analyseWin(handCardInfo, solution, lastCardIndex);
						solution.popWeaveInfo(handCardInfo);
					}

					if (count_1 > 0 && count_3 > 0 && (count_1 < 3 || HandCardInfo.getCardIndex(0x11) == lastCardIndex)
							&& (count_3 < 3 || HandCardInfo.getCardIndex(0x13) == lastCardIndex)) {
						WeaveInfo weaveInfo = new WeaveInfo(Triple.of(6, GameConstants.WIK_LEFT, Arrays.asList(0x12)), positions_1.get(0),
								magicCardPositions.get(0), positions_3.get(0), -1);

						// 递归回溯
						solution.pushWeaveInfo(weaveInfo, handCardInfo);
						analyseWin(handCardInfo, solution, lastCardIndex);
						solution.popWeaveInfo(handCardInfo);
					}
				}
			}

			{
				// 小一二三
				List<Integer> positions_1 = handCardInfo.getCardPositionsByCard(0x01);
				List<Integer> positions_2 = handCardInfo.getCardPositionsByCard(0x02);
				List<Integer> positions_3 = handCardInfo.getCardPositionsByCard(0x03);
				int count_1 = positions_1.size();
				int count_2 = positions_2.size();
				int count_3 = positions_3.size();

				if (count_1 > 0 && count_2 > 0 && count_3 > 0 && (count_1 < 3 || HandCardInfo.getCardIndex(0x01) == lastCardIndex)
						&& (count_2 < 3 || HandCardInfo.getCardIndex(0x02) == lastCardIndex)
						&& (count_3 < 3 || HandCardInfo.getCardIndex(0x03) == lastCardIndex)) {
					WeaveInfo weaveInfo = new WeaveInfo(Triple.of(3, GameConstants.WIK_LEFT, Arrays.asList()), positions_1.get(0), positions_2.get(0),
							positions_3.get(0), -1);

					// 递归回溯
					solution.pushWeaveInfo(weaveInfo, handCardInfo);
					analyseWin(handCardInfo, solution, lastCardIndex);
					solution.popWeaveInfo(handCardInfo);
				}

				if (magicCount >= 1) {
					if (count_1 > 0 && count_2 > 0 && (count_1 < 3 || HandCardInfo.getCardIndex(0x01) == lastCardIndex)
							&& (count_2 < 3 || HandCardInfo.getCardIndex(0x02) == lastCardIndex)) {
						WeaveInfo weaveInfo = new WeaveInfo(Triple.of(3, GameConstants.WIK_LEFT, Arrays.asList(0x03)), positions_1.get(0),
								positions_2.get(0), magicCardPositions.get(0), -1);

						// 递归回溯
						solution.pushWeaveInfo(weaveInfo, handCardInfo);
						analyseWin(handCardInfo, solution, lastCardIndex);
						solution.popWeaveInfo(handCardInfo);
					}

					if (count_2 > 0 && count_3 > 0 && (count_2 < 3 || HandCardInfo.getCardIndex(0x02) == lastCardIndex)
							&& (count_3 < 3 || HandCardInfo.getCardIndex(0x03) == lastCardIndex)) {
						WeaveInfo weaveInfo = new WeaveInfo(Triple.of(3, GameConstants.WIK_LEFT, Arrays.asList(0x01)), magicCardPositions.get(0),
								positions_2.get(0), positions_3.get(0), -1);

						// 递归回溯
						solution.pushWeaveInfo(weaveInfo, handCardInfo);
						analyseWin(handCardInfo, solution, lastCardIndex);
						solution.popWeaveInfo(handCardInfo);
					}

					if (count_1 > 0 && count_3 > 0 && (count_1 < 3 || HandCardInfo.getCardIndex(0x01) == lastCardIndex)
							&& (count_3 < 3 || HandCardInfo.getCardIndex(0x03) == lastCardIndex)) {
						WeaveInfo weaveInfo = new WeaveInfo(Triple.of(3, GameConstants.WIK_LEFT, Arrays.asList(0x02)), positions_1.get(0),
								magicCardPositions.get(0), positions_3.get(0), -1);

						// 递归回溯
						solution.pushWeaveInfo(weaveInfo, handCardInfo);
						analyseWin(handCardInfo, solution, lastCardIndex);
						solution.popWeaveInfo(handCardInfo);
					}
				}
			}

			{
				// 大二七十
				List<Integer> positions_1 = handCardInfo.getCardPositionsByCard(0x12);
				List<Integer> positions_2 = handCardInfo.getCardPositionsByCard(0x17);
				List<Integer> positions_3 = handCardInfo.getCardPositionsByCard(0x1A);
				int count_1 = positions_1.size();
				int count_2 = positions_2.size();
				int count_3 = positions_3.size();

				if (count_1 > 0 && count_2 > 0 && count_3 > 0 && (count_1 < 3 || HandCardInfo.getCardIndex(0x12) == lastCardIndex)
						&& (count_2 < 3 || HandCardInfo.getCardIndex(0x17) == lastCardIndex)
						&& (count_3 < 3 || HandCardInfo.getCardIndex(0x1A) == lastCardIndex)) {
					WeaveInfo weaveInfo = new WeaveInfo(Triple.of(6, GameConstants.WIK_EQS, Arrays.asList()), positions_1.get(0), positions_2.get(0),
							positions_3.get(0), -1);

					// 递归回溯
					solution.pushWeaveInfo(weaveInfo, handCardInfo);
					analyseWin(handCardInfo, solution, lastCardIndex);
					solution.popWeaveInfo(handCardInfo);
				}

				if (magicCount >= 1) {
					if (count_1 > 0 && count_2 > 0 && (count_1 < 3 || HandCardInfo.getCardIndex(0x12) == lastCardIndex)
							&& (count_2 < 3 || HandCardInfo.getCardIndex(0x17) == lastCardIndex)) {
						WeaveInfo weaveInfo = new WeaveInfo(Triple.of(6, GameConstants.WIK_EQS, Arrays.asList(0x1A)), positions_1.get(0),
								positions_2.get(0), magicCardPositions.get(0), -1);

						// 递归回溯
						solution.pushWeaveInfo(weaveInfo, handCardInfo);
						analyseWin(handCardInfo, solution, lastCardIndex);
						solution.popWeaveInfo(handCardInfo);
					}

					if (count_2 > 0 && count_3 > 0 && (count_2 < 3 || HandCardInfo.getCardIndex(0x17) == lastCardIndex)
							&& (count_3 < 3 || HandCardInfo.getCardIndex(0x1A) == lastCardIndex)) {
						WeaveInfo weaveInfo = new WeaveInfo(Triple.of(6, GameConstants.WIK_EQS, Arrays.asList(0x12)), magicCardPositions.get(0),
								positions_2.get(0), positions_3.get(0), -1);

						// 递归回溯
						solution.pushWeaveInfo(weaveInfo, handCardInfo);
						analyseWin(handCardInfo, solution, lastCardIndex);
						solution.popWeaveInfo(handCardInfo);
					}

					if (count_1 > 0 && count_3 > 0 && (count_1 < 3 || HandCardInfo.getCardIndex(0x12) == lastCardIndex)
							&& (count_3 < 3 || HandCardInfo.getCardIndex(0x1A) == lastCardIndex)) {
						WeaveInfo weaveInfo = new WeaveInfo(Triple.of(6, GameConstants.WIK_EQS, Arrays.asList(0x17)), positions_1.get(0),
								magicCardPositions.get(0), positions_3.get(0), -1);

						// 递归回溯
						solution.pushWeaveInfo(weaveInfo, handCardInfo);
						analyseWin(handCardInfo, solution, lastCardIndex);
						solution.popWeaveInfo(handCardInfo);
					}
				}
			}

			{
				// 小二七十
				List<Integer> positions_1 = handCardInfo.getCardPositionsByCard(0x02);
				List<Integer> positions_2 = handCardInfo.getCardPositionsByCard(0x07);
				List<Integer> positions_3 = handCardInfo.getCardPositionsByCard(0x0A);
				int count_1 = positions_1.size();
				int count_2 = positions_2.size();
				int count_3 = positions_3.size();

				if (count_1 > 0 && count_2 > 0 && count_3 > 0 && (count_1 < 3 || HandCardInfo.getCardIndex(0x02) == lastCardIndex)
						&& (count_2 < 3 || HandCardInfo.getCardIndex(0x07) == lastCardIndex)
						&& (count_3 < 3 || HandCardInfo.getCardIndex(0x0A) == lastCardIndex)) {
					WeaveInfo weaveInfo = new WeaveInfo(Triple.of(3, GameConstants.WIK_EQS, Arrays.asList()), positions_1.get(0), positions_2.get(0),
							positions_3.get(0), -1);

					// 递归回溯
					solution.pushWeaveInfo(weaveInfo, handCardInfo);
					analyseWin(handCardInfo, solution, lastCardIndex);
					solution.popWeaveInfo(handCardInfo);
				}

				if (magicCount >= 1) {
					if (count_1 > 0 && count_2 > 0 && (count_1 < 3 || HandCardInfo.getCardIndex(0x02) == lastCardIndex)
							&& (count_2 < 3 || HandCardInfo.getCardIndex(0x07) == lastCardIndex)) {
						WeaveInfo weaveInfo = new WeaveInfo(Triple.of(3, GameConstants.WIK_EQS, Arrays.asList(0x0A)), positions_1.get(0),
								positions_2.get(0), magicCardPositions.get(0), -1);

						// 递归回溯
						solution.pushWeaveInfo(weaveInfo, handCardInfo);
						analyseWin(handCardInfo, solution, lastCardIndex);
						solution.popWeaveInfo(handCardInfo);
					}

					if (count_2 > 0 && count_3 > 0 && (count_2 < 3 || HandCardInfo.getCardIndex(0x07) == lastCardIndex)
							&& (count_3 < 3 || HandCardInfo.getCardIndex(0x0A) == lastCardIndex)) {
						WeaveInfo weaveInfo = new WeaveInfo(Triple.of(3, GameConstants.WIK_EQS, Arrays.asList(0x02)), magicCardPositions.get(0),
								positions_2.get(0), positions_3.get(0), -1);

						// 递归回溯
						solution.pushWeaveInfo(weaveInfo, handCardInfo);
						analyseWin(handCardInfo, solution, lastCardIndex);
						solution.popWeaveInfo(handCardInfo);
					}

					if (count_1 > 0 && count_3 > 0 && (count_1 < 3 || HandCardInfo.getCardIndex(0x02) == lastCardIndex)
							&& (count_3 < 3 || HandCardInfo.getCardIndex(0x0A) == lastCardIndex)) {
						WeaveInfo weaveInfo = new WeaveInfo(Triple.of(3, GameConstants.WIK_EQS, Arrays.asList(0x07)), positions_1.get(0),
								magicCardPositions.get(0), positions_3.get(0), -1);

						// 递归回溯
						solution.pushWeaveInfo(weaveInfo, handCardInfo);
						analyseWin(handCardInfo, solution, lastCardIndex);
						solution.popWeaveInfo(handCardInfo);
					}
				}
			}

			if (solution.totalHuXi < scoreFence) {
				// 剪枝
				return;
			}

			remainCount = remainCardPositions.size();

			Map<Long, WeaveInfo> map = new HashMap<>();

			Set<Integer> set = new HashSet<>();

			for (int i = 0; i < remainCount; i++) {
				for (int j = i + 1; j < remainCount; j++) {
					for (int k = j + 1; k < remainCount; k++) {
						// 手牌里的 非最后翻出来组成3张的 不能拆开 也就是正常的坎 是不能拆开的
						int[] tmpPs = new int[] { remainCardPositions.get(i), remainCardPositions.get(j), remainCardPositions.get(k) };
						int card_1 = handCardInfo.getCard(tmpPs[0]);
						int card_2 = handCardInfo.getCard(tmpPs[1]);
						int card_3 = handCardInfo.getCard(tmpPs[2]);
						int card_index_1 = HandCardInfo.getCardIndex(card_1);
						int card_index_2 = HandCardInfo.getCardIndex(card_2);
						int card_index_3 = HandCardInfo.getCardIndex(card_3);
						int card_count_1 = handCardInfo.cardIndexToCardPositions[card_index_1].size();
						int card_count_2 = handCardInfo.cardIndexToCardPositions[card_index_2].size();
						int card_count_3 = handCardInfo.cardIndexToCardPositions[card_index_3].size();

						boolean can_combine = false;

						if (card_count_1 < 3 && card_count_2 < 3 && card_count_3 < 3)
							can_combine = true;

						if (!can_combine && card_1 == card_2 && card_1 == card_3 && card_index_1 == lastCardIndex) {
							// 三张一样的牌
							can_combine = true;
						}

						if (!can_combine && card_count_1 == 3 && card_index_1 == lastCardIndex) {
							// 第一张牌是三张
							if (card_1 == card_2) {
								if (card_count_3 < 3)
									can_combine = true;
							} else if (card_1 == card_3) {
								if (card_count_2 < 3)
									can_combine = true;
							} else {
								if (card_count_2 < 3 && card_count_3 < 3)
									can_combine = true;
							}
						}

						if (!can_combine && card_count_2 == 3 && card_index_2 == lastCardIndex) {
							// 第二张牌是三张
							if (card_2 == card_1) {
								if (card_count_3 < 3)
									can_combine = true;
							} else if (card_2 == card_3) {
								if (card_count_1 < 3)
									can_combine = true;
							} else {
								if (card_count_1 < 3 && card_count_3 < 3)
									can_combine = true;
							}
						}

						if (!can_combine && card_count_3 == 3 && card_index_3 == lastCardIndex) {
							// 第三张牌是三张
							if (card_3 == card_1) {
								if (card_count_2 < 3)
									can_combine = true;
							} else if (card_3 == card_2) {
								if (card_count_1 < 3)
									can_combine = true;
							} else {
								if (card_count_1 < 3 && card_count_2 < 3)
									can_combine = true;
							}
						}

						if (can_combine) {
							long key = getCardsKey(card_1, card_2, card_3, 0);
							Triple<Integer, Integer, List<Integer>> triple = getTriple(key);
							if (card_1 == card_2 && card_1 == card_3 && card_1 != MAGIC && lastCardIndex == HandCardInfo.getCardIndex(card_1)) {
								int tmpI = triple.getFirst();
								if (tmpI == 6) {
									triple = Triple.of(3, GameConstants.WIK_KAN, Arrays.asList());
								} else {
									triple = Triple.of(1, GameConstants.WIK_KAN, Arrays.asList());
								}
							}
							WeaveInfo weaveInfo = null;

							if (triple.getFirst() >= 0) {
								set.add(tmpPs[0]);
								set.add(tmpPs[1]);
								set.add(tmpPs[2]);
								weaveInfo = new WeaveInfo(triple, tmpPs[0], tmpPs[1], tmpPs[2], -1);
								map.put(key, weaveInfo);
							}

							specialKeyFence = 101;
							key = getCardsKey(card_1, card_2, card_3, 0);
							specialKeyFence = 0;
							triple = getTriple(key);
							if (triple.getFirst() >= 0) {
								set.add(tmpPs[0]);
								set.add(tmpPs[1]);
								set.add(tmpPs[2]);
								weaveInfo = new WeaveInfo(triple, tmpPs[0], tmpPs[1], tmpPs[2], -1);
								map.put(key, weaveInfo);
							}

							if (card_1 == MAGIC && card_2 == MAGIC && card_3 == MAGIC) {
								specialKeyFence = 303;
								key = getCardsKey(card_1, card_2, card_3, 0);
								specialKeyFence = 0;
								triple = getTriple(key);
								if (triple.getFirst() >= 0) {
									set.add(tmpPs[0]);
									set.add(tmpPs[1]);
									set.add(tmpPs[2]);
									weaveInfo = new WeaveInfo(triple, tmpPs[0], tmpPs[1], tmpPs[2], -1);
									map.put(key, weaveInfo);
								}

								specialKeyFence = 404;
								key = getCardsKey(card_1, card_2, card_3, 0);
								specialKeyFence = 0;
								triple = getTriple(key);
								if (triple.getFirst() >= 0) {
									set.add(tmpPs[0]);
									set.add(tmpPs[1]);
									set.add(tmpPs[2]);
									weaveInfo = new WeaveInfo(triple, tmpPs[0], tmpPs[1], tmpPs[2], -1);
									map.put(key, weaveInfo);
								}
							} else if ((card_1 == MAGIC && card_2 == MAGIC) || (card_1 == MAGIC && card_3 == MAGIC)
									|| (card_2 == MAGIC && card_3 == MAGIC)) {
								specialKeyFence = 202;
								key = getCardsKey(card_1, card_2, card_3, 0);
								specialKeyFence = 0;
								triple = getTriple(key);
								if (triple.getFirst() >= 0) {
									set.add(tmpPs[0]);
									set.add(tmpPs[1]);
									set.add(tmpPs[2]);
									weaveInfo = new WeaveInfo(triple, tmpPs[0], tmpPs[1], tmpPs[2], -1);
									map.put(key, weaveInfo);
								}

								specialKeyFence = 505;
								key = getCardsKey(card_1, card_2, card_3, 0);
								specialKeyFence = 0;
								triple = getTriple(key);
								if (triple.getFirst() >= 0) {
									set.add(tmpPs[0]);
									set.add(tmpPs[1]);
									set.add(tmpPs[2]);
									weaveInfo = new WeaveInfo(triple, tmpPs[0], tmpPs[1], tmpPs[2], -1);
									map.put(key, weaveInfo);
								}

								specialKeyFence = 606;
								key = getCardsKey(card_1, card_2, card_3, 0);
								specialKeyFence = 0;
								triple = getTriple(key);
								if (triple.getFirst() >= 0) {
									set.add(tmpPs[0]);
									set.add(tmpPs[1]);
									set.add(tmpPs[2]);
									weaveInfo = new WeaveInfo(triple, tmpPs[0], tmpPs[1], tmpPs[2], -1);
									map.put(key, weaveInfo);
								}

								specialKeyFence = 707;
								key = getCardsKey(card_1, card_2, card_3, 0);
								specialKeyFence = 0;
								triple = getTriple(key);
								if (triple.getFirst() >= 0) {
									set.add(tmpPs[0]);
									set.add(tmpPs[1]);
									set.add(tmpPs[2]);
									weaveInfo = new WeaveInfo(triple, tmpPs[0], tmpPs[1], tmpPs[2], -1);
									map.put(key, weaveInfo);
								}
							}
						}
					}
				}
			}

			if (set.size() < remainCount) {
				if (remainCount - set.size() == 2) {
					// 如果上面的取三张轮询完之后还剩2张
					if (getWangDiao() == true) {
						return;
					} else {
						int tmpCount = 0;
						int[] tmpPs = new int[] { -1, -1 };
						for (int x = 0; x < remainCount; x++) {
							if (!set.contains(remainCardPositions.get(x))) {
								tmpPs[tmpCount++] = remainCardPositions.get(x);
							}
						}

						// 如果还剩下2张
						int card_1 = handCardInfo.getCard(tmpPs[0]);
						int card_2 = handCardInfo.getCard(tmpPs[1]);

						if (card_1 != MAGIC && card_2 != MAGIC && card_1 != card_2) {
							return;
						}
					}
				} else {
					int tmpMagicCount = 0;
					for (int i = 0; i < remainCount; i++) {
						int card = handCardInfo.getCard(remainCardPositions.get(i));
						if (card == MAGIC)
							tmpMagicCount++;
					}
					if (remainCount - set.size() == 1 && tmpMagicCount > 0) {
					} else {
						// 如果上面的取三张轮询完之后还剩1张或3张及以上 剪枝
						return;
					}
				}
			}

			for (Map.Entry<Long, WeaveInfo> entry : map.entrySet()) {
				// 递归回溯
				solution.pushWeaveInfo(entry.getValue(), handCardInfo);
				analyseWin(handCardInfo, solution, lastCardIndex);
				solution.popWeaveInfo(handCardInfo);
			}
		}
	}

	public boolean getSolution(final List<Integer> handCard, int lastCardIndex, int luoDiPaiHuXi) {
		int cardCount = handCard.size();

		hasFoundBestSolution = false;

		bestSolution = new Solution(luoDiPaiHuXi);

		HandCardInfo handCardInfo = new HandCardInfo();
		Solution solution = new Solution(luoDiPaiHuXi);

		// Key: 牌值，Value：相同牌值的牌在手牌里的位置信息
		Map<Integer, List<Integer>> cardToCardPositions = new HashMap<>();

		for (int i = 0; i < cardCount; i++) {
			handCardInfo.addCard(handCard.get(i));

			if (!cardToCardPositions.containsKey(handCard.get(i))) {
				cardToCardPositions.put(handCard.get(i), new ArrayList<Integer>());
			}

			cardToCardPositions.get(handCard.get(i)).add(i);
		}

		for (Map.Entry<Integer, List<Integer>> entry : cardToCardPositions.entrySet()) {
			List<Integer> tmpPositions = entry.getValue();

			int count = tmpPositions.size();

			if (count == 4 && entry.getKey() != MAGIC) {
				// 优先拿出四张牌的
				int card_1 = handCardInfo.getCard(tmpPositions.get(0));
				int card_2 = handCardInfo.getCard(tmpPositions.get(1));
				int card_3 = handCardInfo.getCard(tmpPositions.get(2));
				int card_4 = handCardInfo.getCard(tmpPositions.get(3));

				Triple<Integer, Integer, List<Integer>> triple = getTriple(card_1, card_2, card_3, card_4);

				if (lastCardIndex == HandCardInfo.getCardIndex(entry.getKey())) {
					if (entry.getKey() < 0x10)
						triple = Triple.of(6, GameConstants.WIK_TI_LONG, Arrays.asList());
					else
						triple = Triple.of(9, GameConstants.WIK_TI_LONG, Arrays.asList());
				}

				WeaveInfo weaveInfo = new WeaveInfo(triple, tmpPositions.get(0), tmpPositions.get(1), tmpPositions.get(2), tmpPositions.get(3));

				solution.pushWeaveInfo(weaveInfo, handCardInfo);
			}
		}

		analyseWin(handCardInfo, solution, lastCardIndex);

		return hasFoundBestSolution;
	}

	public int getQiHuFan() {
		return qiHuFan;
	}

	public void setQiHuFan(int qiHuFan) {
		this.qiHuFan = qiHuFan;
	}

	public boolean isHasZiMo() {
		return hasZiMo;
	}

	public void setHasZiMo(boolean hasZiMo) {
		this.hasZiMo = hasZiMo;
	}

	public boolean isHasWangDiao() {
		return hasWangDiao;
	}

	public void setHasWangDiao(boolean hasWangDiao) {
		this.hasWangDiao = hasWangDiao;
	}

	public boolean isHasWangDiaoWang() {
		return hasWangDiaoWang;
	}

	public void setHasWangDiaoWang(boolean hasWangDiaoWang) {
		this.hasWangDiaoWang = hasWangDiaoWang;
	}

	public boolean isHasWangChuang() {
		return hasWangChuang;
	}

	public void setHasWangChuang(boolean hasWangChuang) {
		this.hasWangChuang = hasWangChuang;
	}

	public boolean isHasWangChuangWang() {
		return hasWangChuangWang;
	}

	public void setHasWangChuangWang(boolean hasWangChuangWang) {
		this.hasWangChuangWang = hasWangChuangWang;
	}

	public boolean isHasWangZha() {
		return hasWangZha;
	}

	public void setHasWangZha(boolean hasWangZha) {
		this.hasWangZha = hasWangZha;
	}

	public boolean isHasWangZhaWang() {
		return hasWangZhaWang;
	}

	public void setHasWangZhaWang(boolean hasWangZhaWang) {
		this.hasWangZhaWang = hasWangZhaWang;
	}

	public boolean getWangDiao() {
		return wangDiao;
	}

	public void setWangDiao(boolean wangDiao) {
		this.wangDiao = wangDiao;
	}

	public boolean getQiShouGenXing() {
		return qiShouGenXing;
	}

	public void setQiShouGenXing(boolean qiShouGenXing) {
		this.qiShouGenXing = qiShouGenXing;
	}

	public int getWeiAddedCard() {
		return weiAddedCard;
	}

	public void setWeiAddedCard(int weiAddedCard) {
		this.weiAddedCard = weiAddedCard;
	}
}

class HandCardInfo {
	public List<Integer> handCards = new ArrayList<>();
	private int cardsFlag = 0;
	@SuppressWarnings("unused")
	private int remainCardCount = 0;
	@SuppressWarnings("unchecked")
	public List<Integer>[] cardIndexToCardPositions = new ArrayList[21];

	public HandCardInfo() {
		handCards = new ArrayList<>();
		cardsFlag = 0;
		remainCardCount = 0;

		for (int i = 0; i < 21; i++) {
			cardIndexToCardPositions[i] = new ArrayList<Integer>();
		}
	}

	public static int getCardIndex(int card) {
		if (card >= 0x01 && card <= 0x0A) {
			return card - 1;
		} else if (card >= 0x11 && card <= 0x1A) {
			return 10 + card - 0x11;
		} else if (card == 0xFF) {
			return 20;
		}

		return -1;
	}

	public static int getCardByIndex(int index) {
		if (index == 20)
			return 0xFF;
		else if (index >= 0 && index < 10)
			return index + 1;
		else if (index >= 10 && index < 20)
			return index - 9 + 0x10;
		else
			return 0;
	}

	public void addCard(int card) {
		int index = getCardIndex(card);
		if (index >= 0 && index <= 20) {
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

					if (cardIndex >= 0 && cardIndex <= 20) {
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
					if (cardIndex >= 0 && cardIndex <= 20) {
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
