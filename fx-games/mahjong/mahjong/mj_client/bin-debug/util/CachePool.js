var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var CachePool = (function () {
    function CachePool() {
        /**
         *  作为对象池的词典dict
         */
        this.objPoolDict = new Object();
    }
    CachePool.getInstance = function () {
        if (this.instance == null) {
            this.instance = new CachePool;
        }
        return this.instance;
    };
    /**
     * 向对象池中放入对象，以便重复利用
     * @param disObj 要的放入对象
     
        */
    CachePool.prototype.reBack = function (oldObj) {
        var objName = egret.getQualifiedClassName(oldObj);
        if (oldObj == null) {
            return;
        }
        if (this.objPoolDict[objName] == null) {
            this.objPoolDict[objName] = [];
        }
        if (this.objPoolDict[objName].lenth > 50) {
            return;
        }
        this.objPoolDict[objName].push(oldObj);
    };
    /**
     * 从对象池中取出需要的对象
     * @param targetObj 需要的对象类类名，没必要必须是类实例名 类名就可以
     * @return 取出的相应对象
     *
     */
    CachePool.prototype.getObject = function (targetObj) {
        var objName = egret.getQualifiedClassName(targetObj);
        if (this.objPoolDict[objName] != null && this.objPoolDict[objName].length > 0) {
            return this.objPoolDict[objName].pop();
        }
        //var objClass:any = egret.getDefinitionByName(objName);
        var obj = new targetObj();
        return obj;
    };
    return CachePool;
}());
__reflect(CachePool.prototype, "CachePool");
//# sourceMappingURL=CachePool.js.map