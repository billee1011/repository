/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.game.sdh.handler.yybs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.game.sdh.SDHConstants_XT;
import com.cai.common.constant.game.sdh.SDHConstants_YYBS;

import java.util.Set;

/**
 * 
 * 反主信息
 * 
 * @author WalkerGeek date: 2018年4月28日 上午10:13:53 <br/>
 */
public class MainInfoModel {

	public static final int MainKing = 1; // 王主信息
	public static final int MainTenBlack = 2; // 黑10主信息
	public static final int MainTenRed = 3; // 红10主信息
	public static final int MainTwoBlack = 4; // 黑2主信息
	public static final int MainTwoRed = 5; // 红2主信息

	private Set<Integer> mainColorSet; // 叫主的花色
	private Map<Integer, List<Integer>> mainColorInfo; // 花色主信息
	private boolean mianThree; // 三级主
	private boolean mainFour; // 四级主
	private Map<Integer, List<Integer>> mainInfo; // 三级主
	private int red2Card; // 红二值
	public MainInfoModel infoModel; //反主信息

	public MainInfoModel() {
		mainColorSet = new HashSet<Integer>();
		mianThree = false;
		mainFour = false;
		mainInfo = null;
		mainColorInfo = null;
		red2Card = GameConstants.INVALID_CARD;
		//infoModel = new MainInfoModel();
	}

	public void addMainCard(int color, int value, int card) {

		// 花色主判断
		// hasChang = !mainColorSet.contains(color);
		if (color != 4 && value==10) {
			mainColorSet.add(color);
			if (mainColorInfo == null) {
				mainColorInfo = new HashMap<>();
			}
			List<Integer> cardListColor = mainColorInfo.get(color);
			if (cardListColor == null) {
				cardListColor = new ArrayList<Integer>();
			}
			cardListColor.add(card);
			mainColorInfo.put(color, cardListColor);
		}

		int key = getMapKey(color, value);
		if (mainInfo == null) {
			mainInfo = new HashMap<>();
		}
		List<Integer> cardList = mainInfo.get(key);
		if (cardList == null) {
			cardList = new ArrayList<Integer>();
		}
		cardList.add(card);
		mainInfo.put(key, cardList);

		// 判断是否有三级4级
		for (Entry<Integer, List<Integer>> entry : mainInfo.entrySet()) {
			int count = entry.getValue().size();
			
			if( entry.getKey() == MainKing || entry.getKey() ==MainTenBlack || entry.getKey() == MainTenRed){
				continue;
			}
			switch (count) {
			case 3:
				mianThree = true;
				break;
			case 4:
				mainFour = true;
				break;

			}
		}
	}

	private int getMapKey(int color, int value) {
		int kay = -1;
		if(color == 4){
			return MainKing;
		}
		switch (value) {
		case 2:
			if (color == 0 || color == 2) {
				kay = MainTwoRed;
			} else {
				kay = MainTwoBlack;
			}
			break;
		case 10:
			if (color == 0 || color == 2) {
				kay = MainTenRed;
			} else {
				kay = MainTenBlack;
			}
			break;
		default:
			kay = MainKing;
			break;
		}
		return kay;
	}

