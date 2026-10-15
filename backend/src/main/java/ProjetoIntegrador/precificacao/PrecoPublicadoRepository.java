package ProjetoIntegrador.precificacao;
import org.springframework.data.repository.Repository;
import java.util.*;
public interface PrecoPublicadoRepository extends Repository<PrecoPublicado,UUID> {
    List<PrecoPublicado> findByTenantIdAndItemIdIn(String tenantId,Collection<UUID> ids);
}
