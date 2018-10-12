var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var IconLineRender = (function (_super) {
    __extends(IconLineRender, _super);
    function IconLineRender() {
        return _super.call(this, true) || this;
    }
    IconLineRender.prototype.showIcon = function (type) {
        //var ivo:ItemVO = CurrencyUnit.instance.getItemvoByCurrencyType(type);
        //this.icon.data = ivo;
    };
    return IconLineRender;
}(LineRender));
__reflect(IconLineRender.prototype, "IconLineRender");
//# sourceMappingURL=IconLineRender.js.map