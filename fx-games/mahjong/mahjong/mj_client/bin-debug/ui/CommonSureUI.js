var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var CommonSureUI = (function (_super) {
    __extends(CommonSureUI, _super);
    function CommonSureUI() {
        var _this = _super.call(this, 'resource/UI_exml/CommonSure.exml') || this;
        _this.centerFlag = true;
        _this.isAloneShow = true;
        _this.closeOther = false;
        return _this;
    }
    CommonSureUI.prototype.uiLoadComplete = function () {
        CommonTool.addStroke(this.label_title, 2, 0x888888);
        this.btn_back.addEventListener(egret.TouchEvent.TOUCH_TAP, this.hide, this);
        this.btn_cancel.addEventListener(egret.TouchEvent.TOUCH_TAP, this.hide, this);
    };
    CommonSureUI.prototype.doExecute = function () {
        if (this.paramstype == 'msg') {
            this.label_content.text = this.params;
            this.btn_cancel.visible = this.btn_sure.visible = false;
            this.btn_sure.removeEventListener(egret.TouchEvent.TOUCH_TAP, this.sureDoSomething, this);
        }
        else if (this.paramstype == 'buy') {
            var arr = this.params;
            this.label_content.text = arr[0];
            this.backFun = arr[1];
            this.backThisObj = arr[2];
            this.btn_cancel.visible = this.btn_sure.visible = true;
            this.btn_sure.x = 257;
            this.btn_sure.addEventListener(egret.TouchEvent.TOUCH_TAP, this.sureDoSomething, this);
        }
        else if (this.paramstype == 'noCancel') {
            var arr = this.params;
            this.label_content.text = arr[0];
            this.backFun = arr[1];
            this.backThisObj = arr[2];
            this.btn_cancel.visible = false;
            this.btn_sure.visible = true;
            this.btn_sure.x = 148;
            this.btn_sure.addEventListener(egret.TouchEvent.TOUCH_TAP, this.sureDoSomething, this);
        }
    };
    CommonSureUI.prototype.sureDoSomething = function (evt) {
        this.backFun.call(this.backThisObj);
        this.hide();
    };
    return CommonSureUI;
}(UIBase));
CommonSureUI.NAME = 'CommonSureUI';
__reflect(CommonSureUI.prototype, "CommonSureUI");
//# sourceMappingURL=CommonSureUI.js.map