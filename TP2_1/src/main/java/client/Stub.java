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
	public String tabuleiroToTXT(final Element tabuleiro) {
	    NodeList linhas = tabuleiro.getElementsByTagName("linha");
	    if (linhas.getLength() > 0) {
	        return tabuleiroPontosCaixasToTXT(tabuleiro);
	    }

	    // Fallback para o antigo tabuleiro do galo.
	    StringBuilder sb = new StringBuilder();
	    NodeList casas = tabuleiro.getElementsByTagName("casa");
	    int contador = 1;

	    for (int i = 0; i < 3; i++) {
	        for (int j = 0; j < 3; j++) {
	            String attr = ((Element) casas.item(contador - 1)).getAttribute("simbolo");
	            char simbolo = (attr.length() > 0) ? attr.charAt(0) : ' ';

	            if (simbolo == 'X') {
	                sb.append(" ✖️ ");
	            } else if (simbolo == 'O') {
	                sb.append(" ⭕ ");
	            } else {
	                sb.append(" ").append(contador).append("  ");
	            }

	            if (j < 2) {
	                sb.append("|");
	            }
	            contador++;
	        }
	        sb.append("\n");
	        if (i < 2) {
	            sb.append("------------\n");
	        }
	    }

	    return sb.toString();
	}

	/**
	 * Converte o tabuleiro do jogo em XML para uma string formatada para ser
	 * apresentada na consola com pontos numerados.
	 * * @param tabuleiro O elemento XML <tabuleiro> que contém as <linha> e <caixa>
	 * @return String formatada para exibição na consola
	 */
	public String tabuleiroPontosCaixasToTXT(final Element tabuleiro) 
	{
		String strLinhas = tabuleiro.getAttribute("linhas");
		String strColunas = tabuleiro.getAttribute("colunas");
		
		int pontosLinhas = (strLinhas == null || strLinhas.isEmpty()) ? 3 : Integer.parseInt(strLinhas);
		int pontosColunas = (strColunas == null || strColunas.isEmpty()) ? 3 : Integer.parseInt(strColunas);
		
		boolean[][] hLine = new boolean[pontosLinhas][pontosColunas - 1];
		boolean[][] vLine = new boolean[pontosLinhas - 1][pontosColunas];
		char[][] caixas = new char[pontosLinhas - 1][pontosColunas - 1];

		NodeList linhaNodes = tabuleiro.getElementsByTagName("linha");
		for (int i = 0; i < linhaNodes.getLength(); i++) 
		{
			Element linha = (Element) linhaNodes.item(i);
			String strL = linha.getAttribute("linha");
			String strC = linha.getAttribute("coluna");
			
			if (strL == null || strL.isEmpty() || strC == null || strC.isEmpty()) 
			{
				continue;
			}
			
			int l = Integer.parseInt(strL);
			int c = Integer.parseInt(strC);
			String tipo = linha.getAttribute("tipo");
			boolean ocupada = "true".equals(linha.getAttribute("ocupada"));
			
			if ("H".equals(tipo)) 
			{
				hLine[l][c] = ocupada;
			} 
			else 
			{
				vLine[l][c] = ocupada;
			}
		}

		NodeList caixaNodes = tabuleiro.getElementsByTagName("caixa");
		int idxCaixa = 0;
		for (int i = 0; i < pontosLinhas - 1; i++) 
		{
			for (int j = 0; j < pontosColunas - 1; j++) 
			{
				if (idxCaixa < caixaNodes.getLength()) 
				{
					Element caixa = (Element) caixaNodes.item(idxCaixa);
					String strL = caixa.getAttribute("linha");
					String strC = caixa.getAttribute("coluna");
					String dono = caixa.getAttribute("dono");
					char cDono = (dono == null || dono.isEmpty()) ? ' ' : dono.charAt(0);
					
					if (strL != null && !strL.isEmpty() && strC != null && !strC.isEmpty()) 
					{
						int l = Integer.parseInt(strL);
						int c = Integer.parseInt(strC);
						caixas[l][c] = cDono;
					} 
					else 
					{
						caixas[i][j] = cDono;
					}
					idxCaixa++;
				}
			}
		}

		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < pontosLinhas; i++) 
		{
			for (int j = 0; j < pontosColunas; j++) 
			{
				int dotNumero = i * pontosColunas + j + 1;
				sb.append("(").append(dotNumero).append(")");
				
				if (j < pontosColunas - 1) 
				{
					if (hLine[i][j]) 
					{
						sb.append("-------");
					} 
					else 
					{
						sb.append("       ");
					}
				}
			}
			sb.append("\n");

			if (i < pontosLinhas - 1) 
			{
				for (int subLinha = 0; subLinha < 3; subLinha++) 
				{
					for (int j = 0; j < pontosColunas; j++) 
					{
						if (vLine[i][j]) 
						{
							sb.append(" | ");
						} 
						else 
						{
							sb.append("   ");
						}
						
						if (j < pontosColunas - 1) 
						{
							if (subLinha == 1) 
							{
								char donoCaixa = caixas[i][j];
								String boxChar = (donoCaixa == '\0' || donoCaixa == ' ') ? " " : String.valueOf(donoCaixa);
								sb.append("   ").append(boxChar).append("   ");
							} 
							else 
							{
								sb.append("       ");
							}
						}
					}
					sb.append("\n");
				}
			}
		}

		sb.append("\nLegenda: Introduza o ponto de início e o ponto de fim para desenhar uma linha.\n");
		sb.append("Linha ocupada = '-' ou '|' ; Caixa fechada = X/O\n");

		return sb.toString();
	}

	/**
	 * Converte o estado do jogo (VO, VX, EM, IV e ND) para uma string 
	 * que representa a mensagem a ser apresentada ao jogador.
	 *
	 * @param  valor contem a abreviatura do estado do jogo.
	 * @return String com a mensagem a ser apresentada ao jogador.
	 */
	public String estadoToTXT(final String valor) 
    {
		switch (valor) 
        {
		case "VO":
			return "Vitória do O!";
		case "VX":
			return "Vitória do X!";
		case "EM":
			return "Empate.";
		case "IV":
			return "Jogada inválida!";
        case "BN":
            return "Caixa fechada! Tem direito a uma jogada bónus.";
		default:
			return ""; //nada a registar
		}
	}
	
    /**
     * Valida se o documento XML recebido representa um método válido.
     *
     * @param doc Documento XML a ser validado.
     * @throws Exception Se o documento não for válido.
     */
    private void validXSD(final Document doc) throws Exception {
        try {
            // Valida o documento contra o XSD "metodos.xsd".
            XMLDoc.validDocXSD(doc, XMLDoc.getContexto() + "metodos-cli.xsd");
        } catch (SAXException | IOException e) {
            throw new Exception("Recebeu mensagem inválida: " + e.getLocalizedMessage());
        }
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
	 * Realiza a autenticação do jogador no servidor.
	 *
	 * @param user Nome do jogador.
	 * @param pass Senha do jogador.
	 * @return Símbolo do jogador (O ou X).
	 * @throws Exception   Se ocorrer um erro durante a autenticação.
	 * @throws IOException Se ocorrer um erro de comunicação com o servidor.
	 */
	public boolean iniciar(final String user, final String pass) throws IOException, Exception {

		// **1. Enviar a mensagem de autenticação para o servidor:**
		// Envia a string de acordo com o formato indicado no XSD.
		os.println("<metodo><iniciar nickname='" + xmlAttribute(user)
					   + "' senha='" + xmlAttribute(pass) + "'/></metodo>");

		// **2. Receber a resposta do servidor e analisar o XML:**
		// Recebe a resposta do servidor.
		String resposta = is.readLine();
		registaLog("Cliente{"+resposta+"}");
		if(resposta==null)
			throw new Exception("Ligação ao servidor cancelada remotamente!");
		// Processa a resposta como um documento XML.
		registo = XMLDoc.parseString(resposta);
		validXSD(registo);
		
		return true;
	}

	public char entrarFila() throws IOException, Exception {
		os.println("<metodo><entrar_fila/></metodo>");
		String resposta = is.readLine();
		registaLog("Cliente{"+resposta+"}");
		if(resposta==null)
			throw new Exception("Ligação ao servidor cancelada remotamente!");
		
		Document d = XMLDoc.parseString(resposta);
		validXSD(d);
		
		NodeList jogadores = d.getElementsByTagName("jogador");
		return ((Element)jogadores.item(0)).getAttribute("simbolo").charAt(0);
	}

	public char desafiar(String alvo) throws IOException, Exception {
		os.println("<metodo><desafiar alvo=\"" + alvo + "\"/></metodo>");
		String resposta = is.readLine();
		registaLog("Cliente{"+resposta+"}");
		if(resposta==null)
			throw new Exception("Ligação ao servidor cancelada remotamente!");
		
		Document d = XMLDoc.parseString(resposta);
		validXSD(d);
		
		NodeList jogadores = d.getElementsByTagName("jogador");
		return ((Element)jogadores.item(0)).getAttribute("simbolo").charAt(0);
	}

	public void cancelarDesafio() throws IOException, Exception {
		os.println("<metodo><cancelar_desafio/></metodo>");
		String resposta = is.readLine();
		registaLog("Cliente{"+resposta+"}");
		if(resposta==null)
			throw new Exception("Ligação ao servidor cancelada remotamente!");
		
		Document d = XMLDoc.parseString(resposta);
		validXSD(d);
	}

	public String verificarConvites() throws IOException, Exception {
		os.println("<metodo><verificar_convites/></metodo>");
		String resposta = is.readLine();
		if(resposta==null) return null;
		
		Document d = XMLDoc.parseString(resposta);
		validXSD(d);
		
		NodeList convites = d.getElementsByTagName("convite");
		if (convites.getLength() > 0) {
			return ((Element)convites.item(0)).getAttribute("de");
		}
		return null;
	}

	public char aceitarDesafio(String de) throws IOException, Exception {
		os.println("<metodo><aceitar_desafio de=\"" + de + "\"/></metodo>");
		String resposta = is.readLine();
		registaLog("Cliente{"+resposta+"}");
		if(resposta==null)
			throw new Exception("Ligação ao servidor cancelada remotamente!");
		
		Document d = XMLDoc.parseString(resposta);
		validXSD(d);
		
		NodeList jogadores = d.getElementsByTagName("jogador");
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
		validXSD(d);
		// Obtém o elemento "tabuleiro" da resposta.
		Element tabuleiro = (Element) d.getElementsByTagName("tabuleiro").item(0);
		// Retorna o elemento "tabuleiro".
		return tabuleiro;
	}

	/**
	 * Realiza uma jogada.
	 *
	 * @param numero Número da jogada (1 a 12).
	 * @throws IOException Se ocorrer um erro de comunicação com o servidor.
	 * @throws Exception   Se ocorrer um erro ao processar a resposta do servidor.
	 */
	public Element jogar(final short numero) throws IOException, Exception 
	{
		// Modificado para aceitar o intervalo correto de linhas (1 a 12)
		if (numero < 1 || numero > 12) 
		{
			throw new Exception("Número da linha do jogo inválido!");
		}
		
		// Envia a mensagem com a jogada para o servidor.
		os.println("<metodo><jogar jogada='" + numero + "'/></metodo>");
		
		Document d = null;
		NodeList n = null;
		
		// Drenar respostas residuais de <obter> que possam ter ficado retidas no buffer TCP
		while (true) {
			String resposta = is.readLine();
			if (resposta != null) {
				registaLog("Cliente{" + resposta + "}");
			}
			if (resposta == null) 
			{
				throw new Exception("Ligação ao servidor cancelada remotamente!");
			}
			
			d = XMLDoc.parseString(resposta);  // consome a linha!
			validXSD(d);
			
			n = d.getElementsByTagName("jogar");
			if (n.getLength() > 0) 
			{
				break; // Resposta correta recebida!
			}
			// Se for um <obter>, ignoramos e tentamos ler a próxima linha!
		}

		return (Element) n.item(0).getFirstChild();
	}

	public void atualizarPerfil(String nick, String fotoBase64) throws Exception {
	    atualizarPerfil(nick, fotoBase64, null);
	}

	public void atualizarPerfil(String nick, String fotoBase64, String cor) throws Exception {
	    atualizarPerfil(nick, null, null, null, null, null, null, fotoBase64, cor);
	}

	public void atualizarPerfil(String nick, String firstNames, String lastNames, String email, String gender,
	        String birthdate, String nationality, String fotoBase64, String cor) throws Exception {
	    StringBuilder pedido = new StringBuilder();
	    pedido.append("<metodo><atualizar_perfil nickname='").append(xmlAttribute(nick))
	          .append("' foto='").append(xmlAttribute(fotoBase64)).append("'");
	    appendAttribute(pedido, "cor", cor);
	    appendAttribute(pedido, "firstnames", firstNames);
	    appendAttribute(pedido, "lastnames", lastNames);
	    appendAttribute(pedido, "email", email);
	    appendAttribute(pedido, "gender", gender);
	    appendAttribute(pedido, "birthdate", birthdate);
	    appendAttribute(pedido, "nationality", nationality);
	    pedido.append("/></metodo>");

	    os.println(pedido.toString());

	    String resposta = is.readLine();
	    registaLog("Cliente{" + resposta + "}");

	    if (resposta == null) {
	        throw new Exception("Ligação ao servidor cancelada remotamente!");
	    }

	    Document d = XMLDoc.parseString(resposta);
	    validXSD(d);

	    NodeList respostas = d.getElementsByTagName("atualizar_perfil");
	    if (respostas.getLength() != 1) {
	        throw new Exception("Resposta inválida do servidor para atualização de perfil.");
	    }
	}

	public boolean registar(String nick, String pass, String foto, String nac, int idade) throws Exception {
	    return registar(nick, pass, "Desconhecido", "Desconhecido", nick + "@mail.pt", "X",
	            java.time.LocalDate.now().minusYears(idade).toString(), foto, nac, "#0F172A");
	}

	public boolean registar(String nick, String pass, String firstNames, String lastNames, String email, String gender,
	        String birthdate, String foto, String nac, String cor) throws Exception {
	    StringBuilder pedido = new StringBuilder();
	    pedido.append("<metodo><registar nickname='").append(xmlAttribute(nick))
	          .append("' senha='").append(xmlAttribute(pass))
	          .append("' foto='").append(xmlAttribute(foto))
	          .append("' nacionalidade='").append(xmlAttribute(nac))
	          .append("' firstnames='").append(xmlAttribute(firstNames))
	          .append("' lastnames='").append(xmlAttribute(lastNames))
	          .append("' email='").append(xmlAttribute(email))
	          .append("' gender='").append(xmlAttribute(gender))
	          .append("' birthdate='").append(xmlAttribute(birthdate))
	          .append("' cor='").append(xmlAttribute(cor))
	          .append("'/></metodo>");
	    os.println(pedido.toString());

	    // 2. Receber a resposta do servidor
	    String resposta = is.readLine();
	    registaLog("Cliente{"+resposta+"}");
	    
	    if(resposta == null)
	        throw new Exception("Ligação ao servidor cancelada remotamente!");

	    // 3. Analisar o XML da resposta
	    registo = XMLDoc.parseString(resposta);
	    validXSD(registo); // Valida contra o metodos-cli.xsd
	    
	    return true;
	}

	private static void appendAttribute(StringBuilder xml, String name, String value) {
	    if (value != null && !value.isBlank()) {
		xml.append(" ").append(name).append("='").append(xmlAttribute(value)).append("'");
	    }
	}

	private static String xmlAttribute(String value) {
	    if (value == null) {
		return "";
	    }
	    return value.replace("&", "&amp;")
	            .replace("'", "&apos;")
	            .replace("\"", "&quot;")
	            .replace("<", "&lt;")
	            .replace(">", "&gt;");
	}
	
}
