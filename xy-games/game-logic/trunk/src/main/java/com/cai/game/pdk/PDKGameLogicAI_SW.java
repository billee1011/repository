/**
 * 
 */
package com.cai.game.pdk;

import java.util.HashMap;
import java.util.Map;

import com.cai.common.constant.GameConstants;
import com.cai.game.pdk.data.tagAnalyseIndexResult;

public class PDKGameLogicAI_SW {
	public Map<Integer, Integer> ruleMap = new HashMap<>();

	public PDKGameLogicAI_SW() {

	}

	// 机器人算法
	public int Ai_Out_Card(int cbHandCardData[], int cbHandCardCount, int cbOutCardData[], int out_card_count,
			int card_data[], boolean max_must) {
		int card_count = 0;
		int card_type = this.GetCardType(cbOutCardData, out_card_count);

		switch (card_type) {
		case GameConstants.PDK_CT_SINGLE: {
			return search_out_card_single(cbHandCardData, cbHandCardCount, cbOutCardData, out_card_count, card_data,
					card_count, max_must);
		}
		case GameConstants.PDK_CT_DOUBLE: {
			return search_out_card_double(cbHandCardData, cbHandCardCount, cbOutCardData, out_card_count, card_data,
					card_count);
		}
		case GameConstants.PDK_CT_THREE_TAKE_TWO: {
			return search_out_card_three_take_two(cbHandCardData, cbHandCardCount, cbOutCardData, out_card_count,
					card_data, card_count);
		}
		case GameConstants.PDK_CT_SINGLE_LINE: {
			return search_out_card_single_link(cbHandCardData, cbHandCardCount, cbOutCardData, out_card_count,
					card_data, card_count);
		}
		case GameConstants.PDK_CT_DOUBLE_LINE: {
			return search_out_card_double_link(cbHandCardData, cbHandCardCount, cbOutCardData, out_card_count,
					card_data, card_count);
		}
		case GameConstants.PDK_CT_PLANE: {
			return search_out_card_plane(cbHandCardData, cbHandCardCount, cbOutCardData, out_card_count, card_data,
					card_count);
		}
		case GameConstants.PDK_CT_FOUR_LINE_TAKE_THREE: {
			return search_out_card_four_take_three(cbHandCardData, cbHandCardCount, cbOutCardData, out_card_count,
					card_data, card_count);
		}
		case GameConstants.PDK_CT_BOMB_CARD: {
			return search_out_card_bomb(cbHandCardData, cbHandCardCount, cbOutCardData, out_card_count, card_data,
					card_count);
		}
		case GameConstants.PDK_CT_ERROR: {
			return search_out_card_error(cbHandCardData, cbHandCardCount, cbOutCardData, out_card_count, card_data,
					card_count, max_must);
		}
		}
		return card_count;

	}

