package com.cai.game.hh.handler.wugangphz;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.Constants_WuGang;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.RandomUtil;
import com.cai.dictionary.SysParamDict;
import com.cai.game.hh.HHGameLogic.AnalyseItem;
import com.cai.game.hh.HHTable;

import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;

public class Table_WuGang extends HHTable {

	private static final long serialVersionUID = 6063229660054583199L;

	public Table_WuGang() {
		super();
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weaveCount, int seat_index, int provider_index, int cur_card,
			ChiHuRight chiHuRight, int card_type, int[] hu_xi_hh, boolean dispatch) {
		if (this._is_xiang_gong[seat_index] == true) {
			return GameConstants.WIK_NULL;
		}

		int cbChiHuKind = GameConstants.WIK_NULL;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_HH_INDEX];
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		if (cur_card != GameConstants.INVALID_VALUE) {
			int index = _logic.switch_to_card_index(cur_card);
			cbCardIndexTemp[index]++;
		}

		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();

		_hu_xi[seat_index] = 0;
		int hu_xi[] = new int[1];
		hu_xi[0] = 0;

		boolean bValue = _logic.analyse_card_phz(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index, cur_card, analyseItemArray,
				false, hu_xi, false);

		if (cur_card != 0) {
			for (int i = 0; i < weaveCount; i++) {
				if ((cur_card == weaveItems[i].center_card)
						&& ((weaveItems[i].weave_kind == GameConstants.WIK_PENG) || (weaveItems[i].weave_kind == GameConstants.WIK_WEI))) {

					int index = _logic.switch_to_card_index(cur_card);
					cbCardIndexTemp[index]--;
					int temp_index = analyseItemArray.size();

					boolean temp_bValue = _logic.analyse_card_phz(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index, cur_card,
							analyseItemArray, false, hu_xi, false);

					if (temp_index < analyseItemArray.size()) {
						bValue = temp_bValue;
						AnalyseItem analyseItem = new AnalyseItem();
						for (; temp_index < analyseItemArray.size(); temp_index++) {
							analyseItem = analyseItemArray.get(temp_index);
							hu_xi[0] = 0;
							for (int j = 0; j < 7; j++) {
								if ((cur_card == analyseItem.cbCenterCard[j]) && (analyseItem.cbWeaveKind[j] == GameConstants.WIK_PENG
										|| analyseItem.cbWeaveKind[j] == GameConstants.WIK_WEI))
									analyseItem.cbWeaveKind[j] = GameConstants.WIK_PAO;
								analyseItem.hu_xi[j] = _logic.get_analyse_hu_xi(analyseItem.cbWeaveKind[j], analyseItem.cbCenterCard[j]);
								hu_xi[0] += analyseItem.hu_xi[j];

							}

						}
					}
					break;
				}
			}

			WeaveItem sao_WeaveItem = new WeaveItem();
			int cur_index = _logic.switch_to_card_index(cur_card);
			if (cards_index[cur_index] == 3) {
				cbCardIndexTemp[cur_index] = 1;
				sao_WeaveItem.weave_kind = GameConstants.WIK_KAN;
				sao_WeaveItem.center_card = cur_card;
				sao_WeaveItem.hu_xi = _logic.get_weave_hu_xi(sao_WeaveItem);

				int sao_index = analyseItemArray.size();
				boolean temp_bValue = _logic.analyse_card_phz(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index, cur_card,
						analyseItemArray, false, hu_xi, false);
				if (sao_index < analyseItemArray.size()) {
					bValue = temp_bValue;
					for (; sao_index < analyseItemArray.size(); sao_index++) {
						AnalyseItem analyseItem = new AnalyseItem();
						analyseItem = analyseItemArray.get(sao_index);
						for (int j = 0; j < 7; j++) {
							if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL) {
								analyseItem.cbWeaveKind[j] = sao_WeaveItem.weave_kind;
								analyseItem.cbCenterCard[j] = sao_WeaveItem.center_card;
								analyseItem.hu_xi[j] = sao_WeaveItem.hu_xi;
								break;
							}
						}
					}
				}
			}
		}

		if (!bValue) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		AnalyseItem analyseItem = new AnalyseItem();
		int temp_hu_xi;
		int max_hu_xi = 0;
		int max_hu_index = 0;
		for (int i = 0; i < analyseItemArray.size(); i++) {
			temp_hu_xi = 0;
			analyseItem = analyseItemArray.get(i);

			for (int j = 0; j < 7; j++) {
				if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL)
					break;
				WeaveItem weave_items = new WeaveItem();
				weave_items.center_card = analyseItem.cbCenterCard[j];
				weave_items.weave_kind = analyseItem.cbWeaveKind[j];
				temp_hu_xi += _logic.get_weave_hu_xi(weave_items);
			}
			if (temp_hu_xi > max_hu_xi) {
				max_hu_index = i;
				max_hu_xi = temp_hu_xi;
			}
		}

		if (max_hu_xi < 10) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		hu_xi_hh[0] = max_hu_xi;

		analyseItem = analyseItemArray.get(max_hu_index);

		for (int j = 0; j < 7; j++) {
			if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL)
				break;
			_hu_weave_items[seat_index][j].weave_kind = analyseItem.cbWeaveKind[j];
			_hu_weave_items[seat_index][j].center_card = analyseItem.cbCenterCard[j];
			_hu_weave_items[seat_index][j].hu_xi = _logic.get_weave_hu_xi(_hu_weave_items[seat_index][j]);
			_hu_weave_count[seat_index] = j + 1;
		}

		if (analyseItem.curCardEye == true) {
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].weave_kind = GameConstants.WIK_DUI_ZI;
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].center_card = analyseItem.cbCardEye;
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].hu_xi = 0;
			_hu_weave_count[seat_index]++;
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;

		chiHuRight.opr_or(GameConstants.CHR_ZI_MO);

		return cbChiHuKind;
	}

	public int analyse_chi_hu_card_sixteen(int[] cards_index, WeaveItem[] weaveItems, int weaveCount, int seat_index, int provider_index,
			int cur_card, ChiHuRight chiHuRight, int card_type, int[] hu_xi_hh, boolean dispatch) {
		if (this._is_xiang_gong[seat_index] == true) {
			return GameConstants.WIK_NULL;
		}

		int cbChiHuKind = GameConstants.WIK_NULL;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_HH_INDEX];
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		if (cur_card != GameConstants.INVALID_VALUE) {
			int index = _logic.switch_to_card_index(cur_card);
			cbCardIndexTemp[index]++;
		}

		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();

		_hu_xi[seat_index] = 0;
		int hu_xi[] = new int[1];
		hu_xi[0] = 0;

		boolean bValue = _logic.analyse_card_phz_with_invisible_magic(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index, cur_card,
				analyseItemArray, false, hu_xi);

		if (cur_card != 0) {
			for (int i = 0; i < weaveCount; i++) {
				if ((cur_card == weaveItems[i].center_card)
						&& ((weaveItems[i].weave_kind == GameConstants.WIK_PENG) || (weaveItems[i].weave_kind == GameConstants.WIK_WEI))) {

					int index = _logic.switch_to_card_index(cur_card);
					cbCardIndexTemp[index]--;
					int temp_index = analyseItemArray.size();

					boolean temp_bValue = _logic.analyse_card_phz_with_invisible_magic(cbCardIndexTemp, weaveItems, weaveCount, seat_index,
							provider_index, cur_card, analyseItemArray, false, hu_xi);

					if (temp_index < analyseItemArray.size()) {
						bValue = temp_bValue;
						AnalyseItem analyseItem = new AnalyseItem();
						for (; temp_index < analyseItemArray.size(); temp_index++) {
							analyseItem = analyseItemArray.get(temp_index);
							hu_xi[0] = 0;
							for (int j = 0; j < 7; j++) {
								if ((cur_card == analyseItem.cbCenterCard[j]) && (analyseItem.cbWeaveKind[j] == GameConstants.WIK_PENG
										|| analyseItem.cbWeaveKind[j] == GameConstants.WIK_WEI))
									analyseItem.cbWeaveKind[j] = GameConstants.WIK_PAO;
								analyseItem.hu_xi[j] = _logic.get_analyse_hu_xi(analyseItem.cbWeaveKind[j], analyseItem.cbCenterCard[j]);
								hu_xi[0] += analyseItem.hu_xi[j];

							}

						}
					}
					break;
				}
			}

			WeaveItem sao_WeaveItem = new WeaveItem();
			int cur_index = _logic.switch_to_card_index(cur_card);
			if (cards_index[cur_index] == 3) {
				cbCardIndexTemp[cur_index] = 1;
				sao_WeaveItem.weave_kind = GameConstants.WIK_KAN;
				sao_WeaveItem.center_card = cur_card;
				sao_WeaveItem.hu_xi = _logic.get_weave_hu_xi(sao_WeaveItem);

				int sao_index = analyseItemArray.size();
				boolean temp_bValue = _logic.analyse_card_phz_with_invisible_magic(cbCardIndexTemp, weaveItems, weaveCount, seat_index,
						provider_index, cur_card, analyseItemArray, false, hu_xi);
				if (sao_index < analyseItemArray.size()) {
					bValue = temp_bValue;
					for (; sao_index < analyseItemArray.size(); sao_index++) {
						AnalyseItem analyseItem = new AnalyseItem();
						analyseItem = analyseItemArray.get(sao_index);
						for (int j = 0; j < 7; j++) {
							if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL) {
								analyseItem.cbWeaveKind[j] = sao_WeaveItem.weave_kind;
								analyseItem.cbCenterCard[j] = sao_WeaveItem.center_card;
								analyseItem.hu_xi[j] = sao_WeaveItem.hu_xi;
								break;
							}
						}
					}
				}
			}
		}

		if (!bValue) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		AnalyseItem analyseItem = new AnalyseItem();
		int temp_hu_xi;
		int max_hu_xi = 0;
		int max_hu_index = 0;
		for (int i = 0; i < analyseItemArray.size(); i++) {
			temp_hu_xi = 0;
			analyseItem = analyseItemArray.get(i);

			for (int j = 0; j < 7; j++) {
				if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL)
					break;
				WeaveItem weave_items = new WeaveItem();
				weave_items.center_card = analyseItem.cbCenterCard[j];
				weave_items.weave_kind = analyseItem.cbWeaveKind[j];
				temp_hu_xi += _logic.get_weave_hu_xi(weave_items);
			}
			if (temp_hu_xi > max_hu_xi) {
				max_hu_index = i;
				max_hu_xi = temp_hu_xi;
			}
		}

		if (max_hu_xi < 10) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		hu_xi_hh[0] = max_hu_xi;

		analyseItem = analyseItemArray.get(max_hu_index);

		boolean magic_located = false;

		for (int j = 0; j < 7; j++) {
			if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL)
				break;
			_hu_weave_items[seat_index][j].weave_kind = analyseItem.cbWeaveKind[j];
			_hu_weave_items[seat_index][j].center_card = analyseItem.cbCenterCard[j];
			_hu_weave_items[seat_index][j].hu_xi = _logic.get_weave_hu_xi(_hu_weave_items[seat_index][j]);
			_hu_weave_count[seat_index] = j + 1;

			if (j > weaveCount || (j == weaveCount && cur_card == 0)) {
				if (j == analyseItem.invisibleMagicCardKind) {
					int weave_card_count = 3;
					if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_TI_LONG || analyseItem.cbWeaveKind[j] == GameConstants.WIK_PAO)
						weave_card_count = 4;

					for (int x = 0; x < weave_card_count; x++) {
						if (analyseItem.cbCardData[j][x] == analyseItem.invisibleMagicCard) {
							if (magic_located == false) {
								magic_located = true;
								_hu_weave_items[seat_index][j].public_card = analyseItem.invisibleMagicCard;
								break;
							}
						}
					}
				}
			}
		}

		if (analyseItem.curCardEye == true) {
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].weave_kind = GameConstants.WIK_DUI_ZI;
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].center_card = analyseItem.cbCardEye;
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].hu_xi = 0;

			if (magic_located == false) {
				_hu_weave_items[seat_index][_hu_weave_count[seat_index]].public_card = analyseItem.invisibleMagicCard;
			}

			_hu_weave_count[seat_index]++;
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;

		chiHuRight.opr_or(GameConstants.CHR_ZI_MO);

		return cbChiHuKind;
	}

	public int analyse_chi_hu_card_sixteen_old(int[] cards_index, WeaveItem[] weaveItems, int weaveCount, int seat_index, int provider_index,
			int cur_card, ChiHuRight chiHuRight, int card_type, int[] hu_xi_hh, boolean dispatch) {
		if (this._is_xiang_gong[seat_index] == true) {
			return GameConstants.WIK_NULL;
		}

		int cbChiHuKind = GameConstants.WIK_NULL;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_HH_INDEX];

		boolean[] has_four_card = new boolean[GameConstants.MAX_HH_INDEX]; // 用来处理手牌里有四张一样的牌时的胡牌分析情况

		int real_max_hu_xi = 0;
		int real_magic_card = 0;
		AnalyseItem real_analyse_item = new AnalyseItem();

		// PerformanceTimer timer = new PerformanceTimer();

		for (int c = 0; c < GameConstants.MAX_HH_INDEX; c++) {
			for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
				cbCardIndexTemp[i] = cards_index[i];
			}

			if (cur_card != GameConstants.INVALID_VALUE) {
				int index = _logic.switch_to_card_index(cur_card);
				cbCardIndexTemp[index]++;
			}

			cbCardIndexTemp[c]++;

			int tmp_weave_count = weaveCount;
			WeaveItem[] tmp_weave_items = new WeaveItem[tmp_weave_count + 1];

			for (int i = 0; i < tmp_weave_count + 1; i++) {
				tmp_weave_items[i] = new WeaveItem();
			}

			for (int w = 0; w < tmp_weave_count; w++) {
				tmp_weave_items[w] = weaveItems[w];
			}

			// 把王牌加进来之后，如果牌刚好有4张，有两种情况可以胡：1. 当成提；2. 把坎落下来，最后一张牌当成其他牌；

			// 如果有提，暂时把提落下来
			if (cbCardIndexTemp[c] == 4) {
				if (has_four_card[c] == false) { // 当成提
					has_four_card[c] = true;
					tmp_weave_items[tmp_weave_count].public_card = 1;
					tmp_weave_items[tmp_weave_count].center_card = _logic.switch_to_card_data(c);
					if (cur_card == _logic.switch_to_card_data(c)) {
						tmp_weave_items[tmp_weave_count].weave_kind = GameConstants.WIK_PAO;
					} else {
						tmp_weave_items[tmp_weave_count].weave_kind = GameConstants.WIK_TI_LONG;
					}
					tmp_weave_items[tmp_weave_count].provide_player = seat_index;
					tmp_weave_items[tmp_weave_count].hu_xi = _logic.get_weave_hu_xi(tmp_weave_items[tmp_weave_count]);
					tmp_weave_count++;
					cbCardIndexTemp[c] = 0;
				} else if (has_four_card[c] == true) { // 当成坎
					tmp_weave_items[tmp_weave_count].public_card = 1;
					tmp_weave_items[tmp_weave_count].center_card = _logic.switch_to_card_data(c);
					tmp_weave_items[tmp_weave_count].weave_kind = GameConstants.WIK_KAN;
					tmp_weave_items[tmp_weave_count].provide_player = seat_index;
					tmp_weave_items[tmp_weave_count].hu_xi = _logic.get_weave_hu_xi(tmp_weave_items[tmp_weave_count]);
					tmp_weave_count++;
					cbCardIndexTemp[c] = 1;
					has_four_card[c] = false;
				}
			}

			List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();

			_hu_xi[seat_index] = 0;
			int hu_xi[] = new int[1];
			hu_xi[0] = 0;

			boolean bValue = _logic.analyse_card_phz(cbCardIndexTemp, tmp_weave_items, tmp_weave_count, seat_index, provider_index, cur_card,
					analyseItemArray, false, hu_xi, false);

			// System.out.println("第" + c + "轮耗时:" + timer.getStr());

			if (cur_card != 0) {
				for (int i = 0; i < tmp_weave_count; i++) {
					if ((cur_card == tmp_weave_items[i].center_card) && ((tmp_weave_items[i].weave_kind == GameConstants.WIK_PENG)
							|| (tmp_weave_items[i].weave_kind == GameConstants.WIK_WEI))) {

						int index = _logic.switch_to_card_index(cur_card);
						cbCardIndexTemp[index]--;
						int temp_index = analyseItemArray.size();

						boolean temp_bValue = _logic.analyse_card_phz(cbCardIndexTemp, tmp_weave_items, tmp_weave_count, seat_index, provider_index,
								cur_card, analyseItemArray, false, hu_xi, false);

						if (temp_index < analyseItemArray.size()) {
							bValue = temp_bValue;
							AnalyseItem analyseItem = new AnalyseItem();
							for (; temp_index < analyseItemArray.size(); temp_index++) {
								analyseItem = analyseItemArray.get(temp_index);
								hu_xi[0] = 0;
								for (int j = 0; j < 7; j++) {
									if ((cur_card == analyseItem.cbCenterCard[j]) && (analyseItem.cbWeaveKind[j] == GameConstants.WIK_PENG
											|| analyseItem.cbWeaveKind[j] == GameConstants.WIK_WEI))
										analyseItem.cbWeaveKind[j] = GameConstants.WIK_PAO;
									analyseItem.hu_xi[j] = _logic.get_analyse_hu_xi(analyseItem.cbWeaveKind[j], analyseItem.cbCenterCard[j]);
									hu_xi[0] += analyseItem.hu_xi[j];

								}

							}
						}
						break;
					}
				}

				WeaveItem sao_WeaveItem = new WeaveItem();
				int cur_index = _logic.switch_to_card_index(cur_card);
				if (cards_index[cur_index] == 3) {
					cbCardIndexTemp[cur_index] = 1;
					sao_WeaveItem.weave_kind = GameConstants.WIK_KAN;
					sao_WeaveItem.center_card = cur_card;
					sao_WeaveItem.hu_xi = _logic.get_weave_hu_xi(sao_WeaveItem);

					int sao_index = analyseItemArray.size();
					boolean temp_bValue = _logic.analyse_card_phz(cbCardIndexTemp, tmp_weave_items, tmp_weave_count, seat_index, provider_index,
							cur_card, analyseItemArray, false, hu_xi, false);
					if (sao_index < analyseItemArray.size()) {
						bValue = temp_bValue;
						for (; sao_index < analyseItemArray.size(); sao_index++) {
							AnalyseItem analyseItem = new AnalyseItem();
							analyseItem = analyseItemArray.get(sao_index);
							for (int j = 0; j < 7; j++) {
								if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL) {
									analyseItem.cbWeaveKind[j] = sao_WeaveItem.weave_kind;
									analyseItem.cbCenterCard[j] = sao_WeaveItem.center_card;
									analyseItem.hu_xi[j] = sao_WeaveItem.hu_xi;
									break;
								}
							}
						}
					}
				}
			}

			if (!bValue) {
				if (has_four_card[c] == true) {
					c--;
				}
				continue;
			}

			AnalyseItem analyseItem = new AnalyseItem();
			int temp_hu_xi;
			int max_hu_xi = 0;
			int max_hu_index = 0;
			for (int i = 0; i < analyseItemArray.size(); i++) {
				temp_hu_xi = 0;
				analyseItem = analyseItemArray.get(i);

				for (int j = 0; j < 7; j++) {
					if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL)
						break;
					WeaveItem weave_items = new WeaveItem();
					weave_items.center_card = analyseItem.cbCenterCard[j];
					weave_items.weave_kind = analyseItem.cbWeaveKind[j];
					temp_hu_xi += _logic.get_weave_hu_xi(weave_items);
				}
				if (temp_hu_xi > max_hu_xi) {
					max_hu_index = i;
					max_hu_xi = temp_hu_xi;
				}
			}

			if (max_hu_xi < 10) {
				if (has_four_card[c] == true) {
					c--;
				}
				continue;
			}

			if (max_hu_xi > real_max_hu_xi) {
				real_max_hu_xi = max_hu_xi;
				real_magic_card = _logic.switch_to_card_data(c);
				real_analyse_item = analyseItemArray.get(max_hu_index);
			}

			if (has_four_card[c] == true) {
				c--;
			}
		}

		if (real_max_hu_xi >= 10) {
			boolean magic_located = false;
			hu_xi_hh[0] = real_max_hu_xi;

			for (int j = 0; j < 7; j++) {
				if (real_analyse_item.cbWeaveKind[j] == GameConstants.WIK_NULL)
					break;
				_hu_weave_items[seat_index][j].weave_kind = real_analyse_item.cbWeaveKind[j];
				_hu_weave_items[seat_index][j].center_card = real_analyse_item.cbCenterCard[j];
				_hu_weave_items[seat_index][j].hu_xi = _logic.get_weave_hu_xi(_hu_weave_items[seat_index][j]);
				_hu_weave_count[seat_index] = j + 1;

				for (int x = 0; x < 3; x++) {
					if (real_analyse_item.cbCardData[j][x] == real_magic_card) {
						if (magic_located == false) {
							magic_located = true;
							_hu_weave_items[seat_index][j].public_card = real_magic_card;
							break;
						}
					}
				}
			}

			if (real_analyse_item.curCardEye == true) {
				_hu_weave_items[seat_index][_hu_weave_count[seat_index]].weave_kind = GameConstants.WIK_DUI_ZI;
				_hu_weave_items[seat_index][_hu_weave_count[seat_index]].center_card = real_analyse_item.cbCardEye;
				_hu_weave_items[seat_index][_hu_weave_count[seat_index]].hu_xi = 0;

				if (magic_located == false) {
					_hu_weave_items[seat_index][_hu_weave_count[seat_index]].public_card = real_magic_card;
				}

				_hu_weave_count[seat_index]++;
			}

			cbChiHuKind = GameConstants.WIK_CHI_HU;

			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);

			return cbChiHuKind;
		} else {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}
	}

	public int analyse_chi_hu_card_sixteen_new(int[] cards_index, WeaveItem[] weaveItems, int weaveCount, int seat_index, int provider_index,
			int cur_card, ChiHuRight chiHuRight, int card_type, int[] hu_xi_hh, boolean dispatch) {
		if (this._is_xiang_gong[seat_index] == true) {
			return GameConstants.WIK_NULL;
		}

		int cbChiHuKind = GameConstants.WIK_NULL;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_HH_INDEX];
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		if (cur_card != GameConstants.INVALID_VALUE) {
			int index = _logic.switch_to_card_index(cur_card);
			cbCardIndexTemp[index]++;
		}

		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();

		_hu_xi[seat_index] = 0;
		int hu_xi[] = new int[1];
		hu_xi[0] = 0;

		boolean bValue = _logic.analyse_card_phz_with_invisible_magic(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index, cur_card,
				analyseItemArray, false, hu_xi);

		if (cur_card != 0) {
			for (int i = 0; i < weaveCount; i++) {
				if ((cur_card == weaveItems[i].center_card)
						&& ((weaveItems[i].weave_kind == GameConstants.WIK_PENG) || (weaveItems[i].weave_kind == GameConstants.WIK_WEI))) {

					int index = _logic.switch_to_card_index(cur_card);
					cbCardIndexTemp[index]--;
					int temp_index = analyseItemArray.size();

					boolean temp_bValue = _logic.analyse_card_phz_with_invisible_magic(cbCardIndexTemp, weaveItems, weaveCount, seat_index,
							provider_index, cur_card, analyseItemArray, false, hu_xi);

					if (temp_index < analyseItemArray.size()) {
						bValue = temp_bValue;
						AnalyseItem analyseItem = new AnalyseItem();
						for (; temp_index < analyseItemArray.size(); temp_index++) {
							analyseItem = analyseItemArray.get(temp_index);
							hu_xi[0] = 0;
							for (int j = 0; j < 7; j++) {
								if ((cur_card == analyseItem.cbCenterCard[j]) && (analyseItem.cbWeaveKind[j] == GameConstants.WIK_PENG
										|| analyseItem.cbWeaveKind[j] == GameConstants.WIK_WEI))
									analyseItem.cbWeaveKind[j] = GameConstants.WIK_PAO;
								analyseItem.hu_xi[j] = _logic.get_analyse_hu_xi(analyseItem.cbWeaveKind[j], analyseItem.cbCenterCard[j]);
								hu_xi[0] += analyseItem.hu_xi[j];

							}

						}
					}
					break;
				}
			}

			WeaveItem sao_WeaveItem = new WeaveItem();
			int cur_index = _logic.switch_to_card_index(cur_card);
			if (cards_index[cur_index] == 3) {
				cbCardIndexTemp[cur_index] = 1;
				sao_WeaveItem.weave_kind = GameConstants.WIK_KAN;
				sao_WeaveItem.center_card = cur_card;
				sao_WeaveItem.hu_xi = _logic.get_weave_hu_xi(sao_WeaveItem);

				int sao_index = analyseItemArray.size();
				boolean temp_bValue = _logic.analyse_card_phz_with_invisible_magic(cbCardIndexTemp, weaveItems, weaveCount, seat_index,
						provider_index, cur_card, analyseItemArray, false, hu_xi);
				if (sao_index < analyseItemArray.size()) {
					bValue = temp_bValue;
					for (; sao_index < analyseItemArray.size(); sao_index++) {
						AnalyseItem analyseItem = new AnalyseItem();
						analyseItem = analyseItemArray.get(sao_index);
						for (int j = 0; j < 7; j++) {
							if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL) {
								analyseItem.cbWeaveKind[j] = sao_WeaveItem.weave_kind;
								analyseItem.cbCenterCard[j] = sao_WeaveItem.center_card;
								analyseItem.hu_xi[j] = sao_WeaveItem.hu_xi;
								break;
							}
						}
					}
				}
			}
		}

		if (!bValue) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		AnalyseItem analyseItem = new AnalyseItem();
		int temp_hu_xi;
		int max_hu_xi = 0;
		int max_hu_index = 0;
		for (int i = 0; i < analyseItemArray.size(); i++) {
			temp_hu_xi = 0;
			analyseItem = analyseItemArray.get(i);

			for (int j = 0; j < 7; j++) {
				if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL)
					break;
				WeaveItem weave_items = new WeaveItem();
				weave_items.center_card = analyseItem.cbCenterCard[j];
				weave_items.weave_kind = analyseItem.cbWeaveKind[j];
				temp_hu_xi += _logic.get_weave_hu_xi(weave_items);
			}
			if (temp_hu_xi > max_hu_xi) {
				max_hu_index = i;
				max_hu_xi = temp_hu_xi;
			}
		}

		if (max_hu_xi < 10) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		hu_xi_hh[0] = max_hu_xi;

		analyseItem = analyseItemArray.get(max_hu_index);

		boolean magic_located = false;

		for (int j = 0; j < 7; j++) {
			if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL)
				break;
			_hu_weave_items[seat_index][j].weave_kind = analyseItem.cbWeaveKind[j];
			_hu_weave_items[seat_index][j].center_card = analyseItem.cbCenterCard[j];
			_hu_weave_items[seat_index][j].hu_xi = _logic.get_weave_hu_xi(_hu_weave_items[seat_index][j]);
			_hu_weave_count[seat_index] = j + 1;

			for (int x = 0; x < 3; x++) {
				if (analyseItem.cbCardData[j][x] == analyseItem.invisibleMagicCard) {
					if (magic_located == false) {
						magic_located = true;
						_hu_weave_items[seat_index][j].public_card = analyseItem.invisibleMagicCard;
						break;
					}
				}
			}
		}

		if (analyseItem.curCardEye == true) {
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].weave_kind = GameConstants.WIK_DUI_ZI;
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].center_card = analyseItem.cbCardEye;
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].hu_xi = 0;

			if (magic_located == false) {
				_hu_weave_items[seat_index][_hu_weave_count[seat_index]].public_card = analyseItem.invisibleMagicCard;
			}

			_hu_weave_count[seat_index]++;
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;

		chiHuRight.opr_or(GameConstants.CHR_ZI_MO);

		return cbChiHuKind;
	}

	@Override
	public int get_real_card(int card) {
		if (card > GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_SHOOT && card < GameConstants.CARD_ESPECIAL_TYPE_CAN_SHOOT) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_SHOOT;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_CAN_SHOOT) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_CAN_SHOOT;
		}

		return card;
	}

	protected int get_banker_pre_seat(int banker_seat) {
		int count = 0;
		int seat = banker_seat;
		do {
			count++;
			seat = (4 + seat - 1) % 4;
		} while (get_players()[seat] == null && count <= 5);
		return seat;
	}

	protected int get_banker_next_seat(int banker_seat) {
		int count = 0;
		int seat = banker_seat;
		do {
			count++;
			seat = (seat + 1) % 4;
		} while (get_players()[seat] == null && count <= 5);
		return seat;
	}

	@Override
	public int get_hh_ting_card_twenty(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index, int provate_index) {
		int cbCardIndexTemp[] = new int[GameConstants.MAX_HH_INDEX];

		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int mj_count = GameConstants.MAX_HH_INDEX;

		for (int i = 0; i < mj_count; i++) {
			if (this._logic.is_magic_index(i))
				continue;
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			int hu_xi[] = new int[1];
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, seat_index, provate_index, cbCurrentCard,
					chr, GameConstants.HU_CARD_TYPE_ZIMO, hu_xi, true)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}

		if (count >= mj_count) {
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	public int get_ting_card_sixteen(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index, int provate_index) {
		int cbCardIndexTemp[] = new int[GameConstants.MAX_HH_INDEX];
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int mj_count = GameConstants.MAX_HH_INDEX;

		for (int i = 0; i < mj_count; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			int hu_xi[] = new int[1];
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_sixteen(cbCardIndexTemp, weaveItem, cbWeaveCount, seat_index, provate_index,
					cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO, hu_xi, true)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}

		if (count >= mj_count) {
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	@Override
	public void on_init_table(int game_type_index, int game_rule_index, int game_round) {
		super.on_init_table(game_type_index, game_rule_index, game_round);

		_handler_chi_peng = new HandlerChiPeng_WuGang();
		_handler_dispath_card = new HandlerDispatchCard_WuGang();
		_handler_out_card_operate = new HandlerOutCard_WuGang();
		_handler_gang = new HandlerTiPaoWei_WuGang();
		_handler_dispath_firstcards = new HandlerDispatchFirstCard_WuGang();
	}

	public boolean is_card_has_wei(int card) {
		boolean bTmp = false;

		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			if (this.cards_has_wei[i] != 0) {
				if (i == this._logic.switch_to_card_index(card)) {
					bTmp = true;
					break;
				}
			}
		}

		return bTmp;
	}

	public boolean is_ting_state(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		int cbCardIndexTemp[] = new int[GameConstants.MAX_HH_INDEX];
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();
		chr.set_empty();
		int hu_xi_chi[] = new int[1];
		hu_xi_chi[0] = 0;
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			int cbCurrentCard = _logic.switch_to_card_data(i);

			if (has_rule(Constants_WuGang.GAME_RULE_PLAYER_3)) {
				if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, seat_index, seat_index, cbCurrentCard,
						chr, GameConstants.HU_CARD_TYPE_ZIMO, hu_xi_chi, true))
					return true;
			} else if (has_rule(Constants_WuGang.GAME_RULE_PLAYER_4)) {
				if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_sixteen(cbCardIndexTemp, weaveItem, cbWeaveCount, seat_index, seat_index,
						cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO, hu_xi_chi, true))
					return true;
			}
		}

		return false;
	}

	@Override
	protected boolean on_handler_game_start() {
		_game_status = GameConstants.GS_MJ_PLAY;

		reset_init_data();

		this.progress_banker_select();

		GRR._banker_player = _cur_banker;
		_current_player = GRR._banker_player;

		_repertory_card = new int[GameConstants.CARD_COUNT_HH_YX];
		shuffle(_repertory_card, GameConstants.CARD_DATA_HH_YX);

		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();

		this.game_start_phz_wugang();

		return true;
	}

	@Override
	public void shuffle(int repertory_card[], int mj_cards[]) {
		_all_card_len = repertory_card.length;
		GRR._left_card_count = _all_card_len;

		// _logic.random_card_data(repertory_card, mj_cards);

		int xi_pai_count = 0;
		int rand = RandomUtil.generateRandomNumber(3, 6);
		while (xi_pai_count < 6 && xi_pai_count < rand) {
			if (xi_pai_count == 0)
				_logic.random_card_data(repertory_card, mj_cards);
			else
				_logic.random_card_data(repertory_card, repertory_card);
			xi_pai_count++;
		}

		int send_count;
		int have_send_count = 0;

		int count = this.getTablePlayerNumber();

		for (int i = 0; i < count; i++) {
			/**
			 * if (has_rule(Constants_WuGang.GAME_RULE_PLAYER_4)) { if (i ==
			 * GRR._banker_player) send_count = GameConstants.MAX_FPHZ_COUNT +
			 * 2; else send_count = GameConstants.MAX_FPHZ_COUNT + 1; } else {
			 * if (i == GRR._banker_player) send_count =
			 * GameConstants.MAX_HH_COUNT; else send_count =
			 * GameConstants.MAX_HH_COUNT - 1; }
			 **/
			if (has_rule(Constants_WuGang.GAME_RULE_PLAYER_4)) {
				send_count = GameConstants.MAX_FPHZ_COUNT + 1;
			} else {
				send_count = GameConstants.MAX_HH_COUNT - 1;
			}

			GRR._left_card_count -= send_count;
			_logic.switch_to_cards_index(repertory_card, have_send_count, send_count, GRR._cards_index[i]);
			have_send_count += send_count;
		}

		_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));
	}

	protected boolean game_start_phz_wugang() {
		_logic.clean_magic_cards();

		int playerCount = getPlayerCount();

		this.GRR._banker_player = this._current_player = this._cur_banker;

		this._game_status = GameConstants.GS_MJ_PLAY;

		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(this.GRR._banker_player);
		gameStartResponse.setCurrentPlayer(this._current_player);
		gameStartResponse.setLeftCardCount(this.GRR._left_card_count);

		int hand_cards[][] = new int[playerCount][GameConstants.MAX_HH_COUNT];
		for (int i = 0; i < playerCount; i++) {
			int hand_card_count = this._logic.switch_to_cards_data(this.GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		int ti_card_count[] = new int[this.getTablePlayerNumber()];
		int ti_card_index[][] = new int[this.getTablePlayerNumber()][5];

		for (int i = 0; i < playerCount; i++) {
			ti_card_count[i] = this._logic.get_action_ti_Card(this.GRR._cards_index[i], ti_card_index[i]);
		}

		int FlashTime = 4000;
		int standTime = 1000;

		for (int i = 0; i < playerCount; i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.MAX_HH_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			this.GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.load_room_info_data(roomResponse);
			this.load_common_status(roomResponse);

			if (this._cur_round == 1) {
				this.load_player_info_data(roomResponse);
			}
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setCurrentPlayer(this._current_player == GameConstants.INVALID_SEAT ? this._resume_player : this._current_player);
			roomResponse.setLeftCardCount(this.GRR._left_card_count);
			roomResponse.setGameStatus(this._game_status);
			roomResponse.setLeftCardCount(this.GRR._left_card_count);

			int gameId = this.getGame_id() == 0 ? 8 : this.getGame_id();
			SysParamModel sysParamModel1104 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId).get(1104);
			if (sysParamModel1104 != null && sysParamModel1104.getVal1() > 0 && sysParamModel1104.getVal1() < 10000) {
				FlashTime = sysParamModel1104.getVal1();
			}
			if (sysParamModel1104 != null && sysParamModel1104.getVal2() > 0 && sysParamModel1104.getVal2() < 10000) {
				standTime = sysParamModel1104.getVal2();
			}
			roomResponse.setFlashTime(FlashTime);
			roomResponse.setStandTime(standTime);
			this.send_response_to_player(i, roomResponse);
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		this.load_room_info_data(roomResponse);
		this.load_common_status(roomResponse);
		this.load_player_info_data(roomResponse);
		for (int i = 0; i < playerCount; i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			for (int j = 0; j < GameConstants.MAX_HH_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}

		roomResponse.setGameStart(gameStartResponse);

		roomResponse.setLeftCardCount(this.GRR._left_card_count);
		this.GRR.add_room_response(roomResponse);

		this._handler = this._handler_dispath_firstcards;
		this.exe_dispatch_first_card(_current_player, GameConstants.WIK_NULL, 0);

		return true;
	}

	@Override
	public void process_chi_hu_player_score_phz(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;
		GRR._win_order[seat_index] = 1;
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		countCardType(chr, seat_index);

		int all_hu_xi = 0;
		for (int i = 0; i < this._hu_weave_count[seat_index]; i++) {
			all_hu_xi += this._hu_weave_items[seat_index][i].hu_xi;
		}

		this._hu_xi[seat_index] = all_hu_xi;

		int calculate_score = 1 + (all_hu_xi - 10) / 5;

		int wFanShu = 1;

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (i == seat_index) {
				continue;
			}
			GRR._lost_fan_shu[i][seat_index] = wFanShu;
		}

		int lChiHuScore = wFanShu * calculate_score;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (i == seat_index) {
				continue;
			}
			GRR._game_score[i] -= lChiHuScore;
			GRR._game_score[seat_index] += lChiHuScore;
		}

		GRR._provider[seat_index] = provide_index;
	}

	public void runnable_deal_with_first_card(int seat_index) {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_playerStatus[i].clean_action();
			_playerStatus[i].clean_status();
		}

		_playerStatus[seat_index].chi_hu_round_valid();

		_current_player = seat_index;

		_send_card_count++;
		int send_card_data = _repertory_card[_all_card_len - GRR._left_card_count];
		GRR._left_card_count--;

		_send_card_data = send_card_data;
		_provide_card = _send_card_data;

		GRR._cards_index[seat_index][_logic.switch_to_card_index(send_card_data)]++;

		int cards[] = new int[GameConstants.MAX_HH_COUNT];
		int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], cards);
		operate_player_cards(seat_index, hand_card_count, cards, GRR._weave_count[seat_index], GRR._weave_items[seat_index]);

		// 处理起手的召牌，从庄家开始处理
		for (int p = 0; p < this.getTablePlayerNumber(); p++) {
			int k = (this.get_banker_next_seat(GRR._banker_player) + (p - 1) + this.getTablePlayerNumber()) % this.getTablePlayerNumber();

			this.deal_with_zhao(this, k, GRR._cards_index[k]);
		}

		if (has_rule(Constants_WuGang.GAME_RULE_PLAYER_3)) {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				this._playerStatus[i]._hu_card_count = get_hh_ting_card_twenty(_playerStatus[i]._hu_cards, GRR._cards_index[i], GRR._weave_items[i],
						GRR._weave_count[i], i, i);
				if (this._playerStatus[i]._hu_card_count > 0) {
					this.operate_chi_hu_cards(i, this._playerStatus[i]._hu_card_count, this._playerStatus[i]._hu_cards);
				}
			}
		}

		// 分析当前庄家能不能胡
		int bHupai = 0;
		int hu_xi[] = new int[1];
		int card_type = GameConstants.HU_CARD_TYPE_FAN_PAI;
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int action_hu = 0;
		if (this.has_rule(Constants_WuGang.GAME_RULE_PLAYER_4)) {
			action_hu = this.analyse_chi_hu_card_sixteen(GRR._cards_index[seat_index], GRR._weave_items[seat_index],
					this.GRR._weave_count[seat_index], seat_index, seat_index, 0, chr, card_type, hu_xi, true);
		} else {
			action_hu = this.analyse_chi_hu_card(GRR._cards_index[seat_index], GRR._weave_items[seat_index], GRR._weave_count[seat_index], seat_index,
					seat_index, 0, chr, card_type, hu_xi, true);
		}

		if (action_hu != GameConstants.WIK_NULL) {
			PlayerStatus playerStatus = _playerStatus[seat_index];
			playerStatus.add_action(GameConstants.WIK_CHI_HU);
			playerStatus.add_chi_hu(_send_card_data, seat_index);

			playerStatus.add_action(GameConstants.WIK_NULL);
			playerStatus.add_pass(_send_card_data, seat_index);
			this.operate_player_action(seat_index, false);

			bHupai = 1;
		} else {
			chr.set_empty();
		}

		if (bHupai == 0) {
			_playerStatus[seat_index].set_status(GameConstants.Player_Status_OUT_CARD);
			this.operate_player_status();
		}

		return;
	}

	public void deal_with_zhao(Table_WuGang table, int seat_index, int[] cards_index) {
		int an_long_index[] = new int[5];
		int an_long_count = 0;

		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			if (cards_index[i] == 4) {
				an_long_index[an_long_count++] = i;
			}
		}

		for (int i = 0; i < an_long_count; i++) {
			int cbWeaveIndex = table.GRR._weave_count[seat_index];
			table.GRR._weave_items[seat_index][cbWeaveIndex].public_card = 1;
			table.GRR._weave_items[seat_index][cbWeaveIndex].center_card = table._logic.switch_to_card_data(an_long_index[i]);
			table.GRR._weave_items[seat_index][cbWeaveIndex].weave_kind = GameConstants.WIK_TI_LONG;
			table.GRR._weave_items[seat_index][cbWeaveIndex].provide_player = seat_index;
			table.GRR._weave_items[seat_index][cbWeaveIndex].hu_xi = table._logic.get_weave_hu_xi(table.GRR._weave_items[seat_index][cbWeaveIndex]);
			table.GRR._weave_count[seat_index]++;
			table._long_count[seat_index]++;

			table.GRR._cards_index[seat_index][an_long_index[i]] = 0;

			table.GRR._card_count[seat_index] = table._logic.get_card_count_by_index(table.GRR._cards_index[seat_index]);
		}

		if (an_long_count == 0) { // 手牌没召了
			return;
		} else { // 手牌有召
			int action = GameConstants.WIK_TI_LONG;
			table.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { action }, 1, GameConstants.INVALID_SEAT);

			int cards[] = new int[GameConstants.MAX_HH_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);
			table.operate_player_cards(seat_index, hand_card_count, cards, table.GRR._weave_count[seat_index], table.GRR._weave_items[seat_index]);

			if (has_first_qi_shou_ti[seat_index] == false) {
				has_first_qi_shou_ti[seat_index] = true;
				if (an_long_count == 1) {
					return;
				} else {
					this.dispatch_card(table, seat_index, an_long_count - 1);
				}
			} else if (has_first_qi_shou_ti[seat_index] == true) {
				this.dispatch_card(table, seat_index, an_long_count);
			}

			this.deal_with_zhao(table, seat_index, table.GRR._cards_index[seat_index]);
		}
	}

	public void dispatch_card(Table_WuGang table, int seat_index, int count) {
		for (int i = 0; i < count; i++) {
			table._send_card_count++;
			int send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
			table.GRR._left_card_count--;

			if (seat_index == table.GRR._banker_player) {
				_send_card_data = send_card_data;
				table._send_card_data = _send_card_data;
				table._provide_card = _send_card_data;
			}

			table.GRR._cards_index[seat_index][table._logic.switch_to_card_index(send_card_data)]++;

			int cards[] = new int[GameConstants.MAX_HH_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);
			table.operate_player_cards(seat_index, hand_card_count, cards, table.GRR._weave_count[seat_index], table.GRR._weave_items[seat_index]);
		}
	}

	@Override
	public void set_result_describe(int seat_index) {
		for (int player = 0; player < this.getTablePlayerNumber(); player++) {
			String desStr = "";

			if (GRR._chi_hu_rights[player].is_valid()) {
				desStr = "胡";
			}

			GRR._result_des[player] = desStr;
		}
	}

	@Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x03, 0x03, 0x03, 0x04, 0x04, 0x04, 0x05, 0x05, 0x05, 0x06, 0x08,
				0x14, 0x15, 0x07 };
		int[] cards_of_player1 = new int[] { 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x03, 0x03, 0x03, 0x04, 0x04, 0x04, 0x05, 0x05, 0x05, 0x06, 0x08,
				0x14, 0x15, 0x07 };
		int[] cards_of_player2 = new int[] { 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x03, 0x03, 0x03, 0x04, 0x04, 0x04, 0x05, 0x05, 0x05, 0x06, 0x08,
				0x14, 0x15, 0x07 };
		int[] cards_of_player3 = new int[] { 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x03, 0x03, 0x03, 0x04, 0x04, 0x04, 0x05, 0x05, 0x05, 0x06, 0x08,
				0x14, 0x15, 0x07 };

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_HH_COUNT; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}

		if (this.getTablePlayerNumber() == 3) {
			for (int j = 0; j < GameConstants.MAX_HH_COUNT - 1; j++) {
				GRR._cards_index[0][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[1][_logic.switch_to_card_index(cards_of_player1[j])] += 1;
				GRR._cards_index[2][_logic.switch_to_card_index(cards_of_player2[j])] += 1;
			}
		} else {
			for (int j = 0; j < GameConstants.MAX_FPHZ_COUNT + 1; j++) {
				GRR._cards_index[0][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[1][_logic.switch_to_card_index(cards_of_player1[j])] += 1;
				GRR._cards_index[2][_logic.switch_to_card_index(cards_of_player2[j])] += 1;
				GRR._cards_index[3][_logic.switch_to_card_index(cards_of_player3[j])] += 1;
			}
		}

		if (BACK_DEBUG_CARDS_MODE) {
			if (debug_my_cards != null) {
				if (has_rule(Constants_WuGang.GAME_RULE_PLAYER_3)) {
					if (debug_my_cards.length > 20) {
						int[] temps = new int[debug_my_cards.length];
						System.arraycopy(debug_my_cards, 0, temps, 0, temps.length);
						testRealyCard(temps);
						debug_my_cards = null;
					} else {
						int[] temps = new int[debug_my_cards.length];
						System.arraycopy(debug_my_cards, 0, temps, 0, temps.length);
						testSameCard(temps);
						debug_my_cards = null;
					}
				} else if (has_rule(Constants_WuGang.GAME_RULE_PLAYER_4)) {
					if (debug_my_cards.length > 16) {
						int[] temps = new int[debug_my_cards.length];
						System.arraycopy(debug_my_cards, 0, temps, 0, temps.length);
						testRealyCard(temps);
						debug_my_cards = null;
					} else {
						int[] temps = new int[debug_my_cards.length];
						System.arraycopy(debug_my_cards, 0, temps, 0, temps.length);
						testSameCard(temps);
						debug_my_cards = null;
					}
				}
			}
		}
	}

	@Override
	public void testRealyCard(int[] realyCards) {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_HH_COUNT; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}
		_repertory_card = realyCards;
		_all_card_len = _repertory_card.length;
		GRR._left_card_count = _all_card_len;

		int send_count;
		int have_send_count = 0;

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (has_rule(Constants_WuGang.GAME_RULE_PLAYER_4)) {
				send_count = GameConstants.MAX_FPHZ_COUNT + 1;
			} else {
				send_count = GameConstants.MAX_HH_COUNT - 1;
			}

			GRR._left_card_count -= send_count;
			_logic.switch_to_cards_index(_repertory_card, have_send_count, send_count, GRR._cards_index[i]);
			have_send_count += send_count;
		}
		DEBUG_CARDS_MODE = false;
		BACK_DEBUG_CARDS_MODE = false;
	}

	@Override
	public void testSameCard(int[] cards) {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_HH_COUNT; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			int send_count = 0;
			if (has_rule(Constants_WuGang.GAME_RULE_PLAYER_4)) {
				send_count = GameConstants.MAX_FPHZ_COUNT + 1;
			} else {
				send_count = GameConstants.MAX_HH_COUNT - 1;
			}

			if (send_count > cards.length) {
				send_count = cards.length;
			}

			for (int j = 0; j < send_count; j++) {
				GRR._cards_index[i][_logic.switch_to_card_index(cards[j])] += 1;
			}
		}
		DEBUG_CARDS_MODE = false;
		BACK_DEBUG_CARDS_MODE = false;
	}
}
