package client;

import java.net.Socket;
import java.time.LocalDateTime;
import java.util.Scanner;

import org.w3c.dom.Element;

import util.XMLDoc;

/**
 * Classe Jogador representa um jogador que se liga ao servidor
 * e joga o jogo do galo.
 *
 * @author Engº Porfírio Filipe
 */
public class Jogador {
	/**
     * Host por omissão do servidor (endereço IP).
     */
    private final static String DEFAULT_HOST = "localhost";

    /**
     * Porto por omissão do servidor.
     */
    private final static int DEFAULT_PORT = 5025;

    static String host = DEFAULT_HOST;
    static int port = DEFAULT_PORT;
    
    // Acesso único ao teclado, define um Scanner para ser reutilziado
    private static Scanner leitor = null; 
    
    // Configuração inicial do client
    public Jogador(String Host, int Port, Scanner Sc) {
    	host = Host;
    	port = Port;
    	leitor = Sc;
    }
    
    /**
     * Lê um número curto da entrada do utilizador.
     *
     * @param 	leitor Scanner para ler a entrada do utilizador.
     * @return 	Número curto válido (entre 1 e 12).
     */
    private static short readShort(Scanner leitor) 
    {
        short numero = 0; 
        
        while (true) 
        {
            if (leitor.hasNextShort()) 
            {
                numero = leitor.nextShort();
                
                if (numero < 1 || numero > 12) 
                {
                    System.out.println("Jogada inválida!");
                } 
                else 
                {
                    return numero;
                }
            } 
            else 
            {
                leitor.nextLine();
            }
        }
    }
    
    // Lê a senha em modo camuflado se estiver disponivel
	  private static String leSenha(String prompt, Scanner s) {
	      String senha=null;
	      if(System.console() != null)
		  senha = new String(System.console().readPassword(prompt, 5000));
	      else {
		  System.out.println(prompt);
		  senha = s.nextLine();
	      }
	      return senha;
	  }

	  private static String normalizarCaminhoFoto(String caminho) {
	      if (caminho == null) {
	          return "";
	      }
	      String texto = caminho.trim();
	      if ((texto.startsWith("\"") && texto.endsWith("\"")) || (texto.startsWith("'") && texto.endsWith("'"))) {
	          texto = texto.substring(1, texto.length() - 1).trim();
	      }
	      return texto;
	  }
	  
	  /**
	     * Captura a jogada com base nos pontos de início e fim e realiza validações detalhadas das regras.
	     * Repete o pedido até uma jogada em conformidade ser inserida.
	     */
	    private static short readMoveFromDots(Scanner leitor, int pontosLinhas, int pontosColunas) 
	    {
	        int totalDots = pontosLinhas * pontosColunas;
	        
	        while (true) 
	        {
	            String linhaInput = leitor.nextLine().trim();
	            
	            if (linhaInput.isEmpty()) 
	            {
	                continue;
	            }
	            
	            String[] partes = linhaInput.split("\\s+");
	            
	            if (partes.length != 2) 
	            {
	                System.out.println("Jogada inválida! Deve introduzir exatamente dois números (ponto de início e ponto de fim).");
	                System.out.print("Tente novamente: ");
	                continue;
	            }
	            
	            int p1, p2;
	            
	            try 
	            {
	                p1 = Integer.parseInt(partes[0]);
	                p2 = Integer.parseInt(partes[1]);
	            } 
	            catch (NumberFormatException e) 
	            {
	                System.out.println("Jogada inválida! Os pontos devem ser números inteiros.");
	                System.out.print("Tente novamente: ");
	                continue;
	            }
	            
	            if (p1 < 1 || p1 > totalDots || p2 < 1 || p2 > totalDots) 
	            {
	                System.out.println("Jogada inválida! Os pontos devem estar entre 1 e " + totalDots + ".");
	                System.out.print("Tente novamente: ");
	                continue;
	            }
	            
	            if (p1 == p2) 
	            {
	                System.out.println("Jogada inválida! O ponto de início não pode ser igual ao ponto de fim.");
	                System.out.print("Tente novamente: ");
	                continue;
	            }
	            
	            // Converter identificadores de pontos para coordenadas bidimensionais (0-indexed)
	            int r1 = (p1 - 1) / pontosColunas;
	            int c1 = (p1 - 1) % pontosColunas;
	            int r2 = (p2 - 1) / pontosColunas;
	            int c2 = (p2 - 1) % pontosColunas;
	            
	            // Validação de movimentos diagonais
	            if (r1 != r2 && c1 != c2) 
	            {
	                System.out.println("Jogada inválida! Não são permitidas jogadas na diagonal (Ex: 4 2).");
	                System.out.print("Tente novamente: ");
	                continue;
	            }
	            
	            // Validação de distância maior que 1 segmento de reta (ex: saltar múltiplos pontos)
	            if (Math.abs(r1 - r2) > 1 || Math.abs(c1 - c2) > 1) 
	            {
	                System.out.println("Jogada inválida! Não pode efetuar uma jogada com distância superior a 1 segmento (Ex: 8 10).");
	                System.out.print("Tente novamente: ");
	                continue;
	            }
	            
	            // Mapear par de coordenadas ordenadas para o ID sequencial de linha do servidor
	            short linhaJogo = convertDotsToLineNumber(r1, c1, r2, c2, pontosLinhas, pontosColunas);
	            
	            if (linhaJogo == -1) 
	            {
	                System.out.println("Jogada inválida! Erro ao mapear os pontos para uma linha válida.");
	                System.out.print("Tente novamente: ");
	                continue;
	            }
	            
	            return linhaJogo;
	        }
	    }

