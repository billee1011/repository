var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var SelectBox = (function (_super) {
    __extends(SelectBox, _super);
    function SelectBox(di, gou, select, data) {
        if (select === void 0) { select = true; }
        if (data === void 0) { data = null; }
        var _this = _super.call(this) || this;
        di.touchEnabled = true;
        _this.gou = gou;
        _this.gou.touchEnabled = false;
        _this._select = select;
        gou.visible = select;
        _this.data = data;
        di.addEventListener(egret.TouchEvent.TOUCH_TAP, _this.touchHandler, _this);
        return _this;
    }
    SelectBox.prototype.touchHandler = function (evt) {
        this.select = !this._select;
    };
    Object.defineProperty(SelectBox.prototype, "select", {
        get: function () {
            return this._select;
        },
        set: function (b) {
            this._select = b;
            this.gou.visible = b;
            this.dispatchEvent(new egret.Event(SelectBox.CHANGE));
        },
        enumerable: true,
        configurable: true
    });
    return SelectBox;
}(egret.EventDispatcher));
SelectBox.CHANGE = 'selectbox_change';
__reflect(SelectBox.prototype, "SelectBox");
//# sourceMappingURL=SelectBox.js.map