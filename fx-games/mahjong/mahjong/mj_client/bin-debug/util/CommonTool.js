var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var CommonTool = (function () {
    function CommonTool() {
    }
    /**
     * keepNum 保留几位小数
     */
    CommonTool.getPercentNum = function (value, keepNum) {
        if (keepNum === void 0) { keepNum = 0; }
        var k = Math.pow(10, keepNum);
        var t = value * k * 100;
        t = Math.round(t);
        return t / k + '%';
    };
    /**
     * 保留小数
     */
    CommonTool.getKeepNum = function (value, keepNum) {
        if (keepNum === void 0) { keepNum = 0; }
        var k = Math.pow(10, keepNum);
        var t = value * k;
        t = Math.round(t);
        return t / k;
    };
    /**
     * 大于万显示 xx万
     */
    CommonTool.getMoneyShow = function (value) {
        var vv = Math.floor(value / 10000);
        if (vv > 0) {
            return vv + '万';
        }
        return value.toString();
    };
    CommonTool.addStroke = function (tx, width, color) {
        if (width === void 0) { width = 2; }
        if (color === void 0) { color = 0; }
        tx.stroke = width;
        tx.strokeColor = color;
    };
    CommonTool.setColorText = function (label, ivo) {
        label.textColor = ColorDefine.rareColr[ivo.quality];
        label.text = ivo.name;
    };
    CommonTool.getColorTextByRare = function (content, quality) {
        var c = ColorDefine.rareHtmlColor[quality];
        return "<font color=" + c + ">" + content + "</font>";
    };
    /**
     * color: htmlColor
     */
    CommonTool.getColorText = function (content, color) {
        return "<font color=" + color + ">" + content + "</font>";
    };
    /**
     * 格式 'xxxx{0},xxx{1}',333,666
     */
    CommonTool.replaceStr = function () {
        var arg = [];
        for (var _i = 0; _i < arguments.length; _i++) {
            arg[_i] = arguments[_i];
        }
        var s = arg.shift();
        for (var key in arg) {
            var value = arg[key];
            s = s.replace(/\{\d+\}/, value);
        }
        return s;
    };
    /**
     * 返回html 的 'xxxx{0},xxx{1}',333,666
     */
    CommonTool.replaceStrBackColor = function () {
        var arg = [];
        for (var _i = 0; _i < arguments.length; _i++) {
            arg[_i] = arguments[_i];
        }
        var s = arg.shift();
        for (var key in arg) {
            var value = arg[key];
            s = s.replace(/\{\d+\}/, value);
        }
        return CommonTool.htmlP.parser(s);
    };
    /**
     * 跳转UI
     */
    CommonTool.skipToUI = function (ui) {
        if (!ui)
            return;
        var list = ui.split(':');
        if (list.length == 1) {
            TFacade.toggleUI(list[0]);
        }
        else {
            TFacade.toggleUI(list[0]).execute(list[1]);
        }
    };
    return CommonTool;
}());
CommonTool.htmlP = new egret.HtmlTextParser();
CommonTool.grayFilter = new egret.ColorMatrixFilter([
    0.3, 0.6, 0, 0, 0,
    0.3, 0.6, 0, 0, 0,
    0.3, 0.6, 0, 0, 0,
    0, 0, 0, 1, 0
]);
__reflect(CommonTool.prototype, "CommonTool");
//# sourceMappingURL=CommonTool.js.map