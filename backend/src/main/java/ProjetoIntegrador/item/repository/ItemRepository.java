package ProjetoIntegrador.item.repository;
import ProjetoIntegrador.item.model.Item;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface ItemRepository extends JpaRepository<Item,UUID>,JpaSpecificationExecutor<Item> {
    Optional<Item> findByTokenPublico(UUID tokenPublico);
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Item i where i.id=:id and i.tenantId=:tenant")
    Optional<Item> bloquearCompartilhamento(UUID id,String tenant);
    Optional<Item> findByIdAndTenantId(UUID id,String tenantId);
    boolean existsByTenantIdAndSkuIgnoreCase(String tenantId,String sku);
    boolean existsByTenantIdAndSkuIgnoreCaseAndIdNot(String tenantId,String sku,UUID id);
    List<Item> findByTenantIdAndIdIn(String tenantId,Collection<UUID> ids);
}
