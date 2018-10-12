class OperateUI extends UIBase{
	public static NAME:string = 'OperateUI';
	public constructor() {
		super('resource/UI_exml/Operate.exml');
		//this.centerFlag = true;
		//this.isAloneShow = true;
	}
	private list:HPagelist
	protected bindEvent():void
	{
		
	}
	protected uiLoadComplete():void
	{
		this.y = GlobalDefine.stageH*.5 + 100;
		this.list = new HPagelist(OperateRender,91,100,false);
		this.addChild(this.list);
		this.list.addEventListener(HPagelist.RENDER_CHANGE,this.listChange,this);

	}
	private listChange(data:CurEvent):void
	{
		var opNum:number = data.data;
		this.hide();
		if(opNum == 8){ //吃牌
			TFacade.toggleUI(EatUI.NAME,1).execute('eat');
		}else{
			/*if(opNum == 5){
				Tnotice.instance.popUpTip('你胡了，是的,胡了，别玩了');
				return;
			}*/
			SendOperate.instance.requestOperate(opNum);
		}
	}
	protected doExecute():void
	{
		let temp:any[] = this.params;
		temp.push(7);
		this.list.displayList(temp);

		this.x = GlobalDefine.stageW - this.width - 400;
	}
}