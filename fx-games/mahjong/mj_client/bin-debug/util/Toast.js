var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
/**
 * Created by egret on 2016/1/26.
 */
var Toast = (function (_super) {
    __extends(Toast, _super);
    function Toast(msg) {
        var _this = _super.call(this) || this;
        _this.aimY = 0;
        console.log("Toast:", msg);
        var w = GlobalDefine.stageW;
        var h = GlobalDefine.stageH;
        var bg = new egret.Bitmap(Toast._txtrToastBg);
        _this.addChild(bg);
        var tx = new egret.TextField;
        tx.multiline = true;
        tx.size = 30;
        tx.bold = true;
        tx.textColor = 0xFFFFFF;
        tx.stroke = 2;
        tx.strokeColor = 0;
        tx.text = msg;
        tx.fontFamily = "微软雅黑";
        tx.textAlign = egret.HorizontalAlign.CENTER;
        tx.width = w * .84;
        tx.x = (Toast._txtrToastBg.textureWidth - tx.width) / 2;
        tx.y = 6;
        _this.addChild(tx);
        bg.height = 12 + tx.height;
        //  this.anchorOffsetX = this.width * .5;
        //this.anchorOffsetY = this.height * .5;
        _this.x = w * .5 - bg.width * .5;
        _this.y = h * .618;
        _this.alpha = 1;
        return _this;
    }
    Toast.init = function (txtrToastBg) {
        //  console.log( "Toast.init", txtrToastBg );
        this._txtrToastBg = txtrToastBg;
    };
    return Toast;
}(egret.DisplayObjectContainer));
__reflect(Toast.prototype, "Toast");
//# sourceMappingURL=Toast.js.map