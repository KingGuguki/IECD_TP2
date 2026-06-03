<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="client.Stub, user.User, util.XMLDoc, java.net.Socket"%>
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

    private void atualizarPerfil(String username, String firstNames, String lastNames, String email, String gender,
            String birthdate, String nationality, String fotoBase64, String cor) throws Exception {
        Socket socketPerfil = null;
        Stub stubPerfil = null;
        try {
            socketPerfil = new Socket("localhost", 5025);
            stubPerfil = new Stub(socketPerfil);
            stubPerfil.getClass()
                .getMethod("atualizarPerfil", String.class, String.class, String.class, String.class, String.class,
                        String.class, String.class, String.class, String.class)
                .invoke(stubPerfil, username, firstNames, lastNames, email, gender, birthdate, nationality,
                        fotoBase64, cor);
        } catch (NoSuchMethodException e) {
            throw new Exception("As classes do servidor precisam de ser recompiladas antes de editar o perfil.");
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable causa = e.getCause();
            if (causa instanceof Exception) {
                throw (Exception) causa;
            }
            throw e;
        } finally {
            if (stubPerfil != null) {
                stubPerfil.close();
            } else if (socketPerfil != null) {
                try {
                    socketPerfil.close();
                } catch (Exception e) {
                    // Ignora erros de fecho.
                }
            }
        }
    }
%>
<%
    XMLDoc.setContextoReal(getServletContext().getRealPath("/"));

    String username = (String) session.getAttribute("tp2_username");
    String simbolo = (String) session.getAttribute("tp2_simbolo");
    Socket socket = (Socket) session.getAttribute("tp2_socket");
    Stub stub = (Stub) session.getAttribute("tp2_stub");

    if (username == null || simbolo == null || socket == null || stub == null) {
        response.sendRedirect("jogador.jsp");
        return;
    }

    String mensagem = null;
    String erro = null;

    User jogador;
    String fotoBase64 = "";
    String cor = "#0F172A";
    String firstNames = "";
    String lastNames = "";
    String email = "";
    String gender = "X";
    String birthdate = "";
    String nationality = "";

    try {
        User._load();
        jogador = User._obtain(username);
    } catch (Exception e) {
        response.sendRedirect("jogador.jsp");
        return;
    }

    if (jogador.getPhotography() != null) {
        fotoBase64 = jogador.getPhotography().getBase64();
    }
    cor = obterCorFundo(jogador);
    firstNames = jogador.getFirstNames();
    lastNames = jogador.getLastNames();
    email = jogador.getEmail();
    gender = (jogador.getGender() == null || jogador.getGender().isBlank()) ? "X" : jogador.getGender();
    birthdate = (jogador.getBirthdate() == null) ? "" : jogador.getBirthdate().toString();
    nationality = (jogador.getNationality() == null) ? "" : jogador.getNationality().getAbbreviation();

    String acao = request.getParameter("acao");
    if ("guardar".equals(acao)) {
        String novaFoto = request.getParameter("fotoBase64");
        String novaCor = request.getParameter("cor");
        String novosFirstNames = request.getParameter("firstNames");
        String novosLastNames = request.getParameter("lastNames");
        String novoEmail = request.getParameter("email");
        String novoGender = request.getParameter("gender");
        String novaBirthdate = request.getParameter("birthdate");
        String novaNationality = request.getParameter("nationality");

        try {
            if (novaFoto == null) {
                novaFoto = "";
            }
            if (novaCor == null || novaCor.isBlank()) {
                novaCor = cor;
            }

            atualizarPerfil(username, novosFirstNames, novosLastNames, novoEmail, novoGender, novaBirthdate,
                    novaNationality, novaFoto, novaCor);

            User._load();
            jogador = User._obtain(username);
            fotoBase64 = (jogador.getPhotography() != null) ? jogador.getPhotography().getBase64() : "";
            cor = obterCorFundo(jogador);
            firstNames = jogador.getFirstNames();
            lastNames = jogador.getLastNames();
            email = jogador.getEmail();
            gender = (jogador.getGender() == null || jogador.getGender().isBlank()) ? "X" : jogador.getGender();
            birthdate = (jogador.getBirthdate() == null) ? "" : jogador.getBirthdate().toString();
            nationality = (jogador.getNationality() == null) ? "" : jogador.getNationality().getAbbreviation();
            mensagem = "Perfil atualizado com sucesso.";
        } catch (Exception e) {
            erro = e.getLocalizedMessage();
        }
    }
