<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<html>
    <head>
        <title>无权访问</title>
    </head>
    <body>
    <%
            out.println("对不起,你没有权限访问该功能.<br/><br/>");
     
    %>
    <a href="javascript:void(0);" onclick="history.go(-1)">返回刚才页面</a><br/><br/>
    </body>
</html>