	    /**
	     * Traduz os pontos validados do cliente para o identificador numérico sequencial esperado pelo servidor.
	     */
	    private static short convertDotsToLineNumber(int r1, int c1, int r2, int c2, int pontosLinhas, int pontosColunas) 
	    {
	        int targetRow = -1;
	        int targetCol = -1;
	        boolean isHorizontal = false;

	        if (r1 == r2 && Math.abs(c1 - c2) == 1) 
	        {
	            targetRow = r1;
	            targetCol = Math.min(c1, c2);
	            isHorizontal = true;
	        } 
	        else if (c1 == c2 && Math.abs(r1 - r2) == 1) 
	        {
	            targetRow = Math.min(r1, r2);
	            targetCol = c1;
	            isHorizontal = false;
	        } 
	        else 
	        {
	            return -1;
	        }

	        short contador = 1;
	        
	        for (int i = 0; i < pontosLinhas; i++) 
	        {
	            for (int j = 0; j < pontosColunas - 1; j++) 
	            {
	                if (isHorizontal && i == targetRow && j == targetCol) 
	                {
	                    return contador;
	                }
	                contador++;
	            }
	            
	            if (i == pontosLinhas - 1) 
	            {
	                break;
	            }
	            
	            for (int j = 0; j < pontosColunas; j++) 
	            {
	                if (!isHorizontal && i == targetRow && j == targetCol) 
	                {
	                    return contador;
	                }
	                contador++;
	            }
	        }
	        
	        return -1;
	    }
                                  
