package user;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import util.XMLDoc;

@WebServlet("/halloffame")
public class HallOfFameServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        try {
            User._load();
            List<PlayerStats> ranking = new ArrayList<>();
            
            Document doc = XMLDoc.parseFile(XMLDoc.getContexto() + "users.xml");
            if (doc != null) {
                NodeList users = doc.getElementsByTagName("user");
                for (int i = 0; i < users.getLength(); i++) {
                    Element userEl = (Element) users.item(i);
                    User u = new User();
                    u.fromElement(userEl);
                    
                    int vitorias = u.getVitorias();
                    int derrotas = u.getDerrotas();
                    int empates = u.getEmpates();
                    int tempo = u.getTempo();
                    
                    int totalJogos = vitorias + derrotas + empates;
                    double tempoMedio = 0;
                    if (totalJogos > 0) {
                        tempoMedio = (double) tempo / totalJogos;
                    }
                    
                    String foto = u.getPhotography() != null ? u.getPhotography().getBase64() : "";
                    String nacionalidade = "";
                    try {
                        nacionalidade = u.getPtNationality();
                    } catch (Exception e) {}
                    
                    ranking.add(new PlayerStats(
                        u.getUsername(),
                        u.getName(),
                        foto,
                        nacionalidade,
                        vitorias,
                        derrotas,
                        empates,
                        tempoMedio
                    ));
                }
            }
            
            // Sort by vitorias DESC, then tempoMedio ASC
            Collections.sort(ranking, new Comparator<PlayerStats>() {
                @Override
                public int compare(PlayerStats p1, PlayerStats p2) {
                    if (p1.vitorias != p2.vitorias) {
                        return Integer.compare(p2.vitorias, p1.vitorias);
                    }
                    return Double.compare(p1.tempoMedio, p2.tempoMedio);
                }
            });
            
            // Generate JSON array manually (since standard libraries like Gson may not be available)
            StringBuilder json = new StringBuilder();
            json.append("[");
            for (int i = 0; i < ranking.size(); i++) {
                PlayerStats p = ranking.get(i);
                json.append("{");
                json.append("\"username\": \"").append(escapeJson(p.username)).append("\", ");
                json.append("\"fullName\": \"").append(escapeJson(p.fullName)).append("\", ");
                json.append("\"photo\": \"").append(escapeJson(p.photo)).append("\", ");
                json.append("\"nationality\": \"").append(escapeJson(p.nationality)).append("\", ");
                json.append("\"vitorias\": ").append(p.vitorias).append(", ");
                json.append("\"derrotas\": ").append(p.derrotas).append(", ");
                json.append("\"empates\": ").append(p.empates).append(", ");
                json.append("\"tempoMedio\": ").append(p.tempoMedio);
                json.append("}");
                if (i < ranking.size() - 1) {
                    json.append(", ");
                }
            }
            json.append("]");
            
            out.print(json.toString());
            
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
    
    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
    
    private static class PlayerStats {
        String username;
        String fullName;
        String photo;
        String nationality;
        int vitorias;
        int derrotas;
        int empates;
        double tempoMedio;
        
        PlayerStats(String username, String fullName, String photo, String nationality, 
                    int vitorias, int derrotas, int empates, double tempoMedio) {
            this.username = username;
            this.fullName = fullName;
            this.photo = photo;
            this.nationality = nationality;
            this.vitorias = vitorias;
            this.derrotas = derrotas;
            this.empates = empates;
            this.tempoMedio = tempoMedio;
        }
    }
}
