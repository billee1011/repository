class SettleCardRender extends RenderBase{
	public constructor() {
		super('');
	}
	private icon:AsyImage;
	protected uiLoadComplete():void
	{
		this.icon = new AsyImage(0,0,true);
		this.addChild(this.icon);
	}
	private cvo:CardVO;
	protected dataChanged():void
	{

		if(!this.data) return;

		if(!this.icon){
			this.icon = new AsyImage(0,0,true);
			this.addChild(this.icon);
		}

		let vo:CardVO = this.data as CardVO;
		this.cvo = vo;
		let n:string;
		let tt:number = vo.style == 4 ? vo.type+1-10 : vo.type;
		n = `p4s${vo.style}_${tt}`;
		this.icon.setUrl(n);

		this.icon.setScale(0.64,0.64);		
	}

	public clear():void
	{
		DisplayUtil.removeDisplay(this.icon);
		this.icon = null;
	}
}