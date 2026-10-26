package br.edu.pi2026.produtos.precificacao;

import br.edu.pi2026.produtos.common.NaoEncontradoException;
import br.edu.pi2026.produtos.common.TenantContext;
import br.edu.pi2026.produtos.item.Item;
import br.edu.pi2026.produtos.item.ItemRepository;
import br.edu.pi2026.produtos.precificacao.dto.PrecificacaoRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * BE2-06. Cobre exatamente o que a Especificação v0.2 exige como critério de
 * aceite: independência entre campos (CA-05), preservação de valores
 * divergentes matematicamente (CA-06) e ausência de valor não virar zero
 * (CA-11) — além de isolar item de outro tenant (garantirItemExiste).
 */
@ExtendWith(MockitoExtension.class)
class PrecificacaoServiceTest {

    @Mock private PrecificacaoRepository precificacaoRepository;
    @Mock private PrecificacaoHistoricoRepository historicoRepository;
    @Mock private ItemRepository itemRepository;
    @Mock private TenantContext tenantContext;

    private PrecificacaoService service;

    private final String tenantId = "tenant-local";
    private final String usuarioId = "usuario-local";
    private final UUID itemId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new PrecificacaoService(precificacaoRepository, historicoRepository, itemRepository, tenantContext);
        lenient().when(tenantContext.getTenantId()).thenReturn(tenantId);
        lenient().when(tenantContext.getUsuarioId()).thenReturn(usuarioId);

