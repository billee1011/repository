<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="false"%>
<%@taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<div class="pop_layer_main">
<div class="pop_layer_title">
   <h3>权限管理
   <span id="pop_error" class="tip_error"></span>
   </h3>
</div>
<div class="pop_layer_cont">
<form:form method="POST" modelAttribute="role" id="postform">
<table class="ui_formTable"> 
<!-- 请指定各个列宽 -->
<colgroup>
    <col style="width:80px"> 
    <col style="width:auto">
    </colgroup><tbody>
    <tr> 
       <th scope="row">角色名：</th>
       <td>
       <form:input path="name" class="input" maxlength="11"/>                      
        </td>      
    </tr>
    <tr>
        <th scope="row">描述：</th>
        <td>
           <form:textarea path="description" class="textarea" style="height:160px;"/>
        </td>
    </tr>
    </tbody>
    
 </table>
    <div class="pop_layer_ft"><a class="btn btn_submit not_close btn_blue" href="javascript:popSubmit();"><span class="">增加</span></a><a class="btn btn_white_2 btn_close" href="javascript:cancel();"><span>取消</span></a></div>
</form:form>
</div>
</div>
<script type="text/javascript">
$(function(){
	toShow('.pop_layer_title');
});
</script>
