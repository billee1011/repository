var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
/**
 * 加载moveiclip文件
 */
var EffectItem = (function (_super) {
    __extends(EffectItem, _super);
    function EffectItem(playOnce, mcname) {
        if (playOnce === void 0) { playOnce = false; }
        if (mcname === void 0) { mcname = 'main'; }
        var _this = _super.call(this) || this;
        _this.playOnce = playOnce;
        _this.mcname = mcname;
        return _this;
    }
    Object.defineProperty(EffectItem.prototype, "isSave", {
        get: function () {
            return this._isSave;
        },
        set: function (value) {
            this._isSave = value;
        },
        enumerable: true,
        configurable: true
    });
    /**
     * 不要后缀,二者名字最好相同
     */
    EffectItem.prototype.setUrl = function (png, json, isSave, path) {
        if (json === void 0) { json = null; }
        if (isSave === void 0) { isSave = false; }
        if (path === void 0) { path = PathDefine.EFFECT; }
        this.isSave = isSave;
        this.pngFlag = this.jsonFlag = false;
        if (!json) {
            json = png;
        }
        png += '.png';
        json += '.json';
        this.path = path + png;
        this.mc = ResourceCache.instance.getMcFromCache(this.path); //get cache
        if (this.mc) {
            this.addChild(this.mc);
            this.mc.gotoAndStop(1);
            if (this.playOnce) {
                this.mc.play(1);
                this.mc.addEventListener(egret.MovieClipEvent.COMPLETE, this.playComplete, this);
            }
            else {
                this.mc.play(-1);
            }
        }
        else {
            RES.getResByUrl(path + png, this.pngOver, this, RES.ResourceItem.TYPE_IMAGE);
            RES.getResByUrl(path + json, this.jsonOver, this, RES.ResourceItem.TYPE_JSON);
        }
        this.touchEnabled = false;
        this.addEventListener(egret.Event.REMOVED_FROM_STAGE, this.goAwayFromStage, this);
        this.addEventListener(egret.Event.ADDED_TO_STAGE, this.addToStage, this);
    };
    EffectItem.prototype.pngOver = function (data) {
        this.pngData = data;
        this.pngFlag = true;
        this.doubleOver();
    };
    EffectItem.prototype.jsonOver = function (data) {
        this.jsonData = data;
        this.jsonFlag = true;
        this.doubleOver();
    };
    EffectItem.prototype.doubleOver = function () {
        if (this.jsonFlag && this.pngFlag) {
            this.clear();
            var mcFactory = new egret.MovieClipDataFactory(this.jsonData, this.pngData);
            this.mc = new egret.MovieClip(mcFactory.generateMovieClipData(this.mcname));
            this.addChild(this.mc);
            if (this.playOnce) {
                this.mc.play(1);
                this.mc.addEventListener(egret.MovieClipEvent.COMPLETE, this.playComplete, this);
            }
            else {
                this.mc.play(-1);
            }
            if (this.isSave) {
                ResourceCache.instance.addMcToCache(this.path, this.mc);
            }
        }
    };
    EffectItem.prototype.clear = function () {
        if (!this.mc)
            return;
        this.mc.stop();
        DisplayUtil.removeDisplay(this.mc);
        this.mc = null;
    };
    EffectItem.prototype.playComplete = function (evt) {
        if (this.mc) {
            this.mc.removeEventListener(egret.MovieClipEvent.COMPLETE, this.playComplete, this);
            DisplayUtil.removeDisplay(this.mc);
            DisplayUtil.removeDisplay(this);
        }
    };
    EffectItem.prototype.setScale = function (c1, c2) {
        this.scaleX = c1;
        this.scaleY = c2;
    };
    EffectItem.prototype.updateXY = function (x1, y1) {
        this.x = x1;
        this.y = y1;
    };
    EffectItem.prototype.goAwayFromStage = function (evt) {
        if (this.mc) {
            this.mc.stop();
            DisplayUtil.removeDisplay(this.mc);
        }
        //this.jsonData = this.pngData = this.jsonFlag = this.pngFlag = false;
    };
    EffectItem.prototype.addToStage = function (evt) {
        if (this.mc) {
            if (!this.playOnce) {
                this.mc.play(-1);
            }
            this.addChild(this.mc);
        }
        ///this.jsonData = this.pngData = this.jsonFlag = this.pngFlag = false;
    };
    return EffectItem;
}(egret.DisplayObjectContainer));
__reflect(EffectItem.prototype, "EffectItem");
//# sourceMappingURL=EffectItem.js.map