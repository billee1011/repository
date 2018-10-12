/**
 * 
 */
package com.cai.common.define;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.GameConstants;

/**
 * @author xwy
 * 游戏类型对应收费索引 游戏类型 描述
 *
 */
public enum SysGameTypeEnum {
	
	GAME_TYPE_ZZ(GameConstants.GAME_TYPE_ZZ,GameConstants.SYS_GAME_TYPE_ZZ,"转转",EGameType.MJ.getId(),new int[]{1010,1011,1012}),
	GAME_TYPE_CS(GameConstants.GAME_TYPE_CS,GameConstants.SYS_GAME_TYPE_CS,"长沙",EGameType.MJ.getId(),new int[]{1010,1011,1012}),
	GAME_TYPE_HZ(GameConstants.GAME_TYPE_HZ,GameConstants.SYS_GAME_TYPE_HZ,"红中",EGameType.MJ.getId(),new int[]{1010,1011,1012}),
	GAME_TYPE_SHUANGGUI(GameConstants.GAME_TYPE_SHUANGGUI,GameConstants.SYS_GAME_TYPE_SHUANGGUI,"双鬼",EGameType.MJ.getId(),new int[]{1010,1011,1012}),
	GAME_TYPE_ZHUZHOU(GameConstants.GAME_TYPE_ZHUZHOU,GameConstants.SYS_GAME_TYPE_ZHUZHOU,"株洲麻将",EGameType.MJ.getId(),new int[]{1010,1011,1012}),
	GAME_TYPE_CHENZHOU(GameConstants.GAME_TYPE_CHENZHOU,GameConstants.GAME_TYPE_CHENZHOU,"郴州麻将",EGameType.MJ.getId(),new int[]{1010,1011,1012}),
	GAME_TYPE_XTHH(GameConstants.GAME_TYPE_XTHH,GameConstants.SYS_GAME_TYPE_XTHH,"仙桃晃晃",EGameType.HBMJ.getId(),new int[]{1001,1002,1010,1011,1012}),
	GAME_TYPE_HENAN_AY(GameConstants.GAME_TYPE_HENAN_AY,GameConstants.SYS_GAME_TYPE_HENAN_AY,"河南安阳",EGameType.AY.getId(),new int[]{1010,1011,1012}),
	GAME_TYPE_HENAN_LZ(GameConstants.GAME_TYPE_HENAN_LZ,GameConstants.SYS_GAME_TYPE_HENAN_LZ,"河南林州",EGameType.AY.getId(),new int[]{1010,1011,1012}),
	GAME_TYPE_HENAN(GameConstants.GAME_TYPE_HENAN,GameConstants.SYS_GAME_TYPE_HENAN,"河南麻将",EGameType.AY.getId(),new int[]{1010,1011,1012}),
	GAME_TYPE_HENAN_HZ(GameConstants.GAME_TYPE_HENAN_HZ,GameConstants.SYS_GAME_TYPE_HENAN_HZ,"河南红中",EGameType.AY.getId(),new int[]{1010,1011,1012}),
	GAME_TYPE_HENAN_ZMD(GameConstants.GAME_TYPE_HENAN_ZMD,GameConstants.SYS_GAME_TYPE_HENAN_ZMD,"河南驻马店",EGameType.AY.getId(),new int[]{1010,1011,1012}),
	GAME_TYPE_HENAN_KF(GameConstants.GAME_TYPE_HENAN_KF,GameConstants.SYS_GAME_TYPE_HENAN_KF,"河南开封",EGameType.AY.getId(),new int[]{1010,1011,1012}),
	GAME_TYPE_HENAN_NY(GameConstants.GAME_TYPE_HENAN_NY,GameConstants.SYS_GAME_TYPE_HENAN_NY,"河南南阳",EGameType.AY.getId(),new int[]{1010,1011,1012}),
	GAME_TYPE_HENAN_XC(GameConstants.GAME_TYPE_HENAN_XC,GameConstants.SYS_GAME_TYPE_HENAN_XC,"河南许昌",EGameType.AY.getId(),new int[]{1010,1011,1012}),
	GAME_TYPE_HENAN_LYGC(GameConstants.GAME_TYPE_HENAN_LYGC,GameConstants.SYS_GAME_TYPE_HENAN_LYGC,"洛阳",EGameType.AY.getId(),new int[]{1010,1011,1012}),
	GAME_TYPE_HENAN_XX(GameConstants.GAME_TYPE_HENAN_XX,GameConstants.SYS_GAME_TYPE_HENAN_XX,"河南新乡",EGameType.AY.getId(),new int[]{1010,1011,1012}),
	GAME_TYPE_HENAN_XY(GameConstants.GAME_TYPE_HENAN_XY,GameConstants.SYS_GAME_TYPE_HENAN_XY,"河南信阳",EGameType.AY.getId(),new int[]{1010,1011,1012}),
	GAME_TYPE_HENAN_ZHUAN_ZHUAN(GameConstants.GAME_TYPE_HENAN_ZHUAN_ZHUAN,GameConstants.SYS_GAME_TYPE_HENAN_ZHUAN_ZHUAN,"河南转转",EGameType.AY.getId(),new int[]{1010,1011,1012}),
	GAME_TYPE_HENAN_ZHOU_KOU(GameConstants.GAME_TYPE_HENAN_ZHOU_KOU,GameConstants.SYS_GAME_TYPE_HENAN_ZHOU_KOU,"河南周口",EGameType.AY.getId(),new int[]{1010,1011,1012}),
	GAME_TYPE_HENAN_LUO_HE(GameConstants.GAME_TYPE_HENAN_LH,GameConstants.SYS_GAME_TYPE_HENAN_LUO_HE,"河南漯河",EGameType.AY.getId(),new int[]{1010,1011,1012}),
	GAME_TYPE_HENAN_SMX(GameConstants.GAME_TYPE_HENAN_SMX,GameConstants.SYS_GAME_TYPE_HENAN_SMX,"河南三门峡",EGameType.AY.getId(),new int[]{1010,1011,1012}),
	GAME_TYPE_HENAN_PDS(GameConstants.GAME_TYPE_HENAN_PDS,GameConstants.GAME_TYPE_HENAN_PDS,"河南平顶山",EGameType.AY.getId(),new int[]{1010,1011,1012}),
	
