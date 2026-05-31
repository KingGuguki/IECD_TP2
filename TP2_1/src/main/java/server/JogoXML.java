package server;

/**
 * Classe que implementa o adaptador do jogo para XML.
 *
 * @author Engº Porfírio Filipe
 */
public class JogoXML extends Jogo 
{

    /**
     * Estado do jogo após a última jogada.
     */
    private String estado = "ND";

    /**
     * Converte o tabuleiro do jogo para XML e inclui o estado.
     *
     * @return String com XML que representa o tabuleiro.
     */
    public String tabuleiroToXML() 
    {
        String tab = "<tabuleiro estado='" + estado + "' linhas='" + pontosLinhas + "' colunas='" + pontosColunas + "' >";
        
        for (int i = 0; i < pontosLinhas; i++) 
        {
            for (int j = 0; j < pontosColunas - 1; j++) 
            {
                tab += "<linha tipo='H' linha='" + i + "' coluna='" + j + "' ocupada='" + linhasHorizontais[i][j] + "'/>";
            }
        }
        for (int i = 0; i < pontosLinhas - 1; i++) 
        {
            for (int j = 0; j < pontosColunas; j++) 
            {
                tab += "<linha tipo='V' linha='" + i + "' coluna='" + j + "' ocupada='" + linhasVerticais[i][j] + "'/>";
            }
        }
        for (int i = 0; i < pontosLinhas - 1; i++) 
        {
            for (int j = 0; j < pontosColunas - 1; j++) 
            {
                tab += "<caixa linha='" + i + "' coluna='" + j + "' dono='" + caixas[i][j] + "'/>";
            }
        }

        tab += "</tabuleiro>";

        if (estado.equals("BN") || estado.equals("IV")) 
        {
            estado = "ND";
        }

        return tab;
    }

    /**
     * Concretiza a jogada e atualiza o estado do jogo.
     */
	public boolean joga(short numero, char simbolo) 
	{
		estado = "ND"; 

		if (!super.joga(numero, simbolo)) 
		{
			estado = "IV"; 
			return false;
		}
		
        if (super.ultimaJogadaFechouCaixa()) 
        {
            if (super.terminou()) 
            {
                definirVencedor();
            } 
            else 
            {
                estado = "BN";
            }
        } 
        else 
        {
            if (super.terminou()) 
            {
                definirVencedor();
            }
        }

		return true;
	}

	private void definirVencedor() 
	{
	    if (super.empate()) 
	    {
	        estado = "EM";
	    } 
	    else if (super.vitoria('X')) 
	    {
	        estado = "VX";
	    } 
	    else if (super.vitoria('O')) 
	    {
	        estado = "VO";
	    }
	}

    @Override
    public boolean terminou() 
    {
        return super.terminou();
    }

    public String getEstado() 
    {
        return estado;
    }
}