var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
/**
 * 道具消耗用这个
 */
var CostItemUtil = (function (_super) {
    __extends(CostItemUtil, _super);
    /**
     *
     *
     */
    function CostItemUtil(iconW, fontSize, fontColor, fontStroke) {
        if (iconW === void 0) { iconW = 35; }
        if (fontSize === void 0) { fontSize = 20; }
        if (fontColor === void 0) { fontColor = 0; }
        if (fontStroke === void 0) { fontStroke = 0; }
        var _this = _super.call(this) || this;
        _this.size = iconW;
        _this.gslot = new NoRareSlot(iconW, iconW);
        _this.addChild(_this.gslot);
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
    }
    Object.defineProperty(CostItemUtil.prototype, "data", {
        set: function (ivo) {
            this.gslot.data = ivo;
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(CostItemUtil.prototype, "value", {
        set: function (v) {
            this.label_value.text = v.toString();
            this.label_value.y = this.size - this.label_value.height - 5;
        },
        enumerable: true,
        configurable: true
    });
    CostItemUtil.prototype.updateXY = function (xx, yy) {
        this.x = xx;
        this.y = yy;
    };
    return CostItemUtil;
}(egret.DisplayObjectContainer));
__reflect(CostItemUtil.prototype, "CostItemUtil");
//# sourceMappingURL=CostItemUtil.js.map