	GAME_TYPE_HU_NAN_CHANG_DE(GameConstants.GAME_TYPE_HU_NAN_CHANG_DE,GameConstants.GAME_TYPE_HU_NAN_CHANG_DE,"湖南常德",EGameType.MJ.getId(),new int[]{1010,1011,1012}),
	GAME_TYPE_HU_NAN_SHAOYANG(GameConstants.GAME_TYPE_HUNAN_SHAOYANG,GameConstants.SYS_GAME_TYPE_HU_NAN_SHAO_YANG,"湖南邵阳",EGameType.MJ.getId(),new int[]{1010,1011,1012}),
	GAME_TYPE_HU_NAN_YIYANG(GameConstants.GAME_TYPE_HUNAN_YIYANG,GameConstants.SYS_GAME_TYPE_HU_NAN_YI_YANG,"湖南益阳",EGameType.MJ.getId(),new int[]{1010,1011,1012}),
	
	GAME_TYPE_FLS_LX(GameConstants.GAME_TYPE_FLS_LX,GameConstants.SYS_GAME_TYPE_FLS_LX,"福禄寿",EGameType.FLS.getId(),new int[]{1010,1011,1012}),
	GAME_TYPE_FLS_LX_TWENTY(GameConstants.GAME_TYPE_FLS_LX_TWENTY,GameConstants.SYS_GAME_TYPE_FLS_LX_TWENTY,"福禄寿20张",EGameType.FLS.getId(),new int[]{1010,1011,1012}),
	GAME_TYPE_FLS_LX_DP(GameConstants.GAME_TYPE_FLS_LX_DP,GameConstants.SYS_GAME_TYPE_FLS_LX_DP,"福禄寿带p",EGameType.FLS.getId(),new int[]{1010,1011,1012}),
	GAME_TYPE_FLS_LX_CG(GameConstants.GAME_TYPE_FLS_LX_CG,GameConstants.SYS_GAME_TYPE_FLS_LX_CG,"临湘炒股",EGameType.FLS.getId(),new int[]{1010,1011,1012}),
	GAME_TYPE_FLS_CS_LX(GameConstants.GAME_TYPE_FLS_CS_LX,GameConstants.SYS_GAME_TYPE_LX_CS,"临湘麻将",EGameType.FLS.getId(),new int[]{1010,1011,1012}),
	GAME_TYPE_FLS_HZ_LX(GameConstants.GAME_TYPE_FLS_HZ_LX,GameConstants.SYS_GAME_TYPE_HZ,"临湘红中麻将",EGameType.FLS.getId(),new int[]{1010,1011,1012}),
	GAME_TYPE_SEVER_OX_LX(GameConstants.GAME_TYPE_SEVER_OX_LX,GameConstants.SYS_GAME_TYPE_SEVER_OX,"房主牛牛",EGameType.FLS.getId(),new int[]{1007,1008,1009}),
	GAME_TYPE_SZOX_LX(GameConstants.GAME_TYPE_SZOX_LX,GameConstants.SYS_GAME_TYPE_SZOX,"临湘牛牛上庄 ",EGameType.FLS.getId(),new int[]{1007,1008,1009}),
	GAME_TYPE_LZOX_LX(GameConstants.GAME_TYPE_LZOX_LX,GameConstants.SYS_GAME_TYPE_LZOX,"临湘轮流牛牛 ",EGameType.FLS.getId(),new int[]{1007,1008,1009}),
	GAME_TYPE_ZYQOX_LX(GameConstants.GAME_TYPE_ZYQOX_LX,GameConstants.SYS_GAME_TYPE_ZYQOX,"临湘自由牛牛 ",EGameType.FLS.getId(),new int[]{1007,1008,1009}),
	GAME_TYPE_MSZOX_LX(GameConstants.GAME_TYPE_MSZOX_LX,GameConstants.SYS_GAME_TYPE_MSZOX,"临湘明三张抢庄牛牛 ",EGameType.FLS.getId(),new int[]{1007,1008,1009}),
	GAME_TYPE_MFZOX_LX(GameConstants.GAME_TYPE_MFZOX_LX,GameConstants.SYS_GAME_TYPE_MFZOX,"临湘看四张抢庄牛牛 ",EGameType.FLS.getId(),new int[]{1007,1008,1009}),
	GAME_TYPE_TBOX_LX(GameConstants.GAME_TYPE_TBOX_LX,GameConstants.SYS_GAME_TYPE_TBOX,"临湘通比牛牛 ",EGameType.FLS.getId(),new int[]{1007,1008,1009}),
	GAME_TYPE_BTZ_YY(GameConstants.GAME_TYPE_BTZ_YY,GameConstants.SYS_GAME_TYPE_BTZ_YY,"扳坨子 ",EGameType.FLS.getId(),new int[]{1007,1008,1009}),	
	GAME_TYPE_HH_YX(GameConstants.GAME_TYPE_HH_YX,GameConstants.SYS_GAME_TYPE_YX_HH,"攸县红黑胡",EGameType.PHZ.getId(),new int[]{1010,1011,1012}),
	GAME_TYPE_PHZ_YX(GameConstants.GAME_TYPE_PHZ_YX,GameConstants.SYS_GAME_TYPE_YX_PHZ,"攸县跑胡子",EGameType.PHZYX.getId(),new int[]{1010,1011,1012}),
	GAME_TYPE_FPHZ_YX(GameConstants.GAME_TYPE_FPHZ_YX,GameConstants.SYS_GAME_TYPE_YX_FPHZ,"攸县跑胡子15张",EGameType.FPHZ.getId(),new int[]{1010,1011,1012}),
	GAME_TYPE_PHZ_CHD(GameConstants.GAME_TYPE_PHZ_CHD,GameConstants.SYS_GAME_TYPE_PHZ_CHD,"常德跑胡子",EGameType.PHZCD.getId(),new int[]{1010,1011,1012}),
	GAME_TYPE_LHQ_HD(GameConstants.GAME_TYPE_LHQ_HD,GameConstants.SYS_GAME_TYPE_LHQ_HD,"衡东六胡抢",EGameType.LHQHD.getId(),new int[]{1010,1011,1012}),
	GAME_TYPE_THK_HY(GameConstants.GAME_TYPE_THK_HY,GameConstants.SYS_GAME_TYPE_THK_HY,"衡阳十胡卡",EGameType.THKHY.getId(),new int[]{1010,1011,1012}),
	GAME_TYPE_WMQ_AX(GameConstants.GAME_TYPE_WMQ_AX,GameConstants.SYS_GAME_TYPE_WMQ_AX,"衡阳十胡卡",EGameType.THKHY.getId(),new int[]{1010,1011,1012}),
	GAME_TYPE_PHZ_XT(GameConstants.GAME_TYPE_PHZ_XT,GameConstants.SYS_GAME_TYPE_PHZ_XT,"湘谭跑胡子",EGameType.PHZXT.getId(),new int[]{1001,1002,1010,1011,1012}),
	GAME_TYPE_SEVER_OX(GameConstants.GAME_TYPE_SEVER_OX,GameConstants.SYS_GAME_TYPE_SEVER_OX,"房主牛牛",EGameType.NIUNIU.getId(),new int[]{1007,1008,1009}),
	GAME_TYPE_SZOX(GameConstants.GAME_TYPE_SZOX,GameConstants.SYS_GAME_TYPE_SZOX,"牛牛上庄 ",EGameType.NIUNIU.getId(),new int[]{1007,1008,1009}),
	GAME_TYPE_LZOX(GameConstants.GAME_TYPE_LZOX,GameConstants.SYS_GAME_TYPE_LZOX,"轮流牛牛 ",EGameType.NIUNIU.getId(),new int[]{1007,1008,1009}),
	GAME_TYPE_ZYQOX(GameConstants.GAME_TYPE_ZYQOX,GameConstants.SYS_GAME_TYPE_ZYQOX,"自由牛牛 ",EGameType.NIUNIU.getId(),new int[]{1007,1008,1009}),
	GAME_TYPE_MSZOX(GameConstants.GAME_TYPE_MSZOX,GameConstants.SYS_GAME_TYPE_MSZOX,"明三张抢庄牛牛 ",EGameType.NIUNIU.getId(),new int[]{1007,1008,1009}),
	GAME_TYPE_MFZOX(GameConstants.GAME_TYPE_MFZOX,GameConstants.SYS_GAME_TYPE_MFZOX,"看四张抢庄牛牛 ",EGameType.NIUNIU.getId(),new int[]{1007,1008,1009}),
	GAME_TYPE_TBOX(GameConstants.GAME_TYPE_TBOX,GameConstants.SYS_GAME_TYPE_TBOX,"通比牛牛 ",EGameType.NIUNIU.getId(),new int[]{1007,1008,1009}),
	GAME_TYPE_HJK(GameConstants.GAME_TYPE_HJK,GameConstants.SYS_GAME_TYPE_HJK,"21点 ",EGameType.HJK.getId(),new int[]{1003,1004}),
	GAME_TYPE_PDK_JD(GameConstants.GAME_TYPE_PDK_JD,GameConstants.SYS_GAME_TYPE_PDK_JD,"经典跑得快 ",EGameType.PDK.getId(),new int[]{1003,1004}),
	GAME_TYPE_PDK_FP(GameConstants.GAME_TYPE_PDK_FP,GameConstants.SYS_GAME_TYPE_PDK_JD,"4人跑得快",EGameType.PDK.getId(),new int[]{1003,1004}),
	GAME_TYPE_PDK_LZ(GameConstants.GAME_TYPE_PDK_LZ,GameConstants.SYS_GAME_TYPE_PDK_JD,"癞子跑得快",EGameType.PDK.getId(),new int[]{1003,1004}),
	GAME_TYPE_PDK_YW(GameConstants.GAME_TYPE_PDK_SW,GameConstants.SYS_GAME_TYPE_PDK_JD,"15张跑得快 ",EGameType.PDK.getId(),new int[]{1003,1004}),
	GAME_TYPE_DDZ_JD(GameConstants.GAME_TYPE_DDZ_JD,GameConstants.SYS_GAME_TYPE_DDZ_JD,"经典斗地主 ",EGameType.AY.getId(),new int[]{1011,1012}),
	GAME_TYPE_ZJH_JD(GameConstants.GAME_TYPE_ZJH_JD,GameConstants.SYS_GAME_TYPE_ZJH_JD,"炸金花",EGameType.ZJH.getId(),new int[]{1011,1012})
	;

