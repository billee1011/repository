var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var SettingUI = (function (_super) {
    __extends(SettingUI, _super);
    function SettingUI() {
        var _this = _super.call(this, 'resource/UI_exml/Setting.exml') || this;
        _this._yinyueOn = true;
        _this._yinxiaoOn = true;
        _this.curyinyueV = 0;
        _this.curyinxiaoV = 0;
        _this.centerFlag = true;
        _this.isAloneShow = true;
        _this.closeOther = false;
        return _this;
    }
    SettingUI.prototype.bindEvent = function () {
    };
    SettingUI.prototype.uiLoadComplete = function () {
        this.backBar = new OperateBar(this.img_backTiao, this.img_backHead, this.soundBack, this);
        this.backBar.headOffX = 30;
        this.backBar.parent = this;
        this.backBar.setData(1);
        this.yinxiaoBar = new OperateBar(this.img_backTiao0, this.img_backHead0, this.yinxiaoBack, this);
        this.yinxiaoBar.headOffX = 30;
        this.yinxiaoBar.parent = this;
        this.yinxiaoBar.setData(1);
        this.img_yinyueBtn.source = this.getTexture('set_musicOn');
        this.img_yinxiaoBtn.source = this.getTexture('set_yinxiaoOn');
        this.img_yinyueBtn.addEventListener(egret.TouchEvent.TOUCH_TAP, this.yinyueTapHandler, this);
        this.img_yinxiaoBtn.addEventListener(egret.TouchEvent.TOUCH_TAP, this.yinxiaoTapHandler, this);
        this.btn_close.addEventListener(egret.TouchEvent.TOUCH_TAP, this.hide, this);
        //	this.setQuitEnabled(false);
    };
    SettingUI.prototype.setQuitEnabled = function (bol) {
        if (bol) {
            this.btn_exchange.enabled = true;
            this.btn_exchange.filters = null;
            this.img_quitZi.filters = null;
        }
        else {
            this.btn_exchange.enabled = false;
            this.btn_exchange.filters = [CommonTool.grayFilter];
            this.img_quitZi.filters = [CommonTool.grayFilter];
        }
    };
    SettingUI.prototype.yinyueTapHandler = function (evt) {
        this.yinyueOn = !this.yinyueOn;
    };
    SettingUI.prototype.yinxiaoTapHandler = function (evt) {
        this.yinxiaoOn = !this.yinxiaoOn;
    };
    Object.defineProperty(SettingUI.prototype, "yinyueOn", {
        get: function () {
            return this._yinyueOn;
        },
        set: function (v) {
            this._yinyueOn = v;
            if (v) {
                this.img_yinyueBtn.source = this.getTexture('set_musicOn');
                GlobalDefine.backSoundChannel.volume = this.curyinyueV;
            }
            else {
                this.img_yinyueBtn.source = this.getTexture('set_musicOff');
                GlobalDefine.backSoundChannel.volume = 0;
            }
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(SettingUI.prototype, "yinxiaoOn", {
        get: function () {
            return this._yinxiaoOn;
        },
        set: function (v) {
            this._yinxiaoOn = v;
            if (v) {
                this.img_yinxiaoBtn.source = this.getTexture('set_yinxiaoOn');
                GlobalDefine.playSoundVolume = this.curyinxiaoV;
            }
            else {
                this.img_yinxiaoBtn.source = this.getTexture('set_yinxiaoOff');
                GlobalDefine.playSoundVolume = 0;
            }
        },
        enumerable: true,
        configurable: true
    });
    SettingUI.prototype.soundBack = function (v) {
        v = Math.min(v, 1);
        v = Math.max(v, 0);
        this.curyinyueV = v;
        if (!this.yinyueOn) {
            v = 0;
        }
        GlobalDefine.backSoundChannel.volume = v;
    };
    SettingUI.prototype.yinxiaoBack = function (v) {
        v = Math.min(v, 1);
        v = Math.max(v, 0);
        this.curyinxiaoV = v;
        if (!this.yinxiaoOn) {
            v = 0;
        }
        GlobalDefine.playSoundVolume = v;
    };
    SettingUI.prototype.getTexture = function (name) {
        return SheetManage.getTextureFromSheet(name, 'mj_setting_json');
    };
    return SettingUI;
}(UIBase));
SettingUI.NAME = 'SettingUI';
__reflect(SettingUI.prototype, "SettingUI");
//# sourceMappingURL=SettingUI.js.map