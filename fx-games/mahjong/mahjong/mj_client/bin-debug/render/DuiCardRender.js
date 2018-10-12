var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var DuiCardRender = (function (_super) {
    __extends(DuiCardRender, _super);
    function DuiCardRender() {
        return _super.call(this, '') || this;
    }
    DuiCardRender.prototype.uiLoadComplete = function () {
        this.model = TFacade.getProxy(MjModel.NAME);
        this.icon = new AsyImage(0, 0, true);
        this.addChild(this.icon);
    };
    DuiCardRender.prototype.dataChanged = function () {
        if (!this.data) {
            this.clear();
            return;
        }
        if (!this.icon) {
            this.icon = new AsyImage(0, 0, true);
            this.addChild(this.icon);
        }
        var vo = this.data;
        this.cvo = vo;
        var n;
        var p = this.model.getIconPByPosition(vo.position);
        var tt = vo.style == 4 ? vo.type + 1 - 10 : vo.type;
        n = "p" + p + "s" + vo.style + "_" + tt;
        this.icon.setUrl(n);
    };
    DuiCardRender.prototype.setScale = function (sx, sy) {
        if (this.icon) {
            this.icon.scaleX = sx;
            this.icon.scaleY = sy;
        }
    };
    DuiCardRender.prototype.clear = function () {
        DisplayUtil.removeDisplay(this.icon);
        this.icon = null;
    };
    return DuiCardRender;
}(RenderBase));
__reflect(DuiCardRender.prototype, "DuiCardRender");
//# sourceMappingURL=DuiCardRender.js.map