<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="user.User, util.XMLDoc"%>
<%!
    private String obterCorFundo(User jogador) {
        try {
            Object valor = jogador.getClass().getMethod("getCorFundo").invoke(jogador);
            if (valor instanceof String && !((String) valor).isBlank()) {
                return (String) valor;
            }
        } catch (ReflectiveOperationException e) {
            // Compatibilidade com classes compiladas antes da personalização do perfil.
        }
        return "#0F172A";
    }
%>
<%
    XMLDoc.setContextoReal(getServletContext().getRealPath("/"));

    String username = (String) session.getAttribute("tp2_username");
    String simbolo = (String) session.getAttribute("tp2_simbolo");

    // Apenas validamos se o username não existe. O jogador pode estar no Menu 
    // perfeitamente válido mesmo antes de procurar partida (e portanto sem símbolo)
    if (username == null) {
        response.sendRedirect("login.jsp");
        return;
    }

    User jogador = null;
    String fotoBase64 = "";
    String cor = "#0F172A";
    String nomeCompleto = "";
    String nacionalidade = "";

    int vitorias = 0;
    int derrotas = 0;
    int empates = 0;
    double tempoMedio = 0;

    try {
        User._load();
        jogador = User._obtain(username);
        if (jogador.getPhotography() != null) {
            fotoBase64 = jogador.getPhotography().getBase64();
        }
        cor = obterCorFundo(jogador);
        nomeCompleto = jogador.getName();
        try {
            nacionalidade = jogador.getPtNationality();
        } catch (Exception e) {
            nacionalidade = "";
        }
        
        vitorias = jogador.getVitorias();
        derrotas = jogador.getDerrotas();
        empates = jogador.getEmpates();
        int tempo = jogador.getTempo();
        int totalJogos = vitorias + derrotas + empates;
        if (totalJogos > 0) {
            tempoMedio = (double) tempo / totalJogos;
        }
    } catch (Exception e) {
        response.sendRedirect("login.jsp");
        return;
    }
