package com.cai.common.constant;

public class GameConstants {

	//参数定义
	public static final int INVALID_VALUE	=0;						//无效椅子
	public static final int INVALID_SEAT	=-1;						//无效椅子
	public static final int BLACK_CARD	=-2;						//牌背
	
	//参数索引,正式收费的时候一定要检查
	public static final int SYS_GAME_TYPE_ZZ = 1201;	
	public static final int SYS_GAME_TYPE_CS=				1202;
	public static final int SYS_GAME_TYPE_HZ=				1203;
	public static final int SYS_GAME_TYPE_SHUANGGUI =		1204;//双鬼麻将
	public static final int SYS_GAME_TYPE_ZHUZHOU=			1205;//株洲
	public static final int SYS_GAME_TYPE_XTHH=			1206;//仙桃晃晃麻将
	
	public static final int SYS_GAME_TYPE_HENAN_AY=		1207;//河南安阳
	public static final int SYS_GAME_TYPE_HENAN_LZ=		1208;//河南安阳 -林州
	public static final int SYS_GAME_TYPE_HENAN=		1209;//河南麻将
	public static final int SYS_GAME_TYPE_FLS_LX=		1210;//临湘福禄寿
	public static final int SYS_GAME_TYPE_HENAN_HZ=		1211;//河南红中麻将
	
//////////////////////////////////////////////////////////////麻将地市类型
	public static final int GAME_ID_HUNAN = 1;	//湖南
	public static final int GAME_ID_HUBEI = 2; //湖北
	public static final int GAME_ID_HENAN = 3;//河南麻将
	public static final int GAME_ID_HENGYANG = 4;//衡阳麻将
	public static final int GAME_ID_GUANGDONG = 5;//广东麻将
	
	public static final int GAME_ID_FLS_LX = 5;//临湘福禄寿
	
	//////////////////////////////////////////////////////////////麻将类型
	public static final int GAME_TYPE_ZZ=				0;
	public static final int GAME_TYPE_CS=				1;
	public static final int GAME_TYPE_HZ=				2;
	public static final int GAME_TYPE_SHUANGGUI =		3;//双鬼麻将
	public static final int GAME_TYPE_ZHUZHOU=			4;//株洲
	
	public static final int GAME_TYPE_XTHH=			2001;//仙桃晃晃麻将
	
	public static final int GAME_TYPE_HENAN_AY=		3001;//河南安阳
	public static final int GAME_TYPE_HENAN_LZ=		3002;//河南安阳 -林州
	public static final int GAME_TYPE_HENAN=		3003;//河南麻将
	public static final int GAME_TYPE_HENAN_HZ=		3004;//河南红中
	
	///////////
	public static final int GAME_TYPE_SXG=		4001;//上下鬼麻将
	
	///////////////
	public static final int GAME_TYPE_FLS_LX=		5001;//临湘福禄寿
	
	/////////////////////
	public static final int GAME_TYPE_GD_SHAO_GUAN=		6001;//韶关麻将
	
	
	public static final int MAX_CHI_HU_TYPE = 10;
	
	public static final int  DELAY_SEND_CARD_DELAY = 0;
	//public static final int  DELAY_ADD_CARD_DELAY = 250;
	public static final int  DELAY_AUTO_OUT_CARD = 260;
	public static final int  DELAY_JIAN_PAO_HU = 260;
	
	public static final int  XIAO_HU_DELAY = 5;
	public static final int  GAME_FINISH_DELAY = 2;
	public static final int  GANG_CARD_CS_DELAY = 1200;
	public static final int  GANG_LAST_CARD_DELAY = 1000;
	
	//玩家的状态
	public static final int  Player_Status_NULL = 0;		//没有状态
	public static final int  Player_Status_OUT_CARD = 1;	//等待出牌
	public static final int  Player_Status_OPR_CARD = 2;	//等待操作牌
	public static final int  Player_Status_OPR_OUT_CARD = 3;
	public static final int  Player_Status_WAITTING_TURN = 4;
	public static final int  Player_Status_WAITTING_GANG_CARD = 5;//杠后等摸牌
	public static final int  Player_Status_GANG_CARD = 6;//杠后不能换章
	
	//游戏状态
	public static final int GAME_STATUS_FREE=			0;									//空闲状态
	public static final int GAME_STATUS_PLAY=			100;								//游戏状态
	public static final int GAME_STATUS_WAIT=			200;								//等待状态
	
	//游戏状态
	public static final int GS_MJ_FREE=				GAME_STATUS_FREE;					//空闲状态
	public static final int GS_MJ_PLAY=				GAME_STATUS_PLAY+1;					//游戏状态
	public static final int GS_MJ_XIAOHU=			GAME_STATUS_PLAY+2;					//小胡状态
	public static final int GS_MJ_PAO_QIANG=		GAME_STATUS_PLAY+3;					//跑呛状态
	public static final int GS_MJ_PAO=				GAME_STATUS_PLAY+4;					//跑状态
	public static final int GS_MJ_PIAO=				GAME_STATUS_PLAY+5;					//福禄寿飘状态
	
