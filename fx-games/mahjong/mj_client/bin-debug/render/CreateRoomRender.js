var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var CreateRoomRender = (function (_super) {
    __extends(CreateRoomRender, _super);
    function CreateRoomRender() {
        return _super.call(this, '') || this;
    }
    CreateRoomRender.prototype.uiLoadComplete = function () {
    };
    /**
     * data ['xxxxx',value]
     */
    CreateRoomRender.prototype.dataChanged = function () {
        var vs = this.data;
        this.ck = new CRSelectBox(vs[0]);
        this.addChild(this.ck);
        if (vs[2]) {
            var img = new eui.Image;
            img.x = 96;
            img.y = -6;
            img.source = SheetManage.getTextureFromSheet('main_ka', 'mj_mainFrame_json');
            this.addChild(img);
        }
    };
    CreateRoomRender.prototype.doChoose = function () {
        this.ck.gou.visible = this.choosed;
    };
    return CreateRoomRender;
}(RenderBase));
__reflect(CreateRoomRender.prototype, "CreateRoomRender");
//# sourceMappingURL=CreateRoomRender.js.map