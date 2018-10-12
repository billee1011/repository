var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var SheetManage = (function () {
    function SheetManage() {
    }
    /**
  * 根据name关键字创建一个Bitmap对象。此name 是根据TexturePacker 组合成的一张位图
  */
    SheetManage.createBitmapFromSheet = function (name, sheetName) {
        var sheet = RES.getRes(sheetName);
        var texture = sheet.getTexture(name);
        var result = new egret.Bitmap();
        result.texture = texture;
        return result;
    };
    /**
     * sheetName 组名
     */
    SheetManage.getTextureFromSheet = function (name, sheetName) {
        var sheet = RES.getRes(sheetName);
        var result = sheet.getTexture(name);
        return result;
    };
    SheetManage.getTextureFromCenter = function (name) {
        return SheetManage.getTextureFromSheet(name, 'mj_center_json');
    };
    SheetManage.getTextureFromOperate = function (name) {
        return SheetManage.getTextureFromSheet(name, 'mj_operate_json');
    };
    return SheetManage;
}());
__reflect(SheetManage.prototype, "SheetManage");
//# sourceMappingURL=SheetManage.js.map