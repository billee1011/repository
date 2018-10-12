var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var ColorDefine = (function () {
    function ColorDefine() {
    }
    ColorDefine.getRareIcon = function (rare) {
        return this.rareYinshe[rare];
    };
    ColorDefine.getColorByQuality = function (quailty) {
        return this.rareColr[quailty];
    };
    return ColorDefine;
}());
ColorDefine.rareYinshe = { 99: 'weigoumai', 0: 'rare_hui', 1: 'rare_bai', 2: 'rare_lv', 3: 'rare_lan', 4: 'rare_zi', 5: 'rare_cheng', 6: 'rare_hong' };
ColorDefine.rareColr = { 0: 0xbbbbbb, 1: 0xFFFFFF, 2: 0x00FF00, 3: 0x26aff5, 4: 0xb049ff, 5: 0xff8a00, 6: 0xFF0000 };
ColorDefine.rareHtmlColor = { 0: '#bbbbbb', 1: '#FFFFFF', 2: '#00FF00', 3: '#26aff5', 4: '#b049ff', 5: '#ff8a00', 6: '#FF0000' };
__reflect(ColorDefine.prototype, "ColorDefine");
//# sourceMappingURL=ColorDefine.js.map