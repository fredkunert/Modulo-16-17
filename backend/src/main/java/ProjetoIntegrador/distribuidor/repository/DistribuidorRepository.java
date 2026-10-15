package ProjetoIntegrador.distribuidor.repository;
import ProjetoIntegrador.distribuidor.model.Distribuidor;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface DistribuidorRepository extends JpaRepository<Distribuidor,UUID> {
    Optional<Distribuidor> findByIdAndTenantId(UUID id,String tenantId);
    Page<Distribuidor> findByTenantId(String tenantId,Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Distribuidor p where p.id=:id and p.tenantId=:tenant")
    Optional<Distribuidor> bloquear(UUID id,String tenant);
}
