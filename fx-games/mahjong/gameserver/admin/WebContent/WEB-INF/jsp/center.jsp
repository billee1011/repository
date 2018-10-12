<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%request.setAttribute("ctx", request.getContextPath());%>
<!DOCTYPE html>
<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>乐游科技管理平台</title>
    
    <link href="${ctx }/favicon.ico" rel="bookmark" type="image/x-icon" /> 
	<link href="${ctx }/favicon.ico" rel="icon" type="image/x-icon" /> 
	<link href="${ctx }/favicon.ico" rel="shortcut icon" type="image/x-icon" /> 
	
	<%@include file="jsloader.jsp" %>
	
    <style type="text/css">
    	.mod_slidenav h3 div{padding: 18px 0 0 50px}
    	.mod_slidenav h3 div em{color: white;font-size: 20px;}
    </style>
</head>
<body style="overflow-x: hidden; overflow-y: auto;">
   <div id="homePagePassed" style="display: none;">1</div>
	<!-- 头部内部和一级导航 -->
	<div class="head_manage_v2">
		<div class="nav_area_1">
	       	<h1 class="mod_logo"><a href="javascript:void(0)" class="logo_yun" title="后台管理中心">后台管理中心</a><a href="javascript:void(0)" class="logo_manage" title="后台管理中心">后台管理中心</a></h1>
			<div class="op_area">
				<div class="login_info">
					<span>HI, <em>${sessionScope.USER.nickName }</em></span>
					<span class="stick">|</span>
					<span class="message"><a href="javascript:void(0)" id="message_num"> 消息（<em>1</em>）</a></span>
					<span class="stick">|</span>
	                <span><a href="${ctx }/logout.do">退出</a> </span>
	            </div>
	            <div class="op_nav">
	                <span><a href="javascript:void(0)">求助</a> </span>
	                <span class="right_item"><a href="javascript:void(0)" target="_blank">Wiki</a> </span>
	            </div>
	       </div>
   		</div>

		<!-- 一级导航菜单 -->
		<script type="text/javascript">
		$(function(){
			// AJAX去加载页面, 显示加载图片，加载目标页面，隐藏加载图片
			$(".nav_1").click(function(){$(".mod_table_loading").show();$("li.selected").removeClass("nav_control j_hassub_menu selected"); 
			$(this).parent().addClass("nav_control j_hassub_menu selected"); $(".body").load($(this).attr('href'));$(".mod_table_loading").hide();return false;})
			// 如果有下一个Div就显示
			.hover(function(){$("div.control_slide").hide();$("div.authority_slide").hide(); 
			//alert('该功能正在开发中...');
			//$(this).next().show();//TODO 暂时屏蔽 by Allen
			}); 
			$("div.control_slide").hover(function(){},function(){$(this).hide();});
			$("div.authority_slide").hover(function(){},function(){$(this).hide();});
		});
		</script>
    	<div class="nav_area_2">
        	<div class="nav_items">
                <ul class="nav_ul">
                	<li class="nav_control j_hassub_menu selected"><a href="${ctx }/summary.do" class="nav_1"><span>总览</span></a></li>
					<li><a href="${ctx }/pss/index.do" class="nav_1"><span>客服系统</span></a></li>
					<li><a href="${ctx }/stat/index.do" class="nav_1"><span>统计系统</span></a></li>
					<li><a href="${ctx }/ps/index.do" class="nav_1"><span>运维系统</span></a></li>
					<li><a href="${ctx }/mm/index.do" target="_blank" class="nav_1"><span>后台管理</span></a></li>
				</ul>
            </div>
            <div class="op_area">
             	<a>在线:<span id="titleOnlineNum">${onlineNum}</span> 人</a>
             	<a>版本:<span id="version">${version}</span></a>
             	<span style="display:none;" id="serverTimeMillis">${serverTime.time / 1000}</span>
             	<a><span id="timezone" style="margin-right:10px;">${timezone }</span><span id="serverTime"><fmt:formatDate value="${serverTime}" pattern="yyyy-MM-dd HH:mm:ss"/></span></a>
                <a href="javascript:void(0);" class="btn btn_blue btn_more_op"><span class="select_down" style="padding-right:1px"><span id="display_p_select_down">${platform.name}</span></span><i style="position: absolute; border-color: #fff transparent transparent; margin:8px 5px 8px 5px;" class="ico ico_trangledown"></i></a>
              	<div id="p_select_down" class="select_down" style="position: absolute; width: 230px;right: 66px;display: none;z-index:999;">
	                <ul class="down_list" style="list-style-type:none;width: 110px;">
	                	<c:forEach items="${platformList }" var="p">
	                		<li style="width: 100%" onclick="javascript:selectChange(this, 'display_p_select_down', 'changeplatform', '${p.id }', '${ctx }');"><a href="javascript:;" class="disable" style="width: 100%;padding: 0 0 0 0">${p.name }</a></li>
	                	</c:forEach>
					</ul>
                </div>
                <img src="${ctx}/images/home/left.png" style="width:10px;height:15px;margin-top:5px;cursor:pointer" onclick="changeAreaPage(-1)"/>
	            <a href="#" onclick="toSelectArea(1, '${ctx }')" id="displayCurrentArea" class="btn_buy btn_more_op" style="width: auto;"><span style="display:none;">${area.areaId}</span><span style="display:none;">${area.worldId}</span><span>${area.areaName}</span></a>
	            <img src="${ctx}/images/home/right.png" style="width:10px;height:15px;margin-top:5px;cursor:pointer" onclick="changeAreaPage(1)"/>
            </div>
		</div>
	</div>
	
	<!-- 显示内容的Body -->
	<div class="body" style="overflow:hidden;"></div>
	
	<!-- loading -->
	<div style="z-index: 8999; left: 928px; top: 422px; position: absolute;" class="mod_table_loading">
	    <div class="loading_pad"><i class="ico ico_loading_big"></i></div>
	</div>
	
	<div id="center_mask_div" class="ui_mask" style="width: 1920px; height: 1746px; position: absolute; top: 0px; left: 0px; display: none; background-color: rgb(0, 0, 0); opacity: 0.6; z-index: 999; background-position: initial initial; background-repeat: initial initial;"></div>
