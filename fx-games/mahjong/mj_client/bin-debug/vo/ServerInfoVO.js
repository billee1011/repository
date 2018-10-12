var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var ServerInfoVO = (function () {
    function ServerInfoVO() {
    }
    Object.defineProperty(ServerInfoVO.prototype, "serverTime", {
        get: function () {
            var temp = egret.getTimer() - this.lastTimer + this._serverTime;
            return temp;
        },
        set: function (value) {
            this.lastTimer = egret.getTimer();
            this._serverTime = value;
            this.serverDate = new Date(this._serverTime);
        },
        enumerable: true,
        configurable: true
    });
    return ServerInfoVO;
}());
__reflect(ServerInfoVO.prototype, "ServerInfoVO");
//# sourceMappingURL=ServerInfoVO.js.map