	public int search_out_card_first_out(int cbHandCardData[], int cbHandCardCount, int cbOutCardData[],
			int out_card_count, int card_data[]) {
		int card_count = 0;
		int max_num = 4;
		if (!this.has_rule(GameConstants.GAME_RULE_BOOM)) {
			max_num = 5;
		}
		tagAnalyseIndexResult hand_index = new tagAnalyseIndexResult();
		AnalysebCardDataToIndex(cbHandCardData, cbHandCardCount, hand_index);
		if (has_rule(GameConstants.GAME_RULE_TWO_PLAY)) {
			for (int i = 0; i < GameConstants.PDK_MAX_INDEX; i++) {
				if (hand_index.card_index[i] > 0) {
					if (hand_index.card_index[i] == 4 && !this.has_rule(GameConstants.GAME_RULE_BOOM)) {
						for (int j = hand_index.card_index[i] - 1; j >= 0; j--) {
							card_data[card_count++] = hand_index.card_data[i][j];
						}
						// 判断带牌
						for (int count = 1; count < max_num; count++) {
							for (int j = cbHandCardCount - 1; j >= 0;) {
								int take_index = switch_card_to_idnex(cbHandCardData[j]);
								if (hand_index.card_index[take_index] == count && take_index < 10 && take_index != i) {
									for (int x = 0; x < hand_index.card_index[take_index]; x++) {
										card_data[card_count++] = hand_index.card_data[take_index][x];
										if (card_count == 5) {
											return card_count;
										}
									}
								}
								if (hand_index.card_index[take_index] > 0) {
									j -= hand_index.card_index[take_index];
								} else {
									j--;
								}
							}
						}
						// 判断带牌
						for (int count = 1; count < max_num; count++) {
							for (int j = cbHandCardCount - 1; j >= 0;) {
								int take_index = switch_card_to_idnex(cbHandCardData[j]);
								if (hand_index.card_index[take_index] == count && take_index >= 10 && take_index != i) {
									for (int x = 0; x < hand_index.card_index[take_index]; x++) {
										card_data[card_count++] = hand_index.card_data[take_index][x];
										if (card_count == 5) {
											return card_count;
										}
									}
								}
								if (hand_index.card_index[take_index] > 0) {
									j -= hand_index.card_index[take_index];
								} else {
									j--;
								}
							}
						}
					} else if (hand_index.card_index[i] != 3) {
						for (int j = 0; j < hand_index.card_index[i]; j++) {
							card_data[card_count++] = hand_index.card_data[i][j];
						}
						return card_count;
					} else {
						for (int j = 0; j < hand_index.card_index[i]; j++) {
							card_data[card_count++] = hand_index.card_data[i][j];
						}
						// 判断带牌
						for (int count = 1; count < max_num; count++) {
							for (int j = cbHandCardCount - 1; j >= 0;) {
								int take_index = switch_card_to_idnex(cbHandCardData[j]);
								if (hand_index.card_index[take_index] == count && take_index < 10 && take_index != i) {
									for (int x = 0; x < hand_index.card_index[take_index]; x++) {
										card_data[card_count++] = hand_index.card_data[take_index][x];
										if (card_count == 5) {
											return card_count;
										}
									}
								}
								if (hand_index.card_index[take_index] > 0) {
									j -= hand_index.card_index[take_index];
								} else {
									j--;
								}
							}
						}
						// 判断带牌
						for (int count = 1; count < max_num; count++) {
							for (int j = cbHandCardCount - 1; j >= 0;) {
								int take_index = switch_card_to_idnex(cbHandCardData[j]);
								if (hand_index.card_index[take_index] == count && take_index >= 10 && take_index != i) {
									for (int x = 0; x < hand_index.card_index[take_index]; x++) {
										card_data[card_count++] = hand_index.card_data[take_index][x];
										if (card_count == 5) {
											return card_count;
										}
									}
								}
								if (hand_index.card_index[take_index] > 0) {
									j -= hand_index.card_index[take_index];
								} else {
									j--;
								}
							}
						}
					}
				}
			}
		} else {
			if (has_rule(GameConstants.GAME_RULE_SHOU_JU_HEITAO_SAN)) {
				if (hand_index.card_index[0] != 3) {
					for (int j = 0; j < hand_index.card_index[0]; j++) {
						card_data[card_count++] = hand_index.card_data[0][j];
					}
					return card_count;
				} else {
					for (int j = 0; j < hand_index.card_index[0]; j++) {
						card_data[card_count++] = hand_index.card_data[0][j];
					}
					// 判断带牌
					for (int count = 1; count < max_num; count++) {
						for (int j = cbHandCardCount - 1; j >= 0;) {
							int take_index = switch_card_to_idnex(cbHandCardData[j]);
							if (hand_index.card_index[take_index] == count && take_index < 10 && take_index != 0) {
								for (int x = 0; x < hand_index.card_index[take_index]; x++) {
									card_data[card_count++] = hand_index.card_data[take_index][x];
									if (card_count == 5) {
										return card_count;
									}
								}
							}
							if (hand_index.card_index[take_index] > 0) {
								j -= hand_index.card_index[take_index];
							} else {
								j--;
							}
						}
					}
					// 判断带牌
					for (int count = 1; count < max_num; count++) {
						for (int j = cbHandCardCount - 1; j >= 0;) {
							int take_index = switch_card_to_idnex(cbHandCardData[j]);
							if (hand_index.card_index[take_index] == count && take_index >= 10 && take_index != 0) {
								for (int x = 0; x < hand_index.card_index[take_index]; x++) {
									card_data[card_count++] = hand_index.card_data[take_index][x];
									if (card_count == 5) {
										return card_count;
									}
								}
							}
							if (hand_index.card_index[take_index] > 0) {
								j -= hand_index.card_index[take_index];
							} else {
								j--;
							}
						}
					}
				}
			} else {
				return search_out_card_error(cbHandCardData, cbHandCardCount, cbOutCardData, out_card_count, card_data,
						card_count, false);
			}
		}
		return card_count;
	}

