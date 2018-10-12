var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var AsyImage = (function (_super) {
    __extends(AsyImage, _super);
    function AsyImage(w, h, isSave) {
        if (w === void 0) { w = 0; }
        if (h === void 0) { h = 0; }
        if (isSave === void 0) { isSave = true; }
        var _this = _super.call(this) || this;
        _this._img = new eui.Image();
        _this.addChild(_this._img);
        if (w)
            _this._img.width = w;
        if (h)
            _this._img.height = h;
        _this.isSave = isSave;
        return _this;
    }
    /**
     * 默认在resource的Icon下面，如果不是补全路劲
     */
    AsyImage.prototype.setUrl = function (url, backFun, thisObj) {
        if (backFun === void 0) { backFun = null; }
        if (thisObj === void 0) { thisObj = null; }
        url = url.toString();
        this.backFun = backFun;
        this.backObj = thisObj;
        if (url.indexOf('.') == -1) {
            url = PathDefine.MJ_ICON + url + '.png';
        }
        var tex = ResourceCache.instance.getImageFromCache(url);
        if (tex) {
            this._img.source = tex;
            this.checkCircleMask();
            if (this.backFun != null) {
                this.backFun.call(this.backObj);
            }
            return;
        }
        RES.getResByUrl(url, this.getSucc, this, RES.ResourceItem.TYPE_IMAGE);
    };
    Object.defineProperty(AsyImage.prototype, "width", {
        get: function () {
            return this._img.width;
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(AsyImage.prototype, "height", {
        get: function () {
            return this._img.height;
        },
        enumerable: true,
        configurable: true
    });
    AsyImage.prototype.setScale = function (sx, sy) {
        this._img.scaleX = sx;
        this._img.scaleY = sy;
    };
    AsyImage.prototype.getImg = function () {
        return this._img;
    };
    AsyImage.prototype.clear = function () {
        this._img.source = null;
        //this._img
    };
    AsyImage.prototype.fullAll = function () {
        this._img.left = 0;
        this._img.right = 0;
        this._img.top = 0;
        this._img.bottom = 0;
    };
    AsyImage.prototype.getSucc = function (data, url) {
        if (this.isSave) {
            ResourceCache.instance.addImageToCache(url, data);
        }
        this._img.source = data;
        this.checkCircleMask();
        if (this.backFun != null) {
            this.backFun.call(this.backObj);
        }
    };
    AsyImage.prototype.checkCircleMask = function () {
        if (!this.circleMask)
            return;
        if (!this.maskShape) {
            this.maskShape = new egret.Shape();
        }
        this.maskShape.graphics.clear();
        this.maskShape.graphics.beginFill(0, 1);
        this.maskShape.graphics.lineStyle(1, 0);
        var hh = this._img.width * .5;
        this.maskShape.graphics.drawCircle(hh, hh, hh);
        this.maskShape.graphics.endFill();
        this.addChild(this.maskShape);
        this.mask = this.maskShape;
    };
    AsyImage.prototype.updateXY = function (xx, yy) {
        this.x = xx;
        this.y = yy;
    };
    return AsyImage;
}(egret.DisplayObjectContainer));
__reflect(AsyImage.prototype, "AsyImage");
//# sourceMappingURL=AsyImage.js.map