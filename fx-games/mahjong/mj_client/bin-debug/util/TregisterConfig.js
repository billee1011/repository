var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var TregisterConfig = (function () {
    function TregisterConfig() {
        this.doinitConfig();
    }
    //private test:string = '';
    TregisterConfig.prototype.doinitConfig = function () {
        /*egret.log(typeof this.test);
        var obj:any = {"id":100}
        this.test = obj.id;
        egret.log(typeof this.test);*/
        //egret.log(PropertyDefine.instance.getPropertyName('rebound_proportion'));
        //egret.log(PropertyDefine.instance.getPropertyType('rebound_proportion'));
        /*var loader:egret.URLLoader = new egret.URLLoader();
        loader.dataFormat = egret.URLLoaderDataFormat.TEXTURE;
        loader.addEventListener(egret.Event.COMPLETE,this.okFun,this);
        loader.load(new egret.URLRequest('D:/workspace/23.png'));*/
    };
    return TregisterConfig;
}());
__reflect(TregisterConfig.prototype, "TregisterConfig");
//# sourceMappingURL=TregisterConfig.js.map