	public int search_out_card_error(int cbHandCardData[], int cbHandCardCount, int cbOutCardData[], int out_card_count,
			int card_data[], int card_count, boolean max_must) {

		int max_num = 4;
		if (!this.has_rule(GameConstants.GAME_RULE_BOOM)) {
			max_num = 5;
		}
		tagAnalyseIndexResult hand_index = new tagAnalyseIndexResult();
		AnalysebCardDataToIndex(cbHandCardData, cbHandCardCount, hand_index);
		// 搜索对子单张
		for (int count = 1; count < 3; count++) {
			if (max_must && count == 1) {
				for (int i = 0; i < cbHandCardCount;) {
					int index = switch_card_to_idnex(cbHandCardData[i]);
					if (hand_index.card_index[index] < 3) {
						for (int j = 0; j < hand_index.card_index[index]; j++) {
							card_data[card_count++] = hand_index.card_data[index][j];
						}
						return card_count;
					} else {
						break;
					}
				}
			} else {
				for (int i = cbHandCardCount - 1; i >= 0;) {
					int index = switch_card_to_idnex(cbHandCardData[i]);
					if (hand_index.card_index[index] == count) {
						for (int j = 0; j < hand_index.card_index[index]; j++) {
							card_data[card_count++] = hand_index.card_data[index][j];
						}
						return card_count;
					}
					if (hand_index.card_index[index] > 0) {
						i -= hand_index.card_index[index];
					} else {
						i--;
					}
				}
			}

		}

		// 搜索三带
		for (int i = cbHandCardCount - 1; i >= 0;) {
			int index = switch_card_to_idnex(cbHandCardData[i]);
			if (hand_index.card_index[index] == 3) {
				// 判断带牌
				for (int j = 0; j < hand_index.card_index[index]; j++) {
					card_data[card_count++] = hand_index.card_data[index][j];
				}
				for (int count = 1; count < max_num; count++) {
					for (int j = cbHandCardCount - 1; j >= 0;) {
						int take_index = switch_card_to_idnex(cbHandCardData[j]);
						if (hand_index.card_index[take_index] == count && take_index < 10 && take_index != index) {
							for (int x = 0; x < hand_index.card_index[take_index]; x++) {
								card_data[card_count++] = hand_index.card_data[take_index][x];
								if (card_count == 5) {
									return card_count;
								}
							}
						}
						if (hand_index.card_index[take_index] > 0) {
							j -= hand_index.card_index[take_index];
						} else {
							j--;
						}
					}
				}
				// 判断带牌
				for (int count = 1; count < max_num; count++) {
					for (int j = cbHandCardCount - 1; j >= 0;) {
						int take_index = switch_card_to_idnex(cbHandCardData[j]);
						if (hand_index.card_index[take_index] == count && take_index >= 10 && take_index != index) {
							for (int x = 0; x < hand_index.card_index[take_index]; x++) {
								card_data[card_count++] = hand_index.card_data[take_index][x];
								if (card_count == 5) {
									return card_count;
								}
							}
						}
						if (hand_index.card_index[take_index] > 0) {
							j -= hand_index.card_index[take_index];
						} else {
							j--;
						}
					}
				}
			}
			if (hand_index.card_index[index] > 0) {
				i -= hand_index.card_index[index];
			} else {
				i--;
			}
		}

		card_count = 0;
		for (int i = cbHandCardCount - 1; i >= 0;) {
			int index = switch_card_to_idnex(cbHandCardData[i]);
			if (hand_index.card_index[index] == 4) {
				for (int j = 0; j < hand_index.card_index[index]; j++) {
					card_data[card_count++] = hand_index.card_data[index][j];
				}
				return card_count;
			}
			if (hand_index.card_index[index] > 0) {
				i -= hand_index.card_index[index];
			} else {
				i--;
			}
		}
		return card_count;

	}

	public int search_out_card_bomb(int cbHandCardData[], int cbHandCardCount, int cbOutCardData[], int out_card_count,
			int card_data[], int card_count) {
		tagAnalyseIndexResult hand_index = new tagAnalyseIndexResult();
		AnalysebCardDataToIndex(cbHandCardData, cbHandCardCount, hand_index);

		int turn_index = this.switch_card_to_idnex(cbOutCardData[0]);
		for (int i = cbHandCardCount - 1; i >= 0;) {
			int index = switch_card_to_idnex(cbHandCardData[i]);
			if (index > turn_index && hand_index.card_index[index] == 4) {
				for (int j = 0; j < hand_index.card_index[index]; j++) {
					card_data[card_count++] = hand_index.card_data[index][j];
				}
				return card_count;
			}
			if (hand_index.card_index[index] > 0) {
				i -= hand_index.card_index[index];
			} else {
				i--;
			}
		}
		return card_count;

	}

	public int search_out_card_four_take_three(int cbHandCardData[], int cbHandCardCount, int cbOutCardData[],
			int out_card_count, int card_data[], int card_count) {
		tagAnalyseIndexResult hand_index = new tagAnalyseIndexResult();
		AnalysebCardDataToIndex(cbHandCardData, cbHandCardCount, hand_index);

		if (this.has_rule(GameConstants.GAME_RULE_BOOM)) {
			for (int i = 0; i < cbHandCardCount;) {
				int index = switch_card_to_idnex(cbHandCardData[i]);
				if (hand_index.card_index[index] == 4) {
					for (int j = 0; j < hand_index.card_index[index]; j++) {
						card_data[card_count++] = hand_index.card_data[index][j];
					}
					return card_count;
				}
				if (hand_index.card_index[index] > 0) {
					i += hand_index.card_index[index];
				} else {
					i++;
				}
			}
		} else {
			int turn_index = this.switch_card_to_idnex(cbOutCardData[0]);
			for (int i = cbHandCardCount - 1; i >= 0;) {
				int index = switch_card_to_idnex(cbHandCardData[i]);
				if (index > turn_index && hand_index.card_index[index] == 4) {
					if (cbHandCardCount < out_card_count) {
						if (this.has_rule(GameConstants.GAME_RULE_THREE_LOST_NENG_JIE)) {
							for (int j = 0; j < cbHandCardCount; j++) {
								card_data[card_count++] = cbHandCardData[j];
							}
							return card_count;

						} else {
							return 0;
						}
					} else {
						for (int j = 0; j < hand_index.card_index[index]; j++) {
							card_data[card_count++] = hand_index.card_data[index][j];
						}
						// 判断带牌
						for (int count = 1; count < 5; count++) {
							for (int j = cbHandCardCount - 1; j >= 0;) {
								int take_index = switch_card_to_idnex(cbHandCardData[j]);
								if (hand_index.card_index[take_index] == count && take_index < 10
										&& take_index != index) {
									for (int x = 0; x < hand_index.card_index[take_index]; x++) {
										card_data[card_count++] = hand_index.card_data[take_index][x];
										if (card_count == out_card_count) {
											return card_count;
										}
									}
								}
								if (hand_index.card_index[take_index] > 0) {
									j -= hand_index.card_index[take_index];
								} else {
									j--;
								}
							}
						}
						// 判断带牌
						for (int count = 1; count < 5; count++) {
							for (int j = cbHandCardCount - 1; j >= 0;) {
								int take_index = switch_card_to_idnex(cbHandCardData[j]);
								if (hand_index.card_index[take_index] == count && take_index >= 10
										&& take_index != index) {
									for (int x = 0; x < hand_index.card_index[take_index]; x++) {
										card_data[card_count++] = hand_index.card_data[take_index][x];
										if (card_count == out_card_count) {
											return card_count;
										}
									}
								}
								if (hand_index.card_index[take_index] > 0) {
									j -= hand_index.card_index[take_index];
								} else {
									j--;
								}
							}
						}
					}

					return card_count;
				}
				if (hand_index.card_index[index] > 0) {
					i -= hand_index.card_index[index];
				} else {
					i--;
				}
			}
		}
		return card_count;
	}

