package com.cai.mapreduce.query;

/**
 * 充值每个档位统计
 * @author chansonyan
 * 2018年7月7日
 */
public class RechargeDailySummaryMapReduce {
	
	public static String MAP = "function() {"
        + "emit(this.v1, { account_id: this.account_id, distAcCount : 1, count:1})"
    	+ "}";
	
	public static String REDUCE = "function(key, values){"+
        "var res={count:0,distAcCount:0};"+
        "var accountId = 0;"+
        "values.forEach(function(val){ "+
        "    if(accountId != val.account_id) {"+
        "        accountId = val.account_id;"+
        "        res.distAcCount+=1;"+
        "    }"+
        "    res.count+=val.count;"+
        "});  "+
        "return res;}";

}
