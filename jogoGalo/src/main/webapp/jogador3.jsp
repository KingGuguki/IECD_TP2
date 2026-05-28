<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.net.*, client.Stub, org.w3c.dom.Element, util.XMLDoc"%>
<%-- 
    =======================================================================
    SISTEMA DE JOGO DO GALO - CLIENTE WEB (JSP + SOCKETS)
    =======================================================================
    Funcionamento:
    1. VIGIA: O JSP tenta ligar ao servidor Java via Socket automaticamente.
    2. LOGIN: Após a ligação, solicita a autenticação ao utilizador.
    3. JOGO: O tabuleiro é gerado pelo Stub (Java).
    O JavaScript garante feedback visual instantâneo ao clicar.
    =======================================================================
--%>
<%! 
    // Métodos utilitários declarativos em JSP usam <%!
    private void limparSubSessao(HttpSession session, String idSeparador) {
        if (session != null && idSeparador != null) {
            session.removeAttribute("socketGalo_" + idSeparador);
            session.removeAttribute("stubGalo_" + idSeparador);
            session.removeAttribute("faseGalo_" + idSeparador);
            session.removeAttribute("simbolo_" + idSeparador);
            session.removeAttribute("inicioJogo_" + idSeparador);
            session.removeAttribute("hostGalo_" + idSeparador);
            session.removeAttribute("portoGalo_" + idSeparador);
        }
    }
