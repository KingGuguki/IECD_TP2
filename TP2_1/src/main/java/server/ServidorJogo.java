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

    private void atualizarEstatisticasFimJogo(JogoXML jogo, long tempoPensamentoX, long tempoPensamentoO) throws Exception {
        String userX = Skeleton.obterSocketUtilizador(connectionX);
        String userO = Skeleton.obterSocketUtilizador(connectionO);

        if (userX == null || userO == null) {
            return;
        }

        if (jogo.empate()) {
            User.registarResultadoJogo(userX, userO, null, true, tempoPensamentoX, tempoPensamentoO);
            return;
        }

        if ("VX".equals(jogo.getEstado()) || jogo.vitoria('X')) {
            User.registarResultadoJogo(userX, userO, userX, false, tempoPensamentoX, tempoPensamentoO);
        } else if ("VO".equals(jogo.getEstado()) || jogo.vitoria('O')) {
            User.registarResultadoJogo(userX, userO, userO, false, tempoPensamentoX, tempoPensamentoO);
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
        long inicioJogo = System.currentTimeMillis();
        long tempoTurno = inicioJogo;
        long tempoPensamentoX = 0;
        long tempoPensamentoO = 0;

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
            String userX = Skeleton.obterSocketUtilizador(connectionX);
            String userO = Skeleton.obterSocketUtilizador(connectionO);
            User uX = userX != null ? User._obtain(userX) : null;
            User uO = userO != null ? User._obtain(userO) : null;
            String corX = (uX != null && uX.getCorFundo() != null) ? uX.getCorFundo() : "#3b82f6";
            String corO = (uO != null && uO.getCorFundo() != null) ? uO.getCorFundo() : "#f43f5e";

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
                            Document docX = Skeleton.getNext(isX, connectionX);
                            if (docX.getElementsByTagName("obter").getLength() > 0) {
                                // Responde ao obter manualmente
                                Skeleton.printAndLog(osX, connectionX, "<metodo><obter>" + jogo.tabuleiroToXML(turnoAtual, corX, corO) + "</obter></metodo>");
                            } else if (docX.getElementsByTagName("jogar").getLength() > 0) {
                                // Extrai e executa a jogada
                                Element jogada = (Element) docX.getElementsByTagName("jogar").item(0);
                                short jogadaNum = Short.parseShort(jogada.getAttribute("jogada"));
                                
                                long agora = System.currentTimeMillis();
                                tempoPensamentoX += (agora - tempoTurno);
                                tempoTurno = agora;
                                
                                jogo.joga(jogadaNum, 'X');
                                char proximoTurno = turnoAtual;
                                if (!jogo.terminou() && jogo.getEstado().equals("ND")) {
                                    proximoTurno = 'O';
                                }
                                Skeleton.printAndLog(osX, connectionX, "<metodo><jogar>" + jogo.tabuleiroToXML(proximoTurno, corX, corO) + "</jogar></metodo>");
                                
                                turnoAtual = proximoTurno;
                                
                                // Se o X teve bónus, ele continua a jogar. Mas o Oponente (O) que está à espera no obter precisa de receber o tabuleiro atualizado!
                                if (jogo.terminou()) {
                                    // Notifica o O que o jogo acabou (pois foi o X a jogar)
                                    if (isO.ready()) Skeleton.getNext(isO, connectionO);
                                    Skeleton.printAndLog(osO, connectionO, "<metodo><obter>" + jogo.tabuleiroToXML(turnoAtual, corX, corO) + "</obter></metodo>");
                                    break;
                                } else if (turnoAtual == 'X') {
                                    // Responde ao O imediatamente para ele ver a caixa fechada sem ter de mudar o turno!
                                    if (isO.ready()) {
                                        Skeleton.getNext(isO, connectionO); // consome o pedido de obter pendente
                                    }
                                    Skeleton.printAndLog(osO, connectionO, "<metodo><obter>" + jogo.tabuleiroToXML(turnoAtual, corX, corO) + "</obter></metodo>");
                                }
                                break;
                            } else {
                                throw new Exception("Comando inválido esperado: obter ou jogar");
                            }
                        }
                        
                        if (jogo.terminou()) 
                        {
                            break;
                        }
                    } 
                    else 
                    {
                        while (true) {
                            Document docO = Skeleton.getNext(isO, connectionO);
                            if (docO.getElementsByTagName("obter").getLength() > 0) {
                                Skeleton.printAndLog(osO, connectionO, "<metodo><obter>" + jogo.tabuleiroToXML(turnoAtual, corX, corO) + "</obter></metodo>");
                            } else if (docO.getElementsByTagName("jogar").getLength() > 0) {
                                Element jogada = (Element) docO.getElementsByTagName("jogar").item(0);
                                short jogadaNum = Short.parseShort(jogada.getAttribute("jogada"));
                                
                                long agora = System.currentTimeMillis();
                                tempoPensamentoO += (agora - tempoTurno);
                                tempoTurno = agora;
                                
                                jogo.joga(jogadaNum, 'O');
                                char proximoTurno = turnoAtual;
                                if (!jogo.terminou() && jogo.getEstado().equals("ND")) {
                                    proximoTurno = 'X';
                                }
                                Skeleton.printAndLog(osO, connectionO, "<metodo><jogar>" + jogo.tabuleiroToXML(proximoTurno, corX, corO) + "</jogar></metodo>");
                                
                                turnoAtual = proximoTurno;
                                
                                if (jogo.terminou()) {
                                    // Notifica o X que o jogo acabou (pois foi o O a jogar)
                                    if (isX.ready()) Skeleton.getNext(isX, connectionX);
                                    Skeleton.printAndLog(osX, connectionX, "<metodo><obter>" + jogo.tabuleiroToXML(turnoAtual, corX, corO) + "</obter></metodo>");
                                    break;
                                } else if (turnoAtual == 'O') {
                                    // Responde ao X imediatamente para ele ver a caixa fechada sem ter de mudar o turno!
                                    if (isX.ready()) {
                                        Skeleton.getNext(isX, connectionX); // consome o pedido de obter pendente
                                    }
                                    Skeleton.printAndLog(osX, connectionX, "<metodo><obter>" + jogo.tabuleiroToXML(turnoAtual, corX, corO) + "</obter></metodo>");
                                }
                                break;
                            } else {
                                throw new Exception("Comando inválido esperado: obter ou jogar");
                            }
                        }
                    }
                } // Fim do for (;;)
                atualizarEstatisticasFimJogo(jogo, tempoPensamentoX / 1000, tempoPensamentoO / 1000);
            } catch (java.net.SocketTimeoutException timeoutEx) {
                System.out.println("Servidor dedicado: Timeout atingido por inatividade de um jogador (" + turnoAtual + ")!");
                
                long agora = System.currentTimeMillis();
                if (turnoAtual == 'X') {
                    tempoPensamentoX += (agora - tempoTurno);
                    jogo.setEstado("VO"); // Vitória do O porque o X adormeceu
                    Skeleton.runNotificarTimeout(osO, connectionO, jogo);
                    // Avisar o X também
                    if (isX.ready()) Skeleton.getNext(isX, connectionX);
                    Skeleton.printAndLog(osX, connectionX, "<metodo><obter>" + jogo.tabuleiroToXML() + "</obter></metodo>");
                } else {
                    tempoPensamentoO += (agora - tempoTurno);
                    jogo.setEstado("VX"); // Vitória do X porque o O adormeceu
                    Skeleton.runNotificarTimeout(osX, connectionX, jogo);
                    // Avisar o O também
                    if (isO.ready()) Skeleton.getNext(isO, connectionO);
                    Skeleton.printAndLog(osO, connectionO, "<metodo><obter>" + jogo.tabuleiroToXML() + "</obter></metodo>");
                }
                atualizarEstatisticasFimJogo(jogo, tempoPensamentoX / 1000, tempoPensamentoO / 1000);
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
	}
}
