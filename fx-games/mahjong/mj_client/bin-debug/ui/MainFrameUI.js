var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var MainFrameUI = (function (_super) {
    __extends(MainFrameUI, _super);
    function MainFrameUI() {
        return _super.call(this, 'resource/UI_exml/MainFrame.exml', 'mainFrame') || this;
    }
    MainFrameUI.prototype.bindEvent = function () {
        this._eventList[MjModel.PASSWORD_RIGHT] = [this.enterGame, this];
        this._eventList[MjModel.ENTER_ROOM_SUCC] = [this.sleep, this];
    };
    MainFrameUI.prototype.uiLoadComplete = function () {
        var bg = new AsyImage(0, 0); //
        this.addChildAt(bg, 0);
        bg.setUrl(PathDefine.UI_IMAGE + 'mainFrame.jpg');
        bg = new AsyImage();
        bg.x = 22;
        bg.y = 652;
        this.addChildAt(bg, 1);
        bg.setUrl(PathDefine.UI_IMAGE + 'main_diban.png');
        bg = new AsyImage();
        this.addChild(bg);
        bg.x = 430;
        bg.y = 586;
        bg.setUrl(PathDefine.UI_IMAGE + 'main_logo.png');
        this.img_head = new AsyImage();
        this.addChild(this.img_head);
        this.img_head.setScale(0.9, 0.9);
        this.img_head.updateXY(11, 12);
        this.img_enter = new AsyImage();
        this.addChild(this.img_enter);
        this.img_enter.updateXY(665, 126);
        this.img_enter.setUrl(PathDefine.UI_IMAGE + 'main_enterRoom.png');
        this.img_enter.addEventListener(egret.TouchEvent.TOUCH_TAP, this.enterRoomHandler, this);
        this.img_create = new AsyImage();
        this.addChild(this.img_create);
        this.img_create.updateXY(102, 126);
        this.img_create.setUrl(PathDefine.UI_IMAGE + 'main_createRoom.png');
        this.img_create.addEventListener(egret.TouchEvent.TOUCH_TAP, this.createRoomHandler, this);
        /*this.addChild(this.com_enterRoom);
        this.addChild(this.com_createRoom);

        this.btn_createRoom = new TButton(this.com_createRoom,'btn_createRoom');
        this.btn_createRoom.addBgIcon(this.getTexture('mf_btnbg'));
        this.btn_createRoom.addTextIcon(this.getTexture('mf_createzi'));
        this.btn_createRoom.skin.addEventListener(egret.TouchEvent.TOUCH_TAP,this.createRoomHandler,this);

        this.btn_enterRoom = new TButton(this.com_enterRoom,'btn_enterRoom');
        this.btn_enterRoom.addBgIcon(this.getTexture('mf_btnbg'));
        this.btn_enterRoom.addTextIcon(this.getTexture('mf_enterzi'));
        this.btn_enterRoom.skin.addEventListener(egret.TouchEvent.TOUCH_TAP,this.enterRoomHandler,this);*/
        this.btn_rule = new TButton(this.com_rule, 'btn_rule');
        this.btn_rule.addBgIcon(this.getTexture('main_rule'));
        this.btn_rule.skin.addEventListener(egret.TouchEvent.TOUCH_TAP, this.helpHandler, this);
        this.btn_message = new TButton(this.com_message, 'btn_message');
        this.btn_message.addBgIcon(this.getTexture('main_msg'));
        this.btn_message.skin.addEventListener(egret.TouchEvent.TOUCH_TAP, this.messageHandler, this);
        this.btn_setting = new TButton(this.com_setting, 'btn_setting');
        this.btn_setting.addBgIcon(this.getTexture('main_set'));
        this.btn_setting.skin.addEventListener(egret.TouchEvent.TOUCH_TAP, this.settingHandler, this);
        this.btn_add.addEventListener(egret.TouchEvent.TOUCH_TAP, this.showShop, this);
        this.roleVo = GlobalDefine.herovo;
        this.initRoleMsg();
        var shape = new egret.Shape();
        shape.graphics.beginFill(0, 1);
        shape.graphics.drawRect(0, 0, 577, 34);
        shape.x = 390;
        shape.y = 11;
        shape.graphics.endFill();
        this.addChild(shape);
        Tnotice.instance.playGonggao('浠水麻将666今日公测,欢迎品尝', true, shape);
    };
    MainFrameUI.prototype.initRoleMsg = function () {
        this.label_name.text = this.roleVo.name;
        this.label_diamond.text = this.roleVo.diamond.toString();
        this.img_head.setUrl(this.roleVo.head);
    };
    MainFrameUI.prototype.showShop = function (evt) {
        //	TFacade.toggleUI(ShopUI.NAME);
        //GlobalDefine.socket.simulateReceiveMsg(10016,[1,null,[1,99,[[5,1,2,true],[6,2,3,true]]]]);
        //	GlobalDefine.socket.simulateReceiveMsg(10019,[1,[1,2,'aaa',-6666,1,2,3,4,5],[2,3,'bbbb',7878,4,0,2,3,2],[2,4,'ccccc',7878,4,0,2,3,2],[2,5,'dddd',-878,4,0,2,3,2]]);
        SendOperate.instance.requestOperate(1);
    };
    MainFrameUI.prototype.enterGame = function () {
        TFacade.toggleUI(EnterRoomUI.NAME, 0);
        TFacade.toggleUI(MainGameUI.NAME, 1);
        Tnotice.instance.hideGonggao();
    };
    MainFrameUI.prototype.enterRoomHandler = function (evt) {
        /*if(GlobalDefine.gameState == 2){
            TFacade.toggleUI(MainGameUI.NAME,1);
            return;
        }*/
        TFacade.toggleUI(EnterRoomUI.NAME, 1);
    };
    MainFrameUI.prototype.createRoomHandler = function (evt) {
        TFacade.toggleUI(CreateRoomUI.NAME, 1);
    };
    MainFrameUI.prototype.helpHandler = function (evt) {
        //	this.doSelect(null);
        //	TFacade.toggleUI(HelpUI.NAME);
    };
    MainFrameUI.prototype.sleep = function () {
        Tnotice.instance.hideGonggao();
    };
    //测试选择照片
    /*private doSelect(evt:egret.TouchEvent):void {
        selectImage(this.selectedHandler,this);
    }
    private selectedHandler(thisRef:any,imgURL:string):void {
        RES.getResByUrl(imgURL,thisRef.compFunc,thisRef,RES.ResourceItem.TYPE_IMAGE);
    }
    private compFunc(texture:egret.Texture):void {
        var imgReview:egret.Bitmap = new egret.Bitmap(texture);
        imgReview.width = 300;
        imgReview.height = 300;
        this.addChild(imgReview);
    }*/
    //测试选择照片
    MainFrameUI.prototype.messageHandler = function (evt) {
        //	TFacade.toggleUI(EmailUI.NAME);
    };
    MainFrameUI.prototype.settingHandler = function (evt) {
        TFacade.toggleUI(SettingUI.NAME);
    };
    MainFrameUI.prototype.getTexture = function (name) {
        return SheetManage.getTextureFromSheet(name, 'mj_mainFrame_json');
    };
    return MainFrameUI;
}(UIBase));
MainFrameUI.NAME = 'MainFrameUI';
__reflect(MainFrameUI.prototype, "MainFrameUI");
//# sourceMappingURL=MainFrameUI.js.map