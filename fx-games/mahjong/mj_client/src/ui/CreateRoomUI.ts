class CreateRoomUI extends UIBase{
	public static NAME:string = 'CreateRoomUI';
	public constructor() {
		super('resource/UI_exml/CreateRoom.exml','createRoom');

		this.centerFlag = true;
		this.isAloneShow = true;
		this.closeOther = false;
	}
	private btn_sure:eui.Button;
	//private juList:any[];
	//private zuanList:any[];
	private curJu:number = 2;
	private curZuan:number = 1;

	private moshiList:HPagelist;
	private wanfaList:HPagelist;
	private qihuList:HPagelist;
	private fengdingList:HPagelist;
	private juList:HPagelist;
	private btn_close:eui.Button;
	protected uiLoadComplete():void
	{
		let temp:any[] = [4,8,16,32];
		let sb:CRSelectBox;
		let i:number = 0;

		this.moshiList = new HPagelist(CreateRoomRender,398,35,false);
		this.addChild(this.moshiList);
		this.moshiList.updateXY(183,109);
		this.moshiList.displayList([['开口番',1],['口口番',2]]);
		this.moshiList.selectAt(0);	

		this.wanfaList = new HPagelist(CreateRoomRender,398,50,true,2);
		this.addChild(this.wanfaList);
		this.wanfaList.updateXY(183,170);
		this.wanfaList.displayList([['红中赖子杠',1],['红中发财赖子杠',2],['七皮四赖杠',3],['红中七皮四赖杠',4],['原赖加番',5],['下大雨',6]]);
		this.wanfaList.selectAt(0);	

		this.qihuList = new HPagelist(CreateRoomRender,199,35,false);
		this.addChild(this.qihuList);
		this.qihuList.updateXY(183,324);
		this.qihuList.displayList([['无',1],['16倍起胡',2],['32倍起胡',3],['64倍起胡',4]]);
		this.qihuList.selectAt(0);	

		this.fengdingList = new HPagelist(CreateRoomRender,398,35,false);
		this.addChild(this.fengdingList);
		this.fengdingList.updateXY(183,373);
		this.fengdingList.displayList([['300倍',1],['500倍',2]]);
		this.fengdingList.selectAt(0);	

		this.juList = new HPagelist(CreateRoomRender,398,35,false);
		this.addChild(this.juList);
		this.juList.updateXY(183,480);
		this.juList.displayList([['4局(      ×2)',1,true],['8局(      ×8)',2,true]]);
		this.juList.selectAt(0);	

		this.btn_close.addEventListener(egret.TouchEvent.TOUCH_TAP,this.hide,this);

		/*for(i=0; i<temp.length; i++){
			let value:number = temp[i];
			sb = new CRSelectBox(value+'局');
			sb.data = value;
			sb.x = 174	+ i*(145);
			sb.y = 92;
			this.addChild(sb);
			this.juList.push(sb);
			sb.addEventListener(egret.TouchEvent.TOUCH_TAP,this.selectju,this);
			if(i == 1){
				sb.select = true;
				this.curJu = sb.data;
			}
		}

		temp = ['房主付费','AA付费'];
		this.zuanList = [];
		for(i=0; i<temp.length; i++){
			sb = new CRSelectBox(temp[i]);
			sb.data = i+1;
			sb.x = 174	+ i*(190);
			sb.y = 172;
			this.addChild(sb);
			this.zuanList.push(sb);
			sb.addEventListener(egret.TouchEvent.TOUCH_TAP,this.selectZuan,this);
			if(i == 0){
				sb.select = true;
				this.curZuan = sb.data;
			}
		}*/

		this.btn_sure.addEventListener(egret.TouchEvent.TOUCH_TAP,this.createSureHandler,this);
	}

	/*private selectju(evt:egret.Event):void
	{
		var cur:CRSelectBox = evt.currentTarget;
		cur.select = true;
		var d:number = cur.data;
		this.curJu = d;
		for (var key in this.juList) {
			cur = this.juList[key];
			if(cur.data != d){
				cur.select = false;
			}
		}
	}
	private selectZuan(evt:egret.Event):void
	{
		var cur:CRSelectBox = evt.currentTarget;
		cur.select = true;
		var d:number = cur.data;
		this.curZuan = d;
		for (var key in this.zuanList) {
			cur = this.zuanList[key];
			if(cur.data != d){
				cur.select = false;
			}
		}
	}*/

	private createSureHandler():void
	{
		this.hide();
		SendOperate.instance.requestCreateRoom(this.curJu,this.curZuan);
	}
}