package br.edu.pi2026.produtos.precificacao;

import br.edu.pi2026.produtos.common.NaoEncontradoException;
import br.edu.pi2026.produtos.common.NegocioException;
import br.edu.pi2026.produtos.common.TenantContext;
import br.edu.pi2026.produtos.item.ItemRepository;
import br.edu.pi2026.produtos.precificacao.dto.PrecificacaoRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Grava exatamente o que o funcionário digitou. Nenhum campo é recalculado,
 * comparado com outro ou preenchido automaticamente — é o comportamento
 * confirmado na Especificação v0.2, seção 5.1, e vale tanto para a interface
 * quanto para a API (o servidor não recalcula os dados enviados).
 *
 * Única exigência estrutural, vinda do banco compartilhado: valor e tipo de
 * markup, impostos e comissão andam juntos (ambos preenchidos ou ambos vazios),
 * e valor monetário aceita no máximo 2 casas decimais.
 */
@Service
public class PrecificacaoService {

    private final PrecificacaoRepository precificacaoRepository;
    private final PrecificacaoHistoricoRepository historicoRepository;
    private final ItemRepository itemRepository;
    private final TenantContext tenantContext;

    public PrecificacaoService(PrecificacaoRepository precificacaoRepository,
                                PrecificacaoHistoricoRepository historicoRepository,
                                ItemRepository itemRepository,
                                TenantContext tenantContext) {
        this.precificacaoRepository = precificacaoRepository;
        this.historicoRepository = historicoRepository;
        this.itemRepository = itemRepository;
        this.tenantContext = tenantContext;
    }

    public Precificacao buscar(UUID itemId) {
        String tenantId = tenantContext.getTenantId();
        garantirItemExiste(itemId, tenantId);
        return precificacaoRepository.findByItemIdAndTenantId(itemId, tenantId)
                .orElseGet(() -> vazio(itemId, tenantId));
    }

    public Page<PrecificacaoHistorico> historico(UUID itemId, Pageable pageable) {
        String tenantId = tenantContext.getTenantId();
        garantirItemExiste(itemId, tenantId);
        return historicoRepository.findByItemIdAndTenantIdOrderByAlteradoEmDesc(itemId, tenantId, pageable);
    }

    @Transactional
    public Precificacao salvar(UUID itemId, PrecificacaoRequest request) {
        validarRegrasEstruturais(request);

        String tenantId = tenantContext.getTenantId();
        garantirItemExiste(itemId, tenantId);
        String usuarioId = tenantContext.getUsuarioId();

        Precificacao atual = precificacaoRepository.findByItemIdAndTenantId(itemId, tenantId)
                .orElseGet(() -> vazio(itemId, tenantId));

        registrarDiferenca(itemId, tenantId, usuarioId, "moeda", atual.getMoeda(), request.moeda());
        registrarDiferenca(itemId, tenantId, usuarioId, "custo", atual.getCusto(), request.custo());
        registrarDiferenca(itemId, tenantId, usuarioId, "preco", atual.getPreco(), request.preco());
        registrarDiferenca(itemId, tenantId, usuarioId, "margemPercentual", atual.getMargemPercentual(), request.margemPercentual());
        registrarDiferenca(itemId, tenantId, usuarioId, "markupValor", atual.getMarkupValor(), request.markupValor());
        registrarDiferenca(itemId, tenantId, usuarioId, "markupTipo", atual.getMarkupTipo(), request.markupTipo());
        registrarDiferenca(itemId, tenantId, usuarioId, "impostosValor", atual.getImpostosValor(), request.impostosValor());
        registrarDiferenca(itemId, tenantId, usuarioId, "impostosTipo", atual.getImpostosTipo(), request.impostosTipo());
        registrarDiferenca(itemId, tenantId, usuarioId, "comissaoValor", atual.getComissaoValor(), request.comissaoValor());
        registrarDiferenca(itemId, tenantId, usuarioId, "comissaoTipo", atual.getComissaoTipo(), request.comissaoTipo());

        if (request.moeda() != null) atual.setMoeda(request.moeda());
        atual.setCusto(request.custo());
        atual.setPreco(request.preco());
        atual.setMargemPercentual(request.margemPercentual());
        atual.setMarkupValor(request.markupValor());
        atual.setMarkupTipo(request.markupTipo());
        atual.setImpostosValor(request.impostosValor());
        atual.setImpostosTipo(request.impostosTipo());
        atual.setComissaoValor(request.comissaoValor());
        atual.setComissaoTipo(request.comissaoTipo());
        if (atual.getCreatedBy() == null) {
            atual.setCreatedBy(usuarioId);
        }
        atual.setUpdatedBy(usuarioId);

        return precificacaoRepository.save(atual);
    }

    private void registrarDiferenca(UUID itemId, String tenantId, String usuarioId,
                                     String campo, Object valorAnterior, Object valorNovo) {
        if (Objects.equals(valorAnterior, valorNovo)) {
            return;
        }
        PrecificacaoHistorico historico = new PrecificacaoHistorico();
        historico.setId(UUID.randomUUID());
        historico.setItemId(itemId);
        historico.setTenantId(tenantId);
        historico.setCampoAlterado(campo);
        historico.setValorAnterior(valorAnterior != null ? valorAnterior.toString() : null);
        historico.setValorNovo(valorNovo != null ? valorNovo.toString() : null);
        historico.setAlteradoPor(usuarioId);
        historico.setAlteradoEm(OffsetDateTime.now());
        historicoRepository.save(historico);
    }

    private void validarRegrasEstruturais(PrecificacaoRequest r) {
        validarPar("markupValor", "markupTipo", r.markupValor(), r.markupTipo());
        validarPar("impostosValor", "impostosTipo", r.impostosValor(), r.impostosTipo());
        validarPar("comissaoValor", "comissaoTipo", r.comissaoValor(), r.comissaoTipo());
        validarCasasMonetarias("impostosValor", r.impostosTipo(), r.impostosValor());
        validarCasasMonetarias("comissaoValor", r.comissaoTipo(), r.comissaoValor());
    }

    private void validarPar(String campoValor, String campoTipo, BigDecimal valor, String tipo) {
        if ((valor == null) != (tipo == null)) {
            throw new NegocioException(HttpStatus.UNPROCESSABLE_ENTITY, valor == null ? campoValor : campoTipo,
                    "VALOR_E_TIPO_JUNTOS",
                    "Informe " + campoValor + " e " + campoTipo + " juntos, ou deixe os dois em branco.");
        }
    }

    private void validarCasasMonetarias(String campo, String tipo, BigDecimal valor) {
        if ("MONETARIO".equals(tipo) && valor != null && valor.stripTrailingZeros().scale() > 2) {
            throw new NegocioException(HttpStatus.UNPROCESSABLE_ENTITY, campo, "CASAS_DECIMAIS_MONETARIO",
                    "Valor monetário aceita no máximo 2 casas decimais.");
        }
    }

    private void garantirItemExiste(UUID itemId, String tenantId) {
        itemRepository.findByIdAndTenantIdAndDeletedAtIsNull(itemId, tenantId)
                .orElseThrow(() -> new NaoEncontradoException("Item não encontrado."));
    }

    private Precificacao vazio(UUID itemId, String tenantId) {
        Precificacao p = new Precificacao();
        p.setId(UUID.randomUUID());
        p.setItemId(itemId);
        p.setTenantId(tenantId);
        return p;
    }
}
