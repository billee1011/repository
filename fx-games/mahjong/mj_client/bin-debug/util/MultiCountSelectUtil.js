var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var MultiCountSelectUtil = (function () {
    function MultiCountSelectUtil(add, addTen, cut, cutTen, label) {
        this.min = 0;
        this.max = 0;
        this.btn_add = add;
        this.btn_addTen = addTen;
        this.btn_cut = cut;
        this.btn_cutTen = cutTen;
        this.label_count = label;
        this.opCount = 1;
        this.btn_add.addEventListener(egret.TouchEvent.TOUCH_TAP, this.clickHandler, this);
        this.btn_addTen.addEventListener(egret.TouchEvent.TOUCH_TAP, this.clickHandler, this);
        this.btn_cut.addEventListener(egret.TouchEvent.TOUCH_TAP, this.clickHandler, this);
        this.btn_cutTen.addEventListener(egret.TouchEvent.TOUCH_TAP, this.clickHandler, this);
    }
    MultiCountSelectUtil.prototype.setMinMax = function (min, max) {
        this.min = min;
        this.max = max;
        this.opCount = 1;
    };
    MultiCountSelectUtil.prototype.clickHandler = function (evt) {
        switch (evt.currentTarget) {
            case this.btn_add:
                this.opCount++;
                break;
            case this.btn_addTen:
                this.opCount += 10;
                break;
            case this.btn_cut:
                this.opCount--;
                break;
            case this.btn_cutTen:
                this.opCount -= 10;
                break;
            default:
                break;
        }
    };
    Object.defineProperty(MultiCountSelectUtil.prototype, "opCount", {
        get: function () {
            return this._opCount;
        },
        set: function (vv) {
            vv = Math.max(vv, this.min);
            vv = Math.min(vv, this.max);
            this._opCount = vv;
            this.label_count.text = this._opCount.toString();
        },
        enumerable: true,
        configurable: true
    });
    return MultiCountSelectUtil;
}());
__reflect(MultiCountSelectUtil.prototype, "MultiCountSelectUtil");
//# sourceMappingURL=MultiCountSelectUtil.js.map