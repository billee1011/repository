var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var EatRender = (function (_super) {
    __extends(EatRender, _super);
    function EatRender() {
        return _super.call(this, '') || this;
    }
    EatRender.prototype.uiLoadComplete = function (evt) {
        if (evt === void 0) { evt = null; }
        //override
        this.list = new HPagelist(EatCardRender, 55, 84, false);
        this.addChild(this.list);
    };
    EatRender.prototype.dataChanged = function () {
        this.list.displayList(this.data);
    };
    return EatRender;
}(RenderBase));
__reflect(EatRender.prototype, "EatRender");
//# sourceMappingURL=EatRender.js.map