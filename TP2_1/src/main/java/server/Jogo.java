package server;

/**
 * Classe que implementa as regras do jogo Pontos e Caixas.
 * O tabuleiro é uma grelha de pontos conectados por linhas horizontais
 *
 * @author Engº Porfírio Filipe
 */
public class Jogo {

	protected final int pontosLinhas;
	protected final int pontosColunas;
	protected final boolean[][] linhasHorizontais;
	protected final boolean[][] linhasVerticais;
	protected final char[][] caixas;

	protected final int totalLinhas;
	protected final int totalCaixas;
	protected int linhasMarcadas = 0;
	
    // Alterado de caixas1/caixas2 para caixasX/caixasO
	protected int caixasX = 0;
	protected int caixasO = 0;
	protected int ultimasCaixasFechadas = 0;

	/**
	 * Cria um jogo de Pontos e Caixas com 3x3 pontos (2x2 caixas).
	 */
	public Jogo() {
		this(3, 3);
	}

	/**
	 * Cria um jogo de Pontos e Caixas com a grelha especificada.
	 *
	 * @param pontosLinhas Número de pontos nas linhas.
	 * @param pontosColunas Número de pontos nas colunas.
	 */
	public Jogo(final int pontosLinhas, final int pontosColunas) {
		if (pontosLinhas < 2 || pontosColunas < 2) {
			throw new IllegalArgumentException("O tabuleiro deve ter pelo menos 2 pontos em cada dimensão.");
		}

		this.pontosLinhas = pontosLinhas;
		this.pontosColunas = pontosColunas;
		this.linhasHorizontais = new boolean[pontosLinhas][pontosColunas - 1];
		this.linhasVerticais = new boolean[pontosLinhas - 1][pontosColunas];
		this.caixas = new char[pontosLinhas - 1][pontosColunas - 1];

		for (int i = 0; i < pontosLinhas - 1; i++) {
			for (int j = 0; j < pontosColunas - 1; j++) {
				caixas[i][j] = ' ';
			}
		}

		this.totalLinhas = pontosLinhas * (pontosColunas - 1) + (pontosLinhas - 1) * pontosColunas;
		this.totalCaixas = (pontosLinhas - 1) * (pontosColunas - 1);
	}

	/**
	 * Obtém o número de pontos nas linhas.
	 */
	public int getPontosLinhas() {
		return pontosLinhas;
	}

	/**
	 * Obtém o número de pontos nas colunas.
	 */
	public int getPontosColunas() {
		return pontosColunas;
	}

	/**
	 * Obtém o total de linhas possíveis.
	 */
	public int getTotalLinhas() {
		return totalLinhas;
	}

	/**
	 * Obtém o total de caixas do tabuleiro.
	 */
	public int getTotalCaixas() {
		return totalCaixas;
	}

	/**
	 * Indica quantas caixas foram fechadas na última jogada.
	 */
	public int getUltimasCaixasFechadas() {
		return ultimasCaixasFechadas;
	}

	/**
	 * Indica se a última jogada fechou pelo menos uma caixa.
	 */
	public boolean ultimaJogadaFechouCaixa() {
		return ultimasCaixasFechadas > 0;
	}

	/**
	 * Obtem o número de caixas conquistadas pelo jogador.
	 */
	public int getScore(final char simbolo) 
	{
	    if (simbolo == 'X') return caixasX;
	    if (simbolo == 'O') return caixasO;
	    return 0;
	}
	/**
	 * Indica se o jogo terminou: não há mais linhas disponíveis.
	 */
	public boolean terminou() {
		return linhasMarcadas >= totalLinhas;
	}

	/**
	 * Indica se o jogo terminou empatado.
	 */
	public boolean empate() 
	{
	    return terminou() && caixasX == caixasO;
	}

	/**
	 * Indica se o jogador com o símbolo especificado venceu o jogo.
	 */
	public boolean vitoria(char simbolo) 
	{
	    if (!terminou()) 
	    {
	        return false;
	    }
	    if (simbolo == 'X') 
	    {
	        return caixasX > caixasO;
	    }
	    if (simbolo == 'O') 
	    {
	        return caixasO > caixasX;
	    }
	    return false;
	}

	/**
	 * Concretiza a jogada e atualiza o estado do jogo.
	 *
	 * @param numero Identificador da linha.
	 * @param simbolo Símbolo do jogador ('X' ou 'O').
	 * @return true se a jogada é válida, false caso contrário.
	 */
	public boolean joga(short numero, char simbolo) 
	{
		if (numero < 1 || numero > totalLinhas) 
		{
			return false;
		}

		int linha = 0;
		int coluna = 0;
		boolean horizontal = false;
		int contador = 1;
		boolean encontrado = false;

		for (int i = 0; i < pontosLinhas; i++) 
		{
			for (int j = 0; j < pontosColunas - 1; j++) 
			{
				if (contador == numero) 
				{
					linha = i;
					coluna = j;
					horizontal = true;
					encontrado = true;
					break;
				}
				contador++;
			}
			if (encontrado || i == pontosLinhas - 1) 
			{
				break;
			}
			for (int j = 0; j < pontosColunas; j++) 
			{
				if (contador == numero) 
				{
					linha = i;
					coluna = j;
					horizontal = false;
					encontrado = true;
					break;
				}
				contador++;
			}
			if (encontrado) 
			{
				break;
			}
		}

		int caixasFechadas = 0;

		if (horizontal) 
		{
			if (linhasHorizontais[linha][coluna]) 
			{
				return false;
			}
			linhasHorizontais[linha][coluna] = true;
			caixasFechadas = fecharCaixasPorLinhaHorizontal(linha, coluna, simbolo);
		} 
		else 
		{
			if (linhasVerticais[linha][coluna]) 
			{
				return false;
			}
			linhasVerticais[linha][coluna] = true;
			caixasFechadas = fecharCaixasPorLinhaVertical(linha, coluna, simbolo);
		}

		linhasMarcadas++;
		ultimasCaixasFechadas = caixasFechadas;

		if (simbolo == 'X') 
		{
			caixasX += caixasFechadas;
		} 
		else if (simbolo == 'O') 
		{
			caixasO += caixasFechadas;
		}

		return true;
	}

	private int fecharCaixasPorLinhaHorizontal(int linha, int coluna, char simbolo) {
		int fechadas = 0;
		if (linha > 0) {
			fechadas += fecharCaixa(linha - 1, coluna, simbolo);
		}
		if (linha < pontosLinhas - 1) {
			fechadas += fecharCaixa(linha, coluna, simbolo);
		}
		return fechadas;
	}

	private int fecharCaixasPorLinhaVertical(int linha, int coluna, char simbolo) {
		int fechadas = 0;
		if (coluna > 0) {
			fechadas += fecharCaixa(linha, coluna - 1, simbolo);
		}
		if (coluna < pontosColunas - 1) {
			fechadas += fecharCaixa(linha, coluna, simbolo);
		}
		return fechadas;
	}

	private int fecharCaixa(int linha, int coluna, char simbolo) {
		if (caixas[linha][coluna] != ' ') {
			return 0;
		}

		boolean topo = linhasHorizontais[linha][coluna];
		boolean fundo = linhasHorizontais[linha + 1][coluna];
		boolean esquerda = linhasVerticais[linha][coluna];
		boolean direita = linhasVerticais[linha][coluna + 1];

		if (topo && fundo && esquerda && direita) {
			caixas[linha][coluna] = simbolo;
			return 1;
		}
		return 0;
	}
}