        Item item = new Item();
        item.setId(itemId);
        item.setTenantId(tenantId);
        lenient().when(itemRepository.findByIdAndTenantIdAndDeletedAtIsNull(itemId, tenantId))
                .thenReturn(Optional.of(item));
    }

    @Test
    void buscarLancaNaoEncontradoQuandoItemNaoExisteOuEhDeOutroTenant() {
        when(itemRepository.findByIdAndTenantIdAndDeletedAtIsNull(itemId, tenantId))
                .thenReturn(Optional.empty());

        assertThrows(NaoEncontradoException.class, () -> service.buscar(itemId));
    }

    @Test
    void buscarRetornaObjetoVazioQuandoItemExisteMasNuncaFoiPrecificado() {
        when(precificacaoRepository.findByItemIdAndTenantId(itemId, tenantId)).thenReturn(Optional.empty());

        Precificacao resultado = service.buscar(itemId);

        assertThat(resultado.getItemId()).isEqualTo(itemId);
        assertThat(resultado.getCusto()).isNull();
        assertThat(resultado.getPreco()).isNull();
    }

    @Test
    void salvarGravaExatamenteOsValoresRecebidosSemRecalcular() {
        when(precificacaoRepository.findByItemIdAndTenantId(itemId, tenantId)).thenReturn(Optional.empty());
        when(precificacaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Exemplo da própria Especificação (seção 5.5): margem e markup não
        // batem matematicamente com custo/preço — e mesmo assim são salvos
        // sem bloqueio (CA-06).
        PrecificacaoRequest request = new PrecificacaoRequest(
                "BRL", new BigDecimal("100.00"), new BigDecimal("150.00"),
                new BigDecimal("20.00"), new BigDecimal("10.00"), "PERCENTUAL",
                new BigDecimal("8.00"), "PERCENTUAL", new BigDecimal("5.00"), "PERCENTUAL"
        );

        Precificacao resultado = service.salvar(itemId, request);

        assertThat(resultado.getCusto()).isEqualByComparingTo("100.00");
        assertThat(resultado.getPreco()).isEqualByComparingTo("150.00");
        assertThat(resultado.getMargemPercentual()).isEqualByComparingTo("20.00");
        assertThat(resultado.getMarkupValor()).isEqualByComparingTo("10.00");
    }

    @Test
    void alterarSoCustoNaoMexeNosOutrosCamposNemGeraHistoricoDeles() {
        Precificacao existente = new Precificacao();
        existente.setItemId(itemId);
        existente.setTenantId(tenantId);
        existente.setMoeda("BRL");
        existente.setCusto(new BigDecimal("100.00"));
        existente.setPreco(new BigDecimal("150.00"));

        when(precificacaoRepository.findByItemIdAndTenantId(itemId, tenantId)).thenReturn(Optional.of(existente));
        when(precificacaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PrecificacaoRequest request = new PrecificacaoRequest(
                "BRL", new BigDecimal("120.00"), new BigDecimal("150.00"),
                null, null, null, null, null, null, null
        );

        Precificacao resultado = service.salvar(itemId, request);

        assertThat(resultado.getCusto()).isEqualByComparingTo("120.00");
        assertThat(resultado.getPreco()).isEqualByComparingTo("150.00"); // não recalculou

        ArgumentCaptor<PrecificacaoHistorico> captor = ArgumentCaptor.forClass(PrecificacaoHistorico.class);
        verify(historicoRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getCampoAlterado()).isEqualTo("custo");
        assertThat(captor.getValue().getValorAnterior()).isEqualTo("100.00");
        assertThat(captor.getValue().getValorNovo()).isEqualTo("120.00");
        assertThat(captor.getValue().getAlteradoPor()).isEqualTo(usuarioId);
    }

    @Test
    void enviarCampoNuloApagaValorAnteriorSemVirarZero() {
        Precificacao existente = new Precificacao();
        existente.setItemId(itemId);
        existente.setTenantId(tenantId);
        existente.setComissaoValor(new BigDecimal("5.00"));

        when(precificacaoRepository.findByItemIdAndTenantId(itemId, tenantId)).thenReturn(Optional.of(existente));
        when(precificacaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PrecificacaoRequest request = new PrecificacaoRequest(
                null, null, null, null, null, null, null, null, null, null
        );

        Precificacao resultado = service.salvar(itemId, request);

        assertThat(resultado.getComissaoValor()).isNull(); // CA-11: null, não zero
    }

    @Test
    void historicoLancaNaoEncontradoQuandoItemEhDeOutroTenant() {
        when(itemRepository.findByIdAndTenantIdAndDeletedAtIsNull(itemId, tenantId))
                .thenReturn(Optional.empty());

        assertThrows(NaoEncontradoException.class,
                () -> service.historico(itemId, PageRequest.of(0, 20)));
    }

    @Test
    void historicoDevolvePaginaVindaDoRepositorio() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<PrecificacaoHistorico> paginaEsperada = new PageImpl<>(List.of(new PrecificacaoHistorico()));
        when(historicoRepository.findByItemIdAndTenantIdOrderByAlteradoEmDesc(itemId, tenantId, pageable))
                .thenReturn(paginaEsperada);

        Page<PrecificacaoHistorico> resultado = service.historico(itemId, pageable);

        assertThat(resultado.getTotalElements()).isEqualTo(1);
    }

    /**
     * BE2-06 — "isolamento entre empresas". Mesmo item, mesmo itemId, mas
     * pesquisado por um tenant diferente do dono: precisa se comportar como
     * se o item não existisse, e a consulta nem deve chegar a perguntar pela
     * precificação — o corte acontece na checagem do item (Contrato de
     * Integração, seção 6: tenant_id nunca cruza).
     */
    @Test
    void precificacaoDeUmTenantNaoEVisivelPorOutroTenant() {
        String tenantB = "outro-tenant";
        when(itemRepository.findByIdAndTenantIdAndDeletedAtIsNull(itemId, tenantB))
                .thenReturn(Optional.empty());

        TenantContext contextoDoTenantB = mock(TenantContext.class);
        when(contextoDoTenantB.getTenantId()).thenReturn(tenantB);

        PrecificacaoService servicoComoTenantB = new PrecificacaoService(
                precificacaoRepository, historicoRepository, itemRepository, contextoDoTenantB);

        assertThrows(NaoEncontradoException.class, () -> servicoComoTenantB.buscar(itemId));
        assertThrows(NaoEncontradoException.class,
                () -> servicoComoTenantB.historico(itemId, PageRequest.of(0, 20)));

        verify(precificacaoRepository, never()).findByItemIdAndTenantId(eq(itemId), eq(tenantB));
        verify(historicoRepository, never())
                .findByItemIdAndTenantIdOrderByAlteradoEmDesc(eq(itemId), eq(tenantB), any());
    }

    // ---- regras que vêm do banco compartilhado (CHECKs da tabela precificacoes) ----

    @Test
    void markupValorSemTipoEhRejeitadoComMensagemClara() {
        PrecificacaoRequest request = new PrecificacaoRequest(
                "BRL", null, null, null, new BigDecimal("10.00"), null, null, null, null, null);

        var erro = assertThrows(br.edu.pi2026.produtos.common.NegocioException.class,
                () -> service.salvar(itemId, request));

        assertThat(erro.getCodigo()).isEqualTo("VALOR_E_TIPO_JUNTOS");
        assertThat(erro.getCampo()).isEqualTo("markupTipo");
        verify(precificacaoRepository, never()).save(any());
    }

    @Test
    void comissaoTipoSemValorEhRejeitado() {
        PrecificacaoRequest request = new PrecificacaoRequest(
                "BRL", null, null, null, null, null, null, null, null, "PERCENTUAL");

        var erro = assertThrows(br.edu.pi2026.produtos.common.NegocioException.class,
                () -> service.salvar(itemId, request));

        assertThat(erro.getCampo()).isEqualTo("comissaoValor");
    }

    @Test
    void impostoMonetarioComMaisDeDuasCasasEhRejeitadoMasPercentualAceita() {
        PrecificacaoRequest monetario = new PrecificacaoRequest(
                "BRL", null, null, null, null, null,
                new BigDecimal("8.1234"), "MONETARIO", null, null);

        var erro = assertThrows(br.edu.pi2026.produtos.common.NegocioException.class,
                () -> service.salvar(itemId, monetario));
        assertThat(erro.getCodigo()).isEqualTo("CASAS_DECIMAIS_MONETARIO");

        when(precificacaoRepository.findByItemIdAndTenantId(itemId, tenantId)).thenReturn(Optional.empty());
        when(precificacaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        PrecificacaoRequest percentual = new PrecificacaoRequest(
                "BRL", null, null, null, null, null,
                new BigDecimal("8.1234"), "PERCENTUAL", null, null);

        Precificacao salvo = service.salvar(itemId, percentual);
        assertThat(salvo.getImpostosValor()).isEqualByComparingTo("8.1234");
    }

    @Test
    void salvarPrimeiraVezGravaAutorEIdentificadores() {
        when(precificacaoRepository.findByItemIdAndTenantId(itemId, tenantId)).thenReturn(Optional.empty());
        when(precificacaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PrecificacaoRequest request = new PrecificacaoRequest(
                "BRL", new BigDecimal("10.00"), null, null, null, null, null, null, null, null);

        Precificacao salvo = service.salvar(itemId, request);

        assertThat(salvo.getId()).isNotNull();
        assertThat(salvo.getItemId()).isEqualTo(itemId);
        assertThat(salvo.getTenantId()).isEqualTo(tenantId);
        assertThat(salvo.getCreatedBy()).isEqualTo(usuarioId);
        assertThat(salvo.getUpdatedBy()).isEqualTo(usuarioId);
        assertThat(salvo.isNew()).isTrue();
    }
}
