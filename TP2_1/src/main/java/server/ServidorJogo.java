package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import user.User;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Classe `Accept` representa uma thread responsável por tratar a
 * comunicação entre dois jogadores que interagem durante um jogo do galo.
 * 
 * Esta classe é responsável por:
 * * Gerir a comunicação entre os jogadores X e O.
 * * Ler e escrever dados nos sockets dos jogadores.
 * * Controlar o fluxo do jogo, alternando entre jogadores.
 * * Fechar os sockets quando o jogo termina.
 * 
 * @author Engº Porfírio Filipe
 * 
 */
class ServidorDedicado extends Thread {

    // **Atributos:**
	
	// Tempo máximo para jogar
	final int timeout = 1000*30;  // 0 - sem timeout

    /**
     * Socket da ligação com o jogador X.
     */
    private Socket connectionX = null; 

    /**
     * Socket da ligação com o jogador O.
     */
    private Socket connectionO = null; 

    // **Construtor:**

    /**
     * Manipula os circuitos virtuais dos jogadores.
     *
     * @param connection1 Socket do jogador X.
     * @param connection2 Socket do jogador O.
     */
    public ServidorDedicado(Socket connection1, Socket connection2) {
        this.connectionX = connection1;
        this.connectionO = connection2;
    }

    private void atualizarEstatisticasFimJogo(JogoXML jogo) throws Exception {
        String userX = Skeleton.obterSocketUtilizador(connectionX);
        String userO = Skeleton.obterSocketUtilizador(connectionO);

        if (userX == null || userO == null) {
            return;
        }

        if (jogo.empate()) {
            return;
        }

        if ("VX".equals(jogo.getEstado()) || jogo.vitoria('X')) {
            User.registarResultadoJogo(userX, userO);
        } else if ("VO".equals(jogo.getEstado()) || jogo.vitoria('O')) {
            User.registarResultadoJogo(userO, userX);
        }
    }

    /**
     * Método executado pela thread para gerir um jogo.
     * 
     * Este método é responsável por:
     * * Criar streams de leitura e escrita para os sockets.
     * * Iniciar o jogo.
     * * Gerir o ciclo de jogadas entre os jogadores.
     * * Fechar os sockets quando o jogo termina.
     */
    public void run() {

        try (
            // Cria streams para leitura e escrita de dados nos sockets
        	
        	// **Socket X:**
            // Stream para ler dados do socket X.
            BufferedReader isX = new BufferedReader(new InputStreamReader(connectionX.getInputStream()));
            // Stream para escrever dados no socket X.
            PrintWriter osX = new PrintWriter(connectionX.getOutputStream(), true);
        	
        	// **Socket O:**
            // Stream para ler dados do socket O.
            BufferedReader isO = new BufferedReader(new InputStreamReader(connectionO.getInputStream()));
            // Stream para escrever dados no socket X.
            PrintWriter osO = new PrintWriter(connectionO.getOutputStream(), true);
        ) {
        	// Define timeout para inatvidade
        	connectionX.setSoTimeout(timeout);
        	connectionO.setSoTimeout(timeout);
        	
            // **Informação sobre a thread**

            System.out.println("Iniciou a Thread ("+ this.threadId()+") do servidor dedicado:");

            // **Criação do jogo**

         // Cria uma nova instância do jogo.
            JogoXML jogo = new JogoXML();
            char turnoAtual = 'X';

            // Ciclo para gerir a interação entre jogadores suportando a jogada Bónus
            try {
                for (;;) 
                {
                    if (turnoAtual == 'X') 
                    {
                        // Ciclo para permitir múltiplos obter antes do jogar
                        while (true) {
                            Document docX = Skeleton.getNext(isX);
                            if (docX.getElementsByTagName("obter").getLength() > 0) {
                                // Responde ao obter manualmente
                                osX.println("<metodo><obter>" + jogo.tabuleiroToXML() + "</obter></metodo>");
                            } else if (docX.getElementsByTagName("jogar").getLength() > 0) {
                                // Extrai e executa a jogada
                                Element jogada = (Element) docX.getElementsByTagName("jogar").item(0);
                                short jogadaNum = Short.parseShort(jogada.getAttribute("jogada"));
                                jogo.joga(jogadaNum, 'X');
                                osX.println("<metodo><jogar>" + jogo.tabuleiroToXML() + "</jogar></metodo>");
                                break;
                            } else {
                                throw new Exception("Comando inválido esperado: obter ou jogar");
                            }
                        }
                        
                        if (!jogo.terminou()) 
                        {
                            if (jogo.getEstado().equals("ND")) {
                                turnoAtual = 'O';
                            }
                        } 
                        else 
                        {
                            break;
                        }
                    } 
                    else 
                    {
                        while (true) {
                            Document docO = Skeleton.getNext(isO);
                            if (docO.getElementsByTagName("obter").getLength() > 0) {
                                osO.println("<metodo><obter>" + jogo.tabuleiroToXML() + "</obter></metodo>");
                            } else if (docO.getElementsByTagName("jogar").getLength() > 0) {
                                Element jogada = (Element) docO.getElementsByTagName("jogar").item(0);
                                short jogadaNum = Short.parseShort(jogada.getAttribute("jogada"));
                                jogo.joga(jogadaNum, 'O');
                                osO.println("<metodo><jogar>" + jogo.tabuleiroToXML() + "</jogar></metodo>");
                                break;
                            } else {
                                throw new Exception("Comando inválido esperado: obter ou jogar");
                            }
                        }
                        
                        if (!jogo.terminou()) 
                        {
                            if (jogo.getEstado().equals("ND")) {
                                turnoAtual = 'X';
                            }
                        } 
                        else 
                        {
                            break;
                        }
                    }
                } // Fim do for (;;)
                atualizarEstatisticasFimJogo(jogo);
            } catch (java.net.SocketTimeoutException timeoutEx) {
                System.out.println("Servidor dedicado: Timeout atingido por inatividade de um jogador (" + turnoAtual + ")!");
                if (turnoAtual == 'X') {
                    jogo.setEstado("VO"); // Vitória do O porque o X adormeceu
                    Skeleton.runNotificarTimeout(osO, jogo);
                } else {
                    jogo.setEstado("VX"); // Vitória do X porque o O adormeceu
                    Skeleton.runNotificarTimeout(osX, jogo);
                }
                atualizarEstatisticasFimJogo(jogo);
            } catch (Exception e) {
                System.out.println("⚠️ Jogo interrompido: " + e.getMessage());
            }
		} catch (Exception e) {
			System.out.println("Servidor dedicado: terminou o jogo ("+e.getMessage()+")!");
			// e.printStackTrace();
		} finally {
            // Em vez de descartar as ligações e forçar logout, devolvemo-las à escuta do Lobby!
            // Isto preserva a sessão de ambos os jogadores intacta, mantendo-os logados.
            Servidor.devolverAoLobby(connectionX);
            Servidor.devolverAoLobby(connectionO);
		}
		System.out.println("Servidor dedicado: terminou a Thread ("+ this.threadId()+") do servidor dedicado!");
	} // fim run
} // end Servidor Dedicado
