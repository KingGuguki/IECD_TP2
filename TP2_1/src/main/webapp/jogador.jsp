<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.net.Socket, client.Stub, util.XMLDoc"%>
<%!
    private void fecharSilenciosamente(Stub stub, Socket socket) {
        try {
            if (stub != null) {
                stub.close();
            }
        } catch (Exception e) {
            // Ignora erros de fecho.
        }

        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (Exception e) {
            // Ignora erros de fecho.
        }
    }

    private char registarPerfil(Stub stub, String nickname, String senha, String firstNames, String lastNames,
            String email, String gender, String birthdate, String foto, String nationality, String cor)
            throws Exception {
        try {
            Object simbolo = stub.getClass()
                .getMethod("registar", String.class, String.class, String.class, String.class, String.class,
                        String.class, String.class, String.class, String.class, String.class)
                .invoke(stub, nickname, senha, firstNames, lastNames, email, gender, birthdate, foto, nationality,
                        cor);
            return ((Character) simbolo).charValue();
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable causa = e.getCause();
            if (causa instanceof Exception) {
                throw (Exception) causa;
            }
            throw e;
        }
    }

    private Socket ligarServidor() throws Exception {
        try {
            return new Socket("localhost", 5025);
        } catch (java.net.ConnectException e) {
            Thread servidorLocal = new Thread(() -> {
                try {
                    server.Servidor.main(new String[0]);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
            servidorLocal.setDaemon(true);
            servidorLocal.start();
            Thread.sleep(800);
            return new Socket("localhost", 5025);
        }
    }
%>
<%
    XMLDoc.setContextoReal(getServletContext().getRealPath("/"));

    String acao = request.getParameter("acao");
    String nickname = request.getParameter("nickname");
    String senha = request.getParameter("senha");

    Socket socket = (Socket) session.getAttribute("tp2_socket");
    Stub stub = (Stub) session.getAttribute("tp2_stub");
    String simbolo = (String) session.getAttribute("tp2_simbolo");
    String usernameSessao = (String) session.getAttribute("tp2_username");

    String erro = null;

    if ("logout".equals(acao)) {
        fecharSilenciosamente(stub, socket);
        session.removeAttribute("tp2_socket");
        session.removeAttribute("tp2_stub");
        session.removeAttribute("tp2_simbolo");
        session.removeAttribute("tp2_username");
        response.sendRedirect("index.jsp");
        return;
    }

    if (usernameSessao != null && stub != null && socket != null && simbolo != null && !"login".equals(acao)) {
        response.sendRedirect("menu.jsp");
        return;
    }

    if ("login".equals(acao)) {
        if (nickname == null || nickname.isBlank() || senha == null || senha.isBlank()) {
            erro = "Preenche o nickname e a senha.";
        } else {
            try {
                socket = ligarServidor();
                stub = new Stub(socket);
                char s = stub.iniciar(nickname.trim(), senha);

                session.setAttribute("tp2_socket", socket);
                session.setAttribute("tp2_stub", stub);
                session.setAttribute("tp2_simbolo", String.valueOf(s));
                session.setAttribute("tp2_username", nickname.trim());
                response.sendRedirect("menu.jsp");
                return;
            } catch (Exception primeiroErro) {
                String mensagemErro = primeiroErro.getMessage();
                boolean falhaLigacao = primeiroErro instanceof java.net.ConnectException;
                if (!falhaLigacao && mensagemErro != null) {
                    String texto = mensagemErro.toLowerCase();
                    falhaLigacao = texto.contains("refused") || texto.contains("connect");
                }

                if (!falhaLigacao) {
                    fecharSilenciosamente(stub, socket);
                    session.removeAttribute("tp2_socket");
                    session.removeAttribute("tp2_stub");
                    session.removeAttribute("tp2_simbolo");
                    session.removeAttribute("tp2_username");
                    erro = primeiroErro.getLocalizedMessage();
                } else {
                    try {
                        Thread servidorLocal = new Thread(() -> {
                            try {
                                server.Servidor.main(new String[0]);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        });
                        servidorLocal.setDaemon(true);
                        servidorLocal.start();

                        Thread.sleep(800);

                        socket = new Socket("localhost", 5025);
                        stub = new Stub(socket);
                        char s = stub.iniciar(nickname.trim(), senha);

                        session.setAttribute("tp2_socket", socket);
                        session.setAttribute("tp2_stub", stub);
                        session.setAttribute("tp2_simbolo", String.valueOf(s));
                        session.setAttribute("tp2_username", nickname.trim());
                        response.sendRedirect("menu.jsp");
                        return;
                    } catch (Exception segundoErro) {
                        fecharSilenciosamente(stub, socket);
                        session.removeAttribute("tp2_socket");
                        session.removeAttribute("tp2_stub");
                        session.removeAttribute("tp2_simbolo");
                        session.removeAttribute("tp2_username");
                        erro = segundoErro.getLocalizedMessage();
                        if (erro == null || erro.isBlank()) {
                            erro = primeiroErro.getLocalizedMessage();
                        }
                    }
                }
            }
        }
    }

    if ("registar".equals(acao)) {
        String regNickname = request.getParameter("regNickname");
        String regSenha = request.getParameter("regSenha");
        String regFirstNames = request.getParameter("regFirstNames");
        String regLastNames = request.getParameter("regLastNames");
        String regEmail = request.getParameter("regEmail");
        String regGender = request.getParameter("regGender");
        String regBirthdate = request.getParameter("regBirthdate");
        String regNationality = request.getParameter("regNationality");
        String regCor = request.getParameter("regCor");
        String regFoto = request.getParameter("regFotoBase64");

        try {
            socket = ligarServidor();
            stub = new Stub(socket);
            char s = registarPerfil(stub, regNickname, regSenha, regFirstNames, regLastNames, regEmail, regGender,
                    regBirthdate, regFoto, regNationality, regCor);

            session.setAttribute("tp2_socket", socket);
            session.setAttribute("tp2_stub", stub);
            session.setAttribute("tp2_simbolo", String.valueOf(s));
            session.setAttribute("tp2_username", regNickname.trim());
            response.sendRedirect("menu.jsp");
            return;
        } catch (Exception e) {
            fecharSilenciosamente(stub, socket);
            erro = e.getLocalizedMessage();
        }
    }

    boolean ligado = usernameSessao != null && simbolo != null && !simbolo.isBlank();
%>
<!DOCTYPE html>
<html lang="pt">
<head>
    <meta charset="UTF-8">
    <title>TP2</title>
    <style>
        :root {
            --bg: #0f172a;
            --panel: rgba(15, 23, 42, 0.94);
            --panel-2: rgba(30, 41, 59, 0.88);
            --accent: #38bdf8;
            --text: #e5e7eb;
            --muted: #94a3b8;
            --danger: #f87171;
        }
        body {
            margin: 0;
            font-family: "Segoe UI", Arial, sans-serif;
            background: radial-gradient(circle at top, #1e3a8a 0, #0f172a 42%, #020617 100%);
            color: var(--text);
        }
        .wrap {
            max-width: 520px;
            margin: 0 auto;
            padding: 44px 20px 56px;
        }
        .hero {
            display: grid;
            gap: 16px;
        }
        .card {
            background: var(--panel);
            border: 1px solid rgba(148, 163, 184, 0.18);
            border-radius: 18px;
            padding: 26px;
            box-shadow: 0 18px 60px rgba(0,0,0,.35);
            backdrop-filter: blur(10px);
        }
        h1 {
            margin: 0 0 10px;
            font-size: 2.1rem;
            letter-spacing: -0.04em;
        }
        p {
            margin: 0 0 10px;
            color: var(--muted);
            line-height: 1.55;
        }
        .status {
            display: inline-block;
            padding: 6px 10px;
            border-radius: 999px;
            font-size: 0.85rem;
            margin-top: 12px;
        }
        .ok { background: rgba(34, 197, 94, 0.16); color: #86efac; }
        .warn { background: rgba(248, 113, 113, 0.16); color: #fca5a5; }
        .field {
            display: grid;
            gap: 6px;
            margin-bottom: 14px;
        }
        label {
            font-size: 0.88rem;
            color: #cbd5e1;
        }
        input, select {
            width: 100%;
            box-sizing: border-box;
            border: 1px solid rgba(148, 163, 184, 0.22);
            background: rgba(15, 23, 42, 0.9);
            color: var(--text);
            border-radius: 12px;
            padding: 12px 14px;
            outline: none;
        }
        input:focus, select:focus {
            border-color: var(--accent);
            box-shadow: 0 0 0 3px rgba(56, 189, 248, .15);
        }
        .actions {
            display: flex;
            gap: 10px;
            flex-wrap: wrap;
            margin-top: 18px;
        }
        .actions button, .actions .btnlink {
            flex: 1;
        }
        button, .btnlink {
            border: 0;
            border-radius: 12px;
            padding: 12px 16px;
            font-weight: 600;
            cursor: pointer;
            text-decoration: none;
            display: inline-flex;
            align-items: center;
            justify-content: center;
        }
        .primary {
            background: linear-gradient(135deg, var(--accent), #0ea5e9);
            color: #06263a;
        }
        .secondary {
            background: rgba(148, 163, 184, 0.14);
            color: var(--text);
        }
        .danger {
            background: rgba(248, 113, 113, 0.16);
            color: #fecaca;
        }
        .grid {
            display: grid;
            gap: 14px;
        }
        .mono {
            font-family: Consolas, "Courier New", monospace;
            color: #dbeafe;
        }
        .badge {
            display: inline-block;
            padding: 5px 10px;
            border-radius: 999px;
            background: rgba(56, 189, 248, 0.16);
            color: #bae6fd;
            font-size: 0.82rem;
            margin-bottom: 14px;
        }
        .register-card {
            background: rgba(15, 23, 42, 0.72);
            box-shadow: none;
        }
        .register-card h1 {
            font-size: 1.25rem;
        }
        summary {
            cursor: pointer;
            font-weight: 700;
            color: var(--text);
            list-style: none;
        }
        summary::-webkit-details-marker {
            display: none;
        }
        summary::after {
            content: " +";
            color: var(--accent);
        }
        details[open] summary::after {
            content: " -";
        }
        @media (max-width: 800px) {
            .hero { grid-template-columns: 1fr; }
        }
    </style>
</head>
<body>
<div class="wrap">
    <div class="hero">
        <div class="card">
            <div class="badge">Jogo</div>
            <h1>Entrar no jogo</h1>
            <p>Inicia sessao com o teu jogador para aceder ao painel e jogar.</p>

            <% if (erro != null) { %>
                <div class="status warn"><%= erro %></div>
            <% } %>

            <% if (ligado) { %>
                <div style="margin-top:18px; padding:14px; border-radius:14px; background: rgba(34,197,94,.08); border: 1px solid rgba(34,197,94,.22);">
                    <p style="margin:0 0 6px;">Sessão ativa para <span class="mono"><%= usernameSessao %></span>.</p>
                    <p style="margin:0;">Símbolo atribuído: <span class="mono"><%= simbolo %></span></p>
                </div>
                <div class="actions">
                    <a class="btnlink primary" href="menu.jsp">Continuar</a>
                    <form action="<%= request.getRequestURI() %>" method="post" style="margin:0;">
                        <input type="hidden" name="acao" value="logout">
                        <button class="danger" type="submit">Terminar sessao</button>
                    </form>
                </div>
            <% } else { %>
                <form method="post" action="<%= request.getRequestURI() %>">
                    <input type="hidden" name="acao" value="login">
                    <div class="grid">
                        <div class="field">
                            <label for="nickname">Nickname</label>
                            <input id="nickname" name="nickname" placeholder="ex.: cartwheel" autocomplete="username" required>
                        </div>
                        <div class="field">
                            <label for="senha">Senha</label>
                            <input id="senha" name="senha" type="password" autocomplete="current-password" required>
                        </div>
                    </div>
                    <div class="actions">
                        <button class="primary" type="submit">Entrar</button>
                    </div>
                </form>
            <% } %>
        </div>

        <details class="card register-card">
            <summary>Criar novo jogador</summary>
            <p>Ainda nao tens conta? Cria um perfil para comecar a jogar.</p>
            <form method="post" action="<%= request.getRequestURI() %>">
                <input type="hidden" name="acao" value="registar">
                <input id="regFotoBase64" name="regFotoBase64" type="hidden">

                <div class="field">
                    <label for="regNickname">Nickname</label>
                    <input id="regNickname" name="regNickname" minlength="4" maxlength="10" required>
                </div>
                <div class="field">
                    <label for="regSenha">Senha</label>
                    <input id="regSenha" name="regSenha" type="password" required>
                </div>
                <div class="field">
                    <label for="regFirstNames">Primeiros nomes</label>
                    <input id="regFirstNames" name="regFirstNames" required>
                </div>
                <div class="field">
                    <label for="regLastNames">Apelidos</label>
                    <input id="regLastNames" name="regLastNames" required>
                </div>
                <div class="field">
                    <label for="regEmail">Email</label>
                    <input id="regEmail" name="regEmail" type="email" required>
                </div>
                <div class="field">
                    <label for="regGender">Genero</label>
                    <select id="regGender" name="regGender">
                        <option value="X">Nao definido</option>
                        <option value="M">Masculino</option>
                        <option value="F">Feminino</option>
                    </select>
                </div>
                <div class="field">
                    <label for="regBirthdate">Data de nascimento</label>
                    <input id="regBirthdate" name="regBirthdate" type="date"
                           min="<%= java.time.LocalDate.now().minusYears(130) %>"
                           max="<%= java.time.LocalDate.now().minusYears(3) %>" required>
                </div>
                <div class="field">
                    <label for="regNationality">Nacionalidade (codigo ISO de 2 letras)</label>
                    <input id="regNationality" name="regNationality" maxlength="2" pattern="[A-Za-z]{2}" required>
                </div>
                <div class="field">
                    <label for="regCor">Cor de fundo preferida</label>
                    <input id="regCor" name="regCor" type="color" value="#0F172A">
                </div>
                <div class="field">
                    <label for="regFotoArquivo">Fotografia opcional</label>
                    <input id="regFotoArquivo" type="file" accept="image/*">
                </div>
                <div class="actions">
                    <button class="primary" type="submit">Criar perfil</button>
                </div>
            </form>
        </details>
    </div>
</div>
<script>
    const regFotoArquivo = document.getElementById('regFotoArquivo');
    const regFotoBase64 = document.getElementById('regFotoBase64');
    if (regFotoArquivo && regFotoBase64) {
        regFotoArquivo.addEventListener('change', function () {
            const file = this.files && this.files[0];
            if (!file) {
                regFotoBase64.value = '';
                return;
            }
            const reader = new FileReader();
            reader.onload = function () {
                const dataUrl = String(reader.result || '');
                const comma = dataUrl.indexOf(',');
                regFotoBase64.value = comma >= 0 ? dataUrl.substring(comma + 1) : '';
            };
            reader.readAsDataURL(file);
        });
    }
</script>
</body>
</html>
