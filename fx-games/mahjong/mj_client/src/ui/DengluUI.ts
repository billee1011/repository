class DengluUI extends UIBase{
	public static NAME:string = 'DengluUI';
	public constructor() {
		super('resource/UI_exml/Denglu.exml');
		this.centerFlag = true;
		//this.isAloneShow = true;
		this.closeOther = false;
	}

	protected bindEvent():void
	{
		this._eventList[MjModel.LOGIN_SUCC] = [this.loginSuccBack,this];
	}
	protected backGroundClick():void
    {
    }
	private img_webchat:AsyImage;
	private ck_box:SelectBox;
	//private img_ckBg:eui.Image;
	//private img_ckHead:eui.Image;
	//private label_xieyi:eui.Label;
	private img_loadBg:eui.Image;
	private img_loadTiao:eui.Image;
	//private img_shaizi:eui.Image;
	private label_loadMsg:eui.Label;
	private state:State;
	private pb:ProgressBar;
	private img_zi:eui.Image;
	/*public childrenCreated() {
        super.childrenCreated();
        this.left = 0;
        this.right = 0;
        this.top = 0;
        this.bottom = 0;
    }*/
	private testLay():void
	{
		//this.bg.getImg().height = GlobalDefine.stageH;
	}
	private bg:AsyImage;
	protected uiLoadComplete():void
	{
		
		this.bg = new AsyImage(0,0); //
		this.addChildAt(this.bg,0);
		this.bg.setUrl(PathDefine.UI_IMAGE+'login_bg.jpg',this.testLay,this);

		let timg:eui.Image = new eui.Image();
		this.addChild(timg);
		timg.source = PathDefine.UI_IMAGE+'logo.png';
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
		this.img_webchat.updateXY(440,460);
		this.img_webchat.setUrl(PathDefine.UI_IMAGE+'btn_weixin.png');
		this.img_webchat.addEventListener(egret.TouchEvent.TOUCH_TAP,this.startHandler,this);

		/*let con:egret.DisplayObjectContainer = new egret.DisplayObjectContainer();
		con.addChild(this.img_ckBg);
		con.addChild(this.label_xieyi);
		this.ck_box = new SelectBox(con,this.img_ckHead,true);*/

	//	this.addChild(this.label_xieyi)

		this.state = new State();
		this.addChild(this.state);

		this.state.add('login',[this.img_webchat]);
		this.state.add('loading',[this.img_loadBg,this.img_loadTiao,this.img_zi,this.label_loadMsg]);

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
	}

	/*private testlay():void
	{
		this.img_webchat.y = GlobalDefine.stageH-this.img_webchat.height;
	}*/

	private startHandler(evt:any):void
	{
		if(!GlobalDefine.WEB_SOCKE_COLLECTED){
			return;
		}
		//登录
		SendOperate.instance.requestLogin();

		//this.loginSuccBack();
		if(!GlobalDefine.backSoundChannel){
			let sound:egret.Sound = RES.getRes('bgm2_mp3');
			GlobalDefine.backSoundChannel = sound.play(0,0);
		}
	}

	private loginSuccBack():void
	{
		RES.addEventListener(RES.ResourceEvent.GROUP_COMPLETE, this.onResourceLoadComplete, this);
        RES.addEventListener(RES.ResourceEvent.GROUP_PROGRESS, this.onResourceProgress, this);
		RES.createGroup('gameR',['mainFrame','gameRes']);
        RES.loadGroup("gameR")
		this.state.state = 'loading';

	}

	private onResourceLoadComplete(evt:any):void
	{
		RES.removeEventListener(RES.ResourceEvent.GROUP_COMPLETE, this.onResourceLoadComplete, this);
        RES.removeEventListener(RES.ResourceEvent.GROUP_PROGRESS, this.onResourceProgress, this);
		TFacade.toggleUI(DengluUI.NAME,0);
		//TFacade.toggleUI(MainGameUI.NAME,1);
		TFacade.toggleUI(MainFrameUI.NAME,1);
	}

	private onResourceProgress(event:RES.ResourceEvent):void {
		this.pb.setData(event.itemsLoaded,event.itemsTotal);
		var per:number = Math.round(event.itemsLoaded/event.itemsTotal*100);
		this.label_loadMsg.text = `${per}%`;
	//	this.img_shaizi.x = this.img_loadTiao.x + this.pb.getRectWidth()-15;
    }
	public sleep():void
	{
		Tnotice._instance.hideGonggao();
	}
	/*private loginStart(evt:egret.TouchEvent):void
	{
		var user:string = this.label_username.text;
		var pass:string = this.label_password.text;
		if(user == ''){
			Tnotice.instance.popUpTip('请输入用户名')
			return;
		}
		if(pass == ''){
			Tnotice.instance.popUpTip('请输入密码')
			return;
		}

		var str:string = `{"user":"${user}","pass":"${pass}"}`;
		//CookieManager.instance.writeCookie('lastLogin',str,1);
	}*/

	/*var record:string = document.cookie;
		if(record){
			var obj:any = JSON.parse(record);
			this.label_password.text = obj.pass;
			this.label_username.text = obj.user;
		}*/

		//this.btn_register.addEventListener(egret.TouchEvent.TOUCH_TAP,this.registerHandler,this);
}