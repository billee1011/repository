<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="title_area">
	<h2>权限管理</h2>
</div>

<div class="bd" style="padding-top: 10px;">
	<div class="bd_title">
		<div class="bd_title_btns">
			<a id="btn_rename" href="<c:url value="/mm/role/create.do"/>" class="btn btn_blue" title="增加一个实例"><span>新增</span></a>
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
		      <c:when test="${empty roleVOList}">
				<div class="table_blank">
					<p class="text">您还没实例， <a class="link" href="<c:url value="/mm/role/create.do"/>">立即创建</a></p>
				</div>
			  </c:when>
			  <c:otherwise>
				<div class="bd_content_table">
				
					<table>
					<thead style="height: 31px">
						<tr>
							<th style="width: 35px;"><div class="op"><a href="javascript:void(0);" class="btn_select_16" style="margin-left: 5px"><span class="visually_hidden">checkbox</span></a> </div></th>
							<th style="width:25%"><div class="op"><span class="table_title">角色名</span></div></th>
							<th style="width:30%"><div class="op"><span class="table_title">描述</span></div></th>
							<th style="width:210px;"><div class="op"><span class="table_title">操作</span></div></th>
						</tr>
					</thead>
						<tbody>
						 <c:forEach items="${roleVOList}" var="role" varStatus="status">
							<tr>
								<td class="line_checkbox"><a href="javascript:void(0);" onclick="return false;" class="btn_select_16"><span class="visually_hidden">checkbox</span></a></td>
								<td class="name">
								<div class="name_text">
										<span title="" class="text textoverflow" style="width: 174px;">${role.name}</span>
									</div></td>
								<td><div class="name_text">
								
										<span title="" class="text textoverflow" style="width: 180px;">${role.description}</span>
									</div></td>
								<td style="width: 210px;"><div class="name_text">
										<span class="text text_link"><a href="<c:url value='/mm/role/delete.do?id=${role.id}'/>">删除</a>|<a href="<c:url value='/mm/role/update.do?id=${role.id}'/>">修改</a>|<a href="<c:url value='/mm/role/getPrivilege.do?roleId=${role.id}'/>">服务器权限管理</a></span>
									</div></td>
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

</script>