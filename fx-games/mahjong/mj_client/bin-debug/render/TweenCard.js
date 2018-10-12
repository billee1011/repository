var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var TweenCard = (function (_super) {
    __extends(TweenCard, _super);
    function TweenCard() {
        return _super.call(this, '') || this;
    }
    TweenCard.prototype.uiLoadComplete = function () {
        this.icon = new AsyImage(0, 0, true);
        this.addChild(this.icon);
    };
    TweenCard.prototype.dataChanged = function () {
        if (!this.data)
            return;
        if (!this.icon) {
            this.icon = new AsyImage(0, 0, true);
            this.addChild(this.icon);
        }
        var vo = this.data;
        var n;
        var tt = vo.style == 4 ? vo.type + 1 - 10 : vo.type;
        n = "p4s" + vo.style + "_" + tt;
        this.icon.setUrl(n);
    };
    TweenCard.prototype.clear = function () {
        DisplayUtil.removeDisplay(this.icon);
        this.icon = null;
    };
    return TweenCard;
}(RenderBase));
__reflect(TweenCard.prototype, "TweenCard");
//# sourceMappingURL=TweenCard.js.map