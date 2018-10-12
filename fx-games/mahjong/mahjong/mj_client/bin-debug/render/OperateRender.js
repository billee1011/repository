var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var OperateRender = (function (_super) {
    __extends(OperateRender, _super);
    function OperateRender() {
        return _super.call(this, '') || this;
    }
    OperateRender.prototype.uiLoadComplete = function () {
        this.model = TFacade.getProxy(MjModel.NAME);
    };
    OperateRender.prototype.dataChanged = function () {
        if (!this.img) {
            this.img = new AsyImage(0, 0, true);
            this.addChild(this.img);
        }
        var opNum = this.data;
        this.img.getImg().source = this.model.getOperateTexture(opNum);
    };
    OperateRender.prototype.clear = function () {
        DisplayUtil.removeDisplay(this.img);
        this.img = null;
    };
    return OperateRender;
}(RenderBase));
__reflect(OperateRender.prototype, "OperateRender");
//# sourceMappingURL=OperateRender.js.map