	public static final int GS_MJ_WAIT=				GAME_STATUS_WAIT;					//等待状态
	//public static final int GS_MJ_CS_GANG=				GAME_STATUS_PLAY+4;				//长沙麻将杠
	
	public static final int CELL_SCORE = 1;					//基础单位分数
	
	public static final int HZ_MAGIC_CARD = 0x35;					//红中癞子
	
	public static final int GAME_PLAYER	= 4;								//游戏人数
	
	
	public static final int MAX_ZI = 27;//34;//最大索引
	public static final int MAX_ZI_FENG = 34;//34;//最大索引
	public static final int MAX_INDEX = 42;//34;//最大索引
	public static final int MAX_COUNT=14;	//最大数目 手牌
	public static final int MAX_WEAVE =4;	//最大组合
	
	
	public static final int MAX_FLS_INDEX = 24;////最大索引(福禄寿)
	public static final int MAX_FLS_COUNT=20;	//最大数目 手牌(福禄寿)
	public static final int MIN_SCORE_FLS=11;				//福禄寿最低胡牌分数
	public static final int MAX_WEAVE_FLS =6;	//最大组合
	
	///////////////////////////////////////////////////////////////////////////麻将牌数量
	public static final int CARD_COUNT_HU_NAN = 108; //湖南麻将
	public static final int CARD_COUNT_HZ = 112; //红中麻将
	
	public static final int CARD_COUNT_DAI_FENG_AY = 137; //安阳带风麻将数量--多张花牌
	public static final int CARD_COUNT_BU_DAI_FENG_AY = 109; //安阳不带风麻将数量--多张花牌
	
	public static final int CARD_COUNT_DAI_FENG_LZ = 136; //安阳带风麻将数量
	public static final int CARD_COUNT_BU_DAI_FENG_LZ = 108; //安阳不带风麻将数量
	
	public static final int CARD_COUNT_DAI_FENG_HNHZ = 136; //河南红中带风麻将数量
	public static final int CARD_COUNT_BU_DAI_FENG_HNHZ = 112; //河南红中不带风麻将数量
	
	public static final int CARD_FENG_COUNT = 7; //麻将风牌数量
	public static final int CARD_HUA_COUNT = 8;	//麻将花牌数量
	
	public static final int MAX_CARD_COUNT = 168; //麻将最大数量
	
	public static final int CARD_COUNT_FLS_LX = 96; //临湘福禄寿
	
	
	public static final int MAX_RIGHT_COUNT=1;//最大权位DWORD个数	
	//数值掩码
	public static final int	LOGIC_MASK_COLOR =	0xF0;				//花色掩码
	public static final int	LOGIC_MASK_VALUE =	0x0F;				//数值掩码
	
	public static final int  MAX_NIAO_CARD = 10;								//最大中鸟数
	
	public static final int CARD_COUNT_LEFT_HUANGZHUANG =14;	//河南麻将剩余牌荒张
	
	/////////////////////////////////////////////////////////////////////////////////操作标示
	//动作标志
	public static final int WIK_NULL=					0x00;				//没有类型
	public static final int WIK_LEFT=					0x01;				//左吃类型
	public static final int WIK_CENTER=					0x02;				//中吃类型
	public static final int WIK_RIGHT=					0x04;				//右吃类型
	public static final int WIK_PENG=					0x08;				//碰牌类型
	public static final int WIK_GANG=					0x10;				//杠牌类型
	public static final int WIK_XIAO_HU=				0x20;//小胡			//吃牌类型
	public static final int WIK_CHI_HU=					0x40;				//吃胡类型
	public static final int WIK_ZI_MO=					0x80;				//自摸
	public static final int WIK_BU_ZHNAG=				0x100;				//补张
	public static final int WIK_YAO_HAI_DI=				0x200;				//要海底
	
	
	
	//笑
	public static final int WIK_MENG_XIAO=				0x400;				//闷笑
	public static final int WIK_DIAN_XIAO=				0x800;				//点笑
	public static final int WIK_HUI_TOU_XIAO=			0x1000;				//回头笑
	public static final int WIK_XIAO_CHAO_TIAN=			0x2000;				//小朝天
	public static final int WIK_DA_CHAO_TIAN=			0x4000;				//大朝天
	public static final int WIK_PIAO_LAI=				0x8000;				//飘赖
	
	//
	public static final int WIK_BAO_TING=				0x10000;				//报听
	public static final int WIK_BU_HUA=					0x20000;				//补花
	
	public static final int WIK_ZHAO=				0x40000;				//招-福禄寿
	
	
	public static final int  MASK_CHI_HU_RIGHT	=		0x0fffffff;
	
	
	
	
	public static final int ZHANIAO_0=		0;		//扎鸟2个
	public static final int ZHANIAO_1=		1;		//扎鸟1个
	public static final int ZHANIAO_2=		2;		//扎鸟2个
	public static final int ZHANIAO_4=		4;		//扎鸟4个
	public static final int ZHANIAO_6=		6;		//扎鸟6个
	
