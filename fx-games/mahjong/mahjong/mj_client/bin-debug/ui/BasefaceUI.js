var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var BasefaceUI = (function (_super) {
    __extends(BasefaceUI, _super);
    function BasefaceUI() {
        var _this = _super.call(this, 'resource/UI_exml/BaseFace.exml') || this;
        _this.hideable = false;
        return _this;
    }
    BasefaceUI.prototype.bindEvent = function () {
        this._eventList[TabItemList.TAB_CHANGE] = [this.tabChangeHandler, this];
    };
    BasefaceUI.prototype.uiLoadComplete = function (evt) {
        this.tabList = new TabItemList();
        this.addChild(this.tabList);
        var temp;
        temp = new TButton(this.btn_role, 'btn_role');
        this.tabList.addItem(temp);
        temp = new TButton(this.btn_bag, 'btn_bag');
        this.tabList.addItem(temp);
        // temp.setBadgeOffset(-5,10);
        temp = new TButton(this.btn_baozhilin, 'btn_baozhilin');
        this.tabList.addItem(temp);
        temp = new TButton(this.btn_battle, 'btn_battle');
        this.tabList.addItem(temp);
        temp = new TButton(this.btn_shop, 'btn_shop');
        this.tabList.addItem(temp);
        temp = new TButton(this.btn_more, 'btn_more');
        this.tabList.addItem(temp);
        this.y = GlobalDefine.stage.stageHeight - this.height + 2;
    };
    BasefaceUI.prototype.getTexture = function (key) {
        return SheetManage.getTextureFromSheet(key, 'minihero_json');
    };
    BasefaceUI.prototype.tabChangeHandler = function (tb) {
    };
    return BasefaceUI;
}(UIBase));
BasefaceUI.NAME = 'BasefaceUI';
__reflect(BasefaceUI.prototype, "BasefaceUI");
//# sourceMappingURL=BasefaceUI.js.map