<div id="selectarea" style="display:none;background-color: #fff;" class="pop_layer_v2">
</div>
	
<!-- 默认加载首页的功能 -->
<script type="text/javascript">
var info = $.datepicker.regional['zh-CN'];
time = parseFloat($('#serverTimeMillis').html()) + (${timezonerawoffset} / 1000) + (new Date().getTimezoneOffset() * 60);
gapTime = time - new Date().getTime() / 1000; //服务器时间和本地时间差值
$(function(){
	$.ajaxSetup({
		  cache: true
	});
	$(".body").load($(".nav_1:first").attr("href"));
	$(".mod_table_loading").hide();
	
	setInterval(function(){//1秒更新一次时间
		var date = new Date();
	   date.setTime(date.getTime() + gapTime * 1000);
	   $('#serverTime').html(formatDate(date));
	}, 1000);
	
	setInterval(function(){
		$.get('${ctx}/onlinenum.do', function(data){
			$('#titleOnlineNum').html(data);
		});
	}, 60000);
	
	$(".btn_more_op").click(function(){
		$(this).next('div').hover(function(){},function(){$(this).hide();}).show();
	});
	
});


function changeAreaPage(type){
	var areaId = $('#displayCurrentArea').children("span")[0].innerHTML;
	$.ajax({url:'${ctx}/changeAreaPage.do',data:{areaId:areaId,type:type},success:function(data) {
		     if(areaId!=data){
				selectArea(data, 'pop_error', '${ctx}');
		     }
		}
	});
}
</script>
</body></html>