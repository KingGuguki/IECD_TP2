package user;

import java.io.IOException;
import java.io.PrintWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.w3c.dom.Element;
import client.Stub;
import util.XMLDoc;

/**
 * Servlet responsável pelo motor de jogo (Dots and Boxes) na Web.
 * Interage com a classe Stub para fazer jogadas e obter o tabuleiro.
 */
@WebServlet("/game")
public class GameServlet extends HttpServlet {
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
            if ("estado".equals(acao)) {
                // Obtém o elemento <tabuleiro> atual do Servidor TCP
                Element tabuleiro = stub.obter();
                
                // Extrai a String XML para enviar para a Web
                String xmlString = XMLDoc.documentToString(tabuleiro.getOwnerDocument());
                
                // Embala o XML em JSON para uma leitura limpa no fetch()
                String safeXml = xmlString.replace("\"", "\\\"").replace("\n", "").replace("\r", "");
                out.print("{\"status\": \"ok\", \"xml\": \"" + safeXml + "\"}");
            } 
            else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"error\": \"Ação inválida\"}");
            }
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && (msg.contains("remotamente") || msg.contains("aborted") || msg.contains("Ligação"))) {
                session.removeAttribute("tp2_simbolo");
            }
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
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
            if ("jogar".equals(acao)) {
                String linhaStr = request.getParameter("linha");
                if (linhaStr == null || linhaStr.isEmpty()) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{\"error\": \"Linha não especificada\"}");
                    return;
                }
                
                short linha = Short.parseShort(linhaStr);
                
                // Envia a jogada para o Servidor TCP. O Stub lança Exception se for inválida.
                Element tabuleiro = stub.jogar(linha);
                
                String xmlString = XMLDoc.documentToString(tabuleiro.getOwnerDocument());
                String safeXml = xmlString.replace("\"", "\\\"").replace("\n", "").replace("\r", "");
                
                out.print("{\"status\": \"ok\", \"xml\": \"" + safeXml + "\"}");
            }
            else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"error\": \"Ação inválida\"}");
            }
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && (msg.contains("remotamente") || msg.contains("aborted") || msg.contains("Ligação"))) {
                session.removeAttribute("tp2_simbolo");
            }
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            // Captura erros como "Jogada inválida!"
            out.print("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}
