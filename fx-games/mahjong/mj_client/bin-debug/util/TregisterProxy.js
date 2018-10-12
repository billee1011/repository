var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var TregisterProxy = (function () {
    function TregisterProxy() {
        this.facade = TFacade.facade;
        this.reg(MjModel.NAME, MjModel);
    }
    TregisterProxy.prototype.reg = function (name, proxy) {
        this.facade.registerProxy(name, proxy);
    };
    return TregisterProxy;
}());
__reflect(TregisterProxy.prototype, "TregisterProxy");
//# sourceMappingURL=TregisterProxy.js.map