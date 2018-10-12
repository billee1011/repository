package com.cai.game.mj;

import java.util.HashMap;
import java.util.Map;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.game.Constants_CouYiSe;
import com.cai.common.constant.game.Constants_EZ;
import com.cai.common.constant.game.Constants_HuaXian;
import com.cai.common.constant.game.Constants_HuangShi;
import com.cai.common.constant.game.Constants_HuangZhou;
import com.cai.common.constant.game.Constants_JingDian_CS;
import com.cai.common.constant.game.Constants_LX_CS_QY;
import com.cai.common.constant.game.Constants_YangZhong;
import com.cai.common.constant.game.GameConstants_HanShouWang;
import com.cai.common.constant.game.GameConstants_JZ;
import com.cai.common.constant.game.GameConstants_KLDS;
import com.cai.common.constant.game.GameConstants_KWX;
import com.cai.common.constant.game.GameConstants_SXJY;
import com.cai.common.constant.game.GameConstants_TC;
import com.cai.common.constant.game.mj.Constants_HuangShan;
import com.cai.common.constant.game.mj.Constants_MJ_QUAN_ZHOU;
import com.cai.common.constant.game.mj.Constants_MJ_SXHD;
import com.cai.common.constant.game.mj.Constants_TaoJiang;
import com.cai.common.constant.game.mj.GameConstants_HaiNan;
import com.cai.common.constant.game.mj.GameConstants_ND;
import com.cai.common.constant.game.mj.GameConstants_RUIJIN;
import com.cai.common.constant.game.mj.GameConstants_SXLF;
import com.cai.common.constant.game.mj.GameConstants_SXLX;
import com.cai.common.constant.game.mj.GameConstants_SXNW;
import com.cai.common.constant.game.mj.GameConstants_TIAOTIAO;
import com.cai.common.constant.game.mj.GameConstants_WEIHE;
import com.cai.common.constant.game.mj.GameConstants_XTDGK;
import com.cai.game.mj.chenchuang.dalianqionghu.Table_DaLianQiongHu;
import com.cai.game.mj.chenchuang.guangchang.Table_GuangChang;
import com.cai.game.mj.chenchuang.hlkdd.Table_HLKDD;
import com.cai.game.mj.chenchuang.huaihua.Table_HUAI_HUA;
import com.cai.game.mj.chenchuang.huarong.Table_HuaRong;
import com.cai.game.mj.chenchuang.jingdezhen.Table_JingDeZhen;
import com.cai.game.mj.chenchuang.ningxinag.Table_NING_XIANG;
import com.cai.game.mj.chenchuang.pc.Table_PuCheng;
import com.cai.game.mj.chenchuang.pingjiang.Table_PingJiang;
import com.cai.game.mj.chenchuang.pingxiang258.Table_PING_XIANG;
import com.cai.game.mj.chenchuang.quanzhou.Table_QuanZhou;
import com.cai.game.mj.chenchuang.shanxituidaohu.Table_Shan_Xi_TDH;
import com.cai.game.mj.chenchuang.wanzai.Table_WanZai;
import com.cai.game.mj.chenchuang.xian.Table_XiAn;
import com.cai.game.mj.chenchuang.xianning.Table_XianNing;
import com.cai.game.mj.chenchuang.xinyang.Table_XinYang;
import com.cai.game.mj.chenchuang.yongzhou.Table_Yong_Zhou;
import com.cai.game.mj.chenchuang.zptdh.Table_ZPTDH;
import com.cai.game.mj.fujianmj.MJTable_FuJian_GuangZe;
import com.cai.game.mj.guangxi.liuzhou.Table_GXLZ;
import com.cai.game.mj.guangxi.nanning.Table_NanNing;
import com.cai.game.mj.gzcg.Table_GZCG;
import com.cai.game.mj.hainan.hainanmj.MJTable_HaiNan;
import com.cai.game.mj.handler.gdhundred.Table_GDHundred;
import com.cai.game.mj.handler.henanpy.MJTable_PY;
import com.cai.game.mj.handler.henansmx.MJTable_SMX;
import com.cai.game.mj.handler.hunanchangde.MJTable_HuNan_ChangDe;
import com.cai.game.mj.handler.hunanhengyang.MJTable_HY;
import com.cai.game.mj.handler.hunanliling.MJTable_LL;
import com.cai.game.mj.handler.hunanshaoyan.MJTable_ShaoYang;
import com.cai.game.mj.handler.jsjh.MJTable_JiangSu_JH;
import com.cai.game.mj.handler.jszz.MJTable_JangSu_ZZ;
import com.cai.game.mj.handler.lilingzz.MJTable_LILINGZZ;
import com.cai.game.mj.handler.moyanggui.MjTableMoYangGui;
import com.cai.game.mj.handler.shangqiu.MJTable_ShangQiu;
import com.cai.game.mj.handler.shanxill.MJTable_LVlIANG;
import com.cai.game.mj.handler.xupumj.MJTable_XUPUMJ;
import com.cai.game.mj.handler.yifeng.Table_YiFeng;
import com.cai.game.mj.handler.yiyang.MJTable_YiYang;
import com.cai.game.mj.handler.yiyang.qinyou.MJTable_YiYang_QY;
import com.cai.game.mj.handler.yiyang.szg.MJTable_YiYang_SZG;
import com.cai.game.mj.handler.yongzhou.MJTable_YongZhou;
import com.cai.game.mj.handler.yytdh.MJTable_YYTDH;
import com.cai.game.mj.handler.yyzxz.MJTable_YYZXZ;
import com.cai.game.mj.handler.zhuzhouzz.MJTable_ZHUZHOUZZ;
import com.cai.game.mj.henan.huaxian.Table_HuaXian;
import com.cai.game.mj.henan.huojia.Table_HuoJia;
import com.cai.game.mj.henan.jiaozuo.MJTable_JZ;
import com.cai.game.mj.henan.jiyuan.Table_JiYuan;
import com.cai.game.mj.henan.kfzz.MJTable_KFZZ;
import com.cai.game.mj.henan.kulongdaishen.MJTable_KLDS;
import com.cai.game.mj.henan.newzhenzhou.MJTable_New_ZhenZhou;
import com.cai.game.mj.henan.sanmenxia.MJTable_SanMenXia;
import com.cai.game.mj.henan.wuzhi.Table_WuZhi;
import com.cai.game.mj.henan.zhengzhou.MJTable_ZhengZhou;
import com.cai.game.mj.huangshan.tunxi.Table_HuangShan;
import com.cai.game.mj.hubei.couyise.Table_CouYiSe;
import com.cai.game.mj.hubei.ezhou.Table_EZ;
import com.cai.game.mj.hubei.hsw.Table_HSW;
import com.cai.game.mj.hubei.huangshi.Table_HuangShi;
import com.cai.game.mj.hubei.huangzhou.Table_HuangZhou;
import com.cai.game.mj.hubei.hzlzg.Table_HZ;
import com.cai.game.mj.hunan.anhua.Table_AnHua;
import com.cai.game.mj.hunan.chenzhou.MJTable_HuNan_ChenZhou;
import com.cai.game.mj.hunan.hengyang258.MJTable_HY258;
import com.cai.game.mj.hunan.jingdiancs.Table_JingDian_CS;
import com.cai.game.mj.hunan.jingdiancs.lxqinyou.Table_LX_CS_QY;
import com.cai.game.mj.hunan.jingdiancs.qinyou.Table_JingDian_CS_QY;
import com.cai.game.mj.hunan.lilingtuhaoban.MJTable_LLTH;
import com.cai.game.mj.hunan.new_xiang_tan.MJTable_HuNan_XiangTan;
import com.cai.game.mj.hunan.syhz.MJTable_SYHZ;
import com.cai.game.mj.hunan.taojiang.Table_TaoJiang;
import com.cai.game.mj.hunan.yuanjiang.Table_YuanJiang;
import com.cai.game.mj.jiangsu.yangzhong.Table_YangZhong;
import com.cai.game.mj.jiangxi.leping.Table_LePing;
import com.cai.game.mj.jiangxi.ningdu.MJTable_ND;
import com.cai.game.mj.jiangxi.pxzz.MJTable_PXZZ;
import com.cai.game.mj.jiangxi.ruijin.MJTable_RUIJIN;
import com.cai.game.mj.jiangxi.tiaotiao.MJTable_TT;
import com.cai.game.mj.jiangxi.yudu.GameConstants_YD;
import com.cai.game.mj.jiangxi.yudu.MJTable_YD;
import com.cai.game.mj.jilin.chuangchun.MjTable_ChangChun;
import com.cai.game.mj.jilin.songyuan.MjTable_SongYuan;
import com.cai.game.mj.ningxia.MJTable_NingXia_HuaShui;
import com.cai.game.mj.shanxi.hongdong.MJTable_HongDong;
import com.cai.game.mj.shanxi.jingyue.MJTable_ShanXi_JINGYUE;
import com.cai.game.mj.shanxi.jixian.Table_JiXian;
import com.cai.game.mj.shanxi.koudian.MJTable_KouDian;
import com.cai.game.mj.shanxi.lanxian.MJTable_ShanXi_LANXIAN;
import com.cai.game.mj.shanxi.linfeng.Table_LinFeng;
import com.cai.game.mj.shanxi.loufan.MJTable_ShanXi_LOUFAN;
import com.cai.game.mj.shanxi.ningwu.MJTable_ShanXi_NINGWU;
import com.cai.game.mj.shanxi.sxkdd.MJTable_SXKDD;
import com.cai.game.mj.shanxi.weinan.MJTable_WEIHE;
import com.cai.game.mj.shanxi.xiangning.MJTable_XN;
import com.cai.game.mj.shanxi.xiangyuan.MJTable_ShanXi_XiangYuan;
import com.cai.game.mj.shanxi.yimenpai.Table_YiMenPai;
import com.cai.game.mj.shanximj.Table_ShanXi;
import com.cai.game.mj.shanximj.sxhs.MJTable_ShanXi_HuaShui;
import com.cai.game.mj.sichuan.guangan.Table_GuangAn;
import com.cai.game.mj.sichuan.leshan.Table_LeShan;
import com.cai.game.mj.sichuan.luzhougui.Table_LuZhouGui;
import com.cai.game.mj.sichuan.qionglai.Table_QiongLai;
import com.cai.game.mj.sichuan.sanrenliangfang.Table_srlf;
import com.cai.game.mj.sichuan.xueliuchenghe.Table_XueLiuChengHe;
import com.cai.game.mj.sichuan.xuezhandaodi.Table_xzdd;
import com.cai.game.mj.wuyuanmj.Table_WuYuan;
import com.cai.game.mj.xtdgk.MJTable_XTDGK;
import com.cai.game.mj.yu.bao_ding.Table_BD;
import com.cai.game.mj.yu.gd_huizhou.Table_HuiZhou;
import com.cai.game.mj.yu.gd_tdh.Table_TDH;
import com.cai.game.mj.yu.gd_tdh.td.Table_TDH_3D;
import com.cai.game.mj.yu.gui_yang.Table_GY;
import com.cai.game.mj.yu.kwx.Table_KWX;
import com.cai.game.mj.yu.kwx.TwoD.Table_KWX_2D;
import com.cai.game.mj.yu.kwx.TwoD.xg.Table_KWX_XG_2D;
import com.cai.game.mj.yu.kwx.xg.Table_KWX_XG;
import com.cai.game.mj.yu.lu_he.Table_LUHE;
import com.cai.game.mj.yu.shan_wei.Table_SW;
import com.cai.game.mj.yu.tong_cheng.Table_TC;

