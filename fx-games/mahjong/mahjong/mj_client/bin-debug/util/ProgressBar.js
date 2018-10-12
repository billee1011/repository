var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var ProgressBar = (function () {
    function ProgressBar(bar, label) {
        if (label === void 0) { label = null; }
        this.bar = bar;
        this.label_v = label;
        this.rect = new egret.Rectangle(0, 0, 0, bar.height);
        this.len = this.bar.width;
    }
    ProgressBar.prototype.setData = function (cur, total) {
        if (this.label_v) {
            this.label_v.text = cur + '/' + total;
        }
        this.rect.width = Math.floor(cur / total * this.len);
        this.bar.scrollRect = this.rect;
    };
    ProgressBar.prototype.getRectWidth = function () {
        return this.rect.width;
    };
    return ProgressBar;
}());
__reflect(ProgressBar.prototype, "ProgressBar");
//# sourceMappingURL=ProgressBar.js.map