var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var SettleRender = (function (_super) {
    __extends(SettleRender, _super);
    function SettleRender() {
        return _super.call(this, 'resource/UI_exml/SettleItem.exml') || this;
    }
    SettleRender.prototype.uiLoadComplete = function (evt) {
        if (evt === void 0) { evt = null; }
        var bg = new AsyImage();
        this.addChildAt(bg, 0);
        bg.setUrl(PathDefine.UI_IMAGE + 'settle_bar.png');
        this.list = new HPagelist(SettleCardRender, 36, 84, false);
        this.list.updateXY(118, 45);
        this.addChild(this.list);
        //override
        this.model = TFacade.getProxy(MjModel.NAME);
        this.label_win = new AniLabel('mj_settleWinFont_fnt', -2);
        this.addChild(this.label_win);
        this.label_lose = new AniLabel('mj_settleLoseFont_fnt', -2);
        this.addChild(this.label_lose);
    };
    SettleRender.prototype.dataChanged = function () {
        var vo = this.data;
        var hvo = this.model.playerData[vo.roleId];
        if (hvo) {
            this.label_name.text = hvo.name;
            this.label_id.text = hvo.roleId.toString();
            ;
        }
        this.list.displayList(vo.cardList);
        var temp;
        this.label_lose.visible = vo.curScore < 0;
        this.label_win.visible = vo.curScore >= 0;
        var s;
        if (vo.curScore >= 0) {
            temp = this.label_win;
            s = '+';
        }
        else {
            temp = this.label_lose;
            s = '-';
        }
        temp.text = s + Math.abs(vo.curScore);
        temp.x = 776;
        temp.y = 35;
    };
    return SettleRender;
}(RenderBase));
__reflect(SettleRender.prototype, "SettleRender");
//# sourceMappingURL=SettleRender.js.map