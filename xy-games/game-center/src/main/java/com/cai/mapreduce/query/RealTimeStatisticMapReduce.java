package com.cai.mapreduce.query;

/**
 * 实时统计按账号类型统计充值账号数与充值金额
 * @author chansonyan
 * 2018年6月19日
 */
public class RealTimeStatisticMapReduce {
	
	public static String MAP = "function() {"
        + "emit(this.accountType, { accountId: this.accountId, cardNum: this.cardNum, accountDistinctCount : 1})"
    	+ "}";
	
	public static String REDUCE = "function(key, values) {"+
        "var accountId = 0;"+
        "var result = {accountDistinctCount:0,cardNum:0};"+
        "values.forEach(function(val) {"+
        "    if(accountId != val.accountId) {"+
        "        accountId = val.accountId;"+
        "        result.accountDistinctCount += 1;"+
        "    }"+
        "    result.cardNum += val.cardNum;"+
        "});"+
        "return result;}";

}
