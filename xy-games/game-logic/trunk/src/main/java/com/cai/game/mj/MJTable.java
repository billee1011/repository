package com.cai.game.mj;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.RedisConstant;
import com.cai.common.define.ECardType;
import com.cai.common.define.EMoneyOperateType;
import com.cai.common.domain.AddMoneyResultModel;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.WeaveItem;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.GameDescUtil;
import com.cai.common.util.PBUtil;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.SpringService;
import com.cai.common.util.ZipUtil;
import com.cai.dictionary.BrandIdDict;
import com.cai.dictionary.SysParamDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.AddDiscardRunnable;
import com.cai.future.runnable.DispatchLastCardRunnable;
import com.cai.future.runnable.JianPaoHuRunnable;
import com.cai.future.runnable.KickRunnable;
import com.cai.future.runnable.TuoGuanRunnable;
import com.cai.future.runnable.XiaoHuRunnable;
import com.cai.game.mj.MJGameLogic.AnalyseItem;
import com.cai.game.mj.handler.MJHandlerDiHu;
import com.cai.game.mj.handler.MJHandlerDispatchCard;
import com.cai.game.mj.handler.MJHandlerFinish;
import com.cai.game.mj.handler.ay.MJHandlerChiPeng_AY;
import com.cai.game.mj.handler.ay.MJHandlerDispatchCard_AY;
import com.cai.game.mj.handler.ay.MJHandlerGang_AY;
import com.cai.game.mj.handler.ay.MJHandlerOutCardBaoTing;
import com.cai.game.mj.handler.ay.MJHandlerOutCardOperate_AY;
import com.cai.game.mj.handler.ay.MJHandlerPaoQiang;
import com.cai.game.mj.handler.cs.MJHandlerChiPeng_CS;
import com.cai.game.mj.handler.cs.MJHandlerDispatchCard_CS;
import com.cai.game.mj.handler.cs.MJHandlerGang_CS;
import com.cai.game.mj.handler.cs.MJHandlerGang_CS_DispatchCard;
import com.cai.game.mj.handler.cs.MJHandlerGang_THJ_CS_DispatchCard;
import com.cai.game.mj.handler.cs.MJHandlerHaiDi_CS;
import com.cai.game.mj.handler.cs.MJHandlerOutCardOperate_CS;
import com.cai.game.mj.handler.cs.MJHandlerPiao_CS;
import com.cai.game.mj.handler.cs.MJHandlerXiaoHu;
import com.cai.game.mj.handler.cs.MJHandlerYaoHaiDi_CS;
import com.cai.game.mj.handler.henan.MJHandlerChiPeng_HeNan;
import com.cai.game.mj.handler.henan.MJHandlerDispatchCard_HeNan;
import com.cai.game.mj.handler.henan.MJHandlerGang_HeNan;
import com.cai.game.mj.handler.henan.MJHandlerHun;
import com.cai.game.mj.handler.henan.MJHandlerOutCardOperate_HeNan;
import com.cai.game.mj.handler.henan.MJHandlerPao_HeNan;
import com.cai.game.mj.handler.henan.MJHandlerQiShouHun;
import com.cai.game.mj.handler.henankf.MJHandlerChiPeng_HeNankf;
import com.cai.game.mj.handler.henankf.MJHandlerDispatchCard_HeNankf;
import com.cai.game.mj.handler.henankf.MJHandlerGang_HeNankf;
import com.cai.game.mj.handler.henankf.MJHandlerHun_kf;
import com.cai.game.mj.handler.henankf.MJHandlerOutCardOperate_HeNankf;
import com.cai.game.mj.handler.henankf.MJHandlerPao_HeNankf;
import com.cai.game.mj.handler.henankf.MJHandlerQiShouHunkf;
import com.cai.game.mj.handler.henanlh.MJHandlerChiPeng_HeNan_lh;
import com.cai.game.mj.handler.henanlh.MJHandlerDispatchCard_HeNan_lh;
import com.cai.game.mj.handler.henanlh.MJHandlerGang_HeNan_lh;
import com.cai.game.mj.handler.henanlh.MJHandlerOutCardOperate_HeNan_lh;
import com.cai.game.mj.handler.henanny.MJHandlerChiPeng_HeNanny;
import com.cai.game.mj.handler.henanny.MJHandlerDispatchCard_HeNanny;
import com.cai.game.mj.handler.henanny.MJHandlerGang_HeNanny;
import com.cai.game.mj.handler.henanny.MJHandlerHun_ny;
import com.cai.game.mj.handler.henanny.MJHandlerOutCardOperate_HeNanny;
import com.cai.game.mj.handler.henanny.MJHandlerPao_HeNanny;
import com.cai.game.mj.handler.henanny.MJHandlerQiShouHunny;
import com.cai.game.mj.handler.henanpds.MJHandlerChiPeng_HeNanpds;
import com.cai.game.mj.handler.henanpds.MJHandlerDispatchCard_HeNanpds;
import com.cai.game.mj.handler.henanpds.MJHandlerGang_HeNanpds;
import com.cai.game.mj.handler.henanpds.MJHandlerHun_HeNanpds;
import com.cai.game.mj.handler.henanpds.MJHandlerOutCardBaoTing_HeNanpds;
import com.cai.game.mj.handler.henanpds.MJHandlerOutCardOperate_HeNanpds;
import com.cai.game.mj.handler.henanpds.MJHandlerPaoKou_HeNanpds;
import com.cai.game.mj.handler.henanpds.MJHandlerPao_HeNanpds;
import com.cai.game.mj.handler.henanpds.MJHandlerQiShouHun_HeNanpds;
import com.cai.game.mj.handler.henanxc.MJHandlerChiPeng_XC;
import com.cai.game.mj.handler.henanxc.MJHandlerDispatchCard_XC;
import com.cai.game.mj.handler.henanxc.MJHandlerGang_XC;
import com.cai.game.mj.handler.henanxc.MJHandlerHun_XC;
import com.cai.game.mj.handler.henanxc.MJHandlerOutCardBaoTing_XC;
import com.cai.game.mj.handler.henanxc.MJHandlerOutCardOperate_XC;
import com.cai.game.mj.handler.henanxc.MJHandlerPao_XC;
import com.cai.game.mj.handler.henanxx.MJHandlerChiPeng_HeNanxx;
import com.cai.game.mj.handler.henanxx.MJHandlerDispatchCard_HeNanxx;
import com.cai.game.mj.handler.henanxx.MJHandlerGang_HeNanxx;
import com.cai.game.mj.handler.henanxx.MJHandlerHun_xx;
import com.cai.game.mj.handler.henanxx.MJHandlerOutCardOperate_HeNanxx;
import com.cai.game.mj.handler.henanxx.MJHandlerPao_HeNanxx;
import com.cai.game.mj.handler.henanxx.MJHandlerQiShouHunxx;
import com.cai.game.mj.handler.henanxy.MJHandlerChiPeng_HeNanxy;
import com.cai.game.mj.handler.henanxy.MJHandlerDispatchCard_HeNanxy;
import com.cai.game.mj.handler.henanxy.MJHandlerGang_HeNanxy;
import com.cai.game.mj.handler.henanxy.MJHandlerNao_HenNanxy;
import com.cai.game.mj.handler.henanxy.MJHandlerOutCardOperate_HeNanxy;
import com.cai.game.mj.handler.henanxy.MJHandlerPao_HenNanxy;
import com.cai.game.mj.handler.henanzhoukou.MJHandlerChiPeng_HeNan_ZhouKou;
import com.cai.game.mj.handler.henanzhoukou.MJHandlerDispatchCard_HeNan_ZhouKou;
import com.cai.game.mj.handler.henanzhoukou.MJHandlerGang_HeNan_ZhouKou;
import com.cai.game.mj.handler.henanzhoukou.MJHandlerOutCardBaoTing_HeNan_ZhouKou;
import com.cai.game.mj.handler.henanzhoukou.MJHandlerOutCardOperate_HeNan_ZhouKou;
import com.cai.game.mj.handler.henanzhoukou.MJHandlerPao_HeNan_ZhouKou;
import com.cai.game.mj.handler.henanzhoukou.MJHandlerQiShouHu_HeNan_ZhouKou;
import com.cai.game.mj.handler.henanzhoukou.MJHandlerSelectMagic_HeNan_ZhouKou;
import com.cai.game.mj.handler.henanzhuanzhuan.MJHandlerChiPeng_HeNanZZ;
import com.cai.game.mj.handler.henanzhuanzhuan.MJHandlerDispatchCard_HeNanZZ;
import com.cai.game.mj.handler.henanzhuanzhuan.MJHandlerGang_HeNanZZ;
import com.cai.game.mj.handler.henanzhuanzhuan.MJHandlerOutCardOperate_HeNanZZ;
import com.cai.game.mj.handler.henanzmd.MJHandlerChiPeng_HeNanzmd;
import com.cai.game.mj.handler.henanzmd.MJHandlerDispatchCard_HeNanzmd;
import com.cai.game.mj.handler.henanzmd.MJHandlerGang_HeNanzmd;
import com.cai.game.mj.handler.henanzmd.MJHandlerHun_zmd;
import com.cai.game.mj.handler.henanzmd.MJHandlerOutCardOperate_HeNanzmd;
import com.cai.game.mj.handler.henanzmd.MJHandlerPao_HeNanzmd;
import com.cai.game.mj.handler.henanzmd.MJHandlerQiShouHunzmd;
import com.cai.game.mj.handler.hnhz.MJHandlerChiPeng_HNHZ;
import com.cai.game.mj.handler.hnhz.MJHandlerDispatchCard_HNHZ;
import com.cai.game.mj.handler.hnhz.MJHandlerGang_HNHZ;
import com.cai.game.mj.handler.hnhz.MJHandlerOutCardOperate_HNHZ;
import com.cai.game.mj.handler.hnhz.MJHandlerPao_HeNan_HZ;
import com.cai.game.mj.handler.hz.MJHandlerChiPeng_HZ;
import com.cai.game.mj.handler.hz.MJHandlerDispatchCard_HZ;
import com.cai.game.mj.handler.hz.MJHandlerGang_HZ;
import com.cai.game.mj.handler.hz.MJHandlerOutCardOperate_HZ;
import com.cai.game.mj.handler.hz.MJHandlerPiao_HZ;
import com.cai.game.mj.handler.hz.MJHandlerQiShouHongZhong;
import com.cai.game.mj.handler.lxcg.MJHandlerChiPeng_LXCG;
import com.cai.game.mj.handler.lxcg.MJHandlerDispatchCard_LXCG;
import com.cai.game.mj.handler.lxcg.MJHandlerDispatchLastCard_LXCG;
import com.cai.game.mj.handler.lxcg.MJHandlerGang_LXCG;
import com.cai.game.mj.handler.lxcg.MJHandlerOutCardOperate_LXCG;
import com.cai.game.mj.handler.lxcg.MJHandlerPiao_LXCG;
import com.cai.game.mj.handler.lxcs.MJHandlerChiPeng_CSLX;
import com.cai.game.mj.handler.lxcs.MJHandlerDispatchCard_CSLX;
import com.cai.game.mj.handler.lxcs.MJHandlerDispatchCard_XiaoHu_CSLX;
import com.cai.game.mj.handler.lxcs.MJHandlerGang_CSLX;
import com.cai.game.mj.handler.lxcs.MJHandlerGang_CSLX_DispatchCard;
import com.cai.game.mj.handler.lxcs.MJHandlerHaiDi_CSLX;
import com.cai.game.mj.handler.lxcs.MJHandlerOutCardOperate_CSLX;
import com.cai.game.mj.handler.lxcs.MJHandlerPiao_CSLX;
import com.cai.game.mj.handler.lxcs.MJHandlerXiaoHu_CSLX;
import com.cai.game.mj.handler.lxcs.MJHandlerYaoHaiDi_CSLX;
import com.cai.game.mj.handler.lygc.MJHandlerChiPeng_lygc;
import com.cai.game.mj.handler.lygc.MJHandlerCi_lygc;
import com.cai.game.mj.handler.lygc.MJHandlerDispatchCard_lygc;
import com.cai.game.mj.handler.lygc.MJHandlerGang_lygc;
import com.cai.game.mj.handler.lygc.MJHandlerOutCardOperate_lygc;
import com.cai.game.mj.handler.lygc.MJHandlerPiCi_lygc;
import com.cai.game.mj.handler.lz.MJHandlerChiPeng_LZ;
import com.cai.game.mj.handler.lz.MJHandlerDispatchCard_LZ;
import com.cai.game.mj.handler.lz.MJHandlerGang_LZ;
import com.cai.game.mj.handler.lz.MJHandlerOutCardOperate_LZ;
import com.cai.game.mj.handler.lz.MJHandlerPao;
import com.cai.game.mj.handler.sg.MJHandlerChiPeng_SG;
import com.cai.game.mj.handler.sg.MJHandlerDaDian;
import com.cai.game.mj.handler.sg.MJHandlerDispatchCard_SG;
import com.cai.game.mj.handler.sg.MJHandlerGang_SG;
import com.cai.game.mj.handler.sg.MJHandlerOutCardOperate_SG;
import com.cai.game.mj.handler.xthh.MJHandlerChiPeng_XTHH;
import com.cai.game.mj.handler.xthh.MJHandlerDispatchCard_XTHH;
import com.cai.game.mj.handler.xthh.MJHandlerDispatchLastCard;
import com.cai.game.mj.handler.xthh.MJHandlerGang_XTHH;
import com.cai.game.mj.handler.xthh.MJHandlerLaiGen;
import com.cai.game.mj.handler.xthh.MJHandlerOutCardOperate_XTHH;
import com.cai.game.mj.handler.zhuzhou.MJHandlerChiPeng_ZhuZhou;
import com.cai.game.mj.handler.zhuzhou.MJHandlerDiHu_ZhuZhou;
import com.cai.game.mj.handler.zhuzhou.MJHandlerDispatchCard_ZhuZhou;
import com.cai.game.mj.handler.zhuzhou.MJHandlerGang_ZhuZhou;
import com.cai.game.mj.handler.zhuzhou.MJHandlerGang_ZhuZhou_DispatchCard;
import com.cai.game.mj.handler.zhuzhou.MJHandlerHaiDi_ZhuZhou;
import com.cai.game.mj.handler.zhuzhou.MJHandlerOutCardOperate_ZhuZhou;
import com.cai.game.mj.handler.zhuzhou.MJHandlerTianHu_ZhuZhou;
import com.cai.game.mj.handler.zhuzhou.MJHandlerYaoHaiDi_ZhuZhou;
import com.cai.game.mj.handler.zz.MJHandlerChiPeng_ZZ;
import com.cai.game.mj.handler.zz.MJHandlerDispatchCard_ZZ;
import com.cai.game.mj.handler.zz.MJHandlerGang_ZZ;
import com.cai.game.mj.handler.zz.MJHandlerOutCardOperate_ZZ;
import com.cai.game.mj.jiangxi.pxzz.MJHandlerChiPeng_PXZZ;
import com.cai.game.mj.jiangxi.pxzz.MJHandlerDispatchCard_PXZZ;
import com.cai.game.mj.jiangxi.pxzz.MJHandlerGang_PXZZ;
import com.cai.game.mj.jiangxi.pxzz.MJHandlerOutCardOperate_PXZZ;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.game.util.GameUtilConstants;
import com.cai.redis.service.RedisService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;
import com.cai.util.SysParamServerUtil;

import org.apache.log4j.Logger;
import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.PlayerResultResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;
import protobuf.clazz.mj.Basic.MJ_GAME_END_INFO_EXT;
import protobuf.clazz.mj.Lygc.Lygc_Game_End;

///////////////////////////////////////////////////////////////////////////////////////////////
public class MJTable extends AbstractMJTable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7060061356475703643L;

	private static Logger logger = Logger.getLogger(MJTable.class);

	public boolean _is_xy_nao_zhuang;// 闹庄状态

	public int _bao_ci_start; // 包次开始数量
	public int _bao_ci_state; // 包次触发状态 0 ：未触发 1:触发

	public MJHandlerTianHu_ZhuZhou _handler_tianhu;
	public MJHandlerDiHu _handler_dihu;

	public MJHandlerXiaoHu _handler_xiao_hu;
	public MJHandlerGang_CS_DispatchCard _handler_gang_cs;
	public MJHandlerGang_THJ_CS_DispatchCard _handler_gang_thj_cs;
	public MJHandlerPiao_CS handler_piao_cs;

	public MJHandlerQiShouHongZhong _handler_qishou_hongzhong;
	public MJHandlerDaDian _handler_da_dian;
	public MJHandlerLaiGen _handler_lai_gen;
	public MJHandlerDispatchLastCard _handler_dispath_last_card;

	// 株洲
	public MJHandlerGang_ZhuZhou_DispatchCard _handler_gang_zhuzhou;

	public MJHandlerOutCardBaoTing _handler_out_card_bao_ting; // 报听

	public MJHandlerPaoQiang _handler_pao_qiang; // 跑呛
	public MJHandlerPao _handler_pao; // 跑

	public MJHandlerPao_HeNan _handler_pao_henna;// 河南跑
	public MJHandlerQiShouHun _handler_qishou_hun;// 河南4混
	public MJHandlerHun _handler_hun;// 河南选混

	public MJHandlerPaoKou_HeNanpds _handler_kou_hennapds;// 河南平顶山
	public MJHandlerCi_lygc _hangdler_ci_lygc;// 洛阳杠次
	public MJHandlerPiCi_lygc _handler_pi_ci; // 皮次

	public MJHandlerPao_HeNan_HZ _handler_pao_henna_hz;// 河南红中跑

	public MJHandlerPiao_LXCG _handlerPiao_LXCG;// 临湘炒股飘
	public MJHandlerDispatchLastCard_LXCG _handlerLastCard_lxcg;

	public MJHandlerXiaoHu_CSLX _handler_xiao_hu_cslx;
	public MJHandlerGang_CSLX_DispatchCard _handler_gang_cslx;
	public MJHandlerPiao_CSLX handler_piao_cslx;

	public MJHandlerPao_HenNanxy _handler_pao_henna_xy;
	public MJHandlerNao_HenNanxy _handler_Nao_henna_xy;

	public MJHandlerQiShouHu_HeNan_ZhouKou _handler_qi_shou_hu;
	public MJHandlerSelectMagic_HeNan_ZhouKou _handler_select_magic;

	public boolean LYGC_CI_STATE; // 可杠次状态(杠次胡牌，系统要显示 杠，次，过按钮，杠 就正常杠 不摸次牌 次
									// 就杠之后摸次牌胡牌 过就过)

	public boolean FIRST_DISPATHCARD; // 临湘长沙麻将小胡检测放在庄家发第14张牌后

	public MJHandlerPiao_HZ _handler_piao; // 红中飘分

	public int hu_score[] = new int[getTablePlayerNumber()];// 红中麻将胡分统计
	public int niao_score[] = new int[getTablePlayerNumber()];// 红中麻将鸟分统计
	public int piao_score[] = new int[getTablePlayerNumber()];// 红中麻将飘分显示
	public int hu_type[] = new int[getTablePlayerNumber()]; // 红中麻将胡牌类型

	public boolean send_game_end; // 结算数据是否发送 红中比赛场用

	/**
	 * 洛阳杠次，弹出小结算的延时时间，秒
	 */
	public int game_finish_delay_lygc = 3;

	/**
	 * 是否已经播了包次的动画
	 */
	public boolean has_display_bao_ci_start = false;

	public final int time_for_tou_zi_animation = 1000;
	public final int time_for_tou_zi_fade = 200;
	public final int time_for_run_delay = 500;

	/**
	 * 胡牌时玩家的输赢牌型分
	 */
	public int[] pai_xing_fen = new int[getTablePlayerNumber()];
	/**
	 * 胡牌时玩家的中鸟输赢分
	 */
	public int[] bird_score = new int[getTablePlayerNumber()];

	// 许昌麻将，分析胡牌时，入口点是什么
	public int xc_analyse_type = 0;
	public final int XC_ANALYSE_NORMAL = 1;
	public final int XC_ANALYSE_BAO_TING = 2;
	public final int XC_ANALYSE_TING = 3;

	public MJTable() {

		super();

		_status_cs_gang = false;

		_is_xy_nao_zhuang = false;

		hu_score = new int[getTablePlayerNumber()];// 红中麻将胡分统计
		niao_score = new int[getTablePlayerNumber()];// 红中麻将鸟分统计
		piao_score = new int[getTablePlayerNumber()];// 红中麻将飘分显示
		hu_type = new int[getTablePlayerNumber()]; // 红中麻将胡牌类型

		pai_xing_fen = new int[getTablePlayerNumber()];
		bird_score = new int[getTablePlayerNumber()];

		// 结束信息
	}

	@Override
	protected void onInitTable() {
		if (is_mj_type(GameConstants.GAME_TYPE_CS) || is_mj_type(GameConstants.GAME_TYPE_HALL_CHANG_SHA_MJ)
				|| is_mj_type(GameConstants.GAME_TYPE_THJ_CS)) {
			// 初始化基础牌局handler
			_handler_dispath_card = new MJHandlerDispatchCard_CS();
			_handler_out_card_operate = new MJHandlerOutCardOperate_CS();
			_handler_gang = new MJHandlerGang_CS();
			_handler_chi_peng = new MJHandlerChiPeng_CS();

			// 长沙麻将
			_handler_xiao_hu = new MJHandlerXiaoHu();
			_handler_gang_cs = new MJHandlerGang_CS_DispatchCard();
			_handler_gang_thj_cs = new MJHandlerGang_THJ_CS_DispatchCard();
			_handler_hai_di = new MJHandlerHaiDi_CS();
			_handler_yao_hai_di = new MJHandlerYaoHaiDi_CS();
			handler_piao_cs = new MJHandlerPiao_CS();

		} else if (is_mj_type(GameConstants.GAME_TYPE_HZ) || is_mj_type(GameConstants.GAME_TYPE_HONG_ZHONG_MJ_TH)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_YUYANG_HONGZHON)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_HONG_ZHONG_FEI)
				|| is_mj_type(GameConstants.GAME_TYPE_FLS_HZ_LX)
				|| is_mj_type(GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ)) {
			// 初始化基础牌局handler
			_handler_dispath_card = new MJHandlerDispatchCard_HZ();
			_handler_out_card_operate = new MJHandlerOutCardOperate_HZ();
			_handler_gang = new MJHandlerGang_HZ();
			_handler_chi_peng = new MJHandlerChiPeng_HZ();

			_handler_qishou_hongzhong = new MJHandlerQiShouHongZhong();
			_handler_piao = new MJHandlerPiao_HZ();
			// 邵阳红中飞+萍乡转转
		} else if (is_mj_type(GameConstants.GAME_TYPE_MJ_PING_XIANG_ZZ)) {
			// 初始化基础牌局handler
			_handler_dispath_card = new MJHandlerDispatchCard_PXZZ();
			_handler_out_card_operate = new MJHandlerOutCardOperate_PXZZ();
			_handler_gang = new MJHandlerGang_PXZZ();
			_handler_chi_peng = new MJHandlerChiPeng_PXZZ();

			_handler_qishou_hongzhong = new MJHandlerQiShouHongZhong();
			_handler_piao = new MJHandlerPiao_HZ();
			/*
			 * _handler_qishou_syhz = new MJHandlerQiShouSYHZ(); _handler_sypiao
			 * = new MJHandlerPiao_SYHZ();
			 */
		} else if (is_mj_type(GameConstants.GAME_TYPE_ZZ) || is_mj_type(GameConstants.GAME_TYPE_HALL_ZHUAN_ZHUAN_MJ)) {
			// 初始化基础牌局handler
			_handler_dispath_card = new MJHandlerDispatchCard_ZZ();
			_handler_out_card_operate = new MJHandlerOutCardOperate_ZZ();
			_handler_gang = new MJHandlerGang_ZZ();
			_handler_chi_peng = new MJHandlerChiPeng_ZZ();

			_handler_qishou_hongzhong = new MJHandlerQiShouHongZhong();

			if (has_rule(GameConstants.GAME_RULE_HUNAN_HONGZHONG))
				_logic.add_magic_card_index(_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD));
		} else if (is_mj_type(GameConstants.GAME_TYPE_SHUANGGUI)
				|| is_mj_type(GameConstants.GAME_TYPE_HUNAN_SHANG_XIA_GUI)) {
			// 初始化基础牌局handler
			_handler_dispath_card = new MJHandlerDispatchCard_SG();
			_handler_out_card_operate = new MJHandlerOutCardOperate_SG();
			_handler_gang = new MJHandlerGang_SG();
			_handler_chi_peng = new MJHandlerChiPeng_SG();
			_handler_da_dian = new MJHandlerDaDian();

		} else if (is_mj_type(GameConstants.GAME_TYPE_ZHUZHOU)) {
			// 初始化基础牌局handler
			_handler_dispath_card = new MJHandlerDispatchCard_ZhuZhou();
			_handler_out_card_operate = new MJHandlerOutCardOperate_ZhuZhou();
			_handler_gang = new MJHandlerGang_ZhuZhou();
			_handler_chi_peng = new MJHandlerChiPeng_ZhuZhou();

			_handler_gang_zhuzhou = new MJHandlerGang_ZhuZhou_DispatchCard();
			_handler_hai_di = new MJHandlerHaiDi_ZhuZhou();
			_handler_yao_hai_di = new MJHandlerYaoHaiDi_ZhuZhou();
			_handler_tianhu = new MJHandlerTianHu_ZhuZhou();
			_handler_dihu = new MJHandlerDiHu_ZhuZhou();

		} else if (is_mj_type(GameConstants.GAME_TYPE_XTHH)) {
			// 初始化基础牌局handler
			_handler_dispath_card = new MJHandlerDispatchCard_XTHH();
			_handler_out_card_operate = new MJHandlerOutCardOperate_XTHH();
			_handler_gang = new MJHandlerGang_XTHH();
			_handler_chi_peng = new MJHandlerChiPeng_XTHH();

			_handler_lai_gen = new MJHandlerLaiGen();
			_handler_dispath_last_card = new MJHandlerDispatchLastCard();
			if (has_rule(GameConstants.GAME_RULE_HEBEI_DI_FEN_05)) {
				this.game_cell = 0.5f;
			} else if (has_rule(GameConstants.GAME_RULE_HEBEI_DI_FEN_10)) {
				this.game_cell = 1;
			} else if (has_rule(GameConstants.GAME_RULE_HEBEI_DI_FEN_20)) {
				this.game_cell = 2;
			}
			//
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_AY) || is_mj_type(GameConstants.GAME_TYPE_NEW_AN_YANG)) {
			// 初始化基础牌局handler
			_handler_dispath_card = new MJHandlerDispatchCard_AY();
			_handler_out_card_operate = new MJHandlerOutCardOperate_AY();
			_handler_gang = new MJHandlerGang_AY();
			_handler_chi_peng = new MJHandlerChiPeng_AY();

			_handler_out_card_bao_ting = new MJHandlerOutCardBaoTing();
			_handler_pao_qiang = new MJHandlerPaoQiang();

			_qiang_max_count = 0;
			_lian_zhuang_player = GameConstants.INVALID_SEAT;
			this._shang_zhuang_player = GameConstants.INVALID_SEAT;

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				_player_result.pao[i] = 0;
				_player_result.qiang[i] = 0;
			}
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_XC) || is_mj_type(GameConstants.GAME_TYPE_NEW_XU_CHANG)) {
			// 初始化基础牌局handler
			_handler_dispath_card = new MJHandlerDispatchCard_XC();
			_handler_out_card_operate = new MJHandlerOutCardOperate_XC();
			_handler_gang = new MJHandlerGang_XC();
			_handler_chi_peng = new MJHandlerChiPeng_XC();

			_handler_pao_henna = new MJHandlerPao_XC();
			_handler_hun = new MJHandlerHun_XC();
			_handler_out_card_bao_ting = new MJHandlerOutCardBaoTing_XC();

			_qiang_max_count = 0;
			_lian_zhuang_player = GameConstants.INVALID_SEAT;
			this._shang_zhuang_player = GameConstants.INVALID_SEAT;

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				_player_result.pao[i] = 0;
				_player_result.qiang[i] = 0;
			}
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_LZ) || is_mj_type(GameConstants.GAME_TYPE_NEW_LIN_ZHOU)) {
			// 初始化基础牌局handler
			_handler_dispath_card = new MJHandlerDispatchCard_LZ();
			_handler_out_card_operate = new MJHandlerOutCardOperate_LZ();
			_handler_gang = new MJHandlerGang_LZ();
			_handler_chi_peng = new MJHandlerChiPeng_LZ();

			_handler_pao = new MJHandlerPao();

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				_player_result.pao[i] = 0;
			}
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN) || is_mj_type(GameConstants.GAME_TYPE_NEW_HE_NAN)
				|| is_mj_type(GameConstants.GAME_TYPE_JIAO_ZUO_HUAN_HUAN)) {
			// 初始化基础牌局handler
			_handler_dispath_card = new MJHandlerDispatchCard_HeNan();
			_handler_out_card_operate = new MJHandlerOutCardOperate_HeNan();
			_handler_gang = new MJHandlerGang_HeNan();
			_handler_chi_peng = new MJHandlerChiPeng_HeNan();

			_handler_qishou_hun = new MJHandlerQiShouHun();
			_handler_pao_henna = new MJHandlerPao_HeNan();
			_handler_hun = new MJHandlerHun();

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				_player_result.pao[i] = 0;
			}
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_KF) || is_mj_type(GameConstants.GAME_TYPE_NEW_KAI_FENG)) {
			// 初始化基础牌局handler
			_handler_dispath_card = new MJHandlerDispatchCard_HeNankf();
			_handler_out_card_operate = new MJHandlerOutCardOperate_HeNankf();
			_handler_gang = new MJHandlerGang_HeNankf();
			_handler_chi_peng = new MJHandlerChiPeng_HeNankf();

			_handler_qishou_hun = new MJHandlerQiShouHunkf();
			_handler_pao_henna = new MJHandlerPao_HeNankf();
			_handler_hun = new MJHandlerHun_kf();

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				_player_result.pao[i] = 0;
			}
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_NY) || is_mj_type(GameConstants.GAME_TYPE_NEW_NAN_YANG)) {
			// 初始化基础牌局handler
			_handler_dispath_card = new MJHandlerDispatchCard_HeNanny();
			_handler_out_card_operate = new MJHandlerOutCardOperate_HeNanny();
			_handler_gang = new MJHandlerGang_HeNanny();
			_handler_chi_peng = new MJHandlerChiPeng_HeNanny();

			_handler_qishou_hun = new MJHandlerQiShouHunny();
			_handler_pao_henna = new MJHandlerPao_HeNanny();
			_handler_hun = new MJHandlerHun_ny();

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				_player_result.pao[i] = 0;
			}
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_ZMD)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_ZHU_MA_DIAN)) {
			// 初始化基础牌局handler
			_handler_dispath_card = new MJHandlerDispatchCard_HeNanzmd();
			_handler_out_card_operate = new MJHandlerOutCardOperate_HeNanzmd();
			_handler_gang = new MJHandlerGang_HeNanzmd();
			_handler_chi_peng = new MJHandlerChiPeng_HeNanzmd();

			_handler_qishou_hun = new MJHandlerQiShouHunzmd();
			_handler_pao_henna = new MJHandlerPao_HeNanzmd();
			_handler_hun = new MJHandlerHun_zmd();

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				_player_result.pao[i] = -1;
			}
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_XX) || is_mj_type(GameConstants.GAME_TYPE_NEW_XIN_XIANG)) {
			// 河南新乡麻将 初始化基础牌局handler
			_handler_dispath_card = new MJHandlerDispatchCard_HeNanxx();
			_handler_out_card_operate = new MJHandlerOutCardOperate_HeNanxx();
			_handler_gang = new MJHandlerGang_HeNanxx();
			_handler_chi_peng = new MJHandlerChiPeng_HeNanxx();

			_handler_qishou_hun = new MJHandlerQiShouHunxx();
			_handler_pao_henna = new MJHandlerPao_HeNanxx();
			_handler_hun = new MJHandlerHun_xx();

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				_player_result.pao[i] = 0;
			}
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_PDS)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_PING_DING_SHAN)) {
			// 河南平顶山麻将 初始化基础牌局handler
			_handler_dispath_card = new MJHandlerDispatchCard_HeNanpds();
			_handler_out_card_operate = new MJHandlerOutCardOperate_HeNanpds();
			_handler_gang = new MJHandlerGang_HeNanpds();
			_handler_chi_peng = new MJHandlerChiPeng_HeNanpds();

			_handler_pao_henna = new MJHandlerPao_HeNanpds();
			_handler_kou_hennapds = new MJHandlerPaoKou_HeNanpds();

			_handler_hun = new MJHandlerHun_HeNanpds();
			_handler_qishou_hun = new MJHandlerQiShouHun_HeNanpds();

			_handler_out_card_bao_ting = new MJHandlerOutCardBaoTing_HeNanpds();

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				_player_result.pao[i] = 0;
				_player_result.qiang[i] = 0;
			}
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_HZ)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_HN_HONG_ZHONG)) {
			// 初始化基础牌局handler
			_handler_dispath_card = new MJHandlerDispatchCard_HNHZ();
			_handler_out_card_operate = new MJHandlerOutCardOperate_HNHZ();
			_handler_gang = new MJHandlerGang_HNHZ();
			_handler_chi_peng = new MJHandlerChiPeng_HNHZ();

			_handler_qishou_hongzhong = new MJHandlerQiShouHongZhong();

			_handler_pao_henna_hz = new MJHandlerPao_HeNan_HZ();

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				_player_result.pao[i] = 0;
			}
		} else if (is_mj_type(GameConstants.GAME_TYPE_FLS_LX_CG)) {
			// 初始化基础牌局handler
			_handler_dispath_card = new MJHandlerDispatchCard_LXCG();
			_handler_out_card_operate = new MJHandlerOutCardOperate_LXCG();
			_handler_gang = new MJHandlerGang_LXCG();
			_handler_chi_peng = new MJHandlerChiPeng_LXCG();
			_handlerPiao_LXCG = new MJHandlerPiao_LXCG();
			_handlerLastCard_lxcg = new MJHandlerDispatchLastCard_LXCG();
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				_player_result.pao[i] = 0;
			}
		} else if (is_mj_type(GameConstants.GAME_TYPE_FLS_CS_LX) || is_mj_type(GameConstants.GAME_TYPE_FLS_CS_LX_DT)) {
			// 初始化基础牌局handler
			_handler_dispath_card = new MJHandlerDispatchCard_CSLX();
			_handler_out_card_operate = new MJHandlerOutCardOperate_CSLX();
			_handler_gang = new MJHandlerGang_CSLX();
			_handler_chi_peng = new MJHandlerChiPeng_CSLX();
			_handler_xiao_hu_cslx = new MJHandlerXiaoHu_CSLX();
			_handler_gang_cslx = new MJHandlerGang_CSLX_DispatchCard();
			_handler_hai_di = new MJHandlerHaiDi_CSLX();
			_handler_yao_hai_di = new MJHandlerYaoHaiDi_CSLX();
			handler_piao_cslx = new MJHandlerPiao_CSLX();
			_handler_dispatchCard_xiaohu_cslx = new MJHandlerDispatchCard_XiaoHu_CSLX();
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_LYGC)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_LUO_YANG_GANG_CI)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_LYGC_DIAN_PAO)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_HE_ZE)) {
			// 初始化河南洛阳杠次handler
			_handler_dispath_card = new MJHandlerDispatchCard_lygc();
			_handler_out_card_operate = new MJHandlerOutCardOperate_lygc();
			_handler_gang = new MJHandlerGang_lygc();
			_handler_chi_peng = new MJHandlerChiPeng_lygc();

			_hangdler_ci_lygc = new MJHandlerCi_lygc();
			_handler_pi_ci = new MJHandlerPiCi_lygc();

			int gameId = this.getGame_id() == 0 ? 1 : this.getGame_id();
			SysParamModel sysParamModel1105 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId)
					.get(1105);
			if (sysParamModel1105 != null && sysParamModel1105.getVal1() > 0 && sysParamModel1105.getVal1() <= 10) {
				game_finish_delay_lygc = sysParamModel1105.getVal1();
			}
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_ZHUAN_ZHUAN)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_SAN_MEN_XIA)) { // liuyan
																			// 2017/7/10
			_handler_dispath_card = new MJHandlerDispatchCard_HeNanZZ();
			_handler_out_card_operate = new MJHandlerOutCardOperate_HeNanZZ();
			_handler_gang = new MJHandlerGang_HeNanZZ();
			_handler_chi_peng = new MJHandlerChiPeng_HeNanZZ();
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_XY)) {
			_handler_pao_henna_xy = new MJHandlerPao_HenNanxy();
			_handler_Nao_henna_xy = new MJHandlerNao_HenNanxy();
			_handler_chi_peng = new MJHandlerChiPeng_HeNanxy();
			_handler_dispath_card = new MJHandlerDispatchCard_HeNanxy();
			_handler_gang = new MJHandlerGang_HeNanxy();
			_handler_out_card_operate = new MJHandlerOutCardOperate_HeNanxy();
			_pre_bangker_player = GameConstants.INVALID_CARD;
			_lian_zhuang_win_score = 0;
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_ZHOU_KOU)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_ZHOU_KOU)) {
			_handler_dispath_card = new MJHandlerDispatchCard_HeNan_ZhouKou();
			_handler_out_card_operate = new MJHandlerOutCardOperate_HeNan_ZhouKou();
			_handler_gang = new MJHandlerGang_HeNan_ZhouKou();
			_handler_chi_peng = new MJHandlerChiPeng_HeNan_ZhouKou();

			_handler_out_card_bao_ting = new MJHandlerOutCardBaoTing_HeNan_ZhouKou();

			_handler_qi_shou_hu = new MJHandlerQiShouHu_HeNan_ZhouKou();

			_handler_select_magic = new MJHandlerSelectMagic_HeNan_ZhouKou();

			_handler_pao = new MJHandlerPao_HeNan_ZhouKou();

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				_player_result.pao[i] = 0;
			}
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_LH) || is_mj_type(GameConstants.GAME_TYPE_NEW_LUO_HE)) {
			_handler_dispath_card = new MJHandlerDispatchCard_HeNan_lh();
			_handler_out_card_operate = new MJHandlerOutCardOperate_HeNan_lh();
			_handler_gang = new MJHandlerGang_HeNan_lh();
			_handler_chi_peng = new MJHandlerChiPeng_HeNan_lh();
			if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HENAN_HONG_ZHONG_LAI_ZI)) {
				_logic.add_magic_card_index(_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD));
			}
		}
		_handler_finish = new MJHandlerFinish();
	}

	@Override
	public void on_init_table(int game_type_index, int game_rule_index, int game_round) {
		super.on_init_table(game_type_index, game_rule_index, game_round);

		if (is_mj_type(GameConstants.GAME_TYPE_HZ) || is_mj_type(GameConstants.GAME_TYPE_HENAN_HZ)
				|| is_mj_type(GameConstants.GAME_TYPE_FLS_HZ_LX)
				|| is_mj_type(GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ)
				// 邵阳红中飞+萍乡转转
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_HONG_ZHONG_FEI)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_PING_XIANG_ZZ)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_YUYANG_HONGZHON)
				|| is_mj_type(GameConstants.GAME_TYPE_HONG_ZHONG_MJ_TH)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_HN_HONG_ZHONG))// 红中
		// 红中玩法is_mj_type(MJGameConstants.GAME_TYPE_HZ)
		{
			// 设置红中为癞子
			_logic.add_magic_card_index(_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD));
		} else {
			// _logic.add_magic_card_index(MJGameConstants.MAX_INDEX);
		}
	}

	@Override
	public void runnable_set_trustee(int _seat_index) {
		if (_trustee_schedule == null)
			return;

		if (_trustee_schedule[_seat_index] != null) {
			_trustee_schedule[_seat_index] = null;
		}

		if (_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT)
			return;

		if (is_mj_type(GameConstants.GAME_TYPE_MJ_LYGC_DIAN_PAO)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_LUO_YANG_GANG_CI)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_HE_ZE) || is_mj_type(GameConstants.GAME_TYPE_NEW_XU_CHANG)) {
			if (_handler != null) {
				if (_playerStatus[_seat_index].get_status() == GameConstants.Player_Status_OPR_CARD) {
					_handler.handler_kuai_su_chang(this, _seat_index, 0);
				} else if (_playerStatus[_seat_index].get_status() == GameConstants.Player_Status_OUT_CARD
						|| _playerStatus[_seat_index].get_status() == GameConstants.Player_Status_OPR_OR_OUT_CARD) {
					if (_handler instanceof MJHandlerDispatchCard) {
						_handler.handler_kuai_su_chang(this, _seat_index, _send_card_data);
					} else {
						_handler.handler_kuai_su_chang(this, _seat_index, 0);
					}
				}
			}
		} else {
			handler_request_trustee(_seat_index, true, 0);
		}
	}

	////////////////////////////////////////////////////////////////////////

	/**
	 * 第一轮 初始化庄家 默认第一个。需要的继承
	 */
	@Override
	protected void initBanker() {
		if (is_mj_type(GameConstants.GAME_TYPE_CS) || is_mj_type(GameConstants.GAME_TYPE_HALL_CHANG_SHA_MJ)
				|| is_mj_type(GameConstants.GAME_TYPE_THJ_CS)) {
			this.shuffle_players();

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				this.get_players()[i].set_seat_index(i);
				if (this.get_players()[i].getAccount_id() == this.getRoom_owner_account_id()) {
					this._cur_banker = i;
				}
			}
		}

		if (is_mj_type(GameConstants.GAME_TYPE_HALL_ZHUAN_ZHUAN_MJ)) {
			if (getRuleValue(GameConstants.GAME_RULE_CAN_LESS) == 1) {
				int banker = RandomUtil.getRandomNumber(getTablePlayerNumber() - 1);
				this._cur_banker = banker;
			}else if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_ALL_RANDOM_INDEX)) {
				this.shuffle_players();

				for (int i = 0; i < getTablePlayerNumber(); i++) {
					this.get_players()[i].set_seat_index(i);
					if (this.get_players()[i].getAccount_id() == this.getRoom_owner_account_id()) {
						this._cur_banker = i;
					}
				}
			} else {
				if (has_rule(GameConstants.GAME_RULE_HENAN_THREE)) {
					int banker = RandomUtil.getRandomNumber(GameConstants.GAME_PLAYER - 1);
					this._cur_banker = banker;
				} else {
					int banker = RandomUtil.getRandomNumber(GameConstants.GAME_PLAYER);
					this._cur_banker = banker;
				}
			}
		}

		if (is_mj_type(GameConstants.GAME_TYPE_HENAN) || is_mj_type(GameConstants.GAME_TYPE_HENAN_ZMD)
				|| is_mj_type(GameConstants.GAME_TYPE_HENAN_KF) || is_mj_type(GameConstants.GAME_TYPE_HENAN_NY)
				|| is_mj_type(GameConstants.GAME_TYPE_HENAN_LYGC)
				|| is_mj_type(GameConstants.GAME_TYPE_HENAN_ZHUAN_ZHUAN) || is_mj_type(GameConstants.GAME_TYPE_HENAN_XX)
				|| is_mj_type(GameConstants.GAME_TYPE_HENAN_XY) || is_mj_type(GameConstants.GAME_TYPE_HENAN_ZHOU_KOU)
				|| is_mj_type(GameConstants.GAME_TYPE_HENAN_SMX) || is_mj_type(GameConstants.GAME_TYPE_HENAN_PDS)
				|| is_mj_type(GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_HE_NAN)
				// 邵阳红中飞+萍乡转转
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_HONG_ZHONG_FEI)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_PING_XIANG_ZZ)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_ZHU_MA_DIAN)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_KAI_FENG) || is_mj_type(GameConstants.GAME_TYPE_NEW_NAN_YANG)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_SAN_MEN_XIA)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_XIN_XIANG)
				|| is_mj_type(GameConstants.GAME_TYPE_JIAO_ZUO_HUAN_HUAN)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_LUO_YANG_GANG_CI)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_PING_DING_SHAN)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_ZHOU_KOU)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_LYGC_DIAN_PAO)
				|| is_mj_type(GameConstants.GAME_TYPE_HONG_ZHONG_MJ_TH)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_YUYANG_HONGZHON)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_HE_ZE)) {

			if (getRuleValue(GameConstants.GAME_RULE_CAN_LESS) == 1) {
				int banker = RandomUtil.getRandomNumber(getTablePlayerNumber() - 1);
				this._cur_banker = banker;
			} else if (has_rule(GameConstants.GAME_RULE_HENAN_THREE)) {
				int banker = RandomUtil.getRandomNumber(GameConstants.GAME_PLAYER - 1);
				this._cur_banker = banker;
			} else {
				int banker = RandomUtil.getRandomNumber(GameConstants.GAME_PLAYER);
				this._cur_banker = banker;
			}

		}
	}

	// 游戏开始
	@Override
	protected boolean on_handler_game_start() {
		if (_cur_round == 2) {
			// real_kou_dou();// 记录真实扣豆
		}

		if (_cur_round == 0) {
			// 游戏开始时间
			gameTimer = new PerformanceTimer();
		}

		reset_init_data();

		// 庄家选择
		this.progress_banker_select();

		_game_status = GameConstants.GS_MJ_PLAY;

		// 信阳麻将
		GRR._banker_player = _cur_banker;
		_current_player = GRR._banker_player;

		if (is_mj_type(GameConstants.GAME_TYPE_HZ) || is_mj_type(GameConstants.GAME_TYPE_FLS_HZ_LX)
				|| is_mj_type(GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_YUYANG_HONGZHON)
				|| is_mj_type(GameConstants.GAME_TYPE_HONG_ZHONG_MJ_TH)
				// 邵阳红中+萍乡转转
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_HONG_ZHONG_FEI)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_PING_XIANG_ZZ)) {// 红中玩法
			// 目前只添加了湖南红中8红中玩法
			if ((is_mj_type(GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ)
					|| is_mj_type(GameConstants.GAME_TYPE_MJ_YUYANG_HONGZHON)
					|| is_mj_type(GameConstants.GAME_TYPE_HONG_ZHONG_MJ_TH)
					|| is_mj_type(GameConstants.GAME_TYPE_MJ_PING_XIANG_ZZ))
					&& GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HUNAN_HONG_ZHONG_8_HZ)) {
				_repertory_card = new int[GameConstants.CARD_DATA_HZ_8.length];
				shuffle(_repertory_card, GameConstants.CARD_DATA_HZ_8);
			} else {
				_repertory_card = new int[GameConstants.CARD_COUNT_HZ];
				shuffle(_repertory_card, GameConstants.CARD_DATA_HZ);
			}
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_AY) || is_mj_type(GameConstants.GAME_TYPE_NEW_AN_YANG)) {
			// 安阳麻将
			if (has_rule(GameConstants.GAME_RULE_HENAN_DAIFENG)) {
				_repertory_card = new int[GameConstants.CARD_COUNT_DAI_FENG_AY];
				shuffle(_repertory_card, GameConstants.CARD_DATA_DAI_FENG_AY);
			} else {
				_repertory_card = new int[GameConstants.CARD_COUNT_BU_DAI_FENG_AY];
				shuffle(_repertory_card, GameConstants.CARD_DATA_BU_DAI_FENG_AY);
			}
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_XY)) {
			// 信阳麻将
			_repertory_card = new int[GameConstants.CARD_COUNT_XY];
			shuffle(_repertory_card, GameConstants.CARD_DATA_XY);
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_SMX)) {
			// 三门峡麻将
			if (has_rule(GameConstants.GAME_RULE_HENAN_DAIFENG)) {
				_repertory_card = new int[GameConstants.CARD_COUNT_DAI_FENG_SMX];
				shuffle(_repertory_card, GameConstants.CARD_DATA_DAI_FENG_SMX);
			} else {
				_repertory_card = new int[GameConstants.CARD_COUNT_BU_DAI_FENG_SMX];
				shuffle(_repertory_card, GameConstants.CARD_DATA_BU_DAI_FENG_SMX);
			}

		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_XC) || is_mj_type(GameConstants.GAME_TYPE_NEW_XU_CHANG)) {
			if (getRuleValue(GameConstants.GAME_RULE_HENAN_BU_DAI_FENG) == 1) {
				_repertory_card = new int[GameConstants.CARD_COUNT_BU_DAI_FENG_LZ];
				shuffle(_repertory_card, GameConstants.CARD_DATA_BU_DAI_FENG_LZ);
			} else {
				_repertory_card = new int[GameConstants.CARD_COUNT_DAI_FENG_LZ];
				shuffle(_repertory_card, GameConstants.CARD_DATA_DAI_FENG_LZ);
			}
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_LZ) || is_mj_type(GameConstants.GAME_TYPE_NEW_LIN_ZHOU)) {
			// 林州
			if (has_rule(GameConstants.GAME_RULE_HENAN_DAIFENG)) {
				_repertory_card = new int[GameConstants.CARD_COUNT_DAI_FENG_LZ];
				shuffle(_repertory_card, GameConstants.CARD_DATA_DAI_FENG_LZ);
			} else {
				_repertory_card = new int[GameConstants.CARD_COUNT_BU_DAI_FENG_LZ];
				shuffle(_repertory_card, GameConstants.CARD_DATA_BU_DAI_FENG_LZ);
			}
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN) || is_mj_type(GameConstants.GAME_TYPE_HENAN_ZMD)
				|| is_mj_type(GameConstants.GAME_TYPE_HENAN_KF) || is_mj_type(GameConstants.GAME_TYPE_HENAN_NY)
				|| is_mj_type(GameConstants.GAME_TYPE_HENAN_XX) || is_mj_type(GameConstants.GAME_TYPE_HENAN_PDS)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_HE_NAN) || is_mj_type(GameConstants.GAME_TYPE_NEW_ZHU_MA_DIAN)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_KAI_FENG) || is_mj_type(GameConstants.GAME_TYPE_NEW_NAN_YANG)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_XIN_XIANG)
				|| is_mj_type(GameConstants.GAME_TYPE_JIAO_ZUO_HUAN_HUAN)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_PING_DING_SHAN)) {
			// 河南通用麻将
			if (has_rule(GameConstants.GAME_RULE_HENAN_DAIFENG)) {
				_repertory_card = new int[GameConstants.CARD_COUNT_DAI_FENG_LZ];
				shuffle(_repertory_card, GameConstants.CARD_DATA_DAI_FENG_LZ);
			} else {
				_repertory_card = new int[GameConstants.CARD_COUNT_BU_DAI_FENG_LZ];
				shuffle(_repertory_card, GameConstants.CARD_DATA_BU_DAI_FENG_LZ);
			}
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_HZ)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_HN_HONG_ZHONG)) {
			// 河南红中麻将
			if (has_rule(GameConstants.GAME_RULE_HENAN_DAIFENG)) {
				_repertory_card = new int[GameConstants.CARD_COUNT_DAI_FENG_HNHZ];
				shuffle(_repertory_card, GameConstants.CARD_DATA_DAI_FENG_LZ);
			} else {
				_repertory_card = new int[GameConstants.CARD_COUNT_BU_DAI_FENG_HNHZ];
				shuffle(_repertory_card, GameConstants.CARD_DATA_HZ);
			}
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_LYGC)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_LUO_YANG_GANG_CI)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_LYGC_DIAN_PAO)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_HE_ZE)) {
			// 洛阳杠次初始化牌组 (使用林州牌组)
			if (has_rule(GameConstants.GAME_RULE_HENAN_DAIFENG)) {
				_repertory_card = new int[GameConstants.CARD_COUNT_DAI_FENG_LZ];
				shuffle(_repertory_card, GameConstants.CARD_DATA_DAI_FENG_LZ);
			} else {
				_repertory_card = new int[GameConstants.CARD_COUNT_BU_DAI_FENG_LZ];
				shuffle(_repertory_card, GameConstants.CARD_DATA_BU_DAI_FENG_LZ);
			}
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_ZHOU_KOU)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_ZHOU_KOU)) {
			if (has_rule(GameConstants.GAME_RULE_HENAN_DAIFENG)) { // 136张
				_repertory_card = new int[GameConstants.CARD_COUNT_HE_NAN_ZHOU_KOU_DAI_FENG];
				shuffle(_repertory_card, GameConstants.CARD_DATA_DAI_FENG);
			} else if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HENAN_HONG_ZHONG_LAI_ZI)) { // 112张
				_repertory_card = new int[GameConstants.CARD_COUNT_HE_NAN_ZHOU_KOU_HONG_ZHONG_LAI_ZI];
				shuffle(_repertory_card, GameConstants.CARD_DATA_HONG_ZHONG_LAI_ZI);
			} else { // 108张
				_repertory_card = new int[GameConstants.CARD_COUNT_HE_NAN_ZHOU_KOU];
				shuffle(_repertory_card, GameConstants.CARD_DATA_WAN_TIAO_TONG);
			}
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_LH) || is_mj_type(GameConstants.GAME_TYPE_NEW_LUO_HE)) {
			if (has_rule(GameConstants.GAME_RULE_HENAN_DAIFENG)) { // 136张
				_repertory_card = new int[GameConstants.CARD_COUNT_HE_NAN_ZHOU_KOU_DAI_FENG];
				shuffle(_repertory_card, GameConstants.CARD_DATA_DAI_FENG);
			} else if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HENAN_HONG_ZHONG_LAI_ZI)) { // 112张
				_repertory_card = new int[GameConstants.CARD_COUNT_HE_NAN_ZHOU_KOU_HONG_ZHONG_LAI_ZI];
				shuffle(_repertory_card, GameConstants.CARD_DATA_HONG_ZHONG_LAI_ZI);
			} else { // 108张
				_repertory_card = new int[GameConstants.CARD_COUNT_HE_NAN_ZHOU_KOU];
				shuffle(_repertory_card, GameConstants.CARD_DATA_WAN_TIAO_TONG);
			}
		} else if (is_mj_type(GameConstants.GAME_TYPE_HALL_ZHUAN_ZHUAN_MJ)) {
			if (has_rule(GameConstants.GAME_RULE_HUNAN_HONGZHONG)) {
				_repertory_card = new int[GameConstants.CARD_COUNT_HZ];
				shuffle(_repertory_card, GameConstants.CARD_DATA_HZ);
			} else {
				_repertory_card = new int[GameConstants.CARD_COUNT_HU_NAN];
				shuffle(_repertory_card, GameConstants.CARD_DATA_HU_NAN);
			}
		} else {
			// 晃晃麻将也可以用湖南麻将的牌（推倒胡）
			_repertory_card = new int[GameConstants.CARD_COUNT_HU_NAN];
			shuffle(_repertory_card, GameConstants.CARD_DATA_HU_NAN);
		}

		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();

		getLocationTip();

		try {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				for (int j = 0; j < GRR._cards_index[i].length; j++) {
					if (GRR._cards_index[i][j] == 4) {
						MongoDBServiceImpl.getInstance().card_log(this.get_players()[i], ECardType.anLong, "",
								GRR._cards_index[i][j], 0l, this.getRoom_id());
					}
				}
			}
		} catch (Exception e) {

		}
		// 游戏开始时 初始化 未托管
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			istrustee[i] = false;
		}

		on_game_start();
		//再次同步一下托管状态
		for(int i = 0; i < getTablePlayerNumber(); i++){
			be_in_room_trustee_match(i);
		}
		return false;
	}

	@Override
	protected boolean on_game_start() {

		if (is_mj_type(GameConstants.GAME_TYPE_ZZ) || is_mj_type(GameConstants.GAME_TYPE_HENAN_ZHUAN_ZHUAN)
				|| is_mj_type(GameConstants.GAME_TYPE_HALL_ZHUAN_ZHUAN_MJ)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_SAN_MEN_XIA)) { // liuyan
																			// 2017/7/10
			return game_start_zz();
		} else if (is_mj_type(GameConstants.GAME_TYPE_HZ) || is_mj_type(GameConstants.GAME_TYPE_FLS_HZ_LX)
				|| is_mj_type(GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_YUYANG_HONGZHON)
				|| is_mj_type(GameConstants.GAME_TYPE_HONG_ZHONG_MJ_TH)
				// 邵阳红中+萍乡转转
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_HONG_ZHONG_FEI)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_PING_XIANG_ZZ)) {
			return game_start_hz();
		} else if (is_mj_type(GameConstants.GAME_TYPE_CS) || is_mj_type(GameConstants.GAME_TYPE_HALL_CHANG_SHA_MJ)
				|| is_mj_type(GameConstants.GAME_TYPE_THJ_CS)) {
			return game_start_cs();
		} else if (is_mj_type(GameConstants.GAME_TYPE_SHUANGGUI)
				|| is_mj_type(GameConstants.GAME_TYPE_HUNAN_SHANG_XIA_GUI)) {
			return game_start_sg();
		} else if (is_mj_type(GameConstants.GAME_TYPE_ZHUZHOU)) {
			return game_start_zhuzhou();
		} else if (is_mj_type(GameConstants.GAME_TYPE_XTHH)) {
			return game_start_xthh();
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_AY) || is_mj_type(GameConstants.GAME_TYPE_NEW_AN_YANG)) {
			return game_start_ay();
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_LZ) || is_mj_type(GameConstants.GAME_TYPE_NEW_LIN_ZHOU)) {
			return game_start_lz();
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN) || is_mj_type(GameConstants.GAME_TYPE_HENAN_ZMD)
				|| is_mj_type(GameConstants.GAME_TYPE_HENAN_KF) || is_mj_type(GameConstants.GAME_TYPE_HENAN_NY)
				|| is_mj_type(GameConstants.GAME_TYPE_HENAN_XX) || is_mj_type(GameConstants.GAME_TYPE_HENAN_PDS)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_HE_NAN) || is_mj_type(GameConstants.GAME_TYPE_NEW_ZHU_MA_DIAN)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_KAI_FENG) || is_mj_type(GameConstants.GAME_TYPE_NEW_NAN_YANG)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_XIN_XIANG)
				|| is_mj_type(GameConstants.GAME_TYPE_JIAO_ZUO_HUAN_HUAN)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_PING_DING_SHAN)) {
			return game_start_henan();
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_HZ)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_HN_HONG_ZHONG)) {
			return game_start_hnhz();
		} else if (is_mj_type(GameConstants.GAME_TYPE_FLS_LX_CG)) {
			return game_start_lxcg();
		} else if (is_mj_type(GameConstants.GAME_TYPE_FLS_CS_LX) || is_mj_type(GameConstants.GAME_TYPE_FLS_CS_LX_DT)) {
			return game_start_cs_lx();
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_XC) || is_mj_type(GameConstants.GAME_TYPE_NEW_XU_CHANG)) {
			return game_start_henan_xc();
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_LYGC)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_LUO_YANG_GANG_CI)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_LYGC_DIAN_PAO)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_HE_ZE)) {
			return game_start_henan_lygc();
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_XY)) {
			return game_start_xy();
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_ZHOU_KOU)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_ZHOU_KOU)) {
			return game_start_he_nan_zhou_kou();
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_LH) || is_mj_type(GameConstants.GAME_TYPE_NEW_LUO_HE)) {
			return game_start_henan_lh();
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_SMX)) {
			return game_start_smx();
		}
		return false;
	}

	/// 洗牌
	@Override
	public void shuffle(int repertory_card[], int mj_cards[]) {
		_all_card_len = repertory_card.length;
		GRR._left_card_count = _all_card_len;

		int xi_pai_count = 0;
		int rand = RandomUtil.generateRandomNumber(3, 6);

		while (xi_pai_count < 6 && xi_pai_count < rand) {
			if (xi_pai_count == 0)
				_logic.random_card_data(repertory_card, mj_cards);
			else
				_logic.random_card_data(repertory_card, repertory_card);

			xi_pai_count++;
		}

		int send_count;
		int have_send_count = 0;

		/*
		 * int count = (has_rule(GameConstants.GAME_RULE_HUNAN_THREE) |
		 * has_rule(GameConstants.GAME_RULE_HENAN_THREE)) ?
		 * GameConstants.GAME_PLAYER - 1 : GameConstants.GAME_PLAYER;
		 */
		int count = getTablePlayerNumber();
		// 分发扑克
		for (int i = 0; i < count; i++) {
			// if(GRR._banker_player == i){
			// send_count = MJGameConstants.MAX_COUNT;
			// }else{
			//
			// send_count = (MJGameConstants.MAX_COUNT - 1);
			// }
			send_count = (GameConstants.MAX_COUNT - 1);
			GRR._left_card_count -= send_count;

			// 一人13张牌,庄家多一张
			_logic.switch_to_cards_index(repertory_card, have_send_count, send_count, GRR._cards_index[i]);

			have_send_count += send_count;

		}
		// 记录初始牌型
		if (_recordRoomRecord != null) {
			_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));
		}
	}

	@Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] {0x12,0x12,0x15,0x27,0x27,0x28,0x28,0x29,0x29,0x31,0x31,0x33,0x33};
		/*
		 * int[] cards_of_player1 = new int[] { 0x09, 0x09, 0x08, 0x13, 0x14,
		 * 0x15, 0x15, 0x15, 0x22, 0x22, 0x35, 0x35, 0x35 }; int[]
		 * cards_of_player3 = new int[] { 0x11, 0x11, 0x12, 0x13, 0x14, 0x15,
		 * 0x15, 0x15, 0x22, 0x22, 0x35, 0x35, 0x35 }; int[] cards_of_player2 =
		 * new int[] { 0x11, 0x11, 0x12, 0x13, 0x14, 0x15, 0x15, 0x15, 0x22,
		 * 0x22, 0x35, 0x35, 0x35 };
		 */

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}

		for (int j = 0; j < 13; j++) {
			if (getTablePlayerNumber() == 4) {
				GRR._cards_index[0][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[1][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[2][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[3][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
			} else if (getTablePlayerNumber() == 3) {
				GRR._cards_index[0][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[1][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[2][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
			} else if (getTablePlayerNumber() == 2) {
				GRR._cards_index[0][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[1][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
			}
		}

//		int[] temps1 = new int[]{22, 19, 38, 8, 23, 17, 5, 9, 36, 39, 17, 41, 4, 24, 36, 5, 17, 25, 5, 7, 22, 22, 5, 25, 35, 22, 41, 9, 1, 19, 20, 38, 4, 34, 3, 37, 2, 4, 40, 23, 17, 37, 40, 39, 18, 25, 20, 7, 8, 1, 3, 36, 1, 18, 33, 37, 3, 2, 6, 21, 9, 18, 8, 21, 18, 20, 33, 39, 35, 36, 34, 23, 38, 6, 39, 8, 21, 23, 35, 2, 40, 33, 35, 1, 38, 33, 24, 24, 34, 25, 34, 9, 40, 37, 19, 6, 20, 6, 24, 2, 7, 21, 19, 41, 7, 41, 3, 4};
//		testRealyCard(temps1);
		if (BACK_DEBUG_CARDS_MODE) {
			if (debug_my_cards != null) {
				if (debug_my_cards.length > 14) {
					int[] temps = new int[debug_my_cards.length];
					System.arraycopy(debug_my_cards, 0, temps, 0, temps.length);
					testRealyCard(temps);
					debug_my_cards = null;
				} else {
					int[] temps = new int[debug_my_cards.length];
					System.arraycopy(debug_my_cards, 0, temps, 0, temps.length);
					testSameCard(temps);
					debug_my_cards = null;
				}
			}
		}
	}

	public void testChangeCard() {
		if (BACK_DEBUG_CARDS_MODE) {
			if (debug_my_cards != null) {
				int[] temps = new int[debug_my_cards.length];
				System.arraycopy(debug_my_cards, 0, temps, 0, temps.length);
				testSameCard(temps);
				debug_my_cards = null;
			}
		}
	}

	private boolean game_start_he_nan_zhou_kou() {
		_logic.clean_magic_cards();

		if (has_rule(GameConstants.GAME_RULE_HENAN_DAI_PAO)) {
			this.set_handler(this._handler_pao);
			this._handler_pao.exe(this);
			return true;
		} else {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				_player_result.pao[i] = 0;
				_player_result.qiang[i] = 0;
			}
		}

		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		// gameStartResponse.setSiceIndex(rand);
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants.MAX_COUNT];
		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			// 只发自己的牌
			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			// 回放数据
			GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.load_room_info_data(roomResponse);
			this.load_common_status(roomResponse);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse
					.setCurrentPlayer(_current_player == GameConstants.INVALID_SEAT ? _resume_player : _current_player);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			roomResponse.setGameStatus(_game_status);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			this.send_response_to_player(i, roomResponse);
		}
		////////////////////////////////////////////////// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		this.load_room_info_data(roomResponse);
		this.load_common_status(roomResponse);
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}

		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(GRR._left_card_count);
		GRR.add_room_response(roomResponse);
		///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		exe_select_magic_card(GRR._banker_player);

		// 检测听牌
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i]._hu_card_count = this.get_henan_zhou_kou_ting_card(_playerStatus[i]._hu_cards,
					GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i], false, i);
			if (_playerStatus[i]._hu_card_count > 0) {
				this.operate_chi_hu_cards(i, _playerStatus[i]._hu_card_count, _playerStatus[i]._hu_cards);
			}
		}

		boolean is_qishou_hu = false;
		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HENAN_HONG_ZHONG_LAI_ZI)
				|| has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN)) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				// 起手4个红中
				if (GRR._cards_index[i][_logic.get_magic_card_index(0)] == 4) {

					_playerStatus[i].add_action(GameConstants.WIK_ZI_MO);
					_playerStatus[i].add_zi_mo(_logic.switch_to_card_data(_logic.get_magic_card_index(0)), i);
					GRR._chi_hu_rights[i].opr_or(GameConstants.CHR_ZI_MO);
					GRR._chi_hu_rights[i].opr_or(GameConstants.CHR_HENAN_ZHOU_KOU_QI_SHOU_HU);
					this.exe_qi_shou_hu(i);

					is_qishou_hu = true;
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[i], ECardType.hongZhong4, "", 0, 0l,
							this.getRoom_id());
					break;
				}
			}
		}

		if (is_qishou_hu == false) {
			this.exe_dispatch_card(_current_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);
		}

		// this.exe_dispatch_card(_current_player,true);

		return true;
	}

	// 开始转转麻将
	private boolean game_start_zz() {
		

		hu_score = new int[getTablePlayerNumber()];// 麻将胡分统计
		niao_score = new int[getTablePlayerNumber()];// 麻将鸟分统计
		piao_score = new int[getTablePlayerNumber()];// 麻将飘分显示
		hu_type = new int[getTablePlayerNumber()]; // 麻将胡牌类型
		
		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		// gameStartResponse.setSiceIndex(rand);
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants.MAX_COUNT];
		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			// 只发自己的牌
			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			// 回放数据
			GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.load_room_info_data(roomResponse);
			this.load_common_status(roomResponse);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);

			if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_ALL_RANDOM_INDEX)) {
				this.load_room_info_data(roomResponse);
				this.load_common_status(roomResponse);

				if (this._cur_round == 1) {
					this.load_player_info_data(roomResponse);
				}
			}

			roomResponse.setGameStart(gameStartResponse);
			roomResponse
					.setCurrentPlayer(_current_player == GameConstants.INVALID_SEAT ? _resume_player : _current_player);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			roomResponse.setGameStatus(_game_status);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			this.send_response_to_player(i, roomResponse);
		}
		////////////////////////////////////////////////// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		this.load_room_info_data(roomResponse);
		this.load_common_status(roomResponse);
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}

		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(GRR._left_card_count);
		GRR.add_room_response(roomResponse);
		///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		// 检测听牌
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i]._hu_card_count = this.get_zz_ting_card_new(_playerStatus[i]._hu_cards, GRR._cards_index[i],
					GRR._weave_items[i], GRR._weave_count[i], false);
			if (_playerStatus[i]._hu_card_count > 0) {
				this.operate_chi_hu_cards(i, _playerStatus[i]._hu_card_count, _playerStatus[i]._hu_cards);
			}
		}

		boolean is_qishou_hu = false;

		if (has_rule(GameConstants.GAME_RULE_HUNAN_HONGZHONG)) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				// 起手4个红中
				if (GRR._cards_index[i][_logic.get_magic_card_index(0)] == 4) {

					_playerStatus[i].add_action(GameConstants.WIK_ZI_MO);
					_playerStatus[i].add_zi_mo(_logic.switch_to_card_data(_logic.get_magic_card_index(0)), i);
					GRR._chi_hu_rights[i].opr_or(GameConstants.CHR_ZI_MO);
					GRR._chi_hu_rights[i].opr_or(GameConstants.CHR_HUNAN_HZ_QISHOU_HU);
					this.exe_qishou_hongzhong(i);

					is_qishou_hu = true;
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[i], ECardType.hongZhong4, "", 0, 0l,
							this.getRoom_id());
					break;
				}
			}
		}

		if (is_qishou_hu == false) {
			this.exe_dispatch_card(GRR._banker_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);
		}

		return false;
	}

	// 开始红中麻将
	private boolean game_start_hz() {
		send_game_end = false;
//		Player[] players = this.get_players();
//		for (Player player : players) {
//			if (player == null) {
//				continue;
//			}
//			player.enableRobot(this._game_type_index, this);
//		}
		
		// 3D红中麻将 不至0
		if (is_mj_type(GameConstants.GAME_TYPE_HONG_ZHONG_MJ_TH)) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				_player_result.pao[i] = -1;
			}
		} else {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				_player_result.pao[i] = 0;
			}
		}

		// 红中麻将添加飘分
		if (has_rule(GameConstants.GAME_RULE_HUNAN_CS_PIAO)) {
			set_handler(_handler_piao);
			_handler_piao.exe(this);
			return true;
		}
		return on_game_start_hz_real();
	}

	public boolean on_game_start_hz_real() {
		hu_score = new int[getTablePlayerNumber()];// 红中麻将胡分统计
		niao_score = new int[getTablePlayerNumber()];// 红中麻将鸟分统计
		piao_score = new int[getTablePlayerNumber()];// 红中麻将飘分显示
		hu_type = new int[getTablePlayerNumber()]; // 红中麻将胡牌类型

		_game_status = GameConstants.GS_MJ_PLAY;

		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		// gameStartResponse.setSiceIndex(rand);
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants.MAX_COUNT];
		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			// 只发自己的牌
			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			// 回放数据
			GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.load_room_info_data(roomResponse);
			this.load_common_status(roomResponse);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse
					.setCurrentPlayer(_current_player == GameConstants.INVALID_SEAT ? _resume_player : _current_player);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			roomResponse.setGameStatus(_game_status);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			this.send_response_to_player(i, roomResponse);
		}
		////////////////////////////////////////////////// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		this.load_room_info_data(roomResponse);
		this.load_common_status(roomResponse);
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}

		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(GRR._left_card_count);
		GRR.add_room_response(roomResponse);
		///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		// 检测听牌
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i]._hu_card_count = this.get_hz_ting_card(_playerStatus[i]._hu_cards, GRR._cards_index[i],
					GRR._weave_items[i], GRR._weave_count[i], false);
			if (_playerStatus[i]._hu_card_count > 0) {
				this.operate_chi_hu_cards(i, _playerStatus[i]._hu_card_count, _playerStatus[i]._hu_cards);
			}
		}
		boolean is_qishou_hu = false;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 起手4个红中
			int max_hz = 4;
			if ((is_mj_type(GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ)
					|| is_mj_type(GameConstants.GAME_TYPE_MJ_YUYANG_HONGZHON)
					|| is_mj_type(GameConstants.GAME_TYPE_HONG_ZHONG_MJ_TH)
					|| is_mj_type(GameConstants.GAME_TYPE_MJ_PING_XIANG_ZZ))
					&& GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HUNAN_HONG_ZHONG_8_HZ))
				max_hz = 8;
			if (GRR._cards_index[i][_logic.get_magic_card_index(0)] == max_hz) {

				_playerStatus[i].add_action(GameConstants.WIK_ZI_MO);
				_playerStatus[i].add_zi_mo(_logic.switch_to_card_data(_logic.get_magic_card_index(0)), i);
				GRR._chi_hu_rights[i].opr_or(GameConstants.CHR_ZI_MO);
				GRR._chi_hu_rights[i].opr_or(GameConstants.CHR_HUNAN_HZ_QISHOU_HU);
				this.exe_qishou_hongzhong(i);

				is_qishou_hu = true;
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[i], ECardType.hongZhong4, "", 0, 0l,
						this.getRoom_id());
				break;
			}
		}
		if (is_qishou_hu == false) {
			this.exe_dispatch_card(GRR._banker_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);
		}
		// this.exe_dispatch_card(_current_player,true);

		return false;
	}

	// 开始河南红中麻将
	private boolean game_start_hnhz() {

		if (has_rule(GameConstants.GAME_RULE_HENAN_DAI_PAO)) {
			this.set_handler(this._handler_pao_henna_hz);
			this._handler_pao_henna_hz.exe(this);
			return true;
		} else {
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				_player_result.pao[i] = 0;// 清掉 默认是-1
			}
		}

		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		// gameStartResponse.setSiceIndex(rand);
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[GameConstants.GAME_PLAYER][GameConstants.MAX_COUNT];
		// 发送数据
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		// 发送数据
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			// 只发自己的牌
			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			// 回放数据
			GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.load_room_info_data(roomResponse);
			this.load_common_status(roomResponse);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse
					.setCurrentPlayer(_current_player == GameConstants.INVALID_SEAT ? _resume_player : _current_player);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			roomResponse.setGameStatus(_game_status);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			this.send_response_to_player(i, roomResponse);
		}
		////////////////////////////////////////////////// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		this.load_room_info_data(roomResponse);
		this.load_common_status(roomResponse);
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}

		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(GRR._left_card_count);
		GRR.add_room_response(roomResponse);
		///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		// 检测听牌
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			_playerStatus[i]._hu_card_count = this.get_hnhz_ting_card_new(_playerStatus[i]._hu_cards,
					GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i], false);
			if (_playerStatus[i]._hu_card_count > 0) {
				this.operate_chi_hu_cards(i, _playerStatus[i]._hu_card_count, _playerStatus[i]._hu_cards);
			}
		}
		boolean is_qishou_hu = false;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			// 起手4个红中
			if (GRR._cards_index[i][_logic.get_magic_card_index(0)] == 4) {

				_playerStatus[i].add_action(GameConstants.WIK_ZI_MO);
				_playerStatus[i].add_zi_mo(_logic.switch_to_card_data(_logic.get_magic_card_index(0)), i);
				GRR._chi_hu_rights[i].opr_or(GameConstants.CHR_ZI_MO);
				GRR._chi_hu_rights[i].opr_or(GameConstants.CHR_HENAN_HZ_QISHOU_HU);
				this.exe_qishou_hongzhong(i);

				is_qishou_hu = true;
				break;
			}
		}
		if (is_qishou_hu == false) {
			this.exe_dispatch_card(_current_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);
		}
		// this.exe_dispatch_card(_current_player,true);

		return false;
	}

	public boolean game_starte_real_lxcg() {
		_game_status = GameConstants.GS_MJ_PLAY;// 设置状态

		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		// gameStartResponse.setSiceIndex(rand);
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[GameConstants.GAME_PLAYER][GameConstants.MAX_COUNT];
		// 发送数据
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		// 发送数据
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			// 只发自己的牌
			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			// 回放数据
			GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.load_room_info_data(roomResponse);
			this.load_common_status(roomResponse);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse
					.setCurrentPlayer(_current_player == GameConstants.INVALID_SEAT ? _resume_player : _current_player);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			roomResponse.setGameStatus(_game_status);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			this.send_response_to_player(i, roomResponse);

		}
		////////////////////////////////////////////////// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		this.load_room_info_data(roomResponse);
		this.load_common_status(roomResponse);
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}

		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(GRR._left_card_count);
		GRR.add_room_response(roomResponse);
		///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		// 检测听牌
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			_playerStatus[i]._hu_card_count = this.get_lxcg_ting_card(_playerStatus[i]._hu_cards, GRR._cards_index[i],
					GRR._weave_items[i], GRR._weave_count[i], i);
			if (_playerStatus[i]._hu_card_count > 0) {
				this.operate_chi_hu_cards(i, _playerStatus[i]._hu_card_count, _playerStatus[i]._hu_cards);
			}
		}
		this.exe_dispatch_card(_current_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);
		return false;
	}

	// 开始临湘炒股
	private boolean game_start_lxcg() {

		if (has_rule(GameConstants.GAME_RULE_LIXIANG_FLS_PIAO)) {
			this.set_handler(this._handlerPiao_LXCG);
			this._handlerPiao_LXCG.exe(this);
			return true;
		} else {
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				_player_result.pao[i] = 0;// 清掉 默认是-1
			}
		}
		return game_starte_real_lxcg();

	}

	// 开始转转麻将
	private boolean game_start_sg() {
		_logic.clean_magic_cards();

		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		// gameStartResponse.setSiceIndex(rand);
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[GameConstants.GAME_PLAYER][GameConstants.MAX_COUNT];
		// 发送数据
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		// 发送数据
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			// 只发自己的牌
			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			// 回放数据
			GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.load_room_info_data(roomResponse);
			this.load_common_status(roomResponse);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse
					.setCurrentPlayer(_current_player == GameConstants.INVALID_SEAT ? _resume_player : _current_player);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			roomResponse.setGameStatus(_game_status);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			this.send_response_to_player(i, roomResponse);
		}
		////////////////////////////////////////////////// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		this.load_room_info_data(roomResponse);
		this.load_common_status(roomResponse);
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}

		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(GRR._left_card_count);
		GRR.add_room_response(roomResponse);
		///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		// 延迟调度定位牌

		runnable_ding_gui(GRR._banker_player);
		// GameSchedule.put(new DingGuiRunnable(this.getRoom_id(),
		// GRR._banker_player), 1200, TimeUnit.MILLISECONDS);

		return false;
	}

	// 开始株洲麻将
	private boolean game_start_zhuzhou() {

		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		// gameStartResponse.setSiceIndex(rand);
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[GameConstants.GAME_PLAYER][GameConstants.MAX_COUNT];
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}
		// 发送数据
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			// 只发自己的牌
			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);

				cards.addItem(hand_cards[i][j]);
			}
			// 回放数据
			GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			this.load_room_info_data(roomResponse);
			this.load_common_status(roomResponse);

			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setLeftCardCount(GRR._left_card_count);

			this.send_response_to_player(i, roomResponse);
		}

		////////////////////////////////////////////////////////////////////////////////////////////////
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		this.load_room_info_data(roomResponse);
		this.load_common_status(roomResponse);
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}

		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(GRR._left_card_count);
		GRR.add_room_response(roomResponse);
		// this.exe_tian_hu(_current_player);

		this.exe_dispatch_card(_current_player, GameConstants.DispatchCard_Type_Tian_Hu, 0);
		return true;
	}

	/**
	 * // 开始仙桃晃晃麻将
	 * 
	 * @return
	 */
	private boolean game_start_xthh() {
		_logic.clean_magic_cards();

		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		// gameStartResponse.setSiceIndex(rand);
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[GameConstants.GAME_PLAYER][GameConstants.MAX_COUNT];
		// 发送数据
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		// 发送数据
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			// 只发自己的牌
			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			// 回放数据
			GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.load_room_info_data(roomResponse);
			this.load_common_status(roomResponse);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse
					.setCurrentPlayer(_current_player == GameConstants.INVALID_SEAT ? _resume_player : _current_player);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			roomResponse.setGameStatus(_game_status);
			this.send_response_to_player(i, roomResponse);
		}
		////////////////////////////////////////////////// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		this.load_room_info_data(roomResponse);
		this.load_common_status(roomResponse);
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}

		// 检测听牌
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			this._playerStatus[i]._hu_card_count = this.get_xthh_ting_card(this._playerStatus[i]._hu_cards,
					this.GRR._cards_index[i], this.GRR._weave_items[i], this.GRR._weave_count[i], i);
			if (this._playerStatus[i]._hu_card_count > 0) {
				this.operate_chi_hu_cards(i, this._playerStatus[i]._hu_card_count, this._playerStatus[i]._hu_cards);
			}
		}

		// GRR._left_card_count=6;
		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(GRR._left_card_count);
		GRR.add_room_response(roomResponse);

		///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		// 延迟调度定位牌
		runnable_start_lai_gen(GRR._banker_player);
		// GameSchedule.put(new StartLaiGenRunnable(this.getRoom_id(),
		// GRR._banker_player), 1200, TimeUnit.MILLISECONDS);

		return false;
	}

	/**
	 * 开始 安阳麻将
	 * 
	 * @return
	 */
	private boolean game_start_ay() {

		GRR._banker_player = _current_player = GameConstants.INVALID_SEAT;

		this.set_handler(this._handler_pao_qiang);
		this._handler_pao_qiang.exe(this);

		return false;
	}

	/**
	 * 开始 信阳麻将
	 * 
	 * @return
	 */
	private boolean game_start_xy() {

		_logic.clean_magic_cards();
		_is_xy_nao_zhuang = false;
		// 跑分
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_result.pao[i] = 0;// 清掉 默认是-1
		}
		// 闹庄
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_result.nao[i] = 0;
		}
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			GRR._xianchu_count[i] = 0;
			GRR._dispatch_count[i] = 0;
			GRR._nao_win_score[i] = 0;
			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				GRR._xianchu_cards[i][j] = GameConstants.INVALID_CARD;
			}
		}
		if (_pre_bangker_player == _cur_banker) {
			this.set_handler(this._handler_Nao_henna_xy);
			this._handler.exe(this);
			return true;
		} else if (has_rule(GameConstants.GAME_RULE_HENAN_DAI_PAO)) {
			this.set_handler(this._handler_pao_henna_xy);
			this._handler.exe(this);
			return true;
		} else {

		}
		this.GRR._banker_player = this._current_player = this._pre_bangker_player = this._cur_banker;
		// 游戏开始
		this._game_status = GameConstants.GS_MJ_PLAY;// 设置状态
		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		// gameStartResponse.setSiceIndex(rand);
		gameStartResponse.setBankerPlayer(this.GRR._banker_player);
		gameStartResponse.setCurrentPlayer(this._current_player);
		gameStartResponse.setLeftCardCount(this.GRR._left_card_count);

		int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants.MAX_COUNT];
		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int hand_card_count = this._logic.switch_to_cards_data(this.GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			// 只发自己的牌
			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				if (_player_result.nao[i] > 0 && j <= 11) {
					hand_cards[i][j] += 8000;
				}
				if (_player_result.nao[i] > 0) {
					GRR._xianchu_count[i] = 1;
				}

				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			// 回放数据
			this.GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.load_room_info_data(roomResponse);
			this.load_common_status(roomResponse);

			if (this._cur_round == 1) {
				// shuffle_players();
				this.load_player_info_data(roomResponse);
			}
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setCurrentPlayer(
					this._current_player == GameConstants.INVALID_SEAT ? this._resume_player : this._current_player);
			roomResponse.setLeftCardCount(this.GRR._left_card_count);
			roomResponse.setGameStatus(this._game_status);
			roomResponse.setLeftCardCount(this.GRR._left_card_count);
			this.send_response_to_player(i, roomResponse);
		}
		////////////////////////////////////////////////// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		this.load_room_info_data(roomResponse);
		this.load_common_status(roomResponse);
		this.load_player_info_data(roomResponse);
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}

		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(this.GRR._left_card_count);
		this.GRR.add_room_response(roomResponse);

		// 检测听牌
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			this._playerStatus[i]._hu_card_count = this.get_henanxy_ting_card(this._playerStatus[i]._hu_cards,
					this.GRR._cards_index[i], this.GRR._weave_items[i], this.GRR._weave_count[i]);
			if (this._playerStatus[i]._hu_card_count > 0) {
				this.operate_chi_hu_cards(i, this._playerStatus[i]._hu_card_count, this._playerStatus[i]._hu_cards);
			}
		}

		this.exe_dispatch_card(_current_player, GameConstants.WIK_NULL, 0);

		return true;
	}

	/**
	 * 开始 三门峡麻将
	 * 
	 * @return
	 */
	private boolean game_start_smx() {

		_logic.clean_magic_cards();

		if (has_rule(GameConstants.GAME_RULE_HENAN_DAI_PAO)) {
			this.set_handler(this._handler_pao_henna);
			this._handler_pao_henna.exe(this);
			return true;
		} else {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				_player_result.pao[i] = 0;// 清掉 默认是-1
			}
		}

		this.GRR._banker_player = this._current_player = this._cur_banker;
		// 游戏开始
		this._game_status = GameConstants.GS_MJ_PLAY;// 设置状态
		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		// gameStartResponse.setSiceIndex(rand);
		gameStartResponse.setBankerPlayer(this.GRR._banker_player);
		gameStartResponse.setCurrentPlayer(this._current_player);
		gameStartResponse.setLeftCardCount(this.GRR._left_card_count);

		int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants.MAX_COUNT];
		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int hand_card_count = this._logic.switch_to_cards_data(this.GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			// 只发自己的牌
			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			// 回放数据
			this.GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.load_room_info_data(roomResponse);
			this.load_common_status(roomResponse);

			if (this._cur_round == 1) {
				// shuffle_players();
				this.load_player_info_data(roomResponse);
			}
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setCurrentPlayer(
					this._current_player == GameConstants.INVALID_SEAT ? this._resume_player : this._current_player);
			roomResponse.setLeftCardCount(this.GRR._left_card_count);
			roomResponse.setGameStatus(this._game_status);
			roomResponse.setLeftCardCount(this.GRR._left_card_count);
			this.send_response_to_player(i, roomResponse);
		}
		////////////////////////////////////////////////// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		this.load_room_info_data(roomResponse);
		this.load_common_status(roomResponse);
		this.load_player_info_data(roomResponse);
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}

		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(this.GRR._left_card_count);
		this.GRR.add_room_response(roomResponse);

		if (has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN)) {
			exe_hun(this.GRR._banker_player);
			return true;
		}

		// 检测听牌
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			this._playerStatus[i]._hu_card_count = this.get_henan_ting_card(this._playerStatus[i]._hu_cards,
					this.GRR._cards_index[i], this.GRR._weave_items[i], this.GRR._weave_count[i]);
			if (this._playerStatus[i]._hu_card_count > 0) {
				this.operate_chi_hu_cards(i, this._playerStatus[i]._hu_card_count, this._playerStatus[i]._hu_cards);
			}
		}

		this.exe_dispatch_card(_current_player, GameConstants.WIK_NULL, 0);

		return true;
	}

	/**
	 * 开始 林州麻将
	 * 
	 * @return
	 */
	private boolean game_start_lz() {

		GRR._banker_player = _current_player = GameConstants.INVALID_SEAT;

		this.set_handler(this._handler_pao);
		this._handler_pao.exe(this);

		return false;
	}

	/**
	 * 开始 许昌麻将
	 */
	private boolean game_start_henan_xc() {
		_logic.clean_magic_cards();

		if (has_rule(GameConstants.GAME_RULE_HENAN_DAI_PAO)) {
			this.set_handler(this._handler_pao_henna);
			this._handler_pao_henna.exe(this);
			return true;
		} else {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				_player_result.pao[i] = 0;
			}
		}

		this.GRR._banker_player = this._current_player = this._cur_banker;
		this._game_status = GameConstants.GS_MJ_PLAY;

		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(this.GRR._banker_player);
		gameStartResponse.setCurrentPlayer(this._current_player);
		gameStartResponse.setLeftCardCount(this.GRR._left_card_count);

		int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants.MAX_COUNT];
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int hand_card_count = this._logic.switch_to_cards_data(this.GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			this.GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.load_room_info_data(roomResponse);
			this.load_common_status(roomResponse);

			if (this._cur_round == 1) {
				this.load_player_info_data(roomResponse);
			}

			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setCurrentPlayer(
					this._current_player == GameConstants.INVALID_SEAT ? this._resume_player : this._current_player);
			roomResponse.setLeftCardCount(this.GRR._left_card_count);
			roomResponse.setGameStatus(this._game_status);
			roomResponse.setLeftCardCount(this.GRR._left_card_count);
			this.send_response_to_player(i, roomResponse);
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		this.load_room_info_data(roomResponse);
		this.load_common_status(roomResponse);
		this.load_player_info_data(roomResponse);
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}

		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(this.GRR._left_card_count);
		this.GRR.add_room_response(roomResponse);

		if (has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN)) {
			exe_hun(this.GRR._banker_player);
			return true;
		}

		if (getRuleValue(GameConstants.GAME_RULE_HENAN_BAO_TING) != 1) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				xc_analyse_type = XC_ANALYSE_TING;

				this._playerStatus[i]._hu_card_count = this.get_xc_ting_card(i, this._playerStatus[i]._hu_cards,
						this.GRR._cards_index[i], this.GRR._weave_items[i], this.GRR._weave_count[i]);
				if (this._playerStatus[i]._hu_card_count > 0) {
					this.operate_chi_hu_cards(i, this._playerStatus[i]._hu_card_count, this._playerStatus[i]._hu_cards);
				}
			}
		}

		this.exe_dispatch_card(_current_player, GameConstants.WIK_NULL, 0);

		return true;
	}

	/**
	 * 开始 河南麻将
	 * 
	 * @return
	 */
	private boolean game_start_henan() {
		_logic.clean_magic_cards();
		// WalkerGeek 测试
		// if (this.DEBUG_CARDS_MODE){
//		Player[] players = this.get_players();
//		for (Player player : players) {
//			if (player == null) {
//				continue;
//			}
//			player.enableRobot(this._game_type_index, this);
//		}
		// }

		if (has_rule(GameConstants.GAME_RULE_HENAN_DAI_PAO)) {
			this.set_handler(this._handler_pao_henna);
			this._handler_pao_henna.exe(this);
			return true;
		} else {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				_player_result.pao[i] = 0;// 清掉 默认是-1
				_player_result.qiang[i] = 0;// 清掉 默认是-1
			}
		}

		this.GRR._banker_player = this._current_player = this._cur_banker;
		// 游戏开始
		this._game_status = GameConstants.GS_MJ_PLAY;// 设置状态
		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		// gameStartResponse.setSiceIndex(rand);
		gameStartResponse.setBankerPlayer(this.GRR._banker_player);
		gameStartResponse.setCurrentPlayer(this._current_player);
		gameStartResponse.setLeftCardCount(this.GRR._left_card_count);

		int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants.MAX_COUNT];
		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int hand_card_count = this._logic.switch_to_cards_data(this.GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			// 只发自己的牌
			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			// 回放数据
			this.GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.load_room_info_data(roomResponse);
			this.load_common_status(roomResponse);

			if (this._cur_round == 1) {
				// shuffle_players();
				this.load_player_info_data(roomResponse);
			}
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setCurrentPlayer(
					this._current_player == GameConstants.INVALID_SEAT ? this._resume_player : this._current_player);
			roomResponse.setLeftCardCount(this.GRR._left_card_count);
			roomResponse.setGameStatus(this._game_status);
			roomResponse.setLeftCardCount(this.GRR._left_card_count);
			this.send_response_to_player(i, roomResponse);
		}
		////////////////////////////////////////////////// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		this.load_room_info_data(roomResponse);
		this.load_common_status(roomResponse);
		this.load_player_info_data(roomResponse);
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}

		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(this.GRR._left_card_count);
		this.GRR.add_room_response(roomResponse);

		if (has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN)) {
			exe_hun(this.GRR._banker_player);
			return true;
		}

		if (is_mj_type(GameConstants.GAME_TYPE_HENAN_PDS) || is_mj_type(GameConstants.GAME_TYPE_NEW_PING_DING_SHAN)) {
			if (!GameDescUtil.has_rule(getGameRuleIndexEx(), GameConstants.GAME_RULE_HENAN_CHU_FENG_BAO_TING)) {
				// 检测听牌
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					this._playerStatus[i]._hu_card_count = this.get_henan_ting_card(this._playerStatus[i]._hu_cards,
							this.GRR._cards_index[i], this.GRR._weave_items[i], this.GRR._weave_count[i]);
					if (this._playerStatus[i]._hu_card_count > 0) {
						this.operate_chi_hu_cards(i, this._playerStatus[i]._hu_card_count,
								this._playerStatus[i]._hu_cards);
					}
				}
			}
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_XX) || is_mj_type(GameConstants.GAME_TYPE_NEW_XIN_XIANG)) {
			// 检测听牌
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				this._playerStatus[i]._hu_card_count = this.get_hn_xin_xiang_ting_card_new(
						this._playerStatus[i]._hu_cards, this.GRR._cards_index[i], this.GRR._weave_items[i],
						this.GRR._weave_count[i]);
				if (this._playerStatus[i]._hu_card_count > 0) {
					this.operate_chi_hu_cards(i, this._playerStatus[i]._hu_card_count, this._playerStatus[i]._hu_cards);
				}
			}
		} else {
			// 检测听牌
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				this._playerStatus[i]._hu_card_count = this.get_henan_ting_card(this._playerStatus[i]._hu_cards,
						this.GRR._cards_index[i], this.GRR._weave_items[i], this.GRR._weave_count[i]);
				if (this._playerStatus[i]._hu_card_count > 0) {
					this.operate_chi_hu_cards(i, this._playerStatus[i]._hu_card_count, this._playerStatus[i]._hu_cards);
				}
			}
		}

		this.exe_dispatch_card(_current_player, GameConstants.WIK_NULL, 0);
		
		return true;
	}

	/**
	 * 开始 河南漯河麻将
	 * 
	 * @return
	 */
	private boolean game_start_henan_lh() {
		if (has_rule(GameConstants.GAME_RULE_HENAN_DAI_PAO)) {
			this.set_handler(this._handler_pao_henna);
			this._handler_pao_henna.exe(this);
			return true;
		} else {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				_player_result.pao[i] = 0;// 清掉 默认是-1
			}
		}

		this.GRR._banker_player = this._current_player = this._cur_banker;
		// 游戏开始
		this._game_status = GameConstants.GS_MJ_PLAY;// 设置状态
		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		// gameStartResponse.setSiceIndex(rand);
		gameStartResponse.setBankerPlayer(this.GRR._banker_player);
		gameStartResponse.setCurrentPlayer(this._current_player);
		gameStartResponse.setLeftCardCount(this.GRR._left_card_count);

		int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants.MAX_COUNT];
		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int hand_card_count = this._logic.switch_to_cards_data(this.GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			// 只发自己的牌
			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			// 回放数据
			this.GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.load_room_info_data(roomResponse);
			this.load_common_status(roomResponse);

			if (this._cur_round == 1) {
				// shuffle_players();
				this.load_player_info_data(roomResponse);
			}
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setCurrentPlayer(
					this._current_player == GameConstants.INVALID_SEAT ? this._resume_player : this._current_player);
			roomResponse.setLeftCardCount(this.GRR._left_card_count);
			roomResponse.setGameStatus(this._game_status);
			roomResponse.setLeftCardCount(this.GRR._left_card_count);
			this.send_response_to_player(i, roomResponse);
		}
		////////////////////////////////////////////////// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		this.load_room_info_data(roomResponse);
		this.load_common_status(roomResponse);
		this.load_player_info_data(roomResponse);
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}

		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(this.GRR._left_card_count);
		this.GRR.add_room_response(roomResponse);

		// 检测听牌
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			this._playerStatus[i]._hu_card_count = get_henan_lh_ting_card(this._playerStatus[i]._hu_cards,
					this.GRR._cards_index[i], this.GRR._weave_items[i], this.GRR._weave_count[i]);

			if (this._playerStatus[i]._hu_card_count > 0) {
				this.operate_chi_hu_cards(i, this._playerStatus[i]._hu_card_count, this._playerStatus[i]._hu_cards);
			}
		}

		this.exe_dispatch_card(_current_player, GameConstants.WIK_NULL, 0);

		return true;
	}

	public boolean game_start_cs_real() {

		pai_xing_fen = new int[getTablePlayerNumber()];
		bird_score = new int[getTablePlayerNumber()];

		_game_status = GameConstants.GS_MJ_PLAY;// 设置状态
		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		// gameStartResponse.setSiceIndex(rand);
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		// 判断是否有小胡

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			PlayerStatus playerStatus = _playerStatus[i];
			// 胡牌判断
			int action = this.analyse_chi_hu_card_cs_xiaohu(GRR._cards_index[i], GRR._start_hu_right[i]);

			// 小胡
			if (action != GameConstants.WIK_NULL) {
				playerStatus.add_action(GameConstants.WIK_XIAO_HU);
				_game_status = GameConstants.GS_MJ_XIAOHU;// 设置状态
			} else {
				GRR._start_hu_right[i].set_empty();
			}
		}

		int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants.MAX_COUNT];
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}
		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			// 只发自己的牌
			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);

				cards.addItem(hand_cards[i][j]);
			}
			// 回放数据
			GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			this.load_room_info_data(roomResponse);
			this.load_common_status(roomResponse);

			if (this._cur_round == 1) {
				// shuffle_players();
				this.load_player_info_data(roomResponse);
			}

			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setLeftCardCount(GRR._left_card_count);

			this.send_response_to_player(i, roomResponse);
		}

		////////////////////////////////////////////////////////////////////////////////////////////////
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		this.load_room_info_data(roomResponse);
		this.load_common_status(roomResponse);

		if (this._cur_round == 1) {
			// shuffle_players();
			this.load_player_info_data(roomResponse);
		}
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}
		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(GRR._left_card_count);
		GRR.add_room_response(roomResponse);

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i]._hu_card_count = get_cs_ting_card(_playerStatus[i]._hu_cards, GRR._cards_index[i],
					GRR._weave_items[i], GRR._weave_count[i], false);
			if (_playerStatus[i]._hu_card_count > 0) {
				operate_chi_hu_cards(i, _playerStatus[i]._hu_card_count, _playerStatus[i]._hu_cards);
			}
		}

		// GRR._left_card_count=1;
		///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		// 有小胡
		if (_game_status == GameConstants.GS_MJ_XIAOHU) {

			this.exe_xiao_hu(_current_player);
		} else {
			this.exe_dispatch_card(_current_player, GameConstants.WIK_NULL, 0);
		}

		return true;
	}

	// 开始长沙麻将 有小胡
	private boolean game_start_cs() {

		if (has_rule(GameConstants.GAME_RULE_HUNAN_CS_PIAO)) {
			this.set_handler(this.handler_piao_cs);
			this.handler_piao_cs.exe(this);
			return true;
		} else {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				_player_result.pao[i] = 0;// 清掉 默认是-1
			}
		}

		return game_start_cs_real();
	}

	public boolean game_start_cs_lx_real() {

		_game_status = GameConstants.GS_MJ_PLAY;// 设置状态
		// 初始化小胡检测
		FIRST_DISPATHCARD = true;
		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		// gameStartResponse.setSiceIndex(rand);
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		// 判断是否有小胡

		/*
		 * for (int i = 0; i < getTablePlayerNumber(); i++) { PlayerStatus
		 * playerStatus = _playerStatus[i]; // 胡牌判断 int action =
		 * this.analyse_chi_hu_card_cs_xiaohu_lx(GRR._cards_index[i],
		 * GRR._start_hu_right[i]);
		 * 
		 * // 小胡 if (action != GameConstants.WIK_NULL) {
		 * playerStatus.add_action(GameConstants.WIK_XIAO_HU); _game_status =
		 * GameConstants.GS_MJ_XIAOHU;// 设置状态 } else {
		 * GRR._start_hu_right[i].set_empty(); } }
		 */

		int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants.MAX_COUNT];
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}
		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			// 只发自己的牌
			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);

				cards.addItem(hand_cards[i][j]);
			}
			// 回放数据
			GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			this.load_room_info_data(roomResponse);
			this.load_common_status(roomResponse);

			if (this._cur_round == 1) {
				// shuffle_players();
				this.load_player_info_data(roomResponse);
			}

			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setLeftCardCount(GRR._left_card_count);

			this.send_response_to_player(i, roomResponse);
		}

		////////////////////////////////////////////////////////////////////////////////////////////////
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		this.load_room_info_data(roomResponse);
		this.load_common_status(roomResponse);

		if (this._cur_round == 1) {
			// shuffle_players();
			this.load_player_info_data(roomResponse);
		}
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}
		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(GRR._left_card_count);
		GRR.add_room_response(roomResponse);

		// GRR._left_card_count=1;
		///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		// 有小胡
		/*
		 * if (_game_status == GameConstants.GS_MJ_XIAOHU) { // // for (int i =
		 * 0; i < MJGameConstants.GAME_PLAYER; i++) { // PlayerStatus
		 * playerStatus = _playerStatus[i]; // if(playerStatus._action_count>0){
		 * // this.operate_player_action(i, false); // } // }
		 * this.exe_xiao_hu(_current_player); } else {
		 */
		this.exe_dispatch_card(_current_player, GameConstants.WIK_NULL, 0);
		// _table_scheduled = GameSchedule.put(new
		// DispatchCardRunnable(this.getRoom_id(), _current_player, false),
		// MJGameConstants.SEND_CARD_DELAY, TimeUnit.MILLISECONDS);
		// }

		return true;
	}

	// 开始长沙麻将 有小胡
	private boolean game_start_cs_lx() {
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			_player_result.shaizi[i][0] = -1;// 清掉 默认是-1
			_player_result.shaizi[i][1] = -1;// 清掉 默认是-1
		}

		if (has_rule(GameConstants.GAME_RULE_HUNAN_CS_PIAO)) {
			this.set_handler(this.handler_piao_cslx);
			this.handler_piao_cslx.exe(this);
			return true;
		} else {
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				_player_result.pao[i] = 0;// 清掉 默认是-1
			}
		}
		return game_start_cs_lx_real();
	}

	// 长沙麻将 杠牌,发两张牌,大家都可以看
	// public boolean dispath_cs_gang_card_data(int cur_player){
	// // 状态效验
	// if (cur_player == MJGameConstants.INVALID_SEAT)
	// return false;
	//
	// _status_cs_gang = true;
	//
	// _playerStatus[cur_player].set_card_status(MJGameConstants.CARD_STATUS_CS_GANG);
	// _playerStatus[cur_player].chi_hu_round_valid();//可以胡了
	//
	//
	// this._gang_card_data.clean_cards();
	//
	// PlayerStatus curPlayerStatus = _playerStatus[cur_player];
	// curPlayerStatus.reset();
	//
	// // 设置变量
	// _out_card_data = MJGameConstants.INVALID_VALUE;
	// _out_card_player = MJGameConstants.INVALID_SEAT;
	// _current_player = cur_player;// 轮到操作的人是自己
	//
	// _provide_player = cur_player;
	//
	//
	// int bu_card;
	// // 出牌响应判断
	//
	// // 从牌堆拿出2张牌
	// for(int i=0; i < MJGameConstants.CS_GANG_DRAW_COUNT; i++){
	// _send_card_count++;
	// if (is_mj_type(MJGameConstants.GAME_TYPE_HZ)) {
	// bu_card = _repertory_card_zz[_all_card_len-GRR._left_card_count];
	// } else {
	// bu_card = _repertory_card_cs[_all_card_len-GRR._left_card_count];
	// }
	// --GRR._left_card_count;
	// this._gang_card_data.add_card(bu_card);
	//
	// }
	//
	// //显示两张牌
	// this.operate_out_card(cur_player, MJGameConstants.CS_GANG_DRAW_COUNT,
	// this._gang_card_data.get_cards(),MJGameConstants.OUT_CARD_TYPE_MID,MJGameConstants.INVALID_SEAT);
	//
	// boolean has_action =false;
	//// for(int i=0; i<MJGameConstants.CS_GANG_DRAW_COUNT;i++){
	//// ChiHuRight chr = GRR._chi_hu_rights[i];
	//// chr.set_empty();
	//// }
	// //显示玩家对这两张牌的操作
	// for(int i=0; i < MJGameConstants.CS_GANG_DRAW_COUNT; i++){
	// boolean bAroseAction = false;
	// bu_card = this._gang_card_data.get_card(i);
	//
	// for(int k=0; k < MJGameConstants.GAME_PLAYER;k++){
	// //自己只有杠和胡
	// if(k==cur_player){
	// ChiHuRight chr = GRR._chi_hu_rights[k];
	// chr.set_empty();
	//
	// int action = analyse_chi_hu_card(GRR._cards_index[cur_player],
	// GRR._weave_items[cur_player], GRR._weave_count[cur_player], bu_card,
	// chr,true);//自摸
	// if(action != MJGameConstants.WIK_NULL){
	// //添加动作
	// curPlayerStatus.add_action(MJGameConstants.WIK_ZI_MO);
	// curPlayerStatus.add_zi_mo(bu_card,cur_player);
	// bAroseAction=true;
	// }
	// // 加到手牌
	// GRR._cards_index[cur_player][_logic.switch_to_card_index(bu_card)]++;
	//
	// // 如果牌堆还有牌，判断能不能杠
	// if (GRR._left_card_count > 0) {
	// GangCardResult gangCardResult = new GangCardResult();
	// int cbActionMask=_logic.analyse_gang_card(GRR._cards_index[cur_player],
	// GRR._weave_items[cur_player], GRR._weave_count[cur_player],
	// gangCardResult,true);
	//
	// if(cbActionMask!=MJGameConstants.WIK_NULL){//有杠
	// curPlayerStatus.add_action(MJGameConstants.WIK_GANG);//听牌的时候可以杠
	// for(int j= 0; j < gangCardResult.cbCardCount; j++){
	// curPlayerStatus.add_gang(gangCardResult.cbCardData[j], cur_player,
	// gangCardResult.isPublic[j]);
	// }
	//
	// bAroseAction=true;
	// }
	// }
	// GRR._cards_index[cur_player][_logic.switch_to_card_index(bu_card)]--;
	// }else{
	//
	// bAroseAction = this.estimate_cs_gang_respond(k, bu_card,false);//,
	// EstimatKind.EstimatKind_OutCard
	// }
	// // 出牌响应判断
	//
	// // 如果没有需要操作的玩家，派发扑克
	// if (bAroseAction == true) {
	// has_action= true;
	//
	// }
	// }
	// }
	//
	//
	// if(has_action==false){
	// _status_cs_gang = false;
	// //添加到牌堆
	//// for(int i=0; i < this._gang_card_data.get_card_count(); i++){
	//// GRR._discard_count[cur_player]++;
	//// GRR._discard_cards[cur_player][GRR._discard_count[cur_player] - 1] =
	// this._gang_card_data.get_card(i);
	//// }
	////
	//// this.operate_add_discard(cur_player,
	// this._gang_card_data.get_card_count(),
	// this._gang_card_data.get_card_count());
	//
	// _provide_player = MJGameConstants.INVALID_SEAT;
	// _out_card_player = cur_player;
	//
	// _table_scheduled = GameSchedule.put(new
	// AddDiscardRunnable(this.getRoom_id(), cur_player,
	// this._gang_card_data.get_card_count(),this._gang_card_data.get_cards()),
	// MJGameConstants.SEND_CARD_DELAY-100, TimeUnit.MILLISECONDS);
	//
	//
	// //继续发牌
	// _current_player = (cur_player+1)%MJGameConstants.GAME_PLAYER;
	//
	// _table_scheduled = GameSchedule.put(new
	// DispatchCardRunnable(this.getRoom_id(), _current_player, false),
	// MJGameConstants.SEND_CARD_DELAY, TimeUnit.MILLISECONDS);
	//
	// return false;
	//
	// }else{
	// _provide_player = cur_player;
	// //玩家有操作
	// for (int i = 0; i < MJGameConstants.GAME_PLAYER; i++) {
	// if(_playerStatus[i].has_action()){
	// this.operate_player_action(i, false);
	// }
	// }
	// return true;
	// }
	// }

	/**
	 * 开始 洛阳杠次
	 * 
	 * @return
	 */
	private boolean game_start_henan_lygc() {
		/* _logic.clean_magic_cards(); */
		// 初始化状态包次状态
		_bao_ci_state = GameConstants.LYGC_BAO_CI_END;
		_bao_ci_start = GameConstants.LYGC_BAO_CI_SATRT_NUM;
		LYGC_CI_STATE = false;

		has_display_bao_ci_start = false;

		if (has_rule(GameConstants.GAME_RULE_HENAN_DAI_PAO)) {
			this.set_handler(this._handler_pao_henna);
			this._handler_pao_henna.exe(this);
			return true;
		} else {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				_player_result.pao[i] = 0;// 清掉 默认是-1
				// _playerStatus[i].clean_action();
			}
		}

		this.GRR._banker_player = this._current_player = this._cur_banker;
		// 游戏开始
		this._game_status = GameConstants.GS_MJ_PLAY;// 设置状态
		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		// gameStartResponse.setSiceIndex(rand);
		gameStartResponse.setBankerPlayer(this.GRR._banker_player);
		gameStartResponse.setCurrentPlayer(this._current_player);
		gameStartResponse.setLeftCardCount(this.GRR._left_card_count);

		int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants.MAX_COUNT];
		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int hand_card_count = this._logic.switch_to_cards_data(this.GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			// 只发自己的牌
			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			// 回放数据
			this.GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.load_room_info_data(roomResponse);
			this.load_common_status(roomResponse);

			if (this._cur_round == 1) {
				// shuffle_players();
				this.load_player_info_data(roomResponse);
			}
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setCurrentPlayer(
					this._current_player == GameConstants.INVALID_SEAT ? this._resume_player : this._current_player);
			roomResponse.setLeftCardCount(this.GRR._left_card_count);
			roomResponse.setGameStatus(this._game_status);
			// roomResponse.setLeftCardCount(this.GRR._left_card_count);
			this.send_response_to_player(i, roomResponse);
		}
		////////////////////////////////////////////////// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		this.load_room_info_data(roomResponse);
		this.load_common_status(roomResponse);
		this.load_player_info_data(roomResponse);
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}

		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(this.GRR._left_card_count);
		this.GRR.add_room_response(roomResponse);

		// WalkerGeek 洛阳杠次
		if (is_mj_type(GameConstants.GAME_TYPE_MJ_LYGC_DIAN_PAO) || is_mj_type(GameConstants.GAME_TYPE_MJ_HE_ZE)) {
			// 检测听牌
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				this._playerStatus[i]._hu_card_count = this.get_henan_ting_card_lygc(this._playerStatus[i]._hu_cards,
						this.GRR._cards_index[i], this.GRR._weave_items[i], this.GRR._weave_count[i]);
				if (this._playerStatus[i]._hu_card_count > 0) {
					this.operate_chi_hu_cards(i, this._playerStatus[i]._hu_card_count, this._playerStatus[i]._hu_cards);
				}
			}

			this.exe_dispatch_card(_current_player, GameConstants.WIK_NULL, 0);
		} else {
			// 选取次牌
			exe_ci(this.GRR._banker_player);
		}
		return true;
	}

	// 摸起来的牌 派发牌
	@Override
	public boolean dispatch_card_data(int cur_player, int type, boolean tail) {
		// 牌局未开 或者等待状态 调度不需要执行 add by zain 2017/6/1
		if ((_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) && is_sys())
			return false;
		// add end
		// 发牌
		this.set_handler(this._handler_dispath_card);
		this._handler_dispath_card.reset_status(cur_player, type);
		this._handler.exe(this);

		return true;
	}

	/**
	 * 处理牌桌结束逻辑
	 * 
	 * @param seat_index
	 * @param reason
	 */
	@Override
	protected boolean on_handler_game_finish(int seat_index, int reason) {
		if (is_mj_type(GameConstants.GAME_TYPE_SHUANGGUI) || is_mj_type(GameConstants.GAME_TYPE_HUNAN_SHANG_XIA_GUI)) {
			return this.handler_game_finish_sg(seat_index, reason);
		} else if (is_mj_type(GameConstants.GAME_TYPE_XTHH)) {
			return this.handler_game_finish_xthh(seat_index, reason);
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_AY) || is_mj_type(GameConstants.GAME_TYPE_NEW_AN_YANG)) {
			return this.handler_game_finish_ay(seat_index, reason);
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_LZ) || is_mj_type(GameConstants.GAME_TYPE_NEW_LIN_ZHOU)) {
			return this.handler_game_finish_lz(seat_index, reason);
		} else if (is_mj_type(GameConstants.GAME_TYPE_CS) || is_mj_type(GameConstants.GAME_TYPE_HALL_CHANG_SHA_MJ)
				|| is_mj_type(GameConstants.GAME_TYPE_THJ_CS)) {
			return this.handler_game_finish_cs(seat_index, reason);
		} else if ((is_mj_type(GameConstants.GAME_TYPE_HENAN) || is_mj_type(GameConstants.GAME_TYPE_HENAN_ZMD)
				|| is_mj_type(GameConstants.GAME_TYPE_HENAN_KF) || is_mj_type(GameConstants.GAME_TYPE_HENAN_NY))
				|| is_mj_type(GameConstants.GAME_TYPE_HENAN_XX) || is_mj_type(GameConstants.GAME_TYPE_HENAN_PDS)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_HE_NAN) || is_mj_type(GameConstants.GAME_TYPE_NEW_ZHU_MA_DIAN)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_KAI_FENG) || is_mj_type(GameConstants.GAME_TYPE_NEW_NAN_YANG)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_XIN_XIANG)
				|| is_mj_type(GameConstants.GAME_TYPE_JIAO_ZUO_HUAN_HUAN)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_PING_DING_SHAN)) {
			return this.handler_game_finish_henan(seat_index, reason);
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_HZ)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_HN_HONG_ZHONG)) {
			return this.handler_game_finish_henan_hz(seat_index, reason);
		} else if (is_mj_type(GameConstants.GAME_TYPE_FLS_LX_CG)) {
			return this.handler_game_finish_lxcg(seat_index, reason);
		} else if (is_mj_type(GameConstants.GAME_TYPE_FLS_CS_LX) || is_mj_type(GameConstants.GAME_TYPE_FLS_CS_LX_DT)) {
			return this.handler_game_finish_cs_lx(seat_index, reason);
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_XC) || is_mj_type(GameConstants.GAME_TYPE_NEW_XU_CHANG)) {
			return this.handler_game_finish_henan_xc(seat_index, reason);
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_ZHOU_KOU)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_ZHOU_KOU)) {
			return this.handler_game_finish_he_nan_zhou_kou(seat_index, reason);
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_LYGC)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_LUO_YANG_GANG_CI)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_LYGC_DIAN_PAO)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_HE_ZE)) {
			return this.handler_game_finish_henan_lygc(seat_index, reason);
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_XY)) {
			return this.handler_game_finish_henan_xy(seat_index, reason);
		} else if (is_mj_type(GameConstants.GAME_TYPE_ZZ) || is_mj_type(GameConstants.GAME_TYPE_HENAN_ZHUAN_ZHUAN)
				|| is_mj_type(GameConstants.GAME_TYPE_HALL_ZHUAN_ZHUAN_MJ)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_SAN_MEN_XIA)) {
			return this.handler_game_finish_hunan_zz_hz(seat_index, reason);
		} else if (is_mj_type(GameConstants.GAME_TYPE_HZ) || is_mj_type(GameConstants.GAME_TYPE_HONG_ZHONG_MJ_TH)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_YUYANG_HONGZHON)
				|| is_mj_type(GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ)
				// 邵阳红中+萍乡转转
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_HONG_ZHONG_FEI)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_PING_XIANG_ZZ)) {
			return this.handler_game_finish_hunan_hz(seat_index, reason);
		} else {
			return super.on_handler_game_finish(seat_index, reason);
		}
	}

	public boolean handler_game_finish_he_nan_zhou_kou(int seat_index, int reason) {
		int real_reason = reason;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_ready[i] = 0;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();

		roomResponse.setLeftCardCount(0);

		this.load_common_status(roomResponse);
		this.load_room_info_data(roomResponse);

		// 这里记录了两次，先这样
		RoomInfo.Builder room_info = getRoomInfo();
		game_end.setRoomInfo(room_info);
		game_end.setRunPlayerId(_run_player_id);
		game_end.setRoundOverType(0);
		game_end.setGamePlayerNumber(getTablePlayerNumber());
		game_end.setRoomOverType(0);
		game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间

		setGameEndBasicPrama(game_end);

		if (GRR != null) {// reason == MJGameConstants.Game_End_NORMAL || reason
							// == MJGameConstants.Game_End_DRAW ||
			// (reason ==MJGameConstants.Game_End_RELEASE_PLAY && GRR!=null)
			game_end.setRoundOverType(1);
			game_end.setStartTime(GRR._start_time);

			game_end.setGameTypeIndex(GRR._game_type_index);
			roomResponse.setLeftCardCount(GRR._left_card_count);

			// 特别显示的牌
			for (int i = 0; i < GRR._especial_card_count; i++) {
				game_end.addEspecialShowCards(GRR._especial_show_cards[i]);

			}

			GRR._end_type = reason;

			// 杠牌，每个人的分数
			float lGangScore[] = new float[getTablePlayerNumber()];

			for (int i = 0; i < getTablePlayerNumber(); i++) {

				if (has_rule(GameConstants.GAME_RULE_HENAN_HZBHG) && GRR._end_type == GameConstants.Game_End_DRAW) {
				} else {
					for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
						for (int k = 0; k < getTablePlayerNumber(); k++) {
							lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
						}
					}
				}

				// 记录
				for (int j = 0; j < getTablePlayerNumber(); j++) {
					_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
				}
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				GRR._game_score[i] += lGangScore[i];
				GRR._game_score[i] += GRR._start_hu_score[i];// 起手胡分数
				_player_result.game_score[i] += GRR._game_score[i];
			}

			this.load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);

			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);// 专家
			game_end.setLeftCardCount(GRR._left_card_count);// 剩余牌
			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

			// 设置中鸟数据
			for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i < GRR._count_niao; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao[i]);
			}
			game_end.setCountPickNiao(GRR._count_pick_niao);// 中鸟个数

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					pnc.addItem(GRR._player_niao_cards[i][j]);
				}
				game_end.addPlayerNiaoCards(pnc);
				game_end.addHuResult(GRR._hu_result[i]);

				// 胡的牌
				Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
					hc.addItem(GRR._chi_hu_card[i][j]);
				}

				game_end.addHuCardData(GRR._chi_hu_card[i][0]);
				game_end.addHuCardArray(hc);
			}

			// 现在权值只有一位
			long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

			// 设置胡牌描述
			this.set_result_describe();

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {
					if (has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN)) {
						if (_logic.is_magic_card(GRR._cards_data[i][j])) {
							cs.addItem(GRR._cards_data[i][j] + GameConstants.CARD_ESPECIAL_TYPE_HUN);
						} else {
							cs.addItem(GRR._cards_data[i][j]);
						}
					} else {
						cs.addItem(GRR._cards_data[i][j]);
					}
				}
				game_end.addCardsData(cs);// 牌

				// 组合
				WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
					WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
					weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
					weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
					weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
					weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
					weaveItem_array.addWeaveItem(weaveItem_item);
				}
				game_end.addWeaveItemArray(weaveItem_array);

				GRR._chi_hu_rights[i].get_right_data(rv);// 获取权位数值
				game_end.addChiHuRight(rv[0]);

				GRR._start_hu_right[i].get_right_data(rv);// 获取权位数值
				game_end.addStartHuRight(rv[0]);

				game_end.addProvidePlayer(GRR._provider[i]);
				game_end.addGameScore(GRR._game_score[i]);// 放炮的人？
				game_end.addGangScore(lGangScore[i]);// 杠牌得分
				game_end.addStartHuScore(GRR._start_hu_score[i]);
				game_end.addResultDes(GRR._result_des[i]);

				// 胡牌
				game_end.addWinOrder(GRR._win_order[i]);

				Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < getTablePlayerNumber(); j++) {
					lfs.addItem(GRR._lost_fan_shu[i][j]);
				}

				game_end.addLostFanShu(lfs);

			}

		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round && (!is_sys())) {// 局数到了
				end = true;
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(this.process_player_result(reason));
			} else {
				// 确定下局庄家
				// 以后谁胡牌，下局谁做庄。
				// 流局,庄家下一个
				// 通炮,放炮的玩家

			}
		} else if ((!is_sys()) && (reason == GameConstants.Game_End_RELEASE_PLAY
				|| reason == GameConstants.Game_End_RELEASE_NO_BEGIN || reason == GameConstants.Game_End_RELEASE_RESULT
				|| reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_SYSTEM)) {
			end = true;
			real_reason = GameConstants.Game_End_DRAW;// 刘局
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(this.process_player_result(reason));
		}
		game_end.setEndType(real_reason);

		////////////////////////////////////////////////////////////////////// 得分总的
		roomResponse.setGameEnd(game_end);

		this.send_response_to_room(roomResponse);

		record_game_round(game_end);

		// 结束后刷新玩家
		// RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
		// roomResponse2.setGameStatus(_game_status);
		// roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		// this.load_player_info_data(roomResponse2);
		// this.send_response_to_room(roomResponse2);

		// 超时解散
		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < getTablePlayerNumber(); j++) {
				Player player = this.get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");

			}
		}

		if (end && (!is_sys()))// 删除
		{
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
		}

		if (!is_sys()) {
			GRR = null;
		}

		if (is_sys()) {
			clear_score_in_gold_room();
		}
		// 错误断言
		return false;
	}

	/**
	 * 仙桃晃晃结束
	 * 
	 * @param seat_index
	 * @param reason
	 * @return
	 */
	public boolean handler_game_finish_xthh(int seat_index, int reason) {
		int real_reason = reason;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_ready[i] = 0;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();

		roomResponse.setLeftCardCount(0);

		this.load_common_status(roomResponse);
		this.load_room_info_data(roomResponse);

		RoomInfo.Builder room_info = getRoomInfo();
		game_end.setRoomInfo(room_info);
		game_end.setGamePlayerNumber(getTablePlayerNumber());
		game_end.setRoundOverType(0);
		game_end.setRoomOverType(0);
		game_end.setRunPlayerId(_run_player_id);
		if (GRR != null) {// reason == MJGameConstants.Game_End_NORMAL || reason
							// == MJGameConstants.Game_End_DRAW ||
			// (reason ==MJGameConstants.Game_End_RELEASE_PLAY && GRR!=null)
			game_end.setRoundOverType(1);
			game_end.setStartTime(GRR._start_time);
			game_end.setGameTypeIndex(GRR._game_type_index);
			if (GRR._especial_txt != "") {
				game_end.setEspecialTxt(GRR._especial_txt);
				game_end.setEspecialTxtType(GRR._especial_txt_type);
			}

			roomResponse.setLeftCardCount(GRR._left_card_count);

			// 特别显示的牌
			// for (int i = 0; i < GRR._especial_card_count; i++) {
			// game_end.addEspecialShowCards(GRR._especial_show_cards[i]+MJGameConstants.CARD_ESPECIAL_TYPE_LAI_GEN);
			// }
			game_end.addEspecialShowCards(GRR._especial_show_cards[0] + GameConstants.CARD_ESPECIAL_TYPE_LAI_GEN);
			game_end.addEspecialShowCards(GRR._especial_show_cards[1]);

			GRR._end_type = reason;

			// 杠牌，每个人的分数
			float lGangScore[] = new float[getTablePlayerNumber()];
			for (int i = 0; i < getTablePlayerNumber(); i++) {

				for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
					for (int k = 0; k < getTablePlayerNumber(); k++) {
						lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
						// _allGRR._gang_score[i].scores[j][k]+=
						// GRR._gang_score[i].scores[j][k];//杠牌，每个人的分数

						// allGangScore[k]+=_allGRR._gang_score[i].scores[j][k];
					}
				}

				// 记录
				for (int j = 0; j < getTablePlayerNumber(); j++) {
					_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
				}

			}
			//
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				_player_result.game_score[i] += GRR._game_score[i];

				// 这两个分数已经加过了
				GRR._game_score[i] += lGangScore[i];
				GRR._game_score[i] += GRR._piao_lai_score[i];
			}

			this.load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);

			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);// 专家
			game_end.setLeftCardCount(GRR._left_card_count);// 剩余牌
			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

			// 设置中鸟数据
			for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i < GRR._count_niao; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao[i]);
			}
			game_end.setCountPickNiao(GRR._count_pick_niao);// 中鸟个数

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					pnc.addItem(GRR._player_niao_cards[i][j]);
				}
				game_end.addPlayerNiaoCards(pnc);
				game_end.addHuResult(GRR._hu_result[i]);

				// 胡的牌
				Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
					hc.addItem(GRR._chi_hu_card[i][j]);
				}

				game_end.addHuCardData(GRR._chi_hu_card[i][0]);
				game_end.addHuCardArray(hc);
			}

			// 现在权值只有一位
			long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

			// 设置胡牌描述
			this.set_result_describe();

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {
					// 癞子
					if (_logic.is_magic_card(GRR._cards_data[i][j])) {
						cs.addItem(GRR._cards_data[i][j] + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
					} else {
						cs.addItem(GRR._cards_data[i][j]);
					}
				}
				game_end.addCardsData(cs);// 牌

				// 组合
				WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
					WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
					weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
					weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
					weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
					weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
					weaveItem_array.addWeaveItem(weaveItem_item);
				}
				game_end.addWeaveItemArray(weaveItem_array);

				GRR._chi_hu_rights[i].get_right_data(rv);// 获取权位数值
				game_end.addChiHuRight(rv[0]);

				GRR._start_hu_right[i].get_right_data(rv);// 获取权位数值
				game_end.addStartHuRight(rv[0]);

				game_end.addProvidePlayer(GRR._provider[i]);
				game_end.addGameScore(GRR._game_score[i]);// 放炮的人？
				game_end.addGangScore(lGangScore[i]);// 杠牌得分
				game_end.addStartHuScore(GRR._start_hu_score[i]);
				game_end.addResultDes(GRR._result_des[i]);

				// 胡牌
				game_end.addWinOrder(GRR._win_order[i]);

				Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < getTablePlayerNumber(); j++) {
					lfs.addItem(GRR._lost_fan_shu[i][j]);
				}

				game_end.addLostFanShu(lfs);

			}

		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if ((!is_sys()) && _cur_round >= _game_round) {// 局数到了
				end = true;
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(this.process_player_result(reason));
			} else {
				// 确定下局庄家
				// 以后谁胡牌，下局谁做庄。
				// 流局,庄家下一个
				// 通炮,放炮的玩家

			}
		} else if ((!is_sys()) && (reason == GameConstants.Game_End_RELEASE_PLAY
				|| reason == GameConstants.Game_End_RELEASE_NO_BEGIN || reason == GameConstants.Game_End_RELEASE_RESULT
				|| reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_SYSTEM)) {
			end = true;
			real_reason = GameConstants.Game_End_DRAW;// 刘局
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(this.process_player_result(reason));
		}
		game_end.setEndType(real_reason);

		////////////////////////////////////////////////////////////////////// 得分总的
		roomResponse.setGameEnd(game_end);

		this.send_response_to_room(roomResponse);

		record_game_round(game_end);

		// 结束后刷新玩家
		// RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
		// roomResponse2.setGameStatus(_game_status);
		// roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		// this.load_player_info_data(roomResponse2);
		// this.send_response_to_room(roomResponse2);

		// 超时解散
		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < getTablePlayerNumber(); j++) {
				Player player = this.get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");

			}
		}

		if (end)// 删除
		{
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
		}
		if (!is_sys()) {
			GRR = null;
		}

		if (is_sys()) {
			clear_score_in_gold_room();
		}
		// 错误断言
		return false;
	}

	public boolean handler_game_finish_cs_lx(int seat_index, int reason) {
		int real_reason = reason;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_ready[i] = 0;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();

		roomResponse.setLeftCardCount(0);

		this.load_common_status(roomResponse);
		this.load_room_info_data(roomResponse);

		RoomInfo.Builder room_info = getRoomInfo();
		game_end.setRoomInfo(room_info);
		game_end.setGamePlayerNumber(getTablePlayerNumber());
		game_end.setRoundOverType(0);
		game_end.setRoomOverType(0);
		game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			game_end.addPao(_player_result.pao[i]);
		}

		setGameEndBasicPrama(game_end);

		if (GRR != null) {// reason == MJGameConstants.Game_End_NORMAL || reason
							// == MJGameConstants.Game_End_DRAW ||
			// (reason ==MJGameConstants.Game_End_RELEASE_PLAY && GRR!=null)
			game_end.setRoundOverType(1);
			game_end.setStartTime(GRR._start_time);

			game_end.setGameTypeIndex(GRR._game_type_index);
			roomResponse.setLeftCardCount(GRR._left_card_count);

			// 特别显示的牌
			for (int i = 0; i < GRR._especial_card_count; i++) {
				game_end.addEspecialShowCards(GRR._especial_show_cards[i]);
			}

			GRR._end_type = reason;
			boolean isMutlpNiap = isMutlpDingNiao();// 是否乘法定鸟
			// 重新算小胡分数

			// 临湘长沙麻将 小胡算 骰子
			for (int p = 0; p < getTablePlayerNumber(); p++) {
				if (GRR._start_hu_right[p].is_valid()) {
					int lStartHuScore = 0;

					int wFanShu = _logic.get_chi_hu_action_rank_cs_lx(GRR._start_hu_right[p]);

					lStartHuScore = wFanShu * GameConstants.CELL_SCORE;

					for (int i = 0; i < getTablePlayerNumber(); i++) {
						if (i == p)
							continue;
						int s = lStartHuScore;
						// 庄闲
						if (is_zhuang_xian()) {
							if ((GRR._banker_player == p) || (GRR._banker_player == i)) {
								s += s;
							}
						}

						// 中骰子的是p自己
						int zhong_seat = get_target_shai_zi_player(_player_result.shaizi[p][0],
								_player_result.shaizi[p][1]);
						if (p == zhong_seat) {
							s = isMutlpNiap ? 2 * s : s + 1;
						} else if (zhong_seat == i) {// 中骰子的是其他人， 则中的那个人要多给 ，
														// 其他人不用
							s = isMutlpNiap ? 2 * s : s + 1;
						}

						GRR._start_hu_score[i] -= s;// 输的番薯
						GRR._start_hu_score[p] += s;
					}
				}
			}
			// 杠牌，每个人的分数
			float lGangScore[] = new float[getTablePlayerNumber()];
			for (int i = 0; i < getTablePlayerNumber(); i++) {

				for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
					for (int k = 0; k < getTablePlayerNumber(); k++) {
						lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
						// _allGRR._gang_score[i].scores[j][k]+=
						// GRR._gang_score[i].scores[j][k];//杠牌，每个人的分数

						// allGangScore[k]+=_allGRR._gang_score[i].scores[j][k];
					}
				}

				// 记录
				for (int j = 0; j < getTablePlayerNumber(); j++) {
					_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
				}

			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				GRR._game_score[i] += lGangScore[i];
				GRR._game_score[i] += GRR._start_hu_score[i];// 起手胡分数

				// 记录
				// _all_start_hu_score[i]+=_start_hu_score[i];//起手胡分数
				_player_result.game_score[i] += GRR._game_score[i];

			}

			this.load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);

			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);// 专家
			game_end.setLeftCardCount(GRR._left_card_count);// 剩余牌
			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

			// 设置中鸟数据
			for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i < GRR._count_niao; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao[i]);
			}

			for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i < GRR._count_niao_fei; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao_fei[i]);
			}
			game_end.setCountPickNiao(GRR._count_pick_niao + GRR._count_pick_niao_fei);// 中鸟个数

			// 鸟的数据 这里要注意三人场的特殊处理 三人场必须发四个人的鸟 不然显示不全
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					pnc.addItem(GRR._player_niao_cards[i][j]);
				}
				// 临湘长沙麻将 不用处理飞鸟
				// for (int j = 0; j < GRR._player_niao_count_fei[i]; j++) {
				// pnc.addItem(GRR._player_niao_cards_fei[i][j]);
				// }
				game_end.addPlayerNiaoCards(pnc);
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				game_end.addHuResult(GRR._hu_result[i]);

				// 胡的牌
				Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
					hc.addItem(GRR._chi_hu_card[i][j]);
				}

				game_end.addHuCardData(GRR._chi_hu_card[i][0]);
				game_end.addHuCardArray(hc);
			}

			// 现在权值只有一位
			long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

			// 设置胡牌描述
			this.set_result_describe();

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {

					cs.addItem(GRR._cards_data[i][j]);
				}
				game_end.addCardsData(cs);// 牌

				// 组合
				WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
					WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
					weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
					weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
					weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
					if (j < GRR._weave_count[i]) {
						weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
					} else {
						weaveItem_item.setWeaveKind(GameConstants.WIK_NULL);
					}

					weaveItem_array.addWeaveItem(weaveItem_item);
				}
				game_end.addWeaveItemArray(weaveItem_array);

				GRR._chi_hu_rights[i].get_right_data(rv);// 获取权位数值
				game_end.addChiHuRight(rv[0]);

				GRR._start_hu_right[i].get_right_data(rv);// 获取权位数值
				game_end.addStartHuRight(rv[0]);

				game_end.addProvidePlayer(GRR._provider[i]);
				game_end.addGameScore(GRR._game_score[i]);// 放炮的人？
				game_end.addGangScore(lGangScore[i]);// 杠牌得分
				game_end.addStartHuScore(GRR._start_hu_score[i]);
				game_end.addResultDes(GRR._result_des[i]);

				// 胡牌
				game_end.addWinOrder(GRR._win_order[i]);

				Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < getTablePlayerNumber(); j++) {
					lfs.addItem(GRR._lost_fan_shu[i][j]);
				}

				game_end.addLostFanShu(lfs);

			}

		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round) {// 局数到了
				end = true;
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(this.process_player_result(reason));
			} else {
				// 确定下局庄家
				// 以后谁胡牌，下局谁做庄。
				// 流局,庄家下一个
				// 通炮,放炮的玩家

			}
		} else if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT
				|| reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_SYSTEM) {
			end = true;
			real_reason = GameConstants.Game_End_DRAW;// 刘局
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(this.process_player_result(reason));
		}
		game_end.setEndType(real_reason);

		////////////////////////////////////////////////////////////////////// 得分总的
		roomResponse.setGameEnd(game_end);

		this.send_response_to_room(roomResponse);

		record_game_round(game_end);

		// 结束后刷新玩家
		// RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
		// roomResponse2.setGameStatus(_game_status);
		// roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		// this.load_player_info_data(roomResponse2);
		// this.send_response_to_room(roomResponse2);

		// 超时解散
		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < getTablePlayerNumber(); j++) {
				Player player = this.get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");

			}
		}

		if (end)// 删除
		{
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
		}
		if (!is_sys()) {
			GRR = null;
		}

		// 错误断言
		return false;
	}

	public boolean handler_game_finish_cs(int seat_index, int reason) {
		int real_reason = reason;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_ready[i] = 0;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();

		roomResponse.setLeftCardCount(0);

		this.load_common_status(roomResponse);
		this.load_room_info_data(roomResponse);

		RoomInfo.Builder room_info = getRoomInfo();
		game_end.setRoomInfo(room_info);

		game_end.setRoundOverType(0);
		game_end.setRoomOverType(0);
		game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间
		game_end.setGamePlayerNumber(getTablePlayerNumber());
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			game_end.addPao(_player_result.pao[i]);
		}

		setGameEndBasicPrama(game_end);

		if (GRR != null) {// reason == MJGameConstants.Game_End_NORMAL || reason
							// == MJGameConstants.Game_End_DRAW ||
			// (reason ==MJGameConstants.Game_End_RELEASE_PLAY && GRR!=null)
			game_end.setRoundOverType(1);
			game_end.setStartTime(GRR._start_time);

			game_end.setGameTypeIndex(GRR._game_type_index);
			roomResponse.setLeftCardCount(GRR._left_card_count);

			// 特别显示的牌
			for (int i = 0; i < GRR._especial_card_count; i++) {
				game_end.addEspecialShowCards(GRR._especial_show_cards[i]);

			}

			GRR._end_type = reason;
			boolean isMutlpNiap = isMutlpDingNiao();// 是否乘法定鸟
			// 重新算小胡分数
			if (this.is_mj_type(GameConstants.GAME_TYPE_CS) || is_mj_type(GameConstants.GAME_TYPE_HALL_CHANG_SHA_MJ)
					|| is_mj_type(GameConstants.GAME_TYPE_THJ_CS)) {

				for (int p = 0; p < getTablePlayerNumber(); p++) {
					if (GRR._start_hu_right[p].is_valid()) {
						int lStartHuScore = 0;

						int wFanShu = _logic.get_chi_hu_action_rank_cs(GRR._start_hu_right[p]);

						lStartHuScore = wFanShu * GameConstants.CELL_SCORE;

						for (int i = 0; i < getTablePlayerNumber(); i++) {
							if (i == p)
								continue;
							int s = lStartHuScore;
							// 庄闲
							if (is_zhuang_xian()) {
								if ((GRR._banker_player == p) || (GRR._banker_player == i)) {
									s += s;
								}
							}
							int niao = GRR._player_niao_count[p] + GRR._player_niao_count[i];
							if (niao > 0) {
								if (isMutlpNiap) {
									s *= Math.pow(2, niao);
								} else {
									s *= (niao + 1);
								}
							}
							GRR._start_hu_score[i] -= s;// 输的番薯
							GRR._start_hu_score[p] += s;
						}
					}
				}

			}
			// 杠牌，每个人的分数
			float lGangScore[] = new float[getTablePlayerNumber()];
			for (int i = 0; i < getTablePlayerNumber(); i++) {

				for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
					for (int k = 0; k < getTablePlayerNumber(); k++) {
						lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
						// _allGRR._gang_score[i].scores[j][k]+=
						// GRR._gang_score[i].scores[j][k];//杠牌，每个人的分数

						// allGangScore[k]+=_allGRR._gang_score[i].scores[j][k];
					}
				}

				// 记录
				for (int j = 0; j < getTablePlayerNumber(); j++) {
					_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
				}

			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				GRR._game_score[i] += lGangScore[i];
				GRR._game_score[i] += GRR._start_hu_score[i];// 起手胡分数

				// 记录
				// _all_start_hu_score[i]+=_start_hu_score[i];//起手胡分数
				_player_result.game_score[i] += GRR._game_score[i];

			}

			this.load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);

			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);// 专家
			game_end.setLeftCardCount(GRR._left_card_count);// 剩余牌
			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

			// 设置中鸟数据
			for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i < GRR._count_niao; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao[i]);
			}

			for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i < GRR._count_niao_fei; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao_fei[i]);
			}
			game_end.setCountPickNiao(GRR._count_pick_niao + GRR._count_pick_niao_fei);// 中鸟个数

			// 鸟的数据 这里要注意三人场的特殊处理 三人场必须发四个人的鸟 不然显示不全
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				// 定鸟
				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					int card = GRR._player_niao_cards[i][j];
					if (card < GameConstants.DING_NIAO_INVALID) {
						card += GameConstants.DING_NIAO_INVALID;
					}
					pnc.addItem(card);
				}

				// 飞鸟
				for (int j = 0; j < GRR._player_niao_count_fei[i]; j++) {
					int card = GRR._player_niao_cards_fei[i][j];
					if (card < GameConstants.DING_NIAO_INVALID) {
						card += GameConstants.FEI_NIAO_INVALID;
					}
					pnc.addItem(card);
				}
				game_end.addPlayerNiaoCards(pnc);
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				game_end.addHuResult(GRR._hu_result[i]);

				// 胡的牌
				Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
					hc.addItem(GRR._chi_hu_card[i][j]);
				}

				game_end.addHuCardData(GRR._chi_hu_card[i][0]);
				game_end.addHuCardArray(hc);
			}

			// 现在权值只有一位
			long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

			// 设置胡牌描述
			this.set_result_describe();

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {

					cs.addItem(GRR._cards_data[i][j]);
				}
				game_end.addCardsData(cs);// 牌

				// 组合
				WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
					WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
					weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
					weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
					weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
					if (j < GRR._weave_count[i]) {
						weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
					} else {
						weaveItem_item.setWeaveKind(GameConstants.WIK_NULL);
					}

					weaveItem_array.addWeaveItem(weaveItem_item);
				}
				game_end.addWeaveItemArray(weaveItem_array);

				GRR._chi_hu_rights[i].get_right_data(rv);// 获取权位数值
				game_end.addChiHuRight(rv[0]);

				GRR._start_hu_right[i].get_right_data(rv);// 获取权位数值
				game_end.addStartHuRight(rv[0]);
				game_end.addProvidePlayer(GRR._provider[i]);

				if (is_mj_type(GameConstants.GAME_TYPE_THJ_CS)) {
					// 胡牌分数
					game_end.addJettonScore(pai_xing_fen[i]);
					// 中鸟计分
					game_end.addCardType(bird_score[i]);
				}
				// 飘分
				game_end.addGangScore(getZuoPiaoScore() + _player_result.pao[i]);
				// 小胡分数
				game_end.addStartHuScore(GRR._start_hu_score[i]);
				// 总结算
				game_end.addGameScore(GRR._game_score[i]);

				game_end.addResultDes(GRR._result_des[i]);
				game_end.addWinOrder(GRR._win_order[i]);

				Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < getTablePlayerNumber(); j++) {
					lfs.addItem(GRR._lost_fan_shu[i][j]);
				}

				game_end.addLostFanShu(lfs);

			}

		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round) {// 局数到了
				end = true;
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(this.process_player_result(reason));
			} else {
				// 确定下局庄家
				// 以后谁胡牌，下局谁做庄。
				// 流局,庄家下一个
				// 通炮,放炮的玩家

			}
		} else if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT
				|| reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_SYSTEM) {
			end = true;
			real_reason = GameConstants.Game_End_DRAW;// 刘局
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(this.process_player_result(reason));
		}
		game_end.setEndType(real_reason);

		////////////////////////////////////////////////////////////////////// 得分总的
		roomResponse.setGameEnd(game_end);

		this.send_response_to_room(roomResponse);

		record_game_round(game_end);

		// 结束后刷新玩家
		// RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
		// roomResponse2.setGameStatus(_game_status);
		// roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		// this.load_player_info_data(roomResponse2);
		// this.send_response_to_room(roomResponse2);

		// 超时解散
		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < getTablePlayerNumber(); j++) {
				Player player = this.get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");

			}
		}

		if (end)// 删除
		{
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
		}
		if (!is_sys()) {
			GRR = null;
		}

		// 错误断言
		return false;
	}

	public boolean handler_game_finish_sg(int seat_index, int reason) {
		int real_reason = reason;

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			_player_ready[i] = 0;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();

		roomResponse.setLeftCardCount(0);

		this.load_common_status(roomResponse);
		this.load_room_info_data(roomResponse);

		RoomInfo.Builder room_info = getRoomInfo();
		game_end.setRoomInfo(room_info);

		game_end.setRoundOverType(0);
		game_end.setRoomOverType(0);
		game_end.setGamePlayerNumber(getTablePlayerNumber());

		if (GRR != null) {// reason == MJGameConstants.Game_End_NORMAL || reason
							// == MJGameConstants.Game_End_DRAW ||
			// (reason ==MJGameConstants.Game_End_RELEASE_PLAY && GRR!=null)
			game_end.setRoundOverType(1);
			game_end.setStartTime(GRR._start_time);
			game_end.setGameTypeIndex(GRR._game_type_index);
			roomResponse.setLeftCardCount(GRR._left_card_count);

			// 特别显示的牌
			for (int i = 0; i < GRR._especial_card_count; i++) {
				game_end.addEspecialShowCards(GRR._especial_show_cards[i] + GameConstants.CARD_ESPECIAL_TYPE_DING_GUI);
			}

			GRR._end_type = reason;

			// 杠牌，每个人的分数
			float lGangScore[] = new float[GameConstants.GAME_PLAYER];
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {

				for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
					for (int k = 0; k < GameConstants.GAME_PLAYER; k++) {
						lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
						// _allGRR._gang_score[i].scores[j][k]+=
						// GRR._gang_score[i].scores[j][k];//杠牌，每个人的分数

						// allGangScore[k]+=_allGRR._gang_score[i].scores[j][k];
					}
				}

				// 记录
				for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
					_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
				}

			}

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				GRR._game_score[i] += lGangScore[i];
				GRR._game_score[i] += GRR._start_hu_score[i];// 起手胡分数

				// 记录
				// _all_start_hu_score[i]+=_start_hu_score[i];//起手胡分数
				_player_result.game_score[i] += GRR._game_score[i];

			}

			this.load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);

			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);// 专家
			game_end.setLeftCardCount(GRR._left_card_count);// 剩余牌
			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

			// 设置中鸟数据
			for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i < GRR._count_niao; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao[i]);
			}
			game_end.setCountPickNiao(GRR._count_pick_niao);// 中鸟个数

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					pnc.addItem(GRR._player_niao_cards[i][j]);
				}
				game_end.addPlayerNiaoCards(pnc);
				game_end.addHuResult(GRR._hu_result[i]);

				// 胡的牌
				Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
					hc.addItem(GRR._chi_hu_card[i][j]);
				}

				game_end.addHuCardData(GRR._chi_hu_card[i][0]);
				game_end.addHuCardArray(hc);
			}

			// 现在权值只有一位
			long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

			// 设置胡牌描述
			this.set_result_describe();

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {
					// 癞子
					if (_logic.is_magic_card(GRR._cards_data[i][j])) {
						cs.addItem(GRR._cards_data[i][j] + GameConstants.CARD_ESPECIAL_TYPE_GUI);
					} else {
						cs.addItem(GRR._cards_data[i][j]);
					}
				}
				game_end.addCardsData(cs);// 牌

				// 组合
				WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
					WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
					weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
					weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
					weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
					weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
					weaveItem_array.addWeaveItem(weaveItem_item);
				}
				game_end.addWeaveItemArray(weaveItem_array);

				GRR._chi_hu_rights[i].get_right_data(rv);// 获取权位数值
				game_end.addChiHuRight(rv[0]);

				GRR._start_hu_right[i].get_right_data(rv);// 获取权位数值
				game_end.addStartHuRight(rv[0]);

				game_end.addProvidePlayer(GRR._provider[i]);
				game_end.addGameScore(GRR._game_score[i]);// 放炮的人？
				game_end.addGangScore(lGangScore[i]);// 杠牌得分
				game_end.addStartHuScore(GRR._start_hu_score[i]);
				game_end.addResultDes(GRR._result_des[i]);

				// 胡牌
				game_end.addWinOrder(GRR._win_order[i]);

				Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
					lfs.addItem(GRR._lost_fan_shu[i][j]);
				}

				game_end.addLostFanShu(lfs);

			}

		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round) {// 局数到了
				end = true;
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(this.process_player_result(reason));
			} else {
				// 确定下局庄家
				// 以后谁胡牌，下局谁做庄。
				// 流局,庄家下一个
				// 通炮,放炮的玩家

			}
		} else if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT
				|| reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_SYSTEM) {
			end = true;
			real_reason = GameConstants.Game_End_DRAW;// 刘局
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(this.process_player_result(reason));
		}
		game_end.setEndType(real_reason);

		////////////////////////////////////////////////////////////////////// 得分总的
		roomResponse.setGameEnd(game_end);

		this.send_response_to_room(roomResponse);

		record_game_round(game_end);

		// 结束后刷新玩家
		// RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
		// roomResponse2.setGameStatus(_game_status);
		// roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		// this.load_player_info_data(roomResponse2);
		// this.send_response_to_room(roomResponse2);

		// 超时解散
		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
				Player player = this.get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");

			}
		}

		if (end)// 删除
		{
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
		}
		if (!is_sys()) {
			GRR = null;
		}

		// 错误断言
		return false;
	}

	public boolean handler_game_finish_ay(int seat_index, int reason) {
		int real_reason = reason;

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			_player_ready[i] = 0;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();

		roomResponse.setLeftCardCount(0);

		this.load_common_status(roomResponse);
		this.load_room_info_data(roomResponse);

		RoomInfo.Builder room_info = getRoomInfo();
		game_end.setRoomInfo(room_info);

		game_end.setRoundOverType(0);
		game_end.setRoomOverType(0);
		game_end.setGamePlayerNumber(getTablePlayerNumber());

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			game_end.addPao(_player_result.pao[i]);
			game_end.addQiang(_player_result.qiang[i]);
		}

		setGameEndBasicPrama(game_end);

		if (GRR != null) {// reason == MJGameConstants.Game_End_NORMAL || reason
							// == MJGameConstants.Game_End_DRAW ||
			// (reason ==MJGameConstants.Game_End_RELEASE_PLAY && GRR!=null)
			game_end.setRoundOverType(1);
			game_end.setStartTime(GRR._start_time);
			game_end.setGameTypeIndex(GRR._game_type_index);
			roomResponse.setLeftCardCount(GRR._left_card_count);

			// 特别显示的牌
			for (int i = 0; i < GRR._especial_card_count; i++) {
				game_end.addEspecialShowCards(GRR._especial_show_cards[i] + GameConstants.CARD_ESPECIAL_TYPE_DING_GUI);
			}

			GRR._end_type = reason;
			// 杠牌，每个人的分数
			float lGangScore[] = new float[GameConstants.GAME_PLAYER];
			if (reason == GameConstants.Game_End_NORMAL || this._end_reason == GameConstants.Game_End_NORMAL) {
				for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {

					for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
						for (int k = 0; k < GameConstants.GAME_PLAYER; k++) {
							lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
							// _allGRR._gang_score[i].scores[j][k]+=
							// GRR._gang_score[i].scores[j][k];//杠牌，每个人的分数

							// allGangScore[k]+=_allGRR._gang_score[i].scores[j][k];
						}
					}

					// 记录
					for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
						_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
					}

				}

				for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
					GRR._game_score[i] += lGangScore[i];
					GRR._game_score[i] += GRR._start_hu_score[i];// 起手胡分数

					// 记录
					// _all_start_hu_score[i]+=_start_hu_score[i];//起手胡分数
					_player_result.game_score[i] += GRR._game_score[i];

				}
			}

			this.load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);

			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);// 专家
			game_end.setLeftCardCount(GRR._left_card_count);// 剩余牌
			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

			// 设置中鸟数据
			for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i < GRR._count_niao; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao[i]);
			}
			game_end.setCountPickNiao(GRR._count_pick_niao);// 中鸟个数

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					pnc.addItem(GRR._player_niao_cards[i][j]);
				}
				game_end.addPlayerNiaoCards(pnc);
				game_end.addHuResult(GRR._hu_result[i]);

				// 胡的牌
				Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
					hc.addItem(GRR._chi_hu_card[i][j]);
				}

				game_end.addHuCardData(GRR._chi_hu_card[i][0]);
				game_end.addHuCardArray(hc);

				if (GRR._chi_hu_rights[i].bao_ting_card != 0) {
					game_end.addBaoTingCards(
							GRR._chi_hu_rights[i].bao_ting_card + GameConstants.CARD_ESPECIAL_TYPE_BAO_TING);// 报听的牌
				} else {
					game_end.addBaoTingCards(0);// 报听的牌
				}

			}

			// 现在权值只有一位
			long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

			// 设置胡牌描述
			this.set_result_describe();

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {
					// 癞子
					if (_logic.is_magic_card(GRR._cards_data[i][j])) {
						cs.addItem(GRR._cards_data[i][j] + GameConstants.CARD_ESPECIAL_TYPE_GUI);
					} else {
						cs.addItem(GRR._cards_data[i][j]);
					}
				}
				game_end.addCardsData(cs);// 牌

				// 组合
				WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
					WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
					weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
					weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
					weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
					weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
					weaveItem_array.addWeaveItem(weaveItem_item);
				}
				game_end.addWeaveItemArray(weaveItem_array);

				GRR._chi_hu_rights[i].get_right_data(rv);// 获取权位数值
				game_end.addChiHuRight(rv[0]);

				GRR._start_hu_right[i].get_right_data(rv);// 获取权位数值
				game_end.addStartHuRight(rv[0]);

				game_end.addProvidePlayer(GRR._provider[i]);
				game_end.addGameScore(GRR._game_score[i]);// 放炮的人？
				game_end.addGangScore(lGangScore[i]);// 杠牌得分
				game_end.addStartHuScore(GRR._start_hu_score[i]);
				game_end.addResultDes(GRR._result_des[i]);

				// 胡牌
				game_end.addWinOrder(GRR._win_order[i]);

				Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
					lfs.addItem(GRR._lost_fan_shu[i][j]);
				}

				game_end.addLostFanShu(lfs);

			}

		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round) {// 局数到了
				end = true;
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(this.process_player_result(reason));
			} else {
				// 确定下局庄家
				// 以后谁胡牌，下局谁做庄。
				// 流局,庄家下一个
				// 通炮,放炮的玩家

			}
		} else if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT
				|| reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_SYSTEM) {
			end = true;
			real_reason = GameConstants.Game_End_DRAW;// 刘局
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(this.process_player_result(reason));
		}
		game_end.setEndType(real_reason);

		////////////////////////////////////////////////////////////////////// 得分总的
		roomResponse.setGameEnd(game_end);

		this.send_response_to_room(roomResponse);

		record_game_round(game_end);

		// 结束后刷新玩家
		// RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
		// roomResponse2.setGameStatus(_game_status);
		// roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		// this.load_player_info_data(roomResponse2);
		// this.send_response_to_room(roomResponse2);

		// 超时解散
		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
				Player player = this.get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");

			}
		}

		if (end)// 删除
		{
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
		}
		if (!is_sys()) {
			GRR = null;
		}

		// 错误断言
		return false;
	}

	public boolean handler_game_finish_lz(int seat_index, int reason) {
		int real_reason = reason;

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			_player_ready[i] = 0;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();

		roomResponse.setLeftCardCount(0);

		this.load_common_status(roomResponse);
		this.load_room_info_data(roomResponse);

		RoomInfo.Builder room_info = getRoomInfo();
		game_end.setRoomInfo(room_info);

		game_end.setRoundOverType(0);
		game_end.setRoomOverType(0);
		game_end.setGamePlayerNumber(getTablePlayerNumber());

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			game_end.addPao(_player_result.pao[i]);
		}

		setGameEndBasicPrama(game_end);

		if (GRR != null) {// reason == MJGameConstants.Game_End_NORMAL || reason
							// == MJGameConstants.Game_End_DRAW ||
			// (reason ==MJGameConstants.Game_End_RELEASE_PLAY && GRR!=null)
			game_end.setRoundOverType(1);
			game_end.setStartTime(GRR._start_time);
			game_end.setGameTypeIndex(GRR._game_type_index);
			roomResponse.setLeftCardCount(GRR._left_card_count);

			// 特别显示的牌
			for (int i = 0; i < GRR._especial_card_count; i++) {
				game_end.addEspecialShowCards(GRR._especial_show_cards[i] + GameConstants.CARD_ESPECIAL_TYPE_DING_GUI);
			}

			GRR._end_type = reason;
			// 杠牌，每个人的分数
			float lGangScore[] = new float[GameConstants.GAME_PLAYER];
			if (reason == GameConstants.Game_End_NORMAL || this._end_reason == GameConstants.Game_End_NORMAL) {
				for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {

					for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
						for (int k = 0; k < GameConstants.GAME_PLAYER; k++) {
							lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
							// _allGRR._gang_score[i].scores[j][k]+=
							// GRR._gang_score[i].scores[j][k];//杠牌，每个人的分数

							// allGangScore[k]+=_allGRR._gang_score[i].scores[j][k];
						}
					}

					// 记录
					for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
						_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
					}

				}

				for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
					GRR._game_score[i] += lGangScore[i];
					GRR._game_score[i] += GRR._start_hu_score[i];// 起手胡分数

					// 记录
					// _all_start_hu_score[i]+=_start_hu_score[i];//起手胡分数
					_player_result.game_score[i] += GRR._game_score[i];

				}
			}

			this.load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);

			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);// 专家
			game_end.setLeftCardCount(GRR._left_card_count);// 剩余牌
			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

			// 设置中鸟数据
			for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i < GRR._count_niao; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao[i]);
			}
			game_end.setCountPickNiao(GRR._count_pick_niao);// 中鸟个数

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					pnc.addItem(GRR._player_niao_cards[i][j]);
				}
				game_end.addPlayerNiaoCards(pnc);
				game_end.addHuResult(GRR._hu_result[i]);

				// 胡的牌
				Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
					hc.addItem(GRR._chi_hu_card[i][j]);
				}

				game_end.addHuCardData(GRR._chi_hu_card[i][0]);
				game_end.addHuCardArray(hc);

				if (GRR._chi_hu_rights[i].bao_ting_card != 0) {
					game_end.addBaoTingCards(
							GRR._chi_hu_rights[i].bao_ting_card + GameConstants.CARD_ESPECIAL_TYPE_BAO_TING);// 报听的牌
				} else {
					game_end.addBaoTingCards(0);// 报听的牌
				}

			}

			// 现在权值只有一位
			long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

			// 设置胡牌描述
			this.set_result_describe();

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {
					// 癞子
					if (_logic.is_magic_card(GRR._cards_data[i][j])) {
						cs.addItem(GRR._cards_data[i][j] + GameConstants.CARD_ESPECIAL_TYPE_GUI);
					} else {
						cs.addItem(GRR._cards_data[i][j]);
					}
				}
				game_end.addCardsData(cs);// 牌

				// 组合
				WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
					WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
					weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
					weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
					weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
					weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
					weaveItem_array.addWeaveItem(weaveItem_item);
				}
				game_end.addWeaveItemArray(weaveItem_array);

				GRR._chi_hu_rights[i].get_right_data(rv);// 获取权位数值
				game_end.addChiHuRight(rv[0]);

				GRR._start_hu_right[i].get_right_data(rv);// 获取权位数值
				game_end.addStartHuRight(rv[0]);

				game_end.addProvidePlayer(GRR._provider[i]);
				game_end.addGameScore(GRR._game_score[i]);// 放炮的人？
				game_end.addGangScore(lGangScore[i]);// 杠牌得分
				game_end.addStartHuScore(GRR._start_hu_score[i]);
				game_end.addResultDes(GRR._result_des[i]);

				// 胡牌
				game_end.addWinOrder(GRR._win_order[i]);

				Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
					lfs.addItem(GRR._lost_fan_shu[i][j]);
				}

				game_end.addLostFanShu(lfs);

			}

		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round) {// 局数到了
				end = true;
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(this.process_player_result(reason));
			} else {
				// 确定下局庄家
				// 以后谁胡牌，下局谁做庄。
				// 流局,庄家下一个
				// 通炮,放炮的玩家

			}
		} else if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT
				|| reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_SYSTEM) {
			end = true;
			real_reason = GameConstants.Game_End_DRAW;// 刘局
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(this.process_player_result(reason));
		}
		game_end.setEndType(real_reason);

		////////////////////////////////////////////////////////////////////// 得分总的
		roomResponse.setGameEnd(game_end);

		this.send_response_to_room(roomResponse);

		record_game_round(game_end);

		// 结束后刷新玩家
		// RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
		// roomResponse2.setGameStatus(_game_status);
		// roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		// this.load_player_info_data(roomResponse2);
		// this.send_response_to_room(roomResponse2);

		// 超时解散
		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
				Player player = this.get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");

			}
		}

		if (end)// 删除
		{
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
		}
		if (!is_sys()) {
			GRR = null;
		}

		// 错误断言
		return false;
	}

	/**
	 * 
	 * @param seat_index
	 * @param reason
	 * @return
	 */
	public boolean handler_game_finish_henan_hz(int seat_index, int reason) {
		int real_reason = reason;

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			_player_ready[i] = 0;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();

		roomResponse.setLeftCardCount(0);

		this.load_common_status(roomResponse);
		this.load_room_info_data(roomResponse);

		RoomInfo.Builder room_info = getRoomInfo();
		game_end.setRoomInfo(room_info);

		game_end.setGamePlayerNumber(getTablePlayerNumber());
		game_end.setRoundOverType(0);
		game_end.setRoomOverType(0);
		game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间
		game_end.setRunPlayerId(_run_player_id);
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			game_end.addPao(_player_result.pao[i]);
		}

		setGameEndBasicPrama(game_end);

		if (GRR != null) {// reason == MJGameConstants.Game_End_NORMAL || reason
							// == MJGameConstants.Game_End_DRAW ||
			// (reason ==MJGameConstants.Game_End_RELEASE_PLAY && GRR!=null)
			game_end.setRoundOverType(1);
			game_end.setStartTime(GRR._start_time);

			game_end.setGameTypeIndex(GRR._game_type_index);
			roomResponse.setLeftCardCount(GRR._left_card_count);

			// 特别显示的牌
			for (int i = 0; i < GRR._especial_card_count; i++) {
				game_end.addEspecialShowCards(GRR._especial_show_cards[i]);

			}

			GRR._end_type = reason;

			// 杠牌，每个人的分数
			float lGangScore[] = new float[GameConstants.GAME_PLAYER];
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {

				for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
					for (int k = 0; k < GameConstants.GAME_PLAYER; k++) {
						lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
						// _allGRR._gang_score[i].scores[j][k]+=
						// GRR._gang_score[i].scores[j][k];//杠牌，每个人的分数

						// allGangScore[k]+=_allGRR._gang_score[i].scores[j][k];
					}
				}

				// 记录
				for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
					_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
				}

			}

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				GRR._game_score[i] += lGangScore[i];
				GRR._game_score[i] += GRR._start_hu_score[i];// 起手胡分数

				// 记录
				// _all_start_hu_score[i]+=_start_hu_score[i];//起手胡分数
				_player_result.game_score[i] += GRR._game_score[i];

			}

			this.load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);

			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);// 专家
			game_end.setLeftCardCount(GRR._left_card_count);// 剩余牌
			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

			// 设置中鸟数据
			for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i < GRR._count_niao; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao[i]);
			}
			game_end.setCountPickNiao(GRR._count_pick_niao);// 中鸟个数

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					pnc.addItem(GRR._player_niao_cards[i][j]);
				}
				game_end.addPlayerNiaoCards(pnc);
				game_end.addHuResult(GRR._hu_result[i]);

				// 胡的牌
				Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
					hc.addItem(GRR._chi_hu_card[i][j]);
				}

				game_end.addHuCardData(GRR._chi_hu_card[i][0]);
				game_end.addHuCardArray(hc);
			}

			// 现在权值只有一位
			long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

			// 设置胡牌描述
			this.set_result_describe();

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {

					cs.addItem(GRR._cards_data[i][j]);
				}
				game_end.addCardsData(cs);// 牌

				// 组合
				WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
					WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
					weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
					weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
					weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
					weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
					weaveItem_array.addWeaveItem(weaveItem_item);
				}
				game_end.addWeaveItemArray(weaveItem_array);

				GRR._chi_hu_rights[i].get_right_data(rv);// 获取权位数值
				game_end.addChiHuRight(rv[0]);

				GRR._start_hu_right[i].get_right_data(rv);// 获取权位数值
				game_end.addStartHuRight(rv[0]);

				game_end.addProvidePlayer(GRR._provider[i]);
				game_end.addGameScore(GRR._game_score[i]);// 放炮的人？
				game_end.addGangScore(lGangScore[i]);// 杠牌得分
				game_end.addStartHuScore(GRR._start_hu_score[i]);
				game_end.addResultDes(GRR._result_des[i]);

				// 胡牌
				game_end.addWinOrder(GRR._win_order[i]);

				Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
					lfs.addItem(GRR._lost_fan_shu[i][j]);
				}

				game_end.addLostFanShu(lfs);

			}

		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if ((!is_sys()) && _cur_round >= _game_round) {// 局数到了
				end = true;
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(this.process_player_result(reason));
			} else {
				// 确定下局庄家
				// 以后谁胡牌，下局谁做庄。
				// 流局,庄家下一个
				// 通炮,放炮的玩家

			}
		} else if ((!is_sys()) && (reason == GameConstants.Game_End_RELEASE_PLAY
				|| reason == GameConstants.Game_End_RELEASE_NO_BEGIN || reason == GameConstants.Game_End_RELEASE_RESULT
				|| reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_SYSTEM)) {
			end = true;
			real_reason = GameConstants.Game_End_DRAW;// 刘局
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(this.process_player_result(reason));
		}
		game_end.setEndType(real_reason);

		////////////////////////////////////////////////////////////////////// 得分总的
		roomResponse.setGameEnd(game_end);

		this.send_response_to_room(roomResponse);

		record_game_round(game_end);

		// 结束后刷新玩家
		// RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
		// roomResponse2.setGameStatus(_game_status);
		// roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		// this.load_player_info_data(roomResponse2);
		// this.send_response_to_room(roomResponse2);

		// 超时解散
		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
				Player player = this.get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");

			}
		}

		if (end)// 删除
		{
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
		}
		if (!is_sys()) {
			GRR = null;
		}

		if (is_sys()) {
			clear_score_in_gold_room();
		}
		// 错误断言
		return false;
	}

	/**
	 * 
	 * @param seat_index
	 * @param reason
	 * @return
	 */
	public boolean handler_game_finish_lxcg(int seat_index, int reason) {
		int real_reason = reason;

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			_player_ready[i] = 0;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();

		roomResponse.setLeftCardCount(0);

		this.load_common_status(roomResponse);
		this.load_room_info_data(roomResponse);

		RoomInfo.Builder room_info = getRoomInfo();
		game_end.setRoomInfo(room_info);

		game_end.setRoundOverType(0);
		game_end.setRoomOverType(0);
		game_end.setGamePlayerNumber(getTablePlayerNumber());

		game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			game_end.addPao(_player_result.pao[i]);
		}

		setGameEndBasicPrama(game_end);

		if (GRR != null) {// reason == MJGameConstants.Game_End_NORMAL || reason
							// == MJGameConstants.Game_End_DRAW ||
			// (reason ==MJGameConstants.Game_End_RELEASE_PLAY && GRR!=null)
			game_end.setRoundOverType(1);
			game_end.setStartTime(GRR._start_time);

			game_end.setGameTypeIndex(GRR._game_type_index);
			roomResponse.setLeftCardCount(GRR._left_card_count);

			// 特别显示的牌
			for (int i = 0; i < GRR._especial_card_count; i++) {
				game_end.addEspecialShowCards(GRR._especial_show_cards[i]);

			}

			GRR._end_type = reason;

			// 杠牌，每个人的分数
			float lGangScore[] = new float[GameConstants.GAME_PLAYER];
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {

				// 记录
				for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
					_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
				}

			}

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				_player_result.game_score[i] += GRR._game_score[i];

			}

			this.load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);

			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);// 专家
			game_end.setLeftCardCount(GRR._left_card_count);// 剩余牌
			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

			// 设置中鸟数据
			for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i < GRR._count_niao; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao[i]);
			}
			game_end.setCountPickNiao(GRR._count_pick_niao);// 中鸟个数

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					pnc.addItem(GRR._player_niao_cards[i][j]);
				}
				game_end.addPlayerNiaoCards(pnc);
				game_end.addHuResult(GRR._hu_result[i]);

				// 胡的牌
				Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
					hc.addItem(GRR._chi_hu_card[i][j]);
				}

				game_end.addHuCardData(GRR._chi_hu_card[i][0]);
				game_end.addHuCardArray(hc);
			}

			// 现在权值只有一位
			long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

			// 设置胡牌描述
			this.set_result_describe();

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {

					cs.addItem(GRR._cards_data[i][j]);
				}
				game_end.addCardsData(cs);// 牌

				// 组合
				WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
					WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
					weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
					weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
					weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
					weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
					weaveItem_array.addWeaveItem(weaveItem_item);
				}
				game_end.addWeaveItemArray(weaveItem_array);

				GRR._chi_hu_rights[i].get_right_data(rv);// 获取权位数值
				game_end.addChiHuRight(rv[0]);

				GRR._start_hu_right[i].get_right_data(rv);// 获取权位数值
				game_end.addStartHuRight(rv[0]);

				game_end.addProvidePlayer(GRR._provider[i]);
				game_end.addGameScore(GRR._game_score[i]);// 放炮的人？
				game_end.addGangScore(lGangScore[i]);// 杠牌得分
				game_end.addStartHuScore(GRR._start_hu_score[i]);
				game_end.addResultDes(GRR._result_des[i]);

				// 胡牌
				game_end.addWinOrder(GRR._win_order[i]);

				Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
					lfs.addItem(GRR._lost_fan_shu[i][j]);
				}

				game_end.addLostFanShu(lfs);

			}

		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if ((!is_sys()) && _cur_round >= _game_round) {// 局数到了
				end = true;
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(this.process_player_result(reason));
			} else {
				// 确定下局庄家
				// 以后谁胡牌，下局谁做庄。
				// 流局,庄家下一个
				// 通炮,放炮的玩家

			}
		} else if ((!is_sys()) && (reason == GameConstants.Game_End_RELEASE_PLAY
				|| reason == GameConstants.Game_End_RELEASE_NO_BEGIN || reason == GameConstants.Game_End_RELEASE_RESULT
				|| reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_SYSTEM)) {
			end = true;
			real_reason = GameConstants.Game_End_DRAW;// 刘局
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(this.process_player_result(reason));
		}
		game_end.setEndType(real_reason);

		////////////////////////////////////////////////////////////////////// 得分总的
		roomResponse.setGameEnd(game_end);

		this.send_response_to_room(roomResponse);

		record_game_round(game_end);

		// 结束后刷新玩家
		// RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
		// roomResponse2.setGameStatus(_game_status);
		// roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		// this.load_player_info_data(roomResponse2);
		// this.send_response_to_room(roomResponse2);

		// 超时解散
		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
				Player player = this.get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");

			}
		}

		if (end)// 删除
		{
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
		}
		if (!is_sys()) {
			GRR = null;
		}

		if (is_sys()) {
			clear_score_in_gold_room();
		}
		// 错误断言
		return false;
	}

	public boolean handler_game_finish_henan_xc(int seat_index, int reason) {
		int real_reason = reason;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_ready[i] = 0;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();

		roomResponse.setLeftCardCount(0);

		this.load_common_status(roomResponse);
		this.load_room_info_data(roomResponse);

		RoomInfo.Builder room_info = getRoomInfo();
		game_end.setRoomInfo(room_info);
		game_end.setRoundOverType(0);
		game_end.setGamePlayerNumber(getTablePlayerNumber());
		game_end.setRoomOverType(0);

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			game_end.addPao(_player_result.pao[i]);
		}

		setGameEndBasicPrama(game_end);

		if (GRR != null) {
			game_end.setRoundOverType(1);
			game_end.setStartTime(GRR._start_time);
			game_end.setGameTypeIndex(GRR._game_type_index);
			roomResponse.setLeftCardCount(GRR._left_card_count);

			for (int i = 0; i < GRR._especial_card_count; i++) {
				game_end.addEspecialShowCards(GRR._especial_show_cards[i] + GameConstants.CARD_ESPECIAL_TYPE_HUN);
			}

			GRR._end_type = reason;

			float lGangScore[] = new float[getTablePlayerNumber()];
			if (reason == GameConstants.Game_End_NORMAL || has_rule(GameConstants.GAME_RULE_HENAN_HZBHG)) {
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
						for (int k = 0; k < getTablePlayerNumber(); k++) {
							lGangScore[k] += GRR._gang_score[i].scores[j][k];
						}
					}

					for (int j = 0; j < getTablePlayerNumber(); j++) {
						_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
					}
				}

				for (int i = 0; i < getTablePlayerNumber(); i++) {
					GRR._game_score[i] += lGangScore[i];
					GRR._game_score[i] += GRR._start_hu_score[i];

					_player_result.game_score[i] += GRR._game_score[i];
				}
			}

			this.load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);

			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);
			game_end.setLeftCardCount(GRR._left_card_count);
			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					pnc.addItem(GRR._player_niao_cards[i][j]);
				}
				game_end.addPlayerNiaoCards(pnc);
				game_end.addHuResult(GRR._hu_result[i]);

				Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
					hc.addItem(GRR._chi_hu_card[i][j]);
				}

				game_end.addHuCardData(GRR._chi_hu_card[i][0]);
				game_end.addHuCardArray(hc);

				if (GRR._chi_hu_rights[i].bao_ting_card != 0) {
					game_end.addBaoTingCards(
							GRR._chi_hu_rights[i].bao_ting_card + GameConstants.CARD_ESPECIAL_TYPE_BAO_TING);
				} else {
					game_end.addBaoTingCards(0);
				}
			}

			long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

			this.set_result_describe();

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {
					if (_logic.is_magic_card(GRR._cards_data[i][j])) {
						cs.addItem(GRR._cards_data[i][j] + GameConstants.CARD_ESPECIAL_TYPE_HUN);
					} else {
						cs.addItem(GRR._cards_data[i][j]);
					}
				}
				game_end.addCardsData(cs);

				WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
					WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
					weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
					weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
					weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
					weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
					weaveItem_array.addWeaveItem(weaveItem_item);
				}
				game_end.addWeaveItemArray(weaveItem_array);

				GRR._chi_hu_rights[i].get_right_data(rv);
				game_end.addChiHuRight(rv[0]);

				GRR._start_hu_right[i].get_right_data(rv);
				game_end.addStartHuRight(rv[0]);

				game_end.addProvidePlayer(GRR._provider[i]);
				game_end.addGameScore(GRR._game_score[i]);
				game_end.addGangScore(lGangScore[i]);
				game_end.addStartHuScore(GRR._start_hu_score[i]);
				game_end.addResultDes(GRR._result_des[i]);

				game_end.addWinOrder(GRR._win_order[i]);

				Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < getTablePlayerNumber(); j++) {
					lfs.addItem(GRR._lost_fan_shu[i][j]);
				}

				game_end.addLostFanShu(lfs);
			}
		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round) { // 局数到了
				end = true;
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(this.process_player_result(reason));
			} else {
				// 确定下局庄家
				// 以后谁胡牌，下局谁做庄。
				// 流局,庄家下一个
				// 通炮,放炮的玩家
			}
		} else if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT
				|| reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_SYSTEM) {
			end = true;
			real_reason = GameConstants.Game_End_DRAW;
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(this.process_player_result(reason));
		}
		game_end.setEndType(real_reason);

		roomResponse.setGameEnd(game_end);

		this.send_response_to_room(roomResponse);

		record_game_round(game_end);

		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < getTablePlayerNumber(); j++) {
				Player player = this.get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");

			}
		}

		if (end) {
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
		}
		if (!is_sys()) {
			GRR = null;
		}

		return false;
	}

	public boolean handler_game_finish_henan(int seat_index, int reason) {
		int real_reason = reason;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_ready[i] = 0;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();

		roomResponse.setLeftCardCount(0);

		this.load_common_status(roomResponse);
		this.load_room_info_data(roomResponse);

		RoomInfo.Builder room_info = getRoomInfo();
		game_end.setRoomInfo(room_info);
		game_end.setRoundOverType(0);
		game_end.setGamePlayerNumber(getTablePlayerNumber());
		game_end.setRoomOverType(0);
		game_end.setEndTime(System.currentTimeMillis() / 1000L);

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			game_end.addPao(_player_result.pao[i]);
		}

		setGameEndBasicPrama(game_end);

		if (GRR != null) {// reason == MJGameConstants.Game_End_NORMAL || reason
							// == MJGameConstants.Game_End_DRAW ||
			// (reason ==MJGameConstants.Game_End_RELEASE_PLAY && GRR!=null)
			game_end.setRoundOverType(1);
			game_end.setStartTime(GRR._start_time);
			game_end.setGameTypeIndex(GRR._game_type_index);
			roomResponse.setLeftCardCount(GRR._left_card_count);

			// 特别显示的牌
			for (int i = 0; i < GRR._especial_card_count; i++) {
				game_end.addEspecialShowCards(GRR._especial_show_cards[i] + GameConstants.CARD_ESPECIAL_TYPE_HUN);
			}

			GRR._end_type = reason;
			// 杠牌，每个人的分数
			float lGangScore[] = new float[getTablePlayerNumber()];

			if (reason != GameConstants.Game_End_DRAW || (has_rule(GameConstants.GAME_RULE_HENAN_HZBHG))) {
				for (int i = 0; i < getTablePlayerNumber(); i++) {

					for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
						for (int k = 0; k < getTablePlayerNumber(); k++) {
							lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
							// _allGRR._gang_score[i].scores[j][k]+=
							// GRR._gang_score[i].scores[j][k];//杠牌，每个人的分数

							// allGangScore[k]+=_allGRR._gang_score[i].scores[j][k];
						}
					}

					// 记录
					for (int j = 0; j < getTablePlayerNumber(); j++) {
						_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
					}

				}

				for (int i = 0; i < getTablePlayerNumber(); i++) {
					GRR._game_score[i] += lGangScore[i];
					GRR._game_score[i] += GRR._start_hu_score[i];// 起手胡分数

					// 记录
					// _all_start_hu_score[i]+=_start_hu_score[i];//起手胡分数
					_player_result.game_score[i] += GRR._game_score[i];

				}
			}

			this.load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);

			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);// 专家
			game_end.setLeftCardCount(GRR._left_card_count);// 剩余牌
			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

			// // 设置中鸟数据
			// for (int i = 0; i < MJGameConstants.MAX_NIAO_CARD && i <
			// GRR._count_niao; i++) {
			// game_end.addCardsDataNiao(GRR._cards_data_niao[i]);
			// }
			// game_end.setCountPickNiao(GRR._count_pick_niao);// 中鸟个数

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					pnc.addItem(GRR._player_niao_cards[i][j]);
				}
				game_end.addPlayerNiaoCards(pnc);
				game_end.addHuResult(GRR._hu_result[i]);

				// 胡的牌
				Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
					hc.addItem(GRR._chi_hu_card[i][j]);
				}

				game_end.addHuCardData(GRR._chi_hu_card[i][0]);
				game_end.addHuCardArray(hc);

				if (GRR._chi_hu_rights[i].bao_ting_card != 0) {
					game_end.addBaoTingCards(
							GRR._chi_hu_rights[i].bao_ting_card + GameConstants.CARD_ESPECIAL_TYPE_BAO_TING);// 报听的牌
				} else {
					game_end.addBaoTingCards(0);// 报听的牌
				}

			}

			// 现在权值只有一位
			long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

			// 设置胡牌描述
			this.set_result_describe();

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {
					// 癞子
					if (_logic.is_magic_card(GRR._cards_data[i][j])) {
						cs.addItem(GRR._cards_data[i][j] + GameConstants.CARD_ESPECIAL_TYPE_HUN);
					} else {
						cs.addItem(GRR._cards_data[i][j]);
					}
				}
				game_end.addCardsData(cs);// 牌

				// 组合
				WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
					WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
					weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
					weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
					weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
					weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
					weaveItem_array.addWeaveItem(weaveItem_item);
				}
				game_end.addWeaveItemArray(weaveItem_array);

				GRR._chi_hu_rights[i].get_right_data(rv);// 获取权位数值
				game_end.addChiHuRight(rv[0]);

				GRR._start_hu_right[i].get_right_data(rv);// 获取权位数值
				game_end.addStartHuRight(rv[0]);

				game_end.addProvidePlayer(GRR._provider[i]);
				game_end.addGameScore(GRR._game_score[i]);// 放炮的人？
				game_end.addGangScore(lGangScore[i]);// 杠牌得分
				game_end.addStartHuScore(GRR._start_hu_score[i]);
				game_end.addResultDes(GRR._result_des[i]);

				// 胡牌
				game_end.addWinOrder(GRR._win_order[i]);

				Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
					lfs.addItem(GRR._lost_fan_shu[i][j]);
				}

				game_end.addLostFanShu(lfs);

			}

		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round) {// 局数到了
				end = true;
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(this.process_player_result(reason));

				if (is_mj_type(GameConstants.GAME_TYPE_NEW_ZHU_MA_DIAN)) {
					long durration = gameTimer.get();
					long minuts = TimeUnit.MILLISECONDS.toMinutes(durration);

					if (minuts == 0)
						minuts = 1;

					// 所有小局经历的时间差，单位：分钟
					game_end.setTunShu((int) minuts);
				}
			} else {
				// 确定下局庄家
				// 以后谁胡牌，下局谁做庄。
				// 流局,庄家下一个
				// 通炮,放炮的玩家

			}
		} else if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT
				|| reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_SYSTEM) {
			end = true;
			real_reason = GameConstants.Game_End_DRAW;// 刘局
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(this.process_player_result(reason));

			if (is_mj_type(GameConstants.GAME_TYPE_NEW_ZHU_MA_DIAN)) {
				long durration = gameTimer.get();
				long minuts = TimeUnit.MILLISECONDS.toMinutes(durration);

				if (minuts == 0)
					minuts = 1;

				// 所有小局经历的时间差，单位：分钟
				game_end.setTunShu((int) minuts);
			}
		}
		game_end.setEndType(real_reason);

		////////////////////////////////////////////////////////////////////// 得分总的
		roomResponse.setGameEnd(game_end);

		this.send_response_to_room(roomResponse);

		record_game_round(game_end);

		// 结束后刷新玩家
		// RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
		// roomResponse2.setGameStatus(_game_status);
		// roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		// this.load_player_info_data(roomResponse2);
		// this.send_response_to_room(roomResponse2);

		// 超时解散
		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
				Player player = this.get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");

			}
		}

		if (end)// 删除
		{
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
		}
		if (!is_sys()) {
			GRR = null;
		}

		// 错误断言
		return false;
	}

	public boolean handler_game_finish_henan_xy(int seat_index, int reason) {
		int real_reason = reason;

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			_player_ready[i] = 0;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();

		roomResponse.setLeftCardCount(0);

		this.load_common_status(roomResponse);
		this.load_room_info_data(roomResponse);

		// 这里记录了两次，先这样
		RoomInfo.Builder room_info = getRoomInfo();
		game_end.setRoomInfo(room_info);
		game_end.setRunPlayerId(_run_player_id);
		game_end.setRoundOverType(0);
		game_end.setRoomOverType(0);
		game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间
		game_end.setGamePlayerNumber(getTablePlayerNumber());
		if (GRR != null) {// reason == MJGameConstants.Game_End_NORMAL || reason
							// == MJGameConstants.Game_End_DRAW ||
			// (reason ==MJGameConstants.Game_End_RELEASE_PLAY && GRR!=null)
			game_end.setRoundOverType(1);
			game_end.setStartTime(GRR._start_time);

			game_end.setGameTypeIndex(GRR._game_type_index);
			roomResponse.setLeftCardCount(GRR._left_card_count);

			// 特别显示的牌
			for (int i = 0; i < GRR._especial_card_count; i++) {
				game_end.addEspecialShowCards(GRR._especial_show_cards[i]);

			}

			GRR._end_type = reason;

			this.load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);

			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);// 专家
			game_end.setLeftCardCount(GRR._left_card_count);// 剩余牌
			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

			// 设置中鸟数据
			for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i < GRR._count_niao; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao[i]);
			}
			game_end.setCountPickNiao(GRR._count_pick_niao);// 中鸟个数

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					pnc.addItem(GRR._player_niao_cards[i][j]);
				}
				game_end.addPlayerNiaoCards(pnc);
				game_end.addHuResult(GRR._hu_result[i]);

				// 胡的牌
				Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
					hc.addItem(GRR._chi_hu_card[i][j]);
				}

				game_end.addHuCardData(GRR._chi_hu_card[i][0]);
				game_end.addHuCardArray(hc);
			}

			// 现在权值只有一位
			long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

			// 设置胡牌描述
			this.set_result_describe();

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {

					cs.addItem(GRR._cards_data[i][j]);
				}
				game_end.addCardsData(cs);// 牌

				// 组合
				WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
					WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
					weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
					weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
					weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
					weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
					weaveItem_array.addWeaveItem(weaveItem_item);
				}
				game_end.addWeaveItemArray(weaveItem_array);

				GRR._chi_hu_rights[i].get_right_data(rv);// 获取权位数值
				game_end.addChiHuRight(rv[0]);

				GRR._start_hu_right[i].get_right_data(rv);// 获取权位数值
				game_end.addStartHuRight(rv[0]);

				game_end.addProvidePlayer(GRR._provider[i]);
				game_end.addGameScore(GRR._game_score[i]);// 放炮的人？
				game_end.addGangScore(0);// 杠牌得分
				game_end.addStartHuScore(GRR._start_hu_score[i]);
				game_end.addResultDes(GRR._result_des[i]);

				// 胡牌
				game_end.addWinOrder(GRR._win_order[i]);

				Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
					lfs.addItem(GRR._lost_fan_shu[i][j]);
				}

				game_end.addLostFanShu(lfs);

			}

		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round && (!is_sys())) {// 局数到了
				end = true;
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(this.process_player_result(reason));
			} else {
				// 确定下局庄家
				// 以后谁胡牌，下局谁做庄。
				// 流局,庄家下一个
				// 通炮,放炮的玩家

			}
		} else if ((!is_sys()) && (reason == GameConstants.Game_End_RELEASE_PLAY
				|| reason == GameConstants.Game_End_RELEASE_NO_BEGIN || reason == GameConstants.Game_End_RELEASE_RESULT
				|| reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_SYSTEM)) {
			end = true;
			real_reason = GameConstants.Game_End_DRAW;// 刘局
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(this.process_player_result(reason));
		}
		game_end.setEndType(real_reason);

		////////////////////////////////////////////////////////////////////// 得分总的
		roomResponse.setGameEnd(game_end);

		this.send_response_to_room(roomResponse);

		record_game_round(game_end);

		// 结束后刷新玩家
		// RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
		// roomResponse2.setGameStatus(_game_status);
		// roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		// this.load_player_info_data(roomResponse2);
		// this.send_response_to_room(roomResponse2);

		// 超时解散
		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
				Player player = this.get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");

			}
		}

		if (end && (!is_sys()))// 删除
		{
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
		}

		if (!is_sys()) {
			GRR = null;
		}

		if (is_sys()) {
			clear_score_in_gold_room();
		}
		// 错误断言
		return false;
	}

	/**
	 * 湖南转转麻将的游戏结束
	 * 
	 * @param seat_index
	 * @param reason
	 */
	protected boolean handler_game_finish_hunan_zz_hz(int seat_index, int reason) {
		int real_reason = reason;

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			_player_ready[i] = 0;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();
		MJ_GAME_END_INFO_EXT.Builder gameEndExtBuilder = MJ_GAME_END_INFO_EXT.newBuilder();

		roomResponse.setLeftCardCount(0);

		this.load_common_status(roomResponse);
		this.load_room_info_data(roomResponse);

		// 这里记录了两次，先这样
		RoomInfo.Builder room_info = getRoomInfo();
		game_end.setRoomInfo(room_info);
		game_end.setRunPlayerId(_run_player_id);
		game_end.setRoundOverType(0);
		game_end.setGamePlayerNumber(getTablePlayerNumber());
		game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间
		if (
				 is_mj_type(GameConstants.GAME_TYPE_MJ_YUYANG_HONGZHON)
				|| is_mj_type(GameConstants.GAME_TYPE_HONG_ZHONG_MJ_TH)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_HONG_ZHONG_FEI)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_PING_XIANG_ZZ)) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				game_end.addPao(_player_result.pao[i]);
			}
		}
		if (GRR != null) {// reason == MJGameConstants.Game_End_NORMAL || reason
							// == MJGameConstants.Game_End_DRAW ||
			// (reason ==MJGameConstants.Game_End_RELEASE_PLAY && GRR!=null)
			game_end.setRoundOverType(1);
			game_end.setStartTime(GRR._start_time);

			game_end.setGameTypeIndex(GRR._game_type_index);
			roomResponse.setLeftCardCount(GRR._left_card_count);

			// 特别显示的牌
			for (int i = 0; i < GRR._especial_card_count; i++) {
				game_end.addEspecialShowCards(GRR._especial_show_cards[i]);
			}

			GRR._end_type = reason;

			// 杠牌，每个人的分数
			float lGangScore[] = new float[getTablePlayerNumber()];
			int ming_gang_score[] = new int[getTablePlayerNumber()];
			int an_gang_score[] = new int[getTablePlayerNumber()];
			int zhi_gang_score[] = new int[getTablePlayerNumber()];
			for (int i = 0; i < getTablePlayerNumber(); i++) {

				for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
					for (int k = 0; k < getTablePlayerNumber(); k++) {
						lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
					}
				}

				for (int j = 0; j < GRR._weave_count[i]; j++) {
					if (GRR._weave_items[i][j].weave_kind == GameConstants.WIK_GANG) {
						int type = GRR._weave_items[i][j].type;
						if (type == GameConstants.GANG_TYPE_JIE_GANG) {
							int score = GRR._weave_items[i][j].weave_score;

							zhi_gang_score[GRR._weave_items[i][j].provide_player] -= score;
							zhi_gang_score[i] += score;
						} else {
							for (int k = 0; k < getTablePlayerNumber(); k++) {
								int score = GRR._weave_items[i][j].weave_score;
								if (k == i) {
									continue;
								}
								if (type == GameConstants.GANG_TYPE_ADD_GANG) {
									ming_gang_score[k] -= score;
									ming_gang_score[i] += score;
								} else {
									an_gang_score[k] -= score;
									an_gang_score[i] += score;
								}
							}
						}
					}
				}
				// 记录
				for (int j = 0; j < getTablePlayerNumber(); j++) {
					_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
				}
			}

			// WalkerGeek 红中比赛场杠分等比变更
			if (is_match()) {
				for (int k = 0; k < getTablePlayerNumber(); k++) {
					lGangScore[k] *= getSettleBase(k);// 杠牌分*比赛场倍数
				}
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				gameEndExtBuilder.addPiao(piao_score[i]);
				gameEndExtBuilder.addHuScore(hu_score[i]);
				gameEndExtBuilder.addHuType(hu_type[i]);
				gameEndExtBuilder.addNiaoScore(niao_score[i]);
				gameEndExtBuilder.addMingGangScore(ming_gang_score[i]);
				gameEndExtBuilder.addAnGangScore(an_gang_score[i]);
				gameEndExtBuilder.addZhiGangScore(zhi_gang_score[i]);
			}
			
			
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				GRR._game_score[i] += lGangScore[i];
				GRR._game_score[i] += GRR._start_hu_score[i]; // 起手胡分数
				if(isCoinRoom()){
					GRR._game_score[i] *= getSettleBaseCoin(i);
				}
				// 记录
				_player_result.game_score[i] += GRR._game_score[i];

			}
			/*if(isCoinRoom()){
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					GRR._game_score[i] *= getSettleBaseCoin(i);
				}
			}*/
			
			this.load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);

			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);// 专家
			game_end.setLeftCardCount(GRR._left_card_count);// 剩余牌
			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

			// 设置中鸟数据
			for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i < GRR._count_niao; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao[i]);
			}

			// 鸟的数据 这里要注意三人场的特殊处理 三人场必须发四个人的鸟 不然显示不全
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					pnc.addItem(GRR._player_niao_cards[i][j]);
				}
				game_end.addPlayerNiaoCards(pnc);
			}

			game_end.setCountPickNiao(GRR._count_pick_niao);// 中鸟个数

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					pnc.addItem(GRR._player_niao_cards[i][j]);
				}
				game_end.addPlayerNiaoCards(pnc);
				game_end.addHuResult(GRR._hu_result[i]);

				// 胡的牌
				Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
					hc.addItem(GRR._chi_hu_card[i][j]);
				}

				game_end.addHuCardData(GRR._chi_hu_card[i][0]);
				game_end.addHuCardArray(hc);
			}

			// 现在权值只有一位
			long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

			// 设置胡牌描述
			this.set_result_describe();

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {

					cs.addItem(GRR._cards_data[i][j]);
				}
				game_end.addCardsData(cs);// 牌

				// 组合
				WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
					WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
					weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
					weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
					weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
					weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
					weaveItem_array.addWeaveItem(weaveItem_item);
				}
				game_end.addWeaveItemArray(weaveItem_array);

				GRR._chi_hu_rights[i].get_right_data(rv);// 获取权位数值
				game_end.addChiHuRight(rv[0]);

				GRR._start_hu_right[i].get_right_data(rv);// 获取权位数值
				game_end.addStartHuRight(rv[0]);

				game_end.addProvidePlayer(GRR._provider[i]);
				game_end.addGameScore(GRR._game_score[i]);// 放炮的人？
				game_end.addGangScore(lGangScore[i]);// 杠牌得分
				game_end.addStartHuScore(GRR._start_hu_score[i]);
				game_end.addResultDes(GRR._result_des[i]);

				// 胡牌
				game_end.addWinOrder(GRR._win_order[i]);

				Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < getTablePlayerNumber(); j++) {
					lfs.addItem(GRR._lost_fan_shu[i][j]);
				}

				game_end.addLostFanShu(lfs);

			}

		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round && (!is_sys())) {// 局数到了
				end = true;
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(this.process_player_result(reason));
			} else {
				// 确定下局庄家
				// 以后谁胡牌，下局谁做庄。
				// 流局,庄家下一个
				// 通炮,放炮的玩家

			}
		} else if ((!is_sys()) && (reason == GameConstants.Game_End_RELEASE_PLAY
				|| reason == GameConstants.Game_End_RELEASE_NO_BEGIN || reason == GameConstants.Game_End_RELEASE_RESULT
				|| reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_SYSTEM)) {
			end = true;
			real_reason = GameConstants.Game_End_DRAW;// 刘局
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(this.process_player_result(reason));
		}
		game_end.setEndType(real_reason);

		////////////////////////////////////////////////////////////////////// 得分总的
		
		game_end.setCommResponse(PBUtil.toByteString(gameEndExtBuilder));
		roomResponse.setGameEnd(game_end);

		roomResponse.setCommResponse(PBUtil.toByteString(gameEndExtBuilder));

		this.send_response_to_room(roomResponse);

		record_game_round(game_end);

		// 结束后刷新玩家
		// RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
		// roomResponse2.setGameStatus(_game_status);
		// roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		// this.load_player_info_data(roomResponse2);
		// this.send_response_to_room(roomResponse2);

		// 超时解散
		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
				Player player = this.get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");

			}
		}

		if (end && (!is_sys()))// 删除
		{
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
		}

		if (!is_sys()) {
			GRR = null;
		}

		if (is_sys()) {
			clear_score_in_gold_room();
		}
		// 错误断言
		return false;
	}

	/**
	 * 湖南红中麻将的游戏结束
	 * 
	 * @param seat_index
	 * @param reason
	 */
	protected boolean handler_game_finish_hunan_hz(int seat_index, int reason) {

		// 比赛场:多次重连不发送结算数据
		if (is_match() && send_game_end) {
			return true;
		}

		int real_reason = reason;

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			_player_ready[i] = 0;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();
		MJ_GAME_END_INFO_EXT.Builder gameEndExtBuilder = MJ_GAME_END_INFO_EXT.newBuilder();

		roomResponse.setLeftCardCount(0);

		this.load_common_status(roomResponse);
		this.load_room_info_data(roomResponse);

		// 这里记录了两次，先这样
		RoomInfo.Builder room_info = getRoomInfo();
		game_end.setRoomInfo(room_info);
		game_end.setRunPlayerId(_run_player_id);
		game_end.setRoundOverType(0);
		game_end.setGamePlayerNumber(getTablePlayerNumber());
		game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间
		if (is_mj_type(GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_YUYANG_HONGZHON)
				|| is_mj_type(GameConstants.GAME_TYPE_HONG_ZHONG_MJ_TH)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_HONG_ZHONG_FEI)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_PING_XIANG_ZZ)) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				game_end.addPao(_player_result.pao[i]);
			}
		}
		if (GRR != null) {
			game_end.setRoundOverType(1);
			game_end.setStartTime(GRR._start_time);

			game_end.setGameTypeIndex(GRR._game_type_index);
			roomResponse.setLeftCardCount(GRR._left_card_count);

			// 特别显示的牌
			for (int i = 0; i < GRR._especial_card_count; i++) {
				game_end.addEspecialShowCards(GRR._especial_show_cards[i]);

			}

			GRR._end_type = reason;

			int ming_gang_score[] = new int[getTablePlayerNumber()];
			int an_gang_score[] = new int[getTablePlayerNumber()];
			// 杠牌，每个人的分数
			float lGangScore[] = new float[getTablePlayerNumber()];
			for (int i = 0; i < getTablePlayerNumber(); i++) {

				for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
					for (int k = 0; k < getTablePlayerNumber(); k++) {

						lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
					}
				}

				for (int j = 0; j < GRR._weave_count[i]; j++) {
					if (GRR._weave_items[i][j].weave_kind == GameConstants.WIK_GANG) {
						int type = GRR._weave_items[i][j].type;
						if (type == GameConstants.GANG_TYPE_JIE_GANG) {
							int score = GRR._weave_items[i][j].weave_score;

							ming_gang_score[GRR._weave_items[i][j].provide_player] -= score;
							ming_gang_score[i] += score;
						} else {
							for (int k = 0; k < getTablePlayerNumber(); k++) {
								int score = GRR._weave_items[i][j].weave_score;
								if (k == i) {
									continue;
								}
								if (type == GameConstants.GANG_TYPE_ADD_GANG) {
									ming_gang_score[k] -= score;
									ming_gang_score[i] += score;
								} else {
									an_gang_score[k] -= score;
									an_gang_score[i] += score;
								}
							}
						}
					}
				}

				// 记录
				for (int j = 0; j < getTablePlayerNumber(); j++) {
					_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
				}
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				gameEndExtBuilder.addPiao(piao_score[i]);
				gameEndExtBuilder.addHuScore(hu_score[i]);
				gameEndExtBuilder.addHuType(hu_type[i]);
				gameEndExtBuilder.addNiaoScore(niao_score[i]);
				gameEndExtBuilder.addMingGangScore(ming_gang_score[i]);
				gameEndExtBuilder.addAnGangScore(an_gang_score[i]);
			}
			// WalkerGeek 18-8-10 红中麻将添加倍率
			if(is_mj_type(GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ)){
				for (int k = 0; k < getTablePlayerNumber(); k++) {
					lGangScore[k] *= get_bei_lv();// 杠牌分*比赛场倍数
				}
			}
			// WalkerGeek 红中比赛场杠分等比变更
			if (is_match()) {
				if (is_mj_type(GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ)
						|| is_mj_type(GameConstants.GAME_TYPE_MJ_YUYANG_HONGZHON)
						|| is_mj_type(GameConstants.GAME_TYPE_HONG_ZHONG_MJ_TH)
						|| is_mj_type(GameConstants.GAME_TYPE_MJ_HONG_ZHONG_FEI)
						|| is_mj_type(GameConstants.GAME_TYPE_MJ_PING_XIANG_ZZ)) {
					for (int k = 0; k < getTablePlayerNumber(); k++) {
						lGangScore[k] *= getSettleBase(k);// 杠牌分*比赛场倍数
					}
				}
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				GRR._game_score[i] += lGangScore[i];
				GRR._game_score[i] += GRR._start_hu_score[i]; // 起手胡分数
				if(isCoinRoom()){
					GRR._game_score[i] *= getSettleBaseCoin(i);
				}
				// 记录
				_player_result.game_score[i] += GRR._game_score[i];

			}

			this.load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);

			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);// 专家
			game_end.setLeftCardCount(GRR._left_card_count);// 剩余牌
			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

			// 设置中鸟数据
			for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i < GRR._count_niao; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao[i]);
			}

			// 鸟的数据 这里要注意三人场的特殊处理 三人场必须发四个人的鸟 不然显示不全
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					pnc.addItem(GRR._player_niao_cards[i][j]);
				}
				game_end.addPlayerNiaoCards(pnc);
			}

			game_end.setCountPickNiao(GRR._count_pick_niao);// 中鸟个数

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					pnc.addItem(GRR._player_niao_cards[i][j]);
				}
				game_end.addPlayerNiaoCards(pnc);
				game_end.addHuResult(GRR._hu_result[i]);

				// 胡的牌
				Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
					hc.addItem(GRR._chi_hu_card[i][j]);
				}

				game_end.addHuCardData(GRR._chi_hu_card[i][0]);
				game_end.addHuCardArray(hc);
			}

			// 现在权值只有一位
			long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

			// 设置胡牌描述
			this.set_result_describe();

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {

					cs.addItem(GRR._cards_data[i][j]);
				}
				game_end.addCardsData(cs);// 牌

				// 组合
				WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
					WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
					weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
					weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
					weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
					weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
					weaveItem_array.addWeaveItem(weaveItem_item);
				}
				game_end.addWeaveItemArray(weaveItem_array);

				GRR._chi_hu_rights[i].get_right_data(rv);// 获取权位数值
				game_end.addChiHuRight(rv[0]);

				GRR._start_hu_right[i].get_right_data(rv);// 获取权位数值
				game_end.addStartHuRight(rv[0]);

				game_end.addProvidePlayer(GRR._provider[i]);
				game_end.addGameScore(GRR._game_score[i]);// 放炮的人？
				game_end.addGangScore(lGangScore[i]);// 杠牌得分
				game_end.addStartHuScore(GRR._start_hu_score[i]);
				game_end.addResultDes(GRR._result_des[i]);

				// 胡牌
				game_end.addWinOrder(GRR._win_order[i]);

				Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < getTablePlayerNumber(); j++) {
					lfs.addItem(GRR._lost_fan_shu[i][j]);
				}

				game_end.addLostFanShu(lfs);

			}

		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round && (!is_sys())) {// 局数到了
				end = true;
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(this.process_player_result(reason));
			} else {
				// 确定下局庄家
				// 以后谁胡牌，下局谁做庄。
				// 流局,庄家下一个
				// 通炮,放炮的玩家

			}
		} else if ((!is_sys()) && (reason == GameConstants.Game_End_RELEASE_PLAY
				|| reason == GameConstants.Game_End_RELEASE_NO_BEGIN || reason == GameConstants.Game_End_RELEASE_RESULT
				|| reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_SYSTEM)) {
			end = true;
			real_reason = GameConstants.Game_End_DRAW;// 刘局
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(this.process_player_result(reason));
		}
		game_end.setEndType(real_reason);
		game_end.setCommResponse(PBUtil.toByteString(gameEndExtBuilder));
		roomResponse.setGameEnd(game_end);

		roomResponse.setCommResponse(PBUtil.toByteString(gameEndExtBuilder));
		// 记录小结算发送状态
		send_game_end = true;
		this.send_response_to_room(roomResponse);

		record_game_round(game_end);

		// 超时解散
		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
				Player player = this.get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");

			}
		}

		if (end && (!is_sys()))// 删除
		{
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
		}

		if (!is_sys()) {
			GRR = null;
		}

		if (is_sys()) {
			clear_score_in_gold_room();
		}
		// 错误断言
		return false;
	}

	public boolean handler_game_finish_henan_lygc(int seat_index, int reason) {
		int real_reason = reason;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_ready[i] = 0;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();

		roomResponse.setLeftCardCount(0);

		this.load_common_status(roomResponse);
		this.load_room_info_data(roomResponse);

		RoomInfo.Builder room_info = getRoomInfo();
		game_end.setRoomInfo(room_info);
		game_end.setRoundOverType(0);
		game_end.setGamePlayerNumber(getTablePlayerNumber());
		game_end.setRoomOverType(0);

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			game_end.addPao(_player_result.pao[i]);
		}

		if (GRR != null) {// reason == MJGameConstants.Game_End_NORMAL || reason
							// == MJGameConstants.Game_End_DRAW ||
			// (reason ==MJGameConstants.Game_End_RELEASE_PLAY && GRR!=null)
			game_end.setRoundOverType(1);
			game_end.setStartTime(GRR._start_time);
			game_end.setGameTypeIndex(GRR._game_type_index);
			roomResponse.setLeftCardCount(GRR._left_card_count);

			// 特别显示的牌
			for (int i = 0; i < GRR._especial_card_count; i++) {
				game_end.addEspecialShowCards(GRR._especial_show_cards[i] + GameConstants.CARD_ESPECIAL_TYPE_CI);
			}

			GRR._end_type = reason;
			// 杠牌，每个人的分数
			float lGangScore[] = new float[getTablePlayerNumber()];

			// TODO: 洛阳杠次
			Lygc_Game_End.Builder lygc_game_end = Lygc_Game_End.newBuilder();

			if (reason != GameConstants.Game_End_DRAW) {
				if (reason == GameConstants.Game_End_NORMAL) {
					lygc_game_end.setGameEndType(1);
				} else {
					lygc_game_end.setGameEndType(3);
				}
				lygc_game_end.setZiMoHu(GRR.is_zi_mo_hu);
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					lygc_game_end.addMingGangScore(GRR.ming_gang_score[i]);
					lygc_game_end.addAnGangScore(GRR.an_gang_score[i]);
					lygc_game_end.addTmpGameScore(GRR._game_score[i]);
				}

				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (reason == GameConstants.Game_End_NORMAL) {
						for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
							for (int k = 0; k < getTablePlayerNumber(); k++) {
								lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
								// _allGRR._gang_score[i].scores[j][k]+=
								// GRR._gang_score[i].scores[j][k];//杠牌，每个人的分数

								// allGangScore[k]+=_allGRR._gang_score[i].scores[j][k];
							}
						}
					}

					// 记录
					for (int j = 0; j < getTablePlayerNumber(); j++) {
						_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
					}
				}

				for (int i = 0; i < getTablePlayerNumber(); i++) {
					GRR._game_score[i] += lGangScore[i];
					GRR._game_score[i] += GRR._start_hu_score[i];// 起手胡分数

					// 记录
					// _all_start_hu_score[i]+=_start_hu_score[i];//起手胡分数
					_player_result.game_score[i] += GRR._game_score[i];

				}
			}

			// TODO 洛阳杠次新的大结算页面的各种‘次’的统计
			for (int p = 0; p < getTablePlayerNumber(); p++) {
				lygc_game_end.addMingCiCiShu(_player_result.ming_ci_ci_shu[p]);
				lygc_game_end.addAnCiCiShu(_player_result.an_ci_ci_shu[p]);
				lygc_game_end.addPiCiCiShu(_player_result.pi_ci_ci_shu[p]);
				lygc_game_end.addZiMoCiShu(_player_result.zi_mo_ci_shu[p]);
				lygc_game_end.addBaoCiCiShu(_player_result.bao_ci_ci_shu[p]);
			}

			game_end.setCommResponse(PBUtil.toByteString(lygc_game_end));

			this.load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);

			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);// 专家
			game_end.setLeftCardCount(GRR._left_card_count);// 剩余牌
			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

			// // 设置中鸟数据
			// for (int i = 0; i < MJGameConstants.MAX_NIAO_CARD && i <
			// GRR._count_niao; i++) {
			// game_end.addCardsDataNiao(GRR._cards_data_niao[i]);
			// }
			// game_end.setCountPickNiao(GRR._count_pick_niao);// 中鸟个数

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					pnc.addItem(GRR._player_niao_cards[i][j]);
				}
				game_end.addPlayerNiaoCards(pnc);
				game_end.addHuResult(GRR._hu_result[i]);

				// 胡的牌
				Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
					hc.addItem(GRR._chi_hu_card[i][j]);
				}

				game_end.addHuCardData(GRR._chi_hu_card[i][0]);
				game_end.addHuCardArray(hc);

				if (GRR._chi_hu_rights[i].bao_ting_card != 0) {
					game_end.addBaoTingCards(
							GRR._chi_hu_rights[i].bao_ting_card + GameConstants.CARD_ESPECIAL_TYPE_BAO_TING);// 报听的牌
				} else {
					game_end.addBaoTingCards(0);// 报听的牌
				}

			}

			// 现在权值只有一位
			long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

			// 设置胡牌描述
			this.set_result_describe();

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {
					// 癞子
					if (_logic.is_ci_card(_logic.switch_to_card_index(GRR._cards_data[i][j]))) {
						cs.addItem(GRR._cards_data[i][j] + GameConstants.CARD_ESPECIAL_TYPE_CI);
					} else {
						cs.addItem(GRR._cards_data[i][j]);
					}
				}
				game_end.addCardsData(cs);// 牌

				// 组合
				WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
					WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
					weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
					weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
					weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
					weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
					weaveItem_array.addWeaveItem(weaveItem_item);
				}
				game_end.addWeaveItemArray(weaveItem_array);

				GRR._chi_hu_rights[i].get_right_data(rv);// 获取权位数值
				game_end.addChiHuRight(rv[0]);

				GRR._start_hu_right[i].get_right_data(rv);// 获取权位数值
				game_end.addStartHuRight(rv[0]);

				game_end.addProvidePlayer(GRR._provider[i]);
				game_end.addGameScore(GRR._game_score[i]);// 放炮的人？
				game_end.addGangScore(lGangScore[i]);// 杠牌得分
				game_end.addStartHuScore(GRR._start_hu_score[i]);
				game_end.addResultDes(GRR._result_des[i]);

				// 胡牌
				game_end.addWinOrder(GRR._win_order[i]);

				Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
					lfs.addItem(GRR._lost_fan_shu[i][j]);
				}

				game_end.addLostFanShu(lfs);

			}

		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round) {// 局数到了
				end = true;
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(this.process_player_result(reason));
			} else {
				// 确定下局庄家
				// 以后谁胡牌，下局谁做庄。
				// 流局,庄家下一个
				// 通炮,放炮的玩家

			}
		} else if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT
				|| reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_SYSTEM) {
			end = true;
			real_reason = GameConstants.Game_End_DRAW;// 刘局
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(this.process_player_result(reason));
		}
		game_end.setEndType(real_reason);

		////////////////////////////////////////////////////////////////////// 得分总的
		roomResponse.setGameEnd(game_end);

		this.send_response_to_room(roomResponse);

		record_game_round(game_end);

		// 结束后刷新玩家
		// RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
		// roomResponse2.setGameStatus(_game_status);
		// roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		// this.load_player_info_data(roomResponse2);
		// this.send_response_to_room(roomResponse2);

		// 超时解散
		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
				Player player = this.get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");

			}
		}

		if (end)// 删除
		{
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
		}
		if (!is_sys()) {
			GRR = null;
		}

		// 错误断言
		return false;
	}

	@Override
	public int analyse_chi_hu_card(int cards_index[], WeaveItem weaveItems[], int weave_count, int cur_card,
			ChiHuRight chiHuRight, int card_type, int _seat_index) {

		if (GameConstants.GAME_TYPE_ZZ == _game_type_index
				|| GameConstants.GAME_TYPE_HENAN_ZHUAN_ZHUAN == _game_type_index
				|| is_mj_type(GameConstants.GAME_TYPE_HALL_ZHUAN_ZHUAN_MJ)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_SAN_MEN_XIA)) { // liuyan
																			// 2017/7/10
			return analyse_chi_hu_card_zz(cards_index, weaveItems, weave_count, cur_card, chiHuRight, card_type);
		} else if (is_mj_type(GameConstants.GAME_TYPE_HZ) || is_mj_type(GameConstants.GAME_TYPE_FLS_HZ_LX)
				|| is_mj_type(GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_YUYANG_HONGZHON)
				|| is_mj_type(GameConstants.GAME_TYPE_HONG_ZHONG_MJ_TH)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_HONG_ZHONG_FEI)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_PING_XIANG_ZZ)) {
			if (SysParamServerUtil.is_new_algorithm(3000, 3000, 1)) {
				return analyse_chi_hu_card_hz_new(cards_index, weaveItems, weave_count, cur_card, chiHuRight,
						card_type);
			} else {
				return analyse_chi_hu_card_hz(cards_index, weaveItems, weave_count, cur_card, chiHuRight, card_type);
			}
		} else if (GameConstants.GAME_TYPE_CS == _game_type_index
				|| is_mj_type(GameConstants.GAME_TYPE_HALL_CHANG_SHA_MJ)
				|| is_mj_type(GameConstants.GAME_TYPE_THJ_CS)) {
			return analyse_chi_hu_card_cs(cards_index, weaveItems, weave_count, cur_card, chiHuRight, card_type);
		} else if (GameConstants.GAME_TYPE_SHUANGGUI == _game_type_index
				|| GameConstants.GAME_TYPE_HUNAN_SHANG_XIA_GUI == _game_type_index) {
			return analyse_chi_hu_card_sg(cards_index, weaveItems, weave_count, cur_card, chiHuRight, card_type);
		} else if (is_mj_type(GameConstants.GAME_TYPE_ZHUZHOU)) {
			return analyse_chi_hu_card_zhuzhou(cards_index, weaveItems, weave_count, cur_card, chiHuRight, card_type);
		} else if (is_mj_type(GameConstants.GAME_TYPE_XTHH)) {
			return analyse_chi_hu_card_xthh(cards_index, weaveItems, weave_count, cur_card, chiHuRight, card_type,
					_seat_index);
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_AY)) {
			// return analyse_chi_hu_card_ay(cards_index, weaveItems,
			// weave_count, cur_card, chiHuRight,card_type);

			return 0;
		} else if (GameConstants.GAME_TYPE_FLS_LX_CG == _game_type_index) {
			return analyse_chi_hu_card_lxcg(cards_index, weaveItems, weave_count, cur_card, chiHuRight, card_type);
		} else if (GameConstants.GAME_TYPE_FLS_CS_LX == _game_type_index
				|| GameConstants.GAME_TYPE_FLS_CS_LX_DT == _game_type_index) {
			// 加入新算法
			/*
			 * if (SysParamServerUtil.is_new_algorithm(3000, 3000, 1)) { return
			 * analyse_chi_hu_card_cs_lx_new(cards_index, weaveItems,
			 * weave_count, cur_card, chiHuRight, card_type); } else {
			 */
			return analyse_chi_hu_card_cs_lx_old(cards_index, weaveItems, weave_count, cur_card, chiHuRight, card_type);
			// }
		} else if (GameConstants.GAME_TYPE_HENAN_ZHOU_KOU == _game_type_index
				|| GameConstants.GAME_TYPE_NEW_ZHOU_KOU == _game_type_index) {
			return analyse_chi_hu_card_he_nan_zhou_kou(cards_index, weaveItems, weave_count, cur_card, chiHuRight,
					card_type, _seat_index);
		} else if (GameConstants.GAME_TYPE_HENAN_LH == _game_type_index
				|| GameConstants.GAME_TYPE_NEW_LUO_HE == _game_type_index) {
			return analyse_chi_hu_card_henan_lh(cards_index, weaveItems, weave_count, cur_card, chiHuRight, card_type);
		}

		return 0;
	}

	public int analyse_chi_hu_card_he_nan_zhou_kou(int cards_index[], WeaveItem weaveItems[], int weaveCount,
			int cur_card, ChiHuRight chiHuRight, int card_type, int seat_index) {
		if ((has_rule(GameConstants.GAME_RULE_HENAN_ZIMOHU) && (card_type == GameConstants.HU_CARD_TYPE_PAOHU))) {
			return GameConstants.WIK_NULL;
		}

		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		// 变量定义
		int cbChiHuKind = GameConstants.WIK_NULL;

		// 出风报听、缺门、七小对往前面放
		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HENAN_CHU_FENG_BAO_TING)
				|| GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HENAN_BAO_TING)) {
			if (this._playerStatus[seat_index].is_bao_ting() == false) { // 得用传进来的seat_index进行报听判断，用_seat_index，再点炮时是错的
				return GameConstants.WIK_NULL;
			}
		}

		// 构造扑克
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入扑克
		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		// 缺门
		if (has_rule(GameConstants.GAME_RULE_HENAN_QUE_MEN)) {
			if (!_logic.is_que_yi_se(cbCardIndexTemp, weaveItems, weaveCount)) {
				chiHuRight.set_empty();
				return GameConstants.WIK_NULL;
			}
		}

		int magic_count = _logic.magic_count(cbCardIndexTemp);
		magic_count %= 2;

		// 七小对
		boolean isQiXiaoDui = false;

		if (magic_count == 0) {
			if (_logic.is_qi_xiao_dui_henan(cbCardIndexTemp, weaveItems, weaveCount) != GameConstants.WIK_NULL) {
				isQiXiaoDui = true;
				chiHuRight.opr_or(GameConstants.CHR_HENAN_QI_XIAO_DUI);
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
					chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
				} else {
					chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
				}
				cbChiHuKind = GameConstants.WIK_CHI_HU;

				if (_logic.is_qing_yi_se(cards_index, weaveItems, weaveCount, cur_card)) { // 清一色七小对，因为有可能在bValue判断之前就已经返回了
					chiHuRight.opr_or(GameConstants.CHR_HENAN_ZHOU_KOU_QING_YI_SE);
				}
			}
		}

		// 红中胡
		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HENAN_HONG_ZHONG_LAI_ZI)) {
			if ((cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD)] == 4)
					|| ((cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD)] == 3)
							&& (cur_card == GameConstants.HZ_MAGIC_CARD))) {
				cbChiHuKind = GameConstants.WIK_CHI_HU;
				if (_logic.is_qing_yi_se(cards_index, weaveItems, weaveCount, cur_card)) { // 红中胡时也要判断是不是清一色，注意这里，代码的后部分也要做清一色，杠开的判断
					chiHuRight.opr_or(GameConstants.CHR_HENAN_ZHOU_KOU_QING_YI_SE);
				}
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
					chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
				} else if (card_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
					chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
					chiHuRight.opr_or(GameConstants.CHR_HENAN_GANG_KAI);
				} else {
					chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
				}
			}
		}

		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
		boolean bValue = _logic.analyse_card_henan_zhou_kou(cbCardIndexTemp, weaveItems, weaveCount, analyseItemArray,
				true); // 这里的分析胡牌只能分析一部分的，所以要尽量往后靠

		// 258将
		if (has_rule(GameConstants.GAME_RULE_HENAN_258) && !isQiXiaoDui) {
			boolean hu = false;
			// 胡牌分析 有没有258
			for (int i = 0; i < analyseItemArray.size(); i++) {
				// 变量定义
				AnalyseItem pAnalyseItem = analyseItemArray.get(i);
				if (pAnalyseItem.bMagicEye) { // 如果红中癞子是牌眼
					if (_logic.is_magic_card(pAnalyseItem.cbCardEye)) { // 如果将牌是红中癞子
						hu = true;
						break;
					}
				}
				int color = _logic.get_card_color(pAnalyseItem.cbCardEye);
				if (color > 2)
					continue;
				int cbCardValue = _logic.get_card_value(pAnalyseItem.cbCardEye);
				if (cbCardValue == 2 || cbCardValue == 5 || cbCardValue == 8) {
					hu = true;
					break;
				}
			}
			if (!hu) {
				chiHuRight.set_empty();
				return GameConstants.WIK_NULL;
			}
		}

		if (chiHuRight.is_empty() == false) {
			return cbChiHuKind;
		} // 如果，七小对、红中胡，后面的就不用判断了，而七小对和杠开又是互相冲突的

		if (!bValue) { // 不能胡七小对、也没有红中胡、也胡不了其他类型的、258将、缺门等也满足不了的
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		// 清一色、杠开往后面放，因为如果不能胡牌，清一色、杠开就是没意义的
		// 这里相当于，你胡牌了，但是胡的不是前面的红中胡、七小对，然后要判断是不是清一色和杠上开花
		if (_logic.is_qing_yi_se(cards_index, weaveItems, weaveCount, cur_card)) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_ZHOU_KOU_QING_YI_SE);
		}

		// 和下面重复了，注释掉
		/**
		 * if (card_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
		 * chiHuRight.opr_or(GameConstants.CHR_HENAN_GANG_KAI); }
		 **/

		cbChiHuKind = GameConstants.WIK_CHI_HU;

		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else if (card_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
			chiHuRight.opr_or(GameConstants.CHR_HENAN_GANG_KAI);
		} else {
			chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
		}

		return cbChiHuKind;
	}

	public int analyse_chi_hu_card_he_nan_zhou_kou_new(int cards_index[], WeaveItem weaveItems[], int weaveCount,
			int cur_card, ChiHuRight chiHuRight, int card_type, int seat_index) {
		if ((has_rule(GameConstants.GAME_RULE_HENAN_ZIMOHU) && (card_type == GameConstants.HU_CARD_TYPE_PAOHU))) {
			return GameConstants.WIK_NULL;
		}

		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		int cbChiHuKind = GameConstants.WIK_NULL;

		// 出风报听、缺门、七小对往前面放
		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HENAN_CHU_FENG_BAO_TING)
				|| GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HENAN_BAO_TING)) {
			if (this._playerStatus[seat_index].is_bao_ting() == false) { // 得用传进来的seat_index进行报听判断，用_seat_index，再点炮时是错的
				return GameConstants.WIK_NULL;
			}
		}

		// 构造扑克
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入扑克
		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		// 缺门
		if (has_rule(GameConstants.GAME_RULE_HENAN_QUE_MEN)) {
			if (!_logic.is_que_yi_se(cbCardIndexTemp, weaveItems, weaveCount)) {
				chiHuRight.set_empty();
				return GameConstants.WIK_NULL;
			}
		}

		int magic_count = _logic.magic_count(cbCardIndexTemp);
		magic_count %= 2;

		// 七小对
		boolean isQiXiaoDui = false;

		if (magic_count == 0) {
			if (_logic.is_qi_xiao_dui_henan(cbCardIndexTemp, weaveItems, weaveCount) != GameConstants.WIK_NULL) {
				isQiXiaoDui = true;
				chiHuRight.opr_or(GameConstants.CHR_HENAN_QI_XIAO_DUI);
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
					chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
				} else {
					chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
				}
				cbChiHuKind = GameConstants.WIK_CHI_HU;

				if (_logic.is_qing_yi_se(cards_index, weaveItems, weaveCount, cur_card)) { // 清一色七小对，因为有可能在bValue判断之前就已经返回了
					chiHuRight.opr_or(GameConstants.CHR_HENAN_ZHOU_KOU_QING_YI_SE);
				}
			}
		}

		// 红中胡
		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HENAN_HONG_ZHONG_LAI_ZI)) {
			if ((cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD)] == 4)
					|| ((cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD)] == 3)
							&& (cur_card == GameConstants.HZ_MAGIC_CARD))) {
				cbChiHuKind = GameConstants.WIK_CHI_HU;
				if (_logic.is_qing_yi_se(cards_index, weaveItems, weaveCount, cur_card)) { // 红中胡时也要判断是不是清一色，注意这里，代码的后部分也要做清一色，杠开的判断
					chiHuRight.opr_or(GameConstants.CHR_HENAN_ZHOU_KOU_QING_YI_SE);
				}
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
					chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
				} else if (card_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
					chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
					chiHuRight.opr_or(GameConstants.CHR_HENAN_GANG_KAI);
				} else {
					chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
				}
			}
		}

		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_card_count = _logic.get_magic_card_count();

		if (magic_card_count > 2) { // 一般只有两种癞子牌存在
			magic_card_count = 2;
		}

		for (int i = 0; i < magic_card_count; i++) {
			magic_cards_index[i] = _logic.get_magic_card_index(i);
		}

		boolean can_win = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
				magic_cards_index, magic_card_count);

		// 258将
		if (has_rule(GameConstants.GAME_RULE_HENAN_258) && !isQiXiaoDui) {
			boolean hu = AnalyseCardUtil.analyse_258_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
					magic_cards_index, magic_card_count);
			if (!hu) {
				chiHuRight.set_empty();
				return GameConstants.WIK_NULL;
			}
		}

		if (chiHuRight.is_empty() == false) {
			return cbChiHuKind;
		} // 如果，七小对、红中胡，后面的就不用判断了，而七小对和杠开又是互相冲突的

		if (!can_win) { // 不能胡七小对、也没有红中胡、也胡不了其他类型的、258将、缺门等也满足不了的
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		// 清一色、杠开往后面放，因为如果不能胡牌，清一色、杠开就是没意义的
		// 这里相当于，你胡牌了，但是胡的不是前面的红中胡、七小对，然后要判断是不是清一色和杠上开花
		if (_logic.is_qing_yi_se(cards_index, weaveItems, weaveCount, cur_card)) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_ZHOU_KOU_QING_YI_SE);
		}

		// 和下面重复了，注释掉
		/**
		 * if (card_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
		 * chiHuRight.opr_or(GameConstants.CHR_HENAN_GANG_KAI); }
		 **/

		cbChiHuKind = GameConstants.WIK_CHI_HU;

		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else if (card_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
			chiHuRight.opr_or(GameConstants.CHR_HENAN_GANG_KAI);
		} else {
			chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
		}

		return cbChiHuKind;
	}

	public int analyse_chi_hu_card_chu_feng_bao_ting(int cards_index[], WeaveItem weaveItems[], int weaveCount,
			int cur_card, ChiHuRight chiHuRight, int card_type) {
		if ((has_rule(GameConstants.GAME_RULE_HENAN_ZIMOHU) && (card_type == GameConstants.HU_CARD_TYPE_PAOHU))) {
			return GameConstants.WIK_NULL;
		}

		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		// 变量定义
		int cbChiHuKind = GameConstants.WIK_NULL;

		// 出风报听时检测听牌是不要判断是否已报听的
		/**
		 * if (GameDescUtil.has_rule(gameRuleIndexEx,
		 * GameConstants.GAME_RULE_HENAN_CHU_FENG_BAO_TING)) { if
		 * (this._playerStatus[_current_player].is_bao_ting() == false) { return
		 * GameConstants.WIK_NULL; } }
		 **/

		// 构造扑克
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入扑克
		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		// 缺门
		if (has_rule(GameConstants.GAME_RULE_HENAN_QUE_MEN)) {
			if (!_logic.is_que_yi_se(cbCardIndexTemp, weaveItems, weaveCount)) {
				chiHuRight.set_empty();
				return GameConstants.WIK_NULL;
			}
		}

		int magic_count = _logic.magic_count(cbCardIndexTemp);
		magic_count %= 2;

		// 七小对
		boolean isQiXiaoDui = false;

		if (magic_count == 0) {
			if (_logic.is_qi_xiao_dui_henan(cbCardIndexTemp, weaveItems, weaveCount) != GameConstants.WIK_NULL) {
				isQiXiaoDui = true;
				chiHuRight.opr_or(GameConstants.CHR_HENAN_QI_XIAO_DUI);
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
					chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
				} else {
					chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
				}
				cbChiHuKind = GameConstants.WIK_CHI_HU;

				if (_logic.is_qing_yi_se(cards_index, weaveItems, weaveCount, cur_card)) { // 清一色七小对，因为有可能在bValue判断之前就已经返回了
					chiHuRight.opr_or(GameConstants.CHR_HENAN_ZHOU_KOU_QING_YI_SE);
				}
			}
		}

		// 红中胡
		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HENAN_HONG_ZHONG_LAI_ZI)) {
			if ((cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD)] == 4)
					|| ((cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD)] == 3)
							&& (cur_card == GameConstants.HZ_MAGIC_CARD))) {
				cbChiHuKind = GameConstants.WIK_CHI_HU;
				if (_logic.is_qing_yi_se(cards_index, weaveItems, weaveCount, cur_card)) { // 红中胡时也要判断是不是清一色，注意这里，代码的后部分也要做清一色，杠开的判断
					chiHuRight.opr_or(GameConstants.CHR_HENAN_ZHOU_KOU_QING_YI_SE);
				}
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
					chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
				} else if (card_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
					chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
					chiHuRight.opr_or(GameConstants.CHR_HENAN_GANG_KAI);
				} else {
					chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
				}
			}
		}

		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
		boolean bValue = _logic.analyse_card_henan_zhou_kou(cbCardIndexTemp, weaveItems, weaveCount, analyseItemArray,
				true); // 这里的分析胡牌只能分析一部分的，所以要尽量往后靠

		// 258将
		if (has_rule(GameConstants.GAME_RULE_HENAN_258) && !isQiXiaoDui) {
			boolean hu = false;
			// 胡牌分析 有没有258
			for (int i = 0; i < analyseItemArray.size(); i++) {
				// 变量定义
				AnalyseItem pAnalyseItem = analyseItemArray.get(i);
				if (pAnalyseItem.bMagicEye) { // 如果红中癞子是牌眼
					if (_logic.is_magic_card(pAnalyseItem.cbCardEye)) { // 如果将牌是红中癞子
						hu = true;
						break;
					}
				}
				int color = _logic.get_card_color(pAnalyseItem.cbCardEye);
				if (color > 2)
					continue;
				int cbCardValue = _logic.get_card_value(pAnalyseItem.cbCardEye);
				if (cbCardValue == 2 || cbCardValue == 5 || cbCardValue == 8) {
					hu = true;
					break;
				}
			}
			if (!hu) {
				chiHuRight.set_empty();
				return GameConstants.WIK_NULL;
			}
		}

		if (chiHuRight.is_empty() == false) {
			return cbChiHuKind;
		} // 如果，七小对、红中胡，后面的就不用判断了，而七小对和杠开又是互相冲突的

		if (!bValue) { // 不能胡七小对、也没有红中胡、也胡不了其他类型的、258将、缺门等也满足不了的
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		// 清一色、杠开往后面放，因为如果不能胡牌，清一色、杠开就是没意义的
		// 这里相当于，你胡牌了，但是胡的不是前面的红中胡、七小对，然后要判断是不是清一色和杠上开花
		if (_logic.is_qing_yi_se(cards_index, weaveItems, weaveCount, cur_card)) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_ZHOU_KOU_QING_YI_SE);
		}

		// 和下面重复了，注释掉
		/**
		 * if (card_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
		 * chiHuRight.opr_or(GameConstants.CHR_HENAN_GANG_KAI); }
		 **/

		cbChiHuKind = GameConstants.WIK_CHI_HU;

		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else if (card_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
			chiHuRight.opr_or(GameConstants.CHR_HENAN_GANG_KAI);
		} else {
			chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
		}

		return cbChiHuKind;
	}

	public int analyse_chi_hu_card_chu_feng_bao_ting_new(int cards_index[], WeaveItem weaveItems[], int weaveCount,
			int cur_card, ChiHuRight chiHuRight, int card_type) {
		if ((has_rule(GameConstants.GAME_RULE_HENAN_ZIMOHU) && (card_type == GameConstants.HU_CARD_TYPE_PAOHU))) {
			return GameConstants.WIK_NULL;
		}

		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		// 变量定义
		int cbChiHuKind = GameConstants.WIK_NULL;

		// 出风报听时检测听牌是不要判断是否已报听的
		/**
		 * if (GameDescUtil.has_rule(gameRuleIndexEx,
		 * GameConstants.GAME_RULE_HENAN_CHU_FENG_BAO_TING)) { if
		 * (this._playerStatus[_current_player].is_bao_ting() == false) { return
		 * GameConstants.WIK_NULL; } }
		 **/

		// 构造扑克
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入扑克
		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		// 缺门
		if (has_rule(GameConstants.GAME_RULE_HENAN_QUE_MEN)) {
			if (!_logic.is_que_yi_se(cbCardIndexTemp, weaveItems, weaveCount)) {
				chiHuRight.set_empty();
				return GameConstants.WIK_NULL;
			}
		}

		int magic_count = _logic.magic_count(cbCardIndexTemp);
		magic_count %= 2;

		// 七小对
		boolean isQiXiaoDui = false;

		if (magic_count == 0) {
			if (_logic.is_qi_xiao_dui_henan(cbCardIndexTemp, weaveItems, weaveCount) != GameConstants.WIK_NULL) {
				isQiXiaoDui = true;
				chiHuRight.opr_or(GameConstants.CHR_HENAN_QI_XIAO_DUI);
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
					chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
				} else {
					chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
				}
				cbChiHuKind = GameConstants.WIK_CHI_HU;

				if (_logic.is_qing_yi_se(cards_index, weaveItems, weaveCount, cur_card)) { // 清一色七小对，因为有可能在bValue判断之前就已经返回了
					chiHuRight.opr_or(GameConstants.CHR_HENAN_ZHOU_KOU_QING_YI_SE);
				}
			}
		}

		// 红中胡
		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HENAN_HONG_ZHONG_LAI_ZI)) {
			if ((cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD)] == 4)
					|| ((cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD)] == 3)
							&& (cur_card == GameConstants.HZ_MAGIC_CARD))) {
				cbChiHuKind = GameConstants.WIK_CHI_HU;
				if (_logic.is_qing_yi_se(cards_index, weaveItems, weaveCount, cur_card)) { // 红中胡时也要判断是不是清一色，注意这里，代码的后部分也要做清一色，杠开的判断
					chiHuRight.opr_or(GameConstants.CHR_HENAN_ZHOU_KOU_QING_YI_SE);
				}
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
					chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
				} else if (card_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
					chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
					chiHuRight.opr_or(GameConstants.CHR_HENAN_GANG_KAI);
				} else {
					chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
				}
			}
		}

		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_card_count = _logic.get_magic_card_count();

		if (magic_card_count > 2) { // 一般只有两种癞子牌存在
			magic_card_count = 2;
		}

		for (int i = 0; i < magic_card_count; i++) {
			magic_cards_index[i] = _logic.get_magic_card_index(i);
		}

		boolean can_win = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
				magic_cards_index, magic_card_count);

		// 258将
		if (has_rule(GameConstants.GAME_RULE_HENAN_258) && !isQiXiaoDui) {
			boolean hu = AnalyseCardUtil.analyse_258_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
					magic_cards_index, magic_card_count);
			if (!hu) {
				chiHuRight.set_empty();
				return GameConstants.WIK_NULL;
			}
		}

		if (chiHuRight.is_empty() == false) {
			return cbChiHuKind;
		} // 如果，七小对、红中胡，后面的就不用判断了，而七小对和杠开又是互相冲突的

		if (!can_win) { // 不能胡七小对、也没有红中胡、也胡不了其他类型的、258将、缺门等也满足不了的
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		// 清一色、杠开往后面放，因为如果不能胡牌，清一色、杠开就是没意义的
		// 这里相当于，你胡牌了，但是胡的不是前面的红中胡、七小对，然后要判断是不是清一色和杠上开花
		if (_logic.is_qing_yi_se(cards_index, weaveItems, weaveCount, cur_card)) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_ZHOU_KOU_QING_YI_SE);
		}

		// 和下面重复了，注释掉
		/**
		 * if (card_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
		 * chiHuRight.opr_or(GameConstants.CHR_HENAN_GANG_KAI); }
		 **/

		cbChiHuKind = GameConstants.WIK_CHI_HU;

		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else if (card_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
			chiHuRight.opr_or(GameConstants.CHR_HENAN_GANG_KAI);
		} else {
			chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
		}

		return cbChiHuKind;
	}

	/***
	 * 三门峡麻将胡牌解析
	 * 
	 * @param cards_index
	 * @param weaveItems
	 * @param weaveCount
	 * @param cur_card
	 * @param chiHuRight
	 * @param card_type
	 * @return
	 */
	public int analyse_chi_hu_card_henan_smx(int cards_index[], WeaveItem weaveItems[], int weaveCount, int cur_card,
			ChiHuRight chiHuRight, int card_type) {
		if (card_type == GameConstants.HU_CARD_TYPE_PAOHU)// 如果胡牌类型是炮胡
															// 但没这个规则
															// return
		{
			return GameConstants.WIK_NULL;

		}

		// cbCurrentCard一定不为0 !!!!!!!!!
		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		// 变量定义
		int cbChiHuKind = GameConstants.WIK_NULL;
		// 构造扑克
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入扑克
		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		int igc_count = _logic.magic_count(cbCardIndexTemp);

		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
		// 分析扑克
		boolean bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weaveCount, analyseItemArray,
				has_rule(GameConstants.GAME_RULE_HENAN_DAIFENG));
		if (!bValue) {

			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;
		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else if (card_type == GameConstants.HU_CARD_TYPE_PAOHU) {
			chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
		} else if (card_type == GameConstants.HU_CARD_TYPE_QIANGGANG) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_QIANG_GANG_HU);
		} else if (card_type == GameConstants.HU_CARD_TYPE_TIAN_HU) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_TIAN_HU);
		} else if (card_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_GANG_KAI);
		}

		return cbChiHuKind;
	}

	// 三门峡麻将进入表演判断
	public boolean isonbiaoyan(int cards_index[], WeaveItem weaveItems[], int weaveCount, int cur_card) {
		// 表演判断
		int cbCardIndexTemp[] = new int[GameConstants.MAX_COUNT];
		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			cbCardIndexTemp[i] = GRR._cards_index[_out_card_player][i];
		}
		int igc_count = _logic.magic_count(cbCardIndexTemp);
		if (igc_count == 0) {
			return false;
		}
		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
		// 分析扑克
		boolean bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weaveCount, analyseItemArray,
				has_rule(GameConstants.GAME_RULE_HENAN_DAIFENG));
		if (bValue) {
			for (int i = 0; i < analyseItemArray.size(); i++) {
				AnalyseItem analyseitem = analyseItemArray.get(i);
				if (analyseitem.bMagicEye) {
					if (cur_card == analyseitem.cbCardEye
							|| cur_card == _logic.switch_to_card_data(_logic.get_magic_card_index(0))) {
						return true;
					}
				}
			}
		}

		return false;
	}

	// 三门峡麻将表演2,3,4判断
	public boolean isbiaoyancontinue(int cards_index[], WeaveItem weaveItems[], int weaveCount, int cur_card) {
		// 表演判断
		int cbCardIndexTemp[] = new int[GameConstants.MAX_COUNT];
		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			cbCardIndexTemp[i] = GRR._cards_index[_out_card_player][i];
		}
		int igc_count = _logic.magic_count(cbCardIndexTemp);
		if (igc_count < 1) {
			return false;
		}
		if (cur_card != _logic.switch_to_card_data(_logic.get_magic_card_index(0))) {
			return false;
		}
		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
		// 分析扑克
		boolean bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weaveCount, analyseItemArray,
				has_rule(GameConstants.GAME_RULE_HENAN_DAIFENG));
		if (bValue) {
			for (int i = 0; i < analyseItemArray.size(); i++) {
				AnalyseItem analyseitem = analyseItemArray.get(i);
				if (analyseitem.bMagicEye) {
					if (cur_card == analyseitem.cbCardEye
							&& analyseitem.cbCardEye == _logic.switch_to_card_data(_logic.get_magic_card_index(0))) {
						return true;
					}
				}
			}
		}

		return false;
	}

	/***
	 * 河南麻将胡牌解析
	 * 
	 * @param cards_index
	 * @param weaveItems
	 * @param weaveCount
	 * @param cur_card
	 * @param chiHuRight
	 * @param card_type
	 * @return
	 */
	public int analyse_chi_hu_card_henan(int cards_index[], WeaveItem weaveItems[], int weaveCount, int cur_card,
			ChiHuRight chiHuRight, int card_type) {
		if ((!has_rule(GameConstants.GAME_RULE_HENAN_HENAN_PAO_HU) && (card_type == GameConstants.HU_CARD_TYPE_PAOHU)))// 如果胡牌类型是炮胡
																														// 但没这个规则
																														// return
		{
			return GameConstants.WIK_NULL;

		}

		// cbCurrentCard一定不为0 !!!!!!!!!
		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		// 变量定义
		int cbChiHuKind = GameConstants.WIK_NULL;
		// 构造扑克
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入扑克
		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		int igc_count = _logic.magic_count(cbCardIndexTemp);
		igc_count %= 2;

		if (igc_count == 0) {
			long qxd = _logic.is_qi_xiao_dui_henan(cbCardIndexTemp, weaveItems, weaveCount);
			if (qxd != GameConstants.WIK_NULL) {
				cbChiHuKind = GameConstants.WIK_CHI_HU;
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
					chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
				} else {
					chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
				}
				chiHuRight.opr_or(qxd);// 都是七小对
			}
		}

		if (has_rule(GameConstants.GAME_RULE_HENAN_QUE_MEN)) {
			if (!_logic.is_que_yi_se(cbCardIndexTemp, weaveItems, weaveCount)) {
				chiHuRight.set_empty();
				return GameConstants.WIK_NULL;
			}
		}

		if (has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN)) {// 带混 4混
			if ((cards_index[_logic.get_magic_card_index(0)] == 4)
					|| ((cards_index[_logic.get_magic_card_index(0)] == 3) && (_logic.is_magic_card(cur_card))
							&& (card_type == GameConstants.HU_CARD_TYPE_ZIMO
									|| card_type == GameConstants.HU_CARD_TYPE_GANG_KAI))) {
				cbChiHuKind = GameConstants.WIK_CHI_HU;
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
					chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
					chiHuRight.opr_or(GameConstants.CHR_HENAN_QISHOU_HU);
				} else {
					// 这个没必要。一定是自摸
					chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
					chiHuRight.opr_or(GameConstants.CHR_HENAN_QISHOU_HU);
				}
			}
		}

		if (chiHuRight.is_empty() == false) {
			if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
				chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
			} else if (card_type == GameConstants.HU_CARD_TYPE_PAOHU) {
				chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
			} else if (card_type == GameConstants.HU_CARD_TYPE_QIANGGANG) {
				chiHuRight.opr_or(GameConstants.CHR_HENAN_QIANG_GANG_HU);
			} else if (card_type == GameConstants.HU_CARD_TYPE_TIAN_HU) {
				chiHuRight.opr_or(GameConstants.CHR_HENAN_TIAN_HU);
			} else if (card_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
				chiHuRight.opr_or(GameConstants.CHR_HENAN_GANG_KAI);
			}
			return cbChiHuKind;
		}

		if (has_rule(GameConstants.GAME_RULE_HENAN_HEI_ZI) && (is_mj_type(GameConstants.GAME_TYPE_HENAN_PDS)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_PING_DING_SHAN))) {
			if (_logic.get_se_count(cbCardIndexTemp, weaveItems, weaveCount) <= 1) {
				chiHuRight.opr_or(GameConstants.CHR_HENAN_HEI_ZI);
			}
		}

		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
		// 分析扑克
		boolean bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weaveCount, analyseItemArray,
				has_rule(GameConstants.GAME_RULE_HENAN_DAIFENG));
		if (!bValue) {

			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		/**
		 * // 如果是接炮胡，先把癞子牌，删掉，如果玩家还是能胡牌，那就是能接炮的 boolean can_jie_pao = true;
		 * 
		 * if ((card_type == GameConstants.HU_CARD_TYPE_PAOHU) &&
		 * (_logic.is_magic_card(cur_card))) { int tmp_magic_count =
		 * _logic.get_magic_card_count(); int[] tmp_magic_card_index = new
		 * int[tmp_magic_count]; for (int m = 0; m < tmp_magic_count; m++) {
		 * tmp_magic_card_index[m] = _logic.get_magic_card_index(m); }
		 * _logic.clean_magic_cards();
		 * 
		 * boolean tmp_value = _logic.analyse_card(cbCardIndexTemp, weaveItems,
		 * weaveCount, analyseItemArray,
		 * has_rule(GameConstants.GAME_RULE_HENAN_DAIFENG));
		 * 
		 * for (int m = 0; m < tmp_magic_count; m++) {
		 * _logic.add_magic_card_index(tmp_magic_card_index[m]); }
		 * 
		 * if (tmp_value == false) can_jie_pao = false; } else { boolean
		 * tmp_value = _logic.analyse_card(cbCardIndexTemp, weaveItems,
		 * weaveCount, analyseItemArray,
		 * has_rule(GameConstants.GAME_RULE_HENAN_DAIFENG)); if (tmp_value ==
		 * false) { chiHuRight.set_empty(); return GameConstants.WIK_NULL; } }
		 * 
		 * if (can_jie_pao == false) { chiHuRight.set_empty(); return
		 * GameConstants.WIK_NULL; }
		 **/

		boolean cheng_ju = true;
		if (_logic.is_magic_card(cur_card) && (card_type == GameConstants.HU_CARD_TYPE_PAOHU)) {

			// 胡牌分析
			// 牌型分析 现在没有这个选项
			for (int i = 0; i < analyseItemArray.size(); i++) {
				cheng_ju = false;

				// 变量定义
				AnalyseItem pAnalyseItem = analyseItemArray.get(i);
				if ((pAnalyseItem.bMagicEye == true) && (pAnalyseItem.cbCardEye == cur_card)) {
					cheng_ju = true;
					break;
				}

				for (int j = 0; j < 4; j++) {
					if ((pAnalyseItem.cbCardData[j][0] == cur_card) || (pAnalyseItem.cbCardData[j][1] == cur_card)
							|| (pAnalyseItem.cbCardData[j][2] == cur_card)) {
						if (pAnalyseItem.cbWeaveKind[j] == GameConstants.WIK_PENG) {
							if ((pAnalyseItem.cbCardData[j][0] == pAnalyseItem.cbCardData[j][1])
									&& (pAnalyseItem.cbCardData[j][1] == pAnalyseItem.cbCardData[j][2])
									&& (pAnalyseItem.cbCardData[j][2] == pAnalyseItem.cbCardData[j][0])) {
								cheng_ju = true;
							}

						} else if (pAnalyseItem.cbWeaveKind[j] == GameConstants.WIK_LEFT) {
							if ((pAnalyseItem.cbCardData[j][0] == pAnalyseItem.cbCardData[j][1] - 1)
									&& (pAnalyseItem.cbCardData[j][1] == pAnalyseItem.cbCardData[j][2] - 1)
									&& (pAnalyseItem.cbCardData[j][2] == pAnalyseItem.cbCardData[j][0] + 2)) {
								cheng_ju = true;
							}
						}

					}

					if (cheng_ju == true) {
						break;// 癞子成句
					}
				}

				if (cheng_ju == true) {
					break;// 癞子成句
				}
			}
		}

		if (cheng_ju == false) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		if (has_rule(GameConstants.GAME_RULE_HENAN_258) && !has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN)) {
			boolean hu = false;
			// 胡牌分析 有没有258
			for (int i = 0; i < analyseItemArray.size(); i++) {
				// 变量定义
				AnalyseItem pAnalyseItem = analyseItemArray.get(i);
				int color = _logic.get_card_color(pAnalyseItem.cbCardEye);
				if (color > 2)
					continue;
				int cbCardValue = _logic.get_card_value(pAnalyseItem.cbCardEye);
				if (cbCardValue != 2 && cbCardValue != 5 && cbCardValue != 8) {
					continue;
				}
				hu = true;
			}
			if (!hu) {
				chiHuRight.set_empty();
				return GameConstants.WIK_NULL;
			}

		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;
		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else if (card_type == GameConstants.HU_CARD_TYPE_PAOHU) {
			chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
		} else if (card_type == GameConstants.HU_CARD_TYPE_QIANGGANG) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_QIANG_GANG_HU);
		} else if (card_type == GameConstants.HU_CARD_TYPE_TIAN_HU) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_TIAN_HU);
		} else if (card_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_GANG_KAI);
		}

		return cbChiHuKind;
	}

	public int analyse_chi_hu_card_henan_lygc_new(int cards_index[], WeaveItem weaveItems[], int weaveCount,
			int cur_card, ChiHuRight chiHuRight, int card_type) {
		if ((!has_rule(GameConstants.GAME_RULE_HENAN_HENAN_PAO_HU) && (card_type == GameConstants.HU_CARD_TYPE_PAOHU)))
			return GameConstants.WIK_NULL;

		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_card_count = _logic.get_magic_card_count();

		if (magic_card_count > 2) { // 一般只有两种癞子牌存在
			magic_card_count = 2;
		}

		for (int i = 0; i < magic_card_count; i++) {
			magic_cards_index[i] = _logic.get_magic_card_index(i);
		}

		boolean bValue = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
				magic_cards_index, magic_card_count);

		if (!bValue) {
			if (has_rule(GameConstants.GAME_RULE_HENAN_QIDUI_DOUBLE)) {
				// 如果勾了‘可胡七对’
				int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
				for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
					cbCardIndexTemp[i] = cards_index[i];
				}

				if (cur_card != GameConstants.INVALID_VALUE) {
					cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
				}

				bValue = _logic.is_qi_xiao_dui_henan(cbCardIndexTemp, weaveItems, weaveCount) != 0;
			}

			if (!bValue) {
				chiHuRight.set_empty();
				return GameConstants.WIK_NULL;
			}
		}

		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else if (card_type == GameConstants.HU_CARD_TYPE_PAOHU) {
			chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
		} else if (card_type == GameConstants.HU_CARD_TYPE_QIANGGANG) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_QIANG_GANG_HU);
		} else if (card_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_GANG_KAI);
		}

		return GameConstants.WIK_CHI_HU;
	}

	/***
	 * 漯河麻将胡牌解析
	 * 
	 * @param cards_index
	 * @param weaveItems
	 * @param weaveCount
	 * @param cur_card
	 * @param chiHuRight
	 * @param card_type
	 * @return
	 */
	public int analyse_chi_hu_card_henan_lh(int cards_index[], WeaveItem weaveItems[], int weaveCount, int cur_card,
			ChiHuRight chiHuRight, int card_type) {
		if ((!has_rule(GameConstants.GAME_RULE_HENAN_HENAN_PAO_HU)
				&& (card_type == GameConstants.HU_CARD_TYPE_PAOHU))) {
			return GameConstants.WIK_NULL;
		}

		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		// 变量定义
		int cbChiHuKind = GameConstants.WIK_NULL;
		// 构造扑克
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入扑克
		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		if (!_logic.is_que_yi_se(cbCardIndexTemp, weaveItems, weaveCount)) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
		boolean bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weaveCount, analyseItemArray, true);

		if (_logic.is_qi_xiao_dui_he_nan_zhou_kou(cards_index, weaveItems, weaveCount,
				cur_card) != GameConstants.WIK_NULL) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_QI_XIAO_DUI);
			if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
				chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
			} else {
				chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
			}
			cbChiHuKind = GameConstants.WIK_CHI_HU;
			bValue = true;
		}

		if (_logic.is_qing_yi_se(cards_index, weaveItems, weaveCount, cur_card)) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_ZHOU_KOU_QING_YI_SE);
		}

		if (card_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_GANG_KAI);
		}

		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HENAN_HONG_ZHONG_LAI_ZI)) {
			if ((cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD)] == 4)
					|| ((cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD)] == 3)
							&& (cur_card == GameConstants.HZ_MAGIC_CARD))) {
				cbChiHuKind = GameConstants.WIK_CHI_HU;
				chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
			}
		}

		/**
		 * if (chiHuRight.is_empty() == false) { return cbChiHuKind; }
		 **/

		if (!bValue) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;

		if (card_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
			return cbChiHuKind;
		}
		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else {
			chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
		}

		return cbChiHuKind;
	}

	public int analyse_chi_hu_card_henan_lh_new(int cards_index[], WeaveItem weaveItems[], int weaveCount, int cur_card,
			ChiHuRight chiHuRight, int card_type) {
		if ((!has_rule(GameConstants.GAME_RULE_HENAN_HENAN_PAO_HU)
				&& (card_type == GameConstants.HU_CARD_TYPE_PAOHU))) {
			return GameConstants.WIK_NULL;
		}

		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		int cbChiHuKind = GameConstants.WIK_NULL;
		// 构造扑克
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		if (!_logic.is_que_yi_se(cbCardIndexTemp, weaveItems, weaveCount)) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_card_count = _logic.get_magic_card_count();

		if (magic_card_count > 2) { // 一般只有两种癞子牌存在
			magic_card_count = 2;
		}

		for (int i = 0; i < magic_card_count; i++) {
			magic_cards_index[i] = _logic.get_magic_card_index(i);
		}

		boolean can_win = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
				magic_cards_index, magic_card_count);

		if (_logic.is_qi_xiao_dui_he_nan_zhou_kou(cards_index, weaveItems, weaveCount,
				cur_card) != GameConstants.WIK_NULL) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_QI_XIAO_DUI);
			if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
				chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
			} else {
				chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
			}
			cbChiHuKind = GameConstants.WIK_CHI_HU;
			can_win = true;
		}

		if (_logic.is_qing_yi_se(cards_index, weaveItems, weaveCount, cur_card)) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_ZHOU_KOU_QING_YI_SE);
		}

		if (card_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_GANG_KAI);
		}

		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HENAN_HONG_ZHONG_LAI_ZI)) {
			if ((cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD)] == 4)
					|| ((cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD)] == 3)
							&& (cur_card == GameConstants.HZ_MAGIC_CARD))) {
				cbChiHuKind = GameConstants.WIK_CHI_HU;
				chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
			}
		}

		if (!can_win) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;

		if (card_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
			return cbChiHuKind;
		}
		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else {
			chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
		}

		return cbChiHuKind;
	}

	/***
	 * 信阳麻将胡牌解析
	 * 
	 * @param cards_index
	 * @param weaveItems
	 * @param weaveCount
	 * @param cur_card
	 * @param chiHuRight
	 * @param card_type
	 * @return
	 */
	public int analyse_chi_hu_card_henan_xy(int cards_index[], WeaveItem weaveItems[], int weaveCount, int cur_card,
			ChiHuRight chiHuRight, int card_type) {
		if ((!has_rule(GameConstants.GAME_RULE_HENAN_HENAN_PAO_HU) && (card_type == GameConstants.HU_CARD_TYPE_PAOHU)))// 如果胡牌类型是炮胡
																														// 但没这个规则
																														// return
		{
			return GameConstants.WIK_NULL;

		}

		// cbCurrentCard一定不为0 !!!!!!!!!
		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		// 变量定义
		int cbChiHuKind = GameConstants.WIK_NULL;
		// 构造扑克
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入扑克
		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		long qxd = _logic.is_qi_xiao_dui_henan_xy(cbCardIndexTemp, weaveItems, weaveCount);
		if (qxd != GameConstants.WIK_NULL) {
			cbChiHuKind = GameConstants.WIK_CHI_HU;
			chiHuRight.opr_or(qxd);// 都是七小对
			chiHuRight.opr_or(GameConstants.CHR_HENAN_XY_DUYING);
		}

		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
		// 分析扑克
		boolean bValue = _logic.analyse_card_henanxy(cbCardIndexTemp, weaveItems, weaveCount, cur_card,
				analyseItemArray, true);
		if (!bValue && qxd == GameConstants.WIK_NULL) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}
		if (has_rule(GameConstants.GAME_RULE_HENAN_TUIDAOHU)) {
			cbChiHuKind = GameConstants.WIK_CHI_HU;
		}

		long mq = _logic.is_men_qing_henanxy(cbCardIndexTemp, weaveItems, weaveCount);
		if (mq != GameConstants.WIK_NULL) {
			chiHuRight.opr_or(mq);
			cbChiHuKind = GameConstants.WIK_CHI_HU;
		}
		long bazhang = _logic.is_bazhang_henan_xy(cbCardIndexTemp, weaveItems, weaveCount);
		if (bazhang != GameConstants.WIK_NULL) {
			chiHuRight.opr_or(bazhang);
			cbChiHuKind = GameConstants.WIK_CHI_HU;
		}
		long jiazi = _logic.is_jaizi_henan_xy(cbCardIndexTemp, cur_card, analyseItemArray);
		if (qxd == GameConstants.WIK_NULL) {
			if (jiazi != GameConstants.WIK_NULL) {
				chiHuRight.opr_or(jiazi);
				cbChiHuKind = GameConstants.WIK_CHI_HU;
			}
		}
		long duying = _logic.is_duying_henan_xy(cbCardIndexTemp, cur_card, analyseItemArray);
		if (duying != GameConstants.WIK_NULL) {
			chiHuRight.opr_or(duying);
			cbChiHuKind = GameConstants.WIK_CHI_HU;
		}
		long duanmen = _logic.is_duanmen_henan_xy(cbCardIndexTemp, weaveItems, weaveCount);
		if (duanmen != GameConstants.WIK_NULL) {

			if (duanmen == GameConstants.CHR_HENAN_XY_QINGYISE) {
				chiHuRight.opr_or(GameConstants.CHR_HENAN_XY_QINGQUE);
				if (has_rule(GameConstants.GAME_RULE_HENAN_TUIDAOHU)) {
					chiHuRight.opr_or(duanmen);
				}
			} else {
				chiHuRight.opr_or(duanmen);
			}

			cbChiHuKind = GameConstants.WIK_CHI_HU;
		}
		if (has_rule(GameConstants.GAME_RULE_HENAN_TUIDAOHU)) {
			long sanqiying = _logic.is_sanqiying_henan_xy(cur_card);
			if (sanqiying != GameConstants.WIK_NULL) {
				chiHuRight.opr_or(sanqiying);
			}
			if (qxd == GameConstants.WIK_NULL) {
				long sanqijiang = _logic.is_sanqijiang_henan_xy(analyseItemArray);
				if (sanqijiang != GameConstants.WIK_NULL) {
					chiHuRight.opr_or(sanqijiang);
				}
				long zhongwu = _logic.is_zhongwu_henan_xy(analyseItemArray);
				if (zhongwu != GameConstants.WIK_NULL) {
					chiHuRight.opr_or(zhongwu);
				}
				long lianliu = _logic.is_lianliu_henan_xy(analyseItemArray);
				if (lianliu != GameConstants.WIK_NULL) {
					chiHuRight.opr_or(lianliu);
				}

			}
		}

		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else if (card_type == GameConstants.HU_CARD_TYPE_PAOHU) {
			chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
		} else if (card_type == GameConstants.HU_CARD_TYPE_QIANGGANG) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_QIANG_GANG_HU);
		} else if (card_type == GameConstants.HU_CARD_TYPE_TIAN_HU) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_TIAN_HU);
		} else if (card_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_XY_GANG_KAI);
		}

		return cbChiHuKind;
	}

	public int analyse_chi_hu_card_henan_xy_new(int cards_index[], WeaveItem weaveItems[], int weaveCount, int cur_card,
			ChiHuRight chiHuRight, int card_type) {
		if ((!has_rule(GameConstants.GAME_RULE_HENAN_HENAN_PAO_HU) && (card_type == GameConstants.HU_CARD_TYPE_PAOHU)))
			return GameConstants.WIK_NULL;

		if (cur_card == 0)
			return GameConstants.WIK_NULL;

		int cbChiHuKind = GameConstants.WIK_NULL;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		long qxd = _logic.is_qi_xiao_dui_henan_xy(cbCardIndexTemp, weaveItems, weaveCount);
		if (qxd != GameConstants.WIK_NULL) {
			cbChiHuKind = GameConstants.WIK_CHI_HU;
			chiHuRight.opr_or(qxd);
			chiHuRight.opr_or(GameConstants.CHR_HENAN_XY_DUYING);
		}

		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_card_count = _logic.get_magic_card_count();

		if (magic_card_count > 2) { // 一般只有两种癞子牌存在
			magic_card_count = 2;
		}

		for (int i = 0; i < magic_card_count; i++) {
			magic_cards_index[i] = _logic.get_magic_card_index(i);
		}

		boolean can_win = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
				magic_cards_index, magic_card_count);

		if (!can_win && qxd == GameConstants.WIK_NULL) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		if (has_rule(GameConstants.GAME_RULE_HENAN_TUIDAOHU)) {
			cbChiHuKind = GameConstants.WIK_CHI_HU;
		}

		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else if (card_type == GameConstants.HU_CARD_TYPE_PAOHU) {
			chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
		} else if (card_type == GameConstants.HU_CARD_TYPE_QIANGGANG) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_QIANG_GANG_HU);
		} else if (card_type == GameConstants.HU_CARD_TYPE_TIAN_HU) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_TIAN_HU);
		} else if (card_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_XY_GANG_KAI);
		}

		return cbChiHuKind;
	}

	public int analyse_chi_hu_card_henan_xc(int cards_index[], WeaveItem weaveItems[], int weaveCount, int cur_card,
			ChiHuRight chiHuRight, int card_type, boolean hupai) {
		if ((!has_rule(GameConstants.GAME_RULE_HENAN_HENAN_PAO_HU)
				&& (card_type == GameConstants.HU_CARD_TYPE_PAOHU))) {
			return GameConstants.WIK_NULL;
		}

		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		int cbChiHuKind = GameConstants.WIK_NULL;
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		if (has_rule(GameConstants.GAME_RULE_HENAN_QUE_MEN)) {
			if (!_logic.is_que_yi_se(cbCardIndexTemp, weaveItems, weaveCount)) {
				chiHuRight.set_empty();
				return GameConstants.WIK_NULL;
			}
		}

		if (has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN) && xc_analyse_type != XC_ANALYSE_BAO_TING) {
			if ((cards_index[_logic.get_magic_card_index(0)] == 4)
					|| ((cards_index[_logic.get_magic_card_index(0)] == 3) && (_logic.is_magic_card(cur_card))
							&& (card_type == GameConstants.HU_CARD_TYPE_ZIMO
									|| card_type == GameConstants.HU_CARD_TYPE_GANG_KAI))) {
				cbChiHuKind = GameConstants.WIK_CHI_HU;
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
					chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
					chiHuRight.opr_or(GameConstants.CHR_HENAN_QISHOU_HU);
				} else {
					chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
					chiHuRight.opr_or(GameConstants.CHR_HENAN_QISHOU_HU);
				}
			}
		}

		if (cbChiHuKind == GameConstants.WIK_NULL && getRuleValue(GameConstants.GAME_RULE_HENAN_KE_HU_QI_DUI) == 1) {
			boolean fen_xi_qi_dui = true;
			if (has_rule(GameConstants.GAME_RULE_HENAN_QUE_MEN)) {
				if (!_logic.is_que_yi_se(cbCardIndexTemp, weaveItems, weaveCount)) {
					fen_xi_qi_dui = false;
				}
			}
			if (fen_xi_qi_dui) {
				long qxd = _logic.is_qi_xiao_dui_henan_xc(cbCardIndexTemp, weaveItems, weaveCount, cur_card);
				if (qxd != GameConstants.WIK_NULL) {
					cbChiHuKind = GameConstants.WIK_CHI_HU;
					if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
						chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
					} else {
						chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
					}
					chiHuRight.opr_or(qxd);
				}
			}
		}

		if (hupai) {
			int colorCount = this._logic.get_se_count(cbCardIndexTemp, weaveItems, weaveCount);
			chiHuRight.duanmen_count = 3 - colorCount;
		}

		if (chiHuRight.is_empty() == false) {
			if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
				chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
			} else if (card_type == GameConstants.HU_CARD_TYPE_PAOHU) {
				chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
			} else if (card_type == GameConstants.HU_CARD_TYPE_QIANGGANG) {
				chiHuRight.opr_or(GameConstants.CHR_HENAN_QIANG_GANG_HU);
			} else if (card_type == GameConstants.HU_CARD_TYPE_TIAN_HU) {
				chiHuRight.opr_or(GameConstants.CHR_HENAN_TIAN_HU);
			} else if (card_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
				chiHuRight.opr_or(GameConstants.CHR_HENAN_GANG_KAI);
			}
			return cbChiHuKind;
		}

		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();

		boolean bValue = _logic.analyse_card_feng_chi_zfb(cbCardIndexTemp, weaveItems, weaveCount, analyseItemArray,
				chiHuRight, hupai);
		if (!bValue) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}
		boolean cheng_ju = true;
		if (_logic.is_magic_card(cur_card) && (card_type == GameConstants.HU_CARD_TYPE_PAOHU)) {
			for (int i = 0; i < analyseItemArray.size(); i++) {
				cheng_ju = false;

				AnalyseItem pAnalyseItem = analyseItemArray.get(i);
				if ((pAnalyseItem.bMagicEye == true) && (pAnalyseItem.cbCardEye == cur_card)) {
					cheng_ju = true;
					break;
				}

				for (int j = 0; j < 4; j++) {
					if ((pAnalyseItem.cbCardData[j][0] == cur_card) || (pAnalyseItem.cbCardData[j][1] == cur_card)
							|| (pAnalyseItem.cbCardData[j][2] == cur_card)) {
						if (pAnalyseItem.cbWeaveKind[j] == GameConstants.WIK_PENG) {
							if ((pAnalyseItem.cbCardData[j][0] == pAnalyseItem.cbCardData[j][1])
									&& (pAnalyseItem.cbCardData[j][1] == pAnalyseItem.cbCardData[j][2])
									&& (pAnalyseItem.cbCardData[j][2] == pAnalyseItem.cbCardData[j][0])) {
								cheng_ju = true;
							}

						} else if (pAnalyseItem.cbWeaveKind[j] == GameConstants.WIK_LEFT) {
							if ((pAnalyseItem.cbCardData[j][0] == pAnalyseItem.cbCardData[j][1] - 1)
									&& (pAnalyseItem.cbCardData[j][1] == pAnalyseItem.cbCardData[j][2] - 1)
									&& (pAnalyseItem.cbCardData[j][2] == pAnalyseItem.cbCardData[j][0] + 2)) {
								cheng_ju = true;
							}
						}

					}

					if (cheng_ju == true) {
						break;
					}
				}

				if (cheng_ju == true) {
					break;
				}
			}
		}

		if (cheng_ju == false) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		if (has_rule(GameConstants.GAME_RULE_HENAN_258)) {
			boolean hu = false;

			for (int i = 0; i < analyseItemArray.size(); i++) {
				AnalyseItem pAnalyseItem = analyseItemArray.get(i);
				int cbCardValue = _logic.get_card_value(pAnalyseItem.cbCardEye);
				int color = _logic.get_card_color(pAnalyseItem.cbCardEye);
				if (color > 2)
					continue;
				if (cbCardValue != 2 && cbCardValue != 5 && cbCardValue != 8) {
					continue;
				}
				hu = true;
			}
			if (!hu) {
				chiHuRight.set_empty();
				return GameConstants.WIK_NULL;
			}

		}
		if (hupai && getRuleValue(GameConstants.GAME_RULE_HENAN_BU_DAI_FENG) != 1) {
			int maxHeiFeng = 0;
			int maxBaiFeng = 0;
			int maxFeng = 0;
			for (int i = 0; i < analyseItemArray.size(); i++) {
				AnalyseItem pAnalyseItem = analyseItemArray.get(i);
				int temphei = 0;
				int tempbai = 0;
				for (int j = 0; j < 4; j++) {
					if (pAnalyseItem.cbWeaveKind[j] == GameConstants.WIK_PENG
							&& _logic.is_magic_card(pAnalyseItem.cbCardData[j][0])) {
						chiHuRight.sanhun_kan = 1;
					}
					if (pAnalyseItem.cbWeaveKind[j] == GameConstants.WIK_LEFT
							|| pAnalyseItem.cbWeaveKind[j] == GameConstants.WIK_CENTER
							|| pAnalyseItem.cbWeaveKind[j] == GameConstants.WIK_RIGHT) {
						int curCard = pAnalyseItem.cbCardData[j][0];
						if (_logic.get_card_color(pAnalyseItem.cbCardData[j][0]) == 3
								|| _logic.get_card_color(pAnalyseItem.cbCardData[j][1]) == 3
								|| _logic.get_card_color(pAnalyseItem.cbCardData[j][2]) == 3) {
							if (_logic.switch_to_card_index(curCard) < 31) {
								temphei += 1;
							} else {
								tempbai += 1;
							}
						}
					}
				}
				if (maxFeng < (temphei + tempbai)) {
					maxFeng = temphei + tempbai;
					maxHeiFeng = temphei;
					maxBaiFeng = tempbai;
				}
			}
			chiHuRight.baifeng_count = maxBaiFeng;
			chiHuRight.heifeng_count = maxHeiFeng;
		}
		cbChiHuKind = GameConstants.WIK_CHI_HU;
		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else if (card_type == GameConstants.HU_CARD_TYPE_PAOHU) {
			chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
		} else if (card_type == GameConstants.HU_CARD_TYPE_QIANGGANG) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_QIANG_GANG_HU);
		} else if (card_type == GameConstants.HU_CARD_TYPE_TIAN_HU) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_TIAN_HU);
		} else if (card_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_GANG_KAI);
		}

		return cbChiHuKind;
	}

	public int analyse_chi_hu_card_henan_xc_new(int cards_index[], WeaveItem weaveItems[], int weaveCount, int cur_card,
			ChiHuRight chiHuRight, int card_type, boolean hupai) {
		if ((!has_rule(GameConstants.GAME_RULE_HENAN_HENAN_PAO_HU) && (card_type == GameConstants.HU_CARD_TYPE_PAOHU)))
			return GameConstants.WIK_NULL;

		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		int cbChiHuKind = GameConstants.WIK_NULL;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		if (has_rule(GameConstants.GAME_RULE_HENAN_QUE_MEN)) {
			if (!_logic.is_que_yi_se(cbCardIndexTemp, weaveItems, weaveCount)) {
				chiHuRight.set_empty();
				return GameConstants.WIK_NULL;
			}
		}

		if (has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN) && xc_analyse_type != XC_ANALYSE_BAO_TING) {
			if ((cards_index[_logic.get_magic_card_index(0)] == 4)
					|| ((cards_index[_logic.get_magic_card_index(0)] == 3) && (_logic.is_magic_card(cur_card))
							&& (card_type == GameConstants.HU_CARD_TYPE_ZIMO
									|| card_type == GameConstants.HU_CARD_TYPE_GANG_KAI))) {
				cbChiHuKind = GameConstants.WIK_CHI_HU;
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
					chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
					chiHuRight.opr_or(GameConstants.CHR_HENAN_QISHOU_HU);
				} else {
					chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
					chiHuRight.opr_or(GameConstants.CHR_HENAN_QISHOU_HU);
				}
			}
		}

		if (cbChiHuKind == GameConstants.WIK_NULL && getRuleValue(GameConstants.GAME_RULE_HENAN_KE_HU_QI_DUI) == 1) {
			boolean fen_xi_qi_dui = true;
			if (has_rule(GameConstants.GAME_RULE_HENAN_QUE_MEN)) {
				if (!_logic.is_que_yi_se(cbCardIndexTemp, weaveItems, weaveCount)) {
					fen_xi_qi_dui = false;
				}
			}
			if (fen_xi_qi_dui) {
				long qxd = _logic.is_qi_xiao_dui_henan_xc(cbCardIndexTemp, weaveItems, weaveCount, cur_card);
				if (qxd != GameConstants.WIK_NULL) {
					cbChiHuKind = GameConstants.WIK_CHI_HU;
					if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
						chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
					} else {
						chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
					}
					chiHuRight.opr_or(qxd);
				}
			}
		}

		if (hupai) {
			int colorCount = this._logic.get_se_count(cbCardIndexTemp, weaveItems, weaveCount);
			chiHuRight.duanmen_count = 3 - colorCount;
		}

		if (chiHuRight.is_empty() == false) {
			if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
				chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
			} else if (card_type == GameConstants.HU_CARD_TYPE_PAOHU) {
				chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
			} else if (card_type == GameConstants.HU_CARD_TYPE_QIANGGANG) {
				chiHuRight.opr_or(GameConstants.CHR_HENAN_QIANG_GANG_HU);
			} else if (card_type == GameConstants.HU_CARD_TYPE_TIAN_HU) {
				chiHuRight.opr_or(GameConstants.CHR_HENAN_TIAN_HU);
			} else if (card_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
				chiHuRight.opr_or(GameConstants.CHR_HENAN_GANG_KAI);
			}
			return cbChiHuKind;
		}

		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_card_count = _logic.get_magic_card_count();

		if (magic_card_count > 2) {
			magic_card_count = 2;
		}

		for (int i = 0; i < magic_card_count; i++) {
			magic_cards_index[i] = _logic.get_magic_card_index(i);
		}

		boolean can_win = AnalyseCardUtil.analyse_feng_chi_by_cards_index(cards_index,
				_logic.switch_to_card_index(cur_card), magic_cards_index, magic_card_count);

		if (!can_win) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		boolean lai_zi_cheng_ju = true;
		if (_logic.is_magic_card(cur_card) && (card_type == GameConstants.HU_CARD_TYPE_PAOHU)) {
			lai_zi_cheng_ju = AnalyseCardUtil.analyse_lai_zi_cheng_ju_by_cards_index(cards_index,
					_logic.switch_to_card_index(cur_card), magic_cards_index, magic_card_count);
		}

		if (lai_zi_cheng_ju == false) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		if (has_rule(GameConstants.GAME_RULE_HENAN_258)) {
			boolean hu = AnalyseCardUtil.analyse_258_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
					magic_cards_index, magic_card_count);
			if (!hu) {
				chiHuRight.set_empty();
				return GameConstants.WIK_NULL;
			}
		}
		if (hupai && getRuleValue(GameConstants.GAME_RULE_HENAN_BU_DAI_FENG) != 1) {
			int maxHeiFeng = 0;
			int maxBaiFeng = 0;

			chiHuRight.baifeng_count = maxBaiFeng;
			chiHuRight.heifeng_count = maxHeiFeng;
		}
		cbChiHuKind = GameConstants.WIK_CHI_HU;
		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else if (card_type == GameConstants.HU_CARD_TYPE_PAOHU) {
			chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
		} else if (card_type == GameConstants.HU_CARD_TYPE_QIANGGANG) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_QIANG_GANG_HU);
		} else if (card_type == GameConstants.HU_CARD_TYPE_TIAN_HU) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_TIAN_HU);
		} else if (card_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_GANG_KAI);
		}

		return cbChiHuKind;
	}

	public int analyse_chi_hu_card_hz_new(int cards_index[], WeaveItem weaveItems[], int weaveCount, int cur_card,
			ChiHuRight chiHuRight, int card_type) {
		if ((has_rule(GameConstants.GAME_RULE_HUNAN_ZIMOHU) && (card_type == GameConstants.HU_CARD_TYPE_PAOHU)))
			return GameConstants.WIK_NULL;

		if (cur_card == 0)
			return GameConstants.WIK_NULL;

		int result = GameConstants.WIK_NULL;

		if (has_rule(GameConstants.GAME_RULE_HUNAN_QIDUI) || is_mj_type(GameConstants.GAME_TYPE_HENAN_HZ)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_HN_HONG_ZHONG)) {
			long qxd = _logic.is_qi_xiao_dui(cards_index, weaveItems, weaveCount, cur_card);
			if (qxd != GameConstants.WIK_NULL) {
				result = GameConstants.WIK_CHI_HU;
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
					chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
				} else {
					chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
				}
			}
		}

		int max_hz = 4;
		if ((is_mj_type(GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_YUYANG_HONGZHON)
				|| is_mj_type(GameConstants.GAME_TYPE_HONG_ZHONG_MJ_TH)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_HONG_ZHONG_FEI)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_PING_XIANG_ZZ))
				&& GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HUNAN_HONG_ZHONG_8_HZ))
			max_hz = 8;
		if ((cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD)] == max_hz)
				|| ((cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD)] == (max_hz - 1))
						&& (cur_card == GameConstants.HZ_MAGIC_CARD))) {
			result = GameConstants.WIK_CHI_HU;
			if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
				chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
			} else {
				chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
			}
		}

		if (chiHuRight.is_empty() == false)
			return result;

		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_card_count = _logic.get_magic_card_count();

		if (magic_card_count > 2) { // 一般只有两种癞子牌存在
			magic_card_count = 2;
		}

		for (int i = 0; i < magic_card_count; i++) {
			magic_cards_index[i] = _logic.get_magic_card_index(i);
		}

		boolean can_win = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
				magic_cards_index, magic_card_count);

		if (!can_win) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		result = GameConstants.WIK_CHI_HU;

		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else {
			chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
		}

		return result;
	}

	public int analyse_chi_hu_card_henan_xinxiang_new(int[] cards_index, int cur_card, int card_type,
			WeaveItem[] weave_items, int weave_count, ChiHuRight chiHuRight) {
		if ((!has_rule(GameConstants.GAME_RULE_HENAN_HENAN_PAO_HU) && (card_type == GameConstants.HU_CARD_TYPE_PAOHU)))
			return GameConstants.WIK_NULL;

		if (cur_card == 0)
			return GameConstants.WIK_NULL;

		int[] tmp_cards_index = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			tmp_cards_index[i] = cards_index[i];
		}

		if (cur_card != 0)
			tmp_cards_index[_logic.switch_to_card_index(cur_card)]++;

		int chiHuKind = GameConstants.WIK_NULL;

		// 是否勾了‘可胡七对’
		if (has_rule(GameConstants.GAME_RULE_HENAN_BAOCI)) {
			int magic_count = _logic.magic_count(tmp_cards_index);
			magic_count %= 2;

			if (magic_count == 0) {
				int qi_xiao_dui = _logic.is_qi_xiao_dui_henan(tmp_cards_index, weave_items, weave_count);
				if (qi_xiao_dui != GameConstants.WIK_NULL) {
					chiHuKind = GameConstants.WIK_CHI_HU;
					if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
						chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
					} else {
						chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
					}
					chiHuRight.opr_or(qi_xiao_dui);
				}
			}
		}

		if (has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN)) {
			if ((cards_index[_logic.get_magic_card_index(0)] == 4)
					|| ((cards_index[_logic.get_magic_card_index(0)] == 3) && (_logic.is_magic_card(cur_card))
							&& (card_type == GameConstants.HU_CARD_TYPE_ZIMO
									|| card_type == GameConstants.HU_CARD_TYPE_GANG_KAI))) {
				chiHuKind = GameConstants.WIK_CHI_HU;
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
					chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
					chiHuRight.opr_or(GameConstants.CHR_HENAN_QISHOU_HU);
				} else {
					chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
					chiHuRight.opr_or(GameConstants.CHR_HENAN_QISHOU_HU);
				}

				if (has_rule(GameConstants.GAME_RULE_HENAN_QUE_MEN)) {
					if (!_logic.is_que_yi_se(tmp_cards_index, weave_items, weave_count)) {
						chiHuRight.set_empty();
						return GameConstants.WIK_NULL;
					}
				}
			}
		}

		if (chiHuRight.is_empty() == false) {
			if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
				chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
			} else if (card_type == GameConstants.HU_CARD_TYPE_PAOHU) {
				chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
			} else if (card_type == GameConstants.HU_CARD_TYPE_QIANGGANG) {
				chiHuRight.opr_or(GameConstants.CHR_HENAN_QIANG_GANG_HU);
			} else if (card_type == GameConstants.HU_CARD_TYPE_TIAN_HU) {
				chiHuRight.opr_or(GameConstants.CHR_HENAN_TIAN_HU);
			} else if (card_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
				chiHuRight.opr_or(GameConstants.CHR_HENAN_GANG_KAI);
			}
			return chiHuKind;
		}

		if (has_rule(GameConstants.GAME_RULE_HENAN_QUE_MEN)) {
			if (!_logic.is_que_yi_se(tmp_cards_index, weave_items, weave_count)) {
				chiHuRight.set_empty();
				return GameConstants.WIK_NULL;
			}
		}

		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_card_count = _logic.get_magic_card_count();

		if (magic_card_count > 2) { // 一般只有两种癞子牌存在
			magic_card_count = 2;
		}

		for (int i = 0; i < magic_card_count; i++) {
			magic_cards_index[i] = _logic.get_magic_card_index(i);
		}

		boolean can_win = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
				magic_cards_index, magic_card_count);

		if (!can_win) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		boolean lai_zi_cheng_ju = true;
		if (_logic.is_magic_card(cur_card) && (card_type == GameConstants.HU_CARD_TYPE_PAOHU)) {
			lai_zi_cheng_ju = AnalyseCardUtil.analyse_lai_zi_cheng_ju_by_cards_index(cards_index,
					_logic.switch_to_card_index(cur_card), magic_cards_index, magic_card_count);
		}

		if (lai_zi_cheng_ju == false) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		if (has_rule(GameConstants.GAME_RULE_HENAN_258) && !has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN)) {
			boolean has_258 = AnalyseCardUtil.analyse_258_by_cards_index(cards_index,
					_logic.switch_to_card_index(cur_card), magic_cards_index, magic_card_count);
			if (!has_258) {
				chiHuRight.set_empty();
				return GameConstants.WIK_NULL;
			}
		}

		chiHuKind = GameConstants.WIK_CHI_HU;
		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else if (card_type == GameConstants.HU_CARD_TYPE_PAOHU) {
			chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
		} else if (card_type == GameConstants.HU_CARD_TYPE_QIANGGANG) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_QIANG_GANG_HU);
		} else if (card_type == GameConstants.HU_CARD_TYPE_TIAN_HU) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_TIAN_HU);
		} else if (card_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_GANG_KAI);
		}

		return chiHuKind;
	}

	public int analyse_chi_hu_card_henan_new(int[] cards_index, int cur_card, int card_type, WeaveItem[] weave_items,
			int weave_count, ChiHuRight chiHuRight) {
		if ((!has_rule(GameConstants.GAME_RULE_HENAN_HENAN_PAO_HU) && (card_type == GameConstants.HU_CARD_TYPE_PAOHU)))
			return GameConstants.WIK_NULL;

		if (cur_card == 0)
			return GameConstants.WIK_NULL;

		int[] tmp_cards_index = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			tmp_cards_index[i] = cards_index[i];
		}

		if (cur_card != 0)
			tmp_cards_index[_logic.switch_to_card_index(cur_card)]++;

		int chiHuKind = GameConstants.WIK_NULL;

		int magic_count = _logic.magic_count(tmp_cards_index);
		magic_count %= 2;

		if (magic_count == 0) {
			int qi_xiao_dui = _logic.is_qi_xiao_dui_henan(tmp_cards_index, weave_items, weave_count);
			if (qi_xiao_dui != GameConstants.WIK_NULL) {
				chiHuKind = GameConstants.WIK_CHI_HU;
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
					chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
				} else {
					chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
				}
				chiHuRight.opr_or(qi_xiao_dui);
			}
		}

		if (has_rule(GameConstants.GAME_RULE_HENAN_QUE_MEN)) {
			if (!_logic.is_que_yi_se(tmp_cards_index, weave_items, weave_count)) {
				chiHuRight.set_empty();
				return GameConstants.WIK_NULL;
			}
		}

		if (has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN)) {
			if ((cards_index[_logic.get_magic_card_index(0)] == 4)
					|| ((cards_index[_logic.get_magic_card_index(0)] == 3) && (_logic.is_magic_card(cur_card))
							&& (card_type == GameConstants.HU_CARD_TYPE_ZIMO
									|| card_type == GameConstants.HU_CARD_TYPE_GANG_KAI))) {
				chiHuKind = GameConstants.WIK_CHI_HU;
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
					chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
					chiHuRight.opr_or(GameConstants.CHR_HENAN_QISHOU_HU);
				} else {
					chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
					chiHuRight.opr_or(GameConstants.CHR_HENAN_QISHOU_HU);
				}
			}
		}

		if (chiHuRight.is_empty() == false) {
			if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
				chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
			} else if (card_type == GameConstants.HU_CARD_TYPE_PAOHU) {
				chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
			} else if (card_type == GameConstants.HU_CARD_TYPE_QIANGGANG) {
				chiHuRight.opr_or(GameConstants.CHR_HENAN_QIANG_GANG_HU);
			} else if (card_type == GameConstants.HU_CARD_TYPE_TIAN_HU) {
				chiHuRight.opr_or(GameConstants.CHR_HENAN_TIAN_HU);
			} else if (card_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
				chiHuRight.opr_or(GameConstants.CHR_HENAN_GANG_KAI);
			}
			return chiHuKind;
		}

		if (has_rule(GameConstants.GAME_RULE_HENAN_HEI_ZI) && (is_mj_type(GameConstants.GAME_TYPE_HENAN_PDS)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_PING_DING_SHAN))) {
			if (1 == _logic.get_se_count(tmp_cards_index, weave_items, weave_count)) {
				chiHuRight.opr_or(GameConstants.CHR_HENAN_HEI_ZI);
			}
		}

		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_card_count = _logic.get_magic_card_count();

		if (magic_card_count > 2) { // 一般只有两种癞子牌存在
			magic_card_count = 2;
		}

		for (int i = 0; i < magic_card_count; i++) {
			magic_cards_index[i] = _logic.get_magic_card_index(i);
		}

		boolean can_win = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
				magic_cards_index, magic_card_count);

		if (!can_win) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		boolean lai_zi_cheng_ju = true;
		if (_logic.is_magic_card(cur_card) && (card_type == GameConstants.HU_CARD_TYPE_PAOHU)) {
			lai_zi_cheng_ju = AnalyseCardUtil.analyse_lai_zi_cheng_ju_by_cards_index(cards_index,
					_logic.switch_to_card_index(cur_card), magic_cards_index, magic_card_count);
		}

		if (lai_zi_cheng_ju == false) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		if (has_rule(GameConstants.GAME_RULE_HENAN_258) && !has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN)) {
			boolean has_258 = AnalyseCardUtil.analyse_258_by_cards_index(cards_index,
					_logic.switch_to_card_index(cur_card), magic_cards_index, magic_card_count);
			if (!has_258) {
				chiHuRight.set_empty();
				return GameConstants.WIK_NULL;
			}
		}

		chiHuKind = GameConstants.WIK_CHI_HU;
		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else if (card_type == GameConstants.HU_CARD_TYPE_PAOHU) {
			chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
		} else if (card_type == GameConstants.HU_CARD_TYPE_QIANGGANG) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_QIANG_GANG_HU);
		} else if (card_type == GameConstants.HU_CARD_TYPE_TIAN_HU) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_TIAN_HU);
		} else if (card_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_GANG_KAI);
		}

		return chiHuKind;
	}

	/***
	 * 红中麻将胡牌解析
	 * 
	 * @param cards_index
	 * @param weaveItems
	 * @param weaveCount
	 * @param cur_card
	 * @param chiHuRight
	 * @param card_type
	 * @return
	 */
	public int analyse_chi_hu_card_hz(int cards_index[], WeaveItem weaveItems[], int weaveCount, int cur_card,
			ChiHuRight chiHuRight, int card_type) {
		// card_type 1 zimo 2 paohu 3qiangganghu

		if ((has_rule(GameConstants.GAME_RULE_HUNAN_ZIMOHU) && (card_type == GameConstants.HU_CARD_TYPE_PAOHU)))// 是否选择了自摸胡
																												// !bSelfSendCard)
		{
			return GameConstants.WIK_NULL;

		}

		if (card_type == GameConstants.HU_CARD_TYPE_PAOHU) {
			if (has_rule_ex(GameConstants.GAME_RULE_HUNAN_HONG_ZHONG_BU_JIE_PAO)) {
				int magic_count = _logic.magic_count(cards_index);
				if (magic_count > 0)
					return GameConstants.WIK_NULL;
			}
		}

		// cbCurrentCard一定不为0 !!!!!!!!!
		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		// 变量定义
		int cbChiHuKind = GameConstants.WIK_NULL;

		if (has_rule(GameConstants.GAME_RULE_HUNAN_QIDUI) || is_mj_type(GameConstants.GAME_TYPE_HENAN_HZ)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_HN_HONG_ZHONG)) { // 7.8

			// 七小对牌 豪华七小对
			long qxd = _logic.is_qi_xiao_dui(cards_index, weaveItems, weaveCount, cur_card);
			if (qxd != GameConstants.WIK_NULL) {
				cbChiHuKind = GameConstants.WIK_CHI_HU;
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
					// cbChiHuKind = MJGameConstants.WIK_CHI_HU;
					chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
				} else {
					chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);

				}
				// 红中七小不算大胡
				// chiHuRight.opr_or(qxd);
			}

		}

		if ((cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD)] == 4)
				|| ((cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD)] == 3)
						&& (cur_card == GameConstants.HZ_MAGIC_CARD))) {

			cbChiHuKind = GameConstants.WIK_CHI_HU;
			if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
				chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
			} else {
				// 这个没必要。一定是自摸
				chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
			}
		}

		// 设置变量
		// chiHuRight.set_empty();
		// chiHuRight.opr_or(MJGameConstants.CHR_SHU_FAN);

		// 抢杠胡
		// if (_current_player == MJGameConstants.INVALID_SEAT && _status_gang
		// && (cbChiHuKind == MJGameConstants.WIK_CHI_HU)) {
		// if (has_rule(MJGameConstants.GAME_TYPE_ZZ_QIANGGANGHU))// 是否选择了抢杠胡
		// {
		// cbChiHuKind = MJGameConstants.WIK_CHI_HU;
		// chiHuRight.opr_or(MJGameConstants.CHR_QIANG_GANG_HU);
		// } else {
		// chiHuRight.set_empty();
		// return MJGameConstants.WIK_NULL;
		// }
		//
		// }

		if (chiHuRight.is_empty() == false) {

			return cbChiHuKind;
		}

		// 构造扑克
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入扑克
		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
		// 分析扑克
		boolean bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weaveCount, analyseItemArray, true);
		if (!bValue) {

			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;

		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
			// cbChiHuKind = MJGameConstants.WIK_CHI_HU;

			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else {
			chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);

		}

		return cbChiHuKind;
	}

	/**
	 * 双鬼麻将 自能自摸
	 * 
	 * @param cards_index
	 * @param weaveItems
	 * @param weaveCount
	 * @param cur_card
	 * @param chiHuRight
	 * @param card_type
	 * @return
	 */
	public int analyse_chi_hu_card_sg(int cards_index[], WeaveItem weaveItems[], int weaveCount, int cur_card,
			ChiHuRight chiHuRight, int card_type) {
		// card_type 1 zimo 2 paohu 3qiangganghu
		if (card_type != GameConstants.HU_CARD_TYPE_ZIMO)//
		{
			return GameConstants.WIK_NULL;

		}

		// cbCurrentCard一定不为0 !!!!!!!!!
		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		// 变量定义
		int cbChiHuKind = GameConstants.WIK_NULL;

		// if (has_rule(MJGameConstants.GAME_TYPE_ZZ_QIDUI)){
		// // 七小对牌 豪华七小对
		// long qxd =
		// _logic.is_qi_xiao_dui(cards_index,weaveItems,weaveCount,cur_card);
		// if(qxd!=MJGameConstants.WIK_NULL ) {
		// cbChiHuKind = MJGameConstants.WIK_CHI_HU;
		// if (card_type== MJGameConstants.HU_CARD_TYPE_ZIMO) {
		// //cbChiHuKind = MJGameConstants.WIK_CHI_HU;
		//
		// chiHuRight.opr_or(MJGameConstants.CHR_ZI_MO);
		// }else{
		// chiHuRight.opr_or(MJGameConstants.CHR_SHU_FAN);
		//
		//
		// }
		//
		// //双鬼没大胡
		// chiHuRight.opr_or(qxd);
		// }
		//
		// }

		// if(chiHuRight.is_empty()==false){
		//
		// return cbChiHuKind;
		// }

		// 构造扑克
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入扑克
		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		// 大于6个就胡 先这样处理
		if (_logic.magic_count(cbCardIndexTemp) >= 6) {

			cbChiHuKind = GameConstants.WIK_CHI_HU;

			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);

			return cbChiHuKind;
		}

		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
		// 分析扑克
		boolean bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weaveCount, analyseItemArray, false);
		if (!bValue) {

			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;

		chiHuRight.opr_or(GameConstants.CHR_ZI_MO);

		return cbChiHuKind;
	}

	/**
	 * 
	 * @param cards_index
	 * @param weaveItems
	 * @param weaveCount
	 * @param cur_card
	 * @param chiHuRight
	 * @param card_type
	 * @return
	 */
	public int analyse_chi_hu_card_xthh(int cards_index[], WeaveItem weaveItems[], int weaveCount, int cur_card,
			ChiHuRight chiHuRight, int card_type, int seat_index) {
		// card_type 1 zimo 2 paohu 3qiangganghu

		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		int lai_zi_count = cards_index[_logic.get_magic_card_index(0)];
		if (_logic.is_magic_card(cur_card)) {
			lai_zi_count++;// 摸的牌是癞子
		}

		// 一赖到底。只能有一张癞子
		if (has_rule(GameConstants.GAME_RULE_HEBEI_YI_LAI_DAO_DI)) {
			if (lai_zi_count > 1) {
				return GameConstants.WIK_NULL;
			}
		}

		/**
		 * 
		 * 放铳 ：胡牌的玩家手上没有赖子，牌桌上也没有打出赖子。玩家打出你要胡的字，可以吃胡。
		 * 
		 * 抢扛胡：有玩家碰牌后，又摸了一个碰牌的字，准备杠时，可以抢胡（如A胡一条，但是B碰了一条，然后B又摸了一条准备杠时，A可以抢杠胡牌），前提条件为：胡牌的玩家手上没有赖子，牌桌上也没有打出赖子
		 * 
		 * 热铳
		 * ：玩家在暗杠.点杠,回头杠,任一杠牌过后打出来的字为需要胡牌的字，则为热铳。（只要杠过后打出来胡牌的都为热铳，手上没有赖子，牌桌上也没有打出赖子
		 * 才能胡热铳）
		 * 
		 */
		if (card_type != GameConstants.HU_CARD_TYPE_ZIMO) {
			// 放铳、抢杠胡、热铳,手上有癞子或打过癞子,都不能胡
			if (GRR._piao_lai_count > 0 || lai_zi_count > 0) {
				return GameConstants.WIK_NULL;
			}
		}

		// 变量定义
		int cbChiHuKind = GameConstants.WIK_NULL;

		// if (has_rule(MJGameConstants.GAME_TYPE_ZZ_QIDUI)){
		// // 七小对牌 豪华七小对 不成顺子。有癞子也是当其他牌
		// //两个癞子的情况需要特殊处理
		// long qxd =
		// _logic.is_qi_xiao_dui(cards_index,weaveItems,weaveCount,cur_card);
		// if(qxd!=MJGameConstants.WIK_NULL ) {
		// cbChiHuKind = MJGameConstants.WIK_CHI_HU;
		// if (card_type== MJGameConstants.HU_CARD_TYPE_ZIMO) {
		// if(lai_zi_count>0){
		// //软摸
		// chiHuRight.opr_or(MJGameConstants.CHR_HH_RUAN_MO);
		// }else{
		// //黑摸
		// chiHuRight.opr_or(MJGameConstants.CHR_HH_HEI_MO);
		// }
		//
		// }else if(card_type== MJGameConstants.HU_CARD_TYPE_PAOHU){
		// //抓铳
		// chiHuRight.opr_or(MJGameConstants.CHR_HH_ZHUO_CHONG);
		//
		// }else if(card_type== MJGameConstants.HU_CARD_TYPE_QIANGGANG){
		// //抓铳
		// chiHuRight.opr_or(MJGameConstants.CHR_HH_ZHUO_CHONG);
		//
		// }else if(card_type== MJGameConstants.HU_CARD_TYPE_RE_CHONG){
		// //热铳
		// chiHuRight.opr_or(MJGameConstants.CHR_HH_RE_CHONG);
		//
		// }
		//
		// //没大胡
		// chiHuRight.opr_or(qxd);
		// }
		//
		// }
		//
		//
		// if(chiHuRight.is_empty()==false){
		//
		// return cbChiHuKind;
		// }

		// 构造扑克
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入扑克
		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
		// 分析扑克
		boolean bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weaveCount, analyseItemArray, false);
		if (!bValue) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		boolean cheng_ju = true;

		// 胡牌分析
		// 牌型分析 现在没有这个选项
		for (int i = 0; i < analyseItemArray.size(); i++) {
			cheng_ju = true;

			// 变量定义
			AnalyseItem pAnalyseItem = analyseItemArray.get(i);
			if (pAnalyseItem.bMagicEye == true) {
				cheng_ju = false;
				continue;
			}

			for (int j = 0; j < 4; j++) {

				if (pAnalyseItem.cbWeaveKind[j] == GameConstants.WIK_PENG) {
					if ((pAnalyseItem.cbCardData[j][0] != pAnalyseItem.cbCardData[j][1])
							|| (pAnalyseItem.cbCardData[j][1] != pAnalyseItem.cbCardData[j][2])
							|| (pAnalyseItem.cbCardData[j][2] != pAnalyseItem.cbCardData[j][0])) {
						cheng_ju = false;
					}

				} else if (pAnalyseItem.cbWeaveKind[j] == GameConstants.WIK_LEFT) {
					if ((pAnalyseItem.cbCardData[j][0] != pAnalyseItem.cbCardData[j][1] - 1)
							|| (pAnalyseItem.cbCardData[j][1] != pAnalyseItem.cbCardData[j][2] - 1)
							|| (pAnalyseItem.cbCardData[j][2] != pAnalyseItem.cbCardData[j][0] + 2)) {
						cheng_ju = false;
					}
				}

				if (cheng_ju == false) {
					break;// 癞子成句
				}
			}

			if (cheng_ju == true) {
				break;// 癞子成句
			}
		}

		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
			if (cheng_ju == true) {
				// 黑摸
				chiHuRight.opr_or(GameConstants.CHR_HUBEI_HEI_MO);
			} else {
				if (!has_rule(GameConstants.GAME_RULE_HEBEI_TU_HAO_BI_GANG)) {
					if (GRR.mo_lai_count[seat_index] == 4) {
						chiHuRight.opr_or(GameConstants.CHR_HUBEI_HEI_MO);
					} else {
						chiHuRight.opr_or(GameConstants.CHR_HUBEI_RUAN_MO);
					}

				}
			}

		} else if (card_type == GameConstants.HU_CARD_TYPE_PAOHU) {
			// 抓铳
			chiHuRight.opr_or(GameConstants.CHR_HUBEI_ZHUO_CHONG);

		} else if (card_type == GameConstants.HU_CARD_TYPE_QIANGGANG) {
			// 抢杠胡
			chiHuRight.opr_or(GameConstants.CHR_HUBEI_QIANG_GANG_HU);

		} else if (card_type == GameConstants.HU_CARD_TYPE_RE_CHONG) {
			// 热铳
			chiHuRight.opr_or(GameConstants.CHR_HUBEI_RE_CHONG);

		}

		// 如果是土豪必杠
		if (has_rule(GameConstants.GAME_RULE_HEBEI_TU_HAO_BI_GANG)
				&& has_rule(GameConstants.GAME_RULE_HEBEI_YI_LAI_DAO_DI)) {

			if (GRR.mo_lai_count[seat_index] == 4) {
				if (lai_zi_count <= 1 && cheng_ju) {
					chiHuRight.opr_or(GameConstants.CHR_HUBEI_HEI_MO);
				}
			}

			// 不是黑摸
			if ((chiHuRight.opr_and(GameConstants.CHR_HUBEI_HEI_MO)).is_empty() == true) {
				return GameConstants.WIK_NULL;
			}
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;
		return cbChiHuKind;
	}

	public int analyse_chi_hu_card_xthh_new(int cards_index[], WeaveItem weaveItems[], int weaveCount, int cur_card,
			ChiHuRight chiHuRight, int card_type, int seat_index) {
		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		int lai_zi_count = cards_index[_logic.get_magic_card_index(0)];
		if (_logic.is_magic_card(cur_card)) {
			lai_zi_count++;
		}

		// 一赖到底。只能有一张癞子
		if (has_rule(GameConstants.GAME_RULE_HEBEI_YI_LAI_DAO_DI)) {
			if (lai_zi_count > 1) {
				return GameConstants.WIK_NULL;
			}
		}

		/**
		 * 
		 * 放铳 ：胡牌的玩家手上没有赖子，牌桌上也没有打出赖子。玩家打出你要胡的字，可以吃胡。
		 * 
		 * 抢扛胡：有玩家碰牌后，又摸了一个碰牌的字，准备杠时，可以抢胡（如A胡一条，但是B碰了一条，然后B又摸了一条准备杠时，A可以抢杠胡牌），前提条件为：胡牌的玩家手上没有赖子，牌桌上也没有打出赖子
		 * 
		 * 热铳
		 * ：玩家在暗杠.点杠,回头杠,任一杠牌过后打出来的字为需要胡牌的字，则为热铳。（只要杠过后打出来胡牌的都为热铳，手上没有赖子，牌桌上也没有打出赖子
		 * 才能胡热铳）
		 * 
		 */
		if (card_type != GameConstants.HU_CARD_TYPE_ZIMO) {
			// 放铳、抢杠胡、热铳,手上有癞子或打过癞子,都不能胡
			if (GRR._piao_lai_count > 0 || lai_zi_count > 0) {
				return GameConstants.WIK_NULL;
			}
		}

		// 变量定义
		int cbChiHuKind = GameConstants.WIK_NULL;

		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_card_count = _logic.get_magic_card_count();

		if (magic_card_count > 2) { // 一般只有两种癞子牌存在
			magic_card_count = 2;
		}

		for (int i = 0; i < magic_card_count; i++) {
			magic_cards_index[i] = _logic.get_magic_card_index(i);
		}

		boolean can_win = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
				magic_cards_index, magic_card_count);

		if (!can_win) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		boolean can_hei_hu = true;

		if (lai_zi_count > 0) {
			magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
			magic_card_count = 0;

			can_hei_hu = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
					magic_cards_index, magic_card_count);
		}

		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
			if (can_hei_hu == true) {
				// 黑摸
				chiHuRight.opr_or(GameConstants.CHR_HUBEI_HEI_MO);
			} else {
				if (!has_rule(GameConstants.GAME_RULE_HEBEI_TU_HAO_BI_GANG)) {
					if (GRR.mo_lai_count[seat_index] == 4) {
						chiHuRight.opr_or(GameConstants.CHR_HUBEI_HEI_MO);
					} else {
						chiHuRight.opr_or(GameConstants.CHR_HUBEI_RUAN_MO);
					}
				}
			}

		} else if (card_type == GameConstants.HU_CARD_TYPE_PAOHU) {
			// 抓铳
			chiHuRight.opr_or(GameConstants.CHR_HUBEI_ZHUO_CHONG);
		} else if (card_type == GameConstants.HU_CARD_TYPE_QIANGGANG) {
			// 抢杠胡
			chiHuRight.opr_or(GameConstants.CHR_HUBEI_QIANG_GANG_HU);
		} else if (card_type == GameConstants.HU_CARD_TYPE_RE_CHONG) {
			// 热铳
			chiHuRight.opr_or(GameConstants.CHR_HUBEI_RE_CHONG);
		}

		// 如果是土豪必杠
		if (has_rule(GameConstants.GAME_RULE_HEBEI_TU_HAO_BI_GANG)
				&& has_rule(GameConstants.GAME_RULE_HEBEI_YI_LAI_DAO_DI)) {

			if (GRR.mo_lai_count[seat_index] == 4) {
				if (lai_zi_count <= 1 && can_hei_hu == true) {
					chiHuRight.opr_or(GameConstants.CHR_HUBEI_HEI_MO);
				}
			}

			// 不是黑摸
			if ((chiHuRight.opr_and(GameConstants.CHR_HUBEI_HEI_MO)).is_empty() == true) {
				return GameConstants.WIK_NULL;
			}
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;
		return cbChiHuKind;
	}

	/**
	 * 炒股麻将
	 * 
	 * @param cards_index
	 * @param weaveItems
	 * @param weaveCount
	 * @param cur_card
	 * @param chiHuRight
	 * @param card_type
	 * @return
	 */
	public int analyse_chi_hu_card_lxcg(int cards_index[], WeaveItem weaveItems[], int weaveCount, int cur_card,
			ChiHuRight chiHuRight, int card_type) {

		// cbCurrentCard一定不为0 !!!!!!!!!
		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		// 变量定义
		int cbChiHuKind = GameConstants.WIK_NULL;

		if (chiHuRight.is_empty() == false) {

			return cbChiHuKind;
		}

		// 构造扑克
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入扑克
		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();

		// 分析扑克
		boolean bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weaveCount, analyseItemArray, false);
		if (!bValue) {

			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;
		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {

			boolean isAnGang = _logic.is_an_gang(weaveItems, weaveCount);
			if (weaveCount == 0 || isAnGang) {
				chiHuRight.opr_or(GameConstants.CHR_HUNAN_DADOU);
			} else {
				chiHuRight.opr_or(GameConstants.CHR_HUNAN_XIADOU);
			}
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else {
			chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);

		}

		return cbChiHuKind;
	}

	/**
	 * 转转麻将
	 * 
	 * @param cards_index
	 * @param weaveItems
	 * @param weaveCount
	 * @param cur_card
	 * @param chiHuRight
	 * @param card_type
	 * @return
	 */
	public int analyse_chi_hu_card_zz(int cards_index[], WeaveItem weaveItems[], int weaveCount, int cur_card,
			ChiHuRight chiHuRight, int card_type) {

		if ((has_rule(GameConstants.GAME_RULE_HUNAN_QIANGGANGHU) == false)
				&& (card_type == GameConstants.HU_CARD_TYPE_PAOHU))
			return GameConstants.WIK_NULL;

		if (cur_card == 0)
			return GameConstants.WIK_NULL;

		int cbChiHuKind = GameConstants.WIK_NULL;

		if (has_rule(GameConstants.GAME_RULE_HUNAN_QIDUI)) {
			long qxd = _logic.is_qi_xiao_dui(cards_index, weaveItems, weaveCount, cur_card);
			if (qxd != GameConstants.WIK_NULL) {
				cbChiHuKind = GameConstants.WIK_CHI_HU;
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
					chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
				} else {
					chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);

				}
			}

		}

		if (_current_player == GameConstants.INVALID_SEAT && _status_gang
				&& (cbChiHuKind == GameConstants.WIK_CHI_HU)) {
			if (has_rule(GameConstants.GAME_RULE_HUNAN_QIANGGANGHU)) {
				cbChiHuKind = GameConstants.WIK_CHI_HU;
				chiHuRight.opr_or(GameConstants.CHR_HUNAN_QIANG_GANG_HU);
			} else {
				chiHuRight.set_empty();
				return GameConstants.WIK_NULL;
			}

		}

		if (chiHuRight.is_empty() == false) {
			return cbChiHuKind;
		}

		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_card_count = _logic.get_magic_card_count();

		if (magic_card_count > 2) { // 一般只有两种癞子牌存在
			magic_card_count = 2;
		}

		for (int i = 0; i < magic_card_count; i++) {
			magic_cards_index[i] = _logic.get_magic_card_index(i);
		}

		boolean can_win = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
				magic_cards_index, magic_card_count);

		if (!can_win) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;

		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else {
			chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);

		}

		return cbChiHuKind;
	}

	public int analyse_chi_hu_card_hnhz(int cards_index[], WeaveItem weaveItems[], int weaveCount, int cur_card,
			ChiHuRight chiHuRight, int card_type) {
		if (SysParamServerUtil.is_new_algorithm(3000, 3000, 1)) {
			return analyse_chi_hu_card_hnhz_new(cards_index, weaveItems, weaveCount, cur_card, chiHuRight, card_type);
		} else {
			return analyse_chi_hu_card_hnhz_old(cards_index, weaveItems, weaveCount, cur_card, chiHuRight, card_type);
		}
	}

	/***
	 * 河南红中麻将胡牌解析
	 * 
	 * @param cards_index
	 * @param weaveItems
	 * @param weaveCount
	 * @param cur_card
	 * @param chiHuRight
	 * @param card_type
	 * @return
	 */
	public int analyse_chi_hu_card_hnhz_old(int cards_index[], WeaveItem weaveItems[], int weaveCount, int cur_card,
			ChiHuRight chiHuRight, int card_type) {
		// card_type 1 zimo 2 paohu 3qiangganghu
		if ((has_rule(GameConstants.GAME_RULE_HENAN_ZIMOHU) && (card_type == GameConstants.HU_CARD_TYPE_PAOHU)))// 是否选择了自摸胡||(is_mj_type(MJGameConstants.GAME_TYPE_HZ)&&
																												// !bSelfSendCard)
		{
			return GameConstants.WIK_NULL;

		}

		// cbCurrentCard一定不为0 !!!!!!!!!
		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		// 变量定义
		int cbChiHuKind = GameConstants.WIK_NULL;

		// 七小对牌 豪华七小对
		long qxd = _logic.is_qi_xiao_dui(cards_index, weaveItems, weaveCount, cur_card);
		if (qxd != GameConstants.WIK_NULL) {
			cbChiHuKind = GameConstants.WIK_CHI_HU;
			if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
				// cbChiHuKind = MJGameConstants.WIK_CHI_HU;
				chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
			} else {
				chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);

			}
			// 红中七小不算大胡
			// chiHuRight.opr_or(qxd);
		}

		if ((cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD)] == 4)
				|| ((cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD)] == 3)
						&& (cur_card == GameConstants.HZ_MAGIC_CARD))) {

			cbChiHuKind = GameConstants.WIK_CHI_HU;
			if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
				chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
			} else {
				// 这个没必要。一定是自摸
				chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
			}
		}

		if (chiHuRight.is_empty() == false) {

			return cbChiHuKind;
		}

		// 构造扑克
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入扑克
		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
		// 分析扑克
		boolean bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weaveCount, analyseItemArray, true);
		if (!bValue) {

			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;

		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else {
			chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);

		}

		return cbChiHuKind;
	}

	public int analyse_chi_hu_card_hnhz_new(int cards_index[], WeaveItem weaveItems[], int weaveCount, int cur_card,
			ChiHuRight chiHuRight, int card_type) {
		if ((has_rule(GameConstants.GAME_RULE_HENAN_ZIMOHU) && (card_type == GameConstants.HU_CARD_TYPE_PAOHU)))
			return GameConstants.WIK_NULL;

		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		int cbChiHuKind = GameConstants.WIK_NULL;

		// 是否勾了‘可胡七对’
		if (has_rule(GameConstants.GAME_RULE_HENAN_QIDUI_DOUBLE)) {
			long qxd = _logic.is_qi_xiao_dui(cards_index, weaveItems, weaveCount, cur_card);
			if (qxd != GameConstants.WIK_NULL) {
				cbChiHuKind = GameConstants.WIK_CHI_HU;
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
					chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
				} else {
					chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);

				}
			}
		}

		if ((cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD)] == 4)
				|| ((cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD)] == 3)
						&& (cur_card == GameConstants.HZ_MAGIC_CARD))) {

			cbChiHuKind = GameConstants.WIK_CHI_HU;
			if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
				chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
			} else {
				chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
			}
		}

		if (chiHuRight.is_empty() == false) {
			return cbChiHuKind;
		}

		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_card_count = _logic.get_magic_card_count();

		if (magic_card_count > 2) { // 一般只有两种癞子牌存在
			magic_card_count = 2;
		}

		for (int i = 0; i < magic_card_count; i++) {
			magic_cards_index[i] = _logic.get_magic_card_index(i);
		}

		boolean can_win = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
				magic_cards_index, magic_card_count);

		if (!can_win) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;

		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else {
			chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
		}

		return cbChiHuKind;
	}

	/**
	 * 韶关麻将 胡牌分析
	 * 
	 * @param cards_index
	 * @param weaveItems
	 * @param weaveCount
	 * @param cur_card
	 * @param chiHuRight
	 * @param card_type
	 * @return
	 */
	public int analyse_chi_hu_card_shaoguan(int cards_index[], WeaveItem weaveItems[], int weaveCount, int cur_card,
			ChiHuRight chiHuRight, int card_type) {

		if (card_type == GameConstants.HU_CARD_TYPE_PAOHU) {
			return GameConstants.WIK_NULL;
		}

		// cbCurrentCard一定不为0 !!!!!!!!!
		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		// 变量定义
		int cbChiHuKind = GameConstants.WIK_NULL;

		boolean hu = false;

		// 七小对牌 豪华七小对
		long qxd = _logic.is_qi_xiao_dui(cards_index, weaveItems, weaveCount, cur_card);
		if (qxd != GameConstants.WIK_NULL) {
			cbChiHuKind = GameConstants.WIK_CHI_HU;
			if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
				chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
			} else {
				chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);

			}
			hu = true;
		}

		// 构造扑克
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入扑克
		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
		// 分析扑克
		boolean bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weaveCount, analyseItemArray, false);
		if (!bValue) {

			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		// 牌型分析
		for (int i = 0; i < analyseItemArray.size(); i++) {
			// 变量定义
			AnalyseItem analyseItem = analyseItemArray.get(i);
			/*
			 * // 判断番型
			 */
			// 碰碰和
			if (_logic.is_pengpeng_hu(analyseItem)) {
				chiHuRight.opr_or(GameConstants.CHR_GD_PENGPENGHU);
				hu = true;
				break;
			}

		}

		// 清一色牌
		if (_logic.is_qing_yi_se(cards_index, weaveItems, weaveCount, cur_card)) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_QING_YI_SE);
			hu = true;
		}

		if (hu == true) {
			if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
				chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
			} else {
				chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
			}
			// 有大胡
			return GameConstants.WIK_CHI_HU;
		}

		// 胡牌分析 有没有258
		for (int i = 0; i < analyseItemArray.size(); i++) {
			// 变量定义
			AnalyseItem pAnalyseItem = analyseItemArray.get(i);
			int cbCardValue = _logic.get_card_value(pAnalyseItem.cbCardEye);
			int color = _logic.get_card_color(pAnalyseItem.cbCardEye);
			if (color > 2)
				continue;
			if (cbCardValue != 2 && cbCardValue != 5 && cbCardValue != 8) {
				continue;
			}

			hu = true;
			if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
				chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
			} else {
				chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
			}
			return GameConstants.WIK_CHI_HU;
		}
		// chiHuRight.set_empty();
		// return GameConstants.WIK_NULL;

		// 设置变量
		// chiHuRight.set_empty();
		// chiHuRight.opr_or(MJGameConstants.CHR_SHU_FAN);

		// 抢杠胡
		if (_current_player == GameConstants.INVALID_SEAT && _status_gang
				&& (cbChiHuKind == GameConstants.WIK_CHI_HU)) {
			if (has_rule(GameConstants.GAME_RULE_HUNAN_QIANGGANGHU))// 是否选择了抢杠胡
			{
				cbChiHuKind = GameConstants.WIK_CHI_HU;
				chiHuRight.opr_or(GameConstants.CHR_HUNAN_QIANG_GANG_HU);
			} else {
				chiHuRight.set_empty();
				return GameConstants.WIK_NULL;
			}

		}

		if (chiHuRight.is_empty() == false) {

			return cbChiHuKind;
		}

		// 胡牌分析
		// 牌型分析 现在没有这个选项
		// for (int i=0;i<analyseItemArray.size();i++)
		// {
		// //变量定义
		// AnalyseItem pAnalyseItem=analyseItemArray.get(i);
		// if (has_rule(MJGameConstants.GAME_TYPE_ZZ_258))
		// {
		// int cbCardValue = _logic.get_card_value(pAnalyseItem.cbCardEye);
		// if( cbCardValue != 2 && cbCardValue != 5 && cbCardValue != 8 )
		// {
		// continue;
		// }
		// }
		// cbChiHuKind = MJGameConstants.WIK_CHI_HU;
		// break;
		// }

		cbChiHuKind = GameConstants.WIK_CHI_HU;

		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
			// cbChiHuKind = MJGameConstants.WIK_CHI_HU;

			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else {
			chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);

		}

		return cbChiHuKind;
	}

	public int get_henan_zhou_kou_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount,
			boolean dai_feng, int seat_index) {
		if (SysParamServerUtil.is_new_algorithm(3000, 3000, 1)) {
			return get_henan_zhou_kou_ting_card_new(cards, cards_index, weaveItem, cbWeaveCount, dai_feng, seat_index);
		} else {
			return get_henan_zhou_kou_ting_card_old(cards, cards_index, weaveItem, cbWeaveCount, dai_feng, seat_index);
		}
	}

	public int get_henan_zhou_kou_ting_card_old(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount,
			boolean dai_feng, int seat_index) {
		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HENAN_CHU_FENG_BAO_TING)
				|| GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HENAN_BAO_TING)) {
			if (this._playerStatus[_current_player].is_bao_ting() == false) {
				return 0;
			}
		}

		// 复制数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int l = GameConstants.MAX_ZI;
		int ql = l;

		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HENAN_DAIFENG)) {
			l = GameConstants.MAX_ZI_FENG;
			ql = GameConstants.MAX_ZI_FENG; // 带风时，可以听34张牌
		}

		for (int i = 0; i < l; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_he_nan_zhou_kou(cbCardIndexTemp, weaveItem,
					cbWeaveCount, cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO, seat_index)) {
				cards[count] = cbCurrentCard;
				if (has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN) && _logic.is_magic_card(cbCurrentCard))
					cards[count] = cbCurrentCard + GameConstants.CARD_ESPECIAL_TYPE_HUN;
				count++;
			}
		}

		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HENAN_HONG_ZHONG_LAI_ZI)) {
			if (count == 0) {
				if (cards_index[this._logic.get_magic_card_index(0)] == 3) {
					cards[count] = _logic.switch_to_card_data(this._logic.get_magic_card_index(0));
					count++;
				}
			} else if (count > 0 && count < ql
					&& !GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HENAN_DAIFENG)) {
				cards[count] = _logic.switch_to_card_data(this._logic.get_magic_card_index(0));
				count++;
			} else if (count >= ql) {
				count = 1;
				cards[0] = -1;
			}
		} else {
			if (count >= ql) {
				// 全听
				count = 1;
				cards[0] = -1;
			}
		}

		return count;
	}

	public int get_henan_zhou_kou_ting_card_new(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount,
			boolean dai_feng, int seat_index) {

		PerformanceTimer timer = new PerformanceTimer();

		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HENAN_CHU_FENG_BAO_TING)
				|| GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HENAN_BAO_TING)) {
			if (this._playerStatus[_current_player].is_bao_ting() == false) {
				return 0;
			}
		}

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int l = GameConstants.MAX_ZI;
		int ql = l;

		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HENAN_DAIFENG)) {
			l = GameConstants.MAX_ZI_FENG;
			ql = GameConstants.MAX_ZI_FENG; // 带风时，可以听34张牌
		}

		for (int i = 0; i < l; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_he_nan_zhou_kou_new(cbCardIndexTemp, weaveItem,
					cbWeaveCount, cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO, seat_index)) {
				cards[count] = cbCurrentCard;
				if (has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN) && _logic.is_magic_card(cbCurrentCard))
					cards[count] = cbCurrentCard + GameConstants.CARD_ESPECIAL_TYPE_HUN;
				count++;
			}
		}

		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HENAN_HONG_ZHONG_LAI_ZI)) {
			if (count == 0) {
				if (cards_index[this._logic.get_magic_card_index(0)] == 3) {
					cards[count] = _logic.switch_to_card_data(this._logic.get_magic_card_index(0));
					count++;
				}
			} else if (count > 0 && count < ql
					&& !GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HENAN_DAIFENG)) {
				cards[count] = _logic.switch_to_card_data(this._logic.get_magic_card_index(0));
				count++;
			} else if (count >= ql) {
				count = 1;
				cards[0] = -1;
			}
		} else {
			if (count >= ql) {
				// 全听
				count = 1;
				cards[0] = -1;
			}
		}

		if (timer.get() > 50) {
			logger.warn("cost time too long " + Arrays.toString(cards_index) + ", cost time = " + timer.duration());
		}

		return count;
	}

	public int get_henan_ting_card_chu_feng_bao_ting(int[] cards, int cards_index[], WeaveItem weaveItem[],
			int cbWeaveCount, boolean dai_feng) {
		if (SysParamServerUtil.is_new_algorithm(3000, 3000, 1)) {
			return get_henan_ting_card_chu_feng_bao_ting_new(cards, cards_index, weaveItem, cbWeaveCount, dai_feng);
		} else {
			return get_henan_ting_card_chu_feng_bao_ting_old(cards, cards_index, weaveItem, cbWeaveCount, dai_feng);
		}
	}

	public int get_henan_ting_card_chu_feng_bao_ting_old(int[] cards, int cards_index[], WeaveItem weaveItem[],
			int cbWeaveCount, boolean dai_feng) {
		/**
		 * if (GameDescUtil.has_rule(gameRuleIndexEx,
		 * GameConstants.GAME_RULE_HENAN_CHU_FENG_BAO_TING)) { if
		 * (this._playerStatus[_current_player].is_bao_ting() == false) { return
		 * 0; } }
		 **/

		// 复制数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int l = GameConstants.MAX_ZI;
		int ql = l;

		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HENAN_DAIFENG)) {
			l = GameConstants.MAX_ZI_FENG;
			ql = GameConstants.MAX_ZI_FENG; // 带风时，可以听34张牌
		}

		for (int i = 0; i < l; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_chu_feng_bao_ting(cbCardIndexTemp, weaveItem,
					cbWeaveCount, cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO)) {
				cards[count] = cbCurrentCard;
				if (has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN) && _logic.is_magic_card(cbCurrentCard))
					cards[count] = cbCurrentCard + GameConstants.CARD_ESPECIAL_TYPE_HUN;
				count++;
			}
		}

		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HENAN_HONG_ZHONG_LAI_ZI)) {
			if (count == 0) {
				if (cards_index[this._logic.get_magic_card_index(0)] == 3) {
					cards[count] = _logic.switch_to_card_data(this._logic.get_magic_card_index(0));
					count++;
				}
			} else if (count > 0 && count < ql
					&& !GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HENAN_DAIFENG)) {
				cards[count] = _logic.switch_to_card_data(this._logic.get_magic_card_index(0));
				count++;
			} else if (count >= ql) {
				count = 1;
				cards[0] = -1;
			}
		} else {
			if (count >= ql) {
				// 全听
				count = 1;
				cards[0] = -1;
			}
		}

		return count;
	}

	public int get_henan_ting_card_chu_feng_bao_ting_new(int[] cards, int cards_index[], WeaveItem weaveItem[],
			int cbWeaveCount, boolean dai_feng) {
		/**
		 * if (GameDescUtil.has_rule(gameRuleIndexEx,
		 * GameConstants.GAME_RULE_HENAN_CHU_FENG_BAO_TING)) { if
		 * (this._playerStatus[_current_player].is_bao_ting() == false) { return
		 * 0; } }
		 **/

		PerformanceTimer timer = new PerformanceTimer();

		// 复制数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int l = GameConstants.MAX_ZI;
		int ql = l;

		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HENAN_DAIFENG)) {
			l = GameConstants.MAX_ZI_FENG;
			ql = GameConstants.MAX_ZI_FENG; // 带风时，可以听34张牌
		}

		for (int i = 0; i < l; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_chu_feng_bao_ting_new(cbCardIndexTemp, weaveItem,
					cbWeaveCount, cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO)) {
				cards[count] = cbCurrentCard;
				if (has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN) && _logic.is_magic_card(cbCurrentCard))
					cards[count] = cbCurrentCard + GameConstants.CARD_ESPECIAL_TYPE_HUN;
				count++;
			}
		}

		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HENAN_HONG_ZHONG_LAI_ZI)) {
			if (count == 0) {
				if (cards_index[this._logic.get_magic_card_index(0)] == 3) {
					cards[count] = _logic.switch_to_card_data(this._logic.get_magic_card_index(0));
					count++;
				}
			} else if (count > 0 && count < ql
					&& !GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HENAN_DAIFENG)) {
				cards[count] = _logic.switch_to_card_data(this._logic.get_magic_card_index(0));
				count++;
			} else if (count >= ql) {
				count = 1;
				cards[0] = -1;
			}
		} else {
			if (count >= ql) {
				// 全听
				count = 1;
				cards[0] = -1;
			}
		}

		if (timer.get() > 50) {
			logger.warn("cost time too long " + Arrays.toString(cards_index) + ", cost time = " + timer.duration());
		}

		return count;
	}

	/**
	 * 信阳麻将获取听牌
	 * 
	 * @param cards
	 * @param cards_index
	 * @param weaveItem
	 * @param cbWeaveCount
	 * @return
	 */
	public int get_henanxy_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount) {

		// 复制数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int del = 0;

		int mj_count = GameConstants.MAX_ZI_FENG;

		for (int i = 0; i < mj_count; i++) {
			// if (this._logic.is_magic_index(i))
			// continue;
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_henan_xy(cbCardIndexTemp, weaveItem, cbWeaveCount,
					cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO)) {
				cards[count] = cbCurrentCard;
				if (this._logic.is_magic_index(i)) {
					if (chr.opr_and(GameConstants.CHR_HENAN_QI_XIAO_DUI).is_empty()
							|| chr.opr_and(GameConstants.CHR_HENAN_HH_QI_XIAO_DUI).is_empty()) {
						cards[count] += GameConstants.CARD_ESPECIAL_TYPE_HUN;
					}
				}
				count++;
			}
		}

		// 有胡的牌。癞子肯定能胡
		if (count > 0) {
			// if (has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN)) {
			// cards[count] =
			// _logic.switch_to_card_data(this._logic.get_magic_card_index(0))
			// + GameConstants.CARD_ESPECIAL_TYPE_HUN;
			// count++;
			// }
		} else {
			// if (has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN)) {
			// // 看看鬼牌能不能胡
			// cbCurrentCard =
			// _logic.switch_to_card_data(this._logic.get_magic_card_index(0));
			// chr.set_empty();
			// if (GameConstants.WIK_CHI_HU ==
			// analyse_chi_hu_card_henan(cbCardIndexTemp, weaveItem,
			// cbWeaveCount,
			// cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO)) {
			// cards[count] = cbCurrentCard +
			// GameConstants.CARD_ESPECIAL_TYPE_HUN;
			// count++;
			// }
			// }
		}

		int number = 34;
		if (count >= number) {
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	/**
	 * 河南麻将获取听牌
	 * 
	 * @param cards
	 * @param cards_index
	 * @param weaveItem
	 * @param cbWeaveCount
	 * @return
	 */
	public int get_lxcg_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount,
			int seat_index) {

		// 复制数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int del = 0;

		boolean isDaiFeng = false;
		int mj_count = GameConstants.MAX_ZI;
		if (isDaiFeng) {
			mj_count = GameConstants.MAX_ZI_FENG;
		} else {
			mj_count = GameConstants.MAX_ZI;
		}

		for (int i = 0; i < mj_count; i++) {
			// if (this._logic.is_magic_index(i))
			// continue;
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_lxcg(cbCardIndexTemp, weaveItem, cbWeaveCount,
					cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}
		int number = isDaiFeng ? 34 : 27;
		if (count >= number) {
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	/**
	 * 河南麻将获取听牌
	 * 
	 * @param cards
	 * @param cards_index
	 * @param weaveItem
	 * @param cbWeaveCount
	 * @return
	 */
	public int get_lxcs_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount,
			int seat_index) {

		// 复制数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int del = 0;

		boolean isDaiFeng = false;
		int mj_count = GameConstants.MAX_ZI;
		if (isDaiFeng) {
			mj_count = GameConstants.MAX_ZI_FENG;
		} else {
			mj_count = GameConstants.MAX_ZI;
		}

		for (int i = 0; i < mj_count; i++) {
			// if (this._logic.is_magic_index(i))
			// continue;
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_cs_lx_new(cbCardIndexTemp, weaveItem, cbWeaveCount,
					cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}
		int number = isDaiFeng ? 34 : 27;
		if (count >= number) {
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	public int get_henan_ting_card(int[] cards, int[] cards_index, WeaveItem[] weave_items, int weave_count) {
		if (SysParamServerUtil.is_new_algorithm(3000, 3000, 1)) {
			return get_henan_ting_card_new(cards, cards_index, weave_items, weave_count);
		} else {
			return get_henan_ting_card_old(cards, cards_index, weave_items, weave_count);
		}
	}

	public int get_hn_xin_xiang_ting_card_new(int[] cards, int[] cards_index, WeaveItem[] weave_items,
			int weave_count) {
		PerformanceTimer timer = new PerformanceTimer();

		int count = 0;

		int[] tmp_cards_index = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++)
			tmp_cards_index[i] = cards_index[i];

		int card_type_count = GameConstants.MAX_ZI;
		if (has_rule(GameConstants.GAME_RULE_HENAN_DAIFENG))
			card_type_count = GameConstants.MAX_ZI_FENG;

		ChiHuRight chiHuRight = new ChiHuRight();
		int tmp_card = 0;
		for (int i = 0; i < card_type_count; i++) {
			tmp_card = _logic.switch_to_card_data(i);
			chiHuRight.set_empty();

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_henan_xinxiang_new(tmp_cards_index, tmp_card,
					GameConstants.HU_CARD_TYPE_ZIMO, weave_items, weave_count, chiHuRight)) {
				cards[count] = tmp_card;
				if (_logic.is_magic_index(i)) {
					if (chiHuRight.opr_and(GameConstants.CHR_HENAN_QI_XIAO_DUI).is_empty()
							|| chiHuRight.opr_and(GameConstants.CHR_HENAN_HH_QI_XIAO_DUI).is_empty()) {
						cards[count] += GameConstants.CARD_ESPECIAL_TYPE_HUN;
					}
				}

				count++;
			}
		}

		if (count >= card_type_count) {
			count = 1;
			cards[0] = -1;
		}

		if (timer.get() > 50) {
			logger.warn("cost time too long " + Arrays.toString(cards_index) + ", cost time = " + timer.duration());
		}

		return count;
	}

	public int get_henan_ting_card_new(int[] cards, int[] cards_index, WeaveItem[] weave_items, int weave_count) {
		PerformanceTimer timer = new PerformanceTimer();

		int count = 0;

		int[] tmp_cards_index = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++)
			tmp_cards_index[i] = cards_index[i];

		int card_type_count = GameConstants.MAX_ZI;
		if (has_rule(GameConstants.GAME_RULE_HENAN_DAIFENG))
			card_type_count = GameConstants.MAX_ZI_FENG;

		ChiHuRight chiHuRight = new ChiHuRight();
		int tmp_card = 0;
		for (int i = 0; i < card_type_count; i++) {
			tmp_card = _logic.switch_to_card_data(i);
			chiHuRight.set_empty();

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_henan_new(tmp_cards_index, tmp_card,
					GameConstants.HU_CARD_TYPE_ZIMO, weave_items, weave_count, chiHuRight)) {
				cards[count] = tmp_card;

				if (_logic.is_magic_index(i)) {
					if (chiHuRight.opr_and(GameConstants.CHR_HENAN_QI_XIAO_DUI).is_empty()
							|| chiHuRight.opr_and(GameConstants.CHR_HENAN_HH_QI_XIAO_DUI).is_empty()) {
						cards[count] += GameConstants.CARD_ESPECIAL_TYPE_HUN;
					}
				}

				count++;
			}
		}

		if (count >= card_type_count) {
			count = 1;
			cards[0] = -1;
		}

		if (timer.get() > 50) {
			logger.warn("cost time too long " + Arrays.toString(cards_index) + ", cost time = " + timer.duration());
		}

		return count;
	}

	/**
	 * 河南麻将获取听牌
	 * 
	 * @param cards
	 * @param cards_index
	 * @param weaveItem
	 * @param cbWeaveCount
	 * @return
	 */
	public int get_henan_ting_card_old(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount) {

		// 复制数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int del = 0;

		boolean isDaiFeng = has_rule(GameConstants.GAME_RULE_HENAN_DAIFENG);
		int mj_count = GameConstants.MAX_ZI;
		if (isDaiFeng) {
			mj_count = GameConstants.MAX_ZI_FENG;
		} else {
			mj_count = GameConstants.MAX_ZI;
		}

		for (int i = 0; i < mj_count; i++) {
			// if (this._logic.is_magic_index(i))
			// continue;
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_henan(cbCardIndexTemp, weaveItem, cbWeaveCount,
					cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO)) {

				cards[count] = cbCurrentCard;
				if (this._logic.is_magic_index(i)) {
					if (chr.opr_and(GameConstants.CHR_HENAN_QI_XIAO_DUI).is_empty()
							|| chr.opr_and(GameConstants.CHR_HENAN_HH_QI_XIAO_DUI).is_empty()) {
						cards[count] += GameConstants.CARD_ESPECIAL_TYPE_HUN;
					}
				}
				count++;
			}
		}

		// 有胡的牌。癞子肯定能胡
		if (count > 0) {
			// if (has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN)) {
			// cards[count] =
			// _logic.switch_to_card_data(this._logic.get_magic_card_index(0))
			// + GameConstants.CARD_ESPECIAL_TYPE_HUN;
			// count++;
			// }
		} else {
			// if (has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN)) {
			// // 看看鬼牌能不能胡
			// cbCurrentCard =
			// _logic.switch_to_card_data(this._logic.get_magic_card_index(0));
			// chr.set_empty();
			// if (GameConstants.WIK_CHI_HU ==
			// analyse_chi_hu_card_henan(cbCardIndexTemp, weaveItem,
			// cbWeaveCount,
			// cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO)) {
			// cards[count] = cbCurrentCard +
			// GameConstants.CARD_ESPECIAL_TYPE_HUN;
			// count++;
			// }
			// }
		}

		int number = isDaiFeng ? 34 : 27;
		if (count >= number) {
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	public int get_hz_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount,
			boolean dai_feng) {
		if (SysParamServerUtil.is_new_algorithm(3000, 3000, 1)) {
			return get_hz_ting_card_new(cards, cards_index, weaveItem, cbWeaveCount, dai_feng);
		} else {
			return get_hz_ting_card_old(cards, cards_index, weaveItem, cbWeaveCount, dai_feng);
		}
	}

	public int get_zhuan_zhuan_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount) {
		PerformanceTimer timer = new PerformanceTimer();

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int l = GameConstants.MAX_ZI;

		for (int i = 0; i < l; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_zz(cbCardIndexTemp, weaveItem, cbWeaveCount,
					cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}

		if (has_rule(GameConstants.GAME_RULE_HUNAN_HONGZHONG)) {
			if (count == 0) {
				if (cards_index[_logic.get_magic_card_index(0)] == 3) {
					cards[count] = _logic.switch_to_card_data(_logic.get_magic_card_index(0));
					count++;
				}
			} else if (count > 0 && count < l) {
				cards[count] = _logic.switch_to_card_data(_logic.get_magic_card_index(0));
				count++;
			} else if (count >= l) {
				count = 1;
				cards[0] = -1;
			}
		} else {
			if (count >= l) {
				count = 1;
				cards[0] = -1;
			}
		}

		if (timer.get() > 50) {
			logger.warn("cost time too long " + Arrays.toString(cards_index) + ", cost time = " + timer.duration());
		}

		return count;
	}

	public int get_hnhz_ting_card_new(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount,
			boolean dai_feng) {
		PerformanceTimer timer = new PerformanceTimer();

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int l = GameConstants.MAX_INDEX - GameConstants.CARD_HUA_COUNT - GameConstants.CARD_FENG_COUNT;
		int ql = l;
		if (dai_feng) {
			l += GameConstants.CARD_FENG_COUNT;
			ql += (GameConstants.CARD_FENG_COUNT - 1);
		}
		for (int i = 0; i < l; i++) {
			if (this._logic.is_magic_index(i))
				continue;
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_hnhz(cbCardIndexTemp, weaveItem, cbWeaveCount,
					cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}

		l -= 1;
		if (count == 0) {
			if (cards_index[this._logic.get_magic_card_index(0)] == 3) {
				cards[count] = _logic.switch_to_card_data(this._logic.get_magic_card_index(0));
				count++;
			}
		} else if (count > 0 && count < ql) {
			cards[count] = _logic.switch_to_card_data(this._logic.get_magic_card_index(0));
			count++;
		} else {
			count = 1;
			cards[0] = -1;
		}

		if (timer.get() > 50) {
			logger.warn("cost time too long " + Arrays.toString(cards_index) + ", cost time = " + timer.duration());
		}

		return count;
	}

	public int get_hz_ting_card_new(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount,
			boolean dai_feng) {
		PerformanceTimer timer = new PerformanceTimer();

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int l = GameConstants.MAX_INDEX - GameConstants.CARD_HUA_COUNT - GameConstants.CARD_FENG_COUNT;
		int ql = l;
		if (dai_feng) {
			l += GameConstants.CARD_FENG_COUNT;
			ql += (GameConstants.CARD_FENG_COUNT - 1);
		}
		for (int i = 0; i < l; i++) {
			if (this._logic.is_magic_index(i))
				continue;
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_hz_new(cbCardIndexTemp, weaveItem, cbWeaveCount,
					cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}

		l -= 1;
		if (count == 0) {
			int max_hz = 4;
			if ((is_mj_type(GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ)
					|| is_mj_type(GameConstants.GAME_TYPE_MJ_YUYANG_HONGZHON)
					|| is_mj_type(GameConstants.GAME_TYPE_HONG_ZHONG_MJ_TH)
					|| is_mj_type(GameConstants.GAME_TYPE_MJ_HONG_ZHONG_FEI)
					|| is_mj_type(GameConstants.GAME_TYPE_MJ_PING_XIANG_ZZ))
					&& GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HUNAN_HONG_ZHONG_8_HZ))
				max_hz = 8;
			if (cards_index[this._logic.get_magic_card_index(0)] == (max_hz - 1)) {
				cards[count] = _logic.switch_to_card_data(this._logic.get_magic_card_index(0));
				count++;
			}
		} else if (count > 0 && count < ql) {
			cards[count] = _logic.switch_to_card_data(this._logic.get_magic_card_index(0));
			count++;
		} else {
			count = 1;
			cards[0] = -1;
		}

		if (timer.get() > 50) {
			logger.warn("cost time too long " + Arrays.toString(cards_index) + ", cost time = " + timer.duration());
		}

		return count;
	}

	public int get_hz_ting_card_old(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount,
			boolean dai_feng) {

		// 复制数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int l = GameConstants.MAX_INDEX - GameConstants.CARD_HUA_COUNT - GameConstants.CARD_FENG_COUNT;
		int ql = l;
		if (dai_feng) {
			l += GameConstants.CARD_FENG_COUNT;
			ql += (GameConstants.CARD_FENG_COUNT - 1);
		}
		for (int i = 0; i < l; i++) {
			if (this._logic.is_magic_index(i))
				continue;
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_hz(cbCardIndexTemp, weaveItem, cbWeaveCount,
					cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}

		l -= 1;
		if (count == 0) {
			// 红中能不能胡
			// cbCurrentCard =
			// _logic.switch_to_card_data(this._logic.get_magic_card_index(0));
			// if( MJGameConstants.WIK_CHI_HU == analyse_chi_hu_card_hz(
			// cbCardIndexTemp,weaveItem,cbWeaveCount,cbCurrentCard,chr,MJGameConstants.HU_CARD_TYPE_ZIMO
			// ) ){
			// cards[count] = cbCurrentCard;
			// count++;
			// }
		} else if (count > 0 && count < ql) {
			// 有胡的牌。红中肯定能胡
			cards[count] = _logic.switch_to_card_data(this._logic.get_magic_card_index(0));
			count++;
		} else {
			// 全听
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	public int get_cs_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount,
			boolean dai_feng) {
		PerformanceTimer timer = new PerformanceTimer();

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int l = GameConstants.MAX_ZI;

		for (int i = 0; i < l; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_cs_new(cbCardIndexTemp, weaveItem, cbWeaveCount,
					cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}

		if (count == l) {
			count = 1;
			cards[0] = -1;
		}

		if (timer.get() > 50) {
			logger.warn("cost time too long " + Arrays.toString(cards_index) + ", cost time = " + timer.duration());
		}

		return count;
	}

	public int analyse_chi_hu_card_cs_new(int[] cards_index, WeaveItem[] weaveItem, int weaveCount, int cur_card,
			ChiHuRight chiHuRight, int card_type) {
		if (cur_card == 0)
			return GameConstants.WIK_NULL;

		boolean has_big_win = false;

		long qxd = _logic.is_qi_xiao_dui(cards_index, weaveItem, weaveCount, cur_card);

		if (qxd != GameConstants.WIK_NULL) {
			chiHuRight.opr_or(qxd);
			has_big_win = true;
		}

		if (_logic.is_jiangjiang_hu(cards_index, weaveItem, weaveCount, cur_card)) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_JIANGJIANG_HU);
			has_big_win = true;
		}

		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_card_count = _logic.get_magic_card_count();

		if (magic_card_count > 2) { // 一般只有两种癞子牌存在
			magic_card_count = 2;
		}

		for (int i = 0; i < magic_card_count; i++) {
			magic_cards_index[i] = _logic.get_magic_card_index(i);
		}

		boolean bValue = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
				magic_cards_index, magic_card_count);

		if (bValue == false) {
			if (has_big_win == false) {
				chiHuRight.set_empty();
				return GameConstants.WIK_NULL;
			}
		}

		if (_logic.is_dan_diao(cards_index, cur_card)) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_QUAN_QIU_REN);
			has_big_win = true;
		}

		if (_logic.is_qing_yi_se(cards_index, weaveItem, weaveCount, cur_card)) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_QING_YI_SE);
			has_big_win = true;
		}

		boolean is_peng_peng_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index(cards_index,
				_logic.switch_to_card_index(cur_card), magic_cards_index, magic_card_count);

		if (is_peng_peng_hu) {
			boolean exist_eat = exist_eat(weaveItem, weaveCount);
			if (!exist_eat) {
				chiHuRight.opr_or(GameConstants.CHR_HUNAN_PENGPENG_HU);
				has_big_win = true;
			}
		}

		if (has_big_win) {
			if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
				chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
			} else {
				chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
			}

			return GameConstants.WIK_CHI_HU;
		}

		boolean has_258 = AnalyseCardUtil.analyse_258_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
				magic_cards_index, magic_card_count);

		if (has_258) {
			if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
				chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
			} else {
				chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
			}
			return GameConstants.WIK_CHI_HU;
		}

		chiHuRight.set_empty();
		return GameConstants.WIK_NULL;
	}

	@Override
	public boolean exist_eat(WeaveItem weaveItem[], int cbWeaveCount) {
		for (int i = 0; i < cbWeaveCount; i++) {
			if (weaveItem[i].weave_kind == GameConstants.WIK_LEFT || weaveItem[i].weave_kind == GameConstants.WIK_RIGHT
					|| weaveItem[i].weave_kind == GameConstants.WIK_CENTER)
				return true;
		}

		return false;
	}

	public int get_henan_ting_card_lygc(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount) {
		if (SysParamServerUtil.is_new_algorithm(3000, 3000, 3)) {
			return get_henan_ting_card_lygc_new(cards, cards_index, weaveItem, cbWeaveCount);
		} else {
			return get_henan_ting_card_lygc_old(cards, cards_index, weaveItem, cbWeaveCount);
		}
	}

	public int get_zz_ting_card_new(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount,
			boolean dai_feng) {
		PerformanceTimer timer = new PerformanceTimer();

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int l = GameConstants.MAX_ZI;

		for (int i = 0; i < l; i++) {
			if (this._logic.is_magic_index(i))
				continue;
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_zz(cbCardIndexTemp, weaveItem, cbWeaveCount,
					cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}

		if (count == 0) {
			/*
			 * if (has_rule(GameConstants.GAME_RULE_HUNAN_HONGZHONG)) { if
			 * (cards_index[this._logic.get_magic_card_index(0)] == 3) {
			 * cards[count] =
			 * _logic.switch_to_card_data(this._logic.get_magic_card_index(0));
			 * count++; } }
			 */
		} else if (count > 0 && count < l) {
			if (has_rule(GameConstants.GAME_RULE_HUNAN_HONGZHONG)) {
				cards[count] = _logic.switch_to_card_data(this._logic.get_magic_card_index(0));
				count++;
			}
		} else if (count == l) {
			count = 1;
			cards[0] = -1;
		}

		if (timer.get() > 50) {
			logger.warn("cost time too long " + Arrays.toString(cards_index) + ", cost time = " + timer.duration());
		}

		return count;
	}

	public int get_henan_ting_card_lygc_new(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount) {
		PerformanceTimer timer = new PerformanceTimer();

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		int count = 0;
		int mj_count = GameConstants.MAX_ZI;
		if (has_rule(GameConstants.GAME_RULE_HENAN_DAIFENG)) {
			mj_count = GameConstants.MAX_ZI_FENG;
		}

		int cbCurrentCard;
		ChiHuRight chr = new ChiHuRight();

		if (is_mj_type(GameConstants.GAME_TYPE_MJ_LYGC_DIAN_PAO) || is_mj_type(GameConstants.GAME_TYPE_MJ_HE_ZE)) {
			for (int i = 0; i < mj_count; i++) {
				cbCurrentCard = _logic.switch_to_card_data(i);
				chr.set_empty();
				if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_henan_lygc_new(cbCardIndexTemp, weaveItem,
						cbWeaveCount, cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO)) {
					cards[count] = cbCurrentCard;

					count++;
				}
			}
		} else {
			int ci_card_index = _logic.get_ci_card_index();
			int ci_card = _logic.switch_to_card_data(ci_card_index);

			// TODO 1. 正常自摸时
			boolean can_ting_ci_pai = false;

			if (!has_rule(GameConstants.GAME_RULE_HENAN_YCI)) {
				for (int i = 0; i < mj_count; i++) {
					cbCurrentCard = _logic.switch_to_card_data(i);
					chr.set_empty();
					if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_henan_lygc_new(cbCardIndexTemp, weaveItem,
							cbWeaveCount, cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO)) {
						cards[count] = cbCurrentCard;

						if (i == ci_card_index) {
							cards[count] += GameConstants.CARD_ESPECIAL_TYPE_CI;
							can_ting_ci_pai = true;
						}

						count++;
					}
				}
			}

			// TODO 2. 有皮次玩法时，如果次牌等于2张
			if (can_ting_ci_pai == false) {
				if (has_rule(GameConstants.GAME_RULE_HENAN_PICI))
					if (cbCardIndexTemp[_logic.get_ci_card_index()] == 2)
						cards[count++] = ci_card + GameConstants.CARD_ESPECIAL_TYPE_CI;
			}

			for (int i = 0; i < mj_count; i++) {
				if (cbCardIndexTemp[i] == 4) {
					// TODO 3. 手牌有四张并且次牌少于3张时
					if (cbCardIndexTemp[ci_card_index] < 3) {
						cbCardIndexTemp[i] = 0;
						cbCardIndexTemp[ci_card_index]++;

						int[] temp_cards = new int[mj_count];
						int temp_count = get_lygc_ting_card_temp(temp_cards, cbCardIndexTemp, weaveItem, cbWeaveCount);

						if (temp_count > 0) {
							for (int x = 0; x < temp_count; x++) {
								boolean exist = false;
								for (int y = 0; y < count; y++) {
									if (temp_cards[x] == cards[y]) {
										exist = true;
										break;
									}
								}

								if (!exist) {
									cards[count++] = temp_cards[x];
								}
							}
						}

						cbCardIndexTemp[i] = 4;
						cbCardIndexTemp[ci_card_index]--;
					}
				}
				if (cbCardIndexTemp[i] == 3) {
					int kan_card = _logic.switch_to_card_data(i);
					// TODO 4. 手牌有三张并且次牌少于3张时
					if (cbCardIndexTemp[ci_card_index] < 3) {
						cbCardIndexTemp[i] = 0;

						chr.set_empty();
						int action = analyse_chi_hu_card_henan_lygc_new(cbCardIndexTemp, weaveItem, cbWeaveCount,
								ci_card, chr, GameConstants.HU_CARD_TYPE_ZIMO);

						if (action != GameConstants.WIK_NULL) {
							boolean exist = false;
							for (int y = 0; y < count; y++) {
								if (cards[y] == kan_card) {
									exist = true;
									break;
								}
							}

							if (!exist)
								cards[count++] = kan_card;
						}

						cbCardIndexTemp[i] = 3;
					}
				}
			}

			for (int i = 0; i < cbWeaveCount; i++) {
				if (weaveItem[i].weave_kind == GameConstants.WIK_PENG) {
					int pen_card = weaveItem[i].center_card;
					int pen_card_index = _logic.switch_to_card_index(pen_card);

					if (cbCardIndexTemp[pen_card_index] == 1) {
						// TODO 5. 有碰并且碰的那张牌在手上
						cbCardIndexTemp[pen_card_index] = 0;
						cbCardIndexTemp[ci_card_index]++;

						int[] temp_cards = new int[mj_count];
						int temp_count = get_lygc_ting_card_temp(temp_cards, cbCardIndexTemp, weaveItem, cbWeaveCount);

						if (temp_count > 0) {
							for (int x = 0; x < temp_count; x++) {
								boolean exist = false;
								for (int y = 0; y < count; y++) {
									if (temp_cards[x] == cards[y]) {
										exist = true;
										break;
									}
								}

								if (!exist) {
									cards[count++] = temp_cards[x];
								}
							}
						}

						cbCardIndexTemp[pen_card_index] = 1;
						cbCardIndexTemp[ci_card_index]--;
					} else {
						// TODO 6. 有碰并且碰的那张牌没在手上
						chr.set_empty();
						int action = analyse_chi_hu_card_henan_lygc_new(cbCardIndexTemp, weaveItem, cbWeaveCount,
								ci_card, chr, GameConstants.HU_CARD_TYPE_ZIMO);

						if (action != GameConstants.WIK_NULL) {
							boolean exist = false;
							for (int y = 0; y < count; y++) {
								if (cards[y] == pen_card) {
									exist = true;
									break;
								}
							}

							if (!exist)
								cards[count++] = pen_card;
						}
					}
				}
			}
		}

		if (count >= mj_count) {
			count = 1;
			cards[0] = -1;
		}

		if (timer.get() > 100) {
			logger.warn("cost time too long " + Arrays.toString(cards_index) + ", cost time = " + timer.duration());
		}

		return count;
	}

	private int get_lygc_ting_card_temp(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount) {
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;

		int mj_count = GameConstants.MAX_ZI;
		if (has_rule(GameConstants.GAME_RULE_HENAN_DAIFENG)) {
			mj_count = GameConstants.MAX_ZI_FENG;
		}

		int cbCurrentCard;

		for (int i = 0; i < mj_count; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_henan_lygc_new(cbCardIndexTemp, weaveItem, cbWeaveCount,
					cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO)) {
				cards[count] = cbCurrentCard;

				if (i == _logic.get_ci_card_index()) {
					cards[count] += GameConstants.CARD_ESPECIAL_TYPE_CI;
				}

				count++;
			}
		}

		if (count >= mj_count) {
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	/**
	 * 河南洛阳杠次麻将获取听牌
	 * 
	 * @param cards
	 * @param cards_index
	 * @param weaveItem
	 * @param cbWeaveCount
	 * @return
	 */
	public int get_henan_ting_card_lygc_old(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount) {

		// 复制数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		boolean isDaiFeng = has_rule(GameConstants.GAME_RULE_HENAN_DAIFENG);
		int mj_count = GameConstants.MAX_ZI;
		if (isDaiFeng) {
			mj_count = GameConstants.MAX_ZI_FENG;
		} else {
			mj_count = GameConstants.MAX_ZI;
		}

		// 普通胡牌检测
		for (int i = 0; i < mj_count; i++) {
			// if (this._logic.is_magic_index(i))
			// continue;
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_henan_lygc_new(cbCardIndexTemp, weaveItem, cbWeaveCount,
					cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO)) {

				cards[count] = cbCurrentCard;
				if (this._logic.is_magic_index(i)) {
					if (chr.opr_and(GameConstants.CHR_HENAN_QI_XIAO_DUI).is_empty()
							|| chr.opr_and(GameConstants.CHR_HENAN_HH_QI_XIAO_DUI).is_empty()) {
						cards[count] += GameConstants.CARD_ESPECIAL_TYPE_HUN;
					}
				}
				count++;
			}
		}

		// 听次牌刷新
		boolean flag = false;
		for (int i = 0; i < cards.length; i++) {
			if (cards[i] == _logic.switch_to_card_data(this._logic.get_ci_card_index())) {
				cards[i] = cards[i] + GameConstants.CARD_ESPECIAL_TYPE_CI;
				flag = true;
			}
		}

		// 三张次牌一定能胡
		if (has_rule(GameConstants.GAME_RULE_HENAN_PICI)) {
			if ((cbCardIndexTemp[_logic.get_ci_card_index()] == 2)) {
				if (flag == false) {
					cards[count] = _logic.switch_to_card_data(this._logic.get_ci_card_index())
							+ GameConstants.CARD_ESPECIAL_TYPE_CI;
					count++;
				}
			}
		}

		int number = isDaiFeng ? 34 : 27;
		if (count >= number) {
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	/**
	 * 安阳麻将 胡牌检测
	 * 
	 * @param cards_index
	 * @param weaveItems
	 * @param weaveCount
	 * @param cur_card
	 * @param chiHuRight
	 * @param card_type
	 * @return
	 */
	public int analyse_chi_hu_card_ay(int seat_index, int cards_index[], WeaveItem weaveItems[], int weaveCount,
			int cur_card, ChiHuRight chiHuRight, int card_type) {
		if ((!has_rule(GameConstants.GAME_RULE_HENAN_HENAN_PAO_HU)
				&& (card_type == GameConstants.HU_CARD_TYPE_PAOHU))) {
			return GameConstants.WIK_NULL;
		}

		// cbCurrentCard一定不为0 !!!!!!!!!
		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		// 变量定义
		int cbChiHuKind = GameConstants.WIK_NULL;

		long qxd = _logic.is_qi_xiao_dui(cards_index, weaveItems, weaveCount, cur_card);
		if (qxd != GameConstants.WIK_NULL) {
			cbChiHuKind = GameConstants.WIK_CHI_HU;
			if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
				chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
			} else {
				chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
			}
			chiHuRight.opr_or(GameConstants.CHR_HENAN_DAN_DIAO);
		}

		if (chiHuRight.is_empty() == false) {

			return cbChiHuKind;
		}

		// 构造扑克
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入扑克
		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
		boolean has_feng = false;
		if (has_rule(GameConstants.GAME_RULE_HENAN_DAIFENG)) {
			has_feng = true;
		}
		// 分析扑克
		boolean bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weaveCount, analyseItemArray, has_feng);
		if (!bValue) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		boolean ka_bian = false;
		if (_playerStatus[seat_index]._hu_card_count == 1) {// 1.只胡一张；卡边吊：两嘴
			for (int i = 0; i < analyseItemArray.size(); i++) {
				// 变量定义
				AnalyseItem pAnalyseItem = analyseItemArray.get(i);
				for (int j = 0; j < 4; j++) {
					if (pAnalyseItem.cbWeaveKind[j] != GameConstants.WIK_LEFT) {
						continue;
					}
					if (pAnalyseItem.cbCenterCard[j] == (cur_card - 1)) {
						// 如果是中间的牌
						chiHuRight.opr_or(GameConstants.CHR_HENAN_KA_ZHANG);
						ka_bian = true;
						break;
					} else {
						// 边张
						int cbCardValue = _logic.get_card_value(pAnalyseItem.cbCenterCard[j]);
						if ((cbCardValue == 1) && (pAnalyseItem.cbCenterCard[j] == (cur_card - 2))) {
							chiHuRight.opr_or(GameConstants.CHR_HENAN_BIAN_ZHANG);
							ka_bian = true;
							break;
						} else if ((cbCardValue == 7) && (pAnalyseItem.cbCenterCard[j] == cur_card)) {
							chiHuRight.opr_or(GameConstants.CHR_HENAN_BIAN_ZHANG);
							ka_bian = true;
							break;
						}
					}

					if (ka_bian == true)
						break;
				}
			}

			if (ka_bian == false) {
				// 单
				chiHuRight.opr_or(GameConstants.CHR_HENAN_DAN_DIAO);
			}
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;

		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else {
			chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
		}

		return cbChiHuKind;
	}

	public int analyse_chi_hu_card_ay_new(int seat_index, int cards_index[], WeaveItem weaveItems[], int weaveCount,
			int cur_card, ChiHuRight chiHuRight, int card_type) {
		if ((!has_rule(GameConstants.GAME_RULE_HENAN_HENAN_PAO_HU)
				&& (card_type == GameConstants.HU_CARD_TYPE_PAOHU))) {
			return GameConstants.WIK_NULL;
		}

		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		int cbChiHuKind = GameConstants.WIK_NULL;

		long qxd = _logic.is_qi_xiao_dui(cards_index, weaveItems, weaveCount, cur_card);
		if (qxd != GameConstants.WIK_NULL) {
			cbChiHuKind = GameConstants.WIK_CHI_HU;
			if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
				chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
			} else {
				chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
			}
			chiHuRight.opr_or(GameConstants.CHR_HENAN_DAN_DIAO);
		}

		if (chiHuRight.is_empty() == false) {
			return cbChiHuKind;
		}

		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_card_count = _logic.get_magic_card_count();

		if (magic_card_count > 2) { // 一般只有两种癞子牌存在
			magic_card_count = 2;
		}

		for (int i = 0; i < magic_card_count; i++) {
			magic_cards_index[i] = _logic.get_magic_card_index(i);
		}

		boolean can_win = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
				magic_cards_index, magic_card_count);

		if (!can_win) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;

		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else {
			chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
		}

		return cbChiHuKind;
	}

	/**
	 * 林州麻将 胡牌检测
	 * 
	 * @param cards_index
	 * @param weaveItems
	 * @param weaveCount
	 * @param cur_card
	 * @param chiHuRight
	 * @param card_type
	 * @return
	 */
	public int analyse_chi_hu_card_lz(int seat_index, int cards_index[], WeaveItem weaveItems[], int weaveCount,
			int cur_card, ChiHuRight chiHuRight, int card_type) {
		// cbCurrentCard一定不为0 !!!!!!!!!
		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		// 变量定义
		int cbChiHuKind = GameConstants.WIK_NULL;

		long qxd = _logic.is_qi_xiao_dui(cards_index, weaveItems, weaveCount, cur_card);
		// boolean hu=false;//是否胡的标记--//七小对 将将胡 可能不是能胡牌的牌型 优先判断
		if (qxd != GameConstants.WIK_NULL) {
			if (qxd == GameConstants.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI) {
				chiHuRight.opr_or(GameConstants.CHR_HENAN_HH_QI_XIAO_DUI);
			} else {
				chiHuRight.opr_or(qxd);
			}

			// hu = true;
		}

		if (chiHuRight.is_empty() == false) {
			cbChiHuKind = GameConstants.WIK_CHI_HU;
			if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
				chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
			} else if (card_type == GameConstants.HU_CARD_TYPE_PAOHU) {
				chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
			} else if (card_type == GameConstants.HU_CARD_TYPE_QIANGGANG) {
				chiHuRight.opr_or(GameConstants.CHR_HENAN_QIANG_GANG_HU);
			} else if (card_type == GameConstants.HU_CARD_TYPE_TIAN_HU) {
				chiHuRight.opr_or(GameConstants.CHR_HENAN_TIAN_HU);
			}
			return cbChiHuKind;
		}

		// 构造扑克
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入扑克
		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
		boolean has_feng = false;
		if (has_rule(GameConstants.GAME_RULE_HENAN_DAIFENG)) {
			has_feng = true;
		}
		// 分析扑克
		boolean bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weaveCount, analyseItemArray, has_feng);
		if (!bValue) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;

		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else if (card_type == GameConstants.HU_CARD_TYPE_PAOHU) {
			chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
		} else if (card_type == GameConstants.HU_CARD_TYPE_QIANGGANG) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_QIANG_GANG_HU);
		} else if (card_type == GameConstants.HU_CARD_TYPE_TIAN_HU) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_TIAN_HU);
		}

		return cbChiHuKind;
	}

	public int get_sg_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount) {

		// 复制数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		// int cbCurrentCard =
		// _logic.switch_to_card_data(this._logic.get_magic_card_index());
		// if( MJGameConstants.WIK_CHI_HU == analyse_chi_hu_card_hz(
		// cbCardIndexTemp,weaveItem,cbWeaveCount,cbCurrentCard,chr,true ) ){
		// cards[count] = cbCurrentCard;
		// count++;
		//
		// // cards[0] = -1;
		// }
		int count = 0;
		int cbCurrentCard;
		for (int i = 0; i < GameConstants.MAX_INDEX - GameConstants.CARD_HUA_COUNT
				- GameConstants.CARD_FENG_COUNT; i++) {
			if (this._logic.is_magic_index(i))
				continue;
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_sg(cbCardIndexTemp, weaveItem, cbWeaveCount,
					cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}

		// 有胡的牌。癞子肯定能胡
		if (count > 0) {
			cards[count] = _logic.switch_to_card_data(this._logic.get_magic_card_index(0))
					+ GameConstants.CARD_ESPECIAL_TYPE_GUI;
			count++;
			cards[count] = _logic.switch_to_card_data(this._logic.get_magic_card_index(1))
					+ GameConstants.CARD_ESPECIAL_TYPE_GUI;
			count++;
		} else {
			// 看看鬼牌能不能胡
			cbCurrentCard = _logic.switch_to_card_data(this._logic.get_magic_card_index(0));
			chr.set_empty();
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_sg(cbCardIndexTemp, weaveItem, cbWeaveCount,
					cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO)) {
				cards[count] = cbCurrentCard + GameConstants.CARD_ESPECIAL_TYPE_GUI;
				count++;

				cbCurrentCard = _logic.switch_to_card_data(this._logic.get_magic_card_index(1));
				cards[count] = cbCurrentCard + GameConstants.CARD_ESPECIAL_TYPE_GUI;
				count++;
			}

			// cbCurrentCard = _logic.switch_to_card_data(
			// this._logic.get_magic_card_index(1) );
			// cards[count] =
			// cbCurrentCard+MJGameConstants.CARD_ESPECIAL_TYPE_GUI;
			// count++;
			// chr.set_empty();
			// if( MJGameConstants.WIK_CHI_HU == analyse_chi_hu_card_hz(
			// cbCardIndexTemp,weaveItem,cbWeaveCount,cbCurrentCard,chr,
			// MJGameConstants.HU_CARD_TYPE_ZIMO ) ){
			// cards[count] =
			// cbCurrentCard+MJGameConstants.CARD_ESPECIAL_TYPE_GUI;
			// count++;
			// }

		}

		if (count >= 27) {
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	public int get_xthh_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount,
			int _seat_index) {
		if (SysParamServerUtil.is_new_algorithm(3000, 3000, 1)) {
			return get_xthh_ting_card_new(cards, cards_index, weaveItem, cbWeaveCount, _seat_index);
		} else {
			return get_xthh_ting_card_old(cards, cards_index, weaveItem, cbWeaveCount, _seat_index);
		}
	}

	public int get_xthh_ting_card_old(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount,
			int _seat_index) {
		int has_laizi_count = 0;
		// 复制数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
			if (cards_index[i] > 0 && _logic.is_magic_index(i)) {
				has_laizi_count += cards_index[i];
			}
		}

		// 一赖到底
		if (has_rule(GameConstants.GAME_RULE_HEBEI_YI_LAI_DAO_DI)) {
			if (has_laizi_count > 1) {
				return 0;
			}
		}

		ChiHuRight chr = new ChiHuRight();

		// int cbCurrentCard =
		// _logic.switch_to_card_data(this._logic.get_magic_card_index());
		// if( MJGameConstants.WIK_CHI_HU == analyse_chi_hu_card_hz(
		// cbCardIndexTemp,weaveItem,cbWeaveCount,cbCurrentCard,chr,true ) ){
		// cards[count] = cbCurrentCard;
		// count++;
		//
		// // cards[0] = -1;
		// }
		int count = 0;
		int cbCurrentCard;
		for (int i = 0; i < GameConstants.MAX_INDEX - GameConstants.CARD_FENG_COUNT
				- GameConstants.CARD_HUA_COUNT; i++) {
			if (this._logic.is_magic_index(i))
				continue;
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_xthh(cbCardIndexTemp, weaveItem, cbWeaveCount,
					cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO, _seat_index)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}

		if (has_rule(GameConstants.GAME_RULE_HEBEI_GAN_DENG_YAN)) {
			// 有胡的牌。癞子肯定能胡
			if (count > 0) {
				cards[count] = _logic.switch_to_card_data(this._logic.get_magic_card_index(0))
						+ GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				count++;
			} else {
				// 不能胡，看看癞子能不能胡
				cbCurrentCard = _logic.switch_to_card_data(this._logic.get_magic_card_index(0));
				chr.set_empty();
				if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_xthh(cbCardIndexTemp, weaveItem, cbWeaveCount,
						cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO, _seat_index)) {
					cards[count] = cbCurrentCard + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
					count++;
				}
			}

			if (count >= 27) {
				count = 1;
				cards[0] = -1;
			}

		} else if (has_rule(GameConstants.GAME_RULE_HEBEI_YI_LAI_DAO_DI)) {
			if (has_laizi_count > 0) {
				// 已经有癞子在手上了。不需要加癞子了
				// if(count>0){
				// cards[count] =
				// _logic.switch_to_card_data(this._logic.get_magic_card_index(0))+MJGameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				// count++;
				// }

				if (count >= 26) {
					count = 1;
					cards[0] = -1;
				}
			} else {
				// 手上没有癞子，看看加个癞子能不能胡
				cbCurrentCard = _logic.switch_to_card_data(this._logic.get_magic_card_index(0));
				chr.set_empty();
				if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_xthh(cbCardIndexTemp, weaveItem, cbWeaveCount,
						cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO, _seat_index)) {
					cards[count] = cbCurrentCard + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
					count++;
				}

				// // 有胡的牌。癞子肯定能胡
				// if (count > 0) {
				// cards[count] =
				// _logic.switch_to_card_data(this._logic.get_magic_card_index(0))
				// + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				// count++;
				// } else {
				// // 不能胡，看看癞子能不能胡
				// cbCurrentCard =
				// _logic.switch_to_card_data(this._logic.get_magic_card_index(0));
				// chr.set_empty();
				// if (GameConstants.WIK_CHI_HU ==
				// analyse_chi_hu_card_xthh(cbCardIndexTemp, weaveItem,
				// cbWeaveCount,
				// cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO)) {
				// cards[count] = cbCurrentCard +
				// GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				// count++;
				// }
				// }

				if (count >= 27) {
					count = 1;
					cards[0] = -1;
				}
			}
		}

		return count;
	}

	public int get_xthh_ting_card_new(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount,
			int _seat_index) {
		int has_laizi_count = 0;
		// 复制数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
			if (cards_index[i] > 0 && _logic.is_magic_index(i)) {
				has_laizi_count += cards_index[i];
			}
		}

		// 一赖到底
		if (has_rule(GameConstants.GAME_RULE_HEBEI_YI_LAI_DAO_DI)) {
			if (has_laizi_count > 1) {
				return 0;
			}
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;
		for (int i = 0; i < GameConstants.MAX_INDEX - GameConstants.CARD_FENG_COUNT
				- GameConstants.CARD_HUA_COUNT; i++) {
			if (this._logic.is_magic_index(i))
				continue;
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_xthh_new(cbCardIndexTemp, weaveItem, cbWeaveCount,
					cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO, _seat_index)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}

		if (has_rule(GameConstants.GAME_RULE_HEBEI_GAN_DENG_YAN)) {
			// 有胡的牌。癞子肯定能胡
			if (count > 0) {
				cards[count] = _logic.switch_to_card_data(this._logic.get_magic_card_index(0))
						+ GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				count++;
			} else {
				// 不能胡，看看癞子能不能胡
				cbCurrentCard = _logic.switch_to_card_data(this._logic.get_magic_card_index(0));
				chr.set_empty();
				if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_xthh_new(cbCardIndexTemp, weaveItem, cbWeaveCount,
						cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO, _seat_index)) {
					cards[count] = cbCurrentCard + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
					count++;
				}
			}

			if (count >= 27) {
				count = 1;
				cards[0] = -1;
			}

		} else if (has_rule(GameConstants.GAME_RULE_HEBEI_YI_LAI_DAO_DI)) {
			if (has_laizi_count > 0) {
				if (count >= 26) {
					count = 1;
					cards[0] = -1;
				}
			} else {
				// 手上没有癞子，看看加个癞子能不能胡
				cbCurrentCard = _logic.switch_to_card_data(this._logic.get_magic_card_index(0));
				chr.set_empty();
				if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_xthh_new(cbCardIndexTemp, weaveItem, cbWeaveCount,
						cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO, _seat_index)) {
					cards[count] = cbCurrentCard + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
					count++;
				}

				if (count >= 27) {
					count = 1;
					cards[0] = -1;
				}
			}
		}

		return count;
	}

	public int get_ay_ting_card(int seat_index, int[] cards, int cards_index[], WeaveItem weaveItem[],
			int cbWeaveCount) {
		if (SysParamServerUtil.is_new_algorithm(3000, 3000, 1)) {
			return get_ay_ting_card_new(seat_index, cards, cards_index, weaveItem, cbWeaveCount);
		} else {
			return get_ay_ting_card_old(seat_index, cards, cards_index, weaveItem, cbWeaveCount);
		}
	}

	public int get_ay_ting_card_old(int seat_index, int[] cards, int cards_index[], WeaveItem weaveItem[],
			int cbWeaveCount) {
		// 复制数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();
		int cbCurrentCard;
		int count = 0;
		for (int i = 0; i < GameConstants.MAX_INDEX - GameConstants.CARD_HUA_COUNT; i++) {
			if (this._logic.is_magic_index(i))
				continue;
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_ay(seat_index, cbCardIndexTemp, weaveItem, cbWeaveCount,
					cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}
		return count;
	}

	public int get_ay_ting_card_new(int seat_index, int[] cards, int cards_index[], WeaveItem weaveItem[],
			int cbWeaveCount) {
		PerformanceTimer timer = new PerformanceTimer();

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();
		int cbCurrentCard;
		int count = 0;
		for (int i = 0; i < GameConstants.MAX_INDEX - GameConstants.CARD_HUA_COUNT; i++) {
			if (this._logic.is_magic_index(i))
				continue;
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_ay_new(seat_index, cbCardIndexTemp, weaveItem,
					cbWeaveCount, cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}

		if (timer.get() > 50) {
			logger.warn("cost time too long " + Arrays.toString(cards_index) + ", cost time = " + timer.duration());
		}

		return count;
	}

	public int get_henan_lh_ting_card(int seat_index, int[] cards, int cards_index[], WeaveItem weaveItem[],
			int cbWeaveCount) {
		// 复制数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();
		int cbCurrentCard;
		int count = 0;
		// 遍历所有的牌去判断能不能胡牌
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (this._logic.is_magic_index(i))
				continue;
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_henan_lh(cbCardIndexTemp, weaveItem, cbWeaveCount,
					cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}
		return count;
	}

	public int get_xc_ting_card(int seat_index, int[] cards, int cards_index[], WeaveItem weaveItem[],
			int cbWeaveCount) {
		if (SysParamServerUtil.is_new_algorithm(3000, 3000, 2)) {
			return get_xc_ting_card_new(seat_index, cards, cards_index, weaveItem, cbWeaveCount);
		} else {
			return get_xc_ting_card_old(seat_index, cards, cards_index, weaveItem, cbWeaveCount);
		}
	}

	public int get_xc_ting_card_old(int seat_index, int[] cards, int cards_index[], WeaveItem weaveItem[],
			int cbWeaveCount) {
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();
		int cbCurrentCard;
		int count = 0;

		int card_index_count = GameConstants.MAX_ZI_FENG;
		if (getRuleValue(GameConstants.GAME_RULE_HENAN_BU_DAI_FENG) == 1)
			card_index_count = GameConstants.MAX_ZI;

		for (int i = 0; i < card_index_count; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			xc_analyse_type = XC_ANALYSE_NORMAL;

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_henan_xc(cbCardIndexTemp, weaveItem, cbWeaveCount,
					cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO, false)) {
				cards[count] = cbCurrentCard;
				if (has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN) && _logic.is_magic_card(cbCurrentCard))
					cards[count] = cbCurrentCard + GameConstants.CARD_ESPECIAL_TYPE_HUN;
				count++;
			}
		}

		if (count == card_index_count) {
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	public int get_xc_ting_card_new(int seat_index, int[] cards, int cards_index[], WeaveItem weaveItem[],
			int cbWeaveCount) {
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();
		int cbCurrentCard;
		int count = 0;

		int card_index_count = GameConstants.MAX_ZI_FENG;
		if (getRuleValue(GameConstants.GAME_RULE_HENAN_BU_DAI_FENG) == 1)
			card_index_count = GameConstants.MAX_ZI;

		for (int i = 0; i < card_index_count; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_henan_xc_new(cbCardIndexTemp, weaveItem, cbWeaveCount,
					cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO, false)) {
				cards[count] = cbCurrentCard;
				if (has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN) && _logic.is_magic_card(cbCurrentCard))
					cards[count] = cbCurrentCard + GameConstants.CARD_ESPECIAL_TYPE_HUN;
				count++;
			}
		}

		if (count == card_index_count) {
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	public int get_lz_ting_card(int seat_index, int[] cards, int cards_index[], WeaveItem weaveItem[],
			int cbWeaveCount) {
		if (SysParamServerUtil.is_new_algorithm(3000, 3000, 1)) {
			return get_lz_ting_card_new(seat_index, cards, cards_index, weaveItem, cbWeaveCount);
		} else {
			return get_lz_ting_card_old(seat_index, cards, cards_index, weaveItem, cbWeaveCount);
		}
	}

	public int get_lz_ting_card_old(int seat_index, int[] cards, int cards_index[], WeaveItem weaveItem[],
			int cbWeaveCount) {
		// 复制数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();
		int cbCurrentCard;
		int count = 0;
		for (int i = 0; i < GameConstants.MAX_INDEX - GameConstants.CARD_HUA_COUNT; i++) {
			if (this._logic.is_magic_index(i))
				continue;
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_ay(seat_index, cbCardIndexTemp, weaveItem, cbWeaveCount,
					cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}
		return count;
	}

	public int get_lz_ting_card_new(int seat_index, int[] cards, int cards_index[], WeaveItem weaveItem[],
			int cbWeaveCount) {
		PerformanceTimer timer = new PerformanceTimer();
		// 复制数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();
		int cbCurrentCard;
		int count = 0;
		for (int i = 0; i < GameConstants.MAX_INDEX - GameConstants.CARD_HUA_COUNT; i++) {
			if (this._logic.is_magic_index(i))
				continue;
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_ay_new(seat_index, cbCardIndexTemp, weaveItem,
					cbWeaveCount, cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}
		if (timer.get() > 50) {
			logger.warn("cost time too long " + Arrays.toString(cards_index) + ", cost time = " + timer.duration());
		}
		return count;
	}

	/**
	 * 小胡 1 、大四喜：起完牌后，玩家手上已有四张一样的牌，即可胡牌。（四喜计分等同小胡自摸） 2 、板板胡：起完牌后，玩家手上没有一张 2 、 5
	 * 、 8 （将牌），即可胡牌。（等同小胡自摸） 3 、缺一色：起完牌后，玩家手上筒、索、万任缺一门，即可胡牌。（等同小胡自摸） 4
	 * 、六六顺：起完牌后，玩家手上已有 2 个刻子（刻子：三个一样的牌），即可胡牌。（等同小胡自摸） 5 、平胡： 2 、 5 、 8
	 * 作将，其余成刻子或顺子或一句话，即可胡牌。
	 * 
	 * @param cards_index
	 * @param chiHuRight
	 * @return
	 */
	// 解析吃胡----小胡 长沙玩法
	public int analyse_chi_hu_card_cs_xiaohu(int cards_index[], ChiHuRight chiHuRight) {
		chiHuRight.reset_card();
		int cbChiHuKind = GameConstants.WIK_NULL;

		// 构造扑克
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		boolean bDaSiXi = false;// 大四喜
		boolean bBanBanHu = true;// 板板胡
		int cbQueYiMenColor[] = new int[] { 1, 1, 1 };// 缺一色
		int cbLiuLiuShun = 0;// 六六顺

		// 计算单牌
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];// 数量

			if (cbCardCount == 0) {
				continue;
			}
			// 大四喜：起完牌后，玩家手上已有四张一样的牌，即可胡牌。（四喜计分等同小胡自摸）
			if (cbCardCount == 4) {
				chiHuRight._index_da_si_xi = i;
				bDaSiXi = true;
			}
			// 六六顺：起完牌后，玩家手上已有 2 个刻子（刻子：三个一样的牌），即可胡牌。（等同小胡自摸）
			if (cbCardCount == 3) {
				if (cbLiuLiuShun == 0) {
					chiHuRight._index_liul_liu_shun_1 = i;
				} else if (cbLiuLiuShun == 1) {
					chiHuRight._index_liul_liu_shun_2 = i;
				}
				cbLiuLiuShun++;
			}

			int card = _logic.switch_to_card_data(i);
			int cbValue = _logic.get_card_value(card);
			if (cbValue == 2 || cbValue == 5 || cbValue == 8) {
				// 板板胡：起完牌后，玩家手上 !!没有!! 一张 2 、 5 、 8 （将牌），即可胡牌。（等同小胡自摸）
				bBanBanHu = false;
			}

			// 缺一色：起完牌后，玩家手上筒、索、万任缺一门，即可胡牌。（等同小胡自摸）
			int cbCardColor = _logic.get_card_color(card);
			if (cbCardColor > 2)
				continue;
			cbQueYiMenColor[cbCardColor] = 0;
		}
		if (bDaSiXi) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_XIAO_DA_SI_XI);
			cbChiHuKind = GameConstants.WIK_XIAO_HU;
		}
		if (bBanBanHu) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_XIAO_BAN_BAN_HU);
			cbChiHuKind = GameConstants.WIK_XIAO_HU;
			chiHuRight._show_all = true;
		}
		if ((cbQueYiMenColor[0] == 1) || (cbQueYiMenColor[1] == 1) || (cbQueYiMenColor[2] == 1)) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_XIAO_QUE_YI_SE);
			cbChiHuKind = GameConstants.WIK_XIAO_HU;

			chiHuRight._show_all = true;
		}
		if (cbLiuLiuShun >= 2) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_XIAO_LIU_LIU_SHUN);
			cbChiHuKind = GameConstants.WIK_XIAO_HU;
			// chiHuRight._show_all = true;
		}
		return cbChiHuKind;
	}

	/**
	 * 小胡 1 、大四喜：起完牌后，玩家手上已有四张一样的牌，即可胡牌。（四喜计分等同小胡自摸） 2 、板板胡：起完牌后，玩家手上没有一张 2 、 5
	 * 、 8 （将牌），即可胡牌。（等同小胡自摸） 3 、缺一色：起完牌后，玩家手上筒、索、万任缺一门，即可胡牌。（等同小胡自摸） 4
	 * 、六六顺：起完牌后，玩家手上已有 2 个刻子（刻子：三个一样的牌），即可胡牌。（等同小胡自摸） 5 、平胡： 2 、 5 、 8
	 * 作将，其余成刻子或顺子或一句话，即可胡牌。
	 * 
	 * @param cards_index
	 * @param chiHuRight
	 * @return
	 */
	// 解析吃胡----小胡 长沙玩法
	public int analyse_chi_hu_card_cs_xiaohu_lx(int cards_index[], ChiHuRight chiHuRight) {
		chiHuRight.reset_card();
		int cbChiHuKind = GameConstants.WIK_NULL;

		// 构造扑克
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		boolean bDaSiXi = false;// 大四喜
		boolean bBanBanHu = true;// 板板胡
		int cbQueYiMenColor[] = new int[] { 1, 1, 1 };// 缺一色
		int cbLiuLiuShun = 0; // 六六顺
		boolean bBuBuGao = false; // 步步高
		boolean bSanTon = false; // 三同
		boolean bJinTonYuNv = false; // 金童玉女
		boolean isQueYiSe = false;
		// 一枝花 5的数量
		int num_yzh = 0;
		boolean bYiZhiHua = false;

		// 对牌临时牌组
		int double_card[] = new int[7];
		int double_card_count = 0;

		// 计算单牌
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];// 数量

			if (cbCardCount == 0) {
				continue;
			}
			// 大四喜：起完牌后，玩家手上已有四张一样的牌，即可胡牌。（四喜计分等同小胡自摸）
			if (cbCardCount == 4) {
				chiHuRight._index_da_si_xi = i;
				bDaSiXi = true;
			}
			// 六六顺：起完牌后，玩家手上已有 2 个刻子（刻子：三个一样的牌），即可胡牌。（等同小胡自摸）
			if (cbCardCount >= 3) {
				if (cbLiuLiuShun == 0) {
					chiHuRight._index_liul_liu_shun_1 = i;
				} else if (cbLiuLiuShun == 1) {
					chiHuRight._index_liul_liu_shun_2 = i;
				}
				cbLiuLiuShun++;
			}

			// 对子的数组
			if (cbCardCount >= 2) {
				int card = _logic.switch_to_card_data(i);
				double_card[double_card_count] = card;
				double_card_count++;
			}

			int card = _logic.switch_to_card_data(i);
			int cbValue = _logic.get_card_value(card);
			if (cbValue == 2 || cbValue == 5 || cbValue == 8) {
				// 板板胡：起完牌后，玩家手上 !!没有!! 一张 2 、 5 、 8 （将牌），即可胡牌。（等同小胡自摸）
				bBanBanHu = false;
			}

			if (cbValue == 5) {
				num_yzh += cbCardCount;
			}

			// 缺一色：起完牌后，玩家手上筒、索、万任缺一门，即可胡牌。（等同小胡自摸）
			int cbCardColor = _logic.get_card_color(card);
			if (cbCardColor > 2)
				continue;
			cbQueYiMenColor[cbCardColor] = 0;
		}

		if ((cbQueYiMenColor[0] == 1) || (cbQueYiMenColor[1] == 1) || (cbQueYiMenColor[2] == 1)) {
			isQueYiSe = true;
		}

		// 数组默认升序
		Arrays.sort(double_card);
		// 遍历对牌组合，分析对牌小胡类型
		for (int i = 0; i < double_card.length; i++) {
			if (double_card[i] == 0)
				continue;

			int cbValue = _logic.get_card_value(double_card[i]);
			int cbCardColor = _logic.get_card_color(double_card[i]);
			int san_ton_count = 1; // 三同数量

			// 初始化步步高集合
			int bu_bu_gao_count = 0; // 步步高同花色 连对数量
			int checkValue = GameConstants.INVALID_CARD;
			if (has_rule_ex(GameConstants.GAME_RULE_HUNAN_CS_XIAOHU_BBG)) {
				// bu_bu_gao_arr[bu_bu_gao_count] = double_card[i];
				checkValue = cbValue;
				bu_bu_gao_count++;
			}
			for (int j = i + 1; j < double_card.length; j++) {
				int cbValue_o = _logic.get_card_value(double_card[j]);
				int cbCardColor_o = _logic.get_card_color(double_card[j]);
				// 三同:3对一样，对1万对1筒对1条
				if (has_rule_ex(GameConstants.GAME_RULE_HUNAN_CS_XIAOHU_ST)) {
					if (cbValue == cbValue_o) {
						san_ton_count++;
					}
					if (san_ton_count == 3) {
						bSanTon = true;
					}
				}

				// 金童玉女: 对2条对2筒
				if (has_rule_ex(GameConstants.GAME_RULE_HUNAN_CS_XIAOHU_JTYN)) {
					if (cbValue == 2 && cbValue_o == 2
							&& ((cbCardColor == 1 && cbCardColor_o == 2) || (cbCardColor == 2 && cbCardColor_o == 1))) {
						bJinTonYuNv = true;
					}
				}

				// 步步高：同一花色 三连对
				if (has_rule_ex(GameConstants.GAME_RULE_HUNAN_CS_XIAOHU_BBG)) {

					// 对牌排序后,顺序遍历
					if (cbCardColor == cbCardColor_o && (cbValue_o - checkValue <= 1)) {
						if (cbValue_o - checkValue == 1) {
							checkValue = cbValue_o;
							bu_bu_gao_count++;
						}
					} else {
						// 连对数量游标清零
						bu_bu_gao_count = 0;
					}
					if (bu_bu_gao_count > 2) {
						bBuBuGao = true;
					}
				}

			}
		}

		// 一枝花判断
		if (num_yzh == 1 && has_rule_ex(GameConstants.GAME_RULE_HUNAN_CS_XIAOHU_YZH)) {
			boolean flag = true;
			int cbQueYiMenColor_yzh[] = new int[] { 1, 1, 1 };// 缺一色
			for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
				int cbCardCount = cbCardIndexTemp[i];// 数量
				if (cbCardCount == 0)
					continue;

				int card = _logic.switch_to_card_data(i);
				int cbValue = _logic.get_card_value(card);
				if (cbValue == 2 || cbValue == 8) {
					flag = false;
				}

				if (cbValue == 5)
					continue;
				// 缺一色：起完牌后，玩家手上筒、索、万任缺一门，即可胡牌。（等同小胡自摸）
				int cbCardColor = _logic.get_card_color(card);
				if (cbCardColor > 2)
					continue;
				cbQueYiMenColor_yzh[cbCardColor] = 0;
			}

			if (flag || (((cbQueYiMenColor_yzh[0] == 1) || (cbQueYiMenColor_yzh[1] == 1)
					|| (cbQueYiMenColor_yzh[2] == 1)) && isQueYiSe == false)) {
				bYiZhiHua = true;
			}
		}

		if (bBuBuGao) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_XIAO_BU_BU_GAO);
			cbChiHuKind = GameConstants.WIK_XIAO_HU;
			chiHuRight._show_all = true;
		}
		if (bYiZhiHua) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_XIAO_YI_ZHI_HUA);
			cbChiHuKind = GameConstants.WIK_XIAO_HU;
			chiHuRight._show_all = true;
		}
		if (bSanTon) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_XIAO_SAN_TON);
			cbChiHuKind = GameConstants.WIK_XIAO_HU;
			chiHuRight._show_all = true;
		}
		if (bJinTonYuNv) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_XIAO_JING_TONG_YU_NV);
			cbChiHuKind = GameConstants.WIK_XIAO_HU;
			chiHuRight._show_all = true;
		}

		if (bDaSiXi) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_XIAO_DA_SI_XI);
			cbChiHuKind = GameConstants.WIK_XIAO_HU;
		}
		if (bBanBanHu) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_XIAO_BAN_BAN_HU);
			cbChiHuKind = GameConstants.WIK_XIAO_HU;
			chiHuRight._show_all = true;
		}
		if (isQueYiSe) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_XIAO_QUE_YI_SE);
			cbChiHuKind = GameConstants.WIK_XIAO_HU;

			chiHuRight._show_all = true;
		}
		if (cbLiuLiuShun >= 2) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_XIAO_LIU_LIU_SHUN);
			cbChiHuKind = GameConstants.WIK_XIAO_HU;
			// chiHuRight._show_all = true;
		}
		return cbChiHuKind;
	}

	// 解析吃胡 长沙玩法
	public int analyse_chi_hu_card_cs_lx_old(int cards_index[], WeaveItem weaveItem[], int weaveCount, int cur_card,
			ChiHuRight chiHuRight, int card_type) {
		// 变量定义
		// cbCurrentCard一定不为0 !!!!!!!!!
		if (cur_card == 0)
			return GameConstants.WIK_NULL;
		// 设置变量
		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
		// chiHuRight.set_empty();可以重复

		// 构造扑克
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入扑克
		cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;

		boolean hu = false;// 是否胡的标记--//七小对 将将胡 可能不是能胡牌的牌型 优先判断
		long qxd = _logic.is_qi_xiao_dui(cards_index, weaveItem, weaveCount, cur_card);
		if (qxd != GameConstants.WIK_NULL) {
			chiHuRight.opr_or(qxd);
			hu = true;
		}
		// 将将胡
		if (_logic.is_jiangjiang_hu(cards_index, weaveItem, weaveCount, cur_card)) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_JIANGJIANG_HU);
			hu = true;
		}

		// 分析扑克--通用的判断胡牌方法
		boolean bValue = _logic.analyse_card(cbCardIndexTemp, weaveItem, weaveCount, analyseItemArray, false);

		// 胡牌分析
		if (bValue == false) {
			// 不能胡的情况,有可能是七小对
			// 七小对牌 豪华七小对
			if (hu == false) {
				// chiHuRight.set_empty();
				return GameConstants.WIK_NULL;
			}
		}

		/*
		 * // 特殊番型
		 */

		if (has_rule(GameConstants.GAME_RULE_HUNAN_ALL_OPEN)) {
			if (_logic.is_dan_diao(cards_index, cur_card)) {// weaveCount
				chiHuRight.opr_or(GameConstants.CHR_HUNAN_QUAN_QIU_REN);
				hu = true;
			}
		} else {
			// 全求人
			if (_logic.is_dan_diao(cards_index, cur_card) && card_type != GameConstants.HU_CARD_TYPE_ZIMO) {// weaveCount
																											// ==
																											// 4&&
				int cbCardValue = _logic.get_card_value(cur_card);
				if (cbCardValue == 2 || cbCardValue == 5 || cbCardValue == 8) {
					chiHuRight.opr_or(GameConstants.CHR_HUNAN_QUAN_QIU_REN);
					hu = true;
				}
			}
		}

		// 清一色牌
		if (_logic.is_qing_yi_se(cards_index, weaveItem, weaveCount, cur_card)) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_QING_YI_SE);
			hu = true;
		}

		// 牌型分析
		for (int i = 0; i < analyseItemArray.size(); i++) {
			// 变量定义
			AnalyseItem analyseItem = analyseItemArray.get(i);
			/*
			 * // 判断番型
			 */
			// 碰碰和
			if (_logic.is_pengpeng_hu(analyseItem)) {
				chiHuRight.opr_or(GameConstants.CHR_HUNAN_PENGPENG_HU);
				hu = true;
				break;
			}

		}

		if (hu == true) {
			if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
				chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
			} else {
				chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
			}
			// 有大胡
			return GameConstants.WIK_CHI_HU;
		}

		// 胡牌分析 有没有258
		for (int i = 0; i < analyseItemArray.size(); i++) {
			// 变量定义
			AnalyseItem pAnalyseItem = analyseItemArray.get(i);
			int cbCardValue = _logic.get_card_value(pAnalyseItem.cbCardEye);
			if (cbCardValue != 2 && cbCardValue != 5 && cbCardValue != 8) {
				continue;
			}

			hu = true;
			if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
				chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
			} else {
				chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
			}
			return GameConstants.WIK_CHI_HU;
		}
		// chiHuRight.set_empty();
		return GameConstants.WIK_NULL;
	}

	// 解析吃胡 长沙玩法
	public int analyse_chi_hu_card_cs_lx_new(int cards_index[], WeaveItem weaveItem[], int weaveCount, int cur_card,
			ChiHuRight chiHuRight, int card_type) {
		// 变量定义
		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		boolean hu = false;// 是否胡的标记--//七小对 将将胡 可能不是能胡牌的牌型 优先判断
		long qxd = _logic.is_qi_xiao_dui(cards_index, weaveItem, weaveCount, cur_card);
		if (qxd != GameConstants.WIK_NULL) {
			chiHuRight.opr_or(qxd);
			hu = true;
		}
		// 将将胡
		if (_logic.is_jiangjiang_hu(cards_index, weaveItem, weaveCount, cur_card)) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_JIANGJIANG_HU);
			hu = true;
		}

		// 分析扑克--通用的判断胡牌方法
		// boolean bValue = _logic.analyse_card(cbCardIndexTemp, weaveItem,
		// weaveCount,
		// analyseItemArray, false);
		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_card_count = _logic.get_magic_card_count();
		if (magic_card_count > 2) { // 一般只有两种癞子牌存在
			magic_card_count = 2;
		}
		for (int i = 0; i < magic_card_count; i++) {
			magic_cards_index[i] = _logic.get_magic_card_index(i);
		}

		// 分析扑克--通用的判断胡牌方法
		boolean bValue = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
				magic_cards_index, magic_card_count);

		if (has_rule(GameConstants.GAME_RULE_HUNAN_ALL_OPEN)) {

			if (_logic.is_dan_diao(cards_index, cur_card) && bValue) {// weaveCount
				chiHuRight.opr_or(GameConstants.CHR_HUNAN_QUAN_QIU_REN);
				hu = true;
			}
		}
		// 碰碰胡
		boolean is_peng_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index(cards_index,
				_logic.switch_to_card_index(cur_card), magic_cards_index, magic_card_count);

		if (is_peng_hu) {
			boolean exist_eat = exist_eat(weaveItem, weaveCount);
			if (!exist_eat) {
				chiHuRight.opr_or(GameConstants.CHR_HUNAN_PENGPENG_HU);
				hu = true;
			}
		}

		// 胡牌分析
		if (bValue == false) {
			// 不能胡的情况,有可能是七小对
			// 七小对牌 豪华七小对
			if (hu == false) {
				return GameConstants.WIK_NULL;
			}
		}

		/*
		 * // 特殊番型
		 */
		if (!has_rule(GameConstants.GAME_RULE_HUNAN_ALL_OPEN)) {
			// 全求人
			if (_logic.is_dan_diao(cards_index, cur_card) && card_type != GameConstants.HU_CARD_TYPE_ZIMO) {// weaveCount
																											// ==
																											// 4&&
				int cbCardValue = _logic.get_card_value(cur_card);
				if (cbCardValue == 2 || cbCardValue == 5 || cbCardValue == 8) {
					chiHuRight.opr_or(GameConstants.CHR_HUNAN_QUAN_QIU_REN);
					hu = true;
				}
			}
		}

		// 清一色牌
		if (_logic.is_qing_yi_se(cards_index, weaveItem, weaveCount, cur_card)) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_QING_YI_SE);
			hu = true;
		}

		if (hu == true) {
			if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
				chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
			} else {
				chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
			}
			// 有大胡
			return GameConstants.WIK_CHI_HU;
		}

		// 胡牌分析 有没有258
		/*
		 * for (int i = 0; i < analyseItemArray.size(); i++) { // 变量定义
		 * AnalyseItem pAnalyseItem = analyseItemArray.get(i); int cbCardValue =
		 * _logic.get_card_value(pAnalyseItem.cbCardEye); if (cbCardValue != 2
		 * && cbCardValue != 5 && cbCardValue != 8) { continue; }
		 * 
		 * 
		 * return GameConstants.WIK_CHI_HU; }
		 */
		boolean hu_258 = AnalyseCardUtil.analyse_258_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
				magic_cards_index, magic_card_count);
		if (!hu_258) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		if (bValue) {
			hu = true;
			if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
				chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
			} else {
				chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
			}
			return GameConstants.WIK_CHI_HU;
		}
		// chiHuRight.set_empty();
		return GameConstants.WIK_NULL;
	}

	// 解析吃胡 长沙玩法
	public int analyse_chi_hu_card_cs(int cards_index[], WeaveItem weaveItem[], int weaveCount, int cur_card,
			ChiHuRight chiHuRight, int card_type) {
		// 变量定义
		// cbCurrentCard一定不为0 !!!!!!!!!
		if (cur_card == 0)
			return GameConstants.WIK_NULL;
		// 设置变量
		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
		// chiHuRight.set_empty();可以重复

		// 构造扑克
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入扑克
		cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;

		boolean hu = false;// 是否胡的标记--//七小对 将将胡 可能不是能胡牌的牌型 优先判断
		long qxd = _logic.is_qi_xiao_dui(cards_index, weaveItem, weaveCount, cur_card);
		if (qxd != GameConstants.WIK_NULL) {
			chiHuRight.opr_or(qxd);
			hu = true;
		}
		// 将将胡
		if (_logic.is_jiangjiang_hu(cards_index, weaveItem, weaveCount, cur_card)) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_JIANGJIANG_HU);
			hu = true;
		}

		// 分析扑克--通用的判断胡牌方法
		boolean bValue = _logic.analyse_card(cbCardIndexTemp, weaveItem, weaveCount, analyseItemArray, false);

		// 胡牌分析
		if (bValue == false) {
			// 不能胡的情况,有可能是七小对
			// 七小对牌 豪华七小对
			if (hu == false) {
				// chiHuRight.set_empty();
				return GameConstants.WIK_NULL;
			}
		}

		/*
		 * // 特殊番型
		 */

		// 全求人
		if (_logic.is_dan_diao(cards_index, cur_card)) {// weaveCount == 4&&
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_QUAN_QIU_REN);
			hu = true;
		}

		// 清一色牌
		if (_logic.is_qing_yi_se(cards_index, weaveItem, weaveCount, cur_card)) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_QING_YI_SE);
			hu = true;
		}

		// 牌型分析
		for (int i = 0; i < analyseItemArray.size(); i++) {
			// 变量定义
			AnalyseItem analyseItem = analyseItemArray.get(i);
			/*
			 * // 判断番型
			 */
			// 碰碰和
			if (_logic.is_pengpeng_hu(analyseItem)) {
				chiHuRight.opr_or(GameConstants.CHR_HUNAN_PENGPENG_HU);
				hu = true;
				break;
			}

		}

		if (hu == true) {
			if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
				chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
			} else {
				chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
			}
			// 有大胡
			return GameConstants.WIK_CHI_HU;
		}

		// 胡牌分析 有没有258
		for (int i = 0; i < analyseItemArray.size(); i++) {
			// 变量定义
			AnalyseItem pAnalyseItem = analyseItemArray.get(i);
			int cbCardValue = _logic.get_card_value(pAnalyseItem.cbCardEye);
			if (cbCardValue != 2 && cbCardValue != 5 && cbCardValue != 8) {
				continue;
			}

			hu = true;
			if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
				chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
			} else {
				chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
			}
			return GameConstants.WIK_CHI_HU;
		}
		// chiHuRight.set_empty();
		return GameConstants.WIK_NULL;
	}

	// 解析吃胡 株洲玩法
	public int analyse_chi_hu_card_zhuzhou(int cards_index[], WeaveItem weaveItem[], int weaveCount, int cur_card,
			ChiHuRight chiHuRight, int card_type) {
		// 变量定义
		if ((has_rule(GameConstants.GAME_RULE_HUNAN_QIANGGANGHU) == false)
				&& (card_type == GameConstants.HU_CARD_TYPE_PAOHU))// 是否选择了自摸胡|
																	// !bSelfSendCard)
		{
			return GameConstants.WIK_NULL;
		}

		// cbCurrentCard一定不为0 !!!!!!!!!
		if (cur_card == 0)
			return GameConstants.WIK_NULL;

		// 设置变量
		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
		chiHuRight.set_empty();

		// 构造扑克
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入扑克
		cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;

		boolean hu = false;// 大胡标记
		// 七小对牌 豪华七小对--可能是不能胡的牌型
		long qxd = _logic.is_qi_xiao_dui(cards_index, weaveItem, weaveCount, cur_card);
		if (qxd != GameConstants.WIK_NULL) {
			chiHuRight.opr_or(qxd);
			hu = true;
			if (_logic.is_qing_yi_se(cards_index, weaveItem, weaveCount, cur_card)) {
				chiHuRight.opr_or(GameConstants.CHR_HUNAN_QING_YI_SE);
			}
			return GameConstants.WIK_CHI_HU;// 七小对不算门清 可以有清一色--不与其他大胡组合 直接返回
		}

		// 分析扑克
		boolean bValue = _logic.analyse_card(cbCardIndexTemp, weaveItem, weaveCount, analyseItemArray, false);
		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else {
			chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
		}

		if (hu == false && bValue == false) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		/*
		 * // 特殊番型
		 */

		// // 将将胡
		// if (_logic.is_jiangjiang_hu(cards_index, weaveItem, weaveCount,
		// cur_card)) {
		// //chiHuRight.opr_or(MJGameConstants.CHR_HUNAN_JIANGJIANG_HU);
		// hu = true;
		// }

		// 全求人
		// if (_logic.is_dan_diao(cards_index, cur_card)) {// weaveCount == 4&&
		// //chiHuRight.opr_or(MJGameConstants.CHR_QUAN_QIU_REN);
		// hu = true;
		// }

		// 清一色牌
		if (_logic.is_qing_yi_se(cards_index, weaveItem, weaveCount, cur_card)) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_QING_YI_SE);
			hu = true;

			// if(weaveCount==0) {//特殊牌型 门清可以乱将
			// chiHuRight.opr_or(MJGameConstants.CHR_MEN_QING);
			// }
		}

		// 牌型分析
		boolean isPenghu = false;
		for (int i = 0; i < analyseItemArray.size(); i++) {
			// 变量定义
			AnalyseItem analyseItem = analyseItemArray.get(i);
			// 碰碰和
			if (_logic.is_pengpeng_hu(analyseItem)) {
				chiHuRight.opr_or(GameConstants.CHR_HUNAN_PENGPENG_HU);
				hu = true;
				isPenghu = true;

				// if(weaveCount==0) {//特殊牌型 门清可以乱将
				// chiHuRight.opr_or(MJGameConstants.CHR_MEN_QING);
				// }
			}

			int cbCardValue = _logic.get_card_value(analyseItem.cbCardEye);
			if (cbCardValue == 2 || cbCardValue == 5 || cbCardValue == 8) {
				if (isPenghu) {
					chiHuRight.opr_or(GameConstants.CHR_HUNAN_258_JIANG);// 2 5
																			// 8碰碰胡
				}
			}
		}

		if (hu == true) {
			if (weaveCount == 0) {// 有大胡 门清可以乱将
				chiHuRight.opr_or(GameConstants.CHR_HUNAN_MEN_QING);
			}
			// 有大胡
			return GameConstants.WIK_CHI_HU;
		}

		// 胡牌分析 有没有258
		for (int i = 0; i < analyseItemArray.size(); i++) {
			// 变量定义
			AnalyseItem pAnalyseItem = analyseItemArray.get(i);
			int cbCardValue = _logic.get_card_value(pAnalyseItem.cbCardEye);
			if (cbCardValue != 2 && cbCardValue != 5 && cbCardValue != 8) {
				continue;
			}
			hu = true;
			if (weaveCount == 0) {
				chiHuRight.opr_or(GameConstants.CHR_HUNAN_MEN_QING);
			}
			return GameConstants.WIK_CHI_HU;
		}

		chiHuRight.set_empty();
		return GameConstants.WIK_NULL;
	}

	public boolean estimate_player_out_card_respond_henan_luo_he(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		// 用户状态
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {

			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		int llcard = get_niao_card_num(true, 0);
		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		// 动作判断
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			//// 碰牌判断
			action = _logic.check_peng(GRR._cards_index[i], card);
			if (action != 0) {
				playerStatus.add_action(action);
				playerStatus.add_peng(card, seat_index);
				bAroseAction = true;
			}
			if (GRR._left_card_count > llcard) {
				// 杠牌判断
				action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
				if (action != 0) {
					playerStatus.add_action(GameConstants.WIK_GANG);
					playerStatus.add_gang(card, seat_index, 1);// 加上杠
					bAroseAction = true;
				}
			}

			// 可以胡的情况 判断
			if (_playerStatus[i].is_chi_hu_round() && !_logic.is_magic_card(card)) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card_henan_lh(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						GameConstants.HU_CARD_TYPE_PAOHU);

				// 结果判断
				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
					bAroseAction = true;
				}
			}
		}

		if (bAroseAction) {
			_resume_player = _current_player;// 保存当前轮到的玩家
			_current_player = GameConstants.INVALID_SEAT;// 有需要操作的玩家。当前玩家为空
			_provide_player = seat_index;

		} else {

			return false;
		}

		return true;

	}

	// 玩家出牌的动作检测
	public boolean estimate_player_out_card_respond_cs_lx(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		// 用户状态
		for (int i = 0; i < getTablePlayerNumber(); i++) {

			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		// 动作判断
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			if (_playerStatus[i].lock_huan_zhang() == false) {
				//// 碰牌判断
				action = _logic.check_peng(GRR._cards_index[i], card);
				if (action != 0) {
					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);
					bAroseAction = true;
				}
			}
			// 杠牌判断 如果剩余牌大于1，是否有杠,剩一张为海底
			if (GRR._left_card_count > 1) {
				action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
				if (action != 0) {
					if (_playerStatus[i].lock_huan_zhang() == false) {
						playerStatus.add_action(GameConstants.WIK_BU_ZHNAG);
						playerStatus.add_bu_zhang(card, seat_index, 1);// 加上补张
					}

					// 剩一张为海底
					if (GRR._left_card_count > 2) {

						if (has_rule(GameConstants.GAME_RULE_HUNAN_ONE_GANG)
								|| _playerStatus[i].lock_huan_zhang() == false) {
							boolean is_ting = false;
							// 把牌加回来
							// GRR._cards_index[i][bu_index]=save_count;
							boolean can_gang = false;
							if (is_ting == true) {
								can_gang = true;
							} else {
								// 把可以杠的这张牌去掉。看是不是听牌
								int bu_index = _logic.switch_to_card_index(card);
								int save_count = GRR._cards_index[i][bu_index];
								GRR._cards_index[i][bu_index] = 0;

								int cbWeaveIndex = GRR._weave_count[i];

								GRR._weave_items[i][cbWeaveIndex].public_card = 0;
								GRR._weave_items[i][cbWeaveIndex].center_card = card;
								GRR._weave_items[i][cbWeaveIndex].weave_kind = GameConstants.WIK_GANG;// 接杠
								GRR._weave_items[i][cbWeaveIndex].provide_player = seat_index;
								GRR._weave_count[i]++;

								can_gang = this.is_cs_ting_card(GRR._cards_index[i], GRR._weave_items[i],
										GRR._weave_count[i], i);

								// 把牌加回来
								GRR._cards_index[i][bu_index] = save_count;
								GRR._weave_count[i] = cbWeaveIndex;

							}

							if (can_gang == true) {
								playerStatus.add_action(GameConstants.WIK_GANG);
								playerStatus.add_gang(card, seat_index, 1);// 加上杠
								bAroseAction = true;
							}
						}
					}
				}
			}
			// 如果是自摸胡
			// 可以胡的情况 判断
			if (_playerStatus[i].is_chi_hu_round()) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						GameConstants.HU_CARD_TYPE_PAOHU, i);

				// 结果判断
				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
					bAroseAction = true;
				}
			}
		}

		int chi_seat_index = (seat_index + 1) % getTablePlayerNumber();
		// 长沙麻将吃操作 转转麻将不能吃
		if (_playerStatus[chi_seat_index].lock_huan_zhang() == false) {
			// 这里可能有问题 应该是 |=
			action = _logic.check_chi(GRR._cards_index[chi_seat_index], card);
			if ((action & GameConstants.WIK_LEFT) != 0) {
				_playerStatus[chi_seat_index].add_action(GameConstants.WIK_LEFT);
				_playerStatus[chi_seat_index].add_chi(card, GameConstants.WIK_LEFT, seat_index);
			}
			if ((action & GameConstants.WIK_CENTER) != 0) {
				_playerStatus[chi_seat_index].add_action(GameConstants.WIK_CENTER);
				_playerStatus[chi_seat_index].add_chi(card, GameConstants.WIK_CENTER, seat_index);
			}
			if ((action & GameConstants.WIK_RIGHT) != 0) {
				_playerStatus[chi_seat_index].add_action(GameConstants.WIK_RIGHT);
				_playerStatus[chi_seat_index].add_chi(card, GameConstants.WIK_RIGHT, seat_index);
			}

			// 结果判断
			if (_playerStatus[chi_seat_index].has_action()) {
				bAroseAction = true;
			}
		}

		if (bAroseAction) {
			_resume_player = _current_player;// 保存当前轮到的玩家
			_current_player = GameConstants.INVALID_SEAT;// 有需要操作的玩家。当前玩家为空
			_provide_player = seat_index;
		} else {
			return false;
		}
		return true;

	}

	// 玩家出牌的动作检测
	public boolean estimate_player_out_card_respond_cs(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		// 用户状态
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {

			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		// 动作判断
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			if (_playerStatus[i].lock_huan_zhang() == false) {
				//// 碰牌判断
				action = _logic.check_peng(GRR._cards_index[i], card);
				if (action != 0) {
					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);
					bAroseAction = true;
				}
			}
			// 杠牌判断 如果剩余牌大于1，是否有杠,剩一张为海底
			if (GRR._left_card_count > 1) {
				action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
				if (action != 0) {
					if (_playerStatus[i].lock_huan_zhang() == false) {
						playerStatus.add_action(GameConstants.WIK_BU_ZHNAG);
						playerStatus.add_bu_zhang(card, seat_index, 1);// 加上补张
					}

					// 剩一张为海底
					if (GRR._left_card_count > 2) {
						// 把可以杠的这张牌去掉。看是不是听牌
						// int bu_index = _logic.switch_to_card_index(card);
						// int save_count = GRR._cards_index[i][bu_index];
						// GRR._cards_index[i][bu_index]=0;

						// boolean is_ting =
						// is_cs_ting_card(GRR._cards_index[i],
						// GRR._weave_items[i], GRR._weave_count[i]);

						boolean is_ting = false;
						// 把牌加回来
						// GRR._cards_index[i][bu_index]=save_count;
						boolean can_gang = false;
						if (is_ting == true) {
							can_gang = true;
						} else {
							// 把可以杠的这张牌去掉。看是不是听牌
							int bu_index = _logic.switch_to_card_index(card);
							int save_count = GRR._cards_index[i][bu_index];
							GRR._cards_index[i][bu_index] = 0;

							int cbWeaveIndex = GRR._weave_count[i];

							GRR._weave_items[i][cbWeaveIndex].public_card = 0;
							GRR._weave_items[i][cbWeaveIndex].center_card = card;
							GRR._weave_items[i][cbWeaveIndex].weave_kind = GameConstants.WIK_GANG;// 接杠
							GRR._weave_items[i][cbWeaveIndex].provide_player = seat_index;
							GRR._weave_count[i]++;

							can_gang = this.is_cs_ting_card(GRR._cards_index[i], GRR._weave_items[i],
									GRR._weave_count[i], i);

							// 把牌加回来
							GRR._cards_index[i][bu_index] = save_count;
							GRR._weave_count[i] = cbWeaveIndex;

						}

						if (can_gang == true) {
							playerStatus.add_action(GameConstants.WIK_GANG);
							playerStatus.add_gang(card, seat_index, 1);// 加上杠
							bAroseAction = true;
						}
					}
				}
			}
			// 如果是自摸胡
			// 可以胡的情况 判断
			if (_playerStatus[i].is_chi_hu_round()) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						GameConstants.HU_CARD_TYPE_PAOHU, i);

				// 结果判断
				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
					bAroseAction = true;
				}
			}
		}

		int chi_seat_index = (seat_index + 1) % getTablePlayerNumber();
		// 长沙麻将吃操作 转转麻将不能吃
		if (_playerStatus[chi_seat_index].lock_huan_zhang() == false) {
			// 这里可能有问题 应该是 |=
			action = _logic.check_chi(GRR._cards_index[chi_seat_index], card);
			if ((action & GameConstants.WIK_LEFT) != 0) {
				_playerStatus[chi_seat_index].add_action(GameConstants.WIK_LEFT);
				_playerStatus[chi_seat_index].add_chi(card, GameConstants.WIK_LEFT, seat_index);
			}
			if ((action & GameConstants.WIK_CENTER) != 0) {
				_playerStatus[chi_seat_index].add_action(GameConstants.WIK_CENTER);
				_playerStatus[chi_seat_index].add_chi(card, GameConstants.WIK_CENTER, seat_index);
			}
			if ((action & GameConstants.WIK_RIGHT) != 0) {
				_playerStatus[chi_seat_index].add_action(GameConstants.WIK_RIGHT);
				_playerStatus[chi_seat_index].add_chi(card, GameConstants.WIK_RIGHT, seat_index);
			}

			// 结果判断
			if (_playerStatus[chi_seat_index].has_action()) {
				bAroseAction = true;
			}
		}

		if (bAroseAction) {
			_resume_player = _current_player;// 保存当前轮到的玩家
			_current_player = GameConstants.INVALID_SEAT;// 有需要操作的玩家。当前玩家为空
			_provide_player = seat_index;
		} else {
			return false;
		}
		return true;

	}

	/***
	 * 玩家出牌的动作检测--玩家出牌 响应判断,是否有吃碰杠补胡
	 * 
	 * @param seat_index
	 * @param card
	 * @return
	 */
	public boolean estimate_player_out_card_respond_zhuzhou(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		// 用户状态
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {

			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		// 动作判断
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			if (_playerStatus[i].lock_huan_zhang() == false) {
				//// 碰牌判断
				action = _logic.check_peng(GRR._cards_index[i], card);
				if (action != 0) {
					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);
					bAroseAction = true;
				}
			}
			// 杠牌判断 如果剩余牌大于1，是否有杠,剩一张为海底
			if (GRR._left_card_count > 1) {
				action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
				if (action != 0) {
					// playerStatus.add_action(MJGameConstants.WIK_BU_ZHNAG);
					// playerStatus.add_bu_zhang(card, seat_index, 1);// 加上补张
					// 剩一张为海底
					// if (GRR._left_card_count > 2) {
					// 把可以杠的这张牌去掉。看是不是听牌
					int bu_index = _logic.switch_to_card_index(card);
					int save_count = GRR._cards_index[i][bu_index];
					GRR._cards_index[i][bu_index] = 0;

					boolean is_ting = is_zhuzhou_ting_card(GRR._cards_index[i], GRR._weave_items[i],
							GRR._weave_count[i]);

					// 把牌加回来
					GRR._cards_index[i][bu_index] = save_count;

					if (is_ting == true) {
						playerStatus.add_action(GameConstants.WIK_GANG);
						playerStatus.add_gang(card, seat_index, 1);// 加上杠
						bAroseAction = true;

					}
					// }
				}
			}
			// 如果是自摸胡
			// 可以胡的情况 判断
			if (_playerStatus[i].is_chi_hu_round()) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						GameConstants.HU_CARD_TYPE_PAOHU, i);

				// 结果判断
				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
					bAroseAction = true;
				}
			}
		}

		int chi_seat_index = (seat_index + 1) % GameConstants.GAME_PLAYER;
		// 长沙麻将吃操作 转转麻将不能吃
		if (_playerStatus[chi_seat_index].lock_huan_zhang() == false) {
			// 这里可能有问题 应该是 |=
			action = _logic.check_chi(GRR._cards_index[chi_seat_index], card);
			if ((action & GameConstants.WIK_LEFT) != 0) {
				_playerStatus[chi_seat_index].add_action(GameConstants.WIK_LEFT);
				_playerStatus[chi_seat_index].add_chi(card, GameConstants.WIK_LEFT, seat_index);
			}
			if ((action & GameConstants.WIK_CENTER) != 0) {
				_playerStatus[chi_seat_index].add_action(GameConstants.WIK_CENTER);
				_playerStatus[chi_seat_index].add_chi(card, GameConstants.WIK_CENTER, seat_index);
			}
			if ((action & GameConstants.WIK_RIGHT) != 0) {
				_playerStatus[chi_seat_index].add_action(GameConstants.WIK_RIGHT);
				_playerStatus[chi_seat_index].add_chi(card, GameConstants.WIK_RIGHT, seat_index);
			}

			// 结果判断
			if (_playerStatus[chi_seat_index].has_action()) {
				bAroseAction = true;
			}
		}

		if (bAroseAction) {
			_resume_player = _current_player;// 保存当前轮到的玩家
			_current_player = GameConstants.INVALID_SEAT;// 有需要操作的玩家。当前玩家为空
			_provide_player = seat_index;
		} else {
			return false;
		}
		return true;

	}

	public boolean estimate_player_out_card_respond_henan_xc(int seat_index, int card) {
		boolean bAroseAction = false;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		int llcard = 0;
		if (this.has_rule(GameConstants.GAME_RULE_HENAN_SHUAIHUN)) {
			llcard = this.GRR._piao_lai_count * 10;
		}

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			if (!this._playerStatus[i].is_bao_ting()) {
				action = _logic.check_peng(GRR._cards_index[i], card);
				if (action != 0 && has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN) && _logic.is_magic_card(card)) {// 鬼牌不能碰
					action = 0;
				}
				if (action != 0) {
					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);
					bAroseAction = true;
				}
			}
			if (GRR._left_card_count > llcard) {
				if (_logic.is_magic_card(card)) {
					action = 0;
				} else {
					action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
					if (action != 0) {
						if ((playerStatus.is_bao_ting() == false)) {
							playerStatus.add_action(GameConstants.WIK_GANG);
							playerStatus.add_gang(card, i, 1);// 加上杠
							bAroseAction = true;
						} else {
							if (check_gang_huan_zhang_xc(i, card) == false) {
								playerStatus.add_action(GameConstants.WIK_GANG);
								playerStatus.add_gang(card, i, 1);// 加上杠
								bAroseAction = true;
							}

						}
					}
				}
			}

			if (this.has_rule(GameConstants.GAME_RULE_HENAN_HENAN_PAO_HU)) {
				if (_playerStatus[i].is_chi_hu_round()) {
					ChiHuRight chr = GRR._chi_hu_rights[i];
					chr.set_empty();
					int cbWeaveCount = GRR._weave_count[i];

					for (int k = 0; k < this._playerStatus[i]._hu_card_count; k++) {
						if (this._playerStatus[i]._hu_cards[k] == card) {
							xc_analyse_type = XC_ANALYSE_NORMAL;

							action = analyse_chi_hu_card_henan_xc(GRR._cards_index[i], GRR._weave_items[i],
									cbWeaveCount, card, chr, GameConstants.HU_CARD_TYPE_PAOHU, true);
							if (action != 0) {
								_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
								_playerStatus[i].add_chi_hu(card, seat_index);
								bAroseAction = true;
							}
							break;
						}
					}
				}
			}

			// 许昌麻将，本人出牌之后，如果能报听，添加报听，如果其他人操作了这张牌，取消报听，也就相当于，报听的优先级比其他玩家的吃碰杠胡都要低
			// if (this.getRuleValue(GameConstants.GAME_RULE_HENAN_BAO_TING) ==
			// 1 && this._playerStatus[_out_card_player].is_bao_ting() == false)
			// {
			// this._playerStatus[_out_card_player]._hu_card_count =
			// this.get_xc_ting_card(_out_card_player,
			// this._playerStatus[_out_card_player]._hu_cards,
			// this.GRR._cards_index[_out_card_player],
			// this.GRR._weave_items[_out_card_player],
			// this.GRR._weave_count[_out_card_player]);
			// int ting_count =
			// this._playerStatus[_out_card_player]._hu_card_count;
			// if (ting_count > 0) {
			// this._playerStatus[_out_card_player].add_action(GameConstants.WIK_BAO_TING);
			// bAroseAction = true;
			// }
			// }
		}

		if (bAroseAction) {
			_resume_player = _current_player;
			_current_player = GameConstants.INVALID_SEAT;
			_provide_player = seat_index;
		} else {
			return false;
		}

		return true;
	}

	// 信阳麻将
	public boolean estimate_player_out_card_respond_henan_xy(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		// 用户状态
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {

			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		// int llcard = get_niao_card_num(true, 0);
		int llcard = 0;
		if (has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN)) {// 带混玩法 剩余14张 结算
			llcard = GameConstants.CARD_COUNT_LEFT_HUANGZHUANG;
		}

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		// 动作判断
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			//// 碰牌判断
			int cards_index_temp[] = new int[GameConstants.MAX_INDEX];
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				cards_index_temp[j] = 0;
				cards_index_temp[j] = GRR._cards_index[i][j];
			}
			for (int j = 0; j < GRR._xianchu_count[i]; j++) {
				if (cards_index_temp[_logic.switch_to_card_index(GRR._xianchu_cards[i][j])] > 0) {
					cards_index_temp[_logic.switch_to_card_index(GRR._xianchu_cards[i][j])]--;
				}

			}
			action = _logic.check_peng(cards_index_temp, card);
			if (action != 0 && has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN) && _logic.is_magic_card(card)) {// 鬼牌不能碰
				action = 0;
			}
			if (action != 0) {
				playerStatus.add_action(action);
				playerStatus.add_peng(card, seat_index);
				bAroseAction = true;
			}
			if (GRR._left_card_count > llcard) {
				// 杠牌判断
				action = _logic.estimate_gang_card_out_card(cards_index_temp, card);
				if (action != 0 && has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN) && _logic.is_magic_card(card)) {// 鬼牌不能杠
					action = 0;
				}
				if (action != 0) {
					playerStatus.add_action(GameConstants.WIK_GANG);
					playerStatus.add_gang(card, seat_index, 1);// 加上杠
					bAroseAction = true;
				}
			}

			// 可以胡的情况 判断
			if (_playerStatus[i].is_chi_hu_round()) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card_henan_xy(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						GameConstants.HU_CARD_TYPE_PAOHU);

				// 结果判断
				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
					bAroseAction = true;
				}
			}
			// }
		}

		if (bAroseAction) {
			_resume_player = _current_player;// 保存当前轮到的玩家
			_current_player = GameConstants.INVALID_SEAT;// 有需要操作的玩家。当前玩家为空
			_provide_player = seat_index;

		} else {

			return false;
		}

		return true;

	}

	// 玩家出牌的动作检测
	public boolean estimate_player_out_card_respond_henan(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		// 用户状态
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {

			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		// int llcard = get_niao_card_num(true, 0);
		int llcard = 0;
		if (has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN)) {// 带混玩法 剩余14张 结算
			llcard = GameConstants.CARD_COUNT_LEFT_HUANGZHUANG;
		}

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		// 动作判断
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			//// 碰牌判断
			action = _logic.check_peng(GRR._cards_index[i], card);
			if (action != 0 && has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN) && _logic.is_magic_card(card)) {// 鬼牌不能碰
				action = 0;
			}
			if (action != 0) {
				playerStatus.add_action(action);
				playerStatus.add_peng(card, seat_index);
				bAroseAction = true;
			}
			if (GRR._left_card_count > llcard) {
				// 杠牌判断
				action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
				if (action != 0 && has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN) && _logic.is_magic_card(card)) {// 鬼牌不能杠
					action = 0;
				}
				if (action != 0) {
					playerStatus.add_action(GameConstants.WIK_GANG);
					playerStatus.add_gang(card, seat_index, 1);// 加上杠
					bAroseAction = true;
				}
			}

			// 可以胡的情况 判断
			if (_playerStatus[i].is_chi_hu_round()) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];

				if (is_mj_type(GameConstants.GAME_TYPE_HENAN_XX) || is_mj_type(GameConstants.GAME_TYPE_NEW_XIN_XIANG)) {
					action = analyse_chi_hu_card_henan_xinxiang_new(GRR._cards_index[i], card,
							GameConstants.HU_CARD_TYPE_PAOHU, GRR._weave_items[i], GRR._weave_count[i], chr);
				} else {
					action = analyse_chi_hu_card_henan(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card,
							chr, GameConstants.HU_CARD_TYPE_PAOHU);
				}

				// 结果判断
				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
					bAroseAction = true;
				}
			}
			// }
		}

		if (bAroseAction) {
			_resume_player = _current_player;// 保存当前轮到的玩家
			_current_player = GameConstants.INVALID_SEAT;// 有需要操作的玩家。当前玩家为空
			_provide_player = seat_index;

		} else {

			return false;
		}

		return true;

	}

	public boolean estimate_player_out_card_respond_henan_pds(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		// 用户状态
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {

			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		int llcard = 20;
		if (has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN)) {
			// 带混玩法 剩余14张 结算
			llcard = 14;
		}
		if (has_rule(GameConstants.GAME_RULE_HENAN_LCI)) {
			// 不留底玩法 剩余0张 结算
			llcard = 0;
		}

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		// 动作判断
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			//// 碰牌判断
			if (playerStatus.is_bao_ting() == false) {
				action = _logic.check_peng(GRR._cards_index[i], card);
				if (action != 0 && has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN) && _logic.is_magic_card(card)) {// 鬼牌不能碰
					action = 0;
				}
				if (action != 0) {
					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);
					bAroseAction = true;
				}
			}
			if (GRR._left_card_count > llcard) {
				// 杠牌判断
				action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
				if (action != 0) {
					if ((playerStatus.is_bao_ting() == false)) {
						playerStatus.add_action(GameConstants.WIK_GANG);
						playerStatus.add_gang(card, seat_index, 1);// 加上杠
						bAroseAction = true;
					} else {
						if (check_gang_huan_zhang(GameConstants.GAME_TYPE_HENAN_PDS, i, card) == false) {
							playerStatus.add_action(GameConstants.WIK_GANG);
							playerStatus.add_gang(card, seat_index, 1);// 加上杠
							bAroseAction = true;
						}
					}
				}
			}

			// 可以胡的情况 判断
			if (_playerStatus[i].is_chi_hu_round()
					&& ((!GameDescUtil.has_rule(getGameRuleIndexEx(), GameConstants.GAME_RULE_HENAN_CHU_FENG_BAO_TING))
							|| (GameDescUtil.has_rule(getGameRuleIndexEx(),
									GameConstants.GAME_RULE_HENAN_CHU_FENG_BAO_TING) && playerStatus.is_bao_ting()))) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];

				action = analyse_chi_hu_card_henan(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						GameConstants.HU_CARD_TYPE_PAOHU);

				// 结果判断
				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
					bAroseAction = true;
				}
			}
			// }
		}

		if (bAroseAction) {
			_resume_player = _current_player;// 保存当前轮到的玩家
			_current_player = GameConstants.INVALID_SEAT;// 有需要操作的玩家。当前玩家为空
			_provide_player = seat_index;

		} else {

			return false;
		}

		return true;

	}

	public boolean estimate_player_out_card_respond_henan_zhou_kou(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		// 用户状态
		for (int i = 0; i < getTablePlayerNumber(); i++) {

			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		int llcard = get_niao_card_num(true, 0);
		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		// 动作判断
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			//// 碰牌判断
			if (playerStatus.is_bao_ting() == false) {
				action = _logic.check_peng(GRR._cards_index[i], card);
				if (action != 0) {
					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);
					bAroseAction = true;
				}
			}
			if (GRR._left_card_count > llcard) {
				// 杠牌判断
				action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
				// 有杠而且杠完不换章
				if (action != 0) {
					if ((playerStatus.is_bao_ting() == false)) {
						playerStatus.add_action(GameConstants.WIK_GANG);
						playerStatus.add_gang(card, seat_index, 1);// 加上杠
						bAroseAction = true;
					} else {
						if (check_gang_huan_zhang(GameConstants.GAME_TYPE_HENAN_ZHOU_KOU, i, card) == false) {
							playerStatus.add_action(GameConstants.WIK_GANG);
							playerStatus.add_gang(card, seat_index, 1);// 加上杠
							bAroseAction = true;
						}
					}
				}
			}

			// 红中不能胡
			if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HENAN_HONG_ZHONG_LAI_ZI)) {
				if (card != _logic.switch_to_card_data(_logic.get_magic_card_index(0))) {
					if (_playerStatus[i].is_chi_hu_round()) {
						// 吃胡判断
						ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
						chr.set_empty();
						int cbWeaveCount = GRR._weave_count[i];
						action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
								GameConstants.HU_CARD_TYPE_PAOHU, i);

						// 结果判断
						if (action != 0) {
							_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
							_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
							bAroseAction = true;
						}
					}
				}
			} else {
				if (_playerStatus[i].is_chi_hu_round()) {
					// 吃胡判断
					ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
					chr.set_empty();
					int cbWeaveCount = GRR._weave_count[i];
					action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
							GameConstants.HU_CARD_TYPE_PAOHU, i);

					// 结果判断
					if (action != 0) {
						_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
						_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
						bAroseAction = true;
					}
				}
			}
		}

		if (bAroseAction) {
			_resume_player = _current_player;// 保存当前轮到的玩家
			_current_player = GameConstants.INVALID_SEAT;// 有需要操作的玩家。当前玩家为空
			_provide_player = seat_index;

		} else {

			return false;
		}

		return true;

	}

	// 玩家出牌的动作检测
	public boolean estimate_player_out_card_respond_henan_lygc(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		// 用户状态
		for (int i = 0; i < getTablePlayerNumber(); i++) {

			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		// int llcard = get_niao_card_num(true, 0);
		int llcard = 0;
		if (has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN)) {// 带混玩法 剩余14张 结算
			llcard = GameConstants.CARD_COUNT_LEFT_HUANGZHUANG;
		}

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		// 动作判断
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			//// 碰牌判断
			action = _logic.check_peng(GRR._cards_index[i], card);
			if (action != 0 && has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN) && _logic.is_magic_card(card)) {// 鬼牌不能碰
				action = 0;
			}
			if (action != 0) {
				playerStatus.add_action(action);
				playerStatus.add_peng(card, seat_index);
				bAroseAction = true;
			}
			if (GRR._left_card_count >= llcard) { // 最後一張可以杠
				// 杠牌判断
				action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
				if (action != 0 && has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN) && _logic.is_magic_card(card)) {// 鬼牌不能杠
					action = 0;
				}
				if (action != 0) {
					playerStatus.add_action(GameConstants.WIK_GANG);
					playerStatus.add_gang(card, seat_index, 1);// 加上杠
					if (is_mj_type(GameConstants.GAME_TYPE_NEW_LUO_YANG_GANG_CI)
							|| is_mj_type(GameConstants.GAME_TYPE_HENAN_LYGC)) {
						// 检测杠完之后是否存在次
						int ci_card = this._logic.switch_to_card_data(this._logic.get_ci_card_index());
						// 从手牌移除杠
						int gang_card_num = GRR._cards_index[i][_logic.switch_to_card_index(card)];
						GRR._cards_index[i][_logic.switch_to_card_index(card)] = GameConstants.INVALID_VALUE;
						boolean flag = false;
						int cbCardCount = this._logic.get_card_count_by_index(this.GRR._cards_index[i]);
						if (this.GRR._weave_count[i] == 3 && cbCardCount == 1) { // 次单吊处理
							// 加入杠到组合
							this.GRR._weave_items[i][this.GRR._weave_count[i]].public_card = 1;
							this.GRR._weave_items[i][this.GRR._weave_count[i]].center_card = card;
							this.GRR._weave_items[i][this.GRR._weave_count[i]].weave_kind = GameConstants.WIK_GANG;
							this.GRR._weave_items[i][this.GRR._weave_count[i]].provide_player = i;
							this.GRR._weave_count[i]++;
							flag = true;
						}
						boolean IS_CI = this.estimate_lygc_gang_ci(i, ci_card);
						if (IS_CI) {
							playerStatus.add_action(GameConstants.WIK_LYGC_CI);
							playerStatus.add_lygc_ci(card, i, GameConstants.WIK_LYGC_CI);// 加上杠
						}
						if (flag) {
							// 移除
							this.GRR._weave_count[i]--;
							this.GRR._weave_items[i][this.GRR._weave_count[i]].public_card = 0;
							this.GRR._weave_items[i][this.GRR._weave_count[i]].center_card = GameConstants.INVALID_CARD;
							this.GRR._weave_items[i][this.GRR._weave_count[i]].weave_kind = GameConstants.WIK_NULL;
							this.GRR._weave_items[i][this.GRR._weave_count[i]].provide_player = GameConstants.INVALID_VALUE;
						}
						// 加入杠到手牌
						GRR._cards_index[i][_logic.switch_to_card_index(card)] = gang_card_num;
						bAroseAction = true;
					}
				}
			}

			// 可以胡的情况 判断
			if (_playerStatus[i].is_chi_hu_round() && has_rule(GameConstants.GAME_RULE_HENAN_HENAN_PAO_HU)) { // WalkerGeek
																												// 只有选择可炮胡才能出牌状态接胡
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card_henan_lygc_new(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount,
						card, chr, GameConstants.HU_CARD_TYPE_PAOHU);

				// 结果判断
				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
					bAroseAction = true;
				}
			}
			// }
		}

		if (bAroseAction) {
			// _resume_player = _current_player;// 保存当前轮到的玩家
			// _current_player = GameConstants.INVALID_SEAT;// 有需要操作的玩家。当前玩家为空
			_provide_player = seat_index;
		} else {
			return false;
		}

		return true;

	}

	/**
	 * 检测杆后有没有次牌操作
	 * 
	 * @param seat_index
	 * @param card
	 * @return
	 */
	public boolean estimate_lygc_gang_ci(int seat_index, int card) {
		int action = GameConstants.WIK_NULL;
		boolean bAroseAction = false;// 出现(是否)有
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];// playerStatus._chiHuRight;
		chr.set_empty();
		int cbWeaveCount = GRR._weave_count[seat_index];
		action = analyse_chi_hu_card_henan_lygc_new(GRR._cards_index[seat_index], GRR._weave_items[seat_index],
				cbWeaveCount, card, chr, GameConstants.HU_CARD_TYPE_GANG_CI);

		// 结果判断
		if (action != 0) {
			// _playerStatus[seat_index].add_action(GameConstants.WIK_LYGC_CI);
			// _playerStatus[seat_index].add_lygc_ci(card, seat_index);// 次组合
			bAroseAction = true;
		}

		if (bAroseAction) {
			this.GRR._chi_hu_rights[seat_index].opr_or(GameConstants.CHR_HENAN_GANG_CI);
			// _resume_player = _current_player;// 保存当前轮到的玩家
			// _current_player = GameConstants.INVALID_SEAT;// 有需要操作的玩家。当前玩家为空
			_provide_player = seat_index;
		} else {
			return false;
		}
		return true;
	}

	// 玩家出牌的动作检测
	public boolean estimate_player_out_card_respond_hz(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		// 用户状态
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {

			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		int llcard = get_niao_card_num(true, 0);
		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		// 动作判断
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			//// 碰牌判断
			action = _logic.check_peng(GRR._cards_index[i], card);
			if (action != 0) {
				playerStatus.add_action(action);
				playerStatus.add_peng(card, seat_index);
				bAroseAction = true;
			}
			if (GRR._left_card_count > llcard) {
				// 杠牌判断
				action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
				if (action != 0) {
					playerStatus.add_action(GameConstants.WIK_GANG);
					playerStatus.add_gang(card, seat_index, 1);// 加上杠
					bAroseAction = true;
				}
			}

			// 红中不能胡
			if (card != _logic.switch_to_card_data(_logic.get_magic_card_index(0))) {
				// 可以胡的情况 判断
				if (_playerStatus[i].is_chi_hu_round()) {
					int magic_count = _logic.magic_count(GRR._cards_index[i]);
					if (magic_count == 0 || !GameDescUtil.has_rule(gameRuleIndexEx,
							GameConstants.GAME_RULE_HUNAN_HONG_ZHONG_BU_JIE_PAO)) {
						// 吃胡判断
						ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
						chr.set_empty();
						int cbWeaveCount = GRR._weave_count[i];
						action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
								GameConstants.HU_CARD_TYPE_PAOHU, i);

						// 结果判断
						if (action != 0) {
							_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
							_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
							bAroseAction = true;
						}
					}
				}
			}
			// }
		}

		if (bAroseAction) {
			_resume_player = _current_player;// 保存当前轮到的玩家
			_current_player = GameConstants.INVALID_SEAT;// 有需要操作的玩家。当前玩家为空
			_provide_player = seat_index;

		} else {

			return false;
		}

		return true;

	}

	// 玩家出牌的动作检测
	public boolean estimate_player_out_card_respond_pxzz(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		// 用户状态
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {

			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		int llcard = get_niao_card_num(true, 0);
		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		// 动作判断
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			//// 碰牌判断
			action = _logic.check_peng(GRR._cards_index[i], card);
			if (action != 0) {
				playerStatus.add_action(action);
				playerStatus.add_peng(card, seat_index);
				bAroseAction = true;
			}
			if (GRR._left_card_count > llcard) {
				// 杠牌判断
				action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
				if (action != 0) {
					playerStatus.add_action(GameConstants.WIK_GANG);
					playerStatus.add_gang(card, seat_index, 1);// 加上杠
					bAroseAction = true;
				}
			}

			// 可以胡的情况 判断
			if (_playerStatus[i].is_chi_hu_round()) {
				int magic_count = _logic.magic_count(GRR._cards_index[i]);
				if (magic_count == 0 || !GameDescUtil.has_rule(gameRuleIndexEx,
						GameConstants.GAME_RULE_HUNAN_HONG_ZHONG_BU_JIE_PAO)) {
					// 吃胡判断
					ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
					chr.set_empty();
					int cbWeaveCount = GRR._weave_count[i];
					action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
							GameConstants.HU_CARD_TYPE_PAOHU, i);

					// 结果判断
					if (action != 0) {
						_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
						_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
						bAroseAction = true;
					}
				}
			}

			// }
		}

		if (bAroseAction) {
			_resume_player = _current_player;// 保存当前轮到的玩家
			_current_player = GameConstants.INVALID_SEAT;// 有需要操作的玩家。当前玩家为空
			_provide_player = seat_index;

		} else {

			return false;
		}

		return true;

	}

	/**
	 * 河南红中麻将出牌检测
	 * 
	 * @param seat_index
	 * @param card
	 * @return
	 */
	public boolean estimate_player_out_card_respond_hnhz(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		// 用户状态
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {

			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		int llcard = get_niao_card_num(true, 0);
		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		// 动作判断
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			//// 碰牌判断
			action = _logic.check_peng(GRR._cards_index[i], card);
			if (action != 0) {
				playerStatus.add_action(action);
				playerStatus.add_peng(card, seat_index);
				bAroseAction = true;
			}
			if (GRR._left_card_count > llcard) {
				// 杠牌判断
				action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
				if (action != 0) {
					playerStatus.add_action(GameConstants.WIK_GANG);
					playerStatus.add_gang(card, seat_index, 1);// 加上杠
					bAroseAction = true;
				}
			}

			// 红中不能胡
			if (card != _logic.switch_to_card_data(_logic.get_magic_card_index(0))) {
				// 可以胡的情况 判断
				if (_playerStatus[i].is_chi_hu_round()) {
					// 吃胡判断
					ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
					chr.set_empty();
					int cbWeaveCount = GRR._weave_count[i];
					action = this.analyse_chi_hu_card_hnhz(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card,
							chr, GameConstants.HU_CARD_TYPE_PAOHU);

					// 结果判断
					if (action != 0) {
						_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
						_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
						bAroseAction = true;
					}
				}
			}
			// }
		}

		if (bAroseAction) {
			_resume_player = _current_player;// 保存当前轮到的玩家
			_current_player = GameConstants.INVALID_SEAT;// 有需要操作的玩家。当前玩家为空
			_provide_player = seat_index;

		} else {

			return false;
		}

		return true;

	}

	// 玩家出牌的动作检测
	public boolean estimate_player_out_card_respond_lxcg(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		// 用户状态
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {

			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		// 动作判断
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			//// 碰牌判断
			action = _logic.check_peng(GRR._cards_index[i], card);
			if (action != 0) {
				playerStatus.add_action(action);
				playerStatus.add_peng(card, seat_index);
				bAroseAction = true;
			}

			if (GRR._left_card_count > 0) {
				// 杠牌判断
				action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
				if (action != 0) {
					playerStatus.add_action(GameConstants.WIK_BU_ZHNAG);
					playerStatus.add_bu_zhang(card, seat_index, 1);// 加上杠
					bAroseAction = true;
				}
			}

			// 可以胡的情况 判断
			if (_playerStatus[i].is_chi_hu_round()) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						GameConstants.HU_CARD_TYPE_PAOHU, i);

				// 结果判断
				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
					bAroseAction = true;
				}
			}
			// }
		}

		if (bAroseAction) {
			_resume_player = _current_player;// 保存当前轮到的玩家
			_current_player = GameConstants.INVALID_SEAT;// 有需要操作的玩家。当前玩家为空
			_provide_player = seat_index;

		} else {

			return false;
		}

		return true;

	}

	// 玩家出牌的动作检测
	public boolean estimate_player_out_card_respond_zz(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		// 用户状态
		for (int i = 0; i < getTablePlayerNumber(); i++) {

			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		// 动作判断
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			//// 碰牌判断
			action = _logic.check_peng(GRR._cards_index[i], card);
			if (action != 0) {
				playerStatus.add_action(action);
				playerStatus.add_peng(card, seat_index);
				bAroseAction = true;
			}

			if (GRR._left_card_count > 0) {
				// 杠牌判断
				action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
				if (action != 0) {
					playerStatus.add_action(GameConstants.WIK_GANG);
					playerStatus.add_gang(card, seat_index, 1);// 加上杠
					bAroseAction = true;
				}
			}

			// 可以胡的情况 判断
			if (_playerStatus[i].is_chi_hu_round()) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						GameConstants.HU_CARD_TYPE_PAOHU, i);

				// 结果判断
				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
					bAroseAction = true;
				}
			}
			// }
		}

		if (bAroseAction) {
			_resume_player = _current_player;// 保存当前轮到的玩家
			_current_player = GameConstants.INVALID_SEAT;// 有需要操作的玩家。当前玩家为空
			_provide_player = seat_index;

		} else {

			return false;
		}

		return true;

	}

	/**
	 * 仙桃晃晃 //玩家出牌的动作检测
	 * 
	 * @param seat_index
	 * @param card
	 * @return
	 */
	public boolean estimate_player_out_card_respond_xthh(int seat_index, int card, int type) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		// 用户状态
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {

			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		// 动作判断
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			//// 碰牌判断
			action = _logic.check_peng_xthh(GRR._cards_index[i], card);
			if (action != 0) {
				playerStatus.add_action(action);
				playerStatus.add_peng(card, seat_index);
				bAroseAction = true;
			}

			if (GRR._left_card_count > 0) {
				// 杠牌判断
				action = _logic.estimate_gang_card_out_card_xthh(GRR._cards_index[i], card);
				if (action != 0) {
					playerStatus.add_action(action);
					playerStatus.add_xiao(card, action, seat_index, 1);// 加上笑
					bAroseAction = true;
				}
			}

			// 可以胡的情况 判断
			if (_playerStatus[i].is_chi_hu_round()) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr, type,
						i);

				// 结果判断
				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
					bAroseAction = true;
				}
			}
			// }
		}

		if (bAroseAction) {
			_resume_player = _current_player;// 保存当前轮到的玩家
			_current_player = GameConstants.INVALID_SEAT;// 有需要操作的玩家。当前玩家为空
			_provide_player = seat_index;

		} else {

			return false;
		}

		return true;

	}

	/**
	 * 安阳麻将出牌检测
	 * 
	 * @param seat_index
	 * @param card
	 * @return
	 */
	public boolean estimate_player_out_card_respond_ay(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		// 用户状态
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {

			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		// 动作判断
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			//// 碰牌判断
			if (playerStatus.is_bao_ting() == false) {
				action = _logic.check_peng(GRR._cards_index[i], card);
				if (action != 0) {
					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);
					bAroseAction = true;
				}
			}

			if (GRR._left_card_count > 0) {
				// 杠牌判断
				action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
				// 有杠而且杠完不换章
				if (action != 0) {
					if ((playerStatus.is_bao_ting() == false)) {
						playerStatus.add_action(GameConstants.WIK_GANG);
						playerStatus.add_gang(card, i, 1);// 加上杠
						bAroseAction = true;
					} else {
						if (check_gang_huan_zhang(GameConstants.GAME_TYPE_HENAN_AY, i, card) == false) {
							playerStatus.add_action(GameConstants.WIK_GANG);
							playerStatus.add_gang(card, i, 1);// 加上杠
							bAroseAction = true;
						}

					}
				}
			}

			// 可以胡的情况 判断
			if (_playerStatus[i].is_chi_hu_round()) {
				if (_playerStatus[seat_index].is_bao_ting() == false || _playerStatus[i].is_bao_ting() == false) {
					continue;
				}
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card_ay(i, GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						GameConstants.HU_CARD_TYPE_PAOHU);

				// 结果判断
				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合

					bAroseAction = true;
				}
			}
			// }
		}

		if (bAroseAction) {
			_resume_player = _current_player;// 保存当前轮到的玩家
			_current_player = GameConstants.INVALID_SEAT;// 有需要操作的玩家。当前玩家为空
			_provide_player = seat_index;

		} else {

			return false;
		}

		return true;

	}

	/**
	 * 林州麻将出牌检测
	 * 
	 * @param seat_index
	 * @param card
	 * @return
	 */
	public boolean estimate_player_out_card_respond_lz(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		// 用户状态
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {

			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		// 动作判断
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			//// 碰牌判断
			action = _logic.check_peng(GRR._cards_index[i], card);
			if (action != 0) {
				playerStatus.add_action(action);
				playerStatus.add_peng(card, seat_index);
				bAroseAction = true;
			}

			if (GRR._left_card_count > 0) {
				// 杠牌判断
				action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
				// 有杠而且杠完不换章
				if (action != 0) {
					playerStatus.add_action(GameConstants.WIK_GANG);
					playerStatus.add_gang(card, i, 1);// 加上杠
					bAroseAction = true;
				}
			}

			// 可以胡的情况 判断
			if (_playerStatus[i].is_chi_hu_round()) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card_lz(i, GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						GameConstants.HU_CARD_TYPE_PAOHU);

				// 结果判断
				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合

					bAroseAction = true;
				}
			}
			// }
		}

		if (bAroseAction) {
			_resume_player = _current_player;// 保存当前轮到的玩家
			_current_player = GameConstants.INVALID_SEAT;// 有需要操作的玩家。当前玩家为空
			_provide_player = seat_index;

		} else {

			return false;
		}

		return true;

	}

	/**
	 * //玩家出牌的动作检测
	 * 
	 * private boolean estimate_player_dispath_respond(int seat_index, int
	 * card){ // 变量定义 boolean bAroseAction = false;// 出现(是否)有
	 * 
	 * // 用户状态 for (int i = 0; i < MJGameConstants.GAME_PLAYER; i++) {
	 * 
	 * _playerStatus[i].clean_action(); _playerStatus[i].clean_weave(); }
	 * 
	 * PlayerStatus playerStatus = null;
	 * 
	 * int action = MJGameConstants.WIK_NULL;
	 * 
	 * // 动作判断 for (int i = 0; i < MJGameConstants.GAME_PLAYER; i++) { // 用户过滤
	 * if (seat_index == i) continue;
	 * 
	 * playerStatus = _playerStatus[i]; //// 碰牌判断 action =
	 * _logic.check_peng(GRR._cards_index[i], card); if(action!=0){
	 * playerStatus.add_action(action); playerStatus.add_peng(card,seat_index);
	 * }
	 * 
	 * 
	 * // 杠牌判断 如果剩余牌大于1，是否有杠 if (GRR._left_card_count > 0) { action =
	 * _logic.estimate_gang_card(GRR._cards_index[i], card);
	 * 
	 * if(action!=0){ playerStatus.add_action(MJGameConstants.WIK_BU_ZHNAG);
	 * playerStatus.add_bu_zhang(card, seat_index, 1);//加上补涨
	 * 
	 * //长沙麻将判断是不是听牌 if(is_mj_type(MJGameConstants.GAME_TYPE_CS) &&
	 * action==MJGameConstants.WIK_NULL){ boolean is_ting =
	 * _logic.is_cs_ting_card(GRR._cards_index[i], GRR._weave_items[i],
	 * GRR._weave_count[i]);
	 * 
	 * if(is_ting==true){ playerStatus.add_action(MJGameConstants.WIK_GANG);
	 * playerStatus.add_gang(card, seat_index, 1);//加上杠
	 * 
	 * } } } }
	 * 
	 * // 吃胡判断 ChiHuRight chr =
	 * GRR._chi_hu_rights[i];//playerStatus._chiHuRight; chr.set_empty(); int
	 * cbWeaveCount = GRR._weave_count[i]; action =
	 * analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i],
	 * cbWeaveCount, card,chr );
	 * 
	 * // 结果判断 if (action!=0){
	 * _playerStatus[i].add_action(MJGameConstants.WIK_CHI_HU);
	 * _playerStatus[i].add_chi_hu(card,seat_index);//吃胡的组合 bAroseAction = true;
	 * } }
	 * 
	 * int chi_seat_index = (seat_index+1)%MJGameConstants.GAME_PLAYER; //
	 * 长沙麻将吃操作 转转麻将不能吃 if (_game_type_index == MJGameConstants.GAME_TYPE_CS) {
	 * // 这里可能有问题 应该是 |= action =
	 * _logic.check_chi(GRR._cards_index[chi_seat_index], card);
	 * if((action&MJGameConstants.WIK_LEFT) !=0){
	 * _playerStatus[chi_seat_index].add_action(MJGameConstants.WIK_LEFT);
	 * _playerStatus[chi_seat_index].add_chi(card,MJGameConstants.WIK_LEFT,seat_index);
	 * } if((action&MJGameConstants.WIK_CENTER) !=0){
	 * _playerStatus[chi_seat_index].add_action(MJGameConstants.WIK_CENTER);
	 * _playerStatus[chi_seat_index].add_chi(card,MJGameConstants.WIK_CENTER,seat_index);
	 * } if((action&MJGameConstants.WIK_RIGHT) !=0){
	 * _playerStatus[chi_seat_index].add_action(MJGameConstants.WIK_RIGHT);
	 * _playerStatus[chi_seat_index].add_chi(card,MJGameConstants.WIK_RIGHT,seat_index);
	 * }
	 * 
	 * // 结果判断 if (_playerStatus[chi_seat_index].has_action()){ bAroseAction =
	 * true; } }
	 * 
	 * if(bAroseAction){ _resume_player = _current_player;// 保存当前轮到的玩家
	 * _current_player = MJGameConstants.INVALID_SEAT;// 有需要操作的玩家。当前玩家为空
	 * _provide_player = seat_index;
	 * 
	 * for (int i = 0; i < MJGameConstants.GAME_PLAYER; i++) { playerStatus =
	 * _playerStatus[i]; if(playerStatus.has_action()){
	 * playerStatus.set_status(MJGameConstants.Player_Status_OPR_CARD);//操作状态
	 * 
	 * 
	 * //playerStatus._provide_card = card;
	 * 
	 * this.operate_player_action(i, false); } } }else{
	 * 
	 * return false; }
	 * 
	 * 
	 * return true;
	 * 
	 * }
	 **/

	// 检查杠牌,有没有胡的
	public boolean estimate_gang_respond_zhuzhou(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		// 动作判断
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];
			// 可以胡的情况 判断
			if (playerStatus.is_chi_hu_round()) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						GameConstants.HU_CARD_TYPE_QIANGGANG, i);

				// 结果判断
				if (action != 0) {
					// chr.opr_or(MJGameConstants.CHR_QIANG_GANG_HU);// 抢杠胡
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
					bAroseAction = true;
				}
			}
		}

		if (bAroseAction == true) {
			_provide_player = seat_index;// 谁打的牌
			_provide_card = card;// 打的什么牌
			_resume_player = _current_player;// 保存当前轮到的玩家
			_current_player = GameConstants.INVALID_SEAT;// 有需要操作的玩家。当前玩家为空
		}

		return bAroseAction;
	}

	/***
	 * 检查杠牌,有没有胡的 检查所有人
	 * 
	 * @param seat_index
	 * @param card
	 * @return
	 */
	public boolean estimate_gang_respond_henan(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		// 动作判断
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];
			// 可以胡的情况 判断
			if (playerStatus.is_chi_hu_round()) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card_henan(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						GameConstants.HU_CARD_TYPE_QIANGGANG);

				// 结果判断
				if (action != 0) {
					chr.opr_or(GameConstants.CHR_HUNAN_QIANG_GANG_HU);// 抢杠胡
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
					bAroseAction = true;
				}
			}
		}

		if (bAroseAction == true) {
			_provide_player = seat_index;// 谁打的牌
			_provide_card = card;// 打的什么牌
			_resume_player = _current_player;// 保存当前轮到的玩家
			_current_player = GameConstants.INVALID_SEAT;// 有需要操作的玩家。当前玩家为空
		}

		return bAroseAction;
	}

	public boolean estimate_gang_respond_henan_lygc(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		// 动作判断
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];
			// 可以胡的情况 判断
			if (playerStatus.is_chi_hu_round()) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card_henan(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						GameConstants.HU_CARD_TYPE_QIANGGANG);

				// 结果判断
				if (action != 0) {
					chr.opr_or(GameConstants.CHR_HENAN_QIANG_GANG_HU);// 抢杠胡
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
					bAroseAction = true;
				}
			}
		}

		if (bAroseAction == true) {
			_provide_player = seat_index;// 谁打的牌
			_provide_card = card;// 打的什么牌
			_resume_player = _current_player;// 保存当前轮到的玩家
			_current_player = GameConstants.INVALID_SEAT;// 有需要操作的玩家。当前玩家为空
		}

		return bAroseAction;
	}

	/***
	 * 检查杠牌,有没有胡的 检查所有人
	 * 
	 * @param seat_index
	 * @param card
	 * @return
	 */
	public boolean estimate_gang_respond_henan_xy(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		// 动作判断
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];
			// 可以胡的情况 判断
			if (playerStatus.is_chi_hu_round()) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card_henan_xy(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						GameConstants.HU_CARD_TYPE_PAOHU);

				// 结果判断
				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
					bAroseAction = true;
				}
			}
		}

		if (bAroseAction == true) {
			_provide_player = seat_index;// 谁打的牌
			_provide_card = card;// 打的什么牌
			_resume_player = _current_player;// 保存当前轮到的玩家
			_current_player = GameConstants.INVALID_SEAT;// 有需要操作的玩家。当前玩家为空
		}

		return bAroseAction;
	}

	/***
	 * 检查杠牌,有没有胡的 检查所有人
	 * 
	 * @param seat_index
	 * @param card
	 * @return
	 */
	public boolean estimate_gang_respond_henan_lh(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		// 动作判断
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];
			// 可以胡的情况 判断
			if (playerStatus.is_chi_hu_round()) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card_henan_lh(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						GameConstants.HU_CARD_TYPE_QIANGGANG);

				// 结果判断
				if (action != 0) {
					chr.opr_or(GameConstants.CHR_HENAN_QIANG_GANG_HU);// 抢杠胡
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
					bAroseAction = true;
				}
			}
		}

		if (bAroseAction == true) {
			_provide_player = seat_index;// 谁打的牌
			_provide_card = card;// 打的什么牌
			_resume_player = _current_player;// 保存当前轮到的玩家
			_current_player = GameConstants.INVALID_SEAT;// 有需要操作的玩家。当前玩家为空
		}

		return bAroseAction;
	}

	/***
	 * 河南红中麻将检查杠牌,有没有胡的 检查所有人
	 * 
	 * @param seat_index
	 * @param card
	 * @return
	 */
	public boolean estimate_gang_respond_hnhz(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		// 动作判断
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];
			// 可以胡的情况 判断
			if (playerStatus.is_chi_hu_round()) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card_hnhz(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						GameConstants.HU_CARD_TYPE_QIANGGANG);

				// 结果判断
				if (action != 0) {
					chr.opr_or(GameConstants.CHR_HENAN_QIANG_GANG_HU);// 抢杠胡
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
					bAroseAction = true;
				}
			}
		}

		if (bAroseAction == true) {
			_provide_player = seat_index;// 谁打的牌
			_provide_card = card;// 打的什么牌
			_resume_player = _current_player;// 保存当前轮到的玩家
			_current_player = GameConstants.INVALID_SEAT;// 有需要操作的玩家。当前玩家为空
		}

		return bAroseAction;
	}

	/***
	 * 湖南麻将杠牌检测
	 * 
	 * @param seat_index
	 * @param card
	 * @return
	 */
	@Override
	public boolean estimate_gang_respond(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		// 动作判断
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];
			// 可以胡的情况 判断
			if (playerStatus.is_chi_hu_round()) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						GameConstants.HU_CARD_TYPE_QIANGGANG, i);

				// 结果判断
				if (action != 0) {
					chr.opr_or(GameConstants.CHR_HUNAN_QIANG_GANG_HU);// 抢杠胡
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
					bAroseAction = true;
				}
			}
		}

		if (bAroseAction == true) {
			_provide_player = seat_index;// 谁打的牌
			_provide_card = card;// 打的什么牌
			_resume_player = _current_player;// 保存当前轮到的玩家
			_current_player = GameConstants.INVALID_SEAT;// 有需要操作的玩家。当前玩家为空
		}

		return bAroseAction;
	}

	public boolean estimate_gang_respond_henan_zhou_kou(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		if (!GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HENAN_HENAN_PAO_HU)) {
			return false;
		}

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		// 动作判断
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];
			// 可以胡的情况 判断
			if (playerStatus.is_chi_hu_round()) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						GameConstants.HU_CARD_TYPE_QIANGGANG, i);

				// 结果判断
				if (action != 0) {
					chr.opr_or(GameConstants.CHR_HENAN_QIANG_GANG_HU);// 抢杠胡
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
					bAroseAction = true;
				}
			}
		}

		if (bAroseAction == true) {
			_provide_player = seat_index;// 谁打的牌
			_provide_card = card;// 打的什么牌
			_resume_player = _current_player;// 保存当前轮到的玩家
			_current_player = GameConstants.INVALID_SEAT;// 有需要操作的玩家。当前玩家为空
		}

		return bAroseAction;
	}

	// 检查杠牌,有没有胡的
	public boolean estimate_gang_respond_cs(int seat_index, int card, int _action) {
		// 变量定义
		boolean isGang = _action == GameConstants.WIK_GANG ? true : false;// 补张
																			// 不算抢杠胡

		boolean bAroseAction = false;// 出现(是否)有

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		// 动作判断
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];
			// 可以胡的情况 判断
			// if(playerStatus.is_chi_hu_round()){
			// 吃胡判断
			ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
			chr.set_empty();
			int cbWeaveCount = GRR._weave_count[i];
			action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
					GameConstants.HU_CARD_TYPE_QIANGGANG, i);

			// 结果判断
			if (action != 0) {
				if (isGang) {
					chr.opr_or(GameConstants.CHR_HUNAN_QIANG_GANG_HU);// 抢杠胡
				}
				_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
				_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
				bAroseAction = true;
			}
			// }
		}

		if (bAroseAction == true) {
			_provide_player = seat_index;// 谁打的牌
			_provide_card = card;// 打的什么牌
			_resume_player = _current_player;// 保存当前轮到的玩家
			_current_player = GameConstants.INVALID_SEAT;// 有需要操作的玩家。当前玩家为空
		}

		return bAroseAction;
	}

	/**
	 * 仙桃晃晃 //检查杠牌,有没有胡的
	 * 
	 * @param seat_index
	 * @param card
	 * @return
	 */
	public boolean estimate_gang_xthh_respond(int seat_index, int card) {
		if (GRR._piao_lai_count > 0) {
			return false;
		}

		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		// 动作判断
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			// 有癞子不能抢杠胡
			if (GRR._cards_index[i][_logic.get_magic_card_index(0)] > 0) {
				continue;
			}

			playerStatus = _playerStatus[i];
			// 可以胡的情况 判断
			// if(playerStatus.is_chi_hu_round()){
			// 吃胡判断
			ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
			chr.set_empty();
			int cbWeaveCount = GRR._weave_count[i];
			action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
					GameConstants.HU_CARD_TYPE_QIANGGANG, i);

			// 结果判断
			if (action != 0) {
				_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
				_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
				bAroseAction = true;
			}
			// }
		}

		if (bAroseAction == true) {
			_provide_player = seat_index;// 谁打的牌
			_provide_card = card;// 打的什么牌
			_resume_player = _current_player;// 保存当前轮到的玩家
			_current_player = GameConstants.INVALID_SEAT;// 有需要操作的玩家。当前玩家为空
		}

		return bAroseAction;
	}

	/**
	 * 许昌麻将，检查这个杠有没有胡
	 * 
	 * @param seat_index
	 * @param card
	 * @return
	 */
	public boolean estimate_gang_xc_respond(int seat_index, int card) {
		boolean bAroseAction = false;

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			action = GameConstants.WIK_NULL;

			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			if (playerStatus.is_chi_hu_round()) {
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				for (int k = 0; k < this._playerStatus[i]._hu_card_count; k++) {
					if (card == this._playerStatus[i]._hu_cards[k]) {
						xc_analyse_type = XC_ANALYSE_NORMAL;

						action = analyse_chi_hu_card_henan_xc(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount,
								card, chr, GameConstants.HU_CARD_TYPE_QIANGGANG, true);
						break;
					}
				}
				if (action != 0) {
					chr.opr_or(GameConstants.CHR_HUNAN_QIANG_GANG_HU);
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);
					bAroseAction = true;
				}
			}
		}
		return bAroseAction;
	}

	/**
	 * 安阳麻将，检查这个杠有没有胡
	 * 
	 * @param seat_index
	 * @param card
	 * @return
	 */
	public boolean estimate_gang_ay_respond(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		// 动作判断
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			// 用户过滤
			if (seat_index == i)
				continue;
			if (_playerStatus[seat_index].is_bao_ting() == false || _playerStatus[i].is_bao_ting() == false) {
				continue;
			}

			playerStatus = _playerStatus[i];
			// 可以胡的情况 判断
			if (playerStatus.is_chi_hu_round()) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card_ay(i, GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						GameConstants.HU_CARD_TYPE_QIANGGANG);

				// 结果判断
				if (action != 0) {
					chr.opr_or(GameConstants.CHR_HUNAN_QIANG_GANG_HU);// 抢杠胡
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
					bAroseAction = true;
				}
			}
		}

		if (bAroseAction == true) {
			_provide_player = seat_index;// 谁打的牌
			_provide_card = card;// 打的什么牌
			_resume_player = _current_player;// 保存当前轮到的玩家
			_current_player = GameConstants.INVALID_SEAT;// 有需要操作的玩家。当前玩家为空
		}

		return bAroseAction;
	}

	/**
	 * 林州麻将，检查这个杠有没有胡
	 * 
	 * @param seat_index
	 * @param card
	 * @return
	 */
	public boolean estimate_gang_lz_respond(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		// 动作判断
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];
			// 可以胡的情况 判断
			if (playerStatus.is_chi_hu_round()) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card_lz(i, GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						GameConstants.HU_CARD_TYPE_QIANGGANG);

				// 结果判断
				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
					bAroseAction = true;
				}
			}
		}

		if (bAroseAction == true) {
			_provide_player = seat_index;// 谁打的牌
			_provide_card = card;// 打的什么牌
			_resume_player = _current_player;// 保存当前轮到的玩家
			_current_player = GameConstants.INVALID_SEAT;// 有需要操作的玩家。当前玩家为空
		}

		return bAroseAction;
	}

	// 检查长沙麻将临湘,杠牌
	public boolean estimate_gang_cs_lx_respond(int seat_index, int provider, int card, boolean d, boolean check_chi) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		playerStatus = _playerStatus[seat_index];

		// 吃胡判断
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];// playerStatus._chiHuRight;
		// chr.set_empty();
		int cbWeaveCount = GRR._weave_count[seat_index];
		action = analyse_chi_hu_card(GRR._cards_index[seat_index], GRR._weave_items[seat_index], cbWeaveCount, card,
				chr, GameConstants.HU_CARD_TYPE_PAOHU, seat_index);

		// 结果判断
		if (action != 0) {
			// if (d) {
			// chr.opr_or(GameConstants.CHR_HUNAN_SHUANG_GANG_SHANG_PAO);
			// } else {
			// chr.opr_or(GameConstants.CHR_HUNAN_GANG_SHANG_PAO);
			// }
			if (_playerStatus[seat_index].has_chi_hu() == false) {
				_playerStatus[seat_index].add_action(GameConstants.WIK_CHI_HU);
				_playerStatus[seat_index].add_chi_hu(card, provider);// 吃胡的组合
			}

			bAroseAction = true;
		}

		return bAroseAction;
	}

	// 检查长沙麻将,杠牌
	public boolean estimate_gang_cs_respond(int seat_index, int provider, int card, boolean d, boolean check_chi) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		playerStatus = _playerStatus[seat_index];
		// playerStatus.clean_action();

		if (playerStatus.lock_huan_zhang() == false) {
			//// 碰牌判断
			action = _logic.check_peng(GRR._cards_index[seat_index], card);
			if (action != 0) {
				playerStatus.add_action(action);
				playerStatus.add_peng(card, seat_index);

				bAroseAction = true;
			}

		}

		// 杠牌判断 如果剩余牌大于1，是否有杠
		if (GRR._left_card_count > 1) {
			if (GRR._cards_index[seat_index][_logic.switch_to_card_index(card)] == 3) {
				playerStatus.add_action(GameConstants.WIK_BU_ZHNAG);
				playerStatus.add_bu_zhang(card, provider, 1);// 加上补涨

				if (GRR._left_card_count > 2) {
					// int bu_index = _logic.switch_to_card_index(card);
					// int save_count = GRR._cards_index[seat_index][bu_index];
					// GRR._cards_index[seat_index][bu_index]=0;
					// 长沙麻将判断是不是听牌
					// boolean is_ting =
					// is_cs_ting_card(GRR._cards_index[seat_index],
					// GRR._weave_items[seat_index],
					// GRR._weave_count[seat_index]);
					boolean is_ting = false;
					// 把牌加回来
					// GRR._cards_index[seat_index][bu_index] = save_count;
					// 把牌加回来
					// GRR._cards_index[i][bu_index]=save_count;
					boolean can_gang = false;
					if (is_ting == true) {
						can_gang = true;
					} else {
						// 把可以杠的这张牌去掉。看是不是听牌
						int bu_index = _logic.switch_to_card_index(card);
						int save_count = GRR._cards_index[seat_index][bu_index];
						GRR._cards_index[seat_index][bu_index] = 0;

						int cbWeaveIndex = GRR._weave_count[seat_index];

						GRR._weave_items[seat_index][cbWeaveIndex].public_card = 1;
						GRR._weave_items[seat_index][cbWeaveIndex].center_card = card;
						GRR._weave_items[seat_index][cbWeaveIndex].weave_kind = GameConstants.WIK_GANG;// 接杠
						GRR._weave_items[seat_index][cbWeaveIndex].provide_player = provider;
						GRR._weave_count[seat_index]++;

						can_gang = this.is_cs_ting_card(GRR._cards_index[seat_index], GRR._weave_items[seat_index],
								GRR._weave_count[seat_index], seat_index);

						GRR._weave_count[seat_index] = cbWeaveIndex;
						GRR._cards_index[seat_index][bu_index] = save_count;
					}

					if (can_gang == true) {

						playerStatus.add_action(GameConstants.WIK_GANG);
						playerStatus.add_gang(card, provider, 1);// 加上杠

					}
				}

				bAroseAction = true;
			}

		}

		// 吃胡判断
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];// playerStatus._chiHuRight;
		// chr.set_empty();
		int cbWeaveCount = GRR._weave_count[seat_index];
		action = analyse_chi_hu_card(GRR._cards_index[seat_index], GRR._weave_items[seat_index], cbWeaveCount, card,
				chr, GameConstants.HU_CARD_TYPE_PAOHU, seat_index);

		// 结果判断
		if (action != 0) {
			if (d) {
				chr.opr_or(GameConstants.CHR_HUNAN_SHUANG_GANG_SHANG_PAO);
			} else {
				chr.opr_or(GameConstants.CHR_HUNAN_GANG_SHANG_PAO);
			}
			if (_playerStatus[seat_index].has_chi_hu() == false) {
				_playerStatus[seat_index].add_action(GameConstants.WIK_CHI_HU);
				_playerStatus[seat_index].add_chi_hu(card, provider);// 吃胡的组合
			}

			bAroseAction = true;
		}

		if (check_chi) {
			// int chi_seat_index = (provider + 1) % getTablePlayerNumber();
			if (_playerStatus[seat_index].lock_huan_zhang() == false) {
				// 长沙麻将吃操作 转转麻将不能吃
				action = _logic.check_chi(GRR._cards_index[seat_index], card);
				if ((action & GameConstants.WIK_LEFT) != 0) {
					_playerStatus[seat_index].add_action(GameConstants.WIK_LEFT);
					_playerStatus[seat_index].add_chi(card, GameConstants.WIK_LEFT, seat_index);
				}
				if ((action & GameConstants.WIK_CENTER) != 0) {
					_playerStatus[seat_index].add_action(GameConstants.WIK_CENTER);
					_playerStatus[seat_index].add_chi(card, GameConstants.WIK_CENTER, seat_index);
				}
				if ((action & GameConstants.WIK_RIGHT) != 0) {
					_playerStatus[seat_index].add_action(GameConstants.WIK_RIGHT);
					_playerStatus[seat_index].add_chi(card, GameConstants.WIK_RIGHT, seat_index);
				}

				// 结果判断
				if (_playerStatus[seat_index].has_action()) {
					bAroseAction = true;
				}
			}

		}

		return bAroseAction;
	}

	public boolean estimate_gang_thj_cs_respond(int seat_index, int provider, int card, boolean d, boolean check_chi,
			int[][] special_player_cards, int c_index) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		playerStatus = _playerStatus[seat_index];

		if (playerStatus.lock_huan_zhang() == false) {
			//// 碰牌判断
			action = _logic.check_peng(GRR._cards_index[seat_index], card);
			if (action != 0) {
				playerStatus.add_action(action);
				playerStatus.add_peng(card, seat_index);

				bAroseAction = true;
			}

		}

		// 杠牌判断 如果剩余牌大于1，是否有杠
		if (GRR._left_card_count > 1) {
			if (GRR._cards_index[seat_index][_logic.switch_to_card_index(card)] == 3) {
				playerStatus.add_action(GameConstants.WIK_BU_ZHNAG);
				playerStatus.add_bu_zhang(card, provider, 1);// 加上补涨

				if (GRR._left_card_count > 2) {
					boolean is_ting = false;
					boolean can_gang = false;

					if (is_ting == true) {
						can_gang = true;
					} else {
						// 把可以杠的这张牌去掉。看是不是听牌
						int bu_index = _logic.switch_to_card_index(card);
						int save_count = GRR._cards_index[seat_index][bu_index];
						GRR._cards_index[seat_index][bu_index] = 0;

						int cbWeaveIndex = GRR._weave_count[seat_index];

						GRR._weave_items[seat_index][cbWeaveIndex].public_card = 1;
						GRR._weave_items[seat_index][cbWeaveIndex].center_card = card;
						GRR._weave_items[seat_index][cbWeaveIndex].weave_kind = GameConstants.WIK_GANG;// 接杠
						GRR._weave_items[seat_index][cbWeaveIndex].provide_player = provider;
						GRR._weave_count[seat_index]++;

						can_gang = this.is_cs_ting_card(GRR._cards_index[seat_index], GRR._weave_items[seat_index],
								GRR._weave_count[seat_index], seat_index);

						GRR._weave_count[seat_index] = cbWeaveIndex;
						GRR._cards_index[seat_index][bu_index] = save_count;
					}

					if (can_gang == true) {

						playerStatus.add_action(GameConstants.WIK_GANG);
						playerStatus.add_gang(card, provider, 1);// 加上杠

					}
				}

				bAroseAction = true;
			}
		}

		// 吃胡判断
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		int cbWeaveCount = GRR._weave_count[seat_index];
		action = analyse_chi_hu_card(GRR._cards_index[seat_index], GRR._weave_items[seat_index], cbWeaveCount, card,
				chr, GameConstants.HU_CARD_TYPE_PAOHU, seat_index);

		if (action != 0) {
			special_player_cards[seat_index][c_index] += GameConstants.CARD_ESPECIAL_TYPE_CAN_WIN;

			if (d) {
				chr.opr_or(GameConstants.CHR_HUNAN_SHUANG_GANG_SHANG_PAO);
			} else {
				chr.opr_or(GameConstants.CHR_HUNAN_GANG_SHANG_PAO);
			}
			if (_playerStatus[seat_index].has_chi_hu() == false) {
				_playerStatus[seat_index].add_action(GameConstants.WIK_CHI_HU);
				_playerStatus[seat_index].add_chi_hu(card, provider);// 吃胡的组合
			}

			bAroseAction = true;
		}

		if (check_chi) {
			if (_playerStatus[seat_index].lock_huan_zhang() == false) {
				action = _logic.check_chi(GRR._cards_index[seat_index], card);
				if ((action & GameConstants.WIK_LEFT) != 0) {
					_playerStatus[seat_index].add_action(GameConstants.WIK_LEFT);
					_playerStatus[seat_index].add_chi(card, GameConstants.WIK_LEFT, seat_index);
				}
				if ((action & GameConstants.WIK_CENTER) != 0) {
					_playerStatus[seat_index].add_action(GameConstants.WIK_CENTER);
					_playerStatus[seat_index].add_chi(card, GameConstants.WIK_CENTER, seat_index);
				}
				if ((action & GameConstants.WIK_RIGHT) != 0) {
					_playerStatus[seat_index].add_action(GameConstants.WIK_RIGHT);
					_playerStatus[seat_index].add_chi(card, GameConstants.WIK_RIGHT, seat_index);
				}

				// 结果判断
				if (_playerStatus[seat_index].has_action()) {
					bAroseAction = true;
				}
			}
		}

		return bAroseAction;
	}

	/**
	 * 检查株洲麻将,杠牌 --检查其它玩家对这张牌的 吃碰杆胡
	 * 
	 * @param seat_index
	 * @param provider
	 * @param card
	 * @param d
	 * @param check_chi
	 *            -是否检查吃
	 * @return
	 */
	public boolean estimate_gang_zhuzhou_respond(int seat_index, int provider, int card, boolean d, boolean check_chi) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		playerStatus = _playerStatus[seat_index];
		if (playerStatus.lock_huan_zhang() == false) {
			//// 碰牌判断
			action = _logic.check_peng(GRR._cards_index[seat_index], card);
			if (action != 0) {
				playerStatus.add_action(action);
				playerStatus.add_peng(card, seat_index);

				bAroseAction = true;
			}

		}

		// 杠牌判断 如果剩余牌大于1，是否有杠
		if (GRR._left_card_count > 1) {
			if (GRR._cards_index[seat_index][_logic.switch_to_card_index(card)] == 3) {
				// playerStatus.add_action(MJGameConstants.WIK_BU_ZHNAG);
				// playerStatus.add_bu_zhang(card, provider, 1);//加上补涨

				// if(GRR._left_card_count > 2){
				int bu_index = _logic.switch_to_card_index(card);
				int save_count = GRR._cards_index[seat_index][bu_index];
				GRR._cards_index[seat_index][bu_index] = 0;
				// 长沙麻将判断是不是听牌
				boolean is_ting = is_zhuzhou_ting_card(GRR._cards_index[seat_index], GRR._weave_items[seat_index],
						GRR._weave_count[seat_index]);

				// 把牌加回来
				GRR._cards_index[seat_index][bu_index] = save_count;

				if (is_ting == true) {

					playerStatus.add_action(GameConstants.WIK_GANG);
					playerStatus.add_gang(card, provider, 1);// 加上杠

				}
				// }

				bAroseAction = true;
			}

		}

		// 吃胡判断
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];// playerStatus._chiHuRight;
		chr.set_empty();
		int cbWeaveCount = GRR._weave_count[seat_index];
		action = analyse_chi_hu_card(GRR._cards_index[seat_index], GRR._weave_items[seat_index], cbWeaveCount, card,
				chr, GameConstants.HU_CARD_TYPE_PAOHU, seat_index);

		// 结果判断
		if (action != 0) {
			_playerStatus[seat_index].add_action(GameConstants.WIK_CHI_HU);
			if (d) {
				chr.opr_or(GameConstants.CHR_HUNAN_SHUANG_GANG_SHANG_PAO);
			} else {
				chr.opr_or(GameConstants.CHR_HUNAN_GANG_SHANG_PAO);
			}
			_playerStatus[seat_index].add_chi_hu(card, provider);// 吃胡的组合
			bAroseAction = true;
		}

		if (check_chi) {
			int chi_seat_index = (provider + 1) % GameConstants.GAME_PLAYER;
			if (_playerStatus[chi_seat_index].lock_huan_zhang() == false) {
				// 长沙麻将吃操作 转转麻将不能吃
				action = _logic.check_chi(GRR._cards_index[chi_seat_index], card);
				if ((action & GameConstants.WIK_LEFT) != 0) {
					_playerStatus[chi_seat_index].add_action(GameConstants.WIK_LEFT);
					_playerStatus[chi_seat_index].add_chi(card, GameConstants.WIK_LEFT, seat_index);
				}
				if ((action & GameConstants.WIK_CENTER) != 0) {
					_playerStatus[chi_seat_index].add_action(GameConstants.WIK_CENTER);
					_playerStatus[chi_seat_index].add_chi(card, GameConstants.WIK_CENTER, seat_index);
				}
				if ((action & GameConstants.WIK_RIGHT) != 0) {
					_playerStatus[chi_seat_index].add_action(GameConstants.WIK_RIGHT);
					_playerStatus[chi_seat_index].add_chi(card, GameConstants.WIK_RIGHT, seat_index);
				}

				// 结果判断
				if (_playerStatus[chi_seat_index].has_action()) {
					bAroseAction = true;
				}
			}

		}

		return bAroseAction;
	}

	// 响应判断 是否有碰,如果剩余牌大于1，是否有杠,是否胡牌，下家是否有吃
	// private boolean estimate_player_respond(int seat_index, int card, int
	// estimatKind) {
	// // 变量定义
	// boolean bAroseAction = false;// 出现(是否)有
	//
	// // 用户状态
	// for (int i = 0; i < MJGameConstants.GAME_PLAYER; i++) {
	//// _response[i] = false;// 是否做出反应
	//// _player_action[i] = 0;// 是否有操作
	//// _perform_action[i] = 0;// 操作了什么
	// _playerStatus[i].clean_action();
	// }
	//
	// int action = MJGameConstants.WIK_NULL;
	//
	// // 动作判断
	// for (int i = 0; i < MJGameConstants.GAME_PLAYER; i++) {
	// // 用户过滤
	// if (seat_index == i)
	// continue;
	//
	//
	// // 出牌类型
	// if (estimatKind == EstimatKind.EstimatKind_OutCard) {
	// //// 碰牌判断
	// action = _logic.check_peng(GRR._cards_index[i], card);
	//
	// _playerStatus[i].add_action(action);
	//
	// // 杠牌判断 如果剩余牌大于1，是否有杠
	// if (GRR._left_card_count > 0) {
	// action = _logic.estimate_gang_card(GRR._cards_index[i], card);
	// _playerStatus[i].add_action(action);
	// }
	// }
	//
	// // 吃胡判断
	// ChiHuRight chr = GRR._chi_hu_rights[i];
	// chr.set_empty();
	// int cbWeaveCount = GRR._weave_count[i];
	// action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i],
	// cbWeaveCount, card,chr );
	//
	// // 结果判断
	// if (_playerStatus[i].has_action()){
	// bAroseAction = true;
	// }
	// }
	//
	// // 长沙麻将吃操作 转转麻将不能吃
	// if (_game_type_index == MJGameConstants.GAME_TYPE_CS && seat_index !=
	// _current_player)
	// {
	// // 这里可能有问题 应该是 |=
	// action = _logic.check_chi(GRR._cards_index[_current_player], card);
	// _playerStatus[_current_player].add_action(action);
	//
	// // 结果判断
	// if (_playerStatus[_current_player].has_action()){
	// bAroseAction = true;
	// }
	// }
	// // 结果处理
	// if (bAroseAction == true) {
	// // 设置变量
	// _provide_player = seat_index;// 谁打的牌
	// _provide_card = card;// 打的什么牌
	// _resume_player = _current_player;// 保存当前轮到的玩家
	// _current_player = MJGameConstants.INVALID_SEAT;// 有需要操作的玩家。当前玩家为空
	//
	// for (int i = 0; i < MJGameConstants.GAME_PLAYER; i++) {
	// if (_player_action[i] != MJGameConstants.WIK_NULL) {
	// _player_status[i] = MJGameConstants.Player_Status_OPR_CARD;// 操作状态
	// }
	// }
	//
	// // 发送提示(优先级判断呢？)
	// this.notify_operate();
	// return true;
	// }
	//
	// return false;
	// }

	// 强制把玩家踢出房间
	@Override
	public boolean force_kick_player_out_room(int seat_index, String tip) {

		if (seat_index == GameConstants.INVALID_SEAT)
			return false;
		Player p = this.get_players()[seat_index];
		if (p == null)
			return false;

		RoomResponse.Builder quit_roomResponse = RoomResponse.newBuilder();
		quit_roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);// 直接拉出游戏
		send_response_to_player(seat_index, quit_roomResponse);

		send_error_notify(seat_index, 2, tip);// "您已退出该游戏"
		// PlayerServiceImpl.getInstance().send(this.get_players()[seat_index],MessageResponse.getMsgAllResponse(-1,
		// "您的金币不足，是否打开商城获取金币？!",ESysMsgType.MONEY_ERROR).build());

		this.get_players()[seat_index] = null;
		_player_ready[seat_index] = 0;
		// _player_open_less[seat_index] = 0;

		PlayerServiceImpl.getInstance().quitRoomId(this.getRoom_id(), p.getAccount_id());

		if (getPlayerCount() == 0) {// 释放房间
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
		} else {
			// 刷新玩家
			RoomResponse.Builder refreshroomResponse = RoomResponse.newBuilder();
			refreshroomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);
			this.load_player_info_data(refreshroomResponse);
			send_response_to_other(seat_index, refreshroomResponse);
		}
		if (_kick_schedule != null) {
			_kick_schedule.cancel(false);
			_kick_schedule = null;
		}
		return true;
	}

	@Override
	public boolean check_if_kick_unready_player() {
		if (!is_sys())
			return false;
		if (getPlayerCount() == GameConstants.GAME_PLAYER && _kick_schedule == null) {// 人满
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				if (_player_ready[i] == 1) {
					_kick_schedule = GameSchedule.put(new KickRunnable(getRoom_id()),
							GameConstants.TRUSTEE_TIME_OUT_SECONDS, TimeUnit.SECONDS);
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean check_if_cancel_kick() {
		if (!is_sys())
			return false;

		if (getPlayerCount() == GameConstants.GAME_PLAYER && _kick_schedule != null) {// 人满
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				if (_player_ready[i] == 1) {
					return false;
				}
			}

			_kick_schedule.cancel(false);
			_kick_schedule = null;
		}
		return false;
	}

	@Override
	public boolean handler_player_ready_in_gold(int get_seat_index, boolean is_cancel) {
		if (is_cancel) {// 取消准备
			if (this.get_players()[get_seat_index] == null) {
				return false;
			}
			if (GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) {
				return false;
			}
			check_if_cancel_kick();

			_player_ready[get_seat_index] = 0;
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_PLAYER_READY);
			roomResponse.setOperatePlayer(get_seat_index);
			roomResponse.setIsCancelReady(is_cancel);
			send_response_to_room(roomResponse);
			if (this._cur_round > 0) {
				// 结束后刷新玩家
				RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
				roomResponse2.setGameStatus(_game_status);
				roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
				this.load_player_info_data(roomResponse2);
				this.send_response_to_player(get_seat_index, roomResponse2);
			}
			return false;
		} else {
			Player p = this.get_players()[get_seat_index];
			if (p == null) {
				return false;
			}
			// 金币场配置
			SysParamModel sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(getGame_id())
					.get(5006);
			// 判断金币是否足够
			long gold = p.getMoney();
			long entrygold = sysParamModel.getVal4().longValue();
			if (gold < entrygold) {
				force_kick_player_out_room(get_seat_index, "金币必须大于" + entrygold + "才能游戏!");
				return false;
			}
			boolean ret = handler_player_ready(get_seat_index, is_cancel);
			check_if_kick_unready_player();
			return ret;
		}
	}

	@Override
	public boolean handler_player_ready(int seat_index, boolean is_cancel) {

		if (this.get_players()[seat_index] == null) {
			return false;
		}
		if (GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) {
			return false;
		}

		if (is_cancel) {//
			_player_ready[seat_index] = 0;
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_PLAYER_READY);
			roomResponse.setOperatePlayer(seat_index);
			roomResponse.setIsCancelReady(is_cancel);
			send_response_to_room(roomResponse);
			if (this._cur_round > 0) {
				// 结束后刷新玩家
				RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
				roomResponse2.setGameStatus(_game_status);
				roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
				this.load_player_info_data(roomResponse2);
				this.send_response_to_player(seat_index, roomResponse2);
			}
			return false;
		}

		_player_ready[seat_index] = 1;

		boolean nt = true;
		if (this.get_players()[seat_index].getAccount_id() == this.getRoom_owner_account_id()) {
			nt = false;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PLAYER_READY);
		roomResponse.setOperatePlayer(seat_index);
		send_response_to_room(roomResponse);

		// 如果不是河南安阳麻将才重置跑呛，解决一下河南安阳麻将的跑呛bug
		if (!is_mj_type(GameConstants.GAME_TYPE_HENAN_AY) && !is_mj_type(GameConstants.GAME_TYPE_NEW_AN_YANG)
				&& !is_mj_type(GameConstants.GAME_TYPE_NEW_ZHU_MA_DIAN)) {
			// 跑分
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				_player_result.pao[i] = 0;// 清掉 默认是-1
				_player_result.qiang[i] = 0;// 清掉 默认是-1
			}
		}
		// 闹庄
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_result.nao[i] = 0;
		}

		if (this._cur_round > 0) {
			// 结束后刷新玩家
			RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
			roomResponse2.setGameStatus(_game_status);
			roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
			this.load_player_info_data(roomResponse2);
			this.send_response_to_player(seat_index, roomResponse2);
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (this.get_players()[i] == null) {
				_player_ready[i] = 0;
			}

			if (_player_ready[i] == 0) {
				this.refresh_room_redis_data(GameConstants.PROXY_ROOM_PLAYER, nt);
				return false;
			}
		}

		handler_game_start();

		this.refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, nt);

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(int seat_index) {
		if ((GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status)
				&& this.get_players()[seat_index] != null) {
			if (this._handler != null)
				this._handler.handler_player_be_in_room(this, seat_index);
			be_in_room_trustee_match(seat_index);
			
		}

		if (GameConstants.GS_MJ_WAIT == _game_status) {
			RoomResponse.Builder tmp_roomResponse = RoomResponse.newBuilder();
			tmp_roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_WHEN_GAME_FINISH);

			send_response_to_room(tmp_roomResponse);

			log_warn(" 小局结束的时候，断线重连了！！");
		}

		GameSchedule.put(new Runnable() {
			@Override
			public void run() {
				fixBugTemp(seat_index);
			}
		}, 1, TimeUnit.SECONDS);

		if (is_sys())
			return true;

		return true;
	}

	@Override
	public void fixBugTemp(int seat_index) {
		if (_gameRoomRecord != null) {
			if (_gameRoomRecord.request_player_seat != GameConstants.INVALID_SEAT) {
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_PLAYER_RELEASE);

				SysParamModel sysParamModel3007 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1)
						.get(3007);
				int delay = 60;
				if (sysParamModel3007 != null) {
					delay = sysParamModel3007.getVal1();
				}

				roomResponse.setReleaseTime(delay);
				roomResponse.setOperateCode(0);
				roomResponse.setRequestPlayerSeat(_gameRoomRecord.request_player_seat);
				roomResponse.setLeftTime((_request_release_time - System.currentTimeMillis()) / 1000);
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					roomResponse.addReleasePlayers(_gameRoomRecord.release_players[i]);
				}
				this.send_response_to_player(seat_index, roomResponse);
			}
		}
	}

	/**
	 * //用户出牌
	 * 
	 * @param seat_index
	 * @param card
	 * @return
	 */
	@Override
	public boolean handler_player_out_card(int seat_index, int card) {
		// 牌局未开 或者等待状态 调度不需要执行 add by zain 2017/6/1
		if ((_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) && is_sys())
			return false;
		// add end

		if (this._handler != null) {
			this._handler.handler_player_out_card(this, seat_index, card);
		}

		return true;
	}

	/**
	 * @param get_seat_index
	 * @param out_cards[]
	 * @param card_count
	 * @param b_out_card
	 * @return
	 */
	@Override
	public boolean handler_operate_out_card_mul(int get_seat_index, List<Integer> list, int card_count, int b_out_card,
			String desc) {
		return true;
	}

	/***
	 * //用户操作
	 * 
	 * @param seat_index
	 * @param operate_code
	 * @param operate_card
	 * @return
	 */
	@Override
	public boolean handler_operate_card(int seat_index, int operate_code, int operate_card, int luoCode) {
		// 牌局未开 或者等待状态 调度不需要执行 add by zain 2017/6/1
		if ((_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) && is_sys())
			return false;
		// add end
		if (this._handler != null) {
			this._handler.handler_operate_card(this, seat_index, operate_code, operate_card);
		}

		return true;
	}

	/**
	 * @param get_seat_index
	 * @param operate_code
	 * @return
	 */
	@Override
	public boolean handler_operate_button(int seat_index, int operate_code) {
		return true;
	}

	/**
	 * @param get_seat_index
	 * @param call_banker
	 * @return
	 */
	@Override
	public boolean handler_call_banker(int seat_index, int call_banker) {
		return true;
	}

	/**
	 * @param seat_index
	 * @param jetton
	 * @return
	 */
	@Override
	public boolean handler_add_jetton(int seat_index, int jetton) {
		return true;
	}

	/**
	 * @param seat_index
	 * @param open_flag
	 * @return
	 */
	@Override
	public boolean handler_open_cards(int seat_index, boolean open_flag) {
		return true;
	}

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private boolean process_xiao_hu(int seat_index, int operate_code) {
		PlayerStatus playerStatus = _playerStatus[seat_index];

		if (playerStatus.has_xiao_hu() == false) {
			this.log_player_error(seat_index, "没有小胡");
			return false;
		}

		if (operate_code != GameConstants.WIK_NULL) {
			ChiHuRight start_hu_right = GRR._start_hu_right[seat_index];

			start_hu_right.set_valid(true);// 小胡生效
			// int cbChiHuKind =
			// analyse_chi_hu_card_cs_xiaohu(GRR._cards_index[seat_index],
			// start_hu_right);
			//
			// //判断是不是有小胡
			// if(cbChiHuKind==MJGameConstants.WIK_NULL){
			// this.log_player_error(seat_index,"没有小胡");
			// return false;
			// }
			//
			int lStartHuScore = 0;

			int wFanShu = _logic.get_chi_hu_action_rank_cs(GRR._start_hu_right[seat_index]);

			lStartHuScore = wFanShu * GameConstants.CELL_SCORE;

			GRR._start_hu_score[seat_index] = lStartHuScore * 3;// 赢3个人的分数

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				if (i == seat_index)
					continue;
				GRR._lost_fan_shu[i][seat_index] = wFanShu;// 自己杠了？？？？？？？？？？？
				GRR._start_hu_score[i] -= lStartHuScore;// 输的番薯
			}

			// 效果
			this.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, start_hu_right.type_count,
					start_hu_right.type_list, start_hu_right.type_count, GameConstants.INVALID_SEAT);

			// 构造扑克
			int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
			for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
				cbCardIndexTemp[i] = GRR._cards_index[seat_index][i];
			}

			int hand_card_indexs[] = new int[GameConstants.MAX_INDEX];
			int show_card_indexs[] = new int[GameConstants.MAX_INDEX];

			for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
				hand_card_indexs[i] = GRR._cards_index[seat_index][i];
			}

			if (start_hu_right._show_all) {
				for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
					show_card_indexs[i] = GRR._cards_index[seat_index][i];
					hand_card_indexs[i] = 0;
				}
			} else {
				if (start_hu_right._index_da_si_xi != GameConstants.MAX_INDEX) {
					hand_card_indexs[start_hu_right._index_da_si_xi] = 0;
					show_card_indexs[start_hu_right._index_da_si_xi] = 4;
				}
				if ((start_hu_right._index_liul_liu_shun_1 != GameConstants.MAX_INDEX)
						&& (start_hu_right._index_liul_liu_shun_2 != GameConstants.MAX_INDEX)) {
					hand_card_indexs[start_hu_right._index_liul_liu_shun_1] = 0;
					show_card_indexs[start_hu_right._index_liul_liu_shun_1] = 3;

					hand_card_indexs[start_hu_right._index_liul_liu_shun_2] = 0;
					show_card_indexs[start_hu_right._index_liul_liu_shun_2] = 3;
				}
			}

			int cards[] = new int[GameConstants.MAX_COUNT];

			// 刷新自己手牌
			int hand_card_count = _logic.switch_to_cards_data(hand_card_indexs, cards);
			this.operate_player_cards(seat_index, hand_card_count, cards, 0, null);

			// 显示 小胡排
			hand_card_count = _logic.switch_to_cards_data(show_card_indexs, cards);
			this.operate_show_card(seat_index, GameConstants.Show_Card_XiaoHU, hand_card_count, cards,
					GameConstants.INVALID_SEAT);

		} else {
			GRR._start_hu_right[seat_index].set_empty();
		}

		// 玩家操作
		playerStatus.operate(operate_code, 0);
		//
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			playerStatus = _playerStatus[i];
			if (playerStatus.has_xiao_hu() && playerStatus.is_respone() == false) {
				return false;
			}

		}

		// 用户状态
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			_playerStatus[i].clean_action();
			change_player_status(i, GameConstants.INVALID_VALUE);
			// _playerStatus[i].clean_status();
		}

		_table_scheduled = GameSchedule.put(new XiaoHuRunnable(this.getRoom_id(), seat_index, true),
				GameConstants.XIAO_HU_DELAY, TimeUnit.SECONDS);

		return true;
	}

	/**
	 * 处理吃胡的玩家
	 * 
	 * @param seat_index
	 * @param operate_card
	 * @param rm
	 */
	@Override
	public void process_chi_hu_player_operate(int seat_index, int operate_card, boolean rm) {
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		
		// filtrate_right(seat_index, chr);
		cleanActionAfterHu();
		if (is_mj_type(GameConstants.GAME_TYPE_ZZ) || is_mj_type(GameConstants.GAME_TYPE_HZ)
				|| is_mj_type(GameConstants.GAME_TYPE_FLS_HZ_LX)
				|| is_mj_type(GameConstants.GAME_TYPE_HENAN_ZHUAN_ZHUAN)
				|| is_mj_type(GameConstants.GAME_TYPE_HALL_ZHUAN_ZHUAN_MJ)
				|| is_mj_type(GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ)
				|| is_mj_type(GameConstants.GAME_TYPE_HONG_ZHONG_MJ_TH)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_HONG_ZHONG_FEI)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_PING_XIANG_ZZ)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_YUYANG_HONGZHON)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_SAN_MEN_XIA)) { // liuyan
																			// 2017/7/10
			int effect_count = chr.type_count;
			long effect_indexs[] = new long[effect_count];
			for (int i = 0; i < effect_count; i++) {
				if (chr.type_list[i] == GameConstants.CHR_SHU_FAN) {
					effect_indexs[i] = GameConstants.CHR_HU;
				} else {
					effect_indexs[i] = chr.type_list[i];
				}

			}

			// 效果
			this.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, effect_count, effect_indexs, 1,
					GameConstants.INVALID_SEAT);
		} else {
			// 效果
			this.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, chr.type_count, chr.type_list,
					1, GameConstants.INVALID_SEAT);
		}

		// 手牌删掉
		this.operate_player_cards(seat_index, 0, null, 0, null);

		if (rm) {
			// 把摸的牌从手牌删掉,结算的时候不显示这张牌的
			GRR._cards_index[seat_index][_logic.switch_to_card_index(operate_card)]--;
		}

		// 显示胡牌
		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], cards);
		cards[hand_card_count] = operate_card + GameConstants.CARD_ESPECIAL_TYPE_HU;
		hand_card_count++;
		this.operate_show_card(seat_index, GameConstants.Show_Card_HU, hand_card_count, cards,
				GameConstants.INVALID_SEAT);

		if (is_mj_type(GameConstants.GAME_TYPE_NEW_LUO_YANG_GANG_CI)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_LYGC_DIAN_PAO) || is_mj_type(GameConstants.GAME_TYPE_MJ_HE_ZE)
				|| isCoinRoom()) {
			// 把其他玩家的牌也倒下来展示3秒
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index)
					continue;

				operate_player_cards(i, 0, null, 0, null);

				hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], cards);

				for (int j = 0; j < hand_card_count; j++) {
					if (_logic.is_ci_card(_logic.switch_to_card_index(cards[j]))
							&& (is_mj_type(GameConstants.GAME_TYPE_NEW_LUO_YANG_GANG_CI)
									|| is_mj_type(GameConstants.GAME_TYPE_HENAN_LYGC))) {
						cards[j] += GameConstants.CARD_ESPECIAL_TYPE_CI;
					}
				}

				operate_show_card(i, GameConstants.Show_Card_HU, hand_card_count, cards, GameConstants.INVALID_SEAT);
			}
		}

		return;
	}

	public void process_chi_hu_player_operate_liuju() {
		// 把其他玩家的牌也倒下来展示3秒
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			operate_player_cards(i, 0, null, 0, null);

			int[] cards = new int[GameConstants.MAX_COUNT];
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], cards);

			for (int j = 0; j < hand_card_count; j++) {
				if (_logic.is_ci_card(_logic.switch_to_card_index(cards[j]))
						&& (is_mj_type(GameConstants.GAME_TYPE_NEW_LUO_YANG_GANG_CI)
								|| is_mj_type(GameConstants.GAME_TYPE_HENAN_LYGC))) {
					cards[j] += GameConstants.CARD_ESPECIAL_TYPE_CI;
				}
			}

			operate_show_card(i, GameConstants.Show_Card_HU, hand_card_count, cards, GameConstants.INVALID_SEAT);
		}

		return;
	}

	public void process_chi_hu_player_operate_all() {
		// 把其他玩家的牌也倒下来展示3秒
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			operate_player_cards(i, 0, null, 0, null);

			int[] cards = new int[GameConstants.MAX_COUNT];
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], cards);
			operate_show_card(i, GameConstants.Show_Card_HU, hand_card_count, cards, GameConstants.INVALID_SEAT);
		}

		return;
	}

	public int get_henan_lh_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount) {
		if (SysParamServerUtil.is_new_algorithm(3000, 3000, 1)) {
			return get_henan_lh_ting_card_new(cards, cards_index, weaveItem, cbWeaveCount);
		} else {
			return get_henan_lh_ting_card_old(cards, cards_index, weaveItem, cbWeaveCount);
		}
	}

	public int get_henan_lh_ting_card_old(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount) {
		// 复制数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();
		int cbCurrentCard;
		int count = 0;

		int l = GameConstants.MAX_INDEX - GameConstants.CARD_HUA_COUNT - GameConstants.CARD_FENG_COUNT;
		int ql = l;
		if (has_rule(GameConstants.GAME_RULE_HENAN_DAIFENG)) {
			if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HENAN_HONG_ZHONG_LAI_ZI)) {
				l += GameConstants.CARD_FENG_COUNT;
				ql += (GameConstants.CARD_FENG_COUNT - 1);
			} else {
				l += GameConstants.CARD_FENG_COUNT;
				ql = GameConstants.CARD_FENG_COUNT;
			}
		}
		for (int i = 0; i < l; i++) {
			if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HENAN_HONG_ZHONG_LAI_ZI)) {
				if (this._logic.is_magic_index(i))
					continue;
			}
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_henan_lh(cbCardIndexTemp, weaveItem, cbWeaveCount,
					cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}

		l -= 1;
		if (count == 0) {
		} else if (count > 0 && count < ql) {
			if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HENAN_HONG_ZHONG_LAI_ZI)) {
				// 有胡的牌。红中肯定能胡
				cards[count] = _logic.switch_to_card_data(this._logic.get_magic_card_index(0));
				count++;
			}
		} else {
			// 全听
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	public int get_henan_lh_ting_card_new(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount) {
		PerformanceTimer timer = new PerformanceTimer();

		// 复制数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();
		int cbCurrentCard;
		int count = 0;

		int l = GameConstants.MAX_INDEX - GameConstants.CARD_HUA_COUNT - GameConstants.CARD_FENG_COUNT;
		int ql = l;
		if (has_rule(GameConstants.GAME_RULE_HENAN_DAIFENG)) {
			if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HENAN_HONG_ZHONG_LAI_ZI)) {
				l += GameConstants.CARD_FENG_COUNT;
				ql += (GameConstants.CARD_FENG_COUNT - 1);
			} else {
				l += GameConstants.CARD_FENG_COUNT;
				ql = GameConstants.CARD_FENG_COUNT;
			}
		}
		for (int i = 0; i < l; i++) {
			if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HENAN_HONG_ZHONG_LAI_ZI)) {
				if (this._logic.is_magic_index(i))
					continue;
			}
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_henan_lh_new(cbCardIndexTemp, weaveItem, cbWeaveCount,
					cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}

		l -= 1;
		if (count == 0) {
		} else if (count > 0 && count < ql) {
			if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HENAN_HONG_ZHONG_LAI_ZI)) {
				// 有胡的牌。红中肯定能胡
				cards[count] = _logic.switch_to_card_data(this._logic.get_magic_card_index(0));
				count++;
			}
		} else {
			// 全听
			count = 1;
			cards[0] = -1;
		}

		if (timer.get() > 50) {
			logger.warn("cost time too long " + Arrays.toString(cards_index) + ", cost time = " + timer.duration());
		}

		return count;
	}

	public void process_chi_hu_player_score_he_nan_lh(int seat_index, int provide_index, int operate_card,
			boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int wFanShu = _logic.get_chi_hu_action_rank_henan_lh(chr);// 番数
		countCardType(chr, seat_index);

		// 统计
		if (zimo) {
			// 自摸
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				GRR._lost_fan_shu[i][seat_index] = wFanShu;
			}
		} else {// 点炮
			GRR._lost_fan_shu[provide_index][seat_index] = wFanShu;
		}

		float lChiHuScore = wFanShu;

		// 胡的人是庄家
		boolean zhuang_hu = (GRR._banker_player == seat_index ? true : false);
		// 是否是庄家放胡
		boolean zhuang_fang_hu = (GRR._banker_player == provide_index ? true : false);

		////////////////////////////////////////////////////// 自摸 算分
		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}

				float s = lChiHuScore;

				if (!(chr.opr_and(GameConstants.CHR_HENAN_GANG_KAI)).is_empty()) {
					s += 4;
				}
				if (!(chr.opr_and(GameConstants.CHR_HENAN_QI_XIAO_DUI)).is_empty()) {
					s += 4;
				}
				if (!(chr.opr_and(GameConstants.CHR_HENAN_ZHOU_KOU_QING_YI_SE)).is_empty()) {
					s += 4;
				}
				// 胡牌分
				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;
			}
		}
		////////////////////////////////////////////////////// 点炮 算分
		else {
			float s = lChiHuScore;

			if (!(chr.opr_and(GameConstants.CHR_HENAN_GANG_KAI)).is_empty()) {
				s += 4;
			}
			if (!(chr.opr_and(GameConstants.CHR_HENAN_QI_XIAO_DUI)).is_empty()) {
				s += 4;
			}
			if (!(chr.opr_and(GameConstants.CHR_HENAN_ZHOU_KOU_QING_YI_SE)).is_empty()) {
				s += 4;
			}

			// 胡牌分
			GRR._game_score[provide_index] -= s;
			GRR._game_score[seat_index] += s;

			GRR._chi_hu_rights[provide_index].opr_or(GameConstants.CHR_FANG_PAO);

		}

		// 设置变量
		GRR._provider[seat_index] = provide_index;
		_status_gang = false;
		_status_gang_hou_pao = false;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);
	}

	/**
	 * 处理吃胡的玩家
	 * 
	 * @param seat_index
	 * @param operate_card
	 * @param rm
	 */
	public void process_chi_hu_player_operate_pi_ci(int seat_index, int operate_card, boolean rm) {
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		cleanActionAfterHu();
		// 效果
		this.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, chr.type_count, chr.type_list, 1,
				GameConstants.INVALID_SEAT);

		// 手牌删掉
		this.operate_player_cards(seat_index, 0, null, 0, null);

		if (rm) {
			// 把摸的牌从手牌删掉,结算的时候不显示这张牌的
			GRR._cards_index[seat_index][_logic.switch_to_card_index(operate_card)]--;
		}

		// 显示胡牌
		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], cards);
		/*
		 * cards[hand_card_count] = operate_card +
		 * GameConstants.CARD_ESPECIAL_TYPE_HU; hand_card_count++;
		 */
		this.operate_show_card(seat_index, GameConstants.Show_Card_HU, hand_card_count, cards,
				GameConstants.INVALID_SEAT);

		// 把其他玩家的牌也倒下来展示3秒
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (i == seat_index)
				continue;

			operate_player_cards(i, 0, null, 0, null);

			hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], cards);

			for (int j = 0; j < hand_card_count; j++) {
				if (_logic.is_ci_card(_logic.switch_to_card_index(cards[j]))
						&& (is_mj_type(GameConstants.GAME_TYPE_NEW_LUO_YANG_GANG_CI)
								|| is_mj_type(GameConstants.GAME_TYPE_HENAN_LYGC))) {
					cards[j] += GameConstants.CARD_ESPECIAL_TYPE_CI;
				}
			}

			operate_show_card(i, GameConstants.Show_Card_HU, hand_card_count, cards, GameConstants.INVALID_SEAT);
		}

		return;
	}

	/**
	 * 河南红中麻将
	 * 
	 * @param seat_index
	 * @param operate_card
	 * @param rm
	 */
	public void process_chi_hu_player_operate_hnhz(int seat_index, int operate_card, boolean rm) {
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int effect_count = chr.type_count;
		long effect_indexs[] = new long[effect_count];
		for (int i = 0; i < effect_count; i++) {
			if (chr.type_list[i] == GameConstants.CHR_SHU_FAN) {
				effect_indexs[i] = GameConstants.CHR_HU;
			} else {
				effect_indexs[i] = chr.type_list[i];
			}

		}
		// 效果
		this.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, effect_count, effect_indexs, 1,
				GameConstants.INVALID_SEAT);
		// 手牌删掉
		this.operate_player_cards(seat_index, 0, null, 0, null);

		if (rm) {
			// 把摸的牌从手牌删掉,结算的时候不显示这张牌的
			GRR._cards_index[seat_index][_logic.switch_to_card_index(operate_card)]--;
		}

		// 显示胡牌
		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], cards);
		cards[hand_card_count] = operate_card + GameConstants.CARD_ESPECIAL_TYPE_HU;
		hand_card_count++;
		this.operate_show_card(seat_index, GameConstants.Show_Card_HU, hand_card_count, cards,
				GameConstants.INVALID_SEAT);

	}

	/**
	 * 长沙
	 * 
	 * @param seat_index
	 * @param operate_card
	 * @param rm
	 */
	public void process_chi_hu_player_operate_cs(int seat_index, int operate_card[], int card_count, boolean rm) {
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		// 效果
		this.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, chr.type_count, chr.type_list, 1,
				GameConstants.INVALID_SEAT);

		// 手牌删掉
		this.operate_player_cards(seat_index, 0, null, 0, null);

		if (rm) {
			// 把摸的牌从手牌删掉,结算的时候不显示这张牌的
			for (int i = 0; i < card_count; i++) {
				GRR._cards_index[seat_index][_logic.switch_to_card_index(operate_card[i])]--;
			}
		}

		// 显示胡牌
		int cards[] = new int[GameConstants.MAX_INDEX];
		int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], cards);
		for (int i = 0; i < card_count; i++) {
			cards[hand_card_count++] = operate_card[i] + GameConstants.CARD_ESPECIAL_TYPE_HU;
		}
		this.operate_show_card(seat_index, GameConstants.Show_Card_HU, hand_card_count, cards,
				GameConstants.INVALID_SEAT);

		return;
	}

	/**
	 * 双鬼 处理吃胡的玩家
	 * 
	 * @param seat_index
	 * @param operate_card
	 * @param rm
	 */
	public void process_chi_hu_player_operate_sg(int seat_index, int operate_card, boolean rm) {
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		// filtrate_right(seat_index, chr);

		if (is_mj_type(GameConstants.GAME_TYPE_ZZ) || is_mj_type(GameConstants.GAME_TYPE_HZ)
				|| is_mj_type(GameConstants.GAME_TYPE_HUNAN_SHANG_XIA_GUI)
				|| is_mj_type(GameConstants.GAME_TYPE_SHUANGGUI)) {
			int effect_count = chr.type_count;
			long effect_indexs[] = new long[effect_count];
			for (int i = 0; i < effect_count; i++) {
				if (chr.type_list[i] == GameConstants.CHR_SHU_FAN) {
					effect_indexs[i] = GameConstants.CHR_HU;
				} else {
					effect_indexs[i] = chr.type_list[i];
				}

			}

			// 效果
			this.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, effect_count, effect_indexs, 1,
					GameConstants.INVALID_SEAT);
		} else if (GameConstants.GAME_TYPE_CS == _game_type_index || GameConstants.GAME_TYPE_CS == _game_type_index
				|| is_mj_type(GameConstants.GAME_TYPE_HALL_CHANG_SHA_MJ)) {
			// 效果
			this.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, chr.type_count, chr.type_list,
					1, GameConstants.INVALID_SEAT);
		}

		// 手牌删掉
		this.operate_player_cards(seat_index, 0, null, 0, null);

		if (rm) {
			// 把摸的牌从手牌删掉,结算的时候不显示这张牌的
			GRR._cards_index[seat_index][_logic.switch_to_card_index(operate_card)]--;
		}

		// 显示胡牌
		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], cards);
		for (int i = 0; i < hand_card_count; i++) {
			if (_logic.is_magic_card(cards[i])) {
				cards[i] += GameConstants.CARD_ESPECIAL_TYPE_GUI;
			}
		}
		cards[hand_card_count] = operate_card + GameConstants.CARD_ESPECIAL_TYPE_HU;
		hand_card_count++;
		this.operate_show_card(seat_index, GameConstants.Show_Card_HU, hand_card_count, cards,
				GameConstants.INVALID_SEAT);

		return;
	}

	/**
	 * 处理仙桃晃晃胡牌显示
	 * 
	 * @param seat_index
	 * @param operate_card
	 * @param rm
	 */
	public void process_chi_hu_player_operate_xthh(int seat_index, int operate_card, boolean rm) {
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		// filtrate_right(seat_index, chr);

		int effect_count = chr.type_count;
		long effect_indexs[] = new long[effect_count];
		for (int i = 0; i < effect_count; i++) {
			if (chr.type_list[i] == GameConstants.CHR_SHU_FAN) {
				effect_indexs[i] = GameConstants.CHR_HU;
			} else {
				effect_indexs[i] = chr.type_list[i];
			}

		}

		// 效果
		this.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, effect_count, effect_indexs, 1,
				GameConstants.INVALID_SEAT);

		// 手牌删掉
		this.operate_player_cards(seat_index, 0, null, 0, null);

		if (rm) {
			// 把摸的牌从手牌删掉,结算的时候不显示这张牌的
			GRR._cards_index[seat_index][_logic.switch_to_card_index(operate_card)]--;
		}

		// 显示胡牌
		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], cards);
		for (int i = 0; i < hand_card_count; i++) {
			if (_logic.is_magic_card(cards[i])) {
				cards[i] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
			}
		}
		cards[hand_card_count] = operate_card + GameConstants.CARD_ESPECIAL_TYPE_HU;
		hand_card_count++;
		this.operate_show_card(seat_index, GameConstants.Show_Card_HU, hand_card_count, cards,
				GameConstants.INVALID_SEAT);

		return;
	}

	public void process_chi_hu_player_score_xthh(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int wFanShu = _logic.get_chi_hu_action_rank_xthh(chr);// 番数
		countCardType(chr, seat_index);
		// 统计
		if (zimo) {
			// 自摸
			// _player_result.zi_mo_count[seat_index]++;
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				// GRR._hu_result[i] = MJGameConstants.HU_RESULT_NULL;
				if (i == seat_index) {
					// GRR._hu_result[i] = MJGameConstants.HU_RESULT_ZIMO;
					continue;
				}

				GRR._lost_fan_shu[i][seat_index] = wFanShu;
			}
		} else {// 点炮
			GRR._lost_fan_shu[provide_index][seat_index] = wFanShu;//
			//
			// GRR._hu_result[provide_index] =
			// MJGameConstants.HU_RESULT_FANGPAO;
			// GRR._hu_result[seat_index] = MJGameConstants.HU_RESULT_JIEPAO;
		}

		float lChiHuScore = wFanShu * this.game_cell;// * GRR._bei_shu;

		////////////////////////////////////////////////////// 自摸 算分
		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}

				float s = lChiHuScore;

				s *= this.get_piao_lai_bei_shu(seat_index, i);
				// 胡牌分
				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;

			}
		}
		////////////////////////////////////////////////////// 点炮 算分
		else {
			float s = lChiHuScore;
			s *= this.get_piao_lai_bei_shu(seat_index, provide_index);
			// 如果是抢杠胡
			if (!(chr.opr_and(GameConstants.CHR_HUBEI_QIANG_GANG_HU).is_empty())) {
				// s+=(3* this._di_fen * GRR._bei_shu);
			} else if (!(chr.opr_and(GameConstants.CHR_HUBEI_RE_CHONG).is_empty())) {
				int cbGangIndex = GRR._gang_score[provide_index].gang_count;
				s += GRR._gang_score[provide_index].scores[cbGangIndex - 1][provide_index];// 加上杠的分数

			}

			GRR._game_score[provide_index] -= s;
			GRR._game_score[seat_index] += s;

			// 点炮的时候，删掉这张牌显示
			// GRR._cards_index[seat_index][_logic.switch_to_card_index(_provide_card)]--;

			GRR._chi_hu_rights[provide_index].opr_or(GameConstants.CHR_FANG_PAO);

		}

		// 设置变量
		GRR._provider[seat_index] = provide_index;
		_status_gang = false;
		_status_gang_hou_pao = false;

		_playerStatus[seat_index].clean_status();

	}

	/**
	 * 安阳麻将算分
	 * 
	 * @param seat_index
	 * @param provide_index
	 * @param operate_card
	 * @param zimo
	 */
	public void process_chi_hu_player_score_ay(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int wFanShu = _logic.get_chi_hu_action_rank_ay(chr);// 番数

		// 统计
		if (zimo) {
			// 自摸
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				if (i == seat_index) {
					// GRR._hu_result[i] = MJGameConstants.HU_RESULT_ZIMO;
					continue;
				}

				GRR._lost_fan_shu[i][seat_index] = wFanShu;
			}
		} else {// 点炮
			GRR._lost_fan_shu[provide_index][seat_index] = wFanShu;//
		}

		float lChiHuScore = wFanShu;

		// 花
		lChiHuScore += chr.hua_count;
		////////////////////////////////////////////////////// 自摸 算分
		if (zimo) {
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				if (i == seat_index) {
					continue;
				}

				float s = lChiHuScore;

				if (i == GRR._banker_player || seat_index == GRR._banker_player) {
					s += 5;
					s += (_player_result.qiang[i] + _player_result.qiang[seat_index]);
				} else {
					s += 1;
				}

				s += (_player_result.pao[i] + _player_result.pao[seat_index]);

				// 跑和呛

				s *= 2;

				// 胡牌分
				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;

			}
		}
		////////////////////////////////////////////////////// 点炮 算分
		else {
			float s = lChiHuScore;

			if (provide_index == GRR._banker_player || seat_index == GRR._banker_player) {
				s += 5;
				s += (_player_result.qiang[provide_index] + _player_result.qiang[seat_index]);
			} else {
				s += 1;
			}

			s += (_player_result.pao[provide_index] + _player_result.pao[seat_index]);

			// 跑和呛

			GRR._game_score[provide_index] -= s;
			GRR._game_score[seat_index] += s;

			GRR._chi_hu_rights[provide_index].opr_or(GameConstants.CHR_FANG_PAO);

		}

		// 设置变量
		GRR._provider[seat_index] = provide_index;
		_status_gang = false;
		_status_gang_hou_pao = false;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);
		// _playerStatus[seat_index].clean_status();
	}

	/**
	 * 河南麻将算分
	 * 
	 * @param seat_index
	 * @param provide_index
	 * @param operate_card
	 * @param zimo
	 */
	public void process_chi_hu_player_score_henan(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int wFanShu = 1;

		if (is_mj_type(GameConstants.GAME_TYPE_HENAN_XX) || is_mj_type(GameConstants.GAME_TYPE_NEW_XIN_XIANG)) {
			if (zimo)
				wFanShu = 2;
		} else {
			wFanShu = _logic.get_chi_hu_action_rank_henan(chr);// 番数
		}

		countCardType(chr, seat_index);

		// 统计
		if (zimo) {
			// 自摸
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					// GRR._hu_result[i] = MJGameConstants.HU_RESULT_ZIMO;
					continue;
				}
				GRR._lost_fan_shu[i][seat_index] = wFanShu;
			}
		} else {// 点炮
			GRR._lost_fan_shu[provide_index][seat_index] = wFanShu;//
		}

		float lChiHuScore = wFanShu;

		// 胡的人是庄家
		boolean zhuang_hu = (GRR._banker_player == seat_index ? true : false);
		// 是否是庄家放胡
		boolean zhuang_fang_hu = (GRR._banker_player == provide_index ? true : false);
		// 是否庄家加底
		boolean jia_di = has_rule(GameConstants.GAME_RULE_HENAN_JIA_DI);

		////////////////////////////////////////////////////// 自摸 算分
		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}

				float s = lChiHuScore;

				if (jia_di) {// 庄家加底
					if (zhuang_hu) {
						s += 1;// 庄家hu 别人多输一分
					} else if (GRR._banker_player == i) {// 别人hu 庄家多输一分
						s += 1;
					}
				}
				// 跑
				s += (_player_result.pao[i] + _player_result.pao[seat_index]);

				if (!(chr.opr_and(GameConstants.CHR_HENAN_GANG_KAI)).is_empty()
						&& has_rule(GameConstants.GAME_RULE_HENAN_GANGHUA_DOUBLE)) {
					s *= 2;
				}
				if (!(chr.opr_and(GameConstants.CHR_HENAN_QI_XIAO_DUI)).is_empty()
						&& has_rule(GameConstants.GAME_RULE_HENAN_QIDUI_DOUBLE)) {
					s *= 2;
				}
				if (!(chr.opr_and(GameConstants.CHR_HENAN_QISHOU_HU)).is_empty()) {
					s *= 2;
				}
				if (!(chr.opr_and(GameConstants.CHR_HENAN_HH_QI_XIAO_DUI)).is_empty()) {
					if (has_rule(GameConstants.GAME_RULE_HENAN_HAOQI)) {
						s *= 4;
					} else if (has_rule(GameConstants.GAME_RULE_HENAN_QIDUI_DOUBLE)) {
						s *= 2;
					}

				}

				// WalkerGeek 焦作晃晃自摸翻倍
				if (is_mj_type(GameConstants.GAME_TYPE_JIAO_ZUO_HUAN_HUAN)
						&& has_rule_ex(GameConstants.GAME_RULE_ZI_MO_FAN_BEI)) {
					s *= 2;
				}

				// 胡牌分
				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;

			}
		}
		////////////////////////////////////////////////////// 点炮 算分
		else {
			float s = lChiHuScore;

			if (jia_di) {// 庄家加底
				if (zhuang_hu) {
					s += 1;// 庄家杆 别人多输一分
				} else if (zhuang_fang_hu) {// 庄家放杆 庄家多输一分
					s += 1;
				}
			}

			s += (_player_result.pao[provide_index] + _player_result.pao[seat_index]);

			if (!(chr.opr_and(GameConstants.CHR_HENAN_GANG_KAI)).is_empty()
					&& has_rule(GameConstants.GAME_RULE_HENAN_GANGHUA_DOUBLE)) {
				s *= 2;
			}
			if (!(chr.opr_and(GameConstants.CHR_HENAN_QI_XIAO_DUI)).is_empty()
					&& has_rule(GameConstants.GAME_RULE_HENAN_QIDUI_DOUBLE)) {
				s *= 2;
			}
			if (!(chr.opr_and(GameConstants.CHR_HENAN_QISHOU_HU)).is_empty()) {
				s *= 2;
			}

			if (!(chr.opr_and(GameConstants.CHR_HENAN_HH_QI_XIAO_DUI)).is_empty()) {
				if (has_rule(GameConstants.GAME_RULE_HENAN_HAOQI)) {
					s *= 4;
				} else if (has_rule(GameConstants.GAME_RULE_HENAN_QIDUI_DOUBLE)) {
					s *= 2;
				}

			}

			// 跑和呛
			GRR._game_score[provide_index] -= s;
			GRR._game_score[seat_index] += s;

			GRR._chi_hu_rights[provide_index].opr_or(GameConstants.CHR_FANG_PAO);

		}

		// 设置变量
		GRR._provider[seat_index] = provide_index;
		_status_gang = false;
		_status_gang_hou_pao = false;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);
		// _playerStatus[seat_index].clean_status();
	}

	/**
	 * 河南平顶山麻将算分
	 * 
	 * @param seat_index
	 * @param provide_index
	 * @param operate_card
	 * @param zimo
	 */
	public void process_chi_hu_player_score_henan_pds(int seat_index, int provide_index, int operate_card,
			boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int wFanShu = _logic.get_chi_hu_action_rank_henan(chr);// 番数
		countCardType(chr, seat_index);

		// 统计
		if (zimo) {
			// 自摸
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					// GRR._hu_result[i] = MJGameConstants.HU_RESULT_ZIMO;
					continue;
				}
				GRR._lost_fan_shu[i][seat_index] = wFanShu;
			}
		} else {// 点炮
			GRR._lost_fan_shu[provide_index][seat_index] = wFanShu;//
		}

		float lChiHuScore = wFanShu;

		// 胡的人是庄家
		boolean zhuang_hu = (GRR._banker_player == seat_index ? true : false);
		// 是否是庄家放胡
		boolean zhuang_fang_hu = (GRR._banker_player == provide_index ? true : false);
		// 是否庄家加底
		boolean jia_di = has_rule(GameConstants.GAME_RULE_HENAN_JIA_DI);

		////////////////////////////////////////////////////// 自摸 算分
		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}

				float s = lChiHuScore;

				if (jia_di) {// 庄家加底
					if (zhuang_hu) {
						s += 1;// 庄家hu 别人多输一分
					} else if (GRR._banker_player == i) {// 别人hu 庄家多输一分
						s += 1;
					}
				}
				// 跑(又称为跑头)
				s += (_player_result.pao[i] + _player_result.pao[seat_index]);
				// 跑扣
				s += (_player_result.qiang[i] + _player_result.qiang[seat_index]);

				if (!(chr.opr_and(GameConstants.CHR_HENAN_GANG_KAI)).is_empty()
						&& has_rule(GameConstants.GAME_RULE_HENAN_GANGHUA_DOUBLE)) {
					s *= 2;
				}
				if (!(chr.opr_and(GameConstants.CHR_HENAN_QI_XIAO_DUI)).is_empty()
						&& has_rule(GameConstants.GAME_RULE_HENAN_QIDUI_DOUBLE)) {
					s *= 2;
				}
				if (!(chr.opr_and(GameConstants.CHR_HENAN_QISHOU_HU)).is_empty()) {
					s *= 2;
				}
				if (!(chr.opr_and(GameConstants.CHR_HENAN_HH_QI_XIAO_DUI)).is_empty()) {
					if (has_rule(GameConstants.GAME_RULE_HENAN_HAOQI)) {
						s *= 4;
					} else if (has_rule(GameConstants.GAME_RULE_HENAN_QIDUI_DOUBLE)) {
						s *= 2;
					}

				}

				if (!(chr.opr_and(GameConstants.CHR_HENAN_HEI_ZI)).is_empty()) {
					s *= 2;
				}

				// 胡牌分
				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;

			}
		}
		////////////////////////////////////////////////////// 点炮 算分
		else {
			float s = lChiHuScore;

			if (jia_di) {// 庄家加底
				if (zhuang_hu) {
					s += 1;// 庄家杆 别人多输一分
				} else if (zhuang_fang_hu) {// 庄家放杆 庄家多输一分
					s += 1;
				}
			}

			// 跑(跑头)
			s += (_player_result.pao[provide_index] + _player_result.pao[seat_index]);

			if (!(chr.opr_and(GameConstants.CHR_HENAN_GANG_KAI)).is_empty()
					&& has_rule(GameConstants.GAME_RULE_HENAN_GANGHUA_DOUBLE)) {
				s *= 2;
			}
			if (!(chr.opr_and(GameConstants.CHR_HENAN_QI_XIAO_DUI)).is_empty()
					&& has_rule(GameConstants.GAME_RULE_HENAN_QIDUI_DOUBLE)) {
				s *= 2;
			}
			if (!(chr.opr_and(GameConstants.CHR_HENAN_QISHOU_HU)).is_empty()) {
				s *= 2;
			}

			if (!(chr.opr_and(GameConstants.CHR_HENAN_HH_QI_XIAO_DUI)).is_empty()) {
				if (has_rule(GameConstants.GAME_RULE_HENAN_HAOQI)) {
					s *= 4;
				} else if (has_rule(GameConstants.GAME_RULE_HENAN_QIDUI_DOUBLE)) {
					s *= 2;
				}

			}

			if (!(chr.opr_and(GameConstants.CHR_HENAN_HEI_ZI)).is_empty()) {
				s *= 2;
			}

			// 跑和呛
			GRR._game_score[provide_index] -= s;
			GRR._game_score[seat_index] += s;

			GRR._chi_hu_rights[provide_index].opr_or(GameConstants.CHR_FANG_PAO);

		}

		// 设置变量
		GRR._provider[seat_index] = provide_index;
		_status_gang = false;
		_status_gang_hou_pao = false;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);
		// _playerStatus[seat_index].clean_status();
	}

	public void process_chi_hu_player_score_he_nan_zhou_kou(int seat_index, int provide_index, int operate_card,
			boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int wFanShu = 1; // 番数
		countCardType(chr, seat_index);

		// 统计
		if (zimo) {
			// 自摸
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				GRR._lost_fan_shu[i][seat_index] = wFanShu;
			}
		} else {// 点炮
			GRR._lost_fan_shu[provide_index][seat_index] = wFanShu;
		}

		float lChiHuScore = wFanShu;

		// 胡的人是庄家
		boolean zhuang_hu = (GRR._banker_player == seat_index ? true : false);
		// 是否是庄家放胡
		boolean zhuang_fang_hu = (GRR._banker_player == provide_index ? true : false);
		// 是否庄家加底
		boolean jia_di = has_rule(GameConstants.GAME_RULE_HENAN_JIA_DI);

		////////////////////////////////////////////////////// 自摸 算分
		int magic_count = _logic.magic_count(GRR._cards_index[seat_index]);

		if (_logic.is_magic_card(operate_card))
			magic_count++;

		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}

				float s = lChiHuScore;

				if (jia_di) { // 庄家加底
					if (zhuang_hu) {
						s += 1; // 庄家hu 别人多输一分
					} else if (GRR._banker_player == i) { // 别人hu 庄家多输一分
						s += 1;
					}
				}

				s += (_player_result.pao[i] + _player_result.pao[seat_index]);

				if (magic_count == 4)
					s *= 2;
				if (has_rule_ex(GameConstants.GAME_RULE_HENAN_QIDUI_DOUBLE))
					if (!(chr.opr_and(GameConstants.CHR_HENAN_QI_XIAO_DUI)).is_empty())
						s *= 2;
				if (has_rule_ex(GameConstants.GAME_RULE_HENAN_GANGHUA_DOUBLE))
					if (!(chr.opr_and(GameConstants.CHR_HENAN_GANG_KAI)).is_empty())
						s *= 2;

				// 胡牌分
				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;

			}
		}
		////////////////////////////////////////////////////// 点炮 算分
		else {
			float s = lChiHuScore;

			if (jia_di) { // 庄家加底
				if (zhuang_hu) {
					s += 1; // 庄家接炮 别人多输一分
				} else if (zhuang_fang_hu) {// 庄家放炮 庄家多输一分
					s += 1;
				}
			}

			s += (_player_result.pao[provide_index] + _player_result.pao[seat_index]);

			if (magic_count == 4)
				s *= 2;
			if (has_rule_ex(GameConstants.GAME_RULE_HENAN_QIDUI_DOUBLE))
				if (!(chr.opr_and(GameConstants.CHR_HENAN_QI_XIAO_DUI)).is_empty())
					s *= 2;
			if (has_rule_ex(GameConstants.GAME_RULE_HENAN_GANGHUA_DOUBLE))
				if (!(chr.opr_and(GameConstants.CHR_HENAN_GANG_KAI)).is_empty())
					s *= 2;

			// 胡牌分
			GRR._game_score[provide_index] -= s;
			GRR._game_score[seat_index] += s;

			GRR._chi_hu_rights[provide_index].opr_or(GameConstants.CHR_FANG_PAO);
		}

		// 设置变量
		GRR._provider[seat_index] = provide_index;
		_status_gang = false;
		_status_gang_hou_pao = false;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);
	}

	public void process_chi_hu_player_score_henan_lh(int seat_index, int provide_index, int operate_card,
			boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int wFanShu = _logic.get_chi_hu_action_rank_henan_lh(chr);// 番数
		countCardType(chr, seat_index);

		// 统计
		if (zimo) {
			// 自摸
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				GRR._lost_fan_shu[i][seat_index] = wFanShu;
			}
		} else {// 点炮
			GRR._lost_fan_shu[provide_index][seat_index] = wFanShu;
		}

		float lChiHuScore = wFanShu;

		// 胡的人是庄家
		boolean zhuang_hu = (GRR._banker_player == seat_index ? true : false);
		// 是否是庄家放胡
		boolean zhuang_fang_hu = (GRR._banker_player == provide_index ? true : false);

		////////////////////////////////////////////////////// 自摸 算分
		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}

				float s = lChiHuScore;

				if (!(chr.opr_and(GameConstants.CHR_HUNAN_GANG_KAI)).is_empty()) {
					s += 4;
				}
				if (!(chr.opr_and(GameConstants.CHR_HENAN_QI_XIAO_DUI)).is_empty()) {
					s += 4;
				}
				if (!(chr.opr_and(GameConstants.CHR_HENAN_ZHOU_KOU_QING_YI_SE)).is_empty()) {
					s += 4;
				}

				// 胡牌分
				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;

			}
		}
		////////////////////////////////////////////////////// 点炮 算分
		else {
			float s = lChiHuScore;

			if (!(chr.opr_and(GameConstants.CHR_HUNAN_GANG_KAI)).is_empty()) {
				s += 4;
			}
			if (!(chr.opr_and(GameConstants.CHR_HENAN_QI_XIAO_DUI)).is_empty()) {
				s += 4;
			}
			if (!(chr.opr_and(GameConstants.CHR_HENAN_ZHOU_KOU_QING_YI_SE)).is_empty()) {
				s += 4;
			}

			// 胡牌分
			GRR._game_score[provide_index] -= s;
			GRR._game_score[seat_index] += s;

			GRR._chi_hu_rights[provide_index].opr_or(GameConstants.CHR_FANG_PAO);

		}

		// 设置变量
		GRR._provider[seat_index] = provide_index;
		_status_gang = false;
		_status_gang_hou_pao = false;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);
	}

	/**
	 * 信阳麻将算分
	 * 
	 * @param seat_index
	 * @param provide_index
	 * @param operate_card
	 * @param zimo
	 */
	public void process_chi_hu_player_score_henanxy(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		boolean qidui = false;
		boolean gangshagnhau = false;

		if (!(chr.opr_and(GameConstants.CHR_HENAN_XY_QI_XIAO_DUI)).is_empty()) {
			qidui = true;
		}
		if (!(chr.opr_and(GameConstants.CHR_HENAN_XY_GANG_KAI)).is_empty()) {
			gangshagnhau = true;
		}

		int wFanShu = _logic.get_chi_hu_action_rank_henanxy(chr);// 番数
		countCardType(chr, seat_index);

		// 统计
		if (zimo) {
			// 自摸
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					// GRR._hu_result[i] = MJGameConstants.HU_RESULT_ZIMO;
					continue;
				}
				GRR._lost_fan_shu[i][seat_index] = wFanShu;
			}
		} else {// 点炮全包
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					// GRR._hu_result[i] = MJGameConstants.HU_RESULT_ZIMO;
					continue;
				}
				GRR._lost_fan_shu[provide_index][seat_index] += wFanShu;
			}
			GRR._chi_hu_rights[provide_index].opr_or(GameConstants.CHR_FANG_PAO);
		}

		float lChiHuScore = wFanShu;

		// 胡的人是庄家
		boolean zhuang_hu = (GRR._banker_player == seat_index ? true : false);
		// 是否是庄家放胡
		boolean zhuang_fang_hu = (GRR._banker_player == provide_index ? true : false);

		////////////////////////////////////////////////////// 自摸 算分
		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}

				float s = lChiHuScore;
				// 跑
				s += (_player_result.pao[i] + _player_result.pao[seat_index]);

				if (!(chr.opr_and(GameConstants.CHR_HENAN_XY_GANG_KAI)).is_empty()
						&& has_rule(GameConstants.GAME_RULE_HENAN_GANGHUA_DOUBLE)) {
					s *= 2;
				}
				if (!(chr.opr_and(GameConstants.CHR_HENAN_XY_QI_XIAO_DUI)).is_empty()
						&& has_rule(GameConstants.GAME_RULE_HENAN_QIDUI_DOUBLE)) {
					s *= 2;
				}
				s *= 2;
				// 胡牌分
				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;

			}
		}
		////////////////////////////////////////////////////// 点炮 算分
		else {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				float s = lChiHuScore;
				s += (_player_result.pao[i] + _player_result.pao[seat_index]);

				if (!(chr.opr_and(GameConstants.CHR_HENAN_XY_GANG_KAI)).is_empty()
						&& has_rule(GameConstants.GAME_RULE_HENAN_GANGHUA_DOUBLE)) {
					s *= 2;
				}
				if (!(chr.opr_and(GameConstants.CHR_HENAN_XY_QI_XIAO_DUI)).is_empty()
						&& has_rule(GameConstants.GAME_RULE_HENAN_QIDUI_DOUBLE)) {
					s *= 2;
				}

				// 跑和呛
				GRR._game_score[provide_index] -= s;
				GRR._game_score[seat_index] += s;
			}
			// GRR._chi_hu_rights[provide_index].opr_or(GameConstants.CHR_FANG_PAO);

		}
		for (int i = 0; i < getTablePlayerNumber(); i++) {

			if (zhuang_hu) {
				if (i == seat_index) {
					continue;
				}
				if (_player_result.nao[seat_index] > 0) {
					if (has_rule(GameConstants.GAME_RULE_HENAN_THREE)) {
						int more_win_score = _lian_zhuang_win_score / 2;
						GRR._game_score[i] -= more_win_score / 2 + more_win_score % 2;
						GRR._nao_win_score[i] -= more_win_score / 2 + more_win_score % 2;
						GRR._game_score[seat_index] += more_win_score / 2 + more_win_score % 2;
						GRR._nao_win_score[seat_index] += more_win_score / 2 + more_win_score % 2;
					} else {
						int more_win_score = _lian_zhuang_win_score / 2;
						GRR._game_score[i] -= more_win_score / 3 + more_win_score % 3;
						GRR._game_score[seat_index] += more_win_score / 3 + more_win_score % 3;

						GRR._nao_win_score[i] -= more_win_score / 3 + more_win_score % 3;
						GRR._nao_win_score[seat_index] += more_win_score / 3 + more_win_score % 3;
					}
				}
			} else {
				if (i == GRR._banker_player) {
					continue;
				}
				if (_player_result.nao[i] > 0) {
					GRR._game_score[i] += _lian_zhuang_win_score / 2 + _lian_zhuang_win_score % 2;
					GRR._game_score[GRR._banker_player] -= _lian_zhuang_win_score / 2 + _lian_zhuang_win_score % 2;
					GRR._nao_win_score[i] += _lian_zhuang_win_score / 2 + _lian_zhuang_win_score % 2;
					GRR._nao_win_score[GRR._banker_player] -= _lian_zhuang_win_score / 2 + _lian_zhuang_win_score % 2;
				}
			}
		}

		// 杠牌，每个人的分数
		float lGangScore[] = new float[GameConstants.GAME_PLAYER];
		for (int i = 0; i < getTablePlayerNumber(); i++) {

			for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
				for (int k = 0; k < GameConstants.GAME_PLAYER; k++) {
					lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
				}
			}
		}
		//
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			GRR._game_score[i] += lGangScore[i];
			_player_result.game_score[i] += GRR._game_score[i];
		}

		if (zhuang_hu) {
			_lian_zhuang_win_score += GRR._game_score[seat_index];
		} else {
			_lian_zhuang_win_score = 0;
		}
		// 设置变量
		GRR._provider[seat_index] = provide_index;
		_status_gang = false;
		_status_gang_hou_pao = false;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);
		// _playerStatus[seat_index].clean_status();
	}

	/**
	 * 洛阳杠次麻将算分
	 * 
	 * @param seat_index
	 *            胡牌玩家位置
	 * @param provide_index
	 *            出牌玩家位置
	 * @param operate_card
	 *            胡的牌
	 * @param zimo
	 *            是否自摸
	 * @param bao_ci
	 *            能否
	 */
	public void process_chi_hu_player_score_henan_lygc(int seat_index, int provide_index, int operate_card, int hu_type,
			boolean bao_ci) {
		if (operate_card != GameConstants.INVALID_SEAT) {
			GRR._chi_hu_card[seat_index][0] = operate_card;
		}

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int wFanShu = _logic.get_chi_hu_action_rank_henan(chr);// 番数
		countCardType(chr, seat_index);

		// 统计
		if (hu_type == GameConstants.HU_CARD_TYPE_ZIMO || hu_type == GameConstants.HU_CARD_TYPE_GANG_CI
				|| hu_type == GameConstants.HU_CARD_TYPE_PI_CI) {
			if (hu_type == GameConstants.HU_CARD_TYPE_PI_CI) {
				wFanShu = getRuleValue(GameConstants.GAME_RULE_BASE_SCORE_CI);
				wFanShu += getRuleValue(GameConstants.GAME_RULE_BASE_SCORE_GANG);
			} else if (hu_type == GameConstants.HU_CARD_TYPE_GANG_CI) {
				wFanShu = getRuleValue(GameConstants.GAME_RULE_BASE_SCORE_CI);
			} else {
				wFanShu = getRuleValue(GameConstants.GAME_RULE_BASE_SCORE);
			}

			// 自摸
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					// GRR._hu_result[i] = MJGameConstants.HU_RESULT_ZIMO;
					continue;
				}
				GRR._lost_fan_shu[i][seat_index] = wFanShu;
			}
		} else if (hu_type == GameConstants.HU_CARD_TYPE_PAOHU) { // 点炮
			wFanShu = getRuleValue(GameConstants.GAME_RULE_BASE_SCORE_PAO);
			GRR._lost_fan_shu[provide_index][seat_index] = wFanShu;//
		}

		float lChiHuScore = wFanShu;

		// 胡的人是庄家
		boolean zhuang_hu = (GRR._banker_player == seat_index ? true : false);
		// 是否是庄家放胡
		boolean zhuang_fang_hu = (GRR._banker_player == provide_index ? true : false);
		// 是否庄家加底
		boolean jia_di = has_rule(GameConstants.GAME_RULE_HENAN_JIA_DI);
		// 次风翻倍
		boolean ci_feng_double = getRuleValue(GameConstants.GAME_RULE_HENAN_CI_FENG_DOUBLE) != 0;

		////////////////////////////////////////////////////// 自摸 算分
		if (hu_type == GameConstants.HU_CARD_TYPE_ZIMO || hu_type == GameConstants.HU_CARD_TYPE_GANG_CI
				|| hu_type == GameConstants.HU_CARD_TYPE_PI_CI) { // 自摸和次算分
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}

				float s = lChiHuScore;

				if (jia_di) {// 庄家加底
					if (zhuang_hu) {
						s *= 2;// 庄家hu 别人多输一分
					} else if (GRR._banker_player == i) {// 别人hu 庄家多输一分
						s *= 2;
					}
				}
				// 跑
				s += (_player_result.pao[i] + _player_result.pao[seat_index]);

				if (!(chr.opr_and(GameConstants.CHR_HENAN_GANG_KAI)).is_empty()
						&& has_rule(GameConstants.GAME_RULE_HENAN_GANGHUA_DOUBLE)) {
					s *= 2;
				}
				if (!(chr.opr_and(GameConstants.CHR_HENAN_QI_XIAO_DUI)).is_empty()
						&& has_rule(GameConstants.GAME_RULE_HENAN_QIDUI_DOUBLE)) {
					s *= 2;
				}
				if (!(chr.opr_and(GameConstants.CHR_HENAN_QISHOU_HU)).is_empty()) {
					s *= 2;
				}
				if (!(chr.opr_and(GameConstants.CHR_HENAN_HH_QI_XIAO_DUI)).is_empty()) {
					if (has_rule(GameConstants.GAME_RULE_HENAN_HAOQI)) {
						s *= 4;
					} else if (has_rule(GameConstants.GAME_RULE_HENAN_QIDUI_DOUBLE)) {
						s *= 2;
					}

				}

				if (hu_type == GameConstants.HU_CARD_TYPE_GANG_CI || hu_type == GameConstants.HU_CARD_TYPE_PI_CI) {
					if (ci_feng_double && _logic.get_ci_card_index() >= 27) {
						s *= 2;
					}
				}

				// 胡牌分
				// 包次算分
				if (has_rule(GameConstants.GAME_RULE_HENAN_BAOCI) && _bao_ci_state == GameConstants.LYGC_BAO_CI_SATRT
						&& hu_type == GameConstants.HU_CARD_TYPE_GANG_CI && bao_ci == true
						&& provide_index != seat_index) {
					GRR._chi_hu_rights[seat_index].opr_or(GameConstants.CHR_HENAN_BAO_CI_START);
					GRR._game_score[provide_index] -= s;
				} else {
					GRR._game_score[i] -= s;
				}
				GRR._game_score[seat_index] += s;

			}
		}
		////////////////////////////////////////////////////// 点炮 算分
		else if (hu_type == GameConstants.HU_CARD_TYPE_PAOHU) {
			float s = lChiHuScore;

			if (jia_di) {// 庄家加底
				if (zhuang_hu) {
					s *= 2;// 庄家杆 别人多输一分
				} else if (zhuang_fang_hu) {// 庄家放杆 庄家多输一分
					s *= 2;
				}
			}

			s += (_player_result.pao[provide_index] + _player_result.pao[seat_index]);

			if (!(chr.opr_and(GameConstants.CHR_HENAN_GANG_KAI)).is_empty()
					&& has_rule(GameConstants.GAME_RULE_HENAN_GANGHUA_DOUBLE)) {
				s *= 2;
			}
			if (!(chr.opr_and(GameConstants.CHR_HENAN_QI_XIAO_DUI)).is_empty()
					&& has_rule(GameConstants.GAME_RULE_HENAN_QIDUI_DOUBLE)) {
				s *= 2;
			}
			if (!(chr.opr_and(GameConstants.CHR_HENAN_QISHOU_HU)).is_empty()) {
				s *= 2;
			}

			if (!(chr.opr_and(GameConstants.CHR_HENAN_HH_QI_XIAO_DUI)).is_empty()) {
				if (has_rule(GameConstants.GAME_RULE_HENAN_HAOQI)) {
					s *= 4;
				} else if (has_rule(GameConstants.GAME_RULE_HENAN_QIDUI_DOUBLE)) {
					s *= 2;
				}

			}

			GRR._game_score[provide_index] -= s;
			GRR._game_score[seat_index] += s;

			GRR._chi_hu_rights[provide_index].opr_or(GameConstants.CHR_HENAN_LYGC_FANG_PAO);

			GRR.is_zi_mo_hu = false;
		}

		// 设置变量
		GRR._provider[seat_index] = provide_index;
		_status_gang = false;
		_status_gang_hou_pao = false;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);
		// _playerStatus[seat_index].clean_status();
	}

	public int get_chi_hu_action_rank_henan_xc(ChiHuRight chiHuRight) {
		int wFanShu = 1;

		boolean has_da_hu = false;
		if (!(chiHuRight.opr_and(GameConstants.CHR_HENAN_QI_XIAO_DUI)).is_empty()
				|| !(chiHuRight.opr_and(GameConstants.CHR_HENAN_HH_QI_XIAO_DUI)).is_empty()
				|| !(chiHuRight.opr_and(GameConstants.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI)).is_empty()
				|| !(chiHuRight.opr_and(GameConstants.CHR_HENAN_QISHOU_HU)).is_empty()) {
			wFanShu = 7;
			has_da_hu = true;
		}

		if (!has_da_hu) {
			if (getRuleValue(GameConstants.GAME_RULE_HENAN_QUE_MEN) == 1 && chiHuRight.duanmen_count > 1) {
				wFanShu += 1;
			}
		}

		// 风牌组合加番数,只能计算手上的
		wFanShu += chiHuRight.baifeng_count;
		wFanShu += chiHuRight.heifeng_count;

		return wFanShu;
	}

	/**
	 * 许昌麻将算分
	 * 
	 * @param seat_index
	 * @param provide_index
	 * @param operate_card
	 * @param zimo
	 */
	public void process_chi_hu_player_score_henan_xc(int seat_index, int provide_index, int operate_card,
			boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;

		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		countCardType(chr, seat_index);

		int wFanShu = get_chi_hu_action_rank_henan_xc(chr);

		int max_times = getRuleValue(GameConstants.GAME_RULE_MAX_TIMES);
		if (wFanShu > max_times) {
			wFanShu = max_times + 1;
		}

		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				GRR._lost_fan_shu[i][seat_index] = wFanShu;
			}
		} else {
			GRR._lost_fan_shu[provide_index][seat_index] = wFanShu;
		}

		float lChiHuScore = wFanShu;

		boolean zhuang_hu = (GRR._banker_player == seat_index ? true : false);
		boolean jia_di = has_rule(GameConstants.GAME_RULE_HENAN_JIA_DI);

		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}

				float s = lChiHuScore;
				s *= getRuleValue(GameConstants.GAME_RULE_BASE_SCORE);

				if (jia_di) {
					if (zhuang_hu) {
						s += 1;
					} else if (GRR._banker_player == i) {
						s += 1;
					}
				}

				s += (_player_result.pao[i] + _player_result.pao[seat_index]);

				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;
			}
		} else {
			float s = lChiHuScore;

			s *= getRuleValue(GameConstants.GAME_RULE_BASE_SCORE);

			GRR._game_score[provide_index] -= s;
			GRR._game_score[seat_index] += s;

			GRR._chi_hu_rights[provide_index].opr_or(GameConstants.CHR_FANG_PAO);
		}

		GRR._provider[seat_index] = provide_index;
		_status_gang = false;
		_status_gang_hou_pao = false;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);
	}

	/**
	 * 林州麻将算分
	 * 
	 * @param seat_index
	 * @param provide_index
	 * @param operate_card
	 * @param zimo
	 */
	public void process_chi_hu_player_score_lz(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int wFanShu = _logic.get_chi_hu_action_rank_lz(chr);// 番数

		// 统计
		if (zimo) {
			// 自摸
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				if (i == seat_index) {
					// GRR._hu_result[i] = MJGameConstants.HU_RESULT_ZIMO;
					continue;
				}

				GRR._lost_fan_shu[i][seat_index] = wFanShu;
			}
		} else {// 点炮
			GRR._lost_fan_shu[provide_index][seat_index] = wFanShu;//
		}

		float lChiHuScore = wFanShu;

		////////////////////////////////////////////////////// 自摸 算分
		if (zimo) {
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				if (i == seat_index) {
					continue;
				}

				float s = lChiHuScore * 2;
				// 跑
				s += (_player_result.pao[i] + _player_result.pao[seat_index]);
				// 胡牌分
				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;

			}
		}
		////////////////////////////////////////////////////// 点炮 算分
		else {
			float s = lChiHuScore;

			s += (_player_result.pao[provide_index] + _player_result.pao[seat_index]);

			// 跑和呛

			GRR._game_score[provide_index] -= s;
			GRR._game_score[seat_index] += s;

			GRR._chi_hu_rights[provide_index].opr_or(GameConstants.CHR_FANG_PAO);

		}

		// 设置变量
		GRR._provider[seat_index] = provide_index;
		_status_gang = false;
		_status_gang_hou_pao = false;

		_playerStatus[seat_index].clean_status();
	}

	/**
	 * 胡牌算分
	 * 
	 * @param seat_index
	 * @param provide_index
	 * @param operate_card
	 * @param zimo
	 */
	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {

		if (is_mj_type(GameConstants.GAME_TYPE_CS) || is_mj_type(GameConstants.GAME_TYPE_HALL_CHANG_SHA_MJ)
				|| is_mj_type(GameConstants.GAME_TYPE_THJ_CS)) {
			process_chi_hu_player_score_cs(seat_index, provide_index, zimo);
			return;
		}
		if (is_mj_type(GameConstants.GAME_TYPE_FLS_LX_CG)) {
			process_chi_hu_player_score_lxcg(seat_index, provide_index, operate_card, zimo);
			return;
		}
		if (is_mj_type(GameConstants.GAME_TYPE_HENAN_HZ) || is_mj_type(GameConstants.GAME_TYPE_NEW_HN_HONG_ZHONG)) {
			process_chi_hu_player_score_hnhz(seat_index, provide_index, operate_card, zimo);
			return;
		}
		if (is_mj_type(GameConstants.GAME_TYPE_XTHH)) {
			process_chi_hu_player_score_xthh(seat_index, provide_index, operate_card, zimo);
			return;
		}
		if (is_mj_type(GameConstants.GAME_TYPE_HENAN_AY) || is_mj_type(GameConstants.GAME_TYPE_NEW_AN_YANG)) {
			process_chi_hu_player_score_ay(seat_index, provide_index, operate_card, zimo);
			return;
		}
		if ((is_mj_type(GameConstants.GAME_TYPE_HENAN) || is_mj_type(GameConstants.GAME_TYPE_HENAN_ZMD)
				|| is_mj_type(GameConstants.GAME_TYPE_HENAN_KF) || is_mj_type(GameConstants.GAME_TYPE_HENAN_NY)
				|| is_mj_type(GameConstants.GAME_TYPE_HENAN_XX) || is_mj_type(GameConstants.GAME_TYPE_NEW_HE_NAN)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_ZHU_MA_DIAN)
				|| is_mj_type(GameConstants.GAME_TYPE_JIAO_ZUO_HUAN_HUAN)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_KAI_FENG) || is_mj_type(GameConstants.GAME_TYPE_NEW_NAN_YANG)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_XIN_XIANG))) {
			process_chi_hu_player_score_henan(seat_index, provide_index, operate_card, zimo);
			return;
		}
		if (is_mj_type(GameConstants.GAME_TYPE_HENAN_PDS) || is_mj_type(GameConstants.GAME_TYPE_NEW_PING_DING_SHAN)) {
			process_chi_hu_player_score_henan_pds(seat_index, provide_index, operate_card, zimo);
			return;
		}
		if (is_mj_type(GameConstants.GAME_TYPE_HENAN_LZ) || is_mj_type(GameConstants.GAME_TYPE_NEW_LIN_ZHOU)) {
			process_chi_hu_player_score_lz(seat_index, provide_index, operate_card, zimo);
			return;
		}
		if (is_mj_type(GameConstants.GAME_TYPE_HENAN_XC) || is_mj_type(GameConstants.GAME_TYPE_NEW_XU_CHANG)) {
			process_chi_hu_player_score_henan_xc(seat_index, provide_index, operate_card, zimo);
			return;
		}
		if (is_mj_type(GameConstants.GAME_TYPE_HENAN_ZHOU_KOU) || is_mj_type(GameConstants.GAME_TYPE_NEW_ZHOU_KOU)) {
			process_chi_hu_player_score_he_nan_zhou_kou(seat_index, provide_index, operate_card, zimo);
			return;
		}
		if (is_mj_type(GameConstants.GAME_TYPE_HENAN_LH) || is_mj_type(GameConstants.GAME_TYPE_NEW_LUO_HE)) {
			process_chi_hu_player_score_henan_lh(seat_index, provide_index, operate_card, zimo);
			return;
		}

		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int wFanShu = 0;// 番数
		if (_game_type_index == GameConstants.GAME_TYPE_ZZ
				|| _game_type_index == GameConstants.GAME_TYPE_HENAN_ZHUAN_ZHUAN
				|| is_mj_type(GameConstants.GAME_TYPE_HALL_ZHUAN_ZHUAN_MJ)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_SAN_MEN_XIA)) { // liuyan
			wFanShu = _logic.get_chi_hu_action_rank_zz(chr);
		} else if (is_mj_type(GameConstants.GAME_TYPE_HZ) || is_mj_type(GameConstants.GAME_TYPE_FLS_HZ_LX)
				|| is_mj_type(GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_YUYANG_HONGZHON)
				|| is_mj_type(GameConstants.GAME_TYPE_HONG_ZHONG_MJ_TH)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_HONG_ZHONG_FEI)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_PING_XIANG_ZZ)) {
			wFanShu = _logic.get_chi_hu_action_rank_hz(chr, getTablePlayerNumber()); // walkergeek
																						// 修改杠分三人场算分
		} else if (_game_type_index == GameConstants.GAME_TYPE_CS
				|| is_mj_type(GameConstants.GAME_TYPE_HALL_CHANG_SHA_MJ)
				|| is_mj_type(GameConstants.GAME_TYPE_THJ_CS)) {
			wFanShu = _logic.get_chi_hu_action_rank_cs(chr);
		} else if (_game_type_index == GameConstants.GAME_TYPE_SHUANGGUI
				|| is_mj_type(GameConstants.GAME_TYPE_HUNAN_SHANG_XIA_GUI)) {
			wFanShu = _logic.get_chi_hu_action_rank_sg(chr);
		} else if (_game_type_index == GameConstants.GAME_TYPE_ZHUZHOU) {
			if (has_rule(GameConstants.GAME_RULE_HUNAN_SCORE_MUTIP)) {
				wFanShu = _logic.get_chi_hu_action_rank_zhuzhou_mutip(chr);
			} else {
				wFanShu = _logic.get_chi_hu_action_rank_zhuzhou(chr);
			}
		} else if (is_mj_type(GameConstants.GAME_TYPE_FLS_LX_CG)) {
			wFanShu = _logic.get_chi_hu_action_rank_lxcg(chr);
		} else if (is_mj_type(GameConstants.GAME_TYPE_FLS_CS_LX) || is_mj_type(GameConstants.GAME_TYPE_FLS_CS_LX_DT)) {
			wFanShu = _logic.get_chi_hu_action_rank_cs(chr);
			if (has_rule(GameConstants.GAME_RULE_HUNAN_SCORE_TOP) && wFanShu > 60) {
				wFanShu = 60;
			}
		}

		countCardType(chr, seat_index);

		int lChiHuScore = wFanShu * GameConstants.CELL_SCORE;// wFanShu*m_pGameServiceOption->lCellScore;

		// 杠上炮,呼叫转移 如果开杠者掷骰子补张，补张的牌开杠者若不能胡而其他玩家可以胡属于杠上炮，若胡，则属于杠上开花
		if (!(chr.opr_and(GameConstants.CHR_HUNAN_GANG_SHANG_PAO).is_empty())) {
			/**
			 * int cbGangIndex = GRR._gang_score[_provide_player].gang_count -
			 * 1;// 最后的杠
			 * 
			 * GRR._hu_result[_provide_player] =
			 * MJGameConstants.HU_RESULT_FANGPAO; int cbChiHuCount = 0; for (int
			 * i = 0; i < MJGameConstants.GAME_PLAYER; i++) { if
			 * (GRR._chi_hu_rights[i].is_valid()){//这个胡是有效的 cbChiHuCount++;
			 * GRR._hu_result[i] = MJGameConstants.HU_RESULT_JIEPAO; } }
			 * 
			 * //处理杠的分数 if (cbChiHuCount == 1) { int lScore =
			 * GRR._gang_score[_provide_player].scores[cbGangIndex][seat_index];
			 * GRR._gang_score[_provide_player].scores[cbGangIndex][seat_index]
			 * =
			 * GRR._gang_score[_provide_player].scores[cbGangIndex][_provide_player];
			 * GRR._gang_score[_provide_player].scores[cbGangIndex][_provide_player]
			 * = lScore;
			 * 
			 * } else { // 一炮多响的情况下,胡牌者平分杠得分
			 * 
			 * int lGangScore =
			 * GRR._gang_score[_provide_player].scores[cbGangIndex][_provide_player]/
			 * cbChiHuCount; // 不能小于一番 lGangScore = Math.max(lGangScore,
			 * MJGameConstants.CELL_SCORE); for (int i = 0; i <
			 * MJGameConstants.GAME_PLAYER; i++) { if
			 * (GRR._chi_hu_rights[i].is_valid())
			 * GRR._gang_score[_provide_player].scores[cbGangIndex][i] =
			 * lGangScore; }
			 * GRR._gang_score[_provide_player].scores[cbGangIndex][_provide_player]
			 * = 0;//自己的清空掉 for (int i = 0; i < MJGameConstants.GAME_PLAYER;
			 * i++) { if (i != _provide_player)
			 * GRR._gang_score[_provide_player].scores[cbGangIndex][_provide_player]
			 * += GRR._gang_score[_provide_player].scores[cbGangIndex][i]; }
			 * 
			 * _banker_select = _provide_player; }
			 **/
		}
		// 抢杠杠分不算 玩家在明杠的时候，其他玩家可以胡被杠的此张牌，叫抢杠胡
		else if (!(chr.opr_and(GameConstants.CHR_HUNAN_QIANG_GANG_HU).is_empty())) {
			// GRR._gang_score[_provide_player].gang_count--;//这个杠就不算了
			// _player_result.ming_gang_count[_provide_player]--;

		}

		int real_provide_index = GameConstants.INVALID_SEAT;
		if (is_mj_type(GameConstants.GAME_TYPE_ZHUZHOU)) {
			// 如果有清一色/碰碰胡
			if (!(chr.opr_and(GameConstants.CHR_HUNAN_QING_YI_SE).is_empty())) {
				// 算坎
				// 第三坎
				if (GRR._weave_count[seat_index] > 2) {
					if (GRR._weave_items[seat_index][2].provide_player != seat_index) {
						real_provide_index = GRR._weave_items[seat_index][2].provide_player;
					}
				}
				// 第四坎
				if (GRR._weave_count[seat_index] > 3) {
					if (GRR._weave_items[seat_index][3].provide_player != seat_index) {
						real_provide_index = GRR._weave_items[seat_index][3].provide_player;
					}
				}
			}
		}

		// 统计
		if (zimo) {
			// 自摸
			// _player_result.zi_mo_count[seat_index]++;
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				// GRR._hu_result[i] = MJGameConstants.HU_RESULT_NULL;
				if (i == seat_index) {
					// GRR._hu_result[i] = MJGameConstants.HU_RESULT_ZIMO;
					continue;
				}

				if (real_provide_index == GameConstants.INVALID_SEAT) {
					GRR._lost_fan_shu[i][seat_index] = wFanShu;
				} else {
					// 全包
					GRR._lost_fan_shu[real_provide_index][seat_index] = wFanShu;
				}

			}
		} else {// 点炮
			GRR._lost_fan_shu[provide_index][seat_index] = wFanShu;//

			// GRR._hu_result[provide_index] =
			// MJGameConstants.HU_RESULT_FANGPAO;
			// GRR._hu_result[seat_index] = MJGameConstants.HU_RESULT_JIEPAO;
		}

		/////////////////////////////////////////////// 算分//////////////////////////
		int tmp_niao_count = GRR._count_pick_niao;
		if (!is_mj_type(GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ)
				&& !is_mj_type(GameConstants.GAME_TYPE_HONG_ZHONG_MJ_TH)
				&& !is_mj_type(GameConstants.GAME_TYPE_MJ_YUYANG_HONGZHON)
				&& !is_mj_type(GameConstants.GAME_TYPE_HALL_ZHUAN_ZHUAN_MJ)
				&& !is_mj_type(GameConstants.GAME_TYPE_MJ_HONG_ZHONG_FEI)
				&& !is_mj_type(GameConstants.GAME_TYPE_MJ_PING_XIANG_ZZ))
			GRR._count_pick_niao = 0;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			for (int j = 0; j < GRR._player_niao_count[i]; j++) {
				//////////////////////////////////////////////// 转转麻将抓鸟//////////////////只要159就算
				if ((GameConstants.GAME_TYPE_ZZ == _game_type_index) || is_mj_type(GameConstants.GAME_TYPE_HZ)
						|| is_mj_type(GameConstants.GAME_TYPE_FLS_HZ_LX)
						|| is_mj_type(GameConstants.GAME_TYPE_SHUANGGUI)
						|| is_mj_type(GameConstants.GAME_TYPE_HENAN_ZHUAN_ZHUAN)
						|| is_mj_type(GameConstants.GAME_TYPE_HUNAN_SHANG_XIA_GUI)
						|| is_mj_type(GameConstants.GAME_TYPE_NEW_SAN_MEN_XIA)) { // liuyan
																					// 2017/7/10
					if ((is_mj_type(GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ)
							|| is_mj_type(GameConstants.GAME_TYPE_MJ_YUYANG_HONGZHON)
							|| is_mj_type(GameConstants.GAME_TYPE_HONG_ZHONG_MJ_TH)
							|| is_mj_type(GameConstants.GAME_TYPE_MJ_HONG_ZHONG_FEI)
							|| is_mj_type(GameConstants.GAME_TYPE_MJ_PING_XIANG_ZZ))
							&& (has_rule_ex(GameConstants.GAME_RULE_HUNAN_YI_MA_QUAN_ZHONG)
									|| has_rule_ex(GameConstants.GAME_RULE_HUNAN_MO_JI_JIANG_JI))) {
						GRR._count_pick_niao = tmp_niao_count;
						GRR._player_niao_cards[i][j] = this.set_ding_niao_valid(GRR._player_niao_cards[i][j], true);
					} else {
						int nValue = GRR._player_niao_cards[i][j];
						nValue = nValue > 1000 ? (nValue - 1000) : nValue;
						int v = _logic.get_card_value(nValue);
						if (v == 1 || v == 5 || v == 9) {
							GRR._count_pick_niao++;
							GRR._player_niao_cards[i][j] = this.set_ding_niao_valid(GRR._player_niao_cards[i][j], true);// 胡牌的鸟生效
						} else {
							GRR._player_niao_cards[i][j] = this.set_ding_niao_valid(GRR._player_niao_cards[i][j],
									false);// 胡牌的鸟失效
						}
					}
				} else if (GameConstants.GAME_TYPE_ZHUZHOU == _game_type_index) {
					//////////////////////////////////////////////// 长沙麻将抓鸟//////////////////
					if (zimo) {
						GRR._count_pick_niao++;
						GRR._player_niao_cards[i][j] = this.set_ding_niao_valid(GRR._player_niao_cards[i][j], true);// 胡牌的鸟生效
					} else {
						if (seat_index == i || provide_index == i) {// 自己还有放炮的人有效
							GRR._count_pick_niao++;
							GRR._player_niao_cards[i][j] = this.set_ding_niao_valid(GRR._player_niao_cards[i][j], true);// 胡牌的鸟生效
						} else {
							GRR._player_niao_cards[i][j] = this.set_ding_niao_valid(GRR._player_niao_cards[i][j],
									false);// 胡牌的鸟失效
						}
					}
				} else if (is_mj_type(GameConstants.GAME_TYPE_HALL_ZHUAN_ZHUAN_MJ)) {
					// if (i == seat_index) {
					// GRR._count_pick_niao++;
					// GRR._player_niao_cards[i][j] =
					// this.set_ding_niao_valid(GRR._player_niao_cards[i][j],
					// true);// 胡牌的鸟生效
					// } else {
					// GRR._player_niao_cards[i][j] =
					// this.set_ding_niao_valid(GRR._player_niao_cards[i][j],
					// false);// 胡牌的鸟失效
					// }
				}
			}
		}

		////////////////////////////////////////////////////// 自摸 算分
		if (zimo) {
			if (is_mj_type(GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ)
					|| is_mj_type(GameConstants.GAME_TYPE_HALL_ZHUAN_ZHUAN_MJ)
					|| is_mj_type(GameConstants.GAME_TYPE_MJ_YUYANG_HONGZHON)
					|| is_mj_type(GameConstants.GAME_TYPE_HONG_ZHONG_MJ_TH)
					|| is_mj_type(GameConstants.GAME_TYPE_MJ_HONG_ZHONG_FEI)
					|| is_mj_type(GameConstants.GAME_TYPE_MJ_PING_XIANG_ZZ)) {
				hu_type[seat_index] = GameConstants.ZI_MO;
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}

				int s = lChiHuScore;

				// WalkerGeek 胡牌分记录
				if (is_mj_type(GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ)
						|| is_mj_type(GameConstants.GAME_TYPE_HALL_ZHUAN_ZHUAN_MJ)
						|| is_mj_type(GameConstants.GAME_TYPE_MJ_YUYANG_HONGZHON)
						|| is_mj_type(GameConstants.GAME_TYPE_HONG_ZHONG_MJ_TH)
						|| is_mj_type(GameConstants.GAME_TYPE_MJ_HONG_ZHONG_FEI)
						|| is_mj_type(GameConstants.GAME_TYPE_MJ_PING_XIANG_ZZ)) {
					hu_score[i] -= s;
					hu_score[seat_index] += s;
				}

				if (this.is_mj_type(GameConstants.GAME_TYPE_ZZ)
						|| this.is_mj_type(GameConstants.GAME_TYPE_HENAN_ZHUAN_ZHUAN)
						|| is_mj_type(GameConstants.GAME_TYPE_NEW_SAN_MEN_XIA)) { // liuyan
																					// 2017/7/10
					if (has_rule(GameConstants.GAME_RULE_HUNAN_ZHUANG_XIAN)
							&& (i == GRR._banker_player || seat_index == GRR._banker_player)) {
						s += 1;
					}

					//////////////////////////////////////////////// 转转麻将自摸算分//////////////////
					s += GRR._count_pick_niao;// 只算自己的
				} else if (is_mj_type(GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ)
						|| is_mj_type(GameConstants.GAME_TYPE_MJ_YUYANG_HONGZHON)
						|| is_mj_type(GameConstants.GAME_TYPE_HONG_ZHONG_MJ_TH)
						|| is_mj_type(GameConstants.GAME_TYPE_MJ_HONG_ZHONG_FEI)
						|| is_mj_type(GameConstants.GAME_TYPE_MJ_PING_XIANG_ZZ)) {
					int niao_fen = 2;
					if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_NIAO_FEN_1))
						niao_fen = 1;

					niao_score[i] -= GRR._count_pick_niao * niao_fen;
					niao_score[seat_index] += GRR._count_pick_niao * niao_fen;
					s += GRR._count_pick_niao * niao_fen;
				} else if (is_mj_type(GameConstants.GAME_TYPE_HZ) || is_mj_type(GameConstants.GAME_TYPE_FLS_HZ_LX)) {
					s += GRR._count_pick_niao * 2;

				} else if (is_mj_type(GameConstants.GAME_TYPE_SHUANGGUI)
						|| is_mj_type(GameConstants.GAME_TYPE_HUNAN_SHANG_XIA_GUI)) {
					s += GRR._count_pick_niao * 2;

				} else if (this.is_mj_type(GameConstants.GAME_TYPE_CS)
						|| is_mj_type(GameConstants.GAME_TYPE_HALL_CHANG_SHA_MJ)
						|| is_mj_type(GameConstants.GAME_TYPE_THJ_CS)) {
					//////////////////////////////////////////////// 长沙麻将自摸算分//////////////////
					if (this.is_zhuang_xian()) {
						if ((GRR._banker_player == i) || (GRR._banker_player == seat_index)) {
							int zx = lChiHuScore / 6;
							s += (zx == 0 ? 1 : zx);
						}
					}
					GRR._player_niao_invalid[i] = 1;// 庄家鸟生效

					int niao = GRR._player_niao_count[seat_index] + GRR._player_niao_count[i];
					if (niao > 0) {
						s *= (niao + 1);
					}
				} else if (this.is_mj_type(GameConstants.GAME_TYPE_ZHUZHOU)) {
					//////////////////////////////////////////////// 株洲麻将自摸算分//////////////////
					GRR._player_niao_invalid[i] = 1;// 庄家鸟生效

					int niao = GRR._player_niao_count[seat_index] + GRR._player_niao_count[i];
					if (niao > 0) {
						s += niao;
					}
				} else if (is_mj_type(GameConstants.GAME_TYPE_HALL_ZHUAN_ZHUAN_MJ)) {
					if (has_rule(GameConstants.GAME_RULE_HUNAN_ZHUANG_XIAN)
							&& (i == GRR._banker_player || seat_index == GRR._banker_player)) {
						s += 1;
					}

					//////////////////////////////////////////////// 转转麻将自摸算分//////////////////
					if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_ZHUANG_NIAO)) {
						s += GRR._player_niao_count[seat_index] + GRR._player_niao_count[i];
						niao_score[i] -=  GRR._player_niao_count[seat_index] + GRR._player_niao_count[i];
						niao_score[seat_index] +=  GRR._player_niao_count[seat_index] + GRR._player_niao_count[i];
					} else {
						s += GRR._count_pick_niao;// 只算自己的
						niao_score[i] -= GRR._count_pick_niao ;
						niao_score[seat_index] += GRR._count_pick_niao;
					}
				}

				
				
				// WalkerGeek 湖南红中添加飘分选项
				if ((is_mj_type(GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ)
						|| is_mj_type(GameConstants.GAME_TYPE_MJ_YUYANG_HONGZHON)
						|| is_mj_type(GameConstants.GAME_TYPE_HONG_ZHONG_MJ_TH)
						|| is_mj_type(GameConstants.GAME_TYPE_MJ_HONG_ZHONG_FEI)
						|| is_mj_type(GameConstants.GAME_TYPE_MJ_PING_XIANG_ZZ))
						&& has_rule(GameConstants.GAME_RULE_HUNAN_CS_PIAO)) {
					int piao = (_player_result.pao[i] + _player_result.pao[seat_index]);
					s += piao;
					piao_score[i] -= piao;
					piao_score[seat_index] += piao;
				}
				//WalkerGeek 8.10 红中麻将添加一个倍率
				if(is_mj_type(GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ)){
					s *= get_bei_lv();
				}

				// WalkerGeek 红中比赛场分等比增加
				if (is_match()) {
					if (is_mj_type(GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ)
							|| is_mj_type(GameConstants.GAME_TYPE_HALL_ZHUAN_ZHUAN_MJ)
							|| is_mj_type(GameConstants.GAME_TYPE_MJ_YUYANG_HONGZHON)
							|| is_mj_type(GameConstants.GAME_TYPE_HONG_ZHONG_MJ_TH)
							|| is_mj_type(GameConstants.GAME_TYPE_MJ_HONG_ZHONG_FEI)
							|| is_mj_type(GameConstants.GAME_TYPE_MJ_PING_XIANG_ZZ)) {
						s *= getSettleBase(seat_index);
					}
				}
				// 胡牌分
				if (real_provide_index == GameConstants.INVALID_SEAT) {
					GRR._game_score[i] -= s;
				} else {
					int niao = GRR._player_niao_count[seat_index] + GRR._player_niao_count[i];
					if (niao > 0) {
						s -= niao;// 鸟要最后处理,把上面加的鸟分减掉 ----先这样处理--年后拆分出来
					}
					if (i == getTablePlayerNumber() - 1) {// 循环到最后一次 才把鸟分加上
						niao = GRR._player_niao_count[seat_index] + GRR._player_niao_count[real_provide_index];
						if (niao > 0) {
							s += niao;
						}
					}

					// 全包
					GRR._game_score[real_provide_index] -= s;
				}
				GRR._game_score[seat_index] += s;
			}
		}
		////////////////////////////////////////////////////// 点炮 算分
		else {
			int s = lChiHuScore;
			// WalkerGeek 胡牌分记录
			if (is_mj_type(GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ)
					|| is_mj_type(GameConstants.GAME_TYPE_MJ_YUYANG_HONGZHON)
					|| is_mj_type(GameConstants.GAME_TYPE_HALL_ZHUAN_ZHUAN_MJ)
					|| is_mj_type(GameConstants.GAME_TYPE_HONG_ZHONG_MJ_TH)
					|| is_mj_type(GameConstants.GAME_TYPE_MJ_HONG_ZHONG_FEI)
					|| is_mj_type(GameConstants.GAME_TYPE_MJ_PING_XIANG_ZZ)) {
				hu_score[provide_index] -= s;
				hu_score[seat_index] += s;
			}

			if (this.is_zhuang_xian()) {
				if ((GRR._banker_player == provide_index) || (GRR._banker_player == seat_index)) {
					int zx = GRR._chi_hu_rights[seat_index].da_hu_count;// lChiHuScore/6;
					s += (zx == 0 ? 1 : zx);
				}
			}

			if ((GameConstants.GAME_TYPE_HENAN_ZHUAN_ZHUAN == _game_type_index
					|| is_mj_type(GameConstants.GAME_TYPE_NEW_SAN_MEN_XIA))
					&& this.has_rule(GameConstants.GAME_RULE_HUNAN_ZHUANG_XIAN)) {
				if ((GRR._banker_player == provide_index) || (GRR._banker_player == seat_index)) {
					s += 1;
				}
			}

			if (GameConstants.GAME_TYPE_ZZ == _game_type_index
					|| GameConstants.GAME_TYPE_HENAN_ZHUAN_ZHUAN == _game_type_index
					|| is_mj_type(GameConstants.GAME_TYPE_NEW_SAN_MEN_XIA)) { // liuyan
																				// 2017/7/10
				if (has_rule(GameConstants.GAME_RULE_HUNAN_ZHUANG_XIAN)) {
					if ((GRR._banker_player == provide_index) || (GRR._banker_player == seat_index)) {
						s += 1;
					}
				}
				s += GRR._count_pick_niao;// 只算自己的

			} else if (is_mj_type(GameConstants.GAME_TYPE_HZ) || is_mj_type(GameConstants.GAME_TYPE_FLS_HZ_LX)
					|| is_mj_type(GameConstants.GAME_TYPE_MJ_YUYANG_HONGZHON)
					|| is_mj_type(GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ)
					|| is_mj_type(GameConstants.GAME_TYPE_HONG_ZHONG_MJ_TH)
					|| is_mj_type(GameConstants.GAME_TYPE_MJ_HONG_ZHONG_FEI)
					|| is_mj_type(GameConstants.GAME_TYPE_MJ_PING_XIANG_ZZ)) {
				// 如果是抢杠胡
				if (!(chr.opr_and(GameConstants.CHR_HUNAN_QIANG_GANG_HU).is_empty())) {
					if (is_mj_type(GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ)
							|| is_mj_type(GameConstants.GAME_TYPE_MJ_YUYANG_HONGZHON)
							|| is_mj_type(GameConstants.GAME_TYPE_HONG_ZHONG_MJ_TH)
							|| is_mj_type(GameConstants.GAME_TYPE_MJ_HONG_ZHONG_FEI)
							|| is_mj_type(GameConstants.GAME_TYPE_MJ_PING_XIANG_ZZ)) {
						hu_type[seat_index] = GameConstants.QIANG_GANG_HU;
						hu_type[provide_index] = GameConstants.QIANG_GANG_HU_ALL;
					}

					// 这个玩家全包
					for (int i = 0; i < getTablePlayerNumber(); i++) {
						if (i == seat_index) {
							continue;
						}
						if (is_mj_type(GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ)
								|| is_mj_type(GameConstants.GAME_TYPE_MJ_YUYANG_HONGZHON)
								|| is_mj_type(GameConstants.GAME_TYPE_HONG_ZHONG_MJ_TH)
								|| is_mj_type(GameConstants.GAME_TYPE_MJ_HONG_ZHONG_FEI)
								|| is_mj_type(GameConstants.GAME_TYPE_MJ_PING_XIANG_ZZ)) {
							int niao_fen = 2;
							if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_NIAO_FEN_1))
								niao_fen = 1;

							int niao = GRR._count_pick_niao * niao_fen;
							niao_score[provide_index] -= niao;
							niao_score[seat_index] += niao;
							s += niao;
						} else {
							s += GRR._count_pick_niao * 2;//
						}
					}
				} else {
					if (is_mj_type(GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ)
							|| is_mj_type(GameConstants.GAME_TYPE_MJ_YUYANG_HONGZHON)
							|| is_mj_type(GameConstants.GAME_TYPE_HONG_ZHONG_MJ_TH)
							|| is_mj_type(GameConstants.GAME_TYPE_MJ_HONG_ZHONG_FEI)
							|| is_mj_type(GameConstants.GAME_TYPE_MJ_PING_XIANG_ZZ)) {
						int niao_fen = 2;
						if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_NIAO_FEN_1))
							niao_fen = 1;

						int niao = GRR._count_pick_niao * niao_fen;
						niao_score[provide_index] -= niao;
						niao_score[seat_index] += niao;
						s += niao;
					} else {
						s += GRR._count_pick_niao * 2;//
					}
				}

			} else if (GameConstants.GAME_TYPE_CS == _game_type_index
					|| is_mj_type(GameConstants.GAME_TYPE_HALL_CHANG_SHA_MJ)
					|| is_mj_type(GameConstants.GAME_TYPE_THJ_CS)) {
				int niao = GRR._player_niao_count[seat_index] + GRR._player_niao_count[provide_index];
				if (niao > 0) {
					s *= (niao + 1);
				}
			} else if (GameConstants.GAME_TYPE_ZHUZHOU == _game_type_index) {

			} else if (is_mj_type(GameConstants.GAME_TYPE_HALL_ZHUAN_ZHUAN_MJ)) {
				if (has_rule(GameConstants.GAME_RULE_HUNAN_ZHUANG_XIAN)) {
					if ((GRR._banker_player == provide_index) || (GRR._banker_player == seat_index)) {
						s += 1;
					}
				}
				if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_ZHUANG_NIAO)) {
					s += GRR._player_niao_count[seat_index] + GRR._player_niao_count[provide_index];
					niao_score[provide_index] -= GRR._player_niao_count[seat_index] + GRR._player_niao_count[provide_index];
					niao_score[seat_index] += GRR._player_niao_count[seat_index] + GRR._player_niao_count[provide_index];
				} else {
					s += GRR._count_pick_niao;// 只算自己的
					niao_score[provide_index] -= GRR._count_pick_niao;
					niao_score[seat_index] += GRR._count_pick_niao;
				}
			}

			// WalkerGeek 湖南红中添加飘分选项
			if ((is_mj_type(GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ)
					|| is_mj_type(GameConstants.GAME_TYPE_HALL_ZHUAN_ZHUAN_MJ)
					|| is_mj_type(GameConstants.GAME_TYPE_MJ_YUYANG_HONGZHON)
					|| is_mj_type(GameConstants.GAME_TYPE_HONG_ZHONG_MJ_TH)
					|| is_mj_type(GameConstants.GAME_TYPE_MJ_HONG_ZHONG_FEI)
					|| is_mj_type(GameConstants.GAME_TYPE_MJ_PING_XIANG_ZZ))
					&& has_rule(GameConstants.GAME_RULE_HUNAN_CS_PIAO)) {
				int piao = (_player_result.pao[provide_index] + _player_result.pao[seat_index]);
				s += piao;
				piao_score[provide_index] -= piao;
				piao_score[seat_index] += piao;
			}

			//WalkerGeek 8.10 红中麻将添加一个倍率
			if(is_mj_type(GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ)){
				s *= get_bei_lv();
			}
			// WalkerGeek 红中比赛场分等比增加
			if (is_match()) {
				if (is_mj_type(GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ)
						|| is_mj_type(GameConstants.GAME_TYPE_HALL_ZHUAN_ZHUAN_MJ)
						|| is_mj_type(GameConstants.GAME_TYPE_MJ_YUYANG_HONGZHON)
						|| is_mj_type(GameConstants.GAME_TYPE_HONG_ZHONG_MJ_TH)
						|| is_mj_type(GameConstants.GAME_TYPE_MJ_HONG_ZHONG_FEI)
						|| is_mj_type(GameConstants.GAME_TYPE_MJ_PING_XIANG_ZZ)) {
					s *= getSettleBase(seat_index);
				}
			}
			if (_game_type_index == GameConstants.GAME_TYPE_ZHUZHOU) {
				if (real_provide_index == GameConstants.INVALID_SEAT) {
					int niao = GRR._player_niao_count[seat_index] + GRR._player_niao_count[provide_index];
					if (niao > 0) {
						s += niao;
					}
					GRR._game_score[provide_index] -= s;
				} else {
					s *= 3;
					int niao = GRR._player_niao_count[seat_index] + GRR._player_niao_count[real_provide_index];
					if (niao > 0) {
						s += niao;
					}
					GRR._game_score[real_provide_index] -= s;
				}
			} else {
				if (real_provide_index == GameConstants.INVALID_SEAT) {
					GRR._game_score[provide_index] -= s;
				} else {
					s *= 3;
					GRR._game_score[provide_index] -= s;
				}
			}

			GRR._game_score[seat_index] += s;

			// 点炮的时候，删掉这张牌显示
			// GRR._cards_index[seat_index][_logic.switch_to_card_index(_provide_card)]--;
			GRR._chi_hu_rights[provide_index].opr_or(GameConstants.CHR_FANG_PAO);

		}

		if (real_provide_index == GameConstants.INVALID_SEAT) {
			GRR._provider[seat_index] = provide_index;
		} else {
			GRR._provider[seat_index] = real_provide_index;
			GRR._hu_result[real_provide_index] = GameConstants.HU_RESULT_FANG_KAN_QUAN_BAO;
		}
		// 设置变量

		_status_gang = false;
		_status_gang_hou_pao = false;

		// _playerStatus[seat_index].clean_status();
		change_player_status(seat_index, GameConstants.INVALID_VALUE);
		return;
	}

	public void process_chi_hu_player_score_lxcg(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int wFanShu = 0;// 番数
		wFanShu = _logic.get_chi_hu_action_rank_lxcg(chr);
		countCardType(chr, seat_index);
		int lChiHuScore = wFanShu * GameConstants.CELL_SCORE;// wFanShu*m_pGameServiceOption->lCellScore;

		// 统计
		if (zimo) {
			// 自摸
			// _player_result.zi_mo_count[seat_index]++;
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				// GRR._hu_result[i] = MJGameConstants.HU_RESULT_NULL;
				if (i == seat_index) {
					// GRR._hu_result[i] = MJGameConstants.HU_RESULT_ZIMO;
					continue;
				}

				GRR._lost_fan_shu[i][seat_index] = wFanShu;

			}
		} else {// 点炮
			GRR._lost_fan_shu[provide_index][seat_index] = wFanShu;//
		}

		/////////////////////////////////////////////// 算分//////////////////////////
		GRR._count_pick_niao = 0;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			for (int j = 0; j < GRR._player_niao_count[i]; j++) {
				//////////////////////////////////////////////// 转转麻将抓鸟//////////////////只要159就算
				int nValue = GRR._player_niao_cards[i][j];
				nValue = nValue > 1000 ? (nValue - 1000) : nValue;
				int v = _logic.get_card_value(nValue);
				if (v == 1 || v == 5 || v == 9) {
					GRR._count_pick_niao++;
					GRR._player_niao_cards[i][j] = this.set_ding_niao_valid(GRR._player_niao_cards[i][j], true);// 胡牌的鸟生效
				} else {
					GRR._player_niao_cards[i][j] = this.set_ding_niao_valid(GRR._player_niao_cards[i][j], false);// 胡牌的鸟失效
				}

			}
		}

		////////////////////////////////////////////////////// 自摸 算分
		if (zimo) {
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				if (i == seat_index) {
					continue;
				}

				int s = lChiHuScore;
				s += GRR._count_pick_niao * 2;
				s += (_player_result.pao[i] + _player_result.pao[seat_index]);

				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;
			}
		}
		////////////////////////////////////////////////////// 点炮 算分
		else {
			int s = lChiHuScore;
			// if (this.is_zhuang_xian()) {
			// if ((GRR._banker_player == provide_index) || (GRR._banker_player
			// == seat_index)) {
			// int zx = GRR._chi_hu_rights[seat_index].da_hu_count;//
			// lChiHuScore/6;
			// s += (zx == 0 ? 1 : zx);
			// }
			// }
			// 如果是抢杠胡
			if (!(chr.opr_and(GameConstants.CHR_HENAN_QIANG_GANG_HU).is_empty())) {
				// 这个玩家全包
				for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
					if (i == seat_index) {
						continue;
					}
					s += GRR._count_pick_niao * 2;//
				}
			} else {
				s += GRR._count_pick_niao * 2;//
			}
			s += (_player_result.pao[provide_index] + _player_result.pao[seat_index]);

			GRR._game_score[provide_index] -= s;

			GRR._game_score[seat_index] += s;

			// 点炮的时候，删掉这张牌显示
			// GRR._cards_index[seat_index][_logic.switch_to_card_index(_provide_card)]--;

			GRR._chi_hu_rights[provide_index].opr_or(GameConstants.CHR_FANG_PAO);

		}

		GRR._provider[seat_index] = provide_index;
		// 设置变量

		_status_gang = false;
		_status_gang_hou_pao = false;

		// _playerStatus[seat_index].clean_status();
		change_player_status(seat_index, GameConstants.INVALID_VALUE);
		return;
	}

	/**
	 * 红中
	 * 
	 * @param seat_index
	 * @param provide_index
	 * @param operate_card
	 * @param zimo
	 */
	public void process_chi_hu_player_score_hnhz(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int wFanShu = 0;// 番数
		wFanShu = _logic.get_chi_hu_action_rank_hnhz(chr);
		countCardType(chr, seat_index);
		int lChiHuScore = wFanShu * GameConstants.CELL_SCORE;// wFanShu*m_pGameServiceOption->lCellScore;

		// 统计
		if (zimo) {
			// 自摸
			// _player_result.zi_mo_count[seat_index]++;
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				// GRR._hu_result[i] = MJGameConstants.HU_RESULT_NULL;
				if (i == seat_index) {
					// GRR._hu_result[i] = MJGameConstants.HU_RESULT_ZIMO;
					continue;
				}

				GRR._lost_fan_shu[i][seat_index] = wFanShu;

			}
		} else {// 点炮
			GRR._lost_fan_shu[provide_index][seat_index] = wFanShu;//
		}

		/////////////////////////////////////////////// 算分//////////////////////////
		GRR._count_pick_niao = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GRR._player_niao_count[i]; j++) {
				//////////////////////////////////////////////// 转转麻将抓鸟//////////////////只要159就算
				int nValue = GRR._player_niao_cards[i][j];
				nValue = nValue > 1000 ? (nValue - 1000) : nValue;
				int v = _logic.get_card_value(nValue);
				if (v == 1 || v == 5 || v == 9) {
					GRR._count_pick_niao++;
					GRR._player_niao_cards[i][j] = this.set_ding_niao_valid(GRR._player_niao_cards[i][j], true);// 胡牌的鸟生效
				} else {
					GRR._player_niao_cards[i][j] = this.set_ding_niao_valid(GRR._player_niao_cards[i][j], false);// 胡牌的鸟失效
				}

			}
		}

		////////////////////////////////////////////////////// 自摸 算分
		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}

				int s = lChiHuScore;
				s += GRR._count_pick_niao * 2;
				s += (_player_result.pao[i] + _player_result.pao[seat_index]);

				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;
			}
		}
		////////////////////////////////////////////////////// 点炮 算分
		else {
			int s = lChiHuScore;
			// if (this.is_zhuang_xian()) {
			// if ((GRR._banker_player == provide_index) || (GRR._banker_player
			// == seat_index)) {
			// int zx = GRR._chi_hu_rights[seat_index].da_hu_count;//
			// lChiHuScore/6;
			// s += (zx == 0 ? 1 : zx);
			// }
			// }
			// 如果是抢杠胡
			if (!(chr.opr_and(GameConstants.CHR_HENAN_QIANG_GANG_HU).is_empty())) {
				// 这个玩家全包
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (i == seat_index) {
						continue;
					}
					s += GRR._count_pick_niao * 2;//
				}
			} else {
				s += GRR._count_pick_niao * 2;//
			}
			s += (_player_result.pao[provide_index] + _player_result.pao[seat_index]);

			GRR._game_score[provide_index] -= s;

			GRR._game_score[seat_index] += s;

			// 点炮的时候，删掉这张牌显示
			// GRR._cards_index[seat_index][_logic.switch_to_card_index(_provide_card)]--;

			GRR._chi_hu_rights[provide_index].opr_or(GameConstants.CHR_FANG_PAO);

		}

		GRR._provider[seat_index] = provide_index;
		// 设置变量

		_status_gang = false;
		_status_gang_hou_pao = false;

		// _playerStatus[seat_index].clean_status();
		change_player_status(seat_index, GameConstants.INVALID_VALUE);
		return;
	}

	/**
	 * 长沙胡牌算分
	 * 
	 * @param seat_index
	 * @param provide_index
	 * @param operate_card
	 * @param zimo
	 */
	public void process_chi_hu_player_score_cs_lx(int seat_index, int provide_index, boolean zimo) {
		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int wFanShu = 0;// 番数
		wFanShu = _logic.get_chi_hu_action_rank_cs_lx(chr);

		// if
		// (!(chr.opr_and(GameConstants.CHR_HUNAN_SHUANG_GANG_SHANG_PAO)).is_empty())
		// {
		// wFanShu *= 2;
		// }
		// if
		// (!(chr.opr_and(GameConstants.CHR_HUNAN_SHUANG_GANG_KAI)).is_empty())
		// {
		// wFanShu *= 2;
		// }

		countCardType(chr, seat_index);
		int lChiHuScore = wFanShu * GameConstants.CELL_SCORE;// wFanShu*m_pGameServiceOption->lCellScore;

		// 统计
		if (zimo) {
			// 自摸
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				GRR._lost_fan_shu[i][seat_index] = wFanShu;
			}
		} else {// 点炮
			GRR._lost_fan_shu[provide_index][seat_index] = wFanShu;//

		}

		int zuoPiao = getZuoPiaoScore();
		boolean isMutlpNiap = isMutlpDingNiao();// 是否乘法定鸟

		/////////////////////////////////////////////// 算分//////////////////////////
		GRR._count_pick_niao = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GRR._player_niao_count[i]; j++) {
				//////////////////////////////////////////////// 长沙麻将抓鸟//////////////////
				if (zimo) {
					GRR._count_pick_niao++;
					GRR._player_niao_cards[i][j] = this.set_ding_niao_valid(GRR._player_niao_cards[i][j], true);// 胡牌的鸟生效

				} else {
					if (seat_index == i || provide_index == i) {// 自己还有放炮的人有效
						GRR._count_pick_niao++;
						GRR._player_niao_cards[i][j] = this.set_ding_niao_valid(GRR._player_niao_cards[i][j], true);// 胡牌的鸟生效
					} else {
						GRR._player_niao_cards[i][j] = this.set_ding_niao_valid(GRR._player_niao_cards[i][j], false);// 胡牌的鸟生效
					}
				}

			}
		}

		// 飞鸟
		GRR._count_pick_niao_fei = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GRR._player_niao_count_fei[i]; j++) {
				//////////////////////////////////////////////// 长沙麻将抓鸟//////////////////
				if (seat_index == i) {// 自己有效
					GRR._count_pick_niao_fei++;
					GRR._player_niao_cards_fei[i][j] = this.set_fei_niao_valid(GRR._player_niao_cards_fei[i][j], true);// 胡牌的鸟生效
				} else {
					GRR._player_niao_cards_fei[i][j] = this.set_fei_niao_valid(GRR._player_niao_cards_fei[i][j], false);
				}

			}
		}

		////////////////////////////////////////////////////// 自摸 算分

		if (zimo) {

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}

				int s = lChiHuScore;
				//////////////////////////////////////////////// 长沙麻将自摸算分//////////////////
				if (this.is_zhuang_xian()) {
					if ((GRR._banker_player == i) || (GRR._banker_player == seat_index)) {
						int zx = lChiHuScore / 6;
						s += (zx == 0 ? 1 : zx);

					}
				}

				if (isMutlpNiap && chr.single_da_hu > 0) {
					if (this.is_zhuang_xian()) {
						if ((GRR._banker_player == i) || (GRR._banker_player == seat_index)) {
							s = (int) (7 * Math.pow(2, chr.single_da_hu - 1));
						} else {
							s = (int) (6 * Math.pow(2, chr.single_da_hu - 1));
						}
					}
				}

				GRR._player_niao_invalid[i] = 1;// 庄家鸟生效

				int niao = GRR._player_niao_count[seat_index] + GRR._player_niao_count[i];
				if (niao > 0) {
					if (isMutlpNiap) {
						// s *= (niao + 1);
						s *= Math.pow(2, niao);
					} else {
						s += niao;
					}
				}

				s += (GRR._player_niao_count_fei[seat_index]) * 2;// 飞鸟分

				s += zuoPiao * 2;// 坐飘

				s += ((_player_result.pao[i] < 0 ? 0 : _player_result.pao[i])
						+ (_player_result.pao[seat_index] < 0 ? 0 : _player_result.pao[seat_index]));// 跑分

				if (has_rule(GameConstants.GAME_RULE_HUNAN_SCORE_TOP) && s > 60) {
					s = 60;
				}
				// 胡牌分
				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;
			}
		}
		////////////////////////////////////////////////////// 点炮 算分
		else {
			int s = lChiHuScore;
			if (this.is_zhuang_xian()) {
				if ((GRR._banker_player == provide_index) || (GRR._banker_player == seat_index)) {
					int zx = GRR._chi_hu_rights[seat_index].single_da_hu;// lChiHuScore/6;
					s += (zx == 0 ? 1 : zx);
				}
			}

			if (isMutlpNiap && chr.single_da_hu > 0) {
				if (this.is_zhuang_xian()) {
					if ((GRR._banker_player == provide_index) || (GRR._banker_player == seat_index)) {
						s = (int) (7 * Math.pow(2, chr.single_da_hu - 1));
					} else {
						s = (int) (6 * Math.pow(2, chr.single_da_hu - 1));
					}
				}
			}

			int niao = GRR._player_niao_count[seat_index] + GRR._player_niao_count[provide_index];
			if (niao > 0) {
				if (isMutlpNiap) {
					s *= Math.pow(2, niao);
				} else {
					s += niao;
				}
			}

			s += (GRR._player_niao_count_fei[seat_index]) * 2;// 飞鸟分
			s += zuoPiao * 2;// 坐飘

			s += ((_player_result.pao[provide_index] < 0 ? 0 : _player_result.pao[provide_index])
					+ (_player_result.pao[seat_index] < 0 ? 0 : _player_result.pao[seat_index]));// 飘分

			if (has_rule(GameConstants.GAME_RULE_HUNAN_SCORE_TOP) && s > 60) {
				s = 60;
			}

			GRR._game_score[provide_index] -= s;
			GRR._game_score[seat_index] += s;

			// 点炮的时候，删掉这张牌显示
			// GRR._cards_index[seat_index][_logic.switch_to_card_index(_provide_card)]--;

			GRR._chi_hu_rights[provide_index].opr_or(GameConstants.CHR_FANG_PAO);

		}

		GRR._provider[seat_index] = provide_index;
		// 设置变量

		_status_gang = false;
		_status_gang_hou_pao = false;

		_playerStatus[seat_index].clean_status();

		return;
	}

	/**
	 * 长沙胡牌算分
	 * 
	 * @param seat_index
	 * @param provide_index
	 * @param operate_card
	 * @param zimo
	 */
	public void process_chi_hu_player_score_cs(int seat_index, int provide_index, boolean zimo) {
		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int wFanShu = 0;// 番数
		wFanShu = _logic.get_chi_hu_action_rank_cs(chr);
		countCardType(chr, seat_index);
		int lChiHuScore = wFanShu * GameConstants.CELL_SCORE;// wFanShu*m_pGameServiceOption->lCellScore;

		// 统计
		if (zimo) {
			// 自摸
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				GRR._lost_fan_shu[i][seat_index] = wFanShu;
			}
		} else {// 点炮
			GRR._lost_fan_shu[provide_index][seat_index] = wFanShu;//

		}

		int zuoPiao = getZuoPiaoScore();
		boolean isMutlpNiap = isMutlpDingNiao();// 是否乘法定鸟

		/////////////////////////////////////////////// 算分//////////////////////////
		GRR._count_pick_niao = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GRR._player_niao_count[i]; j++) {
				//////////////////////////////////////////////// 长沙麻将抓鸟//////////////////
				if (zimo) {
					GRR._count_pick_niao++;
					GRR._player_niao_cards[i][j] = this.set_ding_niao_valid(GRR._player_niao_cards[i][j], true);// 胡牌的鸟生效

				} else {
					if (seat_index == i || provide_index == i) {// 自己还有放炮的人有效
						GRR._count_pick_niao++;
						GRR._player_niao_cards[i][j] = this.set_ding_niao_valid(GRR._player_niao_cards[i][j], true);// 胡牌的鸟生效
					} else {
						GRR._player_niao_cards[i][j] = this.set_ding_niao_valid(GRR._player_niao_cards[i][j], false);// 胡牌的鸟生效
					}
				}

			}
		}

		// 飞鸟
		GRR._count_pick_niao_fei = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GRR._player_niao_count_fei[i]; j++) {
				//////////////////////////////////////////////// 长沙麻将抓鸟//////////////////
				if (seat_index == i) {// 自己有效
					GRR._count_pick_niao_fei++;
					GRR._player_niao_cards_fei[i][j] = this.set_fei_niao_valid(GRR._player_niao_cards_fei[i][j], true);// 胡牌的鸟生效
				} else {
					GRR._player_niao_cards_fei[i][j] = this.set_fei_niao_valid(GRR._player_niao_cards_fei[i][j], false);
				}

			}
		}

		////////////////////////////////////////////////////// 自摸 算分
		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}

				int s = lChiHuScore;
				//////////////////////////////////////////////// 长沙麻将自摸算分//////////////////
				if (this.is_zhuang_xian()) {
					if ((GRR._banker_player == i) || (GRR._banker_player == seat_index)) {
						int zx = lChiHuScore / 6;
						s += (zx == 0 ? 1 : zx);
					}
				}

				pai_xing_fen[i] -= s;
				pai_xing_fen[seat_index] += s;

				GRR._player_niao_invalid[i] = 1;// 庄家鸟生效

				int niao = GRR._player_niao_count[seat_index] + GRR._player_niao_count[i];
				if (niao > 0) {
					if (isMutlpNiap) {
						// s *= (niao + 1);
						bird_score[i] -= s * (Math.pow(2, niao) - 1);
						bird_score[seat_index] += s * (Math.pow(2, niao) - 1);

						s *= Math.pow(2, niao);
					} else {
						bird_score[i] -= s * niao;
						bird_score[seat_index] += s * niao;

						s *= (niao + 1);
					}
				}

				bird_score[i] -= (GRR._player_niao_count_fei[seat_index]) * 2;
				bird_score[seat_index] += (GRR._player_niao_count_fei[seat_index]) * 2;

				s += (GRR._player_niao_count_fei[seat_index]) * 2;// 飞鸟分

				s += zuoPiao * 2;// 坐飘

				s += ((_player_result.pao[i] < 0 ? 0 : _player_result.pao[i])
						+ (_player_result.pao[seat_index] < 0 ? 0 : _player_result.pao[seat_index]));// 跑分
				// 胡牌分
				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;
			}
		}
		////////////////////////////////////////////////////// 点炮 算分
		else {
			int s = lChiHuScore;
			if (this.is_zhuang_xian()) {
				if ((GRR._banker_player == provide_index) || (GRR._banker_player == seat_index)) {
					int zx = lChiHuScore / 6;// lChiHuScore/6;
												// GRR._chi_hu_rights[seat_index].da_hu_count
					s += (zx == 0 ? 1 : zx);
				}
			}

			pai_xing_fen[provide_index] -= s;
			pai_xing_fen[seat_index] += s;

			int niao = GRR._player_niao_count[seat_index] + GRR._player_niao_count[provide_index];
			if (niao > 0) {
				if (isMutlpNiap) {
					bird_score[provide_index] -= s * (Math.pow(2, niao) - 1);
					bird_score[seat_index] += s * (Math.pow(2, niao) - 1);

					s *= Math.pow(2, niao);
				} else {
					bird_score[provide_index] -= s * niao;
					bird_score[seat_index] += s * niao;

					s *= (niao + 1);
				}
			}

			bird_score[provide_index] -= (GRR._player_niao_count_fei[seat_index]) * 2;
			bird_score[seat_index] += (GRR._player_niao_count_fei[seat_index]) * 2;

			s += (GRR._player_niao_count_fei[seat_index]) * 2;// 飞鸟分
			s += zuoPiao * 2;// 坐飘

			s += ((_player_result.pao[provide_index] < 0 ? 0 : _player_result.pao[provide_index])
					+ (_player_result.pao[seat_index] < 0 ? 0 : _player_result.pao[seat_index]));// 飘分

			GRR._game_score[provide_index] -= s;
			GRR._game_score[seat_index] += s;

			// 点炮的时候，删掉这张牌显示
			// GRR._cards_index[seat_index][_logic.switch_to_card_index(_provide_card)]--;

			GRR._chi_hu_rights[provide_index].opr_or(GameConstants.CHR_FANG_PAO);

		}

		GRR._provider[seat_index] = provide_index;
		// 设置变量

		_status_gang = false;
		_status_gang_hou_pao = false;

		_playerStatus[seat_index].clean_status();

		return;
	}

	// 发送场景
	// public boolean send_game_data(int seat_index, int cbGameStatus, boolean
	// bSendSecret) {
	// if (cbGameStatus == 0) {
	// cbGameStatus = _game_status;
	// }
	// switch (cbGameStatus) {
	// case MJGameConstants.GS_MJ_FREE: // 空闲状态
	// {
	//
	// // 构造数据
	//
	// RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
	// roomResponse.setType(10);
	// this.load_common_status(roomResponse);
	//
	// //
	// this.load_room_info_data(roomResponse);
	// this.load_player_info_data(roomResponse);
	//
	// this.send_response_to_player(seat_index, roomResponse);
	//
	// // 发送场景
	// // return
	// //
	// m_pITableFrame->SendGameScene(pIServerUserItem,&StatusFree,sizeof(StatusFree));
	// break;
	// }
	// case MJGameConstants.GS_MJ_PLAY: // 游戏状态
	// {
	// RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
	//
	// this.load_common_status(roomResponse);
	// roomResponse.setType(1);
	//
	// TableResponse.Builder tableResponse = TableResponse.newBuilder();
	//
	// // 玩家
	// for (int i = 0; i < MJGameConstants.GAME_PLAYER; i++) {
	// RoomPlayerResponse.Builder roomPlayerResponse =
	// RoomPlayerResponse.newBuilder();
	// roomPlayerResponse.setAccountId(this.get_players()[i].getAccount_id());
	// roomPlayerResponse.setUserName("ddshgjjrtiytity");
	// roomPlayerResponse.setHeadImgUrl("");
	// roomPlayerResponse.setIp("");
	// roomPlayerResponse.setSeatIndex(this.get_players()[i].get_seat_index());
	//
	// roomResponse.addPlayers(roomPlayerResponse);
	// }
	//
	// // 游戏变量
	// tableResponse.setBankerPlayer(GRR._banker_player);
	// tableResponse.setCurrentPlayer(_current_player);
	// tableResponse.setCellScore(0);
	//
	// // 状态变量
	// tableResponse.setActionCard(_provide_card);
	// tableResponse.setLeftCardCount(GRR._left_card_count);
	// //tableResponse.setActionMask((_response[seat_index] == false) ?
	// _player_action[seat_index] : MJGameConstants.WIK_NULL);
	//
	// // 历史记录
	// tableResponse.setOutCardData(_out_card_data);
	// tableResponse.setOutCardPlayer(_out_card_player);
	//
	// for (int i = 0; i < MJGameConstants.GAME_PLAYER; i++) {
	// tableResponse.addTrustee(false);// 是否托管
	// // 剩余牌数
	// tableResponse.addDiscardCount(GRR._discard_count[i]);
	// Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
	// for (int j = 0; j < 55; j++) {
	// int_array.addItem(GRR._discard_cards[i][j]);
	// }
	// tableResponse.addDiscardCards(int_array);
	//
	// // 组合扑克
	// tableResponse.addWeaveCount(GRR._weave_count[i]);
	// WeaveItemResponseArrayResponse.Builder weaveItem_array =
	// WeaveItemResponseArrayResponse.newBuilder();
	// for (int j = 0; j < MJGameConstants.MAX_WEAVE; j++) {
	// WeaveItemResponse.Builder weaveItem_item =
	// WeaveItemResponse.newBuilder();
	// weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
	// weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
	// weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
	// weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
	// weaveItem_array.addWeaveItem(weaveItem_item);
	// }
	// tableResponse.addWeaveItemArray(weaveItem_array);
	//
	// //
	// tableResponse.addWinnerOrder(0);
	//
	// // 牌
	// tableResponse.addCardCount(_logic.get_card_count_by_index(GRR._cards_index[i]));
	//
	// }
	//
	// // //扑克数据
	// tableResponse.setSendCardData(((_send_card_data != 0) && (_provide_player
	// == seat_index)) ? _send_card_data: MJGameConstants.INVALID_VALUE);
	//
	//
	//
	// int hand_cards[] = new int[MJGameConstants.MAX_COUNT];
	//
	// int hand_card_count =
	// _logic.switch_to_cards_data(GRR._cards_index[seat_index], hand_cards);
	// if(seat_index == _current_player){
	// _logic.remove_card_by_data(hand_cards, _send_card_data);
	// }
	// // tableResponse.setCardCount(hand_card_count);
	// for (int j = 0; j < MJGameConstants.MAX_COUNT; j++) {
	// tableResponse.addCardsData(hand_cards[j]);
	// }
	//
	// // //变量定义
	// // CMD_StatusPlay StatusPlay = new CMD_StatusPlay();
	// // StatusPlay.card_count =
	// // _logic.switch_to_cards_data(_cards_index[seat_index],
	// // StatusPlay.cards_data);
	// //
	// // //游戏变量
	// // StatusPlay.banker_player=_banker_player;
	// // StatusPlay.current_player=_current_player;
	// // StatusPlay.cell_score=0;///m_pGameServiceOption->lCellScore;
	// //
	// // //状态变量
	// // StatusPlay.action_card=_provide_card;
	// // StatusPlay.left_card_count=GRR._left_card_count;
	// //
	// StatusPlay.action_mask=(_response[seat_index]==false)?_player_action[seat_index]:MJGameConstants.WIK_NULL;
	// //
	// // //历史记录
	// // StatusPlay.out_card_player=_out_card_player;
	// // StatusPlay.out_card_data=_out_card_data;
	// // for (int i = 0;i<MJGameConstants.GAME_PLAYER;i++)
	// // {
	// // StatusPlay.trustee[i] = false;
	// // //剩余牌数
	// // StatusPlay.discard_count[i] = GRR._discard_count[i];
	// // //组合扑克
	// // StatusPlay.weave_count[i] = _weave_count[i];
	// //
	// // for(int j=0; j < 55; j++){
	// // StatusPlay.discard_cards[i][j] = GRR._discard_cards[i][j];
	// //
	// // }
	// //
	// // for(int j=0; j < MJGameConstants.MAX_WEAVE; j++){
	// // //组合扑克
	// // StatusPlay.weave_items[i][j].center_card =
	// // _weave_items[i][j].center_card;
	// // StatusPlay.weave_items[i][j].provide_player =
	// // _weave_items[i][j].provide_player;
	// // StatusPlay.weave_items[i][j].public_card =
	// // _weave_items[i][j].public_card;
	// // StatusPlay.weave_items[i][j].weave_kind =
	// // _weave_items[i][j].weave_kind;
	// // }
	// //
	// //
	// //
	// // }
	// // //扑克数据
	// // StatusPlay.card_count =
	// // _logic.switch_to_cards_data(_cards_index[seat_index],
	// // StatusPlay.cards_data);
	// // StatusPlay.send_card_data
	// //
	// =((_send_card_data!=0)&&(_provide_player==seat_index))?_send_card_data:0x00;
	//
	// // 发送场景
	// // return
	// //
	// m_pITableFrame->SendGameScene(pIServerUserItem,&StatusPlay,sizeof(StatusPlay));
	// roomResponse.setTable(tableResponse);
	//
	// this.send_response_to_player(0, roomResponse);
	//
	// break;
	// }
	// }
	//
	// return false;
	// }

	// 过滤吃胡权值
	private void filtrate_right(int seat_index, ChiHuRight chr) {
		// 权位增加
		// 抢杠
		if (_current_player == GameConstants.INVALID_SEAT && _status_gang) {
			chr.opr_or(GameConstants.CHR_HUNAN_QIANG_GANG_HU);
		}
		// 海底捞
		// if (GRR._left_card_count == 0) {
		// chr.opr_or(MJGameConstants.CHR_HAI_DI_LAO);
		// }
		// 附加权位
		// 杠上花
		if (_current_player == seat_index && _status_gang) {
			chr.opr_or(GameConstants.CHR_HUNAN_GANG_KAI);
		}
		// 杠上炮
		if (_status_gang_hou_pao && !_status_gang) {
			chr.opr_or(GameConstants.CHR_HUNAN_GANG_SHANG_PAO);
		}
	}

	/**
	 * 
	 * @param seat_index
	 * @param card
	 *            固定的鸟
	 * @param show
	 * @param add_niao
	 *            额外奖鸟
	 */
	public void set_niao_card(int seat_index, int card, boolean show, int add_niao) {
		for (int i = 0; i < GameConstants.MAX_NIAO_CARD; i++) {
			GRR._cards_data_niao[i] = GameConstants.INVALID_VALUE;
		}

		// for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			GRR._player_niao_count[i] = 0;
			for (int j = 0; j < GameConstants.MAX_NIAO_CARD; j++) {
				GRR._player_niao_cards[i][j] = GameConstants.INVALID_VALUE;
			}

		}
		GRR._show_bird_effect = show;
		if (card == GameConstants.INVALID_VALUE) {
			GRR._count_niao = get_niao_card_num(true, add_niao);
		} else {
			GRR._count_niao = get_niao_card_num(false, add_niao);
		}

		if (GRR._count_niao > GameConstants.ZHANIAO_0) {
			if (card == GameConstants.INVALID_VALUE) {
				int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
				// if (is_mj_type(MJGameConstants.GAME_TYPE_HZ)) {
				// _logic.switch_to_cards_index(_repertory_card_zz,
				// _all_card_len-GRR._left_card_count, GRR._count_niao,
				// cbCardIndexTemp);
				// } else {
				// _logic.switch_to_cards_index(_repertory_card_cs,
				// _all_card_len-GRR._left_card_count, GRR._count_niao,
				// cbCardIndexTemp);
				// }
				_logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count, GRR._count_niao,
						cbCardIndexTemp);
				// cbCardIndexTemp[0] = 3;
				// cbCardIndexTemp[1] = 0x25;
				GRR._left_card_count -= GRR._count_niao;
				_logic.switch_to_cards_data(cbCardIndexTemp, GRR._cards_data_niao);

				// for (int i = 0; i < GRR._count_niao; i++) {
				// GRR._cards_data_niao[i] = 0x13;
				// }
			} else {
				for (int i = 0; i < GRR._count_niao; i++) {
					GRR._cards_data_niao[i] = card;
				}
			}
		}

		if (is_mj_type(GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_YUYANG_HONGZHON)
				|| is_mj_type(GameConstants.GAME_TYPE_HONG_ZHONG_MJ_TH)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_HONG_ZHONG_FEI)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_PING_XIANG_ZZ)) {
			if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HUNAN_YI_MA_QUAN_ZHONG)) {
				GRR._count_pick_niao = _logic.get_pick_niao_count_new_hz(GRR._cards_data_niao, GRR._count_niao);
			} else {
				// 中鸟个数
				GRR._count_pick_niao = _logic.get_pick_niao_count(GRR._cards_data_niao, GRR._count_niao);
			}
		} else {
			// 中鸟个数
			GRR._count_pick_niao = _logic.get_pick_niao_count(GRR._cards_data_niao, GRR._count_niao);
		}

		if ((is_mj_type(GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_YUYANG_HONGZHON)
				|| is_mj_type(GameConstants.GAME_TYPE_HONG_ZHONG_MJ_TH)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_HONG_ZHONG_FEI)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_PING_XIANG_ZZ))
				&& GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HUNAN_YI_MA_QUAN_ZHONG)) {
			GRR._player_niao_cards[seat_index][GRR._player_niao_count[seat_index]++] = GRR._cards_data_niao[0];
		} else {
			for (int i = 0; i < GRR._count_niao; i++) {
				int nValue = _logic.get_card_value(GRR._cards_data_niao[i]);
				int seat = 0;
				if ((is_mj_type(GameConstants.GAME_TYPE_FLS_HZ_LX) || is_mj_type(GameConstants.GAME_TYPE_SHUANGGUI)
						|| is_mj_type(GameConstants.GAME_TYPE_HUNAN_SHANG_XIA_GUI)
						|| is_mj_type(GameConstants.GAME_TYPE_HENAN_HZ)
						|| is_mj_type(GameConstants.GAME_TYPE_HENAN_ZHUAN_ZHUAN))
						|| is_mj_type(GameConstants.GAME_TYPE_NEW_HN_HONG_ZHONG)
						|| is_mj_type(GameConstants.GAME_TYPE_NEW_SAN_MEN_XIA)) { // liuyan
					seat = get_zhong_seat_by_value_three(nValue, seat_index);
				} else if (GameConstants.GAME_TYPE_CS == _game_type_index
						|| GameConstants.GAME_TYPE_ZHUZHOU == _game_type_index
						|| is_mj_type(GameConstants.GAME_TYPE_HALL_CHANG_SHA_MJ)
						|| is_mj_type(GameConstants.GAME_TYPE_THJ_CS)) {
					seat = (GRR._banker_player + (nValue - 1) % 4) % 4;
				} else if (is_mj_type(GameConstants.GAME_TYPE_ZZ) || is_mj_type(GameConstants.GAME_TYPE_HZ)
						|| is_mj_type(GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ)
						|| is_mj_type(GameConstants.GAME_TYPE_MJ_YUYANG_HONGZHON)
						|| is_mj_type(GameConstants.GAME_TYPE_HONG_ZHONG_MJ_TH)
						|| is_mj_type(GameConstants.GAME_TYPE_MJ_HONG_ZHONG_FEI)
						|| is_mj_type(GameConstants.GAME_TYPE_MJ_PING_XIANG_ZZ)) {
					seat = get_zhong_seat_by_value_three(nValue, seat_index);
				} else if (is_mj_type(GameConstants.GAME_TYPE_HALL_ZHUAN_ZHUAN_MJ)) { // 2018-1-15--15:34---ysm
					if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_ZHUANG_NIAO)) {
						seat = get_seat_by_value_ZZ(nValue, GRR._banker_player);
					} else {
						seat = get_zhong_seat_by_value_three(nValue, seat_index);
					}
				}
				GRR._player_niao_cards[seat][GRR._player_niao_count[seat]] = GRR._cards_data_niao[i];
				GRR._player_niao_count[seat]++;
			}
		}

	}

	public void set_niao_card_hz(int seat_index, int card, boolean show, int add_niao, int hu_card) {
		for (int i = 0; i < GameConstants.MAX_NIAO_CARD; i++) {
			GRR._cards_data_niao[i] = GameConstants.INVALID_VALUE;
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			GRR._player_niao_count[i] = 0;
			for (int j = 0; j < GameConstants.MAX_NIAO_CARD; j++) {
				GRR._player_niao_cards[i][j] = GameConstants.INVALID_VALUE;
			}
		}

		GRR._show_bird_effect = show;

		GRR._count_niao = get_niao_card_num(true, add_niao);

		if (has_rule_ex(GameConstants.GAME_RULE_HUNAN_MO_JI_JIANG_JI)) {
			if (hu_card == GameConstants.HZ_MAGIC_CARD) {
				GRR._count_niao = GRR._left_card_count > 10 ? 10 : GRR._left_card_count;
			} else {
				GRR._count_niao = _logic.get_card_value(hu_card);
			}
		}
		if (has_rule_ex(GameConstants.GAME_RULE_HUNAN_YI_MA_QUAN_ZHONG)) {
			GRR._count_niao = 1;
		}
		if (GRR._count_niao > GRR._left_card_count) {
			GRR._count_niao = GRR._left_card_count;
		}
		if (GRR._count_niao == GameConstants.ZHANIAO_0)
			return;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];

		_logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count, GRR._count_niao,
				cbCardIndexTemp);

		GRR._left_card_count -= GRR._count_niao;

		_logic.switch_to_cards_data(cbCardIndexTemp, GRR._cards_data_niao);

		if (has_rule_ex(GameConstants.GAME_RULE_HUNAN_YI_MA_QUAN_ZHONG)) {
			GRR._count_pick_niao = _logic.get_pick_niao_count_new_hz(GRR._cards_data_niao, GRR._count_niao);
		} else {
			GRR._count_pick_niao = get_pick_niao_count(GRR._cards_data_niao, GRR._count_niao);
		}

		if (has_rule_ex(GameConstants.GAME_RULE_HUNAN_YI_MA_QUAN_ZHONG)) {
			GRR._player_niao_cards[seat_index][GRR._player_niao_count[seat_index]++] = GRR._cards_data_niao[0];
		} else {
			for (int i = 0; i < GRR._count_niao; i++) {
				int nValue = _logic.get_card_value(GRR._cards_data_niao[i]);
				int seat = get_zhong_seat_by_value_three(nValue, seat_index);
				GRR._player_niao_cards[seat][GRR._player_niao_count[seat]] = GRR._cards_data_niao[i];
				GRR._player_niao_count[seat]++;
			}
		}

		int[] player_niao_count = new int[GameConstants.GAME_PLAYER];
		int[][] player_niao_cards = new int[GameConstants.GAME_PLAYER][GameConstants.MAX_NIAO_CARD];
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			player_niao_count[i] = 0;
			for (int j = 0; j < GameConstants.MAX_NIAO_CARD; j++) {
				player_niao_cards[i][j] = GameConstants.INVALID_VALUE;
			}
		}
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < GRR._player_niao_count[i]; j++) {
				if (seat_index == i) {
					player_niao_cards[i][player_niao_count[i]] = this.set_ding_niao_valid(GRR._player_niao_cards[i][j],
							true);//
					// 胡牌的鸟生效
				} else {
					player_niao_cards[i][player_niao_count[i]] = this.set_ding_niao_valid(GRR._player_niao_cards[i][j],
							false);//
					// 胡牌的鸟生效
				}
				player_niao_count[i]++;
			}
		}
		GRR._player_niao_cards = player_niao_cards;
		GRR._player_niao_count = player_niao_count;
	}

	/**
	 * 抓鸟 1 5 9
	 * 
	 * @param cards_data
	 * @param card_num
	 * @return
	 */
	public int get_pick_niao_count(int cards_data[], int card_num) {
		// MAX_NIAO_CARD
		int cbPickNum = 0;
		for (int i = 0; i < card_num; i++) {
			if (!_logic.is_valid_card(cards_data[i])) {
				return 0;
			}

			int nValue = _logic.get_card_value(cards_data[i]);
			if (nValue == 1 || nValue == 5 || nValue == 9) {
				cbPickNum++;
			}

		}
		return cbPickNum;
	}

	/**
	 * 获取坐飘分
	 */
	public int getZuoPiaoScore() {
		int score = 0;
		if (has_rule(GameConstants.GAME_RULE_HUNAN_CS_ZUOPIAO1)) {
			return GameConstants.ZUOPIAO_1;
		} else if (has_rule(GameConstants.GAME_RULE_HUNAN_CS_ZUOPIAO2)) {
			return GameConstants.ZUOPIAO_2;
		} else if (has_rule(GameConstants.GAME_RULE_HUNAN_CS_ZUOPIAO3)) {
			return GameConstants.ZUOPIAO_3;
		}
		return score;
	}

	/**
	 * 获取飞鸟 数量
	 * 
	 * @return
	 */
	public int getFeiNiaoNum() {
		int num = 0;
		if (has_rule(GameConstants.GAME_RULE_HUNAN_CS_FEI_NIAO2)) {
			return GameConstants.FEINIAO_2;
		} else if (has_rule(GameConstants.GAME_RULE_HUNAN_CS_FEI_NIAO4)) {
			return GameConstants.FEINIAO_4;
		}
		return num;
	}

	/**
	 * 是否乘法 定鸟
	 * 
	 * @return
	 */
	public boolean isMutlpDingNiao() {
		boolean isMutlp = has_rule(GameConstants.GAME_RULE_HUNAN_CS_DING_NIAO1)
				|| has_rule(GameConstants.GAME_RULE_HUNAN_CS_DING_NIAO2);
		return isMutlp;
	}

	/**
	 * 乘法 定鸟 个数
	 * 
	 * @return
	 */
	public int getMutlpDingNiaoNum() {
		int num = 0;
		if (has_rule(GameConstants.GAME_RULE_HUNAN_CS_DING_NIAO1)) {
			num = GameConstants.ZHANIAO_1;
		} else if (has_rule(GameConstants.GAME_RULE_HUNAN_CS_DING_NIAO2)) {
			num = GameConstants.ZHANIAO_2;
		}
		return num;
	}

	/**
	 * 长沙 定鸟 个数
	 * 
	 * @return
	 */
	public int getCsDingNiaoNum() {
		int nNum = GameConstants.ZHANIAO_0;
		// 湖南麻将的抓鸟
		if (is_mj(GameConstants.GAME_ID_HUNAN) || is_mj(GameConstants.GAME_ID_FLS_LX)
				|| is_mj_type(GameConstants.GAME_TYPE_HALL_CHANG_SHA_MJ)
				|| is_mj_type(GameConstants.GAME_TYPE_FLS_CS_LX_DT) || is_mj_type(GameConstants.GAME_TYPE_THJ_CS)) {
			if (isMutlpDingNiao()) {// 乘法鸟
				nNum = getMutlpDingNiaoNum();
			} else {
				if (has_rule(GameConstants.GAME_RULE_HUNAN_ZHANIAO2)) {
					nNum = GameConstants.ZHANIAO_2;
				} else if (has_rule(GameConstants.GAME_RULE_HUNAN_ZHANIAO4)) {
					nNum = GameConstants.ZHANIAO_4;
				} else if (has_rule(GameConstants.GAME_RULE_HUNAN_ZHANIAO6)) {
					nNum = GameConstants.ZHANIAO_6;
				}
			}
		}
		return nNum;
	}

	/**
	 * 获取长沙能抓的定鸟的 数量
	 * 
	 * @return
	 */
	public int get_ding_niao_card_num_cs(boolean check) {
		int nNum = getCsDingNiaoNum();
		if (check == false) {
			return nNum;
		}
		if (nNum > GRR._left_card_count) {
			nNum = GRR._left_card_count;
		}
		return nNum;
	}

	/**
	 * 获取长沙能抓的飞鸟的 数量
	 * 
	 * @return
	 */
	public int get_fei_niao_card_num_cs(boolean check) {
		int fNum = getFeiNiaoNum();
		if (check == false) {
			return fNum;
		}
		if (fNum > GRR._left_card_count) {
			fNum = GRR._left_card_count;
		}
		return fNum;
	}

	/**
	 * 获取长沙能抓的鸟的总 数量
	 * 
	 * @return
	 */
	public int get_niao_card_num_cs(boolean check) {
		int nNum = get_ding_niao_card_num_cs(check);
		if (check == false) {
			return nNum;
		}
		nNum += get_fei_niao_card_num_cs(check);
		return nNum;
	}

	/**
	 * 
	 * @param seat_index
	 * @param card
	 *            固定的鸟
	 * @param show
	 * @param add_niao
	 *            额外奖鸟
	 * @param isTongPao
	 *            --通炮不抓飞鸟
	 */
	public void set_niao_card_cs(int seat_index, int card, boolean show, int add_niao, boolean isTongPao) {
		for (int i = 0; i < GameConstants.MAX_NIAO_CARD; i++) {
			GRR._cards_data_niao[i] = GameConstants.INVALID_VALUE;
		}

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			GRR._player_niao_count[i] = 0;
			for (int j = 0; j < GameConstants.MAX_NIAO_CARD; j++) {
				GRR._player_niao_cards[i][j] = GameConstants.INVALID_VALUE;
			}

		}
		GRR._show_bird_effect = show;
		if (card == GameConstants.INVALID_VALUE) {
			GRR._count_niao = get_ding_niao_card_num_cs(true);
		} else {
			GRR._count_niao = get_ding_niao_card_num_cs(false);
		}

		if (GRR._count_niao > GameConstants.ZHANIAO_0) {
			if (card == GameConstants.INVALID_VALUE) {
				int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
				_logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count, GRR._count_niao,
						cbCardIndexTemp);
				GRR._left_card_count -= GRR._count_niao;
				_logic.switch_to_cards_data(cbCardIndexTemp, GRR._cards_data_niao);
			} else {
				for (int i = 0; i < GRR._count_niao; i++) {
					GRR._cards_data_niao[i] = card;
				}
			}
		}
		// 中鸟个数
		GRR._count_pick_niao = _logic.get_pick_niao_count(GRR._cards_data_niao, GRR._count_niao);

		for (int i = 0; i < GRR._count_niao; i++) {
			int nValue = _logic.get_card_value(GRR._cards_data_niao[i]);
			int seat = 0;
			if ((GameConstants.GAME_TYPE_ZZ == _game_type_index) || is_mj_type(GameConstants.GAME_TYPE_HZ)
					|| is_mj_type(GameConstants.GAME_TYPE_FLS_HZ_LX) || is_mj_type(GameConstants.GAME_TYPE_SHUANGGUI)
					|| is_mj_type(GameConstants.GAME_TYPE_HENAN_HZ)
					|| is_mj_type(GameConstants.GAME_TYPE_HUNAN_SHANG_XIA_GUI)
					|| is_mj_type(GameConstants.GAME_TYPE_MJ_YUYANG_HONGZHON)
					|| is_mj_type(GameConstants.GAME_TYPE_HONG_ZHONG_MJ_TH)
					|| is_mj_type(GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ)
					|| is_mj_type(GameConstants.GAME_TYPE_MJ_HONG_ZHONG_FEI)
					|| is_mj_type(GameConstants.GAME_TYPE_MJ_PING_XIANG_ZZ)
					|| is_mj_type(GameConstants.GAME_TYPE_HALL_ZHUAN_ZHUAN_MJ)) {
				seat = (seat_index + (nValue - 1) % 4) % 4;
			} else if (GameConstants.GAME_TYPE_CS == _game_type_index
					|| is_mj_type(GameConstants.GAME_TYPE_HALL_CHANG_SHA_MJ)
					|| is_mj_type(GameConstants.GAME_TYPE_THJ_CS)) {
				seat = get_seat_by_value(nValue, GRR._banker_player);
			} else if (GameConstants.GAME_TYPE_ZHUZHOU == _game_type_index) {
				seat = get_zhong_seat_by_value_three(nValue, GRR._banker_player);
			} else if (GameConstants.GAME_TYPE_FLS_CS_LX == _game_type_index
					|| GameConstants.GAME_TYPE_FLS_CS_LX_DT == _game_type_index) {
				seat = get_zhong_seat_by_value_cslx(nValue, GRR._banker_player);
			}
			GRR._player_niao_cards[seat][GRR._player_niao_count[seat]] = GRR._cards_data_niao[i];
			GRR._player_niao_count[seat]++;
		}

		if (!isTongPao) {
			set_niao_card_cs_fei(seat_index, card, show, add_niao);
		}

	}

	private int get_zhong_seat_by_value_cslx(int nValue, int banker_seat) {
		int seat = 0;
		if (getTablePlayerNumber() == 4) {
			seat = (banker_seat + (nValue - 1) % 4) % 4;
		} else {// 3人场//这里这么特殊处理是因为 算鸟是根据玩家逻辑位置,只有庄家上家,庄家下家,不存在对家
			int v = (nValue - 1) % 4;
			switch (v) {
			case 0:// 本家//159
				seat = GRR._banker_player;
				break;
			case 1:// 26//下家
				seat = get_banker_next_seat(GRR._banker_player);
				break;
			case 2:// 37//对家
				seat = get_null_seat();
				break;
			default:// 48//上家
				seat = get_banker_pre_seat(GRR._banker_player);
				break;
			}
		} /*
			 * else if(getTablePlayerNumber() == 2){ int v = (nValue - 1) % 4;
			 * switch (v) { case 0:// 本家//159 seat = GRR._banker_player; break;
			 * case 1:// 26//下家 seat = get_null_seat() ; break; case 2:// 37//对家
			 * seat = get_banker_next_seat(GRR._banker_player); break;
			 * default:// 48//上家 seat = get_null_seat() ; break; } }
			 */
		return seat;
	}

	/**
	 * 
	 * @param seat_index
	 * @param card
	 *            固定的鸟
	 * @param show
	 * @param add_niao
	 *            额外奖鸟
	 */
	private void set_niao_card_cs_fei(int seat_index, int card, boolean show, int add_niao) {
		for (int i = 0; i < GameConstants.MAX_NIAO_CARD; i++) {
			GRR._cards_data_niao_fei[i] = GameConstants.INVALID_VALUE;
		}

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			GRR._player_niao_count_fei[i] = 0;
			for (int j = 0; j < GameConstants.MAX_NIAO_CARD; j++) {
				GRR._player_niao_cards_fei[i][j] = GameConstants.INVALID_VALUE;
			}
		}
		GRR._count_niao_fei = get_fei_niao_card_num_cs(true);

		if (GRR._count_niao_fei > GameConstants.ZHANIAO_0) {
			if (card == GameConstants.INVALID_VALUE) {
				int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
				_logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count, GRR._count_niao_fei,
						cbCardIndexTemp);
				GRR._left_card_count -= GRR._count_niao_fei;
				_logic.switch_to_cards_data(cbCardIndexTemp, GRR._cards_data_niao_fei);
			}
		}
		// 中鸟个数
		GRR._count_pick_niao_fei = _logic.get_pick_niao_count(GRR._cards_data_niao_fei, GRR._count_niao_fei);

		for (int i = 0; i < GRR._count_niao_fei; i++) {
			int nValue = _logic.get_card_value(GRR._cards_data_niao_fei[i]);
			int seat = 0;
			if ((GameConstants.GAME_TYPE_ZZ == _game_type_index) || is_mj_type(GameConstants.GAME_TYPE_HZ)
					|| is_mj_type(GameConstants.GAME_TYPE_FLS_HZ_LX) || is_mj_type(GameConstants.GAME_TYPE_SHUANGGUI)
					|| is_mj_type(GameConstants.GAME_TYPE_HUNAN_SHANG_XIA_GUI)
					|| is_mj_type(GameConstants.GAME_TYPE_HENAN_HZ)
					|| is_mj_type(GameConstants.GAME_TYPE_MJ_YUYANG_HONGZHON)
					|| is_mj_type(GameConstants.GAME_TYPE_HONG_ZHONG_MJ_TH)
					|| is_mj_type(GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ)
					|| is_mj_type(GameConstants.GAME_TYPE_MJ_HONG_ZHONG_FEI)
					|| is_mj_type(GameConstants.GAME_TYPE_MJ_PING_XIANG_ZZ)
					|| is_mj_type(GameConstants.GAME_TYPE_HALL_ZHUAN_ZHUAN_MJ)) {
				seat = (seat_index + (nValue - 1) % 4) % 4;
			} else if (GameConstants.GAME_TYPE_ZHUZHOU == _game_type_index) {
				seat = (GRR._banker_player + (nValue - 1) % 4) % 4;
			} else if (GameConstants.GAME_TYPE_CS == _game_type_index
					|| is_mj_type(GameConstants.GAME_TYPE_HALL_CHANG_SHA_MJ)
					|| is_mj_type(GameConstants.GAME_TYPE_THJ_CS)) {
				seat = get_seat_by_value(nValue, GRR._banker_player);
			}
			GRR._player_niao_cards_fei[seat][GRR._player_niao_count_fei[seat]] = GRR._cards_data_niao_fei[i];
			GRR._player_niao_count_fei[seat]++;
		}
	}

	/**
	 * 获取鸟的 数量
	 * 
	 * @param check
	 * @param add_niao
	 * @return
	 */
	public int get_niao_card_num(boolean check, int add_niao) {
		int nNum = GameConstants.ZHANIAO_0;

		// 湖南麻将的抓鸟
		if (is_mj(GameConstants.GAME_ID_HUNAN) || is_mj(GameConstants.GAME_ID_FLS_LX)
				|| _game_type_index == GameConstants.GAME_TYPE_HENAN_ZHUAN_ZHUAN
				|| is_mj_type(GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ)
				|| is_mj_type(GameConstants.GAME_TYPE_HUNAN_SHANG_XIA_GUI)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_HONG_ZHONG_FEI)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_PING_XIANG_ZZ)
				|| is_mj_type(GameConstants.GAME_TYPE_HALL_ZHUAN_ZHUAN_MJ)
				|| is_mj_type(GameConstants.GAME_TYPE_HONG_ZHONG_MJ_TH)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_SAN_MEN_XIA)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_YUYANG_HONGZHON)) {
			if (has_rule(GameConstants.GAME_RULE_HUNAN_ZHANIAO1)) {
				nNum = GameConstants.ZHANIAO_1;
			} else if (has_rule(GameConstants.GAME_RULE_HUNAN_ZHANIAO2)) {
				nNum = GameConstants.ZHANIAO_2;
			} else if (has_rule(GameConstants.GAME_RULE_HUNAN_ZHANIAO4)) {
				nNum = GameConstants.ZHANIAO_4;
			} else if (has_rule(GameConstants.GAME_RULE_HUNAN_ZHANIAO6)) {
				nNum = GameConstants.ZHANIAO_6;
			} else if ((is_mj_type(GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ)
					|| is_mj_type(GameConstants.GAME_TYPE_HONG_ZHONG_MJ_TH)
					|| is_mj_type(GameConstants.GAME_TYPE_MJ_HONG_ZHONG_FEI)
					|| is_mj_type(GameConstants.GAME_TYPE_MJ_PING_XIANG_ZZ))
					&& GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HUNAN_YI_MA_QUAN_ZHONG)) {
				nNum = 0;
				return nNum;
			} else if ((is_mj_type(GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ)
					|| is_mj_type(GameConstants.GAME_TYPE_HONG_ZHONG_MJ_TH)
					|| is_mj_type(GameConstants.GAME_TYPE_MJ_HONG_ZHONG_FEI)
					|| is_mj_type(GameConstants.GAME_TYPE_MJ_PING_XIANG_ZZ))
					&& GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HUNAN_MO_JI_JIANG_JI)) {
				nNum = 0;
				return nNum;
			} else if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HUNAN_YI_MA_QUAN_ZHONG)) {
				nNum = 1;
				return nNum;
			} else if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HUNAN_MO_JI_JIANG_JI)) {
				nNum = 1;
				return nNum;
			}

		} else if (is_mj(GameConstants.GAME_ID_HENAN) || is_new_henan_mj()) {
			// 河南麻将的抓鸟
			if (has_rule(GameConstants.GAME_RULE_HENAN_ZHANIAO2)) {
				nNum = GameConstants.ZHANIAO_2;
			} else if (has_rule(GameConstants.GAME_RULE_HENAN_ZHANIAO4)) {
				nNum = GameConstants.ZHANIAO_4;
			} else if (has_rule(GameConstants.GAME_RULE_HENAN_ZHANIAO6)) {
				nNum = GameConstants.ZHANIAO_6;
			}
		}

		nNum += add_niao;

		if (check == false) {
			return nNum;
		}
		if (nNum > GRR._left_card_count) {
			nNum = GRR._left_card_count;
		}
		return nNum;
	}

	public boolean is_new_henan_mj() {
		if (_game_type_index >= GameConstants.GAME_TYPE_NEW_AN_YANG
				&& _game_type_index <= GameConstants.GAME_TYPE_NEW_JIAO_ZUO) {
			return true;
		}

		return false;
	}

	@Override
	public boolean handler_requst_pao_qiang(Player player, int pao, int qiang) {

		/**
		 * if(_playerStatus[player.get_seat_index()]._is_pao_qiang ||
		 * _playerStatus[player.get_seat_index()]._is_pao){ return false; }
		 */

		if (is_mj_type(GameConstants.GAME_TYPE_HENAN_PDS) || is_mj_type(GameConstants.GAME_TYPE_NEW_PING_DING_SHAN)) {
			if (_handler_pao_henna != null && !this._handler.equals(_handler_kou_hennapds)) {
				return _handler_pao_henna.handler_pao_qiang(this, player.get_seat_index(), pao, qiang);
			} else if (_handler_kou_hennapds != null) {
				return _handler_kou_hennapds.handler_pao_qiang(this, player.get_seat_index(), pao, qiang);
			}
		}

		// ._is_pao_qiang 代码判断被去掉了，子类自己判断。。。上面的不敢动了s
		if (_handler_pao_qiang != null) {
			return _handler_pao_qiang.handler_pao_qiang(this, player.get_seat_index(), pao, qiang);
		} else if (_handler_pao != null) {
			return _handler_pao.handler_pao_qiang(this, player.get_seat_index(), pao, qiang);
		} else if (_handler_pao_henna != null) {
			return _handler_pao_henna.handler_pao_qiang(this, player.get_seat_index(), pao, qiang);
		} else if (handler_piao_cs != null) {
			return handler_piao_cs.handler_pao_qiang(this, player.get_seat_index(), pao, qiang);
		} else if (_handler_pao_henna_hz != null) {
			return _handler_pao_henna_hz.handler_pao_qiang(this, player.get_seat_index(), pao, qiang);
		} else if (_handlerPiao_LXCG != null) {
			return _handlerPiao_LXCG.handler_pao_qiang(this, player.get_seat_index(), pao, qiang);
		} else if (handler_piao_cslx != null) {
			return handler_piao_cslx.handler_pao_qiang(this, player.get_seat_index(), pao, qiang);
		} else if (_handler_pao_henna_xy != null) {
			return _handler_pao_henna_xy.handler_pao_qiang(this, player.get_seat_index(), pao, qiang);
		} else if (_handler_piao != null) {
			return _handler_piao.handler_pao_qiang(this, player.get_seat_index(), pao, qiang);
		}

		return false;

	}

	@Override
	public boolean handler_requst_nao_zhuang(Player player, int nao) {

		if (_handler_Nao_henna_xy != null) {
			return _handler_Nao_henna_xy.handler_nao(this, player.get_seat_index(), nao);
		}
		return false;

	}

	/////////////////////////////////////////////////////// send///////////////////////////////////////////
	/////
	/**
	 * 基础状态
	 * 
	 * @return
	 */
	@Override
	public boolean operate_player_status() {
		//// 发送玩家出牌 201 =玩家出牌(CMD_OutCard: operate_player,operate_card)
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PLAYER_STATUS);
		this.load_common_status(roomResponse);
		return this.send_response_to_room(roomResponse);
	}

	/**
	 * 刷新玩家信息
	 * 
	 * @return
	 */
	@Override
	public boolean operate_player_data() {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);
		this.load_player_info_data(roomResponse);

		GRR.add_room_response(roomResponse);

		return this.send_response_to_room(roomResponse);
	}

	/**
	 * 在玩家的前面显示出的牌 --- 发送玩家出牌
	 * 
	 * @param seat_index
	 * @param count
	 * @param cards
	 * @param type
	 * @param to_player
	 * @return
	 */
	//
	@Override
	public boolean operate_out_card(int seat_index, int count, int cards[], int type, int to_player) {
		if (seat_index == GameConstants.INVALID_SEAT) {
			return false;
		}
		//// 发送玩家出牌 201 =玩家出牌(CMD_OutCard: operate_player,operate_card)
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_OUT_CARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(type);// 出牌
		roomResponse.setCardCount(count);

		int flashTime = 60;
		int standTime = 60;
		int gameId = this.getGame_id() == 0 ? 1 : this.getGame_id();
		SysParamModel sysParamModel105 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId)
				.get(1105);
		if (sysParamModel105 != null && sysParamModel105.getVal3() > 0 && sysParamModel105.getVal3() < 1000) {
			flashTime = sysParamModel105.getVal3();
			standTime = sysParamModel105.getVal4();
		}
		roomResponse.setFlashTime(flashTime);
		roomResponse.setStandTime(standTime);
		for (int i = 0; i < count; i++) {

			roomResponse.addCardData(cards[i]);
		}

		if (to_player == GameConstants.INVALID_SEAT) {
			GRR.add_room_response(roomResponse);
			return this.send_response_to_room(roomResponse);
		} else {
			return this.send_response_to_player(to_player, roomResponse);
		}

	}

	@Override
	public boolean operate_out_card_bao_ting(int seat_index, int count, int cards[], int type, int to_player) {
		if (seat_index == GameConstants.INVALID_SEAT) {
			return false;
		}
		//// 发送玩家出牌 201 =玩家出牌(CMD_OutCard: operate_player,operate_card)
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_OUT_CARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(type);// 出牌
		roomResponse.setCardCount(count);
		int flashTime = 60;
		int standTime = 60;
		int gameId = this.getGame_id() == 0 ? 1 : this.getGame_id();
		SysParamModel sysParamModel105 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId)
				.get(1105);
		if (sysParamModel105 != null && sysParamModel105.getVal3() > 0 && sysParamModel105.getVal3() < 1000) {
			flashTime = sysParamModel105.getVal3();
			standTime = sysParamModel105.getVal4();
		}
		roomResponse.setFlashTime(flashTime);
		roomResponse.setStandTime(standTime);

		if (to_player == GameConstants.INVALID_SEAT) {
			for (int i = 0; i < count; i++) {

				roomResponse.addCardData(cards[i]);
			}
			this.send_response_to_player(seat_index, roomResponse);// 自己有值

			GRR.add_room_response(roomResponse);

			roomResponse.clearCardData();
			for (int i = 0; i < count; i++) {

				roomResponse.addCardData(GameConstants.BLACK_CARD);
			}
			this.send_response_to_other(seat_index, roomResponse);// 别人是背着的
		} else {
			if (to_player == seat_index) {
				for (int i = 0; i < count; i++) {
					roomResponse.addCardData(cards[i]);
				}
			} else {
				for (int i = 0; i < count; i++) {
					roomResponse.addCardData(GameConstants.BLACK_CARD);
				}
			}

			this.send_response_to_player(to_player, roomResponse);
		}

		return true;
	}

	public boolean operate_out_card_bao_ting_zhou_kou(int seat_index, int count, int cards[], int type, int to_player) {
		if (seat_index == GameConstants.INVALID_SEAT) {
			return false;
		}
		//// 发送玩家出牌 201 =玩家出牌(CMD_OutCard: operate_player,operate_card)
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_OUT_CARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(type);// 出牌
		roomResponse.setCardCount(count);
		int flashTime = 60;
		int standTime = 60;
		int gameId = this.getGame_id() == 0 ? 1 : this.getGame_id();
		SysParamModel sysParamModel105 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId)
				.get(1105);
		if (sysParamModel105 != null && sysParamModel105.getVal3() > 0 && sysParamModel105.getVal3() < 1000) {
			flashTime = sysParamModel105.getVal3();
			standTime = sysParamModel105.getVal4();
		}
		roomResponse.setFlashTime(flashTime);
		roomResponse.setStandTime(standTime);

		if (to_player == GameConstants.INVALID_SEAT) {
			for (int i = 0; i < count; i++) {

				roomResponse.addCardData(cards[i]);
			}
			this.send_response_to_player(seat_index, roomResponse);// 自己有值

			GRR.add_room_response(roomResponse);

			roomResponse.clearCardData();

			for (int i = 0; i < count; i++) {

				roomResponse.addCardData(cards[i]);
			}
			this.send_response_to_other(seat_index, roomResponse);
		} else {
			if (to_player == seat_index) {
				for (int i = 0; i < count; i++) {
					roomResponse.addCardData(cards[i]);
				}
			} else {
				for (int i = 0; i < count; i++) {
					roomResponse.addCardData(cards[i]);
				}
			}

			this.send_response_to_player(to_player, roomResponse);
		}

		return true;
	}

	// 添加牌到牌堆
	@Override
	public boolean operate_add_discard(int seat_index, int count, int cards[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_ADD_DISCARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(2);// 出牌
		roomResponse.setCardCount(count);
		for (int i = 0; i < count; i++) {
			roomResponse.addCardData(cards[i]);
		}
		GRR.add_room_response(roomResponse);
		this.send_response_to_room(roomResponse);

		return true;
	}

	/**
	 * 效果 (通知玩家弹出 吃碰杆 胡牌==效果)
	 * 
	 * @param seat_index
	 * @param effect_type
	 * @param effect_count
	 * @param effect_indexs
	 * @param time
	 * @param to_player
	 * @return
	 */
	@Override
	public boolean operate_effect_action(int seat_index, int effect_type, int effect_count, long effect_indexs[],
			int time, int to_player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_EFFECT_ACTION);
		roomResponse.setEffectType(effect_type);
		roomResponse.setTarget(seat_index);
		roomResponse.setEffectCount(effect_count);
		for (int i = 0; i < effect_count; i++) {
			roomResponse.addEffectsIndex(effect_indexs[i]);
		}

		roomResponse.setEffectTime(time);

		if (to_player == GameConstants.INVALID_SEAT) {
			this.send_response_to_room(roomResponse);
		} else {
			this.send_response_to_player(to_player, roomResponse);
		}

		GRR.add_room_response(roomResponse);

		return true;
	}

	// int seat_index, int effect_type, int effect_count, long effect_indexs[],
	// int time, int to_player
	public boolean operate_shai_zi_effect(int num1, int num2, int anim_time, int delay_time) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_EFFECT_ACTION);
		roomResponse.setEffectType(GameConstants.Effect_Action_SHAI_ZI);
		roomResponse.setTarget(get_target_shai_zi_player(num1, num2));
		roomResponse.setEffectCount(2);
		roomResponse.addEffectsIndex(num1);
		roomResponse.addEffectsIndex(num2);
		roomResponse.setEffectTime(anim_time);// anim time//摇骰子动画的时间
		roomResponse.setStandTime(delay_time); // delay time//停留时间
		this.send_response_to_room(roomResponse);
		GRR.add_room_response(roomResponse);
		return true;
	}

	// 计算中骰子的玩家
	public int get_target_shai_zi_player(int num1, int num2) {
		return get_zhong_seat_by_value_cslx(num1 + num2, GRR._banker_player);// GameConstants.INVALID_SEAT;
	}

	public String get_xiao_hu_shai_zi_desc(int seat) {
		int target_seat = get_target_shai_zi_player(_player_result.shaizi[seat][0], _player_result.shaizi[seat][1]);
		if (target_seat == seat)
			return "(骰子:" + _player_result.shaizi[seat][0] + "," + _player_result.shaizi[seat][1] + " 全中)";
		else {
			// 庄家dong
			// get_banker_next_seat(banker_seat)
			if (get_players()[target_seat] == null) {
				return "(骰子:" + _player_result.shaizi[seat][0] + "," + _player_result.shaizi[seat][1] + " 不中)";
			}

			String[] str = new String[4];
			str[0] = "东";
			str[1] = "南";
			str[2] = "西";
			str[3] = "北";
			return "(骰子:" + _player_result.shaizi[seat][0] + "," + _player_result.shaizi[seat][1] + " 中"
					+ str[target_seat] + ")";
			/*
			 * if(GRR._banker_player == target_seat){//zhuang dong return
			 * "(骰子:"+_player_result.shaizi[seat][0]+","+_player_result.shaizi[
			 * seat][1]+" 中东)"; }
			 * 
			 * if( get_banker_next_seat(GRR._banker_player) == target_seat )
			 * return
			 * "(骰子:"+_player_result.shaizi[seat][0]+","+_player_result.shaizi[
			 * seat][1]+" 中南)";
			 * 
			 * if( get_banker_pre_seat(GRR._banker_player) == target_seat )
			 * return
			 * "(骰子:"+_player_result.shaizi[seat][0]+","+_player_result.shaizi[
			 * seat][1]+ ( getTablePlayerNumber()==3?" 中西)":" 中北)"); //4人场 对家
			 * return
			 * "(骰子:"+_player_result.shaizi[seat][0]+","+_player_result.shaizi[
			 * seat][1]+" 中西)";
			 */
		}
	}

	@Override
	public boolean record_effect_action(int seat_index, int effect_type, int effect_count, long effect_indexs[],
			int time) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_EFFECT_ACTION);
		roomResponse.setEffectType(effect_type);
		roomResponse.setTarget(seat_index);
		roomResponse.setEffectCount(effect_count);
		for (int i = 0; i < effect_count; i++) {
			roomResponse.addEffectsIndex(effect_indexs[i]);
		}

		roomResponse.setEffectTime(time);

		GRR.add_room_response(roomResponse);

		return true;
	}

	@Override
	public void record_discard_gang(int seat_index) {
		try {
			boolean gang = _playerStatus[seat_index].has_action_by_code(GameConstants.WIK_GANG)
					|| _playerStatus[seat_index].has_action_by_code(GameConstants.WIK_DA_CHAO_TIAN)
					|| _playerStatus[seat_index].has_action_by_code(GameConstants.WIK_MENG_XIAO)
					|| _playerStatus[seat_index].has_action_by_code(GameConstants.WIK_HUI_TOU_XIAO)
					|| _playerStatus[seat_index].has_action_by_code(GameConstants.WIK_DIAN_XIAO)
					|| _playerStatus[seat_index].has_action_by_code(GameConstants.WIK_XIAO_CHAO_TIAN);
			if (gang) {
				for (int i = 0; i < _playerStatus[seat_index]._weave_count; i++) {
					if ((_playerStatus[seat_index]._action_weaves[i].weave_kind == GameConstants.WIK_GANG
							|| _playerStatus[seat_index]._action_weaves[i].weave_kind == GameConstants.WIK_DA_CHAO_TIAN
							|| _playerStatus[seat_index]._action_weaves[i].weave_kind == GameConstants.WIK_MENG_XIAO
							|| _playerStatus[seat_index]._action_weaves[i].weave_kind == GameConstants.WIK_HUI_TOU_XIAO
							|| _playerStatus[seat_index]._action_weaves[i].weave_kind == GameConstants.WIK_DIAN_XIAO
							|| _playerStatus[seat_index]._action_weaves[i].weave_kind == GameConstants.WIK_XIAO_CHAO_TIAN)) {
						int card = _playerStatus[seat_index]._action_weaves[i].center_card;

						boolean find = false;
						for (int j = 0; j < GRR._discard_count_gang[seat_index]; j++) {
							if (GRR._discard_cards_gang[seat_index][j] == card) {
								find = true;
								break;
							}
						}
						if (!find) {
							GRR._discard_count_gang[seat_index]++;
							GRR._discard_cards_gang[seat_index][GRR._discard_count_gang[seat_index] - 1] = card;
						}

					}
				}
			}
		} catch (Exception e) {
			logger.error("error", e);
		}
	}

	@Override
	public boolean isIndiscardGang(int seat_index, int card) {
		for (int j = 0; j < GRR._discard_count_gang[seat_index]; j++) {
			if (GRR._discard_cards_gang[seat_index][j] == card) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 玩家动作--通知玩家弹出/关闭操作
	 * 
	 * @param seat_index
	 * @param close
	 * @return
	 */
	@Override
	public boolean operate_player_action(int seat_index, boolean close) {
		PlayerStatus curPlayerStatus = _playerStatus[seat_index];

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PLAYER_ACTION);
		roomResponse.setTarget(seat_index);
		this.load_common_status(roomResponse);

		if (close == true) {
			GRR.add_room_response(roomResponse);
			// 通知玩家关闭
			this.send_response_to_player(seat_index, roomResponse);
			return true;
		}
		for (int i = 0; i < curPlayerStatus._action_count; i++) {
			roomResponse.addActions(curPlayerStatus._action[i]);
		}
		// 组合数据
		for (int i = 0; i < curPlayerStatus._weave_count; i++) {
			WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
			weaveItem_item.setCenterCard(curPlayerStatus._action_weaves[i].center_card);
			weaveItem_item.setProvidePlayer(curPlayerStatus._action_weaves[i].provide_player);
			weaveItem_item.setPublicCard(curPlayerStatus._action_weaves[i].public_card);
			weaveItem_item.setWeaveKind(curPlayerStatus._action_weaves[i].weave_kind);
			roomResponse.addWeaveItems(weaveItem_item);
		}
		GRR.add_room_response(roomResponse);
		this.send_response_to_player(seat_index, roomResponse);
		return true;
	}

	/**
	 * 玩家动作--通知玩家弹出/关闭操作
	 * 
	 * @param seat_index
	 * @param close
	 * @return
	 */
	public boolean operate_player_action(int seat_index, boolean close, boolean isNotWait) {
		PlayerStatus curPlayerStatus = _playerStatus[seat_index];

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PLAYER_ACTION);
		roomResponse.setTarget(seat_index);
		roomResponse.setIsGoldRoom(isNotWait);// 暂时用金币场这个字段
		this.load_common_status(roomResponse);

		if (close == true) {
			GRR.add_room_response(roomResponse);
			// 通知玩家关闭
			this.send_response_to_player(seat_index, roomResponse);
			return true;
		}
		for (int i = 0; i < curPlayerStatus._action_count; i++) {
			roomResponse.addActions(curPlayerStatus._action[i]);
		}
		// 组合数据
		for (int i = 0; i < curPlayerStatus._weave_count; i++) {
			WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
			weaveItem_item.setCenterCard(curPlayerStatus._action_weaves[i].center_card);
			weaveItem_item.setProvidePlayer(curPlayerStatus._action_weaves[i].provide_player);
			weaveItem_item.setPublicCard(curPlayerStatus._action_weaves[i].public_card);
			weaveItem_item.setWeaveKind(curPlayerStatus._action_weaves[i].weave_kind);
			roomResponse.addWeaveItems(weaveItem_item);
		}
		GRR.add_room_response(roomResponse);
		this.send_response_to_player(seat_index, roomResponse);
		return true;
	}

	/**
	 * 发牌
	 * 
	 * @param seat_index
	 * @param count
	 * @param cards
	 * @param to_player
	 * @return
	 */
	@Override
	public boolean operate_player_get_card(int seat_index, int count, int cards[], int to_player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_SEND_CARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(2);// get牌
		roomResponse.setCardCount(count);

		if (to_player == GameConstants.INVALID_SEAT) {
			this.send_response_to_other(seat_index, roomResponse);

			for (int i = 0; i < count; i++) {
				roomResponse.addCardData(cards[i]);
			}
			GRR.add_room_response(roomResponse);
			return this.send_response_to_player(seat_index, roomResponse);

		} else {
			if (seat_index == to_player) {
				for (int i = 0; i < count; i++) {
					roomResponse.addCardData(cards[i]);
				}
			}
			// GRR.add_room_response(roomResponse);
			return this.send_response_to_player(to_player, roomResponse);
		}

	}

	/**
	 * 刷新玩家的牌--手牌 和 牌堆上的牌
	 * 
	 * @param seat_index
	 * @param card_count
	 * @param cards
	 *            实际的牌数据(手上的牌)
	 * @param weave_count
	 * @param weaveitems
	 *            牌堆中的组合(落地的牌)
	 * @return
	 */
	@Override
	public boolean operate_player_cards(int seat_index, int card_count, int cards[], int weave_count,
			WeaveItem weaveitems[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS);// 12更新玩家手牌数据
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(1);

		this.load_common_status(roomResponse);

		// 手牌数量
		roomResponse.setCardCount(card_count);
		roomResponse.setWeaveCount(weave_count);
		// 组合牌
		if (weave_count > 0) {
			for (int j = 0; j < weave_count; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(weaveitems[j].provide_player);
				weaveItem_item.setPublicCard(weaveitems[j].public_card);
				weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
				weaveItem_item.setCenterCard(weaveitems[j].center_card);
				roomResponse.addWeaveItems(weaveItem_item);
			}
		}

		this.send_response_to_other(seat_index, roomResponse);

		// 手牌--将自己的手牌数据发给自己
		for (int j = 0; j < card_count; j++) {
			roomResponse.addCardData(cards[j]);
		}
		GRR.add_room_response(roomResponse);
		// 自己才有牌数据
		this.send_response_to_player(seat_index, roomResponse);

		return true;
	}

	/**
	 * 刷新玩家的牌--手牌 和 牌堆上的牌
	 * 
	 * @param seat_index
	 * @param card_count
	 * @param cards
	 *            实际的牌数据(手上的牌)
	 * @param weave_count
	 * @param weaveitems
	 *            牌堆中的组合(落地的牌)
	 * @return
	 */
	public boolean operate_player_weave_cards(int seat_index, int weave_count, WeaveItem weaveitems[]) {
		// System.out.println(seat_index);
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_WEAVE_CARDS);// 12更新玩家手牌数据
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(1);

		this.load_common_status(roomResponse);

		// 手牌数量
		// roomResponse.setCardCount(card_count);
		roomResponse.setWeaveCount(weave_count);
		// 组合牌
		if (weave_count > 0) {
			for (int j = 0; j < weave_count; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(weaveitems[j].provide_player);
				weaveItem_item.setPublicCard(weaveitems[j].public_card);
				weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
				weaveItem_item.setCenterCard(weaveitems[j].center_card);
				roomResponse.addWeaveItems(weaveItem_item);
			}
		}

		this.send_response_to_other(seat_index, roomResponse);

		/*
		 * // 手牌--将自己的手牌数据发给自己 for (int j = 0; j < card_count; j++) {
		 * roomResponse.addCardData(cards[j]); }
		 */
		GRR.add_room_response(roomResponse);
		// 自己才有牌数据
		this.send_response_to_player(seat_index, roomResponse);

		return true;
	}

	@Override
	public boolean operate_player_cards_flag(int seat_index, int card_count, int cards[], int weave_count,
			WeaveItem weaveitems[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS);// 12更新玩家手牌数据
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(2);

		// this.load_common_status(roomResponse);
		//
		// //手牌数量
		// roomResponse.setCardCount(card_count);
		// roomResponse.setWeaveCount(weave_count);
		// //组合牌
		// if (weave_count>0) {
		// for (int j = 0; j < weave_count; j++) {
		// WeaveItemResponse.Builder weaveItem_item =
		// WeaveItemResponse.newBuilder();
		// weaveItem_item.setProvidePlayer(weaveitems[j].provide_player);
		// weaveItem_item.setPublicCard(weaveitems[j].public_card);
		// weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
		// weaveItem_item.setCenterCard(weaveitems[j].center_card);
		// roomResponse.addWeaveItems(weaveItem_item);
		// }
		// }
		//
		// this.send_response_to_other(seat_index, roomResponse);
		//
		// // 手牌
		// for (int j = 0; j < card_count; j++) {
		// roomResponse.addCardData(cards[j]);
		// }
		GRR.add_room_response(roomResponse);
		// 自己才有牌数据
		this.send_response_to_player(seat_index, roomResponse);

		return true;
	}

	/**
	 * 刷新玩家的牌
	 * 
	 * @param seat_index
	 * @param card_count
	 * @param cards
	 * @param weave_count
	 * @param weaveitems
	 * @return
	 */
	@Override
	public boolean operate_player_cards_with_ting(int seat_index, int card_count, int cards[], int weave_count,
			WeaveItem weaveitems[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS);// 12更新玩家手牌数据
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(1);

		this.load_common_status(roomResponse);

		// 手牌数量
		roomResponse.setCardCount(card_count);
		roomResponse.setWeaveCount(weave_count);
		// 组合牌
		if (weave_count > 0) {
			for (int j = 0; j < weave_count; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(weaveitems[j].provide_player);
				weaveItem_item.setPublicCard(weaveitems[j].public_card);
				weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
				weaveItem_item.setCenterCard(weaveitems[j].center_card);
				roomResponse.addWeaveItems(weaveItem_item);
			}
		}

		this.send_response_to_other(seat_index, roomResponse);

		// 手牌
		for (int j = 0; j < card_count; j++) {
			roomResponse.addCardData(cards[j]);
		}

		int ting_count = _playerStatus[seat_index]._hu_out_card_count;

		// 打出去可以听牌的个数
		roomResponse.setOutCardCount(ting_count);

		for (int i = 0; i < ting_count; i++) {
			int ting_card_cout = _playerStatus[seat_index]._hu_out_card_ting_count[i];
			roomResponse.addOutCardTingCount(ting_card_cout);

			roomResponse.addOutCardTing(
					_playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants.CARD_ESPECIAL_TYPE_TING);

			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < ting_card_cout; j++) {
				int tmp_card = _playerStatus[seat_index]._hu_out_cards[i][j];
				if (is_mj_type(GameConstants.GAME_TYPE_HENAN_ZHOU_KOU) || is_mj_type(GameConstants.GAME_TYPE_HENAN)
						|| is_mj_type(GameConstants.GAME_TYPE_NEW_HE_NAN)
						|| is_mj_type(GameConstants.GAME_TYPE_JIAO_ZUO_HUAN_HUAN)
						|| is_mj_type(GameConstants.GAME_TYPE_NEW_ZHOU_KOU)
						|| is_mj_type(GameConstants.GAME_TYPE_NEW_ZHU_MA_DIAN)) {
					if (has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN)) {
						if (_logic.is_magic_card(tmp_card)) {
							tmp_card += GameConstants.CARD_ESPECIAL_TYPE_HUN;
						}
					}
				} else if (is_mj_type(GameConstants.GAME_TYPE_HUNAN_SHANG_XIA_GUI)) {
					if (_logic.is_magic_card(tmp_card)) {
						tmp_card += GameConstants.CARD_ESPECIAL_TYPE_GUI;
					}
				}
				int_array.addItem(tmp_card);
			}
			roomResponse.addOutCardTingCards(int_array);
		}

		// 自己才有牌数据
		this.send_response_to_player(seat_index, roomResponse);

		GRR.add_room_response(roomResponse);

		return true;
	}

	/**
	 * 刷新玩家的牌
	 * 
	 * @param seat_index
	 * @param card_count
	 * @param cards
	 * @param weave_count
	 * @param weaveitems
	 * @return
	 */
	public boolean operate_player_cards_with_ting_xc(int seat_index, int card_count, int cards[], int weave_count,
			WeaveItem weaveitems[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS); // 12更新玩家手牌数据
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(1);

		this.load_common_status(roomResponse);

		// 手牌数量
		roomResponse.setCardCount(card_count);
		roomResponse.setWeaveCount(weave_count);
		// 组合牌
		if (weave_count > 0) {
			for (int j = 0; j < weave_count; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(weaveitems[j].provide_player);
				weaveItem_item.setPublicCard(weaveitems[j].public_card);
				weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
				weaveItem_item.setCenterCard(weaveitems[j].center_card);
				roomResponse.addWeaveItems(weaveItem_item);
			}
		}

		this.send_response_to_other(seat_index, roomResponse);

		for (int j = 0; j < card_count; j++) {
			roomResponse.addCardData(cards[j]);
		}

		int ting_count = _playerStatus[seat_index]._hu_out_card_count;

		roomResponse.setOutCardCount(ting_count);

		for (int i = 0; i < ting_count; i++) {
			int ting_card_cout = _playerStatus[seat_index]._hu_out_card_ting_count[i];
			roomResponse.addOutCardTingCount(ting_card_cout);
			if (_logic.is_magic_card(_playerStatus[seat_index]._hu_out_card_ting[i])) {
				roomResponse.addOutCardTing(
						_playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants.CARD_ESPECIAL_TYPE_HUN);
			} else {
				roomResponse.addOutCardTing(
						_playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants.CARD_ESPECIAL_TYPE_TING);
			}

			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < ting_card_cout; j++) {
				if (this._logic.is_magic_card(_playerStatus[seat_index]._hu_out_cards[i][j])) {
					_playerStatus[seat_index]._hu_out_cards[i][j] += GameConstants.CARD_ESPECIAL_TYPE_HUN;
				}
				int_array.addItem(_playerStatus[seat_index]._hu_out_cards[i][j]);
			}
			roomResponse.addOutCardTingCards(int_array);
		}

		this.send_response_to_player(seat_index, roomResponse);

		GRR.add_room_response(roomResponse);

		return true;
	}

	/**
	 * 删除牌堆的牌 (吃碰杆的那张牌 从废弃牌堆上移除)
	 * 
	 * @param seat_index
	 * @param discard_index
	 * @return
	 */
	@Override
	public boolean operate_remove_discard(int seat_index, int discard_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REMOVE_DISCARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setDiscardIndex(discard_index);

		GRR.add_room_response(roomResponse);
		this.send_response_to_room(roomResponse);

		return true;
	}

	/**
	 * 显示胡的牌
	 * 
	 * @param seat_index
	 * @param count
	 * @param cards
	 * @return
	 */
	@Override
	public boolean operate_chi_hu_cards(int seat_index, int count, int cards[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_CHI_HU_CARDS);
		roomResponse.setTarget(seat_index);

		for (int i = 0; i < count; i++) {
			roomResponse.addChiHuCards(cards[i]);
		}

		this.send_response_to_player(seat_index, roomResponse);
		return true;
	}

	/**
	 * 显示胡的牌
	 * 
	 * @param seat_index
	 * @param count
	 * @param cards
	 * @return
	 */
	public boolean operate_chi_hu_henan_xc_cards(int seat_index, int count, int cards[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_CHI_HU_CARDS);
		roomResponse.setTarget(seat_index);
		int[] sendCards = new int[cards.length];
		if (has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN)) {
			for (int i = 0; i < count; i++) {
				sendCards[i] = cards[i];
				if (this._logic.is_magic_card(cards[i])) {
					sendCards[i] += GameConstants.CARD_ESPECIAL_TYPE_HUN;
				}
				roomResponse.addChiHuCards(sendCards[i]);
			}
		} else {
			for (int i = 0; i < count; i++) {
				roomResponse.addChiHuCards(cards[i]);
			}
		}
		this.send_response_to_player(seat_index, roomResponse);
		return true;
	}

	/***
	 * 刷新特殊描述
	 * 
	 * @param txt
	 * @param type
	 * @return
	 */
	@Override
	public boolean operate_especial_txt(String txt, int type) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_ESPECIAL_TXT);
		roomResponse.setEspecialTxtType(type);
		roomResponse.setEspecialTxt(txt);
		this.send_response_to_room(roomResponse);
		return true;
	}

	/**
	 * 牌局中分数结算
	 * 
	 * @param seat_index
	 * @param score
	 * @return
	 */
	@Override
	public boolean operate_player_score(int seat_index, float[] score) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_PLAYER_SCORE);
		roomResponse.setTarget(seat_index);
		this.load_common_status(roomResponse);
		this.load_player_info_data(roomResponse);
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			roomResponse.addScore(_player_result.game_score[i]);
			roomResponse.addOpereateScore(score[i]);
		}
		this.send_response_to_room(roomResponse);
		return true;
	}

	// 12更新玩家手牌数据
	// 13更新玩家牌数据(包含吃碰杠组合)
	// public boolean refresh_hand_cards(int seat_index, boolean send_weave) {
	// RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
	// if (send_weave == true) {
	// roomResponse.setType(13);// 13更新玩家牌数据(包含吃碰杠组合)
	// } else {
	// roomResponse.setType(12);// 12更新玩家手牌数据
	// }
	// roomResponse.setGameStatus(_game_status);
	// roomResponse.setOperatePlayer(seat_index);
	//
	// int hand_cards[] = new int[MJGameConstants.MAX_COUNT];
	// int hand_card_count =
	// _logic.switch_to_cards_data(GRR._cards_index[seat_index], hand_cards);
	//
	// //手牌数量
	// roomResponse.setCardCount(hand_card_count);
	//
	// // 手牌
	// for (int j = 0; j < hand_card_count; j++) {
	// roomResponse.addCardData(hand_cards[j]);
	// }
	//
	// //组合牌
	// if (send_weave == true) {
	// // 13更新玩家牌数据(包含吃碰杠组合)
	// for (int j = 0; j < GRR._weave_count[seat_index]; j++) {
	// WeaveItemResponse.Builder weaveItem_item =
	// WeaveItemResponse.newBuilder();
	// weaveItem_item.setProvidePlayer(GRR._weave_items[seat_index][j].provide_player);
	// weaveItem_item.setPublicCard(GRR._weave_items[seat_index][j].public_card);
	// weaveItem_item.setWeaveKind(GRR._weave_items[seat_index][j].weave_kind);
	// weaveItem_item.setCenterCard(GRR._weave_items[seat_index][j].center_card);
	// roomResponse.addWeaveItems(weaveItem_item);
	// }
	// }
	// // 自己才有牌数据
	// this.send_response_to_room(roomResponse);
	//
	// return true;
	// }

	// 14摊开玩家手上的牌
	// public boolean send_show_hand_card(int seat_index, int cards[]) {
	// RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
	// roomResponse.setType(14);
	// roomResponse.setCardCount(cards.length);
	// roomResponse.setOperatePlayer(seat_index);
	// for (int i = 0; i < cards.length; i++) {
	// roomResponse.addCardData(cards[i]);
	// }
	//
	// this.send_response_to_room(roomResponse);
	// return true;
	// }

	// public boolean refresh_discards(int seat_index) {
	// if (seat_index == MJGameConstants.INVALID_SEAT) {
	// return false;
	// }
	// RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
	// roomResponse.setType(MsgConstants.RESPONSE_REFRESH_DISCARD);
	// roomResponse.setOperatePlayer(seat_index);
	//
	// int l = GRR._discard_count[seat_index];
	// for (int i = 0; i < l; i++) {
	// roomResponse.addCardData(GRR._discard_cards[seat_index][i]);
	// }
	//
	// this.send_response_to_room(roomResponse);
	//
	// return true;
	// }

	@Override
	public boolean handler_player_offline(Player player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setGameStatus(_game_status);
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		this.load_player_info_data(roomResponse);

		send_response_to_other(player.get_seat_index(), roomResponse);

		// 解散的时候默认同意解散
		// if(_gameRoomRecord!=null &&
		// (_gameRoomRecord.request_player_seat!=MJGameConstants.INVALID_SEAT)){
		// this.handler_release_room(player,
		// MJGameConstants.Release_Room_Type_AGREE);
		// }

		return true;
	}

	@Override
	public boolean send_play_data(int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		load_room_info_data(roomResponse);
		load_player_info_data(roomResponse);

		this.load_common_status(roomResponse);
		// 游戏变量
		tableResponse.setBankerPlayer(GRR._banker_player);
		tableResponse.setCurrentPlayer(_current_player);
		tableResponse.setCellScore(0);

		// 状态变量
		tableResponse.setActionCard(_provide_card);
		// tableResponse.setActionMask((_response[seat_index] == false) ?
		// _player_action[seat_index] : MJGameConstants.WIK_NULL);

		// 历史记录
		tableResponse.setOutCardData(_out_card_data);
		tableResponse.setOutCardPlayer(_out_card_player);

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			tableResponse.addTrustee(false);// 是否托管
			// 剩余牌数
			tableResponse.addDiscardCount(GRR._discard_count[i]);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < 55; j++) {
				int_array.addItem(GRR._discard_cards[i][j]);
			}
			tableResponse.addDiscardCards(int_array);

			// 组合扑克
			tableResponse.addWeaveCount(GRR._weave_count[i]);
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
				weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
				weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			//
			tableResponse.addWinnerOrder(0);

			if (_status_send == true) {
				// 牌

				if (i == _current_player) {
					tableResponse.addCardCount(_logic.get_card_count_by_index(GRR._cards_index[i]) - 1);
				} else {
					tableResponse.addCardCount(_logic.get_card_count_by_index(GRR._cards_index[i]));
				}

			} else {
				// 牌
				tableResponse.addCardCount(_logic.get_card_count_by_index(GRR._cards_index[i]));

			}

		}

		// 数据
		tableResponse
				.setSendCardData(((_send_card_data != GameConstants.INVALID_VALUE) && (_provide_player == seat_index))
						? _send_card_data : GameConstants.INVALID_VALUE);
		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], hand_cards);

		if (_status_send == true) {
			// 牌
			if (seat_index == _current_player) {
				_logic.remove_card_by_data(hand_cards, _send_card_data);
			}
		}

		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);

		this.send_response_to_player(seat_index, roomResponse);

		if (_status_send == true) {
			// 牌
			this.operate_player_get_card(_current_player, 1, new int[] { _send_card_data }, seat_index);
		} else {
			if (_out_card_player != GameConstants.INVALID_SEAT && _out_card_data != GameConstants.INVALID_VALUE) {
				this.operate_out_card(_out_card_player, 1, new int[] { _out_card_data },
						GameConstants.OUT_CARD_TYPE_MID, seat_index);
			} else if (_status_cs_gang == true) {
				this.operate_out_card(this._provide_player, 2, this._gang_card_data.get_cards(),
						GameConstants.OUT_CARD_TYPE_MID, seat_index);
			}
		}

		if (_playerStatus[seat_index].has_action()) {
			this.operate_player_action(seat_index, false);
		}

		return true;

	}

	@Override
	public PlayerResultResponse.Builder process_player_result(int result) {

		this.huan_dou(result);

		// 大赢家
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			_player_result.win_order[i] = -1;
		}
		int win_idx = 0;
		float max_score = 0;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			int winner = -1;
			float s = -999999;
			for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
				if (_player_result.win_order[j] != -1) {
					continue;
				}
				if (_player_result.game_score[j] > s) {
					s = _player_result.game_score[j];
					winner = j;
				}
			}
			if (s >= max_score) {
				max_score = s;
			} else {
				win_idx++;
			}
			if (winner != -1) {
				_player_result.win_order[winner] = win_idx;
				// win_idx++;
			}
		}

		PlayerResultResponse.Builder player_result = PlayerResultResponse.newBuilder();

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			player_result.addGameScore(_player_result.game_score[i]);
			player_result.addWinOrder(_player_result.win_order[i]);

			Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
				lfs.addItem(_player_result.lost_fan_shu[i][j]);
			}

			player_result.addLostFanShu(lfs);

			// if(is_mj_type(MJGameConstants.GAME_TYPE_ZZ) ||
			// is_mj_type(MJGameConstants.GAME_TYPE_HZ)||
			// is_mj_type(MJGameConstants.GAME_TYPE_SHUANGGUI)){
			player_result.addZiMoCount(_player_result.zi_mo_count[i]);
			player_result.addJiePaoCount(_player_result.jie_pao_count[i]);
			player_result.addDianPaoCount(_player_result.dian_pao_count[i]);
			player_result.addAnGangCount(_player_result.an_gang_count[i]);
			player_result.addMingGangCount(_player_result.ming_gang_count[i]);
			player_result.addMenQingCount(_player_result.men_qing[i]);
			player_result.addHaiDiCount(_player_result.hai_di[i]);
			// }else if(is_mj_type(MJGameConstants.GAME_TYPE_CS)||
			// is_mj_type(MJGameConstants.GAME_TYPE_ZHUZHOU)){
			player_result.addDaHuZiMo(_player_result.da_hu_zi_mo[i]);
			player_result.addDaHuJiePao(_player_result.da_hu_jie_pao[i]);
			player_result.addDaHuDianPao(_player_result.da_hu_dian_pao[i]);
			player_result.addXiaoHuZiMo(_player_result.xiao_hu_zi_mo[i]);
			player_result.addXiaoHuJiePao(_player_result.xiao_hu_jie_pao[i]);
			player_result.addXiaoHuDianPao(_player_result.xiao_hu_dian_pao[i]);

			player_result.addPiaoLaiCount(_player_result.piao_lai_count[i]);
			// }
		}

		player_result.setRoomId(this.getRoom_id());
		player_result.setRoomOwnerAccountId(this.getRoom_owner_account_id());
		player_result.setRoomOwnerName(this.getRoom_owner_name());
		player_result.setCreateTime(this.getCreate_time());
		player_result.setRecordId(this.get_record_id());
		player_result.setGameRound(_game_round);
		player_result.setGameRuleDes(get_game_des());
		player_result.setGameRuleIndex(_game_rule_index);
		player_result.setGameTypeIndex(_game_type_index);

		RoomPlayerResponse.Builder room_player = RoomPlayerResponse.newBuilder();
		room_player.setAccountId(this.getCreate_player().getAccount_id());
		room_player.setHeadImgUrl(this.getCreate_player().getAccount_icon());
		room_player.setIp(this.getCreate_player().getAccount_ip());
		room_player.setUserName(this.getCreate_player().getNick_name());
		room_player.setSeatIndex(this.getCreate_player().get_seat_index());
		room_player.setOnline(this.getCreate_player().isOnline() ? 1 : 0);
		room_player.setIpAddr(this.getCreate_player().getAccount_ip_addr());
		room_player.setSex(this.getCreate_player().getSex());
		room_player.setScore(0);
		room_player.setReady(0);
		room_player.setPao(0);
		room_player.setQiang(0);
		if (this.getCreate_player().locationInfor != null) {
			room_player.setLocationInfor(this.getCreate_player().locationInfor);
		}
		player_result.setCreatePlayer(room_player);
		return player_result;
	}

	/**
	 * 加载基础状态 癞子 定鬼 牌型状态 玩家状态
	 * 
	 * @param roomResponse
	 */
	@Override
	public void load_common_status(RoomResponse.Builder roomResponse) {
		roomResponse.setCurrentPlayer(_current_player == GameConstants.INVALID_SEAT ? _resume_player : _current_player);
		if (GRR != null) {
			roomResponse.setLeftCardCount(GRR._left_card_count);
			for (int i = 0; i < GRR._especial_card_count; i++) {
				if (_logic.is_ding_gui_card(GRR._especial_show_cards[i])) {
					roomResponse.addEspecialShowCards(
							GRR._especial_show_cards[i] + GameConstants.CARD_ESPECIAL_TYPE_DING_GUI);
				} else if (_logic.is_lai_gen_card(GRR._especial_show_cards[i])) {
					roomResponse.addEspecialShowCards(
							GRR._especial_show_cards[i] + GameConstants.CARD_ESPECIAL_TYPE_LAI_GEN);
				} else if (_logic.is_ci_card(_logic.switch_to_card_index(GRR._especial_show_cards[i]))) {
					roomResponse
							.addEspecialShowCards(GRR._especial_show_cards[i] + GameConstants.CARD_ESPECIAL_TYPE_CI);
				} else if (_logic.is_ci_card(_logic.switch_to_card_index(GRR._especial_show_cards[i]))) {
					roomResponse
							.addEspecialShowCards(GRR._especial_show_cards[i] + GameConstants.CARD_ESPECIAL_TYPE_CI);
				} else if (_logic.is_wang_ba_card(GRR._especial_show_cards[i])) {
					roomResponse.addEspecialShowCards(
							GRR._especial_show_cards[i] + GameConstants.CARD_ESPECIAL_TYPE_WANG_BA);
				} else {
					roomResponse.addEspecialShowCards(GRR._especial_show_cards[i]);
				}

			}

			if (GRR._especial_txt != "") {
				roomResponse.setEspecialTxt(GRR._especial_txt);
				roomResponse.setEspecialTxtType(GRR._especial_txt_type);
			}
		}
		roomResponse.setGameStatus(_game_status);

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			roomResponse.addCardStatus(_playerStatus[i]._card_status);
			roomResponse.addPlayerStatus(_playerStatus[i].get_status());
		}
	}

	/**
	 * 加载房间里的玩家信息
	 * 
	 * @param roomResponse
	 */
	@Override
	public void load_player_info_data(RoomResponse.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponse.Builder room_player = newPlayerBaseBuilder(rplayer);
			room_player.setAccountId(rplayer.getAccount_id());
//			room_player.setHeadImgUrl(rplayer.getAccount_icon());
			room_player.setIp(rplayer.getAccount_ip());
//			room_player.setUserName(rplayer.getNick_name());
			room_player.setSeatIndex(rplayer.get_seat_index());
			room_player.setOnline(rplayer.isOnline() ? 1 : 0);
			room_player.setIpAddr(rplayer.getAccount_ip_addr());
			room_player.setSex(rplayer.getSex());
			room_player.setScore(_player_result.game_score[i]);
			room_player.setReady(_player_ready[i]);
			room_player.setPao(_player_result.pao[i]);
			room_player.setNao(_player_result.nao[i]);
			room_player.setQiang(_player_result.qiang[i]);
			room_player.setOpenThree(_player_open_less[i] == 0 ? false : true);
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			room_player.setHasPiao(_player_result.haspiao[i]);
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}

			if (_game_status == GameConstants.GS_MJ_PAO) {
				if (null != _playerStatus[i]) {
					// 叫地主的字段值，用来标示玩家是否点了跑呛
					if (_playerStatus[i]._is_pao_qiang) {
						room_player.setJiaoDiZhu(1);
					} else {
						room_player.setJiaoDiZhu(0);
					}
				}
			}

			roomResponse.addPlayers(room_player);
		}
	}

	@Override
	public void clearHasPiao() {
		// int count = getTablePlayerNumber();
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			_player_result.haspiao[i] = 0;
		}
	}

	@Override
	public void progress_banker_select() {
		if (_cur_banker == GameConstants.INVALID_SEAT) {
			_cur_banker = 0;// 创建者的玩家为专家

			// Random random = new Random();//
			// int rand = random.nextInt(6) + 1 + random.nextInt(6) + 1;
			// _banker_select = rand % MJGameConstants.GAME_PLAYER;//
			// ((lSiceCount>>24)+(lSiceCount>>16)-1)%MJGameConstants.GAME_PLAYER;

			_shang_zhuang_player = GameConstants.INVALID_SEAT;
			_lian_zhuang_player = GameConstants.INVALID_SEAT;
		}

		if (is_sys()) {// 金币场 随机庄家
			Random random = new Random();//
			int rand = random.nextInt(6) + 1 + random.nextInt(6) + 1;
			_cur_banker = rand % GameConstants.GAME_PLAYER;//

			_shang_zhuang_player = GameConstants.INVALID_SEAT;
			_lian_zhuang_player = GameConstants.INVALID_SEAT;
		}
	}

	@Override
	public boolean is_mj_type(int type) {
		return _game_type_index == type;
	}

	@Override
	public boolean is_mj(int id) {
		return this.getGame_id() == id;
	}

	@Override
	public void record_game_round(GameEndResponse.Builder game_end) {

		if (GRR != null) {
			game_end.setRecord(GRR.get_video_record());
			long id = BrandIdDict.getInstance().getId();
			game_end.setBrandId(id);

			GameEndResponse ge = game_end.build();

			byte[] gzipByte = ZipUtil.gZip(ge.toByteArray());

			if (!is_sys()) {
				// 记录 to mangodb
				MongoDBServiceImpl.getInstance().childBrand(_gameRoomRecord.getGame_id(), id, this.get_record_id(), "",
						null, null, gzipByte, this.getRoom_id() + "",
						_recordRoomRecord == null ? "" : _recordRoomRecord.getBeginArray(),
						this.getRoom_owner_account_id());
			}

		}
		//
		// if ((cost_dou > 0) && (this._cur_round == 1)) {
		// // 不是正常结束的
		// if ((game_end.getEndType() != GameConstants.Game_End_NORMAL)
		// && (game_end.getEndType() != GameConstants.Game_End_DRAW)) {
		// // 还豆
		// StringBuilder buf = new StringBuilder();
		// buf.append("开局失败[" + game_end.getEndType() + "]" + ":" +
		// this.getRoom_id())
		// .append("game_id:" + this.getGame_id()).append(",game_type_index:" +
		// _game_type_index)
		// .append(",game_round:" + _game_round).append(",房主:" +
		// this.getRoom_owner_account_id())
		// .append(",豆+:" + cost_dou);
		// // 把豆还给玩家
		// AddGoldResultModel result =
		// PlayerServiceImpl.getInstance().addGold(this.getRoom_owner_account_id(),
		// cost_dou, false, buf.toString(), EGoldOperateType.FAILED_ROOM);
		// if (result.isSuccess() == false) {
		// logger.error("房间[" + this.getRoom_id() + "]" + "玩家[" +
		// this.getRoom_owner_account_id() + "]还豆失败");
		// }
		//
		// }
		// }

	}

	private void set_result_describe_he_nan_zhou_kou() {
		int chrTypes;
		long type = 0;
		for (int player = 0; player < getTablePlayerNumber(); player++) {
			StringBuilder gameDesc = new StringBuilder("");

			if (has_rule(GameConstants.GAME_RULE_HENAN_JIA_DI) && player == GRR._banker_player) {
				gameDesc.append(" 庄家加底");
			}

			chrTypes = GRR._chi_hu_rights[player].type_count;

			for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
				type = GRR._chi_hu_rights[player].type_list[typeIndex];

				if (GRR._chi_hu_rights[player].is_valid()) {

					if (type == GameConstants.CHR_ZI_MO) {
						gameDesc.append(" 自摸");
					}

					if (type == GameConstants.CHR_SHU_FAN) {
						gameDesc.append(" 接炮");
					}

					if (type == GameConstants.CHR_HENAN_QIANG_GANG_HU) {
						gameDesc.append(" 抢杠胡");
					}

					if (type == GameConstants.CHR_HENAN_QI_XIAO_DUI) {
						if (has_rule_ex(GameConstants.GAME_RULE_HENAN_QIDUI_DOUBLE))
							gameDesc.append(" 七小对");
					}

					if (type == GameConstants.CHR_HENAN_GANG_KAI) {
						if (has_rule_ex(GameConstants.GAME_RULE_HENAN_GANGHUA_DOUBLE))
							gameDesc.append(" 杠上开花");
					}

					// if (type == GameConstants.CHR_HENAN_ZHOU_KOU_QING_YI_SE)
					// {
					// gameDesc.append(" 清一色");
					// }
				} else if (type == GameConstants.CHR_FANG_PAO) {
					gameDesc.append(" 放炮");
				}
			}

			if (has_rule(GameConstants.GAME_RULE_HENAN_HZBHG) && GRR._end_type == GameConstants.Game_End_DRAW) {
			} else {
				int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0;

				if (GRR != null) {
					for (int tmpPlayer = 0; tmpPlayer < getTablePlayerNumber(); tmpPlayer++) {
						for (int w = 0; w < GRR._weave_count[tmpPlayer]; w++) {
							if (GRR._weave_items[tmpPlayer][w].weave_kind != GameConstants.WIK_GANG) {
								continue;
							}
							if (tmpPlayer == player) {
								if (GRR._weave_items[tmpPlayer][w].provide_player != tmpPlayer) {
									jie_gang++;
								} else {
									if (GRR._weave_items[tmpPlayer][w].public_card == 1) {
										ming_gang++;
									} else {
										an_gang++;
									}
								}
							} else {
								if (GRR._weave_items[tmpPlayer][w].provide_player == player) {
									fang_gang++;
								}
							}
						}
					}
				}

				if (an_gang > 0) {
					gameDesc.append(" 暗杠X" + an_gang);
				}
				if (ming_gang > 0) {
					gameDesc.append(" 明杠X" + ming_gang);
				}
				if (fang_gang > 0) {
					gameDesc.append(" 放杠X" + fang_gang);
				}
				if (jie_gang > 0) {
					gameDesc.append(" 接杠X" + jie_gang);
				}
			}

			GRR._result_des[player] = gameDesc.toString();
		}
	}

	private void set_result_describe_henan_lh() {
		int chrTypes;
		long type = 0;
		for (int player = 0; player < GameConstants.GAME_PLAYER; player++) {
			StringBuilder gameDesc = new StringBuilder("");

			chrTypes = GRR._chi_hu_rights[player].type_count;

			for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
				type = GRR._chi_hu_rights[player].type_list[typeIndex];

				if (GRR._chi_hu_rights[player].is_valid()) {

					if (type == GameConstants.CHR_ZI_MO) {
						gameDesc.append(" 自摸");
					}

					if (type == GameConstants.CHR_SHU_FAN) {
						gameDesc.append(" 接炮");
					}

					if (type == GameConstants.CHR_HENAN_QIANG_GANG_HU) {
						gameDesc.append(" 抢杠胡");
					}

					if (type == GameConstants.CHR_HENAN_QI_XIAO_DUI) {
						gameDesc.append(" 七小对");
					}

					if (type == GameConstants.CHR_HENAN_GANG_KAI) {
						gameDesc.append(" 杠上开花");
					}

					if (type == GameConstants.CHR_HENAN_ZHOU_KOU_QING_YI_SE) {
						gameDesc.append(" 清一色");
					}
				} else if (type == GameConstants.CHR_FANG_PAO) {
					gameDesc.append(" 放炮");
				}
			}

			int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0;

			if (GRR != null) {
				for (int tmpPlayer = 0; tmpPlayer < GameConstants.GAME_PLAYER; tmpPlayer++) {
					for (int w = 0; w < GRR._weave_count[tmpPlayer]; w++) {
						if (GRR._weave_items[tmpPlayer][w].weave_kind != GameConstants.WIK_GANG) {
							continue;
						}
						if (tmpPlayer == player) {
							if (GRR._weave_items[tmpPlayer][w].provide_player != tmpPlayer) {
								jie_gang++;
							} else {
								if (GRR._weave_items[tmpPlayer][w].public_card == 1) {
									ming_gang++;
								} else {
									an_gang++;
								}
							}
						} else {
							if (GRR._weave_items[tmpPlayer][w].provide_player == player) {
								fang_gang++;
							}
						}
					}
				}
			}

			if (an_gang > 0) {
				gameDesc.append(" 暗杠X" + an_gang);
			}
			if (ming_gang > 0) {
				gameDesc.append(" 明杠X" + ming_gang);
			}
			if (fang_gang > 0) {
				gameDesc.append(" 放杠X" + fang_gang);
			}
			if (jie_gang > 0) {
				gameDesc.append(" 接杠X" + jie_gang);
			}

			GRR._result_des[player] = gameDesc.toString();
		}
	}

	private void set_result_descibe_zz() {
		int l;
		long type = 0;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			String des = "";

			if (has_rule(GameConstants.GAME_RULE_HUNAN_ZHUANG_XIAN) && i == GRR._banker_player) {
				des += "庄家加底";
			}
			l = GRR._chi_hu_rights[i].type_count;
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					if (type == GameConstants.CHR_TONG_PAO) {
						des += " 通炮";
						/*
						 * if
						 * (has_rule(GameConstants.GAME_RULE_HUNAN_ZHUANG_XIAN)
						 * && i == GRR._banker_player) { des += " 庄家加底"; }
						 */
					}
					if (type == GameConstants.CHR_ZI_MO) {
						if (!(GRR._chi_hu_rights[i].opr_and(GameConstants.CHR_HUNAN_HZ_QISHOU_HU).is_empty())) {
							des += " 起手自摸";
						} else {
							des += " 自摸";
						}
						if (GRR._count_pick_niao > 0) {
							des += " 中鸟X" + GRR._count_pick_niao;
						}
						/*
						 * if
						 * (has_rule(GameConstants.GAME_RULE_HUNAN_ZHUANG_XIAN)
						 * && i == GRR._banker_player) { des += " 庄家加底"; }
						 */
					}
					if (type == GameConstants.CHR_SHU_FAN) {
						des += " 接炮";
						if (GRR._count_pick_niao > 0) {
							des += " 中鸟X" + GRR._count_pick_niao;
						}
						/*
						 * if
						 * (has_rule(GameConstants.GAME_RULE_HUNAN_ZHUANG_XIAN)
						 * && i == GRR._banker_player) { des += " 庄家加底"; }
						 */
					}
					if (type == GameConstants.CHR_HUNAN_QIANG_GANG_HU) {
						des += " 抢杠胡";
					}
				} else {
					if (type == GameConstants.CHR_FANG_PAO) {
						des += " 放炮";
					}
				}
			}
			int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0;
			if (GRR != null) {
				for (int p = 0; p < GameConstants.GAME_PLAYER; p++) {
					for (int w = 0; w < GRR._weave_count[p]; w++) {
						if (GRR._weave_items[p][w].weave_kind != GameConstants.WIK_GANG) {
							continue;
						}
						if (p == i) {// 自己
							// 接杠
							if (GRR._weave_items[p][w].provide_player != p) {
								jie_gang++;
							} else {
								if (GRR._weave_items[p][w].public_card == 1) {// 明杠
									ming_gang++;
								} else {
									an_gang++;
								}
							}
						} else {
							// 放杠
							if (GRR._weave_items[p][w].provide_player == i) {
								fang_gang++;
							}
						}
					}
				}
			}

			if (an_gang > 0) {
				des += " 暗杠X" + an_gang;
			}
			if (ming_gang > 0) {
				des += " 明杠X" + ming_gang;
			}
			if (fang_gang > 0) {
				des += " 放杠X" + fang_gang;
			}
			if (jie_gang > 0) {
				des += " 接杠X" + jie_gang;
			}

			GRR._result_des[i] = des;
		}
	}

	private void set_result_descibe_hz() {
		int l;
		long type = 0;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			String des = "";

			l = GRR._chi_hu_rights[i].type_count;
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					if (type == GameConstants.CHR_TONG_PAO) {
						des += " 通炮";
					}
					if (type == GameConstants.CHR_ZI_MO) {
						if (!(GRR._chi_hu_rights[i].opr_and(GameConstants.CHR_HUNAN_HZ_QISHOU_HU).is_empty())) {
							des += " 起手自摸";
						} else {
							des += " 自摸";
						}

						if (GRR._count_pick_niao > 0) {
							des += " 中鸟X" + GRR._count_pick_niao;
						}
					}
					if (type == GameConstants.CHR_SHU_FAN) {
						des += " 接炮";
						if (GRR._count_pick_niao > 0) {
							des += " 中鸟X" + GRR._count_pick_niao;
						}
					}
					if (type == GameConstants.CHR_HUNAN_QIANG_GANG_HU) {
						des += " 抢杠胡";
					}
				} else {
					if (type == GameConstants.CHR_FANG_PAO) {
						des += " 放炮";
					}
				}
			}
			int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0;
			if (GRR != null) {
				for (int p = 0; p < GameConstants.GAME_PLAYER; p++) {
					for (int w = 0; w < GRR._weave_count[p]; w++) {
						if (GRR._weave_items[p][w].weave_kind != GameConstants.WIK_GANG) {
							continue;
						}
						if (p == i) {// 自己
							// 接杠
							if (GRR._weave_items[p][w].provide_player != p) {
								jie_gang++;
							} else {
								if (GRR._weave_items[p][w].public_card == 1) {// 明杠
									ming_gang++;
								} else {
									an_gang++;
								}
							}
						} else {
							// 放杠
							if (GRR._weave_items[p][w].provide_player == i) {
								fang_gang++;
							}
						}
					}
				}
			}

			if (an_gang > 0) {
				des += " 暗杠X" + an_gang;
			}
			if (ming_gang > 0) {
				des += " 明杠X" + ming_gang;
			}
			if (fang_gang > 0) {
				des += " 放杠X" + fang_gang;
			}
			if (jie_gang > 0) {
				des += " 接杠X" + jie_gang;
			}

			GRR._result_des[i] = des;
		}
	}

	private void set_result_descibe_lxcg() {
		int l;
		long type = 0;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			String des = "";
			l = GRR._chi_hu_rights[i].type_count;
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					if (type == GameConstants.CHR_TONG_PAO) {
						des += " 通炮";
					}
					if (type == GameConstants.CHR_ZI_MO) {
						if (!(GRR._chi_hu_rights[i].opr_and(GameConstants.CHR_HUNAN_DADOU).is_empty())) {
							des += " 大刀";
						}
						if (!(GRR._chi_hu_rights[i].opr_and(GameConstants.CHR_HUNAN_XIADOU).is_empty())) {
							des += " 小刀";
						}
						if (GRR._count_pick_niao > 0) {
							des += " 中鸟X" + GRR._count_pick_niao;
						}
					}
					if (type == GameConstants.CHR_SHU_FAN) {
						des += " 接炮";
						if (GRR._count_pick_niao > 0) {
							des += " 中鸟X" + GRR._count_pick_niao;
						}
					}
				} else {
					if (type == GameConstants.CHR_FANG_PAO) {
						des += " 放炮";
					}
				}
			}
			GRR._result_des[i] = des;
		}
	}

	private void set_result_descibe_hnhz() {
		int l;
		long type = 0;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			String des = "";

			l = GRR._chi_hu_rights[i].type_count;
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					if (type == GameConstants.CHR_TONG_PAO) {
						des += " 通炮";
					}
					if (type == GameConstants.CHR_ZI_MO) {
						if (!(GRR._chi_hu_rights[i].opr_and(GameConstants.CHR_HENAN_HZ_QISHOU_HU).is_empty())) {
							des += " 起手自摸";
						} else {
							des += " 自摸";
						}

						if (GRR._count_pick_niao > 0) {
							des += " 中鸟X" + GRR._count_pick_niao;
						}
					}
					if (type == GameConstants.CHR_SHU_FAN) {
						des += " 接炮";
						if (GRR._count_pick_niao > 0) {
							des += " 中鸟X" + GRR._count_pick_niao;
						}
					}
					if (type == GameConstants.CHR_HENAN_QIANG_GANG_HU) {
						des += " 抢杠胡";
					}
				} else {
					if (type == GameConstants.CHR_FANG_PAO) {
						des += " 放炮";
					}
				}
			}
			int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0;
			if (GRR != null) {
				for (int p = 0; p < GameConstants.GAME_PLAYER; p++) {
					for (int w = 0; w < GRR._weave_count[p]; w++) {
						if (GRR._weave_items[p][w].weave_kind != GameConstants.WIK_GANG) {
							continue;
						}
						if (p == i) {// 自己
							// 接杠
							if (GRR._weave_items[p][w].provide_player != p) {
								jie_gang++;
							} else {
								if (GRR._weave_items[p][w].public_card == 1) {// 明杠
									ming_gang++;
								} else {
									an_gang++;
								}
							}
						} else {
							// 放杠
							if (GRR._weave_items[p][w].provide_player == i) {
								fang_gang++;
							}
						}
					}
				}
			}

			if (an_gang > 0) {
				des += " 暗杠X" + an_gang;
			}
			if (ming_gang > 0) {
				des += " 明杠X" + ming_gang;
			}
			if (fang_gang > 0) {
				des += " 放杠X" + fang_gang;
			}
			if (jie_gang > 0) {
				des += " 接杠X" + jie_gang;
			}

			GRR._result_des[i] = des;
		}
	}

	@Override
	protected void countCardType(ChiHuRight chiHuRight, int seat_index) {
		try {
			int wFanShu = 0;
			if (getGame_id() == 1) {
				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_PENGPENG_HU)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.mjpph, "",
							_game_type_index, 0l, this.getRoom_id());
				}

				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_JIANGJIANG_HU)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.jjh, "",
							_game_type_index, 0l, this.getRoom_id());
				}

				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_QING_YI_SE)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.qingyise, "",
							_game_type_index, 0l, this.getRoom_id());
				}

				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_HAI_DI_LAO)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.cshaidilao, "",
							_game_type_index, 0l, this.getRoom_id());
				}

				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_HAI_DI_PAO)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.cshaidipao, "",
							_game_type_index, 0l, this.getRoom_id());
				}

				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_QI_XIAO_DUI)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.qidui, "",
							_game_type_index, 0l, this.getRoom_id());
				}

				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.haohuaqidui, "",
							_game_type_index, 0l, this.getRoom_id());
				}
				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_GANG_KAI)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.mjgangshanghua,
							"", _game_type_index, 0l, this.getRoom_id());
				}
				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_QIANG_GANG_HU)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.qiangganghu, "",
							_game_type_index, 0l, this.getRoom_id());
				}

				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_GANG_SHANG_PAO)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.mjgangshangpao,
							"", _game_type_index, 0l, this.getRoom_id());
				}

				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_QUAN_QIU_REN)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.quanqiuren, "",
							_game_type_index, 0l, this.getRoom_id());
				}

				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.shaohuaqidui,
							"", _game_type_index, 0l, this.getRoom_id());
				}

				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_SHUANG_GANG_KAI)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.smjgangshanghua,
							"", _game_type_index, 0l, this.getRoom_id());
				}

				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_SHUANG_GANG_SHANG_PAO)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.smjgangshangpao,
							"", _game_type_index, 0l, this.getRoom_id());
				}

				// 小胡
				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_XIAO_DA_SI_XI)).is_empty())
					wFanShu += 1;
				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_XIAO_BAN_BAN_HU)).is_empty())// 8000
					wFanShu += 1;
				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_XIAO_LIU_LIU_SHUN)).is_empty())
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.liuliushun, "",
							_game_type_index, 0l, this.getRoom_id());
				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_XIAO_QUE_YI_SE)).is_empty())// 10000
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.qingyise, "",
							_game_type_index, 0l, this.getRoom_id());

			}

			if (getGame_id() == 2) {
				if (!(chiHuRight.opr_and(GameConstants.CHR_HUBEI_HEI_MO)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.heimo, "",
							_game_type_index, 0l, this.getRoom_id());
				}
				if (!(chiHuRight.opr_and(GameConstants.CHR_HUBEI_RUAN_MO)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.ruanmo, "",
							_game_type_index, 0l, this.getRoom_id());
				}
				if (!(chiHuRight.opr_and(GameConstants.CHR_HUBEI_ZHUO_CHONG)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.zhuotong, "",
							_game_type_index, 0l, this.getRoom_id());
				}
				if (!(chiHuRight.opr_and(GameConstants.CHR_HUBEI_RE_CHONG)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.rechong, "",
							_game_type_index, 0l, this.getRoom_id());
				}
			}

			if (getGame_id() == 3) {
				if (!(chiHuRight.opr_and(GameConstants.CHR_HENAN_QISHOU_HU)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.sihun, "",
							_game_type_index, 0l, this.getRoom_id());
				}
				if (!(chiHuRight.opr_and(GameConstants.CHR_HENAN_QIANG_GANG_HU)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.henangang, "",
							_game_type_index, 0l, this.getRoom_id());
				}

				if (!(chiHuRight.opr_and(GameConstants.CHR_HENAN_QI_XIAO_DUI)).is_empty()
						|| !(chiHuRight.opr_and(GameConstants.CHR_HENAN_HH_QI_XIAO_DUI)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.henanqidui, "",
							_game_type_index, 0l, this.getRoom_id());
				}

				if (!(chiHuRight.opr_and(GameConstants.CHR_HENAN_HH_QI_XIAO_DUI)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index],
							ECardType.henanqiduihaohua, "", _game_type_index, 0l, this.getRoom_id());
				}

				if (!(chiHuRight.opr_and(GameConstants.CHR_HENAN_GANG_KAI)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.henankaihua, "",
							_game_type_index, 0l, this.getRoom_id());
				}
				if (!(chiHuRight.opr_and(GameConstants.CHR_HENAN_HZ_QISHOU_HU)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.henan4hong, "",
							_game_type_index, 0l, this.getRoom_id());
				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void set_result_describe_sg() {
		int l;
		long type = 0;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			String des = "";

			l = GRR._chi_hu_rights[i].type_count;
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					if (type == GameConstants.CHR_TONG_PAO) {
						des += " 通炮";
					}
					if (type == GameConstants.CHR_ZI_MO) {
						des += " 自摸";
						if (GRR._count_pick_niao > 0) {
							des += " 中鸟X" + GRR._count_pick_niao;
						}
					}
					if (type == GameConstants.CHR_SHU_FAN) {
						des += " 接炮";
						if (GRR._count_pick_niao > 0) {
							des += " 中鸟X" + GRR._count_pick_niao;
						}
					}
					if (type == GameConstants.CHR_HUNAN_QIANG_GANG_HU) {
						des += " 抢杠胡";
					}
				} else {
					if (type == GameConstants.CHR_FANG_PAO) {
						des += " 放炮";
					}
				}
			}
			int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0;
			if (GRR != null) {
				for (int p = 0; p < GameConstants.GAME_PLAYER; p++) {
					for (int w = 0; w < GRR._weave_count[p]; w++) {
						if (GRR._weave_items[p][w].weave_kind != GameConstants.WIK_GANG) {
							continue;
						}
						if (p == i) {// 自己
							// 接杠
							if (GRR._weave_items[p][w].provide_player != p) {
								jie_gang++;
							} else {
								if (GRR._weave_items[p][w].public_card == 1) {// 明杠
									ming_gang++;
								} else {
									an_gang++;
								}
							}
						} else {
							// 放杠
							if (GRR._weave_items[p][w].provide_player == i) {
								fang_gang++;
							}
						}
					}
				}
			}

			if (an_gang > 0) {
				des += " 暗杠X" + an_gang;
			}
			if (ming_gang > 0) {
				des += " 明杠X" + ming_gang;
			}
			if (fang_gang > 0) {
				des += " 放杠X" + fang_gang;
			}
			if (jie_gang > 0) {
				des += " 接杠X" + jie_gang;
			}

			GRR._result_des[i] = des;
		}
	}

	private void set_result_describe_xthh() {
		int l;
		long type = 0;
		// int hjh = this.hei_jia_hei();
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			String des = "";

			l = GRR._chi_hu_rights[i].type_count;
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					if (type == GameConstants.CHR_TONG_PAO) {
						des += " 通炮";
					} else if (type == GameConstants.CHR_HUBEI_HEI_MO) {
						des += " 黑摸";
					} else if (type == GameConstants.CHR_HUBEI_RUAN_MO) {
						des += " 软摸";
					} else if (type == GameConstants.CHR_HUBEI_ZHUO_CHONG) {
						des += " 捉铳";
					} else if (type == GameConstants.CHR_HUBEI_RE_CHONG) {
						des += " 热铳";
					} else if (type == GameConstants.CHR_HUBEI_QIANG_GANG_HU) {
						des += " 抢杠胡";
					}
					if (type == GameConstants.CHR_SHU_FAN) {
						des += " 放铳";
					}
				} else {
					if (type == GameConstants.CHR_FANG_PAO) {
						des += " 放铳";
					}
				}
			}
			int meng_xiao = 0, dian_xiao = 0, hui_tou_xiao = 0, xiao_chao_tian = 0, da_chao_tian = 0, fang_xiao = 0;
			if (GRR != null) {
				for (int p = 0; p < GameConstants.GAME_PLAYER; p++) {
					for (int w = 0; w < GRR._weave_count[p]; w++) {
						if (p == i) {// 自己
							if (GRR._weave_items[p][w].weave_kind == GameConstants.WIK_MENG_XIAO) {
								meng_xiao++;
							} else if (GRR._weave_items[p][w].weave_kind == GameConstants.WIK_DIAN_XIAO) {
								dian_xiao++;
							} else if (GRR._weave_items[p][w].weave_kind == GameConstants.WIK_HUI_TOU_XIAO) {
								hui_tou_xiao++;
							} else if (GRR._weave_items[p][w].weave_kind == GameConstants.WIK_XIAO_CHAO_TIAN) {
								xiao_chao_tian++;
							} else if (GRR._weave_items[p][w].weave_kind == GameConstants.WIK_DA_CHAO_TIAN) {
								da_chao_tian++;
							} else {
							}
						} else {
							// 放杠笑
							if ((GRR._weave_items[p][w].weave_kind == GameConstants.WIK_DIAN_XIAO
									|| GRR._weave_items[p][w].weave_kind == GameConstants.WIK_XIAO_CHAO_TIAN)
									&& GRR._weave_items[p][w].provide_player == i) {
								fang_xiao++;
							}

						}

					}
				}
			}

			if (meng_xiao > 0) {
				des += " 闷笑X" + meng_xiao;
			}
			if (dian_xiao > 0) {
				des += " 点笑X" + dian_xiao;
			}
			if (hui_tou_xiao > 0) {
				des += " 回头笑X" + hui_tou_xiao;
			}
			if (xiao_chao_tian > 0) {
				des += " 小朝天";
			}
			if (da_chao_tian > 0) {
				des += " 大朝天";
			}
			if (fang_xiao > 0) {
				des += " 放笑X" + fang_xiao;
			}

			// if(hjh == i){
			// des+=" 黑加黑";
			// }

			int piao_lai_cout = 0;
			for (int j = 0; j < GRR._piao_lai_count; j++) {
				if (GRR._piao_lai_seat[j] == i) {
					piao_lai_cout++;
				}
			}
			if (piao_lai_cout > 0) {
				des += " 飘赖*" + piao_lai_cout;
			}

			GRR._result_des[i] = des;
		}
	}

	/**
	 * 安阳麻将结算描述
	 */
	private void set_result_describe_ay() {
		int l;
		long type = 0;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			String des = "";

			l = GRR._chi_hu_rights[i].type_count;
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					if (type == GameConstants.CHR_ZI_MO) {
						des += " 自摸";
					}
					if (type == GameConstants.CHR_SHU_FAN) {
						des += " 接炮";
					}
					if (type == GameConstants.CHR_HENAN_QIANG_GANG_HU) {
						des += " 抢杠胡";
					}

					if (type == GameConstants.CHR_HENAN_DAN_DIAO) {
						des += " 单吊";
					} else if (type == GameConstants.CHR_HENAN_KA_ZHANG) {
						des += " 卡张";
					} else if (type == GameConstants.CHR_HENAN_BIAN_ZHANG) {
						des += " 边张";
					}
				} else {
					if (type == GameConstants.CHR_FANG_PAO) {
						des += " 放炮";
					}
				}
			}
			if (GRR._chi_hu_rights[i].hua_count > 0) {
				des += " 财神";
			}
			int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0;
			if (GRR != null) {
				for (int p = 0; p < GameConstants.GAME_PLAYER; p++) {
					for (int w = 0; w < GRR._weave_count[p]; w++) {
						if (GRR._weave_items[p][w].weave_kind != GameConstants.WIK_GANG) {
							continue;
						}
						if (p == i) {// 自己
							// 接杠
							if (GRR._weave_items[p][w].provide_player != p) {
								jie_gang++;
							} else {
								if (GRR._weave_items[p][w].public_card == 1) {// 明杠
									ming_gang++;
								} else {
									an_gang++;
								}
							}
						} else {
							// 放杠
							if (GRR._weave_items[p][w].provide_player == i) {
								fang_gang++;
							}
						}
					}
				}
			}

			if (an_gang > 0) {
				des += " 暗杠X" + an_gang;
			}
			if (ming_gang > 0) {
				des += " 明杠X" + ming_gang;
			}
			if (fang_gang > 0) {
				des += " 放杠X" + fang_gang;
			}
			if (jie_gang > 0) {
				des += " 接杠X" + jie_gang;
			}

			GRR._result_des[i] = des;
		}
	}

	/**
	 * 林州麻将结算描述
	 */
	private void set_result_describe_lz() {
		int l;
		long type = 0;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			String des = "";

			l = GRR._chi_hu_rights[i].type_count;
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					if (type == GameConstants.CHR_ZI_MO) {
						des += " 自摸";
					}
					if (type == GameConstants.CHR_SHU_FAN) {
						des += " 接炮";
					}
					if (type == GameConstants.CHR_HENAN_QIANG_GANG_HU) {
						des += " 抢杠胡";
					}
					if (type == GameConstants.CHR_HENAN_TIAN_HU) {
						des += " 天胡";
					}
					if (type == GameConstants.CHR_HENAN_QI_XIAO_DUI) {
						des += " 七小对";
					}
					if (type == GameConstants.CHR_HENAN_HH_QI_XIAO_DUI) {
						des += " 豪华七小对";
					}
				} else {
					if (type == GameConstants.CHR_FANG_PAO) {
						des += " 放炮";
					}
				}
			}
			int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0;
			if (GRR != null) {
				for (int p = 0; p < GameConstants.GAME_PLAYER; p++) {
					for (int w = 0; w < GRR._weave_count[p]; w++) {
						if (GRR._weave_items[p][w].weave_kind != GameConstants.WIK_GANG) {
							continue;
						}
						if (p == i) {// 自己
							// 接杠
							if (GRR._weave_items[p][w].provide_player != p) {
								jie_gang++;
							} else {
								if (GRR._weave_items[p][w].public_card == 1) {// 明杠
									ming_gang++;
								} else {
									an_gang++;
								}
							}
						} else {
							// 放杠
							if (GRR._weave_items[p][w].provide_player == i) {
								fang_gang++;
							}
						}
					}
				}
			}

			if (an_gang > 0) {
				des += " 暗杠X" + an_gang;
			}
			if (ming_gang > 0) {
				des += " 明杠X" + ming_gang;
			}
			if (fang_gang > 0) {
				des += " 放杠X" + fang_gang;
			}
			if (jie_gang > 0) {
				des += " 接杠X" + jie_gang;
			}

			GRR._result_des[i] = des;
		}
	}

	/**
	 * 河南麻将结算描述
	 */
	private void set_result_describe_henan() {
		// 杠牌，每个人的分数
		int lGangScore[] = new int[GameConstants.GAME_PLAYER];
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {

			for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
				for (int k = 0; k < GameConstants.GAME_PLAYER; k++) {
					lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
				}
			}
		}

		int l;
		long type = 0;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			String des = "";

			if (has_rule(GameConstants.GAME_RULE_HENAN_JIA_DI) && i == GRR._banker_player) {
				des += " 庄家加底";
			}

			// if(has_rule(MJGameConstants.GAME_TYPE_HENAN_GANG_PAO) &&
			// i==GRR._banker_player) {
			// des+=" 杠跑";
			// }

			l = GRR._chi_hu_rights[i].type_count;
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					if (type == GameConstants.CHR_ZI_MO) {
						if (has_rule_ex(GameConstants.GAME_RULE_ZI_MO_FAN_BEI)) {
							des += " 自摸翻倍";
						} else {
							des += " 自摸";
						}
					}

					if (type == GameConstants.CHR_HENAN_QISHOU_HU) {
						if (is_mj_type(GameConstants.GAME_TYPE_HENAN_NY)
								|| is_mj_type(GameConstants.GAME_TYPE_NEW_NAN_YANG)) {
							des += " 4金加倍";
						} else {
							des += " 4混加倍";
						}

					}

					if (type == GameConstants.CHR_SHU_FAN) {
						des += " 接炮";
					}
					if (type == GameConstants.CHR_HENAN_QIANG_GANG_HU) {
						des += " 抢杠胡";
					}
					if (type == GameConstants.CHR_HENAN_TIAN_HU) {
						des += " 天胡";
					}
					if (type == GameConstants.CHR_HENAN_QI_XIAO_DUI) {
						if (has_rule(GameConstants.GAME_RULE_HENAN_QIDUI_DOUBLE)) {
							des += " 七小对加倍";
						} else {
							des += " 七小对";
						}
					}
					if (type == GameConstants.CHR_HENAN_HEI_ZI) {
						if (is_mj_type(GameConstants.GAME_TYPE_HENAN_PDS)
								|| is_mj_type(GameConstants.GAME_TYPE_NEW_PING_DING_SHAN)) {
							des += "黑子";
						}
					}
					if (type == GameConstants.CHR_HENAN_HH_QI_XIAO_DUI) {
						if (has_rule(GameConstants.GAME_RULE_HENAN_HAOQI)) {
							des += " 豪七四倍";
						} else {
							des += " 豪华七小对";
						}
					}
					if (type == GameConstants.CHR_HENAN_GANG_KAI) {
						if (has_rule(GameConstants.GAME_RULE_HENAN_GANGHUA_DOUBLE)) {
							des += " 杠上花加倍";
						} else {
							des += " 杠上花";
						}

						if (has_rule_ex(GameConstants.GAME_RULE_ZI_MO_FAN_BEI)) {
							des += " 自摸翻倍";
						}

					}
				} else {
					if (type == GameConstants.CHR_FANG_PAO) {
						des += " 放炮";
					}
				}
			}
			int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0;
			if (GRR != null) {
				for (int p = 0; p < GameConstants.GAME_PLAYER; p++) {
					for (int w = 0; w < GRR._weave_count[p]; w++) {
						if (GRR._weave_items[p][w].weave_kind != GameConstants.WIK_GANG) {
							continue;
						}
						if (p == i) {// 自己
							// 接杠
							if (GRR._weave_items[p][w].provide_player != p) {
								jie_gang++;
							} else {
								if (GRR._weave_items[p][w].public_card == 1) {// 明杠
									ming_gang++;
								} else {
									an_gang++;
								}
							}
						} else {
							// 放杠
							if (GRR._weave_items[p][w].provide_player == i) {
								fang_gang++;
							}
						}
					}
				}
			}

			if (an_gang > 0) {
				des += " 暗杠X" + an_gang;
			}
			if (ming_gang > 0) {
				des += " 明杠X" + ming_gang;
			}
			if (fang_gang > 0) {
				des += " 放杠X" + fang_gang;
			}
			if (jie_gang > 0) {
				des += " 接杠X" + jie_gang;
			}
			if (lGangScore[i] != 0 && GRR._end_type == GameConstants.Game_End_NORMAL) {
				des += " 杠总分(" + lGangScore[i] + ")";
			}
			GRR._result_des[i] = des;
		}
	}

	/**
	 * 河南洛阳杠次麻将结算描述
	 */
	private void set_result_describe_henan_lygc() {
		// 杠牌，每个人的分数
		int lGangScore[] = new int[GameConstants.GAME_PLAYER];
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {

			for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
				for (int k = 0; k < GameConstants.GAME_PLAYER; k++) {
					lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
				}
			}
		}

		int l;
		long type = 0;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			String des = "";

			if (has_rule(GameConstants.GAME_RULE_HENAN_JIA_DI) && i == GRR._banker_player) {
				des += " 庄家翻倍";
			}

			// if(has_rule(MJGameConstants.GAME_TYPE_HENAN_GANG_PAO) &&
			// i==GRR._banker_player) {
			// des+=" 杠跑";
			// }

			boolean has_bao_ci = false;

			l = GRR._chi_hu_rights[i].type_count;
			for (int j = l - 1; j >= 0; j--) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					if (type == GameConstants.CHR_ZI_MO) {
						des += " 自摸";
					}

					if (type == GameConstants.CHR_HENAN_PI_CI) {
						des += " 皮次";
						des = des.replace(" 自摸", "");
					}

					if (type == GameConstants.CHR_HENAN_HZ_QISHOU_HU) {
						des += " 包次";
					}

					if (type == GameConstants.CHR_HENAN_HZ_DUAN_1) {
						des += " 明次";
					}

					if (type == GameConstants.CHR_HENAN_HZ_DUAN_2) {
						des += " 暗次";
					}

					if (type == GameConstants.CHR_SHU_FAN) {
						des += " 接炮";
					}
					if (type == GameConstants.CHR_HENAN_QIANG_GANG_HU) {
						des += " 抢杠胡";
					}
					if (type == GameConstants.CHR_HENAN_TIAN_HU) {
						des += " 天胡";
					}
					if (type == GameConstants.CHR_HENAN_QI_XIAO_DUI) {
						if (has_rule(GameConstants.GAME_RULE_HENAN_QIDUI_DOUBLE)) {
							des += " 七小对加倍";
						} else {
							des += " 七小对";
						}
					}
					if (type == GameConstants.CHR_HENAN_HH_QI_XIAO_DUI) {
						if (has_rule(GameConstants.GAME_RULE_HENAN_HAOQI)) {
							des += " 豪七四倍";
						} else {
							des += " 豪华七小对";
						}
					}
					/*
					 * if (type == GameConstants.CHR_HENAN_GANG_KAI) { if
					 * (has_rule(GameConstants.GAME_RULE_HENAN_GANGHUA_DOUBLE))
					 * { des += " 杠上花加倍"; } else { des += " 杠上花"; }
					 * 
					 * }
					 */
				} else {
					if (type == GameConstants.CHR_HENAN_LYGC_FANG_PAO) {
						des += " 放炮";
					}
				}
			}
			int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0;
			if (GRR != null) {
				for (int p = 0; p < GameConstants.GAME_PLAYER; p++) {
					for (int w = 0; w < GRR._weave_count[p]; w++) {
						if (GRR._weave_items[p][w].weave_kind != GameConstants.WIK_GANG) {
							continue;
						}
						if (p == i) {// 自己
							// 接杠
							if (GRR._weave_items[p][w].provide_player != p) {
								jie_gang++;
							} else {
								if (GRR._weave_items[p][w].public_card == 1) {// 明杠
									ming_gang++;
								} else {
									an_gang++;
								}
							}
						} else {
							// 放杠
							if (GRR._weave_items[p][w].provide_player == i) {
								fang_gang++;
							}
						}
					}
				}
			}

			if (an_gang > 0) {
				des += " 暗杠X" + an_gang;
			}
			if (ming_gang > 0) {
				des += " 明杠X" + ming_gang;
			}
			if (fang_gang > 0) {
				des += " 放杠X" + fang_gang;
			}
			if (jie_gang > 0) {
				des += " 接杠X" + jie_gang;
			}
			if (lGangScore[i] != 0 && GRR._end_type == GameConstants.Game_End_NORMAL) {
				des += " 杠总分(" + lGangScore[i] + ")";
			}
			GRR._result_des[i] = des;
		}
	}

	// 转转麻将结束描述
	@Override
	protected void set_result_describe() {
		if (this.is_mj_type(GameConstants.GAME_TYPE_ZZ) || this.is_mj_type(GameConstants.GAME_TYPE_HENAN_ZHUAN_ZHUAN)
				|| is_mj_type(GameConstants.GAME_TYPE_HALL_ZHUAN_ZHUAN_MJ)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_SAN_MEN_XIA)) { // liuyan
																			// 2017/7/10
			set_result_descibe_zz();
		} else if (is_mj_type(GameConstants.GAME_TYPE_HZ) || is_mj_type(GameConstants.GAME_TYPE_FLS_HZ_LX)
				|| is_mj_type(GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_YUYANG_HONGZHON)
				|| is_mj_type(GameConstants.GAME_TYPE_HONG_ZHONG_MJ_TH)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_HONG_ZHONG_FEI)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_PING_XIANG_ZZ)) {
			set_result_descibe_hz();
		} else if (this.is_mj_type(GameConstants.GAME_TYPE_CS) || is_mj_type(GameConstants.GAME_TYPE_HALL_CHANG_SHA_MJ)
				|| is_mj_type(GameConstants.GAME_TYPE_THJ_CS)) {
			set_result_describe_cs();
		} else if (is_mj_type(GameConstants.GAME_TYPE_SHUANGGUI)
				|| is_mj_type(GameConstants.GAME_TYPE_HUNAN_SHANG_XIA_GUI)) {
			set_result_describe_sg();
		} else if (is_mj_type(GameConstants.GAME_TYPE_ZHUZHOU)) {
			set_result_describe_zhuzhou();
		} else if (is_mj_type(GameConstants.GAME_TYPE_XTHH)) {
			set_result_describe_xthh();
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_AY) || is_mj_type(GameConstants.GAME_TYPE_NEW_AN_YANG)) {
			set_result_describe_ay();
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_LZ) || is_mj_type(GameConstants.GAME_TYPE_NEW_LIN_ZHOU)) {
			set_result_describe_lz();
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN) || is_mj_type(GameConstants.GAME_TYPE_HENAN_ZMD)
				|| is_mj_type(GameConstants.GAME_TYPE_HENAN_KF) || is_mj_type(GameConstants.GAME_TYPE_HENAN_NY)
				|| is_mj_type(GameConstants.GAME_TYPE_HENAN_XX) || is_mj_type(GameConstants.GAME_TYPE_HENAN_PDS)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_HE_NAN) || is_mj_type(GameConstants.GAME_TYPE_NEW_ZHU_MA_DIAN)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_KAI_FENG) || is_mj_type(GameConstants.GAME_TYPE_NEW_NAN_YANG)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_XIN_XIANG)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_PING_DING_SHAN)
				|| is_mj_type(GameConstants.GAME_TYPE_JIAO_ZUO_HUAN_HUAN)) {
			set_result_describe_henan();
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_HZ)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_HN_HONG_ZHONG)) {
			set_result_descibe_hnhz();
		} else if (is_mj_type(GameConstants.GAME_TYPE_FLS_LX_CG)) {
			set_result_descibe_lxcg();
		} else if (is_mj_type(GameConstants.GAME_TYPE_FLS_CS_LX) || is_mj_type(GameConstants.GAME_TYPE_FLS_CS_LX_DT)) {
			set_result_descibe_lx_cs();
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_LYGC)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_LUO_YANG_GANG_CI)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_LYGC_DIAN_PAO)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_HE_ZE)) {
			set_result_describe_henan_lygc();
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_ZHOU_KOU)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_ZHOU_KOU)) {
			set_result_describe_he_nan_zhou_kou();
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_LH) || is_mj_type(GameConstants.GAME_TYPE_NEW_LUO_HE)) {
			set_result_describe_henan_lh();
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_XC) || is_mj_type(GameConstants.GAME_TYPE_NEW_XU_CHANG)) {
			set_result_describe_henan_xc();
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_XY)) {
			set_result_describe_henan_xy();
		}

	}

	/**
	 * 河南麻将结算描述
	 */
	private void set_result_describe_henan_xc() {
		int lGangScore[] = new int[GameConstants.GAME_PLAYER];
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
				for (int k = 0; k < GameConstants.GAME_PLAYER; k++) {
					lGangScore[k] += GRR._gang_score[i].scores[j][k];
				}
			}
		}

		int l;
		long type = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			String des = "";

			if (has_rule(GameConstants.GAME_RULE_HENAN_JIA_DI) && i == GRR._banker_player) {
				des += " 庄家加底";
			}

			l = GRR._chi_hu_rights[i].type_count;
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					if (type == GameConstants.CHR_ZI_MO) {
						des += " 自摸";
					}

					if (type == GameConstants.CHR_HENAN_QISHOU_HU) {
						des += " 福禄双全";
					}

					if (type == GameConstants.CHR_SHU_FAN) {
						des += " 接炮";
					}
					if (type == GameConstants.CHR_HENAN_QIANG_GANG_HU) {
						des += " 抢杠胡";
					}
					if (type == GameConstants.CHR_HENAN_TIAN_HU) {
						des += " 天胡";
					}
					if (type == GameConstants.CHR_HENAN_QI_XIAO_DUI || type == GameConstants.CHR_HENAN_HH_QI_XIAO_DUI
							|| type == GameConstants.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI) {
						des += " 七小对";
					}
					if (type == GameConstants.CHR_HENAN_GANG_KAI) {
						if (has_rule(GameConstants.GAME_RULE_HENAN_GANGHUA_DOUBLE)) {
							des += " 杠上花加倍";
						} else {
							des += " 杠上花";
						}
					}
				} else {
					if (type == GameConstants.CHR_FANG_PAO) {
						des += " 放炮";
					}
				}
			}
			if (GRR._chi_hu_rights[i].baifeng_count > 0) {
				des += " 白风" + GRR._chi_hu_rights[i].baifeng_count + "组";
			}
			if (GRR._chi_hu_rights[i].heifeng_count > 0) {
				des += " 黑风" + GRR._chi_hu_rights[i].heifeng_count + "组";
			}
			if (GRR._chi_hu_rights[i].sanhun_kan > 0) {
				des += " 三混成坎";
			}
			if (getRuleValue(GameConstants.GAME_RULE_HENAN_QUE_MEN) == 1 && GRR._chi_hu_rights[i].duanmen_count > 1) {
				des += " 断" + GRR._chi_hu_rights[i].duanmen_count + "门";
			}
			int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0;
			if (GRR != null) {
				for (int p = 0; p < getTablePlayerNumber(); p++) {
					for (int w = 0; w < GRR._weave_count[p]; w++) {
						if (GRR._weave_items[p][w].weave_kind != GameConstants.WIK_GANG) {
							continue;
						}
						if (p == i) {
							if (GRR._weave_items[p][w].provide_player != p) {
								jie_gang++;
							} else {
								if (GRR._weave_items[p][w].public_card == 1) {
									ming_gang++;
								} else {
									an_gang++;
								}
							}
						} else {
							if (GRR._weave_items[p][w].provide_player == i) {
								fang_gang++;
							}
						}
					}
				}
			}

			if (an_gang > 0) {
				des += " 暗杠X" + an_gang;
			}
			if (ming_gang > 0) {
				des += " 明杠X" + ming_gang;
			}
			if (fang_gang > 0) {
				des += " 放杠X" + fang_gang;
			}
			if (jie_gang > 0) {
				des += " 接杠X" + jie_gang;
			}

			if (lGangScore[i] != 0 && GRR._end_type == GameConstants.Game_End_NORMAL) {
				des += " 杠总分(" + lGangScore[i] + ")";
			}

			GRR._result_des[i] = des;
		}
	}

	/**
	 * 信阳麻将结算描述
	 */
	private void set_result_describe_henan_xy() {
		// 杠牌，每个人的分数
		int lGangScore[] = new int[GameConstants.GAME_PLAYER];
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {

			for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
				for (int k = 0; k < GameConstants.GAME_PLAYER; k++) {
					lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
				}
			}
		}

		int l;
		long type = 0;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			String des = "";

			// if(has_rule(MJGameConstants.GAME_TYPE_HENAN_GANG_PAO) &&
			// i==GRR._banker_player) {
			// des+=" 杠跑";
			// }
			if (GRR._nao_win_score[i] != 0) {
				des += "闹庄	" + GRR._nao_win_score[i];
			}
			if (GRR._chi_hu_rights[i].is_valid()) {
				des += " 牌钱";
				des += " 小跑";
			}
			l = GRR._chi_hu_rights[i].type_count;
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					if (type == GameConstants.CHR_ZI_MO) {
						des += " 自摸";
					}
					if (type == GameConstants.CHR_SHU_FAN) {
						des += " 接炮";
					}

					if (type == GameConstants.CHR_HENAN_XY_MENQING) {
						// if (is_mj_type(GameConstants.GAME_TYPE_HENAN_XC)) {
						des += " 门清";
						// } else {
						// des += " 4混加倍";
						// }

					}

					if (type == GameConstants.CHR_HENAN_XY_BAZHANG) {
						des += " 八张";
					}
					if (type == GameConstants.CHR_HENAN_XY_JIAZI) {
						des += " 夹子";
					}
					if (type == GameConstants.CHR_HENAN_XY_DUYING) {
						des += " 独赢";
					}
					if (type == GameConstants.CHR_HENAN_XY_QI_XIAO_DUI) {
						des += " 七小对";

					}
					if (type == GameConstants.CHR_HENAN_XY_QINGQUE) {
						des += " 清缺";
					}
					if (type == GameConstants.CHR_HENAN_XY_HUNQUE) {
						des += " 混缺";
					}
					if (type == GameConstants.CHR_HENAN_XY_QINGYISE) {
						des += " 清一色";
					}
					if (type == GameConstants.CHR_HENAN_XY_SANQIYING) {
						des += " 三七赢";
					}
					if (type == GameConstants.CHR_HENAN_XY_SANQIJIANG) {
						des += " 三七将";
					}
					if (type == GameConstants.CHR_HENAN_XY_ZHONGWU) {
						des += " 中五";
					}
					if (type == GameConstants.CHR_HENAN_XY_LIANLIU) {
						des += " 连六";
					}
					if (type == GameConstants.CHR_HENAN_XY_GANG_KAI) {
						des += " 杠上开花";
					}
				} else {
					if (type == GameConstants.CHR_FANG_PAO) {
						des += " 放炮";
					}
				}
			}
			if (has_rule(GameConstants.GAME_RULE_HENAN_TUIDAOHU)) {
				int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0;
				if (GRR != null) {
					for (int p = 0; p < GameConstants.GAME_PLAYER; p++) {
						for (int w = 0; w < GRR._weave_count[p]; w++) {
							if (GRR._weave_items[p][w].weave_kind != GameConstants.WIK_GANG) {
								continue;
							}
							if (p == i) {// 自己
								// 接杠
								if (GRR._weave_items[p][w].provide_player != p) {
									jie_gang++;
								} else {
									if (GRR._weave_items[p][w].public_card == 1) {// 明杠
										ming_gang++;
									} else {
										an_gang++;
									}
								}
							} else {
								// 放杠
								if (GRR._weave_items[p][w].provide_player == i) {
									fang_gang++;
								}
							}
						}
					}
				}
				if (an_gang > 0) {
					des += " 暗杠X" + an_gang;
				}
				if (ming_gang > 0) {
					des += " 明杠X" + ming_gang;
				}
				if (fang_gang > 0) {
					des += " 放杠X" + fang_gang;
				}
				if (jie_gang > 0) {
					des += " 接杠X" + jie_gang;
				}
				if (lGangScore[i] != 0 && GRR._end_type == GameConstants.Game_End_NORMAL) {
					des += " 杠总分(" + lGangScore[i] + ")";
				}
			}

			GRR._result_des[i] = des;
		}
	}

	// 长沙麻将结束描述
	private void set_result_descibe_lx_cs() {
		int l;
		long type = 0;
		// 有可能是同炮
		boolean has_da_hu = false;
		// 大胡
		boolean dahu[] = { false, false, false, false };
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			if (GRR._chi_hu_rights[i].is_valid() && GRR._chi_hu_rights[i].da_hu_count > 0) {
				if (GRR._chi_hu_rights[i].da_hu_count == 1
						&& !(GRR._chi_hu_rights[i].opr_and(GameConstants.CHR_HUNAN_QIANG_GANG_HU)).is_empty()) {
					dahu[i] = false;
					has_da_hu = false;
				} else {
					dahu[i] = true;
					has_da_hu = true;
				}

			}
		}
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			ChiHuRight chr = GRR._chi_hu_rights[i];
			String des = "";
			// 小胡
			if (GRR._start_hu_right[i].is_valid()) {
				l = GRR._start_hu_right[i].type_count;
				for (int j = 0; j < l; j++) {

					type = GRR._start_hu_right[i].type_list[j];
					if (type == GameConstants.CHR_HUNAN_XIAO_DA_SI_XI) {
						des += " 大四喜";

					}
					if (type == GameConstants.CHR_HUNAN_XIAO_BAN_BAN_HU) {
						des += " 板板胡";

					}
					if (type == GameConstants.CHR_HUNAN_XIAO_QUE_YI_SE) {
						des += " 缺一色";

					}
					if (type == GameConstants.CHR_HUNAN_XIAO_LIU_LIU_SHUN) {
						des += " 六六顺";
					}
					if (type == GameConstants.CHR_HUNAN_XIAO_JING_TONG_YU_NV) {
						des += " 金童玉女";

					}
					if (type == GameConstants.CHR_HUNAN_XIAO_YI_ZHI_HUA) {
						des += " 一枝花";

					}
					if (type == GameConstants.CHR_HUNAN_XIAO_SAN_TON) {
						des += " 三同";

					}
					if (type == GameConstants.CHR_HUNAN_XIAO_BU_BU_GAO) {
						des += " 步步高";
					}

				}
				des += get_xiao_hu_shai_zi_desc(i);
			}

			l = GRR._chi_hu_rights[i].type_count;
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					if (type == GameConstants.CHR_HUNAN_PENGPENG_HU) {
						des += " 碰碰胡";
					}
					if (type == GameConstants.CHR_HUNAN_JIANGJIANG_HU) {
						des += " 将将胡";
					}
					if (type == GameConstants.CHR_HUNAN_QING_YI_SE) {
						des += " 清一色";
					}
					if (type == GameConstants.CHR_HUNAN_HAI_DI_LAO) {
						des += " 海底捞";
					}
					if (type == GameConstants.CHR_HUNAN_HAI_DI_PAO) {
						des += " 海底炮";
					}
					if (type == GameConstants.CHR_HUNAN_QI_XIAO_DUI) {
						des += " 七小对";
					}
					if (type == GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI) {
						des += " 豪华七小对";
					}
					if (type == GameConstants.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI) {
						des += " 双豪华七小对";
					}
					if (type == GameConstants.CHR_HUNAN_GANG_KAI) {
						des += " 杠上开花";
					}
					if (type == GameConstants.CHR_HUNAN_SHUANG_GANG_KAI) {
						des += " 双杠上开花";
					}
					if (type == GameConstants.CHR_HUNAN_QIANG_GANG_HU) {
						des += " 抢杠胡";
					}
					if (type == GameConstants.CHR_HUNAN_GANG_SHANG_PAO) {
						des += " 杠上炮";
					}
					if (type == GameConstants.CHR_HUNAN_SHUANG_GANG_SHANG_PAO) {
						des += " 双杠上炮";
					}
					if (type == GameConstants.CHR_HUNAN_QUAN_QIU_REN) {

						des += " 全求人";
					}
					if (type == GameConstants.CHR_ZI_MO) {
						if (dahu[i] == true) {
							des += " 大胡自摸";
						} else {
							des += " 小胡自摸";
						}
					}
					if (type == GameConstants.CHR_SHU_FAN) {
						if (dahu[i] == true) {
							des += " 大胡接炮";
						} else {
							des += " 小胡接炮";
						}
					}
					if (type == GameConstants.CHR_FANG_PAO) {
						des += " 放炮";
					}
					if (type == GameConstants.CHR_TONG_PAO) {
						des += " 通炮";
					}
				} else {
					if (type == GameConstants.CHR_FANG_PAO) {
						if (has_da_hu == true) {
							des += " 大胡放炮";
						} else {
							des += " 小胡放炮";
						}
					}
				}
			}

			boolean isMutlp = isMutlpDingNiao();
			String mutlp = isMutlp ? "(乘法)" : "(加法)";
			if (GRR._player_niao_count[i] > 0) {
				des += " 定鸟X" + GRR._player_niao_count[i] + mutlp;
			}
			if (GRR._player_niao_count_fei[i] > 0) {
				des += " 飞鸟X" + GRR._player_niao_count_fei[i];
			}
			if (getZuoPiaoScore() > 0) {
				des += " 坐飘" + getZuoPiaoScore() + "分";
			}
			GRR._result_des[i] = des;
		}
	}

	// 长沙麻将结束描述
	private void set_result_describe_cs() {
		int l;
		long type = 0;
		// 有可能是同炮
		boolean has_da_hu = false;
		// 大胡
		boolean dahu[] = { false, false, false, false };
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			if (GRR._chi_hu_rights[i].is_valid() && GRR._chi_hu_rights[i].da_hu_count > 0) {
				dahu[i] = true;
				has_da_hu = true;
			}
		}
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			ChiHuRight chr = GRR._chi_hu_rights[i];
			String des = "";
			// 小胡
			if (GRR._start_hu_right[i].is_valid()) {
				l = GRR._start_hu_right[i].type_count;
				for (int j = 0; j < l; j++) {

					type = GRR._start_hu_right[i].type_list[j];
					if (type == GameConstants.CHR_HUNAN_XIAO_DA_SI_XI) {
						des += " 大四喜";

					}
					if (type == GameConstants.CHR_HUNAN_XIAO_BAN_BAN_HU) {
						des += " 板板胡";

					}
					if (type == GameConstants.CHR_HUNAN_XIAO_QUE_YI_SE) {
						des += " 缺一色";

					}
					if (type == GameConstants.CHR_HUNAN_XIAO_LIU_LIU_SHUN) {
						des += " 六六顺";
					}

				}
			}

			l = GRR._chi_hu_rights[i].type_count;
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					if (type == GameConstants.CHR_HUNAN_PENGPENG_HU) {
						if (chr.is_mul(GameConstants.CHR_HUNAN_PENGPENG_HU)) {
							des += " 碰碰胡*2";
						} else {
							des += " 碰碰胡";
						}
					}
					if (type == GameConstants.CHR_HUNAN_JIANGJIANG_HU) {
						if (chr.is_mul(GameConstants.CHR_HUNAN_JIANGJIANG_HU)) {
							des += " 将将胡*2";
						} else {
							des += " 将将胡";
						}
					}
					if (type == GameConstants.CHR_HUNAN_QING_YI_SE) {
						if (chr.is_mul(GameConstants.CHR_HUNAN_QING_YI_SE)) {
							des += " 清一色*2";
						} else {
							des += " 清一色";
						}
					}
					if (type == GameConstants.CHR_HUNAN_HAI_DI_LAO) {
						des += " 海底捞";
					}
					if (type == GameConstants.CHR_HUNAN_HAI_DI_PAO) {
						des += " 海底炮";
					}
					if (type == GameConstants.CHR_HUNAN_QI_XIAO_DUI) {
						if (chr.is_mul(GameConstants.CHR_HUNAN_QI_XIAO_DUI)) {
							des += " 七小对*2";
						} else {
							des += " 七小对";
						}
					}
					if (type == GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI) {
						if (chr.is_mul(GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI)) {
							des += " 豪华七小对*2";
						} else {
							des += " 豪华七小对";
						}
					}
					if (type == GameConstants.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI) {
						if (chr.is_mul(GameConstants.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI)) {
							des += " 双豪华七小对*2";
						} else {
							des += " 双豪华七小对";
						}
					}
					if (type == GameConstants.CHR_HUNAN_GANG_KAI) {
						if (chr.is_mul(GameConstants.CHR_HUNAN_GANG_KAI)) {
							des += " 杠上开花*2";
						} else {
							des += " 杠上开花";
						}
					}
					if (type == GameConstants.CHR_HUNAN_SHUANG_GANG_KAI) {
						if (chr.is_mul(GameConstants.CHR_HUNAN_SHUANG_GANG_KAI)) {
							des += " 双杠上开花*2";
						} else {
							des += " 双杠上开花";
						}
					}
					if (type == GameConstants.CHR_HUNAN_QIANG_GANG_HU) {
						des += " 抢杠胡";
					}
					if (type == GameConstants.CHR_HUNAN_GANG_SHANG_PAO) {
						if (chr.is_mul(GameConstants.CHR_HUNAN_GANG_SHANG_PAO)) {
							des += " 杠上炮*2";
						} else {
							des += " 杠上炮";
						}
					}
					if (type == GameConstants.CHR_HUNAN_SHUANG_GANG_SHANG_PAO) {
						if (chr.is_mul(GameConstants.CHR_HUNAN_SHUANG_GANG_SHANG_PAO)) {
							des += " 双杠上炮*2";
						} else {
							des += " 双杠上炮";
						}
					}
					if (type == GameConstants.CHR_HUNAN_QUAN_QIU_REN) {

						if (chr.is_mul(GameConstants.CHR_HUNAN_QUAN_QIU_REN)) {
							des += " 全求人*2";
						} else {
							des += " 全求人";
						}
					}
					if (type == GameConstants.CHR_ZI_MO) {
						if (dahu[i] == true) {
							des += " 大胡自摸";
						} else {
							des += " 小胡自摸";
						}
					}
					if (type == GameConstants.CHR_SHU_FAN) {
						if (dahu[i] == true) {
							des += " 大胡接炮";
						} else {
							des += " 小胡接炮";
						}
					}
					if (type == GameConstants.CHR_FANG_PAO) {
						des += " 放炮";
					}
					if (type == GameConstants.CHR_TONG_PAO) {
						des += " 通炮";
					}
				} else {
					if (type == GameConstants.CHR_FANG_PAO) {
						if (has_da_hu == true) {
							des += " 大胡放炮";
						} else {
							des += " 小胡放炮";
						}
					}
				}
			}

			boolean isMutlp = isMutlpDingNiao();
			String mutlp = isMutlp ? "(乘法)" : "(加法)";
			if (GRR._player_niao_count[i] > 0) {
				des += " 定鸟X" + GRR._player_niao_count[i] + mutlp;
			}
			if (GRR._player_niao_count_fei[i] > 0) {
				des += " 飞鸟X" + GRR._player_niao_count_fei[i];
			}
			if (getZuoPiaoScore() > 0) {
				des += " 坐飘" + getZuoPiaoScore() + "分";
			}
			GRR._result_des[i] = des;
		}
	}

	// 株洲麻将结束描述
	private void set_result_describe_zhuzhou() {
		int l;
		long type = 0;
		// 有可能是同炮
		boolean has_da_hu = false;
		// 大胡
		boolean dahu[] = { false, false, false, false };
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			if (GRR._chi_hu_rights[i].is_valid() && GRR._chi_hu_rights[i].da_hu_count > 0) {
				dahu[i] = true;
				has_da_hu = true;
			}
		}
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			String des = "";

			l = GRR._chi_hu_rights[i].type_count;
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					if (type == GameConstants.CHR_HUNAN_PENGPENG_HU) {
						des += " 碰碰胡";
					}
					// if (type == MJGameConstants.CHR_HUNAN_JIANGJIANG_HU) {
					// des += " 将将胡";
					// }
					if (type == GameConstants.CHR_HUNAN_QING_YI_SE) {
						des += " 清一色";
					}
					if (type == GameConstants.CHR_HUNAN_HAI_DI_LAO) {
						des += " 海底捞";
					}
					if (type == GameConstants.CHR_HUNAN_HAI_DI_PAO) {
						des += " 海底炮";
					}
					if (type == GameConstants.CHR_HUNAN_QI_XIAO_DUI) {
						des += " 七小对";
					}
					if (type == GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI) {
						des += " 豪华七小对";
					}
					if (type == GameConstants.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI) {
						des += " 双豪华七小对";
					}
					if (type == GameConstants.CHR_HUNAN_GANG_KAI) {
						des += " 杠上开花";
					}
					if (type == GameConstants.CHR_HUNAN_SHUANG_GANG_KAI) {
						des += " 双杠杠上开花";
					}
					// if (type == MJGameConstants.CHR_QIANG_GANG_HU) {
					// des += " 抢杠胡";
					// }
					if (type == GameConstants.CHR_HUNAN_GANG_SHANG_PAO) {
						des += " 杠上炮";
					}
					if (type == GameConstants.CHR_HUNAN_SHUANG_GANG_SHANG_PAO) {
						des += " 双杠上炮";
					}
					// if (type == MJGameConstants.CHR_QUAN_QIU_REN) {
					// des += " 全求人";
					// }
					if (type == GameConstants.CHR_HUNAN_TIAN_HU) {
						des += " 天胡";
					}
					if (type == GameConstants.CHR_HUNAN_DI_HU) {
						des += " 地胡";
					}
					if (type == GameConstants.CHR_HUNAN_MEN_QING) {
						des += " 门清";
					}
					if (type == GameConstants.CHR_ZI_MO) {
						if (dahu[i] == true) {
							des += " 大胡自摸";
						} else {
							des += " 小胡自摸";
						}
					}
					if (type == GameConstants.CHR_SHU_FAN) {
						if (dahu[i] == true) {
							des += " 大胡接炮";
						} else {
							des += " 小胡接炮";
						}
					}
					if (type == GameConstants.CHR_FANG_PAO) {
						des += " 放炮";
					}
					if (type == GameConstants.CHR_TONG_PAO) {
						des += " 通炮";
					}
				} else {
					if (type == GameConstants.CHR_FANG_PAO) {
						if (has_da_hu == true) {
							des += " 大胡放炮";
						} else {
							des += " 小胡放炮";
						}
					}
				}
			}

			if (GRR._player_niao_count[i] > 0) {
				des += " 中鸟X" + GRR._player_niao_count[i];
			}

			if (GRR._hu_result[i] == GameConstants.HU_RESULT_FANG_KAN_QUAN_BAO) {
				des += " 放坎全包";
			}

			GRR._result_des[i] = des;
		}
	}

	// public int refresh_bei_shu(){
	// GRR._bei_shu=1;
	// GRR._especial_txt="";
	// if(GRR._piao_lai_count>0){
	// boolean same_player = true;
	// int seat = GRR._piao_lai_seat[0];
	// for(int i = 0; i < GRR._piao_lai_count; i++){
	// GRR._bei_shu*=2;
	// if(seat!=GRR._piao_lai_seat[i]){
	// same_player = false;
	// }
	// }
	//
	// if(same_player == true && GRR._piao_lai_count==4){
	// GRR._bei_shu*=2;
	// return seat;
	// }
	// }
	// return MJGameConstants.INVALID_SEAT;
	// }

	// 株洲是否听牌
	public boolean is_zhuzhou_ting_card(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount) {
		int handcount = _logic.get_card_count_by_index(cards_index);
		if (handcount == 1) {
			for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
				if (cards_index[i] == 0) {
					continue;
				}
				int cbValue = _logic.get_card_value(_logic.switch_to_card_data(i));

				// 单牌统计
				if ((cbValue != 2) && (cbValue != 5) && (cbValue != 8)) {
					return false;
				}
			}
			return true;
		}

		// 复制数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();
		for (int i = 0; i < GameConstants.MAX_INDEX - 7; i++) {
			int cbCurrentCard = _logic.switch_to_card_data(i);
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_zhuzhou(cbCardIndexTemp, weaveItem, cbWeaveCount,
					cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO))
				return true;
		}
		return false;
	}

	/**
	 * 检查杠牌后是否换章
	 * 
	 * @param seat_index
	 * @param card
	 * @return
	 */
	public boolean check_gang_huan_zhang(int mj_type, int seat_index, int card) {
		// 不能换章，需要检测是否改变了听牌
		int gang_card_index = _logic.switch_to_card_index(card);
		int gang_card_count = GRR._cards_index[seat_index][gang_card_index];
		// 假如杠了
		GRR._cards_index[seat_index][gang_card_index] = 0;

		int hu_cards[] = new int[GameConstants.MAX_INDEX];

		// 检查听牌
		int hu_card_count = 0;
		if (mj_type == GameConstants.GAME_TYPE_HENAN_AY) {
			hu_card_count = get_ay_ting_card(seat_index, hu_cards, GRR._cards_index[seat_index],
					GRR._weave_items[seat_index], GRR._weave_count[seat_index]);
		} else if (mj_type == GameConstants.GAME_TYPE_HENAN_ZHOU_KOU) {
			hu_card_count = this.get_henan_ting_card_chu_feng_bao_ting(hu_cards, GRR._cards_index[seat_index],
					GRR._weave_items[seat_index], GRR._weave_count[seat_index], true);
		} else if (mj_type == GameConstants.GAME_TYPE_HENAN_PDS) {
			hu_card_count = get_henan_ting_card(hu_cards, GRR._cards_index[seat_index], GRR._weave_items[seat_index],
					GRR._weave_count[seat_index]);
		}

		// 还原手牌
		GRR._cards_index[seat_index][gang_card_index] = gang_card_count;

		if (hu_card_count != _playerStatus[seat_index]._hu_card_count) {
			return true;
		} else {
			for (int j = 0; j < hu_card_count; j++) {
				int real_card = get_real_card(_playerStatus[seat_index]._hu_cards[j]);
				if (real_card != get_real_card(hu_cards[j])) {
					return true;
				}
			}
		}

		return false;
	}

	public boolean check_gang_huan_zhang_xc(int seat_index, int card) {
		int gang_card_index = _logic.switch_to_card_index(card);
		int gang_card_count = GRR._cards_index[seat_index][gang_card_index];

		GRR._cards_index[seat_index][gang_card_index] = 0;

		int hu_cards[] = new int[GameConstants.MAX_INDEX];
		int hu_card_count = 0;

		xc_analyse_type = XC_ANALYSE_TING;

		hu_card_count = get_xc_ting_card(seat_index, hu_cards, GRR._cards_index[seat_index],
				GRR._weave_items[seat_index], GRR._weave_count[seat_index]);

		GRR._cards_index[seat_index][gang_card_index] = gang_card_count;

		return hu_card_count > 0 ? false : true;
	}

	// 是否听牌
	public boolean is_cs_ting_card(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		if (is_mj_type(GameConstants.GAME_TYPE_CS) || is_mj_type(GameConstants.GAME_TYPE_HALL_CHANG_SHA_MJ)
				|| is_mj_type(GameConstants.GAME_TYPE_THJ_CS)) {
			int handcount = _logic.get_card_count_by_index(cards_index);
			if (handcount == 1) {
				// 全求人
				return true;
			}
		}

		// 复制数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();
		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			chr.set_empty();
			int cbCurrentCard = _logic.switch_to_card_data(i);
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard,
					chr, GameConstants.HU_CARD_TYPE_ZIMO, seat_index))
				return true;
		}
		return false;
	}

	@Override
	public boolean is_zhuang_xian() {
		if ((GameConstants.GAME_TYPE_ZZ == _game_type_index) || is_mj_type(GameConstants.GAME_TYPE_HONG_ZHONG_MJ_TH)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_YUYANG_HONGZHON) || is_mj_type(GameConstants.GAME_TYPE_HZ)
				|| is_mj_type(GameConstants.GAME_TYPE_FLS_HZ_LX) || is_mj_type(GameConstants.GAME_TYPE_SHUANGGUI)
				|| is_mj_type(GameConstants.GAME_TYPE_ZHUZHOU) || is_mj_type(GameConstants.GAME_TYPE_HENAN_ZHUAN_ZHUAN)
				|| is_mj_type(GameConstants.GAME_TYPE_HENAN_ZHOU_KOU)
				|| is_mj_type(GameConstants.GAME_TYPE_HALL_ZHUAN_ZHUAN_MJ)
				|| is_mj_type(GameConstants.GAME_TYPE_HUNAN_SHANG_XIA_GUI)
				|| is_mj_type(GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_SAN_MEN_XIA)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_HONG_ZHONG_FEI)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_PING_XIANG_ZZ)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_ZHOU_KOU)) {
			return false;
		}
		return true;
	}

	@Override
	public int get_piao_lai_bei_shu(int seat_index, int target_seat) {
		int count = 1;
		for (int i = 0; i < GRR._piao_lai_count; i++) {
			if ((GRR._piao_lai_seat[i] == seat_index) || (GRR._piao_lai_seat[i] == target_seat)) {
				count *= 2;
			}
		}
		int hjh = this.hei_jia_hei();
		if ((seat_index == hjh) || (target_seat == hjh)) {
			count *= 2;
		}
		return count;

	}

	@Override
	public int hei_jia_hei() {
		int same_player = GameConstants.INVALID_SEAT;
		if (GRR._piao_lai_count == 4) {
			same_player = GRR._piao_lai_seat[0];
			for (int i = 0; i < GRR._piao_lai_count; i++) {
				if (GRR._piao_lai_seat[i] != same_player) {
					return GameConstants.INVALID_SEAT;
				}
			}
		}
		return same_player;

	}

	/**
	 * 取出实际牌数据
	 * 
	 * @param card
	 * @return
	 */
	@Override
	public int get_real_card(int card) {
		// 错误断言
		if (card > GameConstants.CARD_ESPECIAL_TYPE_GUI && card < GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_GUI;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI && card < GameConstants.CARD_ESPECIAL_TYPE_DING_GUI) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;

		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_DING_GUI
				&& card < GameConstants.CARD_ESPECIAL_TYPE_LAI_GEN) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_DING_GUI;

		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_LAI_GEN && card < GameConstants.CARD_ESPECIAL_TYPE_TING) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_LAI_GEN;

		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_TING && card < GameConstants.CARD_ESPECIAL_TYPE_BAO_TING) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_TING;

		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_BAO_TING && card < GameConstants.CARD_ESPECIAL_TYPE_HUN) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_BAO_TING;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_HUN && card < GameConstants.CARD_ESPECIAL_TYPE_CI) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_HUN;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_CI && card < GameConstants.CARD_ESPECIAL_TYPE_WANG_BA) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_CI;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_WANG_BA) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
		}

		return card;
	}

	/**
	 * 结束调度
	 * 
	 * @return
	 */
	@Override
	public boolean exe_finish(int reason) {
		this._end_reason = reason;
		if (_end_reason == GameConstants.Game_End_NORMAL || _end_reason == GameConstants.Game_End_DRAW
				|| _end_reason == GameConstants.Game_End_ROUND_OVER) {
			cost_dou = 0;
		}

		this.set_handler(this._handler_finish);
		this._handler_finish.exe(this);

		return true;
	}

	/**
	 * 
	 * @param seat_index
	 * @return
	 */
	public boolean exe_xiao_hu(int seat_index) {
		if (_handler_xiao_hu != null) {
			// 有小胡
			this.set_handler(this._handler_xiao_hu);
			this._handler_xiao_hu.reset_status(seat_index);
			this._handler_xiao_hu.exe(this);
		} else if (_handler_xiao_hu_cslx != null) {
			this.set_handler(this._handler_xiao_hu_cslx);
			this._handler_xiao_hu_cslx.reset_status(seat_index);
			this._handler_xiao_hu_cslx.exe(this);
		}

		return true;
	}

	public boolean exe_select_magic_card(int seat_index) {
		this.set_handler(this._handler_select_magic);
		this._handler_select_magic.reset_status(seat_index);
		this._handler_select_magic.exe(this);
		return true;
	}

	public boolean exe_qi_shou_hu(int seat_index) {
		this.set_handler(this._handler_qi_shou_hu);
		this._handler_qi_shou_hu.reset_status(seat_index);
		this._handler_qi_shou_hu.exe(this);
		return true;
	}

	public boolean exe_qishou_hongzhong(int seat_index) {
		this.set_handler(this._handler_qishou_hongzhong);
		this._handler_qishou_hongzhong.reset_status(seat_index);
		this._handler_qishou_hongzhong.exe(this);

		return true;
	}

	public boolean exe_qishou_hun(int seat_index) {
		this.set_handler(this._handler_qishou_hun);
		this._handler_qishou_hun.reset_status(seat_index);
		this._handler_qishou_hun.exe(this);
		return true;
	}

	public boolean exe_pi_ci(int seat_index) {
		this.set_handler(this._handler_pi_ci);
		this._handler_pi_ci.reset_status(seat_index);
		this._handler_pi_ci.exe(this);
		return true;
	}

	/***
	 * 
	 * @param seat_index
	 * @param card_count
	 * @param card_data
	 * @param send_client
	 * @param delay
	 * @return
	 */
	@Override
	public boolean exe_add_discard(int seat_index, int card_count, int card_data[], boolean send_client, int delay) {
		if (delay == 0) {
			this.runnable_add_discard(seat_index, card_count, card_data, send_client);
			return true;
		}
		GameSchedule.put(
				new AddDiscardRunnable(getRoom_id(), seat_index, card_count, card_data, send_client, getMaxCount()),
				delay, TimeUnit.MILLISECONDS);

		return true;

	}

	/**
	 * //执行发牌 是否延迟
	 * 
	 * @param seat_index
	 * @param delay
	 * @return
	 */
	public boolean exe_dispatch_last_card(int seat_index, int type, int delay_time) {

		if (delay_time > 0) {
			GameSchedule.put(new DispatchLastCardRunnable(this.getRoom_id(), seat_index, type, false), delay_time,
					TimeUnit.MILLISECONDS);// MJGameConstants.GANG_LAST_CARD_DELAY
		} else {
			// 发牌
			if (_handler_dispath_last_card != null) {
				this.set_handler(this._handler_dispath_last_card);
				this._handler_dispath_last_card.reset_status(seat_index, type);
				this._handler.exe(this);
			} else if (_handlerLastCard_lxcg != null) {
				// 发牌
				this.set_handler(this._handlerLastCard_lxcg);
				this._handlerLastCard_lxcg.reset_status(seat_index, type);
				this._handler.exe(this);
			}
		}

		return true;
	}

	/**
	 * 显示中间出的牌
	 * 
	 * @param seat_index
	 */
	public void runnable_remove_hun_middle_cards(int seat_index) {
		// 牌局未开 或者等待状态 调度不需要执行 add by zain 2017/6/1
		if ((_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) && is_sys())
			return;
		// add end

		// 去掉
		this.operate_show_card(seat_index, GameConstants.Show_Card_Center, 0, null, GameConstants.INVALID_SEAT);

		// 刷新有癞子的牌
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			boolean has_lai_zi = false;
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				if (GRR._cards_index[i][j] > 0 && _logic.is_magic_index(j)) {
					has_lai_zi = true;
					break;
				}
			}
			if (has_lai_zi) {
				// 刷新自己手牌
				int cards[] = new int[GameConstants.MAX_COUNT];
				int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], cards);
				for (int j = 0; j < hand_card_count; j++) {
					if (_logic.is_magic_card(cards[j])) {
						cards[j] += GameConstants.CARD_ESPECIAL_TYPE_HUN;
					}
				}
				this.operate_player_cards(i, hand_card_count, cards, 0, null);
			}
		}

		// 检测听牌
		if (is_mj_type(GameConstants.GAME_TYPE_HENAN_PDS) || is_mj_type(GameConstants.GAME_TYPE_NEW_PING_DING_SHAN)) {
			if (!GameDescUtil.has_rule(getGameRuleIndexEx(), GameConstants.GAME_RULE_HENAN_CHU_FENG_BAO_TING)) {
				// 检测听牌
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					this._playerStatus[i]._hu_card_count = this.get_henan_ting_card(this._playerStatus[i]._hu_cards,
							this.GRR._cards_index[i], this.GRR._weave_items[i], this.GRR._weave_count[i]);
					if (this._playerStatus[i]._hu_card_count > 0) {
						this.operate_chi_hu_cards(i, this._playerStatus[i]._hu_card_count,
								this._playerStatus[i]._hu_cards);
					}
				}
			}
		}else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_XX) || is_mj_type(GameConstants.GAME_TYPE_NEW_XIN_XIANG)) {
			// 检测听牌
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				this._playerStatus[i]._hu_card_count = this.get_hn_xin_xiang_ting_card_new(
						this._playerStatus[i]._hu_cards, this.GRR._cards_index[i], this.GRR._weave_items[i],
						this.GRR._weave_count[i]);
				if (this._playerStatus[i]._hu_card_count > 0) {
					this.operate_chi_hu_cards(i, this._playerStatus[i]._hu_card_count, this._playerStatus[i]._hu_cards);
				}
			}
		} else {
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				this._playerStatus[i]._hu_card_count = this.get_henan_ting_card(this._playerStatus[i]._hu_cards,
						this.GRR._cards_index[i], this.GRR._weave_items[i], this.GRR._weave_count[i]);
				if (this._playerStatus[i]._hu_card_count > 0) {
					this.operate_chi_hu_cards(i, this._playerStatus[i]._hu_card_count, this._playerStatus[i]._hu_cards);
				}
			}
		}

		boolean is_qishou_hu = false;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			// 起手4个红中
			if (_logic.get_magic_card_count() != 0) {
				if (this.GRR._cards_index[i][this._logic.get_magic_card_index(0)] == 4) {

					this._playerStatus[i].add_action(GameConstants.WIK_ZI_MO);
					this._playerStatus[i]
							.add_zi_mo(this._logic.switch_to_card_data(this._logic.get_magic_card_index(0)), i);
					this.GRR._chi_hu_rights[i].opr_or(GameConstants.CHR_ZI_MO);
					this.GRR._chi_hu_rights[i].opr_or(GameConstants.CHR_HENAN_QISHOU_HU);
					this.exe_qishou_hun(i);
					is_qishou_hu = true;
					break;
				}
			}
		}
		if (is_qishou_hu == false) {
			this.exe_dispatch_card(this._current_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);
		}

		//
		// this.exe_dispatch_card(seat_index,MJGameConstants.WIK_NULL, 0);
	}

	/**
	 * 
	 * @param seat_index
	 * @param provide_player
	 * @param center_card
	 * @param action
	 * @param type
	 *            //共杠还是明杠
	 * @param self
	 *            自己摸的
	 * @param d
	 *            双杠
	 * @return
	 */
	@Override
	public boolean exe_gang(int seat_index, int provide_player, int center_card, int action, int type, boolean self,
			boolean d) {
		// 是否有抢杠胡
		this.set_handler(this._handler_gang);
		this._handler_gang.reset_status(seat_index, provide_player, center_card, action, type, self, d);
		this._handler.exe(this);

		return true;
	}

	/**
	 * 长沙麻将杠牌处理
	 * 
	 * @param seat_index
	 * @param d
	 * @return
	 */
	public boolean exe_gang_cs(int seat_index, boolean d) {
		this.set_handler(this._handler_gang_cs);
		this._handler_gang_cs.reset_status(seat_index, d);
		this._handler_gang_cs.exe(this);
		return true;
	}

	public boolean exe_gang_thj_cs(int seat_index, boolean d) {
		this.set_handler(this._handler_gang_thj_cs);
		this._handler_gang_thj_cs.reset_status(seat_index, d);
		this._handler_gang_thj_cs.exe(this);
		return true;
	}

	/**
	 * lx长沙麻将杠牌处理
	 * 
	 * @param seat_index
	 * @param d
	 * @return
	 */
	public boolean exe_gang_cslx(int seat_index, boolean d) {
		this.set_handler(this._handler_gang_cslx);
		this._handler_gang_cslx.reset_status(seat_index, d);
		this._handler_gang_cslx.exe(this);
		return true;
	}

	/**
	 * 株洲麻将杠牌处理
	 * 
	 * @param seat_index
	 * @param d
	 * @return
	 */
	public boolean exe_gang_zhuzhu(int seat_index, boolean d) {
		this.set_handler(this._handler_gang_zhuzhou);
		this._handler_gang_zhuzhou.reset_status(seat_index, d);
		this._handler_gang_zhuzhou.exe(this);
		return true;
	}

	/**
	 * 平顶山麻将切换跑扣的exe
	 * 
	 * @return
	 */
	public boolean exe_kou() {
		this.set_handler(this._handler_kou_hennapds);
		this._handler_kou_hennapds.exe(this);
		return true;
	}

	/**
	 * 海底
	 * 
	 * @param seat_index
	 * @return
	 */
	@Override
	public boolean exe_hai_di(int start_index, int seat_index) {
		this.set_handler(this._handler_hai_di);
		this._handler_hai_di.reset_status(start_index, seat_index);
		this._handler_hai_di.exe(this);
		return true;
	}

	/**
	 * 天胡
	 * 
	 * @param seat_index
	 * @return
	 */
	public boolean exe_tian_hu(int seat_index) {
		this.set_handler(this._handler_tianhu);
		this._handler_tianhu.reset_status(seat_index);
		this._handler_tianhu.exe(this);
		return true;
	}

	/**
	 * 地胡
	 * 
	 * @param seat_index
	 * @return
	 */
	public boolean exe_di_hu(int seat_index, int card) {
		this.set_handler(this._handler_dihu);
		this._handler_dihu.reset_status(seat_index, card);
		this._handler_dihu.exe(this);
		return true;
	}

	/**
	 * 切换地胡handler
	 * 
	 * @return
	 */
	public boolean exe_di_hu(int seat_index) {
		this.set_handler(this._handler_dihu);
		this._handler_dihu.reset_status(seat_index);
		return true;
	}

	/**
	 * 要海底
	 * 
	 * @param seat_index
	 * @return
	 */
	@Override
	public boolean exe_yao_hai_di(int seat_index) {
		this.set_handler(this._handler_yao_hai_di);
		this._handler_yao_hai_di.reset_status(seat_index);
		this._handler_yao_hai_di.exe(this);
		return true;
	}

	/**
	 * @param seat_index
	 * @param card
	 * @return
	 */
	public boolean exe_hun(int seat_index) {
		// 出牌
		this.set_handler(this._handler_hun);
		this._handler_hun.reset_status(seat_index);
		this._handler_hun.exe(this);

		return true;
	}

	/**
	 * 选择次牌
	 * 
	 * @param seat_index
	 * @return
	 */
	public boolean exe_ci(int seat_index) {
		// 出牌
		this.set_handler(this._hangdler_ci_lygc);
		this._hangdler_ci_lygc.reset_status(seat_index);
		this._hangdler_ci_lygc.exe(this);
		return true;
	}

	@Override
	public boolean exe_out_card_bao_ting(int seat_index, int card, int type) {
		// 出牌
		this.set_handler(this._handler_out_card_bao_ting);
		this._handler_out_card_bao_ting.reset_status(seat_index, card, type);
		this._handler.exe(this);

		return true;
	}

	@Override
	public boolean exe_jian_pao_hu(int seat_index, int action, int card) {

		GameSchedule.put(new JianPaoHuRunnable(this.getRoom_id(), seat_index, action, card),
				GameConstants.DELAY_JIAN_PAO_HU, TimeUnit.MILLISECONDS);

		return true;
	}

	/**
	 * 调度 发最后4张牌
	 * 
	 * @param cur_player
	 * @param type
	 * @param tail
	 * @return
	 */
	@Override
	public boolean runnable_dispatch_last_card_data(int cur_player, int type, boolean tail) {
		// 牌局未开 或者等待状态 调度不需要执行 add by zain 2017/6/1
		if ((_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) && is_sys())
			return false;
		// add end

		if (_handler_dispath_last_card != null) {
			// 发牌
			this.set_handler(this._handler_dispath_last_card);
			this._handler_dispath_last_card.reset_status(cur_player, type);
			this._handler.exe(this);
		} else if (_handlerLastCard_lxcg != null) {
			// 发牌
			this.set_handler(this._handlerLastCard_lxcg);
			this._handlerLastCard_lxcg.reset_status(cur_player, type);
			this._handler.exe(this);
		}

		return true;
	}

	@Override
	public boolean runnable_chuli_first_card_data(int _seat_index, int _type, boolean _tail) {
		return true;
	}

	@Override
	public boolean runnable_dispatch_first_card_data(int _seat_index, int _type, boolean _tail) {
		return true;
	}

	@Override
	public boolean runnable_gang_card_data(int seat_index, int provide_player, int center_card, int action, int type,
			boolean depatch, boolean self, boolean d) {
		return true;

	}

	/**
	 * 调度,加入牌堆
	 **/
	@Override
	public void runnable_add_discard(int seat_index, int card_count, int card_data[], boolean send_client) {
		// 牌局未开 或者等待状态 调度不需要执行 add by zain 2017/6/1
		if ((_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) && is_sys())
			return;
		// add end

		if (seat_index == GameConstants.INVALID_SEAT) {
			return;
		}
		if (GRR == null)
			return;// 最后一张
		for (int i = 0; i < card_count; i++) {
			GRR._discard_count[seat_index]++;

			GRR._discard_cards[seat_index][GRR._discard_count[seat_index] - 1] = card_data[i];
		}
		if (send_client == true) {
			this.operate_add_discard(seat_index, card_count, card_data);
		}
	}

	/**
	 * 调度,小胡结束
	 **//*
		 * public void runnable_xiao_hu(int seat_index,boolean is_dispatch) { //
		 * 牌局未开 或者等待状态 调度不需要执行 add by zain 2017/6/1 if ((_game_status ==
		 * GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT)
		 * && is_sys()) return; // add end
		 * 
		 * boolean change_handler = true; int cards[] = new
		 * int[GameConstants.MAX_COUNT]; for (int i = 0; i <
		 * GameConstants.GAME_PLAYER; i++) { // 清除动作 //
		 * _playerStatus[i].clean_status(); change_player_status(i,
		 * GameConstants.INVALID_VALUE); if (GRR._start_hu_right[i].is_valid())
		 * { change_handler = false; // 刷新自己手牌 int hand_card_count =
		 * _logic.switch_to_cards_data(GRR._cards_index[i], cards);
		 * this.operate_player_cards(i, hand_card_count, cards, 0, null);
		 * 
		 * // 去掉 小胡排 this.operate_show_card(i, GameConstants.Show_Card_XiaoHU,
		 * 0, null, GameConstants.INVALID_SEAT); } }
		 * 
		 * _game_status = GameConstants.GS_MJ_PLAY;
		 * 
		 * if(is_dispatch){ this.exe_dispatch_card(seat_index,
		 * GameConstants.WIK_NULL, 0); }else{ //小胡操作过后切换handler
		 * if(change_handler){ this.set_handler(_handler_out_card_operate); }
		 * //利用发牌刷新牌 this.operate_player_get_card(seat_index, 0, new int[] {},
		 * GameConstants.INVALID_SEAT); // 变更玩家状态
		 * this.change_player_status(seat_index,
		 * GameConstants.Player_Status_OUT_CARD); this.operate_player_status();
		 * }
		 * 
		 * }
		 */

	/**
	 * 移除赖根
	 * 
	 * @param seat_index
	 */
	@Override
	public void runnable_finish_lai_gen(int seat_index) {
		// 去掉
		this.operate_show_card(seat_index, GameConstants.Show_Card_Center, 0, null, GameConstants.INVALID_SEAT);

		// 刷新有癞子的牌
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			boolean has_lai_zi = false;
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				if (GRR._cards_index[i][j] > 0 && _logic.is_magic_index(j)) {
					has_lai_zi = true;
					GRR.mo_lai_count[i] = GRR._cards_index[i][j];// 摸赖次数
					break;
				}
			}
			if (has_lai_zi) {
				// 刷新自己手牌
				int cards[] = new int[GameConstants.MAX_COUNT];
				int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], cards);
				for (int j = 0; j < hand_card_count; j++) {
					if (_logic.is_magic_card(cards[j])) {
						cards[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
					}
				}
				this.operate_player_cards(i, hand_card_count, cards, 0, null);
			}
		}

		this.exe_dispatch_card(seat_index, GameConstants.WIK_NULL, 0);
	}

	/**
	 * 显示中间出的牌
	 * 
	 * @param seat_index
	 */
	@Override
	public void runnable_remove_middle_cards(int seat_index) {
		// 牌局未开 或者等待状态 调度不需要执行 add by zain 2017/6/1
		if ((_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) && is_sys())
			return;
		// add end
		// 去掉
		this.operate_show_card(seat_index, GameConstants.Show_Card_Center, 0, null, GameConstants.INVALID_SEAT);

		// 刷新有癞子的牌
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			boolean has_lai_zi = false;
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				if (GRR._cards_index[i][j] > 0 && _logic.is_magic_index(j)) {
					has_lai_zi = true;
					break;
				}
			}
			if (has_lai_zi) {
				// 刷新自己手牌
				int cards[] = new int[GameConstants.MAX_COUNT];
				int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], cards);
				for (int j = 0; j < hand_card_count; j++) {
					if (_logic.is_magic_card(cards[j])) {
						cards[j] += GameConstants.CARD_ESPECIAL_TYPE_GUI;
					}
				}
				this.operate_player_cards(i, hand_card_count, cards, 0, null);
			}
		}

		if (is_mj_type(GameConstants.GAME_TYPE_HUNAN_SHANG_XIA_GUI)) {
			// 检测听牌
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				this._playerStatus[i]._hu_card_count = MahjongTingPaiUtil.getInstance().get_sg_ting_card(
						this._playerStatus[i]._hu_cards, this.GRR._cards_index[i], this.GRR._weave_items[i],
						this.GRR._weave_count[i], this);
				if (this._playerStatus[i]._hu_card_count > 0) {
					this.operate_chi_hu_cards(i, this._playerStatus[i]._hu_card_count, this._playerStatus[i]._hu_cards);
				}
			}
		}

		this.exe_dispatch_card(seat_index, GameConstants.WIK_NULL, 0);
	}

	/**
	 * <<<<<<< .mine ||||||| .r1554 移除次牌中间显示 刷新手牌钟的次牌
	 * 
	 * @param seat_index
	 */
	public void runnable_remove_ci_middle_cards(int seat_index) {

		// 牌局未开 或者等待状态 调度不需要执行 add by zain 2017/6/1
		if ((_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) && is_sys())
			return;
		// add end

		// 去掉
		this.operate_show_card(seat_index, GameConstants.Show_Card_Center, 0, null, GameConstants.INVALID_SEAT);

		// 刷新手牌钟的次牌
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			boolean has_ci = false;
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				if (GRR._cards_index[i][j] > 0 && _logic.is_ci_card(j)) {
					has_ci = true;
					break;
				}
			}

			if (has_ci) {
				// 刷新自己手牌
				int cards[] = new int[GameConstants.MAX_COUNT];
				int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], cards);
				for (int j = 0; j < hand_card_count; j++) {
					if (_logic.is_ci_card(_logic.switch_to_card_index(cards[j]))) {
						cards[j] += GameConstants.CARD_ESPECIAL_TYPE_CI;
					}
				}
				this.operate_player_cards(i, hand_card_count, cards, 0, null);
			}
		}

		// 检测听牌
		if (!has_rule(GameConstants.GAME_RULE_HENAN_YCI)) {// 硬次玩法不听牌
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				this._playerStatus[i]._hu_card_count = this.get_henan_ting_card_lygc(this._playerStatus[i]._hu_cards,
						this.GRR._cards_index[i], this.GRR._weave_items[i], this.GRR._weave_count[i]);
				if (this._playerStatus[i]._hu_card_count > 0) {
					this.operate_chi_hu_cards(i, this._playerStatus[i]._hu_card_count, this._playerStatus[i]._hu_cards);
				}
			}
		}

		// 起手皮次
		boolean is_qishou_hu = false;
		// 检测皮次规则
		if (has_rule(GameConstants.GAME_RULE_HENAN_PICI)) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				// 起手3个次牌
				is_qishou_hu = check_pi_ci(i);
				if (is_qishou_hu) {
					break;
				}
			}
		}

		if (is_qishou_hu == false) {
			this.exe_dispatch_card(this._current_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);
		}

		//
		// this.exe_dispatch_card(seat_index,MJGameConstants.WIK_NULL, 0);
	}

	/**
	 * 显示中间出的牌
	 */
	public void runnable_remove_hun_henan_xc_cards(int seat_index) {
		if ((_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) && is_sys())
			return;

		this.operate_show_card(seat_index, GameConstants.Show_Card_Center, 0, null, GameConstants.INVALID_SEAT);

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			boolean has_lai_zi = false;

			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				if (GRR._cards_index[i][j] > 0 && _logic.is_magic_index(j)) {
					has_lai_zi = true;
					break;
				}
			}

			if (has_lai_zi) {
				int cards[] = new int[GameConstants.MAX_COUNT];
				int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], cards);
				for (int j = 0; j < hand_card_count; j++) {
					if (_logic.is_magic_card(cards[j])) {
						cards[j] += GameConstants.CARD_ESPECIAL_TYPE_HUN;
					}
				}
				this.operate_player_cards(i, hand_card_count, cards, 0, null);
			}
		}

		if (getRuleValue(GameConstants.GAME_RULE_HENAN_BAO_TING) != 1) {
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				xc_analyse_type = XC_ANALYSE_TING;

				this._playerStatus[i]._hu_card_count = this.get_xc_ting_card(i, this._playerStatus[i]._hu_cards,
						this.GRR._cards_index[i], this.GRR._weave_items[i], this.GRR._weave_count[i]);
				if (this._playerStatus[i]._hu_card_count > 0) {
					this.operate_chi_hu_cards(i, this._playerStatus[i]._hu_card_count, this._playerStatus[i]._hu_cards);
				}
			}
		}

		this.exe_dispatch_card(this._current_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);
	}

	/**
	 * 检测玩家手牌的皮次
	 * 
	 * @param _seat_index
	 * @return
	 */
	public boolean check_pi_ci(int _seat_index) {
		if (this.GRR._cards_index[_seat_index][this._logic.get_ci_card_index()] >= 3) {
			if (this._playerStatus[_seat_index].has_zi_mo()) {
				this._playerStatus[_seat_index].clean_action(GameConstants.WIK_ZI_MO);
				this._playerStatus[_seat_index].clean_weave();
			}
			this._playerStatus[_seat_index].add_action(GameConstants.WIK_LYGC_PI_CI);
			this._playerStatus[_seat_index].add_lygc_ci(this._logic.switch_to_card_data(this._logic.get_ci_card_index())
					+ GameConstants.CARD_ESPECIAL_TYPE_CI, _seat_index, GameConstants.WIK_LYGC_PI_CI);

			this.exe_pi_ci(_seat_index);
			return true;
		}
		return false;
	}

	/**
	 * 
	 * @param seat_index
	 * @param type
	 */
	@Override
	public void runnable_remove_out_cards(int seat_index, int type) {
		// 牌局未开 或者等待状态 调度不需要执行 add by zain 2017/6/1
		if ((_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) && is_sys())
			return;
		// add end
		// 删掉出来的那张牌
		operate_out_card(seat_index, 0, null, type, GameConstants.INVALID_SEAT);
	}

	public void runnable_ding_gui(int seat_index) {
		this.set_handler(_handler_da_dian);
		_handler_da_dian.reset_status(seat_index);
		_handler_da_dian.exe(this);

	}

	public void runnable_hun(int seat_index) {
		this.set_handler(_handler_hun);
		_handler_hun.reset_status(seat_index);
		_handler_hun.exe(this);
	}

	/***
	 * 
	 * @param seat_index
	 */
	public void runnable_start_lai_gen(int seat_index) {
		this.set_handler(_handler_lai_gen);
		_handler_lai_gen.reset_status(seat_index);
		_handler_lai_gen.exe(this);

	}

	public static void main(String[] args) {
		// int cards[] = new int[1000];
		//
		// String d = cards.toString();
		//
		// byte[] dd = d.getBytes();
		//
		// System.out.println("ddddddddd"+dd.length);
		//
		// for(int i=0;i<100000;i++) {
		// int[] _repertory_card_zz = new int[MJGameConstants.CARD_COUNT_ZZ];
		// MJGameLogic logic = new MJGameLogic();
		// logic.random_card_data(_repertory_card_zz);
		// }
		for (int i = 0; i < 10; i++) {
			int s = (int) Math.pow(2, i);
			System.out.println("s==" + s);
		}

		// 测试骰子
		// Random random=new Random();//
		//
		// for(int i=0; i<1000; i++){
		//
		// int rand=random.nextInt(6)+1;//
		// int lSiceCount =
		// FvMask.make_long(FvMask.make_word(rand,rand),FvMask.make_word(rand,rand));
		// int f =
		// ((lSiceCount>>24)+(lSiceCount>>16)-1)%MJGameConstants.GAME_PLAYER;
		//
		// rand=random.nextInt(MJGameConstants.GAME_PLAYER);//
		// System.out.println("==庄家"+rand);
		// }

		/*
		 * int test[][]={{0,0,0,0},{1,0,0,0},{1,0,0,0},{1,0,0,0}}; int sum[] =
		 * new int[4]; for (int i = 0; i < 4; i++) { for (int j = 0; j < 4; j++)
		 * { sum[i]-=test[i][j]; sum[j] += test[i][j]; } }
		 * 
		 * MJGameLogic test_logic = new MJGameLogic();
		 * test_logic.set_magic_card_index(test_logic.switch_to_card_index(
		 * MJGameConstants.ZZ_MAGIC_CARD)); int cards[] = new int[] { 0x35,
		 * 0x35, 0x01, 0x01, 0x03, 0x04, 0x05, 0x11, 0x11, 0x15, 0x15,
		 * 
		 * 0x29, 0x29 , 0x29};
		 * 
		 * List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
		 * int cards_index[] = new int[MJGameConstants.MAX_INDEX];
		 * test_logic.switch_to_cards_index(cards, 0, 14, cards_index);
		 * WeaveItem weaveItem[] = new WeaveItem[1]; boolean bValue =
		 * test_logic.analyse_card(cards_index, weaveItem, 0, analyseItemArray);
		 * if (!bValue) { System.out.println("==玩法" +
		 * MJGameConstants.CHR_QI_XIAO_DUI); } //七小队
		 */
		// int cards[] = new int[] { 0x01, 0x01, 0x01, 0x01, 0x12, 0x12, 0x18,
		// 0x18, 0x23, 0x23, 0x26,
		//
		// 0x26, 0x29 };
		//
		// Integer nGenCount = 0;
		// int cards_index[] = new int[MJGameConstants.MAX_INDEX];
		// test_logic.switch_to_cards_index(cards, 0, 13, cards_index);
		// WeaveItem weaveItem[] = new WeaveItem[1];
		// if (test_logic.is_qi_xiao_dui(cards_index, weaveItem, 0, 0x29)!=0) {
		// if (nGenCount > 0) {
		// System.out.println("==玩法" + MJGameConstants.CHR_HAOHUA_QI_XIAO_DUI);
		// //chiHuRight.opr_or(MJGameConstants.CHR_HAOHUA_QI_XIAO_DUI);
		// } else {
		// //chiHuRight.opr_or(MJGameConstants.CHR_QI_XIAO_DUI);
		// System.out.println("==玩法" + MJGameConstants.CHR_QI_XIAO_DUI);
		// }
		// }
		//
		//
		// PlayerResult _player_result = new PlayerResult();
		//
		// for (int i = 0; i < MJGameConstants.GAME_PLAYER; i++) {
		// _player_result.win_order[i] = -1;
		//
		// }
		// _player_result.game_score[0] = 400;
		// _player_result.game_score[1] = 400;
		// _player_result.game_score[2] = 300;
		// _player_result.game_score[3] = 200;
		//
		// int win_idx = 0;
		// int max_score = 0;
		// for (int i = 0; i < MJGameConstants.GAME_PLAYER; i++) {
		// int winner = -1;
		// int s = -999999;
		// for (int j = 0; j < MJGameConstants.GAME_PLAYER; j++) {
		// if (_player_result.win_order[j] != -1) {
		// continue;
		// }
		// if (_player_result.game_score[j] > s) {
		// s = _player_result.game_score[j];
		// winner = j;
		// }
		// }
		// if(s>=max_score){
		// max_score = s;
		// }else{
		// win_idx++;
		// }
		//
		// if (winner != -1) {
		// _player_result.win_order[winner] = win_idx;
		// //win_idx++;
		// }
		// }

		// 测试玩法的
		// int rule_1 = FvMask.mask(MJGameConstants.GAME_TYPE_ZZ_ZIMOHU);
		// int rule_2 = FvMask.mask(MJGameConstants.GAME_TYPE_ZZ_ZHANIAO2);
		// int rule = rule_1 | rule_2;
		// boolean has = FvMask.has_any(rule, rule_2);
		// System.out.println("==玩法" + has);
		//
		// // 测试牌局
		// MJTable table = new MJTable();
		// int game_type_index = MJGameConstants.GAME_TYPE_CS;
		// int game_rule_index = rule_1 | rule_2;//
		// MJGameConstants.GAME_TYPE_ZZ_HONGZHONG
		// int game_round = 8;
		// table.init_table(game_type_index, game_rule_index, game_round);
		// boolean start = table.handler_game_start();
		//
		// for (int i = 0; i < MJGameConstants.MAX_INDEX; i++) {
		// if (table._cards_index[table._current_player][i] > 0) {
		// table.handler_player_out_card(table._current_player,
		// _logic.switch_to_card_data(table._cards_index[table._current_player][i]));
		// break;
		// }
	}

	/**
	 * 解决“玩家发起解散游戏之后，断线了，断线的这段时间，有人拒绝了房间解散或所有人都同意了，之后，玩家重连上了，那个倒计时的界面还在”。
	 * 
	 * @param seat_index
	 */
	@Override
	@Deprecated
	public void reconnect_when_release_room(int seat_index) {
		if (_release_scheduled == null) {
			int release_players[] = new int[getTablePlayerNumber()];
			Arrays.fill(release_players, 2);
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setReleaseTime(100);
			roomResponse.setType(MsgConstants.RESPONSE_PLAYER_RELEASE);
			roomResponse.setRequestPlayerSeat(seat_index);
			roomResponse.setOperateCode(1);
			roomResponse.setLeftTime(50);
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				roomResponse.addReleasePlayers(release_players[i]);
			}
			send_response_to_room(roomResponse);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.common.domain.Room#getPlayerNumber()
	 */
	@Override
	public int getTablePlayerNumber() {
		if (is_mj_type(GameConstants.GAME_TYPE_NEW_XU_CHANG)) {
			if (getRuleValue(GameConstants.GAME_RULE_HENAN_TWO_PLAYER) == 1) {
				return 2;
			}
			if (getRuleValue(GameConstants.GAME_RULE_HENAN_THREE) == 1) {
				return 3;
			}
			return 4;
		}
		if (is_mj_type(GameConstants.GAME_TYPE_FLS_CS_LX_DT) || is_mj_type(GameConstants.GAME_TYPE_XTHH)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_YUYANG_HONGZHON)|| is_mj_type(GameConstants.GAME_TYPE_HALL_ZHUAN_ZHUAN_MJ)) {
			if (playerNumber > 0) {
				return playerNumber;
			}
		}
		if (is_mj_type(GameConstants.GAME_TYPE_HALL_CHANG_SHA_MJ) || is_mj_type(GameConstants.GAME_TYPE_THJ_CS)) {
			if (playerNumber > 0) {
				return playerNumber;
			}
			if (has_rule(GameConstants.GAME_RULE_HUNAN_THREE)) {
				return 3;
			}
			return 4;
		}
		if (has_rule(GameConstants.GAME_RULE_HUNAN_THREE)) {
			if (this._game_type_index == GameConstants.GAME_TYPE_FLS_CS_LX
					|| this._game_type_index == GameConstants.GAME_TYPE_HENAN
					|| this._game_type_index == GameConstants.GAME_TYPE_HENAN_ZMD
					|| this._game_type_index == GameConstants.GAME_TYPE_HENAN_KF
					|| this._game_type_index == GameConstants.GAME_TYPE_HENAN_NY
					|| this._game_type_index == GameConstants.GAME_TYPE_HENAN_LYGC
					|| this._game_type_index == GameConstants.GAME_TYPE_HENAN_XY
					|| this._game_type_index == GameConstants.GAME_TYPE_HENAN_XX
					|| this._game_type_index == GameConstants.GAME_TYPE_HENAN_PDS
					|| this._game_type_index == GameConstants.GAME_TYPE_HENAN_ZHUAN_ZHUAN
					|| this._game_type_index == GameConstants.GAME_TYPE_ZZ
					|| this._game_type_index == GameConstants.GAME_TYPE_YUE_YANG_TDH
					|| this._game_type_index == GameConstants.GAME_TYPE_HZ
					|| this._game_type_index == GameConstants.GAME_TYPE_CS
					|| this._game_type_index == GameConstants.GAME_TYPE_FLS_HZ_LX
					|| is_mj_type(GameConstants.GAME_TYPE_HALL_ZHUAN_ZHUAN_MJ)
					|| is_mj_type(GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ)
					|| is_mj_type(GameConstants.GAME_TYPE_HALL_CHANG_SHA_MJ)
					|| is_mj_type(GameConstants.GAME_TYPE_FLS_CS_LX_DT)
					|| is_mj_type(GameConstants.GAME_TYPE_MJ_HONG_ZHONG_FEI)
					|| is_mj_type(GameConstants.GAME_TYPE_MJ_PING_XIANG_ZZ)
					|| is_mj_type(GameConstants.GAME_TYPE_NEW_HE_NAN)
					|| is_mj_type(GameConstants.GAME_TYPE_NEW_ZHU_MA_DIAN)
					|| is_mj_type(GameConstants.GAME_TYPE_NEW_KAI_FENG)
					|| is_mj_type(GameConstants.GAME_TYPE_NEW_NAN_YANG)
					|| is_mj_type(GameConstants.GAME_TYPE_NEW_SAN_MEN_XIA)
					|| is_mj_type(GameConstants.GAME_TYPE_NEW_XIN_XIANG)
					|| is_mj_type(GameConstants.GAME_TYPE_NEW_LUO_YANG_GANG_CI)
					|| is_mj_type(GameConstants.GAME_TYPE_NEW_PING_DING_SHAN)
					|| is_mj_type(GameConstants.GAME_TYPE_JIAO_ZUO_HUAN_HUAN)
					|| is_mj_type(GameConstants.GAME_TYPE_MJ_LYGC_DIAN_PAO)
					|| is_mj_type(GameConstants.GAME_TYPE_HONG_ZHONG_MJ_TH)
					|| is_mj_type(GameConstants.GAME_TYPE_MJ_HE_ZE)
					|| is_mj_type(GameConstants.GAME_TYPE_MJ_YUYANG_HONGZHON)) {
				return GameConstants.GAME_PLAYER - 1;
			}
		}

		if (is_mj_type(GameConstants.GAME_TYPE_HENAN_ZHOU_KOU) || is_mj_type(GameConstants.GAME_TYPE_NEW_ZHOU_KOU)) {
			if (has_rule(GameConstants.GAME_RULE_HENAN_THREE))
				return GameConstants.GAME_PLAYER - 1;
		}

		return GameConstants.GAME_PLAYER;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.common.domain.Room#handler_request_trustee(int, boolean)
	 */
	@Override
	public boolean handler_request_trustee(int get_seat_index, boolean isTrustee, int trustee_type) {
		if (_playerStatus == null || istrustee == null) {
			send_error_notify(get_seat_index, 2, "游戏未开始,无法托管!");
			return false;
		}

		// WalkerGeek 比赛场不受限制
		if (!is_sys() && !is_match() && !isCoinRoom() && !isClubMatch()) {// 麻将普通场
			// WalkerGeek 测试，正式删除
			log_error("房间类型不对,无法托管" + clubInfo.matchId);
			return false;
		}
		
		// 第一局游戏开始前 无法托管
		if (_cur_round == 0 ) {
//			if (_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) {
			send_error_notify(get_seat_index, 2, "游戏还未开始,无法托管!");
			return false;
		}

		istrustee[get_seat_index] = isTrustee;

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_IS_TRUSTEE);
		roomResponse.setOperatePlayer(get_seat_index);
		roomResponse.setIstrustee(isTrustee);
		this.send_response_to_room(roomResponse);

		if (GRR != null) {
			GRR.add_room_response(roomResponse);
		}

		if (is_match() || isCoinRoom() || isClubMatch()) {
			if(istrustee[get_seat_index]){
				if(this._current_player == get_seat_index &&_playerStatus[get_seat_index].get_status() == GameConstants.Player_Status_OUT_CARD){
					int card = MJAIGameLogic.get_card(this, get_seat_index);
					//金币场房间自己托管走智能AI
					if(isCoinRoom()){
						card = MJAIGameLogic.getOutCardTwo(this, get_seat_index);
					}
					if (card != 0) {
						this.handler_player_out_card(get_seat_index, card);
					}
				}else if(_playerStatus[get_seat_index].get_status() == GameConstants.Player_Status_OPR_CARD){
					//金币场房间自己托管走智能AI
					if(isCoinRoom()){
						MJAIGameLogic.AI_Operate_Card_ALL(this, get_seat_index);
					}else{
						MJAIGameLogic.AI_Operate_Card(this, get_seat_index);
					}
				}
			}
			
//			if (istrustee[get_seat_index] && this._current_player == get_seat_index) {
//				if (_playerStatus[get_seat_index].get_status() == GameConstants.Player_Status_OUT_CARD) {
//					int card = MJAIGameLogic.get_card(this, get_seat_index);
//					if (card != 0) {
//						this.handler_player_out_card(get_seat_index, card);
//					}
//				} else if (_playerStatus[get_seat_index].get_status() == GameConstants.Player_Status_OPR_CARD) {
//					MJAIGameLogic.AI_Operate_Card(this, get_seat_index);
//				}
//			} else if (istrustee[get_seat_index]) {
//				if (_playerStatus[get_seat_index].get_status() == GameConstants.Player_Status_OPR_CARD) {
//					MJAIGameLogic.AI_Operate_Card(this, get_seat_index);
//				}
//			}
		}

		if (_handler != null && isTrustee) {
			_handler.handler_be_set_trustee(this, get_seat_index);
		}
		return true;

	}

	@Override
	public int set_ding_niao_valid(int card_data, boolean val) {
		// 先把值还原
		if (val) {
			if (card_data > GameConstants.DING_NIAO_INVALID && card_data < GameConstants.DING_NIAO_VALID) {
				card_data -= GameConstants.DING_NIAO_INVALID;
			} else if (card_data > GameConstants.DING_NIAO_VALID) {
				card_data -= GameConstants.DING_NIAO_VALID;
			}
		} else {
			if (card_data > GameConstants.DING_NIAO_INVALID) {
				return card_data;
			}
		}

		if (val == true) {
			// 生效
			return (card_data < GameConstants.DING_NIAO_VALID ? card_data + GameConstants.DING_NIAO_VALID : card_data);
		} else {
			return (card_data < GameConstants.DING_NIAO_INVALID ? card_data + GameConstants.DING_NIAO_INVALID
					: card_data);
		}
	}

	@Override
	public int set_fei_niao_valid(int card_data, boolean val) {
		if (val) {
			if (card_data > GameConstants.FEI_NIAO_INVALID && card_data < GameConstants.FEI_NIAO_VALID) {
				card_data -= GameConstants.FEI_NIAO_INVALID;
			} else if (card_data > GameConstants.FEI_NIAO_VALID) {
				card_data -= GameConstants.FEI_NIAO_VALID;
			}
		} else {
			if (card_data > GameConstants.FEI_NIAO_INVALID) {
				return card_data;
			}
		}

		if (val == true) {
			// 生效
			return (card_data < GameConstants.FEI_NIAO_VALID ? card_data + GameConstants.FEI_NIAO_VALID : card_data);
		} else {
			return (card_data < GameConstants.FEI_NIAO_INVALID ? card_data + GameConstants.FEI_NIAO_INVALID
					: card_data);
		}

	}

	@Override
	public void load_out_card_ting(int seat_index, RoomResponse.Builder roomResponse,
			TableResponse.Builder tableResponse) {
		int ting_count = _playerStatus[seat_index]._hu_out_card_count;
		if (ting_count <= 0)
			return;

		int l = tableResponse.getCardsDataCount();
		for (int i = 0; i < l; i++) {
			for (int j = 0; j < ting_count; j++) {
				if (tableResponse.getCardsData(i) == _playerStatus[seat_index]._hu_out_card_ting[j]) {
					tableResponse.setCardsData(i,
							tableResponse.getCardsData(i) + GameConstants.CARD_ESPECIAL_TYPE_TING);
				}
			}
		}
		// 打出去可以听牌的个数
		roomResponse.setOutCardCount(ting_count);

		for (int i = 0; i < ting_count; i++) {
			int ting_card_cout = _playerStatus[seat_index]._hu_out_card_ting_count[i];
			roomResponse.addOutCardTingCount(ting_card_cout);
			if (_logic.is_magic_card(_playerStatus[seat_index]._hu_out_card_ting[i])) {
				roomResponse.addOutCardTing(
						_playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
			} else {
				roomResponse.addOutCardTing(
						_playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants.CARD_ESPECIAL_TYPE_TING);
			}

			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < ting_card_cout; j++) {
				int_array.addItem(_playerStatus[seat_index]._hu_out_cards[i][j]);
			}
			roomResponse.addOutCardTingCards(int_array);
		}
	}

	@Override
	public boolean handler_refresh_player_data(int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		this.load_player_info_data(roomResponse);
		this.send_response_to_player(seat_index, roomResponse);
		// this.send_response_to_room(roomResponse);
		return true;
	}

	@Override
	public boolean kickout_not_ready_player() {
		_kick_schedule = null;

		if (!is_sys())
			return false;

		if (_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) {
			if (getPlayerCount() != GameConstants.GAME_PLAYER || _player_ready == null) {
				return false;
			}

			// 检查是否所有人都未准备
			int not_ready_count = 0;
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				if (get_players()[i] != null && _player_ready[i] == 0) {// 未准备的玩家
					not_ready_count++;
				}
			}
			if (not_ready_count == GameConstants.GAME_PLAYER)// 所有人都未准备 不用踢
				return false;

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				Player rPlayer = get_players()[i];
				if (rPlayer != null && _player_ready[i] == 0) {// 未准备的玩家
					send_error_notify(i, 2, "您长时间未准备,被踢出房间!");

					RoomResponse.Builder quit_roomResponse = RoomResponse.newBuilder();
					quit_roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);// 直接拉出游戏
					send_response_to_player(i, quit_roomResponse);

					this.get_players()[i] = null;
					_player_ready[i] = 0;
					// _player_open_less[i] = 0;
					PlayerServiceImpl.getInstance().quitRoomId(this.getRoom_id(), rPlayer.getAccount_id());
				}
			}
			//
			if (getPlayerCount() == 0) {// 释放房间
				// 删除房间
				PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
			} else {
				// 刷新玩家
				RoomResponse.Builder refreshroomResponse = RoomResponse.newBuilder();
				refreshroomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);
				this.load_player_info_data(refreshroomResponse);
				send_response_to_room(refreshroomResponse);
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean open_card_timer() {
		return false;
	}

	@Override
	public boolean robot_banker_timer() {
		return false;
	}

	@Override
	public boolean ready_timer() {
		return false;
	}

	@Override
	public boolean add_jetton_timer() {
		return false;
	}

	/*
	 * public void change_handler(MJHandler dest_handler) { _handler =
	 * dest_handler; }
	 */
	@Override
	public void change_player_status(int seat_index, int st) {
		if (_trustee_schedule == null) {
			_trustee_schedule = new ScheduledFuture[GameConstants.GAME_PLAYER];
		}

		if (_trustee_schedule[seat_index] != null) {
			_trustee_schedule[seat_index].cancel(false);
			_trustee_schedule[seat_index] = null;
		}

		PlayerStatus curPlayerStatus = _playerStatus[seat_index];
		curPlayerStatus.set_status(st);// 操作状态

		if ((is_mj_type(GameConstants.GAME_TYPE_MJ_LYGC_DIAN_PAO)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_LUO_YANG_GANG_CI)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_HE_ZE) || is_mj_type(GameConstants.GAME_TYPE_NEW_XU_CHANG))
				&& ruleMap.containsKey(GameConstants.GAME_RULE_HENAN_KUAI_SU_CHANG)) {
			if (st == GameConstants.Player_Status_OPR_CARD || st == GameConstants.Player_Status_OUT_CARD
					|| st == GameConstants.Player_Status_OPR_OR_OUT_CARD) {
				//自建赛金币场比赛场的快速建房玩法由AI取代
				if(!isClubMatch() && !isCoinRoom() && !is_match()){
					_trustee_schedule[seat_index] = GameSchedule.put(new TuoGuanRunnable(getRoom_id(), seat_index),
							GameConstants.TRUSTEE_TIME_OUT_SECONDS, TimeUnit.SECONDS);
				}
			}
		} else {
			// 如果是出牌或者操作状态 需要倒计时托管&& !istrustee[seat_index]
			if (is_sys()
					&& (st == GameConstants.Player_Status_OPR_CARD || st == GameConstants.Player_Status_OUT_CARD)) {
				_trustee_schedule[seat_index] = GameSchedule.put(new TuoGuanRunnable(getRoom_id(), seat_index),
						GameConstants.TRUSTEE_TIME_OUT_SECONDS, TimeUnit.SECONDS);
			}
		}
	}

	@Override
	public void clear_score_in_gold_room() {
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		int score = 0;

		// 门槛判断
		SysParamModel sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(getGame_id())
				.get(5006);
		int beilv = sysParamModel.getVal2().intValue();// 倍率
		float[] scores = _player_result.getGame_score();
		// GameConstants.GAME_PLAYER
		for (int i = 0; i < scores.length; i++) {
			if (this.get_players()[i] == null) {
				continue;
			}
			score = (int) (scores[i] * beilv);
			// 逻辑处理
			this.get_players()[i].setMoney(this.get_players()[i].getMoney() + score);
			StringBuilder buf = new StringBuilder();
			buf.append("牌局消耗:" + scores[i]).append("game_id:" + getGame_id());
			AddMoneyResultModel addGoldResultModel = centerRMIServer.addAccountMoney(
					this.get_players()[i].getAccount_id(), score, false, buf.toString(), EMoneyOperateType.ROOM_COST);
			if (addGoldResultModel.isSuccess() == false) {
				// 扣费失败
				logger.error("玩家:" + this.get_players()[i].getAccount_id() + ",扣费:" + score + "失败");
			}
			// 结算后 清0
			scores[i] = 0;
		}
		// playerNumber = 0;
	}

	@Override
	public boolean handler_refresh_all_player_data() {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		this.load_player_info_data(roomResponse);
		this.send_response_to_room(roomResponse);
		return true;
	}

	@Override
	public boolean handler_requst_message_deal(Player player, int seat_index, RoomRequest room_rq, int type) {
		return true;
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		return false;
	}
	
	public int get_bei_lv() {
		int bei_lv = 1;
		if (ruleMap.containsKey(GameConstants.GAME_RULE_BEI_LV)) {
			bei_lv = ruleMap.get(GameConstants.GAME_RULE_BEI_LV);
		}
		return bei_lv;
	}

	/**
	 * 同步操作时间
	 */
	public void sendLeftTime() {
		if (is_match() ||isCoinRoom() || isClubMatch()) {
			RoomResponse.Builder response = RoomResponse.newBuilder();
			response.setType(MsgConstants.RESPONSE_REFRESH_DELAY_TIME);
			response.setLeftTime(getPlay_card_time());
			send_response_to_room(response);
		}
	}

}
