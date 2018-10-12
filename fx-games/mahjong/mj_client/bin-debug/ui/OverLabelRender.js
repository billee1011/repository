var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var OverLabelRender = (function (_super) {
    __extends(OverLabelRender, _super);
    function OverLabelRender() {
        return _super.call(this, 'resource/UI_exml/OverLabelItem.exml') || this;
    }
    OverLabelRender.prototype.uiLoadComplete = function (evt) {
        if (evt === void 0) { evt = null; }
        //override
    };
    OverLabelRender.prototype.dataChanged = function () {
        var arr = this.data;
        var s = '';
        switch (arr[0]) {
            case 1:
                s = '自摸次数';
                break;
            case 2:
                s = '接炮次数';
                break;
            case 3:
                s = '点炮次数';
                break;
            case 4:
                s = '暗杠次数';
                break;
            case 5:
                s = '明杠次数';
                break;
        }
        s += '        ' + arr[1];
        this.label_msg.text = s;
    };
    return OverLabelRender;
}(RenderBase));
__reflect(OverLabelRender.prototype, "OverLabelRender");
//# sourceMappingURL=OverLabelRender.js.map