%>
<!DOCTYPE html>
<html lang="pt">
<head>
    <meta charset="UTF-8">
    <title>Editar Perfil</title>
    <style>
        :root {
            --accent: <%= cor %>;
            --panel: rgba(17, 24, 39, 0.9);
            --panel-2: rgba(30, 41, 59, 0.88);
            --text: #e5e7eb;
            --muted: #94a3b8;
            --danger: #f87171;
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
            align-items: stretch;
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
        input, textarea, select {
            width: 100%;
            box-sizing: border-box;
            border: 1px solid rgba(148, 163, 184, 0.22);
            background: rgba(15, 23, 42, 0.9);
            color: var(--text);
            border-radius: 12px;
            padding: 12px 14px;
            outline: none;
        }
        input:focus, textarea:focus, select:focus {
            border-color: var(--accent);
            box-shadow: 0 0 0 3px rgba(56, 189, 248, .15);
        }
        textarea {
            min-height: 120px;
            resize: vertical;
        }
        .actions {
            display: flex;
            gap: 10px;
            flex-wrap: wrap;
            margin-top: 18px;
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
        .avatar {
            width: 164px;
            height: 164px;
            border-radius: 20px;
            overflow: hidden;
            border: 4px solid var(--accent);
            background: rgba(15, 23, 42, 0.9);
            display: flex;
            align-items: center;
            justify-content: center;
            margin-bottom: 16px;
        }
        .avatar img {
            width: 100%;
            height: 100%;
            object-fit: cover;
            display: block;
        }
        .avatar span {
            font-size: 2.4rem;
            font-weight: 700;
            color: #dbeafe;
        }
        .swatch {
            width: 18px;
            height: 18px;
            display: inline-block;
            border-radius: 999px;
            background: var(--accent);
            border: 1px solid rgba(255,255,255,.35);
            vertical-align: middle;
            margin-right: 8px;
        }
        .preview {
            display: flex;
            gap: 12px;
            align-items: center;
            padding: 12px 14px;
            border-radius: 14px;
            background: var(--panel-2);
            border: 1px solid rgba(148, 163, 184, 0.18);
            margin-bottom: 14px;
        }
        .mini-preview {
            width: 56px;
            height: 56px;
            border-radius: 12px;
            overflow: hidden;
            border: 2px solid var(--accent);
            background: rgba(15, 23, 42, 0.9);
            flex: 0 0 auto;
        }
        .mini-preview img {
            width: 100%;
            height: 100%;
            object-fit: cover;
            display: block;
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
            <h1>Editar perfil</h1>
            <p>Atualiza os dados do perfil. O username e a password nao podem ser alterados nesta pagina.</p>

            <% if (mensagem != null) { %>
                <div class="status ok"><%= mensagem %></div>
            <% } else if (erro != null) { %>
                <div class="status warn"><%= erro %></div>
            <% } %>

            <div class="avatar">
                <% if (fotoBase64 != null && !fotoBase64.isBlank()) { %>
                    <img id="fotoPreview" src="data:image/jpeg;base64,<%= fotoBase64 %>" alt="Fotografia atual">
                <% } else { %>
                    <span><%= username.substring(0, 1).toUpperCase() %></span>
                <% } %>
            </div>

            <p>Jogador: <strong><%= username %></strong></p>
            <p>Símbolo atual: <strong><%= simbolo %></strong></p>
            <p>Cor atual: <span class="swatch"></span><strong><%= cor %></strong></p>
        </div>

        <div class="card">
            <form method="post" action="<%= request.getRequestURI() %>">
                <input type="hidden" name="acao" value="guardar">
                <textarea id="fotoBase64" name="fotoBase64" style="display:none;"><%= fotoBase64 %></textarea>

                <div class="field">
                    <label for="firstNames">Primeiros nomes</label>
                    <input id="firstNames" name="firstNames" value="<%= firstNames %>" required>
                </div>

                <div class="field">
                    <label for="lastNames">Apelidos</label>
                    <input id="lastNames" name="lastNames" value="<%= lastNames %>" required>
                </div>

                <div class="field">
                    <label for="email">Email</label>
                    <input id="email" name="email" type="email" value="<%= email %>" required>
                </div>

                <div class="field">
                    <label for="gender">Genero</label>
                    <select id="gender" name="gender">
                        <option value="X" <%= "X".equals(gender) ? "selected" : "" %>>Nao definido</option>
                        <option value="M" <%= "M".equals(gender) ? "selected" : "" %>>Masculino</option>
                        <option value="F" <%= "F".equals(gender) ? "selected" : "" %>>Feminino</option>
                    </select>
                </div>

                <div class="field">
                    <label for="birthdate">Data de nascimento</label>
                    <input id="birthdate" name="birthdate" type="date" value="<%= birthdate %>"
                           min="<%= java.time.LocalDate.now().minusYears(130) %>"
                           max="<%= java.time.LocalDate.now().minusYears(3) %>" required>
                </div>

                <div class="field">
                    <label for="nationality">Nacionalidade (codigo ISO de 2 letras)</label>
                    <input id="nationality" name="nationality" maxlength="2" pattern="[A-Za-z]{2}"
                           value="<%= nationality %>" required>
                </div>

                <div class="preview">
                    <div class="mini-preview">
                        <% if (fotoBase64 != null && !fotoBase64.isBlank()) { %>
                            <img id="fotoMini" src="data:image/jpeg;base64,<%= fotoBase64 %>" alt="Pré-visualização">
                        <% } %>
                    </div>
                    <div>
                        <p style="margin:0; color: var(--text);">Escolhe uma nova fotografia</p>
                        <p style="margin:4px 0 0;">A imagem é convertida para Base64 no navegador.</p>
                    </div>
                </div>

                <div class="field">
                    <label for="fotoArquivo">Fotografia</label>
                    <input id="fotoArquivo" type="file" accept="image/*">
                </div>

                <div class="field">
                    <label for="cor">Cor de fundo preferida</label>
                    <input id="cor" name="cor" type="color" value="<%= cor %>">
                </div>

                <div class="actions">
                    <button class="primary" type="submit">Guardar alterações</button>
                    <a class="btnlink secondary" href="menu.jsp">Voltar ao painel</a>
                </div>
            </form>
        </div>
    </div>
</div>
<script>
    const inputFoto = document.getElementById('fotoArquivo');
    const fotoBase64 = document.getElementById('fotoBase64');
    const fotoPreview = document.getElementById('fotoPreview');
    const fotoMini = document.getElementById('fotoMini');

    if (inputFoto) {
        inputFoto.addEventListener('change', function () {
            const file = this.files && this.files[0];
            if (!file) {
                return;
            }

            const reader = new FileReader();
            reader.onload = function () {
                const dataUrl = String(reader.result || '');
                const comma = dataUrl.indexOf(',');
                const base64 = comma >= 0 ? dataUrl.substring(comma + 1) : '';
                fotoBase64.value = base64;

                if (fotoPreview) {
                    fotoPreview.src = dataUrl;
                }
                if (fotoMini) {
                    fotoMini.src = dataUrl;
                } else {
                    const mini = document.querySelector('.mini-preview');
                    if (mini) {
                        mini.innerHTML = '<img src="' + dataUrl + '" alt="Pré-visualização">';
                    }
                }
            };
            reader.readAsDataURL(file);
        });
    }
</script>
</body>
</html>
