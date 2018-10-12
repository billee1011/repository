var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var Proxy = (function (_super) {
    __extends(Proxy, _super);
    function Proxy() {
        var _this = _super.call(this) || this;
        _this.facade = TFacade.facade;
        return _this;
    }
    Object.defineProperty(Proxy.prototype, "socket", {
        get: function () {
            return GlobalDefine.socket;
        },
        enumerable: true,
        configurable: true
    });
    Proxy.prototype.simpleDispatcher = function (type, data) {
        if (data === void 0) { data = null; }
        this.facade.simpleDispatcher(type, data);
    };
    return Proxy;
}(egret.EventDispatcher));
__reflect(Proxy.prototype, "Proxy");
//# sourceMappingURL=Proxy.js.map