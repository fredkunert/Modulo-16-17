package ProjetoIntegrador.item.model;
import ProjetoIntegrador.comum.AuditavelBase;
import jakarta.persistence.*;
import java.util.UUID;
import java.math.BigDecimal;
@Entity
@Table(name="itens",schema="produtos",uniqueConstraints={@UniqueConstraint(name="uk_itens_tenant_sku",columnNames={"tenant_id","sku"}),@UniqueConstraint(columnNames={"tenant_id","id"})})
public class Item extends AuditavelBase {
    @Id private UUID id=UUID.randomUUID();
    @Column(nullable=false) private boolean ativo=true;
    public Item() {}
    public UUID getId() { return id; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo=ativo; }
    @Column(nullable=false,length=64) private String sku;
    public String getSku() { return sku; }
    public void setSku(String valor) { sku=valor; }
    @Column(nullable=false,columnDefinition="text") private String nome;
    public String getNome() { return nome; }
    public void setNome(String valor) { nome=valor; }
    @Column(name="fabricante_id") private UUID fabricanteId;
    public UUID getFabricanteId() { return fabricanteId; }
    public void setFabricanteId(UUID valor) { fabricanteId=valor; }
    @Column(name="distribuidor_id") private UUID distribuidorId;
    public UUID getDistribuidorId() { return distribuidorId; }
    public void setDistribuidorId(UUID valor) { distribuidorId=valor; }
    @Enumerated(EnumType.STRING) @Column(nullable=false) private Categoria categoria;
    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria valor) { categoria=valor; }
    @Column(columnDefinition="text") private String descricao;
    public String getDescricao() { return descricao; }
    public void setDescricao(String valor) { descricao=valor; }
    @Enumerated(EnumType.STRING) @Column(nullable=false) private UnidadeComercial unidade;
    public UnidadeComercial getUnidade() { return unidade; }
    public void setUnidade(UnidadeComercial valor) { unidade=valor; }
    @Enumerated(EnumType.STRING) @Column(nullable=false) private Periodicidade periodicidade;
    public Periodicidade getPeriodicidade() { return periodicidade; }
    public void setPeriodicidade(Periodicidade valor) { periodicidade=valor; }
    @Column(name="quantidade_minima",nullable=false,precision=15,scale=4) private BigDecimal quantidadeMinima;
    public BigDecimal getQuantidadeMinima() { return quantidadeMinima; }
    public void setQuantidadeMinima(BigDecimal valor) { quantidadeMinima=valor; }
}
