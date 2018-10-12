/**
 * 解析时间
 */
class TimeUtil {
	public static Day:string = '天';
	public static Hour:string = '时';
	public static Minute:string = '分';
	public static Second:string = '秒';
	private static dayTime:number = 24 * 3600;
	public constructor() {
	}
	/**
	 * 单位/毫秒
	 * 返回：xx天xx时xx分xx秒
	 */
	public static getCountDownTime(leftTime:number):string
	{
		leftTime = Math.floor(leftTime/1000);
		var day:number = Math.floor(leftTime/this.dayTime);
		var dayAfter:number = leftTime%this.dayTime;
		var hour:number = Math.floor(dayAfter/3600);
		var hourAfter:number = leftTime%3600;
		var minute:number = Math.floor(hourAfter/60);
		var second:number = hourAfter%60;
		var back:string = '';
		if(day > 0){
			back += day < 10 ? '0'+day : day;
			back += this.Day;
		}
		if(hour > 0){
			back += hour < 10 ? '0'+hour : hour;
			back += this.Hour;
		}
		if(minute > 0){
			back += minute < 10 ? '0'+minute : minute;
			back += this.Minute;
		}
		if(second > 0){
			back += second < 10 ? '0'+second : second;
			back += this.Second;
		}
		return back;
	}
}