/**
 * 
 */
package com.cai.game.paijiu;

import com.cai.common.constant.GameConstants;
import com.cai.common.util.FvMask;
import com.cai.common.util.RandomUtil;

//////////////////////////////////////////////////////////////////////////
//排列类型
enum enSortCardType {
	enDescend, // 降序类型
	enAscend, // 升序类型
	enColor // 花色类型
};

public class PJGameLogic {
	public int _game_rule_index; // 游戏规则
	public int _game_type_index;

	public PJGameLogic() {

	}

	// 获取类型 两两组合成一种牌型
	public int GetCardType(int bCardData[], int bCardCount) {
		switch (bCardData[0]) {
		case 0x0C:
		case 0x2C: {
			switch (bCardData[1]) {
			case 0x0C:
			case 0x2C: {
				// 两个红Q
				return GameConstants.DEH_CT_TIANDUI;
			}
			case 0x02:
			case 0x22: {
				// 红Q红二
				return GameConstants.DEH_CT_TIANDIGONGZI;
			}
			case 0x08:
			case 0x28: {
				// 红Q红八
				return GameConstants.DEH_CT_HONGTIANGANG;
			}
			case 0x04:
			case 0x24: {
				// 红Q红四
				return GameConstants.DEH_CT_TIANGUANLIU;
			}
			case 0x1A:
			case 0x3A: {
				// 红Q黑10
				return GameConstants.DEH_CT_TIANSHIER;
			}
			case 0x14:
			case 0x34: {
				// 红Q黑四
				return GameConstants.DEH_CT_TIANGUANLIU;
			}
			case 0x16:
			case 0x36: {
				// 红Q黑六
				return GameConstants.DEH_CT_TIANGUANBA;
			}
			case 0x1B:
			case 0x3B: {
				// 红Q红J
				return GameConstants.DEH_CT_TIANFUSAN;
			}
			case 0x0A:
			case 0x2A: {
				// 红Q红10
				return GameConstants.DEH_CT_TIANSHIER;
			}
			case 0x06:
			case 0x26: {
				// 红Q红6
				return GameConstants.DEH_CT_TIANGUANBA;
			}
			case 0x07:
			case 0x27: {
				// 红Q红7
				return GameConstants.DEH_CT_TIANGUANJIU;
			}
			case 0x15:
			case 0x35: {
				// 红Q黑5
				return GameConstants.DEH_CT_TIANGUANQI;
			}
			case 0x17:
			case 0x37: {
				// 红Q黑7
				return GameConstants.DEH_CT_TIANGUANJIU;
			}
			case 0x18:
			case 0x38: {
				// 红Q黑8
				return GameConstants.DEH_CT_HEITIANGANG;
			}
			case 0x19:
			case 0x39: {
				// 红Q黑9
				return GameConstants.DEH_CT_TIANJIUWANG;
			}
			case 0x03:
			case 0x23: {
				// 红Q红3
				return GameConstants.DEH_CT_TIANDINGWU;
			}
			case 0x4F: {
				// 红Q大王
				return GameConstants.DEH_CT_TIANGUANBA;
			}
			}
			break;
		}
		case 0x02:
		case 0x22: {
			switch (bCardData[1]) {
			case 0x02:
			case 0x22: {
				return GameConstants.DEH_CT_DIDUI;
			}
			case 0x08:
			case 0x28: {
				return GameConstants.DEH_CT_HONGDIGANG;
			}
			case 0x04:
			case 0x24: {
				return GameConstants.DEH_CT_DIGUANLIU;
			}
			case 0x1A:
			case 0x3A: {
				return GameConstants.DEH_CT_DISHIER;
			}
			case 0x14:
			case 0x34: {
				return GameConstants.DEH_CT_DIGUANLIU;
			}
			case 0x16:
			case 0x36: {
				return GameConstants.DEH_CT_DIGUANBA;
			}
			case 0x1B:
			case 0x3B: {
				return GameConstants.DEH_CT_DIFUSAN;
			}
			case 0x0A:
			case 0x2A: {
				return GameConstants.DEH_CT_DISHIER;
			}
			case 0x06:
			case 0x26: {
				return GameConstants.DEH_CT_DIGUANBA;
			}
			case 0x07:
			case 0x27: {
				return GameConstants.DEH_CT_DIGUANJIU;
			}
			case 0x15:
			case 0x35: {
				return GameConstants.DEH_CT_DIGUANQI;
			}
			case 0x17:
			case 0x37: {
				return GameConstants.DEH_CT_DIGUANJIU;
			}
			case 0x18:
			case 0x38: {
				return GameConstants.DEH_CT_HEIDIGANG;
			}
			case 0x19:
			case 0x39: {
				if (this.has_rule(GameConstants.GAME_RULE_DEH_DI_JIU_WANG)) {
					return GameConstants.DEH_CT_DIJIUWANG;
				} else {
					return GameConstants.DEH_CT_JIADIJIUWANG;
				}
			}
			case 0x03:
			case 0x23: {
				return GameConstants.DEH_CT_DIDINGWU;
			}
			case 0x4F: {
				return GameConstants.DEH_CT_DIGUANBA;
			}
			}
			break;
		}
		case 0x08:
		case 0x28: {
			switch (bCardData[1]) {
			case 0x08:
			case 0x28: {
				return GameConstants.DEH_CT_RENPAIDUI;
			}
			case 0x04:
			case 0x24: {
				return GameConstants.DEH_CT_RENSHIER;
			}
			case 0x1A:
			case 0x3A: {
				return GameConstants.DEH_CT_RENSHIBA;
			}
			case 0x14:
			case 0x34: {
				return GameConstants.DEH_CT_RENSHIER;
			}
			case 0x16:
			case 0x36: {
				return GameConstants.DEH_CT_RENSHISI;
			}
			case 0x1B:
			case 0x3B: {
				return GameConstants.DEH_CT_HONGTONGJIU;
			}
			case 0x0A:
			case 0x2A: {
				return GameConstants.DEH_CT_RENSHIBA;
			}
			case 0x06:
			case 0x26: {
				return GameConstants.DEH_CT_RENSHISI;
			}
			case 0x07:
			case 0x27: {
				return GameConstants.DEH_CT_RENSHIWU;
			}
			case 0x15:
			case 0x35: {
				return GameConstants.DEH_CT_RENSHISAN;
			}
			case 0x17:
			case 0x37: {
				return GameConstants.DEH_CT_RENSHIWU;
			}
			case 0x18:
			case 0x38: {
				return GameConstants.DEH_CT_WENWULIU;
			}
			case 0x19:
			case 0x39: {
				return GameConstants.DEH_CT_SUZHOUQI;
			}
			case 0x03:
			case 0x23: {
				return GameConstants.DEH_CT_RENSHIYI;
			}
			case 0x4F: {
				return GameConstants.DEH_CT_RENSHISI;
			}
			}
			break;
		}
		case 0x04:
		case 0x24: {
			switch (bCardData[1]) {
			case 0x04:
			case 0x24: {
				return GameConstants.DEH_CT_HEPAIDUI;
			}
			case 0x1A:
			case 0x3A: {
				return GameConstants.DEH_CT_HESHISI;
			}
			case 0x14:
			case 0x34: {
				return GameConstants.DEH_CT_HEGUANBA;
			}
			case 0x16:
			case 0x36: {
				return GameConstants.DEH_CT_SILIUCOU;
			}
			case 0x1B:
			case 0x3B: {
				return GameConstants.DEH_CT_HEXINWU;
			}
			case 0x0A:
			case 0x2A: {
				return GameConstants.DEH_CT_HESHISI;
			}
			case 0x06:
			case 0x26: {
				return GameConstants.DEH_CT_SILIUCOU;
			}
			case 0x07:
			case 0x27: {
				return GameConstants.DEH_CT_HESHIYI;
			}
			case 0x15:
			case 0x35: {
				return GameConstants.DEH_CT_HEGUANJIU;
			}
			case 0x17:
			case 0x37: {
				return GameConstants.DEH_CT_HESHIYI;
			}
			case 0x18:
			case 0x38: {
				return GameConstants.DEH_CT_HESHIER;
			}
			case 0x19:
			case 0x39: {
				return GameConstants.DEH_CT_HESHISAN;
			}
			case 0x03:
			case 0x23: {
				return GameConstants.DEH_CT_DINGHEQI;
			}
			case 0x4F: {
				return GameConstants.DEH_CT_SILIUCOU;
			}
			}
			break;
		}
		case 0x1A:
		case 0x3A: {
			switch (bCardData[1]) {
			case 0x1A:
			case 0x3A: {
				return GameConstants.DEH_CT_ZHONGSANDUI;
			}
			case 0x14:
			case 0x34: {
				return GameConstants.DEH_CT_MEISHISI;
			}
			case 0x16:
			case 0x36: {
				return GameConstants.DEH_CT_MEISHILIU;
			}
			case 0x1B:
			case 0x3B: {
				return GameConstants.DEH_CT_MEISHIYI;
			}
			case 0x0A:
			case 0x2A: {
				return GameConstants.DEH_CT_SHUANGHUASHI;
			}
			case 0x06:
			case 0x26: {
				return GameConstants.DEH_CT_MEISHILIU;
			}
			case 0x07:
			case 0x27: {
				return GameConstants.DEH_CT_MEISHIQI;
			}
			case 0x15:
			case 0x35: {
				return GameConstants.DEH_CT_MEISHIWU;
			}
			case 0x17:
			case 0x37: {
				return GameConstants.DEH_CT_MEISHIQI;
			}
			case 0x18:
			case 0x38: {
				return GameConstants.DEH_CT_MEISHIBA;
			}
			case 0x19:
			case 0x39: {
				return GameConstants.DEH_CT_MEISHIJIU;
			}
			case 0x03:
			case 0x23: {
				return GameConstants.DEH_CT_MIESHISAN;
			}
			case 0x4F: {
				return GameConstants.DEH_CT_MEISHILIU;
			}
			}
			break;
		}
		case 0x14:
		case 0x34: {
			switch (bCardData[1]) {
			case 0x14:
			case 0x34: {
				return GameConstants.DEH_CT_ZHONGSANDUI;
			}
			case 0x16:
			case 0x36: {
				return GameConstants.DEH_CT_SILIUCOU;
			}
			case 0x1B:
			case 0x3B: {
				return GameConstants.DEH_CT_BANFUWU;
			}
			case 0x0A:
			case 0x2A: {
				return GameConstants.DEH_CT_CHANGSHISI;
			}
			case 0x1A:
			case 0x3A: {
				return GameConstants.DEH_CT_MEISHISI;
			}
			case 0x06:
			case 0x26: {
				return GameConstants.DEH_CT_SILIUCOU;
			}
			case 0x07:
			case 0x27: {
				return GameConstants.DEH_CT_CHANGSHIYI;
			}
			case 0x15:
			case 0x35: {
				return GameConstants.DEH_CT_BANWUJIU;
			}
			case 0x17:
			case 0x37: {
				return GameConstants.DEH_CT_CHANGSHIYI;
			}
			case 0x18:
			case 0x38: {
				return GameConstants.DEH_CT_CHANGSHIER;
			}
			case 0x19:
			case 0x39: {
				return GameConstants.DEH_CT_SIJIUSAN;
			}
			case 0x03:
			case 0x23: {
				return GameConstants.DEH_CT_DINGCHANGQI;
			}
			case 0x4F: {
				return GameConstants.DEH_CT_SILIUCOU;
			}
			}
			break;
		}
		case 0x16:
		case 0x36: {
			switch (bCardData[1]) {
			case 0x16:
			case 0x36: {
				return GameConstants.DEH_CT_ZHONGSANDUI;
			}
			case 0x1A:
			case 0x3A: {
				return GameConstants.DEH_CT_MEISHILIU;
			}
			case 0x14:
			case 0x34: {
				return GameConstants.DEH_CT_SILIUCOU;
			}
			case 0x1B:
			case 0x3B: {
				return GameConstants.DEH_CT_FUCHANGQI;
			}
			case 0x0A:
			case 0x2A: {
				return GameConstants.DEH_CT_CHANGSHILIU;
			}
			case 0x06:
			case 0x26: {
				return GameConstants.DEH_CT_ERLIUER;
			}
			case 0x07:
			case 0x27: {
				return GameConstants.DEH_CT_CHANGSHISAN;
			}
			case 0x15:
			case 0x35: {
				return GameConstants.DEH_CT_CHANGSANYI;
			}
			case 0x17:
			case 0x37: {
				return GameConstants.DEH_CT_CHANGSHISAN;
			}
			case 0x18:
			case 0x38: {
				return GameConstants.DEH_CT_CHANGSHISI;
			}
			case 0x19:
			case 0x39: {
				return GameConstants.DEH_CT_BANFUWU;
			}
			case 0x03:
			case 0x23: {
				return GameConstants.DEH_CT_DINGCHANGJIU;
			}
			case 0x4F: {
				return GameConstants.DEH_CT_ERLIUER;
			}
			}
			break;
		}
		case 0x1B:
		case 0x3B: {
			switch (bCardData[1]) {
			case 0x1B:
			case 0x3B: {
				return GameConstants.DEH_CT_XIAYODUI;
			}
			case 0x0A:
			case 0x2A: {
				return GameConstants.DEH_CT_FUSHIYI;
			}
			case 0x06:
			case 0x26: {
				return GameConstants.DEH_CT_FUMAOGQI;
			}
			case 0x07:
			case 0x27: {
				return GameConstants.DEH_CT_FUSHIBA;
			}
			case 0x15:
			case 0x35: {
				return GameConstants.DEH_CT_FUSHILIU;
			}
			case 0x17:
			case 0x37: {
				return GameConstants.DEH_CT_FUSHIBA;
			}
			case 0x18:
			case 0x38: {
				return GameConstants.DEH_CT_WULONGJIU;
			}
			case 0x19:
			case 0x39: {
				return GameConstants.DEH_CT_FUJIUCOU;
			}
			case 0x03:
			case 0x23: {
				return GameConstants.DEH_CT_DINGFUSI;
			}
			case 0x4F: {
				return GameConstants.DEH_CT_FUMAOGQI;
			}
			}
			break;
		}
		case 0x0A:
		case 0x2A: {
			switch (bCardData[1]) {
			case 0x0A:
			case 0x2A: {
				return GameConstants.DEH_CT_XIAYODUI;
			}
			case 0x14:
			case 0x34: {
				return GameConstants.DEH_CT_CHANGSHISI;
			}
			case 0x1B:
			case 0x3B: {
				return GameConstants.DEH_CT_FUSHIYI;
			}
			case 0x06:
			case 0x26: {
				return GameConstants.DEH_CT_TIAOSHILIU;
			}
			case 0x07:
			case 0x27: {
				return GameConstants.DEH_CT_TIAOSHIQI;
			}
			case 0x15:
			case 0x35: {
				return GameConstants.DEH_CT_TIAOSHIWU;
			}
			case 0x17:
			case 0x37: {
				return GameConstants.DEH_CT_TIAOSHIQI;
			}
			case 0x18:
			case 0x38: {
				return GameConstants.DEH_CT_TIAOSHIBA;
			}
			case 0x19:
			case 0x39: {
				return GameConstants.DEH_CT_TIAOSHIJIU;
			}
			case 0x03:
			case 0x23: {
				return GameConstants.DEH_CT_TIAOSHISAN;
			}
			case 0x4F: {
				return GameConstants.DEH_CT_TIAOSHILIU;
			}
			}
			break;
		}
		case 0x06:
		case 0x26: {
			switch (bCardData[1]) {
			case 0x06:
			case 0x26: {
				return GameConstants.DEH_CT_XIAYODUI;
			}
			case 0x0A:
			case 0x2A: {
				return GameConstants.DEH_CT_TIAOSHILIU;
			}
			case 0x1B:
			case 0x3B: {
				return GameConstants.DEH_CT_FUMAOGQI;
			}
			case 0x07:
			case 0x27: {
				return GameConstants.DEH_CT_CHANGSHISAN;
			}
			case 0x15:
			case 0x35: {
				return GameConstants.DEH_CT_WULIUYI;
			}
			case 0x17:
			case 0x37: {
				return GameConstants.DEH_CT_CHANGSHISAN;
			}
			case 0x18:
			case 0x38: {
				return GameConstants.DEH_CT_CHANGSHISI;
			}
			case 0x19:
			case 0x39: {
				return GameConstants.DEH_CT_MAOSHIWU;
			}
			case 0x03:
			case 0x23: {
				return GameConstants.DEH_CT_DINGMAOJIU;
			}
			case 0x4F: {
				return GameConstants.DEH_CT_YAOGAOER;
			}
			}
			break;
		}
		case 0x07:
		case 0x27: {
			switch (bCardData[1]) {
			case 0x07:
			case 0x27: {
				return GameConstants.DEH_CT_XIAYODUI;
			}
			case 0x1B:
			case 0x3B: {
				return GameConstants.DEH_CT_FUSHIBA;
			}
			case 0x0A:
			case 0x2A: {
				return GameConstants.DEH_CT_TIAOSHIQI;
			}
			case 0x06:
			case 0x26: {
				return GameConstants.DEH_CT_CHANGSHISAN;
			}
			case 0x15:
			case 0x35: {
				return GameConstants.DEH_CT_YAOGAOER;
			}
			case 0x17:
			case 0x37: {
				return GameConstants.DEH_CT_SHUANGHUAQI;
			}
			case 0x18:
			case 0x38: {
				return GameConstants.DEH_CT_YAOGAOWU;
			}
			case 0x19:
			case 0x39: {
				return GameConstants.DEH_CT_YAOGAOLIU;
			}
			case 0x03:
			case 0x23: {
				return GameConstants.DEH_CT_DINGQICOU;
			}
			case 0x4F: {
				return GameConstants.DEH_CT_QILIUSAN;
			}
			}
			break;
		}
		case 0x15:
		case 0x35: {
			switch (bCardData[1]) {
			case 0x15:
			case 0x35: {
				return GameConstants.DEH_CT_SILAN;
			}
			case 0x17:
			case 0x37: {
				return GameConstants.DEH_CT_QIWUER;
			}
			case 0x18:
			case 0x38: {
				return GameConstants.DEH_CT_BAWUSAN;
			}
			case 0x19:
			case 0x39: {
				return GameConstants.DEH_CT_JIUWUSI;
			}
			case 0x03:
			case 0x23: {
				return GameConstants.DEH_CT_DINGWUBA;
			}
			case 0x4F: {
				return GameConstants.DEH_CT_WULIUYI;
			}
			}
			break;
		}
		case 0x17:
		case 0x37: {
			switch (bCardData[1]) {
			case 0x17:
			case 0x37: {
				return GameConstants.DEH_CT_SILAN;
			}
			case 0x15:
			case 0x35: {
				return GameConstants.DEH_CT_QIWUER;
			}
			case 0x18:
			case 0x38: {
				return GameConstants.DEH_CT_QIBAWU;
			}
			case 0x19:
			case 0x39: {
				return GameConstants.DEH_CT_JIUQILIU;
			}
			case 0x03:
			case 0x23: {
				return GameConstants.DEH_CT_DINGQICOU;
			}
			case 0x4F: {
				return GameConstants.DEH_CT_QILIUSAN;
			}
			}
			break;
		}
		case 0x18:
		case 0x38: {
			switch (bCardData[1]) {
			case 0x18:
			case 0x38: {
				return GameConstants.DEH_CT_SILAN;
			}
			case 0x15:
			case 0x35: {
				return GameConstants.DEH_CT_BAWUSAN;
			}
			case 0x17:
			case 0x37: {
				return GameConstants.DEH_CT_QIBAWU;
			}
			case 0x19:
			case 0x39: {
				return GameConstants.DEH_CT_YANGZHOUQI;
			}
			case 0x03:
			case 0x23: {
				return GameConstants.DEH_CT_DINGBAYI;
			}
			case 0x4F: {
				return GameConstants.DEH_CT_BALIUSI;
			}
			}
			break;
		}
		case 0x19:
		case 0x39: {
			switch (bCardData[1]) {
			case 0x19:
			case 0x39: {
				return GameConstants.DEH_CT_SILAN;
			}
			case 0x18:
			case 0x38: {
				return GameConstants.DEH_CT_YANGZHOUQI;
			}
			case 0x15:
			case 0x35: {
				return GameConstants.DEH_CT_JIUWUSI;
			}
			case 0x17:
			case 0x37: {
				return GameConstants.DEH_CT_JIUQILIU;
			}
			case 0x03:
			case 0x23: {
				return GameConstants.DEH_CT_DINGJIUER;
			}
			case 0x4F: {
				return GameConstants.DEH_CT_YAOGAOWU;
			}
			}
			break;
		}
		case 0x03:
		case 0x23: {
			switch (bCardData[1]) {
			case 0x17:
			case 0x37: {
				return GameConstants.DEH_CT_DINGQICOU;
			}
			case 0x15:
			case 0x35: {
				return GameConstants.DEH_CT_DINGWUBA;
			}
			case 0x18:
			case 0x38: {
				return GameConstants.DEH_CT_DINGBAYI;
			}
			case 0x19:
			case 0x39: {
				return GameConstants.DEH_CT_DINGJIUER;
			}
			case 0x4F: {
				return GameConstants.DEH_CT_DINGERHUANG;
			}
			}
			break;
		}
		case 0x4F: {
			switch (bCardData[1]) {
			case 0x17:
			case 0x37: {
				return GameConstants.DEH_CT_QILIUSAN;
			}
			case 0x15:
			case 0x35: {
				return GameConstants.DEH_CT_WULIUYI;
			}
			case 0x18:
			case 0x38: {
				return GameConstants.DEH_CT_BALIUSI;
			}
			case 0x19:
			case 0x39: {
				return GameConstants.DEH_CT_YAOGAOWU;
			}
			case 0x03:
			case 0x23: {
				return GameConstants.DEH_CT_DINGERHUANG;
			}
			}
			break;
		}
		}
		return GameConstants.DEH_CT_DINGERHUANG;
	}

