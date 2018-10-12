package com.cai.game.mj;

import java.util.List;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.WeaveItem;

public class NewMjGameLogic extends MJGameLogic {

	public NewMjGameLogic(MJType mjType) {
		super(mjType);
	}

	/**
	 * 河南获嘉新乡获嘉麻将
	 * 
	 * @param cards_index
	 *            胡牌分析时的手牌索引数据，一共3n+2张牌
	 * @param weaveItem
	 *            落地牌的组合
	 * @param cbWeaveCount
	 *            落地牌的数目
	 * @param analyseItemArray
	 *            分析结果，最好再轮询一次，以便获取最大牌型分，获取听牌数据时，不用走
	 * @param has_19_cheng_ju
	 *            有19成句玩法
	 * @param has_feng_cheng_ju
	 *            有风成句玩法
	 * @return
	 */
	public boolean analyse_card_huo_jia(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, List<AnalyseItem> analyseItemArray,
			boolean has_19_cheng_ju, boolean has_feng_cheng_ju) {
		int cbCardCount = get_card_count_by_index(cards_index);
		int cbKindItemCount = 0;
		int cbLessKindItem = (cbCardCount - 2) / 3;

		if (cbLessKindItem == 0) {
			int mj_count = GameConstants.MAX_ZI_FENG;

			for (int i = 0; i < mj_count; i++) {
				if (cards_index[i] == 2) {
					AnalyseItem analyseItem = new AnalyseItem();

					for (int j = 0; j < cbWeaveCount; j++) {
						analyseItem.cbWeaveKind[j] = weaveItem[j].weave_kind;
						analyseItem.cbCenterCard[j] = weaveItem[j].center_card;
						get_weave_card(weaveItem[j].weave_kind, weaveItem[j].center_card, analyseItem.cbCardData[j]);
					}

					analyseItem.cbCardEye = switch_to_card_data(i);

					analyseItemArray.add(analyseItem);

					return true;
				}
			}

			return false;
		}

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		KindItem kindItem[] = new KindItem[27 * 9 + 7 + 14];
		for (int i = 0; i < kindItem.length; i++) {
			kindItem[i] = new KindItem();
		}

		if (cbCardCount >= 3) {
			int mj_count = GameConstants.MAX_ZI_FENG;

			for (int i = 0; i < mj_count; i++) {
				// 同牌判断
				if (cbCardIndexTemp[i] >= 3) {
					if (cbKindItemCount >= kindItem.length) {
						return false;
					}

					kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_PENG;
					kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);
					kindItem[cbKindItemCount].cbValidIndex[0] = i;
					kindItem[cbKindItemCount].cbValidIndex[1] = i;
					kindItem[cbKindItemCount].cbValidIndex[2] = i;

					cbKindItemCount++;
				}

				// 连牌判断
				if ((i < (GameConstants.MAX_ZI - 2)) && ((i % 9) < 7)) {
					int chi_count = cbCardIndexTemp[i] + cbCardIndexTemp[i + 1] + cbCardIndexTemp[i + 2];

					if (chi_count >= 3) {
						int cbIndex[] = { cbCardIndexTemp[i], cbCardIndexTemp[i + 1], cbCardIndexTemp[i + 2] };

						int cbValidIndex[] = new int[3];

						boolean can_combime = true;

						while (cbIndex[0] + cbIndex[1] + cbIndex[2] >= 3) {
							for (int j = 0; j < cbIndex.length; j++) {
								if (cbIndex[j] > 0) {
									cbIndex[j]--;
									cbValidIndex[j] = i + j;
								} else {
									can_combime = false;
									break;
								}
							}

							if (can_combime) {
								if (cbKindItemCount >= kindItem.length) {
									return false;
								}

								kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_LEFT;
								kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

								for (int cbValidIndex_index = 0; cbValidIndex_index < 3; cbValidIndex_index++) {
									kindItem[cbKindItemCount].cbValidIndex[cbValidIndex_index] = cbValidIndex[cbValidIndex_index];
								}

								cbKindItemCount++;
							} else {
								break;
							}
						}
					}
				}

				// 风吃、风成句
				// cbSubWeaveKind:
				// 1 东西南
				// 2 东西北
				// 3 东南北
				// 4 西南北
				if (has_feng_cheng_ju) {
					if (i == INDEX_DONG_FENG) {
						{
							// 东西南
							int chi_count = cbCardIndexTemp[INDEX_DONG_FENG] + cbCardIndexTemp[INDEX_XI_FENG] + cbCardIndexTemp[INDEX_NAN_FENG];

							if (chi_count >= 3) {
								int cbIndex[] = { cbCardIndexTemp[INDEX_DONG_FENG], cbCardIndexTemp[INDEX_XI_FENG], cbCardIndexTemp[INDEX_NAN_FENG] };

								boolean can_combime = true;

								while (cbIndex[0] + cbIndex[1] + cbIndex[2] >= 3) {
									for (int j = 0; j < cbIndex.length; j++) {
										if (cbIndex[j] > 0) {
											cbIndex[j]--;
										} else {
											can_combime = false;
											break;
										}
									}

									if (can_combime) {
										if (cbKindItemCount >= kindItem.length) {
											return false;
										}

										kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_FENG_CHI;
										kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

										kindItem[cbKindItemCount].cbValidIndex[0] = INDEX_DONG_FENG;
										kindItem[cbKindItemCount].cbValidIndex[1] = INDEX_XI_FENG;
										kindItem[cbKindItemCount].cbValidIndex[2] = INDEX_NAN_FENG;

										cbKindItemCount++;
									} else {
										break;
									}
								}
							}
						}

						{
							// 东西北
							int chi_count = cbCardIndexTemp[INDEX_DONG_FENG] + cbCardIndexTemp[INDEX_XI_FENG] + cbCardIndexTemp[INDEX_BEI_FENG];

							if (chi_count >= 3) {
								int cbIndex[] = { cbCardIndexTemp[INDEX_DONG_FENG], cbCardIndexTemp[INDEX_XI_FENG], cbCardIndexTemp[INDEX_BEI_FENG] };

								boolean can_combime = true;

								while (cbIndex[0] + cbIndex[1] + cbIndex[2] >= 3) {
									for (int j = 0; j < cbIndex.length; j++) {
										if (cbIndex[j] > 0) {
											cbIndex[j]--;
										} else {
											can_combime = false;
											break;
										}
									}

									if (can_combime) {
										if (cbKindItemCount >= kindItem.length) {
											return false;
										}

										kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_FENG_CHI;
										kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

										kindItem[cbKindItemCount].cbValidIndex[0] = INDEX_DONG_FENG;
										kindItem[cbKindItemCount].cbValidIndex[1] = INDEX_XI_FENG;
										kindItem[cbKindItemCount].cbValidIndex[2] = INDEX_BEI_FENG;

										cbKindItemCount++;
									} else {
										break;
									}
								}
							}
						}

						{
							// 东南北
							int chi_count = cbCardIndexTemp[INDEX_DONG_FENG] + cbCardIndexTemp[INDEX_NAN_FENG] + cbCardIndexTemp[INDEX_BEI_FENG];

							if (chi_count >= 3) {
								int cbIndex[] = { cbCardIndexTemp[INDEX_DONG_FENG], cbCardIndexTemp[INDEX_NAN_FENG],
										cbCardIndexTemp[INDEX_BEI_FENG] };

								boolean can_combime = true;

								while (cbIndex[0] + cbIndex[1] + cbIndex[2] >= 3) {
									for (int j = 0; j < cbIndex.length; j++) {
										if (cbIndex[j] > 0) {
											cbIndex[j]--;
										} else {
											can_combime = false;
											break;
										}
									}

									if (can_combime) {
										if (cbKindItemCount >= kindItem.length) {
											return false;
										}

										kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_FENG_CHI;
										kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

										kindItem[cbKindItemCount].cbValidIndex[0] = INDEX_DONG_FENG;
										kindItem[cbKindItemCount].cbValidIndex[1] = INDEX_NAN_FENG;
										kindItem[cbKindItemCount].cbValidIndex[2] = INDEX_BEI_FENG;

										cbKindItemCount++;
									} else {
										break;
									}
								}
							}
						}
					}
					if (i == INDEX_XI_FENG) {
						// 西南北
						int chi_count = cbCardIndexTemp[INDEX_XI_FENG] + cbCardIndexTemp[INDEX_NAN_FENG] + cbCardIndexTemp[INDEX_BEI_FENG];

						if (chi_count >= 3) {
							int cbIndex[] = { cbCardIndexTemp[INDEX_XI_FENG], cbCardIndexTemp[INDEX_NAN_FENG], cbCardIndexTemp[INDEX_BEI_FENG] };

							boolean can_combime = true;

							while (cbIndex[0] + cbIndex[1] + cbIndex[2] >= 3) {
								for (int j = 0; j < cbIndex.length; j++) {
									if (cbIndex[j] > 0) {
										cbIndex[j]--;
									} else {
										can_combime = false;
										break;
									}
								}

								if (can_combime) {
									if (cbKindItemCount >= kindItem.length) {
										return false;
									}

									kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_FENG_CHI;
									kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

									kindItem[cbKindItemCount].cbValidIndex[0] = INDEX_XI_FENG;
									kindItem[cbKindItemCount].cbValidIndex[1] = INDEX_NAN_FENG;
									kindItem[cbKindItemCount].cbValidIndex[2] = INDEX_BEI_FENG;

									cbKindItemCount++;
								} else {
									break;
								}
							}
						}
					}
					if (i == INDEX_ZHONG_FENG) {
						// 中发白
						int chi_count = cbCardIndexTemp[INDEX_ZHONG_FENG] + cbCardIndexTemp[INDEX_FA_FENG] + cbCardIndexTemp[INDEX_BAI_FENG];

						if (chi_count >= 3) {
							int cbIndex[] = { cbCardIndexTemp[INDEX_ZHONG_FENG], cbCardIndexTemp[INDEX_FA_FENG], cbCardIndexTemp[INDEX_BAI_FENG] };

							boolean can_combime = true;

							while (cbIndex[0] + cbIndex[1] + cbIndex[2] >= 3) {
								for (int j = 0; j < cbIndex.length; j++) {
									if (cbIndex[j] > 0) {
										cbIndex[j]--;
									} else {
										can_combime = false;
										break;
									}
								}

								if (can_combime) {
									if (cbKindItemCount >= kindItem.length) {
										return false;
									}

									kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_FENG_CHI;
									kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

									kindItem[cbKindItemCount].cbValidIndex[0] = INDEX_ZHONG_FENG;
									kindItem[cbKindItemCount].cbValidIndex[1] = INDEX_FA_FENG;
									kindItem[cbKindItemCount].cbValidIndex[2] = INDEX_BAI_FENG;

									cbKindItemCount++;
								} else {
									break;
								}
							}
						}
					}
				}

				// 19成句
				// 三色牌，119或199随机组合，一共有36种可能， 根据排列组合公式计算出来的，((3 + 1 + 1 + 1) * 3)
				// * 2
				if (has_19_cheng_ju) {
					if (i == WAN_FIRST_INDEX) {
						{
							// 1：1万 1万 9万
							int chi_count = cbCardIndexTemp[WAN_FIRST_INDEX] + cbCardIndexTemp[WAN_LAST_INDEX];

							if (chi_count >= 3) {
								int cbIndex[] = { cbCardIndexTemp[WAN_FIRST_INDEX], cbCardIndexTemp[WAN_LAST_INDEX] };

								boolean can_combime = true;

								while (cbIndex[0] + cbIndex[1] >= 3) {
									for (int j = 0; j < cbIndex.length; j++) {
										if (j == 0) {
											if (cbIndex[j] > 1) {
												cbIndex[j]--;
												cbIndex[j]--;
											} else {
												can_combime = false;
												break;
											}
										} else if (j == 1) {
											if (cbIndex[j] > 0) {
												cbIndex[j]--;
											} else {
												can_combime = false;
												break;
											}
										}
									}

									if (can_combime) {
										if (cbKindItemCount >= kindItem.length) {
											return false;
										}

										kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_YAO_JIU;
										kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

										kindItem[cbKindItemCount].cbValidIndex[0] = WAN_FIRST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[1] = WAN_FIRST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[2] = WAN_LAST_INDEX;

										cbKindItemCount++;
									} else {
										break;
									}
								}
							}
						}

						{
							// 2：1万 1万 9条
							int chi_count = cbCardIndexTemp[WAN_FIRST_INDEX] + cbCardIndexTemp[TIAO_LAST_INDEX];

							if (chi_count >= 3) {
								int cbIndex[] = { cbCardIndexTemp[WAN_FIRST_INDEX], cbCardIndexTemp[TIAO_LAST_INDEX] };

								boolean can_combime = true;

								while (cbIndex[0] + cbIndex[1] >= 3) {
									for (int j = 0; j < cbIndex.length; j++) {
										if (j == 0) {
											if (cbIndex[j] > 1) {
												cbIndex[j]--;
												cbIndex[j]--;
											} else {
												can_combime = false;
												break;
											}
										} else if (j == 1) {
											if (cbIndex[j] > 0) {
												cbIndex[j]--;
											} else {
												can_combime = false;
												break;
											}
										}
									}

									if (can_combime) {
										if (cbKindItemCount >= kindItem.length) {
											return false;
										}

										kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_YAO_JIU;
										kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

										kindItem[cbKindItemCount].cbValidIndex[0] = WAN_FIRST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[1] = WAN_FIRST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[2] = TIAO_LAST_INDEX;

										cbKindItemCount++;
									} else {
										break;
									}
								}
							}
						}

						{
							// 3：1万 1万 9筒
							int chi_count = cbCardIndexTemp[WAN_FIRST_INDEX] + cbCardIndexTemp[TONG_LAST_INDEX];

							if (chi_count >= 3) {
								int cbIndex[] = { cbCardIndexTemp[WAN_FIRST_INDEX], cbCardIndexTemp[TONG_LAST_INDEX] };

								boolean can_combime = true;

								while (cbIndex[0] + cbIndex[1] >= 3) {
									for (int j = 0; j < cbIndex.length; j++) {
										if (j == 0) {
											if (cbIndex[j] > 1) {
												cbIndex[j]--;
												cbIndex[j]--;
											} else {
												can_combime = false;
												break;
											}
										} else if (j == 1) {
											if (cbIndex[j] > 0) {
												cbIndex[j]--;
											} else {
												can_combime = false;
												break;
											}
										}
									}

									if (can_combime) {
										if (cbKindItemCount >= kindItem.length) {
											return false;
										}

										kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_YAO_JIU;
										kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

										kindItem[cbKindItemCount].cbValidIndex[0] = WAN_FIRST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[1] = WAN_FIRST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[2] = TONG_LAST_INDEX;

										cbKindItemCount++;
									} else {
										break;
									}
								}
							}
						}

						{
							// 4：1万 1条 9万
							int chi_count = cbCardIndexTemp[WAN_FIRST_INDEX] + cbCardIndexTemp[TIAO_FIRST_INDEX] + cbCardIndexTemp[WAN_LAST_INDEX];

							if (chi_count >= 3) {
								int cbIndex[] = { cbCardIndexTemp[WAN_FIRST_INDEX], cbCardIndexTemp[TIAO_FIRST_INDEX],
										cbCardIndexTemp[WAN_LAST_INDEX] };

								boolean can_combime = true;

								while (cbIndex[0] + cbIndex[1] + cbIndex[2] >= 3) {
									for (int j = 0; j < cbIndex.length; j++) {
										if (cbIndex[j] > 0) {
											cbIndex[j]--;
										} else {
											can_combime = false;
											break;
										}
									}

									if (can_combime) {
										if (cbKindItemCount >= kindItem.length) {
											return false;
										}

										kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_YAO_JIU;
										kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

										kindItem[cbKindItemCount].cbValidIndex[0] = WAN_FIRST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[1] = TIAO_FIRST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[2] = WAN_LAST_INDEX;

										cbKindItemCount++;
									} else {
										break;
									}
								}
							}
						}

						{
							// 5：1万 1条 9条
							int chi_count = cbCardIndexTemp[WAN_FIRST_INDEX] + cbCardIndexTemp[TIAO_FIRST_INDEX] + cbCardIndexTemp[TIAO_LAST_INDEX];

							if (chi_count >= 3) {
								int cbIndex[] = { cbCardIndexTemp[WAN_FIRST_INDEX], cbCardIndexTemp[TIAO_FIRST_INDEX],
										cbCardIndexTemp[TIAO_LAST_INDEX] };

								boolean can_combime = true;

								while (cbIndex[0] + cbIndex[1] + cbIndex[2] >= 3) {
									for (int j = 0; j < cbIndex.length; j++) {
										if (cbIndex[j] > 0) {
											cbIndex[j]--;
										} else {
											can_combime = false;
											break;
										}
									}

									if (can_combime) {
										if (cbKindItemCount >= kindItem.length) {
											return false;
										}

										kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_YAO_JIU;
										kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

										kindItem[cbKindItemCount].cbValidIndex[0] = WAN_FIRST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[1] = TIAO_FIRST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[2] = TIAO_LAST_INDEX;

										cbKindItemCount++;
									} else {
										break;
									}
								}
							}
						}

						{
							// 6：1万 1条 9筒
							int chi_count = cbCardIndexTemp[WAN_FIRST_INDEX] + cbCardIndexTemp[TIAO_FIRST_INDEX] + cbCardIndexTemp[TONG_LAST_INDEX];

							if (chi_count >= 3) {
								int cbIndex[] = { cbCardIndexTemp[WAN_FIRST_INDEX], cbCardIndexTemp[TIAO_FIRST_INDEX],
										cbCardIndexTemp[TONG_LAST_INDEX] };

								boolean can_combime = true;

								while (cbIndex[0] + cbIndex[1] + cbIndex[2] >= 3) {
									for (int j = 0; j < cbIndex.length; j++) {
										if (cbIndex[j] > 0) {
											cbIndex[j]--;
										} else {
											can_combime = false;
											break;
										}
									}

									if (can_combime) {
										if (cbKindItemCount >= kindItem.length) {
											return false;
										}

										kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_YAO_JIU;
										kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

										kindItem[cbKindItemCount].cbValidIndex[0] = WAN_FIRST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[1] = TIAO_FIRST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[2] = TONG_LAST_INDEX;

										cbKindItemCount++;
									} else {
										break;
									}
								}
							}
						}

						{
							// 7：1万 1筒 9万
							int chi_count = cbCardIndexTemp[WAN_FIRST_INDEX] + cbCardIndexTemp[TONG_FIRST_INDEX] + cbCardIndexTemp[WAN_LAST_INDEX];

							if (chi_count >= 3) {
								int cbIndex[] = { cbCardIndexTemp[WAN_FIRST_INDEX], cbCardIndexTemp[TONG_FIRST_INDEX],
										cbCardIndexTemp[WAN_LAST_INDEX] };

								boolean can_combime = true;

								while (cbIndex[0] + cbIndex[1] + cbIndex[2] >= 3) {
									for (int j = 0; j < cbIndex.length; j++) {
										if (cbIndex[j] > 0) {
											cbIndex[j]--;
										} else {
											can_combime = false;
											break;
										}
									}

									if (can_combime) {
										if (cbKindItemCount >= kindItem.length) {
											return false;
										}

										kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_YAO_JIU;
										kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

										kindItem[cbKindItemCount].cbValidIndex[0] = WAN_FIRST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[1] = TONG_FIRST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[2] = WAN_LAST_INDEX;

										cbKindItemCount++;
									} else {
										break;
									}
								}
							}
						}

						{
							// 8：1万 1筒 9条
							int chi_count = cbCardIndexTemp[WAN_FIRST_INDEX] + cbCardIndexTemp[TONG_FIRST_INDEX] + cbCardIndexTemp[TIAO_LAST_INDEX];

							if (chi_count >= 3) {
								int cbIndex[] = { cbCardIndexTemp[WAN_FIRST_INDEX], cbCardIndexTemp[TONG_FIRST_INDEX],
										cbCardIndexTemp[TIAO_LAST_INDEX] };

								boolean can_combime = true;

								while (cbIndex[0] + cbIndex[1] + cbIndex[2] >= 3) {
									for (int j = 0; j < cbIndex.length; j++) {
										if (cbIndex[j] > 0) {
											cbIndex[j]--;
										} else {
											can_combime = false;
											break;
										}
									}

									if (can_combime) {
										if (cbKindItemCount >= kindItem.length) {
											return false;
										}

										kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_YAO_JIU;
										kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

										kindItem[cbKindItemCount].cbValidIndex[0] = WAN_FIRST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[1] = TONG_FIRST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[2] = TIAO_LAST_INDEX;

										cbKindItemCount++;
									} else {
										break;
									}
								}
							}
						}

						{
							// 9：1万 1筒 9筒
							int chi_count = cbCardIndexTemp[WAN_FIRST_INDEX] + cbCardIndexTemp[TONG_FIRST_INDEX] + cbCardIndexTemp[TONG_LAST_INDEX];

							if (chi_count >= 3) {
								int cbIndex[] = { cbCardIndexTemp[WAN_FIRST_INDEX], cbCardIndexTemp[TONG_FIRST_INDEX],
										cbCardIndexTemp[TONG_LAST_INDEX] };

								boolean can_combime = true;

								while (cbIndex[0] + cbIndex[1] + cbIndex[2] >= 3) {
									for (int j = 0; j < cbIndex.length; j++) {
										if (cbIndex[j] > 0) {
											cbIndex[j]--;
										} else {
											can_combime = false;
											break;
										}
									}

									if (can_combime) {
										if (cbKindItemCount >= kindItem.length) {
											return false;
										}

										kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_YAO_JIU;
										kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

										kindItem[cbKindItemCount].cbValidIndex[0] = WAN_FIRST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[1] = TONG_FIRST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[2] = TONG_LAST_INDEX;

										cbKindItemCount++;
									} else {
										break;
									}
								}
							}
						}

						{
							// 10：9万 9万 1万
							int chi_count = cbCardIndexTemp[WAN_LAST_INDEX] + cbCardIndexTemp[WAN_FIRST_INDEX];

							if (chi_count >= 3) {
								int cbIndex[] = { cbCardIndexTemp[WAN_LAST_INDEX], cbCardIndexTemp[WAN_FIRST_INDEX] };

								boolean can_combime = true;

								while (cbIndex[0] + cbIndex[1] >= 3) {
									for (int j = 0; j < cbIndex.length; j++) {
										if (j == 0) {
											if (cbIndex[j] > 1) {
												cbIndex[j]--;
												cbIndex[j]--;
											} else {
												can_combime = false;
												break;
											}
										} else if (j == 1) {
											if (cbIndex[j] > 0) {
												cbIndex[j]--;
											} else {
												can_combime = false;
												break;
											}
										}
									}

									if (can_combime) {
										if (cbKindItemCount >= kindItem.length) {
											return false;
										}

										kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_YAO_JIU;
										kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

										kindItem[cbKindItemCount].cbValidIndex[0] = WAN_LAST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[1] = WAN_LAST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[2] = WAN_FIRST_INDEX;

										cbKindItemCount++;
									} else {
										break;
									}
								}
							}
						}

						{
							// 11：9条 9条 1万
							int chi_count = cbCardIndexTemp[TIAO_LAST_INDEX] + cbCardIndexTemp[WAN_FIRST_INDEX];

							if (chi_count >= 3) {
								int cbIndex[] = { cbCardIndexTemp[TIAO_LAST_INDEX], cbCardIndexTemp[WAN_FIRST_INDEX] };

								boolean can_combime = true;

								while (cbIndex[0] + cbIndex[1] >= 3) {
									for (int j = 0; j < cbIndex.length; j++) {
										if (j == 0) {
											if (cbIndex[j] > 1) {
												cbIndex[j]--;
												cbIndex[j]--;
											} else {
												can_combime = false;
												break;
											}
										} else if (j == 1) {
											if (cbIndex[j] > 0) {
												cbIndex[j]--;
											} else {
												can_combime = false;
												break;
											}
										}
									}

									if (can_combime) {
										if (cbKindItemCount >= kindItem.length) {
											return false;
										}

										kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_YAO_JIU;
										kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

										kindItem[cbKindItemCount].cbValidIndex[0] = TIAO_LAST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[1] = TIAO_LAST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[2] = WAN_FIRST_INDEX;

										cbKindItemCount++;
									} else {
										break;
									}
								}
							}
						}

						{
							// 12：9筒 9筒 1万
							int chi_count = cbCardIndexTemp[TONG_LAST_INDEX] + cbCardIndexTemp[WAN_FIRST_INDEX];

							if (chi_count >= 3) {
								int cbIndex[] = { cbCardIndexTemp[TONG_LAST_INDEX], cbCardIndexTemp[WAN_FIRST_INDEX] };

								boolean can_combime = true;

								while (cbIndex[0] + cbIndex[1] >= 3) {
									for (int j = 0; j < cbIndex.length; j++) {
										if (j == 0) {
											if (cbIndex[j] > 1) {
												cbIndex[j]--;
												cbIndex[j]--;
											} else {
												can_combime = false;
												break;
											}
										} else if (j == 1) {
											if (cbIndex[j] > 0) {
												cbIndex[j]--;
											} else {
												can_combime = false;
												break;
											}
										}
									}

									if (can_combime) {
										if (cbKindItemCount >= kindItem.length) {
											return false;
										}

										kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_YAO_JIU;
										kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

										kindItem[cbKindItemCount].cbValidIndex[0] = TONG_LAST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[1] = TONG_LAST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[2] = WAN_FIRST_INDEX;

										cbKindItemCount++;
									} else {
										break;
									}
								}
							}
						}

						{
							// 13：9万 9条 1万
							int chi_count = cbCardIndexTemp[WAN_LAST_INDEX] + cbCardIndexTemp[TIAO_LAST_INDEX] + cbCardIndexTemp[WAN_FIRST_INDEX];

							if (chi_count >= 3) {
								int cbIndex[] = { cbCardIndexTemp[WAN_LAST_INDEX], cbCardIndexTemp[TIAO_LAST_INDEX],
										cbCardIndexTemp[WAN_FIRST_INDEX] };

								boolean can_combime = true;

								while (cbIndex[0] + cbIndex[1] + cbIndex[2] >= 3) {
									for (int j = 0; j < cbIndex.length; j++) {
										if (cbIndex[j] > 0) {
											cbIndex[j]--;
										} else {
											can_combime = false;
											break;
										}
									}

									if (can_combime) {
										if (cbKindItemCount >= kindItem.length) {
											return false;
										}

										kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_YAO_JIU;
										kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

										kindItem[cbKindItemCount].cbValidIndex[0] = WAN_LAST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[1] = TIAO_LAST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[2] = WAN_FIRST_INDEX;

										cbKindItemCount++;
									} else {
										break;
									}
								}
							}
						}

						{
							// 14：9万 9筒 1万
							int chi_count = cbCardIndexTemp[WAN_LAST_INDEX] + cbCardIndexTemp[TONG_LAST_INDEX] + cbCardIndexTemp[WAN_FIRST_INDEX];

							if (chi_count >= 3) {
								int cbIndex[] = { cbCardIndexTemp[WAN_LAST_INDEX], cbCardIndexTemp[TONG_LAST_INDEX],
										cbCardIndexTemp[WAN_FIRST_INDEX] };

								boolean can_combime = true;

								while (cbIndex[0] + cbIndex[1] + cbIndex[2] >= 3) {
									for (int j = 0; j < cbIndex.length; j++) {
										if (cbIndex[j] > 0) {
											cbIndex[j]--;
										} else {
											can_combime = false;
											break;
										}
									}

									if (can_combime) {
										if (cbKindItemCount >= kindItem.length) {
											return false;
										}

										kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_YAO_JIU;
										kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

										kindItem[cbKindItemCount].cbValidIndex[0] = WAN_LAST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[1] = TONG_LAST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[2] = WAN_FIRST_INDEX;

										cbKindItemCount++;
									} else {
										break;
									}
								}
							}
						}

						{
							// 15：9条 9筒 1万
							int chi_count = cbCardIndexTemp[TIAO_LAST_INDEX] + cbCardIndexTemp[TONG_LAST_INDEX] + cbCardIndexTemp[WAN_FIRST_INDEX];

							if (chi_count >= 3) {
								int cbIndex[] = { cbCardIndexTemp[TIAO_LAST_INDEX], cbCardIndexTemp[TONG_LAST_INDEX],
										cbCardIndexTemp[WAN_FIRST_INDEX] };

								boolean can_combime = true;

								while (cbIndex[0] + cbIndex[1] + cbIndex[2] >= 3) {
									for (int j = 0; j < cbIndex.length; j++) {
										if (cbIndex[j] > 0) {
											cbIndex[j]--;
										} else {
											can_combime = false;
											break;
										}
									}

									if (can_combime) {
										if (cbKindItemCount >= kindItem.length) {
											return false;
										}

										kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_YAO_JIU;
										kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

										kindItem[cbKindItemCount].cbValidIndex[0] = TIAO_LAST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[1] = TONG_LAST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[2] = WAN_FIRST_INDEX;

										cbKindItemCount++;
									} else {
										break;
									}
								}
							}
						}
					}

					if (i == TIAO_FIRST_INDEX) {
						{
							// 16：1条 1条 9万
							int chi_count = cbCardIndexTemp[TIAO_FIRST_INDEX] + cbCardIndexTemp[WAN_LAST_INDEX];

							if (chi_count >= 3) {
								int cbIndex[] = { cbCardIndexTemp[TIAO_FIRST_INDEX], cbCardIndexTemp[WAN_LAST_INDEX] };

								boolean can_combime = true;

								while (cbIndex[0] + cbIndex[1] >= 3) {
									for (int j = 0; j < cbIndex.length; j++) {
										if (j == 0) {
											if (cbIndex[j] > 1) {
												cbIndex[j]--;
												cbIndex[j]--;
											} else {
												can_combime = false;
												break;
											}
										} else if (j == 1) {
											if (cbIndex[j] > 0) {
												cbIndex[j]--;
											} else {
												can_combime = false;
												break;
											}
										}
									}

									if (can_combime) {
										if (cbKindItemCount >= kindItem.length) {
											return false;
										}

										kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_YAO_JIU;
										kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

										kindItem[cbKindItemCount].cbValidIndex[0] = TIAO_FIRST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[1] = TIAO_FIRST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[2] = WAN_LAST_INDEX;

										cbKindItemCount++;
									} else {
										break;
									}
								}
							}
						}

						{
							// 17：1条 1条 9条
							int chi_count = cbCardIndexTemp[TIAO_FIRST_INDEX] + cbCardIndexTemp[TIAO_LAST_INDEX];

							if (chi_count >= 3) {
								int cbIndex[] = { cbCardIndexTemp[TIAO_FIRST_INDEX], cbCardIndexTemp[TIAO_LAST_INDEX] };

								boolean can_combime = true;

								while (cbIndex[0] + cbIndex[1] >= 3) {
									for (int j = 0; j < cbIndex.length; j++) {
										if (j == 0) {
											if (cbIndex[j] > 1) {
												cbIndex[j]--;
												cbIndex[j]--;
											} else {
												can_combime = false;
												break;
											}
										} else if (j == 1) {
											if (cbIndex[j] > 0) {
												cbIndex[j]--;
											} else {
												can_combime = false;
												break;
											}
										}
									}

									if (can_combime) {
										if (cbKindItemCount >= kindItem.length) {
											return false;
										}

										kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_YAO_JIU;
										kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

										kindItem[cbKindItemCount].cbValidIndex[0] = TIAO_FIRST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[1] = TIAO_FIRST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[2] = TIAO_LAST_INDEX;

										cbKindItemCount++;
									} else {
										break;
									}
								}
							}
						}

						{
							// 18：1条 1条 9筒
							int chi_count = cbCardIndexTemp[TIAO_FIRST_INDEX] + cbCardIndexTemp[TONG_LAST_INDEX];

							if (chi_count >= 3) {
								int cbIndex[] = { cbCardIndexTemp[TIAO_FIRST_INDEX], cbCardIndexTemp[TONG_LAST_INDEX] };

								boolean can_combime = true;

								while (cbIndex[0] + cbIndex[1] >= 3) {
									for (int j = 0; j < cbIndex.length; j++) {
										if (j == 0) {
											if (cbIndex[j] > 1) {
												cbIndex[j]--;
												cbIndex[j]--;
											} else {
												can_combime = false;
												break;
											}
										} else if (j == 1) {
											if (cbIndex[j] > 0) {
												cbIndex[j]--;
											} else {
												can_combime = false;
												break;
											}
										}
									}

									if (can_combime) {
										if (cbKindItemCount >= kindItem.length) {
											return false;
										}

										kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_YAO_JIU;
										kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

										kindItem[cbKindItemCount].cbValidIndex[0] = TIAO_FIRST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[1] = TIAO_FIRST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[2] = TONG_LAST_INDEX;

										cbKindItemCount++;
									} else {
										break;
									}
								}
							}
						}

						{
							// 19：1条 1筒 9万
							int chi_count = cbCardIndexTemp[TIAO_FIRST_INDEX] + cbCardIndexTemp[TONG_FIRST_INDEX] + cbCardIndexTemp[WAN_LAST_INDEX];

							if (chi_count >= 3) {
								int cbIndex[] = { cbCardIndexTemp[TIAO_FIRST_INDEX], cbCardIndexTemp[TONG_FIRST_INDEX],
										cbCardIndexTemp[WAN_LAST_INDEX] };

								boolean can_combime = true;

								while (cbIndex[0] + cbIndex[1] + cbIndex[2] >= 3) {
									for (int j = 0; j < cbIndex.length; j++) {
										if (cbIndex[j] > 0) {
											cbIndex[j]--;
										} else {
											can_combime = false;
											break;
										}
									}

									if (can_combime) {
										if (cbKindItemCount >= kindItem.length) {
											return false;
										}

										kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_YAO_JIU;
										kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

										kindItem[cbKindItemCount].cbValidIndex[0] = TIAO_FIRST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[1] = TONG_FIRST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[2] = WAN_LAST_INDEX;

										cbKindItemCount++;
									} else {
										break;
									}
								}
							}
						}

						{
							// 20：1条 1筒 9条
							int chi_count = cbCardIndexTemp[TIAO_FIRST_INDEX] + cbCardIndexTemp[TONG_FIRST_INDEX] + cbCardIndexTemp[TIAO_LAST_INDEX];

							if (chi_count >= 3) {
								int cbIndex[] = { cbCardIndexTemp[TIAO_FIRST_INDEX], cbCardIndexTemp[TONG_FIRST_INDEX],
										cbCardIndexTemp[TIAO_LAST_INDEX] };

								boolean can_combime = true;

								while (cbIndex[0] + cbIndex[1] + cbIndex[2] >= 3) {
									for (int j = 0; j < cbIndex.length; j++) {
										if (cbIndex[j] > 0) {
											cbIndex[j]--;
										} else {
											can_combime = false;
											break;
										}
									}

									if (can_combime) {
										if (cbKindItemCount >= kindItem.length) {
											return false;
										}

										kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_YAO_JIU;
										kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

										kindItem[cbKindItemCount].cbValidIndex[0] = TIAO_FIRST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[1] = TONG_FIRST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[2] = TIAO_LAST_INDEX;

										cbKindItemCount++;
									} else {
										break;
									}
								}
							}
						}

						{
							// 21：1条 1筒 9筒
							int chi_count = cbCardIndexTemp[TIAO_FIRST_INDEX] + cbCardIndexTemp[TONG_FIRST_INDEX] + cbCardIndexTemp[TONG_LAST_INDEX];

							if (chi_count >= 3) {
								int cbIndex[] = { cbCardIndexTemp[TIAO_FIRST_INDEX], cbCardIndexTemp[TONG_FIRST_INDEX],
										cbCardIndexTemp[TONG_LAST_INDEX] };

								boolean can_combime = true;

								while (cbIndex[0] + cbIndex[1] + cbIndex[2] >= 3) {
									for (int j = 0; j < cbIndex.length; j++) {
										if (cbIndex[j] > 0) {
											cbIndex[j]--;
										} else {
											can_combime = false;
											break;
										}
									}

									if (can_combime) {
										if (cbKindItemCount >= kindItem.length) {
											return false;
										}

										kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_YAO_JIU;
										kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

										kindItem[cbKindItemCount].cbValidIndex[0] = TIAO_FIRST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[1] = TONG_FIRST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[2] = TONG_LAST_INDEX;

										cbKindItemCount++;
									} else {
										break;
									}
								}
							}
						}

						{
							// 22：9万 9万 1条
							int chi_count = cbCardIndexTemp[WAN_LAST_INDEX] + cbCardIndexTemp[TIAO_FIRST_INDEX];

							if (chi_count >= 3) {
								int cbIndex[] = { cbCardIndexTemp[WAN_LAST_INDEX], cbCardIndexTemp[TIAO_FIRST_INDEX] };

								boolean can_combime = true;

								while (cbIndex[0] + cbIndex[1] >= 3) {
									for (int j = 0; j < cbIndex.length; j++) {
										if (j == 0) {
											if (cbIndex[j] > 1) {
												cbIndex[j]--;
												cbIndex[j]--;
											} else {
												can_combime = false;
												break;
											}
										} else if (j == 1) {
											if (cbIndex[j] > 0) {
												cbIndex[j]--;
											} else {
												can_combime = false;
												break;
											}
										}
									}

									if (can_combime) {
										if (cbKindItemCount >= kindItem.length) {
											return false;
										}

										kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_YAO_JIU;
										kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

										kindItem[cbKindItemCount].cbValidIndex[0] = WAN_LAST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[1] = WAN_LAST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[2] = TIAO_FIRST_INDEX;

										cbKindItemCount++;
									} else {
										break;
									}
								}
							}
						}

						{
							// 23：9条 9条 1条
							int chi_count = cbCardIndexTemp[TIAO_LAST_INDEX] + cbCardIndexTemp[TIAO_FIRST_INDEX];

							if (chi_count >= 3) {
								int cbIndex[] = { cbCardIndexTemp[TIAO_LAST_INDEX], cbCardIndexTemp[TIAO_FIRST_INDEX] };

								boolean can_combime = true;

								while (cbIndex[0] + cbIndex[1] >= 3) {
									for (int j = 0; j < cbIndex.length; j++) {
										if (j == 0) {
											if (cbIndex[j] > 1) {
												cbIndex[j]--;
												cbIndex[j]--;
											} else {
												can_combime = false;
												break;
											}
										} else if (j == 1) {
											if (cbIndex[j] > 0) {
												cbIndex[j]--;
											} else {
												can_combime = false;
												break;
											}
										}
									}

									if (can_combime) {
										if (cbKindItemCount >= kindItem.length) {
											return false;
										}

										kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_YAO_JIU;
										kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

										kindItem[cbKindItemCount].cbValidIndex[0] = TIAO_LAST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[1] = TIAO_LAST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[2] = TIAO_FIRST_INDEX;

										cbKindItemCount++;
									} else {
										break;
									}
								}
							}
						}

						{
							// 24：9筒 9筒 1条
							int chi_count = cbCardIndexTemp[TONG_LAST_INDEX] + cbCardIndexTemp[TIAO_FIRST_INDEX];

							if (chi_count >= 3) {
								int cbIndex[] = { cbCardIndexTemp[TONG_LAST_INDEX], cbCardIndexTemp[TIAO_FIRST_INDEX] };

								boolean can_combime = true;

								while (cbIndex[0] + cbIndex[1] >= 3) {
									for (int j = 0; j < cbIndex.length; j++) {
										if (j == 0) {
											if (cbIndex[j] > 1) {
												cbIndex[j]--;
												cbIndex[j]--;
											} else {
												can_combime = false;
												break;
											}
										} else if (j == 1) {
											if (cbIndex[j] > 0) {
												cbIndex[j]--;
											} else {
												can_combime = false;
												break;
											}
										}
									}

									if (can_combime) {
										if (cbKindItemCount >= kindItem.length) {
											return false;
										}

										kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_YAO_JIU;
										kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

										kindItem[cbKindItemCount].cbValidIndex[0] = TONG_LAST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[1] = TONG_LAST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[2] = TIAO_FIRST_INDEX;

										cbKindItemCount++;
									} else {
										break;
									}
								}
							}
						}

						{
							// 25：9万 9条 1条
							int chi_count = cbCardIndexTemp[WAN_LAST_INDEX] + cbCardIndexTemp[TIAO_LAST_INDEX] + cbCardIndexTemp[TIAO_FIRST_INDEX];

							if (chi_count >= 3) {
								int cbIndex[] = { cbCardIndexTemp[WAN_LAST_INDEX], cbCardIndexTemp[TIAO_LAST_INDEX],
										cbCardIndexTemp[TIAO_FIRST_INDEX] };

								boolean can_combime = true;

								while (cbIndex[0] + cbIndex[1] + cbIndex[2] >= 3) {
									for (int j = 0; j < cbIndex.length; j++) {
										if (cbIndex[j] > 0) {
											cbIndex[j]--;
										} else {
											can_combime = false;
											break;
										}
									}

									if (can_combime) {
										if (cbKindItemCount >= kindItem.length) {
											return false;
										}

										kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_YAO_JIU;
										kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

										kindItem[cbKindItemCount].cbValidIndex[0] = WAN_LAST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[1] = TIAO_LAST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[2] = TIAO_FIRST_INDEX;

										cbKindItemCount++;
									} else {
										break;
									}
								}
							}
						}

						{
							// 26：9万 9筒 1条
							int chi_count = cbCardIndexTemp[WAN_LAST_INDEX] + cbCardIndexTemp[TONG_LAST_INDEX] + cbCardIndexTemp[TIAO_FIRST_INDEX];

							if (chi_count >= 3) {
								int cbIndex[] = { cbCardIndexTemp[WAN_LAST_INDEX], cbCardIndexTemp[TONG_LAST_INDEX],
										cbCardIndexTemp[TIAO_FIRST_INDEX] };

								boolean can_combime = true;

								while (cbIndex[0] + cbIndex[1] + cbIndex[2] >= 3) {
									for (int j = 0; j < cbIndex.length; j++) {
										if (cbIndex[j] > 0) {
											cbIndex[j]--;
										} else {
											can_combime = false;
											break;
										}
									}

									if (can_combime) {
										if (cbKindItemCount >= kindItem.length) {
											return false;
										}

										kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_YAO_JIU;
										kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

										kindItem[cbKindItemCount].cbValidIndex[0] = WAN_LAST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[1] = TONG_LAST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[2] = TIAO_FIRST_INDEX;

										cbKindItemCount++;
									} else {
										break;
									}
								}
							}
						}

						{
							// 27：9条 9筒 1条
							int chi_count = cbCardIndexTemp[TIAO_LAST_INDEX] + cbCardIndexTemp[TONG_LAST_INDEX] + cbCardIndexTemp[TIAO_FIRST_INDEX];

							if (chi_count >= 3) {
								int cbIndex[] = { cbCardIndexTemp[TIAO_LAST_INDEX], cbCardIndexTemp[TONG_LAST_INDEX],
										cbCardIndexTemp[TIAO_FIRST_INDEX] };

								boolean can_combime = true;

								while (cbIndex[0] + cbIndex[1] + cbIndex[2] >= 3) {
									for (int j = 0; j < cbIndex.length; j++) {
										if (cbIndex[j] > 0) {
											cbIndex[j]--;
										} else {
											can_combime = false;
											break;
										}
									}

									if (can_combime) {
										if (cbKindItemCount >= kindItem.length) {
											return false;
										}

										kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_YAO_JIU;
										kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

										kindItem[cbKindItemCount].cbValidIndex[0] = TIAO_LAST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[1] = TONG_LAST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[2] = TIAO_FIRST_INDEX;

										cbKindItemCount++;
									} else {
										break;
									}
								}
							}
						}
					}

					if (i == TONG_FIRST_INDEX) {
						{
							// 28：1筒 1筒 9万
							int chi_count = cbCardIndexTemp[TONG_FIRST_INDEX] + cbCardIndexTemp[WAN_LAST_INDEX];

							if (chi_count >= 3) {
								int cbIndex[] = { cbCardIndexTemp[TONG_FIRST_INDEX], cbCardIndexTemp[WAN_LAST_INDEX] };

								boolean can_combime = true;

								while (cbIndex[0] + cbIndex[1] >= 3) {
									for (int j = 0; j < cbIndex.length; j++) {
										if (j == 0) {
											if (cbIndex[j] > 1) {
												cbIndex[j]--;
												cbIndex[j]--;
											} else {
												can_combime = false;
												break;
											}
										} else if (j == 1) {
											if (cbIndex[j] > 0) {
												cbIndex[j]--;
											} else {
												can_combime = false;
												break;
											}
										}
									}

									if (can_combime) {
										if (cbKindItemCount >= kindItem.length) {
											return false;
										}

										kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_YAO_JIU;
										kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

										kindItem[cbKindItemCount].cbValidIndex[0] = TONG_FIRST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[1] = TONG_FIRST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[2] = WAN_LAST_INDEX;

										cbKindItemCount++;
									} else {
										break;
									}
								}
							}
						}

						{
							// 29：1筒 1筒 9条
							int chi_count = cbCardIndexTemp[TONG_FIRST_INDEX] + cbCardIndexTemp[TIAO_LAST_INDEX];

							if (chi_count >= 3) {
								int cbIndex[] = { cbCardIndexTemp[TONG_FIRST_INDEX], cbCardIndexTemp[TIAO_LAST_INDEX] };

								boolean can_combime = true;

								while (cbIndex[0] + cbIndex[1] >= 3) {
									for (int j = 0; j < cbIndex.length; j++) {
										if (j == 0) {
											if (cbIndex[j] > 1) {
												cbIndex[j]--;
												cbIndex[j]--;
											} else {
												can_combime = false;
												break;
											}
										} else if (j == 1) {
											if (cbIndex[j] > 0) {
												cbIndex[j]--;
											} else {
												can_combime = false;
												break;
											}
										}
									}

									if (can_combime) {
										if (cbKindItemCount >= kindItem.length) {
											return false;
										}

										kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_YAO_JIU;
										kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

										kindItem[cbKindItemCount].cbValidIndex[0] = TONG_FIRST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[1] = TONG_FIRST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[2] = TIAO_LAST_INDEX;

										cbKindItemCount++;
									} else {
										break;
									}
								}
							}
						}

						{
							// 30：1筒 1筒 9筒
							int chi_count = cbCardIndexTemp[TONG_FIRST_INDEX] + cbCardIndexTemp[TONG_LAST_INDEX];

							if (chi_count >= 3) {
								int cbIndex[] = { cbCardIndexTemp[TONG_FIRST_INDEX], cbCardIndexTemp[TONG_LAST_INDEX] };

								boolean can_combime = true;

								while (cbIndex[0] + cbIndex[1] >= 3) {
									for (int j = 0; j < cbIndex.length; j++) {
										if (j == 0) {
											if (cbIndex[j] > 1) {
												cbIndex[j]--;
												cbIndex[j]--;
											} else {
												can_combime = false;
												break;
											}
										} else if (j == 1) {
											if (cbIndex[j] > 0) {
												cbIndex[j]--;
											} else {
												can_combime = false;
												break;
											}
										}
									}

									if (can_combime) {
										if (cbKindItemCount >= kindItem.length) {
											return false;
										}

										kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_YAO_JIU;
										kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

										kindItem[cbKindItemCount].cbValidIndex[0] = TONG_FIRST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[1] = TONG_FIRST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[2] = TONG_LAST_INDEX;

										cbKindItemCount++;
									} else {
										break;
									}
								}
							}
						}

						{
							// 31：9万 9万 1筒
							int chi_count = cbCardIndexTemp[WAN_LAST_INDEX] + cbCardIndexTemp[TONG_FIRST_INDEX];

							if (chi_count >= 3) {
								int cbIndex[] = { cbCardIndexTemp[WAN_LAST_INDEX], cbCardIndexTemp[TONG_FIRST_INDEX] };

								boolean can_combime = true;

								while (cbIndex[0] + cbIndex[1] >= 3) {
									for (int j = 0; j < cbIndex.length; j++) {
										if (j == 0) {
											if (cbIndex[j] > 1) {
												cbIndex[j]--;
												cbIndex[j]--;
											} else {
												can_combime = false;
												break;
											}
										} else if (j == 1) {
											if (cbIndex[j] > 0) {
												cbIndex[j]--;
											} else {
												can_combime = false;
												break;
											}
										}
									}

									if (can_combime) {
										if (cbKindItemCount >= kindItem.length) {
											return false;
										}

										kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_YAO_JIU;
										kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

										kindItem[cbKindItemCount].cbValidIndex[0] = WAN_LAST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[1] = WAN_LAST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[2] = TONG_FIRST_INDEX;

										cbKindItemCount++;
									} else {
										break;
									}
								}
							}
						}

						{
							// 32：9条 9条 1筒
							int chi_count = cbCardIndexTemp[TIAO_LAST_INDEX] + cbCardIndexTemp[TONG_FIRST_INDEX];

							if (chi_count >= 3) {
								int cbIndex[] = { cbCardIndexTemp[TIAO_LAST_INDEX], cbCardIndexTemp[TONG_FIRST_INDEX] };

								boolean can_combime = true;

								while (cbIndex[0] + cbIndex[1] >= 3) {
									for (int j = 0; j < cbIndex.length; j++) {
										if (j == 0) {
											if (cbIndex[j] > 1) {
												cbIndex[j]--;
												cbIndex[j]--;
											} else {
												can_combime = false;
												break;
											}
										} else if (j == 1) {
											if (cbIndex[j] > 0) {
												cbIndex[j]--;
											} else {
												can_combime = false;
												break;
											}
										}
									}

									if (can_combime) {
										if (cbKindItemCount >= kindItem.length) {
											return false;
										}

										kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_YAO_JIU;
										kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

										kindItem[cbKindItemCount].cbValidIndex[0] = TIAO_LAST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[1] = TIAO_LAST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[2] = TONG_FIRST_INDEX;

										cbKindItemCount++;
									} else {
										break;
									}
								}
							}
						}

						{
							// 33：9筒 9筒 1筒
							int chi_count = cbCardIndexTemp[TONG_LAST_INDEX] + cbCardIndexTemp[TONG_FIRST_INDEX];

							if (chi_count >= 3) {
								int cbIndex[] = { cbCardIndexTemp[TONG_LAST_INDEX], cbCardIndexTemp[TONG_FIRST_INDEX] };

								boolean can_combime = true;

								while (cbIndex[0] + cbIndex[1] >= 3) {
									for (int j = 0; j < cbIndex.length; j++) {
										if (j == 0) {
											if (cbIndex[j] > 1) {
												cbIndex[j]--;
												cbIndex[j]--;
											} else {
												can_combime = false;
												break;
											}
										} else if (j == 1) {
											if (cbIndex[j] > 0) {
												cbIndex[j]--;
											} else {
												can_combime = false;
												break;
											}
										}
									}

									if (can_combime) {
										if (cbKindItemCount >= kindItem.length) {
											return false;
										}

										kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_YAO_JIU;
										kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

										kindItem[cbKindItemCount].cbValidIndex[0] = TONG_LAST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[1] = TONG_LAST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[2] = TONG_FIRST_INDEX;

										cbKindItemCount++;
									} else {
										break;
									}
								}
							}
						}

						{
							// 34：9万 9条 1筒
							int chi_count = cbCardIndexTemp[WAN_LAST_INDEX] + cbCardIndexTemp[TIAO_LAST_INDEX] + cbCardIndexTemp[TONG_FIRST_INDEX];

							if (chi_count >= 3) {
								int cbIndex[] = { cbCardIndexTemp[WAN_LAST_INDEX], cbCardIndexTemp[TIAO_LAST_INDEX],
										cbCardIndexTemp[TONG_FIRST_INDEX] };

								boolean can_combime = true;

								while (cbIndex[0] + cbIndex[1] + cbIndex[2] >= 3) {
									for (int j = 0; j < cbIndex.length; j++) {
										if (cbIndex[j] > 0) {
											cbIndex[j]--;
										} else {
											can_combime = false;
											break;
										}
									}

									if (can_combime) {
										if (cbKindItemCount >= kindItem.length) {
											return false;
										}

										kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_YAO_JIU;
										kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

										kindItem[cbKindItemCount].cbValidIndex[0] = WAN_LAST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[1] = TIAO_LAST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[2] = TONG_FIRST_INDEX;

										cbKindItemCount++;
									} else {
										break;
									}
								}
							}
						}

						{
							// 35：9万 9筒 1筒
							int chi_count = cbCardIndexTemp[WAN_LAST_INDEX] + cbCardIndexTemp[TONG_LAST_INDEX] + cbCardIndexTemp[TONG_FIRST_INDEX];

							if (chi_count >= 3) {
								int cbIndex[] = { cbCardIndexTemp[WAN_LAST_INDEX], cbCardIndexTemp[TONG_LAST_INDEX],
										cbCardIndexTemp[TONG_FIRST_INDEX] };

								boolean can_combime = true;

								while (cbIndex[0] + cbIndex[1] + cbIndex[2] >= 3) {
									for (int j = 0; j < cbIndex.length; j++) {
										if (cbIndex[j] > 0) {
											cbIndex[j]--;
										} else {
											can_combime = false;
											break;
										}
									}

									if (can_combime) {
										if (cbKindItemCount >= kindItem.length) {
											return false;
										}

										kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_YAO_JIU;
										kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

										kindItem[cbKindItemCount].cbValidIndex[0] = WAN_LAST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[1] = TONG_LAST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[2] = TONG_FIRST_INDEX;

										cbKindItemCount++;
									} else {
										break;
									}
								}
							}
						}

						{
							// 36：9条 9筒 1筒
							int chi_count = cbCardIndexTemp[TIAO_LAST_INDEX] + cbCardIndexTemp[TONG_LAST_INDEX] + cbCardIndexTemp[TONG_FIRST_INDEX];

							if (chi_count >= 3) {
								int cbIndex[] = { cbCardIndexTemp[TIAO_LAST_INDEX], cbCardIndexTemp[TONG_LAST_INDEX],
										cbCardIndexTemp[TONG_FIRST_INDEX] };

								boolean can_combime = true;

								while (cbIndex[0] + cbIndex[1] + cbIndex[2] >= 3) {
									for (int j = 0; j < cbIndex.length; j++) {
										if (cbIndex[j] > 0) {
											cbIndex[j]--;
										} else {
											can_combime = false;
											break;
										}
									}

									if (can_combime) {
										if (cbKindItemCount >= kindItem.length) {
											return false;
										}

										kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_YAO_JIU;
										kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

										kindItem[cbKindItemCount].cbValidIndex[0] = TIAO_LAST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[1] = TONG_LAST_INDEX;
										kindItem[cbKindItemCount].cbValidIndex[2] = TONG_FIRST_INDEX;

										cbKindItemCount++;
									} else {
										break;
									}
								}
							}
						}
					}
				}
			}
		}

