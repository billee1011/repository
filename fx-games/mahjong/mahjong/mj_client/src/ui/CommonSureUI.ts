class CommonSureUI extends UIBase{
	public static NAME:string = 'CommonSureUI';
	public constructor() {
		super('resource/UI_exml/CommonSure.exml');
		this.centerFlag = true;
		this.isAloneShow = true;
		this.closeOther = false;
	}
	private btn_back:eui.Button;
	private label_content:eui.Label;
	private btn_sure:eui.Button;
	private btn_cancel:eui.Button;
	private backFun:Function;
	private backThisObj:any;
	private label_title:eui.Label;
	protected uiLoadComplete():void
	{
		CommonTool.addStroke(this.label_title,2,0x888888);
		
		this.btn_back.addEventListener(egret.TouchEvent.TOUCH_TAP,this.hide,this);
		this.btn_cancel.addEventListener(egret.TouchEvent.TOUCH_TAP,this.hide,this);
	}

	protected doExecute():void
	{
		if(this.paramstype == 'msg'){
			this.label_content.text = this.params;
			this.btn_cancel.visible = this.btn_sure.visible = false;
			this.btn_sure.removeEventListener(egret.TouchEvent.TOUCH_TAP,this.sureDoSomething,this);
		}else if(this.paramstype == 'buy')
		{
			var arr:any = this.params;
			this.label_content.text = arr[0];
			this.backFun = arr[1];
			this.backThisObj = arr[2];
			this.btn_cancel.visible = this.btn_sure.visible = true;
			this.btn_sure.x = 257;
			this.btn_sure.addEventListener(egret.TouchEvent.TOUCH_TAP,this.sureDoSomething,this);
		}
		else if(this.paramstype == 'noCancel')
		{
			var arr:any = this.params;
			this.label_content.text = arr[0];
			this.backFun = arr[1];
			this.backThisObj = arr[2];
			this.btn_cancel.visible = false 
			this.btn_sure.visible = true;
			this.btn_sure.x = 148;
			this.btn_sure.addEventListener(egret.TouchEvent.TOUCH_TAP,this.sureDoSomething,this);
		}
	}
	private sureDoSomething(evt:any):void
	{
		this.backFun.call(this.backThisObj);
		this.hide();
	}
}