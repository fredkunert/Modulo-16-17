package ProjetoIntegrador.fabricante.model;
import ProjetoIntegrador.comum.AuditavelBase;
import jakarta.persistence.*;
import java.util.UUID;
@Entity
@Table(name="fabricantes",schema="produtos",uniqueConstraints=@UniqueConstraint(columnNames={"tenant_id","id"}))
public class Fabricante extends AuditavelBase {
    @Id private UUID id=UUID.randomUUID();
    @Column(nullable=false,columnDefinition="text") private String nome;
    @Column(name="situacao",nullable=false) private boolean ativo=true;
    protected Fabricante() {}
    public Fabricante(String nome,boolean ativo) { this.nome=nome; this.ativo=ativo; }
    public UUID getId() { return id; }
    public String getNome() { return nome; }
    public boolean isAtivo() { return ativo; }
    public void setNome(String nome) { this.nome=nome; }
    public void setAtivo(boolean ativo) { this.ativo=ativo; }
}
