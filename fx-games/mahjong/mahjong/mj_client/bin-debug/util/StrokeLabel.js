var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
/**
 * 描边的label
 */
var StrokeLabel = (function () {
    function StrokeLabel(label) {
        this.label = label;
        this.label.stroke = 2;
        this.label.strokeColor = 0;
    }
    Object.defineProperty(StrokeLabel.prototype, "text", {
        set: function (value) {
            this.label.text = value;
        },
        enumerable: true,
        configurable: true
    });
    return StrokeLabel;
}());
__reflect(StrokeLabel.prototype, "StrokeLabel");
//# sourceMappingURL=StrokeLabel.js.map