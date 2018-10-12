var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var PlayerLeftRender = (function (_super) {
    __extends(PlayerLeftRender, _super);
    function PlayerLeftRender() {
        return _super.call(this) || this;
    }
    PlayerLeftRender.prototype.childInit = function () {
        this.horNum = 12;
        this.ownCardList = new HPagelist(CardRender, 24, 30, true);
        this.ownCardList.updateXY(166, 13);
        this.addChild(this.ownCardList);
        this.duiList = new HPagelist(DuiCardRender, 49, 27, true);
        this.duiList.updateXY(154, 14);
        this.addChild(this.duiList);
        this.playedList = new HPagelist(PlayedCardRender, 49, 27, false, this.horNum);
        this.playedList.updateXY(222, 90);
        this.addChild(this.playedList);
        this.head.updateXY(14, 78);
        this.head.readyPic.x = 120;
        this.head.readyPic.y = 52;
        this.opEffectItem.updateXY(250, 232);
    };
    PlayerLeftRender.prototype.layout = function () {
        this.refreshOwnCardPosition();
    };
    PlayerLeftRender.prototype.refreshOwnCardPosition = function () {
        if (this.duiList) {
            this.ownCardList.y = this.vo.duiCardList ? this.vo.duiCardList.length * 27 + 28 : 13;
        }
        else {
            this.ownCardList.y = 13;
        }
    };
    return PlayerLeftRender;
}(PlayerBaseRender));
__reflect(PlayerLeftRender.prototype, "PlayerLeftRender");
//# sourceMappingURL=PlayerLeftRender.js.map