<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="false"%>
<%@taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<link href="${pageContext.request.contextPath}/css/css.css" rel="stylesheet" media="screen">
<div class="pop_layer_main">
<div class="pop_layer_title">
   <h3>游戏服管理
   <span id="pop_error" class="tip_error"></span>
   </h3>
</div>
<div class="pop_layer_cont">
<form:form method="POST" modelAttribute="area" id="postform">
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
       <th scope="row">区名:</th>
       <td>
       <form:input path="name" class="input" maxlength="11"/>                      
        </td>      
    </tr>
    <tr>
        <th scope="row">类型：</th>
        <td>
         <form:input path="type" class="input" maxlength="11"/>
       </td> 
    </tr>
    <tr>
        <th scope="row">IP：</th>
        <td>
         <form:input path="ip" class="input" maxlength="20"/>
       </td> 
    </tr>
    <tr>
        <th scope="row">端口：</th>
        <td>
         <form:input path="port" class="input" maxlength="11"/>
       </td> 
    </tr>
    <tr>
        <th scope="row">平台：</th>
        <td>
         <form:input path="pid" class="input" maxlength="11"/>
       </td> 
    </tr>
    <tr>
        <th scope="row">状态：</th>
        <td>
                          是 <form:radiobutton path="valid" value="true" style="border:0px;"/>
        否    <form:radiobutton path="valid" value="false" style="border:0px;"/>
<!--          <form:input path="valid" class="input" maxlength="11"/> -->
       </td> 
    </tr>
    <tr>
        <th scope="row">描述：</th>
        <td>
           <form:textarea path="description" class="textarea" style="height:80px;"/>
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
	toShow();
});
</script>
