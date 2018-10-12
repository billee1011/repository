<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="false"%>
<%@taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<div class="pop_layer_main">
<div class="pop_layer_title">
   <h3>权限管理
   <span id="pop_error" class="tip_error"></span>
   </h3>
</div>
<div class="pop_layer_cont">
    <div class="tip" style="margin-bottom: 10px;padding-bottom: 0px;padding-top: 0px; text-align: center;">正在更新账号'${username }'的权限</div>
    <form action="${pageContext.request.contextPath}/mm/user/updatePrivilege.do" method="post" id="postform">
    <input type="hidden" name="id" value="${userid }"/>
    <table class="ui_formTable">
       <tr>
        <c:forEach items="${moduleList}" var="moduleVO" varStatus="status" >
        <td style="width:10%;">
            <ul id="black" class="treeview" style="display:inline;">
		         <li class="collapsable" style="display:inline;">
		         <input type="checkbox" name="privilegeListParent" value="${moduleVO.code }" checkname="check_branch_${status.index }" checkroot="root"/>${moduleVO.name }
		         <ul>
		         <c:forEach items="${moduleVO.menuDTOList}" var="menuDTO" varStatus="menuStatus">
		             <li>
		             <input type="checkbox" name="privilegeList" value="${menuDTO.code }" <c:if test="${menuDTO.access}">checked="checked"</c:if> checkname="check_branch_${status.index }_${menuStatus.index}" checkleaf="check_leaf_${status.index }"/>${menuDTO.name }
		             </li>
		         </c:forEach>
		         </ul>
		         </li>
		    </ul>
        </td>
        </c:forEach>
        </tr>
        </table>
        <div class="pop_layer_ft"><a class="btn btn_submit not_close btn_blue" href="javascript:popSubmit();"><span class="">修改</span></a><a class="btn btn_white_2 btn_close" href="javascript:cancel();"><span>取消</span></a></div>
    </form>
</div>
</div>
<script type="text/javascript">
$(function(){
	parentBindChangeEvent("check_branch_");
	checkRoot("root");
	toShow();
});
</script>
