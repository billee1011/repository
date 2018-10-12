var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var EmailUI = (function (_super) {
    __extends(EmailUI, _super);
    function EmailUI() {
        var _this = _super.call(this, 'resource/UI_exml/Email.exml') || this;
        _this.isAloneShow = true;
        return _this;
    }
    EmailUI.prototype.uiLoadComplete = function () {
    };
    return EmailUI;
}(UIBase));
EmailUI.NAME = 'EmailUI';
__reflect(EmailUI.prototype, "EmailUI");
//# sourceMappingURL=EmailUI.js.map