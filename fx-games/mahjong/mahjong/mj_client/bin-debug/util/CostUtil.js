var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
/**
 * 元宝等虚拟货币用这个
 */
var CostUtil = (function (_super) {
    __extends(CostUtil, _super);
    /**
     * type 0:元宝 1：金币 2:寻宝陨石
     *
     */
    function CostUtil(type, iconW, fontSize, fontColor, fontStroke) {
        if (type === void 0) { type = 0; }
        if (iconW === void 0) { iconW = 25; }
        if (fontSize === void 0) { fontSize = 20; }
        if (fontColor === void 0) { fontColor = 0; }
        if (fontStroke === void 0) { fontStroke = 0; }
        var _this = _super.call(this) || this;
        _this.size = iconW;
        _this.image_icon = new eui.Image();
        _this.addChild(_this.image_icon);
        _this.image_icon.width = _this.image_icon.height = _this.size;
        _this.label_value = new eui.Label();
        _this.addChild(_this.label_value);
        _this.label_value.size = fontSize;
        _this.label_value.x = iconW + 2;
        if (fontColor) {
            _this.label_value.textColor = fontColor;
        }
        if (fontStroke) {
            CommonTool.addStroke(_this.label_value, 1, fontStroke);
        }
        return _this;
        ///this.image_icon.source = SheetManage.getCurrencyTexture(type);
    }
    Object.defineProperty(CostUtil.prototype, "type", {
        set: function (value) {
            //this.image_icon.source = SheetManage.getCurrencyTexture(value);
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(CostUtil.prototype, "value", {
        set: function (v) {
            this.label_value.text = v.toString();
            this.label_value.y = this.size - this.label_value.height;
        },
        enumerable: true,
        configurable: true
    });
    CostUtil.prototype.updateXY = function (xx, yy) {
        this.x = xx;
        this.y = yy;
    };
    return CostUtil;
}(egret.DisplayObjectContainer));
__reflect(CostUtil.prototype, "CostUtil");
//# sourceMappingURL=CostUtil.js.map