public enum MJType {
	DEFAULT(-1) {
		@Override
		public AbstractMJTable createTable() {
			return new MJTable();
		}
	},

	GAME_TYPE_XUE_LIU_CHENG_HE(GameConstants.GAME_TYPE_XUE_LIU_CHENG_HE) {
		@Override
		public AbstractMJTable createTable() {
			return new Table_XueLiuChengHe(this);
		}

		@Override
		public int[] getCards() {
			return MJConstants.CARD_DATA_WAN_TIAO_TONG;
		}
	},

	GAME_TYPE_LU_ZHOU_GUI(GameConstants.GAME_TYPE_LU_ZHOU_GUI) {
		@Override
		public AbstractMJTable createTable() {
			return new Table_LuZhouGui(this);
		}
	},

	GAME_TYPE_LE_SHAN_YAO_JI(GameConstants.GAME_TYPE_LE_SHAN_YAO_JI) {
		@Override
		public AbstractMJTable createTable() {
			return new Table_LeShan(this);
		}
	},

	GAME_TYPE_GUANG_AN(GameConstants.GAME_TYPE_GUANG_AN) {
		@Override
		public AbstractMJTable createTable() {
			return new Table_GuangAn(this);
		}
	},

	GAME_TYPE_LIN_FENG(GameConstants.GAME_TYPE_LIN_FENG) {
		@Override
		public AbstractMJTable createTable() {
			return new Table_LinFeng(this);
		}
	},

	GAME_TYPE_LF_YI_MEN_PAI(GameConstants.GAME_TYPE_LF_YI_MEN_PAI) {
		@Override
		public AbstractMJTable createTable() {
			return new Table_YiMenPai(this);
		}
	},

	GAME_TYPE_MJ_ZHENG_ZHOU(GameConstants.GAME_TYPE_MJ_ZHENG_ZHOU) {
		@Override
		public AbstractMJTable createTable() {
			return new MJTable_ZhengZhou();
		}
	},

	GAME_TYPE_KFZZ(GameConstants.GAME_TYPE_KFZZ) {
		@Override
		public AbstractMJTable createTable() {
			return new MJTable_KFZZ();
		}
	},

	GAME_TYPE_MJ_NEW_ZHEN_ZHOU(GameConstants.GAME_TYPE_MJ_NEW_HN_ZHEN_ZHOU) {
		@Override
		public AbstractMJTable createTable() {
			return new MJTable_New_ZhenZhou();
		}
	},

	GAME_TYPE_HENAN_WU_ZHI(GameConstants.GAME_TYPE_HENAN_WU_ZHI) {
		@Override
		public AbstractMJTable createTable() {
			return new Table_WuZhi();
		}
	},

	GAME_TYPE_MJ_YUAN_JIANG(GameConstants.GAME_TYPE_MJ_YUAN_JIANG) {
		@Override
		public AbstractMJTable createTable() {
			return new Table_YuanJiang(this);
		}

		@Override
		public int[] getCards() {
			return MJConstants.CARD_DATA_WAN_TIAO_TONG;
		}
	},

	GAME_TYPE_MJ_SRLF(GameConstants.GAME_TYPE_MJ_SRLF) {
		@Override
		public AbstractMJTable createTable() {
			return new Table_srlf(this);
		}

		@Override
		public int[] getCards() {
			return MJConstants.CARD_DATA_TIAO_TONG;
		}
	},

