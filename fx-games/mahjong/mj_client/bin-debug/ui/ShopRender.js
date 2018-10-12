var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var ShopRender = (function (_super) {
    __extends(ShopRender, _super);
    function ShopRender() {
        return _super.call(this, 'resource/UI_exml/ShopItem.exml') || this;
    }
    ShopRender.prototype.uiLoadComplete = function () {
    };
    ShopRender.prototype.dataChanged = function () {
        var v = this.data;
        this.label_diamond.text = v.toString();
        this.btn_buy.label = v.toString();
    };
    return ShopRender;
}(RenderBase));
__reflect(ShopRender.prototype, "ShopRender");
//# sourceMappingURL=ShopRender.js.map