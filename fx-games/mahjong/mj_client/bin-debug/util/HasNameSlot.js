var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var HasNameSlot = (function (_super) {
    __extends(HasNameSlot, _super);
    function HasNameSlot() {
        var _this = _super.call(this) || this;
        _this.tipable = true;
        return _this;
    }
    HasNameSlot.prototype.doData = function () {
        var vo = this.data;
        this.label = vo.name;
    };
    return HasNameSlot;
}(GoodsSlot));
__reflect(HasNameSlot.prototype, "HasNameSlot");
//# sourceMappingURL=HasNameSlot.js.map