	GAME_TYPE_THJ_YI_YANG(GameConstants.GAME_TYPE_THJ_YI_YANG) {
		@Override
		public AbstractMJTable createTable() {
			return new MJTable_YiYang();
		}

		@Override
		public int[] getCards() {
			return MJConstants.CARD_DATA_WAN_TIAO_TONG;
		}
	},

	GAME_TYPE_THJ_YI_YANG_SZG(GameConstants.GAME_TYPE_THJ_YI_YANG_SGZ) {
		@Override
		public AbstractMJTable createTable() {
			return new MJTable_YiYang_SZG();
		}

		@Override
		public int[] getCards() {
			return MJConstants.CARD_DATA_WAN_TIAO_TONG;
		}
	},

	GAME_TYPE_MJ_JI_XIAN(GameConstants.GAME_TYPE_MJ_JI_XIAN) {
		@Override
		public AbstractMJTable createTable() {
			return new Table_JiXian(this);
		}

		@Override
		public int[] getCards() {
			return GameConstants.CARD_DATA_DAI_FENG;
		}
	},

	GAME_TYPE_THJ_CS(GameConstants.GAME_TYPE_THJ_CS) {
		@Override
		public AbstractMJTable createTable() {
			return new MJTable();
		}
	},

	GAME_TYPE_THJ_JD_CS(GameConstants.GAME_TYPE_THJ_JD_CS) {
		@Override
		public AbstractMJTable createTable() {
			return new Table_JingDian_CS();
		}

		@Override
		public int[] getCards() {
			return Constants_JingDian_CS.CARD_DATA;
		}
	},

	GAME_TYPE_MJ_DT_SHA_MA(GameConstants.GAME_TYPE_MJ_DT_SHA_MA) {
		@Override
		public AbstractMJTable createTable() {
			return new Table_JingDian_CS();
		}

		@Override
		public int[] getCards() {
			return Constants_JingDian_CS.CARD_DATA;
		}
	},

	GAME_TYPE_QIONG_LAI(GameConstants.GAME_TYPE_QIONG_LAI) {
		@Override
		public AbstractMJTable createTable() {
			return new Table_QiongLai(this);
		}

		@Override
		public int[] getCards() {
			return GameConstants.CARD_DATA_WAN_TIAO_TONG;
		}
	},

	GAME_TYPE_MJ_AN_HUA(GameConstants.GAME_TYPE_MJ_AN_HUA) {
		@Override
		public AbstractMJTable createTable() {
			return new Table_AnHua(this);
		}

		@Override
		public int[] getCards() {
			return GameConstants.CARD_DATA_WAN_TIAO_TONG;
		}
	},

	// 岳阳抓虾子
	GAME_TYPE_YYZXZ(GameConstants.GAME_TYPE_YUE_YANG_ZHUA_XIA_ZI) {
		@Override
		public int[] getCards() {
			return MJConstants.CARD_DATA_BU_DAI_FENG_YYZXZ;
		}

		@Override
		public AbstractMJTable createTable() {
			return new MJTable_YYZXZ(this);
		}

		// 3人麻将
		@Override
		public int getPlayerCount() {
			return GameConstants.GAME_PLAYER_HH;
		}

	},
	// 岳阳抓虾子(大厅)
	GAME_TYPE_YYZXZ_DT(GameConstants.GAME_TYPE_YUE_YANG_ZHUA_XIA_ZI_DT) {
		@Override
		public int[] getCards() {
			return MJConstants.CARD_DATA_BU_DAI_FENG_YYZXZ;
		}

		@Override
		public AbstractMJTable createTable() {
			return new MJTable_YYZXZ(this);
		}

		// 3人麻将
		@Override
		public int getPlayerCount() {
			return GameConstants.GAME_PLAYER_HH;
		}

	},

	// 三门峡麻将
	GAME_TYPE_HENNAN_SMX(GameConstants.GAME_TYPE_HENAN_SMX) {

		@Override
		public AbstractMJTable createTable() {
			return new MJTable_SMX();
		}
	},
	// 江苏镇江
	GAME_TYPE_JIANGSU_ZZ(GameConstants.GAME_TYPE_JIANGSU_ZHENJIANG) {

		@Override
		public AbstractMJTable createTable() {
			return new MJTable_JangSu_ZZ();
		}
	},
	// 濮阳麻将
	GAME_TYPE_HENANPY(GameConstants.GAME_TYPE_MJ_PU_YANG) {

		@Override
		public AbstractMJTable createTable() {
			return new MJTable_PY();
		}
	},

	// 濮阳麻将
	GAME_TYPE_NEW_PU_YANG(GameConstants.GAME_TYPE_NEW_PU_YANG) {

		@Override
		public AbstractMJTable createTable() {
			return new MJTable_PY();
		}
	},

	// 湖南邵阳麻将
	GAME_TYPE_HUNAN_SHAOYANG(GameConstants.GAME_TYPE_HUNAN_SHAOYANG) {
		@Override
		public AbstractMJTable createTable() {
			return new MJTable_ShaoYang();
		}
	},

	// 湖南麻将
	GAME_TYPE_HUNAN_YIYANG(GameConstants.GAME_TYPE_HUNAN_YIYANG) {
		@Override
		public AbstractMJTable createTable() {
			return new MJTable_YiYang();
		}

		@Override
		public int[] getCards() {
			return MJConstants.CARD_DATA_WAN_TIAO_TONG;
		}
	},

	// 湖南梭子杠
	GAME_TYPE_HUNAN_YIYANG_SZG(GameConstants.GAME_TYPE_HUNAN_YIYANG_SZG) {
		@Override
		public AbstractMJTable createTable() {
			return new MJTable_YiYang_SZG();
		}

		@Override
		public int[] getCards() {
			return MJConstants.CARD_DATA_WAN_TIAO_TONG;
		}
	},

	// 湖南永州麻将
	GAME_TYPE_HUNAN_YONGZHOU(GameConstants.GAME_TYPE_HUNAN_YONGZHOU) {
		@Override
		public AbstractMJTable createTable() {
			return new MJTable_YongZhou();
		}
	},
	// 湖南新永州麻将
	GAME_TYPE_MJ_YZ(GameConstants.GAME_TYPE_MJ_YZ) {
		@Override
		public AbstractMJTable createTable() {
			return new Table_Yong_Zhou();
		}
	},

	// 岳阳推到胡
	GAME_TYPE_YUEYANG_TUIDAOHU(GameConstants.GAME_TYPE_YUE_YANG_TDH) {
		@Override
		public AbstractMJTable createTable() {
			return new MJTable_YYTDH(this);
		}

		@Override
		public int[] getCards() {
			return MJConstants.CARD_DATA_WAN_TIAO_TONG;
		}
	},

	// 岳阳推到胡(大厅)
	GAME_TYPE_YUEYANG_TUIDAOHU_DT(GameConstants.GAME_TYPE_YUE_YANG_TDH_DT) {
		@Override
		public AbstractMJTable createTable() {
			return new MJTable_YYTDH(GAME_TYPE_YUEYANG_TUIDAOHU);
		}

		@Override
		public int[] getCards() {
			return MJConstants.CARD_DATA_WAN_TIAO_TONG;
		}
	},
	// 海南麻将
	GAME_TYPE_MJ_HAI_NAN(GameConstants.GAME_TYPE_MJ_HAI_NAN) {
		@Override
		public AbstractMJTable createTable() {
			return new MJTable_HaiNan();
		}

		@Override
		public int[] getCards() {
			return GameConstants_HaiNan.CARD_DATA_MAX;
		}
	},

