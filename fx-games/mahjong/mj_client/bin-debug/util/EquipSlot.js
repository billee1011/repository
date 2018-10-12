var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var EquipSlot = (function (_super) {
    __extends(EquipSlot, _super);
    function EquipSlot() {
        return _super.call(this) || this;
    }
    EquipSlot.prototype.doData = function () {
        var vo = this.data;
        this.label = vo.name;
    };
    Object.defineProperty(EquipSlot.prototype, "defaultSkin", {
        set: function (tex) {
            this._defaultSkin.source = tex;
        },
        enumerable: true,
        configurable: true
    });
    return EquipSlot;
}(GoodsSlot));
__reflect(EquipSlot.prototype, "EquipSlot");
//# sourceMappingURL=EquipSlot.js.map