package user;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import util.MyImage;
import util.XMLDoc;

/**
 * Classe para gerir um utilizador
 */
public class User {
    // Nome de ficheiro associado aos utilizadores
    private static String file = "users";

    private static final String COR_PADRAO = "#0F172A";

    // Carrega o ficheiro com os utilizadores
    private static Document doc = null;

    // Identificador universal único do utilizador
    private UUID userId = null;

    // Data da última atualização
    private LocalDateTime updated = LocalDateTime.now();

    // Indica se o utilizador está bloqueado
    private boolean blocked = false;

    // Número que define o perfil do utilizador
    private int profile = 1;

    // Nome do utilizador (nome curto, alcunha ou nickname)
    private String username = null;

    // Senha codificada em MD5
    private String password = null;

    // Primeiros nomes do utilizador
    private String firstNames = null;

    // Últimos nomes do utilizador
    private String lastNames = null;

    // Endereço de correio eletrónico
    private String email = null;

    // Género do utilizador (M, F, X)
    private String gender = null;

    // Data de nascimento do utilizador
    private LocalDate birthdate = null;

    // Fotografia do utilizador (tipo passaporte)
    private MyImage photography = null;

    // Cor de fundo preferida
    private String corFundo = COR_PADRAO;

    // Nacionalidade do utilizador (ISO 3166-1 alpha-2)
    private Nationality nationality = null;

    static Scanner sc = new Scanner(System.in);

    static {
	// Este bloco é executado quando a classe é carregada
	_load();
    }