	// 湖南醴陵麻将
	GAME_TYPE_HUNAN_LILING_TYPE(GameConstants.GAME_TYPE_HUNAN_LILING) {
		@Override
		public AbstractMJTable createTable() {
			return new MJTable_LL();
		}

		@Override
		public int[] getCards() {
			return MJConstants.CARD_DATA_WAN_TIAO_TONG;
		}
	},

	// 湖南醴陵麻将土豪版
	GAME_TYPE_HUNAN_LILING_TH_TYPE(GameConstants.GAME_TYPE_MJ_LI_LLING_TH) {
		@Override
		public AbstractMJTable createTable() {
			return new MJTable_LLTH();
		}

		@Override
		public int[] getCards() {
			return MJConstants.CARD_DATA_WAN_TIAO_TONG;
		}
	},

	// 湖南衡阳
	GAME_TYPE_HUNAN_HENGYANG(GameConstants.GAME_TYPE_HUNAN_HENGYANG) {
		@Override
		public AbstractMJTable createTable() {
			return new MJTable_HY();
		}

		@Override
		public int[] getCards() {
			return MJConstants.CARD_DATA_WAN_TIAO_TONG;
		}
	},

	// 湖南衡阳258
	GAME_TYPE_HUNAN_HENGYANG258(GameConstants.GAME_TYPE_HENG_YANG_258) {
		@Override
		public AbstractMJTable createTable() {
			return new MJTable_HY258();
		}

		@Override
		public int[] getCards() {
			return MJConstants.CARD_DATA_WAN_TIAO_TONG;
		}
	},

	GAME_TYPE_HUNAN_CHANGDE(GameConstants.GAME_TYPE_HU_NAN_CHANG_DE) {
		@Override
		public AbstractMJTable createTable() {
			return new MJTable_HuNan_ChangDe(this);
		}
	},

	GAME_TYPE_MJ_DT_HONG_ZHONG(GameConstants.GAME_TYPE_MJ_DT_HONG_ZHONG) {
		@Override
		public AbstractMJTable createTable() {
			return new MJTable_HuNan_ChangDe(this);
		}
	},

	GAME_TYPE_HUNAN_CHANGDE_DT(GameConstants.GAME_TYPE_HUNAN_MJ_CD_DT) {
		@Override
		public AbstractMJTable createTable() {
			return new MJTable_HuNan_ChangDe(this);
		}
	},

	// 湖南郴州麻将
	GAME_TYPE_HUNAN_CHEN_ZHOU(GameConstants.GAME_TYPE_CHENZHOU) {
		@Override
		public AbstractMJTable createTable() {
			return new MJTable_HuNan_ChenZhou(MJType.GAME_TYPE_HUNAN_CHEN_ZHOU);
		}
	},

	GAME_TYPE_DT_MJ_HUNAN_CHEN_ZHOU(GameConstants.GAME_TYPE_DT_MJ_HUNAN_CHEN_ZHOU) {
		@Override
		public AbstractMJTable createTable() {
			return new MJTable_HuNan_ChenZhou(MJType.GAME_TYPE_DT_MJ_HUNAN_CHEN_ZHOU);
		}
	},

	// 湖南湘潭麻将
	GAME_TYPE_HUNAN_XIANG_TAN(GameConstants.GAME_TYPE_HUNAN_XIANG_TAN) {
		@Override
		public AbstractMJTable createTable() {
			return new MJTable_HuNan_XiangTan(this);
		}

		@Override
		public int[] getCards() {
			return GameConstants.CARD_DATA_WAN_TIAO_TONG;
		}
	},

	// 湖南湘潭麻将(大厅)
	GAME_TYPE_HUNAN_XIANG_TAN_DT(GameConstants.GAME_TYPE_HUNAN_XIANG_TAN_DT) {
		@Override
		public AbstractMJTable createTable() {
			return new MJTable_HuNan_XiangTan(this);
		}

		@Override
		public int[] getCards() {
			return GameConstants.CARD_DATA_WAN_TIAO_TONG;
		}
	},

	// 广西柳州麻将
	GAME_TYPE_GX_LIU_ZHOU(GameConstants.GAME_TYPE_GX_LIU_ZHOU) {
		@Override
		public AbstractMJTable createTable() {
			return new Table_GXLZ();
		}

		@Override
		public int[] getCards() {
			return GameConstants.CARD_DATA_DAI_FENG;
		}
	},

	// 河南济源麻将
	GAME_TYPE_MJ_JI_YUAN(GameConstants.GAME_TYPE_MJ_JI_YUAN) {
		@Override
		public AbstractMJTable createTable() {
			return new Table_JiYuan();
		}
	},

	// 河南济源麻将
	GAME_TYPE_NEW_JI_YUAN(GameConstants.GAME_TYPE_NEW_JI_YUAN) {
		@Override
		public AbstractMJTable createTable() {
			return new Table_JiYuan();
		}
	},

	// 江西乐平麻将
	GAME_TYPE_JX_LE_PING(GameConstants.GAME_TYPE_JX_LE_PING) {
		@Override
		public AbstractMJTable createTable() {
			return new Table_LePing();
		}
	},

	GAME_TYPE_MJ_SHANG_QIU(GameConstants.GAME_TYPE_MJ_SHANG_QIU) {
		@Override
		public AbstractMJTable createTable() {
			return new MJTable_ShangQiu();
		}
	},

	GAME_TYPE_NEW_SHANG_QIU(GameConstants.GAME_TYPE_NEW_SHANG_QIU) {
		@Override
		public AbstractMJTable createTable() {
			return new MJTable_ShangQiu();
		}
	},

	// 广西南宁麻将
	GAME_TYPE_GX_NAN_NING(GameConstants.GAME_TYPE_GX_NAN_NING) {
		@Override
		public AbstractMJTable createTable() {
			return new Table_NanNing();
		}
	},

	// 江苏扬中麻将
	GAME_TYPE_JS_YANG_ZHONG(GameConstants.GAME_TYPE_JS_YANG_ZHONG) {
		@Override
		public AbstractMJTable createTable() {
			return new Table_YangZhong();
		}

		@Override
		public int[] getCards() {
			return Constants_YangZhong.CARD_DATA_YANG_ZHONG;
		}
	},

	// 红中赖子杠
	GAME_TYPE_HU_BEI_HZ_LAI_GANG(GameConstants.GAME_TYPE_HU_BEI_HZ_LAI_GANG) {

		@Override
		public AbstractMJTable createTable() {
			return new Table_HZ();
		}
	},

	GAME_TYPE_HU_BEI_HUANG_ZHOU(GameConstants.GAME_TYPE_HU_BEI_HUANG_ZHOU) {
		@Override
		public AbstractMJTable createTable() {
			return new Table_HuangZhou();
		}

		@Override
		public int[] getCards() {
			return Constants_HuangZhou.CARD_DATA_HUANG_ZHOU;
		}
	},
	GAME_TYPE_JIANGSU_JH(GameConstants.GAME_TYPE_MJ_JIANG_SU_JIN_HU) {
		@Override
		public AbstractMJTable createTable() {
			return new MJTable_JiangSu_JH();
		}
	},

	GAME_TYPE_HE_NAN_HUA_XIAN(GameConstants.GAME_TYPE_HE_NAN_HUA_XIAN) {
		@Override
		public AbstractMJTable createTable() {
			return new Table_HuaXian();
		}

		@Override
		public int[] getCards() {
			return Constants_HuaXian.CARD_DATA;
		}
	},

	GAME_TYPE_NEW_HUA_ZHOU(GameConstants.GAME_TYPE_NEW_HUA_ZHOU) {
		@Override
		public AbstractMJTable createTable() {
			return new Table_HuaXian();
		}

		@Override
		public int[] getCards() {
			return Constants_HuaXian.CARD_DATA;
		}
	},