	public int search_out_card_plane(int cbHandCardData[], int cbHandCardCount, int cbOutCardData[], int out_card_count,
			int card_data[], int card_count) {
		tagAnalyseIndexResult hand_index = new tagAnalyseIndexResult();
		AnalysebCardDataToIndex(cbHandCardData, cbHandCardCount, hand_index);

		int max_index = this.switch_card_to_idnex(cbOutCardData[0]);
		int min_index = this.switch_card_to_idnex(cbOutCardData[out_card_count - 1]);
		int max_num = 4;
		if (!this.has_rule(GameConstants.GAME_RULE_BOOM)) {
			max_num = 5;
		}
		for (int index = 0; index < GameConstants.PDK_MAX_INDEX; index++) {
			if (hand_index.card_index[index] < max_num && hand_index.card_index[index] >= 3 && index > min_index) {
				for (int other_index = index + 1; other_index < GameConstants.PDK_MAX_INDEX; other_index++) {
					if (hand_index.card_index[other_index] < max_num && hand_index.card_index[other_index] >= 3) {
						if ((other_index - index) + 1 == out_card_count / 5) {
							if (cbHandCardCount >= out_card_count) {
								for (int x = other_index; x >= index; x--) {
									for (int y = 0; y < hand_index.card_index[x]; y++) {
										card_data[card_count++] = hand_index.card_data[x][y];
									}
								}
								for (int take_index = 0; take_index < GameConstants.PDK_MAX_INDEX; take_index++) {
									if (take_index < index || take_index > other_index
											&& hand_index.card_index[take_index] < max_num) {
										for (int y = 0; y < hand_index.card_index[take_index]; y++) {
											card_data[card_count++] = hand_index.card_data[take_index][y];
											if (card_count == out_card_count) {
												return card_count;
											}
										}
									}
								}
							} else if (this.has_rule(GameConstants.GAME_RULE_THREE_LOST_NENG_JIE)) {
								for (int x = other_index; x >= index; x--) {
									for (int y = 0; y < hand_index.card_index[x]; y++) {
										card_data[card_count++] = hand_index.card_data[x][y];
									}
								}
								for (int take_index = 0; take_index < GameConstants.PDK_MAX_INDEX; take_index++) {
									if (take_index < index || take_index > other_index
											&& hand_index.card_index[take_index] < max_num) {
										for (int y = 0; y < hand_index.card_index[take_index]; y++) {
											card_data[card_count++] = hand_index.card_data[take_index][y];
											if (card_count == cbHandCardCount) {
												return card_count;
											}
										}
									}
								}
							}

						}

					} else {
						index = other_index;
						break;
					}
				}
			}
		}

		// 找不到直接找炸弹
		card_count = 0;
		for (int index = 0; index < GameConstants.PDK_MAX_INDEX; index++) {
			if (hand_index.card_index[index] == 4) {
				for (int j = 0; j < hand_index.card_index[index]; j++) {
					card_data[card_count++] = hand_index.card_data[index][j];
				}
				return card_count;
			}
			if (this.has_rule(GameConstants.GAME_RULE_KKK_BOOM)) {
				if (hand_index.card_index[index] == 3 && index == 11) {
					for (int j = 0; j < hand_index.card_index[index]; j++) {
						card_data[card_count++] = hand_index.card_data[index][j];
					}
					return card_count;
				}
			}
		}
		return card_count;
	}

