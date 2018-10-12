var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var ToastManager = (function () {
    function ToastManager() {
        this.tipList = [];
        this.diY = GlobalDefine.stageH * .618 - 100;
        this._cont = LayerManage.instance.tipLayer;
    }
    Object.defineProperty(ToastManager, "instance", {
        get: function () {
            if (!this._instance) {
                this._instance = new ToastManager();
            }
            return this._instance;
        },
        enumerable: true,
        configurable: true
    });
    ToastManager.prototype.showTips = function (msg) {
        var toast = new Toast(msg);
        this._cont.addChild(toast);
        this.tipList.push(toast);
        toast.end = false;
        //egret.log('----pre----',toast.hashCode);
        egret.Tween.get(toast)
            .to({ y: toast.y - 100 }, 400 /*, egret.Ease.quintOut*/)
            .call(this.arriveDestination, this, [toast]);
    };
    ToastManager.prototype.arriveDestination = function (tt) {
        var _this = this;
        //	egret.log('----back----',tt.hashCode);
        tt.end = true;
        var temp = this.diY;
        var toast;
        for (var i = this.tipList.length - 1; i >= 0; i--) {
            toast = this.tipList[i];
            //egret.log('%%%',toast.hashCode,toast.end);
            if (!toast.end)
                continue;
            toast.y = temp;
            temp -= toast.height;
        }
        //egret.log('-----',tt.aimY);
        egret.Tween.get(tt)
            .to({ alpha: 0 }, 2200, egret.Ease.quintIn).call(function () {
            DisplayUtil.removeDisplay(tt);
            _this.tipList.shift();
        });
    };
    return ToastManager;
}());
__reflect(ToastManager.prototype, "ToastManager");
//# sourceMappingURL=ToastManager.js.map