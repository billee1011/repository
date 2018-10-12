/**
 * 分流decoder
 */
class FrameDecoder extends TDecoder{
	public constructor() {
		super();

		this.types = [13001,13002,13003,14002,16001,11001];
		this.model = TFacade.getProxy(MjModel.NAME);
	}
	private model:MjModel;
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
	public f_13001(data:any[]):void
	{
		this.model.gonggaoList = [];
		let vo:GonggaoVO;
		for (var key in data) {
			let temp:any[] = data[key];
			vo = new GonggaoVO();
			vo.decode(temp);
			this.model.gonggaoList.push(vo);
		}
		this.model.simpleDispatcher(MjModel.GET_GONGGAO_LIST);
	}
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
	public f_13002(data:any[]):void
	{
		let vo:GonggaoVO = new GonggaoVO();
		vo.decode(data);
		if(!this.model.gonggaoList){
			this.model.gonggaoList = [];
		}
		this.model.gonggaoList.unshift(vo);

	}

	/**
	 * 13003 删除公告
		s->c
		[
			id
		]
	 */
	public f_13003(data:any[]):void
	{
		if(!this.model.gonggaoList) return;
		let len:number = this.model.gonggaoList.length;
		for (var i:number=0; i<len; i++) {
			let vo:GonggaoVO = this.model.gonggaoList[i];
			if(vo.id == data[0]){
				this.model.gonggaoList.splice(i,1);
				break;
			}
		}
	}

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
	public f_14002(data:any[]):void
	{
		this.model.mailList = [];
		for (var key in data) {
			let temp:any[] = data[key];
			let vo:MailVO = new MailVO();
			vo.decode(temp);
			this.model.mailList.push(vo);
		}
	}
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
	public f_16001(data:any[]):void
	{
		let vo:VersionVO = new VersionVO();
		vo.type = data[0];
		vo.content = data[1];
		vo.version = data[2];
		this.model.versionData = vo;
	}
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
	public f_11001(data:any[]):void
	{
		if(!this.checkSucc(data)){
			return;
		}
		
	}
}