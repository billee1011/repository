var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var TDecoder = (function () {
    function TDecoder() {
    }
    TDecoder.prototype.init = function (facade) {
        this.facade = facade;
        var num;
        for (var key in this.types) {
            num = this.types[key];
            //if (this.hasOwnProperty('f_'+num)) {
            facade.registerProtocol(num, [this['f_' + num], this]);
        }
    };
    TDecoder.prototype.checkSucc = function (arr) {
        if (arr[0] == 0) {
            var t = CodeDB.instance.getDes(arr[1]);
            t = t ? t : '不存在的code:' + arr[1];
            Tnotice.instance.popUpTip(t);
            return false;
        }
        return true;
    };
    return TDecoder;
}());
__reflect(TDecoder.prototype, "TDecoder");
//# sourceMappingURL=TDecoder.js.map