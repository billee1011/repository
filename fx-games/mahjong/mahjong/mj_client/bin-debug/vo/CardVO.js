var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var CardVO = (function () {
    function CardVO() {
    }
    CardVO.prototype.decode = function (temp) {
        if (!temp)
            return;
        this.id = temp[0];
        this.style = temp[1];
        this.type = temp[2];
        this.used = temp[3];
    };
    return CardVO;
}());
__reflect(CardVO.prototype, "CardVO");
//# sourceMappingURL=CardVO.js.map