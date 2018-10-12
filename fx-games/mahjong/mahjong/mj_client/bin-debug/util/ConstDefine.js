var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var ConstDefine = (function () {
    /*public static fly_attack:number = 1;
    public static fly_hitpoint:number = 2;*/
    function ConstDefine() {
    }
    /**
     * -----hitType(伤害类型定义)---------
            0：基础伤害
            1：暴击伤害
            2：吸血
            3：反弹伤害
            4：闪避
     */
    ConstDefine.getFlyNumDes = function (type) {
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
    };
    return ConstDefine;
}());
/**
 * 切换地图成功
 */
ConstDefine.CHANGE_MAP_SUCC = 'CHANGE_MAP_SUCC';
//public static CURRENCY_DEFINE:Array<string> = ['元宝','金币'];
//public static CURRENCY_DEFINE_PRO:Array<string> = ['gold','money'];
/**
 * 角色属性信息变更
 */
ConstDefine.ROLE_INFO_CHANGE = 'ROLE_INFO_CHANGE';
/**
 * 面板黑色背景点击触发
 */
ConstDefine.EMPTY_MASK_CLICK = 'EMPTY_MASK_CLICK';
ConstDefine.SERVER_CONNECT_SUCC = 'SERVER_CONNECT_SUCC';
/**
 * 0 元宝
1 金币
2 精力
3 威望
4 勇气
5 战功
6 熔炼值
 */
ConstDefine.CURRENCY_GOLD = 0;
ConstDefine.CURRENCY_MONEY = 1;
/**精力 */
ConstDefine.CURRENCY_ENERGY = 2;
/**威望 */
ConstDefine.CURRENCY_PRESTIGE = 3;
/** 勇气*/
ConstDefine.CURRENCY_COURAGE = 4;
/**战功*/
ConstDefine.CURRENCY_ZHANGONG = 5;
/**熔炼值 */
ConstDefine.CURRENCY_MELTING = 6;
ConstDefine.SLOT_EQUIP = 0;
ConstDefine.SLOT_CLOTH = 5;
ConstDefine.type_floor = 0;
ConstDefine.type_shadow = 1;
ConstDefine.type_body = 2;
ConstDefine.type_hair = 3;
ConstDefine.type_weapon = 4;
ConstDefine.type_wing = 5;
ConstDefine.type_top = 6;
ConstDefine.open_ronglian = 'ronglian';
ConstDefine.open_qianghua = 'qianghua_zb';
ConstDefine.open_skillUp = 'jineng';
//装备升星
ConstDefine.open_shengxing = 'shengxing_zb';
ConstDefine.open_yaoqianshu = 'tree';
ConstDefine.open_wingUpstar = 'chibang';
ConstDefine.open_mountUpstar = 'zuoqi';
ConstDefine.open_qiandao = 'qiandao';
ConstDefine.open_ziyuanBen = 'fb_zy';
ConstDefine.open_ronglianDuihuan = 'shop_ronglian';
ConstDefine.open_juqingBen = 'fb_jq';
ConstDefine.open_jinjie = 'jinjie';
ConstDefine.open_task = 'quest';
ConstDefine.open_xunbao = 'xunbao';
__reflect(ConstDefine.prototype, "ConstDefine");
//# sourceMappingURL=ConstDefine.js.map