	/*
	 * GAME_TYPE_HE_NAN_LUO_HE(GameConstants.GAME_TYPE_HENAN_LH) {
	 * 
	 * @Override public AbstractMJTable createTable() { return new MJTable_LH();
	 * }
	 * 
	 * @Override public int[] getCards() { return GameConstants_LH.CARD_DATA; }
	 * },
	 */

	// 经典长沙麻将
	GAME_TYPE_JD_CHANG_SHA(GameConstants.GAME_TYPE_JD_CHANG_SHA) {
		@Override
		public AbstractMJTable createTable() {
			return new Table_JingDian_CS();
		}

		@Override
		public int[] getCards() {
			return Constants_JingDian_CS.CARD_DATA;
		}
	},
	// 经典长沙麻将
	GAME_TYPE_MJ_CS_QY(GameConstants.GAME_TYPE_MJ_CS_QY) {
		@Override
		public AbstractMJTable createTable() {
			return new Table_JingDian_CS_QY();
		}
		
		@Override
		public int[] getCards() {
			return Constants_JingDian_CS.CARD_DATA;
		}
	},
	
	// 临湘轻友长沙
	GAME_TYPE_MJ_LX_CS_QY(GameConstants.GAMR_TYPE_MJ_LX_QY_CS) {
		@Override
		public AbstractMJTable createTable() {
			return new Table_LX_CS_QY();
		}
		
		@Override
		public int[] getCards() {
			return Constants_LX_CS_QY.CARD_DATA;
		}
	},

	// 湖北鄂州晃晃麻将
	GAME_TYPE_ER_ZHOU(GameConstants.GAME_TYPE_ER_ZHOU) {
		@Override
		public AbstractMJTable createTable() {
			return new Table_EZ();
		}

		@Override
		public int[] getCards() {
			return Constants_EZ.CARD_DATA;
		}

	},

	// 湖北鄂州晃晃麻将
	GAME_TYPE_3D_E_ZHOU(GameConstants.GAME_TYPE_3D_E_ZHOU) {
		@Override
		public AbstractMJTable createTable() {
			return new Table_EZ();
		}

		@Override
		public int[] getCards() {
			return Constants_EZ.CARD_DATA;
		}

	},

	// 河南窟窿带神麻将
	GAME_TYPE_KLDS(GameConstants.GAME_TYPE_HENNAN_MJ_KLDS) {
		@Override
		public AbstractMJTable createTable() {
			return new MJTable_KLDS();
		}

		@Override
		public int[] getCards() {
			return GameConstants_KLDS.CARD_DATA;
		}
	},

	// 河南窟窿带神麻将（鹿邑麻将）
	GAME_TYPE_NEW_LU_YI(GameConstants.GAME_TYPE_NEW_LU_YI) {
		@Override
		public AbstractMJTable createTable() {
			return new MJTable_KLDS();
		}

		@Override
		public int[] getCards() {
			return GameConstants_KLDS.CARD_DATA;
		}
	},

	// 河南焦作麻将
	GAME_TYPE_JZ(GameConstants.GAME_TYPE_HENAN_JZ) {
		@Override
		public AbstractMJTable createTable() {
			return new MJTable_JZ();
		}

		@Override
		public int[] getCards() {
			return GameConstants_JZ.CARD_DATA;
		}
	},

	GAME_TYPE_NEW_JIAO_ZUO(GameConstants.GAME_TYPE_NEW_JIAO_ZUO) {
		@Override
		public AbstractMJTable createTable() {
			return new MJTable_JZ();
		}

		@Override
		public int[] getCards() {
			return GameConstants_JZ.CARD_DATA;
		}
	},

	// 汉寿王麻将
	GAME_TYPE_HAN_SHOU_WANG(GameConstants.GAME_TYPE_HAN_SHOU_WANG) {

		@Override
		public AbstractMJTable createTable() {
			return new Table_HSW();
		}

		@Override
		public int[] getCards() {
			return GameConstants_HanShouWang.CARD_DATA_HAN_SHOU_WANG_112;
		}
	},

	// 贵阳抓鸡
	GAME_TYPE_GUI_YANG(GameConstants.GAME_TYPE_GUI_YANG) {

		@Override
		public AbstractMJTable createTable() {
			return new Table_GY();
		}

		@Override
		public int[] getCards() {
			return MJConstants.CARD_DATA_WAN_TIAO_TONG;
		}
	},

	// 河北保定
	GAME_TYPE_BAO_DING(GameConstants.GAME_TYPE_BAO_DING) {

		@Override
		public AbstractMJTable createTable() {
			return new Table_BD();
		}
	},

	// 鄂州凑一色
	GAME_TYPE_COU_YI_SE(GameConstants.GAME_TYPE_COU_YI_SE) {
		@Override
		public AbstractMJTable createTable() {
			return new Table_CouYiSe();
		}

		@Override
		public int[] getCards() {
			return Constants_CouYiSe.CARD_DATA;
		}
	},
	// 湖北黄石麻将
	GAME_TYPE_HU_BEI_HUANG_SHI(GameConstants.GAME_TYPE_HU_BEI_HUANG_SHI) {
		@Override
		public AbstractMJTable createTable() {
			return new Table_HuangShi();
		}

		@Override
		public int[] getCards() {
			return Constants_HuangShi.CARD_DATA;
		}
	},

	// 湖北黄石麻将
	GAME_TYPE_3D_HUANG_SHI(GameConstants.GAME_TYPE_3D_HUANG_SHI) {
		@Override
		public AbstractMJTable createTable() {
			return new Table_HuangShi();
		}

		@Override
		public int[] getCards() {
			return Constants_HuangShi.CARD_DATA;
		}
	},

	// 汕尾麻将
	GAME_TYPE_SHAN_WEI(GameConstants.GAME_TYPE_SHAN_WEI) {

		@Override
		public AbstractMJTable createTable() {
			return new Table_SW();
		}
	},

	// 陆河麻将
	GAME_TYPE_LU_HE(GameConstants.GAME_TYPE_LU_HE) {

		@Override
		public AbstractMJTable createTable() {
			return new Table_LUHE();
		}
	},

	// 桃江麻将
	GAME_TYPE_HUNAN_TAO_JIANG(GameConstants.GAME_TYPE_HUNAN_TAO_JIANG) {

		@Override
		public AbstractMJTable createTable() {
			return new Table_TaoJiang(this);
		}

		@Override
		public int[] getCards() {
			return Constants_TaoJiang.CARD_DATA;
		}
	},

	// 赣州冲关
	GAME_TYPE_MJ_GZCG(GameConstants.GAME_TYPE_MJ_GZCG) {
		@Override
		public AbstractMJTable createTable() {
			return new Table_GZCG();
		}
	},

	// 宁都麻将
	GAME_TYPE_MJ_NINGDU(GameConstants.GAME_TYPE_MJ_NINGDU) {
		@Override
		public AbstractMJTable createTable() {
			return new MJTable_ND();
		}

		@Override
		public int[] getCards() {
			return GameConstants_ND.CARD_DATA_MAX;
		}

	},
	// 于都麻将
	GAME_TYPE_JX_YUDU(GameConstants.GAME_TYPE_MJ_JX_YUDU) {
		@Override
		public AbstractMJTable createTable() {
			return new MJTable_YD();
		}

		@Override
		public int[] getCards() {
			return GameConstants_YD.CARD_DATA_MAX;
		}

	},

