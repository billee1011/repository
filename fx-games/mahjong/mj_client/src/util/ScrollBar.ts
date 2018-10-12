class ScrollBar extends eui.Scroller{
	public constructor(list_target:eui.IViewport) {
		super();
		this.viewport = list_target;
		this.x = list_target.x;
		this.y = list_target.y;
		this.width = list_target.width+5;
		this.height = list_target.height;
	}
}