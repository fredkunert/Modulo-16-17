package br.edu.pi2026.produtos.precificacao;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PrecificacaoRepository extends JpaRepository<Precificacao, UUID> {
    Optional<Precificacao> findByItemIdAndTenantId(UUID itemId, String tenantId);
}
