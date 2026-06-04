package user;

import java.io.IOException;
import java.io.PrintWriter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import util.XMLDoc;

@WebServlet("/buscar-jogadores")
public class PesquisaJogadoresServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String query = request.getParameter("query");
        if (query == null) {
            query = "";
        }
        query = query.trim().toLowerCase();

        response.setContentType("application/json; charset=UTF-8");
        response.addHeader("Access-Control-Allow-Origin", "*");
        
        PrintWriter out = response.getWriter();
        out.print("[");
        
        try {
            // Garantir que a base de dados está carregada/sincronizada
            try {
                User._load();
            } catch (Exception e) {
                // Ignore load error
            }

            String xmlPath = XMLDoc.getContexto() + "users.xml";
            Document doc = XMLDoc.parseFile(xmlPath);
            
            if (doc != null) {
                NodeList users = doc.getElementsByTagName("user");
                boolean first = true;
                
                for (int i = 0; i < users.getLength(); i++) {
                    Element u = (Element) users.item(i);
                    
                    String username = getElementText(u, "username");
                    String firstnames = getElementText(u, "firstnames");
                    String lastnames = getElementText(u, "lastnames");
                    String nationality = getElementText(u, "nationality");
                    String blockedStr = getElementText(u, "blocked");
                    
                    if ("true".equalsIgnoreCase(blockedStr)) {
                        continue;
                    }

                    String fullName = firstnames + " " + lastnames;

                    if (query.isEmpty() || username.toLowerCase().contains(query) || fullName.toLowerCase().contains(query)) {
                        String photo = getElementText(u, "photography");
                        if (!photo.isEmpty() && !photo.startsWith("data:image/")) {
                            photo = "data:image/jpeg;base64," + photo;
                        }

                        if (!first) {
                            out.print(",");
                        }
                        first = false;

                        out.printf("{\"username\":\"%s\", \"fullName\":\"%s\", \"nationality\":\"%s\", \"photo\":\"%s\"}", 
                            escapeJson(username), 
                            escapeJson(fullName),
                            escapeJson(nationality),
                            escapeJson(photo));
                    }
                }
            }
        } catch (Exception e) {
            // Se houver erro retorna um JSON vazio válido "[]"
        }
        
        out.print("]");
        out.flush();
    }

    private String getElementText(Element parent, String tag) {
        NodeList nl = parent.getElementsByTagName(tag);
        if (nl.getLength() > 0 && nl.item(0) != null) {
            return nl.item(0).getTextContent().trim();
        }
        return "";
    }

    private String escapeJson(String val) {
        if (val == null) return "";
        return val.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}
