package server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import user.User;
import util.XMLDoc;

/**
 * Classe que implementa a adaptação do servidor ao protocolo. 
 * Suporta a interação do cliente com o servidor, 
 * Converte mensagens em XML para acções/objetos e vice-versa.
 * 
 * @author Engº Porfírio Filipe
 */
public class Skeleton {
    private static final Map<Socket, String> socketToUsername = new ConcurrentHashMap<>();

    public static void registarSocketUtilizador(Socket sk, String username) {
        if (sk != null && username != null && !username.isBlank()) {
            socketToUsername.put(sk, username);
        }
    }

    public static String obterSocketUtilizador(Socket sk) {
        return socketToUsername.get(sk);
    }

    public static void limparSocketUtilizador(Socket sk) {
        if (sk != null) {
            socketToUsername.remove(sk);
        }
    }
    // **Métodos:**

    /**
     * Valida se o documento XML recebido representa um método válido.
     *
     * @param doc Documento XML a ser validado.
     * @throws Exception Se o documento não for válido.
     */
    private static void validXSD(final Document doc) throws Exception {
        try {
            // Valida o documento contra o XSD "metodos.xsd".
            XMLDoc.validDocXSD(doc, XMLDoc.getContexto() + "metodos-srv.xsd");
        } catch (SAXException | IOException e) {
            throw new Exception("Recebeu mensagem inválida: " + e.getLocalizedMessage());
        }
    }
    
    // Lê a próxima linha/mensagem e devolve num Document
    public static Document getNext(BufferedReader is, Socket sk) throws Exception {
        String line = is.readLine();
        if (line == null) return null;
        
        registaLog("Servidor{"+line+"}");
        Document d = XMLDoc.parseString(line); 

        validXSD(d);
        return d;
    }
    
    // --- AUDITORIA ---
    public static void printAndLog(PrintWriter os, Socket sk, String msg) {
        try {
            registaLog("Servidor{" + msg + "}");
        } catch (Exception e) {}
        os.println(msg);
    }
    
