<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%request.setAttribute("ctx", request.getContextPath());%>
<div class="selectarea">
   <c:forEach items="${globalAreas }" var="server" varStatus="areaStatus">
       <a href="javascript:;" class="selectspan onlyselectspan multiSelectArea withId_${server.worldId}" areaId="${server.worldId }"  onclick="multiChooseGlobal(this, '${server.worldId}', 'ghostStore', 'mulSelectGlobalDiv');" title="<spring:message code="server.status.${server.status}"></spring:message>">
              <c:choose>
                  <c:when test="${server.status == 0}"> 
		              <img alt="" src="${pageContext.request.contextPath}/images/home/leader_run.png"/>
                  </c:when>
                  <c:when test="${server.status == 1}"> 
		              <img alt="" src="${pageContext.request.contextPath}/images/home/leader_maintain.png"/>
                  </c:when>
                  <c:otherwise>
		              <img alt="" src="${pageContext.request.contextPath}/images/home/leader_stop.png"/>
                  </c:otherwise>
              </c:choose>
             ${server.worldId }-${server.worldName }
       </a>
       <c:if test="${(areaStatus.index + 1) % 10 == 0 }"><br/></c:if>
   </c:forEach>
</div>
<div class="pagearea">
    <c:if test="${curPage > 1 }">
       <a href="javascript:toSelMultiGlobal(1, '${ctx }');">首页</a>
        <a href="javascript:toSelMultiGlobal(${curPage - 1 }, '${ctx }');">上一页</a>
    </c:if>
    <c:forEach items="${displayPages }" var="displayPage">
        <c:choose>
           <c:when test="${displayPage == curPage }">${displayPage }</c:when>
           <c:otherwise><a href="javascript:toSelMultiGlobal(${displayPage }, '${ctx }');">${displayPage }</a></c:otherwise>
        </c:choose>
    </c:forEach>
    <c:if test="${curPage < totalPage }">
        <a href="javascript:toSelMultiGlobal(${curPage + 1 }, '${ctx }');">下一页</a>
        <a href="javascript:toSelMultiGlobal(${totalPage }, '${ctx }');">尾页</a>
    </c:if>
</div>
<script type="text/javascript">
    $(function(){
    	var $dialog = $('#mulSelectGlobalDiv');
    	var $virginSet = $('.virginSet:first');
   	    if($virginSet.size() > 0 && $virginSet.val().length == 0){
   	    	$virginSet.val('1');
   	    	$dialog.find('.defaultSelectArea').click();
   	    }
   	    autoSelectSelectedGlobals('ghostStore');
    });
</script>