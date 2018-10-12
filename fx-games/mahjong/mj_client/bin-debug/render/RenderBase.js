var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var RenderBase = (function (_super) {
    __extends(RenderBase, _super);
    function RenderBase(skinUrl) {
        var _this = _super.call(this) || this;
        _this.addEventListener(eui.UIEvent.COMPLETE, _this.uiLoadComplete, _this);
        _this.skinName = skinUrl;
        return _this;
    }
    RenderBase.prototype.uiLoadComplete = function (evt) {
        if (evt === void 0) { evt = null; }
        //override
    };
    RenderBase.prototype.dataChanged = function () {
    };
    RenderBase.prototype.updateXY = function (xx, yy) {
        this.x = xx;
        this.y = yy;
    };
    RenderBase.prototype.refreshRender = function () {
        this.dataChanged();
    };
    RenderBase.prototype.clear = function () {
    };
    RenderBase.prototype.refreshOther = function () {
    };
    Object.defineProperty(RenderBase.prototype, "choosed", {
        get: function () {
            return this._choosed;
        },
        set: function (value) {
            this._choosed = value;
            this.doChoose();
        },
        enumerable: true,
        configurable: true
    });
    RenderBase.prototype.doChoose = function () {
        // override
    };
    return RenderBase;
}(eui.ItemRenderer));
__reflect(RenderBase.prototype, "RenderBase");
//# sourceMappingURL=RenderBase.js.map