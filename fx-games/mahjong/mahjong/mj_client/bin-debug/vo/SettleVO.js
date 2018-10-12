var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var SettleVO = (function () {
    function SettleVO() {
    }
    SettleVO.prototype.decode = function (arr) {
        this.roleId = arr[0];
        //this.totalScore = arr[1];
        //this.curGangScore = arr[2];
        this.curScore = arr[1];
        var vo;
        this.cardList = [];
        for (var key in arr[2]) {
            vo = new CardVO();
            vo.decode(arr[2][key]);
            this.cardList.push(vo);
        }
    };
    return SettleVO;
}());
__reflect(SettleVO.prototype, "SettleVO");
//# sourceMappingURL=SettleVO.js.map