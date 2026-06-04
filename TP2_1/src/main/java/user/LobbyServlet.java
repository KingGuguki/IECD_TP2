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
                // Bloqueia a thread HTTP enquanto o Servidor TCP não encontrar match!
                char simbolo = stub.entrarFila(); 
                
                // Grava o símbolo recebido na sessão
                session.setAttribute("tp2_simbolo", String.valueOf(simbolo));
                out.print("{\"status\": \"ok\", \"simbolo\": \"" + simbolo + "\"}");
            } 
            else if ("desafiar".equals(acao)) {
                // String target = request.getParameter("target");
                // TODO: Futuramente, usar XML próprio <desafiar alvo="X"/>.
                char simbolo = stub.entrarFila(); 
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
