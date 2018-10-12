class MailVO {
	public id:number;
	public senderId:number;
	public senderName:string;
	public state:number;
	public constructor() {
	}

	public decode(data:any[]):void
	{
		this.id = data[0];
		this.senderId = data[1];
		this.senderName = data[2];
		this.state = data[3];
	}
}