	public static final float DI_FEN_05=	0.5f;		//0.5
	public static final float DI_FEN_10=	1.f;		//1
	public static final float DI_FEN_20=	2.f;		//2
	
	//////////////////////////////////////////////////////////////////////////湖南的玩法，转转、红中、双鬼、株洲、长沙
	
	public static final int GAME_RULE_HUNAN_258=			0;		//只能258做将
	public static final int GAME_RULE_HUNAN_ZIMOHU=		1;		//只能自模胡
	public static final int GAME_RULE_HUNAN_QIDUI=			2;		//可胡七对
	public static final int GAME_RULE_HUNAN_QIANGGANGHU=	3;		//可炮胡
	public static final int GAME_RULE_HUNAN_ZHANIAO2=		4;		//扎鸟2个
	public static final int GAME_RULE_HUNAN_ZHANIAO4=		5;		//扎鸟4个
	public static final int GAME_RULE_HUNAN_ZHANIAO6=		6;		//扎鸟6个
	public static final int GAME_RULE_HUNAN_HONGZHONG=		7;		//红中玩法
	public static final int GAME_RULE_HUNAN_JIANPAOHU=		8;		//见炮胡
	public static final int GAME_RULE_HUNAN_ZHANIAO1=		9;		//扎鸟1个
	public static final int GAME_RULE_HUNAN_GANG_HU=	10;		//抢杠胡
	
	public static final int GAME_RULE_HUNAN_SCORE_ADD=		11;		//加法记分
	public static final int GAME_RULE_HUNAN_SCORE_MUTIP=		12;		//乘法记分

	
	
	/////////////////////////////////////////////////////////////////////////////湖北的玩法,晃晃
	public static final int GAME_RULE_HEBEI_DI_FEN_05=	0;		//0.5
	public static final int GAME_RULE_HEBEI_DI_FEN_10=	1;		//1
	public static final int GAME_RULE_HEBEI_DI_FEN_20=	2;		//2
	
	public static final int GAME_RULE_HEBEI_GAN_DENG_YAN	=	3;		//干瞪眼
	public static final int GAME_RULE_HEBEI_YI_LAI_DAO_DI=	4;		//一赖到底
	public static final int GAME_RULE_HEBEI_PIAO_LAI_YOU_JIANG=	5;		//飘赖有奖
	

	///////////////////////////////////////////////////////////////////////////////河南麻将玩法，河南、安阳、林州、
	public static final int GAME_RULE_HENAN_DAIFENG=	0;		//带风
	public static final int GAME_RULE_HENAN_XIAPAO=	1;		//庄家能不能下跑
	public static final int GAME_RULE_HENAN_DAI_HUN=	2;		//带混
	public static final int GAME_RULE_HENAN_HENAN_PAO_HU=3;  //可炮胡
	public static final int GAME_RULE_HENAN_DAI_PAO=4;  //带跑
	
	public static final int GAME_RULE_HENAN_GANG_PAO=5;  //杆跑
	public static final int GAME_RULE_HENAN_JIA_DI=6;  //庄家加底
	public static final int GAME_RULE_HENAN_GANGHUA_DOUBLE=7;  //杆上花加倍
	public static final int GAME_RULE_HENAN_QIDUI_DOUBLE=8;  //七对加倍
	public static final int GAME_RULE_HENAN_ZHANIAO2=		9;		//扎鸟2个
	public static final int GAME_RULE_HENAN_ZHANIAO4=		10;		//扎鸟4个
	public static final int GAME_RULE_HENAN_ZHANIAO6=		11;		//扎鸟6个
	public static final int GAME_RULE_HENAN_ZIMOHU=		12;		//只能自模胡，可泡胡

	
	///////////////////////////////////////////////////////////////////////////////临湘玩法，福禄寿
	public static final int GAME_RULE_LIXIANG_FLS_ZHUANG=	0;		//0-胡牌者庄  1-轮庄
	public static final int GAME_RULE_LIXIANG_FLS_PIAO=	1;		//1-飘(默认不飘 0 )
	public static final int GAME_RULE_LIXIANG_FLS_IP=	2;		//是否允许相同ip进入(默认允许0)
	
	
	
	
	///////////////////////////////////////////////////////////////////////////////////////广东麻将玩法
	

	//////////////////////////////////////////////////////////////////湖南麻将的胡牌定义////////
	//胡牌定义

