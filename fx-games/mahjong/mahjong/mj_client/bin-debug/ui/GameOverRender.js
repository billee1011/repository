var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var GameOverRender = (function (_super) {
    __extends(GameOverRender, _super);
    function GameOverRender() {
        return _super.call(this, 'resource/UI_exml/GameOverItem.exml') || this;
    }
    GameOverRender.prototype.uiLoadComplete = function (evt) {
        if (evt === void 0) { evt = null; }
        var bg = new AsyImage();
        this.addChildAt(bg, 0);
        bg.setUrl(PathDefine.UI_IMAGE + 'settle_renderBg.png');
        this.list = new HPagelist(OverLabelRender, 180, 33);
        this.list.updateXY(35, 122);
        this.addChild(this.list);
        this.label_win = new AniLabel('mj_settleWinFont_fnt', -2);
        this.addChild(this.label_win);
        this.label_lose = new AniLabel('mj_settleLoseFont_fnt', -2);
        this.addChild(this.label_lose);
    };
    GameOverRender.prototype.dataChanged = function () {
        var vo = this.data;
        this.label_id.text = vo.roleid.toString();
        this.label_name.text = vo.name;
        this.list.displayList(vo.countList);
        var temp;
        this.label_lose.visible = vo.score < 0;
        this.label_win.visible = vo.score >= 0;
        var s;
        if (vo.score >= 0) {
            temp = this.label_win;
            s = '+';
        }
        else {
            temp = this.label_lose;
            s = '-';
        }
        temp.text = s + Math.abs(vo.score);
        temp.x = 110 - temp.width * .5;
        temp.y = 338;
    };
    return GameOverRender;
}(RenderBase));
__reflect(GameOverRender.prototype, "GameOverRender");
//# sourceMappingURL=GameOverRender.js.map