%>
<%
	/* 🛡️ CONFIGURAÇÃO DE SESSÃO E PROTOCOLO */
	String acao = request.getParameter("acao");
	
	// 🆔 GESTÃO DO ID DO SEPARADOR (Usando UUID)
	String idSeparador = request.getParameter("idSeparador");
	if (idSeparador == null || idSeparador.trim().isEmpty()) {
	    idSeparador = java.util.UUID.randomUUID().toString();
	}
	
	// 🚿 CORREÇÃO: "Login" NÃO pode apagar a sessão de rede! Apenas "Sair" limpa tudo.
	if ("Sair".equals(acao)) {
	    limparSubSessao(session, idSeparador);
	}
	
	// Obter os objetos específicos deste separador
	Socket socket = (Socket) session.getAttribute("socketGalo_" + idSeparador);
	Stub stub = (Stub) session.getAttribute("stubGalo_" + idSeparador);
	String fase = (String) session.getAttribute("faseGalo_" + idSeparador);
	if (fase == null) fase = "VIGIA";
	
	String erroGrave = null;
	String caminhoBase = getServletContext().getRealPath("/");
	XMLDoc.setContextoReal(caminhoBase);
	
	// 🕵️ PASSO 1: VIGILÂNCIA / CONFIGURAÇÃO INICIAL
	if (fase.equals("VIGIA")) {
	    String hostParam = request.getParameter("host");
	    String portoParam = request.getParameter("porto");
	
	    // Se o utilizador submeteu o formulário de rede:
	    if (hostParam != null && !hostParam.trim().isEmpty() && portoParam != null && !portoParam.trim().isEmpty()) {
	        session.setAttribute("hostGalo_" + idSeparador, hostParam.trim());
	        session.setAttribute("portoGalo_" + idSeparador, portoParam.trim());
	        session.setAttribute("faseGalo_" + idSeparador, "CONECTAR");
	        fase = "CONECTAR";
	    }
	}

	// 🔥 PASSO 1.5: TENTAR LIGAÇÃO COM OS DADOS CONFIGURADOS
	if (fase.equals("CONECTAR")) {
	    String hostSessao = (String) session.getAttribute("hostGalo_" + idSeparador);
	    String portoSessao = (String) session.getAttribute("portoGalo_" + idSeparador);
	    
	    if (hostSessao == null) hostSessao = "localhost";
	    int portoFinal = 5025;
	    try { portoFinal = Integer.parseInt(portoSessao); } catch(Exception e){}
	
	    try {
	        socket = new Socket(hostSessao, portoFinal);
	        stub = new Stub(socket);
	        session.setAttribute("socketGalo_" + idSeparador, socket);
	        session.setAttribute("stubGalo_" + idSeparador, stub);
	        session.setAttribute("faseGalo_" + idSeparador, "LOGIN");
	        fase = "LOGIN";
	    } catch (Exception e) {
	        limparSubSessao(session, idSeparador);
	        erroGrave = "Não foi possível ligar ao servidor " + hostSessao + ":" + portoFinal + ". Verifique se está ativo.";
	        fase = "VIGIA";
	    }
	}
	
	// 🔑 PASSO 2: AUTENTICAÇÃO (Robusta contra perdas de Socket com Redirecionamento Seguro)
	if ("Login".equals(acao)) {
	    try {
	        // Se o stub caiu ou ficou nulo, reconecta usando os parâmetros ocultos do formulário
	        if (stub == null) {
	            String hParam = request.getParameter("host");
	            String pParam = request.getParameter("porto");
	            int port = 5025;
	            try { port = Integer.parseInt(pParam); } catch(Exception e){}
	            if (hParam != null && !hParam.isEmpty()) {
	                socket = new Socket(hParam.trim(), port);
	                stub = new Stub(socket);
	                session.setAttribute("socketGalo_" + idSeparador, socket);
	                session.setAttribute("stubGalo_" + idSeparador, stub);
	            }
	        }

	        if (stub != null) {
	            String user = request.getParameter("nome");
	            String pass = request.getParameter("senha");
	            char simbolo = stub.iniciar(user, pass); 
	            
	            session.setAttribute("simbolo_" + idSeparador, String.valueOf(simbolo).toUpperCase().trim());
	            session.setAttribute("faseGalo_" + idSeparador, "TABULEIRO");
	            session.setAttribute("inicioJogo_" + idSeparador, System.currentTimeMillis());
	            
	            // 🚀 CORREÇÃO: Redirecionar imediatamente para limpar o POST do formulário
	            response.sendRedirect(request.getRequestURI() + "?idSeparador=" + idSeparador);
	            return;
	        } else {
	            throw new Exception("Conexão ao servidor perdida. Configure a rede novamente.");
	        }
	    } catch (Exception e) {
	        erroGrave = "Erro no Login: " + e.getMessage();
	        // Se o erro foi recusa de ligação do servidor, volta para VIGIA. Senão, mantém em LOGIN.
	        if (e.getMessage() != null && (e.getMessage().contains("refused") || e.getMessage().contains("lost"))) {
	            limparSubSessao(session, idSeparador);
	            fase = "VIGIA";
	        } else {
	            fase = "LOGIN";
	        }
	    }
	}

    // 🕹️ PASSO 3: JOGADA
    String jogadaParam = request.getParameter("jogada");
    if (jogadaParam != null && stub != null) {
        try {
            stub.jogar(Short.parseShort(jogadaParam));
        } catch (Exception e) {
            if (stub != null) try { stub.close(); } catch(Exception ex) {}
            limparSubSessao(session, idSeparador);
            response.sendRedirect(request.getRequestURI() + "?idSeparador=" + idSeparador);
            return;
        }
    }

    // 🛑 SAIR
    if ("Sair".equals(acao)) {
        if (stub != null) try { stub.close(); } catch(Exception e){}
        limparSubSessao(session, idSeparador);
        response.sendRedirect(request.getRequestURI());
        return;
    }

    // 🔄 ANTECIPAÇÃO DA LEITURA DO ESTADO DO JOGO (Para sincronizar com JavaScript)
    boolean jogoTerminado = false;
    Element tabuleiroDoServidor = null;
    if ("TABULEIRO".equals(fase) && stub != null) {
        try {
            tabuleiroDoServidor = stub.obter();
            String estado = tabuleiroDoServidor.getAttribute("estado");
            if (!estado.equals("ND")) {
                jogoTerminado = true;
            }
        } catch (Exception e) {
            limparSubSessao(session, idSeparador);
            response.sendRedirect(request.getRequestURI() + "?idSeparador=" + idSeparador);
            return;
        }
    }
%>