	//胡牌
	public static final int CHK_NULL=					0x00;										//非胡类型
	public static final int CHK_CHI_HU=				0x01;											//胡类型
	/***
	 * 这两个公用
	 */
	public static final int CHR_ZI_MO=					0x01000000;									//自摸
	public static final int CHR_SHU_FAN=				0x02000000;									//素翻
	
	
	// 大胡
	public static final int CHR_HUNAN_PENGPENG_HU=			0x00000001;									//碰碰胡
	public static final int CHR_HUNAN_JIANGJIANG_HU=			0x00000002;									//将将胡
	public static final int CHR_HUNAN_QING_YI_SE=				0x00000004;									//清一色
	public static final int CHR_HUNAN_HAI_DI_LAO=				0x00000008;									//海底捞
	public static final int CHR_HUNAN_HAI_DI_PAO=				0x00000010;									//海底捞
	public static final int CHR_HUNAN_QI_XIAO_DUI=			0x00000020;									//七小对
	public static final int CHR_HUNAN_HAOHUA_QI_XIAO_DUI=		0x00000040;									//豪华七小对
	public static final int CHR_HUNAN_GANG_KAI=				0x00000080;									//杠上开花
	public static final int CHR_HUNAN_QIANG_GANG_HU=			0x00000100;									//抢杠胡
	public static final int CHR_HUNAN_GANG_SHANG_PAO=			0x00000200;									//杠上跑
	public static final int CHR_HUNAN_QUAN_QIU_REN=			0x00000400;									//全求人
	public static final int CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI=			0x00000800;						//双豪华七小对
	public static final int CHR_HUNAN_SHUANG_GANG_KAI=		0x00001000;									//双杠杠上开花
	public static final int CHR_HUNAN_SHUANG_GANG_SHANG_PAO=	0x00002000;									//双杠上跑
	

	//小胡
	public static final int CHR_HUNAN_XIAO_DA_SI_XI=			0x00004000;									//大四喜
	public static final int CHR_HUNAN_XIAO_BAN_BAN_HU=		0x00008000;									//板板胡
	public static final int CHR_HUNAN_XIAO_QUE_YI_SE=			0x00010000;									//缺一色
	public static final int CHR_HUNAN_XIAO_LIU_LIU_SHUN=		0x00020000;									//六六顺



	public static final int CHR_FANG_PAO=				0x00040000;									//放炮
	public static final int CHR_TONG_PAO=				0x00080000;									//通炮
	public static final int CHR_HU=						0x00100000;									//胡
	
	public static final int CHR_HUNAN_TIAN_HU	=				0x00200000;									//天胡
	public static final int CHR_HUNAN_DI_HU	=				0x00400000;									//地胡
	public static final int CHR_HUNAN_MEN_QING=				0x00800000;									//门清
	
	
	
	public static final int CHR_HUNAN_258_JIANG=				0x04000000;									//将胡有2/5/8将
	public static final int CHR_HUNAN_HZ_QISHOU_HU=			0x08000000;									//红中起手胡
	
	
	////////////////////////////湖北分支///////////////////////////////////////晃晃麻将的胡牌定义
	public static final int CHR_HUBEI_HEI_MO=				0x00000001;									//黑摸
	public static final int CHR_HUBEI_RUAN_MO=				0x00000002;									//软摸
	public static final int CHR_HUBEI_ZHUO_CHONG=			0x00000004;									//捉铳
	public static final int CHR_HUBEI_RE_CHONG=			0x00000008;									//黑摸
	public static final int CHR_HUBEI_QIANG_GANG_HU=		0x00000010;									//抢杠胡
	
	
	
	
	//////////////////////////////河南分支//////////////////////////////////////安阳麻将胡牌定义
	public static final int CHR_HENAN_DAN_DIAO=			0x00000001;									//单吊
	public static final int CHR_HENAN_KA_ZHANG=			0x00000002;									//卡张
	public static final int CHR_HENAN_BIAN_ZHANG=		0x00000004;									//边张
	public static final int CHR_HENAN_QIANG_GANG_HU=	0x00000008;									//抢杠胡
	public static final int CHR_HENAN_TIAN_HU=			0x00000010;									//天胡
	public static final int CHR_HENAN_QI_XIAO_DUI=		0x00000020;									//七小对
	public static final int CHR_HENAN_HH_QI_XIAO_DUI=	0x00000040;									//豪华七小对
	public static final int CHR_HENAN_GANG_KAI=			0x00000080;									//杠上开花
	public static final int CHR_HENAN_HZ_QISHOU_HU=		0x00000100;									//红中起手胡
	
