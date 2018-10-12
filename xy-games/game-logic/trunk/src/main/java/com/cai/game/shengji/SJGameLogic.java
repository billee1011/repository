package com.cai.game.shengji;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cai.common.constant.GameConstants;
import com.cai.common.util.RandomUtil;
import com.cai.game.shengji.data.tagAnalyseCardType;
import com.cai.game.shengji.data.tagAnalyseIndexResult_SJ;

public class SJGameLogic {
	private static Logger logger = Logger.getLogger(SJGameLogic.class);
	public Map<Integer, Integer> ruleMap = new HashMap<>();
	public int _zhu_type = GameConstants.INVALID_CARD;
	public int _zhu_value = 0x1000;
	public int _chang_zhu_one = 15;
	public int _chang_zhu_two = 14;
	public int _chang_zhu_three = 2;
	public int _chang_zhu_four = 2;

	/***
	 * 扑克转换--将实际数据 转换为 索引
	 * 
	 * @param card
	 * @return
	 */
	public void switch_to_card_index(int card_data[], int card_count, int card_index[]) {
		for (int i = 0; i < card_count; i++) {
			int index = GetCardLogicValue(card_data[i]);
			card_index[index - 3]++;
		}
	}

	public Map<Integer, Integer> getRuleMap() {
		return ruleMap;
	}

	public void setRuleMap(Map<Integer, Integer> ruleMap) {
		this.ruleMap = ruleMap;
	}

	public int get_card_index(int card_data) {
		int index = GetCardLogicValue(card_data);
		if ((card_data > this._zhu_value && card_data < 0x3000 && this.GetCardValue(card_data) == 2)
				|| this.GetCardValue(card_data) == this._chang_zhu_one
				|| this.GetCardValue(card_data) == this._chang_zhu_two) {
			return index - 2;
		}
		return index - 3;
	}

	public int get_index_value(int index) {
		return index + 3;
	}

	// 分析扑克
	public void AnalysebCardDataToIndex_No_Zhu(int cbCardData[], int cbCardCount,
			tagAnalyseIndexResult_SJ AnalyseIndexResult) {
		int card_index[] = new int[GameConstants.ABZ_MAX_INDEX];

		// if (_zhu_type == 4) {
		// return;
		// }
		for (int i = 0; i < cbCardCount; i++) {
			int index = GetCardLogicValue(cbCardData[i]);
			int color = this.GetCardColor(cbCardData[i]);
			if (color == 4) {
				color = 3;
			}
			if ((cbCardData[i] > this._zhu_value && cbCardData[i] < 0x3000 && this.GetCardValue(cbCardData[i]) == 2)
					|| this.GetCardValue(cbCardData[i]) == this._chang_zhu_one
					|| this.GetCardValue(cbCardData[i]) == this._chang_zhu_two) {
				AnalyseIndexResult.card_data[3][index - 2][AnalyseIndexResult.card_index[3][index - 2]] = cbCardData[i];
				AnalyseIndexResult.card_index[3][index - 2]++;
			} else {
				AnalyseIndexResult.card_data[color][index - 3][AnalyseIndexResult.card_index[color][index
						- 3]] = cbCardData[i];
				AnalyseIndexResult.card_index[color][index - 3]++;
			}

		}
	}

	// 分析扑克
	public void AnalysebCardDataToIndex(int cbCardData[], int cbCardCount,
			tagAnalyseIndexResult_SJ AnalyseIndexResult) {
		int card_index[] = new int[GameConstants.ABZ_MAX_INDEX];

		// if (_zhu_type == 4) {
		// return;
		// }
		for (int i = 0; i < cbCardCount; i++) {
			int index = GetCardLogicValue(cbCardData[i]);
			int color = this.GetCardColor(cbCardData[i]);
			if ((cbCardData[i] > this._zhu_value && cbCardData[i] < 0x3000 && this.GetCardValue(cbCardData[i]) == 2)
					|| this.GetCardValue(cbCardData[i]) == this._chang_zhu_one
					|| this.GetCardValue(cbCardData[i]) == this._chang_zhu_two) {
				AnalyseIndexResult.card_data[this._zhu_type][index - 2][AnalyseIndexResult.card_index[_zhu_type][index
						- 2]] = cbCardData[i];
				AnalyseIndexResult.card_index[_zhu_type][index - 2]++;
			} else {
				AnalyseIndexResult.card_data[color][index - 3][AnalyseIndexResult.card_index[color][index
						- 3]] = cbCardData[i];
				AnalyseIndexResult.card_index[color][index - 3]++;
			}

		}
	}

