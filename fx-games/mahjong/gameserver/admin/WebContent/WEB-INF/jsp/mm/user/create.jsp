<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="false"%>
<%@taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="pop_layer_main">
<div class="pop_layer_title">
   <h3>账号管理
   <span id="pop_error" class="tip_error"></span>
   </h3>
</div>
<div class="pop_layer_cont">
<form:form method="POST" modelAttribute="user" id="postform">
<table class="ui_formTable"> 
<!-- 请指定各个列宽 -->
<colgroup>
    <col style="width:80px"> 
    <col style="width:auto">
    </colgroup><tbody>
<!--     <tr> 
       <th scope="row">ID:</th>
       <td>
       <form:input path="id" class="input" maxlength="11"/>                      
        </td>      
    </tr>
     -->
    <tr> 
       <th scope="row">用户名:</th>
       <td>
       <form:input path="name" class="input" maxlength="40"/>                      
        </td>      
    </tr>
    <tr>
        <th scope="row">昵称：</th>
        <td>
         <form:input path="nickName" class="input" maxlength="40"/>
       </td> 
    </tr>
    <tr>
        <th scope="row">密码：</th>
        <td>
         <form:input type="password" id="createPassword" path="password" class="input" maxlength="40"/>
       </td> 
    </tr>
    <tr>
        <th scope="row">邮件：</th>
        <td>
         <form:input path="email" class="input" maxlength="40"/>
       </td> 
    </tr>
    <tr>
        <th scope="row">角色：</th>
        <td>
          <form:select path="roleId">
              <c:forEach items = "${roleList }" var="role">
                  <form:option value="${role.id }">${role.name }</form:option>
              </c:forEach>
          </form:select>
       </td> 
    </tr>
    <tr>
        <th scope="row">平台：</th>
        <td>
         <form:select path="platformIdList">
             <c:forEach items="${platformList }" var="platform">
                 <form:option value="${platform.id }">${platform.name }</form:option>
             </c:forEach>
         </form:select>
       </td> 
    </tr>
    </tbody>
 </table>
    <div class="pop_layer_ft"><a class="btn btn_submit not_close btn_blue" href="javascript:createUser();"><span class="">增加</span></a><a class="btn btn_white_2 btn_close" href="javascript:cancel();"><span>取消</span></a></div>
</form:form>
</div>
</div>
<div id="i18ncode" style="display:none;">
    <span class="name"><spring:message code="nullcheck.name"></spring:message></span>
    <span class="nickName"><spring:message code="nullcheck.nickName"></spring:message></span>
    <span class="createPassword"><spring:message code="nullcheck.password"></spring:message></span>
    <span class="email"><spring:message code="nullcheck.email"></spring:message></span>
    <span class="roleId"><spring:message code="nullcheck.roleId"></spring:message></span>
    <span class="platformIdList"><spring:message code="nullcheck.platformIdList"></spring:message></span>
</div>
<script type="text/javascript">
$(function(){
	toShow();
});

function createUserCheck(){
	var ret = true;
	$('#i18ncode span').each(function(){
		var flag = createUserPopError($(this).attr('class'));
		   if(!flag){
			   ret = false;
			   return ret;
		   }
	});
	return ret;	
}

function createUserPopError(inputName){
	var $obj = $("#postform #" + inputName);
	var name = $obj.val();
	if(!name || name.length == 0){
		$('#pop_error').html($('#i18ncode span.' + inputName).html());
		return false;
	}
	return true;
}

function createUser(){
	var flag = createUserCheck();
	if(!flag){
		return;
	}
	createPassword();
	popSubmit();
}

function createPassword(){
	$('#createPassword').val($.md5($('#createPassword').val()));
}
</script>
