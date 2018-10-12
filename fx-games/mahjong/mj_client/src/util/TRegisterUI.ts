class TRegisterUI {
	private facade:Facade;
	public constructor() {
		this.facade = TFacade.facade;
		this.reg(BasefaceUI.NAME,BasefaceUI);
		this.reg(DengluUI.NAME,DengluUI);
		this.reg(MainGameUI.NAME,MainGameUI);
		this.reg(MainFrameUI.NAME,MainFrameUI);
		this.reg(EnterRoomUI.NAME,EnterRoomUI);
		this.reg(OperateUI.NAME,OperateUI);
		this.reg(SettlementUI.NAME,SettlementUI);
		this.reg(SettingUI.NAME,SettingUI);
		this.reg(HelpUI.NAME,HelpUI);
		this.reg(GonggaoUI.NAME,GonggaoUI);
		this.reg(CreateRoomUI.NAME,CreateRoomUI);
		this.reg(ShopUI.NAME,ShopUI);
		this.reg(EmailUI.NAME,EmailUI);
		this.reg(CommonSureUI.NAME,CommonSureUI);
		this.reg(EatUI.NAME,EatUI);
		this.reg(GameOverUI.NAME,GameOverUI);
	}

	private reg(name:string,ui:any):void
	{
		this.facade.registerUI(name,ui);
	}
}