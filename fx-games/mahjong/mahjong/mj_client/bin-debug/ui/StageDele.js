var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
/**
 * 监听添加/移除舞台事件
 */
var StageDele = (function (_super) {
    __extends(StageDele, _super);
    function StageDele() {
        var _this = _super.call(this) || this;
        _this.addEventListener(egret.Event.ADDED_TO_STAGE, _this.toStage, _this);
        _this.addEventListener(egret.Event.REMOVED_FROM_STAGE, _this.awayStage, _this);
        return _this;
    }
    StageDele.prototype.toStage = function (evt) {
    };
    StageDele.prototype.awayStage = function (evt) {
    };
    return StageDele;
}(egret.Sprite));
__reflect(StageDele.prototype, "StageDele");
//# sourceMappingURL=StageDele.js.map