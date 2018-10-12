var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var ScrollBar = (function (_super) {
    __extends(ScrollBar, _super);
    function ScrollBar(list_target) {
        var _this = _super.call(this) || this;
        _this.viewport = list_target;
        _this.x = list_target.x;
        _this.y = list_target.y;
        _this.width = list_target.width + 5;
        _this.height = list_target.height;
        return _this;
    }
    return ScrollBar;
}(eui.Scroller));
__reflect(ScrollBar.prototype, "ScrollBar");
//# sourceMappingURL=ScrollBar.js.map