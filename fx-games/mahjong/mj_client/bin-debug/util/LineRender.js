var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
/**
 * 图标+属性+值（）
 */
var LineRender = (function (_super) {
    __extends(LineRender, _super);
    function LineRender(hasIcon) {
        if (hasIcon === void 0) { hasIcon = false; }
        var _this = _super.call(this, '') || this;
        _this.label_name = new eui.Label();
        _this.label_name.size = 18;
        _this.label_name.fontFamily = '微软雅黑';
        _this.addChild(_this.label_name);
        _this.label_name.x = 0;
        _this.label_name.y = 6;
        _this.label_name.textColor = 0xB68173;
        if (hasIcon) {
            _this.icon = new NoRareSlot(35, 35);
            _this.addChild(_this.icon);
            _this.label_name.x = 32;
            _this.icon.updateXY(0, -5);
        }
        _this.label_value = new eui.Label();
        _this.label_value.size = 16;
        _this.label_name.fontFamily = '微软雅黑';
        _this.label_value.textColor = 0xB68173;
        _this.addChild(_this.label_value);
        _this.label_value.y = 8;
        return _this;
    }
    /**                  type:百分比还是整型
     * data [[name,value,type],iconType]
     */
    LineRender.prototype.dataChanged = function () {
        var list = this.data;
        this.label_name.text = list[0][0] + ':';
        this.label_value.text = list[0][1];
        this.label_value.x = this.label_name.x + this.label_name.width + 4;
        if (list.length > 1) {
            this.showIcon(list[1]);
            this.label_name.text = '';
        }
    };
    LineRender.prototype.showIcon = function (type) {
        //override
    };
    return LineRender;
}(RenderBase));
__reflect(LineRender.prototype, "LineRender");
//# sourceMappingURL=LineRender.js.map