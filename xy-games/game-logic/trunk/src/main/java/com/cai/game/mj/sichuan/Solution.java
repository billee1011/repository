package com.cai.game.mj.sichuan;

import java.util.ArrayList;
import java.util.List;

import com.cai.common.util.Pair;

public class Solution {
	public List<WeaveInfo> weaveInfoList = new ArrayList<>();
	public List<Integer> magicToRealCard = new ArrayList<>();
	public int gengCount = 0;

	public Solution() {
	}

	public Solution(Solution s) {
		for (WeaveInfo wInfo : s.weaveInfoList) {
			Pair<Integer, List<Integer>> tmpPair = wInfo.pair;

			int tmpCount = wInfo.cardPositions.size();

			WeaveInfo tmpWeaveInfo = null;

			if (tmpCount == 2) {
				tmpWeaveInfo = new WeaveInfo(tmpPair, wInfo.cardPositions.get(0), wInfo.cardPositions.get(1), -1);
			} else if (tmpCount == 3) {
				tmpWeaveInfo = new WeaveInfo(tmpPair, wInfo.cardPositions.get(0), wInfo.cardPositions.get(1), wInfo.cardPositions.get(2));
			}

			if (tmpWeaveInfo != null)
				weaveInfoList.add(tmpWeaveInfo);
		}

		for (int card : s.magicToRealCard)
			magicToRealCard.add(card);

		gengCount = s.gengCount;
	}

	public void pushWeaveInfo(WeaveInfo weaveInfo, HandCardInfo handCardInfo) {
		weaveInfoList.add(weaveInfo);

		if (handCardInfo != null) {
			List<Integer> cardPositions = weaveInfo.cardPositions;

			for (int i = 0; i < cardPositions.size(); i++)
				handCardInfo.setIsUsed(cardPositions.get(i), true);
		}
	}

	public void popWeaveInfo(HandCardInfo handCardInfo) {
		int weaveCount = weaveInfoList.size();
		if (weaveCount > 0 && handCardInfo != null) {
			List<Integer> cardPositions = weaveInfoList.get(weaveCount - 1).cardPositions;
			for (int i = 0; i < cardPositions.size(); i++) {
				handCardInfo.setIsUsed(cardPositions.get(i), false);
			}
		}
		weaveInfoList.remove(weaveCount - 1);
	}
}