    /**
     * Carrega e valida o documento util que tem os dados dos utilizadores
     */
    public static void _load() {
	String docXML = XMLDoc.getContexto() + file + ".xml";
	String xsdPath = XMLDoc.getContexto() + file + ".xsd";
	Document d = null;
	boolean carregado = false;

	try {
	    d = XMLDoc.parseFile(docXML);
	    if (d != null) {
		XMLDoc.validDocXSD(d, xsdPath);
		carregado = true;
	    }
	} catch (Exception e) {
	    System.err.println("Erro ao carregar ou validar " + file + ".xml: " + e.getMessage());
	}

	if (!carregado) {
	    System.err.println("Tentando recuperar a partir do último backup...");
	    int maxVersao = XMLDoc.obterNumeroVersao(docXML);
	    if (maxVersao > 0) {
		String backupPath = XMLDoc.removerExtensao(docXML) + "(" + maxVersao + ").util";
		java.io.File backupFile = new java.io.File(backupPath);
		if (backupFile.exists()) {
		    try {
			java.nio.file.Files.copy(backupFile.toPath(), new java.io.File(docXML).toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			System.out.println("Restaurado com sucesso a partir de " + backupFile.getName());
			
			d = XMLDoc.parseFile(docXML);
			if (d != null) {
			    XMLDoc.validDocXSD(d, xsdPath);
			    carregado = true;
			}
		    } catch (Exception ex) {
			System.err.println("Erro crítico ao recuperar backup: " + ex.getMessage());
		    }
		}
	    }
	}

	doc = d; // atualiza o documento na classe
    }

    /**
     * Construtor por omissão
     */
    public User() {
	this.userId = UUID.randomUUID();
	this.updated = LocalDateTime.now();
	this.blocked = true;
	this.profile = 1;
	this.corFundo = COR_PADRAO;
    }

    /**
     * Construtor da classe User, os atributos opcionais são tratados via setters
     * 
     * @param username   nome do utilizador
     * @param password   senha para autenticação
     * @param firstNames primeiros nomes do utilizador
     * @param lastNames  últimos nomes do utilizador
     * @throws NoSuchAlgorithmException em caso de erro
     */
    public User(String username, String password, String firstNames, String lastNames) throws NoSuchAlgorithmException {
	this.userId = UUID.randomUUID();
	this.updated = LocalDateTime.now();
	this.blocked = true;	// quando é criado fica bloqueado
	this.profile = 1;
	this.corFundo = COR_PADRAO;
	setUsername(username);
	setPassword(password);
	setFirstNames(firstNames);
	setLastNames(lastNames);
	/*
	 * usar os respetivos setters para os atributos opcionais:
	 * 	 email, gender, birthdate, nationality, photography
	 */
    }

    // Getters e setters

    /**
     * @return UUID
     */
    public UUID getUserId() {
	return userId;
    }

    /**
     * @param userId UUID
     * @return sucesso
     */
    public boolean setUserId(UUID userId) {
	this.userId = userId;
	return true;
    }

    /**
     * @return Data da última atualização
     */
    public LocalDateTime getUpdated() {
	return updated;
    }

    /**
     * @param updated data da última atualização
     * @return sucesso
     */
    public boolean setUpdated(LocalDateTime updated) {
	this.updated = updated;
	return true;
    }

    /**
     * @return se for true significa que está bloqueado
     */
    public boolean isBlocked() {
	return blocked;
    }

    /**
     * @param blocked indica se está bloqueado
     * @return sucesso
     */
    public boolean setBlocked(boolean blocked) {
	this.blocked = blocked;
	return true;
    }

    /**
     * @return o indicador de perfil
     */
    public int getProfile() {
	return profile;
    }

    /**
     * @param profile indicador de perfil
     * @return sucesso
     */
    public boolean setProfile(int profile) {
	if (profile >= 0 && profile <= 10) {
	    this.profile = profile;
	    return true;
	}
	return false;
    }

    /**
     * @return nome curto do utilizador
     */
    public String getUsername() {
	return username;
    }

    /**
     * @param username valida o nome curto de um utilizador
     * @return sucesso
     */
    public static boolean validarUserName(String username) {

	// Tamanho mínimo e máximo do username
	final int TAMANHO_MINIMO = 4;
	final int TAMANHO_MAXIMO = 10;

	// Regex para validar caracteres permitidos no username
	// A expressão regular valida sequências de carateres, com tamanho entre 4 e 10,
	// que sejam apenas letras minúsculas, maiúsculas, números, underline e hífen,
	// desde que não estejam vazias (deve ter pelo menos um caractere) e
	// não contenham nenhum outro tipo de caractere especial
	final String REGEX_CARACTERES_VALIDOS = "^[a-zA-Z0-9_-]{" + TAMANHO_MINIMO + "," + TAMANHO_MAXIMO + "}$";

	// Verifica se o username está vazio
	if (username.isEmpty()) {
	    return false;
	}

	// Verifica se o username contém apenas caracteres válidos
	if (!username.matches(REGEX_CARACTERES_VALIDOS)) {
	    return false;
	}

	// Username é válido
	return true;
    }

    /**
     * @param username nome do utilizador
     * @return sucesso
     */
    public boolean setUsername(String username) {
	if (!validarUserName(username)) {
	    return false;
	}
	this.username = username;
	return true;
    }

    /**
     * Este método não deve ser usado em produção, deve até ser removido!
     * 
     * @return password hash da password com SHA-256
     */
    public String getPassword() {
	return password;
    }

    /**
     * Aceita, obtem hash com SHA-256 e atualiza a password
     * 
     * @param password em claro
     * @return sucesso
     * @throws NoSuchAlgorithmException em caso de erro
     */
    public boolean setPassword(String password) throws NoSuchAlgorithmException {
	this.password = XMLDoc.SHA256(password);
	return true;
    }

    /**
     * @return primeiros nomes do utilizador
     */
    public String getFirstNames() {
	return firstNames;
    }

    /**
     * @param firstNames primeiros nomes do utilizador
     * @return sucesso
     */
    public boolean setFirstNames(String firstNames) {
	this.firstNames = firstNames;
	return true;
    }

    /**
     * @return últimos nomes do utilizador
     */
    public String getLastNames() {
	return lastNames;
    }

    /**
     * @param lastNames últimos nomes do utilizador
     * @return sucesso
     */
    public boolean setLastNames(String lastNames) {
	this.lastNames = lastNames;
	return true;
    }

    /**
     * @return nome completo
     */
    public String getName() {
	return capitalizar(firstNames + " " + lastNames);
    }

    /**
     * @return endereço de correio eletrónico
     */
    public String getEmail() {
	return email;
    }

    /**
     * @param email endereço de correio eletrónico
     * @return sucesso
     */
    public boolean setEmail(String email) {
	if (email != null) {
	    String regex = "^(.+)@([\\w\\-]+\\.)+([\\w\\-]+)$";
	    Pattern pattern = Pattern.compile(regex);
	    Matcher matcher = pattern.matcher(email);
	    if (!matcher.matches())
		return false;
	}
	this.email = email;
	return true;
    }

    /**
     * @return género do utilizador
     */
    public String getGender() {
	return gender;
    }

    /**
     * @param gender género do utilizador
     * @return sucesso
     */
    public boolean setGender(String gender) {
	if (gender != null)
	    if (!gender.equals("M") && !gender.equals("F") && !gender.equals("X"))
		return false;
	this.gender = gender;
	return true;
    }

    /**
     * @return data de nascimento
     */
    public LocalDate getBirthdate() {
	return birthdate;
    }

    /**
     * @param birthdate data de nascimento
     * @return sucesso
     */
    public boolean setBirthdate(LocalDate birthdate) {
	if (birthdate != null)
	    if (birthdate.isAfter(LocalDate.now()))
		return false;
	this.birthdate = birthdate;
	return true;
    }

    /**
     * @return a idade calculada a partir da data de nascimento
     */
    public int getAge() {
	if (birthdate == null)
	    return -1;
	// Calcula a idade a partir da data de nascimento
	return Period.between(birthdate, LocalDate.now()).getYears();
    }

    /**
     * @return objeto que contém a fotografia
     */
    public MyImage getPhotography() {
	return photography;
    }

    /**
     * @param photo em base64
     * @return false se houver erro
     */
    public boolean setPhotography(String photo) {
	if (photo != null) {
	    MyImage i = new MyImage();
	    i.setBase64(photo.replaceAll("[\\s]", "")); // linhas e brancos a mais
	    return setPhotography(i);
	}
	this.photography = null;
	return true;
    }

    /**
     * @param photo objeto que contém a fotografia
     * @return sucesso
     */
    public boolean setPhotography(MyImage photo) {
	this.photography = photo;
	return true;
    }

    /**
     * @return cor de fundo preferida
     */
    public String getCorFundo() {
	return corFundo;
    }

    /**
     * @param corFundo cor de fundo preferida no formato hexadecimal
     * @return sucesso
     */
    public boolean setCorFundo(String corFundo) {
	if (corFundo == null || corFundo.isBlank()) {
	    this.corFundo = COR_PADRAO;
	    return true;
	}

	String valor = corFundo.trim().toUpperCase(Locale.ROOT);
	if (!valor.matches("^#[0-9A-F]{6}$")) {
	    return false;
	}

	this.corFundo = valor;
	return true;
    }
    
    

    /**
     * @return objeto que representa a nacionalidade
     */
    public Nationality getNationality() {
	return nationality;
    }

    /**
     * @return designação da nacionalidade em português em conformidade com o género
     * @throws XPathExpressionException em caso de erro
     * @throws Exception em caso de erro
     */
    public String getPtNationality() throws XPathExpressionException, Exception {
	return nationality.pt(gender);
    }

    /**
     * @param nationality objeto que representa a nacionalidade
     * @return sucesso
     */
    public boolean setNationality(Nationality nationality) {
	this.nationality = nationality;
	return true;
    }

    /**
     * Afeta a nacionalidade com o objeto que a representa determinado a partir do código
     * @param abbreviation	código ISO da nacionalidade
     * @return sucesso
     * @throws XPathExpressionException em caso de erro
     */
    public boolean setNationality(String abbreviation) throws XPathExpressionException {
	this.nationality = Nationality.getByAbbreviation(abbreviation);
	return true;
    }

    /**
     * Método para ler os dados do utilizador
     * @return o utilizador lido
     */
    public static User read() {
	User user = new User();
	do {
	    System.out.print("Nome do utilizador: ");
	    String username = sc.nextLine();
	    if (user.setUsername(username))
		break;
	    System.out.print("Nome de utilizador inválido! ");
	} while (true);

	do {
	    System.out.print("Senha para autenticação: ");
	    String password = sc.nextLine();
	    try {
		if (user.setPassword(password))
		    break;
	    } catch (NoSuchAlgorithmException e) {
		e.printStackTrace();
	    }
	    System.out.print("Senha inválida! ");
	} while (true);

	do {
	    System.out.print("Primeiros nomes do utilizador: ");
	    String firstNames = sc.nextLine();
	    if (user.setFirstNames(firstNames))
		break;
	    System.out.print("Nomes inválidos! ");
	} while (true);

	do {
	    System.out.print("Últimos nomes do utilizador: ");
	    String lastNames = sc.nextLine();
	    if (user.setLastNames(lastNames))
		break;
	    System.out.print("Nomes inválidos! ");
	} while (true);

	do {
	    System.out.print("Endereço de correio eletrónico: ");
	    String email = sc.nextLine();
	    if (user.setEmail(email))
		break;
	    System.out.print("Endereço de correio eletrónico inválido! ");
	} while (true);

	do {
	    System.out.print("Género (M, F, X): ");
	    String gender = sc.nextLine().toUpperCase();
	    if (gender == null || user.setGender(gender))
		break;
	} while (true);

	do {
	    try {
		System.out.print("Data de nascimento (YYYY-MM-DD): ");
		String birthdateStr = sc.nextLine();
		if (birthdateStr == null)
		    break;
		if (user.setBirthdate(LocalDate.parse(birthdateStr)))
		    break;
	    } catch (Exception e) {

	    } finally {
		System.err.println("Data de nascimento inválida. Tente novamente (YYYY-MM-DD): ");
	    }
	} while (true);

	do {
	    System.out.print("Indique o caminho para o ficheiro que contém a fotografia tipo pass: ");
	    String foto = sc.nextLine();
	    if (foto == null)
		break;
	    // Fotografia do utilizador (tipo passaporte)
	    MyImage ft = new MyImage(XMLDoc.getContexto() + foto);
	    ft.view();
	    if (user.setPhotography(ft))
		break;
	} while (true);

	do {// pode ser melhorado e facilitar a procura com uma lista
	    System.out.print("Indique o código iso (2 letras) da nacionalidade: ");
	    String nat = sc.nextLine();
	    if (nat == null)
		break;
	    try {
		if (user.setNationality(nat))
		    break;
	    } catch (XPathExpressionException e) {
		e.printStackTrace();
	    }
	} while (true);

	return user;
    }
    
    /**
     * Regista um novo utilizador, cria os nós XML apropriados (incluindo estatísticas) 
     * e guarda no disco usando as funções nativas da classe.
     */
/**
     * Regista um novo utilizador, cria os nós XML apropriados na ordem correta e guarda no disco.
     */
    public static synchronized User register(String nick, String pass, String foto, String nac, int idade) throws Exception {
        
        // 1. Verificar se o username já existe
        NodeList us = XMLDoc.getXPath("/users/user[username/text()='" + nick + "']", doc);
        if (us.getLength() > 0) {
            throw new Exception("O username '" + nick + "' já está em uso!");
        }

        // 2. Criar a instância de User
        User u = new User(); 
        u.setBlocked(false); 
        
        if (!u.setUsername(nick)) {
            throw new Exception("Username inválido! Deve ter entre 4 e 10 caracteres.");
        }
        
        u.setPassword(pass); 
        u.setPhotography(foto); 
        u.setNationality(nac); 
        u.setBirthdate(LocalDate.now().minusYears(idade));

        // 3. Construir o Elemento XML respeitando a ORDEM RIGOROSA do XSD
        Element userElement = doc.createElement("user");

        // A ordem obrigatória do ficheiro user.xsd é:
        // userid -> updated -> blocked -> profile -> username -> firstnames -> lastnames -> email -> gender -> birthdate -> photography -> nationality -> cor -> password
        
        addChild(doc, userElement, "userid", u.getUserId().toString());
        addChild(doc, userElement, "updated", dateTimeToXsd(u.getUpdated()));
        addChild(doc, userElement, "blocked", "false");
        addChild(doc, userElement, "profile", String.valueOf(u.getProfile()));
        addChild(doc, userElement, "username", u.getUsername());

        // Nomes (obrigatórios no XSD)
        addChild(doc, userElement, "firstnames", "Desconhecido");
        addChild(doc, userElement, "lastnames", "Desconhecido");

        // Email e Gender (Opcionais, mas preenchemos com valores padrão para manter a sequência feliz)
        addChild(doc, userElement, "email", u.getUsername() + "@mail.pt");
        addChild(doc, userElement, "gender", "X");

        // Data de Nascimento
        if (u.getBirthdate() != null) {
            addChild(doc, userElement, "birthdate", dateToXsd(u.getBirthdate()));
        }

        // Fotografia Base64
        if (u.getPhotography() != null) {
            addChild(doc, userElement, "photography", u.getPhotography().getBase64());
        }

        // Nacionalidade
        if (u.getNationality() != null) {
            addChild(doc, userElement, "nationality", u.getNationality().getAbbreviation());
        }

        // Cor de fundo preferida
        addChild(doc, userElement, "cor", u.getCorFundo());

        // Password (Hash)
        addChild(doc, userElement, "password", u.getPassword());

        // --- ESTATÍSTICAS DO JOGO ---
        // ATENÇÃO: Se o servidor der um erro de XSD a dizer "Invalid content... 'vitorias'", 
        // significa que precisas de ir ao teu ficheiro `users.xsd` ou `user.xsd` e adicionar estas 3 tags!
        addChild(doc, userElement, "vitorias", "0");
        addChild(doc, userElement, "derrotas", "0");
        addChild(doc, userElement, "tempo", "0");

        // 4. Anexar à Raiz do Documento
        NodeList uss = doc.getElementsByTagName("users");
        if (uss.getLength() != 1) {
            throw new Exception("Erro interno: Não encontrou o elemento raiz <users>!");
        }
        uss.item(0).appendChild(userElement);

        // 5. Gravar no disco (Faz a validação do XSD final)
        _save(); 

        return u;
    }

    public static synchronized User register(String nick, String pass, String firstNames, String lastNames,
	    String email, String gender, String birthdate, String foto, String nac, String corFundo) throws Exception {
	NodeList existentes = XMLDoc.getXPath("/users/user[username/text()='" + nick + "']", doc);
	if (existentes.getLength() > 0) {
	    throw new Exception("O username '" + nick + "' ja esta em uso!");
	}

	User u = new User();
	u.setBlocked(false);
	if (!u.setUsername(nick)) {
	    throw new Exception("Username invalido! Deve ter entre 4 e 10 caracteres.");
	}
	if (pass == null || pass.isBlank()) {
	    throw new Exception("A senha e obrigatoria.");
	}
	u.setPassword(pass);
	if (firstNames == null || firstNames.isBlank() || !u.setFirstNames(firstNames.trim())) {
	    throw new Exception("Os primeiros nomes sao obrigatorios.");
	}
	if (lastNames == null || lastNames.isBlank() || !u.setLastNames(lastNames.trim())) {
	    throw new Exception("Os apelidos sao obrigatorios.");
	}
	if (email == null || email.isBlank() || !u.setEmail(email.trim())) {
	    throw new Exception("O email indicado nao e valido.");
	}
	if (!u.setGender((gender == null || gender.isBlank()) ? "X" : gender.trim().toUpperCase(Locale.ROOT))) {
	    throw new Exception("O genero indicado nao e valido.");
	}
	try {
	    if (birthdate == null || birthdate.isBlank() || !u.setBirthdate(LocalDate.parse(birthdate.trim()))) {
		throw new Exception("A data de nascimento indicada nao e valida.");
	    }
	    if (u.getAge() < 3 || u.getAge() > 130) {
		throw new Exception("A idade deve estar entre 3 e 130 anos.");
	    }
	} catch (Exception e) {
	    throw new Exception(e.getMessage());
	}
	if (nac == null || nac.isBlank() || !u.setNationality(nac.trim().toUpperCase(Locale.ROOT))
		|| u.getNationality() == null) {
	    throw new Exception("A nacionalidade indicada nao e valida.");
	}
	if (!u.setCorFundo(corFundo)) {
	    throw new Exception("A cor de fundo indicada nao e valida.");
	}
	if (foto != null && !foto.isBlank()) {
	    MyImage imagem = new MyImage();
	    imagem.setBase64(foto.replaceAll("[\\s]", ""));
	    if (!imagem.isOk()) {
		throw new Exception("A fotografia indicada nao e valida.");
	    }
	    u.setPhotography(imagem);
	}

	Element userElement = doc.createElement("user");
	addChild(doc, userElement, "userid", u.getUserId().toString());
	addChild(doc, userElement, "updated", dateTimeToXsd(u.getUpdated()));
	addChild(doc, userElement, "blocked", "false");
	addChild(doc, userElement, "profile", String.valueOf(u.getProfile()));
	addChild(doc, userElement, "username", u.getUsername());
	addChild(doc, userElement, "firstnames", u.getFirstNames());
	addChild(doc, userElement, "lastnames", u.getLastNames());
	addChild(doc, userElement, "email", u.getEmail());
	addChild(doc, userElement, "gender", u.getGender());
	addChild(doc, userElement, "birthdate", dateToXsd(u.getBirthdate()));
	if (u.getPhotography() != null) {
	    addChild(doc, userElement, "photography", u.getPhotography().getBase64());
	}
	addChild(doc, userElement, "nationality", u.getNationality().getAbbreviation());
	addChild(doc, userElement, "cor", u.getCorFundo());
	addChild(doc, userElement, "password", u.getPassword());
	addChild(doc, userElement, "vitorias", "0");
	addChild(doc, userElement, "derrotas", "0");
	addChild(doc, userElement, "tempo", "0");

	doc.getElementsByTagName("users").item(0).appendChild(userElement);
	_save();
	return u;
    }

    /**
     * Método auxiliar para criar elementos XML de forma mais limpa e evitar repetição de código.
     */
    private static void addChild(Document doc, Element parent, String name, String value) {
        Element child = doc.createElement(name);
        child.setTextContent(value != null ? value : "");
        parent.appendChild(child);
    }
    


    /**
     * Mostra os dados do utilizador na consola
     * 
     * @throws Exception em caso de erro
     */
    public void print() throws Exception {
	System.out.println("----- Dados do Utilizador -----");
	if (blocked)
	    System.out.println("Está bloqueado!");
	else
	    System.out.println("Não está bloqueado!");
	System.out.println("Identificador (UUID): " + userId);
	System.out.println("Última atualização em: " + updated);
	System.out.println("Perfil: " + profile);
	System.out.println("Nome de utilizador: " + username);
	// Evitar mostrar a senha por motivos de segurança
	System.out.println("Senha: " + password);
	System.out.println("Nome completo: " + getName());
	if (email != null)
	    System.out.println("Endereço de email: " + email);
	if (gender != null)
	    System.out.println("Género: " + gender);
	if (corFundo != null)
	    System.out.println("Cor de fundo: " + corFundo);
	if (birthdate != null) {
	    System.out.println("Data de nascimento: " + birthdate);
	    System.out.println("Idade: " + getAge());
	}
	if (!getPtNationality().equals(""))
	    System.out.println("Nacionalidade: " + getPtNationality());
	System.out.println("---------------------------------------------------");
	if (getPhotography() != null)
	    getPhotography().view();
    }

    /**
     * Atualiza o DOM com os dados do utilizador atual
     * 
     * @throws ParserConfigurationException em caso de erro
     */
    public void toDocument() throws ParserConfigurationException {
	Element userElement = doc.createElement("user");

	Element aux = doc.createElement("userid");
	aux.setTextContent(getUserId().toString());
	userElement.appendChild(aux);

	aux = doc.createElement("updated");
	aux.setTextContent(dateTimeToXsd(LocalDateTime.now()));  //getUpdated()
	userElement.appendChild(aux);

	aux = doc.createElement("blocked");
	aux.setTextContent(isBlocked() ? "true" : "false");
	userElement.appendChild(aux);

	aux = doc.createElement("profile");
	aux.setTextContent(String.valueOf(getProfile()));
	userElement.appendChild(aux);

	aux = doc.createElement("username");
	aux.setTextContent(getUsername());
	userElement.appendChild(aux);

	aux = doc.createElement("firstnames");
	aux.setTextContent(getFirstNames());
	userElement.appendChild(aux);

	aux = doc.createElement("lastnames");
	aux.setTextContent(getLastNames());
	userElement.appendChild(aux);

	if (getEmail() != null) {
	    aux = doc.createElement("email");
	    String e = getEmail();
	    aux.setTextContent((e == null) ? "" : e);
	    userElement.appendChild(aux);
	}

	if (getGender() != null) {
	    aux = doc.createElement("gender");
	    String g = getGender();
	    aux.setTextContent((g == null) ? "" : g);
	    userElement.appendChild(aux);
	}

	if (getBirthdate() != null) {
	    aux = doc.createElement("birthdate");
	    if (getBirthdate() == null)
		aux.setTextContent("");
	    else
		aux.setTextContent(dateToXsd(getBirthdate()));
	    userElement.appendChild(aux);
	}
	// Fotografia (se disponível)
	if (getPhotography() != null) {
	    aux = doc.createElement("photography");
	    String foto = getPhotography().getBase64();
	    aux.setTextContent((foto == null) ? "" : foto);
	    userElement.appendChild(aux);
	}

	// Nacionalidade
	if (getNationality() != null) {
	    aux = doc.createElement("nationality");
	    String abr = getNationality().getAbbreviation();
	    aux.setTextContent((abr == null) ? "" : abr);
	    userElement.appendChild(aux);
	}

	aux = doc.createElement("cor");
	aux.setTextContent((getCorFundo() == null) ? COR_PADRAO : getCorFundo());
	userElement.appendChild(aux);

	aux = doc.createElement("password");
	aux.setTextContent(getPassword());
	userElement.appendChild(aux);

	// Procura o Nó principal
	NodeList uss = doc.getElementsByTagName("users");
	if (uss.getLength() != 1) {
	    System.out.println("Não encontrou o elemento raiz!");
	    return; // erro, inconsistencia
	}
	Node principal = uss.item(0);

	// Procura o Nó correpondente ao utilizador atual
	NodeList nl = doc.getElementsByTagName("userid");
	int i = 0;
	for (; i <= nl.getLength(); i++)
	    if (nl.item(i).getTextContent().equals(userId.toString()))
		break;

	// Remove o utilizador antigo
	principal.removeChild(nl.item(i).getParentNode());

	// Acrescenta o utilizaor atual
	principal.appendChild(userElement);

	try {// para prevenir algum bug
	    XMLDoc.validDocXSD(doc, XMLDoc.getContexto() + file + ".xsd");
	} catch (SAXException e) {
	    e.printStackTrace();
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

    /**
     * Gera string que representa dados de um utilizador a ser usado remotamente
     * @param simbolo do jogador
     * @return o utilizador formatado para uso remoto
     * 
     * @throws ParserConfigurationException em caso de erro
     */
    public String toXMLString(char simbolo) throws ParserConfigurationException {
	String ret = "<jogador simbolo='"+simbolo+"'>"
		+ "<userid>"+getUserId().toString()+"</userid>"
		+ "<updated>"+dateTimeToXsd(getUpdated())+"</updated>"
		+ "<blocked>"+isBlocked()+"</blocked>"
		+ "<profile>"+getProfile()+"</profile>"
		+ "<username>"+escapeXml(getUsername())+"</username>"
		+ "<firstnames>"+escapeXml(getFirstNames())+"</firstnames>"
		+ "<lastnames>"+escapeXml(getLastNames())+"</lastnames>";
		if(getEmail()!=null && !getEmail().isBlank())
		    ret+="<email>"+escapeXml(getEmail())+"</email>";
		if(getGender()!=null && !getGender().isBlank())
		    ret+="<gender>"+getGender()+"</gender>";
		if(getBirthdate()!=null)
		    ret+="<birthdate>"+dateToXsd(getBirthdate())+"</birthdate>";
		if(getPhotography()!=null)
		    ret+="<photography>"+getPhotography().getBase64()+"</photography>";
		ret+="<full-nationality>";
		Nationality nat = getNationality();
		ret +=	"<abbreviation>"+escapeXml(nat.getAbbreviation())+"</abbreviation>"
			+ "<name>"+escapeXml(nat.getName())+"</name>"
			+ "<official>"+escapeXml(nat.getOfficial())+"</official>"
			+ "<pt-name>"+escapeXml(nat.getPtName())+"</pt-name>"
			+ "<pt-nationality>"+escapeXml(nat.getPtNationality())+"</pt-nationality>"
			+ "<pt-male>"+escapeXml(nat.getPtMale())+"</pt-male>"
			+ "<pt-female>"+escapeXml(nat.getPtFemale())+"</pt-female>"
			+ "<flag>"+nat.getFlag().getBase64()+"</flag>";
		ret +=		
			"</full-nationality>"
			  + "<full-name>"+escapeXml(getName())+"</full-name>"
			  + "<age>"+((getAge()==-1)?"":getAge())+"</age>"
		+ "</jogador>";
	return ret; //.replaceAll("[\\s]", "");
    }

    private static String escapeXml(String value) {
	if (value == null) {
	    return "";
	}
	return value.replace("&", "&amp;")
		.replace("<", "&lt;")
		.replace(">", "&gt;")
		.replace("\"", "&quot;")
		.replace("'", "&apos;");
    }

    /**
     * Atualiza o utilizdor atual a partir do elemento indicado em parametro
     * 
     * @param userElement elemento que representa um utilizador no DOM
     * @throws Exception em caso de erro
     */
    public void fromElement(Element userElement) throws Exception {
	// Extract and set the user data from the XML element
	setUserId(UUID.fromString(userElement.getElementsByTagName("userid").item(0).getTextContent()));
	setUpdated(xsdToLocalDateTime(userElement.getElementsByTagName("updated").item(0).getTextContent()));
	setBlocked(Boolean.parseBoolean(userElement.getElementsByTagName("blocked").item(0).getTextContent()));
	setProfile(Integer.parseInt(userElement.getElementsByTagName("profile").item(0).getTextContent()));
	setUsername(userElement.getElementsByTagName("username").item(0).getTextContent());
	password = userElement.getElementsByTagName("password").item(0).getTextContent();
	setFirstNames(userElement.getElementsByTagName("firstnames").item(0).getTextContent());
	setLastNames(userElement.getElementsByTagName("lastnames").item(0).getTextContent());
	NodeList nl = userElement.getElementsByTagName("email");
	setEmail((nl.getLength() == 1) ? nl.item(0).getTextContent() : null);
	nl = userElement.getElementsByTagName("gender");
	setGender((nl.getLength() == 1) ? nl.item(0).getTextContent() : null);
	nl = userElement.getElementsByTagName("birthdate");
	setBirthdate((nl.getLength() == 1) ? xsdToLocalDate(nl.item(0).getTextContent()) : null);
	nl = userElement.getElementsByTagName("nationality");
	setNationality((nl.getLength() == 1) ? nl.item(0).getTextContent() : null);
	nl = userElement.getElementsByTagName("cor");
	setCorFundo((nl.getLength() == 1) ? nl.item(0).getTextContent() : null);
	nl = userElement.getElementsByTagName("photography");
	setPhotography((nl.getLength() == 1) ? nl.item(0).getTextContent() : null);
    }

    /**
     * @param xsdDate data no formato do XSD
     * @return data no Java
     */
    public static LocalDate xsdToLocalDate(String xsdDate) {
	if(xsdDate==null || xsdDate.isBlank())
	    return null;
	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	return LocalDate.parse(xsdDate, formatter);
    }

    /**
     * @param xsdDateTime data/hora no formato do XSD
     * @return data/hora no Java
     * @throws Exception em caso de erro
     */
    public static LocalDateTime xsdToLocalDateTime(String xsdDateTime) throws Exception {
	if(xsdDateTime==null || xsdDateTime.isBlank())
	    return null;
	XMLGregorianCalendar xmlCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(xsdDateTime);
	return LocalDateTime.ofInstant(xmlCalendar.toGregorianCalendar().toInstant(), ZoneId.systemDefault());
    }

    /**
     * @param localDate data no Java
     * @return string que representa data no formato do XSD
     */
    public static String dateToXsd(LocalDate localDate) {
	if(localDate==null)
	    return "";
	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	return localDate.format(formatter);
    }

    /**
     * @param localDateTime data/hora no Java
     * @return string que representa data/hora no formato do XSD
     */
    public static String dateTimeToXsd(LocalDateTime localDateTime) {// norma ISO 8601
	if(localDateTime==null)
	    return "";
	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
	return localDateTime.format(formatter);
    }

    // Capitaliza palavras com mais de 3 letras
    private static String capitalizar(String nome) {

	String[] palavras = nome.trim().split(" ");
	StringBuilder capitaliza = new StringBuilder();

	for (String palavra : palavras)
	    if (palavra.length() > 3)
		capitaliza.append(palavra.substring(0, 1).toUpperCase(Locale.ROOT)).append(palavra.substring(1))
			.append(" ");
	    else
		capitaliza.append(palavra).append(" ");

	return capitaliza.toString();
    }

    /**
     * Retorna um único utilizador (ou null) que respeite a expressão em argumento
     * @param xpath Expressão XPATH
     * @return	o utilizador encontrado
     * @throws Exception em caso de erro
     */
    public static User getUser(String xpath) throws Exception {
	NodeList l = XMLDoc.getXPath(xpath, doc);
	if (l.getLength() == 1) {
	    User us = new User();
	    us.fromElement((Element) l.item(0));
	    return us;
	}
	System.out.println("Não encontrou!");
	return null;
    }

    /**
     * Procura utilizador com o userId indicado
     * @param userId 	 Identificador do utilizador
     * @return 		 Utilizador encontrado
     * @throws Exception Em caso de erro
     */
    public static User getByUserId(String userId) throws Exception {
	return getUser("/users/user[userid/text()='" + userId + "']");
    }

    /**
     * Procura o utilizador com o username indicado
     * @param username 		Nome de utilizador
     * @return 			Utilizador encontrado
     * @throws Exception em 	Em caso de erro
     */
    public static User getByUserName(String username) throws Exception {
	return getUser("/users/user[username/text()='" + username + "']");
    }

    /**
     * Apresenta lista de usernames com as letras iniciais e retorna a hipotese
     * indicada pelo utilizador
     * 
     * @return O username selecionado pelo utilizador
     * @throws XPathExpressionException em caso de erro
     */
    public static String askUserName() throws XPathExpressionException {

	// Lê as primeiras letras do nome do utilizador
	System.out.println("Indique as primeiras letras do nome do utilizador: ");
	String inicio = sc.nextLine();

	// Cria expressão XPath para filtrar usernames
	String xp = "boolean('true')";
	if (inicio.length() != 0) {
	    xp = "starts-with(text(),'" + inicio + "')";
	}

	// Obtém lista de usernames que coincidem com as letras iniciais
	NodeList pl = XMLDoc.getXPath("/users/user/username[" + xp + "]/text()", doc);

	// Se não encontrar nenhum username, retorna ""
	if (pl.getLength() == 0) {
	    return "";
	}

	// Se encontrar apenas um username, retorna o username
	if (pl.getLength() == 1) {
	    return pl.item(0).getTextContent();
	}

	// Se encontrar mais de um username, apresenta lista para o utilizador escolher
	List<String> lista = new ArrayList<>();
	for (int i = 0; i < pl.getLength(); i++) {
	    lista.add(pl.item(i).getTextContent());
	}
	lista.sort(Comparator.naturalOrder());
	for (int i = 0; i < lista.size(); i++) {
	    System.out.println((i + 1) + " - " + lista.get(i));
	}

	// Lê o número do username escolhido pelo utilizador
	System.out.println("\nDigite o número associado ao nome do utilizador:");
	int ut = sc.nextInt();

	// Retorna o username escolhido pelo utilizador
	return lista.get(ut - 1);
    }

    // Apresenta dados do utilizador que tem um determinado username
    private static void exemplo1() throws Exception {
	String username = askUserName();
	User ur = getByUserName(username);
	ur.print();
    }

    // Altera a foto
    private static void exemplo2() throws XPathExpressionException, NoSuchAlgorithmException, SAXException, IOException,
	    TransformerFactoryConfigurationError, TransformerException {
	String username = askUserName();
	if(username==null || username.isBlank())
	    return;
	// Lê a senha antiga do utilizador.
	System.out.println("Indique o nome do ficheiro na pasta corrente:");
	String fich = null;
	do {
	    fich = sc.nextLine();
	} while (fich == null || fich.length() == 0);
	if (_chgFoto(username, fich))
	    System.out.println("Alteração da fotografia realizada com sucesso!");
	else
	    System.out.println("Falhou a alteração da fotografia!");
	_save();
	_load();
    }

    // Altera a senha/password
    private static void exemplo3() throws XPathExpressionException, NoSuchAlgorithmException, SAXException, IOException,
	    TransformerFactoryConfigurationError, TransformerException {
	// Lê o nome do utilizador.
	String nome = askUserName();
	if(nome==null || nome.isBlank())
	    return;
	// Lê a senha antiga do utilizador.
	System.out.println("Indique a senha antiga:");
	String senhaAntiga = null;
	do {
	    senhaAntiga = sc.nextLine();
	} while (senhaAntiga == null || senhaAntiga.length() == 0);

	// Lê a senha nova do utilizador.
	System.out.println("Indique a senha nova:");
	String senhaNova = null;
	do {
	    senhaNova = sc.nextLine();
	} while (senhaNova == null || senhaNova.length() == 0);

	if (_chgPass(nome, senhaAntiga, senhaNova))
	    System.out.println("Alteração da senha realizada com sucesso!");
	else
	    System.out.println("Falhou a alteração da senha!");
	_save();
	_load();
    }

    // Bloqueia o utilizador
    private static void exemplo4() {
	try {
	    String nome = askUserName();
	    if(nome==null || nome.isBlank())
		return;
	    _block(nome);
	    _save();
	    _load();
	    System.out.println("Bloqueio realizado com sucesso!");
	} catch (XPathExpressionException | SAXException | IOException | TransformerFactoryConfigurationError
		| TransformerException e) {
	    e.printStackTrace();
	}
    }

    // Desbloqueia o utilizador
    private static void exemplo5() {
	try {
	    String nome = askUserName();
	    if(nome==null || nome.isBlank())
		return;
	    _unblock(nome);
	    _save();
	    _load();
	    System.out.println("Desloqueio realizado com sucesso!");
	} catch (XPathExpressionException | SAXException | IOException | TransformerFactoryConfigurationError
		| TransformerException e) {
	    e.printStackTrace();
	}
    }

    private static void exemplo6() throws Exception {
	// Lê o nome do utilizador.
	String nome = askUserName();
	if(nome==null || nome.isBlank())
	    return;
	// Lê a senha do utilizador.
	System.out.println("Indique a senha do utilizador:");
	String senha = null;
	do {
	    senha = sc.nextLine();
	} while (senha == null || senha.length() == 0);

	// Inicia a sessão.
	User login = _authenticate(nome, senha);
	if (login != null) {
	    System.out.println("Autenticação (login) realizada com sucesso!");
	    // Mostra dados do utilizador que fez login
	    login.print();
	} else
	    System.out.println("Falhou a autenticação (login)!");
    }
    
    private static void exemplo7() throws Exception {
	// Lê o nome do utilizador.
	String nome = askUserName();
	if(nome==null || nome.isBlank())
	    return;
	User ur = getByUserName(nome);
	Document d=XMLDoc.parseString(ur.toXMLString('X'));
	System.out.println("Nacionalidade: "+d.getElementsByTagName("pt-nationality").item(0).getTextContent());
	System.out.println("Nome: "+d.getElementsByTagName("full-name").item(0).getTextContent());
    }

    /**
     * Menu que demostra a utilização desta classe
     */
    public static void menu() {
	char op;
	do {
	    System.out.println();
	    System.out.println();
	    System.out.println("*** Utilizador ***");
	    System.out.println("1 - Consultar utilizador");
	    System.out.println("2 - Alterar fotografia");
	    System.out.println("3 - Alterar senha");
	    System.out.println("4 - Bloquear utilizador");
	    System.out.println("5 - Desbloquear utilizador");
	    System.out.println("6 - Simular autenticação/login");
	    System.out.println("7 - Testar utilizador remoto");
	    System.out.println("8 - ?Atualizar utilizador");
	    System.out.println("0 - Terminar!");
	    String str = null;
	    do {
		str = sc.nextLine();
	    } while (str == null || str.length() == 0);
	    op = str.charAt(0);
	    switch (op) {
	    case '1':
		try {
		    exemplo1();
		} catch (Exception e) {
		    e.printStackTrace();
		}
		break;
	    case '2':
		try {
		    exemplo2();
		} catch (Exception e) {
		    e.printStackTrace();
		}
		break;
	    case '3':
		try {
		    exemplo3();
		} catch (Exception e) {
		    e.printStackTrace();
		}
		break;
	    case '4':
		try {
		    exemplo4();
		} catch (Exception e) {
		    e.printStackTrace();
		}
		break;
	    case '5':
		try {
		    exemplo5();
		} catch (Exception e) {
		    e.printStackTrace();
		}
		break;
	    case '6':
		try {
		    exemplo6();
		} catch (Exception e) {
		    e.printStackTrace();
		}
		break;
	    case '7':
		try {
		    exemplo7();
		} catch (Exception e) {
		    e.printStackTrace();
		}
		break;
	    default:
		System.out.println("Opção inválida, esolha uma opção do menu.");
	    }
	} while (op != '0');
	System.out.println("Terminou a execução.");
	System.exit(0);
    }

    /**
     * Autenticar um utilizador. Se o utilizador estiver bloqueado falha a
     * autenticação.
     * 
     * @param username Nome de utilizador
     * @param password Senha
     * @return Instância da classe User com informações do utilizador se
     *         autenticação for bem-sucedida, null caso contrário
     * @throws Exception em caso de erro
     */
    public static User _authenticate(String username, String password) throws Exception {
	// Procura o utilizador (não bloqueado) no ficheiro XML com o nome de utilizador
	// e senha fornecidos

	NodeList us = XMLDoc.getXPath("/users/user[username/text()='" + username + "' and password/text()='"
		+ XMLDoc.SHA256(password) + "' and blocked/text()='false']/userid/text()", doc);

	// Verifica se um único utilizador desbloqueado foi encontrado com as
	// credenciais
	if (us.getLength() != 1) {
	    return null; // Utilizador não encontrado ou credenciais inválidas
	}

	// Retorna os dados do utilizador que se autenticou
	return getByUserName(username);
    }

    /**
     * Altera a senha do utilizador indicado em parametro. Não grava a alteração no
     * disco.
     * 
     * @param username    Nome de utilizador
     * @param senhaAntiga Senha antiga
     * @param senhaNova   Senha nova
     * @return sucesso
     * @throws NoSuchAlgorithmException em caso de erro
     * @throws XPathExpressionException em caso de erro
     */
    public static boolean _chgPass(String username, String senhaAntiga, String senhaNova)
	    throws XPathExpressionException, NoSuchAlgorithmException {
	// Procura o utilizador no DOM com o nome de utilizador e senha fornecidos

	NodeList us = XMLDoc.getXPath("/users/user[username/text()='" + username + "' and password/text()='"
		+ XMLDoc.SHA256(senhaAntiga) + "']/password", doc);

	// Verifica se foi encontrado um único utilizador com as credenciais
	if (us.getLength() != 1) {
	    return false; // Utilizador não encontrado ou credenciais inválidas
	}

	// Altera a senha
	us.item(0).setTextContent(XMLDoc.SHA256(senhaNova));

	return true;
    }

    /**
     * Altera a fotografia do utilizador indicado em parametro. Não grava a
     * alteração no disco.
     * 
     * @param username Nome de utilizador
     * @param file     Ficheiro local com a foto
     * @return Sucesso
     * @throws XPathExpressionException em caso de erro
     */
    public static boolean _chgFoto(String username, String file) throws XPathExpressionException {
	MyImage f = new MyImage(XMLDoc.getContexto() + file);
	if (!f.isOk())
	    return false;
	return _updatePerfil(username, f.getBase64(), null);
    }

    public static boolean _chgFotoBase64(String username, String fotoBase64) throws XPathExpressionException {
	if (fotoBase64 == null || fotoBase64.isBlank()) {
	    return false;
	}
	return _updatePerfil(username, fotoBase64, null);
    }

    public static synchronized boolean _updatePerfil(String username, String fotoBase64, String corFundo)
	    throws XPathExpressionException {
	return _updatePerfil(username, null, null, null, null, null, null, fotoBase64, corFundo);
    }

    public static synchronized boolean _updatePerfil(String username, String firstNames, String lastNames,
	    String email, String gender, String birthdate, String nationality, String fotoBase64, String corFundo)
	    throws XPathExpressionException {
	NodeList us = XMLDoc.getXPath("/users/user[username/text()='" + username + "']", doc);
	if (us.getLength() != 1) {
	    return false;
	}

	Element user = (Element) us.item(0);
	boolean alterou = false;
	String fotoNormalizada = null;
	String corNormalizada = null;
	LocalDate birthdateNormalizada = null;
	String nationalityNormalizada = null;

	if (firstNames != null && firstNames.isBlank()) {
	    return false;
	}
	if (lastNames != null && lastNames.isBlank()) {
	    return false;
	}
	if (email != null) {
	    User validacao = new User();
	    if (email.isBlank() || !validacao.setEmail(email.trim())) {
		return false;
	    }
	}
	if (gender != null && !gender.isBlank()
		&& !gender.trim().toUpperCase(Locale.ROOT).matches("[MFX]")) {
	    return false;
	}
	if (birthdate != null) {
	    try {
		birthdateNormalizada = LocalDate.parse(birthdate.trim());
		if (birthdateNormalizada.isAfter(LocalDate.now())) {
		    return false;
		}
		int idade = Period.between(birthdateNormalizada, LocalDate.now()).getYears();
		if (idade < 3 || idade > 130) {
		    return false;
		}
	    } catch (Exception e) {
		return false;
	    }
	}
	if (nationality != null) {
	    nationalityNormalizada = nationality.trim().toUpperCase(Locale.ROOT);
	    if (nationalityNormalizada.isBlank() || Nationality.getByAbbreviation(nationalityNormalizada) == null) {
		return false;
	    }
	}

	if (fotoBase64 != null && !fotoBase64.isBlank()) {
	    MyImage foto = new MyImage();
	    foto.setBase64(fotoBase64.replaceAll("[\\s]", ""));
	    if (!foto.isOk()) {
		return false;
	    }
	    fotoNormalizada = foto.getBase64();
	}

	if (corFundo != null && !corFundo.isBlank()) {
	    corNormalizada = corFundo.trim().toUpperCase(Locale.ROOT);
	    if (!corNormalizada.matches("^#[0-9A-F]{6}$")) {
		return false;
	    }
	}

	if (fotoNormalizada != null) {
	    Element photography = ensureChildBeforeAny(user, "photography", "", "nationality", "cor", "password");
	    photography.setTextContent(fotoNormalizada);
	    alterou = true;
	}

	if (corNormalizada != null) {
	    Element cor = ensureChildBeforeAny(user, "cor", COR_PADRAO, "password");
	    cor.setTextContent(corNormalizada);
	    alterou = true;
	}

	if (firstNames != null) {
	    getDirectChild(user, "firstnames").setTextContent(firstNames.trim());
	    alterou = true;
	}
	if (lastNames != null) {
	    getDirectChild(user, "lastnames").setTextContent(lastNames.trim());
	    alterou = true;
	}
	if (email != null) {
	    getDirectChild(user, "email").setTextContent(email.trim());
	    alterou = true;
	}
	if (gender != null && !gender.isBlank()) {
	    Element genderElement = ensureChildBeforeAny(user, "gender", "X", "birthdate", "photography",
		    "nationality", "cor", "password");
	    genderElement.setTextContent(gender.trim().toUpperCase(Locale.ROOT));
	    alterou = true;
	}
	if (birthdateNormalizada != null) {
	    Element birthdateElement = ensureChildBeforeAny(user, "birthdate", "", "photography", "nationality",
		    "cor", "password");
	    birthdateElement.setTextContent(dateToXsd(birthdateNormalizada));
	    alterou = true;
	}
	if (nationalityNormalizada != null) {
	    Element nationalityElement = ensureChildBeforeAny(user, "nationality", "", "cor", "password");
	    nationalityElement.setTextContent(nationalityNormalizada);
	    alterou = true;
	}

	if (alterou) {
	    Element updated = getDirectChild(user, "updated");
	    if (updated != null) {
		updated.setTextContent(dateTimeToXsd(LocalDateTime.now()));
	    }
	}

	return alterou;
    }

    /**
     * Bloqueia/Desbloqueia o utilizador referido em parametro Não grava a alteração
     * no disco.
     * 
     * @param username Nome de utilizador a ser bloqueado
     * @param valor "true"-bloquear ou "false"-desbloquear
     * @throws XPathExpressionException em caso de erro
     */
    private static Element getDirectChild(Element parent, String tagName) {
	NodeList children = parent.getChildNodes();
	for (int i = 0; i < children.getLength(); i++) {
	    Node n = children.item(i);
	    if (n.getNodeType() == Node.ELEMENT_NODE && tagName.equals(n.getNodeName())) {
		return (Element) n;
	    }
	}
	return null;
    }

    private static Element ensureChildAfter(Element parent, String tagName, String afterTagName) {
	Element existing = getDirectChild(parent, tagName);
	if (existing != null) {
	    return existing;
	}

	Element novo = doc.createElement(tagName);
	novo.setTextContent("0");

	Element after = getDirectChild(parent, afterTagName);
	if (after == null) {
	    parent.appendChild(novo);
	    return novo;
	}

	Node ref = after.getNextSibling();
	while (ref != null && ref.getNodeType() != Node.ELEMENT_NODE) {
	    ref = ref.getNextSibling();
	}
	if (ref == null) {
	    parent.appendChild(novo);
	} else {
	    parent.insertBefore(novo, ref);
	}
	return novo;
    }

    private static Element ensureChildBeforeAny(Element parent, String tagName, String defaultValue,
	    String... beforeTagNames) {
	Element existing = getDirectChild(parent, tagName);
	if (existing != null) {
	    return existing;
	}

	Element novo = doc.createElement(tagName);
	novo.setTextContent(defaultValue);

	for (String beforeTagName : beforeTagNames) {
	    Element before = getDirectChild(parent, beforeTagName);
	    if (before != null) {
		parent.insertBefore(novo, before);
		return novo;
	    }
	}

	parent.appendChild(novo);
	return novo;
    }

    private static int parseIntOrZero(String valor) {
	try {
	    return Integer.parseInt(valor.trim());
	} catch (Exception e) {
	    return 0;
	}
    }

    private static boolean incrementarEstatistica(String username, String campo) throws XPathExpressionException {
	NodeList us = XMLDoc.getXPath("/users/user[username/text()='" + username + "']", doc);
	if (us.getLength() != 1) {
	    return false;
	}

	Element user = (Element) us.item(0);
	ensureChildAfter(user, "vitorias", "password");
	ensureChildAfter(user, "derrotas", "vitorias");
	ensureChildAfter(user, "tempo", "derrotas");

	Element target = getDirectChild(user, campo);
	if (target == null) {
	    return false;
	}
	int atual = parseIntOrZero(target.getTextContent());
	target.setTextContent(String.valueOf(atual + 1));
	return true;
    }

    public static synchronized void registarResultadoJogo(String vencedor, String vencido) throws Exception {
	boolean alterou = false;
	if (vencedor != null && !vencedor.isBlank()) {
	    alterou = incrementarEstatistica(vencedor, "vitorias") || alterou;
	}
	if (vencido != null && !vencido.isBlank()) {
	    alterou = incrementarEstatistica(vencido, "derrotas") || alterou;
	}
	if (alterou) {
	    _save();
	    _load();
	}
    }

    public static void lock(String username, String valor) throws XPathExpressionException {
	// Procura o nó "blocked" do utilizador
	NodeList us = XMLDoc.getXPath("/users/user[username/text()='" + username + "']/blocked", doc);

	// Se o utilizador for encontrado
	if (us.getLength() == 1) {
	    // Altera o conteúdo do nó "blocked" para "true" ou "false"
	    us.item(0).setTextContent(valor);
	} else {
	    // Caso contrário, lança uma exceção
	    throw new XPathExpressionException("Utilizador não encontrado: " + username);
	}
    }

    /**
     * @param username utilizador que vai ser bloqueado
     * @throws XPathExpressionException em caso de erro
     */
    public static void _block(String username) throws XPathExpressionException {
	lock(username, "true");
    }

    /**
     * @param username utilizador que vai ser desbloqueado
     * @throws XPathExpressionException em caso de erro
     */
    public static void _unblock(String username) throws XPathExpressionException {
	lock(username, "false");
    }

    /**
     * Obtém o utilizador com o nome de utilizador especificado
     * 
     * @param username Nome de utilizador
     * @return Instância da classe User com informações do utilizador
     * @throws Exception em caso de erro
     */
    public static User _obtain(String username) throws Exception {
	NodeList us = XMLDoc.getXPath("/users/user[username/text()='" + username + "']", doc);

	if (us.getLength() == 1) {
	    Element pai = (Element) us.item(0);
	    User ret = new User();
	    ret.fromElement(pai);
	    return ret;
	}

	throw new Exception("Utilizador não encontrado: " + username);
    }

    /**
     * Atualiza um utilizador existente ou cria um novo se não existir
     * 
     * Este método recebe um objeto da classe `User` como argumento e atualiza os
     * dados do utilizador indicado em parametro no DOM. Se o utilizador não existir
     * no documento, ele é criado.
     * 
     * @param user Instância da classe User com os dados do utilizador a ser
     *             atualizado/adicionado
     * @throws XPathExpressionException             Exceção lançada caso ocorra um
     *                                              erro na expressão XPath
     *                                              utilizada para localizar o
     *                                              utilizador
     * @throws DOMException                         Exceção lançada caso ocorra um
     *                                              erro ao manipular o documento
     *                                              XML
     * @throws ParserConfigurationException         Exceção lançada caso não seja
     *                                              possível criar um novo documento
     *                                              XML (necessário para a criação
     *                                              de um novo utilizador)
     * @throws TransformerException                 Exceção lançada caso ocorra um
     *                                              erro ao transformar o documento
     *                                              XML
     * @throws TransformerFactoryConfigurationError Exceção lançada caso não seja
     *                                              possível configurar a fábrica de
     *                                              transformadores XSLT
     * @throws IOException                          Exceção lançada caso ocorra um
     *                                              erro ao ler ou escrever o
     *                                              documento XML
     * @throws SAXException                         Exceção lançada caso ocorra um
     *                                              erro ao analisar o documento XML
     */
    public static void _replace(User user) throws ParserConfigurationException, SAXException, IOException,
	    TransformerFactoryConfigurationError, TransformerException, XPathExpressionException {

	// Procura o elemento "user" com o mesmo ID do utilizador indicado em parametro
	NodeList us = XMLDoc.getXPath("/users/user[userid/text()='" + user.getUserId() + "']", doc);

	// Comentário:
	// - A expressão XPath acima procura por um elemento "user" dentro do elemento
	// "users"
	// - O filtro `[userid/text()=' + user.getUserId() + ']` garante que apenas o
	// utilizador com o ID especificado seja selecionado.

	// Procura o Nó principal
	NodeList nl = doc.getElementsByTagName("users");
	if (nl.getLength() != 1) {
	    System.out.println("Não encontrou o elemento raiz!");
	    return; // erro, inconsistencia
	}
	Node principal = nl.item(0);

	// Comentário:
	// - Obtém o elemento "users" principal do documento XML.
	// - Verifica se existe apenas um elemento "users". Se não, há um erro na
	// estrutura do documento.

	// Se o utilizador existir, remove a versão antiga antes de inserir a atualizada.
	if (us.getLength() == 1) {
	    principal.removeChild(us.item(0));
	}
	// Garante que a versão atual do utilizador fica no DOM.
	user.toDocument();
	// Salva as alterações no disco.
	_save();
    }

    /**
     * Valida o documento XML, garante backup e grava as alterações.
     * 
     * @throws SAXException                         Se ocorrer um erro ao analisar o
     *                                              documento XML.
     * @throws IOException                          Se ocorrer um erro ao ler ou
     *                                              escrever arquivos.
     * @throws TransformerFactoryConfigurationError Se houver um problema na
     *                                              configuração da fábrica de
     *                                              transformação XML.
     * @throws TransformerException                 Se ocorrer um erro ao
     *                                              transformar o documento XML.
     */
    public static void _save()
	    throws SAXException, IOException, TransformerFactoryConfigurationError, TransformerException {

	// 1. Valida o documento XML contra o esquema XSD para garantir sua conformidade
	// com a estrutura esperada
	// --> Prevenir a gravação de dados inválidos ou inconsistentes.
	XMLDoc.validDocXSD(doc, XMLDoc.getContexto() + file + ".xsd");

	// 2. Cria o caminho completo para o ficheiro XML de utilizadores
	String docXML = XMLDoc.getContexto() + file + ".xml";

	// 3. Gera um nome de ficheiro para guardar um backup do ficheiro original
	String backup = XMLDoc.gerarNomeFBackupVersao(docXML);

	// 4. Grava as alterações feitas no ficheiro XML de utilizadores
	// --> Usando um mecanismo de bloqueio para evitar problemas de concorrência.
	// --> Renomeia o ficheiro original antes de o alterar.
	XMLDoc.gravarLock(doc, docXML, backup);
    }
 
    /**
     * Demonstra como se usa a classe
     * @param args não usado
     */
    public static void main(final String[] args) {
    	try {
			System.out.println("-->"+XMLDoc.SHA256("p9"));
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	menu();
    }
}

/*
 * Resumo dos utilizadores configurados
 * "('cartwheel','p1','Santiago','Castro Pacheco','San.Pacheco@mail.pt')"
 * "('milkshake','p2','Veríssimo','Simões Silvestre','Ver.Silvestre@mail.us')"
 * "('cranberry','p3','Ismael','Cravo Abril','Ism.Abril@mail.es')"
 * "('gandalf','p4','Camilo','Rosa Braga','Cam.Braga@mail.es')"
 * "('opera','p5','Gustavo','Pascoal Pires','Gus.Pires@mail.fr')"
 * "('pegasus','p6','Odilon','Nogueira Dantas','Od.Dantas@mail.it')"
 * "('deneb','p7','Olga','Faria Luz','Ol.Luz@bustayes.com')"
 * "('luzkira','p8','Luzia','Gilda Reis','Luz.Reis@mail.pt')"
 * "('smoke','p9','Felicidade','Varejão Amaral','Fel.Amaral@mail.es')"
 * "('bagel','p10','Luísa','Carriço d''Almeida','Lu.Almeida@mail.es')"
 * "('rush','p11','Natacha','Caetano Prego','Nat.Prego@mail.es')"
 * "('bird','p12','Isabela','Peres da Ponte','Isa.Ponte@mail.pt')"
 * "('robotik','p13','Engracia','Gwyther Ximenez','egwyther0@redcross.org','F')"
 * "('daybreak','p14','Marielle','Bonicelli MacNeachtain','mbonicelli2@sitemeter.com','F')"
 * "('astroboy','p15','Blakelee','Wilcot Watkinson','bwilcot4@twitpic.com','M')"
 */