    public static int runLobby(Socket sk) throws Exception {
        BufferedReader is = new BufferedReader(new InputStreamReader(sk.getInputStream()));
        PrintWriter os = new PrintWriter(sk.getOutputStream(), true);
        
        Document x = getNext(is, sk); 
        
        if (x.getElementsByTagName("iniciar").getLength() > 0) {
            System.out.println("   -> Recebido pedido de LOGIN...");
            String Nome  = getMethod(x,"iniciar").getAttribute("nickname");
            String Senha = getMethod(x,"iniciar").getAttribute("senha");
            
            User jg = User._authenticate(Nome, Senha);
            if(jg == null) throw new Exception("Falhou a autenticação do utilizador '"+Nome+"'!");
            
            // Responder com <iniciar> SEM o símbolo, pois ainda não está em jogo
            Document respostaDoc = XMLDoc.parseString("<metodo><iniciar nickname='" + xmlAttribute(Nome)
                    + "' senha='" + xmlAttribute(Senha) + "'/></metodo>");
            
            registarSocketUtilizador(sk, Nome);
            printAndLog(os, sk, XMLDoc.documentToString(respostaDoc));
            return 0; // continue no lobby
        } 
        else if (x.getElementsByTagName("registar").getLength() > 0) {
            System.out.println("   -> Recebido pedido de REGISTO...");
            Element reg = getMethod(x, "registar");
            String nick  = reg.getAttribute("nickname");
            String senha = reg.getAttribute("senha");
            String foto  = reg.getAttribute("foto");
            String nac   = reg.getAttribute("nacionalidade");
            String firstNames = reg.getAttribute("firstnames");
            String lastNames = reg.getAttribute("lastnames");
            String email = reg.getAttribute("email");
            String gender = reg.getAttribute("gender");
            String birthdate = reg.getAttribute("birthdate");
            String cor = reg.getAttribute("cor");

            User jg = User.register(nick, senha, firstNames, lastNames, email, gender, birthdate, foto, nac, cor);
            System.out.println("   ✅ Novo Jogador Registado: " + nick);

            Document respostaDoc = XMLDoc.parseString("<metodo><iniciar nickname='" + xmlAttribute(nick)
                    + "' senha='" + xmlAttribute(senha) + "'/></metodo>");
            
            registarSocketUtilizador(sk, nick);
            printAndLog(os, sk, XMLDoc.documentToString(respostaDoc));
            return 0; // continue no lobby
        } 
        else if (x.getElementsByTagName("atualizar_perfil").getLength() > 0) {
            System.out.println("   -> Recebido pedido de ATUALIZAÇÃO DE PERFIL...");
            Element req = getMethod(x, "atualizar_perfil");
            String nick = req.getAttribute("nickname");
            String novaFoto = req.getAttribute("foto");
            String cor = optionalAttribute(req, "cor");
            String firstNames = optionalAttribute(req, "firstnames");
            String lastNames = optionalAttribute(req, "lastnames");
            String email = optionalAttribute(req, "email");
            String gender = optionalAttribute(req, "gender");
            String birthdate = optionalAttribute(req, "birthdate");
            String nationality = optionalAttribute(req, "nationality");

            boolean sucesso = User._updatePerfil(nick, firstNames, lastNames, email, gender, birthdate,
                    nationality, novaFoto, cor);
            if (!sucesso) {
                throw new Exception("Não foi possível atualizar o perfil do utilizador '" + nick + "'.");
            }
            User._save();
            User._load();

            registarSocketUtilizador(sk, nick);
            printAndLog(os, sk, XMLDoc.documentToString(x));
            return 9; // action: disconnect silently
        } 
        else if (x.getElementsByTagName("entrar_fila").getLength() > 0) {
            System.out.println("   -> Recebido pedido de ENTRAR NA FILA...");
            return 1; // action: entrar na fila publica
        }
        else if (x.getElementsByTagName("desafiar").getLength() > 0) {
            System.out.println("   -> Recebido pedido de DESAFIAR PRIVADO...");
            String alvo = getMethod(x, "desafiar").getAttribute("alvo");
            String eu = obterSocketUtilizador(sk);
            Servidor.convitesPendentes.put(alvo, eu);
            return 2; // action: desafiar
        }
        else if (x.getElementsByTagName("verificar_convites").getLength() > 0) {
            String eu = obterSocketUtilizador(sk);
            boolean isInvited = Servidor.convitesPendentes.containsKey(eu);
            if (isInvited) {
                String inviter = Servidor.convitesPendentes.get(eu);
                printAndLog(os, sk, "<metodo><verificar_convites><convite de='" + xmlAttribute(inviter) + "'/></verificar_convites></metodo>");
            } else {
                printAndLog(os, sk, "<metodo><verificar_convites/></metodo>");
            }
            return 0; // continue in lobby
        }
        else if (x.getElementsByTagName("aceitar_desafio").getLength() > 0) {
            System.out.println("   -> Recebido pedido de ACEITAR DESAFIO...");
            return 1;
        }
        else {
            throw new Exception("Operação de lobby desconhecida!");
        }
    }
    
    public static void sendEntrarFilaResponse(Socket sk, char simbolo) throws Exception {
        PrintWriter os = new PrintWriter(sk.getOutputStream(), true);
        String username = obterSocketUtilizador(sk);
        User jg = User._obtain(username);
        
        Document d = XMLDoc.parseString(jg.toXMLString(simbolo));
        Node jogadorNode = d.getElementsByTagName("jogador").item(0);
        
        Document respostaDoc = XMLDoc.parseString("<metodo><entrar_fila/></metodo>");
        Node cloneElement = respostaDoc.importNode(jogadorNode, true);
        respostaDoc.getElementsByTagName("entrar_fila").item(0).appendChild(cloneElement);
        
        printAndLog(os, sk, XMLDoc.documentToString(respostaDoc));
    }
	/**
	 * Método que atende a chamada Iniciar.
	 * @param sk 		Circuto virtual estabelecido com o jogador
	 * @param simbolo 	Simbolo do jogador
	 * @throws Exception 	Em caso de erro
	 */
    public static void runIniciar(Socket sk, char simbolo) throws Exception {
	// estes streamms não podem ser fechados porque fecham o socket
	// Stream para ler dados do socket.
	BufferedReader is = new BufferedReader(new InputStreamReader(sk.getInputStream()));
	// Stream para escrever dados no socket.
	PrintWriter os = new PrintWriter(sk.getOutputStream(), true);
        System.out.println("   Jogador '"+simbolo+"': "+sk);
        Document x = getNext(is, sk);
        // Trata o jogador
        // Extrai o nome e senha do jogador X.
        String Nome 	= 	getMethod(x,"iniciar").getAttribute("nickname");
        String Senha 	= 	getMethod(x,"iniciar").getAttribute("senha");
        System.out.println("   Jogador '"+simbolo+"': " + Nome + "/" + Senha);
        User jg = User._authenticate(Nome, Senha);
        if(jg == null)
            throw new Exception("Falhou a autenticação do utilizador '"+Nome+"'!");
        System.out.println("Autenticação/login do utilizador '"+Nome+"' realizado com sucesso!");
        Document d=XMLDoc.parseString(jg.toXMLString(simbolo));
        Node jogador = d.getElementsByTagName("jogador").item(0);
        Node cloneElement = x.importNode(jogador, true);
        x.getElementsByTagName("iniciar").item(0).appendChild(cloneElement);
        // Envia a mensagem de "iniciar" para o jogador, 
        // com o seu símbolo que confirma o login bem sucedido.
        String msg=XMLDoc.documentToString(x);
        printAndLog(os, sk, msg);
    }
    
