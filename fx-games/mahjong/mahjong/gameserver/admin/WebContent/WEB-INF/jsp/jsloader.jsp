    <%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%request.setAttribute("ctx", request.getContextPath());
  long timestamp = System.currentTimeMillis();
  request.setAttribute("timestamp", timestamp);
%>
    <link href="${ctx }/css/global/g_min.css" rel="stylesheet" media="screen">
    <link href="${ctx }/css/manage_center/mc_control_platform.css" rel="stylesheet" media="screen">
    <link href="${ctx }/css/manage_center/xg.css" rel="stylesheet" media="screen">
    <link href="${ctx}/css/vs.css" rel="stylesheet" media="screen">
    <link href="${ctx}/css/css.css" rel="stylesheet" media="screen">
    <link href="${ctx}/css/datePicker/datePicker.css" rel="stylesheet" type="text/css"></link>
    <link href="${ctx}/css/jqueryui-1.11.1/jquery-ui.min.css" rel="stylesheet" type="text/css"></link>
	<link href="${ctx}/css/jquery-ui-1.10.4.custom.min.css" rel="stylesheet" media="screen">
	<link href="${ctx}/css/jquery.datetimepicker.css" rel="stylesheet" type="text/css"></link>
	<link rel="stylesheet" type="text/css"	href="${ctx}/css/easyui_themes/default/easyui.css">
	<link rel="stylesheet" type="text/css"	href="${ctx}/css/easyui_themes/icon.css">
	<link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/easyui_demo.css">
	<link href="${ctx}/css/pqgrid.min.css" rel="stylesheet" media="screen">
	<link rel="stylesheet" href="${ctx}/css/themes/Office/pqgrid.css" />
	<link rel="stylesheet" href="${ctx}/css/jquery.percentageloader-0.1.css" />
	<link rel="stylesheet" href="${ctx}/css/jquery.percentageloader-0.1.css" />

	

    <script type="text/javascript" src="${ctx }/js/My97DatePicker/WdatePicker.js"></script>
    <script type="text/javascript" src="${ctx }/js/jquery-2.1.1.min.js"></script>
    <script type="text/javascript" src="${ctx }/js/jquery.md5.js"></script>
    <script type="text/javascript" src="${ctx }/js/jquery-ui.min.js"></script>
    <script type="text/javascript" src="${ctx }/js/jquery.datepicker-zh-CN.js"></script>
    <script type="text/javascript" src="${ctx }/js/selectall.js"></script>
    <script type="text/javascript" src="${ctx }/js/highlight.pack.js"></script>
    <script type="text/javascript" src="${ctx }/js/plugins/jquery.form.js"></script>
    <script type="text/javascript" src="${ctx }/js/jquery.fileupload.js"></script>
    <script type="text/javascript" src="${ctx}/js/jquery.easyui.min.js"></script>
    <script type="text/javascript" src="${ctx}/js/datagrid-detailview.js"></script>
    <script type="text/javascript" src="${ctx}/js/jquery.easyui.extend.js"></script>
    <script type="text/javascript" src="${ctx}/js/easyloader.js"></script>
    <script type="text/javascript" src="${ctx}/js/pqgrid.min.js"></script>
	<script type="text/javascript" src="${ctx }/js/highCharts/highcharts.js"></script>
	<script type="text/javascript" src="${ctx }/js/highCharts/highcharts-3d.js"></script>
	<script type="text/javascript" src="${ctx }/js/highCharts/exporting.js"></script>
	<script type="text/javascript" src="${ctx }/js/highCharts/export-excel.js"></script>
	<script type="text/javascript" src="${ctx }/js/highCharts/highcharts-more.js"></script>
    <script type="text/javascript" src="${ctx }/js/baofeng.js?date=${timestamp}"></script>
    <script type="text/javascript" src="${ctx }/js/jquery.cookie.js"></script>
    <script type="text/javascript" src="${ctx }/js/jquery.blockUI.js"></script>
    <script type="text/javascript" src="${ctx }/js/swfobject.js"></script>
    <script type="text/javascript" src="${ctx }/js/jquery.percentageloader-0.1.min.js"></script>
	<script type="text/javascript" src="${ctx }/js/editor/ueditor/ueditor.config.js"></script>
    <script type="text/javascript" src="${ctx }/js/editor/ueditor/ueditor.all.js"> </script>
    <script type="text/javascript" src="${ctx }/js/editor/ueditor/lang/zh-cn/zh-cn.js"> </script>
	<script type="text/javascript" src="${ctx }/js/dynamicNum.js"></script>
	<script type="text/javascript" src="${ctx }/js/jqconsole.min.js"></script>
	<script type="text/javascript" src="${ctx }/js/ystep/ystep.js"></script>
	
<!-- 在线编辑器相关的CSS和JS -->
<link rel="stylesheet" href="${ctx }/css/codemirror/codemirror.css">
<link rel="stylesheet" href="${ctx }/css/codemirror/eclipse.css">
<link rel="stylesheet" href="${ctx }/css/codemirror/show-hint.css" />

<script src="${ctx }/js/codemirror/codemirror.js"></script>
<script src="${ctx }/js/codemirror/javascript.js"></script>
<script src="${ctx }/js/codemirror/active-line.js"></script>
<script src="${ctx }/js/codemirror/matchbrackets.js"></script>
<script src="${ctx }/js/codemirror/sql.js"></script>
<script src="${ctx }/js/codemirror/show-hint.js"></script>
<script src="${ctx }/js/codemirror/sql-hint.js"></script>
<script src="${ctx }/js/codemirror/xml-hint.js"></script>
<script src="${ctx }/js/codemirror/xml.js"></script>


<script>
Date.prototype.Format = function(fmt)   
{
  var o = {   
    "M+" : this.getMonth()+1,                 //月份   
    "d+" : this.getDate(),                    //日   
    "h+" : this.getHours(),                   //小时   
    "m+" : this.getMinutes(),                 //分   
    "s+" : this.getSeconds(),                 //秒   
    "q+" : Math.floor((this.getMonth()+3)/3), //季度   
    "S"  : this.getMilliseconds()             //毫秒   
  };   
  if(/(y+)/.test(fmt))   
    fmt=fmt.replace(RegExp.$1, (this.getFullYear()+"").substr(4 - RegExp.$1.length));   
  for(var k in o)   
    if(new RegExp("("+ k +")").test(fmt))   
  fmt = fmt.replace(RegExp.$1, (RegExp.$1.length==1) ? (o[k]) : (("00"+ o[k]).substr((""+ o[k]).length)));   
  return fmt;   
};

Date.prototype.diff = function(date){
	  return (this.getTime() - date.getTime())/(24 * 60 * 60 * 1000);
	}

function jobStrChange(info){
	info = info.toUpperCase();
	switch(info)
	{
	case "A":
		return "战士";
	case "B":
		return "法师";
	case "C":
		return "牧师";
	case "D":
		return "潜行者";
	case "E":
		return "猎人";
	case "F":
		return "骑士";
	}
	
}
</script>


