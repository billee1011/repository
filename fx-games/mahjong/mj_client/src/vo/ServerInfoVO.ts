class ServerInfoVO {
	private _serverTime:number;
	public lastTimer:number;
	private serverDate:Date;
	public constructor() {
	}

	public get serverTime(): number {
		let temp:number = egret.getTimer()-this.lastTimer+this._serverTime;
	    return temp;
	}
	public set serverTime(value: number) {
		this.lastTimer = egret.getTimer();
	    this._serverTime = value;
		this.serverDate = new Date(this._serverTime);
	}
	
}