		if (cbKindItemCount >= cbLessKindItem) {
			int cbIndex[] = new int[] { 0, 1, 2, 3 };

			KindItem pKindItem[] = new KindItem[4];

			for (int i = 0; i < 4; i++) {
				pKindItem[i] = new KindItem();
			}

			do {
				int mj_count = GameConstants.MAX_ZI_FENG;

				for (int i = 0; i < mj_count; i++) {
					cbCardIndexTemp[i] = cards_index[i];
				}

				for (int i = 0; i < cbLessKindItem; i++) {
					pKindItem[i].cbWeaveKind = kindItem[cbIndex[i]].cbWeaveKind;
					pKindItem[i].cbCenterCard = kindItem[cbIndex[i]].cbCenterCard;

					for (int j = 0; j < 3; j++) {
						pKindItem[i].cbValidIndex[j] = kindItem[cbIndex[i]].cbValidIndex[j];
					}
				}

				boolean bEnoughCard = true;

				for (int i = 0; i < cbLessKindItem * 3; i++) {
					int cbCardIndex = pKindItem[i / 3].cbValidIndex[i % 3];

					if (cbCardIndexTemp[cbCardIndex] == 0) {
						bEnoughCard = false;
						break;
					} else {
						cbCardIndexTemp[cbCardIndex]--;
					}
				}

				if (bEnoughCard == true) {
					int cbCardEye = 0;

					for (int i = 0; i < mj_count; i++) {
						if (cbCardIndexTemp[i] == 2) {
							cbCardEye = switch_to_card_data(i);
							break;
						}
					}

					if (cbCardEye != 0) {
						AnalyseItem analyseItem = new AnalyseItem();

						for (int i = 0; i < cbWeaveCount; i++) {
							analyseItem.cbWeaveKind[i] = weaveItem[i].weave_kind;
							analyseItem.cbCenterCard[i] = weaveItem[i].center_card;
						}

						for (int i = 0; i < cbLessKindItem; i++) {
							analyseItem.cbWeaveKind[i + cbWeaveCount] = pKindItem[i].cbWeaveKind;
							analyseItem.cbCenterCard[i + cbWeaveCount] = pKindItem[i].cbCenterCard;
						}

						analyseItemArray.add(analyseItem);
					}
				}

				if (cbIndex[cbLessKindItem - 1] == (cbKindItemCount - 1)) {
					int i = cbLessKindItem - 1;

					for (; i > 0; i--) {
						if ((cbIndex[i - 1] + 1) != cbIndex[i]) {
							int cbNewIndex = cbIndex[i - 1];

							for (int j = (i - 1); j < cbLessKindItem; j++)
								cbIndex[j] = cbNewIndex + j - i + 2;

							break;
						}
					}

					if (i == 0) {
						break;
					}
				} else {
					cbIndex[cbLessKindItem - 1]++;
				}
			} while (true);
		}

