class DuiCardRender extends RenderBase{
	public constructor() {
		super('');
	}
	private icon:AsyImage;
	private model:MjModel;
	protected uiLoadComplete():void
	{
		this.model = TFacade.getProxy(MjModel.NAME);
		this.icon = new AsyImage(0,0,true);
		this.addChild(this.icon);
	}
	private cvo:CardVO;
	protected dataChanged():void
	{
		if(!this.data) {
			this.clear();
			return;
		}

		if(!this.icon){
			this.icon = new AsyImage(0,0,true);
			this.addChild(this.icon);
		}

		let vo:CardVO = this.data as CardVO;
		this.cvo = vo;
		let n:string;
		let p:number = this.model.getIconPByPosition(vo.position);
		let tt:number = vo.style == 4 ? vo.type+1-10 : vo.type;
		n = `p${p}s${vo.style}_${tt}`;
		this.icon.setUrl(n);

		
	}

	public setScale(sx:number,sy:number):void
	{
		if(this.icon){
			this.icon.scaleX = sx;
			this.icon.scaleY = sy;
		}
	}

	public clear():void
	{
		DisplayUtil.removeDisplay(this.icon);
		this.icon = null;
	}
}