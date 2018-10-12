var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var QueenVO = (function () {
    function QueenVO(tip) {
        if (tip === void 0) { tip = null; }
        this.tips = tip;
    }
    return QueenVO;
}());
__reflect(QueenVO.prototype, "QueenVO");
//# sourceMappingURL=QueenVO.js.map