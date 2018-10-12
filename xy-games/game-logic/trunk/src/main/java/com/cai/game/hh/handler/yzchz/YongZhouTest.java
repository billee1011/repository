package com.cai.game.hh.handler.yzchz;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import com.cai.common.constant.GameConstants;
import com.cai.game.hh.HHGameLogic;
import com.cai.game.hh.HHGameLogic.AnalyseItem;
import com.cai.game.hh.util.AnalyseUtil;

public class YongZhouTest {
	private static final Random random = new Random();

	public static void main(String[] args) {
		int loops = 100000000;
		int weaveCount = 2;
		test(loops, weaveCount);
		weaveCount = 3;
		test(loops, weaveCount);
		weaveCount = 4;
		test(loops, weaveCount);
		weaveCount = 5;
		test(loops, weaveCount);
		weaveCount = 6;
		test(loops, weaveCount);
	}

	private static void test(int loops, int weaveCount) {
		HHGameLogic logic = new HHGameLogic();
		PrintWriter stdout = null;

		try {
			stdout = new PrintWriter(loops + "_" + weaveCount + "_old_new_compare.txt");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		stdout.println("Compare Test Begin...");

		int winCount = 0;
		int loseCount = 0;

		int[] cardsIndex = new int[GameConstants.MAX_HH_INDEX];
		int cardCount = 0;
		int randomIndex = 0;
		boolean canWin = false;
		boolean oldCanWin = false;

		long timer = System.currentTimeMillis();

		for (int i = 0; i < loops; i++) {
			cardCount = 0;
			Arrays.fill(cardsIndex, 0);

			while (cardCount < GameConstants.MAX_HH_COUNT && cardCount < weaveCount * 3 + 2) {
				randomIndex = random.nextInt(GameConstants.MAX_HH_INDEX);

				if (cardsIndex[randomIndex] < 3) {
					cardsIndex[randomIndex]++;
					cardCount++;
				}
			}

			AnalyseUtil analyseUtil = new AnalyseUtil(0, 1, 0, 0, null, true, false);
			List<Integer> cardsList = new ArrayList<>();
			int[] handCards = new int[GameConstants.MAX_HH_COUNT];
			int handCardCount = logic.switch_to_cards_data(cardsIndex, handCards);

			int lastCard = handCards[random.nextInt(handCardCount)];
			int lastCardIndex = logic.switch_to_card_index(lastCard);

			for (int c = 0; c < handCardCount; c++) {
				cardsList.add(handCards[c]);
			}
			canWin = analyseUtil.getSolution(cardsList, lastCardIndex, 0);

			List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
			oldCanWin = logic.analyse_card_phz(cardsIndex, null, 0, 0, 0, lastCard, analyseItemArray, false, new int[] { 0 }, false);

			if (canWin != oldCanWin) {
				stdout.println("比对失败。" + "新算法：" + canWin + "，旧算法" + oldCanWin);
				stdout.println(cardsList.toString());
				stdout.println();
			}

			if (canWin)
				winCount++;
			else
				loseCount++;
		}

		stdout.println(loops + "次随机测试，用时" + (System.currentTimeMillis() - timer) + "毫秒，" + "赢牌" + winCount + "次，" + "输牌" + loseCount + "次。");
		stdout.println("Compare Test Finished...");

		if (stdout != null)
			stdout.close();
	}
}
