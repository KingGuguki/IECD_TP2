<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    if (session.getAttribute("tp2_username") != null) {
        response.sendRedirect("menu.jsp");
    } else {
        response.sendRedirect("jogador.jsp");
    }
%>
