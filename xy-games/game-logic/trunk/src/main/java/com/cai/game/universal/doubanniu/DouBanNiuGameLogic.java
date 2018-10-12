package com.cai.game.universal.doubanniu;

import com.cai.common.constant.GameConstants;
import com.cai.common.util.FvMask;
import com.cai.common.util.RandomUtil;

public class DouBanNiuGameLogic {

    public int get_card_value(int card) {
        int card_value = card & GameConstants.LOGIC_MASK_VALUE;
        return card_value > 10 ? 10 : card_value;
    }

    public int get_real_card_value(int card) {
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

    public int get_card_count_by_index(int cards_index[]) {
        int card_count = 0;
        for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++)
            card_count += cards_index[i];

        return card_count;
    }

    public int get_card_type(int card_data[], int card_count, int game_rule_index) {
        if (card_count != GameConstants.OX_MAX_CARD_COUNT)
            return 0;

        int king_count = 0, ten_count = 0;
        int sum = 0;
        int four_count = 0;
        for (int i = 0; i < card_count; i++) {
            four_count = 1;
            int temp_real_value = get_real_card_value(card_data[i]);
            for (int j = 0; j < card_count; j++) {
                if ((i != j) && (temp_real_value == get_real_card_value(card_data[j])))
                    four_count++;
            }
            if (four_count == 4)
                break;
        }
        for (int i = 0; i < card_count; i++) {
            if (get_real_card_value(card_data[i]) > 10) {
                king_count++;
            } else if (get_real_card_value(card_data[i]) == 10) {
                ten_count++;
            }
            if (get_card_value(card_data[i]) >= 5)
                sum = 11;
            sum += get_card_value(card_data[i]);

        }

        if (FvMask.has_any(game_rule_index, FvMask.mask(GameConstants.GAME_RULE_WU_XIAO_NIU))) {
            if (sum <= 10)
                return GameConstants.DBN_WUXIAONIU;
        }
        if (FvMask.has_any(game_rule_index, FvMask.mask(GameConstants.GAME_RULE_ZHA_DAN_NIU))) {
            if (four_count == 4)
                return GameConstants.DBN_BOOM;
        }
        if (FvMask.has_any(game_rule_index, FvMask.mask(GameConstants.GAME_RULE_WU_HUA_NIU))) {
            if (king_count == GameConstants.OX_MAX_CARD_COUNT)
                return GameConstants.DBN_FIVE_KING;

        }

        int temp_value[] = new int[GameConstants.OX_MAX_CARD_COUNT];
        sum = 0;
        for (int i = 0; i < card_count; i++) {
            temp_value[i] = get_card_value(card_data[i]);
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

    public int get_times_one(int card_data[], int card_count, int game_rule_index) {
        int times = 0;
        if (card_count != GameConstants.OX_MAX_CARD_COUNT)
            return 0;
        times = get_card_type(card_data, card_count, game_rule_index);
        if (times < 7)
            times = 1;
        else if (times == 7)
            times = 2;
        else if (times == 8)
            times = 2;
        else if (times == 9)
            times = 3;
        else if (times == 10)
            times = 4;
        else if (times == GameConstants.DBN_FIVE_KING)
            times = 5;
        else if (times == GameConstants.DBN_BOOM)
            times = 6;
        else if (times == GameConstants.DBN_WUXIAONIU)
            times = 8;
        return times;
    }

    public int get_times_mul(int card_data[], int card_count, int game_rule_index, int game_type_index) {
        int times = 0;
        if (card_count != GameConstants.OX_MAX_CARD_COUNT)
            return 0;
        times = get_card_type(card_data, card_count, game_rule_index);
        if (times < 8)
            times = 1;
        else if (times == 8)
            times = 2;
        else if (times == 9)
            times = 3;
        else if (times == 10)
            times = 4;
        else if (times == GameConstants.OX_YING_NIU)
            times = 5;
        else if (times == GameConstants.OX_KING_NIU)
            times = 6;
        else if (times == GameConstants.OX_BOOM_NIU)
            times = 8;
        else if (times == GameConstants.OX_WUXIAONIU_NIU)
            times = 8;
        return times;
    }

    public int get_times_two(int card_data[], int card_count, int game_rule_index) {
        int times = 0;
        if (card_count != GameConstants.OX_MAX_CARD_COUNT)
            return 0;
        times = get_card_type(card_data, card_count, game_rule_index);
        if (times <= 7)
            times = 1;
        else if (times == 8)
            times = 2;
        else if (times == 9)
            times = 2;
        else if (times == 10)
            times = 3;
        else if (times == GameConstants.DBN_FIVE_KING)
            times = 5;
        else if (times == GameConstants.DBN_BOOM)
            times = 6;
        else if (times == GameConstants.DBN_WUXIAONIU)
            times = 8;
        return times;
    }

    public boolean get_ox_card(int card_data[], int card_count, int game_rule_index) {
        if (card_count != GameConstants.OX_MAX_CARD_COUNT)
            return false;
        int king_count = 0, ten_count = 0;
        int sum = 0;
        int four_count = 0;
        for (int i = 0; i < card_count; i++) {
            four_count = 1;
            int temp_real_value = get_real_card_value(card_data[i]);
            for (int j = 0; j < card_count; j++) {
                if ((i != j) && (temp_real_value == get_real_card_value(card_data[j])))
                    four_count++;
            }
            if (four_count == 4)
                break;
        }
        for (int i = 0; i < card_count; i++) {
            if (get_real_card_value(card_data[i]) > 10) {
                king_count++;
            } else if (get_card_value(card_data[i]) == 10) {
                ten_count++;
            }
            if (get_card_value(card_data[i]) >= 5)
                sum = 11;
            sum += get_card_value(card_data[i]);

        }

        if (FvMask.has_any(game_rule_index, FvMask.mask(GameConstants.GAME_RULE_WU_XIAO_NIU))) {
            if (sum <= 10)
                return true;
        }
        if (FvMask.has_any(game_rule_index, FvMask.mask(GameConstants.GAME_RULE_ZHA_DAN_NIU))) {
            if (four_count == 4)
                return true;
        }
        if (FvMask.has_any(game_rule_index, FvMask.mask(GameConstants.GAME_RULE_WU_HUA_NIU))) {
            if (king_count == GameConstants.OX_MAX_CARD_COUNT)
                return true;
            else if (king_count == GameConstants.OX_MAX_CARD_COUNT - 1 && ten_count == 1)
                return true;
        }

        // 设置变量
        int temp_value[] = new int[GameConstants.OX_MAX_CARD_COUNT];
        int temp_card[] = new int[GameConstants.OX_MAX_CARD_COUNT];
        for (int i = 0; i < card_count; i++) {
            temp_card[i] = card_data[i];
        }
        sum = 0;
        for (int i = 0; i < card_count; i++) {
            temp_value[i] = get_card_value(card_data[i]);
            sum += temp_value[i];
        }

        // 查找牛牛
        for (int i = 0; i < card_count - 1; i++) {
            for (int j = i + 1; j < card_count; j++) {
                if ((sum - temp_value[i] - temp_value[j]) % 10 == 0) {
                    int count = 0;
                    for (int k = 0; k < card_count; k++) {
                        if (k != i && k != j) {
                            card_data[count++] = card_data[k];
                        }
                    }

                    card_data[count++] = temp_card[i];
                    card_data[count++] = temp_card[j];

                    return true;
                }
            }
        }

        return false;
    }

    public boolean IsIntValue(int card_data[], int card_count) {
        if (card_count != GameConstants.OX_MAX_CARD_COUNT)
            return false;
        int sum = 0;
        for (int i = 0; i < card_count; i++) {
            sum += get_card_value(card_data[i]);
        }

        return (sum % 10 == 0);
    }

    public void ox_sort_card_list(int card_data[], int card_count) {
        // 转换数值
        int logic_value[] = new int[GameConstants.OX_MAX_CARD_COUNT];
        for (int i = 0; i < card_count; i++)
            logic_value[i] = get_real_card_value(card_data[i]);

        // 排序操作
        boolean sorted = true;
        int temp_date, last = card_count - 1;
        do {
            sorted = true;
            for (int i = 0; i < last; i++) {
                if ((logic_value[i] < logic_value[i + 1])
                        || ((logic_value[i] == logic_value[i + 1]) && (card_data[i] < card_data[i + 1]))) {
                    // 交换位置
                    temp_date = card_data[i];
                    card_data[i] = card_data[i + 1];
                    card_data[i + 1] = temp_date;
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

    public boolean compare_card(int first_data[], int next_data[], int card_count, boolean first_ox, boolean next_ox,
            int game_rule_index) {
        if (first_ox != next_ox) {
            if (first_ox == true)
                return true;
            if (next_ox == true)
                return false;
        }

        // 比较牛大小

        // 获取点数
        int next_type = get_card_type(next_data, card_count, game_rule_index);
        int first_type = get_card_type(first_data, card_count, game_rule_index);

        // 点数判断
        if (first_type != next_type)
            return (first_type > next_type);
        if (FvMask.has_any(game_rule_index, FvMask.mask(GameConstants.GAME_RULE_ZHUANG_WIN))) {
            if (get_real_card_value(first_type) < get_real_card_value(next_type))
                return false;
            else
                return true;

        }

        // 排序大小
        int first_temp[] = new int[GameConstants.OX_MAX_CARD_COUNT];
        int next_temp[] = new int[GameConstants.OX_MAX_CARD_COUNT];
        for (int i = 0; i < card_count; i++) {
            first_temp[i] = first_data[i];
            next_temp[i] = next_data[i];
        }

        ox_sort_card_list(first_temp, card_count);
        ox_sort_card_list(next_temp, card_count);

        // 比较数值

        int next_max_value = get_real_card_value(next_temp[0]);
        int first_max_value = get_real_card_value(first_temp[0]);
        if (next_max_value != first_max_value)
            return first_max_value > next_max_value;

        // 比较颜色
        return get_card_color(first_temp[0]) > get_card_color(next_temp[0]);

    }

    public boolean is_valid_card(int card) {
        int cbValue = get_card_value(card);
        int cbColor = get_card_color(card);
        return (cbValue >= 1) && (cbValue <= 10) && (cbColor <= 1);
    }
}
