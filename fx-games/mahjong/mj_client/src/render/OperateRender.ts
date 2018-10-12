class OperateRender extends RenderBase{
	private img:AsyImage;
	public constructor() {
		super('');
	}
	private model:MjModel;
	protected uiLoadComplete():void
	{
		this.model = TFacade.getProxy(MjModel.NAME);
	}
	protected dataChanged(): void{

		if(!this.img){
			this.img = new AsyImage(0,0,true);
			this.addChild(this.img);
		}

		var opNum:number = this.data;
		this.img.getImg().source = this.model.getOperateTexture(opNum);
     }

	 public clear():void
	{
		DisplayUtil.removeDisplay(this.img);
		this.img = null;
	}
}