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
    private static Document getNext(BufferedReader is) throws Exception {
	// Lê a linha que contém a mensagem.
    	String line=is.readLine();
    	registaLog("Servidor{"+line+"}");
        Document d = XMLDoc.parseString(line); 
        // Valida o schema XSD da mensagem.
        validXSD(d);
        return d;
    }
    
    public static boolean runEntrada(Socket sk, char simbolo) throws Exception {
        BufferedReader is = new BufferedReader(new InputStreamReader(sk.getInputStream()));
        PrintWriter os = new PrintWriter(sk.getOutputStream(), true);
        
        // Lê e valida a mensagem (pode ser <iniciar> ou <registar>)
        Document x = getNext(is); 
        
        // Verifica se a mensagem tem a tag <iniciar>
        if (x.getElementsByTagName("iniciar").getLength() > 0) {
            System.out.println("   -> Recebido pedido de LOGIN...");
            // Copia para aqui toda a lógica que estava dentro do antigo runIniciar
            String Nome  = getMethod(x,"iniciar").getAttribute("nickname");
            String Senha = getMethod(x,"iniciar").getAttribute("senha");
            
            User jg = User._authenticate(Nome, Senha);
            if(jg == null) throw new Exception("Falhou a autenticação do utilizador '"+Nome+"'!");
            
            Document d=XMLDoc.parseString(jg.toXMLString(simbolo));
            Node jogador = d.getElementsByTagName("jogador").item(0);
            Node cloneElement = x.importNode(jogador, true);
            x.getElementsByTagName("iniciar").item(0).appendChild(cloneElement);
            
            registarSocketUtilizador(sk, Nome);
            os.println(XMLDoc.documentToString(x));
            return true;
        } 
        // Verifica se a mensagem tem a tag <registar>
        else if (x.getElementsByTagName("registar").getLength() > 0) {
            System.out.println("   -> Recebido pedido de REGISTO...");
            // Copia para aqui toda a lógica que fizemos há bocado para o Registo
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
            
            System.out.println("   ✅ Novo Jogador Registado: " + nick + " com o símbolo '" + simbolo + "'");

            // 5. Preparar a Resposta (O truque do <iniciar>)
            // Criamos o nó do jogador com os dados todos (foto, idade, etc.)
            Document d = XMLDoc.parseString(jg.toXMLString(simbolo)); 
            Node jogadorNode = d.getElementsByTagName("jogador").item(0);
            
            // Criamos uma nova resposta limpa com a tag <iniciar> para o metodos-cli.xsd deixar passar!
            Document respostaDoc = XMLDoc.parseString("<metodo><iniciar nickname='" + xmlAttribute(nick)
                    + "' senha='" + xmlAttribute(senha) + "'/></metodo>");
            
            // Importamos e anexamos o nó do jogador dentro do <iniciar>
            Node cloneElement = respostaDoc.importNode(jogadorNode, true);
            respostaDoc.getElementsByTagName("iniciar").item(0).appendChild(cloneElement);
            
            // 6. Enviar o XML final de volta para o Stub
            String msgResposta = XMLDoc.documentToString(respostaDoc);
            registarSocketUtilizador(sk, nick);
            os.println(msgResposta);
            return true;
        } 
        // Pedido de atualização de perfil (não entra na fila de jogo)
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

            // Responde com a própria estrutura válida no metodos-cli.xsd
            os.println(XMLDoc.documentToString(x));
            return false;
        } 
        else {
            throw new Exception("Operação de entrada desconhecida!");
        }
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
        Document x = getNext(is);
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
        os.println(msg);
    }
    
    public static void runRegistar(Socket sk, char simbolo) throws Exception {
        // 1. Configurar os streams de comunicação
        BufferedReader is = new BufferedReader(new InputStreamReader(sk.getInputStream()));
        PrintWriter os = new PrintWriter(sk.getOutputStream(), true);
        
        // 2. Ler a mensagem XML enviada pelo Stub (<metodo><registar .../></metodo>)
        // O getNext já faz a validação contra o metodos-srv.xsd!
        Document x = getNext(is); 
        
        // 3. Extrair o elemento "registar" e os seus atributos
        // Usamos o método auxiliar getMethod que já existe no teu Skeleton
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

        // 4. Lógica de Negócio: Criar o utilizador no users.xml
        // O método 'register' que fizemos no User.java trata de tudo e lança 
        // exceção se o nick já existir.
        User jg = User.register(nick, senha, firstNames, lastNames, email, gender, birthdate, foto, nac, cor);
        
        System.out.println("   Novo Jogador Registado: " + nick + " com o símbolo '" + simbolo + "'");

        // 5. Preparar a Resposta (Seguindo o padrão do runIniciar)
        // Isto converte o objeto User para o XML <jogador simbolo='X' .../>
        Document d = XMLDoc.parseString(jg.toXMLString(simbolo)); 
        Node jogadorNode = d.getElementsByTagName("jogador").item(0);
        
        // Importamos o nó do jogador para o documento original da mensagem
        Node cloneElement = x.importNode(jogadorNode, true);
        
        // Anexamos o jogador dentro da tag <registar> para confirmar o sucesso
        reg.appendChild(cloneElement);
        
        // 6. Enviar o XML final de volta para o Stub
        String msgResposta = XMLDoc.documentToString(x);
        os.println(msgResposta);
    }
    
    
    public static void runAtualizarPerfil(Socket sk) throws Exception {
        BufferedReader is = new BufferedReader(new InputStreamReader(sk.getInputStream()));
        PrintWriter os = new PrintWriter(sk.getOutputStream(), true);
        
        // 1. Receber e validar a mensagem XML
        Document x = getNext(is); 
        Element req = (Element) x.getElementsByTagName("atualizar_perfil").item(0);
        
        // 2. Extrair os dados
        String nick = req.getAttribute("nickname");
        String novaFoto = req.getAttribute("foto");
        String cor = optionalAttribute(req, "cor");
        String firstNames = optionalAttribute(req, "firstnames");
        String lastNames = optionalAttribute(req, "lastnames");
        String email = optionalAttribute(req, "email");
        String gender = optionalAttribute(req, "gender");
        String birthdate = optionalAttribute(req, "birthdate");
        String nationality = optionalAttribute(req, "nationality");

        // 3. Usar o método nativo do professor para atualizar o ficheiro!
        // Assumindo que o método retorna um boolean de sucesso
        boolean sucesso = User._updatePerfil(nick, firstNames, lastNames, email, gender, birthdate, nationality,
                novaFoto, cor);
        if (sucesso) {
            User._save();
            User._load();
            // Enviar uma mensagem de sucesso de volta ao cliente
            os.println("<metodo><resposta estado='OK'>Perfil atualizado com sucesso!</resposta></metodo>");
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
        Document obter = getNext(is);
        
        // Verifica a existencia do elemento "obter" na mensagem.
        getMethod(obter, "obter");

        // Envia a mensagem "obter" para o jogador, 
        // com o tabuleiro atualizado indicando o estado atual do jogo.
        os.println("<metodo><obter>" + jogo.tabuleiroToXML() + "</obter></metodo>");
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
        Document jogar = getNext(is);

        // Obtém o elemento "jogar" da mensagem.
        Element jogada = getMethod(jogar, "jogar");

        // Extrai a jogada do jogador.
        String jogadaStr = jogada.getAttribute("jogada");
        short jogadaNum = Short.parseShort(jogadaStr);

        // Concretiza a jogada.
        jogo.joga(jogadaNum, simbolo);

        // Envia a mesma mensagem recebida como resposta "jogar" para o jogador.
        os.println(XMLDoc.documentToString(jogar));   	
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