	// 获取类型
	public int GetCardType(int cbCardData[], int cbCardCount) {
		if (cbCardCount == 1) {
			return GameConstants.XFGD_CT_SINGLE;
		}
		tagAnalyseCardType type_card = new tagAnalyseCardType();
		Analyse_card_type(cbCardData, cbCardCount, type_card);

		for (int i = 1; i < type_card.type_count; i++) {
			if (type_card.type[i] != type_card.type[0]) {
				return GameConstants.XFGD_CT_ERROR;
			}
		}

		int color = this.GetCardColor(cbCardData[0]);
		if (cbCardData[0] > this._zhu_value && cbCardData[0] < 0x3000) {
			color = _zhu_type;
		}

		tagAnalyseIndexResult_SJ IndexResult = new tagAnalyseIndexResult_SJ();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, IndexResult);
		int max_index = this.get_card_index(cbCardData[0]);
		int min_idnex = get_card_index(cbCardData[cbCardCount - 1]);
		if (cbCardCount == 2) {
			if (IndexResult.card_index[color][max_index] == 2) {
				return GameConstants.XFGD_CT_DOUBLE;
			}

		}
		if (type_card.type_count > 1) {
			return GameConstants.XFGD_CT_SHUAI_PAI;
		} else {
			return GameConstants.XFGD_CT_DOUBLE_LINK;
		}
	}

	// 分析牌型
	public void Analyse_card_type(int cbCardData[], int cbCardCount, tagAnalyseCardType type_card) {
		tagAnalyseIndexResult_SJ IndexResult = new tagAnalyseIndexResult_SJ();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, IndexResult);
		if (cbCardCount < 4) {
			for (int i = GameConstants.XFGD_MAX_INDEX - 1; i >= 0; i--) {
				if (i == GameConstants.XFGD_MAX_INDEX - 4) {
					for (int color = 0; color < 4; color++) {
						if (IndexResult.card_index[color][i] == 2) {
							type_card.count[type_card.type_count] = IndexResult.card_index[color][i];
							type_card.type[type_card.type_count] = GameConstants.XFGD_CT_DOUBLE;
							for (int j = 0; j < IndexResult.card_index[color][i]; j++) {
								type_card.card_data[type_card.type_count][j] = IndexResult.card_data[color][i][j];
							}
							type_card.type_count++;
						} else {
							for (int j = 0; j < IndexResult.card_index[color][i]; j++) {
								type_card.count[type_card.type_count] = 1;
								type_card.type[type_card.type_count] = GameConstants.XFGD_CT_SINGLE;
								type_card.card_data[type_card.type_count][0] = IndexResult.card_data[color][i][j];
								type_card.type_count++;
							}
						}
					}
				} else {
					if (IndexResult.card_index[_zhu_type][i] == 2) {
						type_card.count[type_card.type_count] = IndexResult.card_index[_zhu_type][i];
						type_card.type[type_card.type_count] = GameConstants.XFGD_CT_DOUBLE;
						for (int j = 0; j < IndexResult.card_index[_zhu_type][i]; j++) {
							type_card.card_data[type_card.type_count][j] = IndexResult.card_data[_zhu_type][i][j];
						}
						type_card.type_count++;
					} else {
						for (int j = 0; j < IndexResult.card_index[_zhu_type][i]; j++) {
							type_card.count[type_card.type_count] = 1;
							type_card.type[type_card.type_count] = GameConstants.XFGD_CT_SINGLE;
							type_card.card_data[type_card.type_count][0] = IndexResult.card_data[_zhu_type][i][j];
							type_card.type_count++;
						}
					}
				}
			}
			for (int color = 0; color < 4; color++) {
				if (color == _zhu_type) {
					continue;
				}
				for (int i = GameConstants.XFGD_MAX_INDEX - 5; i >= 0; i--) {

					if (IndexResult.card_index[color][i] == 2) {
						type_card.count[type_card.type_count] = IndexResult.card_index[color][i];
						type_card.type[type_card.type_count] = GameConstants.XFGD_CT_DOUBLE;
						for (int j = 0; j < IndexResult.card_index[color][i]; j++) {
							type_card.card_data[type_card.type_count][j] = IndexResult.card_data[color][i][j];
						}
						type_card.type_count++;
					} else {
						for (int j = 0; j < IndexResult.card_index[color][i]; j++) {
							type_card.count[type_card.type_count] = 1;
							type_card.type[type_card.type_count] = GameConstants.XFGD_CT_SINGLE;
							type_card.card_data[type_card.type_count][0] = IndexResult.card_data[color][i][j];
							type_card.type_count++;
						}
					}

				}
			}

		} else {
			// 主牌先

			for (int i = GameConstants.XFGD_MAX_INDEX - 1; i >= 0; i--) {
				if (i == GameConstants.XFGD_MAX_INDEX - 4) {
					for (int color_index = 3; color_index >= 0; color_index--) {
						if (IndexResult.card_index[color_index][i] != 2) {
							for (int j = 0; j < IndexResult.card_index[color_index][i]; j++) {
								type_card.count[type_card.type_count] = 1;
								type_card.type[type_card.type_count] = GameConstants.XFGD_CT_SINGLE;
								type_card.card_data[type_card.type_count][0] = IndexResult.card_data[color_index][i][j];
								type_card.type_count++;
							}
						} else {
							int max_index = i;
							int min_index = i;
							for (int j = max_index - 1; j >= 0; j--) {
								if (IndexResult.card_index[_zhu_type][j] != 2 && max_index - min_index == 0) {

									type_card.count[type_card.type_count] = IndexResult.card_index[color_index][i];
									type_card.type[type_card.type_count] = GameConstants.XFGD_CT_DOUBLE;
									for (int x = 0; x < IndexResult.card_index[color_index][i]; x++) {
										type_card.card_data[type_card.type_count][x] = IndexResult.card_data[color_index][i][x];
									}
									type_card.type_count++;
									IndexResult.card_index[color_index][i] = 0;

									if (IndexResult.card_index[_zhu_type][j] == 1) {
										type_card.count[type_card.type_count] = IndexResult.card_index[_zhu_type][j];
										type_card.type[type_card.type_count] = GameConstants.XFGD_CT_SINGLE;
										for (int x = 0; x < IndexResult.card_index[_zhu_type][j]; x++) {
											type_card.card_data[type_card.type_count][x] = IndexResult.card_data[_zhu_type][j][x];
										}
										type_card.type_count++;
									}
									break;
								} else if (IndexResult.card_index[_zhu_type][j] != 2 && max_index - min_index > 0) {
									min_index = j + 1;
									type_card.type[type_card.type_count] = GameConstants.XFGD_CT_DOUBLE_LINK;
									for (int x = 0; x < IndexResult.card_index[color_index][i]; x++) {
										type_card.card_data[type_card.type_count][type_card.count[type_card.type_count]++] = IndexResult.card_data[color_index][i][x];
									}
									IndexResult.card_index[color_index][i] = 0;
									for (int x = max_index - 1; x >= min_index; x--) {
										for (int y = 0; y < IndexResult.card_index[_zhu_type][x]; y++) {
											type_card.card_data[type_card.type_count][type_card.count[type_card.type_count]++] = IndexResult.card_data[_zhu_type][x][y];
										}
										IndexResult.card_index[_zhu_type][x] = 0;
									}
									type_card.type_count++;
									break;
								} else if (IndexResult.card_index[_zhu_type][j] == 2) {
									min_index = j;
								}
							}
						}
					}
				} else {
					if (IndexResult.card_index[_zhu_type][i] != 2) {
						for (int j = 0; j < IndexResult.card_index[_zhu_type][i]; j++) {
							type_card.count[type_card.type_count] = 1;
							type_card.type[type_card.type_count] = GameConstants.XFGD_CT_SINGLE;
							type_card.card_data[type_card.type_count][0] = IndexResult.card_data[_zhu_type][i][j];
							type_card.type_count++;
						}
					} else {
						int max_index = i;
						int min_index = i;
						if (min_index == 0) {
							type_card.count[type_card.type_count] = IndexResult.card_index[_zhu_type][i];
							type_card.type[type_card.type_count] = GameConstants.XFGD_CT_DOUBLE;
							for (int x = 0; x < IndexResult.card_index[_zhu_type][i]; x++) {
								type_card.card_data[type_card.type_count][x] = IndexResult.card_data[_zhu_type][i][x];
							}
							type_card.type_count++;
						} else {
							boolean isfind = false;
							for (int j = max_index - 1; j >= 0; j--) {
								if (j == GameConstants.XFGD_MAX_INDEX - 4) {

									for (int color_index = 3; color_index >= 0; color_index--) {
										if (IndexResult.card_index[color_index][j] == 2) {
											min_index = j;
											if (IndexResult.card_index[_zhu_type][j - 1] != 2
													&& IndexResult.card_index[_zhu_type][max_index] == 2 && !isfind) {
												type_card.type[type_card.type_count] = GameConstants.XFGD_CT_DOUBLE_LINK;
												for (int x = max_index; x >= j + 1; x--) {
													for (int y = 0; y < IndexResult.card_index[_zhu_type][x]; y++) {
														type_card.card_data[type_card.type_count][type_card.count[type_card.type_count]++] = IndexResult.card_data[_zhu_type][x][y];
													}
													IndexResult.card_index[_zhu_type][x] = 0;
												}
												for (int y = 0; y < IndexResult.card_index[color_index][j]; y++) {
													type_card.card_data[type_card.type_count][type_card.count[type_card.type_count]++] = IndexResult.card_data[color_index][j][y];

												}
												IndexResult.card_index[color_index][j] = 0;
												type_card.type_count++;
											} else if (IndexResult.card_index[_zhu_type][j - 1] != 2
													&& IndexResult.card_index[_zhu_type][max_index] != 2) {
												type_card.type[type_card.type_count] = GameConstants.XFGD_CT_DOUBLE;
												for (int y = 0; y < IndexResult.card_index[color_index][j]; y++) {
													type_card.card_data[type_card.type_count][type_card.count[type_card.type_count]++] = IndexResult.card_data[color_index][j][y];

												}
												IndexResult.card_index[color_index][j] = 0;
												type_card.type_count++;
											} else {
												if (max_index > min_index) {
													for (int color_index_temp = color_index
															+ 1; color_index_temp < 4; color_index_temp++) {
														if (IndexResult.card_index[color_index_temp][j] == 2) {
															type_card.type[type_card.type_count] = GameConstants.XFGD_CT_DOUBLE_LINK;
															for (int x = max_index; x >= j + 1; x--) {
																for (int y = 0; y < IndexResult.card_index[_zhu_type][x]; y++) {
																	type_card.card_data[type_card.type_count][type_card.count[type_card.type_count]++] = IndexResult.card_data[_zhu_type][x][y];
																}
																IndexResult.card_index[_zhu_type][x] = 0;
															}
															for (int y = 0; y < IndexResult.card_index[color_index][j]; y++) {
																type_card.card_data[type_card.type_count][type_card.count[type_card.type_count]++] = IndexResult.card_data[color_index][j][y];

															}
															IndexResult.card_index[color_index][j] = 0;
															type_card.type_count++;
															max_index = j;
															break;
														}
													}
												}
											}
											isfind = true;
										} else if (IndexResult.card_index[color_index][j] == 1) {
											type_card.type[type_card.type_count] = GameConstants.XFGD_CT_SINGLE;
											for (int y = 0; y < IndexResult.card_index[color_index][j]; y++) {
												type_card.card_data[type_card.type_count][type_card.count[type_card.type_count]++] = IndexResult.card_data[color_index][j][y];

											}
											IndexResult.card_index[color_index][j] = 0;
											type_card.type_count++;
										}
									}
									if (min_index != j) {
										if (max_index > min_index) {
											type_card.type[type_card.type_count] = GameConstants.XFGD_CT_DOUBLE_LINK;
											for (int x = max_index; x >= min_index; x--) {
												for (int y = 0; y < IndexResult.card_index[_zhu_type][x]; y++) {
													type_card.card_data[type_card.type_count][type_card.count[type_card.type_count]++] = IndexResult.card_data[_zhu_type][x][y];
												}
												IndexResult.card_index[_zhu_type][x] = 0;
											}
											type_card.type_count++;

										} else {
											type_card.type[type_card.type_count] = GameConstants.XFGD_CT_DOUBLE;
											for (int y = 0; y < IndexResult.card_index[_zhu_type][i]; y++) {
												type_card.card_data[type_card.type_count][type_card.count[type_card.type_count]++] = IndexResult.card_data[_zhu_type][i][y];

											}
											IndexResult.card_index[_zhu_type][i] = 0;
											type_card.type_count++;
										}
										i = j;
										break;
									} else if (max_index - min_index > 0
											&& IndexResult.card_index[_zhu_type][j - 1] != 2 && min_index != j) {
										type_card.type[type_card.type_count] = GameConstants.XFGD_CT_DOUBLE_LINK;
										for (int x = max_index; x >= min_index; x--) {
											for (int y = 0; y < IndexResult.card_index[_zhu_type][x]; y++) {
												type_card.card_data[type_card.type_count][type_card.count[type_card.type_count]++] = IndexResult.card_data[_zhu_type][x][y];
											}
										}
										if (type_card.count[type_card.type_count] > 0) {
											type_card.type_count++;
										}

										i = j;
										break;
									} else if (IndexResult.card_index[_zhu_type][j - 1] != 2) {
										i = j;
										break;
									}

								} else {
									if (IndexResult.card_index[_zhu_type][j] != 2 && max_index - min_index == 0
											&& IndexResult.card_index[_zhu_type][max_index] == 2) {
										type_card.count[type_card.type_count] = IndexResult.card_index[_zhu_type][max_index];
										type_card.type[type_card.type_count] = GameConstants.XFGD_CT_DOUBLE;
										for (int x = 0; x < IndexResult.card_index[_zhu_type][max_index]; x++) {
											type_card.card_data[type_card.type_count][x] = IndexResult.card_data[_zhu_type][max_index][x];
										}
										type_card.type_count++;
										if (IndexResult.card_index[_zhu_type][j] == 1) {
											type_card.count[type_card.type_count] = IndexResult.card_index[_zhu_type][j];
											type_card.type[type_card.type_count] = GameConstants.XFGD_CT_SINGLE;
											for (int x = 0; x < IndexResult.card_index[_zhu_type][j]; x++) {
												type_card.card_data[type_card.type_count][x] = IndexResult.card_data[_zhu_type][j][x];
											}
											type_card.type_count++;
										}
										i = j;
										break;
									} else if (IndexResult.card_index[_zhu_type][j] != 2 && max_index - min_index > 0) {
										type_card.type[type_card.type_count] = GameConstants.XFGD_CT_DOUBLE_LINK;
										for (int x = max_index; x >= min_index; x--) {
											if (x == GameConstants.XFGD_MAX_INDEX - 4) {
												for (int color_index = 3; color_index >= 0; color_index--) {
													if (IndexResult.card_index[color_index][x] == 2) {
														for (int y = 0; y < IndexResult.card_index[color_index][x]; y++) {
															type_card.card_data[type_card.type_count][type_card.count[type_card.type_count]++] = IndexResult.card_data[color_index][x][y];
														}
														break;
													}
												}

											} else {
												for (int y = 0; y < IndexResult.card_index[_zhu_type][x]; y++) {
													type_card.card_data[type_card.type_count][type_card.count[type_card.type_count]++] = IndexResult.card_data[_zhu_type][x][y];
												}
											}

										}
										if (type_card.count[type_card.type_count] > 0) {
											type_card.type_count++;
										}

										i = min_index;
										break;
									} else if (IndexResult.card_index[_zhu_type][j] == 2) {
										min_index = j;
										if (min_index == 0 && max_index - min_index > 0) {
											if (max_index < GameConstants.XFGD_MAX_INDEX - 4
													|| min_index >= GameConstants.XFGD_MAX_INDEX - 4) {
												type_card.type[type_card.type_count] = GameConstants.XFGD_CT_DOUBLE_LINK;
												for (int x = max_index; x >= min_index; x--) {
													for (int y = 0; y < IndexResult.card_index[_zhu_type][x]; y++) {
														type_card.card_data[type_card.type_count][type_card.count[type_card.type_count]++] = IndexResult.card_data[_zhu_type][x][y];
													}
												}
												type_card.type_count++;
											} else {
												for (int x = max_index; x >= min_index; x--) {
													type_card.count[type_card.type_count] = IndexResult.card_index[_zhu_type][x];
													type_card.type[type_card.type_count] = GameConstants.XFGD_CT_DOUBLE;
													for (int y = 0; y < IndexResult.card_index[_zhu_type][i]; y++) {
														type_card.card_data[type_card.type_count][y] = IndexResult.card_data[_zhu_type][x][y];
													}
													type_card.type_count++;
												}

											}
											i = j;
										}
									}
								}

							}
						}
					}
				}

			}
			// 拖来机
			for (int color = 0; color < 4; color++) {
				if (color != this._zhu_type) {
					for (int i = GameConstants.XFGD_MAX_INDEX - 5; i >= 0; i--) {
						if (IndexResult.card_index[color][i] != 2) {
							for (int j = 0; j < IndexResult.card_index[color][i]; j++) {
								type_card.count[type_card.type_count] = 1;
								type_card.type[type_card.type_count] = GameConstants.XFGD_CT_SINGLE;
								type_card.card_data[type_card.type_count][0] = IndexResult.card_data[color][i][j];
								type_card.type_count++;
							}
						} else {
							int max_index = i;
							int min_index = i;
							if (min_index == 0) {
								type_card.count[type_card.type_count] = IndexResult.card_index[color][i];
								type_card.type[type_card.type_count] = GameConstants.XFGD_CT_DOUBLE;
								for (int x = 0; x < IndexResult.card_index[color][i]; x++) {
									type_card.card_data[type_card.type_count][x] = IndexResult.card_data[color][i][x];
								}
								type_card.type_count++;
							} else {
								for (int j = max_index - 1; j >= 0; j--) {
									if (IndexResult.card_index[color][j] != 2 && max_index - min_index == 0) {
										type_card.count[type_card.type_count] = IndexResult.card_index[color][i];
										type_card.type[type_card.type_count] = GameConstants.XFGD_CT_DOUBLE;
										for (int x = 0; x < IndexResult.card_index[color][i]; x++) {
											type_card.card_data[type_card.type_count][x] = IndexResult.card_data[color][i][x];
										}
										type_card.type_count++;
										if (IndexResult.card_index[color][j] == 1) {
											type_card.count[type_card.type_count] = IndexResult.card_index[color][j];
											type_card.type[type_card.type_count] = GameConstants.XFGD_CT_SINGLE;
											for (int x = 0; x < IndexResult.card_index[color][j]; x++) {
												type_card.card_data[type_card.type_count][x] = IndexResult.card_data[color][j][x];
											}
											type_card.type_count++;
										}
										i = j;
										break;
									} else if (IndexResult.card_index[color][j] != 2 && max_index - min_index > 0) {
										if (max_index < GameConstants.XFGD_MAX_INDEX - 4
												|| min_index >= GameConstants.XFGD_MAX_INDEX - 4) {
											type_card.type[type_card.type_count] = GameConstants.XFGD_CT_DOUBLE_LINK;
											for (int x = max_index; x >= min_index; x--) {
												for (int y = 0; y < IndexResult.card_index[color][x]; y++) {
													type_card.card_data[type_card.type_count][type_card.count[type_card.type_count]++] = IndexResult.card_data[color][x][y];
												}
											}
											type_card.type_count++;
										} else {
											for (int x = max_index; x >= min_index; x--) {
												type_card.count[type_card.type_count] = IndexResult.card_index[color][x];
												type_card.type[type_card.type_count] = GameConstants.XFGD_CT_DOUBLE;
												for (int y = 0; y < IndexResult.card_index[color][i]; y++) {
													type_card.card_data[type_card.type_count][y] = IndexResult.card_data[color][x][y];
												}
												type_card.type_count++;
											}

										}
										if (IndexResult.card_index[color][j] == 1) {
											type_card.count[type_card.type_count] = IndexResult.card_index[color][j];
											type_card.type[type_card.type_count] = GameConstants.XFGD_CT_SINGLE;
											for (int x = 0; x < IndexResult.card_index[color][j]; x++) {
												type_card.card_data[type_card.type_count][x] = IndexResult.card_data[color][j][x];
											}
											type_card.type_count++;
										}
										i = j;
										break;
									} else if (IndexResult.card_index[color][j] == 2) {
										min_index = j;
										if (min_index == 0 && max_index - min_index > 0) {
											if (max_index < GameConstants.XFGD_MAX_INDEX - 4
													|| min_index >= GameConstants.XFGD_MAX_INDEX - 4) {
												type_card.type[type_card.type_count] = GameConstants.XFGD_CT_DOUBLE_LINK;
												for (int x = max_index; x >= min_index; x--) {
													for (int y = 0; y < IndexResult.card_index[color][x]; y++) {
														type_card.card_data[type_card.type_count][type_card.count[type_card.type_count]++] = IndexResult.card_data[color][x][y];
													}
												}
												type_card.type_count++;
											} else {
												for (int x = max_index; x >= min_index; x--) {
													type_card.count[type_card.type_count] = IndexResult.card_index[color][x];
													type_card.type[type_card.type_count] = GameConstants.XFGD_CT_DOUBLE;
													for (int y = 0; y < IndexResult.card_index[color][i]; y++) {
														type_card.card_data[type_card.type_count][y] = IndexResult.card_data[color][x][y];
													}
													type_card.type_count++;
												}

											}
											i = j;
										}
									}
								}
							}
						}
					}
				}

			}

		}
	}

	// 分析牌型
	public void Analyse_card_type_No_Zhu(int cbCardData[], int cbCardCount, tagAnalyseCardType type_card) {
		tagAnalyseIndexResult_SJ IndexResult = new tagAnalyseIndexResult_SJ();
		AnalysebCardDataToIndex_No_Zhu(cbCardData, cbCardCount, IndexResult);
		// 拖来机
		for (int color = 0; color < 4; color++) {
			for (int i = GameConstants.XFGD_MAX_INDEX - 5; i >= 0; i--) {
				if (IndexResult.card_index[color][i] != 2) {
					for (int j = 0; j < IndexResult.card_index[color][i]; j++) {
						type_card.count[type_card.type_count] = 1;
						type_card.type[type_card.type_count] = GameConstants.XFGD_CT_SINGLE;
						type_card.card_data[type_card.type_count][0] = IndexResult.card_data[color][i][j];
						type_card.type_count++;
					}
				} else {
					int max_index = i;
					int min_index = i;
					if (min_index == 0) {
						type_card.count[type_card.type_count] = IndexResult.card_index[color][i];
						type_card.type[type_card.type_count] = GameConstants.XFGD_CT_DOUBLE;
						for (int x = 0; x < IndexResult.card_index[color][i]; x++) {
							type_card.card_data[type_card.type_count][x] = IndexResult.card_data[color][i][x];
						}
						type_card.type_count++;
					} else {
						for (int j = max_index - 1; j >= 0; j--) {
							if (IndexResult.card_index[color][j] != 2 && max_index - min_index == 0) {
								type_card.count[type_card.type_count] = IndexResult.card_index[color][i];
								type_card.type[type_card.type_count] = GameConstants.XFGD_CT_DOUBLE;
								for (int x = 0; x < IndexResult.card_index[color][i]; x++) {
									type_card.card_data[type_card.type_count][x] = IndexResult.card_data[color][i][x];
								}
								type_card.type_count++;
								if (IndexResult.card_index[color][j] == 1) {
									type_card.count[type_card.type_count] = IndexResult.card_index[color][j];
									type_card.type[type_card.type_count] = GameConstants.XFGD_CT_SINGLE;
									for (int x = 0; x < IndexResult.card_index[color][j]; x++) {
										type_card.card_data[type_card.type_count][x] = IndexResult.card_data[color][j][x];
									}
									type_card.type_count++;
								}
								i = j;
								break;
							} else if (IndexResult.card_index[color][j] != 2 && max_index - min_index > 0) {
								if (max_index < GameConstants.XFGD_MAX_INDEX - 4
										|| min_index >= GameConstants.XFGD_MAX_INDEX - 4) {
									type_card.type[type_card.type_count] = GameConstants.XFGD_CT_DOUBLE_LINK;
									for (int x = max_index; x >= min_index; x--) {
										for (int y = 0; y < IndexResult.card_index[color][x]; y++) {
											type_card.card_data[type_card.type_count][type_card.count[type_card.type_count]++] = IndexResult.card_data[color][x][y];
										}
									}
									type_card.type_count++;
								} else {
									for (int x = max_index; x >= min_index; x--) {
										type_card.count[type_card.type_count] = IndexResult.card_index[color][x];
										type_card.type[type_card.type_count] = GameConstants.XFGD_CT_DOUBLE;
										for (int y = 0; y < IndexResult.card_index[color][i]; y++) {
											type_card.card_data[type_card.type_count][y] = IndexResult.card_data[color][x][y];
										}
										type_card.type_count++;
									}

								}
								if (IndexResult.card_index[color][j] == 1) {
									type_card.count[type_card.type_count] = IndexResult.card_index[color][j];
									type_card.type[type_card.type_count] = GameConstants.XFGD_CT_SINGLE;
									for (int x = 0; x < IndexResult.card_index[color][j]; x++) {
										type_card.card_data[type_card.type_count][x] = IndexResult.card_data[color][j][x];
									}
									type_card.type_count++;
								}
								i = j;
								break;
							} else if (IndexResult.card_index[color][j] == 2) {
								min_index = j;
								if (min_index == 0 && max_index - min_index > 0) {
									if (max_index < GameConstants.XFGD_MAX_INDEX - 4
											|| min_index >= GameConstants.XFGD_MAX_INDEX - 4) {
										type_card.type[type_card.type_count] = GameConstants.XFGD_CT_DOUBLE_LINK;
										for (int x = max_index; x >= min_index; x--) {
											for (int y = 0; y < IndexResult.card_index[color][x]; y++) {
												type_card.card_data[type_card.type_count][type_card.count[type_card.type_count]++] = IndexResult.card_data[color][x][y];
											}
										}
										type_card.type_count++;
									} else {
										for (int x = max_index; x >= min_index; x--) {
											type_card.count[type_card.type_count] = IndexResult.card_index[color][x];
											type_card.type[type_card.type_count] = GameConstants.XFGD_CT_DOUBLE;
											for (int y = 0; y < IndexResult.card_index[color][i]; y++) {
												type_card.card_data[type_card.type_count][y] = IndexResult.card_data[color][x][y];
											}
											type_card.type_count++;
										}

									}
									i = j;
								}
							}
						}
					}
				}
			}

		}
	}

	// 分析牌型
	public void get_card_all_type(int cbCardData[], int cbCardCount, tagAnalyseCardType type_card, int type) {
		tagAnalyseIndexResult_SJ IndexResult = new tagAnalyseIndexResult_SJ();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, IndexResult);
		if (type == GameConstants.XFGD_CT_SINGLE) {
			for (int color = 0; color < 4; color++) {
				for (int i = GameConstants.XFGD_MAX_INDEX - 1; i >= 0; i--) {
					if (IndexResult.card_index[color][i] > 0) {
						for (int j = 0; j < IndexResult.card_index[color][i]; j++) {
							type_card.count[type_card.type_count] = 1;
							type_card.type[type_card.type_count] = GameConstants.XFGD_CT_SINGLE;
							type_card.card_data[type_card.type_count][j] = IndexResult.card_data[color][i][j];
							type_card.type_count++;
						}
					}
				}
			}

		} else {
			for (int color = 0; color < 4; color++) {
				for (int i = GameConstants.XFGD_MAX_INDEX - 1; i >= 0; i--) {
					if (IndexResult.card_index[color][i] == 1) {
						for (int j = 0; j < IndexResult.card_index[color][i]; j++) {
							type_card.count[type_card.type_count] = 1;
							type_card.type[type_card.type_count] = GameConstants.XFGD_CT_SINGLE;
							type_card.card_data[type_card.type_count][j] = IndexResult.card_data[color][i][j];
							type_card.type_count++;
						}
					}
					if (IndexResult.card_index[color][i] == 2) {
						type_card.count[type_card.type_count] = 2;
						type_card.type[type_card.type_count] = GameConstants.XFGD_CT_DOUBLE;
						for (int j = 0; j < IndexResult.card_index[color][i]; j++) {
							type_card.card_data[type_card.type_count][j] = IndexResult.card_data[color][i][j];
						}
						type_card.type_count++;
					}
				}
			}

		}
	}

	// 获取牌里的所有类型
	public void get_card_all_type(int cbCardData[], int cbCardCount, tagAnalyseCardType type_card) {
		tagAnalyseIndexResult_SJ IndexResult = new tagAnalyseIndexResult_SJ();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, IndexResult);
		if (cbCardCount <= 4) {
			for (int i = GameConstants.XFGD_MAX_INDEX - 1; i >= 0; i--) {
				for (int color = 0; color < 4; color++) {
					if (IndexResult.card_index[color][i] == 2) {
						type_card.count[type_card.type_count] = IndexResult.card_index[color][i];
						type_card.type[type_card.type_count] = GameConstants.XFGD_CT_DOUBLE;
						for (int j = 0; j < IndexResult.card_index[color][i]; j++) {
							type_card.card_data[type_card.type_count][j] = IndexResult.card_data[color][i][j];
						}
						type_card.type_count++;

						for (int j = 0; j < IndexResult.card_index[color][i]; j++) {
							type_card.count[type_card.type_count] = 1;
							type_card.type[type_card.type_count] = GameConstants.XFGD_CT_SINGLE;
							type_card.card_data[type_card.type_count][0] = IndexResult.card_data[color][i][j];
							type_card.type_count++;
						}
					} else {
						for (int j = 0; j < IndexResult.card_index[color][i]; j++) {
							type_card.count[type_card.type_count] = 1;
							type_card.type[type_card.type_count] = GameConstants.XFGD_CT_SINGLE;
							type_card.card_data[type_card.type_count][0] = IndexResult.card_data[color][i][j];
							type_card.type_count++;
						}
					}

				}
			}

		} else {
			// 拖来机
			for (int color = 0; color < 4; color++) {
				for (int i = GameConstants.XFGD_MAX_INDEX - 1; i >= 0; i--) {
					if (color == 4) {
						color = this._zhu_type;
					}
					if (IndexResult.card_index[color][i] != 2) {
						for (int j = 0; j < IndexResult.card_index[color][i]; j++) {
							type_card.count[type_card.type_count] = 1;
							type_card.type[type_card.type_count] = GameConstants.XFGD_CT_SINGLE;
							type_card.card_data[type_card.type_count][0] = IndexResult.card_data[color][i][j];
							type_card.type_count++;
						}
					} else {
						int max_index = i;
						int min_index = i;
						for (int j = max_index - 1; j >= 0; j--) {
							if (IndexResult.card_index[color][j] != 2 && max_index - min_index == 0) {
								// 对子
								type_card.count[type_card.type_count] = IndexResult.card_index[color][i];
								type_card.type[type_card.type_count] = GameConstants.XFGD_CT_DOUBLE;
								for (int x = 0; x < IndexResult.card_index[color][i]; x++) {
									type_card.card_data[type_card.type_count][x] = IndexResult.card_data[color][i][x];
								}
								type_card.type_count++;
								// 单张
								for (int x = 0; x < IndexResult.card_index[color][i]; x++) {
									type_card.count[type_card.type_count] = 1;
									type_card.type[type_card.type_count] = GameConstants.XFGD_CT_SINGLE;
									type_card.card_data[type_card.type_count][0] = IndexResult.card_data[color][i][x];
									type_card.type_count++;
								}
								i = j;
								break;
							} else if (IndexResult.card_index[color][j] != 2 && max_index - min_index > 0) {
								if (max_index < GameConstants.XFGD_MAX_INDEX - 4
										|| min_index >= GameConstants.XFGD_MAX_INDEX - 4) {
									type_card.type[type_card.type_count] = GameConstants.XFGD_CT_DOUBLE_LINK;
									for (int x = max_index; x >= min_index; x--) {
										for (int y = 0; y < IndexResult.card_index[color][x]; y++) {
											type_card.card_data[type_card.type_count][type_card.count[type_card.type_count]++] = IndexResult.card_data[color][x][y];
										}
									}
									type_card.type_count++;
								}

								for (int x = max_index; x >= min_index; x--) {
									// 对子
									type_card.count[type_card.type_count] = IndexResult.card_index[color][i];
									type_card.type[type_card.type_count] = GameConstants.XFGD_CT_DOUBLE;
									for (int y = 0; y < IndexResult.card_index[color][x]; y++) {
										type_card.card_data[type_card.type_count][y] = IndexResult.card_data[color][x][y];
									}
									type_card.type_count++;
									// 单张
									for (int y = 0; y < IndexResult.card_index[color][x]; y++) {
										type_card.count[type_card.type_count] = 1;
										type_card.type[type_card.type_count] = GameConstants.XFGD_CT_SINGLE;
										type_card.card_data[type_card.type_count][0] = IndexResult.card_data[color][x][y];
										type_card.type_count++;
									}
								}
								i = j;
								break;
							} else if (IndexResult.card_index[color][j] == 2) {
								min_index = j;
							}
						}

					}
				}
			}

		}

	}

	public boolean is_he_li(int first_card[], int first_count, int out_card[], int out_count, int hand_card_data[],
			int hand_card_count) {
		if (first_count != out_count) {
			return false;
		}
		int frist_type = this.GetCardType(first_card, first_count);
		int can_out_data[] = new int[hand_card_count];
		int must_out_data[] = new int[hand_card_count];
		int must_out_count = 0;
		int out_card_temp[] = new int[out_count];
		for (int j = 0; j < out_count; j++) {
			out_card_temp[j] = out_card[j];
		}
		int color = this.GetCardColor(first_card[0]);
		if (first_card[0] > this._zhu_value) {
			color = this._zhu_type;
		}
		int can_out_count = Player_Can_out_card(hand_card_data, hand_card_count, first_card, first_count, can_out_data);
		if (can_out_count != hand_card_count
				|| GetCardColor_Count(hand_card_data, hand_card_count, color) == hand_card_count) {
			tagAnalyseCardType first_type_card = new tagAnalyseCardType();
			Analyse_card_type(first_card, first_count, first_type_card);
			if (first_type_card.type[0] == GameConstants.XFGD_CT_SINGLE) {
				for (int j = 0; j < out_count; j++) {
					for (int x = 0; x < can_out_count; x++) {
						if (out_card_temp[j] == can_out_data[x]) {
							can_out_data[x] = 0;
							break;
						}
						if (x == can_out_count - 1) {
							return false;
						}
					}
				}
			} else {
				tagAnalyseCardType out_type_card = new tagAnalyseCardType();
				tagAnalyseCardType can_out_type_card = new tagAnalyseCardType();
				get_card_all_type(out_card_temp, out_count, out_type_card, frist_type);
				get_card_all_type(can_out_data, can_out_count, can_out_type_card, frist_type);

				for (int i = 0; i < out_type_card.type_count; i++) {
					for (int j = 0; j < can_out_type_card.type_count; j++) {
						if (this.remove_cards_by_data(out_type_card.card_data[i], out_type_card.count[i],
								can_out_type_card.card_data[j], can_out_type_card.count[j])) {
							break;
						}
						if (j == can_out_type_card.type_count - 1) {
							return false;
						}
					}
				}
			}

		} else {

			if (color == _zhu_type) {
				for (int i = 0; i < hand_card_count; i++) {
					if (hand_card_data[i] > this._zhu_value) {
						must_out_data[must_out_count++] = hand_card_data[i];
					}
				}
			} else {
				for (int i = 0; i < hand_card_count; i++) {
					if (this.GetCardColor(hand_card_data[i]) == GetCardColor(first_card[0])
							&& hand_card_data[i] < this._zhu_value) {
						must_out_data[must_out_count++] = hand_card_data[i];
					}
				}

			}
			for (int j = 0; j < must_out_count; j++) {
				for (int x = 0; x < out_count; x++) {
					if (must_out_data[j] == out_card_temp[x]) {
						out_card_temp[x] = 0;
						break;
					}
					if (x == out_count - 1) {
						return false;
					}
				}
			}

		}

		return true;
	}

	public boolean comparecarddata(int first_card[], int first_count, int next_card[], int next_count) {
		tagAnalyseIndexResult_SJ IndexResult_first = new tagAnalyseIndexResult_SJ();
		tagAnalyseIndexResult_SJ IndexResult_next = new tagAnalyseIndexResult_SJ();
		AnalysebCardDataToIndex(first_card, first_count, IndexResult_first);
		AnalysebCardDataToIndex(next_card, next_count, IndexResult_next);

		int first_type = this.GetCardType(first_card, first_count);
		int next_type = this.GetCardType(next_card, next_count);
		int first_color = this.GetCardColor(first_card[first_count - 1]);
		int next_color = this.GetCardColor(next_card[next_count - 1]);

		if (first_type == GameConstants.XFGD_CT_SHUAI_PAI) {
			return false;
		}
		if (first_card[first_count - 1] > _zhu_value && next_card[next_count - 1] < _zhu_value
				&& first_type == next_type) {
			return false;
		} else if (first_card[first_count - 1] < _zhu_value && next_card[next_count - 1] > _zhu_value
				&& first_type == next_type) {
			return true;
		}
		if (this.GetCardValue(first_card[0]) == this._chang_zhu_one
				|| this.GetCardValue(first_card[0]) == this._chang_zhu_two
				|| this.GetCardValue(first_card[0]) == this._chang_zhu_three
				|| this.GetCardValue(first_card[0]) == this._chang_zhu_four) {
			first_color = this._zhu_type;
		}
		if (this.GetCardValue(next_card[0]) == this._chang_zhu_one
				|| this.GetCardValue(next_card[0]) == this._chang_zhu_two
				|| this.GetCardValue(next_card[0]) == this._chang_zhu_three
				|| this.GetCardValue(next_card[0]) == this._chang_zhu_four) {
			next_color = this._zhu_type;
		}
		if (first_color == next_color) {
			if (next_type == first_type) {
				if (this.get_card_index(first_card[first_count - 1]) >= get_card_index(next_card[next_count - 1])) {
					return false;
				} else {
					return true;
				}
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	// public int Player_Can_out_card(int hand_card_data[], int cbHandCardCount,
	// int cbOutCardData[], int out_card_count,
	// int card_data[]) {
	// int car_out_count = 0;
	// if (out_card_count == 0) {
	// for (int i = 0; i < cbHandCardCount; i++) {
	// card_data[car_out_count++] = hand_card_data[i];
	// }
	// return car_out_count;
	// }
	// tagAnalyseCardType type_card = new tagAnalyseCardType();
	// Analyse_card_type(cbOutCardData, out_card_count, type_card);
	// int color = this.GetCardColor(cbOutCardData[0]);
	// if (cbOutCardData[0] > this._zhu_value) {
	// color = this._zhu_type;
	// }
	// if (GetCardColor_Count(hand_card_data, cbHandCardCount, color) >=
	// out_card_count) {
	//
	// int out_type = this.GetCardType(cbOutCardData, out_card_count);
	// if (out_type == GameConstants.XFGD_CT_SHUAI_PAI) {
	// int hand_card_data_temp[] = new int[cbHandCardCount];
	// for (int i = 0; i < cbHandCardCount; i++) {
	// hand_card_data_temp[i] = hand_card_data[i];
	// }
	// tagAnalyseIndexResult IndexResult = new tagAnalyseIndexResult();
	// AnalysebCardDataToIndex(hand_card_data, cbHandCardCount, IndexResult);
	// for (int type_index = 0; type_index < type_card.type_count; type_index++)
	// {
	// for (int i = 0; i < cbHandCardCount; i++) {
	// if (type_card.type[type_index] == GameConstants.XFGD_CT_SINGLE) {
	// if (color == this._zhu_type) {
	// if (this.GetCardValue(hand_card_data_temp[i]) == 2) {
	// card_data[car_out_count++] = hand_card_data_temp[i];
	// hand_card_data_temp[i] = 0;
	// } else if (this.GetCardColor(hand_card_data_temp[i]) == this._zhu_type) {
	// card_data[car_out_count++] = hand_card_data[i];
	// hand_card_data_temp[i] = 0;
	// }
	// } else {
	// if (this.GetCardColor(hand_card_data_temp[i]) ==
	// GetCardColor(cbOutCardData[0])
	// && hand_card_data[i] < this._zhu_value) {
	// card_data[car_out_count++] = hand_card_data_temp[i];
	// hand_card_data_temp[i] = 0;
	// }
	// }
	// } else if (type_card.type[type_index] == GameConstants.XFGD_CT_DOUBLE) {
	//
	// for (int index = 0; index < GameConstants.XFGD_MAX_INDEX - 4; index++) {
	// if (IndexResult.card_index[color][index] >= 2) {
	// for (int j = 0; j < IndexResult.card_index[color][index]; j++) {
	// card_data[car_out_count++] = IndexResult.card_data[color][index][j];
	// }
	// IndexResult.card_index[color][index] = 0;
	// }
	// }
	// if (color == this._zhu_type) {
	// for (int index = GameConstants.XFGD_MAX_INDEX
	// - 4; index < GameConstants.XFGD_MAX_INDEX; index++) {
	// if (index == GameConstants.XFGD_MAX_INDEX - 4) {
	// for (int color_index = 0; color_index < 4; color_index++) {
	// if (IndexResult.card_index[color_index][index] >= 2) {
	// for (int j = 0; j < IndexResult.card_index[color_index][index]; j++) {
	// card_data[car_out_count++] =
	// IndexResult.card_data[color_index][index][j];
	// }
	// }
	// IndexResult.card_index[color_index][index] = 0;
	// }
	// } else {
	// if (IndexResult.card_index[color][index] >= 2) {
	// for (int j = 0; j < IndexResult.card_index[color][index]; j++) {
	// card_data[car_out_count++] = IndexResult.card_data[color][index][j];
	// }
	// IndexResult.card_index[color][index] = 0;
	// }
	// }
	//
	// }
	// }
	// } else {
	// int link_num = out_card_count / 2;
	// if (color == this._zhu_type) {
	// for (int index = 0; index < GameConstants.XFGD_MAX_INDEX; index++) {
	// if (IndexResult.card_index[color][index] >= 2) {
	// for (int j = index + 1; j < GameConstants.XFGD_MAX_INDEX; j++) {
	// if (j == GameConstants.XFGD_MAX_INDEX - 4) {
	// boolean is_find = false;
	// for (int color_index = 0; color_index < 4; color_index++) {
	// if (IndexResult.card_index[color_index][j] < 2) {
	// if (j - index >= link_num) {
	// for (int x = index; x < j; x++) {
	// for (int y = 0; y < IndexResult.card_index[color][x]; y++) {
	// card_data[car_out_count++] = IndexResult.card_data[color][x][y];
	// }
	// IndexResult.card_index[color][x] = 0;
	// }
	// }
	// } else {
	// is_find = true;
	// }
	// }
	// if (is_find) {
	// index = j;
	// break;
	// }
	// } else {
	// if (IndexResult.card_index[color][j] < 2) {
	// if (j - index >= link_num) {
	// for (int x = index; x < j; x++) {
	// for (int y = 0; y < IndexResult.card_index[color][x]; y++) {
	// card_data[car_out_count++] = IndexResult.card_data[color][x][y];
	// }
	// IndexResult.card_index[color][x] = 0;
	// }
	// }
	// index = j;
	// break;
	// }
	// }
	//
	// }
	//
	// }
	// }
	// if (car_out_count == 0) {
	// for (int j = 0; j < GameConstants.XFGD_MAX_INDEX; j++) {
	// if (j == GameConstants.XFGD_MAX_INDEX - 4) {
	// for (int color_index = 0; color_index < 4; color_index++) {
	// if (IndexResult.card_index[color_index][j] >= 2) {
	// for (int y = 0; y < IndexResult.card_index[color_index][j]; y++) {
	// card_data[car_out_count++] = IndexResult.card_data[color_index][j][y];
	// }
	// }
	// }
	// } else {
	// if (IndexResult.card_index[color][j] >= 2) {
	// for (int y = 0; y < IndexResult.card_index[color][j]; y++) {
	// card_data[car_out_count++] = IndexResult.card_data[color][j][y];
	// }
	// }
	// }
	//
	// }
	// }
	// } else {
	// for (int index = 0; index < GameConstants.XFGD_MAX_INDEX - 4; index++) {
	// if (IndexResult.card_index[color][index] >= 2) {
	// for (int j = index + 1; j < GameConstants.XFGD_MAX_INDEX - 4; j++) {
	// if (IndexResult.card_index[color][j] < 2) {
	// if (j - index >= link_num) {
	// for (int x = index; x < j; x++) {
	// for (int y = 0; y < IndexResult.card_index[color][x]; y++) {
	// card_data[car_out_count++] = IndexResult.card_data[color][x][y];
	// }
	// IndexResult.card_index[color][x] = 0;
	// }
	// }
	// index = j;
	// break;
	// }
	// }
	// }
	// }
	// if (car_out_count == 0) {
	// for (int j = 0; j < GameConstants.XFGD_MAX_INDEX - 4; j++) {
	// if (IndexResult.card_index[color][j] >= 2) {
	// for (int y = 0; y < IndexResult.card_index[color][j]; y++) {
	// card_data[car_out_count++] = IndexResult.card_data[color][j][y];
	// }
	// }
	// }
	// }
	// }
	//
	// }
	//
	// }
	// }
	//
	// if (car_out_count < out_card_count) {
	// car_out_count = 0;
	// for (int i = 0; i < cbHandCardCount; i++) {
	// if (this.GetCardColor(hand_card_data[i]) == color
	// && this.GetCardValue(hand_card_data[i]) != 2) {
	// card_data[car_out_count++] = hand_card_data[i];
	// } else if (color == this._zhu_type) {
	// if (this.GetCardValue(hand_card_data[i]) == 2) {
	// card_data[car_out_count++] = hand_card_data[i];
	// }
	// }
	//
	// }
	// }
	// return car_out_count;
	// } else {
	// if (out_type == GameConstants.XFGD_CT_SINGLE) {
	// for (int i = 0; i < cbHandCardCount; i++) {
	// if (color == this._zhu_type) {
	// if (this.GetCardValue(hand_card_data[i]) == 2) {
	// card_data[car_out_count++] = hand_card_data[i];
	// } else if (this.GetCardColor(hand_card_data[i]) == this._zhu_type) {
	// card_data[car_out_count++] = hand_card_data[i];
	// }
	// } else if (this.GetCardColor(hand_card_data[i]) ==
	// GetCardColor(cbOutCardData[0])
	// && hand_card_data[i] < this._zhu_value) {
	// card_data[car_out_count++] = hand_card_data[i];
	// }
	// }
	// } else if (out_type == GameConstants.XFGD_CT_DOUBLE) {
	// tagAnalyseIndexResult IndexResult = new tagAnalyseIndexResult();
	// AnalysebCardDataToIndex(hand_card_data, cbHandCardCount, IndexResult);
	// for (int i = 0; i < GameConstants.XFGD_MAX_INDEX - 4; i++) {
	// if (IndexResult.card_index[color][i] >= 2) {
	// for (int j = 0; j < IndexResult.card_index[color][i]; j++) {
	// card_data[car_out_count++] = IndexResult.card_data[color][i][j];
	// }
	// }
	// }
	// if (color == this._zhu_type) {
	// for (int i = GameConstants.XFGD_MAX_INDEX - 4; i <
	// GameConstants.XFGD_MAX_INDEX; i++) {
	// if (i == GameConstants.XFGD_MAX_INDEX - 4) {
	// for (int color_index = 0; color_index < 4; color_index++) {
	// if (IndexResult.card_index[color_index][i] >= 2) {
	// for (int j = 0; j < IndexResult.card_index[color_index][i]; j++) {
	// card_data[car_out_count++] = IndexResult.card_data[color_index][i][j];
	// }
	// }
	// }
	// } else {
	// if (IndexResult.card_index[color][i] >= 2) {
	// for (int j = 0; j < IndexResult.card_index[color][i]; j++) {
	// card_data[car_out_count++] = IndexResult.card_data[color][i][j];
	// }
	// }
	// }
	//
	// }
	// }
	// } else {
	// tagAnalyseIndexResult IndexResult = new tagAnalyseIndexResult();
	// AnalysebCardDataToIndex(hand_card_data, cbHandCardCount, IndexResult);
	// int link_num = out_card_count / 2;
	// if (color == this._zhu_type) {
	// for (int i = 0; i < GameConstants.XFGD_MAX_INDEX; i++) {
	// if (IndexResult.card_index[color][i] >= 2) {
	// for (int j = i + 1; j < GameConstants.XFGD_MAX_INDEX; j++) {
	// if (j == GameConstants.XFGD_MAX_INDEX - 4) {
	// boolean is_find = false;
	// for (int color_index = 0; color_index < 4; color_index++) {
	// if (IndexResult.card_index[color_index][j] >= 2) {
	// if (j + 1 - i >= link_num - 1) {
	// for (int y = 0; y < IndexResult.card_index[color_index][j]; y++) {
	// card_data[car_out_count++] = IndexResult.card_data[color_index][j][y];
	// }
	// is_find = true;
	// }
	//
	// }
	// }
	// if (is_find) {
	//
	// for (int x = i; x < j; x++) {
	// for (int y = 0; y < IndexResult.card_index[color][x]; y++) {
	// card_data[car_out_count++] = IndexResult.card_data[color][x][y];
	// }
	// }
	// } else {
	// i = j;
	// break;
	// }
	// } else {
	// if (IndexResult.card_index[color][j] < 2) {
	// if (j - i >= link_num) {
	// for (int x = i; x < j; x++) {
	// for (int y = 0; y < IndexResult.card_index[color][x]; y++) {
	// card_data[car_out_count++] = IndexResult.card_data[color][x][y];
	// }
	// }
	// }
	// i = j;
	// break;
	// } else if (j == GameConstants.XFGD_MAX_INDEX - 1) {
	// if (j - i + 1 >= link_num) {
	// for (int x = i; x <= j; x++) {
	// for (int y = 0; y < IndexResult.card_index[color][x]; y++) {
	// card_data[car_out_count++] = IndexResult.card_data[color][x][y];
	// }
	// }
	// }
	// }
	// }
	//
	// }
	//
	// }
	// }
	// if (car_out_count == 0) {
	// for (int i = 0; i < GameConstants.XFGD_MAX_INDEX; i++) {
	// if (i == GameConstants.XFGD_MAX_INDEX - 4) {
	// for (int color_index = 0; color_index < 4; color_index++) {
	// if (IndexResult.card_index[color_index][i] >= 2) {
	// for (int y = 0; y < IndexResult.card_index[color_index][i]; y++) {
	// card_data[car_out_count++] = IndexResult.card_data[color_index][i][y];
	// }
	// IndexResult.card_index[color_index][i] = 0;
	// }
	// }
	// } else {
	// if (IndexResult.card_index[color][i] >= 2) {
	// for (int y = 0; y < IndexResult.card_index[color][i]; y++) {
	// card_data[car_out_count++] = IndexResult.card_data[color][i][y];
	// }
	// IndexResult.card_index[color][i] = 0;
	// }
	// }
	//
	// }
	// if (car_out_count < out_card_count) {
	// for (int i = 0; i < GameConstants.XFGD_MAX_INDEX; i++) {
	// if (i == GameConstants.XFGD_MAX_INDEX - 4) {
	// for (int color_index = 0; color_index < 4; color_index++) {
	// for (int y = 0; y < IndexResult.card_index[color_index][i]; y++) {
	// card_data[car_out_count++] = IndexResult.card_data[color_index][i][y];
	// }
	// }
	// } else {
	// for (int y = 0; y < IndexResult.card_index[color][i]; y++) {
	// card_data[car_out_count++] = IndexResult.card_data[color][i][y];
	// }
	// }
	//
	// }
	// }
	// }
	// } else {
	// for (int i = 0; i < GameConstants.XFGD_MAX_INDEX - 4; i++) {
	// if (IndexResult.card_index[color][i] >= 2) {
	// for (int j = i + 1; j < GameConstants.XFGD_MAX_INDEX - 4; j++) {
	// if (IndexResult.card_index[color][j] < 2) {
	// if (j - i >= link_num) {
	// for (int x = i; x < j; x++) {
	// for (int y = 0; y < IndexResult.card_index[color][x]; y++) {
	// card_data[car_out_count++] = IndexResult.card_data[color][x][y];
	// }
	// }
	// }
	// i = j;
	// break;
	// }
	// }
	// }
	// }
	// if (car_out_count == 0) {
	// for (int i = 0; i < GameConstants.XFGD_MAX_INDEX - 4; i++) {
	// if (IndexResult.card_index[color][i] >= 2) {
	// for (int y = 0; y < IndexResult.card_index[color][i]; y++) {
	// card_data[car_out_count++] = IndexResult.card_data[color][i][y];
	// }
	// }
	// }
	// }
	// if (car_out_count < out_card_count) {
	// for (int i = 0; i < GameConstants.XFGD_MAX_INDEX - 4; i++) {
	// for (int y = 0; y < IndexResult.card_index[color][i]; y++) {
	// card_data[car_out_count++] = IndexResult.card_data[color][i][y];
	// }
	// }
	// }
	// }
	//
	// }
	// if (car_out_count == 0) {
	// for (int i = 0; i < cbHandCardCount; i++) {
	// if (this.GetCardColor(hand_card_data[i]) == color
	// && this.GetCardValue(hand_card_data[i]) != 2) {
	// card_data[car_out_count++] = hand_card_data[i];
	// } else if (color == this._zhu_type) {
	// if (this.GetCardValue(hand_card_data[i]) == 2) {
	// card_data[car_out_count++] = hand_card_data[i];
	// }
	// }
	//
	// }
	// }
	// return car_out_count;
	// }
	// } else {
	// for (int i = 0; i < cbHandCardCount; i++) {
	// card_data[car_out_count++] = hand_card_data[i];
	// }
	// return car_out_count;
	// }
	// }
	public int Player_Can_out_card(int hand_card_data[], int cbHandCardCount, int cbOutCardData[], int out_card_count,
			int card_data[]) {
		int card_out_count = 0;
		if (out_card_count == 0) {
			for (int i = 0; i < cbHandCardCount; i++) {
				card_data[card_out_count++] = hand_card_data[i];
			}
			return card_out_count;
		}
		tagAnalyseCardType type_card = new tagAnalyseCardType();
		Analyse_card_type(cbOutCardData, out_card_count, type_card);
		int color = this.GetCardColor(cbOutCardData[0]);
		if (cbOutCardData[0] > this._zhu_value) {
			color = this._zhu_type;
		}
		if (GetCardColor_Count(hand_card_data, cbHandCardCount, color) >= out_card_count) {

			int out_type = this.GetCardType(cbOutCardData, out_card_count);
			if (out_type == GameConstants.XFGD_CT_SHUAI_PAI) {
				int hand_card_data_temp[] = new int[cbHandCardCount];
				for (int i = 0; i < cbHandCardCount; i++) {
					hand_card_data_temp[i] = hand_card_data[i];
				}
				tagAnalyseIndexResult_SJ IndexResult = new tagAnalyseIndexResult_SJ();
				AnalysebCardDataToIndex(hand_card_data, cbHandCardCount, IndexResult);
				for (int type_index = 0; type_index < type_card.type_count; type_index++) {
					if (type_card.type[type_index] == GameConstants.XFGD_CT_SINGLE) {
						for (int i = 0; i < cbHandCardCount; i++) {
							if (color == this._zhu_type) {
								if (hand_card_data_temp[i] > this._zhu_value) {
									card_data[card_out_count++] = hand_card_data_temp[i];
									hand_card_data_temp[i] = 0;
								}
							} else {
								if (this.GetCardColor(hand_card_data_temp[i]) == GetCardColor(cbOutCardData[0])
										&& hand_card_data_temp[i] < this._zhu_value) {
									card_data[card_out_count++] = hand_card_data_temp[i];
									hand_card_data_temp[i] = 0;
								}
							}
						}

					} else if (type_card.type[type_index] == GameConstants.XFGD_CT_DOUBLE) {
						for (int index = 0; index < GameConstants.XFGD_MAX_INDEX - 4; index++) {
							if (IndexResult.card_index[color][index] >= 2) {
								for (int j = 0; j < IndexResult.card_index[color][index]; j++) {
									card_data[card_out_count++] = IndexResult.card_data[color][index][j];
								}
								IndexResult.card_index[color][index] = 0;
							}
						}
						if (color == this._zhu_type) {
							for (int index = GameConstants.XFGD_MAX_INDEX
									- 4; index < GameConstants.XFGD_MAX_INDEX; index++) {
								if (index == GameConstants.XFGD_MAX_INDEX - 4) {
									for (int color_index = 3; color_index >= 0; color_index--) {
										if (IndexResult.card_index[color_index][index] >= 2) {
											for (int j = 0; j < IndexResult.card_index[color_index][index]; j++) {
												card_data[card_out_count++] = IndexResult.card_data[color_index][index][j];
											}
											IndexResult.card_index[color_index][index] = 0;
										}

									}
								} else {
									if (IndexResult.card_index[color][index] >= 2) {
										for (int j = 0; j < IndexResult.card_index[color][index]; j++) {
											card_data[card_out_count++] = IndexResult.card_data[color][index][j];
										}
										IndexResult.card_index[color][index] = 0;
									}
								}

							}
						}

						if (type_index == type_card.type_count - 1) {
							if (card_out_count < out_card_count) {
								if (color == this._zhu_type) {
									for (int index = GameConstants.XFGD_MAX_INDEX - 1; index >= 0; index--) {
										if (index == GameConstants.XFGD_MAX_INDEX - 4) {
											for (int color_index = 3; color_index >= 0; color_index--) {
												if (IndexResult.card_index[color_index][index] > 0) {
													for (int j = 0; j < IndexResult.card_index[color_index][index]; j++) {
														card_data[card_out_count++] = IndexResult.card_data[color_index][index][j];
														if (card_out_count >= out_card_count) {
															break;
														}
													}
												}
												IndexResult.card_index[color_index][index] = 0;
												if (card_out_count >= out_card_count) {
													break;
												}
											}
										} else {
											if (IndexResult.card_index[color][index] > 0) {
												for (int j = 0; j < IndexResult.card_index[color][index]; j++) {
													card_data[card_out_count++] = IndexResult.card_data[color][index][j];
													if (card_out_count >= out_card_count) {
														break;
													}
												}
												IndexResult.card_index[color][index] = 0;
											}
										}
										if (card_out_count >= out_card_count) {
											return card_out_count;
										}
									}
								} else {
									for (int index = GameConstants.XFGD_MAX_INDEX - 5; index >= 0; index--) {
										if (IndexResult.card_index[color][index] > 0) {
											for (int j = 0; j < IndexResult.card_index[color][index]; j++) {
												card_data[card_out_count++] = IndexResult.card_data[color][index][j];
												if (card_out_count >= out_card_count) {
													break;
												}
											}
											IndexResult.card_index[color][index] = 0;
										}
										if (card_out_count >= out_card_count) {
											return card_out_count;
										}
									}
								}

							}
						}

					} else {
						if (color == this._zhu_type) {

							for (int index = 0; index < GameConstants.XFGD_MAX_INDEX; index++) {
								if (index == GameConstants.XFGD_MAX_INDEX - 4) {
									for (int color_index = 3; color_index >= 0; color_index--) {
										if (IndexResult.card_index[color_index][index] >= 2) {
											for (int j = 0; j < IndexResult.card_index[color_index][index]; j++) {
												card_data[card_out_count++] = IndexResult.card_data[color_index][index][j];
											}
											IndexResult.card_index[color_index][index] = 0;
										}

									}
								} else {
									if (IndexResult.card_index[color][index] >= 2) {
										for (int j = 0; j < IndexResult.card_index[color][index]; j++) {
											card_data[card_out_count++] = IndexResult.card_data[color][index][j];
										}
										IndexResult.card_index[color][index] = 0;
									}
								}
							}
							if (type_index == type_card.type_count - 1) {
								if (card_out_count < out_card_count) {
									for (int index = GameConstants.XFGD_MAX_INDEX - 1; index >= 0; index--) {
										if (index == GameConstants.XFGD_MAX_INDEX - 4) {
											for (int color_index = 3; color_index >= 0; color_index--) {
												if (IndexResult.card_index[color_index][index] > 0) {
													for (int j = 0; j < IndexResult.card_index[color_index][index]; j++) {
														card_data[card_out_count++] = IndexResult.card_data[color_index][index][j];
														if (card_out_count >= out_card_count) {
															break;
														}
													}
												}
												IndexResult.card_index[color_index][index] = 0;
												if (card_out_count >= out_card_count) {
													break;
												}
											}
										} else {
											if (IndexResult.card_index[color][index] > 0) {
												for (int j = 0; j < IndexResult.card_index[color][index]; j++) {
													card_data[card_out_count++] = IndexResult.card_data[color][index][j];
													if (card_out_count >= out_card_count) {
														break;
													}
												}
												IndexResult.card_index[color][index] = 0;
											}
										}
										if (card_out_count >= out_card_count) {
											return card_out_count;
										}
									}
								}
							}

						} else {
							for (int index = 0; index < GameConstants.XFGD_MAX_INDEX - 4; index++) {
								if (IndexResult.card_index[color][index] >= 2) {
									for (int j = 0; j < IndexResult.card_index[color][index]; j++) {
										card_data[card_out_count++] = IndexResult.card_data[color][index][j];
									}
									IndexResult.card_index[color][index] = 0;
								}
							}
							if (type_index == type_card.type_count - 1) {
								if (card_out_count < out_card_count) {
									for (int index = GameConstants.XFGD_MAX_INDEX - 1; index >= 0; index--) {
										if (IndexResult.card_index[color][index] > 0) {
											for (int j = 0; j < IndexResult.card_index[color][index]; j++) {
												card_data[card_out_count++] = IndexResult.card_data[color][index][j];
												if (card_out_count >= out_card_count) {
													break;
												}
											}
											IndexResult.card_index[color][index] = 0;
										}
										if (card_out_count >= out_card_count) {
											return card_out_count;
										}
									}
								}
							}

						}

					}
				}

				return card_out_count;
			} else {
				if (out_type == GameConstants.XFGD_CT_SINGLE) {
					for (int i = 0; i < cbHandCardCount; i++) {
						if (color == this._zhu_type) {
							if (hand_card_data[i] > this._zhu_value) {
								card_data[card_out_count++] = hand_card_data[i];
							}
						} else if (this.GetCardColor(hand_card_data[i]) == GetCardColor(cbOutCardData[0])
								&& hand_card_data[i] < this._zhu_value) {
							card_data[card_out_count++] = hand_card_data[i];
						}
					}
				} else if (out_type == GameConstants.XFGD_CT_DOUBLE) {
					tagAnalyseIndexResult_SJ IndexResult = new tagAnalyseIndexResult_SJ();
					AnalysebCardDataToIndex(hand_card_data, cbHandCardCount, IndexResult);
					for (int i = 0; i < GameConstants.XFGD_MAX_INDEX - 4; i++) {
						if (IndexResult.card_index[color][i] >= 2) {
							for (int j = 0; j < IndexResult.card_index[color][i]; j++) {
								card_data[card_out_count++] = IndexResult.card_data[color][i][j];
							}
						}
					}
					if (color == this._zhu_type) {
						for (int i = GameConstants.XFGD_MAX_INDEX - 4; i < GameConstants.XFGD_MAX_INDEX; i++) {
							if (i == GameConstants.XFGD_MAX_INDEX - 4) {
								for (int color_index = 3; color_index >= 0; color_index--) {
									if (IndexResult.card_index[color_index][i] >= 2) {
										for (int j = 0; j < IndexResult.card_index[color_index][i]; j++) {
											card_data[card_out_count++] = IndexResult.card_data[color_index][i][j];
										}
									}
								}
							} else {
								if (IndexResult.card_index[color][i] >= 2) {
									for (int j = 0; j < IndexResult.card_index[color][i]; j++) {
										card_data[card_out_count++] = IndexResult.card_data[color][i][j];
									}
								}
							}

						}
					}
					if (card_out_count < out_card_count) {
						for (int i = 0; i < cbHandCardCount; i++) {
							if (color == this._zhu_type) {
								if (hand_card_data[i] > this._zhu_value) {
									card_data[card_out_count++] = hand_card_data[i];
									if (card_out_count >= out_card_count) {
										break;
									}
								}
							} else {
								if (this.GetCardColor(hand_card_data[i]) == color
										&& hand_card_data[i] < this._zhu_value) {
									card_data[card_out_count++] = hand_card_data[i];
									if (card_out_count >= out_card_count) {
										break;
									}
								}
							}
						}
					}
				} else {
					tagAnalyseIndexResult_SJ IndexResult = new tagAnalyseIndexResult_SJ();
					AnalysebCardDataToIndex(hand_card_data, cbHandCardCount, IndexResult);
					if (color == this._zhu_type) {
						for (int index = 0; index < GameConstants.XFGD_MAX_INDEX; index++) {
							if (index == GameConstants.XFGD_MAX_INDEX - 4) {
								for (int color_index = 3; color_index >= 0; color_index--) {
									if (IndexResult.card_index[color_index][index] >= 2) {
										for (int j = 0; j < IndexResult.card_index[color_index][index]; j++) {
											card_data[card_out_count++] = IndexResult.card_data[color_index][index][j];
										}
										IndexResult.card_index[color_index][index] = 0;
									}
								}
							} else {
								if (IndexResult.card_index[color][index] >= 2) {
									for (int j = 0; j < IndexResult.card_index[color][index]; j++) {
										card_data[card_out_count++] = IndexResult.card_data[color][index][j];
									}
									IndexResult.card_index[color][index] = 0;
								}
							}
						}
					} else {
						for (int index = 0; index < GameConstants.XFGD_MAX_INDEX - 4; index++) {
							if (IndexResult.card_index[color][index] >= 2) {
								for (int j = 0; j < IndexResult.card_index[color][index]; j++) {
									card_data[card_out_count++] = IndexResult.card_data[color][index][j];

								}
								IndexResult.card_index[color][index] = 0;
							}
						}
					}
					if (card_out_count < out_card_count) {
						if (color == this._zhu_type) {
							for (int index = GameConstants.XFGD_MAX_INDEX - 1; index >= 0; index--) {
								if (index == GameConstants.XFGD_MAX_INDEX - 4) {
									for (int color_index = 3; color_index >= 0; color_index--) {
										if (IndexResult.card_index[color_index][index] > 0) {
											for (int j = 0; j < IndexResult.card_index[color_index][index]; j++) {
												card_data[card_out_count++] = IndexResult.card_data[color_index][index][j];
												if (card_out_count >= out_card_count) {
													break;
												}
											}
											IndexResult.card_index[color_index][index] = 0;
										}
										if (card_out_count >= out_card_count) {
											break;
										}
									}
								} else {
									if (IndexResult.card_index[color][index] > 0) {
										for (int j = 0; j < IndexResult.card_index[color][index]; j++) {
											card_data[card_out_count++] = IndexResult.card_data[color][index][j];
											if (card_out_count >= out_card_count) {
												break;
											}
										}
										IndexResult.card_index[color][index] = 0;
									}
								}
								if (card_out_count >= out_card_count) {
									break;
								}
							}
						} else {
							for (int index = GameConstants.XFGD_MAX_INDEX - 5; index >= 0; index--) {
								if (IndexResult.card_index[color][index] > 0) {
									for (int j = 0; j < IndexResult.card_index[color][index]; j++) {
										card_data[card_out_count++] = IndexResult.card_data[color][index][j];
										if (card_out_count >= out_card_count) {
											break;
										}
									}
									IndexResult.card_index[color][index] = 0;
									if (card_out_count >= out_card_count) {
										break;
									}
								}
							}
						}
					}
				}

				return card_out_count;
			}
		} else {
			for (int i = 0; i < cbHandCardCount; i++) {
				card_data[card_out_count++] = hand_card_data[i];
			}
			return card_out_count;
		}
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////
	public int GetCardLogicValue(int CardData) {
		// 扑克属性
		int cbCardColor = GetCardColor(CardData);
		int cbCardValue = GetCardValue(CardData);

		// 转换数值
		if (cbCardValue >= 14)
			return cbCardValue + 2;
		if (cbCardValue == 1 || cbCardValue == 2)
			return cbCardValue + 13;
		return cbCardValue;
	}

	// 获取数值
	public int GetCardValue(int cbCardData) {
		return cbCardData & GameConstants.LOGIC_MASK_VALUE;
	}

	// 获取花色
	public int GetCardColor(int cbCardData) {
		if (cbCardData == 0) {
			return -1;
		}
		if (this.GetCardValue(cbCardData) == 14 || this.GetCardValue(cbCardData) == 15) {
			return this._zhu_type;
		}
		if (cbCardData > _zhu_value && cbCardData < 0x3000) {
			return this._zhu_type;
		}
		return (cbCardData & GameConstants.LOGIC_MASK_COLOR) >> 4;
	}

	public int GetCardColor_Count(int cbCardData[], int card_count, int color) {
		int count = 0;
		for (int i = 0; i < card_count; i++) {
			if (color != this._zhu_type) {
				if (cbCardData[i] > this._zhu_value) {
					continue;
				}
			}
			if (GetCardColor(cbCardData[i]) == color) {
				count++;
			} else {
				if (color == _zhu_type && cbCardData[i] > this._zhu_value) {
					count++;
				}
			}
		}
		return count;
	}

	public int GetZhu_Count(int cbCardData[], int card_count) {
		int count = 0;
		for (int i = 0; i < card_count; i++) {
			if (cbCardData[i] > this._zhu_value) {
				count++;
			}
		}
		return count;
	}

	// 洗牌
	public void random_card_data(int return_cards[], final int mj_cards[]) {
		int card_count = return_cards.length;
		int card_data[] = new int[card_count];
		for (int i = 0; i < card_count; i++) {
			card_data[i] = mj_cards[i];
		}
		random_cards(card_data, return_cards, card_count);

	}

	// 混乱准备
	private static void random_cards(int card_data[], int return_cards[], int card_count) {
		// 混乱扑克
		int bRandCount = 0, bPosition = 0;
		do {
			bPosition = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % (card_count - bRandCount));
			return_cards[bRandCount++] = card_data[bPosition];
			card_data[bPosition] = card_data[card_count - bRandCount];
		} while (bRandCount < card_count);

	}

	/***
	 * //排列扑克
	 * 
	 * @param card_date
	 * @param card_count
	 * @return
	 */
	// 排列扑克
	public void SortCardList(int cbCardData[], int cbCardCount) {
		// 排序过虑
		if (cbCardCount == 0)
			return;
		// 转换数值
		int cbSortValue[] = new int[cbCardCount];
		for (int i = 0; i < cbCardCount; i++) {
			if (this.GetCardValue(cbCardData[i]) == 14 || this.GetCardValue(cbCardData[i]) == 15) {
				cbSortValue[i] = (4 << 4) + GetCardLogicValue(cbCardData[i]) + ((GetCardColor(cbCardData[i]) + 1) << 5);
			} else if (this.GetCardValue(cbCardData[i]) == 2) {

				if (cbCardData[i] > 0x3000) {
					cbSortValue[i] = (4 << 4) + GetCardLogicValue(cbCardData[i])
							+ ((GetCardColor(cbCardData[i]) + 1) << 2);
				} else {
					cbSortValue[i] = (4 << 4) + GetCardLogicValue(cbCardData[i])
							+ ((GetCardColor(cbCardData[i]) + 1) << 5);
				}
			} else if (cbCardData[i] > this._zhu_value) {
				cbSortValue[i] = (4 << 4) + GetCardLogicValue(cbCardData[i]);
			} else {
				cbSortValue[i] = (GetCardColor(cbCardData[i]) << 4) + GetCardLogicValue(cbCardData[i]);
			}

		}

		// 排序操作
		boolean bSorted = true;
		int cbSwitchData = 0, cbLast = cbCardCount - 1;
		do {
			bSorted = true;
			for (int i = 0; i < cbLast; i++) {
				if ((cbSortValue[i] < cbSortValue[i + 1]) || ((cbSortValue[i] == cbSortValue[i + 1])
						&& (GetCardLogicValue(cbCardData[i]) < GetCardLogicValue(cbCardData[i + 1])))) {
					// 设置标志
					bSorted = false;

					// 扑克数据
					cbSwitchData = cbCardData[i];
					cbCardData[i] = cbCardData[i + 1];
					cbCardData[i + 1] = cbSwitchData;

					// 排序权位
					cbSwitchData = cbSortValue[i];
					cbSortValue[i] = cbSortValue[i + 1];
					cbSortValue[i + 1] = cbSwitchData;
				}
			}
			cbLast--;
		} while (bSorted == false);

		return;
	}

	// 删除扑克
	public boolean remove_cards_by_data(int cards[], int card_count, int remove_cards[], int remove_count) {

		// 检验数据
		if (card_count < remove_count)
			return false;

		// 定义变量
		int cbDeleteCount = 0;
		int cbTempCardData[] = new int[card_count];

		for (int i = 0; i < card_count; i++) {
			cbTempCardData[i] = cards[i];
		}

		// 置零扑克
		for (int i = 0; i < remove_count; i++) {
			for (int j = 0; j < card_count; j++) {
				if (remove_cards[i] == cbTempCardData[j]) {
					cbDeleteCount++;
					cbTempCardData[j] = 0;
					break;
				}
			}
		}

		// 成功判断
		if (cbDeleteCount != remove_count) {
			return false;
		}

		// 清理扑克
		int cbCardPos = 0;
		for (int i = 0; i < card_count; i++) {
			if (cbTempCardData[i] != 0)
				cards[cbCardPos++] = cbTempCardData[i];
		}

		return true;
	}

	public int GetCardScore(int cbCardData[], int cbCardCount) {
		int score = 0;
		for (int i = 0; i < cbCardCount; i++) {
			if (this.GetCardLogicValue(cbCardData[i]) == 5) {
				score += 5;
			} else if (this.GetCardLogicValue(cbCardData[i]) == 10 || this.GetCardLogicValue(cbCardData[i]) == 13) {
				score += 10;
			}
		}
		return score;
	}

	public int get_card_double(int cbCardData[], int cbCardCount) {
		int double_count = 0;
		tagAnalyseIndexResult_SJ IndexResult = new tagAnalyseIndexResult_SJ();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, IndexResult);
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < GameConstants.XFGD_MAX_INDEX; j++) {
				if (IndexResult.card_index[i][j] == 2) {
					double_count++;
				}
			}
		}

		return double_count;
	}

	public boolean has_rule(int cbRule) {
		return ruleMap.containsKey(cbRule);
	}

}
