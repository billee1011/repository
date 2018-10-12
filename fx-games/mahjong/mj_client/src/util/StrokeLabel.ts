/**
 * 描边的label
 */
class StrokeLabel {
	private label:eui.Label;
	public constructor(label:eui.Label) {
		this.label = label;
		this.label.stroke = 2;
		this.label.strokeColor = 0;
	}
	public set text(value:string)
	{
		this.label.text = value;
	}
}