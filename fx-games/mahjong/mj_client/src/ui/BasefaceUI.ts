class BasefaceUI extends UIBase
{
    public static NAME:string = 'BasefaceUI';
    public constructor(){
        super('resource/UI_exml/BaseFace.exml');
        this.hideable = false;
    }
    private btn_bag:eui.Button;
    private btn_role:eui.Button;
    private btn_shop:eui.Button;
    private btn_battle:eui.Button;
    private btn_baozhilin:eui.Button;
    private btn_more:eui.Button;

    private tabList:TabItemList;

    private image_ditu:eui.Image;

    protected bindEvent():void
    {
        this._eventList[TabItemList.TAB_CHANGE] = [this.tabChangeHandler,this];
    }

    protected uiLoadComplete(evt:eui.UIEvent):void
    {

        this.tabList = new TabItemList();
        this.addChild(this.tabList);
        var temp:TButton;
        temp = new TButton(this.btn_role,'btn_role');
        this.tabList.addItem(temp);

        temp = new TButton(this.btn_bag,'btn_bag');
        this.tabList.addItem(temp);
       // temp.setBadgeOffset(-5,10);

        temp = new TButton(this.btn_baozhilin,'btn_baozhilin');
        this.tabList.addItem(temp);

        temp = new TButton(this.btn_battle,'btn_battle');
        this.tabList.addItem(temp);
        

        temp = new TButton(this.btn_shop,'btn_shop');
        this.tabList.addItem(temp);

        temp = new TButton(this.btn_more,'btn_more');
        this.tabList.addItem(temp);


       this.y = GlobalDefine.stage.stageHeight - this.height + 2;
    }

     private getTexture(key:string):egret.Texture
	{
		return SheetManage.getTextureFromSheet(key,'minihero_json');
	}

    private tabChangeHandler(tb:TButton):void
    {
        
    }

}