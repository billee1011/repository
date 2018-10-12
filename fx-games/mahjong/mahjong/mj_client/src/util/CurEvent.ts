class CurEvent extends egret.Event{
	public curdir:number;
	public curstate:number;
	public item:any;
	public data:any;
	public constructor(type:string,item:any,data:any, bubbles:boolean=false, cancelable:boolean=false) {
		super(type,bubbles,cancelable);
		this.item = item;
		this.data = data
	}
}