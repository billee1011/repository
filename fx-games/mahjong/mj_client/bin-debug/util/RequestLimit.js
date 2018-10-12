var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var RequestLimit = (function () {
    function RequestLimit() {
        this.record = {};
    }
    Object.defineProperty(RequestLimit, "instance", {
        get: function () {
            if (!this._instance) {
                this._instance = new RequestLimit();
            }
            return this._instance;
        },
        enumerable: true,
        configurable: true
    });
    RequestLimit.prototype.check = function (fun, limitTime) {
        if (limitTime === void 0) { limitTime = 500; }
        if (!this.record[fun]) {
            this.record[fun] = egret.getTimer();
            return true;
        }
        else if (egret.getTimer() - this.record[fun] >= limitTime) {
            this.record[fun] = egret.getTimer();
            return true;
        }
        else {
            //this.record[fun] = egret.getTimer();
            return false;
        }
    };
    return RequestLimit;
}());
__reflect(RequestLimit.prototype, "RequestLimit");
//# sourceMappingURL=RequestLimit.js.map