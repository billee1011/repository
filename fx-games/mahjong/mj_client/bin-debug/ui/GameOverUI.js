var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var GameOverUI = (function (_super) {
    __extends(GameOverUI, _super);
    function GameOverUI() {
        var _this = _super.call(this, 'resource/UI_exml/GameOver.exml', 'settle') || this;
        _this.isAloneShow = true;
        return _this;
    }
    GameOverUI.prototype.uiLoadComplete = function () {
        this.model = TFacade.getProxy(MjModel.NAME);
        this.list = new HPagelist(GameOverRender, 236, 404, false);
        this.addChild(this.list);
        this.list.updateXY(46, 113);
        this.btn_close.addEventListener(egret.TouchEvent.TOUCH_TAP, this.hide, this);
    };
    GameOverUI.prototype.sleep = function () {
        TFacade.toggleUI(MainGameUI.NAME, 0);
        TFacade.toggleUI(MainFrameUI.NAME, 1);
    };
    GameOverUI.prototype.doExecute = function () {
        var temp = [];
        for (var key in this.model.overData) {
            temp.push(this.model.overData[key]);
        }
        this.list.displayList(temp);
    };
    GameOverUI.prototype.backGroundClick = function () {
    };
    return GameOverUI;
}(UIBase));
GameOverUI.NAME = 'GameOverUI';
__reflect(GameOverUI.prototype, "GameOverUI");
//# sourceMappingURL=GameOverUI.js.map