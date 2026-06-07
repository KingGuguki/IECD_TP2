# 📌 Jogo Pontos e Caixas (Dots and Boxes) — IECD

Este repositório contém o projeto desenvolvido no âmbito da unidade curricular de **Infraestruturas Computacionais Distribuídas** (2ª Parte do Trabalho Prático — Verão 25/26) da Licenciatura em Engenharia Informática e Multimédia do **Instituto Superior de Engenharia de Lisboa (ISEL)**.

O objetivo principal desta fase consistiu na aplicação prática do conceito de **Interoperabilidade**, expandindo um motor de jogo central em Java (TCP) para suportar múltiplos clientes em simultâneo através de uma interface dinâmica na Web.

---

## 👥 Metadados do Projeto

* **Instituição:** Instituto Superior de Engenharia de Lisboa (ISEL)
* **Curso:** Licenciatura em Engenharia Informática e Multimédia (LEIM)
* **Unidade Curricular:** Infraestruturas Computacionais Distribuídas (IECD)
* **Docente:** Prof. Cedric Grueau
* **Turma:** 44D
* **Grupo:**
    * Filipe Mendes — nº 48628
    * Diogo Santos — nº 48626
    * João Olivier — nº 52560
* **Data de Entrega:** 7 de junho de 2026

---

## 🛠️ Stack Tecnológico & Restrições

Alinhado estritamente com as restrições impostas no enunciado da cadeira, o projeto foi desenvolvido sem recurso a bibliotecas ou *frameworks* externas:
* **Ambiente de Execução:** JDK 25 (Standard Edition)
* **Servidor de Aplicações:** Apache Tomcat 11
* **Tecnologias Web:** JavaServer Pages (JSP), Java Servlets, Vanilla JavaScript (AJAX / Fetch API), HTML5 e CSS3.
* **Persistência e Comunicação:** Documentos XML validados rigorosamente via esquemas XSD nativos.
* **Camada de Transporte:** Sockets TCP (Porto por omissão: `5025`).

---

## 🚀 Funcionalidades Implementadas

* **Arquitetura Thin Client:** Toda a lógica de jogo, validação de turnos, fecho de caixas e cálculo de pontuações reside exclusivamente no servidor central TCP.
* **Interoperabilidade Total:** Sucesso na comunicação cruzada, permitindo que um jogador na interface Web (Browser) jogue em tempo real contra um jogador na interface de Consola.
* **Pesquisa Dinâmica (AutoComplete):** Barra de pesquisa de oponentes para desafios privados com sugestões em tempo real via pedidos assíncronos ao servidor.
* **Personalização de Perfil:** Atualização visual da cor de fundo da aplicação Web guardada nas preferências do utilizador dentro do `users.xml`.
* **Gestão de Concorrência & Tempo:** Suporte a múltiplas partidas assíncronas em paralelo controladas por *Threads* dedicadas, com um *timeout* rígido de 30 segundos por jogada para evitar inatividade.
* **Hall of Fame Resiliente:** Quadro de honra ordenado por vitórias com critério de desempate baseado no *tempo médio de jogo*. Persistência baseada na escrita atómica do DOM para evitar corrupção de ficheiros.

---

## 💻 Como Compilar e Executar

### 1. Servidor Principal (Engine do Jogo)
1. Importe o projeto no Eclipse IDE.
2. Certifique-se de que o JDK 25 está configurado como a JRE de execução.
3. Execute a classe principal que inicia o servidor TCP (escutando no porto `5025`).

### 2. Cliente Web (Tomcat)
1. Configure o Apache Tomcat 11 no Eclipse.
2. Adicione o módulo web do projeto (`TP2`) ao servidor Tomcat.
3. Faça um *Clean* e *Publish* ao servidor.
4. Inicie o Tomcat e aceda através do browser em:
   ```text
   http://localhost:8080/TP2/login.jsp