var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var DengluUI = (function (_super) {
    __extends(DengluUI, _super);
    function DengluUI() {
        var _this = _super.call(this, 'resource/UI_exml/Denglu.exml') || this;
        _this.centerFlag = true;
        //this.isAloneShow = true;
        _this.closeOther = false;
        return _this;
    }
    DengluUI.prototype.bindEvent = function () {
        this._eventList[MjModel.LOGIN_SUCC] = [this.loginSuccBack, this];
    };
    DengluUI.prototype.backGroundClick = function () {
    };
    /*public childrenCreated() {
        super.childrenCreated();
        this.left = 0;
        this.right = 0;
        this.top = 0;
        this.bottom = 0;
    }*/
    DengluUI.prototype.testLay = function () {
        //this.bg.getImg().height = GlobalDefine.stageH;
    };
    DengluUI.prototype.uiLoadComplete = function () {
        this.bg = new AsyImage(0, 0); //
        this.addChildAt(this.bg, 0);
        this.bg.setUrl(PathDefine.UI_IMAGE + 'login_bg.jpg', this.testLay, this);
        var timg = new eui.Image();
        this.addChild(timg);
        timg.source = PathDefine.UI_IMAGE + 'logo.png';
        timg.x = 100;
        timg.y = 84;
        //bg.fullAll();
        /*bg = new AsyImage();
        this.addChild(bg);
        bg.updateXY(266,130);
        bg.setUrl(PathDefine.UI_IMAGE+'logo.png');*/
        /*var test:AsyImage = new AsyImage();
        this.addChild(test);
        test.setUrl('p4b3_1');
        test.getImg().left = 200;*/
        /*	var cc:eui.Image = new eui.Image();
            this.addChild(cc);
            cc.source = PathDefine.UI_IMAGE+'login_bg.jpg';
            cc.height = GlobalDefine.stageH;
            cc.left = 0;
            cc.right = 0;
            cc.top= 0;
            cc.bottom = 0;*/
        //console.log(cc.parent,cc.parent.parent);
        /*test.setUrl('p4b4_1');
        test.setUrl('p4s1_5');*/
        /*var shape:egret.Shape = new egret.Shape();
        shape.graphics.beginFill(0);
        shape.graphics.drawRect(0,0,50,50);

        var c:eui.Component = new eui.Component();
        c.addChild(shape); //c.width = 0

        var d:egret.DisplayObjectContainer = new egret.DisplayObjectContainer();
        d.addChild(shape); //c.width = 50;*/
        this.img_webchat = new AsyImage();
        this.addChild(this.img_webchat);
        this.img_webchat.updateXY(440, 460);
        this.img_webchat.setUrl(PathDefine.UI_IMAGE + 'btn_weixin.png');
        this.img_webchat.addEventListener(egret.TouchEvent.TOUCH_TAP, this.startHandler, this);
        /*let con:egret.DisplayObjectContainer = new egret.DisplayObjectContainer();
        con.addChild(this.img_ckBg);
        con.addChild(this.label_xieyi);
        this.ck_box = new SelectBox(con,this.img_ckHead,true);*/
        //	this.addChild(this.label_xieyi)
        this.state = new State();
        this.addChild(this.state);
        this.state.add('login', [this.img_webchat]);
        this.state.add('loading', [this.img_loadBg, this.img_loadTiao, this.img_zi, this.label_loadMsg]);
        this.state.state = 'login';
        this.pb = new ProgressBar(this.img_loadTiao);
        //Tnotice.instance.playGonggao('抵制不良游戏，拒绝盗版游戏。注意自我保护，谨防受骗上当。适度游戏益脑，沉迷游戏伤身。合理安排时间，享受健康生活',true);
        /*	let sp:egret.Sprite = new egret.Sprite();
            this.addChild(sp);
            sp.touchEnabled = true;
            sp.graphics.beginFill(0x00ff00,1);
            sp.graphics.drawRect(0,100,20,20);
            sp.addEventListener(egret.TouchEvent.TOUCH_TAP,this.test,this);*/
        /*var opT:OperateEffectItem = new OperateEffectItem();
        this.addChild(opT);
        opT.updateXY(100,100);
        opT.playEffect(1);*/
    };
    /*private testlay():void
    {
        this.img_webchat.y = GlobalDefine.stageH-this.img_webchat.height;
    }*/
    DengluUI.prototype.startHandler = function (evt) {
        if (!GlobalDefine.WEB_SOCKE_COLLECTED) {
            return;
        }
        //登录
        SendOperate.instance.requestLogin();
        //this.loginSuccBack();
        if (!GlobalDefine.backSoundChannel) {
            var sound = RES.getRes('bgm2_mp3');
            GlobalDefine.backSoundChannel = sound.play(0, 0);
        }
    };
    DengluUI.prototype.loginSuccBack = function () {
        RES.addEventListener(RES.ResourceEvent.GROUP_COMPLETE, this.onResourceLoadComplete, this);
        RES.addEventListener(RES.ResourceEvent.GROUP_PROGRESS, this.onResourceProgress, this);
        RES.createGroup('gameR', ['mainFrame', 'gameRes']);
        RES.loadGroup("gameR");
        this.state.state = 'loading';
    };
    DengluUI.prototype.onResourceLoadComplete = function (evt) {
        RES.removeEventListener(RES.ResourceEvent.GROUP_COMPLETE, this.onResourceLoadComplete, this);
        RES.removeEventListener(RES.ResourceEvent.GROUP_PROGRESS, this.onResourceProgress, this);
        TFacade.toggleUI(DengluUI.NAME, 0);
        //TFacade.toggleUI(MainGameUI.NAME,1);
        TFacade.toggleUI(MainFrameUI.NAME, 1);
    };
    DengluUI.prototype.onResourceProgress = function (event) {
        this.pb.setData(event.itemsLoaded, event.itemsTotal);
        var per = Math.round(event.itemsLoaded / event.itemsTotal * 100);
        this.label_loadMsg.text = per + "%";
        //	this.img_shaizi.x = this.img_loadTiao.x + this.pb.getRectWidth()-15;
    };
    DengluUI.prototype.sleep = function () {
        Tnotice._instance.hideGonggao();
    };
    return DengluUI;
}(UIBase));
DengluUI.NAME = 'DengluUI';
__reflect(DengluUI.prototype, "DengluUI");
//# sourceMappingURL=DengluUI.js.map