	public int search_out_card_double_link(int cbHandCardData[], int cbHandCardCount, int cbOutCardData[],
			int out_card_count, int card_data[], int card_count) {
		tagAnalyseIndexResult hand_index = new tagAnalyseIndexResult();
		AnalysebCardDataToIndex(cbHandCardData, cbHandCardCount, hand_index);
		int max_num = 4;
		if (!this.has_rule(GameConstants.GAME_RULE_BOOM)) {
			max_num = 5;
		}
		int max_index = this.switch_card_to_idnex(cbOutCardData[0]);
		int min_index = this.switch_card_to_idnex(cbOutCardData[out_card_count - 1]);
		for (int i = cbHandCardCount - 1; i >= 0;) {
			int index = this.switch_card_to_idnex(cbHandCardData[i]);
			int prv_index = index;
			if (hand_index.card_index[index] < max_num && hand_index.card_index[index] >= 2 && index > min_index) {
				for (int j = i - hand_index.card_index[index]; j >= 0;) {
					int other_index = this.switch_card_to_idnex(cbHandCardData[j]);
					if (hand_index.card_index[other_index] < max_num && hand_index.card_index[other_index] >= 2
							&& prv_index == other_index - 1) {
						prv_index = other_index;
						if ((prv_index - index) + 1 == out_card_count / 2) {
							for (int x = prv_index; x >= index; x--) {
								card_data[card_count++] = hand_index.card_data[x][0];
								card_data[card_count++] = hand_index.card_data[x][1];
							}
							return card_count;
						}

					} else {
						break;
					}
					if (hand_index.card_index[other_index] > 0) {
						j -= hand_index.card_index[other_index];
					} else {
						j--;
					}
				}
			}

			if (hand_index.card_index[index] > 0) {
				i -= hand_index.card_index[index];
			} else {
				i--;
			}
		}

		// 找不到直接找炸弹
		card_count = 0;
		if (this.has_rule(GameConstants.GAME_RULE_BOOM)) {
			for (int i = 0; i < cbHandCardCount;) {
				int index = switch_card_to_idnex(cbHandCardData[i]);
				if (hand_index.card_index[index] == 4) {
					for (int j = 0; j < hand_index.card_index[index]; j++) {
						card_data[card_count++] = hand_index.card_data[index][j];
					}
					return card_count;
				}
				if (hand_index.card_index[index] > 0) {
					i += hand_index.card_index[index];
				} else {
					i++;
				}
			}
		}
		return card_count;
	}

	public int search_out_card_single_link(int cbHandCardData[], int cbHandCardCount, int cbOutCardData[],
			int out_card_count, int card_data[], int card_count) {
		tagAnalyseIndexResult hand_index = new tagAnalyseIndexResult();
		AnalysebCardDataToIndex(cbHandCardData, cbHandCardCount, hand_index);
		int max_num = 4;
		if (!this.has_rule(GameConstants.GAME_RULE_BOOM)) {
			max_num = 5;
		}
		int max_index = this.switch_card_to_idnex(cbOutCardData[0]);
		int min_index = this.switch_card_to_idnex(cbOutCardData[out_card_count - 1]);
		for (int i = cbHandCardCount - 1; i >= 0;) {
			int index = this.switch_card_to_idnex(cbHandCardData[i]);
			int prv_index = index;
			if (hand_index.card_index[index] < max_num && hand_index.card_index[index] > 0 && index > min_index) {
				for (int j = i - hand_index.card_index[index]; j >= 0;) {
					int other_index = this.switch_card_to_idnex(cbHandCardData[j]);
					if (hand_index.card_index[other_index] < 4 && hand_index.card_index[other_index] > 0
							&& prv_index == other_index - 1) {
						prv_index = other_index;
						if ((prv_index - index) + 1 == out_card_count) {
							for (int x = prv_index; x >= index; x--) {
								card_data[card_count++] = hand_index.card_data[x][0];
							}
							return card_count;
						}

					} else {
						break;
					}
					if (hand_index.card_index[other_index] > 0) {
						j -= hand_index.card_index[other_index];
					} else {
						j--;
					}
				}
			}

			if (hand_index.card_index[index] > 0) {
				i -= hand_index.card_index[index];
			} else {
				i--;
			}
		}

		// 找不到直接找炸弹
		card_count = 0;
		if (this.has_rule(GameConstants.GAME_RULE_BOOM)) {
			for (int i = 0; i < cbHandCardCount;) {
				int index = switch_card_to_idnex(cbHandCardData[i]);
				if (hand_index.card_index[index] == 4) {
					for (int j = 0; j < hand_index.card_index[index]; j++) {
						card_data[card_count++] = hand_index.card_data[index][j];
					}
					return card_count;
				}
				if (hand_index.card_index[index] > 0) {
					i += hand_index.card_index[index];
				} else {
					i++;
				}
			}
		}
		return card_count;
	}