	private int game_type_index;

	private String desc;

	/**
	 * 收费索引
	 */
	private int gold_type;
	
	/**
	 * 游戏ID
	 */
	private int gameID;
	
	
	/**
	 * 收费的id索引  
	 */
	private int[] goldIndex;
	/**
	 * 收费有几种方式
	 */
	private int  paymode;
	

	private final static Map<Integer, Integer> map = new HashMap<Integer, Integer>();
	
	private final static Map<Integer, Integer> gameIdMap = new HashMap<Integer, Integer>();
	
	
	private final static Map<Integer, String> gameTypeDescMap = new HashMap<Integer, String>();
	
	
	private final static Map<Integer, int[]> gameTypeIndexToGoldIndex = new HashMap<Integer, int[]>();

	public static final Logger logger = LoggerFactory.getLogger(SysGameTypeEnum.class);

	SysGameTypeEnum(int game_type_index, int gold_type, String desc,int gameID,int[] goldIndex) {
		this.game_type_index = game_type_index;
		this.gold_type = gold_type;
		this.desc = desc;
		this.gameID = gameID;
		this.goldIndex = goldIndex;
		
	}

	static {
		for (SysGameTypeEnum c : SysGameTypeEnum.values()) {
			Integer type = map.get(c.getGame_type_index());
			if (type != null) {
				System.exit(-1);
				logger.error("SysGameTypeEnum定义了相同类型");
			}
			map.put(c.getGame_type_index(), c.getGold_type());
			gameIdMap.put(c.getGame_type_index(), c.getGameID());
			gameTypeDescMap.put(c.getGame_type_index(), c.getDesc());
			gameTypeIndexToGoldIndex.put(c.getGame_type_index(),c.getGoldIndex());
		}
	}

