package br.edu.pi2026.produtos.precificacao;

import br.edu.pi2026.produtos.common.ApiResponse;
import br.edu.pi2026.produtos.common.PaginaResposta;
import br.edu.pi2026.produtos.common.TenantContext;
import br.edu.pi2026.produtos.precificacao.dto.PrecificacaoHistoricoResponse;
import br.edu.pi2026.produtos.precificacao.dto.PrecificacaoRequest;
import br.edu.pi2026.produtos.precificacao.dto.PrecificacaoResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * BE2-06. Testes de unidade do controller — sem MockMvc/contexto Spring de
 * propósito: este pacote não tem SecurityConfig (é do Backend 3), então um
 * teste web completo só faz sentido depois do merge. Aqui garantimos que o
 * controller delega certo pro service, pagina o histórico e mascara os
 * campos sensíveis conforme a permissão do token.
 */
@ExtendWith(MockitoExtension.class)
class PrecificacaoControllerTest {

    @Mock
    private PrecificacaoService precificacaoService;

    @Mock
    private TenantContext tenantContext;

    private Precificacao precificacaoDeExemplo(UUID itemId) {
        Precificacao p = new Precificacao();
        p.setItemId(itemId);
        p.setMoeda("BRL");
        p.setCusto(new BigDecimal("100.00"));
        p.setPreco(new BigDecimal("150.00"));
        p.setMargemPercentual(new BigDecimal("20.00"));
        p.setComissaoValor(new BigDecimal("5.00"));
        p.setUpdatedAt(OffsetDateTime.now());
        return p;
    }

    @Test
    void consultarDevolveTodosOsCamposQuandoTemPermissaoDeValoresSensiveis() {
        UUID itemId = UUID.randomUUID();
        when(precificacaoService.buscar(itemId)).thenReturn(precificacaoDeExemplo(itemId));
        when(tenantContext.possuiAutoridade("produtos.precificacao.ver_valores_sensiveis")).thenReturn(true);

        PrecificacaoController controller = new PrecificacaoController(precificacaoService, tenantContext);
        ResponseEntity<ApiResponse<PrecificacaoResponse>> resposta = controller.consultar(itemId);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        PrecificacaoResponse data = resposta.getBody().data();
        assertThat(data.custo()).isEqualByComparingTo("100.00");
        assertThat(data.margemPercentual()).isEqualByComparingTo("20.00");
        assertThat(data.comissaoValor()).isEqualByComparingTo("5.00");
        assertThat(data.preco()).isEqualByComparingTo("150.00"); // sempre visível
    }

    @Test
    void consultarOcultaCamposSensiveisSemAPermissaoEspecifica() {
        UUID itemId = UUID.randomUUID();
        when(precificacaoService.buscar(itemId)).thenReturn(precificacaoDeExemplo(itemId));
        when(tenantContext.possuiAutoridade("produtos.precificacao.ver_valores_sensiveis")).thenReturn(false);

        PrecificacaoController controller = new PrecificacaoController(precificacaoService, tenantContext);
        ResponseEntity<ApiResponse<PrecificacaoResponse>> resposta = controller.consultar(itemId);

        PrecificacaoResponse data = resposta.getBody().data();
        assertThat(data.custo()).isNull();
        assertThat(data.margemPercentual()).isNull();
        assertThat(data.comissaoValor()).isNull();
        assertThat(data.preco()).isEqualByComparingTo("150.00"); // continua visível
    }

    @Test
    void salvarRepassaORequestParaOServiceEDevolveOResultadoMascarado() {
        UUID itemId = UUID.randomUUID();
        PrecificacaoRequest request = new PrecificacaoRequest(
                "BRL", new BigDecimal("50.00"), new BigDecimal("90.00"),
                null, null, null, null, null, null, null
        );

        Precificacao salvo = new Precificacao();
        salvo.setItemId(itemId);
        salvo.setMoeda("BRL");
        salvo.setCusto(new BigDecimal("50.00"));
        salvo.setPreco(new BigDecimal("90.00"));

        when(precificacaoService.salvar(itemId, request)).thenReturn(salvo);
        when(tenantContext.possuiAutoridade("produtos.precificacao.ver_valores_sensiveis")).thenReturn(true);

        PrecificacaoController controller = new PrecificacaoController(precificacaoService, tenantContext);
        ResponseEntity<ApiResponse<PrecificacaoResponse>> resposta = controller.salvar(itemId, request);

        verify(precificacaoService).salvar(itemId, request);
        assertThat(resposta.getBody().data().preco()).isEqualByComparingTo("90.00");
    }