<!DOCTYPE html>
<html lang="pt-pt">
<head>
    <meta charset="UTF-8">
    <link rel="icon" type="image/x-icon" href="favicon.ico">
<%-- Comentado para evitar loops de refresh enquanto o utilizador configura o servidor --%>
<%-- 
<% if (fase.equals("VIGIA")) { %>
    <meta http-equiv="refresh" content="3;url=<%= request.getRequestURI() %>?idSeparador=<%= idSeparador %>">
<% } %>
--%>
    <title>Jogo 🕹️ do Galo 🐔 JSP 🇵🇹 </title>
    <style>
        body { font-family: 'Segoe UI', sans-serif; text-align: center; background: #f4f7f6; padding-top: 50px; }
        .tabuleiro-svg { background: white; border-radius: 15px; box-shadow: 0 10px 25px rgba(0,0,0,0.1); margin: 20px auto; }
        .casa { fill: transparent; cursor: pointer; }
        .grelha { stroke: #cbd5e0; stroke-width: 5; stroke-linecap: round; }
        .x-peca { stroke: #e63946; stroke-width: 8; stroke-linecap: round; }
        .o-peca { stroke: #457b9d; stroke-width: 8; fill: none; }
        .temp-peca { opacity: 0.5; } 
        .btn { padding: 12px 24px; border: none; border-radius: 6px; cursor: pointer; font-weight: bold; background: #319795; color: white; }
	    .dica-anonima { margin-top: 20px; font-size: 0.9em; color: #2c5282; background: #ebf8ff; padding: 12px; border-radius: 8px; border: 1px solid #bee3f8; display: inline-block; max-width: 450px; line-height: 1.4; }
	    .atalho { font-weight: bold; color: #2b6cb0; background: #fff; padding: 2px 5px; border-radius: 4px; border: 1px solid #cbd5e0; } 
	    .lista-utilizadores { text-align: left; display: inline-block; margin-top: 15px; font-family: monospace; color: #4a5568; }
	    .lista-limpa { list-style-type: none; padding: 0; margin: 0; }
	    .lista-limpa li { margin: 0; line-height: 1.2; }
	    .lista-limpa a { text-decoration: none; color: #319795; font-weight: bold; display: block; padding: 2px 0; }
		.lista-limpa a:hover { color: #2c7a7b; text-decoration: underline; }
    </style>
    
	<script>
    	/* 🛡️ Captura estrita do símbolo usando a chave dinâmica da sub-sessão */
    	var simboloJogador = "<%= (session.getAttribute("simbolo_" + idSeparador) != null) ? session.getAttribute("simbolo_" + idSeparador).toString().trim().toUpperCase() : "" %>";
    	/* 🛡️ Declaração da variável com o ID do separador para o JavaScript poder usar */
    	var idSeparadorAtual = "<%= idSeparador %>";
    	/* 🛑 Variável global para saber se o jogo já terminou - AGORA SINCRONIZADA */
        var jogoTerminadoGlobal = <%= jogoTerminado ? "true" : "false" %>;
        
      /**
       * ✨ FUNÇÃO: clique
       * Interpõe feedback visual imediato e bloqueia cliques pós-jogo de forma robusta.
       */
      function clique(event, i) { 
    		// 1. TRAVAR IMEDIATAMENTE O LINK (Previne navegação nativa abrupta)
    	    if (event) {
    	        event.preventDefault();
    	    } else {
    	        event = window.event;
    	        if (event) event.preventDefault();
    	    }

    		// 2. VALIDAÇÕES DE PARAGEM: Se o jogo terminou ou o jogador for inválido, sai aqui
    	    if (jogoTerminadoGlobal || (simboloJogador !== "X" && simboloJogador !== "O")) {
    	        return;
    	    }

    	    let urlDestino = null;
    	    if (event && event.currentTarget) {
    	    	const href = event.currentTarget.getAttribute("href");
    	    	if (href) {
                    urlDestino = href + (href.includes('?') ? '&' : '?') + "idSeparador=" + idSeparadorAtual;
                }
    	    }
    	    
    	 	// =================================================================
    	    // 🔒 TRANCA INSTANTÂNEA CONTRA CLIQUES REPETIDOS / OUTRAS CASAS
    	    // =================================================================
			if (urlDestino) {
			    // 🔒 Lógico: Tranca imediatamente o JS para qualquer outro clique concorrente
			    jogoTerminadoGlobal = true; 
			    
			    // 🎨 Visual: Remove instantaneamente a "mãozinha" e os efeitos hover do rato no SVG
			    document.querySelector('.tabuleiro-svg').style.pointerEvents = 'none';
			}
    	    // =================================================================
    	    	
    	    // 3. DESENHAR PEÇA: Abordagem createElementNS compatível com todos os browsers (W3C)
    	    const svg = document.querySelector('.tabuleiro-svg');
    	    if (svg) {
    	        const x = (i % 3) * 100;
    	        const y = Math.floor(i / 3) * 100;
    	        const svgNS = "http://www.w3.org/2000/svg";
    	        try {
    	            let peca = null;
    	            if (simboloJogador === "X") {
    	                peca = document.createElementNS(svgNS, "g");
    	                peca.setAttribute("class", "x-peca temp-peca");
    	                
    	                // Função utilitária interna para evitar código redundante de linhas
    	                const criarLinha = (x1, y1, x2, y2) => {
    	                    const line = document.createElementNS(svgNS, "line");
    	                    line.setAttribute("x1", x1); line.setAttribute("y1", y1);
    	                    line.setAttribute("x2", x2); line.setAttribute("y2", y2);
    	                    return line;
    	                };
    	                peca.appendChild(criarLinha(x + 25, y + 25, x + 75, y + 75));
    	                peca.appendChild(criarLinha(x + 75, y + 25, x + 25, y + 75));
    	            } else if (simboloJogador === "O") { // 🛡️ Alterado de 'else' simples para verificação estrita
    	                peca = document.createElementNS(svgNS, "circle");
    	                peca.setAttribute("cx", x + 50); 
    	                peca.setAttribute("cy", y + 50);
    	                peca.setAttribute("r", "25"); 
    	                peca.setAttribute("class", "o-peca temp-peca");
    	            }
    	            svg.appendChild(peca);
    	        } catch (e) { 
    	            console.error("Erro visual:", e); 
    	        }
    	    }

    	    // 4. REDIRECIONAMENTO CONTROLADO
    	    if (urlDestino) {
    	    	setTimeout(() => {
    	        	window.location.href = urlDestino;
    	    	}, 150);
    	    }
    	}

    /* 🎯 Função para preenchimento automático no Login */
    function preencher(user, pass) {
        const campoNome = document.querySelector('input[name="nome"]');
        const campoSenha = document.querySelector('input[name="senha"]');
        if (campoNome && campoSenha) {
            campoNome.value = user;
            campoSenha.value = pass;
            const btn = document.querySelector('button[value="Login"]');
            if (btn) {
                btn.style.transform = "scale(1.05)";
                setTimeout(() => btn.style.transform = "scale(1)", 200);
            }
        }
    }
    
    /* 🔒 Impede cliques repetidos no login permitindo que o formulário avance */
    function bloquearLogin(formulario) {
        // Procura o botão de submissão com valor "Login" dentro do formulário
        const botao = formulario.querySelector('button[value="Login"]');
        if (botao) {
            // Agendamos a desativação para o milissegundo seguinte
            setTimeout(() => {
                // 1. Altera o texto para dar feedback visual de carregamento
                botao.innerText = "A entrar... ⏳";
                
                // 2. Desativa o botão para bloquear novos cliques
                botao.disabled = true;
                
                // 3. Modifica o visual para parecer desativado
                botao.style.opacity = "0.6";
                botao.style.cursor = "not-allowed";
            }, 1); // 1 milissegundo é suficiente para o formulário arrancar com segurança
        }
    }
    
 	// Ativa os cronómetros apenas se o utilizador estiver no ecrã do tabuleiro
    <% if ("TABULEIRO".equals(fase)) { 
        long inicio = (Long) session.getAttribute("inicioJogo_" + idSeparador);
        long decorridoNoServidor = System.currentTimeMillis() - inicio;
    %>
    window.addEventListener('DOMContentLoaded', () => {
    	const jogoAcabou = <%= jogoTerminado %>;
        let tempoInicialMS = <%= decorridoNoServidor %>; 
        let inicioJogoLocal = Date.now() - tempoInicialMS;

        function atualizarTempoJogo() {
            let totalSegundos = Math.floor((Date.now() - inicioJogoLocal) / 1000);
            let minutos = Math.floor(totalSegundos / 60); // ✔️ Alterado para minutos
            let segundos = totalSegundos % 60;
            let formatoMin = minutos < 10 ? "0" + minutos : minutos;
            let formatoSeg = segundos < 10 ? "0" + segundos : segundos;
            
            const elJogo = document.getElementById("tempoJogo");
            if (elJogo) elJogo.innerText = formatoMin + ":" + formatoSeg;
        }

        let inicioJogadaLocal = Date.now();

        function atualizarTempoJogada() {
            let totalSegundos = Math.floor((Date.now() - inicioJogadaLocal) / 1000);
            let minutos = Math.floor(totalSegundos / 60); // ✔️ Alterado para minutos
            let segundos = totalSegundos % 60;
            let formatoMin = minutos < 10 ? "0" + minutos : minutos;
            let formatoSeg = segundos < 10 ? "0" + segundos : segundos;
            
            const elJogada = document.getElementById("tempoJogada");
            if (elJogada) elJogada.innerText = formatoMin + ":" + formatoSeg;
        }

        atualizarTempoJogo();
        atualizarTempoJogada();
        
        if (!jogoAcabou) {
            setInterval(atualizarTempoJogo, 1000); 
            setInterval(atualizarTempoJogada, 1000);
        } else {
        	// Feedback visual extra: Altera a cor dos contadores e desliga eventos do rato no SVG
            const elJogada = document.getElementById("tempoJogada");
            if (elJogada) elJogada.style.color = "#718096"; 
            
            const elJogo = document.getElementById("tempoJogo");
            if (elJogo) elJogo.style.color = "#718096";

            const svgElement = document.querySelector('.tabuleiro-svg');
            if (svgElement) svgElement.style.pointerEvents = 'none';
        }
    });
    <% } %>
    
	</script>
</head>
<body>

    <h1>Jogo 🕹️ do Galo 🐔 JSP 🇵🇹</h1>

    <% if (fase.equals("VIGIA")) { %>
		<div style="background: white; display: inline-block; padding: 30px; border-radius: 12px; box-shadow: 0 4px 10px rgba(0,0,0,0.05);">
            <h3>🌐 Configuração do Servidor</h3>
            <% if (erroGrave != null) { %><p style="color:red;"><%= erroGrave %></p><% } %>
            <form method="post">
                <input type="hidden" name="idSeparador" value="<%= idSeparador %>">
                
                <label style="font-size: 0.9em; color: #4a5568; display: block; text-align: left; margin-bottom: 5px;">Endereço IP / Host:</label>
                <input type="text" name="host" value="localhost" placeholder="ex: 127.0.0.1" required style="padding: 8px; width: 200px; margin-bottom: 15px;"><br>
                
                <label style="font-size: 0.9em; color: #4a5568; display: block; text-align: left; margin-bottom: 5px;">Porto:</label>
                <input type="number" name="porto" value="5025" placeholder="ex: 5025" required style="padding: 8px; width: 200px; margin-bottom: 20px;"><br>
                
                <button type="submit" class="btn">Estabelecer Ligação</button>
            </form>
        </div>

    <% } else if (fase.equals("LOGIN")) { %>
       		<div style="background: white; display: inline-block; padding: 30px; border-radius: 12px;">
            	<h3>🔐 Autenticação</h3>
            	<% if (erroGrave != null) { %><p style="color:red;"><%= erroGrave %></p><% } %>
           		 <form method="post" onsubmit="bloquearLogin(this);">
            		<input type="hidden" name="idSeparador" value="<%= idSeparador %>">
                
               		<input type="hidden" name="host" value="<%= session.getAttribute("hostGalo_" + idSeparador) %>">
                	<input type="hidden" name="porto" value="<%= session.getAttribute("portoGalo_" + idSeparador) %>">

                	<input type="text" name="nome" placeholder="Utilizador" required><br><br>
                	<input type="password" name="senha" placeholder="Senha" required><br><br>
                	<button type="submit" name="acao" value="Login" class="btn">Entrar</button>
            	</form>
			<div class="lista-utilizadores">
			    <strong>Utilizadores para teste:</strong>
			    <ul class="lista-limpa">
			        <li><a href="javascript:void(0)" onclick="preencher('cartwheel', 'p1')">cartwheel:p1</a></li>
			        <li><a href="javascript:void(0)" onclick="preencher('milkshake', 'p2')">milkshake:p2</a></li>
			        <li><a href="javascript:void(0)" onclick="preencher('gandalf', 'p4')">gandalf:p4</a></li>
			        <li><a href="javascript:void(0)" onclick="preencher('opera', 'p5')">opera:p5</a></li>
			        <li><a href="javascript:void(0)" onclick="preencher('smoke', 'p9')">smoke:p9</a></li>
			        <li><a href="javascript:void(0)" onclick="preencher('bagel', 'p10')">bagel:p10</a></li>
			    </ul>
			</div>
			<div id="mensagemAnonima" class="dica-anonima" style="display:none;"></div>
        </div>
        <script>
            window.addEventListener('DOMContentLoaded', () => {
                const ua = navigator.userAgent;
                const msgDiv = document.getElementById('mensagemAnonima');
                if (!msgDiv) return;
                let browserName = "teu browser", atalho = "Ctrl + Shift + N";
                if (ua.indexOf("Firefox") > -1) { browserName = "Firefox"; atalho = "Ctrl + Shift + P"; }
                else if (ua.indexOf("Safari") > -1 && ua.indexOf("Chrome") == -1) { browserName = "Safari"; atalho = "Cmd + Shift + P"; }
                msgDiv.innerHTML = "Browser: <b>" + browserName + "</b>. Atalho para janela anónima: <span class='atalho'>" + atalho + "</span> 🕹️";
                msgDiv.style.display = 'inline-block';
            });
        </script>

    <% } else if (fase.equals("TABULEIRO")) { %>
        <p>Jogador com: <b><%= session.getAttribute("simbolo_" + idSeparador) %></b></p>
		<div style="margin: 15px auto 25px auto; max-width: 400px; background: white; padding: 15px; border-radius: 8px; box-shadow: 0 4px 10px rgba(0,0,0,0.05); display: flex; justify-content: space-around;">
            <div>
                <span style="font-size: 0.85em; color: #718096; display: block;">TEMPO DE JOGO</span>
                <strong id="tempoJogo" style="font-size: 1.4em; color: #2d3748; font-family: monospace;">00:00</strong>
            </div>
            <div style="border-left: 1px solid #e2e8f0;"></div>
            <div>
                <span style="font-size: 0.85em; color: #718096; display: block;">TEMPO DA JOGADA</span>
                <strong id="tempoJogada" style="font-size: 1.4em; color: #319795; font-family: monospace;">00:00</strong>
            </div>
        </div>
        <%
            // Renderização segura do tabuleiro lido previamente no cabeçalho
            if (tabuleiroDoServidor != null) {
                String estado = tabuleiroDoServidor.getAttribute("estado");
                if (!estado.equals("ND")) {
                    out.write("<h2 style='color:#2c7a7b;'>" + Stub.estadoToTXT(estado) + "</h2>");
                }
                out.write(Stub.tabuleiroToSVG(tabuleiroDoServidor));
            }
        %>
        <form method="post">
        	<input type="hidden" name="idSeparador" value="<%= idSeparador %>">
            <button type="submit" name="acao" value="Sair" class="btn" style="background:#e53e3e;">Sair 🛑</button>
        </form>
    <% } %>

</body>
</html>