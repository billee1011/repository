class HPagelist extends egret.Sprite{
	public static RENDER_CHANGE:string = 'pagelist_render_change';
	private cls:any;
	private rwidth:number;
	private rheight:number;
	/**
	 *是不是水平排列 
		* true 先横排再竖排  false：先竖排再横排
		*/		
	private isHor:Boolean;
	private horNum:number;
	private dataList:any[];
	/**
	 * -1 为反向排列(hor=false时使用较好)
	 */
	public layoutDirX:number = 1;
	/**
	 * -1 为反向排列(hor=true时使用较好)
	 */
	public layoutDirY:number = 1;
	/**
	 * 是否从下往上排列   ()
	 */
	public downUp:boolean;
	/**
	 * 是否从右往左排   ()
	 */
	public rightLeft:boolean;

	public gap:number = 0;
	private len:number;

	public currentItem:RenderBase;
	public constructor(render:any,renderWidth:number,renderHeight:number,hor:Boolean=true,horNum:number=1)
	{
		super();
		this.touchEnabled = true;
		this.cls = render;
		this.rwidth = renderWidth;
		this.rheight = renderHeight;
		this.isHor = hor;
		this.horNum = horNum;
	}
	public displayList(data:any[]):void
	{
		this.len = data ? data.length : 0;
		var render:RenderBase;
		for(let key in this.dataList) 
		{
			render = this.dataList[key];
			//render.removeAllEvent();
			render.removeEventListener(egret.TouchEvent.TOUCH_TAP,this.renderClickHandler,this);
			render.clear();
			DisplayUtil.removeDisplay(render);
			CachePool.getInstance().reBack(render);
		}
		if(this.dataList){
			this.dataList.length = 0;
		}else{
			this.dataList = [];
		}
		for (var i:number = 0; i < this.len; i++) 
		{
			var value:any = data[i]; 
			render = CachePool.getInstance().getObject(this.cls) as RenderBase;
			render.data = value;
			this.arrange(render as egret.DisplayObject,i);
			render.addEventListener(egret.TouchEvent.TOUCH_TAP,this.renderClickHandler,this);
			this.dataList.push(render);
		}
	}
	
	public getAllItem():any[]
	{
		return this.dataList;
	}
	
	public refreshAll():void
	{
		var render:RenderBase;
		for (let key in this.dataList) 
		{
			render = this.dataList[key];
			render.refreshRender();
		}
	}
	public refreshOther():void
	{
		var render:RenderBase;
		for (let key in this.dataList) 
		{
			render = this.dataList[key];
			render.refreshOther();
		}
	}
	protected renderClickHandler(event:egret.TouchEvent):void
	{
		var render:RenderBase = event.currentTarget as RenderBase;
		
		this.selectItem(render);
	}
	public selectItem(render:RenderBase):void
	{
		this.currentItem = render;
		render.choosed = true;
		this.simpleEvent(render);
		var temp:RenderBase;
		for (var key in this.dataList) 
		{
			temp = this.dataList[key];
			if(temp != render && temp.choosed){
				temp.choosed = false;
				break;
			}
		}
	}	
	/**
	 *派发事件 
		* @param render
		* 
		*/		
	private simpleEvent(render:RenderBase):void
	{
		//EventControl.dispatchEvent(HPagelist.RENDER_CHANGE,obj);
		this.dispatchEvent(new CurEvent(HPagelist.RENDER_CHANGE,render,render.data));
	}
	
	public selectAt(index:number):void
	{
		var render:RenderBase = this.dataList[index];
		this.selectItem(render);
	}
	
	/*public selectItem(render:RenderBase):void
	{
		render.selected = true;
		this.simpleEvent(render);
		var temp:RenderBase;
		for (let key in this.dataList) 
		{
			temp = this.dataList[key];
			if(temp != render && temp.selected){
				temp.selected = false;
				break;
			}
		}
	}	*/	
	
	private arrange(dis:egret.DisplayObject, i:number):void
	{
		if(this.downUp){
			this.addChildAt(dis,0);
		}else
			this.addChild(dis);
		var a:number = Math.floor(i % this.horNum);
		var b:number = Math.floor(i / this.horNum);
		if(this.isHor) //先横排再竖排
		{
			if(this.rightLeft)
			{
				a = this.horNum - a;
			}
			dis.x = a * (this.rwidth+this.gap)*this.layoutDirX;
			dis.y = b * (this.rheight+this.gap)*this.layoutDirY;
			if(this.layoutDirY == -1){
				this.setChildIndex(dis,a);
			}
		}  
		else  //先竖排再横排
		{
			dis.x = b * (this.rwidth+this.gap)*this.layoutDirX;
			if(this.downUp)
			{	
				a = this.horNum - a;
			}
			dis.y = a * (this.rheight+this.gap)*this.layoutDirY;
		}
	}
	public updateXY(x:number,y:number):void
	{
		this.x = x;
		this.y = y;
	}
}