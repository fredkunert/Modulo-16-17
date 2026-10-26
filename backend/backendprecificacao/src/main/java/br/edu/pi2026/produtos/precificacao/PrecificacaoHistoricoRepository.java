package br.edu.pi2026.produtos.precificacao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PrecificacaoHistoricoRepository extends JpaRepository<PrecificacaoHistorico, UUID> {
    // BE2-03: "fornecer consulta paginada" — histórico pode crescer sem limite.
    Page<PrecificacaoHistorico> findByItemIdAndTenantIdOrderByAlteradoEmDesc(UUID itemId, String tenantId, Pageable pageable);
}