	public int search_out_card_three_take_two(int cbHandCardData[], int cbHandCardCount, int cbOutCardData[],
			int out_card_count, int card_data[], int card_count) {
		int max_num = 4;
		if (!this.has_rule(GameConstants.GAME_RULE_BOOM)) {
			max_num = 5;
		}
		tagAnalyseIndexResult hand_index = new tagAnalyseIndexResult();
		AnalysebCardDataToIndex(cbHandCardData, cbHandCardCount, hand_index);
		int turn_index = this.switch_card_to_idnex(cbOutCardData[0]);
		for (int i = cbHandCardCount - 1; i >= 0;) {
			int index = switch_card_to_idnex(cbHandCardData[i]);
			if (index > turn_index && hand_index.card_index[index] >= 3 && hand_index.card_index[index] < max_num) {
				if (cbHandCardCount < out_card_count) {
					if (this.has_rule(GameConstants.GAME_RULE_THREE_LOST_NENG_JIE)) {
						for (int j = 0; j < cbHandCardCount; j++) {
							card_data[card_count++] = cbHandCardData[j];
						}
					} else {
						return 0;
					}
				} else {
					for (int j = 0; j < hand_index.card_index[index]; j++) {
						card_data[card_count++] = hand_index.card_data[index][j];
					}
					// 判断带牌
					for (int count = 1; count < max_num; count++) {
						for (int j = cbHandCardCount - 1; j >= 0;) {
							int take_index = switch_card_to_idnex(cbHandCardData[j]);
							if (hand_index.card_index[take_index] == count && take_index < 10 && take_index != index) {
								for (int x = 0; x < hand_index.card_index[take_index]; x++) {
									card_data[card_count++] = hand_index.card_data[take_index][x];
									if (card_count == out_card_count) {
										return card_count;
									}
								}
							}
							if (hand_index.card_index[take_index] > 0) {
								j -= hand_index.card_index[take_index];
							} else {
								j--;
							}
						}
					}
					// 判断带牌
					for (int count = 1; count < max_num; count++) {
						for (int j = cbHandCardCount - 1; j >= 0;) {
							int take_index = switch_card_to_idnex(cbHandCardData[j]);
							if (hand_index.card_index[take_index] == count && take_index >= 10 && take_index != index) {
								for (int x = 0; x < hand_index.card_index[take_index]; x++) {
									card_data[card_count++] = hand_index.card_data[take_index][x];
									if (card_count == out_card_count) {
										return card_count;
									}
								}
							}
							if (hand_index.card_index[take_index] > 0) {
								j -= hand_index.card_index[take_index];
							} else {
								j--;
							}
						}
					}
				}

				return card_count;
			}
			if (hand_index.card_index[index] > 0) {
				i -= hand_index.card_index[index];
			} else {
				i--;
			}
		}

		// 找不到直接找炸弹
		card_count = 0;
		if (this.has_rule(GameConstants.GAME_RULE_BOOM)) {
			for (int i = 0; i < cbHandCardCount;) {
				int index = switch_card_to_idnex(cbHandCardData[i]);
				if (hand_index.card_index[index] == 4) {
					for (int j = 0; j < hand_index.card_index[index]; j++) {
						card_data[card_count++] = hand_index.card_data[index][j];
					}
					return card_count;
				}
				if (hand_index.card_index[index] > 0) {
					i += hand_index.card_index[index];
				} else {
					i++;
				}
			}
		}
		return card_count;
	}

	public int search_out_card_double(int cbHandCardData[], int cbHandCardCount, int cbOutCardData[],
			int out_card_count, int card_data[], int card_count) {
		int max_num = 4;
		if (!this.has_rule(GameConstants.GAME_RULE_BOOM)) {
			max_num = 5;
		}
		tagAnalyseIndexResult hand_index = new tagAnalyseIndexResult();
		AnalysebCardDataToIndex(cbHandCardData, cbHandCardCount, hand_index);
		int turn_index = this.switch_card_to_idnex(cbOutCardData[0]);
		for (int count = 2; count < max_num; count++) {
			for (int i = cbHandCardCount - 1; i >= 0;) {
				int index = switch_card_to_idnex(cbHandCardData[i]);
				if (index > turn_index && hand_index.card_index[index] == count) {
					card_data[card_count++] = hand_index.card_data[index][0];
					card_data[card_count++] = hand_index.card_data[index][1];
					return card_count;
				}
				if (hand_index.card_index[index] > 0) {
					i -= hand_index.card_index[index];
				} else {
					i--;
				}
			}
		}

		if (this.has_rule(GameConstants.GAME_RULE_BOOM)) {
			for (int i = cbHandCardCount - 1; i >= 0;) {
				int index = switch_card_to_idnex(cbHandCardData[i]);
				if (hand_index.card_index[index] == 4) {
					for (int j = 0; j < hand_index.card_index[index]; j++) {
						card_data[card_count++] = hand_index.card_data[index][j];
					}
					return card_count;
				}
				if (hand_index.card_index[index] > 0) {
					i -= hand_index.card_index[index];
				} else {
					i--;
				}
			}
		}
		return card_count;
	}

