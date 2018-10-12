var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var DisplayUtil = (function () {
    function DisplayUtil() {
    }
    DisplayUtil.removeDisplay = function (dis, parent) {
        if (parent === void 0) { parent = null; }
        if (!dis)
            return;
        if (!parent) {
            parent = dis.parent;
        }
        if (!parent)
            return;
        parent.removeChild(dis);
    };
    return DisplayUtil;
}());
__reflect(DisplayUtil.prototype, "DisplayUtil");
//# sourceMappingURL=DisplayUtil.js.map