var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var TFacade = (function () {
    function TFacade() {
    }
    /**
     * type: 0:关闭    1：打开  -1 自适应打开关闭
     * defalutLayer:默认打开到第一层面板，1、2选择
     * name: UIName;
     */
    TFacade.toggleUI = function (name, type, defalutLayer) {
        if (type === void 0) { type = -1; }
        if (defalutLayer === void 0) { defalutLayer = 1; }
        var instance = this.facade.getInstance(name);
        if (!instance) {
            if (type == 0) {
                return null;
            }
            var define = this.facade.getUIClass(name);
            instance = new define();
            this.facade.registerInstance(name, instance);
        }
        if (instance.stage) {
            if (type == 0 || type == -1) {
                DisplayUtil.removeDisplay(instance);
            }
        }
        else {
            if (type == 1 || type == -1) {
                if (this.facade.currentShowUI && instance.closeOther == true && this.facade.currentShowUI.stage && this.facade.currentShowUI.hideable) {
                    DisplayUtil.removeDisplay(this.facade.currentShowUI);
                }
                if (instance.closeOther == true) {
                    this.facade.currentShowUI = instance;
                }
                if (defalutLayer == 1 && !instance.isAloneShow) {
                    LayerManage.instance.panelChildLayer1.addChild(instance);
                }
                else {
                    LayerManage.instance.panelChildLayer2.addChild(instance);
                }
            }
        }
        return instance;
    };
    TFacade.getProxy = function (name) {
        var p = this.facade.getProxy(name);
        if (!p) {
            throw new Error("请先注册");
        }
        return p;
    };
    return TFacade;
}());
TFacade.facade = new Facade();
__reflect(TFacade.prototype, "TFacade");
//# sourceMappingURL=TFacade.js.map