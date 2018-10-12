<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8" isELIgnored="false"%>
<%@taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<div class="pop_layer_main">
	<div class="pop_layer_title">
		<h3>
			账号管理 <span id="pop_error" class="tip_error"></span>
		</h3>
	</div>
	<div class="pop_layer_cont">
		<form:form method="POST" modelAttribute="user" id="postform">
			<table class="ui_formTable">
				<!-- 请指定各个列宽 -->
				<colgroup>
					<col style="width: 80px">
					<col style="width: auto">
				</colgroup>
				<tbody>
					<tr>
						<th scope="row">ID:</th>
						<td><form:input path="id" class="input" readonly="true"
								maxlength="40" /></td>
					</tr>
					<tr>
						<th scope="row">用户名:</th>
						<td><form:input path="name" class="input" readonly="true"
								maxlength="40" /></td>
					</tr>
					<tr>
						<th scope="row">昵称：</th>
						<td><form:input path="nickName" class="input" maxlength="40" />
						</td>
					</tr>
					<tr>
						<th scope="row">邮件：</th>
						<td><form:input path="email" class="input" maxlength="40" />
						</td>
					</tr>
					<tr>
						<th scope="row">权限角色：</th>
						<td><form:input path="roleId" class="input" readonly="true"
								maxlength="40" value="${rolename}" /></td>
					</tr>
					<tr>
						<th scope="row">绑定平台：</th>
						<td ><form:select path="platformIdList"   style="width:162px;">
								<c:forEach items="${platformList }" var="platform">
									<option value="${platform.id}" 
										<c:if test="${platform.success eq 1}">  selected="true" </c:if>>${platform.name}</option>
								</c:forEach>
							</form:select></td>
					</tr>
				</tbody>

			</table>
			<div class="pop_layer_ft">
				<a class="btn btn_submit not_close btn_blue"
					href="javascript:udpateUser();"><span class="">更新</span></a><a
					class="btn btn_white_2 btn_close" href="javascript:cancel();"><span>取消</span></a>
			</div>
		</form:form>
	</div>
</div>
<div id="i18ncodeupdate" style="display: none;">
	<span class="name"><spring:message code="nullcheck.name"></spring:message></span>
	<span class="nickName"><spring:message code="nullcheck.nickName"></spring:message></span>
	<span class="email"><spring:message code="nullcheck.email"></spring:message></span>
	<span class="roleId"><spring:message code="nullcheck.roleId"></spring:message></span>
</div>
<script type="text/javascript">
	$(function() {
		toShow();
	});

	function updateUserCheck() {
		var ret = true;
		$('#i18ncodeupdate span').each(function() {
			var flag = udpateUserPopError($(this).attr('class'));
			if (!flag) {
				ret = false;
				return ret;
			}
		});
		return ret;
	}

	function udpateUserPopError(inputName) {
		var $obj = $("#postform #" + inputName);
		var name = $obj.val();
		if (!name || name.length == 0) {
			$('#pop_error').html($('#i18ncodeupdate span.' + inputName).html());
			return false;
		}
		return true;
	}

	function udpateUser() {
		var flag = updateUserCheck();
		if (!flag) {
			return;
		}
		popSubmit();
	}
</script>
