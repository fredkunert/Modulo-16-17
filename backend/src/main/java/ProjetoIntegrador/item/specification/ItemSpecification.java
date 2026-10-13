package ProjetoIntegrador.item.specification;
import ProjetoIntegrador.item.model.*;
import org.springframework.data.jpa.domain.Specification;
import java.util.*;
public final class ItemSpecification {
    private ItemSpecification() {}
    public static Specification<Item> filtros(String tenant,String q,Categoria categoria,UUID fabricante,UUID distribuidor,Boolean ativo) {
        return (root,query,cb) -> {
            var predicates=new ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.equal(root.get("tenantId"),tenant));
            if(q!=null && !q.isBlank()) {
                String termo="%"+q.strip().toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_")+"%";
                predicates.add(cb.or(cb.like(cb.lower(root.get("sku")),termo,'!'),cb.like(cb.lower(root.get("nome")),termo,'!')));
            }
            if(categoria!=null) predicates.add(cb.equal(root.get("categoria"),categoria));
            if(fabricante!=null) predicates.add(cb.equal(root.get("fabricanteId"),fabricante));
            if(distribuidor!=null) predicates.add(cb.equal(root.get("distribuidorId"),distribuidor));
            if(ativo!=null) predicates.add(cb.equal(root.get("ativo"),ativo));
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }
}
