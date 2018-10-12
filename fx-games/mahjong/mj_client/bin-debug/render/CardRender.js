var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
/***
 * 每位玩家手上的牌
 */
var CardRender = (function (_super) {
    __extends(CardRender, _super);
    function CardRender() {
        return _super.call(this, '') || this;
    }
    CardRender.prototype.uiLoadComplete = function () {
        this.model = TFacade.getProxy(MjModel.NAME);
        this.plIcon = new eui.Image;
        this.addChild(this.plIcon);
    };
    CardRender.prototype.dataChanged = function () {
        if (!this.data)
            return;
        this.plIcon.source = null;
        if (!this.icon) {
            this.icon = new AsyImage(0, 0, true);
            this.addChildAt(this.icon, 0);
            this.icon.touchEnabled = this.icon.touchChildren = false;
        }
        var vo = this.data;
        this.cvo = vo;
        var n;
        if (vo.position == this.model.mainDir) {
            var style = this.model.getIconStyle(vo.position);
            var p = this.model.getIconPByPosition(vo.position);
            var tt = vo.style == 4 ? vo.type + 1 - 10 : vo.type;
            n = "p" + p + style + vo.style + "_" + tt;
            this.icon.setUrl(n);
            if (this.model.card_pizi.style == vo.style && this.model.card_pizi.type == vo.type) {
                this.plIcon.visible = true;
                this.plIcon.source = SheetManage.getTextureFromCenter('icon_pizi');
            }
            if (this.model.card_laizi.style == vo.style && this.model.card_laizi.type == vo.type) {
                this.plIcon.visible = true;
                this.plIcon.source = SheetManage.getTextureFromCenter('icon_laizi');
            }
            this.plIcon.x = 53;
            this.plIcon.y = 2;
        }
        else {
            var dir = this.model.getIconPByPosition(vo.position);
            n = "tbgs_" + dir;
            this.icon.setUrl(n);
            this.plIcon.visible = false;
        }
        this.dir = vo.position;
    };
    CardRender.prototype.refreshOther = function () {
        this.offsetLastCard();
    };
    CardRender.prototype.offsetLastCard = function () {
        if (!this.cvo.justGet) {
            return;
        }
        var gap = 20;
        var mdir = this.model.mainDir;
        if (this.model.checkIsTop(mdir, this.dir)) {
            this.x -= gap;
        }
        else if (this.model.checkIsLeft(mdir, this.dir)) {
            this.y += gap;
        }
        else if (this.model.checkIsRight(mdir, this.dir)) {
            this.y -= gap + 8;
        }
        else {
            this.x += gap;
        }
    };
    CardRender.prototype.clear = function () {
        DisplayUtil.removeDisplay(this.icon);
        this.icon = null;
    };
    return CardRender;
}(RenderBase));
__reflect(CardRender.prototype, "CardRender");
//# sourceMappingURL=CardRender.js.map