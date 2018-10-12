var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var OperateBar = (function () {
    function OperateBar(bar, head, backFun, thisObj) {
        this.headOffX = 0;
        this.bar = bar;
        this.rect = new egret.Rectangle(0, 0, 0, bar.height);
        this.len = this.bar.width;
        this.head = head;
        this.backFun = backFun;
        this.thisObj = thisObj;
        head.addEventListener(egret.TouchEvent.TOUCH_BEGIN, this.touchBegin, this);
    }
    OperateBar.prototype.touchBegin = function (evt) {
        GlobalDefine.stage.addEventListener(egret.TouchEvent.TOUCH_MOVE, this.touchMove, this);
        GlobalDefine.stage.addEventListener(egret.TouchEvent.TOUCH_END, this.touchEnd, this);
    };
    OperateBar.prototype.touchEnd = function (evt) {
        GlobalDefine.stage.removeEventListener(egret.TouchEvent.TOUCH_MOVE, this.touchMove, this);
        GlobalDefine.stage.removeEventListener(egret.TouchEvent.TOUCH_END, this.touchEnd, this);
    };
    OperateBar.prototype.touchMove = function (evt) {
        if (!this.parent)
            return;
        var h = this.head;
        var gx = this.parent.globalToLocal(evt.stageX, evt.stageY).x;
        h.x = gx - this.headOffX;
        if (h.x + this.headOffX < this.bar.x) {
            h.x = this.bar.x - this.headOffX;
        }
        if (h.x + this.headOffX > this.bar.x + this.len) {
            h.x = this.bar.x + this.len - this.headOffX;
        }
        var a = (this.head.x + this.headOffX - this.bar.x) / this.len;
        a = Math.abs(a);
        this.setData(a);
        if (this.backFun != null) {
            this.backFun.call(this.thisObj, CommonTool.getKeepNum(a, 1));
        }
    };
    OperateBar.prototype.setData = function (a) {
        this.rect.width = Math.floor(a * this.len);
        this.bar.scrollRect = this.rect;
        if (a == 1)
            this.head.x = this.bar.x + this.len - this.headOffX;
    };
    return OperateBar;
}());
__reflect(OperateBar.prototype, "OperateBar");
//# sourceMappingURL=OperateBar.js.map