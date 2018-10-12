var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var TabItem = (function () {
    function TabItem(skin, n) {
        if (n === void 0) { n = null; }
        throw new Error('并没有用到的类');
        /*this.tb = skin;
        this.bm = new egret.Bitmap();
        this.tb.addChild(this.bm);
        this.name = n;*/
    }
    /**
     * t1默认皮肤
     * t2选中皮肤
     */
    TabItem.prototype.addIcon = function (t1, t2) {
        if (t2 === void 0) { t2 = null; }
        this.tex1 = t1;
        this.tex2 = t2;
        this.bm.texture = t1;
        this.bm.x = (this.tb.width - this.bm.width) >> 1;
        this.bm.y = (this.tb.height - this.bm.height) >> 1;
    };
    Object.defineProperty(TabItem.prototype, "select", {
        get: function () {
            return this.tb.selected;
        },
        set: function (bol) {
            this.tb.selected = bol;
            if (this.tex2) {
                this.bm.texture = bol ? this.tex2 : this.tex1;
            }
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(TabItem.prototype, "icon", {
        set: function (tex) {
            this.tb.icon = tex;
        },
        enumerable: true,
        configurable: true
    });
    return TabItem;
}());
__reflect(TabItem.prototype, "TabItem");
//# sourceMappingURL=TabItem.js.map