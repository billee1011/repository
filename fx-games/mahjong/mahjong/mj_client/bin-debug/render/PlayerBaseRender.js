var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var PlayerBaseRender = (function (_super) {
    __extends(PlayerBaseRender, _super);
    function PlayerBaseRender() {
        return _super.call(this, '') || this;
    }
    PlayerBaseRender.prototype.uiLoadComplete = function () {
        this.head = new RoleHeadRender();
        this.addChild(this.head);
        this.opEffectItem = new OperateEffectItem();
        this.childInit();
        this.isMain = false;
    };
    PlayerBaseRender.prototype.childInit = function () {
        //override
    };
    PlayerBaseRender.prototype.dataChanged = function () {
        this.vo = this.data;
        this.head.data = this.vo;
        this.refreshOwnCard();
        this.refreshDuiCard();
        this.refreshPlayedCard();
        this.layout();
        /*if(GlobalDefine.gameState == 0){
            this.showReadyHand();
        }*/
        if (this.vo.ready) {
            this.showReadyHand();
        }
    };
    PlayerBaseRender.prototype.refreshDuiCard = function () {
        if (this.duiList) {
            this.duiList.displayList(this.vo.duiCardList);
            this.refreshOwnCardPosition();
        }
    };
    /**
     * 对过牌后 该干啥干啥
     */
    PlayerBaseRender.prototype.refreshOwnCardPosition = function () {
    };
    /**
     *
     */
    PlayerBaseRender.prototype.refreshPlayedCard = function () {
        if (this.playedList) {
            this.playedList.displayList(this.vo.playedCardList);
        }
    };
    PlayerBaseRender.prototype.addVoToOwnCardList = function (vo) {
        this.vo.ownCardList.push(vo);
    };
    /**
     * 对牌添加方式
     */
    PlayerBaseRender.prototype.addVoToduiCardList = function (vo) {
        this.vo.duiCardList.push(vo);
    };
    /**
     * 因为有些是从数组前面添加，固可重写此方法
     */
    PlayerBaseRender.prototype.addVoToPlayedCardList = function (vo) {
        this.vo.playedCardList.push(vo);
    };
    /**
     * 出牌后先隐藏，待动画播放完毕再显示这张牌
     */
    PlayerBaseRender.prototype.setNowPlayedCardHide = function (id) {
        //this.refreshPlayedCard();
        var render;
        var t = this.playedList.getAllItem();
        for (var key in t) {
            render = t[key];
            if (render.data.id == id) {
                render.visible = false;
                return render.localToGlobal(0, 0);
            }
        }
        return null;
    };
    /**
     * 动画播放完毕显示牌面
     */
    PlayerBaseRender.prototype.setNowPlayedCardShow = function (id) {
        var render;
        var t = this.playedList.getAllItem();
        for (var key in t) {
            render = t[key];
            if (render.data.id == id) {
                render.visible = true;
                render.showSign();
                break;
            }
        }
    };
    PlayerBaseRender.prototype.refreshOwnCard = function () {
        if (this.ownCardList) {
            this.ownCardList.displayList(this.vo.ownCardList);
            this.ownCardList.refreshOther();
        }
    };
    PlayerBaseRender.prototype.layout = function () {
        //override
    };
    PlayerBaseRender.prototype.showReadyHand = function () {
        this.head.readyPic.visible = true;
    };
    PlayerBaseRender.prototype.hideReadyHand = function () {
        this.head.readyPic.visible = false;
    };
    PlayerBaseRender.prototype.showOperateEffect = function (type) {
        this.opEffectItem.playEffect(type);
        this.addChild(this.opEffectItem);
    };
    return PlayerBaseRender;
}(RenderBase));
__reflect(PlayerBaseRender.prototype, "PlayerBaseRender");
//# sourceMappingURL=PlayerBaseRender.js.map