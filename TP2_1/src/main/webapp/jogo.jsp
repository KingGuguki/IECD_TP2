<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="user.User" %>
<%
    String username = (String) session.getAttribute("tp2_username");
    if (username == null) {
        response.sendRedirect("login.jsp");
        return;
    }

    User jg = User._obtain(username);
    String corFavorita = (jg != null && jg.getCorFundo() != null) ? jg.getCorFundo() : "#3b82f6";

    String simbolo = (String) session.getAttribute("tp2_simbolo");
%>
<!DOCTYPE html>
<html lang="pt">
<head>
    <meta charset="UTF-8">
    <title>Pontos e Caixas - Web</title>
    <style>
        :root {
            --accent: <%= corFavorita %>;
            --accent-glow: <%= corFavorita %>80;
            --enemy: #f43f5e;
            --enemy-glow: rgba(244, 63, 94, 0.5);
            --bg: #0f172a;
            --surface: #1e293b;
            --muted: #64748b;
        }
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
        .actions { display: flex; gap: 10px; flex-wrap: margin-top: 18px; }
        a {
            border-radius: 12px;
            padding: 12px 16px;
            font-weight: 600;
            text-decoration: none;
            background: rgba(148, 163, 184, 0.14);
            color: #e5e7eb;
        }
        
        .suggestion-item {
            display: flex;
            align-items: center;
            gap: 12px;
            padding: 10px 14px;
            border-bottom: 1px solid rgba(148, 163, 184, 0.18);
            cursor: pointer;
            transition: background 0.2s;
        }
        .suggestion-item:last-child { border-bottom: none; }
        .suggestion-item:hover { background: rgba(56, 189, 248, 0.1); }
        .suggestion-photo {
            width: 36px;
            height: 36px;
            border-radius: 50%;
            object-fit: cover;
            border: 2px solid var(--accent);
            background: #0f172a;
        }
        .suggestion-placeholder {
            width: 36px;
            height: 36px;
            border-radius: 50%;
            background: #0f172a;
            border: 2px solid var(--accent);
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 0.85rem;
            font-weight: bold;
            color: #bae6fd;
        }
        .suggestion-info { flex: 1; display: flex; flex-direction: column; }
        .suggestion-name { font-weight: 600; font-size: 0.95rem; color: #e5e7eb; }
        .suggestion-nick { font-size: 0.8rem; color: #94a3b8; }
        .no-suggestions { padding: 14px; text-align: center; color: #94a3b8; font-size: 0.9rem; }

        .board-container {
            position: relative;
            margin: 40px auto;
            background: rgba(15, 23, 42, 0.6);
            border-radius: 16px;
            border: 2px solid var(--accent);
            padding: 30px;
            box-shadow: 0 10px 40px rgba(0,0,0,0.5), inset 0 0 40px rgba(0,0,0,0.5), 0 0 25px var(--accent-glow);
            display: inline-block;
        }
        .board { position: relative; }
        .dot {
            position: absolute;
            width: 18px;
            height: 18px;
            background: #e5e7eb;
            border-radius: 50%;
            box-shadow: 0 0 10px rgba(255,255,255,0.2);
            z-index: 5;
            transform: translate(-50%, -50%);
        }
        .line {
            position: absolute;
            background: var(--surface);
            cursor: pointer;
            transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
            border-radius: 4px;
        }
        .line:not(.occupied):hover {
            background: var(--accent);
            box-shadow: 0 0 8px var(--accent-glow);
            transform: scale(1.1);
        }
        .line.occupied.mine {
            background: var(--accent);
            box-shadow: 0 0 10px var(--accent-glow);
            cursor: default;
        }
        .line.occupied.enemy {
            background: var(--enemy);
            box-shadow: 0 0 10px var(--enemy-glow);
            cursor: default;
        }
        
        .box {
            position: absolute;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 2.2rem;
            font-weight: 800;
            z-index: 3;
            border-radius: 8px;
            transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
            transform: scale(0.8);
            opacity: 0;
        }
        .box.owned { transform: scale(1); opacity: 1; }
        .box.my-box { color: var(--accent); background: var(--accent-glow); }
        .box.enemy-box { color: var(--enemy); background: var(--enemy-glow); }
        
        .status-bar {
            text-align: center;
            padding: 16px;
            font-size: 1.1rem;
            font-weight: 600;
            border-radius: 12px;
            margin-bottom: 20px;
            background: rgba(30, 41, 59, 0.8);
            border: 1px solid rgba(148, 163, 184, 0.18);
        }
        .status-win { background: rgba(34, 197, 94, 0.15); color: #86efac; border-color: rgba(34, 197, 94, 0.3); }
        .status-lose { background: rgba(248, 113, 113, 0.15); color: #fca5a5; border-color: rgba(248, 113, 113, 0.3); }
        .status-draw { background: rgba(250, 204, 21, 0.15); color: #fde047; border-color: rgba(250, 204, 21, 0.3); }
        @keyframes pulse { 0% { opacity: 1; } 50% { opacity: 0.4; } 100% { opacity: 1; } }
    </style>
</head>
<body>
<div class="wrap">
    <div class="card">
        <h1>Área de jogo</h1>
        <p>Jogador: <strong><%= username %></strong> 
            <% if (simbolo != null && !simbolo.isBlank()) { %> | Símbolo: <strong><%= simbolo %></strong> <% } %>
        </p>
        <div class="actions">
            <a href="#" onclick="abandonarPartida(); return false;" style="background: rgba(148, 163, 184, 0.14); color: #e5e7eb; padding: 10px 16px; border-radius: 12px; text-decoration: none; font-weight: 600; display: inline-block;">Voltar ao menu</a>
        </div>
        <script>
            function abandonarPartida() {
                fetch("lobby?action=sair_jogo")
                    .then(() => window.location.href = "menu.jsp")
                    .catch(() => window.location.href = "menu.jsp");
            }
        </script>
        
        <% if (simbolo != null && !simbolo.isBlank()) { %>
            <h2 style="font-size:1.4rem; margin-bottom: 4px; margin-top: 0;">Partida em curso</h2>
            <div id="gameStatus" class="status-bar">A carregar estado do jogo...</div>
            <div style="text-align: center;">
                <div class="board-container">
                    <div id="board" class="board"></div>
                </div>
            </div>
        <% } else { %>
            <hr style="border: 0; border-top: 1px solid rgba(148, 163, 184, 0.18); margin: 24px 0;">
            <h2 style="font-size:1.2rem; margin-bottom: 8px; margin-top: 0;">Fila Pública</h2>
            <button id="btnProcurarPartida" style="width: 100%; border-radius: 12px; padding: 12px 16px; font-weight: 600; cursor: pointer; border: 0; background: linear-gradient(135deg, var(--accent), #0ea5e9); color: #06263a;">Procurar Partida</button>
            <div id="loadingFila" style="display: none; margin-top: 14px; text-align: center; color: var(--accent);">
                <span style="display: inline-block; animation: pulse 1.5s infinite;">A procurar adversário...</span>
            </div>
            <hr style="border: 0; border-top: 1px solid rgba(148, 163, 184, 0.18); margin: 24px 0;">
            <h2 style="font-size:1.2rem; margin-bottom: 8px; margin-top: 0;">Desafiar Oponente</h2>
            <div style="position: relative;">
                <input type="text" id="opponentSearch" placeholder="Digita um nome..." autocomplete="off" style="width:100%; box-sizing:border-box; border:1px solid rgba(148,163,184,0.22); background:rgba(15,23,42,0.9); color:#e5e7eb; border-radius:12px; padding:12px 14px; outline:none;">
                <div id="suggestionsBox" style="position: absolute; left: 0; right: 0; top: 100%; background: #1e293b; border: 1px solid rgba(148, 163, 184, 0.3); border-radius: 12px; margin-top: 8px; max-height: 220px; overflow-y: auto; z-index: 100; display: none; box-shadow: 0 10px 25px rgba(0,0,0,0.5);"></div>
            </div>
        <% } %>
    </div>
</div>
<script>
    document.addEventListener("DOMContentLoaded", () => {
        const mySymbol = "<%= simbolo %>";
        if (mySymbol && mySymbol.trim() !== "" && mySymbol !== "null") {
            const boardEl = document.getElementById("board");
            const statusEl = document.getElementById("gameStatus");
            const CELL_SIZE = 70;
            let isGameOver = false;
            let timerInterval = null;
            let timeLeft = 30;

            function updateTimerDisplay() {
                if (isGameOver) return;
                statusEl.innerHTML = "A tua vez de jogar! Símbolo: <span style='color:var(--accent); font-size:1.4rem;'>" + mySymbol + "</span> <br><small style='color:var(--danger); font-weight:bold;'>Tempo restante: " + timeLeft + "s</small>";
            }

            function startTimer() {
                clearInterval(timerInterval);
                timeLeft = 30;
                updateTimerDisplay();
                timerInterval = setInterval(() => {
                    timeLeft--;
                    if (timeLeft <= 0) {
                        clearInterval(timerInterval);
                        isGameOver = true;
                        statusEl.className = "status-bar status-lose";
                        statusEl.innerHTML = "💀 Derrota por inatividade!";
                        document.getElementById("btnVoltarLobby")?.removeAttribute("style");
                    } else {
                        updateTimerDisplay();
                    }
                }, 1000);
            }

            function stopTimer() {
                clearInterval(timerInterval);
            }

            function parseXML(xmlStr) { return new DOMParser().parseFromString(xmlStr, "application/xml"); }

            function updateBoard(xmlDoc) {
                const tabuleiro = xmlDoc.getElementsByTagName("tabuleiro")[0];
                if (!tabuleiro) return;
                const linhasCnt = parseInt(tabuleiro.getAttribute("linhas"));
                const colunasCnt = parseInt(tabuleiro.getAttribute("colunas"));
                const estado = tabuleiro.getAttribute("estado");
                const vez = tabuleiro.getAttribute("vez");
                const corX = tabuleiro.getAttribute("corX") || "#3b82f6";
                const corO = tabuleiro.getAttribute("corO") || "#f43f5e";
                
                // Atualiza cores dinamicamente
                if (mySymbol === "X") {
                    document.documentElement.style.setProperty("--accent", corX);
                    document.documentElement.style.setProperty("--enemy", corO);
                } else if (mySymbol === "O") {
                    document.documentElement.style.setProperty("--accent", corO);
                    document.documentElement.style.setProperty("--enemy", corX);
                }
                
                boardEl.style.width = ((colunasCnt - 1) * CELL_SIZE) + "px";
                boardEl.style.height = ((linhasCnt - 1) * CELL_SIZE) + "px";
                
                if (estado.startsWith("V") || estado === "EM") {
                    isGameOver = true;
                    stopTimer();
                    boardEl.style.pointerEvents = "none";
                    boardEl.style.opacity = "0.7";
                    statusEl.className = "status-bar " + (estado === "EM" ? "status-draw" : (estado.charAt(1) === mySymbol ? "status-win" : "status-lose"));
                    if (estado === "EM") {
                        statusEl.innerHTML = "🤝 Empate!";
                    } else if (estado.charAt(1) === mySymbol) {
                        const todasLinhasOcupadas = Array.from(xmlDoc.getElementsByTagName("linha")).every(l => l.getAttribute("ocupada") === "true");
                        if (todasLinhasOcupadas) {
                            statusEl.innerHTML = "🎉 Vitória!";
                        } else {
                            statusEl.innerHTML = "🎉 Vitória por desistência (Tempo)!";
                        }
                    } else {
                        const todasLinhasOcupadas = Array.from(xmlDoc.getElementsByTagName("linha")).every(l => l.getAttribute("ocupada") === "true");
                        if (todasLinhasOcupadas) {
                            statusEl.innerHTML = "💀 Derrota!";
                        } else {
                            statusEl.innerHTML = "⏱️ Derrota por tempo!";
                        }
                    }
                    document.getElementById("btnVoltarLobby")?.removeAttribute("style");
                }

                let newHTML = "";
                const linhas = xmlDoc.getElementsByTagName("linha");
                const caixas = xmlDoc.getElementsByTagName("caixa");
                let idContador = 1;
                
                for (let i = 0; i < linhasCnt; i++) {
                    for (let j = 0; j < colunasCnt - 1; j++) {
                        let linhaEl = Array.from(linhas).find(l => l.getAttribute("tipo") === "H" && parseInt(l.getAttribute("linha")) === i && parseInt(l.getAttribute("coluna")) === j);
                        let isOcupada = linhaEl && linhaEl.getAttribute("ocupada") === "true";
                        let dono = isOcupada ? linhaEl.getAttribute("dono") : "";
                        let lineClass = "line " + (isOcupada ? "occupied " + (dono === mySymbol ? "mine" : "enemy") : "");
                        newHTML += "<div class='" + lineClass + "' style='left:" + (j*CELL_SIZE) + "px; top:" + (i*CELL_SIZE) + "px; width:" + CELL_SIZE + "px; height:8px;' " + (!isOcupada ? "onclick='fazerJogada(" + idContador + ")'" : "") + "></div>";
                        idContador++;
                    }
                    if (i < linhasCnt - 1) {
                        for (let j = 0; j < colunasCnt; j++) {
                            let linhaEl = Array.from(linhas).find(l => l.getAttribute("tipo") === "V" && parseInt(l.getAttribute("linha")) === i && parseInt(l.getAttribute("coluna")) === j);
                            let isOcupada = linhaEl && linhaEl.getAttribute("ocupada") === "true";
                            let dono = isOcupada ? linhaEl.getAttribute("dono") : "";
                            let lineClass = "line " + (isOcupada ? "occupied " + (dono === mySymbol ? "mine" : "enemy") : "");
                            newHTML += "<div class='" + lineClass + "' style='left:" + (j*CELL_SIZE) + "px; top:" + (i*CELL_SIZE) + "px; width:8px; height:" + CELL_SIZE + "px;' " + (!isOcupada ? "onclick='fazerJogada(" + idContador + ")'" : "") + "></div>";
                            idContador++;
                        }
                    }
                }
                // Caixas
                for (let i = 0; i < linhasCnt - 1; i++) {
                    for (let j = 0; j < colunasCnt - 1; j++) {
                        let dono = "";
                        for(let k=0; k<caixas.length; k++) {
                            if (parseInt(caixas[k].getAttribute("linha")) === i && 
                                parseInt(caixas[k].getAttribute("coluna")) === j) {
                                dono = caixas[k].getAttribute("dono");
                                break;
                            }
                        }
                        // O dono pode vir " " ou \0 do Java
                        if (dono && dono.trim().length > 0) {
                            let isMine = dono === mySymbol;
                            let boxClass = isMine ? 'my-box' : 'enemy-box';
                            newHTML += "<div class='box owned " + boxClass + "' style='left: " + (j * CELL_SIZE) + "px; top: " + (i * CELL_SIZE) + "px; width: " + CELL_SIZE + "px; height: " + CELL_SIZE + "px;'>" + dono + "</div>";
                        }
                    }
                }
                
                // Pontos
                for (let i = 0; i < linhasCnt; i++) {
                    for (let j = 0; j < colunasCnt; j++) {
                        newHTML += "<div class='dot' style='left: " + (j * CELL_SIZE) + "px; top: " + (i * CELL_SIZE) + "px;'></div>";
                    }
                }
                
                boardEl.innerHTML = newHTML;
                
                if (!isGameOver && vez) {
                    window.currentTurn = vez;
                    if (vez === mySymbol) {
                        startTimer();
                        boardEl.style.pointerEvents = "auto";
                        boardEl.style.opacity = "1";
                    } else {
                        stopTimer();
                        statusEl.innerHTML = "A aguardar jogada do adversário... (máx 30s)";
                        boardEl.style.pointerEvents = "none";
                        boardEl.style.opacity = "0.7";
                    }
                }
            }

            // Função global para o clique
            window.fazerJogada = function(linhaId) {
                if (isGameOver) return;
                if (window.currentTurn && window.currentTurn !== mySymbol) return;
                
                // Mostrar feedback visual imediato de que a jogada foi enviada
                statusEl.innerHTML = "<span style='color:var(--muted)'>A enviar jogada " + linhaId + "...</span>";
                
                fetch("game?action=jogar&linha=" + linhaId, { method: "POST" })
                    .then(r => r.json())
                    .then(data => {
                        if (data.status === "ok" && data.xml) {
                            const xmlDoc = parseXML(data.xml);
                            updateBoard(xmlDoc);
                            
                            // Se o turno passou para o adversário, iniciamos o long-polling para escutar a resposta dele!
                            if (window.currentTurn !== mySymbol && !isGameOver) {
                                fetchEstado(); 
                            }
                        } else if (data.status === "ok") {
                            // Fallback caso venha sem xml por algum motivo
                            stopTimer();
                            statusEl.innerHTML = "A aguardar jogada do adversário... (máx 30s)";
                            fetchEstado(); 
                        } else {
                            // Se der erro (Jogada inválida, Não é a tua vez), mostra no status
                            statusEl.innerHTML = "<span style='color:#f87171'>⚠️ " + data.error + "</span>";
                            setTimeout(() => { if(!isGameOver) statusEl.innerHTML = "É a tua vez! Tenta novamente."; }, 2000);
                        }
                    })
                    .catch(e => {
                        console.error(e);
                        statusEl.innerHTML = "<span style='color:#f87171'>Falha ao enviar a jogada.</span>";
                    });
            };

            // Função de Long Polling que aguarda o "obter" do servidor TCP
            function fetchEstado() {
                fetch("game?action=estado")
                    .then(r => r.json())
                    .then(data => {
                        if (data.status === "ok" && data.xml) {
                            const xmlDoc = parseXML(data.xml);
                            updateBoard(xmlDoc);
                            
                            // O servidor respondeu ao nosso Long Polling. 
                            // Se ainda não for a nossa vez (ex: o adversário fez uma jogada extra e o servidor só nos mandou o estado atualizado), temos de reiniciar o Long Polling!
                            if (window.currentTurn !== mySymbol && !isGameOver) {
                                fetchEstado();
                            }
                        } else {
                            statusEl.innerHTML = "Erro no servidor: " + data.error;
                            if (data.error.includes("Ligação")) {
                                isGameOver = true;
                                stopTimer();
                                document.getElementById("btnVoltarLobby")?.removeAttribute("style");
                            }
                        }
                    })
                    .catch(e => {
                        console.error(e);
                        statusEl.innerHTML = "<span style='color:#f87171'>Ligação ao servidor perdida! O adversário pode ter desconectado.</span>";
                        isGameOver = true;
                        stopTimer();
                        document.getElementById("btnVoltarLobby")?.removeAttribute("style");
                    });
            }

            // Inicia o ciclo bloqueante de estado. Se formos o Jogador O, 
            // este fetch ficará pendurado (Long Polling) até o Jogador X jogar.
            fetchEstado();

        }

        // ------------------
        // LÓGICA DO LOBBY
        // ------------------
        const searchInput = document.getElementById("opponentSearch");
        const suggestionsBox = document.getElementById("suggestionsBox");
        const btnProcurar = document.getElementById("btnProcurarPartida");
        const loadingFila = document.getElementById("loadingFila");

        if (btnProcurar && loadingFila) {
            btnProcurar.addEventListener("click", () => {
                btnProcurar.disabled = true;
                btnProcurar.style.opacity = "0.5";
                loadingFila.style.display = "block";

                fetch("lobby?action=entrar_fila")
                    .then(r => {
                        if (!r.ok) throw new Error("Erro na rede");
                        return r.json();
                    })
                    .then(data => {
                        if (data.status === "ok") {
                            // Jogo encontrado!
                            window.location.reload();
                        } else {
                            alert("Erro: " + data.error);
                            btnProcurar.disabled = false;
                            btnProcurar.style.opacity = "1";
                            loadingFila.style.display = "none";
                        }
                    })
                    .catch(err => {
                        console.error(err);
                        alert("A ligação caiu ou ocorreu um erro.");
                        btnProcurar.disabled = false;
                        btnProcurar.style.opacity = "1";
                        loadingFila.style.display = "none";
                    });
            });
        }

        if (searchInput && suggestionsBox) {
            searchInput.addEventListener("input", function() {
                const query = this.value.trim();
                if (query.length < 1) {
                    suggestionsBox.style.display = "none";
                    suggestionsBox.innerHTML = "";
                    return;
                }

                fetch("buscar-jogadores?query=" + encodeURIComponent(query))
                    .then(response => {
                        if (!response.ok) throw new Error("Erro na rede");
                        return response.json();
                    })
                    .then(data => {
                        suggestionsBox.innerHTML = "";
                        if (data.length === 0) {
                            suggestionsBox.innerHTML = '<div class="no-suggestions">Nenhum jogador encontrado</div>';
                            suggestionsBox.style.display = "block";
                            return;
                        }

                        data.forEach(player => {
                            const item = document.createElement("div");
                            item.className = "suggestion-item";
                            
                            const playerColor = player.color || 'var(--accent)';

                            // Photo or placeholder
                            let photoHtml = "";
                            if (player.photo && player.photo.trim().length > 0) {
                                photoHtml = '<img src="' + player.photo + '" class="suggestion-photo" alt="' + player.username + '" style="border: 2px solid ' + playerColor + '; box-shadow: 0 0 8px ' + playerColor + '40;">';
                            } else {
                                const initials = player.username.substring(0, 2).toUpperCase();
                                photoHtml = '<div class="suggestion-placeholder" style="border: 2px solid ' + playerColor + '; box-shadow: 0 0 8px ' + playerColor + '40;">' + initials + '</div>';
                            }

                            item.innerHTML = 
                                photoHtml +
                                '<div class="suggestion-info">' +
                                    '<span class="suggestion-name">' + player.fullName + '</span>' +
                                    '<span class="suggestion-nick">@' + player.username + ' (' + player.nationality + ')</span>' +
                                '</div>';

                            item.addEventListener("click", () => {
                                searchInput.value = player.username;
                                searchInput.disabled = true;
                                suggestionsBox.style.display = "none";
                                
                                if (btnProcurar) {
                                    btnProcurar.style.display = "none";
                                }
                                
                                loadingFila.innerHTML = '<span style="display: block; animation: pulse 1.5s infinite; margin-bottom: 12px; color: var(--accent);">A aguardar aceitação de @' + player.username + '...</span>' +
                                    '<button id="btnCancelarConvite" style="padding: 8px 16px; font-size: 0.9rem; background: #ef4444; color: white; border: none; border-radius: 8px; cursor: pointer; font-weight: 500; box-shadow: 0 4px 12px rgba(239, 68, 68, 0.3);">Cancelar Convite</button>';
                                loadingFila.style.display = "block";

                                fetch("lobby?action=desafiar&target=" + encodeURIComponent(player.username))
                                    .then(r => {
                                        if (!r.ok) return r.json().then(err => { throw err; });
                                        return r.json();
                                    })
                                    .then(data => {
                                        if (data.status === "ok") {
                                            window.location.reload();
                                        }
                                    })
                                    .catch(err => {
                                        if (err.error && !err.error.includes("cancelada")) {
                                            alert("Erro no convite: " + err.error);
                                        }
                                        resetConviteUI();
                                    });

                                document.getElementById("btnCancelarConvite").addEventListener("click", () => {
                                    fetch("lobby?action=cancelar_desafio")
                                        .then(r => r.json())
                                        .then(() => {
                                            resetConviteUI();
                                        });
                                });
                                
                                function resetConviteUI() {
                                    searchInput.value = "";
                                    searchInput.disabled = false;
                                    loadingFila.style.display = "none";
                                    loadingFila.innerHTML = '<span style="display: inline-block; animation: pulse 1.5s infinite;">A procurar adversário...</span>';
                                    if (btnProcurar) {
                                        btnProcurar.style.display = "block";
                                    }
                                }
                            });

                            suggestionsBox.appendChild(item);
                        });
                        suggestionsBox.style.display = "block";
                    })
                    .catch(err => {
                        console.error("Erro na pesquisa:", err);
                    });
            });

            // Fechar caixa ao clicar fora
            document.addEventListener("click", function(e) {
                if (e.target !== searchInput && e.target !== suggestionsBox && !suggestionsBox.contains(e.target)) {
                    suggestionsBox.style.display = "none";
                }
            });
        }
    });
</script>
</body>
</html>
