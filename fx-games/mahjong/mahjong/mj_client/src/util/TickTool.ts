class TickTool {
	private label:eui.Label;
	private type:number;
	private runing:boolean;
	private record:number;
	private overTime:number;
	public backFun:Function;
	public funThis:any;
	public constructor(label:eui.Label,type:number=1) {
		this.label = label;
		this.type = type;
		this.record = -1;
	}
	/**
	 * time：倒计时时间间隔 /毫秒
	 */
	public setOverTime(time:number):void
	{
		this.overTime = GlobalDefine.serverInfo.serverTime+time;
	}
	public startTick():void
	{
		if(!this.runing){
			this.record = egret.setInterval(this.tick,this,1000);
			this.runing = true;
			this.tick();
		}
	}

	private tick():void
	{
		var countDownTime:number =Math.ceil(this.overTime - GlobalDefine.serverInfo.serverTime);
		if(this.type == 1){
			this.label.text = TimeUtil.getCountDownTime(countDownTime);
		}
		if(countDownTime < 0){
			this.stopTick();
			if(this.backFun != null){
				this.label.text = '';
				this.backFun.call(this.funThis);
			}
		}
	}

	public stopTick():void
	{
		if(this.runing){
			egret.clearInterval(this.record);
			this.runing = false;
		}
	}
}