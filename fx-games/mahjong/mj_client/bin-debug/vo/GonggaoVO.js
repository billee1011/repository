var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var GonggaoVO = (function () {
    function GonggaoVO() {
    }
    GonggaoVO.prototype.decode = function (data) {
        this.id = data[0];
        this.content = data[1];
        this.interval = data[2];
        this.beginTime = data[3];
        this.endTime = data[4];
    };
    return GonggaoVO;
}());
__reflect(GonggaoVO.prototype, "GonggaoVO");
//# sourceMappingURL=GonggaoVO.js.map