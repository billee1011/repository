package com.cai.game.bullfight.yiyang;

import java.util.Arrays;
import java.util.Map;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.game.bullfight.Constants_YiYang;
import com.cai.common.util.RandomUtil;

public class YiYangBullFightGameLogic {
    public int get_card_value(int card) {
        int card_value = card & GameConstants.LOGIC_MASK_VALUE;
        return card_value > 10 ? 10 : card_value;
    }

    public int get_real_card_value(int card) {
        return card & GameConstants.LOGIC_MASK_VALUE;
    }

    public int get_logic_card_value(int card) {
        if ((card & GameConstants.LOGIC_MASK_VALUE) == 1)
            return 14;
        return card & GameConstants.LOGIC_MASK_VALUE;
    }

    public int get_card_color(int card) {
        return (card & GameConstants.LOGIC_MASK_COLOR) >> 4;
    }

    public void random_card_data(int return_cards[], final int mj_cards[]) {
        int card_count = return_cards.length;
        int card_data[] = new int[card_count];
        for (int i = 0; i < card_count; i++) {
            card_data[i] = mj_cards[i];
        }
        random_cards(card_data, return_cards, card_count);
    }

    private static void random_cards(int card_data[], int return_cards[], int card_count) {
        int bRandCount = 0, bPosition = 0;
        do {
            bPosition = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % (card_count - bRandCount));
            return_cards[bRandCount++] = card_data[bPosition];
            card_data[bPosition] = card_data[card_count - bRandCount];
        } while (bRandCount < card_count);

    }

    public int getRuleValue(Map<Integer, Integer> ruleMaps, int game_rule) {
        if (!ruleMaps.containsKey(game_rule)) {
            return 0;
        }
        return ruleMaps.get(game_rule);
    }

    public void ox_sort_card_list(int card_date[], int card_count) {
        int logic_value[] = new int[GameConstants.OX_MAX_CARD_COUNT];
        for (int i = 0; i < card_count; i++)
            logic_value[i] = get_real_card_value(card_date[i]);

        boolean sorted = true;
        int temp_date, last = card_count - 1;
        do {
            sorted = true;
            for (int i = 0; i < last; i++) {
                if ((logic_value[i] < logic_value[i + 1])
                        || ((logic_value[i] == logic_value[i + 1]) && (card_date[i] < card_date[i + 1]))) {
                    temp_date = card_date[i];
                    card_date[i] = card_date[i + 1];
                    card_date[i + 1] = temp_date;
                    temp_date = logic_value[i];
                    logic_value[i] = logic_value[i + 1];
                    logic_value[i + 1] = temp_date;
                    sorted = false;
                }
            }
            last--;
        } while (sorted == false);

        return;
    }

    public int get_card_type(int card_date[], int card_count, Map<Integer, Integer> ruleMaps) {
        if (card_count != GameConstants.OX_MAX_CARD_COUNT)
            return 0;

        int temp_card_data[] = new int[GameConstants.OX_MAX_CARD_COUNT];
        int card_real_value_count[] = new int[14];
        Arrays.fill(card_real_value_count, 0);

        for (int i = 0; i < GameConstants.OX_MAX_CARD_COUNT; i++) {
            temp_card_data[i] = card_date[i];
            card_real_value_count[get_real_card_value(temp_card_data[i])]++;
        }

        ox_sort_card_list(temp_card_data, card_count);

        boolean same_color = true;
        boolean line_card = true;
        @SuppressWarnings("unused")
        boolean special = false;

        int first_color = get_card_color(temp_card_data[0]);
        int first_value = get_real_card_value(temp_card_data[0]);

        if (first_value == 13) {
            if (get_real_card_value(temp_card_data[GameConstants.OX_MAX_CARD_COUNT - 1]) == 1) {
                temp_card_data[GameConstants.OX_MAX_CARD_COUNT
                        - 1] = get_card_color(temp_card_data[GameConstants.OX_MAX_CARD_COUNT - 1]) * 16 + 9;
                special = true;
            }
        }

        for (int i = 1; i < GameConstants.OX_MAX_CARD_COUNT; i++) {
            if (get_card_color(temp_card_data[i]) != first_color)
                same_color = false;
            if (get_real_card_value(temp_card_data[i]) != first_value - i)
                line_card = false;
            if (same_color == false && line_card == false)
                break;
        }

        if (getRuleValue(ruleMaps, Constants_YiYang.GAME_RULE_TONG_HUA_SHUN_NIU_SHI_BEI) == 1) {
            // 同花顺 10倍
            if ((same_color == true) && (line_card == true))
                return GameConstants.OX_TONG_HUA_XHUN_VALUE;
        }

        @SuppressWarnings("unused")
        int king_count = 0, ten_count = 0;
        int sum = 0;
        int four_count = 0;

        for (int i = 0; i < card_count; i++) {
            four_count = 1;
            int temp_real_value = get_real_card_value(card_date[i]);
            for (int j = 0; j < card_count; j++) {
                if ((i != j) && (temp_real_value == get_real_card_value(card_date[j])))
                    four_count++;
            }
            if (four_count == 4)
                break;
        }

        boolean all_card_less_than_five = true;

        for (int i = 0; i < card_count; i++) {
            if (get_real_card_value(card_date[i]) > 10) {
                king_count++;
            } else if (get_real_card_value(card_date[i]) == 10) {
                ten_count++;
            }
            if (get_card_value(card_date[i]) > 5) {
                all_card_less_than_five = false;
            }
            sum += get_card_value(card_date[i]);
        }

        if (getRuleValue(ruleMaps, Constants_YiYang.GAME_RULE_ZHA_DAN_NIU_BA_BEI) == 1) {
            // 炸弹 8倍
            if (four_count == 4)
                return GameConstants.OX_BOOM_VALUE;
        }

        boolean san_zhang = false;
        boolean two_zhang = false;
        for (int i = 0; i < 14; i++) {
            if (card_real_value_count[i] == 3)
                san_zhang = true;
            if (card_real_value_count[i] == 2)
                two_zhang = true;
        }

        if (getRuleValue(ruleMaps, Constants_YiYang.GAME_RULE_HU_LU_NIU_QI_BEI) == 1) {
            // 葫芦 7倍
            if (san_zhang == true && two_zhang == true) {
                return GameConstants.OX_HU_LU_VALUE;
            }
        }

        if ((same_color == true) && getRuleValue(ruleMaps, Constants_YiYang.GAME_RULE_TONG_HUA_NIU_LIU_BEI) == 1) {
            // 同花 6倍
            return GameConstants.OX_TONG_HUA_VALUE;
        }

        if (getRuleValue(ruleMaps, Constants_YiYang.GAME_RULE_SI_SHI_DA_LIU_BEI) == 1) {
            // 40大 6倍
            if (sum >= 40)
                return GameConstants.OX_WUXIAONIU_VALUE;
        }

        if (getRuleValue(ruleMaps, Constants_YiYang.GAME_RULE_SHI_XIAO_LIU_BEI) == 1) {
            // 10小 6倍
            if (sum <= 10 && all_card_less_than_five)
                return GameConstants.OX_WUXIAONIU_VALUE;
        }

        if ((line_card == true) && getRuleValue(ruleMaps, Constants_YiYang.GAME_RULE_SHUN_ZI_WU_BEI) == 1) {
            // 顺子 5倍
            return GameConstants.OX_SHUN_ZI_VALUE;
        }

        int temp_value[] = new int[GameConstants.OX_MAX_CARD_COUNT];
        sum = 0;
        for (int i = 0; i < card_count; i++) {
            temp_value[i] = get_card_value(card_date[i]);
            sum += temp_value[i];
        }

        for (int i = 0; i < card_count - 1; i++) {
            for (int j = i + 1; j < card_count; j++) {
                if ((sum - temp_value[i] - temp_value[j]) % 10 == 0) {
                    return ((temp_value[i] + temp_value[j]) > 10) ? (temp_value[i] + temp_value[j] - 10)
                            : (temp_value[i] + temp_value[j]);
                }
            }
        }

        return GameConstants.OX_VALUE0;
    }

    public int get_bei_lv(int card_date[], int card_count, Map<Integer, Integer> ruleMaps) {
        int bei_lv = 0;
        if (card_count != GameConstants.OX_MAX_CARD_COUNT)
            return 0;
        bei_lv = get_card_type(card_date, card_count, ruleMaps);
        if (bei_lv < 7) {
            bei_lv = 1;
        } else if (bei_lv == 7) {
            if (getRuleValue(ruleMaps, Constants_YiYang.GAME_RULE_NIU_QI_WU_BEI_SHU) == 1) {
                bei_lv = 1;
            } else {
                bei_lv = 2;
            }
        } else if (bei_lv == 8) {
            bei_lv = 2;
        } else if (bei_lv == 9) {
            if (getRuleValue(ruleMaps, Constants_YiYang.GAME_RULE_NIU_JIU_LIANG_BEI) == 1) {
                bei_lv = 2;
            } else {
                bei_lv = 3;
            }
        } else if (bei_lv == 10) {
            if (getRuleValue(ruleMaps, Constants_YiYang.GAME_RULE_NIU_SHI_SAN_BEI) == 1) {
                bei_lv = 3;
            } else {
                bei_lv = 4;
            }
        } else if (bei_lv == GameConstants.OX_SHUN_ZI_VALUE) {
            bei_lv = 5;
        } else if (bei_lv == GameConstants.OX_WUXIAONIU_VALUE) {
            bei_lv = 6;
        } else if (bei_lv == GameConstants.OX_SI_SHI_DA_VALUE) {
            bei_lv = 6;
        } else if (bei_lv == GameConstants.OX_TONG_HUA_VALUE) {
            bei_lv = 6;
        } else if (bei_lv == GameConstants.OX_HU_LU_VALUE) {
            bei_lv = 7;
        } else if (bei_lv == GameConstants.OX_BOOM_VALUE) {
            bei_lv = 8;
        } else if (bei_lv == GameConstants.OX_TONG_HUA_XHUN_VALUE) {
            bei_lv = 10;
        }

        return bei_lv;
    }

    public boolean analyse_ox(int card_date[], int card_count, Map<Integer, Integer> ruleMaps) {
        if (card_count != GameConstants.OX_MAX_CARD_COUNT)
            return false;

        int temp_card_data[] = new int[GameConstants.OX_MAX_CARD_COUNT];
        int card_real_value_count[] = new int[14];
        Arrays.fill(card_real_value_count, 0);

        for (int i = 0; i < GameConstants.OX_MAX_CARD_COUNT; i++) {
            temp_card_data[i] = card_date[i];
            card_real_value_count[get_real_card_value(temp_card_data[i])]++;
        }

        ox_sort_card_list(temp_card_data, card_count);

        boolean same_color = true;
        boolean line_card = true;

        int first_color = get_card_color(temp_card_data[0]);
        int first_value = get_real_card_value(temp_card_data[0]);

        if (first_value == 13) {
            if (get_real_card_value(temp_card_data[GameConstants.OX_MAX_CARD_COUNT - 1]) == 1) {
                temp_card_data[GameConstants.OX_MAX_CARD_COUNT
                        - 1] = get_card_color(temp_card_data[GameConstants.OX_MAX_CARD_COUNT - 1]) * 16 + 9;
            }
        }

        for (int i = 1; i < GameConstants.OX_MAX_CARD_COUNT; i++) {
            if (get_card_color(temp_card_data[i]) != first_color)
                same_color = false;
            if (get_real_card_value(temp_card_data[i]) != first_value - i)
                line_card = false;
            if (same_color == false && line_card == false)
                break;
        }

        if (getRuleValue(ruleMaps, Constants_YiYang.GAME_RULE_TONG_HUA_SHUN_NIU_SHI_BEI) == 1) {
            // 同花顺
            if ((same_color == true) && (line_card == true))
                return true;
        }

        @SuppressWarnings("unused")
        int king_count = 0, ten_count = 0;
        int sum = 0;
        int four_count = 0;

        for (int i = 0; i < card_count; i++) {
            four_count = 1;
            int temp_real_value = get_real_card_value(card_date[i]);
            for (int j = 0; j < card_count; j++) {
                if ((i != j) && (temp_real_value == get_real_card_value(card_date[j])))
                    four_count++;
            }
            if (four_count == 4)
                break;
        }

        boolean all_card_less_than_five = true;

        for (int i = 0; i < card_count; i++) {
            if (get_real_card_value(card_date[i]) > 10) {
                king_count++;
            } else if (get_real_card_value(card_date[i]) == 10) {
                ten_count++;
            }
            if (get_card_value(card_date[i]) > 5) {
                all_card_less_than_five = false;
            }
            sum += get_card_value(card_date[i]);
        }

        if (getRuleValue(ruleMaps, Constants_YiYang.GAME_RULE_ZHA_DAN_NIU_BA_BEI) == 1) {
            // 炸弹
            if (four_count == 4)
                return true;
        }

        boolean san_zhang = false;
        boolean two_zhang = false;
        for (int i = 0; i < 14; i++) {
            if (card_real_value_count[i] == 3)
                san_zhang = true;
            if (card_real_value_count[i] == 2)
                two_zhang = true;
        }

        if (getRuleValue(ruleMaps, Constants_YiYang.GAME_RULE_HU_LU_NIU_QI_BEI) == 1) {
            // 葫芦
            if (san_zhang == true && two_zhang == true) {
                return true;
            }
        }

        if ((same_color == true) && getRuleValue(ruleMaps, Constants_YiYang.GAME_RULE_TONG_HUA_NIU_LIU_BEI) == 1) {
            // 同花
            return true;
        }

        if (getRuleValue(ruleMaps, Constants_YiYang.GAME_RULE_SI_SHI_DA_LIU_BEI) == 1) {
            // 40大
            if (sum >= 40)
                return true;
        }

        if (getRuleValue(ruleMaps, Constants_YiYang.GAME_RULE_SHI_XIAO_LIU_BEI) == 1) {
            // 10小
            if (sum <= 10 && all_card_less_than_five)
                return true;
        }

        if ((line_card == true) && getRuleValue(ruleMaps, Constants_YiYang.GAME_RULE_TONG_HUA_NIU_LIU_BEI) == 1) {
            // 顺子
            return true;
        }

        // 设置变量
        int temp_value[] = new int[GameConstants.OX_MAX_CARD_COUNT];
        int temp_card[] = new int[GameConstants.OX_MAX_CARD_COUNT];
        for (int i = 0; i < card_count; i++) {
            temp_card[i] = card_date[i];
        }
        sum = 0;
        for (int i = 0; i < card_count; i++) {
            temp_value[i] = get_card_value(card_date[i]);
            sum += temp_value[i];
        }

        // 查找牛牛
        for (int i = 0; i < card_count - 1; i++) {
            for (int j = i + 1; j < card_count; j++) {
                if ((sum - temp_value[i] - temp_value[j]) % 10 == 0) {
                    int count = 0;
                    for (int k = 0; k < card_count; k++) {
                        if (k != i && k != j) {
                            card_date[count++] = card_date[k];
                        }
                    }

                    card_date[count++] = temp_card[i];
                    card_date[count++] = temp_card[j];

                    return true;
                }
            }
        }

        return false;
    }

    public boolean IsIntValue(int card_date[], int card_count) {
        if (card_count != GameConstants.OX_MAX_CARD_COUNT)
            return false;
        int sum = 0;
        for (int i = 0; i < card_count; i++) {
            sum += get_card_value(card_date[i]);
        }

        return (sum % 10 == 0);
    }

    public int get_boom_value(int card_data[], int card_count) {
        for (int i = 0; i < card_count; i++) {
            int four_count = 1;
            int temp_real_value = get_logic_card_value(card_data[i]);
            for (int j = 0; j < card_count; j++) {
                if ((i != j) && (temp_real_value == get_logic_card_value(card_data[j])))
                    four_count++;
            }

            if (four_count == 4) {
                return temp_real_value;
            }
        }
        return 0;
    }

    public int compare_card(int first_data[], int next_date[], int card_count, boolean first_ox, boolean next_ox,
            Map<Integer, Integer> ruleMaps) {
        if (first_ox != next_ox) {
            if (first_ox == true)
                return 1;
            if (next_ox == true)
                return -1;
        }

        int next_type = get_card_type(next_date, card_count, ruleMaps);
        int first_type = get_card_type(first_data, card_count, ruleMaps);

        if (first_type > next_type)
            return 1;
        else if (first_type < next_type)
            return -1;

        int first_temp[] = new int[GameConstants.OX_MAX_CARD_COUNT];
        int next_temp[] = new int[GameConstants.OX_MAX_CARD_COUNT];

        for (int i = 0; i < card_count; i++) {
            first_temp[i] = first_data[i];
            next_temp[i] = next_date[i];
        }

        ox_sort_card_list(first_temp, card_count);
        ox_sort_card_list(next_temp, card_count);

        int next_max_value = get_real_card_value(next_temp[0]);
        int first_max_value = get_real_card_value(first_temp[0]);

        int first_color = get_card_color(first_temp[0]);
        int next_color = get_card_color(next_temp[0]);

        if (next_type == GameConstants.OX_SHUN_ZI_VALUE || next_type == GameConstants.OX_TONG_HUA_XHUN_VALUE) {
            if (next_max_value == 13 && get_real_card_value(next_temp[GameConstants.OX_MAX_CARD_COUNT - 1]) == 1) {
                next_max_value = 14;
                next_color = get_card_color(next_temp[GameConstants.OX_MAX_CARD_COUNT - 1]);
            }
            if (first_max_value == 13 && get_real_card_value(first_temp[GameConstants.OX_MAX_CARD_COUNT - 1]) == 1) {
                first_max_value = 14;
                first_color = get_card_color(first_temp[GameConstants.OX_MAX_CARD_COUNT - 1]);
            }
        }

        if (next_max_value > first_max_value)
            return -1;
        else if (next_max_value < first_max_value)
            return 1;

        if (first_color > next_color)
            return 1;
        else
            return -1;
    }

    public boolean is_valid_card(int card) {
        int cbValue = get_card_value(card);
        int cbColor = get_card_color(card);
        return (cbValue >= 1) && (cbValue <= 10) && (cbColor <= 1);
    }
}
