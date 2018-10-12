class TweenCard extends RenderBase{
	public constructor() {
		super('');
	}
	private icon:AsyImage;
	protected uiLoadComplete():void
	{
		this.icon = new AsyImage(0,0,true);
		this.addChild(this.icon);
	}
	protected dataChanged():void
	{
		if(!this.data) return;
		
		if(!this.icon){
			this.icon = new AsyImage(0,0,true);
			this.addChild(this.icon);
		}

		let vo:CardVO = this.data as CardVO;
		let n:string;
		let tt:number = vo.style == 4 ? vo.type+1-10 : vo.type;
		n = `p4s${vo.style}_${tt}`;
		this.icon.setUrl(n);
	}

	public clear():void
	{
		DisplayUtil.removeDisplay(this.icon);
		this.icon = null;
	}
}