package ProjetoIntegrador.comum;
import jakarta.persistence.*;
import java.time.Instant;
@MappedSuperclass

public abstract class AuditavelBase {
    @Column(name="tenant_id",nullable=false,updatable=false) private String tenantId;
    @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
    @Column(name="created_by",nullable=false,updatable=false) private String createdBy;
    @Column(name="updated_at",nullable=false) private Instant updatedAt;
    @Column(name="updated_by",nullable=false) private String updatedBy;
    @Column(name="deleted_at") private Instant deletedAt;
    @Version private Long versao;
    public void registrarCriacao(String tenant,String autor) {
        tenantId=tenant; createdBy=autor; createdAt=Instant.now(); updatedBy=autor; updatedAt=createdAt;
    }
    public void registrarAlteracao(String autor) { updatedBy=autor; updatedAt=Instant.now(); }
    public String getTenantId() { return tenantId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