	// 株洲转转
	GAME_TYPE_MJ_ZHUZHOU_ZZ(GameConstants.GAME_TYPE_ZHUZHOU_ZZ) {
		@Override
		public AbstractMJTable createTable() {
			return new MJTable_ZHUZHOUZZ();
		}

		@Override
		public int[] getCards() {
			return GameConstants.CARD_DATA_HU_NAN;
		}
	},

	// 溆浦麻将
	GAME_TYPE_MJ_XU_PU(GameConstants.GAME_TYPE_MJ_XU_PU) {
		@Override
		public AbstractMJTable createTable() {
			return new MJTable_XUPUMJ();
		}

		@Override
		public int[] getCards() {
			return GameConstants_RUIJIN.CARD_DATA_MAX;
		}
	},

	// 获嘉麻将
	GAME_TYPE_MJ_HUO_JIA(GameConstants.GAME_TYPE_MJ_HUO_JIA) {
		@Override
		public AbstractMJTable createTable() {
			return new Table_HuoJia();
		}

		@Override
		public int[] getCards() {
			return GameConstants.CARD_DATA_DAI_FENG;
		}
	},

	// 推倒胡
	GAME_TYPE_MJ_TDH(GameConstants.GAME_TYPE_MJ_TDH) {
		@Override
		public AbstractMJTable createTable() {
			return new Table_TDH();
		}
	},
	// 广东推倒胡3D
	GAME_TYPE_MJ_TDH_3D(GameConstants.GAME_TYPE_MJ_TDH_3D) {
		@Override
		public AbstractMJTable createTable() {
			return new Table_TDH_3D();
		}
	},

	// 广东100张
	GAME_TYPE_MJ_GD_HUNDRED(GameConstants.GAME_TYPE_MJ_GD_HUNDRED) {
		@Override
		public AbstractMJTable createTable() {
			return new Table_GDHundred();
		}
	},

	// 咸宁麻将
	GAME_TYPE_MJ_XIAN_NING(GameConstants.GAME_TYPE_MJ_XIAN_NING) {

		@Override
		public AbstractMJTable createTable() {
			return new Table_XianNing();
		}

	},
	// 泉州麻将
	GAME_TYPE_MJ_QUAN_ZHOU(GameConstants.GAME_TYPE_MJ_QUAN_ZHOU) {

		@Override
		public AbstractMJTable createTable() {
			return new Table_QuanZhou();
		}

		@Override
		public int[] getCards() {
			return Constants_MJ_QUAN_ZHOU.CARD_DATA_MAX;
		}
	},
	// 广昌麻将
	GAME_TYPE_MJ_GUANG_CHANG(GameConstants.GAME_TYPE_MJ_GUANG_CHANG) {

		@Override
		public AbstractMJTable createTable() {
			return new Table_GuangChang();
		}

	},
	// 麻将亲友版
	GAME_TYPE_MJ_YI_YANG_QIN_YOU(GameConstants.GAME_TYPE_MJ_YI_YANG_QIN_YOU) {
		@Override
		public AbstractMJTable createTable() {
			return new MJTable_YiYang_QY();
		}

		@Override
		public int[] getCards() {
			return MJConstants.CARD_DATA_WAN_TIAO_TONG;
		}

	},
	// 万载麻将
	GAME_TYPE_MJ_WAN_ZAI(GameConstants.GAME_TYPE_MJ_WAN_ZAI) {
		@Override
		public AbstractMJTable createTable() {
			return new Table_WanZai();
		}
	},
	// 怀化麻将
	GAME_TYPE_MJ_HUAI_HUA(GameConstants.GAME_TYPE_MJ_HUAI_HUA) {
		@Override
		public AbstractMJTable createTable() {
			return new Table_HUAI_HUA();
		}
	},
	// 萍乡258
	GAME_TYPE_MJ_PING_XIANG_258(GameConstants.GAME_TYPE_MJ_PING_XIANG_258) {
		@Override
		public AbstractMJTable createTable() {
			return new Table_PING_XIANG();
		}
	},
	// 平江扎鸟麻将
	GAME_TYPE_MJ_PJ_ZHA_NIAO(GameConstants.GAME_TYPE_MJ_PJ_ZHA_NIAO) {
		@Override
		public AbstractMJTable createTable() {
			return new Table_PingJiang();
		}

		@Override
		public int[] getCards() {
			return GameConstants.CARD_DATA_WAN_TIAO_TONG;
		}
	},
	// 景德镇麻将
	GAME_TYPE_MJ_JING_DE_ZHEN(GameConstants.GAME_TYPE_MJ_JING_DE_ZHEN) {
		@Override
		public AbstractMJTable createTable() {
			return new Table_JingDeZhen();
		}
	},
	// 宁乡麻将
	GAME_TYPE_MJ_NING_XIANG(GameConstants.GAME_TYPE_MJ_NING_XIANG) {
		@Override
		public AbstractMJTable createTable() {
			return new Table_NING_XIANG();
		}

		@Override
		public int[] getCards() {
			return GameConstants.CARD_DATA_WAN_TIAO_TONG;
		}
	},
	// 西安麻将
	GAME_TYPE_MJ_XI_AN(GameConstants.GAME_TYPE_MJ_XI_AN) {

		@Override
		public AbstractMJTable createTable() {
			return new Table_XiAn();
		}
	},
	// 华容逞癞子
	GAME_TYPE_MJ_HUA_RONG(GameConstants.GAME_TYPE_MJ_HUA_RONG) {

		@Override
		public AbstractMJTable createTable() {
			return new Table_HuaRong(this);
		}
	},
	// 三人华容逞癞子
	GAME_TYPE_MJ_HUA_RONG3(GameConstants.GAME_TYPE_MJ_HUA_RONG3) {

		@Override
		public AbstractMJTable createTable() {
			return new Table_HuaRong(this);
		}

		@Override
		public int getPlayerCount() {
			return 3;
		}
	},
	// 信阳麻将
	GAME_TYPE_MJ_XIN_YANG(GameConstants.GAME_TYPE_MJ_XIN_YANG) {

		@Override
		public AbstractMJTable createTable() {
			return new Table_XinYang(this);
		}
	},

	// 卡五星
	GAME_TYPE_MJ_KWX(GameConstants.GAME_TYPE_MJ_KA_WU_XING) {

		@Override
		public AbstractMJTable createTable() {
			return new Table_KWX();
		}

		@Override
		public int[] getCards() {
			return GameConstants_KWX.CARD_DATA_DEFAULT;
		}
	},

	// 卡五星
	GAME_TYPE_MJ_KWX_2D(GameConstants.GAME_TYPE_KWX_2D) {

		@Override
		public AbstractMJTable createTable() {
			return new Table_KWX_2D();
		}

		@Override
		public int[] getCards() {
			return GameConstants_KWX.CARD_DATA_DEFAULT;
		}
	},

	GAME_TYPE_MJ_LILING_ZZ(GameConstants.GAME_TYPE_LILING_ZZ) {
		@Override
		public AbstractMJTable createTable() {
			return new MJTable_LILINGZZ();
		}

		@Override
		public int[] getCards() {
			return GameConstants.CARD_DATA_HU_NAN;
		}
	},

	GAME_TYPE_MJ_XUE_ZHAN_DAO_DI(GameConstants.GAME_TYPE_MJ_XUE_ZHAN_DAO_DI) {
		@Override
		public AbstractMJTable createTable() {
			return new Table_xzdd(this);
		}

		@Override
		public int[] getCards() {
			return GameConstants.CARD_DATA_WAN_TIAO_TONG;
		}
	},

