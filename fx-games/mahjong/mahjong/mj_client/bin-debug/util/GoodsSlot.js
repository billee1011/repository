var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var GoodsSlot = (function (_super) {
    __extends(GoodsSlot, _super);
    function GoodsSlot(w, h) {
        if (w === void 0) { w = 60; }
        if (h === void 0) { h = 60; }
        var _this = _super.call(this, '') || this;
        /**
         * 是否点击弹出物品tip
         */
        _this._tipable = false;
        _this.b_offX = 0;
        _this.b_offY = 0;
        _this._defaultSkin = new eui.Image();
        _this._defaultSkin.width = w;
        _this._defaultSkin.height = w;
        _this.addChild(_this._defaultSkin);
        _this.iconW = w;
        _this.rareSkin = new eui.Image();
        _this.rareSkin.width = w;
        _this.rareSkin.height = w;
        _this.addChild(_this.rareSkin);
        var t = 12;
        _this.img_icon = new AsyImage(w - t, w - t);
        _this.img_icon.x = t / 2;
        _this.img_icon.y = t / 2;
        _this.addChild(_this.img_icon);
        _this.label_count = new eui.Label();
        _this.label_count.size = 14;
        _this.label_count.fontFamily = '微软雅黑';
        _this.addChild(_this.label_count);
        CommonTool.addStroke(_this.label_count, 1, 0x000000);
        _this.label_count.touchEnabled = false;
        _this.label_name = new eui.Label();
        _this.addChild(_this.label_name);
        _this.label_name.y = w;
        //this.label_name.textAlign = 'center';
        _this.label_name.size = 16;
        _this.label_name.fontFamily = '微软雅黑';
        CommonTool.addStroke(_this.label_name, 1, 0x000000);
        return _this;
        //this._skin.touchChildren = this._skin.touchEnabled = false;
    }
    Object.defineProperty(GoodsSlot.prototype, "rarelevel", {
        set: function (v) {
            var r = ColorDefine.rareYinshe[v];
            // this.rareSkin.source = SheetManage.getTongyongTexture(r);
        },
        enumerable: true,
        configurable: true
    });
    GoodsSlot.prototype.dataChanged = function () {
        if (!this.data)
            return;
        this._vo = this.data;
        this.rarelevel = this.data.quality;
        var url = PathDefine.MJ_ICON + this.data.looks + '.png';
        this.img_icon.setUrl(url);
        this.count = this.data.count;
        if (this._vo.type == 25) {
            this.showEquipLevel();
        }
        this.doData();
    };
    GoodsSlot.prototype.doData = function () {
        //override
    };
    GoodsSlot.prototype.showEquipLevel = function () {
        this.label_count.text = 'lv.' + this._vo.level;
        this.label_count.x = this.iconW - this.label_count.width - 4;
        this.label_count.y = this.iconW - this.label_count.height - 2;
    };
    Object.defineProperty(GoodsSlot.prototype, "count", {
        set: function (value) {
            if (this._vo.type == 25) {
                return;
            }
            if (value > 1) {
                this.label_count.text = CommonTool.getMoneyShow(value);
                this.label_count.x = this.iconW - this.label_count.width - 4;
                this.label_count.y = this.iconW - this.label_count.height - 2;
            }
            else {
                this.label_count.text = '';
            }
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(GoodsSlot.prototype, "label", {
        set: function (value) {
            this.label_name.text = value;
            this.label_name.textColor = ColorDefine.rareColr[this.data.quality];
            this.label_name.x = (this.iconW - this.label_name.width) >> 1;
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(GoodsSlot.prototype, "tipable", {
        /*public set icontouch(value:boolean)
        {
            this.img_icon.touchEnabled = value;
        }*/
        set: function (value) {
            this._tipable = value;
            if (value) {
                this.img_icon.touchEnabled = true;
                this.img_icon.addEventListener(egret.TouchEvent.TOUCH_TAP, this.showTips, this);
            }
            else {
                this.img_icon.touchEnabled = false;
                this.img_icon.removeEventListener(egret.TouchEvent.TOUCH_TAP, this.showTips, this);
            }
        },
        enumerable: true,
        configurable: true
    });
    GoodsSlot.prototype.showTips = function (evt) {
    };
    Object.defineProperty(GoodsSlot.prototype, "badage", {
        set: function (v) {
            if (this._badage) {
                this._badage.visible = v;
            }
            else if (v) {
                this._badage = new eui.Image();
                this._badage.x = this.iconW + this.b_offX - 13;
                this._badage.y = this.b_offY - 6;
                //this._badage.texture = SheetManage.getTongyongTexture('redPoint');
                this.addChild(this._badage);
            }
        },
        enumerable: true,
        configurable: true
    });
    GoodsSlot.prototype.setBadgeOffset = function (ox, oy) {
        if (ox === void 0) { ox = 0; }
        if (oy === void 0) { oy = 0; }
        this.b_offX = ox;
        this.b_offY = oy;
    };
    return GoodsSlot;
}(RenderBase));
__reflect(GoodsSlot.prototype, "GoodsSlot");
//# sourceMappingURL=GoodsSlot.js.map