	public boolean is_dui_zi(int bCardData[], int bCardCount) {
		if (this.get_card_value(bCardData[0]) != this.get_card_value(bCardData[1])) {
			return false;
		}
		int card_color_one = get_card_color(bCardData[0]);
		int card_color_two = get_card_color(bCardData[1]);
		if ((card_color_one + 2) % 4 == card_color_two) {
			return true;
		}
		return false;
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
	public void SortCardList(int cbCardData[], int cbCardCount) {
		// 转换数值
		int cbLogicValue[] = new int[cbCardCount];
		for (int i = 0; i < cbCardCount; i++)
			cbLogicValue[i] = GetCardLogicValue(cbCardData[i]);

		// 排序操作
		boolean bSorted = true;
		int cbTempData, bLast = cbCardCount - 1;
		do {
			bSorted = true;
			for (int i = 0; i < bLast; i++) {
				if ((cbLogicValue[i] < cbLogicValue[i + 1])
						|| ((cbLogicValue[i] == cbLogicValue[i + 1]) && (cbCardData[i] < cbCardData[i + 1]))) {
					// 交换位置
					cbTempData = cbCardData[i];
					cbCardData[i] = cbCardData[i + 1];
					cbCardData[i + 1] = cbTempData;
					cbTempData = cbLogicValue[i];
					cbLogicValue[i] = cbLogicValue[i + 1];
					cbLogicValue[i + 1] = cbTempData;
					bSorted = false;
				}
			}
			bLast--;
		} while (bSorted == false);

		return;
	}

	public void SortCardListByValue(int cbCardData[], int cbCardCount) {
		// 转换数值
		int cbLogicValue[] = new int[cbCardCount];
		for (int i = 0; i < cbCardCount; i++)
			cbLogicValue[i] = get_sort_value(cbCardData[i]);

		// 排序操作
		boolean bSorted = true;
		int cbTempData, bLast = cbCardCount - 1;
		do {
			bSorted = true;
			for (int i = 0; i < bLast; i++) {
				if ((cbLogicValue[i] < cbLogicValue[i + 1])
						|| ((cbLogicValue[i] == cbLogicValue[i + 1]) && (cbCardData[i] < cbCardData[i + 1]))) {
					// 交换位置
					cbTempData = cbCardData[i];
					cbCardData[i] = cbCardData[i + 1];
					cbCardData[i + 1] = cbTempData;
					cbTempData = cbLogicValue[i];
					cbLogicValue[i] = cbLogicValue[i + 1];
					cbLogicValue[i + 1] = cbTempData;
					bSorted = false;
				}
			}
			bLast--;
		} while (bSorted == false);

		return;
	}

	public int GetCardLogicValue(int card) {
		if (card == 0x0C || card == 0x2C) {
			return 17;
		}
		if (card == 0x02 || card == 0x22) {
			return 16;
		}
		if (card == 0x08 || card == 0x28) {
			return 15;
		}
		if (card == 0x04 || card == 0x24) {
			return 14;
		}
		if (card == 0x1A || card == 0x3A) {
			return 13;
		}
		if (card == 0x14 || card == 0x34) {
			return 13;
		}
		if (card == 0x16 || card == 0x36) {
			return 13;
		}
		if (card == 0x1B || card == 0x3B) {
			return 12;
		}
		if (card == 0x0A || card == 0x2A) {
			return 12;
		}
		if (card == 0x06 || card == 0x26) {
			return 12;
		}
		if (card == 0x07 || card == 0x27) {
			return 12;
		}
		if (card == 0x15 || card == 0x35) {
			return 11;
		}
		if (card == 0x17 || card == 0x37) {
			return 11;
		}
		if (card == 0x18 || card == 0x38) {
			return 11;
		}
		if (card == 0x19 || card == 0x39) {
			return 11;
		}
		if (card == 0x03 || card == 0x23) {
			return 11;
		}
		if (card == 0x4E) {
			return 11;
		}
		return 0;
	}

	public int compare_data(int frist_card[], int frist_count, int next_card[], int next_count) {
		int first_color_one = this.get_card_color(frist_card[0]) % 2;
		int first_color_two = this.get_card_color(frist_card[1]) % 2;
		int first_value_one = this.get_card_value(frist_card[0]);
		int first_value_two = this.get_card_value(frist_card[1]);
		int next_color_one = this.get_card_color(next_card[0]) % 2;
		int next_color_two = this.get_card_color(next_card[1]) % 2;
		int next_value_one = this.get_card_value(next_card[0]);
		int next_value_two = this.get_card_value(next_card[1]);
		if (frist_card[0] >= 0x4E) {
			first_color_one = 3;
		}
		if (frist_card[1] >= 0x4E) {
			first_color_two = 3;
		}
		if (next_card[0] >= 0x4E) {
			next_color_one = 3;
		}
		if (next_card[1] >= 0x4E) {
			next_color_two = 3;
		}
		// 丁二皇最大
		int frist_type = this.GetCardType(frist_card, frist_count);
		int next_type = this.GetCardType(next_card, next_count);
		if (frist_type == GameConstants.DEH_CT_DINGERHUANG) {
			return 1;
		}
		if (next_type == GameConstants.DEH_CT_DINGERHUANG) {
			return -1;
		}
		// 对子
		if (first_color_one == first_color_two && first_value_one == first_value_two) {
			if (next_color_one != next_color_two || next_value_one != next_value_two) {
				return 1;
			} else {
				if (GetCardLogicValue(next_card[0]) > GetCardLogicValue(frist_card[0])) {
					return -1;
				} else if (GetCardLogicValue(next_card[0]) < GetCardLogicValue(frist_card[0])) {
					return 1;
				} else {
					return 0;
				}
			}
		} else {
			if (next_color_one == next_color_two && next_value_one == next_value_two) {
				return -1;
			}
		}
		if (frist_type >= GameConstants.DEH_CT_HEIDIGANG && next_type < GameConstants.DEH_CT_HEIDIGANG) {
			return 1;
		} else if (frist_type < GameConstants.DEH_CT_HEIDIGANG && next_type >= GameConstants.DEH_CT_HEIDIGANG) {
			return -1;
		} else if (frist_type >= GameConstants.DEH_CT_HEIDIGANG && next_type >= GameConstants.DEH_CT_HEIDIGANG) {
			if (frist_type == next_type) {
				return 0;
			} else {
				if (frist_type == GameConstants.DEH_CT_TIANJIUWANG) {
					return 1;
				}
				if (next_type == GameConstants.DEH_CT_TIANJIUWANG) {
					return -1;
				}
				if (frist_type == GameConstants.DEH_CT_DIJIUWANG) {
					return 1;
				}
				if (next_type == GameConstants.DEH_CT_DIJIUWANG) {
					return -1;
				}
				if (frist_type == GameConstants.DEH_CT_HONGTIANGANG) {
					if (next_type == GameConstants.DEH_CT_HEITIANGANG
							|| next_type == GameConstants.DEH_CT_HONGTIANGANG) {
						return 0;
					}
					return 1;
				}
				if (next_type == GameConstants.DEH_CT_HONGTIANGANG) {
					if (frist_type == GameConstants.DEH_CT_HEITIANGANG) {
						return 0;
					}
					return -1;
				}
				if (frist_type == GameConstants.DEH_CT_HEITIANGANG) {
					return 1;
				}
				if (next_type == GameConstants.DEH_CT_HEITIANGANG) {
					return -1;
				}
				return 0;
			}
		}

		if ((first_value_one + first_value_two) % 10 > (next_value_one + next_value_two) % 10) {
			return 1;
		} else if ((first_value_one + first_value_two) % 10 < (next_value_one + next_value_two) % 10) {
			return -1;
		} else {
			if ((first_value_one + first_value_two) % 10 == 0 && (next_value_one + next_value_two) % 10 == 0) {
				return 0;
			}
			if (GetCardLogicValue(next_card[0]) > GetCardLogicValue(frist_card[0])) {
				return -1;
			} else if (GetCardLogicValue(next_card[0]) < GetCardLogicValue(frist_card[0])) {
				return 1;
			} else {
				return 0;
			}
		}

	}

	// 获取数值
	public int get_card_value(int card) {
		if (card == 0x4F) {
			return 6;
		}
		return card & GameConstants.LOGIC_MASK_VALUE;
	}

	// 获取数值
	public int get_sort_value(int card) {

		return card & GameConstants.LOGIC_MASK_VALUE;
	}

	// 获取花色
	public int get_card_color(int card) {
		return (card & GameConstants.LOGIC_MASK_COLOR) >> 4;
	}

	public boolean has_rule(int cbRule) {
		return FvMask.has_any(_game_rule_index, FvMask.mask(cbRule));
	}

	// 删除扑克
	public boolean check_data(int cards[], int card_count, int check_cards[], int check_count) {
		// 检验数据
		if (card_count != check_count)
			return false;

		// 定义变量
		int cbDeleteCount = 0;
		int cbTempCardData[] = new int[card_count];

		for (int i = 0; i < card_count; i++) {
			cbTempCardData[i] = cards[i];
		}

		// 置零扑克
		for (int i = 0; i < check_count; i++) {
			for (int j = 0; j < card_count; j++) {
				if (check_cards[i] == cbTempCardData[j]) {
					cbDeleteCount++;
					cbTempCardData[j] = 0;
					break;
				}
			}
		}

		// 成功判断
		if (cbDeleteCount != check_count) {
			return false;
		}
		return true;
	}
}
