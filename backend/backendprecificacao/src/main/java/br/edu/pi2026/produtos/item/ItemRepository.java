package br.edu.pi2026.produtos.item;

import org.springframework.data.repository.Repository;

import java.util.Optional;
import java.util.UUID;

/** Somente leitura: itens são criados e alterados pelo serviço de catálogo. */
public interface ItemRepository extends Repository<Item, UUID> {
    Optional<Item> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, String tenantId);
}