    public static void runRegistar(Socket sk, char simbolo) throws Exception {
        BufferedReader is = new BufferedReader(new InputStreamReader(sk.getInputStream()));
        PrintWriter os = new PrintWriter(sk.getOutputStream(), true);
        
        // O getNext já faz a validação contra o metodos-srv.xsd
        Document x = getNext(is, sk); 
        
        // Extrair o elemento "registar" e os seus atributos
        Element reg = getMethod(x, "registar");
        
        String nick  = reg.getAttribute("nickname");
        String senha = reg.getAttribute("senha");
        String foto  = reg.getAttribute("foto");
        String nac   = reg.getAttribute("nacionalidade");
        String firstNames = reg.getAttribute("firstnames");
        String lastNames = reg.getAttribute("lastnames");
        String email = reg.getAttribute("email");
        String gender = reg.getAttribute("gender");
        String birthdate = reg.getAttribute("birthdate");
        String cor = reg.getAttribute("cor");

        // O método 'register' que fizemos no User.java trata de tudo e lança 
        // exceção se o nick já existir.
        User jg = User.register(nick, senha, firstNames, lastNames, email, gender, birthdate, foto, nac, cor);
        
        System.out.println("   Novo Jogador Registado: " + nick + " com o símbolo '" + simbolo + "'");

        // Isto converte o objeto User para o XML <jogador simbolo='X' .../>
        Document d = XMLDoc.parseString(jg.toXMLString(simbolo)); 
        Node jogadorNode = d.getElementsByTagName("jogador").item(0);
        
        // Importamos o nó do jogador para o documento original da mensagem
        Node cloneElement = x.importNode(jogadorNode, true);
        
        // Anexamos o jogador dentro da tag <registar> para confirmar o sucesso
        reg.appendChild(cloneElement);
        
        // Enviar o XML final de volta para o Stub
        String msgResposta = XMLDoc.documentToString(x);
        printAndLog(os, sk, msgResposta);
    }
    
    
    public static void runAtualizarPerfil(Socket sk) throws Exception {
        BufferedReader is = new BufferedReader(new InputStreamReader(sk.getInputStream()));
        PrintWriter os = new PrintWriter(sk.getOutputStream(), true);
        
        // Receber e validar a mensagem XML
        Document x = getNext(is, sk); 
        Element req = (Element) x.getElementsByTagName("atualizar_perfil").item(0);
        
        // Extrair os dados
        String nick = req.getAttribute("nickname");
        String novaFoto = req.getAttribute("foto");
        String cor = optionalAttribute(req, "cor");
        String firstNames = optionalAttribute(req, "firstnames");
        String lastNames = optionalAttribute(req, "lastnames");
        String email = optionalAttribute(req, "email");
        String gender = optionalAttribute(req, "gender");
        String birthdate = optionalAttribute(req, "birthdate");
        String nationality = optionalAttribute(req, "nationality");

        // Usar o método nativo do professor para atualizar o ficheiro
        // Assumindo que o método retorna um boolean de sucesso
        boolean sucesso = User._updatePerfil(nick, firstNames, lastNames, email, gender, birthdate, nationality,
                novaFoto, cor);
        if (sucesso) {
            User._save();
            User._load();
            // Enviar uma mensagem de sucesso de volta ao cliente
            printAndLog(os, sk, "<metodo><resposta estado='OK'>Perfil atualizado com sucesso!</resposta></metodo>");
        } else {
            // Se retornar false, provavelmente o utilizador não existe
            throw new Exception("Não foi possível atualizar a fotografia do utilizador '" + nick + "'.");
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

    private static String optionalAttribute(Element element, String name) {
        String value = element.getAttribute(name);
        return value.isBlank() ? null : value;
    }
    
	/**
	 * Método que atende a chamada Obter.
	 * @param is 			entrada
	 * @param os 			saida
	 * @param simbolo 		simbolo do jogador
	 * @param sk 			dados da ligação para efeitos de registo
	 * @param jogo 			representa o jogo atual
	 * @throws Exception 	em caso de erro
	 */
    public static void runObter(BufferedReader is, PrintWriter os, char simbolo, Socket sk, JogoXML jogo) throws Exception {
        Document obter = getNext(is, sk);
        
        // Verifica a existencia do elemento "obter" na mensagem.
        getMethod(obter, "obter");

        // Envia a mensagem "obter" para o jogador, 
        // com o tabuleiro atualizado indicando o estado atual do jogo.
        printAndLog(os, sk, "<metodo><obter>" + jogo.tabuleiroToXML() + "</obter></metodo>");
    }
    
    /**
     * Envia o estado atual do jogo (usualmente uma derrota por timeout) sem esperar pelo pedido "obter"
     */
    public static void runNotificarTimeout(PrintWriter os, Socket sk, JogoXML jogo) throws Exception {
        printAndLog(os, sk, "<metodo><obter>" + jogo.tabuleiroToXML() + "</obter></metodo>");
    }
	/**
	 * Método que atende a chamada Jogar.
	 * @param is 			entrada
	 * @param os 			saida
	 * @param simbolo 		simbolo do jogador
	 * @param sk 			dados da ligação para efeitos de registo
	 * @param jogo 			representa o jogo atual
	 * @return 				o jogo atualizado
	 * @throws Exception 	em caso de erro
	 */
    public static JogoXML runJogar(BufferedReader is, PrintWriter os, char simbolo, Socket sk, JogoXML jogo) throws Exception {
        Document jogar = getNext(is, sk);

        // Obtém o elemento "jogar" da mensagem.
        Element jogada = getMethod(jogar, "jogar");

        // Extrai a jogada do jogador.
        String jogadaStr = jogada.getAttribute("jogada");
        short jogadaNum = Short.parseShort(jogadaStr);

        // Concretiza a jogada.
        jogo.joga(jogadaNum, simbolo);

        // Envia a mesma mensagem recebida como resposta "jogar" para o jogador.
        printAndLog(os, sk, XMLDoc.documentToString(jogar));   	
    	return jogo;
    }
    /**
     * Obtém o elemento do documento XML que representa o método especificado.
     *
     * @param doc 			Documento XML.
     * @param Method 		Nome do método a ser obtido.
     * @throws Exception 	Se o método não for encontrado no documento.
     * @return 				Elemento XML que representa o método.
     */
    private static Element getMethod(final Document doc, final String Method) throws Exception {
        NodeList items = doc.getElementsByTagName(Method);
        if (items.getLength() != 1) {
            throw new Exception("Erro de lógica: espera método '" + Method + "'!");
        }
        return ((Element) doc.getElementsByTagName(Method).item(0));
    }
    
    /**
     * @param ficheiro de destino
     * @param texto	a adicionar o ficheiro
     * @throws IOException se houver erro
     */
    private static void adicionarStringFicheiro(String ficheiro, String texto) throws IOException {
        try (
            // Cria um PrintWriter para escrever no ficheiro.
            PrintWriter escritor = new PrintWriter(new BufferedWriter(new FileWriter(ficheiro, true)))) {

            // Escreve a string no ficheiro.
            escritor.println(texto);
        }
    }
    
    /**
     * @param evento 		mensagem a resgistar
     * @throws IOException	em caso de erro
     */
    private static void registaLog(String evento) throws IOException {
    	adicionarStringFicheiro(XMLDoc.getContexto()+"protocolo.log", LocalDateTime.now()+" - "+evento.replaceAll("\n",""));
    }
}
