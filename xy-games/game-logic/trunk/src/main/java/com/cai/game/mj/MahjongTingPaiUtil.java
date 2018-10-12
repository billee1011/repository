package com.cai.game.mj;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.WeaveItem;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.game.util.GameUtilConstants;
import com.cai.util.SysParamServerUtil;

public class MahjongTingPaiUtil {
    private static MahjongTingPaiUtil instance;
    
    private MahjongTingPaiUtil() {
    }
    
    public static MahjongTingPaiUtil getInstance() {
        if (null == instance)
            instance = new MahjongTingPaiUtil();
        
        return instance;
    }

    public static int get_sg_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, MJTable table) {
        if (SysParamServerUtil.is_new_algorithm(3000, 3000, 1)) {
            return get_sg_ting_card_new(cards, cards_index, weaveItem, cbWeaveCount, table);
        } else {
            // return table.get_sg_ting_card(cards, cards_index, weaveItem, cbWeaveCount);
            return 0;
        }
    }

    public static int get_sg_ting_card_new(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, MJTable table) {
        int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
        for (int i = 0; i < GameConstants.MAX_INDEX; i++)
            cbCardIndexTemp[i] = cards_index[i];

        ChiHuRight chr = new ChiHuRight();

        int count = 0;
        int cbCurrentCard;
        int card_type_count = GameConstants.MAX_ZI;
        for (int i = 0; i < card_type_count; i++) {
            cbCurrentCard = table._logic.switch_to_card_data(i);
            chr.set_empty();
            if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_sg(cbCardIndexTemp, weaveItem, cbWeaveCount,
                    cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO, table)) {
                cards[count] = cbCurrentCard;
                if (table._logic.is_magic_card(cbCurrentCard))
                    cards[count] = cbCurrentCard + GameConstants.CARD_ESPECIAL_TYPE_GUI;
                count++;
            }
        }

        if (count >= card_type_count) {
            count = 1;
            cards[0] = -1;
        }

        return count;
    }

    public static int analyse_chi_hu_card_sg(int cards_index[], WeaveItem weaveItems[], int weaveCount, int cur_card,
            ChiHuRight chiHuRight, int card_type, MJTable table) {
        if (card_type != GameConstants.HU_CARD_TYPE_ZIMO)
            return GameConstants.WIK_NULL;

        if (cur_card == 0)
            return GameConstants.WIK_NULL;

        int cbChiHuKind = GameConstants.WIK_NULL;

        int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
        for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
            cbCardIndexTemp[i] = cards_index[i];
        }

        if (cur_card != GameConstants.INVALID_VALUE) {
            cbCardIndexTemp[table._logic.switch_to_card_index(cur_card)]++;
        }

        int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
        int magic_card_count = table._logic.get_magic_card_count();

        if (magic_card_count > 2) { // 一般只有两种癞子牌存在
            magic_card_count = 2;
        }

        for (int i = 0; i < magic_card_count; i++) {
            magic_cards_index[i] = table._logic.get_magic_card_index(i);
        }

        boolean bValue = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, table._logic.switch_to_card_index(cur_card),
                magic_cards_index, magic_card_count);
        
        if (!bValue) {
            chiHuRight.set_empty();
            return GameConstants.WIK_NULL;
        }

        cbChiHuKind = GameConstants.WIK_CHI_HU;

        chiHuRight.opr_or(GameConstants.CHR_ZI_MO);

        return cbChiHuKind;
    }
}
