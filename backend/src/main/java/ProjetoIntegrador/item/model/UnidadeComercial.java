package ProjetoIntegrador.item.model;
public enum UnidadeComercial {
    USUARIO, LICENCA, ENDPOINT, SERVIDOR, VM, GB, TB, CAIXA_POSTAL, DOMINIO, HORA, UNIDADE;
    public int casasDecimais() { return this==GB || this==TB || this==HORA ? 4 : 0; }
}
