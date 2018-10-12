var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var AsySoundManager = (function () {
    function AsySoundManager() {
        this.loader = new egret.URLLoader();
        this.loader.dataFormat = egret.URLLoaderDataFormat.SOUND;
        this.loader.addEventListener(egret.Event.COMPLETE, this.onLoadComplete, this);
    }
    AsySoundManager.prototype.loadSound = function (name) {
        var vo = new QueenVO();
        vo.data = name;
        if (!this.last) {
            this.first = this.last = vo;
        }
        else {
            this.last.next = vo;
            vo.pre = this.last;
            this.last = vo;
        }
        this.startLoad();
    };
    AsySoundManager.prototype.startLoad = function () {
        this.loader.load(new egret.URLRequest(PathDefine.MJ_SOUND + this.first.data + '.mp3'));
    };
    AsySoundManager.prototype.onLoadComplete = function (evt) {
        var sound = this.loader.data;
        var c = sound.play(0, 1);
        c.volume = GlobalDefine.playSoundVolume;
        this.first = this.first.next;
        if (!this.first) {
            this.first = this.last = null;
            return;
        }
        this.startLoad();
    };
    return AsySoundManager;
}());
AsySoundManager.instance = new AsySoundManager();
__reflect(AsySoundManager.prototype, "AsySoundManager");
//# sourceMappingURL=AsySoundManager.js.map