	public static final int CHR_HENAN_QISHOU_HU=		0x08000000;									//4混胡
	
	
	//////////////////////////////福禄寿分支//////////////////////////////////////福禄寿胡牌定义
	public static final int CHR_FLS_MENQING=			0x00000001;									//福禄寿门清
	public static final int CHR_FLS_PENGPENGHU=			0x00000002;									//福禄寿碰碰胡
	public static final int CHR_FLS_GANGSHANGHUA=			0x00000004;									//福禄寿杠上花
	public static final int CHR_FLS_HAIDI=			0x00000008;									//福禄寿海底
	public static final int CHR_FLS_MANTIAN_FEI=			0x00000010;									//福禄寿满天飞
	public static final int CHR_FLS_GUNGUN=			0x00000020;									//福禄寿滚滚
	public static final int CHR_FLS_GANGSHANGPAO=			0x00000040;									//福禄寿杠上pao

	
	//////////////////////////////////////////////////////////////////广东麻将的胡牌定义////////
	public static final int CHR_GD_PENGPENGHU=			0x00000001;									//对对胡(碰碰胡)
	public static final int CHR_GD_HUN_YI_SE=			0x00000002;									//混一色
	public static final int CHR_GD_QING_YI_SE=			0x00000004;									//清一色
	public static final int CHR_GD_YAO_JIU=				0x00000008;									//幺九(带风)
	public static final int CHR_GD_QUAN_YAO_JIU=		0x00000010;									//全幺九
	public static final int CHR_GD_SHI_SAN_YAO=			0x00000020;									//十三幺
	public static final int CHR_GD_QUAN_FAN=			0x00000040;									//全番(全大炮)
	public static final int CHR_GD_QI_XIAO_DUI=			0x00000080;									//七小对
	public static final int CHR_GD_TIAN_HU=				0x00000100;									//天胡
	public static final int CHR_GD_DI_HU=				0x00000200;									//地胡
	
	
	
	
	//牌局结束
	//结束原因
	public static final int GER_NORMAL=					0x00;								//常规结束
	public static final int GER_DISMISS=				0x01;								//游戏解散
	public static final int GER_USER_LEAVE=				0x02;								//用户离开
	public static final int GER_NETWORK_ERROR=			0x03;								//网络错误
	
	
	//////////////////////////////////////////////////////////////////////////////////////
	//结束
	public static final int Game_End_NORMAL = 1;			//正常结束
	public static final int Game_End_DRAW = 2;					//流局
	public static final int Game_End_ROUND_OVER = 3;			//圈数用完了，结束
	public static final int Game_End_RELEASE_NO_BEGIN = 4;		//没开始就解散了
	public static final int Game_End_RELEASE_PLAY = 5;			//游戏过程中解散的
	public static final int Game_End_RELEASE_RESULT = 6;			//游戏过程中解散的
	public static final int Game_End_RELEASE_PLAY_TIME_OUT = 7;			//超时解散
	public static final int Game_End_RELEASE_WAIT_TIME_OUT = 8;			//超时解散
	public static final int Game_End_RELEASE_SYSTEM = 9;			//系统强制解散
	
//////////////////////////////////////////////////////////////////////////
//	public static final int HU_RESULT_NULL = 0;
//	public static final int HU_RESULT_ZIMO = 1;
//	public static final int HU_RESULT_FANGPAO = 2;
//	public static final int HU_RESULT_JIEPAO = 3;
	public static final int HU_RESULT_FANG_KAN_QUAN_BAO =4;
	////////////////////////////////////////////////////////////////////////////////////////
	//效果定义
	public static final int EFFECT_ACTION_TYPE_HU = 1;
	public static final int EFFECT_ACTION_TYPE_ACTION = 2;
	public static final int Effect_Action_Other = 3;
	
	
	///其他类型效果
	public static final int EFFECT_LAST_FOUR_CARD = 1;//最后4张牌
	
	public static final int Show_Card_HU=1;
	public static final int	Show_Card_XiaoHU=2;
	public static final int	Show_Card_Center=3;
	/////////////////////////////////////////////////////////////////////
	
	public static final int CS_GANG_DRAW_COUNT = 2; //
	
	public static final int ZHUZHOU_GANG_DRAW_COUNT = 1; //
	
	public static final int OUT_CARD_TYPE_MID = 0;
	public static final int OUT_CARD_TYPE_LEFT = 1;
	
	/////////////////////////////////////////////////////////////////////
	public static final int CARD_STATUS_NORMAL = 0;
	public static final int CARD_STATUS_CS_GANG = 1;
	public static final int CARD_STATUS_BAO_TING = 2; //报听
	
	/////////////////////////////////////////////////////////////////////
	public static final int HU_CARD_TYPE_ZIMO  =1 ;
	public static final int HU_CARD_TYPE_PAOHU  =2 ;
	public static final int HU_CARD_TYPE_QIANGGANG  =3 ;
	public static final int HU_CARD_TYPE_RE_CHONG  =4 ;
	public static final int HU_CARD_TYPE_TIAN_HU  =5 ;
	public static final int HU_CARD_TYPE_GANG_KAI  =6 ;//判断杠上开花
	
	////////////////////////////////////////////////////////////////////////
	public static final int GANG_TYPE_AN_GANG = 1;
	public static final int GANG_TYPE_JIE_GANG = 2;
	public static final int GANG_TYPE_ADD_GANG = 3;
	
