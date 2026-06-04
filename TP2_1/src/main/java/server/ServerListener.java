package server;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/**
 * Listener global que arranca o motor de Jogo TCP mal a Aplicação Web inicie.
 */
@WebListener
public class ServerListener implements ServletContextListener {
    private Thread servidorThread;
    
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("[ServerListener] A iniciar o Motor de Jogo TCP na porta 5025...");
        servidorThread = new Thread(() -> {
            try {
                // Inicia o Servidor (bloqueante) na sua própria thread
                Servidor.main(new String[0]);
            } catch (Exception e) {
                System.err.println("[ServerListener] Falha ao arrancar o Servidor TCP: " + e.getMessage());
                e.printStackTrace();
            }
        });
        
        // setDaemon(true) garante que a Thread morre imediatamente se o Tomcat desligar.
        servidorThread.setDaemon(true); 
        servidorThread.start();
    }
    
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("[ServerListener] A encerrar o Motor de Jogo TCP...");
        if (servidorThread != null) {
            servidorThread.interrupt();
        }
    }
}
