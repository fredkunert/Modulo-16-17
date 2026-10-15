package ProjetoIntegrador.comum;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController

public class HealthController {
    private final JdbcTemplate jdbc;
    public HealthController(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    @GetMapping("/api/produtos/health")
    public ResponseEntity<?> health() {
        try { jdbc.queryForObject("select 1",Integer.class); return ResponseEntity.ok(ApiResponse.ok(Map.of("status","UP"))); }
        catch(org.springframework.dao.DataAccessException e) {
            return ResponseEntity.status(503).body(ApiResponse.erro(null,"BANCO_INDISPONIVEL","Serviço indisponível."));
        }
    }
}
