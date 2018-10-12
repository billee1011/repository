class ConstDefine {
	/**
	 * 切换地图成功
	 */
	public static CHANGE_MAP_SUCC:string = 'CHANGE_MAP_SUCC';

	//public static CURRENCY_DEFINE:Array<string> = ['元宝','金币'];
	//public static CURRENCY_DEFINE_PRO:Array<string> = ['gold','money'];
	/**
	 * 角色属性信息变更
	 */
	public static ROLE_INFO_CHANGE:string = 'ROLE_INFO_CHANGE';
	/**
	 * 面板黑色背景点击触发
	 */
	public static EMPTY_MASK_CLICK:string = 'EMPTY_MASK_CLICK';

	public static SERVER_CONNECT_SUCC:string = 'SERVER_CONNECT_SUCC';
	/**
	 * 0 元宝
	1 金币
	2 精力
	3 威望
	4 勇气
	5 战功
	6 熔炼值
	 */
	public static CURRENCY_GOLD:number = 0;
	public static CURRENCY_MONEY:number = 1;
	/**精力 */
	public static CURRENCY_ENERGY:number = 2;
	/**威望 */
	public static CURRENCY_PRESTIGE:number = 3;
	/** 勇气*/
	public static CURRENCY_COURAGE:number = 4;
	/**战功*/
	public static CURRENCY_ZHANGONG:number = 5;
	/**熔炼值 */
	public static CURRENCY_MELTING:number = 6;

	public static SLOT_EQUIP:number = 0;
	public static SLOT_CLOTH:number = 5;

	public static type_floor:number =  0;
	public static type_shadow:number =  1;
	public static type_body:number = 2;
	public static type_hair:number = 3;
	public static type_weapon:number = 4;
	public static type_wing:number = 5;
	public static type_top:number = 6;

	public static open_ronglian:string = 'ronglian';
	public static open_qianghua:string = 'qianghua_zb';
	public static open_skillUp:string = 'jineng';
	//装备升星
	public static open_shengxing:string = 'shengxing_zb';
	public static open_yaoqianshu:string = 'tree';
	public static open_wingUpstar:string = 'chibang';
	public static open_mountUpstar:string = 'zuoqi';
	public static open_qiandao:string = 'qiandao';
	public static open_ziyuanBen:string = 'fb_zy';
	public static open_ronglianDuihuan:string = 'shop_ronglian';
	public static open_juqingBen:string = 'fb_jq';
	public static open_jinjie:string = 'jinjie';
	public static open_task:string = 'quest';
	public static open_xunbao:string = 'xunbao';

	
	/*public static fly_attack:number = 1;
	public static fly_hitpoint:number = 2;*/
	public constructor() {
	}

	/**
	 * -----hitType(伤害类型定义)---------
			0：基础伤害
			1：暴击伤害
			2：吸血
			3：反弹伤害
			4：闪避
	 */
	public static getFlyNumDes(type:number):string
	{
		switch (type) {
			case 0:
			return '伤害';
			case 1:
			return '暴击';
			case 2:
			return '生命';
			case 3:
			return '反弹伤害';
			case 4:
			return '闪避';
		}
	}
}