var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var OperateUI = (function (_super) {
    __extends(OperateUI, _super);
    function OperateUI() {
        return _super.call(this, 'resource/UI_exml/Operate.exml') || this;
        //this.centerFlag = true;
        //this.isAloneShow = true;
    }
    OperateUI.prototype.bindEvent = function () {
    };
    OperateUI.prototype.uiLoadComplete = function () {
        this.y = GlobalDefine.stageH * .5 + 100;
        this.list = new HPagelist(OperateRender, 91, 100, false);
        this.addChild(this.list);
        this.list.addEventListener(HPagelist.RENDER_CHANGE, this.listChange, this);
    };
    OperateUI.prototype.listChange = function (data) {
        var opNum = data.data;
        this.hide();
        if (opNum == 8) {
            TFacade.toggleUI(EatUI.NAME, 1).execute('eat');
        }
        else {
            /*if(opNum == 5){
                Tnotice.instance.popUpTip('你胡了，是的,胡了，别玩了');
                return;
            }*/
            SendOperate.instance.requestOperate(opNum);
        }
    };
    OperateUI.prototype.doExecute = function () {
        var temp = this.params;
        temp.push(7);
        this.list.displayList(temp);
        this.x = GlobalDefine.stageW - this.width - 400;
    };
    return OperateUI;
}(UIBase));
OperateUI.NAME = 'OperateUI';
__reflect(OperateUI.prototype, "OperateUI");
//# sourceMappingURL=OperateUI.js.map