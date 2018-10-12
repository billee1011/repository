/**
 * 监听添加/移除舞台事件
 */
class StageDele extends egret.Sprite{
	public constructor() {
		super();
		this.addEventListener(egret.Event.ADDED_TO_STAGE,this.toStage,this)
		this.addEventListener(egret.Event.REMOVED_FROM_STAGE,this.awayStage,this)
	}

	protected toStage(evt:any):void
	{

	}
	protected awayStage(evt:any):void
	{
		
	}
}