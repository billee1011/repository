var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
/**
 * 资源缓存
 */
var ResourceCache = (function () {
    function ResourceCache() {
        this.imageCache = {};
        this.mcCache = {};
    }
    Object.defineProperty(ResourceCache, "instance", {
        get: function () {
            if (!this._instance) {
                this._instance = new ResourceCache();
            }
            return this._instance;
        },
        enumerable: true,
        configurable: true
    });
    ResourceCache.prototype.addImageToCache = function (key, data) {
        this.imageCache[key] = data;
    };
    ResourceCache.prototype.getImageFromCache = function (key) {
        return this.imageCache[key];
    };
    ResourceCache.prototype.addMcToCache = function (key, data) {
        this.mcCache[key] = data;
    };
    ResourceCache.prototype.getMcFromCache = function (key) {
        return this.mcCache[key];
    };
    return ResourceCache;
}());
__reflect(ResourceCache.prototype, "ResourceCache");
//# sourceMappingURL=ResourceCache.js.map