var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var OperateEffectItem = (function (_super) {
    __extends(OperateEffectItem, _super);
    function OperateEffectItem() {
        var _this = _super.call(this) || this;
        _this.econ = new egret.Sprite;
        _this.addChild(_this.econ);
        _this.img_eff = new eui.Image;
        _this.econ.addChild(_this.img_eff);
        return _this;
    }
    OperateEffectItem.prototype.comOk = function (evt) {
    };
    OperateEffectItem.prototype.playEffect = function (type) {
        var model = TFacade.getProxy(MjModel.NAME);
        this.img_eff.addEventListener(egret.Event.COMPLETE, this.comOk, this);
        this.img_eff.source = model.getOperateTexture(type);
        this.img_eff.x = -83 * .5;
        this.img_eff.y = -79 * .5;
        this.econ.alpha = 0;
        this.econ.scaleX = 2;
        this.econ.scaleY = 2;
        this.tw = egret.Tween.get(this.econ);
        this.tw.to({ alpha: 1, scaleX: 1, scaleY: 1 }, 600)
            .wait(500)
            .to({ alpha: 0, scaleX: 1.2, scaleY: 1.2 }, 400)
            .call(this.playEnd, this);
    };
    OperateEffectItem.prototype.playEnd = function () {
        this.img_eff.source = null;
        DisplayUtil.removeDisplay(this);
    };
    OperateEffectItem.prototype.updateXY = function (xx, yy) {
        this.x = xx;
        this.y = yy;
    };
    return OperateEffectItem;
}(egret.Sprite));
__reflect(OperateEffectItem.prototype, "OperateEffectItem");
//# sourceMappingURL=OperateEffectItem.js.map