	/**
	 * 获取游戏收费索引--用于判断是否开放
	 * @param game_type_index
	 * @return
	 */
	public static Integer getGameGoldTypeIndex(int game_type_index) {
		return map.get(game_type_index);
	}
	
	
	/**
	 * 获取收费的索引 --根据局数判断 
	 * @param game_type_index
	 * @return
	 */
	public static int[] getGoldIndexByTypeIndex(int game_type_index) {
		return gameTypeIndexToGoldIndex.get(game_type_index);
	}
	
	/**
	 * 获取游戏ID--根据typeIndex
	 * @param game_type_index
	 * @return
	 */
	public static Integer getGameIDByTypeIndex(int game_type_index) {
		return gameIdMap.get(game_type_index);
	}

	public int getGame_type_index() {
		return game_type_index;
	}

	public void setGame_type_index(int game_type_index) {
		this.game_type_index = game_type_index;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public int getGold_type() {
		return gold_type;
	}

	public void setGold_type(int gold_type) {
		this.gold_type = gold_type;
	}

	public int getGameID() {
		return gameID;
	}

	public void setGameID(int gameID) {
		this.gameID = gameID;
	}
	
	
	
	
	//麻将小类型
	public static String getMJname(int v2) {
		return gameTypeDescMap.get(v2);
	}


	public int[] getGoldIndex() {
		return goldIndex;
	}


	public void setGoldIndex(int[] goldIndex) {
		this.goldIndex = goldIndex;
	}

}
