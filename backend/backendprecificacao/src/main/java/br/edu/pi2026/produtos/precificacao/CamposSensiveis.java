package br.edu.pi2026.produtos.precificacao;

import java.util.Set;

/**
 * BE2-06 ("proteção dos campos financeiros"). Campos de rentabilidade
 * interna — custo, margem, markup e comissão — só aparecem pra quem tem
 * produtos.precificacao.ver_valores_sensiveis, além de produtos.precificacao.ver.
 * Preço e impostos ficam visíveis com a permissão básica, porque quem cota
 * um item pro cliente (ex.: um vendedor) precisa deles sem enxergar
 * rentabilidade interna.
 *
 * PROPOSTA — o Modelo de Dados do Grupo 2 já prevê que o perfil TÉCNICO "não
 * vê margem nem valor de contrato"; esta é a interpretação do Backend 2 pra
 * fazer valer isso na prática. Confirmar com o time se o corte de campos
 * está certo, e quais perfis (seção 7 do Modelo de Dados) devem receber essa
 * permissão além de FINANCEIRO/GESTOR/ADMINISTRADOR.
 */
public final class CamposSensiveis {

    public static final Set<String> NOMES = Set.of(
            "custo", "margemPercentual", "markupValor", "markupTipo", "comissaoValor", "comissaoTipo"
    );

    private CamposSensiveis() {
    }
}
