var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var FinalSettleVO = (function () {
    function FinalSettleVO() {
    }
    FinalSettleVO.prototype.decode = function (arr) {
        this.countList = [];
        for (var i = 0; i < arr.length; i++) {
            this.countList.push([i + 1, arr[i]]);
        }
    };
    return FinalSettleVO;
}());
__reflect(FinalSettleVO.prototype, "FinalSettleVO");
//# sourceMappingURL=FinalSettleVO.js.map