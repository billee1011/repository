package com.cai.game.util;

import java.util.HashMap;
import java.util.Map;

class GenerateFengTable_FengChi {
    private static Map<Integer, Boolean>[] eye_table = new HashMap[GameUtilConstants.TABLE_COUNT];
    private static Map<Integer, Boolean>[] table = new HashMap[GameUtilConstants.TABLE_COUNT];

    private static void initialize() {
        for (int i = 0; i < GameUtilConstants.TABLE_COUNT; i++) {
            eye_table[i] = new HashMap<Integer, Boolean>();
            table[i] = new HashMap<Integer, Boolean>();
        }
    }

    private static boolean put(int[] cards, int magic_count, boolean has_eye, boolean check_peng_hu,
            boolean check_258) {
        if (magic_count < 0 || magic_count >= GameUtilConstants.TABLE_COUNT) {
            // System.err.println("王牌数目不能小于0或大于8");
            return false;
        }

        int key = 0;

        for (int i = 0; i < GameUtilConstants.MAX_FENG_CARD_VALUE; i++) {
            key = key * 10 + cards[i];
        }

        if (0 == key)
            return false;

        Map<Integer, Boolean> map = null;

        if (has_eye) {
            map = eye_table[magic_count];
        } else {
            map = table[magic_count];
        }

        if (map.containsKey(key))
            return false;

        map.put(key, true);

        for (int i = 0; i < GameUtilConstants.MAX_FENG_CARD_VALUE; i++) {
            if (cards[i] > GameUtilConstants.CARD_COUNT_PER_CARD) {
                return true;
            }
        }

        TableManager.getInstance().put(key, magic_count, has_eye, false, check_peng_hu, check_258, true);

        return true;
    }

    private static void generate_key(int[] cards, boolean has_eye, boolean check_peng_hu, boolean check_258) {
        if (false == put(cards, 0, has_eye, check_peng_hu, check_258))
            return;

        generate_sub_key(cards, 1, has_eye, check_peng_hu, check_258);
    }

    private static void generate_sub_key(int[] cards, int magic_count, boolean has_eye, boolean check_peng_hu,
            boolean check_258) {
        for (int i = 0; i < GameUtilConstants.MAX_FENG_CARD_VALUE; i++) {
            if (0 == cards[i])
                continue;

            cards[i]--;

            if (false == put(cards, magic_count, has_eye, check_peng_hu, check_258)) {
                cards[i]++;
                continue;
            }

            if (magic_count < GameUtilConstants.MAX_MAGIC_COUNT) {
                generate_sub_key(cards, magic_count + 1, has_eye, check_peng_hu, check_258);
            }

            cards[i]++;
        }
    }

    private static void generate_table(boolean has_eye, boolean check_peng_hu, boolean check_258) {
        int[] cards = new int[GameUtilConstants.MAX_FENG_CARD_VALUE];
        for (int i = 0; i < GameUtilConstants.MAX_FENG_CARD_VALUE; i++) {
            cards[i] = 0;
        }

        generate_sub_table(cards, 1, false, check_peng_hu, check_258);
    }

    private static void generate_eye_table(boolean has_eye, boolean check_peng_hu, boolean check_258) {
        int[] cards = new int[GameUtilConstants.MAX_FENG_CARD_VALUE];
        for (int i = 0; i < GameUtilConstants.MAX_FENG_CARD_VALUE; i++) {
            cards[i] = 0;
        }

        for (int i = 0; i < GameUtilConstants.MAX_FENG_CARD_VALUE; i++) {
            cards[i] = 2;
            generate_key(cards, true, check_peng_hu, check_258);
            generate_sub_table(cards, 1, true, check_peng_hu, check_258);
            cards[i] = 0;
        }
    }

