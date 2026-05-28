package client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import util.MyImage;
import util.XMLDoc;

/**
 * Classe que implementa a adaptação do cliente/jogador ao protocolo. 
 * Suporta a interação do cliente com o servidor, 
 * Converte mensagens em XML para acções/objetos e vice-versa.
 * 
 * @author Engº Porfírio Filipe
 */
public class Stub implements AutoCloseable {

	// **Atributos:**

	// Stream para ler dados do socket.
	private BufferedReader is = null;
	// Stream para escrever dados no socket.
	private PrintWriter os = null;
	// dados do jogador atual, conhecido após autenticação
        private Document registo=null;

	// **Construtor:**

	/**
	 * Construtor que recebe um socket e inicia os streams de entrada e saída.
	 *
	 * @param sk Socket 	Ligação ao servidor.
	 * @throws IOException 	Se ocorrer um erro ao iniciar os streams.
	 */
	public Stub(Socket sk) throws IOException {
		// Cria um BufferedReader para ler do socket.
		is = new BufferedReader(new InputStreamReader(sk.getInputStream()));
		// Cria um PrintWriter para escrever no socket.
		os = new PrintWriter(sk.getOutputStream(), true);
	}
	
	/**
	 * Fecha a comunicação com o servidor
	 */
	@Override
	public void close() {
		// Fecha o BufferedReader.
		try {
			is.close();
		} catch (IOException e) {// ignora erro
		}
		// Fecha o PrintWriter.
		os.close();
	}

	/**
	 * 📝 Converte o tabuleiro do jogo em XML para uma string formatada para ser
	 * apresentada na consola.
	 * Usa ✖️ e ⭕ para as jogadas e números para as casas vazias.
	 * @param tabuleiro O elemento XML <tabuleiro> que contém as <casa>
	 * @return String formatada para exibição na consola
	 */
	public static String tabuleiroToTXT(final Element tabuleiro) {
	    // 🧱 Usamos StringBuilder para uma construção de String mais eficiente e rápida
	    StringBuilder sb = new StringBuilder();
	    
	    // 🗂️ Obtém a lista de todos os nós <casa> dentro do elemento tabuleiro
	    NodeList casas = tabuleiro.getElementsByTagName("casa");
	    
	    int contador = 1; // 📍 Contador para identificar as quadrículas de 1 a 9

	    // 🔄 Ciclo para percorrer as 3 linhas do tabuleiro
	    for (int i = 0; i < 3; i++) {
	        // 🔄 Ciclo para percorrer as 3 colunas de cada linha
	        for (int j = 0; j < 3; j++) {
	            
	            // 🔍 Extrai o atributo "simbolo" da casa correspondente ao índice atual
	            // O índice na NodeList é (contador - 1) pois a lista começa em 0
	            String attr = ((Element) casas.item(contador - 1)).getAttribute("simbolo");
	            char simbolo = (attr.length() > 0) ? attr.charAt(0) : ' ';

	            // 🎨 Lógica de conversão visual para emojis pesados
	            if (simbolo == 'X') {
	                sb.append(" ✖️ "); // Estilo semelhante ao emoji de multiplicação
	            } else if (simbolo == 'O') {
	                sb.append(" ⭕ "); // Círculo vermelho oco para contraste
	            } else {
	                // 🔢 Se a casa estiver vazia, mostra o seu número (1 a 9)
	                // Os espaços extra garantem que o alinhamento se mantém com os emojis
	                sb.append(" ").append(contador).append("  ");
	            }

	            // 📏 Adiciona a barra vertical separadora, exceto na última coluna
	            if (j < 2) {
	                sb.append("|");
	            }
	            
	            contador++; // Próxima casa...
	        }
	        
	        sb.append("\n"); // 📑 Salto de linha após completar uma fila
	        
	        // ➖ Adiciona a linha horizontal separadora entre as filas
	        if (i < 2) {
	            sb.append("------------\n");
	        }
	    }
	    
	    // ✨ Retorna o tabuleiro finalizado pronto a ser impresso
	    return sb.toString();
	}

