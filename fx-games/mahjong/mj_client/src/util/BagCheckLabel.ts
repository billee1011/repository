class BagCheckLabel extends egret.DisplayObjectContainer{
	private label:eui.Label;
	private id:number;
	private aim:number;
	public constructor(label:eui.Label,align:string='left') {
		super();
		if(!label){
			this.label = new eui.Label();
			this.label.size = 16;
			this.label.fontFamily = '微软雅黑';
			this.label.textAlign = align;
		}else{
			this.label = label;
		}
		this.label.x = 0;
		this.label.y = 0; 
		this.addChild(this.label);
		this.addEventListener(egret.Event.ADDED_TO_STAGE,this.toStageHandler,this);
		this.addEventListener(egret.Event.REMOVED_FROM_STAGE,this.leaveStageHandler,this);
	}
	public updateXY(xx:number,yy:number):void
	{
		this.x = xx;
		this.y = yy;
	}

	private toStageHandler(evt:any):void
	{
	}

	private leaveStageHandler(evt:any):void
	{
	}

	private update():void
	{
		if(!this.id){
			return;
		}
		// var gproxy:GoodsProxy = TFacade.getProxy(GoodsProxy.NAME);
		// var have:number = gproxy.getCountByItemId(this.id);
		// this.label.text = `${have}/${this.aim}`;
		// this.label.textColor = have >= this.aim ? 0x00FF00 : 0xFF0000;
	}

	public setData(id:number,aimCount:number)
	{
		this.id = id;
		this.aim = aimCount;
		this.update();
	}	
}