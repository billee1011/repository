var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var TregisterDecoder = (function () {
    function TregisterDecoder() {
        var facade = TFacade.facade;
        //new SceneDecoder().init(facade);
        new GameDecoder().init(facade);
        new FrameDecoder().init(facade);
    }
    return TregisterDecoder;
}());
__reflect(TregisterDecoder.prototype, "TregisterDecoder");
//# sourceMappingURL=TregisterDecoder.js.map