	/**
	 * 🎨 Converte o elemento XML <tabuleiro> em elementos gráficos SVG.
	 * @param tabuleiro O elemento XML <tabuleiro> obtido do servidor.
	 * @return String contendo os elementos <line>, <circle> e <a> do SVG.
	 * Ordem de desenho: Grelha -> Peças Existentes -> Áreas de Clique (Links).
	 */
	public static String tabuleiroToSVG(Element tabuleiro) {
    StringBuilder sb = new StringBuilder();
    sb.append("<svg width='300' height='300' class='tabuleiro-svg' xmlns='http://www.w3.org/2000/svg'>");

    // Grelha
    sb.append("<line x1='100' y1='10' x2='100' y2='290' class='grelha' />");
    sb.append("<line x1='200' y1='10' x2='200' y2='290' class='grelha' />");
    sb.append("<line x1='10' y1='100' x2='290' y2='100' class='grelha' />");
    sb.append("<line x1='10' y1='200' x2='290' y2='200' class='grelha' />");

    NodeList casas = tabuleiro.getElementsByTagName("casa");

    for (int i = 0; i < 9; i++) {
        int x = (i % 3) * 100;
        int y = (i / 3) * 100;
        
        Element casa = (Element) casas.item(i);
        String attr = casa.getAttribute("simbolo");

        if (attr.equals("X")) {
            sb.append("<g class='x-peca'>");
            sb.append("<line x1='").append(x+25).append("' y1='").append(y+25).append("' x2='").append(x+75).append("' y2='").append(y+75).append("' />");
            sb.append("<line x1='").append(x+75).append("' y1='").append(y+25).append("' x2='").append(x+25).append("' y2='").append(y+75).append("' />");
            sb.append("</g>");
        } else if (attr.equals("O")) {
            sb.append("<circle cx='").append(x+50).append("' cy='").append(y+50).append("' r='25' class='o-peca' />");
        } else {
            // Se não é X nem O, assume-se VAZIO (tal como no seu método TXT)
            sb.append("<a onclick='clique(event, "+i+");' href='?jogada=").append(i+1).append("'>");
            sb.append("<rect x='").append(x).append("' y='").append(y).append("' width='100' height='100' class='casa' />");
            sb.append("</a>");
        }
    }
    sb.append("</svg>");
    return sb.toString();
	}
	
	/**
	 * Converte o estado do jogo (VO, VX, EM, IV e ND) para uma string 
	 * que representa a mensagem a ser apresentada ao jogador.
	 *
	 * @param  valor contem a abreviatura do estado do jogo.
	 * @return String com a mensagem a ser apresentada ao jogador.
	 */
	public static String estadoToTXT(final String valor) {
		switch (valor) {
		case "VO":
			return "Vitória do O!";
		case "VX":
			return "Vitória do X!";
		case "EM":
			return "Empate.";
		case "IV":
			return "Jogada inválida!";
		default:
			return ""; //nada a registar
		}
	}
	
    /**
     * Valida se o documento XML recebido representa um método válido.
     *
     * @param doc Documento XML a ser validado.
     * @param xsdPath CAminho para o XSD usado na validação.
     * @throws Exception Se o documento não for válido.
     */
    private void validXSD(final Document doc, final String xsdPath) throws Exception {
        try {
            // Valida o documento contra o XSD.
            XMLDoc.validDocXSD(doc, xsdPath);
        } catch (SAXException | IOException e) {
            throw new Exception("Recebeu mensagem inválida: " + e.getLocalizedMessage());
        }
    }
    
    // valida as mensagens recebidas do servidor
    private void validCli(final Document doc) throws Exception {
        // Valida o documento contra o XSD "metodos.xsd".
    	validXSD(doc, XMLDoc.getContexto()+"metodos-cli.xsd");
    }
    
    /**
     * @param ficheiro de destino
     * @param texto	a adicionar o ficheiro
     * @throws IOException se houver erro
     */
    private static void adicionarStringFicheiro(String ficheiro, String texto) {
        try (
            // Cria um PrintWriter para escrever no ficheiro.
            PrintWriter escritor = new PrintWriter(new BufferedWriter(new FileWriter(ficheiro, true)))) {

            // Escreve a string no ficheiro.
            escritor.println(texto);
        } catch (IOException e) {
            // Ignora o erro, pode haver divergencia no caminho relativo/absoluto do ficheiro
	}
    }
    
    /**
     * @param evento 		mensagem a resgistar
     * @throws IOException	em caso de erro
     */
    private static void registaLog(String evento) {
	adicionarStringFicheiro(XMLDoc.getContexto()+"protocolo.log", LocalDateTime.now()+" - "+evento.replaceAll("\n",""));
    }
    
    private Element getElement(String tag) {
	NodeList itens = registo.getElementsByTagName(tag);
	if(itens.getLength()==1)
	    return (Element)itens.item(0);
	return null;
    }
    
