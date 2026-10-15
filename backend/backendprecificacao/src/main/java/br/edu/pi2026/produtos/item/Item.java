package br.edu.pi2026.produtos.item;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Projeção SOMENTE LEITURA da tabela produtos.itens, que pertence ao serviço de
 * catálogo (backend 1) e é criada pela migration V1 dele. Este serviço só
 * precisa saber se o item existe e a qual tenant pertence.
 */
@Entity
@Immutable
@Table(name = "itens")
@Getter
@Setter
public class Item {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;
}
