var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var Facade = (function () {
    //private groupObj:Object;
    function Facade() {
        this.classObj = {};
        this.instanceObj = {};
        this.proxyObj = {};
        this.protocolObj = {};
        this.listenerObj = {};
        //this.groupObj = {};
    }
    //public getGroup
    /**
     * 注册UI类名
     */
    Facade.prototype.registerUI = function (name, cls) {
        this.classObj[name] = cls;
    };
    /**
     * 取UI类名
     */
    Facade.prototype.getUIClass = function (name) {
        var t = this.classObj[name];
        if (t == null) {
            throw new Error("请先注册UI");
        }
        return this.classObj[name];
    };
    /**
     * 注册UI实例
     */
    Facade.prototype.registerInstance = function (name, cls) {
        this.instanceObj[name] = cls;
    };
    /**
     * 取UI实例
     */
    Facade.prototype.getInstance = function (name) {
        return this.instanceObj[name];
    };
    /**
     * 注册prxoy
     */
    Facade.prototype.registerProxy = function (name, cls) {
        var proxy = this.proxyObj[name];
        if (!proxy) {
            proxy = new cls;
        }
        else {
            throw new Error("重复注册Proxy");
        }
        this.proxyObj[name] = proxy;
    };
    /**
     * 取proxy
     */
    Facade.prototype.getProxy = function (name) {
        return this.proxyObj[name];
    };
    /**
     * 注册协议函数 data:[function,this]
     */
    Facade.prototype.registerProtocol = function (id, data) {
        var ff = this.protocolObj[id];
        if (!ff) {
            this.protocolObj[id] = data;
        }
        else {
            throw new Error("重复注册Protocol");
        }
    };
    /**
     * 获得协议函数
     */
    Facade.prototype.getProtocolFun = function (id) {
        return this.protocolObj[id];
    };
    /**
     * 添加事件监听
     */
    Facade.prototype.addListener = function (type, listenArr) {
        var arr = this.listenerObj[type];
        if (!arr) {
            this.listenerObj[type] = arr = [];
            arr.push(listenArr);
        }
        else if (arr.indexOf(listenArr) == -1) {
            arr.push(listenArr);
        }
    };
    /**
     * 移除监听
     */
    Facade.prototype.removeListener = function (type, listerArr) {
        var arr = this.listenerObj[type];
        if (!arr || arr.length == 0) {
            return;
        }
        var index = arr.indexOf(listerArr);
        if (index != -1) {
            arr.splice(index, 1);
        }
    };
    /**
     * 简单抛事件
     */
    Facade.prototype.simpleDispatcher = function (type, data) {
        if (data === void 0) { data = null; }
        var funList = this.listenerObj[type];
        if (funList) {
            for (var i in funList) {
                var arr = funList[i];
                arr[0].call(arr[1], data);
            }
        }
    };
    return Facade;
}());
__reflect(Facade.prototype, "Facade");
//# sourceMappingURL=Facade.js.map