	public int search_out_card_single(int cbHandCardData[], int cbHandCardCount, int cbOutCardData[],
			int out_card_count, int card_data[], int card_count, boolean max_must) {
		int max_num = 4;
		if (!this.has_rule(GameConstants.GAME_RULE_BOOM)) {
			max_num = 5;
		}
		tagAnalyseIndexResult hand_index = new tagAnalyseIndexResult();
		AnalysebCardDataToIndex(cbHandCardData, cbHandCardCount, hand_index);
		int turn_index = this.switch_card_to_idnex(cbOutCardData[0]);

		if (!max_must) {
			for (int count = 1; count < max_num; count++) {
				for (int i = cbHandCardCount - 1; i >= 0;) {
					int index = switch_card_to_idnex(cbHandCardData[i]);
					if (index > turn_index && hand_index.card_index[index] == count) {
						card_data[card_count++] = hand_index.card_data[index][0];
						return card_count;
					}
					if (hand_index.card_index[index] > 0) {
						i -= hand_index.card_index[index];
					} else {
						i--;
					}
				}
			}
		} else {
			for (int i = 0; i < cbHandCardCount;) {
				int index = switch_card_to_idnex(cbHandCardData[i]);
				if (index > turn_index && hand_index.card_index[index] < max_num) {
					card_data[card_count++] = hand_index.card_data[index][0];
					return card_count;
				} else {
					break;
				}
			}
		}

		if (this.has_rule(GameConstants.GAME_RULE_BOOM)) {
			for (int i = cbHandCardCount - 1; i >= 0;) {
				int index = switch_card_to_idnex(cbHandCardData[i]);
				if (hand_index.card_index[index] == 4) {
					for (int j = 0; j < hand_index.card_index[index]; j++) {
						card_data[card_count++] = hand_index.card_data[index][j];
					}
					return card_count;
				}
				if (hand_index.card_index[index] > 0) {
					i -= hand_index.card_index[index];
				} else {
					i--;
				}
			}
		}
		return card_count;
	}

	public int GetCardType(int cbCardData[], int cbCardCount) {
		tagAnalyseIndexResult card_index = new tagAnalyseIndexResult();
		AnalysebCardDataToIndex(cbCardData, cbCardCount, card_index);

		int card_type = GameConstants.PDK_CT_ERROR;
		if (cbCardCount == 0) {
			return GameConstants.PDK_CT_ERROR;
		}
		if (cbCardCount == 1) {
			return GameConstants.PDK_CT_SINGLE;
		} else if (cbCardCount == 2) {
			for (int i = GameConstants.PDK_MAX_INDEX - 1; i >= 0; i--) {
				if (card_index.card_index[i] != 2 && card_index.card_index[i] != 0) {
					return GameConstants.PDK_CT_ERROR;
				}
				if (card_index.card_index[i] == 2 && card_index.card_index[i] != 0) {
					return GameConstants.PDK_CT_DOUBLE;
				}
			}
		} else if (cbCardCount == 3) {
			// 三条
			for (int i = GameConstants.PDK_MAX_INDEX - 1; i >= 0; i--) {
				if (card_index.card_index[i] != 3 && card_index.card_index[i] != 0) {
					return GameConstants.PDK_CT_ERROR;
				}
				if (card_index.card_index[i] == 3) {
					if (has_rule(GameConstants.GAME_RULE_KKK_BOOM) && i == 11) {
						return GameConstants.PDK_CT_BOMB_CARD;
					} else {
						return GameConstants.PDK_CT_THREE;
					}
				}
			}
		} else if (cbCardCount == 4) {
			// 炸弹
			if (this.has_rule(GameConstants.GAME_RULE_BOOM)) {
				for (int i = GameConstants.PDK_MAX_INDEX - 1; i >= 0; i--) {
					if (card_index.card_index[i] != 4 && card_index.card_index[i] != 0) {
						break;
					}
					if (card_index.card_index[i] == 4) {
						return GameConstants.PDK_CT_BOMB_CARD;
					}
				}
			} else {
				for (int i = GameConstants.PDK_MAX_INDEX - 1; i >= 0; i--) {
					if (card_index.card_index[i] != 4 && card_index.card_index[i] != 0) {
						break;
					}
					if (card_index.card_index[i] == 4) {
						return GameConstants.PDK_CT_THREE_TAKE_ONE;
					}
				}
			}

			// 飞机缺翅膀
			for (int i = GameConstants.PDK_MAX_INDEX - 1; i >= 0; i--) {
				if (card_index.card_index[i] == 3) {
					return GameConstants.PDK_CT_THREE_TAKE_ONE;
				}
			}

		} else if (cbCardCount == 5) {

			// 三带2
			for (int i = GameConstants.PDK_MAX_INDEX - 1; i >= 0; i--) {
				if (card_index.card_index[i] == 3) {
					return GameConstants.PDK_CT_THREE_TAKE_TWO;
				} else if (card_index.card_index[i] == 4) {
					if (has_rule(GameConstants.GAME_RULE_ZHADAN_BUKECHAI)) {
						return GameConstants.PDK_CT_ERROR;
					} else {
						return GameConstants.PDK_CT_THREE_TAKE_TWO;
					}
				}
			}
			// 四带1
			if (has_rule(GameConstants.GAME_RULE_FOUR_DAI_SAN)) {
				for (int i = GameConstants.PDK_MAX_INDEX - 1; i >= 0; i--) {
					if (card_index.card_index[i] == 4) {
						return GameConstants.PDK_CT_FOUR_LINE_TAKE_ONE;
					}
				}
			}
		} else if (cbCardCount == 6) {
			// 四带2
			if (has_rule(GameConstants.GAME_RULE_FOUR_DAI_SAN)) {
				for (int i = GameConstants.PDK_MAX_INDEX - 1; i >= 0; i--) {
					if (card_index.card_index[i] == 4) {
						return GameConstants.PDK_CT_FOUR_LINE_TAKE_TWO;
					}
				}
			}

		} else if (cbCardCount == 7) {
			// 四带3
			if (has_rule(GameConstants.GAME_RULE_FOUR_DAI_SAN)) {
				for (int i = GameConstants.PDK_MAX_INDEX - 1; i >= 0; i--) {
					if (card_index.card_index[i] == 4) {
						return GameConstants.PDK_CT_FOUR_LINE_TAKE_THREE;
					}
				}
			}
		}

		// 飞机
		for (int i = GameConstants.PDK_MAX_INDEX - 1; i >= 0; i--) {
			if (card_index.card_index[i] >= 3) {
				for (int j = i - 1; j >= 0; j--) {
					if (card_index.card_index[j] >= 3) {
						if ((i - j + 1) * 5 == cbCardCount) {
							return GameConstants.PDK_CT_PLANE;
						}
						if (j == 0) {
							if ((i - j + 1) * 5 > cbCardCount && i - j > 0) {
								return GameConstants.PDK_CT_PLANE_LOST;
							} else {
								return GameConstants.PDK_CT_ERROR;
							}
						}
					} else {
						if ((i - j) * 5 > cbCardCount && i - j > 1) {
							return GameConstants.PDK_CT_PLANE_LOST;
						} else {
							i = j;
							break;
						}
					}
				}
			}
		}
		// 连对
		for (int i = GameConstants.PDK_MAX_INDEX - 1; i >= 0; i--) {
			if (card_index.card_index[i] != 2 && card_index.card_index[i] != 0) {
				break;
			}
			if (cbCardCount % 2 != 0) {
				break;
			}
			int link_number = cbCardCount / 2;

			if (card_index.card_index[i] == 2 && i >= link_number - 1 && link_number >= 2) {
				for (int j = i - 1; j >= 0; j--) {
					if (card_index.card_index[j] != 2 && j > i - link_number) {
						return GameConstants.PDK_CT_ERROR;
					} else if (card_index.card_index[j] != 2 && card_index.card_index[j] != 0) {
						return GameConstants.PDK_CT_ERROR;
					}
				}
				return GameConstants.PDK_CT_DOUBLE_LINE;
			}
		}
		// 顺子
		for (int i = GameConstants.PDK_MAX_INDEX - 1; i >= 0; i--) {
			if (card_index.card_index[i] > 0 && i == GameConstants.PDK_MAX_INDEX - 1) {
				return GameConstants.PDK_CT_ERROR;
			}
			if (card_index.card_index[i] != 1 && card_index.card_index[i] != 0) {
				break;
			}
			if (card_index.card_index[i] == 1 && i >= cbCardCount - 1 && cbCardCount >= 5) {
				for (int j = i - 1; j >= 0; j--) {
					if (card_index.card_index[j] != 1 && j > i - cbCardCount) {
						return GameConstants.PDK_CT_ERROR;
					} else if (card_index.card_index[j] > 1) {
						return GameConstants.PDK_CT_ERROR;
					}
				}
				return GameConstants.PDK_CT_SINGLE_LINE;
			}
		}

		return GameConstants.PDK_CT_ERROR;
	}

