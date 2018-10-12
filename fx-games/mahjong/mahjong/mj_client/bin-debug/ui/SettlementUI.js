var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var SettlementUI = (function (_super) {
    __extends(SettlementUI, _super);
    function SettlementUI() {
        var _this = _super.call(this, 'resource/UI_exml/Settlement.exml', 'settle') || this;
        _this.centerFlag = true;
        _this.isAloneShow = true;
        return _this;
    }
    SettlementUI.prototype.bindEvent = function () {
        this.model = TFacade.getProxy(MjModel.NAME);
    };
    SettlementUI.prototype.uiLoadComplete = function () {
        this.bg = new AsyImage();
        this.addChildAt(this.bg, 0);
        this.bg.setUrl(PathDefine.UI_IMAGE + 'jiesuan_bg.png');
        this.list = new HPagelist(SettleRender, 200, 120, true, 1);
        this.addChild(this.list);
        this.list.updateXY(25, 100);
        this.state = new State();
        this.state.add('win', [this.img_titlewin]);
        this.state.add('ping', [this.img_titlehuang]);
        this.state.add('lose', [this.img_titlescore]);
        this.state.state = 'win';
        this.addChild(this.state);
        this.btn_close.addEventListener(egret.TouchEvent.TOUCH_TAP, this.hide, this);
        this.btn_start.addEventListener(egret.TouchEvent.TOUCH_TAP, this.startgame, this);
    };
    SettlementUI.prototype.startgame = function (evt) {
        this.hide();
    };
    SettlementUI.prototype.sleep = function () {
        SendOperate.instance.requestStartAgainGame();
    };
    /*private loaderOver():void
    {
        this.width = this.bg.width;
        this.height = this.bg.height;
        this.layoutP();
    }*/
    SettlementUI.prototype.doExecute = function () {
        var temp = [];
        for (var key in this.model.settleData) {
            temp.push(this.model.settleData[key]);
        }
        this.list.displayList(temp);
        this.state.state = this.params;
    };
    return SettlementUI;
}(UIBase));
SettlementUI.NAME = 'SettlementUI';
__reflect(SettlementUI.prototype, "SettlementUI");
//# sourceMappingURL=SettlementUI.js.map