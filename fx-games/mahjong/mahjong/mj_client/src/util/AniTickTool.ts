class AniTickTool {
	private label:AniLabel;
	private type:number;
	private runing:boolean;
	private record:number;
	private overTime:number;
	public backFun:Function;
	public funThis:any;
	private extendLabel:AniLabel;
	private conditionTime:number;
	private centerX:number;
	private centerY:number;
	/**
	 * label 美术字
	 * extendLabel 替代的美术字
	 * conditionTime 剩余多少时间的时候用替代的美术字
	 */
	public constructor(label:AniLabel,extendLabel:AniLabel,conditionTime:number,centerX:number=-1,centerY:number=-1) {
		this.label = label;
		this.record = -1;
		this.extendLabel = extendLabel;
		if(extendLabel)
			this.extendLabel.visible = false;
		this.conditionTime = conditionTime;
		this.centerX = centerX;
		this.centerY = centerY;
	}
	/**
	 * time：倒计时时间间隔 /毫秒
	 */
	public setOverTime(time:number):void
	{
		this.overTime = egret.getTimer()+time;
		this.label.visible = true;
		if(this.extendLabel)
			this.extendLabel.visible = false;
	}
	public startTick(time:number=-1):void
	{
		if(time != -1){
			this.setOverTime(time);
		}
		if(!this.runing){
			this.record = egret.setInterval(this.tick,this,1000);
			this.runing = true;
			this.tick();
		}
	}

	private tick():void
	{
		let countDownTime:number =Math.ceil(this.overTime - egret.getTimer());
		let current:AniLabel; 
		if(this.extendLabel == null || countDownTime > this.conditionTime){
			this.label.text = Math.round(countDownTime/1000).toString();
			current = this.label;
		}else{
			this.label.visible = false;
			this.extendLabel.visible = true;
			this.extendLabel.text = Math.floor(countDownTime/1000).toString();
			current = this.extendLabel;
		}
		if(this.centerX != -1){
			current.x = this.centerX - current.width*.5;
		}

		if(countDownTime < 0){
			this.stopTick();

			if(this.extendLabel){
				this.extendLabel.text = '0';
			}
			if(this.backFun != null){
				//this.label.text = '';
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