/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.tasks;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.game.util.AnalyseCardUtil;

/**
 * 麻将手牌分析[只是测试用]
 *
 * @author wu_hc date: 2017年10月11日 上午10:06:32 <br/>
 */
public final class AnalyseMJCardTask implements Runnable {

	/**
	 * 
	 */
	private static final Logger logger = LoggerFactory.getLogger(AnalyseMJCardTask.class);

	/**
	 * 当前手牌
	 */
	private final int[] handCardsIndex;

	/**
	 * 当前牌索引
	 */
	private final int curCardIndex;

	/**
	 * 癞子
	 */
	private final int[] magicCardsIndex;

	/**
	 * 
	 */
	private final int totalMagicCardIndexs;

	/**
	 * 预期结果
	 */
	private final boolean expectResult;

	/**
	 * 麻将game_type
	 */
	private final int mjGameTtype;

	/**
	 * @param handCardsIndex
	 * @param curCardIndex
	 * @param magicCardsIndex
	 * @param totalMagicCardIndexs
	 */
	public AnalyseMJCardTask(int[] handCardsIndex, int curCardIndex, int[] magicCardsIndex, int totalMagicCardIndexs, int mjGameTtype,
			boolean expectResult) {
		this.handCardsIndex = handCardsIndex;
		this.curCardIndex = curCardIndex;
		this.magicCardsIndex = magicCardsIndex;
		this.totalMagicCardIndexs = totalMagicCardIndexs;
		this.expectResult = expectResult;
		this.mjGameTtype = mjGameTtype;
	}

	@Override
	public void run() {
		boolean analyseRst = AnalyseCardUtil.analyse_win_by_cards_index(handCardsIndex, curCardIndex, magicCardsIndex, totalMagicCardIndexs);

		// 新方式的计算结果和旧方式的计算结果不一致
		if (!Objects.equals(analyseRst, expectResult)) {
			logger.warn("麻将[gameType:{}]新算法有异常[旧算法:{},新算法:{}]，handCardsIndex:{},curCardIndex:{},magicCardsIndex:{},totalMagicCardIndexs:{}",
					this.mjGameTtype, expectResult, analyseRst, handCardsIndex, curCardIndex, magicCardsIndex, totalMagicCardIndexs);
		}
	}
}