	////////////////////////////////////////////////////////////////////////吃碰牌的类型
	public static final int CHI_PENG_TYPE_OUT_CARD = 0;
	public static final int CHI_PENG_TYPE_GANG = 1;
	public static final int CHI_PENG_TYPE_CHAO_TIAN = 2;
	
	////////////////////////////////////////////////////////////////////////下跑最大个数
	public static final int PAO_MAX_COUNT = 2;
	
	public static final int PAO_MAX_COUNT_HENAN= 3;
	
	public static final int PAO_MAX_COUNT_PIAO_FLS= 5;//福禄寿飘 最大5分
	
//	public static final int GANG_TYPE_MENG_XIAO = 4;
//	public static final int GANG_TYPE_DIAN_XIAO = 5;
//	public static final int GANG_TYPE_HUI_TOU_XIAO = 6;
//	public static final int GANG_TYPE_DA_CHAO_TIAN = 7;
//	public static final int GANG_TYPE_XIAO_CHAO_TIAN = 8;
//	扑克定义

//	0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,						//万子
//	0x11,0x12,0x13,0x14,0x15,0x16,0x17,0x18,0x19,						//索子
//	0x21,0x22,0x23,0x24,0x25,0x26,0x27,0x28,0x29,						//同子
//	0x31,0x32,0x33,0x34,0x35,0x36,0x37,									//番子,东南西北，中发白
//	0x38,0x39,0x3A,0x3B,0x3C,0x3D,0x3E,0x3F,							//花子

//////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////
	//静态变量

	//湖南麻将数据
	public static final int CARD_DATA_HU_NAN[] = new int[]
	{
		0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,						//万子
		0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,						//万子
		0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,						//万子
		0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,						//万子
		0x11,0x12,0x13,0x14,0x15,0x16,0x17,0x18,0x19,						//索子
		0x11,0x12,0x13,0x14,0x15,0x16,0x17,0x18,0x19,						//索子
		0x11,0x12,0x13,0x14,0x15,0x16,0x17,0x18,0x19,						//索子
		0x11,0x12,0x13,0x14,0x15,0x16,0x17,0x18,0x19,						//索子
		0x21,0x22,0x23,0x24,0x25,0x26,0x27,0x28,0x29,						//同子
		0x21,0x22,0x23,0x24,0x25,0x26,0x27,0x28,0x29,						//同子
		0x21,0x22,0x23,0x24,0x25,0x26,0x27,0x28,0x29,						//同子
		0x21,0x22,0x23,0x24,0x25,0x26,0x27,0x28,0x29,						//同子
	};


	//红中麻将数据
	public static final int CARD_DATA_HZ[] = new int[]
	{
		0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,						//万子
		0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,						//万子
		0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,						//万子
		0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,						//万子
		0x11,0x12,0x13,0x14,0x15,0x16,0x17,0x18,0x19,						//索子
		0x11,0x12,0x13,0x14,0x15,0x16,0x17,0x18,0x19,						//索子
		0x11,0x12,0x13,0x14,0x15,0x16,0x17,0x18,0x19,						//索子
		0x11,0x12,0x13,0x14,0x15,0x16,0x17,0x18,0x19,						//索子
		0x21,0x22,0x23,0x24,0x25,0x26,0x27,0x28,0x29,						//同子
		0x21,0x22,0x23,0x24,0x25,0x26,0x27,0x28,0x29,						//同子
		0x21,0x22,0x23,0x24,0x25,0x26,0x27,0x28,0x29,						//同子
		0x21,0x22,0x23,0x24,0x25,0x26,0x27,0x28,0x29,						//同子

		0x35,0x35,0x35,0x35,												//红中

	};
	
	//全部的麻将数据
	public static final int CARD_DATA_MAX[] = new int[]
	{
		0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,						//万子
		0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,						//万子
		0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,						//万子
		0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,						//万子
		0x11,0x12,0x13,0x14,0x15,0x16,0x17,0x18,0x19,						//索子
		0x11,0x12,0x13,0x14,0x15,0x16,0x17,0x18,0x19,						//索子
		0x11,0x12,0x13,0x14,0x15,0x16,0x17,0x18,0x19,						//索子
		0x11,0x12,0x13,0x14,0x15,0x16,0x17,0x18,0x19,						//索子
		0x21,0x22,0x23,0x24,0x25,0x26,0x27,0x28,0x29,						//同子
		0x21,0x22,0x23,0x24,0x25,0x26,0x27,0x28,0x29,						//同子
		0x21,0x22,0x23,0x24,0x25,0x26,0x27,0x28,0x29,						//同子
		0x21,0x22,0x23,0x24,0x25,0x26,0x27,0x28,0x29,						//同子

		0x31,0x31,0x31,0x31,												//东风
		0x32,0x32,0x32,0x32,												//西风
		0x33,0x33,0x33,0x33,												//南风
		0x34,0x34,0x34,0x34,												//北风
		0x35,0x35,0x35,0x35,												//红中
		0x36,0x36,0x36,0x36,												//绿发
		0x37,0x37,0x37,0x37,												//白板
		
		0x38,0x38,0x38,0x38,												//春
		0x39,0x39,0x39,0x39,												//夏
		0x41,0x41,0x41,0x41,												//秋
		0x42,0x42,0x42,0x42,												//冬
		0x43,0x43,0x43,0x43,												//梅
		0x44,0x44,0x44,0x44,												//兰
		0x45,0x45,0x45,0x45,												//竹
		0x46,0x46,0x46,0x46,												//菊
	};
	
