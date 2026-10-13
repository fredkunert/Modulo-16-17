package ProjetoIntegrador.item.repository;
import ProjetoIntegrador.item.model.Item;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface ItemRepository extends JpaRepository<Item,UUID>,JpaSpecificationExecutor<Item> {
    Optional<Item> findByIdAndTenantId(UUID id,String tenantId);
    boolean existsByTenantIdAndSkuIgnoreCase(String tenantId,String sku);
    boolean existsByTenantIdAndSkuIgnoreCaseAndIdNot(String tenantId,String sku,UUID id);
    List<Item> findByTenantIdAndIdIn(String tenantId,Collection<UUID> ids);
}