    private String getText(String tag) {
	Element el = getElement(tag);
	if(el==null)
	    return "";
	else
	    return el.getTextContent();
    }
    /**
     * Mostra todos os dados do jogador autenticado
     */
    public void print() {
	if(registo==null) {
		System.out.println("Não existe jogador autenticado!");
		return;
	}
	System.out.println("------------ Jogador '"+getElement("jogador").getAttribute("simbolo")+"' ------------");
	System.out.println("Identificador (UUID): " + getText("userid"));
	System.out.println("Última atualização em: " + getText("updated"));
	System.out.println("Perfil: " + getText("profile"));
	System.out.println("Nome de utilizador: " + getText("username"));
	System.out.println("Nome completo: " + getText("full-name"));
	String email=getText("email");
	String gender=getText("gender");
	String birthdate=getText("birthdate");
	String nationality=getText("pt-nationality");
	if(gender.equals("M"))
	    nationality=getText("pt-male");
	else if(gender.equals("F"))
	    nationality=getText("pt-female");
	String flag=getText("flag");
	String photography=getText("photography");
	
	if (!email.isBlank())
	    System.out.println("Endereço de email: " + getText("email"));
	if (!gender.isBlank())
	    System.out.println("Género: " + gender);
	if (!birthdate.isBlank()) {
	    System.out.println("Data de nascimento: " + birthdate);
	    System.out.println("Idade: " + getText("age"));}
	if (!nationality.isBlank()) 
	    System.out.println("Nacionalidade: " + nationality);
	System.out.println("--------------------------------------------------------");
	
	if (!photography.isBlank()) {
	    MyImage p = new MyImage();
	    p.setBase64(photography);
	    try {
		p.view();
	    } catch (Exception e) {
		// e.printStackTrace();
	    }
	}
	if (!flag.isBlank()) {
	    MyImage f = new MyImage();
	    f.setBase64(flag);
	    try {
		f.view();
	    } catch (Exception e) {
		// e.printStackTrace();
	    }
	}
    }
    