	//安阳麻将数据
	public static final int CARD_DATA_DAI_FENG_AY[] = new int[]
	{
		0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,						//万子
		0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,						//万子
		0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,						//万子
		0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,						//万子
		0x11,0x12,0x13,0x14,0x15,0x16,0x17,0x18,0x19,						//索子
		0x11,0x12,0x13,0x14,0x15,0x16,0x17,0x18,0x19,						//索子
		0x11,0x12,0x13,0x14,0x15,0x16,0x17,0x18,0x19,						//索子
		0x11,0x12,0x13,0x14,0x15,0x16,0x17,0x18,0x19,						//索子
		0x21,0x22,0x23,0x24,0x25,0x26,0x27,0x28,0x29,						//同子
		0x21,0x22,0x23,0x24,0x25,0x26,0x27,0x28,0x29,						//同子
		0x21,0x22,0x23,0x24,0x25,0x26,0x27,0x28,0x29,						//同子
		0x21,0x22,0x23,0x24,0x25,0x26,0x27,0x28,0x29,						//同子

		0x31,0x31,0x31,0x31,												//东风
		0x32,0x32,0x32,0x32,												//西风
		0x33,0x33,0x33,0x33,												//南风
		0x34,0x34,0x34,0x34,												//北风
		0x35,0x35,0x35,0x35,												//红中
		0x36,0x36,0x36,0x36,												//绿发
		0x37,0x37,0x37,0x37,												//白板
		
		
		0x38,																//春
	};
	
	//安阳麻将数据 不带风
	public static final int CARD_DATA_BU_DAI_FENG_AY[] = new int[]
	{
		0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,						//万子
		0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,						//万子
		0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,						//万子
		0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,						//万子
		0x11,0x12,0x13,0x14,0x15,0x16,0x17,0x18,0x19,						//索子
		0x11,0x12,0x13,0x14,0x15,0x16,0x17,0x18,0x19,						//索子
		0x11,0x12,0x13,0x14,0x15,0x16,0x17,0x18,0x19,						//索子
		0x11,0x12,0x13,0x14,0x15,0x16,0x17,0x18,0x19,						//索子
		0x21,0x22,0x23,0x24,0x25,0x26,0x27,0x28,0x29,						//同子
		0x21,0x22,0x23,0x24,0x25,0x26,0x27,0x28,0x29,						//同子
		0x21,0x22,0x23,0x24,0x25,0x26,0x27,0x28,0x29,						//同子
		0x21,0x22,0x23,0x24,0x25,0x26,0x27,0x28,0x29,						//同子

//		0x31,0x31,0x31,0x31,												//东风
//		0x32,0x32,0x32,0x32,												//西风
//		0x33,0x33,0x33,0x33,												//南风
//		0x34,0x34,0x34,0x34,												//北风
//		0x35,0x35,0x35,0x35,												//红中
//		0x36,0x36,0x36,0x36,												//绿发
//		0x37,0x37,0x37,0x37,												//白板
		
		
		0x38,																//春
	};
	
	//林州麻将数据
	public static final int CARD_DATA_DAI_FENG_LZ[] = new int[]
	{
		0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,						//万子
		0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,						//万子
		0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,						//万子
		0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,						//万子
		0x11,0x12,0x13,0x14,0x15,0x16,0x17,0x18,0x19,						//索子
		0x11,0x12,0x13,0x14,0x15,0x16,0x17,0x18,0x19,						//索子
		0x11,0x12,0x13,0x14,0x15,0x16,0x17,0x18,0x19,						//索子
		0x11,0x12,0x13,0x14,0x15,0x16,0x17,0x18,0x19,						//索子
		0x21,0x22,0x23,0x24,0x25,0x26,0x27,0x28,0x29,						//同子
		0x21,0x22,0x23,0x24,0x25,0x26,0x27,0x28,0x29,						//同子
		0x21,0x22,0x23,0x24,0x25,0x26,0x27,0x28,0x29,						//同子
		0x21,0x22,0x23,0x24,0x25,0x26,0x27,0x28,0x29,						//同子

		0x31,0x31,0x31,0x31,												//东风
		0x32,0x32,0x32,0x32,												//西风
		0x33,0x33,0x33,0x33,												//南风
		0x34,0x34,0x34,0x34,												//北风
		0x35,0x35,0x35,0x35,												//红中
		0x36,0x36,0x36,0x36,												//绿发
		0x37,0x37,0x37,0x37,												//白板
	};
		
