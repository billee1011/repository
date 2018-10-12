<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="title_area">
	<h2>游戏服管理</h2>
</div>

<div class="bd" style="padding-top: 10px;">
	<div class="bd_title">
		<div class="bd_title_btns">
			<a id="btn_rename" href="<c:url value="//area/create.do"/>" style="display: none;" class="btn btn_blue" title="增加一个实例"><span>新增</span></a>
			<a id="btn_back" href="javascript:void(0);" style="display: none;" onclick="return false;" class="btn btn_disable_v2" title="请选择一个或多个实例"><span>退还</span></a>
			<a id="btn_project" href="javascript:void(0);" onclick="return false;" class="btn btn_disable_v2" title="请选择一个或多个实例" style="display: none;"><span>分配至项目</span></a>
		</div>
		<div class="bd_title_op">
			<a id="btn_refresh" href="javascript:void(0);" onclick="return false;" class="ico ico_refresh" title="刷新当前页面"><span class="visually_hidden">刷新</span></a>
			<a id="btn_setting" href="javascript:void(0);" onclick="return false;" class="ico ico_setting" title="设置列表显示字段"><span class="visually_hidden">设置</span></a>
		</div>
	</div>
                            
	<div class="table_area" style="overflow: auto;">
		<div class="table_mask" id="cont_table_area" style="width: 100%;">
		  <c:choose>
		    <c:when test="${empty gameAreaVoList}">
			  <div class="table_blank">
				<p class="text">还没有区服</p>
		      </div>
			</c:when>
			<c:otherwise>
				<div class="bd_content_table">
					<table>
					<thead style="height: 31px">
						<tr>
							<th style="width:5%" title="研发商分配的世界ID"><div class="op"><span class="table_title">ID</span></div></th>
							<th style="width:10%" title="研发商分配的名称"><div class="op"><span class="table_title">服务器名</span></div></th>
							<th style="width:5%" title="主机leader的ID"><div class="op"><span class="table_title">主机</span></div></th>
							<th style="width:5%" title="平台分配的ID"><div class="op"><span class="table_title">游戏区ID</span></div></th>
							<th style="width:10%" title="平台分配的区名"><div class="op"><span class="table_title">游戏区名</span></div></th>
							<th style="width:10%"><div class="op"><span class="table_title">类型</span></div></th>
							<th style="width:20%"><div class="op"><span class="table_title">游戏地址</span></div></th>
							<th style="width:10%"><div class="op"><span class="table_title">后台地址</span></div></th>
							<th style="width:10%"><div class="op"><span class="table_title">平台</span></div></th>
							<th style="width:10%"><div class="op"><span class="table_title">状态</span></div></th>
							<!-- <th style="width:210px;"><div class="op"><span class="table_title">操作</span></div></th> -->
						</tr>
					</thead>
						<tbody>
						 <c:forEach items="${gameAreaVoList}" var="area" varStatus="status">
						    <c:choose>
							  <c:when test="${area.followerId eq 0}">
							  <tr>
							 </c:when>
					         <c:otherwise>
					          <tr style="background-color: #FEFEFE;">
					         </c:otherwise>
					      </c:choose>
								<td class="name">
								<div class="name_text">
										<span title="" class="text textoverflow" style="width: 154px;">${area.worldId}</span>
									</div></td>
								<td class="name">
								<div class="name_text">
										<span title="" class="text textoverflow" style="width: 154px;">${area.worldName}</span>
									</div></td>
								<td class="name">
								<div class="name_text">
								     <c:choose>
								         <c:when test="${area.followerId eq 0}">
								             <span title="" class="text textoverflow" style="width: 154px;"></span>
								         </c:when>
								         <c:otherwise>
								             <span title="" class="text textoverflow" style="width: 154px;">${area.followerId}</span>
								         </c:otherwise>
								     </c:choose>
									</div></td>
								<td class="name">
								<div class="name_text">
										<span title="" class="text textoverflow" style="width: 154px;">${area.areaId}</span>
									</div></td>
								<td><div class="name_text">
								
										<span title="" class="text textoverflow" style="width: 180px;">${area.areaName }</span>
									</div></td>
								<td><div class="name_text">
										<span title=""
											class="text textoverflow" style="width: 180px;"><spring:message code="mm.area.servertype.${area.type}" text="no message"/></span>
									</div></td>
								<td><div class="name_text">
										<span title=""
											class="text textoverflow" style="width: 180px;">${area.ip}:${area.port}</span>
									</div></td>
								<td><div class="name_text">
										<span title=""
											class="text textoverflow" style="width: 180px;">${area.externalIp}:${area.tcpPort}</span>
									</div></td>
								<td><div class="name_text">
										<span title=""
											class="text textoverflow" style="width: 180px;">${area.platformName}</span>
									</div></td>
								<td><div class="name_text">
										<span title=""
											class="text textoverflow" style="width: 180px;">
											<c:choose>
											    <c:when test="${area.valid}">
											        <img alt="" style="vertical-align: middle;margin-right:10px;" src="${pageContext.request.contextPath}/images/manage_center/running.jpg"><spring:message code="mm.area.running" text="运行中"></spring:message>
											    </c:when>
											    <c:otherwise>
											        <img alt="" style="vertical-align: middle;margin-right:10px;" src="${pageContext.request.contextPath}/images/manage_center/stop.jpg"><spring:message code="mm.area.stop" text="停止"></spring:message>
											    </c:otherwise>
											</c:choose>
											</span>
									</div></td>
								<!--<td><div class="name_text" style="width: 100px;">
										<span class="text text_link"><a href="<c:url value='//area/delete.do?id=${area.areaId}'/>">删除</a>|<a href="<c:url value='//area/update.do?id=${area.areaId}'/>">修改</a></span>
									</div></td>  -->
							</tr>
							</c:forEach>
						</tbody>
					</table>
				  </div>
				</c:otherwise>
			</c:choose>
        </div>
    </div>
</div>
<div id="mask_div" class="ui_mask" style="width: 1920px; height: 1746px; position: absolute; top: 0px; left: 0px; display: none; background-color: rgb(0, 0, 0); opacity: 0.6; z-index: 999; background-position: initial initial; background-repeat: initial initial;"></div>
<div id="formDiv" style="display:none;" class="pop_layer_v2">
</div>
<script type="text/javascript">
$(function(){
	// AJAX去加载页面, 显示加载图片，加载目标页面，隐藏加载图片
	$("div.bd a").unbind("click").click(function(){
		var url = $(this).attr('href');
		$.get(url, function(data){
			$('#formDiv').html(data);
		});
		return false;
	});
});

function toShow(){
	  $('#mask_div').show();
	  $('#formDiv').css("position", "absolute");
	  var left = ($('.content_main').width() - $('#formDiv').width()) / 2;
	  $('#formDiv').css("left", left);
	  $('#formDiv').show();
	  $('#formDiv').draggable({handle:'.pop_layer_title'});
 }
 function cancel(){
	  $('#mask_div').hide();
	  $('#formDiv').hide();
 }
 
 function popSubmit(){
	 var action = $("#postform").attr("action");
	 
	 $.ajax({
         cache: true,
         type: "POST",
         url:action,
         data:$('#postform').serialize(),// 你的formid
         async: false,
         error: function(request, errorMsg) {
             $('#pop_error').html(errorMsg);
         },
         success: function(data) {
             $('.content_main').html(data);
         }
     });
 }
</script>