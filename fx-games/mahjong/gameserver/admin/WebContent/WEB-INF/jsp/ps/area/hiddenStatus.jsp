<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<div id="hideStatusDiv" style="display:none;">
   <div class="image_0">
        <img alt="" style="vertical-align: middle;margin-right:10px;" src="${pageContext.request.contextPath}/images/manage_center/running.jpg"><spring:message code="mm.area.running" text="运行中"></spring:message>
    </div>
    <div class="image_1">
        <img alt="" style="vertical-align: middle;margin-right:10px;" src="${pageContext.request.contextPath}/images/manage_center/maintain.jpg"><spring:message code="mm.area.maintain" text="维护中"></spring:message>
    </div>
    <div class="image_2">
        <img alt="" style="vertical-align: middle;margin-right:10px;" src="${pageContext.request.contextPath}/images/manage_center/stop.jpg"><spring:message code="mm.area.stop" text="停止"></spring:message>
    </div>
</div>