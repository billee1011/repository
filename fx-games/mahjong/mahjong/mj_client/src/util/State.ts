class State extends egret.DisplayObjectContainer {
	private obj:Object;
	private _state:string;
	public constructor() {
		super();
		this.obj = {};
	}

	public add(type:string,elements:Array<egret.DisplayObject>):void
	{
		this.obj[type] = elements;
	}
	public get state():string
	{
		return this._state;
	}
	public set state(type:string)
	{
		this._state = type;
		var arr:Array<egret.DisplayObject> = this.obj[type];
		if(!arr) return;
		this.switchDisplay(true,arr);
		for (var key in this.obj) {
			if(key != type){
				arr = this.obj[key];
				this.switchDisplay(false,arr);
			}
		}
	}
	private switchDisplay(show:boolean,arr:Array<egret.DisplayObject>):void
	{
		var dis:egret.DisplayObject;
		for (var key in arr) {
			dis = arr[key];
			if(show && dis.stage == null){
				this.addChild(dis);
			}else if(!show && dis.parent){
				DisplayUtil.removeDisplay(dis,dis.parent);
			}
		}
	}
}