    /**
     * 👑 =======================================================================
     * MÉTODO REFEITO: printJSP() - NACIONALIDADE JUNTO À BANDEIRA
     * =======================================================================
     * 🎯 Objetivo: Apresentar os dados reais e estruturar a origem do jogador
     * (Texto da Nacionalidade + Imagem da Bandeira) no painel lateral direito.
     * 📐 Alinhamento: Liberta espaço no bloco central, movendo a nacionalidade
     * para a direita, evitando o esmagamento do nome de utilizador.
     * =======================================================================
     */
    public String printJSP() {
        
        // 🛑 PASSO 1: Validação de Segurança
        if (registo == null) {
            return "";
        }

        StringBuilder html = new StringBuilder();
        
        try {
            // 🔍 PASSO 2: Extração Real de Dados do XML (Fidelidade ao protocolo)
            String simbolo      = getElement("jogador").getAttribute("simbolo");
            String userid       = getText("userid");
            String updated      = getText("updated");
            String profile      = getText("profile");
            String username     = getText("username");
            String fullName     = getText("full-name");
            String email        = getText("email");
            String gender       = getText("gender");
            String birthdate    = getText("birthdate");
            String age          = getText("age");
            
            // 🛡️ Higienização estrita: Valores nulos são convertidos para o estado em branco ou sinalizados
            if (simbolo == null) simbolo = "?";
            userid    = (userid == null)    ? "[N/D]" : userid.trim();
            updated   = (updated == null)   ? "[N/D]" : updated.trim();
            profile   = (profile == null)   ? "[N/D]" : profile.trim();
            username  = (username == null)  ? ""      : username.trim();
            fullName  = (fullName == null)  ? ""      : fullName.trim();
            email     = (email == null)     ? ""      : email.trim();
            gender    = (gender == null)    ? ""      : gender.trim();
            birthdate = (birthdate == null) ? ""      : birthdate.trim();
            age       = (age == null)       ? ""      : age.trim();
            
            // 🌍 PASSO 3: Extração e Processamento da Nacionalidade
            String nationality = getText("pt-nationality");
            if ("M".equals(gender)) {
                nationality = getText("pt-male");
            } else if ("F".equals(gender)) {
                nationality = getText("pt-female");
            }
            nationality = (nationality == null) ? "" : nationality.trim();
            
            String flag        = getText("flag");
            String photography = getText("photography");

            // 🎨 PASSO 4: Contentor Principal (Restrito à largura de 400px do tabuleiro)
            html.append("<div class='perfil-jogador' style='");
            html.append("background: #edf2f7; ");
            html.append("padding: 12px 15px; ");
            html.append("border-radius: 10px; ");
            html.append("max-width: 400px; ");             
            html.append("margin: 0 auto 15px auto; ");     
            html.append("box-sizing: border-box; ");       
            html.append("display: flex; ");
            html.append("flex-direction: column; ");       // Camada superior e inferior técnica
            html.append("gap: 10px; ");
            html.append("box-shadow: 0 4px 6px rgba(0,0,0,0.05); ");
            html.append("border-left: 5px solid #319795;"); 
            html.append("'>");

            // --- SECÇÃO SUPERIOR (Foto | Dados Principais | Origem e Peça) ---
            html.append("  <div style='display: flex; width: 100%; gap: 12px; align-items: center;'>");

            // 📸 PASSO 5: Fotografia de Perfil
            if (photography != null && !photography.isBlank()) {
                html.append("    <img src='data:image/png;base64,").append(photography.trim()).append("' style='");
                html.append("width: 55px; height: 55px; ");
                html.append("border-radius: 50%; ");      
                html.append("object-fit: cover; ");       
                html.append("border: 2px solid #319795; "); 
                html.append("flex-shrink: 0; ");            
                html.append("' alt='Foto Perfil'>");
            } else {
                html.append("    <div style='width: 55px; height: 55px; border-radius: 50%; background: #cbd5e0; display: flex; align-items: center; justify-content: center; font-size: 1.3em; color: white; flex-shrink: 0;'>👤</div>");
            }

            // 📝 PASSO 6: Bloco de Texto Central (Nome, Email e Aniversário)
            html.append("    <div style='flex-grow: 1; font-family: sans-serif; line-height: 1.3; text-align: left;'>");
            
            // Nome + @username (Permite quebra caso o nome ocupe muito espaço na linha)
            html.append("      <div style='font-size: 1.1em; color: #2d3748; font-weight: bold;'>");
            if (!fullName.isBlank()) {
                html.append(fullName).append(" ");
            }
            if (!username.isBlank()) {
                html.append("<span style='font-size: 0.85em; color: #718096; font-weight: normal;'>(@").append(username).append(")</span>");
            } else {
                html.append("<span style='font-size: 0.85em; color: red; font-weight: normal;'>([N/D @])</span>");
            }
            html.append("      </div>");
            
            // Email (Quebra se for muito comprido para não deformar o container)
            if (!email.isBlank()) {
                html.append("      <div style='font-size: 0.85em; color: #4a5568; word-break: break-all;'>📧 ").append(email).append("</div>");
            }
            
            // Metadados Temporais apenas (Idade e Nascimento)
            if (!birthdate.isBlank()) {
                html.append("      <div style='font-size: 0.8em; color: #718096; margin-top: 2px;'>");
                html.append("🎂 ").append(age).append(" anos (").append(birthdate).append(")");
                html.append("      </div>");
            }
            html.append("    </div>"); // Fim do bloco central

            // 🏷️ PASSO 7: Painel Lateral Direito (Peça + Bloco de Origem Agrupado)
            html.append("    <div style='text-align: right; display: flex; flex-direction: column; align-items: center; justify-content: center; min-width: 80px; flex-shrink: 0; font-family: sans-serif;'>");
            
            // Exibição da Peça (X ou O)
            html.append("      <span style='font-size: 0.65em; color: #718096; text-transform: uppercase; font-weight: bold; display: block; line-height: 1;'>Peça</span>");
            html.append("      <span style='font-size: 1.5em; color: #319795; font-weight: bold; line-weight: 1.1; margin-bottom: 4px;'>").append(simbolo).append("</span>");
            
            // ⭐ INJEÇÃO COLOQUIAL DA NACIONALIDADE AO PÉ DA BANDEIRA
            if (!nationality.isBlank() || (flag != null && !flag.isBlank())) {
                html.append("      <div style='display: flex; align-items: center; gap: 4px; justify-content: flex-end;'>");
                
                // Texto da Nacionalidade (Tamanho menor para não colidir com o centro)
                if (!nationality.isBlank()) {
                    html.append("        <span style='font-size: 0.75em; color: #4a5568; font-weight: 500;'>").append(nationality).append("</span>");
                }
                
                // Renderização da Bandeira ao lado do texto
                if (flag != null && !flag.isBlank()) {
                    html.append("        <img src='data:image/png;base64,").append(flag.trim()).append("' style='");
                    html.append("width: 18px; height: 12px; ");
                    html.append("box-shadow: 0 1px 2px rgba(0,0,0,0.15);"); 
                    html.append("' alt='Bandeira'>");
                }
                
                html.append("      </div>");
            }
            
            html.append("    </div>");
            html.append("  </div>"); // Fim da Secção Superior

            // --- SECÇÃO INFERIOR TÉCNICA (UUID, Perfil Técnico e Última Atualização) ---
            html.append("  <div style='border-top: 1px solid #e2e8f0; padding-top: 6px; margin-top: 2px; display: flex; flex-direction: column; gap: 3px; font-family: monospace; font-size: 0.75em; color: #718096; text-align: left;'>");
            
            html.append("    <div><b style='color:#4a5568;'>Perfil:</b> ").append(profile.isBlank() ? "[N/D]" : profile).append("</div>");
            html.append("    <div style='word-break: break-all;'><b style='color:#4a5568;'>Identificador (UUID):</b> ").append(userid.isBlank() ? "[N/D]" : userid).append("</div>");
            html.append("    <div><b style='color:#4a5568;'>Última atualização em:</b> ").append(updated.isBlank() ? "[N/D]" : updated).append("</div>");
            
            html.append("  </div>"); 

            html.append("</div>"); 

        } catch (Exception e) {
            e.printStackTrace();
            return "<div class='perfil-jogador' style='max-width: 400px; margin: 0 auto 15px auto; color: red; padding: 10px;'>⚠️ Erro ao processar tags do XML.</div>";
        }

        return html.toString();
    }
    
