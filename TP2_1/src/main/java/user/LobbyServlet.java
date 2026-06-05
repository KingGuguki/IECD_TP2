package user;

import java.io.IOException;
import java.io.PrintWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import client.Stub;

/**
 * Servlet responsável pela interação de Lobby (Matchmaking).
 * Recebe os pedidos via AJAX/Fetch do jogo.jsp para entrar na fila,
 * usando a instância TCP mantida na Sessão.
 */
@WebServlet("/lobby")
public class LobbyServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private interface StubAction<T> {
        T execute(Stub stub) throws Exception;
    }

    private <T> T executeWithRetry(HttpSession session, StubAction<T> action) throws Exception {
        Stub stub = (Stub) session.getAttribute("tp2_stub");
        try {
            return action.execute(stub);
        } catch (Exception socketMorto) {
            String nick = (String) session.getAttribute("tp2_username");
            String senha = (String) session.getAttribute("tp2_senha");
            
            if (nick != null && senha != null) {
                java.net.Socket oldSocket = (java.net.Socket) session.getAttribute("tp2_socket");
                if (oldSocket != null && !oldSocket.isClosed()) {
                    try { oldSocket.close(); } catch (Exception e) {}
                }
                
                java.net.Socket novoSocket = new java.net.Socket("localhost", 5025);
                Stub novoStub = new Stub(novoSocket);
                novoStub.iniciar(nick, senha);
                
                session.setAttribute("tp2_socket", novoSocket);
                session.setAttribute("tp2_stub", novoStub);
                
                return action.execute(novoStub);
            } else {
                throw socketMorto;
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("tp2_stub") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"error\": \"Não autenticado\"}");
            return;
        }

        String acao = request.getParameter("action");
        Stub stub = (Stub) session.getAttribute("tp2_stub");

        try {
            if ("entrar_fila".equals(acao)) {
                char simbolo = executeWithRetry(session, s -> s.entrarFila());
                session.setAttribute("tp2_simbolo", String.valueOf(simbolo));
                out.print("{\"status\": \"ok\", \"simbolo\": \"" + simbolo + "\"}");
            } 
            else if ("desafiar".equals(acao)) {
                String target = request.getParameter("target");
                if (target == null || target.isBlank()) throw new Exception("Alvo não especificado");
                char simbolo = executeWithRetry(session, s -> s.desafiar(target));
                session.setAttribute("tp2_simbolo", String.valueOf(simbolo));
                out.print("{\"status\": \"ok\", \"simbolo\": \"" + simbolo + "\"}");
            }
            else if ("cancelar_desafio".equals(acao)) {
                executeWithRetry(session, s -> { s.cancelarDesafio(); return null; });
                out.print("{\"status\": \"ok\"}");
            }
            else if ("verificar_convites".equals(acao)) {
                String inviter = executeWithRetry(session, s -> s.verificarConvites());
                if (inviter != null) {
                    out.print("{\"convite\": \"" + inviter + "\"}");
                } else {
                    out.print("{\"convite\": null}");
                }
            }
            else if ("aceitar_desafio".equals(acao)) {
                String de = request.getParameter("de");
                char simbolo = executeWithRetry(session, s -> s.aceitarDesafio(de));
                session.setAttribute("tp2_simbolo", String.valueOf(simbolo));
                out.print("{\"status\": \"ok\", \"simbolo\": \"" + simbolo + "\"}");
            }
            else if ("sair_jogo".equals(acao)) {
                // Remove o símbolo da sessão HTTP para voltar a ver o Lobby
                session.removeAttribute("tp2_simbolo");
                out.print("{\"status\": \"ok\"}");
            }
            else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"error\": \"Ação inválida\"}");
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}
