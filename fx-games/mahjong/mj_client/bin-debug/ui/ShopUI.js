var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var ShopUI = (function (_super) {
    __extends(ShopUI, _super);
    function ShopUI() {
        var _this = _super.call(this, 'resource/UI_exml/Shop.exml') || this;
        _this.centerFlag = true;
        _this.closeOther = false;
        _this.isAloneShow = true;
        return _this;
    }
    ShopUI.prototype.uiLoadComplete = function () {
        var sb = new ScrollBar(this.list_charge);
        this.addChild(sb);
        this.list = new PageList(this.list_charge, ShopRender);
        this.list.displayList([88, 99, 100, 500, 1000, 3000]);
    };
    return ShopUI;
}(UIBase));
ShopUI.NAME = 'ShopUI';
__reflect(ShopUI.prototype, "ShopUI");
//# sourceMappingURL=ShopUI.js.map