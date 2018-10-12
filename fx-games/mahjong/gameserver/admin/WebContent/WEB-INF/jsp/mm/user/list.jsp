<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<div class="title_area">
	<h2>账号管理</h2>
</div>

<div class="bd" style="padding-top: 10px;">
	<div class="bd_title">
		<div class="bd_title_btns">
			<a id="btn_rename" href="<c:url value="/mm/user/create.do"/>" class="btn btn_blue" title="增加一个实例" targetSelector="#formDiv"><span>新增</span></a>
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
		      <c:when test="${empty userVoList }">
				<div class="table_blank">
					<p class="text">您还没实例， <a class="link" href="<c:url value="/mm/user/create.do"/>">立即创建</a></p>
				</div>
			  </c:when>
			  <c:otherwise>
				<div>
					<table id="mmUserTable" style="height: 650px">
					<thead style="height: 31px">
						<tr>
							<th data-options="field:'check',width:calColumnWidth(0.02)"
										style="width: 35px;" data-options="field:'check'"><div class="op"><a href="javascript:void(0);" class="btn_select_16" style="margin-left: 5px"><span class="visually_hidden">checkbox</span></a> </div></th>
							<th data-options="field:'id',width:calColumnWidth(0.025)">ID</th>
							<th data-options="field:'name',width:calColumnWidth(0.1)">用户名</th>
							<th data-options="field:'nickName',width:calColumnWidth(0.1)">昵称</th>
							<th data-options="field:'email',width:calColumnWidth(0.18)">邮件</th>
							<th data-options="field:'role',width:calColumnWidth(0.08)">角色</th>
							<th data-options="field:'lastArea',width:calColumnWidth(0.1)">上次登录的区</th>
							<th data-options="field:'platform',width:calColumnWidth(0.1)">所属平台</th>
							<th data-options="field:'opt',width:calColumnWidth(0.12)">操作</th>
							<th data-options="field:'createTime',width:calColumnWidth(0.1)">创建时间</th>
						</tr>
					</thead>
						<tbody>
						 <c:forEach items="${userVoList}" var="user" varStatus="status">
							<tr>
								<td class="line_checkbox"><a href="javascript:void(0);" onclick="return false;" class="btn_select_16"><span class="visually_hidden">checkbox</span></a></td>
								<td class="name">${user.id}</td>
								<td>${user.name }</td>
								<td>${user.nickName }</td>
								<td>${user.email}</td>
								<td>${user.roleName}</td>
								<td>${user.lastAreaName}</td>
								<td>
							    	[
								    <c:forEach items="${user.platformIdList}" var="platformId" varStatus="varStatus">
										<c:if test="${varStatus.index gt 0 }">
										   ,
										</c:if>								        
								        <spring:message code="platform.id.${platformId }" text="${platformId }"></spring:message>
								    </c:forEach>
								     ]
								
								</td>
								<td>
									<span class="text text_link"><a href="javascript:;" class="exclude" link="<c:url value='/mm/user/delete.do?id=${user.id}'/>"  onclick="linkHref(this);" targetSelector="#formDiv">删除</a>|
									<a href="javascript:;" class="exclude" link="<c:url value='/mm/user/update.do?id=${user.id}'/>"  onclick="linkHref(this);" targetSelector="#formDiv">修改</a>|
									<a href="javascript:;" class="exclude"  link="<c:url value='/mm/user/updatePassword.do?id=${user.id}'/>" onclick="linkHref(this);" targetSelector="#formDiv">修改密码</a>|
									<a href="javascript:;" class="exclude"  link="<c:url value='/mm/user/getPrivilege.do?id=${user.id}'/>" onclick="linkHref(this)" targetSelector="#formDiv">权限修改</a></span>
								</td>
								<td><fmt:formatDate value="${user.addTime}" pattern="yyyy-MM-dd HH:mm:ss"/></td>
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
	ajaxALink('div.bd a');
	commonPagination('#mmUserTable');
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