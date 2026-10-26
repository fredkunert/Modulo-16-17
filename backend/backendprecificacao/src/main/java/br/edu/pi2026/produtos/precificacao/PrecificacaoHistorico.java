package br.edu.pi2026.produtos.precificacao;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Tabela produtos.precificacoes_historico (migration V3 do serviço de catálogo). */
@Entity
@Table(name = "precificacoes_historico")
@Getter
@Setter
public class PrecificacaoHistorico {

    @Id
    private UUID id;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "campo_alterado", nullable = false)
    private String campoAlterado;

    @Column(name = "valor_anterior")
    private String valorAnterior;

    @Column(name = "valor_novo")
    private String valorNovo;

    @Column(name = "alterado_por", nullable = false)
    private String alteradoPor;

    @Column(name = "alterado_em", nullable = false)
    private OffsetDateTime alteradoEm;
}
