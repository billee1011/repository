package com.cai.game.util;

import java.util.HashMap;
import java.util.Map;

class GenerateFengTable_PengHu {
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

        TableManager.getInstance().put(key, magic_count, has_eye, false, check_peng_hu, check_258, false);

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
        for (int i = 0; i < GameUtilConstants.MAX_FENG_CARD_VALUE; i++) {
            if (cards[i] > 3)
                continue;

            cards[i] += 3;

            generate_key(cards, has_eye, check_peng_hu, check_258);

            if (weave_count < GameUtilConstants.MAX_WEAVE_COUNT)
                generate_sub_table(cards, weave_count + 1, has_eye, check_peng_hu, check_258);

            cards[i] -= 3;
        }
    }

    private static void generate() {
        System.out.println("Generating peng hu feng table begin ...");
        initialize();
        generate_table(false, true, false);
        generate_eye_table(true, true, false);
        TableManager.getInstance().dump_ph_feng();
        System.out.println("Generating peng hu feng table finished ...");
    }

    public static void main(String[] args) {
        generate();
    }
}
