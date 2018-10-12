var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var TRegisterUI = (function () {
    function TRegisterUI() {
        this.facade = TFacade.facade;
        this.reg(BasefaceUI.NAME, BasefaceUI);
        this.reg(DengluUI.NAME, DengluUI);
        this.reg(MainGameUI.NAME, MainGameUI);
        this.reg(MainFrameUI.NAME, MainFrameUI);
        this.reg(EnterRoomUI.NAME, EnterRoomUI);
        this.reg(OperateUI.NAME, OperateUI);
        this.reg(SettlementUI.NAME, SettlementUI);
        this.reg(SettingUI.NAME, SettingUI);
        this.reg(HelpUI.NAME, HelpUI);
        this.reg(GonggaoUI.NAME, GonggaoUI);
        this.reg(CreateRoomUI.NAME, CreateRoomUI);
        this.reg(ShopUI.NAME, ShopUI);
        this.reg(EmailUI.NAME, EmailUI);
        this.reg(CommonSureUI.NAME, CommonSureUI);
        this.reg(EatUI.NAME, EatUI);
        this.reg(GameOverUI.NAME, GameOverUI);
    }
    TRegisterUI.prototype.reg = function (name, ui) {
        this.facade.registerUI(name, ui);
    };
    return TRegisterUI;
}());
__reflect(TRegisterUI.prototype, "TRegisterUI");
//# sourceMappingURL=TRegisterUI.js.map