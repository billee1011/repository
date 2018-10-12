<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<% request.setAttribute("ctx", request.getContextPath()); %>
<div class="mod_content" style="margin-left: 12px;">
	<div class="content_main">
		<div class="title_area" style="height:1000px">
			<h2 style="float: left;">总览</h2>
			<form>
				<div class="r relative" id="datePicker" style="right: 20px;">
					<a class="time" id="datePicker_a">
					<span><p>日期选择<b id="displayDateTip" style="font-weight:normal"> : 查询日期</b><br><font class="calendar" id="dateValue">2014-06-04</font></p></span></a>
					<div class="timetxt hide panone" id="datePanel" style="width: 278px; left: 0px; display: none;">
						<div id="dateBanner" class="timetop"> 
						</div>
						<p class="TimeDef" id="timeInputRoom">
							自定<input type="text" id="startTime" name="startTime" class="Timeinput">到 <input type="text" id="endTime" name="endTime" class="Timeinput mrnone">
						</p>
						<div class="opeDiv after">
							<small id="datamessage" style="display: none; margin-left: 30px;" class="l"></small>
							<a class="timecolse r Confirm" id="confirmBtn">确定</a>
							<a class="r Cancel" id="cancelBtn">取消</a>
						</div>
					</div>
				</div>
			</form>
		</div>
	</div>
</div>


<script type="text/javascript">

function updateAll(){
	blockAll();
}
function blockAll(){
	$("#divGaiKuang").block({message:"<image src='images/WaitProcess.gif'>",css:{border:'none',backgroundColor:'transparent'},overlayCSS:{opacity:0}});
	$("#divHuoYue").block({message:"<image src='images/WaitProcess.gif'>",css:{border:'none',backgroundColor:'transparent'},overlayCSS:{opacity:0}});
	$("#divChongZhi").block({message:"<image src='images/WaitProcess.gif'>",css:{border:'none',backgroundColor:'transparent'},overlayCSS:{opacity:0}});
	$("#divXiaoFei").block({message:"<image src='images/WaitProcess.gif'>",css:{border:'none',backgroundColor:'transparent'},overlayCSS:{opacity:0}});
	$("#divZhuanHuaLv").block({message:"<image src='images/WaitProcess.gif'>",css:{border:'none',backgroundColor:'transparent'},overlayCSS:{opacity:0}});
	$("#divLiuCun").block({message:"<image src='images/WaitProcess.gif'>",css:{border:'none',backgroundColor:'transparent'},overlayCSS:{opacity:0}});
	$("#divZuanChart").block({message:"<image src='images/WaitProcess.gif'>",css:{border:'none',backgroundColor:'transparent'},overlayCSS:{opacity:0}});
	$("#divShopChart").block({message:"<image src='images/WaitProcess.gif'>",css:{border:'none',backgroundColor:'transparent'},overlayCSS:{opacity:0}});
	$("#divShouRuChart").block({message:"<image src='images/WaitProcess.gif'>",css:{border:'none',backgroundColor:'transparent'},overlayCSS:{opacity:0}});
	$("#divShiShiChart").block({message:"<image src='images/WaitProcess.gif'>",css:{border:'none',backgroundColor:'transparent'},overlayCSS:{opacity:0}});
}

function _lessStartDay(day){
	var end = $("#startTime").val();
	end = new Date(end);
	end.setDate(end.getDate() - day);
	return end.Format("yyyy-MM-dd");
}

$(function () {
	initDatePicker({func:updateAll,days:1,noEnd:true},'${serverTime}','${timezonerawoffset}');
	updateAll();
	$(".content_main").css("overflow","scroll");
});
</script>