		int hand_card_count = this.get_card_count_by_index(cards_index);
		// 四对牌型判断，手牌数目为8张时，直接判断手牌是否是刚好四对；11张牌时，轮询剔除掉1个组合，剩下的牌还是否刚好四对；14张牌时，轮询剔除2个组合，看还是不是刚好4对，找到之后，马上跳出循环。
		if (hand_card_count == 8) {
			if (is_si_dui(cbCardIndexTemp)) {
				AnalyseItem analyseItem = new AnalyseItem();

				for (int i = 0; i < cbWeaveCount; i++) {
					analyseItem.cbWeaveKind[i] = weaveItem[i].weave_kind;
					analyseItem.cbCenterCard[i] = weaveItem[i].center_card;
				}

				analyseItem.cbWeaveKind[cbWeaveCount] = GameConstants.WIK_SI_DUI;

				analyseItemArray.add(analyseItem);
			}
		} else if (hand_card_count == 11 || hand_card_count == 14) {
			if (hand_card_count == 11) {
				cbLessKindItem = 1;
			} else {
				cbLessKindItem = 2;
			}

			if (cbKindItemCount >= cbLessKindItem) {
				int cbIndex[] = new int[] { 0, 1, 2, 3 };

				KindItem pKindItem[] = new KindItem[4];

				for (int i = 0; i < 4; i++) {
					pKindItem[i] = new KindItem();
				}

				do {
					int mj_count = GameConstants.MAX_ZI_FENG;

					for (int i = 0; i < mj_count; i++) {
						cbCardIndexTemp[i] = cards_index[i];
					}

					for (int i = 0; i < cbLessKindItem; i++) {
						pKindItem[i].cbWeaveKind = kindItem[cbIndex[i]].cbWeaveKind;
						pKindItem[i].cbCenterCard = kindItem[cbIndex[i]].cbCenterCard;

						for (int j = 0; j < 3; j++) {
							pKindItem[i].cbValidIndex[j] = kindItem[cbIndex[i]].cbValidIndex[j];
						}
					}

					boolean bEnoughCard = true;

					for (int i = 0; i < cbLessKindItem * 3; i++) {
						int cbCardIndex = pKindItem[i / 3].cbValidIndex[i % 3];

						if (cbCardIndexTemp[cbCardIndex] == 0) {
							bEnoughCard = false;
							break;
						} else {
							cbCardIndexTemp[cbCardIndex]--;
						}
					}

					if (bEnoughCard == true) {
						if (is_si_dui(cbCardIndexTemp)) {
							AnalyseItem analyseItem = new AnalyseItem();

							for (int i = 0; i < cbWeaveCount; i++) {
								analyseItem.cbWeaveKind[i] = weaveItem[i].weave_kind;
								analyseItem.cbCenterCard[i] = weaveItem[i].center_card;
							}

							for (int i = 0; i < cbLessKindItem; i++) {
								analyseItem.cbWeaveKind[i + cbWeaveCount] = pKindItem[i].cbWeaveKind;
								analyseItem.cbCenterCard[i + cbWeaveCount] = pKindItem[i].cbCenterCard;
							}

							analyseItem.cbWeaveKind[cbWeaveCount + cbLessKindItem] = GameConstants.WIK_SI_DUI;

							analyseItemArray.add(analyseItem);
						}
					}

					if (cbIndex[cbLessKindItem - 1] == (cbKindItemCount - 1)) {
						int i = cbLessKindItem - 1;

						for (; i > 0; i--) {
							if ((cbIndex[i - 1] + 1) != cbIndex[i]) {
								int cbNewIndex = cbIndex[i - 1];

								for (int j = (i - 1); j < cbLessKindItem; j++)
									cbIndex[j] = cbNewIndex + j - i + 2;

								break;
							}
						}

						if (i == 0) {
							break;
						}
					} else {
						cbIndex[cbLessKindItem - 1]++;
					}
				} while (true);
			}
		}

