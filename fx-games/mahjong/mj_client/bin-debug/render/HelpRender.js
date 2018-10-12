var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var HelpRender = (function (_super) {
    __extends(HelpRender, _super);
    function HelpRender() {
        return _super.call(this, '') || this;
    }
    HelpRender.prototype.uiLoadComplete = function () {
        this.label = new eui.Label();
        this.addChild(this.label);
        this.label.size = 30;
        //this.label.height = 422;
        this.label.width = 444;
        this.label.wordWrap = true;
        this.img = new AsyImage();
        this.addChild(this.img);
        this.img.touchEnabled = true;
    };
    HelpRender.prototype.dataChanged = function () {
        // this.label.text = this.data;
        this.img.setUrl(PathDefine.UI_IMAGE + 'testHelp.png', this.loadOver, this);
    };
    HelpRender.prototype.loadOver = function () {
        this.height = this.img.height;
    };
    return HelpRender;
}(RenderBase));
__reflect(HelpRender.prototype, "HelpRender");
//# sourceMappingURL=HelpRender.js.map