<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%request.setAttribute("ctx", request.getContextPath());%>
<!-- 要实现多选服的页面要include这个页面 -->
<div id="mulSelectAreaDiv" style="display:none;" class="pop_layer_v2">
    <input class="ghostStore" type="hidden"/>
    <input class="virginSet" type="hidden"/>
    <input class="defaultSelectFlag" type="hidden"/>
    <div class="pop_layer_main">
		<div class="pop_layer_title">
  		  <h3>选服管理
           <span id="pop_error" class="tip_error"></span>
          </h3>
        </div>
        <div class="pop_layer_cont">
        	<input type="checkbox" class="btn_select_16" onchange="allSelectOrNot(this)"/>当前页全选
        	<input type="checkbox" class="btn_select_16 allAreaOption"/>选择所有区（选中这个所有服操作）
        </div>
        <div class="muliDisplayArea pop_layer_cont">
            
        </div>
     </div>
<div class="pop_layer_ft"><a class="btn btn_submit not_close btn_blue" href="javascript:multiAssure();"><span class="">确定</span></a><a class="btn btn_white_2 btn_close" href="javascript:multiCancel();"><span>取消</span></a></div>
</div>
<script type="text/javascript">
    //往areaCache里放入默认的选区
    multiAreaReset();
     var $displayCurArea = $('#displayCurrentArea span');
     var areaId = $($displayCurArea[0]).html();
     if(areaId && areaId.length > 0){
    	 var id = parseInt(areaId);
		  areaCache[id] = id;
     }
     var worldId = $($displayCurArea[1]).html();
     if(worldId && worldId.length > 0){
    	 var id = parseInt(worldId);
    	 worldCache[id] = id;
     }
</script>