		return (analyseItemArray.size() > 0 ? true : false);
	}

	public boolean is_si_dui(int[] cards_index) {
		int dui_zi_count = 0;

		for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
			if (cards_index[i] == 2) {
				dui_zi_count += 1;
			} else if (cards_index[i] == 4) {
				dui_zi_count += 2;
			}
		}

		return dui_zi_count == 4 ? true : false;
	}

	public boolean analyse_card_ji_xian(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, List<AnalyseItem> analyseItemArray) {
		int cbCardCount = get_card_count_by_index(cards_index);
		int cbKindItemCount = 0;
		int cbLessKindItem = (cbCardCount - 2) / 3;

		if (cbLessKindItem == 0) {
			for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
				if (cards_index[i] == 2) {
					AnalyseItem analyseItem = new AnalyseItem();

					for (int j = 0; j < cbWeaveCount; j++) {
						analyseItem.cbWeaveKind[j] = weaveItem[j].weave_kind;
						analyseItem.cbCenterCard[j] = weaveItem[j].center_card;
						get_weave_card(weaveItem[j].weave_kind, weaveItem[j].center_card, analyseItem.cbCardData[j]);
					}

					analyseItem.cbCardEye = switch_to_card_data(i);

					analyseItemArray.add(analyseItem);

					return true;
				}
			}

			return false;
		}

		int cbTmpCardIndex[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbTmpCardIndex[i] = cards_index[i];
		}

		KindItem kindItem[] = new KindItem[27 * 9 + 7 + 14];
		for (int i = 0; i < kindItem.length; i++) {
			kindItem[i] = new KindItem();
		}

		if (cbCardCount >= 3) {
			for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
				if (cbTmpCardIndex[i] >= 3) {
					if (cbKindItemCount >= kindItem.length) {
						return false;
					}

					kindItem[cbKindItemCount].cbCardIndex[0] = i;
					kindItem[cbKindItemCount].cbCardIndex[1] = i;
					kindItem[cbKindItemCount].cbCardIndex[2] = i;

					int center_card = switch_to_card_data(i);

					if (get_card_value(center_card) == 5 && get_card_color(center_card) < 3) {
						kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_PENG_CENTER_FIVE;
					} else {
						kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_PENG;
					}

					kindItem[cbKindItemCount].cbCenterCard = center_card;

					kindItem[cbKindItemCount].cbValidIndex[0] = cbTmpCardIndex[i] > 0 ? i : get_magic_card_index(0);
					kindItem[cbKindItemCount].cbValidIndex[1] = cbTmpCardIndex[i] > 1 ? i : get_magic_card_index(0);
					kindItem[cbKindItemCount].cbValidIndex[2] = cbTmpCardIndex[i] > 2 ? i : get_magic_card_index(0);

					cbKindItemCount++;
				}

				if ((i < (GameConstants.MAX_ZI - 2)) && ((i % 9) < 7)) {
					int chi_count = cbTmpCardIndex[i] + cbTmpCardIndex[i + 1] + cbTmpCardIndex[i + 2];

					if (chi_count >= 3) {
						int cbIndex[] = { cbTmpCardIndex[i], cbTmpCardIndex[i + 1], cbTmpCardIndex[i + 2] };

						int cbValidIndex[] = new int[3];

						while (cbIndex[0] + cbIndex[1] + cbIndex[2] >= 3) {
							boolean has_enough_card = true;

							for (int j = 0; j < cbIndex.length; j++) {
								if (cbIndex[j] > 0) {
									cbIndex[j]--;
									cbValidIndex[j] = i + j;
								} else {
									has_enough_card = false;
									break;
								}
							}

							if (has_enough_card) {
								if (cbKindItemCount >= kindItem.length) {
									return false;
								}

								kindItem[cbKindItemCount].cbCardIndex[0] = i;
								kindItem[cbKindItemCount].cbCardIndex[1] = i + 1;
								kindItem[cbKindItemCount].cbCardIndex[2] = i + 2;

								int center_card = switch_to_card_data(i);

								if (get_card_value(center_card) == 4) {
									kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_CHI_CENTER_FIVE;
								} else {
									kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_LEFT;
								}

								kindItem[cbKindItemCount].cbCenterCard = center_card;

								for (int cbValidIndex_index = 0; cbValidIndex_index < 3; cbValidIndex_index++) {
									kindItem[cbKindItemCount].cbValidIndex[cbValidIndex_index] = cbValidIndex[cbValidIndex_index];
								}

								cbKindItemCount++;
							} else {
								break;
							}
						}
					}
				}

				// 风吃、风成句
				// cbSubWeaveKind:
				// 1 东西南
				// 2 东西北
				// 3 东南北
				// 4 西南北
				if (i == INDEX_DONG_FENG) {
					{
						// 东西南
						int chi_count = cbTmpCardIndex[INDEX_DONG_FENG] + cbTmpCardIndex[INDEX_XI_FENG] + cbTmpCardIndex[INDEX_NAN_FENG];

						if (chi_count >= 3) {
							int cbIndex[] = { cbTmpCardIndex[INDEX_DONG_FENG], cbTmpCardIndex[INDEX_XI_FENG], cbTmpCardIndex[INDEX_NAN_FENG] };

							boolean can_combime = true;

							while (cbIndex[0] + cbIndex[1] + cbIndex[2] >= 3) {
								for (int j = 0; j < cbIndex.length; j++) {
									if (cbIndex[j] > 0) {
										cbIndex[j]--;
									} else {
										can_combime = false;
										break;
									}
								}

								if (can_combime) {
									if (cbKindItemCount >= kindItem.length) {
										return false;
									}

									kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_FENG_CHI;
									kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

									kindItem[cbKindItemCount].cbValidIndex[0] = INDEX_DONG_FENG;
									kindItem[cbKindItemCount].cbValidIndex[1] = INDEX_XI_FENG;
									kindItem[cbKindItemCount].cbValidIndex[2] = INDEX_NAN_FENG;

									cbKindItemCount++;
								} else {
									break;
								}
							}
						}
					}

					{
						// 东西北
						int chi_count = cbTmpCardIndex[INDEX_DONG_FENG] + cbTmpCardIndex[INDEX_XI_FENG] + cbTmpCardIndex[INDEX_BEI_FENG];

						if (chi_count >= 3) {
							int cbIndex[] = { cbTmpCardIndex[INDEX_DONG_FENG], cbTmpCardIndex[INDEX_XI_FENG], cbTmpCardIndex[INDEX_BEI_FENG] };

							boolean can_combime = true;

							while (cbIndex[0] + cbIndex[1] + cbIndex[2] >= 3) {
								for (int j = 0; j < cbIndex.length; j++) {
									if (cbIndex[j] > 0) {
										cbIndex[j]--;
									} else {
										can_combime = false;
										break;
									}
								}

								if (can_combime) {
									if (cbKindItemCount >= kindItem.length) {
										return false;
									}

									kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_FENG_CHI;
									kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

									kindItem[cbKindItemCount].cbValidIndex[0] = INDEX_DONG_FENG;
									kindItem[cbKindItemCount].cbValidIndex[1] = INDEX_XI_FENG;
									kindItem[cbKindItemCount].cbValidIndex[2] = INDEX_BEI_FENG;

									cbKindItemCount++;
								} else {
									break;
								}
							}
						}
					}

					{
						// 东南北
						int chi_count = cbTmpCardIndex[INDEX_DONG_FENG] + cbTmpCardIndex[INDEX_NAN_FENG] + cbTmpCardIndex[INDEX_BEI_FENG];

						if (chi_count >= 3) {
							int cbIndex[] = { cbTmpCardIndex[INDEX_DONG_FENG], cbTmpCardIndex[INDEX_NAN_FENG], cbTmpCardIndex[INDEX_BEI_FENG] };

							boolean can_combime = true;

							while (cbIndex[0] + cbIndex[1] + cbIndex[2] >= 3) {
								for (int j = 0; j < cbIndex.length; j++) {
									if (cbIndex[j] > 0) {
										cbIndex[j]--;
									} else {
										can_combime = false;
										break;
									}
								}

								if (can_combime) {
									if (cbKindItemCount >= kindItem.length) {
										return false;
									}

									kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_FENG_CHI;
									kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

									kindItem[cbKindItemCount].cbValidIndex[0] = INDEX_DONG_FENG;
									kindItem[cbKindItemCount].cbValidIndex[1] = INDEX_NAN_FENG;
									kindItem[cbKindItemCount].cbValidIndex[2] = INDEX_BEI_FENG;

									cbKindItemCount++;
								} else {
									break;
								}
							}
						}
					}
				}
				if (i == INDEX_XI_FENG) {
					// 西南北
					int chi_count = cbTmpCardIndex[INDEX_XI_FENG] + cbTmpCardIndex[INDEX_NAN_FENG] + cbTmpCardIndex[INDEX_BEI_FENG];

					if (chi_count >= 3) {
						int cbIndex[] = { cbTmpCardIndex[INDEX_XI_FENG], cbTmpCardIndex[INDEX_NAN_FENG], cbTmpCardIndex[INDEX_BEI_FENG] };

						boolean can_combime = true;

						while (cbIndex[0] + cbIndex[1] + cbIndex[2] >= 3) {
							for (int j = 0; j < cbIndex.length; j++) {
								if (cbIndex[j] > 0) {
									cbIndex[j]--;
								} else {
									can_combime = false;
									break;
								}
							}

							if (can_combime) {
								if (cbKindItemCount >= kindItem.length) {
									return false;
								}

								kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_FENG_CHI;
								kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

								kindItem[cbKindItemCount].cbValidIndex[0] = INDEX_XI_FENG;
								kindItem[cbKindItemCount].cbValidIndex[1] = INDEX_NAN_FENG;
								kindItem[cbKindItemCount].cbValidIndex[2] = INDEX_BEI_FENG;

								cbKindItemCount++;
							} else {
								break;
							}
						}
					}
				}
				if (i == INDEX_ZHONG_FENG) {
					// 中发白
					int chi_count = cbTmpCardIndex[INDEX_ZHONG_FENG] + cbTmpCardIndex[INDEX_FA_FENG] + cbTmpCardIndex[INDEX_BAI_FENG];

					if (chi_count >= 3) {
						int cbIndex[] = { cbTmpCardIndex[INDEX_ZHONG_FENG], cbTmpCardIndex[INDEX_FA_FENG], cbTmpCardIndex[INDEX_BAI_FENG] };

						boolean can_combime = true;

						while (cbIndex[0] + cbIndex[1] + cbIndex[2] >= 3) {
							for (int j = 0; j < cbIndex.length; j++) {
								if (cbIndex[j] > 0) {
									cbIndex[j]--;
								} else {
									can_combime = false;
									break;
								}
							}

							if (can_combime) {
								if (cbKindItemCount >= kindItem.length) {
									return false;
								}

								kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_FENG_CHI;
								kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

								kindItem[cbKindItemCount].cbValidIndex[0] = INDEX_ZHONG_FENG;
								kindItem[cbKindItemCount].cbValidIndex[1] = INDEX_FA_FENG;
								kindItem[cbKindItemCount].cbValidIndex[2] = INDEX_BAI_FENG;

								cbKindItemCount++;
							} else {
								break;
							}
						}
					}
				}

				int first_index = -1;
				int last_index = -1;

				if (i < GameConstants.MAX_ZI && get_card_value(switch_to_card_data(i)) != 1 && get_card_value(switch_to_card_data(i)) != 9) {
					if (i < 9) {
						first_index = 1;
						last_index = 7;
					} else if (i < 18) {
						first_index = 10;
						last_index = 16;
					} else if (i < 27) {
						first_index = 19;
						last_index = 25;
					}

					if (i == first_index) {
						int chi_count = cbTmpCardIndex[first_index] + cbTmpCardIndex[last_index];

						if (chi_count >= 3) {
							int cbIndex[] = { cbTmpCardIndex[first_index], cbTmpCardIndex[last_index] };

							int cbValidIndex[] = new int[3];

							while (cbIndex[0] + cbIndex[1] >= 3) {
								boolean has_enough_card = true;

								for (int j = 0; j < 3; j++) {
									if (j == 0 || j == 1) {
										if (cbIndex[0] > 0) {
											cbIndex[0]--;
											cbValidIndex[j] = first_index;
										} else {
											has_enough_card = false;
											break;
										}
									} else if (j == 2) {
										if (cbIndex[1] > 0) {
											cbIndex[1]--;
											cbValidIndex[j] = last_index;
										} else {
											has_enough_card = false;
											break;
										}
									}
								}

								if (has_enough_card) {
									if (cbKindItemCount >= kindItem.length) {
										return false;
									}

									kindItem[cbKindItemCount].cbCardIndex[0] = first_index;
									kindItem[cbKindItemCount].cbCardIndex[1] = first_index;
									kindItem[cbKindItemCount].cbCardIndex[2] = last_index;

									kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_TWO_EIGHT_MIX;

									kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

									for (int cbValidIndex_index = 0; cbValidIndex_index < 3; cbValidIndex_index++) {
										kindItem[cbKindItemCount].cbValidIndex[cbValidIndex_index] = cbValidIndex[cbValidIndex_index];
									}

									cbKindItemCount++;
								} else {
									break;
								}
							}
						}
					} else if (i == last_index) {
						int chi_count = cbTmpCardIndex[first_index] + cbTmpCardIndex[last_index];

						if (chi_count >= 3) {
							int cbIndex[] = { cbTmpCardIndex[first_index], cbTmpCardIndex[last_index] };

							int cbValidIndex[] = new int[3];

							while (cbIndex[0] + cbIndex[1] >= 3) {
								boolean has_enough_card = true;

								for (int j = 0; j < 3; j++) {
									if (j == 0) {
										if (cbIndex[0] > 0) {
											cbIndex[0]--;
											cbValidIndex[j] = first_index;
										} else {
											has_enough_card = false;
											break;
										}
									} else if (j == 1 || j == 2) {
										if (cbIndex[1] > 0) {
											cbIndex[1]--;
											cbValidIndex[j] = last_index;
										} else {
											has_enough_card = false;
											break;
										}
									}
								}

								if (has_enough_card) {
									if (cbKindItemCount >= kindItem.length) {
										return false;
									}

									kindItem[cbKindItemCount].cbCardIndex[0] = first_index;
									kindItem[cbKindItemCount].cbCardIndex[1] = last_index;
									kindItem[cbKindItemCount].cbCardIndex[2] = last_index;

									kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_TWO_EIGHT_MIX;

									kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

									for (int cbValidIndex_index = 0; cbValidIndex_index < 3; cbValidIndex_index++) {
										kindItem[cbKindItemCount].cbValidIndex[cbValidIndex_index] = cbValidIndex[cbValidIndex_index];
									}

									cbKindItemCount++;
								} else {
									break;
								}
							}
						}
					} else if (first_index != -1 && last_index != -1 && i != first_index && i != last_index) {
						int chi_count = cbTmpCardIndex[i] + cbTmpCardIndex[first_index] + cbTmpCardIndex[last_index];

						if (chi_count >= 3) {
							int cbIndex[] = { cbTmpCardIndex[i], cbTmpCardIndex[first_index], cbTmpCardIndex[last_index] };

							int cbValidIndex[] = new int[3];

							while (cbIndex[0] + cbIndex[1] + cbIndex[2] >= 3) {
								boolean has_enough_card = true;

								for (int j = 0; j < cbIndex.length; j++) {
									if (cbIndex[j] > 0) {
										cbIndex[j]--;

										if (j == 0)
											cbValidIndex[j] = i;
										if (j == 1)
											cbValidIndex[j] = first_index;
										if (j == 2)
											cbValidIndex[j] = last_index;
									} else {
										has_enough_card = false;
										break;
									}
								}

								if (has_enough_card) {
									if (cbKindItemCount >= kindItem.length) {
										return false;
									}

									kindItem[cbKindItemCount].cbCardIndex[0] = i;
									kindItem[cbKindItemCount].cbCardIndex[1] = first_index;
									kindItem[cbKindItemCount].cbCardIndex[2] = last_index;

									kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_TWO_EIGHT_MIX;

									kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

									for (int cbValidIndex_index = 0; cbValidIndex_index < 3; cbValidIndex_index++) {
										kindItem[cbKindItemCount].cbValidIndex[cbValidIndex_index] = cbValidIndex[cbValidIndex_index];
									}

									cbKindItemCount++;
								} else {
									break;
								}
							}
						}
					}
				}

				if (i < GameConstants.MAX_ZI) {
					if (i < 9) {
						first_index = 0;
						last_index = 8;
					} else if (i < 18) {
						first_index = 9;
						last_index = 17;
					} else if (i < 27) {
						first_index = 18;
						last_index = 26;
					}

					if (i == first_index) {
						int chi_count = cbTmpCardIndex[first_index] + cbTmpCardIndex[last_index];

						if (chi_count >= 3) {
							int cbIndex[] = { cbTmpCardIndex[first_index], cbTmpCardIndex[last_index] };

							int cbValidIndex[] = new int[3];

							while (cbIndex[0] + cbIndex[1] >= 3) {
								boolean has_enough_card = true;

								for (int j = 0; j < 3; j++) {
									if (j == 0 || j == 1) {
										if (cbIndex[0] > 0) {
											cbIndex[0]--;
											cbValidIndex[j] = first_index;
										} else {
											has_enough_card = false;
											break;
										}
									} else if (j == 2) {
										if (cbIndex[1] > 0) {
											cbIndex[1]--;
											cbValidIndex[j] = last_index;
										} else {
											has_enough_card = false;
											break;
										}
									}
								}

								if (has_enough_card) {
									if (cbKindItemCount >= kindItem.length) {
										return false;
									}

									kindItem[cbKindItemCount].cbCardIndex[0] = first_index;
									kindItem[cbKindItemCount].cbCardIndex[1] = first_index;
									kindItem[cbKindItemCount].cbCardIndex[2] = last_index;

									kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_ONE_NINE_MIX;

									kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

									for (int cbValidIndex_index = 0; cbValidIndex_index < 3; cbValidIndex_index++) {
										kindItem[cbKindItemCount].cbValidIndex[cbValidIndex_index] = cbValidIndex[cbValidIndex_index];
									}

									cbKindItemCount++;
								} else {
									break;
								}
							}
						}
					} else if (i == last_index) {
						int chi_count = cbTmpCardIndex[first_index] + cbTmpCardIndex[last_index];

						if (chi_count >= 3) {
							int cbIndex[] = { cbTmpCardIndex[first_index], cbTmpCardIndex[last_index] };

							int cbValidIndex[] = new int[3];

							while (cbIndex[0] + cbIndex[1] >= 3) {
								boolean has_enough_card = true;

								for (int j = 0; j < 3; j++) {
									if (j == 0) {
										if (cbIndex[0] > 0) {
											cbIndex[0]--;
											cbValidIndex[j] = first_index;
										} else {
											has_enough_card = false;
											break;
										}
									} else if (j == 1 || j == 2) {
										if (cbIndex[1] > 0) {
											cbIndex[1]--;
											cbValidIndex[j] = last_index;
										} else {
											has_enough_card = false;
											break;
										}
									}
								}

								if (has_enough_card) {
									if (cbKindItemCount >= kindItem.length) {
										return false;
									}

									kindItem[cbKindItemCount].cbCardIndex[0] = first_index;
									kindItem[cbKindItemCount].cbCardIndex[1] = last_index;
									kindItem[cbKindItemCount].cbCardIndex[2] = last_index;

									kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_ONE_NINE_MIX;

									kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

									for (int cbValidIndex_index = 0; cbValidIndex_index < 3; cbValidIndex_index++) {
										kindItem[cbKindItemCount].cbValidIndex[cbValidIndex_index] = cbValidIndex[cbValidIndex_index];
									}

									cbKindItemCount++;
								} else {
									break;
								}
							}
						}
					} else if (first_index != -1 && last_index != -1 && i != first_index && i != last_index) {
						int chi_count = cbTmpCardIndex[i] + cbTmpCardIndex[first_index] + cbTmpCardIndex[last_index];

						if (chi_count >= 3) {
							int cbIndex[] = { cbTmpCardIndex[i], cbTmpCardIndex[first_index], cbTmpCardIndex[last_index] };

							int cbValidIndex[] = new int[3];

							while (cbIndex[0] + cbIndex[1] + cbIndex[2] >= 3) {
								boolean has_enough_card = true;

								for (int j = 0; j < cbIndex.length; j++) {
									if (cbIndex[j] > 0) {
										cbIndex[j]--;

										if (j == 0)
											cbValidIndex[j] = i;
										if (j == 1)
											cbValidIndex[j] = first_index;
										if (j == 2)
											cbValidIndex[j] = last_index;
									} else {
										has_enough_card = false;
										break;
									}
								}

								if (has_enough_card) {
									if (cbKindItemCount >= kindItem.length) {
										return false;
									}

									kindItem[cbKindItemCount].cbCardIndex[0] = i;
									kindItem[cbKindItemCount].cbCardIndex[1] = first_index;
									kindItem[cbKindItemCount].cbCardIndex[2] = last_index;

									kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_ONE_NINE_MIX;

									kindItem[cbKindItemCount].cbCenterCard = switch_to_card_data(i);

									for (int cbValidIndex_index = 0; cbValidIndex_index < 3; cbValidIndex_index++) {
										kindItem[cbKindItemCount].cbValidIndex[cbValidIndex_index] = cbValidIndex[cbValidIndex_index];
									}

									cbKindItemCount++;
								} else {
									break;
								}
							}
						}
					}
				}
			}
		}

		// 组合分析
		if (cbKindItemCount >= cbLessKindItem) {
			int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];

			int cbIndex[] = new int[] { 0, 1, 2, 3 };

			KindItem pKindItem[] = new KindItem[4];

			for (int i = 0; i < 4; i++) {
				pKindItem[i] = new KindItem();
			}

			do {
				for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
					cbCardIndexTemp[i] = cards_index[i];
				}

				for (int i = 0; i < cbLessKindItem; i++) {
					pKindItem[i].cbWeaveKind = kindItem[cbIndex[i]].cbWeaveKind;
					pKindItem[i].cbCenterCard = kindItem[cbIndex[i]].cbCenterCard;

					for (int j = 0; j < 3; j++) {
						pKindItem[i].cbCardIndex[j] = kindItem[cbIndex[i]].cbCardIndex[j];
						pKindItem[i].cbValidIndex[j] = kindItem[cbIndex[i]].cbValidIndex[j];
					}
				}

				boolean bEnoughCard = true;

				for (int i = 0; i < cbLessKindItem * 3; i++) {
					int cbCardIndex = pKindItem[i / 3].cbValidIndex[i % 3];

					if (cbCardIndexTemp[cbCardIndex] == 0) {
						bEnoughCard = false;
						break;
					} else {
						cbCardIndexTemp[cbCardIndex]--;
					}
				}

				if (bEnoughCard == true) {
					int cbCardEye = 0;

					for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
						if (cbCardIndexTemp[i] == 2) {
							cbCardEye = switch_to_card_data(i);
						}
					}

					if (cbCardEye != 0) {
						AnalyseItem analyseItem = new AnalyseItem();

						for (int i = 0; i < cbWeaveCount; i++) {
							analyseItem.cbWeaveKind[i] = weaveItem[i].weave_kind;
							analyseItem.cbCenterCard[i] = weaveItem[i].center_card;

							get_weave_card(weaveItem[i].weave_kind, weaveItem[i].center_card, analyseItem.cbCardData[i]);
						}

						for (int i = 0; i < cbLessKindItem; i++) {
							analyseItem.cbWeaveKind[i + cbWeaveCount] = pKindItem[i].cbWeaveKind;
							analyseItem.cbCenterCard[i + cbWeaveCount] = pKindItem[i].cbCenterCard;

							analyseItem.cbCardData[cbWeaveCount + i][0] = switch_to_card_data(pKindItem[i].cbValidIndex[0]);
							analyseItem.cbCardData[cbWeaveCount + i][1] = switch_to_card_data(pKindItem[i].cbValidIndex[1]);
							analyseItem.cbCardData[cbWeaveCount + i][2] = switch_to_card_data(pKindItem[i].cbValidIndex[2]);
						}

						analyseItem.cbCardEye = cbCardEye;

						analyseItemArray.add(analyseItem);
					}
				}

				if (cbIndex[cbLessKindItem - 1] == (cbKindItemCount - 1)) {
					int i = cbLessKindItem - 1;

					for (; i > 0; i--) {
						if ((cbIndex[i - 1] + 1) != cbIndex[i]) {
							int cbNewIndex = cbIndex[i - 1];

							for (int j = (i - 1); j < cbLessKindItem; j++)
								cbIndex[j] = cbNewIndex + j - i + 2;

							break;
						}
					}

					if (i == 0) {
						break;
					}
				} else {
					cbIndex[cbLessKindItem - 1]++;
				}
			} while (true);
		}

		return (analyseItemArray.size() > 0 ? true : false);
	}
}
