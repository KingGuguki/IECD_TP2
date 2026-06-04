<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" deferredSyntaxAllowedAsLiteral="true" %>
<%
    // Garante que o utilizador está autenticado antes de carregar a página
    String username = (String) session.getAttribute("tp2_username");
    if (username == null) {
        response.sendRedirect("login.jsp");
        return;
    }
%>
<!DOCTYPE html>
<html lang="pt">
<head>
    <meta charset="UTF-8">
    <title>Hall of Fame</title>
    <style>
        :root {
            --bg: #0b1120;
            --panel: rgba(17, 24, 39, 0.9);
            --accent: #38bdf8;
            --text: #e5e7eb;
            --muted: #94a3b8;
        }
        body {
            margin: 0;
            font-family: "Segoe UI", Arial, sans-serif;
            background: radial-gradient(circle at top, #1e293b 0, #0f172a 44%, #020617 100%);
            color: var(--text);
            min-height: 100vh;
        }
        .wrap {
            max-width: 900px;
            margin: 0 auto;
            padding: 48px 20px 56px;
        }
        .header-bar {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 30px;
        }
        .back-btn {
            background: rgba(255, 255, 255, 0.1);
            color: #fff;
            text-decoration: none;
            padding: 8px 16px;
            border-radius: 8px;
            font-size: 0.9rem;
            transition: background 0.2s;
        }
        .back-btn:hover {
            background: rgba(255, 255, 255, 0.2);
        }
        .card {
            background: var(--panel);
            border: 1px solid rgba(148, 163, 184, 0.18);
            border-radius: 18px;
            padding: 24px;
            box-shadow: 0 18px 60px rgba(0,0,0,.35);
            backdrop-filter: blur(10px);
        }
        h1 {
            margin: 0 0 12px;
            font-size: 2.2rem;
            letter-spacing: -0.03em;
            text-align: center;
            color: var(--accent);
        }
        table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 20px;
        }
        th, td {
            padding: 16px;
            text-align: left;
            border-bottom: 1px solid rgba(148, 163, 184, 0.1);
        }
        th {
            color: var(--muted);
            font-weight: 600;
            font-size: 0.9rem;
            text-transform: uppercase;
            letter-spacing: 0.05em;
        }
        td {
            font-size: 1.05rem;
        }
        .avatar {
            width: 40px;
            height: 40px;
            border-radius: 50%;
            object-fit: cover;
            vertical-align: middle;
            margin-right: 12px;
            background: #1e293b;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            font-weight: bold;
            color: #fff;
        }
        .rank {
            font-size: 1.5rem;
            font-weight: bold;
            text-align: center;
            width: 50px;
        }
        .rank-1 { color: #fbbf24; }
        .rank-2 { color: #94a3b8; }
        .rank-3 { color: #b45309; }
        .user-info {
            display: flex;
            align-items: center;
        }
        .stats-highlight {
            color: #4ade80;
            font-weight: bold;
        }
        .stats-low {
            color: #f87171;
        }
        .stats-neutral {
            color: #fbbf24;
        }
        .loading {
            text-align: center;
            padding: 40px;
            color: var(--muted);
        }
    </style>
</head>
<body>
<div class="wrap">
    <div class="header-bar">
        <a href="menu.jsp" class="back-btn">← Voltar ao Painel</a>
    </div>
    
    <div class="card">
        <h1>🏆 Hall of Fame</h1>
        <p style="text-align: center; color: var(--muted); margin-bottom: 30px;">Os melhores jogadores do servidor, ordenados por vitórias.</p>
        
        <div id="tableContainer">
            <div class="loading">A carregar estatísticas...</div>
        </div>
    </div>
</div>

<script>
    document.addEventListener("DOMContentLoaded", () => {
        // Concatenamos diretamente o context path injetado pelo Java de forma segura
        const urlFinal = "<%= request.getContextPath() %>/halloffame";

        fetch(urlFinal)
            .then(r => {
                if (!r.ok) {
                    throw new Error("Erro do Servidor: " + r.status);
                }
                return r.json();
            })
            .then(data => {
                if (data.error) {
                    document.getElementById("tableContainer").innerHTML = "<p style='color:red; text-align:center;'>Erro: " + data.error + "</p>";
                    return;
                }
                
                let html = `
                    <table>
                        <thead>
                            <tr>
                                <th style="text-align:center">Posição</th>
                                <th>Jogador</th>
                                <th>Vitórias</th>
                                <th>Derrotas</th>
                                <th>Empates</th>
                                <th>Tempo Médio</th>
                            </tr>
                        </thead>
                        <tbody>
                `;
                
                data.forEach((p, index) => {
                    const rank = index + 1;
                    let rankClass = "";
                    let rankIcon = rank;
                    
                    if (rank === 1) { rankClass = "rank-1"; rankIcon = "🥇"; }
                    else if (rank === 2) { rankClass = "rank-2"; rankIcon = "🥈"; }
                    else if (rank === 3) { rankClass = "rank-3"; rankIcon = "🥉"; }
                    
                    // Escapamos o hífen e os cifrões para o JSP antigo não estragar a concatenação
                    const avatarHtml = p.photo 
                        ? "<img src='data:image/jpeg;base64," + p.photo + "' class='avatar'>" 
                        : "<div class='avatar'>" + p.username.charAt(0).toUpperCase() + "</div>";
                        
                    html += "<tr>" +
                            "<td class='rank " + rankClass + "'>" + rankIcon + "</td>" +
                            "<td>" +
                                "<div class='user-info'>" +
                                    avatarHtml +
                                    "<div>" +
                                        "<strong>" + p.username + "</strong><br>" +
                                        "<span style='font-size:0.85rem; color:var(--muted)'>" + (p.nationality || 'Desconhecida') + "</span>" +
                                    "</div>" +
                                "</div>" +
                            "</td>" +
                            "<td class='stats-highlight'>" + p.vitorias + "</td>" +
                            "<td class='stats-low'>" + p.derrotas + "</td>" +
                            "<td class='stats-neutral'>" + p.empates + "</td>" +
                            "<td>" + (p.tempoMedio > 0 ? p.tempoMedio.toFixed(1) + 's' : '-') + "</td>" +
                        "</tr>";
                });
                
                html += "</tbody></table>";
                document.getElementById("tableContainer").innerHTML = html;
            })
            .catch(err => {
                console.error(err);
                document.getElementById("tableContainer").innerHTML = "<p style='color:red; text-align:center;'>Erro de ligação ao carregar dados.</p>";
            });
    });
</script>
</body>
</html>