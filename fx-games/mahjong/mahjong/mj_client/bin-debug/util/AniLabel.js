var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var AniLabel = (function (_super) {
    __extends(AniLabel, _super);
    function AniLabel(fnt, space) {
        if (space === void 0) { space = 0; }
        var _this = _super.call(this) || this;
        var bf = RES.getRes(fnt);
        _this.font = bf;
        _this.letterSpacing = space;
        return _this;
    }
    AniLabel.prototype.updateXY = function (xx, yy) {
        this.x = xx;
        this.y = yy;
    };
    return AniLabel;
}(egret.BitmapText));
__reflect(AniLabel.prototype, "AniLabel");
//# sourceMappingURL=AniLabel.js.map