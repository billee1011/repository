var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var GonggaoUI = (function (_super) {
    __extends(GonggaoUI, _super);
    function GonggaoUI() {
        var _this = _super.call(this, 'resource/UI_exml/Gonggao.exml') || this;
        _this.centerFlag = true;
        _this.isAloneShow = true;
        _this.closeOther = false;
        return _this;
    }
    GonggaoUI.prototype.uiLoadComplete = function () {
    };
    return GonggaoUI;
}(UIBase));
GonggaoUI.NAME = 'GonggaoUI';
__reflect(GonggaoUI.prototype, "GonggaoUI");
//# sourceMappingURL=GonggaoUI.js.map