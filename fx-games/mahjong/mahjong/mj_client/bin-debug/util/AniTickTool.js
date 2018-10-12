var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var AniTickTool = (function () {
    /**
     * label 美术字
     * extendLabel 替代的美术字
     * conditionTime 剩余多少时间的时候用替代的美术字
     */
    function AniTickTool(label, extendLabel, conditionTime, centerX, centerY) {
        if (centerX === void 0) { centerX = -1; }
        if (centerY === void 0) { centerY = -1; }
        this.label = label;
        this.record = -1;
        this.extendLabel = extendLabel;
        if (extendLabel)
            this.extendLabel.visible = false;
        this.conditionTime = conditionTime;
        this.centerX = centerX;
        this.centerY = centerY;
    }
    /**
     * time：倒计时时间间隔 /毫秒
     */
    AniTickTool.prototype.setOverTime = function (time) {
        this.overTime = egret.getTimer() + time;
        this.label.visible = true;
        if (this.extendLabel)
            this.extendLabel.visible = false;
    };
    AniTickTool.prototype.startTick = function (time) {
        if (time === void 0) { time = -1; }
        if (time != -1) {
            this.setOverTime(time);
        }
        if (!this.runing) {
            this.record = egret.setInterval(this.tick, this, 1000);
            this.runing = true;
            this.tick();
        }
    };
    AniTickTool.prototype.tick = function () {
        var countDownTime = Math.ceil(this.overTime - egret.getTimer());
        var current;
        if (this.extendLabel == null || countDownTime > this.conditionTime) {
            this.label.text = Math.round(countDownTime / 1000).toString();
            current = this.label;
        }
        else {
            this.label.visible = false;
            this.extendLabel.visible = true;
            this.extendLabel.text = Math.floor(countDownTime / 1000).toString();
            current = this.extendLabel;
        }
        if (this.centerX != -1) {
            current.x = this.centerX - current.width * .5;
        }
        if (countDownTime < 0) {
            this.stopTick();
            if (this.extendLabel) {
                this.extendLabel.text = '0';
            }
            if (this.backFun != null) {
                //this.label.text = '';
                this.backFun.call(this.funThis);
            }
        }
    };
    AniTickTool.prototype.stopTick = function () {
        if (this.runing) {
            egret.clearInterval(this.record);
            this.runing = false;
        }
    };
    return AniTickTool;
}());
__reflect(AniTickTool.prototype, "AniTickTool");
//# sourceMappingURL=AniTickTool.js.map