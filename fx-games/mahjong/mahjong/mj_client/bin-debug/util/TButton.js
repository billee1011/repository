var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var TButton = (function () {
    function TButton(skin, name, isToggleBtn) {
        if (name === void 0) { name = ''; }
        if (isToggleBtn === void 0) { isToggleBtn = false; }
        this.b_offX = 0;
        this.b_offY = 0;
        this.skin = skin;
        this.name = name;
        this.skin.name = name; //事件的target是skin 而不是Tbutton
        this.isToggleBtn = isToggleBtn;
        this.diBm = new egret.Bitmap();
        this.skin.addChildAt(this.diBm, 0);
        this.diBm.width = skin.width;
        this.diBm.height = skin.height;
        /*if(this.isToggleBtn){
            skin.addEventListener(egret.TouchEvent.TOUCH_TAP,this.clicked,this,false,999);
        }*/
    }
    /**
     * 相对右上角的偏移量
     */
    TButton.prototype.setBadgeOffset = function (ox, oy) {
        if (ox === void 0) { ox = 0; }
        if (oy === void 0) { oy = 0; }
        this.b_offX = ox;
        this.b_offY = oy;
    };
    Object.defineProperty(TButton.prototype, "badage", {
        set: function (v) {
            if (this._badage) {
                this._badage.visible = v;
            }
            else if (v) {
                this._badage = new eui.Image();
                this._badage.x = this.skin.width + this.b_offX - 13;
                this._badage.y = this.b_offY - 5;
                //this._badage.texture = SheetManage.getTongyongTexture('redPoint');
                this.skin.addChild(this._badage);
            }
        },
        enumerable: true,
        configurable: true
    });
    /*private clicked(evt:egret.TouchEvent):void
    {
        if(!this.selected){
            this.selected = true;
        }
    }*/
    /**
     * 如果skin为component则添加
     */
    TButton.prototype.addBgIcon = function (d1, d2) {
        if (d2 === void 0) { d2 = null; }
        this.d1 = d1;
        this.d2 = d2;
        this.diBm.texture = d1;
        if (this.isToggleBtn && !d2) {
            this.d2 = this.d1;
        }
    };
    Object.defineProperty(TButton.prototype, "texture", {
        set: function (tex) {
            this.diBm.texture = tex;
        },
        enumerable: true,
        configurable: true
    });
    TButton.prototype.addTextIcon = function (t1, t2) {
        if (t2 === void 0) { t2 = null; }
        if (!this.ziBm) {
            this.ziBm = new egret.Bitmap();
            this.skin.addChild(this.ziBm);
        }
        this.tex1 = t1;
        this.tex2 = t2;
        this.ziBm.texture = t1;
        this.ziBm.x = (this.skin.width - this.ziBm.width) >> 1;
        this.ziBm.y = (this.skin.height - this.ziBm.height) >> 1;
    };
    Object.defineProperty(TButton.prototype, "selected", {
        get: function () {
            return this._selected;
        },
        set: function (bol) {
            if (!this.isToggleBtn)
                return;
            this._selected = bol;
            /*if(bol){
            }*/
            this.diBm.texture = bol ? this.d2 : this.d1;
            if (this.tex2)
                this.ziBm.texture = bol ? this.tex2 : this.tex1;
        },
        enumerable: true,
        configurable: true
    });
    return TButton;
}());
__reflect(TButton.prototype, "TButton");
//# sourceMappingURL=TButton.js.map