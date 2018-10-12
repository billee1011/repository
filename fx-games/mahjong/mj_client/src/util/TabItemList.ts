class TabItemList extends egret.Sprite{
	private tabArr:Array<TButton>;
	public static TAB_CHANGE:string = 'TABLIST_TAB_CHANGE';
	private _selectedIcon:eui.Image;
	private _offx:number;
	private _offy:number;
	public constructor() {
		super();
		this.tabArr = [];
	}
	public addItem(ti:TButton):void
	{
		this.tabArr.push(ti);
		this.addChild(ti.skin);
		this.addEventListener(egret.TouchEvent.TOUCH_TAP,this.touchHandler,this);
	}

	public setSelectedIcon(tex:egret.Texture,offx:number=0,offy:number=0):void
	{
		this._selectedIcon = new eui.Image();
		this._selectedIcon.source = tex;
		this.addChild(this._selectedIcon);
		this.setChildIndex(this._selectedIcon,0);
		this._offx = offx;
		this._offy = offy;
	}

	private touchHandler(evt:egret.Event):void
	{
		var ti:eui.Component = evt.target;
		if(!ti){
			return;
		}
		var item:TButton;
		for (var key in this.tabArr) {
			var element:TButton = this.tabArr[key];
			if(element.skin != ti){
				element.selected = false;
			}else{
				item = element;
			}
		}
		this.updateSelectIcon(item);
		item.selected = true;
		TFacade.facade.simpleDispatcher(TabItemList.TAB_CHANGE,item);
	}

	public get selectItem():TButton
	{
		for (var key in this.tabArr) {
			var element:TButton = this.tabArr[key];
			if(element.selected){
				return element;
			}
		}
		return null;
	}

	public set selectItem(tb:TButton)
	{
		tb.selected = true;
		for (var key in this.tabArr) {
			var element:TButton = this.tabArr[key];
			if(element != tb){
				element.selected = false;
			}
		}
		this.updateSelectIcon(tb);
		//EventControl.dispatchEvent(TabItemList.TAB_CHANGE,tb);
		TFacade.facade.simpleDispatcher(TabItemList.TAB_CHANGE,tb);
	}
	private updateSelectIcon(tb:TButton):void
	{
		if(this._selectedIcon){
			this._selectedIcon.x = tb.skin.x + this._offx;
			this._selectedIcon.y = tb.skin.y + this._offy;
		}
	}
	public selectAt(index:number):void
	{
		var item:TButton = this.tabArr[index];
		this.selectItem = item;
	}
}