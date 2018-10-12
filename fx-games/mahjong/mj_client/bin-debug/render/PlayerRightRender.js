var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var PlayerRightRender = (function (_super) {
    __extends(PlayerRightRender, _super);
    function PlayerRightRender() {
        return _super.call(this) || this;
    }
    PlayerRightRender.prototype.childInit = function () {
        this.topNum = 12;
        this.ownCardList = new HPagelist(CardRender, 24, 30, true);
        this.ownCardList.updateXY(52, 15);
        //this.ownCardList.downUp = true;
        this.addChild(this.ownCardList);
        this.duiList = new HPagelist(DuiCardRender, 49, 27, true);
        this.duiList.updateXY(38, 448);
        this.addChild(this.duiList);
        this.playedList = new HPagelist(PlayedCardRender, 49, 27, false, this.topNum);
        this.playedList.layoutDirX = -1;
        this.playedList.downUp = true;
        this.playedList.updateXY(-31, 81);
        this.addChild(this.playedList);
        this.head.readyPic.x = -38;
        this.head.readyPic.y = 62;
        this.opEffectItem.updateXY(-86, 227);
        this.head.updateXY(90, 44);
    };
    PlayerRightRender.prototype.layout = function () {
        /*	var t:number = Math.min(this.topNum,this.vo.playedCardList?this.vo.playedCardList.length:0)
            var len:number = t*27;
            this.playedList.y = 441-len;*/
        this.refreshDuiCardLocation();
    };
    PlayerRightRender.prototype.refreshOwnCard = function () {
        if (this.ownCardList) {
            this.ownCardList.displayList(this.vo.ownCardList);
            this.ownCardList.refreshOther();
            this.refreshOwnCardPosition();
        }
    };
    PlayerRightRender.prototype.refreshDuiCardLocation = function () {
        var len = this.vo.duiCardList ? this.vo.duiCardList.length * 27 : 0;
        this.duiList.y = 458 - len;
        this.refreshOwnCardPosition();
    };
    /**
     * override
     */
    PlayerRightRender.prototype.refreshOwnCardPosition = function () {
        var len = this.vo.ownCardList ? this.vo.ownCardList.length * 30 : 0;
        this.ownCardList.y = this.duiList.y - 30 - len;
    };
    PlayerRightRender.prototype.addVoToOwnCardList = function (vo) {
        this.vo.ownCardList.unshift(vo);
    };
    PlayerRightRender.prototype.addVoToduiCardList = function (vo) {
        this.vo.duiCardList.unshift(vo);
    };
    PlayerRightRender.prototype.refreshPlayedCard = function () {
        if (this.playedList) {
            //var temp:any[] = this.vo.playedCardList.concat();
            //temp.reverse();
            this.playedList.displayList(this.vo.playedCardList);
        }
    };
    return PlayerRightRender;
}(PlayerBaseRender));
__reflect(PlayerRightRender.prototype, "PlayerRightRender");
//# sourceMappingURL=PlayerRightRender.js.map