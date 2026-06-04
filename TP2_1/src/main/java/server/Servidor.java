package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 🕹️ Classe Servidor: Gere um jogo do galo multi-jogador usando TCP.
 * Atua como um "lobby" que emparelha jogadores e lança instâncias dedicadas.
 * * @author Engº Porfírio Filipe
 * */
public class Servidor {

    // 🔌 Porto por omissão onde o servidor "escuta" novas ligações
    public final static int DEFAULT_PORT = 5025;

    // ⏱️ Tempo máximo de espera antes de o servidor se desligar por inatividade
    private static int timeout = 0; 

    // 🏁 Se true, o servidor fecha após terminar o primeiro jogo
    private static boolean single = false;

    // 🧱 Referência global à fila para devolver os jogadores ao Lobby após os jogos
    private static FIFOJogador filaGlobal = null;

    /**
     * 🚀 Ponto de entrada do sistema.
     */
    public static void main(String[] args) {
        // Garantir que a aplicação Servidor grava na mesma pasta do Tomcat!
        util.XMLDoc.setContextoReal("src/main/webapp/");
        
        int port = DEFAULT_PORT;

        // 📝 Processamento de argumentos da linha de comandos
        if (args.length >= 1) port = Integer.parseInt(args[0]);
        if (args.length >= 2) single = args[1].equalsIgnoreCase("S");
        if (args.length >= 3) timeout = Integer.parseInt(args[2]);

        // 📢 Logs de inicialização para o administrador do sistema
        System.out.println(single ? "⚠️ Modo: Jogo Único" : "🔄 Modo: Multi-Jogo");
        
        // 📥 Criação da fila (FIFO) que gere os jogadores em espera
        filaGlobal = new Servidor().new FIFOJogador();
        FIFOJogador fIFOJogador = filaGlobal;

        /**
         * 🏗️ TAREFA DE EMPARELHAMENTO (Matchmaking)
         * Esta Thread corre em background para casar jogadores 2 a 2.
         */
        new Thread(() -> { 
            for(;;) { 
                Socket sk1 = null;
                Socket sk2 = null;
                try {
                    // 🛑 BLOQUEANTE: Espera que o Jogador 1 entre na fila
                    sk1 = fIFOJogador.remove();
                    // 🛑 BLOQUEANTE: Espera que o Jogador 2 entre na fila
                    sk2 = fIFOJogador.remove();
                    
                    System.out.println("🤝 Par encontrado! A iniciar Servidor Dedicado...");
                    
                    // 🏎️ Lança uma thread separada para gerir a lógica deste jogo específico
                    Thread jogo = new ServidorDedicado(sk1, sk2);
                    jogo.start(); 

                    // 🛑 Se for modo Single, espera o jogo acabar e encerra o programa
                    if (single) { 
                        try { jogo.join(); } catch (InterruptedException e) {}
                        System.out.println("👋 Modo single-game terminado. A sair...");
                        System.exit(0);
                    }    
                } catch (InterruptedException e) {
                    System.out.println("❌ Erro na tarefa de gestão de fila.");
                }
            }
        }).start();

        /**
         * 👂 CICLO DE ACEITAÇÃO (Socket Server)
         * Responsável por aceitar novas ligações físicas dos clientes.
         */
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("🌍 Servidor TCP à escuta no porto: " + port);
            
            while (true) {
                System.out.println("⏳ Aguardando nova ligação...");
                
                // 🕒 Define quanto tempo o servidor espera por um cliente antes de dar erro
                serverSocket.setSoTimeout(timeout); 
                
                // 📞 Aceita a ligação do socket do cliente
                Socket newSock = serverSocket.accept();
                System.out.println("✅ Ligação aceite: " + newSock.getInetAddress());

                // ➕ Tenta adicionar o jogador à fila de espera
                try {
                    fIFOJogador.add(newSock);
                } catch (InterruptedException e) {
                    System.out.println("❌ Erro ao colocar jogador na fila.");
                }    
            }
        } catch (IOException e) {
            System.err.println("🚨 Erro crítico no Servidor: " + e.getLocalizedMessage());
        }
    }

    // 🌟 Mapas para gestão de convites privados
    public static final java.util.Map<String, String> convitesPendentes = new java.util.concurrent.ConcurrentHashMap<>();
    public static final java.util.Map<String, Socket> esperaVIP = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 🧵 Classe interna para gerir a fila de jogadores (First-In, First-Out).
     * Usa uma BlockingQueue para garantir segurança entre threads (Thread-Safe).
     */
    private final class FIFOJogador {
        // 🧱 Fila que bloqueia a leitura se estiver vazia e a escrita se estiver cheia
        private final BlockingQueue<Socket> queue = new LinkedBlockingQueue<>();
        
        // Alterado para iniciar com 'X' em vez de '1'
        private char proximoSimbolo = 'X';

        /**
         * 📥 Adiciona um jogador à fila e envia-lhe o seu símbolo.
         */
        public synchronized void add(Socket element) throws InterruptedException {
            // Lança uma tarefa para o Lobby do jogador
            new Thread(() -> {
                try {
                    while (true) {
                        int acao = Skeleton.runLobby(element);
                        
                        if (acao == 1) { // Entrar na fila publica
                            String username = Skeleton.obterSocketUtilizador(element);
                            
                            // 🌟 INTERCEÇÃO PARA JOGADORES DE CONSOLA OU ACEITAÇÕES 🌟
                            // Se este jogador tiver um convite pendente para ele, intercetamos!
                            if (username != null && convitesPendentes.containsKey(username)) {
                                String inviter = convitesPendentes.remove(username);
                                Socket inviterSocket = esperaVIP.remove(inviter);
                                
                                if (inviterSocket != null && !inviterSocket.isClosed()) {
                                    System.out.println("🎉 Convite aceite implicitamente/explicitamente por: " + username);
                                    
                                    // Associa os símbolos e envia respostas
                                    Skeleton.sendEntrarFilaResponse(inviterSocket, 'X');
                                    Skeleton.sendEntrarFilaResponse(element, 'O');
                                    
                                    // Arranca logo com a thread dedicada!
                                    ServidorDedicado sd = new ServidorDedicado(inviterSocket, element);
                                    sd.start();
                                    break; // Sai do lobby
                                }
                            }

                            char atribuido = proximoSimbolo;
                            Skeleton.sendEntrarFilaResponse(element, atribuido);
                            queue.put(element);
                            proximoSimbolo = (atribuido == 'X' ? 'O' : 'X');
                            break; 
                        } 
                        else if (acao == 2) { // Desafiar privado (Entra na fila VIP)
                            String username = Skeleton.obterSocketUtilizador(element);
                            esperaVIP.put(username, element);
                            
                            // Lança uma thread para vigiar se este socket envia cancelar!
                            new Thread(() -> {
                                try {
                                    BufferedReader is = new java.io.BufferedReader(new java.io.InputStreamReader(element.getInputStream()));
                                    while (esperaVIP.containsKey(username)) {
                                        if (is.ready()) {
                                            String linha = is.readLine();
                                            if (linha != null && linha.contains("cancelar_desafio")) {
                                                System.out.println("🚫 Jogador " + username + " cancelou o convite.");
                                                esperaVIP.remove(username);
                                                // Remove qualquer convite que ele tenha feito
                                                convitesPendentes.values().removeIf(val -> val.equals(username));
                                                
                                                java.io.PrintWriter os = new java.io.PrintWriter(element.getOutputStream(), true);
                                                os.println("<metodo><cancelar_desafio/></metodo>");
                                                
                                                // Devolve ao Lobby!
                                                add(element);
                                                break;
                                            }
                                        }
                                        Thread.sleep(200);
                                    }
                                } catch (Exception e) {}
                            }).start();

                            break; // Sai da thread do Lobby normal
                        }
                    }
                } catch (Exception e) {
                    System.out.println("⚠️ Jogador saiu do lobby: " + e.getMessage());
                    Skeleton.limparSocketUtilizador(element);
                    try { element.close(); } catch (IOException e1) {}
                }
            }).start();
        }

        /**
         * 📤 Retira um jogador da fila. 
         * @return O Socket do jogador. Se a fila estiver vazia, a thread "dorme" aqui.
         */
        public Socket remove() throws InterruptedException {
            return queue.take(); // 🛑 Método estritamente bloqueante
        }
    }

    /**
     * ♻️ Devolve o socket do jogador ao Lobby após o fim de uma partida.
     */
    public static void devolverAoLobby(Socket sk) {
        if (filaGlobal != null && sk != null) {
            try {
                filaGlobal.add(sk);
            } catch (InterruptedException e) { }
        }
    }
}
