var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var CurrencyUnit = (function () {
    function CurrencyUnit() {
        this.currencyList = {};
        this.currencyIconObj = {};
    }
    Object.defineProperty(CurrencyUnit, "instance", {
        get: function () {
            if (!this._instance) {
                this._instance = new CurrencyUnit();
            }
            return this._instance;
        },
        enumerable: true,
        configurable: true
    });
    /**
     * 0 元宝
    1 金币
    2 精力
    3 威望
    4 勇气
    5 战功
    6 熔炼值
     */
    CurrencyUnit.prototype.updateCurrency = function (type, value) {
        this.currencyList[type] = value;
    };
    CurrencyUnit.prototype.getCurrency = function (type) {
        var v = this.currencyList[type];
        if (!v) {
            v = 0;
        }
        return v;
    };
    CurrencyUnit.prototype.getGold = function () {
        var v = this.currencyList[ConstDefine.CURRENCY_GOLD];
        if (!v) {
            v = 0;
        }
        return v;
    };
    CurrencyUnit.prototype.getMoney = function () {
        var v = this.currencyList[ConstDefine.CURRENCY_MONEY];
        if (!v) {
            v = 0;
        }
        return v;
    };
    return CurrencyUnit;
}());
__reflect(CurrencyUnit.prototype, "CurrencyUnit");
//# sourceMappingURL=CurrencyUnit.js.map