    private static void generate_sub_table(int[] cards, int weave_count, boolean has_eye, boolean check_peng_hu,
            boolean check_258) {
        for (int i = 0; i < GameUtilConstants.MAX_FENG_LOOPS; i++) {
            if (i < GameUtilConstants.MAX_FENG_CARD_VALUE) {
                if (cards[i] > 3)
                    continue;

                cards[i] += 3;
            } else if (i > GameUtilConstants.MAX_FENG_CARD_VALUE + 3 && i < GameUtilConstants.MAX_FENG_LOOPS) { // 中发白的任意三种不同的牌组成吃
                int card_index = 6;

                if (cards[card_index - 2] > GameUtilConstants.CRITICAL_CARD_COUNT_PER_CARD
                        || cards[card_index - 1] > GameUtilConstants.CRITICAL_CARD_COUNT_PER_CARD
                        || cards[card_index] > GameUtilConstants.CRITICAL_CARD_COUNT_PER_CARD)
                    continue;

                cards[card_index - 2] += 1;
                cards[card_index - 1] += 1;
                cards[card_index] += 1;
            } else { // 东南西北的任意三种不同的牌组成吃
                if (i == GameUtilConstants.MAX_FENG_CARD_VALUE) { // 东南西
                    int card_index = 0;
                    if (cards[card_index] > GameUtilConstants.CRITICAL_CARD_COUNT_PER_CARD
                            || cards[card_index + 1] > GameUtilConstants.CRITICAL_CARD_COUNT_PER_CARD
                            || cards[card_index + 2] > GameUtilConstants.CRITICAL_CARD_COUNT_PER_CARD)
                        continue;

                    cards[card_index] += 1;
                    cards[card_index + 1] += 1;
                    cards[card_index + 2] += 1;
                } else if (i == GameUtilConstants.MAX_FENG_CARD_VALUE + 1) { // 东南北
                    int card_index = 0;
                    if (cards[card_index] > GameUtilConstants.CRITICAL_CARD_COUNT_PER_CARD
                            || cards[card_index + 1] > GameUtilConstants.CRITICAL_CARD_COUNT_PER_CARD
                            || cards[card_index + 3] > GameUtilConstants.CRITICAL_CARD_COUNT_PER_CARD)
                        continue;

                    cards[card_index] += 1;
                    cards[card_index + 1] += 1;
                    cards[card_index + 3] += 1;
                } else if (i == GameUtilConstants.MAX_FENG_CARD_VALUE + 2) { // 东西北
                    int card_index = 0;
                    if (cards[card_index] > GameUtilConstants.CRITICAL_CARD_COUNT_PER_CARD
                            || cards[card_index + 2] > GameUtilConstants.CRITICAL_CARD_COUNT_PER_CARD
                            || cards[card_index + 3] > GameUtilConstants.CRITICAL_CARD_COUNT_PER_CARD)
                        continue;

                    cards[card_index] += 1;
                    cards[card_index + 2] += 1;
                    cards[card_index + 3] += 1;
                } else if (i == GameUtilConstants.MAX_FENG_CARD_VALUE + 3) { // 南西北
                    int card_index = 1;
                    if (cards[card_index] > GameUtilConstants.CRITICAL_CARD_COUNT_PER_CARD
                            || cards[card_index + 1] > GameUtilConstants.CRITICAL_CARD_COUNT_PER_CARD
                            || cards[card_index + 2] > GameUtilConstants.CRITICAL_CARD_COUNT_PER_CARD)
                        continue;

                    cards[card_index] += 1;
                    cards[card_index + 1] += 1;
                    cards[card_index + 2] += 1;
                }
            }

            generate_key(cards, has_eye, check_peng_hu, check_258);

            if (weave_count < GameUtilConstants.MAX_WEAVE_COUNT)
                generate_sub_table(cards, weave_count + 1, has_eye, check_peng_hu, check_258);

            if (i < GameUtilConstants.MAX_FENG_CARD_VALUE) { // 刻子
                cards[i] -= 3;
            } else if (i > GameUtilConstants.MAX_FENG_CARD_VALUE + 3 && i < GameUtilConstants.MAX_FENG_LOOPS) { // 中发白
                int card_index = 6;

                cards[card_index - 2] -= 1;
                cards[card_index - 1] -= 1;
                cards[card_index] -= 1;
            } else {
                if (i == GameUtilConstants.MAX_FENG_CARD_VALUE) { // 东南西
                    int card_index = 0;

                    cards[card_index] -= 1;
                    cards[card_index + 1] -= 1;
                    cards[card_index + 2] -= 1;
                } else if (i == GameUtilConstants.MAX_FENG_CARD_VALUE + 1) { // 东南北
                    int card_index = 0;

                    cards[card_index] -= 1;
                    cards[card_index + 1] -= 1;
                    cards[card_index + 3] -= 1;
                } else if (i == GameUtilConstants.MAX_FENG_CARD_VALUE + 2) { // 东西北
                    int card_index = 0;

                    cards[card_index] -= 1;
                    cards[card_index + 2] -= 1;
                    cards[card_index + 3] -= 1;
                } else if (i == GameUtilConstants.MAX_FENG_CARD_VALUE + 3) { // 南西北
                    int card_index = 1;

                    cards[card_index] -= 1;
                    cards[card_index + 1] -= 1;
                    cards[card_index + 2] -= 1;
                }
            }
        }
    }

    private static void generate() {
        System.out.println("Generating feng chi table begin ...");
        initialize();
        generate_table(false, false, false);
        generate_eye_table(true, false, false);
        TableManager.getInstance().dump_feng_chi();
        System.out.println("Generating feng chi table finished ...");
    }

    public static void main(String[] args) {
        generate();
    }
}