	// 分析扑克
	public void AnalysebCardDataToIndex(int cbCardData[], int cbCardCount, tagAnalyseIndexResult AnalyseIndexResult) {
		int card_index[] = new int[GameConstants.PDK_MAX_INDEX];

		for (int i = 0; i < cbCardCount; i++) {
			int index = GetCardLogicValue(cbCardData[i]);
			AnalyseIndexResult.card_data[index - 3][AnalyseIndexResult.card_index[index - 3]] = cbCardData[i];
			AnalyseIndexResult.card_index[index - 3]++;

		}
	}

	public int switch_card_to_idnex(int card) {
		int index = GetCardLogicValue(card) - 3;
		return index;
	}

	public int GetCardLogicValue(int CardData) {
		if (CardData == 0) {
			return -1;
		}
		// 扑克属性
		int cbCardColor = get_card_color(CardData);
		int cbCardValue = get_card_value(CardData);

		// 转换数值
		if (cbCardColor == 0x04)
			return cbCardValue + 2;
		return (cbCardValue <= 2) ? (cbCardValue + 13) : cbCardValue;
	}

	public int get_card_value(int card) {
		if (card == 0) {
			return -1;
		}
		return card & GameConstants.LOGIC_MASK_VALUE;
	}

	// 获取花色
	public int get_card_color(int card) {
		if (card == 0) {
			return -1;
		}
		return (card & GameConstants.LOGIC_MASK_COLOR) >> 4;
	}

	public boolean has_rule(int cbRule) {
		return ruleMap.containsKey(cbRule);
	}

	public Map<Integer, Integer> getRuleMap() {
		return ruleMap;
	}

	public void setRuleMap(Map<Integer, Integer> ruleMap) {
		this.ruleMap = ruleMap;
	}
}