	/**
	 * 能叫主类型
	 * @param types
	 * @return
	 */
	public long[] getEfferTypes(int types,boolean disPatchCardEnd) {
		List<Integer> effertypeList = new ArrayList<Integer>();
		Set<Integer> color = getMainColorSet(types,disPatchCardEnd);
		if (color.size() > 0) {
			effertypeList.add(SDHConstants_YYBS.Player_Status_CALL_MAIN);
		}

		if (mianThree) {
			effertypeList.add(SDHConstants_YYBS.PLAYER_STATUS_FAN_MAIN3);
		}

		if (mainFour) {
			effertypeList.add(SDHConstants_YYBS.PLAYER_STATUS_FAN_MAIN4);
		}

		long type[] = new long[effertypeList.size()];
		for (int i = 0; i < effertypeList.size(); i++) {
			type[i] = effertypeList.get(i);
		}

		return type;
	}
	public boolean checkValidity(int[] mainCards,int mainColor,  List<Integer> mainCardArray,int type ,boolean disPatchCardEnd) {
		int count = 0;
		int key = 0;
		for(int i = 0; i < mainCards.length; i++){
			if(mainCards[i] == 0){
				continue;
			}
			count++;
			key = getMapKey(getCardColor(mainCards[i]), getCardValue(mainCards[i]));
		}
			
		if(mainCardArray.size() == 0 && mainColor != -1){
			//已经3级反主再有二级反正进入直接过滤
			if(count > 2){
				return false;
			}
			
			if(count == 0){
				//第一个叫主玩家校验数据是否合法
				Set<Integer> set= getMainColorSet(type, disPatchCardEnd);
				if(set.contains(mainColor)){
					return true;
				}else{
					return false;
				}
			}else if(count == 1){ //校验第二次叫主信息
				Set<Integer> set= getMainColorSet(type, disPatchCardEnd);
				if(set.contains(mainColor)){
					if(mainColorInfo.get(mainColor).size() > 1){
						return true;
					}else{
						return false;
					}
				}
			}else if(count == 2){
				Set<Integer> set= getMainColorSet(type, disPatchCardEnd);
				if(set.contains(mainColor) && mainColorInfo.get(mainColor).size() > 1){
					int cardColor1 = getCardColor(mainCards[0]);
					if(mainColor > cardColor1){
						return true;
					}else {
						return false;
					}
				}
				
			}
		}
		if(mainCardArray.size()> count){
			return true;
		}else if(mainCardArray.size() == count){
			int key2 = getMapKey(getCardColor(mainCardArray.get(0)), getCardValue(mainCardArray.get(0)));
			if(key2 < key){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 反主类型,初始化反主信息
	 * @param types
	 * @param mainCards
	 * @return
	 */
	public long[] getEfferTypesBack(int types,int[] mainCards,boolean disPatchCardEnd) {
		if(mainInfo == null){
			return new long[0];
		}
		//初始化反主信息
		infoModel = new MainInfoModel();
		//上家叫主信息分析
		int count = 0;
		int key = 0;
		for(int i = 0; i < mainCards.length; i++){
			if(mainCards[i] == 0){
				continue;
			}
			count++;
			key = getMapKey(getCardColor(mainCards[i]), getCardValue(mainCards[i]));
		}
		
		final int number = count;
		final int key1 = key;
		Set<Integer> sets = new HashSet<Integer>();
		if(key>0){
			// 判断是否有三级4级
			for (Entry<Integer, List<Integer>> entry : mainInfo.entrySet()) {
				if( entry.getKey() ==MainTenBlack || entry.getKey() == MainTenRed){
					continue;
				}
				switch (entry.getValue().size()) {
				case 3:
					mianThree = true;
					break;
				case 4:
					mianThree = true;
					mainFour = true;
					break;

				}
			}
		}else {
			for (Entry<Integer, List<Integer>> entry : mainInfo.entrySet()) {
				if( entry.getKey() ==MainTenBlack || entry.getKey() == MainTenRed){
					continue;
				}
				if(entry.getValue().size() > 2){
					sets.add(SDHConstants_YYBS.PLAYER_STATUS_CANT_FAN_MAIN_KING);
				}
			}
		}
		//判断符合反主的卡牌
		List<Integer> effertypeList = new ArrayList<Integer>();
		Set<Integer> color = getMainColorSet(types,disPatchCardEnd);
		if (color.size() > 0 && count <= 2) {
			//上家是单牌：所有对子
			if(count <= 1){
				mainColorInfo.forEach((keyColor, list) -> {
					int cardValue = getCardValue(list.get(0));
					if(cardValue == 10 && list.size() > number){
						addColorMainBack(keyColor, list);
					}
				});
			} else if(count == 2){ // 对子互反
				final int colorTwo = getCardColor(mainCards[0]);
				mainColorInfo.forEach((keyColor, list) -> {
					if(list.size() == number){
						int cardValue = getCardValue(list.get(0));
						int cardColor = getCardColor(list.get(0));
						if(cardValue == 10 && cardColor > colorTwo){
							addColorMainBack(keyColor, list);
						}
					}
				});
			}
			if(infoModel.mainColorSet.size() > 0 ){
				effertypeList.add(SDHConstants_YYBS.Player_Status_CALL_MAIN);
			}
		}
		
		if (mianThree && count <= 3) {
			if(count < 3){
				mainInfo.forEach((keyColor, list) -> {
					int key2 =getMapKey(getCardColor(list.get(0)), getCardValue(list.get(0)));
					if(key2 ==MainTenRed || key2 == MainTenBlack || ( key2 == MainKing && key1== 0)){
						if(key2 == MainKing && key1== 0){
							sets.add(SDHConstants_YYBS.PLAYER_STATUS_CANT_FAN_MAIN_KING);
						}
						return;
					}
					if(list.size() >= 3){
						addThreeMainBack(keyColor, list);
						infoModel.mianThree = true;
					}
				});
			} else if(count == 3){
				mainInfo.forEach((keyColor, list) -> {
					int key2 =getMapKey(getCardColor(list.get(0)), getCardValue(list.get(0)));
					if(key2 ==MainTenRed || key2 == MainTenBlack || ( key2 == MainKing && key1== 0)){
						if(key2 == MainKing && key1== 0){
							sets.add(SDHConstants_YYBS.PLAYER_STATUS_CANT_FAN_MAIN_KING);
						}
						return;
					}
					if(list.size() >= 3 ){
						if(key2 < key1){
							addThreeMainBack(keyColor, list);
							infoModel.mianThree = true;
						}
					}
				});
			} 
			if(infoModel.mianThree){
				effertypeList.add(SDHConstants_YYBS.PLAYER_STATUS_FAN_MAIN3);
			}
		}

		if (mainFour && count <= 4 ) {
			if(count < 4){
				mainInfo.forEach((keyColor, list) -> {
					int key2 = getMapKey(getCardColor(list.get(0)), getCardValue(list.get(0)));
					if(key2 == MainTenRed || key2 == MainTenBlack || ( key2 == MainKing && key1== 0)){
						if(key2 == MainKing && key1== 0){
							sets.add(SDHConstants_YYBS.PLAYER_STATUS_CANT_FAN_MAIN_KING);
						}
						return;
					}
					if(list.size() == 4){
						addThreeMainBack(keyColor, list);
						infoModel.mainFour = true;
					}
				});
			} else if(count == 4 && key != MainKing){
				mainInfo.forEach((keyColor, list) -> {
					if(list.size() == 4){
						int key2 = getMapKey(getCardColor(list.get(0)), getCardValue(list.get(0)));
						if(key2 ==MainTenRed || key2 == MainTenBlack || ( key2 == MainKing && key1== 0)){
							if(key2 == MainKing && key1== 0){
								sets.add(SDHConstants_YYBS.PLAYER_STATUS_CANT_FAN_MAIN_KING);
							}
							return;
						}
						if(key2 < key1){
							addThreeMainBack(keyColor, list);
							infoModel.mainFour = true;
						}
					}
				});
			}
			
			if(infoModel.mainFour){
				effertypeList.add(SDHConstants_YYBS.PLAYER_STATUS_FAN_MAIN4);
			}
		}
		
		
		//反主保留红二数据
		if (infoModel.mainInfo == null) {
			infoModel.mainInfo = new HashMap<>();
			infoModel.mainInfo.put(MainTwoRed, mainInfo.get(MainTwoRed));
		}else if(types == 1){
			infoModel.mainInfo.put(MainTwoRed, mainInfo.get(MainTwoRed));
		}
		
		if(effertypeList.size() > 0){
			effertypeList.add(SDHConstants_YYBS.PLAYER_STATUS_GIVE_UP_MAIN);
		}else{
			if(sets.size() > 0 ){
				effertypeList.add(SDHConstants_YYBS.PLAYER_STATUS_CANT_FAN_MAIN_KING);
			}
		}
		
		
		long type[] = new long[effertypeList.size()];
		for (int i = 0; i < effertypeList.size(); i++) {
			type[i] = effertypeList.get(i);
		}

		return type;
	}
	
	public void addColorMainBack(int color,List<Integer> list){
		infoModel.mainColorSet.add(color);
		if (infoModel.mainColorInfo == null) {
			infoModel.mainColorInfo = new HashMap<>();
		}
		infoModel.mainColorInfo.put(color, list);
	}
	
	public void addThreeMainBack(int key,List<Integer> list){
		if (infoModel.mainInfo == null) {
			infoModel.mainInfo = new HashMap<>();
		}
		
		infoModel.mainInfo.put(key, list);

		/*// 判断是否有三级4级
		for (Entry<Integer, List<Integer>> entry : infoModel.mainInfo.entrySet()) {
			int count = entry.getValue().size();
			switch (count) {
			case 3:
				infoModel.mianThree = true;
				break;
			case 4:
				infoModel.mianThree = true;
				infoModel.mainFour = true;
				break;

			}
		}*/
	}


	/**
	 * 获取能叫主的花色
	 * 
	 * @return the mainColorSet
	 */
	public Set<Integer> getMainColorSet(int type,final boolean disPatchCardEnd) {
		Set<Integer> rtMainColorSet = new HashSet<Integer>();
		boolean has_color_main = true;
		if(mainInfo == null){
			has_color_main = false;
		}
		if (has_color_main) {
			if(mainColorInfo == null){
				return rtMainColorSet;
			}
			mainColorInfo.forEach((color, list) -> {
				if (type == 1 && (mainInfo.get(MainTwoRed) == null || (mainInfo.get(MainTwoRed) != null && mainInfo.get(MainTwoRed).size() == 0))) { // 带二玩法的叫主
					if(list.size() == 1){
						return;
					}
				}else{
					if(list.size() == 1){
						list.forEach(card -> {
							if ((card & SDHConstants_XT.LOGIC_MASK_VALUE) == 10) {
								rtMainColorSet.add(color);
							}
						});
					}
				}
				if(disPatchCardEnd){
					list.forEach(card -> {
						if ((card & SDHConstants_XT.LOGIC_MASK_VALUE) == 10) {
							rtMainColorSet.add(color);
						}
					});
				}
			});
		}
		
		return rtMainColorSet;
	}
	
	/**
	 * 获取能反主的花色
	 * 
	 * @return the mainColorSet
	 */
	@Deprecated
	public Set<Integer> getMainColorSetFan(int type) {
		Set<Integer> rtMainColorSet = new HashSet<Integer>();
		boolean has_color_main = true;
		
		if (has_color_main) {
			mainColorInfo.forEach((color, list) -> {
				list.forEach(card -> {
					if ( getCardValue(card) == 10 ) {
						rtMainColorSet.add(color);
					}
				});
			});
		}
		if (type == 1 && (mainInfo.get(MainTwoRed) == null || mainInfo.get(MainTwoRed).size() == 0)) { // 带二玩法的叫主
			if(rtMainColorSet.size() == 1){
				rtMainColorSet.clear();
			}
		}
		return rtMainColorSet;
	}

	public List<Integer> getMainColor(int color){
		return mainColorInfo.get(color);
	}

	/**
	 * @param mainColorSet
	 *            the mainColorSet to set
	 */
	public void setMainColorSet(Set<Integer> mainColorSet) {
		this.mainColorSet = mainColorSet;
	}

	/**
	 * @return the mainColorInfo
	 */
	public Map<Integer, List<Integer>> getMainColorInfo() {
		return mainColorInfo;
	}

	/**
	 * @param mainColorInfo
	 *            the mainColorInfo to set
	 */
	public void setMainColorInfo(Map<Integer, List<Integer>> mainColorInfo) {
		this.mainColorInfo = mainColorInfo;
	}

	/**
	 * @return the mianThree
	 */
	public boolean isMianThree() {
		return mianThree;
	}

	/**
	 * @param mianThree
	 *            the mianThree to set
	 */
	public void setMianThree(boolean mianThree) {
		this.mianThree = mianThree;
	}

	/**
	 * @return the mainFour
	 */
	public boolean isMainFour() {
		return mainFour;
	}

	/**
	 * @param mainFour
	 *            the mainFour to set
	 */
	public void setMainFour(boolean mainFour) {
		this.mainFour = mainFour;
	}

	/**
	 * @return the mainInfo
	 */
	public Map<Integer, List<Integer>> getMainInfo() {
		return mainInfo;
	}

	/**
	 * @param mainInfo
	 *            the mainInfo to set
	 */
	public void setMainInfo(Map<Integer, List<Integer>> mainInfo) {
		this.mainInfo = mainInfo;
	}

	// 获取数值
	public int getCardValue(int card) {
		return card & SDHConstants_XT.LOGIC_MASK_VALUE;
	}

	// 获取花色
	public int getCardColor(int card) {
		return (card & SDHConstants_XT.LOGIC_MASK_COLOR) >> 4;
	}
	
	/**
	 * 选择红2
	 */
	public void choiceRedTwo(){
		List<Integer> red2Cards = mainInfo.get(MainTwoRed);
		if(red2Card == GameConstants.INVALID_CARD && mainInfo.size() != 0){
			red2Card = red2Cards.get(0);
		}
	}

	/**
	 * @return the red2Card
	 */
	public int getRed2Card() {
		return red2Card;
	}

	
	
	
	
	
}
