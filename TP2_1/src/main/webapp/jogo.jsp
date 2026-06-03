<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    String username = (String) session.getAttribute("tp2_username");
    String simbolo = (String) session.getAttribute("tp2_simbolo");
    if (username == null || simbolo == null) {
        response.sendRedirect("jogador.jsp");
        return;
    }
%>
<!DOCTYPE html>
<html lang="pt">
<head>
    <meta charset="UTF-8">
    <title>Área de Jogo</title>
    <style>
        body {
            margin: 0;
            font-family: "Segoe UI", Arial, sans-serif;
            background: radial-gradient(circle at top, #1e293b 0, #0f172a 44%, #020617 100%);
            color: #e5e7eb;
        }
        .wrap {
            max-width: 900px;
            margin: 0 auto;
            padding: 48px 20px 56px;
        }
        .card {
            background: rgba(17, 24, 39, 0.9);
            border: 1px solid rgba(148, 163, 184, 0.18);
            border-radius: 18px;
            padding: 24px;
            box-shadow: 0 18px 60px rgba(0,0,0,.35);
            backdrop-filter: blur(10px);
        }
        h1 { margin: 0 0 12px; }
        p { color: #94a3b8; line-height: 1.55; }
        .actions { display: flex; gap: 10px; flex-wrap: wrap; margin-top: 18px; }
        a {
            border-radius: 12px;
            padding: 12px 16px;
            font-weight: 600;
            text-decoration: none;
            background: rgba(148, 163, 184, 0.14);
            color: #e5e7eb;
        }
    </style>
</head>
<body>
<div class="wrap">
    <div class="card">
        <h1>Área de jogo</h1>
        <p>Jogador: <strong><%= username %></strong> | Símbolo: <strong><%= simbolo %></strong></p>
        <p>A interface do jogo vem a seguir. Nesta fase a página serve para confirmar a navegação do painel.</p>
        <div class="actions">
            <a href="menu.jsp">Voltar ao painel</a>
            <a href="jogador.jsp?acao=logout">Terminar sessão</a>
        </div>
    </div>
</div>
</body>
</html>
