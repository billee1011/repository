var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
/**
 * 创建房间界面使用
 */
var CRSelectBox = (function (_super) {
    __extends(CRSelectBox, _super);
    function CRSelectBox(text) {
        var _this = _super.call(this) || this;
        var di = new eui.Image();
        _this.addChild(di);
        di.source = SheetManage.getTextureFromSheet('create_noselect', 'mj_createRoom_json');
        _this.gou = new eui.Image();
        _this.addChild(_this.gou);
        _this.gou.source = SheetManage.getTextureFromSheet('create_select', 'mj_createRoom_json');
        _this.gou.visible = false;
        var label = new eui.Label();
        label.size = 26;
        label.textColor = 0x8a6053;
        _this.addChild(label);
        label.fontFamily = 'Microsoft YaHei';
        label.text = text;
        label.x = 42;
        label.y = 30 - label.textHeight;
        return _this;
        //this.addEventListener(egret.TouchEvent.TOUCH_TAP,this.touchHandler,this);
    }
    Object.defineProperty(CRSelectBox.prototype, "select", {
        get: function () {
            return this._select;
        },
        /*private touchHandler(evt:egret.TouchEvent):void
        {
            this.select = !this._select;
        }
    */
        set: function (b) {
            this._select = b;
            this.gou.visible = b;
        },
        enumerable: true,
        configurable: true
    });
    return CRSelectBox;
}(egret.Sprite));
CRSelectBox.CHANGE = 'crbox_change';
__reflect(CRSelectBox.prototype, "CRSelectBox");
//# sourceMappingURL=CRSelectBox.js.map