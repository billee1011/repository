class SettingUI extends UIBase{
	public static NAME:string = 'SettingUI';
	public constructor() {
		super('resource/UI_exml/Setting.exml');

		this.centerFlag = true;
		this.isAloneShow = true;
		this.closeOther = false;
	}
	protected bindEvent():void
	{
	}
	private img_backTiao:eui.Image;
	private img_backHead:eui.Image;
	private img_backTiao0:eui.Image;
	private img_backHead0:eui.Image;
	private backBar:OperateBar;
	private yinxiaoBar:OperateBar;
	private img_yinxiaoBtn:eui.Image;
	private img_yinyueBtn:eui.Image;
	private _yinyueOn:boolean = true;
	private _yinxiaoOn:boolean = true;
	private curyinyueV:number=0;
	private curyinxiaoV:number=0;
	private btn_close:eui.Button;
	private btn_exchange:eui.Button;
	private img_quitZi:eui.Image;
	protected uiLoadComplete():void
	{
		this.backBar = new OperateBar(this.img_backTiao,this.img_backHead,this.soundBack,this);
		this.backBar.headOffX = 30;
		this.backBar.parent = this;
		this.backBar.setData(1);

		this.yinxiaoBar = new OperateBar(this.img_backTiao0,this.img_backHead0,this.yinxiaoBack,this);
		this.yinxiaoBar.headOffX = 30;
		this.yinxiaoBar.parent = this;
		this.yinxiaoBar.setData(1);

		this.img_yinyueBtn.source = this.getTexture('set_musicOn');
		this.img_yinxiaoBtn.source = this.getTexture('set_yinxiaoOn');
		this.img_yinyueBtn.addEventListener(egret.TouchEvent.TOUCH_TAP,this.yinyueTapHandler,this);
		this.img_yinxiaoBtn.addEventListener(egret.TouchEvent.TOUCH_TAP,this.yinxiaoTapHandler,this);

		this.btn_close.addEventListener(egret.TouchEvent.TOUCH_TAP,this.hide,this);

	//	this.setQuitEnabled(false);
	}

	private setQuitEnabled(bol:boolean):void
	{
		if(bol){
			this.btn_exchange.enabled = true;
			this.btn_exchange.filters = null;
			this.img_quitZi.filters = null;
		}else{
			this.btn_exchange.enabled = false;
			this.btn_exchange.filters = [CommonTool.grayFilter];
			this.img_quitZi.filters = [CommonTool.grayFilter];
		}
	}

	private yinyueTapHandler(evt:any):void
	{
		this.yinyueOn = !this.yinyueOn;
	}
	private yinxiaoTapHandler(evt:any):void
	{
		this.yinxiaoOn = !this.yinxiaoOn;
	}

	private get yinyueOn():boolean{
		return this._yinyueOn;
	}
	private set yinyueOn(v:boolean){
		this._yinyueOn = v;
		if(v){
			this.img_yinyueBtn.source = this.getTexture('set_musicOn');
			GlobalDefine.backSoundChannel.volume = this.curyinyueV;
		}else{
			this.img_yinyueBtn.source = this.getTexture('set_musicOff');
			GlobalDefine.backSoundChannel.volume = 0;
		}
	}
	private get yinxiaoOn():boolean{
		return this._yinxiaoOn;
	}
	private set yinxiaoOn(v:boolean){
		this._yinxiaoOn = v;
		if(v){
			this.img_yinxiaoBtn.source = this.getTexture('set_yinxiaoOn');
			GlobalDefine.playSoundVolume = this.curyinxiaoV;
		}else{
			this.img_yinxiaoBtn.source = this.getTexture('set_yinxiaoOff');
			GlobalDefine.playSoundVolume = 0;
		}
	}

	private soundBack(v:number):void
	{
		v = Math.min(v,1);
		v = Math.max(v,0);
		this.curyinyueV = v;
		if(!this.yinyueOn){
			v = 0;
		}
		GlobalDefine.backSoundChannel.volume = v;
	}
	private yinxiaoBack(v:number):void
	{
		v = Math.min(v,1);
		v = Math.max(v,0);
		this.curyinxiaoV = v;
		if(!this.yinxiaoOn){
			v = 0;
		}
		GlobalDefine.playSoundVolume = v;
	}
	private getTexture(name:string):egret.Texture
	{
		return SheetManage.getTextureFromSheet(name,'mj_setting_json');
	}
}