	// 瑞金麻将
	GAME_TYPE_MJ_RUIJIN(GameConstants.GAME_TYPE_MJ_RUI_JIN) {
		@Override
		public AbstractMJTable createTable() {
			return new MJTable_RUIJIN();
		}

		@Override
		public int[] getCards() {
			return GameConstants_RUIJIN.CARD_DATA_MAX;
		}

	},

	// 幺筒断勾卡麻将
	GAME_TYPE_MJ_XTDGK(GameConstants.GAME_TYPE_MJ_XTDGK) {
		@Override
		public AbstractMJTable createTable() {
			return new MJTable_XTDGK();
		}

		@Override
		public int[] getCards() {
			return GameConstants_XTDGK.CARD_DATA_MAX;
		}

	},

	// 江西宜丰跳跳麻将
	GAME_TYPE_MJ_TT(GameConstants.GAME_TYPE_MJ_TIAO_TIAO) {
		@Override
		public AbstractMJTable createTable() {
			return new MJTable_TT();
		}

		@Override
		public int[] getCards() {
			return GameConstants_TIAOTIAO.CARD_DATA_MAX;
		}

	},

	// 渭南麻将
	GAME_TYPE_MJ_WEINAN(GameConstants.GAME_TYPE_MJ_WEINAN) {

		@Override
		public AbstractMJTable createTable() {
			return new MJTable_WEIHE();
		}

		@Override
		public int[] getCards() {
			return GameConstants_WEIHE.CARD_DATA_MAX;
		}

	},
	/*
	 * // 漯河麻将 GAME_TYPE_HE_NAN_LUO_HE(GameConstants.GAME_TYPE_HENAN_LH) {
	 * 
	 * @Override public AbstractMJTable createTable() { return new MJTable_LH();
	 * }
	 * 
	 * @Override public int[] getCards() { return GameConstants_LH.CARD_DATA; }
	 * },
	 */

	// 山西推倒胡麻将
	GAME_TYPE_SX_TUIDAOHU(GameConstants.GAME_TYPE_SX_TUIDAOHU) {
		@Override
		public AbstractMJTable createTable() {
			return new Table_Shan_Xi_TDH();
		}
	},
	// 欢乐扣点点
	GAME_TYPE_MJ_HLKDD(GameConstants.GAME_TYPE_MJ_HLKDD) {
		@Override
		public AbstractMJTable createTable() {
			return new Table_HLKDD();
		}
	},
	// 做牌推倒胡
	GAME_TYPE_MJ_ZPTDH(GameConstants.GAME_TYPE_MJ_ZPTDH) {
		@Override
		public AbstractMJTable createTable() {
			return new Table_ZPTDH();
		}
	},

	// 通城麻将
	GAME_TYPE_TONG_CHENG(GameConstants.GAME_TYPE_TONG_CHENG) {

		@Override
		public AbstractMJTable createTable() {
			return new Table_TC();
		}

		@Override
		public int[] getCards() {
			return GameConstants_TC.DEFAULT_CARDS;
		}
	},

	// 山西静乐麻将
	GAME_TYPE_MJ_SXKD(GameConstants.GAME_TYPE_SX_JINGYUE) {

		@Override
		public AbstractMJTable createTable() {
			return new MJTable_ShanXi_JINGYUE();
		}

		@Override
		public int[] getCards() {
			return GameConstants_SXJY.CARD_DATA_MAX;
		}

	},

	// 陕西滑水麻将
	GAME_TYPE_MJ_SX_HUA_SHUI(GameConstants.GAME_TYPE_MJ_SX_HUA_SHUI) {

		@Override
		public AbstractMJTable createTable() {
			return new MJTable_ShanXi_HuaShui(this);
		}
	},
	// 宁夏滑水麻将
	GAME_TYPE_MJ_NX_HUA_SHUI(GameConstants.GAME_TYPE_MJ_NX_HUA_SHUI) {

		@Override
		public AbstractMJTable createTable() {
			return new MJTable_NingXia_HuaShui(this);
		}
	},

	// 山西襄垣麻将
	GAME_TYPE_MJ_XIANG_YUAN(GameConstants.GAME_TYPE_XIANG_YUAN) {

		@Override
		public AbstractMJTable createTable() {
			return new MJTable_ShanXi_XiangYuan(this);
		}
	},
	// 福建光泽麻将
	GAME_TYPE_MJ_FJ_GUANG_ZE(GameConstants.GAME_TYPE_MJ_FJ_GUANG_ZE) {

		@Override
		public AbstractMJTable createTable() {
			return new MJTable_FuJian_GuangZe(this);
		}
	},

	// 山西岚县麻将
	GAME_TYPE_MJ_SXLX(GameConstants.GAME_TYPE_MJ_SX_LAN_XIAN) {
		@Override
		public AbstractMJTable createTable() {
			return new MJTable_ShanXi_LANXIAN();
		}

		@Override
		public int[] getCards() {
			return GameConstants_SXLX.CARD_DATA_MAX;
		}

	},

	// 山西娄烦麻将
	GAME_TYPE_MJ_SXLF(GameConstants.GAME_TYPE_MJ_SX_LOU_FAN) {
		@Override
		public AbstractMJTable createTable() {
			return new MJTable_ShanXi_LOUFAN();
		}

		@Override
		public int[] getCards() {
			return GameConstants_SXLF.CARD_DATA_MAX;
		}

	},

	// 山西宁武麻将
	GAME_TYPE_MJ_SXNW(GameConstants.GAME_TYPE_MJ_SX_NING_WU) {
		@Override
		public AbstractMJTable createTable() {
			return new MJTable_ShanXi_NINGWU();
		}

		@Override
		public int[] getCards() {
			return GameConstants_SXNW.CARD_DATA_MAX;
		}

	},

	// 广东惠州庄
	GAME_TYPE_MJ_HUIZHOU(GameConstants.GAME_TYPE_MJ_GD_HUIZHOUZHUANG) {
		@Override
		public AbstractMJTable createTable() {
			return new Table_HuiZhou();
		}

		// 4人麻将
		@Override
		public int getPlayerCount() {
			return GameConstants.GAME_PLAYER;
		}

	},
	// 吕梁麻将
	GAME_TYPE_SXLL(GameConstants.GAME_TYPE_NEW_LV_LIANG) {
		@Override
		public int[] getCards() {
			return MJConstants.DEFAULT;
		}

		@Override
		public AbstractMJTable createTable() {
			return new MJTable_LVlIANG();
		}

		// 4人麻将
		@Override
		public int getPlayerCount() {
			return GameConstants.GAME_PLAYER;
		}

	},

	// 山西扣点
	GAME_TYPE_SXKD(GameConstants.GAME_TYPE_MJ_SX_KOU_DIAN) {
		@Override
		public int[] getCards() {
			return MJConstants.DEFAULT;
		}

		@Override
		public AbstractMJTable createTable() {
			return new MJTable_KouDian();
		}

		// 4人麻将
		@Override
		public int getPlayerCount() {
			return GameConstants.GAME_PLAYER;
		}

	},

	// 陕西乡宁
	GAME_TYPE_SXXN(GameConstants.GAME_TYPE_MJ_SHAN_XI_XIANG_NING) {
		@Override
		public int[] getCards() {
			return MJConstants.DEFAULT;
		}

		@Override
		public AbstractMJTable createTable() {
			return new MJTable_XN();
		}
	},

	// 山西扣点点
	GAME_TYPE_SXKDD(GameConstants.GAME_TYPE_MJ_SXKDD) {
		@Override
		public int[] getCards() {
			return MJConstants.DEFAULT;
		}

		@Override
		public AbstractMJTable createTable() {
			return new MJTable_SXKDD();
		}
	},

