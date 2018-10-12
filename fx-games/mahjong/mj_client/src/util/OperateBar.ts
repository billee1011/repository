class OperateBar {
	private bar:egret.DisplayObject;
	private rect:egret.Rectangle;
	private len:number;
	private head:egret.DisplayObject;
	public headOffX:number = 0;
	private backFun:Function;
	private thisObj:any;
	public parent:egret.DisplayObjectContainer;
	public constructor(bar:egret.DisplayObject,head:egret.DisplayObject,backFun:Function,thisObj:any) {
		this.bar = bar;
		this.rect = new egret.Rectangle(0,0,0,bar.height);
		this.len = this.bar.width;
		this.head = head;
		this.backFun = backFun;
		this.thisObj = thisObj;
		head.addEventListener(egret.TouchEvent.TOUCH_BEGIN,this.touchBegin,this);
	}
	private touchBegin(evt:any):void
	{
		GlobalDefine.stage.addEventListener(egret.TouchEvent.TOUCH_MOVE,this.touchMove,this);
		GlobalDefine.stage.addEventListener(egret.TouchEvent.TOUCH_END,this.touchEnd,this);
	}
	private touchEnd(evt:any):void
	{
		GlobalDefine.stage.removeEventListener(egret.TouchEvent.TOUCH_MOVE,this.touchMove,this);
		GlobalDefine.stage.removeEventListener(egret.TouchEvent.TOUCH_END,this.touchEnd,this);
	}
	private touchMove(evt:egret.TouchEvent):void
	{
		if(!this.parent) return;
		let h:egret.DisplayObject = this.head;
		var gx:number = this.parent.globalToLocal(evt.stageX,evt.stageY).x;
		h.x = gx-this.headOffX;
		if(h.x + this.headOffX < this.bar.x){
			h.x = this.bar.x - this.headOffX;
		}
		if(h.x + this.headOffX > this.bar.x + this.len){
			h.x = this.bar.x + this.len - this.headOffX;
		}
		var a:number = (this.head.x+this.headOffX-this.bar.x)/this.len;
		a = Math.abs(a);
		this.setData(a);
		if(this.backFun != null){
			this.backFun.call(this.thisObj,CommonTool.getKeepNum(a,1));
		}
	}

	public setData(a):void
	{
		this.rect.width = Math.floor(a*this.len)
		this.bar.scrollRect = this.rect; 
		if(a == 1)
			this.head.x = this.bar.x + this.len - this.headOffX;
	}
}