	/**
	 * Realiza a autenticação do jogador no servidor.
	 *
	 * @param user Nome do jogador.
	 * @param pass Senha do jogador.
	 * @return Símbolo do jogador (O ou X).
	 * @throws Exception   Se ocorrer um erro durante a autenticação.
	 * @throws IOException Se ocorrer um erro de comunicação com o servidor.
	 */
	public char iniciar(final String user, final String pass) throws IOException, Exception {

		// **1. Enviar a mensagem de autenticação para o servidor:**
		// Envia a string de acordo com o formato indicado no XSD.
		os.println("<metodo><iniciar nickname='" + user 
					   + "' senha='" + pass + "'/></metodo>");

		// **2. Receber a resposta do servidor e analisar o XML:**
		// Recebe a resposta do servidor.
		String resposta = is.readLine();
		registaLog("Cliente{"+resposta+"}");
		if(resposta==null)
			throw new Exception("Ligação ao servidor cancelada remotamente!");
		// Processa a resposta como um documento XML.
		registo = XMLDoc.parseString(resposta);
		
		validCli(registo);
		
		// **3. Obter o elemento "jogador" da resposta:**
		// Obtém a lista de elementos "jogador".
		NodeList jogadores = registo.getElementsByTagName("jogador");
	
		// **4. Vai retornar o símbolo do jogador:**
		// Obtem o conteúdo do atributo 'símbolo' do elemento "jogador".
		// Retorna o símbolo do jogador.
		return ((Element)jogadores.item(0)).getAttribute("simbolo").charAt(0);
	}

	/**
	 * Obtém o tabuleiro do jogo a partir do servidor.
	 *
	 * @return Elemento XML que representa o tabuleiro do jogo.
	 * @throws Exception   Se ocorrer um erro durante a comunicação com o servidor.
	 * @throws IOException Se ocorrer um erro de leitura do socket.
	 */
	public Element obter() throws IOException, Exception {
		// Envia a mensagem "obter" para o servidor.
		os.println("<metodo><obter/></metodo>");
		// Recebe a resposta do servidor e processa o XML.
		String resposta=is.readLine();
		registaLog("Cliente{"+resposta+"}");
		if(resposta==null)
			throw new Exception("Ligação ao servidor cancelada remotamente!");
		Document d = XMLDoc.parseString(resposta);
		validCli(d);
		// Obtém o elemento "tabuleiro" da resposta.
		Element tabuleiro = (Element) d.getElementsByTagName("tabuleiro").item(0);
		// Retorna o elemento "tabuleiro".
		return tabuleiro;
	}

	/**
	 * Realiza uma jogada.
	 *
	 * @param numero Número da jogada (1 a 9).
	 * @throws IOException Se ocorrer um erro de comunicação com o servidor.
	 * @throws Exception   Se ocorrer um erro ao processar a resposta do servidor.
	 */
	public void jogar(final short numero) throws IOException, Exception {
		// Verifica se o número da casa do jogo é válido.
		if (numero<0 || numero>9) 
			throw new Exception("Número da casa do jogo inválido!");
		// Envia a mensagem com a jogada para o servidor.
		os.println("<metodo><jogar jogada='" + numero + "'/></metodo>");
		// Recebe a resposta do servidor e retorna o estado.
		String resposta=is.readLine();
		registaLog("Cliente{"+resposta+"}");
		if(resposta==null)
			throw new Exception("Ligação ao servidor cancelada remotamente!");
		Document d = XMLDoc.parseString(resposta);  // consome a linha!
		validCli(d);
	}
}
