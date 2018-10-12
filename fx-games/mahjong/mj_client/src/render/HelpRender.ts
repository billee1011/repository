class HelpRender extends RenderBase{
	public constructor() {
		super('');
	}
	private label:eui.Label;
	private img:AsyImage;
	protected uiLoadComplete():void
	{
		this.label = new eui.Label();
		this.addChild(this.label);
		this.label.size = 30;
		//this.label.height = 422;
		this.label.width = 444;
		this.label.wordWrap = true;

		this.img = new AsyImage();
		this.addChild(this.img);
		this.img.touchEnabled = true;
	}
	 protected dataChanged(): void{
		// this.label.text = this.data;
		this.img.setUrl(PathDefine.UI_IMAGE+'testHelp.png',this.loadOver,this);
     }
	 private loadOver():void
	 {
		 this.height = this.img.height;
	 }

}