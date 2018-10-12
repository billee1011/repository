<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%
	request.setAttribute("ctx", request.getContextPath());
%>
<div class="title_area">
	<h2>资源管理</h2>
</div>
<input id="ctx" type="hidden" value="${ctx }">
<div>
	<h3>
		<span id="returnInfo_pop_error" class="tip_error"></span>
	</h3>
</div>
<div class="bd" style="padding-top: 10px; margin-bottom: 10px;">
	<a href="#" id="reloadData" class="btn btn_blue" style="margin-left:40px;">reload数据</a>
</div>
<script>
	$(document).ready(function(){
		$("#reloadData").click(function(){
			$.post("${ctx}/mm/datamanager/reload.do", function(data){
				if(data == 1){
					alert('<spring:message code="1" text="success"></spring:message>');
				}else{
					alert('<spring:message code="0" text="fail"></spring:message>');
				}
			});
		});
	});
</script>

