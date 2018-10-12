<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%request.setAttribute("ctx", request.getContextPath());%>
<div style="display:none;">
    <form action="${ctx }/ps/area/downloadconfig.do" id="downloadForm" method="get">
        <input type="text" name="servertype"/>
        <input type="text" name="worldid"/>
    </form>
</div>