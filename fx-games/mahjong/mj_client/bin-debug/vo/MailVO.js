var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var MailVO = (function () {
    function MailVO() {
    }
    MailVO.prototype.decode = function (data) {
        this.id = data[0];
        this.senderId = data[1];
        this.senderName = data[2];
        this.state = data[3];
    };
    return MailVO;
}());
__reflect(MailVO.prototype, "MailVO");
//# sourceMappingURL=MailVO.js.map