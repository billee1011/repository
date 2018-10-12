var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var LayerManage = (function () {
    function LayerManage() {
    }
    Object.defineProperty(LayerManage, "instance", {
        get: function () {
            if (!this._instance) {
                this._instance = new LayerManage();
            }
            return this._instance;
        },
        enumerable: true,
        configurable: true
    });
    LayerManage.prototype.initLayer = function (stage) {
        this.world2D = new egret.Sprite();
        stage.addChild(this.world2D);
        this.effectLayer = new egret.Sprite();
        this.world2D.addChild(this.effectLayer);
        this.panelLayer = new egret.Sprite();
        stage.addChild(this.panelLayer);
        this.panelChildLayer1 = new egret.Sprite();
        this.panelLayer.addChild(this.panelChildLayer1);
        this.panelChildLayer2 = new egret.Sprite();
        this.panelLayer.addChild(this.panelChildLayer2);
        this.tipLayer = new egret.Sprite();
        stage.addChild(this.tipLayer);
    };
    LayerManage.prototype.playGonggao = function (content, loop, mask) {
        if (loop === void 0) { loop = false; }
        if (mask === void 0) { mask = null; }
        //if(!this.notice){
        this.notice = new Notice();
        //}
        this.tipLayer.addChild(this.notice);
        if (mask) {
            var gp = mask.localToGlobal(0, 0);
            this.notice.x = gp.x;
            this.notice.y = gp.y + 5;
            this.notice.addChild(mask);
            mask.x = 0;
            mask.y = 0;
            this.notice.mask = mask;
        }
        else {
            this.notice.x = 0;
            this.notice.y = 3;
        }
        this.notice.play(content, loop, mask);
    };
    LayerManage.prototype.hideGonggao = function () {
        DisplayUtil.removeDisplay(this.notice);
    };
    LayerManage.prototype.showPanelMask = function (index) {
        if (!this.panelBgMask) {
            this.panelBgMask = new egret.Sprite();
            this.panelBgMask.touchEnabled = true;
            this.panelBgMask.addEventListener(egret.TouchEvent.TOUCH_TAP, this.maskClickHandler, this);
        }
        this.panelBgMask.graphics.clear();
        this.panelBgMask.graphics.beginFill(0, 0.7);
        this.panelBgMask.graphics.drawRect(0, 0, GlobalDefine.stageW, GlobalDefine.stageH);
        this.panelBgMask.graphics.endFill();
        if (!this.panelBgMask.stage) {
            this.panelChildLayer2.addChildAt(this.panelBgMask, index);
        }
        else {
            this.panelChildLayer2.addChildAt(this.panelBgMask, index - 1);
        }
    };
    LayerManage.prototype.hidePanelMask = function () {
        if (this.panelBgMask) {
            this.panelBgMask.graphics.clear();
            DisplayUtil.removeDisplay(this.panelBgMask);
        }
    };
    LayerManage.prototype.maskClickHandler = function (evt) {
        TFacade.facade.simpleDispatcher(ConstDefine.EMPTY_MASK_CLICK);
    };
    LayerManage.prototype.getUpDown = function () {
        if (!this._updown) {
            this._updown = new UpdownUtil('outmjtile_sign');
        }
        return this._updown;
    };
    return LayerManage;
}());
__reflect(LayerManage.prototype, "LayerManage");
//# sourceMappingURL=LayerManage.js.map