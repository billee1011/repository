var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var Notice = (function (_super) {
    __extends(Notice, _super);
    function Notice() {
        var _this = _super.call(this) || this;
        /*this.bg = new egret.Bitmap(RES.getRes("toast-bg_png"));
        this.addChild(this.bg);
        this.bg.width = GlobalDefine.stageW;
        this.bg.height = 22;*/
        _this.label = new eui.Label();
        _this.label.size = 24;
        //this.label.textColor = 0xFFFF00;
        _this.label.fontFamily = 'Microsoft YaHei';
        _this.addChild(_this.label);
        _this.addEventListener(egret.Event.REMOVED_FROM_STAGE, _this.removeHandler, _this);
        return _this;
    }
    Notice.prototype.play = function (content, loop, mask) {
        if (loop === void 0) { loop = false; }
        if (mask === void 0) { mask = null; }
        this.masks = mask;
        this.loop = loop;
        if (this.tw) {
            egret.Tween.removeTweens(this.label);
        }
        this.label.text = content;
        this.doPlay();
    };
    Notice.prototype.playEnd = function () {
        if (this.loop) {
            this.doPlay();
        }
        else {
            DisplayUtil.removeDisplay(this);
        }
    };
    Notice.prototype.doPlay = function () {
        this.tw = egret.Tween.get(this.label);
        this.label.x = this.masks ? this.masks.x + this.masks.width : GlobalDefine.stageW;
        var aimX = this.masks ? this.masks.x - this.label.width - 30 : -this.label.width - 30;
        this.tw.to({ x: aimX }, 20000);
        this.tw.call(this.playEnd, this);
    };
    Notice.prototype.removeHandler = function (evt) {
        egret.Tween.removeTweens(this.label);
        this.tw = null;
    };
    return Notice;
}(egret.Sprite));
__reflect(Notice.prototype, "Notice");
//# sourceMappingURL=Notice.js.map