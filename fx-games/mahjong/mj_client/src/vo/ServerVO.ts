class ServerVO {

	public serverid:number;
	public servername:string;
	public serverip:string;
	public port:number;
	/***
	 * 0：新服
	 * 1：热服
	 */
	public state:number;
	public constructor() {
	}
}