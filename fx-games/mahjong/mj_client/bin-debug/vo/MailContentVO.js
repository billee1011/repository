var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var MailContentVO = (function () {
    function MailContentVO() {
    }
    MailContentVO.prototype.decode = function (data) {
        this.title = data[0];
        this.senderId = data[1];
        this.senderName = data[2];
        this.content = data[3];
        this.addTime = data[4];
        this.diamond = data[5];
    };
    return MailContentVO;
}());
__reflect(MailContentVO.prototype, "MailContentVO");
//# sourceMappingURL=MailContentVO.js.map