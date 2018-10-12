var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var PlayerTopRender = (function (_super) {
    __extends(PlayerTopRender, _super);
    function PlayerTopRender() {
        return _super.call(this) || this;
    }
    PlayerTopRender.prototype.childInit = function () {
        this.horNum = 13;
        this.ownCardList = new HPagelist(CardRender, 38, 59, false);
        this.ownCardList.updateXY(55, 4);
        this.ownCardList.rightLeft = true;
        this.addChild(this.ownCardList);
        this.duiList = new HPagelist(DuiCardRender, 36, 55, false);
        this.duiList.updateXY(586, 6);
        this.addChild(this.duiList);
        this.playedList = new HPagelist(PlayedCardRender, 36, 43, true, this.horNum);
        this.playedList.rightLeft = true;
        this.playedList.updateXY(46, 92);
        this.addChild(this.playedList);
        this.head.readyPic.x = -45;
        this.head.readyPic.y = 47;
        this.opEffectItem.updateXY(341, 125);
        this.head.updateXY(633, 0);
    };
    PlayerTopRender.prototype.layout = function () {
        /*	let len:number = this.vo.playedCardList ? this.vo.playedCardList.length*36:0;
            this.playedList.x = 500-len;*/
        this.refreshOwnCardPosition();
    };
    PlayerTopRender.prototype.refreshOwnCardPosition = function () {
        var len = this.vo.duiCardList ? this.vo.duiCardList.length * 36 : 0;
        this.duiList.x = 596 - len;
    };
    PlayerTopRender.prototype.refreshPlayedCard = function () {
        if (this.playedList) {
            //var temp:any[] = this.vo.playedCardList.concat();
            //temp.reverse();
            this.playedList.displayList(this.vo.playedCardList);
        }
    };
    PlayerTopRender.prototype.addVoToOwnCardList = function (vo) {
        this.vo.ownCardList.unshift(vo);
    };
    return PlayerTopRender;
}(PlayerBaseRender));
__reflect(PlayerTopRender.prototype, "PlayerTopRender");
//# sourceMappingURL=PlayerTopRender.js.map