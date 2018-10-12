class EatRender extends RenderBase{
	public constructor() {
		super('');
	}
	private list:HPagelist;
	protected uiLoadComplete(evt:eui.UIEvent=null):void
    {
        //override
		this.list = new HPagelist(EatCardRender,55,84,false);
		this.addChild(this.list);
    }

    protected dataChanged(): void{
		this.list.displayList(this.data);
     }

}