	// 陕西麻将
	GAME_TYPE_MJ_SHANXI(GameConstants.GAME_TYPE_MJ_SHANXI) {
		@Override
		public int[] getCards() {
			return MJConstants.CARD_DATA_WAN_TIAO_TONG;
		}

		@Override
		public AbstractMJTable createTable() {
			return new Table_ShanXi();
		}

		// 4人麻将
		@Override
		public int getPlayerCount() {
			return GameConstants.GAME_PLAYER;
		}
	},

	// 茉阳鬼
	GAME_TYPE_MYG(GameConstants.GAME_TYPE_NEW_MOYANG_GUI) {
		@Override
		public int[] getCards() {
			return MJConstants.DEFAULT;
		}

		@Override
		public AbstractMJTable createTable() {
			return new MjTableMoYangGui();
		}

		// 4人麻将
		@Override
		public int getPlayerCount() {
			return GameConstants.GAME_PLAYER;
		}
	},

	// 孝感卡五星
	GAME_TYPE_MJ_KWX_XG(GameConstants.GAME_TYPE_MJ_KA_WU_XING_XG) {

		@Override
		public AbstractMJTable createTable() {
			return new Table_KWX_XG();
		}

		@Override
		public int[] getCards() {
			return GameConstants_KWX.CARD_DATA_DEFAULT;
		}
	},

	// 孝感卡五星
	GAME_TYPE_MJ_KWX_XG_2D(GameConstants.GAME_TYPE_KWX_XG_2D) {

		@Override
		public AbstractMJTable createTable() {
			return new Table_KWX_XG_2D();
		}

		@Override
		public int[] getCards() {
			return GameConstants_KWX.CARD_DATA_DEFAULT;
		}
	},

	// 黄山屯溪麻将
	GAME_TYPE_MJ_HUANGSHAN(GameConstants.GAME_TYPE_MJ_HUANG_SHAN_TUN_XI) {

		@Override
		public AbstractMJTable createTable() {
			return new Table_HuangShan();
		}

		@Override
		public int[] getCards() {
			return Constants_HuangShan.CARD_DATA;
		}
	},

	// 婺源麻将
	GAME_TYPE_MJ_WUYUAN(GameConstants.GAME_TYPE_MJ_WU_YUAN) {

		@Override
		public AbstractMJTable createTable() {
			return new Table_WuYuan();
		}

		@Override
		public int[] getCards() {
			return MJConstants.DEFAULT;
		}
	},

	// 宜丰麻将
	GAME_TYPE_YIFENG(GameConstants.GAME_TYPE_NEW_YI_FENG) {
		@Override
		public int[] getCards() {
			return MJConstants.DEFAULT;
		}

		@Override
		public AbstractMJTable createTable() {
			return new Table_YiFeng();
		}

		// 4人麻将
		@Override
		public int getPlayerCount() {
			return GameConstants.GAME_PLAYER;
		}
	},

	// 三门峡麻将
	GAME_TYPE_SANMENXIA(GameConstants.GAME_TYPE_NEW_SAN_MEN_XIA_NEW) {
		@Override
		public int[] getCards() {
			return MJConstants.DEFAULT;
		}

		@Override
		public AbstractMJTable createTable() {
			return new MJTable_SanMenXia();
		}

		// 4人麻将
		@Override
		public int getPlayerCount() {
			return GameConstants.GAME_PLAYER;
		}
	},

	// 吉林长春麻将
	GAME_TYPE_CHANG_CHUN(GameConstants.GAME_TYPE_NEW_CHANG_CHUN) {
		@Override
		public int[] getCards() {
			return MJConstants.DEFAULT;
		}

		@Override
		public AbstractMJTable createTable() {
			return new MjTable_ChangChun();
		}

		// 4人麻将
		@Override
		public int getPlayerCount() {
			return GameConstants.GAME_PLAYER;
		}
	},

	// 吉林松原麻将
	GAME_TYPE_SONG_YUAN(GameConstants.GAME_TYPE_NEW_SONG_YUAN) {
		@Override
		public int[] getCards() {
			return MJConstants.CARD_DATA_HNCZ;
		}

		@Override
		public AbstractMJTable createTable() {
			return new MjTable_SongYuan();
		}

		// 4人麻将
		@Override
		public int getPlayerCount() {
			return GameConstants.GAME_PLAYER;
		}
	},
	// 大连穷胡麻将
	GAME_TYPE_NEW_MJ_DLQH(GameConstants.GAME_TYPE_NEW_MJ_DLQH) {
		@Override
		public int[] getCards() {
			return MJConstants.DEFAULT;
		}

		@Override
		public AbstractMJTable createTable() {
			return new Table_DaLianQiongHu();
		}
	},
	// 蒲城麻将
	GAME_TYPE_MJ_PU_CHENG(GameConstants.GAME_TYPE_MJ_PU_CHENG) {
		@Override
		public int[] getCards() {
			return MJConstants.CARD_DATA_HNCZ;
		}

		@Override
		public AbstractMJTable createTable() {
			return new Table_PuCheng();
		}

	},

	// 山西洪洞王牌麻将
	GAME_TYPE_MJ_SXHD(GameConstants.GAME_TYPE_MJ_SX_HDWP) {

		@Override
		public AbstractMJTable createTable() {
			return new MJTable_HongDong();
		}

		@Override
		public int[] getCards() {
			return Constants_MJ_SXHD.CARD_DATA_MAX;
		}

	},

	// 萍乡转转
	GAME_TYPE_MJ_PING_XIANG_ZZ(GameConstants.GAME_TYPE_MJ_PING_XIANG_ZZ) {
		@Override
		public int[] getCards() {
			return GameConstants.CARD_DATA_BU_DAI_FENG_LZ;
		}

		@Override
		public AbstractMJTable createTable() {
			return new MJTable_PXZZ(this);
		}

	},
	// 红中麻将
	GAME_TYPE_MJ_PING_XIANG_HZ(GameConstants.GAME_TYPE_MJ_PING_XIANG_HZ) {
		@Override
		public int[] getCards() {
			return MJConstants.CARD_DATA_HNCZ;
		}

		@Override
		public AbstractMJTable createTable() {
			return new MJTable_PXZZ(this);
		}

	}, // 邵阳3D红中飞
	GAME_TYPE_MJ_HONG_ZHONG_FEI(GameConstants.GAME_TYPE_MJ_HONG_ZHONG_FEI) {
		/*
		 * @Override public int[] getCards() { return
		 * MJConstants.CARD_DATA_HNCZ; }
		 */

		@Override
		public AbstractMJTable createTable() {
			return new MJTable_SYHZ();
		}

	},;

	private final int value;

	MJType(int value) {
		this.value = value;
	}

	public int getValue() {
		return this.value;
	}

	// 创建麻将桌子
	public abstract AbstractMJTable createTable();

	// 获取 该牌桌麻将数据 需要的自己继承
	public int[] getCards() {
		return MJConstants.DEFAULT;
	}

	public int getCardLength() {
		return this.getCards().length;
	}

	public int getPlayerCount() {
		return GameConstants.GAME_PLAYER;
	}

	public static MJType getType(int typeValue) {
		return maps.get(typeValue);
	}

	private static final Map<Integer, MJType> maps = new HashMap<>();

	static {
		MJType[] temp = MJType.values();
		for (int i = 0; i < temp.length; i++) {
			maps.put(temp[i].value, temp[i]);
		}
	}
}