	//林州麻将数据 不带风
	public static final int CARD_DATA_BU_DAI_FENG_LZ[] = new int[]
	{
		0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,						//万子
		0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,						//万子
		0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,						//万子
		0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,						//万子
		0x11,0x12,0x13,0x14,0x15,0x16,0x17,0x18,0x19,						//索子
		0x11,0x12,0x13,0x14,0x15,0x16,0x17,0x18,0x19,						//索子
		0x11,0x12,0x13,0x14,0x15,0x16,0x17,0x18,0x19,						//索子
		0x11,0x12,0x13,0x14,0x15,0x16,0x17,0x18,0x19,						//索子
		0x21,0x22,0x23,0x24,0x25,0x26,0x27,0x28,0x29,						//同子
		0x21,0x22,0x23,0x24,0x25,0x26,0x27,0x28,0x29,						//同子
		0x21,0x22,0x23,0x24,0x25,0x26,0x27,0x28,0x29,						//同子
		0x21,0x22,0x23,0x24,0x25,0x26,0x27,0x28,0x29,						//同子
	};
	
	
	
	public static final int CARD_DATA_FLS_LX[] = new int[]
	{
		0x01,0x01,0x01,0x01,						//上
		0x02,0x02,0x02,0x02,						//大
		0x03,0x03,0x03,0x03,					    //人
		
		0x11,0x11,0x11,0x11,						//丘
		0x12,0x12,0x12,0x12,						//乙
		0x13,0x13,0x13,0x13,					    //己
		
		0x21,0x21,0x21,0x21,						//化
		0x22,0x22,0x22,0x22,						//三
		0x23,0x23,0x23,0x23,					    //千
		
		0x31,0x31,0x31,0x31,						//七
		0x32,0x32,0x32,0x32,						//十
		0x33,0x33,0x33,0x33,					    //土
		
		0x41,0x41,0x41,0x41,						//尔
		0x42,0x42,0x42,0x42,						//小
		0x43,0x43,0x43,0x43,					    //生
		
		0x51,0x51,0x51,0x51,						//八
		0x52,0x52,0x52,0x52,						//九
		0x53,0x53,0x53,0x53,					    //子
		
		0x61,0x61,0x61,0x61,						//佳
		0x62,0x62,0x62,0x62,						//作
		0x63,0x63,0x63,0x63,					    //亡
		
		0x71,0x71,0x71,0x71,						//福
		0x72,0x72,0x72,0x72,						//禄
		0x73,0x73,0x73,0x73,					    //寿
	};
		
		
	public static final int CARD_CAI_SHENG = 0x38;			//
	
	public static final int Release_Room_Type_SEND = 1;			//发起解散
	public static final int Release_Room_Type_AGREE=2;			//同意
	public static final int Release_Room_Type_DONT_AGREE=3;		//不同意
	public static final int Release_Room_Type_CANCEL=4;			//还没开始,房主解散房间
	public static final int Release_Room_Type_QUIT=5;				//还没开始,普通玩家退出房间
	
	
	
	public static final int CARD_ESPECIAL_TYPE_NIAO = 1000;
	public static final int CARD_ESPECIAL_TYPE_HU = 2000;
	
	public static final int CARD_ESPECIAL_TYPE_GUI = 3000;//鬼
	public static final int CARD_ESPECIAL_TYPE_LAI_ZI = 3200;//;//癞子
	
	public static final int CARD_ESPECIAL_TYPE_DING_GUI = 4000;//定鬼
	public static final int CARD_ESPECIAL_TYPE_LAI_GEN = 4200;//;//赖根
	
	public static final int CARD_ESPECIAL_TYPE_TING = 5000;//;//听牌标示
	public static final int CARD_ESPECIAL_TYPE_BAO_TING = 5200;//;//报听标示
	
	public static final int CARD_ESPECIAL_TYPE_HUN = 6000;//混牌
	
	
	//////////
	public static final int WEAVE_SHOW_DIRECT = 1000;//;//显示组合牌的 放坎人
	
	//特殊描述类型
	
	//特别显示的描述
	public static final int Especial_Txt_Type_NULL = 0;//无,不显示
	public static final int Especial_Txt_Type_PIAO_LAI = 1;//飘赖显示
	

	//牌局过程结算
	public static final int Score_Type_NULL = 0;//无,不显示
	public static final int Score_Type_PIAO_LAI = 1;//飘赖分数
	public static final int Score_Type_GANG = 2;//杠(笑)牌,分数
	
//	//发牌类型
	public static final int DispatchCard_Type_Noraml = 1;//普通发牌
	public static final int DispatchCard_Type_Tian_Hu = 2;//第一张牌
//	
//	//发牌类型
	public static final int OutCard_Type_Noraml = 1;//普通出牌
	public static final int OutCard_Type_Di_Hu = 2;
//	public static final int OutCard_Type_Ming_Gang2 = 3;//明杠出牌
}
