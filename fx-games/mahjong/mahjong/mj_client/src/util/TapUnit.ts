class TapUnit {
	public constructor() {
	}
	private dis:egret.DisplayObject;
	private w:number;
	private h:number;
	public bind(dis:egret.DisplayObject):void
	{
		this.dis = dis;
		this.w = dis.width;
		this.h = dis.height;
		dis.touchEnabled = true;
		dis.addEventListener(egret.TouchEvent.TOUCH_BEGIN,this.bigerHandler,this);
		dis.addEventListener(egret.TouchEvent.TOUCH_END,this.backHandler,this);
	}
	private bigerHandler(evt:any):void
	{
		this.dis.anchorOffsetX = 6;
		this.dis.anchorOffsetY = 6;
		this.dis.width = this.w+12;
		this.dis.height = this.h+12;
	}
	private backHandler(evt:any):void
	{
		this.dis.anchorOffsetX = 0;
		this.dis.anchorOffsetY = 0;
		this.dis.width = this.w;
		this.dis.height = this.h;
	}
}