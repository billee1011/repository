class SelectBox extends egret.EventDispatcher{
	private _select:boolean;
	private gou:egret.DisplayObject;
	public data:any;
	public static CHANGE:string = 'selectbox_change';
	public constructor(di:egret.DisplayObject,gou:egret.DisplayObject,select:boolean=true,data:any=null) {
		super();
		di.touchEnabled = true;
		this.gou = gou;
		this.gou.touchEnabled = false;
		this._select = select;
		gou.visible = select;
		this.data = data;
		di.addEventListener(egret.TouchEvent.TOUCH_TAP,this.touchHandler,this);
	}

	private touchHandler(evt:egret.TouchEvent):void
	{
		this.select = !this._select;
	}

	public set select(b:boolean)
	{
		this._select = b;
		this.gou.visible = b;
		this.dispatchEvent(new egret.Event(SelectBox.CHANGE));
	}

	public get select():boolean
	{
		return this._select;
	}
}