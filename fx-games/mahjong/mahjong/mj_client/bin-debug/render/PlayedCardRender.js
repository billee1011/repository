var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var PlayedCardRender = (function (_super) {
    __extends(PlayedCardRender, _super);
    function PlayedCardRender() {
        return _super.call(this, '') || this;
    }
    PlayedCardRender.prototype.uiLoadComplete = function () {
        this.model = TFacade.getProxy(MjModel.NAME);
        this.icon = new AsyImage(0, 0, true);
        this.addChild(this.icon);
    };
    PlayedCardRender.prototype.dataChanged = function () {
        if (!this.data)
            return;
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
        if (vo.position == this.model.mainDir) {
            this.icon.setScale(0.65, 0.65);
        }
    };
    PlayedCardRender.prototype.showSign = function () {
        this.ud = LayerManage.instance.getUpDown();
        this.addChild(this.ud);
        if (this.cvo.position != this.model.mainDir) {
            this.ud.x = this.icon.width * .5;
            this.ud.y = -15;
        }
        else {
            this.ud.x = 16;
            this.ud.y = -15;
        }
    };
    PlayedCardRender.prototype.clear = function () {
        DisplayUtil.removeDisplay(this.ud);
        this.icon.clear();
        DisplayUtil.removeDisplay(this.icon);
        this.icon = null;
    };
    return PlayedCardRender;
}(RenderBase));
__reflect(PlayedCardRender.prototype, "PlayedCardRender");
//# sourceMappingURL=PlayedCardRender.js.map