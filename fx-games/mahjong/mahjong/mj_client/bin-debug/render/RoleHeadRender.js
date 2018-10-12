var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var RoleHeadRender = (function (_super) {
    __extends(RoleHeadRender, _super);
    function RoleHeadRender() {
        return _super.call(this, 'resource/UI_exml/RoleItem.exml') || this;
    }
    RoleHeadRender.prototype.uiLoadComplete = function () {
        this.bg = new AsyImage(81, 79);
        this.addChild(this.bg);
        this.bg.updateXY(25, 11);
        this.addChild(this.label_name);
        this.readyPic = new eui.Image();
        this.readyPic.source = SheetManage.getTextureFromCenter('ready_sign');
        this.addChild(this.readyPic);
    };
    RoleHeadRender.prototype.dataChanged = function () {
        this.vo = this.data;
        this.bg.setUrl(PathDefine.UI_IMAGE + this.vo.head + '.png');
        this.label_name.text = this.vo.name;
        this.label_id.text = this.vo.roleId.toString();
    };
    return RoleHeadRender;
}(RenderBase));
__reflect(RoleHeadRender.prototype, "RoleHeadRender");
//# sourceMappingURL=RoleHeadRender.js.map