class QueenVO {
	public pre:QueenVO;
	public next:QueenVO;
	public tips:string;
	public data:any;
	public constructor(tip:string=null) {
		this.tips = tip;
	}
}