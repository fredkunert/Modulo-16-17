package br.edu.pi2026.produtos.precificacao;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Registro manual e independente (Especificação v0.2, seção 5.1): alterar um
 * campo nunca recalcula, limpa ou sobrescreve outro. O servidor não valida
 * consistência matemática entre os campos.
 *
 * Mapeia a tabela COMPARTILHADA produtos.precificacoes (migrations V1 e V3 do
 * serviço de catálogo). Colunas que este serviço não gerencia — preco_minimo,
 * desconto_maximo, deleted_at — não são mapeadas e nunca são tocadas.
 */
@Entity
@Table(name = "precificacoes")
@Getter
@Setter
public class Precificacao implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(name = "item_id", nullable = false, updatable = false)
    private UUID itemId;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private String tenantId;

    @Column(nullable = false, length = 3)
    private String moeda = "BRL";

    private BigDecimal custo;

    @Column(name = "preco_venda")
    private BigDecimal preco;

    @Column(name = "margem")
    private BigDecimal margemPercentual;

    @Column(name = "markup_valor")
    private BigDecimal markupValor;

    @Column(name = "markup_tipo")
    private String markupTipo;

    @Column(name = "impostos_valor")
    private BigDecimal impostosValor;

    @Convert(converter = TipoValorConverter.class)
    @Column(name = "impostos_tipo")
    private String impostosTipo;

    @Column(name = "comissao_valor")
    private BigDecimal comissaoValor;

    @Convert(converter = TipoValorConverter.class)
    @Column(name = "comissao_tipo")
    private String comissaoTipo;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "updated_by", nullable = false)
    private String updatedBy;

    /**
     * Lock otimista (BE2-04). O Hibernate compara a versão lida com a versão em
     * disco no UPDATE; duas edições quase simultâneas do mesmo item fazem a
     * segunda receber ObjectOptimisticLockingFailureException, traduzida em 409
     * pelo GlobalExceptionHandler.
     */
    @Version
    @Column(name = "versao", nullable = false)
    private long versao;

    /** ID é gerado pela aplicação, então o Spring Data precisa saber se é INSERT ou UPDATE. */
    @Transient
    private boolean novo = true;

    @Override
    public boolean isNew() {
        return novo;
    }

    @PostLoad
    @PostPersist
    void marcarComoExistente() {
        this.novo = false;
    }
}
