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
<%
	/* 🛡️ CONFIGURAÇÃO DE SESSÃO E PROTOCOLO */
	String acao = request.getParameter("acao");

	//🚿 LIMPEZA PREVENTIVA: Se o utilizador clicar em "Login" ou "Sair", limpamos tudo.
	if ("Login".equals(acao) || "Sair".equals(acao)) {
	    if (session != null) 
	        session.invalidate(); 
	    session = request.getSession(true);
	}
   
    Socket socket = (Socket) session.getAttribute("socketGalo");
    Stub stub = (Stub) session.getAttribute("stubGalo");
    String fase = (String) session.getAttribute("faseGalo");
    if (fase == null) fase = "VIGIA";
    
    String erroGrave = null;
    String caminhoBase = getServletContext().getRealPath("/");
    XMLDoc.setContextoReal(caminhoBase);

    // 🕵️ PASSO 1: VIGILÂNCIA (Deteção Automática)
    if (fase.equals("VIGIA")) {
        try {
            socket = new Socket("localhost", 5025);
            stub = new Stub(socket);
            session.setAttribute("socketGalo", socket);
            session.setAttribute("stubGalo", stub);
            session.setAttribute("faseGalo", "LOGIN");
            fase = "LOGIN";
        } catch (Exception e) {
            socket = null;
        }
    }

    // 🔑 PASSO 2: AUTENTICAÇÃO
    if ("Login".equals(acao) && stub != null) {
        try {
            String user = request.getParameter("nome");
            String pass = request.getParameter("senha");
            char simbolo = stub.iniciar(user, pass); 
            session.setAttribute("simbolo", String.valueOf(simbolo));
            session.setAttribute("faseGalo", "TABULEIRO");
            fase = "TABULEIRO";
        } catch (Exception e) {
            erroGrave = "Erro: " + e.getMessage();
        }
    }

    // 🕹️ PASSO 3: JOGADA
    String jogadaParam = request.getParameter("jogada");
    if (jogadaParam != null && stub != null) {
        try {
            stub.jogar(Short.parseShort(jogadaParam));
        } catch (Exception e) {
            erroGrave = "Jogada falhou: " + e.getMessage();
        }
    }

    // 🛑 SAIR
    if ("Sair".equals(acao)) {
        if (stub != null) stub.close();
        session.invalidate();
        response.sendRedirect(request.getRequestURI());
        return;
    }
%>

<!DOCTYPE html>
<html lang="pt-pt">
<head>
    <meta charset="UTF-8">
    <link rel="icon" type="image/x-icon" href="favicon.ico">
    <% if (fase.equals("VIGIA")) { %><meta http-equiv="refresh" content="3"><% } %>
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
    /* 🛡️ Captura estrita do símbolo do jogador para evitar erros de caractere */
    var simboloJogador = "<%= (session.getAttribute("simbolo") != null) ? session.getAttribute("simbolo").toString().trim().toUpperCase() : "" %>";
    
     /**
      * ✨ FUNÇÃO: clique
      * Gere o feedback visual antes de comunicar a jogada.
      */
      function clique(event, i) {   	    
    	    if (simboloJogador !== "X" && simboloJogador !== "O") 
    	        return;

    	    let x = (i % 3) * 100;
    	    let y = Math.floor(i / 3) * 100;
    	 	// Criamos a variável do link fora, começando vazia
    	    let urlDestino = null;
    	    if (event) {
    	        event.preventDefault(); // Impede a navegação imediata
    	    	urlDestino = event.currentTarget.getAttribute("href");  
    	    }

    	    // Desenhar a peça imediatamente
    	    var svg = document.querySelector('.tabuleiro-svg');
    	    if (svg) {
    	        try {
    	            var peca = null;
    	            
    	            if (simboloJogador === "X") {
    	                peca = document.createElementNS("http://www.w3.org/2000/svg", "g");
    	                peca.setAttribute("class", "x-peca temp-peca");
    	                
    	                var l1 = document.createElementNS("http://www.w3.org/2000/svg", "line");
    	                l1.setAttribute("x1", x + 25); l1.setAttribute("y1", y + 25);
    	                l1.setAttribute("x2", x + 75); l1.setAttribute("y2", y + 75);
    	                
    	                var l2 = document.createElementNS("http://www.w3.org/2000/svg", "line");
    	                l2.setAttribute("x1", x + 75); l2.setAttribute("y1", y + 25);
    	                l2.setAttribute("x2", x + 25); l2.setAttribute("y2", y + 75);
    	                
    	                peca.appendChild(l1);
    	                peca.appendChild(l2);
    	            } else {
    	                peca = document.createElementNS("http://www.w3.org/2000/svg", "circle");
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
    	 	// Só faz o redirecionamento se existia um evento e uma URL válida
    	    if (urlDestino) {
    	    	// Navega para o servidor após o feedback visual (150ms)
    	    	setTimeout(function() {
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
	</script>
</head>
<body>

    <h1>Jogo 🕹️ do Galo 🐔 JSP 🇵🇹</h1>

    <% if (fase.equals("VIGIA")) { %>
        <div style="padding: 40px;">
            <h2>🔎 À procura do servidor...</h2>
            <p>A aguardar ligação no porto 5025.</p>
        </div>

    <% } else if (fase.equals("LOGIN")) { %>
        <div style="background: white; display: inline-block; padding: 30px; border-radius: 12px;">
            <h3>🔐 Autenticação</h3>
            <% if (erroGrave != null) { %><p style="color:red;"><%= erroGrave %></p><% } %>
            <form method="post">
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
        <p>Tu jogas com: <b><%= session.getAttribute("simbolo") %></b></p>
        <%
            try {
                Element tab = stub.obter();
                String estado = tab.getAttribute("estado");
                if (!estado.equals("ND")) {
                    out.write("<h2 style='color:#2c7a7b;'>" + Stub.estadoToTXT(estado) + "</h2>");
                }
                out.write(Stub.tabuleiroToSVG(tab)); 
            } catch (Exception e) {
                session.invalidate();
                response.sendRedirect(request.getRequestURI());
            }
        %>
        <form method="post">
            <button type="submit" name="acao" value="Sair" class="btn" style="background:#e53e3e;">Sair 🛑</button>
        </form>
    <% } %>

</body>
</html>