<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%
	request.setAttribute("ctx", request.getContextPath());
%>
<link href="${ctx}/css/css.css" rel="stylesheet" media="screen">
<style type="text/css">
<!--
.file {
	filter: alpha(opacity : 0);
	opacity: 0;
}
-->
</style>
<div class="title_area">
	<h2>服务器维护</h2>
</div>
<input id="ctx" type="hidden" value="${ctx }">
<div class="bd" style="padding-top: 10px; margin-bottom: 10px;">
	<div id="content_area" class="table_area"
		style="overflow: auto; vertical-align: top;">
		<div class="to_redeem_div">
			<div>
				<button class="btn btn_blue" onclick="dialogInfo('dialog')">当前区信息</button>
				<button class="btn btn_blue" onclick="toSelMultiArea(1, '${ctx}')">选区</button>
			</div>
			<div id="dialog" style="display: none;">
				<div id="dialogInfo_error" style="display: none;"><p>无法连接</p></div>
				<div id="dialogInfo_Info">
					<div><span>服务器名:</span><span id="serverInfo_worldName"></span></div>
	        		<div><span>和服时间:</span><span id="serverInfo_combineTime"></span></div>
					<div><span>维护时间:</span><span id="serverInfo_maintainTime"></span></div>
					<div><span>开区时间:</span><span id="serverInfo_startTime"></span></div>
					<div><span>重启次数:</span><span id="serverInfo_times"></span></div>
					<div><span>服务器状态:</span><span id="serverInfo_status"></span></div>
					<div><span>最大玩家人数:</span><span id="serverInfo_maxConcurrentUser"></span></div>
				</div>
    		</div>
			<div>
				<span class="tip" style="vertical-align: top;">操作原因：</span>
				<textarea id="reason" name="reason" rows="14" cols="75"
					style="width: auto;"></textarea>
			</div>
			<div>
				<button class="btn btn_blue" onclick="killOffPlayers($('#reason').val())">踢玩家下线</button>
				<button class="btn btn_blue" onclick="serverMaintain(1)">维护</button>
				<button class="btn btn_blue" onclick="serverMaintain(0)">开启</button>
			</div>
		</div>
	</div>
</div>

<%@include file="../mutiselectarea.jsp"%>
 <%@include file="../mutiselectcrossarea.jsp" %>

<div id="announcingList" style="display: none;" class="pop_layer_v2">
</div>
<div id="annoucingDeleteDialog" style="display: none;"
	class="pop_layer_v2"></div>
<input type="hidden" id="serverInfo" value="${serverInfo }">
<input type="hidden" id="info" value="${info }">
<div id="easyuiDlg" style="display: none;margin-left:30px;margin-top: 10px;">操作成功</div>

<div id="mask_div" class="ui_mask" style="width: 1920px; height: 1746px; position: absolute; top: 0px; left: 0px; display: none; background-color: rgb(0, 0, 0); opacity: 0.0; z-index: 999; background-position: initial initial; background-repeat: initial initial;"></div>
<div id="progressBarContainer" style="display:none;position:absolute;width: 600px; height: 600px;z-index: 10000;" class="pop_layer_v2">
</div>
<script>
	$(document).ready(function(){
		var info = $("#info").val();
		if(info){
			$("#easyuiDlg").show();
			$("#easyuiDlg").dialog({
				title:"操作结果",
				width:150,
				height:80
			});
			$(".content_main").append($("div[class='panel window']"));
			$(".content_main").append($("div[class='window-shadow']"));
		}
		var data = $("#serverInfo").val();
		if(!data || data == "null"){
			$("#dialogInfo_error").css({display:"block"});
			$("#dialogInfo_Info").css({display:"none"});
		}else{
			$("#dialogInfo_error").css({display:"none"});
			$("#dialogInfo_Info").css({display:"block"});
			var json = eval('('+data+')');
			$("#serverInfo_worldName").text(json.worldName);
			$("#serverInfo_combineTime").text(json.combineTime);
			$("#serverInfo_maintainTime").text(json.maintainTime);
			$("#serverInfo_startTime").text(json.startTime);
			$("#serverInfo_times").text(json.times);
			$("#serverInfo_status").text(Util_ServerStatus(json.status));
			$("#serverInfo_maxConcurrentUser").text(json.maxConcurrentUser);
		}
		 <c:if test="${empty  serverInfo}">
	       alert("<spring:message code="exception.serverstoped" text="Server may be stoped, please try later."></spring:message>");
	     </c:if>
	     
	     var openMillis = $('#foreseeOpenTime').val();
	     if(openMillis){
	    	 $('#foreseeOpenTime').val(new Date(parseInt(openMillis)).bfFormatter());
	     }
	});
	
	var delay = 1;
	var maxCount = 10;
    function progressBar(callback){
    	var count = 0;
    	$('#mask_div').show();
    	popUp2('progressBarContainer', '#progressBarContainer .nothing');
    	var $loader = $("#progressBarContainer").percentageLoader({
            width : 300, height : 300, progress : 0, onProgressUpdate : function (value) {
            	$loader.setProgress(value);
            }});
    	$loader.setValue('');
    	var intervalId = setInterval(function(){
    		count++;
    		$loader.setProgress((count / maxCount));
    		if(count >= maxCount){
    			clearInterval(intervalId);
    			setTimeout(function(){
    				$('#mask_div').hide();
    				$('#progressBarContainer').hide();
    				callback();
    			}, 10);
    		}
    	}, delay);
    }
</script>

