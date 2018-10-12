package com.cai.game.util;

import java.util.HashMap;
import java.util.Map;

public class GenerateTable_YaoJiu {
    private static Map<Integer, Boolean>[] eye_table = new HashMap[GameUtilConstants.TABLE_COUNT];
    private static Map<Integer, Boolean>[] table = new HashMap[GameUtilConstants.TABLE_COUNT];

    private static void initialize() {
        for (int i = 0; i < GameUtilConstants.TABLE_COUNT; i++) {
            eye_table[i] = new HashMap<Integer, Boolean>();
            table[i] = new HashMap<Integer, Boolean>();
        }
    }

    private static boolean put(int[] cards, int magic_count, boolean has_eye) {
        if (magic_count < 0 || magic_count >= GameUtilConstants.TABLE_COUNT) {
            // System.err.println("王牌数目不能小于0或大于8");
            return false;
        }

        int key = 0;

        for (int i = 0; i < GameUtilConstants.MAX_CARD_VALUE; i++) {
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

        for (int i = 0; i < GameUtilConstants.MAX_CARD_VALUE; i++) {
            if (cards[i] > GameUtilConstants.CARD_COUNT_PER_CARD) {
                return true;
            }
        }

        TableManager.getInstance().put_yao_jiu(key, magic_count, has_eye);

        return true;
    }

    private static void generate_key(int[] cards, boolean has_eye) {
        if (false == put(cards, 0, has_eye))
            return;

        generate_sub_key(cards, 1, has_eye);
    }

    private static void generate_sub_key(int[] cards, int magic_count, boolean has_eye) {
        for (int i = 0; i < GameUtilConstants.MAX_CARD_VALUE; i++) {
            if (0 == cards[i])
                continue;

            cards[i]--;

            if (false == put(cards, magic_count, has_eye)) {
                cards[i]++;
                continue;
            }

            if (magic_count < GameUtilConstants.MAX_MAGIC_COUNT) {
                generate_sub_key(cards, magic_count + 1, has_eye);
            }

            cards[i]++;
        }
    }

    private static void generate_table(boolean has_eye) {
        int[] cards = new int[GameUtilConstants.MAX_CARD_VALUE];
        for (int i = 0; i < GameUtilConstants.MAX_CARD_VALUE; i++) {
            cards[i] = 0;
        }

        generate_sub_table(cards, 1, has_eye);
    }

    private static void generate_eye_table(boolean has_eye) {
        int[] cards = new int[GameUtilConstants.MAX_CARD_VALUE];
        for (int i = 0; i < GameUtilConstants.MAX_CARD_VALUE; i++) {
            cards[i] = 0;
        }

        for (int i = 0; i < GameUtilConstants.MAX_CARD_VALUE; i++) {
            if (i == 0 || i == 8) {
                cards[i] = 2;
                generate_key(cards, true);
                generate_sub_table(cards, 1, has_eye);
                cards[i] = 0;
            }
        }
    }

    private static void generate_sub_table(int[] cards, int weave_count, boolean has_eye) {
        for (int i = 0; i < GameUtilConstants.MAX_YAO_JIU_LOOPS; i++) {
            if (i == 0) {
                if (cards[0] > 3)
                    continue;

                cards[0] += 3;
            } else if (i == 1) {
                if (cards[8] > 3)
                    continue;

                cards[8] += 3;
            } else if (i == 2) {
                if (cards[0] > GameUtilConstants.CRITICAL_CARD_COUNT_PER_CARD
                        || cards[1] > GameUtilConstants.CRITICAL_CARD_COUNT_PER_CARD
                        || cards[2] > GameUtilConstants.CRITICAL_CARD_COUNT_PER_CARD)
                    continue;

                cards[0] += 1;
                cards[1] += 1;
                cards[2] += 1;
            } else if (i == 3) {
                if (cards[6] > GameUtilConstants.CRITICAL_CARD_COUNT_PER_CARD
                        || cards[7] > GameUtilConstants.CRITICAL_CARD_COUNT_PER_CARD
                        || cards[8] > GameUtilConstants.CRITICAL_CARD_COUNT_PER_CARD)
                    continue;

                cards[6] += 1;
                cards[7] += 1;
                cards[8] += 1;
            }

            generate_key(cards, has_eye);

            if (weave_count < GameUtilConstants.MAX_WEAVE_COUNT)
                generate_sub_table(cards, weave_count + 1, has_eye);

            if (i == 0) {
                cards[0] -= 3;
            } else if (i == 1) {
                cards[8] -= 3;
            } else if (i == 2) {
                cards[0] -= 1;
                cards[1] -= 1;
                cards[2] -= 1;
            } else if (i == 3) {
                cards[6] -= 1;
                cards[7] -= 1;
                cards[8] -= 1;
            }
        }
    }

    private static void generate() {
        System.out.println("Generating yao jiu table begin ...");
        initialize();
        generate_table(false);
        generate_eye_table(true);
        TableManager.getInstance().dump_yao_jiu();
        System.out.println("Generating yao jiu table finished ...");
    }

    public static void main(String[] args) {
        generate();
    }
}
