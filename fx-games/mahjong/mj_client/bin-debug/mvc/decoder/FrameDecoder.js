var __reflect = (this && this.__reflect) || function (p, c, t) {
    p.__class__ = c, t ? t.push(c) : t = [c], p.__types__ = p.__types__ ? t.concat(p.__types__) : t;
};
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
/**
 * 分流decoder
 */
var FrameDecoder = (function (_super) {
    __extends(FrameDecoder, _super);
    function FrameDecoder() {
        var _this = _super.call(this) || this;
        _this.types = [13001, 13002, 13003, 14002, 16001, 11001];
        _this.model = TFacade.getProxy(MjModel.NAME);
        return _this;
    }
    /**
     * 13001 拉取公告列表
        c->s
        s->c
            [
                id,
                content,
                interval,
                beginTime, 格式：yyyy-MM-dd HH:mm:ss，返回的是string
                endTime, 格式：yyyy-MM-dd HH:mm:ss，返回的是string
            ]...
     */
    FrameDecoder.prototype.f_13001 = function (data) {
        this.model.gonggaoList = [];
        var vo;
        for (var key in data) {
            var temp = data[key];
            vo = new GonggaoVO();
            vo.decode(temp);
            this.model.gonggaoList.push(vo);
        }
        this.model.simpleDispatcher(MjModel.GET_GONGGAO_LIST);
    };
    /**
     * 13002 添加公告列表
        s->c
            [
                id,
                content,
                interval,
                beginTime, 格式：yyyy-MM-dd HH:mm:ss，返回的是string
                endTime, 格式：yyyy-MM-dd HH:mm:ss，返回的是string
            ]
     */
    FrameDecoder.prototype.f_13002 = function (data) {
        var vo = new GonggaoVO();
        vo.decode(data);
        if (!this.model.gonggaoList) {
            this.model.gonggaoList = [];
        }
        this.model.gonggaoList.unshift(vo);
    };
    /**
     * 13003 删除公告
        s->c
        [
            id
        ]
     */
    FrameDecoder.prototype.f_13003 = function (data) {
        if (!this.model.gonggaoList)
            return;
        var len = this.model.gonggaoList.length;
        for (var i = 0; i < len; i++) {
            var vo = this.model.gonggaoList[i];
            if (vo.id == data[0]) {
                this.model.gonggaoList.splice(i, 1);
                break;
            }
        }
    };
    /**
     * 14002 拉取邮件列表
        c->s
        s->c
            [
                id,
                senderId,发送者id，为0就是系统发送，senderName就是null。
                senderName,
                status  邮件状态 1=已读取  0=未读取
            ]...
     */
    FrameDecoder.prototype.f_14002 = function (data) {
        this.model.mailList = [];
        for (var key in data) {
            var temp = data[key];
            var vo = new MailVO();
            vo.decode(temp);
            this.model.mailList.push(vo);
        }
    };
    /***
     * 16001 拉去版本公告
        c->s
        s->c
            [
                type,
                content,
                version
            ]...
     */
    FrameDecoder.prototype.f_16001 = function (data) {
        var vo = new VersionVO();
        vo.type = data[0];
        vo.content = data[1];
        vo.version = data[2];
        this.model.versionData = vo;
    };
    /**
     * 11001 聊天
        c->s
        Object[] obj
        s->c
            失败=[0,errorcode]
            成功=[
                obj（服务器只做转发）
            ]
     */
    FrameDecoder.prototype.f_11001 = function (data) {
        if (!this.checkSucc(data)) {
            return;
        }
    };
    return FrameDecoder;
}(TDecoder));
__reflect(FrameDecoder.prototype, "FrameDecoder");
//# sourceMappingURL=FrameDecoder.js.map