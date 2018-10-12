<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="false"%>
<%@taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
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
       <th scope="row">ID:</th>
       <td>
       <form:input path="id" class="input" readonly="true" maxlength="11"/>                      
        </td>      
    </tr>
    <tr> 
       <th scope="row">用户名:</th>
       <td>
       <form:input path="name" class="input" readonly="true" maxlength="11"/>                      
        </td>      
    </tr>
    <tr>
        <th scope="row">密码：</th>
        <td>
         <form:input type="password" path="password" class="input" maxlength="20"/>
       </td>
    </tr>
    </tbody>
 </table>
    <div class="pop_layer_ft"><a class="btn btn_submit not_close btn_blue" href="javascript:changePasswordSubmit();"><span class="">修改</span></a><a class="btn btn_white_2 btn_close" href="javascript:cancel();"><span>取消</span></a></div>
</form:form>
</div>
</div>
<script type="text/javascript">
$(function(){
	toShow();
});
function changePasswordSubmit(){
	var id = $('#postform').find('input[name="id"]').val();
	var password = $('#postform').find('input[name="password"]').val();
	var url = $('#postform').attr('action');
	if(password == ""){
		$('#pop_error').html('密码不可以为空');
	}else{
		$.post(url, {"id" : id, "password" : $.md5(password)}).success(function(data) {
			$('.content_main').html(data);
		});
	}
}
</script>
