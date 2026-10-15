package ProjetoIntegrador.fabricante.repository;
import ProjetoIntegrador.fabricante.model.Fabricante;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface FabricanteRepository extends JpaRepository<Fabricante,UUID> {
    Optional<Fabricante> findByIdAndTenantId(UUID id,String tenantId);
    Page<Fabricante> findByTenantId(String tenantId,Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Fabricante p where p.id=:id and p.tenantId=:tenant")
    Optional<Fabricante> bloquear(UUID id,String tenant);
}
