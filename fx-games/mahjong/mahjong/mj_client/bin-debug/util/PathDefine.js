var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var PathDefine = (function () {
    function PathDefine() {
    }
    return PathDefine;
}());
/**
 * 特效
 */
PathDefine.EFFECT = 'resource/Effect/';
/**
 * UI图片
 */
PathDefine.UI_IMAGE = 'resource/UIImage/';
/**
 * 麻将图标
 */
PathDefine.MJ_ICON = 'resource/mjIcon/';
PathDefine.MJ_SOUND = 'resource/sound/';
__reflect(PathDefine.prototype, "PathDefine");
//# sourceMappingURL=PathDefine.js.map