    /**
     * Método principal do programa ClienteTCP.
     *
     * @param args argumentos da linha de comando: host e porta
     */
	  public static void main(String[] args) {
        // Lê os argumentos da linha de comando (se existirem).
        if (args != null && args.length == 2) {
            host = args[0];
            port = Integer.parseInt(args[1]);
        }  
        if(leitor == null) 
            leitor = new Scanner(System.in);
        
        try (   
            // Tenta criar um socket para se conectar ao servidor.
            Socket socket = new Socket(host, port);
            // Cria um adaptador para comunicar com o servidor. (Usa letra minúscula para a variável!)
            Stub stub = new Stub(socket)) {

            // Mostra informações sobre a ligação.
            System.out.println("Cliente -> Ligação estabelecida: " + socket);
            
            System.out.println("1 - Login | 2 - Criar Conta");
            int opcao = leitor.nextInt();
            leitor.nextLine(); // Limpa o 'Enter' que ficou pendurado do nextInt()!

            char simbolo = ' '; // Variável para guardar o 'X' ou 'O'

            if (opcao == 2) {
                // --- FLUXO DE REGISTO ---
                System.out.println("<<< ***** CRIAR NOVA CONTA ***** >>>");
                
                System.out.print("Indique o seu nickname: ");
                String nick = leitor.nextLine();
                
                String pass = leSenha("Indique a sua senha: ", leitor);
                
                System.out.print("Nacionalidade (Ex: PT, FR, UK): ");
                String nac = leitor.nextLine();
                
                System.out.print("Idade: ");
                int idade = leitor.nextInt();
                leitor.nextLine(); // Limpar o 'Enter'

                // NOVA LÓGICA PARA A FOTOGRAFIA:
                System.out.print("Indique o caminho COMPLETO para a sua fotografia");
                System.out.print("(Ex: C:\\Users\\O_Teu_Nome\\Desktop\\foto.jpg ou /Users/Nome/foto.jpg): ");
                String caminhoFoto = normalizarCaminhoFoto(leitor.nextLine());

                // Usamos a classe do professor para ler a imagem e gerar a Base64
                util.MyImage img = new util.MyImage(caminhoFoto);
                if (!img.isOk()) {
                    System.out.println("Erro: Não foi possível ler a imagem! A abortar...");
                    return;
                }
                String foto = img.getBase64(); // A string gigante é gerada aqui de forma segura!

                // Agora sim, chamamos o registo
                stub.registar(nick, pass, foto, nac, idade);
                System.out.println("A colocar-te no Menu Principal...");

            } else if (opcao == 1) {
                // --- FLUXO DE LOGIN ---
                System.out.println("<<< ***** LOGIN ***** >>>");
                System.out.println("Utilizadores existentes para teste:");
                System.out.println("cartwheel:p1; milkshake:p2; gandalf:p4; opera:p5; smoke:p9; bagel:p10");
                
                System.out.print("Indique o seu nome de utilizador: ");
                String nome = leitor.nextLine();
                
                String senha = leSenha("Indique a sua senha: ", leitor);

                // Validação imediata das credenciais.
                stub.iniciar(nome, senha);
            } else {
                System.out.println("Opção inválida! A fechar o jogo...");
                return;
            }
            
            // --- GRANDE CICLO DE SESSÃO (MENU <-> JOGO) ---
            for (;;) {
                System.out.println();
                System.out.println("===== MENU =====");
                System.out.println("1 - Entrar em Jogo");
                System.out.println("2 - Definições de Perfil (Alterar fotografia)");
                System.out.println("0 - Sair");
                System.out.print("Opção: ");

                int opcaoMenu = leitor.nextInt();
                leitor.nextLine();

                if (opcaoMenu == 1) {
                    System.out.println("A colocar-te na fila de espera global...");
                    simbolo = stub.entrarFila();
                    
                    System.out.println("Foi-lhe atribuído o identificador de jogador: " + simbolo);
                    if(simbolo == 'O') {
                        System.out.println("À espera que o oponente jogue...");
                    }
                    
                    // Loop do jogo, enquanto não for o fim do jogo
                    for(;;) 
                    {
                        // Mostra o tabuleiro atual.
                        Element tab = stub.obter();
                        System.out.println(stub.tabuleiroPontosCaixasToTXT(tab));
                        
                        String estado = tab.getAttribute("estado");
                        if(!estado.equals("ND")) 
                        {
                            // Mostra o estado do jogo após a jogada.
                            System.out.println(stub.estadoToTXT(estado));
                            // O loop só deve quebrar se o jogo terminar (VX, VO, EM)
                            if(!estado.equals("IV") && !estado.equals("BN"))
                            {
                                break;
                            }
                        }
                        
                        // Pede ao jogador para fazer uma jogada informando o ponto inicial e final.
                        System.out.print("Joga " + simbolo + " - Introduza o ponto inicial e final (ex: 1 2): ");
                        short jogada = readMoveFromDots(leitor, 3, 3);
                        
                        // Envia jogada para o servidor.
                        stub.jogar(jogada);
                    }
                    System.out.println("\n--- A partida terminou! ---");
                    System.out.println("Vais ser devolvido ao Lobby Principal.");
                }
                else if (opcaoMenu == 2) {
                    System.out.print("Indique o caminho COMPLETO para a nova fotografia: ");
                    String caminhoFoto = normalizarCaminhoFoto(leitor.nextLine());

                    util.MyImage img = new util.MyImage(caminhoFoto);
                    if (!img.isOk()) {
                        System.out.println("Erro: Não foi possível ler a imagem.");
                        continue;
                    }

                    // Usa o próprio Socket autenticado para atualizar (já não cria um novo Socket cego!)
                    stub.atualizarPerfil("", img.getBase64());
                    System.out.println("Fotografia de perfil atualizada com sucesso.");
                }
                else if (opcaoMenu == 0) {
                    System.out.println("A terminar sessão...");
                    return;
                } else {
                    System.out.println("Opção inválida.");
                }
            }

        } catch (Exception e) {
            System.err.println("Ligação terminada ou Erro: " + e.getLocalizedMessage());
        } 
        System.out.println("Jogador: terminou a execução!");
    }
}