%>
<!DOCTYPE html>
<html lang="pt">
<head>
    <meta charset="UTF-8">
    <title>Painel do Jogador</title>
    <style>
        :root {
            --bg: #0b1120;
            --panel: rgba(17, 24, 39, 0.9);
            --panel-2: rgba(30, 41, 59, 0.88);
            --accent: <%= cor %>;
            --text: #e5e7eb;
            --muted: #94a3b8;
        }
        body {
            margin: 0;
            font-family: "Segoe UI", Arial, sans-serif;
            background: radial-gradient(circle at top, #1e293b 0, #0f172a 44%, #020617 100%);
            color: var(--text);
        }
        .wrap {
            max-width: 1100px;
            margin: 0 auto;
            padding: 48px 20px 56px;
        }
        .hero {
            display: grid;
            gap: 18px;
            grid-template-columns: 1fr 1fr;
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
            font-size: 2rem;
            letter-spacing: -0.03em;
        }
        p {
            margin: 0 0 10px;
            color: var(--muted);
            line-height: 1.55;
        }
        .pill {
            display: inline-flex;
            align-items: center;
            gap: 8px;
            padding: 6px 10px;
            border-radius: 999px;
            background: rgba(56, 189, 248, 0.14);
            color: #bae6fd;
            font-size: 0.85rem;
            margin-bottom: 14px;
        }
        .avatar {
            width: 112px;
            height: 112px;
            border-radius: 18px;
            overflow: hidden;
            border: 4px solid var(--accent);
            background: rgba(15, 23, 42, 0.9);
            display: flex;
            align-items: center;
            justify-content: center;
            margin-bottom: 14px;
        }
        .avatar img {
            width: 100%;
            height: 100%;
            object-fit: cover;
            display: block;
        }
        .avatar span {
            font-size: 2.1rem;
            font-weight: 700;
            color: #dbeafe;
        }
        .grid {
            display: grid;
            gap: 14px;
        }
        .action {
            display: block;
            text-decoration: none;
            border-radius: 16px;
            padding: 16px 18px;
            border: 1px solid rgba(148, 163, 184, 0.18);
            background: var(--panel-2);
            color: var(--text);
        }
        .action strong {
            display: block;
            font-size: 1.05rem;
            margin-bottom: 4px;
        }
        .action span {
            color: var(--muted);
            font-size: 0.95rem;
        }
        .action.primary {
            border-color: rgba(56, 189, 248, 0.28);
            box-shadow: inset 0 0 0 1px rgba(56, 189, 248, 0.1);
        }
        .swatch {
            width: 18px;
            height: 18px;
            display: inline-block;
            border-radius: 999px;
            background: var(--accent);
            vertical-align: middle;
            margin-right: 8px;
            border: 1px solid rgba(255,255,255,.35);
        }
        .topline {
            display: flex;
            flex-wrap: wrap;
            gap: 10px;
            align-items: center;
            margin-bottom: 16px;
        }
        .muted {
            color: var(--muted);
        }
        @media (max-width: 850px) {
            .hero { grid-template-columns: 1fr; }
        }
    </style>
</head>
<body>
<div class="wrap">
    <div class="hero">
        <div class="card">
            <div class="pill">Painel do jogador</div>
            <h1>Bem-vindo, <%= username %></h1>
            <div class="topline">
                <span class="muted"><span class="swatch"></span>Cor definida: <%= cor %></span>
            </div>

            <div class="avatar">
                <% if (fotoBase64 != null && !fotoBase64.isBlank()) { %>
                    <img src="data:image/jpeg;base64,<%= fotoBase64 %>" alt="Fotografia de perfil">
                <% } else { %>
                    <span><%= username.substring(0, 1).toUpperCase() %></span>
                <% } %>
            </div>

            <p>Nome completo: <%= nomeCompleto %></p>
            <% if (nacionalidade != null && !nacionalidade.isBlank()) { %>
                <p>Nacionalidade: <%= nacionalidade %></p>
            <% } %>
            
            <div style="margin-top: 16px; padding: 12px; background: rgba(0,0,0,0.2); border-radius: 12px; display: flex; gap: 16px; justify-content: space-around;">
                <div style="text-align: center;">
                    <div style="color: #4ade80; font-weight: bold; font-size: 1.2rem;"><%= vitorias %></div>
                    <div style="font-size: 0.8rem; color: var(--muted); text-transform: uppercase;">Vitórias</div>
                </div>
                <div style="text-align: center;">
                    <div style="color: #f87171; font-weight: bold; font-size: 1.2rem;"><%= derrotas %></div>
                    <div style="font-size: 0.8rem; color: var(--muted); text-transform: uppercase;">Derrotas</div>
                </div>
                <div style="text-align: center;">
                    <div style="color: #fbbf24; font-weight: bold; font-size: 1.2rem;"><%= empates %></div>
                    <div style="font-size: 0.8rem; color: var(--muted); text-transform: uppercase;">Empates</div>
                </div>
                <div style="text-align: center;">
                    <div style="color: #60a5fa; font-weight: bold; font-size: 1.2rem;"><%= tempoMedio > 0 ? String.format("%.1f", tempoMedio) + "s" : "-" %></div>
                    <div style="font-size: 0.8rem; color: var(--muted); text-transform: uppercase;">Tempo Médio</div>
                </div>
            </div>
        </div>

        <div class="card">
            <h1 style="font-size:1.2rem;">Ações disponíveis</h1>
            <div class="grid">
                <a class="action primary" href="jogo.jsp">
                    <strong>Iniciar jogo</strong>
                    <span>Avança para a área de jogo da sessão atual.</span>
                </a>
                <a class="action" href="perfil.jsp">
                    <strong>Editar perfil</strong>
                    <span>Atualiza os dados pessoais, a fotografia e a cor de fundo preferida.</span>
                </a>
                <a class="action" href="halloffame.jsp" style="border-color: rgba(251, 191, 36, 0.4);">
                    <strong style="color: #fbbf24;">🏆 Hall of Fame</strong>
                    <span>Vê o ranking dos melhores jogadores e os seus tempos médios.</span>
                </a>
                <a class="action" href="login.jsp?acao=logout">
                    <strong>Terminar sessão</strong>
                    <span>Fecha a ligação ao servidor e limpa a sessão.</span>
                </a>
            </div>
        </div>
    </div>
</div>
</body>
<script>
    document.addEventListener("DOMContentLoaded", () => {
        let isInviteOpen = false;

        function checkInvites() {
            if (isInviteOpen) return;
            fetch("lobby?action=verificar_convites")
                .then(r => r.json())
                .then(data => {
                    if (data.convite && data.convite !== "null" && !isInviteOpen) {
                        isInviteOpen = true;
                        showInviteModal(data.convite);
                    }
                })
                .catch(err => console.error("Erro no polling de convites:", err));
        }

        function showInviteModal(inviter) {
            const overlay = document.createElement("div");
            overlay.style.cssText = "position:fixed; top:0; left:0; width:100%; height:100%; background:rgba(0,0,0,0.7); backdrop-filter:blur(5px); z-index:9999; display:flex; align-items:center; justify-content:center;";
            
            const modal = document.createElement("div");
            modal.style.cssText = "background:var(--panel); border:1px solid rgba(148, 163, 184, 0.2); border-radius:18px; padding:32px; box-shadow:0 25px 50px rgba(0,0,0,0.5); text-align:center; max-width:400px; width:90%; color:var(--text); animation: popIn 0.3s ease-out;";
            
            modal.innerHTML = `
                <div style="font-size:3rem; margin-bottom:16px;">⚔️</div>
                <h2 style="margin:0 0 12px; font-size:1.5rem;">Foste Desafiado!</h2>
                <p style="color:var(--muted); margin-bottom:24px; font-size:1.1rem;">O jogador <strong style="color:var(--accent);">@\${inviter}</strong> convidou-te para uma partida privada!</p>
                <div style="display:flex; gap:12px; justify-content:center;">
                    <button id="btnRecusarConvite" style="padding:12px 24px; border-radius:10px; border:none; background:rgba(255,255,255,0.1); color:white; font-weight:600; cursor:pointer; flex:1;">Ignorar</button>
                    <button id="btnAceitarConvite" style="padding:12px 24px; border-radius:10px; border:none; background:var(--accent); color:black; font-weight:bold; cursor:pointer; flex:1; box-shadow:0 4px 15px rgba(255,255,255,0.2);">Aceitar Desafio</button>
                </div>
            `;
            
            overlay.appendChild(modal);
            document.body.appendChild(overlay);

            document.getElementById("btnRecusarConvite").addEventListener("click", () => {
                document.body.removeChild(overlay);
                isInviteOpen = false;
            });

            document.getElementById("btnAceitarConvite").addEventListener("click", () => {
                const btn = document.getElementById("btnAceitarConvite");
                btn.disabled = true;
                btn.innerHTML = "A entrar...";
                
                fetch("lobby?action=aceitar_desafio&de=" + encodeURIComponent(inviter))
                    .then(r => r.json())
                    .then(data => {
                        if (data.status === "ok") {
                            window.location.href = "jogo.jsp";
                        } else {
                            alert("Erro ao aceitar convite: " + data.error);
                            document.body.removeChild(overlay);
                            isInviteOpen = false;
                        }
                    })
                    .catch(err => {
                        console.error(err);
                        alert("Ocorreu um erro de ligação.");
                        document.body.removeChild(overlay);
                        isInviteOpen = false;
                    });
            });
        }

        const style = document.createElement('style');
        style.innerHTML = `@keyframes popIn { 0% { transform: scale(0.8); opacity: 0; } 100% { transform: scale(1); opacity: 1; } }`;
        document.head.appendChild(style);

        // Verifica os convites a cada 3 segundos
        setInterval(checkInvites, 3000);
        checkInvites();
    });
</script>
</html>
