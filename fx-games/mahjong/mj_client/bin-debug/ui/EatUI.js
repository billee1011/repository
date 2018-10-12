var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var EatUI = (function (_super) {
    __extends(EatUI, _super);
    function EatUI() {
        var _this = _super.call(this, '') || this;
        _this.isAloneShow = true;
        _this.centerFlag = true;
        return _this;
    }
    EatUI.prototype.uiLoadComplete = function () {
        this.y = GlobalDefine.stageH * 0.88;
        this.model = TFacade.getProxy(MjModel.NAME);
        this.list = new HPagelist(EatRender, 220, 84, false);
        this.addChild(this.list);
        this.list.addEventListener(HPagelist.RENDER_CHANGE, this.renderClick, this);
    };
    EatUI.prototype.renderClick = function (evt) {
        var data = this.list.currentItem.data;
        var t = [];
        for (var key in data) {
            var vo = data[key];
            if (vo.id != this.model.playingCard.id) {
                t.push(vo.id);
            }
        }
        SendOperate.instance.requestEatCard(t);
        this.hide();
    };
    /**
     * 点击吃标签后弹出，没事别乱弹
     */
    EatUI.prototype.doExecute = function () {
        var cur = this.model.playingCard;
        var hvo = this.model.getHeromsg()[GlobalDefine.herovo.roleId];
        this.ownList = hvo.ownCardList;
        //筛选出 可以吃的牌的情况  //痞子癞子不能参与吃
        var farr = [];
        var a = this.getCard(cur.style, cur.type + 1);
        var b = this.getCard(cur.style, cur.type + 2);
        if (a && b) {
            farr.push([cur, a, b]);
        }
        var c = this.getCard(cur.style, cur.type - 1);
        if (a && c) {
            farr.push([c, cur, a]);
        }
        var d = this.getCard(cur.style, cur.type - 2);
        if (c && d) {
            farr.push([d, c, cur]);
        }
        this.list.displayList(farr);
        this.outIndex = egret.setTimeout(this.layout, this, 100);
        this.list.visible = false;
    };
    EatUI.prototype.layout = function () {
        egret.clearTimeout(this.outIndex);
        this.x = GlobalDefine.stageW * .5 - this.list.width * .5;
        this.list.visible = true;
        this.y = GlobalDefine.stageH * 0.68;
    };
    EatUI.prototype.getCard = function (style, value) {
        var temp;
        for (var i = 0; i < this.ownList.length; i++) {
            temp = this.ownList[i];
            if (temp.style != style)
                continue;
            if (style == this.model.card_pizi.style && value == this.model.card_pizi.type)
                continue; //不能是痞子
            if (style == this.model.card_laizi.style && value == this.model.card_laizi.type)
                continue; //不能是癞子
            if (temp.style == style && temp.type == value) {
                return temp;
            }
        }
        return null;
    };
    EatUI.prototype.backGroundClick = function () {
    };
    return EatUI;
}(UIBase));
EatUI.NAME = 'EatUI';
__reflect(EatUI.prototype, "EatUI");
//# sourceMappingURL=EatUI.js.map