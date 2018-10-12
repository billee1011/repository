var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var ItemVO = (function () {
    function ItemVO() {
        this.guid = 0;
        this.id = 0;
        this.name = '';
        this.level = 0;
        this.job = 0;
        this.count = 0;
        /**
         * 性别  0：通用  1：男  2：女
         */
        this.gender = 0;
        this.tips = '';
        this.script = '';
        this.packages = '';
        /**
         * 物品类型
            0 普通物品
            1 消耗品
            2 货币类
            3 宝箱类道具
            25 装备
         */
        this.type = 0;
        /**
         * 道具使用打开的面板
         */
        this.open_ui = '';
        /**
         * type :( 25 装备， 此值为部位)
         */
        this.param1 = 0;
        /**
         * 品质
         */
        this.quality = 0;
        /**
         * 叠加数量
         */
        this.stack = 0;
        /**
         * icon编号
         */
        this.looks = 0;
        /**
         * 外观
         */
        this.avatar = 0;
        this.combat = 0;
        /**
         * 基础战力（不含强化等） 用于快速换装提示
         */
        this.baseCombat = 0;
        //-----------------runData---------
        this.slot = 0;
        /**
         * 是否允许批量使用 1:允许
         */
        this.batch_use = 0;
    }
    /**
     * guid,
        id,
        count,
        quality,
        qianghua:[],
        jinjie,
        shengxing,
        combat,
        baseCombat,
        slot
        ...
     */
    ItemVO.prototype.decodeRunData = function (data) {
        var i = 0;
        this.guid = data[i++];
        this.id = data[i++];
        this.count = data[i++];
        this.quality = data[i++];
        this.strongMsg = data[i++];
        this.jinjieMsg = data[i++];
        this.upStarMsg = data[i++];
        this.combat = data[i++];
        this.baseCombat = data[i++];
        this.slot = data[i++];
    };
    return ItemVO;
}());
__reflect(ItemVO.prototype, "ItemVO");
//# sourceMappingURL=ItemVO.js.map