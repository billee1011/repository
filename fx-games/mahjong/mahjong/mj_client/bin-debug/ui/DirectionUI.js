var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var DirectionUI = (function (_super) {
    __extends(DirectionUI, _super);
    function DirectionUI() {
        return _super.call(this, 'resource/UI_exml/Direction.exml') || this;
    }
    DirectionUI.prototype.uiLoadComplete = function () {
        this.container = new egret.Sprite();
        this.container.addChild(this.img_bg);
        for (var i = 1; i <= 4; i++) {
            this['img_' + i].visible = false;
            this.container.addChild(this['img_' + i]);
        }
        this.rotationSp = new egret.Sprite();
        var w = this.img_bg.width / 2;
        var h = this.img_bg.height / 2;
        this.rotationSp.addChild(this.container);
        this.container.x = -w;
        this.container.y = -h;
        this.addChild(this.rotationSp);
        this.rotationSp.x = w;
        this.rotationSp.y = h;
        //this.rotationSp.graphics.beginFill(0xff0000,1);
        //this.rotationSp.graphics.drawCircle(0,0,70);
        //this.addEventListener(egret.TouchEvent.TOUCH_TAP,this.testHandler,this);
    };
    /*private testHandler(evt:any):void{
        this.setDirection(Math.ceil(Math.random()*4));
        this.playCardDirection(Math.ceil(Math.random()*4));
    }*/
    DirectionUI.prototype.bindEvent = function () {
        this.model = TFacade.getProxy(MjModel.NAME);
    };
    /***
     * 1 东
     * 2 南
     * 3 西
     * 4 北   之前做的是有东南西北字样的，所以用到了旋转
     */
    DirectionUI.prototype.setDirection = function (meDir) {
        switch (meDir) {
            case 1:
                this.rotationSp.rotation = 90;
                break;
            case 2:
                this.rotationSp.rotation = 180;
                break;
            case 3:
                this.rotationSp.rotation = -90;
                break;
            case 4:
                this.rotationSp.rotation = 0;
                break;
        }
    };
    /**
     * 出牌的玩家方向显示高亮
     */
    DirectionUI.prototype.playCardDirection = function (dir) {
        var img;
        for (var i = 1; i <= 4; i++) {
            img = this['img_' + i];
            img.visible = dir == i;
        }
    };
    return DirectionUI;
}(UIBase));
__reflect(DirectionUI.prototype, "DirectionUI");
//# sourceMappingURL=DirectionUI.js.map