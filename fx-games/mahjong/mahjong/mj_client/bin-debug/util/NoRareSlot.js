var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var NoRareSlot = (function (_super) {
    __extends(NoRareSlot, _super);
    function NoRareSlot(w, h) {
        var _this = _super.call(this, w, h) || this;
        _this.tipable = true;
        return _this;
    }
    Object.defineProperty(NoRareSlot.prototype, "rarelevel", {
        set: function (v) {
        },
        enumerable: true,
        configurable: true
    });
    return NoRareSlot;
}(GoodsSlot));
__reflect(NoRareSlot.prototype, "NoRareSlot");
//# sourceMappingURL=NoRareSlot.js.map