    @Test
    void historicoPaginaEMascaraCamposSensiveisPorLinha() {
        UUID itemId = UUID.randomUUID();

        PrecificacaoHistorico linhaSensivel = new PrecificacaoHistorico();
        linhaSensivel.setCampoAlterado("custo");
        linhaSensivel.setValorAnterior("100.00");
        linhaSensivel.setValorNovo("120.00");
        linhaSensivel.setAlteradoPor("usuario-local");
        linhaSensivel.setAlteradoEm(OffsetDateTime.now());

        PrecificacaoHistorico linhaNaoSensivel = new PrecificacaoHistorico();
        linhaNaoSensivel.setCampoAlterado("preco");
        linhaNaoSensivel.setValorAnterior("140.00");
        linhaNaoSensivel.setValorNovo("150.00");
        linhaNaoSensivel.setAlteradoPor("usuario-local");
        linhaNaoSensivel.setAlteradoEm(OffsetDateTime.now());

        when(precificacaoService.historico(eq(itemId), any()))
                .thenReturn(new PageImpl<>(List.of(linhaSensivel, linhaNaoSensivel)));
        when(tenantContext.possuiAutoridade("produtos.precificacao.ver_valores_sensiveis")).thenReturn(false);

        PrecificacaoController controller = new PrecificacaoController(precificacaoService, tenantContext);
        ResponseEntity<ApiResponse<PaginaResposta<PrecificacaoHistoricoResponse>>> resposta =
                controller.historico(itemId, 0, 20);

        List<PrecificacaoHistoricoResponse> itens = resposta.getBody().data().itens();
        assertThat(itens).hasSize(2);

        PrecificacaoHistoricoResponse custo = itens.stream()
                .filter(i -> i.campoAlterado().equals("custo")).findFirst().orElseThrow();
        assertThat(custo.valorAnterior()).isNull(); // sensível, oculto

        PrecificacaoHistoricoResponse preco = itens.stream()
                .filter(i -> i.campoAlterado().equals("preco")).findFirst().orElseThrow();
        // não sensível, visível — e agora como número, não texto (correção da Especificação)
        assertThat(preco.valorAnterior()).isInstanceOf(BigDecimal.class);
        assertThat((BigDecimal) preco.valorAnterior()).isEqualByComparingTo("140.00");
    }

    @Test
    void historicoDeCampoDeTipoContinuaComoTextoNaoNumero() {
        UUID itemId = UUID.randomUUID();

        PrecificacaoHistorico linhaDeTipo = new PrecificacaoHistorico();
        linhaDeTipo.setCampoAlterado("markupTipo");
        linhaDeTipo.setValorAnterior("PERCENTUAL");
        linhaDeTipo.setValorNovo("FATOR");
        linhaDeTipo.setAlteradoPor("usuario-local");
        linhaDeTipo.setAlteradoEm(OffsetDateTime.now());

        when(precificacaoService.historico(eq(itemId), any()))
                .thenReturn(new PageImpl<>(List.of(linhaDeTipo)));
        when(tenantContext.possuiAutoridade("produtos.precificacao.ver_valores_sensiveis")).thenReturn(true);

        PrecificacaoController controller = new PrecificacaoController(precificacaoService, tenantContext);
        ResponseEntity<ApiResponse<PaginaResposta<PrecificacaoHistoricoResponse>>> resposta =
                controller.historico(itemId, 0, 20);

        PrecificacaoHistoricoResponse linha = resposta.getBody().data().itens().get(0);
        assertThat(linha.valorAnterior()).isInstanceOf(String.class);
        assertThat(linha.valorAnterior()).isEqualTo("PERCENTUAL");
    }

    @Test
    void historicoLimitaTamanhoDaPaginaA100() {
        UUID itemId = UUID.randomUUID();
        when(precificacaoService.historico(eq(itemId), any())).thenReturn(new PageImpl<>(List.of()));
        when(tenantContext.possuiAutoridade(anyString())).thenReturn(true);

        PrecificacaoController controller = new PrecificacaoController(precificacaoService, tenantContext);
        controller.historico(itemId, 0, 500);

        verify(precificacaoService).historico(itemId, PageRequest.of(0, 100));
    }
}
