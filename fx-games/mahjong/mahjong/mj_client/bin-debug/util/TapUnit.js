var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var TapUnit = (function () {
    function TapUnit() {
    }
    TapUnit.prototype.bind = function (dis) {
        this.dis = dis;
        this.w = dis.width;
        this.h = dis.height;
        dis.touchEnabled = true;
        dis.addEventListener(egret.TouchEvent.TOUCH_BEGIN, this.bigerHandler, this);
        dis.addEventListener(egret.TouchEvent.TOUCH_END, this.backHandler, this);
    };
    TapUnit.prototype.bigerHandler = function (evt) {
        this.dis.anchorOffsetX = 6;
        this.dis.anchorOffsetY = 6;
        this.dis.width = this.w + 12;
        this.dis.height = this.h + 12;
    };
    TapUnit.prototype.backHandler = function (evt) {
        this.dis.anchorOffsetX = 0;
        this.dis.anchorOffsetY = 0;
        this.dis.width = this.w;
        this.dis.height = this.h;
    };
    return TapUnit;
}());
__reflect(TapUnit.prototype, "TapUnit");
//# sourceMappingURL=TapUnit.js.map