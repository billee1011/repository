package com.cai.game.hh.util;

import java.util.ArrayList;
import java.util.List;

public class Solution {
	public int totalHuXi = 0;
	public int handCardHuXi = -1;
	public List<WeaveInfo> weaveInfoList = new ArrayList<>();
	public List<Integer> magicToRealCard = new ArrayList<>();
	public int totalScore = 0;
	public boolean smallCardTypeMoreScore = false;

	public Solution() {
	}

	public Solution(int huXi) {
		this.totalHuXi = huXi;
		this.totalScore = 0;
	}

	public Solution(Solution s) {
		this.totalHuXi = s.totalHuXi;
		this.handCardHuXi = s.handCardHuXi;
		this.smallCardTypeMoreScore = s.smallCardTypeMoreScore;
		for (WeaveInfo wInfo : s.weaveInfoList) {
			Triple<Integer, Integer, List<Integer>> tmpTriple = wInfo.triple;
			int tmpCount = wInfo.cardPositions.size();

			WeaveInfo tmpWeaveInfo = null;

			if (tmpCount == 2) {
				tmpWeaveInfo = new WeaveInfo(tmpTriple, wInfo.cardPositions.get(0), wInfo.cardPositions.get(1), -1, -1);
			} else if (tmpCount == 3) {
				tmpWeaveInfo = new WeaveInfo(tmpTriple, wInfo.cardPositions.get(0), wInfo.cardPositions.get(1), wInfo.cardPositions.get(2), -1);
			} else if (tmpCount == 4) {
				tmpWeaveInfo = new WeaveInfo(tmpTriple, wInfo.cardPositions.get(0), wInfo.cardPositions.get(1), wInfo.cardPositions.get(2),
						wInfo.cardPositions.get(3));
			}

			if (tmpWeaveInfo != null) {
				this.weaveInfoList.add(wInfo);
			}
		}
		for (int card : s.magicToRealCard) {
			this.magicToRealCard.add(card);
		}
	}

	public void pushWeaveInfo(WeaveInfo weaveInfo, HandCardInfo handCardInfo) {
		int tmpHuXi = weaveInfo.triple.getFirst();
		totalHuXi += tmpHuXi;
		if (weaveInfoList.size() == 0) {
			handCardHuXi = 0;
		}
		handCardHuXi += tmpHuXi;
		weaveInfoList.add(weaveInfo);

		if (handCardInfo != null) {
			List<Integer> cardPositions = weaveInfo.cardPositions;
			for (int i = 0; i < cardPositions.size(); i++) {
				handCardInfo.setIsUsed(cardPositions.get(i), true);
			}
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

		int tmpHuXi = weaveInfoList.get(weaveCount - 1).triple.getFirst();
		totalHuXi -= tmpHuXi;
		handCardHuXi -= tmpHuXi;
		weaveInfoList.remove(weaveCount - 1);

		if (weaveInfoList.size() == 0) {
			handCardHuXi = -1;
		}
	}

	@Override
	public String toString() {
		return "Solution [" + "TotalHuXi:" + totalHuXi + ", HandCardHuXi: " + handCardHuXi + ", TotalScore:" + totalScore + ", magicToRealCard:"
				+ magicToRealCard.toString() + ", " + weaveInfoList.toString() + "] ";
	}
}
