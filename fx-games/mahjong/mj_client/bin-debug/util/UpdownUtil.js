var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var UpdownUtil = (function (_super) {
    __extends(UpdownUtil, _super);
    function UpdownUtil(skin) {
        var _this = _super.call(this) || this;
        _this.img = new eui.Image();
        _this.img.source = SheetManage.getTextureFromCenter(skin);
        _this.addChild(_this.img);
        return _this;
    }
    UpdownUtil.prototype.toStage = function (evt) {
        egret.Tween.removeTweens(this.img);
        this.img.y = -40;
        this.img.x = -17;
        egret.Tween.get(this.img, { loop: true })
            .to({ y: -25 }, 600)
            .to({ y: -40 }, 600);
    };
    UpdownUtil.prototype.awayStage = function (evt) {
        egret.Tween.removeTweens(this.img);
    };
    return UpdownUtil;
}(StageDele));
__reflect(UpdownUtil.prototype, "UpdownUtil");
//# sourceMappingURL=UpdownUtil.js.map