package ProjetoIntegrador.distribuidor.model;
import ProjetoIntegrador.comum.AuditavelBase;
import jakarta.persistence.*;
import java.util.UUID;
@Entity
@Table(name="distribuidores",schema="produtos",uniqueConstraints=@UniqueConstraint(columnNames={"tenant_id","id"}))
public class Distribuidor extends AuditavelBase {
    @Id private UUID id=UUID.randomUUID();
    @Column(nullable=false,columnDefinition="text") private String nome;
    @Column(name="situacao",nullable=false) private boolean ativo=true;
    protected Distribuidor() {}
    public Distribuidor(String nome,boolean ativo) { this.nome=nome; this.ativo=ativo; }
    public UUID getId() { return id; }
    public String getNome() { return nome; }
    public boolean isAtivo() { return ativo; }
    public void setNome(String nome) { this.nome=nome; }
    public void setAtivo(boolean ativo) { this.ativo=ativo; }
}
