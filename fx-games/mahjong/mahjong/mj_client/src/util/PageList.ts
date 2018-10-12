class PageList {
	public ac:eui.ArrayCollection;
	public list:eui.List;
	private renderObj:Object;
	private centerLay:number;
	private offset:number;
	private outIndex:number;
	public constructor(list:eui.List,render:any) {
		this.ac = new eui.ArrayCollection();
		this.list = list;
		this.list.itemRenderer = render;
	}

	public addItem(data:any):void
	{
		this.ac.addItem(data);
		this.list.dataProvider = this.ac;
	}

	public displayList(data:any[],centerLay:number=0,offset:number=0):void
	{
		this.ac.source = data;
		this.list.dataProvider = this.ac;
		
		if(centerLay > 0){
			this.centerLay = centerLay;
			this.offset = offset;
			this.outIndex = egret.setTimeout(this.layout,this,100);
		}
	}
	private layout():void
	{
		egret.clearTimeout(this.outIndex);
		this.list.x = this.centerLay - this.list.contentWidth*.5 + this.offset;
	}
	/**
	 * eui.list 本身就是局部刷新，可以不使用此方法，直接refreshAll
	 */
	public setRenderObj(key:any):void
	{
		this.renderObj = {};
		var rb:RenderBase;
		for(var i:number=0; i<this.list.numElements; i++)
		{
			rb = this.list.getElementAt(i) as RenderBase;
			if(rb){
				var data:any = rb.data;
				this.renderObj[data[key]] = rb;
			}
		}
	}
	public updateChange():void
	{
		this.list.dataProvider = this.ac;
	}
	/*public addItemToAC(data:any):void
	{
		this.ac.addItem(data);
		//this.list.dataProvider = this.ac;
	}*/
	/**
	 * eui.list 本身就是局部刷新，可以不使用此方法，直接refreshAll
	 */
	public singleRefresh(key:any):void
	{
		var rb:RenderBase = this.renderObj[key];
		if(rb){
			rb.refreshRender();
		}
	}
	public refreshAll():void
	{
		var rb:RenderBase;
		for(var i:number=0; i<this.list.numElements; i++)
		{
			rb = this.list.getElementAt(i) as RenderBase;
			if(rb){
				rb.refreshRender();
			}
		}
	}
}