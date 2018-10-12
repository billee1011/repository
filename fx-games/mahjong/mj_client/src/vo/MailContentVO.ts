class MailContentVO {
	public title:string;
	public senderId:number;
	public senderName:string;
	public content:string;
	public addTime:string;
	public diamond:number;
	public constructor() {
	}

	public decode(data:any[]):void
	{
		this.title = data[0];
		this.senderId = data[1];
		this.senderName = data[2];
		this.content = data[3];
		this.addTime = data[4];
		this.diamond = data[5];
	}
}