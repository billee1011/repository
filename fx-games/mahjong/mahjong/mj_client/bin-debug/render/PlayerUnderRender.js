var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var PlayerUnderRender = (function (_super) {
    __extends(PlayerUnderRender, _super);
    function PlayerUnderRender() {
        return _super.call(this) || this;
    }
    PlayerUnderRender.prototype.childInit = function () {
        this.horNum = 13;
        this.ownCardList = new HPagelist(CardRender, 83, 125, false);
        this.ownCardList.updateXY(23, 141);
        this.addChild(this.ownCardList);
        this.duiList = new HPagelist(DuiCardRender, 55, 84, false);
        this.duiList.updateXY(30, 177);
        this.addChild(this.duiList);
        this.playedList = new HPagelist(PlayedCardRender, 36, 43, true, this.horNum);
        this.playedList.layoutDirY = -1;
        this.playedList.updateXY(346, 68);
        this.addChild(this.playedList);
        this.head.readyPic.x = 125;
        this.head.readyPic.y = 53;
        this.opEffectItem.updateXY(575, 80);
        this.head.updateXY(0, -15);
    };
    PlayerUnderRender.prototype.layout = function () {
        this.refreshOwnCardPosition();
    };
    PlayerUnderRender.prototype.refreshOwnCardPosition = function () {
        if (this.duiList) {
            this.ownCardList.x = this.vo.duiCardList ? this.vo.duiCardList.length * 55 + 43 : 23;
        }
        else {
            this.ownCardList.x = 23;
        }
    };
    return PlayerUnderRender;
}(PlayerBaseRender));
__reflect(PlayerUnderRender.prototype, "PlayerUnderRender");
//# sourceMappingURL=PlayerUnderRender.js.map