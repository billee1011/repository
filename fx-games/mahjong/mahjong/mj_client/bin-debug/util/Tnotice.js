var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var Tnotice = (function () {
    function Tnotice() {
        this.timer = new egret.Timer(100, 0);
        this.timer.addEventListener(egret.TimerEvent.TIMER, this.startCheck, this);
    }
    Object.defineProperty(Tnotice, "instance", {
        get: function () {
            if (!this._instance) {
                this._instance = new Tnotice();
            }
            return this._instance;
        },
        enumerable: true,
        configurable: true
    });
    Tnotice.prototype.popUpTip = function (vv) {
        var vo = new QueenVO(vv);
        if (!this.last) {
            this.first = this.last = vo;
        }
        else {
            this.last.next = vo;
            vo.pre = this.last;
            this.last = vo;
        }
        if (this.first) {
            this.timer.start();
        }
    };
    Tnotice.prototype.startCheck = function (evt) {
        var vo = this.first;
        ToastManager.instance.showTips(vo.tips);
        this.first = vo.next;
        if (vo.next) {
            vo.next = null;
        }
        if (vo.pre) {
            vo.pre = null;
        }
        if (!this.first) {
            this.first = this.last = null;
            this.timer.stop();
        }
    };
    Tnotice.prototype.showMsg = function (msg) {
        TFacade.toggleUI(CommonSureUI.NAME, 1).execute('msg', msg);
    };
    Tnotice.prototype.showSureMsg = function (msg, backFun, thisObj) {
        TFacade.toggleUI(CommonSureUI.NAME, 1).execute('buy', [msg, backFun, thisObj]);
    };
    Tnotice.prototype.showSureNoCancelMsg = function (msg, backFun, thisObj) {
        TFacade.toggleUI(CommonSureUI.NAME, 1).execute('noCancel', [msg, backFun, thisObj]);
    };
    Tnotice.prototype.playGonggao = function (content, loop, mask) {
        if (loop === void 0) { loop = false; }
        if (mask === void 0) { mask = null; }
        this.hideGonggao();
        LayerManage.instance.playGonggao(content, loop, mask);
    };
    Tnotice.prototype.hideGonggao = function () {
        LayerManage.instance.hideGonggao();
    };
    return Tnotice;
}());
__reflect(Tnotice.prototype, "Tnotice");
//# sourceMappingURL=Tnotice.js.map