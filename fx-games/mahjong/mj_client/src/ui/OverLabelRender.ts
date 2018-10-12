class OverLabelRender extends RenderBase{
	public constructor() {
		super('resource/UI_exml/OverLabelItem.exml');
	}

	protected uiLoadComplete(evt:eui.UIEvent=null):void
    {
        //override
    }
	private label_msg:eui.Label;
    protected dataChanged():void{
		var arr:any[] = this.data;
		var s:string = '';
		switch (arr[0]) {
			case 1:
				s = '自摸次数';
				break;
			case 2:
				s = '接炮次数';
				break;
			case 3:
				s = '点炮次数';
				break;
			case 4:
				s = '暗杠次数';
				break;
			case 5:
				s = '明杠次数';
				break;
		}
		s += '        ' + arr[1];
		this.label_msg.text = s;
    }

}