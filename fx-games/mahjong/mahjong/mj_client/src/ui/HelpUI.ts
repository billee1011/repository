class HelpUI extends UIBase{
	public static NAME:string = 'HelpUI';
	public constructor() {
		super('resource/UI_exml/Help.exml');

		this.centerFlag = true;
		this.isAloneShow = true;
		this.closeOther = false;
	}
	private list_target:eui.List;
	private plist:PageList;
	private sb_target:ScrollBar;
	protected uiLoadComplete():void
	{
		this.sb_target = new ScrollBar(this.list_target)
		this.addChild(this.sb_target);

		this.plist = new PageList(this.list_target,HelpRender);

		this.plist.displayList(['啊发顺丰福建省付款as发骚发骚发骚撒发顺丰发杀神风啊啊算法沙发上发as发生发发啊发顺丰福建省付款as发骚发骚发骚撒发顺丰发杀神风啊啊算法沙发上发as发生发发啊发顺丰福建省付款as发骚发骚发骚撒发顺丰发杀神风啊啊算法沙发上发as发生发发啊发顺丰福建省付款as发骚发骚发骚撒发顺丰发杀神风啊啊算法沙发上发as发生发发啊发顺丰福建省付款as发骚发骚发骚撒发顺丰发杀神风啊啊算法沙发上发as发生发发啊发顺丰福建省付款as发骚发骚发骚撒发顺丰发杀神风啊啊算法沙发上发as发生发发啊发顺丰福建省付款as发骚发骚发骚撒发顺丰发杀神风啊啊算法沙发上发as发生发发啊发顺丰福建省付款as发骚发骚发骚撒发顺丰发杀神风啊啊算法沙发上发as发生发发啊发顺丰福建省付款as发骚发骚发骚撒发顺丰发杀神风啊啊算法沙发上发as发生发发啊发顺丰福建省付款as发骚发骚发骚撒发顺丰发杀神风啊啊算法沙发上发as发生发发啊发顺丰福建省付款as发骚发骚发骚撒发顺丰发杀神风啊啊算法沙发上发as发生发发']);
	}
}