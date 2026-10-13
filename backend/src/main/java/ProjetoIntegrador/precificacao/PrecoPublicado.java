package ProjetoIntegrador.precificacao;
import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import java.util.UUID;
import java.math.BigDecimal;
import ProjetoIntegrador.comum.AuditavelBase;

@Entity @Immutable @Table(name="precificacoes",schema="produtos")
public class PrecoPublicado extends AuditavelBase {
    @Id private UUID id;
    @Column(name="item_id",nullable=false) private UUID itemId;
    @Column(name="preco_venda",precision=15,scale=2) private BigDecimal precoVenda;
    @Column(name="preco_minimo",precision=15,scale=2) private BigDecimal precoMinimo;
    @Column(name="desconto_maximo",precision=15,scale=4) private BigDecimal descontoMaximo;
    public UUID getItemId() { return itemId; }
    public BigDecimal getPrecoVenda() { return precoVenda; }
    public BigDecimal getPrecoMinimo() { return precoMinimo; }
    public BigDecimal getDescontoMaximo() { return descontoMaximo; }
}
