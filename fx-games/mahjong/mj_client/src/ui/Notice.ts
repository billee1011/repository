class Notice extends egret.Sprite {
	//private bg:egret.Bitmap;
	public label:eui.Label;
	private tw:egret.Tween;
	public constructor() {
		super();
		/*this.bg = new egret.Bitmap(RES.getRes("toast-bg_png"));
		this.addChild(this.bg);
		this.bg.width = GlobalDefine.stageW;
		this.bg.height = 22;*/

		this.label = new eui.Label();
		this.label.size = 24;
		//this.label.textColor = 0xFFFF00;
		this.label.fontFamily = 'Microsoft YaHei';
		this.addChild(this.label);

		this.addEventListener(egret.Event.REMOVED_FROM_STAGE,this.removeHandler,this)
	}
	private loop:boolean;
	private masks:egret.Shape;
	public play(content:string,loop:boolean=false,mask:egret.Shape=null):void
	{
		this.masks = mask;
		this.loop = loop;
		if(this.tw){
			egret.Tween.removeTweens(this.label);
		}
		this.label.text = content;
		this.doPlay();
	}
	private playEnd():void
	{
		if(this.loop){
			this.doPlay();
		}else{
			DisplayUtil.removeDisplay(this);
		}
	}

	private doPlay():void
	{
		this.tw = egret.Tween.get( this.label );
		this.label.x = this.masks ? this.masks.x + this.masks.width : GlobalDefine.stageW;
		let aimX:number = this.masks ? this.masks.x-this.label.width-30 : -this.label.width-30;
		this.tw.to({x:aimX},20000)
		this.tw.call(this.playEnd,this);
	}

	private removeHandler(evt:any):void
	{
		egret.Tween.removeTweens(this.label);
		this.tw = null;
	}
	
}