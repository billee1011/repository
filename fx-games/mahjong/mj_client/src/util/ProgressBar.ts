class ProgressBar {
	private bar:egret.DisplayObject;
	private label_v:eui.Label;
	private rect:egret.Rectangle;
	private len:number;
	public constructor(bar:egret.DisplayObject,label:eui.Label=null) {
		this.bar = bar;
		this.label_v = label;
		this.rect = new egret.Rectangle(0,0,0,bar.height);
		this.len = this.bar.width;
	}
	public setData(cur:number,total:number):void
	{
		if(this.label_v){
			this.label_v.text = cur + '/' + total;
		}
		this.rect.width = Math.floor(cur/total*this.len)
		this.bar.scrollRect = this.rect; 
	}
	public getRectWidth():number
	{
		return this.rect.width;
	}
}