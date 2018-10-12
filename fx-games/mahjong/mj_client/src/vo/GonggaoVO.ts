class GonggaoVO {
	public id:number;
	public content:string;
	public interval:number;
	public beginTime:string;
	public endTime:string;
	public constructor() {
	}

	public decode(data:any[]):void
	{
		this.id = data[0];
		this.content = data[1];
		this.interval = data[2];
		this.beginTime = data[3];
		this.endTime = data[4];
	}
}